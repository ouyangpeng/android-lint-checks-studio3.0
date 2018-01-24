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

import static com.android.utils.XmlUtils.toXmlAttributeValue;

import com.android.tools.lint.detector.api.Detector;

public class VectorPathDetectorTest extends AbstractCheckTest {
    private static final String SHORT_PATH = ""
            + "M 37.8337860107,-40.3974914551 c 0,0 -35.8077850342,31.5523681641 -35.8077850342,31.5523681641 "
            + "c 0,0 40.9884796143,40.9278411865 40.9884796143,40.9278411865 c 0,0 -2.61700439453,2.0938873291 -2.61700439453,2.0938873291 "
            + "c 0,0 -41.1884460449,-40.9392852783 -41.1884460449,-40.9392852783 c 0,0 -34.6200408936,25.4699249268 -34.6200408936,25.4699249268 "
            + "c 0,0 55.9664764404,69.742401123 55.9664764404,69.742401123 c 0,0 73.2448120117,-59.1047973633 73.2448120117,-59.1047973633 "
            + "c 0,0 -55.9664916992,-69.7423400879 -55.9664916992,-69.7423400879 Z ";

    private static final String LONG_PATH = SHORT_PATH + SHORT_PATH + SHORT_PATH;

    @Override
    protected Detector getDetector() {
        return new VectorPathDetector();
    }

    public void test() {
        lint().files(
                xml("res/drawable/my_vector.xml", ""
                        + "<vector\n"
                        + "  xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "  android:name=\"root\"\n"
                        + "  android:width=\"48dp\"\n"
                        + "  android:height=\"48dp\"\n"
                        + "  android:tint=\"?attr/colorControlNormal\"\n"
                        + "  android:viewportHeight=\"48\"\n"
                        + "  android:viewportWidth=\"48\">\n"
                        + "\n"
                        + "  <group\n"
                        + "    android:translateX=\"-1.21595\"\n"
                        + "    android:translateY=\"6.86752\">\n"
                        + "\n"
                        + "    <clip-path\n"
                        + "      android:name=\"maskClipPath\"\n"
                        + "      android:pathData=\"@string/airplane_mask_clip_path_enabled\"/>\n"
                        + "\n"
                        + "    <path\n"
                        + "      android:name=\"crossPath\"\n"
                        + "      android:pathData=\"@string/airplane_cross_path\"\n"
                        + "      android:strokeColor=\"@android:color/white\"\n"
                        + "      android:strokeWidth=\"3.5\"\n"
                        + "      android:trimPathEnd=\"0\"/>\n"
                        + "\n"
                        + "    <group\n"
                        + "      android:translateX=\"23.481\"\n"
                        + "      android:translateY=\"18.71151\">\n"
                        + "      <path\n"
                        + "        android:fillColor=\"@android:color/white\"\n"
                        + "        android:pathData=\"@string/airplane_path\"/>\n"
                        + "    </group>\n"
                        + "    <group\n"
                        + "      android:translateX=\"23.481\"\n"
                        + "      android:translateY=\"18.71151\">\n"
                        + "      <path\n"
                        + "        android:fillColor=\"@android:color/white\"\n"
                        + "        android:pathData=\"" + toXmlAttributeValue(LONG_PATH) + "\"/>\n"
                        + "    </group>\n"
                        + "  </group>\n"
                        + "</vector>"),
                xml("res/values/paths.xml", ""
                        + "<resources>\n"
                        + "\n"
                        + "  <string name=\"airplane_path\">\n"
                        + SHORT_PATH
                        + "  </string>\n"
                        + "  <string name=\"airplane_cross_path\">" + SHORT_PATH + "</string>\n"
                        + "  <string name=\"airplane_mask_clip_path_disabled\">\n"
                        + LONG_PATH
                        + "  </string>\n"
                        + "  <string name=\"airplane_mask_clip_path_enabled\">\n"
                        + LONG_PATH
                        + "  </string>\n"
                        + "\n"
                        + "</resources>"),
                // Interpolator: don't flag long paths here
                xml("res/interpolator/my_interpolator.xml", ""
                        + "<pathInterpolator\n"
                        + "  xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "  android:pathData=\"" + toXmlAttributeValue(LONG_PATH) + "\"/>"))
                .incremental("res/drawable/my_vector.xml")
                .run()
                .maxLineLength(100)
                .expect(""
                        + "res/drawable/my_vector.xml:16: Warning: Very long vector path (1626 characters), which is bad for p…\n"
                        + "      android:pathData=\"@string/airplane_mask_clip_path_enabled\"/>\n"
                        + "                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "res/drawable/my_vector.xml:37: Warning: Very long vector path (1623 characters), which is bad for p…\n"
                        + "        android:pathData=\"M 37.8337860107,-40.3974914551 c 0,0 -35.8077850342,31.5523681641 -35.807…\n"
                        + "                          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~…\n"
                        + "0 errors, 2 warnings\n");
    }

