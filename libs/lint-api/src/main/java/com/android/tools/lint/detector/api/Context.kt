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

import com.android.SdkConstants.DOT_GRADLE
import com.android.SdkConstants.DOT_JAVA
import com.android.SdkConstants.DOT_XML
import com.android.SdkConstants.SUPPRESS_ALL
import com.android.tools.lint.client.api.Configuration
import com.android.tools.lint.client.api.LintClient
import com.android.tools.lint.client.api.LintDriver
import com.android.tools.lint.client.api.SdkInfo
import com.android.utils.CharSequences
import com.android.utils.CharSequences.indexOf
import com.google.common.annotations.Beta
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UElement
import org.w3c.dom.Node
import java.io.File
import java.util.EnumSet

/**
 * Context passed to the detectors during an analysis run. It provides
 * information about the file being analyzed, it allows shared properties (so
 * the detectors can share results), etc.
 *
 **NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.**
 */
@Beta
open class Context
/**
 * Construct a new [Context]
 */
(
        /** The driver running through the checks  */
        val driver: LintDriver,
        /** The project containing the file being checked  */
        val project: Project,
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
        private val mainProject: Project?,
        /**
         * The file being checked. Note that this may not always be to a concrete
         * file. For example, in the [Detector.beforeCheckProject]
         * method, the context file is the directory of the project.
         */
        @JvmField
        val file: File,

        /** The contents of the file  */
        private var contents: CharSequence? = null) {

    /** The current configuration controlling which checks are enabled etc  */
    val configuration: Configuration

    /** Whether this file contains any suppress markers (null means not yet determined)  */
    private var containsCommentSuppress: Boolean? = null

    init {
        configuration = project.getConfiguration(driver)
    }

    /**
     * The scope for the lint job
     */
    val scope: EnumSet<Scope>
        get() = driver.scope

    /**
     * Returns the main project if this project is a library project, or self
     * if this is not a library project. The main project is the root project
     * of all library projects, not necessarily the directly including project.
     *
     * @return the main project, never null
     */
    fun getMainProject(): Project = mainProject ?: project

    /**
     * The lint client requesting the lint check
     */
    val client: LintClient
        get() = driver.client

    /**
     * Returns the contents of the file. This may not be the contents of the
     * file on disk, since it delegates to the [LintClient], which in turn
     * may decide to return the current edited contents of the file open in an
     * editor.
     *
     * @return the contents of the given file, or null if an error occurs.
     */
    open fun getContents(): CharSequence? {
        if (contents == null) {
            contents = driver.client.readFile(file)
        }

        return contents
    }

    /**
     * Gets the SDK info for the current project.
     *
     * @return the SDK info for the current project, never null
     */
    val sdkInfo: SdkInfo
        get() = project.getSdkInfo()

    // ---- Convenience wrappers  ---- (makes the detector code a bit leaner)

    /**
     * Returns false if the given issue has been disabled. Convenience wrapper
     * around [Configuration.getSeverity].
     *
     * @param issue the issue to check
     *
     * @return false if the issue has been disabled
     */
    fun isEnabled(issue: Issue): Boolean = configuration.isEnabled(issue)

    /**
     * Reports an issue. Convenience wrapper around [LintClient.report]
     *
     * @param issue        the issue to report
     *
     * @param location     the location of the issue
     *
     * @param message      the message for this warning
     *
     * @param quickfixData parameterized data for IDE quickfixes
     */
    @JvmOverloads
    open fun report(
            issue: Issue,
            location: Location,
            message: String,
            quickfixData: LintFix? = null) {
        // See if we actually have an associated source for this location, and if so
        // check to see if the warning might be suppressed.
        val source = location.source
        if (source is Node) {
            // Also see if we have the context for this location (e.g. code could
            // have directly called XmlContext/JavaContext report methods instead); this
            // is better because the context also checks for issues suppressed via comment
            if (this is XmlContext) {
                if (source.ownerDocument === this.document) {
                    this.report(issue, source, location, message, quickfixData)
                    return
                }
            }
            if (driver.isSuppressed(null, issue, source)) {
                return
            }
        } else if (source is PsiElement) {
            // Check for suppressed issue via location node
            if (this is JavaContext) {
                val javaContext = this
                if (source.containingFile == javaContext.psiFile) {
                    javaContext.report(issue, source, location, message, quickfixData)
                    return
                }
            }
            if (driver.isSuppressed(null, issue, source)) {
                return
            }
        } else if (source is UElement) {
            val element = source.psi
            if (element != null && this is JavaContext) {
                val javaContext = this
                if (element.containingFile == javaContext.psiFile) {
                    javaContext.report(issue, element, location, message, quickfixData)
                    return
                }
            }
            if (driver.isSuppressed(null, issue, element)) {
                return
            }
        }

        doReport(issue, location, message, quickfixData)
    }

    @Deprecated("Here for temporary compatibility; the new typed quickfix data parameter" +
            " should be used instead", ReplaceWith("report(issue, location, message)"))
    fun report(
            issue: Issue,
            location: Location,
            message: String,
            quickfixData: Any?) = report(issue, location, message)

    // Method not callable outside of the lint infrastructure: perform the actual reporting.
    // This is a separate method instead of just having Context#report() do this work,
    // since Context#report() will possibly redirect to the XmlContext or JavaContext reporting
    // mechanisms if it discovers that it's been called on the wrong node.
    protected fun doReport(
            issue: Issue,
            location: Location,
            message: String,
            quickfixData: LintFix?) {

        @Suppress("SENSELESS_COMPARISON")
        if (location == null) {
            // Misbehaving third-party lint detectors, called from Java
            assert(false) { issue }
            return
        }

        if (location === Location.NONE) {
            // Detector reported error for issue in a non-applicable location etc
            return
        }

        var configuration = this.configuration

        // If this error was computed for a context where the context corresponds to
        // a project instead of a file, the actual error may be in a different project (e.g.
        // a library project), so adjust the configuration as necessary.
        val project = driver.findProjectFor(location.file)
        if (project != null) {
            configuration = project.getConfiguration(driver)
        }

        // If an error occurs in a library project, but you've disabled that check in the
        // main project, disable it in the library project too. (In some cases you don't
        // control the lint.xml of a library project, and besides, if you're not interested in
        // a check for your main project you probably don't care about it in the library either.)
        if (configuration !== this.configuration && this.configuration.getSeverity(issue) === Severity.IGNORE) {
            return
        }

        val severity = configuration.getSeverity(issue)
        if (severity === Severity.IGNORE) {
            return
        }

        driver.client.report(this, issue, severity, location, message, TextFormat.RAW,
                quickfixData)
    }

    /**
     * Send an exception to the log. Convenience wrapper around [LintClient.log].
     *
     * @param exception the exception, possibly null
     *
     * @param format the error message using [java.lang.String.format] syntax, possibly null
     *
     * @param args any arguments for the format string
     */
    fun log(
            exception: Throwable?,
            format: String?,
            vararg args: Any) = driver.client.log(exception, format, *args)

    /**
     * Returns the current phase number. The first pass is numbered 1. Only one pass
     * will be performed, unless a [Detector] calls [.requestRepeat].
     *
     * @return the current phase, usually 1
     */
    val phase: Int
        get() = driver.phase

    /**
     * Requests another pass through the data for the given detector. This is
     * typically done when a detector needs to do more expensive computation,
     * but it only wants to do this once it **knows** that an error is
     * present, or once it knows more specifically what to check for.
     *
     * @param detector the detector that should be included in the next pass.
     *            Note that the lint runner may refuse to run more than a couple
     *            of runs.
     *
     * @param scope the scope to be revisited. This must be a subset of the
     *       current scope ([.getScope], and it is just a performance hint;
     *       in particular, the detector should be prepared to be called on other
     *       scopes as well (since they may have been requested by other detectors).
     *       You can pall null to indicate "all".
     */
    fun requestRepeat(detector: Detector, scope: EnumSet<Scope>?) =
            driver.requestRepeat(detector, scope)

    /** Returns the comment marker used in Studio to suppress statements for language, if any  */
    protected open val suppressCommentPrefix: String?
        get() {

            val path = file.path
            if (path.endsWith(DOT_JAVA) || path.endsWith(DOT_GRADLE)) {
                return SUPPRESS_JAVA_COMMENT_PREFIX
            } else if (path.endsWith(DOT_XML)) {
                return SUPPRESS_XML_COMMENT_PREFIX
            } else if (path.endsWith(".cfg") || path.endsWith(".pro")) {
                return "#suppress "
            }

            return null
        }

    /** Returns whether this file contains any suppress comment markers  */
    fun containsCommentSuppress(): Boolean {
        if (containsCommentSuppress == null) {
            containsCommentSuppress = false
            val prefix = suppressCommentPrefix
            if (prefix != null) {
                val contents = getContents()
                if (contents != null) {
                    containsCommentSuppress = indexOf(contents, prefix) != -1
                }
            }
        }

        return containsCommentSuppress!!
    }

    /**
     * Returns true if the given issue is suppressed at the given character offset
     * in the file's contents
     */
    fun isSuppressedWithComment(startOffset: Int, issue: Issue): Boolean {
        val prefix = suppressCommentPrefix ?: return false

        if (startOffset <= 0) {
            return false
        }

        // Check whether there is a comment marker
        val contents: CharSequence = getContents() ?: ""
        if (startOffset >= contents.length) {
            return false
        }

        // Scan backwards to the previous line and see if it contains the marker
        val lineStart = contents.lastIndexOf('\n', startOffset) + 1
        if (lineStart <= 1) {
            return false
        }
        val index = findPrefixOnPreviousLine(contents, lineStart, prefix)
        if (index != -1 && index + prefix.length < lineStart) {
            val line = contents.subSequence(index + prefix.length, lineStart).toString()
            return line.contains(issue.id) || line.contains(SUPPRESS_ALL) && line.trim { it <= ' ' }.startsWith(SUPPRESS_ALL)
        }

        return false
    }

    private fun findPrefixOnPreviousLine(contents: CharSequence, lineStart: Int,
                                         prefix: String): Int {
        // Search backwards on the previous line until you find the prefix start (also look
        // back on previous lines if the previous line(s) contain just whitespace
        val first = prefix[0]
        var offset = lineStart - 2 // 0: first char on this line, -1: \n on previous line, -2 last
        var seenNonWhitespace = false
        while (offset >= 0) {
            val c = contents[offset]
            if (seenNonWhitespace && c == '\n') {
                return -1
            }

            if (!seenNonWhitespace && !Character.isWhitespace(c)) {
                seenNonWhitespace = true
            }

            if (c == first && CharSequences.regionMatches(contents, offset, prefix, 0,
                    prefix.length)) {
                return offset
            }
            offset--
        }

        return -1
    }
}
