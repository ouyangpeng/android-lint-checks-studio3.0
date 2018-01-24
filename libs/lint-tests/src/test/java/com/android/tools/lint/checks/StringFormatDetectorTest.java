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

import static com.android.tools.lint.checks.StringFormatDetector.isLocaleSpecific;

import com.android.tools.lint.detector.api.Detector;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings({"javadoc", "ClassNameDiffersFromFileName"})
public class StringFormatDetectorTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new StringFormatDetector();
    }

    public void testAll() throws Exception {
        String expected = ""
                + "src/test/pkg/StringFormatActivity.java:13: Error: Wrong argument type for formatting argument '#1' in hello: conversion is 'd', received String (argument #2 in method call) [StringFormatMatches]\n"
                + "        String output1 = String.format(hello, target);\n"
                + "                                              ~~~~~~\n"
                + "    res/values-es/formatstrings.xml:3: Conflicting argument declaration here\n"
                + "src/test/pkg/StringFormatActivity.java:15: Error: Wrong argument count, format string hello2 requires 3 but format call supplies 2 [StringFormatMatches]\n"
                + "        String output2 = String.format(hello2, target, \"How are you\");\n"
                + "                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values-es/formatstrings.xml:4: This definition requires 3 arguments\n"
                + "src/test/pkg/StringFormatActivity.java:21: Error: Wrong argument type for formatting argument '#1' in score: conversion is 'd', received boolean (argument #2 in method call) (Did you mean formatting character b?) [StringFormatMatches]\n"
                + "        String output4 = String.format(score, true);  // wrong\n"
                + "                                              ~~~~\n"
                + "    res/values/formatstrings.xml:6: Conflicting argument declaration here\n"
                + "src/test/pkg/StringFormatActivity.java:22: Error: Wrong argument type for formatting argument '#1' in score: conversion is 'd', received boolean (argument #2 in method call) (Did you mean formatting character b?) [StringFormatMatches]\n"
                + "        String output  = String.format(score, won);   // wrong\n"
                + "                                              ~~~\n"
                + "    res/values/formatstrings.xml:6: Conflicting argument declaration here\n"
                + "src/test/pkg/StringFormatActivity.java:24: Error: Wrong argument count, format string hello2 requires 3 but format call supplies 2 [StringFormatMatches]\n"
                + "        String.format(getResources().getString(R.string.hello2), target, \"How are you\");\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values-es/formatstrings.xml:4: This definition requires 3 arguments\n"
                + "src/test/pkg/StringFormatActivity.java:26: Error: Wrong argument count, format string hello2 requires 3 but format call supplies 2 [StringFormatMatches]\n"
                + "        getResources().getString(R.string.hello2, target, \"How are you\");\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values-es/formatstrings.xml:4: This definition requires 3 arguments\n"
                + "src/test/pkg/StringFormatActivity.java:33: Error: Wrong argument type for formatting argument '#1' in hello: conversion is 'd', received String (argument #2 in method call) [StringFormatMatches]\n"
                + "        String output1 = String.format(hello, target);\n"
                + "                                              ~~~~~~\n"
                + "    res/values-es/formatstrings.xml:3: Conflicting argument declaration here\n"
                + "src/test/pkg/StringFormatActivity.java:41: Error: Wrong argument type for formatting argument '#1' in score: conversion is 'd', received Boolean (argument #2 in method call) (Did you mean formatting character b?) [StringFormatMatches]\n"
                + "        String output1  = String.format(score, won);   // wrong\n"
                + "                                               ~~~\n"
                + "    res/values/formatstrings.xml:6: Conflicting argument declaration here\n"
                + "res/values-es/formatstrings.xml:3: Error: Inconsistent formatting types for argument #1 in format string hello ('%1$d'): Found both 's' and 'd' (in values/formatstrings.xml) [StringFormatMatches]\n"
                + "    <string name=\"hello\">%1$d</string>\n"
                + "                         ~~~~\n"
                + "    res/values/formatstrings.xml:3: Conflicting argument type here\n"
                + "res/values-es/formatstrings.xml:4: Warning: Inconsistent number of arguments in formatting string hello2; found both 2 and 3 [StringFormatCount]\n"
                + "    <string name=\"hello2\">%3$d: %1$s, %2$s?</string>\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/formatstrings.xml:4: Conflicting number of arguments here\n"
                + "res/values/formatstrings.xml:5: Warning: Formatting string 'missing' is not referencing numbered arguments [1, 2] [StringFormatCount]\n"
                + "    <string name=\"missing\">Hello %3$s World</string>\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "9 errors, 2 warnings\n";
        lint().files(
                mFormatstrings,
                mFormatstrings2,
                mStringFormatActivity)
                .run()
                .expect(expected);
    }

    public void testArgCount() {
        assertEquals(0, StringFormatDetector.getFormatArgumentCount(
                "%n%% ", null));
        assertEquals(1, StringFormatDetector.getFormatArgumentCount(
                "%n%% %s", null));
        assertEquals(3, StringFormatDetector.getFormatArgumentCount(
                "First: %1$s, Second %2$s, Third %3$s", null));
        assertEquals(11, StringFormatDetector.getFormatArgumentCount(
                "Skipping stuff: %11$s", null));
        assertEquals(1, StringFormatDetector.getFormatArgumentCount(
                "First: %1$s, Skip \\%2$s", null));
        assertEquals(1, StringFormatDetector.getFormatArgumentCount(
                "First: %s, Skip \\%s", null));

        Set<Integer> indices = new HashSet<>();
        assertEquals(11, StringFormatDetector.getFormatArgumentCount(
                "Skipping stuff: %2$d %11$s", indices));
        assertEquals(2, indices.size());
        assertTrue(indices.contains(2));
        assertTrue(indices.contains(11));
    }

    public void testArgType() {
        assertEquals("s", StringFormatDetector.getFormatArgumentType(
                "First: %n%% %1$s, Second %2$s, Third %3$s", 1));
        assertEquals("s", StringFormatDetector.getFormatArgumentType(
                "First: %1$s, Second %2$s, Third %3$s", 1));
        assertEquals("d", StringFormatDetector.getFormatArgumentType(
                "First: %1$s, Second %2$-5d, Third %3$s", 2));
        assertEquals("s", StringFormatDetector.getFormatArgumentType(
                "Skipping stuff: %11$s",11));
        assertEquals("d", StringFormatDetector.getFormatArgumentType(
                "First: %1$s, Skip \\%2$s, Value=%2$d", 2));
    }

    public void testWrongSyntax() throws Exception {
        //noinspection all // Sample code
        lint().files(
                xml("res/values/formatstrings2.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name=\"hour_minute_24\">%H:%M</string>\n"
                        + "    <string name=\"numeric_wday1_mdy1_time1_wday2_mdy2_time2\">%5$s %1$s, %4$s-%2$s-%3$s \u2013 %10$s %6$s, %9$s-%7$s-%8$s</string>\n"
                        + "    <string name=\"bogus\">%2.99999s</string>\n"
                        + "</resources>\n"))
                .run()
                .expectClean();
    }

    public void testDateStrings() throws Exception {
        //noinspection all // Sample code
        lint().files(
                mFormatstrings_version1,
                xml("res/values/donottranslate-cldr.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name=\"hour_minute_24\">%H:%M</string>\n"
                        + "    <string name=\"numeric_date\">%-m/%-e/%Y</string>\n"
                        + "    <string name=\"month_day_year\">%B %-e, %Y</string>\n"
                        + "</resources>\n"))
                .run()
                .expectClean();
    }

    public void testUa() throws Exception {
        //noinspection all // Sample code
        lint().files(
                mFormatstrings_version1,
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.content.Context;\n"
                        + "\n"
                        + "public class StringFormat2 extends Activity {\n"
                        + "    public static final String buildUserAgent(Context context) {\n"
                        + "        StringBuilder arg = new StringBuilder();\n"
                        + "        // Snip\n"
                        + "        final String base = context.getResources().getText(R.string.web_user_agent).toString();\n"
                        + "        String ua = String.format(base, arg);\n"
                        + "        return ua;\n"
                        + "    }\n"
                        + "\n"
                        + "    public static final class R {\n"
                        + "        public static final class string {\n"
                        + "            public static final int web_user_agent = 0x7f0a000e;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expectClean();
    }

    public void testSuppressed() throws Exception {
        //noinspection ClassNameDiffersFromFileName,ConstantConditions
        lint().files(
                xml("res/values/formatstrings.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <string name=\"hello\" tools:ignore=\"StringFormatMatches\">Hello %1$s</string>\n"
                        + "    <string name=\"hello2\" tools:ignore=\"StringFormatMatches,StringFormatCount\">Hello %1$s, %2$s?</string>\n"
                        + "    <string name=\"missing\" tools:ignore=\"StringFormatCount\">Hello %3$s World</string>\n"
                        + "    <string name=\"score\">Score: %1$d</string>\n"
                        + "</resources>\n"),
                xml("res/values-es/formatstrings.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <string name=\"hello\" tools:ignore=\"StringFormatMatches\">%1$d</string>\n"
                        + "    <string name=\"hello2\" tools:ignore=\"StringFormatMatches,StringFormatCount\">%3$d: %1$s, %2$s?</string>\n"
                        + "</resources>\n"),
                java("src/test/pkg/StringFormatActivity.java", ""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.annotation.SuppressLint;\n"
                        + "import android.app.Activity;\n"
                        + "import android.os.Bundle;\n"
                        + "\n"
                        + "public class StringFormatActivity extends Activity {\n"
                        + "    /** Called when the activity is first created. */\n"
                        + "    @SuppressLint(\"all\")\n"
                        + "    @Override\n"
                        + "    public void onCreate(Bundle savedInstanceState) {\n"
                        + "        super.onCreate(savedInstanceState);\n"
                        + "        String target = \"World\";\n"
                        + "        String hello = getResources().getString(R.string.hello);\n"
                        + "        String output1 = String.format(hello, target);\n"
                        + "        String hello2 = getResources().getString(R.string.hello2);\n"
                        + "        String output2 = String.format(hello2, target, \"How are you\");\n"
                        + "        setContentView(R.layout.main);\n"
                        + "        String score = getResources().getString(R.string.score);\n"
                        + "        int points = 50;\n"
                        + "        boolean won = true;\n"
                        + "        String output3 = String.format(score, points);\n"
                        + "        String output4 = String.format(score, true);  // wrong\n"
                        + "        String output  = String.format(score, won);   // wrong\n"
                        + "        String output5 = String.format(score, 75);\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class R {\n"
                        + "        private static class string {\n"
                        + "            public static final int hello = 1;\n"
                        + "            public static final int hello2 = 2;\n"
                        + "            public static final int score = 3;\n"
                        + "        }\n"
                        + "        private static class layout {\n"
                        + "            public static final int main = 4;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expectClean();
    }

    public void testIssue27108() throws Exception {
        lint().files(mFormatstrings3).run().expectClean();
    }

    public void testIssue39758() throws Exception {
        //noinspection all // Sample code
        lint().files(
                xml("res/values/formatstrings4.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name=\"formattest1\">\"%1$s, %2$tF %3$tR\"</string>\n"
                        + "    <string name=\"formattest2\">%1$s, %2$tF %3$tR</string>\n"
                        + "    <string name=\"formattest3\">\"Note: Start point RPM differs by %d%%.\\n\\n\"</string>\n"
                        + "    <string name=\"formattest4\">Note: Start point RPM differs by %d%%.\\n\\n</string>\n"
                        + "    <string name=\"formattest5\">%s%% c</string>\n"
                        + "    <string name=\"formattest6\">%s%% </string>\n"
                        + "    <string name=\"formattest7\">%1$s%%</string>\n"
                        + "</resources>\n"),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.os.Bundle;\n"
                        + "\n"
                        + "public class StringFormatActivity2 extends Activity {\n"
                        + "    /** Called when the activity is first created. */\n"
                        + "    @Override\n"
                        + "    public void onCreate(Bundle savedInstanceState) {\n"
                        + "        super.onCreate(savedInstanceState);\n"
                        + "        String target = \"World\";\n"
                        + "        getResources().getString(R.string.formattest1, \"hello\");\n"
                        + "        getResources().getString(R.string.formattest2, \"hello\");\n"
                        + "        getResources().getString(R.string.formattest3, 42);\n"
                        + "        getResources().getString(R.string.formattest4, 42);\n"
                        + "        getResources().getString(R.string.formattest5, \"hello\");\n"
                        + "        getResources().getString(R.string.formattest6, \"hello\");\n"
                        + "        getResources().getString(R.string.formattest7, \"hello\");\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class R {\n"
                        + "        private static class string {\n"
                        + "            public static final int formattest1 = 1;\n"
                        + "            public static final int formattest2 = 2;\n"
                        + "            public static final int formattest3 = 3;\n"
                        + "            public static final int formattest4 = 4;\n"
                        + "            public static final int formattest5 = 5;\n"
                        + "            public static final int formattest6 = 6;\n"
                        + "            public static final int formattest7 = 7;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expectClean();
    }

    public void testIssue42798() throws Exception {
        // http://code.google.com/p/android/issues/detail?id=42798
        // String playsCount = String.format(Locale.FRANCE, this.context.getString(R.string.gridview_views_count), article.playsCount);
        String expected = ""
                + "src/test/pkg/StringFormat3.java:12: Error: Wrong argument type for formatting argument '#1' in gridview_views_count: conversion is 'd', received String (argument #3 in method call) [StringFormatMatches]\n"
                + "                context.getString(R.string.gridview_views_count), article.playsCount);\n"
                + "                                                                  ~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/formatstrings5.xml:3: Conflicting argument declaration here\n"
                + "src/test/pkg/StringFormat3.java:16: Error: Wrong argument type for formatting argument '#1' in gridview_views_count: conversion is 'd', received String (argument #3 in method call) [StringFormatMatches]\n"
                + "                context.getString(R.string.gridview_views_count), \"wrong\");\n"
                + "                                                                  ~~~~~~~\n"
                + "    res/values/formatstrings5.xml:3: Conflicting argument declaration here\n"
                + "src/test/pkg/StringFormat3.java:17: Error: Wrong argument type for formatting argument '#1' in gridview_views_count: conversion is 'd', received String (argument #2 in method call) [StringFormatMatches]\n"
                + "        String s4 = String.format(context.getString(R.string.gridview_views_count), \"wrong\");\n"
                + "                                                                                    ~~~~~~~\n"
                + "    res/values/formatstrings5.xml:3: Conflicting argument declaration here\n"
                + "src/test/pkg/StringFormat3.java:22: Error: Wrong argument type for formatting argument '#1' in gridview_views_count: conversion is 'd', received String (argument #3 in method call) [StringFormatMatches]\n"
                + "                context.getString(R.string.gridview_views_count), \"string\");\n"
                + "                                                                  ~~~~~~~~\n"
                + "    res/values/formatstrings5.xml:3: Conflicting argument declaration here\n"
                + "res/values/formatstrings5.xml:3: Warning: Formatting %d followed by words (\"vues\"): This should probably be a plural rather than a string [PluralsCandidate]\n"
                + "    <string name=\"gridview_views_count\">%d vues</string>\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "4 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/values/formatstrings5.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name=\"gridview_views_count\">%d vues</string>\n"
                        + "</resources>\n"),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.content.Context;\n"
                        + "\n"
                        + "import java.util.Locale;\n"
                        + "\n"
                        + "public class StringFormat3 extends Activity {\n"
                        + "    public final void test(Context context) {\n"
                        + "        Article article = new Article();\n"
                        + "        String s1 = String.format(Locale.FRANCE,\n"
                        + "                context.getString(R.string.gridview_views_count), article.playsCount);\n"
                        + "        String s2 = String.format(Locale.FRANCE,\n"
                        + "                context.getString(R.string.gridview_views_count), 5);\n"
                        + "        String s3 = String.format(Locale.FRANCE,\n"
                        + "                context.getString(R.string.gridview_views_count), \"wrong\");\n"
                        + "        String s4 = String.format(context.getString(R.string.gridview_views_count), \"wrong\");\n"
                        + "        String s5 = String.format(context.getString(R.string.gridview_views_count), 5); // OK\n"
                        + "        String s6 = String.format(Locale.getDefault(),\n"
                        + "                context.getString(R.string.gridview_views_count), 5);\n"
                        + "        String s7 = String.format((Locale) null,\n"
                        + "                context.getString(R.string.gridview_views_count), \"string\");\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class Article {\n"
                        + "        String playsCount;\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class R {\n"
                        + "        private static class string {\n"
                        + "            public static final int gridview_views_count = 1;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expect(expected);
    }

    public void testIsLocaleSpecific() throws Exception {
        assertFalse(isLocaleSpecific(""));
        assertFalse(isLocaleSpecific("Hello World!"));
        assertFalse(isLocaleSpecific("%% %n"));
        assertFalse(isLocaleSpecific(" %%f"));
        assertFalse(isLocaleSpecific("%x %A %c %b %B %h %n %%"));
        assertTrue(isLocaleSpecific("%f"));
        assertTrue(isLocaleSpecific(" %1$f "));
        assertTrue(isLocaleSpecific(" %5$e "));
        assertTrue(isLocaleSpecific(" %E "));
        assertTrue(isLocaleSpecific(" %g "));
        assertTrue(isLocaleSpecific(" %1$tm %1$te,%1$tY "));
    }

    public void testGetStringAsParameter() throws Exception {
        String expected = ""
                + "src/test/pkg/StringFormat4.java:11: Error: Wrong argument count, format string error_and_source requires 2 but format call supplies 1 [StringFormatMatches]\n"
                + "        getString(R.string.error_and_source, getString(R.string.data_source)); // ERROR\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/formatstrings6.xml:24: This definition requires 2 arguments\n"
                + "src/test/pkg/StringFormat4.java:13: Error: Wrong argument count, format string error_and_source requires 2 but format call supplies 1 [StringFormatMatches]\n"
                + "        getString(R.string.error_and_source, \"data source\"); // ERROR\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/formatstrings6.xml:24: This definition requires 2 arguments\n"
                + "2 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/values/formatstrings6.xml", ""
                        + "<!--\n"
                        + "  ~ Copyright (C) 2013 The Android Open Source Project\n"
                        + "  ~\n"
                        + "  ~ Licensed under the Apache License, Version 2.0 (the \"License\");\n"
                        + "  ~ you may not use this file except in compliance with the License.\n"
                        + "  ~ You may obtain a copy of the License at\n"
                        + "  ~\n"
                        + "  ~      http://www.apache.org/licenses/LICENSE-2.0\n"
                        + "  ~\n"
                        + "  ~ Unless required by applicable law or agreed to in writing, software\n"
                        + "  ~ distributed under the License is distributed on an \"AS IS\" BASIS,\n"
                        + "  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
                        + "  ~ See the License for the specific language governing permissions and\n"
                        + "  ~ limitations under the License.\n"
                        + "  -->\n"
                        + "<resources>\n"
                        + "\t<string name=\"store\">Teh Store</string>\n"
                        + "    <string name=\"app_name\">Demo2</string>\n"
                        + "    <string name=\"hello_world\">Hello world!</string>\n"
                        + "    <string name=\"menu_settings\">Settings</string>\n"
                        + "    <string name=\"title_activity_demo\">DemoActivity</string>\n"
                        + "    <string name=\"message1\">The total of %1$s is %2$d</string>\n"
                        + "    <string name=\"message2\">Add %1$s is %2$d</string>\n"
                        + "    <string name=\"error_and_source\">Add %1$s is %2$d</string>\n"
                        + "    <string name=\"data_source\">Foo</string>\n"
                        + "    <string name=\"preferences_about_app_title\">%1$s version %2$s</string>\n"
                        + "</resources>\n"),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.content.Context;\n"
                        + "\n"
                        + "public class StringFormat4 extends Activity {\n"
                        + "    public final void test(Context context) {\n"
                        + "        // data_source takes 0 formatting arguments\n"
                        + "        // error_and_source takes two formatting arguments\n"
                        + "        // preferences_about_app_title takes two formatting arguments\n"
                        + "        getString(R.string.error_and_source, getString(R.string.data_source)); // ERROR\n"
                        + "        getString(R.string.error_and_source, getString(R.string.data_source), 5); // OK\n"
                        + "        getString(R.string.error_and_source, \"data source\"); // ERROR\n"
                        + "        getString(R.string.error_and_source, \"data source\", 5); // OK\n"
                        + "        String.format(getString(R.string.preferences_about_app_title), getString(R.string.app_name), \"\"); // OK\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class R {\n"
                        + "        private static class string {\n"
                        + "            public static final int error_and_source = 1;\n"
                        + "            public static final int data_source = 2;\n"
                        + "            public static final int preferences_about_app_title = 3;\n"
                        + "            public static final int app_name = 4;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expect(expected);
    }

    public void testNotLocaleMethod() throws Exception {
        // https://code.google.com/p/android/issues/detail?id=53238
        //noinspection all // Sample code
        lint().files(
                xml("res/values/formatstrings7.xml", ""
                        + "<resources>\n"
                        + "    <string name=\"VibrationLevelIs\">Vibration level is %s.</string>\n"
                        + "</resources>\n"),
                java(""
                        + "package test.pkg;\n"
                        + "import android.app.Activity;\n"
                        + "import android.content.Context;\n"
                        + "import android.content.res.Resources;\n"
                        + "\n"
                        + "public class StringFormat5 extends Activity {\n"
                        + "    public final void test(Context context) {\n"
                        + "        Resources resources = getResources();\n"
                        + "        String string = resources.getString(R.string.VibrationLevelIs, resources.getString(PolarPoint.textResourceForIPS()));\n"
                        + "        System.out.println(string);\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class PolarPoint {\n"
                        + "        public static int textResourceForIPS() {\n"
                        + "            return R.string.app_name;\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class R {\n"
                        + "        private static class string {\n"
                        + "            public static final int VibrationLevelIs = 1;\n"
                        + "            public static final int app_name = 2;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expectClean();
    }

    public void testNewlineChar() throws Exception {
        // https://code.google.com/p/android/issues/detail?id=65692
        String expected = ""
                + "src/test/pkg/StringFormat8.java:12: Error: Wrong argument count, format string amount_string requires 1 but format call supplies 0 [StringFormatMatches]\n"
                + "        String amount4 = String.format(getResources().getString(R.string.amount_string));  // ERROR\n"
                + "                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/formatstrings8.xml:2: This definition requires 1 arguments\n"
                + "src/test/pkg/StringFormat8.java:13: Error: Wrong argument count, format string amount_string requires 1 but format call supplies 2 [StringFormatMatches]\n"
                + "        String amount5 = getResources().getString(R.string.amount_string, amount, amount); // ERROR\n"
                + "                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/formatstrings8.xml:2: This definition requires 1 arguments\n"
                + "2 errors, 0 warnings\n";

        //noinspection all // Sample code
        lint().files(
                xml("res/values/formatstrings8.xml", ""
                        + "<resources>\n"
                        + "    <string name=\"amount_string\">Amount: $%1$.2f %n</string>\n"
                        + "    <string name=\"percent_newline\">%%%n%n%n</string>\n"
                        + "</resources>\n"),
                java(""
                        + "package test.pkg;\n"
                        + "import android.app.Activity;\n"
                        + "import android.content.Context;\n"
                        + "import android.content.res.Resources;\n"
                        + "\n"
                        + "public class StringFormat8 extends Activity {\n"
                        + "    public final void test(Context context, float amount, Resources resource) {\n"
                        + "        Resources resources = getResources();\n"
                        + "        String amount1 = resources.getString(R.string.amount_string, amount);\n"
                        + "        String amount2 = getResources().getString(R.string.amount_string, amount);\n"
                        + "        String amount3 = String.format(getResources().getString(R.string.amount_string), amount);\n"
                        + "        String amount4 = String.format(getResources().getString(R.string.amount_string));  // ERROR\n"
                        + "        String amount5 = getResources().getString(R.string.amount_string, amount, amount); // ERROR\n"
                        + "        String misc = String.format(resource.getString(R.string.percent_newline));\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class R {\n"
                        + "        private static class string {\n"
                        + "            public static final int amount_string = 1;\n"
                        + "            public static final int percent_newline = 2;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expect(expected);
    }

    public void testIncremental() throws Exception {
        String expected = ""
                + "src/test/pkg/StringFormatActivity.java:13: Error: Wrong argument type for formatting argument '#1' in hello: conversion is 'd', received String (argument #2 in method call) [StringFormatMatches]\n"
                + "        String output1 = String.format(hello, target);\n"
                + "                                              ~~~~~~\n"
                + "    res/values-es/formatstrings.xml: Conflicting argument declaration here\n"
                + "src/test/pkg/StringFormatActivity.java:15: Error: Wrong argument count, format string hello2 requires 3 but format call supplies 2 [StringFormatMatches]\n"
                + "        String output2 = String.format(hello2, target, \"How are you\");\n"
                + "                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values-es/formatstrings.xml: This definition requires 3 arguments\n"
                + "src/test/pkg/StringFormatActivity.java:21: Error: Wrong argument type for formatting argument '#1' in score: conversion is 'd', received boolean (argument #2 in method call) (Did you mean formatting character b?) [StringFormatMatches]\n"
                + "        String output4 = String.format(score, true);  // wrong\n"
                + "                                              ~~~~\n"
                + "    res/values/formatstrings.xml: Conflicting argument declaration here\n"
                + "src/test/pkg/StringFormatActivity.java:22: Error: Wrong argument type for formatting argument '#1' in score: conversion is 'd', received boolean (argument #2 in method call) (Did you mean formatting character b?) [StringFormatMatches]\n"
                + "        String output  = String.format(score, won);   // wrong\n"
                + "                                              ~~~\n"
                + "    res/values/formatstrings.xml: Conflicting argument declaration here\n"
                + "src/test/pkg/StringFormatActivity.java:24: Error: Wrong argument count, format string hello2 requires 3 but format call supplies 2 [StringFormatMatches]\n"
                + "        String.format(getResources().getString(R.string.hello2), target, \"How are you\");\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values-es/formatstrings.xml: This definition requires 3 arguments\n"
                + "src/test/pkg/StringFormatActivity.java:26: Error: Wrong argument count, format string hello2 requires 3 but format call supplies 2 [StringFormatMatches]\n"
                + "        getResources().getString(R.string.hello2, target, \"How are you\");\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values-es/formatstrings.xml: This definition requires 3 arguments\n"
                + "src/test/pkg/StringFormatActivity.java:33: Error: Wrong argument type for formatting argument '#1' in hello: conversion is 'd', received String (argument #2 in method call) [StringFormatMatches]\n"
                + "        String output1 = String.format(hello, target);\n"
                + "                                              ~~~~~~\n"
                + "    res/values-es/formatstrings.xml: Conflicting argument declaration here\n"
                + "src/test/pkg/StringFormatActivity.java:41: Error: Wrong argument type for formatting argument '#1' in score: conversion is 'd', received Boolean (argument #2 in method call) (Did you mean formatting character b?) [StringFormatMatches]\n"
                + "        String output1  = String.format(score, won);   // wrong\n"
                + "                                               ~~~\n"
                + "    res/values/formatstrings.xml: Conflicting argument declaration here\n"
                + "res/values/formatstrings.xml: Error: Inconsistent formatting types for argument #1 in format string hello ('%1$s'): Found both 'd' and 's' (in values-es/formatstrings.xml) [StringFormatMatches]\n"
                + "    res/values-es/formatstrings.xml: Conflicting argument type here\n"
                + "res/values/formatstrings.xml: Warning: Inconsistent number of arguments in formatting string hello2; found both 3 and 2 [StringFormatCount]\n"
                + "    res/values-es/formatstrings.xml: Conflicting number of arguments here\n"
                + "9 errors, 1 warnings\n";
        lint().files(
                mFormatstrings,
                mFormatstrings2,
                mStringFormatActivity)
                .incremental("src/test/pkg/StringFormatActivity.java")
                .run()
                .expect(expected);
    }

    public void testNotStringFormat() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=67597
        lint().files(
                mFormatstrings3,//"res/values/formatstrings.xml",
                mShared_prefs_keys,
                mSharedPrefsTest6)
                .run()
                .expectClean();
    }

    public void testNotStringFormatIncrementally() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=67597
        lint().files(
                mFormatstrings3,//"res/values/formatstrings.xml",
                mShared_prefs_keys,
                mSharedPrefsTest6)
                .incremental("src/test/pkg/SharedPrefsFormat.java")
                .run()
                .expectClean();
    }

    public void testIncrementalNonMatch() throws Exception {
        // Regression test for scenario where the below source files would crash during
        // a string format check with
        //   java.lang.IllegalStateException: No match found
        //       at java.util.regex.Matcher.group(Matcher.java:468)
        //       at com.android.tools.lint.checks.StringFormatDetector.checkStringFormatCall(StringFormatDetector.java:1028)
        // ...
        //noinspection all // Sample code
        lint().files(
                xml("res/values/formatstrings11.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "\n"
                        + "    <string name=\"format_90\">Battery remaining: 90%</string>\n"
                        + "    <string name=\"format_80\">Battery remaining: 80%!</string>\n"
                        + "\n"
                        + "</resources>\n"),
                xml("res/values-de/formatstrings11de.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "\n"
                        + "    <string name=\"format_90\">Battery remaining: 90%</string>\n"
                        + "    <string name=\"format_80\">Battery remaining: 80%!</string>\n"
                        + "\n"
                        + "</resources>\n"),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.os.Bundle;\n"
                        + "\n"
                        + "public class StringFormatActivity3 extends Activity {\n"
                        + "    @Override\n"
                        + "    public void onCreate(Bundle savedInstanceState) {\n"
                        + "        super.onCreate(savedInstanceState);\n"
                        + "        getResources().getString(R.string.format_90);\n"
                        + "        getResources().getString(R.string.format_80);\n"
                        + "        String.format(getResources().getString(R.string.format_90));\n"
                        + "        String.format(getResources().getString(R.string.format_80));\n"
                        + "    }\n"
                        + "\n"
                        + "    public static final class R {\n"
                        + "        public static final class string {\n"
                        + "            public static final int format_90 = 0x7f0a000e;\n"
                        + "            public static final int format_80 = 0x7f0a000f;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .incremental("src/test/pkg/StringFormatActivity3.java")
                .run()
                .expectClean();
    }

    public void testXliff() throws Exception {
        lint().files(
                mFormatstrings9,
                mStringFormat9)
                .run()
                .expectClean();
    }

    public void testXliffIncremental() throws Exception {
        lint().files(
                mFormatstrings9,
                mStringFormat9)
                .incremental("src/test/pkg/StringFormat9.java")
                .run()
                .expectClean();
    }

    public void testBigDecimal() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=69527
        //noinspection all // Sample code
        lint().files(
                mFormatstrings10,
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.content.Context;\n"
                        + "import android.widget.TextView;\n"
                        + "\n"
                        + "import java.math.BigDecimal;\n"
                        + "import java.math.BigInteger;\n"
                        + "\n"
                        + "@SuppressWarnings(\"UnusedDeclaration\")\n"
                        + "public class StringFormat10 extends Activity {\n"
                        + "    protected void check() {\n"
                        + "        // BigDecimal is okay as a float: f, e, E, g, G, a, A\n"
                        + "        // BigInteger is okay as an integer: d, o, x, X\n"
                        + "        // http://developer.android.com/reference/java/util/Formatter.html\n"
                        + "        BigDecimal decimal = new BigDecimal(\"3.14159265358979323846264338327950288419716939\");\n"
                        + "        BigInteger integer = new BigInteger(\"2089986280348253421170679821480865132823066470\");\n"
                        + "        TextView view1 = (TextView) findViewById(R.id.my_hello);\n"
                        + "        TextView view2 = (TextView) findViewById(R.id.my_hello2);\n"
                        + "        view1.setText(getResources().getString(R.string.format_float, decimal));\n"
                        + "        view2.setText(getResources().getString(R.string.format_integer, integer));\n"
                        + "        view1.setText(getResources().getString(R.string.format_hex_float, decimal));\n"
                        + "        view2.setText(getResources().getString(R.string.format_hex, integer));\n"
                        + "    }\n"
                        + "\n"
                        + "    protected void check2(Context mContext) {\n"
                        + "        String formatted = mContext.getString(R.string.decimal_format_string,\n"
                        + "                fnReturningDouble());\n"
                        + "    }\n"
                        + "\n"
                        + "    private BigDecimal fnReturningDouble() {\n"
                        + "        return new BigDecimal(\"3.14159265358979323846264338327950288419716939\");\n"
                        + "    }\n"
                        + "\n"
                        + "    public static final class R {\n"
                        + "        public static final class string {\n"
                        + "            public static final int format_float = 0x7f0a000f;\n"
                        + "            public static final int format_integer = 0x7f0a0010;\n"
                        + "            public static final int format_hex_float = 0x7f0a0009;\n"
                        + "            public static final int format_hex = 0x7f0a000d;\n"
                        + "            public static final int decimal_format_string = 0x7f0a000e;\n"
                        + "        }\n"
                        + "\n"
                        + "        public static final class id {\n"
                        + "            public static final int my_hello = 0x7f07003c;\n"
                        + "            public static final int my_hello2 = 0x7f07003d;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"
                        + "\n"))
                .run()
                .expectClean();
    }

    public void testWrapperClasses() throws Exception {
        //noinspection all // Sample code
        lint().files(
                mFormatstrings10,
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "\n"
                        + "import java.math.BigDecimal;\n"
                        + "import java.math.BigInteger;\n"
                        + "\n"
                        + "public class StringFormat11 extends Activity {\n"
                        + "\n"
                        + "    protected void check() {\n"
                        + "        // -------------------------\n"
                        + "        // Test integral types\n"
                        + "        // -------------------------\n"
                        + "\n"
                        + "        byte varByte = 1;\n"
                        + "        Byte varByteWrapper = 1;\n"
                        + "        short varShort = 1;\n"
                        + "        Short varShortWrapper = 1;\n"
                        + "        int varInt = 1;\n"
                        + "        Integer varIntWrapper = 1;\n"
                        + "        long varLong = 1L;\n"
                        + "        Long varLongWrapper = 1L;\n"
                        + "        BigInteger varBigInteger = new BigInteger(\"1\");\n"
                        + "        float varFloat = 1.0f;\n"
                        + "        Float varFloatWrapper = 1.0f;\n"
                        + "        double varDouble = 1.0d;\n"
                        + "        double varDoubleWrapper = 1.0d;\n"
                        + "        BigDecimal varBigDecimal = new BigDecimal(\"1.0\");\n"
                        + "\n"
                        + "        // Check variable references with known types\n"
                        + "        getResources().getString(R.string.format_integer, varByte);\n"
                        + "        getResources().getString(R.string.format_integer, varByteWrapper);\n"
                        + "        getResources().getString(R.string.format_integer, varShort);\n"
                        + "        getResources().getString(R.string.format_integer, varShortWrapper);\n"
                        + "        getResources().getString(R.string.format_integer, varInt);\n"
                        + "        getResources().getString(R.string.format_integer, varIntWrapper);\n"
                        + "        getResources().getString(R.string.format_integer, varLong);\n"
                        + "        getResources().getString(R.string.format_integer, varLongWrapper);\n"
                        + "        getResources().getString(R.string.format_integer, varBigInteger);\n"
                        + "\n"
                        + "        // Check resolved types\n"
                        + "        getResources().getString(R.string.format_integer, getResolvedByte());\n"
                        + "        getResources().getString(R.string.format_integer, getResolvedByteWrapper());\n"
                        + "        getResources().getString(R.string.format_integer, getResolvedShort());\n"
                        + "        getResources().getString(R.string.format_integer, getResolvedShortWrapper());\n"
                        + "        getResources().getString(R.string.format_integer, getResolvedInt());\n"
                        + "        getResources().getString(R.string.format_integer, getResolvedIntWrapper());\n"
                        + "        getResources().getString(R.string.format_integer, getResolvedLong());\n"
                        + "        getResources().getString(R.string.format_integer, getResolvedLongWrapper());\n"
                        + "        getResources().getString(R.string.format_integer, getResolvedBigInteger());\n"
                        + "\n"
                        + "        // -------------------------\n"
                        + "        // Test float types\n"
                        + "        // -------------------------\n"
                        + "\n"
                        + "        // Check variable references with known types\n"
                        + "        getResources().getString(R.string.format_float, varFloat);\n"
                        + "        getResources().getString(R.string.format_float, varFloatWrapper);\n"
                        + "        getResources().getString(R.string.format_float, varDouble);\n"
                        + "        getResources().getString(R.string.format_float, varDoubleWrapper);\n"
                        + "        getResources().getString(R.string.format_float, varBigDecimal);\n"
                        + "\n"
                        + "        // Check resolved types\n"
                        + "        getResources().getString(R.string.format_float, getResolvedFloat());\n"
                        + "        getResources().getString(R.string.format_float, getResolvedFloatWrapper());\n"
                        + "        getResources().getString(R.string.format_float, getResolvedDouble());\n"
                        + "        getResources().getString(R.string.format_float, getResolvedDoubleWrapper());\n"
                        + "        getResources().getString(R.string.format_float, getResolvedBigDecimal());\n"
                        + "\n"
                        + "        // -------------------------\n"
                        + "        // Test conversions\n"
                        + "        // -------------------------\n"
                        + "\n"
                        + "        // Conversion (integer as float)\n"
                        + "        getResources().getString(R.string.format_float, varInt);\n"
                        + "        getResources().getString(R.string.format_float, varIntWrapper);\n"
                        + "        getResources().getString(R.string.format_float, getResolvedInt());\n"
                        + "        getResources().getString(R.string.format_float, getResolvedIntWrapper());\n"
                        + "\n"
                        + "        // Conversion (float as integer)\n"
                        + "        getResources().getString(R.string.format_integer, varFloat);\n"
                        + "        getResources().getString(R.string.format_integer, varFloatWrapper);\n"
                        + "        getResources().getString(R.string.format_integer, getResolvedFloat());\n"
                        + "        getResources().getString(R.string.format_integer, getResolvedFloatWrapper());\n"
                        + "    }\n"
                        + "\n"
                        + "    private byte getResolvedByte() { return 0; }\n"
                        + "    private Byte getResolvedByteWrapper() { return 0; }\n"
                        + "    private short getResolvedShort() { return 0; }\n"
                        + "    private Short getResolvedShortWrapper() { return 0; }\n"
                        + "    private int getResolvedInt() { return 0; }\n"
                        + "    private Integer getResolvedIntWrapper() { return 0; }\n"
                        + "    private long getResolvedLong() { return 0L; }\n"
                        + "    private Long getResolvedLongWrapper() { return 0L; }\n"
                        + "    private float getResolvedFloat() { return 0f; }\n"
                        + "    private Float getResolvedFloatWrapper() { return 0f; }\n"
                        + "    private double getResolvedDouble() { return 0f; }\n"
                        + "    private Double getResolvedDoubleWrapper() { return 0d; }\n"
                        + "    private BigInteger getResolvedBigInteger() { return new BigInteger(\"0\"); }\n"
                        + "    private BigDecimal getResolvedBigDecimal() { return new BigDecimal(\"0.0\"); }\n"
                        + "\n"
                        + "    public static final class R {\n"
                        + "        public static final class string {\n"
                        + "            public static final int format_float = 0x7f0a000f;\n"
                        + "            public static final int format_integer = 0x7f0a0010;\n"
                        + "\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expectClean();
    }

    public void testPluralsCandidates() throws Exception {
        String expected = ""
                + "res/values/plurals_candidates.xml:4: Warning: Formatting %d followed by words (\"times\"): This should probably be a plural rather than a string [PluralsCandidate]\n"
                + "    <string name=\"lockscreen_too_many_failed_attempts_dialog_message1\">\n"
                + "    ^\n"
                + "res/values/plurals_candidates.xml:10: Warning: Formatting %d followed by words (\"times\"): This should probably be a plural rather than a string [PluralsCandidate]\n"
                + "    <string name=\"lockscreen_too_many_failed_attempts_dialog_message2\">\n"
                + "    ^\n"
                + "res/values/plurals_candidates.xml:14: Warning: Formatting %d followed by words (\"moves\"): This should probably be a plural rather than a string [PluralsCandidate]\n"
                + "    <string name=\"win_dialog\">You won in %1$s and %2$d moves!</string>\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/values/plurals_candidates.xml:15: Warning: Formatting %d followed by words (\"times\"): This should probably be a plural rather than a string [PluralsCandidate]\n"
                + "    <string name=\"countdown_complete_sub\">Timer was paused %d times</string>\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/values/plurals_candidates.xml:16: Warning: Formatting %d followed by words (\"satellites\"): This should probably be a plural rather than a string [PluralsCandidate]\n"
                + "    <string name=\"service_gpsstatus\">Logging: %s (%s with %d satellites)</string>\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/values/plurals_candidates.xml:17: Warning: Formatting %d followed by words (\"seconds\"): This should probably be a plural rather than a string [PluralsCandidate]\n"
                + "    <string name=\"sync_log_clocks_unsynchronized\">The clock on your device is incorrect by %1$d seconds%2$s;</string>\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/values/plurals_candidates.xml:18: Warning: Formatting %d followed by words (\"tasks\"): This should probably be a plural rather than a string [PluralsCandidate]\n"
                + "    <string name=\"EPr_manage_purge_deleted_status\">Purged %d tasks!</string>\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 7 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/values/plurals_candidates.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
                        + "    <!-- Warnings: should be plurals instead -->\n"
                        + "    <string name=\"lockscreen_too_many_failed_attempts_dialog_message1\">\n"
                        + "        You have incorrectly drawn your unlock pattern %d times.\n"
                        + "        \\n\\nTry again in >%d seconds.\n"
                        + "    </string>\n"
                        + "    <!-- Same as previous string, but using xliff which means that the formatting strings\n"
                        + "         appear as separate DOM element nodes -->\n"
                        + "    <string name=\"lockscreen_too_many_failed_attempts_dialog_message2\">\n"
                        + "        You have incorrectly drawn your unlock pattern <xliff:g id=\"number\">%d</xliff:g> times.\n"
                        + "        \\n\\nTry again in <xliff:g id=\"number\">%d</xliff:g> seconds.\n"
                        + "    </string>\n"
                        + "    <string name=\"win_dialog\">You won in %1$s and %2$d moves!</string>\n"
                        + "    <string name=\"countdown_complete_sub\">Timer was paused %d times</string>\n"
                        + "    <string name=\"service_gpsstatus\">Logging: %s (%s with %d satellites)</string>\n"
                        + "    <string name=\"sync_log_clocks_unsynchronized\">The clock on your device is incorrect by %1$d seconds%2$s;</string>\n"
                        + "    <string name=\"EPr_manage_purge_deleted_status\">Purged %d tasks!</string>\n"
                        + "\n"
                        + "    <!-- Not needing plurals -->\n"
                        + "    <string name=\"not_plural1\">Ends with %d</string>\n"
                        + "    <string name=\"not_plural2\">Ends with punctuation: %d.</string>\n"
                        + "    <string name=\"not_plural3\">Ends with punctuation: %d !</string>\n"
                        + "    <string name=\"not_plural4\">Ends with non-letter: %d 3.14</string>\n"
                        + "    <string name=\"not_plural5\">Ends with unit: %d KB</string>\n"
                        + "    <string name=\"not_plural6\">Ends with unit: %d MiB.</string>\n"
                        + "    <string name=\"not_plural7\">Ends with unit: Elevation gain: %1$d m (%2$d ft)</string>\n"
                        + "    <string name=\"not_plural8\">Uses floating point: %.2f entries.</string>\n"
                        + "    <string name=\"not_plural9\">Uses floating point: %.2f entries.</string>\n"
                        + "    <string name=\"not_plural10\">Uses string: %s entries.</string>\n"
                        + "</resources>\n"),
                // Should not flag on anything but English strings
                xml("res/values-de/plurals_candidates.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
                        + "    <!-- Warnings: should be plurals instead -->\n"
                        + "    <string name=\"lockscreen_too_many_failed_attempts_dialog_message1\">\n"
                        + "        You have incorrectly drawn your unlock pattern %d times.\n"
                        + "        \\n\\nTry again in >%d seconds.\n"
                        + "    </string>\n"
                        + "    <!-- Same as previous string, but using xliff which means that the formatting strings\n"
                        + "         appear as separate DOM element nodes -->\n"
                        + "    <string name=\"lockscreen_too_many_failed_attempts_dialog_message2\">\n"
                        + "        You have incorrectly drawn your unlock pattern <xliff:g id=\"number\">%d</xliff:g> times.\n"
                        + "        \\n\\nTry again in <xliff:g id=\"number\">%d</xliff:g> seconds.\n"
                        + "    </string>\n"
                        + "    <string name=\"win_dialog\">You won in %1$s and %2$d moves!</string>\n"
                        + "    <string name=\"countdown_complete_sub\">Timer was paused %d times</string>\n"
                        + "    <string name=\"service_gpsstatus\">Logging: %s (%s with %d satellites)</string>\n"
                        + "    <string name=\"sync_log_clocks_unsynchronized\">The clock on your device is incorrect by %1$d seconds%2$s;</string>\n"
                        + "    <string name=\"EPr_manage_purge_deleted_status\">Purged %d tasks!</string>\n"
                        + "\n"
                        + "    <!-- Not needing plurals -->\n"
                        + "    <string name=\"not_plural1\">Ends with %d</string>\n"
                        + "    <string name=\"not_plural2\">Ends with punctuation: %d.</string>\n"
                        + "    <string name=\"not_plural3\">Ends with punctuation: %d !</string>\n"
                        + "    <string name=\"not_plural4\">Ends with non-letter: %d 3.14</string>\n"
                        + "    <string name=\"not_plural5\">Ends with unit: %d KB</string>\n"
                        + "    <string name=\"not_plural6\">Ends with unit: %d MiB.</string>\n"
                        + "    <string name=\"not_plural7\">Ends with unit: Elevation gain: %1$d m (%2$d ft)</string>\n"
                        + "    <string name=\"not_plural8\">Uses floating point: %.2f entries.</string>\n"
                        + "    <string name=\"not_plural9\">Uses floating point: %.2f entries.</string>\n"
                        + "    <string name=\"not_plural10\">Uses string: %s entries.</string>\n"
                        + "</resources>\n"))
                .run()
                .expect(expected);
    }

    public void testAdditionalGetStringMethods() throws Exception {
        // Regression test for
        //   https://code.google.com/p/android/issues/detail?id=183643
        //   183643: Lint format detector should apply to Context#getString
        // It also checks that we handle Object[] properly
        String expected = ""
                + "src/test/pkg/FormatCheck.java:11: Error: Format string 'zero_args' is not a valid format string so it should not be passed to String.format [StringFormatInvalid]\n"
                + "        context.getString(R.string.zero_args, \"first\"); // ERROR\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml:4: This definition does not require arguments\n"
                + "src/test/pkg/FormatCheck.java:13: Error: Format string 'zero_args' is not a valid format string so it should not be passed to String.format [StringFormatInvalid]\n"
                + "        context.getString(R.string.zero_args, new Object[] { \"first\" }); // ERROR\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml:4: This definition does not require arguments\n"
                + "src/test/pkg/FormatCheck.java:17: Error: Wrong argument count, format string one_arg requires 1 but format call supplies 2 [StringFormatMatches]\n"
                + "        context.getString(R.string.one_arg, \"too\", \"many\"); // ERROR: too many arguments\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml:5: This definition requires 1 arguments\n"
                + "src/test/pkg/FormatCheck.java:18: Error: Wrong argument count, format string one_arg requires 1 but format call supplies 0 [StringFormatMatches]\n"
                + "        context.getString(R.string.one_arg, new Object[0]); // ERROR: not enough arguments\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml:5: This definition requires 1 arguments\n"
                + "src/test/pkg/FormatCheck.java:20: Error: Wrong argument count, format string one_arg requires 1 but format call supplies 2 [StringFormatMatches]\n"
                + "        context.getString(R.string.one_arg, new Object[] { \"first\", \"second\" }); // ERROR\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml:5: This definition requires 1 arguments\n"
                + "src/test/pkg/FormatCheck.java:22: Error: Wrong argument count, format string two_args requires 2 but format call supplies 1 [StringFormatMatches]\n"
                + "        context.getString(R.string.two_args, \"first\"); // ERROR: too few\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml:6: This definition requires 2 arguments\n"
                + "src/test/pkg/FormatCheck.java:24: Error: Wrong argument count, format string two_args requires 2 but format call supplies 0 [StringFormatMatches]\n"
                + "        context.getString(R.string.two_args, new Object[0]); // ERROR: not enough arguments\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml:6: This definition requires 2 arguments\n"
                + "src/test/pkg/FormatCheck.java:26: Error: Wrong argument count, format string two_args requires 2 but format call supplies 3 [StringFormatMatches]\n"
                + "        context.getString(R.string.two_args, new Object[] { \"first\", \"second\", \"third\" }); // ERROR\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml:6: This definition requires 2 arguments\n"
                + "src/test/pkg/FormatCheck.java:30: Error: Wrong argument count, format string two_args requires 2 but format call supplies 3 [StringFormatMatches]\n"
                + "        context.getString(R.string.two_args, args3); // ERROR\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml:6: This definition requires 2 arguments\n"
                + "src/test/pkg/FormatCheck.java:36: Error: Wrong argument count, format string one_arg requires 1 but format call supplies 3 [StringFormatMatches]\n"
                + "        fragment.getString(R.string.one_arg, \"too\", \"many\", \"args\"); // ERROR: not enough arguments\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml:5: This definition requires 1 arguments\n"
                + "10 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                java("src/test/pkg/FormatCheck.java", ""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Fragment;\n"
                        + "import android.content.Context;\n"
                        + "\n"
                        + "\n"
                        + "\n"
                        + "public class FormatCheck {\n"
                        + "    public static void testContext(Context context) {\n"
                        + "        context.getString(R.string.zero_args); // OK: Just looking up the string (includes %1$s)\n"
                        + "        context.getString(R.string.zero_args, \"first\"); // ERROR\n"
                        + "        context.getString(R.string.zero_args, new Object[0]); // OK\n"
                        + "        context.getString(R.string.zero_args, new Object[] { \"first\" }); // ERROR\n"
                        + "\n"
                        + "        context.getString(R.string.one_arg); // OK: Just looking up the string (includes %1$s)\n"
                        + "        context.getString(R.string.one_arg, \"first\"); // OK\n"
                        + "        context.getString(R.string.one_arg, \"too\", \"many\"); // ERROR: too many arguments\n"
                        + "        context.getString(R.string.one_arg, new Object[0]); // ERROR: not enough arguments\n"
                        + "        context.getString(R.string.one_arg, new Object[] { \"first\" }); // OK\n"
                        + "        context.getString(R.string.one_arg, new Object[] { \"first\", \"second\" }); // ERROR\n"
                        + "        \n"
                        + "        context.getString(R.string.two_args, \"first\"); // ERROR: too few\n"
                        + "        context.getString(R.string.two_args, \"first\", \"second\"); // OK\n"
                        + "        context.getString(R.string.two_args, new Object[0]); // ERROR: not enough arguments\n"
                        + "        context.getString(R.string.two_args, new Object[] { \"first\", \"second\" }); // OK\n"
                        + "        context.getString(R.string.two_args, new Object[] { \"first\", \"second\", \"third\" }); // ERROR\n"
                        + "        String[] args2 = new String[] { \"first\", \"second\" };\n"
                        + "        context.getString(R.string.two_args, args2); // OK\n"
                        + "        String[] args3 = new String[] { \"first\", \"second\", \"third\" };\n"
                        + "        context.getString(R.string.two_args, args3); // ERROR\n"
                        + "    }\n"
                        + "\n"
                        + "    public static void testFragment(Fragment fragment) {\n"
                        + "        fragment.getString(R.string.one_arg); // OK: Just looking up the string\n"
                        + "        fragment.getString(R.string.one_arg, \"\"); // OK: Not checking non-varargs version\n"
                        + "        fragment.getString(R.string.one_arg, \"too\", \"many\", \"args\"); // ERROR: not enough arguments\n"
                        + "    }\n"
                        + "\n"
                        + "    public static void testArrayTypeConversions(Context context) {\n"
                        + "        context.getString(R.string.one_arg, new Object[] { 5 }); // ERROR: Wrong type\n"
                        + "        context.getString(R.string.two_args, new Object[] { 5, 5.0f }); // ERROR: Wrong type\n"
                        + "    }\n"
                        + "\n"
                        + "    public static final class R {\n"
                        + "        public static final class string {\n"
                        + "            public static final int hello = 0x7f0a0000;\n"
                        + "            public static final int zero_args = 0x7f0a0001;\n"
                        + "            public static final int one_arg = 0x7f0a0002;\n"
                        + "            public static final int two_args = 0x7f0a0003;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"),
                xml("res/values/strings.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name=\"hello\">Hello %1$s</string>\n"
                        + "    <string name=\"zero_args\">Hello</string>\n"
                        + "    <string name=\"one_arg\">Hello %1$s</string>\n"
                        + "    <string name=\"two_args\">Hello %1$s %2$s</string>\n"
                        + "</resources>\n"
                        + "\n"))
                .run()
                .expect(expected);
    }

    /**
     * This test checks the same behaviour as {@link #testAdditionalGetStringMethods()} when lint
     * is running incrementally.
     */
    public void testAdditionalGetStringMethodsIncrementally() throws Exception {
        String expected = ""
                + "src/test/pkg/FormatCheck.java:11: Error: Format string 'zero_args' is not a valid format string so it should not be passed to String.format [StringFormatInvalid]\n"
                + "        context.getString(R.string.zero_args, \"first\"); // ERROR\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml: This definition does not require arguments\n"
                + "src/test/pkg/FormatCheck.java:13: Error: Format string 'zero_args' is not a valid format string so it should not be passed to String.format [StringFormatInvalid]\n"
                + "        context.getString(R.string.zero_args, new Object[] { \"first\" }); // ERROR\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml: This definition does not require arguments\n"
                + "src/test/pkg/FormatCheck.java:17: Error: Wrong argument count, format string one_arg requires 1 but format call supplies 2 [StringFormatMatches]\n"
                + "        context.getString(R.string.one_arg, \"too\", \"many\"); // ERROR: too many arguments\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml: This definition requires 1 arguments\n"
                + "src/test/pkg/FormatCheck.java:18: Error: Wrong argument count, format string one_arg requires 1 but format call supplies 0 [StringFormatMatches]\n"
                + "        context.getString(R.string.one_arg, new Object[0]); // ERROR: not enough arguments\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml: This definition requires 1 arguments\n"
                + "src/test/pkg/FormatCheck.java:20: Error: Wrong argument count, format string one_arg requires 1 but format call supplies 2 [StringFormatMatches]\n"
                + "        context.getString(R.string.one_arg, new Object[] { \"first\", \"second\" }); // ERROR\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml: This definition requires 1 arguments\n"
                + "src/test/pkg/FormatCheck.java:22: Error: Wrong argument count, format string two_args requires 2 but format call supplies 1 [StringFormatMatches]\n"
                + "        context.getString(R.string.two_args, \"first\"); // ERROR: too few\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml: This definition requires 2 arguments\n"
                + "src/test/pkg/FormatCheck.java:24: Error: Wrong argument count, format string two_args requires 2 but format call supplies 0 [StringFormatMatches]\n"
                + "        context.getString(R.string.two_args, new Object[0]); // ERROR: not enough arguments\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml: This definition requires 2 arguments\n"
                + "src/test/pkg/FormatCheck.java:26: Error: Wrong argument count, format string two_args requires 2 but format call supplies 3 [StringFormatMatches]\n"
                + "        context.getString(R.string.two_args, new Object[] { \"first\", \"second\", \"third\" }); // ERROR\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml: This definition requires 2 arguments\n"
                + "src/test/pkg/FormatCheck.java:30: Error: Wrong argument count, format string two_args requires 2 but format call supplies 3 [StringFormatMatches]\n"
                + "        context.getString(R.string.two_args, args3); // ERROR\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml: This definition requires 2 arguments\n"
                + "src/test/pkg/FormatCheck.java:36: Error: Wrong argument count, format string one_arg requires 1 but format call supplies 3 [StringFormatMatches]\n"
                + "        fragment.getString(R.string.one_arg, \"too\", \"many\", \"args\"); // ERROR: not enough arguments\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml: This definition requires 1 arguments\n"
                + "10 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                java("src/test/pkg/FormatCheck.java", ""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Fragment;\n"
                        + "import android.content.Context;\n"
                        + "\n"
                        + "\n"
                        + "\n"
                        + "public class FormatCheck {\n"
                        + "    public static void testContext(Context context) {\n"
                        + "        context.getString(R.string.zero_args); // OK: Just looking up the string (includes %1$s)\n"
                        + "        context.getString(R.string.zero_args, \"first\"); // ERROR\n"
                        + "        context.getString(R.string.zero_args, new Object[0]); // OK\n"
                        + "        context.getString(R.string.zero_args, new Object[] { \"first\" }); // ERROR\n"
                        + "\n"
                        + "        context.getString(R.string.one_arg); // OK: Just looking up the string (includes %1$s)\n"
                        + "        context.getString(R.string.one_arg, \"first\"); // OK\n"
                        + "        context.getString(R.string.one_arg, \"too\", \"many\"); // ERROR: too many arguments\n"
                        + "        context.getString(R.string.one_arg, new Object[0]); // ERROR: not enough arguments\n"
                        + "        context.getString(R.string.one_arg, new Object[] { \"first\" }); // OK\n"
                        + "        context.getString(R.string.one_arg, new Object[] { \"first\", \"second\" }); // ERROR\n"
                        + "        \n"
                        + "        context.getString(R.string.two_args, \"first\"); // ERROR: too few\n"
                        + "        context.getString(R.string.two_args, \"first\", \"second\"); // OK\n"
                        + "        context.getString(R.string.two_args, new Object[0]); // ERROR: not enough arguments\n"
                        + "        context.getString(R.string.two_args, new Object[] { \"first\", \"second\" }); // OK\n"
                        + "        context.getString(R.string.two_args, new Object[] { \"first\", \"second\", \"third\" }); // ERROR\n"
                        + "        String[] args2 = new String[] { \"first\", \"second\" };\n"
                        + "        context.getString(R.string.two_args, args2); // OK\n"
                        + "        String[] args3 = new String[] { \"first\", \"second\", \"third\" };\n"
                        + "        context.getString(R.string.two_args, args3); // ERROR\n"
                        + "    }\n"
                        + "\n"
                        + "    public static void testFragment(Fragment fragment) {\n"
                        + "        fragment.getString(R.string.one_arg); // OK: Just looking up the string\n"
                        + "        fragment.getString(R.string.one_arg, \"\"); // OK: Not checking non-varargs version\n"
                        + "        fragment.getString(R.string.one_arg, \"too\", \"many\", \"args\"); // ERROR: not enough arguments\n"
                        + "    }\n"
                        + "\n"
                        + "    public static void testArrayTypeConversions(Context context) {\n"
                        + "        context.getString(R.string.one_arg, new Object[] { 5 }); // ERROR: Wrong type\n"
                        + "        context.getString(R.string.two_args, new Object[] { 5, 5.0f }); // ERROR: Wrong type\n"
                        + "    }\n"
                        + "\n"
                        + "    public static final class R {\n"
                        + "        public static final class string {\n"
                        + "            public static final int hello = 0x7f0a0000;\n"
                        + "            public static final int zero_args = 0x7f0a0001;\n"
                        + "            public static final int one_arg = 0x7f0a0002;\n"
                        + "            public static final int two_args = 0x7f0a0003;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"),
                xml("res/values/strings.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name=\"hello\">Hello %1$s</string>\n"
                        + "    <string name=\"zero_args\">Hello</string>\n"
                        + "    <string name=\"one_arg\">Hello %1$s</string>\n"
                        + "    <string name=\"two_args\">Hello %1$s %2$s</string>\n"
                        + "</resources>\n"
                        + "\n"))
                .incremental("src/test/pkg/FormatCheck.java")
                .run()
                .expect(expected);
    }

    public void testIssue197940() throws Exception {
        // Regression test for
        //   https://code.google.com/p/android/issues/detail?id=197940
        //    197940: The linter alerts about a wrong String.format format, but it's ok
        //noinspection all // Sample code
        lint().files(
                java("src/test/pkg/FormatCheck.java", ""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.res.Resources;\n"
                        + "\n"
                        + "public class FormatCheck {\n"

                        + "    private static String test(Resources resources) {\n"
                        + "        return String.format(\"%s\", resources.getString(R.string.a_b_c));\n"
                        + "    }\n"
                        + "\n"
                        + "    public static final class R {\n"
                        + "        public static final class string {\n"
                        + "            public static final int a_b_c = 0x7f0a0000;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"),
                xml("res/values/strings.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name=\"a_b_c\">A b c </string>\n\n"
                        + "    <string name=\"a_b_c_2\">A %1$s c </string>\n\n"
                        + "</resources>\n"
                        + "\n"))
                .run()
                .expectClean();
    }

    public void testStripQuotes() {
        assertEquals("", StringFormatDetector.stripQuotes(""));
        assertEquals("\\'a", StringFormatDetector.stripQuotes("\\'a'"));
        assertEquals("", StringFormatDetector.stripQuotes("\""));
        assertEquals("'", StringFormatDetector.stripQuotes("\"\'"));
        assertEquals("\\\"Escaped quotes\\' and not escaped ",
                StringFormatDetector.stripQuotes("\\\"Escaped quotes\\' and not escaped \""));
        assertEquals("\\\\Not escaped quote", StringFormatDetector.stripQuotes("\\\\\"Not escaped quote"));
        assertEquals("This\\'ll work", StringFormatDetector.stripQuotes("This\\'ll work"));
        assertEquals("This'll also work", StringFormatDetector.stripQuotes("\"This'll also work\""));
    }

    public void testIssue196494() throws Exception {
        // Regression test for
        //   http://b.android.com/196494
        //    196494: StringFormatCount false positive with nested/escaped quotes and xliff tags
        //noinspection all // Sample code
        lint().files(
                xml("res/values/strings.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name=\"a\">%1$s in %2$s</string>\n\n"
                        + "</resources>\n"
                        + "\n"),
                xml("res/values/strings-es.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
                        + "    <string name=\"a\">\"\\\"<xliff:g id=\"source\" example=\"hello\">%1$s</xliff:g>\\\" in %2$s</string>\n\n"
                        + "</resources>\n"
                        + "\n"))
                .run()
                .expectClean();
    }

    public void testIssue202241() throws Exception {
        // Regression test for
        //   http://b.android.com/202241
        // We need to handle string references.
        //noinspection all // Sample code
        lint().files(
                xml("res/values/strings.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "   <string name=\"test_string_en\">This is English fallback - %1$d</string>\n"
                        + "   <string name=\"test_string\" translatable=\"false\">  @string/test_string_en  </string>\n"
                        + "</resources>\n"
                        + "\n"),
                xml("res/values/strings-es.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
                        + "    <string name=\"test_string\">Pretend this is French - %1$d</string>\n"
                        + "</resources>\n"
                        + "\n"))
                .run()
                .expectClean();
    }

    public void testIssue228570() throws Exception {
        // Regression test for
        //   http://b.android.com/228570
        // We need to handle string references.
        //noinspection all // Sample code
        lint().files(
                xml("res/values/strings.xml", ""
                        + "<resources>\n"
                        + "    <string name=\"confirm_delete_message\">@string/Do_you_want_to_delete_X_items</string>\n"
                        + "    <string name=\"Do_you_want_to_delete_X_items\">Do you want to delete %d entries?</string>\n"
                        + "</resources>"),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "public class Test extends android.app.Activity {\n"
                        + "\n"
                        + "    public void test() {\n"
                        + "        int itemsCount = 5;\n"
                        + "        String string2 = getString(R.string.confirm_delete_message, itemsCount); // OK\n"
                        + "        String string1 = getString(R.string.confirm_delete_message, itemsCount, 5); // ERROR\n"
                        + "    }\n"
                        + "    public static final class R {\n"
                        + "        public static final class string {\n"
                        + "            public static final int confirm_delete_message = 0x7f0a0000;\n"
                        + "        }\n"
                        + "    }\n"
                        + ""
                        + "}"))
                .issues(StringFormatDetector.ARG_TYPES)
                .incremental("src/test/pkg/Test.java")
                .run()
                .expect(""
                        + "src/test/pkg/Test.java:8: Error: Wrong argument count, format string confirm_delete_message requires 1 but format call supplies 2 [StringFormatMatches]\n"
                        + "        String string1 = getString(R.string.confirm_delete_message, itemsCount, 5); // ERROR\n"
                        + "                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "    res/values/strings.xml: This definition requires 1 arguments\n"
                        + "1 errors, 0 warnings\n");
    }

    public void testFormattingCharSuggestions() throws Exception {
        String expected = ""
                + "src/test/pkg/FormatSuggestions.java:7: Error: Wrong argument type for formatting argument '#1' in key1: conversion is 's', received boolean (argument #2 in method call) (Did you mean formatting character b?) [StringFormatMatches]\n"
                + "        resources.getString(R.string.key1, true);\n"
                + "                                           ~~~~\n"
                + "    res/values/strings.xml:3: Conflicting argument declaration here\n"
                + "src/test/pkg/FormatSuggestions.java:8: Error: Wrong argument type for formatting argument '#1' in key1: conversion is 's', received Boolean (argument #2 in method call) (Did you mean formatting character b?) [StringFormatMatches]\n"
                + "        resources.getString(R.string.key1, Boolean.valueOf(true));\n"
                + "                                           ~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml:3: Conflicting argument declaration here\n"
                + "src/test/pkg/FormatSuggestions.java:9: Error: Suspicious argument type for formatting argument #1 in key1: conversion is s, received int (argument #2 in method call) (Did you mean formatting character d, 'o' or x?) [StringFormatMatches]\n"
                + "        resources.getString(R.string.key1, 1);\n"
                + "                                           ~\n"
                + "    res/values/strings.xml:3: Conflicting argument declaration here\n"
                + "src/test/pkg/FormatSuggestions.java:10: Error: Suspicious argument type for formatting argument #1 in key1: conversion is s, received long (argument #2 in method call) (Did you mean formatting character d, 'o' or x?) [StringFormatMatches]\n"
                + "        resources.getString(R.string.key1, 1L);\n"
                + "                                           ~~\n"
                + "    res/values/strings.xml:3: Conflicting argument declaration here\n"
                + "src/test/pkg/FormatSuggestions.java:11: Error: Suspicious argument type for formatting argument #1 in key1: conversion is s, received Integer (argument #2 in method call) (Did you mean formatting character d, 'o' or x?) [StringFormatMatches]\n"
                + "        resources.getString(R.string.key1, new Integer(42));\n"
                + "                                           ~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml:3: Conflicting argument declaration here\n"
                + "src/test/pkg/FormatSuggestions.java:12: Error: Suspicious argument type for formatting argument #1 in key1: conversion is s, received Long (argument #2 in method call) (Did you mean formatting character d, 'o' or x?) [StringFormatMatches]\n"
                + "        resources.getString(R.string.key1, new Long(42));\n"
                + "                                           ~~~~~~~~~~~~\n"
                + "    res/values/strings.xml:3: Conflicting argument declaration here\n"
                + "src/test/pkg/FormatSuggestions.java:13: Error: Suspicious argument type for formatting argument #1 in key1: conversion is s, received float (argument #2 in method call) (Did you mean formatting character e, 'f', 'g' or a?) [StringFormatMatches]\n"
                + "        resources.getString(R.string.key1, 3.14f);\n"
                + "                                           ~~~~~\n"
                + "    res/values/strings.xml:3: Conflicting argument declaration here\n"
                + "src/test/pkg/FormatSuggestions.java:14: Error: Suspicious argument type for formatting argument #1 in key1: conversion is s, received double (argument #2 in method call) (Did you mean formatting character e, 'f', 'g' or a?) [StringFormatMatches]\n"
                + "        resources.getString(R.string.key1, 3.14);\n"
                + "                                           ~~~~\n"
                + "    res/values/strings.xml:3: Conflicting argument declaration here\n"
                + "src/test/pkg/FormatSuggestions.java:15: Error: Suspicious argument type for formatting argument #1 in key1: conversion is s, received Float (argument #2 in method call) (Did you mean formatting character d, 'o' or x?) [StringFormatMatches]\n"
                + "        resources.getString(R.string.key1, new Float(3.14f));\n"
                + "                                           ~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml:3: Conflicting argument declaration here\n"
                + "src/test/pkg/FormatSuggestions.java:16: Error: Suspicious argument type for formatting argument #1 in key1: conversion is s, received Double (argument #2 in method call) (Did you mean formatting character d, 'o' or x?) [StringFormatMatches]\n"
                + "        resources.getString(R.string.key1, new Double(3.14));\n"
                + "                                           ~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml:3: Conflicting argument declaration here\n"
                + "src/test/pkg/FormatSuggestions.java:19: Error: Suspicious argument type for formatting argument #1 in key1: conversion is s, received byte (argument #2 in method call) (Did you mean formatting character d, 'o' or x?) [StringFormatMatches]\n"
                + "        resources.getString(R.string.key1, (byte)0);\n"
                + "                                           ~~~~~~~\n"
                + "    res/values/strings.xml:3: Conflicting argument declaration here\n"
                + "src/test/pkg/FormatSuggestions.java:20: Error: Suspicious argument type for formatting argument #1 in key1: conversion is s, received Byte (argument #2 in method call) (Did you mean formatting character d, 'o' or x?) [StringFormatMatches]\n"
                + "        resources.getString(R.string.key1, Byte.valueOf((byte)0));\n"
                + "                                           ~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml:3: Conflicting argument declaration here\n"
                + "src/test/pkg/FormatSuggestions.java:21: Error: Suspicious argument type for formatting argument #1 in key1: conversion is s, received short (argument #2 in method call) (Did you mean formatting character d, 'o' or x?) [StringFormatMatches]\n"
                + "        resources.getString(R.string.key1, (short)0);\n"
                + "                                           ~~~~~~~~\n"
                + "    res/values/strings.xml:3: Conflicting argument declaration here\n"
                + "src/test/pkg/FormatSuggestions.java:22: Error: Suspicious argument type for formatting argument #1 in key1: conversion is s, received Short (argument #2 in method call) (Did you mean formatting character d, 'o' or x?) [StringFormatMatches]\n"
                + "        resources.getString(R.string.key1, Short.valueOf((short)0));\n"
                + "                                           ~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml:3: Conflicting argument declaration here\n"
                + "src/test/pkg/FormatSuggestions.java:23: Error: Wrong argument type for formatting argument '#1' in key2: conversion is 'b', received Object (argument #2 in method call) (Did you mean formatting character 's' or 'h'?) [StringFormatMatches]\n"
                + "        resources.getString(R.string.key2, new Object());\n"
                + "                                           ~~~~~~~~~~~~\n"
                + "    res/values/strings.xml:5: Conflicting argument declaration here\n"
                + "15 errors, 0 warnings\n";

        //noinspection all // Sample code
        lint().files(
                java("src/test/pkg/FormatSuggestions.java", ""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.res.Resources;\n"
                        + "\n"
                        + "public class FormatSuggestions {\n"

                        + "    private static void test(Resources resources) {\n"
                        + "        resources.getString(R.string.key1, true);\n"
                        + "        resources.getString(R.string.key1, Boolean.valueOf(true));\n"
                        + "        resources.getString(R.string.key1, 1);\n"
                        + "        resources.getString(R.string.key1, 1L);\n"
                        + "        resources.getString(R.string.key1, new Integer(42));\n"
                        + "        resources.getString(R.string.key1, new Long(42));\n"
                        + "        resources.getString(R.string.key1, 3.14f);\n"
                        + "        resources.getString(R.string.key1, 3.14);\n"
                        + "        resources.getString(R.string.key1, new Float(3.14f));\n"
                        + "        resources.getString(R.string.key1, new Double(3.14));\n"
                        + "        resources.getString(R.string.key1, 'c');\n"
                        + "        resources.getString(R.string.key1, new Character('c'));\n"
                        + "        resources.getString(R.string.key1, (byte)0);\n"
                        + "        resources.getString(R.string.key1, Byte.valueOf((byte)0));\n"
                        + "        resources.getString(R.string.key1, (short)0);\n"
                        + "        resources.getString(R.string.key1, Short.valueOf((short)0));\n"
                        + "        resources.getString(R.string.key2, new Object());\n"
                        + "    }\n"
                        + "\n"
                        + "    public static final class R {\n"
                        + "        public static final class string {\n"
                        + "            public static final int key1 = 0x7f0a0000;\n"
                        + "            public static final int key2 = 0x7f0a0001;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"),
                xml("res/values/strings.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name=\"key1\">%s</string>\n\n"
                        + "    <string name=\"key2\">%b</string>\n\n"
                        + "</resources>\n"
                        + "\n"))
                .run()
                .expect(expected);
    }

    public void testStringIndirection() throws Exception {
        // Regression test for
        // https://code.google.com/p/android/issues/detail?id=201812
        // Make sure that we can handle string format with indirect resources.
        // (The below error message isn't the bug; the bug was that it used to report
        // an invalid format string; now it correctly identifies this as a scenario
        // which should be using plurals instead.)

        String expected = ""
                + "res/values/strings.xml:4: Warning: Formatting %d followed by words (\"entries\"): This should probably be a plural rather than a string [PluralsCandidate]\n"
                + "    <string name=\"Do_you_want_to_delete_X_items\">Do you want to delete %d entries?</string>\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                java("src/test/pkg/Indirection.java", ""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.res.Resources;\n"
                        + "\n"
                        + "public class Indirection {\n"

                        + "    private static void test(Resources resources, int itemsCount) {\n"
                        + "        resources.getString(R.string.confirm_delete_message, itemsCount);\n"
                        + "    }\n"
                        + "\n"
                        + "    public static final class R {\n"
                        + "        public static final class string {\n"
                        + "            public static final int confirm_delete_message = 0x7f0a0000;\n"
                        + "            public static final int Do_you_want_to_delete_X_items = 0x7f0a0001;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"),
                xml("res/values/strings.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name=\"confirm_delete_message\">@string/Do_you_want_to_delete_X_items</string>\n"
                        + "    <string name=\"Do_you_want_to_delete_X_items\">Do you want to delete %d entries?</string>\n"
                        + "</resources>\n"
                        + "\n"))
                .run()
                .expect(expected);
    }

    public void testPercentS() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=219153
        // %s can be passed anything.
        String expected = ""
                + "src/test/pkg/PercentS.java:8: Error: Suspicious argument type for formatting argument #1 in some_string: conversion is s, received int (argument #2 in method call) (Did you mean formatting character d, 'o' or x?) [StringFormatMatches]\n"
                + "        resources.getString(R.string.some_string, val);\n"
                + "                                                  ~~~\n"
                + "    res/values/strings.xml:3: Conflicting argument declaration here\n"
                + "1 errors, 0 warnings\n";

        //noinspection all // Sample code
        lint().files(
                java("src/test/pkg/PercentS.java", ""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.res.Resources;\n"
                        + "\n"
                        + "public class PercentS {\n"
                        + "    private static void test(Resources resources, int itemsCount) {\n"
                        + "        int val = 5;\n"
                        + "        resources.getString(R.string.some_string, val);\n"
                        + "    }\n"
                        + "\n"
                        + "    public static final class R {\n"
                        + "        public static final class string {\n"
                        + "            public static final int some_string = 0x7f0a0000;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"),
                xml("res/values/strings.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "<string name=\"some_string\">This is a value: %1$s</string>\n"
                        + "</resources>\n"
                        + "\n"))
                .run()
                .expect(expected);
    }

    public void testVarArgs() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=219152
        // Varargs wasn't handled correctly in some scenarios.
        //noinspection all // Sample code
        lint().files(
                java("src/test/pkg/VarArgs.java", ""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.res.Resources;\n"
                        + "\n"
                        + "public class VarArgs {\n"
                        + "    private static void test(Resources resources, int itemsCount) {\n"
                        + "        String first = \"John\";\n"
                        + "        String last = \"Doe\";\n"
                        + "        String[] args = {first, last};\n"
                        + "        resources.getString(R.string.some_string, first, last);\n"
                        + "        resources.getString(R.string.some_string, args);\n"
                        + "    }\n"
                        + "\n"
                        + "    public static final class R {\n"
                        + "        public static final class string {\n"
                        + "            public static final int some_string = 0x7f0a0000;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"),
                xml("res/values/strings.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name=\"some_string\">First name: %1$s Last name: %2$s</string>\n"
                        + "</resources>\n"
                        + "\n"))
                .run()
                .expectClean();
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mFormatstrings = xml("res/values/formatstrings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <string name=\"hello\">Hello %1$s</string>\n"
            + "    <string name=\"hello2\">Hello %1$s, %2$s?</string>\n"
            + "    <string name=\"missing\">Hello %3$s World</string>\n"
            + "    <string name=\"score\">Score: %1$d</string>\n"
            + "    <string name=\"score2\">Score: %1$b</string>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mFormatstrings10 = xml("res/values/formatstrings10.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "\n"
            + "    <string name=\"format_float\">Formatted float value: %f</string>\n"
            + "    <string name=\"format_integer\">Formatted integer value: %d</string>\n"
            + "    <string name=\"format_hex_float\">Formatted float value: %A</string>\n"
            + "    <string name=\"format_hex\">Formatted integer value: %h</string>\n"
            + "    <string name=\"decimal_format_string\">Decimal string: %.2f</string>\n"
            + "\n"
            + "</resources>\n"
            + "\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mFormatstrings2 = xml("res/values-es/formatstrings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <string name=\"hello\">%1$d</string>\n"
            + "    <string name=\"hello2\">%3$d: %1$s, %2$s?</string>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mFormatstrings3 = xml("res/values/formatstrings3.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <string name=\"multiple_formats_with_percentage\">%1$s 3%% %2$s</string>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mFormatstrings9 = xml("res/values/formatstrings9.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
            + "    <string name=\"toast_percent_copy_quota_used\">\n"
            + "        You\\'ve used about <xliff:g id=\"copyQuotaUsed\">%d</xliff:g>%% of your\n"
            + "        quota</string>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mFormatstrings_version1 = xml("res/values-tl/donottranslate-cldr.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <string name=\"hour_minute_24\">%-k:%M</string>\n"
            + "    <string name=\"numeric_date\">%Y-%m-%d</string>\n"
            + "    <string name=\"month_day_year\">%Y %B %-e</string>\n"
            + "    <string translatable=\"false\" name=\"web_user_agent\">\n"
            + "      Foo (Bar %s) Foo/731.11+ (Foo, like Bar) Version/1.2.3 Foo Bar/123.14.4\n"
            + "    </string>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mSharedPrefsTest6 = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.content.Context;\n"
            + "import android.content.SharedPreferences;\n"
            + "import android.preference.PreferenceManager;\n"
            + "\n"
            + "@SuppressWarnings(\"UnusedDeclaration\")\n"
            + "public class SharedPrefsFormat {\n"
            + "    public void test(Context sessionContext) {\n"
            + "        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(sessionContext);\n"
            + "        final String nameKey = sessionContext.getString(R.string.pref_key_assigned_bluetooth_device_name);\n"
            + "        final String addressKey = sessionContext.getString(R.string.pref_key_assigned_bluetooth_device_address);\n"
            + "        final String name = prefs.getString(nameKey, null);\n"
            + "        final String address = prefs.getString(addressKey, null);\n"
            + "    }\n"
            + "\n"
            + "    public static final class R {\n"
            + "        public static final class string {\n"
            + "            public static final int pref_key_assigned_bluetooth_device_name = 0x7f0a000e;\n"
            + "            public static final int pref_key_assigned_bluetooth_device_address = 0x7f0a000f;\n"
            + "        }\n"
            + "    }\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mShared_prefs_keys = xml("res/values/shared_prefs_keys.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <string name=\"pref_key_assigned_bluetooth_device_name\">Device Name</string>\n"
            + "    <string name=\"pref_key_assigned_bluetooth_device_address\">Device Address %1$s %2$s</string>\n"
            + "</resources>\n"
            + "\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStringFormat9 = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.content.res.Resources;\n"
            + "\n"
            + "public class StringFormat9 {\n"
            + "    public String format(Resources resources, int percentUsed) {\n"
            + "        return resources.getString(R.string.toast_percent_copy_quota_used, percentUsed);\n"
            + "    }\n"
            + "\n"
            + "    private static class R {\n"
            + "        private static class string {\n"
            + "            public static final int toast_percent_copy_quota_used = 1;\n"
            + "        }\n"
            + "    }\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStringFormatActivity = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.app.Activity;\n"
            + "import android.os.Bundle;\n"
            + "\n"
            + "public class StringFormatActivity extends Activity {\n"
            + "    /** Called when the activity is first created. */\n"
            + "    @Override\n"
            + "    public void onCreate(Bundle savedInstanceState) {\n"
            + "        super.onCreate(savedInstanceState);\n"
            + "        String target = \"World\";\n"
            + "        String hello = getResources().getString(R.string.hello);\n"
            + "        String output1 = String.format(hello, target);\n"
            + "        String hello2 = getResources().getString(R.string.hello2);\n"
            + "        String output2 = String.format(hello2, target, \"How are you\");\n"
            + "        setContentView(R.layout.main);\n"
            + "        String score = getString(R.string.score);\n"
            + "        int points = 50;\n"
            + "        boolean won = true;\n"
            + "        String output3 = String.format(score, points);\n"
            + "        String output4 = String.format(score, true);  // wrong\n"
            + "        String output  = String.format(score, won);   // wrong\n"
            + "        String output5 = String.format(score, 75);\n"
            + "        String.format(getResources().getString(R.string.hello2), target, \"How are you\");\n"
            + "        //getResources().getString(R.string.hello, target, \"How are you\");\n"
            + "        getResources().getString(R.string.hello2, target, \"How are you\");\n"
            + "    }\n"
            + "\n"
            + "    // Test constructor handling (issue 35588)\n"
            + "    public StringFormatActivity() {\n"
            + "        String target = \"World\";\n"
            + "        String hello = getResources().getString(R.string.hello);\n"
            + "        String output1 = String.format(hello, target);\n"
            + "    }\n"
            + "\n"
            + "    public void testPrimitiveWrappers() {\n"
            + "        Boolean won = true;\n"
            + "        Integer value = 1;\n"
            + "        String score = getResources().getString(R.string.score);\n"
            + "        String score2 = getResources().getString(R.string.score2);\n"
            + "        String output1  = String.format(score, won);   // wrong\n"
            + "        String output2  = String.format(score, value);   // ok\n"
            + "        String output3  = String.format(score2, won);   // ok\n"
            + "    }\n"
            + "\n"
            + "    private static class R {\n"
            + "        private static class string {\n"
            + "            public static final int hello = 1;\n"
            + "            public static final int hello2 = 2;\n"
            + "            public static final int score = 3;\n"
            + "            public static final int score2 = 5;\n"
            + "        }\n"
            + "        private static class layout {\n"
            + "            public static final int main = 4;\n"
            + "        }\n"
            + "    }\n"
            + "}\n");
}
