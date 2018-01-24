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

import static com.android.SdkConstants.FN_ANDROID_MANIFEST_XML;

import com.android.tools.lint.detector.api.Detector;

public class AndroidAutoDetectorTest extends AbstractCheckTest {

    //noinspection all // Sample code
    private final TestFile mValidAutomotiveDescriptor =
            xml("res/xml/automotive_app_desc.xml", ""
                    + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    + "<automotiveApp>\n"
                    + "    <uses name=\"media\"/>\n"
                    + "</automotiveApp>\n");

    //noinspection all // Sample code
    private final TestFile mValidAutoAndroidXml = xml(FN_ANDROID_MANIFEST_XML, ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "          xmlns:tools=\"http://schemas.android.com/tools\"\n"
            + "          package=\"com.example.android.uamp\">\n"
            + "\n"
            + "    <application\n"
            + "        android:name=\".UAMPApplication\"\n"
            + "        android:label=\"@string/app_name\"\n"
            + "        android:theme=\"@style/UAmpAppTheme\">\n"
            + "\n"
            + "    <meta-data\n"
            + "        android:name=\"com.google.android.gms.car.application\"\n"
            + "        android:resource=\"@xml/automotive_app_desc\"/>\n"
            + "\n"
            + "        <service\n"
            + "            android:name=\".MusicService\"\n"
            + "            android:exported=\"true\"\n"
            + "            tools:ignore=\"ExportedService\">\n"
            + "            <intent-filter>\n"
            + "                <action android:name=\"android.media.browse.MediaBrowserService\"/>\n"
            + "            </intent-filter>\n"
            + "            <intent-filter>\n"
            + "                <action android:name=\"android.media.action.MEDIA_PLAY_FROM_SEARCH\"/>\n"
            + "                <category android:name=\"android.intent.category.DEFAULT\"/>\n"
            + "            </intent-filter>\n"
            + "        </service>\n"
            + "\n"
            + "    </application>\n"
            + "\n"
            + "</manifest>\n");

    @Override
    protected Detector getDetector() {
        return new AndroidAutoDetector();
    }

    public void testMissingIntentFilter() throws Exception {
        String expected = "AndroidManifest.xml:6: Error: Missing intent-filter for action android.media.browse.MediaBrowserService that is required for android auto support [MissingMediaBrowserServiceIntentFilter]\n"
                + "    <application\n"
                + "    ^\n"
                + "1 errors, 0 warnings\n";

        //noinspection all // Sample code
        lint().files(
                xml(FN_ANDROID_MANIFEST_XML, ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "          package=\"com.example.android.uamp\">\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:name=\".UAMPApplication\"\n"
                        + "        android:icon=\"@drawable/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/UAmpAppTheme\"\n"
                        + "        android:banner=\"@drawable/banner_tv\">\n"
                        + "\n"
                        + "        <meta-data\n"
                        + "            android:name=\"com.google.android.gms.car.application\"\n"
                        + "            android:resource=\"@xml/automotive_app_desc\"/>\n"
                        + "        <service\n"
                        + "            android:name=\".MusicService\"\n"
                        + "            android:exported=\"true\"\n"
                        + "            tools:ignore=\"ExportedService\">\n"
                        + "        </service>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"),
                mValidAutomotiveDescriptor)
                .issues(AndroidAutoDetector.MISSING_MEDIA_BROWSER_SERVICE_ACTION_ISSUE)
                .run()
                .expect(expected);
    }

    public void testInvalidUsesTagInMetadataFile() throws Exception {
        String expected = "" +
                "res/xml/automotive_app_desc.xml:3: Error: Expecting one of media or notification for the name attribute in uses tag. [InvalidUsesTagAttribute]\n"
                + "    <uses name=\"medias\"/>\n"
                + "          ~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n";

        //noinspection all // Sample code
        lint().files(
                mValidAutoAndroidXml,
                xml("res/xml/automotive_app_desc.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<automotiveApp>\n"
                        + "    <uses name=\"medias\"/>\n"
                        + "</automotiveApp>\n"))
                .issues(AndroidAutoDetector.INVALID_USES_TAG_ISSUE)
                .run()
                .expect(expected);
    }

    public void testMissingMediaSearchIntent() throws Exception {
        String expected = "AndroidManifest.xml:6: Error: Missing intent-filter for action android.media.action.MEDIA_PLAY_FROM_SEARCH. [MissingIntentFilterForMediaSearch]\n"
                + "    <application\n"
                + "    ^\n"
                + "1 errors, 0 warnings\n";

        //noinspection all // Sample code
        lint().files(
                xml(FN_ANDROID_MANIFEST_XML, ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "          package=\"com.example.android.uamp\">\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:name=\".UAMPApplication\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/UAmpAppTheme\">\n"
                        + "\n"
                        + "    <meta-data\n"
                        + "        android:name=\"com.google.android.gms.car.application\"\n"
                        + "        android:resource=\"@xml/automotive_app_desc\"/>\n"
                        + "\n"
                        + "        <service\n"
                        + "            android:name=\".MusicService\"\n"
                        + "            android:exported=\"true\"\n"
                        + "            tools:ignore=\"ExportedService\">\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.media.browse"
                        + ".MediaBrowserService\"/>\n"
                        + "            </intent-filter>\n"
                        + "        </service>\n"
                        + "\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"),
                mValidAutomotiveDescriptor)
                .issues(AndroidAutoDetector.MISSING_INTENT_FILTER_FOR_MEDIA_SEARCH)
                .run()
                .expect(expected);
    }

    public void testMissingOnPlayFromSearch() throws Exception {
        String expected = "src/com/example/android/uamp/MSessionCallback.java:5: Error: This class does not override onPlayFromSearch from MediaSession.Callback The method should be overridden and implemented to support Voice search on Android Auto. [MissingOnPlayFromSearch]\n"
                + "public class MSessionCallback extends Callback {\n"
                + "             ~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n";

        //noinspection all // Sample code
        lint().files(
                mValidAutoAndroidXml,
                mValidAutomotiveDescriptor,
                java("src/com/example/android/uamp/MSessionCallback.java", ""
                        + "package com.example.android.uamp;\n"
                        + "\n"
                        + "import android.media.session.MediaSession.Callback;\n"
                        + "\n"
                        + "public class MSessionCallback extends Callback {\n"
                        + "    @Override\n"
                        + "    public void onPlay() {\n"
                        + "        // custom impl\n"
                        + "    }\n"
                        + "}\n"))
                .issues(AndroidAutoDetector.MISSING_ON_PLAY_FROM_SEARCH)
                .run()
                .expect(expected);
    }

    public void testValidOnPlayFromSearch() throws Exception {
        //noinspection all // Sample code
        lint().files(
                mValidAutoAndroidXml,
                mValidAutomotiveDescriptor,
                java("src/com/example/android/uamp/MSessionCallback.java", ""
                        + "package com.example.android.uamp;\n"
                        + "\n"
                        + "import android.os.Bundle;\n"
                        + "\n"
                        + "import android.media.session.MediaSession.Callback;\n"
                        + "\n"
                        + "public class MSessionCallback extends Callback {\n"
                        + "    @Override\n"
                        + "    public void onPlayFromSearch(String query, Bundle bundle) {\n"
                        + "        // custom impl\n"
                        + "    }\n"
                        + "}\n"))
                .issues(AndroidAutoDetector.MISSING_ON_PLAY_FROM_SEARCH)
                .run()
                .expectClean();
    }
}
