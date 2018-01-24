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
public class TypoDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new TypoDetector();
    }

    public void testPlainValues() {
        String expected = ""
                + "res/values/strings.xml:6: Warning: \"Andriod\" is a common misspelling; did you mean \"Android\" ? [Typos]\n"
                + "    <string name=\"s2\">Andriod activites!</string>\n"
                + "                      ^\n"
                + "res/values/strings.xml:6: Warning: \"activites\" is a common misspelling; did you mean \"activities\" ? [Typos]\n"
                + "    <string name=\"s2\">Andriod activites!</string>\n"
                + "                              ^\n"
                + "res/values/strings.xml:8: Warning: \"Cmoputer\" is a common misspelling; did you mean \"Computer\" ? [Typos]\n"
                + "    <string name=\"s3\"> (Cmoputer </string>\n"
                + "                        ^\n"
                + "res/values/strings.xml:10: Warning: \"throught\" is a common misspelling; did you mean \"thought\" or \"through\" or \"throughout\" ? [Typos]\n"
                + "    <string name=\"s4\"><b>throught</b></string>\n"
                + "                         ^\n"
                + "res/values/strings.xml:12: Warning: \"Seach\" is a common misspelling; did you mean \"Search\" ? [Typos]\n"
                + "    <string name=\"s5\">Seach</string>\n"
                + "                      ^\n"
                + "res/values/strings.xml:16: Warning: \"Tuscon\" is a common misspelling; did you mean \"Tucson\" ? [Typos]\n"
                + "    <string name=\"s7\">Tuscon tuscon</string>\n"
                + "                      ^\n"
                + "res/values/strings.xml:20: Warning: \"Ok\" is usually capitalized as \"OK\" [Typos]\n"
                + "    <string name=\"dlg_button_ok\">Ok</string>\n"
                + "                                 ^\n"
                + "0 errors, 7 warnings\n";
        lint().files(typos1).run().expect(expected).expectFixDiffs(""
                + "Fix for res/values/strings.xml line 5: Replace with \"Android\":\n"
                + "@@ -6 +6\n"
                + "-     <string name=\"s2\">Andriod activites!</string>\n"
                + "+     <string name=\"s2\">Android activites!</string>\n"
                + "Fix for res/values/strings.xml line 5: Replace with \"activities\":\n"
                + "@@ -6 +6\n"
                + "-     <string name=\"s2\">Andriod activites!</string>\n"
                + "+     <string name=\"s2\">Andriod activities!</string>\n"
                + "Fix for res/values/strings.xml line 7: Replace with \"Computer\":\n"
                + "@@ -8 +8\n"
                + "-     <string name=\"s3\"> (Cmoputer </string>\n"
                + "+     <string name=\"s3\"> (Computer </string>\n"
                + "Fix for res/values/strings.xml line 9: Replace with \"thought\":\n"
                + "@@ -10 +10\n"
                + "-     <string name=\"s4\"><b>throught</b></string>\n"
                + "+     <string name=\"s4\"><b>thought</b></string>\n"
                + "Fix for res/values/strings.xml line 9: Replace with \"through\":\n"
                + "@@ -10 +10\n"
                + "-     <string name=\"s4\"><b>throught</b></string>\n"
                + "+     <string name=\"s4\"><b>through</b></string>\n"
                + "Fix for res/values/strings.xml line 9: Replace with \"throughout\":\n"
                + "@@ -10 +10\n"
                + "-     <string name=\"s4\"><b>throught</b></string>\n"
                + "+     <string name=\"s4\"><b>throughout</b></string>\n"
                + "Fix for res/values/strings.xml line 11: Replace with \"Search\":\n"
                + "@@ -12 +12\n"
                + "-     <string name=\"s5\">Seach</string>\n"
                + "+     <string name=\"s5\">Search</string>\n"
                + "Fix for res/values/strings.xml line 15: Replace with \"Tucson\":\n"
                + "@@ -16 +16\n"
                + "-     <string name=\"s7\">Tuscon tuscon</string>\n"
                + "+     <string name=\"s7\">Tucson tuscon</string>\n"
                + "Fix for res/values/strings.xml line 19: Replace with \"OK\":\n"
                + "@@ -20 +20\n"
                + "-     <string name=\"dlg_button_ok\">Ok</string>\n"
                + "+     <string name=\"dlg_button_ok\">OK</string>\n");
    }

    public void testRepeatedWords() {
        String expected = ""
                + "res/values/strings.xml:5: Warning: Repeated word \"to\" in message: possible typo [Typos]\n"
                + "     extra location provider commands.  This may allow the app to to interfere\n"
                + "                                                               ^\n"
                + "res/values/strings.xml:7: Warning: Repeated word \"zü\" in message: possible typo [Typos]\n"
                + "    <string name=\"other\">\"ü test\\n zü zü\"</string>\n"
                + "                                   ^\n"
                + "0 errors, 2 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/values/strings.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <!-- Repeated words -->\n"
                        + "    <string name=\"permdesc_accessLocationExtraCommands\">Allows the app to access\n"
                        + "     extra location provider commands.  This may allow the app to to interfere\n"
                        + "     with the operation of the GPS or other location sources.</string>\n"
                        + "    <string name=\"other\">\"ü test\\n zü zü\"</string>\n"
                        + "    <string name=\"ignore1\">android/android/foo\"</string>\n"
                        + "    <string name=\"ignore2\">%s/%s/%s</string>\n"
                        + "    <string name=\"ignore3\">\"Dial (866) 555 0123\\\" \\n\\\"Dial 911, 811, ...\"</string>\n"
                        + "</resources>\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for res/values/strings.xml line 4: Delete repeated word:\n"
                        + "@@ -5 +5\n"
                        + "-      extra location provider commands.  This may allow the app to to interfere\n"
                        + "+      extra location provider commands.  This may allow the app to interfere\n"
                        + "Fix for res/values/strings.xml line 6: Delete repeated word:\n"
                        + "@@ -7 +7\n"
                        + "-     <string name=\"other\">\"ü test\\n zü zü\"</string>\n"
                        + "+     <string name=\"other\">\"ü test\\n zü\"</string>\n");
    }

    public void testEnLanguage() {
        String expected = ""
                + "res/values-en-rUS/strings-en.xml:6: Warning: \"Andriod\" is a common misspelling; did you mean \"Android\" ? [Typos]\n"
                + "    <string name=\"s2\">Andriod activites!</string>\n"
                + "                      ^\n"
                + "res/values-en-rUS/strings-en.xml:6: Warning: \"activites\" is a common misspelling; did you mean \"activities\" ? [Typos]\n"
                + "    <string name=\"s2\">Andriod activites!</string>\n"
                + "                              ^\n"
                + "res/values-en-rUS/strings-en.xml:8: Warning: \"Cmoputer\" is a common misspelling; did you mean \"Computer\" ? [Typos]\n"
                + "    <string name=\"s3\"> (Cmoputer </string>\n"
                + "                        ^\n"
                + "res/values-en-rUS/strings-en.xml:10: Warning: \"throught\" is a common misspelling; did you mean \"thought\" or \"through\" or \"throughout\" ? [Typos]\n"
                + "    <string name=\"s4\"><b>throught</b></string>\n"
                + "                         ^\n"
                + "res/values-en-rUS/strings-en.xml:12: Warning: \"Seach\" is a common misspelling; did you mean \"Search\" ? [Typos]\n"
                + "    <string name=\"s5\">Seach</string>\n"
                + "                      ^\n"
                + "res/values-en-rUS/strings-en.xml:16: Warning: \"Tuscon\" is a common misspelling; did you mean \"Tucson\" ? [Typos]\n"
                + "    <string name=\"s7\">Tuscon tuscon</string>\n"
                + "                      ^\n"
                + "res/values-en-rUS/strings-en.xml:20: Warning: \"Ok\" is usually capitalized as \"OK\" [Typos]\n"
                + "    <string name=\"dlg_button_ok\">Ok</string>\n"
                + "                                 ^\n"
                + "0 errors, 7 warnings\n";
        lint().files(typos2).run().expect(expected);
    }

    public void testEnLanguageBcp47() {
        // Check BCP-47 locale declaration for English
        String expected = ""
                + "res/values-b+en+USA/strings-en.xml:6: Warning: \"Andriod\" is a common misspelling; did you mean \"Android\" ? [Typos]\n"
                + "    <string name=\"s2\">Andriod activites!</string>\n"
                + "                      ^\n"
                + "res/values-b+en+USA/strings-en.xml:6: Warning: \"activites\" is a common misspelling; did you mean \"activities\" ? [Typos]\n"
                + "    <string name=\"s2\">Andriod activites!</string>\n"
                + "                              ^\n"
                + "res/values-b+en+USA/strings-en.xml:8: Warning: \"Cmoputer\" is a common misspelling; did you mean \"Computer\" ? [Typos]\n"
                + "    <string name=\"s3\"> (Cmoputer </string>\n"
                + "                        ^\n"
                + "res/values-b+en+USA/strings-en.xml:10: Warning: \"throught\" is a common misspelling; did you mean \"thought\" or \"through\" or \"throughout\" ? [Typos]\n"
                + "    <string name=\"s4\"><b>throught</b></string>\n"
                + "                         ^\n"
                + "res/values-b+en+USA/strings-en.xml:12: Warning: \"Seach\" is a common misspelling; did you mean \"Search\" ? [Typos]\n"
                + "    <string name=\"s5\">Seach</string>\n"
                + "                      ^\n"
                + "res/values-b+en+USA/strings-en.xml:16: Warning: \"Tuscon\" is a common misspelling; did you mean \"Tucson\" ? [Typos]\n"
                + "    <string name=\"s7\">Tuscon tuscon</string>\n"
                + "                      ^\n"
                + "res/values-b+en+USA/strings-en.xml:20: Warning: \"Ok\" is usually capitalized as \"OK\" [Typos]\n"
                + "    <string name=\"dlg_button_ok\">Ok</string>\n"
                + "                                 ^\n"
                + "0 errors, 7 warnings\n";
        lint().files(typos3).run().expect(expected);
    }

    public void testNorwegian() {
        // UTF-8 handling
        String expected = ""
                + "res/values-nb/typos.xml:6: Warning: \"Andriod\" is a common misspelling; did you mean \"Android\" ? [Typos]\n"
                + "    <string name=\"s2\">Mer morro med Andriod</string>\n"
                + "                                    ^\n"
                + "res/values-nb/typos.xml:6: Warning: \"morro\" is a common misspelling; did you mean \"moro\" ? [Typos]\n"
                + "    <string name=\"s2\">Mer morro med Andriod</string>\n"
                + "                          ^\n"
                + "res/values-nb/typos.xml:8: Warning: \"Parallel\" is a common misspelling; did you mean \"Parallell\" ? [Typos]\n"
                + "    <string name=\"s3\"> Parallel </string>\n"
                + "                       ^\n"
                + "res/values-nb/typos.xml:10: Warning: \"altid\" is a common misspelling; did you mean \"alltid\" ? [Typos]\n"
                + "    <string name=\"s4\"><b>altid</b></string>\n"
                + "                         ^\n"
                + "res/values-nb/typos.xml:12: Warning: \"Altid\" is a common misspelling; did you mean \"Alltid\" ? [Typos]\n"
                + "    <string name=\"s5\">Altid</string>\n"
                + "                      ^\n"
                + "res/values-nb/typos.xml:18: Warning: \"karriære\" is a common misspelling; did you mean \"karrière\" ? [Typos]\n"
                + "    <string name=\"s7\">Koding er en spennende karriære</string>\n"
                + "                                             ^\n"
                + "0 errors, 6 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/values-nb/typos.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <!-- No typos -->\n"
                        + "    <string name=\"s1\">Dette er en test</string>\n"
                        + "    <!-- Plain typos -->\n"
                        + "    <string name=\"s2\">Mer morro med Andriod</string>\n"
                        + "    <!-- Make capitalization match typo -->\n"
                        + "    <string name=\"s3\"> Parallel </string>\n"
                        + "    <!-- Markup indirection -->\n"
                        + "    <string name=\"s4\"><b>altid</b></string>\n"
                        + "    <!-- Typo, match capitalized -->\n"
                        + "    <string name=\"s5\">Altid</string>\n"
                        + "    <!-- random words, shouldn't flag -->\n"
                        + "    <string name=\"s6\">abcdefg qwerty asdf jklm</string>\n"
                        + "    <!-- typo, but should only match when capitalized -->\n"
                        + "    <string name=\"s7\">Midt-Østen midt-østen</string>\n"
                        + "    <!-- Non-ASCII UTF-8 string -->\n"
                        + "    <string name=\"s7\">Koding er en spennende karriære</string>\n"
                        + "    <string name=\"internet\">\"Koble til Internett.</string>\n"
                        + "\n"
                        + "</resources>\n"
                        + "\n"))
                .run()
                .expect(expected);
    }

    public void testGerman() {
        // Test globbing and multiple word matching
        String expected = ""
                + "res/values-de/typos.xml:6: Warning: \"befindet eine\" is a common misspelling; did you mean \"befindet sich eine\" ? [Typos]\n"
                + "           wo befindet eine ip\n"
                + "              ^\n"
                + "res/values-de/typos.xml:9: Warning: \"Authorisierungscode\" is a common misspelling; did you mean \"Autorisierungscode\" ? [Typos]\n"
                + "    <string name=\"s2\">(Authorisierungscode!)</string>\n"
                + "                       ^\n"
                + "res/values-de/typos.xml:10: Warning: \"zurück gefoobaren\" is a common misspelling; did you mean \"zurückgefoobaren\" ? [Typos]\n"
                + "    <string name=\"s3\">   zurück gefoobaren!</string>\n"
                + "                         ^\n"
                + "0 errors, 3 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/values-de/typos.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<resources>\n"
                            + "\n"
                            + "    <string name=\"s1\">\n"
                            + "\n"
                            + "           wo befindet eine ip\n"
                            + "\n"
                            + "           </string>\n"
                            + "    <string name=\"s2\">(Authorisierungscode!)</string>\n"
                            + "    <string name=\"s3\">   zurück gefoobaren!</string>\n"
                            + "    <!-- escaped separator -->\n"
                            + "    <string name=\"issue39599\">\"ü test\\nciht zu\"</string>\n"
                            + "\n"
                            + "</resources>\n"))
                .run()
                .expect(expected);
    }

    public void testOk() {
        lint().files(typos4).run().expectClean();
    }
    
    public void testMultipleReplacements() {
        lint().files(
                xml("res/values-en/typos.xml", ""
                        + "<resources>\n"
                        + "    <string name=\"s1\">throught</string>\n"
                        + "</resources>\n"))
                .run()
                .expect(""
                        + "res/values-en/typos.xml:2: Warning: \"throught\" is a common misspelling; did you mean \"thought\" or \"through\" or \"throughout\" ? [Typos]\n"
                        + "    <string name=\"s1\">throught</string>\n"
                        + "                      ^\n"
                        + "0 errors, 1 warnings\n")
                .expectFixDiffs(""
                        + "Fix for res/values-en/typos.xml line 1: Replace with \"thought\":\n"
                        + "@@ -2 +2\n"
                        + "-     <string name=\"s1\">throught</string>\n"
                        + "+     <string name=\"s1\">thought</string>\n"
                        + "Fix for res/values-en/typos.xml line 1: Replace with \"through\":\n"
                        + "@@ -2 +2\n"
                        + "-     <string name=\"s1\">throught</string>\n"
                        + "+     <string name=\"s1\">through</string>\n"
                        + "Fix for res/values-en/typos.xml line 1: Replace with \"throughout\":\n"
                        + "@@ -2 +2\n"
                        + "-     <string name=\"s1\">throught</string>\n"
                        + "+     <string name=\"s1\">throughout</string>\n");
    }

    public void testNorwegianDefault() {
        String expected = ""
                + "res/values/typos.xml:5: Warning: \"altid\" is a common misspelling; did you mean \"alltid\" ? [Typos]\n"
                + "    <string name=\"s4\"><b>altid</b></string>\n"
                + "                         ^\n"
                + "res/values/typos.xml:7: Warning: \"Altid\" is a common misspelling; did you mean \"Alltid\" ? [Typos]\n"
                + "    <string name=\"s5\">Altid</string>\n"
                + "                      ^\n"
                + "0 errors, 2 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/values/typos.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "    tools:locale=\"nb\">\n"
                        + "    <!-- Markup indirection -->\n"
                        + "    <string name=\"s4\"><b>altid</b></string>\n"
                        + "    <!-- Typo, match capitalized -->\n"
                        + "    <string name=\"s5\">Altid</string>\n"
                        + "</resources>\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for res/values/typos.xml line 4: Replace with \"alltid\":\n"
                        + "@@ -5 +5\n"
                        + "-     <string name=\"s4\"><b>altid</b></string>\n"
                        + "+     <string name=\"s4\"><b>alltid</b></string>\n"
                        + "Fix for res/values/typos.xml line 6: Replace with \"Alltid\":\n"
                        + "@@ -7 +7\n"
                        + "-     <string name=\"s5\">Altid</string>\n"
                        + "+     <string name=\"s5\">Alltid</string>\n");
    }

    public void testPluralsAndStringArrays() {
        // Regression test for https://code.google.com/p/android/issues/detail?id=82588
        //noinspection all // Sample code
        String expected = ""
                + "res/values/plurals_typography.xml:8: Warning: \"Andriod\" is a common misspelling; did you mean \"Android\" ? [Typos]\n"
                + "        <item quantity=\"other\">Andriod</item>\n"
                + "                               ^\n"
                + "res/values/plurals_typography.xml:13: Warning: \"Seach\" is a common misspelling; did you mean \"Search\" ? [Typos]\n"
                + "        <item>Seach</item>\n"
                + "              ^\n"
                + "0 errors, 2 warnings\n";
        lint().files(
                xml("res/values/plurals_typography.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <plurals name=\"ndash\">\n"
                        + "        <!-- Nonsensical plural, just adding strings for typography check in plurals -->\n"
                        + "        <item quantity=\"one\">For ages 3-5</item>\n"
                        + "        <item quantity=\"few\">1/4 times</item>\n"
                        + "        <!-- ..and a typo -->\n"
                        + "        <item quantity=\"other\">Andriod</item>\n"
                        + "    </plurals>\n"
                        + "\n"
                        + "    <!-- Nonsensical string array, just adding strings for typography check in arrays -->\n"
                        + "    <string-array name=\"security_questions\">\n"
                        + "        <item>Seach</item>\n"
                        + "        <item>``second''</item>\n"
                        + "    </string-array>\n"
                        + "</resources>\n"))
                .run()
                .expect(expected);
    }

    public void testTokens() {
        // Make sure we don't pick up tokens in the middle of a string
        //noinspection all // Sample code
        lint().files(
                xml("res/values/strings.xml", "" +
                        "<resources>\n" +
                        "    <string name=\"simple\">\n" +
                        "        1. Say &lt;b>\\\"&lt;xliff:g id=\"ok_google\">^1&lt;/xliff:g>,\n" +
                        "    </string>\n" +
                        "</resources>\n"))
                .run()
                .expectClean();
    }

    public void testValidRepeatedWords() {
        // Make sure we don't pick up tokens in the middle of a string
        //noinspection all // Sample code
        lint().files(
                xml("res/values/strings.xml", "" +
                        "<resources>\n" +
                        "    <string name=\"simple\">\n" +
                        "    I know that that will not work,\n" +
                        "    yadda yadda. Let's visit Pago Pago.\n" +
                        "    Time to say bye bye and ride the " +
                        "    choo choo train! It's a no no, tsk tsk." +
                        "    </string>\n" +
                        "</resources>\n"))
                .run()
                .expectClean();
    }

    @SuppressWarnings("all") // Sample code
    private TestFile typos1 = xml("res/values/strings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <!-- No typos -->\n"
            + "    <string name=\"s1\">Home Sample</string>\n"
            + "    <!-- Plain typos -->\n"
            + "    <string name=\"s2\">Andriod activites!</string>\n"
            + "    <!-- Make capitalization match typo -->\n"
            + "    <string name=\"s3\"> (Cmoputer </string>\n"
            + "    <!-- Markup indirection -->\n"
            + "    <string name=\"s4\"><b>throught</b></string>\n"
            + "    <!-- Typo, match capitalized -->\n"
            + "    <string name=\"s5\">Seach</string>\n"
            + "    <!-- random words, shouldn't flag -->\n"
            + "    <string name=\"s6\">abcdefg qwerty asdf jklm</string>\n"
            + "    <!-- typo, but should only match when capitalized -->\n"
            + "    <string name=\"s7\">Tuscon tuscon</string>\n"
            + "    <!-- case changes only: valid -->\n"
            + "    <string name=\"s8\">OK Cancel dialog with a long message</string>\n"
            + "    <!-- case changes only: invalid -->\n"
            + "    <string name=\"dlg_button_ok\">Ok</string>\n"
            + "    <!-- escaped separator -->\n"
            + "    <string name=\"issue39599\">\"Please take a moment\\nto rate ^1\"</string>\n"
            + "    <!-- escaped separator 2 -->\n"
            + "    <string name=\"issue39599_2\">\"\\nto</string>\n"
            + "    <!-- not translatable -->\n"
            + "    <string name=\"translatable\" translatable=\"false\">\"Andriod</string>\n"
            + "    <!-- don't spell check resource references; not user visible anyway -->\n"
            + "    <string name=\"issue77269_1\">@android:string/ok</string>\n"
            + "    <string name=\"issue77269_2\"> @string/cmoputer </string>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile typos2 = xml("res/values-en-rUS/strings-en.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <!-- No typos -->\n"
            + "    <string name=\"s1\">Home Sample</string>\n"
            + "    <!-- Plain typos -->\n"
            + "    <string name=\"s2\">Andriod activites!</string>\n"
            + "    <!-- Make capitalization match typo -->\n"
            + "    <string name=\"s3\"> (Cmoputer </string>\n"
            + "    <!-- Markup indirection -->\n"
            + "    <string name=\"s4\"><b>throught</b></string>\n"
            + "    <!-- Typo, match capitalized -->\n"
            + "    <string name=\"s5\">Seach</string>\n"
            + "    <!-- random words, shouldn't flag -->\n"
            + "    <string name=\"s6\">abcdefg qwerty asdf jklm</string>\n"
            + "    <!-- typo, but should only match when capitalized -->\n"
            + "    <string name=\"s7\">Tuscon tuscon</string>\n"
            + "    <!-- case changes only: valid -->\n"
            + "    <string name=\"s8\">OK Cancel dialog with a long message</string>\n"
            + "    <!-- case changes only: invalid -->\n"
            + "    <string name=\"dlg_button_ok\">Ok</string>\n"
            + "    <!-- escaped separator -->\n"
            + "    <string name=\"issue39599\">\"Please take a moment\\nto rate ^1\"</string>\n"
            + "    <!-- escaped separator 2 -->\n"
            + "    <string name=\"issue39599_2\">\"\\nto</string>\n"
            + "    <!-- not translatable -->\n"
            + "    <string name=\"translatable\" translatable=\"false\">\"Andriod</string>\n"
            + "    <!-- don't spell check resource references; not user visible anyway -->\n"
            + "    <string name=\"issue77269_1\">@android:string/ok</string>\n"
            + "    <string name=\"issue77269_2\"> @string/cmoputer </string>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile typos3 = xml("res/values-b+en+USA/strings-en.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <!-- No typos -->\n"
            + "    <string name=\"s1\">Home Sample</string>\n"
            + "    <!-- Plain typos -->\n"
            + "    <string name=\"s2\">Andriod activites!</string>\n"
            + "    <!-- Make capitalization match typo -->\n"
            + "    <string name=\"s3\"> (Cmoputer </string>\n"
            + "    <!-- Markup indirection -->\n"
            + "    <string name=\"s4\"><b>throught</b></string>\n"
            + "    <!-- Typo, match capitalized -->\n"
            + "    <string name=\"s5\">Seach</string>\n"
            + "    <!-- random words, shouldn't flag -->\n"
            + "    <string name=\"s6\">abcdefg qwerty asdf jklm</string>\n"
            + "    <!-- typo, but should only match when capitalized -->\n"
            + "    <string name=\"s7\">Tuscon tuscon</string>\n"
            + "    <!-- case changes only: valid -->\n"
            + "    <string name=\"s8\">OK Cancel dialog with a long message</string>\n"
            + "    <!-- case changes only: invalid -->\n"
            + "    <string name=\"dlg_button_ok\">Ok</string>\n"
            + "    <!-- escaped separator -->\n"
            + "    <string name=\"issue39599\">\"Please take a moment\\nto rate ^1\"</string>\n"
            + "    <!-- escaped separator 2 -->\n"
            + "    <string name=\"issue39599_2\">\"\\nto</string>\n"
            + "    <!-- not translatable -->\n"
            + "    <string name=\"translatable\" translatable=\"false\">\"Andriod</string>\n"
            + "    <!-- don't spell check resource references; not user visible anyway -->\n"
            + "    <string name=\"issue77269_1\">@android:string/ok</string>\n"
            + "    <string name=\"issue77269_2\"> @string/cmoputer </string>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile typos4 = xml("res/values-xy/strings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <!-- No typos -->\n"
            + "    <string name=\"s1\">Home Sample</string>\n"
            + "    <!-- Plain typos -->\n"
            + "    <string name=\"s2\">Andriod activites!</string>\n"
            + "    <!-- Make capitalization match typo -->\n"
            + "    <string name=\"s3\"> (Cmoputer </string>\n"
            + "    <!-- Markup indirection -->\n"
            + "    <string name=\"s4\"><b>throught</b></string>\n"
            + "    <!-- Typo, match capitalized -->\n"
            + "    <string name=\"s5\">Seach</string>\n"
            + "    <!-- random words, shouldn't flag -->\n"
            + "    <string name=\"s6\">abcdefg qwerty asdf jklm</string>\n"
            + "    <!-- typo, but should only match when capitalized -->\n"
            + "    <string name=\"s7\">Tuscon tuscon</string>\n"
            + "    <!-- case changes only: valid -->\n"
            + "    <string name=\"s8\">OK Cancel dialog with a long message</string>\n"
            + "    <!-- case changes only: invalid -->\n"
            + "    <string name=\"dlg_button_ok\">Ok</string>\n"
            + "    <!-- escaped separator -->\n"
            + "    <string name=\"issue39599\">\"Please take a moment\\nto rate ^1\"</string>\n"
            + "    <!-- escaped separator 2 -->\n"
            + "    <string name=\"issue39599_2\">\"\\nto</string>\n"
            + "    <!-- not translatable -->\n"
            + "    <string name=\"translatable\" translatable=\"false\">\"Andriod</string>\n"
            + "    <!-- don't spell check resource references; not user visible anyway -->\n"
            + "    <string name=\"issue77269_1\">@android:string/ok</string>\n"
            + "    <string name=\"issue77269_2\"> @string/cmoputer </string>\n"
            + "</resources>\n");
}
