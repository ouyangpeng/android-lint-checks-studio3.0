/*
 * Copyright (C) 2016 The Android Open Source Project
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
public class AllCapsDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new AllCapsDetector();
    }

    public void testAllCaps() throws Exception {
        assertEquals(""
                + "res/layout/constraint.xml:12: Warning: Using textAllCaps with a string (has_markup) that contains markup; the markup will be dropped by the caps conversion [AllCaps]\n"
                + "        android:textAllCaps=\"true\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",
                lintProjectIncrementally(
                        "res/layout/constraint.xml",
                        xml("res/layout/constraint.xml", ""
                                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                + "<merge xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                                + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\">\n"
                                + "    <Button\n"
                                + "        android:text=\"@string/plain\"\n"
                                + "        android:textAllCaps=\"true\"\n"
                                + "        android:layout_width=\"wrap_content\"\n"
                                + "        android:layout_height=\"wrap_content\" />\n"
                                + "    <Button\n"
                                + "        android:text=\"@string/has_markup\"\n"
                                + "        android:textAllCaps=\"true\"\n"
                                + "        android:layout_width=\"wrap_content\"\n"
                                + "        android:layout_height=\"wrap_content\" />\n"
                                + "    <Button\n"
                                + "        android:text=\"@string/has_markup\"\n"
                                + "        android:textAllCaps=\"false\"\n"
                                + "        android:layout_width=\"wrap_content\"\n"
                                + "        android:layout_height=\"wrap_content\" />\n"
                                + "    <Button\n"
                                + "        android:text=\"@string/has_markup\"\n"
                                + "        android:textAllCaps=\"true\"\n"
                                + "        tools:ignore=\"AllCaps\"\n"
                                + "        android:layout_width=\"wrap_content\"\n"
                                + "        android:layout_height=\"wrap_content\" />\n"
                                + "</merge>"),
                        xml("res/values/strings.xml", ""
                                + "<resources>\n"
                                + "    <string name=\"plain\">Home Sample</string>\n"
                                + "    <string name=\"has_markup\">This is <b>bold</b></string>\n"
                                + "</resources>")));
    }
}
