/*
 * Copyright (C) 2017 The Android Open Source Project
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
import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.checks.infrastructure.TestLintTask;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import java.util.Collections;
import java.util.List;

/** Makes sure that legacy PSI detectors are called from lint testing infrastructure */
@SuppressWarnings("MethodMayBeStatic")
public class CompatibilityCheckTest extends LintDetectorTest { // NOTE: Not AbstractCheckTest
    public static final Issue TEST_ISSUE = Issue.create(
            "CompatibilityCheckTest",
            "Not applicable",

            "Not applicable",

            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            new Implementation(
                    CompatibilityDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    @Override
    protected Detector getDetector() {
        return new CompatibilityDetector();
    }

    @Override
    protected List<Issue> getIssues() {
        return Collections.singletonList(TEST_ISSUE);
    }

    public void testInvoked() throws Exception {
        String expected = ""
                + "src/test/pkg/Test.java:5: Warning: Detector appears to be used [CompatibilityCheckTest]\n"
                + "    public void test() { foo(); }\n"
                + "                         ~~~~~\n"
                + "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        TestLintTask.lint().files( // Bypassing AbstractCheckTest.lint() to avoid registration chk
                java(""
                    + "package test.pkg;\n"
                    + "\n"
                    + "public class Test {\n"
                        + "    public void foo() { }\n"
                        + "    public void test() { foo(); }\n"
                    + "}\n"))
                .allowCompilationErrors(true)
                .allowSystemErrors(true)
                .allowMissingSdk(true)
                .issues(TEST_ISSUE)
                .run()
                .expect(expected);
    }

    @SuppressWarnings("deprecation")
    public static class CompatibilityDetector extends Detector
            implements Detector.JavaPsiScanner {

        @Override
        public void visitMethod(@NonNull JavaContext context, @Nullable JavaElementVisitor visitor,
                @NonNull PsiMethodCallExpression call, @NonNull PsiMethod method) {
            context.report(TEST_ISSUE, call, context.getLocation(call),
                    "Detector appears to be used");
        }

        @Nullable
        @Override
        public List<String> getApplicableMethodNames() {
            return Collections.singletonList("foo");
        }
    }
}
