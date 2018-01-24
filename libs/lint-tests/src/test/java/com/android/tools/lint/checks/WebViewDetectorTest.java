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
public class WebViewDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new WebViewDetector();
    }

    public void testMatchParentWidth() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/webview.xml:19: Error: Placing a <WebView> in a parent element that uses a wrap_content layout_height can lead to subtle bugs; use match_parent instead [WebViewLayout]\n"
                + "        <WebView\n"
                + "        ^\n"
                + "    res/layout/webview.xml:16: wrap_content here may not work well with WebView below\n"
                + "1 errors, 0 warnings\n",

                lintFiles(xml("res/layout/webview.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "    android:orientation=\"vertical\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\">\n"
                            + "\n"
                            + "    <!-- OK -->\n"
                            + "    <WebView\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\" />\n"
                            + "\n"
                            + "    <LinearLayout\n"
                            + "        android:orientation=\"vertical\"\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"wrap_content\">\n"
                            + "\n"
                            + "        <!-- Report error that parent height is wrap_content -->\n"
                            + "        <WebView\n"
                            + "            android:layout_width=\"match_parent\"\n"
                            + "            android:layout_height=\"match_parent\" />\n"
                            + "\n"
                            + "        <!-- Suppressed -->\n"
                            + "        <WebView\n"
                            + "            android:layout_width=\"match_parent\"\n"
                            + "            android:layout_height=\"match_parent\"\n"
                            + "            tools:ignore=\"WebViewLayout\" />\n"
                            + "    </LinearLayout>\n"
                            + "\n"
                            + "</LinearLayout>\n")));
    }

    public void testMatchParentHeight() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/webview2.xml:20: Error: Placing a <WebView> in a parent element that uses a wrap_content layout_width can lead to subtle bugs; use match_parent instead [WebViewLayout]\n"
                + "        <WebView\n"
                + "        ^\n"
                + "    res/layout/webview2.xml:16: wrap_content here may not work well with WebView below\n"
                + "1 errors, 0 warnings\n",

                lintFiles(xml("res/layout/webview2.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<!-- Like webview.xml, but with a wrap on the height instead -->\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "              xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "              android:orientation=\"vertical\"\n"
                            + "              android:layout_width=\"match_parent\"\n"
                            + "              android:layout_height=\"match_parent\">\n"
                            + "\n"
                            + "    <!-- OK -->\n"
                            + "    <WebView\n"
                            + "            android:layout_width=\"wrap_content\"\n"
                            + "            android:layout_height=\"wrap_content\"/>\n"
                            + "\n"
                            + "    <LinearLayout\n"
                            + "            android:orientation=\"vertical\"\n"
                            + "            android:layout_width=\"wrap_content\"\n"
                            + "            android:layout_height=\"fill_parent\">\n"
                            + "\n"
                            + "        <!-- Report error that parent height is wrap_content -->\n"
                            + "        <WebView\n"
                            + "                android:layout_width=\"match_parent\"\n"
                            + "                android:layout_height=\"match_parent\"/>\n"
                            + "\n"
                            + "        <!-- Suppressed -->\n"
                            + "        <WebView\n"
                            + "                android:layout_width=\"match_parent\"\n"
                            + "                android:layout_height=\"match_parent\"\n"
                            + "                tools:ignore=\"WebViewLayout\"/>\n"
                            + "    </LinearLayout>\n"
                            + "\n"
                            + "</LinearLayout>\n")));
    }
    public void testMissingLayoutHeight() throws Exception {
        // Regression test for
        //   https://code.google.com/p/android/issues/detail?id=74646
        //noinspection all // Sample code
        assertEquals("No warnings.",

                lintFiles(xml("res/layout/webview3.xml", ""
                            + "<!-- Note the lack of explicit 'layout_height' on root layout; it comes from the app's theme -->\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "  android:layout_width=\"match_parent\"\n"
                            + "  android:orientation=\"vertical\">\n"
                            + "\n"
                            + "    <!-- other views can go here -->\n"
                            + "\n"
                            + "    <WebView\n"
                            + "      android:id=\"@+id/webview\"\n"
                            + "      android:layout_width=\"match_parent\"\n"
                            + "      android:layout_height=\"match_parent\" />\n"
                            + "\n"
                            + "</LinearLayout>\n")));
    }
}
