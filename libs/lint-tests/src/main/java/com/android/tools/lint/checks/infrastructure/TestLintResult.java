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

import static java.util.regex.Pattern.MULTILINE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.LintCoreApplicationEnvironment;
import com.android.tools.lint.Warning;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Position;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.TextFormat;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.util.ArrayUtil;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import org.intellij.lang.annotations.Language;

/**
 * The result of running a {@link TestLintTask}.
 *
 * <p><b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@SuppressWarnings({"UnusedReturnValue", "SameParameterValue"}) // Allow chaining of these report checks
public class TestLintResult {
    private final String output;
    private final Exception exception;
    private final TestLintTask task;
    private final List<Warning> warnings;
    private int maxLineLength;

    TestLintResult(@NonNull TestLintTask task, @Nullable String output,
            @Nullable Exception e, @NonNull List<Warning> warnings) {
        this.task = task;
        this.output = output;
        this.exception = e;
        this.warnings = warnings;
    }

    /**
     * Sets the maximum line length in the report. This is useful if some lines are
     * particularly long and you don't care about the details at the end of the line
     *
     * @param maxLineLength the maximum number of characters to show in the report
     * @return this
     */
    @SuppressWarnings("SameParameterValue") // Lint internally always using 100, but allow others
    @NonNull
    public TestLintResult maxLineLength(int maxLineLength) {
        this.maxLineLength = maxLineLength;
        return this;
    }

    /**
     * Checks that the lint result had the expected report format.
     *
     * @param expectedText the text to expect
     * @return this
     */
    public TestLintResult expect(@NonNull String expectedText) {
        String actual = describeOutput();
        if (!actual.trim().equals((expectedText.trim()))) {
            // See if it's a Windows path issue
            if (actual.equals(expectedText.replace(File.separatorChar, '/'))) {
                assertEquals("The expected lint output does not match, but it *does* "
                        + "match when Windows file separators (\\) are replaced by Unix ones.\n"
                        + "Make sure your lint detector calls LintClient.getDisplayPath(File) "
                        + "instead of displaying paths directly (in unit tests they will then "
                        + "be converted to forward slashes for test output stability.)\n",
                        expectedText, actual);
            }

            assertEquals(expectedText, actual);
        }
        cleanup();
        return this;
    }

    private static final String TRUNCATION_MARKER = "\u2026";

    private String describeOutput() {
        String output = this.output;
        if (output == null) {
            output = "";
        } else if (maxLineLength > TRUNCATION_MARKER.length()) {
            StringBuilder sb = new StringBuilder();
            for (String line : Splitter.on('\n').split(output)) {
                if (line.length() > maxLineLength) {
                    line = line.substring(0, maxLineLength - TRUNCATION_MARKER.length()) +
                            TRUNCATION_MARKER;
                }
                sb.append(line).append('\n');
            }
            output = sb.toString();
            if (output.endsWith("\n\n") && !this.output.endsWith("\n\n")) {
                output = output.substring(0, output.length() - 1);
            }
        }

        if (exception != null) {
            StringWriter writer = new StringWriter();
            exception.printStackTrace(new PrintWriter(writer));

            if (!output.isEmpty()) {
                writer.write(output);
            }

            return writer.toString();
        } else {
            return output;
        }
    }

    /**
     * Checks that there were no errors or exceptions.
     *
     * @return this
     */
    public TestLintResult expectClean() {
        expect("No warnings.");
        cleanup();
        return this;
    }

    /**
     * Checks that the results correspond to the messages inlined in the source files
     *
     * @return this
     */
    public TestLintResult expectInlinedMessages() {
        for (ProjectDescription project : task.projects) {
            for (TestFile file : project.getFiles()) {
                String plainContents;
                String contents;
                try {
                    plainContents = file.getContents();
                    contents = file.getRawContents();
                    if (contents == null || plainContents == null) {
                        continue;
                    }
                } catch (Throwable ignore) {
                    continue;
                }

                String targetPath = file.getTargetPath();
                boolean isXml = targetPath.endsWith(SdkConstants.DOT_XML);

                try {
                    // TODO: Make comment token warnings depend on the source language
                    // For now, only handling Java

                    // We'll perform this check by going through all the files
                    // in the project, removing any inlined error messages in the file,
                    // then inserting error messages from the lint check, then asserting
                    // that the original file (with inlined error messages) is identical
                    // to the annotated file.

                    // We'll use a Swing document such that we can remove error comment
                    // ranges from the doc, and use the Position class to easily map
                    // offsets in reported errors to the corresponding location in the
                    // document after those edits have been made.

                    Document doc = new PlainDocument();
                    doc.insertString(0, isXml ? plainContents : contents, null);
                    Map<Integer, javax.swing.text.Position> positionMap = Maps.newHashMap();

                    // Find any errors reported in this document
                    List<Warning> matches = findWarnings(targetPath);

                    for (Warning warning : matches) {
                        Location location = warning.location;
                        Position start = location.getStart();
                        Position end = location.getEnd();

                        int startOffset = start != null ? start.getOffset() : 0;
                        int endOffset = end != null ? end.getOffset() : 0;

                        javax.swing.text.Position startPos = doc.createPosition(startOffset);
                        javax.swing.text.Position endPos = doc.createPosition(endOffset);

                        positionMap.put(startOffset, startPos);
                        positionMap.put(endOffset, endPos);
                    }

                    // Next remove any error regions from the document
                    stripMarkers(isXml, doc, contents);

                    // Finally write the expected errors back in
                    for (Warning warning : matches) {
                        Location location = warning.location;

                        Position start = location.getStart();
                        Position end = location.getEnd();

                        int startOffset = start != null ? start.getOffset() : 0;
                        int endOffset = end != null ? end.getOffset() : 0;

                        javax.swing.text.Position startPos = positionMap.get(startOffset);
                        javax.swing.text.Position endPos = positionMap.get(endOffset);

                        assertNotNull(startPos);
                        assertNotNull(endPos);

                        String startMarker;
                        String endMarker;
                        String message = warning.message;

                        // Use plain ascii in the test golden files for now. (This also ensures
                        // that the markup is well-formed, e.g. if we have a ` without a matching
                        // closing `, the ` would show up in the plain text.)
                        message = TextFormat.RAW.convertTo(message, TextFormat.TEXT);

                        if (isXml) {
                            String tag = warning.severity.getDescription().toLowerCase(Locale.ROOT);
                            startMarker = "<?" + tag + " message=\""
                                    + message + "\"?>";
                            endMarker = "<?" + tag + "?>";
                        } else {
                            // Java, Gradle, Kotlin, ...
                            startMarker = "/*" + message + "*/";
                            endMarker = "/**/";
                        }

                        startOffset = startPos.getOffset();
                        endOffset = endPos.getOffset();

                        doc.insertString(endOffset, endMarker, null);
                        doc.insertString(startOffset, startMarker, null);
                    }

                    // Assert equality
                    assertEquals(contents, doc.getText(0, doc.getLength()));
                } catch (BadLocationException ignore) {
                }
            }
        }

        cleanup();
        return this;
    }

    private static void stripMarkers(boolean isXml, Document doc, String contents)
            throws BadLocationException {

        if (isXml) {
            // For processing instructions just remove all non-XML processing instructions
            // (we don't need to match beginning and ending ones)
            int index = contents.length();
            while (index >= 0) {
                int endEndOffset = contents.lastIndexOf("?>", index);
                if (endEndOffset == -1) {
                    break;
                }
                int endStartOffset = contents.lastIndexOf("<?", endEndOffset);
                if (endStartOffset == -1) {
                    break;
                }
                if (contents.startsWith("<?xml", endStartOffset)) {
                    index = endStartOffset - 1;
                    continue;
                }

                doc.remove(endStartOffset, endEndOffset + "?>".length() - endStartOffset);

                index = endStartOffset;
            }
        } else {
            // For Java/Groovy/Kotlin etc we don't want to remove *all* block comments;
            // only those that end with /**/. Note that this may not handle nested
            // ones correctly.
            int index = contents.length();
            while (index >= 0) {
                int endOffset = contents.lastIndexOf("/**/", index);
                if (endOffset == -1) {
                    break;
                }
                int regionStart = contents.lastIndexOf("/*", endOffset - 1);
                if (regionStart == -1) {
                    break;
                }
                int commentEnd = contents.indexOf("*/", regionStart + 2);
                if (commentEnd == -1 || commentEnd > endOffset) {
                    break;
                }

                doc.remove(endOffset, 4);
                doc.remove(regionStart, commentEnd + 2 - regionStart);

                index = regionStart;
            }
        }
    }

    @NonNull
    private List<Warning> findWarnings(@NonNull String targetFile) {
        // The target file should be platform neutral (/, not \ as separator)
        assertTrue(targetFile, !targetFile.contains("\\"));

        // Find any errors reported in this document
        List<Warning> matches = Lists.newArrayList();
        for (Warning warning : warnings) {
            String path = warning.file.getPath().replace(File.separatorChar, '/');
            if (path.endsWith(targetFile)) {
                matches.add(warning);
            }
        }

        // Sort by descending offset
        matches.sort((o1, o2) -> o2.offset - o1.offset);

        return matches;
    }

    /**
     * Checks that the lint report matches the given regular expression
     *
     * @param regexp the regular expression to match the input with (note that it's using {@link
     *               Matcher#find()}, not {@link Matcher#match(int, int)}, so you don't have to
     *               include wildcards at the beginning or end if looking for a match inside the
     *               report
     * @return this
     */
    public TestLintResult expectMatches(@Language("RegExp") @NonNull String regexp) {
        String output = describeOutput();
        Pattern pattern = Pattern.compile(regexp, MULTILINE);
        boolean found = pattern.matcher(output).find();
        if (!found) {
            int reached = computeSubstringMatch(pattern, output);
            fail("Did not find pattern\n  " + regexp + "\n in \n" + output + "; "
                    + "the incomplete match was " + output.substring(0, reached));
        }

        cleanup();
        return this;
    }

    /**
     * Checks the output using the given custom checker, which should throw an
     * {@link AssertionError} if the result is not as expected.
     *
     * @param checker the checker to apply, typically a lambda
     * @return this
     */
    public TestLintResult check(@NonNull ResultChecker checker) {
        checker.check(describeOutput());
        cleanup();
        return this;
    }

    /** Interface implemented by result checkers. */
    public interface ResultChecker {
        /**
         * Checks that the result is as expected; should throw {@link AssertionError} if the output
         * is not as expected
         *
         * @param output the output from lint
         */
        void check(@NonNull String output);
    }

    private static int computeSubstringMatch(@NonNull Pattern pattern, @NonNull String output) {
        for (int i = output.length() - 1; i > 0; i--) {
            String partial = output.substring(0, i);
            Matcher matcher = pattern.matcher(partial);
            if (!matcher.matches() && matcher.hitEnd()) {
                return i;
            }
        }

        return 0;
    }
    /**
     * Checks that the actual number of errors in this lint check matches exactly the given
     * count
     *
     * @param expectedCount the expected error count
     * @return this
     */
    public TestLintResult expectWarningCount(int expectedCount) {
        return expectCount(expectedCount, Severity.WARNING);
    }

    /**
     * Checks that the actual number of errors in this lint check matches exactly the given
     * count
     *
     * @param expectedCount the expected error count
     * @return this
     */
    public TestLintResult expectErrorCount(int expectedCount) {
        return expectCount(expectedCount, Severity.ERROR, Severity.FATAL);
    }

    /**
     * Checks that the actual number of problems with a given severity in this lint check
     * matches exactly the given count.
     *
     * @param expectedCount the expected count
     * @param severities    the severities to count
     * @return this
     */
    @NonNull
    public TestLintResult expectCount(int expectedCount, Severity... severities) {
        int count = 0;
        for (Warning warning : warnings) {
            if (ArrayUtil.contains(warning.severity, severities)) {
                count++;
            }
        }

        if (count != expectedCount) {
            assertEquals("Expected " + expectedCount + " problems with severity "
                            + Joiner.on(" or ").join(severities) + " but was " + count,
                    expectedCount, count);
        }

        return this;
    }

    /** Verify quick fixes */
    public LintFixVerifier verifyFixes() {
        return new LintFixVerifier(task, warnings);
    }

    /**
     * Checks what happens with the given fix in this result as applied to the given
     * test file, and making sure that the result is the new contents
     *
     * @param fix   the fix description, or null to pick the first one
     * @param after the file after applying the fix
     * @return this
     */
    public TestLintResult checkFix(@Nullable String fix, @NonNull TestFile after) {
        verifyFixes().checkFix(fix, after);
        return this;
    }

    /**
     * Applies the fixes and provides diffs to all the files.
     * Convenience wrapper around {@link #verifyFixes()} and
     * {@link LintFixVerifier#expectFixDiffs(String)} if you don't want to configure
     * any diff options.
     *
     * @param expected the diff description resulting from applying the diffs
     * @return this
     */
    public TestLintResult expectFixDiffs(@NonNull String expected) {
        verifyFixes().expectFixDiffs(expected);
        return this;
    }

    @SuppressWarnings("MethodMayBeStatic")
    private void cleanup() {
        LintCoreApplicationEnvironment.disposeApplicationEnvironment();
    }
}
