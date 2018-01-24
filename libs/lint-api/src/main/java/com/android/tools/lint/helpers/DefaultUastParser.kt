/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.tools.lint.helpers

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.JavaEvaluator
import com.android.tools.lint.client.api.UastParser
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Project
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiPlainText
import com.intellij.psi.PsiPlainTextFile
import com.intellij.psi.impl.light.LightElement
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UastContext
import org.jetbrains.uast.getContainingFile
import org.jetbrains.uast.getIoFile
import org.jetbrains.uast.psi.UElementWithLocation
import java.io.File

open class DefaultUastParser(
        // Fully qualified names here:
        // class traffics in Project from both lint and openapi so be explicit
        project: com.android.tools.lint.detector.api.Project?,
        p: com.intellij.openapi.project.Project) : UastParser() {
    private val uastContext: UastContext?
    private val javaEvaluator: JavaEvaluator

    init {
        @Suppress("LeakingThis")
        javaEvaluator = createEvaluator(project, p)
        uastContext = if (!p.isDisposed) {
            ServiceManager.getService(p, UastContext::class.java)
        } else {
            null
        }
    }

    open protected fun createEvaluator(project: Project?,
                                  p: com.intellij.openapi.project.Project): DefaultJavaEvaluator =
            DefaultJavaEvaluator(p, project)

    /**
     * Prepare to parse the given contexts. This method will be called before
     * a series of [.parse] calls, which allows some
     * parsers to do up front global computation in case they want to more
     * efficiently process multiple files at the same time. This allows a single
     * type-attribution pass for example, which is a lot more efficient than
     * performing global type analysis over and over again for each individual
     * file
     *
     * @param contexts a list of contexts to be parsed
     *
     * @return true if the preparation succeeded; false if there were errors
     */
    override fun prepare(contexts: List<JavaContext>): Boolean = true

    /**
     * Returns an evaluator which can perform various resolution tasks,
     * evaluate inheritance lookup etc.
     *
     * @return an evaluator
     */
    override fun getEvaluator(): JavaEvaluator = javaEvaluator

    /**
     * Parse the file pointed to by the given context.
     *
     * @param context the context pointing to the file to be parsed, typically via
     *                [Context.getContents] but the file handle ([Context.file]) can also be
     *                used to map to an existing editor buffer in the surrounding tool, etc)
     * @return the compilation unit node for the file
     */
    override fun parse(context: JavaContext): UFile? {
        val ideaProject = uastContext?.project ?: return null
        if (ideaProject.isDisposed) {
            return null
        }

        val virtualFile = StandardFileSystems.local()
                .findFileByPath(context.file.absolutePath) ?: return null

        val psiFile = PsiManager.getInstance(ideaProject).findFile(virtualFile) ?: return null

        if (psiFile is PsiPlainTextFile) { // plain text: file too large to process with PSI
            if (!warnedAboutLargeFiles) {
                warnedAboutLargeFiles = true
                val max = FileUtilRt.getUserFileSizeLimit()
                val size = context.file.length() / 1024
                val sizeRoundedUp = Math.pow(2.0,
                        Math.ceil(Math.log10(size.toDouble()) / Math.log10(2.0) + 0.2)).toInt()
                context.report(
                        issue = IssueRegistry.LINT_ERROR,
                        location = Location.create(context.file),
                        message = "Source file too large for lint to process (${size}KB); the " +
                                "current max size is ${max}KB. You can increase the limit by " +
                                "setting this system property: " +
                                "`idea.max.intellisense.filesize=${sizeRoundedUp}` (or even higher)")
            }
            return null
        }

        return uastContext.convertElementWithParent(psiFile, UFile::class.java) as? UFile ?:
                // No need to log this; the parser should be reporting
                // a full warning (such as IssueRegistry#PARSER_ERROR)
                // with details, location, etc.
                return null
    }

    /**
     * Returns a UastContext which can provide UAST representations for source files
     */
    override fun getUastContext(): UastContext? = uastContext

    /**
     * Returns a [Location] for the given element
     *
     * @param context information about the file being parsed
     *
     * @param element the element to create a location for
     *
     * @return a location for the given node
     */
    override // subclasses may want to override/optimize
    fun getLocation(context: JavaContext, element: PsiElement): Location {
        var range: TextRange? = null

        if (element is PsiCompiledElement) {
            if (element is LightElement) {
                range = (element as PsiElement).textRange
            }
            if (range == null || TextRange.EMPTY_RANGE == range) {
                val containingFile = element.getContainingFile()
                if (containingFile != null) {
                    val virtualFile = containingFile.virtualFile
                    if (virtualFile != null) {
                        return Location.create(VfsUtilCore.virtualToIoFile(virtualFile))
                    }
                }
                return Location.create(context.file)
            }
        } else {
            range = element.textRange
        }

        val containingFile = getContainingFile(context, element)
        var file = context.file
        var contents: CharSequence = context.getContents() ?: ""

        if (containingFile != null && containingFile != context.psiFile) {
            // Reporting an error in a different file.
            if (context.driver.scope.size == 1) {
                // Don't bother with this error if it's in a different file during single-file analysis
                return Location.NONE
            }
            val ioFile = getFile(containingFile) ?: return Location.NONE
            file = ioFile
            contents = getFileContents(containingFile)
        }

        if (range == null) { // e.g. light elements
            if (element is LightElement) {
                val parent = element.getParent()
                if (parent != null) {
                    return getLocation(context, parent)
                }
            }
            return Location.create(file)
        }

        return Location.create(file, contents, range.startOffset, range.endOffset)
                .setSource(element)
    }

    /** Returns the containing file for the given element */
    private fun getContainingFile(context: JavaContext, element: PsiElement): PsiFile? {
        val containingFile = element.containingFile
        if (containingFile != context.psiFile) {
            // In Kotlin files identifiers are sometimes using LightElements that are hosted in
            // a dummy file, these do not have the right PsiFile as containing elements
            val cls = containingFile.javaClass
            val name = cls.name
            if (name.startsWith("org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration")) {
                try {
                    val declaredField = cls.superclass.getDeclaredField("ktFile")
                    declaredField.isAccessible = true
                    return (declaredField.get(containingFile) as? PsiFile?) ?: containingFile
                } catch (e: Throwable) {
                }
            }
        }

        return containingFile
    }

    override // subclasses may want to override/optimize
    fun getLocation(context: JavaContext, element: UElement): Location {
        if (element is UElementWithLocation) {
            val file = element.getContainingFile() ?: return Location.NONE
            val ioFile = file.getIoFile() ?: return Location.NONE
            val text = file.psi.text
            val location = Location.create(ioFile, text, element.startOffset, element.endOffset)
            location.setSource(element)
            return location
        } else {
            val psiElement = element.psi
            if (psiElement != null) {
                return getLocation(context, psiElement)
            }
            val parent = element.uastParent
            if (parent != null) {
                return getLocation(context, parent)
            }
        }

        return Location.NONE
    }

    override // subclasses may want to override/optimize
    fun getCallLocation(context: JavaContext, call: UCallExpression,
                        includeReceiver: Boolean, includeArguments: Boolean): Location {
        val receiver = call.receiver
        if (!includeReceiver || receiver == null) {
            if (includeArguments) {
                // Method with arguments but no receiver is the default range for UCallExpressions
                return getLocation(context, call)
            }
            // Just the method name
            val methodIdentifier = call.methodIdentifier
            if (methodIdentifier != null) {
                return getLocation(context, methodIdentifier)
            }
        } else {
            if (!includeArguments) {
                val methodIdentifier = call.methodIdentifier
                if (methodIdentifier != null) {
                    return getRangeLocation(context, receiver, 0, methodIdentifier, 0)
                }
            }
            return getRangeLocation(context, receiver, 0, call, 0)
        }

        return getLocation(context, call)
    }

    override fun getFile(file: PsiFile): File? {
        val virtualFile = file.virtualFile
        return if (virtualFile != null) VfsUtilCore.virtualToIoFile(virtualFile) else null
    }

    override fun getFileContents(file: PsiFile): CharSequence = file.text

    override fun createLocation(element: PsiElement): Location {
        val range = element.textRange
        val containingFile = element.containingFile
        val contents: CharSequence
        val file = getFile(containingFile) ?: return Location.NONE
        contents = getFileContents(containingFile)
        return Location.create(file, contents, range.startOffset, range.endOffset)
                .setSource(element)
    }

    override fun createLocation(element: UElement): Location {
        if (element is UElementWithLocation) {
            val file = element.getContainingFile() ?: return Location.NONE
            val ioFile = file.getIoFile() ?: return Location.NONE
            val text = file.psi.text
            val location = Location.create(ioFile, text, element.startOffset,
                    element.endOffset)
            location.setSource(element)
            return location
        } else {
            val psiElement = element.psi
            if (psiElement != null) {
                return createLocation(psiElement)
            }
            val parent = element.uastParent
            if (parent != null) {
                return createLocation(parent)
            }
        }

        return Location.NONE
    }

    /**
     * Returns a [Location] for the given node range (from the starting offset of the first
     * node to the ending offset of the second node).
     *
     * @param context   information about the file being parsed
     *
     * @param from      the AST node to get a starting location from
     *
     * @param fromDelta Offset delta to apply to the starting offset
     *
     * @param to        the AST node to get a ending location from
     *
     * @param toDelta   Offset delta to apply to the ending offset
     *
     * @return a location for the given node
     */
    override fun getRangeLocation(context: JavaContext, from: PsiElement,
                         fromDelta: Int, to: PsiElement, toDelta: Int): Location {
        val contents = context.getContents()
        val fromRange = from.textRange
        val start = Math.max(0, fromRange.startOffset + fromDelta)
        val end = Math.min(contents?.length ?: Integer.MAX_VALUE,
                to.textRange.endOffset + toDelta)
        if (end <= start) {
            // Some AST nodes don't have proper bounds, such as empty parameter lists
            return Location.create(context.file, contents, start, fromRange.endOffset)
                    .setSource(from)
        }
        return Location.create(context.file, contents, start, end).setSource(from)
    }

    private fun getTextRange(element: UElement): TextRange? {
        if (element is UElementWithLocation) {
            return TextRange(element.startOffset, element.endOffset)
        } else {
            val psiElement = element.psi
            if (psiElement != null) {
                return psiElement.textRange
            }
        }

        return null
    }

    override fun getRangeLocation(context: JavaContext, from: UElement,
                         fromDelta: Int, to: UElement, toDelta: Int): Location {
        var contents = context.getContents()
        val fromRange = getTextRange(from)
        val toRange = getTextRange(to)

        // Make sure this element is reported in the correct file
        var file = context.file
        val psi = findPsi(from)
        if (psi != null) {
            val containingFile = psi.containingFile
            contents = context.getContents()
            if (containingFile != context.psiFile) {
                // Reporting an error in a different file.
                if (context.driver.scope.size == 1) {
                    // Don't bother with this error if it's in a different file during single-file analysis
                    return Location.NONE
                }
                val ioFile = getFile(containingFile) ?: return Location.NONE
                file = ioFile
                contents = getFileContents(containingFile)
            }
        }

        if (fromRange != null && toRange != null) {
            val start = Math.max(0, fromRange.startOffset + fromDelta)
            val end = Math.min(if (contents == null) Integer.MAX_VALUE else contents.length,
                    toRange.endOffset + toDelta)
            if (end <= start) {
                // Some AST nodes don't have proper bounds, such as empty parameter lists
                return Location.create(file, contents, start, fromRange.endOffset)
                        .setSource(from)
            }
            return Location.create(file, contents, start, end).setSource(from)
        }

        return Location.create(file).setSource(from)
    }

    private fun findPsi(element: UElement?): PsiElement? {
        var currentElement = element
        while (currentElement != null) {
            val psi = currentElement.psi
            if (psi != null) {
                return psi
            }
            currentElement = currentElement.uastParent
        }
        return null
    }

    /**
     * Like [.getRangeLocation]
     * but both offsets are relative to the starting offset of the given node. This is
     * sometimes more convenient than operating relative to the ending offset when you
     * have a fixed range in mind.
     *
     * @param context   information about the file being parsed
     *
     * @param from      the AST node to get a starting location from
     *
     * @param fromDelta Offset delta to apply to the starting offset
     *
     * @param toDelta   Offset delta to apply to the starting offset
     *
     * @return a location for the given node
     */
    override fun getRangeLocation(context: JavaContext, from: PsiElement,
                         fromDelta: Int, toDelta: Int): Location =
            getRangeLocation(context, from, fromDelta, from, -(from.textRange.length - toDelta))

    override fun getRangeLocation(context: JavaContext, from: UElement,
                         fromDelta: Int, toDelta: Int): Location {
        val fromRange = getTextRange(from)
        if (fromRange != null) {
            return getRangeLocation(context, from, fromDelta, from,
                    -(fromRange.length - toDelta))
        }
        return Location.create(context.file).setSource(from)
    }

    /**
     * Returns a [Location] for the given node. This attempts to pick a shorter
     * location range than the entire node; for a class or method for example, it picks
     * the name node (if found). For statement constructs such as a `switch` statement
     * it will highlight the keyword, etc.
     *
     * @param context information about the file being parsed
     *
     * @param element the node to create a location for
     *
     * @return a location for the given node
     */
    override fun getNameLocation(context: JavaContext, element: PsiElement): Location {
        var namedElement = element
        val nameNode = JavaContext.findNameElement(namedElement)
        if (nameNode != null) {
            namedElement = nameNode
        }

        return getLocation(context, namedElement)
    }

    override fun getNameLocation(context: JavaContext, element: UElement): Location {
        var namedElement = element
        val nameNode = JavaContext.findNameElement(namedElement)
        if (nameNode != null) {
            namedElement = nameNode
        } else if (namedElement is PsiNameIdentifierOwner) {
            val nameIdentifier = namedElement.nameIdentifier
            if (nameIdentifier != null) {
                return getLocation(context, nameIdentifier)
            }
        }

        return getLocation(context, namedElement)
    }

    companion object {
        var warnedAboutLargeFiles = false
    }
}
