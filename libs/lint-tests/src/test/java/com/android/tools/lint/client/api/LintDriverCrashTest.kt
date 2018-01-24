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

package com.android.tools.lint.client.api

import com.android.tools.lint.checks.AbstractCheckTest
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LayoutDetector
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UFile
import org.w3c.dom.Element

class LintDriverCrashTest : AbstractCheckTest() {
    fun testLintDriverError() {
        // Regression test for 34248502
        lint().files(
                xml("res/layout/foo.xml", "<LinearLayout/>"),
                java("""
package test.pkg;
@SuppressWarnings("ALL") class Foo {
}
"""))
                .allowSystemErrors(true)
                .run()
                // Checking for manual substrings instead of doing an actual equals check
                // since the stacktrace contains a number of specific line numbers from
                // the lint implementation, including this test, which keeps shifting every
                // time there is an edit
                .check {
                    it.contains("Foo.java: Error: Unexpected failure during lint analysis of Foo.java (this is a bug in lint or one of the libraries it depends on)")
                    it.contains("You can set environment variable LINT_PRINT_STACKTRACE=true to dump a full stacktrace to stdout. [LintError]")
                    it.contains("ArithmeticException:LintDriverCrashTest\$CrashingDetector\$createUastHandler$1.visitFile(LintDriverCrashTest.kt:")
                    it.contains("1 errors, 0 warnings")
                }
        LintDriver.clearCrashCount()
    }

    override fun getIssues(): List<Issue> = listOf(CrashingDetector.CRASHING_ISSUE,
            DisposedThrowingDetector.DISPOSED_ISSUE)

    override fun getDetector(): Detector = CrashingDetector()

    class CrashingDetector : Detector(), Detector.UastScanner {

        override fun getApplicableUastTypes(): List<Class<out UElement>>? =
                listOf<Class<out UElement>>(UFile::class.java)

        override fun createUastHandler(context: JavaContext): UElementHandler? =
                object : UElementHandler() {
                    override fun visitFile(uFile: UFile) {
                        @Suppress("DIVISION_BY_ZERO", "UNUSED_VARIABLE") // Intentional crash
                        val x = 1 / 0
                        super.visitFile(uFile)
                    }
                }

        companion object {
            val CRASHING_ISSUE = Issue
                    .create("_TestCrash", "test", "test", Category.LINT, 10, Severity.FATAL,
                            Implementation(CrashingDetector::class.java, Scope.JAVA_FILE_SCOPE))
        }
    }

    class DisposedThrowingDetector : LayoutDetector(), Detector.XmlScanner {

        override fun getApplicableElements(): Collection<String> {
            return arrayListOf("LinearLayout")
        }

        override fun visitElement(context: XmlContext, element: Element) {
            throw AssertionError("Already disposed: " + this)
        }

        companion object {
            val DISPOSED_ISSUE = Issue.create("_TestDisposed", "test", "test", Category.LINT,
                    10, Severity.FATAL,
                    Implementation(DisposedThrowingDetector::class.java,
                            Scope.RESOURCE_FILE_SCOPE))
        }
    }
}
