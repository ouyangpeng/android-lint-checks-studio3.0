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
public class UselessViewDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new UselessViewDetector();
    }

    public void testUseless() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/useless.xml:85: Warning: This FrameLayout view is useless (no children, no background, no id, no style) [UselessLeaf]\n"
                + "    <FrameLayout\n"
                + "    ^\n"
                + "res/layout/useless.xml:13: Warning: This LinearLayout layout or its FrameLayout parent is useless [UselessParent]\n"
                + "        <LinearLayout\n"
                + "        ^\n"
                + "res/layout/useless.xml:47: Warning: This LinearLayout layout or its FrameLayout parent is useless; transfer the background attribute to the other view [UselessParent]\n"
                + "        <LinearLayout\n"
                + "        ^\n"
                + "res/layout/useless.xml:65: Warning: This LinearLayout layout or its FrameLayout parent is useless; transfer the background attribute to the other view [UselessParent]\n"
                + "        <LinearLayout\n"
                + "        ^\n"
                + "0 errors, 4 warnings\n",
            lintFiles(xml("res/layout/useless.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<merge xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\" >\n"
                            + "\n"
                            + "    <!-- Neither parent nor child define background: delete is okay -->\n"
                            + "\n"
                            + "    <FrameLayout\n"
                            + "        android:id=\"@+id/LinearLayout\"\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"match_parent\" >\n"
                            + "\n"
                            + "        <LinearLayout\n"
                            + "            android:layout_width=\"match_parent\"\n"
                            + "            android:layout_height=\"match_parent\" >\n"
                            + "\n"
                            + "            <TextView\n"
                            + "                android:layout_width=\"wrap_content\"\n"
                            + "                android:layout_height=\"wrap_content\" />\n"
                            + "        </LinearLayout>\n"
                            + "    </FrameLayout>\n"
                            + "\n"
                            + "    <!-- Both define background: cannot be deleted -->\n"
                            + "\n"
                            + "    <FrameLayout\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"match_parent\"\n"
                            + "        android:background=\"@drawable/bg\" >\n"
                            + "\n"
                            + "        <LinearLayout\n"
                            + "            android:layout_width=\"match_parent\"\n"
                            + "            android:layout_height=\"match_parent\"\n"
                            + "            android:background=\"@drawable/bg\" >\n"
                            + "\n"
                            + "            <TextView\n"
                            + "                android:layout_width=\"wrap_content\"\n"
                            + "                android:layout_height=\"wrap_content\" />\n"
                            + "        </LinearLayout>\n"
                            + "    </FrameLayout>\n"
                            + "\n"
                            + "    <!-- Only child defines background: delete is okay -->\n"
                            + "\n"
                            + "    <FrameLayout\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"match_parent\" >\n"
                            + "\n"
                            + "        <LinearLayout\n"
                            + "            android:layout_width=\"match_parent\"\n"
                            + "            android:layout_height=\"match_parent\"\n"
                            + "            android:background=\"@drawable/bg\" >\n"
                            + "\n"
                            + "            <TextView\n"
                            + "                android:layout_width=\"wrap_content\"\n"
                            + "                android:layout_height=\"wrap_content\" />\n"
                            + "        </LinearLayout>\n"
                            + "    </FrameLayout>\n"
                            + "\n"
                            + "    <!-- Only parent defines background: delete is okay -->\n"
                            + "\n"
                            + "    <FrameLayout\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"match_parent\"\n"
                            + "        android:background=\"@drawable/bg\" >\n"
                            + "\n"
                            + "        <LinearLayout\n"
                            + "            android:layout_width=\"match_parent\"\n"
                            + "            android:layout_height=\"match_parent\" >\n"
                            + "\n"
                            + "            <TextView\n"
                            + "                android:layout_width=\"wrap_content\"\n"
                            + "                android:layout_height=\"wrap_content\" />\n"
                            + "        </LinearLayout>\n"
                            + "    </FrameLayout>\n"
                            + "\n"
                            + "    <!-- Leaf cannot be deleted because it has a background -->\n"
                            + "\n"
                            + "    <FrameLayout\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"match_parent\"\n"
                            + "        android:background=\"@drawable/bg\" >\n"
                            + "    </FrameLayout>\n"
                            + "\n"
                            + "    <!-- Useless leaf -->\n"
                            + "\n"
                            + "    <FrameLayout\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"match_parent\" >\n"
                            + "    </FrameLayout>\n"
                            + "</merge>\n")));
    }

    public void testTabHost() throws Exception {
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintFiles(xml("res/layout/useless2.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<TabHost xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\" >\n"
                            + "\n"
                            + "    <LinearLayout\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"match_parent\"\n"
                            + "        android:orientation=\"vertical\" >\n"
                            + "\n"
                            + "        <TabWidget\n"
                            + "            android:layout_width=\"match_parent\"\n"
                            + "            android:layout_height=\"wrap_content\" />\n"
                            + "\n"
                            + "        <FrameLayout\n"
                            + "            android:layout_width=\"match_parent\"\n"
                            + "            android:layout_height=\"0px\"\n"
                            + "            android:layout_weight=\"1\" >\n"
                            + "\n"
                            + "            <Button\n"
                            + "                android:layout_width=\"wrap_content\"\n"
                            + "                android:layout_height=\"wrap_content\" />\n"
                            + "        </FrameLayout>\n"
                            + "    </LinearLayout>\n"
                            + "\n"
                            + "</TabHost>\n")));
    }

    public void testStyleAttribute() throws Exception {
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintFiles(xml("res/layout/useless3.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<TableRow\n"
                            + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    style=\"@style/keyboard_table_row\">\n"
                            + "</TableRow>\n")));
    }

    public void testUselessLeafRoot() throws Exception {
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintFiles(xml("res/layout/breadcrumbs_in_fragment.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    android:layout_width=\"0dip\"\n"
                            + "    android:layout_height=\"0dip\"\n"
                            + "    android:visibility=\"gone\" />\n")));
    }

    public void testUseless65519() throws Exception {
        // https://code.google.com/p/android/issues/detail?id=65519
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",

                lintFiles(xml("res/layout/useless4.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "\n"
                            + "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "             android:layout_width=\"match_parent\"\n"
                            + "             android:layout_height=\"match_parent\"\n"
                            + "             android:background=\"@drawable/detail_panel_counter_bg\">\n"
                            + "\n"
                            + "    <LinearLayout\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:padding=\"5dp\">\n"
                            + "\n"
                            + "        <View\n"
                            + "            android:layout_width=\"5dp\"\n"
                            + "            android:layout_height=\"5dp\" />\n"
                            + "\n"
                            + "        <View\n"
                            + "            android:layout_width=\"5dp\"\n"
                            + "            android:layout_height=\"5dp\" />\n"
                            + "    </LinearLayout>\n"
                            + "</FrameLayout>\n")));
    }

    public void testUselessWithPaddingAttrs() throws Exception {
        // https://code.google.com/p/android/issues/detail?id=205250
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/useless5.xml:7: Warning: This RelativeLayout layout or its FrameLayout parent is useless [UselessParent]\n"
                + "    <RelativeLayout\n"
                + "    ^\n"
                + "0 errors, 1 warnings\n",

                lintFiles(xml("res/layout/useless5.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "\n"
                            + "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "             android:layout_width=\"match_parent\"\n"
                            + "             android:layout_height=\"wrap_content\">\n"
                            + "\n"
                            + "    <RelativeLayout\n"
                            + "            android:layout_width=\"match_parent\"\n"
                            + "            android:layout_height=\"wrap_content\"\n"
                            + "            android:paddingBottom=\"16dp\"\n"
                            + "            android:paddingLeft=\"16dp\"\n"
                            + "            android:paddingRight=\"16dp\"\n"
                            + "            android:paddingTop=\"16dp\">\n"
                            + "\n"
                            + "        <TextView\n"
                            + "                android:layout_width=\"wrap_content\"\n"
                            + "                android:layout_height=\"wrap_content\"/>\n"
                            + "    </RelativeLayout>\n"
                            + "</FrameLayout>\n")));
    }

    public void testUselessParentWithStyleAttribute() throws Exception {
        assertEquals("No warnings.",
                lintProject(xml("res/layout/my_layout.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout\n"
                        + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "    android:orientation=\"vertical\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:background=\"@color/header\">\n"
                        + "  <!-- The FrameLayout acts as grey header border around the searchbox -->\n"
                        + "  <FrameLayout style=\"@style/Header.SearchBox\">\n"
                        + "    <!-- This is an editable form of @layout/search_field_unedittable -->\n"
                        + "    <LinearLayout\n"
                        + "        android:orientation=\"horizontal\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        style=\"@style/SearchBox\">\n"
                        + "      <TextView\n"
                        + "          android:id=\"@+id/search_prefix\"\n"
                        + "          style=\"@style/SearchBoxText.Prefix\"\n"
                        + "          tools:text=\"From:\"/>\n"
                        + "      <EditText\n"
                        + "          android:id=\"@+id/search_query\"\n"
                        + "          android:layout_width=\"match_parent\"\n"
                        + "          android:layout_height=\"wrap_content\"\n"
                        + "          android:singleLine=\"true\"\n"
                        + "          style=\"@style/SearchBoxText\"/>\n"
                        + "    </LinearLayout>\n"
                        + "  </FrameLayout>\n"
                        + "</LinearLayout>")));
    }

    public void testDataBinding() {
        // Regression test for 37140356
        lint().files(
                xml("res/layout/layout.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<FrameLayout \n"
                        + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:app=\"http://schemas.android.com/apk/res/foo.bar.baz\"\n"
                        + "             android:layout_width=\"match_parent\"\n"
                        + "             android:layout_height=\"wrap_content\">\n"
                        + "        <LinearLayout\n"
                        + "            android:layout_width=\"match_parent\" \n"
                        + "            android:layout_height=\"wrap_content\" \n"
                        + "            android:orientation=\"vertical\" \n"
                        + "            app:viewModel=\"@{viewModel}\" /> "
                        + "</FrameLayout>\n"))
                .run()
                .expectClean();
    }
}
