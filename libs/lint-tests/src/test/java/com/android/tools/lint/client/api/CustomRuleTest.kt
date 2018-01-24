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

import com.android.tools.lint.checks.infrastructure.TestFiles.base64gzip
import com.android.tools.lint.checks.infrastructure.TestFiles.classpath
import com.android.tools.lint.checks.infrastructure.TestFiles.gradle
import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.manifest
import com.android.tools.lint.checks.infrastructure.TestLintClient
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Project
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito
import java.io.File
import java.nio.file.Files

class CustomRuleTest {

    @Test
    fun testProjectLintJar() {
        val expected = "" +
                "src/test/pkg/AppCompatTest.java:7: Warning: Should use getSupportActionBar instead of getActionBar name [AppCompatMethod]\n" +
                "        getActionBar();                    // ERROR\n" + "        ~~~~~~~~~~~~\n" +
                "src/test/pkg/AppCompatTest.java:10: Warning: Should use startSupportActionMode instead of startActionMode name [AppCompatMethod]\n" +
                "        startActionMode(null);             // ERROR\n" + "        ~~~~~~~~~~~~~~~\n" +
                "src/test/pkg/AppCompatTest.java:13: Warning: Should use supportRequestWindowFeature instead of requestWindowFeature name [AppCompatMethod]\n" +
                "        requestWindowFeature(0);           // ERROR\n" +
                "        ~~~~~~~~~~~~~~~~~~~~\n" +
                "src/test/pkg/AppCompatTest.java:16: Warning: Should use setSupportProgressBarVisibility instead of setProgressBarVisibility name [AppCompatMethod]\n" +
                "        setProgressBarVisibility(true);    // ERROR\n" +
                "        ~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                "src/test/pkg/AppCompatTest.java:17: Warning: Should use setSupportProgressBarIndeterminate instead of setProgressBarIndeterminate name [AppCompatMethod]\n" +
                "        setProgressBarIndeterminate(true);\n" +
                "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                "src/test/pkg/AppCompatTest.java:18: Warning: Should use setSupportProgressBarIndeterminateVisibility instead of setProgressBarIndeterminateVisibility name [AppCompatMethod]\n" +
                "        setProgressBarIndeterminateVisibility(true);\n" +
                "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" + "0 errors, 6 warnings\n"

        lint().files(
                classpath(),
                manifest().minSdk(1),
                appCompatTestSource,
                appCompatTestClass)
                .allowObsoleteLintChecks(false)
                .client(object : TestLintClient() {
                    override fun findGlobalRuleJars(): List<File> = emptyList()

                    override fun findRuleJars(project: Project): List<File> = listOf(lintJar)
                }).customRules(lintJar).allowMissingSdk().run().expect(expected)
    }

    @Test
    fun testProjectLintJarWithServiceRegistry() {
        // Like testProjectLintJar, but the custom rules are loaded via
        // META-INF/services/*IssueRegistry loading instead of a manifest key
        val expected = "" +
                "src/test/pkg/AppCompatTest.java:7: Warning: Should use getSupportActionBar instead of getActionBar name [AppCompatMethod]\n" +
                "        getActionBar();                    // ERROR\n" + "        ~~~~~~~~~~~~\n" +
                "src/test/pkg/AppCompatTest.java:10: Warning: Should use startSupportActionMode instead of startActionMode name [AppCompatMethod]\n" +
                "        startActionMode(null);             // ERROR\n" + "        ~~~~~~~~~~~~~~~\n" +
                "src/test/pkg/AppCompatTest.java:13: Warning: Should use supportRequestWindowFeature instead of requestWindowFeature name [AppCompatMethod]\n" +
                "        requestWindowFeature(0);           // ERROR\n" +
                "        ~~~~~~~~~~~~~~~~~~~~\n" +
                "src/test/pkg/AppCompatTest.java:16: Warning: Should use setSupportProgressBarVisibility instead of setProgressBarVisibility name [AppCompatMethod]\n" +
                "        setProgressBarVisibility(true);    // ERROR\n" +
                "        ~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                "src/test/pkg/AppCompatTest.java:17: Warning: Should use setSupportProgressBarIndeterminate instead of setProgressBarIndeterminate name [AppCompatMethod]\n" +
                "        setProgressBarIndeterminate(true);\n" +
                "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                "src/test/pkg/AppCompatTest.java:18: Warning: Should use setSupportProgressBarIndeterminateVisibility instead of setProgressBarIndeterminateVisibility name [AppCompatMethod]\n" +
                "        setProgressBarIndeterminateVisibility(true);\n" +
                "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" + "0 errors, 6 warnings\n"

        lint().files(
                classpath(),
                manifest().minSdk(1),
                appCompatTestSource,
                appCompatTestClass)
                .allowObsoleteLintChecks(false)
                .client(object : TestLintClient() {
                    override fun findGlobalRuleJars(): List<File> = emptyList()

                    override fun findRuleJars(project: Project): List<File> = listOf(
                            lintJarWithServiceRegistry)
                }).customRules(lintJar).allowMissingSdk().run().expect(expected)
    }

    @Test
    fun testProjectIsLibraryLintJar() {
        val expected = "" +
                "src/main/java/test/pkg/AppCompatTest.java:7: Warning: Should use getSupportActionBar instead of getActionBar name [AppCompatMethod]\n" +
                "        getActionBar();                    // ERROR\n" +
                "        ~~~~~~~~~~~~\n" +
                "src/main/java/test/pkg/AppCompatTest.java:10: Warning: Should use startSupportActionMode instead of startActionMode name [AppCompatMethod]\n" +
                "        startActionMode(null);             // ERROR\n" +
                "        ~~~~~~~~~~~~~~~\n" +
                "src/main/java/test/pkg/AppCompatTest.java:13: Warning: Should use supportRequestWindowFeature instead of requestWindowFeature name [AppCompatMethod]\n" +
                "        requestWindowFeature(0);           // ERROR\n" +
                "        ~~~~~~~~~~~~~~~~~~~~\n" +
                "src/main/java/test/pkg/AppCompatTest.java:16: Warning: Should use setSupportProgressBarVisibility instead of setProgressBarVisibility name [AppCompatMethod]\n" +
                "        setProgressBarVisibility(true);    // ERROR\n" +
                "        ~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                "src/main/java/test/pkg/AppCompatTest.java:17: Warning: Should use setSupportProgressBarIndeterminate instead of setProgressBarIndeterminate name [AppCompatMethod]\n" +
                "        setProgressBarIndeterminate(true);\n" +
                "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                "src/main/java/test/pkg/AppCompatTest.java:18: Warning: Should use setSupportProgressBarIndeterminateVisibility instead of setProgressBarIndeterminateVisibility name [AppCompatMethod]\n" +
                "        setProgressBarIndeterminateVisibility(true);\n" +
                "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                "0 errors, 6 warnings\n"
        lint().files(
                classpath(),
                manifest().minSdk(1),
                gradle(""
                        + "apply plugin: 'com.android.library'\n"
                        + "dependencies {\n"
                        + "    compile 'my.test.group:artifact:1.0'\n"
                        + "}\n"),

                appCompatTestSource,
                appCompatTestClass)
                .incremental("bin/classes/test/pkg/AppCompatTest.class")
                .allowDelayedIssueRegistration()
                .issueIds("AppCompatMethod")
                .allowObsoleteLintChecks(false)
                .modifyGradleMocks { _, variant ->
                    val dependencies = variant.mainArtifact.dependencies
                    val library = dependencies.libraries.iterator().next()
                    Mockito.`when`(library.lintJar).thenReturn(lintJar)
                }.allowMissingSdk().run().expect(expected)
    }

