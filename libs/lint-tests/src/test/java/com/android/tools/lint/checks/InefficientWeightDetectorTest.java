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
public class InefficientWeightDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new InefficientWeightDetector();
    }

    public void testWeights() {
        String expected = ""
                + "res/layout/inefficient_weight.xml:3: Error: Wrong orientation? No orientation specified, and the default is horizontal, yet this layout has multiple children where at least one has layout_width=\"match_parent\" [Orientation]\n"
                + "<LinearLayout\n"
                + "^\n"
                + "res/layout/inefficient_weight.xml:10: Warning: Use a layout_width of 0dp instead of match_parent for better performance [InefficientWeight]\n"
                + "     android:layout_width=\"match_parent\"\n"
                + "     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/inefficient_weight.xml:24: Warning: Use a layout_height of 0dp instead of wrap_content for better performance [InefficientWeight]\n"
                + "      android:layout_height=\"wrap_content\"\n"
                + "      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 2 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/inefficient_weight.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "\n"
                        + "<LinearLayout\n"
                        + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\">\n"
                        + "\n"
                        + "\t<Button\n"
                        + "\t    android:layout_width=\"match_parent\"\n"
                        + "\t    android:layout_height=\"wrap_content\"\n"
                        + "        android:layout_weight=\"1.0\" />\n"
                        + "\n"
                        + "\t<LinearLayout\n"
                        + "\t    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "\n"
                        + "\t    android:layout_width=\"match_parent\"\n"
                        + "\t    android:layout_height=\"match_parent\"\n"
                        + "\n"
                        + "\t\tandroid:orientation=\"vertical\">\n"
                        + "\n"
                        + "\t\t<Button\n"
                        + "\t\t    android:layout_width=\"match_parent\"\n"
                        + "\t\t    android:layout_height=\"wrap_content\"\n"
                        + "\t\t    android:layout_weight=\"1.0\" />\n"
                        + "\n"
                        + "\t</LinearLayout>\n"
                        + "\n"
                        + "\t<LinearLayout\n"
                        + "\t    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "\n"
                        + "\t    android:layout_width=\"match_parent\"\n"
                        + "\t    android:layout_height=\"match_parent\"\n"
                        + "\n"
                        + "\t\tandroid:orientation=\"vertical\">\n"
                        + "\n"
                        + "\t\t<Button\n"
                        + "\t\t    android:layout_width=\"match_parent\"\n"
                        + "\t\t    android:layout_height=\"0dip\"\n"
                        + "            android:layout_weight=\"1.0\" />\n"
                        + "\n"
                        + "\t</LinearLayout>\n"
                        + "\n"
                        + "    <LinearLayout\n"
                        + "            xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "            style=\"@style/MyStyle\"\n"
                        + "            android:layout_width=\"match_parent\"\n"
                        + "            android:layout_height=\"match_parent\">\n"
                        + "        <Button\n"
                        + "                android:layout_width=\"match_parent\"\n"
                        + "                android:layout_height=\"wrap_content\" />\n"
                        + "\n"
                        + "        <Button\n"
                        + "                android:layout_width=\"match_parent\"\n"
                        + "                android:layout_height=\"wrap_content\" />\n"
                        + "\n"
                        + "    </LinearLayout>\n"
                        + "\n"
                        + "\n"
                        + "</LinearLayout>\n"))
                .run()
                .expect(expected)
                .verifyFixes().window(1).expectFixDiffs(""
                + "Fix for res/layout/inefficient_weight.xml line 2: Set orientation=\"horizontal\" (default):\n"
                + "@@ -4 +4\n"
                + "      android:layout_width=\"match_parent\"\n"
                + "-     android:layout_height=\"match_parent\" >\n"
                + "+     android:layout_height=\"match_parent\"\n"
                + "+     android:orientation=\"horizontal\" >\n"
                + "  \n"
                + "Fix for res/layout/inefficient_weight.xml line 2: Set orientation=\"vertical\" (changes layout):\n"
                + "@@ -4 +4\n"
                + "      android:layout_width=\"match_parent\"\n"
                + "-     android:layout_height=\"match_parent\" >\n"
                + "+     android:layout_height=\"match_parent\"\n"
                + "+     android:orientation=\"vertical\" >\n"
                + "  \n");
    }

    public void testWeights2() {
        String expected = ""
                + "res/layout/nested_weights.xml:23: Warning: Nested weights are bad for performance [NestedWeights]\n"
                + "            android:layout_weight=\"1\"\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/nested_weights.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:orientation=\"horizontal\" >\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:id=\"@+id/button1\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <LinearLayout\n"
                        + "        android:id=\"@+id/linearLayout1\"\n"
                        + "        android:layout_weight=\"1\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"match_parent\" >\n"
                        + "\n"
                        + "        <Button\n"
                        + "            android:id=\"@+id/button3\"\n"
                        + "            android:layout_width=\"0dp\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:layout_weight=\"1\"\n"
                        + "            android:text=\"Button\" />\n"
                        + "    </LinearLayout>\n"
                        + "\n"
                        + "    <FrameLayout\n"
                        + "        android:id=\"@+id/frameLayout1\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:layout_weight=\"1\" >\n"
                        + "\n"
                        + "        <Button\n"
                        + "            android:id=\"@+id/button2\"\n"
                        + "            android:layout_width=\"wrap_content\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:text=\"Button\" />\n"
                        + "    </FrameLayout>\n"
                        + "\n"
                        + "</LinearLayout>\n"))
                .run()
                .expect(expected);
    }

    public void testWeights3() {
        String expected = ""
                + "res/layout/baseline_weights.xml:2: Warning: Set android:baselineAligned=\"false\" on this element for better performance [DisableBaselineAlignment]\n"
                + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "^\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/baseline_weights.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:orientation=\"horizontal\" >\n"
                        + "\n"
                        + "    <LinearLayout\n"
                        + "        android:id=\"@+id/linearLayout1\"\n"
                        + "        android:layout_weight=\"0.3\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"match_parent\"\n"
                        + "        android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "\n"
                        + "        <Button\n"
                        + "            android:id=\"@+id/button1\"\n"
                        + "            android:layout_width=\"wrap_content\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:text=\"Button\" />\n"
                        + "\n"
                        + "        <Button\n"
                        + "            android:id=\"@+id/button2\"\n"
                        + "            android:layout_width=\"wrap_content\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:text=\"Button\" />\n"
                        + "\n"
                        + "        <Button\n"
                        + "            android:id=\"@+id/button3\"\n"
                        + "            android:layout_width=\"wrap_content\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:text=\"Button\" />\n"
                        + "    </LinearLayout>\n"
                        + "\n"
                        + "    <FrameLayout\n"
                        + "        android:id=\"@+id/frameLayout1\"\n"
                        + "        android:layout_weight=\"0.7\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"match_parent\" >\n"
                        + "    </FrameLayout>\n"
                        + "\n"
                        + "</LinearLayout>\n"))
                .run()
                .expect(expected)
                .verifyFixes().window(1).expectFixDiffs(""
                + "Fix for res/layout/baseline_weights.xml line 1: Set baselineAligned=\"false\":\n"
                + "@@ -5 +5\n"
                + "      android:layout_height=\"match_parent\"\n"
                + "+     android:baselineAligned=\"false\"\n"
                + "      android:orientation=\"horizontal\" >\n");
    }

    public void testWeights4() {
        String expected = ""
                + "res/layout/activity_item_two_pane.xml:1: Warning: Set android:baselineAligned=\"false\" on this element for better performance [DisableBaselineAlignment]\n"
                + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "^\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/activity_item_two_pane.xml", ""
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:layout_marginLeft=\"16dp\"\n"
                        + "    android:layout_marginRight=\"16dp\"\n"
                        + "    android:divider=\"?android:attr/dividerHorizontal\"\n"
                        + "    android:orientation=\"horizontal\"\n"
                        + "    android:showDividers=\"middle\"\n"
                        + "    tools:context=\".ItemListActivity\" >\n"
                        + "\n"
                        + "    <!--\n"
                        + "    This layout is a two-pane layout for the Items\n"
                        + "    master/detail flow. See res/values-large/refs.xml and\n"
                        + "    res/values-sw600dp/refs.xml for an example of layout aliases\n"
                        + "    that replace the single-pane version of the layout with\n"
                        + "    this two-pane version.\n"
                        + "\n"
                        + "    For more on layout aliases, see:\n"
                        + "    http://developer.android.com/training/multiscreen/screensizes.html#TaskUseAliasFilters\n"
                        + "    -->\n"
                        + "\n"
                        + "    <fragment\n"
                        + "        android:id=\"@+id/item_list\"\n"
                        + "        android:name=\"com.example.master.ItemListFragment\"\n"
                        + "        android:layout_width=\"0dp\"\n"
                        + "        android:layout_height=\"match_parent\"\n"
                        + "        android:layout_weight=\"1\"\n"
                        + "        tools:layout=\"@android:layout/list_content\" />\n"
                        + "\n"
                        + "    <FrameLayout\n"
                        + "        android:id=\"@+id/item_detail_container\"\n"
                        + "        android:layout_width=\"0dp\"\n"
                        + "        android:layout_height=\"match_parent\"\n"
                        + "        android:layout_weight=\"3\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"))
                .run()
                .expect(expected);
    }

    public void testNoVerticalWeights3() {
        // Orientation=vertical
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/baseline_weights2.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "    <LinearLayout\n"
                        + "        android:id=\"@+id/linearLayout1\"\n"
                        + "        android:layout_weight=\"0.3\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"match_parent\"\n"
                        + "        android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "        <Button\n"
                        + "            android:id=\"@+id/button1\"\n"
                        + "            android:layout_width=\"wrap_content\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:text=\"Button\" />\n"
                        + "\n"
                        + "        <Button\n"
                        + "            android:id=\"@+id/button2\"\n"
                        + "            android:layout_width=\"wrap_content\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:text=\"Button\" />\n"
                        + "\n"
                        + "        <Button\n"
                        + "            android:id=\"@+id/button3\"\n"
                        + "            android:layout_width=\"wrap_content\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:text=\"Button\" />\n"
                        + "    </LinearLayout>\n"
                        + "\n"
                        + "    <FrameLayout\n"
                        + "        android:id=\"@+id/frameLayout1\"\n"
                        + "        android:layout_weight=\"0.7\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"match_parent\" >\n"
                        + "    </FrameLayout>\n"
                        + "\n"
                        + "</LinearLayout>\n"))
                .run()
                .expectClean();
    }

    public void testNoVerticalWeights4() {
        // Orientation not specified =⇒ horizontal
        String expected = ""
                + "res/layout/baseline_weights3.xml:2: Warning: Set android:baselineAligned=\"false\" on this element for better performance [DisableBaselineAlignment]\n"
                + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "^\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/baseline_weights3.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\" >\n"
                        + "\n"
                        + "    <LinearLayout\n"
                        + "        android:id=\"@+id/linearLayout1\"\n"
                        + "        android:layout_weight=\"0.3\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"match_parent\"\n"
                        + "        android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "        <Button\n"
                        + "            android:id=\"@+id/button1\"\n"
                        + "            android:layout_width=\"wrap_content\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:text=\"Button\" />\n"
                        + "\n"
                        + "        <Button\n"
                        + "            android:id=\"@+id/button2\"\n"
                        + "            android:layout_width=\"wrap_content\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:text=\"Button\" />\n"
                        + "\n"
                        + "        <Button\n"
                        + "            android:id=\"@+id/button3\"\n"
                        + "            android:layout_width=\"wrap_content\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:text=\"Button\" />\n"
                        + "    </LinearLayout>\n"
                        + "\n"
                        + "    <FrameLayout\n"
                        + "        android:id=\"@+id/frameLayout1\"\n"
                        + "        android:layout_weight=\"0.7\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"match_parent\" >\n"
                        + "    </FrameLayout>\n"
                        + "\n"
                        + "</LinearLayout>\n"))
                .run()
                .expect(expected);
    }

    public void testSuppressed() {
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/inefficient_weight2.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout\n"
                        + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\">\n"
                        + "\n"
                        + "       <SeekBar\n"
                        + "            android:id=\"@+id/seekbar\"\n"
                        + "            android:layout_width=\"fill_parent\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:layout_gravity=\"center_vertical\"\n"
                        + "            android:layout_weight=\"1\"\n"
                        + "            android:max=\"100\"\n"
                        + "            android:paddingBottom=\"10dip\"\n"
                        + "            android:paddingLeft=\"15dip\"\n"
                        + "            android:paddingRight=\"15dip\"\n"
                        + "            android:paddingTop=\"10dip\"\n"
                        + "            android:secondaryProgress=\"0\"\n"
                        + "            tools:ignore=\"InefficientWeight\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"))
                .run()
                .expectClean();
    }

    public void testNestedWeights() {
        // Regression test for http://code.google.com/p/android/issues/detail?id=22889
        // (Comment 8)
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/nested_weights2.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "   android:layout_width=\"fill_parent\"\n"
                        + "   android:layout_height=\"fill_parent\" >\n"
                        + "\n"
                        + "   <LinearLayout\n"
                        + "       android:layout_width=\"fill_parent\"\n"
                        + "       android:layout_height=\"fill_parent\"\n"
                        + "       android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "       <LinearLayout\n"
                        + "           android:layout_width=\"fill_parent\"\n"
                        + "           android:layout_height=\"wrap_content\"\n"
                        + "           android:orientation=\"horizontal\" >\n"
                        + "\n"
                        + "           <ImageView\n"
                        + "               android:layout_width=\"32dp\"\n"
                        + "               android:layout_height=\"32dp\"\n"
                        + "               android:layout_gravity=\"center_vertical\"\n"
                        + "               android:src=\"@drawable/launcher_icon\" />\n"
                        + "\n"
                        + "           <TextView\n"
                        + "               android:layout_width=\"0dp\"\n"
                        + "               android:layout_height=\"fill_parent\"\n"
                        + "               android:layout_gravity=\"center_vertical\"\n"
                        + "               android:layout_weight=\"1\"\n"
                        + "               android:text=\"test\" />\n"
                        + "       </LinearLayout>\n"
                        + "\n"
                        + "       <LinearLayout\n"
                        + "           android:layout_width=\"fill_parent\"\n"
                        + "           android:layout_weight=\"1\"\n"
                        + "           android:layout_height=\"0dp\"\n"
                        + "           android:orientation=\"vertical\" >\n"
                        + "       </LinearLayout>\n"
                        + "   </LinearLayout>\n"
                        + "\n"
                        + "</FrameLayout>\n"))
                .run()
                .expectClean();
    }

    public void testWrong0Dp() {
        String expected = ""
                + "res/layout/wrong0dp.xml:19: Error: Suspicious size: this will make the view invisible, should be used with layout_weight [Suspicious0dp]\n"
                + "            android:layout_width=\"0dp\"\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/wrong0dp.xml:25: Error: Suspicious size: this will make the view invisible, should be used with layout_weight [Suspicious0dp]\n"
                + "            android:layout_height=\"0dp\"\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/wrong0dp.xml:34: Error: Suspicious size: this will make the view invisible, probably intended for layout_height [Suspicious0dp]\n"
                + "            android:layout_width=\"0dp\"\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/wrong0dp.xml:67: Error: Suspicious size: this will make the view invisible, probably intended for layout_width [Suspicious0dp]\n"
                + "            android:layout_height=\"0dp\"\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/wrong0dp.xml:90: Error: Suspicious size: this will make the view invisible, probably intended for layout_width [Suspicious0dp]\n"
                + "            android:layout_height=\"0dp\"\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "5 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/wrong0dp.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:orientation=\"vertical\"\n"
                        + "    tools:ignore=\"HardcodedText\" >\n"
                        + "\n"
                        + "    <!-- Vertical Layout -->\n"
                        + "\n"
                        + "    <LinearLayout\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"match_parent\"\n"
                        + "        android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "        <!-- No weight: Always an error -->\n"
                        + "\n"
                        + "        <Button\n"
                        + "            android:layout_width=\"0dp\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:text=\"Button\" />\n"
                        + "\n"
                        + "        <Button\n"
                        + "            android:layout_width=\"wrap_content\"\n"
                        + "            android:layout_height=\"0dp\"\n"
                        + "            android:text=\"Button\" />\n"
                        + "\n"
                        + "        <!--\n"
                        + "             0dp not along the orientation axis is wrong;\n"
                        + "             here layout_height is okay, layout_width is not\n"
                        + "        -->\n"
                        + "\n"
                        + "        <Button\n"
                        + "            android:layout_width=\"0dp\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:layout_weight=\"1.0\"\n"
                        + "            android:text=\"Button\" />\n"
                        + "\n"
                        + "        <!-- OK -->\n"
                        + "\n"
                        + "        <Button\n"
                        + "            android:layout_width=\"wrap_content\"\n"
                        + "            android:layout_height=\"0dp\"\n"
                        + "            android:layout_weight=\"1.0\"\n"
                        + "            android:text=\"Button\" />\n"
                        + "    </LinearLayout>\n"
                        + "\n"
                        + "    <!-- Horizontal Layout -->\n"
                        + "\n"
                        + "    <LinearLayout\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"match_parent\"\n"
                        + "        android:orientation=\"horizontal\" >\n"
                        + "\n"
                        + "        <!-- OK -->\n"
                        + "\n"
                        + "        <Button\n"
                        + "            android:layout_width=\"0dp\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:layout_weight=\"1.0\"\n"
                        + "            android:text=\"Button\" />\n"
                        + "\n"
                        + "        <!-- Not OK -->\n"
                        + "\n"
                        + "        <Button\n"
                        + "            android:layout_width=\"wrap_content\"\n"
                        + "            android:layout_height=\"0dp\"\n"
                        + "            android:layout_weight=\"1.0\"\n"
                        + "            android:text=\"Button\" />\n"
                        + "    </LinearLayout>\n"
                        + "\n"
                        + "    <!-- No orientation specified, so horizontal -->\n"
                        + "\n"
                        + "    <LinearLayout\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"match_parent\" >\n"
                        + "\n"
                        + "        <!-- OK -->\n"
                        + "\n"
                        + "        <Button\n"
                        + "            android:layout_width=\"0dp\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:layout_weight=\"1.0\"\n"
                        + "            android:text=\"Button\" />\n"
                        + "\n"
                        + "        <!-- Not OK -->\n"
                        + "\n"
                        + "        <Button\n"
                        + "            android:layout_width=\"wrap_content\"\n"
                        + "            android:layout_height=\"0dp\"\n"
                        + "            android:layout_weight=\"1.0\"\n"
                        + "            android:text=\"Button\" />\n"
                        + "\n"
                        + "        <!-- Check suppressed -->\n"
                        + "\n"
                        + "        <Button\n"
                        + "            android:layout_width=\"0dp\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:text=\"Button\"\n"
                        + "            tools:ignore=\"Suspicious0dp\" />\n"
                        + "    </LinearLayout>\n"
                        + "\n"
                        + "</FrameLayout>\n"))
                .run()
                .expect(expected);
    }

    public void testOrientation() {
        String expected = ""
                + "res/layout/orientation.xml:52: Error: No orientation specified, and the default is horizontal. This is a common source of bugs when children are added dynamically. [Orientation]\n"
                + "    <LinearLayout\n"
                + "    ^\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/orientation.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<!--\n"
                        + "  ~ Copyright (C) 2013 The Android Open Source Project\n"
                        + "  ~\n"
                        + "  ~ Licensed under the Apache License, Version 2.0 (the \"License\");\n"
                        + "  ~ you may not use this file except in compliance with the License.\n"
                        + "  ~ You may obtain a copy of the License at\n"
                        + "  ~\n"
                        + "  ~      http://www.apache.org/licenses/LICENSE-2.0\n"
                        + "  ~\n"
                        + "  ~ Unless required by applicable law or agreed to in writing, software\n"
                        + "  ~ distributed under the License is distributed on an \"AS IS\" BASIS,\n"
                        + "  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
                        + "  ~ See the License for the specific language governing permissions and\n"
                        + "  ~ limitations under the License.\n"
                        + "  -->\n"
                        + "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    tools:ignore=\"HardcodedText\" >\n"
                        + "\n"
                        + "    <!-- OK: specifies orientation -->\n"
                        + "    <LinearLayout\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"match_parent\"\n"
                        + "        android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "    </LinearLayout>\n"
                        + "\n"
                        + "    <!-- OK: no id -->\n"
                        + "    <LinearLayout\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"match_parent\" >\n"
                        + "\n"
                        + "    </LinearLayout>\n"
                        + "\n"
                        + "    <!-- OK: has children -->\n"
                        + "    <LinearLayout\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"match_parent\" >\n"
                        + "\n"
                        + "        <Button\n"
                        + "            android:layout_width=\"0dp\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:layout_weight=\"1.0\"\n"
                        + "            android:text=\"Button\" />\n"
                        + "\n"
                        + "    </LinearLayout>\n"
                        + "\n"
                        + "    <!-- Error: Missing orientation -->\n"
                        + "    <LinearLayout\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"match_parent\"\n"
                        + "        android:id=\"@+id/mylayout\" >\n"
                        + "\n"
                        + "    </LinearLayout>\n"
                        + "\n"
                        + "</FrameLayout>\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for res/layout/orientation.xml line 51: Set orientation=\"horizontal\" (default):\n"
                        + "@@ -56 +56\n"
                        + "-         android:layout_height=\"match_parent\" >\n"
                        + "+         android:layout_height=\"match_parent\"\n"
                        + "+         android:orientation=\"horizontal\" >\n"
                        + "Fix for res/layout/orientation.xml line 51: Set orientation=\"vertical\" (changes layout):\n"
                        + "@@ -56 +56\n"
                        + "-         android:layout_height=\"match_parent\" >\n"
                        + "+         android:layout_height=\"match_parent\"\n"
                        + "+         android:orientation=\"vertical\" >\n");
    }

    public void testIncremental1() {
        String expected = ""
                + "res/layout/orientation2.xml:5: Error: No orientation specified, and the default is horizontal. This is a common source of bugs when children are added dynamically. [Orientation]\n"
                + "    <LinearLayout\n"
                + "    ^\n"
                + "1 errors, 0 warnings\n";
        lint().files(
                mOrientation2)
                .incremental("res/layout/orientation2.xml")
                .run()
                .expect(expected);
    }

    public void testIncremental2() {
        lint().files(
                mOrientation2,
                mStyles_inherited_orientation)
                .incremental("res/layout/orientation2.xml")
                .run()
                .expectClean();
    }

    public void testIncremental3() {
        lint().files(
                mOrientation2,
                mStyles_orientation)
                .incremental("res/layout/orientation2.xml")
                .run()
                .expectClean();
    }

    public void testIncremental4() {
        String expected = ""
                + "res/layout/inefficient_weight3.xml:9: Warning: Use a layout_height of 0dp instead of (undefined) for better performance [InefficientWeight]\n"
                + "    <Button\n"
                + "    ^\n"
                + "0 errors, 1 warnings\n";
        lint().files(
                mInefficient_weight3)
                .incremental("res/layout/inefficient_weight3.xml")
                .run()
                .expect(expected);
    }

    public void testIncremental5() {
        lint().files(
                mInefficient_weight3,
                mStyles_orientation)
                .incremental("res/layout/inefficient_weight3.xml")
                .run()
                .expectClean();
    }

    public void testIncremental6() {
        String expected = ""
                + "res/layout/inefficient_weight3.xml:9: Warning: Use a layout_height of 0dp instead of wrap_content for better performance [InefficientWeight]\n"
                + "    <Button\n"
                + "    ^\n"
                + "0 errors, 1 warnings\n";
        lint().files(
                mInefficient_weight3,
                mStyles_inherited_orientation)
                .incremental("res/layout/inefficient_weight3.xml")
                .run()
                .expect(expected);
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mInefficient_weight3 = xml("res/layout/inefficient_weight3.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "\n"
            + "<LinearLayout\n"
            + "        xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "        android:orientation=\"vertical\"\n"
            + "        android:layout_width=\"match_parent\"\n"
            + "        android:layout_height=\"match_parent\">\n"
            + "\n"
            + "    <Button\n"
            + "            style=\"@style/MyButtonStyle\"\n"
            + "            android:layout_width=\"match_parent\"\n"
            + "            android:layout_weight=\"1.0\"/>\n"
            + "\n"
            + "    <Button\n"
            + "            android:layout_width=\"match_parent\"\n"
            + "            android:layout_height=\"wrap_content\"/>\n"
            + "\n"
            + "</LinearLayout>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mOrientation2 = xml("res/layout/orientation2.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:layout_width=\"match_parent\"\n"
            + "    android:layout_height=\"match_parent\">\n"
            + "    <LinearLayout\n"
            + "        android:id=\"@+id/linear\"\n"
            + "        style=\"@style/Layout.Horizontal\" />\n"
            + "\n"
            + "</FrameLayout>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStyles_inherited_orientation = xml(
            "res/values/styles-inherited-orientation.xml", ""
                    + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                    + "    <style name=\"Layout\">\n"
                    + "        <item name=\"android:orientation\">vertical</item>\n"
                    + "    </style>\n"
                    + "\n"
                    + "    <style name=\"Layout.Horizontal\">\n"
                    + "        <item name=\"android:layout_width\">match_parent</item>\n"
                    + "        <item name=\"android:layout_height\">wrap_content</item>\n"
                    + "    </style>\n"
                    + "\n"
                    + "    <style name=\"MyButtonStyle\" parent=\"@style/Layout\">\n"
                    + "        <item name=\"android:layout_width\">match_parent</item>\n"
                    + "        <item name=\"android:layout_height\">wrap_content</item>\n"
                    + "    </style>\n"
                    + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStyles_orientation = xml("res/values/styles-orientation.xml", ""
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
            + "</resources>\n");
}
