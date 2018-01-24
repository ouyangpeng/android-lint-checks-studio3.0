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
public class CommentDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new CommentDetector();
    }

    public void testJava() throws Exception {
        String expected = ""
                + "src/test/pkg/Hidden.java:11: Warning: STOPSHIP comment found; points to code which must be fixed prior to release [StopShip]\n"
                + "    // STOPSHIP\n"
                + "       ~~~~~~~~\n"
                + "src/test/pkg/Hidden.java:12: Warning: STOPSHIP comment found; points to code which must be fixed prior to release [StopShip]\n"
                + "    /* We must STOPSHIP! */\n"
                + "               ~~~~~~~~\n"
                + "src/test/pkg/Hidden.java:5: Warning: Code might be hidden here; found unicode escape sequence which is interpreted as comment end, compiled code follows [EasterEgg]\n"
                + "    /* \\u002a\\u002f static { System.out.println(\"I'm executed on class load\"); } \\u002f\\u002a */\n"
                + "       ~~~~~~~~~~~~\n"
                + "src/test/pkg/Hidden.java:6: Warning: Code might be hidden here; found unicode escape sequence which is interpreted as comment end, compiled code follows [EasterEgg]\n"
                + "    /* \\u002A\\U002F static { System.out.println(\"I'm executed on class load\"); } \\u002f\\u002a */\n"
                + "       ~~~~~~~~~~~~\n"
                + "0 errors, 4 warnings\n";
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "public class Hidden {\n"
                        + "    // Innocent comment...?\n"
                        + "    /* \\u002a\\u002f static { System.out.println(\"I'm executed on class load\"); } \\u002f\\u002a */\n"
                        + "    /* \\u002A\\U002F static { System.out.println(\"I'm executed on class load\"); } \\u002f\\u002a */\n"
                        + "    /* Normal \\\\u002A\\U002F */ // OK\n"
                        + "    static {\n"
                        + "        String s = \"\\u002a\\u002f\"; // OK\n"
                        + "    }\n"
                        + "    // STOPSHIP\n"
                        + "    /* We must STOPSHIP! */\n"
                        + "    String x = \"STOPSHIP\"; // OK\n"
                        + "}\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for src/test/pkg/Hidden.java line 10: Remove STOPSHIP:\n"
                        + "@@ -11 +11\n"
                        + "-     // STOPSHIP\n"
                        + "+     // \n"
                        + "Fix for src/test/pkg/Hidden.java line 11: Remove STOPSHIP:\n"
                        + "@@ -12 +12\n"
                        + "-     /* We must STOPSHIP! */\n"
                        + "+     /* We must ! */\n");
    }

    public void test2() throws Exception {
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import java.io.File;\n"
                        + "\n"
                        + "import android.content.Intent;\n"
                        + "import android.net.Uri;\n"
                        + "\n"
                        + "/**\n"
                        + " * Ignore comments - create(\"/sdcard/foo\")\n"
                        + " */\n"
                        + "public class SdCardTest {\n"
                        + "\tprivate static final boolean PROFILE_STARTUP = true;\n"
                        + "\tprivate static final String SDCARD_TEST_HTML = \"/sdcard/test.html\";\n"
                        + "\tpublic static final String SDCARD_ROOT = \"/sdcard\";\n"
                        + "\tpublic static final String PACKAGES_PATH = \"/sdcard/o/packages/\";\n"
                        + "\tFile deviceDir = new File(\"/sdcard/vr\");\n"
                        + "\n"
                        + "\tpublic SdCardTest() {\n"
                        + "\t\tif (PROFILE_STARTUP) {\n"
                        + "\t\t\tandroid.os.Debug.startMethodTracing(\"/sdcard/launcher\");\n"
                        + "\t\t}\n"
                        + "\t\tif (new File(\"/sdcard\").exists()) {\n"
                        + "\t\t}\n"
                        + "\t\tString FilePath = \"/sdcard/\" + new File(\"test\");\n"
                        + "\t\tSystem.setProperty(\"foo.bar\", \"file://sdcard\");\n"
                        + "\n"
                        + "\n"
                        + "\t\tIntent intent = new Intent(Intent.ACTION_PICK);\n"
                        + "\t\tintent.setDataAndType(Uri.parse(\"file://sdcard/foo.json\"), \"application/bar-json\");\n"
                        + "\t\tintent.putExtra(\"path-filter\", \"/sdcard(/.+)*\");\n"
                        + "\t\tintent.putExtra(\"start-dir\", \"/sdcard\");\n"
                        + "\t\tString mypath = \"/data/data/foo\";\n"
                        + "\t\tString base = \"/data/data/foo.bar/test-profiling\";\n"
                        + "\t\tString s = \"file://sdcard/foo\";\n"
                        + "\t}\n"
                        + "}\n"))
                .run()
                .expectClean();
    }

    public void testXml() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=207168
        // StopShip doesn't work in XML
        String expected = ""
                + "res/layout/foo.xml:1: Warning: STOPSHIP comment found; points to code which must be fixed prior to release [StopShip]\n"
                + "<!-- STOPSHIP implement this first -->\n"
                + "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/foo.xml", ""
                        + "<!-- STOPSHIP implement this first -->\n"
                        + "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"0dip\"\n"
                        + "    android:layout_height=\"0dip\"\n"
                        + "    android:visibility=\"gone\" />"))
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for res/layout/foo.xml line 0: Remove STOPSHIP:\n"
                        + "@@ -1 +1\n"
                        + "- <!-- STOPSHIP implement this first -->\n"
                        + "+ <!-- implement this first -->\n");
    }
}
