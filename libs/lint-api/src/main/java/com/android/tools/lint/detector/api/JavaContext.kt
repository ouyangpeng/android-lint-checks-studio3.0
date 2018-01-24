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

@file:Suppress("DEPRECATION") // Still using deprecated APIs for backwards compatibility

package com.android.tools.lint.detector.api

import com.android.SdkConstants.CLASS_CONTEXT
import com.android.tools.lint.client.api.JavaEvaluator
import com.android.tools.lint.client.api.JavaParser
import com.android.tools.lint.client.api.JavaParser.ResolvedClass
import com.android.tools.lint.client.api.JavaParser.ResolvedNode
import com.android.tools.lint.client.api.JavaParser.TypeDescriptor
import com.android.tools.lint.client.api.LintDriver
import com.android.tools.lint.client.api.UastParser
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiLabeledStatement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiSwitchStatement
import lombok.ast.AnnotationElement
import lombok.ast.AnnotationMethodDeclaration
import lombok.ast.ClassDeclaration
import lombok.ast.ConstructorDeclaration
import lombok.ast.ConstructorInvocation
import lombok.ast.EnumConstant
import lombok.ast.Expression
import lombok.ast.LabelledStatement
import lombok.ast.MethodDeclaration
import lombok.ast.MethodInvocation
import lombok.ast.Node
import lombok.ast.TypeDeclaration
import lombok.ast.VariableReference
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UEnumConstant
import org.jetbrains.uast.UField
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.USwitchExpression
import org.jetbrains.uast.UastContext
import java.io.File
import java.util.Collections

/**
 * A [Context] used when checking Java files.
 *
 * **NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.**
 */
open class JavaContext
/**
 * Constructs a [JavaContext] for running lint on the given file, with
 * the given scope, in the given project reporting errors to the given
 * client.
 */
