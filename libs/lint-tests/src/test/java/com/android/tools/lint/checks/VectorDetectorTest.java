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
import org.intellij.lang.annotations.Language;

@SuppressWarnings("javadoc")
public class VectorDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new VectorDetector();
    }

    @Language("XML")
    private static final String VECTOR = ""
            + "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "        android:height=\"76dp\"\n"
            + "        android:width=\"76dp\"\n"
            + "        android:viewportHeight=\"48\"\n"
            + "        android:viewportWidth=\"48\"\n"
            + "        android:autoMirrored=\"true\"\n"
            + "        android:tint=\"?attr/colorControlActivated\">\n"
            + "\n"
            + "    <clip-path />\n" // couldn't find any examples
            + "\n"
            + "    <group\n"
            + "        android:name=\"root\"\n"
            + "        android:translateX=\"24.0\"\n"
            + "        android:translateY=\"24.0\" >\n"
            + "        <path\n"
            + "            android:name=\"progressBar\"\n"
            + "            android:fillColor=\"#00000000\"\n"
            + "            android:pathData=\"M0, 0 m 0, -19 a 19,19 0 1,1 0,38 a 19,19 0 1,1 0,-38\"\n"
            + "            android:strokeColor=\"@color/white\"\n"
            + "            android:strokeLineCap=\"square\"\n"
            + "            android:strokeLineJoin=\"miter\"\n"
            + "            android:strokeWidth=\"4\"\n"
            + "            android:trimPathEnd=\"0\"\n"
            + "            android:trimPathOffset=\"0\"\n"
            + "            android:trimPathStart=\"0\" />\n"
            + "    </group>\n"
            + "\n"
            + "</vector>";

    public void testWarn() throws Exception {
        String expected = ""
                + "res/drawable/foo.xml:6: Warning: This attribute is not supported in images generated from this vector icon for API < 21; check generated icon to make sure it looks acceptable [VectorRaster]\n"
                + "        android:autoMirrored=\"true\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~\n"
                + "res/drawable/foo.xml:7: Warning: Resource references will not work correctly in images generated for this vector icon for API < 21; check generated icon to make sure it looks acceptable [VectorRaster]\n"
                + "        android:tint=\"?attr/colorControlActivated\">\n"
                + "                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/drawable/foo.xml:9: Warning: This tag is not supported in images generated from this vector icon for API < 21; check generated icon to make sure it looks acceptable [VectorRaster]\n"
                + "    <clip-path />\n"
                + "    ~~~~~~~~~~~~~\n"
                + "res/drawable/foo.xml:11: Warning: Update Gradle plugin version to 1.5+ to correctly handle <group> tags in generated bitmaps [VectorRaster]\n"
                + "    <group\n"
                + "    ^\n"
                + "res/drawable/foo.xml:19: Warning: Resource references will not work correctly in images generated for this vector icon for API < 21; check generated icon to make sure it looks acceptable [VectorRaster]\n"
                + "            android:strokeColor=\"@color/white\"\n"
                + "                                 ~~~~~~~~~~~~\n"
                + "res/drawable/foo.xml:23: Warning: This attribute is not supported in images generated from this vector icon for API < 21; check generated icon to make sure it looks acceptable [VectorRaster]\n"
                + "            android:trimPathEnd=\"0\"\n"
                + "            ~~~~~~~~~~~~~~~~~~~\n"
                + "res/drawable/foo.xml:24: Warning: This attribute is not supported in images generated from this vector icon for API < 21; check generated icon to make sure it looks acceptable [VectorRaster]\n"
                + "            android:trimPathOffset=\"0\"\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/drawable/foo.xml:25: Warning: This attribute is not supported in images generated from this vector icon for API < 21; check generated icon to make sure it looks acceptable [VectorRaster]\n"
                + "            android:trimPathStart=\"0\" />\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 8 warnings\n";

        //noinspection all // Sample code
        lint().files(
                manifest().minSdk(14),
                xml("res/drawable/foo.xml", VECTOR),
                gradle(""
                        + "buildscript {\n"
                        + "    dependencies {\n"
                        + "        classpath 'com.android.tools.build:gradle:1.4.0-alpha2'\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expect(expected);
    }

    public void testNoWarningsWithMinSdk21() throws Exception {
        //noinspection all // Sample code
        lint().files(
                manifest().minSdk(21),
                xml("res/drawable/foo.xml", VECTOR),
                gradle(""
                        + "buildscript {\n"
                        + "    dependencies {\n"
                        + "        classpath 'com.android.tools.build:gradle:1.4.0-alpha2'\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expectClean();
    }

    public void testNoWarningsInV21Folder() throws Exception {
        lint().files(
                manifest().minSdk(14),
                xml("res/drawable-v21/foo.xml", VECTOR),
                gradle(""
                        + "buildscript {\n"
                        + "    dependencies {\n"
                        + "        classpath 'com.android.tools.build:gradle:1.4.0-alpha2'\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expectClean();
    }

    public void testNoGroupWarningWithPlugin15() throws Exception {
        lint().files(
                manifest().minSdk(14),
                xml("res/drawable/foo.xml", ""
                        + "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "        android:height=\"76dp\"\n"
                        + "        android:width=\"76dp\"\n"
                        + "        android:viewportHeight=\"48\"\n"
                        + "        android:viewportWidth=\"48\">\n"
                        + "\n"
                        + "    <group />"
                        + "\n"
                        + "</vector>"
                        + ""),
                gradle(""
                        + "buildscript {\n"
                        + "    dependencies {\n"
                        + "        classpath 'com.android.tools.build:gradle:1.5.0-alpha1'\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expectClean();
    }

    public void testNoWarningsWithSupportLibVectors() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=206005
        lint().files(
                manifest().minSdk(14),
                xml("res/drawable/foo.xml", VECTOR),
                gradle(""
                        + "buildscript {\n"
                        + "    dependencies {\n"
                        + "        classpath 'com.android.tools.build:gradle:2.0.0'\n"
                        + "    }\n"
                        + "}\n"
                        + "android.defaultConfig.vectorDrawables.useSupportLibrary = true\n"))
                .run()
                .expectClean();
    }
}
