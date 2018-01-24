/*
 * Copyright (C) 2014 The Android Open Source Project
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

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Detector.UastScanner
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiKeyword
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UPolyadicExpression
import org.jetbrains.uast.isNullLiteral
import org.jetbrains.uast.java.JavaUAssertExpression

/**
 * Looks for assertion usages.
 */
/** Constructs a new [AssertDetector] check  */
class AssertDetector : Detector(), UastScanner {
    companion object Issues {
        /** Using assertions  */
        @JvmField val ISSUE = Issue.create(
                "Assert",
                "Assertions",

                """
Assertions are not checked at runtime. There are ways to request that they be used by Dalvik \
(`adb shell setprop debug.assert 1`), but note that this is not implemented in ART (the newer \
runtime), and even in Dalvik the property is ignored in many places and can not be relied upon. \
Instead, perform conditional checking inside `if (BuildConfig.DEBUG) { }` blocks. That constant \
is a static final boolean which is true in debug builds and false in release builds, and the \
Java compiler completely removes all code inside the if-body from the app.

For example, you can replace `assert speed > 0` with `if (BuildConfig.DEBUG && !(speed > 0)) { \
throw new AssertionError() }`.

(Note: This lint check does not flag assertions purely asserting nullness or non-nullness; these \
are typically more intended for tools usage than runtime checks.)""",

                Category.CORRECTNESS,
                6,
                Severity.WARNING,
                Implementation(
                        AssertDetector::class.java,
                        Scope.JAVA_FILE_SCOPE))
                .addMoreInfo("https://code.google.com/p/android/issues/detail?id=65183")
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>>? =
            listOf<Class<out UElement>>(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        if (!context.getMainProject().isAndroidProject) {
            return null
        }

        return object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                if (node is JavaUAssertExpression) {
                    checkAssertion(node, context)
                }
            }
        }
    }

    private fun checkAssertion(expression: JavaUAssertExpression,
                               context: JavaContext) {
        // Allow "assert true"; it's basically a no-op
        val condition = expression.condition
        if (condition is ULiteralExpression) {
            val value = condition.value
            if (java.lang.Boolean.TRUE == value) {
                return
            }
        } else {
            // Allow assertions of the form "assert foo != null" because they are often used
            // to make statements to tools about known nullness properties. For example,
            // findViewById() may technically return null in some cases, but a developer
            // may know that it won't be when it's called correctly, so the assertion helps
            // to clear nullness warnings.
            if (isNullCheck(condition)) {
                return
            }
        }

        // Tracking bug for ART: b/18833580
        val message = "Assertions are unreliable in Dalvik and unimplemented in ART. " +
                "Use `BuildConfig.DEBUG` conditional checks instead."

        // Attempt to just get the assert keyword location
        val location: Location
        val firstChild = expression.psi.firstChild
        location = if (firstChild is PsiKeyword && PsiKeyword.ASSERT == firstChild.getText()) {
            context.getLocation(firstChild)
        } else {
            context.getLocation(expression)
        }

        context.report(ISSUE, expression, location, message)
    }

    /**
     * Checks whether the given expression is purely a non-null check, e.g. it will return
     * true for expressions like "a != null" and "a != null && b != null" and
     * "b == null || c != null".
     */
    private fun isNullCheck(expression: UExpression): Boolean {
        if (expression is UParenthesizedExpression) {
            return isNullCheck(expression.expression)
        }
        return when (expression) {
            is UBinaryExpression -> {
                val lOperand = expression.leftOperand
                val rOperand = expression.rightOperand
                lOperand.isNullLiteral() || rOperand.isNullLiteral()
                        || isNullCheck(lOperand) && isNullCheck(rOperand)
            }
            is UPolyadicExpression -> {
                for (operand in expression.operands) {
                    if (!isNullCheck(operand)) {
                        return false
                    }
                }
                true
            }
            else -> false
        }
    }
}
