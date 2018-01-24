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

import static com.android.SdkConstants.CLASS_VIEW;
import static com.android.tools.lint.checks.JavaPerformanceDetector.ON_DRAW;
import static com.android.tools.lint.checks.JavaPerformanceDetector.ON_LAYOUT;
import static com.android.tools.lint.checks.JavaPerformanceDetector.ON_MEASURE;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.UastScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiMethod;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.USuperExpression;
import org.jetbrains.uast.UastUtils;

/**
 * Checks for cases where the wrong call is being made
 */
public class WrongCallDetector extends Detector implements UastScanner {
    /** Calling the wrong method */
    public static final Issue ISSUE = Issue.create(
            "WrongCall",
            "Using wrong draw/layout method",

            "Custom views typically need to call `measure()` on their children, not `onMeasure`. " +
            "Ditto for onDraw, onLayout, etc.",

            Category.CORRECTNESS,
            6,
            Severity.FATAL,
            new Implementation(
                    WrongCallDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    /** Constructs a new {@link WrongCallDetector} */
    public WrongCallDetector() {
    }

    // ---- Implements UastScanner ----

    @Override
    @Nullable
    public List<String> getApplicableMethodNames() {
        return Arrays.asList(
                ON_DRAW,
                ON_MEASURE,
                ON_LAYOUT
        );
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @NonNull UCallExpression node,
            @NonNull PsiMethod calledMethod) {
        // Call is only allowed if it is both only called on the super class (invoke special)
        // as well as within the same overriding method (e.g. you can't call super.onLayout
        // from the onMeasure method)
        UExpression operand = node.getReceiver();
        if (!(operand instanceof USuperExpression)) {
            report(context, node, calledMethod);
            return;
        }

        PsiMethod method = UastUtils.getParentOfType(node, UMethod.class, true);
        if (method != null) {
            String callName = node.getMethodName();
            if (callName != null && !callName.equals(method.getName())) {
                report(context, node, calledMethod);
            }
        }
    }

    private static void report(
            @NonNull JavaContext context,
            @NonNull UCallExpression node,
            @NonNull PsiMethod method) {
        // Make sure the call is on a view
        if (!context.getEvaluator().isMemberInSubClassOf(method, CLASS_VIEW, false)) {
            return;
        }

        String name = method.getName();
        String suggestion = Character.toLowerCase(name.charAt(2)) + name.substring(3);
        String message = String.format(
                "Suspicious method call; should probably call \"`%1$s`\" rather than \"`%2$s`\"",
                suggestion, name);
        LintFix fix = fix()
                .name("Replace call with " + suggestion + "()").replace().text(name)
                .with(suggestion)
                .build();
        context.report(ISSUE, node, context.getNameLocation(node), message, fix);
    }
}
