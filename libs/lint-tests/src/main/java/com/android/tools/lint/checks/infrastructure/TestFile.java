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

import static com.android.SdkConstants.DOT_GIF;
import static com.android.SdkConstants.DOT_GRADLE;
import static com.android.SdkConstants.DOT_JAVA;
import static com.android.SdkConstants.DOT_JPEG;
import static com.android.SdkConstants.DOT_JPG;
import static com.android.SdkConstants.DOT_KT;
import static com.android.SdkConstants.DOT_PNG;
import static com.android.SdkConstants.DOT_XML;
import static com.android.SdkConstants.FN_ANDROID_MANIFEST_XML;
import static com.android.tools.lint.checks.infrastructure.BaseLintDetectorTest.makeTestFile;
import static com.android.utils.SdkUtils.escapePropertyValue;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.Warning;
import com.android.tools.lint.detector.api.Severity;
import com.android.utils.SdkUtils;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.intellij.util.ArrayUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import javax.imageio.ImageIO;
import junit.framework.TestCase;
import org.intellij.lang.annotations.Language;
import org.objectweb.asm.Opcodes;

/**
 * Test file description, which can copy from resource directory or from
 * a specified hardcoded string literal, and copy into a target directory
 */
public class TestFile {
    // TODO: Make this a top level class
    public String sourceRelativePath;
    public String targetRelativePath;
    public String contents;
    public byte[] bytes;
    private TestResourceProvider resourceProvider;
    public String targetRootFolder;

    public TestFile() {
    }

    public TestFile withSource(@NonNull String source) {
        contents = source;
        return this;
    }

    public TestFile from(@NonNull String from, @NonNull TestResourceProvider provider) {
        sourceRelativePath = from;
        resourceProvider = provider;
        return this;
    }

    public TestFile to(@NonNull String to) {
        targetRelativePath = to;
        return this;
    }

    public TestFile within(@Nullable String root) {
        targetRootFolder = root;
        return this;
    }

    public TestFile copy(@NonNull String relativePath, @NonNull TestResourceProvider provider) {
        // Support replacing filenames and paths with a => syntax, e.g.
        //   dir/file.txt=>dir2/dir3/file2.java
        // will read dir/file.txt from the test data and write it into the target
        // directory as dir2/dir3/file2.java
        String targetPath = relativePath;
        int replaceIndex = relativePath.indexOf("=>");
        if (replaceIndex != -1) {
            // foo=>bar
            targetPath = relativePath.substring(replaceIndex + "=>".length());
            relativePath = relativePath.substring(0, replaceIndex);
        }
        from(relativePath, provider);
        to(targetPath);
        return this;
    }

    @NonNull
    public String getTargetPath() {
        String target = targetRelativePath;
        if (targetRootFolder != null) {
            target = targetRootFolder + '/' + target;
        }

        return target;
    }

    @NonNull
    public File createFile(@NonNull File targetDir) throws IOException {
        InputStream stream;
        if (contents != null) {
            stream = new ByteArrayInputStream(contents.getBytes(Charsets.UTF_8));
        } else if (bytes != null) {
            stream = new ByteArrayInputStream(bytes);
        } else {
            assert resourceProvider != null;
            stream = resourceProvider.getTestResource(sourceRelativePath, true);
            assert stream != null : sourceRelativePath + " does not exist";
        }
        String target = getTargetPath();
        int index = target.lastIndexOf('/');
        String relative = null;
        String name = target;
        if (index != -1) {
            name = target.substring(index + 1);
            relative = target.substring(0, index);
        }

        return makeTestFile(targetDir, name, relative, stream);
    }

    @Nullable
    public String getContents() {
        if (contents != null) {
            return contents;
        } else if (bytes != null) {
            return Base64.getEncoder().encodeToString(bytes);
        } else if (sourceRelativePath != null) {
            assert resourceProvider != null;
            InputStream stream = resourceProvider.getTestResource(sourceRelativePath, true);
            if (stream != null) {
                try {
                    return new String(ByteStreams.toByteArray(stream), Charsets.UTF_8);
                } catch (IOException ignore) {
                    return "<couldn't open test file " + sourceRelativePath + ">";
                }
            }
        }
        return null;
    }

    @Nullable
    public String getRawContents() {
        return contents;
    }

    public TestFile withBytes(@NonNull byte[] bytes) {
        this.bytes = bytes;
        return this;
    }

