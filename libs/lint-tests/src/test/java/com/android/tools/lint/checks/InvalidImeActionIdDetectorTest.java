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
public class InvalidImeActionIdDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new InvalidImeActionIdDetector();
    }

    public void testNoWarnings() throws Exception {
        //noinspection all // Sample code
        assertEquals("No warnings.",

                lintFiles(xml("res/layout/namespace.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\" android:orientation=\"vertical\">\n"
                        + "    <EditText android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\" android:imeActionId=\"6\"/>\n"
                        + "</LinearLayout>\n"
                        + "\n")));
    }

    public void testInvalidResourceType() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/namespace.xml:3: Error: Invalid resource type, expected integer value [InvalidImeActionId]\n"
                + "    <EditText android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\" android:imeActionId=\"@+id/login\"/>\n"
                + "                                                                                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintFiles(xml("res/layout/namespace.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\" android:orientation=\"vertical\">\n"
                            + "    <EditText android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\" android:imeActionId=\"@+id/login\"/>\n"
                            + "</LinearLayout>\n"
                            + "\n")));
    }

    public void testInvalidResourceValue() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                        + "res/layout/namespace.xml:3: Error: \"mmm\" is not an integer [InvalidImeActionId]\n"
                        + "    <EditText android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\" android:imeActionId=\"mmm\"/>\n"
                        + "                                                                                       ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n",

                lintFiles(xml("res/layout/namespace.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\" android:orientation=\"vertical\">\n"
                        + "    <EditText android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\" android:imeActionId=\"mmm\"/>\n"
                        + "</LinearLayout>\n"
                        + "\n")));
    }
}
