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

import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.google.common.collect.Lists;
import java.util.List;

@SuppressWarnings("javadoc")
public class DetectMissingPrefixTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new DetectMissingPrefix();
    }

    public void test() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/namespace.xml:2: Error: Attribute is missing the Android namespace prefix [MissingPrefix]\n"
                + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" xmlns:other=\"http://foo.bar\" android:id=\"@+id/newlinear\" android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\" orientation=\"true\">\n"
                + "                                                                                                                                                                                                                                          ~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/namespace.xml:3: Error: Attribute is missing the Android namespace prefix [MissingPrefix]\n"
                + "    <Button style=\"@style/setupWizardOuterFrame\" android.text=\"Button\" android:id=\"@+id/button1\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"></Button>\n"
                + "                                                 ~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/namespace.xml:5: Error: Unexpected namespace prefix \"other\" found for tag LinearLayout [MissingPrefix]\n"
                + "    <LinearLayout other:orientation=\"horizontal\"/>\n"
                + "                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "3 errors, 0 warnings\n",

            lintFiles(xml("res/layout/namespace.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" xmlns:other=\"http://foo.bar\" android:id=\"@+id/newlinear\" android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\" orientation=\"true\">\n"
                            + "    <Button style=\"@style/setupWizardOuterFrame\" android.text=\"Button\" android:id=\"@+id/button1\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"></Button>\n"
                            + "    <ImageView android:style=\"@style/bogus\" android:id=\"@+id/android_logo\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                            + "    <LinearLayout other:orientation=\"horizontal\"/>\n"
                            + "</LinearLayout>\n"
                            + "\n")));
    }

    public void testCustomNamespace() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/namespace2.xml:9: Error: Attribute is missing the Android namespace prefix [MissingPrefix]\n"
                + "    orientation=\"true\">\n"
                + "    ~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintFiles(xml("res/layout/namespace2.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout \n"
                            + "    xmlns:customprefix=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:bogus=\"http://foo.com/bar\"\n"
                            + "    customprefix:id=\"@+id/newlinear\"\n"
                            + "    customprefix:layout_width=\"match_parent\"\n"
                            + "    customprefix:layout_height=\"match_parent\"\n"
                            + "    customprefix:orientation=\"vertical\"\n"
                            + "    orientation=\"true\">\n"
                            + "\n"
                            + "    <view class=\"foo.bar.LinearLayout\">\n"
                            + "        bogus:orientation=\"bogus\"\n"
                            + "    </view>\n"
                            + "\n"
                            + "    <foo.bar.LinearLayout\n"
                            + "        customprefix:id=\"@+id/newlinear2\"\n"
                            + "        customprefix:layout_width=\"match_parent\"\n"
                            + "        customprefix:layout_height=\"match_parent\"\n"
                            + "        customprefix:orientation=\"vertical\"\n"
                            + "        bogus:orientation=\"bogus\"\n"
                            + "        orientation=\"true\">\n"
                            + "\n"
                            + "        <view class=\"foo.bar.LinearLayout\">\n"
                            + "            bogus:orientation=\"bogus\"\n"
                            + "        </view>\n"
                            + "\n"
                            + "    </foo.bar.LinearLayout>\n"
                            + "\n"
                            + "</LinearLayout>\n")));
    }

    public void testCustomAttributesOnFragment() throws Exception {
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintFiles(xml("res/layout/fragment_custom_attrs.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout\n"
                            + "        xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "        xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"match_parent\"\n"
                            + "        android:orientation=\"vertical\">\n"
                            + "\n"
                            + "    <fragment\n"
                            + "            android:name=\"android.app.ListFragment\"\n"
                            + "            android:layout_width=\"match_parent\"\n"
                            + "            app:custom_attribute=\"some_value\"\n"
                            + "            android:layout_height=\"wrap_content\"/>\n"
                            + "\n"
                            + "</LinearLayout>\n")));
    }

    public void testManifest() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:4: Error: Attribute is missing the Android namespace prefix [MissingPrefix]\n"
                + "    versionCode=\"1\"\n"
                + "    ~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:11: Error: Attribute is missing the Android namespace prefix [MissingPrefix]\n"
                + "        android.label=\"@string/app_name\" >\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:18: Error: Attribute is missing the Android namespace prefix [MissingPrefix]\n"
                + "                <category name=\"android.intent.category.LAUNCHER\" />\n"
                + "                          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "3 errors, 0 warnings\n",

            lintFiles(xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"foo.bar2\"\n"
                            + "    versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\" >\n"
                            + "\n"
                            + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android.label=\"@string/app_name\" >\n"
                            + "        <activity\n"
                            + "            android:label=\"@string/app_name\"\n"
                            + "            android:name=\".Foo2Activity\" >\n"
                            + "            <intent-filter >\n"
                            + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                            + "\n"
                            + "                <category name=\"android.intent.category.LAUNCHER\" />\n"
                            + "            </intent-filter>\n"
                            + "        </activity>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n")));
    }

    public void testLayoutAttributes() throws Exception {
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintFiles(xml("res/layout/namespace3.xml", ""
                            + "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:app=\"http://schemas.android.com/apk/res/com.example.apicalltest\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\" >\n"
                            + "\n"
                            + "    <com.example.library.MyView\n"
                            + "        android:layout_width=\"300dp\"\n"
                            + "        android:layout_height=\"300dp\"\n"
                            + "        android:background=\"#ccc\"\n"
                            + "        android:paddingBottom=\"40dp\"\n"
                            + "        android:paddingLeft=\"20dp\"\n"
                            + "        app:exampleColor=\"#33b5e5\"\n"
                            + "        app:exampleDimension=\"24sp\"\n"
                            + "        app:exampleDrawable=\"@android:drawable/ic_menu_add\"\n"
                            + "        app:exampleString=\"Hello, MyView\" />\n"
                            + "\n"
                            + "</FrameLayout>\n")));
    }

    public void testLayoutAttributes2() throws Exception {
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintFiles(xml("res/layout/namespace4.xml", ""
                            + "<android.support.v7.widget.GridLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "    xmlns:app=\"http://schemas.android.com/apk/res/com.example.apicalltest\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\"\n"
                            + "    app:columnCount=\"1\"\n"
                            + "    tools:context=\".MainActivity\" >\n"
                            + "\n"
                            + "    <Button\n"
                            + "        android:id=\"@+id/button1\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        app:layout_column=\"0\"\n"
                            + "        app:layout_gravity=\"center\"\n"
                            + "        app:layout_row=\"0\"\n"
                            + "        android:text=\"Button\" />\n"
                            + "\n"
                            + "</android.support.v7.widget.GridLayout>\n")));
    }

    public void testUnusedNamespace() throws Exception {
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintProject(xml("res/layout/message_edit_detail.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout\n"
                            + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\"\n"
                            + "    android:orientation=\"vertical\"\n"
                            + "    xmlns:app=\"http://schemas.android.com/apk/res/foo.bar.baz\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\">\n"
                            + "\n"
                            + "    <android.support.v7.widget.GridLayout\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        app:orientation=\"horizontal\"\n"
                            + "        app:useDefaultMargins=\"true\">\n"
                            + "\n"
                            + "        <TextView\n"
                            + "            app:layout_rowSpan=\"1\"\n"
                            + "            android:text=\"@string/birthdays\"\n"
                            + "\t        android:layout_width=\"wrap_content\"\n"
                            + "\t        android:layout_height=\"wrap_content\" />\n"
                            + "        <TextView\n"
                            + "            android:text=\"@string/abs__action_bar_home_description\"\n"
                            + "\t        android:layout_width=\"wrap_content\"\n"
                            + "\t        android:layout_height=\"wrap_content\" />\n"
                            + "        <TextView\n"
                            + "            android:text=\"@string/abs__action_mode_done\"\n"
                            + "\t        android:layout_width=\"wrap_content\"\n"
                            + "\t        android:layout_height=\"wrap_content\" />\n"
                            + "    </android.support.v7.widget.GridLayout>\n"
                            + "\n"
                            + "    <EditText\n"
                            + "        android:id=\"@+id/editTextView\"\n"
                            + "        android:visibility=\"invisible\"\n"
                            + "        android:inputType=\"textMultiLine|textLongMessage|textAutoCorrect\"\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"match_parent\" />\n"
                            + "\n"
                            + "</LinearLayout>\n")));
    }

    public void testMissingLayoutAttribute() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/rtl.xml:7: Error: Attribute is missing the Android namespace prefix [MissingPrefix]\n"
                + "        layout_gravity=\"left\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/rtl.xml:8: Error: Attribute is missing the Android namespace prefix [MissingPrefix]\n"
                + "        layout_alignParentLeft=\"true\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/rtl.xml:9: Error: Attribute is missing the Android namespace prefix [MissingPrefix]\n"
                + "        editable=\"false\"\n"
                + "        ~~~~~~~~~~~~~~~~\n"
                + "3 errors, 0 warnings\n",

            lintProject(
                    projectProperties().compileSdk(10),
                    manifest().minSdk(5).targetSdk(17),
                    xml("res/layout/rtl.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "    tools:ignore=\"HardcodedText\" >\n"
                            + "\n"
                            + "    <Button\n"
                            + "        layout_gravity=\"left\"\n"
                            + "        layout_alignParentLeft=\"true\"\n"
                            + "        editable=\"false\"\n"
                            + "        android:text=\"Button\" />\n"
                            + "\n"
                            + "</LinearLayout>\n")
            ));
    }

    public void testDataBinding() throws Exception {
        assertEquals("No warnings.",
                lintProject(xml("res/layout/test.xml", "\n"
                        + "<layout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:bind=\"http://schemas.android.com/apk/res-auto\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <data>\n"
                        + "        <variable name=\"activity\" type=\"com.android.example.bindingdemo.MainActivity\"/>\n"
                        + "        <!---->\n"
                        + "        <import\n"
                        + "            type=\"android.view.View\"\n"
                        + "            />\n"
                        + "        <!---->\n"
                        + "        <import type=\"com.android.example.bindingdemo.R.string\" alias=\"Strings\"/>\n"
                        + "        <import type=\"com.android.example.bindingdemo.vo.User\"/>\n"
                        + "    </data>\n"
                        + "    <LinearLayout\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"match_parent\"\n"
                        + "        android:orientation=\"vertical\"\n"
                        + "        android:id=\"@+id/activityRoot\"\n"
                        + "        android:clickable=\"true\"\n"
                        + "        android:onClickListener=\"@{activity.onUnselect}\">\n"
                        + "        <android.support.v7.widget.CardView\n"
                        + "            android:id=\"@+id/selected_card\"\n"
                        + "            bind:contentPadding=\"@{activity.selected == null ? 5 : activity.selected.name.length()}\"\n"
                        + "            android:layout_width=\"match_parent\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            bind:visibility=\"@{activity.selected == null ? View.INVISIBLE : View.VISIBLE}\">\n"
                        + "\n"
                        + "            <GridLayout\n"
                        + "                android:layout_width=\"match_parent\"\n"
                        + "                android:layout_height=\"wrap_content\"\n"
                        + "                android:columnCount=\"2\"\n"
                        + "                android:rowCount=\"4\">\n"
                        + "                <Button\n"
                        + "                    android:id=\"@+id/edit_button\"\n"
                        + "                    bind:onClickListener=\"@{activity.onSave}\"\n"
                        + "                    android:text='@{\"Save changes to \" + activity.selected.name}'\n"
                        + "                    android:layout_width=\"wrap_content\"\n"
                        + "                    android:layout_height=\"wrap_content\"\n"
                        + "                    android:layout_column=\"1\"\n"
                        + "                    android:layout_gravity=\"right\"\n"
                        + "                    android:layout_row=\"2\"/>\n"
                        + "            </GridLayout>\n"
                        + "        </android.support.v7.widget.CardView>"
                        + "    </LinearLayout>\n"
                        + "</layout>")));
    }

    public void testAppCompat() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=201790
        assertEquals("No warnings.",
                lintProject(xml("res/layout/app_compat.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:orientation=\"vertical\">\n"
                        + "\n"
                        + "    <ImageButton\n"
                        + "        android:id=\"@+id/vote_button\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        app:srcCompat=\"@mipmap/ic_launcher\" />\n"
                        + "\n"
                        + "</LinearLayout>")));
    }

    public void testAppCompatOther() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=211348
        assertEquals("No warnings.",
                lintProjectIncrementally(
                        "res/layout/app_compat.xml",
                        xml("res/layout/app_compat.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:orientation=\"vertical\">\n"
                        + "\n"
                        + "    <ImageButton\n"
                        + "        android:id=\"@+id/vote_button\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        app:buttonTint=\"#ff00ff\" />\n"
                        + "\n"
                        + "</LinearLayout>"),
                        xml("res/values/res.xml", ""
                                + "<resources>\n"
                                + "    <attr name=\"buttonTint\" />\n"
                                + "</resources>\n")));
    }

    public void testAaptBundleFormat() throws Exception {
        assertEquals("No warnings.",
                lintProject(
                        xml("res/drawable/my_drawable.xml", ""
                                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                + "<inset xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                + "    xmlns:aapt=\"http://schemas.android.com/aapt\"\n"
                                + "    android:inset=\"100dp\">\n"
                                + "\n"
                                + "    <aapt:attr name=\"android:drawable\">\n"
                                + "        <color android:color=\"@color/colorAccent\" />\n"
                                + "    </aapt:attr>\n"
                                + "</inset>")
                )
        );
    }

    public void testXmlns() throws Exception {
        assertEquals(""
                + "res/layout/foo.xml:10: Warning: Unused namespace declaration xmlns:android; already declared on the root element [UnusedNamespace]\n"
                + "        xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",
                lintProject(
                        xml("res/layout/foo.xml", ""
                                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                + "<RelativeLayout\n"
                                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                + "    android:id=\"@+id/playbackReplayOptionsLayout\"\n"
                                + "    android:layout_width=\"match_parent\"\n"
                                + "    android:layout_height=\"wrap_content\"\n"
                                + "    android:layout_marginBottom=\"10dp\">\n"
                                + "    <RelativeLayout\n"
                                + "        android:id=\"@+id/continueBlock\"\n"
                                + "        xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                + "        android:layout_width=\"wrap_content\"\n"
                                + "        android:layout_height=\"wrap_content\"\n"
                                + "        android:layout_toRightOf=\"@+id/replayBlock\">\n"
                                + "    </RelativeLayout>\n"
                                + "</RelativeLayout>\n")
                )
        );
    }

    @Override
    protected List<Issue> getIssues() {
        List<Issue> combined = Lists.newArrayList(super.getIssues());
        combined.add(NamespaceDetector.UNUSED);
        return combined;
    }

    @Override
    protected TestLintClient createClient() {
        if (getName().equals("testAppCompatOther")) {
            return new ToolsBaseTestLintClient() {
                // Set fake library name on resources in this test to pretend the
                // attr comes from appcompat
                @Override
                @Nullable
                protected String getProjectResourceLibraryName() {
                    return "appcompat-v7";
                }
            };
        }
        return super.createClient();
    }
}
