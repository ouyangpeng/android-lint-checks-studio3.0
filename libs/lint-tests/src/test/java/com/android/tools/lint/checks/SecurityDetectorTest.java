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
public class SecurityDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new SecurityDetector();
    }

    @Override
    protected boolean allowCompilationErrors() {
        // Some of these unit tests are still relying on source code that references
        // unresolved symbols etc.
        return true;
    }

    public void testBroken() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:12: Warning: Exported service does not require permission [ExportedService]\n"
                + "        <service\n"
                + "        ^\n"
                + "0 errors, 1 warnings\n",
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
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <service\n"
                            + "            android:exported=\"true\"\n"
                            + "            android:label=\"@string/app_name\"\n"
                            + "            android:name=\"com.sample.service.serviceClass\"\n"
                            + "            android:process=\":remote\" >\n"
                            + "            <intent-filter >\n"
                            + "                <action android:name=\"com.sample.service.serviceClass\" >\n"
                            + "                </action>\n"
                            + "            </intent-filter>\n"
                            + "        </service>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"
                            + "\n"),
                    mStrings));
    }

    public void testBroken2() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:12: Warning: Exported service does not require permission [ExportedService]\n"
                + "        <service\n"
                + "        ^\n"
                + "0 errors, 1 warnings\n",
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
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <service\n"
                            + "            android:label=\"@string/app_name\"\n"
                            + "            android:name=\"com.sample.service.serviceClass\"\n"
                            + "            android:process=\":remote\" >\n"
                            + "            <intent-filter >\n"
                            + "                <action android:name=\"com.sample.service.serviceClass\" >\n"
                            + "                </action>\n"
                            + "            </intent-filter>\n"
                            + "        </service>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                    mStrings));
    }

    public void testBroken3() throws Exception {
        // Not defining exported, but have intent-filters
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:12: Warning: Exported service does not require permission [ExportedService]\n"
                + "        <service\n"
                + "        ^\n"
                + "0 errors, 1 warnings\n",
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
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <service\n"
                            + "            android:label=\"@string/app_name\"\n"
                            + "            android:name=\"com.sample.service.serviceClass\"\n"
                            + "            android:process=\":remote\" >\n"
                            + "            <intent-filter >\n"
                            + "                <action android:name=\"com.sample.service.serviceClass\" >\n"
                            + "                </action>\n"
                            + "            </intent-filter>\n"
                            + "        </service>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"
                            + "\n"),
                    mStrings));
    }

    public void testOk1() throws Exception {
        // Defines a permission on the <service> element
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
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <service\n"
                            + "            android:label=\"@string/app_name\"\n"
                            + "            android:name=\"com.sample.service.serviceClass\"\n"
                            + "            android:permission=\"android.permission.RECEIVE_BOOT_COMPLETED\"\n"
                            + "            android:process=\":remote\" >\n"
                            + "            <intent-filter >\n"
                            + "                <action android:name=\"com.sample.service.serviceClass\" >\n"
                            + "                </action>\n"
                            + "            </intent-filter>\n"
                            + "        </service>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                    mStrings));
    }

    public void testOk2() throws Exception {
        // Defines a permission on the parent <application> element
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
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:permission=\"android.permission.RECEIVE_BOOT_COMPLETED\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <service\n"
                            + "            android:label=\"@string/app_name\"\n"
                            + "            android:name=\"com.sample.service.serviceClass\"\n"
                            + "            android:process=\":remote\" >\n"
                            + "            <intent-filter >\n"
                            + "                <action android:name=\"com.sample.service.serviceClass\" >\n"
                            + "                </action>\n"
                            + "            </intent-filter>\n"
                            + "        </service>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                    mStrings));
    }

    public void testUri() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:25: Warning: Content provider shares everything; this is potentially dangerous. [GrantAllUris]\n"
                + "        <grant-uri-permission android:path=\"/\"/>\n"
                + "                              ~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:26: Warning: Content provider shares everything; this is potentially dangerous. [GrantAllUris]\n"
                + "        <grant-uri-permission android:pathPrefix=\"/\"/>\n"
                + "                              ~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

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
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <activity\n"
                            + "            android:label=\"@string/app_name\"\n"
                            + "            android:name=\".Foo2Activity\"\n"
                            + "            android:permission=\"Foo\" >\n"
                            + "            <intent-filter >\n"
                            + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                            + "\n"
                            + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                            + "            </intent-filter>\n"
                            + "        </activity>\n"
                            + "        <!-- good: -->\n"
                            + "        <grant-uri-permission android:pathPrefix=\"/all_downloads/\"/>\n"
                            + "        <!-- bad: -->\n"
                            + "        <grant-uri-permission android:path=\"/\"/>\n"
                            + "        <grant-uri-permission android:pathPrefix=\"/\"/>\n"
                            + "        <grant-uri-permission android:pathPattern=\".*\"/>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                    mStrings));
    }

    // exportprovider1.xml has two exported content providers with no permissions
    public void testContentProvider1() throws Exception {
        String expected = ""
                + "AndroidManifest.xml:14: Warning: Exported content providers can provide access to potentially sensitive data [ExportedContentProvider]\n"
                + "        <provider\n"
                + "        ^\n"
                + "AndroidManifest.xml:20: Warning: Exported content providers can provide access to potentially sensitive data [ExportedContentProvider]\n"
                + "        <provider\n"
                + "        ^\n"
                + "0 errors, 2 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"foo.bar2\"\n"
                        + "    android:versionCode=\"1\"\n"
                        + "    android:versionName=\"1.0\" >\n"
                        + "\n"
                        + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:icon=\"@drawable/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\" >\n"
                        + "\n"
                        + "        <!-- exported implicitly, fail -->\n"
                        + "        <provider\n"
                        + "            android:name=\"com.sample.provider.providerClass1\"\n"
                        + "            android:authorities=\"com.sample.provider.providerData\">\n"
                        + "        </provider>\n"
                        + "\n"
                        + "        <!-- exported explicitly, fail -->\n"
                        + "        <provider\n"
                        + "            android:exported=\"true\"\n"
                        + "            android:name=\"com.sample.provider.providerClass2\"\n"
                        + "            android:authorities=\"com.sample.provider.providerData\">\n"
                        + "        </provider>\n"
                        + "\n"
                        + "        <!-- not exported, win -->\n"
                        + "        <provider\n"
                        + "            android:exported=\"false\"\n"
                        + "            android:name=\"com.sample.provider.providerClass3\"\n"
                        + "            android:authorities=\"com.sample.provider.providerData\">\n"
                        + "        </provider>\n"
                        + "    </application>\n"
                        + "</manifest>\n"),
                mStrings)
                .run()
                .expect(expected)
                .verifyFixes().window(1).expectFixDiffs(""
                + "Fix for AndroidManifest.xml line 13: Set exported=\"false\":\n"
                + "@@ -16 +16\n"
                + "              android:name=\"com.sample.provider.providerClass1\"\n"
                + "-             android:authorities=\"com.sample.provider.providerData\" >\n"
                + "+             android:authorities=\"com.sample.provider.providerData\"\n"
                + "+             android:exported=\"false\" >\n"
                + "          </provider>\n"
                + "Fix for AndroidManifest.xml line 19: Set exported=\"false\":\n"
                + "@@ -23 +23\n"
                + "              android:authorities=\"com.sample.provider.providerData\"\n"
                + "-             android:exported=\"true\" >\n"
                + "+             android:exported=\"false\" >\n"
                + "          </provider>\n");
    }

    // exportprovider2.xml has no un-permissioned exported content providers
    public void testContentProvider2() throws Exception {
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
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "\n"
                            + "        <!-- read+write permission attribute, win -->\n"
                            + "        <provider\n"
                            + "            android:name=\"com.sample.provider.providerClass\"\n"
                            + "            android:authorities=\"com.sample.provider.providerData\"\n"
                            + "            android:readPermission=\"com.sample.provider.READ_PERMISSON\"\n"
                            + "            android:writePermission=\"com.sample.provider.WRITE_PERMISSON\">\n"
                            + "        </provider>\n"
                            + "\n"
                            + "        <!-- permission attribute, win -->\n"
                            + "        <provider\n"
                            + "            android:name=\"com.sample.provider.providerClass\"\n"
                            + "            android:authorities=\"com.sample.provider.providerData\"\n"
                            + "            android:permission=\"com.sample.provider.PERMISSION\">\n"
                            + "        </provider>\n"
                            + "\n"
                            + "        <!-- path-permission, win -->\n"
                            + "        <provider\n"
                            + "            android:name=\"com.sample.provider.providerClass\"\n"
                            + "            android:authorities=\"com.sample.provider.providerData\">\n"
                            + "            <path-permission\n"
                            + "                android:pathPrefix=\"/hello\"\n"
                            + "                android:permission=\"com.sample.provider.PERMISSION\">\n"
                            + "            </path-permission>\n"
                            + "        </provider>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                        mStrings));
    }

    public void testWorldWriteable() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/WorldWriteableFile.java:41: Warning: Setting file permissions to world-readable can be risky, review carefully [SetWorldReadable]\n"
                + "            mFile.setReadable(true, false);\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/WorldWriteableFile.java:48: Warning: Setting file permissions to world-writable can be risky, review carefully [SetWorldWritable]\n"
                + "            mFile.setWritable(true, false);\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/WorldWriteableFile.java:27: Warning: Using MODE_WORLD_READABLE when creating files can be risky, review carefully [WorldReadableFiles]\n"
                + "            out = openFileOutput(mFile.getName(), MODE_WORLD_READABLE);\n"
                + "                                                  ~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/WorldWriteableFile.java:32: Warning: Using MODE_WORLD_READABLE when creating files can be risky, review carefully [WorldReadableFiles]\n"
                + "            prefs = getSharedPreferences(mFile.getName(), MODE_WORLD_READABLE);\n"
                + "                                                          ~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/WorldWriteableFile.java:36: Warning: Using MODE_WORLD_READABLE when creating files can be risky, review carefully [WorldReadableFiles]\n"
                + "            dir = getDir(mFile.getName(), MODE_WORLD_READABLE);\n"
                + "                                          ~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/WorldWriteableFile.java:26: Warning: Using MODE_WORLD_WRITEABLE when creating files can be risky, review carefully [WorldWriteableFiles]\n"
                + "            out = openFileOutput(mFile.getName(), MODE_WORLD_WRITEABLE);\n"
                + "                                                  ~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/WorldWriteableFile.java:31: Warning: Using MODE_WORLD_WRITEABLE when creating files can be risky, review carefully [WorldWriteableFiles]\n"
                + "            prefs = getSharedPreferences(mFile.getName(), MODE_WORLD_WRITEABLE);\n"
                + "                                                          ~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/WorldWriteableFile.java:35: Warning: Using MODE_WORLD_WRITEABLE when creating files can be risky, review carefully [WorldWriteableFiles]\n"
                + "            dir = getDir(mFile.getName(), MODE_WORLD_WRITEABLE);\n"
                + "                                          ~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 8 warnings\n",

            lintProject(
                java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import java.io.File;\n"
                            + "import java.io.IOException;\n"
                            + "import java.io.OutputStream;\n"
                            + "import java.io.InputStream;\n"
                            + "import java.io.FileNotFoundException;\n"
                            + "import android.content.Context;\n"
                            + "import android.content.SharedPreferences;\n"
                            + "import android.app.Activity;\n"
                            + "import android.os.Bundle;\n"
                            + "\n"
                            + "public class WorldWriteableFile extends Activity {\n"
                            + "    File mFile;\n"
                            + "    Context mContext;\n"
                            + "\n"
                            + "    public void foo() {\n"
                            + "        OutputStream out = null;\n"
                            + "        SharedPreferences prefs = null;\n"
                            + "        File dir = null;\n"
                            + "\n"
                            + "        boolean success = false;\n"
                            + "        try {\n"
                            + "            //out = openFileOutput(mFile.getName()); // ok\n"
                            + "            out = openFileOutput(mFile.getName(), MODE_PRIVATE); // ok\n"
                            + "            out = openFileOutput(mFile.getName(), MODE_WORLD_WRITEABLE);\n"
                            + "            out = openFileOutput(mFile.getName(), MODE_WORLD_READABLE);\n"
                            + "\n"
                            + "            prefs = getSharedPreferences(mFile.getName(), 0); // ok\n"
                            + "            prefs = getSharedPreferences(mFile.getName(), MODE_PRIVATE); // ok\n"
                            + "            prefs = getSharedPreferences(mFile.getName(), MODE_WORLD_WRITEABLE);\n"
                            + "            prefs = getSharedPreferences(mFile.getName(), MODE_WORLD_READABLE);\n"
                            + "\n"
                            + "            dir = getDir(mFile.getName(), MODE_PRIVATE); // ok\n"
                            + "            dir = getDir(mFile.getName(), MODE_WORLD_WRITEABLE);\n"
                            + "            dir = getDir(mFile.getName(), MODE_WORLD_READABLE);\n"
                            + "\n"
                            + "            mFile.setReadable(true, true); // ok\n"
                            + "            mFile.setReadable(false, true); // ok\n"
                            + "            mFile.setReadable(false, false); // ok\n"
                            + "            mFile.setReadable(true, false);\n"
                            + "            mFile.setReadable(true); // ok\n"
                            + "            mFile.setReadable(false); // ok\n"
                            + "\n"
                            + "            mFile.setWritable(true, true); // ok\n"
                            + "            mFile.setWritable(false, true); // ok\n"
                            + "            mFile.setWritable(false, false); // ok\n"
                            + "            mFile.setWritable(true, false);\n"
                            + "            mFile.setWritable(true); // ok\n"
                            + "            mFile.setWritable(false); // ok\n"
                            + "\n"
                            + "            // Flickr.get().downloadPhoto(params[0], Flickr.PhotoSize.LARGE,\n"
                            + "            // out);\n"
                            + "            success = true;\n"
                            + "        } catch (FileNotFoundException e) {\n"
                            + "        }\n"
                            + "    }\n"
                            + "}\n")));
    }

    public void testReceiver0() throws Exception {
        // Activities that do not have intent-filters do not need warnings
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
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <receiver\n"
                            + "            android:label=\"@string/app_name\"\n"
                            + "            android:name=\"com.sample.service.serviceClass\" >\n"
                            + "        </receiver>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"
                            + "\n"),
                mStrings));
    }

    public void testReceiver1() throws Exception {
        String expected = ""
                + "AndroidManifest.xml:12: Warning: Exported receiver does not require permission [ExportedReceiver]\n"
                + "        <receiver\n"
                + "        ^\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"foo.bar2\"\n"
                        + "    android:versionCode=\"1\"\n"
                        + "    android:versionName=\"1.0\" >\n"
                        + "\n"
                        + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:icon=\"@drawable/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\" >\n"
                        + "        <receiver\n"
                        + "            android:label=\"@string/app_name\"\n"
                        + "            android:name=\"com.sample.service.serviceClass\" >\n"
                        + "            <intent-filter >\n"
                        + "                <action android:name=\"com.sample.service.serviceClass\" >\n"
                        + "                </action>\n"
                        + "            </intent-filter>\n"
                        + "        </receiver>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"
                        + "\n"),
                mStrings)
                .run()
                .expect(expected)
                .verifyFixes().window(1).expectFixDiffs(""
                + "Fix for AndroidManifest.xml line 11: Set permission:\n"
                + "@@ -14 +14\n"
                + "              android:name=\"com.sample.service.serviceClass\"\n"
                + "-             android:label=\"@string/app_name\" >\n"
                + "+             android:label=\"@string/app_name\"\n"
                + "+             android:permission=\"|\" >\n"
                + "              <intent-filter>\n");
    }

    public void testReceiver2() throws Exception {
        // Defines a permission on the <activity> element
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
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <receiver\n"
                            + "            android:label=\"@string/app_name\"\n"
                            + "            android:name=\"com.sample.service.serviceClass\"\n"
                            + "            android:permission=\"android.permission.RECEIVE_BOOT_COMPLETED\"\n"
                            + "            android:process=\":remote\" >\n"
                            + "            <intent-filter >\n"
                            + "                <action android:name=\"com.sample.service.serviceClass\" >\n"
                            + "                </action>\n"
                            + "            </intent-filter>\n"
                            + "        </receiver>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                mStrings));
    }

    public void testReceiver3() throws Exception {
        // Defines a permission on the parent <application> element
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
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:permission=\"android.permission.RECEIVE_BOOT_COMPLETED\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <receiver\n"
                            + "            android:label=\"@string/app_name\"\n"
                            + "            android:name=\"com.sample.service.serviceClass\"\n"
                            + "            android:process=\":remote\" >\n"
                            + "            <intent-filter >\n"
                            + "                <action android:name=\"com.sample.service.serviceClass\" >\n"
                            + "                </action>\n"
                            + "            </intent-filter>\n"
                            + "        </receiver>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                mStrings));
    }

    public void testReceiver4() throws Exception {
        // Not defining exported, but have intent-filters
        String expected = ""
                + "AndroidManifest.xml:12: Warning: Exported receiver does not require permission [ExportedReceiver]\n"
                + "        <receiver\n"
                + "        ^\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"foo.bar2\"\n"
                        + "    android:versionCode=\"1\"\n"
                        + "    android:versionName=\"1.0\" >\n"
                        + "\n"
                        + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:icon=\"@drawable/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\" >\n"
                        + "        <receiver\n"
                        + "            android:label=\"@string/app_name\"\n"
                        + "            android:name=\"com.sample.service.serviceClass\"\n"
                        + "            android:process=\":remote\" >\n"
                        + "            <intent-filter >\n"
                        + "                <action android:name=\"com.sample.service.serviceClass\" >\n"
                        + "                </action>\n"
                        + "            </intent-filter>\n"
                        + "        </receiver>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"
                        + "\n"),
                mStrings)
                .run()
                .expect(expected)
                .verifyFixes().window(1).expectFixDiffs(""
                + "Fix for AndroidManifest.xml line 11: Set permission:\n"
                + "@@ -15 +15\n"
                + "              android:label=\"@string/app_name\"\n"
                + "+             android:permission=\"|\"\n"
                + "              android:process=\":remote\" >\n");
    }

    public void testReceiver5() throws Exception {
      // Intent filter for standard Android action
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
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\">\n"
                            + "        <receiver>\n"
                            + "            <intent-filter>\n"
                            + "                <action android:name=\"android.intent.action.BOOT_COMPLETED\" />\n"
                            + "            </intent-filter>\n"
                            + "        </receiver>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"
                            + "\n"),
              mStrings));
    }

    public void testStandard() throws Exception {
        // Various regression tests for http://code.google.com/p/android/issues/detail?id=33976
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",
            lintProject(xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"foo.bar2\"\n"
                            + "    android:versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\" >\n"
                            + "\n"
                            + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "\n"
                            + "        <receiver android:name=\".DockReceiver\" >\n"
                            + "            <intent-filter>\n"
                            + "                <action android:name=\"android.intent.action.DOCK_EVENT\" />\n"
                            + "                <action android:name=\"android.app.action.ENTER_CAR_MODE\" />\n"
                            + "            </intent-filter>\n"
                            + "        </receiver>\n"
                            + "\n"
                            + "        <receiver\n"
                            + "            android:name=\"com.foo.BarReceiver\"\n"
                            + "            android:enabled=\"false\" >\n"
                            + "            <intent-filter>\n"
                            + "                <action android:name=\"android.intent.action.ACTION_POWER_CONNECTED\" />\n"
                            + "                <action android:name=\"android.net.conn.CONNECTIVITY_CHANGE\" />\n"
                            + "            </intent-filter>\n"
                            + "        </receiver>\n"
                            + "\n"
                            + "        <receiver\n"
                            + "            android:name=\".AppWidget\"\n"
                            + "            android:exported=\"true\"\n"
                            + "            android:label=\"@string/label\" >\n"
                            + "            <intent-filter>\n"
                            + "                <action android:name=\"android.appwidget.action.APPWIDGET_UPDATE\" />\n"
                            + "            </intent-filter>\n"
                            + "\n"
                            + "            <meta-data\n"
                            + "                android:name=\"android.appwidget.provider\"\n"
                            + "                android:resource=\"@xml/config\" />\n"
                            + "        </receiver>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n")));
    }

    public void testUsingInstallReferrerReceiver() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=73934
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",
                lintProject(xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"test.pkg\" >\n"
                            + "\n"
                            + "    <uses-permission android:name=\"android.permission.internet\"/>\n"
                            + "    <application\n"
                            + "        android:allowBackup=\"true\"\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\"\n"
                            + "        android:theme=\"@style/AppTheme\" >\n"
                            + "        <activity\n"
                            + "            android:name=\".MyActivity\"\n"
                            + "            android:label=\"@string/app_name\" >\n"
                            + "            <intent-filter>\n"
                            + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                            + "\n"
                            + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                            + "            </intent-filter>\n"
                            + "        </activity>\n"
                            + "\n"
                            + "        <!-- Used for install referrer tracking-->\n"
                            + "        <service android:name=\"com.google.android.gms.tagmanager.InstallReferrerService\"/>\n"
                            + "        <receiver\n"
                            + "            android:name=\"com.google.android.gms.tagmanager.InstallReferrerReceiver\"\n"
                            + "            android:exported=\"true\">\n"
                            + "            <intent-filter>\n"
                            + "                <action android:name=\"com.android.vending.INSTALL_REFERRER\" />\n"
                            + "            </intent-filter>\n"
                            + "        </receiver>\n"
                            + "    </application>\n"
                            + "</manifest>\n")));
    }

    public void testGmsWearable() throws Exception {
        // As documented in
        //    https://developer.android.com/training/wearables/data-layer/events.html
        // you shouldn't need a permission here.
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",
                lintProject(xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"test.pkg\" >\n"
                            + "\n"
                            + "    <uses-permission android:name=\"android.permission.internet\"/>\n"
                            + "    <application\n"
                            + "        android:allowBackup=\"true\"\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\"\n"
                            + "        android:theme=\"@style/AppTheme\" >\n"
                            + "        <service android:name=\".DataLayerListenerService\">\n"
                            + "            <intent-filter>\n"
                            + "                <action android:name=\"com.google.android.gms.wearable.DATA_CHANGED\" />\n"
                            + "            </intent-filter>\n"
                            + "        </service>\n"
                            + "    </application>\n"
                            + "</manifest>\n")));

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
