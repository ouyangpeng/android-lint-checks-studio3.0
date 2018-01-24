/*
 * Copyright (C) 2015 The Android Open Source Project
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

import static com.android.tools.lint.checks.AndroidTvDetector.IMPLIED_TOUCHSCREEN_HARDWARE;
import static com.android.tools.lint.checks.AndroidTvDetector.MISSING_BANNER;
import static com.android.tools.lint.checks.AndroidTvDetector.MISSING_LEANBACK_LAUNCHER;
import static com.android.tools.lint.checks.AndroidTvDetector.MISSING_LEANBACK_SUPPORT;
import static com.android.tools.lint.checks.AndroidTvDetector.PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE;
import static com.android.tools.lint.checks.AndroidTvDetector.UNSUPPORTED_TV_HARDWARE;

import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings("javadoc")
public class AndroidTvDetectorTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new AndroidTvDetector();
    }

    public void testInvalidNoLeanbackActivity() {
        String expected =
                "AndroidManifest.xml:2: Error: Expecting an activity to have android.intent.category.LEANBACK_LAUNCHER intent filter. [MissingLeanbackLauncher]\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "^\n"
                        + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-feature android:name=\"android.software.leanback\"/>\n"
                        + "    <application>\n"
                        + "        <!-- Application contains an activity, but it isn't a leanback launcher activity -->\n"
                        + "        <activity android:name=\"com.example.android.test.Activity\">\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.SEND\"/>\n"
                        + "                <category android:name=\"android.intent.category.DEFAULT\"/>\n"
                        + "                <data android:mimeType=\"text/plain\"/>\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "</manifest>\n"))
                .issues(MISSING_LEANBACK_LAUNCHER)
                .run()
                .expect(expected);
    }

    public void testValidLeanbackActivity() {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-feature android:name=\"android.software.leanback\"/>\n"
                        + "    <application>\n"
                        + "        <activity android:name=\"com.example.android.TvActivity\">\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                        + "                <category android:name=\"android.intent.category.LEANBACK_LAUNCHER\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "</manifest>\n"))
                .issues(MISSING_LEANBACK_LAUNCHER)
                .run()
                .expectClean();
    }

    public void testValidLeanbackActivityAlias() {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-feature android:name=\"android.software.leanback\"/>\n"
                        + "    <application>\n"
                        + "        <activity android:name=\".ui.TvActivity\" />\n"
                        + "        <activity-alias "
                        + "            android:name=\".TvActivity\"\n"
                        + "            android:targetActivity=\".ui.TvActivity\">\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                        + "                <category android:name=\"android.intent.category.LEANBACK_LAUNCHER\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity-alias>\n"
                        + "    </application>\n"
                        + "</manifest>\n"))
                .issues(MISSING_LEANBACK_LAUNCHER)
                .run()
                .expectClean();
    }

    public void testInvalidNoUsesFeatureLeanback() {
        String expected =
                "AndroidManifest.xml:2: Error: Expecting <uses-feature android:name=\"android.software.leanback\" android:required=\"false\" /> tag. [MissingLeanbackSupport]\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "^\n"
                        + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <application>\n"
                        + "        <activity android:name=\"com.example.android.TvActivity\">\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                        + "                <category android:name=\"android.intent.category.LEANBACK_LAUNCHER\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "</manifest>\n"))
                .issues(MISSING_LEANBACK_SUPPORT)
                .run()
                .expect(expected);
    }

    public void testValidUsesFeatureLeanback() {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-feature android:name=\"android.software.leanback\" android:required=\"false\" />\n"
                        + "</manifest>\n"))
                .issues(MISSING_LEANBACK_SUPPORT)
                .run()
                .expectClean();
    }

    public void testInvalidUnsupportedHardware() {
        String expected =
                "AndroidManifest.xml:6: Error: Expecting android:required=\"false\" for this hardware feature that may not be supported by all Android TVs. [UnsupportedTvHardware]\n"
                        + "        android:name=\"android.hardware.touchscreen\" android:required=\"true\"/>\n"
                        + "                                                    ~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-feature android:name=\"android.software.leanback\"/>\n"
                        + "    <uses-feature\n"
                        + "        android:name=\"android.hardware.touchscreen\" android:required=\"true\"/>\n"
                        + "\n"
                        + "</manifest>\n"))
                .issues(UNSUPPORTED_TV_HARDWARE)
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for AndroidManifest.xml line 5: Set required=\"false\":\n"
                        + "@@ -8 +8\n"
                        + "-         android:required=\"true\" />\n"
                        + "+         android:required=\"false\" />\n");
    }

    public void testValidUnsupportedHardware() {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-feature android:name=\"android.software.leanback\"/>\n"
                        + "    <uses-feature\n"
                        + "        android:name=\"android.hardware.touchscreen\"\n"
                        + "        android:required=\"false\" />\n"
                        + "</manifest>\n"))
                .issues(UNSUPPORTED_TV_HARDWARE)
                .run()
                .expectClean();
    }

    public void testMissingUsesFeatureTouchScreen() {
        String expected =
                "AndroidManifest.xml:2: Error: Hardware feature android.hardware.touchscreen not explicitly marked as optional  [ImpliedTouchscreenHardware]\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "^\n"
                        + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-feature android:name=\"android.software.leanback\"/>\n"
                        + "    <uses-feature android:name=\"android.hardware.telephony\"/>\n"
                        + "</manifest>\n"))
                .issues(IMPLIED_TOUCHSCREEN_HARDWARE)
                .run()
                .expect(expected);
    }

    public void testValidUsesFeatureTouchScreen() {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-feature android:name=\"android.software.leanback\"/>\n"
                        + "    <uses-feature\n"
                        + "            android:name=\"android.hardware.touchscreen\"\n"
                        + "            android:required=\"false\"/>\n"
                        + "</manifest>\n"))
                .issues(UNSUPPORTED_TV_HARDWARE)
                .run()
                .expectClean();
    }

    public void testValidPermissionImpliesNotMissingUnsupportedHardware() {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-feature android:name=\"android.software.leanback\"/>\n"
                        + "    <uses-permission android:name=\"android.permission.CALL_PHONE\"/>\n"
                        + "    <uses-feature android:required=\"false\" android:name=\"android.hardware.telephony\"/>\n"
                        + "</manifest>\n"))
                .issues(PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE)
                .run()
                .expectClean();
    }

    public void testInvalidPermissionImpliesNotMissingUnsupportedHardware() {
        String expected =
                "AndroidManifest.xml:5: Warning: Permission exists without corresponding hardware <uses-feature android:name=\"android.hardware.telephony\" required=\"false\"> tag. [PermissionImpliesUnsupportedHardware]\n"
                        + "    <uses-permission android:name=\"android.permission.CALL_PHONE\"/>\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-feature android:name=\"android.software.leanback\"/>\n"
                        + "    <uses-permission android:name=\"android.permission.CALL_PHONE\"/>\n"
                        + "</manifest>\n"))
                .issues(PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE)
                .run()
                .expect(expected);
    }

    public void testInvalidPermissionImpliesMissingUnsupportedHardware() {
        String expected =
                "AndroidManifest.xml:5: Warning: Permission exists without corresponding hardware <uses-feature android:name=\"android.hardware.telephony\" required=\"false\"> tag. [PermissionImpliesUnsupportedHardware]\n"
                        + "    <uses-permission android:name=\"android.permission.CALL_PHONE\"/>\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-feature android:name=\"android.software.leanback\"/>\n"
                        + "    <uses-permission android:name=\"android.permission.CALL_PHONE\"/>\n"
                        + "</manifest>\n"))
                .issues(PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE)
                .run()
                .expect(expected);
    }

    public void testValidPermissionImpliesUnsupportedHardware() {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-feature android:name=\"android.software.leanback\"/>\n"
                        + "    <uses-permission android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\"/>\n"
                        + "</manifest>\n"))
                .issues(PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE)
                .run()
                .expectClean();
    }

    public void testBannerMissingInApplicationTag() {
        String expected =
                "AndroidManifest.xml:5: Error: Expecting android:banner with the <application> tag or each Leanback launcher activity. [MissingTvBanner]\n"
                        + "    <application>\n"
                        + "    ^\n"
                        + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-feature android:name=\"android.software.leanback\"/>\n"
                        + "    <application>\n"
                        + "        <activity>\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.MAIN\"/>\n"
                        + "                <category android:name=\"android.intent.category.LEANBACK_LAUNCHER\"/>\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "</manifest>\n"))
                .issues(MISSING_BANNER)
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for AndroidManifest.xml line 4: Set banner:\n"
                        + "@@ -7 +7\n"
                        + "-     <application>\n"
                        + "+     <application android:banner=\"[TODO]|\" >\n");
    }

    public void testBannerInLeanbackLauncherActivity() {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-feature android:name=\"android.software.leanback\"/>\n"
                        + "    <application>\n"
                        + "        <activity android:banner=\"@drawable/banner\">\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.MAIN\"/>\n"
                        + "                <category android:name=\"android.intent.category.LEANBACK_LAUNCHER\"/>\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "</manifest>\n"))
                .issues(MISSING_BANNER)
                .run()
                .expectClean();
    }

    public void testBannerInApplicationTag() {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-feature android:name=\"android.software.leanback\"/>\n"
                        + "    <application android:banner=\"@drawable/banner\">\n"
                        + "        <activity>\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.MAIN\"/>\n"
                        + "                <category android:name=\"android.intent.category.LEANBACK_LAUNCHER\"/>\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "</manifest>\n"))
                .issues(MISSING_BANNER)
                .run()
                .expectClean();
    }

    // Implicit trigger tests

    public void testLeanbackSupportTrigger() {
        // Expect some issue to be raised when there is the leanback support trigger.
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-feature android:name=\"android.software.leanback\"/>\n"
                        + "    <uses-permission android:name=\"android.permission.CALL_PHONE\"/>\n"
                        + "</manifest>\n"))
                .issues(PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE)
                .run()
                .expectWarningCount(1);

        // Expect no warnings when there is no trigger.
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-permission android:name=\"android.permission.CALL_PHONE\"/>\n"
                        + "</manifest>\n"))
                .issues(PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE)
                .run()
                .expectClean();
    }

    public void testLeanbackLauncherTrigger() {
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <application>\n"
                        + "        <activity android:name=\"com.example.android.TvActivity\">\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                        + "                <category android:name=\"android.intent.category.LEANBACK_LAUNCHER\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "</manifest>\n"))
                .issues(MISSING_LEANBACK_SUPPORT)
                .run()
                .expectErrorCount(1);

        // Expect no warnings when there is no trigger.
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\">\n"
                        + "    <uses-permission android:name=\"android.permission.CALL_PHONE\"/>\n"
                        + "</manifest>\n"))
                .issues(MISSING_LEANBACK_SUPPORT)
                .run()
                .expectClean();
    }
}
