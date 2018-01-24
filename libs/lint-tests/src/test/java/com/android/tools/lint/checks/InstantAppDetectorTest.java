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

import com.android.tools.lint.checks.infrastructure.TestFile.GradleTestFile;
import com.android.tools.lint.detector.api.Detector;

public class InstantAppDetectorTest extends AbstractCheckTest {
    public void testNoWarningInNonInstantAppModules() {
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.BroadcastReceiver;\n"
                        + "import android.content.Context;\n"
                        + "import android.content.IntentFilter;\n"
                        + "\n"
                        + "public class RegisterReceiverTest {\n"
                        + "    public void test(Context context, BroadcastReceiver receiver, IntentFilter filter) {\n"
                        + "        context.registerReceiver(receiver, filter);\n"
                        + "    }\n"
                        + "}\n"),
                gradle(""
                        + "apply plugin: 'com.android.application'\n")) // not atom or instant-app
                .run()
                .expectClean();
    }

    public void testForbiddenManifestTags() {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg\" >\n"
                        + "    <uses-sdk android:targetSdkVersion=\"23\" />\n"
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
                        + "        <provider android:name=\".TestService\" />\n"
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
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"),
                createGradleTestFile())
                .run()
                .expect(""
                        + "src/main/AndroidManifest.xml:10: Warning: Instant Apps are not allowed to export services, receivers, and providers [InstantApps]\n"
                        + "        <service android:name=\".WearMessageListenerService\">\n"
                        + "        ^\n"
                        + "src/main/AndroidManifest.xml:15: Warning: Instant Apps are not allowed to export services, receivers, and providers [InstantApps]\n"
                        + "        <provider android:name=\".TestService\" />\n"
                        + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "src/main/AndroidManifest.xml:16: Warning: Instant Apps are not allowed to export services, receivers, and providers [InstantApps]\n"
                        + "        <receiver android:name=\".DeviceAdminTestReceiver\"\n"
                        + "        ^\n"
                        + "0 errors, 3 warnings\n");
    }

    public void testRegisterReceiverFromJava() {
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.BroadcastReceiver;\n"
                        + "import android.content.Context;\n"
                        + "import android.content.IntentFilter;\n"
                        + "\n"
                        + "public class RegisterReceiverTest {\n"
                        + "    public void test(Context context, BroadcastReceiver receiver, IntentFilter filter) {\n"
                        + "        context.registerReceiver(receiver, filter);\n"
                        + "    }\n"
                        + "}\n"),
                createGradleTestFile())
                .run()
                .expect(""
                        + "src/main/java/test/pkg/RegisterReceiverTest.java:9: Warning: Instant Apps are not allowed to listen to broadcasts from system or other apps [InstantApps]\n"
                        + "        context.registerReceiver(receiver, filter);\n"
                        + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "0 errors, 1 warnings\n");
    }

    public void testTargetSdkVersion() {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg\" >\n"
                        + "    <uses-sdk android:targetSdkVersion=\"22\" />\n"
                        + "    <uses-sdk android:targetSdkVersion=\"23\" />\n"
                        + "    <uses-sdk android:targetSdkVersion=\"24\" />\n"
                        + "    <uses-sdk android:targetSdkVersion=\"O\" />\n"
                        + "\n"
                        + "</manifest>\n"),
                createGradleTestFile())
                .run()
                .expect(""
                        + "src/main/AndroidManifest.xml:4: Warning: Instant Apps must target API 23+ [InstantApps]\n"
                        + "    <uses-sdk android:targetSdkVersion=\"22\" />\n"
                        + "              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "build.gradle: Warning: Instant Apps must target API 23+ (was API 22) [InstantApps]\n"
                        + "0 errors, 2 warnings\n");
    }

    public void testTargetSdkVersionFromGradle() {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg\" >\n"
                        + "\n"
                        + "</manifest>\n"),
                gradle(""
                        + "buildscript {\n"
                        + "    dependencies {\n"
                        + "        classpath 'com.android.tools.build:gradle:2.4.0-alpha1'\n"
                        + "    }\n"
                        + "}\n"
                        + "\n"
                        + "apply plugin: 'com.android.instantapp'\n"
                        + "\n"
                        + "android {\n"
                        + "    defaultConfig {\n"
                        + "        targetSdkVersion 21\n"
                        + "    }\n"
                        + "}\n"
                        + "\n"))
                .run()
                .expect(""
                        + "build.gradle: Warning: Instant Apps must target API 23+ (was API 21) [InstantApps]\n"
                        + "0 errors, 1 warnings\n");
    }

    public void testPermission() {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg\" >\n"

                        // Current allowed permissions:
                        + "    <uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\" />\n"
                        + "    <uses-permission android:name=\"android.permission.ACCESS_WIFI_STATE\" />\n"
                        + "    <uses-permission android:name=\"android.permission.INTERNET\" />\n"
                        + "    <uses-permission android:name=\"android.permission.WAKE_LOCK\" />\n"
                        + "    <uses-permission android:name=\"android.permission.VIBRATE\" />\n"
                        + "    <uses-permission android:name=\"android.permission.ACCESS_COARSE_LOCATION\" />\n"
                        + "    <uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\" />\n"
                        + "    <uses-permission android:name=\"android.permission.RECORD_AUDIO\" />\n"

                        // Other permissions are not allowed:
                        + "    <uses-permission android:name=\"android.permission.SET_PREFERRED_APPLICATIONS\" />\n"
                        + "    <uses-permission android:name=\"com.example.helloworld.permission\" />\n"
                        + "    <permission android:name=\"test.pkg.permission.C2D_MESSAGE\"\n"
                        + "                android:protectionLevel=\"signature\" />\n"
                        + "    <uses-permission android:name=\"test.pkg.permission.C2D_MESSAGE\" />\n"
                        + "\n"
                        + "</manifest>\n"),
                createGradleTestFile())
                .run()
                .expect(""
                        + "src/main/AndroidManifest.xml:12: Warning: This permission is not allowed for Instant Apps [InstantApps]\n"
                        + "    <uses-permission android:name=\"android.permission.SET_PREFERRED_APPLICATIONS\" />\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "src/main/AndroidManifest.xml:16: Warning: Instant Apps are not allowed to use Google Cloud Messaging (GCM) [InstantApps]\n"
                        + "    <uses-permission android:name=\"test.pkg.permission.C2D_MESSAGE\" />\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "0 errors, 2 warnings\n");
    }

    public void testNotifications() {
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.app.Notification;\n"
                        + "import android.app.NotificationManager;\n"
                        + "\n"
                        + "public class NotificationTest extends Activity {\n"
                        + "    public void test() {\n"
                        + "        Notification notification = new Notification.Builder(this).setColor(0xFF00FFFF).build();\n"
                        + "        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);\n"
                        + "        manager.notify(1, notification);\n"
                        + "    }\n"
                        + "}\n"),
                createGradleTestFile())
                .run()
                .expect(""
                        + "src/main/java/test/pkg/NotificationTest.java:11: Warning: Instant Apps are not allowed to create notifications [InstantApps]\n"
                        + "        manager.notify(1, notification);\n"
                        + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "0 errors, 1 warnings\n");
    }

    public void testNoMacAddressAccess() {
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.bluetooth.BluetoothAdapter;\n"
                        + "import android.net.wifi.WifiInfo;\n"
                        + "\n"
                        + "public class MacAddressTest {\n"
                        + "    public void test(WifiInfo wifiInfo) {\n"
                        + "        String macAddress = wifiInfo.getMacAddress();\n"
                        + "    }\n"
                        + "\n"
                        + "    public void test(BluetoothAdapter adapter) {\n"
                        + "        String address = adapter.getAddress();\n"
                        + "    }\n"
                        + "}\n"),
                createGradleTestFile())
                .run()
                .expect(""
                        + "src/main/java/test/pkg/MacAddressTest.java:8: Warning: Instant Apps accessing \"Mac Addresses\" will get a XXX value [InstantApps]\n"
                        + "        String macAddress = wifiInfo.getMacAddress();\n"
                        + "                            ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "src/main/java/test/pkg/MacAddressTest.java:12: Warning: Instant Apps accessing \"Mac Addresses\" will get a XXX value [InstantApps]\n"
                        + "        String address = adapter.getAddress();\n"
                        + "                         ~~~~~~~~~~~~~~~~~~~~\n"
                        + "0 errors, 2 warnings\n");
    }

    public void testNoSerialAccess() {
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.os.Build;\n"
                        + "\n"
                        + "import static android.os.Build.SERIAL;\n"
                        + "\n"
                        + "public class SerialTest {\n"
                        + "    public void test() {\n"
                        + "        String serial1 = android.os.Build.SERIAL;\n"
                        + "        String serial2 = Build.SERIAL;\n"
                        + "        String serial3 = SERIAL;\n"
                        + "    }\n"
                        + "}\n"),
                createGradleTestFile())
                .run()
                .expect("src/main/java/test/pkg/SerialTest.java:9: Warning: Instant Apps accessing \"Build Serial\" will get a XXX value [InstantApps]\n" +
                        "        String serial1 = android.os.Build.SERIAL;\n" +
                        "                                          ~~~~~~\n" +
                        "src/main/java/test/pkg/SerialTest.java:10: Warning: Instant Apps accessing \"Build Serial\" will get a XXX value [InstantApps]\n" +
                        "        String serial2 = Build.SERIAL;\n" +
                        "                               ~~~~~~\n" +
                        "src/main/java/test/pkg/SerialTest.java:11: Warning: Instant Apps accessing \"Build Serial\" will get a XXX value [InstantApps]\n" +
                        "        String serial3 = SERIAL;\n" +
                        "                         ~~~~~~\n" +
                        "0 errors, 3 warnings\n");
    }

    public void testAndroidIdFromGservices() {
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.ContentResolver;\n"
                        + "\n"
                        + "public class GSerialTest {\n"
                        + "    public void test(ContentResolver resolver) {\n"
                        + "        com.google.android.gsf.Gservices.getLong(resolver, \"ok\", 0);\n"
                        + "        com.google.android.gsf.Gservices.getLong(resolver, \"android_id\", 0);\n"
                        + "    }\n"
                        + "}\n"),
                java(""
                        + "package com.google.android.gsf;\n"
                        + "\n"
                        + "import android.content.ContentResolver;\n"
                        + "\n"
                        + "// Dummy stub\n"
                        + "public class Gservices {\n"
                        + "    public static long getLong(ContentResolver cr, String key, long defValue) {\n"
                        + "        return 0L;\n"
                        + "    }\n"
                        + "}\n"),
                createGradleTestFile())
                .run()
                .expect(""
                        + "src/main/java/test/pkg/GSerialTest.java:8: Warning: Instant Apps accessing \"Android Id\" will get a XXX value [InstantApps]\n"
                        + "        com.google.android.gsf.Gservices.getLong(resolver, \"android_id\", 0);\n"
                        + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "0 errors, 1 warnings\n");
    }

    public void testAccessSettingsSecureAndroidId() {
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.ContentResolver;\n"
                        + "import android.provider.Settings;\n"
                        + "\n"
                        + "public class SecureIdTest {\n"
                        + "    public void test(ContentResolver resolver) {\n"
                        + "        String s = Settings.Secure.ANDROID_ID;\n"
                        + "    }\n"
                        + "}\n"),
                createGradleTestFile())
                .run()
                .expect("" +
                        "src/main/java/test/pkg/SecureIdTest.java:8: Warning: Instant Apps accessing \"Settings.Secure Android Id\" will get a XXX value [InstantApps]\n"+
                        "        String s = Settings.Secure.ANDROID_ID;\n"+
                        "                                   ~~~~~~~~~~\n"+"0 errors, 1 warnings\n");
    }

    public void testSingleLaunchableActivity() {
        //noinspection all // Sample code
        lint().files(
                manifest(""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg\" >\n"
                        + "    <uses-sdk android:targetSdkVersion=\"23\" />\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:allowBackup=\"false\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "        <activity\n"
                        + "            android:name=\".LaunchableActivity1\" >\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                        + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "        <activity\n"
                        + "            android:name=\".NonLaunchableActivity\" >\n"
                        + "        </activity>\n"
                        + "\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"),
                manifest("src/debug/AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg.debug\" >\n"
                        + "    <application android:useRtl='true'>\n"
                        + "        <activity\n"
                        + "            android:name=\".LaunchableActivity2\" >\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                        + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"),

                createGradleTestFile())
                .run()
                .expect(""
                        + "src/debug/AndroidManifest.xml:9: Warning: Instant Apps are not allowed to have multiple launchable activities [InstantApps]\n"
                        + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                        + "                          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "    src/main/AndroidManifest.xml:14: Other launchable activity here\n"
                        + "0 errors, 1 warnings\n");
    }

    /**
     * Gradle test file used by all the instant app tests that specify a new-enough Gradle
     * version and uses an instant app or atom model type
     */
    private static GradleTestFile createGradleTestFile() {
        return gradle(""
                + "buildscript {\n"
                + "    repositories {\n"
                + "        jcenter()\n"
                + "    }\n"
                + "    dependencies {\n"
                + "        classpath 'com.android.tools.build:gradle:2.4.0-alpha1'\n"
                + "    }\n"
                + "}\n"
                + ""
                + "apply plugin: 'com.android.instantapp'\n");
    }

    @Override
    protected Detector getDetector() {
        return new InstantAppDetector();
    }
}