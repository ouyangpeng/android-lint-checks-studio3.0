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


import com.android.tools.lint.client.api.JavaParser.TYPE_OBJECT
import com.android.tools.lint.client.api.JavaParser.TYPE_STRING
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

/**
 * Ensures that addJavascriptInterface is not called for API levels below 17.
 */
class AddJavascriptInterfaceDetector : Detector(), Detector.UastScanner {
    companion object {
        @JvmField val ISSUE = Issue.create(
                "AddJavascriptInterface",
                "addJavascriptInterface Called",
"""
For applications built for API levels below 17, `WebView#addJavascriptInterface` presents a \
security hazard as JavaScript on the target web page has the ability to use reflection to access \
the injected object's public fields and thus manipulate the host application in unintended ways.
""",
                Category.SECURITY,
                9,
                Severity.WARNING,
                Implementation(
                        AddJavascriptInterfaceDetector::class.java,
                        Scope.JAVA_FILE_SCOPE)).addMoreInfo(
                "https://labs.mwrinfosecurity.com/blog/2013/09/24/webview-addjavascriptinterface-remote-code-execution/")

        const val WEB_VIEW = "android.webkit.WebView"
        const val ADD_JAVASCRIPT_INTERFACE = "addJavascriptInterface"
    }

    // ---- Implements UastScanner ----

    override fun getApplicableMethodNames(): List<String>? = listOf(ADD_JAVASCRIPT_INTERFACE)

    override fun visitMethod(context: JavaContext, node: UCallExpression,
                             method: PsiMethod) {
        // Ignore the issue if we never build for any API less than 17.
        if (context.getMainProject().minSdk >= 17) {
            return
        }

        val evaluator = context.evaluator
        if (!evaluator.methodMatches(method, WEB_VIEW, true, TYPE_OBJECT, TYPE_STRING)) {
            return
        }

        val message = "`WebView.addJavascriptInterface` should not be called with " +
                "minSdkVersion < 17 for security reasons: JavaScript can use reflection " +
                "to manipulate application"
        context.report(ISSUE, node, context.getNameLocation(node), message)
    }
}
