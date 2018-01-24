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

import static com.android.tools.lint.checks.IconDetector.DUPLICATES_CONFIGURATIONS;
import static com.android.tools.lint.checks.IconDetector.DUPLICATES_NAMES;
import static com.android.tools.lint.checks.IconDetector.GIF_USAGE;
import static com.android.tools.lint.checks.IconDetector.ICON_COLORS;
import static com.android.tools.lint.checks.IconDetector.ICON_DENSITIES;
import static com.android.tools.lint.checks.IconDetector.ICON_DIP_SIZE;
import static com.android.tools.lint.checks.IconDetector.ICON_EXPECTED_SIZE;
import static com.android.tools.lint.checks.IconDetector.ICON_EXTENSION;
import static com.android.tools.lint.checks.IconDetector.ICON_LAUNCHER_SHAPE;
import static com.android.tools.lint.checks.IconDetector.ICON_LOCATION;
import static com.android.tools.lint.checks.IconDetector.ICON_MISSING_FOLDER;
import static com.android.tools.lint.checks.IconDetector.ICON_MIX_9PNG;
import static com.android.tools.lint.checks.IconDetector.ICON_NODPI;
import static com.android.tools.lint.checks.IconDetector.ICON_XML_AND_PNG;
import static com.android.tools.lint.checks.IconDetector.WEBP_ELIGIBLE;
import static com.android.tools.lint.checks.IconDetector.WEBP_UNSUPPORTED;

import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;

