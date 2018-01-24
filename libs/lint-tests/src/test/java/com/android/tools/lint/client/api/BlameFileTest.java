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

package com.android.tools.lint.client.api;

import static com.google.common.truth.Truth.assertThat;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.TestFile;
import com.android.tools.lint.checks.infrastructure.TestFile.XmlTestFile;
import com.android.tools.lint.checks.infrastructure.TestLintClient;
import com.android.utils.Pair;
import com.android.utils.XmlUtils;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class BlameFileTest {
    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

    @Test
    public void test() throws IOException {
        File root = folder.getRoot();

        File sourceManifest = XmlTestFile.create("app/src/main/AndroidManifest.xml", ""
                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    package=\"test.pkg.myapplication\">\n"
                + "\n"
                + "    <application\n"
                + "        android:allowBackup=\"true\"\n"
                + "        android:icon=\"@mipmap/ic_launcher\"\n"
                + "        android:label=\"@string/app_name\"\n"
                + "        android:roundIcon=\"@mipmap/ic_launcher_round\"\n"
                + "        android:supportsRtl=\"true\"\n"
                + "        android:theme=\"@style/AppTheme\">\n"
                + "        <activity android:name=\".MainActivity\">\n"
                + "            <intent-filter>\n"
                + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                + "\n"
                + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                + "            </intent-filter>\n"
                + "        </activity>\n"
                + "    </application>\n"
                + "\n"
                + "</manifest>")
                .createFile(root);
        File mergedManifest = XmlTestFile.create("app/build/intermediates/manifests/full/debug/AndroidManifest.xml", ""
                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    package=\"test.pkg.myapplication\"\n"
                + "    android:versionCode=\"1\"\n"
                + "    android:versionName=\"1.0\" >\n"
                + "\n"
                + "    <uses-sdk\n"
                + "        android:minSdkVersion=\"25\"\n"
                + "        android:targetSdkVersion=\"25\" />\n"
                + "\n"
                + "    <application\n"
                + "        android:allowBackup=\"true\"\n"
                + "        android:icon=\"@mipmap/ic_launcher\"\n"
                + "        android:label=\"@string/app_name\"\n"
                + "        android:roundIcon=\"@mipmap/ic_launcher_round\"\n"
                + "        android:supportsRtl=\"true\"\n"
                + "        android:theme=\"@style/AppTheme\" >\n"
                + "        <activity android:name=\"test.pkg.myapplication.MainActivity\" >\n"
                + "            <intent-filter>\n"
                + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                + "\n"
                + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                + "            </intent-filter>\n"
                + "        </activity>\n"
                + "    </application>\n"
                + "\n"
                + "</manifest>")
                .createFile(root);
        String blameLog = ""
                + "-- Merging decision tree log ---\n"
                + "manifest\n"
                + "ADDED from ${ROOT}/app/src/main/AndroidManifest.xml:2:1-21:12\n"
                + "\tpackage\n"
                + "\t\tADDED from ${ROOT}/app/src/main/AndroidManifest.xml:3:5-37\n"
                + "\t\tINJECTED from ${ROOT}/app/src/main/AndroidManifest.xml\n"
                + "\t\tINJECTED from ${ROOT}/app/src/main/AndroidManifest.xml\n"
                + "\tandroid:versionName\n"
                + "\t\tINJECTED from ${ROOT}/app/src/main/AndroidManifest.xml\n"
                + "\t\tINJECTED from ${ROOT}/app/src/main/AndroidManifest.xml\n"
                + "\txmlns:android\n"
                + "\t\tADDED from ${ROOT}/app/src/main/AndroidManifest.xml:2:11-69\n"
                + "\tandroid:versionCode\n"
                + "\t\tINJECTED from ${ROOT}/app/src/main/AndroidManifest.xml\n"
                + "\t\tINJECTED from ${ROOT}/app/src/main/AndroidManifest.xml\n"
                + "application\n"
                + "ADDED from ${ROOT}/app/src/main/AndroidManifest.xml:5:5-19:19\n"
                + "MERGED from [com.android.support:appcompat-v7:25.1.0] /Users/android-studio/.android/build-cache/464951d17cb0cdd62cb8c51961d7fe17aa117c02/output/AndroidManifest.xml:25:5-20\n"
                + "MERGED from [com.android.support:support-v4:25.1.0] /Users/android-studio/.android/build-cache/001548c9243f58e5971d6824e456eca9f166350f/output/AndroidManifest.xml:25:5-20\n"
                + "MERGED from [com.android.support:support-fragment:25.1.0] /Users/android-studio/.android/build-cache/f3ab5c6dee0d58f519be2484a899f5a59a33ed80/output/AndroidManifest.xml:25:5-20\n"
                + "MERGED from [com.android.support:support-media-compat:25.1.0] /Users/android-studio/.android/build-cache/42b1e890b38aa4e77f0ce55b2b567d07588af73a/output/AndroidManifest.xml:25:5-20\n"
                + "MERGED from [com.android.support:support-core-ui:25.1.0] /Users/android-studio/.android/build-cache/a908841307bc59a94c150de3f05482491ac1b4c0/output/AndroidManifest.xml:25:5-20\n"
                + "MERGED from [com.android.support:support-core-utils:25.1.0] /Users/android-studio/.android/build-cache/083cf279e1369354c582f8cbbcff781c5f8e39a1/output/AndroidManifest.xml:25:5-20\n"
                + "MERGED from [com.android.support:animated-vector-drawable:25.1.0] /Users/android-studio/.android/build-cache/8be2ac0e03edb0ede39b0ef1a1c6c1b157745a11/output/AndroidManifest.xml:22:5-20\n"
                + "MERGED from [com.android.support:support-vector-drawable:25.1.0] /Users/android-studio/.android/build-cache/9dd1195e71f0171b09c52952f1764029f48226cb/output/AndroidManifest.xml:23:5-20\n"
                + "MERGED from [com.android.support:support-compat:25.1.0] /Users/android-studio/.android/build-cache/2c49c076e18edfa3db00468dda17eb0c39c6061f/output/AndroidManifest.xml:25:5-20\n"
                + "MERGED from [com.android.support.constraint:constraint-layout:1.0.0-beta4] /Users/android-studio/.android/build-cache/0ce0d5454d1fa55b883c367e8f21c424817b47bb/output/AndroidManifest.xml:9:5-20\n"
                + "\tandroid:label\n"
                + "\t\tADDED from ${ROOT}/app/src/main/AndroidManifest.xml:8:9-41\n"
                + "\tandroid:supportsRtl\n"
                + "\t\tADDED from ${ROOT}/app/src/main/AndroidManifest.xml:10:9-35\n"
                + "\tandroid:roundIcon\n"
                + "\t\tADDED from ${ROOT}/app/src/main/AndroidManifest.xml:9:9-54\n"
                + "\tandroid:allowBackup\n"
                + "\t\tADDED from ${ROOT}/app/src/main/AndroidManifest.xml:6:9-35\n"
                + "\tandroid:icon\n"
                + "\t\tADDED from ${ROOT}/app/src/main/AndroidManifest.xml:7:9-43\n"
                + "\tandroid:theme\n"
                + "\t\tADDED from ${ROOT}/app/src/main/AndroidManifest.xml:11:9-40\n"
                + "activity#test.pkg.myapplication.MainActivity\n"
                + "ADDED from ${ROOT}/app/src/main/AndroidManifest.xml:12:9-18:20\n"
                + "\tandroid:name\n"
                + "\t\tADDED from ${ROOT}/app/src/main/AndroidManifest.xml:12:19-47\n"
                + "intent-filter#android.intent.action.MAIN+android.intent.category.LAUNCHER\n"
                + "ADDED from ${ROOT}/app/src/main/AndroidManifest.xml:13:13-17:29\n"
                + "action#android.intent.action.MAIN\n"
                + "ADDED from ${ROOT}/app/src/main/AndroidManifest.xml:14:17-69\n"
                + "\tandroid:name\n"
                + "\t\tADDED from ${ROOT}/app/src/main/AndroidManifest.xml:14:25-66\n"
                + "category#android.intent.category.LAUNCHER\n"
                + "ADDED from ${ROOT}/app/src/main/AndroidManifest.xml:16:17-77\n"
                + "\tandroid:name\n"
                + "\t\tADDED from ${ROOT}/app/src/main/AndroidManifest.xml:16:27-74\n"
                + "uses-sdk\n"
                + "INJECTED from ${ROOT}/app/src/main/AndroidManifest.xml reason: use-sdk injection requested\n"
                + "MERGED from [com.android.support:appcompat-v7:25.1.0] /Users/android-studio/.android/build-cache/464951d17cb0cdd62cb8c51961d7fe17aa117c02/output/AndroidManifest.xml:21:5-23:78\n"
                + "MERGED from [com.android.support:support-v4:25.1.0] /Users/android-studio/.android/build-cache/001548c9243f58e5971d6824e456eca9f166350f/output/AndroidManifest.xml:21:5-23:54\n"
                + "MERGED from [com.android.support:support-fragment:25.1.0] /Users/android-studio/.android/build-cache/f3ab5c6dee0d58f519be2484a899f5a59a33ed80/output/AndroidManifest.xml:21:5-23:60\n"
                + "MERGED from [com.android.support:support-media-compat:25.1.0] /Users/android-studio/.android/build-cache/42b1e890b38aa4e77f0ce55b2b567d07588af73a/output/AndroidManifest.xml:21:5-23:63\n"
                + "MERGED from [com.android.support:support-core-ui:25.1.0] /Users/android-studio/.android/build-cache/a908841307bc59a94c150de3f05482491ac1b4c0/output/AndroidManifest.xml:21:5-23:58\n"
                + "MERGED from [com.android.support:support-core-utils:25.1.0] /Users/android-studio/.android/build-cache/083cf279e1369354c582f8cbbcff781c5f8e39a1/output/AndroidManifest.xml:21:5-23:61\n"
                + "MERGED from [com.android.support:animated-vector-drawable:25.1.0] /Users/android-studio/.android/build-cache/8be2ac0e03edb0ede39b0ef1a1c6c1b157745a11/output/AndroidManifest.xml:20:5-44\n"
                + "MERGED from [com.android.support:support-vector-drawable:25.1.0] /Users/android-studio/.android/build-cache/9dd1195e71f0171b09c52952f1764029f48226cb/output/AndroidManifest.xml:21:5-43\n"
                + "MERGED from [com.android.support:support-compat:25.1.0] /Users/android-studio/.android/build-cache/2c49c076e18edfa3db00468dda17eb0c39c6061f/output/AndroidManifest.xml:21:5-23:58\n"
                + "MERGED from [com.android.support.constraint:constraint-layout:1.0.0-beta4] /Users/android-studio/.android/build-cache/0ce0d5454d1fa55b883c367e8f21c424817b47bb/output/AndroidManifest.xml:5:5-7:41\n"
                + "\ttools:overrideLibrary\n"
                + "\t\tADDED from [com.android.support:appcompat-v7:25.1.0] /Users/android-studio/.android/build-cache/464951d17cb0cdd62cb8c51961d7fe17aa117c02/output/AndroidManifest.xml:23:9-75\n"
                + "\tandroid:targetSdkVersion\n"
                + "\t\tINJECTED from ${ROOT}/app/src/main/AndroidManifest.xml\n"
                + "\t\tINJECTED from ${ROOT}/app/src/main/AndroidManifest.xml\n"
                + "\tandroid:minSdkVersion\n"
                + "\t\tINJECTED from ${ROOT}/app/src/main/AndroidManifest.xml\n"
                + "\t\tINJECTED from ${ROOT}/app/src/main/AndroidManifest.xml\n";
        blameLog = blameLog.replace("${ROOT}", root.getPath());

        File blameFile = new TestFile()
                .to("app/build/outputs/logs/manifest-merger-debug-report.txt")
                .withSource(blameLog)
                .createFile(root);

        BlameFile file = BlameFile.parse(blameFile);
        assertThat(file).isNotNull();

        String xml = Files.toString(mergedManifest, Charsets.UTF_8);
        Document document = XmlUtils.parseDocumentSilently(xml, true);
        assertThat(document).isNotNull();

        LintClient client = new TestLintClient();
        BlameFile.XmlVisitor.accept(document, new BlameFile.XmlVisitor() {
            @Override
            public boolean visitAttribute(Attr attribute) {
                Pair<File,Node> source = file.findSourceAttribute(client, attribute);
                if (source == null) {
                    String name = attribute.getName();
                    // Injected
                    if (name.equals("android:versionCode")
                            || name.equals("android:versionName")
                            || name.equals("android:minSdkVersion")
                            || name.equals("android:targetSdkVersion")) {
                        return false;
                    }
                } else {
                    assertThat(source.getFirst()).isEqualTo(sourceManifest);
                }
                assertThat(source).named(attribute.getName()).isNotNull();
                return false;
            }

            @Override
            public boolean visitTag(Element element, String tag) {
                Pair<File,Node> source = file.findSourceElement(client, element);

                // Injected
                if (tag.equals("uses-sdk")) {
                    return false;
                }

                assertThat(source).named(tag).isNotNull();
                assertThat(source.getFirst()).isEqualTo(sourceManifest);
                return false;
            }
        });
    }
}