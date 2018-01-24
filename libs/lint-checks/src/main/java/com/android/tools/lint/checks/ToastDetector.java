/*
 * Copyright (C) 2012 The Android Open Source Project
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

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.UastScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiMethod;
import java.util.Collections;
import java.util.List;
import org.jetbrains.uast.UBlockExpression;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.ULambdaExpression;
import org.jetbrains.uast.ULiteralExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.UReturnExpression;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

/** Detector looking for Toast.makeText() without a corresponding show() call */
public class ToastDetector extends Detector implements UastScanner {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "ShowToast",
            "Toast created but not shown",

            "`Toast.makeText()` creates a `Toast` but does **not** show it. You must call " +
            "`show()` on the resulting object to actually make the `Toast` appear.",

            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            new Implementation(
                    ToastDetector.class,
                    Scope.JAVA_FILE_SCOPE));


    /** Constructs a new {@link ToastDetector} check */
    public ToastDetector() {
    }

    // ---- Implements UastScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList("makeText");
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @NonNull UCallExpression call,
            @NonNull PsiMethod method) {
        if (!context.getEvaluator().isMemberInClass(method, "android.widget.Toast")) {
            return;
        }

        // Make sure you pass the right kind of duration: it's not a delay, it's
        //  LENGTH_SHORT or LENGTH_LONG
        // (see http://code.google.com/p/android/issues/detail?id=3655)
        List<UExpression> args = call.getValueArguments();
        if (args.size() == 3) {
            UExpression duration = args.get(2);
            if (duration instanceof ULiteralExpression) {
                context.report(ISSUE, duration, context.getLocation(duration),
                        "Expected duration `Toast.LENGTH_SHORT` or `Toast.LENGTH_LONG`, a custom " +
                        "duration value is not supported");
            }
        }

        @SuppressWarnings("unchecked")
        UElement surroundingDeclaration = UastUtils.getParentOfType(
                call, true,
                UMethod.class, UBlockExpression.class, ULambdaExpression.class);

        if (surroundingDeclaration == null) {
            return;
        }

        UElement parent = call.getUastParent();
        if (parent instanceof UMethod || parent instanceof UReferenceExpression &&
                parent.getUastParent() instanceof UMethod) {
            // Kotlin expression body
            return;
        }

        ShowFinder finder = new ShowFinder(call);
        surroundingDeclaration.accept(finder);
        if (!finder.isShowCalled()) {
            context.report(ISSUE, call, context.getCallLocation(call, true, false),
                    "Toast created but not shown: did you forget to call `show()` ?");
        }
    }

    private static class ShowFinder extends AbstractUastVisitor {
        /** The target makeText call */
        private final UCallExpression target;
        /** Whether we've found the show method */
        private boolean found;
        /** Whether we've seen the target makeText node yet */
        private boolean seenTarget;

        private ShowFinder(UCallExpression target) {
            this.target = target;
        }

        @Override
        public boolean visitCallExpression(UCallExpression node) {
            if (node == target || node.getPsi() != null && node.getPsi() == target.getPsi()) {
                seenTarget = true;
            } else {
                if ((seenTarget || target.equals(node.getReceiver()))
                        && "show".equals(node.getMethodName())) {
                    // TODO: Do more flow analysis to see whether we're really calling show
                    // on the right type of object?
                    found = true;
                }
            }
            return super.visitCallExpression(node);
        }

        @Override
        public boolean visitReturnExpression(UReturnExpression node) {
            if (UastUtils.isChildOf(target, node.getReturnExpression(), true)) {
                // If you just do "return Toast.makeText(...) don't warn
                found = true;
            }
            return super.visitReturnExpression(node);
        }

        boolean isShowCalled() {
            return found;
        }
    }
}
