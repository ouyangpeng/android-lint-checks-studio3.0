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
public class ResourceCycleDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new ResourceCycleDetector();
    }

    public void testStyles() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/values/styles.xml:9: Error: Style DetailsPage_EditorialBuyButton should not extend itself [ResourceCycle]\n"
                + "<style name=\"DetailsPage_EditorialBuyButton\" parent=\"@style/DetailsPage_EditorialBuyButton\" />\n"
                + "                                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintProject(xml("res/values/styles.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<resources>\n"
                            + "\n"
                            + "<style name=\"DetailsPage_BuyButton\" parent=\"@style/DetailsPage_Button\">\n"
                            + "       <item name=\"android:textColor\">@color/buy_button</item>\n"
                            + "       <item name=\"android:background\">@drawable/details_page_buy_button</item>\n"
                            + "</style>\n"
                            + "\n"
                            + "<style name=\"DetailsPage_EditorialBuyButton\" parent=\"@style/DetailsPage_EditorialBuyButton\" />\n"
                            + "<!-- Should have been:\n"
                            + "<style name=\"DetailsPage_EditorialBuyButton\" parent=\"@style/DetailsPage_BuyButton\" />\n"
                            + "-->\n"
                            + "\n"
                            + "</resources>\n"
                            + "\n")));
    }

    public void testStyleImpliedParent() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/values/stylecycle.xml:3: Error: Potential cycle: PropertyToggle is the implied parent of PropertyToggle.Base and this defines the opposite [ResourceCycle]\n"
                + "  <style name=\"PropertyToggle\" parent=\"@style/PropertyToggle.Base\"></style>\n"
                + "                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintProject(xml("res/values/stylecycle.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                            + "  <style name=\"PropertyToggle\" parent=\"@style/PropertyToggle.Base\"></style>\n"
                            + "  <style name=\"PropertyToggle.Base\"></style>\n"
                            + "</resources>\n")));
    }

    public void testLayouts() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/layoutcycle1.xml:10: Error: Layout layoutcycle1 should not include itself [ResourceCycle]\n"
                + "        layout=\"@layout/layoutcycle1\" />\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

                lintProject(xml("res/layout/layoutcycle1.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\"\n"
                            + "    android:orientation=\"vertical\" >\n"
                            + "\n"
                            + "    <include\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        layout=\"@layout/layoutcycle1\" />\n"
                            + "\n"
                            + "</LinearLayout>\n")));
    }

    public void testColors() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/values/colorcycle1.xml:2: Error: Color test should not reference itself [ResourceCycle]\n"
                + "    <color name=\"test\">@color/test</color>\n"
                + "                       ^\n"
                + "1 errors, 0 warnings\n",

                lintProject(xml("res/values/colorcycle1.xml", ""
                            + "<resources>\n"
                            + "    <color name=\"test\">@color/test</color>\n"
                            + "</resources>\n")));
    }

    public void testAaptCrash() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/values/aaptcrash.xml:5: Error: This construct can potentially crash aapt during a build. Change @+id/titlebar to @id/titlebar and define the id explicitly using <item type=\"id\" name=\"titlebar\"/> instead. [AaptCrash]\n"
                + "        <item name=\"android:id\">@+id/titlebar</item>\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

                lintProject(xml("res/values/aaptcrash.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                            + "    <style name=\"TitleBar\">\n"
                            + "        <item name=\"android:orientation\">horizontal</item>\n"
                            + "        <item name=\"android:id\">@+id/titlebar</item>\n"
                            + "        <item name=\"android:background\">@drawable/bg_titlebar</item>\n"
                            + "        <item name=\"android:layout_width\">fill_parent</item>\n"
                            + "        <item name=\"android:layout_height\">@dimen/titlebar_height</item>\n"
                            + "    </style>\n"
                            + "</resources>\n")));
    }

    public void testDeepColorCycle1() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/values/colorcycle2.xml:2: Error: Color Resource definition cycle: test1 => test2 => test3 => test1 [ResourceCycle]\n"
                + "    <color name=\"test1\">@color/test2</color>\n"
                + "                        ^\n"
                + "    res/values/colorcycle4.xml:2: Reference from @color/test3 to color/test1 here\n"
                + "    res/values/colorcycle3.xml:2: Reference from @color/test2 to color/test3 here\n"
                + "1 errors, 0 warnings\n",

                lintProject(
                        mColorcycle2,
                        xml("res/values/colorcycle3.xml", ""
                            + "<resources>\n"
                            + "    <color name=\"test2\">@color/test3</color>\n"
                            + "    <color name=\"test2b\">#ff00ff00</color>\n"
                            + "</resources>\n"),
                        xml("res/values/colorcycle4.xml", ""
                            + "<resources>\n"
                            + "    <color name=\"test3\">@color/test1</color>\n"
                            + "</resources>\n")
                ));
    }

    public void testDeepColorCycle2() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/values/colorcycle5.xml:2: Error: Color Resource definition cycle: test1 => test2 => test1 [ResourceCycle]\n"
                + "    <color name=\"test1\">@color/test2</color>\n"
                + "                        ^\n"
                + "    res/values/colorcycle5.xml:3: Reference from @color/test2 to color/test1 here\n"
                + "1 errors, 0 warnings\n",

                lintProject(
                        xml("res/values/colorcycle5.xml", ""
                            + "<resources>\n"
                            + "    <color name=\"test1\">@color/test2</color>\n"
                            + "    <color name=\"test2\">@color/test1</color>\n"
                            + "</resources>\n")
                ));
    }

    public void testDeepStyleCycle1() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/values/stylecycle1.xml:6: Error: Style Resource definition cycle: ButtonStyle => ButtonStyle.Base => ButtonStyle [ResourceCycle]\n"
                + "    <style name=\"ButtonStyle\" parent=\"ButtonStyle.Base\">\n"
                + "                              ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/stylecycle1.xml:3: Reference from @style/ButtonStyle.Base to style/ButtonStyle here\n"
                + "1 errors, 0 warnings\n",

                lintProject(
                        xml("res/values/stylecycle1.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<resources>\n"
                            + "    <style name=\"ButtonStyle.Base\">\n"
                            + "        <item name=\"android:textColor\">#ff0000</item>\n"
                            + "    </style>\n"
                            + "    <style name=\"ButtonStyle\" parent=\"ButtonStyle.Base\">\n"
                            + "        <item name=\"android:layout_height\">40dp</item>\n"
                            + "    </style>\n"
                            + "</resources>\n")
                ));
    }

    public void testDeepStyleCycle2() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/values/stylecycle2.xml:3: Error: Style Resource definition cycle: mystyle1 => mystyle2 => mystyle3 => mystyle1 [ResourceCycle]\n"
                + "    <style name=\"mystyle1\" parent=\"@style/mystyle2\">\n"
                + "                           ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/stylecycle2.xml:9: Reference from @style/mystyle3 to style/mystyle1 here\n"
                + "    res/values/stylecycle2.xml:6: Reference from @style/mystyle2 to style/mystyle3 here\n"
                + "1 errors, 0 warnings\n",

                lintProject(
                        xml("res/values/stylecycle2.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<resources>\n"
                            + "    <style name=\"mystyle1\" parent=\"@style/mystyle2\">\n"
                            + "        <item name=\"android:textColor\">#ff0000</item>\n"
                            + "    </style>\n"
                            + "    <style name=\"mystyle2\" parent=\"@style/mystyle3\">\n"
                            + "        <item name=\"android:textColor\">#ff0ff</item>\n"
                            + "    </style>\n"
                            + "    <style name=\"mystyle3\" parent=\"@style/mystyle1\">\n"
                            + "        <item name=\"android:textColor\">#ffff00</item>\n"
                            + "    </style>\n"
                            + "</resources>\n")
                ));
    }

    public void testDeepIncludeOk() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "No warnings.",

                lintProject(
                        mLayout1,
                        mLayout2,
                        mLayout3,
                        xml("res/layout/layout4.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\"\n"
                            + "    android:orientation=\"vertical\" >\n"
                            + "\n"
                            + "    <Button\n"
                            + "        android:id=\"@+id/button1\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:text=\"Button\" />\n"
                            + "\n"
                            + "    <Button\n"
                            + "        android:id=\"@+id/button2\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:text=\"Button\" />\n"
                            + "\n"
                            + "</LinearLayout>\n")
                ));
    }

    public void testDeepIncludeCycle() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/layout1.xml:10: Error: Layout Resource definition cycle: layout1 => layout2 => layout4 => layout1 [ResourceCycle]\n"
                + "        layout=\"@layout/layout2\" />\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/layout/layout4.xml:16: Reference from @layout/layout4 to layout/layout1 here\n"
                + "    res/layout/layout2.xml:16: Reference from @layout/layout2 to layout/layout4 here\n"
                + "1 errors, 0 warnings\n",

                lintProject(
                        mLayout1,
                        mLayout2,
                        mLayout3,
                        xml("res/layout/layout4.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\"\n"
                            + "    android:orientation=\"vertical\" >\n"
                            + "\n"
                            + "    <RadioButton\n"
                            + "        android:id=\"@+id/radioButton1\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:text=\"RadioButton\" />\n"
                            + "\n"
                            + "    <include\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        layout=\"@layout/layout1\" />\n"
                            + "\n"
                            + "</LinearLayout>\n")
                ));
    }

    public void testDeepAliasCycle() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/values/aliases.xml:2: Error: Layout Resource definition cycle: layout10 => layout20 => layout30 => layout10 [ResourceCycle]\n"
                + "    <item name=\"layout10\" type=\"layout\">@layout/layout20</item>\n"
                + "                                        ^\n"
                + "    res/values/aliases.xml:4: Reference from @layout/layout30 to layout/layout10 here\n"
                + "    res/values/aliases.xml:3: Reference from @layout/layout20 to layout/layout30 here\n"
                + "res/values/colorcycle2.xml:2: Error: Color Resource definition cycle: test1 => test2 => test1 [ResourceCycle]\n"
                + "    <color name=\"test1\">@color/test2</color>\n"
                + "                        ^\n"
                + "    res/values/aliases.xml:5: Reference from @color/test2 to color/test1 here\n"
                + "res/layout/layout1.xml:10: Error: Layout Resource definition cycle: layout1 => layout2 => layout4 => layout1 [ResourceCycle]\n"
                + "        layout=\"@layout/layout2\" />\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/values/aliases.xml:6: Reference from @layout/layout4 to layout/layout1 here\n"
                + "    res/layout/layout2.xml:16: Reference from @layout/layout2 to layout/layout4 here\n"
                + "3 errors, 0 warnings\n",

                lintProject(
                        xml("res/values/aliases.xml", ""
                            + "<resources>\n"
                            + "    <item name=\"layout10\" type=\"layout\">@layout/layout20</item>\n"
                            + "    <item name=\"layout20\" type=\"layout\">@layout/layout30</item>\n"
                            + "    <item name=\"layout30\" type=\"layout\">@layout/layout10</item>\n"
                            + "    <item name=\"test2\" type=\"color\">@color/test1</item>\n"
                            + "    <item name=\"layout4\" type=\"layout\">@layout/layout1</item>\n"
                            + "</resources>\n"
                            + "\n"),
                        mLayout1,
                        mLayout2,
                        mLayout3,
                        mColorcycle2
                ));
    }

    public void testColorStateListCycle() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/values/aliases2.xml:2: Error: Color Resource definition cycle: bright_foreground_dark => color1 => bright_foreground_dark [ResourceCycle]\n"
                + "    <item name=\"bright_foreground_dark\" type=\"color\">@color/color1</item>\n"
                + "                                                     ^\n"
                + "    res/color/color1.xml:3: Reference from @color/color1 to color/bright_foreground_dark here\n"
                + "1 errors, 0 warnings\n",

                lintProject(
                        xml("res/color/color1.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                            + "    <item android:state_enabled=\"false\" android:color=\"@color/bright_foreground_dark_disabled\"/>\n"
                            + "    <item android:color=\"@color/bright_foreground_dark\"/>\n"
                            + "</selector>\n"),
                        mAliases2
                ));
    }

    public void testDrawableStateListCycle() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/drawable/drawable1.xml:4: Error: Drawable Resource definition cycle: drawable1 => textfield_search_pressed => drawable2 => drawable1 [ResourceCycle]\n"
                + "    <item android:state_window_focused=\"false\" android:state_enabled=\"true\"\n"
                + "    ^\n"
                + "    res/values/aliases2.xml:4: Reference from @drawable/drawable2 to drawable/drawable1 here\n"
                + "    res/values/aliases2.xml:3: Reference from @drawable/textfield_search_pressed to drawable/drawable2 here\n"
                + "1 errors, 0 warnings\n",

                lintProject(
                        xml("res/drawable/drawable1.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                            + "\n"
                            + "    <item android:state_window_focused=\"false\" android:state_enabled=\"true\"\n"
                            + "        android:drawable=\"@drawable/textfield_search_default\" />\n"
                            + "\n"
                            + "    <item android:state_pressed=\"true\"\n"
                            + "        android:drawable=\"@drawable/textfield_search_pressed\" />\n"
                            + "\n"
                            + "    <item android:state_enabled=\"true\" android:state_focused=\"true\"\n"
                            + "        android:drawable=\"@drawable/textfield_search_selected\" />\n"
                            + "\n"
                            + "    <item android:drawable=\"@drawable/textfield_search_default\" />\n"
                            + "</selector>\n"
                            + "\n"),
                        mAliases2
                ));
    }

    public void testFontCycle() throws Exception {
        //noinspection all // Sample code
        assertEquals(
                ""
                        + "res/font/font1.xml:6: Error: Font Resource definition cycle: font1 => font2 => font1 [ResourceCycle]\n"
                        + "        android:font=\"@font/font2\" />\n"
                        + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "    res/font/font2.xml:6: Reference from @font/font2 to font/font1 here\n"
                        + "1 errors, 0 warnings\n",
                lintProject(
                        xml(
                                "res/font/font1.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                                        + "    <font\n"
                                        + "        android:fontStyle=\"normal\"\n"
                                        + "        android:fontWeight=\"400\"\n"
                                        + "        android:font=\"@font/font2\" />\n"
                                        + "</font-family>"
                                        + "\n"),
                        xml(
                                "res/font/font2.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                                        + "    <font\n"
                                        + "        android:fontStyle=\"italic\"\n"
                                        + "        android:fontWeight=\"400\"\n"
                                        + "        android:font=\"@font/font1\" />\n"
                                        + "</font-family>"
                                        + "\n")));
    }

    public void testFontCycleWithLocation() throws Exception {
        //noinspection all // Sample code
        assertEquals(
                ""
                        + "res/font/font1.xml:6: Error: Font Resource definition cycle: font1 => font2 => font3 => font1 [ResourceCycle]\n"
                        + "        android:font=\"@font/font2\" />\n"
                        + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "    res/font/font3.xml:6: Reference from @font/font3 to font/font1 here\n"
                        + "    res/font/font2.xml:6: Reference from @font/font2 to font/font3 here\n"
                        + "1 errors, 0 warnings\n",
                lintProject(
                        xml(
                                "res/font/font1.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                                        + "    <font\n"
                                        + "        android:fontStyle=\"normal\"\n"
                                        + "        android:fontWeight=\"400\"\n"
                                        + "        android:font=\"@font/font2\" />\n"
                                        + "</font-family>"
                                        + "\n"),
                        xml(
                                "res/font/font2.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                                        + "    <font\n"
                                        + "        android:fontStyle=\"italic\"\n"
                                        + "        android:fontWeight=\"400\"\n"
                                        + "        android:font=\"@font/font3\" />\n"
                                        + "</font-family>"
                                        + "\n"),
                        xml(
                                "res/font/font3.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                                        + "    <font\n"
                                        + "        android:fontStyle=\"normal\"\n"
                                        + "        android:fontWeight=\"700\"\n"
                                        + "        android:font=\"@font/font1\" />\n"
                                        + "</font-family>"
                                        + "\n")));
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mAliases2 = xml("res/values/aliases2.xml", ""
            + "<resources>\n"
            + "    <item name=\"bright_foreground_dark\" type=\"color\">@color/color1</item>\n"
            + "    <item name=\"textfield_search_pressed\" type=\"drawable\">@drawable/drawable2</item>\n"
            + "    <item name=\"drawable2\" type=\"drawable\">@drawable/drawable1</item>\n"
            + "</resources>\n"
            + "\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mColorcycle2 = xml("res/values/colorcycle2.xml", ""
            + "<resources>\n"
            + "    <color name=\"test1\">@color/test2</color>\n"
            + "    <color name=\"unrelated1\">@color/test2b</color>\n"
            + "    <color name=\"unrelated2\">#ff0000</color>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mLayout1 = xml("res/layout/layout1.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:layout_width=\"match_parent\"\n"
            + "    android:layout_height=\"match_parent\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "    <include\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        layout=\"@layout/layout2\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:id=\"@+id/button1\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:id=\"@+id/button2\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "</LinearLayout>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mLayout2 = xml("res/layout/layout2.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:layout_width=\"match_parent\"\n"
            + "    android:layout_height=\"match_parent\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "    <RadioButton\n"
            + "        android:id=\"@+id/radioButton1\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:text=\"RadioButton\" />\n"
            + "\n"
            + "    <include\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        layout=\"@layout/layout3\" />\n"
            + "\n"
            + "    <include\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        layout=\"@layout/layout4\" />\n"
            + "\n"
            + "</LinearLayout>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mLayout3 = xml("res/layout/layout3.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:layout_width=\"match_parent\"\n"
            + "    android:layout_height=\"match_parent\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "    <Button\n"
            + "        android:id=\"@+id/button1\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <CheckBox\n"
            + "        android:id=\"@+id/checkBox1\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:text=\"CheckBox\" />\n"
            + "\n"
            + "</LinearLayout>\n");
}