    public void testNoWarningWhenGradlePluginGeneratedImage() {
        lint().files(
                xml("res/drawable/my_vector.xml", ""
                        + "<vector\n"
                        + "  xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "  android:name=\"root\" android:width=\"48dp\" android:height=\"48dp\"\n"
                        + "  android:viewportHeight=\"48\" android:viewportWidth=\"48\">\n"
                        + "  <path\n"
                        + "    android:fillColor=\"@android:color/white\"\n"
                        + "    android:pathData=\"" + toXmlAttributeValue(LONG_PATH) + "\"/>\n"
                        + "</vector>"),
                gradle(""
                        + "buildscript {\n"
                        + "    dependencies {\n"
                        + "        classpath 'com.android.tools.build:gradle:2.2.0'\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expectClean()
                .expectWarningCount(0) // redundant - just testing this infrastructure method
                .expectErrorCount(0);
    }

    public void testWarningWhenUsingSupportVectors() {
        lint().files(
                xml("res/drawable/my_vector.xml", ""
                        + "<vector\n"
                        + "  xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "  android:name=\"root\" android:width=\"48dp\" android:height=\"48dp\"\n"
                        + "  android:viewportHeight=\"48\" android:viewportWidth=\"48\">\n"
                        + "  <path\n"
                        + "    android:fillColor=\"@android:color/white\"\n"
                        + "    android:pathData=\"" + toXmlAttributeValue(LONG_PATH) + "\"/>\n"
                        + "</vector>"),
                gradle(""
                        + "buildscript {\n"
                        + "    dependencies {\n"
                        + "        classpath 'com.android.tools.build:gradle:2.0.0'\n"
                        + "    }\n"
                        + "}\n"
                        + "android.defaultConfig.vectorDrawables.useSupportLibrary = true\n"))
                .run()
                .maxLineLength(100)
                .expectWarningCount(1)
                .expectErrorCount(0)
                .expect(""
                        + "res/drawable/my_vector.xml:7: Warning: Very long vector path (1623 characters), which is bad for pe…\n"
                        + "    android:pathData=\"M 37.8337860107,-40.3974914551 c 0,0 -35.8077850342,31.5523681641 -35.8077850…\n"
                        + "                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~…\n"
                        + "0 errors, 1 warnings\n");
    }

    public void testInvalidScientificNotation() {
        // Regression test for https://code.google.com/p/android/issues/detail?id=254147
        lint().files(
                xml("res/drawable/my_vector.xml", ""
                        + "<vector\n"
                        + "  xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "  android:name=\"root\" android:width=\"48dp\" android:height=\"48dp\"\n"
                        + "  android:viewportHeight=\"48\" android:viewportWidth=\"48\">\n"
                        + "  <path\n"
                        + "    android:fillColor=\"@android:color/white\"\n"
                        + "    android:pathData=\"m 1.05e-4,2.75448\" />\n"
                        + "</vector>"))
                .incremental("res/drawable/my_vector.xml")
                .run()
                .expect(""
                        + "res/drawable/my_vector.xml:7: Error: Avoid scientific notation (1.05e-4) in vector paths because it can lead to crashes on some devices. Use 0.000105 instead. [InvalidVectorPath]\n"
                        + "    android:pathData=\"m 1.05e-4,2.75448\" />\n"
                        + "                        ~~~~~~~\n"
                        + "1 errors, 0 warnings\n")
                .expectFixDiffs(""
                        + "Fix for res/drawable/my_vector.xml line 6: Replace with 0.000105:\n"
                        + "@@ -7 +7\n"
                        + "-     android:pathData=\"m 1.05e-4,2.75448\" />\n"
                        + "+     android:pathData=\"m 0.000105,2.75448\" />\n");
    }

    public void testInvalidScientificNotationWithResources() {
        lint().files(
                xml("res/drawable/my_vector.xml", ""
                        + "<vector\n"
                        + "  xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "  android:name=\"root\" android:width=\"48dp\" android:height=\"48dp\"\n"
                        + "  android:viewportHeight=\"48\" android:viewportWidth=\"48\">\n"
                        + "  <path\n"
                        + "    android:fillColor=\"@android:color/white\"\n"
                        + "    android:pathData=\"@string/my_vector_path\" />\n"
                        + "</vector>"),
                xml("res/values/strings.xml", ""
                        + "<resources>\n"
                        + "  <string name=\"my_vector_path\">m1.05e-4,2.75448</string>\n"
                        + "</resources>"))
                .incremental("res/drawable/my_vector.xml")
                .run()
                .expect(""
                        + "res/drawable/my_vector.xml:7: Error: Avoid scientific notation (1.05e-4) in vector paths because it can lead to crashes on some devices. Use 0.000105 instead. [InvalidVectorPath]\n"
                        + "    android:pathData=\"@string/my_vector_path\" />\n"
                        + "                      ~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n")
                .expectFixDiffs("");
    }

    public void testInvalidFloatPattern() {
        // Regression test for https://issuetracker.google.com/issues/27515021
        lint().files(
                xml("res/drawable/my_vector.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:width=\"24dp\"\n"
                        + "    android:height=\"24dp\"\n"
                        + "    android:viewportHeight=\"24.0\"\n"
                        + "    android:viewportWidth=\"24.0\">\n"
                        + "    <path\n"
                        + "        android:fillColor=\"#000000\"\n"
                        + "        android:pathData=\"M18 8c0-3.31-2.69-6-6-6s-6 2.69-6 6c0 4.5 6 11 6 11s6-6.5 6-11zm-8 0c0-1.1.9-2 2-2s2 .9 2 2-.89 2-2 2c-1.1 0-2-.9-2-2zm-5 12v2h14v-2h-14z\" />\n"
                        + "\n"
                        + "    <path\n"
                        + "        android:pathData=\"M0 0h24v24h-24z\" />\n"
                        + "\n"
                        + "</vector>"))
                .incremental("res/drawable/my_vector.xml")
                .run()
                .expect(""
                        + "res/drawable/my_vector.xml:9: Error: Use -0.89 instead of -.89 to avoid crashes on some devices [InvalidVectorPath]\n"
                        + "        android:pathData=\"M18 8c0-3.31-2.69-6-6-6s-6 2.69-6 6c0 4.5 6 11 6 11s6-6.5 6-11zm-8 0c0-1.1.9-2 2-2s2 .9 2 2-.89 2-2 2c-1.1 0-2-.9-2-2zm-5 12v2h14v-2h-14z\" />\n"
                        + "                                                                                                                     ~~~~\n"
                        + "res/drawable/my_vector.xml:9: Error: Use -0.9 instead of -.9 to avoid crashes on some devices [InvalidVectorPath]\n"
                        + "        android:pathData=\"M18 8c0-3.31-2.69-6-6-6s-6 2.69-6 6c0 4.5 6 11 6 11s6-6.5 6-11zm-8 0c0-1.1.9-2 2-2s2 .9 2 2-.89 2-2 2c-1.1 0-2-.9-2-2zm-5 12v2h14v-2h-14z\" />\n"
                        + "                                                                                                                                        ~~~\n"
                        + "res/drawable/my_vector.xml:9: Error: Use 0.9 instead of .9 to avoid crashes on some devices [InvalidVectorPath]\n"
                        + "        android:pathData=\"M18 8c0-3.31-2.69-6-6-6s-6 2.69-6 6c0 4.5 6 11 6 11s6-6.5 6-11zm-8 0c0-1.1.9-2 2-2s2 .9 2 2-.89 2-2 2c-1.1 0-2-.9-2-2zm-5 12v2h14v-2h-14z\" />\n"
                        + "                                                                                                    ~~\n"
                        + "res/drawable/my_vector.xml:9: Error: Use 0.9 instead of .9 to avoid crashes on some devices [InvalidVectorPath]\n"
                        + "        android:pathData=\"M18 8c0-3.31-2.69-6-6-6s-6 2.69-6 6c0 4.5 6 11 6 11s6-6.5 6-11zm-8 0c0-1.1.9-2 2-2s2 .9 2 2-.89 2-2 2c-1.1 0-2-.9-2-2zm-5 12v2h14v-2h-14z\" />\n"
                        + "                                                                                                               ~~\n"
                        + "4 errors, 0 warnings\n")
                .expectFixDiffs(""
                        + "Fix for res/drawable/my_vector.xml line 8: Replace with -0.89:\n"
                        + "@@ -9 +9\n"
                        + "-         android:pathData=\"M18 8c0-3.31-2.69-6-6-6s-6 2.69-6 6c0 4.5 6 11 6 11s6-6.5 6-11zm-8 0c0-1.1.9-2 2-2s2 .9 2 2-.89 2-2 2c-1.1 0-2-.9-2-2zm-5 12v2h14v-2h-14z\" />\n"
                        + "+         android:pathData=\"M18 8c0-3.31-2.69-6-6-6s-6 2.69-6 6c0 4.5 6 11 6 11s6-6.5 6-11zm-8 0c0-1.1.9-2 2-2s2 .9 2 2-0.89 2-2 2c-1.1 0-2-.9-2-2zm-5 12v2h14v-2h-14z\" />\n"
                        + "Fix for res/drawable/my_vector.xml line 8: Replace with -0.9:\n"
                        + "@@ -9 +9\n"
                        + "-         android:pathData=\"M18 8c0-3.31-2.69-6-6-6s-6 2.69-6 6c0 4.5 6 11 6 11s6-6.5 6-11zm-8 0c0-1.1.9-2 2-2s2 .9 2 2-.89 2-2 2c-1.1 0-2-.9-2-2zm-5 12v2h14v-2h-14z\" />\n"
                        + "+         android:pathData=\"M18 8c0-3.31-2.69-6-6-6s-6 2.69-6 6c0 4.5 6 11 6 11s6-6.5 6-11zm-8 0c0-1.1.9-2 2-2s2 .9 2 2-.89 2-2 2c-1.1 0-2-0.9-2-2zm-5 12v2h14v-2h-14z\" />\n"
                        + "Fix for res/drawable/my_vector.xml line 8: Replace with 0.9:\n"
                        + "@@ -9 +9\n"
                        + "-         android:pathData=\"M18 8c0-3.31-2.69-6-6-6s-6 2.69-6 6c0 4.5 6 11 6 11s6-6.5 6-11zm-8 0c0-1.1.9-2 2-2s2 .9 2 2-.89 2-2 2c-1.1 0-2-.9-2-2zm-5 12v2h14v-2h-14z\" />\n"
                        + "+         android:pathData=\"M18 8c0-3.31-2.69-6-6-6s-6 2.69-6 6c0 4.5 6 11 6 11s6-6.5 6-11zm-8 0c0-1.10.9-2 2-2s2 .9 2 2-.89 2-2 2c-1.1 0-2-.9-2-2zm-5 12v2h14v-2h-14z\" />\n"
                        + "Fix for res/drawable/my_vector.xml line 8: Replace with 0.9:\n"
                        + "@@ -9 +9\n"
                        + "-         android:pathData=\"M18 8c0-3.31-2.69-6-6-6s-6 2.69-6 6c0 4.5 6 11 6 11s6-6.5 6-11zm-8 0c0-1.1.9-2 2-2s2 .9 2 2-.89 2-2 2c-1.1 0-2-.9-2-2zm-5 12v2h14v-2h-14z\" />\n"
                        + "+         android:pathData=\"M18 8c0-3.31-2.69-6-6-6s-6 2.69-6 6c0 4.5 6 11 6 11s6-6.5 6-11zm-8 0c0-1.1.9-2 2-2s2 0.9 2 2-.89 2-2 2c-1.1 0-2-.9-2-2zm-5 12v2h14v-2h-14z\" />\n");
    }

    public void testValidFloatPattern() {
        // Regression test for https://issuetracker.google.com/issues/27515021
        // (testing valid data)
        lint().files(
                xml("res/drawable/my_vector.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:width=\"24dp\"\n"
                        + "    android:height=\"24dp\"\n"
                        + "    android:viewportWidth=\"24\"\n"
                        + "    android:viewportHeight=\"24\">\n"
                        + "\n"
                        + "    <path\n"
                        + "        android:fillColor=\"#000000\"\n"
                        + "        android:pathData=\"M12 2c-3.87 0-7 3.13-7 7 0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0\n"
                        + "9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z\" />\n"
                        + "    <path\n"
                        + "        android:pathData=\"M0 0h24v24h-24z\" />\n"
                        + "</vector>"))
                .incremental("res/drawable/my_vector.xml")
                .run()
                .expectClean();
    }

    public void testValidMultipleLineOfPathDataPattern() {
        // Regression test for https://issuetracker.google.com/issues/27515021
        // (testing valid data)
        lint().files(
                xml("res/drawable/my_vector.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:height=\"16dp\"\n"
                        + "    android:viewportHeight=\"16\"\n"
                        + "    android:viewportWidth=\"16\"\n"
                        + "    android:width=\"16dp\">\n"
                        + "\n"
                        + "    <path\n"
                        + "        android:fillColor=\"#FFFF00FF\"\n"
                        + "        android:pathData=\"M9.69758952,10.4615385 L13.6123406,10.4615385 C14.2852173,10.4615385\n"
                        + "\t14.8461538,9.9121408 14.8461538,9.23442444 L14.8461538,1.84249863\n"
                        + "C14.8461538,1.17296776 14.2937568,0.615384615 13.6123406,0.615384615\n"
                        + " \tL1.31073636,0.615384615 C0.637859638,0.615384615 0.0769230769,1.16478227\n"
                        + "\n"
                        + "0.0769230769,1.84249863\tL0.0769230769,9.23442444 C0.0769230769,9.90395531\n"
                        + " 0.629320101,10.4615385 1.31073636,10.4615385 L5.1043961,10.4615385\n"
                        + "\t C5.12507441,10.5113515 5.16340706,10.5657803 5.2204207,10.622794\n"
                        + "\r\n"
                        + "L6.96901704,12.3713903 C7.21238046,12.6147537 7.59317013,12.6094967\t\n"
                        + "7.83127651,12.3713903 L9.57987285,10.622794 C9.63729929,10.5653675\n"
                        + "\t9.676186,10.5110368 9.69758952,10.4615385 Z\" />\n"
                        + "</vector>"))
                .incremental("res/drawable/my_vector.xml")
                .run()
                .expectClean();
    }
}