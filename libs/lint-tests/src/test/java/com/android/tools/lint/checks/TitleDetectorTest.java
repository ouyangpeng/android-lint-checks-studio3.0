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
public class TitleDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new TitleDetector();
    }

    public void test() throws Exception {
        String expected = ""
                + "res/menu/titles.xml:3: Error: Menu items should specify a title [MenuTitle]\n"
                + "    <item android:id=\"@+id/action_bar_progress_spinner\"\n"
                + "    ^\n"
                + "res/menu/titles.xml:12: Error: Menu items should specify a title [MenuTitle]\n"
                + "    <item android:id=\"@+id/menu_plus_one\"\n"
                + "    ^\n"
                + "2 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                    manifest().minSdk(14),
                    mTitles)
                .run()
                .expect(expected)
                .verifyFixes().window(1).expectFixDiffs(""
                + "Fix for res/menu/titles.xml line 2: Set title:\n"
                + "@@ -9 +9\n"
                + "          android:selectableItemBackground=\"@null\"\n"
                + "-         android:showAsAction=\"always\"/>\n"
                + "+         android:showAsAction=\"always\"\n"
                + "+         android:title=\"|\"/>\n"
                + "      <item\n"
                + "Fix for res/menu/titles.xml line 11: Set title:\n"
                + "@@ -18 +18\n"
                + "          android:icon=\"@drawable/ic_menu_plus1\"\n"
                + "-         android:showAsAction=\"always\"/>\n"
                + "+         android:showAsAction=\"always\"\n"
                + "+         android:title=\"|\"/>\n"
                + "  \n");
    }

    public void testOk() throws Exception {
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintProject(
                    manifest().minSdk(1),
                    mTitles));
    }

    public void testOk2() throws Exception {
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintProject(xml("res/menu-land/actions.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                            + "\n"
                            + "    <item\n"
                            + "        android:id=\"@+id/menu_search\"\n"
                            + "        android:showAsAction=\"always|collapseActionView\"\n"
                            + "        android:actionViewClass=\"android.widget.SearchView\" />\n"
                            + "\n"
                            + "    <group android:id=\"@+id/reader_items\">\n"
                            + "\n"
                            + "        <item\n"
                            + "            android:id=\"@+id/menu_table_of_contents\"\n"
                            + "            android:showAsAction=\"always\"\n"
                            + "            android:actionLayout=\"@layout/action_table_of_contents\" />\n"
                            + "\n"
                            + "        <item\n"
                            + "            android:id=\"@+id/menu_settings\"\n"
                            + "            android:showAsAction=\"always\" />\n"
                            + "\n"
                            + "        <item android:id=\"@+id/menu_mode\"\n"
                            + "            android:showAsAction=\"never\" />\n"
                            + "\n"
                            + "        <item\n"
                            + "            android:id=\"@+id/menu_buy\"\n"
                            + "            android:showAsAction=\"never\" />\n"
                            + "\n"
                            + "        <item\n"
                            + "            android:id=\"@+id/menu_about\"\n"
                            + "            android:showAsAction=\"never\" />\n"
                            + "\n"
                            + "        <item\n"
                            + "            android:id=\"@+id/menu_share\"\n"
                            + "            android:showAsAction=\"never\" />\n"
                            + "\n"
                            + "        <item\n"
                            + "            android:id=\"@+id/menu_keep\"\n"
                            + "            android:checkable=\"true\"\n"
                            + "            android:showAsAction=\"never\" />\n"
                            + "\n"
                            + "        <item\n"
                            + "            android:id=\"@+id/menu_d\"\n"
                            + "            android:showAsAction=\"never\" />\n"
                            + "\n"
                            + "        <item\n"
                            + "            android:id=\"@+id/menu_help\"\n"
                            + "            android:showAsAction=\"never\" />\n"
                            + "\n"
                            + "    </group>\n"
                            + "\n"
                            + "    <group android:id=\"@+id/search_items\">\n"
                            + "\n"
                            + "        <item\n"
                            + "            android:id=\"@+id/menu_table_of_contents\"\n"
                            + "            android:showAsAction=\"always\"\n"
                            + "            android:actionLayout=\"@layout/action_table_of_contents\" />\n"
                            + "\n"
                            + "        <item android:id=\"@+id/menu_search_exit\"\n"
                            + "              android:showAsAction=\"never\" />\n"
                            + "\n"
                            + "    </group>\n"
                            + "\n"
                            + "</menu>\n")));
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mTitles = xml("res/menu/titles.xml", ""
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
            + "</menu>\n");
}
