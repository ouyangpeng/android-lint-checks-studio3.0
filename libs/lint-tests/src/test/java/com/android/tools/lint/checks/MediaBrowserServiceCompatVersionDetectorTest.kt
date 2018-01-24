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

package com.android.tools.lint.checks

import com.android.tools.lint.detector.api.Detector

class MediaBrowserServiceCompatVersionDetectorTest : AbstractCheckTest() {

    private val mediaBrowserCompat: TestFile =
            java(""
                    + "package android.support.v4.media;\n"
                    + "\n"
                    + "public class MediaBrowserCompat {\n"
                    + "    public static class MediaItem {}\n"
                    + "}\n")

    private val mediaBrowserServiceCompat: TestFile =
            java(""
                    + "package android.support.v4.media;\n"
                    + "\n"
                    + "import android.os.Bundle;\n"
                    + "import java.util.List;\n"
                    + "\n"
                    + "public abstract class MediaBrowserServiceCompat {\n"
                    +
                    "    public abstract BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints);\n"
                    + "\n"
                    +
                    "    public abstract void onLoadChildren(String parentId, Result<List<MediaBrowserCompat.MediaItem>> result);\n"
                    + "    public static class BrowserRoot {}\n"
                    + "    public static class Result<T> {}\n"
                    + "}\n")

    private val browserService: TestFile =
            java(""
                    + "package test.pkg;\n"
                    + "\n"
                    + "import android.os.Bundle;\n"
                    + "import android.support.v4.media.MediaBrowserCompat;\n"
                    + "import android.support.v4.media.MediaBrowserServiceCompat;\n"
                    + "\n"
                    + "import java.util.List;\n"
                    + "\n"
                    + "public class MyBrowserService extends MediaBrowserServiceCompat {\n"
                    + "\n"
                    + "    @Override\n"
                    + "    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {\n"
                    + "        return null;\n"
                    + "    }\n"
                    + "\n"
                    + "    @Override\n"
                    + "    public void onLoadChildren(String parentId, Result<List<MediaBrowserCompat.MediaItem>> result) {\n"
                    + "\n"
                    + "    }\n"
                    + "}\n")

    override fun getDetector(): Detector {
        return MediaBrowserServiceCompatVersionDetector()
    }

    fun testMediaBrowserServiceCompatOldVersion() {
        lint().files(
                mediaBrowserCompat,
                mediaBrowserServiceCompat,
                browserService,
                gradle(""
                        + "apply plugin: 'com.android.application'\n"
                        + "\n"
                        + "dependencies {\n"
                        + "    compile 'com.android.support:support-v4:23.4.0' \n"
                        + "}"),
                manifest().minSdk(22))
                .allowCompilationErrors()
                .run()
                .expect("" +
                        "build.gradle:4: Warning: Using a version of the class that is not forward compatible [IncompatibleMediaBrowserServiceCompatVersion]\n" +
                        "    compile 'com.android.support:support-v4:23.4.0' \n" +
                        "             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                        "0 errors, 1 warnings\n")
    }

    fun testMediaBrowserServiceCompat24Plus() {
        lint().files(
                mediaBrowserCompat,
                mediaBrowserServiceCompat,
                browserService,
                gradle(""
                        + "apply plugin: 'com.android.application'\n"
                        + "\n"
                        + "dependencies {\n"
                        + "    compile 'com.android.support:support-v4:24.0.0' \n"
                        + "}"),
                manifest().minSdk(22))
                .allowCompilationErrors()
                .run()
                .expectClean()
    }
}
