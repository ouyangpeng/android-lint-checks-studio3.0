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
public class UseCompoundDrawableDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new UseCompoundDrawableDetector();
    }

    public void testCompound() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/compound.xml:3: Warning: This tag and its children can be replaced by one <TextView/> and a compound drawable [UseCompoundDrawables]\n"
                + "<LinearLayout\n"
                + "^\n"
                + "0 errors, 1 warnings\n",
            lintFiles(xml("res/layout/compound.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "\n"
                            + "<LinearLayout\n"
                            + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\">\n"
                            + "\n"
                            + "    <ImageView\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\" />\n"
                            + "\n"
                            + "    <TextView\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\" />\n"
                            + "\n"
                            + "</LinearLayout>\n")));
    }

    public void testCompound2() throws Exception {
        // Ignore layouts that set a custom background
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",
                lintFiles(xml("res/layout/compound2.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "\n"
                            + "<LinearLayout\n"
                            + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    android:background=\"@android:drawable/ic_dialog_alert\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\">\n"
                            + "\n"
                            + "    <ImageView\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\" />\n"
                            + "\n"
                            + "    <TextView\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\" />\n"
                            + "\n"
                            + "</LinearLayout>\n")));
    }

    public void testCompound3() throws Exception {
        // Ignore layouts that set an image scale type
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",
                lintFiles(xml("res/layout/compound3.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "\n"
                            + "<LinearLayout\n"
                            + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\">\n"
                            + "\n"
                            + "    <ImageView\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:scaleType=\"fitStart\" />\n"
                            + "\n"
                            + "    <TextView\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\" />\n"
                            + "\n"
                            + "</LinearLayout>\n")));
    }
}
