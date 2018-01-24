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

import static com.android.tools.lint.checks.infrastructure.ProjectDescription.Type.LIBRARY;

import com.android.tools.lint.checks.infrastructure.ProjectDescription;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import java.io.File;
import java.util.Arrays;
import org.intellij.lang.annotations.Language;

@SuppressWarnings({"javadoc", "ClassNameDiffersFromFileName"})
public class UnusedResourceDetectorTest extends AbstractCheckTest {
    private boolean mEnableIds = false;

    @Override
    protected Detector getDetector() {
        return new UnusedResourceDetector();
    }

    @Override
    protected boolean allowCompilationErrors() {
        // Some of these unit tests are still relying on source code that references
        // unresolved symbols etc.
        return true;
    }

    @Override
    protected boolean isEnabled(Issue issue) {
        //noinspection SimplifiableIfStatement
        if (issue == UnusedResourceDetector.ISSUE_IDS) {
            return mEnableIds;
        } else {
            return true;
        }
    }

    public void testUnused() throws Exception {
        mEnableIds = false;
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/accessibility.xml:2: Warning: The resource R.layout.accessibility appears to be unused [UnusedResources]\n"
                + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" android:id=\"@+id/newlinear\" android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">\n"
                + "^\n"
                + "res/layout/main.xml:2: Warning: The resource R.layout.main appears to be unused [UnusedResources]\n"
                + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "^\n"
                + "res/layout/other.xml:2: Warning: The resource R.layout.other appears to be unused [UnusedResources]\n"
                + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "^\n"
                + "res/values/strings2.xml:3: Warning: The resource R.string.hello appears to be unused [UnusedResources]\n"
                + "    <string name=\"hello\">Hello</string>\n"
                + "            ~~~~~~~~~~~~\n"
                + "0 errors, 4 warnings\n",

            lintProject(
                    mStrings2,
                    mLayout1,
                    xml("res/layout/other.xml", ""
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

                    // Rename .txt files to .java
                    mTest,
                    mR,
                    manifest().minSdk(14),
                    mAccessibility));
    }

