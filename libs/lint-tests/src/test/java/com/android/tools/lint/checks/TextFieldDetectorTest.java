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
public class TextFieldDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new TextFieldDetector();
    }

    public void testField() throws Exception {
        String expected = ""
                + "res/layout/note_edit.xml:50: Warning: This text field does not specify an inputType or a hint [TextFields]\n"
                + "        <EditText\n"
                + "        ^\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/note_edit.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:orientation=\"vertical\"\n"
                        + "    android:layout_width=\"fill_parent\"\n"
                        + "    android:layout_height=\"fill_parent\">\n"
                        + "    <include layout=\"@layout/colorstrip\" android:layout_height=\"@dimen/colorstrip_height\" android:layout_width=\"match_parent\"/>\n"
                        + "\n"
                        + "    <LinearLayout style=\"@style/TitleBar\" android:id=\"@+id/header\">\n"
                        + "        <ImageView style=\"@style/TitleBarLogo\"\n"
                        + "            android:contentDescription=\"@string/description_logo\"\n"
                        + "            android:src=\"@drawable/title_logo\" />\n"
                        + "\n"
                        + "        <View style=\"@style/TitleBarSpring\" />\n"
                        + "\n"
                        + "        <ImageView style=\"@style/TitleBarSeparator\" />\n"
                        + "        <ImageButton style=\"@style/TitleBarAction\"\n"
                        + "            android:id=\"@+id/btn_title_refresh\"\n"
                        + "            android:contentDescription=\"@string/description_refresh\"\n"
                        + "            android:src=\"@drawable/ic_title_refresh\"\n"
                        + "            android:layout_width=\"wrap_content\"\n"
                        + "            android:layout_height=\"42dp\"\n"
                        + "            android:onClick=\"onRefreshClick\" />\n"
                        + "        <ProgressBar style=\"@style/TitleBarProgressIndicator\"\n"
                        + "            android:id=\"@+id/title_refresh_progress\"\n"
                        + "            android:layout_width=\"wrap_content\"\n"
                        + "            android:visibility=\"visible\"/>\n"
                        + "\n"
                        + "        <ImageView style=\"@style/TitleBarSeparator\" />\n"
                        + "        <ImageButton style=\"@style/TitleBarAction\"\n"
                        + "            android:contentDescription=\"@string/description_search\"\n"
                        + "            android:src=\"@drawable/ic_title_search\"\n"
                        + "            android:layout_width=\"wrap_content\"\n"
                        + "            android:layout_height=\"42dp\"\n"
                        + "            android:onClick=\"onSearchClick\" />\n"
                        + "    </LinearLayout>\n"
                        + "\n"
                        + "    <LinearLayout\n"
                        + "        android:id=\"@+id/noteArea\"\n"
                        + "        android:layout_width=\"fill_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:layout_weight=\"1\"\n"
                        + "        android:layout_margin=\"5dip\">\n"
                        + "        <EditText\n"
                        + "            android:id=\"@android:id/text1\"\n"
                        + "            android:layout_height=\"fill_parent\"\n"
                        + "            android:hint=\"@string/note_hint\"\n"
                        + "            android:freezesText=\"true\"\n"
                        + "            android:gravity=\"top\" android:layout_width=\"wrap_content\" android:layout_weight=\"1\">\n"
                        + "        </EditText>\n"
                        + "        <EditText\n"
                        + "            android:id=\"@android:id/text2\"\n"
                        + "            android:layout_height=\"fill_parent\"\n"
                        + "            android:freezesText=\"true\"\n"
                        + "            android:gravity=\"top\" android:layout_width=\"wrap_content\" android:layout_weight=\"1\">\n"
                        + "            <requestFocus />\n"
                        + "        </EditText>\n"
                        + "    </LinearLayout>\n"
                        + "\n"
                        + "    <LinearLayout\n"
                        + "        android:orientation=\"horizontal\"\n"
                        + "        android:layout_width=\"fill_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        style=\"@android:style/ButtonBar\">\n"
                        + "        <Button\n"
                        + "            android:layout_width=\"0dip\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:layout_weight=\"1\"\n"
                        + "            android:onClick=\"onSaveClick\"\n"
                        + "            android:text=\"@string/note_save\" />\n"
                        + "        <Button\n"
                        + "            android:layout_width=\"0dip\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:layout_weight=\"1\"\n"
                        + "            android:onClick=\"onDiscardClick\"\n"
                        + "            android:text=\"@string/note_discard\" />\n"
                        + "    </LinearLayout>\n"
                        + "\n"
                        + "</LinearLayout>\n"))
                .run()
                .expect(expected)
                .verifyFixes().window(2).expectFixDiffs(""
                + "Fix for res/layout/note_edit.xml line 49: Set inputType:\n"
                + "@@ -74 +74\n"
                + "              android:layout_weight=\"1\"\n"
                + "              android:freezesText=\"true\"\n"
                + "-             android:gravity=\"top\" >\n"
                + "+             android:gravity=\"top\"\n"
                + "+             android:inputType=\"|\" >\n"
                + "  \n"
                + "              <requestFocus />\n");
    }

    public void testTypeFromName() throws Exception {
        String expected = ""
                + "res/layout/edit_type.xml:14: Warning: The view name (@+id/mypassword) suggests this is a password, but it does not include 'textPassword' in the inputType [TextFields]\n"
                + "        android:inputType=\"text\" >\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/layout/edit_type.xml:10: id defined here\n"
                + "res/layout/edit_type.xml:45: Warning: The view name (@+id/password_length) suggests this is a number, but it does not include a numeric inputType (such as 'numberSigned') [TextFields]\n"
                + "        android:inputType=\"text\" />\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/layout/edit_type.xml:41: id defined here\n"
                + "res/layout/edit_type.xml:54: Warning: The view name (@+id/welcome_url) suggests this is a URI, but it does not include 'textUri' in the inputType [TextFields]\n"
                + "        android:inputType=\"text\" />\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/layout/edit_type.xml:50: id defined here\n"
                + "res/layout/edit_type.xml:63: Warning: The view name (@+id/start_date) suggests this is a date, but it does not include 'date' or 'datetime' in the inputType [TextFields]\n"
                + "        android:inputType=\"text\" />\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/layout/edit_type.xml:59: id defined here\n"
                + "res/layout/edit_type.xml:72: Warning: The view name (@+id/email_address) suggests this is an e-mail address, but it does not include 'textEmail' in the inputType [TextFields]\n"
                + "        android:inputType=\"text\" />\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/layout/edit_type.xml:68: id defined here\n"
                + "res/layout/edit_type.xml:81: Warning: The view name (@+id/login_pin) suggests this is a password, but it does not include 'numberPassword' in the inputType [TextFields]\n"
                + "        android:inputType=\"textPassword\" />\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/layout/edit_type.xml:77: id defined here\n"
                + "res/layout/edit_type.xml:83: Warning: This text field does not specify an inputType or a hint [TextFields]\n"
                + "    <EditText\n"
                + "    ^\n"
                + "0 errors, 7 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/edit_type.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "    <!-- Wrong: doesn't specify textPassword -->\n"
                        + "\n"
                        + "    <EditText\n"
                        + "        android:id=\"@+id/mypassword\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:ems=\"10\"\n"
                        + "        android:inputType=\"text\" >\n"
                        + "\n"
                        + "        <requestFocus />\n"
                        + "    </EditText>\n"
                        + "\n"
                        + "    <!-- OK, specifies textPassword: -->\n"
                        + "\n"
                        + "    <EditText\n"
                        + "        android:id=\"@+id/password1\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:ems=\"10\"\n"
                        + "        android:inputType=\"text|numberPassword\" />\n"
                        + "\n"
                        + "    <!-- OK, specifies password: -->\n"
                        + "\n"
                        + "    <EditText\n"
                        + "        android:id=\"@+id/password2\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:ems=\"10\"\n"
                        + "        android:inputType=\"text\"\n"
                        + "        android:password=\"true\" />\n"
                        + "\n"
                        + "    <!-- Wrong, doesn't include number -->\n"
                        + "\n"
                        + "    <EditText\n"
                        + "        android:id=\"@+id/password_length\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:ems=\"10\"\n"
                        + "        android:inputType=\"text\" />\n"
                        + "\n"
                        + "    <!-- Wrong, doesn't include URL -->\n"
                        + "\n"
                        + "    <EditText\n"
                        + "        android:id=\"@+id/welcome_url\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:ems=\"10\"\n"
                        + "        android:inputType=\"text\" />\n"
                        + "\n"
                        + "    <!-- Wrong, doesn't include date -->\n"
                        + "\n"
                        + "    <EditText\n"
                        + "        android:id=\"@+id/start_date\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:ems=\"10\"\n"
                        + "        android:inputType=\"text\" />\n"
                        + "\n"
                        + "    <!-- Wrong, doesn't include e-mail -->\n"
                        + "\n"
                        + "    <EditText\n"
                        + "        android:id=\"@+id/email_address\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:ems=\"10\"\n"
                        + "        android:inputType=\"text\" />\n"
                        + "\n"
                        + "    <!-- Wrong, uses wrong password type for PIN -->\n"
                        + "\n"
                        + "    <EditText\n"
                        + "        android:id=\"@+id/login_pin\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:ems=\"10\"\n"
                        + "        android:inputType=\"textPassword\" />\n"
                        + "\n"
                        + "    <EditText\n"
                        + "        android:id=\"@+id/number_of_items\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:ems=\"10\" />\n"
                        + "\n"
                        + "    <EditText\n"
                        + "        style=\"@style/foo\"\n"
                        + "        android:id=\"@+id/number_of_items\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:ems=\"10\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"))
                .run()
                .expect(expected);
    }

    public void testContainsWord() {
        assertFalse(containsWord("", "foob"));
        assertFalse(containsWord("foo", "foob"));

        assertTrue(containsWord("foo", "foo"));
        assertTrue(containsWord("Foo", "foo"));
        assertTrue(containsWord("foo_bar", "foo"));
        assertTrue(containsWord("bar_foo", "foo"));
        assertTrue(containsWord("bar_Foo", "foo"));
        assertTrue(containsWord("bar_foo_baz", "foo"));
        assertTrue(containsWord("bar_Foo_baz", "foo"));
        assertTrue(containsWord("barFooBaz", "foo"));
        assertTrue(containsWord("barFOO_", "foo"));
        assertTrue(containsWord("FooBaz", "foo"));
        assertTrue(containsWord("BarFoo", "foo"));
        assertFalse(containsWord("barfoo", "foo"));
        assertTrue(containsWord("barfoo", "foo", false, true));
        assertTrue(containsWord("foobar", "foo", true, false));
        assertFalse(containsWord("foobar", "foo"));
        assertFalse(containsWord("barfoobar", "foo"));

        assertTrue(containsWord("phoneNumber", "phone"));
        assertTrue(containsWord("phoneNumber", "number"));
        assertTrue(containsWord("uri_prefix", "uri"));
        assertTrue(containsWord("fooURI", "uri"));
        assertTrue(containsWord("my_url", "url"));
        assertTrue(containsWord("network_prefix_length", "length"));

        assertFalse(containsWord("sizer", "size"));
        assertFalse(containsWord("synthesize_to_filename", "size"));
        assertFalse(containsWord("update_text", "date"));
        assertFalse(containsWord("daten", "date"));

        assertFalse(containsWord("phonenumber", "phone"));
        assertFalse(containsWord("myphone", "phone"));
        assertTrue(containsWord("phonenumber", "phone", true, true));
        assertTrue(containsWord("myphone", "phone", true, true));
        assertTrue(containsWord("phoneNumber", "phone"));

        assertTrue(containsWord("phoneNumber", "phone"));
        assertTrue(containsWord("@id/phoneNumber", "phone"));
        assertTrue(containsWord("@+id/phoneNumber", "phone"));
    }

    private static boolean containsWord(String name, String word, boolean allowPrefix,
            boolean allowSuffix) {
        return TextFieldDetector.containsWord(name, word, allowPrefix, allowSuffix);
    }

    private static boolean containsWord(String name, String word) {
        return TextFieldDetector.containsWord(name, word);
    }

    public void testIncremental1() throws Exception {
        String expected = ""
                + "res/layout/note_edit2.xml:7: Warning: This text field does not specify an inputType or a hint [TextFields]\n"
                + "    <EditText\n"
                + "    ^\n"
                + "res/layout/note_edit2.xml:12: Warning: This text field does not specify an inputType or a hint [TextFields]\n"
                + "    <EditText\n"
                + "    ^\n"
                + "0 errors, 2 warnings\n";
        lint().files(mNote_edit2)
                .incremental("res/layout/note_edit2.xml")
                .run()
                .expect(expected);
    }

    public void testIncremental2() throws Exception {
        //noinspection all // Sample code
        lint().files(
                mNote_edit2,
                xml("res/values/styles-orientation.xml", ""
                        + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "    <style name=\"Layout.Horizontal\" parent=\"@style/Layout\">\n"
                        + "        <item name=\"android:layout_width\">match_parent</item>\n"
                        + "        <item name=\"android:orientation\">vertical</item>\n"
                        + "        <item name=\"android:layout_height\">wrap_content</item>\n"
                        + "    </style>\n"
                        + "\n"
                        + "    <style name=\"MyButtonStyle\" parent=\"@style/Layout\">\n"
                        + "        <item name=\"android:layout_width\">match_parent</item>\n"
                        + "        <item name=\"android:layout_height\">0dp</item>\n"
                        + "    </style>\n"
                        + "\n"
                        + "    <style name=\"TextWithHint\">\n"
                        + "        <item name=\"android:hint\">Number</item>\n"
                        + "    </style>\n"
                        + "\n"
                        + "    <style name=\"TextWithInput\">\n"
                        + "        <item name=\"android:hint\">number</item>\n"
                        + "    </style>\n"
                        + "</resources>\n"))
                .incremental("res/layout/note_edit2.xml")
                .run()
                .expectClean();
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mNote_edit2 = xml("res/layout/note_edit2.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "              android:orientation=\"vertical\"\n"
            + "              android:layout_width=\"fill_parent\"\n"
            + "              android:layout_height=\"fill_parent\">\n"
            + "\n"
            + "    <EditText\n"
            + "            style=\"@style/TextWithHint\"\n"
            + "            android:layout_width=\"wrap_content\"\n"
            + "            android:layout_height=\"wrap_content\" />\n"
            + "\n"
            + "    <EditText\n"
            + "            style=\"@style/TextWithInput\"\n"
            + "            android:layout_width=\"wrap_content\"\n"
            + "            android:layout_height=\"wrap_content\"\n"
            + "            android:layout_weight=\"1\" />\n"
            + "\n"
            + "</LinearLayout>\n");
}
