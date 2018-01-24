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

import com.android.tools.lint.checks.LocaleFolderDetector.suggestBcp47Correction
import com.android.tools.lint.detector.api.Detector
import org.intellij.lang.annotations.Language

class LocaleFolderDetectorTest : AbstractCheckTest() {

    override fun getDetector(): Detector = LocaleFolderDetector()

    fun testDeprecated() {
        lint().files(
                xml("res/values/strings.xml", stringsXml),
                xml("res/values-no/strings.xml", stringsXml),
                xml("res/values-he/strings.xml", stringsXml),
                xml("res/values-id/strings.xml", stringsXml),
                xml("res/values-yi/strings.xml", stringsXml))
                .run()
                .expect("""
res/values-he: Warning: The locale folder "he" should be called "iw" instead; see the java.util.Locale documentation [LocaleFolder]
res/values-id: Warning: The locale folder "id" should be called "in" instead; see the java.util.Locale documentation [LocaleFolder]
res/values-yi: Warning: The locale folder "yi" should be called "ji" instead; see the java.util.Locale documentation [LocaleFolder]
0 errors, 3 warnings
""")
    }

    fun testSuspiciousRegion() {
        lint().files(
                xml("res/values/strings.xml", stringsXml),
                xml("res/values-no/strings.xml", stringsXml),
                xml("res/values-nb-rNO/strings.xml", stringsXml),
                xml("res/values-nb-rSJ/strings.xml", stringsXml),
                xml("res/values-nb-rSE/strings.xml", stringsXml),
                xml("res/values-sv-rSV/strings.xml", stringsXml),
                xml("res/values-en-rXA/strings.xml", stringsXml),
                xml("res/values-ff-rNO/strings.xml", stringsXml))
                .run()
                .expect("""
res/values-ff-rNO: Warning: Suspicious language and region combination ff (Fulah) with NO (Norway): language ff is usually paired with: SN (Senegal), CM (Cameroon), GN (Guinea), MR (Mauritania) [WrongRegion]
res/values-nb-rSE: Warning: Suspicious language and region combination nb (Norwegian Bokmål) with SE (Sweden): language nb is usually paired with: NO (Norway), SJ (Svalbard & Jan Mayen) [WrongRegion]
res/values-sv-rSV: Warning: Suspicious language and region combination sv (Swedish) with SV (El Salvador): language sv is usually paired with: SE (Sweden), AX (Åland Islands), FI (Finland) [WrongRegion]
0 errors, 3 warnings
""")
    }

    fun testAlpha3() {
        val expected = """
res/values-b+nor+NOR: Warning: For compatibility, should use 2-letter language codes when available; use no instead of nor [UseAlpha2]
res/values-b+nor+NOR: Warning: For compatibility, should use 2-letter region codes when available; use NO instead of nor [UseAlpha2]
0 errors, 2 warnings
"""
        lint().files(
                xml("res/values/strings.xml", stringsXml),
                xml("res/values-no/strings.xml", stringsXml),
                xml("res/values-b+kok+IN//strings.xml", stringsXml), // OK
                xml("res/values-b+nor+NOR/strings.xml", stringsXml)) // Not OK
                .run()
                .expect(expected)

    }

    fun testInvalidFolder() {
        lint().files(
                xml("res/values/strings.xml", stringsXml),
                xml("res/values-ldtrl-mnc123/strings.xml", stringsXml),
                xml("res/values-kok-rIN//strings.xml", stringsXml),
                xml("res/values-no-rNOR/strings.xml", stringsXml))
                .run()
                .expect("""
res/values-ldtrl-mnc123: Error: Invalid resource folder: make sure qualifiers appear in the correct order, are spelled correctly, etc. [InvalidResourceFolder]
res/values-no-rNOR: Error: Invalid resource folder; did you mean b+no+NO ? [InvalidResourceFolder]
2 errors, 0 warnings
""")
    }

    fun testConflictingScripts() {
        lint().files(
                xml("res/values/strings.xml", stringsXml),
                xml("res/values-b+en+Scr1/strings.xml", stringsXml),
                xml("res/values-b+en+Scr2/strings.xml", stringsXml),
                xml("res/values-b+en+Scr3-v21/strings.xml", stringsXml),
                xml("res/values-b+fr+Scr1-v21/strings.xml", stringsXml),
                xml("res/values-b+fr+Scr2-v21/strings.xml", stringsXml),
                xml("res/values-b+no+Scr1/strings.xml", stringsXml),
                xml("res/values-b+no+Scr2-v21/strings.xml", stringsXml),
                xml("res/values-b+se+Scr1/strings.xml", stringsXml),
                xml("res/values-b+de+Scr1+DE/strings.xml", stringsXml),
                xml("res/values-b+de+Scr2+AT/strings.xml", stringsXml))
                .run()
                .expect("""
res/values-b+en+Scr1: Error: Multiple locale folders for language en map to a single folder in versions < API 21: values-b+en+Scr2, values-b+en+Scr1 [InvalidResourceFolder]
    res/values-b+en+Scr2: <No location-specific message
1 errors, 0 warnings
""")
    }

