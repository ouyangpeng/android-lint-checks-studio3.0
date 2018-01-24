/*
 * Copyright (C) 2012 The Android Open Source Project
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
public class DuplicateResourceDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new DuplicateResourceDetector();
    }

    public void test() throws Exception {
        String expected = ""
                + "res/values/customattr2.xml:2: Error: ContentFrame has already been defined in this folder [DuplicateDefinition]\n"
                + "    <declare-styleable name=\"ContentFrame\">\n"
                + "                       ~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/customattr.xml:2: Previously defined here\n"
                + "res/values/strings2.xml:19: Error: wallpaper_instructions has already been defined in this folder [DuplicateDefinition]\n"
                + "    <string name=\"wallpaper_instructions\">Tap image to set landscape wallpaper</string>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/strings.xml:29: Previously defined here\n"
                + "2 errors, 0 warnings\n";
        lint().files(
                strings,
                strings2,
                strings3,
                customattr,
                customattr2)
                .run()
                .expect(expected);
    }

    public void testDotAliases() throws Exception {
        String expected = ""
                + "res/values/duplicate-strings2.xml:5: Error: app_name has already been defined in this folder (app_name is equivalent to app.name) [DuplicateDefinition]\n"
                + "    <string name=\"app.name\">App Name 1</string>\n"
                + "            ~~~~~~~~~~~~~~~\n"
                + "    res/values/duplicate-strings2.xml:4: Previously defined here\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/values/duplicate-strings2.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "\n"
                        + "    <string name=\"app_name\">App Name</string>\n"
                        + "    <string name=\"app.name\">App Name 1</string>\n"
                        + "\n"
                        + "</resources>\n"
                        + "\n"))
                .run()
                .expect(expected);
    }

    public void testSameFile() throws Exception {
        String expected = ""
                + "res/values/duplicate-strings.xml:6: Error: app_name has already been defined in this folder [DuplicateDefinition]\n"
                + "    <string name=\"app_name\">App Name 1</string>\n"
                + "            ~~~~~~~~~~~~~~~\n"
                + "    res/values/duplicate-strings.xml:4: Previously defined here\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/values/duplicate-strings.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "\n"
                        + "    <string name=\"app_name\">App Name</string>\n"
                        + "    <string name=\"hello_world\">Hello world!</string>\n"
                        + "    <string name=\"app_name\">App Name 1</string>\n"
                        + "    <string name=\"app_name2\">App Name 2</string>\n"
                        + "\n"
                        + "</resources>\n"
                        + "\n"))
                .run()
                .expect(expected);
    }

    public void testStyleItems() throws Exception {
        String expected = ""
                + "res/values/duplicate-items.xml:7: Error: android:textColor has already been defined in this <style> [DuplicateDefinition]\n"
                + "        <item name=\"android:textColor\">#ff0000</item>\n"
                + "              ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/duplicate-items.xml:5: Previously defined here\n"
                + "res/values/duplicate-items.xml:13: Error: contentId has already been defined in this <declare-styleable> [DuplicateDefinition]\n"
                + "        <attr name=\"contentId\" format=\"integer\" />\n"
                + "              ~~~~~~~~~~~~~~~~\n"
                + "    res/values/duplicate-items.xml:11: Previously defined here\n"
                + "2 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/values/duplicate-items.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "\n"
                        + "    <style name=\"DetailsPage_BuyButton\" parent=\"@style/DetailsPage_Button\">\n"
                        + "        <item name=\"android:textColor\">@color/buy_button</item>\n"
                        + "        <item name=\"android:background\">@drawable/details_page_buy_button</item>\n"
                        + "        <item name=\"android:textColor\">#ff0000</item>\n"
                        + "    </style>\n"
                        + "\n"
                        + "    <declare-styleable name=\"ContentFrame\">\n"
                        + "        <attr name=\"content\" format=\"reference\" />\n"
                        + "        <attr name=\"contentId\" format=\"reference\" />\n"
                        + "        <attr name=\"contentId\" format=\"integer\" />\n"
                        + "    </declare-styleable>\n"
                        + "\n"
                        + "</resources>\n"
                        + "\n"))
                .run()
                .expect(expected);
    }

    public void testOk() throws Exception {
        //noinspection all // Sample code
        lint().files(
                strings,
                strings3,
                xml("res/values-de-rDE/strings.xml", ""
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
                        + "</resources>\n"),
                xml("res/values-es/strings.xml", ""
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
                        + "</resources>\n"),
                xml("res/values-es-rUS/strings.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
                        + "    <string name=\"menu_search\">\"Búsqueda\"</string>\n"
                        + "</resources>\n"),
                strings4,
                xml("res/values-cs/arrays.xml", ""
                        + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
                        + "  <string-array name=\"security_questions\">\n"
                        + "    <item>\"Oblíbené jídlo?\"</item>\n"
                        + "    <item>\"M\u011bsto narození.\"</item>\n"
                        + "    <item>\"Jméno nejlep\u0161ího kamaráda z d\u011btství?\"</item>\n"
                        + "    <item>\"Název st\u0159ední \u0161koly\"</item>\n"
                        + "  </string-array>\n"
                        + "</resources>\n"),
                xml("res/values-es/donottranslate.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
                        + "    <string name=\"full_wday_month_day_no_year\">EEEE, d MMMM</string>\n"
                        + "</resources>\n"),
                xml("res/values-nl-rNL/strings.xml", ""
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
                        + "</resources>\n"))
                .run()
                .expectClean();
    }

    public void testResourceAliases() throws Exception {
        String expected = ""
                + "res/values/refs.xml:3: Error: Unexpected resource reference type; expected value of type @string/ [ReferenceType]\n"
                + "    <item name=\"invalid1\" type=\"string\">@layout/other</item>\n"
                + "                                        ^\n"
                + "res/values/refs.xml:5: Error: Unexpected resource reference type; expected value of type @drawable/ [ReferenceType]\n"
                + "          @layout/other\n"
                + "          ^\n"
                + "res/values/refs.xml:10: Error: Unexpected resource reference type; expected value of type @string/ [ReferenceType]\n"
                + "    <string name=\"invalid4\">@layout/indirect</string>\n"
                + "                            ^\n"
                + "res/values/refs.xml:15: Error: Unexpected resource reference type; expected value of type @color/ [ReferenceType]\n"
                + "    <item name=\"drawableAsColor\" type=\"color\">@drawable/my_drawable</item>\n"
                + "                                              ^\n"
                + "4 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/values/refs.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <item name=\"invalid1\" type=\"string\">@layout/other</item>\n"
                        + "    <item name=\"invalid1\" type=\"drawable\">\n"
                        + "          @layout/other\n"
                        + "    </item>\n"
                        + "    <item name=\"string1\" type=\"string\">Plain String</item>\n"
                        + "    <item name=\"string2\" type=\"string\">@string/indirect</item>\n"
                        + "    <string name=\"string3\">@string/indirect</string>\n"
                        + "    <string name=\"invalid4\">@layout/indirect</string>\n"
                        + "    <item name=\"other2\" type=\"layout\">@layout/indirect2</item>\n"
                        + "    <item name=\"indirect2\" type=\"layout\">  @layout/indirect1 </item>\n"
                        + "    <item name=\"indirect1\" type=\"layout\">@layout/to</item>\n"
                        + "    <item name=\"colorAsDrawable\" type=\"drawable\">@color/my_color</item>\n"
                        + "    <item name=\"drawableAsColor\" type=\"color\">@drawable/my_drawable</item>\n"
                        + "</resources>\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for res/values/refs.xml line 2: Replace with @string/:\n"
                        + "@@ -3 +3\n"
                        + "-     <item name=\"invalid1\" type=\"string\">@layout/other</item>\n"
                        + "+     <item name=\"invalid1\" type=\"string\">@string/other</item>\n"
                        + "Fix for res/values/refs.xml line 4: Replace with @drawable/:\n"
                        + "@@ -5 +5\n"
                        + "-           @layout/other\n"
                        + "+           @drawable/other\n"
                        + "Fix for res/values/refs.xml line 9: Replace with @string/:\n"
                        + "@@ -10 +10\n"
                        + "-     <string name=\"invalid4\">@layout/indirect</string>\n"
                        + "+     <string name=\"invalid4\">@string/indirect</string>\n"
                        + "Fix for res/values/refs.xml line 14: Replace with @color/:\n"
                        + "@@ -15 +15\n"
                        + "-     <item name=\"drawableAsColor\" type=\"color\">@drawable/my_drawable</item>\n"
                        + "+     <item name=\"drawableAsColor\" type=\"color\">@color/my_drawable</item>\n");
    }

    public void testPublic() throws Exception {
        //noinspection all // Sample code
        lint().files(
                xml("res/values/refs.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <item type='dimen' name='largePadding'>20dp</item>\n"
                        + "    <item type='dimen' name='smallPadding'>15dp</item>\n"
                        + "    <public type='dimen' name='largePadding' />"
                        + "    <public type='string' name='largePadding' />"
                        + "    <public type='dimen' name='smallPadding' />"
                        + "    <public type='dimen' name='smallPadding' />"
                        + "</resources>\n"))
                .run()
                .expectClean();
    }

    public void testMipmapDrawable() throws Exception {
        // https://code.google.com/p/android/issues/detail?id=109892
        //noinspection all // Sample code
        lint().files(
                xml("res/values/refs2.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <drawable name=\"ic_shortcut_resource_name\">@mipmap/ic_shortcut_resource_name</drawable>\n"
                        + "</resources>\n"))
                .run()
                .expectClean();
    }

    public void testInvalidXml() throws Exception {

        // Regression test for https://code.google.com/p/android/issues/detail?id=224150
        // 224150: Flag apostrophes escaping in XML string resources
        String expected = ""
                + "res/values/strings.xml:3: Error: Apostrophe not preceded by \\ [StringEscaping]\n"
                + "<string name=\"some_string\">'ERROR'</string>\n"
                + "                           ^\n"
                + "res/values/strings.xml:5: Error: Apostrophe not preceded by \\ [StringEscaping]\n"
                + "<string name=\"some_string3\">What's New</string>\n"
                + "                                ^\n"
                + "res/values/strings.xml:12: Error: Bad character in \\u unicode escape sequence [StringEscaping]\n"
                + "<string name=\"some_string10\">Unicode\\u12.</string>\n"
                + "                                        ^\n"
                + "3 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/values/strings.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "<string name=\"some_string\">'ERROR'</string>\n"
                        + "<string name=\"some_string2\">\"'OK'\"</string>\n"
                        + "<string name=\"some_string3\">What's New</string>\n"
                        + "<string name=\"some_string4\">Unfinished\\</string>\n"
                        + "<string name=\"some_string5\">Unicode\\u</string>\n"
                        + "<string name=\"some_string6\">Unicode\\u1</string>\n"
                        + "<string name=\"some_string7\">Unicode\\u12</string>\n"
                        + "<string name=\"some_string8\">Unicode\\u123</string>\n"
                        + "<string name=\"some_string9\">Unicode\\u1234</string>\n"
                        + "<string name=\"some_string10\">Unicode\\u12.</string>\n"
                        + "<string name=\"news\">  \"  What's New \"    </string>\n"
                        + "</resources>\n"
                        + "\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for res/values/strings.xml line 2: Escape Apostrophe:\n"
                        + "@@ -3 +3\n"
                        + "- <string name=\"some_string\">'ERROR'</string>\n"
                        + "+ <string name=\"some_string\">\\'ERROR'</string>\n"
                        + "Fix for res/values/strings.xml line 4: Escape Apostrophe:\n"
                        + "@@ -5 +5\n"
                        + "- <string name=\"some_string3\">What's New</string>\n"
                        + "+ <string name=\"some_string3\">What\\'s New</string>\n");
    }

    @SuppressWarnings("all") // Sample code
    private TestFile customattr = xml("res/values/customattr.xml", ""
            + "<resources>\n"
            + "    <declare-styleable name=\"ContentFrame\">\n"
            + "        <attr name=\"content\" format=\"reference\" />\n"
            + "        <attr name=\"contentId\" format=\"reference\" />\n"
            + "    </declare-styleable>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile customattr2 = xml("res/values/customattr2.xml", ""
            + "<resources>\n"
            + "    <declare-styleable name=\"ContentFrame\">\n"
            + "        <attr name=\"content\" format=\"reference\" />\n"
            + "        <attr name=\"contentId\" format=\"reference\" />\n"
            + "    </declare-styleable>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile strings = xml("res/values/strings.xml", ""
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
    private TestFile strings2 = xml("res/values/strings2.xml", ""
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
    private TestFile strings3 = xml("res/values-cs/strings.xml", ""
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
    private TestFile strings4 = xml("res/values-land/strings.xml", ""
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
}
