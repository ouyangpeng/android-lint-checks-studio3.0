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

package com.android.tools.lint.checks

import com.android.resources.ResourceFolderType
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.DefaultPosition
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.ResourceXmlDetector
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import org.w3c.dom.Document
import org.w3c.dom.Node

/**
 * Check which looks for invalid resources. Aapt already performs some validation,
 * such as making sure that resource references point to resources that exist, but this
 * detector looks for additional issues.
 */
/** Constructs a new detector  */
class ExtraTextDetector : ResourceXmlDetector() {
    companion object Issues {

        /** The main issue discovered by this detector  */
        @JvmField val ISSUE = Issue.create(
                "ExtraText",
                "Extraneous text in resource files",

                """
Layout resource files should only contain elements and attributes. Any XML text content found \
in the file is likely accidental (and potentially dangerous if the text resembles XML and the \
developer believes the text to be functional)""",
                Category.CORRECTNESS,
                3,
                Severity.WARNING,
                Implementation(
                    ExtraTextDetector::class.java,
                    Scope.MANIFEST_AND_RESOURCE_SCOPE,
                    Scope.RESOURCE_FILE_SCOPE,
                    Scope.MANIFEST_SCOPE)
        )
    }

    private var foundText: Boolean = false

    override fun appliesTo(folderType: ResourceFolderType): Boolean =
            folderType == ResourceFolderType.LAYOUT
                    || folderType == ResourceFolderType.MENU
                    || folderType == ResourceFolderType.ANIM
                    || folderType == ResourceFolderType.ANIMATOR
                    || folderType == ResourceFolderType.DRAWABLE
                    || folderType == ResourceFolderType.COLOR

    override fun visitDocument(context: XmlContext, document: Document) {
        foundText = false
        visitNode(context, document)
    }

    private fun visitNode(context: XmlContext, node: Node) {
        val nodeType = node.nodeType
        if (nodeType == Node.TEXT_NODE && !foundText) {
            val text = node.nodeValue
            var i = 0
            val n = text.length
            while (i < n) {
                val c = text[i]
                if (!Character.isWhitespace(c)) {
                    var snippet = text.trim { it <= ' ' }
                    val maxLength = 100
                    if (snippet.length > maxLength) {
                        snippet = snippet.substring(0, maxLength) + "..."
                    }
                    var location = context.getLocation(node)
                    if (i > 0) {
                        // Adjust the error position to point to the beginning of
                        // the text rather than the beginning of the text node
                        // (which is often the newline at the end of the previous
                        // line and the indentation)
                        var start = location.start
                        if (start != null) {
                            var line = start.line
                            var column = start.column
                            var offset = start.offset

                            for (j in 0 until i) {
                                offset++

                                if (text[j] == '\n') {
                                    if (line != -1) {
                                        line++
                                    }
                                    if (column != -1) {
                                        column = 0
                                    }
                                } else if (column != -1) {
                                    column++
                                }
                            }

                            start = DefaultPosition(line, column, offset)
                            location = Location.create(context.file, start, location.end)
                        }
                    }
                    context.report(ISSUE, node, location,
                            "Unexpected text found in layout file: \"$snippet\"")
                    foundText = true
                    break
                }
                i++
            }
        }

        // Visit children
        val childNodes = node.childNodes
        var i = 0
        val n = childNodes.length
        while (i < n) {
            val child = childNodes.item(i)
            visitNode(context, child)
            i++
        }
    }
}
