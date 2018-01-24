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

package com.android.tools.lint.client.api;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Position;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.TextFormat;
import com.android.utils.XmlUtils;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * A lint baseline is a collection of warnings for a project that have been
 * obtained from a previous run of lint. These warnings are them exempt from
 * reporting. This lets you set a "baseline" with a known set of issues that you
 * haven't attempted to fix yet, but then be alerted whenever new issues crop
 * up.
 */
public class LintBaseline {
    /**
     * Root tag in baseline files (which can be the XML output report files from lint, or a
     * subset of these
     */
    @SuppressWarnings("unused") // used from IDE
    public static final String TAG_ISSUES = "issues";

    private static final String TAG_ISSUE = "issue";
    private static final String TAG_LOCATION = "location";

    private static final String ATTR_ID = "id";
    private static final String ATTR_MESSAGE = "message";
    private static final String ATTR_FILE = "file";
    private static final String ATTR_LINE = "line";
    private static final String ATTR_COLUMN = "column";

    /** Client to log to */
    private final LintClient client;

    /** Count of number of errors that were filtered out */
    private int foundErrorCount;

    /** Count of number of warnings that were filtered out */
    private int foundWarningCount;

    /** Raw number of issues found in the baseline when opened */
    private int baselineIssueCount;

    /** Map from message to {@link Entry} */
    private final Multimap<String, Entry> messageToEntry = ArrayListMultimap.create(100, 20);

    /**
     * Whether we should write the baseline file when the baseline is closed, if the
     * baseline file doesn't already exist. We don't always do this because for example
     * when lint is run from Gradle, and it's analyzing multiple variants, it does its own
     * merging (across variants) of the results first and then writes that, via the
     * XML reporter.
     */
    private boolean writeOnClose;

    /**
     * Whether the baseline, when configured to write results into the file, will
     * include all found issues, or only issues that are already known. The difference
     * here is whether we're initially creating the baseline (or resetting it), or
     * whether we're trying to only remove fixed issues.
     */
    private boolean removeFixed;

    /**
     * The file to read the baselines from, and if {@link #writeOnClose} is set, to write
     * to when the baseline is {@link #close()}'ed.
     */
    private final File baselineFile;

    /**
     * If non-null, a list of issues to write back out to the baseline file when the
     * baseline is closed.
     */
    private List<ReportedEntry> entriesToWrite;

    public LintBaseline(@NonNull LintClient client, @NonNull File baselineFile) {
        this.client = client;
        this.baselineFile = baselineFile;
        readBaselineFile();
    }

    public static String describeBaselineFilter(int errors, int warnings,
            String baselineDisplayPath) {
        String counts = LintUtils.describeCounts(errors, warnings, false, true);
        // Keep in sync with isFilteredMessage() below
        if (errors + warnings == 1) {
            return String.format("%1$s was filtered out because it is listed in the " +
                    "baseline file, %2$s\n", counts, baselineDisplayPath);
        } else {
            return String.format("%1$s were filtered out because they are listed in the " +
                            "baseline file, %2$s\n", counts, baselineDisplayPath);
        }
    }

