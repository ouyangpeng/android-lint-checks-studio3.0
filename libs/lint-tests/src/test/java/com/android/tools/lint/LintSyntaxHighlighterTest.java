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

package com.android.tools.lint;

import com.android.annotations.NonNull;
import com.android.utils.HtmlBuilder;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import junit.framework.TestCase;
import org.intellij.lang.annotations.Language;
import org.junit.Assert;

public class LintSyntaxHighlighterTest extends TestCase {
    private static final boolean DEBUG = false;

    @NonNull
    private static String highlight(String source, int beginOffset, int endOffset, boolean error,
            String fileName, boolean dedent) {
        LintSyntaxHighlighter highlighter = new LintSyntaxHighlighter(fileName, source);
        highlighter.setPadCaretLine(false);
        highlighter.setDedent(dedent);
        HtmlBuilder builder = new HtmlBuilder();
        highlighter.generateHtml(builder, beginOffset, endOffset, error);

        if (DEBUG) { // For debugging only: Show snippet in browser
            try {
                String path = "/tmp/syntax.html";
                Files.write((""
                                + "<html><head>\n"
                                + "<style>\n"
                                + MaterialHtmlReporter.CSS_STYLES
                                + "</style>\n"
                                + "</head>\n<body>\n") + builder.getHtml()
                                + "</body></html>\n",
                        new File(path), Charsets.UTF_8);
                Runtime.getRuntime().exec("/usr/bin/open " + path);
            } catch (IOException ignore) {
            }
        }

        return builder.getHtml();
    }

    public void testXml() {
        //noinspection all // Sample code
        @Language("XML") String source = ""
                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    package=\"pkg.my.myapplication\">\n"
                + "\n"
                + "    <!-- This is my comment! -->\n"
                + "    <string name=\"app_name\">_Test-Basic</string>\n"
                + "    <application\n"
                + "        android:allowBackup=\"true\"\n"
                + "        android:icon=\"@mipmap/ic_launcher\"\n"
                + "        android:label=\"@string/app_name\"\n"
                + "        android:roundIcon=\"@mipmap/ic_launcher_round\"\n"
                + "        android:supportsRtl=\"true\"\n"
                + "        android:theme=\"@style/AppTheme\">\n"
                + "        <activity android:name=\".MainActivity\">\n"
                + "            <intent-filter>\n"
                + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                + "\n"
                + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                + "            </intent-filter>\n"
                + "        </activity>\n"
                + "    </application>\n"
                + "\n"
                + "</manifest>";
        int beginOffset = source.indexOf("@style");
        int endOffset = source.indexOf('\"', beginOffset);
        boolean error = true;
        assertTrue(beginOffset != -1);
        assertTrue(endOffset != -1);

        @Language("HTML")
        String html = highlight(source, beginOffset, endOffset, error, "foo.xml", false);

        Assert.assertEquals(""
                        + "<pre class=\"errorlines\">\n"
                        + "<span class=\"lineno\"> 10 </span>        <span class=\"prefix\">android:</span><span class=\"attribute\">label</span>=<span class=\"value\">\"@string/app_name\"</span>\n"
                        + "<span class=\"lineno\"> 11 </span>        <span class=\"prefix\">android:</span><span class=\"attribute\">roundIcon</span>=<span class=\"value\">\"@mipmap/ic_launcher_round\"</span>\n"
                        + "<span class=\"lineno\"> 12 </span>        <span class=\"prefix\">android:</span><span class=\"attribute\">supportsRtl</span>=<span class=\"value\">\"true\"</span>\n"
                        + "<span class=\"caretline\"><span class=\"lineno\"> 13 </span>        <span class=\"prefix\">android:</span><span class=\"attribute\">theme</span>=<span class=\"value\">\"</span><span class=\"error\"><span class=\"value\">@style/AppTheme</span></span><span class=\"value\">\"</span>></span>\n"
                        + "<span class=\"lineno\"> 14 </span>        <span class=\"tag\">&lt;activity</span><span class=\"attribute\"> </span><span class=\"prefix\">android:</span><span class=\"attribute\">name</span>=<span class=\"value\">\".MainActivity\"</span>>\n"
                        + "<span class=\"lineno\"> 15 </span>            <span class=\"tag\">&lt;intent-filter></span>\n"
                        + "<span class=\"lineno\"> 16 </span>                <span class=\"tag\">&lt;action</span><span class=\"attribute\"> </span><span class=\"prefix\">android:</span><span class=\"attribute\">name</span>=<span class=\"value\">\"android.intent.action.MAIN\"</span> />\n"
                        + "</pre>\n",
                html);
    }

