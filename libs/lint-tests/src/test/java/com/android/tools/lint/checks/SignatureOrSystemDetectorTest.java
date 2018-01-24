/*
 * Copyright (C) 2014 The Android Open Source Project
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

public class SignatureOrSystemDetectorTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new SignatureOrSystemDetector();
    }

    public void testNoWarningOnProtectionLevelsOtherThanSignatureOrSystem() throws Exception {
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          package=\"foo.bar2\"\n"
                        + "          android:versionCode=\"1\"\n"
                        + "          android:versionName=\"1.0\" >\n"
                        + "\n"
                        + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                        + "\n"
                        + "    <permission android:name=\"foo.permission.NORMAL\"\n"
                        + "                android:label=\"@string/foo\"\n"
                        + "                android:description=\"@string/foo\"\n"
                        + "                android:protectionLevel=\"normal\"/>\n"
                        + "    <permission android:name=\"foo.permission.DANGEROUS\"\n"
                        + "                android:label=\"@string/foo\"\n"
                        + "                android:description=\"@string/foo\"\n"
                        + "                android:protectionLevel=\"dangerous\"/>\n"
                        + "    <permission android:name=\"foo.permission.SIGNATURE\"\n"
                        + "                android:label=\"@string/foo\"\n"
                        + "                android:description=\"@string/foo\"\n"
                        + "                android:protectionLevel=\"signature\"/>\n"
                        + "\n"
                        + "    <application\n"
                        + "            android:icon=\"@drawable/ic_launcher\"\n"
                        + "            android:label=\"@string/app_name\" >\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expectClean();
    }

    public void testWarningOnSignatureOrSystemProtectionLevel() throws Exception {
        //noinspection all // Sample code
        String expected = ""
                + "AndroidManifest.xml:13: Warning: protectionLevel should probably not be set to signatureOrSystem [SignatureOrSystemPermissions]\n"
                + "                android:protectionLevel=\"signatureOrSystem\"/>\n"
                + "                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          package=\"foo.bar2\"\n"
                        + "          android:versionCode=\"1\"\n"
                        + "          android:versionName=\"1.0\" >\n"
                        + "\n"
                        + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                        + "\n"
                        + "    <permission android:name=\"foo.permission.SIGNATURE_OR_SYSTEM\"\n"
                        + "                android:label=\"@string/foo\"\n"
                        + "                android:description=\"@string/foo\"\n"
                        + "                android:protectionLevel=\"signatureOrSystem\"/>\n"
                        + "\n"
                        + "    <application\n"
                        + "            android:icon=\"@drawable/ic_launcher\"\n"
                        + "            android:label=\"@string/app_name\" >\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for AndroidManifest.xml line 12: Replace with signature:\n"
                        + "@@ -13 +13\n"
                        + "-                 android:protectionLevel=\"signatureOrSystem\"/>\n"
                        + "+                 android:protectionLevel=\"signature\"/>\n");
    }
}
