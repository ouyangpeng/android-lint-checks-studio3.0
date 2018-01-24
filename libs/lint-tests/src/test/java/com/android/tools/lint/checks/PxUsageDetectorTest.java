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
public class PxUsageDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new PxUsageDetector();
    }

    public void testPx() throws Exception {
        String expected = ""
                + "res/layout/now_playing_after.xml:49: Warning: Avoid using \"mm\" as units (it does not work accurately on all devices); use \"dp\" instead [InOrMmUsage]\n"
                + "        android:layout_width=\"100mm\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/now_playing_after.xml:50: Warning: Avoid using \"in\" as units (it does not work accurately on all devices); use \"dp\" instead [InOrMmUsage]\n"
                + "        android:layout_height=\"120in\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/now_playing_after.xml:41: Warning: Avoid using \"px\" as units; use \"dp\" instead [PxUsage]\n"
                + "        android:layout_width=\"2px\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 3 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/now_playing_after.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:id=\"@+id/now_playing\"\n"
                        + "    android:layout_width=\"fill_parent\"\n"
                        + "    android:layout_height=\"@dimen/now_playing_height\"\n"
                        + "    android:orientation=\"horizontal\">\n"
                        + "    <LinearLayout\n"
                        + "        android:background=\"@color/background2\"\n"
                        + "        android:paddingLeft=\"14dip\"\n"
                        + "        android:paddingRight=\"14dip\"\n"
                        + "        android:paddingTop=\"10dip\"\n"
                        + "        android:paddingBottom=\"10dip\"\n"
                        + "        android:layout_width=\"fill_parent\"\n"
                        + "        android:layout_height=\"fill_parent\"\n"
                        + "        android:layout_weight=\"1\"\n"
                        + "        android:orientation=\"vertical\">\n"
                        + "        <TextView\n"
                        + "            android:id=\"@+id/now_playing_title\"\n"
                        + "            android:duplicateParentState=\"true\"\n"
                        + "            android:layout_width=\"fill_parent\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:textStyle=\"bold\"\n"
                        + "            android:textSize=\"@dimen/text_size_large\"\n"
                        + "            android:textColor=\"@color/foreground1\"\n"
                        + "            android:text=\"@string/now_playing_after_title\"\n"
                        + "            android:maxLines=\"2\"\n"
                        + "            android:ellipsize=\"end\" />\n"
                        + "        <TextView\n"
                        + "            android:id=\"@+id/now_playing_subtitle\"\n"
                        + "            android:duplicateParentState=\"true\"\n"
                        + "            android:layout_width=\"fill_parent\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:paddingTop=\"3dip\"\n"
                        + "            android:textColor=\"@color/foreground2\"\n"
                        + "            android:textSize=\"@dimen/text_size_small\"\n"
                        + "            android:text=\"@string/now_playing_after_subtitle\"\n"
                        + "            android:singleLine=\"true\"\n"
                        + "            android:ellipsize=\"end\" />\n"
                        + "    </LinearLayout>\n"
                        + "    <View\n"
                        + "        android:layout_width=\"2px\"\n"
                        + "        android:layout_height=\"fill_parent\"\n"
                        + "        android:background=\"@android:color/white\" />\n"
                        + "    <ImageButton\n"
                        + "        android:background=\"@drawable/btn_now_playing_more\"\n"
                        + "        android:id=\"@+id/now_playing_more\"\n"
                        + "        android:src=\"@drawable/ic_now_playing_logo\"\n"
                        + "        android:padding=\"12dip\"\n"
                        + "        android:layout_width=\"100mm\"\n"
                        + "        android:layout_height=\"120in\"\n"
                        + "        android:onClick=\"onNowPlayingLogoClick\"\n"
                        + "        android:maxHeight=\"1px\"\n"
                        + "        android:scaleType=\"center\" />\n"
                        + "</LinearLayout>\n"))
                .run()
                .expect(expected);
    }

    public void testSp() throws Exception {
        String expected = ""
                + "res/layout/textsize.xml:11: Warning: Should use \"sp\" instead of \"dp\" for text sizes [SpUsage]\n"
                + "        android:textSize=\"14dp\" />\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/textsize.xml:16: Warning: Should use \"sp\" instead of \"dp\" for text sizes [SpUsage]\n"
                + "        android:textSize=\"14dip\" />\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/textsize.xml:33: Warning: Avoid using sizes smaller than 12sp: 11sp [SmallSp]\n"
                + "        android:textSize=\"11sp\" />\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/textsize.xml:37: Warning: Avoid using sizes smaller than 12sp: 6sp [SmallSp]\n"
                + "        android:layout_height=\"6sp\" />\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 4 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/textsize.xml", ""
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "    android:id=\"@+id/LinearLayout1\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:textSize=\"14dp\" />\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:textSize=\"14dip\" />\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:textSize=\"14sp\" />\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:textSize=\"@android/dimen/mysizedp\" />\n"
                        + "\n"
                        + "    <!-- Small -->\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:textSize=\"11sp\" />\n"
                        + "\n"
                        + "    <ImageView\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"6sp\" />\n"
                        + "\n"
                        + "    <!-- No warnings: wrong attribute, size == 0, etc -->\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:textSize=\"0sp\" />\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:marginTop=\"5sp\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for res/layout/textsize.xml line 10: Replace with sp:\n"
                        + "@@ -11 +11\n"
                        + "-         android:textSize=\"14dp\" />\n"
                        + "+         android:textSize=\"14sp\" />\n"
                        + "Fix for res/layout/textsize.xml line 15: Replace with sp:\n"
                        + "@@ -16 +16\n"
                        + "-         android:textSize=\"14dip\" />\n"
                        + "+         android:textSize=\"14sp\" />\n");
    }

    public void testStyles() throws Exception {
        String expected = ""
                + "res/values/pxsp.xml:23: Warning: Avoid using \"mm\" as units (it does not work accurately on all devices); use \"dp\" instead [InOrMmUsage]\n"
                + "        <item name=\"android:textSize\">50mm</item>\n"
                + "                                      ^\n"
                + "res/values/pxsp.xml:25: Warning: Avoid using \"in\" as units (it does not work accurately on all devices); use \"dp\" instead [InOrMmUsage]\n"
                + "            50in\n"
                + "            ^\n"
                + "res/values/pxsp.xml:6: Warning: Should use \"sp\" instead of \"dp\" for text sizes [SpUsage]\n"
                + "        <item name=\"android:textSize\">50dp</item>\n"
                + "                                      ^\n"
                + "res/values/pxsp.xml:12: Warning: Should use \"sp\" instead of \"dp\" for text sizes [SpUsage]\n"
                + "        <item name=\"android:textSize\"> 50dip </item>\n"
                + "                                       ^\n"
                + "res/values/pxsp.xml:9: Warning: Avoid using \"px\" as units; use \"dp\" instead [PxUsage]\n"
                + "        <item name=\"android:textSize\">50px</item>\n"
                + "                                      ^\n"
                + "res/values/pxsp.xml:17: Warning: Avoid using \"px\" as units; use \"dp\" instead [PxUsage]\n"
                + "        <item name=\"android:paddingRight\"> 50px </item>\n"
                + "                                           ^\n"
                + "res/values/pxsp.xml:18: Warning: Avoid using \"px\" as units; use \"dp\" instead [PxUsage]\n"
                + "        <item name=\"android:paddingTop\">50px</item>\n"
                + "                                        ^\n"
                + "0 errors, 7 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/values/pxsp.xml", ""
                        + "<resources>\n"
                        + "    <style name=\"Style1\">\n"
                        + "        <item name=\"android:textSize\">50sp</item>\n"
                        + "    </style>\n"
                        + "    <style name=\"Style2\">\n"
                        + "        <item name=\"android:textSize\">50dp</item>\n"
                        + "    </style>\n"
                        + "    <style name=\"Style3\">\n"
                        + "        <item name=\"android:textSize\">50px</item>\n"
                        + "    </style>\n"
                        + "    <style name=\"Style4\">\n"
                        + "        <item name=\"android:textSize\"> 50dip </item>\n"
                        + "    </style>\n"
                        + " \n"
                        + "    <style name=\"Style5\">\n"
                        + "        <item name=\"android:paddingLeft\">@dimen/whats_on_item_padding</item>\n"
                        + "        <item name=\"android:paddingRight\"> 50px </item>\n"
                        + "        <item name=\"android:paddingTop\">50px</item>\n"
                        + "        <item name=\"android:paddingBottom\">50dip</item>\n"
                        + "    </style>\n"
                        + "\n"
                        + "    <style name=\"Style6\">\n"
                        + "        <item name=\"android:textSize\">50mm</item>\n"
                        + "        <item name=\"android:textSize\">\n"
                        + "            50in\n"
                        + "        </item>\n"
                        + "    </style>\n"
                        + "\n"
                        + "    <style name=\"Widget.TabStrip\" parent=\"Widget\">\n"
                        + "        <item name=\"android:divider\">?android:attr/listDivider</item>\n"
                        + "        <item name=\"android:showDividers\">middle</item>\n"
                        + "        <item name=\"android:dividerPadding\">0px</item>\n"
                        + "        <item name=\"android:maxHeight\">1px</item>\n"
                        + "    </style>\n"
                        + "</resources>\n"))
                .run()
                .expect(expected)
                .verifyFixes().expectFixDiffs(""
                + "Fix for res/values/pxsp.xml line 5: Replace with sp:\n"
                + "@@ -6 +6\n"
                + "-         <item name=\"android:textSize\">50dp</item>\n"
                + "+         <item name=\"android:textSize\">50sp</item>\n"
                + "Fix for res/values/pxsp.xml line 11: Replace with sp:\n"
                + "@@ -12 +12\n"
                + "-         <item name=\"android:textSize\"> 50dip </item>\n"
                + "+         <item name=\"android:textSize\"> 50sp </item>\n");
    }

    public void testIncrementalDimensions() throws Exception {
        String expected = ""
                + "res/layout/textsize2.xml:9: Warning: Should use \"sp\" instead of \"dp\" for text sizes (@dimen/bottom_bar_portrait_button_font_size is defined as 16dp in values/dimens.xml [SpUsage]\n"
                + "        android:textSize=\"@dimen/bottom_bar_portrait_button_font_size\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";
        lint().files(
                dimens, textsize2)
                .incremental("res/layout/textsize2.xml")
                .run()
                .expect(expected);
    }

    public void testBatchDimensions() throws Exception {
        String expected = ""
                + "res/values/dimens.xml:2: Warning: This dimension is used as a text size: Should use \"sp\" instead of \"dp\" [SpUsage]\n"
                + "    <dimen name=\"bottom_bar_portrait_button_font_size\">16dp</dimen>\n"
                + "                                                       ^\n"
                + "    res/layout/textsize2.xml:9: Dimension used as a text size here\n"
                + "0 errors, 1 warnings\n";
        lint().files(
                dimens, textsize2)
                .run()
                .expect(expected);
    }

    @SuppressWarnings("all") // Sample code
    private TestFile dimens = xml("res/values/dimens.xml", ""
            + "<resources>\n"
            + "    <dimen name=\"bottom_bar_portrait_button_font_size\">16dp</dimen>\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile textsize2 = xml("res/layout/textsize2.xml", ""
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
            + "    android:id=\"@+id/LinearLayout1\"\n"
            + "    android:layout_width=\"match_parent\"\n"
            + "    android:layout_height=\"match_parent\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "    <TextView\n"
            + "        android:textSize=\"@dimen/bottom_bar_portrait_button_font_size\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\" />\n"
            + "\n"
            + "</LinearLayout>\n");
}
