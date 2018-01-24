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

import static com.android.tools.lint.checks.ChromeOsDetector.PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE;
import static com.android.tools.lint.checks.ChromeOsDetector.UNSUPPORTED_CHROME_OS_HARDWARE;

import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings("javadoc")
public class ChromeOsDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new ChromeOsDetector();
    }

    public void testInvalidUnsupportedHardware() {
        String expected = ""
                + "AndroidManifest.xml:5: Error: Expecting android:required=\"false\" for this hardware feature that may not be supported by all Chrome OS devices. [UnsupportedChromeOsHardware]\n"
                + "        android:name=\"android.hardware.touchscreen\" android:required=\"true\"/>\n"
                + "                                                    ~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n";
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-feature\n"
                        + "        android:name=\"android.hardware.touchscreen\" android:required=\"true\"/>\n"
                        + "\n"
                        + "</manifest>\n"))
                .issues(UNSUPPORTED_CHROME_OS_HARDWARE)
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for AndroidManifest.xml line 4: Set required=\"false\":\n"
                        + "@@ -7 +7\n"
                        + "-         android:required=\"true\" />\n"
                        + "+         android:required=\"false\" />\n");
    }

    public void testValidUnsupportedHardware() {
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-feature\n"
                        + "        android:name=\"android.hardware.touchscreen\"\n"
                        + "        android:required=\"false\" />\n"
                        + "</manifest>\n"))
                .issues(UNSUPPORTED_CHROME_OS_HARDWARE)
                .run()
                .expectClean();
    }

    public void testValidSupportedHardware() {
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-feature\n"
                        + "        android:name=\"android.hardware.microphone\" />\n"
                        + "</manifest>\n"))
                .issues(UNSUPPORTED_CHROME_OS_HARDWARE)
                .run()
                .expectClean();
    }

    public void testValidPermissionImpliesNotMissingUnsupportedHardware() {
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-permission android:name=\"android.permission.CALL_PHONE\"/>\n"
                        + "    <uses-feature android:required=\"false\" android:name=\"android.hardware.telephony\"/>\n"
                        + "</manifest>\n"))
                .issues(PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE)
                .run()
                .expectClean();
    }

    public void testInvalidPermissionImpliesNotMissingUnsupportedHardware() {
        String expected =
                "AndroidManifest.xml:4: Error: Permission exists without corresponding hardware <uses-feature android:name=\"android.hardware.telephony\" required=\"false\"> tag. [PermissionImpliesUnsupportedChromeOsHardware]\n"
                        + "    <uses-permission android:name=\"android.permission.CALL_PHONE\"/>\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n";
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-permission android:name=\"android.permission.CALL_PHONE\"/>\n"
                        + "</manifest>\n"))
                .issues(PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE)
                .run()
                .expect(expected);
    }

    public void testInvalidPermissionImpliesMissingUnsupportedHardware() {
        String expected =
                "AndroidManifest.xml:4: Error: Permission exists without corresponding hardware <uses-feature android:name=\"android.hardware.telephony\" required=\"false\"> tag. [PermissionImpliesUnsupportedChromeOsHardware]\n"
                        + "    <uses-permission android:name=\"android.permission.CALL_PHONE\"/>\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n";
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-permission android:name=\"android.permission.CALL_PHONE\"/>\n"
                        + "</manifest>\n"))
                .issues(PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE)
                .run()
                .expect(expected);
    }

    public void testCameraPermissionImpliesMissingUnsupportedCamera() {
        String expected =
                "AndroidManifest.xml:4: Error: Permission exists without corresponding hardware <uses-feature android:name=\"android.hardware.camera\" required=\"false\"> tag. [PermissionImpliesUnsupportedChromeOsHardware]\n"
                        + "    <uses-permission android:name=\"android.permission.CAMERA\"/>\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n";
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-permission android:name=\"android.permission.CAMERA\"/>\n"
                        + "    <uses-feature android:name=\"android.hardware.camera.autofocus\" />\n"
                        + "</manifest>\n"))
                .issues(PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE)
                .run()
                .expect(expected);
    }

    public void testCameraPermissionImpliesMissingUnsupportedCameraAutofocus() {
        String expected =
                "AndroidManifest.xml:4: Error: Permission exists without corresponding hardware <uses-feature android:name=\"android.hardware.camera.autofocus\" required=\"false\"> tag. [PermissionImpliesUnsupportedChromeOsHardware]\n"
                        + "    <uses-permission android:name=\"android.permission.CAMERA\"/>\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n";
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-permission android:name=\"android.permission.CAMERA\"/>\n"
                        + "    <uses-feature android:name=\"android.hardware.camera\" />\n"
                        + "</manifest>\n"))
                .issues(PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE)
                .run()
                .expect(expected);
    }

    public void testCameraPermissionImpliesTwoMissingUnsupportedCameraHardware() {
        String expected =
                "AndroidManifest.xml:4: Error: Permission exists without corresponding hardware <uses-feature android:name=\"android.hardware.camera\" required=\"false\"> tag. [PermissionImpliesUnsupportedChromeOsHardware]\n"
                        + "    <uses-permission android:name=\"android.permission.CAMERA\"/>\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "AndroidManifest.xml:4: Error: Permission exists without corresponding hardware <uses-feature android:name=\"android.hardware.camera.autofocus\" required=\"false\"> tag. [PermissionImpliesUnsupportedChromeOsHardware]\n"
                        + "    <uses-permission android:name=\"android.permission.CAMERA\"/>\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "2 errors, 0 warnings\n";
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-permission android:name=\"android.permission.CAMERA\"/>\n"
                        + "</manifest>\n"))
                .issues(PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE)
                .run()
                .expect(expected);
    }

    public void testCameraPermissionImpliesNotMissingUnsupportedHardware() {
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-permission android:name=\"android.permission.CAMERA\"/>\n"
                        + "    <uses-feature android:required=\"false\" android:name=\"android.hardware.camera\"/>\n"
                        + "    <uses-feature android:required=\"false\" android:name=\"android.hardware.camera.autofocus\"/>\n"
                        + "</manifest>\n"))
                .issues(PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE)
                .run()
                .expectClean();
    }

    public void testValidPermissionImpliesUnsupportedHardware() {
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-permission android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\"/>\n"
                        + "</manifest>\n"))
                .issues(PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE)
                .run()
                .expectClean();
    }
}