    @Test
    fun `Load lint custom rules from locally packaged lint jars (via lintChecks)`() {
        // Regression test for https://issuetracker.google.com/65941946
        // Copy lint.jar into build/intermediates/lint and make sure the custom rules
        // are picked up in a Gradle project

        val expected = "" +
                "src/main/java/test/pkg/AppCompatTest.java:7: Warning: Should use getSupportActionBar instead of getActionBar name [AppCompatMethod]\n" +
                "        getActionBar();                    // ERROR\n" +
                "        ~~~~~~~~~~~~\n" +
                "src/main/java/test/pkg/AppCompatTest.java:10: Warning: Should use startSupportActionMode instead of startActionMode name [AppCompatMethod]\n" +
                "        startActionMode(null);             // ERROR\n" +
                "        ~~~~~~~~~~~~~~~\n" +
                "src/main/java/test/pkg/AppCompatTest.java:13: Warning: Should use supportRequestWindowFeature instead of requestWindowFeature name [AppCompatMethod]\n" +
                "        requestWindowFeature(0);           // ERROR\n" +
                "        ~~~~~~~~~~~~~~~~~~~~\n" +
                "src/main/java/test/pkg/AppCompatTest.java:16: Warning: Should use setSupportProgressBarVisibility instead of setProgressBarVisibility name [AppCompatMethod]\n" +
                "        setProgressBarVisibility(true);    // ERROR\n" +
                "        ~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                "src/main/java/test/pkg/AppCompatTest.java:17: Warning: Should use setSupportProgressBarIndeterminate instead of setProgressBarIndeterminate name [AppCompatMethod]\n" +
                "        setProgressBarIndeterminate(true);\n" +
                "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                "src/main/java/test/pkg/AppCompatTest.java:18: Warning: Should use setSupportProgressBarIndeterminateVisibility instead of setProgressBarIndeterminateVisibility name [AppCompatMethod]\n" +
                "        setProgressBarIndeterminateVisibility(true);\n" +
                "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                "0 errors, 6 warnings\n"

        // Copy lint.jar into build/intermediates/lint/
        val listener = object : LintListener {
            override fun update(driver: LintDriver,
                    type: LintListener.EventType,
                    project: Project?,
                    context: Context?) {
                if (type == LintListener.EventType.REGISTERED_PROJECT) {
                    val buildFolder = project?.gradleProjectModel?.buildFolder ?: return
                    val lintFolder = File(buildFolder, "intermediates/lint")
                    lintFolder.mkdirs()
                    lintJar.copyTo(File(lintFolder, lintJar.name))
                }
            }
        }
        lint().files(
                classpath(),
                manifest().minSdk(1),
                gradle(""
                        + "apply plugin: 'com.android.library'\n"
                        + "dependencies {\n"
                        + "    compile 'my.test.group:artifact:1.0'\n"
                        + "}\n"),

                appCompatTestSource,
                appCompatTestClass)
                .incremental("bin/classes/test/pkg/AppCompatTest.class")
                .allowDelayedIssueRegistration()
                .issueIds("AppCompatMethod")
                .allowObsoleteLintChecks(false)
                .listener(listener)
                .allowMissingSdk()
                .run()
                .expect(expected)
    }

    @Test
    fun testGlobalLintJar() {
        val expected = "" +
                "src/test/pkg/AppCompatTest.java:7: Warning: Should use getSupportActionBar instead of getActionBar name [AppCompatMethod]\n" +
                "        getActionBar();                    // ERROR\n" + "        ~~~~~~~~~~~~\n" +
                "src/test/pkg/AppCompatTest.java:10: Warning: Should use startSupportActionMode instead of startActionMode name [AppCompatMethod]\n" +
                "        startActionMode(null);             // ERROR\n" + "        ~~~~~~~~~~~~~~~\n" +
                "src/test/pkg/AppCompatTest.java:13: Warning: Should use supportRequestWindowFeature instead of requestWindowFeature name [AppCompatMethod]\n" +
                "        requestWindowFeature(0);           // ERROR\n" +
                "        ~~~~~~~~~~~~~~~~~~~~\n" +
                "src/test/pkg/AppCompatTest.java:16: Warning: Should use setSupportProgressBarVisibility instead of setProgressBarVisibility name [AppCompatMethod]\n" +
                "        setProgressBarVisibility(true);    // ERROR\n" +
                "        ~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                "src/test/pkg/AppCompatTest.java:17: Warning: Should use setSupportProgressBarIndeterminate instead of setProgressBarIndeterminate name [AppCompatMethod]\n" +
                "        setProgressBarIndeterminate(true);\n" +
                "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                "src/test/pkg/AppCompatTest.java:18: Warning: Should use setSupportProgressBarIndeterminateVisibility instead of setProgressBarIndeterminateVisibility name [AppCompatMethod]\n" +
                "        setProgressBarIndeterminateVisibility(true);\n" +
                "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" + "0 errors, 6 warnings\n"
        lint()
                .allowObsoleteLintChecks(false)
                .files(classpath(), manifest().minSdk(1), appCompatTestSource, appCompatTestClass)
                .client(object : TestLintClient() {
                    override fun findGlobalRuleJars(): List<File> = listOf(lintJar)

                    override fun findRuleJars(project: Project): List<File> = emptyList()
                }).customRules(lintJar).allowMissingSdk().run().expect(expected)
    }

    @Test
    fun testLegacyLombokJavaLintRule() {
        val expected = """
project0: Warning: Lint found one or more custom checks using its older Java API; these checks are still run in compatibility mode, but this causes duplicated parsing, and in the next version lint will no longer include this legacy mode. Make sure the following lint detectors are upgraded to the new API: googleio.demo.MyDetector [ObsoleteLintCustomCheck]
src/test/pkg/Test.java:5: Warning: Did you mean bar? [MyId]
        foo(5);
        ~~~~~~
0 errors, 2 warnings
"""

        //noinspection all // Sample code
        lint().files(
                classpath(),
                manifest().minSdk(1),
                java("" +
                        "package test.pkg;\n" +
                        "\n" +
                        "public class Test {\n" +
                        "    public void foo(int var) {\n" +
                        "        foo(5);\n" +
                        "    }\n"
                        + "}"))
                .client(object : TestLintClient() {
                    override fun findGlobalRuleJars(): List<File> = listOf(oldLintJar)

                    override fun findRuleJars(project: Project): List<File> = emptyList()
                })
                .issueIds("MyId")
                .allowMissingSdk()
                .allowObsoleteLintChecks(false)
                .allowCompilationErrors()
                .run()
                .expect(expected)
    }

