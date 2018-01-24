/*
 * Copyright (C) 2015 The Android Open Source Project
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
import com.android.tools.lint.detector.api.ConstantEvaluator
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression

/**
 * Makes sure that alarms are handled correctly
 */
class AlarmDetector : Detector(), Detector.UastScanner {
    companion object Issues {
        private val IMPLEMENTATION = Implementation(
                AlarmDetector::class.java,
                Scope.JAVA_FILE_SCOPE)

        /** Alarm set too soon/frequently   */
        @JvmField val ISSUE = Issue.create(
                "ShortAlarm",
                "Short or Frequent Alarm",

                """
Frequent alarms are bad for battery life. As of API 22, the `AlarmManager` will override \
near-future and high-frequency alarm requests, delaying the alarm at least 5 seconds into the \
future and ensuring that the repeat interval is at least 60 seconds.

If you really need to do work sooner than 5 seconds, post a delayed message or runnable to a \
Handler.""",

                Category.CORRECTNESS,
                6,
                Severity.WARNING,
                IMPLEMENTATION)
    }

    override fun getApplicableMethodNames(): List<String>? = listOf("setRepeating")

    override fun visitMethod(context: JavaContext, node: UCallExpression,
                             method: PsiMethod) {
        val evaluator = context.evaluator
        if (evaluator.isMemberInClass(method, "android.app.AlarmManager") &&
                evaluator.getParameterCount(method) == 4) {
            ensureAtLeast(context, node, 1, 5000L)
            ensureAtLeast(context, node, 2, 60000L)
        }
    }

    private fun ensureAtLeast(context: JavaContext,
                              node: UCallExpression, parameter: Int, min: Long) {
        val argument = node.valueArguments[parameter]
        val value = getLongValue(context, argument)
        if (value < min) {
            val message = "Value will be forced up to $min as of Android 5.1; " +
                    "don't rely on this to be exact"
            context.report(ISSUE, argument, context.getLocation(argument), message)
        }
    }

    private fun getLongValue(
            context: JavaContext,
            argument: UExpression): Long {
        val value = ConstantEvaluator.evaluate(context, argument)
        if (value is Number) {
            return value.toLong()
        }

        return java.lang.Long.MAX_VALUE
    }
}