    public static class JavaTestFile extends LintDetectorTest.TestFile {
        private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+(.*)\\s*;");
        private static final Pattern CLASS_PATTERN = Pattern.compile(
                "(class|interface|@interface|enum)\\s*(\\S+)\\s*(<.+>)?\\s*(extends.*)?\\s*(implements.*)?\\{",
                Pattern.MULTILINE);

        public JavaTestFile() {
        }

        @NonNull
        public static LintDetectorTest.TestFile create(@NonNull @Language("JAVA") String source) {
            // Figure out the "to" path: the package plus class name + java in the src/ folder
            Matcher matcher = PACKAGE_PATTERN.matcher(source);
            boolean foundPackage = matcher.find();
            assert foundPackage : "Couldn't find package declaration in source";
            String pkg = matcher.group(1).trim();
            matcher = CLASS_PATTERN.matcher(source);
            boolean foundClass = matcher.find();
            String to;
            if (!foundClass) {
                assert !source.contains("{") : "Couldn't find class declaration in source";
                to = pkg.replace('.', '/') + '/' + "package-info.java";
            } else {
                String cls = matcher.group(2).trim();
                if (cls.contains("<")) {
                    // Remove type variables
                    cls = cls.substring(0, cls.indexOf('<'));
                }
                to = pkg.replace('.', '/') + '/' + cls + DOT_JAVA;
            }

            return new JavaTestFile().to(to).within("src").withSource(source);
        }

        @NonNull
        public static LintDetectorTest.TestFile create(@NonNull String to,
                @NonNull @Language("JAVA") String source) {
            if (!to.endsWith(DOT_JAVA)) {
                throw new IllegalArgumentException("Expected .java suffix for Java test file");
            }
            return new JavaTestFile().to(to).withSource(source);
        }
    }

    public static class KotlinTestFile extends LintDetectorTest.TestFile {
        private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([\\S&&[^;]]*)");
        private static final Pattern CLASS_PATTERN = Pattern
                .compile("(class|interface|enum)\\s*(\\S+)\\s*(extends.*)?\\s*(implements.*)?\\{",
                        Pattern.MULTILINE);

        public KotlinTestFile() {
        }

        @NonNull
        public static LintDetectorTest.TestFile create(@NonNull @Language("kotlin") String source) {
            // Figure out the "to" path: the package plus class name + kt in the src/ folder
            Matcher matcher = PACKAGE_PATTERN.matcher(source);
            boolean foundPackage = matcher.find();
            assert foundPackage : "Couldn't find package declaration in source";
            String pkg = matcher.group(1).trim();
            matcher = CLASS_PATTERN.matcher(source);
            boolean foundClass = matcher.find();
            String cls;
            if (foundClass) {
                cls = matcher.group(2).trim();
                if (cls.contains("<")) {
                    // Remove type variables
                    cls = cls.substring(0, cls.indexOf('<'));
                }
            } else {
                // Don't require Kotlin test files to contain a class -- it could just be
                // top level functions
                cls = "test";
            }
            String to = pkg.replace('.', '/') + '/' + cls + DOT_KT;

            return new KotlinTestFile().to(to).within("src").withSource(source);
        }

        @NonNull
        public static LintDetectorTest.TestFile create(@NonNull String to,
                @NonNull @Language("kotlin") String source) {
            if (!to.endsWith(DOT_KT)) {
                throw new IllegalArgumentException("Expected .kt suffix for Kotlin test file");
            }
            return new KotlinTestFile().to(to).withSource(source);
        }
    }

    public static class XmlTestFile extends LintDetectorTest.TestFile {

        public XmlTestFile() {
        }

        @NonNull
        public static LintDetectorTest.TestFile create(@NonNull String to,
                @NonNull @Language("XML") String source) {
            if (!to.endsWith(DOT_XML)) {
                throw new IllegalArgumentException("Expected .xml suffix for XML test file");
            }

            String plainSource = stripErrorMarkers(source);
            return new XmlTestFile().withRawSource(source).to(to).withSource(plainSource);
        }

        private String rawSource;

        private XmlTestFile withRawSource(String rawSource) {
            this.rawSource = rawSource;
            return this;
        }

        /** Normally, XML processing instructions (other than {@code <?xml?>} are
         * taken to be inlined error messages and are stripped before being passed to
         * the XML parser. This flag allows you to tell lint to leave your processing
         * instructions alone, if needed by your test.
         */
        public XmlTestFile keepProcessingInstructions() {
            withSource(rawSource);
            return this;
        }

