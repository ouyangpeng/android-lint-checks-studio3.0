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

package com.android.tools.lint;

import static com.android.tools.lint.LintCliFlags.ERRNO_CREATED_BASELINE;
import static com.android.tools.lint.LintCliFlags.ERRNO_EXISTS;
import static com.android.tools.lint.LintCliFlags.ERRNO_INVALID_ARGS;
import static com.android.tools.lint.LintCliFlags.ERRNO_SUCCESS;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.checks.AbstractCheckTest;
import com.android.tools.lint.checks.AccessibilityDetector;
import com.android.tools.lint.client.api.LintDriver;
import com.android.tools.lint.client.api.LintListener;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import org.intellij.lang.annotations.Language;

@SuppressWarnings("javadoc")
public class MainTest extends AbstractCheckTest {
    public interface Cleanup {
        String cleanup(String s);
    }

    @Override
    public String cleanup(String result) {
        return super.cleanup(result);
    }

    private void checkDriver(String expectedOutput, String expectedError, int expectedExitCode,
            String[] args) {
        checkDriver(expectedOutput, expectedError, expectedExitCode, args, MainTest.this::cleanup,
                null);
    }

    public static void checkDriver(
            String expectedOutput,
            String expectedError,
            int expectedExitCode,
            String[] args,
            @Nullable Cleanup cleanup,
            @Nullable LintListener listener) {

        PrintStream previousOut = System.out;
        PrintStream previousErr = System.err;
        try {
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            System.setOut(new PrintStream(output));
            final ByteArrayOutputStream error = new ByteArrayOutputStream();
            System.setErr(new PrintStream(error));

            int exitCode = 0;
            try {
                Main main = new Main() {
                    @Override
                    protected void initializeDriver(@NonNull LintDriver driver) {
                        super.initializeDriver(driver);
                        if (listener != null) {
                            driver.addLintListener(listener);
                        }
                    }
                };
                main.run(args);
            } catch (Main.ExitException e) {
                exitCode = e.getStatus();
            }

            String stderr = error.toString();
            if (cleanup != null) {
                stderr = cleanup.cleanup(stderr);
            }
            if (!expectedError.trim().equals(stderr.trim())) {
                assertEquals(expectedError, stderr); // instead of fail: get difference in output
            }
            if (expectedOutput != null) {
                String stdout = output.toString();
                if (cleanup != null) {
                    stdout = cleanup.cleanup(stdout);
                }
                if (!expectedOutput.trim().equals(stdout.trim())) {
                    assertEquals(expectedOutput, stdout);
                }
            }
            assertEquals(expectedExitCode, exitCode);
        } finally {
            System.setOut(previousOut);
            System.setErr(previousErr);
        }
    }

    public void testArguments() throws Exception {
        checkDriver(
        // Expected output
        "\n" +
        "Scanning MainTest_testArguments: .\n" +
        "res/layout/accessibility.xml:4: Warning: Missing contentDescription attribute on image [ContentDescription]\n" +
        "    <ImageView android:id=\"@+id/android_logo\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n" +
        "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
        "res/layout/accessibility.xml:5: Warning: Missing contentDescription attribute on image [ContentDescription]\n" +
        "    <ImageButton android:importantForAccessibility=\"yes\" android:id=\"@+id/android_logo2\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n" +
        "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
        "0 errors, 2 warnings\n",

        // Expected error
        "",

        // Expected exit code
        ERRNO_SUCCESS,

        // Args
        new String[] {
                "--check",
                "ContentDescription",
                "--disable",
                "LintError",
                getProjectDir(null, mAccessibility).getPath()

        });
    }

