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

import com.android.tools.lint.detector.api.Detector;

public class PreferenceActivityDetectorTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new PreferenceActivityDetector();
    }

    public void testWarningWhenImplicitlyExportingPreferenceActivity() throws Exception {
        String expected = ""
                + "AndroidManifest.xml:28: Warning: PreferenceActivity should not be exported [ExportedPreferenceActivity]\n"
                + "        <activity\n"
                + "        ^\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<!--\n"
                        + "  ~ Copyright (C) 2014 The Android Open Source Project\n"
                        + "  ~\n"
                        + "  ~ Licensed under the Apache License, Version 2.0 (the \"License\");\n"
                        + "  ~ you may not use this file except in compliance with the License.\n"
                        + "  ~ You may obtain a copy of the License at\n"
                        + "  ~\n"
                        + "  ~      http://www.apache.org/licenses/LICENSE-2.0\n"
                        + "  ~\n"
                        + "  ~ Unless required by applicable law or agreed to in writing, software\n"
                        + "  ~ distributed under the License is distributed on an \"AS IS\" BASIS,\n"
                        + "  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
                        + "  ~ See the License for the specific language governing permissions and\n"
                        + "  ~ limitations under the License.\n"
                        + "  -->\n"
                        + "\n"
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
                        + "            android:name=\"android.preference.PreferenceActivity\"\n"
                        + "            android:label=\"@string/app_name\" >\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                        + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testWarningWhenExplicitlyExportingPreferenceActivity() throws Exception {
        String expected = ""
                + "AndroidManifest.xml:28: Warning: PreferenceActivity should not be exported [ExportedPreferenceActivity]\n"
                + "        <activity\n"
                + "        ^\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<!--\n"
                        + "  ~ Copyright (C) 2014 The Android Open Source Project\n"
                        + "  ~\n"
                        + "  ~ Licensed under the Apache License, Version 2.0 (the \"License\");\n"
                        + "  ~ you may not use this file except in compliance with the License.\n"
                        + "  ~ You may obtain a copy of the License at\n"
                        + "  ~\n"
                        + "  ~      http://www.apache.org/licenses/LICENSE-2.0\n"
                        + "  ~\n"
                        + "  ~ Unless required by applicable law or agreed to in writing, software\n"
                        + "  ~ distributed under the License is distributed on an \"AS IS\" BASIS,\n"
                        + "  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
                        + "  ~ See the License for the specific language governing permissions and\n"
                        + "  ~ limitations under the License.\n"
                        + "  -->\n"
                        + "\n"
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
                        + "            android:name=\"android.preference.PreferenceActivity\"\n"
                        + "            android:label=\"@string/app_name\"\n"
                        + "            android:exported=\"true\">\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testNoWarningWhenExportingNonPreferenceActivity() throws Exception {
        lint().files(manifest().minSdk(14)).run().expectClean();
    }

    public void testNoWarningWhenSuppressed() throws Exception {
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<!--\n"
                        + "  ~ Copyright (C) 2014 The Android Open Source Project\n"
                        + "  ~\n"
                        + "  ~ Licensed under the Apache License, Version 2.0 (the \"License\");\n"
                        + "  ~ you may not use this file except in compliance with the License.\n"
                        + "  ~ You may obtain a copy of the License at\n"
                        + "  ~\n"
                        + "  ~      http://www.apache.org/licenses/LICENSE-2.0\n"
                        + "  ~\n"
                        + "  ~ Unless required by applicable law or agreed to in writing, software\n"
                        + "  ~ distributed under the License is distributed on an \"AS IS\" BASIS,\n"
                        + "  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
                        + "  ~ See the License for the specific language governing permissions and\n"
                        + "  ~ limitations under the License.\n"
                        + "  -->\n"
                        + "\n"
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
                        + "        <!--suppress ExportedPreferenceActivity -->\n"
                        + "        <activity\n"
                        + "            android:name=\"android.preference.PreferenceActivity\"\n"
                        + "            android:label=\"@string/app_name\" >\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                        + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expectClean();
    }

    public void testWarningWhenImplicitlyExportingPreferenceActivitySubclass() throws Exception {
        String expected = ""
                + "AndroidManifest.xml:28: Warning: PreferenceActivity subclass test.pkg.PreferenceActivitySubclass should not be exported [ExportedPreferenceActivity]\n"
                + "        <activity\n"
                + "        ^\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                mPreferenceActivity,
                mPreferenceActivitySubclass,
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<!--\n"
                        + "  ~ Copyright (C) 2014 The Android Open Source Project\n"
                        + "  ~\n"
                        + "  ~ Licensed under the Apache License, Version 2.0 (the \"License\");\n"
                        + "  ~ you may not use this file except in compliance with the License.\n"
                        + "  ~ You may obtain a copy of the License at\n"
                        + "  ~\n"
                        + "  ~      http://www.apache.org/licenses/LICENSE-2.0\n"
                        + "  ~\n"
                        + "  ~ Unless required by applicable law or agreed to in writing, software\n"
                        + "  ~ distributed under the License is distributed on an \"AS IS\" BASIS,\n"
                        + "  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
                        + "  ~ See the License for the specific language governing permissions and\n"
                        + "  ~ limitations under the License.\n"
                        + "  -->\n"
                        + "\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg\"\n"
                        + "    android:versionCode=\"1\"\n"
                        + "    android:versionName=\"1.0\" >\n"
                        + "\n"
                        + "    <uses-sdk android:minSdkVersion=\"10\" />\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:icon=\"@drawable/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\" >\n"
                        + "        <activity\n"
                        + "            android:name=\".PreferenceActivitySubclass\"\n"
                        + "            android:label=\"@string/app_name\" >\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                        + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testWarningWhenExplicitlyExportingPreferenceActivitySubclass() throws Exception {
        String expected = ""
                + "AndroidManifest.xml:28: Warning: PreferenceActivity subclass test.pkg.PreferenceActivitySubclass should not be exported [ExportedPreferenceActivity]\n"
                + "        <activity\n"
                + "        ^\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                mPreferenceActivity,
                mPreferenceActivitySubclass,
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<!--\n"
                        + "  ~ Copyright (C) 2014 The Android Open Source Project\n"
                        + "  ~\n"
                        + "  ~ Licensed under the Apache License, Version 2.0 (the \"License\");\n"
                        + "  ~ you may not use this file except in compliance with the License.\n"
                        + "  ~ You may obtain a copy of the License at\n"
                        + "  ~\n"
                        + "  ~      http://www.apache.org/licenses/LICENSE-2.0\n"
                        + "  ~\n"
                        + "  ~ Unless required by applicable law or agreed to in writing, software\n"
                        + "  ~ distributed under the License is distributed on an \"AS IS\" BASIS,\n"
                        + "  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
                        + "  ~ See the License for the specific language governing permissions and\n"
                        + "  ~ limitations under the License.\n"
                        + "  -->\n"
                        + "\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg\"\n"
                        + "    android:versionCode=\"1\"\n"
                        + "    android:versionName=\"1.0\" >\n"
                        + "\n"
                        + "    <uses-sdk android:minSdkVersion=\"10\" />\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:icon=\"@drawable/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\" >\n"
                        + "        <activity\n"
                        + "            android:name=\".PreferenceActivitySubclass\"\n"
                        + "            android:label=\"@string/app_name\"\n"
                        + "            android:exported=\"true\">\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testNoWarningWhenActivityNotExported() throws Exception {
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<!--\n"
                        + "  ~ Copyright (C) 2014 The Android Open Source Project\n"
                        + "  ~\n"
                        + "  ~ Licensed under the Apache License, Version 2.0 (the \"License\");\n"
                        + "  ~ you may not use this file except in compliance with the License.\n"
                        + "  ~ You may obtain a copy of the License at\n"
                        + "  ~\n"
                        + "  ~      http://www.apache.org/licenses/LICENSE-2.0\n"
                        + "  ~\n"
                        + "  ~ Unless required by applicable law or agreed to in writing, software\n"
                        + "  ~ distributed under the License is distributed on an \"AS IS\" BASIS,\n"
                        + "  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
                        + "  ~ See the License for the specific language governing permissions and\n"
                        + "  ~ limitations under the License.\n"
                        + "  -->\n"
                        + "\n"
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
                        + "            android:name=\"android.preference.PreferenceActivity\"\n"
                        + "            android:label=\"@string/app_name\" >\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expectClean();
    }

    public void testWarningWhenTargetSDK19ButNoIsValidFragmentOverridden() throws Exception {
        lint().files(
                mPreferenceActivity,
                mPreferenceActivitySubclass,
                mExport_preference_activity_subclass_target_sdk_19)
                .run()
                .expect(""
                + "AndroidManifest.xml:30: Warning: PreferenceActivity subclass test.pkg.PreferenceActivitySubclass should not be exported [ExportedPreferenceActivity]\n"
                + "        <activity\n"
                + "        ^\n"
                + "0 errors, 1 warnings\n");
    }

    public void testNoWarningWhenTargetSDK19AndIsValidFragmentOverridden() throws Exception {
        //noinspection all // Sample code
        lint().files(
                mPreferenceActivity,
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.preference.PreferenceActivity;\n"
                        + "\n"
                        + "public class PreferenceActivitySubclass extends PreferenceActivity {\n"
                        + "\n"
                        + "    protected boolean isValidFragment(String fragmentName) {\n"
                        + "        return false;\n"
                        + "    }\n"
                        + "}\n"),
                mExport_preference_activity_subclass_target_sdk_19)
                .run()
                .expectClean();
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mExport_preference_activity_subclass_target_sdk_19 = xml("AndroidManifest.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<!--\n"
            + "  ~ Copyright (C) 2014 The Android Open Source Project\n"
            + "  ~\n"
            + "  ~ Licensed under the Apache License, Version 2.0 (the \"License\");\n"
            + "  ~ you may not use this file except in compliance with the License.\n"
            + "  ~ You may obtain a copy of the License at\n"
            + "  ~\n"
            + "  ~      http://www.apache.org/licenses/LICENSE-2.0\n"
            + "  ~\n"
            + "  ~ Unless required by applicable law or agreed to in writing, software\n"
            + "  ~ distributed under the License is distributed on an \"AS IS\" BASIS,\n"
            + "  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
            + "  ~ See the License for the specific language governing permissions and\n"
            + "  ~ limitations under the License.\n"
            + "  -->\n"
            + "\n"
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    package=\"test.pkg\"\n"
            + "    android:versionCode=\"1\"\n"
            + "    android:versionName=\"1.0\" >\n"
            + "\n"
            + "    <uses-sdk\n"
            + "            android:minSdkVersion=\"10\"\n"
            + "            android:targetSdkVersion=\"19\" />\n"
            + "\n"
            + "    <application\n"
            + "        android:icon=\"@drawable/ic_launcher\"\n"
            + "        android:label=\"@string/app_name\" >\n"
            + "        <activity\n"
            + "            android:name=\".PreferenceActivitySubclass\"\n"
            + "            android:label=\"@string/app_name\"\n"
            + "            android:exported=\"true\">\n"
            + "        </activity>\n"
            + "    </application>\n"
            + "\n"
            + "</manifest>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mPreferenceActivity = java(""
            + "package android.preference;\n"
            + "\n"
            + "import android.app.Activity;\n"
            + "\n"
            + "/**\n"
            + " * A mock PreferenceActivity for use in tests.\n"
            + " */\n"
            + "public class PreferenceActivity extends Activity {\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mPreferenceActivitySubclass = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.preference.PreferenceActivity;\n"
            + "\n"
            + "public class PreferenceActivitySubclass extends PreferenceActivity {\n"
            + "\n"
            + "}\n");
}