        @Nullable
        @Override
        public String getRawContents() {
            return rawSource;
        }

        private static String stripErrorMarkers(@NonNull String source) {
            if (source.contains("<?error")
                    || source.contains("<?warning")
                    || source.contains("?info")) {
                StringBuilder sb = new StringBuilder(source.length());
                int prev = 0;
                int index = 0;
                while (true) {
                    index = source.indexOf("<?", index);
                    if (index == -1) {
                        break;
                    }

                    // Keep XML preprocessing instructions
                    if (source.startsWith("<?xml", index)) {
                        index += 4;
                        continue;
                    }

                    sb.append(source.substring(prev, index));
                    index = source.indexOf("?>", index);
                    if (index == -1) {
                        break;
                    }
                    index += 2;
                    prev = index;
                }

                sb.append(source.substring(prev, source.length()));

                return sb.toString();
            }
            return source;
        }
    }

    public static class JarTestFile extends LintDetectorTest.TestFile {
        private final List<LintDetectorTest.TestFile> files = Lists.newArrayList();
        private final Map<LintDetectorTest.TestFile, String> path = Maps.newHashMap();

        public JarTestFile(@NonNull String to) {
            to(to);
        }

        public JarTestFile files(@NonNull LintDetectorTest.TestFile... files) {
            this.files.addAll(Arrays.asList(files));
            return this;
        }

        public JarTestFile add(@NonNull LintDetectorTest.TestFile file, @NonNull String path) {
            add(file);
            this.path.put(file, path);
            return this;
        }

        public JarTestFile add(@NonNull LintDetectorTest.TestFile file) {
            files.add(file);
            path.put(file, null);
            return this;
        }

        @Override
        public LintDetectorTest.TestFile withSource(@NonNull String source) {
            TestCase.fail("Don't call withSource on " + this.getClass());
            return this;
        }

        @Override
        @NonNull
        public String getContents() {
            TestCase.fail("Don't call getContents on binary " + this.getClass());
            return null;
        }

        @NonNull
        @Override
        public File createFile(@NonNull File targetDir) throws IOException {
            String target = getTargetPath();
            int index = target.lastIndexOf('/');
            String relative = null;
            String name = target;
            if (index != -1) {
                name = target.substring(index + 1);
                relative = target.substring(0, index);
            }

            File dir = targetDir;
            if (relative != null) {
                dir = new File(dir, relative);
                if (!dir.exists()) {
                    boolean mkdir = dir.mkdirs();
                    TestCase.assertTrue(dir.getPath(), mkdir);
                }
            } else if (!dir.exists()) {
                boolean mkdir = dir.mkdirs();
                TestCase.assertTrue(dir.getPath(), mkdir);
            }
            File tempFile = new File(dir, name);
            if (tempFile.exists()) {
                boolean deleted = tempFile.delete();
                TestCase.assertTrue(tempFile.getPath(), deleted);
            }

            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

            try (JarOutputStream jarOutputStream = new JarOutputStream(
                    new BufferedOutputStream(new FileOutputStream(tempFile)), manifest)) {
                for (LintDetectorTest.TestFile file : files) {
                    String path = this.path.get(file);
                    if (path == null) {
                        path = file.targetRelativePath;
                    }
                    jarOutputStream.putNextEntry(new ZipEntry(path));
                    if (file instanceof BinaryTestFile) {
                        byte[] bytes = ((BinaryTestFile) file).getBinaryContents();
                        TestCase.assertNotNull(file.targetRelativePath, bytes);
                        ByteStreams.copy(new ByteArrayInputStream(bytes), jarOutputStream);
                    } else {
                        String contents = file.getContents();
                        TestCase.assertNotNull(file.targetRelativePath, contents);
                        byte[] bytes = contents.getBytes(Charsets.UTF_8);
                        ByteStreams.copy(new ByteArrayInputStream(bytes), jarOutputStream);
                    }
                    jarOutputStream.closeEntry();
                }
            }

            return tempFile;
        }
    }

    public static class ImageTestFile extends BinaryTestFile {
        private final BufferedImage mImage;
        private String mFormat;

