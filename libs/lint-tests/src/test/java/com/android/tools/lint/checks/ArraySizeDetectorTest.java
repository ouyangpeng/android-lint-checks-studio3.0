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
public class ArraySizeDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new ArraySizeDetector();
    }
    public void testArraySizes() throws Exception {
        String expected = ""
                + "res/values/arrays.xml:3: Warning: Array security_questions has an inconsistent number of items (3 in values-nl-rNL/arrays.xml, 4 in values-cs/arrays.xml) [InconsistentArrays]\n"
                + "    <string-array name=\"security_questions\">\n"
                + "    ^\n"
                + "    res/values-cs/arrays.xml:3: Declaration with array size (4)\n"
                + "    res/values-es/strings.xml:12: Declaration with array size (4)\n"
                + "    res/values-nl-rNL/arrays.xml:3: Declaration with array size (3)\n"
                + "res/values/arrays.xml:10: Warning: Array signal_strength has an inconsistent number of items (5 in values/arrays.xml, 6 in values-land/arrays.xml) [InconsistentArrays]\n"
                + "    <array name=\"signal_strength\">\n"
                + "    ^\n"
                + "    res/values-land/arrays.xml:2: Declaration with array size (6)\n"
                + "0 errors, 2 warnings\n";
        lint().files(
                mArrays,
                mArrays2,
                mArrays3,
                mArrays4,
                mStrings)
                .run()
                .expect(expected);
    }

    public void testMultipleArrays() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/values/stringarrays.xml:3: Warning: Array map_density_desc has an inconsistent number of items (5 in values/stringarrays.xml, 1 in values-it/stringarrays.xml) [InconsistentArrays]\n"
                + "    <string-array name=\"map_density_desc\">\n"
                + "    ^\n"
                + "    res/values-it/stringarrays.xml:6: Declaration with array size (1)\n"
                + "0 errors, 1 warnings\n",

            lintProject(
                 xml("res/values-it/stringarrays.xml", ""
                            + "<?xml version='1.0' encoding='utf-8'?>\n"
                            + "<resources>\n"
                            + "    <string-array name=\"track_type_desc\">\n"
                            + "        <item>Pendenza</item>\n"
                            + "    </string-array>\n"
                            + "    <string-array name=\"map_density_desc\">\n"
                            + "        <item>Automatico (mappa leggibile su display HD)</item>\n"
                            + "    </string-array>\n"
                            + "    <string-array name=\"cache_size_desc\">\n"
                            + "        <item>Piccolo (100)</item>\n"
                            + "    </string-array>\n"
                            + "</resources>\n"),
                 xml("res/values/stringarrays.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<resources>\n"
                            + "    <string-array name=\"map_density_desc\">\n"
                            + "        <item>Automatic (readable map on HD displays)</item>\n"
                            + "        <item>1 map pixel = 1 screen pixel</item>\n"
                            + "        <item>1 map pixel = 1.25 screen pixels</item>\n"
                            + "        <item>1 map pixel = 1.5 screen pixels</item>\n"
                            + "        <item>1 map pixel = 2 screen pixels</item>\n"
                            + "    </string-array>\n"
                            + "    <string-array name=\"spatial_resolution_desc\">\n"
                            + "        <item>5m/yd (fine, default)</item>\n"
                            + "    </string-array>\n"
                            + "</resources>\n")));
    }

    public void testArraySizesSuppressed() throws Exception {
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintProject(
                 mArrays,
                 xml("res/values-land/arrays.xml", ""
                            + "<resources xmlns:tools=\"http://schemas.android.com/tools\">\n"
                            + "    <array name=\"signal_strength\" tools:ignore=\"InconsistentArrays\">\n"
                            + "        <item>@drawable/ic_setups_signal_0</item>\n"
                            + "        <item>@drawable/ic_setups_signal_1</item>\n"
                            + "        <item>@drawable/ic_setups_signal_2</item>\n"
                            + "        <item>@drawable/ic_setups_signal_3</item>\n"
                            + "        <item>@drawable/ic_setups_signal_4</item>\n"
                            + "        <item>@drawable/extra</item>\n"
                            + "    </array>\n"
                            + "</resources>\n"
                            + "\n")));
    }

    public void testArraySizesIncremental() throws Exception {
        assertEquals(""
                + "res/values/arrays.xml:3: Warning: Array security_questions has an inconsistent number of items (4 in values/arrays.xml, 3 in values-nl-rNL/arrays.xml) [InconsistentArrays]\n"
                + "    <string-array name=\"security_questions\">\n"
                + "    ^\n"
                + "res/values/arrays.xml:10: Warning: Array signal_strength has an inconsistent number of items (5 in values/arrays.xml, 6 in values-land/arrays.xml) [InconsistentArrays]\n"
                + "    <array name=\"signal_strength\">\n"
                + "    ^\n"
                + "0 errors, 2 warnings\n",

            lintProjectIncrementally("res/values/arrays.xml",
                    mArrays,
                    mArrays2,
                    mArrays3,
                    mArrays4,
                    mStrings));
    }
    @SuppressWarnings("all") // Sample code
    private TestFile mArrays = xml("res/values/arrays.xml", ""
            + "<resources>\n"
            + "    <!-- Choices for Locations in SetupWizard's Set Time and Data Activity -->\n"
            + "    <string-array name=\"security_questions\">\n"
            + "        <item>Favorite food?</item>\n"
            + "        <item>City of birth?</item>\n"
            + "        <item>Best childhood friend\\'s name?</item>\n"
            + "        <item>Highschool name?</item>\n"
            + "    </string-array>\n"
            + "\n"
            + "    <array name=\"signal_strength\">\n"
            + "        <item>@drawable/ic_setups_signal_0</item>\n"
            + "        <item>@drawable/ic_setups_signal_1</item>\n"
            + "        <item>@drawable/ic_setups_signal_2</item>\n"
            + "        <item>@drawable/ic_setups_signal_3</item>\n"
            + "        <item>@drawable/ic_setups_signal_4</item>\n"
            + "    </array>\n"
            + "</resources>\n"
            + "\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mArrays2 = xml("res/values-cs/arrays.xml", ""
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
    private TestFile mArrays3 = xml("res/values-land/arrays.xml", ""
            + "<resources>\n"
            + "    <array name=\"signal_strength\">\n"
            + "        <item>@drawable/ic_setups_signal_0</item>\n"
            + "        <item>@drawable/ic_setups_signal_1</item>\n"
            + "        <item>@drawable/ic_setups_signal_2</item>\n"
            + "        <item>@drawable/ic_setups_signal_3</item>\n"
            + "        <item>@drawable/ic_setups_signal_4</item>\n"
            + "        <item>@drawable/extra</item>\n"
            + "    </array>\n"
            + "</resources>\n"
            + "\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mArrays4 = xml("res/values-nl-rNL/arrays.xml", ""
            + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
            + "  <string-array name=\"security_questions\">\n"
            + "    <item>\"Favoriete eten?\"</item>\n"
            + "    <item>\"Geboorteplaats?\"</item>\n"
            + "    <item>\"Naam van middelbare school?\"</item>\n"
            + "  </string-array>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStrings = xml("res/values-es/strings.xml", ""
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
}
