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

package com.android.tools.lint.checks;

import com.android.tools.lint.detector.api.Detector;
import org.intellij.lang.annotations.Language;

/**
 * Tests for {@link VectorDrawableCompatDetector}.
 */
public class VectorDrawableCompatDetectorTest extends AbstractCheckTest {

    @Language("XML")
    private static final String VECTOR =
            "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "        android:height=\"256dp\"\n"
            + "        android:width=\"256dp\"\n"
            + "        android:viewportWidth=\"32\"\n"
            + "        android:viewportHeight=\"32\">\n"
            + "    <path android:fillColor=\"#8fff\"\n"
            + "          android:pathData=\"M20.5,9.5\n"
            + "                        c-1.955,0,-3.83,1.268,-4.5,3\n"
            + "                        c-0.67,-1.732,-2.547,-3,-4.5,-3\n"
            + "                        C8.957,9.5,7,11.432,7,14\n"
            + "                        c0,3.53,3.793,6.257,9,11.5\n"
            + "                        c5.207,-5.242,9,-7.97,9,-11.5\n"
            + "                        C25,11.432,23.043,9.5,20.5,9.5z\" />\n"
            + "</vector>\n";

    @Language("XML")
    private static final String LAYOUT_SRC =
            "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "                xmlns:app=\"http://schemas.android.com/apk/res-auto\">\n"
            + "    <ImageView android:src=\"@drawable/foo\" />\n"
            + "</RelativeLayout>\n";

    @Language("XML")
    private static final String LAYOUT_SRC_COMPAT =
            "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "                xmlns:app=\"http://schemas.android.com/apk/res-auto\">\n"
            + "    <ImageView app:srcCompat=\"@drawable/foo\" />\n"
            + "</RelativeLayout>\n";

    @Override
    protected Detector getDetector() {
        return new VectorDrawableCompatDetector();
    }

    public void testSrcCompat() throws Exception {
        String expected = ""
                + "src/main/res/layout/main_activity.xml:3: Error: To use VectorDrawableCompat, you need to set android.defaultConfig.vectorDrawables.useSupportLibrary = true. [VectorDrawableCompat]\n"
                + "    <ImageView app:srcCompat=\"@drawable/foo\" />\n"
                + "               ~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n";

        lint().files(
                xml("src/main/res/drawable/foo.xml", VECTOR),
                xml("src/main/res/layout/main_activity.xml", LAYOUT_SRC_COMPAT),
                gradle(""
                        + "buildscript {\n"
                        + "    dependencies {\n"
                        + "        classpath 'com.android.tools.build:gradle:2.0.0'\n"
                        + "    }\n"
                        + "}\n"
                        + "android.defaultConfig.vectorDrawables.useSupportLibrary = false\n"))
                .run()
                .expect(expected);
    }

    public void testSrcCompat_incremental() throws Exception {
        String expected = ""
                + "src/main/res/layout/main_activity.xml:3: Error: To use VectorDrawableCompat, you need to set android.defaultConfig.vectorDrawables.useSupportLibrary = true. [VectorDrawableCompat]\n"
                + "    <ImageView app:srcCompat=\"@drawable/foo\" />\n"
                + "               ~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n";
        lint().files(
                xml("src/main/res/drawable/foo.xml", VECTOR),
                xml("src/main/res/layout/main_activity.xml", LAYOUT_SRC_COMPAT),
                gradle(""
                        + "buildscript {\n"
                        + "    dependencies {\n"
                        + "        classpath 'com.android.tools.build:gradle:2.0.0'\n"
                        + "    }\n"
                        + "}\n"
                        + "android.defaultConfig.vectorDrawables.useSupportLibrary = false\n"))
                .incremental("src/main/res/layout/main_activity.xml")
                .run()
                .expect(expected);
    }

    public void testSrc() throws Exception {
        String expected = ""
                + "src/main/res/layout/main_activity.xml:3: Error: When using VectorDrawableCompat, you need to use app:srcCompat. [VectorDrawableCompat]\n"
                + "    <ImageView android:src=\"@drawable/foo\" />\n"
                + "               ~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n";
        lint().files(
                xml("src/main/res/drawable/foo.xml", VECTOR),
                xml("src/main/res/layout/main_activity.xml", LAYOUT_SRC),
                gradle(""
                        + "buildscript {\n"
                        + "    dependencies {\n"
                        + "        classpath 'com.android.tools.build:gradle:2.0.0'\n"
                        + "    }\n"
                        + "}\n"
                        + "android.defaultConfig.vectorDrawables.useSupportLibrary = true\n"))
                .run()
                .expect(expected);
    }

    public void testSrc_incremental() throws Exception {
        String expected = ""
                + "src/main/res/layout/main_activity.xml:3: Error: When using VectorDrawableCompat, you need to use app:srcCompat. [VectorDrawableCompat]\n"
                + "    <ImageView android:src=\"@drawable/foo\" />\n"
                + "               ~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n";

        lint().files(
                xml("src/main/res/drawable/foo.xml", VECTOR),
                xml("src/main/res/layout/main_activity.xml", LAYOUT_SRC),
                gradle(""
                        + "buildscript {\n"
                        + "    dependencies {\n"
                        + "        classpath 'com.android.tools.build:gradle:2.0.0'\n"
                        + "    }\n"
                        + "}\n"
                        + "android.defaultConfig.vectorDrawables.useSupportLibrary = true\n"))
                .incremental("src/main/res/layout/main_activity.xml")
                .run()
                .expect(expected);
    }
}