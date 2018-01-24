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

package com.android.tools.lint;

import com.android.tools.lint.checks.AbstractCheckTest;
import com.android.tools.lint.checks.HardcodedValuesDetector;
import com.android.tools.lint.detector.api.Detector;
import com.intellij.codeInsight.CustomExceptionHandler;
import com.intellij.openapi.extensions.Extensions;

public class LintCliClientTest extends AbstractCheckTest {
    public void testUnknownId() {
        lint().files(
                gradle(""
                        + "\n"
                        + "android {\n"
                        + "    lintOptions {\n"
                        + "        // Let's disable UnknownLintId\n"
                        + "        /* Let's disable UnknownLintId */\n"
                        + "        check 'HardcodedText', 'UnknownLintId'\n"
                        + "    }\n"
                        + "}\n"))
                .issues(HardcodedValuesDetector.ISSUE)
                .allowSystemErrors(true)
                .run()
                .expect(""
                        + "build.gradle:6: Error: Unknown issue id \"UnknownLintId\" [LintError]\n"
                        + "        check 'HardcodedText', 'UnknownLintId'\n"
                        + "                                ~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n");
    }

    public void testMissingExtensionPoints() {
        LintCoreApplicationEnvironment.get();
        // Regression test for 37817771
        Extensions.getExtensions(CustomExceptionHandler.KEY);
    }

    @Override
    protected Detector getDetector() {
        return new HardcodedValuesDetector();
    }
}