    public void testShowDescription() throws Exception {
        checkDriver(
        // Expected output
        "NewApi\n" +
        "------\n" +
        "Summary: Calling new methods on older versions\n" +
        "\n" +
        "Priority: 6 / 10\n" +
        "Severity: Error\n" +
        "Category: Correctness\n" +
        "\n" +
        "This check scans through all the Android API calls in the application and\n" +
        "warns about any calls that are not available on all versions targeted by this\n" +
        "application (according to its minimum SDK attribute in the manifest).\n" +
        "\n" +
        "If you really want to use this API and don't need to support older devices\n" +
        "just set the minSdkVersion in your build.gradle or AndroidManifest.xml files.\n" +
        "\n" +
        "If your code is deliberately accessing newer APIs, and you have ensured (e.g.\n" +
        "with conditional execution) that this code will only ever be called on a\n" +
        "supported platform, then you can annotate your class or method with the\n" +
        "@TargetApi annotation specifying the local minimum SDK to apply, such as\n" +
        "@TargetApi(11), such that this check considers 11 rather than your manifest\n" +
        "file's minimum SDK as the required API level.\n" +
        "\n" +
        "If you are deliberately setting android: attributes in style definitions, make\n" +
        "sure you place this in a values-vNN folder in order to avoid running into\n" +
        "runtime conflicts on certain devices where manufacturers have added custom\n" +
        "attributes whose ids conflict with the new ones on later platforms.\n" +
        "\n" +
        "Similarly, you can use tools:targetApi=\"11\" in an XML file to indicate that\n" +
        "the element will only be inflated in an adequate context.\n" +
        "\n" +
        "\n",

        // Expected error
        "",

        // Expected exit code
        ERRNO_SUCCESS,

        // Args
        new String[] {
                "--show",
                "NewApi"
        });
    }

    public void testShowDescriptionWithUrl() throws Exception {
        checkDriver(""
                // Expected output
                + "SdCardPath\n"
                + "----------\n"
                + "Summary: Hardcoded reference to /sdcard\n"
                + "\n"
                + "Priority: 6 / 10\n"
                + "Severity: Warning\n"
                + "Category: Correctness\n"
                + "\n"
                + "Your code should not reference the /sdcard path directly; instead use\n"
                + "Environment.getExternalStorageDirectory().getPath().\n"
                + "\n"
                + "Similarly, do not reference the /data/data/ path directly; it can vary in\n"
                + "multi-user scenarios. Instead, use Context.getFilesDir().getPath().\n"
                + "\n"
                + "More information: \n"
                + "http://developer.android.com/guide/topics/data/data-storage.html#filesExternal\n"
                + "\n",

                // Expected error
                "",

                // Expected exit code
                ERRNO_SUCCESS,

                // Args
                new String[] {
                        "--show",
                        "SdCardPath"
                });
    }

    public void testNonexistentLibrary() throws Exception {
        checkDriver(
        "",
        "Library foo.jar does not exist.\n",

        // Expected exit code
        ERRNO_INVALID_ARGS,

        // Args
        new String[] {
                "--libraries",
                "foo.jar",
                "prj"

        });
    }

    public void testMultipleProjects() throws Exception {
        File project = getProjectDir(null,
                jar("libs/classes.jar")); // dummy file

        checkDriver(
        "",
        "The --sources, --classpath, --libraries and --resources arguments can only be used with a single project\n",

        // Expected exit code
        ERRNO_INVALID_ARGS,

        // Args
        new String[] {
                "--libraries",
                new File(project, "libs/classes.jar").getPath(),
                "--disable",
                "LintError",
                project.getPath(),
                project.getPath()

        });
    }

    public void testCustomResourceDirs() throws Exception {
        File project = getProjectDir(null,
                mAccessibility2,
                mAccessibility3
        );

        checkDriver(
                "\n"
                + "Scanning MainTest_testCustomResourceDirs: ..\n"
                + "myres1/layout/accessibility1.xml:4: Warning: Missing contentDescription attribute on image [ContentDescription]\n"
                + "    <ImageView android:id=\"@+id/android_logo\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "myres2/layout/accessibility1.xml:4: Warning: Missing contentDescription attribute on image [ContentDescription]\n"
                + "    <ImageView android:id=\"@+id/android_logo\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "myres1/layout/accessibility1.xml:5: Warning: Missing contentDescription attribute on image [ContentDescription]\n"
                + "    <ImageButton android:importantForAccessibility=\"yes\" android:id=\"@+id/android_logo2\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "myres2/layout/accessibility1.xml:5: Warning: Missing contentDescription attribute on image [ContentDescription]\n"
                + "    <ImageButton android:importantForAccessibility=\"yes\" android:id=\"@+id/android_logo2\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 4 warnings\n", // Expected output
                "",

                // Expected exit code
                ERRNO_SUCCESS,

                // Args
                new String[] {
                        "--check",
                        "ContentDescription",
                        "--disable",
                        "LintError",
                        "--resources",
                        new File(project, "myres1").getPath(),
                        "--resources",
                        new File(project, "myres2").getPath(),
                        "--compile-sdk-version",
                        "15",
                        project.getPath(),
                });
    }

