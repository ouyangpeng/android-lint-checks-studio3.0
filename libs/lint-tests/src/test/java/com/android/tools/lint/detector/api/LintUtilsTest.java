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

package com.android.tools.lint.detector.api;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.DOT_JAVA;
import static com.android.tools.lint.client.api.JavaParser.TYPE_BOOLEAN;
import static com.android.tools.lint.client.api.JavaParser.TYPE_BOOLEAN_WRAPPER;
import static com.android.tools.lint.client.api.JavaParser.TYPE_BYTE;
import static com.android.tools.lint.client.api.JavaParser.TYPE_BYTE_WRAPPER;
import static com.android.tools.lint.client.api.JavaParser.TYPE_CHAR;
import static com.android.tools.lint.client.api.JavaParser.TYPE_CHARACTER_WRAPPER;
import static com.android.tools.lint.client.api.JavaParser.TYPE_DOUBLE;
import static com.android.tools.lint.client.api.JavaParser.TYPE_DOUBLE_WRAPPER;
import static com.android.tools.lint.client.api.JavaParser.TYPE_FLOAT;
import static com.android.tools.lint.client.api.JavaParser.TYPE_FLOAT_WRAPPER;
import static com.android.tools.lint.client.api.JavaParser.TYPE_INT;
import static com.android.tools.lint.client.api.JavaParser.TYPE_INTEGER_WRAPPER;
import static com.android.tools.lint.client.api.JavaParser.TYPE_LONG;
import static com.android.tools.lint.client.api.JavaParser.TYPE_LONG_WRAPPER;
import static com.android.tools.lint.client.api.JavaParser.TYPE_SHORT;
import static com.android.tools.lint.client.api.JavaParser.TYPE_SHORT_WRAPPER;
import static com.android.tools.lint.detector.api.LintUtils.computeResourceName;
import static com.android.tools.lint.detector.api.LintUtils.convertVersion;
import static com.android.tools.lint.detector.api.LintUtils.describeCounts;
import static com.android.tools.lint.detector.api.LintUtils.editDistance;
import static com.android.tools.lint.detector.api.LintUtils.endsWith;
import static com.android.tools.lint.detector.api.LintUtils.findSubstring;
import static com.android.tools.lint.detector.api.LintUtils.formatList;
import static com.android.tools.lint.detector.api.LintUtils.getAutoBoxedType;
import static com.android.tools.lint.detector.api.LintUtils.getBaseName;
import static com.android.tools.lint.detector.api.LintUtils.getChildren;
import static com.android.tools.lint.detector.api.LintUtils.getCommonParent;
import static com.android.tools.lint.detector.api.LintUtils.getEncodedString;
import static com.android.tools.lint.detector.api.LintUtils.getFileNameWithParent;
import static com.android.tools.lint.detector.api.LintUtils.getFormattedParameters;
import static com.android.tools.lint.detector.api.LintUtils.getLocale;
import static com.android.tools.lint.detector.api.LintUtils.getLocaleAndRegion;
import static com.android.tools.lint.detector.api.LintUtils.getPrimitiveType;
import static com.android.tools.lint.detector.api.LintUtils.idReferencesMatch;
import static com.android.tools.lint.detector.api.LintUtils.isDataBindingExpression;
import static com.android.tools.lint.detector.api.LintUtils.isEditableTo;
import static com.android.tools.lint.detector.api.LintUtils.isImported;
import static com.android.tools.lint.detector.api.LintUtils.isJavaKeyword;
import static com.android.tools.lint.detector.api.LintUtils.isModelOlderThan;
import static com.android.tools.lint.detector.api.LintUtils.isXmlFile;
import static com.android.tools.lint.detector.api.LintUtils.resolveManifestName;
import static com.android.tools.lint.detector.api.LintUtils.splitPath;
import static com.android.tools.lint.detector.api.LintUtils.startsWith;
import static com.android.tools.lint.detector.api.LintUtils.stripIdPrefix;
import static com.android.utils.SdkUtils.escapePropertyValue;
import static com.google.common.truth.Truth.assertThat;
import static java.io.File.separatorChar;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.model.ApiVersion;
import com.android.ide.common.repository.GradleVersion;
import com.android.resources.ResourceFolderType;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.testutils.TestUtils;
import com.android.tools.lint.LintCliClient;
import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.checks.infrastructure.TestFiles;
import com.android.tools.lint.checks.infrastructure.TestIssueRegistry;
import com.android.tools.lint.checks.infrastructure.TestLintClient;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.client.api.LintDriver;
import com.android.tools.lint.client.api.LintRequest;
import com.android.tools.lint.client.api.UastParser;
import com.android.tools.lint.client.api.XmlParser;
import com.android.utils.Pair;
import com.android.utils.XmlUtils;
import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import com.intellij.openapi.Disposable;
import com.intellij.psi.PsiJavaFile;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junit.framework.TestCase;
import lombok.ast.Node;
import org.intellij.lang.annotations.Language;
import org.jetbrains.uast.UFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@SuppressWarnings("javadoc")
public class LintUtilsTest extends TestCase {
    public void testPrintList() throws Exception {
        assertEquals("bar, baz, foo",
                formatList(Arrays.asList("foo", "bar", "baz"), 3));
        assertEquals("foo, bar, baz",
                formatList(Arrays.asList("foo", "bar", "baz"), 3, false));
        assertEquals("foo, bar, baz",
                formatList(Arrays.asList("foo", "bar", "baz"), 5, false));

        assertEquals("foo, bar, baz... (3 more)",
                formatList(
                        Arrays.asList("foo", "bar", "baz", "4", "5", "6"), 3, false));
        assertEquals("foo... (5 more)",
                formatList(
                        Arrays.asList("foo", "bar", "baz", "4", "5", "6"), 1, false));
        assertEquals("foo, bar, baz",
                formatList(Arrays.asList("foo", "bar", "baz"), 0, false));
    }