(
        /** the driver running through the checks */
        driver: LintDriver,

        /** the project to run lint on which contains the given file */
        project: Project,

        /**
         * The main project if this project is a library project, or
         * null if this is not a library project. The main project is
         * the root project of all library projects, not necessarily the
         * directly including project.
         */
        main: Project?,

        /** the file to be analyzed */
        file: File) : Context(driver, project, main, file) {

    /**
     * The compilation result. Not intended for client usage; the lint infrastructure
     * will set this when a context has been processed
     */
    @Deprecated("Use {@link #uastFile} instead (see {@link UastScanner})")
    var compilationUnit: Node? = null

    /** The parse tree, when using PSI  */
    var psiFile: PsiFile? = null
        private set

    /** The parse tree, when using UAST  */
    var uastFile: UFile? = null

    /** The UAST parser which produced the parse tree  */
    var uastParser: UastParser? = null

    /** The parser which produced the parse tree  */
    @Deprecated("Use {@link #uastParser} instead (see {@link UastScanner})")
    var parser: JavaParser? = null

    /** Whether this context is in a test source folder  */
    var isTestSource: Boolean = false

    /**
     * Returns a location for the given node
     *
     * @param node the AST node to get a location for
     *
     * @return a location for the given node
     */
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Use {@link getLocation(UElement)} instead (see {@link UastScanner})")
    fun getLocation(node: Node): Location = parser!!.getLocation(this, node)

    /**
     * Returns a location for the given node range (from the starting offset of the first node to
     * the ending offset of the second node).
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
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Use {@link getRangeLocation(UElement)} instead (see {@link UastScanner})")
    fun getRangeLocation(
            from: Node,
            fromDelta: Int,
            to: Node,
            toDelta: Int): Location = parser!!.getRangeLocation(this, from, fromDelta, to, toDelta)

    /**
     * Returns a location for the given node range (from the starting offset of the first node to
     * the ending offset of the second node).
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
    fun getRangeLocation(
            from: PsiElement,
            fromDelta: Int,
            to: PsiElement,
            toDelta: Int): Location {
        return if (uastParser != null) {
            uastParser!!.getRangeLocation(this, from, fromDelta, to, toDelta)
        } else {
            parser!!.getRangeLocation(this, from, fromDelta, to, toDelta)
        }
    }

    fun getRangeLocation(
            from: UElement,
            fromDelta: Int,
            to: UElement,
            toDelta: Int): Location =
            uastParser!!.getRangeLocation(this, from, fromDelta, to, toDelta)

    // Disambiguate since UDeclarations implement both PsiElement and UElement
    fun getRangeLocation(
            from: UDeclaration,
            fromDelta: Int,
            to: UDeclaration,
            toDelta: Int): Location =
            uastParser!!.getRangeLocation(this, from as PsiElement, fromDelta, to, toDelta)

    /**
     * Returns a location for the given node range (from the starting offset of the first node to
     * the ending offset of the second node).
     *
     * @param from      the AST node to get a starting location from
     *
     * @param fromDelta Offset delta to apply to the starting offset
     *
     * @param length    The number of characters to add from the delta
     *
     * @return a location for the given node
     */
    @Suppress("unused", "unused")
    fun getRangeLocation(
            from: PsiElement,
            fromDelta: Int,
            length: Int): Location {
        return if (uastParser != null) {
            uastParser!!.getRangeLocation(this, from, fromDelta, fromDelta + length)
        } else {
            parser!!.getRangeLocation(this, from, fromDelta, fromDelta + length)
        }
    }

    fun getRangeLocation(
            from: UElement,
            fromDelta: Int,
            length: Int): Location =
            uastParser!!.getRangeLocation(this, from, fromDelta, fromDelta + length)

    /**
     * Returns a [Location] for the given node. This attempts to pick a shorter
     * location range than the entire node; for a class or method for example, it picks
     * the name node (if found). For statement constructs such as a `switch` statement
     * it will highlight the keyword, etc.
     *
     * @param node the AST node to create a location for
     *
     * @return a location for the given node
     */
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Use {@link getNameLocation(UElement)} instead (see {@link UastScanner})")
    fun getNameLocation(node: Node): Location = parser!!.getNameLocation(this, node)

    /**
     * Returns a [Location] for the given element. This attempts to pick a shorter
     * location range than the entire element; for a class or method for example, it picks
     * the name element (if found). For statement constructs such as a `switch` statement
     * it will highlight the keyword, etc.
     *
     * @param element the AST element to create a location for
     *
     * @return a location for the given element
     */
    fun getNameLocation(element: PsiElement): Location {
        return if (uastParser != null) {
            if (element is PsiSwitchStatement) {
                // Just use keyword
                return uastParser!!.getRangeLocation(this, element, 0, 6) // 6: "switch".length()
            }
            uastParser!!.getNameLocation(this, element)
        } else {
            if (element is PsiSwitchStatement) {
                // Just use keyword
                return parser!!.getRangeLocation(this, element, 0, 6) // 6: "switch".length()
            }
            parser!!.getNameLocation(this, element)
        }
    }

    /**
     * Returns a [Location] for the given element. This attempts to pick a shorter
     * location range than the entire element; for a class or method for example, it picks
     * the name element (if found). For statement constructs such as a `switch` statement
     * it will highlight the keyword, etc.
     *
     * @param element the AST element to create a location for
     *
     * @return a location for the given element
     */
    fun getNameLocation(element: UElement): Location {
        if (element is USwitchExpression) {
            // Just use keyword
            return uastParser!!.getRangeLocation(this, element, 0, 6) // 6: "switch".length()
        }
        return uastParser!!.getNameLocation(this, element)
    }

    /**
     * Returns a [Location] for the given element. This attempts to pick a shorter
     * location range than the entire element; for a class or method for example, it picks
     * the name element (if found). For statement constructs such as a `switch` statement
     * it will highlight the keyword, etc.

     *
     *
     * [UClass] is both a [PsiElement] and a [UElement] so this method
     * is here to make calling getNameLocation(UClass) easier without having to make an
     * explicit cast.
     *
     * @param cls the AST class element to create a location for
     *
     * @return a location for the given element
     */
    fun getNameLocation(cls: UClass): Location = getNameLocation(cls as UElement)

    /**
     * Returns a [Location] for the given element. This attempts to pick a shorter
     * location range than the entire element; for a class or method for example, it picks
     * the name element (if found). For statement constructs such as a `switch` statement
     * it will highlight the keyword, etc.

     *
     *
     * [UMethod] is both a [PsiElement] and a [UElement] so this method
     * is here to make calling getNameLocation(UMethod) easier without having to make an
     * explicit cast.
     *
     * @param cls the AST class element to create a location for
     *
     * @return a location for the given element
     */
    fun getNameLocation(cls: UMethod): Location = getNameLocation(cls as UElement)

    fun getLocation(node: PsiElement): Location {
        return if (uastParser != null) {
            uastParser!!.getLocation(this, node)
        } else {
            parser!!.getLocation(this, node)
        }
    }

    fun getLocation(element: UElement): Location {
        if (element is UCallExpression) {
            return uastParser!!.getCallLocation(this, element, true, true)
        }
        return uastParser!!.getLocation(this, element)
    }

    fun getLocation(element: UMethod): Location =
            uastParser!!.getLocation(this, element as PsiMethod)

    fun getLocation(element: UField): Location =
            uastParser!!.getLocation(this, element as PsiField)

    /**
     * Creates a location for the given call.
     *
     * @param call             the call to create a location range for
     *
     * @param includeReceiver  whether we should include the receiver of the method call if
     *                         applicable
     *
     * @param includeArguments whether we should include the arguments to the call
     *
     * @return a location
     */
    fun getCallLocation(
            call: UCallExpression,
            includeReceiver: Boolean,
            includeArguments: Boolean): Location =
            uastParser!!.getCallLocation(this, call, includeReceiver, includeArguments)

    val evaluator: JavaEvaluator
        get() = if (uastParser != null) uastParser!!.evaluator else parser!!.evaluator

    /**
     * Returns the [PsiJavaFile].
     *
     * @return the parsed Java source file
     */
    @Suppress("unused")
    @Deprecated("Use {@link #getPsiFile()} instead",
            replaceWith = ReplaceWith("psiFile"))
    val javaFile: PsiJavaFile?
        get() {
            return if (psiFile is PsiJavaFile) {
                psiFile as PsiJavaFile?
            } else {
                null
            }
        }

    /**
     * Sets the compilation result. Not intended for client usage; the lint infrastructure
     * will set this when a context has been processed
     *
     * @param javaFile the parse tree
     */
    fun setJavaFile(javaFile: PsiFile?) {
        this.psiFile = javaFile
    }

    override fun report(issue: Issue, location: Location,
                        message: String, quickfixData: LintFix?) {
        if (driver.isSuppressed(this, issue, psiFile)) {
            return
        }
        super.report(issue, location, message, quickfixData)
    }

    /**
     * Reports an issue applicable to a given AST node. The AST node is used as the
     * scope to check for suppress lint annotations.
     *
     * @param issue the issue to report
     *
     * @param scope the AST node scope the error applies to. The lint infrastructure
     *    will check whether there are suppress annotations on this node (or its enclosing
     *    nodes) and if so suppress the warning without involving the client.
     *
     * @param location the location of the issue, or null if not known
     *
     * @param message the message for this warning
     *
     */
    @Deprecated("use {@link #report(Issue, PsiElement, Location, String)} instead")
    fun report(
            issue: Issue,
            scope: Node?,
            location: Location,
            message: String) {
        if (scope != null && driver.isSuppressed(this, issue, scope)) {
            return
        }
        super.report(issue, location, message, null)
    }

    /**
     * Reports an issue applicable to a given AST node. The AST node is used as the
     * scope to check for suppress lint annotations.
     *
     * @param issue        the issue to report
     *
     * @param scope        the AST node scope the error applies to. The lint infrastructure will
     *                     check whether there are suppress annotations on this node (or its
     *                     enclosing nodes) and if so suppress the warning without involving the
     *                     client.
     *
     * @param location     the location of the issue, or null if not known
     *
     * @param message      the message for this warning
     *
     * @param quickfixData optional data to pass to the IDE for use by a quickfix.
     */
    @JvmOverloads fun report(
            issue: Issue,
            scope: PsiElement?,
            location: Location,
            message: String,
            quickfixData: LintFix? = null) {
        if (scope != null && driver.isSuppressed(this, issue, scope)) {
            return
        }
        super.doReport(issue, location, message, quickfixData)
    }

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Here for temporary compatibility; the new typed quickfix data parameter " +
            "should be used instead")
    fun report(
            issue: Issue,
            scope: PsiElement?,
            location: Location,
            message: String,
            quickfixData: Any?) = report(issue, scope, location, message)

    /**
     * Reports an issue applicable to a given AST node. The AST node is used as the
     * scope to check for suppress lint annotations.
     *
     * @param issue        the issue to report
     *
     * @param scope        the AST node scope the error applies to. The lint infrastructure will
     *                     check whether there are suppress annotations on this node (or its
     *                     enclosing nodes) and if so suppress the warning without involving the
     *                     client.
     *
     * @param location     the location of the issue, or null if not known
     *
     * @param message      the message for this warning
     *
     * @param quickfixData optional data to pass to the IDE for use by a quickfix.
     */
    @JvmOverloads fun report(
            issue: Issue,
            scope: UElement?,
            location: Location,
            message: String,
            quickfixData: LintFix? = null) {
        if (scope != null && driver.isSuppressed(this, issue, scope)) {
            return
        }
        super.doReport(issue, location, message, quickfixData)
    }

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Here for temporary compatibility; the new typed quickfix data parameter " +
            "should be used instead")
    fun report(
            issue: Issue,
            scope: UElement?,
            location: Location,
            message: String,
            quickfixData: Any?) = report(issue, scope, location, message)

    /**
     * [UClass] is both a [PsiElement] and a [UElement] so this method
     * is here to make calling report(..., UClass, ...) easier without having to make
     * an explicit cast.
     */
    fun report(
            issue: Issue,
            scopeClass: UClass?,
            location: Location,
            message: String) = report(issue, scopeClass as UElement?, location, message)

    /**
     * [UClass] is both a [PsiElement] and a [UElement] so this method
     * is here to make calling report(..., UClass, ...) easier without having to make
     * an explicit cast.
     */
    fun report(
            issue: Issue,
            scopeClass: UClass?,
            location: Location,
            message: String,
            quickfixData: LintFix?) = report(issue, scopeClass as UElement?, location, message, quickfixData)

    /**
     * [UMethod] is both a [PsiElement] and a [UElement] so this method
     * is here to make calling report(..., UMethod, ...) easier without having to make
     * an explicit cast.
     */
    fun report(
            issue: Issue,
            scopeClass: UMethod?,
            location: Location,
            message: String) = report(issue, scopeClass as UElement?, location, message)

    /**
     * [UMethod] is both a [PsiElement] and a [UElement] so this method
     * is here to make calling report(..., UMethod, ...) easier without having to make
     * an explicit cast.
     */
    fun report(
            issue: Issue,
            scopeClass: UMethod?,
            location: Location,
            message: String,
            quickfixData: LintFix?) =
            report(issue, scopeClass as UElement?, location, message, quickfixData)

    /**
     * [UField] is both a [PsiElement] and a [UElement] so this method
     * is here to make calling report(..., UField, ...) easier without having to make
     * an explicit cast.
     */
    fun report(
            issue: Issue,
            scopeClass: UField?,
            location: Location,
            message: String) =
            report(issue, scopeClass as UElement?, location, message)

    /**
     * [UField] is both a [PsiElement] and a [UElement] so this method
     * is here to make calling report(..., UField, ...) easier without having to make
     * an explicit cast.
     */
    fun report(
            issue: Issue,
            scopeClass: UField?,
            location: Location,
            message: String,
            quickfixData: LintFix?) =
            report(issue, scopeClass as UElement?, location, message, quickfixData)

    /**
     * Report an error.
     * Like [.report] but with
     * a now-unused data parameter at the end.
     */
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Use {@link #report(Issue, Node, Location, String)} instead; " +
            "this method is here for custom rule compatibility")
    fun report(
            issue: Issue,
            scope: Node?,
            location: Location,
            message: String,
            data: Any?) = report(issue, scope, location, message)

    override val suppressCommentPrefix: String?
        get() = SUPPRESS_JAVA_COMMENT_PREFIX

    @Deprecated("Use {@link #isSuppressedWithComment(PsiElement, Issue)} instead")
    fun isSuppressedWithComment(scope: Node, issue: Issue): Boolean {
        // Check whether there is a comment marker
        getContents() ?: return false
        val position = scope.position ?: return false

        val start = position.start
        return isSuppressedWithComment(start, issue)
    }

    fun isSuppressedWithComment(scope: PsiElement, issue: Issue): Boolean {
        if (scope is PsiCompiledElement) {
            return false
        }

        // Check whether there is a comment marker
        getContents() ?: return false
        val textRange = scope.textRange ?: return false
        val start = textRange.startOffset
        return isSuppressedWithComment(start, issue)
    }

    fun isSuppressedWithComment(scope: UElement, issue: Issue): Boolean {
        val psi = scope.psi
        return psi != null && isSuppressedWithComment(psi, issue)
    }

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Location handles aren't needed for AST nodes anymore; just use the " +
            "{@link PsiElement} from the AST")
    fun createLocationHandle(node: Node): Location.Handle =
            parser!!.createLocationHandle(this, node)

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Use PsiElement resolve methods (varies by AST node type, e.g. " +
            "{@link PsiMethodCallExpression#resolveMethod()}")
    fun resolve(node: Node): ResolvedNode? = parser!!.resolve(this, node)

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Use {@link JavaEvaluator#findClass(String)} instead")
    fun findClass(fullyQualifiedName: String): ResolvedClass? =
            parser!!.findClass(this, fullyQualifiedName)

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Use {@link PsiExpression#getType()} )} instead")
    fun getType(node: Node): TypeDescriptor? = parser!!.getType(this, node)

    /**
     * Returns true if the given method invocation node corresponds to a call on a
     * `android.content.Context`
     *
     * @param node the method call node
     *
     * @return true iff the method call is on a class extending context
     *
     */
    @Deprecated("use {@link JavaEvaluator#isMemberInSubClassOf(PsiMember, String, boolean)} " +
            "instead")
    fun isContextMethod(node: MethodInvocation): Boolean {
        // Method name used in many other contexts where it doesn't have the
        // same semantics; only use this one if we can resolve types
        // and we're certain this is the Context method
        val resolved = resolve(node)
        if (resolved is JavaParser.ResolvedMethod) {
            val containingClass = resolved.containingClass
            if (containingClass.isSubclassOf(CLASS_CONTEXT, false)) {
                return true
            }
        }
        return false
    }

    /** This method is marked as non-null but that will only be the case from
     * UastScanners. It should never be called from old PSI or Lombok scanners.  */
    val uastContext: UastContext
        get() = uastParser!!.uastContext!!

    companion object {
        @Deprecated("Use {@link PsiTreeUtil#getParentOfType(PsiElement, Class[])} " +
                "with PsiMethod.class instead")
        @JvmStatic
        fun findSurroundingMethod(scope: Node?): Node? {
            var currentScope = scope
            while (currentScope != null) {
                val type = currentScope.javaClass
                // The Lombok AST uses a flat hierarchy of node type implementation classes
                // so no need to do instanceof stuff here.
                if (type == MethodDeclaration::class.java ||
                        type == ConstructorDeclaration::class.java) {
                    return currentScope
                }

                currentScope = currentScope.parent
            }

            return null
        }

        @Deprecated("Use {@link PsiTreeUtil#getParentOfType(PsiElement, Class[])} " +
                "with PsiMethod.class instead")
        @JvmStatic
        fun findSurroundingClass(scope: Node?): ClassDeclaration? {
            var currentScope = scope
            while (currentScope != null) {
                val type = currentScope.javaClass
                // The Lombok AST uses a flat hierarchy of node type implementation classes
                // so no need to do instanceof stuff here.
                if (type == ClassDeclaration::class.java) {
                    return currentScope as ClassDeclaration?
                }

                currentScope = currentScope.parent
            }

            return null
        }

        @Suppress("DeprecatedCallableAddReplaceWith")
        @Deprecated("Use {@link #getMethodName(PsiElement)} instead")
        @JvmStatic
        fun getMethodName(call: Node): String? =
                when (call) {
                    is MethodInvocation -> call.astName().astValue()
                    is ConstructorInvocation -> call.astTypeReference().typeName
                    is EnumConstant -> call.astName().astValue()
                    else -> null
                }

        // Not necessary in UAST
        @JvmStatic
        fun getMethodName(call: PsiElement): String? =
                when (call) {
                    is PsiMethodCallExpression -> call.methodExpression.referenceName
                    is PsiNewExpression -> call.classReference?.referenceName
                    is PsiEnumConstant -> call.name
                    else -> null
                }

        // TODO: Move to LintUtils etc
        @JvmStatic
        fun getMethodName(call: UElement): String? =
                when (call) {
                    is UEnumConstant -> call.name
                    is UCallExpression -> call.methodName ?: call.classReference?.resolvedName
                    else -> null
                }

        /**
         * Searches for a name node corresponding to the given node
         * @return the name node to use, if applicable
         *
         */
        @Deprecated("Use {@link #findNameElement(PsiElement)} instead")
        @JvmStatic
        fun findNameNode(node: Node): Node? =
                when (node) {
                    is TypeDeclaration ->
                        // ClassDeclaration, AnnotationDeclaration, EnumDeclaration,
                        // InterfaceDeclaration
                        node.astName()
                    is MethodDeclaration -> node.astMethodName()
                    is ConstructorDeclaration -> node.astTypeName()
                    is MethodInvocation -> node.astName()
                    is ConstructorInvocation -> node.astTypeReference()
                    is EnumConstant -> node.astName()
                    is AnnotationElement -> node.astName()
                    is AnnotationMethodDeclaration -> node.astMethodName()
                    is VariableReference -> node.astIdentifier()
                    is LabelledStatement -> node.astLabel()
                    else -> null
                }

        /**
         * Searches for a name node corresponding to the given node
         * @return the name node to use, if applicable
         */
        @JvmStatic
        fun findNameElement(element: PsiElement): PsiElement? {
            when (element) {
                is PsiClass -> {
                    if (element is PsiAnonymousClass) {
                        return element.baseClassReference
                    }
                    return element.nameIdentifier
                }
                is PsiMethod -> return element.nameIdentifier
                is PsiMethodCallExpression -> return element.methodExpression.referenceNameElement
                is PsiNewExpression -> return element.classReference
                is PsiField -> return element.nameIdentifier
                is PsiAnnotation -> return element.nameReferenceElement
                is PsiReferenceExpression -> return element.referenceNameElement
                is PsiLabeledStatement -> return element.labelIdentifier
                else -> return null
            }

        }

        @JvmStatic
        fun findNameElement(element: UElement): UElement? {
            if (element is UDeclaration) {
                return element.uastAnchor
                //} else if (element instanceof PsiNameIdentifierOwner) {
                //    return ((PsiNameIdentifierOwner) element).getNameIdentifier();
            } else if (element is UCallExpression) {
                return element.methodIdentifier
            }

            return null
        }

        @Suppress("DeprecatedCallableAddReplaceWith")
        @Deprecated("")
        @JvmStatic
        fun getParameters(call: Node): Iterator<Expression> = when (call) {
            is MethodInvocation -> call.astArguments().iterator()
            is ConstructorInvocation -> call.astArguments().iterator()
            is EnumConstant -> call.astArguments().iterator()
            else -> Collections.emptyIterator<Expression>()
        }

        @Suppress("DeprecatedCallableAddReplaceWith")
        @Deprecated("")
        @JvmStatic
        fun getParameter(call: Node, parameter: Int): Node? {
            val iterator = getParameters(call)

            for (i in 0 until parameter - 1) {
                if (!iterator.hasNext()) {
                    return null
                }
                iterator.next()
            }
            return if (iterator.hasNext()) iterator.next() else null
        }

        /**
         * Returns the first ancestor node of the given type
         *
         * @param element the element to search from
         *
         * @param clz     the target node type
         *
         * @param <T>     the target node type
         *
         * @return the nearest ancestor node in the parent chain, or null if not found
         *
         */
        @Suppress("DeprecatedCallableAddReplaceWith")
        @Deprecated("Use {@link PsiTreeUtil#getParentOfType} instead")
        @JvmStatic
        fun <T : Node> getParentOfType(
                element: Node?,
                clz: Class<T>): T? = getParentOfType(element, clz, true)

        /**
         * Returns the first ancestor node of the given type
         *
         * @param element the element to search from
         *
         * @param clz     the target node type
         *
         * @param strict  if true, do not consider the element itself, only its parents
         *
         * @param <T>     the target node type
         *
         * @return the nearest ancestor node in the parent chain, or null if not found
         *
         */
        @Deprecated("Use {@link PsiTreeUtil#getParentOfType} instead")
        @JvmStatic
        fun <T : Node> getParentOfType(
                element: Node?,
                clz: Class<T>,
                strict: Boolean): T? {
            var current: Node? = element ?: return null

            if (strict) {
                current = current?.parent
            }

            while (current != null) {
                if (clz.isInstance(current)) {
                    @Suppress("UNCHECKED_CAST")
                    return current as T?
                }
                current = current.parent
            }

            return null
        }

        /**
         * Returns the first ancestor node of the given type, stopping at the given type
         *
         * @param element     the element to search from
         *
         * @param clz         the target node type
         *
         * @param strict      if true, do not consider the element itself, only its parents
         *
         * @param terminators optional node types to terminate the search at
         *
         * @param <T>         the target node type
         *
         * @return the nearest ancestor node in the parent chain, or null if not found
         *
         */
        @Deprecated("Use {@link PsiTreeUtil#getParentOfType} instead")
        @JvmStatic
        fun <T : Node> getParentOfType(element: Node?,
                                       clz: Class<T>,
                                       strict: Boolean,
                                       vararg terminators: Class<out Node>): T? {
            var current: Node? = element ?: return null
            if (strict) {
                current = current?.parent
            }

            while (current != null && !clz.isInstance(current)) {
                for (terminator in terminators) {
                    if (terminator.isInstance(current)) {
                        return null
                    }
                }
                current = current.parent
            }

            @Suppress("UNCHECKED_CAST")
            return current as T?
        }

        /**
         * Returns the first sibling of the given node that is of the given class
         *
         * @param sibling the sibling to search from
         *
         * @param clz     the type to look for
         *
         * @param <T>     the type
         *
         * @return the first sibling of the given type, or null
         *
         */
        @Deprecated("Use {@link PsiTreeUtil#getNextSiblingOfType(PsiElement, Class)} instead")
        @JvmStatic
        fun <T : Node> getNextSiblingOfType(sibling: Node?,
                                            clz: Class<T>): T? {
            if (sibling == null) {
                return null
            }
            val parent = sibling.parent ?: return null

            val iterator = parent.children.iterator()
            while (iterator.hasNext()) {
                if (iterator.next() === sibling) {
                    break
                }
            }

            while (iterator.hasNext()) {
                val child = iterator.next()
                if (clz.isInstance(child)) {
                    @Suppress("UNCHECKED_CAST")
                    return child as T
                }

            }

            return null
        }

        /**
         * Returns the given argument of the given call
         *
         * @param call the call containing arguments
         *
         * @param index the index of the target argument
         *
         * @return the argument at the given index
         *
         * @throws IllegalArgumentException if index is outside the valid range
         */
        @Deprecated("")
        @JvmStatic
        fun getArgumentNode(call: MethodInvocation, index: Int): Node {
            var i = 0
            @Suppress("UseWithIndex")
            for (parameter in call.astArguments()) {
                if (i == index) {
                    return parameter
                }
                i++
            }
            throw IllegalArgumentException(Integer.toString(index))
        }
    }
}