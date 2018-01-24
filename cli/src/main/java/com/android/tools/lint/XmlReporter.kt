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

package com.android.tools.lint

import com.android.tools.lint.checks.BuiltinIssueRegistry
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.TextFormat.RAW
import com.android.utils.SdkUtils
import com.android.utils.XmlUtils
import com.google.common.annotations.Beta
import com.google.common.base.Charsets
import com.google.common.base.Joiner
import com.google.common.io.Files
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.Writer

/**
 * A reporter which emits lint results into an XML report.
 *
 *
 * **NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.**
 */
@Beta
class XmlReporter
/**
 * Constructs a new [XmlReporter]
 *
 * @param client the client
 *
 * @param output the output file
 *
 * @throws IOException if an error occurs
 */
@Throws(IOException::class)
constructor(client: LintCliClient, output: File) : Reporter(client, output) {
    private val writer: Writer = BufferedWriter(Files.newWriter(output, Charsets.UTF_8))
    var isIntendedForBaseline: Boolean = false

    @Throws(IOException::class)
    override fun write(stats: Reporter.Stats, issues: List<Warning>) {
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        // Format 4: added urls= attribute with all more info links, comma separated
        writer.write("<issues format=\"4\"")
        val revision = client.getClientRevision()
        if (revision != null) {
            writer.write(String.format(" by=\"lint %1\$s\"", revision))
        }
        writer.write(">\n")

        if (!issues.isEmpty()) {
            writeIssues(issues)
        }

        writer.write("\n</issues>\n")
        writer.close()

        if (!client.getFlags().isQuiet && (stats.errorCount > 0 || stats.warningCount > 0)) {
            val url = SdkUtils.fileToUrlString(output.absoluteFile)
            println(String.format("Wrote XML report to %1\$s", url))
        }
    }

    private fun writeIssues(issues: List<Warning>) {
        for (warning in issues) {
            writeIssue(warning)
        }
    }

    private fun writeIssue(warning: Warning) {
        writer.write('\n'.toInt())
        indent(writer, 1)
        writer.write("<issue")
        val issue = warning.issue
        writeAttribute(writer, 2, "id", issue.id)
        if (!isIntendedForBaseline) {
            writeAttribute(writer, 2, "severity",
                    warning.severity.description)
        }
        writeAttribute(writer, 2, "message", warning.message)

        if (!isIntendedForBaseline) {
            writeAttribute(writer, 2, "category", issue.category.fullName)
            writeAttribute(writer, 2, "priority", Integer.toString(issue.priority))
            // We don't need issue metadata in baselines
            writeAttribute(writer, 2, "summary", issue.getBriefDescription(RAW))
            writeAttribute(writer, 2, "explanation", issue.getExplanation(RAW))

            val moreInfo = issue.moreInfo
            if (!moreInfo.isEmpty()) {
                // Compatibility with old format: list first URL
                writeAttribute(writer, 2, "url", moreInfo[0])
                writeAttribute(writer, 2, "urls", Joiner.on(',').join(issue.moreInfo))
            }
        }
        if (warning.errorLine != null && !warning.errorLine.isEmpty()) {
            val line = warning.errorLine
            val index1 = line.indexOf('\n')
            if (index1 != -1) {
                val index2 = line.indexOf('\n', index1 + 1)
                if (index2 != -1) {
                    val line1 = line.substring(0, index1)
                    val line2 = line.substring(index1 + 1, index2)
                    writeAttribute(writer, 2, "errorLine1", line1)
                    writeAttribute(writer, 2, "errorLine2", line2)
                }
            }
        }

        if (warning.isVariantSpecific) {
            writeAttribute(writer, 2, "includedVariants", Joiner.on(',').join(warning.includedVariantNames))
            writeAttribute(writer, 2, "excludedVariants", Joiner.on(',').join(warning.excludedVariantNames))
        }

        if (!isIntendedForBaseline && client.getRegistry() is BuiltinIssueRegistry) {
            if (hasAutoFix(issue)) {
                writeAttribute(writer, 2, "quickfix", "studio")
            }
        }

        assert(warning.file != null == (warning.location != null))

        if (warning.file != null) {
            assert(warning.location.file === warning.file)
        }

        var location: Location? = warning.location
        if (location != null) {
            writer.write(">\n")
            while (location != null) {
                indent(writer, 2)
                writer.write("<location")
                val path = LintCliClient.getDisplayPath(warning.project,
                        location.file,
                        // Don't use absolute paths in baseline files
                        client.flags.isFullPath && !isIntendedForBaseline)
                writeAttribute(writer, 3, "file", path)
                val start = location.start
                if (start != null) {
                    val line = start.line
                    val column = start.column
                    if (line >= 0) {
                        // +1: Line numbers internally are 0-based, report should be
                        // 1-based.
                        writeAttribute(writer, 3, "line", Integer.toString(line + 1))
                        if (column >= 0) {
                            writeAttribute(writer, 3, "column", Integer.toString(column + 1))
                        }
                    }
                }

                writer.write("/>\n")
                location = location.secondary
            }
            indent(writer, 1)
            writer.write("</issue>\n")
        } else {
            writer.write('\n'.toInt())
            indent(writer, 1)
            writer.write("/>\n")
        }
    }

    @Throws(IOException::class)
    private fun writeAttribute(writer: Writer, indent: Int, name: String, value: String) {
        writer.write('\n'.toInt())
        indent(writer, indent)
        writer.write(name)
        writer.write('='.toInt())
        writer.write('"'.toInt())
        writer.write(XmlUtils.toXmlAttributeValue(value))
        writer.write('"'.toInt())
    }

    @Throws(IOException::class)
    private fun indent(writer: Writer, indent: Int) {
        for (level in 0 until indent) {
            writer.write("    ")
        }
    }
}