    public void testIsDataBindingExpression() {
        assertTrue(isDataBindingExpression("@{foo}"));
        assertTrue(isDataBindingExpression("@={foo}"));
        assertFalse(isDataBindingExpression("@string/foo"));
        assertFalse(isDataBindingExpression("?string/foo"));
        assertFalse(isDataBindingExpression(""));
        assertFalse(isDataBindingExpression("foo"));
    }

    public void testDescribeCounts() throws Exception {
        assertThat(describeCounts(0, 0, true, true)).isEqualTo("No errors or warnings");
        assertThat(describeCounts(0, 0, true, false)).isEqualTo("no errors or warnings");
        assertThat(describeCounts(0, 1, true, true)).isEqualTo("1 warning");
        assertThat(describeCounts(1, 0, true, true)).isEqualTo("1 error");
        assertThat(describeCounts(0, 2, true, true)).isEqualTo("2 warnings");
        assertThat(describeCounts(2, 0, true, true)).isEqualTo("2 errors");
        assertThat(describeCounts(2, 1, false, true)).isEqualTo("2 errors and 1 warning");
        assertThat(describeCounts(1, 2, false, true)).isEqualTo("1 error and 2 warnings");
        assertThat(describeCounts(5, 4, false, true)).isEqualTo("5 errors and 4 warnings");
        assertThat(describeCounts(2, 1, true, true)).isEqualTo("2 errors, 1 warning");
        assertThat(describeCounts(1, 2, true, true)).isEqualTo("1 error, 2 warnings");
        assertThat(describeCounts(5, 4, true, true)).isEqualTo("5 errors, 4 warnings");
    }

    public void testEndsWith() throws Exception {
        assertTrue(endsWith("Foo", ""));
        assertTrue(endsWith("Foo", "o"));
        assertTrue(endsWith("Foo", "oo"));
        assertTrue(endsWith("Foo", "Foo"));
        assertTrue(endsWith("Foo", "FOO"));
        assertTrue(endsWith("Foo", "fOO"));

        assertFalse(endsWith("Foo", "f"));
    }

    public void testStartsWith() throws Exception {
        assertTrue(startsWith("FooBar", "Bar", 3));
        assertTrue(startsWith("FooBar", "BAR", 3));
        assertTrue(startsWith("FooBar", "Foo", 0));
        assertFalse(startsWith("FooBar", "Foo", 2));
    }

    public void testIsXmlFile() throws Exception {
        assertTrue(isXmlFile(new File("foo.xml")));
        assertTrue(isXmlFile(new File("foo.Xml")));
        assertTrue(isXmlFile(new File("foo.XML")));

        assertFalse(isXmlFile(new File("foo.png")));
        assertFalse(isXmlFile(new File("xml")));
        assertFalse(isXmlFile(new File("xml.png")));
    }

    public void testGetBasename() throws Exception {
        assertEquals("foo", getBaseName("foo.png"));
        assertEquals("foo", getBaseName("foo.9.png"));
        assertEquals(".foo", getBaseName(".foo"));
    }

    public void testEditDistance() {
        assertEquals(0, editDistance("kitten", "kitten"));

        // editing kitten to sitting has edit distance 3:
        //   replace k with s
        //   replace e with i
        //   append g
        assertEquals(3, editDistance("kitten", "sitting"));

        assertEquals(3, editDistance("saturday", "sunday"));
        assertEquals(1, editDistance("button", "bitton"));
        assertEquals(6, editDistance("radiobutton", "bitton"));

        assertEquals(6, editDistance("radiobutton", "bitton", 10));
        assertEquals(6, editDistance("radiobutton", "bitton", 6));
        assertEquals(Integer.MAX_VALUE, editDistance("radiobutton", "bitton", 3));

        assertTrue(isEditableTo("radiobutton", "bitton", 10));
        assertTrue(isEditableTo("radiobutton", "bitton", 6));
        assertFalse(isEditableTo("radiobutton", "bitton", 3));
    }

