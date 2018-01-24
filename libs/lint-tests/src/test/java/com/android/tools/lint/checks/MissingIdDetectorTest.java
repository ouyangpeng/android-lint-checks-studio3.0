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

import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings("javadoc")
public class MissingIdDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new MissingIdDetector();
    }

    public void test() {
        String expected = ""
                + "res/layout/fragment.xml:7: Warning: This <fragment> tag should specify an id or a tag to preserve state across activity restarts [MissingId]\n"
                + "    <fragment\n"
                + "    ^\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/fragment.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "    <fragment\n"
                        + "        android:name=\"android.app.ListFragment\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\" />\n"
                        + "\n"
                        + "    <fragment\n"
                        + "        android:name=\"android.app.DialogFragment\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:tag=\"mytag\" />\n"
                        + "\n"
                        + "    <fragment\n"
                        + "        android:id=\"@+id/fragment3\"\n"
                        + "        android:name=\"android.preference.PreferenceFragment\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"))
                .run()
                .expect(expected)
                .verifyFixes().window(1).expectFixDiffs(""
                + "Fix for res/layout/fragment.xml line 6: Set id:\n"
                + "@@ -8 +8\n"
                + "      <fragment\n"
                + "+         android:id=\"@+id/[TODO]|\"\n"
                + "          android:name=\"android.app.ListFragment\"\n");
    }
}
