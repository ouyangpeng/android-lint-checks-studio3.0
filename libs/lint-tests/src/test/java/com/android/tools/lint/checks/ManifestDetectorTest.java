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

import static com.android.tools.lint.checks.GradleDetectorTest.createRelativePaths;
import static com.android.tools.lint.checks.infrastructure.ProjectDescription.Type.LIBRARY;

import com.android.testutils.TestUtils;
import com.android.tools.lint.checks.infrastructure.ProjectDescription;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("javadoc")
public class ManifestDetectorTest extends AbstractCheckTest {

    private File mSdkDir;

    @Override
    protected Detector getDetector() {
        return new ManifestDetector();
    }

    private Set<Issue> mEnabled = new HashSet<>();

    @Override
    protected boolean isEnabled(Issue issue) {
        return super.isEnabled(issue) && mEnabled.contains(issue);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        if (mSdkDir != null) {
            deleteFile(mSdkDir);
            mSdkDir = null;
        }
    }

    public void testOrderOk() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.ORDER);
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",
                lintProject(
                        manifest().minSdk(14),
                        mStrings));
    }

    public void testBrokenOrder() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.ORDER);
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:16: Warning: <uses-sdk> tag appears after <application> tag [ManifestOrder]\n"
                + "   <uses-sdk android:minSdkVersion=\"Froyo\" />\n"
                + "   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",

            lintProject(
                    xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "     package=\"com.example.helloworld\"\n"
                            + "     android:versionCode=\"1\"\n"
                            + "     android:versionName=\"1.0\">\n"
                            + "   <application android:icon=\"@drawable/icon\" android:label=\"@string/app_name\">\n"
                            + "       <activity android:name=\".HelloWorld\"\n"
                            + "                 android:label=\"@string/app_name\">\n"
                            + "           <intent-filter>\n"
                            + "               <action android:name=\"android.intent.action.MAIN\" />\n"
                            + "               <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                            + "           </intent-filter>\n"
                            + "       </activity>\n"
                            + "\n"
                            + "   </application>\n"
                            + "   <uses-sdk android:minSdkVersion=\"Froyo\" />\n"
                            + "\n"
                            + "</manifest>\n"),
                    mStrings));
    }

    public void testMissingUsesSdk() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.USES_SDK);
        assertEquals(""
                + "AndroidManifest.xml: Warning: Manifest should specify a minimum API level with <uses-sdk android:minSdkVersion=\"?\" />; if it really supports all versions of Android set it to 1. [UsesMinSdkAttributes]\n"
                + "0 errors, 1 warnings\n",
            lintProject(
                    mMissingusessdk,
                    mStrings));
    }

    public void testMissingUsesSdkInGradle() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.SET_VERSION);
        assertEquals(""
                + "No warnings.",
                lintProject(mMissingusessdk,
                        mLibrary)); // dummy; only name counts
    }

    public void testMissingMinSdk() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.USES_SDK);
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:7: Warning: <uses-sdk> tag should specify a minimum API level with android:minSdkVersion=\"?\" [UsesMinSdkAttributes]\n"
                + "    <uses-sdk android:targetSdkVersion=\"10\" />\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",
            lintProject(
                    xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"test.bytecode\"\n"
                            + "    android:versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\" >\n"
                            + "\n"
                            + "    <uses-sdk android:targetSdkVersion=\"10\" />\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <activity\n"
                            + "            android:name=\".BytecodeTestsActivity\"\n"
                            + "            android:label=\"@string/app_name\" >\n"
                            + "            <intent-filter>\n"
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

    public void testMissingTargetSdk() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.USES_SDK);
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:7: Warning: <uses-sdk> tag should specify a target API level (the highest verified version; when running on later versions, compatibility behaviors may be enabled) with android:targetSdkVersion=\"?\" [UsesMinSdkAttributes]\n"
                + "    <uses-sdk android:minSdkVersion=\"10\" />\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",
            lintProject(
                    xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"test.bytecode\"\n"
                            + "    android:versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\" >\n"
                            + "\n"
                            + "    <uses-sdk android:minSdkVersion=\"10\" />\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <activity\n"
                            + "            android:name=\".BytecodeTestsActivity\"\n"
                            + "            android:label=\"@string/app_name\" >\n"
                            + "            <intent-filter>\n"
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

    public void testOldTargetSdk() throws Exception {
        String expected = ""
                + "AndroidManifest.xml:7: Warning: Not targeting the latest versions of Android; compatibility modes apply. Consider testing and updating this version. Consult the android.os.Build.VERSION_CODES javadoc for details. [OldTargetApi]\n"
                + "    <uses-sdk android:minSdkVersion=\"10\" android:targetSdkVersion=\"14\" />\n"
                + "                                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                        manifest(
                                ""
                                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                        + "    package=\"test.bytecode\"\n"
                                        + "    android:versionCode=\"1\"\n"
                                        + "    android:versionName=\"1.0\" >\n"
                                        + "\n"
                                        + "    <uses-sdk android:minSdkVersion=\"10\" android:targetSdkVersion=\"14\" />\n"
                                        + "\n"
                                        + "    <application\n"
                                        + "        android:icon=\"@drawable/ic_launcher\"\n"
                                        + "        android:label=\"@string/app_name\" >\n"
                                        + "        <activity\n"
                                        + "            android:name=\".BytecodeTestsActivity\"\n"
                                        + "            android:label=\"@string/app_name\" >\n"
                                        + "            <intent-filter>\n"
                                        + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                                        + "\n"
                                        + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                                        + "            </intent-filter>\n"
                                        + "        </activity>\n"
                                        + "    </application>\n"
                                        + "\n"
                                        + "</manifest>\n"),
                        mStrings)
                .issues(ManifestDetector.TARGET_NEWER)
                .run()
                .expect(expected)
                .expectFixDiffs(
                        ""
                                + "Fix for AndroidManifest.xml line 6: Update targetSdkVersion to 26:\n"
                                + "@@ -7 +7\n"
                                + "-     <uses-sdk android:minSdkVersion=\"10\" android:targetSdkVersion=\"14\" />\n"
                                + "+     <uses-sdk android:minSdkVersion=\"10\" android:targetSdkVersion=\"26\" />\n");
    }

    public void testMultipleSdk() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.MULTIPLE_USES_SDK);
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:8: Error: There should only be a single <uses-sdk> element in the manifest: merge these together [MultipleUsesSdk]\n"
                + "    <uses-sdk android:targetSdkVersion=\"14\" />\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    AndroidManifest.xml:7: Also appears here\n"
                + "    AndroidManifest.xml:9: Also appears here\n"
                + "1 errors, 0 warnings\n",

            lintProject(
                    xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"test.bytecode\"\n"
                            + "    android:versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\" >\n"
                            + "\n"
                            + "    <uses-sdk android:minSdkVersion=\"5\" />\n"
                            + "    <uses-sdk android:targetSdkVersion=\"14\" />\n"
                            + "    <uses-sdk android:maxSdkVersion=\"15\" />\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "        <activity\n"
                            + "            android:name=\".BytecodeTestsActivity\"\n"
                            + "            android:label=\"@string/app_name\" >\n"
                            + "            <intent-filter>\n"
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

    public void testWrongLocation() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.WRONG_PARENT);
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:8: Error: The <uses-sdk> element must be a direct child of the <manifest> root element [WrongManifestParent]\n"
                + "       <uses-sdk android:minSdkVersion=\"Froyo\" />\n"
                + "       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:9: Error: The <uses-permission> element must be a direct child of the <manifest> root element [WrongManifestParent]\n"
                + "       <uses-permission />\n"
                + "       ~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:10: Error: The <permission> element must be a direct child of the <manifest> root element [WrongManifestParent]\n"
                + "       <permission />\n"
                + "       ~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:11: Error: The <permission-tree> element must be a direct child of the <manifest> root element [WrongManifestParent]\n"
                + "       <permission-tree />\n"
                + "       ~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:12: Error: The <permission-group> element must be a direct child of the <manifest> root element [WrongManifestParent]\n"
                + "       <permission-group />\n"
                + "       ~~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:14: Error: The <uses-sdk> element must be a direct child of the <manifest> root element [WrongManifestParent]\n"
                + "       <uses-sdk />\n"
                + "       ~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:15: Error: The <uses-configuration> element must be a direct child of the <manifest> root element [WrongManifestParent]\n"
                + "       <uses-configuration />\n"
                + "       ~~~~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:16: Error: The <uses-feature> element must be a direct child of the <manifest> root element [WrongManifestParent]\n"
                + "       <uses-feature />\n"
                + "       ~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:17: Error: The <supports-screens> element must be a direct child of the <manifest> root element [WrongManifestParent]\n"
                + "       <supports-screens />\n"
                + "       ~~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:18: Error: The <compatible-screens> element must be a direct child of the <manifest> root element [WrongManifestParent]\n"
                + "       <compatible-screens />\n"
                + "       ~~~~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:19: Error: The <supports-gl-texture> element must be a direct child of the <manifest> root element [WrongManifestParent]\n"
                + "       <supports-gl-texture />\n"
                + "       ~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:24: Error: The <uses-library> element must be a direct child of the <application> element [WrongManifestParent]\n"
                + "   <uses-library />\n"
                + "   ~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:25: Error: The <activity> element must be a direct child of the <application> element [WrongManifestParent]\n"
                + "   <activity android:name=\".HelloWorld\"\n"
                + "   ^\n"
                + "13 errors, 0 warnings\n",

            lintProject(xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "     package=\"com.example.helloworld\"\n"
                            + "     android:versionCode=\"1\"\n"
                            + "     android:versionName=\"1.0\">\n"
                            + "   <application android:icon=\"@drawable/icon\" android:label=\"@string/app_name\">\n"
                            + "       <!-- Wrong declaration locations -->\n"
                            + "       <uses-sdk android:minSdkVersion=\"Froyo\" />\n"
                            + "       <uses-permission />\n"
                            + "       <permission />\n"
                            + "       <permission-tree />\n"
                            + "       <permission-group />\n"
                            + "       <instrumentation />\n"
                            + "       <uses-sdk />\n"
                            + "       <uses-configuration />\n"
                            + "       <uses-feature />\n"
                            + "       <supports-screens />\n"
                            + "       <compatible-screens />\n"
                            + "       <supports-gl-texture />\n"
                            + "\n"
                            + "   </application>\n"
                            + "\n"
                            + "   <!-- Wrong declaration locations -->\n"
                            + "   <uses-library />\n"
                            + "   <activity android:name=\".HelloWorld\"\n"
                            + "                 android:label=\"@string/app_name\" />\n"
                            + "\n"
                            + "</manifest>\n")));
    }

    public void testDuplicateActivity() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.DUPLICATE_ACTIVITY);
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:16: Error: Duplicate registration for activity com.example.helloworld.HelloWorld [DuplicateActivity]\n"
                + "       <activity android:name=\"com.example.helloworld.HelloWorld\"\n"
                + "                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

            lintProject(
                    xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "     package=\"com.example.helloworld\"\n"
                            + "     android:versionCode=\"1\"\n"
                            + "     android:versionName=\"1.0\">\n"
                            + "   <uses-sdk android:minSdkVersion=\"14\" />\n"
                            + "   <application android:icon=\"@drawable/icon\" android:label=\"@string/app_name\">\n"
                            + "       <activity android:name=\".HelloWorld\"\n"
                            + "                 android:label=\"@string/app_name\">\n"
                            + "           <intent-filter>\n"
                            + "               <action android:name=\"android.intent.action.MAIN\" />\n"
                            + "               <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                            + "           </intent-filter>\n"
                            + "       </activity>\n"
                            + "\n"
                            + "       <activity android:name=\"com.example.helloworld.HelloWorld\"\n"
                            + "                 android:label=\"@string/app_name\">\n"
                            + "       </activity>\n"
                            + "\n"
                            + "   </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                    mStrings));
    }

    public void testDuplicateActivityAcrossSourceSets() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.DUPLICATE_ACTIVITY);
        File master = getProjectDir("MasterProject",
                // Master project
                manifest().minSdk(14),
                projectProperties().property("android.library.reference.1", "../LibraryProject").property("manifestmerger.enabled", "true"),
                mMainCode
        );
        File library = getProjectDir("LibraryProject",
                // Library project
                manifest().minSdk(14),
                projectProperties().library(true).compileSdk(14),
                mLibraryCode,
                mLibraryStrings
        );
        assertEquals("No warnings.",
                checkLint(Arrays.asList(master, library)));
    }

    public void testIgnoreDuplicateActivity() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.DUPLICATE_ACTIVITY);
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintProject(
                    xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "     xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "     package=\"com.example.helloworld\"\n"
                            + "     android:versionCode=\"1\"\n"
                            + "     android:versionName=\"1.0\">\n"
                            + "   <uses-sdk android:minSdkVersion=\"14\" />\n"
                            + "   <application android:icon=\"@drawable/icon\" android:label=\"@string/app_name\" tools:ignore=\"DuplicateActivity\">\n"
                            + "       <activity android:name=\".HelloWorld\"\n"
                            + "                 android:label=\"@string/app_name\">\n"
                            + "           <intent-filter>\n"
                            + "               <action android:name=\"android.intent.action.MAIN\" />\n"
                            + "               <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                            + "           </intent-filter>\n"
                            + "       </activity>\n"
                            + "\n"
                            + "       <activity android:name=\"com.example.helloworld.HelloWorld\"\n"
                            + "                 android:label=\"@string/app_name\">\n"
                            + "       </activity>\n"
                            + "\n"
                            + "   </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                    mStrings));
    }

    public void testAllowBackup() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.ALLOW_BACKUP);
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:9: Warning: Should explicitly set android:allowBackup to true or false (it's true by default, and that can have some security implications for the application's data) [AllowBackup]\n"
                + "    <application\n"
                + "    ^\n"
                + "0 errors, 1 warnings\n",
                lintProject(
                        manifest().minSdk(14),
                        xml("AndroidManifest.xml", ""
                                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                + "    package=\"test.bytecode\"\n"
                                + "    android:versionCode=\"1\"\n"
                                + "    android:versionName=\"1.0\" >\n"
                                + "\n"
                                + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                                + "\n"
                                + "    <application\n"
                                + "        android:icon=\"@drawable/ic_launcher\"\n"
                                + "        android:label=\"@string/app_name\" >\n"
                                + "    </application>\n"
                                + "\n"
                                + "</manifest>\n"),
                        mStrings));
    }

    public void testAllowBackupOk() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.ALLOW_BACKUP);
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
                            + "        android:label=\"@string/app_name\"\n"
                            + "        android:allowBackup=\"true\" >\n"
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

    public void testAllowBackupOk2() throws Exception {
        // Requires build api >= 4
        mEnabled = Collections.singleton(ManifestDetector.ALLOW_BACKUP);
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",
                lintProject(
                        manifest().minSdk(1),
                        mStrings));
    }

    public void testAllowBackupOk3() throws Exception {
        // Not flagged in library projects
        mEnabled = Collections.singleton(ManifestDetector.ALLOW_BACKUP);
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",
                lintProject(
                        manifest().minSdk(14),
                        projectProperties().library(true).compileSdk(14),
                        mStrings));
    }

    public void testAllowIgnore() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.ALLOW_BACKUP);
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",
                lintProject(
                        xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "    package=\"foo.bar2\"\n"
                            + "    android:versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\" >\n"
                            + "\n"
                            + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\"\n"
                            + "        tools:ignore=\"AllowBackup\"\n>"
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

    public void testDuplicatePermissions() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.UNIQUE_PERMISSION);
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:12: Error: Permission name SEND_SMS is not unique (appears in both foo.permission.SEND_SMS and bar.permission.SEND_SMS) [UniquePermission]\n"
                + "    <permission android:name=\"bar.permission.SEND_SMS\"\n"
                + "                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "    AndroidManifest.xml:9: Previous permission here\n"
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
                            + "    <permission android:name=\"foo.permission.SEND_SMS\"\n"
                            + "        android:label=\"@string/foo\"\n"
                            + "        android:description=\"@string/foo\" />\n"
                            + "    <permission android:name=\"bar.permission.SEND_SMS\"\n"
                            + "        android:label=\"@string/foo\"\n"
                            + "        android:description=\"@string/foo\" />\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                        mStrings));
    }

    public void testDuplicatePermissionsMultiProject() {

        //noinspection all // Sample code
        ProjectDescription library = project(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"foo.bar2\"\n"
                        + "    android:versionCode=\"1\"\n"
                        + "    android:versionName=\"1.0\" >\n"
                        + "\n"
                        + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                        + "\n"
                        + "    <permission android:name=\"bar.permission.SEND_SMS\"\n"
                        + "        android:label=\"@string/foo\"\n"
                        + "        android:description=\"@string/foo\" />\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:icon=\"@drawable/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\" >\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n")
        ).type(LIBRARY).name("Library");

        //noinspection all // Sample code
        ProjectDescription main = project(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"foo.bar2\"\n"
                        + "    android:versionCode=\"1\"\n"
                        + "    android:versionName=\"1.0\" >\n"
                        + "\n"
                        + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                        + "\n"
                        + "    <permission android:name=\"foo.permission.SEND_SMS\"\n"
                        + "        android:label=\"@string/foo\"\n"
                        + "        android:description=\"@string/foo\" />\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:icon=\"@drawable/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\" >\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n")
        ).name("App").dependsOn(library);

        lint()
                .projects(main, library)
                .incremental("App/AndroidManifest.xml").
                issues(ManifestDetector.UNIQUE_PERMISSION)
                .run()
                .expect(""
                        + "../Library/AndroidManifest.xml:9: Error: Permission name SEND_SMS is not unique (appears in both foo.permission.SEND_SMS and bar.permission.SEND_SMS) [UniquePermission]\n"
                        + "    <permission android:name=\"bar.permission.SEND_SMS\"\n"
                        + "                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "    AndroidManifest.xml:9: Previous permission here\n"
                        + "1 errors, 0 warnings\n");
    }

    public void testUniquePermissionsPrunedViaManifestRemove() {
        // Actually checks 4 separate things:
        // (1) The unique permission check looks across multiple projects via the
        //     manifest merge (in an incremental way)
        // (2) It allows duplicate permission names if the whole package, not just
        //     the base name is the same
        // (3) It flags permissions that vary by package name, not base name, across
        //     manifests
        // (4) It ignores permissions that have been removed via manifest merger
        //     directives. This is a regression test for
        //     https://code.google.com/p/android/issues/detail?id=227683
        // (5) Using manifest placeholders

        //noinspection all // Sample code
        ProjectDescription library = project(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg.library\" >\n"
                        + "    <permission\n"
                        + "        android:name=\"a.b.c.SHARED_ACCESS\"\n"
                        + "        android:label=\"Shared Access\"\n"
                        + "        android:protectionLevel=\"signature\"/>\n"
                        + "    <permission android:name=\"pkg1.PERMISSION_NAME_1\"/>\n"
                        + "    <permission android:name=\"${applicationId}.permission.PERMISSION_NAME_2\"/>\n"
                        + "    <permission android:name=\"${unknownPlaceHolder1}.permission.PERMISSION_NAME_3\"/>\n"
                        + "</manifest>\n")
        ).type(LIBRARY).name("Library");

        //noinspection all // Sample code
        ProjectDescription main = project(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "    package=\"test.pkg.app\" >\n"
                        + "    <permission\n"
                        + "        android:name=\"a.b.c.SHARED_ACCESS\"\n"
                        + "        tools:node=\"remove\"/>\n"
                        + "    <permission android:name=\"pkg2.PERMISSION_NAME_1\"/>\n"
                        + "    <permission android:name=\"test.pkg.app.permission.PERMISSION_NAME_2\"/>\n"
                        + "    <permission android:name=\"${unknownPlaceHolder2}.permission.PERMISSION_NAME_3\"/>\n"
                        + "</manifest>\n")
        ).name("App").dependsOn(library);

        lint()
                .projects(main, library)
                .incremental("App/AndroidManifest.xml").
                issues(ManifestDetector.UNIQUE_PERMISSION)
                .run()
                .expect(""
                        + "../Library/AndroidManifest.xml:8: Error: Permission name PERMISSION_NAME_1 is not unique (appears in both pkg2.PERMISSION_NAME_1 and pkg1.PERMISSION_NAME_1) [UniquePermission]\n"
                        + "    <permission android:name=\"pkg1.PERMISSION_NAME_1\"/>\n"
                        + "                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "    AndroidManifest.xml:5: Previous permission here\n"
                        + "1 errors, 0 warnings\n");
    }

    public void testMissingVersion() throws Exception {
        String expected = ""
                + "AndroidManifest.xml:2: Warning: Should set android:versionCode to specify the application version [MissingVersion]\n"
                + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "^\n"
                + "AndroidManifest.xml:2: Warning: Should set android:versionName to specify the application version [MissingVersion]\n"
                + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "^\n"
                + "0 errors, 2 warnings\n";
        lint().files(
                mNo_version)
                .issues(ManifestDetector.SET_VERSION)
                .run()
                .expect(expected)
                .verifyFixes().window(1).expectFixDiffs(""
                + "Fix for AndroidManifest.xml line 1: Set versionCode:\n"
                + "@@ -3 +3\n"
                + "  <manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "-     package=\"foo.bar2\" >\n"
                + "+     package=\"foo.bar2\"\n"
                + "+     android:versionCode=\"|\" >\n"
                + "  \n"
                + "Fix for AndroidManifest.xml line 1: Set versionName:\n"
                + "@@ -3 +3\n"
                + "  <manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "-     package=\"foo.bar2\" >\n"
                + "+     package=\"foo.bar2\"\n"
                + "+     android:versionName=\"|\" >\n"
                + "  \n");
    }

    public void testVersionNotMissingInGradleProjects() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.SET_VERSION);
        assertEquals(""
            + "No warnings.",
            lintProject(mNo_version,
                    mLibrary)); // dummy; only name counts
    }

    public void testIllegalReference() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.ILLEGAL_REFERENCE);
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:4: Warning: The android:versionCode cannot be a resource url, it must be a literal integer [IllegalResourceRef]\n"
                + "    android:versionCode=\"@dimen/versionCode\"\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:7: Warning: The android:minSdkVersion cannot be a resource url, it must be a literal integer (or string if a preview codename) [IllegalResourceRef]\n"
                + "    <uses-sdk android:minSdkVersion=\"@dimen/minSdkVersion\" android:targetSdkVersion=\"@dimen/targetSdkVersion\" />\n"
                + "              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:7: Warning: The android:targetSdkVersion cannot be a resource url, it must be a literal integer (or string if a preview codename) [IllegalResourceRef]\n"
                + "    <uses-sdk android:minSdkVersion=\"@dimen/minSdkVersion\" android:targetSdkVersion=\"@dimen/targetSdkVersion\" />\n"
                + "                                                           ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 3 warnings\n",

            lintProject(xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"foo.bar2\"\n"
                            + "    android:versionCode=\"@dimen/versionCode\"\n"
                            + "    android:versionName=\"@dimen/versionName\" >\n"
                            + "\n"
                            + "    <uses-sdk android:minSdkVersion=\"@dimen/minSdkVersion\" android:targetSdkVersion=\"@dimen/targetSdkVersion\" />\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
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
                            + "</manifest>\n")));
    }

    public void testDuplicateUsesFeature() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.DUPLICATE_USES_FEATURE);
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:11: Warning: Duplicate declaration of uses-feature android.hardware.camera [DuplicateUsesFeature]\n"
                + "    <uses-feature android:name=\"android.hardware.camera\"/>\n"
                + "                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",
            lintProject(
                    xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "    package=\"foo.bar2\"\n"
                            + "    android:versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\">\n"
                            + "\n"
                            + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                            + "\n"
                            + "    <uses-feature android:name=\"android.hardware.camera\"/>\n"
                            + "    <uses-feature android:name=\"android.hardware.camera\"/>\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                    mStrings));
    }

    public void testDuplicateUsesFeatureOk() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.DUPLICATE_USES_FEATURE);
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",
            lintProject(
                    xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "    package=\"foo.bar2\"\n"
                            + "    android:versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\">\n"
                            + "\n"
                            + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                            + "\n"
                            + "    <uses-feature android:name=\"android.hardware.camera\"/>\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\" >\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                    mStrings));
    }

    public void testMissingApplicationIcon() throws Exception {
        String expected = ""
                + "AndroidManifest.xml:9: Warning: Should explicitly set android:icon, there is no default [MissingApplicationIcon]\n"
                + "    <application\n"
                + "    ^\n"
                + "0 errors, 1 warnings\n";
        lint().files(
                mMissing_application_icon,
                mStrings)
                .issues(ManifestDetector.APPLICATION_ICON)
                .run()
                .expect(expected)
                .verifyFixes().window(1).expectFixDiffs(""
                + "Fix for AndroidManifest.xml line 8: Set icon:\n"
                + "@@ -9 +9\n"
                + "  \n"
                + "-     <application android:label=\"@string/app_name\" >\n"
                + "+     <application\n"
                + "+         android:icon=\"@mipmap/|\"\n"
                + "+         android:label=\"@string/app_name\" >\n"
                + "          <activity\n");
    }

    public void testMissingApplicationIconInLibrary() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.APPLICATION_ICON);
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",
            lintProject(
                mMissing_application_icon,
                projectProperties().library(true).compileSdk(14),
                mStrings));
    }

    public void testMissingApplicationIconOk() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.APPLICATION_ICON);
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",
            lintProject(
                manifest().minSdk(14),
                mStrings));
    }

    public void testDeviceAdmin() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.DEVICE_ADMIN);
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:31: Warning: You must have an intent filter for action android.app.action.DEVICE_ADMIN_ENABLED [DeviceAdmin]\n"
                + "            <meta-data android:name=\"android.app.device_admin\"\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:44: Warning: You must have an intent filter for action android.app.action.DEVICE_ADMIN_ENABLED [DeviceAdmin]\n"
                + "            <meta-data android:name=\"android.app.device_admin\"\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:56: Warning: You must have an intent filter for action android.app.action.DEVICE_ADMIN_ENABLED [DeviceAdmin]\n"
                + "            <meta-data android:name=\"android.app.device_admin\"\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 3 warnings\n",
                lintProject(xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "          xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "          package=\"foo.bar2\"\n"
                            + "          android:versionCode=\"1\"\n"
                            + "          android:versionName=\"1.0\">\n"
                            + "\n"
                            + "    <uses-sdk android:minSdkVersion=\"14\"/>\n"
                            + "\n"
                            + "    <application\n"
                            + "            android:icon=\"@drawable/ic_launcher\"\n"
                            + "            android:label=\"@string/app_name\">\n"
                            + "\n"
                            + "        <!-- OK -->\n"
                            + "        <receiver android:name=\".DeviceAdminTestReceiver\"\n"
                            + "                  android:label=\"@string/app_name\"\n"
                            + "                  android:description=\"@string/app_name\"\n"
                            + "                  android:permission=\"android.permission.BIND_DEVICE_ADMIN\">\n"
                            + "            <meta-data android:name=\"android.app.device_admin\"\n"
                            + "                       android:resource=\"@xml/device_admin\"/>\n"
                            + "            <intent-filter>\n"
                            + "                <action android:name=\"android.app.action.DEVICE_ADMIN_ENABLED\"/>\n"
                            + "            </intent-filter>\n"
                            + "        </receiver>\n"
                            + "\n"
                            + "        <!-- Specifies data -->\n"
                            + "        <receiver android:name=\".DeviceAdminTestReceiver\"\n"
                            + "                  android:label=\"@string/app_name\"\n"
                            + "                  android:description=\"@string/app_name\"\n"
                            + "                  android:permission=\"android.permission.BIND_DEVICE_ADMIN\">\n"
                            + "            <meta-data android:name=\"android.app.device_admin\"\n"
                            + "                       android:resource=\"@xml/device_admin\"/>\n"
                            + "            <intent-filter>\n"
                            + "                <action android:name=\"android.app.action.DEVICE_ADMIN_ENABLED\"/>\n"
                            + "                <data android:scheme=\"content\" />\n"
                            + "            </intent-filter>\n"
                            + "        </receiver>\n"
                            + "\n"
                            + "        <!-- Missing right intent-filter -->\n"
                            + "        <receiver android:name=\".DeviceAdminTestReceiver\"\n"
                            + "                  android:label=\"@string/app_name\"\n"
                            + "                  android:description=\"@string/app_name\"\n"
                            + "                  android:permission=\"android.permission.BIND_DEVICE_ADMIN\">\n"
                            + "            <meta-data android:name=\"android.app.device_admin\"\n"
                            + "                       android:resource=\"@xml/device_admin\"/>\n"
                            + "            <intent-filter>\n"
                            + "                <action android:name=\"com.test.foo.DEVICE_ADMIN_ENABLED\"/>\n"
                            + "            </intent-filter>\n"
                            + "        </receiver>\n"
                            + "\n"
                            + "        <!-- Missing intent-filter -->\n"
                            + "        <receiver android:name=\".DeviceAdminTestReceiver\"\n"
                            + "                  android:label=\"@string/app_name\"\n"
                            + "                  android:description=\"@string/app_name\"\n"
                            + "                  android:permission=\"android.permission.BIND_DEVICE_ADMIN\">\n"
                            + "            <meta-data android:name=\"android.app.device_admin\"\n"
                            + "                       android:resource=\"@xml/device_admin\"/>\n"
                            + "        </receiver>\n"
                            + "\n"
                            + "        <!-- Suppressed -->\n"
                            + "        <receiver android:name=\".DeviceAdminTestReceiver\"\n"
                            + "                  android:label=\"@string/app_name\"\n"
                            + "                  android:description=\"@string/app_name\"\n"
                            + "                  android:permission=\"android.permission.BIND_DEVICE_ADMIN\"\n"
                            + "                  tools:ignore=\"DeviceAdmin\">\n"
                            + "            <meta-data android:name=\"android.app.device_admin\"\n"
                            + "                       android:resource=\"@xml/device_admin\"/>\n"
                            + "            <intent-filter>\n"
                            + "                <action android:name=\"com.test.foo.DEVICE_ADMIN_ENABLED\"/>\n"
                            + "            </intent-filter>\n"
                            + "        </receiver>\n"
                            + "\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n")));
    }

    public void testMockLocations() throws Exception {
        String expected = ""
                + "src/main/AndroidManifest.xml:9: Error: Mock locations should only be requested in a test or debug-specific manifest file (typically src/debug/AndroidManifest.xml) [MockLocation]\n"
                + "    <uses-permission android:name=\"android.permission.ACCESS_MOCK_LOCATION\" /> \n"
                + "                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n";

        lint().files(
                xml("src/main/AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"foo.bar2\"\n"
                        + "    android:versionCode=\"1\"\n"
                        + "    android:versionName=\"1.0\" >\n"
                        + "\n"
                        + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                        + "    <uses-permission android:name=\"com.example.helloworld.permission\" />\n"
                        + "    <uses-permission android:name=\"android.permission.ACCESS_MOCK_LOCATION\" /> \n"
                        + "\n"
                        + "</manifest>\n"),
                xml("src/debug/AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"foo.bar2\"\n"
                        + "    android:versionCode=\"1\"\n"
                        + "    android:versionName=\"1.0\" >\n"
                        + "\n"
                        + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                        + "    <uses-permission android:name=\"com.example.helloworld.permission\" />\n"
                        + "    <uses-permission android:name=\"android.permission.ACCESS_MOCK_LOCATION\" /> \n"
                        + "\n"
                        + "</manifest>\n"),
                xml("src/test/AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"foo.bar2\"\n"
                        + "    android:versionCode=\"1\"\n"
                        + "    android:versionName=\"1.0\" >\n"
                        + "\n"
                        + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                        + "    <uses-permission android:name=\"com.example.helloworld.permission\" />\n"
                        + "    <uses-permission android:name=\"android.permission.ACCESS_MOCK_LOCATION\" /> \n"
                        + "\n"
                        + "</manifest>\n"),
                gradle(""
                        + "android {\n"
                        + "    compileSdkVersion 25\n"
                        + "    buildToolsVersion \"25.0.0\"\n"
                        + "    defaultConfig {\n"
                        + "        applicationId \"com.android.tools.test\"\n"
                        + "        minSdkVersion 5\n"
                        + "        targetSdkVersion 16\n"
                        + "        versionCode 2\n"
                        + "        versionName \"MyName\"\n"
                        + "    }\n"
                        + "}"))
                .issues(ManifestDetector.MOCK_LOCATION)
                .run()
                .expect(expected);

        // TODO: When we have an instantiatable gradle model, test with real model and verify
        // that a manifest file in a debug build type does not get flagged.
    }

    public void testMockLocationsOk() throws Exception {
        lint().files(
                // Not a Gradle project
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"foo.bar2\"\n"
                        + "    android:versionCode=\"1\"\n"
                        + "    android:versionName=\"1.0\" >\n"
                        + "\n"
                        + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                        + "    <uses-permission android:name=\"com.example.helloworld.permission\" />\n"
                        + "    <uses-permission android:name=\"android.permission.ACCESS_MOCK_LOCATION\" /> \n"
                        + "\n"
                        + "</manifest>\n"))
                .issues(ManifestDetector.MOCK_LOCATION)
                .run()
                .expectClean();
    }

    public void testGradleOverrides() throws Exception {
        String expected = ""
                + "src/main/AndroidManifest.xml:4: Warning: This versionCode value (1) is not used; it is always overridden by the value specified in the Gradle build script (2) [GradleOverrides]\n"
                + "    android:versionCode=\"1\"\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/main/AndroidManifest.xml:5: Warning: This versionName value (1.0) is not used; it is always overridden by the value specified in the Gradle build script (MyName) [GradleOverrides]\n"
                + "    android:versionName=\"1.0\" >\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/main/AndroidManifest.xml:7: Warning: This minSdkVersion value (14) is not used; it is always overridden by the value specified in the Gradle build script (5) [GradleOverrides]\n"
                + "    <uses-sdk android:minSdkVersion=\"14\" android:targetSdkVersion=\"17\" />\n"
                + "              ~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/main/AndroidManifest.xml:7: Warning: This targetSdkVersion value (17) is not used; it is always overridden by the value specified in the Gradle build script (16) [GradleOverrides]\n"
                + "    <uses-sdk android:minSdkVersion=\"14\" android:targetSdkVersion=\"17\" />\n"
                + "                                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 4 warnings\n";
        lint().files(
                mGradle_override,
                gradle(""
                        + "android {\n"
                        + "    compileSdkVersion 25\n"
                        + "    buildToolsVersion \"25.0.0\"\n"
                        + "    defaultConfig {\n"
                        + "        applicationId \"com.android.tools.test\"\n"
                        + "        minSdkVersion 5\n"
                        + "        targetSdkVersion 16\n"
                        + "        versionCode 2\n"
                        + "        versionName \"MyName\"\n"
                        + "    }\n"
                        + "}"))
                .issues(ManifestDetector.GRADLE_OVERRIDES)
                .run()
                .expect(expected);
    }

    public void testGradleOverridesOk() throws Exception {
        lint().files(
                mGradle_override,
                gradle(""
                        + "android {\n"
                        + "}"))
                .issues(ManifestDetector.GRADLE_OVERRIDES)
                .run()
                .expectClean();
    }

    public void testGradleOverrideManifestMergerOverride() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=186762
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "    package=\"test.pkg\">\n"
                        + "\n"
                        + "    <uses-sdk android:minSdkVersion=\"14\" tools:overrideLibrary=\"lib.pkg\" />\n"
                        + "\n"
                        + "</manifest>\n"),
                projectProperties().library(true).compileSdk(14),
                gradle(""
                        + "android {\n"
                        + "    compileSdkVersion 25\n"
                        + "    buildToolsVersion \"25.0.0\"\n"
                        + "    defaultConfig {\n"
                        + "        applicationId \"com.android.tools.test\"\n"
                        + "        minSdkVersion 5\n"
                        + "        targetSdkVersion 16\n"
                        + "        versionCode 2\n"
                        + "        versionName \"MyName\"\n"
                        + "    }\n"
                        + "}"))
                .issues(ManifestDetector.GRADLE_OVERRIDES)
                .run()
                .expectClean();
    }

    public void testManifestPackagePlaceholder() throws Exception {
        String expected = ""
                + "src/main/AndroidManifest.xml:3: Warning: Cannot use placeholder for the package in the manifest; set applicationId in build.gradle instead [GradleOverrides]\n"
                + "    package=\"${packageName}\" >\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"${packageName}\" >\n"
                        + "    <uses-sdk android:minSdkVersion=\"14\" android:targetSdkVersion=\"17\" />\n"
                        + "    <application\n"
                        + "        android:icon=\"@drawable/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\" >\n"
                        + "    </application>\n"
                        + "</manifest>\n"),
                gradle(""
                        + "android {\n"
                        + "}"))
                .issues(ManifestDetector.GRADLE_OVERRIDES)
                .run()
                .expect(expected);
    }

    public void testMipMap() throws Exception {
        mEnabled = Collections.singleton(ManifestDetector.MIPMAP);
        assertEquals("No warnings.",

                lintProject(
                        mMipmap));
    }

    public void testMipMapWithDensityFiltering() throws Exception {
        String expected = ""
                + "src/main/AndroidManifest.xml:9: Warning: Should use @mipmap instead of @drawable for launcher icons [MipmapIcons]\n"
                + "        android:icon=\"@drawable/ic_launcher\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/main/AndroidManifest.xml:14: Warning: Should use @mipmap instead of @drawable for launcher icons [MipmapIcons]\n"
                + "            android:icon=\"@drawable/activity1\"\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n";

        lint().files(
                mMipmap,
                gradle(""
                        + "android {\n"
                        + "    defaultConfig {\n"
                        + "        resConfigs \"cs\"\n"
                        + "    }\n"
                        + "    flavorDimensions  \"pricing\", \"releaseType\"\n"
                        + "    productFlavors {\n"
                        + "        beta {\n"
                        + "            flavorDimension \"releaseType\"\n"
                        + "            resConfig \"en\", \"de\"\n"
                        + "            resConfigs \"nodpi\", \"hdpi\"\n"
                        + "        }\n"
                        + "        normal { flavorDimension \"releaseType\" }\n"
                        + "        free { flavorDimension \"pricing\" }\n"
                        + "        paid { flavorDimension \"pricing\" }\n"
                        + "    }\n"
                        + "}\n"))
                .issues(ManifestDetector.MIPMAP)
                .variant("freeBetaDebug")
                .run()
                .expect(expected);
    }

    public void testFullBackupContentBoolean() throws Exception {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:fullBackupContent=\"true\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .issues(ManifestDetector.ALLOW_BACKUP)
                .incremental()
                .run()
                .expectClean();
    }

    public void testFullBackupContentMissing() throws Exception {
        String expected = ""
                + "AndroidManifest.xml:7: Warning: Missing <full-backup-content> resource [AllowBackup]\n"
                + "        android:fullBackupContent=\"@xml/backup\"\n"
                + "                                   ~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:fullBackupContent=\"@xml/backup\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .issues(ManifestDetector.ALLOW_BACKUP)
                .incremental()
                .run()
                .expect(expected);
    }

    public void testFullBackupContentMissingInLibrary() throws Exception {
        //noinspection all // Sample code
        lint().files(
                projectProperties().library(true).compileSdk(14),
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:fullBackupContent=\"@xml/backup\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .issues(ManifestDetector.ALLOW_BACKUP)
                .incremental("AndroidManifest.xml")
                .run()
                .expectClean();
    }

    public void testFullBackupContentOk() throws Exception {
        //noinspection all // Sample code
        lint().files(
                projectProperties().library(true).compileSdk(14),
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:fullBackupContent=\"@xml/backup\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"),
                xml("res/xml/backup.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<full-backup-content>\n"
                        + "     <include domain=\"file\" path=\"dd\"/>\n"
                        + "     <exclude domain=\"file\" path=\"dd/fo3o.txt\"/>\n"
                        + "     <exclude domain=\"file\" path=\"dd/ss/foo.txt\"/>\n"
                        + "</full-backup-content>"))
                .issues(ManifestDetector.ALLOW_BACKUP)
                .incremental("AndroidManifest.xml")
                .run()
                .expectClean();
    }

    public void testHasBackupSpecifiedInTarget23() throws Exception {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "    <uses-sdk android:targetSdkVersion=\"23\" />"
                        + "\n"
                        + "    <application\n"
                        + "        android:fullBackupContent=\"no\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .issues(ManifestDetector.ALLOW_BACKUP)
                .run()
                .expectClean();

    }

    public void testMissingBackupInTarget23() throws Exception {
        String expected = ""
                + "AndroidManifest.xml:5: Warning: On SDK version 23 and up, your app data will be automatically backed up and restored on app install. Consider adding the attribute android:fullBackupContent to specify an @xml resource which configures which files to backup. More info: https://developer.android.com/training/backup/autosyncapi.html [AllowBackup]\n"
                + "    <application\n"
                + "    ^\n"
                + "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "    <uses-sdk android:targetSdkVersion=\"23\" />"
                        + "\n"
                        + "    <application\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .issues(ManifestDetector.ALLOW_BACKUP)
                .run()
                .expect(expected);

    }

    public void testMissingBackupInPreTarget23() throws Exception {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "    <uses-sdk android:targetSdkVersion=\"21\" />"
                        + "\n"
                        + "    <application\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .issues(ManifestDetector.ALLOW_BACKUP)
                .run()
                .expectClean();

    }

    public void testMissingBackupWithoutGcmPreTarget23() throws Exception {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "    <uses-sdk android:targetSdkVersion=\"21\" />"
                        + "\n"
                        + "    <application\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .issues(ManifestDetector.ALLOW_BACKUP)
                .run()
                .expectClean();

    }

    public void testMissingBackupWithoutGcmPostTarget23() throws Exception {
        String expected = ""
                + "AndroidManifest.xml:5: Warning: On SDK version 23 and up, your app data will be automatically backed up and restored on app install. Consider adding the attribute android:fullBackupContent to specify an @xml resource which configures which files to backup. More info: https://developer.android.com/training/backup/autosyncapi.html [AllowBackup]\n"
                + "    <application\n"
                + "    ^\n"
                + "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "    <uses-sdk android:targetSdkVersion=\"23\" />"
                        + "\n"
                        + "    <application\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .issues(ManifestDetector.ALLOW_BACKUP)
                .run()
                .expect(expected);
    }

    public void testMissingBackupWithGcmPreTarget23() throws Exception {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "    <uses-sdk android:targetSdkVersion=\"21\" />"
                        + "\n"
                        + "    <application\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >"
                        + "        <receiver\n"
                        + "            android:name=\".GcmBroadcastReceiver\"\n"
                        + "            android:permission=\"com.google.android.c2dm.permission.SEND\" >\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"com.google.android.c2dm.intent.RECEIVE\" />\n"
                        + "                <category android:name=\"com.example.gcm\" />\n"
                        + "            </intent-filter>\n"
                        + "        </receiver>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .issues(ManifestDetector.ALLOW_BACKUP)
                .run()
                .expectClean();

    }

    public void testMissingBackupWithGcmPostTarget23() throws Exception {
        String expected = ""
                + "AndroidManifest.xml:5: Warning: On SDK version 23 and up, your app data will be automatically backed up, and restored on app install. Your GCM regid will not work across restores, so you must ensure that it is excluded from the back-up set. Use the attribute android:fullBackupContent to specify an @xml resource which configures which files to backup. More info: https://developer.android.com/training/backup/autosyncapi.html [AllowBackup]\n"
                + "    <application\n"
                + "    ^\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "    <uses-sdk android:targetSdkVersion=\"23\" />"
                        + "\n"
                        + "    <application\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >"
                        + "        <receiver\n"
                        + "            android:name=\".GcmBroadcastReceiver\"\n"
                        + "            android:permission=\"com.google.android.c2dm.permission.SEND\" >\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"com.google.android.c2dm.intent.RECEIVE\" />\n"
                        + "                <category android:name=\"com.example.gcm\" />\n"
                        + "            </intent-filter>\n"
                        + "        </receiver>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .issues(ManifestDetector.ALLOW_BACKUP)
                .run()
                .expect(expected);
    }

    public void testNoMissingFullBackupWithDoNotAllowBackup() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=181805
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "    <uses-sdk android:targetSdkVersion=\"21\" />"
                        + "\n"
                        + "    <application\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:allowBackup=\"false\"\n"
                        + "        android:theme=\"@style/AppTheme\" >"
                        + "        <receiver\n"
                        + "            android:name=\".GcmBroadcastReceiver\"\n"
                        + "            android:permission=\"com.google.android.c2dm.permission.SEND\" >\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"com.google.android.c2dm.intent.RECEIVE\" />\n"
                        + "                <category android:name=\"com.example.gcm\" />\n"
                        + "            </intent-filter>\n"
                        + "        </receiver>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .issues(ManifestDetector.ALLOW_BACKUP)
                .run()
                .expectClean();
    }

    public void testFullBackupContentMissingIgnored() throws Exception {
        // Make sure now that we look at the merged manifest that we correctly handle tools:ignore
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "\n"
                        + "    <application\n"
                        + "        tools:ignore=\"AllowBackup\"\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:fullBackupContent=\"@xml/backup\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .issues(ManifestDetector.ALLOW_BACKUP)
                .incremental()
                .run()
                .expectClean();
    }

    public void testBackupAttributeFromMergedManifest() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=236584
        // Library project specifies backup descriptor, main project does not.

        //noinspection all // Sample code
        ProjectDescription library = project(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg.library\" >\n"
                        + "    <uses-sdk android:targetSdkVersion=\"23\" />"
                        + "\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:fullBackupContent=\"@xml/backup\">\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"),
                xml("res/xml/backup.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<full-backup-content>\n"
                        + "     <include domain=\"file\" path=\"dd\"/>\n"
                        + "     <exclude domain=\"file\" path=\"dd/fo3o.txt\"/>\n"
                        + "     <exclude domain=\"file\" path=\"dd/ss/foo.txt\"/>\n"
                        + "</full-backup-content>")
        ).type(LIBRARY).name("LibraryProject");

        //noinspection all // Sample code
        ProjectDescription main = project(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg.app\" >\n"
                        + "    <uses-sdk android:targetSdkVersion=\"23\" />"
                        + "\n"
                        + "    <application\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n")
        ).dependsOn(library);

        lint().projects(main, library).issues(ManifestDetector.ALLOW_BACKUP).
                run().expectClean();
    }

    public void testWearableBindListener() throws Exception {
        String expected = ""
                + "src/main/AndroidManifest.xml:11: Error: The com.google.android.gms.wearable.BIND_LISTENER action is deprecated. [WearableBindListener]\n"
                + "                  <action android:name=\"com.google.android.gms.wearable.BIND_LISTENER\" />\n"
                + "                          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n";
        lint().files(
                xml("src/main/AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "    <uses-sdk android:targetSdkVersion=\"22\" />"
                        + "\n"
                        + "    <application\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:allowBackup=\"false\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "        <service android:name=\".WearMessageListenerService\">\n"
                        + "              <intent-filter>\n"
                        + "                  <action android:name=\"com.google.android.gms.wearable.BIND_LISTENER\" />\n"
                        + "              </intent-filter>\n"
                        + "        </service>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"),
                gradle(""
                        + "apply plugin: 'com.android.application'\n"
                        + "\n"
                        + "dependencies {\n"
                        + "    compile 'com.google.android.gms:play-services-wearable:8.4.0'\n"
                        + "}"))
                .issues(ManifestDetector.WEARABLE_BIND_LISTENER)
                .run()
                .expect(expected);
    }

    // No warnings here because the variant points to a gms dependency version 8.1.0
    public void testWearableBindListenerNoWarn() throws Exception {
        lint().files(
                xml("src/main/AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "    <uses-sdk android:targetSdkVersion=\"22\" />"
                        + "\n"
                        + "    <application\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:allowBackup=\"false\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "        <service android:name=\".WearMessageListenerService\">\n"
                        + "              <intent-filter>\n"
                        + "                  <action android:name=\"com.google.android.gms.wearable.BIND_LISTENER\" />\n"
                        + "              </intent-filter>\n"
                        + "        </service>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"),
                gradle(""
                        + "apply plugin: 'com.android.application'\n"
                        + "\n"
                        + "dependencies {\n"
                        + "    compile 'com.google.android.gms:play-services-wearable:8.1.+'\n"
                        + "}"))
                .issues(ManifestDetector.WEARABLE_BIND_LISTENER)
                .run()
                .expectClean();
    }

    public void testWearableBindListenerCompileSdk24() throws Exception {
        String expected = ""
                + "src/main/AndroidManifest.xml:11: Error: The com.google.android.gms.wearable.BIND_LISTENER action is deprecated. Please upgrade to the latest available version of play-services-wearable: 8.4.0 [WearableBindListener]\n"
                + "                  <action android:name=\"com.google.android.gms.wearable.BIND_LISTENER\" />\n"
                + "                          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n";
        lint().files(
                projectProperties().compileSdk(24),
                xml("src/main/AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "    <uses-sdk android:targetSdkVersion=\"22\" />"
                        + "\n"
                        + "    <application\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:allowBackup=\"false\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "        <service android:name=\".WearMessageListenerService\">\n"
                        + "              <intent-filter>\n"
                        + "                  <action android:name=\"com.google.android.gms.wearable.BIND_LISTENER\" />\n"
                        + "              </intent-filter>\n"
                        + "        </service>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"),
                gradle(""
                        + "apply plugin: 'com.android.application'\n"
                        + "\n"
                        + "dependencies {\n"
                        + "    compile 'com.google.android.gms:play-services-wearable:8.1.+'\n"
                        + "}"))
                .issues(ManifestDetector.WEARABLE_BIND_LISTENER)
                // This test uses a mock SDK home to ensure that the latest expected
                // version is 8.4.0 rather than whatever happens to actually be the
                // latest version at the time (such as 9.6.1 at the moment of this writing)
                .sdkHome(getMockSupportLibraryInstallation())
                .run()
                .expect(expected);
    }

    private File getMockSupportLibraryInstallation() {
        if (mSdkDir == null) {
            // Make fake SDK "installation" such that we can predict the set
            // of Maven repositories discovered by this test
            mSdkDir = TestUtils.createTempDirDeletedOnExit();

            String[] paths = new String[]{
                    "extras/google/m2repository/com/google/android/gms/play-services-wearable/8.4.0/play-services-wearable-8.4.0.aar",
            };

            createRelativePaths(mSdkDir, paths);
        }

        return mSdkDir;
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mGradle_override = xml("AndroidManifest.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    package=\"foo.bar2\"\n"
            + "    android:versionCode=\"1\"\n"
            + "    android:versionName=\"1.0\" >\n"
            + "\n"
            + "    <uses-sdk android:minSdkVersion=\"14\" android:targetSdkVersion=\"17\" />\n"
            + "    <uses-permission android:name=\"com.example.helloworld.permission\" />\n"
            + "    <uses-permission android:name=\"android.permission.ACCESS_MOCK_LOCATION\" /> \n"
            + "\n"
            + "    <application\n"
            + "        android:icon=\"@drawable/ic_launcher\"\n"
            + "        android:label=\"@string/app_name\" >\n"
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
            + "</manifest>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mLibrary = source("build.gradle", "");

    @SuppressWarnings("all") // Sample code
    private TestFile mMipmap = xml("AndroidManifest.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    package=\"test.mipmap\"\n"
            + "    android:versionCode=\"1\"\n"
            + "    android:versionName=\"1.0\" >\n"
            + "\n"
            + "    <!-- Wrong icon resource type -->\n"
            + "    <application\n"
            + "        android:icon=\"@drawable/ic_launcher\"\n"
            + "        android:label=\"@string/app_name\" >\n"
            + "        <!-- Wrong icon resource type -->\n"
            + "        <activity\n"
            + "            android:name=\".Activity1\"\n"
            + "            android:icon=\"@drawable/activity1\"\n"
            + "            android:label=\"@string/activity1\" >\n"
            + "            <intent-filter>\n"
            + "                <action android:name=\"android.intent.action.MAIN\" />\n"
            + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
            + "            </intent-filter>\n"
            + "        </activity>\n"
            + "        <!-- Already a @mipmap resource -->\n"
            + "        <activity\n"
            + "            android:name=\".Activity2\"\n"
            + "            android:icon=\"@mipmap/activity2\"\n"
            + "            android:label=\"@string/activity2\" >\n"
            + "        </activity>\n"
            + "        <!-- Not a launchable activity -->\n"
            + "        <activity\n"
            + "            android:name=\".Activity3\"\n"
            + "            android:icon=\"@drawable/activity3\"\n"
            + "            android:label=\"@string/activity3\" >\n"
            + "        </activity>\n"
            + "    </application>\n"
            + "\n"
            + "</manifest>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mMissing_application_icon = xml("AndroidManifest.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    package=\"foo.bar2\"\n"
            + "    android:versionCode=\"1\"\n"
            + "    android:versionName=\"1.0\" >\n"
            + "\n"
            + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
            + "\n"
            + "    <application\n"
            + "        android:label=\"@string/app_name\" >\n"
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
            + "</manifest>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mMissingusessdk = xml("AndroidManifest.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    package=\"test.bytecode\"\n"
            + "    android:versionCode=\"1\"\n"
            + "    android:versionName=\"1.0\" >\n"
            + "\n"
            + "    <application\n"
            + "        android:icon=\"@drawable/ic_launcher\"\n"
            + "        android:label=\"@string/app_name\" >\n"
            + "        <activity\n"
            + "            android:name=\".BytecodeTestsActivity\"\n"
            + "            android:label=\"@string/app_name\" >\n"
            + "            <intent-filter>\n"
            + "                <action android:name=\"android.intent.action.MAIN\" />\n"
            + "\n"
            + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
            + "            </intent-filter>\n"
            + "        </activity>\n"
            + "    </application>\n"
            + "\n"
            + "</manifest>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mNo_version = xml("AndroidManifest.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    package=\"foo.bar2\" >\n"
            + "\n"
            + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
            + "\n"
            + "    <application\n"
            + "        android:icon=\"@drawable/ic_launcher\"\n"
            + "        android:label=\"@string/app_name\" >\n"
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
            + "</manifest>\n");

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

    @SuppressWarnings("all") // Sample code
    private TestFile mLibraryCode = java(""
            + "package foo.library;\n"
            + "\n"
            + "public class LibraryCode {\n"
            + "    static {\n"
            + "        System.out.println(R.string.string1);\n"
            + "    }\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mMainCode = java(""
            + "package foo.main;\n"
            + "\n"
            + "public class MainCode {\n"
            + "    static {\n"
            + "        System.out.println(R.string.string2);\n"
            + "    }\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mLibraryStrings = xml("res/values/strings.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "\n"
            + "    <string name=\"app_name\">LibraryProject</string>\n"
            + "    <string name=\"string1\">String 1</string>\n"
            + "    <string name=\"string2\">String 2</string>\n"
            + "    <string name=\"string3\">String 3</string>\n"
            + "\n"
            + "</resources>\n");
}
