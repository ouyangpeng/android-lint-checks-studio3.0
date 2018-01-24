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

package com.android.tools.lint.detector.api

import com.android.resources.ResourceFolderType
import com.android.tools.lint.client.api.LintDriver
import com.android.tools.lint.client.api.XmlParser
import com.google.common.annotations.Beta
import org.w3c.dom.Attr
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.File

internal const val SUPPRESS_XML_COMMENT_PREFIX = "<!--suppress "
internal const val SUPPRESS_JAVA_COMMENT_PREFIX = "//noinspection "

/**
 * A [Context] used when checking XML files.
 *
 * **NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.**
 */
@Beta
open class XmlContext
/**
 * Construct a new [XmlContext]
 */
(
        /** the driver running through the checks */
        driver: LintDriver,

        /** the project containing the file being checked */
        project: Project,

        /**
         * The "main" project. For normal projects, this is the same as [.project],
         * but for library projects, it's the root project that includes (possibly indirectly)
         * the various library projects and their library projects.
         *
         * Note that this is a property on the [Context], not the
         * [Project], since a library project can be included from multiple
         * different top level projects, so there isn't **one** main project,
         * just one per main project being analyzed with its library projects.
         */
        main: Project?,
        /** the file being checked */
        file: File,
        /** the [ResourceFolderType] of this file, if any */
        folderType: ResourceFolderType?,
        /** The XML parser  */
        val parser: XmlParser,

        /** The XML contents of the file */
        contents: String,

        /** The XML document */
        @JvmField // backwards compatibility
        val document: Document
) : ResourceContext(driver, project, main, file, folderType, contents) {

    /**
     * Returns the location for the given node, which may be an element or an attribute.
     *
     * @param node the node to look up the location for
     *
     * @return the location for the node
     */
    fun getLocation(node: Node): Location = parser.getLocation(this, node)

    /**
     * Returns the location for name-portion of the given element or attribute.
     *
     * @param node the node to look up the location for
     *
     * @return the location for the node
     */
    fun getNameLocation(node: Node): Location = parser.getNameLocation(this, node)

    /**
     * Returns the location for value-portion of the given attribute
     *
     * @param node the node to look up the location for
     *
     * @return the location for the node
     */
    fun getValueLocation(node: Attr): Location = parser.getValueLocation(this, node)

    /**
     * Creates a new location within an XML text node
     *
     * @param textNode the text node
     *
     * @param begin the start offset within the text node (inclusive)
     *
     * @param end the end offset within the text node (exclusive)
     *
     * @return a new location
     */
    fun getLocation(textNode: Node, begin: Int, end: Int): Location {
        assert(textNode.nodeType == Node.TEXT_NODE || textNode.nodeType == Node.COMMENT_NODE)
        return parser.getLocation(this, textNode, begin, end)
    }

    /**
     * Reports an issue applicable to a given DOM node. The DOM node is used as the
     * scope to check for suppress lint annotations.
     *
     * @param issue        the issue to report
     *
     * @param scope        the DOM node scope the error applies to. The lint infrastructure will
     *                     check whether there are suppress directives on this node (or its
     *                     enclosing nodes) and if so suppress the warning without involving the
     *                     client.
     *
     * @param location     the location of the issue, or null if not known
     *
     * @param message      the message for this warning
     *
     * @param quickfixData optional data to pass to the IDE for use by a quickfix.
     */
    @JvmOverloads
    fun report(
            issue: Issue,
            scope: Node?,
            location: Location,
            message: String,
            quickfixData: LintFix? = null) {
        if (scope != null && driver.isSuppressed(this, issue, scope)) {
            return
        }
        super.doReport(issue, location, message, quickfixData)
    }

    @Deprecated("Here for temporary compatibility; the new typed quickfix data parameter " +
            "should be used instead",
            ReplaceWith("report(issue, scope, location, message)"))
    fun report(
            issue: Issue,
            scope: Node?,
            location: Location,
            message: String,
            quickfixData: Any?) = report(issue, scope, location, message)

    override fun report(
            issue: Issue,
            location: Location,
            message: String,
            quickfixData: LintFix?) {
        // Warn if clients use the non-scoped form? No, there are cases where an
        //  XML detector's error isn't applicable to one particular location (or it's
        //  not feasible to compute it cheaply)
        //driver.getClient().log(null, "Warning: Issue " + issue
        //        + " was reported without a scope node: Can't be suppressed.");

        // For now just check the document root itself
        if (driver.isSuppressed(this, issue, document)) {
            return
        }

        super.report(issue, location, message, quickfixData)
    }

    override val suppressCommentPrefix: String?
        get() = SUPPRESS_XML_COMMENT_PREFIX

    fun isSuppressedWithComment(node: Node, issue: Issue): Boolean {
        // Check whether there is a comment marker
        getContents() ?: return false
        val start = parser.getNodeStartOffset(this, node)
        if (start != -1) {
            return isSuppressedWithComment(start, issue)
        }

        return false
    }

    fun createLocationHandle(node: Node): Location.Handle =
            parser.createLocationHandle(this, node)

    override fun getResourceFolder(): File? =
            if (resourceFolderType != null) file.parentFile else null
}