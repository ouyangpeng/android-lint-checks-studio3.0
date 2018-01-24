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

import static com.android.SdkConstants.CURRENT_PLATFORM;
import static com.android.SdkConstants.DOT_9PNG;
import static com.android.SdkConstants.DOT_PNG;
import static com.android.SdkConstants.PLATFORM_LINUX;
import static com.android.SdkConstants.UTF_8;
import static com.android.tools.lint.detector.api.LintUtils.endsWith;
import static java.io.File.separatorChar;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.checks.AccessibilityDetector;
import com.android.tools.lint.checks.AlwaysShowActionDetector;
import com.android.tools.lint.checks.AndroidAutoDetector;
import com.android.tools.lint.checks.AndroidTvDetector;
import com.android.tools.lint.checks.AnnotationDetector;
import com.android.tools.lint.checks.ApiDetector;
import com.android.tools.lint.checks.AppCompatCallDetector;
import com.android.tools.lint.checks.AppIndexingApiDetector;
import com.android.tools.lint.checks.ByteOrderMarkDetector;
import com.android.tools.lint.checks.CleanupDetector;
import com.android.tools.lint.checks.CommentDetector;
import com.android.tools.lint.checks.DetectMissingPrefix;
import com.android.tools.lint.checks.DuplicateResourceDetector;
import com.android.tools.lint.checks.GradleDetector;
import com.android.tools.lint.checks.GridLayoutDetector;
import com.android.tools.lint.checks.IconDetector;
import com.android.tools.lint.checks.IncludeDetector;
import com.android.tools.lint.checks.InefficientWeightDetector;
import com.android.tools.lint.checks.JavaPerformanceDetector;
import com.android.tools.lint.checks.ManifestDetector;
import com.android.tools.lint.checks.MissingClassDetector;
import com.android.tools.lint.checks.MissingIdDetector;
import com.android.tools.lint.checks.NamespaceDetector;
import com.android.tools.lint.checks.ObsoleteLayoutParamsDetector;
import com.android.tools.lint.checks.ParcelDetector;
import com.android.tools.lint.checks.PropertyFileDetector;
import com.android.tools.lint.checks.PxUsageDetector;
import com.android.tools.lint.checks.ReadParcelableDetector;
import com.android.tools.lint.checks.RtlDetector;
import com.android.tools.lint.checks.ScrollViewChildDetector;
import com.android.tools.lint.checks.SecurityDetector;
import com.android.tools.lint.checks.SignatureOrSystemDetector;
import com.android.tools.lint.checks.SupportAnnotationDetector;
import com.android.tools.lint.checks.TextFieldDetector;
import com.android.tools.lint.checks.TextViewDetector;
import com.android.tools.lint.checks.TitleDetector;
import com.android.tools.lint.checks.TypoDetector;
import com.android.tools.lint.checks.TypographyDetector;
import com.android.tools.lint.checks.UnusedResourceDetector;
import com.android.tools.lint.checks.UselessViewDetector;
import com.android.tools.lint.checks.Utf8Detector;
import com.android.tools.lint.checks.WrongCallDetector;
import com.android.tools.lint.checks.WrongCaseDetector;
import com.android.tools.lint.detector.api.Issue;
import com.android.utils.SdkUtils;
import com.google.common.annotations.Beta;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** A reporter is an output generator for lint warnings
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public abstract class Reporter {

    public static final String NEW_FORMAT_PROPERTY = "lint.old-html-style";
    public static final boolean USE_MATERIAL_HTML_STYLE = !Boolean.getBoolean(NEW_FORMAT_PROPERTY);

    protected final LintCliClient client;
    protected final File output;
    protected String title = "Lint Report";
    protected boolean simpleFormat;
    protected boolean bundleResources;
    protected Map<String, String> urlMap;
    protected File resources;
    protected final Map<File, String> resourceUrl = new HashMap<>();
    protected final Map<String, File> nameToFile = new HashMap<>();
    protected boolean displayEmpty = true;

    /**
     * Creates a new HTML {@link Reporter}
     *
     * @param client       the associated client
     * @param output       the output file
     * @param flags        the command line flags
     * @param simpleFormat if true, use simple HTML format
     * @throws IOException if an error occurs
     */
    @NonNull
    public static Reporter createHtmlReporter(
            @NonNull LintCliClient client,
            @NonNull File output,
            @NonNull LintCliFlags flags,
            boolean simpleFormat) throws IOException {
        if (USE_MATERIAL_HTML_STYLE) {
            return new MaterialHtmlReporter(client, output, flags);
        }
        HtmlReporter reporter = new HtmlReporter(client, output, flags);
        if (simpleFormat) {
            reporter.setSimpleFormat(true);
        }
        return reporter;
    }

    /**
     * Constructs a new text {@link Reporter}
     *
     * @param client the client
     * @param flags the flags
     * @param file the file corresponding to the writer, if any
     * @param writer the writer to write into
     * @param close whether the writer should be closed when done
     */
    @NonNull
    public static Reporter createTextReporter(
            @NonNull LintCliClient client,
            @NonNull LintCliFlags flags,
            @Nullable File file,
            @NonNull Writer writer,
            boolean close)  {
        return new TextReporter(client, flags, file, writer, close);
    }

    /**
     * Constructs a new {@link XmlReporter}
     *
     * @param client              the client
     * @param output              the output file
     * @param intendedForBaseline whether this XML report is used to write a baseline file
     * @throws IOException if an error occurs
     */
    public static  Reporter createXmlReporter(
            @NonNull LintCliClient client,
            @NonNull File output,
            boolean intendedForBaseline) throws IOException {
        XmlReporter reporter = new XmlReporter(client, output);
        reporter.setIntendedForBaseline(intendedForBaseline);
        return reporter;
    }

    /**
     * Write the given warnings into the report
     * @param stats  the vital statistics for the lint report
     * @param issues the issues to be reported  @throws IOException if an error occurs
     */
    public abstract void write(@NonNull Stats stats, List<Warning> issues) throws IOException;

    /**
     * Writes a project overview table
     * @param stats  the vital statistics for the lint report
     * @param projects the projects to write
     */
    public void writeProjectList(@NonNull Stats stats,
            @NonNull List<MultiProjectHtmlReporter.ProjectEntry> projects) throws IOException {
        throw new UnsupportedOperationException();
    }

    protected Reporter(@NonNull LintCliClient client, @NonNull File output) {
        this.client = client;
        this.output = output;
    }

    /**
     * Sets the report title
     *
     * @param title the title of the report
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /** @return the title of the report */
    public String getTitle() {
        return title;
    }

    /**
     * Sets whether the report should bundle up resources along with the HTML report.
     * This implies a non-simple format (see {@link #setSimpleFormat(boolean)}).
     *
     * @param bundleResources if true, copy images into a directory relative to
     *            the report
     */
    public void setBundleResources(boolean bundleResources) {
        this.bundleResources = bundleResources;
        simpleFormat = false;
    }

    /**
     * Sets whether the report should use simple formatting (meaning no JavaScript,
     * embedded images, etc).
     *
     * @param simpleFormat whether the formatting should be simple
     */
    public void setSimpleFormat(boolean simpleFormat) {
        this.simpleFormat = simpleFormat;
    }

    /**
     * Returns whether the report should use simple formatting (meaning no JavaScript,
     * embedded images, etc).
     *
     * @return whether the report should use simple formatting
     */
    public boolean isSimpleFormat() {
        return simpleFormat;
    }


    String getUrl(File file) {
        if (bundleResources && !simpleFormat) {
            String url = getRelativeResourceUrl(file);
            if (url != null) {
                return url;
            }
        }

        if (urlMap != null) {
            String path = file.getAbsolutePath();
            // Perform the comparison using URLs such that we properly escape spaces etc.
            String pathUrl = encodeUrl(path);
            for (Map.Entry<String, String> entry : urlMap.entrySet()) {
                String prefix = entry.getKey();
                String prefixUrl = encodeUrl(prefix);
                if (pathUrl.startsWith(prefixUrl)) {
                    String relative = pathUrl.substring(prefixUrl.length());
                    return entry.getValue() + relative;
                }
            }
        }

        if (file.isAbsolute()) {
            String relativePath = getRelativePath(output.getParentFile(), file);
            if (relativePath != null) {
                relativePath = relativePath.replace(separatorChar, '/');
                return encodeUrl(relativePath);
            }
        }

        try {
            return SdkUtils.fileToUrlString(file);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /** Encodes the given String as a safe URL substring, escaping spaces etc */
    static String encodeUrl(String url) {
        try {
            url = url.replace('\\', '/');
            return URLEncoder.encode(url, UTF_8).replace("%2F", "/");
        } catch (UnsupportedEncodingException e) {
            // This shouldn't happen for UTF-8
            System.err.println("Invalid string " + e.getLocalizedMessage());
            return url;
        }
    }

    /** Set mapping of path prefixes to corresponding URLs in the HTML report */
    public void setUrlMap(@Nullable Map<String, String> urlMap) {
        this.urlMap = urlMap;
    }

    /** Gets a pointer to the local resource directory, if any */
    File getResourceDir() {
        if (resources == null && bundleResources) {
            resources = computeResourceDir();
            if (resources == null) {
                bundleResources = false;
            }
        }

        return resources;
    }

    /** Finds/creates the local resource directory, if possible */
    File computeResourceDir() {
        String fileName = output.getName();
        int dot = fileName.indexOf('.');
        if (dot != -1) {
            fileName = fileName.substring(0, dot);
        }

        File resources = new File(output.getParentFile(), fileName + "_files");
        if (!resources.exists() && !resources.mkdir()) {
            resources = null;
        }

        return resources;
    }

    /** Returns a URL to a local copy of the given file, or null */
    protected String getRelativeResourceUrl(File file) {
        String resource = resourceUrl.get(file);
        if (resource != null) {
            return resource;
        }

        String name = file.getName();
        if (!endsWith(name, DOT_PNG) || endsWith(name, DOT_9PNG)) {
            return null;
        }

        // Attempt to make local copy
        File resourceDir = getResourceDir();
        if (resourceDir != null) {
            String base = file.getName();

            File path = nameToFile.get(base);
            if (path != null && !path.equals(file)) {
                // That filename already exists and is associated with a different path:
                // make a new unique version
                for (int i = 0; i < 100; i++) {
                    base = '_' + base;
                    path = nameToFile.get(base);
                    if (path == null || path.equals(file)) {
                        break;
                    }
                }
            }

            File target = new File(resourceDir, base);
            try {
                Files.copy(file, target);
            } catch (IOException e) {
                return null;
            }
            return resourceDir.getName() + '/' + encodeUrl(base);
        }
        return null;
    }

    /** Returns a URL to a local copy of the given resource, or null. There is
     * no filename conflict resolution. */
    protected String addLocalResources(URL url) throws IOException {
        // Attempt to make local copy
        File resourceDir = computeResourceDir();
        if (resourceDir != null) {
            String base = url.getFile();
            base = base.substring(base.lastIndexOf('/') + 1);
            nameToFile.put(base, new File(url.toExternalForm()));

            File target = new File(resourceDir, base);
            try (FileOutputStream output = new FileOutputStream(target);
                 InputStream input = url.openStream()) {
                ByteStreams.copy(input, output);
            }
            return resourceDir.getName() + '/' + encodeUrl(base);
        }
        return null;
    }

    // Based on similar code in com.intellij.openapi.util.io.FileUtilRt
    @Nullable
    static String getRelativePath(File base, File file) {
        if (base == null || file == null) {
            return null;
        }
        if (!base.isDirectory()) {
            base = base.getParentFile();
            if (base == null) {
                return null;
            }
        }
        if (base.equals(file)) {
            return ".";
        }

        final String filePath = file.getAbsolutePath();
        String basePath = base.getAbsolutePath();

        // TODO: Make this return null if we go all the way to the root!

        basePath = !basePath.isEmpty() && basePath.charAt(basePath.length() - 1) == separatorChar
                ? basePath : basePath + separatorChar;

        // Whether filesystem is case sensitive. Technically on OSX you could create a
        // sensitive one, but it's not the default.
        boolean caseSensitive = CURRENT_PLATFORM == PLATFORM_LINUX;
        Locale l = Locale.getDefault();
        String basePathToCompare = caseSensitive ? basePath : basePath.toLowerCase(l);
        String filePathToCompare = caseSensitive ? filePath : filePath.toLowerCase(l);
        if (basePathToCompare.equals(!filePathToCompare.isEmpty()
                && filePathToCompare.charAt(filePathToCompare.length() - 1) == separatorChar
                ? filePathToCompare : filePathToCompare + separatorChar)) {
            return ".";
        }
        int len = 0;
        int lastSeparatorIndex = 0;
        // bug in inspection; see http://youtrack.jetbrains.com/issue/IDEA-118971
        //noinspection ConstantConditions
        while (len < filePath.length() && len < basePath.length()
                && filePathToCompare.charAt(len) == basePathToCompare.charAt(len)) {
            if (basePath.charAt(len) == separatorChar) {
                lastSeparatorIndex = len;
            }
            len++;
        }
        if (len == 0) {
            return null;
        }

        StringBuilder relativePath = new StringBuilder();
        for (int i = len; i < basePath.length(); i++) {
            if (basePath.charAt(i) == separatorChar) {
                relativePath.append("..");
                relativePath.append(separatorChar);
            }
        }
        relativePath.append(filePath.substring(lastSeparatorIndex + 1));
        return relativePath.toString();
    }

    /**
     * Returns whether this report should display info if no issues were found
     */
    public boolean isDisplayEmpty() {
        return displayEmpty;
    }

    /**
     * Sets whether this report should display info if no issues were found
     */
    public void setDisplayEmpty(boolean displayEmpty) {
        this.displayEmpty = displayEmpty;
    }

    private static Set<Issue> studioFixes;

    /**
     * Returns true if the given issue has an automatic IDE fix.
     *
     * @param issue the issue to be checked
     * @return true if the given tool is known to have an automatic fix for the
     *         given issue
     */
    public static boolean hasAutoFix(Issue issue) {
        // List generated by AndroidLintInspectionToolProviderTest in tools/adt/idea;
        // set LIST_ISSUES_WITH_QUICK_FIXES to true
        if (studioFixes == null) {
            studioFixes = Sets.newHashSet(
                    AccessibilityDetector.ISSUE,
                    AlwaysShowActionDetector.ISSUE,
                    AndroidAutoDetector.INVALID_USES_TAG_ISSUE,
                    AndroidTvDetector.MISSING_BANNER,
                    AndroidTvDetector.MISSING_LEANBACK_SUPPORT,
                    AndroidTvDetector.PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE,
                    AndroidTvDetector.UNSUPPORTED_TV_HARDWARE,
                    AnnotationDetector.SWITCH_TYPE_DEF,
                    ApiDetector.INLINED,
                    ApiDetector.OVERRIDE,
                    ApiDetector.UNSUPPORTED,
                    ApiDetector.UNUSED,
                    AppCompatCallDetector.ISSUE,
                    AppIndexingApiDetector.ISSUE_APP_INDEXING,
                    AppIndexingApiDetector.ISSUE_APP_INDEXING_API,
                    //AppIndexingApiDetector.ISSUE_URL_ERROR,
                    ByteOrderMarkDetector.BOM,
                    CleanupDetector.SHARED_PREF,
                    CommentDetector.STOP_SHIP,
                    DetectMissingPrefix.MISSING_NAMESPACE,
                    DuplicateResourceDetector.TYPE_MISMATCH,
                    GradleDetector.COMPATIBILITY,
                    GradleDetector.DEPENDENCY,
                    GradleDetector.DEPRECATED,
                    GradleDetector.NOT_INTERPOLATED,
                    GradleDetector.PLUS,
                    GradleDetector.REMOTE_VERSION,
                    GradleDetector.MIN_SDK_TOO_LOW,
                    GradleDetector.STRING_INTEGER,
                    GridLayoutDetector.ISSUE,
                    IconDetector.WEBP_ELIGIBLE,
                    IconDetector.WEBP_UNSUPPORTED,
                    IncludeDetector.ISSUE,
                    InefficientWeightDetector.BASELINE_WEIGHTS,
                    InefficientWeightDetector.INEFFICIENT_WEIGHT,
                    InefficientWeightDetector.ORIENTATION,
                    JavaPerformanceDetector.USE_VALUE_OF,
                    ManifestDetector.ALLOW_BACKUP,
                    ManifestDetector.APPLICATION_ICON,
                    ManifestDetector.MIPMAP,
                    ManifestDetector.MOCK_LOCATION,
                    ManifestDetector.TARGET_NEWER,
                    MissingClassDetector.INNERCLASS,
                    MissingIdDetector.ISSUE,
                    NamespaceDetector.RES_AUTO,
                    ObsoleteLayoutParamsDetector.ISSUE,
                    ParcelDetector.ISSUE,
                    PropertyFileDetector.ESCAPE,
                    PropertyFileDetector.HTTP,
                    PxUsageDetector.DP_ISSUE,
                    PxUsageDetector.PX_ISSUE,
                    ReadParcelableDetector.ISSUE,
                    RtlDetector.COMPAT,
                    ScrollViewChildDetector.ISSUE,
                    SecurityDetector.EXPORTED_SERVICE,
                    SignatureOrSystemDetector.ISSUE,
                    SupportAnnotationDetector.CHECK_PERMISSION,
                    SupportAnnotationDetector.CHECK_RESULT,
                    SupportAnnotationDetector.MISSING_PERMISSION,
                    TextFieldDetector.ISSUE,
                    TextViewDetector.SELECTABLE,
                    TitleDetector.ISSUE,
                    TypoDetector.ISSUE,
                    TypographyDetector.DASHES,
                    TypographyDetector.ELLIPSIS,
                    TypographyDetector.FRACTIONS,
                    TypographyDetector.OTHER,
                    TypographyDetector.QUOTES,
                    UnusedResourceDetector.ISSUE,
                    UnusedResourceDetector.ISSUE_IDS,
                    UselessViewDetector.USELESS_LEAF,
                    Utf8Detector.ISSUE,
                    WrongCallDetector.ISSUE,
                    WrongCaseDetector.WRONG_CASE
            );
        }
        return studioFixes.contains(issue);
    }

    private String stripPrefix;

    protected String stripPath(@NonNull String path) {
        if (stripPrefix != null && path.startsWith(stripPrefix)
                && path.length() > stripPrefix.length()) {
            int index = stripPrefix.length();
            if (path.charAt(index) == File.separatorChar) {
                index++;
            }
            return path.substring(index);
        }

        return path;
    }

    /** Sets path prefix to strip from displayed file names */
    public void setStripPrefix(@Nullable String prefix) {
        stripPrefix = prefix;
    }

    /**
     * Value object passed to {@link Reporter} instances providing statistics to include in
     * the summary
     */
    public static final class Stats {
        public final int errorCount;
        public final int warningCount;
        public final int baselineWarningCount;
        public final int baselineErrorCount;
        public final int baselineFixedCount;

        public Stats(
                int errorCount,
                int warningCount,
                int baselineErrorCount,
                int baselineWarningCount,
                int baselineFixedCount) {
            this.errorCount = errorCount;
            this.warningCount = warningCount;
            this.baselineWarningCount = baselineWarningCount;
            this.baselineErrorCount = baselineErrorCount;
            this.baselineFixedCount = baselineFixedCount;
        }

        public Stats(
                int errorCount,
                int warningCount) {
            this(errorCount, warningCount, 0, 0, 0);
        }

        public int count() {
            return errorCount + warningCount;
        }
    }
}