    public void testPathList() throws Exception {
        File project = getProjectDir(null,
                mAccessibility2,
                mAccessibility3
        );

        checkDriver(
                "\n"
                + "Scanning MainTest_testPathList: ..\n"
                + "myres1/layout/accessibility1.xml:4: Warning: Missing contentDescription attribute on image [ContentDescription]\n"
                + "    <ImageView android:id=\"@+id/android_logo\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "myres2/layout/accessibility1.xml:4: Warning: Missing contentDescription attribute on image [ContentDescription]\n"
                + "    <ImageView android:id=\"@+id/android_logo\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "myres1/layout/accessibility1.xml:5: Warning: Missing contentDescription attribute on image [ContentDescription]\n"
                + "    <ImageButton android:importantForAccessibility=\"yes\" android:id=\"@+id/android_logo2\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "myres2/layout/accessibility1.xml:5: Warning: Missing contentDescription attribute on image [ContentDescription]\n"
                + "    <ImageButton android:importantForAccessibility=\"yes\" android:id=\"@+id/android_logo2\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 4 warnings\n", // Expected output
                "",

                // Expected exit code
                ERRNO_SUCCESS,

                // Args
                new String[] {
                        "--check",
                        "ContentDescription",
                        "--disable",
                        "LintError",
                        "--resources",
                        // Combine two paths with a single separator here
                        new File(project, "myres1").getPath()
                            + ':' + new File(project, "myres2").getPath(),
                        project.getPath(),
                });
    }

    public void testClassPath() throws Exception {
        File project = getProjectDir(null,
                manifest().minSdk(1),
                mGetterTest,
                mGetterTest2
        );
        checkDriver(
        "\n" +
        "Scanning MainTest_testClassPath: \n" +
        "src/test/bytecode/GetterTest.java:47: Warning: Calling getter method getFoo1() on self is slower than field access (mFoo1) [FieldGetter]\n" +
        "  getFoo1();\n" +
        "  ~~~~~~~\n" +
        "src/test/bytecode/GetterTest.java:48: Warning: Calling getter method getFoo2() on self is slower than field access (mFoo2) [FieldGetter]\n" +
        "  getFoo2();\n" +
        "  ~~~~~~~\n" +
        "src/test/bytecode/GetterTest.java:52: Warning: Calling getter method isBar1() on self is slower than field access (mBar1) [FieldGetter]\n" +
        "  isBar1();\n" +
        "  ~~~~~~\n" +
        "src/test/bytecode/GetterTest.java:54: Warning: Calling getter method getFoo1() on self is slower than field access (mFoo1) [FieldGetter]\n" +
        "  this.getFoo1();\n" +
        "       ~~~~~~~\n" +
        "src/test/bytecode/GetterTest.java:55: Warning: Calling getter method getFoo2() on self is slower than field access (mFoo2) [FieldGetter]\n" +
        "  this.getFoo2();\n" +
        "       ~~~~~~~\n" +
        "0 errors, 5 warnings\n",
        "",

        // Expected exit code
        ERRNO_SUCCESS,

        // Args
        new String[] {
                "--check",
                "FieldGetter",
                "--classpath",
                new File(project, "bin/classes.jar").getPath(),
                "--disable",
                "LintError",
                project.getPath()
        });
    }

    public void testLibraries() throws Exception {
        File project = getProjectDir(null,
                manifest().minSdk(1),
                mGetterTest,
                mGetterTest2
        );
        checkDriver(
        "\n" +
        "Scanning MainTest_testLibraries: \n" +
        "No issues found.\n",
        "",

        // Expected exit code
        ERRNO_SUCCESS,

        // Args
        new String[] {
                "--check",
                "FieldGetter",
                "--libraries",
                new File(project, "bin/classes.jar").getPath(),
                "--disable",
                "LintError",
                project.getPath()
        });
    }

