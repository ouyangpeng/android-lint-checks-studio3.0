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

import com.android.tools.lint.checks.infrastructure.TestFile.JarTestFile;
import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings("SpellCheckingInspection")
public class AppCompatResourceDetectorTest extends AbstractCheckTest {
    public void testNotGradleProject() throws Exception {
        // dependsOn('appcompat') should reliably work even for
        // non gradle projects.
        String expected = ""
                + "res/menu/showAction1.xml:6: Error: Should use android:showAsAction when not using the appcompat library [AppCompatResource]\n"
                + "        app:showAsAction=\"never\" />\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n";
        lint().files(mShowAction1)
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for res/menu/showAction1.xml line 5: Update to android:showAsAction:\n"
                        + "@@ -8 +8\n"
                        + "-         android:title=\"@string/action_settings\"\n"
                        + "-         app:showAsAction=\"never\"/>\n"
                        + "+         android:showAsAction=\"never\"\n"
                        + "+         android:title=\"@string/action_settings\"/>\n");
    }

    public void testNoAppCompat() throws Exception {
        lint().files(
                mShowAction1,
                mLibrary) // dummy; only name counts
                .run()
                .expect(""
                        + "res/menu/showAction1.xml:6: Error: Should use android:showAsAction when not using the appcompat library [AppCompatResource]\n"
                        + "        app:showAsAction=\"never\" />\n"
                        + "        ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n");
    }

    public void testCorrectAppCompat() throws Exception {
        lint().files(mShowAction1, mAppCompatJar)
                .run()
                .expectClean();
    }

    public void testWrongAppCompat() throws Exception {
        lint().files(
                mShowAction2,
                mAppCompatJar,
                mLibrary) // dummy; only name counts
                .run()
                .expect(""
                        + "res/menu/showAction2.xml:5: Error: Should use app:showAsAction with the appcompat library with xmlns:app=\"http://schemas.android.com/apk/res-auto\" [AppCompatResource]\n"
                        + "        android:showAsAction=\"never\" />\n"
                        + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n");
    }

    public void testAppCompatV14() throws Exception {
        lint().files(
                mShowAction2_class,
                mAppCompatJar,
                mLibrary) // dummy; only name counts
                .run()
                .expectClean();
    }

    public void testActionProviderClass() throws Exception {
        lint().files(mShowAction3_class, mAppCompatJar)
                .run()
                .expect("res/menu/showAction3.xml:5: Error: Should use app:showAsAction with the appcompat library with xmlns:app=\"http://schemas.android.com/apk/res-auto\" [AppCompatResource]\n"
                        + "     android:showAsAction=\"ifRoom\"\n"
                        + "     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "res/menu/showAction3.xml:6: Error: Should use app:actionProviderClass with the appcompat library with xmlns:app=\"http://schemas.android.com/apk/res-auto\" [AppCompatResource]\n"
                        + "     android:actionProviderClass=\"android.widget.ShareActionProvider\" />\n"
                        + "     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "2 errors, 0 warnings\n")
                .expectFixDiffs(""
                        + "Fix for res/menu/showAction3.xml line 4: Update to app:showAsAction:\n"
                        + "@@ -2 +2\n"
                        + "- <menu xmlns:android=\"http://schemas.android.com/apk/res/android\" >\n"
                        + "+ <menu xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "+     xmlns:app=\"http://schemas.android.com/apk/res-auto\" >\n"
                        + "@@ -8 +9\n"
                        + "-         android:showAsAction=\"ifRoom\"\n"
                        + "-         android:title=\"@string/action_share\"/>\n"
                        + "+         android:title=\"@string/action_share\"\n"
                        + "+         app:showAsAction=\"ifRoom\"/>\n"
                        +
                        "Fix for res/menu/showAction3.xml line 5: Update to app:actionProviderClass:\n"
                        + "@@ -2 +2\n"
                        + "- <menu xmlns:android=\"http://schemas.android.com/apk/res/android\" >\n"
                        + "+ <menu xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "+     xmlns:app=\"http://schemas.android.com/apk/res-auto\" >\n"
                        + "@@ -6 +7\n"
                        +
                        "-         android:actionProviderClass=\"android.widget.ShareActionProvider\"\n"
                        + "@@ -9 +9\n"
                        + "-         android:title=\"@string/action_share\"/>\n"
                        + "+         android:title=\"@string/action_share\"\n"
                        +
                        "+         app:actionProviderClass=\"android.widget.ShareActionProvider\"/>\n");
    }

    public void testActionViewClass() throws Exception {
        String expected = ""
                + "res/menu/showAction3.xml:5: Error: Should use app:showAsAction with the appcompat library with xmlns:app=\"http://schemas.android.com/apk/res-auto\" [AppCompatResource]\n"
                + "     android:showAsAction=\"ifRoom\"\n"
                + "     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/menu/showAction3.xml:6: Error: Should use app:actionViewClass with the appcompat library with xmlns:app=\"http://schemas.android.com/apk/res-auto\" [AppCompatResource]\n"
                + "     android:actionViewClass=\"android.widget.SearchView\" />\n"
                + "     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "2 errors, 0 warnings\n";
        lint().files(mShowAction4_class, mAppCompatJar)
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for res/menu/showAction3.xml line 4: Update to app:showAsAction:\n"
                        + "@@ -2 +2\n"
                        + "- <menu xmlns:android=\"http://schemas.android.com/apk/res/android\" >\n"
                        + "+ <menu xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "+     xmlns:app=\"http://schemas.android.com/apk/res-auto\" >\n"
                        + "@@ -8 +9\n"
                        + "-         android:showAsAction=\"ifRoom\"\n"
                        + "-         android:title=\"@string/action_search\"/>\n"
                        + "+         android:title=\"@string/action_search\"\n"
                        + "+         app:showAsAction=\"ifRoom\"/>\n"
                        + "Fix for res/menu/showAction3.xml line 5: Update to app:actionViewClass:\n"
                        + "@@ -2 +2\n"
                        + "- <menu xmlns:android=\"http://schemas.android.com/apk/res/android\" >\n"
                        + "+ <menu xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "+     xmlns:app=\"http://schemas.android.com/apk/res-auto\" >\n"
                        + "@@ -6 +7\n"
                        + "-         android:actionViewClass=\"android.widget.SearchView\"\n"
                        + "@@ -9 +9\n"
                        + "-         android:title=\"@string/action_search\"/>\n"
                        + "+         android:title=\"@string/action_search\"\n"
                        + "+         app:actionViewClass=\"android.widget.SearchView\"/>\n");
    }

    @Override
    protected Detector getDetector() {
        return new AppCompatResourceDetector();
    }

    // Dummy file
    private final JarTestFile mAppCompatJar = jar("libs/appcompat-v7-18.0.0.jar");

    @SuppressWarnings("all") // Sample code
    private TestFile mLibrary = source("build.gradle", "");

    @SuppressWarnings("all") // Sample code
    private TestFile mShowAction1 = xml("res/menu/showAction1.xml", ""
            + "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\">\n"
            + "    <item android:id=\"@+id/action_settings\"\n"
            + "        android:title=\"@string/action_settings\"\n"
            + "        android:orderInCategory=\"100\"\n"
            + "        app:showAsAction=\"never\" />\n"
            + "</menu>\n"
            + "\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mShowAction2 = xml("res/menu/showAction2.xml", ""
            + "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
            + "    <item android:id=\"@+id/action_settings\"\n"
            + "        android:title=\"@string/action_settings\"\n"
            + "        android:orderInCategory=\"100\"\n"
            + "        android:showAsAction=\"never\" />\n"
            + "</menu>\n"
            + "\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mShowAction2_class = xml("res/menu-v14/showAction2.xml", ""
            + "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
            + "    <item android:id=\"@+id/action_settings\"\n"
            + "        android:title=\"@string/action_settings\"\n"
            + "        android:orderInCategory=\"100\"\n"
            + "        android:showAsAction=\"never\" />\n"
            + "</menu>\n"
            + "\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mShowAction3_class = xml("res/menu/showAction3.xml", ""
            + "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
            + "    <item android:id=\"@+id/action_share\"\n"
            + "     android:title=\"@string/action_share\"\n"
            + "     android:icon=\"@drawable/ic_share\"\n"
            + "     android:showAsAction=\"ifRoom\"\n"
            + "     android:actionProviderClass=\"android.widget.ShareActionProvider\" />\n"
            + "</menu>\n"
            + "\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mShowAction4_class = xml("res/menu/showAction3.xml", ""
            + "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
            + "    <item android:id=\"@+id/action_search\"\n"
            + "     android:title=\"@string/action_search\"\n"
            + "     android:icon=\"@drawable/ic_search\"\n"
            + "     android:showAsAction=\"ifRoom\"\n"
            + "     android:actionViewClass=\"android.widget.SearchView\" />\n"
            + "</menu>\n"
            + "\n");
}
