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

package com.android.tools.lint.client.api;

import static com.android.SdkConstants.CURRENT_PLATFORM;
import static com.android.SdkConstants.PLATFORM_WINDOWS;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.TextFormat;
import com.android.utils.XmlUtils;
import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

/**
 * Default implementation of a {@link Configuration} which reads and writes
 * configuration data into {@code lint.xml} in the project directory.
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public class DefaultConfiguration extends Configuration {

    private final LintClient client;
    /** Default name of the configuration file */
    public static final String CONFIG_FILE_NAME = "lint.xml";

    // Lint XML File

    /** The root tag in a configuration file */
    public static final String TAG_LINT = "lint";

    private static final String TAG_ISSUE = "issue";
    private static final String ATTR_ID = "id";
    private static final String ATTR_SEVERITY = "severity";
    private static final String ATTR_PATH = "path";
    private static final String ATTR_REGEXP = "regexp";
    private static final String TAG_IGNORE = "ignore";
    public static final String VALUE_ALL = "all";
    private static final String ATTR_BASELINE = "baseline";

    private static final String RES_PATH_START = "res" + File.separatorChar;
    private static final int RES_PATH_START_LEN = RES_PATH_START.length();

    private final Configuration parent;
    private final Project project;
    private final File configFile;
    private boolean bulkEditing;
    private File baselineFile;

    /** Map from id to list of project-relative paths for suppressed warnings */
    private Map<String, List<String>> suppressed;

    /** Map from id to regular expressions. */
    @Nullable
    private Map<String, List<Pattern>> regexps;

    /**
     * Map from id to custom {@link Severity} override
     */
    protected Map<String, Severity> severity;

    protected DefaultConfiguration(
            @NonNull LintClient client,
            @Nullable Project project,
            @Nullable Configuration parent,
            @NonNull File configFile) {
        this.client = client;
        this.project = project;
        this.parent = parent;
        this.configFile = configFile;
    }

    protected DefaultConfiguration(
            @NonNull LintClient client,
            @NonNull Project project,
            @Nullable Configuration parent) {
        this(client, project, parent, new File(project.getDir(), CONFIG_FILE_NAME));
    }

    /**
     * Creates a new {@link DefaultConfiguration}
     *
     * @param client the client to report errors to etc
     * @param project the associated project
     * @param parent the parent/fallback configuration or null
     * @return a new configuration
     */
    @NonNull
    public static DefaultConfiguration create(
            @NonNull LintClient client,
            @NonNull Project project,
            @Nullable Configuration parent) {
        return new DefaultConfiguration(client, project, parent);
    }

    /**
     * Creates a new {@link DefaultConfiguration} for the given lint config
     * file, not affiliated with a project. This is used for global
     * configurations.
     *
     * @param client   the client to report errors to etc
     * @param lintFile the lint file containing the configuration
     * @return a new configuration
     */
    @NonNull
    public static DefaultConfiguration create(@NonNull LintClient client, @NonNull File lintFile) {
        return new DefaultConfiguration(client, null, null, lintFile);
    }

    @Override
    public boolean isIgnored(
            @NonNull Context context,
            @NonNull Issue issue,
            @Nullable Location location,
            @NonNull String message) {
        ensureInitialized();

        String id = issue.getId();
        List<String> paths = suppressed.get(id);
        List<String> all = suppressed.get(VALUE_ALL);
        if (paths == null) {
            paths = all;
        } else if (all != null) {
            paths.addAll(all);
        }
        if (paths != null && location != null) {
            File file = location.getFile();
            String relativePath = context.getProject().getRelativePath(file);
            for (String suppressedPath : paths) {
                if (suppressedPath.equals(relativePath)) {
                    return true;
                }
                // Also allow a prefix
                if (relativePath.startsWith(suppressedPath)) {
                    return true;
                }
            }
            // A project can have multiple resources folders. The code before this
            // only checks for paths relative to project root (which doesn't work for paths such as
            // res/layout/foo.xml defined in lint.xml - when using gradle where the
            // resource directory points to src/main/res)
            // Here we check if any of the suppressed paths are relative to the resource folders
            // of a project.
            Set<Path> suppressedPathSet = new HashSet<>();
            for (String p : paths) {
                if (p.startsWith(RES_PATH_START)) {
                    Path path = Paths.get(p.substring(RES_PATH_START_LEN));
                    suppressedPathSet.add(path);
                }
            }

            if (!suppressedPathSet.isEmpty()) {
                Path toCheck = file.toPath();
                // Is it relative to any of the resource folders?
                for (File resDir : context.getProject().getResourceFolders()) {
                    Path path = resDir.toPath();
                    Path relative = path.relativize(toCheck);
                    if (suppressedPathSet.contains(relative)) {
                        return true;
                    }
                    // Allow suppress the relativePath if it is a prefix
                    if (suppressedPathSet.stream().anyMatch(relative::startsWith)) {
                        return true;
                    }
                }
            }
        }

        if (regexps != null) {
            List<Pattern> regexps = this.regexps.get(id);
            List<Pattern> allRegexps = this.regexps.get(VALUE_ALL);
            if (regexps == null) {
                regexps = allRegexps;
            } else if (allRegexps != null) {
                regexps.addAll(allRegexps);
            }
            if (regexps != null && location != null) {
                // Check message
                for (Pattern regexp : regexps) {
                    Matcher matcher = regexp.matcher(message);
                    if (matcher.find()) {
                        return true;
                    }
                }

                // Check location
                File file = location.getFile();
                String relativePath = context.getProject().getRelativePath(file);
                boolean checkUnixPath = false;
                for (Pattern regexp : regexps) {
                    Matcher matcher = regexp.matcher(relativePath);
                    if (matcher.find()) {
                        return true;
                    } else if (regexp.pattern().indexOf('/') != -1) {
                        checkUnixPath = true;
                    }
                }

                if (checkUnixPath && CURRENT_PLATFORM == PLATFORM_WINDOWS) {
                    relativePath = relativePath.replace('\\', '/');
                    for (Pattern regexp : regexps) {
                        Matcher matcher = regexp.matcher(relativePath);
                        if (matcher.find()) {
                            return true;
                        }
                    }
                }
            }
        }

        return parent != null && parent.isIgnored(context, issue, location, message);
    }

    @NonNull
    protected Severity getDefaultSeverity(@NonNull Issue issue) {
        if (!issue.isEnabledByDefault()) {
            return Severity.IGNORE;
        }

        return issue.getDefaultSeverity();
    }

    @Override
    @NonNull
    public Severity getSeverity(@NonNull Issue issue) {
        ensureInitialized();

        Severity severity = this.severity.get(issue.getId());
        if (severity == null) {
            severity = this.severity.get(VALUE_ALL);
        }

        if (severity != null) {
            return severity;
        }

        if (parent != null) {
            return parent.getSeverity(issue);
        }

        return getDefaultSeverity(issue);
    }

    private void ensureInitialized() {
        if (suppressed == null) {
            readConfig();
        }
    }

    private void formatError(String message, Object... args) {
        if (args != null && args.length > 0) {
            message = String.format(message, args);
        }
        message = "Failed to parse `lint.xml` configuration file: " + message;
        LintDriver driver = new LintDriver(
                new IssueRegistry() {
            @Override @NonNull public List<Issue> getIssues() {
                return Collections.emptyList();
            }
        }, client, new LintRequest(client, Collections.emptyList()));
        client.report(new Context(driver, project, project, configFile, null),
                IssueRegistry.LINT_ERROR,
                project.getConfiguration(driver).getSeverity(IssueRegistry.LINT_ERROR),
                Location.create(configFile), message, TextFormat.RAW, null);
    }

    private void readConfig() {
        suppressed = new HashMap<>();
        severity = new HashMap<>();

        if (!configFile.exists()) {
            return;
        }

        try {
            // TODO: Switch to a pull parser!
            Document document = XmlUtils.parseUtfXmlFile(configFile, false);
            String baseline = document.getDocumentElement().getAttribute(ATTR_BASELINE);
            if (!baseline.isEmpty()) {
                baselineFile = new File(baseline.replace('/', File.separatorChar));
                if (!baselineFile.isAbsolute()) {
                    baselineFile = new File(project.getDir(), baselineFile.getPath());
                }
            }
            NodeList issues = document.getElementsByTagName(TAG_ISSUE);
            Splitter splitter = Splitter.on(',').trimResults().omitEmptyStrings();
            for (int i = 0, count = issues.getLength(); i < count; i++) {
                Node node = issues.item(i);
                Element element = (Element) node;
                String idList = element.getAttribute(ATTR_ID);
                if (idList.isEmpty()) {
                    formatError("Invalid lint config file: Missing required issue id attribute");
                    continue;
                }
                Iterable<String> ids = splitter.split(idList);

                NamedNodeMap attributes = node.getAttributes();
                for (int j = 0, n = attributes.getLength(); j < n; j++) {
                    Node attribute = attributes.item(j);
                    String name = attribute.getNodeName();
                    String value = attribute.getNodeValue();
                    if (ATTR_ID.equals(name)) {
                        // already handled
                    } else if (ATTR_SEVERITY.equals(name)) {
                        for (Severity severity : Severity.values()) {
                            if (value.equalsIgnoreCase(severity.name())) {
                                for (String id : ids) {
                                    this.severity.put(id, severity);
                                }
                                break;
                            }
                        }
                    } else {
                        formatError("Unexpected attribute \"%1$s\"", name);
                    }
                }

                // Look up ignored errors
                NodeList childNodes = element.getChildNodes();
                if (childNodes.getLength() > 0) {
                    for (int j = 0, n = childNodes.getLength(); j < n; j++) {
                        Node child = childNodes.item(j);
                        if (child.getNodeType() == Node.ELEMENT_NODE) {
                            Element ignore = (Element) child;
                            String path = ignore.getAttribute(ATTR_PATH);
                            if (path.isEmpty()) {
                                String regexp = ignore.getAttribute(ATTR_REGEXP);
                                if (regexp.isEmpty()) {
                                    formatError("Missing required attribute %1$s or %2$s under %3$s",
                                        ATTR_PATH, ATTR_REGEXP, idList);
                                } else {
                                    addRegexp(idList, ids, n, regexp, false);
                                }
                            } else {
                                // Normalize path format to File.separator. Also
                                // handle the file format containing / or \.
                                if (File.separatorChar == '/') {
                                    path = path.replace('\\', '/');
                                } else {
                                    path = path.replace('/', File.separatorChar);
                                }

                                if (path.indexOf('*') != -1) {
                                    String regexp = globToRegexp(path);
                                    addRegexp(idList, ids, n, regexp, false);
                                } else {
                                    for (String id : ids) {
                                        List<String> paths = suppressed.get(id);
                                        if (paths == null) {
                                            paths = new ArrayList<>(n / 2 + 1);
                                            suppressed.put(id, paths);
                                        }
                                        paths.add(path);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (SAXParseException e) {
            formatError(e.getMessage());
        } catch (Exception e) {
            client.log(e, null);
        }
    }

    @NonNull
    public static String globToRegexp(@NonNull String glob) {
        StringBuilder sb = new StringBuilder(glob.length() * 2);
        int begin = 0;
        sb.append('^');
        for (int i = 0, n = glob.length(); i < n; i++) {
            char c = glob.charAt(i);
            if (c == '*') {
                begin = appendQuoted(sb, glob, begin, i) + 1;
                if (i < n - 1 && glob.charAt(i + 1) == '*') {
                    i++;
                    begin++;
                }
                sb.append(".*?");
            } else if (c == '?') {
                begin = appendQuoted(sb, glob, begin, i) + 1;
                sb.append(".?");
            }
        }
        appendQuoted(sb, glob, begin, glob.length());
        sb.append('$');
        return sb.toString();
    }

    private static int appendQuoted(StringBuilder sb, String s, int from, int to) {
        if (to > from) {
            boolean isSimple = true;
            for (int i = from; i < to; i++) {
                char c = s.charAt(i);
                if (!Character.isLetterOrDigit(c) && c != '/' && c != ' ') {
                    isSimple = false;
                    break;
                }
            }
            if (isSimple) {
                for (int i = from; i < to; i++) {
                    sb.append(s.charAt(i));
                }
                return to;
            }
            sb.append(Pattern.quote(s.substring(from, to)));
        }
        return to;
    }

    private void addRegexp(@NonNull String idList, @NonNull Iterable<String> ids, int n,
            @NonNull String regexp, boolean silent) {
        try {
            if (regexps == null) {
                regexps = new HashMap<>();
            }
            Pattern pattern = Pattern.compile(regexp);
            for (String id : ids) {
                List<Pattern> paths = regexps.get(id);
                if (paths == null) {
                    paths = new ArrayList<>(n / 2 + 1);
                    regexps.put(id, paths);
                }
                paths.add(pattern);
            }
        } catch (PatternSyntaxException e) {
            if (!silent) {
                formatError("Invalid pattern %1$s under %2$s: %3$s",
                        regexp, idList, e.getDescription());
            }
        }
    }

    private void writeConfig() {
        try {
            // Write the contents to a new file first such that we don't clobber the
            // existing file if some I/O error occurs.
            File file = new File(configFile.getParentFile(),
                    configFile.getName() + ".new");

            Writer writer = new BufferedWriter(new FileWriter(file));
            writer.write(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<");
            writer.write(TAG_LINT);

            if (baselineFile != null) {
                writer.write(" baseline=\"");
                String path = project != null ?
                        project.getRelativePath(baselineFile) : baselineFile.getPath();
                writeAttribute(writer, ATTR_BASELINE, path.replace('\\', '/'));
            }
            writer.write(">\n");

            if (!suppressed.isEmpty() || !severity.isEmpty()) {
                // Process the maps in a stable sorted order such that if the
                // files are checked into version control with the project,
                // there are no random diffs just because hashing algorithms
                // differ:
                Set<String> idSet = new HashSet<>();
                for (String id : suppressed.keySet()) {
                    idSet.add(id);
                }
                for (String id : severity.keySet()) {
                    idSet.add(id);
                }
                List<String> ids = new ArrayList<>(idSet);
                Collections.sort(ids);

                for (String id : ids) {
                    writer.write("    <");
                    writer.write(TAG_ISSUE);
                    writeAttribute(writer, ATTR_ID, id);
                    Severity severity = this.severity.get(id);
                    if (severity != null) {
                        writeAttribute(writer, ATTR_SEVERITY,
                                severity.name().toLowerCase(Locale.US));
                    }

                    List<Pattern> regexps = this.regexps != null ? this.regexps.get(id) : null;
                    List<String> paths = suppressed.get(id);
                    if (paths != null && !paths.isEmpty()
                            || regexps != null && !regexps.isEmpty()) {
                        writer.write('>');
                        writer.write('\n');
                        // The paths are already kept in sorted order when they are modified
                        // by ignore(...)
                        if (paths != null) {
                            for (String path : paths) {
                                writer.write("        <");
                                writer.write(TAG_IGNORE);
                                writeAttribute(writer, ATTR_PATH, path.replace('\\', '/'));
                                writer.write(" />\n");
                            }
                        }
                        if (regexps != null) {
                            for (Pattern regexp : regexps) {
                                writer.write("        <");
                                writer.write(TAG_IGNORE);
                                writeAttribute(writer, ATTR_REGEXP, regexp.pattern());
                                writer.write(" />\n");
                            }
                        }
                        writer.write("    </");
                        writer.write(TAG_ISSUE);
                        writer.write('>');
                        writer.write('\n');
                    } else {
                        writer.write(" />\n");
                    }
                }
            }

            writer.write("</lint>\n");
            writer.close();

            // Move file into place: move current version to lint.xml~ (removing the old ~ file
            // if it exists), then move the new version to lint.xml.
            File oldFile = new File(configFile.getParentFile(),
                    configFile.getName() + '~');
            if (oldFile.exists()) {
                oldFile.delete();
            }
            if (configFile.exists()) {
                configFile.renameTo(oldFile);
            }
            boolean ok = file.renameTo(configFile);
            if (ok && oldFile.exists()) {
                oldFile.delete();
            }
        } catch (Exception e) {
            client.log(e, null);
        }
    }

    private static void writeAttribute(
            @NonNull Writer writer, @NonNull String name, @NonNull String value)
            throws IOException {
        writer.write(' ');
        writer.write(name);
        writer.write('=');
        writer.write('"');
        writer.write(value);
        writer.write('"');
    }

    @Override
    public void ignore(
            @NonNull Context context,
            @NonNull Issue issue,
            @Nullable Location location,
            @NonNull String message) {
        // This configuration only supports suppressing warnings on a per-file basis
        if (location != null) {
            ignore(issue, location.getFile());
        }
    }

    @Override
    public void ignore(@NonNull Issue issue, @NonNull File file) {
        ensureInitialized();

        String path = project != null ? project.getRelativePath(file) : file.getPath();

        List<String> paths = suppressed.get(issue.getId());
        if (paths == null) {
            paths = new ArrayList<>();
            suppressed.put(issue.getId(), paths);
        }
        paths.add(path);

        // Keep paths sorted alphabetically; makes XML output stable
        Collections.sort(paths);

        if (!bulkEditing) {
            writeConfig();
        }
    }

    @Override
    public void setSeverity(@NonNull Issue issue, @Nullable Severity severity) {
        ensureInitialized();

        String id = issue.getId();
        if (severity == null) {
            this.severity.remove(id);
        } else {
            this.severity.put(id, severity);
        }

        if (!bulkEditing) {
            writeConfig();
        }
    }

    @Override
    public void startBulkEditing() {
        bulkEditing = true;
    }

    @Override
    public void finishBulkEditing() {
        bulkEditing = false;
        writeConfig();
    }

    @VisibleForTesting
    File getConfigFile() {
        return configFile;
    }

    @Override
    @Nullable
    public File getBaselineFile() {
        if (baselineFile != null) {
            if (project != null && !baselineFile.isAbsolute()) {
                return new File(project.getDir(), baselineFile.getPath());
            }
        }
        return baselineFile;
    }

    @Override
    public void setBaselineFile(@Nullable File baselineFile) {
        this.baselineFile = baselineFile;
    }
}