    /**
     * Checks if we should report baseline activity (filtered out issues, found fixed issues etc
     * and if so reports them
     */
    void reportBaselineIssues(@NonNull LintDriver driver, @NonNull Project project) {
        if (foundErrorCount > 0 || foundWarningCount > 0) {
            LintClient client = driver.getClient();
            File baselineFile = getFile();
            String message = describeBaselineFilter(foundErrorCount,
                    foundWarningCount, getDisplayPath(project, baselineFile));
            client.report(new Context(driver, project, project, baselineFile, null),
                    IssueRegistry.BASELINE,
                    client.getConfiguration(project, driver).getSeverity(IssueRegistry.BASELINE),
                    Location.create(baselineFile), message, TextFormat.RAW, null);
        }

        int fixedCount = getFixedCount();
        if (fixedCount > 0 && !(writeOnClose && removeFixed)) {
            LintClient client = driver.getClient();
            File baselineFile = getFile();
            Map<String, Integer> ids = Maps.newHashMap();
            for (Entry entry : messageToEntry.values()) {
                Integer count = ids.get(entry.issueId);
                if (count == null) {
                    count = 1;
                } else {
                    count = count+1;
                }
                ids.put(entry.issueId, count);
            }
            List<String> sorted = Lists.newArrayList(ids.keySet());
            Collections.sort(sorted);
            StringBuilder issueTypes = new StringBuilder();
            for (String id : sorted) {
                if (issueTypes.length() > 0) {
                    issueTypes.append(", ");
                }
                issueTypes.append(id);
                Integer count = ids.get(id);
                if (count > 1) {
                    issueTypes.append(" (").append(Integer.toString(count)).append(")");
                }
            }

            // Keep in sync with isFixedMessage() below
            String message = String.format("%1$d errors/warnings were listed in the "
                    + "baseline file (%2$s) but not found in the project; perhaps they have "
                    + "been fixed?", fixedCount, getDisplayPath(project, baselineFile));
            if (LintClient.Companion.isGradle() && project.getGradleProjectModel() != null &&
                    !project.getGradleProjectModel().getLintOptions().isCheckDependencies()) {
                message += " Another possible explanation is that lint recently stopped " +
                        "analyzing (and including results from) dependent projects by default. " +
                        "You can turn this back on with " +
                        "`android.lintOptions.checkDependencies=true`.";
            }
            message += " Unmatched issue types: " + issueTypes;
            client.report(new Context(driver, project, project, baselineFile, null),
                    IssueRegistry.BASELINE,
                    client.getConfiguration(project, driver).getSeverity(IssueRegistry.BASELINE),
                    Location.create(baselineFile), message, TextFormat.RAW, null);
        }
    }

    /**
     * Given an error message produced by this lint detector for the given issue type,
     * determines whether this corresponds to the warning (produced by
     * {link {@link #reportBaselineIssues(LintDriver, Project)} above) that one or
     * more issues have been filtered out.
     * <p>
     * Intended for IDE quickfix implementations.
     */
    @SuppressWarnings("unused") // Used from the IDE
    public static boolean isFilteredMessage(@NonNull String errorMessage,
            @NonNull TextFormat format) {
        return format.toText(errorMessage).contains("filtered out because");
    }

    /**
     * Given an error message produced by this lint detector for the given issue type,
     * determines whether this corresponds to the warning (produced by
     * {link {@link #reportBaselineIssues(LintDriver, Project)} above) that one or
     * more issues have been fixed (present in baseline but not in project.)
     * <p>
     * Intended for IDE quickfix implementations.
     */
    @SuppressWarnings("unused") // Used from the IDE
    public static boolean isFixedMessage(@NonNull String errorMessage,
            @NonNull TextFormat format) {
        errorMessage = format.toText(errorMessage);

        return errorMessage.contains("perhaps they have been fixed");
    }

    /**
     * Checks whether the given warning (of the given issue type, message and location)
     * is present in this baseline, and if so marks it as used such that a second call will
     * not find it.
     * <p>
     * When issue analysis is done you can call {@link #getFoundErrorCount()} and
     * {@link #getFoundWarningCount()} to get a count of the warnings or errors that were
     * matched during the run, and {@link #getFixedCount()} to get a count of the issues
     * that were present in the baseline that were not matched (e.g. have been fixed.)
     *
     * @param issue    the issue type
     * @param location the location of the error
     * @param message  the exact error message (in {@link TextFormat#RAW} format)
     * @param severity the severity of the issue, used to count baseline match as error or warning
     * @param project  the relevant project, if any
     * @return true if this error was found in the baseline and marked as used, and false if this
     * issue is not part of the baseline
     */
    public boolean findAndMark(@NonNull Issue issue, @NonNull Location location,
            @NonNull String message, @Nullable Severity severity, @Nullable Project project) {
        boolean found = findAndMark(issue, location, message, severity);
        if (writeOnClose && (!removeFixed || found)) {
            //noinspection VariableNotUsedInsideIf
            if (entriesToWrite != null) {
                entriesToWrite.add(new ReportedEntry(issue, project, location, message));
            }
        }

        return found;
    }

