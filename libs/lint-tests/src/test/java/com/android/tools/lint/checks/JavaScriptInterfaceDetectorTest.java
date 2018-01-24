/*
 * Copyright (C) 2013 The Android Open Source Project
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
public class JavaScriptInterfaceDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new JavaScriptInterfaceDetector();
    }

    public void testOlderSdk() throws Exception {
        lint().files(
                classpath(),
                projectProperties().compileSdk(19),
                manifest().minSdk(10),
                mAnnotatedObject,
                mInheritsFromAnnotated,
                mNonAnnotatedObject,
                mJavaScriptTest)
                .run()
                .expectClean();
    }

    public void test() throws Exception {
        String expected = ""
                + "src/test/pkg/JavaScriptTest.java:10: Error: None of the methods in the added interface (NonAnnotatedObject) have been annotated with @android.webkit.JavascriptInterface; they will not be visible in API 17 [JavascriptInterface]\n"
                + "  webview.addJavascriptInterface(new NonAnnotatedObject(), \"myobj\");\n"
                + "          ~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/JavaScriptTest.java:13: Error: None of the methods in the added interface (NonAnnotatedObject) have been annotated with @android.webkit.JavascriptInterface; they will not be visible in API 17 [JavascriptInterface]\n"
                + "  webview.addJavascriptInterface(o, \"myobj\");\n"
                + "          ~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/JavaScriptTest.java:20: Error: None of the methods in the added interface (NonAnnotatedObject) have been annotated with @android.webkit.JavascriptInterface; they will not be visible in API 17 [JavascriptInterface]\n"
                + "  webview.addJavascriptInterface(object2, \"myobj\");\n"
                + "          ~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/JavaScriptTest.java:31: Error: None of the methods in the added interface (NonAnnotatedObject) have been annotated with @android.webkit.JavascriptInterface; they will not be visible in API 17 [JavascriptInterface]\n"
                + "  webview.addJavascriptInterface(t, \"myobj\");\n"
                + "          ~~~~~~~~~~~~~~~~~~~~~~\n"
                + "4 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                classpath(),
                projectProperties().compileSdk(19),
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.bytecode\"\n"
                        + "    android:versionCode=\"1\"\n"
                        + "    android:versionName=\"1.0\" >\n"
                        + "\n"
                        + "    <uses-sdk android:minSdkVersion=\"10\" android:targetSdkVersion=\"17\" />\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:icon=\"@drawable/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\" >\n"
                        + "        <activity\n"
                        + "            android:name=\".BytecodeTestsActivity\"\n"
                        + "            android:label=\"@string/app_name\" >\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                        + "\n"
                        + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"),
                mAnnotatedObject,
                mInheritsFromAnnotated,
                mNonAnnotatedObject,
                mJavaScriptTest)
                .run()
                .expect(expected);
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mAnnotatedObject = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.webkit.JavascriptInterface;\n"
            + "\n"
            + "public class AnnotatedObject {\n"
            + "\t@JavascriptInterface\n"
            + "\tpublic void test1() {\n"
            + "\t}\n"
            + "\n"
            + "\tpublic void test2() {\n"
            + "\t}\n"
            + "\n"
            + "\t@JavascriptInterface\n"
            + "\tpublic void test3() {\n"
            + "\t}\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mInheritsFromAnnotated = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.webkit.JavascriptInterface;\n"
            + "\n"
            + "public class InheritsFromAnnotated extends AnnotatedObject {\n"
            + "\n"
            + "\t@Override\n"
            + "\tpublic void test1() {\n"
            + "\t}\n"
            + "\n"
            + "\t@Override\n"
            + "\tpublic void test2() {\n"
            + "\t}\n"
            + "\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mJavaScriptTest = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.annotation.SuppressLint;\n"
            + "import android.webkit.WebView;\n"
            + "\n"
            + "public class JavaScriptTest {\n"
            + "\tpublic void test(WebView webview) {\n"
            + "\t\twebview.addJavascriptInterface(new AnnotatedObject(), \"myobj\");\n"
            + "\t\twebview.addJavascriptInterface(new InheritsFromAnnotated(), \"myobj\");\n"
            + "\t\twebview.addJavascriptInterface(new NonAnnotatedObject(), \"myobj\");\n"
            + "\n"
            + "\t\tObject o = new NonAnnotatedObject();\n"
            + "\t\twebview.addJavascriptInterface(o, \"myobj\");\n"
            + "\t\to = new InheritsFromAnnotated();\n"
            + "\t\twebview.addJavascriptInterface(o, \"myobj\");\n"
            + "\t}\n"
            + "\n"
            + "\tpublic void test(WebView webview, AnnotatedObject object1, NonAnnotatedObject object2) {\n"
            + "\t\twebview.addJavascriptInterface(object1, \"myobj\");\n"
            + "\t\twebview.addJavascriptInterface(object2, \"myobj\");\n"
            + "\t}\n"
            + "\n"
            + "\t@SuppressLint(\"JavascriptInterface\")\n"
            + "\tpublic void testSuppressed(WebView webview) {\n"
            + "\t\twebview.addJavascriptInterface(new NonAnnotatedObject(), \"myobj\");\n"
            + "\t}\n"
            + "\n"
            + "\tpublic void testLaterReassignment(WebView webview) {\n"
            + "\t\tObject o = new NonAnnotatedObject();\n"
            + "\t\tObject t = o;\n"
            + "\t\twebview.addJavascriptInterface(t, \"myobj\");\n"
            + "\t\to = new AnnotatedObject();\n"
            + "\t}\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mNonAnnotatedObject = java(""
            + "package test.pkg;\n"
            + "\n"
            + "public class NonAnnotatedObject {\n"
            + "\tpublic void test1() {\n"
            + "\t}\n"
            + "\tpublic void test2() {\n"
            + "\t}\n"
            + "}\n");
}
