/*
 * Copyright (C) 2013 The Android Open Source Project
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

import com.android.testutils.TestUtils
import com.android.tools.lint.checks.AbstractCheckTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Severity
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.StringWriter
import java.util.Arrays

class JarFileIssueRegistryTest : AbstractCheckTest() {
    fun testError() {
        val loggedWarnings = StringWriter()
        val client = createClient(loggedWarnings)
        getSingleRegistry(client, File("bogus"))
        assertThat(loggedWarnings.toString()).contains(
                "Could not load custom lint check jar files: bogus")
    }

    fun testCached() {
        val targetDir = TestUtils.createTempDirDeletedOnExit()
        val file1 = base64gzip("lint.jar",
                CustomRuleTest.LINT_JAR_BASE64_GZIP).createFile(targetDir)
        val file2 = jar("unsupported.jar").createFile(targetDir)
        assertTrue(file1.path, file1.exists())
        val loggedWarnings = StringWriter()
        val client = createClient(loggedWarnings)
        val registry1 = getSingleRegistry(client, file1) ?: fail()
        val registry2 = getSingleRegistry(client, File(file1.path)) ?: fail()
        assertSame(registry1, registry2)
        val registry3 = getSingleRegistry(client, file2)
        assertThat(registry3).isNull()

        assertEquals(1, registry1.issues.size)
        assertEquals("AppCompatMethod", registry1.issues[0].id)

        // Access detector state. On Java 7/8 this will access the detector class after
        // the jar loader has been closed; this tests that we still have valid classes.
        val detector = registry1.issues[0].implementation.detectorClass.newInstance()
        detector.applicableAsmNodeTypes
        val applicableCallNames = detector.applicableCallNames
        assertNotNull(applicableCallNames)
        assertTrue(applicableCallNames!!.contains("getActionBar"))

        assertEquals("Custom lint rule jar " + file2.path + " does not contain a valid "
                + "registry manifest key (Lint-Registry-v2).\n"
                + "Either the custom jar is invalid, or it uses an outdated API not "
                + "supported this lint client", loggedWarnings.toString())

        // Make sure we handle up to date checks properly too
        val composite = CompositeIssueRegistry(
                Arrays.asList<IssueRegistry>(registry1, registry2))
        assertThat(composite.isUpToDate).isTrue()

        assertThat(registry1.isUpToDate).isTrue()
        file1.setLastModified(file1.lastModified() + 2000)
        assertThat(registry1.isUpToDate).isFalse()
        assertThat(composite.isUpToDate).isFalse()
    }

    fun testDeduplicate() {
        val targetDir = TestUtils.createTempDirDeletedOnExit()
        val file1 = base64gzip("lint1.jar",
                CustomRuleTest.LINT_JAR_BASE64_GZIP).createFile(targetDir)
        val file2 = base64gzip("lint2.jar",
                CustomRuleTest.LINT_JAR_BASE64_GZIP).createFile(targetDir)
        assertTrue(file1.path, file1.exists())
        assertTrue(file2.path, file2.exists())

        val loggedWarnings = StringWriter()
        val client = createClient(loggedWarnings)

        val registries = JarFileIssueRegistry.get(client, listOf(file1, file2))
        // Only *one* registry should have been computed, since the two provide the same lint
        // class names!
        assertThat(registries.size).isEqualTo(1)
    }

    override fun getDetector(): Detector? {
        fail("Not used in this test")
        return null
    }

    private fun getSingleRegistry(client: LintClient, file: File): JarFileIssueRegistry? {
        val list = listOf(file)
        val registries = JarFileIssueRegistry.get(client, list)
        return if (registries.size == 1) registries[0] else null
    }

    private fun createClient(loggedWarnings: StringWriter): LintClient {
        return object : TestLintClient() {
            override fun log(exception: Throwable?, format: String?, vararg args: Any) {
                if (format != null) {
                    loggedWarnings.append(String.format(format, *args))
                }
            }

            override fun log(severity: Severity, exception: Throwable?,
                    format: String?, vararg args: Any) {
                if (format != null) {
                    loggedWarnings.append(String.format(format, *args))
                }
            }
        }
    }

    private fun fail(): Nothing {
        error("Test failed")
    }
}