    @Test
    fun testLegacyPsiJavaLintRule() {
        val expected = """
project0: Warning: Lint found one or more custom checks using its older Java API; these checks are still run in compatibility mode, but this causes duplicated parsing, and in the next version lint will no longer include this legacy mode. Make sure the following lint detectors are upgraded to the new API: com.example.google.lint.MainActivityDetector [ObsoleteLintCustomCheck]
src/test/pkg/Test.java:5: Error: Did you mean bar instead ? [MainActivityDetector]
        foo(5);
        ~~~~~~
1 errors, 1 warnings
         """

        lint().files(
                classpath(),
                manifest().minSdk(1),
                java("" +
                        "package test.pkg;\n" +
                        "\n" +
                        "public class Test {\n" +
                        "    public void foo(int var) {\n" +
                        "        foo(5);\n" +
                        "    }\n"
                        + "}"))
                .client(object : TestLintClient() {
                    override fun findGlobalRuleJars(): List<File> = listOf(psiLintJar)

                    override fun findRuleJars(project: Project): List<File> = emptyList()
                })
                .issueIds("MainActivityDetector")
                .allowMissingSdk()
                .allowCompilationErrors()
                .allowObsoleteLintChecks(false)
                .run()
                .expect(expected)
    }

    private // Sample code
    val appCompatTestSource = java("" +
                    "package test.pkg;\n" +
                    "\n" +
                    "import android.support.v7.app.ActionBarActivity;\n" + "\n" +
                    "public class AppCompatTest extends ActionBarActivity {\n" +
                    "    public void test() {\n" +
                    "        getActionBar();                    // ERROR\n" +
                    "        getSupportActionBar();             // OK\n" + "\n" +
                    "        startActionMode(null);             // ERROR\n" +
                    "        startSupportActionMode(null);      // OK\n" + "\n" +
                    "        requestWindowFeature(0);           // ERROR\n" +
                    "        supportRequestWindowFeature(0);    // OK\n" + "\n" +
                    "        setProgressBarVisibility(true);    // ERROR\n" +
                    "        setProgressBarIndeterminate(true);\n" +
                    "        setProgressBarIndeterminateVisibility(true);\n" + "\n" +
                    "        setSupportProgressBarVisibility(true); // OK\n" +
                    "        setSupportProgressBarIndeterminate(true);\n" +
                    "        setSupportProgressBarIndeterminateVisibility(true);\n" + "    }\n" +
                    "}\n")

    private val appCompatTestClass = base64gzip("bin/classes/test/pkg/AppCompatTest.class", "" +
                    "H4sIAAAAAAAAAJVU21ITQRA9E0ICcRTkjqAogmwisuIF1AASolRRFS1LqKSK" +
                    "t0kyhSNhd92dhPJb/ApfYpUPfoAfZdmzhFzKxOg+nLmd7j7d07M/f33/AeAR" +
                    "XscRYZjSMtC2d3piZzwv6555Qh/RThxRBks4Zd9VZTuoep7ra7u2aQvPszMl" +
                    "rVxnT/hmUlP6M0NsSzlK7zAMWMk8QzTrlmUCAxjmGESMYSSnHPm2elaU/pEo" +
                    "ViTDWM4tiUpe+MqsG5tR/UEFDDO57qrShkFjAgyjHNcxxsBPpG4KYpi1krlL" +
                    "2R1a08ZogmMSUwzjZHR4kVSb7VKbba+UQzczHLO4QVkFWlx6eEMZM2xbTQ81" +
                    "Jc/t1tlSVlQqRVE6TSd7UULf8xw3cYsuJvTdIfIixKHVTeQ/ROvNDgPf5riD" +
                    "RaqRLz9VqcgF5ZTd830pdNU3V2MdJI8Nb4lj2fDmGv7ed6Eb3gqHhSQVK5D6" +
                    "ne+e+DIIqIB5FaiiqoRdE7WOk3nDvc+xarhzndwDpyy19M+UI3Toc43DNrzl" +
                    "v/BaAYzFOqdWJ4uFoHnjXcUY7hOOp4a72JX7h5hNjmeGvtqf3p504tCt+iW5" +
                    "r8JX0NHfax9FTVBLHziO9LMVEQQyiGOb6vMfNx7HS0qhvwHDUMtkj+6/X+PG" +
                    "8YphugeLeoceP8wXoRk9esI4rWwaGY2DqW8Y+hoeJwhjF5u4Qsgbc46rNDJc" +
                    "axp/CZ0BmVQdIwWC8UKK1TEd4lwhNVDHQoh3C6loHfcMpAw8MPDQwGMDG63A" +
                    "G2GQOIUbonUCoyRhivbmKewirVbot7KKMaxjgriTSNPpLqbbhGYaQiN4HuIL" +
                    "4oAsItjCDnZjw8TImvE3oCvXeGsFAAA=")

