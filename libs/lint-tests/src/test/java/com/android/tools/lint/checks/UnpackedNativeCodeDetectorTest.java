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

/**
 * <b>NOTE: This is not a final API; if you rely on this be prepared to adjust your code for the
 * next tools release.</b>
 */
@SuppressWarnings("javadoc")
public class UnpackedNativeCodeDetectorTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new UnpackedNativeCodeDetector();
    }

    /**
     * Test that a manifest without extractNativeLibs produces warnings for Runtime.loadLibrary
     */
    public void testRuntimeLoadLibrary() throws Exception {
        String expected = ""
                + "src/main/AndroidManifest.xml:4: Warning: Missing attribute "
                + "android:extractNativeLibs=\"false\" on the "
                + "<application> tag. [UnpackedNativeCode]\n"
                + "    <application android:allowBackup=\"true\">\n"
                + "    ^\n"
                + "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest package=\"com.example.android.custom-lint-rules\"\n"
                        + "          xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                        + "\n"
                        + "    <application android:allowBackup=\"true\">\n"
                        + "    </application>\n"
                        + "</manifest>"),
                java("src/main/java/test/pkg/Load.java", ""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import java.lang.Runtime;\n"
                        + "\n"
                        + "public class Load {\n"
                        + "    public static void foo() {\n"
                        + "            Runtime.getRuntime().loadLibrary(\"hello\"); \n"
                        + "    }\n"
                        + "}\n"),
                gradle(""
                        + "buildscript {\n"
                        + "    dependencies {\n"
                        + "        classpath 'com.android.tools.build:gradle:2.2.0'\n"
                        + "    }\n"
                        + "}\n"
                        + "android {\n"
                        + "    compileSdkVersion 23\n"
                        + "}"))
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for src/main/AndroidManifest.xml line 3: Set extractNativeLibs=\"false\":\n"
                        + "@@ -5 +5\n"
                        + "-     <application android:allowBackup=\"true\" >\n"
                        + "+     <application\n"
                        + "+         android:allowBackup=\"true\"\n"
                        + "+         android:extractNativeLibs=\"false\" >\n");
    }

    /**
     * Test that a manifest without extractNativeLibs produces warnings for System.loadLibrary
     */
    public void testSystemLoadLibrary() throws Exception {
        String expected = ""
                + "src/main/AndroidManifest.xml:4: Warning: Missing attribute android:extractNativeLibs=\"false\" on the <application> tag. [UnpackedNativeCode]\n"
                + "    <application>\n"
                + "    ^\n"
                + "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest package=\"com.example.android.custom-lint-rules\"\n"
                        + "          xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                        + "\n"
                        + "    <application>\n"
                        + "    </application>\n"
                        + "</manifest>"),
                java("src/main/java/test/pkg/Load.java", ""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import java.lang.System;\n"
                        + "\n"
                        + "public class Load {\n"
                        + "    public static void foo() {\n"
                        + "            System.loadLibrary(\"hello\"); \n"
                        + "    }\n"
                        + "}\n"),
                gradle(""
                        + "buildscript {\n"
                        + "    dependencies {\n"
                        + "        classpath 'com.android.tools.build:gradle:2.2.0'\n"
                        + "    }\n"
                        + "}\n"
                        + "android {\n"
                        + "    compileSdkVersion 23\n"
                        + "}"))
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for src/main/AndroidManifest.xml line 3: Set extractNativeLibs=\"false\":\n"
                        + "@@ -5 +5\n"
                        + "-     <application>\n"
                        + "+     <application android:extractNativeLibs=\"false\" >\n");
    }

    /**
     * Test that a manifest with extractNativeLibs has no warnings.
     */
    public void testHasExtractNativeLibs() throws Exception {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest package=\"com.example.android.custom-lint-rules\"\n"
                        + "          xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "    <application android:extractNativeLibs=\"false\">\n"
                        + "    </application>\n"
                        + "</manifest>"),
                java("src/main/java/test/pkg/Load.java", ""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import java.lang.Runtime;\n"
                        + "\n"
                        + "public class Load {\n"
                        + "    public static void foo() {\n"
                        + "            Runtime.getRuntime().loadLibrary(\"hello\"); // ok\n"
                        + "    }\n"
                        + "}\n"),
                gradle(""
                        + "buildscript {\n"
                        + "    dependencies {\n"
                        + "        classpath 'com.android.tools.build:gradle:2.2.0'\n"
                        + "    }\n"
                        + "}\n"
                        + "android {\n"
                        + "    compileSdkVersion 23\n"
                        + "}"))
                .run()
                .expectClean();
    }

    /**
     * Test that suppressing the lint check using tools:ignore works.
     */
    public void testSuppress() throws Exception {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest package=\"com.example.android.custom-lint-rules\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "          xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                        + "\n"
                        + "    <application tools:ignore=\"UnpackedNativeCode\">\n"
                        + "    </application>\n"
                        + "</manifest>"),
                java("src/main/java/test/pkg/Load.java", ""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import java.lang.Runtime;\n"
                        + "\n"
                        + "public class Load {\n"
                        + "    public static void foo() {\n"
                        + "            Runtime.getRuntime().loadLibrary(\"hello\"); // ok\n"
                        + "    }\n"
                        + "}\n"),
                gradle(""
                        + "buildscript {\n"
                        + "    dependencies {\n"
                        + "        classpath 'com.android.tools.build:gradle:2.2.0'\n"
                        + "    }\n"
                        + "}\n"
                        + "android {\n"
                        + "    compileSdkVersion 23\n"
                        + "}"))
                .run()
                .expectClean();
    }
}