    private boolean findAndMark(@NonNull Issue issue, @NonNull Location location,
              @NonNull String message, @Nullable Severity severity) {
        Collection<Entry> entries = messageToEntry.get(message);
        if (entries == null || entries.isEmpty()) {
            return false;
        }

        File file = location.getFile();
        String path = file.getPath();
        String issueId = issue.getId();
        for (Entry entry : entries) {
            if (entry.issueId.equals(issueId)) {
                if (isSamePathSuffix(path, entry.path)) {
                    // Remove all linked entries. We don't loop through all the locations;
                    // they're allowed to vary over time, we just assume that all entries
                    // for the same warning should be cleared.
                    while (entry.previous != null) {
                        entry = entry.previous;
                    }
                    while (entry != null) {
                        messageToEntry.remove(entry.message, entry);
                        entry = entry.next;
                    }

                    if (severity == null) {
                        severity = issue.getDefaultSeverity();
                    }
                    if (severity.isError()) {
                        foundErrorCount++;
                    } else {
                        foundWarningCount++;
                    }

                    return true;
                }
            }
        }

        return false;
    }

    /** Returns the number of errors that have been matched from the baseline */
    public int getFoundErrorCount() {
        return foundErrorCount;
    }

    /** Returns the number of warnings that have been matched from the baseline */
    public int getFoundWarningCount() {
        return foundWarningCount;
    }

    /**
     * Returns the number of issues that appear to have been fixed (e.g. are present
     * in the baseline but have not been matched
     */
    public int getFixedCount() {
        return baselineIssueCount - foundErrorCount - foundWarningCount;
    }

    /** Returns the total number of issues contained in this baseline */
    public int getTotalCount() {
        return baselineIssueCount;
    }

    /** Like path.endsWith(suffix), but considers \\ and / identical */
    static boolean isSamePathSuffix(@NonNull String path, @NonNull String suffix) {
        int i = path.length() - 1;
        int j = suffix.length() - 1;

        int begin = 0;
        for (; begin < j; begin++) {
            char c = suffix.charAt(begin);
            if (c != '.' && c != '/' && c != '\\') {
                break;
            }
        }

        if (j - begin > i) {
            return false;
        }

        for (; j > begin; i--, j--) {
            char c1 = path.charAt(i);
            char c2 = suffix.charAt(j);
            if (c1 != c2) {
                if (c1 == '\\') {
                    c1 = '/';
                }
                if (c2 == '\\') {
                    c2 = '/';
                }
                if (c1 != c2) {
                    return false;
                }
            }
        }

        return true;
    }