    public void testPlaintext() {
        //noinspection all // Sample code
        @Language("XML") String source = ""
                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    package=\"pkg.my.myapplication\">\n"
                + "\n"
                + "    <!-- This is my comment! -->\n"
                + "    <string name=\"app_name\">_Test-Basic</string>\n"
                + "    <application\n"
                + "        android:allowBackup=\"true\"\n"
                + "        android:icon=\"@mipmap/ic_launcher\"\n"
                + "        android:label=\"@string/app_name\"\n"
                + "        android:roundIcon=\"@mipmap/ic_launcher_round\"\n"
                + "        android:supportsRtl=\"true\"\n"
                + "        android:theme=\"@style/AppTheme\">\n"
                + "        <activity android:name=\".MainActivity\">\n"
                + "            <intent-filter>\n"
                + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                + "\n"
                + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                + "            </intent-filter>\n"
                + "        </activity>\n"
                + "    </application>\n"
                + "\n"
                + "</manifest>";
        int beginOffset = source.indexOf("@style");
        int endOffset = source.indexOf('\"', beginOffset);
        boolean error = true;
        assertTrue(beginOffset != -1);
        assertTrue(endOffset != -1);

        @Language("HTML")
        String html = highlight(source, beginOffset, endOffset, error, "foo.txt", false);

        Assert.assertEquals(""
                        + "<pre class=\"errorlines\">\n"
                        + "<span class=\"lineno\"> 10 </span>        android:label=\"@string/app_name\"\n"
                        + "<span class=\"lineno\"> 11 </span>        android:roundIcon=\"@mipmap/ic_launcher_round\"\n"
                        + "<span class=\"lineno\"> 12 </span>        android:supportsRtl=\"true\"\n"
                        + "<span class=\"caretline\"><span class=\"lineno\"> 13 </span>        android:theme=\"<span class=\"error\">@style/AppTheme</span>\"></span>\n"
                        + "<span class=\"lineno\"> 14 </span>        &lt;activity android:name=\".MainActivity\">\n"
                        + "<span class=\"lineno\"> 15 </span>            &lt;intent-filter>\n"
                        + "<span class=\"lineno\"> 16 </span>                &lt;action android:name=\"android.intent.action.MAIN\" />\n"
                        + "</pre>\n",
                html);
    }

    public void testJava1() {
        //noinspection all // Sample code
        @Language("Java") String source = ""
                + "import static java.util.regex.Matcher.quoteReplacement;\n"
                + "import static java.util.regex.Pattern.DOTALL;\n"
                + "\n"
                + "/**\n"
                + " * Comprehensive language test to verify ECJ PSI bridge\n"
                + " */\n"
                + "@SuppressWarnings(\"all\")\n"
                + "public abstract class LanguageTest<K> extends ArrayList<K> implements Comparable<K>, Cloneable {\n"
                + "    public LanguageTest(@SuppressWarnings(\"unused\") int x) {\n"
                + "        super(x);\n"
                + "    }\n"
                + "}\n";
        int beginOffset = source.indexOf("@SuppressWarnings(\"all\")");
        int endOffset = source.indexOf('{', beginOffset);
        boolean error = true;
        assertTrue(beginOffset != -1);
        assertTrue(endOffset != -1);

        @Language("HTML")
        String html = highlight(source, beginOffset, endOffset, error, "Foo.java", false);

        Assert.assertEquals(""
                        + "<pre class=\"errorlines\">\n"
                        + "<span class=\"lineno\">  4 </span><span class=\"javadoc\">/**\n"
                        + "</span><span class=\"lineno\">  5 </span><span class=\"javadoc\"> * Comprehensive language test to verify ECJ PSI bridge\n"
                        + "</span><span class=\"lineno\">  6 </span><span class=\"javadoc\"> */</span>\n"
                        + "<span class=\"caretline\"><span class=\"lineno\">  7 </span><span class=\"error\"><span class=\"annotation\">@SuppressWarnings</span>(<span class=\"string\">\"all\"</span>)</span></span>\n"
                        + "<span class=\"lineno\">  8 </span><span class=\"keyword\">public</span> <span class=\"keyword\">abstract</span> <span class=\"keyword\">class</span> LanguageTest&lt;K> <span class=\"keyword\">extends</span> ArrayList&lt;K> <span class=\"keyword\">implements</span> Comparable&lt;K>, Cloneable {\n"
                        + "<span class=\"lineno\">  9 </span>    <span class=\"keyword\">public</span> LanguageTest(<span class=\"annotation\">@SuppressWarnings</span>(<span class=\"string\">\"unused\"</span>) <span class=\"keyword\">int</span> x) {\n"
                        + "<span class=\"lineno\"> 10 </span>        <span class=\"keyword\">super</span>(x);\n"
                        + "</pre>\n",
                html);
    }