    fun testUsing3LetterCodesWithoutGetLocales() {
        // b/34520084

        lint().files(
                manifest().minSdk(18),
                xml("res/values-no/strings.xml", stringsXml),
                xml("res/values-fil/strings.xml", stringsXml),
                xml("res/values-b+kok+IN//strings.xml", stringsXml))
                .run()

                // No warnings when there's no call to getLocales anywhere
                .expectClean()
    }

    fun testCrashApi19FromSource() {
        // b/34520084

        lint().files(
                manifest().minSdk(18),
                // Explicit call to getLocales
                java(""
                        + "package test.pkg.myapplication;\n"
                        + "\n"
                        + "import android.content.res.AssetManager;\n"
                        + "import android.content.res.Resources;\n"
                        + "\n"
                        + "public class MyLibrary {\n"
                        + "    public static void doSomething(Resources resources) {\n"
                        + "        AssetManager assets = resources.getAssets();\n"
                        + "        String[] locales = assets.getLocales();\n"
                        + "    }\n"
                        + "}\n"),
                xml("res/values-no/strings.xml", stringsXml),
                xml("res/values-fil/strings.xml", stringsXml),
                xml("res/values-b+kok+IN//strings.xml", stringsXml))
                .run()
                .expect("""
src/test/pkg/myapplication/MyLibrary.java:9: Error: The app will crash on platforms older than v21 (minSdkVersion is 18) because AssetManager#getLocales is called and it contains one or more v21-style (3-letter or BCP47 locale) folders: values-b+kok+IN, values-fil [GetLocales]
        String[] locales = assets.getLocales();
                           ~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
""")
    }

    fun testCrashApi19FromLibrary() {
        // b/34520084

        lint().files(
                manifest().minSdk(18),
                jar("libs/build-compat.jar",
                base64gzip("android/support/v4/os/BuildCompat.class", "" +
                        "H4sIAAAAAAAAAIVSy07CQBQ9g0ihouID3woaF+DCWbjUmBATV1UTMW5cDWVSR8uUTAcTPsu" +
                        "NJi78AD/KeKegbox0cZ/n3HvupB+fb+8AjrDnYxpVHytY9bGGdQ8bHjYZCidKK3vKMNVo3j" +
                        "Lkz5KuZJgPlJaXg15HmhvRiamyGCShiG+FUS4fF/P2XqUMe4GVqeX9x4j3hqLfj1UorEo0v" +
                        "xgGqmOEGR4zzHSTdtKTxNARURqB0F2TqC4PE22lttzIlF/LNBmYUKbHTkzJfKcMtQl4Bi92" +
                        "Ch126S54EE+Cx0JHvG0NbaS+386g58opn/uRduigZRTgedgqYxs7Hmpl1LHLUJ90F0Pld9N" +
                        "V50GGlmH7X6V0ViRtK02lpXi/0fzzsKx/IbSIpCHt9UkYOo+mBt8vUG00/3gD7CJPf4H7cm" +
                        "DuZLJFyjh5Rn764BXsOWuXyBZGRfhky+N4hiJQdxZzY3KLfM7VDl6Qe8FU8DvAzxoewYrZk" +
                        "JUREPOokC8ScQGL2e6ljLP8BSDzZ1KvAgAA")),
                xml("res/values-no/strings.xml", stringsXml),
                xml("res/values-fil/strings.xml", stringsXml),
                xml("res/values-b+kok+IN//strings.xml", stringsXml))
                .run()
                .expect("""
res/values-b+kok+IN: Error: The app will crash on platforms older than v21 (minSdkVersion is 18) because AssetManager#getLocales() is called (from the library jar file libs/build-compat.jar) and this folder resource name only works on v21 or later with that call present in the app [GetLocales]
res/values-fil: Error: The app will crash on platforms older than v21 (minSdkVersion is 18) because AssetManager#getLocales() is called (from the library jar file libs/build-compat.jar) and this folder resource name only works on v21 or later with that call present in the app [GetLocales]
2 errors, 0 warnings
""")
    }

