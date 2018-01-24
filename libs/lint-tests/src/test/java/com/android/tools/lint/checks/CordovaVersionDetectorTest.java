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

@SuppressWarnings("javadoc")
public class CordovaVersionDetectorTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new CordovaVersionDetector();
    }

    /** Test to check that a valid version does not cause lint to report an issue. */
    public void testValidCordovaVersionInJS() throws Exception {
        assertEquals("No warnings.",
                lintProject(source("assets/www/cordova.js", ""
                        + ";(function() {\n"
                        + "var PLATFORM_VERSION_BUILD_LABEL = '4.1.1';\n"
                        + "})();\n")
                        ));
    }

    /** Check that vulnerable versions in cordova.js are flagged*/
    public void testVulnerableCordovaVersionInJS() throws Exception {
        assertEquals(""
                + "assets/www/cordova.js: Warning: You are using a vulnerable version of Cordova: 3.7.1 [VulnerableCordovaVersion]\n"
                + "0 errors, 1 warnings\n",
                lintProject(source("assets/www/cordova.js", ""
                        + ";(function() {\n"
                        + "var CORDOVA_JS_BUILD_LABEL = '3.7.1-dev';\n"
                        + "})();\n")
                ));

        assertEquals(""
                + "assets/www/cordova.js: Warning: You are using a vulnerable version of Cordova: 4.0.0 [VulnerableCordovaVersion]\n"
                + "0 errors, 1 warnings\n",
                lintProject(source("assets/www/cordova.js", ""
                        + ";(function() {\n"
                        + "var PLATFORM_VERSION_BUILD_LABEL = '4.0.0';\n"
                        + "})();\n")
                ));
    }

    /** Test to ensure that cordova.js.X.X.android is also detected. */
    public void testVulnerableCordovaVersionInJS2() throws Exception {
        assertEquals(""
                + "assets/www/cordova.js.4.0.android: Warning: You are using a vulnerable version of Cordova: 4.0.0 [VulnerableCordovaVersion]\n"
                + "0 errors, 1 warnings\n",
                lintProject(source("assets/www/cordova.js.4.0.android", ""
                        + ";(function() {\n"
                        + "var CORDOVA_JS_BUILD_LABEL = '4.0.0';"
                        + "})();\n")
                ));
    }

    /** Check whether the detector picks up the version from the Device class. */
    public void testVulnerableCordovaVersionInClasses() throws Exception {
        assertEquals(""
                + "bin/classes/org/apache/cordova/Device.class: Warning: You are using a vulnerable version of Cordova: 2.7.0 [VulnerableCordovaVersion]\n"
                + "0 errors, 1 warnings\n",
                lintProject(
                        base64gzip("bin/classes/org/apache/cordova/Device.class", ""
                                + "H4sIAAAAAAAAAF2PvU7DQBCEZxP/xyTB0FCARAcUNkpDQUQDooqgCEp/vpzM"
                                + "RcaHDsfvFVEgUfAAPBRibSKQaGb35r4d7X5+vX8AmGA/gotRgHEIB7s+Eh97"
                                + "hKE0dmkasVD2RZuKkMxWohFZKaoim9dWV8UlwZvqStdXhP7J6YLgXJulIoxm"
                                + "ulJ366dc2QeRl+wEU1luyWhu1laqW936gxvVaKnSNjpGgJDgTtKL9DyGB59w"
                                + "YGyRiWchH1W2XSj7GSGM//a5z1dK1jjmA1y+iRC249z1uOdU1ohfh1yJq3v2"
                                + "Btp04IDV60yHwfgXPeo8/kt6r+j/Z13WnS5++A0madraRwEAAA==")
                ));
    }

    /**
     * In the presence of both a class as well as the js, detecting the version in the .class wins.
     * In the real world, this won't happen since cordova versions >= 3.x.x have this version
     * declared only in the JS.
     */
    public void testVulnerableVersionInBothJsAndClasses() throws Exception {
        assertEquals(""
                + "bin/classes/org/apache/cordova/Device.class: Warning: You are using a vulnerable version of Cordova: 2.7.0 [VulnerableCordovaVersion]\n"
                + "0 errors, 1 warnings\n",
                lintProject(
                        base64gzip("bin/classes/org/apache/cordova/Device.class", ""
                                + "H4sIAAAAAAAAAF2PvU7DQBCEZxP/xyTB0FCARAcUNkpDQUQDooqgCEp/vpzM"
                                + "RcaHDsfvFVEgUfAAPBRibSKQaGb35r4d7X5+vX8AmGA/gotRgHEIB7s+Eh97"
                                + "hKE0dmkasVD2RZuKkMxWohFZKaoim9dWV8UlwZvqStdXhP7J6YLgXJulIoxm"
                                + "ulJ366dc2QeRl+wEU1luyWhu1laqW936gxvVaKnSNjpGgJDgTtKL9DyGB59w"
                                + "YGyRiWchH1W2XSj7GSGM//a5z1dK1jjmA1y+iRC249z1uOdU1ohfh1yJq3v2"
                                + "Btp04IDV60yHwfgXPeo8/kt6r+j/Z13WnS5++A0madraRwEAAA=="),
                        source("assets/www/cordova.js.4.0.android", ""
                                + ";(function() {\n"
                                + "var CORDOVA_JS_BUILD_LABEL = '4.0.1';\n"
                                + "})();\n")
                ));

    }

    /** Ensure that the version string is read from the CordovaWebView.class. */
    public void testVulnerableVersionInWebView() throws Exception {
        assertEquals(""
                + "bin/classes/org/apache/cordova/CordovaWebView.class: Warning: You are using a vulnerable version of Cordova: 4.0.0 [VulnerableCordovaVersion]\n"
                + "0 errors, 1 warnings\n",
                lintProject(
                        base64gzip("bin/classes/org/apache/cordova/CordovaWebView.class", ""
                                + "yv66vgAAADIAEgoAAwAOBwAPBwAQAQAPQ09SRE9WQV9WRVJTSU9OAQASTGph\n"
                                + "dmEvbGFuZy9TdHJpbmc7AQANQ29uc3RhbnRWYWx1ZQgAEQEABjxpbml0PgEA\n"
                                + "AygpVgEABENvZGUBAA9MaW5lTnVtYmVyVGFibGUBAApTb3VyY2VGaWxlAQAT\n"
                                + "Q29yZG92YVdlYlZpZXcuamF2YQwACAAJAQAhb3JnL2FwYWNoZS9jb3Jkb3Zh\n"
                                + "L0NvcmRvdmFXZWJWaWV3AQAQamF2YS9sYW5nL09iamVjdAEABTQuMC4wACEA\n"
                                + "AgADAAAAAQAZAAQABQABAAYAAAACAAcAAQABAAgACQABAAoAAAAdAAEAAQAA\n"
                                + "AAUqtwABsQAAAAEACwAAAAYAAQAAAAMAAQAMAAAAAgAN")));

    }
}
