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
public class ChildCountDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new ChildCountDetector();
    }

    public void testChildCount() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/has_children.xml:3: Warning: A list/grid should have no children declared in XML [AdapterViewChildren]\n"
                + "<ListView\n"
                + "^\n"
                + "0 errors, 1 warnings\n",
            lintFiles(xml("res/layout/has_children.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "\n"
                            + "<ListView\n"
                            + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\">\n"
                            + "\n"
                            + "\t<ListView\n"
                            + "\t    android:layout_width=\"match_parent\"\n"
                            + "\t    android:layout_height=\"match_parent\" />\n"
                            + "\n"
                            + "</ListView>\n")));
    }

    public void testChildCount2() throws Exception {
        // A <requestFocus/> tag is okay.
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",
                lintFiles(xml("res/layout/has_children2.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "\n"
                            + "<ListView\n"
                            + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\">\n"
                            + "\n"
                            + "        <requestFocus/>\n"
                            + "\n"
                            + "</ListView>\n")));
    }
}
