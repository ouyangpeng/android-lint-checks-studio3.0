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

public class WearStandaloneAppDetectorTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new WearStandaloneAppDetector();
    }

    public void testInvalidAttributeValueForUsesFeature() throws Exception {
        String expected = ""
                + "AndroidManifest.xml:4: Error: android:required=\"false\" is not supported for this feature [InvalidWearFeatureAttribute]\n"
                + "    <uses-feature android:name=\"android.hardware.type.watch\" android:required=\"false\"/>\n"
                + "                                                             ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" >\n"
                        + "    <uses-sdk android:targetSdkVersion=\"23\" />\n"
                        + "    <uses-feature android:name=\"android.hardware.type.watch\" android:required=\"false\"/>\n"
                        + "    <application>\n"
                        + "        <meta-data \n"
                        + "            android:name=\"com.google.android.wearable.standalone\" \n"
                        + "            android:value=\"true\"\n />"
                        + "    </application>\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testMissingMetadata() throws Exception {
        String expected = ""
                + "AndroidManifest.xml:5: Warning: Missing <meta-data android:name=\"com.google.android.wearable.standalone\" ../> element [WearStandaloneAppFlag]\n"
                + "    <application>\n"
                + "    ^\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" >\n"
                        + "    <uses-sdk android:targetSdkVersion=\"23\" />\n"
                        + "    <uses-feature android:name=\"android.hardware.type.watch\"/>\n"
                        + "    <application>\n"
                        + "        <!-- Missing meta-data element -->\n"
                        + "    </application>\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testInvalidAttributeValueForStandaloneMetadata() throws Exception {
        String expected = ""
                + "AndroidManifest.xml:7: Warning: Expecting a boolean value for attribute android:value [WearStandaloneAppFlag]\n"
                + "            android:value=\"@string/foo\" />\n"
                + "                           ~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" >\n"
                        + "    <uses-feature android:name=\"android.hardware.type.watch\"/>\n"
                        + "    <application>\n"
                        + "        <meta-data \n"
                        + "            android:name=\"com.google.android.wearable.standalone\" \n"
                        + "            android:value=\"@string/foo\" />\n"
                        + "    </application>\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for AndroidManifest.xml line 6: Replace with true:\n"
                        + "@@ -7 +7\n"
                        + "-             android:value=\"@string/foo\" />\n"
                        + "+             android:value=\"true\" />\n"
                        + "Fix for AndroidManifest.xml line 6: Replace with false:\n"
                        + "@@ -7 +7\n"
                        + "-             android:value=\"@string/foo\" />\n"
                        + "+             android:value=\"false\" />\n");
    }

    public void testValidUsesFeatureAndMetadata() throws Exception {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" >\n"
                        + "    <uses-feature android:name=\"android.hardware.type.watch\"/>\n"
                        + "    <application>\n"
                        + "        <meta-data \n"
                        + "            android:name=\"com.google.android.wearable.standalone\" \n"
                        + "            android:value=\"true\" />\n"
                        + "    </application>\n"
                        + "</manifest>\n"))
                .run()
                .expectClean();
    }

    public void testMissingAndroidValue() {
        String expected = ""
                + "AndroidManifest.xml:7: Warning: Missing android:value attribute [WearStandaloneAppFlag]\n"
                + "      <meta-data\n"
                + "      ^\n"
                + "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          package=\"test.pkg\">\n"
                        + "  <uses-feature android:name=\"android.hardware.type.watch\"/>\n"
                        + "\n"
                        + "  <application android:allowBackup=\"false\">\n"
                        + "      <meta-data\n"
                        + "          android:name=\"com.google.android.wearable.standalone\" />\n"
                        + "  </application>\n"
                        + "\n"
                        + "</manifest>"))
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for AndroidManifest.xml line 6: Set value=\"true\":\n"
                        + "@@ -8 +8\n"
                        + "-         <meta-data android:name=\"com.google.android.wearable.standalone\" />\n"
                        + "+         <meta-data\n"
                        + "+             android:name=\"com.google.android.wearable.standalone\"\n"
                        + "+             android:value=\"true\" />\n");
    }
}
