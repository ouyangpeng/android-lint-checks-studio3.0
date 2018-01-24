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
public class LabelForDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new LabelForDetector();
    }

    public void test() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/labelfor.xml:54: Warning: No label views point to this text field with an android:labelFor=\"@+id/editText2\" attribute [LabelFor]\n"
                + "    <EditText\n"
                + "    ^\n"
                + "res/layout/labelfor.xml:61: Warning: No label views point to this text field with an android:labelFor=\"@+id/autoCompleteTextView2\" attribute [LabelFor]\n"
                + "    <AutoCompleteTextView\n"
                + "    ^\n"
                + "res/layout/labelfor.xml:68: Warning: No label views point to this text field with an android:labelFor=\"@+id/multiAutoCompleteTextView2\" attribute [LabelFor]\n"
                + "    <MultiAutoCompleteTextView\n"
                + "    ^\n"
                + "0 errors, 3 warnings\n",

        lintProject(
                manifest().minSdk(17),
                mLabelfor
        ));
    }

    public void testSuppressed() throws Exception {
        //noinspection all // Sample code
        assertEquals(
        "No warnings.",

        lintProject(
            manifest().minSdk(17),
            xml("res/layout/labelfor_ignore.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\"\n"
                            + "    android:orientation=\"vertical\" >\n"
                            + "\n"
                            + "    <EditText\n"
                            + "        android:id=\"@+id/editText2\"\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:ems=\"10\"\n"
                            + "        android:inputType=\"textPostalAddress\"\n"
                            + "        tools:ignore=\"LabelFor\"/>\n"
                            + "\n"
                            + "</LinearLayout>\n")
        ));
    }


    public void testOk() throws Exception {
        //noinspection all // Sample code
        assertEquals(
        "No warnings.",

        lintProject(
                manifest().minSdk(17),
                xml("res/layout/accessibility.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" android:id=\"@+id/newlinear\" android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">\n"
                            + "    <Button android:text=\"Button\" android:id=\"@+id/button1\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"></Button>\n"
                            + "    <ImageView android:id=\"@+id/android_logo\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                            + "    <ImageButton android:importantForAccessibility=\"yes\" android:id=\"@+id/android_logo2\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                            + "    <Button android:text=\"Button\" android:id=\"@+id/button2\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"></Button>\n"
                            + "    <Button android:id=\"@+android:id/summary\" android:contentDescription=\"@string/label\" />\n"
                            + "    <ImageButton android:importantForAccessibility=\"no\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                            + "</LinearLayout>\n")
            ));
    }

    public void testNotApplicable() throws Exception {
        //noinspection all // Sample code
        assertEquals(
        "No warnings.",

        lintProject(
                manifest().minSdk(14),
                mLabelfor
        ));
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mLabelfor = xml("res/layout/labelfor.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:layout_width=\"match_parent\"\n"
            + "    android:layout_height=\"match_parent\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "    <TextView\n"
            + "        android:id=\"@+id/textView1\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:labelFor=\"@+id/editText1\"\n"
            + "        android:text=\"Medium Text\"\n"
            + "        android:textAppearance=\"?android:attr/textAppearanceMedium\" />\n"
            + "\n"
            + "    <EditText\n"
            + "        android:id=\"@+id/editText1\"\n"
            + "        android:layout_width=\"match_parent\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:ems=\"10\"\n"
            + "        android:inputType=\"textPersonName\" >\n"
            + "\n"
            + "        <requestFocus />\n"
            + "    </EditText>\n"
            + "\n"
            + "    <TextView\n"
            + "        android:id=\"@+id/textView2\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:labelFor=\"@+id/autoCompleteTextView1\"\n"
            + "        android:text=\"TextView\" />\n"
            + "\n"
            + "    <AutoCompleteTextView\n"
            + "        android:id=\"@+id/autoCompleteTextView1\"\n"
            + "        android:layout_width=\"match_parent\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:ems=\"10\"\n"
            + "        android:text=\"AutoCompleteTextView\" />\n"
            + "\n"
            + "    <TextView\n"
            + "        android:id=\"@+id/textView3\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:labelFor=\"@+id/multiAutoCompleteTextView1\"\n"
            + "        android:text=\"Large Text\"\n"
            + "        android:textAppearance=\"?android:attr/textAppearanceLarge\" />\n"
            + "\n"
            + "    <MultiAutoCompleteTextView\n"
            + "        android:id=\"@+id/multiAutoCompleteTextView1\"\n"
            + "        android:layout_width=\"match_parent\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:ems=\"10\"\n"
            + "        android:text=\"MultiAutoCompleteTextView\" />\n"
            + "\n"
            + "    <EditText\n"
            + "        android:id=\"@+id/editText2\"\n"
            + "        android:layout_width=\"match_parent\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:ems=\"10\"\n"
            + "        android:inputType=\"textPostalAddress\" />\n"
            + "\n"
            + "    <AutoCompleteTextView\n"
            + "        android:id=\"@+id/autoCompleteTextView2\"\n"
            + "        android:layout_width=\"match_parent\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:ems=\"10\"\n"
            + "        android:text=\"AutoCompleteTextView\" />\n"
            + "\n"
            + "    <MultiAutoCompleteTextView\n"
            + "        android:id=\"@+id/multiAutoCompleteTextView2\"\n"
            + "        android:layout_width=\"match_parent\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:ems=\"10\"\n"
            + "        android:text=\"MultiAutoCompleteTextView\" />\n"
            + "\n"
            + "    <EditText\n"
            + "        android:id=\"@+id/editText20\"\n"
            + "        android:hint=\"Enter your address\"\n"
            + "        android:layout_width=\"match_parent\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:ems=\"10\"\n"
            + "        android:inputType=\"textPostalAddress\" />\n"
            + "\n"
            + "\n"
            + "</LinearLayout>\n");
}

