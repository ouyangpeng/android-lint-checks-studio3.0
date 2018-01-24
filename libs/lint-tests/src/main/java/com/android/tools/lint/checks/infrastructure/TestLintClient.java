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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.DOT_KT;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.model.AndroidArtifact;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.BuildTypeContainer;
import com.android.builder.model.LintOptions;
import com.android.builder.model.ProductFlavorContainer;
import com.android.builder.model.SourceProvider;
import com.android.builder.model.Variant;
import com.android.ide.common.res2.AbstractResourceRepository;
import com.android.ide.common.res2.DuplicateDataException;
import com.android.ide.common.res2.MergingException;
import com.android.ide.common.res2.ResourceFile;
import com.android.ide.common.res2.ResourceItem;
import com.android.ide.common.res2.ResourceMerger;
import com.android.ide.common.res2.ResourceRepository;
import com.android.ide.common.res2.ResourceSet;
import com.android.resources.ResourceType;
import com.android.sdklib.AndroidTargetHash;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.testutils.TestUtils;
import com.android.tools.lint.EcjParser;
import com.android.tools.lint.ExternalAnnotationRepository;
import com.android.tools.lint.LintCliClient;
import com.android.tools.lint.LintCliFlags;
import com.android.tools.lint.Reporter;
import com.android.tools.lint.TextReporter;
import com.android.tools.lint.Warning;
import com.android.tools.lint.checks.ApiLookup;
import com.android.tools.lint.client.api.CircularDependencyException;
import com.android.tools.lint.client.api.Configuration;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.client.api.LintDriver;
import com.android.tools.lint.client.api.LintListener;
import com.android.tools.lint.client.api.LintRequest;
import com.android.tools.lint.client.api.UastParser;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Position;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.TextFormat;
import com.android.utils.ILogger;
import com.android.utils.Pair;
import com.android.utils.StdLogger;
import com.android.utils.XmlUtils;
import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.util.PsiTreeUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.jetbrains.uast.UFile;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A {@link LintClient} class for use in lint unit tests.
 *
 * <p><b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
public class TestLintClient extends LintCliClient {
    protected final StringWriter writer = new StringWriter();
    protected File incrementalCheck;
    /** Managed by the {@link TestLintTask} */
    @SuppressWarnings("NullableProblems")
    @NonNull
    TestLintTask task;

    public TestLintClient() {
        super(new LintCliFlags(), "test");
        TextReporter reporter = new TextReporter(this, flags, writer, false);
        reporter.setForwardSlashPaths(true); // stable tests
        flags.getReporters().add(reporter);
    }

    protected void setLintTask(@Nullable TestLintTask task) {
        if (task != null && task.optionSetter != null) {
            task.optionSetter.set(flags);
        }

        // Client should not be used outside of the check process
        //noinspection ConstantConditions
        this.task = task;

        //noinspection VariableNotUsedInsideIf
        if (task != null && !task.allowMissingSdk) {
            ensureSdkExists(this);
        }
    }

    static void ensureSdkExists(@NonNull LintClient client) {
        File sdkHome = client.getSdkHome();
        String message;
        if (sdkHome == null) {
            message = "No SDK configured. ";
        } else if (!sdkHome.isDirectory()) {
            message = sdkHome + " is not a directory. ";
        } else {
            return;
        }

        message = "This test requires an Android SDK: " + message + "\n"
                    + "If this test does not really need an SDK, set "
                    + "TestLintTask#allowMissingSdk(). Otherwise, make sure an SDK is "
                    + "available either by specifically pointing to one via "
                    + "TestLintTask#sdkHome(File), or configure $ANDROID_HOME in the "
                    + "environment";
        fail(message);
    }

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

