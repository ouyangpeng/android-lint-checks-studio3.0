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
public class ManifestTypoDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new ManifestTypoDetector();
    }

    public void testOk() throws Exception {
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",
                lintProject(
                        xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"foo.bar2\"\n"
                            + "    android:versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\" >\n"
                            + "\n"
                            + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                            + "\n"
                            + "    <uses-permission android:name=\"com.example.helloworld.permission\" />\n"
                            + "\n"
                            + "    <uses-feature android:name=\"android.hardware.wifi\" />\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <uses-library android:name=\"com.example.helloworld\" />\n"
                            + "        <activity\n"
                            + "            android:label=\"@string/app_name\"\n"
                            + "            android:name=\".Foo2Activity\" >\n"
                            + "            <intent-filter >\n"
                            + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                            + "\n"
                            + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                            + "            </intent-filter>\n"
                            + "        </activity>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                        mStrings));
    }

    public void testTypoUsesSdk() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:7: Error: Misspelled tag <use-sdk>: Did you mean <uses-sdk> ? [ManifestTypo]\n"
                + "    <use-sdk android:minSdkVersion=\"14\" />\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintProject(
                    xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"foo.bar2\"\n"
                            + "    android:versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\" >\n"
                            + "\n"
                            + "    <use-sdk android:minSdkVersion=\"14\" />\n"
                            + "\n"
                            + "    <uses-permission android:name=\"com.example.helloworld.permission\" />\n"
                            + "\n"
                            + "    <uses-feature android:name=\"android.hardware.wifi\" />\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <uses-library android:name=\"com.example.helloworld\" />\n"
                            + "        <activity\n"
                            + "            android:label=\"@string/app_name\"\n"
                            + "            android:name=\".Foo2Activity\" >\n"
                            + "            <intent-filter >\n"
                            + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                            + "\n"
                            + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                            + "            </intent-filter>\n"
                            + "        </activity>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                    mStrings));
    }

    public void testTypoUsesSdk2() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:7: Error: Misspelled tag <user-sdk>: Did you mean <uses-sdk> ? [ManifestTypo]\n"
                + "    <user-sdk android:minSdkVersion=\"14\" />\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintProject(
                    xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"foo.bar2\"\n"
                            + "    android:versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\" >\n"
                            + "\n"
                            + "    <user-sdk android:minSdkVersion=\"14\" />\n"
                            + "\n"
                            + "    <uses-permission android:name=\"com.example.helloworld.permission\" />\n"
                            + "\n"
                            + "    <uses-feature android:name=\"android.hardware.wifi\" />\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <uses-library android:name=\"com.example.helloworld\" />\n"
                            + "        <activity\n"
                            + "            android:label=\"@string/app_name\"\n"
                            + "            android:name=\".Foo2Activity\" >\n"
                            + "            <intent-filter >\n"
                            + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                            + "\n"
                            + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                            + "            </intent-filter>\n"
                            + "        </activity>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                    mStrings));
    }

    public void testTypoUsesPermission() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:9: Error: Misspelled tag <use-permission>: Did you mean <uses-permission> ? [ManifestTypo]\n"
                + "    <use-permission android:name=\"com.example.helloworld.permission\" />\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintProject(
                    xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"foo.bar2\"\n"
                            + "    android:versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\" >\n"
                            + "\n"
                            + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                            + "\n"
                            + "    <use-permission android:name=\"com.example.helloworld.permission\" />\n"
                            + "\n"
                            + "    <uses-feature android:name=\"android.hardware.wifi\" />\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <uses-library android:name=\"com.example.helloworld\" />\n"
                            + "        <activity\n"
                            + "            android:label=\"@string/app_name\"\n"
                            + "            android:name=\".Foo2Activity\" >\n"
                            + "            <intent-filter >\n"
                            + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                            + "\n"
                            + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                            + "            </intent-filter>\n"
                            + "        </activity>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                    mStrings));
    }

    public void testTypoUsesPermission2() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:9: Error: Misspelled tag <user-permission>: Did you mean <uses-permission> ? [ManifestTypo]\n"
                + "    <user-permission android:name=\"com.example.helloworld.permission\" />\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintProject(
                    xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"foo.bar2\"\n"
                            + "    android:versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\" >\n"
                            + "\n"
                            + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                            + "\n"
                            + "    <user-permission android:name=\"com.example.helloworld.permission\" />\n"
                            + "\n"
                            + "    <uses-feature android:name=\"android.hardware.wifi\" />\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <uses-library android:name=\"com.example.helloworld\" />\n"
                            + "        <activity\n"
                            + "            android:label=\"@string/app_name\"\n"
                            + "            android:name=\".Foo2Activity\" >\n"
                            + "            <intent-filter >\n"
                            + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                            + "\n"
                            + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                            + "            </intent-filter>\n"
                            + "        </activity>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                    mStrings));
    }

    public void testTypoUsesFeature() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:11: Error: Misspelled tag <use-feature>: Did you mean <uses-feature> ? [ManifestTypo]\n"
                + "    <use-feature android:name=\"android.hardware.wifi\" />\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintProject(
                    xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"foo.bar2\"\n"
                            + "    android:versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\" >\n"
                            + "\n"
                            + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                            + "\n"
                            + "    <uses-permission android:name=\"com.example.helloworld.permission\" />\n"
                            + "\n"
                            + "    <use-feature android:name=\"android.hardware.wifi\" />\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <uses-library android:name=\"com.example.helloworld\" />\n"
                            + "        <activity\n"
                            + "            android:label=\"@string/app_name\"\n"
                            + "            android:name=\".Foo2Activity\" >\n"
                            + "            <intent-filter >\n"
                            + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                            + "\n"
                            + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                            + "            </intent-filter>\n"
                            + "        </activity>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                    mStrings));
    }

    public void testTypoUsesFeature2() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:11: Error: Misspelled tag <user-feature>: Did you mean <uses-feature> ? [ManifestTypo]\n"
                + "    <user-feature android:name=\"android.hardware.wifi\" />\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintProject(
                    xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"foo.bar2\"\n"
                            + "    android:versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\" >\n"
                            + "\n"
                            + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                            + "\n"
                            + "    <uses-permission android:name=\"com.example.helloworld.permission\" />\n"
                            + "\n"
                            + "    <user-feature android:name=\"android.hardware.wifi\" />\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <uses-library android:name=\"com.example.helloworld\" />\n"
                            + "        <activity\n"
                            + "            android:label=\"@string/app_name\"\n"
                            + "            android:name=\".Foo2Activity\" >\n"
                            + "            <intent-filter >\n"
                            + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                            + "\n"
                            + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                            + "            </intent-filter>\n"
                            + "        </activity>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                    mStrings));
    }

    public void testTypoUsesLibrary() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:16: Error: Misspelled tag <use-library>: Did you mean <uses-library> ? [ManifestTypo]\n"
                + "        <use-library android:name=\"com.example.helloworld\" />\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintProject(
                    xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"foo.bar2\"\n"
                            + "    android:versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\" >\n"
                            + "\n"
                            + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                            + "\n"
                            + "    <uses-permission android:name=\"com.example.helloworld.permission\" />\n"
                            + "\n"
                            + "    <uses-feature android:name=\"android.hardware.wifi\" />\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <use-library android:name=\"com.example.helloworld\" />\n"
                            + "        <activity\n"
                            + "            android:label=\"@string/app_name\"\n"
                            + "            android:name=\".Foo2Activity\" >\n"
                            + "            <intent-filter >\n"
                            + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                            + "\n"
                            + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                            + "            </intent-filter>\n"
                            + "        </activity>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                    mStrings));
    }

    public void testTypoUsesLibrary2() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:16: Error: Misspelled tag <user-library>: Did you mean <uses-library> ? [ManifestTypo]\n"
                + "        <user-library android:name=\"com.example.helloworld\" />\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintProject(
                    xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"foo.bar2\"\n"
                            + "    android:versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\" >\n"
                            + "\n"
                            + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                            + "\n"
                            + "    <uses-permission android:name=\"com.example.helloworld.permission\" />\n"
                            + "\n"
                            + "    <uses-feature android:name=\"android.hardware.wifi\" />\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <user-library android:name=\"com.example.helloworld\" />\n"
                            + "        <activity\n"
                            + "            android:label=\"@string/app_name\"\n"
                            + "            android:name=\".Foo2Activity\" >\n"
                            + "            <intent-filter >\n"
                            + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                            + "\n"
                            + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                            + "            </intent-filter>\n"
                            + "        </activity>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                    mStrings));
    }

    public void testOtherTypos() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:2: Error: Misspelled tag <mannifest>: Did you mean <manifest> ? [ManifestTypo]\n"
                + "<mannifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "^\n"
                + "AndroidManifest.xml:7: Error: Misspelled tag <uses-sd>: Did you mean <uses-sdk> ? [ManifestTypo]\n"
                + "    <uses-sd android:minSdkVersion=\"14\" />\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:9: Error: Misspelled tag <spplication>: Did you mean <application> ? [ManifestTypo]\n"
                + "    <spplication\n"
                + "    ^\n"
                + "AndroidManifest.xml:12: Error: Misspelled tag <acctivity>: Did you mean <activity> ? [ManifestTypo]\n"
                + "        <acctivity\n"
                + "        ^\n"
                + "AndroidManifest.xml:15: Error: Misspelled tag <inten-filter>: Did you mean <intent-filter> ? [ManifestTypo]\n"
                + "            <inten-filter >\n"
                + "            ^\n"
                + "AndroidManifest.xml:16: Error: Misspelled tag <aktion>: Did you mean <action> ? [ManifestTypo]\n"
                + "                <aktion android:name=\"android.intent.action.MAIN\" />\n"
                + "                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:18: Error: Misspelled tag <caaategory>: Did you mean <category> ? [ManifestTypo]\n"
                + "                <caaategory android:name=\"android.intent.category.LAUNCHER\" />\n"
                + "                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "7 errors, 0 warnings\n",

                lintProject(
                        xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<mannifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"foo.bar2\"\n"
                            + "    android:versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\" >\n"
                            + "\n"
                            + "    <uses-sd android:minSdkVersion=\"14\" />\n"
                            + "\n"
                            + "    <spplication\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <acctivity\n"
                            + "            android:label=\"@string/app_name\"\n"
                            + "            android:name=\".Foo2Activity\" >\n"
                            + "            <inten-filter >\n"
                            + "                <aktion android:name=\"android.intent.action.MAIN\" />\n"
                            + "\n"
                            + "                <caaategory android:name=\"android.intent.category.LAUNCHER\" />\n"
                            + "            </inten-filter>\n"
                            + "        </acctivity>\n"
                            + "    </spplication>\n"
                            + "\n"
                            + "</mannifest>\n"),
                        mStrings));
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mStrings = xml("res/values/strings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<!-- Copyright (C) 2007 The Android Open Source Project\n"
            + "\n"
            + "     Licensed under the Apache License, Version 2.0 (the \"License\");\n"
            + "     you may not use this file except in compliance with the License.\n"
            + "     You may obtain a copy of the License at\n"
            + "\n"
            + "          http://www.apache.org/licenses/LICENSE-2.0\n"
            + "\n"
            + "     Unless required by applicable law or agreed to in writing, software\n"
            + "     distributed under the License is distributed on an \"AS IS\" BASIS,\n"
            + "     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
            + "     See the License for the specific language governing permissions and\n"
            + "     limitations under the License.\n"
            + "-->\n"
            + "\n"
            + "<resources>\n"
            + "    <!-- Home -->\n"
            + "    <string name=\"home_title\">Home Sample</string>\n"
            + "    <string name=\"show_all_apps\">All</string>\n"
            + "\n"
            + "    <!-- Home Menus -->\n"
            + "    <string name=\"menu_wallpaper\">Wallpaper</string>\n"
            + "    <string name=\"menu_search\">Search</string>\n"
            + "    <string name=\"menu_settings\">Settings</string>\n"
            + "    <string name=\"dummy\" translatable=\"false\">Ignore Me</string>\n"
            + "\n"
            + "    <!-- Wallpaper -->\n"
            + "    <string name=\"wallpaper_instructions\">Tap picture to set portrait wallpaper</string>\n"
            + "</resources>\n"
            + "\n");
}
