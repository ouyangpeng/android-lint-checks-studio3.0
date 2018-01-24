/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static com.android.SdkConstants.APPCOMPAT_LIB_ARTIFACT_ID;
import static com.android.ide.common.fonts.FontProviderKt.GOOGLE_FONT_AUTHORITY;
import static com.android.ide.common.fonts.FontProviderKt.GOOGLE_FONT_CERTIFICATE;
import static com.android.ide.common.fonts.FontProviderKt.GOOGLE_FONT_DEVELOPMENT_CERTIFICATE;
import static com.android.ide.common.fonts.FontProviderKt.GOOGLE_FONT_NAME;
import static com.android.ide.common.fonts.FontProviderKt.GOOGLE_FONT_PACKAGE_NAME;
import static com.android.ide.common.fonts.FontProviderKt.GOOGLE_FONT_URL;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.fonts.FontLoader;
import com.android.testutils.TestUtils;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.openapi.util.io.FileUtil;
import java.io.File;
import java.io.IOException;

public class FontDetectorTest extends AbstractCheckTest {
    private static File ourMockSdk;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        FontLoader.getInstance(getSdk());
    }

    @Override
    protected Detector getDetector() {
        return new FontDetector();
    }

    public void testBothDownloadableAndFontFamilyPresent() throws Exception {
        String expected =
                ""
                        + "res/font/font1.xml:4: Error: A downloadable font cannot have a <font> sub tag [FontValidationError]\n"
                        + "    <font\n"
                        + "    ^\n"
                        + "1 errors, 0 warnings\n";
        String expectedFix =
                ""
                        + "Fix for res/font/font1.xml line 3: Replace with :\n"
                        + "@@ -4 +4\n"
                        + "-     <font\n"
                        + "-         android:fontStyle=\"normal\"\n"
                        + "-         android:fontWeight=\"400\"\n"
                        + "-         android:font=\"@font/monserrat\" />\n";
        //noinspection all // Sample code
        lint().files(
                        xml(
                                "res/font/font1.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                        + "    android:fontProviderQuery=\"Monserrat\">\n"
                                        + "    <font\n"
                                        + "        android:fontStyle=\"normal\"\n"
                                        + "        android:fontWeight=\"400\"\n"
                                        + "        android:font=\"@font/monserrat\" />\n"
                                        + "</font-family>"
                                        + "\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(expectedFix);
    }

    public void testAppCompatVersion() throws Exception {
        String expected =
                ""
                        + "res/font/font1.xml:2: Error: Using version 26.0.0-alpha7' of the appcompat-v7 library. Required version for using downloadable fonts: 26.0.0-beta1 or higher. [FontValidationError]\n"
                        + "<font-family xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                        + "^\n"
                        + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                manifest().minSdk(25),
                xml(
                        "res/font/font1.xml",
                        ""
                                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                + "<font-family xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                                + "    app:fontProviderAuthority=\"com.google.android.gms.fonts\"\n"
                                + "    app:fontProviderQuery=\"Monserrat\"\n"
                                + "    app:fontProviderPackage=\"com.google.android.gms\"\n"
                                + "    app:fontProviderCerts=\"@array/certs\">\n"
                                + "</font-family>"
                                + "\n"),
                gradle(""
                        + "apply plugin: 'com.android.application'\n"
                        + "\n"
                        + "dependencies {\n"
                        + "    compile 'com.android.support:appcompat-v7:26.0.0-alpha7\"'\n"
                        + "}"))
                .checkMessage(this::checkReportedError)
                .run()
                .expect(expected);
    }

    @Override
    protected void checkReportedError(@NonNull Context context, @NonNull Issue issue,
            @NonNull Severity severity, @NonNull Location location, @NonNull String message,
            @Nullable LintFix fixData) {
        assertTrue(fixData instanceof LintFix.DataMap);
        LintFix.DataMap map = (LintFix.DataMap) fixData;
        assertEquals(map.get(String.class), APPCOMPAT_LIB_ARTIFACT_ID);
    }

    public void testAppAttributesPresentOnApi26() throws Exception {
        String expected =
                ""
                        + "res/font/font1.xml:8: Warning: For minSdkVersion=26 only android: attributes should be used [FontValidationWarning]\n"
                        + "    app:fontProviderCerts=\"@array/certs\">\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "0 errors, 1 warnings\n";
        String expectedFix =
                ""
                        + "Fix for res/font/font1.xml line 7: Delete fontProviderCerts:\n"
                        + "@@ -7 +7\n"
                        + "-     android:fontProviderQuery=\"Monserrat\"\n"
                        + "-     app:fontProviderCerts=\"@array/certs\" >\n"
                        + "+     android:fontProviderQuery=\"Monserrat\" >\n";
        //noinspection all // Sample code
        lint().files(
                        manifest().minSdk(26),
                        xml(
                                "res/font/font1.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                        + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                                        + "    android:fontProviderAuthority=\"com.google.android.gms.fonts\"\n"
                                        + "    android:fontProviderQuery=\"Monserrat\"\n"
                                        + "    android:fontProviderPackage=\"com.google.android.gms\"\n"
                                        + "    android:fontProviderCerts=\"@array/certs\"\n"
                                        + "    app:fontProviderCerts=\"@array/certs\">\n"
                                        + "</font-family>"
                                        + "\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(expectedFix);
    }

    public void testAndroidAttributesPresentOnApi25() throws Exception {
        //noinspection all // Sample code
        String expected =
                ""
                        + "res/font/font1.xml:8: Warning: For minSdkVersion=25 only app: attributes should be used [FontValidationWarning]\n"
                        + "    android:fontProviderAuthority=\"com.google.android.gms.fonts\">\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "0 errors, 1 warnings\n";
        String expectedFix =
                ""
                        + "Fix for res/font/font1.xml line 7: Delete fontProviderAuthority:\n"
                        + "@@ -4 +4\n"
                        + "-     android:fontProviderAuthority=\"com.google.android.gms.fonts\"\n";
        lint().files(
                        manifest().minSdk(25),
                        xml(
                                "res/font/font1.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                        + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                                        + "    app:fontProviderAuthority=\"com.google.android.gms.fonts\"\n"
                                        + "    app:fontProviderQuery=\"Monserrat\"\n"
                                        + "    app:fontProviderPackage=\"com.google.android.gms\"\n"
                                        + "    app:fontProviderCerts=\"@array/certs\"\n"
                                        + "    android:fontProviderAuthority=\"com.google.android.gms.fonts\">\n"
                                        + "</font-family>"
                                        + "\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(expectedFix);
    }

    public void testMissingAttributesOnApi26() throws Exception {
        //noinspection all // Sample code
        String expected =
                ""
                        + "res/font/font1.xml:2: Error: Missing required attributes: android:fontProviderAuthority, android:fontProviderPackage, android:fontProviderCerts [FontValidationError]\n"
                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "^\n"
                        + "1 errors, 0 warnings\n";
        String expectedFix =
                ""
                        + "Fix for res/font/font1.xml line 1: Set fontProviderAuthority=\"com.google.android.gms.fonts\":\n"
                        + "@@ -3 +3\n"
                        + "+     android:fontProviderAuthority=\"com.google.android.gms.fonts\"\n"
                        + "+     android:fontProviderCerts=\"@array/com_google_android_gms_fonts_certs\"\n"
                        + "+     android:fontProviderPackage=\"com.google.android.gms\"\n";
        lint().files(
                        manifest().minSdk(26),
                        xml(
                                "res/font/font1.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                        + "    android:fontProviderQuery=\"Monserrat\">\n"
                                        + "</font-family>"
                                        + "\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(expectedFix);
    }

    public void testMissingAttributesOnApi26WithMultipleKnownProviders() throws Exception {
        //noinspection all // Sample code
        String expected =
                ""
                        + "res/font/font1.xml:2: Error: Missing required attributes: android:fontProviderAuthority, android:fontProviderPackage, android:fontProviderCerts [FontValidationError]\n"
                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "^\n"
                        + "1 errors, 0 warnings\n";
        String expectedFix =
                ""
                        + "Fix for res/font/font1.xml line 1: Set fontProviderAuthority:\n"
                        + "@@ -3 +3\n"
                        + "+     android:fontProviderAuthority=\"[TODO]|\"\n"
                        + "+     android:fontProviderCerts=\"[TODO]|\"\n"
                        + "+     android:fontProviderPackage=\"[TODO]|\"\n";
        lint().files(
                        manifest().minSdk(26),
                        xml(
                                "res/font/font1.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                        + "    android:fontProviderQuery=\"Monserrat\">\n"
                                        + "</font-family>"
                                        + "\n"))
                .sdkHome(getMockSdkWithFontProviders())
                .run()
                .expect(expected)
                .expectFixDiffs(expectedFix);
    }

    public void testMissingAttributesOnApi25() throws Exception {
        //noinspection all // Sample code
        String expected =
                ""
                        + "res/font/font1.xml:2: Error: Missing required attributes: app:fontProviderAuthority, app:fontProviderPackage, app:fontProviderCerts [FontValidationError]\n"
                        + "<font-family xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                        + "^\n"
                        + "1 errors, 0 warnings\n";
        String expectedFix =
                ""
                        + "Fix for res/font/font1.xml line 1: Set fontProviderAuthority=\"com.google.android.gms.fonts\":\n"
                        + "@@ -3 +3\n"
                        + "+     app:fontProviderAuthority=\"com.google.android.gms.fonts\"\n"
                        + "+     app:fontProviderCerts=\"@array/com_google_android_gms_fonts_certs\"\n"
                        + "+     app:fontProviderPackage=\"com.google.android.gms\"\n";
        lint().files(
                        manifest().minSdk(25),
                        xml(
                                "res/font/font1.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                                        + "    app:fontProviderQuery=\"Monserrat\">\n"
                                        + "</font-family>"
                                        + "\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(expectedFix);
    }

    public void testMissingAttributesOnApi26Preview() throws Exception {
        //noinspection all // Sample code
        String expected =
                ""
                        + "res/font/font1.xml:2: Error: Missing required attribute: app:fontProviderQuery [FontValidationError]\n"
                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "^\n"
                        + "res/font/font1.xml:2: Error: Missing required attributes: android:fontProviderPackage, android:fontProviderCerts [FontValidationError]\n"
                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "^\n"
                        + "2 errors, 0 warnings\n";
        String expectedFix =
                ""
                        + "Fix for res/font/font1.xml line 1: Set fontProviderQuery:\n"
                        + "@@ -8 +8\n"
                        + "-     app:fontProviderPackage=\"com.google.android.gms\" >\n"
                        + "+     app:fontProviderPackage=\"com.google.android.gms\"\n"
                        + "+     app:fontProviderQuery=\"[TODO]|\" >\n"
                        + "Fix for res/font/font1.xml line 1: Set fontProviderPackage=\"com.google.android.gms\":\n"
                        + "@@ -5 +5\n"
                        + "+     android:fontProviderCerts=\"@array/com_google_android_gms_fonts_certs\"\n"
                        + "+     android:fontProviderPackage=\"com.google.android.gms\"\n";
        lint().files(
                        manifest().minSdk("O"),
                        xml(
                                "res/font/font1.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                        + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                                        + "    android:fontProviderAuthority=\"com.google.android.gms.fonts\"\n"
                                        + "    android:fontProviderQuery=\"Monserrat\"\n"
                                        + "    app:fontProviderAuthority=\"com.google.android.gms.fonts\"\n"
                                        + "    app:fontProviderPackage=\"com.google.android.gms\"\n"
                                        + "    app:fontProviderCerts=\"@array/certs\">\n"
                                        + "</font-family>"
                                        + "\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(expectedFix);
    }

    public void testUnknownAuthorityOnApi25() throws Exception {
        //noinspection all // Sample code
        String expected =
                ""
                        + "res/font/font1.xml:3: Error: Unknown font provider authority [FontValidationError]\n"
                        + "    app:fontProviderAuthority=\"com.google.android.gms.helpme\"\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n";
        String expectedFix =
                ""
                        + "Fix for res/font/font1.xml line 2: Replace with com.google.android.gms.fonts:\n"
                        + "@@ -3 +3\n"
                        + "-     app:fontProviderAuthority=\"com.google.android.gms.helpme\"\n"
                        + "+     app:fontProviderAuthority=\"com.google.android.gms.fonts\"\n";
        lint().files(
                        manifest().minSdk(25),
                        xml(
                                "res/font/font1.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                                        + "    app:fontProviderAuthority=\"com.google.android.gms.helpme\"\n"
                                        + "    app:fontProviderPackage=\"com.google.android.gms\"\n"
                                        + "    app:fontProviderCerts=\"@array/certs\"\n"
                                        + "    app:fontProviderQuery=\"Monserrat\">\n"
                                        + "</font-family>"
                                        + "\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(expectedFix);
    }

    public void testUnknownAuthorityOnApi26() throws Exception {
        //noinspection all // Sample code
        String expected =
                ""
                        + "res/font/font1.xml:3: Error: Unknown font provider authority [FontValidationError]\n"
                        + "    android:fontProviderAuthority=\"com.google.android.gms.helpme\"\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n";
        String expectedFix =
                ""
                        + "Fix for res/font/font1.xml line 2: Replace with com.google.android.gms.fonts:\n"
                        + "@@ -3 +3\n"
                        + "-     android:fontProviderAuthority=\"com.google.android.gms.helpme\"\n"
                        + "+     android:fontProviderAuthority=\"com.google.android.gms.fonts\"\n";
        lint().files(
                        manifest().minSdk(26),
                        xml(
                                "res/font/font1.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                        + "    android:fontProviderAuthority=\"com.google.android.gms.helpme\"\n"
                                        + "    android:fontProviderPackage=\"com.google.android.gms\"\n"
                                        + "    android:fontProviderCerts=\"@array/certs\"\n"
                                        + "    android:fontProviderQuery=\"Monserrat\">\n"
                                        + "</font-family>"
                                        + "\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(expectedFix);
    }

    public void testDualAuthorityOnApi26Preview() throws Exception {
        //noinspection all // Sample code
        String expected =
                ""
                        + "res/font/font1.xml:8: Error: Unexpected font provider authority [FontValidationError]\n"
                        + "    app:fontProviderAuthority=\"com.alternate.font.provider\"\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n";
        String expectedFix =
                ""
                        + "Fix for res/font/font1.xml line 7: Replace with com.google.android.gms.fonts:\n"
                        + "@@ -8 +8\n"
                        + "-     app:fontProviderAuthority=\"com.alternate.font.provider\"\n"
                        + "+     app:fontProviderAuthority=\"com.google.android.gms.fonts\"\n";
        lint().files(
                        manifest().minSdk("O"),
                        xml(
                                "res/font/font1.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                        + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                                        + "    android:fontProviderAuthority=\"com.google.android.gms.fonts\"\n"
                                        + "    android:fontProviderPackage=\"com.google.android.gms\"\n"
                                        + "    android:fontProviderCerts=\"@array/certs\"\n"
                                        + "    android:fontProviderQuery=\"Montserrat\"\n"
                                        + "    app:fontProviderAuthority=\"com.alternate.font.provider\"\n"
                                        + "    app:fontProviderPackage=\"com.alternate.font\"\n"
                                        + "    app:fontProviderCerts=\"@array/certs\"\n"
                                        + "    app:fontProviderQuery=\"Biryani\">\n"
                                        + "</font-family>"
                                        + "\n"))
                .sdkHome(getMockSdkWithFontProviders())
                .run()
                .expect(expected)
                .expectFixDiffs(expectedFix);
    }

    public void testPackageMismatchOnApi25() throws Exception {
        //noinspection all // Sample code
        String expected =
                ""
                        + "res/font/font1.xml:4: Error: Unexpected font provider package [FontValidationError]\n"
                        + "    app:fontProviderPackage=\"com.google.android.gms.fonts\"\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n";
        String expectedFix =
                ""
                        + "Fix for res/font/font1.xml line 3: Replace with com.google.android.gms:\n"
                        + "@@ -4 +4\n"
                        + "-     app:fontProviderPackage=\"com.google.android.gms.fonts\"\n"
                        + "+     app:fontProviderPackage=\"com.google.android.gms\"\n";
        lint().files(
                        manifest().minSdk(25),
                        xml(
                                "res/font/font1.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                                        + "    app:fontProviderAuthority=\"com.google.android.gms.fonts\"\n"
                                        + "    app:fontProviderPackage=\"com.google.android.gms.fonts\"\n"
                                        + "    app:fontProviderCerts=\"@array/certs\"\n"
                                        + "    app:fontProviderQuery=\"Monserrat\">\n"
                                        + "</font-family>"
                                        + "\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(expectedFix);
    }

    public void testPackageMismatchOnApi26() throws Exception {
        //noinspection all // Sample code
        String expected =
                ""
                        + "res/font/font1.xml:4: Error: Unexpected font provider package [FontValidationError]\n"
                        + "    android:fontProviderPackage=\"com.google.android.gms.fonts\"\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n";
        String expectedFix =
                ""
                        + "Fix for res/font/font1.xml line 3: Replace with com.google.android.gms:\n"
                        + "@@ -4 +4\n"
                        + "-     android:fontProviderPackage=\"com.google.android.gms.fonts\"\n"
                        + "+     android:fontProviderPackage=\"com.google.android.gms\"\n";
        lint().files(
                        manifest().minSdk(26),
                        xml(
                                "res/font/font1.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                        + "    android:fontProviderAuthority=\"com.google.android.gms.fonts\"\n"
                                        + "    android:fontProviderPackage=\"com.google.android.gms.fonts\"\n"
                                        + "    android:fontProviderCerts=\"@array/certs\"\n"
                                        + "    android:fontProviderQuery=\"Monserrat\">\n"
                                        + "</font-family>"
                                        + "\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(expectedFix);
    }

    public void testMissingQuery() throws Exception {
        //noinspection all // Sample code
        String expected =
                ""
                        + "res/font/font1.xml:6: Error: Missing provider query [FontValidationError]\n"
                        + "    android:fontProviderQuery=\"\">\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n";
        String expectedFix =
                ""
                        + "Fix for res/font/font1.xml line 5: Set fontProviderQuery:\n"
                        + "@@ -6 +6\n"
                        + "-     android:fontProviderQuery=\"\" >\n"
                        + "+     android:fontProviderQuery=\"[TODO]|\" >\n";
        lint().files(
                        manifest().minSdk(26),
                        xml(
                                "res/font/font1.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                        + "    android:fontProviderAuthority=\"com.google.android.gms.fonts\"\n"
                                        + "    android:fontProviderPackage=\"com.google.android.gms\"\n"
                                        + "    android:fontProviderCerts=\"@array/certs\"\n"
                                        + "    android:fontProviderQuery=\"\">\n"
                                        + "</font-family>"
                                        + "\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(expectedFix);
    }

    public void testQueryErrorV11() throws Exception {
        //noinspection all // Sample code
        String expected =
                ""
                        + "res/font/font1.xml:6: Error: Unexpected keyword: size expected one of: width, weight, italic, besteffort [FontValidationError]\n"
                        + "    android:fontProviderQuery=\"name=Monserrat&amp;size=15\">\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n";
        lint().files(
                        manifest().minSdk(26),
                        xml(
                                "res/font/font1.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                        + "    android:fontProviderAuthority=\"com.google.android.gms.fonts\"\n"
                                        + "    android:fontProviderPackage=\"com.google.android.gms\"\n"
                                        + "    android:fontProviderCerts=\"@array/certs\"\n"
                                        + "    android:fontProviderQuery=\"name=Monserrat&amp;size=15\">\n"
                                        + "</font-family>"
                                        + "\n"))
                .run()
                .expect(expected);
    }

    public void testQueryErrorV12() throws Exception {
        //noinspection all // Sample code
        String expected =
                ""
                        + "res/font/font1.xml:6: Error: Unexpected keyword: Aladin expected one of: wght, wdth, ital, bold, exact, nearest [FontValidationError]\n"
                        + "    android:fontProviderQuery=\"Monserrat:300i,Aladin:200\">\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n";
        lint().files(
                        manifest().minSdk(26),
                        xml(
                                "res/font/font1.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                        + "    android:fontProviderAuthority=\"com.google.android.gms.fonts\"\n"
                                        + "    android:fontProviderPackage=\"com.google.android.gms\"\n"
                                        + "    android:fontProviderCerts=\"@array/certs\"\n"
                                        + "    android:fontProviderQuery=\"Monserrat:300i,Aladin:200\">\n"
                                        + "</font-family>"
                                        + "\n"))
                .run()
                .expect(expected);
    }

    public void testQueryUsingUnknownFont() throws Exception {
        //noinspection all // Sample code
        String expected =
                ""
                        + "res/font/font1.xml:6: Error: Unknown font: NewUnknownFontName [FontValidationError]\n"
                        + "    android:fontProviderQuery=\"NewUnknownFontName\">\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n";
        lint().files(
                        manifest().minSdk(26),
                        xml(
                                "res/font/font1.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                        + "    android:fontProviderAuthority=\"com.google.android.gms.fonts\"\n"
                                        + "    android:fontProviderPackage=\"com.google.android.gms\"\n"
                                        + "    android:fontProviderCerts=\"@array/certs\"\n"
                                        + "    android:fontProviderQuery=\"NewUnknownFontName\">\n"
                                        + "</font-family>"
                                        + "\n"))
                .sdkHome(getMockSdkWithFontProviders())
                .run()
                .expect(expected);
    }

    public void testQueryNearMatchWarning() throws Exception {
        //noinspection all // Sample code
        String expected =
                ""
                        + "res/font/font1.xml:6: Warning: No exact match found for: Montserrat [FontValidationWarning]\n"
                        + "    android:fontProviderQuery=\"name=Montserrat&amp;weight=600\">\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "0 errors, 1 warnings\n";
        String expectedFix =
                ""
                        + "Fix for res/font/font1.xml line 5: Replace with closest font: name=Montserrat&weight=700:\n"
                        + "@@ -6 +6\n"
                        + "-     android:fontProviderQuery=\"name=Montserrat&amp;weight=600\" >\n"
                        + "+     android:fontProviderQuery=\"name=Montserrat&amp;weight=700\" >\n";
        lint().files(
                        manifest().minSdk(26),
                        xml(
                                "res/font/font1.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                        + "    android:fontProviderAuthority=\"com.google.android.gms.fonts\"\n"
                                        + "    android:fontProviderPackage=\"com.google.android.gms\"\n"
                                        + "    android:fontProviderCerts=\"@array/certs\"\n"
                                        + "    android:fontProviderQuery=\"name=Montserrat&amp;weight=600\">\n"
                                        + "</font-family>"
                                        + "\n"))
                .sdkHome(getMockSdkWithFontProviders())
                .run()
                .expect(expected)
                .expectFixDiffs(expectedFix);
    }

    public void testQueryNearMatchError() throws Exception {
        //noinspection all // Sample code
        String expected =
                ""
                        + "res/font/font1.xml:6: Error: No exact match found for: Montserrat [FontValidationError]\n"
                        + "    android:fontProviderQuery=\"name=Montserrat&amp;weight=600&amp;besteffort=false\">\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n";
        String expectedFix =
                ""
                        + "Fix for res/font/font1.xml line 5: Replace with closest font: name=Montserrat&weight=700&besteffort=false:\n"
                        + "@@ -6 +6\n"
                        + "-     android:fontProviderQuery=\"name=Montserrat&amp;weight=600&amp;besteffort=false\" >\n"
                        + "+     android:fontProviderQuery=\"name=Montserrat&amp;weight=700&amp;besteffort=false\" >\n";
        lint().files(
                        manifest().minSdk(26),
                        xml(
                                "res/font/font1.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                        + "    android:fontProviderAuthority=\"com.google.android.gms.fonts\"\n"
                                        + "    android:fontProviderPackage=\"com.google.android.gms\"\n"
                                        + "    android:fontProviderCerts=\"@array/certs\"\n"
                                        + "    android:fontProviderQuery=\"name=Montserrat&amp;weight=600&amp;besteffort=false\">\n"
                                        + "</font-family>"
                                        + "\n"))
                .sdkHome(getMockSdkWithFontProviders())
                .run()
                .expect(expected)
                .expectFixDiffs(expectedFix);
    }

    public void testDualQueryOnApi26Preview() throws Exception {
        //noinspection all // Sample code
        String expected =
                ""
                        + "res/font/font1.xml:11: Error: Unexpected query [FontValidationError]\n"
                        + "    app:fontProviderQuery=\"Dosis\">\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n";
        String expectedFix =
                ""
                        + "Fix for res/font/font1.xml line 10: Replace with Montserrat:\n"
                        + "@@ -11 +11\n"
                        + "-     app:fontProviderQuery=\"Dosis\" >\n"
                        + "+     app:fontProviderQuery=\"Montserrat\" >\n";
        lint().files(
                        manifest().minSdk("O"),
                        xml(
                                "res/font/font1.xml",
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                        + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n"
                                        + "    android:fontProviderAuthority=\"com.google.android.gms.fonts\"\n"
                                        + "    android:fontProviderPackage=\"com.google.android.gms\"\n"
                                        + "    android:fontProviderCerts=\"@array/certs\"\n"
                                        + "    android:fontProviderQuery=\"Montserrat\"\n"
                                        + "    app:fontProviderAuthority=\"com.google.android.gms.fonts\"\n"
                                        + "    app:fontProviderPackage=\"com.google.android.gms\"\n"
                                        + "    app:fontProviderCerts=\"@array/certs\"\n"
                                        + "    app:fontProviderQuery=\"Dosis\">\n"
                                        + "</font-family>"
                                        + "\n"))
                .sdkHome(getMockSdkWithFontProviders())
                .run()
                .expect(expected)
                .expectFixDiffs(expectedFix);
    }

    private static File getMockSdkWithFontProviders() throws IOException {
        if (ourMockSdk == null) {
            ourMockSdk = createMockSdkWithFontProviders();
        }
        return ourMockSdk;
    }

    private static File createMockSdkWithFontProviders() throws IOException {
        File sdkRootDir = TestUtils.createTempDirDeletedOnExit();

        File fontDir = new File(sdkRootDir, "fonts");
        File providersDir = new File(fontDir, "providers");
        File providersFile = new File(providersDir, "provider_directory.xml");

        FileUtil.writeToFile(
                providersFile,
                ""
                        + "<provider_directory>\n"
                        + "    <providers>\n"
                        + "        <provider name=\"Alternate\" authority=\"com.alternate.font.provider\" package=\"com.alternate.font\" url=\"\" cert=\"\" dev_cert=\"\"/>\n"
                        + "        <provider name=\""
                        + GOOGLE_FONT_NAME
                        + "\" authority=\""
                        + GOOGLE_FONT_AUTHORITY
                        + "\" package=\""
                        + GOOGLE_FONT_PACKAGE_NAME
                        + "\" url=\""
                        + GOOGLE_FONT_URL
                        + "\" cert=\""
                        + GOOGLE_FONT_CERTIFICATE
                        + "\" dev_cert=\""
                        + GOOGLE_FONT_DEVELOPMENT_CERTIFICATE
                        + "\"/>\n"
                        + "    </providers>\n"
                        + "</provider_directory>\n");

        File googleFontDir = new File(fontDir, GOOGLE_FONT_AUTHORITY);
        File googleDirectory = new File(googleFontDir, "directory");
        File googleDirectoryFile = new File(googleDirectory, "font_directory.xml");

        FileUtil.writeToFile(
                googleDirectoryFile,
                ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<font_directory version='1'>\n"
                        + "    <families>\n"
                        + "        <family name='Dosis' menu='//fonts.gstatic.com/s/dosis/v6/0bOjty8DEhwcbM3d4ICpMQ.ttf'>\n"
                        + "            <font weight='200' width='100.0' italic='0.0' styleName='ExtraLight' url='//fonts.gstatic.com/s/dosis/v6/ztftab0r6hcd7AeurUGrSQ.ttf'/>\n"
                        + "            <font weight='300' width='100.0' italic='0.0' styleName='Light' url='//fonts.gstatic.com/s/dosis/v6/awIB6L0h5mb0plIKorXmuA.ttf'/>\n"
                        + "            <font weight='400' width='100.0' italic='0.0' styleName='Regular' url='//fonts.gstatic.com/s/dosis/v6/rJRlixu-w0JZ1MyhJpao_Q.ttf'/>\n"
                        + "            <font weight='500' width='100.0' italic='0.0' styleName='Medium' url='//fonts.gstatic.com/s/dosis/v6/ruEXDOFMxDPGnjCBKRqdAQ.ttf'/>\n"
                        + "            <font weight='600' width='100.0' italic='0.0' styleName='SemiBold' url='//fonts.gstatic.com/s/dosis/v6/KNAswRNwm3tfONddYyidxg.ttf'/>\n"
                        + "            <font weight='700' width='100.0' italic='0.0' styleName='Bold' url='//fonts.gstatic.com/s/dosis/v6/AEEAj0ONidK8NQQMBBlSig.ttf'/>\n"
                        + "            <font weight='800' width='100.0' italic='0.0' styleName='ExtraBold' url='//fonts.gstatic.com/s/dosis/v6/nlrKd8E69vvUU39XGsvR7Q.ttf'/>\n"
                        + "        </family>\n"
                        + "        <family name='Montserrat' menu='//fonts.gstatic.com/s/montserrat/v10/sp_E9wlxrieAFN73uHUfti3USBnSvpkopQaUR-2r7iU.ttf'>\n"
                        + "            <font weight='400' width='100.0' italic='0.0' styleName='Regular' url='//fonts.gstatic.com/s/montserrat/v10/Kqy6-utIpx_30Xzecmeo8_esZW2xOQ-xsNqO47m55DA.ttf'/>\n"
                        + "            <font weight='700' width='100.0' italic='0.0' styleName='Bold' url='//fonts.gstatic.com/s/montserrat/v10/IQHow_FEYlDC4Gzy_m8fcgJKKGfqHaYFsRG-T3ceEVo.ttf'/>\n"
                        + "        </family>\n"
                        + "    </families>\n"
                        + "</font_directory>\n");

        File alternateFontDir = new File(fontDir, "com.alternate.font.provider");
        File alternateDirectory = new File(alternateFontDir, "directory");
        File alternateDirectoryFile = new File(alternateDirectory, "font_directory.xml");

        FileUtil.writeToFile(
                alternateDirectoryFile,
                ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<font_directory version='1'>\n"
                        + "    <families>\n"
                        + "        <family name='BioRhyme' menu='//fonts.gstatic.com/s/biorhyme/v1/S5TAweMZQtbEzQHWntc2svesZW2xOQ-xsNqO47m55DA.ttf'>\n"
                        + "            <font weight='200' width='100.0' italic='0.0' styleName='ExtraLight' url='//fonts.gstatic.com/s/biorhyme/v1/bj-6g_1gJHCc9xQZtLWL36CWcynf_cDxXwCLxiixG1c.ttf'/>\n"
                        + "            <font weight='300' width='100.0' italic='0.0' styleName='Light' url='//fonts.gstatic.com/s/biorhyme/v1/jWqHmLFlu30n7xp12uZd8qCWcynf_cDxXwCLxiixG1c.ttf'/>\n"
                        + "            <font weight='400' width='100.0' italic='0.0' styleName='Regular' url='//fonts.gstatic.com/s/biorhyme/v1/n6v5UkVPy_CjbP3fvsu1CA.ttf'/>\n"
                        + "            <font weight='700' width='100.0' italic='0.0' styleName='Bold' url='//fonts.gstatic.com/s/biorhyme/v1/36KN76U1iKt5TFDm2lBz0KCWcynf_cDxXwCLxiixG1c.ttf'/>\n"
                        + "            <font weight='800' width='100.0' italic='0.0' styleName='ExtraBold' url='//fonts.gstatic.com/s/biorhyme/v1/k6bYbUnESjLYnworWvSTL6CWcynf_cDxXwCLxiixG1c.ttf'/>\n"
                        + "            <font weight='200' width='125.0' italic='0.0' styleName='Expanded ExtraLight' url='//fonts.gstatic.com/s/biorhymeexpanded/v1/FKL4Vyxmq2vsiDrSOzz2sC7oxZzNh3ej55UHm-HviBI.ttf'/>\n"
                        + "            <font weight='300' width='125.0' italic='0.0' styleName='Expanded Light' url='//fonts.gstatic.com/s/biorhymeexpanded/v1/FKL4Vyxmq2vsiDrSOzz2sFu4cYPPksG4MRjB5UiYPPw.ttf'/>\n"
                        + "            <font weight='400' width='125.0' italic='0.0' styleName='Expanded Regular' url='//fonts.gstatic.com/s/biorhymeexpanded/v1/hgBNpgjTRZzGmZxqN5OuVjndr_hij4ilAk2n1d1AhsE.ttf'/>\n"
                        + "            <font weight='700' width='125.0' italic='0.0' styleName='Expanded Bold' url='//fonts.gstatic.com/s/biorhymeexpanded/v1/FKL4Vyxmq2vsiDrSOzz2sMVisRVfPEfQ0jijOMQbr0Q.ttf'/>\n"
                        + "            <font weight='800' width='125.0' italic='0.0' styleName='Expanded ExtraBold' url='//fonts.gstatic.com/s/biorhymeexpanded/v1/FKL4Vyxmq2vsiDrSOzz2sIv1v1eCT6RPbcYZYQ1T1CE.ttf'/>\n"
                        + "        </family>\n"
                        + "        <family name='Biryani' menu='//fonts.gstatic.com/s/biryani/v1/rW_FlEaEcKjjRYlyQSNuAg.ttf'>\n"
                        + "            <font weight='200' width='100.0' italic='0.0' styleName='ExtraLight' url='//fonts.gstatic.com/s/biryani/v1/Xx38YzyTFF8n6mRS1Yd88vesZW2xOQ-xsNqO47m55DA.ttf'/>\n"
                        + "            <font weight='300' width='100.0' italic='0.0' styleName='Light' url='//fonts.gstatic.com/s/biryani/v1/u-bneRbizmFMd0VQp5Ze6vesZW2xOQ-xsNqO47m55DA.ttf'/>\n"
                        + "            <font weight='400' width='100.0' italic='0.0' styleName='Regular' url='//fonts.gstatic.com/s/biryani/v1/W7bfR8-IY76Xz0QoB8L2xw.ttf'/>\n"
                        + "            <font weight='600' width='100.0' italic='0.0' styleName='SemiBold' url='//fonts.gstatic.com/s/biryani/v1/1EdcPCVxBR2txgjrza6_YPesZW2xOQ-xsNqO47m55DA.ttf'/>\n"
                        + "            <font weight='700' width='100.0' italic='0.0' styleName='Bold' url='//fonts.gstatic.com/s/biryani/v1/qN2MTZ0j1sKSCtfXLB2dR_esZW2xOQ-xsNqO47m55DA.ttf'/>\n"
                        + "            <font weight='800' width='100.0' italic='0.0' styleName='ExtraBold' url='//fonts.gstatic.com/s/biryani/v1/DJyziS7FEy441v22InYdevesZW2xOQ-xsNqO47m55DA.ttf'/>\n"
                        + "            <font weight='900' width='100.0' italic='0.0' styleName='Black' url='//fonts.gstatic.com/s/biryani/v1/trcLkrIut0lM_PPSyQfAMPesZW2xOQ-xsNqO47m55DA.ttf'/>\n"
                        + "        </family>\n"
                        + "    </families>\n"
                        + "</font_directory>\n");

        return sdkRootDir;
    }
}