    public void testCreateBaseline() throws Exception {
        File baseline = File.createTempFile("baseline", "xml");
        //noinspection ResultOfMethodCallIgnored
        baseline.delete(); // shouldn't exist
        assertFalse(baseline.exists());
        //noinspection ConcatenationWithEmptyString
        checkDriver(
                // Expected output
                null,

                // Expected error
                ""
                        + "Created baseline file " + cleanup(baseline.getPath()) + "\n"
                        + "\n"
                        + "Also breaking the build in case this was not intentional. If you\n"
                        + "deliberately created the baseline file, re-run the build and this\n"
                        + "time it should succeed without warnings.\n"
                        + "\n"
                        + "If not, investigate the baseline path in the lintOptions config\n"
                        + "or verify that the baseline file has been checked into version\n"
                        + "control.\n"
                        + "\n",

                // Expected exit code
                ERRNO_CREATED_BASELINE,

                // Args
                new String[]{
                        "--check",
                        "ContentDescription",
                        "--baseline",
                        baseline.getPath(),
                        "--disable",
                        "LintError",
                        getProjectDir(null, mAccessibility).getPath()

                });
        assertTrue(baseline.exists());
        //noinspection ResultOfMethodCallIgnored
        baseline.delete();
    }


    @Override
    protected Detector getDetector() {
        // Sample issue to check by the main driver
        return new AccessibilityDetector();
    }

    public void test_getCleanPath() throws Exception {
        assertEquals("foo", LintCliClient.getCleanPath(new File("foo")));
        String sep = File.separator;
        assertEquals("foo" + sep + "bar",
                LintCliClient.getCleanPath(new File("foo" + sep + "bar")));
        assertEquals(sep,
                LintCliClient.getCleanPath(new File(sep)));
        assertEquals("foo" + sep + "bar",
                LintCliClient.getCleanPath(new File("foo" + sep + "." + sep + "bar")));
        assertEquals("bar",
                LintCliClient.getCleanPath(new File("foo" + sep + ".." + sep + "bar")));
        assertEquals("",
                LintCliClient.getCleanPath(new File("foo" + sep + "..")));
        assertEquals("foo",
                LintCliClient.getCleanPath(new File("foo" + sep + "bar" + sep + "..")));
        assertEquals("foo" + sep + ".foo" + sep + "bar",
                LintCliClient.getCleanPath(new File("foo" + sep + ".foo" + sep + "bar")));
        assertEquals("foo" + sep + "bar",
                LintCliClient.getCleanPath(new File("foo" + sep + "bar" + sep + ".")));
        assertEquals("foo" + sep + "...",
                LintCliClient.getCleanPath(new File("foo" + sep + "...")));
        assertEquals(".." + sep + "foo",
                LintCliClient.getCleanPath(new File(".." + sep + "foo")));
        assertEquals(sep + "foo",
                LintCliClient.getCleanPath(new File(sep + "foo")));
        assertEquals(sep,
                LintCliClient.getCleanPath(new File(sep + "foo" + sep + "..")));
        assertEquals(sep + "foo",
                LintCliClient.getCleanPath(new File(sep + "foo" + sep + "bar " + sep + "..")));

        if (SdkConstants.CURRENT_PLATFORM != SdkConstants.PLATFORM_WINDOWS) {
            assertEquals(sep + "c:",
                    LintCliClient.getCleanPath(new File(sep + "c:")));
            assertEquals(sep + "c:" + sep + "foo",
                    LintCliClient.getCleanPath(new File(sep + "c:" + sep + "foo")));
        }
    }

    public void testGradle() throws Exception {
        File project = getProjectDir(null,
                manifest().minSdk(1),
                source("build.gradle", ""), // dummy; only name counts
                // dummy to ensure we have .class files
                source("bin/classes/foo/bar/ApiCallTest.class", "")
        );
        checkDriver(""
                + "\n"
                + "MainTest_testGradle: Error: \"MainTest_testGradle\" is a Gradle project. To correctly analyze Gradle projects, you should run \"gradlew :lint\" instead. [LintError]\n"
                + "1 errors, 0 warnings\n",

                "",

                // Expected exit code
                ERRNO_SUCCESS,

                // Args
                new String[] {
                        "--check",
                        "HardcodedText",
                        project.getPath()
                });
    }