    public void testJava2() {
        //noinspection all // Sample code
        @Language("Java") String source = ""
                + "import static java.util.regex.Matcher.quoteReplacement;\n"
                + "import static java.util.regex.Pattern.DOTALL;\n"
                + "\n"
                + "/**\n"
                + " * Comprehensive language test to verify ECJ PSI bridge\n"
                + " */\n"
                + "@SuppressWarnings(\"all\")\n"
                + "public abstract class LanguageTest<K> extends ArrayList<K> implements Comparable<K>, Cloneable {\n"
                + "    public void literals() {\n"
                + "        char x = 'x';\n"
                + "        char y = '\\u1234';\n"
                + "        Object n = null;\n"
                + "        Boolean b1 = true;\n"
                + "        int digits = 100_000_000;\n"
                + "        int hex = 0xAB;\n"
                + "        String s = \"myString\";\n"
                + "        int value1 = 42;\n"
                + "        long value2 = 42L;\n"
                + "        float value3 = 42f;\n"
                + "        float value4 = 42.0F;\n"
                + "        int[] array1 = new int[5];\n"
                + "        int[] array2 = new int[] { 1, 2, 3, 4 };\n"
                + "    }\n"
                + "}\n";
        int beginOffset = source.indexOf("int digits");
        int endOffset = source.indexOf(';', beginOffset);
        boolean error = true;
        assertTrue(beginOffset != -1);
        assertTrue(endOffset != -1);

        @Language("HTML")
        String html = highlight(source, beginOffset, endOffset, error, "Foo.java", false);

        Assert.assertEquals(""
                        + "<pre class=\"errorlines\">\n"
                        + "<span class=\"lineno\"> 11 </span>        <span class=\"keyword\">char</span> y = <span class=\"string\">'\\u1234'</span>;\n"
                        + "<span class=\"lineno\"> 12 </span>        Object n = <span class=\"keyword\">null</span>;\n"
                        + "<span class=\"lineno\"> 13 </span>        Boolean b1 = <span class=\"keyword\">true</span>;\n"
                        + "<span class=\"caretline\"><span class=\"lineno\"> 14 </span>        <span class=\"error\"><span class=\"keyword\">int</span> digits = <span class=\"number\">100_000_000</span></span>;</span>\n"
                        + "<span class=\"lineno\"> 15 </span>        <span class=\"keyword\">int</span> hex = <span class=\"number\">0xAB</span>;\n"
                        + "<span class=\"lineno\"> 16 </span>        String s = <span class=\"string\">\"myString\"</span>;\n"
                        + "<span class=\"lineno\"> 17 </span>        <span class=\"keyword\">int</span> value1 = <span class=\"number\">42</span>;\n"
                        + "</pre>\n",
                html);
    }

    public void testDedent() {
        //noinspection all // Sample code
        @Language("Java") String source = ""
                + "@SuppressWarnings(\"all\")\n"
                + "public abstract class LanguageTest<K> extends ArrayList<K> implements Comparable<K>, Cloneable {\n"
                + "    public void literals() {\n"
                + "        char x = 'x';\n"
                + "        char y = '\\u1234';\n"
                + "        Object n = null;\n"
                + "        Boolean b1 = true;\n"
                + "        int digits = 100_000_000; // This line needs to be long to push the dedent algorithm to not leave it in place. This line needs to be long to push the dedent algorithm to not leave it in place.\n"
                + "\n"
                + "        int hex = 0xAB;\n"
                + "        String s = \"myString\";\n"
                + "        int value1 = 42;\n"
                + "    }\n"
                + "}\n";
        int beginOffset = source.indexOf("int digits");
        int endOffset = source.indexOf(';', beginOffset);
        boolean error = true;
        assertTrue(beginOffset != -1);
        assertTrue(endOffset != -1);

        @Language("HTML")
        String html = highlight(source, beginOffset, endOffset, error, "Foo.java", true);

        Assert.assertEquals(""
                        + "<pre class=\"errorlines\">\n"
                        + "<span class=\"lineno\">  5 </span>  <span class=\"keyword\">char</span> y = <span class=\"string\">'\\u1234'</span>;\n"
                        + "<span class=\"lineno\">  6 </span>  Object n = <span class=\"keyword\">null</span>;\n"
                        + "<span class=\"lineno\">  7 </span>  Boolean b1 = <span class=\"keyword\">true</span>;\n"
                        + "<span class=\"caretline\"><span class=\"lineno\">  8 </span>  <span class=\"error\"><span class=\"keyword\">int</span> digits = <span class=\"number\">100_000_000</span></span>; <span class=\"comment\">// This line needs to be long to push the dedent algorithm to not leave it in place. This line needs to be long to push the dedent algorithm to not leave it in place.</span></span>\n"
                        + "<span class=\"lineno\">  9 </span>\n"
                        + "<span class=\"lineno\"> 10 </span>  <span class=\"keyword\">int</span> hex = <span class=\"number\">0xAB</span>;\n"
                        + "<span class=\"lineno\"> 11 </span>  String s = <span class=\"string\">\"myString\"</span>;\n"
                        + "</pre>\n",
                html);
    }

