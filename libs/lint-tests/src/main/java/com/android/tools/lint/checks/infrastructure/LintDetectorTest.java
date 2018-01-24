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

package com.android.tools.lint.checks.infrastructure;

import static com.android.SdkConstants.ANDROID_MANIFEST_XML;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.NEW_ID_PREFIX;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.res2.AbstractResourceRepository;
import com.android.ide.common.res2.DuplicateDataException;
import com.android.ide.common.res2.MergingException;
import com.android.ide.common.res2.ResourceFile;
import com.android.ide.common.res2.ResourceItem;
import com.android.ide.common.res2.ResourceMerger;
import com.android.ide.common.res2.ResourceRepository;
import com.android.ide.common.res2.ResourceSet;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.sdklib.IAndroidTarget;
import com.android.testutils.TestUtils;
import com.android.tools.lint.EcjParser;
import com.android.tools.lint.ExternalAnnotationRepository;
import com.android.tools.lint.LintCliClient;
import com.android.tools.lint.LintCliFlags;
import com.android.tools.lint.LintCoreApplicationEnvironment;
import com.android.tools.lint.Reporter;
import com.android.tools.lint.Reporter.Stats;
import com.android.tools.lint.TextReporter;
import com.android.tools.lint.Warning;
import com.android.tools.lint.checks.ApiLookup;
import com.android.tools.lint.checks.BuiltinIssueRegistry;
import com.android.tools.lint.checks.infrastructure.TestFile.BinaryTestFile;
import com.android.tools.lint.checks.infrastructure.TestFile.BytecodeProducer;
import com.android.tools.lint.checks.infrastructure.TestFile.GradleTestFile;
import com.android.tools.lint.checks.infrastructure.TestFile.ImageTestFile;
import com.android.tools.lint.checks.infrastructure.TestFile.JarTestFile;
import com.android.tools.lint.checks.infrastructure.TestFile.JavaTestFile;
import com.android.tools.lint.checks.infrastructure.TestFile.KotlinTestFile;
import com.android.tools.lint.checks.infrastructure.TestFile.ManifestTestFile;
import com.android.tools.lint.client.api.CircularDependencyException;
import com.android.tools.lint.client.api.Configuration;
import com.android.tools.lint.client.api.DefaultConfiguration;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.client.api.LintDriver;
import com.android.tools.lint.client.api.LintRequest;
import com.android.tools.lint.client.api.UastParser;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.TextFormat;
import com.android.utils.ILogger;
import com.android.utils.StdLogger;
import com.android.utils.XmlUtils;
import com.google.common.annotations.Beta;
import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.util.PsiTreeUtil;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.intellij.lang.annotations.Language;
import org.jetbrains.uast.UFile;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Test case for lint detectors.
 *
 * <p><b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