    /** Read in the XML report */
    private void readBaselineFile() {
        if (!baselineFile.exists()) {
            return;
        }

        try (Reader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(baselineFile), StandardCharsets.UTF_8))) {
            KXmlParser parser = new KXmlParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(reader);

            String issue = null;
            String message = null;
            String path = null;
            String line = null;
            Entry currentEntry = null;

            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                int eventType = parser.getEventType();
                if (eventType == XmlPullParser.END_TAG) {
                    String tag = parser.getName();
                    if (tag.equals(TAG_LOCATION)) {
                        if (issue != null && message != null && path != null) {
                            Entry entry = new Entry(issue, message, path, line);
                            if (currentEntry != null) {
                                currentEntry.next = entry;
                            }
                            entry.previous = currentEntry;
                            currentEntry = entry;
                            messageToEntry.put(entry.message, entry);
                        }
                    } else if (tag.equals(TAG_ISSUE)) {
                        baselineIssueCount++;
                        issue = null;
                        message = null;
                        path = null;
                        line = null;
                        currentEntry = null;
                    }
                } else if (eventType != XmlPullParser.START_TAG) {
                    continue;
                }

                for (int i = 0, n = parser.getAttributeCount(); i < n; i++) {
                    String name = parser.getAttributeName(i);
                    String value = parser.getAttributeValue(i);
                    switch (name) {
                        case ATTR_ID: issue = value; break;
                        case ATTR_MESSAGE: {
                            message = value;
                            // Error message changed recently; let's stay compatible
                            if (message.startsWith("[")) {
                                if (message.startsWith("[I18N] ")) {
                                    message = message.substring("[I18N] ".length());
                                } else if (message.startsWith("[Accessibility] ")) {
                                    message = message.substring("[Accessibility] ".length());
                                }
                            }
                            break;
                        }
                        case ATTR_FILE: path = value; break;
                        case ATTR_LINE: line = value; break;
                    }
                }
            }
        } catch (IOException | XmlPullParserException e) {
            if (client != null) {
                client.log(e, null);
            } else {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the file which records the data in this baseline
     * @return the baseline file
     */
    @NonNull
    public File getFile() {
        return baselineFile;
    }

    /** Returns whether this baseline is writing its result upon close */
    public boolean isWriteOnClose() {
        return writeOnClose;
    }

    /** Sets whether the baseline should write its matched entries on {@link #close()} */
    public void setWriteOnClose(boolean writeOnClose) {
        if (writeOnClose) {
            int count = baselineIssueCount > 0 ? baselineIssueCount + 10 : 30;
            entriesToWrite = Lists.newArrayListWithCapacity(count);
        }
        this.writeOnClose = writeOnClose;
    }

    /**
     * Whether the baseline when writing the file will skip fixed issues, or include all.
     * @return true if skipping fixed issues
     */
    public boolean isRemoveFixed() {
        return removeFixed;
    }

    /**
     * Whether the baseline when writing the file should skip fixed issues, or include all.
     * @param skipFixed true to skip fixed issues
     */
    public void setRemoveFixed(boolean skipFixed) {
        this.removeFixed = skipFixed;
    }

    /** Finishes writing the baseline */
    public void close() {
        if (writeOnClose) {
            File parentFile = baselineFile.getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                boolean mkdirs = parentFile.mkdirs();
                if (!mkdirs) {
                    client.log(null, "Couldn't create %1$s", parentFile);
                    return;
                }
            }

            try (Writer writer = new BufferedWriter(new FileWriter(baselineFile))) {
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                // Format 4: added urls= attribute with all more info links, comma separated
                writer.write('<');
                writer.write(TAG_ISSUES);
                writer.write(" format=\"4\"");
                String revision = client.getClientRevision();
                if (revision != null) {
                    writer.write(String.format(" by=\"lint %1$s\"", revision));
                }
                writer.write(">\n");

                baselineIssueCount = 0;
                if (entriesToWrite != null) {
                    Collections.sort(entriesToWrite);
                    for (ReportedEntry entry : entriesToWrite) {
                        entry.write(writer, client);
                        baselineIssueCount++;
                    }
                }

                writer.write("\n</");
                writer.write(TAG_ISSUES);
                writer.write(">\n");
                writer.close();
            } catch (IOException ioe) {
                client.log(ioe, null);
            }
        }
    }

    private static String getDisplayPath(@Nullable Project project, @NonNull File file) {
        String path = file.getPath();
        if (project != null && path.startsWith(project.getReferenceDir().getPath())) {
            int chop = project.getReferenceDir().getPath().length();
            if (path.length() > chop && path.charAt(chop) == File.separatorChar) {
                chop++;
            }
            path = path.substring(chop);
            if (path.isEmpty()) {
                path = file.getName();
            }
        }

        return path;
    }

    private static void writeAttribute(Writer writer, int indent, String name, String value)
            throws IOException {
        writer.write('\n');
        indent(writer, indent);
        writer.write(name);
        writer.write('=');
        writer.write('"');
        writer.write(XmlUtils.toXmlAttributeValue(value));
        writer.write('"');
    }

    private static void indent(Writer writer, int indent) throws IOException {
        for (int level = 0; level < indent; level++) {
            writer.write("    ");
        }
    }

    /**
     * Entries that have been reported during this lint run. We only create these
     * when we need to write a baseline file (since we need to sort them before
     * writing out the result file, to ensure stable files.
     */
    private static class ReportedEntry implements Comparable<ReportedEntry> {
        public final Issue issue;
        public final String message;
        public final Location location;
        public final Project project;

        public ReportedEntry(@NonNull Issue issue, @Nullable Project project,
                @NonNull Location location, @NonNull String message) {
            this.issue = issue;
            this.location = location;
            this.project = project;
            this.message = message;
        }

        @Override
        public int compareTo(@NonNull ReportedEntry other) {
            // Sort by category, then by priority, then by id,
            // then by file, then by line
            int categoryDelta = issue.getCategory().compareTo(other.issue.getCategory());
            if (categoryDelta != 0) {
                return categoryDelta;
            }
            // DECREASING priority order
            int priorityDelta = other.issue.getPriority() - issue.getPriority();
            if (priorityDelta != 0) {
                return priorityDelta;
            }
            String id1 = issue.getId();
            String id2 = other.issue.getId();
            int idDelta = id1.compareTo(id2);
            if (idDelta != 0) {
                return idDelta;
            }
            File file = location.getFile();
            File otherFile = other.location.getFile();
            int fileDelta = file.getName().compareTo(
                    otherFile.getName());
            if (fileDelta != 0) {
                return fileDelta;
            }

            Position start = location.getStart();
            Position otherStart = other.location.getStart();
            int line = start != null ? start.getLine() : -1;
            int otherLine = otherStart != null ? otherStart.getLine() : -1;

            if (line != otherLine) {
                return line - otherLine;
            }

            int delta = message.compareTo(other.message);
            if (delta != 0) {
                return delta;
            }

            delta = file.compareTo(otherFile);
            if (delta != 0) {
                return delta;
            }

            Location secondary1 = location.getSecondary();
            File secondaryFile1 = secondary1 != null ? secondary1.getFile() : null;
            Location secondary2 = other.location.getSecondary();
            File secondaryFile2 = secondary2 != null ? secondary2.getFile() : null;
            if (secondaryFile1 != null) {
                if (secondaryFile2 != null) {
                    return secondaryFile1.compareTo(secondaryFile2);
                } else {
                    return -1;
                }
            } else //noinspection VariableNotUsedInsideIf
                if (secondaryFile2 != null) {
                return 1;
            }

            // This handles the case where you have a huge XML document without hewlines,
            // such that all the errors end up on the same line.
            if (start != null && otherStart != null) {
                delta = start.getColumn() - otherStart.getColumn();
                if (delta != 0) {
                    return delta;
                }
            }

            return 0;
        }

        /**
         * Given the report of an issue, add it to the baseline being built in the XML writer
         */
        void write(
                @NonNull Writer writer,
                @NonNull LintClient client) {
            try {
                writer.write('\n');
                indent(writer, 1);
                writer.write('<');
                writer.write(TAG_ISSUE);
                writeAttribute(writer, 2, ATTR_ID, issue.getId());

                writeAttribute(writer, 2, ATTR_MESSAGE, message);

                writer.write(">\n");
                Location currentLocation = location;
                while (currentLocation != null) {
                    //
                    //
                    //
                    // IMPORTANT: Keep this format compatible with the XML report format
                    //            encoded by the XmlReporter! That way XML reports and baseline
                    //            files can be mix & matched. (Compatible=subset.)
                    //
                    //
                    indent(writer, 2);
                    writer.write('<');
                    writer.write(TAG_LOCATION);
                    String path = getDisplayPath(project, currentLocation.getFile());
                    writeAttribute(writer, 3, ATTR_FILE, path);
                    Position start = currentLocation.getStart();
                    if (start != null) {
                        int line = start.getLine();
                        if (line >= 0) {
                            // +1: Line numbers internally are 0-based, report should be
                            // 1-based.
                            writeAttribute(writer, 3, ATTR_LINE, Integer.toString(line + 1));
                        }
                    }

                    writer.write("/>\n");
                    currentLocation = currentLocation.getSecondary();
                }
                indent(writer, 1);
                writer.write("</");
                writer.write(TAG_ISSUE);
                writer.write(">\n");
            } catch (IOException ioe) {
                client.log(ioe, null);
            }
        }
    }

    /**
     * Entry loaded from the baseline file. Note that for an error with multiple locations,
     * there may be multiple entries; these are linked by next/previous fields.
     */
    private static class Entry {
        public final String issueId;
        public final String message;
        public final String path;
        public final String line;
        /**
         * An issue can have multiple locations; we create a separate entry for each
         * but we link them together such that we can mark them all fixed
         */
        public Entry next;
        public Entry previous;

        public Entry(
                @NonNull String issueId,
                @NonNull String message,
                @NonNull String path,
                @Nullable String line) {
            this.issueId = issueId;
            this.message = message;
            this.path = path;
            this.line = line;
        }
    }
}