    public void testJavaAnnotations() {
        //noinspection all // Sample code
        @Language("Java") String source = ""
                + "/**\n"
                + " * Comprehensive language test to verify ECJ PSI bridge\n"
                + " */\n"
                + "@SuppressWarnings(\"all\")\n"
                + "public abstract class LanguageTest<K> extends ArrayList<K> implements Comparable<K>, Cloneable {\n"
                + "    public LanguageTest(@SuppressWarnings(\"unused\") int x) {\n"
                + "        super(x);\n"
                + "    }\n"
                + "}\n";
        int beginOffset = source.indexOf("LanguageTest(");
        int endOffset = source.indexOf('(', beginOffset);
        boolean error = true;
        assertTrue(beginOffset != -1);
        assertTrue(endOffset != -1);

        @Language("HTML")
        String html = highlight(source, beginOffset, endOffset, error, "Foo.java", false);

        Assert.assertEquals("<pre class=\"errorlines\">\n"
                        + "<span class=\"lineno\">  3 </span><span class=\"javadoc\"> */</span>\n"
                        + "<span class=\"lineno\">  4 </span><span class=\"annotation\">@SuppressWarnings</span>(<span class=\"string\">\"all\"</span>)\n"
                        + "<span class=\"lineno\">  5 </span><span class=\"keyword\">public</span> <span class=\"keyword\">abstract</span> <span class=\"keyword\">class</span> LanguageTest&lt;K> <span class=\"keyword\">extends</span> ArrayList&lt;K> <span class=\"keyword\">implements</span> Comparable&lt;K>, Cloneable {\n"
                        + "<span class=\"caretline\"><span class=\"lineno\">  6 </span>    <span class=\"keyword\">public</span> <span class=\"error\">LanguageTest</span>(<span class=\"annotation\">@SuppressWarnings</span>(<span class=\"string\">\"unused\"</span>) <span class=\"keyword\">int</span> x) {</span>\n"
                        + "<span class=\"lineno\">  7 </span>        <span class=\"keyword\">super</span>(x);\n"
                        + "<span class=\"lineno\">  8 </span>    }\n"
                        + "<span class=\"lineno\">  9 </span>}\n"
                        + "</pre>\n",
                html);
    }

    public void testGroovy() {
        //noinspection all // Sample code
        @Language("Groovy") String source = ""
                + "map.'single quote'\n"
                + "map.\"double quote\"\n"
                + "map.'''triple single quote'''\n"
                + "map.\"\"\"triple double quote\"\"\"\n"
                + "map./slashy string/\n"
                + "map.$/dollar slashy string/$    \n";
        int beginOffset = source.indexOf("triple double");
        int endOffset = source.indexOf('\"', beginOffset);
        boolean error = true;
        assertTrue(beginOffset != -1);
        assertTrue(endOffset != -1);

        @Language("HTML")
        String html = highlight(source, beginOffset, endOffset, error, "Foo.java", false);

        Assert.assertEquals(""
                        + "<pre class=\"errorlines\">\n"
                        + "<span class=\"lineno\"> 1 </span>map.<span class=\"string\">'single quote'</span>\n"
                        + "<span class=\"lineno\"> 2 </span>map.<span class=\"string\">\"double quote\"</span>\n"
                        + "<span class=\"lineno\"> 3 </span>map.<span class=\"string\">'''triple single quote'''</span>\n"
                        + "<span class=\"caretline\"><span class=\"lineno\"> 4 </span>map.<span class=\"string\">\"\"\"</span><span class=\"error\"><span class=\"string\">triple double quote</span></span><span class=\"string\">\"\"\"</span></span>\n"
                        + "<span class=\"lineno\"> 5 </span>map./slashy string/\n"
                        + "<span class=\"lineno\"> 6 </span>map.$/dollar slashy string/$    \n"
                        + "</pre>\n",
                html);
    }
}