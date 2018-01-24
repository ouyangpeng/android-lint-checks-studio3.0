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

import com.android.tools.lint.detector.api.Detector;

public class IncludeDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new IncludeDetector();
    }

    public void test() throws Exception {
        String expected = ""
                + "res/layout/include_params.xml:43: Error: Layout parameter layout_margin ignored unless both layout_width and layout_height are also specified on <include> tag [IncludeLayoutParam]\n"
                + "        android:layout_margin=\"20dp\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/include_params.xml:44: Error: Layout parameter layout_weight ignored unless both layout_width and layout_height are also specified on <include> tag [IncludeLayoutParam]\n"
                + "        android:layout_weight=\"1.5\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/include_params.xml:51: Error: Layout parameter layout_weight ignored unless layout_width is also specified on <include> tag [IncludeLayoutParam]\n"
                + "        android:layout_weight=\"1.5\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/include_params.xml:58: Error: Layout parameter layout_weight ignored unless layout_height is also specified on <include> tag [IncludeLayoutParam]\n"
                + "        android:layout_weight=\"1.5\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/include_params.xml:65: Error: Layout parameter layout_width ignored unless layout_height is also specified on <include> tag [IncludeLayoutParam]\n"
                + "            android:layout_width=\"fill_parent\"\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/include_params.xml:72: Error: Layout parameter layout_height ignored unless layout_width is also specified on <include> tag [IncludeLayoutParam]\n"
                + "            android:layout_height=\"fill_parent\"\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "6 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/include_params.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:orientation=\"vertical\">\n"
                        + "\n"
                        + "    <!-- Ok: No layout params -->\n"
                        + "    <include layout=\"@layout/myincluded\" />\n"
                        + "\n"
                        + "    <!-- Ok: No layout params -->\n"
                        + "    <include\n"
                        + "        android:id=\"@+id/myInclude\"\n"
                        + "        layout=\"@layout/myincluded\"\n"
                        + "        android:gravity=\"left\"\n"
                        + "        android:visibility=\"visible\" />\n"
                        + "\n"
                        + "    <!-- Ok: No layout params -->\n"
                        + "    <include\n"
                        + "        layout=\"@layout/myincluded\"\n"
                        + "        android:layout_width=\"fill_parent\"\n"
                        + "        android:layout_height=\"fill_parent\" />\n"
                        + "\n"
                        + "    <!-- Ok: Specifies both width and height -->\n"
                        + "    <include\n"
                        + "        layout=\"@layout/myincluded\"\n"
                        + "        android:layout_width=\"fill_parent\"\n"
                        + "        android:layout_height=\"0dp\"\n"
                        + "        android:layout_weight=\"1.5\"\n"
                        + "        android:visibility=\"visible\" />\n"
                        + "\n"
                        + "    <!-- Ok: ignored -->\n"
                        + "    <include\n"
                        + "        layout=\"@layout/myincluded\"\n"
                        + "        android:layout_width=\"fill_parent\"\n"
                        + "        android:layout_weight=\"1.5\"\n"
                        + "        android:visibility=\"visible\"\n"
                        + "        tools:ignore=\"IncludeLayoutParam\" />\n"
                        + "\n"
                        + "    <!-- Wrong: Missing both -->\n"
                        + "    <include\n"
                        + "        layout=\"@layout/myincluded\"\n"
                        + "        android:layout_margin=\"20dp\"\n"
                        + "        android:layout_weight=\"1.5\"\n"
                        + "        android:visibility=\"visible\" />\n"
                        + "\n"
                        + "    <!-- Wrong: Missing width -->\n"
                        + "    <include\n"
                        + "        layout=\"@layout/myincluded\"\n"
                        + "        android:layout_height=\"0dp\"\n"
                        + "        android:layout_weight=\"1.5\"\n"
                        + "        android:visibility=\"visible\" />\n"
                        + "\n"
                        + "    <!-- Wrong: Missing height -->\n"
                        + "    <include\n"
                        + "        layout=\"@layout/myincluded\"\n"
                        + "        android:layout_width=\"fill_parent\"\n"
                        + "        android:layout_weight=\"1.5\"\n"
                        + "        android:visibility=\"visible\" />\n"
                        + "\n"
                        + "    <!-- Wrong: Specified only width -->\n"
                        + "    <include\n"
                        + "            android:id=\"@+id/myInclude\"\n"
                        + "            layout=\"@layout/myincluded\"\n"
                        + "            android:layout_width=\"fill_parent\"\n"
                        + "            android:visibility=\"visible\" />\n"
                        + "\n"
                        + "    <!-- Wrong: Specified only height -->\n"
                        + "    <include\n"
                        + "            android:id=\"@+id/myInclude\"\n"
                        + "            layout=\"@layout/myincluded\"\n"
                        + "            android:layout_height=\"fill_parent\"\n"
                        + "            android:visibility=\"visible\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"))
                .run()
                .expect(expected)
                .verifyFixes()
                .expectFixDiffs(""
                        + "Fix for res/layout/include_params.xml line 42: Set layout_width:\n"
                        + "@@ -48 +48\n"
                        + "+         android:layout_width=\"|\"\n"
                        + "Fix for res/layout/include_params.xml line 42: Set layout_height:\n"
                        + "@@ -48 +48\n"
                        + "+         android:layout_height=\"|\"\n"
                        + "Fix for res/layout/include_params.xml line 43: Set layout_width:\n"
                        + "@@ -48 +48\n"
                        + "+         android:layout_width=\"|\"\n"
                        + "Fix for res/layout/include_params.xml line 43: Set layout_height:\n"
                        + "@@ -48 +48\n"
                        + "+         android:layout_height=\"|\"\n"
                        + "Fix for res/layout/include_params.xml line 50: Set layout_width:\n"
                        + "@@ -56 +56\n"
                        + "+         android:layout_width=\"|\"\n"
                        + "Fix for res/layout/include_params.xml line 57: Set layout_height:\n"
                        + "@@ -65 +65\n"
                        + "+         android:layout_height=\"|\"\n"
                        + "Fix for res/layout/include_params.xml line 64: Set layout_height:\n"
                        + "@@ -74 +74\n"
                        + "+         android:layout_height=\"|\"\n"
                        + "Fix for res/layout/include_params.xml line 71: Set layout_width:\n"
                        + "@@ -81 +81\n"
                        + "+         android:layout_width=\"|\"\n");
    }
}