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
public class NamespaceDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new NamespaceDetector();
    }

    public void testCustom() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/customview.xml:5: Error: When using a custom namespace attribute in a library project, use the namespace \"http://schemas.android.com/apk/res-auto\" instead. [LibraryCustomView]\n"
                + "    xmlns:foo=\"http://schemas.android.com/apk/res/foo\"\n"
                + "               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintProject(
                    manifest().pkg("foo.library").minSdk(14),
                    projectProperties().library(true).compileSdk(14),
                    mCustomview
            ));
    }

    public void testGradle() throws Exception {
        assertEquals(""
                + "res/layout/customview.xml:5: Error: In Gradle projects, always use http://schemas.android.com/apk/res-auto for custom attributes [ResAuto]\n"
                + "    xmlns:foo=\"http://schemas.android.com/apk/res/foo\"\n"
                + "               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

                lintProject(
                        mLibrary, // dummy; only name counts
                        mCustomview
                ));
    }

    public void testCustomOk() throws Exception {
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintProject(
                    manifest().pkg("foo.library").minSdk(14),

                    // Use a standard project properties instead: no warning since it's
                    // not a library project:
                    //"multiproject/library.properties⇒project.properties",

                    mCustomview
            ));
    }

    public void testCustomOk2() throws Exception {
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintProject(
                    manifest().pkg("foo.library").minSdk(14),
                    projectProperties().library(true).compileSdk(14),
                    // This project already uses the res-auto package
                    xml("res/layout/customview2.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "    xmlns:other=\"http://schemas.foo.bar.com/other\"\n"
                            + "    xmlns:foo=\"http://schemas.android.com/apk/res-auto\"\n"
                            + "    android:id=\"@+id/newlinear\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\"\n"
                            + "    android:orientation=\"vertical\" >\n"
                            + "\n"
                            + "    <foo.bar.Baz\n"
                            + "        android:id=\"@+id/button1\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:text=\"Button1\"\n"
                            + "        foo:misc=\"Custom attribute\"\n"
                            + "        tools:ignore=\"HardcodedText\" >\n"
                            + "    </foo.bar.Baz>\n"
                            + "\n"
                            + "</LinearLayout>\n")
            ));
    }

    public void testTypo() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/wrong_namespace.xml:2: Error: Unexpected namespace URI bound to the \"android\" prefix, was http://schemas.android.com/apk/res/andriod, expected http://schemas.android.com/apk/res/android [NamespaceTypo]\n"
                + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/andriod\"\n"
                + "                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintProject(xml("res/layout/wrong_namespace.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/andriod\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\"\n"
                            + "    android:orientation=\"vertical\" >\n"
                            + "\n"
                            + "    <include\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        layout=\"@layout/layout2\" />\n"
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
                            + "</LinearLayout>\n")));
    }

    public void testTypo2() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/wrong_namespace2.xml:2: Error: URI is case sensitive: was \"http://schemas.android.com/apk/res/Android\", expected \"http://schemas.android.com/apk/res/android\" [NamespaceTypo]\n"
                + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/Android\"\n"
                + "                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintProject(xml("res/layout/wrong_namespace2.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/Android\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\"\n"
                            + "    android:orientation=\"vertical\" >\n"
                            + "\n"
                            + "    <include\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        layout=\"@layout/layout2\" />\n"
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
                            + "</LinearLayout>\n")));
    }

    public void testTypo3() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/wrong_namespace3.xml:2: Error: Unexpected namespace URI bound to the \"android\" prefix, was http://schemas.android.com/apk/res/androi, expected http://schemas.android.com/apk/res/android [NamespaceTypo]\n"
                + "<LinearLayout xmlns:a=\"http://schemas.android.com/apk/res/androi\"\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintProject(xml("res/layout/wrong_namespace3.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:a=\"http://schemas.android.com/apk/res/androi\"\n"
                            + "    a:layout_width=\"match_parent\"\n"
                            + "    a:layout_height=\"match_parent\"\n"
                            + "    a:orientation=\"vertical\" >\n"
                            + "\n"
                            + "    <include\n"
                            + "        a:layout_width=\"wrap_content\"\n"
                            + "        a:layout_height=\"wrap_content\"\n"
                            + "        layout=\"@layout/layout2\" />\n"
                            + "\n"
                            + "    <Button\n"
                            + "        a:id=\"@+id/button1\"\n"
                            + "        a:layout_width=\"wrap_content\"\n"
                            + "        a:layout_height=\"wrap_content\"\n"
                            + "        a:text=\"Button\" />\n"
                            + "\n"
                            + "    <Button\n"
                            + "        a:id=\"@+id/button2\"\n"
                            + "        a:layout_width=\"wrap_content\"\n"
                            + "        a:layout_height=\"wrap_content\"\n"
                            + "        a:text=\"Button\" />\n"
                            + "\n"
                            + "</LinearLayout>\n")));
    }

    public void testTypo4() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/wrong_namespace5.xml:2: Error: Suspicious namespace: should start with http:// [NamespaceTypo]\n"
                + "    xmlns:noturi=\"tp://schems.android.com/apk/res/com.my.package\"\n"
                + "                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/wrong_namespace5.xml:3: Error: Possible typo in URL: was \"http://schems.android.com/apk/res/com.my.package\", should probably be \"http://schemas.android.com/apk/res/com.my.package\" [NamespaceTypo]\n"
                + "    xmlns:typo1=\"http://schems.android.com/apk/res/com.my.package\"\n"
                + "                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/wrong_namespace5.xml:4: Error: Possible typo in URL: was \"http://schems.android.comm/apk/res/com.my.package\", should probably be \"http://schemas.android.com/apk/res/com.my.package\" [NamespaceTypo]\n"
                + "    xmlns:typo2=\"http://schems.android.comm/apk/res/com.my.package\"\n"
                + "                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "3 errors, 0 warnings\n",

            lintProject(xml("res/layout/wrong_namespace5.xml", ""
                            + "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:noturi=\"tp://schems.android.com/apk/res/com.my.package\"\n"
                            + "    xmlns:typo1=\"http://schems.android.com/apk/res/com.my.package\"\n"
                            + "    xmlns:typo2=\"http://schems.android.comm/apk/res/com.my.package\"\n"
                            + "    xmlns:ok=\"http://foo.bar/res/unrelated\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "    xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\" >\n"
                            + "\n"
                            + "</RelativeLayout>\n")));
    }

    public void testMisleadingPrefix() throws Exception {
        assertEquals(""
                + "res/layout/layout.xml:3: Error: Suspicious namespace and prefix combination [NamespaceTypo]\n"
                + "    xmlns:app=\"http://schemas.android.com/tools\"\n"
                + "               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/layout.xml:4: Error: Suspicious namespace and prefix combination [NamespaceTypo]\n"
                + "    xmlns:tools=\"http://schemas.android.com/apk/res/android\"\n"
                + "                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "2 errors, 0 warnings\n",

                lintProject(
                        xml("res/layout/layout.xml", ""
                                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                + "    xmlns:app=\"http://schemas.android.com/tools\"\n"
                                + "    xmlns:tools=\"http://schemas.android.com/apk/res/android\"\n"
                                + "    android:layout_width=\"match_parent\"\n"
                                + "    android:layout_height=\"match_parent\"\n"
                                + "    android:orientation=\"vertical\"\n"
                                + "    app:foo=\"true\"\n"
                                + "    tools:bar=\"true\" />\n")));
    }

    public void testTypoOk() throws Exception {
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",

                lintProject(xml("res/layout/wrong_namespace4.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<!-- This file does *not* have a wrong namespace: it's testdata to make sure we don't complain when \"a\" is defined for something unrelated -->\n"
                            + "<LinearLayout\n"
                            + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:a=\"http://something/very/different\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\"\n"
                            + "    android:orientation=\"vertical\" >\n"
                            + "\n"
                            + "    <include\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        layout=\"@layout/layout2\" />\n"
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
                            + "</LinearLayout>\n")));
    }

    public void testUnused() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/unused_namespace.xml:3: Warning: Unused namespace unused1 [UnusedNamespace]\n"
                + "    xmlns:unused1=\"http://schemas.android.com/apk/res/unused1\"\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/unused_namespace.xml:4: Warning: Unused namespace unused2 [UnusedNamespace]\n"
                + "    xmlns:unused2=\"http://schemas.android.com/apk/res/unused1\"\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

            lintProject(xml("res/layout/unused_namespace.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<foo.bar.LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:unused1=\"http://schemas.android.com/apk/res/unused1\"\n"
                            + "    xmlns:unused2=\"http://schemas.android.com/apk/res/unused1\"\n"
                            + "    xmlns:unused3=\"http://foo.bar.com/foo\"\n"
                            + "    xmlns:notunused=\"http://schemas.android.com/apk/res/notunused\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\" >\n"
                            + "\n"
                            + "    <foo.bar.Button\n"
                            + "        notunused:foo=\"Foo\"\n"
                            + "        tools:ignore=\"HardcodedText\" >\n"
                            + "    </foo.bar.Button>\n"
                            + "\n"
                            + "</foo.bar.LinearLayout>\n")));
    }

    public void testUnusedOk() throws Exception {
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",

                lintProject(xml("res/layout/layout1.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\"\n"
                            + "    android:orientation=\"vertical\" >\n"
                            + "\n"
                            + "    <include\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        layout=\"@layout/layout2\" />\n"
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
                            + "</LinearLayout>\n")));
    }

    public void testLayoutAttributesOk() throws Exception {
        assertEquals(
            "No warnings.",

            lintFiles(mNamespace3));
    }

    public void testLayoutAttributesOk2() throws Exception {
        assertEquals(
            "No warnings.",

            lintFiles(mNamespace4));
    }

    public void testLayoutAttributes() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/namespace3.xml:2: Error: When using a custom namespace attribute in a library project, use the namespace \"http://schemas.android.com/apk/res-auto\" instead. [LibraryCustomView]\n"
                + "    xmlns:app=\"http://schemas.android.com/apk/res/com.example.apicalltest\"\n"
                + "               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintFiles(mNamespace3,
                      manifest().pkg("foo.library").minSdk(14),
                      projectProperties().library(true).compileSdk(14)));
    }

    public void testLayoutAttributes2() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/namespace4.xml:3: Error: When using a custom namespace attribute in a library project, use the namespace \"http://schemas.android.com/apk/res-auto\" instead. [LibraryCustomView]\n"
                + "    xmlns:app=\"http://schemas.android.com/apk/res/com.example.apicalltest\"\n"
                + "               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintFiles(mNamespace4,
                    manifest().pkg("foo.library").minSdk(14),
                    projectProperties().library(true).compileSdk(14)));
    }

    public void testWrongResAutoUrl() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/namespace5.xml:3: Error: Suspicious namespace: Did you mean http://schemas.android.com/apk/res-auto? [ResAuto]\n"
                + "    xmlns:app=\"http://schemas.android.com/apk/auto-res/\"\n"
                + "               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintFiles(xml("res/layout/namespace5.xml", ""
                            + "<android.support.v7.widget.GridLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "    xmlns:app=\"http://schemas.android.com/apk/auto-res/\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\"\n"
                            + "    app:columnCount=\"1\"\n"
                            + "    tools:context=\".MainActivity\" >\n"
                            + "\n"
                            + "    <Button\n"
                            + "        android:id=\"@+id/button1\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        app:layout_column=\"0\"\n"
                            + "        app:layout_gravity=\"center\"\n"
                            + "        app:layout_row=\"0\"\n"
                            + "        android:text=\"Button\" />\n"
                            + "\n"
                            + "</android.support.v7.widget.GridLayout>\n"),
                    manifest().pkg("foo.library").minSdk(14),
                    projectProperties().library(true).compileSdk(14)));
    }

    public void testWrongResUrl() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:2: Error: Suspicious namespace: should start with http:// [NamespaceTypo]\n"
                + "<manifest xmlns:android=\"https://schemas.android.com/apk/res/android\"\n"
                + "                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintFiles(xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"https://schemas.android.com/apk/res/android\"\n"
                            + "          package=\"com.g.a.g.c\"\n"
                            + "          android:versionCode=\"21\"\n"
                            + "          android:versionName=\"0.0.1\">\n"
                            + "    <uses-sdk android:minSdkVersion=\"4\" />\n"
                            + "    <application />\n"
                            + "</manifest>\n"
                            + "\n")));
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mCustomview = xml("res/layout/customview.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
            + "    xmlns:other=\"http://schemas.foo.bar.com/other\"\n"
            + "    xmlns:foo=\"http://schemas.android.com/apk/res/foo\"\n"
            + "    android:id=\"@+id/newlinear\"\n"
            + "    android:layout_width=\"match_parent\"\n"
            + "    android:layout_height=\"match_parent\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "    <foo.bar.Baz\n"
            + "        android:id=\"@+id/button1\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:text=\"Button1\"\n"
            + "        foo:misc=\"Custom attribute\"\n"
            + "        tools:ignore=\"HardcodedText\" >\n"
            + "    </foo.bar.Baz>\n"
            + "\n"
            + "    <!-- Wrong namespace uri prefix: Don't warn -->\n"
            + "    <foo.bar.Baz\n"
            + "        android:id=\"@+id/button1\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:text=\"Button1\"\n"
            + "        other:misc=\"Custom attribute\"\n"
            + "        tools:ignore=\"HardcodedText\" >\n"
            + "    </foo.bar.Baz>\n"
            + "\n"
            + "</LinearLayout>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mLibrary = source("build.gradle", "");

    @SuppressWarnings("all") // Sample code
    private TestFile mNamespace3 = xml("res/layout/namespace3.xml", ""
            + "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:app=\"http://schemas.android.com/apk/res/com.example.apicalltest\"\n"
            + "    android:layout_width=\"match_parent\"\n"
            + "    android:layout_height=\"match_parent\" >\n"
            + "\n"
            + "    <com.example.library.MyView\n"
            + "        android:layout_width=\"300dp\"\n"
            + "        android:layout_height=\"300dp\"\n"
            + "        android:background=\"#ccc\"\n"
            + "        android:paddingBottom=\"40dp\"\n"
            + "        android:paddingLeft=\"20dp\"\n"
            + "        app:exampleColor=\"#33b5e5\"\n"
            + "        app:exampleDimension=\"24sp\"\n"
            + "        app:exampleDrawable=\"@android:drawable/ic_menu_add\"\n"
            + "        app:exampleString=\"Hello, MyView\" />\n"
            + "\n"
            + "</FrameLayout>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mNamespace4 = xml("res/layout/namespace4.xml", ""
            + "<android.support.v7.widget.GridLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
            + "    xmlns:app=\"http://schemas.android.com/apk/res/com.example.apicalltest\"\n"
            + "    android:layout_width=\"match_parent\"\n"
            + "    android:layout_height=\"match_parent\"\n"
            + "    app:columnCount=\"1\"\n"
            + "    tools:context=\".MainActivity\" >\n"
            + "\n"
            + "    <Button\n"
            + "        android:id=\"@+id/button1\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        app:layout_column=\"0\"\n"
            + "        app:layout_gravity=\"center\"\n"
            + "        app:layout_row=\"0\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "</android.support.v7.widget.GridLayout>\n");
}