    // TODO: Test v21? Test suppress?

    fun testBcpReplacement() {
        assertEquals("b+no+NO", suggestBcp47Correction("values-nor-rNO"))
        assertEquals("b+no+NO", suggestBcp47Correction("values-nor-rNOR"))
        assertEquals("b+es+419", suggestBcp47Correction("values-es-419"))
        assertNull(suggestBcp47Correction("values-car"))
        assertNull(suggestBcp47Correction("values-b+foo+bar"))
    }

    fun testMagicJar() {
        // Same test case as #testCrashApi19FromLibrary, but with the classes packed
        // in a jar file that has an unknown/foreign header; make sure lint reads files
        // from the index at the back of the file instead

        lint().files(
                manifest().minSdk(18),
                base64gzip("libs/build-compat.jar", ""
                        + "H4sIAAAAAAAAAEspzc2t1M1ITUxJLeIK8GZmEWHg4OBgsFjE682ABEQYWBh8"
                        + "XUMcdT393PR9Hf083VyDQ/R83f6dYmD47HvmtI+3rt5FXm9drXNnzm8OMrhi"
                        + "/ODpo6dMDAHe7Byb6pneSQONkARi3BaoA3FiXkpRfmaKfnFpQUF+UYl+mYl+"
                        + "frG+U2lmTopzfm5BYoleck5icXFr0Gm/Qw4its0eCxc9Yr7PJSX+4FDkjisz"
                        + "BITDQ4UN82J4U4PcT0/xYRe2O92rpveHwf5UxfIFeT0lhfOf36l+t0R+fv57"
                        + "hr4Nz5OlQuW1I75ubCvll5Yzk+RS99KuPmVwNeOe0ZPzk9alzOCfMm16c5z7"
                        + "LOmLnSs3Sags7MhvDdSTF//8LW4pT3Xj1JVP/x7v+N626H5/qMYaL5P9Emyr"
                        + "klsk5YzPlEwuuzT5yAXBQKnGC7GTV+jxC+cu3XbnxJT40yaSMwKWHffzOVmz"
                        + "nGcrZwWbfhmXbNlLvQoB/6bjTu57cnlzl+y7f27B81Oa839PLHuxIFXkQeUN"
                        + "rWRpa7ms1CsbTwfMLxF3flp+OfS5Y9u0xN/xS0vCVHK1fZcurdh/8c/nDWvq"
                        + "Fbd0LFF8+9VTwqp/Gdt9/YBck/8VzG+U/OsbfxclvEvZ5HrKotKt7tcD0TeW"
                        + "2dNPiLm5/kg5/biiTSmgXKw4uai75txVvrgJ1QdDPlgfOC8ld7Dr5oSpggKf"
                        + "V0zQFypk7L75bmnP5j+soBhV+JweVMvIwLCeCRSjjEwiDIg4RY5tUKJBBbiS"
                        + "ELopyDaoo5iQSHw6CfBmZQNpYQLC2UDamAnEAwBHypjM8QIAAA=="),
                xml("res/values-no/strings.xml", stringsXml),
                xml("res/values-fil/strings.xml", stringsXml),
                xml("res/values-b+kok+IN//strings.xml", stringsXml))
                .run()
                .expect("""
res/values-b+kok+IN: Error: The app will crash on platforms older than v21 (minSdkVersion is 18) because AssetManager#getLocales() is called (from the library jar file libs/build-compat.jar) and this folder resource name only works on v21 or later with that call present in the app [GetLocales]
res/values-fil: Error: The app will crash on platforms older than v21 (minSdkVersion is 18) because AssetManager#getLocales() is called (from the library jar file libs/build-compat.jar) and this folder resource name only works on v21 or later with that call present in the app [GetLocales]
2 errors, 0 warnings
""")
    }

    companion object {
        // Sample code
        @Language("XML")
        private val stringsXml =
"""<?xml version="1.0" encoding="utf-8"?>
<!-- Copyright (C) 2007 The Android Open Source Project

     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
-->

<resources>
    <!-- Home -->
    <string name="home_title">Home Sample</string>
    <string name="show_all_apps">All</string>

    <!-- Home Menus -->
    <string name="menu_wallpaper">Wallpaper</string>
    <string name="menu_search">Search</string>
    <string name="menu_settings">Settings</string>
    <string name="dummy" translatable="false">Ignore Me</string>

    <!-- Wallpaper -->
    <string name="wallpaper_instructions">Tap picture to set portrait wallpaper</string>
</resources>

"""
    }
}