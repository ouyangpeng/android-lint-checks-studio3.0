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

import com.android.SdkConstants.CONSTRUCTOR_NAME
import com.android.SdkConstants.DOT_CLASS
import com.android.SdkConstants.DOT_JAVA
import com.android.tools.lint.client.api.LintDriver
import com.android.tools.lint.detector.api.Location.SearchDirection
import com.android.tools.lint.detector.api.Location.SearchDirection.BACKWARD
import com.android.tools.lint.detector.api.Location.SearchDirection.EOL_BACKWARD
import com.android.tools.lint.detector.api.Location.SearchDirection.FORWARD
import com.android.tools.lint.detector.api.Location.SearchHints
import com.google.common.annotations.Beta
import com.google.common.base.Splitter
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import java.io.File

/**
 * A [Context] used when checking .class files.
 *
 * **NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.**
 */
@Beta
class ClassContext
/**
 * Construct a new [ClassContext]
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

        /**
         * The jar file, if any. If this is null, the .class file is a real file
         * on disk, otherwise it represents a relative path within the jar file.
         *
         * @return the jar file, or null
         */
        val jarFile: File?,

        /** the root binary directory containing this .class file */
        private val binDir: File,

        /** The class file byte data  */
        val bytecode: ByteArray,

        /** The class file DOM root node  */
        val classNode: ClassNode,

        /**
         * Whether this class is part of a library (rather than corresponding to one of the
         * source files in this project
         */
        val isFromClassLibrary: Boolean,

        /** The contents of the source file, if source file is known/found  */
        private var sourceContents: CharSequence?) : Context(driver, project, main, file) {

    /** The source file, if known/found  */
    private var sourceFile: File? = null

    /** Whether we've searched for the source file (used to avoid repeated failed searches)  */
    private var searchedForSource: Boolean = false

    /**
     * Returns the source file for this class file, if possible.
     *
     * @return the source file, or null
     */
    fun getSourceFile(): File? {
        if (sourceFile == null && !searchedForSource) {
            searchedForSource = true

            var source: String? = classNode.sourceFile
            if (source == null) {
                source = file.name
                if (source!!.endsWith(DOT_CLASS)) {
                    source = source.substring(0, source.length - DOT_CLASS.length) + DOT_JAVA
                }
                val index = source.indexOf('$')
                if (index != -1) {
                    source = source.substring(0, index) + DOT_JAVA
                }
            }
            if (jarFile != null) {
                val relative = file.parent + File.separator + source
                val sources = project.getJavaSourceFolders()
                for (dir in sources) {
                    val sourceFile = File(dir, relative)
                    if (sourceFile.exists()) {
                        this.sourceFile = sourceFile
                        break
                    }
                }
            } else {
                // Determine package
                val topPath = binDir.path
                val parentPath = file.parentFile.path
                if (parentPath.startsWith(topPath)) {
                    val start = topPath.length + 1
                    val relative = if (start > parentPath.length)
                    // default package?
                        ""
                    else
                        parentPath.substring(start)
                    val sources = project.getJavaSourceFolders()
                    for (dir in sources) {
                        val sourceFile = File(dir, relative + File.separator + source)
                        if (sourceFile.exists()) {
                            this.sourceFile = sourceFile
                            break
                        }
                    }
                }
            }
        }

        return sourceFile
    }

    /**
     * Returns the contents of the source file for this class file, if found.
     *
     * @return the source contents, or ""
     */
    fun getSourceContents(): CharSequence {
        if (sourceContents == null) {
            val sourceFile = getSourceFile()
            if (sourceFile != null) {
                sourceContents = client.readFile(sourceFile)
            }

            if (sourceContents == null) {
                sourceContents = ""
            }
        }

        return sourceContents!!
    }

    /**
     * Returns the contents of the source file for this class file, if found. If
     * `read` is false, do not read the source contents if it has not
     * already been read. (This is primarily intended for the lint
     * infrastructure; most client code would call [.getSourceContents]
     * .)
     *
     * @param read whether to read the source contents if it has not already
     *            been initialized
     *
     * @return the source contents, which will never be null if `read` is
     *         true, or null if `read` is false and the source contents
     *         hasn't already been read.
     */
    fun getSourceContents(read: Boolean): CharSequence? {
        return if (read) {
            getSourceContents()
        } else {
            sourceContents
        }
    }

    /**
     * Returns a location for the given source line number in this class file's
     * source file, if available.
     *
     * @param line the line number (1-based, which is what ASM uses)
     *
     * @param patternStart optional pattern to search for in the source for
     *            range start
     *
     * @param patternEnd optional pattern to search for in the source for range
     *            end
     *
     * @param hints additional hints about the pattern search (provided
     *            `patternStart` is non null)
     *
     * @return a location, never null
     */
    fun getLocationForLine(line: Int, patternStart: String?,
                           patternEnd: String?, hints: SearchHints?): Location {
        val sourceFile = getSourceFile()
        if (sourceFile != null) {
            // ASM line numbers are 1-based, and lint line numbers are 0-based
            return if (line != -1) {
                Location.create(sourceFile, getSourceContents(), line - 1,
                        patternStart, patternEnd, hints)
            } else {
                Location.create(sourceFile)
            }
        }

        return Location.create(file)
    }

    /**
     * Reports an issue.
     *
     * Detectors should only call this method if an error applies to the whole class
     * scope and there is no specific method or field that applies to the error.
     * If so, use
     * [.report] or
     * [.report], such that
     * suppress annotations are checked.
     *
     * @param issue the issue to report
     *
     * @param location the location of the issue, or null if not known
     *
     * @param message the message for this warning
     */
    override fun report(
            issue: Issue,
            location: Location,
            message: String,
            quickfixData: LintFix?) {
        if (driver.isSuppressed(issue, classNode)) {
            return
        }
        var curr: ClassNode? = classNode
        while (curr != null) {
            val prev = curr
            curr = driver.getOuterClassNode(curr)
            if (curr != null) {
                if (prev.outerMethod != null) {
                    val methods = curr.methods// ASM API
                    for (m in methods) {
                        val method = m as MethodNode
                        if (method.name == prev.outerMethod && method.desc == prev.outerMethodDesc) {
                            // Found the outer method for this anonymous class; continue
                            // reporting on it (which will also work its way up the parent
                            // class hierarchy)
                            if (driver.isSuppressed(issue, classNode, method, null)) {
                                return
                            }
                            break
                        }
                    }
                }
                if (driver.isSuppressed(issue, curr)) {
                    return
                }
            }
        }

        super.doReport(issue, location, message, quickfixData)
    }

    // Unfortunately, ASMs nodes do not extend a common DOM node type with parent
    // pointers, so we have to have multiple methods which pass in each type
    // of node (class, method, field) to be checked.

    /**
     * Reports an issue applicable to a given method node.
     *
     * @param issue the issue to report
     *
     * @param method the method scope the error applies to. The lint
     *            infrastructure will check whether there are suppress
     *            annotations on this method (or its enclosing class) and if so
     *            suppress the warning without involving the client.
     *
     * @param instruction the instruction within the method the error applies
     *            to. You cannot place annotations on individual method
     *            instructions (for example, annotations on local variables are
     *            allowed, but are not kept in the .class file). However, this
     *            instruction is needed to handle suppressing errors on field
     *            initializations; in that case, the errors may be reported in
     *            the `<clinit>` method, but the annotation is found not
     *            on that method but for the [FieldNode]'s.
     *
     * @param location the location of the issue, or null if not known
     *
     * @param message the message for this warning
     */
    fun report(
            issue: Issue,
            method: MethodNode?,
            instruction: AbstractInsnNode?,
            location: Location,
            message: String) {
        if (method != null && driver.isSuppressed(issue, classNode, method, instruction)) {
            return
        }
        report(issue, location, message) // also checks the class node
    }

    /**
     * Reports an issue applicable to a given method node.
     *
     * @param issue the issue to report
     *
     * @param field the scope the error applies to. The lint infrastructure
     *    will check whether there are suppress annotations on this field (or its enclosing
     *    class) and if so suppress the warning without involving the client.
     *
     * @param location the location of the issue, or null if not known
     *
     * @param message the message for this warning
     */
    fun report(
            issue: Issue,
            field: FieldNode?,
            location: Location,
            message: String) {
        if (field != null && driver.isSuppressed(issue, field)) {
            return
        }
        report(issue, location, message) // also checks the class node
    }

    /**
     * Report an error.
     * Like [.report] but with
     * a now-unused data parameter at the end.
     */
    @Deprecated("Use {@link #report(Issue, FieldNode, Location, String)} instead; this method " +
            "is here for custom rule compatibility",
            ReplaceWith("report(issue, method, instruction, location, message)"))
    fun report(
            issue: Issue,
            method: MethodNode?,
            instruction: AbstractInsnNode?,
            location: Location,
            message: String,
            data: Any?) = report(issue, method, instruction, location, message)

    /**
     * Report an error.
     * Like [.report] but with
     * a now-unused data parameter at the end.
     *
     */
    @Deprecated("Use {@link #report(Issue, FieldNode, Location, String)} instead; this method " +
            "is here for custom rule compatibility",
            ReplaceWith("report(issue, field, location, message)"))
    fun report(
            issue: Issue,
            field: FieldNode?,
            location: Location,
            message: String,
            data: Any?) = report(issue, field, location, message)

    /**
     * Returns a location for the given [ClassNode], where class node is
     * either the top level class, or an inner class, in the current context.
     *
     * @param classNode the class in the current context
     *
     * @return a location pointing to the class declaration, or as close to it
     *         as possible
     */
    fun getLocation(classNode: ClassNode): Location {
        // Attempt to find a proper location for this class. This is tricky
        // since classes do not have line number entries in the class file; we need
        // to find a method, look up the corresponding line number then search
        // around it for a suitable tag, such as the class name.
        var pattern: String
        pattern = if (isAnonymousClass(classNode.name)) {
            classNode.superName
        } else {
            classNode.name
        }
        var index = pattern.lastIndexOf('$')
        if (index != -1) {
            pattern = pattern.substring(index + 1)
        }
        index = pattern.lastIndexOf('/')
        if (index != -1) {
            pattern = pattern.substring(index + 1)
        }

        return getLocationForLine(findLineNumber(classNode), pattern, null,
                SearchHints.create(BACKWARD).matchJavaSymbol())
    }

    /**
     * Returns a location for the given [MethodNode].
     *
     * @param methodNode the class in the current context
     *
     * @param classNode the class containing the method
     *
     * @return a location pointing to the class declaration, or as close to it
     *         as possible
     */
    fun getLocation(methodNode: MethodNode,
                    classNode: ClassNode): Location {
        // Attempt to find a proper location for this class. This is tricky
        // since classes do not have line number entries in the class file; we need
        // to find a method, look up the corresponding line number then search
        // around it for a suitable tag, such as the class name.
        val pattern: String
        val searchMode: SearchDirection
        if (methodNode.name == CONSTRUCTOR_NAME) {
            searchMode = EOL_BACKWARD
            pattern = if (isAnonymousClass(classNode.name)) {
                classNode.superName.substring(classNode.superName.lastIndexOf('/') + 1)
            } else {
                classNode.name.substring(classNode.name.lastIndexOf('$') + 1)
            }
        } else {
            searchMode = BACKWARD
            pattern = methodNode.name
        }

        return getLocationForLine(findLineNumber(methodNode), pattern, null,
                SearchHints.create(searchMode).matchJavaSymbol())
    }

    /**
     * Returns a location for the given [AbstractInsnNode].
     *
     * @param instruction the instruction to look up the location for
     *
     * @return a location pointing to the instruction, or as close to it
     *         as possible
     */
    fun getLocation(instruction: AbstractInsnNode): Location {
        var hints = SearchHints.create(FORWARD).matchJavaSymbol()
        var pattern: String? = null
        if (instruction is MethodInsnNode) {
            if (instruction.name == CONSTRUCTOR_NAME) {
                pattern = instruction.owner
                hints = hints.matchConstructor()
            } else {
                pattern = instruction.name
            }
            var index = pattern!!.lastIndexOf('$')
            if (index != -1) {
                pattern = pattern.substring(index + 1)
            }
            index = pattern.lastIndexOf('/')
            if (index != -1) {
                pattern = pattern.substring(index + 1)
            }
        }

        val line = findLineNumber(instruction)
        return getLocationForLine(line, pattern, null, hints)
    }

    companion object {

        /**
         * Finds the line number closest to the given node
         *
         * @param node the instruction node to get a line number for
         *
         * @return the closest line number, or -1 if not known
         */
        @JvmStatic
        fun findLineNumber(node: AbstractInsnNode): Int {
            var curr: AbstractInsnNode? = node

            // First search backwards
            while (curr != null) {
                if (curr.type == AbstractInsnNode.LINE) {
                    return (curr as LineNumberNode).line
                }
                curr = curr.previous
            }

            // Then search forwards
            curr = node
            while (curr != null) {
                if (curr.type == AbstractInsnNode.LINE) {
                    return (curr as LineNumberNode).line
                }
                curr = curr.next
            }

            return -1
        }

        /**
         * Finds the line number closest to the given method declaration
         *
         * @param node the method node to get a line number for
         *
         * @return the closest line number, or -1 if not known
         */
        @JvmStatic
        fun findLineNumber(node: MethodNode): Int {
            if (node.instructions != null && node.instructions.size() > 0) {
                return findLineNumber(node.instructions.get(0))
            }

            return -1
        }

        /**
         * Finds the line number closest to the given class declaration
         *
         * @param node the method node to get a line number for
         *
         * @return the closest line number, or -1 if not known
         */
        @JvmStatic
        fun findLineNumber(node: ClassNode): Int {
            if (node.methods != null && !node.methods.isEmpty()) {
                val firstMethod = getFirstRealMethod(node)
                if (firstMethod != null) {
                    return findLineNumber(firstMethod)
                }
            }

            return -1
        }

        @JvmStatic
        private fun getFirstRealMethod(classNode: ClassNode): MethodNode? {
            // Return the first method in the class for line number purposes. Skip <init>,
            // since it's typically not located near the real source of the method.
            if (classNode.methods != null) {
                val methods = classNode.methods// ASM API
                for (m in methods) {
                    val method = m as MethodNode
                    if (method.name[0] != '<') {
                        return method
                    }
                }

                if (!classNode.methods.isEmpty()) {
                    return classNode.methods[0] as MethodNode
                }
            }

            return null
        }

        @JvmStatic
        private fun isAnonymousClass(fqcn: String): Boolean {
            val lastIndex = fqcn.lastIndexOf('$')
            if (lastIndex != -1 && lastIndex < fqcn.length - 1) {
                if (Character.isDigit(fqcn[lastIndex + 1])) {
                    return true
                }
            }
            return false
        }

        /**
         * Converts from a VM owner name (such as foo/bar/Foo$Baz) to a
         * fully qualified class name (such as foo.bar.Foo.Baz).
         *
         * @param owner the owner name to convert
         *
         * @return the corresponding fully qualified class name
         */
        @JvmStatic
        fun getFqcn(owner: String): String = owner.replace('/', '.').replace('$', '.')

        /**
         * Computes a user-readable type signature from the given class owner, name
         * and description. For example, for owner="foo/bar/Foo$Baz", name="foo",
         * description="(I)V", it returns "void foo.bar.Foo.Bar#foo(int)".
         *
         * @param owner the class name
         *
         * @param name the method name
         *
         * @param desc the method description
         *
         * @return a user-readable string
         */
        @JvmStatic
        fun createSignature(owner: String?, name: String?, desc: String?): String {
            val sb = StringBuilder(100)

            if (desc != null) {
                val returnType = Type.getReturnType(desc)
                sb.append(getTypeString(returnType))
                sb.append(' ')
            }

            if (owner != null) {
                sb.append(getFqcn(owner))
            }
            if (name != null) {
                sb.append('#')
                sb.append(name)
                if (desc != null) {
                    val argumentTypes = Type.getArgumentTypes(desc)
                    if (argumentTypes != null && argumentTypes.isNotEmpty()) {
                        sb.append('(')
                        var first = true
                        for (type in argumentTypes) {
                            if (first) {
                                first = false
                            } else {
                                sb.append(", ")
                            }
                            sb.append(getTypeString(type))
                        }
                        sb.append(')')
                    }
                }
            }

            return sb.toString()
        }

        @JvmStatic
        private fun getTypeString(type: Type): String {
            val s = type.className
            return if (s.startsWith("java.lang.")) {
                s.substring("java.lang.".length)
            } else {
                s
            }
        }

        /**
         * Computes the internal class name of the given fully qualified class name.
         * For example, it converts foo.bar.Foo.Bar into foo/bar/Foo$Bar
         *
         * @param qualifiedName the fully qualified class name
         *
         * @return the internal class name
         */
        @JvmStatic
        fun getInternalName(qualifiedName: String): String {
            var fqcn = qualifiedName
            if (fqcn.indexOf('.') == -1) {
                return fqcn
            }

            val index = fqcn.indexOf('<')
            if (index != -1) {
                fqcn = fqcn.substring(0, index)
            }

            // If class name contains $, it's not an ambiguous inner class name.
            if (fqcn.indexOf('$') != -1) {
                return fqcn.replace('.', '/')
            }
            // Let's assume that components that start with Caps are class names.
            val sb = StringBuilder(fqcn.length)
            var prev: String? = null
            for (part in Splitter.on('.').split(fqcn)) {
                if (prev != null && !prev.isEmpty()) {
                    if (Character.isUpperCase(prev[0])) {
                        sb.append('$')
                    } else {
                        sb.append('/')
                    }
                }
                sb.append(part)
                prev = part
            }

            return sb.toString()
        }
    }
}
