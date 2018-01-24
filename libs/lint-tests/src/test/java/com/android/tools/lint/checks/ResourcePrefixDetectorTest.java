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

import static com.android.sdklib.SdkVersionInfo.camelCaseToUnderlines;
import static com.android.sdklib.SdkVersionInfo.underlinesToCamelCase;
import static com.android.tools.lint.checks.infrastructure.ProjectDescription.Type.LIBRARY;

import com.android.tools.lint.checks.infrastructure.ProjectDescription;
import com.android.tools.lint.detector.api.Detector;

public class ResourcePrefixDetectorTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new ResourcePrefixDetector();
    }

    public void testResourceFiles() {
        String expected = ""
                + "res/drawable-mdpi/frame.png: Error: Resource named 'frame' does not start with the project's resource prefix 'unit_test_prefix_'; rename to 'unit_test_prefix_frame' ? [ResourceName]\n"
                + "res/layout/layout1.xml:2: Error: Resource named 'layout1' does not start with the project's resource prefix 'unit_test_prefix_'; rename to 'unit_test_prefix_layout1' ? [ResourceName]\n"
                + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "^\n"
                + "res/menu/menu.xml:2: Error: Resource named 'menu' does not start with the project's resource prefix 'unit_test_prefix_'; rename to 'unit_test_prefix_menu' ? [ResourceName]\n"
                + "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\" >\n"
                + "^\n"
                + "3 errors, 0 warnings\n";

        ProjectDescription project = project(
                xml("res/layout/layout1.xml", ""
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
                        + "</LinearLayout>\n"),
                xml("res/menu/menu.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\" >\n"
                        + "\n"
                        + "    <item\n"
                        + "        android:id=\"@+id/item1\"\n"
                        + "        android:icon=\"@drawable/icon1\"\n"
                        + "        android:title=\"My title 1\">\n"
                        + "    </item>\n"
                        + "    <item\n"
                        + "        android:id=\"@+id/item2\"\n"
                        + "        android:icon=\"@drawable/icon2\"\n"
                        + "        android:showAsAction=\"ifRoom\"\n"
                        + "        android:title=\"My title 2\">\n"
                        + "    </item>\n"
                        + "\n"
                        + "</menu>\n"),
                xml("res/layout/unit_test_prefix_ok.xml", ""
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
                        + "</LinearLayout>\n"),
                image("res/drawable-mdpi/frame.png", 472, 290),
                image("res/drawable/unit_test_prefix_ok1.png", 472, 290),
                image("res/drawable/unit_test_prefix_ok2.9.png", 472, 290),
                gradle(""
                        + "apply plugin: 'com.android.library'\n"
                        + "\n"
                        + "android {\n"
                        + "    resourcePrefix 'unit_test_prefix_'\n"
                        + "}")
        );

        lint().projects(project).run().expect(expected);
    }

    public void testValues() {
        String expected = ""
                + "res/values/customattr.xml:2: Error: Resource named 'ContentFrame' does not start with the project's resource prefix 'unit_test_prefix_'; rename to 'UnitTestPrefixContentFrame' ? [ResourceName]\n"
                + "    <declare-styleable name=\"ContentFrame\">\n"
                + "                       ~~~~~~~~~~~~~~~~~~~\n"
                + "res/values/customattr.xml:3: Error: Resource named 'content' does not start with the project's resource prefix 'unit_test_prefix_'; rename to 'unit_test_prefix_content' ? [ResourceName]\n"
                + "        <attr name=\"content\" format=\"reference\" />\n"
                + "              ~~~~~~~~~~~~~~\n"
                + "res/values/customattr.xml:4: Error: Resource named 'contentId' does not start with the project's resource prefix 'unit_test_prefix_'; rename to 'unit_test_prefix_contentId' ? [ResourceName]\n"
                + "        <attr name=\"contentId\" format=\"reference\" />\n"
                + "              ~~~~~~~~~~~~~~~~\n"
                + "res/layout/customattrlayout.xml:2: Error: Resource named 'customattrlayout' does not start with the project's resource prefix 'unit_test_prefix_'; rename to 'unit_test_prefix_customattrlayout' ? [ResourceName]\n"
                + "<foo.bar.ContentFrame\n"
                + "^\n"
                + "4 errors, 0 warnings\n";

        //noinspection all // Sample code
        ProjectDescription project = project(
                xml("res/values/customattr.xml", ""
                        + "<resources>\n"
                        + "    <declare-styleable name=\"ContentFrame\">\n"
                        + "        <attr name=\"content\" format=\"reference\" />\n"
                        + "        <attr name=\"contentId\" format=\"reference\" />\n"
                        + "    </declare-styleable>\n"
                        + "</resources>\n"),
                xml("res/layout/customattrlayout.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<foo.bar.ContentFrame\n"
                        + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:foobar=\"http://schemas.android.com/apk/res/foo.bar\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    foobar:contentId=\"@+id/test\" />\n"),
                java(""
                        + "/* AUTO-GENERATED FILE.  DO NOT MODIFY.\n"
                        + " *\n"
                        + " * This class was automatically generated by the\n"
                        + " * aapt tool from the resource data it found.  It\n"
                        + " * should not be modified by hand.\n"
                        + " */\n"
                        + "\n"
                        + "package my.pkg;\n"
                        + "\n"
                        + "public final class R {\n"
                        + "    public static final class attr {\n"
                        + "        public static final int contentId=0x7f020000;\n"
                        + "    }\n"
                        + "}\n"),
                manifest().minSdk(14),
                gradle(""
                        + "apply plugin: 'com.android.library'\n"
                        + "\n"
                        + "android {\n"
                        + "    resourcePrefix 'unit_test_prefix_'\n"
                        + "}")
        );

        lint().projects(project).run().expect(expected);
    }

    public void testMultiProject() {
        //noinspection all // Sample code
        ProjectDescription library = project(
                // Library project
                manifest().pkg("foo.library").minSdk(14),
                projectProperties().library(true).compileSdk(14),
                java(""
                        + "package foo.library;\n"
                        + "\n"
                        + "public class LibraryCode {\n"
                        + "    static {\n"
                        + "        System.out.println(R.string.string1);\n"
                        + "    }\n"
                        + "}\n"),
                xml("res/values/strings.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "\n"
                        + "    <string name=\"app_name\">LibraryProject</string>\n"
                        + "    <string name=\"string1\">String 1</string>\n"
                        + "    <string name=\"string2\">String 2</string>\n"
                        + "    <string name=\"string3\">String 3</string>\n"
                        + "\n"
                        + "</resources>\n"),
                gradle(""
                        + "apply plugin: 'com.android.library'\n"
                        + "\n"
                        + "android {\n"
                        + "    resourcePrefix 'unit_test_prefix_'\n"
                        + "}")
        ).type(LIBRARY).name("LibraryProject");

        //noinspection all // Sample code
        ProjectDescription main = project(
                // Master project
                manifest().pkg("foo.master").minSdk(14),
                java(""
                        + "package foo.main;\n"
                        + "\n"
                        + "public class MainCode {\n"
                        + "    static {\n"
                        + "        System.out.println(R.string.string2);\n"
                        + "    }\n"
                        + "}\n"),
                gradle(""
                        + "apply plugin: 'com.android.application'\n")
        );

        main.dependsOn(library);

        String expected = ""
                + "LibraryProject/res/values/strings.xml:4: Error: Resource named 'app_name' does not start with the project's resource prefix 'unit_test_prefix_'; rename to 'unit_test_prefix_app_name' ? [ResourceName]\n"
                + "    <string name=\"app_name\">LibraryProject</string>\n"
                + "            ~~~~~~~~~~~~~~~\n"
                + "LibraryProject/res/values/strings.xml:5: Error: Resource named 'string1' does not start with the project's resource prefix 'unit_test_prefix_'; rename to 'unit_test_prefix_string1' ? [ResourceName]\n"
                + "    <string name=\"string1\">String 1</string>\n"
                + "            ~~~~~~~~~~~~~~\n"
                + "LibraryProject/res/values/strings.xml:6: Error: Resource named 'string2' does not start with the project's resource prefix 'unit_test_prefix_'; rename to 'unit_test_prefix_string2' ? [ResourceName]\n"
                + "    <string name=\"string2\">String 2</string>\n"
                + "            ~~~~~~~~~~~~~~\n"
                + "LibraryProject/res/values/strings.xml:7: Error: Resource named 'string3' does not start with the project's resource prefix 'unit_test_prefix_'; rename to 'unit_test_prefix_string3' ? [ResourceName]\n"
                + "    <string name=\"string3\">String 3</string>\n"
                + "            ~~~~~~~~~~~~~~\n"
                + "4 errors, 0 warnings\n";

        lint().projects(main, library).run().expect(expected);
    }

    public void testSuppressGeneratedRs() {
        lint().files(source("res/raw/blend.bc", "dummy file"))
                .run()
                .expectClean();
    }

    public void testAndroidPrefix() {
        // Regression test for
        // 208973: Lint check for resource prefix doesn't ignore android: attributes
        lint().files
                (xml("res/values/values.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "  <declare-styleable name=\"Unit_test_prefix_MyView\">\n"
                        + "    <attr name=\"android:textColor\"/>\n"
                        + "    <attr name=\"unit_test_prefix_myAttribute\" format=\"reference\"/>\n"
                        + "  </declare-styleable>\n"
                        + "</resources>\n"),
                gradle(""
                        + "apply plugin: 'com.android.library'\n"
                        + "\n"
                        + "android {\n"
                        + "    resourcePrefix 'unit_test_prefix_'\n"
                        + "}"))

                .run()
                .expectClean();
    }

    public void testStyleableName() {
        lint().files(
                xml("res/values/values.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "  <declare-styleable name=\"Unit_test_prefixMyView\"/>\n"
                        + "</resources>\n"),
                gradle(""
                        + "apply plugin: 'com.android.library'\n"
                        + "\n"
                        + "android {\n"
                        + "    resourcePrefix 'unit_test_prefix_'\n"
                        + "}"))
                .run()
                .expectClean();
    }

    public void testPublicStyleableName() {
        lint().files(
                xml("res/values/values.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "  <public name=\"Unit_test_prefixMyView\" type=\"declare-styleable\"/>\n"
                        + "</resources>\n"),
                gradle(""
                        + "apply plugin: 'com.android.library'\n"
                        + "\n"
                        + "android {\n"
                        + "    resourcePrefix 'unit_test_prefix_'\n"
                        + "}"))
                .run()
                .expectClean();
    }

    // TODO: Test suppressing root level tag

    public void testPrefixMatches() {
        // Regression test for https://code.google.com/p/android/issues/detail?id=213569

        // Let's say the prefix is "myLib".
        // We'd like this to *allow* matching the following prefixes:
        //  - MyLibTheme
        //  - myLibAttr
        //  - my_lib_layout
        //  - mylib_layout
        // We similarly want the prefix "mylib_" (note the suffix) to also match
        // MyLibTheme (e.g. the _ can be omitted).

        assertFalse(checkPrefixMatches("a", "b"));
        assertFalse(checkPrefixMatches("ab", "b"));


        assertTrue(checkPrefixMatches("", ""));
        assertTrue(checkPrefixMatches("", "MyLibTheme"));
        assertTrue(checkPrefixMatches("myLib", "MyLibTheme"));
        assertTrue(checkPrefixMatches("myLib", "myLibAttr"));
        assertTrue(checkPrefixMatches("myLib", "my_lib_layout"));
        assertTrue(checkPrefixMatches("myLib", "mylib_layout"));

        assertTrue(checkPrefixMatches("my_lib", "MyLibTheme"));
        assertTrue(checkPrefixMatches("my_lib", "myLibAttr"));
        assertTrue(checkPrefixMatches("my_lib", "my_lib_layout"));
        assertTrue(checkPrefixMatches("my_lib", "mylib_layout"));

        assertTrue(checkPrefixMatches("my_lib_", "MyLibTheme"));
        assertTrue(checkPrefixMatches("my_lib_", "myLibAttr"));
        assertTrue(checkPrefixMatches("my_lib_", "my_lib_layout"));
        assertTrue(checkPrefixMatches("my_lib_", "mylib_layout"));
    }

    private static boolean checkPrefixMatches(String prefix, String name) {
        return ResourcePrefixDetector.libraryPrefixMatches(camelCaseToUnderlines(prefix), name) ||
                ResourcePrefixDetector.libraryPrefixMatches(underlinesToCamelCase(prefix), name);
    }
}
