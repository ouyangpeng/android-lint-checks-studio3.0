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
public class MergeMarkerDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new MergeMarkerDetector();
    }

    public void testMergeMarker() throws Exception {
        assertEquals(""
                + "src/test/pkg/Test.java:2: Error: Missing merge marker? [MergeMarker]\n"
                + "<<<<<<< HEAD\n"
                + "~~~~~~~\n"
                + "src/test/pkg/Test.java:4: Error: Missing merge marker? [MergeMarker]\n"
                + "=======\n"
                + "~~~~~~~\n"
                + "src/test/pkg/Test.java:5: Error: Missing merge marker? [MergeMarker]\n"
                + ">>>>>>> branch-a\n"
                + "~~~~~~~\n"
                + "res/values/strings.xml:5: Error: Missing merge marker? [MergeMarker]\n"
                + "<<<<<<< HEAD\n"
                + "~~~~~~~\n"
                + "res/values/strings.xml:7: Error: Missing merge marker? [MergeMarker]\n"
                + "=======\n"
                + "~~~~~~~\n"
                + "res/values/strings.xml:9: Error: Missing merge marker? [MergeMarker]\n"
                + ">>>>>>> branch-a    \n"
                + "~~~~~~~\n"
                + "6 errors, 0 warnings\n",
                lintProject(
                        source("src/test/pkg/Test.java", ""
                                + "package test.pkg;\n"
                                + "<<<<<<< HEAD\n"
                                + "import java.util.List;\n"
                                + "=======\n"
                                + ">>>>>>> branch-a\n"
                                + "class Test {\n"
                                + "    PrintTitle('<<<<<<< ', 'X509 certificate chain', name)\n"
                                + "}"),
                        source("res/values/strings.xml", ""
                                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                + "<resources>\n"
                                + "\n"
                                + "    <string name=\"app_name\">LibraryProject</string>\n"
                                + "<<<<<<< HEAD\n"
                                + "    <string name=\"string1\">String 1</string>\n"
                                + "=======\n"
                                + "    <string name=\"string2\">String 2</string>\n"
                                + ">>>>>>> branch-a    \n"
                                + "    <string name=\"string3\">String 3</string>\n"
                                + "\n"
                                + "</resources>\n"),
                        // Make sure we don't try to read binary contents
                        source("res/drawable-mdpi/my_icon.png", ""
                                + "<<<<<<< HEAD\n"
                                + "=======\n"
                                + ">>>>>>> branch-a    \n"
                                + "</resources>\n")

                ));
    }
}