    public void testUnusedIds() throws Exception {
        mEnableIds = true;

        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/accessibility.xml:2: Warning: The resource R.layout.accessibility appears to be unused [UnusedResources]\n"
                + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" android:id=\"@+id/newlinear\" android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">\n"
                + "^\n"
                + "res/layout/accessibility.xml:2: Warning: The resource R.id.newlinear appears to be unused [UnusedIds]\n"
                + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" android:id=\"@+id/newlinear\" android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">\n"
                + "                                                                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/accessibility.xml:3: Warning: The resource R.id.button1 appears to be unused [UnusedIds]\n"
                + "    <Button android:text=\"Button\" android:id=\"@+id/button1\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"></Button>\n"
                + "                                  ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/accessibility.xml:4: Warning: The resource R.id.android_logo appears to be unused [UnusedIds]\n"
                + "    <ImageView android:id=\"@+id/android_logo\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                + "               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/accessibility.xml:5: Warning: The resource R.id.android_logo2 appears to be unused [UnusedIds]\n"
                + "    <ImageButton android:importantForAccessibility=\"yes\" android:id=\"@+id/android_logo2\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                + "                                                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 5 warnings\n",

            lintProject(
                mTest,
                mR,
                manifest().minSdk(14),
                mAccessibility));
    }

    public void testImplicitFragmentUsage() throws Exception {
        mEnableIds = true;
        // Regression test for https://code.google.com/p/android/issues/detail?id=209393
        // Ensure fragment id's aren't deleted.
        assertEquals("No warnings.",

                lintProject(
                        xml("res/layout/has_fragment.xml", ""
                                + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                                + "<fragment\n"
                                + "    android:id=\"@+id/viewer\"\n"
                                + "    android:name=\"package.name.MyFragment\"\n"
                                + "    android:layout_width=\"match_parent\"\n"
                                + "    android:layout_height=\"match_parent\"/>\n"
                                + "</LinearLayout>\n"),
                        java("src/test/pkg/Test.java", ""
                                + "package test.pkg;\n"
                                + "public class Test {\n"
                                + "    public void test() {"
                                + "        int used = R.layout.has_fragment;\n"
                                + "    }"
                                + "}")
                ));
    }

    public void testArrayReference() throws Exception {
        mEnableIds = false;
        assertEquals(""
                + "res/values/arrayusage.xml:2: Warning: The resource R.string.my_item appears to be unused [UnusedResources]\n"
                + "<string name=\"my_item\">An Item</string>\n"
                + "        ~~~~~~~~~~~~~~\n"
                + "res/values/arrayusage.xml:3: Warning: The resource R.array.my_array appears to be unused [UnusedResources]\n"
                + "<string-array name=\"my_array\">\n"
                + "              ~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

            lintProject(
                xml("res/values/arrayusage.xml", ""
                        + "<resources>\n"
                        + "<string name=\"my_item\">An Item</string>\n"
                        + "<string-array name=\"my_array\">\n"
                        + "   <item>@string/my_item</item>\n"
                        + "</string-array>\n"
                        + "</resources>\n")
                     ));
    }

    public void testArrayReferenceIncluded() throws Exception {
        mEnableIds = false;
        assertEquals("No warnings.",

                lintProject(
                        xml("res/values/arrayusage.xml", ""
                                + "<resources xmlns:tools=\"http://schemas.android.com/tools\""
                                + "   tools:keep=\"@array/my_array\">\n"
                                + "<string name=\"my_item\">An Item</string>\n"
                                + "<string-array name=\"my_array\">\n"
                                + "   <item>@string/my_item</item>\n"
                                + "</string-array>\n"
                                + "</resources>\n")
                ));
    }

    public void testAttrs() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/customattrlayout.xml:2: Warning: The resource R.layout.customattrlayout appears to be unused [UnusedResources]\n"
                + "<foo.bar.ContentFrame\n"
                + "^\n"
                + "0 errors, 1 warnings\n",

            lintProject(
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
                manifest().minSdk(14)));
    }

    public void testMultiProjectIgnoreLibraries() throws Exception {
        //noinspection all // Sample code
        assertEquals(
           "No warnings.",

            lintProject(
                // Master project
                manifest().pkg("foo.master").minSdk(14),
                projectProperties().property("android.library.reference.1", "../LibraryProject"),
                java(""
                            + "package foo.main;\n"
                            + "\n"
                            + "public class MainCode {\n"
                            + "    static {\n"
                            + "        System.out.println(R.string.string2);\n"
                            + "    }\n"
                            + "}\n"),

                // Library project
                manifest().pkg("foo.library").minSdk(14),
                source("../LibraryProject/project.properties", ""
                            + "target=android-14\n"
                            + "android.library=true\n"),
                mLibraryCode,
                xml("../LibraryProject/res/values/strings.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<resources>\n"
                            + "\n"
                            + "    <string name=\"app_name\">LibraryProject</string>\n"
                            + "    <string name=\"string1\">String 1</string>\n"
                            + "    <string name=\"string2\">String 2</string>\n"
                            + "    <string name=\"string3\">String 3</string>\n"
                            + "\n"
                            + "</resources>\n")
            ));
    }

    public void testMultiProject() throws Exception {
        //noinspection all // Sample code
        ProjectDescription library = project(
                // Library project
                mLibraryManifest,
                mLibraryCode,
                mLibraryStrings
        ).type(LIBRARY).name("LibraryProject");

        //noinspection all // Sample code
        ProjectDescription main = project(
                mMainCode
        ).name("App").dependsOn(library);

        lint().projects(main, library)
                .run()
                .expectClean();
    }

    public void testFqcnReference() throws Exception {
        //noinspection all // Sample code
        assertEquals(
           "No warnings.",

            lintProject(
                mLayout1,
                java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import android.app.Activity;\n"
                            + "import android.os.Bundle;\n"
                            + "\n"
                            + "public class UnusedReference extends Activity {\n"
                            + "    @Override\n"
                            + "    public void onCreate(Bundle savedInstanceState) {\n"
                            + "        super.onCreate(savedInstanceState);\n"
                            + "        setContentView(test.pkg.R.layout.main);\n"
                            + "    }\n"
                            + "}\n"),
                manifest().minSdk(14)));
    }

    /* Not sure about this -- why would we ignore drawable XML?
    public void testIgnoreXmlDrawable() throws Exception {
        assertEquals(
           "No warnings.",

            lintProject(
                    "res/drawable/ic_menu_help.xml",
                    "gen/my/pkg/R2.java.txt=>gen/my/pkg/R.java"
            ));
    }
    */

    public void testPlurals() throws Exception {
        //noinspection ClassNameDiffersFromFileName
        assertEquals("No warnings.",
            lintProject(
                xml("res/values/strings4.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <string name=\"hello\">Hello</string>\n"
                        + "</resources>\n"),
                xml("res/values/plurals.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <plurals name=\"my_plural\">\n"
                        + "        <item quantity=\"one\">@string/hello</item>\n"
                        + "        <item quantity=\"few\">@string/hello</item>\n"
                        + "        <item quantity=\"other\">@string/hello</item>\n"
                        + "    </plurals>\n"
                        + "</resources>\n"),
                java("src/test/pkg/Test.java", ""
                        + "package test.pkg;\n"
                        + "public class Test {\n"
                        + "    public void test() {"
                        + "        int used = R.plurals.my_plural;\n"
                        + "    }"
                        + "}")
            ));
    }

    public void testLibraryMerging() throws Exception {
        // http://code.google.com/p/android/issues/detail?id=36952
        File master = getProjectDir("MasterProject",
                // Master project
                manifest().pkg("foo.master").minSdk(14),
                projectProperties().property("android.library.reference.1", "../LibraryProject").property("manifestmerger.enabled", "true"),
                mMainCode
        );

        File library = getProjectDir("LibraryProject",
                // Library project
                mLibraryManifest,
                projectProperties().library(true).compileSdk(14),
                mLibraryCode,
                mLibraryStrings
        );
        assertEquals(
           // The strings are all referenced in the library project's manifest file
           // which in this project is merged in
           "No warnings.",

           checkLint(Arrays.asList(master, library)));
    }

    public void testCornerCase() throws Exception {
        // See http://code.google.com/p/projectlombok/issues/detail?id=415
        mEnableIds = true;
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

             lintProject(
                 java(""
                            + "// http://code.google.com/p/projectlombok/issues/detail?id=415\n"
                            + "package test.pkg;\n"
                            + "public class X {\n"
                            + "  public void X(Y parent) {\n"
                            + "    parent.new Z(parent.getW()).execute();\n"
                            + "  }\n"
                            + "}\n"),
                 manifest().minSdk(14)));
    }

    public void testAnalytics() throws Exception {
        // See http://code.google.com/p/android/issues/detail?id=42565
        mEnableIds = false;
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",

                lintProject(
                        xml("res/values/analytics.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n"
                            + "<resources>\n"
                            + "  <!--Replace placeholder ID with your tracking ID-->\n"
                            + "  <string name=\"ga_trackingId\">UA-12345678-1</string>\n"
                            + "\n"
                            + "  <!--Enable Activity tracking-->\n"
                            + "  <bool name=\"ga_autoActivityTracking\">true</bool>\n"
                            + "\n"
                            + "  <!--Enable automatic exception tracking-->\n"
                            + "  <bool name=\"ga_reportUncaughtExceptions\">true</bool>\n"
                            + "\n"
                            + "  <!-- The screen names that will appear in your reporting -->\n"
                            + "  <string name=\"com.example.app.BaseActivity\">Home</string>\n"
                            + "  <string name=\"com.example.app.PrefsActivity\">Preferences</string>\n"
                            + "  <string name=\"test.pkg.OnClickActivity\">Clicks</string>\n"
                            + "</resources>\n")
        ));
    }

    public void testIntegers() throws Exception {
        // See https://code.google.com/p/android/issues/detail?id=53995
        mEnableIds = true;
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",

                lintProject(
                        xml("res/values/integers.xml", ""
                            + "<resources>\n"
                            + "    <item name=\"bar_display_duration\" type=\"integer\">3600</item>\n"
                            + "    <item name=\"bar_slide_out_duration\" type=\"integer\">2400</item>\n"
                            + "</resources>\n"),
                        xml("res/anim/slide_in_out.xml", ""
                            + "<set xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "     xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "     tools:ignore=\"UnusedResources\">\n"
                            + "    <translate\n"
                            + "      android:duration=\"@integer/bar_slide_out_duration\"\n"
                            + "      android:startOffset=\"@integer/bar_display_duration\" />\n"
                            + "</set>\n"
                            + "\n")
                ));
    }

    public void testIntegerArrays() throws Exception {
        // See http://code.google.com/p/android/issues/detail?id=59761
        mEnableIds = false;
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",
                lintProject(xml("res/values/integer_arrays.xml", ""
                            + "<resources xmlns:tools=\"http://schemas.android.com/tools\">\n"
                            + "    <dimen name=\"used\">16dp</dimen>\n"
                            + "\n"
                            + "    <integer-array name=\"iconsets_array_ids\" tools:ignore=\"UnusedResources\">\n"
                            + "        <item>@array/iconset_pixelmixer_basic</item>\n"
                            + "        <item>@array/iconset_dryicons_coquette</item>\n"
                            + "    </integer-array>\n"
                            + "\n"
                            + "    <integer-array name=\"iconset_pixelmixer_basic\">\n"
                            + "        <item>@dimen/used</item>\n"
                            + "    </integer-array>\n"
                            + "\n"
                            + "    <integer-array name=\"iconset_dryicons_coquette\">\n"
                            + "        <item>@dimen/used</item>\n"
                            + "    </integer-array>\n"
                            + "</resources>\n")));
    }

    public void testUnitTestReferences() throws Exception {
        // Make sure that we pick up references in unit tests as well
        // Regression test for
        // https://code.google.com/p/android/issues/detail?id=79066
        mEnableIds = false;

        //noinspection ClassNameDiffersFromFileName
        assertEquals("No warnings.",

                lintProject(
                        mStrings2,
                        mLayout1,
                        mOther,
                        mTest,
                        mR,
                        manifest(),
                        mAccessibility,

                        // Add unit test source which references resources which would otherwise
                        // be marked as unused
                        java("test/my/pkg/MyTest.java", ""
                                + "package my.pkg;\n"
                                + "class MyTest {\n"
                                + "    public void test() {\n"
                                + "        System.out.println(R.layout.accessibility);\n"
                                + "        System.out.println(R.layout.main);\n"
                                + "        System.out.println(R.layout.other);\n"
                                + "        System.out.println(R.string.hello);\n"
                                + "    }\n"
                                + "}\n")
                        ));
    }

    public void testDataBinding() throws Exception {
        // Make sure that resources referenced only via a data binding expression
        // are not counted as unused.
        // Regression test for https://code.google.com/p/android/issues/detail?id=183934
        mEnableIds = false;
        assertEquals("No warnings.",

                lintProject(
                        xml("res/values/resources.xml", ""
                                + "<resources>\n"
                                + "    <item type='dimen' name='largePadding'>20dp</item>\n"
                                + "    <item type='dimen' name='smallPadding'>15dp</item>\n"
                                + "    <item type='string' name='nameFormat'>%1$s %2$s</item>\n"
                                + "</resources>"),

                        // Add unit test source which references resources which would otherwise
                        // be marked as unused
                        xml("res/layout/db.xml", ""
                                + "<layout xmlns:android=\"http://schemas.android.com/apk/res/android\""
                                + "    xmlns:tools=\"http://schemas.android.com/tools\" "
                                + "    tools:keep=\"@layout/db\">\n"
                                + "   <data>\n"
                                + "       <variable name=\"user\" type=\"com.example.User\"/>\n"
                                + "   </data>\n"
                                + "   <LinearLayout\n"
                                + "       android:orientation=\"vertical\"\n"
                                + "       android:layout_width=\"match_parent\"\n"
                                + "       android:layout_height=\"match_parent\"\n"
                                // Data binding expressions
                                + "       android:padding=\"@{large? @dimen/largePadding : @dimen/smallPadding}\"\n"
                                + "       android:text=\"@{@string/nameFormat(firstName, lastName)}\" />\n"
                                + "</layout>")
                ));
    }

    public void testDataBindingIds() throws Exception {
        // Make sure id's in data binding layouts aren't considered unused
        // (since the compiler will generate accessors for these that
        // may not be visible when running lint on edited sources)
        // Regression test for https://code.google.com/p/android/issues/detail?id=189065
        mEnableIds = true;
        assertEquals("No warnings.",

                lintProject(
                        xml("res/layout/db.xml", ""
                                + "<layout xmlns:android=\"http://schemas.android.com/apk/res/android\""
                                + "    xmlns:tools=\"http://schemas.android.com/tools\" "
                                + "    tools:keep=\"@layout/db\">\n"
                                + "   <data>\n"
                                + "       <variable name=\"user\" type=\"com.example.User\"/>\n"
                                + "   </data>\n"
                                + "   <LinearLayout\n"
                                + "       android:orientation=\"vertical\"\n"
                                + "       android:id=\"@+id/my_id\"\n"
                                + "       android:layout_width=\"match_parent\"\n"
                                + "       android:layout_height=\"match_parent\" />\n"
                                + "</layout>")
                ));
    }

    public void testPublic() throws Exception {
        // Resources marked as public should not be listed as potentially unused
        mEnableIds = false;
        assertEquals(""
                + "res/values/resources.xml:4: Warning: The resource R.string.nameFormat appears to be unused [UnusedResources]\n"
                + "    <item type='string' name='nameFormat'>%1$s %2$s</item>\n"
                + "                        ~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",

                lintProject(
                        xml("res/values/resources.xml", ""
                                + "<resources>\n"
                                + "    <item type='dimen' name='largePadding'>20dp</item>\n"
                                + "    <item type='dimen' name='smallPadding'>15dp</item>\n"
                                + "    <item type='string' name='nameFormat'>%1$s %2$s</item>\n"
                                + "    <public type='dimen' name='largePadding' />"
                                + "    <public type='dimen' name='smallPadding' />"
                                + "</resources>")
                ));
    }

    public void testDynamicResources() throws Exception {
        String expected = ""
                + "build.gradle: Warning: The resource R.string.cat appears to be unused [UnusedResources]\n"
                + "build.gradle: Warning: The resource R.string.dog appears to be unused [UnusedResources]\n"
                + "0 errors, 2 warnings\n";

        //noinspection all // Sample code
        lint().files(
                mLayout1,
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.os.Bundle;\n"
                        + "import android.support.design.widget.Snackbar;\n"
                        + "\n"
                        + "public class UnusedReferenceDynamic extends Activity {\n"
                        + "    @Override\n"
                        + "    public void onCreate(Bundle savedInstanceState) {\n"
                        + "        super.onCreate(savedInstanceState);\n"
                        + "        setContentView(test.pkg.R.layout.main);\n"
                        + "        Snackbar.make(view, R.string.xyz, Snackbar.LENGTH_LONG);\n"
                        + "    }\n"
                        + "}\n"),
                manifest().minSdk(14),
                gradle(""
                        + "android {\n"
                        + "    defaultConfig {\n"
                        + "        resValue \"string\", \"cat\", \"Some Data\"\n"
                        + "    }\n"
                        + "    buildTypes {\n"
                        + "        debug {\n"
                        + "            resValue \"string\", \"foo\", \"Some Data\"\n"
                        + "        }\n"
                        + "        release {\n"
                        + "            resValue \"string\", \"xyz\", \"Some Data\"\n"
                        + "            resValue \"string\", \"dog\", \"Some Data\"\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .variant("release")
                .issues(UnusedResourceDetector.ISSUE) // skip UnusedResourceDetector.ISSUE_IDS
                .allowCompilationErrors()
                .run()
                .expect(expected);
    }

    public void testStaticImport() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=40293
        // 40293: Lint reports resource as unused when referenced via "import static"
        mEnableIds = false;
        assertEquals(""
                + "No warnings.",

                lintProject(
                        xml("res/values/resources.xml", ""
                                + "<resources>\n"
                                + "    <item type='dimen' name='largePadding'>20dp</item>\n"
                                + "    <item type='dimen' name='smallPadding'>15dp</item>\n"
                                + "    <item type='string' name='nameFormat'>%1$s %2$s</item>\n"
                                + "</resources>"),

                        // Add unit test source which references resources which would otherwise
                        // be marked as unused
                        java("src/test/pkg/TestCode.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import static test.pkg.R.dimen.*;\n"
                                + "import static test.pkg.R.string.nameFormat;\n"
                                + "import test.pkg.R.dimen;\n"
                                + "\n"
                                + "public class TestCode {\n"
                                + "    public void test() {\n"
                                + "        int x = dimen.smallPadding; // Qualified import\n"
                                + "        int y = largePadding; // Static wildcard import\n"
                                + "        int z = nameFormat; // Static explicit import\n"
                                + "    }\n"
                                + "}\n"),
                        java("src/test/pkg/R.java", ""
                                + "package test.pkg;\n"
                                + "public class R {\n"
                                + "    public static class dimen {\n"
                                + "        public static final int largePadding = 1;\n"
                                + "        public static final int smallPadding = 2;\n"
                                + "    }\n"
                                + "    public static class string {\n"
                                + "        public static final int nameFormat = 3;\n"
                                + "    }\n"
                                + "}")
                ));
    }

    public void testStyles() throws Exception {
        String expected = ""
                + "res/values/styles.xml:4: Warning: The resource R.style.UnusedStyleExtendingFramework appears to be unused [UnusedResources]\n"
                + "   <style name=\"UnusedStyleExtendingFramework\" parent=\"android:Theme\"/>\n"
                + "          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/values/styles.xml:5: Warning: The resource R.style.UnusedStyle appears to be unused [UnusedResources]\n"
                + "    <style name=\"UnusedStyle\"/>\n"
                + "           ~~~~~~~~~~~~~~~~~~\n"
                + "res/values/styles.xml:6: Warning: The resource R.style.UnusedStyle_Sub appears to be unused [UnusedResources]\n"
                + "    <style name=\"UnusedStyle.Sub\"/>\n"
                + "           ~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/values/styles.xml:7: Warning: The resource R.style.UnusedStyle_Something_Sub appears to be unused [UnusedResources]\n"
                + "    <style name=\"UnusedStyle.Something.Sub\" parent=\"UnusedStyle\"/>\n"
                + "           ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/values/styles.xml:8: Warning: The resource R.style.ImplicitUsed appears to be unused [UnusedResources]\n"
                + "    <style name=\"ImplicitUsed\" parent=\"android:Widget.ActionBar\"/>\n"
                + "           ~~~~~~~~~~~~~~~~~~~\n"
                + "res/values/styles.xml:9: Warning: The resource R.style.EmptyParent appears to be unused [UnusedResources]\n"
                + "    <style name=\"EmptyParent\" parent=\"\"/>\n"
                + "           ~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 6 warnings\n";
        lint().files(
                xml("res/values/styles.xml", ""
                        + "<resources>\n\n\n"
                        +  "   <style name=\"UnusedStyleExtendingFramework\" parent=\"android:Theme\"/>\n"
                        + "    <style name=\"UnusedStyle\"/>\n"
                        + "    <style name=\"UnusedStyle.Sub\"/>\n"
                        + "    <style name=\"UnusedStyle.Something.Sub\" parent=\"UnusedStyle\"/>\n"

                        + "    <style name=\"ImplicitUsed\" parent=\"android:Widget.ActionBar\"/>\n"
                        + "    <style name=\"EmptyParent\" parent=\"\"/>\n"
                        + "</resources>"))
                .run()
                .expect(expected);
    }

    public void testStylePrefix() throws Exception {
        // AAPT accepts parent style references that simply start with "style/" (not @style);
        // similarly, it also allows android:style/ rather than @android:style/
        lint().files(
                xml("res/values/styles.xml", ""
                        + "<resources \n"
                        + "        xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "        tools:keep=\"@style/MyInheritingStyle\" >\n"
                        +  "    <style name=\"MyStyle\">\n"
                        + "        <item name=\"android:textColor\">#ffff00ff</item>\n"
                        + "    </style>\n"
                        + "\n"
                        + "    <style name=\"MyInheritingStyle\" parent=\"style/MyStyle\">\n"
                        + "        <item name=\"android:textSize\">24pt</item>\n"
                        + "    </style>\n"
                        + "</resources>"))
                .run()
                .expectClean();
    }

    public void testThemeFromLayout() throws Exception {
        mEnableIds = false;
        assertEquals("No warnings.",

                lintProject(
                        xml("res/values/styles.xml", ""
                                + "<resources>\n"
                                + "    <style name=\"InlineActionView\" />\n"
                                + "    <style name=\"InlineActionView.Like\">\n"
                                + "    </style>\n"
                                + "</resources>\n"),
                        xml("res/layout/main.xml", ""
                                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                + "    android:orientation=\"vertical\" android:layout_width=\"match_parent\"\n"
                                + "    android:layout_height=\"match_parent\">\n"
                                + "\n"
                                + "    <Button\n"
                                + "        android:layout_width=\"wrap_content\"\n"
                                + "        android:layout_height=\"wrap_content\"\n"
                                + "        style=\"@style/InlineActionView.Like\"\n"
                                + "        android:layout_gravity=\"center_horizontal\" />\n"
                                + "</LinearLayout>"),
                        java("test/my/pkg/MyTest.java", ""
                                + "package my.pkg;\n"
                                + "class MyTest {\n"
                                + "    public void test() {\n"
                                + "        System.out.println(R.layout.main);\n"
                                + "    }\n"
                                + "}\n")
                ));
    }

    public void testReferenceFromObjectLiteralArguments() throws Exception {
        lint().files(
                xml("res/layout/main.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:orientation=\"vertical\" android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\" />\n"),
                java("test/my/pkg/MyTest.java", ""
                        + "package test.pkg;\n"
                        + "\n"
                        + "public class MyTest {\n"
                        + "    public Object test() {\n"
                        + "        return new Inner<String>(R.layout.main) {\n"
                        + "            @Override\n"
                        + "            public void foo() {\n"
                        + "                super.foo();\n"
                        + "            }\n"
                        + "        };\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class Inner<T> {\n"
                        + "        public Inner(int id) {\n"
                        + "        }\n"
                        + "        public void foo() {\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expectClean();
    }

    public void testKeepAndDiscard() throws Exception {
        mEnableIds = false;
        assertEquals("No warnings.",

                lintProject(
                        // By name
                        xml("res/raw/keep.xml", ""
                                + "<foo/>"),

                        // By content
                        xml("res/raw/used.xml", ""
                                + "<resources\n"
                                + "        xmlns:tools=\"http://schemas.android.com/tools\"\n"
                                + "        tools:shrinkMode=\"strict\"\n"
                                + "        tools:discard=\"@raw/unused\"\n"
                                + "        tools:keep=\"@raw/used\" />\n")

                ));
    }

    public void testStringsWithDots() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=214189

        mEnableIds = false;
        assertEquals("No warnings.",

                lintProject(
                        xml("res/values/strings.xml", ""
                                + "<resources>\n"
                                + "    <string name=\"foo.bar.your_name\">Your Name</string>\n"
                                + "</resources>\n"),
                        java("test/my/pkg/MyTest.java", ""
                                + "package my.pkg;\n"
                                + "class MyTest {\n"
                                + "    public void test() {\n"
                                + "        System.out.println(R.string.foo_bar_your_name);\n"
                                + "    }\n"
                                + "}\n")
                ));
    }

    public void testToolsNamespaceReferences() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=226204
        mEnableIds = false;
        assertEquals("No warnings.",
                lintProject(
                        xml("res/layout/my_layout.xml", ""
                                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                + "<android.support.constraint.ConstraintLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                                + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                                + "    android:id=\"@+id/activity_main\"\n"
                                + "    android:layout_width=\"match_parent\"\n"
                                + "    android:layout_height=\"match_parent\"\n"
                                + "    tools:context=\"com.example.tnorbye.myapplication.MainActivity\">\n"
                                + "\n"
                                + "    <TextView\n"
                                + "        android:layout_width=\"wrap_content\"\n"
                                + "        android:layout_height=\"wrap_content\"\n"
                                + "        android:text=\"Hello World!\"\n"
                                + "        tools:background=\"@drawable/my_drawable\"\n"
                                + "        app:layout_constraintBottom_toBottomOf=\"@+id/activity_main\"\n"
                                + "        app:layout_constraintLeft_toLeftOf=\"@+id/activity_main\"\n"
                                + "        app:layout_constraintRight_toRightOf=\"@+id/activity_main\"\n"
                                + "        app:layout_constraintTop_toTopOf=\"@+id/activity_main\" />\n"
                                + "\n"
                                + "</android.support.constraint.ConstraintLayout>\n"),
                        xml("res/drawable/my_drawable.xml", ""
                                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                + "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                                + "\n"
                                + "</selector>"),

                        // By content
                        xml("res/raw/used.xml", ""
                                + "<resources\n"
                                + "        xmlns:tools=\"http://schemas.android.com/tools\"\n"
                                + "        tools:shrinkMode=\"strict\"\n"
                                + "        tools:keep=\"@raw/used,@layout/my_layout\" />\n")
                ));
    }

    public void testReferenceFromDataBinding() throws Exception {
        // Regression test for https://issuetracker.google.com/38213600
        //noinspection all // Sample code
        lint().files(
                // Data binding layout
                xml("res/layout/added_view.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<layout xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"match_parent\"\n"
                        + "        android:orientation=\"vertical\"\n"
                        + "        android:text=\"Hello World\"/>\n"
                        + "</layout>"),
                xml("res/layout/added_view2.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<layout xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "    <data class=\".IndependentLibraryBinding\">\n"
                        + "    </data>\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"match_parent\"\n"
                        + "        android:orientation=\"vertical\"\n"
                        + "        android:text=\"Hello World\"/>\n"
                        + "</layout>"),
                xml("res/layout/third_added_view.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<layout xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "    <data>\n"
                        + "    </data>\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"match_parent\"\n"
                        + "        android:orientation=\"vertical\"\n"
                        + "        android:text=\"Hello World\"/>\n"
                        + "</layout>"),
                // Only usage: data binding class
                java(""
                        + "package my.pkg;\n"
                        + "\n"
                        + "import android.view.LayoutInflater;\n"
                        + "\n"
                        + "public class Ref {\n"
                        + "    public void test(LayoutInflater inflater){\n"
                        + "        final AddedViewBinding addedView = AddedViewBinding.inflate(inflater, null, true);\n"
                        + "        final ThirdAddedViewBinding addedView2 = ThirdAddedViewBinding.inflate(inflater, null, true);\n"
                        + "        final AddedViewBinding addedView3 = IndependentLibraryBinding.inflate(inflater, null, true);\n"
                        + "    }\n"
                        + "}\n"),
                // Stubs to make type resolution work in test without actual data binding
                // code-gen and data binding runtime libraries
                java(""
                        + "package my.pkg;\n"
                        + "\n"
                        + "abstract class AddedViewBinding extends android.databinding.ViewDataBinding {\n"
                        + "    public AddedViewBinding(android.databinding.DataBindingComponent bindingComponent,\n"
                        + "                             android.view.View root, int localFieldCount) {\n"
                        + "        super(bindingComponent, root, localFieldCount);\n"
                        + "    }\n"
                        + "\n"
                        + "    public static AddedViewBinding inflate(android.view.LayoutInflater inflater, \n"
                        + "                                           android.view.ViewGroup root, \n"
                        + "                                           boolean attachToRoot) {\n"
                        + "        return null;\n"
                        + "    }\n"
                        + "}\n"),
                java(""
                        + "package my.pkg;\n"
                        + "\n"
                        + "abstract class IndependentLibraryBinding extends android.databinding.ViewDataBinding {\n"
                        + "    public IndependentLibraryBinding(android.databinding.DataBindingComponent bindingComponent,\n"
                        + "                             android.view.View root, int localFieldCount) {\n"
                        + "        super(bindingComponent, root, localFieldCount);\n"
                        + "    }\n"
                        + "\n"
                        + "    public static IndependentLibraryBinding inflate(android.view.LayoutInflater inflater, \n"
                        + "                                           android.view.ViewGroup root, \n"
                        + "                                           boolean attachToRoot) {\n"
                        + "        return null;\n"
                        + "    }\n"
                        + "}\n"),
                java(""
                        + "package my.pkg;\n"
                        + "\n"
                        + "abstract class ThirdAddedViewBinding extends android.databinding.ViewDataBinding {\n"
                        + "    public ThirdAddedViewBinding(android.databinding.DataBindingComponent bindingComponent,\n"
                        + "                             android.view.View root, int localFieldCount) {\n"
                        + "        super(bindingComponent, root, localFieldCount);\n"
                        + "    }\n"
                        + "\n"
                        + "    public static ThirdAddedViewBinding inflate(android.view.LayoutInflater inflater, \n"
                        + "                                           android.view.ViewGroup root, \n"
                        + "                                           boolean attachToRoot) {\n"
                        + "        return null;\n"
                        + "    }\n"
                        + "}\n"),
                java(""
                        + "package android.databinding;\n"
                        + "public abstract class ViewDataBinding {\n"
                        + "}"))
                .run()
                .expectClean();
    }

    @SuppressWarnings("SpellCheckingInspection")
    public void testButterknife() throws Exception {
        // Regression test for https://issuetracker.google.com/62640956
        //noinspection all // Sample code
        lint().files(
                // Data binding layout
                xml("res/values/colors.xml", "" +
                        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<resources>\n" +
                        "    <color name=\"bgColor\">#FF4444</color>\n" +
                        "</resources>\n"),
                java("" +
                        "package my.pkg;\n" +
                        "import butterknife.BindColor;\n" +
                        "\n" +
                        "public class Reference {\n" +
                        "    @BindColor(R2.color.bgColor)\n" +
                        "    int bgColor;\n" +
                        "}\n"),
                java("" +
                        "package butterknife;\n" +
                        "import java.lang.annotation.*;\n" +
                        "import static java.lang.annotation.ElementType.FIELD;\n" +
                        "import static java.lang.annotation.RetentionPolicy.CLASS;\n" +
                        "@Retention(CLASS) @Target(FIELD)\n" +
                        "public @interface BindColor {\n" +
                        "  int value();\n" +
                        "}\n"),
                java("" +
                        "package my.pkg;\n" +
                        "\n" +
                        "public final class R {\n" +
                        "    public static final class color {\n" +
                        "        public static final int bgColor=0x7f05001f;" +
                        "    }\n" +
                        "}\n"),
                java("" +
                        "package my.pkg;\n" +
                        "\n" +
                        "public final class R2 {\n" +
                        "    public static final class color {\n" +
                        "        public static final int bgColor=0x7f05001f;" +
                        "    }\n" +
                        "}\n"))
                .run()
                .expectClean();
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mAccessibility = xml("res/layout/accessibility.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" android:id=\"@+id/newlinear\" android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">\n"
            + "    <Button android:text=\"Button\" android:id=\"@+id/button1\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"></Button>\n"
            + "    <ImageView android:id=\"@+id/android_logo\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
            + "    <ImageButton android:importantForAccessibility=\"yes\" android:id=\"@+id/android_logo2\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
            + "    <Button android:text=\"Button\" android:id=\"@+id/button2\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"></Button>\n"
            + "    <Button android:id=\"@+android:id/summary\" android:contentDescription=\"@string/label\" />\n"
            + "    <ImageButton android:importantForAccessibility=\"no\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
            + "</LinearLayout>\n");

    @Language("XML")
    private static final String LAYOUT_XML = ""
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
            + "</LinearLayout>\n";


    private TestFile mLayout1 = xml("res/layout/main.xml", LAYOUT_XML);
    private TestFile mOther = xml("res/layout/other.xml", LAYOUT_XML);

    @SuppressWarnings("all") // Sample code
    private TestFile mR = java(""
            + "package my.pkg;\n"
            + "\n"
            + "public final class R {\n"
            + "    public static final class attr {\n"
            + "    }\n"
            + "    public static final class drawable {\n"
            + "        public static final int ic_launcher=0x7f020000;\n"
            + "    }\n"
            + "    public static final class id {\n"
            + "        public static final int button1=0x7f050000;\n"
            + "        public static final int button2=0x7f050004;\n"
            + "        public static final int imageView1=0x7f050003;\n"
            + "        public static final int include1=0x7f050005;\n"
            + "        public static final int linearLayout1=0x7f050001;\n"
            + "        public static final int linearLayout2=0x7f050002;\n"
            + "    }\n"
            + "    public static final class layout {\n"
            // Not final: happens in libraries. Make sure we handle it correctly.
            + "        public static int main=0x7f030000;\n"
            + "        public static int other=0x7f030001;\n"
            + "    }\n"
            + "    public static final class string {\n"
            + "        public static final int app_name=0x7f040001;\n"
            + "        public static final int hello=0x7f040000;\n"
            + "    }\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mTest = java(""
            + "package my.pgk;\n"
            + "\n"
            + "class Test {\n"
            + "   private static String s = \" R.id.button1 \\\" \"; // R.id.button1 should not be considered referenced\n"
            + "   static {\n"
            + "       System.out.println(R.id.button2);\n"
            + "       char c = '\"';\n"
            + "       System.out.println(R.id.linearLayout1);\n"
            + "   }\n"
            + "}\n");

    private TestFile mLibraryManifest = xml("AndroidManifest.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    package=\"foo.library\"\n"
            + "    android:versionCode=\"1\"\n"
            + "    android:versionName=\"1.0\" >\n"
            + "\n"
            + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
            + "\n"
            + "    <application\n"
            + "        android:icon=\"@drawable/ic_launcher\"\n"
            + "        android:label=\"@string/app_name\" >\n"
            + "        <activity\n"
            + "            android:name=\".LibraryProjectActivity\"\n"
            + "            android:label=\"@string/app_name\" >\n"
            + "            <intent-filter>\n"
            + "                <action android:name=\"android.intent.action.MAIN\" />\n"
            + "\n"
            + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
            + "            </intent-filter>\n"
            + "        </activity>\n"
            + "\n"
            + "        <!-- Dummy string references for unused resource check -->\n"
            + "        <meta-data\n"
            + "            android:name=\"com.google.android.backup.api_key\"\n"
            + "            android:value=\"@string/string3\" />\n"
            + "        <meta-data\n"
            + "            android:name=\"foo\"\n"
            + "            android:value=\"@string/string1\" />\n"
            + "    </application>\n"
            + "\n"
            + "</manifest>\n");

    private TestFile mLibraryStrings = xml("res/values/strings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "\n"
            + "    <string name=\"app_name\">LibraryProject</string>\n"
            + "    <string name=\"string1\">String 1</string>\n"
            + "    <string name=\"string2\">String 2</string>\n"
            + "    <string name=\"string3\">String 3</string>\n"
            + "\n"
            + "</resources>\n");

    private TestFile mLibraryCode = java("src/foo/library/LibraryCode.java", ""
            + "package foo.library;\n"
            + "\n"
            + "public class LibraryCode {\n"
            + "    static {\n"
            + "        System.out.println(R.string.string1);\n"
            + "    }\n"
            + "}\n");

    private TestFile mMainCode = java("src/foo/main/MainCode.java", ""
            + "package foo.main;\n"
            + "\n"
            + "public class MainCode {\n"
            + "    static {\n"
            + "        System.out.println(R.string.string2);\n"
            + "    }\n"
            + "}\n");

    private TestFile mStrings2 = xml("res/values/strings2.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <string name=\"hello\">Hello</string>\n"
            + "</resources>\n"
            + "\n");
}
