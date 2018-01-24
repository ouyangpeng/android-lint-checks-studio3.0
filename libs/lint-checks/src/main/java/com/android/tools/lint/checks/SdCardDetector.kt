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

package com.android.tools.lint.checks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.getValueIfStringLiteral

/**
 * Looks for hardcoded references to /sdcard/.
 */
class SdCardDetector : Detector(), Detector.UastScanner {
    companion object Issues {
        /** Hardcoded /sdcard/ references  */
        @JvmField val ISSUE = Issue.create(
                "SdCardPath",
                "Hardcoded reference to `/sdcard`",

                """
Your code should not reference the `/sdcard` path directly; instead use \
`Environment.getExternalStorageDirectory().getPath()`.

Similarly, do not reference the `/data/data/` path directly; it can vary in multi-user scenarios. \
Instead, use `Context.getFilesDir().getPath()`.""",

                Category.CORRECTNESS,
                6,
                Severity.WARNING,
                Implementation(
                        SdCardDetector::class.java,
                        Scope.JAVA_FILE_SCOPE))
                .addMoreInfo(
                        "http://developer.android.com/guide/topics/data/data-storage.html#filesExternal")
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>>? =
            listOf<Class<out UElement>>(ULiteralExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler? = object : UElementHandler() {
        override fun visitLiteralExpression(node: ULiteralExpression) {
            val s = node.getValueIfStringLiteral()
            if (s != null && !s.isEmpty()) {
                val c = s[0]
                if (c != '/' && c != 'f') {
                    return
                }

                if (s.startsWith("/sdcard")
                        || s.startsWith("/mnt/sdcard/")
                        || s.startsWith("/system/media/sdcard")
                        || s.startsWith("file://sdcard/")
                        || s.startsWith("file:///sdcard/")) {
                    val message = """Do not hardcode "/sdcard/"; use `Environment.getExternalStorageDirectory().getPath()` instead"""
                    val location = context.getLocation(node)
                    context.report(ISSUE, node, location, message)
                } else if (s.startsWith("/data/data/") || s.startsWith("/data/user/")) {
                    val message = """Do not hardcode "`/data/`"; use `Context.getFilesDir().getPath()` instead"""
                    val location = context.getLocation(node)
                    context.report(ISSUE, node, location, message)
                }
            }
        }
    }
}
