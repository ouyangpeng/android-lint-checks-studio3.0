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

public class NegativeMarginDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new NegativeMarginDetector();
    }

    public void testLayoutWithoutRepositorySupport() throws Exception {
        assertEquals(""
                + "res/layout/negative_margins.xml:11: Warning: Margin values should not be negative [NegativeMargin]\n"
                + "    <TextView android:layout_marginTop=\"-1dp\"/> <!-- WARNING -->\n"
                + "              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",
                lintFiles(mNegative_margins));
    }

    public void testIncrementalInLayout() throws Exception {
        assertEquals(""
                + "res/layout/negative_margins.xml:11: Warning: Margin values should not be negative [NegativeMargin]\n"
                + "    <TextView android:layout_marginTop=\"-1dp\"/> <!-- WARNING -->\n"
                + "              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/negative_margins.xml:13: Warning: Margin values should not be negative (@dimen/negative is defined as -16dp in values/negative_margins.xml [NegativeMargin]\n"
                + "    <TextView android:layout_marginTop=\"@dimen/negative\"/> <!-- WARNING -->\n"
                + "              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",
                lintProjectIncrementally(
                        "res/layout/negative_margins.xml",
                        mNegative_margins2, mNegative_margins));
    }

    public void testValuesWithoutRepositorySupport() throws Exception {
        assertEquals(""
                + "res/values/negative_margins.xml:11: Warning: Margin values should not be negative [NegativeMargin]\n"
                + "        <item name=\"android:layout_marginBottom\">-5dp</item> <!-- WARNING -->\n"
                + "                                                 ^\n"
                + "0 errors, 1 warnings\n",
                lintFiles(mNegative_margins2));
    }

    public void testIncrementalInValues() throws Exception {
        assertEquals(""
                + "res/values/negative_margins.xml:10: Warning: Margin values should not be negative (@dimen/negative is defined as -16dp in values/negative_margins.xml [NegativeMargin]\n"
                + "        <item name=\"android:layout_marginTop\">@dimen/negative</item> <!-- WARNING -->\n"
                + "                                              ^\n"
                + "res/values/negative_margins.xml:11: Warning: Margin values should not be negative [NegativeMargin]\n"
                + "        <item name=\"android:layout_marginBottom\">-5dp</item> <!-- WARNING -->\n"
                + "                                                 ^\n"
                + "0 errors, 2 warnings\n",
                lintProjectIncrementally(
                        "res/values/negative_margins.xml",
                        mNegative_margins2, mNegative_margins));
    }

    public void testBatch() throws Exception {
        assertEquals(""
                + "res/layout/negative_margins.xml:11: Warning: Margin values should not be negative [NegativeMargin]\n"
                + "    <TextView android:layout_marginTop=\"-1dp\"/> <!-- WARNING -->\n"
                + "              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/values/negative_margins.xml:11: Warning: Margin values should not be negative [NegativeMargin]\n"
                + "        <item name=\"android:layout_marginBottom\">-5dp</item> <!-- WARNING -->\n"
                + "                                                 ^\n"
                + "res/layout/negative_margins.xml:13: Warning: Margin values should not be negative [NegativeMargin]\n"
                + "    <TextView android:layout_marginTop=\"@dimen/negative\"/> <!-- WARNING -->\n"
                + "              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 3 warnings\n",

            lintFiles(mNegative_margins2, mNegative_margins));
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mNegative_margins = xml("res/layout/negative_margins.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<GridLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "            xmlns:tools=\"http://schemas.android.com/tools\"\n"
            + "            android:layout_width=\"match_parent\"\n"
            + "            android:layout_height=\"match_parent\"\n"
            + "            android:orientation=\"vertical\">\n"
            + "\n"
            + "    <TextView android:layout_margin=\"1dp\"/> <!-- OK -->\n"
            + "    <TextView android:layout_marginLeft=\"1dp\"/> <!-- OK -->\n"
            + "    <TextView android:layout_marginLeft=\"0dp\"/> <!-- OK -->\n"
            + "    <TextView android:layout_marginTop=\"-1dp\"/> <!-- WARNING -->\n"
            + "    <TextView android:layout_marginTop=\"@dimen/positive\"/> <!-- OK -->\n"
            + "    <TextView android:layout_marginTop=\"@dimen/negative\"/> <!-- WARNING -->\n"
            + "    <TextView android:paddingLeft=\"-1dp\"/> <!-- OK -->\n"
            + "    <TextView android:layout_marginTop=\"-1dp\" tools:ignore=\"NegativeMargin\"/> <!-- SUPPRESSED -->\n"
            + "\n"
            + "</GridLayout>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mNegative_margins2 = xml("res/values/negative_margins.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <dimen name=\"activity_horizontal_margin\">16dp</dimen>\n"
            + "    <dimen name=\"positive\">16dp</dimen>\n"
            + "    <dimen name=\"negative\">-16dp</dimen>\n"
            + "\n"
            + "    <style name=\"MyStyle\">\n"
            + "        <item name=\"android:layout_margin\">5dp</item> <!-- OK -->\n"
            + "        <item name=\"android:layout_marginLeft\">@dimen/positive</item> <!-- OK -->\n"
            + "        <item name=\"android:layout_marginTop\">@dimen/negative</item> <!-- WARNING -->\n"
            + "        <item name=\"android:layout_marginBottom\">-5dp</item> <!-- WARNING -->\n"
            + "    </style>\n"
            + "</resources>\n");
}