        public ImageTestFile(@NonNull String to, int width, int height) {
            super(to, new BytecodeProducer());

            if (SdkUtils.endsWithIgnoreCase(to, DOT_PNG)) {
                mFormat = "PNG";
            } else if (SdkUtils.endsWithIgnoreCase(to, DOT_JPG) || SdkUtils.endsWithIgnoreCase(to, DOT_JPEG)) {
                mFormat = "JPG";
            } else if (SdkUtils.endsWithIgnoreCase(to, DOT_GIF)) {
                mFormat = "GIF";
            }
            mImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        @NonNull
        public ImageTestFile fill(int argb) {
            return fill(0, 0, mImage.getWidth(), mImage.getHeight(), argb);
        }

        @NonNull
        public ImageTestFile fill(int x, int y, int width, int height, int argb) {
            Graphics2D graphics = mImage.createGraphics();
            graphics.setColor(new Color(argb, true));
            graphics.fillRect(x, y, width, height);
            graphics.dispose();
            return this;
        }

        @NonNull
        public ImageTestFile text(int x, int y, String s, int argb) {
            Graphics2D graphics = mImage.createGraphics();
            graphics.setColor(new Color(argb, true));
            graphics.drawString(s, x, y);
            graphics.dispose();
            return this;
        }

        public ImageTestFile fillOval(int x, int y, int width, int height, int argb) {
            Graphics2D graphics = mImage.createGraphics();
            graphics.setColor(new Color(argb, true));
            graphics.fillOval(x, y, width, height);
            graphics.dispose();
            return this;
        }

        @Override
        public byte[] getBinaryContents() {
            TestCase.assertNotNull("Must set image type", mFormat);
            ByteArrayOutputStream output = new ByteArrayOutputStream(2048);
            try {
                ImageIO.write(mImage, mFormat, output);
            } catch (IOException e) {
                TestCase.fail(e.getLocalizedMessage() + " writing " + output + " in format " + mFormat);
            }

            return output.toByteArray();
        }

        public ImageTestFile format(String format) {
            this.mFormat = format;
            return this;
        }
    }

    public static class ManifestTestFile extends LintDetectorTest.TestFile {
        public String pkg = "test.pkg";
        public String minSdk = "";
        public String targetSdk = "";
        public String[] permissions;

        public ManifestTestFile() {
            to(FN_ANDROID_MANIFEST_XML);
        }

        public ManifestTestFile minSdk(int min) {
            minSdk = String.valueOf(min);
            return this;
        }

        public ManifestTestFile minSdk(@NonNull String codename) {
            minSdk = codename;
            return this;
        }

        public ManifestTestFile targetSdk(int target) {
            targetSdk = String.valueOf(target);
            return this;
        }

        public ManifestTestFile targetSdk(@NonNull String codename) {
            targetSdk = codename;
            return this;
        }

        public ManifestTestFile permissions(String... permissions) {
            this.permissions = permissions;
            return this;
        }

        public ManifestTestFile pkg(String pkg) {
            this.pkg = pkg;
            return this;
        }

        @Override
        @NonNull
        public String getContents() {
            if (contents == null) {
                StringBuilder sb = new StringBuilder();
                sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
                sb.append("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n");
                sb.append("    package=\"").append(pkg).append("\"\n");
                sb.append("    android:versionCode=\"1\"\n");
                sb.append("    android:versionName=\"1.0\" >\n");
                if (!minSdk.isEmpty() || !targetSdk.isEmpty()) {
                    sb.append("    <uses-sdk ");
                    if (!minSdk.isEmpty()) {
                        sb.append(" android:minSdkVersion=\"").append(minSdk).append("\"");
                    }
                    if (!targetSdk.isEmpty()) {
                        sb.append(" android:targetSdkVersion=\"").append(targetSdk).append("\"");
                    }
                    sb.append(" />\n");
                }
                if (permissions != null && permissions.length > 0) {
                    StringBuilder permissionBlock = new StringBuilder();
                    for (String permission : permissions) {
                        permissionBlock
                                .append("    <uses-permission android:name=\"")
                                .append(permission)
                                .append("\" />\n");
                    }
                    sb.append(permissionBlock);
                    sb.append("\n");
                }

                sb.append(""
                        + "\n"
                        + "    <application\n"
                        + "        android:icon=\"@drawable/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\" >\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>");
                contents = sb.toString();
            }

            return contents;
        }

        @NonNull
        @Override
        public File createFile(@NonNull File targetDir) throws IOException {
            getContents(); // lazy init
            return super.createFile(targetDir);
        }
    }

