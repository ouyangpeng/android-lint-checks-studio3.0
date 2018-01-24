/*
 * Copyright (C) 2013 The Android Open Source Project
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

import static com.android.manifmerger.MergingReport.MergedManifestKind.MERGED;
import static com.android.tools.lint.LintCliFlags.ERRNO_CREATED_BASELINE;
import static com.android.tools.lint.LintCliFlags.ERRNO_ERRORS;
import static com.android.tools.lint.LintCliFlags.ERRNO_SUCCESS;
import static com.android.utils.CharSequences.indexOf;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.ApiVersion;
import com.android.builder.model.JavaCompileOptions;
import com.android.builder.model.ProductFlavor;
import com.android.builder.model.SourceProvider;
import com.android.manifmerger.ManifestMerger2;
import com.android.manifmerger.ManifestMerger2.Invoker.Feature;
import com.android.manifmerger.ManifestMerger2.MergeType;
import com.android.manifmerger.MergingReport;
import com.android.manifmerger.XmlDocument;
import com.android.sdklib.IAndroidTarget;
import com.android.tools.lint.Reporter.Stats;
import com.android.tools.lint.checks.HardcodedValuesDetector;
import com.android.tools.lint.client.api.Configuration;
import com.android.tools.lint.client.api.DefaultConfiguration;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.client.api.LintBaseline;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.client.api.LintDriver;
import com.android.tools.lint.client.api.LintListener;
import com.android.tools.lint.client.api.LintRequest;
import com.android.tools.lint.client.api.UastParser;
import com.android.tools.lint.client.api.XmlParser;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Position;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.TextFormat;
import com.android.tools.lint.helpers.DefaultUastParser;
import com.android.utils.CharSequences;
import com.android.utils.StdLogger;
import com.google.common.annotations.Beta;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.util.Disposer;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.util.lang.UrlClassLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Lint client for command line usage. Supports the flags in {@link LintCliFlags},
 * and offers text, HTML and XML reporting, etc.
 * <p>
 * Minimal example:
 * <pre>
 * // files is a list of java.io.Files, typically a directory containing
 * // lint projects or direct references to project root directories
 * IssueRegistry registry = new BuiltinIssueRegistry();
 * LintCliFlags flags = new LintCliFlags();
 * LintCliClient client = new LintCliClient(flags);
 * int exitCode = client.run(registry, files);
 * </pre>
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public class LintCliClient extends LintClient {
    protected final List<Warning> warnings = new ArrayList<>();
    protected boolean hasErrors;
    protected int errorCount;
    protected int warningCount;
    protected IssueRegistry registry;
    protected LintDriver driver;
    protected final LintCliFlags flags;
    private Configuration configuration;
    private boolean validatedIds;

    /** Creates a CLI driver */
    public LintCliClient() {
        super(CLIENT_CLI);
        flags = new LintCliFlags();
        TextReporter reporter = new TextReporter(this, flags, new PrintWriter(System.out, true),
                false);
        flags.getReporters().add(reporter);
    }

    /** Creates a CLI driver */
    public LintCliClient(@NonNull LintCliFlags flags, @NonNull String clientName) {
        super(clientName);
        this.flags = flags;
    }

    /**
     * Runs the static analysis command line driver. You need to add at least one error reporter
     * to the command line flags.
     */
    public int run(@NonNull IssueRegistry registry, @NonNull List<File> files) throws IOException {
        assert !flags.getReporters().isEmpty();
        this.registry = registry;

        LintRequest lintRequest = createLintRequest(files);
        driver = createDriver(registry, lintRequest);

        addProgressPrinter();
        validateIssueIds();

        driver.analyze();

        Collections.sort(warnings);

        int baselineErrorCount = 0;
        int baselineWarningCount = 0;
        int fixedCount = 0;

        LintBaseline baseline = driver.getBaseline();
        if (baseline != null) {
            baselineErrorCount = baseline.getFoundErrorCount();
            baselineWarningCount = baseline.getFoundWarningCount();
            fixedCount = baseline.getFixedCount();
        }

        Stats stats = new Stats(errorCount, warningCount,
                baselineErrorCount, baselineWarningCount, fixedCount);

        boolean hasConsoleOutput = false;
        for (Reporter reporter : flags.getReporters()) {
            reporter.write(stats, warnings);
            if (reporter instanceof TextReporter && ((TextReporter)reporter).isWriteToConsole()) {
                hasConsoleOutput = true;
            }
        }

        File baselineFile = flags.getBaselineFile();
        if (!flags.isQuiet() && !hasConsoleOutput) {
            if (baselineErrorCount > 0 || baselineWarningCount > 0) {
                if (errorCount == 0 && warningCount == 1) {
                    // the warning is the warning about baseline issues having been filtered
                    // out, don't list this as "1 warning"
                    System.out.print("Lint found no new issues");
                } else {
                    System.out.print(String.format("Lint found %1$s",
                            LintUtils.describeCounts(errorCount, Math.max(0, warningCount - 1),
                                    true, false)));
                }
                assert baselineFile != null;
                System.out.print(String.format(" (%1$s filtered by baseline %2$s)",
                        LintUtils.describeCounts(stats.baselineErrorCount,
                                stats.baselineWarningCount, true, true),
                        baselineFile.getName()));
            } else {
                System.out.print(String.format("Lint found %1$s",
                        LintUtils.describeCounts(errorCount, warningCount, true, false)));
            }
            System.out.println();
        }

        if (baselineFile != null && !baselineFile.exists() && flags.isWriteBaselineIfMissing()) {
            File dir = baselineFile.getParentFile();
            boolean ok = true;
            if (dir != null && !dir.isDirectory()) {
                ok = dir.mkdirs();
            }
            if (!ok) {
                System.err.println("Couldn't create baseline folder " + dir);
            } else {
                Reporter reporter = Reporter.createXmlReporter(this, baselineFile, true);
                reporter.write(stats, warnings);
                String message = ""
                        + "Created baseline file " + baselineFile + "\n"
                        + "\n"
                        + "Also breaking the build in case this was not intentional. If you\n"
                        + "deliberately created the baseline file, re-run the build and this\n"
                        + "time it should succeed without warnings.\n"
                        + "\n"
                        + "If not, investigate the baseline path in the lintOptions config\n"
                        + "or verify that the baseline file has been checked into version\n"
                        + "control.\n";

                if (LintClient.Companion.isGradle()) {
                    message += ""
                            + "\n"
                            + "You can set the system property lint.baselines.continue=true\n"
                            + "if you want to create many missing baselines in one go.";

                }

                System.err.println(message);
                return ERRNO_CREATED_BASELINE;
            }
        }

        return flags.isSetExitCode() ? (hasErrors ? ERRNO_ERRORS : ERRNO_SUCCESS) : ERRNO_SUCCESS;
    }

    protected void validateIssueIds() {
        driver.addLintListener((driver, type, project, context) -> {
            if (type == LintListener.EventType.SCANNING_PROJECT && !validatedIds) {
                // Make sure all the id's are valid once the driver is all set up and
                // ready to run (such that custom rules are available in the registry etc)
                validateIssueIds(project);
            }
        });
    }

    @NonNull
    protected LintDriver createDriver(@NonNull IssueRegistry registry,
            @NonNull LintRequest request) {
        driver = new LintDriver(registry, this, request);
        driver.setAbbreviating(!flags.isShowEverything());
        driver.setCheckTestSources(flags.isCheckTestSources());
        driver.setCheckGeneratedSources(flags.isCheckGeneratedSources());
        driver.setFatalOnlyMode(flags.isFatalOnly());
        driver.setCheckDependencies(flags.isCheckDependencies());

        File baselineFile = flags.getBaselineFile();
        if (baselineFile != null) {
            LintBaseline baseline = new LintBaseline(this, baselineFile);
            driver.setBaseline(baseline);
            if (flags.isRemoveFixedBaselineIssues()) {
                baseline.setWriteOnClose(true);
                baseline.setRemoveFixed(true);
            }
        }

        return driver;
    }

    protected void addProgressPrinter() {
        if (!flags.isQuiet()) {
            driver.addLintListener(new ProgressPrinter());
        }
    }

    /** Creates a lint request */
    @NonNull
    protected LintRequest createLintRequest(@NonNull List<File> files) {
        return new LintRequest(this, files);
    }

    @Override
    public void log(
            @NonNull Severity severity,
            @Nullable Throwable exception,
            @Nullable String format,
            @Nullable Object... args) {
        System.out.flush();
        if (!flags.isQuiet()) {
            // Place the error message on a line of its own since we're printing '.' etc
            // with newlines during analysis
            System.err.println();
        }
        if (format != null) {
            System.err.println(String.format(format, args));
        }
        if (exception != null) {
            exception.printStackTrace();
        }
    }

    @NonNull
    @Override
    public XmlParser getXmlParser() {
        return new LintCliXmlParser(this);
    }

    @NonNull
    @Override
    public Configuration getConfiguration(@NonNull Project project, @Nullable LintDriver driver) {
        return new CliConfiguration(getConfiguration(), project, flags.isFatalOnly());
    }

    /** File content cache */
    private final Map<File, CharSequence> mFileContents = new HashMap<>(100);

    /** Read the contents of the given file, possibly cached */
    private CharSequence getContents(File file) {
        return mFileContents.computeIfAbsent(file, k -> readFile(file));
    }

    @Override
    public JavaParser getJavaParser(@Nullable Project project) {
        return new EcjParser(this, project);
    }

    @Nullable
    @Override
    public UastParser getUastParser(@Nullable Project project) {
        return new LintCliUastParser(project);
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
        assert context.isEnabled(issue) || issue.getCategory() == Category.LINT;

        if (severity.isError()) {
            hasErrors = true;
            errorCount++;
        } else {
            warningCount++;
        }

        // Store the message in the raw format internally such that we can
        // convert it to text for the text reporter, HTML for the HTML reporter
        // and so on.
        message = format.convertTo(message, TextFormat.RAW);
        Warning warning = new Warning(issue, message, severity, context.getProject());
        warnings.add(warning);

        //noinspection ConstantConditions
        if (location == null) {
            // Misbehaving third party lint rules
            log(Severity.ERROR, null, "No location provided for issue " + issue);
            return;
        }

        warning.location = location;
        File file = location.getFile();
        warning.file = file;
        warning.path = getDisplayPath(context.getProject(), file);
        warning.quickfixData = fix;

        Position startPosition = location.getStart();
        if (startPosition != null) {
            int line = startPosition.getLine();
            warning.line = line;
            warning.offset = startPosition.getOffset();
            Position endPosition = location.getEnd();
            if (endPosition != null) {
                warning.endOffset = endPosition.getOffset();
            }
            if (line >= 0) {
                if (context.file == location.getFile()) {
                    warning.fileContents = context.getContents();
                }
                if (warning.fileContents == null) {
                    warning.fileContents = getContents(location.getFile());
                }

                if (flags.isShowSourceLines()) {
                    // Compute error line contents
                    warning.errorLine = getLine(warning.fileContents, line);
                    if (warning.errorLine != null) {
                        // Replace tabs with spaces such that the column
                        // marker (^) lines up properly:
                        warning.errorLine = warning.errorLine.replace('\t', ' ');
                        int column = startPosition.getColumn();
                        if (column < 0) {
                            column = 0;
                            for (int i = 0; i < warning.errorLine.length(); i++, column++) {
                                if (!Character.isWhitespace(warning.errorLine.charAt(i))) {
                                    break;
                                }
                            }
                        }
                        StringBuilder sb = new StringBuilder(100);
                        sb.append(warning.errorLine);
                        sb.append('\n');
                        for (int i = 0; i < column; i++) {
                            sb.append(' ');
                        }

                        boolean displayCaret = true;
                        if (endPosition != null) {
                            int endLine = endPosition.getLine();
                            int endColumn = endPosition.getColumn();
                            if (endLine == line && endColumn > column) {
                                for (int i = column; i < endColumn; i++) {
                                    sb.append('~');
                                }
                                displayCaret = false;
                            }
                        }

                        if (displayCaret) {
                            sb.append('^');
                        }
                        sb.append('\n');
                        warning.errorLine = sb.toString();
                    }
                }
            }
        }
    }

    /** Look up the contents of the given line */
    static String getLine(CharSequence contents, int line) {
        int index = getLineOffset(contents, line);
        if (index != -1) {
            return getLineOfOffset(contents, index);
        } else {
            return null;
        }
    }

    static String getLineOfOffset(CharSequence contents, int offset) {
        int end = indexOf(contents, '\n', offset);
        if (end == -1) {
            end = indexOf(contents, '\r', offset);
        }
        return contents.subSequence(offset, end != -1 ? end : contents.length()).toString();
    }


    /** Look up the contents of the given line */
    static int getLineOffset(CharSequence contents, int line) {
        int index = 0;
        for (int i = 0; i < line; i++) {
            index = indexOf(contents, '\n', index);
            if (index == -1) {
                return -1;
            }
            index++;
        }

        return index;
    }

    @NonNull
    @Override
    public CharSequence readFile(@NonNull File file) {
        try {
            return LintUtils.getEncodedString(this, file, false);
        } catch (IOException e) {
            return "";
        }
    }

    boolean isCheckingSpecificIssues() {
        return flags.getExactCheckedIds() != null;
    }

    private Map<Project, ClassPathInfo> mProjectInfo;

    @Override
    @NonNull
    protected ClassPathInfo getClassPath(@NonNull Project project) {
        ClassPathInfo classPath = super.getClassPath(project);

        List<File> sources = flags.getSourcesOverride();
        List<File> classes = flags.getClassesOverride();
        List<File> libraries = flags.getLibrariesOverride();
        if (classes == null && sources == null && libraries == null) {
            return classPath;
        }

        ClassPathInfo info;
        if (mProjectInfo == null) {
            mProjectInfo = Maps.newHashMap();
            info = null;
        } else {
            info = mProjectInfo.get(project);
        }

        if (info == null) {
            if (sources == null) {
                sources = classPath.getSourceFolders();
            }
            if (classes == null) {
                classes = classPath.getClassFolders();
            }
            if (libraries == null) {
                libraries = classPath.getLibraries(true);
            }

            info = new ClassPathInfo(sources, classes, libraries, classPath.getLibraries(false),
                    classPath.getTestSourceFolders(), classPath.getTestLibraries(),
                    classPath.getGeneratedFolders());
            mProjectInfo.put(project, info);
        }

        return info;
    }

    @NonNull
    @Override
    public List<File> getResourceFolders(@NonNull Project project) {
        List<File> resources = flags.getResourcesOverride();
        if (resources == null) {
            return super.getResourceFolders(project);
        }

        return resources;
    }

    /**
     * Consult the lint.xml file, but override with the --enable and --disable
     * flags supplied on the command line
     */
    protected class CliConfiguration extends DefaultConfiguration {
        private final boolean mFatalOnly;

        protected CliConfiguration(@NonNull Configuration parent, @NonNull Project project,
                boolean fatalOnly) {
            super(LintCliClient.this, project, parent);
            mFatalOnly = fatalOnly;
        }

        protected CliConfiguration(File lintFile, boolean fatalOnly) {
            super(LintCliClient.this, null, null, lintFile);
            mFatalOnly = fatalOnly;
        }

        protected CliConfiguration(
                @NonNull File lintFile,
                @Nullable Configuration parent,
                @Nullable Project project,
                boolean fatalOnly) {
            super(LintCliClient.this, project, parent, lintFile);
            mFatalOnly = fatalOnly;
        }

        @NonNull
        @Override
        public Severity getSeverity(@NonNull Issue issue) {
            Severity severity = computeSeverity(issue);

            if (mFatalOnly && severity != Severity.FATAL) {
                return Severity.IGNORE;
            }

            if (flags.isWarningsAsErrors() && severity == Severity.WARNING) {
                if (issue == IssueRegistry.BASELINE) {
                    // Don't promote the baseline informational issue
                    // (number of issues promoted) to error
                    return severity;
                }
                severity = Severity.ERROR;
            }

            if (flags.isIgnoreWarnings() && severity == Severity.WARNING) {
                severity = Severity.IGNORE;
            }

            return severity;
        }

        @NonNull
        @Override
        protected Severity getDefaultSeverity(@NonNull Issue issue) {
            if (flags.isCheckAllWarnings()) {
                return issue.getDefaultSeverity();
            }

            return super.getDefaultSeverity(issue);
        }

        private Severity computeSeverity(@NonNull Issue issue) {
            Severity severity = super.getSeverity(issue);

            String id = issue.getId();
            Set<String> suppress = flags.getSuppressedIds();
            if (suppress.contains(id)) {
                return Severity.IGNORE;
            }

            Severity manual = flags.getSeverityOverrides().get(id);
            if (manual != null) {
                if (this.severity != null && (this.severity.containsKey(id)
                        || this.severity.containsKey(VALUE_ALL))) {
                    // Ambiguity! We have a specific severity override provided
                    // via lint options for the main app module, but a local lint.xml
                    // file in the library (not a lintOptions definition) which also
                    // specifies severity for the same issue.
                    //
                    // Who should win? Should the intent from the main app module
                    // win, such that you have a global way to say "this is the severity
                    // I want during this lint run?". Or should the library-local definition
                    // win, to say "there's a local problem in this library; I need to
                    // change things here?".
                    //
                    // Both are plausible, so for now I'm going with a middle ground: local
                    // definitions should be used to turn of issues that don't work right.
                    // Therefore, we'll take the minimum of the two severities!
                    return Severity.min(severity, manual);
                }
                return manual;
            }

            Set<String> enabled = flags.getEnabledIds();
            Set<String> check = flags.getExactCheckedIds();
            if (enabled.contains(id) || (check != null && check.contains(id))) {
                // Overriding default
                // Detectors shouldn't be returning ignore as a default severity,
                // but in case they do, force it up to warning here to ensure that
                // it's run
                if (severity == Severity.IGNORE) {
                    severity = issue.getDefaultSeverity();
                    if (severity == Severity.IGNORE) {
                        severity = Severity.WARNING;
                    }
                }

                return severity;
            }

            if (check != null && issue.getCategory() != Category.LINT) {
                return Severity.IGNORE;
            }

            return severity;
        }
    }

    @NonNull
    @Override
    protected Project createProject(@NonNull File dir, @NonNull File referenceDir) {
        Project project = super.createProject(dir, referenceDir);

        String compileSdkVersion = flags.getCompileSdkVersionOverride();
        if (compileSdkVersion != null) {
            project.setBuildTargetHash(compileSdkVersion);
        }

        return project;
    }

    /**
     * Checks that any id's specified by id refer to valid, known, issues. This
     * typically can't be done right away (in for example the Gradle code which
     * handles DSL references to strings, or in the command line parser for the
     * lint command) because the full set of valid id's is not known until lint
     * actually starts running and for example gathers custom rules from all
     * AAR dependencies reachable from libraries, etc.
     */
    private void validateIssueIds(@Nullable Project project) {
        if (driver != null) {
            IssueRegistry registry = driver.getRegistry();
            if (!registry.isIssueId(HardcodedValuesDetector.ISSUE.getId())) {
                // This should not be necessary, but there have been some strange
                // reports where lint has reported some well known builtin issues
                // to not exist:
                //
                //   Error: Unknown issue id "DuplicateDefinition" [LintError]
                //   Error: Unknown issue id "GradleIdeError" [LintError]
                //   Error: Unknown issue id "InvalidPackage" [LintError]
                //   Error: Unknown issue id "JavascriptInterface" [LintError]
                //   ...
                //
                // It's not clear how this can happen, though it's probably related
                // to using 3rd party lint rules (where lint will create new composite
                // issue registries to wrap the various additional issues) - but
                // we definitely don't want to validate issue id's if we can't find
                // well known issues.
                return;
            }
            validatedIds = true;
            validateIssueIds(project, registry, flags.getExactCheckedIds());
            validateIssueIds(project, registry, flags.getEnabledIds());
            validateIssueIds(project, registry, flags.getSuppressedIds());
            validateIssueIds(project, registry, flags.getSeverityOverrides().keySet());
        }
    }

    private void validateIssueIds(@Nullable Project project, @NonNull IssueRegistry registry,
            @Nullable Collection<String> ids) {
        if (ids != null) {
            for (String id : ids) {
                if (registry.getIssue(id) == null) {
                    reportNonExistingIssueId(project, id);
                }
            }
        }
    }

    protected void reportNonExistingIssueId(@Nullable Project project, @NonNull String id) {
        String message = String.format("Unknown issue id \"%1$s\"", id);

        if (driver != null && project != null) {
            Location location = LintUtils.guessGradleLocation(this, project.getDir(), id);
            if (!isSuppressed(IssueRegistry.LINT_ERROR)) {
                report(new Context(driver, project, project, project.getDir(), ""),
                        IssueRegistry.LINT_ERROR,
                        project.getConfiguration(driver).getSeverity(IssueRegistry.LINT_ERROR),
                        location, message, TextFormat.RAW, LintFix.create().data(id));
            }
        } else {
            log(Severity.ERROR, null, "Lint: %1$s", message);
        }
    }

    private static class ProgressPrinter implements LintListener {
        @Override
        public void update(
                @NonNull LintDriver lint,
                @NonNull EventType type,
                @Nullable Project project,
                @Nullable Context context) {
            switch (type) {
                case SCANNING_PROJECT: {
                    String name = context != null ? context.getProject().getName() : "?";
                    if (lint.getPhase() > 1) {
                        System.out.print(String.format(
                                "\nScanning %1$s (Phase %2$d): ",
                                name,
                                lint.getPhase()));
                    } else {
                        System.out.print(String.format(
                                "\nScanning %1$s: ",
                                name));
                    }
                    break;
                }
                case SCANNING_LIBRARY_PROJECT: {
                    String name = context != null ? context.getProject().getName() : "?";
                    System.out.print(String.format(
                            "\n         - %1$s: ",
                            name));
                    break;
                }
                case SCANNING_FILE:
                    System.out.print('.');
                    break;
                case NEW_PHASE:
                    // Ignored for now: printing status as part of next project's status
                    break;
                case CANCELED:
                case COMPLETED:
                    System.out.println();
                    break;
                case REGISTERED_PROJECT:
                case STARTING:
                    // Ignored for now
                    break;
            }
        }
    }

    /**
     * Given a file, it produces a cleaned up path from the file.
     * This will clean up the path such that
     * <ul>
     *   <li>  {@code foo/./bar} becomes {@code foo/bar}
     *   <li>  {@code foo/bar/../baz} becomes {@code foo/baz}
     * </ul>
     *
     * Unlike {@link java.io.File#getCanonicalPath()} however, it will <b>not</b> attempt
     * to make the file canonical, such as expanding symlinks and network mounts.
     *
     * @param file the file to compute a clean path for
     * @return the cleaned up path
     */
    @VisibleForTesting
    @NonNull
    static String getCleanPath(@NonNull File file) {
        String path = file.getPath();
        StringBuilder sb = new StringBuilder(path.length());

        if (path.startsWith(File.separator)) {
            sb.append(File.separator);
        }
        elementLoop:
        for (String element : Splitter.on(File.separatorChar).omitEmptyStrings().split(path)) {
            if (element.equals(".")) {
                continue;
            } else if (element.equals("..")) {
                if (sb.length() > 0) {
                    for (int i = sb.length() - 1; i >= 0; i--) {
                        char c = sb.charAt(i);
                        if (c == File.separatorChar) {
                            sb.setLength(i == 0 ? 1 : i);
                            continue elementLoop;
                        }
                    }
                    sb.setLength(0);
                    continue;
                }
            }

            if (sb.length() > 1) {
                sb.append(File.separatorChar);
            } else if (sb.length() > 0 && sb.charAt(0) != File.separatorChar) {
                sb.append(File.separatorChar);
            }
            sb.append(element);
        }
        if (path.endsWith(File.separator) && sb.length() > 0
                && sb.charAt(sb.length() - 1) != File.separatorChar) {
            sb.append(File.separator);
        }

        return sb.toString();
    }

    @NonNull
    String getDisplayPath(@NonNull Project project, @NonNull File file) {
        return getDisplayPath(project, file, flags.isFullPath());
    }

    static String getDisplayPath(@NonNull Project project, @NonNull File file, boolean fullPath) {
        String path = file.getPath();
        if (!fullPath && path.startsWith(project.getReferenceDir().getPath())) {
            int chop = project.getReferenceDir().getPath().length();
            if (path.length() > chop && path.charAt(chop) == File.separatorChar) {
                chop++;
            }
            path = path.substring(chop);
            if (path.isEmpty()) {
                path = file.getName();
            }
        } else if (fullPath) {
            path = getCleanPath(file.getAbsoluteFile());
        } else if (file.isAbsolute() && file.exists()) {
            path = Reporter.getRelativePath(project.getReferenceDir(), file);
        }

        return path;
    }

    /** Returns whether all warnings are enabled, including those disabled by default */
    boolean isAllEnabled() {
        return flags.isCheckAllWarnings();
    }

    /** Returns the issue registry used by this client */
    IssueRegistry getRegistry() {
        return registry;
    }

    /** Returns the driver running the lint checks */
    LintDriver getDriver() {
        return driver;
    }

    private static Set<File> sAlreadyWarned;

    /** Returns the configuration used by this client */
    protected Configuration getConfiguration() {
        if (configuration == null) {
            File configFile = flags.getDefaultConfiguration();
            if (configFile != null) {
                if (!configFile.exists()) {
                    if (sAlreadyWarned == null || !sAlreadyWarned.contains(configFile)) {
                        log(Severity.ERROR, null,
                                "Warning: Configuration file %1$s does not exist", configFile);
                    }
                    if (sAlreadyWarned == null) {
                        sAlreadyWarned = Sets.newHashSet();
                    }
                    sAlreadyWarned.add(configFile);
                }
                configuration = createConfigurationFromFile(configFile);
            }
        }

        return configuration;
    }

    /** Returns true if the given issue has been explicitly disabled */
    boolean isSuppressed(Issue issue) {
        return flags.getSuppressedIds().contains(issue.getId());
    }

    public Configuration createConfigurationFromFile(File file) {
        return new CliConfiguration(file, flags.isFatalOnly());
    }


    @Nullable private com.intellij.openapi.project.Project ideaProject;
    @Nullable private Disposable projectDisposer;

    @Nullable
    public com.intellij.openapi.project.Project getIdeaProject() {
        return ideaProject;
    }

    @Override
    public void initializeProjects(@NonNull Collection<? extends Project> knownProjects) {
        // Initialize the associated idea project to use

        LintCoreApplicationEnvironment appEnv = LintCoreApplicationEnvironment.get();
        Disposable parentDisposable = Disposer.newDisposable();
        projectDisposer = parentDisposable;

        LintCoreProjectEnvironment projectEnvironment = LintCoreProjectEnvironment.create(
                parentDisposable, appEnv);
        ideaProject = projectEnvironment.getProject();

        // knownProject only lists root projects, not dependencies
        Set<Project> allProjects = Sets.newIdentityHashSet();
        for (Project project : knownProjects) {
            allProjects.add(project);
            allProjects.addAll(project.getAllLibraries());
        }

        List<File> files = Lists.newArrayListWithCapacity(50);

        for (Project project : allProjects) {
            // Note that there could be duplicates here since we're including multiple library
            // dependencies that could have the same dependencies (e.g. lib1 and lib2 both
            // referencing guava.jar)
            files.addAll(project.getJavaSourceFolders());
            files.addAll(project.getGeneratedSourceFolders());
            files.addAll(project.getJavaLibraries(true));
            files.addAll(project.getTestLibraries());

            // Don't include the class folders:
            //  files.addAll(project.getJavaClassFolders());
            // These are the outputs from the sources and generated sources, which we will
            // parse directly with PSI/UAST anyway. Including them here leads lint to do
            // a lot more work (e.g. when resolving symbols it looks at both .java and .class
            // matches)
        }

        IAndroidTarget buildTarget = null;
        for (Project project : knownProjects) {
            IAndroidTarget t = project.getBuildTarget();
            if (t != null) {
                if (buildTarget == null) {
                    buildTarget = t;
                } else if (buildTarget.getVersion().compareTo(t.getVersion()) > 0) {
                    buildTarget = t;
                }
            }
        }

        if (buildTarget != null) {
            File file = buildTarget.getFile(IAndroidTarget.ANDROID_JAR);
            if (file != null) {
                files.add(file);
            }
        }

        projectEnvironment.registerPaths(files);

        LanguageLevel maxLevel = LanguageLevel.JDK_1_7;
        for (Project project : knownProjects) {
            AndroidProject model = project.getGradleProjectModel();
            if (model != null) {
                JavaCompileOptions javaCompileOptions = model.getJavaCompileOptions();
                String sourceCompatibility = javaCompileOptions.getSourceCompatibility();
                LanguageLevel level = LanguageLevel.parse(sourceCompatibility);
                if (level != null && maxLevel.isLessThan(level)) {
                    maxLevel = level;
                }
            }
        }

        com.intellij.openapi.project.Project ideaProject = getIdeaProject();
        if (ideaProject != null) {
            LanguageLevelProjectExtension languageLevelProjectExtension = ideaProject
                    .getComponent(LanguageLevelProjectExtension.class);
            if (languageLevelProjectExtension != null) {
                languageLevelProjectExtension.setLanguageLevel(maxLevel);
            }
        }

        super.initializeProjects(knownProjects);
    }

    @Override
    public void disposeProjects(@NonNull Collection<? extends Project> knownProjects) {
        if (projectDisposer != null) {
            Disposer.dispose(projectDisposer);
            LintCoreApplicationEnvironment.clearAccessorCache();
        }
        ideaProject = null;
        projectDisposer = null;

        super.disposeProjects(knownProjects);
    }

    @Override
    @Nullable
    public String getClientRevision() {
        try {
            File file = findResource("tools" + File.separator +
                    "source.properties");
            if (file != null && file.exists()) {
                try (FileInputStream input = new FileInputStream(file)) {
                    Properties properties = new Properties();
                    properties.load(input);

                    String revision = properties.getProperty("Pkg.Revision");
                    if (revision != null && !revision.isEmpty()) {
                        return revision;
                    }
                }
            }
        } catch (Throwable ignore) {
            // dev builds, tests, etc: fall through to unknown
        }

        return "unknown";
    }

    @NonNull
    public LintCliFlags getFlags() {
        return flags;
    }

    public boolean haveErrors() {
        return errorCount > 0;
    }

    @VisibleForTesting
    public void reset() {
        warnings.clear();
        errorCount = 0;
        warningCount = 0;

        getProjectDirs().clear();
        getDirToProject().clear();
    }

    @NonNull
    @Override
    public ClassLoader createUrlClassLoader(@NonNull URL[] urls, @NonNull ClassLoader parent) {
        return UrlClassLoader.build().parent(parent).urls(urls).get();
    }

    @Nullable
    @Override
    public Document getMergedManifest(@NonNull Project project) {
        List<File> manifests = Lists.newArrayList();
        for (Project dependency : project.getAllLibraries()) {
            manifests.addAll(dependency.getManifestFiles());
        }

        File injectedFile = new File("injected-from-gradle");
        StringBuilder injectedXml = new StringBuilder();
        if (project.getGradleProjectModel() != null && project.getCurrentVariant() != null) {
            ProductFlavor mergedFlavor = project.getCurrentVariant().getMergedFlavor();
            ApiVersion targetSdkVersion = mergedFlavor.getTargetSdkVersion();
            ApiVersion minSdkVersion = mergedFlavor.getMinSdkVersion();
            if (targetSdkVersion != null || minSdkVersion != null) {
                injectedXml.append(""
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "    <uses-sdk");
                if (minSdkVersion != null) {
                    injectedXml.append(" android:minSdkVersion=\"")
                            .append(minSdkVersion.getApiString()).append("\"");
                }
                if (targetSdkVersion != null) {
                    injectedXml.append(" android:targetSdkVersion=\"")
                            .append(targetSdkVersion.getApiString()).append("\"");
                }
                injectedXml.append(" />\n"
                        + "</manifest>\n");
                manifests.add(injectedFile);
            }
        }

        File mainManifest = null;

        if (project.getGradleProjectModel() != null && project.getCurrentVariant() != null) {
            for (SourceProvider provider : LintUtils.getSourceProviders(
                    project.getGradleProjectModel(), project.getCurrentVariant())) {
                File manifestFile = provider.getManifestFile();
                if (manifestFile.exists()) { // model returns path whether or not it exists
                    if (mainManifest == null) {
                        mainManifest = manifestFile;
                    } else {
                        manifests.add(manifestFile);
                    }
                }
            }
            if (mainManifest == null) {
                return null;
            }
        } else {
            List<File> projectManifests;
            projectManifests = project.getManifestFiles();
            if (projectManifests.isEmpty()) {
                return null;
            }
            mainManifest = projectManifests.get(0);
            for (int i = 1; i < projectManifests.size(); i++) {
                manifests.add(projectManifests.get(i));
            }
        }

        if (manifests.isEmpty()) {
            // Only the main manifest: that's easy
            try {
                Document document = getXmlParser().parseXml(mainManifest);
                if (document != null) {
                    resolveMergeManifestSources(document, mainManifest);
                }
                return document;
            } catch (IOException | SAXException | ParserConfigurationException e) {
                log(Severity.WARNING, e, "Could not parse %1$s", mainManifest);
            }

            return null;
        }

        try {
            StdLogger logger = new StdLogger(StdLogger.Level.INFO);
            MergeType type = project.isLibrary() ? MergeType.LIBRARY : MergeType.APPLICATION;
            MergingReport mergeReport = ManifestMerger2
                    .newMerger(mainManifest, logger, type)
                    .withFeatures(
                            // TODO: How do we get the *opposite* of EXTRACT_FQCNS:
                            // ensure that all names are made fully qualified?
                            Feature.SKIP_BLAME,
                            Feature.SKIP_XML_STRING,
                            Feature.NO_PLACEHOLDER_REPLACEMENT)
                    .addLibraryManifests(manifests.toArray(new File[manifests.size()]))
                    .withFileStreamProvider(new ManifestMerger2.FileStreamProvider() {
                        @Override
                        protected InputStream getInputStream(@NonNull File file) throws
                                FileNotFoundException {
                            if (injectedFile.equals(file)) {
                                return CharSequences.getInputStream(injectedXml.toString());
                            }
                            CharSequence text = readFile(file);
                            // TODO: Avoid having to convert back and forth
                            return CharSequences.getInputStream(text);
                        }
                    })
                    .merge();

            XmlDocument xmlDocument = mergeReport.getMergedXmlDocument(MERGED);
            if (xmlDocument != null) {
                Document document = xmlDocument.getXml();
                if (document != null) {
                    resolveMergeManifestSources(document, mergeReport.getActions());
                    return document;
                }
            }
        }
        catch (ManifestMerger2.MergeFailureException e) {
            log(Severity.ERROR, e, "Couldn't parse merged manifest");
        }

        return super.getMergedManifest(project);
    }

    protected class LintCliUastParser extends DefaultUastParser {

        private final Project project;

        public LintCliUastParser(Project project) {
            //noinspection ConstantConditions
            super(project, LintCliClient.this.ideaProject);
            this.project = project;
        }

        @Override
        public boolean prepare(@NonNull final List<? extends JavaContext> contexts) {
            // If we're using Kotlin, ensure we initialize the bridge
            for (JavaContext context : contexts) {
                if (context.file.getPath().endsWith(SdkConstants.DOT_KT)) {
                    LintCoreApplicationEnvironment.registerKotlinUastPlugin();
                    break;
                }
            }

            boolean ok = super.prepare(contexts);

            if (project == null || contexts.isEmpty()) {
                return ok;
            }

            // Now that we have a project context, ensure that the annotations manager
            // is up to date
            if (ideaProject != null) {
                LintExternalAnnotationsManager annotationsManager =
                    (LintExternalAnnotationsManager) ExternalAnnotationsManager.getInstance(
                                ideaProject);
                annotationsManager.updateAnnotationRoots(LintCliClient.this);
            }

            return ok;
        }
    }
}