    public void testValidateOutput() throws Exception {
        if (SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_WINDOWS) {
            // This test relies on making directories not writable, then
            // running lint pointing the output to that directory
            // and checking that error messages make sense. This isn't
            // supported on Windows; calling file.setWritable(false) returns
            // false; so skip this entire test on Windows.
            return;
        }
        File project = getProjectDir(null,
                mAccessibility2
        );

        File outputDir = new File(project, "build");
        assertTrue(outputDir.mkdirs());
        assertTrue(outputDir.setWritable(true));

        checkDriver(
                "\n"
                        + "Scanning MainTest_testValidateOutput: .\n"
                        + "Scanning MainTest_testValidateOutput (Phase 2): \n", // Expected output

                "",

                // Expected exit code
                ERRNO_SUCCESS,

                // Args
                new String[]{
                        "--text",
                        new File(outputDir, "foo2.text").getPath(),
                        project.getPath(),
                });

        //noinspection ResultOfMethodCallIgnored
        boolean disabledWrite = outputDir.setWritable(false);
        assertTrue(disabledWrite);

        checkDriver(
                "", // Expected output

                "Cannot write XML output file /TESTROOT/build/foo.xml\n", // Expected error

                // Expected exit code
                ERRNO_EXISTS,

                // Args
                new String[] {
                        "--xml",
                        new File(outputDir, "foo.xml").getPath(),
                        project.getPath(),
                });

        checkDriver(
                "", // Expected output

                "Cannot write HTML output file /TESTROOT/build/foo.html\n", // Expected error

                // Expected exit code
                ERRNO_EXISTS,

                // Args
                new String[] {
                        "--html",
                        new File(outputDir, "foo.html").getPath(),
                        project.getPath(),
                });

        checkDriver(
                "", // Expected output

                "Cannot write text output file /TESTROOT/build/foo.text\n", // Expected error

                // Expected exit code
                ERRNO_EXISTS,

                // Args
                new String[] {
                        "--text",
                        new File(outputDir, "foo.text").getPath(),
                        project.getPath(),
                });
    }

    @Override
    protected boolean isEnabled(Issue issue) {
        return true;
    }

    @Language("XML")
    private static final String ACCESSIBILITY_XML = ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" android:id=\"@+id/newlinear\" android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">\n"
            + "    <Button android:text=\"Button\" android:id=\"@+id/button1\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"></Button>\n"
            + "    <ImageView android:id=\"@+id/android_logo\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
            + "    <ImageButton android:importantForAccessibility=\"yes\" android:id=\"@+id/android_logo2\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
            + "    <Button android:text=\"Button\" android:id=\"@+id/button2\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"></Button>\n"
            + "    <Button android:id=\"@+android:id/summary\" android:contentDescription=\"@string/label\" />\n"
            + "    <ImageButton android:importantForAccessibility=\"no\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
            + "</LinearLayout>\n";

    private final TestFile mAccessibility = xml("res/layout/accessibility.xml", ACCESSIBILITY_XML);

    private final TestFile mAccessibility2 = xml("myres1/layout/accessibility1.xml", ACCESSIBILITY_XML);

    private final TestFile mAccessibility3 = xml("myres2/layout/accessibility1.xml", ACCESSIBILITY_XML);