    public void testSplitPath() throws Exception {
        assertTrue(Arrays.equals(new String[] { "/foo", "/bar", "/baz" },
                Iterables.toArray(splitPath("/foo:/bar:/baz"), String.class)));

        assertTrue(Arrays.equals(new String[] { "/foo", "/bar" },
                Iterables.toArray(splitPath("/foo;/bar"), String.class)));

        assertTrue(Arrays.equals(new String[] { "/foo", "/bar:baz" },
                Iterables.toArray(splitPath("/foo;/bar:baz"), String.class)));

        assertTrue(Arrays.equals(new String[] { "\\foo\\bar", "\\bar\\foo" },
                Iterables.toArray(splitPath("\\foo\\bar;\\bar\\foo"), String.class)));

        assertTrue(Arrays.equals(new String[] { "${sdk.dir}\\foo\\bar", "\\bar\\foo" },
                Iterables.toArray(splitPath("${sdk.dir}\\foo\\bar;\\bar\\foo"),
                        String.class)));

        assertTrue(Arrays.equals(new String[] { "${sdk.dir}/foo/bar", "/bar/foo" },
                Iterables.toArray(splitPath("${sdk.dir}/foo/bar:/bar/foo"),
                        String.class)));

        assertTrue(Arrays.equals(new String[] { "C:\\foo", "/bar" },
                Iterables.toArray(splitPath("C:\\foo:/bar"), String.class)));
    }

    public void testCommonParen1() {
        assertEquals(new File("/a"), (getCommonParent(
                new File("/a/b/c/d/e"), new File("/a/c"))));
        assertEquals(new File("/a"), (getCommonParent(
                new File("/a/c"), new File("/a/b/c/d/e"))));

        assertEquals(new File("/"), getCommonParent(
                new File("/foo/bar"), new File("/bar/baz")));
        assertEquals(new File("/"), getCommonParent(
                new File("/foo/bar"), new File("/")));
        assertNull(getCommonParent(
               new File("C:\\Program Files"), new File("F:\\")));
        assertNull(getCommonParent(
                new File("C:/Program Files"), new File("F:/")));

        assertEquals(new File("/foo/bar/baz"), getCommonParent(
                new File("/foo/bar/baz"), new File("/foo/bar/baz")));
        assertEquals(new File("/foo/bar"), getCommonParent(
                new File("/foo/bar/baz"), new File("/foo/bar")));
        assertEquals(new File("/foo/bar"), getCommonParent(
                new File("/foo/bar/baz"), new File("/foo/bar/foo")));
        assertEquals(new File("/foo"), getCommonParent(
                new File("/foo/bar"), new File("/foo/baz")));
        assertEquals(new File("/foo"), getCommonParent(
                new File("/foo/bar"), new File("/foo/baz")));
        assertEquals(new File("/foo/bar"), getCommonParent(
                new File("/foo/bar"), new File("/foo/bar/baz")));
    }

    public void testCommonParent2() {
        assertEquals(new File("/"), getCommonParent(
                Arrays.asList(new File("/foo/bar"), new File("/bar/baz"))));
        assertEquals(new File("/"), getCommonParent(
                Arrays.asList(new File("/foo/bar"), new File("/"))));
        assertNull(getCommonParent(
                Arrays.asList(new File("C:\\Program Files"), new File("F:\\"))));
        assertNull(getCommonParent(
                Arrays.asList(new File("C:/Program Files"), new File("F:/"))));

        assertEquals(new File("/foo"), getCommonParent(
                Arrays.asList(new File("/foo/bar"), new File("/foo/baz"))));
        assertEquals(new File("/foo"), getCommonParent(
                Arrays.asList(new File("/foo/bar"), new File("/foo/baz"),
                        new File("/foo/baz/f"))));
        assertEquals(new File("/foo/bar"), getCommonParent(
                Arrays.asList(new File("/foo/bar"), new File("/foo/bar/baz"),
                        new File("/foo/bar/foo2/foo3"))));
    }

    public void testStripIdPrefix() throws Exception {
        assertEquals("foo", stripIdPrefix("@+id/foo"));
        assertEquals("foo", stripIdPrefix("@id/foo"));
        assertEquals("foo", stripIdPrefix("foo"));
    }

    public void testIdReferencesMatch() throws Exception {
        assertTrue(idReferencesMatch("@+id/foo", "@+id/foo"));
        assertTrue(idReferencesMatch("@id/foo", "@id/foo"));
        assertTrue(idReferencesMatch("@id/foo", "@+id/foo"));
        assertTrue(idReferencesMatch("@+id/foo", "@id/foo"));

        assertFalse(idReferencesMatch("@+id/foo", "@+id/bar"));
        assertFalse(idReferencesMatch("@id/foo", "@+id/bar"));
        assertFalse(idReferencesMatch("@+id/foo", "@id/bar"));
        assertFalse(idReferencesMatch("@+id/foo", "@+id/bar"));

        assertFalse(idReferencesMatch("@+id/foo", "@+id/foo1"));
        assertFalse(idReferencesMatch("@id/foo", "@id/foo1"));
        assertFalse(idReferencesMatch("@id/foo", "@+id/foo1"));
        assertFalse(idReferencesMatch("@+id/foo", "@id/foo1"));

        assertFalse(idReferencesMatch("@+id/foo1", "@+id/foo"));
        assertFalse(idReferencesMatch("@id/foo1", "@id/foo"));
        assertFalse(idReferencesMatch("@id/foo1", "@+id/foo"));
        assertFalse(idReferencesMatch("@+id/foo1", "@id/foo"));
    }

