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
public class WrongIdDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new WrongIdDetector();
    }

    public void test() throws Exception {
        assertEquals(""
                + "res/layout/layout1.xml:14: Error: The id \"button5\" is not defined anywhere. Did you mean one of {button1, button2, button3, button4} ? [UnknownId]\n"
                + "        android:layout_alignBottom=\"@+id/button5\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/layout1.xml:17: Error: The id \"my_id3\" is not defined anywhere. Did you mean my_id2 ? [UnknownId]\n"
                + "        android:layout_alignRight=\"@+id/my_id3\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/layout1.xml:18: Error: The id \"my_id1\" is defined but not assigned to any views. Did you mean my_id2 ? [UnknownId]\n"
                + "        android:layout_alignTop=\"@+id/my_id1\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/layout1.xml:15: Warning: The id \"my_id2\" is not referring to any views in this layout [UnknownIdInLayout]\n"
                + "        android:layout_alignLeft=\"@+id/my_id2\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "3 errors, 1 warnings\n",

            lintProject(
                    mLayout1,
                    mLayout2,
                    mIds
        ));
    }

    public void testSingleFile() throws Exception {
        assertEquals(""
                + "res/layout/layout1.xml:14: Warning: The id \"button5\" is not referring to any views in this layout [UnknownIdInLayout]\n"
                + "        android:layout_alignBottom=\"@+id/button5\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/layout1.xml:15: Warning: The id \"my_id2\" is not referring to any views in this layout [UnknownIdInLayout]\n"
                + "        android:layout_alignLeft=\"@+id/my_id2\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/layout1.xml:17: Warning: The id \"my_id3\" is not referring to any views in this layout [UnknownIdInLayout]\n"
                + "        android:layout_alignRight=\"@+id/my_id3\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/layout1.xml:18: Warning: The id \"my_id1\" is not referring to any views in this layout [UnknownIdInLayout]\n"
                + "        android:layout_alignTop=\"@+id/my_id1\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 4 warnings\n",

            lintFiles(mLayout1));
    }

    public void testSuppressed() throws Exception {
        assertEquals(
            "No warnings.",

            lintProject(
                    mIgnorelayout1,
                    mLayout2,
                    mIds
        ));
    }

    public void testSuppressedSingleFile() throws Exception {
        assertEquals(
            "No warnings.",

            lintFiles(mIgnorelayout1));
    }

    public void testNewIdPrefix() throws Exception {
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",

                lintFiles(xml("res/layout/default_item_badges.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    android:id=\"@+id/video_badges\"\n"
                            + "    android:layout_width=\"wrap_content\"\n"
                            + "    android:layout_height=\"wrap_content\" />\n"),
                          xml("res/layout/detailed_item.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<RelativeLayout\n"
                            + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"wrap_content\" >\n"
                            + "\n"
                            + "    <FrameLayout\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:layout_below=\"@id/video_badges\" />\n"
                            + "\n"
                            + "</RelativeLayout>\n")));
    }

    public void testSiblings() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/siblings.xml:55: Error: @id/button5 is not a sibling in the same RelativeLayout [NotSibling]\n"
                + "        android:layout_alignTop=\"@id/button5\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/siblings.xml:56: Error: @id/button6 is not a sibling in the same RelativeLayout [NotSibling]\n"
                + "        android:layout_toRightOf=\"@id/button6\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/siblings.xml:63: Error: @+id/button5 is not a sibling in the same RelativeLayout [NotSibling]\n"
                + "        android:layout_alignTop=\"@+id/button5\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/siblings.xml:64: Error: @+id/button6 is not a sibling in the same RelativeLayout [NotSibling]\n"
                + "        android:layout_toRightOf=\"@+id/button6\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "4 errors, 0 warnings\n",

                lintFiles(xml("res/layout/siblings.xml", ""
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
                            + "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\" >\n"
                            + "\n"
                            + "    <Button\n"
                            + "        android:id=\"@+id/button4\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:layout_alignParentLeft=\"true\"\n"
                            + "        android:layout_alignParentTop=\"true\"\n"
                            + "        android:text=\"Button\" />\n"
                            + "\n"
                            + "    <LinearLayout\n"
                            + "        android:id=\"@+id/linearLayout1\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:layout_below=\"@+id/button4\"\n"
                            + "        android:layout_toRightOf=\"@+id/button4\"\n"
                            + "        android:orientation=\"vertical\" >\n"
                            + "\n"
                            + "        <Button\n"
                            + "            android:id=\"@+id/button5\"\n"
                            + "            android:layout_width=\"wrap_content\"\n"
                            + "            android:layout_height=\"wrap_content\"\n"
                            + "            android:text=\"Button\" />\n"
                            + "\n"
                            + "        <Button\n"
                            + "            android:id=\"@id/button6\"\n"
                            + "            android:layout_width=\"wrap_content\"\n"
                            + "            android:layout_height=\"wrap_content\"\n"
                            + "            android:text=\"Button\" />\n"
                            + "    </LinearLayout>\n"
                            + "\n"
                            + "    <Button\n"
                            + "        android:id=\"@+id/button7\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:layout_alignTop=\"@id/button5\"\n"
                            + "        android:layout_toRightOf=\"@id/button6\"\n"
                            + "        android:text=\"Button\" />\n"
                            + "\n"
                            + "    <Button\n"
                            + "        android:id=\"@+id/button8\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:layout_alignTop=\"@+id/button5\"\n"
                            + "        android:layout_toRightOf=\"@+id/button6\"\n"
                            + "        android:text=\"Button\" />\n"
                            + "\n"
                            + "</RelativeLayout>\n")));
    }

    public void testInvalidIds1() throws Exception {
        // See https://code.google.com/p/android/issues/detail?id=56029
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/invalid_ids.xml:23: Error: ID definitions must be of the form @+id/name; try using @+id/menu_Reload [InvalidId]\n"
                + "        android:id=\"@+menu/Reload\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/invalid_ids.xml:31: Error: ID definitions must be of the form @+id/name; try using @+id/_id_foo [InvalidId]\n"
                + "        android:id=\"@+/id_foo\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/invalid_ids.xml:37: Error: ID definitions must be of the form @+id/name; try using @+id/myid_button5 [InvalidId]\n"
                + "            android:id=\"@+myid/button5\"\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/invalid_ids.xml:43: Error: ID definitions must be of the form @+id/name; try using @+id/string_whatevs [InvalidId]\n"
                + "            android:id=\"@+string/whatevs\"\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "4 errors, 0 warnings\n",

                lintFiles(xml("res/layout/invalid_ids.xml", ""
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
                            + "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\" >\n"
                            + "\n"
                            + "    <Button\n"
                            + "        android:id=\"@+menu/Reload\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:layout_alignParentLeft=\"true\"\n"
                            + "        android:layout_alignParentTop=\"true\"\n"
                            + "        android:text=\"Button\" />\n"
                            + "\n"
                            + "    <LinearLayout\n"
                            + "        android:id=\"@+/id_foo\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:orientation=\"vertical\" >\n"
                            + "\n"
                            + "        <Button\n"
                            + "            android:id=\"@+myid/button5\"\n"
                            + "            android:layout_width=\"wrap_content\"\n"
                            + "            android:layout_height=\"wrap_content\"\n"
                            + "            android:text=\"Button\" />\n"
                            + "\n"
                            + "        <Button\n"
                            + "            android:id=\"@+string/whatevs\"\n"
                            + "            android:layout_width=\"wrap_content\"\n"
                            + "            android:layout_height=\"wrap_content\"\n"
                            + "            android:text=\"Button\" />\n"
                            + "    </LinearLayout>\n"
                            + "\n"
                            + "</RelativeLayout>\n")));
    }

    public void testInvalidIds2() throws Exception {
        // https://code.google.com/p/android/issues/detail?id=65244
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/invalid_ids2.xml:8: Error: ID definitions must be of the form @+id/name; try using @+id/btn_skip [InvalidId]\n"
                + "        android:id=\"@+id/btn/skip\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/invalid_ids2.xml:16: Error: Invalid id: missing value [InvalidId]\n"
                + "        android:id=\"@+id/\"\n"
                + "        ~~~~~~~~~~~~~~~~~~\n"
                + "2 errors, 0 warnings\n",

                lintFiles(xml("res/layout/invalid_ids2.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\" >\n"
                            + "\n"
                            + "    <Button\n"
                            + "        android:id=\"@+id/btn/skip\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:layout_alignParentLeft=\"true\"\n"
                            + "        android:layout_alignParentTop=\"true\"\n"
                            + "        android:text=\"Button\" />\n"
                            + "\n"
                            + "    <Button\n"
                            + "        android:id=\"@+id/\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:text=\"Button\" />\n"
                            + "\n"
                            + "</RelativeLayout>\n")));
    }

    public void testIncremental() throws Exception {
        assertEquals(""
                + "res/layout/layout1.xml:14: Error: The id \"button5\" is not defined anywhere. Did you mean one of {button1, button2, button3, button4} ? [UnknownId]\n"
                + "        android:layout_alignBottom=\"@+id/button5\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/layout1.xml:17: Error: The id \"my_id3\" is not defined anywhere. Did you mean one of {my_id1, my_id2} ? [UnknownId]\n"
                + "        android:layout_alignRight=\"@+id/my_id3\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/layout1.xml:18: Error: The id \"my_id1\" is defined but not assigned to any views. Did you mean one of {my_id2, my_id3} ? [UnknownId]\n"
                + "        android:layout_alignTop=\"@+id/my_id1\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/layout1.xml:15: Warning: The id \"my_id2\" is not referring to any views in this layout [UnknownIdInLayout]\n"
                + "        android:layout_alignLeft=\"@+id/my_id2\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "3 errors, 1 warnings\n",

            lintProjectIncrementally(
                    "res/layout/layout1.xml",

                    mLayout1,
                    mLayout2,
                    mIds
            ));
    }

    public void testMissingNamespace() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=227687
        // Make sure we properly handle a missing namespace
        //noinspection all // Sample code
        assertEquals("No warnings.",

                lintFiles(xml("res/layout/layout3.xml", ""
                        + "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\" android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\" android:id=\"@+id/tv_portfolio_title\">\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        \n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        layout_below=\"@+id/tv_portfolio_title\"/>\n"
                        + "</RelativeLayout>\n"
                        + "\n")));
    }

    public void testSelfReference() throws Exception {
        // Make sure we highlight direct references to self
        // Regression test for https://code.google.com/p/android/issues/detail?id=136103
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/layout3.xml:9: Error: Cannot be relative to self: id=tv_portfolio_title, layout_below=tv_portfolio_title [NotSibling]\n"
                + "        android:layout_below=\"@+id/tv_portfolio_title\"/>\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

                lintFiles(xml("res/layout/layout3.xml", ""
                            + "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\" android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\" android:paddingLeft=\"@dimen/activity_horizontal_margin\">\n"
                            + "\n"
                            + "    <TextView\n"
                            + "        android:id=\"@+id/tv_portfolio_title\"\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:layout_below=\"@+id/tv_portfolio_title\"/>\n"
                            + "</RelativeLayout>\n"
                            + "\n")));
    }

    public void testPercent() throws Exception {
        assertEquals(""
                + "res/layout/test.xml:18: Error: The id \"textView1\" is not defined anywhere. Did you mean textview1 ? [UnknownId]\n"
                + "            android:layout_below=\"@id/textView1\"\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

                lintProject(xml("res/layout/test.xml", ""
                        + "<android.support.percent.PercentRelativeLayout "
                        + "     xmlns:android=\"http://schemas.android.com/apk/res/android\""
                        + "     xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                        + "     android:layout_width=\"match_parent\"\n"
                        + "     android:layout_height=\"match_parent\">\n"
                        + "        <View\n"
                        + "            android:id=\"@+id/textview1\"\n"
                        + "            android:layout_gravity=\"center\"\n"
                        + "            app:layout_widthPercent=\"50%\"\n"
                        + "            app:layout_heightPercent=\"50%\"/>\n"
                        + "        <View\n"
                        + "            android:id=\"@+id/textview2\"\n"
                        + "            android:layout_below=\"@id/textview1\"\n" // OK
                        + "            android:layout_width=\"wrap_content\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            app:layout_marginStartPercent=\"25%\"\n"
                        + "            app:layout_marginEndPercent=\"25%\"/>\n"
                        + "        <View\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            android:layout_below=\"@id/textView1\"\n" // WRONG (case)
                        + "            app:layout_widthPercent=\"60%\"/>\n"
                        + "\n"
                        + "</android.support.percent.PercentRelativeLayout>\n")));
    }

    public void testConstraintLayoutCycle() throws Exception {
        assertEquals(""
                + "res/layout/constraint.xml:21: Error: The id \"typo\" is not defined anywhere. [UnknownId]\n"
                + "        app:layout_constraintRight_toLeftOf=\"@+id/typo\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/constraint.xml:12: Error: Cannot be relative to self: id=button4, layout_constraintRight_toRightOf=button4 [NotSibling]\n"
                + "        app:layout_constraintRight_toRightOf=\"@+id/button4\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "2 errors, 0 warnings\n",
                lintProject(xml("res/layout/constraint.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<android.support.constraint.ConstraintLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                        + "    android:layout_width=\"800dp\" android:layout_height=\"1143dp\"\n"
                        + "    android:id=\"@+id/com.google.tnt.sherpa.ConstraintLayout\">\n"
                        + "\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:text=\"Button\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        app:layout_constraintRight_toRightOf=\"@+id/button4\"\n"
                        + "        app:layout_editor_absoluteX=\"24dp\"\n"
                        + "        app:layout_editor_absoluteY=\"26dp\"\n"
                        + "        android:id=\"@+id/button4\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:text=\"Button\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        app:layout_constraintRight_toLeftOf=\"@+id/typo\"\n"
                        + "        app:layout_editor_absoluteX=\"150dp\"\n"
                        + "        app:layout_editor_absoluteY=\"94dp\"\n"
                        + "        android:id=\"@+id/button5\" />\n"
                        + "</android.support.constraint.ConstraintLayout>")));
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mIds = xml("res/values/ids.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "\n"
            + "    <item name=\"my_id1\" type=\"id\"/>\n"
            + "\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mIgnorelayout1 = xml("res/layout/layout1.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
            + "    android:id=\"@+id/RelativeLayout1\"\n"
            + "    android:layout_width=\"fill_parent\"\n"
            + "    android:layout_height=\"fill_parent\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "    <!-- my_id1 is defined in ids.xml, my_id2 is defined in main2, my_id3 does not exist -->\n"
            + "\n"
            + "    <Button\n"
            + "        android:id=\"@+id/button1\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:layout_alignBottom=\"@+id/button5\"\n"
            + "        android:layout_alignLeft=\"@+id/my_id2\"\n"
            + "        android:layout_alignParentTop=\"true\"\n"
            + "        android:layout_alignRight=\"@+id/my_id3\"\n"
            + "        android:layout_alignTop=\"@+id/my_id1\"\n"
            + "        android:text=\"Button\"\n"
            + "        tools:ignore=\"UnknownIdInLayout,UnknownId\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:id=\"@+id/button2\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:layout_alignParentLeft=\"true\"\n"
            + "        android:layout_below=\"@+id/button1\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:id=\"@+id/button3\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:layout_alignParentLeft=\"true\"\n"
            + "        android:layout_below=\"@+id/button2\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:id=\"@+id/button4\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:layout_alignParentLeft=\"true\"\n"
            + "        android:layout_below=\"@+id/button3\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "</RelativeLayout>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mLayout1 = xml("res/layout/layout1.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:id=\"@+id/RelativeLayout1\"\n"
            + "    android:layout_width=\"fill_parent\"\n"
            + "    android:layout_height=\"fill_parent\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "    <!-- my_id1 is defined in ids.xml, my_id2 is defined in main2, my_id3 does not exist -->\n"
            + "\n"
            + "    <Button\n"
            + "        android:id=\"@+id/button1\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:layout_alignBottom=\"@+id/button5\"\n"
            + "        android:layout_alignLeft=\"@+id/my_id2\"\n"
            + "        android:layout_alignParentTop=\"true\"\n"
            + "        android:layout_alignRight=\"@+id/my_id3\"\n"
            + "        android:layout_alignTop=\"@+id/my_id1\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:id=\"@+id/button2\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:layout_alignParentLeft=\"true\"\n"
            + "        android:layout_below=\"@+id/button1\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:id=\"@+id/button3\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:layout_alignParentLeft=\"true\"\n"
            + "        android:layout_below=\"@+id/button2\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:id=\"@+id/button4\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:layout_alignParentLeft=\"true\"\n"
            + "        android:layout_below=\"@+id/button3\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "</RelativeLayout>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mLayout2 = xml("res/layout/layout2.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:layout_width=\"match_parent\"\n"
            + "    android:layout_height=\"match_parent\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "    <Button\n"
            + "        android:id=\"@+id/my_id2\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "</LinearLayout>\n");
}