@SuppressWarnings("javadoc")
public class IconDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new IconDetector();
    }

    private static final Issue[] ALL_ISSUES = new Issue[] {
            DUPLICATES_CONFIGURATIONS,
            DUPLICATES_NAMES,
            GIF_USAGE,
            ICON_DENSITIES,
            ICON_DIP_SIZE,
            ICON_EXTENSION,
            ICON_LOCATION,
            ICON_MISSING_FOLDER,
            ICON_NODPI,
            ICON_COLORS,
            ICON_XML_AND_PNG,
            ICON_LAUNCHER_SHAPE,
            ICON_MIX_9PNG,
            WEBP_ELIGIBLE,
            WEBP_UNSUPPORTED
    };

    public void test() throws Exception {
        String expected = ""
                + "res/drawable-mdpi/sample_icon.gif: Warning: Using the .gif format for bitmaps is discouraged [GifUsage]\n"
                + "res/drawable/ic_launcher.png: Warning: The ic_launcher.png icon has identical contents in the following configuration folders: drawable-mdpi, drawable [IconDuplicatesConfig]\n"
                + "    res/drawable-mdpi/ic_launcher.png: <No location-specific message\n"
                + "res/drawable/ic_launcher.png: Warning: Found bitmap drawable res/drawable/ic_launcher.png in densityless folder [IconLocation]\n"
                + "res/drawable-hdpi: Warning: Missing the following drawables in drawable-hdpi: sample_icon.gif (found in drawable-mdpi) [IconDensities]\n"
                + "res: Warning: Missing density variation folders in res: drawable-xhdpi, drawable-xxhdpi [IconMissingDensityFolder]\n"
                + "0 "
                + "errors, 5 warnings\n";
        lint().files(
                    // Use minSDK4 to ensure that we get warnings about missing drawables
                    manifest().minSdk(4),
                    image("res/drawable/ic_launcher.png", 48, 48).fill(10, 10, 20, 20, 0xFF00FFFF),
                    image("res/drawable-mdpi/ic_launcher.png", 48, 48).fill(10, 10, 20, 20, 0xFF00FFFF),
                    image("res/drawable-mdpi/sample_icon.gif", 48, 48).fill(10, 10, 20, 20, 0xFF00FFFF),
                    // Make a dummy file named .svn to make sure it doesn't get seen as
                    // an icon name
                    source("res/drawable-hdpi/.svn", ""),
                    image("res/drawable-hdpi/ic_launcher.png", 72, 72).fill(10, 10, 30, 30, 0xFFFF00FF))
                .issues(ALL_ISSUES)
                .run()
                .expect(expected);
    }

    public void testMixed() throws Exception {
        String expected = ""
                + "res/drawable/background.xml: Warning: The following images appear both as density independent .xml files and as bitmap files: res/drawable-mdpi/background.png, res/drawable/background.xml [IconXmlAndPng]\n"
                + "    res/drawable-mdpi/background.png: <No location-specific message\n"
                + "0 errors, 1 warnings\n";
        lint().files(
                manifest().minSdk(4),
                xml("res/drawable/background.xml", ""
                        + "<tag/>"),
                image("res/drawable-mdpi/background.png", 48, 48).fill(0xFF00FF30))
                .issues(ICON_XML_AND_PNG)
                .run()
                .expect(expected);
    }

    public void testMixedVectors() throws Exception {
        lint().files(
                manifest().minSdk(4),
                xml("res/drawable-v21/vector.xml", ""
                        + "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" />\n"),
                image("res/drawable-mdpi/vector.png", 48, 48),
                image("res/drawable-hdpi/vector.png", 48, 48))
                .issues(ICON_XML_AND_PNG)
                .run()
                .expectClean();
    }

    public void testApi1() throws Exception {
        lint().files(
                // manifest file which specifies uses sdk = 2
                manifest().minSdk(2),
                image("res/drawable/ic_launcher.png", 48, 48).fill(10, 10, 20, 20, 0xFF00FFFF))
                .issues(ALL_ISSUES)
                .run()
                .expectClean();
    }

    public void test2() throws Exception {
        String expected = ""
                + "res/drawable-hdpi/other.9.png: Warning: The following unrelated icon files have identical contents: appwidget_bg.9.png, other.9.png [IconDuplicates]\n"
                + "    res/drawable-hdpi/appwidget_bg.9.png: <No location-specific message\n"
                + "res/drawable-hdpi/unrelated.png: Warning: The following unrelated icon files have identical contents: ic_launcher.png, unrelated.png [IconDuplicates]\n"
                + "    res/drawable-hdpi/ic_launcher.png: <No location-specific message\n"
                + "res: Warning: Missing density variation folders in res: drawable-mdpi, drawable-xhdpi, drawable-xxhdpi [IconMissingDensityFolder]\n"
                + "0 errors, 3 warnings\n";
        lint().files(
                image("res/drawable-hdpi/appwidget_bg.9.png", 48, 48).fill(0xFF00FF29),
                image("res/drawable-hdpi/appwidget_bg_focus.9.png", 49, 49).fill(0xFF00FF29),
                image("res/drawable-hdpi/other.9.png", 48, 48).fill(0xFF00FF29),
                image("res/drawable-hdpi/ic_launcher.png", 72, 72).fill(10, 10, 20, 20, 0x00000000),
                image("res/drawable-hdpi/unrelated.png", 72, 72).fill(10, 10, 20, 20, 0x00000000))
                .issues(ALL_ISSUES)
                .run()
                .expect(expected);
    }

    public void testNoDpi() throws Exception {
        String expected = ""
                + "res/drawable-mdpi/frame.png: Warning: The following images appear in both -nodpi and in a density folder: frame.png [IconNoDpi]\n"
                + "res/drawable-xlarge-nodpi-v11/frame.png: Warning: The frame.png icon has identical contents in the following configuration folders: drawable-mdpi, drawable-nodpi, drawable-xlarge-nodpi-v11 [IconDuplicatesConfig]\n"
                + "    res/drawable-nodpi/frame.png: <No location-specific message\n"
                + "    res/drawable-mdpi/frame.png: <No location-specific message\n"
                + "res: Warning: Missing density variation folders in res: drawable-hdpi, drawable-xhdpi, drawable-xxhdpi [IconMissingDensityFolder]\n"
                + "0 errors, 3 warnings\n";
        lint().files(
                image("res/drawable-mdpi/frame.png", 472, 290).fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000),
                image("res/drawable-nodpi/frame.png", 472, 290).fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000),
                image("res/drawable-xlarge-nodpi-v11/frame.png", 472, 290).fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000))
                .issues(ALL_ISSUES)
                .run()
                .expect(expected);
    }

    public void testNoDpi2() throws Exception {
        // Having additional icon names in the no-dpi folder should not cause any complaints
        String expected = "" +
                "res/drawable-xxxhdpi/frame.png: Warning: The image frame.png varies significantly in its density-independent (dip) size across the various density versions: drawable-hdpi/frame.png: 315x193 dp (472x290 px), drawable-ldpi/frame.png: 629x387 dp (472x290 px), drawable-mdpi/frame.png: 472x290 dp (472x290 px), drawable-xhdpi/frame.png: 236x145 dp (472x290 px), drawable-xxhdpi/frame.png: 157x97 dp (472x290 px), drawable-xxxhdpi/frame.png: 118x73 dp (472x290 px) [IconDipSize]\n" +
                "    res/drawable-xxhdpi/frame.png: <No location-specific message\n" +
                "    res/drawable-xhdpi/frame.png: <No location-specific message\n" +
                "    res/drawable-hdpi/frame.png: <No location-specific message\n" +
                "    res/drawable-mdpi/frame.png: <No location-specific message\n" +
                "    res/drawable-ldpi/frame.png: <No location-specific message\n" +
                "res/drawable-xxxhdpi/frame.png: Warning: The following unrelated icon files have identical contents: frame.png, frame.png, frame.png, file1.png, file2.png, frame.png, frame.png, frame.png [IconDuplicates]\n" +
                "    res/drawable-xxhdpi/frame.png: <No location-specific message\n" +
                "    res/drawable-xhdpi/frame.png: <No location-specific message\n" +
                "    res/drawable-nodpi/file2.png: <No location-specific message\n" +
                "    res/drawable-nodpi/file1.png: <No location-specific message\n" +
                "    res/drawable-mdpi/frame.png: <No location-specific message\n" +
                "    res/drawable-ldpi/frame.png: <No location-specific message\n" +
                "    res/drawable-hdpi/frame.png: <No location-specific message\n" +
                "0 errors, 2 warnings\n";
        lint().files(
                image("res/drawable-mdpi/frame.png", 472, 290).fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000),
                image("res/drawable-hdpi/frame.png", 472, 290).fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000),
                image("res/drawable-ldpi/frame.png", 472, 290).fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000),
                image("res/drawable-xhdpi/frame.png", 472, 290).fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000),
                image("res/drawable-xxhdpi/frame.png", 472, 290).fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000),
                image("res/drawable-xxxhdpi/frame.png", 472, 290).fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000),
                image("res/drawable-nodpi/file1.png", 472, 290).fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000),
                image("res/drawable-nodpi/file2.png", 472, 290).fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000))
                .issues(ALL_ISSUES)
                .run()
                .expect(expected);
    }

    public void testNoDpiMix() throws Exception {
        String expected = ""
                + "res/drawable-mdpi/frame.xml: Warning: The following images appear in both -nodpi and in a density folder: frame.png, frame.xml [IconNoDpi]\n"
                + "    res/drawable-mdpi/frame.png: <No location-specific message\n"
                + "res/drawable-nodpi/frame.xml: Warning: The following images appear both as density independent .xml files and as bitmap files: res/drawable-mdpi/frame.png, res/drawable-nodpi/frame.xml [IconXmlAndPng]\n"
                + "    res/drawable-mdpi/frame.png: <No location-specific message\n"
                + "res: Warning: Missing density variation folders in res: drawable-hdpi, drawable-xhdpi, drawable-xxhdpi [IconMissingDensityFolder]\n"
                + "0 errors, 3 warnings\n";
        lint().files(
                image("res/drawable-mdpi/frame.png", 472, 290).fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000),
                xml("res/drawable-nodpi/frame.xml", ""
                        + "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "    <item  android:color=\"#ff000000\"/> <!-- WRONG, SHOULD BE LAST -->\n"
                        + "    <item android:state_pressed=\"true\"\n"
                        + "          android:color=\"#ffff0000\"/> <!-- pressed -->\n"
                        + "    <item android:state_focused=\"true\"\n"
                        + "          android:color=\"#ff0000ff\"/> <!-- focused -->\n"
                        + "</selector>\n"))
                .issues(ALL_ISSUES)
                .run()
                .expect(expected);
    }

    public void testNoWarningForMipmapsOnly() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=162958
        lint().files(
                xml("res/drawable/foo.xml", "<bitmap/>"),
                image("res/drawable-nodpi/file1.png", 472, 290).fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000),
                image("res/mipmap-hdpi/frame.png", 472, 290).fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000),
                image("res/mipmap-mdpi/frame.png", 472, 290).fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000))
                .issues(ICON_MISSING_FOLDER)
                .run()
                .expectClean();
    }

    public void testMixedFormat() throws Exception {
        // Test having a mixture of .xml and .png resources for the same name
        // Make sure we don't get:
        // drawable-hdpi: Warning: Missing the following drawables in drawable-hdpi: f.png (found in drawable-mdpi)
        // drawable-xhdpi: Warning: Missing the following drawables in drawable-xhdpi: f.png (found in drawable-mdpi)
        String expected = ""
                + "res/drawable-xxxhdpi/f.xml: Warning: The following images appear both as density independent .xml files and as bitmap files: res/drawable-hdpi/f.xml, res/drawable-mdpi/f.png [IconXmlAndPng]\n"
                + "    res/drawable-xxhdpi/f.xml: <No location-specific message\n"
                + "    res/drawable-xhdpi/f.xml: <No location-specific message\n"
                + "    res/drawable-mdpi/f.png: <No location-specific message\n"
                + "    res/drawable-hdpi/f.xml: <No location-specific message\n"
                + "0 errors, 1 warnings\n";
        lint().files(
                image("res/drawable-mdpi/f.png", 472, 290).fill(0xFFFFFFFF)
                        .fill(10, 10, 362, 280, 0x00000000),
                xml("res/drawable-hdpi/f.xml", ""
                        + "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "    <item  android:color=\"#ff000000\"/> <!-- WRONG, SHOULD BE LAST -->\n"
                        + "    <item android:state_pressed=\"true\"\n"
                        + "          android:color=\"#ffff0000\"/> <!-- pressed -->\n"
                        + "    <item android:state_focused=\"true\"\n"
                        + "          android:color=\"#ff0000ff\"/> <!-- focused -->\n"
                        + "</selector>\n"),
                xml("res/drawable-xhdpi/f.xml", ""
                        + "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "    <item  android:color=\"#ff000000\"/> <!-- WRONG, SHOULD BE LAST -->\n"
                        + "    <item android:state_pressed=\"true\"\n"
                        + "          android:color=\"#ffff0000\"/> <!-- pressed -->\n"
                        + "    <item android:state_focused=\"true\"\n"
                        + "          android:color=\"#ff0000ff\"/> <!-- focused -->\n"
                        + "</selector>\n"),
                xml("res/drawable-xxhdpi/f.xml", ""
                        + "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "    <item  android:color=\"#ff000000\"/> <!-- WRONG, SHOULD BE LAST -->\n"
                        + "    <item android:state_pressed=\"true\"\n"
                        + "          android:color=\"#ffff0000\"/> <!-- pressed -->\n"
                        + "    <item android:state_focused=\"true\"\n"
                        + "          android:color=\"#ff0000ff\"/> <!-- focused -->\n"
                        + "</selector>\n"),
                xml("res/drawable-xxxhdpi/f.xml", ""
                        + "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "    <item  android:color=\"#ff000000\"/> <!-- WRONG, SHOULD BE LAST -->\n"
                        + "    <item android:state_pressed=\"true\"\n"
                        + "          android:color=\"#ffff0000\"/> <!-- pressed -->\n"
                        + "    <item android:state_focused=\"true\"\n"
                        + "          android:color=\"#ff0000ff\"/> <!-- focused -->\n"
                        + "</selector>\n"))
                .issues(ALL_ISSUES)
                .run()
                .expect(expected);
    }

    public void testMisleadingFileName() throws Exception {
        if (!imageFormatSupported("JPG")) {
            return;
        }

        String expected = ""
                + "res/drawable-mdpi/frame.gif: Warning: Misleading file extension; named .gif but the file format is png [IconExtension]\n"
                + "res/drawable-mdpi/frame.jpg: Warning: Misleading file extension; named .jpg but the file format is png [IconExtension]\n"
                + "res/drawable-mdpi/myjpg.png: Warning: Misleading file extension; named .png but the file format is JPEG [IconExtension]\n"
                + "res/drawable-mdpi/sample_icon.jpeg: Warning: Misleading file extension; named .jpeg but the file format is gif [IconExtension]\n"
                + "res/drawable-mdpi/sample_icon.jpg: Warning: Misleading file extension; named .jpg but the file format is gif [IconExtension]\n"
                + "res/drawable-mdpi/sample_icon.png: Warning: Misleading file extension; named .png but the file format is gif [IconExtension]\n"
                + "0 errors, 6 warnings\n";
        lint().files(
                image("res/drawable-mdpi/myjpg.jpg", 48, 48).format("JPG").fill(10, 10, 20, 20, 0xFF00FFFF), // VALID
                image("res/drawable-mdpi/myjpg.jpeg", 48, 48).format("JPG").fill(10, 10, 20, 20, 0xFF00FFFF), // VALID
                image("res/drawable-mdpi/frame.gif", 472, 290).format("PNG").fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000),
                image("res/drawable-mdpi/frame.jpg", 472, 290).format("PNG").fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000),
                image("res/drawable-mdpi/myjpg.png", 48, 48).format("JPG").fill(10, 10, 20, 20, 0xFF00FFFF),
                image("res/drawable-mdpi/sample_icon.jpg", 48, 48).format("GIF").fill(10, 10, 20, 20, 0xFF00FFFF),
                image("res/drawable-mdpi/sample_icon.jpeg", 48, 48).format("GIF").fill(10, 10, 20, 20, 0xFF00FFFF),
                image("res/drawable-mdpi/sample_icon.png", 48, 48).format("GIF").fill(10, 10, 20, 20, 0xFF00FFFF))
                .issues(ICON_EXTENSION)
                .run()
                .expect(expected);
    }

    public void testMisleadingWebpFileName() throws Exception {
        String expected = ""
                + "res/drawable-mdpi/foo.png: Warning: Misleading file extension; named .png but the file format is webp [IconExtension]\n"
                + "0 errors, 1 warnings\n";
        lint().files(
                base64gzip("res/drawable-mdpi/foo.png", ""
                        + "H4sIAAAAAAAAAAvydHNTYWBgCHd1CggLsPARB7L1LQ/wMrBf2O/3ddt/RgXF"
                        + "P/XrXb/Yf2VkAABv2HPZLAAAAA=="),
                base64gzip("res/drawable-mdpi/ok.webp", ""
                        + "H4sIAAAAAAAAAAvydHNTYWBgCHd1CggLsPARB7L1LQ/wMrBf2O/3ddt/RgXF"
                        + "P/XrXb/Yf2VkAABv2HPZLAAAAA=="))
                .issues(ICON_EXTENSION)
                .run()
                .expect(expected);
    }

    public void testColors() throws Exception {
        String expected = ""
                + "res/drawable-mdpi/ic_menu_my_action.png: Warning: Action Bar icons should use a single gray color (#333333 for light themes (with 60%/30% opacity for enabled/disabled), and #FFFFFF with opacity 80%/30% for dark themes [IconColors]\n"
                + "res/drawable-mdpi-v11/ic_stat_my_notification.png: Warning: Notification icons must be entirely white [IconColors]\n"
                + "res/drawable-mdpi-v9/ic_stat_my_notification2.png: Warning: Notification icons must be entirely white [IconColors]\n"
                + "0 errors, 3 warnings\n";
        lint().files(
                manifest().minSdk(14),
                image("res/drawable-mdpi/ic_menu_my_action.png", 48, 48).fill(0xFF00FF30),
                image("res/drawable-mdpi-v11/ic_stat_my_notification.png", 48, 48).fill(0xFF00FF30),
                image("res/drawable-mdpi-v9/ic_stat_my_notification2.png", 48, 48).fill(0xFF00FF30),
                image("res/drawable-mdpi/ic_menu_add_clip_normal.png", 32, 32).fill(10, 10, 20, 20, 0xFFFFFFFF))
                .issues(ICON_COLORS)
                .run()
                .expect(expected);
    }

    public void testNotActionBarIcons() throws Exception {
        lint().files(
            // No Java code designates the menu as an action bar menu
                manifest().minSdk(14),
                mMenu,
                image("res/drawable-mdpi/icon1.png", 48, 48).fill(0xFF00FF30),
                image("res/drawable-mdpi/icon2.png", 48, 48).fill(0xFF00FF30),
                image("res/drawable-mdpi/icon3.png", 48, 48).fill(10, 10, 20, 20, 0xFF00FFFF), // Not action bar
                image("res/drawable-mdpi/ic_menu_add_clip_normal.png", 32, 32).fill(10, 10, 20, 20, 0xFFFFFFFF))
                .issues(ICON_COLORS)
                .run()
                .expectClean();
    }

    public void testActionBarIcons() throws Exception {
        String expected = ""
                + "res/drawable-mdpi/icon1.png: Warning: Action Bar icons should use a single gray color (#333333 for light themes (with 60%/30% opacity for enabled/disabled), and #FFFFFF with opacity 80%/30% for dark themes [IconColors]\n"
                + "res/drawable-mdpi/icon2.png: Warning: Action Bar icons should use a single gray color (#333333 for light themes (with 60%/30% opacity for enabled/disabled), and #FFFFFF with opacity 80%/30% for dark themes [IconColors]\n"
                + "0 errors, 2 warnings\n";
        lint().files(
                manifest().minSdk(14),
                mMenu,
                mActionBarTest,
                image("res/drawable-mdpi/icon1.png", 48, 48).fill(0xFF00FF30),
                image("res/drawable-mdpi/icon2.png", 48, 48).fill(0xFF00FF30),
                image("res/drawable-mdpi/icon3.png", 48, 48).fill(10, 10, 20, 20, 0xFF00FFFF), // Not action bar
                image("res/drawable-mdpi/ic_menu_add_clip_normal.png", 32, 32).fill(10, 10, 20, 20, 0xFFFFFFFF))
                .issues(ICON_COLORS)
                .run()
                .expect(expected);
    }

    public void testOkActionBarIcons() throws Exception {
        lint().files(
                manifest().minSdk(14),
                mMenu,
                image("res/drawable-mdpi/icon1.png", 32, 32).fill(10, 10, 20, 20, 0xFFFFFFFF),
                image("res/drawable-mdpi/icon2.png", 32, 32).fill(10, 10, 20, 20, 0xFFFFFFFF))
                .issues(ICON_COLORS)
                .run()
                .expectClean();
    }

    public void testNotificationIcons() throws Exception {
        String expected = ""
                + "res/drawable-mdpi/icon1.png: Warning: Notification icons must be entirely white [IconColors]\n"
                + "    src/test/pkg/NotificationTest.java:11: Icon used in notification here\n"
                + "res/drawable-mdpi/icon2.png: Warning: Notification icons must be entirely white [IconColors]\n"
                + "    src/test/pkg/NotificationTest.java:16: Icon used in notification here\n"
                + "res/drawable-mdpi/icon3.png: Warning: Notification icons must be entirely white [IconColors]\n"
                + "    src/test/pkg/NotificationTest.java:23: Icon used in notification here\n"
                + "res/drawable-mdpi/icon4.png: Warning: Notification icons must be entirely white [IconColors]\n"
                + "    src/test/pkg/NotificationTest.java:29: Icon used in notification here\n"
                + "res/drawable-mdpi/icon5.png: Warning: Notification icons must be entirely white [IconColors]\n"
                + "    src/test/pkg/NotificationTest.java:36: Icon used in notification here\n"
                + "0 errors, 5 warnings\n";
        lint().files(
                manifest().minSdk(14),
                mNotificationTest,
                image("res/drawable-mdpi/icon1.png", 48, 48).fill(0xFF00FF30),
                image("res/drawable-mdpi/icon2.png", 48, 48).fill(0xFF00FF30),
                image("res/drawable-mdpi/icon3.png", 48, 48).fill(10, 10, 20, 20, 0xFF00FFFF),
                image("res/drawable-mdpi/icon4.png", 48, 48).fill(10, 10, 20, 20, 0xFF00FFFF),
                image("res/drawable-mdpi/icon5.png", 48, 48).fill(10, 10, 20, 20, 0xFF00FFFF),
                image("res/drawable-mdpi/icon6.png", 48, 48).fill(10, 10, 20, 20, 0xFF00FFFF), // not a notification
                image("res/drawable-mdpi/icon7.png", 48, 48).fill(10, 10, 20, 20, 0xFF00FFFF), // ditto
                image("res/drawable-mdpi/ic_menu_add_clip_normal.png", 32, 32).fill(10, 10, 20, 20, 0xFFFFFFFF))
                .issues(ICON_COLORS)
                .run()
                .expect(expected);
    }

    public void testOkNotificationIcons() throws Exception {
        lint().files(
                manifest().minSdk(14),
                mNotificationTest,
                image("res/drawable-mdpi/icon1.png", 32, 32).fill(10, 10, 20, 20, 0xFFFFFFFF),
                image("res/drawable-mdpi/icon2.png", 32, 32).fill(10, 10, 20, 20, 0xFFFFFFFF),
                image("res/drawable-mdpi/icon3.png", 32, 32).fill(10, 10, 20, 20, 0xFFFFFFFF),
                image("res/drawable-mdpi/icon4.png", 32, 32).fill(10, 10, 20, 20, 0xFFFFFFFF),
                image("res/drawable-mdpi/icon5.png", 32, 32).fill(10, 10, 20, 20, 0xFFFFFFFF))
                .issues(ICON_COLORS)
                .run()
                .expectClean();
    }

    public void testExpectedSize() throws Exception {
        String expected = ""
                + "res/drawable-mdpi/ic_launcher.png: Warning: Incorrect icon size for drawable-mdpi/ic_launcher.png: expected 48x48, but was 24x24 [IconExpectedSize]\n"
                + "res/drawable-mdpi/icon1.png: Warning: Incorrect icon size for drawable-mdpi/icon1.png: expected 32x32, but was 48x48 [IconExpectedSize]\n"
                + "res/drawable-mdpi/icon3.png: Warning: Incorrect icon size for drawable-mdpi/icon3.png: expected 24x24, but was 48x48 [IconExpectedSize]\n"
                + "0 errors, 3 warnings\n";
        lint().files(
                manifest().minSdk(14),
                mNotificationTest,
                mMenu,
                mActionBarTest,

                // 3 wrong-sized icons:
                image("res/drawable-mdpi/icon1.png", 48, 48).fill(0xFF00FF30),
                image("res/drawable-mdpi/icon3.png", 48, 48).fill(10, 10, 20, 20, 0xFF00FFFF),
                image("res/drawable-mdpi/ic_launcher.png", 24, 24),

                // OK sizes
                image("res/drawable-mdpi/icon2.png", 32, 32).fill(10, 10, 20, 20, 0xFFFFFFFF),
                image("res/drawable-mdpi/icon4.png", 24, 24),
                image("res/drawable-mdpi/ic_launcher2.png", 48, 48).fill(10, 10, 20, 20, 0xFF00FFFF))
                .issues(ICON_EXPECTED_SIZE)
                .run()
                .expect(expected);
    }

    public void testExpectedSizeMipmap() throws Exception {
        String expected = ""
                + "res/mipmap-mdpi/ic_launcher.png: Warning: Incorrect icon size for mipmap-mdpi/ic_launcher.png: expected 48x48, but was 24x24 [IconExpectedSize]\n"
                + "0 errors, 1 warnings\n";
        lint().files(
                manifest().minSdk(14),
                // 3 wrong-sized icons:
                image("res/mipmap-mdpi/ic_launcher.png", 24, 24),

                // OK sizes
                image("res/mipmap-xxhdpi/ic_launcher2.png", 144, 144).fill(10, 10, 20, 20, 0xFF00FFFF))
                .issues(ICON_EXPECTED_SIZE)
                .run()
                .expect(expected);
    }

    public void testCheckNullDensity() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=230324
        // Make sure we gracefully handle a null density qualifier
        lint().files(
                manifest().minSdk(14),
                image("res/drawable/ic_launcher.png", 24, 24))
                .issues(ICON_EXPECTED_SIZE)
                .run()
                .expectClean();
    }

    public void testAbbreviate() throws Exception {
        String expected = ""
                + "res/drawable-hdpi: Warning: Missing the following drawables in drawable-hdpi: ic_launcher10.png, ic_launcher11.png, ic_launcher12.png, ic_launcher2.png, ic_launcher3.png... (6 more) [IconDensities]\n"
                + "res/drawable-xhdpi: Warning: Missing the following drawables in drawable-xhdpi: ic_launcher10.png, ic_launcher11.png, ic_launcher12.png, ic_launcher2.png, ic_launcher3.png... (6 more) [IconDensities]\n"
                + "0 errors, 2 warnings\n";
        lint().files(
                // Use minSDK4 to ensure that we get warnings about missing drawables
                manifest().minSdk(4),
                image("res/drawable-hdpi/ic_launcher1.png", 48, 48).fill(0xFF00FF16),
                image("res/drawable-xhdpi/ic_launcher1.png", 48, 48).fill(0xFF00FF17),
                image("res/drawable-mdpi/ic_launcher1.png", 48, 48).fill(0xFF00FF18),
                image("res/drawable-mdpi/ic_launcher2.png", 48, 48).fill(10, 10, 20, 20, 0xFF00FFFF),
                image("res/drawable-mdpi/ic_launcher3.png", 48, 48).fill(0xFF00FF19),
                image("res/drawable-mdpi/ic_launcher4.png", 48, 48).fill(0xFF00FF20),
                image("res/drawable-mdpi/ic_launcher5.png", 48, 48).fill(0xFF00FF21),
                image("res/drawable-mdpi/ic_launcher6.png", 48, 48).fill(0xFF00FF22),
                image("res/drawable-mdpi/ic_launcher7.png", 48, 48).fill(0xFF00FF23),
                image("res/drawable-mdpi/ic_launcher8.png", 48, 48).fill(0xFF00FF24),
                image("res/drawable-mdpi/ic_launcher9.webp", 48, 48).fill(0xFF00FF25).format("PNG"),
                image("res/drawable-mdpi/ic_launcher10.png", 48, 48).fill(0xFF00FF26),
                image("res/drawable-mdpi/ic_launcher11.png", 48, 48).fill(0xFF00FF26),
                image("res/drawable-mdpi/ic_launcher12.png", 48, 48).fill(0xFF00FF28))
                .issues(ICON_DENSITIES)
                .run()
                .expect(expected);
    }

    public void testSourceFolders() throws Exception {
        // Regression test for https://issuetracker.google.com/37684894
        String expected = ""
                + "res/drawable-hdpi: Warning: Missing the following drawables in drawable-hdpi: ic_launcher2.png (found in drawable-mdpi) [IconDensities]\n" +
                "res/drawable-mdpi: Warning: Missing the following drawables in drawable-mdpi: ic_launcher1.png (found in drawable-hdpi, drawable-xhdpi, drawable-xxhdpi) [IconDensities]\n" +
                "res/drawable-xhdpi: Warning: Missing the following drawables in drawable-xhdpi: ic_launcher2.png (found in drawable-mdpi) [IconDensities]\n" +
                "res/drawable-xxhdpi: Warning: Missing the following drawables in drawable-xxhdpi: ic_launcher2.png (found in drawable-mdpi) [IconDensities]\n" +
                "0 errors, 4 warnings\n";
        lint().files(
                image("res/drawable-hdpi/ic_launcher1.png", 48, 48).fill(0xFF00FF16),
                image("res/drawable-mdpi/ic_launcher2.png", 48, 48).fill(0xFF00FF16),
                image("res/drawable-xhdpi/ic_launcher1.png", 48, 48).fill(0xFF00FF17),
                image("res/drawable-xxhdpi/ic_launcher1.png", 48, 48).fill(0xFF00FF18),
                image("res/drawable-xhdpi/ic_launcher1.png", 48, 48).fill(0xFF00FF18))
                .issues(ICON_DENSITIES)
                .run()
                .expect(expected);
    }

    public void testShowAll() throws Exception {
        String expected = ""
                + "res/drawable-hdpi: Warning: Missing the following drawables in drawable-hdpi: ic_launcher10.png, ic_launcher11.png, ic_launcher12.png, ic_launcher2.png, ic_launcher3.png, ic_launcher4.png, ic_launcher5.png, ic_launcher6.png, ic_launcher7.png, ic_launcher8.png, ic_launcher9.png [IconDensities]\n"
                + "res/drawable-xhdpi: Warning: Missing the following drawables in drawable-xhdpi: ic_launcher10.png, ic_launcher11.png, ic_launcher12.png, ic_launcher2.png, ic_launcher3.png, ic_launcher4.png, ic_launcher5.png, ic_launcher6.png, ic_launcher7.png, ic_launcher8.png, ic_launcher9.png [IconDensities]\n"
                + "0 errors, 2 warnings\n";
        lint().files(
                // Use minSDK4 to ensure that we get warnings about missing drawables
                manifest().minSdk(4),
                image("res/drawable-hdpi/ic_launcher1.png", 48, 48).fill(0xFF00FF16),
                image("res/drawable-xhdpi/ic_launcher1.png", 48, 48).fill(0xFF00FF17),
                image("res/drawable-mdpi/ic_launcher1.png", 48, 48).fill(0xFF00FF18),
                image("res/drawable-mdpi/ic_launcher2.png", 48, 48).fill(10, 10, 20, 20, 0xFF00FFFF),
                image("res/drawable-mdpi/ic_launcher3.png", 48, 48).fill(0xFF00FF19),
                image("res/drawable-mdpi/ic_launcher4.png", 48, 48).fill(0xFF00FF20),
                image("res/drawable-mdpi/ic_launcher5.png", 48, 48).fill(0xFF00FF21),
                image("res/drawable-mdpi/ic_launcher6.png", 48, 48).fill(0xFF00FF22),
                image("res/drawable-mdpi/ic_launcher7.png", 48, 48).fill(0xFF00FF23),
                image("res/drawable-mdpi/ic_launcher8.png", 48, 48).fill(0xFF00FF24),
                image("res/drawable-mdpi/ic_launcher9.png", 48, 48).fill(0xFF00FF29),
                image("res/drawable-mdpi/ic_launcher10.png", 48, 48).fill(0xFF00FF26),
                image("res/drawable-mdpi/ic_launcher11.png", 48, 48).fill(0xFF00FF26),
                image("res/drawable-mdpi/ic_launcher12.png", 48, 48).fill(0xFF00FF28))
                .issues(ICON_DENSITIES)
                .configureDriver((driver) -> driver.setAbbreviating(false))
                .run()
                .expect(expected);
    }

    public void testIgnoreMissingFolders() throws Exception {
        lint().files(
                // Use minSDK4 to ensure that we get warnings about missing drawables
                manifest().minSdk(4),
                // source() instead of xml() because IDE validation shows error on <ignore...>
                source("lint.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<lint>\n"
                        + "    <issue id=\"IconDensities\" severity=\"warning\">\n"
                        + "        <ignore path=\"res/drawable-hdpi\" />\n"
                        + "  </issue>\n"
                        + "</lint>\n"),
                image("res/drawable-hdpi/ic_launcher1.png", 48, 48).fill(0xFF00FF16),
                image("res/drawable-mdpi/ic_launcher1.png", 48, 48).fill(0xFF00FF18),
                image("res/drawable-mdpi/ic_launcher2.png", 48, 48).fill(10, 10, 20, 20, 0xFF00FFFF))
                .issues(ICON_DENSITIES)
                .run()
                .expectClean();
    }

    public void testSquareLauncher() throws Exception {
        String expected = ""
                + "res/drawable-hdpi/ic_launcher_filled.png: Warning: Launcher icons should not fill every pixel of their square region; see the design guide for details [IconLauncherShape]\n"
                + "0 errors, 1 warnings\n";
        lint().files(
                manifest().minSdk(4),
                image("res/drawable-hdpi/ic_launcher_filled.png", 50, 40).fill(0xFFFFFFFF),
                image("res/drawable-mdpi/ic_launcher_2.png", 50, 40).text(5, 5, "x", 0xFFFFFFFF))
                .issues(ICON_LAUNCHER_SHAPE)
                .run()
                .expect(expected);
    }

    public void testSquareLauncherFromNonStandardMipmapName() throws Exception {
        // Checks both launcher icons not named ic_launcher and roundIcon attributes
        String expected = ""
                + "res/mipmap-mdpi/my_launcher.png: Warning: Launcher icons should not fill every pixel of their square region; see the design guide for details [IconLauncherShape]\n"
                + "res/mipmap-hdpi/my_launcher_round.png: Warning: Launcher icon used as round icon did not have a circular shape [IconLauncherShape]\n"
                + "0 errors, 2 warnings\n";
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"pkg.my.myapplication\">\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:icon=\"@mipmap/my_launcher\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:roundIcon=\"@mipmap/my_launcher_round\""
                        + "/>\n"
                        + "</manifest>"),
                image("res/mipmap-hdpi/my_launcher_round.png", 50, 50).fill(0xFFFFFFFF),
                image("res/mipmap-hdpi/my_launcher2_round.png", 50, 50).fillOval(0,0, 50, 50, 0xFFFFFFFF),
                image("res/mipmap-mdpi/my_launcher.png", 50, 50).fill(0xFFFFFFFF),
                image("res/mipmap-mdpi/ic_launcher.png", 50, 40).text(5, 5, "x", 0xFFFFFFFF)) // OK
                .issues(ICON_LAUNCHER_SHAPE)
                .run()
                .expect(expected);
    }

    public void testShadow() throws Exception {
        lint().files(
                base64gzip("res/mipmap-mdpi/ic_launcher_round.png", ""
                        + "H4sIAAAAAAAAAAH7CQT2iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABX"
                        + "AvmHAAAJwklEQVR42sVaa1BU5xmWCLFJbJr8sCm9jalNmpQ/Zjq2nf7p+Kc/"
                        + "quwuWlIzlmlsOlPHSaZp7DjWNioQ7iCImqAU0Y4IchEEsxXFXXaXm0IocrOA"
                        + "yB2yQOUqF2V5+77ffufst8s5hwUh7swzHJbvvN/zvLfv+85hzZon/ACAD2It"
                        + "wo9+ejFeHkv3rnlaH5z8GYSvyt/WI/wRr3LQ9XqVsb5k66kSx99fR+xBpMzP"
                        + "z5c6HI42hB2vRwh0Td/R32gMH/v6Vy6EQu/h5RAkVYyYQACS9Br8nmKygXhB"
                        + "aY6VJO4j5Te/3oskWjxIz8/NzTm8AY31ENRCNqWa4NHwWSnya4XrnyHpKoE4"
                        + "EXrscH5giVhwL/5eRXMozb0S5D9E4nPC5HPLIK0IbsvBU4uuP3xiEeKNaPBT"
                        + "yevca/MrRd4jBR9Lvz969Chl2SLEboDGMjh5KX9hNeEKhgNmZ2cvIgWfJYkQ"
                        + "iweNfUbkRaOEqZlJsI910vcrQRi+HOtgNj3qg803MTFxWhDh440AX+75P0ue"
                        + "9yzSxp4yOF21BwZHu59YgH20C05W7IKGbuuCIsf5HY8fP4aRkZGPRG6L9nnM"
                        + "v59KBauUNoODdkgv3w8F9VGYq7OqnhWhNIbuza0LhVTrBzA4NKiYTuRETKW5"
                        + "vr6+X2iuE2Leo+oqoWAVJn4ENY1WOFX+O2jstcgkZ2ZnoPt/zXC76wqUtJyB"
                        + "wsZYhhstp+FWZwF0DTfhmGl5fH2PCZJsO6G60cJsqjiCcRgdHb2N1J7x5Log"
                        + "ddDQn4TUUe02Y2NjcLkyCc7e3gd9w/fA1n4RzlbvhRPlb0MyQzDD8fLfyEgq"
                        + "2wmpt/4IlnsXoHe4DdPwD5BTkQDj4+Oa3YlSiQRiFN5XTCVJEf58Hr3fIvRm"
                        + "zeK719EKn5W/C6cqd8OJit8y8osJSCrbwXCc/m59B9rRxmLNgLiQUzEKrUaj"
                        + "8cUFUZAUTU5Ohgje1zQ6OTUOVxpiWQEy8ksUkMgQBJfvRKCtMW/2Tg6sBbh/"
                        + "//57blEQW9PMzEyxVu67yI9BTt1hRt5TQLIgYCH5nQJ5p4BEmwEya/+2qAiJ"
                        + "k91uLxFqwUdeIAYHB1/DQRNSC1MzRLlY6Ol5r72vLICQj5FQ62gSJ+RK68JE"
                        + "WVlZgLy4SW0J8+s9nj7zWnlf3XnVjfhJxOdNx8DUchZSqn6v6H0xfT7Fein5"
                        + "7z+xO8U5xSD5YzY9w+2OQs16oGqmNGpubt4nt1QpAtgJUtX6voSRiUE4c2uP"
                        + "7G1CEbZJFM9Q02lc1Ps0RhpfUB/pJuBUxW42h9Y2gwR2dHSco9VZ3F74PHz4"
                        + "0KYlgG60tmW4kSdcaz4pE6rvMWt6n1DfbZbHG5uS3AQQzK3/Uo2CxG1gYKAC"
                        + "ObtaqdlsXj89Pd2mJWBq+iGk3dorFKkT5LXy9hz4T9cNSK/ep0Lelftpt/dC"
                        + "bVcxlN27BMllwW7kE2w6tjZMT0+pCqA0Hxoaao+KinpZFlBXV/cdLM4vtQR0"
                        + "2hvl9FCCJ3FP8mLrFIvXU0CCVQ8d9gZNAbiI2tPT0zfKAmpqajbhAvZAS0Bl"
                        + "e4HXxNXJuwsQybsEBEJFW56mAKzXkdzc3DdkARaL5TUtAZSTxY0pqmSXQ17R"
                        + "+1yAsf6kYh2IAs6dOxcgCygqKvo+LmJ2NQG0pb1cG42TIjkVJOKmLNEqYYeM"
                        + "YzKCOJC4xQAJFr2MeIuOI5Ah94sINqeaAGwAg8nJyZtkAQcPHnx5ampKtYhp"
                        + "8cqwhcHfMw1w6KIe/kHIRGTp4eNLiGw9HM4xwJFcPRwl5CEu6yE0H1GAuKKH"
                        + "sCs6CCsiBEL4VSfoOqxoO8c2COW4UHZYcXcqCcDVuD0kJOSb4n5uHR4ayrQE"
                        + "5NSEQ7xpB3xSaIDwwiAILwqCTxARnxsgwmiAyH8HQeQ1A0QVB0HU9SCIRsTc"
                        + "QJQYINZkgDiCWe9EqQgdxJoD3ZBVfURVAP3s7u6uQs7Pixs5X+yt6VoCcmvD"
                        + "WT4fs+yASKNE2sBIRxYbOGEDIx17E2EK4qQNEF+KKVPK08aqnPeEeI5LNeoC"
                        + "qDYaGhouIGc/xp1vJXzq6+vfl/bfntsJUQAhgURccwmIukYeN8gej7np8no8"
                        + "E4BkS/VuAtTIx1u3qwmgncQ81ipcv359P1+JXVuJ8+fPv4U3jSlt5lgK1YYJ"
                        + "3cQpIqqYC2AR0Dsj4CbAmTLxZqlgdYuSdwpYWAPkfdrMYaqPx8TE/JwvwL7i"
                        + "dno97khNSttpl4AgNyRYKd9RAJKPRvLRJSjiph7J6xn5WJPOKaCUOg15X7co"
                        + "+TjrNshSFsA4tbS0WJDrN9yOAlIdWK3WD3j/dUsjNQGsl1sodfRO3HQJkMlL"
                        + "ApB8vFWNfKBMXkkAHciQ4zxudyA/P5/Sx9ftWCkdz7Zs2fI9DFGr55GSCfgi"
                        + "VF6APEHpEV2iY3AK0Aned/V4b8gzAdULBLAjJXafts2bN29UPNhzRV8zmUyH"
                        + "aBFhJ2keBTKWjQI8l363ldRK3te5kY8z65iAOEmARtrIsPzaU4Do/SPEUfH5"
                        + "kKDIv7+/v0bMOykCWgIkEUyA2Ym4ZZBnAoQUEnK/Frl9lwhmZ2ev1Xoqty4x"
                        + "MdGAB/xZ/lhxjiJy4855deI2V3FSnpPn43BBii8lbHduEbTIc+KEWERxXTrb"
                        + "SiDmqPM8ePBgNjQ0NJi44cbTz5tHiy8ZjcYI6rkUPiqH/oE+yLWegFTTXyH1"
                        + "5n5FnJFQsh9OC5C+d/7+kQf+IiMFkWM5jgeWfqltzuNBC/Ly8mKI06KPFqXW"
                        + "FBwcTCH6dmVlZQadQfFDD5YcWOD0cAl6e3tXBWSb5qC5aE5yIPb6LOJCnLx+"
                        + "a8PrgUL1anV1dSGPBAvpaj9el9KG5qyoqLiKHH5AXFTzfpEXHOsQm2w220Xc"
                        + "rTIRVBLU1VbhvQB7wUFzUNpgNyTP/5A4sBV3OR9+I4nYSHmI4Z3hj2fmV0qI"
                        + "QJzZGh4ensnKyoqlOZ+IvJhOAQEBz1Ie4rnhnbt379ZQNKhDCUIcS3ztJL3R"
                        + "ZMTJFtlsbm6uOXDgwG6ai+ZcctpoFTYaIxH0JODNzMzMsI6OjlYKNY8I+0hv"
                        + "VhZ5xeoQxrN0IVsZGRnhaPvHNAfNtSr/hsB7MKXUtzZs2PBWWlraoTt37liG"
                        + "hoZGyYPSMZBHx+0jvSSkMbSqYqqM4hbeQjbIFtn0qs+vRDT4JM8hXkH8aNeu"
                        + "XdtwO/4xbgazMA2qaM+Cx74BXICGCXTd09PThulXRWNoLN2D977BbTxHNo8e"
                        + "PfpU/meCxLzIiVDhvenv7/+TrVu3/tJgMPyKQNf0Hf2Nj3mF3+P3lf+zh1JE"
                        + "qFM0NTU9y+tEig79/8PXOV7g3/nx/Paje1Yiz/8PRlkryP/HqzsAAAAASUVO"
                        + "RK5CYIK+BOPh+wkAAA=="))
                .issues(ICON_LAUNCHER_SHAPE)
                .run()
                .expectClean();
    }

    public void testMixNinePatch() throws Exception {
        // https://code.google.com/p/android/issues/detail?id=43075
        String expected = ""
                + "res/drawable-mdpi/ic_launcher_filled.png: Warning: The files ic_launcher_filled.png and ic_launcher_filled.9.png clash; both will map to @drawable/ic_launcher_filled [IconMixedNinePatch]\n"
                + "    res/drawable-hdpi/ic_launcher_filled.png: <No location-specific message\n"
                + "    res/drawable-hdpi/ic_launcher_filled.9.png: <No location-specific message\n"
                + "0 errors, 1 warnings\n";
        lint().files(
                manifest().minSdk(4),
                image("res/drawable-mdpi/ic_launcher_filled.png", 50, 40).fill(0xFFFFFFFF),
                image("res/drawable-hdpi/ic_launcher_filled.png", 50, 40).fill(0xFFFFFFFF),
                image("res/drawable-hdpi/ic_launcher_filled.9.png", 50, 40).fill(0xFFFFFFFF),
                image("res/drawable-mdpi/ic_launcher_2.png", 50, 40).text(5, 5, "x", 0xFFFFFFFF))
                .issues(ICON_MIX_9PNG)
                .run()
                .expect(expected);
    }

    public void test67486() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=67486
        lint().files(
                manifest().minSdk(14),
                base64gzip("res/drawable-xhdpi/ic_stat_notify.png", ""
                    + "H4sIAAAAAAAAAAGwAU/+iVBORw0KGgoAAAANSUhEUgAAABgAAAAlCAYAAABY"
                    + "kymLAAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAAsTAAALEwEAmpwYAAAA"
                    + "B3RJTUUH3gMYEiAHIq8UQAAAAT1JREFUSMftljtOxEAMhj9nIyEKkJCQ6Oi2"
                    + "oqbhEhSIk9ByCa5AQ881aDkANfW+YJP8NA4MqySMdmMKhCUr0mSSz//4oYF/"
                    + "+/NmQy8lFRn/kJkpPFJJlq1A0gVwA+wD6tnXrhvwYGb3uZGcSHqW1CjfXiVd"
                    + "t0pS7wKcSVpIqt2bTH+RdL4JKNMz9GSp41iGiqFN8ClwB9wCReeHLmkKPAEH"
                    + "PwA0kJNPKzc2HQOXwOEWJd6pskii3wOu3MdvNElT4BE4ciWTgRLNtrRTK2Dl"
                    + "z9E6MwWs3G3XqPsA6wTQRACqBBByRPVvAJaRgAqYJ+U5OqCJViAHFGN28tfU"
                    + "M6uBma+FlCnAIkyB23LMLu4DEAmY5Vxnds1BqIK5V5BF5qCOBKx9ZIQB3h1S"
                    + "RAGqaAVv7pOxAGWHgjTJ20zVb+o/ACXX5l8tolS6AAAAAElFTkSuQmCCifyr"
                    + "tLABAAA="))
                .issues(ICON_COLORS)
                .run()
                .expectClean();
    }

    public void testDuplicatesWithDpNames() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=74584
        lint().files(
                image("res/drawable-mdpi/foo_72dp.png", 72, 72).fill(10, 10, 20, 20, 0x00000000),
                image("res/drawable-xhdpi/foo_36dp.png", 72, 72).fill(10, 10, 20, 20, 0x00000000))
                .issues(DUPLICATES_NAMES)
                .run()
                .expectClean();
    }

    public void testClaimedSize() throws Exception {
        // Check that icons which declare a dp size actually correspond to that dp size
        String expected = ""
                + "res/drawable-xhdpi/foo_30dp.png: Warning: Suspicious file name foo_30dp.png: The implied 30 dp size does not match the actual dp size (pixel size 72×72 in a drawable-xhdpi folder computes to 36×36 dp) [IconDipSize]\n"
                + "res/drawable-mdpi/foo_80dp.png: Warning: Suspicious file name foo_80dp.png: The implied 80 dp size does not match the actual dp size (pixel size 72×72 in a drawable-mdpi folder computes to 72×72 dp) [IconDipSize]\n"
                + "0 errors, 2 warnings\n";
        lint().files(
                image("res/drawable-mdpi/foo_72dp.png", 72, 72).fill(10, 10, 20, 20, 0x00000000),  // ok
                image("res/drawable-mdpi/foo_80dp.png", 72, 72).fill(10, 10, 20, 20, 0x00000000),  // wrong
                image("res/drawable-xhdpi/foo_36dp.png", 72, 72).fill(10, 10, 20, 20, 0x00000000), // ok
                image("res/drawable-xhdpi/foo_35dp.png", 72, 72).fill(10, 10, 20, 20, 0x00000000), // ~ok
                image("res/drawable-xhdpi/foo_30dp.png", 72, 72).fill(10, 10, 20, 20, 0x00000000)) // wrong
                .issues(ICON_DIP_SIZE)
                .run()
                .expect(expected);
    }

    public void testClaimedSizeWebp() throws Exception {
        // Check size decoding of webp headers
        String expected = ""
                + "res/drawable-mdpi/my_lossless_72dp.webp: Warning: Suspicious file name my_lossless_72dp.webp: The implied 72 dp size does not match the actual dp size (pixel size 58×56 in a drawable-mdpi folder computes to 58×56 dp) [IconDipSize]\n"
                + "res/mipmap-mdpi/my_lossy2_72dp.webp: Warning: Suspicious file name my_lossy2_72dp.webp: The implied 72 dp size does not match the actual dp size (pixel size 58×56 in a mipmap-mdpi folder computes to 58×56 dp) [IconDipSize]\n"
                + "res/drawable-mdpi/my_lossy_72dp.webp: Warning: Suspicious file name my_lossy_72dp.webp: The implied 72 dp size does not match the actual dp size (pixel size 58×56 in a drawable-mdpi folder computes to 58×56 dp) [IconDipSize]\n"
                + "0 errors, 3 warnings\n";
        lint().files(
                manifest().minSdk(1),
                base64gzip("res/drawable-mdpi/my_lossy_72dp.webp", ""
                        // Lossy
                        + "H4sIAAAAAAAAAAvydHPzYGBgCHd1CggLsFCwAbIvMDPMZdSyYrBgsJvoscBH"
                        + "dYmykhIHwwYhzkyGMgYGhbxlC7g+chcxMPw7vf3/Wx8ht6D//wV23zjUANTK"
                        + "AADVeQHzUAAAAA=="),
                base64gzip("res/mipmap-mdpi/my_lossy2_72dp.webp", ""
                        // Lossy
                        + "H4sIAAAAAAAAAAvydHPzYGBgCHd1CggLsFCwAbIvMDPMZdSyYrBgsJvoscBH"
                        + "dYmykhIHwwYhzkyGMgYGhbxlC7g+chcxMPw7vf3/Wx8ht6D//wV23zjUANTK"
                        + "AADVeQHzUAAAAA=="),

                base64gzip("res/drawable-mdpi/my_lossless_72dp.webp", ""
                        // Lossless
                        + "H4sIAAAAAAAAAAvydHNTYWBgCHd1CggLsPARB7L1LQ/wMrBf2O/3ddt/RgXF"
                        + "P/XrXb/Yf2VkAABv2HPZLAAAAA=="))
                .issues(ICON_DIP_SIZE)
                .run()
                .expect(expected);
    }

    public void testResConfigs1() throws Exception {
        // resConfigs in the Gradle model sets up the specific set of resource configs
        // that are included in the packaging: we use this to limit the set of required
        // densities

        String expected = ""
                + "res: Warning: Missing density variation folders in res: drawable-hdpi [IconMissingDensityFolder]\n"
                + "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        lint().files(
                image("res/drawable-mdpi/frame.png", 472, 290).fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000),
                image("res/drawable-nodpi/frame.png", 472, 290).fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000),
                image("res/drawable-xlarge-nodpi-v11/frame.png", 472, 290).fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000),
                gradle(""
                        + "android {\n"
                        + "    defaultConfig {\n"
                        + "        resConfigs \"mdpi\"\n"
                        + "    }\n"
                        + "    flavorDimensions  \"pricing\", \"releaseType\"\n"
                        + "    productFlavors {\n"
                        + "        beta {\n"
                        + "            flavorDimension \"releaseType\"\n"
                        + "            resConfig \"en\"\n"
                        + "            resConfigs \"nodpi\", \"hdpi\"\n"
                        + "        }\n"
                        + "        normal { flavorDimension \"releaseType\" }\n"
                        + "        free { flavorDimension \"pricing\" }\n"
                        + "        paid { flavorDimension \"pricing\" }\n"
                        + "    }\n"
                        + "}\n"))
                .issues(ICON_DENSITIES, ICON_MISSING_FOLDER)
                .run()
                .expect(expected);
    }

    public void testResConfigs2() throws Exception {
        String expected = ""
                + "res/drawable-hdpi: Warning: Missing the following drawables in drawable-hdpi: sample_icon.gif (found in drawable-mdpi) [IconDensities]\n"
                + "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        lint().files(
                // Use minSDK4 to ensure that we get warnings about missing drawables
                manifest().minSdk(4),
                image("res/drawable/ic_launcher.png", 48, 48).fill(10, 10, 20, 20, 0xFF00FFFF),
                image("res/drawable-mdpi/ic_launcher.png", 48, 48).fill(10, 10, 20, 20, 0xFF00FFFF),
                image("res/drawable-xhdpi/ic_launcher.png", 48, 48).fill(0xFF00FF30),
                image("res/drawable-mdpi/sample_icon.gif", 48, 48).fill(10, 10, 20, 20, 0xFF00FFFF),
                image("res/drawable-hdpi/ic_launcher.png", 72, 72).fill(10, 10, 30, 30, 0xFFFF00FF),
                gradle(""
                        + "android {\n"
                        + "    defaultConfig {\n"
                        + "        resConfigs \"mdpi\"\n"
                        + "    }\n"
                        + "    flavorDimensions  \"pricing\", \"releaseType\"\n"
                        + "    productFlavors {\n"
                        + "        beta {\n"
                        + "            flavorDimension \"releaseType\"\n"
                        + "            resConfig \"en\"\n"
                        + "            resConfigs \"nodpi\", \"hdpi\"\n"
                        + "        }\n"
                        + "        normal { flavorDimension \"releaseType\" }\n"
                        + "        free { flavorDimension \"pricing\" }\n"
                        + "        paid { flavorDimension \"pricing\" }\n"
                        + "    }\n"
                        + "}\n"))
                .issues(ICON_DENSITIES, ICON_MISSING_FOLDER)
                .run()
                .expect(expected);
    }

    public void testSplits1() throws Exception {
        // splits in the Gradle model sets up the specific set of resource configs
        // that are included in the packaging: we use this to limit the set of required
        // densities

        String expected = ""
                + "res: Warning: Missing density variation folders in res: drawable-hdpi [IconMissingDensityFolder]\n"
                + "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        lint().files(
                image("res/drawable-mdpi/frame.png", 472, 290).fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000),
                image("res/drawable-nodpi/frame.png", 472, 290).fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000),
                image("res/drawable-xlarge-nodpi-v11/frame.png", 472, 290).fill(0xFFFFFFFF).fill(10, 10, 362, 280, 0x00000000),
                gradle(""
                        + "android {\n"
                        + "    splits {\n"
                        + "        density {\n"
                        + "            enable true\n"
                        + "            reset()\n"
                        + "            include \"mdpi\", \"hdpi\"\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .issues(ICON_DENSITIES, ICON_MISSING_FOLDER)
                .run()
                .expect(expected);
    }

    public void testWebpEligible() throws Exception {
        String expected = ""
                + "res/drawable-mdpi/random.png: Warning: One or more images in this project can be converted to the WebP format which typically results in smaller file sizes, even for lossless conversion (but launcher icons should use PNG). [ConvertToWebp]\n"
                + "0 errors, 1 warnings\n";
        lint().files(
                manifest().minSdk(18),
                image("res/drawable-mdpi/random.png", 48, 48).fill(10, 10, 20, 20, 0xFF00FFFF))
                .issues(WEBP_ELIGIBLE)
                .run()
                .expect(expected);

    }

    public void testWebpNotEligibleForLauncherIcons() throws Exception {
        lint().files(
                manifest().minSdk(18),
                image("res/drawable-mdpi/ic_launcher.png", 48, 48).fill(10, 10, 20, 20, 0xFF00FFFF))
                .issues(WEBP_ELIGIBLE)
                .run()
                .expectClean();
    }

    public void testWebpUnsupported() throws Exception {
        String expected = ""
                + "res/drawable-mdpi/ic_launcher.webp: Error: Launcher icons must be in PNG format [WebpUnsupported]\n"
                + "res/mipmap-mdpi/my_lossless.webp: Error: WebP extended or lossless format requires Android 4.2.1 (API 18); current minSdkVersion is 10 [WebpUnsupported]\n"
                + "res/drawable-mdpi-v13/my_lossless.webp: Error: WebP extended or lossless format requires Android 4.2.1 (API 18); current minSdkVersion is 13 [WebpUnsupported]\n"
                + "res/drawable-mdpi/my_lossy.webp: Error: WebP requires Android 4.0 (API 15); current minSdkVersion is 10 [WebpUnsupported]\n"
                + "4 errors, 0 warnings\n";

        lint().files(
                manifest().minSdk(10),
                // "PNG" format: cheating since we don't have a WEBP encoder outside of
                // Studio, but we know lint won't actually look inside these files
                // yet

                // OK: lossy webp okay in API 15 and up
                base64gzip("res/drawable-mdpi-v15/my_lossy.webp", ""
                        + "H4sIAAAAAAAAAAvydHPzYGBgCHd1CggLsFCwAbIvMDPMZdSyYrBgsJvoscBH"
                        + "dYmykhIHwwYhzkyGMgYGhbxlC7g+chcxMPw7vf3/Wx8ht6D//wV23zjUANTK"
                        + "AADVeQHzUAAAAA=="),

                // Error: requires API level 15
                base64gzip("res/drawable-mdpi/my_lossy.webp", ""
                        + "H4sIAAAAAAAAAAvydHPzYGBgCHd1CggLsFCwAbIvMDPMZdSyYrBgsJvoscBH"
                        + "dYmykhIHwwYhzkyGMgYGhbxlC7g+chcxMPw7vf3/Wx8ht6D//wV23zjUANTK"
                        + "AADVeQHzUAAAAA=="),

                // Error: requires API level 18
                base64gzip("res/mipmap-mdpi/my_lossless.webp", ""
                        + "H4sIAAAAAAAAAAvydHNTYWBgCHd1CggLsPARB7L1LQ/wMrBf2O/3ddt/RgXF"
                        + "P/XrXb/Yf2VkAABv2HPZLAAAAA=="),

                // Error: requires API level 18; has minSdk 13
                base64gzip("res/drawable-mdpi-v13/my_lossless.webp", ""
                        + "H4sIAAAAAAAAAAvydHNTYWBgCHd1CggLsPARB7L1LQ/wMrBf2O/3ddt/RgXF"
                        + "P/XrXb/Yf2VkAABv2HPZLAAAAA=="),

                // OK: requires API 15 but is in v16 folder
                base64gzip("res/drawable-mdpi-v16/my_lossless.webp", "" // still not okay needs 18
                        + "H4sIAAAAAAAAAAvydHPzYGBgCHd1CggLsFCwAbIvMDPMZdSyYrBgsJvoscBH"
                        + "dYmykhIHwwYhzkyGMgYGhbxlC7g+chcxMPw7vf3/Wx8ht6D//wV23zjUANTK"
                        + "AADVeQHzUAAAAA=="),

                // OK: requires API 18 but is in v18 folder
                base64gzip("res/drawable-mdpi-v18/my_lossless.webp", "" // OK 18
                        + "H4sIAAAAAAAAAAvydHNTYWBgCHd1CggLsPARB7L1LQ/wMrBf2O/3ddt/RgXF"
                        + "P/XrXb/Yf2VkAABv2HPZLAAAAA=="),

                // Error: launcher icons can't be in WEBP or XML
                base64gzip("res/drawable-mdpi/ic_launcher.webp", ""
                        + "H4sIAAAAAAAAAAvydHPzYGBgCHd1CggLsFCwAbIvMDPMZdSyYrBgsJvoscBH"
                        + "dYmykhIHwwYhzkyGMgYGhbxlC7g+chcxMPw7vf3/Wx8ht6D//wV23zjUANTK"
                        + "AADVeQHzUAAAAA=="))
                .issues(WEBP_ELIGIBLE, WEBP_UNSUPPORTED)
                .run()
                .expect(expected);
    }

    public void testXmlLauncherIconsAllowed() throws Exception {
        lint().files(
                manifest().minSdk(24),
                // Now vectors are allowed
                xml("res/drawable/ic_launcher.xml", ""
                        + "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" />\n"))
                .run()
                .expectClean();
    }

    public void test118398_a() throws Exception {
        // Regression test for http://b.android.com/118398
        lint().files(
                // Use minSDK4 to ensure that we get warnings about missing drawables
                manifest().minSdk(4),
                image("res/drawable-hdpi/ic_action_name.png", 48, 48),
                image("res/drawable-hdpi/ic_stat_name.png", 48, 48),
                image("res/drawable-hdpi-v11/ic_stat_name.png", 48, 48),
                image("res/drawable-hdpi-v9/ic_stat_name.png", 48, 48),
                image("res/drawable-mdpi/ic_action_name.png", 48, 48),
                image("res/drawable-mdpi/ic_stat_name.png", 48, 48),
                image("res/drawable-mdpi-v11/ic_stat_name.png", 48, 48),
                image("res/drawable-mdpi-v9/ic_stat_name.png", 48, 48),
                image("res/drawable-xhdpi/ic_action_name.png", 48, 48),
                image("res/drawable-xhdpi/ic_stat_name.png", 48, 48),
                image("res/drawable-xhdpi-v11/ic_stat_name.png", 48, 48),
                image("res/drawable-xhdpi-v9/ic_stat_name.png", 48, 48),
                image("res/drawable-xxhdpi/ic_action_name.png", 48, 48),
                image("res/drawable-xxhdpi/ic_stat_name.png", 48, 48),
                image("res/drawable-xxhdpi-v11/ic_stat_name.png", 48, 48),
                image("res/drawable-xxhdpi-v9/ic_stat_name.png", 48, 48),
                image("res/mipmap-hdpi/ic_launcher.png", 48, 48),
                image("res/mipmap-hdpi/ic_launcher_round.png", 48, 48),
                image("res/mipmap-mdpi/ic_launcher.png", 48, 48),
                image("res/mipmap-mdpi/ic_launcher_round.png", 48, 48),
                image("res/mipmap-xhdpi/ic_launcher.png", 48, 48),
                image("res/mipmap-xhdpi/ic_launcher_round.png", 48, 48),
                image("res/mipmap-xxhdpi/ic_launcher.png", 48, 48),
                image("res/mipmap-xxhdpi/ic_launcher_round.png", 48, 48),
                image("res/mipmap-xxxhdpi/ic_launcher.png", 48, 48),
                image("res/mipmap-xxxhdpi/ic_launcher_round.png", 48, 48))
                .issues(ICON_DENSITIES, ICON_MISSING_FOLDER)
                .run()
                .expectClean();
    }

    public void test118398_b() throws Exception {
        // Regression test for http://b.android.com/118398
        lint().files(
                // Use minSDK4 to ensure that we get warnings about missing drawables
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"pkg.my.myapplication\">\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:icon=\"@drawable/ic_action_name\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:roundIcon=\"@mipmap/ic_launcher_round\"/>\n"
                        + "</manifest>"),
                image("res/drawable-hdpi/ic_action_name.png", 48, 48),
                image("res/drawable-hdpi/ic_stat_name.png", 48, 48),
                image("res/drawable-hdpi-v11/ic_stat_name.png", 48, 48),
                image("res/drawable-hdpi-v9/ic_stat_name.png", 48, 48),
                image("res/drawable-mdpi/ic_action_name.png", 48, 48),
                image("res/drawable-mdpi/ic_stat_name.png", 48, 48),
                image("res/drawable-mdpi-v11/ic_stat_name.png", 48, 48),
                image("res/drawable-mdpi-v9/ic_stat_name.png", 48, 48),
                image("res/drawable-xhdpi/ic_action_name.png", 48, 48),
                image("res/drawable-xhdpi/ic_stat_name.png", 48, 48),
                image("res/drawable-xhdpi-v11/ic_stat_name.png", 48, 48),
                image("res/drawable-xhdpi-v9/ic_stat_name.png", 48, 48),
                image("res/drawable-xxhdpi/ic_action_name.png", 48, 48),
                image("res/drawable-xxhdpi/ic_stat_name.png", 48, 48),
                image("res/drawable-xxhdpi-v11/ic_stat_name.png", 48, 48),
                image("res/drawable-xxhdpi-v9/ic_action_name.png", 48, 48),
                image("res/drawable-xxxhdpi/ic_stat_name.png", 48, 48),
                image("res/mipmap-hdpi/ic_launcher.png", 48, 48),
                image("res/mipmap-hdpi/ic_launcher_round.png", 48, 48),
                image("res/mipmap-mdpi/ic_launcher.png", 48, 48),
                image("res/mipmap-mdpi/ic_launcher_round.png", 48, 48),
                image("res/mipmap-xhdpi/ic_launcher.png", 48, 48),
                image("res/mipmap-xhdpi/ic_launcher_round.png", 48, 48),
                image("res/mipmap-xxhdpi/ic_launcher.png", 48, 48),
                image("res/mipmap-xxhdpi/ic_launcher_round.png", 48, 48),
                image("res/mipmap-xxxhdpi/ic_launcher.png", 48, 48),
                image("res/mipmap-xxxhdpi/ic_launcher_round.png", 48, 48))
                .issues(ICON_DENSITIES, ICON_MISSING_FOLDER)
                .run()
                .expectClean();
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mActionBarTest = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.app.Activity;\n"
            + "import android.view.Menu;\n"
            + "import android.view.MenuInflater;\n"
            + "\n"
            + "public class ActionBarTest extends Activity {\n"
            + "    @Override\n"
            + "    public boolean onCreateOptionsMenu(Menu menu) {\n"
            + "        MenuInflater inflater = getMenuInflater();\n"
            + "        inflater.inflate(R.menu.menu, menu);\n"
            + "        return true;\n"
            + "    }\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mMenu = xml("res/menu/menu.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\" >\n"
            + "\n"
            + "    <item\n"
            + "        android:id=\"@+id/item1\"\n"
            + "        android:icon=\"@drawable/icon1\"\n"
            + "        android:title=\"My title 1\">\n"
            + "    </item>\n"
            + "    <item\n"
            + "        android:id=\"@+id/item2\"\n"
            + "        android:icon=\"@drawable/icon2\"\n"
            + "        android:showAsAction=\"ifRoom\"\n"
            + "        android:title=\"My title 2\">\n"
            + "    </item>\n"
            + "\n"
            + "</menu>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mNotificationTest = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.app.Notification;\n"
            + "import android.app.Notification.Builder;\n"
            + "import android.content.Context;\n"
            + "import android.graphics.Bitmap;\n"
            + "\n"
            + "@SuppressWarnings({ \"deprecation\", \"unused\", \"javadoc\" })\n"
            + "class NotificationTest {\n"
            + "    public void test1() {\n"
            + "        Notification notification = new Notification(R.drawable.icon1, \"Test1\", 0);\n"
            + "    }\n"
            + "\n"
            + "    public void test2() {\n"
            + "        int resource = R.drawable.icon2;\n"
            + "        Notification notification = new Notification(resource, \"Test1\", 0);\n"
            + "    }\n"
            + "\n"
            + "    public void test3() {\n"
            + "        int icon = R.drawable.icon3;\n"
            + "        CharSequence tickerText = \"Hello\";\n"
            + "        long when = System.currentTimeMillis();\n"
            + "        Notification notification = new Notification(icon, tickerText, when);\n"
            + "    }\n"
            + "\n"
            + "    public void test4(Context context, String sender, String subject, Bitmap bitmap) {\n"
            + "        Notification notification = new Notification.Builder(context)\n"
            + "                .setContentTitle(\"New mail from \" + sender.toString())\n"
            + "                .setContentText(subject).setSmallIcon(R.drawable.icon4)\n"
            + "                .setLargeIcon(bitmap).build();\n"
            + "    }\n"
            + "\n"
            + "    public void test5(Context context, String sender, String subject, Bitmap bitmap) {\n"
            + "        Notification notification = new Builder(context)\n"
            + "                .setContentTitle(\"New mail from \" + sender.toString())\n"
            + "                .setContentText(subject).setSmallIcon(R.drawable.icon5)\n"
            + "                .setLargeIcon(bitmap).build();\n"
            + "    }\n"
            + "}\n");

}
