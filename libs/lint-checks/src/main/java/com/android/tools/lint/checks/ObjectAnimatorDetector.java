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

import static com.android.SdkConstants.SUPPORT_ANNOTATIONS_PREFIX;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.BuildTypeContainer;
import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ConstantEvaluator;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.UastScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.TypeEvaluator;
import com.android.tools.lint.detector.api.UastLintUtils;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UDeclaration;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UFile;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.USimpleNameReferenceExpression;
import org.jetbrains.uast.UastUtils;

/**
 * Looks for issues around ObjectAnimator usages
 */
public class ObjectAnimatorDetector extends Detector implements UastScanner {
    public static final String KEEP_ANNOTATION = SUPPORT_ANNOTATIONS_PREFIX + "Keep";

    private static final Implementation IMPLEMENTATION = new Implementation(
            ObjectAnimatorDetector.class,
            Scope.JAVA_FILE_SCOPE);

    /** Missing @Keep */
    public static final Issue MISSING_KEEP = Issue.create(
            "AnimatorKeep",
            "Missing @Keep for Animated Properties",

            "When you use property animators, properties can be accessed via reflection. "
                    + "Those methods should be annotated with @Keep to ensure that during "
                    + "release builds, the methods are not potentially treated as unused "
                    + "and removed, or treated as internal only and get renamed to something "
                    + "shorter.\n"
                    + "\n"
                    + "This check will also flag other potential reflection problems it "
                    + "encounters, such as a missing property, wrong argument types, etc.",

            Category.PERFORMANCE,
            4,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Incorrect ObjectAnimator binding */
    public static final Issue BROKEN_PROPERTY = Issue.create(
            "ObjectAnimatorBinding",
            "Incorrect ObjectAnimator Property",

            "This check cross references properties referenced by String from `ObjectAnimator` "
                    + "and `PropertyValuesHolder` method calls and ensures that the corresponding "
                    + "setter methods exist and have the right signatures.",

            Category.CORRECTNESS,
            4,
            Severity.ERROR,
            IMPLEMENTATION);

    /**
     * Multiple properties might all point back to the same setter; we don't want to
     * highlight these more than once (duplicate warnings etc) so keep track of them here
     */
    private Set<Object> mAlreadyWarned;

    /** Constructs a new {@link ObjectAnimatorDetector} */
    public ObjectAnimatorDetector() {
    }

    @Nullable
    @Override
    public List<String> getApplicableMethodNames() {
        return Arrays.asList(
                "ofInt",
                "ofArgb",
                "ofFloat",
                "ofMultiInt",
                "ofMultiFloat",
                "ofObject",
                "ofPropertyValuesHolder");
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @NonNull UCallExpression call,
            @NonNull PsiMethod method) {
        JavaEvaluator evaluator = context.getEvaluator();
        if (!evaluator.isMemberInClass(method, "android.animation.ObjectAnimator") &&
                !(method.getName().equals("ofPropertyValuesHolder")
                    && evaluator.isMemberInClass(method, "android.animation.ValueAnimator"))) {
            return;
        }

        List<UExpression> expressions = call.getValueArguments();
        if (expressions.size() < 2) {
            return;
        }

        PsiType type = TypeEvaluator.evaluate(expressions.get(0));
        if (!(type instanceof PsiClassType)) {
            return;
        }
        PsiClass targetClass = ((PsiClassType) type).resolve();
        if (targetClass == null) {
            return;
        }

        String methodName = method.getName();
        if (methodName.equals("ofPropertyValuesHolder")) {
            if (!evaluator.isMemberInClass(method, "android.animation.ObjectAnimator")) {
                // *Don't* match ValueAnimator.ofPropertyValuesHolder(); for that method,
                // arg0 is another PropertyValuesHolder, *not* the target object!
                return;
            }

            // Try to find the corresponding property value holder initializations
            // and validate each one
            checkPropertyValueHolders(context, targetClass, expressions);
        } else {
            // If "ObjectAnimator#ofObject", look for the type evaluator type in
            // argument at index 2 (third argument)
            String expectedType = getExpectedType(call, 2);
            if (expectedType != null) {
                checkProperty(context, expressions.get(1), targetClass, expectedType);
            }
        }
    }

    @Nullable
    private static String getExpectedType(
            @NonNull UCallExpression method,
            int evaluatorIndex) {
        String methodName = method.getMethodName();

        if (methodName == null) {
            return null;
        }

        switch (methodName) {
            case "ofArgb":
            case "ofInt" : return "int";
            case "ofFloat" : return "float";
            case "ofMultiInt" : return "int[]";
            case "ofMultiFloat" : return "float[]";
            case "ofKeyframe" : return "android.animation.Keyframe";
            case "ofObject" : {
                List<UExpression> args = method.getValueArguments();
                if (args.size() > evaluatorIndex) {
                    PsiType evaluatorType = TypeEvaluator.evaluate(args.get(evaluatorIndex));
                    if (evaluatorType != null) {
                        String typeName = evaluatorType.getCanonicalText();
                        if ("android.animation.FloatEvaluator".equals(typeName)) {
                            return "float";
                        } else if ("android.animation.FloatArrayEvaluator".equals(typeName)) {
                            return "float[]";
                        } else if ("android.animation.IntEvaluator".equals(typeName)
                                || "android.animation.ArgbEvaluator".equals(typeName)) {
                            return "int";
                        } else if ("android.animation.IntArrayEvaluator".equals(typeName)) {
                            return "int[]";
                        } else if ("android.animation.PointFEvaluator".equals(typeName)) {
                            return "android.graphics.PointF";
                        }
                    }
                }
            }
        }

        return null;
    }

    private void checkPropertyValueHolders(
            @NonNull JavaContext context,
            @NonNull PsiClass targetClass,
            @NonNull List<UExpression> expressions) {
        for (int i = 1; i < expressions.size(); i++) { // expressions[0] is the target class
            UExpression arg = expressions.get(i);
            // Find last assignment for each argument; this should be generic
            // infrastructure.
            UCallExpression holder = findHolderConstruction(context, arg);
            if (holder != null) {
                List<UExpression> args = holder.getValueArguments();
                if (args.size() >= 2) {
                    // If "PropertyValueHolder#ofObject", look for the type evaluator type in
                    // argument at index 1 (second argument)
                    String expectedType = getExpectedType(holder, 1);
                    if (expectedType != null) {
                        checkProperty(context, args.get(0), targetClass, expectedType);
                    }
                }
            }
        }
    }

    @Nullable
    private static UCallExpression findHolderConstruction(@NonNull JavaContext context,
            @Nullable UExpression arg) {
        if (arg == null) {
            return null;
        }
        if (arg instanceof UCallExpression) {
            UCallExpression callExpression = (UCallExpression) arg;
            if (isHolderConstructionMethod(context, callExpression)) {
                return callExpression;
            }
            // else: look inside the method and see if it's a method which trivially returns
            // an instance?
        } else if (arg instanceof UReferenceExpression) {
            if (arg instanceof UQualifiedReferenceExpression) {
                UQualifiedReferenceExpression qualified = (UQualifiedReferenceExpression) arg;
                if (qualified.getSelector() instanceof UCallExpression) {
                    UCallExpression selector = (UCallExpression) qualified.getSelector();
                    if (isHolderConstructionMethod(context, selector)) {
                        return selector;
                    }
                }
            }

            // Variable reference? Field reference? etc.
            PsiElement resolved = ((UReferenceExpression) arg).resolve();
            if (resolved instanceof PsiVariable) {
                PsiVariable variable = (PsiVariable) resolved;

                UExpression lastAssignment = UastLintUtils.findLastAssignment(
                        variable, arg);
                // Resolve variable reassignments
                while (lastAssignment instanceof USimpleNameReferenceExpression) {
                    PsiElement el = ((USimpleNameReferenceExpression)lastAssignment).resolve();
                    if (el instanceof PsiLocalVariable) {
                        lastAssignment = UastLintUtils.findLastAssignment(
                                (PsiLocalVariable)el, lastAssignment);
                    } else {
                        break;
                    }
                }

                if (lastAssignment instanceof UCallExpression) {
                    UCallExpression callExpression = (UCallExpression) lastAssignment;
                    if (isHolderConstructionMethod(context, callExpression)) {
                        return callExpression;
                    }
                } else if (lastAssignment instanceof UQualifiedReferenceExpression) {
                    UQualifiedReferenceExpression expression
                            = (UQualifiedReferenceExpression) lastAssignment;
                    if (expression.getSelector() instanceof UCallExpression) {
                        UCallExpression callExpression = (UCallExpression) expression.getSelector();
                        if (isHolderConstructionMethod(context, callExpression)) {
                            return callExpression;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isHolderConstructionMethod(@NonNull JavaContext context,
            @NonNull UCallExpression callExpression) {
        String referenceName = callExpression.getMethodName();
        if (referenceName != null && referenceName.startsWith("of")
                // This will require more indirection; see unit test
                && !referenceName.equals("ofKeyframe")) {
            PsiMethod resolved = callExpression.resolve();
            if (resolved != null && context.getEvaluator().isMemberInClass(resolved,
                    "android.animation.PropertyValuesHolder")) {
                return true;
            }
        }

        return false;
    }

    private void checkProperty(
            @NonNull JavaContext context,
            @NonNull UExpression propertyNameExpression,
            @NonNull PsiClass targetClass,
            @NonNull String expectedType) {
        Object property = ConstantEvaluator.evaluate(context, propertyNameExpression);
        if (!(property instanceof String)) {
            return;
        }
        String propertyName = (String) property;

        String qualifiedName = targetClass.getQualifiedName();
        if (qualifiedName == null || qualifiedName.indexOf('.') == -1) { // resolve error?
            return;
        }

        String methodName = getMethodName("set", propertyName);
        PsiMethod[] methods = targetClass.findMethodsByName(methodName, true);

        PsiMethod bestMethod = null;
        boolean isExactMatch = false;

        for (PsiMethod m : methods) {
            if (m.getParameterList().getParametersCount() == 1) {
                if (bestMethod == null) {
                    bestMethod = m;
                }
                if (context.getEvaluator().parametersMatch(m, expectedType)) {
                    bestMethod = m;
                    isExactMatch = true;
                    break;
                }
            } else if (bestMethod == null) {
                bestMethod = m;
            }
        }

        if (bestMethod == null) {
            report(context, BROKEN_PROPERTY, propertyNameExpression, null,
                    String.format("Could not find property setter method `%1$s` on `%2$s`",
                            methodName, qualifiedName), null);
            return;
        }

        if (!isExactMatch) {
            report(context, BROKEN_PROPERTY, propertyNameExpression, bestMethod,
                    String.format("The setter for this property does not match the "
                                    + "expected signature (`public void %1$s(%2$s arg`)",
                            methodName, expectedType), null);
        } else if (context.getEvaluator().isStatic(bestMethod)) {
            report(context, BROKEN_PROPERTY, propertyNameExpression, bestMethod,
                    String.format("The setter for this property (%1$s.%2$s) should not be static",
                            qualifiedName, methodName), null);
        } else {
            PsiModifierListOwner owner = bestMethod;
            while (owner != null) {
                PsiModifierList modifierList = owner.getModifierList();
                if (modifierList != null) {
                    for (PsiAnnotation annotation : modifierList.getAnnotations()) {
                        if (KEEP_ANNOTATION.equals(annotation.getQualifiedName())) {
                            return;
                        }
                    }
                }
                owner = PsiTreeUtil.getParentOfType(owner, PsiModifierListOwner.class, true);
            }

            // Only flag these warnings if minifyEnabled is true in at least one
            // variant?
            if (!isMinifying(context)) {
                return;
            }

            report(context, MISSING_KEEP, propertyNameExpression, bestMethod, ""
                  + "This method is accessed from an ObjectAnimator so it should be "
                  + "annotated with `@Keep` to ensure that it is not discarded or "
                  + "renamed in release builds", fix().data(bestMethod));
        }
    }

    private void report(
            @NonNull JavaContext context,
            @NonNull Issue issue,
            @NonNull UExpression propertyNameExpression,
            @Nullable PsiMethod method,
            @NonNull String message,
            @Nullable LintFix fix) {
        boolean reportOnMethod = issue == MISSING_KEEP && method != null;

        // No need to report @Keep issues in third party libraries
        if (reportOnMethod && method instanceof PsiCompiledElement) {
            return;
        }

        Object locationNode = reportOnMethod ? method : propertyNameExpression;

        if (mAlreadyWarned != null && mAlreadyWarned.contains(locationNode)) {
            return;
        } else if (mAlreadyWarned == null) {
            mAlreadyWarned = Sets.newIdentityHashSet();
        }
        mAlreadyWarned.add(locationNode);

        Location methodLocation = null;
        if (method != null && !(method instanceof PsiCompiledElement)) {
            methodLocation = method.getNameIdentifier() != null
                    ? context.getRangeLocation(method.getNameIdentifier(), 0, method.getParameterList(), 0)
                    : context.getNameLocation(method);
        }

        Location location;
        if (reportOnMethod) {
            location = methodLocation;
            Location secondary = context.getLocation(propertyNameExpression);
            location.setSecondary(secondary);
            secondary.setMessage("ObjectAnimator usage here");

            // In the same compilation unit, don't show the error on the reference,
            // but in other files (where you may not spot the problem), highlight it.
            if (isInSameCompilationUnit(propertyNameExpression, method)) {
                // Same compilation unit: we don't need to show (in the IDE) the secondary
                // location since we're drawing attention to the keep issue)
                secondary.setVisible(false);
            } else {
                //noinspection ConstantConditions
                assert issue == MISSING_KEEP;
                String secondaryMessage = String.format("The method referenced here (%1$s) has "
                                + "not been annotated with `@Keep` which means it could be "
                                + "discarded or renamed in release builds",
                        method.getName());

                // If on the other hand we're in a separate compilation unit, we should
                // draw attention to the problem
                if (location == Location.NONE) {
                    // When running within the IDE in single file scope the IDE just creates
                    // none-locations for items in other files; in this case make this
                    // the primary locations instead
                    location = secondary;
                    message = secondaryMessage;
                } else {
                    secondary.setMessage(secondaryMessage);
                }
            }
        } else {
            location = context.getNameLocation(propertyNameExpression);
            if (methodLocation != null) {
                location = location.withSecondary(methodLocation, "Property setter here");
            }
        }

        // Allow suppressing at either the property binding site *or* the property site
        // (we report errors on both)
        UElement owner = UastUtils.getParentOfType(propertyNameExpression, UDeclaration.class,
                false);
        if (owner != null && context.getDriver().isSuppressed(context, issue, owner)) {
            return;
        }

        context.report(issue, method, location, message, fix);
    }

    private static boolean isInSameCompilationUnit(
            @NonNull UElement element1,
            @NonNull PsiElement element2) {
        UFile containingFile = UastUtils.getContainingFile(element1);
        PsiFile file = containingFile != null ? containingFile.getPsi() : null;
        return Objects.equals(file, element2.getContainingFile());
    }

    // Copy of PropertyValuesHolder#getMethodName - copy to ensure lint & platform agree
    private static String getMethodName(
            @SuppressWarnings("SameParameterValue") String prefix, String propertyName) {
        //noinspection SizeReplaceableByIsEmpty
        if (propertyName == null || propertyName.length() == 0) {
            // shouldn't get here
            return prefix;
        }
        char firstLetter = Character.toUpperCase(propertyName.charAt(0));
        String theRest = propertyName.substring(1);
        return prefix + firstLetter + theRest;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static boolean isMinifying(@NonNull JavaContext context) {
        Project project = context.getMainProject();
        if (!project.isGradleProject()) {
            // Not a Gradle project: assume project may be using ProGuard/other shrinking
            return true;
        }

        AndroidProject model = project.getGradleProjectModel();
        if (model != null) {
            for (BuildTypeContainer buildTypeContainer : model.getBuildTypes()) {
                if (buildTypeContainer.getBuildType().isMinifyEnabled()) {
                    return true;
                }
            }
        } else {
            // No model? Err on the side of caution.
            return true;
        }

        return false;
    }
}