    @Nullable
    private static File findIncrementalProject(@NonNull List<File> files,
            @Nullable String incrementalFileName) {
        // Multiple projects: assume the project names were included in the incremental
        // task names
        if (incrementalFileName == null) {
            if (files.size() == 1) {
                assert false : "Need to specify incremental file name if more than one project";
            } else {
                return files.get(0);
            }
        }
        if (files.size() > 1) {
            for (File dir : files) {
                File root = dir.getParentFile(); // Allow the project name to be part of the name
                File current = new File(root,
                        incrementalFileName.replace('/', File.separatorChar));
                if (current.exists()) {
                    return dir;
                }
            }
        }

        for (File dir : files) {
            File current = new File(dir,
                    incrementalFileName.replace('/', File.separatorChar));
            if (current.exists()) {
                return dir;
            }
        }

        return null;
    }

    protected Pair<String,List<Warning>> checkLint(List<File> files, List<Issue> issues)
            throws Exception {
        if (task.incrementalFileName != null) {
            boolean found = false;

            File dir = findIncrementalProject(files, task.incrementalFileName);
            if (dir != null) {
                File current = new File(dir,
                        task.incrementalFileName.replace('/', File.separatorChar));
                if (!current.exists()) {
                    // Specified the project name as part of the name to disambiguate
                    current = new File(dir.getParentFile(),
                            task.incrementalFileName.replace('/', File.separatorChar));
                }
                if (current.exists()) {
                    setIncremental(current);
                    found = true;
                }
            }
            if (!found) {
                fail("Could not find incremental file " + task.incrementalFileName
                        + " in the project folders " + files);
            }
        }

        if (!allowAndroidBuildEnvironment() && System.getenv("ANDROID_BUILD_TOP") != null) {
            fail("Don't run the lint tests with $ANDROID_BUILD_TOP set; that enables lint's "
                    + "special support for detecting AOSP projects (looking for .class "
                    + "files in $ANDROID_HOST_OUT etc), and this confuses lint.");
        }

        // Reset state here in case a client is reused for multiple runs
        output = new StringBuilder();
        writer.getBuffer().setLength(0);
        warnings.clear();
        errorCount = 0;
        warningCount = 0;

        String result = analyze(files, issues);

        if (runExtraTokenChecks()) {
            output.setLength(0);
            reset();
            try {
                //lintClient.warnings.clear();
                Field field = LintCliClient.class.getDeclaredField("warnings");
                field.setAccessible(true);
                List list = (List)field.get(this);
                list.clear();
            } catch (Throwable t) {
                fail(t.toString());
            }

            String secondResult;
            try {
                //EcjPsiBuilder.setDebugOptions(true, true);
                secondResult = analyze(files, issues);
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
            TestUtils.deleteFile(f);
        }

        return Pair.of(result, warnings);
    }

    private boolean runExtraTokenChecks() {
        if (task.skipExtraTokenChecks) {
            return false;
        }
        for (Issue issue : task.getCheckedIssues()) {
            if (issue.getImplementation().getScope().contains(Scope.JAVA_FILE)) {
                Class<? extends Detector> detectorClass = issue.getImplementation()
                        .getDetectorClass();
                if (Detector.JavaPsiScanner.class.isAssignableFrom(detectorClass)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void reset() {
        super.reset();
        writer.getBuffer().setLength(0);
    }

    @Nullable
    @Override
    public File getSdkHome() {
        if (task.sdkHome != null) {
            return task.sdkHome;
        }

        return super.getSdkHome();
    }

    @NonNull
    @Override
    protected Project createProject(@NonNull File dir, @NonNull File referenceDir) {
        if (getProjectDirs().contains(dir)) {
            throw new CircularDependencyException(
                    "Circular library dependencies; check your project.properties files carefully");
        }
        getProjectDirs().add(dir);

        ProjectDescription description;
        try {
            description = task.dirToProjectDescription.get(dir.getCanonicalFile());
        } catch (IOException ignore) {
            description = task.dirToProjectDescription.get(dir);
        }

        GradleModelMocker mocker;
        try {
            mocker = task.projectMocks.get(dir.getCanonicalFile());
        } catch (IOException ignore) {
            mocker = task.projectMocks.get(dir);
        }
        if (mocker != null && mocker.getProject() != null)  {
            syncLintOptionsToFlags(mocker.getProject().getLintOptions(), flags);
            if (task.variantName != null) {
                mocker.setVariantName(task.variantName);
            }
        }
        if (mocker != null && (mocker.hasJavaPlugin() || mocker.hasJavaLibraryPlugin())) {
            description.type(ProjectDescription.Type.JAVA);
        }

        return new TestProject(this, dir, referenceDir, description, mocker);
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

    @NonNull
    @Override
    public String getDisplayPath(File file) {
        return file.getPath().replace(File.separatorChar, '/'); // stable tests
    }

    @Override
    public String getClientRevision() {
        return "unittest"; // Hardcode version to keep unit test output stable
    }

    @SuppressWarnings("StringBufferField")
    private StringBuilder output = null;

    private void syncLintOptionsToFlags(
            @NonNull LintOptions options,
            @NonNull LintCliFlags flags) {
        flags.getSuppressedIds().addAll(options.getDisable());
        flags.getEnabledIds().addAll(options.getEnable());
        if (options.getCheck() != null && !options.getCheck().isEmpty()) {
            flags.setExactCheckedIds(options.getCheck());
        }
        flags.setSetExitCode(options.isAbortOnError());
        flags.setFullPath(options.isAbsolutePaths());
        flags.setShowSourceLines(!options.isNoLines());
        flags.setQuiet(options.isQuiet());
        flags.setCheckAllWarnings(options.isCheckAllWarnings());
        flags.setIgnoreWarnings(options.isIgnoreWarnings());
        flags.setWarningsAsErrors(options.isWarningsAsErrors());
        flags.setCheckTestSources(options.isCheckTestSources());
        flags.setCheckGeneratedSources(options.isCheckGeneratedSources());
        flags.setShowEverything(options.isShowAll());
        flags.setDefaultConfiguration(options.getLintConfig());
        flags.setExplainIssues(options.isExplainIssues());
        flags.setBaselineFile(options.getBaselineFile());
        flags.setFatalOnly(task.vital);

        Map<String, Integer> severityOverrides = options.getSeverityOverrides();
        if (severityOverrides != null) {
            Map<String, Severity> overrides = Maps.newHashMap();
            for (Map.Entry<String, Integer> entry : severityOverrides.entrySet()) {
                String id = entry.getKey();
                Severity severity;
                switch (entry.getValue()) {
                    case LintOptions.SEVERITY_FATAL: severity = Severity.FATAL; break;
                    case LintOptions.SEVERITY_ERROR: severity = Severity.ERROR; break;
                    case LintOptions.SEVERITY_WARNING: severity = Severity.WARNING; break;
                    case LintOptions.SEVERITY_INFORMATIONAL: severity = Severity.INFORMATIONAL; break;
                    case LintOptions.SEVERITY_IGNORE: severity = Severity.IGNORE; break;
                    default: continue;
                }
                overrides.put(id, severity);
            }
            flags.setSeverityOverrides(overrides);
        }
    }

    public String analyze(List<File> files, List<Issue> issues) throws Exception {
        // We'll sync lint options to flags later when the project is created, but try
        // to do it early before the driver is initialized
        if (!files.isEmpty()) {
            GradleModelMocker mocker = task.projectMocks.get(files.get(0));
            if (mocker != null) {
                syncLintOptionsToFlags(mocker.getProject().getLintOptions(), flags);
            }
        }

        LintRequest request = createLintRequest(files);
        if (task.customScope != null) {
            request = request.setScope(task.customScope);
        }

        if (incrementalCheck != null) {
            File projectDir = findIncrementalProject(files, task.incrementalFileName);
            assert projectDir != null;
            assertTrue(isProjectDirectory(projectDir));
            Project project = createProject(projectDir, projectDir);
            project.addFile(incrementalCheck);
            List<Project> projects = Collections.singletonList(project);
            request.setProjects(projects);
        }

        driver = createDriver(new TestIssueRegistry(issues), request);

        if (task.driverConfigurator != null) {
            task.driverConfigurator.configure(driver);
        }

        if (task.listener != null) {
            driver.addLintListener(task.listener);
        }

        if (task.mockModifier != null) {
            driver.addLintListener((driver, type, project, context) -> {
                if (type == LintListener.EventType.REGISTERED_PROJECT && project != null) {
                    AndroidProject model = project.getGradleProjectModel();
                    Variant variant = project.getCurrentVariant();
                    if (model != null && variant != null) {
                        task.mockModifier.modify(model, variant);
                    }
                }
            });
        }

        validateIssueIds();

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

        Reporter.Stats stats = new Reporter.Stats(errorCount, warningCount);
        for (Reporter reporter : flags.getReporters()) {
            reporter.write(stats, warnings);
        }

        output.append(writer.toString());

        if (output.length() == 0) {
            output.append("No warnings.");
        }

        String result = output.toString();
        if (result.equals("No issues found.\n")) {
            result = "No warnings.";
        }

        result = cleanup(result);

        if (task.listener != null) {
            driver.removeLintListener(task.listener);
        }

        return result;
    }

    @NonNull
    @Override
    protected LintDriver createDriver(@NonNull IssueRegistry registry,
            @NonNull LintRequest request) {
        LintDriver driver = super.createDriver(registry, request);
        // 3rd party lint unit tests may need this for a while
        driver.setRunCompatChecks(task.runCompatChecks, task.runCompatChecks);
        driver.setFatalOnlyMode(task.vital);
        return driver;
    }

    protected void addCleanupDir(@NonNull File dir) {
        cleanupDirs.add(dir);
        try {
            cleanupDirs.add(dir.getCanonicalFile());
        } catch (IOException e) {
            fail(e.getLocalizedMessage());
        }
        cleanupDirs.add(dir.getAbsoluteFile());
    }

    protected final Set<File> cleanupDirs = Sets.newHashSet();

    protected String cleanup(String result) {
        List<File> sorted = new ArrayList<>(cleanupDirs);
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
            String path = dir.getPath();
            if (result.contains(path)) {
                result = result.replace(path, "/TESTROOT");
            }
            path = path.replace(File.separatorChar, '/');
            if (result.contains(path)) {
                result = result.replace(path, "/TESTROOT");
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
                if (task.forceSymbolResolutionErrors) {
                    ok = false;
                }
                return ok;
            }

            @Nullable
            @Override
            public UFile parse(@NonNull JavaContext context) {
                if (context.file.getPath().endsWith(DOT_KT)) {
                    // We don't yet have command line invocation of Kotlin working;
                    // for now do simple (VERY simple) mocking
                    context.report(IssueRegistry.LINT_ERROR, Location.create(context.file),
                            "Kotlin not supported in the test file infrastructure yet; "
                                    + "for now test manually in the IDE");
                    return mock(UFile.class);
                }

                UFile file = super.parse(context);

                if (!task.allowCompilationErrors) {
                    if (file != null) {
                        PsiErrorElement error = PsiTreeUtil
                                .findChildOfType(file.getPsi(), PsiErrorElement.class);
                        if (error != null) {
                            fail("Found error element " + error);
                            // TODO: Use ECJ parser to produce build errors with better
                            // error messages, source offsets, etc?
                        }
                    } else {
                        fail("Failure processing source " + context.file +
                                ": No UAST AST created");
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
                boolean success = super.prepareJavaParse(contexts);
                if (task.forceSymbolResolutionErrors) {
                    success = false;
                }
                if (!task.allowCompilationErrors && ecjResult != null) {
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

        if (issue == IssueRegistry.LINT_ERROR) {
            if (!task.allowSystemErrors) {
                return;
            }

            // We don't care about this error message from lint tests; we don't compile
            // test project files
            if (message.startsWith("No `.class` files were found in project")) {
                return;
            }
        }

        if (task.messageChecker != null) {
            task.messageChecker.checkReportedError(context, issue, severity,
                    location, format.convertTo(message, TextFormat.TEXT), fix);
        }

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
            assert prev == null || !warning.equals(prev) : "Warning (message, location) reported more than once: " + warning;
            prev = warning;
        }

        if (fix instanceof LintFix.ReplaceString) {
            LintFix.ReplaceString replaceFix = (LintFix.ReplaceString) fix;
            String oldPattern = replaceFix.oldPattern;
            String oldString = replaceFix.oldString;
            Location rangeLocation = replaceFix.range != null ? replaceFix.range : location;
            String contents = readFile(rangeLocation.getFile()).toString();
            Position start = rangeLocation.getStart();
            Position end = rangeLocation.getEnd();
            assert start != null;
            assert end != null;
            String locationRange = contents.substring(start.getOffset(), end.getOffset());

            if (oldString != null) {
                int startIndex = contents.indexOf(oldString, start.getOffset());
                if (startIndex == -1 || startIndex > end.getOffset()) {
                    fail("Did not find \"" + oldString + "\" in \"" + locationRange
                            + "\" as suggested in the quickfix for issue " + issue);
                }
            } else if (oldPattern != null) {
                Pattern pattern = Pattern.compile(oldPattern);
                if (!pattern.matcher(locationRange).find()) {
                    fail("Did not match pattern \"" + oldPattern + "\" in \"" + locationRange
                                    + "\" as suggested in the quickfix for issue " + issue);
                }
            }
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
        return new TestConfiguration(task, this, project, null);
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
        if (task.supportResourceRepository != null) {
            return task.supportResourceRepository;
        }
        return incrementalCheck != null;
    }

    @SuppressWarnings("MethodMayBeStatic")
    @Nullable
    protected String getProjectResourceLibraryName() {
        return null;
    }

    @Nullable
    @Override
    public AbstractResourceRepository getResourceRepository(Project project,
            boolean includeDependencies, boolean includeLibraries) {
        if (!supportsProjectResources()) {
            return null;
        }

        ResourceRepository repository = new ResourceRepository();
        ILogger logger = new StdLogger(StdLogger.Level.INFO);
        ResourceMerger merger = new ResourceMerger(0);

        ResourceSet resourceSet =
                new ResourceSet(project.getName(), null, getProjectResourceLibraryName(), true) {
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
            ListMultimap<String, ResourceItem> layouts = items.get(ResourceType.LAYOUT);
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
                                    items.computeIfAbsent(ResourceType.ID,
                                            k -> ArrayListMultimap.create());
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

    @Nullable
    @Override
    public IAndroidTarget getCompileTarget(@NonNull Project project) {
        IAndroidTarget compileTarget = super.getCompileTarget(project);
        if (compileTarget == null) {
            if (task.requireCompileSdk && project.getBuildTargetHash() != null) {
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

    @Nullable
    @Override
    public URLConnection openConnection(@NonNull URL url, int timeout) throws IOException {
        if (task.mockNetworkData != null) {
            String query = url.toExternalForm();
            byte[] bytes = task.mockNetworkData.get(query);
            if (bytes != null) {
                return new URLConnection(url) {
                    @Override
                    public void connect() throws IOException {
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        return new ByteArrayInputStream(bytes);
                    }
                };
            }
        }

        if (!task.allowNetworkAccess) {
            fail("Lint detector test attempted to read from the network. Normally this means "
                    + "that you have forgotten to set up mock data (calling networkData() on the "
                    + "lint task) or the URL no longer matches. The URL encountered was " +
                    url);
        }

        return super.openConnection(url, timeout);
    }

    public static class TestProject extends Project {
        @Nullable
        public final GradleModelMocker mocker;
        private final ProjectDescription projectDescription;

        public TestProject(@NonNull LintClient client, @NonNull File dir,
                @NonNull File referenceDir, @Nullable ProjectDescription projectDescription,
                @Nullable GradleModelMocker mocker) {
            super(client, dir, referenceDir);
            this.projectDescription = projectDescription;
            this.mocker = mocker;
            // In the old days merging was opt in, but we're almost exclusively using/supporting
            // Gradle projects now, so make this the default behavior for test projects too, even
            // if they don't explicitly opt into Gradle features like mocking during the test.
            // E.g. a simple project like
            //     ManifestDetectorTest#testUniquePermissionsPrunedViaManifestRemove
            // which simply registers library and app manifests (no build files) should exhibit
            // manifest merging.
            this.mergeManifests = true;
        }

        @Override
        public boolean isGradleProject() {
            return mocker != null || super.isGradleProject();
        }

        @Nullable
        @Override
        public Variant getCurrentVariant() {
            return mocker != null ? mocker.getVariant() : null;
        }

        @Override
        public boolean isLibrary() {
            if (mocker != null && mocker.isLibrary()) {
                return true;
            }

            return super.isLibrary()  || projectDescription != null
                    && projectDescription.getType() == ProjectDescription.Type.LIBRARY;
        }

        @Override
        public boolean isAndroidProject() {
            if (mocker != null && (mocker.hasJavaPlugin() || mocker.hasJavaLibraryPlugin())) {
                return false;
            }

            return projectDescription == null ||
                    projectDescription.getType() != ProjectDescription.Type.JAVA;
        }

        @Override
        public int getBuildSdk() {
            if (mocker != null) {
                String compileTarget = mocker.getProject().getCompileTarget();
                //noinspection ConstantConditions
                if (compileTarget != null && !compileTarget.isEmpty()) {
                    AndroidVersion version = AndroidTargetHash.getPlatformVersion(compileTarget);
                    if (version != null) {
                        return version.getApiLevel();
                    }
                }
            }

            return super.getBuildSdk();
        }

        @Nullable
        @Override
        public String getBuildTargetHash() {
            if (mocker != null) {
                String compileTarget = mocker.getProject().getCompileTarget();
                //noinspection ConstantConditions
                if (compileTarget != null && !compileTarget.isEmpty()) {
                    return compileTarget;
                }
            }

            return super.getBuildTargetHash();
        }

        @Override
        public boolean getReportIssues() {
            if (projectDescription != null && !projectDescription.getReport()) {
                return false;
            }
            return super.getReportIssues();
        }

        @Nullable
        @Override
        public AndroidProject getGradleProjectModel() {
            return mocker != null ? mocker.getProject() : null;
        }

        private List<SourceProvider> mProviders;

        private List<SourceProvider> getSourceProviders() {
            if (mProviders == null) {
                AndroidProject project = getGradleProjectModel();
                Variant variant = getCurrentVariant();
                if (project == null || variant == null) {
                    return Collections.emptyList();
                }

                List<SourceProvider> providers = Lists.newArrayList();
                AndroidArtifact mainArtifact = variant.getMainArtifact();

                providers.add(project.getDefaultConfig().getSourceProvider());

                for (String flavorName : variant.getProductFlavors()) {
                    for (ProductFlavorContainer flavor : project.getProductFlavors()) {
                        if (flavorName.equals(flavor.getProductFlavor().getName())) {
                            providers.add(flavor.getSourceProvider());
                            break;
                        }
                    }
                }

                SourceProvider multiProvider = mainArtifact.getMultiFlavorSourceProvider();
                if (multiProvider != null) {
                    providers.add(multiProvider);
                }

                String buildTypeName = variant.getBuildType();
                for (BuildTypeContainer buildType : project.getBuildTypes()) {
                    if (buildTypeName.equals(buildType.getBuildType().getName())) {
                        providers.add(buildType.getSourceProvider());
                        break;
                    }
                }

                SourceProvider variantProvider = mainArtifact.getVariantSourceProvider();
                if (variantProvider != null) {
                    providers.add(variantProvider);
                }

                mProviders = providers;
            }

            return mProviders;
        }

        @NonNull
        @Override
        public List<File> getManifestFiles() {
            if (manifestFiles == null) {
                //noinspection VariableNotUsedInsideIf
                if (mocker != null) {
                    for (SourceProvider provider : getSourceProviders()) {
                        File manifestFile = provider.getManifestFile();
                        if (manifestFile.exists()) { // model returns path whether or not it exists
                            if (manifestFiles == null) {
                                manifestFiles = Lists.newArrayList();
                            }
                            manifestFiles.add(manifestFile);
                        }
                    }
                }

                if (manifestFiles == null) {
                    manifestFiles = super.getManifestFiles();
                }
            }

            return manifestFiles;
        }

        @NonNull
        @Override
        public List<File> getResourceFolders() {
            if (resourceFolders == null) {
                //noinspection VariableNotUsedInsideIf
                if (mocker != null) {
                    for (SourceProvider provider : getSourceProviders()) {
                        Collection<File> list = provider.getResDirectories();
                        for (File file : list) {
                            if (file.exists()) { // model returns path whether or not it exists
                                if (resourceFolders == null) {
                                    resourceFolders = Lists.newArrayList();
                                }
                                resourceFolders.add(file);
                            }
                        }
                    }
                }

                if (resourceFolders == null) {
                    resourceFolders = super.getResourceFolders();
                }

            }
            return resourceFolders;
        }

        @NonNull
        @Override
        public List<File> getJavaSourceFolders() {
            if (javaSourceFolders == null) {
                //noinspection VariableNotUsedInsideIf
                if (mocker != null) {
                    List<File> list = Lists.newArrayList();
                    for (SourceProvider provider : getSourceProviders()) {
                        Collection<File> srcDirs = provider.getJavaDirectories();
                        // model returns path whether or not it exists
                        for (File srcDir : srcDirs) {
                            if (!isGenerated(srcDir) && srcDir.exists()) {
                                list.add(srcDir);
                            }
                        }
                    }
                    javaSourceFolders = list;
                }
                if (javaSourceFolders == null || javaSourceFolders.isEmpty()) {
                    javaSourceFolders = super.getJavaSourceFolders();
                }
            }

            return javaSourceFolders;
        }

        private static boolean isGenerated(@NonNull File srcDir) {
            return srcDir.getName().equals("generated") ||
                    srcDir.getName().equals("gen");
        }

        @NonNull
        @Override
        public List<File> getGeneratedSourceFolders() {
            // In the tests the only way to mark something as generated is "gen" or "generated"
            if (generatedSourceFolders == null) {
                //noinspection VariableNotUsedInsideIf
                if (mocker != null) {
                    List<File> list = Lists.newArrayList();
                    for (SourceProvider provider : getSourceProviders()) {
                        Collection<File> srcDirs = provider.getJavaDirectories();
                        // model returns path whether or not it exists
                        for (File srcDir : srcDirs) {
                            if (isGenerated(srcDir) && srcDir.exists()) {
                                list.add(srcDir);
                            }
                        }
                    }
                    generatedSourceFolders = list;
                }
                if (generatedSourceFolders == null || generatedSourceFolders.isEmpty()) {
                    generatedSourceFolders = super.getGeneratedSourceFolders();
                }
            }

            return generatedSourceFolders;
        }

        @NonNull
        @Override
        public List<File> getTestSourceFolders() {
            if (testSourceFolders == null) {
                //noinspection VariableNotUsedInsideIf
                if (mocker != null) {
                    testSourceFolders = Lists.newArrayList();
                    for (SourceProvider provider : LintUtils
                            .getTestSourceProviders(mocker.getProject(), mocker.getVariant())) {
                        Collection<File> srcDirs = provider.getJavaDirectories();
                        // model returns path whether or not it exists
                        List<File> list = new ArrayList<>();
                        for (File srcDir : srcDirs) {
                            if (srcDir.exists()) {
                                list.add(srcDir);
                            }
                        }
                        testSourceFolders.addAll(list);
                    }
                }
                if (testSourceFolders == null || testSourceFolders.isEmpty()) {
                    testSourceFolders = super.getTestSourceFolders();
                }
            }

            return testSourceFolders;
        }
    }
}