    companion object {

        @JvmField
        val LINT_JAR_BASE64_GZIP = "" +
                "H4sIAAAAAAAAAJ2Xd1CTXRbGAyIfRWpCb1KlBZEuHQMECL0jTXoNhISmIEUB" +
                "RYrSW8DGCiJNxQ9pIVQpgVCWJkhoQXqTpiIsrjv7yTfi7Ox95/xxZ87z3DP3" +
                "vHfmd4xhZ8hAAIrjjy+UGQL4aYEAZAADLXMNsK6h9kUDDUNdbS0zcwkD7cMu" +
                "AOCTAa5bHwaW6KeBgUV7cfjXppL/lJ4ibnXixPpp9Azw4hIiJVWmOjiYSBde" +
                "v19MAizccVG0X0x6pJukpA8lLozoxYN1xDfFJSQMYp9/JM4QSQHGsD8oCHhK" +
                "uNnx0dbHYXxcGNW/C/l7YZTH8d/CTk+jOA4nP1ekv5frb7Lof8pCBSEQ/sjA" +
                "32QDf5EdLP8bAeuvBb5efj+O+fXVX/yNSgOBgPjDEU6Bmm6Bbi6B/kgJF18n" +
                "FCrf6nMIxxDTITksN0WKt1eANb3yXHt7MNpaKHpglNc8hWheJi7IPqbtuxu6" +
                "7Wsglcsgza+0Rg62X9SD2erAbGv7G1qN0wavRHr2euEXI5uO+lcxKaUd8eeQ" +
                "KM6e+q9rdcshhIZP+KODzxWAZBfpOBMcdJNVj487ayMm5oKgsuC0FyU1I7NL" +
                "30tQbPQrMmYIXaaLtYYgOZDF+S1FwrqF4CgjJ6UsJV4HIRjKGM4QFHFN9p5G" +
                "6eaZrH18bYyRzn5hjJHufiDoYdNrE+U7+Hv76Lsfn+4rY+hbABjWlqwNN0rH" +
                "aTyLZrhF6whZjHSuXB6zZK4s2vtKdXe5SyYQM2vt0CwFVzNxV5UOf2ewICud" +
                "slmvmAdCLI8FBvDmiLpv1cRKd94CCIZpOA4BM/0qBGQIsqtMM+/zkL74EXik" +
                "YfWSPjaiLFu8qrdnrkhnXSNJIF9sDs88kNvuBsa5yZBUEzR759iVQkJGc/RY" +
                "BmNz9CVsBOJvxm5/HquytMsuSjo4aA5UtEGD+7Tn0r3yDr1IVFr4UaRXRIr0" +
                "tVe7c+W6h17AcOAlGPMsRiHOr2exvHPEwLmjIpubbtDybv2Nsvm3Ec4709ro" +
                "1DdXu54PB0aWsElSRVU1h5e1W9tgX06nlPNxVg72qyrq8YEHp76Y1boRt0X5" +
                "qUGMoNbatTxLPp8bqgFQWVTb8LdaWOZkJLDleV4aKpdHShT3tkOLOhT+VmzZ" +
                "NHPterrFQ/utC6+T61IRmRJ9u+y2egsOstN1ibLZSNAE12X3bMeALvtKV188" +
                "7Whos+YWYdCqQ6vfM/v81P5az3yyv43wo6PbDefnzM/4kE2MxEJjmPjvLhpn" +
                "YUwYFH3kUIuPDC4L7dlZvg68PbnrjfGzQ/f7kFAtyvHq50uVCiW75JqF0MjB" +
                "kVG7H8iwlwqC2OhmW/gwIrO2oSbq60k69OdRhWyYxVGPwFdOPBal4wI4Mt7a" +
                "FpfDyyM2ASIHrVIeN+ZTvXIOa6OaVm3Z8u58KMuN0RruDdm/ub9AixiTDpr/" +
                "/OmZffDwMxuAKwMhc5OrXmCmTmqLlUVy4vH29lVwv1RBY2mqKhuhZU+rXnyW" +
                "yY558wybWusLvgiB2Xf78Ykou1ESh1yay/DevbO9/k/lnRH3R5wJvCCoEnKF" +
                "NeWQzv2jkw8CpNgKxUZZBixZ2pSF6XOFX8OaGdTCcxws892CNhTKODy9YddV" +
                "sU8SRzbM2N5JCs167NckOk5lpe8xR7jnXmtEDfNHFX8qhzSW2Qf5hHnn4jbN" +
                "IXvZReUW2d3m9rJLBevmQ1pdCQyVF0uS3ps0xL2EJY7bTr6ywIZ9YnIHJr3Y" +
                "EhNwWS70zoGVr5XzEF0l0IdhhhmyZ28s1Zr5p3X5JHd7UpOyeCj5WVSbv3Of" +
                "LKxdeStaV2jlw6xUIhGsjt3ocK6LatsdeNRw6Y2OvTqRJZHdrqCEqMDSoC4x" +
                "sv4cA/S0p3YelgzUb12Wioh1TNhPSiTMhAT6BA351ORYuH9VlFfH6ozkQ1vX" +
                "3Q5IIufeHMXdFZOS23gWBRXFOObpPMi0cL8sO58sK5gcgGJ3l3Hj8WlX1RBx" +
                "z2oXjRhK3WQ+1JRcb98GQ8jQEzRf1REfveP8/qh+HN4i1fKQJGmvKaW8TuQD" +
                "oSbcSShnNbhrbKmpYWTccyBoJ2iAvV9l0mRxniOCL8zFySB4AXfbfgfAqC9Y" +
                "bls4/ec0LXJxkud90xbgDnc0FsfIvCxb4jooJplQNjCV4b+dAZfKAwKyLmPG" +
                "53Ub0vScu2StakY38z3Yl0xiJqaUe6x6gGlKLUX8ajuN2klyg70eebTJgewq" +
                "/N31a52TaVyGK+ia1Su7D4JVqeWWKmTqzdSMJK5jXnAc7nj9QywbHj8/b/qu" +
                "6BVwwy4EZe1CS8jPzFNZoY+rkRBT1vrm1RbwPGVVl0gdG1iS/TTADmsd8sn6" +
                "0/ZXgHOYxkASkRxuhjZiRuZMjPnJwvPRK7vNkgvOhV1GbmnbQZfyO5+xVJkf" +
                "ct5U4fR8sVplX+GdmLVnrFRP+sAweGrWaVXv5gOrW7enpVQBFCm0C9/UROq6" +
                "YO4hTau0r8e/2GTJvtppqoFezPLgzHJbcYNin3JVRK8Rpx2+RHCffUzHNxPO" +
                "+IRuD6JzDe1J1/cVTNH/JXRcUX8ZCF+mKOPxtU2oiiiGg/u/QODLwrr0++KG" +
                "jRfikbcKSFVaoJNxCMFrOY8IrCrR/IURoLRtIflsBNX9PjOsl/J7kOQwiP49" +
                "Fuxf1Qslvfpk1bc4aJdfh5FFLaygONy/WH5L4oaeip6OWGx6pSWVGEMZyESz" +
                "TEA01TI1vTiY6SVHm9qoUESxyKE+No3vPWiABhgb0rEh3F/RBvF3wmsqcFvO" +
                "Gaw0JNbNTL94uyNFm+zn7KhcH++A+Xz/CBNCpPL4oMbAIKQpCLpPvyd2V6Qr" +
                "Zl/RPQORgIgCPkvXSUA8ABoKOycgOC5si5km9L2j/JrhmXZetNrobsDmOqvl" +
                "GkVjQujzC4YOaXrI/b4YmdrolqebfUJribBmobUZtiXKK5X6USRtpgJZe2kt" +
                "AcjQKDvuJhkiGHI9j3dqjuLc9a6hy08Kigik8481oAxMuOY2zuH4LWXuFmrh" +
                "aUHl5KxNkEPboEwJeMq/tRNyIBDrf6ZBwAUN0Ir2Jc9rzG+D3iaz48/noTZG" +
                "0fBAM0vn/KVdNe76ht+JVDRhT57VOvwAoQynIJxPaPeqUEcHtB6eNYkZvh85" +
                "S6+yWJF1uMrFG9vGfDVtFFjMZbJmuWZiTpyk2d7TlkMQHCWNFaOaaCaO5OLK" +
                "TSWhcgFzBpwYxvNriq5fOprHAHyowoezaBaCMIY7/pDuO3ap0baaKxzjkgLw" +
                "B3b9Gkpk/hco0UWhgtxM3Ty8UIHI6z/IJMPUwGhcnf4mta7+rfH0qvCMAbrn" +
                "d8gEMpujIfQWGX8ypg7pxSAJCmIh/e4jr0Z0vllVZVly7pMcCLeWAh8xajGp" +
                "4kILCLi8m5MJ3a9RANvQD+pMRsuhcZ/JmStZr4lgpSBwKzbiLuuz8Xz+OD08" +
                "rSBE2lQ7QkT7TA7XvWihQqYx6wX+FBFHI+y9r+XIuB6dbCf3oQcftXLf4zbT" +
                "uMozAg4gubsrwkx+anjq6+eBGXUr0KzHBR2XtFfLgi8oMl+4xWLoxc3QBvwi" +
                "eqmZia2zvhEymuSsNDNS0ns1//7ZOwOdo2OJCeabpo/qoIPsN21dXx6UHgyL" +
                "HU16t8BiinkpNpRbqRzIrGk2GStY5biD1ckj8BEvLVMHeoKrufKP9MLR6KTH" +
                "tYT1yK+tlYcADtZ5f5UVm7L4R6W8RzzELoGrij71b875j7tUv5GkPFJUEHLe" +
                "6+Vp5jBf0P7sQYWr0w6IK4F3JSKDVuVH8VuofOfbr+4z4UzJmi/FlzyQAke/" +
                "a904M2lATtYoufPHYJE8v7fgxMELlewqeVgvsHm+q3qY6Xv3R0e5b6BJAIA9" +
                "0u/dJyEFAf7q/89A/n0eOLlOmw6+u1ABTof3v1bOSZQ/XUZxQoYF/Iz2p6vo" +
                "T6jmf/ED/04NPKEGkZyC/qcbsJ4w0Pq1wX9GgZP3/vOLvHjCJew3LqeMBn83" +
                "/7nhMifM6an+vyduDDtL/qNLFAD5Y1fac993/wK1/pChWw4AAA=="

        @JvmField
        val LINT_JAR_SERVICE_REGISTRY_BASE64_GZIP = "" +
                "H4sIAAAAAAAAAJ2XCTSU+//Hx5IsWWexkz1EsiW7BoOxb5Ety4x1zJixVbIU" +
                "yrUUsg+3bopkq3RlHwxFmPCzRYYs185IKBq/3N/9/y8d3fP//77nPOc5z3k+" +
                "r/fz+X4+z/mez9sSTkcPAjAyMgLYqqXggAOLCUAPMDOw1ZMzNjc8Q+0EAOgA" +
                "lvDjjPuvaP8KsfwpDPp+/S9spmdubGhgYytvZvjJrOutKVxOvpcVLifT00V6" +
                "Ya3wL6WJGay8idlpY7PekBJ6Joc5XiJ/Lt885NFsoSSkSMYn/FGmGlgCOQuW" +
                "BS2C52f817EULM2fucQ/sZEw+f4lk79yYQYAvudH90MuXAdzwSGwob6eCNyZ" +
                "v3P/MV7vyHhPNErePdALi/b1kg9GowNw8gG+gcHyngG+iO83d4yvvDEOF4Kw" +
                "Rnj74oKxVzK6tZhbdEFEinYRT2VlKuqYxrOzl2dYM5c4hJE6w1uDd0fr0oDD" +
                "JDT1eBKuMyMbm2T6WGobFD8JaPfghxjb3tLYYgJviuq/5Mx4UWyHTutHzJUi" +
                "mcydpGtyd/Aur2fLH41hT4+3C19dQS7GHF8uXhPRPfF7p76D4Qb6ghOpTP+y" +
                "DyK1g3/v2x7tfp30Rh5dT/i+q5FDdRINh0AP7nu/tX/t8MzPozgOROFCMBg0" +
                "NvgfooFHRIee+weA52hgv9YHWvYjdeYfKD0MBopGYdyD9RHBCM9gNPZ7z9xx" +
                "uPyLX8L4B8BUBnhuqqJIjzjPvcoT7e2heAfJmL5hEdvUGduy0xJ8I4YBm+Eb" +
                "AWaKuZxKYhorDHIu8yZwJyO4U21vA9Eyvf9ClE+PL2k+qnmvd7kxtfRNwgks" +
                "TqC7fmelbjGM3PCJtLf7pQKQ4qkUb9UFo/CYiAplrcXGSkloSkz6MrFwQTzf" +
                "PQPFxTynh0DZMz0d9CQYgNwerxgTV+0khrkEmFSYSEYYiXCuCM6QyMsqv+iV" +
                "Uuiytkm1sRZG24WxFsbbwaBfm19Yad4i/bKNv/3Hw23NRo5WQCNPa9Yagslt" +
                "ksStH2FHHKKPVcpVzYMo5Krg/S5Uvy33zAQ2Tjm4tiiidKyQ2koRr83mVJRS" +
                "KfXqeSDM4khwkEiODHK9Jk6p4wZA4pqe2wAwM7BCXJmssgz++D4PG0AaQkWZ" +
                "Vy+YEiLLsk9X9XRPFxmt6iWL58tOkyB9ue0IuS6EMk01Wb9nmk8jLGw4x4S7" +
                "Py7HVN5RPOF63MaXkSp75+yi5N3dlmB1R7zcO8Ppe755VF8arVYxHO0F6SJT" +
                "w+W3uapvB57Cu+QW4JCpRrX4wO758o4hM483FdlC7P32t+uvls2+ivT4PGmI" +
                "T3t5qfPJYHBUCa8Cc3RVS0RZu4Mj4dlkarmoQGV/r7a6iahc/8RXm1rEzIaM" +
                "GAuIC0SsXcmzF/W/qh0EU8G1DX6rhWeORwFbn+Sl43KFFWW6Xr0xYAlHvZJd" +
                "tM5cuXLP7leXdakXKXVpmEz5d5t8TiZzriqTdUkq2VjQmOB5ZLZbUKdLpVcA" +
                "iW04vEV/ndx/8Y1Br0/2yYntle7ZFLTjqft7NxtOTtvS+dOPDcXBYsFit+ct" +
                "sxqtONX9VXHz983OS245278Ivjm+6dcY6Izv9adhnlcVMc1XLJVM8cy1CWNV" +
                "RWGjNz/QE84WhPCyT7WKNkpPOYVb6a4mG3GcxBXyNs4Pewc/dxe2Kx0V76IX" +
                "qW31pJ4fcgyS3iUqel+dTfPNodZGNy878ebd+lCWG2sw2BO2fX17jg0zohQy" +
                "++XTY5fQwceOAC9OciZFsF78Y53iOg+3wtiDjY1Lcr2KBU2ladq85NYtg/rT" +
                "U2BnCIWOV4f4VDRSfOr1dkISznmYxjWX9TyqZ+tYD/rhOQ/MnSEPsggIpoFd" +
                "4kmlsiP/cPfHgNSJMEK0fdCCvWPZNVPBiMsEG7NaVI6rfT4iZE2tjN/HD35F" +
                "m/Bb0tCaDe9rBckp7+2aJLeJrHtbkEhk7uUm3KBYdPGncmhTmUuI/zW/3C6K" +
                "LXQru6jcLvutrYvKQsGq7YBBZyJn5ZmS5PdWDfHP4EmjTuPP7QjXPoGRwOSn" +
                "67LinouFfjnw8pVy4RkveTz1mnmGyrGrC7U26PRO/5S3Piy03N4agXbVtq+R" +
                "44W1S69k6gov+kM0SuRDdQlrbzzqots2++43nH1p5KI7w53E51xQMqPG3aAr" +
                "P7T6pBHo48LiMagQbEpcVIyMc0vcTk4ifwwL9g8Z8K/JsUPuqJ/TJRgN5cOI" +
                "q4hdmqjpl3vxt2UVVdceR8NkGt3yjO5m2iHPq8ymqEikBOH4kMoIYf92bT1p" +
                "ZFa7TORAGgVC1VdYbd+Qg9Ljx1h3dDF/+MUHHq9+ENGq2PorTfJWc2p5nfQH" +
                "ck2Eu2TOcmjnyEJzw9CoT1/I55A+vl6tcav5Wf5I0Wue7mahc103XT4DuEwl" +
                "yp0KJ3+fZMPOjwu/b14H3BKKIXRxQRZVSrz6ZRUSy/omMtAbGSjFPCAg63zj" +
                "6KxxQ7qJR6fKxZphSr4334JV7NiEZvfFbmC6RmuRmM7nJsNk1f4e7zy2lGA+" +
                "LbG39Ssd4+mC5kv4muULm3dDtVlUFyqU6210LOSvND7lp372fSSbjUqYnbV+" +
                "XfQcuOYchnPwZCPnZ+ZpLXHE18jLahp8820LepK6bDzDEhdckv0wyJngEPbJ" +
                "4dPGDsDjml5f8gwDygZvAcHmjI0EqqDy8UubLQpzHoWdFoj0jZCz+R2Puats" +
                "qQLXtQR8ni5XuVT4JWVtWWrU0941D52Ycl82uX734o2bk4raAMZUtrlvOtJ1" +
                "nXBkWPMy24vRr45ZKs8/N9fAzmR5C2QhlhAwwkPBipiVmUnXr5FCxx6wi36M" +
                "4PqNfQtqdBnvw/5uR46x92v4qLrpIhC1yFgmHOCUWBVZjJLr/QpFLZ4y5tg+" +
                "bd4klYC9UUCr1Qobj8dIXM65T+bRihErjASlb0iey8Yw33lnQ/DVfA9SGARx" +
                "vCfIoat6YLSXflsOKA7ZFDPi4ta5VlAcgS4+ty5/1UTLxEg27l6lPbMsZxnI" +
                "Sr9MXCbNPu1ecSj4GX+bzrBkZLE01ZSQLvoe1McKjAt7s3aqt6INinYn6asJ" +
                "2U+bLTUk1X2cfPrqsyJbSqCHm2Z9gmvjlzt7jWEzzN4fdDg5JfUlQHc4tmRv" +
                "S3fGbqsjMzCJmGjg43tGiZi7QPNTHokYfqkNWevEd6+ZdjJ80k/KVFvcDqKs" +
                "8tivMDYlhj+RMndNN8Fuv4tVro1pfUh5J7mSBG+RXPnIu8B0odI0mqbNWjxr" +
                "K701CBse7SzUrDwjB72SJzIxzXjiSufA+d8Kisi0sw/0YJzgrpY2gcGEdU2h" +
                "VpZTkxKaKVkUkGtbv3KJ3ASa2AHdFY9D0zWIe+IBBjEBDHlN+W2wm/TOYvnC" +
                "LJY4VmFYZuk0WslL73ZAxK0odSu+lCkD6gcoUwQj+WRiu2+FLj6ISD1mFTt4" +
                "J2qKQ2u+Iou6LCgS1wa5lD4MLBa0WrFfsbKdGWfd2DJUxZDdFCzVo5tZx/ZU" +
                "48utFWCqQdNmAo1cJ1fUvb6+aRkBiOIKf53Cc5NPNQolUNn35ykdNqKt2vfh" +
                "RQ14cAb+cShR/r8MJYeGxv9MJhnWZhajuhzXWYxNb4zeq4rI6GN/cotePLMl" +
                "Bsphl/E7V9qASSyWrCYb1oscej5k9O1iVZa9wDbN7iliKfA+lwFYuyu8gNyV" +
                "d3088e0LHMAp/IMu2GIxPP4LA6SS57I0QRGKusg7s8nzeDRfLN6ExCYBVbI2" +
                "jJQ2pMsR/CVGshA84jAnlirtZkH4ZaccG99tlO2OHLj7h0Hu+y5KumB5RtAu" +
                "NHdz6RQ4UIfEcuUkMKNuCZb1oODNWcPlslApdYjUDW5zXyHONuBXmbMtYN6O" +
                "+ibocLKHxsehkp5L+XeO3errGB5JSrSlWN+vg/XzXXfyerZbujsouzfu1wqP" +
                "LRZhXNMkMrvSO7BSuCp4VIVCdRkiSZHP7NP6ukOrBfP3TCLw+OQHteTVqB1i" +
                "JRXAzzOL1lpyLEu4XyqyJzzTKX5J3b/+5Qn0qGf1SwWmPXU1SY+tHuEWfts5" +
                "wy/ezF11hkHxJajOJGzI8rlh0jou3+Pm8zvgLmv6lrMJJXcV5WJeE9foxs0Y" +
                "6JsUPh/vLzon5icxtvtUK7vqHLwH2DLbWT0I3u/+8LDQVTwNALBFu999GloQ" +
                "4LAH+h93tG+gDq9DdupH9KCZAR3CtH5ipvYVmAE/Nz1/r44jLc3BDPb5gzZB" +
                "7xD/5Uj+/2mJDqZ7lPf4e1FoDjqRn1Mchyhu2qOcyc9p4CHa4Aj6T6fycwGe" +
                "QwKhRwv85Vz+rvS+ysED5MwhldJ/UPmJk/lR/OD/qXxIXI/lvzuRLOHHGPb5" +
                "/ULMflfVYd1/+jeeVNdOOxAAAA=="

        private val LOMBOK_LINT_JAR_BASE64_GZIP = "" +
                "H4sIAAAAAAAAAI1WezgTeh+fy2xTaJp6c0m5TDQjIYWGMYYxM5dJHMZEzV0R" +
                "vee4tCG3yK0sl6hXCLnrOl1cNsQ0iYiG5DK3UHR6nafnfU7pPe97Pr/n+8fv" +
                "eT6f7/f7x/eGtxISFgUAwGDAuJklFrAJQcA3QDYNZ0Y0VsfaYDSEAPgfiI0x" +
                "3V7oTYL5psG+J+KMbbAYM3siEodZwrFZ1lbqyB4xK3W1LnZ3DUGz7/Db8cV2" +
                "9sEeMUtcNwKperuWYNHdbduN7WT3W2E7O/uXKpYRSCTuInYpeCFYYEvQrdn5" +
                "BAT4nPH2Dfgpu++J4t8TvbypP7OtdjTESgABgPPbAYC9P7FxEabeod7k0IBg" +
                "JPmMR0hIieOJczsdYL/T4LlK1qrFvYRi6SoTS1/QPOYaIcmX+Uu65y0Y04ph" +
                "bcOJzj0aNKEkeWH+3PyEbVqzo8eUC/LCx9W198FN3Up1yVQSxICdx5phNTyq" +
                "WT2+NIl6tCHyVu2hrSdsW9CQxRial74GjpCywpoGHU45EbyhF+0B42UPBkUp" +
                "tHrKluh1BCFUAkH58CpB9LPbifcg5aNukqpRWU8vGOAXmhI1YKbbaEHnsExi" +
                "z9gd//MO+YMOJwPnhJvJkSaVWkwE88Kb8sHz+8HSiLJ9ugGR9c54zbRcV/sd" +
                "7Utq9prEWkNH/0apicqrUvSzBDP/3K6rU4PBbwaS4nH6+MBm53f3JzSvZUhk" +
                "T5dwPVuljuo5kBWcKvXrlcMsrc8s+g2E7pkq0cp4Rqdw+mRazi4XyYHTbf9x" +
                "tvOw2ROV9clVCLuhP2XqZSh6rd6JhUzr7bkp96qI6sqk3FQR9c8oMVgVjkTc" +
                "NaPfk/rd4ohQ4aRgNbwTuvFgkDoNrMSagq9Fktqd2LNrBvUtgnPapDBQnOO8" +
                "wcSk3GGwV3m/RKzXUd0bHqAXchyJHFQPNVmBWKFLADp9Fk+pvTb7Gyxpr6Sq" +
                "3M3de7+sbSM77ljVf0GMMrObA6jTOwayLxHUMqaM0TncG+kkCc7BWlR173zc" +
                "rFaMkZFvtrZqhPBSyPVWM3DiK8PB8f6hL8XZTU7+euIk1LnG7RCLVK3mCJPX" +
                "yPhipszDZ/6vtTj0syHnMZQCWn8QLZESGyI1Lf2QVlpUxBRcUEgIv8jhVkVL" +
                "D8AjxK5V6SSN1tLo4cVvnxadxDWEK85qcaycrL/SREBIhlupQ0G5jEZdr9QI" +
                "RWaqYLT55ojlUnLtKy/fE1z9y0YjcnMvc/IpqdNEzH7gSRM1AzHZMKmRU/PG" +
                "M26zpYWNYfQmmOspO+6X/YbGwIsvjewJaxWJrdIuazbMEre8mVf4wAr5BUh7" +
                "nUfuXYVSUmgFxf0YWSpviGoTMafHK2OkxArowOmQufbrZqS8AU591kEJrGak" +
                "0TRnHXHm9rTNIbUDPitcqjM+o5KYRGe0RneIprB0+f3SWkKLeYm69+1ERWmd" +
                "Xf7tMC58m+OJsBj9dNUZPi9D4n1gm001hwrjfY6/falkzP9J4qfyrgstS+iA" +
                "fpN1ytBkA6oBJNzAJadM4ph+QwHQ3Z/GMzNLNvwi2jVh7OW4X8zXgSqvlAOq" +
                "4EtU4+G+yKbM4MKoMidb/as837sqEZdYGfLNjBOsihEELJubvLEo99ljz7A9" +
                "xMrtiU4OgrYt8jT8PqYG1IZ1jFx0O7X6KGf8sKcF0z33i3kfx8adjn+f2j68" +
                "WFPWs+IsMVv8nPSZJQvU8Tn/nFdn8ugW9CrHG8MPcpnSVtPGaByPXbwCN58X" +
                "eUAendg3XNzA3WV0lR09MTm/C4sdZ8mNO39oC9m+sYya7TF8XZ2yYnpgSXeW" +
                "0XoxJXPSSx/zbqhvWVch5HJ1m3z8ceayM++c6WQRuYf6VAHlGYU4lkZogP4a" +
                "09e3S5JDMMg91iyGG9sLNswXV2dEW2bRT/nEaboqjdTwSEWkAr5iHCpWddHy" +
                "uH4jb0byJro8JGe/S0pcDavRJMvhPvrXj3xoGJ/jL9sxkuOwIHCnxy/8tysU" +
                "tOdEy+qZA+lfUkdJSmiQj31sKbm0UF0WtXewJcH9cnO+9HDL6e7AxaOYf2LG" +
                "fP/1kDVyqEVTfjGM4ZlWLbZjwswaIxnnA+X3hic6/66Vtbb7x1GqR9axSxMA" +
                "AB5sTl/F/zJKsSEhYd4Ebx/fkNDgiG/zNJOAs5U5BJNFp1N7L8bBOo4UEiQg" +
                "Fs5XyEXLAKKJi5HC4zcDzpcUWzH3hi03DnYA+4RQBajbHwapwmBhycj5Gp/J" +
                "TpnUjUk+PxIwUuVmDM3vyHnmCsw/xPdgPlFAl95ICj5Ja6dM5xVWnQalF0of" +
                "UEzttXN/fquoElIK0oV9dn2qpp/KjLmHh6AV48uTa4lZEry2xSdlsUrY8YQk" +
                "4thYmS76aDqyNf5kQH1M4Zpq+IcrEwJ2Wia2ZphGbfhLvgaYsa9WEcneV+/3" +
                "mFF3bNRlj/m7vO2M5KZGooH8i7l0lxIfjt4VFJESvJLLqLAeNIdcpyvLs5wX" +
                "qj6QxcRoL49otDx2X4D6KZcaNIbPOIffaTxSz8YbvP/YtbJHdyRIn9bUeups" +
                "BdcUz8WX2ydoabeRMpSfz9d9QpmqWz9NXUcr1LsLp2oceHGv7SGkCrHTBfXC" +
                "l652w2WoEbie+RXJcSkUjSqG568U6KhAx+yVd+bAMfAqw13z/ppB9PZpUAJB" +
                "vP96beS7Fb+uUqhuBv/D183dKCAIE/rrpfsNOwCzxoDv9/5W2dYD4T8ojlb9" +
                "i3Ph7we+/n1l/S+Z+A+y1p8Kcqt26yXwZ9If/+9dsNXX1lb405eRyN9qDLwV" +
                "UOQPgcjmS9h0BAX/8fs37IxfaLAJAAA="

        private val PSI_LINT_JAR_BASE64_GZIP = "" +
                "H4sIAAAAAAAAAJVVd1DT6RYNVRCwANJVwCggSehkBUKRpSUUaZHqiiGGCCaU" +
                "wEpRiAgCg4AgoS6wUkSy4BIF8RkjBCPFaDAQ3VAkGlpWUIqCBWRx1jdKnjLz" +
                "7m++P77fnHO+O3fuvecQQkx8MwAgJQX4E+UBB6yFKODfkF47bg4+dmAXd0cD" +
                "McChdcD2GjTqM9pz7Sh+C3Szc3dxdPD2gbg5LrgxH7giwJDHcgjw/kdM1nUv" +
                "wwET3vh8D1P/sRzcjQWC6DXc8HJ+xOIgGp2Z4G4XDoLF8tjkdcB2RwKtsILB" +
                "qL1qmEYNogZ98zas9Venb5MUXzso/Ml1+QljZL9g0KdCTkZGoDfEKghhMXg8" +
                "RogiXCeV71MisDjC//BScy+rG63l7Lf2y3gDnlsIFmeHImDjsIT4n9EENIqA" +
                "j4agIkJiYgqR4d7qvipnWm1xckX3kYGNcZ6D6QBua5VS6oCpuERnrT+88WY0" +
                "5cC5zojF0DiPbWdSbeh3j0bVzUxl27SP8jFdSxEHO+i8fSZj5TDqZPyzBwu0" +
                "sruf3q94AEzeGaXJzkv76Q13jADTIw3SsEzgbPMRGxdev/grHcOyexhdoNF5" +
                "Fr84l28n/0tVMV1zV90h+kGN8Z+U5GRB3EU+hZiZtadMaeFvmmIswkUgSdK3" +
                "QnNuvcjJpzEC6AGbz5obqGQ8lPaojxc7zZ1zE1vZ6z8pma5P55Nnjl/4qP7H" +
                "BNulTnvnBM74l6nJVyF3B7cTiUnd3Uv+18ejyVroVFJedJr0tSndZp2BjoNF" +
                "Fc7XzNv2twY0RZo+2nzYkkLrLS3/05g71wsZQiEDpZ82ZNhePM1Wksu92upR" +
                "ftNdlRlbETxhBgp8Tw9XnBCwGbDtUhwR6CzC3CuAJH0VCGMs+WpRbZWGYxlN" +
                "nM0Wq+Db1B5Yy9UOveJ+x5fEqTxS8UvSUqD1VlGtOwsgGYu3UVc6w3k4VhN7" +
                "5k6OvSTOcb4sy+ktQW7sspPtCWxcf/wKNUwWOKFxk0y+BB6idSqnKTFbrotf" +
                "fDqO4J+k2u8fJhnn1JqS5mLViqL25sDV9P329kp4QreGDkVET5QEmk3gS+gy" +
                "ipjYBTu1cD9o5HtQklgRcgvzPI03VHea5xJNOrdYH9ZazFKzDp8pZkM5HuxQ" +
                "My6ybvD5vhifslt1fcH50MrIfq1Hce0Pg5r2jSRAoFic53TQzhvvntT0FBZA" +
                "+acuOe6I01DHWkkWvjlS4z4QXI3oVNzLbQ81eKjYVWBK6/D+jYbnv4NceD1n" +
                "r8xfzchd8J5iJLmSM7NOesOt3G+ef2blbCBoGaNo9oFPj7cFI9ix0ude1Cx0" +
                "pAQUeunCRhiS9yk6avGNrYj0NJN0IgcV0wesiqruOqxzV59r0jI0s/VY9tlk" +
                "8SuJ9EmwQOEh67GjK2a+4qU9HlK+Kzmh/Od820y9Z3mfRMbrbyAbSQh1MnrI" +
                "unTKE6+sTeEm5X4EHkk4XEjmtJzwqahOvGeyq+fZRfCKfCAEp1QJhvj2sqbZ" +
                "gxZ31EYtfq+jPMUNBsn8vegyktYALg+5EqOWCfvDoIc+d2C8q32U2y+RRWB7" +
                "S5MSHoSa1n7CGDtMZ+dQjeB3McZVl/pHzWDdAqiHzQy2t3/JN6G0jlMVq7kk" +
                "fz9mMEjTXtNBP08HJfOaDRkxoie2uZOJS3Mi/mEplsEpfhN0/TcrH9S0lCrc" +
                "K9t2vnxG2z5YTDDP2nobF6GwCGjco3B+qrK0OIFIdJ2WeIFPIOzuLJYzwlgs" +
                "Brve+SiF0mJJ4V4gw2Bnh7ovGwxf2OP0GGg4Z/HGnz5Q+dhVu4BZsyUqChiV" +
                "8Wa3SlKpqsW9WUQ2lV472rdqJfv+p3p1cWtVC/O0awefEtJN3hmTlpXXL6AQ" +
                "DpBXIgIAPF/bXuCNFlC8S0xMLNoLjcHGEKLjv+wer2CEup0iDGX9pKb5RMNK" +
                "a8A2Vx+NczQHEKNVMyLQsHSp5/hfR0qY3a8iwj4VfjgmsE+2TfYRjLjVO2yf" +
                "th5OnHx1YInPXP6wwtdJgfqRq886mYFCbuV0nZLnYVMwohwYZWxJAt4Y51GV" +
                "xJMMOGoNJNtgOmnuOb6jkXWq/3EuqYDr2ngUoFcLvYhxKrd+HxgdmGWGqVoq" +
                "XzcJvbNsau3a77pbUOxVmW0p2uL3UbuLepy8s6LhYn6Jzh779LEUCAP5HvT8" +
                "Ax4vspiF7N0i7yQm+M1EUNNvm7gzvsg51pES1sedPPG26UG+c3JN0nCtOn96" +
                "rEW9XltqlkpUPqV6tJZ42G/HX22TcNH5nExt+9KR0fgm1aFfnyBDRx0qHMe6" +
                "Xl+RkfbZcnukYKto4Grz7fb5lshNj1Qk866DPgTBSeoJmBiYxJNAhcwLlOZl" +
                "tg366OSTFp5Z7/KZebyk9uHsGVAlMLGDRomXiPJcbmpuVOrZMcA13wew4klp" +
                "sJ5rVUfPv7qkEXRswhI+CZ0KX+1L1TtNawdF4pozSpPXTEZEVFHsxzb+b2wD" +
                "zNgBvvVqYZqwqf83aoh6P7D4bxW+Z81fH6790mwbMWTXMXqE2nMjpsI65tvv" +
                "N/ZGVVJZJ6Ar8uPJEFYRtvSvRTu6gcqPDV5YX3hiv+o/l/g/5/cQQkLyM1Vq" +
                "7dNZk2Ns+nz7B9ep+6YCCgAA"

        @ClassRule @JvmField var temp = TemporaryFolder()
        init {
            temp.create()
        }
        private val lintJar: File = base64gzip("lint1.jar", LINT_JAR_BASE64_GZIP).createFile(temp.root)
        private val oldLintJar: File = base64gzip("lint2.jar", LOMBOK_LINT_JAR_BASE64_GZIP).createFile(temp.root)
        private val psiLintJar: File = base64gzip("lint3.jar", PSI_LINT_JAR_BASE64_GZIP).createFile(temp.root)
        private val lintJarWithServiceRegistry: File = base64gzip("lint1.jar",
                LINT_JAR_SERVICE_REGISTRY_BASE64_GZIP).createFile(temp.root)
    }
}
