/*
 * Copyright (C) 2013 The Android Open Source Project
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
public class LayoutConsistencyDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new LayoutConsistencyDetector();
    }

    public void test() throws Exception {
        String expected = ""
                + "res/layout/layout1.xml:11: Warning: The id \"button1\" in layout \"layout1\" is missing from the following layout configurations: layout-xlarge (present in layout) [InconsistentLayout]\n"
                + "        android:id=\"@+id/button1\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/layout1.xml:38: Warning: The id \"button4\" in layout \"layout1\" is missing from the following layout configurations: layout-xlarge (present in layout) [InconsistentLayout]\n"
                + "        android:id=\"@+id/button4\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n";

        lint().files(
                mFoo,
                mLayout1,
                mLayout2)
                .allowCompilationErrors()
                .run()
                .expect(expected);
    }

    public void testSuppress() throws Exception {
        // Same as unit test above, but button1 is suppressed with tools:ignore; button4 is not
        String expected = ""
                + "res/layout/layout1.xml:56: Warning: The id \"button4\" in layout \"layout1\" is missing from the following layout configurations: layout-xlarge (present in layout) [InconsistentLayout]\n"
                + "        android:id=\"@+id/button4\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        lint().files(
                mFoo,
                xml("res/layout/layout1.xml", ""
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
                        + "\n"
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
                        + "        tools:ignore=\"InconsistentLayout\"/>\n"
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
                        + "</RelativeLayout>\n"),
                mLayout2)
                .allowCompilationErrors()
                .run()
                .expect(expected);
    }

    public void test2() throws Exception {
        String expected = ""
                + "res/layout/layout1.xml:11: Warning: The id \"button1\" in layout \"layout1\" is missing from the following layout configurations: layout-xlarge (present in layout, layout-sw600dp, layout-sw600dp-land) [InconsistentLayout]\n"
                + "        android:id=\"@+id/button1\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/layout-sw600dp/layout1.xml:11: Occurrence in layout-sw600dp\n"
                + "    res/layout-sw600dp-land/layout1.xml:11: Occurrence in layout-sw600dp-land\n"
                + "res/layout/layout1.xml:38: Warning: The id \"button4\" in layout \"layout1\" is missing from the following layout configurations: layout-xlarge (present in layout, layout-sw600dp, layout-sw600dp-land) [InconsistentLayout]\n"
                + "        android:id=\"@+id/button4\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    res/layout-sw600dp/layout1.xml:38: Occurrence in layout-sw600dp\n"
                + "    res/layout-sw600dp-land/layout1.xml:38: Occurrence in layout-sw600dp-land\n"
                + "0 errors, 2 warnings\n";
        lint().files(
                mFoo,
                mLayout1,
                mLayout1_class,
                mLayout1_class2,
                mLayout2)
                .allowCompilationErrors()
                .run()
                .expect(expected);
    }

    public void test3() throws Exception {
        String expected = ""
                + "res/layout/layout1.xml:11: Warning: The id \"button1\" in layout \"layout1\" is only present in the following layout configurations: layout (missing from layout-sw600dp, layout-sw600dp-land, layout-xlarge) [InconsistentLayout]\n"
                + "        android:id=\"@+id/button1\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/layout1.xml:38: Warning: The id \"button4\" in layout \"layout1\" is only present in the following layout configurations: layout (missing from layout-sw600dp, layout-sw600dp-land, layout-xlarge) [InconsistentLayout]\n"
                + "        android:id=\"@+id/button4\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n";
        lint().files(
                mFoo,
                mLayout1,
                mLayout2_class,
                mLayout2_class2,
                mLayout2)
                .allowCompilationErrors()
                .run()
                .expect(expected);
    }

    public void testNoJavaRefs() throws Exception {
        lint().files(
                mLayout1,
                mLayout2)
                .allowCompilationErrors()
                .run()
                .expectClean();
    }

    public void testSkipIgnoredProjects() throws Exception {
        // https://code.google.com/p/android/issues/detail?id=227098
        // We shouldn't flag issues in non-reporting projects
        lint().projects(project(
                mFoo,
                mLayout1,
                mLayout2)
                .report(false))
                .allowCompilationErrors()
                .run()
                .expectClean();
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mFoo = java(""
            + "package test.pkg;\n"
            + "public class X {\n"
            + "  public void X(Y parent) {\n"
            + "    parent.foo(R.id.button1);\n"
            + "    parent.foo(R.id.button4);\n"
            + "  }\n"
            + "}\n");

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
    private TestFile mLayout1_class = xml("res/layout-sw600dp/layout1.xml", ""
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
    private TestFile mLayout1_class2 = xml("res/layout-sw600dp-land/layout1.xml", ""
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
    private TestFile mLayout2 = xml("res/layout-xlarge/layout1.xml", ""
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

    @SuppressWarnings("all") // Sample code
    private TestFile mLayout2_class = xml("res/layout-sw600dp/layout1.xml", ""
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

    @SuppressWarnings("all") // Sample code
    private TestFile mLayout2_class2 = xml("res/layout-sw600dp-land/layout1.xml", ""
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
