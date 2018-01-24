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

import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings("javadoc")
public class AppIndexingApiDetectorTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new AppIndexingApiDetector();
    }

    public void testOk() throws Exception {
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:icon=\"@mipmap/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "        <activity\n"
                        + "            android:name=\".FullscreenActivity\"\n"
                        + "            android:configChanges=\"orientation|keyboardHidden|screenSize\"\n"
                        + "            android:label=\"@string/title_activity_fullscreen\"\n"
                        + "            android:theme=\"@style/FullscreenTheme\" >\n"
                        + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                        + "                <action android:name=\"android.intent.action.VIEW\" />\n"
                        + "                <data android:scheme=\"http\"\n"
                        + "                    android:host=\"example.com\"\n"
                        + "                    android:pathPrefix=\"/gizmos\" />\n"
                        + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
                        + "                <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "        <meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\" />\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expectClean();
    }

    public void testNoActivity() throws Exception {
        String expected = ""
                + "AndroidManifest.xml:5: Warning: App is not indexable by Google Search; consider adding at least one Activity with an ACTION-VIEW intent filter. See issue explanation for more details. [GoogleAppIndexingWarning]\n"
                + "    <application\n"
                + "    ^\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:icon=\"@mipmap/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "        <meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\" />\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testNoWarningInLibraries() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=194937
        // 194937: App indexing lint check shouldn't apply to library projects
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:icon=\"@mipmap/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "        <meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\" />\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"),
                // Mark project as library
                source("project.properties", "android.library=true\n"))
                .run()
                .expectClean();
    }

    public void testNoActionView() throws Exception {
        String expected = ""
                + "AndroidManifest.xml:5: Warning: App is not indexable by Google Search; consider adding at least one Activity with an ACTION-VIEW intent filter. See issue explanation for more details. [GoogleAppIndexingWarning]\n"
                + "    <application\n"
                + "    ^\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:icon=\"@mipmap/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "        <activity\n"
                        + "            android:name=\".MainActivity\"\n"
                        + "            android:label=\"@string/app_name\" >\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                        + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "        <meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\" />\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testJavaOk() throws Exception {
        lint().files(
                mAppIndexingApiTestOk,
                mApp_indexing_api_test,
                mAppIndex,
                mAppIndexApi,
                mGoogleApiClient,
                mActivity,
                mApi)
                .run()
                .expectClean();
    }

    public void testNoManifest() throws Exception {
        String expected = ""
                + "src/com/example/helloworld/AppIndexingApiTest.java:28: Warning: Missing support for Firebase App Indexing in the manifest [GoogleAppIndexingApiWarning]\n"
                + "    AppIndex.AppIndexApi.start(mClient, action);\n"
                + "                         ~~~~~\n"
                + "src/com/example/helloworld/AppIndexingApiTest.java:36: Warning: Missing support for Firebase App Indexing in the manifest [GoogleAppIndexingApiWarning]\n"
                + "    AppIndex.AppIndexApi.end(mClient, action);\n"
                + "                         ~~~\n"
                + "0 errors, 2 warnings\n";
        lint().files(
                mAppIndexingApiTestOk,
                mAppIndex,
                mAppIndexApi,
                mGoogleApiClient,
                mActivity,
                mApi)
                .run()
                .expect(expected);
    }

    public void testNoStartEnd() throws Exception {
        String expected = ""
                + "src/com/example/helloworld/AppIndexingApiTest.java:11: Warning: Missing support for Firebase App Indexing API [GoogleAppIndexingApiWarning]\n"
                + "public class AppIndexingApiTest extends Activity {\n"
                + "             ~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package com.example.helloworld;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.net.Uri;\n"
                        + "import android.os.Bundle;\n"
                        + "\n"
                        + "import com.google.android.gms.appindexing.Action;\n"
                        + "import com.google.android.gms.appindexing.AppIndex;\n"
                        + "import com.google.android.gms.common.api.GoogleApiClient;\n"
                        + "\n"
                        + "public class AppIndexingApiTest extends Activity {\n"
                        + "  static final Uri APP_URI = Uri.parse(\"android-app://com.example.helloworld/http/example.com/gizmos\");\n"
                        + "  static final Uri WEB_URL = Uri.parse(\"http://example.com/gizmos\");\n"
                        + "  private GoogleApiClient mClient;\n"
                        + "\n"
                        + "  @Override\n"
                        + "  protected void onCreate(Bundle savedInstanceState) {\n"
                        + "    super.onCreate(savedInstanceState);\n"
                        + "    mClient = new GoogleApiClient.Builder(this).addApi(AppIndex.APP_INDEX_API).build();\n"
                        + "  }\n"
                        + "\n"
                        + "  @Override\n"
                        + "  public void onStart(){\n"
                        + "    super.onStart();\n"
                        + "    mClient.connect();\n"
                        + "  }\n"
                        + "\n"
                        + "  @Override\n"
                        + "  public void onStop(){\n"
                        + "    super.onStop();\n"
                        + "    mClient.disconnect();\n"
                        + "  }\n"
                        + "}\n"
                        + "\n"),
                mApp_indexing_api_test,
                mAppIndex,
                mAppIndexApi,
                mGoogleApiClient,
                mActivity,
                mApi)
                .run()
                .expect(expected);
    }

    public void testStartMatch() throws Exception {
        String expected = ""
                + "src/com/example/helloworld/AppIndexingApiTest.java:27: Warning: GoogleApiClient mClient is not connected [GoogleAppIndexingApiWarning]\n"
                + "    AppIndex.AppIndexApi.start(mClient, action);\n"
                + "                               ~~~~~~~\n"
                + "src/com/example/helloworld/AppIndexingApiTest.java:27: Warning: Missing corresponding AppIndex.AppIndexApi.end method [GoogleAppIndexingApiWarning]\n"
                + "    AppIndex.AppIndexApi.start(mClient, action);\n"
                + "                         ~~~~~\n"
                + "0 errors, 2 warnings\n";
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package com.example.helloworld;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.net.Uri;\n"
                        + "import android.os.Bundle;\n"
                        + "\n"
                        + "import com.google.android.gms.appindexing.Action;\n"
                        + "import com.google.android.gms.appindexing.AppIndex;\n"
                        + "import com.google.android.gms.common.api.GoogleApiClient;\n"
                        + "\n"
                        + "public class AppIndexingApiTest extends Activity {\n"
                        + "  static final Uri APP_URI = Uri.parse(\"android-app://com.example.helloworld/http/example.com/gizmos\");\n"
                        + "  static final Uri WEB_URL = Uri.parse(\"http://example.com/gizmos\");\n"
                        + "  private GoogleApiClient mClient;\n"
                        + "\n"
                        + "  @Override\n"
                        + "  protected void onCreate(Bundle savedInstanceState) {\n"
                        + "    super.onCreate(savedInstanceState);\n"
                        + "    mClient = new GoogleApiClient.Builder(this).addApi(AppIndex.APP_INDEX_API).build();\n"
                        + "  }\n"
                        + "\n"
                        + "  @Override\n"
                        + "  public void onStart(){\n"
                        + "    super.onStart();\n"
                        + "    final String title = \"App Indexing API Title\";\n"
                        + "    Action action = Action.newAction(Action.TYPE_VIEW, title, WEB_URL, APP_URI);\n"
                        + "    AppIndex.AppIndexApi.start(mClient, action);\n"
                        + "  }\n"
                        + "\n"
                        + "  @Override\n"
                        + "  public void onStop(){\n"
                        + "    super.onStop();\n"
                        + "    mClient.disconnect();\n"
                        + "  }\n"
                        + "}\n"
                        + "\n"),
                mApp_indexing_api_test,
                mAppIndex,
                mAppIndexApi,
                mGoogleApiClient,
                mActivity,
                mApi)
                .run()
                .expect(expected);
    }

    public void testEndMatch() throws Exception {
        //noinspection all // Sample code
        String expected = ""
                + "src/com/example/helloworld/AppIndexingApiTest.java:33: Warning: GoogleApiClient mClient is not disconnected [GoogleAppIndexingApiWarning]\n"
                + "    AppIndex.AppIndexApi.end(mClient, action);\n"
                + "                             ~~~~~~~\n"
                + "src/com/example/helloworld/AppIndexingApiTest.java:33: Warning: Missing corresponding AppIndex.AppIndexApi.start method [GoogleAppIndexingApiWarning]\n"
                + "    AppIndex.AppIndexApi.end(mClient, action);\n"
                + "                         ~~~\n"
                + "0 errors, 2 warnings\n";
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package com.example.helloworld;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.net.Uri;\n"
                        + "import android.os.Bundle;\n"
                        + "\n"
                        + "import com.google.android.gms.appindexing.Action;\n"
                        + "import com.google.android.gms.appindexing.AppIndex;\n"
                        + "import com.google.android.gms.common.api.GoogleApiClient;\n"
                        + "\n"
                        + "public class AppIndexingApiTest extends Activity {\n"
                        + "  static final Uri APP_URI = Uri.parse(\"android-app://com.example.helloworld/http/example.com/gizmos\");\n"
                        + "  static final Uri WEB_URL = Uri.parse(\"http://example.com/gizmos\");\n"
                        + "  private GoogleApiClient mClient;\n"
                        + "\n"
                        + "  @Override\n"
                        + "  protected void onCreate(Bundle savedInstanceState) {\n"
                        + "    super.onCreate(savedInstanceState);\n"
                        + "    mClient = new GoogleApiClient.Builder(this).addApi(AppIndex.APP_INDEX_API).build();\n"
                        + "  }\n"
                        + "\n"
                        + "  @Override\n"
                        + "  public void onStart(){\n"
                        + "    super.onStart();\n"
                        + "    mClient.connect();\n"
                        + "  }\n"
                        + "\n"
                        + "  @Override\n"
                        + "  public void onStop(){\n"
                        + "    super.onStop();\n"
                        + "    final String title = \"App Indexing API Title\";\n"
                        + "    Action action = Action.newAction(Action.TYPE_VIEW, title, WEB_URL, APP_URI);\n"
                        + "    AppIndex.AppIndexApi.end(mClient, action);\n"
                        + "  }\n"
                        + "}\n"),
                mApp_indexing_api_test,
                mAppIndex,
                mAppIndexApi,
                mGoogleApiClient,
                mActivity,
                mApi)
                .run()
                .expect(expected);
    }

    public void testViewMatch() throws Exception {
        //noinspection all // Sample code
        String expected = ""
                + "src/com/example/helloworld/AppIndexingApiTest.java:26: Warning: GoogleApiClient mClient is not connected [GoogleAppIndexingApiWarning]\n"
                + "    AppIndex.AppIndexApi.view(mClient, this, APP_URI, title, WEB_URL, null);\n"
                + "                              ~~~~~~~\n"
                + "src/com/example/helloworld/AppIndexingApiTest.java:26: Warning: Missing corresponding AppIndex.AppIndexApi.end method [GoogleAppIndexingApiWarning]\n"
                + "    AppIndex.AppIndexApi.view(mClient, this, APP_URI, title, WEB_URL, null);\n"
                + "                         ~~~~\n"
                + "0 errors, 2 warnings\n";
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package com.example.helloworld;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.net.Uri;\n"
                        + "import android.os.Bundle;\n"
                        + "\n"
                        + "import com.google.android.gms.appindexing.Action;\n"
                        + "import com.google.android.gms.appindexing.AppIndex;\n"
                        + "import com.google.android.gms.common.api.GoogleApiClient;\n"
                        + "\n"
                        + "public class AppIndexingApiTest extends Activity {\n"
                        + "  static final Uri APP_URI = Uri.parse(\"android-app://com.example.helloworld/http/example.com/gizmos\");\n"
                        + "  static final Uri WEB_URL = Uri.parse(\"http://example.com/gizmos\");\n"
                        + "  private GoogleApiClient mClient;\n"
                        + "\n"
                        + "  @Override\n"
                        + "  protected void onCreate(Bundle savedInstanceState) {\n"
                        + "    super.onCreate(savedInstanceState);\n"
                        + "    mClient = new GoogleApiClient.Builder(this).addApi(AppIndex.APP_INDEX_API).build();\n"
                        + "  }\n"
                        + "\n"
                        + "  @Override\n"
                        + "  public void onStart(){\n"
                        + "    super.onStart();\n"
                        + "    final String title = \"App Indexing API Title\";\n"
                        + "    AppIndex.AppIndexApi.view(mClient, this, APP_URI, title, WEB_URL, null);\n"
                        + "  }\n"
                        + "\n"
                        + "  @Override\n"
                        + "  public void onStop(){\n"
                        + "    super.onStop();\n"
                        + "  }\n"
                        + "}\n"),
                mApp_indexing_api_test,
                mAppIndex,
                mAppIndexApi,
                mGoogleApiClient,
                mActivity,
                mApi)
                .run()
                .expect(expected);
    }

    public void testViewEndMatch() throws Exception {
        String expected = ""
                + "src/com/example/helloworld/AppIndexingApiTest.java:29: Warning: GoogleApiClient mClient is not disconnected [GoogleAppIndexingApiWarning]\n"
                + "    AppIndex.AppIndexApi.viewEnd(mClient, this, APP_URI);\n"
                + "                                 ~~~~~~~\n"
                + "src/com/example/helloworld/AppIndexingApiTest.java:29: Warning: Missing corresponding AppIndex.AppIndexApi.start method [GoogleAppIndexingApiWarning]\n"
                + "    AppIndex.AppIndexApi.viewEnd(mClient, this, APP_URI);\n"
                + "                         ~~~~~~~\n"
                + "0 errors, 2 warnings\n";
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package com.example.helloworld;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.net.Uri;\n"
                        + "import android.os.Bundle;\n"
                        + "\n"
                        + "import com.google.android.gms.appindexing.Action;\n"
                        + "import com.google.android.gms.appindexing.AppIndex;\n"
                        + "import com.google.android.gms.common.api.GoogleApiClient;\n"
                        + "\n"
                        + "public class AppIndexingApiTest extends Activity {\n"
                        + "  static final Uri APP_URI = Uri.parse(\"android-app://com.example.helloworld/http/example.com/gizmos\");\n"
                        + "  private GoogleApiClient mClient;\n"
                        + "\n"
                        + "  @Override\n"
                        + "  protected void onCreate(Bundle savedInstanceState) {\n"
                        + "    super.onCreate(savedInstanceState);\n"
                        + "    mClient = new GoogleApiClient.Builder(this).addApi(AppIndex.APP_INDEX_API).build();\n"
                        + "  }\n"
                        + "\n"
                        + "  @Override\n"
                        + "  public void onStart(){\n"
                        + "    super.onStart();\n"
                        + "  }\n"
                        + "\n"
                        + "  @Override\n"
                        + "  public void onStop(){\n"
                        + "    super.onStop();\n"
                        + "    AppIndex.AppIndexApi.viewEnd(mClient, this, APP_URI);\n"
                        + "  }\n"
                        + "}\n"),
                mApp_indexing_api_test,
                mAppIndex,
                mAppIndexApi,
                mGoogleApiClient,
                mActivity,
                mApi)
                .run()
                .expect(expected);
    }

    public void testWrongOrder() throws Exception {
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package com.example.helloworld;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.net.Uri;\n"
                        + "import android.os.Bundle;\n"
                        + "\n"
                        + "import com.google.android.gms.appindexing.Action;\n"
                        + "import com.google.android.gms.appindexing.AppIndex;\n"
                        + "import com.google.android.gms.common.api.GoogleApiClient;\n"
                        + "\n"
                        + "public class AppIndexingApiTest extends Activity {\n"
                        + "  static final Uri APP_URI = Uri.parse(\"android-app://com.example.helloworld/http/example.com/gizmos\");\n"
                        + "  static final Uri WEB_URL = Uri.parse(\"http://example.com/gizmos\");\n"
                        + "  private GoogleApiClient mClient;\n"
                        + "\n"
                        + "  @Override\n"
                        + "  protected void onCreate(Bundle savedInstanceState) {\n"
                        + "    super.onCreate(savedInstanceState);\n"
                        + "    mClient = new GoogleApiClient.Builder(this).addApi(AppIndex.APP_INDEX_API).build();\n"
                        + "  }\n"
                        + "\n"
                        + "  @Override\n"
                        + "  public void onStart(){\n"
                        + "    super.onStart();\n"
                        + "    final String title = \"App Indexing API Title\";\n"
                        + "    Action action = Action.newAction(Action.TYPE_VIEW, title, WEB_URL, APP_URI);\n"
                        + "    AppIndex.AppIndexApi.end(mClient, action);\n"
                        + "    mClient.disconnect();\n"
                        + "  }\n"
                        + "\n"
                        + "  @Override\n"
                        + "  public void onStop(){\n"
                        + "    super.onStop();\n"
                        + "    mClient.connect();\n"
                        + "    final String title = \"App Indexing API Title\";\n"
                        + "    Action action = Action.newAction(Action.TYPE_VIEW, title, WEB_URL, APP_URI);\n"
                        + "    AppIndex.AppIndexApi.start(mClient, action);\n"
                        + "  }\n"
                        + "}\n"
                        + "\n"),
                mApp_indexing_api_test,
                mAppIndex,
                mAppIndexApi,
                mGoogleApiClient,
                mActivity,
                mApi)
                .run()
                .expectClean();
    }

    public void testGoogleApiClientAddApi() throws Exception {
        String expected = ""
                + "src/com/example/helloworld/AppIndexingApiTest.java:28: Warning: GoogleApiClient mClient has not added support for App Indexing API [GoogleAppIndexingApiWarning]\n"
                + "    AppIndex.AppIndexApi.start(mClient, action);\n"
                + "                               ~~~~~~~\n"
                + "src/com/example/helloworld/AppIndexingApiTest.java:36: Warning: GoogleApiClient mClient has not added support for App Indexing API [GoogleAppIndexingApiWarning]\n"
                + "    AppIndex.AppIndexApi.end(mClient, action);\n"
                + "                             ~~~~~~~\n"
                + "0 errors, 2 warnings\n";
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package com.example.helloworld;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.net.Uri;\n"
                        + "import android.os.Bundle;\n"
                        + "\n"
                        + "import com.google.android.gms.appindexing.Action;\n"
                        + "import com.google.android.gms.appindexing.AppIndex;\n"
                        + "import com.google.android.gms.common.api.GoogleApiClient;\n"
                        + "\n"
                        + "public class AppIndexingApiTest extends Activity {\n"
                        + "  static final Uri APP_URI = Uri.parse(\"android-app://com.example.helloworld/http/example.com/gizmos\");\n"
                        + "  static final Uri WEB_URL = Uri.parse(\"http://example.com/gizmos\");\n"
                        + "  private GoogleApiClient mClient;\n"
                        + "\n"
                        + "  @Override\n"
                        + "  protected void onCreate(Bundle savedInstanceState) {\n"
                        + "    super.onCreate(savedInstanceState);\n"
                        + "    mClient = new GoogleApiClient.Builder(this).build();\n"
                        + "  }\n"
                        + "\n"
                        + "  @Override\n"
                        + "  public void onStart(){\n"
                        + "    super.onStart();\n"
                        + "    mClient.connect();\n"
                        + "    final String title = \"App Indexing API Title\";\n"
                        + "    Action action = Action.newAction(Action.TYPE_VIEW, title, WEB_URL, APP_URI);\n"
                        + "    AppIndex.AppIndexApi.start(mClient, action);\n"
                        + "  }\n"
                        + "\n"
                        + "  @Override\n"
                        + "  public void onStop(){\n"
                        + "    super.onStop();\n"
                        + "    final String title = \"App Indexing API Title\";\n"
                        + "    Action action = Action.newAction(Action.TYPE_VIEW, title, WEB_URL, APP_URI);\n"
                        + "    AppIndex.AppIndexApi.end(mClient, action);\n"
                        + "    mClient.disconnect();\n"
                        + "  }\n"
                        + "}\n"
                        + "\n"),
                mApp_indexing_api_test,
                mAppIndex,
                mAppIndexApi,
                mGoogleApiClient,
                mActivity,
                mApi)
                .run()
                .expect(expected);
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mActivity = java(""
            + "package android.app;\n"
            + "\n"
            + "import android.content.Context;\n"
            + "\n"
            + "// Stub class for testing.\n"
            + "public class Activity extends Context {}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mApi = java(""
            + "package com.google.android.gms.common.api;\n"
            + "\n"
            + "public final class Api<O extends Api.ApiOptions> {\n"
            + "    public interface ApiOptions {\n"
            + "        public interface NotRequiredOptions extends Api.ApiOptions {}\n"
            + "        public static final class NoOptions implements Api.ApiOptions.NotRequiredOptions {}\n"
            + "    }\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mAppIndex = java(""
            + "package com.google.android.gms.appindexing;\n"
            + "\n"
            + "import com.google.android.gms.appindexing.AppIndexApi;\n"
            + "import com.google.android.gms.common.api.Api;\n"
            + "import com.google.android.gms.common.api.Api.ApiOptions.NoOptions;\n"
            + "\n"
            + "public final class AppIndex {\n"
            + "    public static final AppIndexApi AppIndexApi;\n"
            + "    public static final Api<NoOptions> APP_INDEX_API;\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mAppIndexApi = java(""
            + "package com.google.android.gms.appindexing;\n"
            + "\n"
            + "import android.app.Activity;\n"
            + "import android.content.Intent;\n"
            + "import android.net.Uri;\n"
            + "import android.view.View;\n"
            + "import com.google.android.gms.appindexing.Action;\n"
            + "import com.google.android.gms.common.api.GoogleApiClient;\n"
            + "import com.google.android.gms.common.api.PendingResult;\n"
            + "import com.google.android.gms.common.api.Status;\n"
            + "import java.util.List;\n"
            + "\n"
            + "public class AppIndexApi {\n"
            + "    PendingResult<Status> view(GoogleApiClient var1, Activity var2, Intent var3, String var4, Uri var5, List<AppIndexApi.AppIndexingLink> var6) {}\n"
            + "\n"
            + "    PendingResult<Status> viewEnd(GoogleApiClient var1, Activity var2, Intent var3) {}\n"
            + "\n"
            + "    PendingResult<Status> view(GoogleApiClient var1, Activity var2, Uri var3, String var4, Uri var5, List<AppIndexApi.AppIndexingLink> var6) {}\n"
            + "\n"
            + "    PendingResult<Status> viewEnd(GoogleApiClient var1, Activity var2, Uri var3) {}\n"
            + "\n"
            + "    PendingResult<Status> start(GoogleApiClient var1, Action var2) {}\n"
            + "\n"
            + "    PendingResult<Status> end(GoogleApiClient var1, Action var2) {}\n"
            + "\n"
            + "    public static final class AppIndexingLink {}\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mAppIndexingApiTestOk = java(""
            + "package com.example.helloworld;\n"
            + "\n"
            + "import android.app.Activity;\n"
            + "import android.net.Uri;\n"
            + "import android.os.Bundle;\n"
            + "\n"
            + "import com.google.android.gms.appindexing.Action;\n"
            + "import com.google.android.gms.appindexing.AppIndex;\n"
            + "import com.google.android.gms.common.api.GoogleApiClient;\n"
            + "\n"
            + "public class AppIndexingApiTest extends Activity {\n"
            + "  static final Uri APP_URI = Uri.parse(\"android-app://com.example.helloworld/http/example.com/gizmos\");\n"
            + "  static final Uri WEB_URL = Uri.parse(\"http://example.com/gizmos\");\n"
            + "  private GoogleApiClient mClient;\n"
            + "\n"
            + "  @Override\n"
            + "  protected void onCreate(Bundle savedInstanceState) {\n"
            + "    super.onCreate(savedInstanceState);\n"
            + "    mClient = new GoogleApiClient.Builder(this).addApi(AppIndex.APP_INDEX_API).build();\n"
            + "  }\n"
            + "\n"
            + "  @Override\n"
            + "  public void onStart(){\n"
            + "    super.onStart();\n"
            + "    mClient.connect();\n"
            + "    final String title = \"App Indexing API Title\";\n"
            + "    Action action = Action.newAction(Action.TYPE_VIEW, title, WEB_URL, APP_URI);\n"
            + "    AppIndex.AppIndexApi.start(mClient, action);\n"
            + "  }\n"
            + "\n"
            + "  @Override\n"
            + "  public void onStop(){\n"
            + "    super.onStop();\n"
            + "    final String title = \"App Indexing API Title\";\n"
            + "    Action action = Action.newAction(Action.TYPE_VIEW, title, WEB_URL, APP_URI);\n"
            + "    AppIndex.AppIndexApi.end(mClient, action);\n"
            + "    mClient.disconnect();\n"
            + "  }\n"
            + "}\n"
            + "\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mApp_indexing_api_test = xml("AndroidManifest.xml", ""
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "          package=\"com.example.helloworld\">\n"
            + "\n"
            + "    <application\n"
            + "            android:allowBackup=\"true\"\n"
            + "            android:icon=\"@mipmap/ic_launcher\"\n"
            + "            android:label=\"@string/app_name\"\n"
            + "            android:theme=\"@style/AppTheme\" >\n"
            + "        <activity\n"
            + "                android:name=\".AppIndexingApiTest\"\n"
            + "                android:configChanges=\"orientation|keyboardHidden|screenSize\"\n"
            + "                android:label=\"@string/title_activity_fullscreen\"\n"
            + "                android:theme=\"@style/FullscreenTheme\" >\n"
            + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
            + "                <action android:name=\"android.intent.action.VIEW\" />\n"
            + "                <data android:scheme=\"http\"\n"
            + "                      android:host=\"example.com\"\n"
            + "                      android:pathPrefix=\"/gizmos\" />\n"
            + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
            + "                <category android:name=\"android.intent.category.BROWSABLE\" />\n"
            + "            </intent-filter>\n"
            + "        </activity>\n"
            + "\n"
            + "        <meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\" />\n"
            + "    </application>\n"
            + "\n"
            + "</manifest>\n"
            + "\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mAppindexing_manifest = xml("AndroidManifest.xml", ""
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "          package=\"com.example.helloworld\">\n"
            + "\n"
            + "    <application\n"
            + "            android:allowBackup=\"true\"\n"
            + "            android:icon=\"@mipmap/ic_launcher\"\n"
            + "            android:label=\"@string/app_name\"\n"
            + "            android:theme=\"@style/AppTheme\" >\n"
            + "        <activity\n"
            + "                android:name=\".FullscreenActivity\"\n"
            + "                android:configChanges=\"orientation|keyboardHidden|screenSize\"\n"
            + "                android:label=\"@string/title_activity_fullscreen\"\n"
            + "                android:theme=\"@style/FullscreenTheme\" >\n"
            + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
            + "                <action android:name=\"android.intent.action.VIEW\" />\n"
            + "                <data android:scheme=\"http\"\n"
            + "                      android:host=\"example.com\"\n"
            + "                      android:pathPrefix=\"@string/path_prefix\"\n"
            + "                      android:port=\"@string/port\"/>\n"
            + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
            + "                <category android:name=\"android.intent.category.BROWSABLE\" />\n"
            + "            </intent-filter>\n"
            + "        </activity>\n"
            + "\n"
            + "        <meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\" />\n"
            + "    </application>\n"
            + "\n"
            + "</manifest>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mGoogleApiClient = java(""
            + "package com.google.android.gms.common.api;\n"
            + "\n"
            + "import com.google.android.gms.common.api.Api;\n"
            + "import com.google.android.gms.common.api.Api.ApiOptions.NotRequiredOptions;\n"
            + "\n"
            + "public abstract class GoogleApiClient {\n"
            + "    public abstract void connect() {}\n"
            + "    public abstract void disconnect() {}\n"
            + "\n"
            + "    public static final class Builder {\n"
            + "        public Builder(Context c);\n"
            + "        public GoogleApiClient.Builder addApi(Api<? extends NotRequiredOptions> api);\n"
            + "        public GoogleApiClient build();\n"
            + "    }\n"
            + "}\n");
}