    private static void checkEncoding(String encoding, boolean writeBom, String lineEnding)
            throws Exception {
        @SuppressWarnings("StringBufferReplaceableByString")
        StringBuilder sb = new StringBuilder();

        // Norwegian extra vowel characters such as "latin small letter a with ring above"
        String value = "\u00e6\u00d8\u00e5";
        String expected = "First line." + lineEnding + "Second line." + lineEnding
                + "Third line." + lineEnding + value + lineEnding;
        sb.append(expected);
        File file = File.createTempFile("getEncodingTest" + encoding + writeBom, ".txt");
        file.deleteOnExit();
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(file));
        OutputStreamWriter writer = new OutputStreamWriter(stream, encoding);

        if (writeBom) {
            String normalized = encoding.toLowerCase(Locale.US).replace("-", "_");
            if (normalized.equals("utf_8")) {
                stream.write(0xef);
                stream.write(0xbb);
                stream.write(0xbf);
            } else if (normalized.equals("utf_16")) {
                stream.write(0xfe);
                stream.write(0xff);
            } else if (normalized.equals("utf_16le")) {
                stream.write(0xff);
                stream.write(0xfe);
            } else if (normalized.equals("utf_32")) {
                stream.write(0x0);
                stream.write(0x0);
                stream.write(0xfe);
                stream.write(0xff);
            } else if (normalized.equals("utf_32le")) {
                stream.write(0xff);
                stream.write(0xfe);
                stream.write(0x0);
                stream.write(0x0);
            } else {
                fail("Can't write BOM for encoding " + encoding);
            }
        }
        writer.write(sb.toString());
        writer.close();

        String s = getEncodedString(new LintCliClient(), file, true).toString();
        assertEquals(expected, s);