    /** Produces byte arrays, for use with {@link BinaryTestFile} */
    public static class ByteProducer {
        @SuppressWarnings("MethodMayBeStatic") // intended for override
        @NonNull
        public byte[] produce() {
            return new byte[0];
        }
    }

    public static class BinaryTestFile extends LintDetectorTest.TestFile {
        private final BytecodeProducer producer;

        public BinaryTestFile(@NonNull String to, @NonNull BytecodeProducer producer) {
            to(to);
            this.producer = producer;
        }

        @Override
        public LintDetectorTest.TestFile withSource(@NonNull String source) {
            TestCase.fail("Don't call withSource on " + this.getClass());
            return this;
        }

        @Override
        @NonNull
        public String getContents() {
            TestCase.fail("Don't call getContents on binary " + this.getClass());
            return null;
        }

        public byte[] getBinaryContents() {
            return producer.produce();
        }

        @NonNull
        @Override
        public File createFile(@NonNull File targetDir) throws IOException {
            String target = getTargetPath();
            int index = target.lastIndexOf('/');
            String relative = null;
            String name = target;
            if (index != -1) {
                name = target.substring(index + 1);
                relative = target.substring(0, index);
            }
            InputStream stream = new ByteArrayInputStream(getBinaryContents());
            return makeTestFile(targetDir, name, relative, stream);
        }
    }

    public static class BytecodeProducer extends ByteProducer implements Opcodes {
        /**
         *  Typically generated by first getting asm output like this:
         * <pre>
         *     assertEquals("", asmify(new File("/full/path/to/my/file.class")));
         * </pre>
         * ...and when the test fails, the actual test output is the necessary assembly
         *
         */
        @Override
        @SuppressWarnings("MethodMayBeStatic") // intended for override
        @NonNull
        public byte[] produce() {
            return new byte[0];
        }
    }

    public static class GradleTestFile extends LintDetectorTest.TestFile {
        private GradleModelMocker mocker;

        public GradleTestFile(@NonNull String to, @NonNull @Language("Groovy") String source) {
            to(to).withSource(source);
            if (!to.endsWith(DOT_GRADLE)) {
                throw new IllegalArgumentException("Expected .gradle suffix for Gradle test file");
            }
        }

        @NonNull
        public GradleModelMocker getMocker(@NonNull File projectDir) {
            if (mocker == null) {
                assert contents != null;
                //noinspection LanguageMismatch
                mocker = new GradleModelMocker(contents).withProjectDir(projectDir);
            } else {
                assert projectDir == mocker.getProjectDir();
            }

            return mocker;
        }
    }

    public static class PropertyTestFile extends LintDetectorTest.TestFile {
        @SuppressWarnings("StringBufferField")
        private final StringBuilder stringBuilder = new StringBuilder();

        private int nextLibraryIndex = 1;

        public PropertyTestFile() {
            to("project.properties");
        }

        public PropertyTestFile property(String key, String value) {
            String escapedValue = escapePropertyValue(value);
            stringBuilder.append(key).append('=').append(escapedValue).append('\n');
            return this;
        }

        public PropertyTestFile compileSdk(int target) {
            stringBuilder.append("target=android-").append(Integer.toString(target)).append('\n');
            return this;
        }

        public PropertyTestFile library(boolean isLibrary) {
            stringBuilder.append("android.library=").append(Boolean.toString(isLibrary))
                    .append('\n');
            return this;
        }

        public PropertyTestFile manifestMerger(boolean merger) {
            stringBuilder.append("manifestmerger.enabled=").append(Boolean.toString(merger))
                    .append('\n');
            return this;
        }

        public PropertyTestFile dependsOn(String relative) {
            TestCase.assertTrue(relative.startsWith("../"));
            stringBuilder.append("android.library.reference.")
                    .append(Integer.toString(nextLibraryIndex++))
                    .append("=").append(escapePropertyValue(relative))
                    .append('\n');
            return this;
        }

        @Override
        public LintDetectorTest.TestFile withSource(@NonNull String source) {
            TestCase.fail("Don't call withSource on " + this.getClass());
            return this;
        }

        @Override
        @NonNull
        public String getContents() {
            contents = stringBuilder.toString();
            return contents;
        }

        @NonNull
        @Override
        public File createFile(@NonNull File targetDir) throws IOException {
            getContents(); // lazy init
            return super.createFile(targetDir);
        }
    }
}
