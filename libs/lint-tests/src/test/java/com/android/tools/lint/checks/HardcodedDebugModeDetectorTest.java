/*
 * Copyright (C) 2012 The Android Open Source Project
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
public class HardcodedDebugModeDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new HardcodedDebugModeDetector();
    }

    public void test() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:10: Error: Avoid hardcoding the debug mode; leaving it out allows debug and release builds to automatically assign one [HardcodedDebugMode]\n"
                + "        android:debuggable=\"true\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",
            lintProject(xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"foo.bar2\"\n"
                            + "    android:versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\" >\n"
                            + "\n"
                            + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:debuggable=\"true\"\n"
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

    public void testOk() throws Exception {
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",
                lintProject(manifest().minSdk(14)));
    }
}
