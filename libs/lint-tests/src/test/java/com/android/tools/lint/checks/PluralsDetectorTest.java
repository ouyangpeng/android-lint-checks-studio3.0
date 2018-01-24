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

package com.android.tools.lint.checks;

import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("javadoc")
public class PluralsDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new PluralsDetector();
    }

    public void test1() throws Exception {
        mEnabled = Sets.newHashSet(PluralsDetector.MISSING, PluralsDetector.EXTRA);
        assertEquals(""
                + "res/values-pl/plurals2.xml:3: Error: For locale \"pl\" (Polish) the following quantities should also be defined: many [MissingQuantity]\n"
                + "    <plurals name=\"numberOfSongsAvailable\">\n"
                + "    ^\n"
                + "1 errors, 0 warnings\n",

            lintProject(
                 mPlurals,
                 mPlurals2,
                 mPlurals2_class));
    }

    public void test2() throws Exception {
        mEnabled = Sets.newHashSet(PluralsDetector.MISSING, PluralsDetector.EXTRA);
        //noinspection all // Sample code
        assertEquals(""
                + "res/values-cs/plurals3.xml:3: Error: For locale \"cs\" (Czech) the following quantities should also be defined: few [MissingQuantity]\n"
                + "  <plurals name=\"draft\">\n"
                + "  ^\n"
                + "res/values-zh-rCN/plurals3.xml:3: Warning: For language \"zh\" (Chinese) the following quantities are not relevant: one [UnusedQuantity]\n"
                + "  <plurals name=\"draft\">\n"
                + "  ^\n"
                + "res/values-zh-rCN/plurals3.xml:7: Warning: For language \"zh\" (Chinese) the following quantities are not relevant: one [UnusedQuantity]\n"
                + "  <plurals name=\"title_day_dialog_content\">\n"
                + "  ^\n"
                + "1 errors, 2 warnings\n",

                lintProject(
                        xml("res/values-zh-rCN/plurals3.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                            + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                            + "  <plurals name=\"draft\">\n"
                            + "    <item quantity=\"one\">\"\u8349\u7a3f\"</item>\n"
                            + "    <item quantity=\"other\">\"\u8349\u7a3f\"</item>\n"
                            + "  </plurals>\n"
                            + "  <plurals name=\"title_day_dialog_content\">\n"
                            + "    <item quantity=\"one\">\"\u5929\"</item>\n"
                            + "    <item quantity=\"other\">\"\u5929\"</item>\n"
                            + "  </plurals>\n"
                            + "</resources>\n"),
                        xml("res/values-cs/plurals3.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                            + "<resources>\n"
                            + "  <plurals name=\"draft\">\n"
                            + "    <item quantity=\"one\">\"Koncept\"</item>\n"
                            + "    <item quantity=\"other\">\"Koncepty\"</item>\n"
                            + "  </plurals>\n"
                            + "</resources>\n")));
    }

    public void testEmptyPlural() throws Exception {
        mEnabled = Sets.newHashSet(PluralsDetector.MISSING, PluralsDetector.EXTRA);
        //noinspection all // Sample code
        assertEquals(""
                + "res/values/plurals4.xml:3: Error: There should be at least one quantity string in this <plural> definition [MissingQuantity]\n"
                + "   <plurals name=\"minutes_until_num\">\n"
                + "   ^\n"
                + "1 errors, 0 warnings\n",

                lintProject(
                        xml("res/values/plurals4.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<resources>\n"
                            + "   <plurals name=\"minutes_until_num\">\n"
                            + "   </plurals>\n"
                            + "</resources>\n")));
    }

    public void testPolish() throws Exception {
        // Test for https://code.google.com/p/android/issues/detail?id=67803
        mEnabled = Sets.newHashSet(PluralsDetector.MISSING, PluralsDetector.EXTRA);
        //noinspection all // Sample code
        assertEquals(""
                + "res/values-pl/plurals5.xml:3: Error: For locale \"pl\" (Polish) the following quantities should also be defined: many [MissingQuantity]\n"
                + "    <plurals name=\"my_plural\">\n"
                + "    ^\n"
                + "res/values-pl/plurals5.xml:3: Warning: For language \"pl\" (Polish) the following quantities are not relevant: zero [UnusedQuantity]\n"
                + "    <plurals name=\"my_plural\">\n"
                + "    ^\n"
                + "1 errors, 1 warnings\n",

                lintProject(
                        xml("res/values-pl/plurals5.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<resources>\n"
                            + "    <plurals name=\"my_plural\">\n"
                            + "        <item quantity=\"zero\">@string/hello</item>\n"
                            + "        <item quantity=\"one\">@string/hello</item>\n"
                            + "        <item quantity=\"few\">@string/hello</item>\n"
                            + "        <item quantity=\"other\">@string/hello</item>\n"
                            + "    </plurals>\n"
                            + "</resources>\n")));
    }

    public void testRussian() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=75799
        mEnabled = Sets.newHashSet(PluralsDetector.MISSING, PluralsDetector.EXTRA);
        //noinspection all // Sample code
        assertEquals("No warnings.",

                lintProject(
                        xml("res/values-ru/plurals6.xml", ""
                            + "<plurals name=\"in_num_minutes\">\n"
                            + "    <item quantity=\"one\">\u0447\u0435\u0440\u0435\u0437 %d \u043c\u0438\u043d\u0443\u0442\u0443</item>\n"
                            + "    <item quantity=\"few\">\u0447\u0435\u0440\u0435\u0437 %d \u043c\u0438\u043d\u0443\u0442\u044b</item>\n"
                            + "    <item quantity=\"many\">\u0447\u0435\u0440\u0435\u0437 %d \u043c\u0438\u043d\u0443\u0442</item>\n"
                            + "</plurals>\n")));
    }

    public void testImpliedQuantity() throws Exception {
        mEnabled = Collections.singleton(PluralsDetector.IMPLIED_QUANTITY);
        assertEquals(""
                + "res/values-sl/plurals2.xml:4: Error: The quantity 'one' matches more than one specific number in this locale (1, 101, 201, 301, 401, 501, 601, 701, 1001, \u2026), but the message did not include a formatting argument (such as %d). This is usually an internationalization error. See full issue explanation for more. [ImpliedQuantity]\n"
                + "        <item quantity=\"one\">Znaleziono jedn\u0105 piosenk\u0119.</item>\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

                lintProject(
                        mPlurals,
                        mPlurals2,
                        mPlurals2_class,
                        // Simulate locale message for locale which has multiple values for one
                        mPlurals2_class2));
    }

    public void testExpandTemplates() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=228989
        // "ImpliedQuantity lint check should allow ^1 as format argument"
        lint().files(
                xml("res/values-uk/strings.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
                        + "    <plurals name=\"foobar\">\n"
                        + "        <item quantity=\"one\">foobar one <xliff:g id=\"foobar\" example=\"1\">^1</xliff:g></item>\n"
                        + "        <item quantity=\"few\">foobar few <xliff:g id=\"foobar\" example=\"1\">^1</xliff:g></item>\n"
                        + "        <item quantity=\"many\">foobar many <xliff:g id=\"foobar\" example=\"1\">^1</xliff:g></item>\n"
                        + "        <item quantity=\"other\">foobar other <xliff:g id=\"foobar\" example=\"23\">^1</xliff:g></item>\n"
                        + "    </plurals>\n"
                        + "</resources>"))
                .run()
                .expectClean();
    }

    public void testUnused() throws Exception {
        lint().files(
                xml("res/values-en/strings.xml", ""
                        + "<resources tools:locale=\"en\" xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <string name=\"app_name\">My Application</string>\n"
                        + "    <plurals name=\"plural_test\">\n"
                        + "        <item quantity=\"zero\">zero %1$d</item>\n"
                        + "        <item quantity=\"one\">one %1$d</item>\n"
                        + "        <item quantity=\"two\">two %1$d</item>\n"
                        + "        <item quantity=\"few\">few %1$d</item>\n"
                        + "        <item quantity=\"many\">many %1$d</item>\n"
                        + "        <item quantity=\"other\">other %1$d</item>\n"
                        + "    </plurals>\n"
                        + "\n"
                        + "</resources>\n"))
                .run()
                .expect(""
                        + "res/values-en/strings.xml:3: Warning: For language \"en\" (English) the following quantities are not relevant: few, many, two, zero [UnusedQuantity]\n"
                        + "    <plurals name=\"plural_test\">\n"
                        + "    ^\n"
                        + "0 errors, 1 warnings\n");
    }

    public void testUnused2() throws Exception {
        lint().files(
                xml("res/values-en/strings.xml", ""
                        + "<resources>\n"
                        + "    <string name=\"app_name\">My Application</string>\n"
                        + "    <plurals name=\"plural_test\">\n"
                        + "        <item quantity=\"zero\">zero %1$d</item>\n"
                        + "        <item quantity=\"one\">one %1$d</item>\n"
                        + "        <item quantity=\"two\">two %1$d</item>\n"
                        + "        <item quantity=\"few\">few %1$d</item>\n"
                        + "        <item quantity=\"many\">many %1$d</item>\n"
                        + "        <item quantity=\"other\">other %1$d</item>\n"
                        + "    </plurals>\n"
                        + "\n"
                        + "</resources>\n"))
                .run()
                .expect(""
                        + "res/values-en/strings.xml:3: Warning: For language \"en\" (English) the following quantities are not relevant: few, many, two, zero [UnusedQuantity]\n"
                        + "    <plurals name=\"plural_test\">\n"
                        + "    ^\n"
                        + "0 errors, 1 warnings\n");
    }

    private Set<Issue> mEnabled = new HashSet<>();

    @Override
    protected boolean isEnabled(Issue issue) {
        return super.isEnabled(issue) && mEnabled.contains(issue);
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mPlurals = xml("res/values/plurals.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <plurals name=\"my_plural\">\n"
            + "        <item quantity=\"one\">@string/hello</item>\n"
            + "        <item quantity=\"few\">@string/hello</item>\n"
            + "        <item quantity=\"other\">@string/hello</item>\n"
            + "    </plurals>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mPlurals2 = xml("res/values/plurals2.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <plurals name=\"numberOfSongsAvailable\">\n"
            + "        <item quantity=\"one\">One song found.</item>\n"
            + "        <item quantity=\"other\">%d songs found.</item>\n"
            + "    </plurals>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mPlurals2_class = xml("res/values-pl/plurals2.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <plurals name=\"numberOfSongsAvailable\">\n"
            + "        <item quantity=\"one\">Znaleziono jedn\u0105 piosenk\u0119.</item>\n"
            + "        <item quantity=\"few\">Znaleziono %d piosenki.</item>\n"
            + "        <item quantity=\"other\">Znaleziono %d piosenek.</item>\n"
            + "    </plurals>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mPlurals2_class2 = xml("res/values-sl/plurals2.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <plurals name=\"numberOfSongsAvailable\">\n"
            + "        <item quantity=\"one\">Znaleziono jedn\u0105 piosenk\u0119.</item>\n"
            + "        <item quantity=\"few\">Znaleziono %d piosenki.</item>\n"
            + "        <item quantity=\"other\">Znaleziono %d piosenek.</item>\n"
            + "    </plurals>\n"
            + "</resources>\n");
}
