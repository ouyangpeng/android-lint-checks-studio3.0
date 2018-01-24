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

package com.android.tools.lint.checks

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.ClassContext
import com.android.tools.lint.detector.api.ConstantEvaluator
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.TypeEvaluator
import com.android.tools.lint.detector.api.UastLintUtils
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiVariable
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULocalVariable
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UTypeReferenceExpression

/**
 * Checks that the code is not using reflection to access hidden Android APIs
 */
class PrivateApiDetector : Detector(), Detector.UastScanner {
    companion object Issues {
        /** Using hidden/private APIs  */
        @JvmField val ISSUE = Issue.create(
                "PrivateApi",
                "Using Private APIs",

                """
Using reflection to access hidden/private Android APIs is not safe; it will often not work on \
devices from other vendors, and it may suddenly stop working (if the API is removed) or crash \
spectacularly (if the API behavior changes, since there are no guarantees for compatibility.)
""",

                Category.CORRECTNESS,
                6,
                Severity.WARNING,
                Implementation(
                        PrivateApiDetector::class.java,
                        Scope.JAVA_FILE_SCOPE))

        private const val LOAD_CLASS = "loadClass"
        private const val FOR_NAME = "forName"
        private const val GET_CLASS = "getClass"
        private const val GET_DECLARED_METHOD = "getDeclaredMethod"
        private const val ERROR_MESSAGE = "Accessing internal APIs via reflection is not " +
                "supported and may not work on all devices or in the future"
    }

    // ---- Implements JavaPsiScanner ----

    override fun getApplicableMethodNames(): List<String>? =
            listOf(FOR_NAME, LOAD_CLASS, GET_DECLARED_METHOD)

    override fun visitMethod(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val evaluator = context.evaluator
        if (LOAD_CLASS == method.name) {
            if (evaluator.isMemberInClass(method, "java.lang.ClassLoader")
                    || evaluator.isMemberInClass(method, "dalvik.system.DexFile")) {
                checkLoadClass(context, node)
            }
        } else {
            if (!evaluator.isMemberInClass(method, "java.lang.Class")) {
                return
            }
            if (GET_DECLARED_METHOD == method.name) {
                checkGetDeclaredMethod(context, node)
            } else {
                checkLoadClass(context, node)
            }
        }
    }

    private fun checkGetDeclaredMethod(context: JavaContext,
                                       call: UCallExpression) {
        val cls = getClassFromMemberLookup(call) ?: return

        if (!(cls.startsWith("com.android.") || cls.startsWith("android."))) {
            return
        }

        val arguments = call.valueArguments
        if (arguments.isEmpty()) {
            return
        }
        val methodName = ConstantEvaluator.evaluateString(context, arguments[0], false)

        val aClass = context.evaluator.findClass(cls) ?: return

        // TODO: Fields?
        val methodsByName = aClass.findMethodsByName(methodName, true)
        if (methodsByName.isEmpty()) {
            val location = context.getLocation(call)
            context.report(ISSUE, call, location, ERROR_MESSAGE)
        }
    }

    private fun checkLoadClass(context: JavaContext,
                               call: UCallExpression) {
        val arguments = call.valueArguments
        if (arguments.isEmpty()) {
            return
        }
        val value = ConstantEvaluator.evaluate(context, arguments[0]) as? String ?: return

        var isInternal = false
        if (value.startsWith("com.android.internal.")) {
            isInternal = true
        } else if (value.startsWith("com.android.") || value.startsWith("android.") &&
                !value.startsWith("android.support.")) {
            // Attempting to access internal API? Look in two places:
            //  (1) SDK class
            //  (2) API database
            val aClass = context.evaluator.findClass(value)

            if (aClass != null) { // Found in SDK: not internal
                return
            }
            val owner = ClassContext.getInternalName(value)
            val apiLookup = ApiLookup.get(context.client,
                    context.getMainProject().buildTarget) ?: return
            isInternal = !apiLookup.containsClass(owner)
        }

        if (isInternal) {
            val location = context.getLocation(call)
            context.report(ISSUE, call, location, ERROR_MESSAGE)
        }
    }

    /**
     * Given a Class#getMethodDeclaration or getFieldDeclaration etc call,
     * figure out the corresponding class name the method is being invoked on
     *
     * @param call the [Class.getDeclaredMethod] or [Class.getDeclaredField] call
     *
     * @return the fully qualified name of the class, if found
     */
    private fun getClassFromMemberLookup(call: UCallExpression): String? =
            findReflectionClass(call.receiver)

    private fun findReflectionClass(element: UElement?): String? {
        if (element is UQualifiedReferenceExpression &&
                element.selector is UCallExpression) {
            return findReflectionClass(element.selector)
        }
        if (element is UCallExpression) {
            // Inlined class lookup?
            //   foo.getClass()
            //   Class.forName(cls)
            //   loader.loadClass()

            val name = element.methodName
            if (FOR_NAME == name || LOAD_CLASS == name) {
                val arguments = element.valueArguments
                if (arguments.isNotEmpty()) {
                    return ConstantEvaluator.evaluateString(null, arguments[0], false)
                }
            } else if (GET_CLASS == name) {
                val qualifier = element.receiver
                val qualifierType = TypeEvaluator.evaluate(qualifier)
                if (qualifierType is PsiClassType) {
                    // Called getClass(): return the internal class mapping to the public class?
                    return qualifierType.canonicalText
                }
            }

            // TODO: Are there any other common reflection utility methods (from reflection
            // libraries etc) ?
        } else if (element is UReferenceExpression) {
            // Variable (local, parameter or field) reference
            //   myClass.getDeclaredMethod()
            val resolved = element.resolve()
            if (resolved is ULocalVariable) {
                val expression = UastLintUtils.findLastAssignment(resolved as PsiVariable, element)
                return findReflectionClass(expression)
            }
        } else if (element is UClassLiteralExpression) {
            // Class literal, e.g.
            //   MyClass.class
            val expression = element.expression
            if (expression is UTypeReferenceExpression) {
                return expression.getQualifiedName()
            }
            val type = element.getExpressionType()
            if (type is PsiClassType) {
                return type.canonicalText
            }
        }

        return null
    }
}
