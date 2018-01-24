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

import com.android.builder.model.LintOptions
import com.google.common.annotations.Beta

/**
 * Severity of an issue found by lint
 *
 * **NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.**
 */
@Beta
enum class Severity constructor(
        /**
         * A description of this severity suitable for display to the user
         */
        val description: String) {

    /**
     * Fatal: Use sparingly because a warning marked as fatal will be
     * considered critical and will abort Export APK etc in ADT
     */
    FATAL("Fatal"),

    /**
     * Errors: The issue is known to be a real error that must be addressed.
     */
    ERROR("Error"),

    /**
     * Warning: Probably a problem.
     */
    WARNING("Warning"),

    /**
     * Information only: Might not be a problem, but the check has found
     * something interesting to say about the code.
     */
    INFORMATIONAL("Information"),

    /**
     * Ignore: The user doesn't want to see this issue
     */
    IGNORE("Ignore");

    /** Returns true if this severity is at least an error  */
    val isError: Boolean
        get() = this == ERROR || this == FATAL

    companion object {
        /**
         * Looks up the severity corresponding to a given named severity. The severity
         * string should be one returned by [.toString]
         *
         * @param name the name to look up
         *
         * @return the corresponding severity, or null if it is not a valid severity name
         */
        @JvmStatic
        fun fromName(name: String): Severity? {
            for (severity in values()) {
                if (severity.name.equals(name, ignoreCase = true)) {
                    return severity
                }
            }

            return null
        }

        /**
         * Returns a severity corresponding to the various [LintOptions] severity
         * constants
         *
         * @param severity the [LintOptions] severity constant, such as
         * [LintOptions.SEVERITY_FATAL]
         *
         * @return the corresponding lint [Severity]
         */
        @JvmStatic
        fun fromLintOptionSeverity(severity: Int): Severity =
                when (severity) {
                    LintOptions.SEVERITY_FATAL -> Severity.FATAL
                    LintOptions.SEVERITY_ERROR -> Severity.ERROR
                    LintOptions.SEVERITY_WARNING -> Severity.WARNING
                    LintOptions.SEVERITY_INFORMATIONAL -> Severity.INFORMATIONAL
                    LintOptions.SEVERITY_IGNORE -> Severity.IGNORE
                    else -> Severity.IGNORE
                }

        /**
         * Returns the smallest / least severe of the two given severities
         *
         * @param severity1 the first severity to compare
         * @param severity2 the second severity to compare
         * @return the least severe of the given severities
         */
        @JvmStatic
        fun min(severity1: Severity, severity2: Severity): Severity =
                // Using ">" instead of "<" here because compareTo is inherited from
                // enum and the severity constants are in descending order of severity
                if (severity1 > severity2) severity1 else severity2

        /**
         * Returns the largest / most severe of the two given severities
         *
         * @param severity1 the first severity to compare
         * @param severity2 the second severity to compare*
         * @return the most severe of the given severities
         */
        @JvmStatic
        fun max(severity1: Severity, severity2: Severity): Severity =
                // Using "<" instead of ">" here because compareTo is inherited from
                // enum and the severity constants are in descending order of severity
                if (severity1 < severity2) severity1 else severity2
    }
}