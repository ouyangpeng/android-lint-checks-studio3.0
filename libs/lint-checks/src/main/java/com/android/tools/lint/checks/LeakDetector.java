/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.lint.checks;

import static com.android.SdkConstants.CLASS_CONTEXT;
import static com.android.SdkConstants.CLASS_FRAGMENT;
import static com.android.SdkConstants.CLASS_VIEW;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.client.api.UElementHandler;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiKeyword;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.jetbrains.uast.UAnonymousClass;
import org.jetbrains.uast.UBinaryExpression;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UField;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UObjectLiteralExpression;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.UResolvable;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.util.UastExpressionUtils;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

/**
 * Looks for leaks via static fields
 */
public class LeakDetector extends Detector implements Detector.UastScanner {
    /** Leaking data via static fields */
    public static final Issue ISSUE = Issue.create(
            "StaticFieldLeak",
            "Static Field Leaks",

            "A static field will leak contexts.\n" +
            "\n" +
            "Non-static inner classes have an implicit reference to their outer class. " +
            "If that outer class is for example a `Fragment` or `Activity`, then this " +
            "reference means that the long-running handler/loader/task will hold a reference " +
            "to the activity which prevents it from getting garbage collected.\n" +
            "\n" +
            "Similarly, direct field references to activities and fragments from these " +
            "longer running instances can cause leaks.\n" +
            "\n" +
            "ViewModel classes should never point to Views or non-application Contexts.",

            Category.PERFORMANCE,
            6,
            Severity.WARNING,
            new Implementation(
                    LeakDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    private static final List<String> SUPER_CLASSES = Arrays.asList(
            "android.content.Loader",
            "android.support.v4.content.Loader",
            "android.os.AsyncTask",
            "android.arch.lifecycle.ViewModel"
    );

    /** Constructs a new {@link LeakDetector} check */
    public LeakDetector() {
    }

    // ---- Implements UastScanner ----

    @Nullable
    @Override
    public List<String> applicableSuperClasses() {
        return SUPER_CLASSES;
    }

    /** Warn about inner classes that aren't static: these end up retaining the outer class */
    @Override
    public void visitClass(@NonNull JavaContext context, @NonNull UClass declaration) {
        PsiClass containingClass = UastUtils.getContainingUClass(declaration);
        boolean isAnonymous = declaration instanceof UAnonymousClass;

        // Only consider static inner classes
        boolean isStatic = context.getEvaluator().isStatic(declaration) || containingClass == null;
        if (isStatic || isAnonymous) { // containingClass == null: implicitly static
            // But look for fields that store contexts
            for (UField field : declaration.getFields()) {
                checkInstanceField(context, field);
            }

            if (!isAnonymous) {
                return;
            }
        }

        String superClass = null;
        for (String cls : SUPER_CLASSES) {
            if (context.getEvaluator().inheritsFrom(declaration, cls, false)) {
                superClass = cls;
                break;
            }
        }
        assert superClass != null;

        //noinspection unchecked
        UCallExpression invocation = UastUtils.getParentOfType(
                declaration, UObjectLiteralExpression.class, true, UMethod.class);

        Location location;
        if (isAnonymous && invocation != null) {
            location = context.getCallLocation(invocation, false, false);
        } else {
            location = context.getNameLocation(declaration);
        }
        String name;
        if (isAnonymous) {
            name = "anonymous " + ((UAnonymousClass)declaration).getBaseClassReference().getQualifiedName();
        } else {
            name = declaration.getQualifiedName();
        }

        String superClassName = superClass.substring(superClass.lastIndexOf('.') + 1);
        context.report(ISSUE, declaration, location, String.format(
                "This %1$s class should be static or leaks might occur (%2$s)",
                superClassName,  name));
    }

    private static void checkInstanceField(@NonNull JavaContext context, @NonNull UField field) {
        PsiType type = field.getType();
        if (!(type instanceof PsiClassType)) {
            return;
        }

        String fqn = type.getCanonicalText();
        if (fqn.startsWith("java.")) {
            return;
        }
        PsiClass cls = ((PsiClassType) type).resolve();
        if (cls == null) {
            return;
        }

        if (LeakDetector.isLeakCandidate(cls, context.getEvaluator())) {
            context.report(LeakDetector.ISSUE, field, context.getLocation(field),
                    "This field leaks a context object");
        }
    }

    @Override
    public List<Class<? extends UElement>> getApplicableUastTypes() {
        return Collections.singletonList(UField.class);
    }

    @Override
    public UElementHandler createUastHandler(@NonNull JavaContext context) {
        return new FieldChecker(context);
    }

    private static class FieldChecker extends UElementHandler {
        private final JavaContext mContext;

        public FieldChecker(JavaContext context) {
            mContext = context;
        }

        @Override
        public void visitField(@NonNull UField field) {
            PsiModifierList modifierList = field.getModifierList();
            if (modifierList == null || !modifierList.hasModifierProperty(PsiModifier.STATIC)) {
                return;
            }

            PsiType type = field.getType();
            if (!(type instanceof PsiClassType)) {
                return;
            }

            String fqn = type.getCanonicalText();
            if (fqn.startsWith("java.")) {
                return;
            }
            PsiClass cls = ((PsiClassType) type).resolve();
            if (cls == null) {
                return;
            }
            if (fqn.startsWith("android.")) {
                if (isLeakCandidate(cls, mContext.getEvaluator())
                        && !isAppContextName(cls, field)
                        && !isInitializedToAppContext(field)) {
                    String message = "Do not place Android context classes in static fields; "
                            + "this is a memory leak (and also breaks Instant Run)";
                    report(field, modifierList, message);
                }
            } else {
                // User application object -- look to see if that one itself has
                // static fields?
                // We only check *one* level of indirection here
                int count = 0;
                for (PsiField referenced : cls.getAllFields()) {
                    // Only check a few; avoid getting bogged down on large classes
                    if (count++ == 20) {
                        break;
                    }

                    PsiType innerType = referenced.getType();
                    if (!(innerType instanceof PsiClassType)) {
                        continue;
                    }

                    fqn = innerType.getCanonicalText();
                    if (fqn.startsWith("java.")) {
                        continue;
                    }
                    PsiClass innerCls = ((PsiClassType) innerType).resolve();
                    if (innerCls == null) {
                        continue;
                    }
                    if (fqn.startsWith("android.")) {
                        if (isLeakCandidate(innerCls, mContext.getEvaluator())
                                && !isAppContextName(innerCls, field)) {
                            String message =
                                    "Do not place Android context classes in static fields "
                                            + "(static reference to `"
                                            + cls.getName() + "` which has field "
                                            + "`" + referenced.getName() + "` pointing to `"
                                            + innerCls.getName() + "`); "
                                        + "this is a memory leak (and also breaks Instant Run)";
                            report(field, modifierList, message);
                            break;
                        }
                    }
                }
            }
        }

        /**
         * If it's a static field see if it's initialized to an app context in
         * one of the constructors
         */
        private boolean isInitializedToAppContext(
                @NonNull UField field) {
            PsiClass containingClass = field.getContainingClass();
            if (containingClass == null) {
                return false;
            }

            for (PsiMethod method : containingClass.getConstructors()) {
                UExpression methodBody = mContext.getUastContext().getMethodBody(method);
                if (methodBody == null) {
                    continue;
                }
                Ref<Boolean> assignedToAppContext = new Ref<>(false);

                methodBody.accept(new AbstractUastVisitor() {
                    @Override
                    public boolean visitBinaryExpression(UBinaryExpression node) {
                        if (UastExpressionUtils.isAssignment(node) &&
                                node.getLeftOperand() instanceof UResolvable &&
                                field.getPsi().equals(
                                        ((UResolvable)node.getLeftOperand()).resolve())) {
                            // Yes, assigning to this field
                            // See if the right hand side looks like an app context
                            UElement rhs = node.getRightOperand();
                            while (rhs instanceof UQualifiedReferenceExpression) {
                                rhs = ((UQualifiedReferenceExpression)rhs).getSelector();
                            }
                            if (rhs instanceof UCallExpression) {
                                UCallExpression call = (UCallExpression) rhs;
                                if ("getApplicationContext".equals(call.getMethodName())) {
                                    assignedToAppContext.set(true);
                                }
                            }
                        }
                        return super.visitBinaryExpression(node);
                    }
                });

                if (assignedToAppContext.get()) {
                    return true;
                }
            }

            return false;
        }

        private void report(@NonNull PsiField field, @NonNull PsiModifierList modifierList,
                @NonNull String message) {
            PsiElement locationNode = field;
            // Try to find the static modifier itself
            if (modifierList.hasExplicitModifier(PsiModifier.STATIC)) {
                PsiElement child = modifierList.getFirstChild();
                while (child != null) {
                    if (child instanceof PsiKeyword
                            && PsiKeyword.STATIC.equals(child.getText())) {
                        locationNode = child;
                        break;
                    }
                    child = child.getNextSibling();
                }
            }
            Location location = mContext.getLocation(locationNode);
            mContext.report(ISSUE, field, location, message);
        }
    }

    private static boolean isAppContextName(@NonNull PsiClass cls, @NonNull PsiField field) {
        // Don't flag names like "sAppContext" or "applicationContext".
        String name = field.getName();
        if (name != null) {
            String lower = name.toLowerCase(Locale.US);
            if (lower.contains("appcontext") || lower.contains("application")) {
                if (CLASS_CONTEXT.equals(cls.getQualifiedName())) {
                    return true;
                }
            }
        }

        return false;
    }

    static boolean isLeakCandidate(
            @NonNull PsiClass cls,
            @NonNull JavaEvaluator evaluator) {
        return evaluator.extendsClass(cls, CLASS_CONTEXT, false)
                || evaluator.extendsClass(cls, CLASS_VIEW, false)
                || evaluator.extendsClass(cls, CLASS_FRAGMENT, false);
    }
}
