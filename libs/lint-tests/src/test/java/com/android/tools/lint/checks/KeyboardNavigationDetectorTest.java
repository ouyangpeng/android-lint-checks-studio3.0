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

import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings("javadoc")
public class KeyboardNavigationDetectorTest extends AbstractCheckTest {

    private static final String DUMMY_FILE_NAME = "res/layout/mywidget.xml";
    private static final String EXPECTED_WARNING_PREFIX = "res/layout/mywidget.xml:2: Warning: "
        + KeyboardNavigationDetector.MESSAGE
        + " [KeyboardInaccessibleWidget]\n";

    @Override
    protected Detector getDetector() {
        return new KeyboardNavigationDetector();
    }

    public void testFocusableElement_noIssue() throws Exception {
        lint().files(xml(DUMMY_FILE_NAME, ""
            + "<Button xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:clickable=\"true\"\n"
            + "    android:focusable=\"true\" />"))
            .run()
            .expectClean();
    }

    public void testNonClickableElement_noIssue() throws Exception {
        lint().files(xml(DUMMY_FILE_NAME, ""
             + "<Button xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
             + "    android:clickable=\"false\"\n"
             + "    android:focusable=\"false\" />"))
            .run()
            .expectClean();
    }

    public void testUnspecifiedFocusableElement_triggersIssue() throws Exception {
        lint().files(xml(DUMMY_FILE_NAME, ""
            + "<Button xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:clickable=\"true\" />"))
            .run()
            .expect(EXPECTED_WARNING_PREFIX
            + "    android:clickable=\"true\" />\n"
            + "    ~~~~~~~~~~~~~~~~~~~~~~~~\n"
            + "0 errors, 1 warnings\n")
            .expectFixDiffs(""
            + "Fix for res/layout/mywidget.xml line 1: Set focusable=\"true\":\n"
            + "@@ -3 +3\n"
            + "-     android:clickable=\"true\" />\n"
            + "@@ -4 +3\n"
            + "+     android:clickable=\"true\"\n"
            + "+     android:focusable=\"true\" />\n");
    }

    public void testUnfocusableElement_triggersIssue() throws Exception {
        lint().files(xml(DUMMY_FILE_NAME, ""
            + "<Button xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:clickable=\"true\"\n"
            + "    android:focusable=\"false\" />"))
            .run()
            .expect(EXPECTED_WARNING_PREFIX
            + "    android:clickable=\"true\"\n"
            + "    ~~~~~~~~~~~~~~~~~~~~~~~~\n"
            + "0 errors, 1 warnings\n");
    }
}