@SuppressWarnings("javadoc")
public abstract class LintDetectorTest extends BaseLintDetectorTest {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        BuiltinIssueRegistry.reset();
        LintDriver.clearCrashCount();
        //EcjParser.skipComputingEcjErrors = false;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        List<Issue> issues;
        try {
            // Some detectors extend LintDetectorTest but don't actually
            // provide issues and assert instead; gracefully ignore those
            // here
            issues = getIssues();
        } catch (Throwable t) {
            issues = Collections.emptyList();
        }
        for (Issue issue : issues) {
            EnumSet<Scope> scope = issue.getImplementation().getScope();
            if (scope.contains(Scope.JAVA_FILE)
                    || scope.contains(Scope.ALL_JAVA_FILES)
                    || scope.contains(Scope.RESOURCE_FILE)
                    || scope.contains(Scope.ALL_RESOURCE_FILES)) {
                if (LintDriver.getCrashCount() > 0) {
                    fail("There was a crash during lint execution; consult log for details");
                }
                break;
            }
        }
    }

    protected abstract Detector getDetector();

    private Detector mDetector;

    protected final Detector getDetectorInstance() {
        if (mDetector == null) {
            mDetector = getDetector();
        }

        return mDetector;
    }

    protected boolean allowCompilationErrors() {
        return false;
    }

    protected boolean allowObsoleteCustomRules() {
        return true;
    }

    /**
     * Returns whether the test task should allow the SDK to be missing. Normally false.
     */
    @SuppressWarnings("MethodMayBeStatic")
    protected boolean allowMissingSdk() {
        return false;
    }

    /**
     * Returns whether the test requires that the compileSdkVersion (specified
     * in the project description) must be installed.
     */
    @SuppressWarnings("MethodMayBeStatic")
    protected boolean requireCompileSdk() {
        return false;
    }

    /**
     * If false (the default), lint will run your detectors <b>twice</b>, first on the
     * plain source code, and then a second time where it has inserted whitespace
     * and parentheses pretty much everywhere, to help catch bugs where your detector
     * is only checking direct parents or siblings rather than properly allowing for
     * whitespace and parenthesis nodes which can be present for example when using
     * PSI inside the IDE.
     */
    protected boolean skipExtraTokenChecks() {
        return false;
    }

    protected abstract List<Issue> getIssues();

    public class CustomIssueRegistry extends IssueRegistry {
        @NonNull
        @Override
        public List<Issue> getIssues() {
            return LintDetectorTest.this.getIssues();
        }
    }

    protected String lintFiles(TestFile... relativePaths) throws Exception {
        List<File> files = new ArrayList<>();
        File targetDir = getTargetDir();
        for (TestFile testFile : relativePaths) {
            File file = testFile.createFile(targetDir);
            assertNotNull(file);
            files.add(file);
        }

        return lintFiles(targetDir, files);
    }

    /**
     * @deprecated Use {@link #lintFiles(TestFile...)} instead
     */
    @Deprecated
    protected String lintFiles(String... relativePaths) throws Exception {
        List<File> files = new ArrayList<>();
        File targetDir = getTargetDir();
        for (String relativePath : relativePaths) {
            File file = getTestfile(targetDir, relativePath);
            assertNotNull(file);
            files.add(file);
        }

        return lintFiles(targetDir, files);
    }

    private String lintFiles(File targetDir, List<File> files) throws Exception {
        files.sort((file1, file2) -> {
            ResourceFolderType folder1 = ResourceFolderType.getFolderType(
                    file1.getParentFile().getName());
            ResourceFolderType folder2 = ResourceFolderType.getFolderType(
                    file2.getParentFile().getName());
            if (folder1 != null && folder2 != null && folder1 != folder2) {
                return folder1.compareTo(folder2);
            }
            return file1.compareTo(file2);
        });

        addManifestFileIfNecessary(new File(targetDir, ANDROID_MANIFEST_XML));

        return checkLint(files);
    }

    protected String checkLint(List<File> files) throws Exception {
        TestLintClient lintClient = createClient();
        return checkLint(lintClient, files);
    }

    /**
     * Normally having $ANDROID_BUILD_TOP set when running lint is a bad idea
     * (because it enables some special support in lint for checking code in AOSP
     * itself.) However, some lint tests (particularly custom lint checks) may not care
     * about this.
     */
    protected boolean allowAndroidBuildEnvironment() {
        return true;
    }

    protected String checkLint(TestLintClient lintClient, List<File> files) throws Exception {

        if (!allowAndroidBuildEnvironment() && System.getenv("ANDROID_BUILD_TOP") != null) {
            fail("Don't run the lint tests with $ANDROID_BUILD_TOP set; that enables lint's "
                    + "special support for detecting AOSP projects (looking for .class "
                    + "files in $ANDROID_HOST_OUT etc), and this confuses lint.");
        }

        if (!allowMissingSdk()) {
            com.android.tools.lint.checks.infrastructure.TestLintClient.ensureSdkExists(
                    lintClient);
        }

        mOutput = new StringBuilder();
        String result = lintClient.analyze(files);

        if (getDetector() instanceof Detector.JavaPsiScanner && !skipExtraTokenChecks()) {
            mOutput.setLength(0);
            lintClient.reset();
            try {
                //lintClient.warnings.clear();
                Field field = LintCliClient.class.getDeclaredField("warnings");
                field.setAccessible(true);
                List list = (List)field.get(lintClient);
                list.clear();
            } catch (Throwable t) {
                fail(t.toString());
            }

            String secondResult;
            try {
                //EcjPsiBuilder.setDebugOptions(true, true);
                secondResult = lintClient.analyze(files);
            } finally {
                //EcjPsiBuilder.setDebugOptions(false, false);
            }

            assertEquals("The lint check produced different results when run on the "
                    + "normal test files and a version where parentheses and whitespace tokens "
                    + "have been inserted everywhere. The lint check should be resilient towards "
                    + "these kinds of differences (since in the IDE, PSI will include both "
                    + "types of nodes. Your detector should call LintUtils.skipParenthes(parent) "
                    + "to jump across parentheses nodes when checking parents, and there are "
                    + "similar methods in LintUtils to skip across whitespace siblings.\n",
                    result, secondResult);
        }

        for (File f : files) {
            deleteFile(f);
        }

        LintCoreApplicationEnvironment.disposeApplicationEnvironment();

        return result;
    }

    protected void checkReportedError(
            @NonNull Context context,
            @NonNull Issue issue,
            @NonNull Severity severity,
            @NonNull Location location,
            @NonNull String message) {
    }

    protected void checkReportedError(
            @NonNull Context context,
            @NonNull Issue issue,
            @NonNull Severity severity,
            @NonNull Location location,
            @NonNull String message,
            @Nullable LintFix fixData) {
        checkReportedError(context, issue, severity, location, message);
    }

    protected TestLintClient createClient() {
        return new TestLintClient();
    }

    protected TestConfiguration getConfiguration(LintClient client, Project project) {
        return new TestConfiguration(client, project, null);
    }

    protected void configureDriver(LintDriver driver) {
    }

    /**
     * Run lint on the given files when constructed as a separate project
     *
     * @return The output of the lint check. On Windows, this transforms all directory separators to
     * the unix-style forward slash.
     * @deprecated Use {@link #lintProject(TestFile...)} instead
     */
    @Deprecated
    protected String lintProject(String... relativePaths) throws Exception {
        File projectDir = getProjectDir(null, relativePaths);
        return checkLint(Collections.singletonList(projectDir));
    }

    /**
     * @deprecated Use {@link #lintProjectIncrementally(String, TestFile...)} instead
     */
    @Deprecated
    protected String lintProjectIncrementally(String currentFile, String... relativePaths)
            throws Exception {
        File projectDir = getProjectDir(null, relativePaths);
        File current = new File(projectDir, currentFile.replace('/', File.separatorChar));
        assertTrue(current.exists());
        TestLintClient client = createClient();
        client.setIncremental(current);
        return checkLint(client, Collections.singletonList(projectDir));
    }

    protected String lintProjectIncrementally(String currentFile, TestFile... files)
            throws Exception {
        File projectDir = getProjectDir(null, files);
        File current = new File(projectDir, currentFile.replace('/', File.separatorChar));
        assertTrue(current.exists());
        TestLintClient client = createClient();
        client.setIncremental(current);
        return checkLint(client, Collections.singletonList(projectDir));
    }

    protected static ProjectDescription project(@NonNull TestFile... files) {
        return new ProjectDescription(files);
    }

    @NonNull
    protected TestLintTask lint() {
        TestLintTask task = TestLintTask.lint();
        task.issues(getIssues().toArray(new Issue[0]));
        return task;
    }

    // TODO: Configure whether to show text summary or HTML;
    // make a result object so you can assert which output format to use,
    // which isssues to include

    /**
     * Run lint on the given files when constructed as a separate project
     * @return The output of the lint check. On Windows, this transforms all directory
     *   separators to the unix-style forward slash.
     */
    protected String lintProject(TestFile... files) throws Exception {
        File projectDir = getProjectDir(null, files);
        return checkLint(Collections.singletonList(projectDir));
    }

    @Override
    protected File getTargetDir() {
        File targetDir = new File(getTempDir(), getClass().getSimpleName() + "_" + getName());
        addCleanupDir(targetDir);
        return targetDir;
    }

    @NonNull
    public static TestFile file() {
        return TestFiles.file();
    }

    @NonNull
    public static TestFile source(@NonNull String to, @NonNull String source) {
        return TestFiles.source(to, source);
    }

    @NonNull
    public static TestFile java(@NonNull String to, @NonNull @Language("JAVA") String source) {
        return TestFiles.java(to, source);
    }

    @NonNull
    public static TestFile java(@NonNull @Language("JAVA") String source) {
        return TestFiles.java(source);
    }

    @NonNull
    public static TestFile kotlin(@NonNull String to, @NonNull @Language("kotlin") String source) {
        return TestFiles.kotlin(to, source);
    }

    @NonNull
    public static TestFile kotlin(@NonNull @Language("kotlin") String source) {
        return TestFiles.kotlin(source);
    }

    @NonNull
    public static TestFile xml(@NonNull String to, @NonNull @Language("XML") String source) {
        return TestFiles.xml(to, source);
    }

    @NonNull
    public TestFile copy(@NonNull String from) {
        return TestFiles.copy(from, this);
    }

    @NonNull
    public TestFile copy(@NonNull String from, @NonNull String to) {
        return TestFiles.copy(from, to, this);
    }

    @NonNull
    public static GradleTestFile gradle(@NonNull String to,
            @NonNull @Language("Groovy") String source) {
        return TestFiles.gradle(to, source);
    }

    @NonNull
    public static GradleTestFile gradle(@NonNull @Language("Groovy") String source) {
        return TestFiles.gradle(source);
    }

    @NonNull
    public static ManifestTestFile manifest() {
        return TestFiles.manifest();
    }

    @NonNull
    public static TestFile manifest(@NonNull @Language("XML") String source) {
        return TestFiles.source(ANDROID_MANIFEST_XML, source);
    }

    @NonNull
    public static TestFile manifest(@NonNull String path, @NonNull @Language("XML") String source) {
        return TestFiles.source(path, source);
    }

    @NonNull
    public static com.android.tools.lint.checks.infrastructure.TestFile.PropertyTestFile projectProperties() {
        return TestFiles.projectProperties();
    }

    @NonNull
    public static BinaryTestFile bytecode(@NonNull String to, @NonNull BytecodeProducer producer) {
        return TestFiles.bytecode(to, producer);
    }

    @NonNull
    public static BinaryTestFile bytes(@NonNull String to, @NonNull byte[] bytes) {
        return TestFiles.bytes(to, bytes);
    }

    public static String toBase64(@NonNull byte[] bytes) {
        return TestFiles.toBase64(bytes);
    }

    public static String toBase64gzip(@NonNull byte[] bytes) {
        return TestFiles.toBase64gzip(bytes);
    }

    public static String toBase64(@NonNull File file) throws IOException {
        return TestFiles.toBase64(file);
    }

    public static String toBase64gzip(@NonNull File file) throws IOException {
        return TestFiles.toBase64gzip(file);
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
    public static BinaryTestFile base64(@NonNull String to, @NonNull String encoded) {
        return TestFiles.base64(to, encoded);
    }

    /**
     * Decodes base64 strings into gzip data, then decodes that into a data file.
     * To create this data, use {@link #toBase64gzip(File)} or {@link #toBase64gzip(byte[])},
     * for example via
     * <pre>{@code assertEquals("", toBase64gzip(new File("path/to/your.class")));}</pre>
     */
    @NonNull
    public static BinaryTestFile base64gzip(@NonNull String to, @NonNull String encoded) {
        return TestFiles.base64gzip(to, encoded);
    }

    public static TestFile classpath(String... extraLibraries) {
        return TestFiles.classpath(extraLibraries);
    }

    @NonNull
    public static JarTestFile jar(@NonNull String to) {
        return TestFiles.jar(to);
    }

    @NonNull
    public static JarTestFile jar(@NonNull String to, @NonNull TestFile... files) {
        return TestFiles.jar(to, files);
    }

    public static ImageTestFile image(@NonNull String to, int width, int height) {
        return TestFiles.image(to, width, height);
    }

    protected static boolean imageFormatSupported(@NonNull String format) {
        if ("PNG".equals(format)) {
            // Always supported
            return true;
        }
        try {
            // Can't just look through ImageIO.getWriterFormatNames() -- it lies.
            // (For example, on some systems it will claim to support JPG but then
            // throw an exception when actually used.)
            ImageIO.write(new BufferedImage(0, 0, BufferedImage.TYPE_INT_ARGB),
                    format, new ByteArrayOutputStream());
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Creates a project directory structure from the given files
     *
     * @deprecated Use {@link #getProjectDir(String, TestFile...)} instead
     */
    @Deprecated
    protected File getProjectDir(String name, String ...relativePaths) throws Exception {
        assertFalse("getTargetDir must be overridden to make a unique directory",
                getTargetDir().equals(getTempDir()));

        List<TestFile> testFiles = Lists.newArrayList();
        for (String relativePath : relativePaths) {
            testFiles.add(file().copy(relativePath, this));
        }
        return getProjectDir(name, testFiles.toArray(new TestFile[testFiles.size()]));
    }

    /** Creates a project directory structure from the given files */
    protected File getProjectDir(String name, TestFile... testFiles) throws Exception {
        assertFalse("getTargetDir must be overridden to make a unique directory",
                getTargetDir().equals(getTempDir()));

        File projectDir = getTargetDir();
        if (name != null) {
            projectDir = new File(projectDir, name);
        }
        populateProjectDirectory(projectDir, testFiles);
        return projectDir;
    }

    public static void populateProjectDirectory(@NonNull File projectDir,
            @NonNull TestFile... testFiles) throws IOException {
        if (!projectDir.exists()) {
            assertTrue(projectDir.getPath(), projectDir.mkdirs());
        }

        boolean haveGradle = false;
        for (TestFile fp : testFiles) {
            if (fp instanceof GradleTestFile) {
                haveGradle = true;
            }
        }

        for (TestFile fp : testFiles) {
            if (haveGradle) {
                if (ANDROID_MANIFEST_XML.equals(fp.targetRelativePath)) {
                    // The default should be src/main/AndroidManifest.xml, not just AndroidManifest.xml
                    //fp.to("src/main/AndroidManifest.xml");
                    fp.within("src/main");
                } else if (fp instanceof JavaTestFile
                        && fp.targetRootFolder != null
                        && fp.targetRootFolder.equals("src")) {
                    fp.within("src/main/java");
                } else if (fp instanceof KotlinTestFile
                        && fp.targetRootFolder != null
                        && fp.targetRootFolder.equals("src")) {
                    fp.within("src/main/kotlin");
                }
            }

            File file = fp.createFile(projectDir);
            assertNotNull(file);
        }

        File manifest;
        if (haveGradle) {
            manifest = new File(projectDir, "src/main/AndroidManifest.xml");
        } else {
            manifest = new File(projectDir, ANDROID_MANIFEST_XML);
        }
        addManifestFileIfNecessary(manifest);
    }

    private static void addManifestFileIfNecessary(File manifest) throws IOException {
        // Ensure that there is at least a manifest file there to make it a valid project
        // as far as Lint is concerned:
        if (!manifest.exists()) {
            File parentFile = manifest.getParentFile();
            if (parentFile != null && !parentFile.isDirectory()) {
                boolean ok = parentFile.mkdirs();
                assertTrue("Couldn't create directory " + parentFile, ok);
            }
            try (FileWriter fw = new FileWriter(manifest)) {
                fw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "    package=\"foo.bar2\"\n" +
                        "    android:versionCode=\"1\"\n" +
                        "    android:versionName=\"1.0\" >\n" +
                        "</manifest>\n");
            }
        }
    }

    private StringBuilder mOutput = null;

    // Implements TestResourceProvider

    @Override
    public InputStream getTestResource(String relativePath, boolean expectExists) {
        String path = "data" + File.separator + relativePath;
        InputStream stream = getClass().getResourceAsStream(path);
        if (!expectExists && stream == null) {
            return null;
        }
        return stream;
    }

    protected boolean isEnabled(Issue issue) {
        if (issue == IssueRegistry.LINT_ERROR) {
            return !ignoreSystemErrors();
        } else if (issue == IssueRegistry.PARSER_ERROR) {
            return !allowCompilationErrors();
        } else if (issue == IssueRegistry.OBSOLETE_LINT_CHECK) {
            return !allowObsoleteCustomRules();
        } else {
            return getIssues().contains(issue);
        }
    }

    protected boolean includeParentPath() {
        return false;
    }

    protected EnumSet<Scope> getLintScope(List<File> file) {
        return null;
    }

    public String getSuperClass(Project project, String name) {
        return null;
    }

    /**
     * If true, simulate symbol resolutions when {@link JavaParser#prepareJavaParse(List)}
     * is called
     */
    protected boolean forceErrors() {
        return false;
    }

    protected boolean ignoreSystemErrors() {
        return true;
    }

    public class TestLintClient extends LintCliClient {
        public TestLintClient() {
            super(new LintCliFlags(), "test");
            TextReporter reporter = new TextReporter(this, flags, writer, false);
            reporter.setForwardSlashPaths(true); // stable tests
            flags.getReporters().add(reporter);
        }

        protected final StringWriter writer = new StringWriter();
        protected File incrementalCheck;

        /**
         * Normally having $ANDROID_BUILD_TOP set when running lint is a bad idea
         * (because it enables some special support in lint for checking code in AOSP
         * itself.) However, some lint tests (particularly custom lint checks) may not care
         * about this.
         */
        @SuppressWarnings("MethodMayBeStatic")
        protected boolean allowAndroidBuildEnvironment() {
            return true;
        }

        @NonNull
        @Override
        public String getDisplayPath(File file) {
            return file.getPath().replace(File.separatorChar, '/'); // stable tests
        }

        @Nullable
        @Override
        public File getCacheDir(@Nullable String name, boolean create) {
            File cacheDir = super.getCacheDir(name, create);
            // Separate test caches from user's normal caches
            cacheDir = new File(cacheDir, "unit-tests");
            if (create) {
                //noinspection ResultOfMethodCallIgnored
                cacheDir.mkdirs();
            }
            return cacheDir;
        }

        @Override
        public String getSuperClass(@NonNull Project project, @NonNull String name) {
            String superClass = LintDetectorTest.this.getSuperClass(project, name);
            if (superClass != null) {
                return superClass;
            }

            return super.getSuperClass(project, name);
        }

        @Override
        public void reset() {
            super.reset();
            writer.getBuffer().setLength(0);
        }

        @NonNull
        @Override
        protected Project createProject(@NonNull File dir, @NonNull File referenceDir) {
            if (getProjectDirs().contains(dir)) {
                throw new CircularDependencyException(
                        "Circular library dependencies; check your project.properties files carefully");
            }
            getProjectDirs().add(dir);
            return Project.create(this, dir, referenceDir);
        }

        @Override
        public String getClientRevision() {
            return "unittest"; // Hardcode version to keep unit test output stable
        }

        protected String cleanup(String result) {
            List<File> sorted = new ArrayList<>(sCleanDirs);
            // Process dirs in order such that we match longest substrings first
            sorted.sort((file1, file2) -> {
                String path1 = file1.getPath();
                String path2 = file2.getPath();
                int delta = path2.length() - path1.length();
                if (delta != 0) {
                    return delta;
                } else {
                    return path1.compareTo(path2);
                }
            });

            for (File dir : sorted) {
                if (result.contains(dir.getPath())) {
                    result = result.replace(dir.getPath(), "/TESTROOT");
                }
            }

            return result;
        }

        public String getErrors() {
            return writer.toString();
        }

        @Nullable
        @Override
        public UastParser getUastParser(@Nullable Project project) {
            return new LintCliUastParser(project) {
                @Override
                public boolean prepare(@NonNull List<? extends JavaContext> contexts) {
                    boolean ok = super.prepare(contexts);
                    if (forceErrors()) {
                        ok = false;
                    }
                    return ok;
                }

                @Nullable
                @Override
                public UFile parse(@NonNull JavaContext context) {
                    UFile file = super.parse(context);

                    if (!allowCompilationErrors()) {
                        if (file != null) {
                            PsiErrorElement error = PsiTreeUtil
                                    .findChildOfType(file.getPsi(), PsiErrorElement.class);
                            if (error != null) {
                                fail("Found error element " + error);
                                // TODO: Use ECJ parser to produce build errors with better
                                // error messages, source offsets, etc?
                            }
                        } else {
                            fail("Failure processing source " + context.file);
                        }
                    }

                    return file;
                }
            };
        }

        @Override
        public JavaParser getJavaParser(@Nullable Project project) {
            return new EcjParser(this, project) {
                @Override
                public boolean prepareJavaParse(@NonNull List<JavaContext> contexts) {
                    if (!allowCompilationErrors()) {
                        EcjParser.skipComputingEcjErrors = false;
                    }

                    boolean success = super.prepareJavaParse(contexts);
                    if (forceErrors()) {
                        success = false;
                    }
                    if (!allowCompilationErrors() && ecjResult != null) {
                        StringBuilder sb = new StringBuilder();
                        for (CompilationUnitDeclaration unit : ecjResult.getCompilationUnits()) {
                            // so maybe I don't need my map!!
                            CategorizedProblem[] problems = unit.compilationResult()
                                    .getAllProblems();
                            if (problems != null) {
                                for (IProblem problem : problems) {
                                    if (problem == null || !problem.isError()) {
                                        continue;
                                    }
                                    String filename = new File(new String(
                                            problem.getOriginatingFileName())).getName();
                                    sb.append(filename)
                                            .append(":")
                                            .append(problem.isError() ? "Error" : "Warning")
                                            .append(": ").append(problem.getSourceLineNumber())
                                            .append(": ").append(problem.getMessage())
                                            .append('\n');
                                }
                            }
                        }
                        if (sb.length() > 0) {
                            fail("Found compilation problems in lint test not overriding "
                                    + "allowCompilationErrors():\n" + sb);
                        }

                    }

                    return success;
                }
            };
        }

        @Override
        public void report(
                @NonNull Context context,
                @NonNull Issue issue,
                @NonNull Severity severity,
                @NonNull Location location,
                @NonNull String message,
                @NonNull TextFormat format,
                @Nullable LintFix fix) {
            assertNotNull(location);

            if (ignoreSystemErrors() && issue == IssueRegistry.LINT_ERROR) {
                return;
            }

            checkReportedError(context, issue, severity, location,
                    format.convertTo(message, TextFormat.TEXT), fix);

            if (severity == Severity.FATAL) {
                // Treat fatal errors like errors in the golden files.
                severity = Severity.ERROR;
            }

            // For messages into all secondary locations to ensure they get
            // specifically included in the text report
            if (location.getSecondary() != null) {
                Location l = location.getSecondary();
                if (l == location) {
                    fail("Location link cycle");
                }
                while (l != null) {
                    if (l.getMessage() == null) {
                        l.setMessage("<No location-specific message");
                    }
                    if (l == l.getSecondary()) {
                        fail("Location link cycle");
                    }
                    l = l.getSecondary();
                }
            }

            super.report(context, issue, severity, location, message, format, fix);

            // Make sure errors are unique!
            Warning prev = null;
            for (Warning warning : warnings) {
                assertNotSame(warning, prev);
                assert prev == null || !warning.equals(prev) : warning;
                prev = warning;
            }
        }

        @Override
        public void log(Throwable exception, String format, Object... args) {
            if (exception != null) {
                exception.printStackTrace();
            }
            StringBuilder sb = new StringBuilder();
            if (format != null) {
                sb.append(String.format(format, args));
            }
            if (exception != null) {
                sb.append(exception.toString());
            }
            System.err.println(sb);

            if (exception != null) {
                // Ensure that we get the full cause
                //fail(exception.toString());
                throw new RuntimeException(exception);
            }
        }

        @NonNull
        @Override
        public Configuration getConfiguration(@NonNull Project project,
                @Nullable LintDriver driver) {
            return LintDetectorTest.this.getConfiguration(this, project);
        }

        @Override
        public File findResource(@NonNull String relativePath) {
            if (relativePath.equals(ExternalAnnotationRepository.SDK_ANNOTATIONS_PATH)) {
                try {
                    File rootDir = TestUtils.getWorkspaceRoot();
                    File file = new File(rootDir, "tools/adt/idea/android/annotations");
                    if (!file.exists()) {
                        throw new RuntimeException("File " + file + " not found");
                    }
                    return file;
                } catch (Throwable ignore) {
                    // Lint checks not running inside a tools build -- typically
                    // a third party lint check.
                    return super.findResource(relativePath);
                }
            } else if (relativePath.startsWith("tools/support/")) {
                try {
                    File rootDir = TestUtils.getWorkspaceRoot();
                    String base = relativePath.substring("tools/support/".length());
                    File file = new File(rootDir, "tools/base/files/typos/" + base);
                    if (!file.exists()) {
                        return null;
                    }
                    return file;
                } catch (Throwable ignore) {
                    // Lint checks not running inside a tools build -- typically
                    // a third party lint check.
                    return super.findResource(relativePath);
                }
            } else if (relativePath.equals(ApiLookup.XML_FILE_PATH)) {
                File file = super.findResource(relativePath);
                if (file == null || !file.exists()) {
                    throw new RuntimeException("File "
                            + (file == null ? relativePath : file.getPath()) + " not found");
                }
                return file;
            }
            throw new RuntimeException("Resource " + relativePath + " not found.");
        }

        @NonNull
        @Override
        public List<File> findGlobalRuleJars() {
            // Don't pick up random custom rules in ~/.android/lint when running unit tests
            return Collections.emptyList();
        }

        public void setIncremental(File currentFile) {
            incrementalCheck = currentFile;
        }

        @Override
        public boolean supportsProjectResources() {
            return incrementalCheck != null;
        }

        @Nullable
        protected String getProjectResourceLibraryName() {
            return null;
        }

        @Nullable
        @Override
        public AbstractResourceRepository getResourceRepository(Project project,
                boolean includeDependencies, boolean includeLibraries) {
            if (incrementalCheck == null) {
                return null;
            }

            ResourceRepository repository = new ResourceRepository();
            ILogger logger = new StdLogger(StdLogger.Level.INFO);
            ResourceMerger merger = new ResourceMerger(0);

            ResourceSet resourceSet =
                    new ResourceSet(
                            project.getName(), null, getProjectResourceLibraryName(), true) {
                        @Override
                        protected void checkItems() throws DuplicateDataException {
                            // No checking in ProjectResources; duplicates can happen, but
                            // the project resources shouldn't abort initialization
                        }
                    };
            // Only support 1 resource folder in test setup right now
            int size = project.getResourceFolders().size();
            assertTrue("Found " + size + " test resources folders", size <= 1);
            if (size == 1) {
                resourceSet.addSource(project.getResourceFolders().get(0));
            }
            try {
                resourceSet.loadFromFiles(logger);
                merger.addDataSet(resourceSet);
                repository.getItems().update(merger);

                // Make tests stable: sort the item lists!
                for (ListMultimap<String, ResourceItem> multimap : repository.getItems().values()) {
                    ResourceRepositories.sortItemLists(multimap);
                }

                // Workaround: The repository does not insert ids from layouts! We need
                // to do that here.
                // TODO: namespaces
                Map<ResourceType, ListMultimap<String, ResourceItem>> items =
                        repository.getItems().row(null);
                ListMultimap<String, ResourceItem> layouts = items
                        .get(ResourceType.LAYOUT);
                if (layouts != null) {
                    for (ResourceItem item : layouts.values()) {
                        ResourceFile source = item.getSource();
                        if (source == null) {
                            continue;
                        }
                        File file = source.getFile();
                        try {
                            String xml = Files.toString(file, Charsets.UTF_8);
                            Document document = XmlUtils.parseDocumentSilently(xml, true);
                            assertNotNull(document);
                            Set<String> ids = Sets.newHashSet();
                            addIds(ids, document); // TODO: pull parser
                            if (!ids.isEmpty()) {
                                ListMultimap<String, ResourceItem> idMap =
                                        items.get(ResourceType.ID);
                                if (idMap == null) {
                                    idMap = ArrayListMultimap.create();
                                    items.put(ResourceType.ID, idMap);
                                }
                                for (String id : ids) {
                                    ResourceItem idItem =
                                            new ResourceItem(id, null, ResourceType.ID, null, null);
                                    String qualifiers = file.getParentFile().getName();
                                    if (qualifiers.startsWith("layout-")) {
                                        qualifiers = qualifiers.substring("layout-".length());
                                    } else if (qualifiers.equals("layout")) {
                                        qualifiers = "";
                                    }

                                    // Creating the resource file will set the source of
                                    // idItem.
                                    //noinspection ResultOfObjectAllocationIgnored
                                    ResourceFile.createSingle(file, idItem, qualifiers);
                                    idMap.put(id, idItem);
                                }
                            }
                        } catch (IOException e) {
                            fail(e.toString());
                        }
                    }
                }
            }
            catch (MergingException e) {
                fail(e.getMessage());
            }

            return repository;
        }

        @Nullable
        @Override
        public IAndroidTarget getCompileTarget(@NonNull Project project) {
            IAndroidTarget compileTarget = super.getCompileTarget(project);
            if (compileTarget == null) {
                if (requireCompileSdk() && project.getBuildTargetHash() != null) {
                    fail("Could not find SDK to compile with (" + project.getBuildTargetHash() + "). "
                            + "Either allow the test to use any installed SDK (it defaults to the "
                            + "highest version) via TestLintTask#requireCompileSdk(false), or make "
                            + "sure the SDK being used is the right  one via "
                            + "TestLintTask#sdkHome(File) or $ANDROID_HOME and that the actual SDK "
                            + "platform (platforms/" + project.getBuildTargetHash() + " is installed "
                            + "there");
                }

                IAndroidTarget[] targets = getTargets();
                for (int i = targets.length - 1; i >= 0; i--) {
                    IAndroidTarget target = targets[i];
                    if (target.isPlatform()) {
                        return target;
                    }
                }
            }

            return compileTarget;
        }

        @NonNull
        @Override
        public List<File> getTestSourceFolders(@NonNull Project project) {
            List<File> testSourceFolders = super.getTestSourceFolders(project);

            File tests = new File(project.getDir(), "test");
            if (tests.exists()) {
                List<File> all = Lists.newArrayList(testSourceFolders);
                all.add(tests);
                testSourceFolders = all;
            }

            return testSourceFolders;
        }

        @NonNull
        @Override
        protected LintDriver createDriver(@NonNull IssueRegistry registry,
                @NonNull LintRequest request) {
            LintDriver driver = super.createDriver(registry, request);
            // 3rd party lint unit tests may need this for a while
            driver.setRunCompatChecks(true, true);
            return driver;
        }

        public String analyze(List<File> files) throws Exception {
            LintRequest request = createLintRequest(files);
            request.setScope(getLintScope(files));

            if (incrementalCheck != null) {
                assertEquals(1, files.size());
                File projectDir = files.get(0);
                assertTrue(isProjectDirectory(projectDir));
                Project project = createProject(projectDir, projectDir);
                project.addFile(incrementalCheck);
                List<Project> projects = Collections.singletonList(project);
                request.setProjects(projects);
            }

            driver = createDriver(new LintDetectorTest.CustomIssueRegistry(), request);
            configureDriver(driver);

            driver.analyze();

            // Check compare contract
            Warning prev = null;
            for (Warning warning : warnings) {
                if (prev != null) {
                    boolean equals = warning.equals(prev);
                    assertEquals(equals, prev.equals(warning));
                    int compare = warning.compareTo(prev);
                    assertEquals(equals, compare == 0);
                    assertEquals(-compare, prev.compareTo(warning));
                }
                prev = warning;
            }

            Collections.sort(warnings);

            // Check compare contract and transitivity
            Warning prev2 = prev;
            prev = null;
            for (Warning warning : warnings) {
                if (prev != null && prev2 != null) {
                    assertTrue(warning.compareTo(prev) >= 0);
                    assertTrue(prev.compareTo(prev2) >= 0);
                    assertTrue(warning.compareTo(prev2) >= 0);

                    assertTrue(prev.compareTo(warning) <= 0);
                    assertTrue(prev2.compareTo(prev) <= 0);
                    assertTrue(prev2.compareTo(warning) <= 0);
                }
                prev2 = prev;
                prev = warning;
            }

            Stats stats = new Stats(errorCount, warningCount);
            for (Reporter reporter : flags.getReporters()) {
                reporter.write(stats, warnings);
            }

            mOutput.append(writer.toString());

            if (mOutput.length() == 0) {
                mOutput.append("No warnings.");
            }

            String result = mOutput.toString();
            if (result.equals("No issues found.\n")) {
                result = "No warnings.";
            }

            result = cleanup(result);

            return result;
        }
    }

    private static void addIds(Set<String> ids, Node node) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            String id = element.getAttributeNS(ANDROID_URI, ATTR_ID);
            if (id != null && !id.isEmpty()) {
                ids.add(LintUtils.stripIdPrefix(id));
            }

            NamedNodeMap attributes = element.getAttributes();
            for (int i = 0, n = attributes.getLength(); i < n; i++) {
                Attr attribute = (Attr) attributes.item(i);
                String value = attribute.getValue();
                if (value.startsWith(NEW_ID_PREFIX)) {
                    ids.add(value.substring(NEW_ID_PREFIX.length()));
                }
            }
        }

        NodeList children = node.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node child = children.item(i);
            addIds(ids, child);
        }
    }

    public class TestConfiguration extends DefaultConfiguration {
        protected TestConfiguration(
                @NonNull LintClient client,
                @NonNull Project project,
                @Nullable Configuration parent) {
            super(client, project, parent);
        }

        public TestConfiguration(
                @NonNull LintClient client,
                @Nullable Project project,
                @Nullable Configuration parent,
                @NonNull File configFile) {
            super(client, project, parent, configFile);
        }

        @Override
        @NonNull
        protected Severity getDefaultSeverity(@NonNull Issue issue) {
            // In unit tests, include issues that are ignored by default
            Severity severity = super.getDefaultSeverity(issue);
            if (severity == Severity.IGNORE) {
                if (issue.getDefaultSeverity() != Severity.IGNORE) {
                    return issue.getDefaultSeverity();
                }
                return Severity.WARNING;
            }
            return severity;
        }

        @Override
        public boolean isEnabled(@NonNull Issue issue) {
            return LintDetectorTest.this.isEnabled(issue);
        }

        @Override
        public void ignore(@NonNull Context context, @NonNull Issue issue,
                @Nullable Location location, @NonNull String message) {
            fail("Not supported in tests.");
        }

        @Override
        public void setSeverity(@NonNull Issue issue, @Nullable Severity severity) {
            fail("Not supported in tests.");
        }
    }

    /**
     * Test file description, which can copy from resource directory or from
     * a specified hardcoded string literal, and copy into a target directory
     * <p>
     * This class is just a temporary shim to keep the API compatible; new code should
     * reference com.android.tools.lint.checks.infrastructure.TestFile.
     */
    public static class TestFile extends com.android.tools.lint.checks.infrastructure.TestFile {
        public TestFile() {
        }

        // This source file is indented: dedent the contents before creating the file
        public TestFile indented() {
            contents = kotlin.text.StringsKt.trimIndent(contents);
            return this;
        }

        @Override
        public TestFile withSource(@NonNull String source) {
            super.withSource(source);
            return this;
        }

        @Override
        public TestFile from(@NonNull String from, @NonNull TestResourceProvider provider) {
            super.from(from, provider);
            return this;
        }

        @Override
        public TestFile to(@NonNull String to) {
            super.to(to);
            return this;
        }

        @Override
        public TestFile copy(@NonNull String relativePath, @NonNull TestResourceProvider provider) {
            super.copy(relativePath, provider);
            return this;
        }

        @Override
        public TestFile withBytes(@NonNull byte[] bytes) {
            super.withBytes(bytes);
            return this;
        }

        @Override
        public TestFile within(@Nullable String root) {
            super.within(root);
            return this;
        }
    }

    protected boolean skipKotlinTests() {
        System.out.println("Warning: Skipping Kotlin unit tests for now");
        return true;
    }
}
