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
public class HardcodedValuesDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new HardcodedValuesDetector();
    }

    public void testStrings() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/accessibility.xml:3: Warning: Hardcoded string \"Button\", should use @string resource [HardcodedText]\n"
                + "    <Button android:text=\"Button\" android:id=\"@+id/button1\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"></Button>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/accessibility.xml:6: Warning: Hardcoded string \"Button\", should use @string resource [HardcodedText]\n"
                + "    <Button android:text=\"Button\" android:id=\"@+id/button2\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"></Button>\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

            lintFiles(xml("res/layout/accessibility.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" android:id=\"@+id/newlinear\" android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">\n"
                            + "    <Button android:text=\"Button\" android:id=\"@+id/button1\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"></Button>\n"
                            + "    <ImageView android:id=\"@+id/android_logo\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                            + "    <ImageButton android:importantForAccessibility=\"yes\" android:id=\"@+id/android_logo2\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                            + "    <Button android:text=\"Button\" android:id=\"@+id/button2\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"></Button>\n"
                            + "    <Button android:id=\"@+android:id/summary\" android:contentDescription=\"@string/label\" />\n"
                            + "    <ImageButton android:importantForAccessibility=\"no\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                            + "</LinearLayout>\n")));
    }

    public void testMenus() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/menu/menu.xml:7: Warning: Hardcoded string \"My title 1\", should use @string resource [HardcodedText]\n"
                + "        android:title=\"My title 1\">\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/menu/menu.xml:13: Warning: Hardcoded string \"My title 2\", should use @string resource [HardcodedText]\n"
                + "        android:title=\"My title 2\">\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

            lintFiles(xml("res/menu/menu.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\" >\n"
                            + "\n"
                            + "    <item\n"
                            + "        android:id=\"@+id/item1\"\n"
                            + "        android:icon=\"@drawable/icon1\"\n"
                            + "        android:title=\"My title 1\">\n"
                            + "    </item>\n"
                            + "    <item\n"
                            + "        android:id=\"@+id/item2\"\n"
                            + "        android:icon=\"@drawable/icon2\"\n"
                            + "        android:showAsAction=\"ifRoom\"\n"
                            + "        android:title=\"My title 2\">\n"
                            + "    </item>\n"
                            + "\n"
                            + "</menu>\n")));
    }

    public void testMenusOk() throws Exception {
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",
            lintFiles(xml("res/menu/titles.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                            + "    <item android:id=\"@+id/action_bar_progress_spinner\"\n"
                            + "        android:showAsAction=\"always\"\n"
                            + "        android:background=\"@null\"\n"
                            + "        android:selectableItemBackground=\"@null\"\n"
                            + "        android:actionLayout=\"@layout/action_bar_progress_spinner_layout\"/>\n"
                            + "    <item android:id=\"@+id/refresh\"\n"
                            + "        android:title=\"@string/menu_refresh\"\n"
                            + "        android:showAsAction=\"always\"\n"
                            + "        android:icon=\"@drawable/ic_menu_refresh\"/>\n"
                            + "    <item android:id=\"@+id/menu_plus_one\"\n"
                            + "        android:showAsAction=\"always\"\n"
                            + "        android:icon=\"@drawable/ic_menu_plus1\"/>\n"
                            + "</menu>\n")));
    }

    public void testSuppress() throws Exception {
        // All but one errors in the file contain ignore attributes - direct, inherited
        // and lists
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/ignores.xml:61: Warning: Hardcoded string \"Hardcoded\", should use @string resource [HardcodedText]\n"
                + "        android:text=\"Hardcoded\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",

            lintFiles(xml("res/layout/ignores.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "    android:id=\"@+id/newlinear\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\"\n"
                            + "    android:orientation=\"vertical\" >\n"
                            + "\n"
                            + "    <!-- Ignored via attribute, should be hidden -->\n"
                            + "\n"
                            + "    <Button\n"
                            + "        android:id=\"@+id/button1\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:text=\"Button1\"\n"
                            + "        tools:ignore=\"HardcodedText\" >\n"
                            + "    </Button>\n"
                            + "\n"
                            + "    <!-- Inherited ignore from parent -->\n"
                            + "\n"
                            + "    <LinearLayout\n"
                            + "        android:id=\"@+id/parent\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        tools:ignore=\"HardcodedText\" >\n"
                            + "\n"
                            + "        <Button\n"
                            + "            android:id=\"@+id/button2\"\n"
                            + "            android:layout_width=\"wrap_content\"\n"
                            + "            android:layout_height=\"wrap_content\"\n"
                            + "            android:text=\"Button2\" >\n"
                            + "        </Button>\n"
                            + "    </LinearLayout>\n"
                            + "\n"
                            + "    <!-- Hardcoded text warning ignored through \"all\" -->\n"
                            + "\n"
                            + "    <Button\n"
                            + "        android:id=\"@+id/button3\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:text=\"Button3\"\n"
                            + "        tools:ignore=\"all\" >\n"
                            + "    </Button>\n"
                            + "\n"
                            + "    <!-- Ignored through item in ignore list -->\n"
                            + "\n"
                            + "    <Button\n"
                            + "        android:id=\"@+id/button4\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:text=\"Hardcoded\"\n"
                            + "        tools:ignore=\"NewApi,HardcodedText\" >\n"
                            + "    </Button>\n"
                            + "\n"
                            + "    <!-- Not ignored: should show up as a warning -->\n"
                            + "\n"
                            + "    <Button\n"
                            + "        android:id=\"@+id/button5\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:text=\"Hardcoded\"\n"
                            + "        tools:ignore=\"Other\" >\n"
                            + "    </Button>\n"
                            + "\n"
                            + "</LinearLayout>\n")));
    }

    public void testSuppressViaComment() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/ignores2.xml:51: Warning: Hardcoded string \"Hardcoded\", should use @string resource [HardcodedText]\n"
                + "        android:text=\"Hardcoded\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",

                lintFiles(xml("res/layout/ignores2.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    android:id=\"@+id/newlinear\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\"\n"
                            + "    android:orientation=\"vertical\" >\n"
                            + "\n"
                            + "    <!-- Ignored via comment, should be hidden -->\n"
                            + "\n"
                            + "    <!--suppress AndroidLintHardcodedText -->\n"
                            + "    <Button\n"
                            + "        android:id=\"@+id/button1\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:text=\"Button1\" >\n"
                            + "    </Button>\n"
                            + "\n"
                            + "    <!-- Inherited ignore from parent -->\n"
                            + "\n"
                            + "    <!--suppress AndroidLintHardcodedText-->\n"
                            + "    <LinearLayout\n"
                            + "        android:id=\"@+id/parent\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\" >\n"
                            + "\n"
                            + "        <Button\n"
                            + "            android:id=\"@+id/button2\"\n"
                            + "            android:layout_width=\"wrap_content\"\n"
                            + "            android:layout_height=\"wrap_content\"\n"
                            + "            android:text=\"Button2\" >\n"
                            + "        </Button>\n"
                            + "    </LinearLayout>\n"
                            + "\n"
                            + "    <!-- Ignored through item in ignore list -->\n"
                            + "\n"
                            + "    <!--suppress AndroidLintNewApi,AndroidLintHardcodedText -->\n"
                            + "\n"
                            + "    <Button\n"
                            + "        android:id=\"@+id/button4\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:text=\"Hardcoded\" >\n"
                            + "    </Button>\n"
                            + "\n"
                            + "    <!-- Not ignored: should show up as a warning -->\n"
                            + "    <!--suppress AndroidLintNewApi -->\n"
                            + "    <Button\n"
                            + "        android:id=\"@+id/button5\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:text=\"Hardcoded\"\n"
                            + "        >\n"
                            + "    </Button>\n"
                            + "\n"
                            + "</LinearLayout>\n")));
    }

    public void testSkippingPlaceHolders() throws Exception {
        assertEquals("No warnings.",
                lintProject(
                        xml("res/layout/test.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\">\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:id=\"@+id/textView\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"Hello World!\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:id=\"@+id/button\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"New Button\" />\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:id=\"@+id/textView2\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"Large Text\"\n"
                        + "        android:textAppearance=\"?android:attr/textAppearanceLarge\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:id=\"@+id/button2\"\n"
                        + "        style=\"?android:attr/buttonStyleSmall\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"New Button\" />\n"
                        + "\n"
                        + "    <CheckBox\n"
                        + "        android:id=\"@+id/checkBox\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"New CheckBox\" />\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:id=\"@+id/textView3\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"New Text\" />\n"
                        + "</LinearLayout>\n")));
    }

    public void testAppRestrictions() throws Exception {
        // Sample from https://developer.android.com/samples/AppRestrictionSchema/index.html
        assertEquals(""
                + "res/xml/app_restrictions.xml:12: Warning: Hardcoded string \"Hardcoded description\", should use @string resource [HardcodedText]\n"
                + "        android:description=\"Hardcoded description\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/xml/app_restrictions.xml:15: Warning: Hardcoded string \"Hardcoded title\", should use @string resource [HardcodedText]\n"
                + "        android:title=\"Hardcoded title\"/>\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",
                lintProject(
                        xml("res/xml/app_restrictions.xml", ""
                                + "<restrictions xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                                + " \n"
                                + "    <restriction\n"
                                + "        android:defaultValue=\"@bool/default_can_say_hello\"\n"
                                + "        android:description=\"@string/description_can_say_hello\"\n"
                                + "        android:key=\"can_say_hello\"\n"
                                + "        android:restrictionType=\"bool\"\n"
                                + "        android:title=\"@string/title_can_say_hello\"/>\n"
                                + " \n"
                                + "    <restriction\n"
                                + "        android:defaultValue=\"Hardcoded default value\"\n"
                                + "        android:description=\"Hardcoded description\"\n"
                                + "        android:key=\"message\"\n"
                                + "        android:restrictionType=\"string\"\n"
                                + "        android:title=\"Hardcoded title\"/>\n"
                                + " \n"
                                + "</restrictions>"),
                        xml("res/xml/random_file.xml", ""
                                + "<myRoot xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                                + " \n"
                                + "    <myElement\n"
                                + "        android:description=\"Hardcoded description\"\n"
                                + "        android:title=\"Hardcoded title\"/>\n"
                                + " \n"
                                + "</myRoot>")
                ));
    }

    public void testToggleButtonLabels() throws Exception {
        // Regression test for
        // https://code.google.com/p/android/issues/detail?id=206106
        // Ensure that the toggle button text label attributes are internationalized
        String expected = ""
                + "res/layout/test.xml:5: Warning: Hardcoded string \"Hi tools!\", should use @string resource [HardcodedText]\n"
                + "     android:textOn=\"Hi tools!\"\n"
                + "     ~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/test.xml:6: Warning: Hardcoded string \"Bye tools!\", should use @string resource [HardcodedText]\n"
                + "     android:textOff=\"Bye tools!\" />\n"
                + "     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n";
        lint().files(
                xml("res/layout/test.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<ToggleButton xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "     android:layout_width=\"wrap_content\"\n"
                        + "     android:layout_height=\"wrap_content\"\n"
                        + "     android:textOn=\"Hi tools!\"\n"
                        + "     android:textOff=\"Bye tools!\" />"))
                .run()
                .expect(expected);
    }
}