    @SuppressWarnings("all") // Sample code
    private TestFile mGetterTest = java(""
            + "package test.bytecode;\n"
            + "\n"
            + "public class GetterTest {\n"
            + "\tprivate int mFoo1;\n"
            + "\tprivate String mFoo2;\n"
            + "\tprivate int mBar1;\n"
            + "\tprivate static int sFoo4;\n"
            + "\n"
            + "\tpublic int getFoo1() {\n"
            + "\t\treturn mFoo1;\n"
            + "\t}\n"
            + "\n"
            + "\tpublic String getFoo2() {\n"
            + "\t\treturn mFoo2;\n"
            + "\t}\n"
            + "\n"
            + "\tpublic int isBar1() {\n"
            + "\t\treturn mBar1;\n"
            + "\t}\n"
            + "\n"
            + "\t// Not \"plain\" getters:\n"
            + "\n"
            + "\tpublic String getFoo3() {\n"
            + "\t\t// NOT a plain getter\n"
            + "\t\tif (mFoo2 == null) {\n"
            + "\t\t\tmFoo2 = \"\";\n"
            + "\t\t}\n"
            + "\t\treturn mFoo2;\n"
            + "\t}\n"
            + "\n"
            + "\tpublic int getFoo4() {\n"
            + "\t\t// NOT a plain getter (using static)\n"
            + "\t\treturn sFoo4;\n"
            + "\t}\n"
            + "\n"
            + "\tpublic int getFoo5(int x) {\n"
            + "\t\t// NOT a plain getter (has extra argument)\n"
            + "\t\treturn sFoo4;\n"
            + "\t}\n"
            + "\n"
            + "\tpublic int isBar2(String s) {\n"
            + "\t\t// NOT a plain getter (has extra argument)\n"
            + "\t\treturn mFoo1;\n"
            + "\t}\n"
            + "\n"
            + "\tpublic void test() {\n"
            + "\t\tgetFoo1();\n"
            + "\t\tgetFoo2();\n"
            + "\t\tgetFoo3();\n"
            + "\t\tgetFoo4();\n"
            + "\t\tgetFoo5(42);\n"
            + "\t\tisBar1();\n"
            + "\t\tisBar2(\"foo\");\n"
            + "\t\tthis.getFoo1();\n"
            + "\t\tthis.getFoo2();\n"
            + "\t\tthis.getFoo3();\n"
            + "\t\tthis.getFoo4();\n"
            + "\t}\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mGetterTest2 = base64gzip("bin/classes.jar", ""
            + "H4sIAAAAAAAAAAvwZmYRYeAAQoFoOwcGJMDJwMLg6xriqOvp56b/7xQDAzND"
            + "gDc7B0iKCaokAKdmESCGa/Z19PN0cw0O0fN1++x75rSPt67eRV5vXa1zZ85v"
            + "DjK4YvzgaZGel6+Op+/F0lUsnDNeSh6RjtTKsBATebJEq+KZ6uvMT0UfixjB"
            + "tk83XmlkAzTbBmo7F8Q6NNtZgbgktbhEH7cSPpiSpMqS1OT8lFR9hGfQ1cph"
            + "qHVPLSlJLQoBiukl5yQWF5cGxeYLOYrYMuf8LODq2LJty62krYdWLV16wak7"
            + "d5EnL+dVdp/KuIKja3WzE7K/5P+wrglYbPrxYLhw/ZSP9xJ3q26onbmz+L3t"
            + "83mWxvX///7iXdDx14CJqbjPsoDrbX/fzY3xM1vTlz2e8Xf6FG5llQk2Zvek"
            + "W4UXX9fdkyE/W9bdwdp2w1texsDyx4scVhXevF7yK2z97tNH1d3mS21lNJ3K"
            + "siwr7HzRN5amnX8mOrzQPNut2NFyxNSj0eXwq5nnz/vdNrmfMX+GT3Z5z2Tl"
            + "xfkfb/q2zTG/5qBweYeXRS9fuW/6iklpVxcL7NBcmHhq9YRnJXr2K2dFi6sc"
            + "6pgQl31A/MGV3M4XHFXGTWsYni6f3XexsjpjT/HWnV+Fkt95HnEzSA2at/r5"
            + "SZOPD5tmh5x5oua6Yhnj/Sl5wsqrTDtN0iyips84bOPu2rk0MWRShGTYdpWw"
            + "wvmLu44opSndUGSPu222PEuo8gXTxmW1197PYBfj9ou5te2Y1YSl5xRq+wWY"
            + "ciRcGcuc3waW9n3cmvHc+tLujdwlWhf8pjlcrlf6F7pVPXNu0EmFdZe12nk9"
            + "HrLdsNl1ieWHdZp9f2PyvoSig+xzfhqx9f1uEq9Vvy81f84nVv3Kyfwro79+"
            + "fGLf8WrlU/kTMSc4tJbtKCqeZ3NGIK2wxfCp0b3AvUmzJmnPW2caHv5C+l3f"
            + "6VN9E1psIr980NvmVP2A682qQ+f4XutNWzxnFfc/RT3vq6kfayezK5vMcl8c"
            + "aLcoQ67q/6PJrwN97Y8vFtNljTOruJnz0vPWKZn87V9Cvsrs1t2/7fT7EJW4"
            + "OhPe11/0zSYs8JGaHeHAeVpjMmu0SfVsLdGuVTeOnuuIND2/5nhX4Xt7UEY4"
            + "ZPg5Pw+YD7lZQRmBkUmEATUjwrIoKBejApQ8ja4VOX+JoGizxZGjQSZwMeDO"
            + "hwiwG5ErcWvhQ9FyD0suRTgYpBc5HORQ9HIxEsq1Ad6sbBBnsjJYAFUfYQbx"
            + "AFJZ3LASBQAA");
}