        CharSequence seq = getEncodedString(new LintCliClient(), file, false);
        if (encoding.equalsIgnoreCase("utf-8")) {
            assertFalse(seq instanceof String);
        }
        assertEquals(expected, seq.toString());
    }

    public void testGetEncodedString() throws Exception {
        checkEncoding("utf-8", false /*bom*/, "\n");
        checkEncoding("UTF-8", false /*bom*/, "\n");
        checkEncoding("UTF_16", false /*bom*/, "\n");
        checkEncoding("UTF-16", false /*bom*/, "\n");
        checkEncoding("UTF_16LE", false /*bom*/, "\n");

        // Try BOM's
        checkEncoding("utf-8", true /*bom*/, "\n");
        checkEncoding("UTF-8", true /*bom*/, "\n");
        checkEncoding("UTF_16", true /*bom*/, "\n");
        checkEncoding("UTF-16", true /*bom*/, "\n");
        checkEncoding("UTF_16LE", true /*bom*/, "\n");
        checkEncoding("UTF_32", true /*bom*/, "\n");
        checkEncoding("UTF_32LE", true /*bom*/, "\n");

        // Make sure this works for \r and \r\n as well
        checkEncoding("UTF-16", false /*bom*/, "\r");
        checkEncoding("UTF_16LE", false /*bom*/, "\r");
        checkEncoding("UTF-16", false /*bom*/, "\r\n");
        checkEncoding("UTF_16LE", false /*bom*/, "\r\n");
        checkEncoding("UTF-16", true /*bom*/, "\r");
        checkEncoding("UTF_16LE", true /*bom*/, "\r");
        checkEncoding("UTF_32", true /*bom*/, "\r");
        checkEncoding("UTF_32LE", true /*bom*/, "\r");
        checkEncoding("UTF-16", true /*bom*/, "\r\n");
        checkEncoding("UTF_16LE", true /*bom*/, "\r\n");
        checkEncoding("UTF_32", true /*bom*/, "\r\n");
        checkEncoding("UTF_32LE", true /*bom*/, "\r\n");
    }

    public void testGetLocale() throws Exception {
        assertNull(getLocale(""));
        assertNull(getLocale("values"));
        assertNull(getLocale("values-xlarge-port"));
        assertEquals("en", getLocale("values-en").getLanguage());
        assertEquals("pt", getLocale("values-pt-rPT-nokeys").getLanguage());
        assertEquals("pt", getLocale("values-b+pt+PT-nokeys").getLanguage());
        assertEquals("zh", getLocale("values-zh-rCN-keyshidden").getLanguage());
    }


    public void testGetLocale2() throws Exception {
        LintDetectorTest.TestFile xml;
        XmlContext context;

        xml = TestFiles.xml("res/values/strings.xml", ""
                + "<resources>\n"
                + "</resources>\n");
        context = createXmlContext(xml.getContents(), new File(xml.getTargetPath()));
        assertNull(getLocale(context));

        xml = TestFiles.xml("res/values-no/strings.xml", ""
                + "<resources>\n"
                + "</resources>\n");
        context = createXmlContext(xml.getContents(), new File(xml.getTargetPath()));
        assertEquals("no", getLocale(context).getLanguage());

        xml = TestFiles.xml("res/values/strings.xml", ""
                + "<resources tools:locale=\"nb\" xmlns:tools=\"http://schemas.android.com/tools\">\n"
                + "</resources>\n");
        context = createXmlContext(xml.getContents(), new File(xml.getTargetPath()));
        assertEquals("nb", getLocale(context).getLanguage());

        // tools:locale wins over folder location
        xml = TestFiles.xml("res/values-fr/strings.xml", ""
                + "<resources tools:locale=\"nb\" xmlns:tools=\"http://schemas.android.com/tools\">\n"
                + "</resources>\n");
        context = createXmlContext(xml.getContents(), new File(xml.getTargetPath()));
        assertEquals("nb", getLocale(context).getLanguage());
    }

    public void testGetLocaleAndRegion() throws Exception {
        assertNull(getLocaleAndRegion(""));
        assertNull(getLocaleAndRegion("values"));
        assertNull(getLocaleAndRegion("values-xlarge-port"));
        assertEquals("en", getLocaleAndRegion("values-en"));
        assertEquals("pt-rPT", getLocaleAndRegion("values-pt-rPT-nokeys"));
        assertEquals("b+pt+PT", getLocaleAndRegion("values-b+pt+PT-nokeys"));
        assertEquals("zh-rCN", getLocaleAndRegion("values-zh-rCN-keyshidden"));
        assertEquals("ms", getLocaleAndRegion("values-ms-keyshidden"));
    }

    @SuppressWarnings("all") // sample code
    public void testIsImported() throws Exception {
        assertFalse(isImported(getCompilationUnit(
                "package foo.bar;\n" +
                "class Foo {\n" +
                "}\n"),
                "android.app.Activity"));

        assertTrue(isImported(getCompilationUnit(
                "package foo.bar;\n" +
                "import foo.bar.*;\n" +
                "import android.app.Activity;\n" +
                "import foo.bar.Baz;\n" +
                "class Foo {\n" +
                "}\n"),
                "android.app.Activity"));

        assertTrue(isImported(getCompilationUnit(
                "package foo.bar;\n" +
                "import android.app.Activity;\n" +
                "class Foo {\n" +
                "}\n"),
                "android.app.Activity"));

        assertTrue(isImported(getCompilationUnit(
                "package foo.bar;\n" +
                "import android.app.*;\n" +
                "class Foo {\n" +
                "}\n"),
                "android.app.Activity"));

        assertFalse(isImported(getCompilationUnit(
                "package foo.bar;\n" +
                "import android.app.*;\n" +
                "import foo.bar.Activity;\n" +
                "class Foo {\n" +
                "}\n"),
                "android.app.Activity"));
    }

    public void testComputeResourceName() {
        assertEquals("", computeResourceName("", "", null));
        assertEquals("foo", computeResourceName("", "foo", null));
        assertEquals("foo", computeResourceName("foo", "", null));
        assertEquals("prefix_name", computeResourceName("prefix_", "name", null));
        assertEquals("prefixName", computeResourceName("prefix", "name", null));
        assertEquals("PrefixName", computeResourceName("prefix", "Name", null));
        assertEquals("PrefixName", computeResourceName("prefix_", "Name", null));
        assertEquals("MyPrefixName", computeResourceName("myPrefix", "Name", null));
        assertEquals("my_prefix_name", computeResourceName("myPrefix", "name", ResourceFolderType.LAYOUT));
        assertEquals("UnitTestPrefixContentFrame", computeResourceName("unit_test_prefix_", "ContentFrame", ResourceFolderType.VALUES));
        assertEquals("MyPrefixMyStyle", computeResourceName("myPrefix_", "MyStyle", ResourceFolderType.VALUES));
    }

    public static Node getCompilationUnit(@Language("JAVA") String javaSource) {
        return getCompilationUnit(javaSource, new File("test"));
    }

    public static Node getCompilationUnit(@Language("JAVA") String javaSource, File relativePath) {
        JavaContext context = parse(javaSource, relativePath);
        return context.getCompilationUnit();
    }

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+(.*)\\s*;");
    private static final Pattern CLASS_PATTERN = Pattern
            .compile("class\\s*(\\S+)\\s*(extends.*)?(implements.*)?\\{");

    public static Pair<JavaContext,Disposable> parsePsi(@Language("JAVA") String javaSource) {
        // Figure out the "to" path: the package plus class name + java in the src/ folder
        Matcher matcher = PACKAGE_PATTERN.matcher(javaSource);
        String pkg = "";
        if (matcher.find()) {
            pkg = matcher.group(1).trim();
        }
        matcher = CLASS_PATTERN.matcher(javaSource);
        assertTrue("Couldn't find class declaration in source", matcher.find());
        String cls = matcher.group(1).trim();
        int typeParameter = cls.indexOf('<');
        if (typeParameter != -1) {
            cls = cls.substring(0, typeParameter);
        }
        String path = "src/" + pkg.replace('.', '/') + '/' + cls + DOT_JAVA;
        return parsePsi(javaSource, new File(path.replace('/', separatorChar)));
    }

    /** @deprecated Use {@link #parseUast(String, File)} or {@link #parsePsi(String, File)}
     * instead */
    @Deprecated
    public static JavaContext parse(@Language("JAVA") String javaSource) {
        // Figure out the "to" path: the package plus class name + java in the src/ folder
        Matcher matcher = PACKAGE_PATTERN.matcher(javaSource);
        String pkg = "";
        if (matcher.find()) {
            pkg = matcher.group(1).trim();
        }
        matcher = CLASS_PATTERN.matcher(javaSource);
        assertTrue("Couldn't find class declaration in source", matcher.find());
        String cls = matcher.group(1).trim();
        String path = "src/" + pkg.replace('.', '/') + '/' + cls + DOT_JAVA;
        return parse(javaSource, new File(path.replace('/', separatorChar)));
    }

    /** @deprecated Use {@link #parseUast(String, File)} or {@link #parsePsi(String, File)}
     * instead */
    @Deprecated
    public static JavaContext parse(@Language("JAVA") final String javaSource,
            final File relativePath) {
        Pair<JavaContext, Disposable> parse =
                parse(javaSource, relativePath, true, false, false);
        // Disposal not necessary for lombok
        return parse.getFirst();
    }

    public static Pair<JavaContext,Disposable> parsePsi(@Language("JAVA") final String javaSource,
            final File relativePath) {
        return parse(javaSource, relativePath, false, true, false);
    }

    public static Pair<JavaContext,Disposable> parseUast(@Language("JAVA") final String javaSource,
            final File relativePath) {
        return parse(javaSource, relativePath, false, true, true);
    }

    private static Project createTestProjectForFile(File dir, File relativePath, String source) {
        final File fullPath = new File(dir, relativePath.getPath());
        fullPath.getParentFile().mkdirs();
        try {
            Files.write(source, fullPath, Charsets.UTF_8);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        LintCliClient client = new LintCliClient() {
            @NonNull
            @Override
            public CharSequence readFile(@NonNull File file) {
                if (file.getPath().equals(fullPath.getPath())) {
                    return source;
                }
                return super.readFile(file);
            }

            @Nullable
            @Override
            public IAndroidTarget getCompileTarget(@NonNull Project project) {
                IAndroidTarget[] targets = getTargets();
                for (int i = targets.length - 1; i >= 0; i--) {
                    IAndroidTarget target = targets[i];
                    if (target.isPlatform()) {
                        return target;
                    }
                }

                return super.getCompileTarget(project);
            }

            @NonNull
            @Override
            public File getSdkHome() {
                return TestUtils.getSdk();
            }
        };
        Project project = client.getProject(dir, dir);
        client.initializeProjects(Collections.singletonList(project));
        return project;
    }

    public static XmlContext createXmlContext(@Language("XML") String xml, File relativePath) {
        File dir = new File(System.getProperty("java.io.tmpdir"));
        final File fullPath = new File(dir, relativePath.getPath());
        Project project = createTestProjectForFile(dir, relativePath, xml);
        LintCliClient client = (LintCliClient) project.getClient();

        LintRequest request = new LintRequest(client, Collections.singletonList(fullPath));
        LintDriver driver = new LintDriver(new TestIssueRegistry(),
                new LintCliClient(), request);
        driver.setScope(Scope.JAVA_FILE_SCOPE);
        ResourceFolderType folderType = ResourceFolderType.getFolderType(
                relativePath.getParentFile().getName());

        XmlParser parser = client.getXmlParser();
        Document document = parser.parseXml(xml, fullPath);
        return new XmlTestContext(driver, project, xml, fullPath, folderType, parser, document);
    }

    public static Pair<JavaContext,Disposable>  parse(@Language("JAVA") final String javaSource,
            final File relativePath, boolean lombok, boolean psi, boolean uast) {
        // TODO: Clean up -- but where?
        File dir = Files.createTempDir();
        final File fullPath = new File(dir, relativePath.getPath());
        fullPath.getParentFile().mkdirs();
        try {
            Files.write(javaSource, fullPath, Charsets.UTF_8);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        Project project = createTestProjectForFile(dir, relativePath, javaSource);
        LintCliClient client = (LintCliClient) project.getClient();
        LintRequest request = new LintRequest(client, Collections.singletonList(fullPath));

        LintDriver driver = new LintDriver(new TestIssueRegistry(),
                new LintCliClient(), request);
        driver.setScope(Scope.JAVA_FILE_SCOPE);
        JavaTestContext context = new JavaTestContext(driver, client, project, javaSource, fullPath);
        JavaParser parser = null;
        if (lombok || psi) {
            parser = client.getJavaParser(project);
            context.setParser(parser);
            assertNotNull(parser);
            parser.prepareJavaParse(Collections.singletonList(context));
        }
        if (lombok) {
            Node compilationUnit = parser.parseJava(context);
            assertNotNull(javaSource, compilationUnit);
            context.setCompilationUnit(compilationUnit);
        }
        if (psi) {
            PsiJavaFile javaFile = parser.parseJavaToPsi(context);
            assertNotNull("Couldn't parse source", javaFile);
            context.setJavaFile(javaFile);
        }
        if (uast) {
            UastParser uastParser = client.getUastParser(project);
            assertNotNull(uastParser);
            context.setUastParser(uastParser);
            uastParser.prepare(Collections.singletonList(context));
            UFile uFile = uastParser.parse(context);
            context.setUastFile(uFile);
        }
        Disposable disposable = () -> client.disposeProjects(Collections.singletonList(project));
        return Pair.of(context, disposable);
    }

    public void testConvertVersion() {
        assertEquals(new AndroidVersion(5, null), convertVersion(new DefaultApiVersion(5, null),
                null));
        assertEquals(new AndroidVersion(19, null), convertVersion(new DefaultApiVersion(19, null),
                null));
        //noinspection SpellCheckingInspection
        assertEquals(new AndroidVersion(18, "KITKAT"), // a preview platform API level is not final
                convertVersion(new DefaultApiVersion(0, "KITKAT"),
                null));
    }

    public void testIsModelOlderThan() throws Exception {
        Project project = mock(Project.class);
        when(project.getGradleModelVersion()).thenReturn(GradleVersion.parse("0.10.4"));

        assertTrue(isModelOlderThan(project, 0, 10, 5));
        assertTrue(isModelOlderThan(project, 0, 11, 0));
        assertTrue(isModelOlderThan(project, 0, 11, 4));
        assertTrue(isModelOlderThan(project, 1, 0, 0));

        project = mock(Project.class);
        when(project.getGradleModelVersion()).thenReturn(GradleVersion.parse("0.11.0"));

        assertTrue(isModelOlderThan(project, 1, 0, 0));
        assertFalse(isModelOlderThan(project, 0, 11, 0));
        assertFalse(isModelOlderThan(project, 0, 10, 4));

        project = mock(Project.class);
        when(project.getGradleModelVersion()).thenReturn(GradleVersion.parse("0.11.5"));

        assertTrue(isModelOlderThan(project, 1, 0, 0));
        assertFalse(isModelOlderThan(project, 0, 11, 0));

        project = mock(Project.class);
        when(project.getGradleModelVersion()).thenReturn(GradleVersion.parse("1.0.0"));

        assertTrue(isModelOlderThan(project, 1, 0, 1));
        assertFalse(isModelOlderThan(project, 1, 0, 0));
        assertFalse(isModelOlderThan(project, 0, 11, 0));

        project = mock(Project.class);
        assertTrue(isModelOlderThan(project, 0, 0, 0, true));
        assertFalse(isModelOlderThan(project, 0, 0, 0, false));
    }

    private static final class DefaultApiVersion implements ApiVersion {
        private final int mApiLevel;
        private final String mCodename;

        public DefaultApiVersion(int apiLevel, @Nullable String codename) {
            mApiLevel = apiLevel;
            mCodename = codename;
        }

        @Override
        public int getApiLevel() {
            return mApiLevel;
        }

        @Nullable
        @Override
        public String getCodename() {
            return mCodename;
        }

        @NonNull
        @Override
        public String getApiString() {
            fail("Not needed in this test");
            return "<invalid>";
        }
    }

    public void testFindSubstring() {
       assertEquals("foo", findSubstring("foo", null, null));
       assertEquals("foo", findSubstring("foo  ", null, "  "));
       assertEquals("foo", findSubstring("  foo", "  ", null));
       assertEquals("foo", findSubstring("[foo]", "[", "]"));
    }

    public void testGetFormattedParameters() {
        assertEquals(Arrays.asList("foo","bar"),
                getFormattedParameters("Prefix %1$s Divider %2$s Suffix",
                        "Prefix foo Divider bar Suffix"));
    }

    public void testEscapePropertyValue() throws Exception {
        assertEquals("foo", escapePropertyValue("foo"));
        assertEquals("\\  foo  ", escapePropertyValue("  foo  "));
        assertEquals("c\\:/foo/bar", escapePropertyValue("c:/foo/bar"));
        assertEquals("\\!\\#\\:\\\\a\\\\b\\\\c", escapePropertyValue("!#:\\a\\b\\c"));
        assertEquals(
                "foofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoo\\#foofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoo",
                escapePropertyValue("foofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoo#foofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoofoo"));
    }

    public void testGetAutoBoxedType() {
        assertEquals(TYPE_INTEGER_WRAPPER, getAutoBoxedType(TYPE_INT));
        assertEquals(TYPE_INT, getPrimitiveType(TYPE_INTEGER_WRAPPER));

        String[] pairs = new String[]{
                TYPE_BOOLEAN,
                TYPE_BOOLEAN_WRAPPER,
                TYPE_BYTE,
                TYPE_BYTE_WRAPPER,
                TYPE_CHAR,
                TYPE_CHARACTER_WRAPPER,
                TYPE_DOUBLE,
                TYPE_DOUBLE_WRAPPER,
                TYPE_FLOAT,
                TYPE_FLOAT_WRAPPER,
                TYPE_INT,
                TYPE_INTEGER_WRAPPER,
                TYPE_LONG,
                TYPE_LONG_WRAPPER,
                TYPE_SHORT,
                TYPE_SHORT_WRAPPER
        };

        for (int i = 0; i < pairs.length; i += 2) {
            String primitive = pairs[i];
            String autoBoxed = pairs[i + 1];
            assertEquals(autoBoxed, getAutoBoxedType(primitive));
            assertEquals(primitive, getPrimitiveType(autoBoxed));
        }
    }

    @NonNull
    private static Element getElementWithNameValue(
            @NonNull @Language("XML") String xml,
            @NonNull String activityName) {
        Document document = XmlUtils.parseDocumentSilently(xml, true);
        assertNotNull(document);
        Element root = document.getDocumentElement();
        assertNotNull(root);
        for (Element application : getChildren(root)) {
            for (Element element : getChildren(application)) {
                String name = element.getAttributeNS(ANDROID_URI, ATTR_NAME);
                if (activityName.equals(name)) {
                    return element;
                }
            }
        }

        fail("Didn't find " + activityName);
        throw new AssertionError("Didn't find " + activityName);
    }

    public void testResolveManifestName() throws Exception {
        assertEquals("test.pkg.TestActivity", resolveManifestName(getElementWithNameValue(""
                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    package=\"test.pkg\">\n"
                + "    <application>\n"
                + "        <activity android:name=\".TestActivity\" />\n"
                + "    </application>\n"
                + "</manifest>\n", ".TestActivity")));


        assertEquals("test.pkg.TestActivity", resolveManifestName(getElementWithNameValue(""
                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    package=\"test.pkg\">\n"
                + "    <application>\n"
                + "        <activity android:name=\"TestActivity\" />\n"
                + "    </application>\n"
                + "</manifest>\n", "TestActivity")));

        assertEquals("test.pkg.TestActivity", resolveManifestName(getElementWithNameValue(""
                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    package=\"test.pkg\">\n"
                + "    <application>\n"
                + "        <activity android:name=\"test.pkg.TestActivity\" />\n"
                + "    </application>\n"
                + "</manifest>\n", "test.pkg.TestActivity")));

        assertEquals("test.pkg.TestActivity.Bar", resolveManifestName(getElementWithNameValue(""
                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    package=\"test.pkg\">\n"
                + "    <application>\n"
                + "        <activity android:name=\"test.pkg.TestActivity$Bar\" />\n"
                + "    </application>\n"
                + "</manifest>\n", "test.pkg.TestActivity$Bar")));
    }

    public void testGetFileNameWithParent() {
        assertThat(getFileNameWithParent(new TestLintClient(), new File("tmp" +
            File.separator + "foo" + File.separator + "bar.baz"))).isEqualTo("foo/bar.baz");
        assertThat(getFileNameWithParent(new LintCliClient(), new File("tmp" +
                File.separator + "foo" + File.separator + "bar.baz"))).isEqualTo(
                        File.separatorChar == '/' ? "foo/bar.baz" : "foo\\\\bar.baz");
    }

    public void testJavaKeyword() {
        assertThat(isJavaKeyword("")).isFalse();
        assertThat(isJavaKeyword("iff")).isFalse();
        assertThat(isJavaKeyword("if")).isTrue();
        assertThat(isJavaKeyword("true")).isTrue();
        assertThat(isJavaKeyword("false")).isTrue();
    }

    private static class JavaTestContext extends JavaContext {
        private final String mJavaSource;
        public JavaTestContext(LintDriver driver, LintCliClient client, Project project,
                String javaSource, File file) {
            //noinspection ConstantConditions
            super(driver, project, null, file);

            mJavaSource = javaSource;
        }

        @Override
        @Nullable
        public String getContents() {
            return mJavaSource;
        }
    }

    private static class XmlTestContext extends XmlContext {
        private final String xmlSource;
        public XmlTestContext(LintDriver driver, Project project,
                String xmlSource, File file, ResourceFolderType type, XmlParser parser,
                Document document) {
            //noinspection ConstantConditions
            super(driver, project, null, file, type, parser, xmlSource, document);
            this.xmlSource = xmlSource;
        }

        @Override
        @Nullable
        public String getContents() {
            return xmlSource;
        }
    }

}
