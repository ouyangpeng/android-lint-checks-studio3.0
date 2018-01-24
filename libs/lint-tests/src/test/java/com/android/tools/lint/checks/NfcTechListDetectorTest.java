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

package com.android.tools.lint.checks;

import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings("javadoc")
public class NfcTechListDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new NfcTechListDetector();
    }

    public void test() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/xml/nfc_tech_list_formatted.xml:6: Error: There should not be any whitespace inside <tech> elements [NfcTechWhitespace]\n"
                + "android.nfc.tech.NfcA\n"
                + "^\n"
                + "res/xml/nfc_tech_list_formatted.xml:12: Error: There should not be any whitespace inside <tech> elements [NfcTechWhitespace]\n"
                + "android.nfc.tech.MifareUltralight\n"
                + "^\n"
                + "res/xml/nfc_tech_list_formatted.xml:18: Error: There should not be any whitespace inside <tech> elements [NfcTechWhitespace]\n"
                + "android.nfc.tech.ndefformatable\n"
                + "^\n"
                + "3 errors, 0 warnings\n",

            lintProject(
                    xml("res/xml/nfc_tech_list_formatted.xml", ""
                            + "<resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\" >\n"
                            + "\n"
                            + "    <!-- capture anything using NfcF -->\n"
                            + "    <tech-list>\n"
                            + "        <tech>\n"
                            + "android.nfc.tech.NfcA\n"
                            + "        </tech>\n"
                            + "    </tech-list>\n"
                            + "    <!-- OR -->\n"
                            + "    <tech-list>\n"
                            + "        <tech>\n"
                            + "android.nfc.tech.MifareUltralight\n"
                            + "        </tech>\n"
                            + "    </tech-list>\n"
                            + "    <!-- OR -->\n"
                            + "    <tech-list>\n"
                            + "        <tech>\n"
                            + "android.nfc.tech.ndefformatable\n"
                            + "        </tech>\n"
                            + "    </tech-list>\n"
                            + "\n"
                            + "</resources>\n")));
    }

    public void testOk() throws Exception {
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintProject(
                    xml("res/xml/nfc_tech_list.xml", ""
                            + " <resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
                            + "     <!-- capture anything using NfcF -->\n"
                            + "     <tech-list>\n"
                            + "         <tech>android.nfc.tech.NfcA</tech>\n"
                            + "     </tech-list>\n"
                            + "     <!-- OR -->\n"
                            + "     <tech-list>\n"
                            + "          <tech>android.nfc.tech.MifareUltralight</tech>\n"
                            + "     </tech-list>\n"
                            + "     <!-- OR -->\n"
                            + "      <tech-list>\n"
                            + "          <tech>android.nfc.tech.ndefformatable</tech>\n"
                            + "     </tech-list>\n"
                            + " </resources>\n")));
    }
}
