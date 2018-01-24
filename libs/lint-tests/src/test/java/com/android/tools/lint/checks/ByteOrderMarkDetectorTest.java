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

@SuppressWarnings("javadoc")
public class ByteOrderMarkDetectorTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new ByteOrderMarkDetector();
    }

    public void testXml() throws Exception {
        // See issue b.android.com/65103
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<manifest package='foo.\ufeffbar'>\n"
                        + "</manifest>"),
                xml("res/values-zh-rCN/bom.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "\t<string name=\"hanping_chinese\ufeff_lite\ufeff_app_name\">(Translated name)</string>\n"
                        + "\t<string tools:ignore='ByteOrderMark' name=\"something\">test\ufefftest2</string>\n"
                        + "</resources>\n"),
                java(""
                        + "package test.pkg;\n"
                        + "import android.annotation.SuppressLint;\n"
                        + "public class MyTest {\n"
                        + "    public void test1() {\n"
                        // source code is allowed to reference byteorder mark indirectly
                        + "        String s = \"\\uFEFF\"; // OK\n"
                        + "        String t = \"\uFEFF\"; // ERROR\n"
                        + "    }\n"
                        + "    @SuppressLint(\"ByteOrderMark\")\n"
                        + "    public void test2() {\n"
                        + "        String s = \"\uFEFF\"; //OK/suppressed\n"
                        + "    }\n"
                        + "}\n"),
                source("proguard.cfg", ""
                        + "-optimizationpasses\uFEFF 5\n"
                        + "-dontusemixedcaseclassnames\n"
                        + "-dontskipnonpubliclibraryclasses\n"
                        + "-dontpreverify\n"
                        + "-verbose\n"))
                .run()
                .expect(""
                        + "AndroidManifest.xml:1: Error: Found byte-order-mark in the middle of a file [ByteOrderMark]\n"
                        + "<manifest package='foo.\uFEFFbar'>\n"
                        + "                       ~\n"
                        + "src/test/pkg/MyTest.java:6: Error: Found byte-order-mark in the middle of a file [ByteOrderMark]\n"
                        + "        String t = \"\uFEFF\"; // ERROR\n"
                        + "                    ~\n"
                        + "res/values-zh-rCN/bom.xml:3: Error: Found byte-order-mark in the middle of a file [ByteOrderMark]\n"
                        + " <string name=\"hanping_chinese\uFEFF_lite\uFEFF_app_name\">(Translated name)</string>\n"
                        + "                              ~\n"
                        + "res/values-zh-rCN/bom.xml:3: Error: Found byte-order-mark in the middle of a file [ByteOrderMark]\n"
                        + " <string name=\"hanping_chinese\uFEFF_lite\uFEFF_app_name\">(Translated name)</string>\n"
                        + "                                    ~\n"
                        + "proguard.cfg:1: Error: Found byte-order-mark in the middle of a file [ByteOrderMark]\n"
                        + "-optimizationpasses\uFEFF 5\n"
                        + "                   ~\n"
                        + "5 errors, 0 warnings\n");
    }
}
