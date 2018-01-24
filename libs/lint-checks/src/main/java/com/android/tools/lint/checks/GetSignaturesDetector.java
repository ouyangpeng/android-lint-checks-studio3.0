/*
 * Copyright (C) 2014 The Android Open Source Project
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
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ConstantEvaluator;
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
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;

public class GetSignaturesDetector extends Detector implements UastScanner  {
    public static final Issue ISSUE = Issue.create(
            "PackageManagerGetSignatures",
            "Potential Multiple Certificate Exploit",
            "Improper validation of app signatures could lead to issues where a malicious app " +
            "submits itself to the Play Store with both its real certificate and a fake " +
            "certificate and gains access to functionality or information it shouldn't " +
            "have due to another application only checking for the fake certificate and " +
            "ignoring the rest. Please make sure to validate all signatures returned " +
            "by this method.",
            Category.SECURITY,
            8,
            Severity.INFORMATIONAL,
            new Implementation(
                    GetSignaturesDetector.class,
                    Scope.JAVA_FILE_SCOPE))
            .addMoreInfo("https://bluebox.com/technical/android-fake-id-vulnerability/");

    private static final String PACKAGE_MANAGER_CLASS = "android.content.pm.PackageManager";
    private static final String GET_PACKAGE_INFO = "getPackageInfo";
    private static final int GET_SIGNATURES_FLAG = 0x00000040;

    // ---- Implements UastScanner ----

    @Override
    @Nullable
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList(GET_PACKAGE_INFO);
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @NonNull UCallExpression node,
            @NonNull PsiMethod method) {
        JavaEvaluator evaluator = context.getEvaluator();
        if (!evaluator.methodMatches(method, PACKAGE_MANAGER_CLASS, true,
                JavaParser.TYPE_STRING,
                JavaParser.TYPE_INT)) {
            return;
        }

        List<UExpression> arguments = node.getValueArguments();
        if (arguments.size() == 2) {
            UExpression second = arguments.get(1);
            Object number = ConstantEvaluator.evaluate(context, second);
            if (number instanceof Number) {
                int flagValue = ((Number)number).intValue();
                maybeReportIssue(flagValue, context, node, second);
            }
        }
    }

    private static void maybeReportIssue(
            int flagValue, JavaContext context, UCallExpression node,
            UExpression last) {
        if ((flagValue & GET_SIGNATURES_FLAG) != 0) {
            context.report(ISSUE, node, context.getLocation(last),
                "Reading app signatures from getPackageInfo: The app signatures "
                    + "could be exploited if not validated properly; "
                    + "see issue explanation for details.");
        }
    }
}
