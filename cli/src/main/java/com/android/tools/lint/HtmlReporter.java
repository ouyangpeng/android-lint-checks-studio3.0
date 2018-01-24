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

import static com.android.SdkConstants.DOT_JPG;
import static com.android.SdkConstants.DOT_PNG;
import static com.android.SdkConstants.VALUE_FALSE;
import static com.android.tools.lint.detector.api.LintUtils.describeCounts;
import static com.android.tools.lint.detector.api.LintUtils.endsWith;
import static com.android.tools.lint.detector.api.TextFormat.HTML;
import static com.android.tools.lint.detector.api.TextFormat.RAW;

import com.android.annotations.NonNull;
import com.android.tools.lint.checks.BuiltinIssueRegistry;
import com.android.tools.lint.client.api.Configuration;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Position;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.TextFormat;
import com.android.utils.SdkUtils;
import com.google.common.annotations.Beta;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A reporter which emits lint results into an HTML report.
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public class HtmlReporter extends Reporter {
    public static final boolean INLINE_RESOURCES =
            !VALUE_FALSE.equals(System.getProperty("lint.inline-resources"));
    private static final boolean USE_HOLO_STYLE = true;
    @SuppressWarnings("ConstantConditions")
    private static final String CSS = USE_HOLO_STYLE
            ? "hololike.css" : "default.css";

    /**
     * Maximum number of warnings allowed for a single issue type before we
     * split up and hide all but the first {@link #SHOWN_COUNT} items.
     */
    private static final int SPLIT_LIMIT = 8;
    /**
     * When a warning has at least {@link #SPLIT_LIMIT} items, then we show the
     * following number of items before the "Show more" button/link.
     */
    private static final int SHOWN_COUNT = SPLIT_LIMIT - 3;

    protected final Writer writer;
    protected final LintCliFlags flags;
    private String fixUrl;

    /**
     * Creates a new {@link HtmlReporter}
     *
     * @param client the associated client
     * @param output the output file
     * @param flags the command line flags
     * @throws IOException if an error occurs
     */
    public HtmlReporter(
            @NonNull LintCliClient client,
            @NonNull File output,
            @NonNull LintCliFlags flags) throws IOException {
        super(client, output);
        writer = new BufferedWriter(Files.newWriter(output, Charsets.UTF_8));
        this.flags = flags;
    }

    @Override
    public void write(@NonNull Stats stats, List<Warning> issues) throws IOException {
        Map<Issue, String> missing = computeMissingIssues(issues);

        writer.write(
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                "<head>\n" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />" +
                "<title>" + title + "</title>\n");

        writeStyleSheet();

        if (!simpleFormat) {
            // JavaScript for collapsing/expanding long lists
            writer.write(
                "<script language=\"javascript\" type=\"text/javascript\"> \n" +
                "<!--\n" +
                "function reveal(id) {\n" +
                "if (document.getElementById) {\n" +
                "document.getElementById(id).style.display = 'block';\n" +
                "document.getElementById(id+'Link').style.display = 'none';\n" +
                "}\n" +
                "}\n" +
                "//--> \n" +
                "</script>\n");
        }

        writer.write(
                "</head>\n" +
                "<body>\n" +
                "<h1>" +
                        title +
                "</h1>\n" +
                "<div class=\"titleSeparator\"></div>\n");

        writer.write(String.format("Check performed at %1$s.",
                new Date().toString()));
        writer.write("<br/>\n");
        writer.write(String.format("%1$s found",
                describeCounts(stats.errorCount, stats.warningCount, false, true)));
        if (stats.baselineErrorCount > 0 || stats.baselineWarningCount > 0) {
            File baselineFile = flags.getBaselineFile();
            assert baselineFile != null;
            writer.write(String.format(" (%1$s filtered by baseline %2$s)",
                    describeCounts(stats.baselineErrorCount, stats.baselineWarningCount, false,
                            true),
                    baselineFile.getName()));
        }
        writer.write(":");
        writer.write("<br/><br/>\n");

        Issue previousIssue = null;
        if (!issues.isEmpty()) {
            List<List<Warning>> related = new ArrayList<>();
            List<Warning> currentList = null;
            for (Warning warning : issues) {
                if (warning.issue != previousIssue) {
                    previousIssue = warning.issue;
                    currentList = new ArrayList<>();
                    related.add(currentList);
                }
                assert currentList != null;
                currentList.add(warning);
            }

            writeOverview(related, missing.size());

            Category previousCategory = null;
            for (List<Warning> warnings : related) {
                Warning first = warnings.get(0);
                Issue issue = first.issue;

                if (issue.getCategory() != previousCategory) {
                    previousCategory = issue.getCategory();
                    writer.write("\n<a name=\"");
                    writer.write(issue.getCategory().getFullName());
                    writer.write("\"></a>\n");
                    writer.write("<div class=\"category\"><a href=\"#\" title=\"Return to top\">");
                    writer.write(issue.getCategory().getFullName());
                    writer.write("</a><div class=\"categorySeparator\"></div>\n");
                    writer.write("</div>\n");
                }

                writer.write("<a name=\"" + issue.getId() + "\"></a>\n");
                writer.write("<div class=\"issue\">\n");

                // Explain this issue
                writer.write("<div class=\"id\"><a href=\"#\" title=\"Return to top\">");
                writer.write(issue.getId());
                writer.write(": ");
                writer.write(issue.getBriefDescription(HTML));
                writer.write("</a><div class=\"issueSeparator\"></div>\n");
                writer.write("</div>\n");

                writer.write("<div class=\"warningslist\">\n");
                boolean partialHide = !simpleFormat && warnings.size() > SPLIT_LIMIT;

                int count = 0;
                for (Warning warning : warnings) {
                    if (partialHide && count == SHOWN_COUNT) {
                        String id = warning.issue.getId() + "Div";
                        writer.write("<button id=\"");
                        writer.write(id);
                        writer.write("Link\" onclick=\"reveal('");
                        writer.write(id);
                        writer.write("');\" />");
                        writer.write(String.format("+ %1$d More Occurrences...",
                                warnings.size() - SHOWN_COUNT));
                        writer.write("</button>\n");
                        writer.write("<div id=\"");
                        writer.write(id);
                        writer.write("\" style=\"display: none\">\n");
                    }
                    count++;
                    String url = null;
                    if (warning.path != null) {
                        url = writeLocation(warning.file, warning.path, warning.line);
                        writer.write(':');
                        writer.write(' ');
                    }

                    // Is the URL for a single image? If so, place it here near the top
                    // of the error floating on the right. If there are multiple images,
                    // they will instead be placed in a horizontal box below the error
                    boolean addedImage = false;
                    if (url != null && warning.location != null
                            && warning.location.getSecondary() == null) {
                        addedImage = addImage(url, warning.location);
                    }
                    writer.write("<span class=\"message\">");
                    writer.append(RAW.convertTo(warning.message, HTML));
                    writer.write("</span>");
                    if (addedImage) {
                        writer.write("<br clear=\"right\"/>");
                    } else {
                        writer.write("<br />");
                    }

                    // Insert surrounding code block window
                    if (warning.line >= 0 && warning.fileContents != null) {
                        writer.write("<pre class=\"errorlines\">\n");
                        appendCodeBlock(warning.fileContents, warning.line, warning.offset);
                        writer.write("\n</pre>");
                    }
                    writer.write('\n');
                    if (warning.location != null && warning.location.getSecondary() != null) {
                        writer.write("<ul>");
                        Location l = warning.location.getSecondary();
                        int otherLocations = 0;
                        while (l != null) {
                            String message = l.getMessage();
                            if (message != null && !message.isEmpty()) {
                                Position start = l.getStart();
                                int line = start != null ? start.getLine() : -1;
                                String path = client.getDisplayPath(warning.project, l.getFile());
                                writeLocation(l.getFile(), path, line);
                                writer.write(':');
                                writer.write(' ');
                                writer.write("<span class=\"message\">");
                                writer.append(RAW.convertTo(message, HTML));
                                writer.write("</span>");
                                writer.write("<br />");

                                String name = l.getFile().getName();
                                if (!(endsWith(name, DOT_PNG) || endsWith(name, DOT_JPG))) {
                                    CharSequence s = client.readFile(l.getFile());
                                    if (s.length() > 0) {
                                        writer.write("<pre class=\"errorlines\">\n");
                                        int offset = start != null ? start.getOffset() : -1;
                                        appendCodeBlock(s, line, offset);
                                        writer.write("\n</pre>");
                                    }
                                }
                            } else {
                                otherLocations++;
                            }

                            l = l.getSecondary();
                        }
                        writer.write("</ul>");
                        if (otherLocations > 0) {
                            String id = "Location" + count + "Div";
                            writer.write("<button id=\"");
                            writer.write(id);
                            writer.write("Link\" onclick=\"reveal('");
                            writer.write(id);
                            writer.write("');\" />");
                            writer.write(String.format("+ %1$d Additional Locations...",
                                    otherLocations));
                            writer.write("</button>\n");
                            writer.write("<div id=\"");
                            writer.write(id);
                            writer.write("\" style=\"display: none\">\n");

                            writer.write("Additional locations: ");
                            writer.write("<ul>\n");
                            l = warning.location.getSecondary();
                            while (l != null) {
                                Position start = l.getStart();
                                int line = start != null ? start.getLine() : -1;
                                String path = client.getDisplayPath(warning.project, l.getFile());
                                writer.write("<li> ");
                                writeLocation(l.getFile(), path, line);
                                writer.write("\n");
                                l = l.getSecondary();
                            }
                            writer.write("</ul>\n");

                            writer.write("</div><br/><br/>\n");
                        }
                    }

                    // Place a block of images?
                    if (!addedImage && url != null && warning.location != null
                            && warning.location.getSecondary() != null) {
                        addImage(url, warning.location);
                    }

                    if (warning.isVariantSpecific()) {
                        writer.write("\n");
                        writer.write("Applies to variants: ");
                        writer.write(Joiner.on(", ").join(warning.getIncludedVariantNames()));
                        writer.write("<br/>\n");
                        writer.write("Does <b>not</b> apply to variants: ");
                        writer.write(Joiner.on(", ").join(warning.getExcludedVariantNames()));
                        writer.write("<br/>\n");
                    }
                }
                if (partialHide) { // Close up the extra div
                    writer.write("</div>\n");
                }

                writer.write("</div>\n");
                writeIssueMetadata(issue, first.severity, null);

                writer.write("</div>\n");
            }

            if (!client.isCheckingSpecificIssues()) {
                writeMissingIssues(missing);
            }

            writeSuppressInfo();
        } else {
            writer.write("Congratulations!");
        }
        writer.write("\n</body>\n</html>");
        writer.close();

        if (!client.getFlags().isQuiet()
                && (stats.errorCount > 0 || stats.warningCount > 0)) {
            String url = SdkUtils.fileToUrlString(output.getAbsoluteFile());
            System.out.println(String.format("Wrote HTML report to %1$s", url));
        }
    }

    private void writeIssueMetadata(Issue issue, Severity severity, String disabledBy)
            throws IOException {
        writer.write("<div class=\"metadata\">");

        if (client.getRegistry() instanceof BuiltinIssueRegistry) {
            if (hasAutoFix(issue)) {
                writer.write("Note: This issue has an associated quickfix operation in "
                        + "Android Studio and IntelliJ");
                if (!INLINE_RESOURCES) {
                    writer.write(getFixIcon());
                } else if (fixUrl != null) {
                    writer.write("&nbsp;<img alt=\"Fix\" border=\"0\" align=\"top\" src=\"");
                    writer.write(fixUrl);
                    writer.write("\" />\n");
                }

                writer.write("<br>\n");
            }
        }

        if (disabledBy != null) {
            writer.write(String.format("Disabled By: %1$s<br/>\n", disabledBy));
        }

        writer.write("Priority: ");
        writer.write(String.format("%1$d / 10", issue.getPriority()));
        writer.write("<br/>\n");
        writer.write("Category: ");
        writer.write(issue.getCategory().getFullName());
        writer.write("</div>\n");

        writer.write("Severity: ");
        if (severity.isError()) {
            writer.write("<span class=\"error\">");
        } else if (severity == Severity.WARNING) {
            writer.write("<span class=\"warning\">");
        } else {
            writer.write("<span>");
        }
        appendEscapedText(severity.getDescription());
        writer.write("</span>");

        writer.write("<div class=\"summary\">\n");
        writer.write("Explanation: ");
        String description = issue.getBriefDescription(HTML);
        writer.write(description);
        if (!description.isEmpty()
                && Character.isLetter(description.charAt(description.length() - 1))) {
            writer.write('.');
        }
        writer.write("</div>\n");
        writer.write("<div class=\"explanation\">\n");
        String explanationHtml = issue.getExplanation(HTML);
        writer.write(explanationHtml);
        writer.write("\n</div>\n");
        List<String> moreInfo = issue.getMoreInfo();
        writer.write("<br/>");
        writer.write("<div class=\"moreinfo\">");
        writer.write("More info: ");
        int count = moreInfo.size();
        if (count > 1) {
            writer.write("<ul>");
        }
        for (String uri : moreInfo) {
            if (count > 1) {
                writer.write("<li>");
            }
            writer.write("<a href=\"");
            writer.write(uri);
            writer.write("\">"    );
            writer.write(uri);
            writer.write("</a>\n");
        }
        if (count > 1) {
            writer.write("</ul>");
        }
        writer.write("</div>");

        writer.write("<br/>");
        writer.write(String.format(
                "To suppress this error, use the issue id \"%1$s\" as explained in the " +
                "%2$sSuppressing Warnings and Errors%3$s section.",
                issue.getId(),
                "<a href=\"#SuppressInfo\">", "</a>"));
        writer.write("<br/>\n");
    }

    private void writeSuppressInfo() throws IOException {
        //getSuppressHelp
        writer.write("\n<a name=\"SuppressInfo\"></a>\n");
        writer.write("<div class=\"category\">");
        writer.write("Suppressing Warnings and Errors");
        writer.write("<div class=\"categorySeparator\"></div>\n");
        writer.write("</div>\n");
        writer.write(TextFormat.RAW.convertTo(Main.getSuppressHelp(), TextFormat.HTML));
        writer.write('\n');
    }

    protected Map<Issue, String> computeMissingIssues(List<Warning> warnings) {
        Set<Project> projects = new HashSet<>();
        Set<Issue> seen = new HashSet<>();
        for (Warning warning : warnings) {
            projects.add(warning.project);
            seen.add(warning.issue);
        }
        Configuration cliConfiguration = client.getConfiguration();
        Map<Issue, String> map = Maps.newHashMap();
        for (Issue issue : client.getRegistry().getIssues()) {
            if (!seen.contains(issue)) {
                if (client.isSuppressed(issue)) {
                    map.put(issue, "Command line flag");
                    continue;
                }

                if (!issue.isEnabledByDefault() && !client.isAllEnabled()) {
                    map.put(issue, "Default");
                    continue;
                }

                if (cliConfiguration != null && !cliConfiguration.isEnabled(issue)) {
                    map.put(issue, "Command line supplied --config lint.xml file");
                    continue;
                }

                // See if any projects disable this warning
                for (Project project : projects) {
                    if (!project.getConfiguration(null).isEnabled(issue)) {
                        map.put(issue, "Project lint.xml file");
                        break;
                    }
                }
            }
        }

        return map;
    }

    private void writeMissingIssues(Map<Issue, String> missing) throws IOException {
        writer.write("\n<a name=\"MissingIssues\"></a>\n");
        writer.write("<div class=\"category\">");
        writer.write("Disabled Checks");
        writer.write("<div class=\"categorySeparator\"></div>\n");
        writer.write("</div>\n");

        writer.write(
                "The following issues were not run by lint, either " +
                "because the check is not enabled by default, or because " +
                "it was disabled with a command line flag or via one or " +
                "more lint.xml configuration files in the project directories.");
        writer.write("\n<br/><br/>\n");

        List<Issue> list = new ArrayList<>(missing.keySet());
        Collections.sort(list);


        for (Issue issue : list) {
            writer.write("<a name=\"" + issue.getId() + "\"></a>\n");
            writer.write("<div class=\"issue\">\n");

            // Explain this issue
            writer.write("<div class=\"id\">");
            writer.write(issue.getId());
            writer.write("<div class=\"issueSeparator\"></div>\n");
            writer.write("</div>\n");
            String disabledBy = missing.get(issue);
            writeIssueMetadata(issue, issue.getDefaultSeverity(), disabledBy);
            writer.write("</div>\n");
        }
    }

    protected void writeStyleSheet() throws IOException {
        if (USE_HOLO_STYLE) {
            writer.write(
                "<link rel=\"stylesheet\" type=\"text/css\" " +
                "href=\"http://fonts.googleapis.com/css?family=Roboto\" />\n" );
        }

        URL cssUrl = HtmlReporter.class.getResource(CSS);
        if (simpleFormat || INLINE_RESOURCES) {
            // Inline the CSS
            writer.write("<style>\n");
            InputStream input = cssUrl.openStream();
            byte[] bytes = ByteStreams.toByteArray(input);
            try {
                Closeables.close(input, true /* swallowIOException */);
            } catch (IOException e) {
                // cannot happen
            }
            String css = new String(bytes, Charsets.UTF_8);
            writer.write(css);
            writer.write("</style>\n");
        } else {
            String ref = addLocalResources(cssUrl);
            if (ref != null) {
                writer.write(
                "<link rel=\"stylesheet\" type=\"text/css\" href=\""
                            + ref + "\" />\n");
            }
        }
    }

    private void writeOverview(List<List<Warning>> related, int missingCount)
            throws IOException {
        // Write issue id summary
        writer.write("<table class=\"overview\">\n");

        String errorUrl = null;
        String warningUrl = null;
        if (!INLINE_RESOURCES && !simpleFormat) {
            errorUrl = addLocalResources(getErrorIconUrl());
            warningUrl = addLocalResources(getWarningIconUrl());
            fixUrl = addLocalResources(HtmlReporter.class.getResource("lint-run.png"));
        }

        Category previousCategory = null;
        for (List<Warning> warnings : related) {
            Issue issue = warnings.get(0).issue;

            boolean isError = false;
            for (Warning warning : warnings) {
                if (warning.severity.isError()) {
                    isError = true;
                    break;
                }
            }

            if (issue.getCategory() != previousCategory) {
                writer.write("<tr><td></td><td class=\"categoryColumn\">");
                previousCategory = issue.getCategory();
                String categoryName = issue.getCategory().getFullName();
                writer.write("<a href=\"#");
                writer.write(categoryName);
                writer.write("\">");
                writer.write(categoryName);
                writer.write("</a>\n");
                writer.write("</td></tr>");
                writer.write("\n");
            }
            writer.write("<tr>\n");

            // Count column
            writer.write("<td class=\"countColumn\">");
            writer.write(Integer.toString(warnings.size()));
            writer.write("</td>");

            writer.write("<td class=\"issueColumn\">");

            if (INLINE_RESOURCES) {
                String markup = isError ? getErrorIcon() : getWarningIcon();
                writer.write(markup);
                writer.write('\n');
            } else {
                String imageUrl = isError ? errorUrl : warningUrl;
                if (imageUrl != null) {
                    writer.write("<img border=\"0\" align=\"top\" src=\"");
                    writer.write(imageUrl);
                    writer.write("\" alt=\"");
                    writer.write(isError ? "Error" : "Warning");
                    writer.write("\" />\n");
                }
            }

            writer.write("<a href=\"#");
            writer.write(issue.getId());
            writer.write("\">");
            writer.write(issue.getId());
            writer.write(": ");
            writer.write(issue.getBriefDescription(HTML));
            writer.write("</a>\n");

            writer.write("</td></tr>\n");
        }

        if (missingCount > 0 && !client.isCheckingSpecificIssues()) {
            writer.write("<tr><td></td>");
            writer.write("<td class=\"categoryColumn\">");
            writer.write("<a href=\"#MissingIssues\">");
            writer.write(String.format("Disabled Checks (%1$d)",
                    missingCount));

            writer.write("</a>\n");
            writer.write("</td></tr>");
        }

        writer.write("</table>\n");
        writer.write("<br/>");
    }

    private String writeLocation(File file, String path, int line) throws IOException {
        String url;
        writer.write("<span class=\"location\">");

        url = getUrl(file);
        if (url != null) {
            writer.write("<a href=\"");
            writer.write(url);
            writer.write("\">");
        }

        String displayPath = stripPath(path);
        if (url != null && url.startsWith("../") && new File(displayPath).isAbsolute()) {
            displayPath = url;
        }
        writer.write(displayPath);
        //noinspection VariableNotUsedInsideIf
        if (url != null) {
            writer.write("</a>");
        }
        if (line >= 0) {
            // 0-based line numbers, but display 1-based
            writer.write(':');
            writer.write(Integer.toString(line + 1));
        }
        writer.write("</span>");
        return url;
    }

    private boolean addImage(String url, Location location) throws IOException {
        if (url != null && endsWith(url, DOT_PNG)) {
            if (location.getSecondary() != null) {
                // Emit many images
                // Add in linked images as well
                List<String> urls = new ArrayList<>();
                while (location != null && location.getFile() != null) {
                    String imageUrl = getUrl(location.getFile());
                    if (imageUrl != null
                            && endsWith(imageUrl, DOT_PNG)) {
                        urls.add(imageUrl);
                    }
                    location = location.getSecondary();
                }
                if (!urls.isEmpty()) {
                    // Sort in order
                    Collections.sort(urls, (s1, s2) -> getDpiRank(s1) - getDpiRank(s2));
                    writer.write("<table>");
                    writer.write("<tr>");
                    for (String linkedUrl : urls) {
                        // Image series: align top
                        writer.write("<td>");
                        writer.write("<a href=\"");
                        writer.write(linkedUrl);
                        writer.write("\">");
                        writer.write("<img border=\"0\" align=\"top\" src=\"");
                        writer.write(linkedUrl);
                        writer.write("\" /></a>\n");
                        writer.write("</td>");
                    }
                    writer.write("</tr>");

                    writer.write("<tr>");
                    for (String linkedUrl : urls) {
                        writer.write("<th>");
                        int index = linkedUrl.lastIndexOf("drawable-");
                        if (index != -1) {
                            index += "drawable-".length();
                            int end = linkedUrl.indexOf('/', index);
                            if (end != -1) {
                                writer.write(linkedUrl.substring(index, end));
                            }
                        }
                        writer.write("</th>");
                    }
                    writer.write("</tr>\n");

                    writer.write("</table>\n");
                }
            } else {
                // Just this image: float to the right
                writer.write("<img class=\"embedimage\" align=\"right\" src=\"");
                writer.write(url);
                writer.write("\" />");
            }

            return true;
        }

        return false;
    }

    /** Provide a sorting rank for a url */
    private static int getDpiRank(String url) {
        if (url.contains("-xhdpi")) {
            return 0;
        } else if (url.contains("-hdpi")) {
            return 1;
        } else if (url.contains("-mdpi")) {
            return 2;
        } else if (url.contains("-ldpi")) {
            return 3;
        } else {
            return 4;
        }
    }

    private void appendCodeBlock(CharSequence contents, int lineno, int offset)
            throws IOException {
        int max = lineno + 3;
        int min = lineno - 3;
        for (int l = min; l < max; l++) {
            if (l >= 0) {
                int lineOffset = LintCliClient.getLineOffset(contents, l);
                if (lineOffset == -1) {
                    break;
                }

                writer.write(String.format("<span class=\"lineno\">%1$4d</span> ", (l + 1)));

                String line = LintCliClient.getLineOfOffset(contents, lineOffset);
                if (offset != -1 && lineOffset <= offset && lineOffset+line.length() >= offset) {
                    // Text nodes do not always have correct lines/offsets
                    //assert l == lineno;

                    // This line contains the beginning of the offset
                    // First print everything before
                    int delta = offset - lineOffset;
                    appendEscapedText(line.substring(0, delta));
                    writer.write("<span class=\"errorspan\">");
                    appendEscapedText(line.substring(delta));
                    writer.write("</span>");
                } else if (offset == -1 && l == lineno) {
                    writer.write("<span class=\"errorline\">");
                    appendEscapedText(line);
                    writer.write("</span>");
                } else {
                    appendEscapedText(line);
                }
                if (l < max - 1) {
                    writer.write("\n");
                }
            }
        }
    }

    @Override
    public void writeProjectList(@NonNull Stats stats,
            @NonNull List<MultiProjectHtmlReporter.ProjectEntry> projects) throws IOException {
        writer.write(
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
                        "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                        "<head>\n" +
                        "<title>" + title + "</title>\n");
        writeStyleSheet();
        writer.write(
                "</head>\n" +
                        "<body>\n" +
                        "<h1>" +
                        title +
                        "</h1>\n" +
                        "<div class=\"titleSeparator\"></div>\n");


        writer.write(String.format("Check performed at %1$s.",
                new Date().toString()));
        writer.write("<br/>\n");
        appendEscapedText(String.format("%1$s found",
                describeCounts(stats.errorCount, stats.warningCount, false, true)));
        if (stats.baselineErrorCount > 0 || stats.baselineWarningCount > 0) {
            File baselineFile = flags.getBaselineFile();
            assert baselineFile != null;
            appendEscapedText(String.format(" (%1$ss filtered by "
                            + "baseline %2$s)",
                    describeCounts(stats.baselineErrorCount, stats.baselineWarningCount, false,
                            true),
                    baselineFile.getName()));
        }
        writer.write(":\n<br/><br/>\n");

        if (stats.errorCount == 0 && stats.warningCount == 0) {
            writer.write("Congratulations!");
            return;
        }

        String errorUrl = null;
        String warningUrl = null;
        if (!INLINE_RESOURCES && !simpleFormat) {
            errorUrl = addLocalResources(HtmlReporter.getErrorIconUrl());
            warningUrl = addLocalResources(HtmlReporter.getWarningIconUrl());
        }

        writer.write("<table class=\"overview\">\n");
        writer.write("<tr><th>");
        writer.write("Project");
        writer.write("</th><th class=\"countColumn\">");

        if (INLINE_RESOURCES) {
            String markup = getErrorIcon();
            writer.write(markup);
            writer.write('\n');
        } else {
            if (errorUrl != null) {
                writer.write("<img border=\"0\" align=\"top\" src=\"");
                writer.write(errorUrl);
                writer.write("\" alt=\"Error\" />\n");
            }
        }
        writer.write("Errors");
        writer.write("</th><th class=\"countColumn\">");

        if (INLINE_RESOURCES) {
            String markup = getWarningIcon();
            writer.write(markup);
            writer.write('\n');
        } else {
            if (warningUrl != null) {
                writer.write("<img border=\"0\" align=\"top\" src=\"");
                writer.write(warningUrl);
                writer.write("\" alt=\"Warning\" />\n");
            }
        }
        writer.write("Warnings");
        writer.write("</th></tr>\n");

        for (MultiProjectHtmlReporter.ProjectEntry entry : projects) {
            writer.write("<tr><td>");
            writer.write("<a href=\"");
            appendEscapedText(entry.fileName);
            writer.write("\">");
            writer.write(entry.path);
            writer.write("</a></td><td class=\"countColumn\">");
            writer.write(Integer.toString(entry.errorCount));
            writer.write("</td><td class=\"countColumn\">");
            writer.write(Integer.toString(entry.warningCount));
            writer.write("</td></tr>\n");
        }
        writer.write("</table>\n");

        writer.write("</body>\n</html>\n");
    }

    protected void appendEscapedText(String textValue) throws IOException {
        for (int i = 0, n = textValue.length(); i < n; i++) {
            char c = textValue.charAt(i);
            if (c == '<') {
                writer.write("&lt;");
            } else if (c == '&') {
                writer.write("&amp;");
            } else if (c == '\n') {
                writer.write("<br/>\n");
            } else {
                if (c > 255) {
                    writer.write("&#");
                    writer.write(Integer.toString(c));
                    writer.write(';');
                } else {
                    writer.write(c);
                }
            }
        }
    }

    static URL getWarningIconUrl() {
        return HtmlReporter.class.getResource("lint-warning.png");
    }

    static URL getErrorIconUrl() {
        return HtmlReporter.class.getResource("lint-error.png");
    }

    static String getErrorIcon() {
        return "<img border=\"0\" align=\"top\" width=\"15\" height=\"15\" alt=\"Error\" src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA8AAAAPCAYAAAA71pVKAAAB00lEQVR42nWTS0hbQRiF587MzkUXooi6UHAjhNKNSkGhCDXxkZhUIwWhBLoRsQpi3bXmIboSV2aliI+WKqLUtqsuSxclrhRBUMnVmIpa2oIkQon+zhlr9F7jwOEw/znfLO6dYcy2Arys6AUv6x7klTNh4ViFY485u2+N8Uc8yB1DH0Vt6ki2UkZ20LkS/Eh6CXPk6FnAKVHNJ3nViind9E/6tTKto3TxaU379Qw5euhn4QXxOGzKFjqT7Vmlwx8IC357jh76GvzC64pj4mn6VLbRbf0Nvdcw3J6hr7gS9o3XDxwIN/0RPot+h95pGG7P0AfH1oVz6UR4ya5foXkNw3Pl4Ngub/p6yD1k13FoTsPwXDk4ti89SwnuJrtigYiGY4FhypWDY2aeb0CJ4rzZou9GPc0Y1drtGfrgWLzweUm8uPNsx2ikrHgjHT6LUOrzD/rpDpIlU0JfcaX6d8UfdoW38/20ZbiuxF10MHL1tRNvp2/mSuihn70kZl2/MJ+8Xtkq8NOm4VRqoIUKLy0Hx2mx3PN/5iTk6KFvuaJmyxux3zE8tFPTm9p84KMNdcAGa9COvZqnkaN37wNJvpooSvZFexIvx2b3OkdX4dgne6N3XtUl5wqoyBY2uZQAAAAASUVORK5CYII=\" />";
    }

    static String getWarningIcon() {
        return "<img border=\"0\" align=\"top\" width=\"16\" height=\"15\" alt=\"Warning\" src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAPCAQAAABHeoekAAAA3klEQVR42nWPsWoCQRCGVyJiF9tAsNImbcDKR/ABBEurYCsBsfQRQiAPYGPyAAnYWQULS9MErNU2Vsr/ObMX7g6O+xd2/5n5dmY3hFQEBVVpuCsVT/yoUl6u4XotBz4E4qR2YYyH6ugEWY8comR/t+tvPPJtSLPYvhvvTswtbdCmCOwjMHXAzjP9kB/ByB7nejbgy43WVPF3WNG+p9+kzkozdhGAQdZh7BlHdGTL3z98pp6Um7okKdvHNuIzWk+9xN+yINOcHps0OnAfuOOoHJH3pmHghhYP2VJcaXx7BaKz9YB2HVrDAAAAAElFTkSuQmCC\"/>";
    }

    static String getFixIcon() {
        return "<img border=\"0\" align=\"top\" width=\"16\" height=\"16\" alt=\"Fix\" src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAA1ElEQVR42s2Szw7BQBCH+wTezAM4u7h7BBKEa3v1AK7iKvoAuDg5uSoaqTR2u90xs9P1J2FLHNjky29mu/Nt09Tz/mb1gxF8wlPBKoG3cAoiCbAVTCQe69bcN8+dgp1k9oTgpL4+bYIXVKCNEqfgIJk4w0RirGmIhklCe07BeBPCEQ9ZOsUwpd17KRiuQ3O4u/DhpMDkfU8kquQNesVQdVIzSX2KQ2l+wykQeKAx4w9GSf05532LU5BpZrD0rzUhLVAiwAtAaYbqXDPKpkvw1a/8s3UBSc/bWGUWa6wAAAAASUVORK5CYII=\"/>";
    }
}
