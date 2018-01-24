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
public class MergeRootFrameLayoutDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new MergeRootFrameLayoutDetector();
    }

    public void testMergeRefFromJava() throws Exception {
        String expected = ""
                + "res/layout/simple.xml:3: Warning: This <FrameLayout> can be replaced with a <merge> tag [MergeRootFrame]\n"
                + "<FrameLayout\n"
                + "^\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                mSimple,
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.os.Bundle;\n"
                        + "\n"
                        + "public class ImportFrameActivity extends Activity {\n"
                        + "    @Override\n"
                        + "    public void onCreate(Bundle savedInstanceState) {\n"
                        + "        super.onCreate(savedInstanceState);\n"
                        + "        setContentView(R.layout.simple);\n"
                        + "    }\n"
                        + "}\n"),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "public final class R {\n"
                        + "    public static final class layout {\n"
                        + "        public static final int simple = 0x7f0a0000;\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expect(expected);
    }

    public void testMergeRefFromInclude() throws Exception {
        String expected = ""
                + "res/layout/simple.xml:3: Warning: This <FrameLayout> can be replaced with a <merge> tag [MergeRootFrame]\n"
                + "<FrameLayout\n"
                + "^\n"
                + "0 errors, 1 warnings\n";
        lint().files(mSimple, mSimpleinclude).run().expect(expected);
    }

    public void testMergeRefFromIncludeSuppressed() throws Exception {
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/simple.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "\n"
                        + "<FrameLayout\n"
                        + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    tools:ignore=\"MergeRootFrame\" />\n"),
                mSimpleinclude)
                .run()
                .expectClean();
    }

    public void testNotIncluded() throws Exception {
        lint().files(mSimple).run().expectClean();
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mSimple = xml("res/layout/simple.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "\n"
            + "<FrameLayout\n"
            + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "\n"
            + "    android:layout_width=\"match_parent\"\n"
            + "    android:layout_height=\"match_parent\" />\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mSimpleinclude = xml("res/layout/simpleinclude.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:layout_width=\"match_parent\"\n"
            + "    android:layout_height=\"match_parent\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "    <include\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        layout=\"@layout/simple\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:id=\"@+id/button1\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:id=\"@+id/button2\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "</LinearLayout>\n");
}
