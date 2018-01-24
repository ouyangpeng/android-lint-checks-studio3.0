/*
 * Copyright (C) 2016 The Android Open Source Project
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
public class ManifestPermissionAttributeDetectorTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new ManifestPermissionAttributeDetector();
    }

    public void testWrongTagPermissions1() throws Exception {
        assertEquals(""
                + "AndroidManifest.xml:19: Error: Protecting an unsupported element with a permission is a no-op and potentially dangerous. [InvalidPermission]\n"
                + "                        android:permission=\"android.permission.READ_CONTACTS\"/>\n"
                + "                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:22: Error: Protecting an unsupported element with a permission is a no-op and potentially dangerous. [InvalidPermission]\n"
                + "                          android:permission=\"android.permission.SET_WALLPAPER\"/>\n"
                + "                          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "2 errors, 0 warnings\n",
                lintProject(xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          package=\"foo.bar2\"\n"
                        + "          android:versionCode=\"1\"\n"
                        + "          android:versionName=\"1.0\">\n"
                        + "\n"
                        + "    <uses-sdk android:minSdkVersion=\"14\"/>\n"
                        + "\n"
                        + "    <application\n"
                        + "            android:icon=\"@drawable/ic_launcher\"\n"
                        + "            android:label=\"@string/app_name\"\n"
                        + "            android:permission=\"android.permission.READ_CONTACTS\">\n"
                        + "        <activity\n"
                        + "                android:label=\"@string/app_name\"\n"
                        + "                android:name=\"com.sample.service.serviceClass\"\n"
                        + "                android:permission=\"android.permission.CAMERA\">\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.MAIN\"\n"
                        + "                        android:permission=\"android.permission.READ_CONTACTS\"/>\n"
                        + "\n"
                        + "                <category android:name=\"android.intent.category.LAUNCHER\"\n"
                        + "                          android:permission=\"android.permission.SET_WALLPAPER\"/>\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"
                        + "\n")));

    }

    public void testWrongTagPermissions2() throws Exception {
        assertEquals(
                "No warnings.",
                lintProject(xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          package=\"foo.bar2\"\n"
                        + "          android:versionCode=\"1\"\n"
                        + "          android:versionName=\"1.0\">\n"
                        + "\n"
                        + "    <uses-sdk android:minSdkVersion=\"11\"/>\n"
                        + "\n"
                        + "    <uses-permission\n"
                        + "            android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\"\n"
                        + "            android:maxSdkVersion=\"18\"/>\n"
                        + "\n"
                        + "    <application\n"
                        + "            android:icon=\"@drawable/ic_launcher\"\n"
                        + "            android:label=\"@string/app_name\">\n"
                        + "        <activity\n"
                        + "                android:label=\"@string/app_name\"\n"
                        + "                android:name=\"com.sample.service.serviceClass\"\n"
                        + "                android:permission=\"android.permission.CAMERA\">\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.MAIN\"/>\n"
                        + "\n"
                        + "                <category android:name=\"android.intent.category.LAUNCHER\"/>\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"
                        + "\n")));

    }
}