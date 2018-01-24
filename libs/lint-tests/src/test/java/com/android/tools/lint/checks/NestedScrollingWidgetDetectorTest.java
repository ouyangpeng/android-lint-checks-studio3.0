/*
 * Copyright (C) 2011 The Android Open Source Project
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
public class NestedScrollingWidgetDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new NestedScrollingWidgetDetector();
    }

    public void testNested() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/scrolling.xml:13: Warning: The vertically scrolling ScrollView should not contain another vertically scrolling widget (ListView) [NestedScrolling]\n"
                + "  <ListView\n"
                + "  ^\n"
                + "0 errors, 1 warnings\n",

            lintFiles(xml("res/layout/scrolling.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "\n"
                            + "<ScrollView\n"
                            + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\">\n"
                            + "\n"
                            + "\t<LinearLayout\n"
                            + "\t    android:layout_width=\"match_parent\"\n"
                            + "\t    android:layout_height=\"match_parent\">\n"
                            + "\n"
                            + "\t\t<ListView\n"
                            + "\t\t    android:layout_width=\"match_parent\"\n"
                            + "\t\t    android:layout_height=\"match_parent\" />\n"
                            + "\n"
                            + "\t</LinearLayout>\n"
                            + "\n"
                            + "</ScrollView>\n")));
    }
}
