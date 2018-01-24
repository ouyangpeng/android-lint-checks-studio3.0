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
public class OverdrawDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new OverdrawDetector();
    }

    public void test() throws Exception {
        String expected = ""
                + "res/layout/main.xml:5: Warning: Possible overdraw: Root element paints background @drawable/ic_launcher with a theme that also paints a background (inferred theme is @style/MyTheme_First) [Overdraw]\n"
                + "    android:background=\"@drawable/ic_launcher\"\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/second.xml:5: Warning: Possible overdraw: Root element paints background @drawable/ic_launcher with a theme that also paints a background (inferred theme is @style/MyTheme) [Overdraw]\n"
                + "    android:background=\"@drawable/ic_launcher\"\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/sixth.xml:4: Warning: Possible overdraw: Root element paints background @drawable/custombg with a theme that also paints a background (inferred theme is @style/MyTheme) [Overdraw]\n"
                + "    android:background=\"@drawable/custombg\"\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/third.xml:5: Warning: Possible overdraw: Root element paints background @drawable/ic_launcher with a theme that also paints a background (inferred theme is @style/MyTheme_Third) [Overdraw]\n"
                + "    android:background=\"@drawable/ic_launcher\"\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 4 warnings\n";
        //noinspection all // Sample code
        lint().files(
                mAndroidManifest,
                projectProperties().compileSdk(10),
                mCustombg,
                mCustombg2,
                image("res/drawable-ldpi/ic_launcher.png", 50, 40).fill(0xFFFFFFFF),
                xml("res/layout/sixth.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"fill_parent\"\n"
                        + "    android:background=\"@drawable/custombg\"\n"
                        + "    android:layout_height=\"fill_parent\"\n"
                        + "    android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"fill_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"@string/hello\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"),
                xml("res/layout/fifth.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"fill_parent\"\n"
                        + "    android:layout_height=\"fill_parent\"\n"
                        + "    android:background=\"@drawable/custombg2\"\n"
                        + "    android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"fill_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"@string/hello\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"),
                xml("res/layout/fourth.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"fill_parent\"\n"
                        + "    android:layout_height=\"fill_parent\"\n"
                        + "    android:background=\"@drawable/ic_launcher\"\n"
                        + "    android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"fill_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"@string/hello\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"),
                xml("res/layout/main.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"fill_parent\"\n"
                        + "    android:layout_height=\"fill_parent\"\n"
                        + "    android:background=\"@drawable/ic_launcher\"\n"
                        + "    android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"fill_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"@string/hello\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"),
                xml("res/layout/second.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"fill_parent\"\n"
                        + "    android:layout_height=\"fill_parent\"\n"
                        + "    android:background=\"@drawable/ic_launcher\"\n"
                        + "    android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"fill_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"@string/hello\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"),
                xml("res/layout/third.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"fill_parent\"\n"
                        + "    android:layout_height=\"fill_parent\"\n"
                        + "    android:background=\"@drawable/ic_launcher\"\n"
                        + "    android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"fill_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"@string/hello\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"),
                mStrings,
                mStyles,

                mR,
                mFourthActivity,
                mOverdrawActivity,
                mSecondActivity,
                mThirdActivity)
                .run()
                .expect(expected);
    }

    public void testSuppressed() throws Exception {
        //noinspection all // Sample code
        lint().files(
                mAndroidManifest,
                projectProperties().compileSdk(10),
                mCustombg,
                mCustombg2,
                image("res/drawable-ldpi/ic_launcher.png", 50, 40).fill(0xFFFFFFFF),
                xml("res/layout/main.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "    android:layout_width=\"fill_parent\"\n"
                        + "    android:layout_height=\"fill_parent\"\n"
                        + "    android:background=\"@drawable/ic_launcher\"\n"
                        + "    tools:ignore=\"Overdraw\"\n"
                        + "    android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"fill_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"@string/hello\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"),
                mStrings,
                mStyles,

                mR,
                mFourthActivity,
                mOverdrawActivity,
                mSecondActivity,
                mThirdActivity)
                .run()
                .expectClean();
    }

    public void testContextAttribute() throws Exception {
        //noinspection all // Sample code
        lint().files(
                mAndroidManifest,
                projectProperties().compileSdk(10),
                image("res/drawable-ldpi/ic_launcher.png", 50, 40).fill(0xFFFFFFFF),
                xml("res/layout/fourth.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"fill_parent\"\n"
                        + "    android:layout_height=\"fill_parent\"\n"
                        + "    android:background=\"@drawable/ic_launcher\"\n"
                        + "    android:orientation=\"vertical\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "    tools:context=\"test.pkg.FourthActivity\">\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"fill_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"@string/hello\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"),
                mStrings,
                mStyles)
                .run()
                .expectClean();
    }

    public void testContextAttribute2() throws Exception {
        //noinspection all // Sample code
        lint().files(
                mAndroidManifest,
                projectProperties().compileSdk(10),
                image("res/drawable-ldpi/ic_launcher.png", 50, 40).fill(0xFFFFFFFF),
                xml("res/layout/fourth.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"fill_parent\"\n"
                        + "    android:layout_height=\"fill_parent\"\n"
                        + "    android:background=\"@drawable/ic_launcher\"\n"
                        + "    android:orientation=\"vertical\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "    tools:context=\".FourthActivity\">\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"fill_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"@string/hello\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"),
                mStrings,
                mStyles)
                .run()
                .expectClean();
    }

    public void testNull() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=71197
        // @null as a background should not trigger a warning
        //noinspection all // Sample code
        lint().files(
                mAndroidManifest,
                projectProperties().compileSdk(10),
                xml("res/layout/null.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"fill_parent\"\n"
                        + "    android:layout_height=\"fill_parent\"\n"
                        + "    android:background=\"@null\"\n"
                        + "    android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"fill_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"@string/hello\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"),
                mStrings,
                mStyles)
                .run()
                .expectClean();
    }

    public void testToolsBackground() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=80679
        // tools:background instead of android:background should not trigger a warning
        //noinspection all // Sample code
        lint().files(
                mAndroidManifest,
                projectProperties().compileSdk(10),
                xml("res/layout/tools_background.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "    android:layout_width=\"fill_parent\"\n"
                        + "    android:layout_height=\"fill_parent\"\n"
                        + "    tools:background=\"@drawable/ic_launcher\"\n"
                        + "    android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:layout_width=\"fill_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"@string/hello\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"),
                mStrings,
                mStyles)
                .run()
                .expectClean();
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mAndroidManifest = xml("AndroidManifest.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    package=\"test.pkg\"\n"
            + "    android:versionCode=\"1\"\n"
            + "    android:versionName=\"1.0\" >\n"
            + "\n"
            + "    <uses-sdk android:minSdkVersion=\"10\" />\n"
            + "\n"
            + "    <application\n"
            + "        android:icon=\"@drawable/ic_launcher\"\n"
            + "        android:label=\"@string/app_name\"\n"
            + "        android:theme=\"@style/MyTheme\" >\n"
            + "        <activity\n"
            + "            android:name=\".OverdrawActivity\"\n"
            + "            android:label=\"@string/app_name\"\n"
            + "            android:theme=\"@style/MyTheme.First\" >\n"
            + "            <intent-filter>\n"
            + "                <action android:name=\"android.intent.action.MAIN\" />\n"
            + "\n"
            + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
            + "            </intent-filter>\n"
            + "        </activity>\n"
            + "        <activity\n"
            + "            android:name=\".SecondActivity\"\n"
            + "            android:label=\"@string/app_name\" >\n"
            + "        </activity>\n"
            + "        <activity\n"
            + "            android:name=\".ThirdActivity\"\n"
            + "            android:label=\"@string/app_name\" >\n"
            + "        </activity>\n"
            + "        <activity\n"
            + "            android:name=\"test.pkg.FourthActivity\"\n"
            + "            android:label=\"@string/app_name\"\n"
            + "            android:theme=\"@style/MyTheme.Fourth\" >\n"
            + "        </activity>\n"
            + "    </application>\n"
            + "\n"
            + "</manifest>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mCustombg = xml("res/drawable/custombg.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<bitmap\n"
            + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:src=\"@drawable/ic_launcher\"\n"
            + "    android:tileMode=\"clamp\" />\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mCustombg2 = xml("res/drawable/custombg2.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
            + "    <item android:drawable=\"@drawable/ic_launcher\" />\n"
            + "</selector>\n"
            + "\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mFourthActivity = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.app.Activity;\n"
            + "import android.os.Bundle;\n"
            + "\n"
            + "public class FourthActivity extends Activity {\n"
            + "    /** Called when the activity is first created. */\n"
            + "    @Override\n"
            + "    public void onCreate(Bundle savedInstanceState) {\n"
            + "        super.onCreate(savedInstanceState);\n"
            + "        setContentView(R.layout.fourth);\n"
            + "    }\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mOverdrawActivity = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.app.Activity;\n"
            + "import android.os.Bundle;\n"
            + "\n"
            + "public class OverdrawActivity extends Activity {\n"
            + "    /** Called when the activity is first created. */\n"
            + "    @Override\n"
            + "    public void onCreate(Bundle savedInstanceState) {\n"
            + "        super.onCreate(savedInstanceState);\n"
            + "        setContentView(R.layout.main);\n"
            + "    }\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mR = java(""
            + "/* AUTO-GENERATED FILE.  DO NOT MODIFY.\n"
            + " *\n"
            + " * This class was automatically generated by the\n"
            + " * aapt tool from the resource data it found.  It\n"
            + " * should not be modified by hand.\n"
            + " */\n"
            + "\n"
            + "package test.pkg;\n"
            + "\n"
            + "public final class R {\n"
            + "    public static final class attr {\n"
            + "    }\n"
            + "    public static final class drawable {\n"
            + "        public static final int ic_launcher=0x7f020000;\n"
            + "    }\n"
            + "    public static final class layout {\n"
            + "        public static final int fifth=0x7f030000;\n"
            + "        public static final int fourth=0x7f030001;\n"
            + "        public static final int main=0x7f030002;\n"
            + "        public static final int second=0x7f030003;\n"
            + "        public static final int third=0x7f030004;\n"
            + "    }\n"
            + "    public static final class string {\n"
            + "        public static final int app_name=0x7f040001;\n"
            + "        public static final int hello=0x7f040000;\n"
            + "    }\n"
            + "    public static final class style {\n"
            + "        public static final int MyTheme=0x7f050000;\n"
            + "        public static final int MyTheme_First=0x7f050001;\n"
            + "        public static final int MyTheme_Fourth=0x7f050004;\n"
            + "        public static final int MyTheme_Second=0x7f050002;\n"
            + "        public static final int MyTheme_Third=0x7f050003;\n"
            + "    }\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mSecondActivity = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.app.Activity;\n"
            + "import android.os.Bundle;\n"
            + "\n"
            + "public class SecondActivity extends Activity {\n"
            + "    /** Called when the activity is first created. */\n"
            + "    @Override\n"
            + "    public void onCreate(Bundle savedInstanceState) {\n"
            + "        super.onCreate(savedInstanceState);\n"
            + "        setContentView(R.layout.second);\n"
            + "    }\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStrings = xml("res/values/strings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "\n"
            + "    <string name=\"hello\">Hello World, OverdrawActivity!</string>\n"
            + "    <string name=\"app_name\">Overdraw</string>\n"
            + "\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mStyles = xml("res/values/styles.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "\n"
            + "    <style name=\"MyTheme\" parent=\"@android:style/Theme.Light\">\n"
            + "        <item name=\"android:windowBackground\">@drawable/ic_launcher</item>\n"
            + "    </style>\n"
            + "\n"
            + "    <style name=\"MyTheme.First\">\n"
            + "        <item name=\"android:textColor\">#ff00ff00</item>\n"
            + "    </style>\n"
            + "\n"
            + "    <style name=\"MyTheme.Second\">\n"
            + "        <item name=\"android:windowIsTranslucent\">true</item>\n"
            + "    </style>\n"
            + "\n"
            + "    <style name=\"MyTheme.Third\">\n"
            + "        <item name=\"android:textColor\">#ff000000</item>\n"
            + "    </style>\n"
            + "\n"
            + "    <style name=\"MyTheme.Fourth\">\n"
            + "        <item name=\"android:windowBackground\">@null</item>\n"
            + "        <item name=\"android:textColor\">#ff000000</item>\n"
            + "    </style>\n"
            + "\n"
            + "</resources>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mThirdActivity = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.app.Activity;\n"
            + "import android.os.Bundle;\n"
            + "\n"
            + "public class ThirdActivity extends Activity {\n"
            + "    /** Called when the activity is first created. */\n"
            + "    @Override\n"
            + "    public void onCreate(Bundle savedInstanceState) {\n"
            + "        super.onCreate(savedInstanceState);\n"
            + "        setTheme(R.style.MyTheme_Third);\n"
            + "        setContentView(R.layout.third);\n"
            + "    }\n"
            + "}\n");
}