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
public class OnClickDetectorTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new OnClickDetector();
    }

    public void test() {
        String expected = ""
                + "src/test/pkg/OnClickActivity.java:26: Error: onClick handler wrong5(View) must be public [OnClick]\n"
                + "    // Wrong modifier (not public)\n"
                + "    ^\n"
                + "src/test/pkg/OnClickActivity.java:31: Error: onClick handler wrong6(View) should not be static [OnClick]\n"
                + "    public static void wrong6(View view) {\n"
                + "           ~~~~~~\n"
                + "src/test/pkg/OnClickActivity.java:45: Error: onClick handler wrong7(View) must be public [OnClick]\n"
                + "    void wrong7(View view) {\n"
                + "    ^\n"
                + "res/layout/onclick.xml:10: Error: Corresponding method handler 'public void nonexistent(android.view.View)' not found [OnClick]\n"
                + "        android:onClick=\"nonexistent\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/onclick.xml:16: Error: Corresponding method handler 'public void wrong1(android.view.View)' not found [OnClick]\n"
                + "        android:onClick=\"wrong1\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/onclick.xml:22: Error: Corresponding method handler 'public void wrong2(android.view.View)' not found [OnClick]\n"
                + "        android:onClick=\"wrong2\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/onclick.xml:28: Error: Corresponding method handler 'public void wrong3(android.view.View)' not found [OnClick]\n"
                + "        android:onClick=\"wrong3\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/onclick.xml:58: Error: Corresponding method handler 'public void simple_typo(android.view.View)' not found (did you mean OnClickActivity#simple_tyop ?) [OnClick]\n"
                + "        android:onClick=\"simple_typo\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/onclick.xml:82: Error: onClick handler method name cannot start with the character '1' [OnClick]\n"
                + "        android:onClick=\"1invalidname\"\n"
                + "                         ~~~~~~~~~~~~\n"
                + "res/layout/onclick.xml:88: Error: There should be no spaces in the onClick handler name [OnClick]\n"
                + "        android:onClick=\"invalid name\"\n"
                + "                         ~~~~~~~~~~~~\n"
                + "res/layout/onclick.xml:94: Error: onClick handler method name cannot contain the character '(' [OnClick]\n"
                + "        android:onClick=\"invalidname()\"\n"
                + "                         ~~~~~~~~~~~~~\n"
                + "res/layout/onclick.xml:100: Error: onClick handler method name cannot be a Java keyword [OnClick]\n"
                + "        android:onClick=\"new\"\n"
                + "                         ~~~\n"
                + "12 errors, 0 warnings\n";

        //noinspection all // Sample code
        lint().files(
                classpath(),
                manifest().minSdk(10),
                xml("res/layout/onclick.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:onClick=\"nonexistent\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:onClick=\"wrong1\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:onClick=\"wrong2\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:onClick=\"wrong3\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:onClick=\"ok2\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:onClick=\"wrong5\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:onClick=\"wrong6\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:onClick=\"ok\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:onClick=\"simple_typo\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:onClick=\"my\\u1234method\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:onClick=\"wrong7\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:onClick=\"@string/ok\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:onClick=\"1invalidname\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:onClick=\"invalid name\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:onClick=\"invalidname()\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:onClick=\"new\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.util.Log;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "/** Test data for the OnClickDetector */\n"
                        + "public class OnClickActivity extends Activity {\n"
                        + "    // Wrong argument type 1\n"
                        + "    public void wrong1() {\n"
                        + "    }\n"
                        + "\n"
                        + "    // Wrong argument type 2\n"
                        + "    public void wrong2(int i) {\n"
                        + "    }\n"
                        + "\n"
                        + "    // Wrong argument type 3\n"
                        + "    public void wrong3(View view, int i) {\n"
                        + "    }\n"
                        + "\n"
                        + "    // Return type is allowed to not be void\n"
                        + "    public int ok2(View view) {\n"
                        + "        return 0;\n"
                        + "    }\n"
                        + "\n"
                        + "    // Wrong modifier (not public)\n"
                        + "    void wrong5(View view) {\n"
                        + "    }\n"
                        + "\n"
                        + "    // Wrong modifier (is static)\n"
                        + "    public static void wrong6(View view) {\n"
                        + "    }\n"
                        + "\n"
                        + "    public void ok(View view) {\n"
                        + "    }\n"
                        + "\n"
                        + "    // Ok: Unicode escapes\n"
                        + "    public void my\u1234method(View view) {\n"
                        + "    }\n"
                        + "\n"
                        + "    // Typo\n"
                        + "    public void simple_tyop(View view) {\n"
                        + "    }\n"
                        + "\n"
                        + "    void wrong7(View view) {\n"
                        + "        Log.i(\"x\", \"wrong7: called\");\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expect(expected);
    }

    public void testWithContextAttribute() {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"my.pkg\">\n"
                        + "\n"
                        + "    <application android:allowBackup=\"true\" android:icon=\"@mipmap/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\" android:roundIcon=\"@mipmap/ic_launcher_round\"\n"
                        + "        android:supportsRtl=\"true\" android:theme=\"@style/AppTheme\">\n"
                        + "        <activity android:name=\".MainActivity\">\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                        + "\n"
                        + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>"),
                xml("res/layout/main_activity.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout\n"
                        + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    tools:context=\".MainActivity\">\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:onClick=\"myHandler\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:onClick=\"missing\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "</LinearLayout>"),
                java("src/my/pkg/MainActivity.java", ""
                        + "package my.pkg;\n"
                        + "public class MainActivity {\n"
                        + "    public void myHandler(android.view.View v) { }\n"
                        + "}\n"))
                .incremental("res/layout/main_activity.xml")
                .run()
                .expect(""
                        + "res/layout/main_activity.xml:19: Error: Corresponding method handler 'public void missing(android.view.View)' not found [OnClick]\n"
                        + "        android:onClick=\"missing\"\n"
                        + "                         ~~~~~~~\n"
                        + "1 errors, 0 warnings\n");
    }

    public void testOk() {
        // No onClick attributes
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/accessibility.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" android:id=\"@+id/newlinear\" android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">\n"
                        + "    <Button android:text=\"Button\" android:id=\"@+id/button1\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"></Button>\n"
                        + "    <ImageView android:id=\"@+id/android_logo\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                        + "    <ImageButton android:importantForAccessibility=\"yes\" android:id=\"@+id/android_logo2\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                        + "    <Button android:text=\"Button\" android:id=\"@+id/button2\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"></Button>\n"
                        + "    <Button android:id=\"@+android:id/summary\" android:contentDescription=\"@string/label\" />\n"
                        + "    <ImageButton android:importantForAccessibility=\"no\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                        + "</LinearLayout>\n"))
                .run()
                .expectClean();
    }

    public void testDataBinding() {
        // Regression test for
        // 235032: Data binding fails with lint errors on lambdas
        lint().files(
                xml("res/layout/accessibility.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:id=\"@+id/newlinear\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:orientation=\"vertical\">\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:id=\"@+id/button1\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:onClick=\"@{() -> handlers.goToPercentInListMinHeight()}\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "</LinearLayout>\n"))
                .run()
                .expectClean();
    }
}
