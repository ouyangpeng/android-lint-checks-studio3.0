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

package com.android.tools.lint.checks.infrastructure;

import static com.android.SdkConstants.DOT_JAR;
import static com.android.SdkConstants.DOT_XML;
import static com.android.SdkConstants.FN_BUILD_GRADLE;

import com.android.annotations.NonNull;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.intellij.lang.annotations.Language;

/**
 * A utility class which provides unit test file descriptions
 *
 * <p><b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
public class TestFiles {

    private TestFiles() {
    }

    @NonNull
    public static LintDetectorTest.TestFile file() {
        return new LintDetectorTest.TestFile();
    }

    @NonNull
    public static LintDetectorTest.TestFile source(@NonNull String to, @NonNull String source) {
        return file().to(to).withSource(source);
    }

    @NonNull
    public static LintDetectorTest.TestFile java(@NonNull String to,
            @NonNull @Language("JAVA") String source) {
        return TestFile.JavaTestFile.create(to, source);
    }

    @NonNull
    public static LintDetectorTest.TestFile java(@NonNull @Language("JAVA") String source) {
        return TestFile.JavaTestFile.create(source);
    }

    @NonNull
    public static LintDetectorTest.TestFile kt(@NonNull @Language("kotlin") String source) {
        return kotlin(source);
    }

    @NonNull
    public static LintDetectorTest.TestFile kt(@NonNull String to,
            @NonNull @Language("kotlin") String source) {
        return kotlin(to, source);
    }

    @NonNull
    public static LintDetectorTest.TestFile kotlin(@NonNull @Language("kotlin") String source) {
        return TestFile.KotlinTestFile.create(source);
    }

    @NonNull
    public static LintDetectorTest.TestFile kotlin(@NonNull String to,
            @NonNull @Language("kotlin") String source) {
        return TestFile.KotlinTestFile.create(source);
    }

    @NonNull
    public static LintDetectorTest.TestFile xml(@NonNull String to,
            @NonNull @Language("XML") String source) {
        if (!to.endsWith(DOT_XML)) {
            throw new IllegalArgumentException("Expected .xml suffix for XML test file");
        }

        return TestFile.XmlTestFile.create(to, source);
    }

    @NonNull
    public static LintDetectorTest.TestFile copy(@NonNull String from,
            @NonNull TestResourceProvider resourceProvider) {
        return file().from(from, resourceProvider).to(from);
    }

    @NonNull
    public static LintDetectorTest.TestFile copy(@NonNull String from, @NonNull String to,
            @NonNull TestResourceProvider resourceProvider) {
        return file().from(from, resourceProvider).to(to);
    }

    @NonNull
    public static TestFile.GradleTestFile gradle(@NonNull String to,
            @NonNull @Language("Groovy") String source) {
        return new TestFile.GradleTestFile(to, source);
    }

    @NonNull
    public static TestFile.GradleTestFile gradle(@NonNull @Language("Groovy") String source) {
        return new TestFile.GradleTestFile(FN_BUILD_GRADLE, source);
    }

    @NonNull
    public static TestFile.ManifestTestFile manifest() {
        return new TestFile.ManifestTestFile();
    }

    @NonNull
    public static TestFile.PropertyTestFile projectProperties() {
        return new TestFile.PropertyTestFile();
    }

    @NonNull
    public static TestFile.BinaryTestFile bytecode(@NonNull String to, @NonNull TestFile.BytecodeProducer producer) {
        return new TestFile.BinaryTestFile(to, producer);
    }

    @NonNull
    public static TestFile.BinaryTestFile bytes(@NonNull String to, @NonNull byte[] bytes) {
        TestFile.BytecodeProducer producer = new TestFile.BytecodeProducer() {
            @NonNull
            @Override
            public byte[] produce() {
                return bytes;
            }
        };
        return new TestFile.BinaryTestFile(to, producer);
    }

    public static String toBase64(@NonNull byte[] bytes) {
        String base64 = Base64.getEncoder().encodeToString(bytes);
        return "\"\"\n+ \""
                + Joiner.on("\"\n+ \"").join(Splitter.fixedLength(60).split(base64)) + "\"";
    }

    public static String toBase64gzip(@NonNull byte[] bytes) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (GZIPOutputStream stream = new GZIPOutputStream(out)) {
                stream.write(bytes);
            }
            bytes = out.toByteArray();
        } catch (IOException ignore) {
            // Can't happen on a ByteArrayInputStream
        }

        String base64 = Base64.getEncoder().encodeToString(bytes);
        return "\"\" +\n\""
                + Joiner.on("\" +\n\"").join(Splitter.fixedLength(60).split(base64)) + "\"";
    }

    public static String toBase64(@NonNull File file) throws IOException {
        return toBase64(Files.toByteArray(file));
    }

    public static String toBase64gzip(@NonNull File file) throws IOException {
        return toBase64gzip(Files.toByteArray(file));
    }

    /**
     * Creates a test file from the given base64 data. To create this data, use {@link
     * #toBase64(File)} or {@link #toBase64(byte[])}, for example via
     * <pre>{@code assertEquals("", toBase64(new File("path/to/your.class")));}</pre>
     *
     * @param to      the file to write as
     * @param encoded the encoded data
     * @return the new test file
     */
    public static TestFile.BinaryTestFile base64(@NonNull String to, @NonNull String encoded) {
        encoded = encoded.replaceAll("\n", "");
        final byte[] bytes = Base64.getDecoder().decode(encoded);
        return new TestFile.BinaryTestFile(to, new TestFile.BytecodeProducer() {
            @NonNull
            @Override
            public byte[] produce() {
                return bytes;
            }
        });
    }

    /**
     * Decodes base64 strings into gzip data, then decodes that into a data file.
     * To create this data, use {@link #toBase64gzip(File)} or {@link #toBase64gzip(byte[])},
     * for example via
     * <pre>{@code assertEquals("", toBase64gzip(new File("path/to/your.class")));}</pre>
     */
    @NonNull
    public static TestFile.BinaryTestFile base64gzip(@NonNull String to, @NonNull String encoded) {
        encoded = encoded.replaceAll("\n", "");
        byte[] bytes = Base64.getDecoder().decode(encoded);

        try {
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            GZIPInputStream stream = new GZIPInputStream(in);
            bytes = ByteStreams.toByteArray(stream);
        } catch (IOException ignore) {
            // Can't happen on a ByteArrayInputStream
        }

        byte[] finalBytes = bytes;
        return new TestFile.BinaryTestFile(to, new TestFile.BytecodeProducer() {
            @NonNull
            @Override
            public byte[] produce() {
                return finalBytes;
            }
        });
    }

    public static LintDetectorTest.TestFile classpath(String... extraLibraries) {
        StringBuilder sb = new StringBuilder();
        sb.append(""
                + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<classpath>\n"
                + "\t<classpathentry kind=\"src\" path=\"src\"/>\n"
                + "\t<classpathentry kind=\"src\" path=\"gen\"/>\n"
                + "\t<classpathentry kind=\"con\" path=\"com.android.ide.eclipse.adt.ANDROID_FRAMEWORK\"/>\n"
                + "\t<classpathentry kind=\"con\" path=\"com.android.ide.eclipse.adt.LIBRARIES\"/>\n"
                + "\t<classpathentry kind=\"output\" path=\"bin/classes\"/>\n");
        for (String path : extraLibraries) {
            sb.append("\t<classpathentry kind=\"lib\" path=\"").append(path).append("\"/>\n");
        }
        sb.append("</classpath>\n");
        return source(".classpath", sb.toString());
    }

    @NonNull
    public static TestFile.JarTestFile jar(@NonNull String to) {
        return new TestFile.JarTestFile(to);
    }

    @NonNull
    public static TestFile.JarTestFile jar(@NonNull String to, @NonNull LintDetectorTest.TestFile... files) {
        if (!to.endsWith(DOT_JAR)) {
            throw new IllegalArgumentException("Expected .jar suffix for jar test file");
        }

        TestFile.JarTestFile jar = new TestFile.JarTestFile(to);
        jar.files(files);
        return jar;
    }

    public static TestFile.ImageTestFile image(@NonNull String to, int width, int height) {
        return new TestFile.ImageTestFile(to, width, height);
    }
}
