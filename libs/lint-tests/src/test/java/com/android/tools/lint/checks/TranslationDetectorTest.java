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

package com.android.tools.lint.checks;

import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings("javadoc")
public class TranslationDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new TranslationDetector();
    }

    @Override
    protected boolean includeParentPath() {
        return true;
    }

    public void testTranslation() throws Exception {
        TranslationDetector.sCompleteRegions = false;
        assertEquals(""
                + "res/values/strings.xml:20: Error: \"show_all_apps\" is not translated in \"nl-NL\" (Dutch: Netherlands) [MissingTranslation]\n"
                + "    <string name=\"show_all_apps\">All</string>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~\n"
                + "res/values/strings.xml:23: Error: \"menu_wallpaper\" is not translated in \"nl-NL\" (Dutch: Netherlands) [MissingTranslation]\n"
                + "    <string name=\"menu_wallpaper\">Wallpaper</string>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/values/strings.xml:25: Error: \"menu_settings\" is not translated in \"cs\" (Czech), \"de-DE\" (German: Germany), \"es\" (Spanish), \"es-US\" (Spanish: United States), \"nl-NL\" (Dutch: Netherlands) [MissingTranslation]\n"
                + "    <string name=\"menu_settings\">Settings</string>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~\n"
                + "res/values-cs/arrays.xml:3: Error: \"security_questions\" is translated here but not found in default locale [ExtraTranslation]\n"
                + "  <string-array name=\"security_questions\">\n"
                + "                ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values-es/strings.xml:12: Also translated here\n"
                + "res/values-de-rDE/strings.xml:11: Error: \"continue_skip_label\" is translated here but not found in default locale [ExtraTranslation]\n"
                + "    <string name=\"continue_skip_label\">\"Weiter\"</string>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "5 errors, 0 warnings\n",

            lintProject(
                 mStrings,
                 mStrings2,
                 mStrings3,
                 mStrings4,
                 mStrings5,
                 mStrings6,
                 mArrays,
                 mDonottranslate,
                 mStrings7));
    }

    public void testTranslationWithCompleteRegions() throws Exception {
        TranslationDetector.sCompleteRegions = true;
        assertEquals(""
                + "res/values/strings.xml:19: Error: \"home_title\" is not translated in \"es-US\" (Spanish: United States) [MissingTranslation]\n"
                + "    <string name=\"home_title\">Home Sample</string>\n"
                + "            ~~~~~~~~~~~~~~~~~\n"
                + "res/values/strings.xml:20: Error: \"show_all_apps\" is not translated in \"es-US\" (Spanish: United States), \"nl-NL\" (Dutch: Netherlands) [MissingTranslation]\n"
                + "    <string name=\"show_all_apps\">All</string>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~\n"
                + "res/values/strings.xml:23: Error: \"menu_wallpaper\" is not translated in \"es-US\" (Spanish: United States), \"nl-NL\" (Dutch: Netherlands) [MissingTranslation]\n"
                + "    <string name=\"menu_wallpaper\">Wallpaper</string>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/values/strings.xml:25: Error: \"menu_settings\" is not translated in \"cs\" (Czech), \"de-DE\" (German: Germany), \"es-US\" (Spanish: United States), \"nl-NL\" (Dutch: Netherlands) [MissingTranslation]\n"
                + "    <string name=\"menu_settings\">Settings</string>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~\n"
                + "res/values/strings.xml:29: Error: \"wallpaper_instructions\" is not translated in \"es-US\" (Spanish: United States) [MissingTranslation]\n"
                + "    <string name=\"wallpaper_instructions\">Tap picture to set portrait wallpaper</string>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values-land/strings.xml:19: <No location-specific message\n"
                + "res/values-de-rDE/strings.xml:11: Error: \"continue_skip_label\" is translated here but not found in default locale [ExtraTranslation]\n"
                + "    <string name=\"continue_skip_label\">\"Weiter\"</string>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "6 errors, 0 warnings\n",

            lintProject(
                 mStrings,
                 mStrings2,
                 mStrings3,
                 mStrings5,
                 mStrings6,
                 mStrings7));
    }

    public void testBcp47() throws Exception {
        TranslationDetector.sCompleteRegions = false;
        assertEquals(""
                + "res/values/strings.xml:25: Error: \"menu_settings\" is not translated in \"tlh\" (Klingon; tlhIngan-Hol) [MissingTranslation]\n"
                + "    <string name=\"menu_settings\">Settings</string>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

                lintProject(
                        mStrings,
                        mStrings8));
    }

    public void testHandleBom() throws Exception {
        // This isn't really testing translation detection; it's just making sure that the
        // XML parser doesn't bomb on BOM bytes (byte order marker) at the beginning of
        // the XML document
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",
            lintProject(
                 xml("res/values/strings.xml", ""
                            + "\ufeff<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
                            + "    <string name=\"app_name\">Unit Test</string>\n"
                            + "</resources>\n")
            ));
    }

    public void testTranslatedArrays() throws Exception {
        TranslationDetector.sCompleteRegions = true;
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintProject(
                 xml("res/values/translatedarrays.xml", ""
                            + "<resources>\n"
                            + "    <string name=\"item1\">Item1</string>\n"
                            + "    <string name=\"item2\">Item2</string>\n"
                            + "    <string-array name=\"myarray\">\n"
                            + "        <item>@string/item1</item>\n"
                            + "        <item>@string/item2</item>\n"
                            + "    </string-array>\n"
                            + "</resources>\n"),
                 xml("res/values-cs/translatedarrays.xml", ""
                            + "<resources>\n"
                            + "    <string name=\"item1\">Item1-cs</string>\n"
                            + "    <string name=\"item2\">Item2-cs</string>\n"
                            + "</resources>\n")));
    }

    public void testTranslationSuppresss() throws Exception {
        TranslationDetector.sCompleteRegions = false;
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintProject(
                    xml("res/values/strings.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<resources xmlns:tools=\"http://schemas.android.com/tools\">\n"
                            + "    <!-- Home -->\n"
                            + "    <string name=\"home_title\">Home Sample</string>\n"
                            + "    <string name=\"show_all_apps\" tools:ignore=\"MissingTranslation\">All</string>\n"
                            + "\n"
                            + "    <!-- Home Menus -->\n"
                            + "    <string name=\"menu_wallpaper\" tools:ignore=\"MissingTranslation\">Wallpaper</string>\n"
                            + "    <string name=\"menu_search\">Search</string>\n"
                            + "    <string name=\"menu_settings\" tools:ignore=\"all\">Settings</string>\n"
                            + "    <string name=\"dummy\" translatable=\"false\">Ignore Me</string>\n"
                            + "\n"
                            + "    <!-- Wallpaper -->\n"
                            + "    <string name=\"wallpaper_instructions\">Tap picture to set portrait wallpaper</string>\n"
                            + "</resources>\n"),
                    xml("res/values-es/strings.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                            + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "    xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
                            + "    <string name=\"home_title\">\"Casa\"</string>\n"
                            + "    <string name=\"show_all_apps\">\"Todo\"</string>\n"
                            + "    <string name=\"menu_wallpaper\">\"Papel tapiz\"</string>\n"
                            + "    <string name=\"menu_search\">\"Búsqueda\"</string>\n"
                            + "    <!-- no translation found for menu_settings (1769059051084007158) -->\n"
                            + "    <skip />\n"
                            + "    <string name=\"wallpaper_instructions\">\"Puntee en la imagen para establecer papel tapiz vertical\"</string>\n"
                            + "    <string name=\"other\" tools:ignore=\"ExtraTranslation\">\"?\"</string>\n"
                            + "\n"
                            + "  <string-array name=\"security_questions\" tools:ignore=\"ExtraTranslation\">\n"
                            + "    <item>\"Comida favorita\"</item>\n"
                            + "    <item>\"Ciudad de nacimiento\"</item>\n"
                            + "    <item>\"Nombre de tu mejor amigo/a de la infancia\"</item>\n"
                            + "    <item>\"Nombre de tu colegio\"</item>\n"
                            + "  </string-array>\n"
                            + "</resources>\n"),
                    mStrings7));
    }

    public void testMixedTranslationArrays() throws Exception {
        // See issue http://code.google.com/p/android/issues/detail?id=29263
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",

                lintProject(
                        xml("res/values/strings.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<resources xmlns:tools=\"http://schemas.android.com/tools\" tools:ignore=\"MissingTranslation\">\n"
                            + "\n"
                            + "    <string name=\"test_string\">Test (English)</string>\n"
                            + "\n"
                            + "    <string-array name=\"test_string_array\">\n"
                            + "\t\t<item>@string/test_string</item>\n"
                            + "\t</string-array>\n"
                            + "\n"
                            + "</resources>\n"),
                        xml("res/values-fr/strings.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<resources>\n"
                            + "\n"
                            + "    <string-array name=\"test_string_array\">\n"
                            + "\t\t<item>Test (French)</item>\n"
                            + "\t</string-array>\n"
                            + "\n"
                            + "</resources>\n")));
    }

    public void testLibraryProjects() throws Exception {
        // If a library project provides additional locales, that should not force
        // the main project to include all those translations
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

             lintProject(
                 // Master project
                 manifest().pkg("foo.master").minSdk(14),
                 projectProperties().property("android.library.reference.1", "../LibraryProject"),
                 mStrings2_class,

                 // Library project
                 manifest().pkg("foo.library").minSdk(14),
                 source("../LibraryProject/project.properties", ""
                            + "# This file is automatically generated by Android Tools.\n"
                            + "# Do not modify this file -- YOUR CHANGES WILL BE ERASED!\n"
                            + "#\n"
                            + "# This file must be checked in Version Control Systems.\n"
                            + "#\n"
                            + "# To customize properties used by the Ant build system use,\n"
                            + "# \"ant.properties\", and override values to adapt the script to your\n"
                            + "# project structure.\n"
                            + "\n"
                            + "# Project target.\n"
                            + "target=android-14\n"
                            + "android.library=true\n"),

                 mStrings9,
                 mStrings10,
                 mStrings11,
                 mStrings12
             ));
    }

    public void testNonTranslatable1() throws Exception {
        TranslationDetector.sCompleteRegions = true;
        //noinspection all // Sample code
        assertEquals(""
                + "res/values-nb/nontranslatable.xml:3: Error: The resource string \"dummy\" has been marked as translatable=\"false\" [ExtraTranslation]\n"
                + "    <string name=\"dummy\">Ignore Me</string>\n"
                + "            ~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintProject(mNontranslatable,
                    xml("res/values-nb/nontranslatable.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<resources>\n"
                            + "    <string name=\"dummy\">Ignore Me</string>\n"
                            + "</resources>\n"
                            + "\n")));
    }

    public void testNonTranslatable2() throws Exception {
        TranslationDetector.sCompleteRegions = true;
        assertEquals(""
                + "res/values-nb/nontranslatable.xml:3: Error: Non-translatable resources should only be defined in the base values/ folder [ExtraTranslation]\n"
                + "    <string name=\"dummy\" translatable=\"false\">Ignore Me</string>\n"
                + "                         ~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintProject(mNontranslatable2));
    }

    public void testNonTranslatable3() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=92861
        // Don't treat "google_maps_key" or "google_maps_key_instructions" as translatable
        TranslationDetector.sCompleteRegions = true;
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",

                lintProject(xml("res/values/google_maps_api.xml", ""
                            + "<resources>\n"
                            + "    <string name=\"google_maps_key_instructions\"><!--\n"
                            + "    TODO: Before you run your application, you need a Google Maps API key.\n"
                            + "    Once you have your key, replace the \"google_maps_key\" string in this file.\n"
                            + "    --></string>\n"
                            + "\n"
                            + "    <string name=\"google_maps_key\">\n"
                            + "        YOUR_KEY_HERE\n"
                            + "    </string>\n"
                            + "</resources>\n"),
                        mStrings2_class,
                        mStrings2_class2));
    }

    public void testSpecifiedLanguageOk() throws Exception {
        TranslationDetector.sCompleteRegions = false;
        assertEquals(
            "No warnings.",

            lintProject(
                 mStrings13,
                 mStrings4,
                 mStrings5));
    }

    public void testSpecifiedLanguage() throws Exception {
        TranslationDetector.sCompleteRegions = false;
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintProject(
                 xml("res/values/strings.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                            + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "    tools:locale=\"es\">\n"
                            + "    <string name=\"home_title\">\"Casa\"</string>\n"
                            + "    <string name=\"show_all_apps\">\"Todo\"</string>\n"
                            + "    <string name=\"menu_wallpaper\">\"Papel tapiz\"</string>\n"
                            + "    <string name=\"menu_search\">\"Búsqueda\"</string>\n"
                            + "    <!-- no translation found for menu_settings (1769059051084007158) -->\n"
                            + "    <skip />\n"
                            + "    <string name=\"wallpaper_instructions\">\"Puntee en la imagen para establecer papel tapiz vertical\"</string>\n"
                            + "\n"
                            + "  <string-array name=\"security_questions\">\n"
                            + "    <item>\"Comida favorita\"</item>\n"
                            + "    <item>\"Ciudad de nacimiento\"</item>\n"
                            + "    <item>\"Nombre de tu mejor amigo/a de la infancia\"</item>\n"
                            + "    <item>\"Nombre de tu colegio\"</item>\n"
                            + "  </string-array>\n"
                            + "</resources>\n"),
                 mStrings5));
    }

    public void testAnalytics() throws Exception {
        // See http://code.google.com/p/android/issues/detail?id=43070
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",

                lintProject(
                        xml("res/values/analytics.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n"
                            + "<resources>\n"
                            + "  <!--Replace placeholder ID with your tracking ID-->\n"
                            + "  <string name=\"ga_trackingId\">UA-12345678-1</string>\n"
                            + "\n"
                            + "  <!--Enable Activity tracking-->\n"
                            + "  <bool name=\"ga_autoActivityTracking\">true</bool>\n"
                            + "\n"
                            + "  <!--Enable automatic exception tracking-->\n"
                            + "  <bool name=\"ga_reportUncaughtExceptions\">true</bool>\n"
                            + "\n"
                            + "  <!-- The screen names that will appear in your reporting -->\n"
                            + "  <string name=\"com.example.app.BaseActivity\">Home</string>\n"
                            + "  <string name=\"com.example.app.PrefsActivity\">Preferences</string>\n"
                            + "  <string name=\"test.pkg.OnClickActivity\">Clicks</string>\n"
                            + "\n"
                            + "  <string name=\"google_crash_reporting_api_key\" translatable=\"false\">AIzbSyCILMsOuUKwN3qhtxrPq7FFemDJUAXTyZ8</string>\n"
                            + "</resources>\n"),
                        mDonottranslate // to make app multilingual
                ));
    }

    public void testIssue33845() throws Exception {
        // See http://code.google.com/p/android/issues/detail?id=33845
        //noinspection all // Sample code
        assertEquals(""
                + "res/values/strings.xml:5: Error: \"dateTimeFormat\" is not translated in \"de\" (German) [MissingTranslation]\n"
                + "    <string name=\"dateTimeFormat\">MM/dd/yyyy - HH:mm</string>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

                lintProject(
                        classpath(),
                        mAndroidManifest,
                        projectProperties().compileSdk(17),
                        xml("res/values/strings.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                            + "<resources xmlns:tools=\"http://schemas.android.com/tools\" tools:locale=\"en\">\n"
                            + "\n"
                            + "    <string name=\"dateFormat\">MM/dd/yyyy</string>\n"
                            + "    <string name=\"dateTimeFormat\">MM/dd/yyyy - HH:mm</string>\n"
                            + "\n"
                            + "</resources>\n"),
                        xml("res/values-de/strings.xml", ""
                            + "<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n"
                            + "<resources>\n"
                            + "\n"
                            + "    <string name=\"dateFormat\">dd.MM.yyyy</string>\n"
                            + "\n"
                            + "</resources>\n"),
                        xml("res/values-en-rGB/strings.xml", ""
                            + "<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n"
                            + "<resources>\n"
                            + "\n"
                            + "    <string name=\"dateFormat\">dd/MM/yyyy</string>\n"
                            + "\n"
                            + "</resources>\n")
                ));
    }

    public void testIssue33845b() throws Exception {
        // Similar to issue 33845, but with some variations to the test data
        // See http://code.google.com/p/android/issues/detail?id=33845
        //noinspection all // Sample code
        assertEquals("No warnings.",

                lintProject(
                        classpath(),
                        mAndroidManifest,
                        projectProperties().compileSdk(17),
                        xml("res/values/styles.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                            + "\n"
                            + "    <!-- DeleteThisFileToGetRidOfOtherWarning -->\n"
                            + "\n"
                            + "</resources>\n"),
                        xml("res/values/strings.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                            + "<resources xmlns:tools=\"http://schemas.android.com/tools\" tools:locale=\"en\">\n"
                            + "\n"
                            + "    <string name=\"dateFormat\">defaultformat</string>\n"
                            + "\n"
                            + "</resources>\n"),
                        xml("res/values-en-rGB/strings.xml", ""
                            + "<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n"
                            + "<resources xmlns:tools=\"http://schemas.android.com/tools\">\n"
                            + "\n"
                            + "    <string name=\"dateFormat\">ukformat</string>\n"
                            + "    <string name=\"dummy\" tools:ignore=\"ExtraTranslation\">DeleteMeToGetRidOfOtherWarning</string>\n"
                            + "\n"
                            + "</resources>\n")
                ));
    }

    public void testEnglishRegionAndValuesAsEnglish1() throws Exception {
        TranslationDetector.sCompleteRegions = false;
        // tools:locale=en in base folder
        // Regression test for https://code.google.com/p/android/issues/detail?id=75879
        //noinspection all // Sample code
        assertEquals("No warnings.",

                lintProject(
                        xml("res/values/strings.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                            + "<resources xmlns:tools=\"http://schemas.android.com/tools\" tools:locale=\"en\">\n"
                            + "\n"
                            + "    <string name=\"dateFormat\">defaultformat</string>\n"
                            + "    <string name=\"other\">other</string>\n"
                            + "\n"
                            + "</resources>\n"),
                        mStrings3_class
                ));
    }

    public void testEnglishRegionAndValuesAsEnglish2() throws Exception {
        TranslationDetector.sCompleteRegions = false;
        // No tools:locale specified in the base folder: *assume* English
        // Regression test for https://code.google.com/p/android/issues/detail?id=75879
        //noinspection all // Sample code
        assertEquals(""
                + "res/values/strings.xml:5: Error: \"other\" is not translated in \"de-DE\" (German: Germany) [MissingTranslation]\n"
                + "    <string name=\"other\">other</string>\n"
                + "            ~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

                lintProject(
                        xml("res/values/strings.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                            + "<resources>\n"
                            + "\n"
                            + "    <string name=\"dateFormat\">defaultformat</string>\n"
                            + "    <string name=\"other\">other</string>\n"
                            + "\n"
                            + "</resources>\n"),
                        // Flagged because it's not the default locale:
                        mStrings3_class2,
                        // Not flagged because it's the implicit default locale
                        mStrings3_class
                ));
    }

    public void testEnglishRegionAndValuesAsEnglish3() throws Exception {
        TranslationDetector.sCompleteRegions = false;
        // tools:locale=de in base folder
        // Regression test for https://code.google.com/p/android/issues/detail?id=75879
        //noinspection all // Sample code
        assertEquals("No warnings.",

                lintProject(
                        xml("res/values/strings.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                            + "<resources xmlns:tools=\"http://schemas.android.com/tools\" tools:locale=\"de\">\n"
                            + "\n"
                            + "    <string name=\"dateFormat\">defaultformat</string>\n"
                            + "    <string name=\"other\">other</string>\n"
                            + "\n"
                            + "</resources>\n"),
                        mStrings3_class2
                ));
    }

    public void testResConfigs() throws Exception {
        TranslationDetector.sCompleteRegions = false;
        String expected = ""
                + "res/values/strings.xml:25: Error: \"menu_settings\" is not translated in \"cs\" (Czech), \"de-DE\" (German: Germany) [MissingTranslation]\n"
                + "    <string name=\"menu_settings\">Settings</string>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~\n"
                + "res/values-cs/arrays.xml:3: Error: \"security_questions\" is translated here but not found in default locale [ExtraTranslation]\n"
                + "  <string-array name=\"security_questions\">\n"
                + "                ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values-es/strings.xml:12: Also translated here\n"
                + "res/values-de-rDE/strings.xml:11: Error: \"continue_skip_label\" is translated here but not found in default locale [ExtraTranslation]\n"
                + "    <string name=\"continue_skip_label\">\"Weiter\"</string>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "3 errors, 0 warnings\n";

        //noinspection all // Sample code
        lint().files(
                mStrings,
                mStrings2,
                mStrings3,
                mStrings4,
                mStrings5,
                mStrings6,
                mArrays,
                mDonottranslate,
                mStrings7,
                gradle(""
                        + "apply plugin: 'com.android.application'\n"
                        + "\n"
                        + "android {\n"
                        + "    defaultConfig {\n"
                        + "        resConfigs \"cs\"\n"
                        + "    }\n"
                        + "    flavorDimensions  \"pricing\", \"releaseType\"\n"
                        + "    productFlavors {\n"
                        + "        beta {\n"
                        + "            flavorDimension \"releaseType\"\n"
                        + "            resConfig \"en\", \"de\"\n"
                        + "            resConfigs \"nodpi\", \"hdpi\"\n"
                        + "        }\n"
                        + "        normal { flavorDimension \"releaseType\" }\n"
                        + "        free { flavorDimension \"pricing\" }\n"
                        + "        paid { flavorDimension \"pricing\" }\n"
                        + "    }\n"
                        + "}"))
                .run()
                .expect(expected);
    }

    public void testMissingBaseCompletely() throws Exception {
        TranslationDetector.sCompleteRegions = false;
        assertEquals(""
                + "res/values-cs/strings.xml:4: Error: \"home_title\" is translated here but not found in default locale [ExtraTranslation]\n"
                + "    <string name=\"home_title\">\"Dom\u016f\"</string>\n"
                + "            ~~~~~~~~~~~~~~~~~\n"
                + "res/values-cs/strings.xml:5: Error: \"show_all_apps\" is translated here but not found in default locale [ExtraTranslation]\n"
                + "    <string name=\"show_all_apps\">\"V\u0161e\"</string>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~\n"
                + "res/values-cs/strings.xml:6: Error: \"menu_wallpaper\" is translated here but not found in default locale [ExtraTranslation]\n"
                + "    <string name=\"menu_wallpaper\">\"Tapeta\"</string>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/values-cs/strings.xml:7: Error: \"menu_search\" is translated here but not found in default locale [ExtraTranslation]\n"
                + "    <string name=\"menu_search\">\"Hledat\"</string>\n"
                + "            ~~~~~~~~~~~~~~~~~~\n"
                + "res/values-cs/strings.xml:10: Error: \"wallpaper_instructions\" is translated here but not found in default locale [ExtraTranslation]\n"
                + "    <string name=\"wallpaper_instructions\">\"Klepnutím na obrázek nastavíte tapetu portrétu\"</string>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "5 errors, 0 warnings\n",

                lintProject(mStrings2));
    }

    public void testMissingSomeBaseStrings() throws Exception {
        TranslationDetector.sCompleteRegions = false;
        assertEquals(""
                + "res/values-es/strings.xml:4: Error: \"home_title\" is translated here but not found in default locale [ExtraTranslation]\n"
                + "    <string name=\"home_title\">\"Casa\"</string>\n"
                + "            ~~~~~~~~~~~~~~~~~\n"
                + "res/values-es/strings.xml:5: Error: \"show_all_apps\" is translated here but not found in default locale [ExtraTranslation]\n"
                + "    <string name=\"show_all_apps\">\"Todo\"</string>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~\n"
                + "res/values-es/strings.xml:6: Error: \"menu_wallpaper\" is translated here but not found in default locale [ExtraTranslation]\n"
                + "    <string name=\"menu_wallpaper\">\"Papel tapiz\"</string>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/values-es/strings.xml:10: Error: \"wallpaper_instructions\" is translated here but not found in default locale [ExtraTranslation]\n"
                + "    <string name=\"wallpaper_instructions\">\"Puntee en la imagen para establecer papel tapiz vertical\"</string>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/values-es/strings.xml:12: Error: \"security_questions\" is translated here but not found in default locale [ExtraTranslation]\n"
                + "  <string-array name=\"security_questions\">\n"
                + "                ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "5 errors, 0 warnings\n",

                lintProject(
                        mStrings14,
                        mStrings4

                ));
    }

    public void testConfigKeys() throws Exception {
        // Some developer services create config files merged with your project, but in some
        // versions they were missing a translatable="false" entry. Since we know these aren't
        // keys you normally want to translate, let's filter them for users.
        TranslationDetector.sCompleteRegions = false;
        assertEquals("No warnings.",
                lintProject(
                        xml("res/values/config.xml", ""
                                + "<resources>\n"
                                + "    <string name=\"gcm_defaultSenderId\">SENDER_ID</string>\n"
                                + "    <string name=\"google_app_id\">App Id</string>\n"
                                + "    <string name=\"ga_trackingID\">Analytics</string>\n"
                                + "</resources>\n"),
                        xml("res/values/strings.xml", ""
                                + "<resources>\n"
                                + "    <string name=\"app_name\">My Application</string>\n"
                                + "</resources>\n"),
                        xml("res/values-nb/strings.xml", ""
                                + "<resources>\n"
                                + "    <string name=\"app_name\">Min Applikasjon</string>\n"
                                + "</resources>\n")
                ));
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mAndroidManifest = xml("AndroidManifest.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    package=\"com.example.mytest\"\n"
            + "    android:versionCode=\"1\"\n"
            + "    android:versionName=\"1.0\">\n"
            + "\n"
            + "    <uses-sdk\n"
            + "        android:minSdkVersion=\"8\"\n"
            + "        android:targetSdkVersion=\"17\"/>\n"
            + "\n"
            + "</manifest>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mArrays = xml("res/values-cs/arrays.xml", ""
            + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
            + "  <string-array name=\"security_questions\">\n"
            + "    <item>\"Oblíbené jídlo?\"</item>\n"
            + "    <item>\"M\u011bsto narození.\"</item>\n"
            + "    <item>\"Jméno nejlep\u0161ího kamaráda z d\u011btství?\"</item>\n"
            + "    <item>\"Název st\u0159ední \u0161koly\"</item>\n"
            + "  </string-array>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mDonottranslate = xml("res/values-es/donottranslate.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
            + "    <string name=\"full_wday_month_day_no_year\">EEEE, d MMMM</string>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mNontranslatable = xml("res/values/nontranslatable.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <string name=\"dummy\" translatable=\"false\">Ignore Me</string>\n"
            + "</resources>\n"
            + "\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mNontranslatable2 = xml("res/values-nb/nontranslatable.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <string name=\"dummy\" translatable=\"false\">Ignore Me</string>\n"
            + "</resources>\n"
            + "\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStrings = xml("res/values/strings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<!-- Copyright (C) 2007 The Android Open Source Project\n"
            + "\n"
            + "     Licensed under the Apache License, Version 2.0 (the \"License\");\n"
            + "     you may not use this file except in compliance with the License.\n"
            + "     You may obtain a copy of the License at\n"
            + "\n"
            + "          http://www.apache.org/licenses/LICENSE-2.0\n"
            + "\n"
            + "     Unless required by applicable law or agreed to in writing, software\n"
            + "     distributed under the License is distributed on an \"AS IS\" BASIS,\n"
            + "     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
            + "     See the License for the specific language governing permissions and\n"
            + "     limitations under the License.\n"
            + "-->\n"
            + "\n"
            + "<resources>\n"
            + "    <!-- Home -->\n"
            + "    <string name=\"home_title\">Home Sample</string>\n"
            + "    <string name=\"show_all_apps\">All</string>\n"
            + "\n"
            + "    <!-- Home Menus -->\n"
            + "    <string name=\"menu_wallpaper\">Wallpaper</string>\n"
            + "    <string name=\"menu_search\">Search</string>\n"
            + "    <string name=\"menu_settings\">Settings</string>\n"
            + "    <string name=\"dummy\" translatable=\"false\">Ignore Me</string>\n"
            + "\n"
            + "    <!-- Wallpaper -->\n"
            + "    <string name=\"wallpaper_instructions\">Tap picture to set portrait wallpaper</string>\n"
            + "</resources>\n"
            + "\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStrings10 = xml("../LibraryProject/res/values-cs/strings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
            + "    <string name=\"home_title\">\"Dom\u016f\"</string>\n"
            + "    <string name=\"show_all_apps\">\"V\u0161e\"</string>\n"
            + "    <string name=\"menu_wallpaper\">\"Tapeta\"</string>\n"
            + "    <string name=\"menu_search\">\"Hledat\"</string>\n"
            + "    <!-- no translation found for menu_settings (1769059051084007158) -->\n"
            + "    <skip />\n"
            + "    <string name=\"wallpaper_instructions\">\"Klepnutím na obrázek nastavíte tapetu portrétu\"</string>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStrings11 = xml("../LibraryProject/res/values-de/strings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
            + "    <string name=\"home_title\">\"Dom\u016f\"</string>\n"
            + "    <string name=\"show_all_apps\">\"V\u0161e\"</string>\n"
            + "    <string name=\"menu_wallpaper\">\"Tapeta\"</string>\n"
            + "    <string name=\"menu_search\">\"Hledat\"</string>\n"
            + "    <!-- no translation found for menu_settings (1769059051084007158) -->\n"
            + "    <skip />\n"
            + "    <string name=\"wallpaper_instructions\">\"Klepnutím na obrázek nastavíte tapetu portrétu\"</string>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStrings12 = xml("../LibraryProject/res/values-nl/strings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
            + "    <string name=\"home_title\">\"Dom\u016f\"</string>\n"
            + "    <string name=\"show_all_apps\">\"V\u0161e\"</string>\n"
            + "    <string name=\"menu_wallpaper\">\"Tapeta\"</string>\n"
            + "    <string name=\"menu_search\">\"Hledat\"</string>\n"
            + "    <!-- no translation found for menu_settings (1769059051084007158) -->\n"
            + "    <skip />\n"
            + "    <string name=\"wallpaper_instructions\">\"Klepnutím na obrázek nastavíte tapetu portrétu\"</string>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStrings13 = xml("res/values/strings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
            + "    <string name=\"home_title\">\"Casa\"</string>\n"
            + "    <string name=\"show_all_apps\">\"Todo\"</string>\n"
            + "    <string name=\"menu_wallpaper\">\"Papel tapiz\"</string>\n"
            + "    <string name=\"menu_search\">\"Búsqueda\"</string>\n"
            + "    <!-- no translation found for menu_settings (1769059051084007158) -->\n"
            + "    <skip />\n"
            + "    <string name=\"wallpaper_instructions\">\"Puntee en la imagen para establecer papel tapiz vertical\"</string>\n"
            + "\n"
            + "  <string-array name=\"security_questions\">\n"
            + "    <item>\"Comida favorita\"</item>\n"
            + "    <item>\"Ciudad de nacimiento\"</item>\n"
            + "    <item>\"Nombre de tu mejor amigo/a de la infancia\"</item>\n"
            + "    <item>\"Nombre de tu colegio\"</item>\n"
            + "  </string-array>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStrings14 = xml("res/values/strings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
            + "    <string name=\"menu_search\">\"Búsqueda\"</string>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStrings2 = xml("res/values-cs/strings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
            + "    <string name=\"home_title\">\"Dom\u016f\"</string>\n"
            + "    <string name=\"show_all_apps\">\"V\u0161e\"</string>\n"
            + "    <string name=\"menu_wallpaper\">\"Tapeta\"</string>\n"
            + "    <string name=\"menu_search\">\"Hledat\"</string>\n"
            + "    <!-- no translation found for menu_settings (1769059051084007158) -->\n"
            + "    <skip />\n"
            + "    <string name=\"wallpaper_instructions\">\"Klepnutím na obrázek nastavíte tapetu portrétu\"</string>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStrings2_class = xml("res/values/strings2.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <string name=\"hello\">Hello</string>\n"
            + "</resources>\n"
            + "\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStrings2_class2 = xml("res/values-nb/strings2.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <string name=\"hello\">Hello</string>\n"
            + "</resources>\n"
            + "\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStrings3 = xml("res/values-de-rDE/strings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
            + "    <string name=\"home_title\">\"Startseite\"</string>\n"
            + "    <string name=\"show_all_apps\">\"Alle\"</string>\n"
            + "    <string name=\"menu_wallpaper\">\"Bildschirmhintergrund\"</string>\n"
            + "    <string name=\"menu_search\">\"Suchen\"</string>\n"
            + "    <!-- no translation found for menu_settings (1769059051084007158) -->\n"
            + "    <skip />\n"
            + "    <string name=\"wallpaper_instructions\">\"Tippen Sie auf Bild, um Porträt-Bildschirmhintergrund einzustellen\"</string>\n"
            + "    <string name=\"continue_skip_label\">\"Weiter\"</string>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStrings3_class = xml("res/values-en-rGB/strings.xml", ""
            + "<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n"
            + "<resources>\n"
            + "\n"
            + "    <string name=\"dateFormat\">ukformat</string>\n"
            + "\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStrings3_class2 = xml("res/values-de-rDE/strings.xml", ""
            + "<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n"
            + "<resources>\n"
            + "\n"
            + "    <string name=\"dateFormat\">ukformat</string>\n"
            + "\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStrings4 = xml("res/values-es/strings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
            + "    <string name=\"home_title\">\"Casa\"</string>\n"
            + "    <string name=\"show_all_apps\">\"Todo\"</string>\n"
            + "    <string name=\"menu_wallpaper\">\"Papel tapiz\"</string>\n"
            + "    <string name=\"menu_search\">\"Búsqueda\"</string>\n"
            + "    <!-- no translation found for menu_settings (1769059051084007158) -->\n"
            + "    <skip />\n"
            + "    <string name=\"wallpaper_instructions\">\"Puntee en la imagen para establecer papel tapiz vertical\"</string>\n"
            + "\n"
            + "  <string-array name=\"security_questions\">\n"
            + "    <item>\"Comida favorita\"</item>\n"
            + "    <item>\"Ciudad de nacimiento\"</item>\n"
            + "    <item>\"Nombre de tu mejor amigo/a de la infancia\"</item>\n"
            + "    <item>\"Nombre de tu colegio\"</item>\n"
            + "  </string-array>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStrings5 = xml("res/values-es-rUS/strings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
            + "    <string name=\"menu_search\">\"Búsqueda\"</string>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStrings6 = xml("res/values-land/strings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<!-- Copyright (C) 2007 The Android Open Source Project\n"
            + "\n"
            + "     Licensed under the Apache License, Version 2.0 (the \"License\");\n"
            + "     you may not use this file except in compliance with the License.\n"
            + "     You may obtain a copy of the License at\n"
            + "\n"
            + "          http://www.apache.org/licenses/LICENSE-2.0\n"
            + "\n"
            + "     Unless required by applicable law or agreed to in writing, software\n"
            + "     distributed under the License is distributed on an \"AS IS\" BASIS,\n"
            + "     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
            + "     See the License for the specific language governing permissions and\n"
            + "     limitations under the License.\n"
            + "-->\n"
            + "\n"
            + "<resources>\n"
            + "    <!-- Wallpaper -->\n"
            + "    <string name=\"wallpaper_instructions\">Tap image to set landscape wallpaper</string>\n"
            + "</resources>\n"
            + "\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStrings7 = xml("res/values-nl-rNL/strings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
            + "    <string name=\"home_title\">\"Start\"</string>\n"
            + "    <!-- Commented out in the unit test to generate extra warnings:\n"
            + "    <string name=\"show_all_apps\">\"Alles\"</string>\n"
            + "    <string name=\"menu_wallpaper\">\"Achtergrond\"</string>\n"
            + "    -->\n"
            + "    <string name=\"menu_search\">\"Zoeken\"</string>\n"
            + "    <!-- no translation found for menu_settings (1769059051084007158) -->\n"
            + "    <skip />\n"
            + "    <string name=\"wallpaper_instructions\">\"Tik op afbeelding om portretachtergrond in te stellen\"</string>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStrings8 = xml("res/values-b+tlh/strings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
            + "    <string name=\"home_title\">\"Dom\u016f\"</string>\n"
            + "    <string name=\"show_all_apps\">\"V\u0161e\"</string>\n"
            + "    <string name=\"menu_wallpaper\">\"Tapeta\"</string>\n"
            + "    <string name=\"menu_search\">\"Hledat\"</string>\n"
            + "    <!-- no translation found for menu_settings (1769059051084007158) -->\n"
            + "    <skip />\n"
            + "    <string name=\"wallpaper_instructions\">\"Klepnutím na obrázek nastavíte tapetu portrétu\"</string>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStrings9 = xml("../LibraryProject/res/values/strings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<!-- Copyright (C) 2007 The Android Open Source Project\n"
            + "\n"
            + "     Licensed under the Apache License, Version 2.0 (the \"License\");\n"
            + "     you may not use this file except in compliance with the License.\n"
            + "     You may obtain a copy of the License at\n"
            + "\n"
            + "          http://www.apache.org/licenses/LICENSE-2.0\n"
            + "\n"
            + "     Unless required by applicable law or agreed to in writing, software\n"
            + "     distributed under the License is distributed on an \"AS IS\" BASIS,\n"
            + "     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
            + "     See the License for the specific language governing permissions and\n"
            + "     limitations under the License.\n"
            + "-->\n"
            + "\n"
            + "<resources>\n"
            + "    <!-- Home -->\n"
            + "    <string name=\"home_title\">Home Sample</string>\n"
            + "    <string name=\"show_all_apps\">All</string>\n"
            + "\n"
            + "    <!-- Home Menus -->\n"
            + "    <string name=\"menu_wallpaper\">Wallpaper</string>\n"
            + "    <string name=\"menu_search\">Search</string>\n"
            + "    <string name=\"menu_settings\">Settings</string>\n"
            + "    <string name=\"dummy\" translatable=\"false\">Ignore Me</string>\n"
            + "\n"
            + "    <!-- Wallpaper -->\n"
            + "    <string name=\"wallpaper_instructions\">Tap picture to set portrait wallpaper</string>\n"
            + "</resources>\n"
            + "\n");
}
