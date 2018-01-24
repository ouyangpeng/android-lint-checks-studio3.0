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
public class AlwaysShowActionDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new AlwaysShowActionDetector();
    }

    public void testXmlMenus() throws Exception {
        //noinspection all // Sample code
        String expected = ""
                + "res/menu-land/actions.xml:6: Warning: Prefer \"ifRoom\" instead of \"always\" [AlwaysShowAction]\n"
                + "        android:showAsAction=\"always|collapseActionView\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/menu-land/actions.xml:13: <No location-specific message\n"
                + "    res/menu-land/actions.xml:18: <No location-specific message\n"
                + "    res/menu-land/actions.xml:54: <No location-specific message\n"
                + "0 errors, 1 warnings\n";
        lint().files(
                xml("res/menu-land/actions.xml", ""
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
                        + "</menu>\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for res/menu-land/actions.xml line 5: Replace with ifRoom:\n"
                        + "@@ -6 +6\n"
                        + "-         android:showAsAction=\"always|collapseActionView\"\n"
                        + "+         android:showAsAction=\"ifRoom|collapseActionView\"\n");
    }

    public void testXmlMenusWithFlags() throws Exception {
        String expected = ""
                + "res/menu-land/actions2.xml:6: Warning: Prefer \"ifRoom\" instead of \"always\" [AlwaysShowAction]\n"
                + "        android:showAsAction=\"always|collapseActionView\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/menu-land/actions2.xml:13: <No location-specific message\n"
                + "    res/menu-land/actions2.xml:18: <No location-specific message\n"
                + "    res/menu-land/actions2.xml:54: <No location-specific message\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/menu-land/actions2.xml", ""
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
                        + "            android:showAsAction=\"always|collapseActionView\"\n"
                        + "            android:actionLayout=\"@layout/action_table_of_contents\" />\n"
                        + "\n"
                        + "        <item\n"
                        + "            android:id=\"@+id/menu_settings\"\n"
                        + "            android:showAsAction=\"always|collapseActionView\" />\n"
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
                        + "            android:showAsAction=\"always|collapseActionView\"\n"
                        + "            android:actionLayout=\"@layout/action_table_of_contents\" />\n"
                        + "\n"
                        + "        <item android:id=\"@+id/menu_search_exit\"\n"
                        + "              android:showAsAction=\"never\" />\n"
                        + "\n"
                        + "    </group>\n"
                        + "\n"
                        + "</menu>\n"))
                .run()
                .expect(expected);
    }

    public void testJavaFail() throws Exception {
        String expected = ""
                + "src/test/pkg/ActionTest1.java:7: Warning: Prefer \"SHOW_AS_ACTION_IF_ROOM\" instead of \"SHOW_AS_ACTION_ALWAYS\" [AlwaysShowAction]\n"
                + "        System.out.println(MenuItem.SHOW_AS_ACTION_ALWAYS);\n"
                + "                                    ~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";
        lint().files(
                // Only references to ALWAYS
                mActionTest1)
                .run()
                .expect(expected);
    }

    public void testJavaPass() throws Exception {
        //noinspection all // Sample code
        lint().files(
                // Both references to ALWAYS and IF_ROOM
                mActionTest1,
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.view.MenuItem;\n"
                        + "\n"
                        + "public class ActionTest2 {\n"
                        + "    public void foo() {\n"
                        + "        System.out.println(MenuItem.SHOW_AS_ACTION_IF_ROOM);\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expectClean();
    }

    public void testSuppress() throws Exception {
        //noinspection all // Sample code
        lint().files(
                xml("res/menu-land/actions2_ignore.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "\n"
                        + "    <item\n"
                        + "        android:id=\"@+id/menu_search\"\n"
                        + "        android:showAsAction=\"always|collapseActionView\"\n"
                        + "        android:actionViewClass=\"android.widget.SearchView\"\n"
                        + "        tools:ignore=\"AlwaysShowAction\" />\n"
                        + "\n"
                        + "    <group android:id=\"@+id/reader_items\">\n"
                        + "\n"
                        + "        <item\n"
                        + "            android:id=\"@+id/menu_table_of_contents\"\n"
                        + "            android:showAsAction=\"always|collapseActionView\"\n"
                        + "            android:actionLayout=\"@layout/action_table_of_contents\" />\n"
                        + "\n"
                        + "        <item\n"
                        + "            android:id=\"@+id/menu_settings\"\n"
                        + "            android:showAsAction=\"always|collapseActionView\" />\n"
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
                        + "            android:showAsAction=\"always|collapseActionView\"\n"
                        + "            android:actionLayout=\"@layout/action_table_of_contents\" />\n"
                        + "\n"
                        + "        <item android:id=\"@+id/menu_search_exit\"\n"
                        + "              android:showAsAction=\"never\" />\n"
                        + "\n"
                        + "    </group>\n"
                        + "\n"
                        + "</menu>\n"),
                    java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import android.view.MenuItem;\n"
                            + "\n"
                            + "public class ActionTest1 {\n"
                            + "    @android.annotation.SuppressLint(\"AlwaysShowAction\")\n"
                            + "    public void foo() {\n"
                            + "        System.out.println(MenuItem.SHOW_AS_ACTION_ALWAYS);\n"
                            + "    }\n"
                            + "}\n"))
                .run()
                .expectClean();
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mActionTest1 = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.view.MenuItem;\n"
            + "\n"
            + "public class ActionTest1 {\n"
            + "    public void foo() {\n"
            + "        System.out.println(MenuItem.SHOW_AS_ACTION_ALWAYS);\n"
            + "    }\n"
            + "}\n");
}
