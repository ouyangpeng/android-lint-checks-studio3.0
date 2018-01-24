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

import static com.android.SdkConstants.TAG_USES_PERMISSION;
import static com.android.SdkConstants.TAG_USES_PERMISSION_SDK_23;
import static com.android.SdkConstants.TAG_USES_PERMISSION_SDK_M;
import static com.android.tools.lint.checks.AnnotationDetectorTest.SUPPORT_ANNOTATIONS_JAR_BASE64_GZIP;
import static com.android.tools.lint.checks.SupportAnnotationDetector.PERMISSION_ANNOTATION;

import com.android.annotations.Nullable;
import com.android.tools.lint.ExternalAnnotationRepository;
import com.android.tools.lint.ExternalAnnotationRepositoryTest;
import com.android.tools.lint.checks.infrastructure.ProjectDescription;
import com.android.tools.lint.client.api.JavaParser.ResolvedAnnotation;
import com.android.tools.lint.client.api.JavaParser.ResolvedMethod;
import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings("all") // Lots of test sample projects with faulty code
public class SupportAnnotationDetectorTest extends AbstractCheckTest {

    private static final boolean SDK_ANNOTATIONS_AVAILABLE = true;

    @Override
    protected Detector getDetector() {
        return new SupportAnnotationDetector();
    }

    public void testRange() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/RangeTest.java:32: Error: Expected length 5 (was 4) [Range]\n"
                + "        printExact(\"1234\"); // ERROR\n"
                + "                   ~~~~~~\n"
                + "src/test/pkg/RangeTest.java:34: Error: Expected length 5 (was 6) [Range]\n"
                + "        printExact(\"123456\"); // ERROR\n"
                + "                   ~~~~~~~~\n"
                + "src/test/pkg/RangeTest.java:36: Error: Expected length \u2265 5 (was 4) [Range]\n"
                + "        printMin(\"1234\"); // ERROR\n"
                + "                 ~~~~~~\n"
                + "src/test/pkg/RangeTest.java:43: Error: Expected length \u2264 8 (was 9) [Range]\n"
                + "        printMax(\"123456789\"); // ERROR\n"
                + "                 ~~~~~~~~~~~\n"
                + "src/test/pkg/RangeTest.java:45: Error: Expected length \u2265 4 (was 3) [Range]\n"
                + "        printRange(\"123\"); // ERROR\n"
                + "                   ~~~~~\n"
                + "src/test/pkg/RangeTest.java:49: Error: Expected length \u2264 6 (was 7) [Range]\n"
                + "        printRange(\"1234567\"); // ERROR\n"
                + "                   ~~~~~~~~~\n"
                + "src/test/pkg/RangeTest.java:53: Error: Expected size 5 (was 4) [Range]\n"
                + "        printExact(new int[]{1, 2, 3, 4}); // ERROR\n"
                + "                   ~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RangeTest.java:55: Error: Expected size 5 (was 6) [Range]\n"
                + "        printExact(new int[]{1, 2, 3, 4, 5, 6}); // ERROR\n"
                + "                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RangeTest.java:57: Error: Expected size \u2265 5 (was 4) [Range]\n"
                + "        printMin(new int[]{1, 2, 3, 4}); // ERROR\n"
                + "                 ~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RangeTest.java:65: Error: Expected size \u2264 8 (was 9) [Range]\n"
                + "        printMax(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9}); // ERROR\n"
                + "                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RangeTest.java:67: Error: Expected size \u2265 4 (was 3) [Range]\n"
                + "        printRange(new int[] {1,2,3}); // ERROR\n"
                + "                   ~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RangeTest.java:71: Error: Expected size \u2264 6 (was 7) [Range]\n"
                + "        printRange(new int[] {1,2,3,4,5,6,7}); // ERROR\n"
                + "                   ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RangeTest.java:74: Error: Expected size to be a multiple of 3 (was 4 and should be either 3 or 6) [Range]\n"
                + "        printMultiple(new int[] {1,2,3,4}); // ERROR\n"
                + "                      ~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RangeTest.java:75: Error: Expected size to be a multiple of 3 (was 5 and should be either 3 or 6) [Range]\n"
                + "        printMultiple(new int[] {1,2,3,4,5}); // ERROR\n"
                + "                      ~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RangeTest.java:77: Error: Expected size to be a multiple of 3 (was 7 and should be either 6 or 9) [Range]\n"
                + "        printMultiple(new int[] {1,2,3,4,5,6,7}); // ERROR\n"
                + "                      ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RangeTest.java:80: Error: Expected size \u2265 4 (was 3) [Range]\n"
                + "        printMinMultiple(new int[]{1, 2, 3}); // ERROR\n"
                + "                         ~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RangeTest.java:84: Error: Value must be \u2265 4 (was 3) [Range]\n"
                + "        printAtLeast(3); // ERROR\n"
                + "                     ~\n"
                + "src/test/pkg/RangeTest.java:91: Error: Value must be \u2264 7 (was 8) [Range]\n"
                + "        printAtMost(8); // ERROR\n"
                + "                    ~\n"
                + "src/test/pkg/RangeTest.java:93: Error: Value must be \u2265 4 (was 3) [Range]\n"
                + "        printBetween(3); // ERROR\n"
                + "                     ~\n"
                + "src/test/pkg/RangeTest.java:98: Error: Value must be \u2264 7 (was 8) [Range]\n"
                + "        printBetween(8); // ERROR\n"
                + "                     ~\n"
                + "src/test/pkg/RangeTest.java:102: Error: Value must be \u2265 2.5 (was 2.49) [Range]\n"
                + "        printAtLeastInclusive(2.49f); // ERROR\n"
                + "                              ~~~~~\n"
                + "src/test/pkg/RangeTest.java:106: Error: Value must be > 2.5 (was 2.49) [Range]\n"
                + "        printAtLeastExclusive(2.49f); // ERROR\n"
                + "                              ~~~~~\n"
                + "src/test/pkg/RangeTest.java:107: Error: Value must be > 2.5 (was 2.5) [Range]\n"
                + "        printAtLeastExclusive(2.5f); // ERROR\n"
                + "                              ~~~~\n"
                + "src/test/pkg/RangeTest.java:113: Error: Value must be \u2264 7.0 (was 7.1) [Range]\n"
                + "        printAtMostInclusive(7.1f); // ERROR\n"
                + "                             ~~~~\n"
                + "src/test/pkg/RangeTest.java:117: Error: Value must be < 7.0 (was 7.0) [Range]\n"
                + "        printAtMostExclusive(7.0f); // ERROR\n"
                + "                             ~~~~\n"
                + "src/test/pkg/RangeTest.java:118: Error: Value must be < 7.0 (was 7.1) [Range]\n"
                + "        printAtMostExclusive(7.1f); // ERROR\n"
                + "                             ~~~~\n"
                + "src/test/pkg/RangeTest.java:120: Error: Value must be \u2265 2.5 (was 2.4) [Range]\n"
                + "        printBetweenFromInclusiveToInclusive(2.4f); // ERROR\n"
                + "                                             ~~~~\n"
                + "src/test/pkg/RangeTest.java:124: Error: Value must be \u2264 5.0 (was 5.1) [Range]\n"
                + "        printBetweenFromInclusiveToInclusive(5.1f); // ERROR\n"
                + "                                             ~~~~\n"
                + "src/test/pkg/RangeTest.java:126: Error: Value must be > 2.5 (was 2.4) [Range]\n"
                + "        printBetweenFromExclusiveToInclusive(2.4f); // ERROR\n"
                + "                                             ~~~~\n"
                + "src/test/pkg/RangeTest.java:127: Error: Value must be > 2.5 (was 2.5) [Range]\n"
                + "        printBetweenFromExclusiveToInclusive(2.5f); // ERROR\n"
                + "                                             ~~~~\n"
                + "src/test/pkg/RangeTest.java:129: Error: Value must be \u2264 5.0 (was 5.1) [Range]\n"
                + "        printBetweenFromExclusiveToInclusive(5.1f); // ERROR\n"
                + "                                             ~~~~\n"
                + "src/test/pkg/RangeTest.java:131: Error: Value must be \u2265 2.5 (was 2.4) [Range]\n"
                + "        printBetweenFromInclusiveToExclusive(2.4f); // ERROR\n"
                + "                                             ~~~~\n"
                + "src/test/pkg/RangeTest.java:135: Error: Value must be < 5.0 (was 5.0) [Range]\n"
                + "        printBetweenFromInclusiveToExclusive(5.0f); // ERROR\n"
                + "                                             ~~~~\n"
                + "src/test/pkg/RangeTest.java:137: Error: Value must be > 2.5 (was 2.4) [Range]\n"
                + "        printBetweenFromExclusiveToExclusive(2.4f); // ERROR\n"
                + "                                             ~~~~\n"
                + "src/test/pkg/RangeTest.java:138: Error: Value must be > 2.5 (was 2.5) [Range]\n"
                + "        printBetweenFromExclusiveToExclusive(2.5f); // ERROR\n"
                + "                                             ~~~~\n"
                + "src/test/pkg/RangeTest.java:141: Error: Value must be < 5.0 (was 5.0) [Range]\n"
                + "        printBetweenFromExclusiveToExclusive(5.0f); // ERROR\n"
                + "                                             ~~~~\n"
                + "src/test/pkg/RangeTest.java:145: Error: Value must be \u2265 4 (was -7) [Range]\n"
                + "        printBetween(-7); // ERROR\n"
                + "                     ~~\n"
                + "src/test/pkg/RangeTest.java:146: Error: Value must be > 2.5 (was -10.0) [Range]\n"
                + "        printAtLeastExclusive(-10.0f); // ERROR\n"
                + "                              ~~~~~~\n"
                + "src/test/pkg/RangeTest.java:156: Error: Value must be \u2265 -1 (was -2) [Range]\n"
                + "        printIndirect(-2); // ERROR\n"
                + "                      ~~\n"
                + "src/test/pkg/RangeTest.java:157: Error: Value must be \u2264 42 (was 43) [Range]\n"
                + "        printIndirect(43); // ERROR\n"
                + "                      ~~\n"
                + "src/test/pkg/RangeTest.java:158: Error: Expected length 5 (was 7) [Range]\n"
                + "        printIndirectSize(\"1234567\"); // ERROR\n"
                + "                          ~~~~~~~~~\n"
                + "41 errors, 0 warnings\n",

                lintProject(java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import android.support.annotation.FloatRange;\n"
                            + "import android.support.annotation.IntRange;\n"
                            + "import android.support.annotation.Size;\n"
                            + "\n"
                            + "@SuppressWarnings(\"UnusedDeclaration\")\n"
                            + "public class RangeTest {\n"
                            + "    public void printExact(@Size(5) String arg) { System.out.println(arg); }\n"
                            + "    public void printMin(@Size(min=5) String arg) { }\n"
                            + "    public void printMax(@Size(max=8) String arg) { }\n"
                            + "    public void printRange(@Size(min=4,max=6) String arg) { }\n"
                            + "    public void printExact(@Size(5) int[] arg) { }\n"
                            + "    public void printMin(@Size(min=5) int[] arg) { }\n"
                            + "    public void printMax(@Size(max=8) int[] arg) { }\n"
                            + "    public void printRange(@Size(min=4,max=6) int[] arg) { }\n"
                            + "    public void printMultiple(@Size(multiple=3) int[] arg) { }\n"
                            + "    public void printMinMultiple(@Size(min=4,multiple=3) int[] arg) { }\n"
                            + "    public void printAtLeast(@IntRange(from=4) int arg) { }\n"
                            + "    public void printAtMost(@IntRange(to=7) int arg) { }\n"
                            + "    public void printBetween(@IntRange(from=4,to=7) int arg) { }\n"
                            + "    public void printAtLeastInclusive(@FloatRange(from=2.5) float arg) { }\n"
                            + "    public void printAtLeastExclusive(@FloatRange(from=2.5,fromInclusive=false) float arg) { }\n"
                            + "    public void printAtMostInclusive(@FloatRange(to=7) double arg) { }\n"
                            + "    public void printAtMostExclusive(@FloatRange(to=7,toInclusive=false) double arg) { }\n"
                            + "    public void printBetweenFromInclusiveToInclusive(@FloatRange(from=2.5,to=5.0) float arg) { }\n"
                            + "    public void printBetweenFromExclusiveToInclusive(@FloatRange(from=2.5,to=5.0,fromInclusive=false) float arg) { }\n"
                            + "    public void printBetweenFromInclusiveToExclusive(@FloatRange(from=2.5,to=5.0,toInclusive=false) float arg) { }\n"
                            + "    public void printBetweenFromExclusiveToExclusive(@FloatRange(from=2.5,to=5.0,fromInclusive=false,toInclusive=false) float arg) { }\n"
                            + "\n"
                            + "    public void testLength() {\n"
                            + "        printExact(\"1234\"); // ERROR\n"
                            + "        printExact(\"12345\"); // OK\n"
                            + "        printExact(\"123456\"); // ERROR\n"
                            + "\n"
                            + "        printMin(\"1234\"); // ERROR\n"
                            + "        printMin(\"12345\"); // OK\n"
                            + "        printMin(\"123456\"); // OK\n"
                            + "\n"
                            + "        printMax(\"123456\"); // OK\n"
                            + "        printMax(\"1234567\"); // OK\n"
                            + "        printMax(\"12345678\"); // OK\n"
                            + "        printMax(\"123456789\"); // ERROR\n"
                            + "\n"
                            + "        printRange(\"123\"); // ERROR\n"
                            + "        printRange(\"1234\"); // OK\n"
                            + "        printRange(\"12345\"); // OK\n"
                            + "        printRange(\"123456\"); // OK\n"
                            + "        printRange(\"1234567\"); // ERROR\n"
                            + "    }\n"
                            + "\n"
                            + "    public void testSize() {\n"
                            + "        printExact(new int[]{1, 2, 3, 4}); // ERROR\n"
                            + "        printExact(new int[]{1, 2, 3, 4, 5}); // OK\n"
                            + "        printExact(new int[]{1, 2, 3, 4, 5, 6}); // ERROR\n"
                            + "\n"
                            + "        printMin(new int[]{1, 2, 3, 4}); // ERROR\n"
                            + "        printMin(new int[]{1, 2, 3, 4, 5}); // OK\n"
                            + "        printMin(new int[]{1, 2, 3, 4, 5, 6}); // OK\n"
                            + "\n"
                            + "        printMax(new int[]{1, 2, 3, 4, 5, 6}); // OK\n"
                            + "        printMax(new int[]{1, 2, 3, 4, 5, 6, 7}); // OK\n"
                            + "        printMax(new int[]{1, 2, 3, 4, 5, 6, 7, 8}); // OK\n"
                            + "        printMax(new int[]{1, 2, 3, 4, 5, 6, 7, 8}); // OK\n"
                            + "        printMax(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9}); // ERROR\n"
                            + "\n"
                            + "        printRange(new int[] {1,2,3}); // ERROR\n"
                            + "        printRange(new int[] {1,2,3,4}); // OK\n"
                            + "        printRange(new int[] {1,2,3,4,5}); // OK\n"
                            + "        printRange(new int[] {1,2,3,4,5,6}); // OK\n"
                            + "        printRange(new int[] {1,2,3,4,5,6,7}); // ERROR\n"
                            + "\n"
                            + "        printMultiple(new int[] {1,2,3}); // OK\n"
                            + "        printMultiple(new int[] {1,2,3,4}); // ERROR\n"
                            + "        printMultiple(new int[] {1,2,3,4,5}); // ERROR\n"
                            + "        printMultiple(new int[] {1,2,3,4,5,6}); // OK\n"
                            + "        printMultiple(new int[] {1,2,3,4,5,6,7}); // ERROR\n"
                            + "\n"
                            + "        printMinMultiple(new int[] {1,2,3,4,5,6}); // OK\n"
                            + "        printMinMultiple(new int[]{1, 2, 3}); // ERROR\n"
                            + "    }\n"
                            + "\n"
                            + "    public void testIntRange() {\n"
                            + "        printAtLeast(3); // ERROR\n"
                            + "        printAtLeast(4); // OK\n"
                            + "        printAtLeast(5); // OK\n"
                            + "\n"
                            + "        printAtMost(5); // OK\n"
                            + "        printAtMost(6); // OK\n"
                            + "        printAtMost(7); // OK\n"
                            + "        printAtMost(8); // ERROR\n"
                            + "\n"
                            + "        printBetween(3); // ERROR\n"
                            + "        printBetween(4); // OK\n"
                            + "        printBetween(5); // OK\n"
                            + "        printBetween(6); // OK\n"
                            + "        printBetween(7); // OK\n"
                            + "        printBetween(8); // ERROR\n"
                            + "    }\n"
                            + "\n"
                            + "    public void testFloatRange() {\n"
                            + "        printAtLeastInclusive(2.49f); // ERROR\n"
                            + "        printAtLeastInclusive(2.5f); // OK\n"
                            + "        printAtLeastInclusive(2.6f); // OK\n"
                            + "\n"
                            + "        printAtLeastExclusive(2.49f); // ERROR\n"
                            + "        printAtLeastExclusive(2.5f); // ERROR\n"
                            + "        printAtLeastExclusive(2.501f); // OK\n"
                            + "\n"
                            + "        printAtMostInclusive(6.8f); // OK\n"
                            + "        printAtMostInclusive(6.9f); // OK\n"
                            + "        printAtMostInclusive(7.0f); // OK\n"
                            + "        printAtMostInclusive(7.1f); // ERROR\n"
                            + "\n"
                            + "        printAtMostExclusive(6.9f); // OK\n"
                            + "        printAtMostExclusive(6.99f); // OK\n"
                            + "        printAtMostExclusive(7.0f); // ERROR\n"
                            + "        printAtMostExclusive(7.1f); // ERROR\n"
                            + "\n"
                            + "        printBetweenFromInclusiveToInclusive(2.4f); // ERROR\n"
                            + "        printBetweenFromInclusiveToInclusive(2.5f); // OK\n"
                            + "        printBetweenFromInclusiveToInclusive(3f); // OK\n"
                            + "        printBetweenFromInclusiveToInclusive(5.0f); // OK\n"
                            + "        printBetweenFromInclusiveToInclusive(5.1f); // ERROR\n"
                            + "\n"
                            + "        printBetweenFromExclusiveToInclusive(2.4f); // ERROR\n"
                            + "        printBetweenFromExclusiveToInclusive(2.5f); // ERROR\n"
                            + "        printBetweenFromExclusiveToInclusive(5.0f); // OK\n"
                            + "        printBetweenFromExclusiveToInclusive(5.1f); // ERROR\n"
                            + "\n"
                            + "        printBetweenFromInclusiveToExclusive(2.4f); // ERROR\n"
                            + "        printBetweenFromInclusiveToExclusive(2.5f); // OK\n"
                            + "        printBetweenFromInclusiveToExclusive(3f); // OK\n"
                            + "        printBetweenFromInclusiveToExclusive(4.99f); // OK\n"
                            + "        printBetweenFromInclusiveToExclusive(5.0f); // ERROR\n"
                            + "\n"
                            + "        printBetweenFromExclusiveToExclusive(2.4f); // ERROR\n"
                            + "        printBetweenFromExclusiveToExclusive(2.5f); // ERROR\n"
                            + "        printBetweenFromExclusiveToExclusive(2.51f); // OK\n"
                            + "        printBetweenFromExclusiveToExclusive(4.99f); // OK\n"
                            + "        printBetweenFromExclusiveToExclusive(5.0f); // ERROR\n"
                            + "    }\n"
                            + "\n"
                            + "    public void testNegative() {\n"
                            + "        printBetween(-7); // ERROR\n"
                            + "        printAtLeastExclusive(-10.0f); // ERROR\n"
                            + "    }\n"
                            + "\n"
                            + "    public static final int MINIMUM = -1;\n"
                            + "    public static final int MAXIMUM = 42;\n"
                            + "    public void printIndirect(@IntRange(from = MINIMUM, to = MAXIMUM) int arg) { }\n"
                            + "    public static final int SIZE = 5;\n"
                            + "    public static void printIndirectSize(@Size(SIZE) String foo) { }\n"
                            + "\n"
                            + "    public void testIndirect() {\n"
                            + "        printIndirect(-2); // ERROR\n"
                            + "        printIndirect(43); // ERROR\n"
                            + "        printIndirectSize(\"1234567\"); // ERROR\n"
                            + "    }\n"
                            + "}\n"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testTypeDef() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/IntDefTest.java:31: Error: Must be one of: IntDefTest.STYLE_NORMAL, IntDefTest.STYLE_NO_TITLE, IntDefTest.STYLE_NO_FRAME, IntDefTest.STYLE_NO_INPUT [WrongConstant]\n"
                + "        setStyle(0, 0); // ERROR\n"
                + "                 ~\n"
                + "src/test/pkg/IntDefTest.java:32: Error: Must be one of: IntDefTest.STYLE_NORMAL, IntDefTest.STYLE_NO_TITLE, IntDefTest.STYLE_NO_FRAME, IntDefTest.STYLE_NO_INPUT [WrongConstant]\n"
                + "        setStyle(-1, 0); // ERROR\n"
                + "                 ~~\n"
                + "src/test/pkg/IntDefTest.java:33: Error: Must be one of: IntDefTest.STYLE_NORMAL, IntDefTest.STYLE_NO_TITLE, IntDefTest.STYLE_NO_FRAME, IntDefTest.STYLE_NO_INPUT [WrongConstant]\n"
                + "        setStyle(UNRELATED, 0); // ERROR\n"
                + "                 ~~~~~~~~~\n"
                + "src/test/pkg/IntDefTest.java:34: Error: Must be one of: IntDefTest.STYLE_NORMAL, IntDefTest.STYLE_NO_TITLE, IntDefTest.STYLE_NO_FRAME, IntDefTest.STYLE_NO_INPUT [WrongConstant]\n"
                + "        setStyle(IntDefTest.UNRELATED, 0); // ERROR\n"
                + "                 ~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/IntDefTest.java:35: Error: Flag not allowed here [WrongConstant]\n"
                + "        setStyle(IntDefTest.STYLE_NORMAL|STYLE_NO_FRAME, 0); // ERROR: Not a flag\n"
                + "                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/IntDefTest.java:36: Error: Flag not allowed here [WrongConstant]\n"
                + "        setStyle(~STYLE_NO_FRAME, 0); // ERROR: Not a flag\n"
                + "                 ~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/IntDefTest.java:55: Error: Must be one or more of: IntDefTest.STYLE_NORMAL, IntDefTest.STYLE_NO_TITLE, IntDefTest.STYLE_NO_FRAME, IntDefTest.STYLE_NO_INPUT [WrongConstant]\n"
                + "        setFlags(\"\", UNRELATED); // ERROR\n"
                + "                     ~~~~~~~~~\n"
                + "src/test/pkg/IntDefTest.java:56: Error: Must be one or more of: IntDefTest.STYLE_NORMAL, IntDefTest.STYLE_NO_TITLE, IntDefTest.STYLE_NO_FRAME, IntDefTest.STYLE_NO_INPUT [WrongConstant]\n"
                + "        setFlags(\"\", UNRELATED|STYLE_NO_TITLE); // ERROR\n"
                + "                     ~~~~~~~~~\n"
                + "src/test/pkg/IntDefTest.java:57: Error: Must be one or more of: IntDefTest.STYLE_NORMAL, IntDefTest.STYLE_NO_TITLE, IntDefTest.STYLE_NO_FRAME, IntDefTest.STYLE_NO_INPUT [WrongConstant]\n"
                + "        setFlags(\"\", STYLE_NORMAL|STYLE_NO_TITLE|UNRELATED); // ERROR\n"
                + "                                                 ~~~~~~~~~\n"
                + "src/test/pkg/IntDefTest.java:58: Error: Must be one or more of: IntDefTest.STYLE_NORMAL, IntDefTest.STYLE_NO_TITLE, IntDefTest.STYLE_NO_FRAME, IntDefTest.STYLE_NO_INPUT [WrongConstant]\n"
                + "        setFlags(\"\", 1); // ERROR\n"
                + "                     ~\n"
                + "src/test/pkg/IntDefTest.java:59: Error: Must be one or more of: IntDefTest.STYLE_NORMAL, IntDefTest.STYLE_NO_TITLE, IntDefTest.STYLE_NO_FRAME, IntDefTest.STYLE_NO_INPUT [WrongConstant]\n"
                + "        setFlags(\"\", arg < 0 ? STYLE_NORMAL : UNRELATED); // ERROR\n"
                + "                                              ~~~~~~~~~\n"
                + "src/test/pkg/IntDefTest.java:60: Error: Must be one or more of: IntDefTest.STYLE_NORMAL, IntDefTest.STYLE_NO_TITLE, IntDefTest.STYLE_NO_FRAME, IntDefTest.STYLE_NO_INPUT [WrongConstant]\n"
                + "        setFlags(\"\", arg < 0 ? UNRELATED : STYLE_NORMAL); // ERROR\n"
                + "                               ~~~~~~~~~\n"
                + "src/test/pkg/IntDefTest.java:79: Error: Must be one of: IntDefTest.TYPE_1, IntDefTest.TYPE_2 [WrongConstant]\n"
                + "        setTitle(\"\", UNRELATED_TYPE); // ERROR\n"
                + "                     ~~~~~~~~~~~~~~\n"
                + "src/test/pkg/IntDefTest.java:80: Error: Must be one of: IntDefTest.TYPE_1, IntDefTest.TYPE_2 [WrongConstant]\n"
                + "        setTitle(\"\", \"type2\"); // ERROR\n"
                + "                     ~~~~~~~\n"
                + "src/test/pkg/IntDefTest.java:87: Error: Must be one of: IntDefTest.TYPE_1, IntDefTest.TYPE_2 [WrongConstant]\n"
                + "        setTitle(\"\", type); // ERROR\n"
                + "                     ~~~~\n"
                + "src/test/pkg/IntDefTest.java:92: Error: Must be one or more of: IntDefTest.STYLE_NORMAL, IntDefTest.STYLE_NO_TITLE, IntDefTest.STYLE_NO_FRAME, IntDefTest.STYLE_NO_INPUT [WrongConstant]\n"
                + "        setFlags(\"\", flag); // ERROR\n"
                + "                     ~~~~\n"
                + (SDK_ANNOTATIONS_AVAILABLE ?
                "src/test/pkg/IntDefTest.java:99: Error: Must be one of: View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL, View.LAYOUT_DIRECTION_INHERIT, View.LAYOUT_DIRECTION_LOCALE [WrongConstant]\n"
                + "        view.setLayoutDirection(View.TEXT_DIRECTION_LTR); // ERROR\n"
                + "                                ~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/IntDefTest.java:100: Error: Must be one of: View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL, View.LAYOUT_DIRECTION_INHERIT, View.LAYOUT_DIRECTION_LOCALE [WrongConstant]\n"
                + "        view.setLayoutDirection(0); // ERROR\n"
                + "                                ~\n"
                + "src/test/pkg/IntDefTest.java:101: Error: Flag not allowed here [WrongConstant]\n"
                + "        view.setLayoutDirection(View.LAYOUT_DIRECTION_LTR|View.LAYOUT_DIRECTION_RTL); // ERROR\n"
                + "                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                // Unstable (since the set of constants keep changing for each release; hidden for now
                //+ "src/test/pkg/IntDefTest.java:102: Error: Must be one of: Context.POWER_SERVICE, Context.WINDOW_SERVICE, Context.LAYOUT_INFLATER_SERVICE, Context.ACCOUNT_SERVICE, Context.ACTIVITY_SERVICE, Context.ALARM_SERVICE, Context.NOTIFICATION_SERVICE, Context.ACCESSIBILITY_SERVICE, Context.CAPTIONING_SERVICE, Context.KEYGUARD_SERVICE, Context.LOCATION_SERVICE, Context.SEARCH_SERVICE, Context.SENSOR_SERVICE, Context.STORAGE_SERVICE, Context.WALLPAPER_SERVICE, Context.VIBRATOR_SERVICE, Context.CONNECTIVITY_SERVICE, Context.NETWORK_STATS_SERVICE, Context.WIFI_SERVICE, Context.WIFI_P2P_SERVICE, Context.NSD_SERVICE, Context.AUDIO_SERVICE, Context.FINGERPRINT_SERVICE, Context.MEDIA_ROUTER_SERVICE, Context.TELEPHONY_SERVICE, Context.TELEPHONY_SUBSCRIPTION_SERVICE, Context.CARRIER_CONFIG_SERVICE, Context.TELECOM_SERVICE, Context.CLIPBOARD_SERVICE, Context.INPUT_METHOD_SERVICE, Context.TEXT_SERVICES_MANAGER_SERVICE, Context.APPWIDGET_SERVICE, Context.DROPBOX_SERVICE, Context.DEVICE_POLICY_SERVICE, Context.UI_MODE_SERVICE, Context.DOWNLOAD_SERVICE, Context.NFC_SERVICE, Context.BLUETOOTH_SERVICE, Context.USB_SERVICE, Context.LAUNCHER_APPS_SERVICE, Context.INPUT_SERVICE, Context.DISPLAY_SERVICE, Context.USER_SERVICE, Context.RESTRICTIONS_SERVICE, Context.APP_OPS_SERVICE, Context.CAMERA_SERVICE, Context.PRINT_SERVICE, Context.CONSUMER_IR_SERVICE, Context.TV_INPUT_SERVICE, Context.USAGE_STATS_SERVICE, Context.MEDIA_SESSION_SERVICE, Context.BATTERY_SERVICE, Context.JOB_SCHEDULER_SERVICE, Context.MEDIA_PROJECTION_SERVICE, Context.MIDI_SERVICE, Context.HARDWARE_PROPERTIES_SERVICE [WrongConstant]\n"
                //+ "        context.getSystemService(TYPE_1); // ERROR\n"
                //+ "                                 ~~~~~~\n"
                + "19 errors, 0 warnings\n" :
                "16 errors, 0 warnings\n"),

                lintProject(java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import android.content.Context;\n"
                            + "import android.support.annotation.IntDef;\n"
                            + "import android.support.annotation.StringDef;\n"
                            + "import android.view.View;\n"
                            + "\n"
                            + "import java.lang.annotation.Retention;\n"
                            + "import java.lang.annotation.RetentionPolicy;\n"
                            + "\n"
                            + "@SuppressWarnings(\"UnusedDeclaration\")\n"
                            + "public class IntDefTest {\n"
                            + "    @IntDef({STYLE_NORMAL, STYLE_NO_TITLE, STYLE_NO_FRAME, STYLE_NO_INPUT})\n"
                            + "    @Retention(RetentionPolicy.SOURCE)\n"
                            + "    private @interface DialogStyle {}\n"
                            + "\n"
                            + "    public static final int STYLE_NORMAL = 0;\n"
                            + "    public static final int STYLE_NO_TITLE = 1;\n"
                            + "    public static final int STYLE_NO_FRAME = 2;\n"
                            + "    public static final int STYLE_NO_INPUT = 3;\n"
                            + "    public static final int UNRELATED = 3;\n"
                            + "\n"
                            + "    public void setStyle(@DialogStyle int style, int theme) {\n"
                            + "    }\n"
                            + "\n"
                            + "    public void testIntDef(int arg) {\n"
                            + "        setStyle(STYLE_NORMAL, 0); // OK\n"
                            + "        setStyle(IntDefTest.STYLE_NORMAL, 0); // OK\n"
                            + "        setStyle(arg, 0); // OK (not sure)\n"
                            + "\n"
                            + "        setStyle(0, 0); // ERROR\n"
                            + "        setStyle(-1, 0); // ERROR\n"
                            + "        setStyle(UNRELATED, 0); // ERROR\n"
                            + "        setStyle(IntDefTest.UNRELATED, 0); // ERROR\n"
                            + "        setStyle(IntDefTest.STYLE_NORMAL|STYLE_NO_FRAME, 0); // ERROR: Not a flag\n"
                            + "        setStyle(~STYLE_NO_FRAME, 0); // ERROR: Not a flag\n"
                            + "    }\n"
                            + "    @IntDef(value = {STYLE_NORMAL, STYLE_NO_TITLE, STYLE_NO_FRAME, STYLE_NO_INPUT}, flag=true)\n"
                            + "    @Retention(RetentionPolicy.SOURCE)\n"
                            + "    private @interface DialogFlags {}\n"
                            + "\n"
                            + "    public void setFlags(Object first, @DialogFlags int flags) {\n"
                            + "    }\n"
                            + "\n"
                            + "    public void testFlags(int arg) {\n"
                            + "        setFlags(\"\", -1); // OK\n"
                            + "        setFlags(\"\", 0); // OK\n"
                            + "        setFlags(\"\", STYLE_NORMAL); // OK\n"
                            + "        setFlags(arg, 0); // OK (not sure)\n"
                            + "        setFlags(\"\", IntDefTest.STYLE_NORMAL); // OK\n"
                            + "        setFlags(\"\", STYLE_NORMAL|STYLE_NO_TITLE); // OK\n"
                            + "        setFlags(\"\", STYLE_NORMAL|STYLE_NO_TITLE|STYLE_NO_INPUT); // OK\n"
                            + "        setFlags(\"\", arg < 0 ? STYLE_NORMAL : STYLE_NO_TITLE); // OK\n"
                            + "\n"
                            + "        setFlags(\"\", UNRELATED); // ERROR\n"
                            + "        setFlags(\"\", UNRELATED|STYLE_NO_TITLE); // ERROR\n"
                            + "        setFlags(\"\", STYLE_NORMAL|STYLE_NO_TITLE|UNRELATED); // ERROR\n"
                            + "        setFlags(\"\", 1); // ERROR\n"
                            + "        setFlags(\"\", arg < 0 ? STYLE_NORMAL : UNRELATED); // ERROR\n"
                            + "        setFlags(\"\", arg < 0 ? UNRELATED : STYLE_NORMAL); // ERROR\n"
                            + "    }\n"
                            + "\n"
                            + "    public static final String TYPE_1 = \"type1\";\n"
                            + "    public static final String TYPE_2 = \"type2\";\n"
                            + "    public static final String UNRELATED_TYPE = \"other\";\n"
                            + "\n"
                            + "    @StringDef({TYPE_1, TYPE_2})\n"
                            + "    @Retention(RetentionPolicy.SOURCE)\n"
                            + "    private @interface DialogType {}\n"
                            + "\n"
                            + "    public void setTitle(String title, @DialogType String type) {\n"
                            + "    }\n"
                            + "\n"
                            + "    public void testStringDef(String typeArg) {\n"
                            + "        setTitle(\"\", TYPE_1); // OK\n"
                            + "        setTitle(\"\", TYPE_2); // OK\n"
                            + "        setTitle(\"\", null); // OK\n"
                            + "        setTitle(\"\", typeArg); // OK (unknown)\n"
                            + "        setTitle(\"\", UNRELATED_TYPE); // ERROR\n"
                            + "        setTitle(\"\", \"type2\"); // ERROR\n"
                            + "    }\n"
                            + "\n"
                            + "    public void testFlow() {\n"
                            + "        String type = TYPE_1;\n"
                            + "        setTitle(\"\", type); // OK\n"
                            + "        type = UNRELATED_TYPE;\n"
                            + "        setTitle(\"\", type); // ERROR\n"
                            + "        int flag = 0;\n"
                            + "        flag |= STYLE_NORMAL;\n"
                            + "        setFlags(\"\", flag); // OK\n"
                            + "        flag = UNRELATED;\n"
                            + "        setFlags(\"\", flag); // ERROR\n"
                            + "    }\n"
                            + "\n"
                            + "    public void testExternalAnnotations(View view, Context context) {\n"
                            + "        view.setLayoutDirection(View.LAYOUT_DIRECTION_LTR); // OK\n"
                            + "        context.getSystemService(Context.ALARM_SERVICE); // OK\n"
                            + "\n"
                            + "        view.setLayoutDirection(View.TEXT_DIRECTION_LTR); // ERROR\n"
                            + "        view.setLayoutDirection(0); // ERROR\n"
                            + "        view.setLayoutDirection(View.LAYOUT_DIRECTION_LTR|View.LAYOUT_DIRECTION_RTL); // ERROR\n"
                            + "        //context.getSystemService(TYPE_1); // ERROR\n"
                            + "    }\n"
                            + "}\n"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testTypeDef37324044() {
        // Regression test for issue 37324044
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import java.util.Calendar;\n"
                        + "\n"
                        + "public class IntDefTest {\n"
                        + "    public void test() {\n"
                        + "        Calendar.getInstance().get(Calendar.DAY_OF_MONTH);\n"
                        + "    }\n"
                        + "}\n"),
                mSupportClasspath,
                mSupportJar)
                .run()
                .expectClean();
    }

    public void testColorInt() throws Exception {
        // Needs updated annotations!
        assertEquals((SDK_ANNOTATIONS_AVAILABLE ? ""
                + "src/test/pkg/WrongColor.java:9: Error: Should pass resolved color instead of resource id here: getResources().getColor(R.color.blue) [ResourceAsColor]\n"
                + "        paint2.setColor(R.color.blue);\n"
                + "                        ~~~~~~~~~~~~\n"
                + "src/test/pkg/WrongColor.java:11: Error: Should pass resolved color instead of resource id here: getResources().getColor(R.color.red) [ResourceAsColor]\n"
                + "        textView.setTextColor(R.color.red);\n"
                + "                              ~~~~~~~~~~~\n"
                + "src/test/pkg/WrongColor.java:12: Error: Should pass resolved color instead of resource id here: getResources().getColor(android.R.color.black) [ResourceAsColor]\n"
                + "        textView.setTextColor(android.R.color.black);\n"
                + "                              ~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/WrongColor.java:13: Error: Should pass resolved color instead of resource id here: getResources().getColor(R.color.blue) [ResourceAsColor]\n"
                + "        textView.setTextColor(foo > 0 ? R.color.green : R.color.blue);\n"
                + "                                                        ~~~~~~~~~~~~\n"
                + "src/test/pkg/WrongColor.java:13: Error: Should pass resolved color instead of resource id here: getResources().getColor(R.color.green) [ResourceAsColor]\n"
                + "        textView.setTextColor(foo > 0 ? R.color.green : R.color.blue);\n"
                + "                                        ~~~~~~~~~~~~~\n" : "")
                + "src/test/pkg/WrongColor.java:21: Error: Should pass resolved color instead of resource id here: getResources().getColor(R.color.blue) [ResourceAsColor]\n"
                + "        foo2(R.color.blue);\n"
                + "             ~~~~~~~~~~~~\n"
                + "src/test/pkg/WrongColor.java:20: Error: Expected resource of type color [ResourceType]\n"
                + "        foo1(0xffff0000);\n"
                + "             ~~~~~~~~~~\n"
                + (SDK_ANNOTATIONS_AVAILABLE ? "7 errors, 0 warnings\n" : "2 errors, 0 warnings\n"),

                lintProject(
                        java("src/test/pkg/WrongColor.java", ""
                                + "package test.pkg;\n"
                                + "import android.app.Activity;\n"
                                + "import android.graphics.Paint;\n"
                                + "import android.widget.TextView;\n"
                                + "\n"
                                + "public class WrongColor extends Activity {\n"
                                + "    public void foo(TextView textView, int foo) {\n"
                                + "        Paint paint2 = new Paint();\n"
                                + "        paint2.setColor(R.color.blue);\n"
                                + "        // Wrong\n"
                                + "        textView.setTextColor(R.color.red);\n"
                                + "        textView.setTextColor(android.R.color.black);\n"
                                + "        textView.setTextColor(foo > 0 ? R.color.green : R.color.blue);\n"
                                + "        // OK\n"
                                + "        textView.setTextColor(getResources().getColor(R.color.red));\n"
                                + "        // OK\n"
                                + "        foo1(R.color.blue);\n"
                                + "        foo2(0xffff0000);\n"
                                + "        // Wrong\n"
                                + "        foo1(0xffff0000);\n"
                                + "        foo2(R.color.blue);\n"
                                + "    }\n"
                                + "\n"
                                + "    private void foo1(@android.support.annotation.ColorRes int c) {\n"
                                + "    }\n"
                                + "\n"
                                + "    private void foo2(@android.support.annotation.ColorInt int c) {\n"
                                + "    }\n"
                                + "\n"
                                + "    private static class R {\n"
                                + "        private static class color {\n"
                                + "            public static final int red=0x7f060000;\n"
                                + "            public static final int green=0x7f060001;\n"
                                + "            public static final int blue=0x7f060002;\n"
                                + "        }\n"
                                + "    }\n"
                                + "}\n"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testColorInt2() throws Exception {
        assertEquals(""
                + "src/test/pkg/ColorTest.java:22: Error: Should pass resolved color instead of resource id here: getResources().getColor(actualColor) [ResourceAsColor]\n"
                + "        setColor2(actualColor); // ERROR\n"
                + "                  ~~~~~~~~~~~\n"
                + "src/test/pkg/ColorTest.java:23: Error: Should pass resolved color instead of resource id here: getResources().getColor(getColor1()) [ResourceAsColor]\n"
                + "        setColor2(getColor1()); // ERROR\n"
                + "                  ~~~~~~~~~~~\n"
                + "src/test/pkg/ColorTest.java:16: Error: Expected a color resource id (R.color.) but received an RGB integer [ResourceType]\n"
                + "        setColor1(actualColor); // ERROR\n"
                + "                  ~~~~~~~~~~~\n"
                + "src/test/pkg/ColorTest.java:17: Error: Expected a color resource id (R.color.) but received an RGB integer [ResourceType]\n"
                + "        setColor1(getColor2()); // ERROR\n"
                + "                  ~~~~~~~~~~~\n"
                + "4 errors, 0 warnings\n",

                lintProject(
                        java("src/test/pkg/ColorTest.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.support.annotation.ColorInt;\n"
                                + "import android.support.annotation.ColorRes;\n"
                                + "\n"
                                + "public abstract class ColorTest {\n"
                                + "    @ColorRes\n"
                                + "    public abstract int getColor1();\n"
                                + "    public abstract void setColor1(@ColorRes int color);\n"
                                + "    @ColorInt\n"
                                + "    public abstract int getColor2();\n"
                                + "    public abstract void setColor2(@ColorInt int color);\n"
                                + "\n"
                                + "    public void test1() {\n"
                                + "        int actualColor = getColor2();\n"
                                + "        setColor1(actualColor); // ERROR\n"
                                + "        setColor1(getColor2()); // ERROR\n"
                                + "        setColor1(getColor1()); // OK\n"
                                + "    }\n"
                                + "    public void test2() {\n"
                                + "        int actualColor = getColor1();\n"
                                + "        setColor2(actualColor); // ERROR\n"
                                + "        setColor2(getColor1()); // ERROR\n"
                                + "        setColor2(getColor2()); // OK\n"
                                + "    }\n"
                                + "}\n"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testColorInt3() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=176321

        if (!SDK_ANNOTATIONS_AVAILABLE) {
            return;
        }
        assertEquals(""
                + "src/test/pkg/ColorTest.java:11: Error: Expected a color resource id (R.color.) but received an RGB integer [ResourceType]\n"
                + "        setColor(actualColor);\n"
                + "                 ~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

                lintProject(
                        java("src/test/pkg/ColorTest.java", ""
                                + "package test.pkg;\n"
                                + "import android.content.Context;\n"
                                + "import android.content.res.Resources;\n"
                                + "import android.support.annotation.ColorRes;\n"
                                + "\n"
                                + "public abstract class ColorTest {\n"
                                + "    public abstract void setColor(@ColorRes int color);\n"
                                + "\n"
                                + "    public void test(Context context, @ColorRes int id) {\n"
                                + "        int actualColor = context.getResources().getColor(id, null);\n"
                                + "        setColor(actualColor);\n"
                                + "    }\n"
                                + "}\n"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testPx() throws Exception {
        assertEquals(""
                + "src/test/pkg/PxTest.java:22: Error: Should pass resolved pixel dimension instead of resource id here: getResources().getDimension*(actualSize) [ResourceAsColor]\n"
                + "        setDimension2(actualSize); // ERROR\n"
                + "                      ~~~~~~~~~~\n"
                + "src/test/pkg/PxTest.java:23: Error: Should pass resolved pixel dimension instead of resource id here: getResources().getDimension*(getDimension1()) [ResourceAsColor]\n"
                + "        setDimension2(getDimension1()); // ERROR\n"
                + "                      ~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/PxTest.java:16: Error: Expected a dimension resource id (R.color.) but received a pixel integer [ResourceType]\n"
                + "        setDimension1(actualSize); // ERROR\n"
                + "                      ~~~~~~~~~~\n"
                + "src/test/pkg/PxTest.java:17: Error: Expected a dimension resource id (R.color.) but received a pixel integer [ResourceType]\n"
                + "        setDimension1(getDimension2()); // ERROR\n"
                + "                      ~~~~~~~~~~~~~~~\n"
                + "4 errors, 0 warnings\n",

                lintProject(
                        java("src/test/pkg/PxTest.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.support.annotation.Px;\n"
                                + "import android.support.annotation.DimenRes;\n"
                                + "\n"
                                + "public abstract class PxTest {\n"
                                + "    @DimenRes\n"
                                + "    public abstract int getDimension1();\n"
                                + "    public abstract void setDimension1(@DimenRes int dimension);\n"
                                + "    @Px\n"
                                + "    public abstract int getDimension2();\n"
                                + "    public abstract void setDimension2(@Px int dimension);\n"
                                + "\n"
                                + "    public void test1() {\n"
                                + "        int actualSize = getDimension2();\n"
                                + "        setDimension1(actualSize); // ERROR\n"
                                + "        setDimension1(getDimension2()); // ERROR\n"
                                + "        setDimension1(getDimension1()); // OK\n"
                                + "    }\n"
                                + "    public void test2() {\n"
                                + "        int actualSize = getDimension1();\n"
                                + "        setDimension2(actualSize); // ERROR\n"
                                + "        setDimension2(getDimension1()); // ERROR\n"
                                + "        setDimension2(getDimension2()); // OK\n"
                                + "    }\n"
                                + "}\n"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testResourceType() throws Exception {
        assertEquals(""
                + "src/p1/p2/Flow.java:13: Error: Expected resource of type drawable [ResourceType]\n"
                + "        resources.getDrawable(10); // ERROR\n"
                + "                              ~~\n"
                + "src/p1/p2/Flow.java:18: Error: Expected resource of type drawable [ResourceType]\n"
                + "        resources.getDrawable(R.string.my_string); // ERROR\n"
                + "                              ~~~~~~~~~~~~~~~~~~\n"
                + "src/p1/p2/Flow.java:22: Error: Expected resource of type drawable [ResourceType]\n"
                + "        myMethod(R.string.my_string, null); // ERROR\n"
                + "                 ~~~~~~~~~~~~~~~~~~\n"
                + "src/p1/p2/Flow.java:26: Error: Expected resource of type drawable [ResourceType]\n"
                + "        resources.getDrawable(R.string.my_string); // ERROR\n"
                + "                              ~~~~~~~~~~~~~~~~~~\n"
                + "src/p1/p2/Flow.java:32: Error: Expected resource identifier (R.type.name) [ResourceType]\n"
                + "        myAnyResMethod(50); // ERROR\n"
                + "                       ~~\n"
                + "src/p1/p2/Flow.java:43: Error: Expected resource of type drawable [ResourceType]\n"
                + "        resources.getDrawable(s1); // ERROR\n"
                + "                              ~~\n"
                + "src/p1/p2/Flow.java:50: Error: Expected resource of type drawable [ResourceType]\n"
                + "        resources.getDrawable(MimeTypes.style); // ERROR\n"
                + "                              ~~~~~~~~~~~~~~~\n"
                + "src/p1/p2/Flow.java:60: Error: Expected resource of type drawable [ResourceType]\n"
                + "        resources.getDrawable(MimeTypes.getAnnotatedString()); // Error\n"
                + "                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/p1/p2/Flow.java:68: Error: Expected resource of type drawable [ResourceType]\n"
                + "        myMethod(z, null); // ERROR\n"
                + "                 ~\n"
                + "src/p1/p2/Flow.java:71: Error: Expected resource of type drawable [ResourceType]\n"
                + "        myMethod(w, null); // ERROR\n"
                + "                 ~\n"
                + "10 errors, 0 warnings\n",

                lintProject(
                        java("src/p1/p2/Flow.java", ""
                                + "import android.content.res.Resources;\n"
                                + "import android.support.annotation.DrawableRes;\n"
                                + "import android.support.annotation.StringRes;\n"
                                + "import android.support.annotation.StyleRes;\n"
                                + "\n"
                                + "import java.util.Random;\n"
                                + "\n"
                                + "@SuppressWarnings(\"UnusedDeclaration\")\n"
                                + "public class Flow {\n"
                                + "    public void testLiterals(Resources resources) {\n"
                                + "        resources.getDrawable(0); // OK\n"
                                + "        resources.getDrawable(-1); // OK\n"
                                + "        resources.getDrawable(10); // ERROR\n"
                                + "    }\n"
                                + "\n"
                                + "    public void testConstants(Resources resources) {\n"
                                + "        resources.getDrawable(R.drawable.my_drawable); // OK\n"
                                + "        resources.getDrawable(R.string.my_string); // ERROR\n"
                                + "    }\n"
                                + "\n"
                                + "    public void testLocalAnnotation() {\n"
                                + "        myMethod(R.string.my_string, null); // ERROR\n"
                                + "    }\n"
                                + "\n"
                                + "    private void myMethod(@DrawableRes int arg, Resources resources) {\n"
                                + "        resources.getDrawable(R.string.my_string); // ERROR\n"
                                + "    }\n"
                                + "\n"
                                + "    private void testAnyRes() {\n"
                                + "        myAnyResMethod(R.drawable.my_drawable); // OK\n"
                                + "        myAnyResMethod(R.string.my_string); // OK\n"
                                + "        myAnyResMethod(50); // ERROR\n"
                                + "    }\n"
                                + "\n"
                                + "    private void myAnyResMethod(@android.support.annotation.AnyRes int arg) {\n"
                                + "    }\n"
                                + "\n"
                                + "    public void testFields(String fileExt, Resources resources) {\n"
                                + "        int mimeIconId = MimeTypes.styleAndDrawable;\n"
                                + "        resources.getDrawable(mimeIconId); // OK\n"
                                + "\n"
                                + "        int s1 = MimeTypes.style;\n"
                                + "        resources.getDrawable(s1); // ERROR\n"
                                + "        int s2 = MimeTypes.styleAndDrawable;\n"
                                + "        resources.getDrawable(s2); // OK\n"
                                + "        int w3 = MimeTypes.drawable;\n"
                                + "        resources.getDrawable(w3); // OK\n"
                                + "\n"
                                + "        // Direct reference\n"
                                + "        resources.getDrawable(MimeTypes.style); // ERROR\n"
                                + "        resources.getDrawable(MimeTypes.styleAndDrawable); // OK\n"
                                + "        resources.getDrawable(MimeTypes.drawable); // OK\n"
                                + "    }\n"
                                + "\n"
                                + "    public void testCalls(String fileExt, Resources resources) {\n"
                                + "        int mimeIconId = MimeTypes.getIconForExt(fileExt);\n"
                                + "        resources.getDrawable(mimeIconId); // OK\n"
                                + "        resources.getDrawable(MimeTypes.getInferredString()); // OK (wrong but can't infer type)\n"
                                + "        resources.getDrawable(MimeTypes.getInferredDrawable()); // OK\n"
                                + "        resources.getDrawable(MimeTypes.getAnnotatedString()); // Error\n"
                                + "        resources.getDrawable(MimeTypes.getAnnotatedDrawable()); // OK\n"
                                + "        resources.getDrawable(MimeTypes.getUnknownType()); // OK (unknown/uncertain)\n"
                                + "    }\n"
                                + "\n"
                                + "    public void testFlow() {\n"
                                + "        int x = R.string.my_string;\n"
                                + "        int z = x;\n"
                                + "        myMethod(z, null); // ERROR\n"
                                + "\n"
                                + "        int w = MY_RESOURCE;\n"
                                + "        myMethod(w, null); // ERROR\n"
                                + "    }\n"
                                + "\n"
                                + "    private static final int MY_RESOURCE = R.string.my_string;\n"
                                + "\n"
                                + "    private static class MimeTypes {\n"
                                + "        @android.support.annotation.StyleRes\n"
                                + "        @android.support.annotation.DrawableRes\n"
                                + "        public static int styleAndDrawable;\n"
                                + "\n"
                                + "        @android.support.annotation.StyleRes\n"
                                + "        public static int style;\n"
                                + "\n"
                                + "        @android.support.annotation.DrawableRes\n"
                                + "        public static int drawable;\n"
                                + "\n"
                                + "        @android.support.annotation.DrawableRes\n"
                                + "        public static int getIconForExt(String ext) {\n"
                                + "            return R.drawable.my_drawable;\n"
                                + "        }\n"
                                + "\n"
                                + "        public static int getInferredString() {\n"
                                + "            // Implied string - can we handle this?\n"
                                + "            return R.string.my_string;\n"
                                + "        }\n"
                                + "\n"
                                + "        public static int getInferredDrawable() {\n"
                                + "            // Implied drawable - can we handle this?\n"
                                + "            return R.drawable.my_drawable;\n"
                                + "        }\n"
                                + "\n"
                                + "        @android.support.annotation.StringRes\n"
                                + "        public static int getAnnotatedString() {\n"
                                + "            return R.string.my_string;\n"
                                + "        }\n"
                                + "\n"
                                + "        @android.support.annotation.DrawableRes\n"
                                + "        public static int getAnnotatedDrawable() {\n"
                                + "            return R.drawable.my_drawable;\n"
                                + "        }\n"
                                + "\n"
                                + "        public static int getUnknownType() {\n"
                                + "            return new Random(1000).nextInt();\n"
                                + "        }\n"
                                + "    }\n"
                                + "\n"
                                + "    public static final class R {\n"
                                + "        public static final class drawable {\n"
                                + "            public static final int my_drawable =0x7f020057;\n"
                                + "        }\n"
                                + "        public static final class string {\n"
                                + "            public static final int my_string =0x7f0a000e;\n"
                                + "        }\n"
                                + "    }\n"
                                + "}\n"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testTypes2() throws Exception {
        assertEquals(""
                + "src/test/pkg/ActivityType.java:5: Error: Expected resource of type drawable [ResourceType]\n"
                + "    SKI(1),\n"
                + "        ~\n"
                + "src/test/pkg/ActivityType.java:6: Error: Expected resource of type drawable [ResourceType]\n"
                + "    SNOWBOARD(2);\n"
                + "              ~\n"
                + "2 errors, 0 warnings\n",

                lintProject(
                        java("src/test/pkg/ActivityType.java", ""
                                + "import android.support.annotation.DrawableRes;\n"
                                + "\n"
                                + "enum ActivityType {\n"
                                + "\n"
                                + "    SKI(1),\n"
                                + "    SNOWBOARD(2);\n"
                                + "\n"
                                + "    private final int mIconResId;\n"
                                + "\n"
                                + "    ActivityType(@DrawableRes int iconResId) {\n"
                                + "        mIconResId = iconResId;\n"
                                + "    }\n"
                                + "}"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    // Temporarily disabled; TypedArray.getResourceId has now been annotated with @StyleRes
    //public void testResourceTypesIssue182433() throws Exception {
    //    // Regression test for https://code.google.com/p/android/issues/detail?id=182433
    //    assertEquals("No warnings.",
    //            lintProject(
    //                    java("src/test/pkg/ResourceTypeTest.java", ""
    //                            + "package test.pkg;\n"
    //                            + "import android.app.Activity;\n"
    //                            + "import android.content.res.TypedArray;\n"
    //                            + "\n"
    //                            + "@SuppressWarnings(\"unused\")\n"
    //                            + "public class ResourceTypeTest extends Activity {\n"
    //                            + "    public static void test(TypedArray typedArray) {\n"
    //                            + "       typedArray.getResourceId(2 /* index */, 0 /* invalid drawableRes */);\n"
    //                            + "    }\n"
    //                            + "}\n"),
    //                    mAnyResAnnotation
    //            ));
    //}

    @SuppressWarnings({"MethodMayBeStatic", "ResultOfObjectAllocationIgnored"})
    public void testConstructor() throws Exception {
        assertEquals(""
                + "src/test/pkg/ConstructorTest.java:14: Error: Expected resource of type drawable [ResourceType]\n"
                + "        new ConstructorTest(1, 3);\n"
                + "                            ~\n"
                + "src/test/pkg/ConstructorTest.java:14: Error: Value must be \u2265 5 (was 3) [Range]\n"
                + "        new ConstructorTest(1, 3);\n"
                + "                               ~\n"
                + "src/test/pkg/ConstructorTest.java:19: Error: Constructor ConstructorTest must be called from the UI thread, currently inferred thread is worker thread [WrongThread]\n"
                + "        new ConstructorTest(res, range);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "3 errors, 0 warnings\n",

                lintProject(
                        java("src/test/pkg/ConstructorTest.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.support.annotation.DrawableRes;\n"
                                + "import android.support.annotation.IntRange;\n"
                                + "import android.support.annotation.UiThread;\n"
                                + "import android.support.annotation.WorkerThread;\n"
                                + "\n"
                                + "public class ConstructorTest {\n"
                                + "    @UiThread\n"
                                + "    ConstructorTest(@DrawableRes int iconResId, @IntRange(from = 5) int start) {\n"
                                + "    }\n"
                                + "\n"
                                + "    public void testParameters() {\n"
                                + "        new ConstructorTest(1, 3);\n"
                                + "    }\n"
                                + "\n"
                                + "    @WorkerThread\n"
                                + "    public void testMethod(int res, int range) {\n"
                                + "        new ConstructorTest(res, range);\n"
                                + "    }\n"
                                + "}\n"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testColorAsDrawable() throws Exception {
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",

                lintProject(java(""
                            + "package p1.p2;\n"
                            + "\n"
                            + "import android.content.Context;\n"
                            + "import android.view.View;\n"
                            + "\n"
                            + "public class ColorAsDrawable {\n"
                            + "    static void test(Context context) {\n"
                            + "        View separator = new View(context);\n"
                            + "        separator.setBackgroundResource(android.R.color.black);\n"
                            + "    }\n"
                            + "}\n")));
    }

    public void testCheckResult() throws Exception {
        if (!SDK_ANNOTATIONS_AVAILABLE) {
            // Currently only tests @CheckResult on SDK annotations
            return;
        }
        assertEquals(""
                + "src/test/pkg/CheckPermissions.java:22: Warning: The result of extractAlpha is not used [CheckResult]\n"
                + "        bitmap.extractAlpha(); // WARNING\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/Intersect.java:7: Warning: The result of intersect is not used. If the rectangles do not intersect, no change is made and the original rectangle is not modified. These methods return false to indicate that this has happened. [CheckResult]\n"
                + "    rect.intersect(aLeft, aTop, aRight, aBottom);\n"
                + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/CheckPermissions.java:10: Warning: The result of checkCallingOrSelfPermission is not used; did you mean to call #enforceCallingOrSelfPermission(String,String)? [UseCheckPermission]\n"
                + "        context.checkCallingOrSelfPermission(Manifest.permission.INTERNET); // WRONG\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/CheckPermissions.java:11: Warning: The result of checkPermission is not used; did you mean to call #enforcePermission(String,int,int,String)? [UseCheckPermission]\n"
                + "        context.checkPermission(Manifest.permission.INTERNET, 1, 1);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 4 warnings\n",

                lintProject(
                        java(""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.Manifest;\n"
                                + "import android.content.Context;\n"
                                + "import android.content.pm.PackageManager;\n"
                                + "import android.graphics.Bitmap;\n"
                                + "\n"
                                + "public class CheckPermissions {\n"
                                + "    private void foo(Context context) {\n"
                                + "        context.checkCallingOrSelfPermission(Manifest.permission.INTERNET); // WRONG\n"
                                + "        context.checkPermission(Manifest.permission.INTERNET, 1, 1);\n"
                                + "        check(context.checkCallingOrSelfPermission(Manifest.permission.INTERNET)); // OK\n"
                                + "        int check = context.checkCallingOrSelfPermission(Manifest.permission.INTERNET); // OK\n"
                                + "        if (context.checkCallingOrSelfPermission(Manifest.permission.INTERNET) // OK\n"
                                + "                != PackageManager.PERMISSION_GRANTED) {\n"
                                + "            showAlert(context, \"Error\",\n"
                                + "                    \"Application requires permission to access the Internet\");\n"
                                + "        }\n"
                                + "    }\n"
                                + "\n"
                                + "    private Bitmap checkResult(Bitmap bitmap) {\n"
                                + "        bitmap.extractAlpha(); // WARNING\n"
                                + "        Bitmap bitmap2 = bitmap.extractAlpha(); // OK\n"
                                + "        call(bitmap.extractAlpha()); // OK\n"
                                + "        return bitmap.extractAlpha(); // OK\n"
                                + "    }\n"
                                + "\n"
                                + "    private void showAlert(Context context, String error, String s) {\n"
                                + "    }\n"
                                + "\n"
                                + "    private void check(int i) {\n"
                                + "    }\n"
                                + "    private void call(Bitmap bitmap) {\n"
                                + "    }\n"
                                + "}"),
                        java("src/test/pkg/Intersect.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.graphics.Rect;\n"
                                + "\n"
                                + "public class Intersect {\n"
                                + "  void check(Rect rect, int aLeft, int aTop, int aRight, int aBottom) {\n"
                                + "    rect.intersect(aLeft, aTop, aRight, aBottom);\n"
                                + "  }\n"
                                + "}")
                        ));
    }

    public void testIdResource() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=220612
        assertEquals("No warnings.",

                lintProject(
                        java("src/com./example/myapplication/Test1.java", ""
                                + "package com.example.myapplication;\n"
                                + "\n"
                                + "import android.support.annotation.IdRes;\n"
                                + "import android.support.annotation.LayoutRes;\n"
                                + "\n"
                                + "public class Test1 {\n"
                                + "\n"
                                + "    private final int layout;\n"
                                + "    private final int id;\n"
                                + "    private boolean visible;\n"
                                + "\n"
                                + "    public Test1(@LayoutRes int layout, @IdRes int id) {\n"
                                + "        this.layout = layout;\n"
                                + "        this.id = id;\n"
                                + "        this.visible = true;\n"
                                + "    }\n"
                                + "}"),
                        java("src/com/example/myapplication/Test2.java", ""
                                + "package com.example.myapplication;\n"
                                + "\n"
                                + "import android.support.annotation.IdRes;\n"
                                + "import android.support.annotation.StringRes;\n"
                                + "\n"
                                + "public class Test2 extends Test1 {\n"
                                + "\n"
                                + "    public Test2(@IdRes int id, @StringRes int titleResId) {\n"
                                + "        super(R.layout.somelayout, id);\n"
                                + "    }\n"
                                + "    public static final class R {\n"
                                + "        public static final class layout {\n"
                                + "            public static final int somelayout = 0x7f0a0000;\n"
                                + "        }\n"
                                + "    }\n"
                                + "}"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    private final TestFile mLocationManagerStub = java("src/android/location/LocationManager.java", ""
            + "package android.location;\n"
            + "\n"
            + "import android.support.annotation.RequiresPermission;\n"
            + "\n"
            + "import static android.Manifest.permission.ACCESS_COARSE_LOCATION;\n"
            + "import static android.Manifest.permission.ACCESS_FINE_LOCATION;\n"
            + "\n"
            + "@SuppressWarnings(\"UnusedDeclaration\")\n"
            + "public abstract class LocationManager {\n"
            + "    @RequiresPermission(anyOf = {ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION})\n"
            + "    public abstract Location myMethod(String provider);\n"
            + "    public static class Location {\n"
            + "    }\n"
            + "}\n");

    private final TestFile mPermissionTest = java("src/test/pkg/PermissionTest.java", ""
            + "package test.pkg;\n"
            + "\n"
            + "import android.location.LocationManager;\n"
            + "\n"
            + "public class PermissionTest {\n"
            + "    public static void test(LocationManager locationManager, String provider) {\n"
            + "        LocationManager.Location location = locationManager.myMethod(provider);\n"
            + "    }\n"
            + "}\n");

    private TestFile getManifestWithPermissions(int targetSdk, String... permissions) {
        return getManifestWithPermissions(1, targetSdk, permissions);
    }

    private TestFile getThingsManifestWithPermissions(int targetSdk, @Nullable Boolean isRequired, String... permissions) {
        StringBuilder applicationBlock = new StringBuilder();
        applicationBlock.append("<uses-library android:name=\"com.google.android.things\"");
        if (isRequired != null) {
            applicationBlock.append(" android:required=");
            if (isRequired) {
                applicationBlock.append("\"true\"");
            } else {
                applicationBlock.append("\"false\"");
            }
        }
        applicationBlock.append("/>\n");
        return getManifestWithPermissions(
                applicationBlock.toString(),
                1,
                targetSdk,
                permissions);
    }

    private TestFile getManifestWithPermissions(int minSdk, int targetSdk, String... permissions) {
        return getManifestWithPermissions(null, minSdk, targetSdk, permissions);
    }

    private TestFile getManifestWithPermissions(
            @Nullable String applicationBlock, int minSdk, int targetSdk, String... permissions) {
        StringBuilder permissionBlock = new StringBuilder();
        for (String permission : permissions) {
            permissionBlock.append("    <uses-permission android:name=\"").append(permission)
                    .append("\" />\n");
        }
        return xml("AndroidManifest.xml", ""
                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    package=\"foo.bar2\"\n"
                + "    android:versionCode=\"1\"\n"
                + "    android:versionName=\"1.0\" >\n"
                + "\n"
                + "    <uses-sdk android:minSdkVersion=\"" + minSdk + "\" android:targetSdkVersion=\""
                + targetSdk + "\" />\n"
                + "\n"
                + permissionBlock.toString()
                + "\n"
                + "    <application\n"
                + "        android:icon=\"@drawable/ic_launcher\"\n"
                + "        android:label=\"@string/app_name\" >\n"
                + (applicationBlock != null ? applicationBlock : "")
                + "    </application>\n"
                + "\n"
                + "</manifest>");
    }

    private TestFile mRevokeTest = java("src/test/pkg/RevokeTest.java", ""
            + "package test.pkg;\n"
            + "\n"
            + "import android.content.Context;\n"
            + "import android.content.pm.PackageManager;\n"
            + "import android.location.LocationManager;\n"
            + "import java.io.IOException;\n"
            + "import java.security.AccessControlException;\n"
            + "\n"
            + "public class RevokeTest {\n"
            + "    public static void test1(LocationManager locationManager, String provider) {\n"
            + "        try {\n"
            + "            // Ok: Security exception caught in one of the branches\n"
            + "            locationManager.myMethod(provider); // OK\n"
            + "        } catch (IllegalArgumentException ignored) {\n"
            + "        } catch (SecurityException ignored) {\n"
            + "        }\n"
            + "\n"
            + "        try {\n"
            + "            // You have to catch SecurityException explicitly, not parent\n"
            + "            locationManager.myMethod(provider); // ERROR\n"
            + "        } catch (RuntimeException e) { // includes Security Exception\n"
            + "        }\n"
            + "\n"
            + "        try {\n"
            + "            // Ok: Caught in outer statement\n"
            + "            try {\n"
            + "                locationManager.myMethod(provider); // OK\n"
            + "            } catch (IllegalArgumentException e) {\n"
            + "                // inner\n"
            + "            }\n"
            + "        } catch (SecurityException ignored) {\n"
            + "        }\n"
            + "\n"
            + "        try {\n"
            + "            // You have to catch SecurityException explicitly, not parent\n"
            + "            locationManager.myMethod(provider); // ERROR\n"
            + "        } catch (Exception e) { // includes Security Exception\n"
            + "        }\n"
            + "\n"
            + "        // NOT OK: Catching security exception subclass (except for dedicated ones?)\n"
            + "\n"
            + "        try {\n"
            + "            // Error: catching security exception, but not all of them\n"
            + "            locationManager.myMethod(provider); // ERROR\n"
            + "        } catch (AccessControlException e) { // security exception but specific one\n"
            + "        }\n"
            + "    }\n"
            + "\n"
            + "    public static void test2(LocationManager locationManager, String provider) {\n"
            + "        locationManager.myMethod(provider); // ERROR: not caught\n"
            + "    }\n"
            + "\n"
            + "    public static void test3(LocationManager locationManager, String provider)\n"
            + "            throws IllegalArgumentException {\n"
            + "        locationManager.myMethod(provider); // ERROR: not caught by right type\n"
            + "    }\n"
            + "\n"
            + "    public static void test4(LocationManager locationManager, String provider)\n"
            + "            throws AccessControlException {  // Security exception but specific one\n"
            + "        locationManager.myMethod(provider); // ERROR\n"
            + "    }\n"
            + "\n"
            + "    public static void test5(LocationManager locationManager, String provider)\n"
            + "            throws SecurityException {\n"
            + "        locationManager.myMethod(provider); // OK\n"
            + "    }\n"
            + "\n"
            + "    public static void test6(LocationManager locationManager, String provider)\n"
            + "            throws Exception { // includes Security Exception\n"
            + "        // You have to throw SecurityException explicitly, not parent\n"
            + "        locationManager.myMethod(provider); // ERROR\n"
            + "    }\n"
            + "\n"
            + "    public static void test7(LocationManager locationManager, String provider, Context context)\n"
            + "            throws IllegalArgumentException {\n"
            + "        if (context.getPackageManager().checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, context.getPackageName()) != PackageManager.PERMISSION_GRANTED) {\n"
            + "            return;\n"
            + "        }\n"
            + "        locationManager.myMethod(provider); // OK: permission checked\n"
            + "    }\n"
            + "\n"
            + "    public void test8(LocationManager locationManager, String provider) {\n"
            + "          // Regression test for http://b.android.com/187204\n"
            + "        try {\n"
            + "            locationManager.myMethod(provider); // ERROR\n"
            + "            mightThrow();\n"
            + "        } catch (SecurityException | IOException se) { // OK: Checked in multi catch\n"
            + "        }\n"
            + "        try {\n"
            + "            locationManager.myMethod(provider); // ERROR\n"
            + "            mightThrow();\n"
            + "        } catch (IOException | SecurityException se) { // OK: Checked in multi catch\n"
            + "        }\n"
            + "    }\n"
            + "\n"
            + "    public void mightThrow() throws IOException {\n"
            + "    }\n"
            + "\n"
            + "}\n");

    public void testMissingPermissions() throws Exception {
        assertEquals(""
                + "src/test/pkg/PermissionTest.java:7: Error: Missing permissions required by LocationManager.myMethod: android.permission.ACCESS_FINE_LOCATION or android.permission.ACCESS_COARSE_LOCATION [MissingPermission]\n"
                + "        LocationManager.Location location = locationManager.myMethod(provider);\n"
                + "                                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",
                lintProject(
                        getManifestWithPermissions(14),
                        mPermissionTest,
                        mLocationManagerStub,
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testHasPermission() throws Exception {
        assertEquals("No warnings.",
                lintProject(
                        getManifestWithPermissions(14, "android.permission.ACCESS_FINE_LOCATION"),
                        mPermissionTest,
                        mLocationManagerStub,
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testRevokePermissions() throws Exception {
        assertEquals(""
                + "src/test/pkg/RevokeTest.java:20: Error: Call requires permission which may be rejected by user: code should explicitly check to see if permission is available (with checkPermission) or explicitly handle a potential SecurityException [MissingPermission]\n"
                + "            locationManager.myMethod(provider); // ERROR\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RevokeTest.java:36: Error: Call requires permission which may be rejected by user: code should explicitly check to see if permission is available (with checkPermission) or explicitly handle a potential SecurityException [MissingPermission]\n"
                + "            locationManager.myMethod(provider); // ERROR\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RevokeTest.java:44: Error: Call requires permission which may be rejected by user: code should explicitly check to see if permission is available (with checkPermission) or explicitly handle a potential SecurityException [MissingPermission]\n"
                + "            locationManager.myMethod(provider); // ERROR\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RevokeTest.java:50: Error: Call requires permission which may be rejected by user: code should explicitly check to see if permission is available (with checkPermission) or explicitly handle a potential SecurityException [MissingPermission]\n"
                + "        locationManager.myMethod(provider); // ERROR: not caught\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RevokeTest.java:55: Error: Call requires permission which may be rejected by user: code should explicitly check to see if permission is available (with checkPermission) or explicitly handle a potential SecurityException [MissingPermission]\n"
                + "        locationManager.myMethod(provider); // ERROR: not caught by right type\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RevokeTest.java:60: Error: Call requires permission which may be rejected by user: code should explicitly check to see if permission is available (with checkPermission) or explicitly handle a potential SecurityException [MissingPermission]\n"
                + "        locationManager.myMethod(provider); // ERROR\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RevokeTest.java:71: Error: Call requires permission which may be rejected by user: code should explicitly check to see if permission is available (with checkPermission) or explicitly handle a potential SecurityException [MissingPermission]\n"
                + "        locationManager.myMethod(provider); // ERROR\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "7 errors, 0 warnings\n",
                lintProject(
                        getManifestWithPermissions(23, "android.permission.ACCESS_FINE_LOCATION"),
                        mLocationManagerStub,
                        mRevokeTest,
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testImpliedPermissions() throws Exception {
        // Regression test for
        //   https://code.google.com/p/android/issues/detail?id=177381
        assertEquals(""
                + "src/test/pkg/PermissionTest2.java:11: Error: Missing permissions required by PermissionTest2.method1: my.permission.PERM2 [MissingPermission]\n"
                + "        method1(); // ERROR\n"
                + "        ~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",
                lintProject(
                        getManifestWithPermissions(14, 14, "android.permission.ACCESS_FINE_LOCATION"),
                        java("src/test/pkg/PermissionTest2.java", ""
                                + "package test.pkg;\n"
                                + "import android.support.annotation.RequiresPermission;\n"
                                + "\n"
                                + "public class PermissionTest2 {\n"
                                + "    @RequiresPermission(allOf = {\"my.permission.PERM1\",\"my.permission.PERM2\"})\n"
                                + "    public void method1() {\n"
                                + "    }\n"
                                + "\n"
                                + "    @RequiresPermission(\"my.permission.PERM1\")\n"
                                + "    public void method2() {\n"
                                + "        method1(); // ERROR\n"
                                + "    }\n"
                                + "\n"
                                + "    @RequiresPermission(allOf = {\"my.permission.PERM1\",\"my.permission.PERM2\"})\n"
                                + "    public void method3() {\n"
                                + "        // The above @RequiresPermission implies that we are holding these\n"
                                + "        // permissions here, so the call to method1() should not be flagged as\n"
                                + "        // missing a permission!\n"
                                + "        method1(); // OK\n"
                                + "    }\n"
                                + "}\n"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testRevokePermissionsPre23() throws Exception {
        assertEquals("No warnings.",
                lintProject(
                        getManifestWithPermissions(14, "android.permission.ACCESS_FINE_LOCATION"),
                        mLocationManagerStub,
                        mRevokeTest,
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testUsesPermissionSdk23() throws Exception {
        TestFile manifest = getManifestWithPermissions(14,
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.BLUETOOTH");
        String contents = manifest.getContents();
        assertNotNull(contents);
        String s = contents.replace(TAG_USES_PERMISSION, TAG_USES_PERMISSION_SDK_23);
        manifest.withSource(s);
        assertEquals("No warnings.",
                lintProject(
                        manifest,
                        mPermissionTest,
                        mLocationManagerStub,
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testUsesPermissionSdkM() throws Exception {
        TestFile manifest = getManifestWithPermissions(14,
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.BLUETOOTH");
        String contents = manifest.getContents();
        assertNotNull(contents);
        String s = contents.replace(TAG_USES_PERMISSION, TAG_USES_PERMISSION_SDK_M);
        manifest.withSource(s);
        assertEquals("No warnings.",
                lintProject(
                        manifest,
                        mPermissionTest,
                        mLocationManagerStub,
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testPermissionAnnotation() throws Exception {
        assertEquals(""
                + "src/test/pkg/LocationManager.java:24: Error: Missing permissions required by LocationManager.getLastKnownLocation: android.permission.ACCESS_FINE_LOCATION or android.permission.ACCESS_COARSE_LOCATION [MissingPermission]\n"
                + "        Location location = manager.getLastKnownLocation(\"provider\");\n"
                + "                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",
                lintProject(
                        java("src/test/pkg/LocationManager.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.support.annotation.RequiresPermission;\n"
                                + "\n"
                                + "import java.lang.annotation.Retention;\n"
                                + "import java.lang.annotation.RetentionPolicy;\n"
                                + "\n"
                                + "import static android.Manifest.permission.ACCESS_COARSE_LOCATION;\n"
                                + "import static android.Manifest.permission.ACCESS_FINE_LOCATION;\n"
                                + "\n"
                                + "@SuppressWarnings(\"UnusedDeclaration\")\n"
                                + "public abstract class LocationManager {\n"
                                + "    @RequiresPermission(anyOf = {ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION})\n"
                                + "    @Retention(RetentionPolicy.SOURCE)\n"
                                + "    @interface AnyLocationPermission {\n"
                                + "    }\n"
                                + "\n"
                                + "    @AnyLocationPermission\n"
                                + "    public abstract Location getLastKnownLocation(String provider);\n"
                                + "    public static class Location {\n"
                                + "    }\n"
                                + "    \n"
                                + "    public static void test(LocationManager manager) {\n"
                                + "        Location location = manager.getLastKnownLocation(\"provider\");\n"
                                + "    }\n"
                                + "}\n"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testMissingManifestLevelPermissionsWithAndroidThings() throws Exception {
        assertEquals(
                ""
                        + "src/test/pkg/PermissionTest.java:7: Error: Missing permissions required by LocationManager.myMethod: android.permission.ACCESS_FINE_LOCATION or android.permission.ACCESS_COARSE_LOCATION [MissingPermission]\n"
                        + "        LocationManager.Location location = locationManager.myMethod(provider);\n"
                        + "                                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n",
                lintProject(
                        getThingsManifestWithPermissions(24, null),
                        mPermissionTest,
                        mLocationManagerStub,
                        mSupportClasspath,
                        mSupportJar));
    }

    public void testHasManifestLevelPermissionsWithAndroidThings() throws Exception {
        assertEquals(
                "No warnings.",
                lintProject(
                        getThingsManifestWithPermissions(
                                24, null, "android.permission.ACCESS_FINE_LOCATION"),
                        mPermissionTest,
                        mLocationManagerStub,
                        mSupportClasspath,
                        mSupportJar));
    }

    public void testMissingRuntimePermissionsWithOptionalAndroidThings() throws Exception {
        assertEquals(
                ""
                        + "src/test/pkg/PermissionTest.java:7: Error: Call requires permission which may be rejected by user: code should explicitly check to see if permission is available (with checkPermission) or explicitly handle a potential SecurityException [MissingPermission]\n"
                        + "        LocationManager.Location location = locationManager.myMethod(provider);\n"
                        + "                                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n",
                lintProject(
                        getThingsManifestWithPermissions(24, false, "android.permission.ACCESS_FINE_LOCATION"),
                        mPermissionTest,
                        mLocationManagerStub,
                        mSupportClasspath,
                        mSupportJar));
    }

    public void testThreading() throws Exception {
        assertEquals(""
                + "src/test/pkg/ThreadTest.java:15: Error: Method onPreExecute must be called from the main thread, currently inferred thread is worker thread [WrongThread]\n"
                + "                onPreExecute(); // ERROR\n"
                + "                ~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ThreadTest.java:16: Error: Method paint must be called from the UI thread, currently inferred thread is worker thread [WrongThread]\n"
                + "                view.paint(); // ERROR\n"
                + "                ~~~~~~~~~~~~\n"
                + "src/test/pkg/ThreadTest.java:22: Error: Method publishProgress must be called from the worker thread, currently inferred thread is main thread [WrongThread]\n"
                + "                publishProgress(); // ERROR\n"
                + "                ~~~~~~~~~~~~~~~~~\n"
                + "3 errors, 0 warnings\n",

            lintProject(
                java("src/test/pkg/ThreadTest.java", ""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.support.annotation.MainThread;\n"
                        + "import android.support.annotation.UiThread;\n"
                        + "import android.support.annotation.WorkerThread;\n"
                        + "\n"
                        + "public class ThreadTest {\n"
                        + "    public static AsyncTask testTask() {\n"
                        + "\n"
                        + "        return new AsyncTask() {\n"
                        + "            final CustomView view = new CustomView();\n"
                        + "\n"
                        + "            @Override\n"
                        + "            protected void doInBackground(Object... params) {\n"
                        + "                onPreExecute(); // ERROR\n"
                        + "                view.paint(); // ERROR\n"
                        + "                publishProgress(); // OK\n"
                        + "            }\n"
                        + "\n"
                        + "            @Override\n"
                        + "            protected void onPreExecute() {\n"
                        + "                publishProgress(); // ERROR\n"
                        + "                onProgressUpdate(); // OK\n"
                        + "                // Suppressed via older Android Studio inspection id:\n"
                        + "                //noinspection ResourceType\n"
                        + "                publishProgress(); // SUPPRESSED\n"
                        + "                // Suppressed via new lint id:\n"
                        + "                //noinspection WrongThread\n"
                        + "                publishProgress(); // SUPPRESSED\n"
                        + "                // Suppressed via Studio inspection id:\n"
                        + "                //noinspection AndroidLintWrongThread\n"
                        + "                publishProgress(); // SUPPRESSED\n"
                        + "            }\n"
                        + "        };\n"
                        + "    }\n"
                        + "\n"
                        + "    @UiThread\n"
                        + "    public static class View {\n"
                        + "        public void paint() {\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    public static class CustomView extends View {\n"
                        + "        @Override public void paint() {\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    public abstract static class AsyncTask {\n"
                        + "        @WorkerThread\n"
                        + "        protected abstract void doInBackground(Object... params);\n"
                        + "\n"
                        + "        @MainThread\n"
                        + "        protected void onPreExecute() {\n"
                        + "        }\n"
                        + "\n"
                        + "        @MainThread\n"
                        + "        protected void onProgressUpdate(Object... values) {\n"
                        + "        }\n"
                        + "\n"
                        + "        @WorkerThread\n"
                        + "        protected final void publishProgress(Object... values) {\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"),
                    mSupportClasspath,
                    mSupportJar
            ));
    }

    @SuppressWarnings("all") // Sample code, warts and all
    public void testThreadingIssue207313() throws Exception {
        // Regression test for scenario in
        //  https://code.google.com/p/android/issues/detail?id=207313
        assertEquals(""
                + "src/test/pkg/BigClassClient.java:10: Error: Constructor BigClass must be called from the UI thread, currently inferred thread is worker thread [WrongThread]\n"
                + "        BigClass o = new BigClass();\n"
                + "                     ~~~~~~~~~~~~~~\n"
                + "src/test/pkg/BigClassClient.java:11: Error: Method f1 must be called from the UI thread, currently inferred thread is worker thread [WrongThread]\n"
                + "        o.f1();   // correct WrongThread: must be called from the UI thread currently inferred thread is worker\n"
                + "        ~~~~~~\n"
                + "src/test/pkg/BigClassClient.java:12: Error: Method f2 must be called from the UI thread, currently inferred thread is worker thread [WrongThread]\n"
                + "        o.f2();   // correct WrongThread: must be called from the UI thread currently inferred thread is worker\n"
                + "        ~~~~~~\n"
                + "src/test/pkg/BigClassClient.java:13: Error: Method f100 must be called from the UI thread, currently inferred thread is worker thread [WrongThread]\n"
                + "        o.f100(); // correct WrongThread: must be called from the UI thread currently inferred thread is worker\n"
                + "        ~~~~~~~~\n"
                + "src/test/pkg/BigClassClient.java:22: Error: Method g must be called from the worker thread, currently inferred thread is UI thread [WrongThread]\n"
                + "        o.g();    // correct WrongThread: must be called from the worker thread currently inferred thread is UI\n"
                + "        ~~~~~\n"
                + "5 errors, 0 warnings\n",

                lintProject(
                        java("src/test/pkg/BigClass.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.support.annotation.UiThread;\n"
                                + "import android.support.annotation.WorkerThread;\n"
                                + "\n"
                                + "@UiThread // it's here to prevent putting it on all 100 methods\n"
                                + "class BigClass {\n"
                                + "    void f1() { }\n"
                                + "    void f2() { }\n"
                                + "    //...\n"
                                + "    void f100() { }\n"
                                + "    @WorkerThread // this single method is not UI, it's something else\n"
                                + "    void g() { }\n"
                                + "    BigClass() { }\n"
                                + "}\n"),
                        java("src/test/pkg/BigClassClient.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.support.annotation.UiThread;\n"
                                + "import android.support.annotation.WorkerThread;\n"
                                + "\n"
                                + "@SuppressWarnings(\"unused\")\n"
                                + "public class BigClassClient {\n"
                                + "    @WorkerThread\n"
                                + "    void worker() {\n"
                                + "        BigClass o = new BigClass();\n"
                                + "        o.f1();   // correct WrongThread: must be called from the UI thread currently inferred thread is worker\n"
                                + "        o.f2();   // correct WrongThread: must be called from the UI thread currently inferred thread is worker\n"
                                + "        o.f100(); // correct WrongThread: must be called from the UI thread currently inferred thread is worker\n"
                                + "        o.g();    // unexpected WrongThread: must be called from the UI thread currently inferred thread is worker\n"
                                + "    }\n"
                                + "    @UiThread\n"
                                + "    void ui() {\n"
                                + "        BigClass o = new BigClass();\n"
                                + "        o.f1();   // no problem\n"
                                + "        o.f2();   // no problem\n"
                                + "        o.f100(); // no problem\n"
                                + "        o.g();    // correct WrongThread: must be called from the worker thread currently inferred thread is UI\n"
                                + "    }\n"
                                + "}\n"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    @SuppressWarnings("all") // Sample code
    public void testThreadingIssue207302() throws Exception {
        // Regression test for
        //    https://code.google.com/p/android/issues/detail?id=207302
        assertEquals("No warnings.",

                lintProject(
                        java("src/test/pkg/TestPostRunnable.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.support.annotation.WorkerThread;\n"
                                + "import android.view.View;\n"
                                + "\n"
                                + "public class TestPostRunnable {\n"
                                + "    View view;\n"
                                + "    @WorkerThread\n"
                                + "    void f() {\n"
                                + "        view.post(new Runnable() {\n"
                                + "            @Override public void run() {\n"
                                + "                // stuff on UI thread\n"
                                + "            }\n"
                                + "        });\n"
                                + "    }\n"
                                + "}"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    @SuppressWarnings("all") // Sample code
    public void testAnyThread() throws Exception {
        assertEquals(""
                + "src/test/pkg/AnyThreadTest.java:11: Error: Method worker must be called from the worker thread, currently inferred thread is any thread [WrongThread]\n"
                + "        worker(); // ERROR\n"
                + "        ~~~~~~~~\n"
                + "1 errors, 0 warnings\n",
                lintProject(
                        java("src/test/pkg/AnyThreadTest.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.support.annotation.AnyThread;\n"
                                + "import android.support.annotation.UiThread;\n"
                                + "import android.support.annotation.WorkerThread;\n"
                                + "\n"
                                + "@UiThread\n"
                                + "class AnyThreadTest {\n"
                                + "    @AnyThread\n"
                                + "    static void threadSafe() {\n"
                                + "        worker(); // ERROR\n"
                                + "    }\n"
                                + "    @WorkerThread\n"
                                + "    static void worker() {\n"
                                + "        threadSafe(); // OK\n"
                                + "    }\n"
                                + "}\n"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    @SuppressWarnings("all") // Sample code
    public void testMultipleThreads() throws Exception {
        // Ensure that when multiple threading annotations are specified
        // on methods, this is handled properly: calls can satisfy any one
        // threading annotation on the target, but if multiple threads are
        // found in the context, all of them must be valid for all targets
        assertEquals(""
                + "src/test/pkg/MultiThreadTest.java:21: Error: Method calleee must be called from the UI or worker thread, currently inferred thread is binder and worker thread [WrongThread]\n"
                + "        calleee(); // Not ok: thread could be binder thread, not supported by target\n"
                + "        ~~~~~~~~~\n"
                + "src/test/pkg/MultiThreadTest.java:28: Error: Method calleee must be called from the UI or worker thread, currently inferred thread is worker and binder thread [WrongThread]\n"
                + "        calleee(); // Not ok: thread could be binder thread, not supported by target\n"
                + "        ~~~~~~~~~\n"
                + "2 errors, 0 warnings\n",

                lintProject(
                        java("src/test/pkg/MultiThreadTest.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.support.annotation.BinderThread;\n"
                                + "import android.support.annotation.UiThread;\n"
                                + "import android.support.annotation.WorkerThread;\n"
                                + "\n"
                                + "class MultiThreadTest {\n"
                                + "    @UiThread\n"
                                + "    @WorkerThread\n"
                                + "    private static void calleee() {\n"
                                + "    }\n"
                                + "\n"
                                + "    @WorkerThread\n"
                                + "    private static void call1() {\n"
                                + "        calleee(); // OK - context is included in target\n"
                                + "    }\n"
                                + "\n"
                                + "    @BinderThread\n"
                                + "    @WorkerThread\n"
                                + "    private static void call2() {\n"
                                + "        calleee(); // Not ok: thread could be binder thread, not supported by target\n"
                                + "    }\n"
                                + "\n"
                                + "    // Same case as call2 but different order to make sure we don't just test the first one:\n"
                                + "    @WorkerThread\n"
                                + "    @BinderThread\n"
                                + "    private static void call3() {\n"
                                + "        calleee(); // Not ok: thread could be binder thread, not supported by target\n"
                                + "    }\n"
                                + "}\n"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testStaticMethod() throws Exception {
        // Regression test for
        //  https://code.google.com/p/android/issues/detail?id=175397
        assertEquals("No warnings.",
                lintProject(
                        java("src/test/pkg/StaticMethods.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.content.Context;\n"
                                + "import android.os.AsyncTask;\n"
                                + "import android.support.annotation.WorkerThread;\n"
                                + "import android.view.View;\n"
                                + "\n"
                                + "public class StaticMethods extends View {\n"
                                + "    public StaticMethods(Context context) {\n"
                                + "        super(context);\n"
                                + "    }\n"
                                + "\n"
                                + "    class MyAsyncTask extends AsyncTask<Long, Void, Boolean> {\n"
                                + "        @Override\n"
                                + "        protected Boolean doInBackground(Long... sizes) {\n"
                                + "            return workedThreadMethod();\n"
                                + "        }\n"
                                + "\n"
                                + "        @Override\n"
                                + "        protected void onPostExecute(Boolean isEnoughFree) {\n"
                                + "        }\n"
                                + "    }\n"
                                + "\n"
                                + "    public static boolean workedThreadMethod() {\n"
                                + "        return true;\n"
                                + "    }\n"
                                + "}"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testIntentPermission() throws Exception {
        if (SDK_ANNOTATIONS_AVAILABLE) {
            TestLintClient client = createClient();
            ExternalAnnotationRepository repository = ExternalAnnotationRepository.get(client);
            ResolvedMethod method = ExternalAnnotationRepositoryTest.createMethod(
                    "android.content.Context", "void", "startActivity",
                    "android.content.Intent");
            ResolvedAnnotation a = repository.getAnnotation(method, 0, PERMISSION_ANNOTATION);
            if (a == null) {
                // Running tests from outside the IDE (where it can't find the
                // bundled up to date annotations in tools/adt/idea/android/annotations)
                // and we have the annotations.zip file available in platform-tools,
                // but its contents are old (it's from Android M Preview 1, not including
                // the new intent-annotation data); skip this test for now.
                return;
            }
        }

        assertEquals(!SDK_ANNOTATIONS_AVAILABLE ? "" // Most of the intent/content provider checks are based on framework annotations
                + "src/test/pkg/ActionTest.java:86: Error: Missing permissions required by intent ActionTest.ACTION_CALL: android.permission.CALL_PHONE [MissingPermission]\n"
                + "        myStartActivity(\"\", null, new Intent(ACTION_CALL));\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:87: Error: Missing permissions required to read ActionTest.BOOKMARKS_URI: com.android.browser.permission.READ_HISTORY_BOOKMARKS [MissingPermission]\n"
                + "        myReadResolverMethod(\"\", BOOKMARKS_URI);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:88: Error: Missing permissions required to read ActionTest.BOOKMARKS_URI: com.android.browser.permission.READ_HISTORY_BOOKMARKS [MissingPermission]\n"
                + "        myWriteResolverMethod(BOOKMARKS_URI);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "3 errors, 0 warnings\n" : ""

                + "src/test/pkg/ActionTest.java:36: Error: Missing permissions required by intent ActionTest.ACTION_CALL: android.permission.CALL_PHONE [MissingPermission]\n"
                + "        activity.startActivity(intent);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:42: Error: Missing permissions required by intent ActionTest.ACTION_CALL: android.permission.CALL_PHONE [MissingPermission]\n"
                + "        activity.startActivity(intent);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:43: Error: Missing permissions required by intent ActionTest.ACTION_CALL: android.permission.CALL_PHONE [MissingPermission]\n"
                + "        activity.startActivity(intent, null);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:44: Error: Missing permissions required by intent ActionTest.ACTION_CALL: android.permission.CALL_PHONE [MissingPermission]\n"
                + "        activity.startActivityForResult(intent, 0);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:45: Error: Missing permissions required by intent ActionTest.ACTION_CALL: android.permission.CALL_PHONE [MissingPermission]\n"
                + "        activity.startActivityFromChild(activity, intent, 0);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:46: Error: Missing permissions required by intent ActionTest.ACTION_CALL: android.permission.CALL_PHONE [MissingPermission]\n"
                + "        activity.startActivityIfNeeded(intent, 0);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:47: Error: Missing permissions required by intent ActionTest.ACTION_CALL: android.permission.CALL_PHONE [MissingPermission]\n"
                + "        activity.startActivityFromFragment(null, intent, 0);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:48: Error: Missing permissions required by intent ActionTest.ACTION_CALL: android.permission.CALL_PHONE [MissingPermission]\n"
                + "        activity.startNextMatchingActivity(intent);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:54: Error: Missing permissions required by intent ActionTest.ACTION_CALL: android.permission.CALL_PHONE [MissingPermission]\n"
                + "        context.sendBroadcast(intent);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:55: Error: Missing permissions required by intent ActionTest.ACTION_CALL: android.permission.CALL_PHONE [MissingPermission]\n"
                + "        context.sendBroadcast(intent, \"\");\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:56: Error: Missing permissions required by Context.sendBroadcastAsUser: android.permission.INTERACT_ACROSS_USERS [MissingPermission]\n"
                + "        context.sendBroadcastAsUser(intent, null);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:56: Error: Missing permissions required by intent ActionTest.ACTION_CALL: android.permission.CALL_PHONE [MissingPermission]\n"
                + "        context.sendBroadcastAsUser(intent, null);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:57: Error: Missing permissions required by Context.sendStickyBroadcast: android.permission.BROADCAST_STICKY [MissingPermission]\n"
                + "        context.sendStickyBroadcast(intent);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:57: Error: Missing permissions required by intent ActionTest.ACTION_CALL: android.permission.CALL_PHONE [MissingPermission]\n"
                + "        context.sendStickyBroadcast(intent);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:62: Error: Missing permissions required to read ActionTest.BOOKMARKS_URI: com.android.browser.permission.READ_HISTORY_BOOKMARKS [MissingPermission]\n"
                + "        resolver.query(BOOKMARKS_URI, null, null, null, null);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:65: Error: Missing permissions required to write ActionTest.BOOKMARKS_URI: com.android.browser.permission.WRITE_HISTORY_BOOKMARKS [MissingPermission]\n"
                + "        resolver.insert(BOOKMARKS_URI, null);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:66: Error: Missing permissions required to write ActionTest.BOOKMARKS_URI: com.android.browser.permission.WRITE_HISTORY_BOOKMARKS [MissingPermission]\n"
                + "        resolver.delete(BOOKMARKS_URI, null, null);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:67: Error: Missing permissions required to write ActionTest.BOOKMARKS_URI: com.android.browser.permission.WRITE_HISTORY_BOOKMARKS [MissingPermission]\n"
                + "        resolver.update(BOOKMARKS_URI, null, null, null);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:86: Error: Missing permissions required by intent ActionTest.ACTION_CALL: android.permission.CALL_PHONE [MissingPermission]\n"
                + "        myStartActivity(\"\", null, new Intent(ACTION_CALL));\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:87: Error: Missing permissions required to read ActionTest.BOOKMARKS_URI: com.android.browser.permission.READ_HISTORY_BOOKMARKS [MissingPermission]\n"
                + "        myReadResolverMethod(\"\", BOOKMARKS_URI);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActionTest.java:88: Error: Missing permissions required to read ActionTest.BOOKMARKS_URI: com.android.browser.permission.READ_HISTORY_BOOKMARKS [MissingPermission]\n"
                + "        myWriteResolverMethod(BOOKMARKS_URI);\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "21 errors, 0 warnings\n",

                lintProject(
                        getManifestWithPermissions(14, 23),
                        java("src/test/pkg/ActionTest.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.Manifest;\n"
                                + "import android.app.Activity;\n"
                                + "import android.content.ContentResolver;\n"
                                + "import android.content.Context;\n"
                                + "import android.content.Intent;\n"
                                + "import android.database.Cursor;\n"
                                + "import android.net.Uri;\n"
                                + "import android.support.annotation.RequiresPermission;\n"
                                + "\n"
                                //+ "import static android.Manifest.permission.READ_HISTORY_BOOKMARKS;\n"
                                //+ "import static android.Manifest.permission.WRITE_HISTORY_BOOKMARKS;\n"
                                + "\n"
                                + "@SuppressWarnings({\"deprecation\", \"unused\"})\n"
                                + "public class ActionTest {\n"
                                + "     public static final String READ_HISTORY_BOOKMARKS=\"com.android.browser.permission.READ_HISTORY_BOOKMARKS\";\n"
                                + "     public static final String WRITE_HISTORY_BOOKMARKS=\"com.android.browser.permission.WRITE_HISTORY_BOOKMARKS\";\n"
                                + "    @RequiresPermission(Manifest.permission.CALL_PHONE)\n"
                                + "    public static final String ACTION_CALL = \"android.intent.action.CALL\";\n"
                                + "\n"
                                + "    @RequiresPermission.Read(@RequiresPermission(READ_HISTORY_BOOKMARKS))\n"
                                + "    @RequiresPermission.Write(@RequiresPermission(WRITE_HISTORY_BOOKMARKS))\n"
                                + "    public static final Uri BOOKMARKS_URI = Uri.parse(\"content://browser/bookmarks\");\n"
                                + "\n"
                                + "    public static final Uri COMBINED_URI = Uri.withAppendedPath(BOOKMARKS_URI, \"bookmarks\");\n"
                                + "    \n"
                                + "    public static void activities1(Activity activity) {\n"
                                + "        Intent intent = new Intent(Intent.ACTION_CALL);\n"
                                + "        intent.setData(Uri.parse(\"tel:1234567890\"));\n"
                                + "        // This one will only be flagged if we have framework metadata on Intent.ACTION_CALL\n"
                                // Too flaky
                                + "        //activity.startActivity(intent);\n"
                                + "    }\n"
                                + "\n"
                                + "    public static void activities2(Activity activity) {\n"
                                + "        Intent intent = new Intent(ACTION_CALL);\n"
                                + "        intent.setData(Uri.parse(\"tel:1234567890\"));\n"
                                + "        activity.startActivity(intent);\n"
                                + "    }\n"
                                + "    public static void activities3(Activity activity) {\n"
                                + "        Intent intent;\n"
                                + "        intent = new Intent(ACTION_CALL);\n"
                                + "        intent.setData(Uri.parse(\"tel:1234567890\"));\n"
                                + "        activity.startActivity(intent);\n"
                                + "        activity.startActivity(intent, null);\n"
                                + "        activity.startActivityForResult(intent, 0);\n"
                                + "        activity.startActivityFromChild(activity, intent, 0);\n"
                                + "        activity.startActivityIfNeeded(intent, 0);\n"
                                + "        activity.startActivityFromFragment(null, intent, 0);\n"
                                + "        activity.startNextMatchingActivity(intent);\n"
                                + "    }\n"
                                + "\n"
                                + "    public static void broadcasts(Context context) {\n"
                                + "        Intent intent;\n"
                                + "        intent = new Intent(ACTION_CALL);\n"
                                + "        context.sendBroadcast(intent);\n"
                                + "        context.sendBroadcast(intent, \"\");\n"
                                + "        context.sendBroadcastAsUser(intent, null);\n"
                                + "        context.sendStickyBroadcast(intent);\n"
                                + "    }\n"
                                + "\n"
                                + "    public static void contentResolvers(Context context, ContentResolver resolver) {\n"
                                + "        // read\n"
                                + "        resolver.query(BOOKMARKS_URI, null, null, null, null);\n"
                                + "\n"
                                + "        // write\n"
                                + "        resolver.insert(BOOKMARKS_URI, null);\n"
                                + "        resolver.delete(BOOKMARKS_URI, null, null);\n"
                                + "        resolver.update(BOOKMARKS_URI, null, null, null);\n"
                                + "\n"
                                + "        // Framework (external) annotation\n"
                                + "//REMOVED        resolver.query(android.provider.Browser.BOOKMARKS_URI, null, null, null, null);\n"
                                + "\n"
                                + "        // TODO: Look for more complex URI manipulations\n"
                                + "    }\n"
                                + "\n"
                                + "    public static void myStartActivity(String s1, String s2, \n"
                                + "                                       @RequiresPermission Intent intent) {\n"
                                + "    }\n"
                                + "\n"
                                + "    public static void myReadResolverMethod(String s1, @RequiresPermission.Read(@RequiresPermission) Uri uri) {\n"
                                + "    }\n"
                                + "\n"
                                + "    public static void myWriteResolverMethod(@RequiresPermission.Read(@RequiresPermission) Uri uri) {\n"
                                + "    }\n"
                                + "    \n"
                                + "    public static void testCustomMethods() {\n"
                                + "        myStartActivity(\"\", null, new Intent(ACTION_CALL));\n"
                                + "        myReadResolverMethod(\"\", BOOKMARKS_URI);\n"
                                + "        myWriteResolverMethod(BOOKMARKS_URI);\n"
                                + "    }\n"
                                + "}\n"),
                        mSupportClasspath,
                        mSupportJar

                ));
    }

    public void testCombinedIntDefAndIntRange() throws Exception {
        assertEquals(""
                + "src/test/pkg/X.java:27: Error: Must be one of: X.LENGTH_INDEFINITE, X.LENGTH_SHORT, X.LENGTH_LONG [WrongConstant]\n"
                + "        setDuration(UNRELATED); /// OK within range\n" // Not sure about this one
                + "                    ~~~~~~~~~\n"
                + "src/test/pkg/X.java:28: Error: Must be one of: X.LENGTH_INDEFINITE, X.LENGTH_SHORT, X.LENGTH_LONG or value must be ≥ 10 (was -5) [WrongConstant]\n"
                + "        setDuration(-5); // ERROR (not right int def or value\n"
                + "                    ~~\n"
                + "src/test/pkg/X.java:29: Error: Must be one of: X.LENGTH_INDEFINITE, X.LENGTH_SHORT, X.LENGTH_LONG or value must be ≥ 10 (was 8) [WrongConstant]\n"
                + "        setDuration(8); // ERROR (not matching number range)\n"
                + "                    ~\n"
                + "3 errors, 0 warnings\n",
                lintProject(
                        getManifestWithPermissions(14, 23),
                        java("src/test/pkg/X.java", ""
                                + "\n"
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.support.annotation.IntDef;\n"
                                + "import android.support.annotation.IntRange;\n"
                                + "\n"
                                + "import java.lang.annotation.Retention;\n"
                                + "import java.lang.annotation.RetentionPolicy;\n"
                                + "\n"
                                + "@SuppressWarnings({\"UnusedParameters\", \"unused\", \"SpellCheckingInspection\"})\n"
                                + "public class X {\n"
                                + "\n"
                                + "    public static final int UNRELATED = 500;\n"
                                + "\n"
                                + "    @IntDef({LENGTH_INDEFINITE, LENGTH_SHORT, LENGTH_LONG})\n"
                                + "    @IntRange(from = 10)\n"
                                + "    @Retention(RetentionPolicy.SOURCE)\n"
                                + "    public @interface Duration {}\n"
                                + "\n"
                                + "    public static final int LENGTH_INDEFINITE = -2;\n"
                                + "    public static final int LENGTH_SHORT = -1;\n"
                                + "    public static final int LENGTH_LONG = 0;\n"
                                + "    public void setDuration(@Duration int duration) {\n"
                                + "    }\n"
                                + "\n"
                                + "    public void test() {\n"
                                + "        setDuration(UNRELATED); /// OK within range\n"
                                + "        setDuration(-5); // ERROR (not right int def or value\n"
                                + "        setDuration(8); // ERROR (not matching number range)\n"
                                + "        setDuration(8000); // OK (@IntRange applies)\n"
                                + "        setDuration(LENGTH_INDEFINITE); // OK (@IntDef)\n"
                                + "        setDuration(LENGTH_LONG); // OK (@IntDef)\n"
                                + "        setDuration(LENGTH_SHORT); // OK (@IntDef)\n"
                                + "    }\n"
                                + "}\n"),
                        mSupportClasspath,
                        mSupportJar

                ));
    }

    public void testMultipleProjects() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=182179
        // 182179: Lint gives erroneous @StringDef errors in androidTests
        assertEquals(""
                + "src/test/zpkg/SomeClassTest.java:10: Error: Must be one of: SomeClass.MY_CONSTANT [WrongConstant]\n"
                + "        SomeClass.doSomething(\"error\");\n"
                + "                              ~~~~~~~\n"
                + "1 errors, 0 warnings\n",

                lintProject(
                        getManifestWithPermissions(14, 23),
                        java("src/test/pkg/SomeClass.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.support.annotation.StringDef;\n"
                                + "import android.util.Log;\n"
                                + "\n"
                                + "import java.lang.annotation.Documented;\n"
                                + "import java.lang.annotation.Retention;\n"
                                + "import java.lang.annotation.RetentionPolicy;\n"
                                + "\n"
                                + "public class SomeClass {\n"
                                + "\n"
                                + "    public static final String MY_CONSTANT = \"foo\";\n"
                                + "\n"
                                + "    public static void doSomething(@MyTypeDef final String myString) {\n"
                                + "        Log.v(\"tag\", myString);\n"
                                + "    }\n"
                                + "\n"
                                + "\n"
                                + "    /**\n"
                                + "     * Defines the possible values for state type.\n"
                                + "     */\n"
                                + "    @StringDef({MY_CONSTANT})\n"
                                + "    @Documented\n"
                                + "    @Retention(RetentionPolicy.SOURCE)\n"
                                + "    public @interface MyTypeDef {\n"
                                + "\n"
                                + "    }\n"
                                + "}"),
                        // test.zpkg: alphabetically after test.pkg: We want to make sure
                        // that the SomeClass source unit is disposed before we try to
                        // process SomeClassTest and try to resolve its SomeClass.MY_CONSTANT
                        // @IntDef reference
                        java("src/test/zpkg/SomeClassTest.java", ""
                                + "package test.zpkg;\n"
                                + "\n"
                                + "import test.pkg.SomeClass;\n"
                                + "import junit.framework.TestCase;\n"
                                + "\n"
                                + "public class SomeClassTest extends TestCase {\n"
                                + "\n"
                                + "    public void testDoSomething() {\n"
                                + "        SomeClass.doSomething(SomeClass.MY_CONSTANT);\n"
                                + "        SomeClass.doSomething(\"error\");\n"
                                + "    }\n"
                                + "}"),
                        mSupportClasspath,
                        mSupportJar

                ));
    }

    @SuppressWarnings({"InstantiationOfUtilityClass", "ResultOfObjectAllocationIgnored"})
    public void testMultipleResourceTypes() throws Exception {
        // Regression test for
        //   https://code.google.com/p/android/issues/detail?id=187181
        // Make sure that parameters which specify multiple resource types are handled
        // correctly.
        assertEquals(""
                + "src/test/pkg/ResourceTypeTest.java:14: Error: Expected resource of type drawable or string [ResourceType]\n"
                + "        new ResourceTypeTest(res, R.raw.my_raw_file); // ERROR\n"
                + "                                  ~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

                lintProject(
                        java("src/test/pkg/ResourceTypeTest.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.content.res.Resources;\n"
                                + "import android.support.annotation.DrawableRes;\n"
                                + "import android.support.annotation.StringRes;\n"
                                + "\n"
                                + "public class ResourceTypeTest {\n"
                                + "    public ResourceTypeTest(Resources res, @DrawableRes @StringRes int id) {\n"
                                + "    }\n"
                                + "\n"
                                + "    public static void test(Resources res) {\n"
                                + "        new ResourceTypeTest(res, R.drawable.ic_announcement_24dp); // OK\n"
                                + "        new ResourceTypeTest(res, R.string.action_settings); // OK\n"
                                + "        new ResourceTypeTest(res, R.raw.my_raw_file); // ERROR\n"
                                + "    }\n"
                                + "\n"
                                + "    public static final class R {\n"
                                + "        public static final class drawable {\n"
                                + "            public static final int ic_announcement_24dp = 0x7f0a0000;\n"
                                + "        }\n"
                                + "        public static final class string {\n"
                                + "            public static final int action_settings = 0x7f0a0001;\n"
                                + "        }\n"
                                + "        public static final class raw {\n"
                                + "            public static final int my_raw_file = 0x7f0a0002;\n"
                                + "        }\n"
                                + "    }"
                                + "}"),
                        mSupportClasspath,
                        mSupportJar

                ));
    }

    @SuppressWarnings({"InstantiationOfUtilityClass", "ResultOfObjectAllocationIgnored"})
    public void testAnyRes() throws Exception {
        // Make sure error messages for @AnyRes are handled right since it's now an
        // enum set containing all possible resource types
        assertEquals(""
                + "src/test/pkg/AnyResTest.java:14: Error: Expected resource identifier (R.type.name) [ResourceType]\n"
                + "        new AnyResTest(res, 52); // ERROR\n"
                + "                            ~~\n"
                + "1 errors, 0 warnings\n",

                lintProject(
                        java("src/test/pkg/AnyResTest.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.content.res.Resources;\n"
                                + "import android.support.annotation.AnyRes;\n"
                                + "\n"
                                + "public class AnyResTest {\n"
                                + "    public AnyResTest(Resources res, @AnyRes int id) {\n"
                                + "    }\n"
                                + "\n"
                                + "    public static void test(Resources res) {\n"
                                + "        new AnyResTest(res, R.drawable.ic_announcement_24dp); // OK\n"
                                + "        new AnyResTest(res, R.string.action_settings); // OK\n"
                                + "        new AnyResTest(res, R.raw.my_raw_file); // OK\n"
                                + "        new AnyResTest(res, 52); // ERROR\n"
                                + "    }\n"
                                + "\n"
                                + "    public static final class R {\n"
                                + "        public static final class drawable {\n"
                                + "            public static final int ic_announcement_24dp = 0x7f0a0000;\n"
                                + "        }\n"
                                + "        public static final class string {\n"
                                + "            public static final int action_settings = 0x7f0a0001;\n"
                                + "        }\n"
                                + "        public static final class raw {\n"
                                + "            public static final int my_raw_file = 0x7f0a0002;\n"
                                + "        }\n"
                                + "    }"
                                + "}"),
                        mSupportClasspath,
                        mSupportJar

                ));
    }

    /**
     * Test @IntDef when applied to multiple elements like arrays or varargs.
     */
    public void testIntDefMultiple() throws Exception {
        assertEquals(""
                + "src/test/pkg/IntDefMultiple.java:24: Error: Must be one of: IntDefMultiple.VALUE_A, IntDefMultiple.VALUE_B [WrongConstant]\n"
                + "        restrictedArray(/*Must be one of: X.VALUE_A, X.VALUE_B*/new int[]{VALUE_A, 0, VALUE_B}/**/); // ERROR;\n"
                + "                                                                                   ~\n"
                + "src/test/pkg/IntDefMultiple.java:26: Error: Must be one of: IntDefMultiple.VALUE_A, IntDefMultiple.VALUE_B [WrongConstant]\n"
                + "        restrictedArray(/*Must be one of: X.VALUE_A, X.VALUE_B*/INVALID_ARRAY/**/); // ERROR\n"
                + "                                                                ~~~~~~~~~~~~~\n"
                + "src/test/pkg/IntDefMultiple.java:27: Error: Must be one of: IntDefMultiple.VALUE_A, IntDefMultiple.VALUE_B [WrongConstant]\n"
                + "        restrictedArray(/*Must be one of: X.VALUE_A, X.VALUE_B*/INVALID_ARRAY2/**/); // ERROR\n"
                + "                                                                ~~~~~~~~~~~~~~\n"
                + "src/test/pkg/IntDefMultiple.java:31: Error: Must be one of: IntDefMultiple.VALUE_A, IntDefMultiple.VALUE_B [WrongConstant]\n"
                + "        restrictedEllipsis(VALUE_A, /*Must be one of: X.VALUE_A, X.VALUE_B*/0/**/, VALUE_B); // ERROR\n"
                + "                                                                            ~\n"
                + "src/test/pkg/IntDefMultiple.java:32: Error: Must be one of: IntDefMultiple.VALUE_A, IntDefMultiple.VALUE_B [WrongConstant]\n"
                + "        restrictedEllipsis(/*Must be one of: X.VALUE_A, X.VALUE_B*/0/**/); // ERROR\n"
                + "                                                                   ~\n"
                + "5 errors, 0 warnings\n",
                lintProject(
                        java("src/test/pkg/IntDefMultiple.java", ""
                                + "package test.pkg;\n"
                                + "import android.support.annotation.IntDef;\n"
                                + "\n"
                                + "public class IntDefMultiple {\n"
                                + "    private static final int VALUE_A = 0;\n"
                                + "    private static final int VALUE_B = 1;\n"
                                + "\n"
                                + "    private static final int[] VALID_ARRAY = {VALUE_A, VALUE_B};\n"
                                + "    private static final int[] INVALID_ARRAY = {VALUE_A, 0, VALUE_B};\n"
                                + "    private static final int[] INVALID_ARRAY2 = {10};\n"
                                + "\n"
                                + "    @IntDef({VALUE_A, VALUE_B})\n"
                                + "    public @interface MyIntDef {}\n"
                                + "\n"
                                + "    @MyIntDef\n"
                                + "    public int a = 0;\n"
                                + "\n"
                                + "    @MyIntDef\n"
                                + "    public int[] b;\n"
                                + "\n"
                                + "    public void testCall() {\n"
                                + "        restrictedArray(new int[]{VALUE_A}); // OK\n"
                                + "        restrictedArray(new int[]{VALUE_A, VALUE_B}); // OK\n"
                                + "        restrictedArray(/*Must be one of: X.VALUE_A, X.VALUE_B*/new int[]{VALUE_A, 0, VALUE_B}/**/); // ERROR;\n"
                                + "        restrictedArray(VALID_ARRAY); // OK\n"
                                + "        restrictedArray(/*Must be one of: X.VALUE_A, X.VALUE_B*/INVALID_ARRAY/**/); // ERROR\n"
                                + "        restrictedArray(/*Must be one of: X.VALUE_A, X.VALUE_B*/INVALID_ARRAY2/**/); // ERROR\n"
                                + "\n"
                                + "        restrictedEllipsis(VALUE_A); // OK\n"
                                + "        restrictedEllipsis(VALUE_A, VALUE_B); // OK\n"
                                + "        restrictedEllipsis(VALUE_A, /*Must be one of: X.VALUE_A, X.VALUE_B*/0/**/, VALUE_B); // ERROR\n"
                                + "        restrictedEllipsis(/*Must be one of: X.VALUE_A, X.VALUE_B*/0/**/); // ERROR\n"
                                + "        // Suppressed via older Android Studio inspection id:\n"
                                + "        //noinspection ResourceType\n"
                                + "        restrictedEllipsis(0); // SUPPRESSED\n"
                                + "    }\n"
                                + "\n"
                                + "    private void restrictedEllipsis(@MyIntDef int... test) {}\n"
                                + "\n"
                                + "    private void restrictedArray(@MyIntDef int[] test) {}\n"
                                + "}"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    /**
     * Test @IntRange and @FloatRange support annotation applied to arrays and vargs.
     */
    public void testRangesMultiple() throws Exception {
        assertEquals(""
                        + "src/test/pkg/RangesMultiple.java:20: Error: Value must be ≥ 10.0 (was 5) [Range]\n"
                        + "        a[0] = /*Value must be ≥ 10.0 and ≤ 15.0 (was 5f)*/5f/**/; // ERROR\n"
                        + "                                                           ~~\n"
                        + "src/test/pkg/RangesMultiple.java:22: Error: Value must be ≥ 10.0 (was 5.0) [Range]\n"
                        + "        varargsFloat(15.0f, 10.0f, /*Value must be ≥ 10.0 and ≤ 15.0 (was 5.0f)*/5.0f/**/); // ERROR\n"
                        + "                                                                                 ~~~~\n"
                        + "src/test/pkg/RangesMultiple.java:24: Error: Value must be ≥ 10.0 (was 5.0) [Range]\n"
                        + "        restrictedFloatArray(/*Value must be ≥ 10.0 and ≤ 15.0*/INVALID_FLOAT_ARRAY/**/); // ERROR\n"
                        + "                                                                ~~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/RangesMultiple.java:26: Error: Value must be ≤ 15.0 (was 500.0) [Range]\n"
                        + "        restrictedFloatArray(/*Value must be ≥ 10.0 and ≤ 15.0*/new float[]{12.0f, 500.0f}/**/); // ERROR\n"
                        + "                                                                ~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/RangesMultiple.java:30: Error: Value must be ≥ 10 (was 5) [Range]\n"
                        + "        b[0] = /*Value must be ≥ 10 and ≤ 500 (was 5)*/5/**/; // ERROR\n"
                        + "                                                       ~\n"
                        + "src/test/pkg/RangesMultiple.java:32: Error: Value must be ≤ 500 (was 510) [Range]\n"
                        + "        varargsInt(15, 10, /*Value must be ≥ 10 and ≤ 500 (was 510)*/510/**/); // ERROR\n"
                        + "                                                                     ~~~\n"
                        + "src/test/pkg/RangesMultiple.java:34: Error: Value must be ≥ 10 (was 5) [Range]\n"
                        + "        restrictedIntArray(/*Value must be ≥ 10 and ≤ 500*/INVALID_INT_ARRAY/**/); // ERROR\n"
                        + "                                                           ~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/RangesMultiple.java:36: Error: Value must be ≥ 10 (was 0) [Range]\n"
                        + "        restrictedIntArray(/*Value must be ≥ 10 and ≤ 500*/new int[]{0, 500}/**/); // ERROR\n"
                        + "                                                           ~~~~~~~~~~~~~~~~~\n"
                        + "8 errors, 0 warnings\n",
                lintProject(
                        java("src/test/pkg/RangesMultiple.java", ""
                                + "package test.pkg;\n"
                                + "import android.support.annotation.FloatRange;\n"
                                + "import android.support.annotation.IntRange;\n"
                                + "\n"
                                + "public class RangesMultiple {\n"
                                + "    private static final float[] VALID_FLOAT_ARRAY = new float[] {10.0f, 12.0f, 15.0f};\n"
                                + "    private static final float[] INVALID_FLOAT_ARRAY = new float[] {10.0f, 12.0f, 5.0f};\n"
                                + "\n"
                                + "    private static final int[] VALID_INT_ARRAY = new int[] {15, 120, 500};\n"
                                + "    private static final int[] INVALID_INT_ARRAY = new int[] {15, 120, 5};\n"
                                + "\n"
                                + "    @FloatRange(from = 10.0, to = 15.0)\n"
                                + "    public float[] a;\n"
                                + "\n"
                                + "    @IntRange(from = 10, to = 500)\n"
                                + "    public int[] b;\n"
                                + "\n"
                                + "    public void testCall() {\n"
                                + "        a = new float[2];\n"
                                + "        a[0] = /*Value must be ≥ 10.0 and ≤ 15.0 (was 5f)*/5f/**/; // ERROR\n"
                                + "        a[1] = 14f; // OK\n"
                                + "        varargsFloat(15.0f, 10.0f, /*Value must be ≥ 10.0 and ≤ 15.0 (was 5.0f)*/5.0f/**/); // ERROR\n"
                                + "        restrictedFloatArray(VALID_FLOAT_ARRAY); // OK\n"
                                + "        restrictedFloatArray(/*Value must be ≥ 10.0 and ≤ 15.0*/INVALID_FLOAT_ARRAY/**/); // ERROR\n"
                                + "        restrictedFloatArray(new float[]{10.5f, 14.5f}); // OK\n"
                                + "        restrictedFloatArray(/*Value must be ≥ 10.0 and ≤ 15.0*/new float[]{12.0f, 500.0f}/**/); // ERROR\n"
                                + "\n"
                                + "\n"
                                + "        b = new int[2];\n"
                                + "        b[0] = /*Value must be ≥ 10 and ≤ 500 (was 5)*/5/**/; // ERROR\n"
                                + "        b[1] = 100; // OK\n"
                                + "        varargsInt(15, 10, /*Value must be ≥ 10 and ≤ 500 (was 510)*/510/**/); // ERROR\n"
                                + "        restrictedIntArray(VALID_INT_ARRAY); // OK\n"
                                + "        restrictedIntArray(/*Value must be ≥ 10 and ≤ 500*/INVALID_INT_ARRAY/**/); // ERROR\n"
                                + "        restrictedIntArray(new int[]{50, 500}); // OK\n"
                                + "        restrictedIntArray(/*Value must be ≥ 10 and ≤ 500*/new int[]{0, 500}/**/); // ERROR\n"
                                + "    }\n"
                                + "\n"
                                + "    public void restrictedIntArray(@IntRange(from = 10, to = 500) int[] a) {\n"
                                + "    }\n"
                                + "\n"
                                + "    public void varargsInt(@IntRange(from = 10, to = 500) int... a) {\n"
                                + "    }\n"
                                + "\n"
                                + "    public void varargsFloat(@FloatRange(from = 10.0, to = 15.0) float... a) {\n"
                                + "    }\n"
                                + "\n"
                                + "    public void restrictedFloatArray(@FloatRange(from = 10.0, to = 15.0) float[] a) {\n"
                                + "    }\n"
                                + "}\n"
                                + "\n"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testNegativeFloatRange() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=219246
        // Make sure we correctly handle negative ranges for floats
        assertEquals(""
                + "src/test/pkg/FloatRangeTest.java:8: Error: Value must be \u2265 -90.0 (was -150.0) [Range]\n"
                + "        call(-150.0); // ERROR\n"
                + "             ~~~~~~\n"
                + "src/test/pkg/FloatRangeTest.java:10: Error: Value must be \u2264 -5.0 (was -3.0) [Range]\n"
                + "        call(-3.0); // ERROR\n"
                + "             ~~~~\n"
                + "2 errors, 0 warnings\n",

                lintProject(
                        java("src/test/pkg/FloatRangeTest.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.support.annotation.FloatRange;\n"
                                + "\n"
                                + "@SuppressWarnings(\"unused\")\n"
                                + "public class FloatRangeTest {\n"
                                + "    public void test() {\n"
                                + "        call(-150.0); // ERROR\n"
                                + "        call(-45.0); // OK\n"
                                + "        call(-3.0); // ERROR\n"
                                + "    }\n"
                                + "\n"
                                + "    private void call(@FloatRange(from=-90.0, to=-5.0) double arg) {\n"
                                + "    }\n"
                                + "}\n"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testIntDefInBuilder() throws Exception {
        // Ensure that we only check constants, not instance fields, when passing
        // fields as arguments to typedef parameters.
        assertEquals("No warnings.",
                lintProject(
                        java("src/test/pkg/Product.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.support.annotation.IntDef;\n"
                                + "\n"
                                + "import java.lang.annotation.Retention;\n"
                                + "import java.lang.annotation.RetentionPolicy;\n"
                                + "\n"
                                + "public class Product {\n"
                                + "    @IntDef({\n"
                                + "         STATUS_AVAILABLE, STATUS_BACK_ORDER, STATUS_UNAVAILABLE\n"
                                + "    })\n"
                                + "    @Retention(RetentionPolicy.SOURCE)\n"
                                + "    public @interface Status {\n"
                                + "    }\n"
                                + "    public static final int STATUS_AVAILABLE = 1;\n"
                                + "    public static final int STATUS_BACK_ORDER = 2;\n"
                                + "    public static final int STATUS_UNAVAILABLE = 3;\n"
                                + "\n"
                                + "    @Status\n"
                                + "    private final int mStatus;\n"
                                + "    private final String mName;\n"
                                + "\n"
                                + "    private Product(String name, @Status int status) {\n"
                                + "        mName = name;\n"
                                + "        mStatus = status;\n"
                                + "    }\n"
                                + "    public static class Builder {\n"
                                + "        @Status\n"
                                + "        private int mStatus;\n"
                                + "        private final int mStatus2 = STATUS_AVAILABLE;\n"
                                + "        @Status static final int DEFAULT_STATUS = Product.STATUS_UNAVAILABLE;\n"
                                + "        private String mName;\n"
                                + "\n"
                                + "        public Builder(String name, @Status int status) {\n"
                                + "            mName = name;\n"
                                + "            mStatus = status;\n"
                                + "        }\n"
                                + "\n"
                                + "        public Builder setStatus(@Status int status) {\n"
                                + "            mStatus = status;\n"
                                + "            return this;\n"
                                + "        }\n"
                                + "\n"
                                + "        public Product build() {\n"
                                + "            return new Product(mName, mStatus);\n"
                                + "        }\n"
                                + "\n"
                                + "        public Product build2() {\n"
                                + "            return new Product(mName, mStatus2);\n"
                                + "        }\n"
                                + "\n"
                                + "        public static Product build3() {\n"
                                + "            return new Product(\"\", DEFAULT_STATUS);\n"
                                + "        }\n"
                                + "    }\n"
                                + "}\n"),
                        mSupportClasspath,
                        mSupportJar
                )
        );
    }

    public void testObtainStyledAttributes() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=201882
        // obtainStyledAttributes normally expects a styleable but you can also supply a
        // custom int array
        assertEquals("No warnings.",
                lintProject(
                        java("src/test/pkg/ObtainTest.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.app.Activity;\n"
                                + "import android.content.Context;\n"
                                + "import android.content.res.TypedArray;\n"
                                + "import android.graphics.Color;\n"
                                + "import android.util.AttributeSet;\n"
                                + "\n"
                                + "public class ObtainTest {\n"
                                + "    public static void test1(Activity activity, float[] foregroundHsv, float[] backgroundHsv) {\n"
                                + "        TypedArray attributes = activity.obtainStyledAttributes(\n"
                                + "                new int[] {\n"
                                + "                        R.attr.setup_wizard_navbar_theme,\n"
                                + "                        android.R.attr.colorForeground,\n"
                                + "                        android.R.attr.colorBackground });\n"
                                + "        Color.colorToHSV(attributes.getColor(1, 0), foregroundHsv);\n"
                                + "        Color.colorToHSV(attributes.getColor(2, 0), backgroundHsv);\n"
                                + "        attributes.recycle();\n"
                                + "    }\n"
                                + "\n"
                                + "    public static void test2(Context context, AttributeSet attrs, int defStyle) {\n"
                                + "        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BezelImageView,\n"
                                + "                defStyle, 0);\n"
                                + "        a.getDrawable(R.styleable.BezelImageView_maskDrawable);\n"
                                + "        a.recycle();\n"
                                + "    }\n"
                                + "\n"
                                + "    public void test(Context context, AttributeSet attrs) {\n"
                                + "        int[] attrsArray = new int[] {\n"
                                + "                android.R.attr.entries, // 0\n"
                                + "                android.R.attr.labelFor\n"
                                + "        };\n"
                                + "        TypedArray ta = context.obtainStyledAttributes(attrs, attrsArray);\n"
                                + "        if(null == ta) {\n"
                                + "            return;\n"
                                + "        }\n"
                                + "        CharSequence[] entries = ta.getTextArray(0);\n"
                                + "        CharSequence label = ta.getText(1);\n"
                                + "    }\n"
                                + "\n"
                                + "    public static class R {\n"
                                + "        public static class attr {\n"
                                + "            public static final int setup_wizard_navbar_theme = 0x7f01003b;\n"
                                + "        }\n"
                                + "        public static class styleable {\n"
                                + "            public static final int[] BezelImageView = {\n"
                                + "                    0x7f01005d, 0x7f01005e, 0x7f01005f\n"
                                + "            };\n"
                                + "            public static final int BezelImageView_maskDrawable = 0;\n"
                                + "        }\n"
                                + "    }\n"
                                + "}\n")));
    }

    @SuppressWarnings("ALL") // sample code with warnings
    public void testRestrictToSubClass() throws Exception {
        assertEquals(""
                + "src/test/pkg/RestrictToSubclassTest.java:20: Error: Class1.onSomething can only be called from subclasses [RestrictedApi]\n"
                + "            cls.onSomething(); // ERROR: Not from subclass\n"
                + "                ~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",
                lintProject(
                        java(""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.support.annotation.RestrictTo;\n"
                                + "\n"
                                + "public class RestrictToSubclassTest {\n"
                                + "    public static class Class1 {\n"
                                + "        @RestrictTo(RestrictTo.Scope.SUBCLASSES)\n"
                                + "        public void onSomething() {\n"
                                + "        }\n"
                                + "    }\n"
                                + "\n"
                                + "    public static class SubClass extends Class1 {\n"
                                + "        public void test1() {\n"
                                + "            onSomething(); // OK: Call from subclass\n"
                                + "        }\n"
                                + "    }\n"
                                + "\n"
                                + "    public static class NotSubClass {\n"
                                + "        public void test2(Class1 cls) {\n"
                                + "            cls.onSomething(); // ERROR: Not from subclass\n"
                                + "        }\n"
                                + "    }\n"
                                + "}\n"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testRestrictToGroupId() {
        ProjectDescription project = project().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import library.pkg.internal.InternalClass;\n"
                        + "import library.pkg.Library;\n"
                        + "import library.pkg.PrivateClass;\n"
                        + "\n"
                        + "public class TestLibrary {\n"
                        + "    public void test() {\n"
                        + "        Library.method(); // OK\n"
                        + "        Library.privateMethod(); // ERROR\n"
                        + "        PrivateClass.method(); // ERROR\n"
                        + "        InternalClass.method(); // ERROR\n"
                        + "    }\n"
                        + "}\n"),

                        /*
                        Compiled version of these 5 files (and the RestrictTo annotation);
                        we need to use a compiled version of these to mimic the Gradle artifact
                        layout (which is relevant for this lint check which enforces restrictions
                        across library compilation units) :

                        Library.java:
                            package library.pkg;

                            import android.support.annotation.RestrictTo;

                            public class Library {
                                public static void method() {
                                }

                                @RestrictTo(RestrictTo.Scope.GROUP_ID)
                                public static void privateMethod() {
                                }
                            }

                        PrivateClass.java:
                            package library.pkg;

                            import android.support.annotation.RestrictTo;

                            @RestrictTo(RestrictTo.Scope.GROUP_ID)
                            public class PrivateClass {
                                public static void method() {
                                }
                            }

                        package-info.java:
                            @RestrictTo(RestrictTo.Scope.GROUP_ID)
                            package library.pkg.internal;

                            import android.support.annotation.RestrictTo;

                        InternalClass.java:
                            package library.pkg.internal;

                            public class InternalClass {
                                public static void method() {
                                }
                            }
                         */
                base64gzip("libs/exploded-aar/my.group.id/mylib/25.0.0-SNAPSHOT/jars/classes.jar",
                        ""
                                + "H4sIAAAAAAAAAJVXB1RT2RYNxdBCMQSQXpQS6eWLCogBiXTpMhQNhCIQEghF"
                                + "iqJIkSYd6dXQJnRHRIpSIjXggChSVFAQEAQElCb4wfF/Ez4w829Wsl5Wzt65"
                                + "b5/zzrnbQIeKGgKgpaUFXDvLqwUgWXQAaoCehglMUksfLr3VCQBQAQx0aGh3"
                                + "fqL8GWKwLxiy/f4vWA+mrwXXMDaR0oMv6RG7dHUkpXoZdSSP9xCf3TeS6Zcf"
                                + "ncBKaetJaOn1ev1OTWc+xUlI54ZeWeUQFpnEHT85ycE2ziF8F5Dksoj9jKX4"
                                + "sQnd2Nvu6tt/ce7nJugBgO2NWe/axM5eUU62WBusr/T+USCSKDcXxwMi2XZF"
                                + "OqE97bFoG5T0Lx12Q8T2g2j9vFBH2Xh4SCF3PkMuVOq3yEDqB3B0jIHsJ+As"
                                + "VDKjgYf788FG+UWgnDl95wdS6Fs8HOHPbNVsR6lWxQj3LQVTfFoy367NSdwI"
                                + "aLgBqGmGhdJuQNehwOBx+MAQI13v4FoiS0xaqtAygzW+w8/0+IaATkunTa9J"
                                + "n27+i4C3EZWWU3fdLI5YWzhdJjx/bzU1BLXUOdo63NLeXt3wodCsUzKNU7AO"
                                + "87La6OFpyPdKa6mJ+8TD93ROrj1JklmgsLFT4bg6YWTK1Da1QLPpefZxuOyY"
                                + "ngyrx8o7xmWuMuPWY2GHvY8s3aFQsSOqzG4Fx1SksuAuwzDt6oNvFHdyaGHH"
                                + "7jqzLY8tBWkh7RZQdD8B3WyQLjaO9pJOaAfMX/olXVDRoYKxXMe3Rjr2R4YI"
                                + "mN2yFXRn6FKIyOOhjYs0Y6+6ytkiMjNbWXd19PL9BZrVewRTmMR7UMu1+tSV"
                                + "lJWYAUVA4Sv9ijiEsQDvcKupZz4j3bKLafeLRGBa9myH7gNtRPmVk1OOnY9e"
                                + "XEoO7Ul1NYBZfqU8rxgh2vMxfFnP2pIDtx5iDztz0jpESQky9UenQqJA5ERN"
                                + "lVfizARxXCpqMUtoACFbFjQ/0/6qQQq+8jBmLb/xcW++8ZzGy6Xifp72SrnF"
                                + "eUXKwQHVPrGsTsNT944fm2Lg+a5uPOqW+4n3LIJOtZqHt9ISJvpBQggvG09D"
                                + "23zaT/O9wp0EqeoK7+oNuh1J23U9i7e2Bcs7UFLOXZLq/nX9U0TDDu0dETNd"
                                + "FaK5kotk/Vggip5MfGyGQtoIq7Cup0GeIneq9WtvZ2XdzJ7QWhilWTW01QMT"
                                + "gjRRU79ffDHfUGx75szbNxSziMowCQde4qkiGchXXLx3tmBddUBLLH76LuLI"
                                + "Sj7YUasOSmTDhPanLns1tnCj7SxW0k9tCCBDq/g7C8Y+DEZmzak8uX21o05y"
                                + "NggtGwWHe9ewFW7WDmkmJrkaXuTl1Kk6baJtic43s3v40eOFnEV1/clwIdeS"
                                + "tifZDaFhpXGBDX3Lgddu3w53MxD24ZwKg2NntOmMGXx5GBaHuIguj/AibbkJ"
                                + "rWtfnlpvhuX6FLOyhiFT0R+/PhwKGOedE7qt3NIvbSmfaBTKgfWaz1Tk10Dw"
                                + "SsigtpCg0KTAV+ZqoQH2flWIaV0Ig4lvIufEt1bxVwyBIJ6IkEus6pDXZ/jg"
                                + "cYsy8MdP2Zb1L7YH0PLmPTrX9U2Pu0nsKXhMtvb9QwSlcYrIb4+UOkAiUI7p"
                                + "0mBfXR6Txz+6Ws9dmWKf7dSlUh6UPr6900fSSJS+YAbOAIFAJJBCCugAvOQN"
                                + "POMNDIADT6QT64lgJTjQgbFbBg5kn/yWaAIHBmQT602e9I3g+vNiCTienLDY"
                                + "CwFYW31zW0ORMGY2O2hHu4SDXexiR4fucW2J3/ul2/UHVgIrMj3AkZGlnJw1"
                                + "t5Z0IuONX8vBuKjjhL9UgbvAvkfMj5hzch5zS21sFwAYFWQnpCSXzWcnJqVZ"
                                + "Nee0N1IvU9LKUAoaqMbaUS0uN10DAs1KP4GiuANBIkBvgQ4gu53DKCBemeUQ"
                                + "KANkhfg0gjBV/qGOzrXuw33b9857YHHvVscA6+Rt42lPok6SYZc2lSxEpTLG"
                                + "yeUB89tSbtjnkJ7mfCFtDShEsrTWXXBa4Y/i5zg7uUOqXUmjzcwL9whKxlA0"
                                + "dUOsf0KCf0+HMH82Zh4QXZGlTtnFZMZTIoe7gIv/mihYed//WWzJdBlCTAEH"
                                + "HhirOEqUnF8NTuQeer+loih3+kaQ1nAyhXr+WFf0doHzqnwZKYhDojW6J2Fu"
                                + "ukB1m43arliXmT993hnBiX3Pyloo+D1CncVNzJg0rO/YC3SMWkFdHyVZxHV4"
                                + "B+HLeqtmgGNXcHe81paHXMMbuaYHLfWii2VX1Mdy1IarC8/bJZeLC+uiRDRn"
                                + "RjEW6bLpEPmLpel1H5CbQjHHjED4J91dBlEO7tfL/Ob0HAWUZ3I6K74zq4aW"
                                + "qnohqeSXEwyJNzWY72UUJOgkGep9arjVQBhgmvAEFcTlZmQrCETyvJBwKBz/"
                                + "l66QSBU/AeQ7gaw8RTSWBFvkmF4DsJoTwOy03YeNhOIWB4c4drI2uGr81Xk7"
                                + "Y+6UpJO6Wvl/J7UN2g6LcbKT3j+KhSTKw8vNDYP1PCCae49oGzQa42nj6YRB"
                                + "k8zs1/rcZEDZg4FG9h6eWCekpwnmmDES42b/s6qM5y88N4CoxHi5XKxlbDtH"
                                + "s8rtSfVb6s00VJNeRqjdYXUaus9BvokRwq9n+ry+WcuBZbkYszfvRa0kaQiC"
                                + "39eOv8m8Oq+yMjIwzK/65BpNRXChlpjYx5SW+gg6NobTfWJaSOf2KYiCfWte"
                                + "qPt0Fxe2T9WokpDTMDTyym8JvXlIfaRqM7oFUcIwRu3CN++dKdWilhDZ0Hh1"
                                + "hYguf/456M9xtVVBZuvLKen6iiArJdSavKFXyBULRry97NTLl0sPHF5XcVH3"
                                + "p37y89DZEm/gjZd2b2/IglSZizFZ4kvW/BUMC6bpJ73KG2aoJR/P9IZzxaHr"
                                + "HYs40mi+xabzcWwUaFDbPjHiqdKrnh4eGf4QKD5IIwlpGo6O2tS4IG9qPRaK"
                                + "B7Eme16WRht7rTblpybcrZP2ecUQBjWLpSh19g965zjpzJ03UMYk7P9A6G3c"
                                + "WwbUZL74IcctXOKxykA8NlMEJIHUeIoMXRNcZdQdCw8crkEKBJUEujth2+Vw"
                                + "0p0FSZUql2NvWeX6oHF3XlO1MWlNzN54YG9+6kZq75aZlP9DjrqLzvbrR/qW"
                                + "haEP6k3oY/n5VzOWkQmLInbA+JrQGUQ7a63OpTN+/LOscP3DG5phNvEooYh8"
                                + "6A3650Hf7KPOFfqZpW5ErUIhqapYwe/A395LMCi6i0c8Z4tPAtkTpmiKPJae"
                                + "i2rybN6zEvY9xruSl8F5JUXYX4SQCxs3/k5vL4pv1c/hPcpJYR3eeIU6yofa"
                                + "KBwTbHvdopU3P8bQRfTN+c7TQKV1FcyJPKJAiGzzxYWbBAKwe541/vHZSayU"
                                + "BVqZYC75nWHnScoYcKS/tP0UNVOT9r/dxSv+T4v3Z9kaDWu3nGXJ/CRpgS+9"
                                + "ig8TsoA00a5/iBQssIjScjKDrQsMe0Kh02itEyFbacwBN/mF9Y+bB99al3Fk"
                                + "uu8opVQ3z1+OAbhRKHGhnCvyYaZBrpS+z5LV9XP6WW6dd2h/l9J2hK9Spmlh"
                                + "7bPWsEMed0cOvq3DTexovrwgzvZ4rBrms7lGYHmRcgakVshg2Q68Vs8XnHI5"
                                + "i75nHUWgNbEMN2i6qXrI/U+6R5+tGermhovhzKlTm6i8Gk1Os/JNIadOmYU2"
                                + "MWJ69ZgvtbejqAH7lxq1102zHuLjVCWabQlKSYxt7sUw7rIltex09sCRZHFl"
                                + "8+WeBsEKRG30ecsYES/LV/VvKlBRznot9YpoQ5/lQmFwkW6hu9L1OXFKf3ma"
                                + "DKnoxmnVS0Xwm2eBkpFvA8fd4Cnqd0N8CSCIDcWmcBXYWYEpsoJ6IrE6tzGX"
                                + "sgtagqtIa7agn4GizlLIR4p1506U47N3Evcw00sLsd0CPX60QApKCIDcM/3H"
                                + "Te0YLvJFZr92Q0k9EIQMpgLY23ztMNAD9vdKv1YzgNQ57Y8CkaGmALud1P5I"
                                + "NjIkmGJfZ/XrpncoSE2DGBmFxn4Uezit3ayk52ZRMtZQyn9sP3aTkp7mOMlI"
                                + "B6kOOIDvpiE99vCR0aQc+ruD4G4u0mFMzlUN/LtjE2ky9xrfvxaKlnSY749i"
                                + "IUPF0+413PdHc5OhG/dAkw37X0LsdEzSXipLRrR2MNHew383PekTL06uM8P/"
                                + "1Z4NdA4Bd2DM2y/p7b1iGHe+/RsdMTpPtBEAAA=="
                ),
                classpath(SUPPORT_JAR_PATH,
                        "libs/exploded-aar/my.group.id/mylib/25.0.0-SNAPSHOT/jars/classes.jar"),
                mSupportJar,
                gradle(""
                        + "apply plugin: 'com.android.application'\n"
                        + "\n"
                        + "dependencies {\n"
                        + "    compile 'my.group.id:mylib:25.0.0-SNAPSHOT'\n"
                        + "}")
        );
        lint().projects(project).run().expect(""
                + "src/main/java/test/pkg/TestLibrary.java:10: Error: Library.privateMethod can only be called from within the same library group (groupId=my.group.id) [RestrictedApi]\n"
                + "        Library.privateMethod(); // ERROR\n"
                + "                ~~~~~~~~~~~~~\n"
                + "src/main/java/test/pkg/TestLibrary.java:11: Error: PrivateClass can only be called from within the same library group (groupId=my.group.id) [RestrictedApi]\n"
                + "        PrivateClass.method(); // ERROR\n"
                + "        ~~~~~~~~~~~~\n"
                + "src/main/java/test/pkg/TestLibrary.java:12: Error: InternalClass.method can only be called from within the same library group (groupId=my.group.id) [RestrictedApi]\n"
                + "        InternalClass.method(); // ERROR\n"
                + "                      ~~~~~~\n"
                + "3 errors, 0 warnings\n");
    }

    @SuppressWarnings("ALL") // sample code with warnings
    public void testRestrictToTests() throws Exception {
        assertEquals(""
                + "src/test/pkg/ProductionCode.java:9: Error: ProductionCode.testHelper2 can only be called from tests [RestrictedApi]\n"
                + "        testHelper2(); // ERROR\n"
                + "        ~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",
                lintProject(
                        java(""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.support.annotation.RestrictTo;\n"
                                + "import android.support.annotation.VisibleForTesting;\n"
                                + "\n"
                                + "public class ProductionCode {\n"
                                + "    public void code() {\n"
                                + "        testHelper1(); // ERROR? (We currently don't flag @VisibleForTesting; it deals with *visibility*)\n"
                                + "        testHelper2(); // ERROR\n"
                                + "    }\n"
                                + "\n"
                                + "    @VisibleForTesting\n"
                                + "    public void testHelper1() {\n"
                                + "        testHelper1(); // OK\n"
                                + "        code(); // OK\n"
                                + "    }\n"
                                + "\n"
                                + "    @RestrictTo(RestrictTo.Scope.TESTS)\n"
                                + "    public void testHelper2() {\n"
                                + "        testHelper1(); // OK\n"
                                + "        code(); // OK\n"
                                + "    }\n"
                                + "}\n"),
                        // test/ prefix makes it a test folder entry:
                        java("test/test/pkg/UnitTest.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "public class UnitTest {\n"
                                + "    public void test() {\n"
                                + "        new ProductionCode().code(); // OK\n"
                                + "        new ProductionCode().testHelper1(); // OK\n"
                                + "        new ProductionCode().testHelper2(); // OK\n"
                                + "        \n"
                                + "    }\n"
                                + "}\n"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testVisibleForTesting() throws Exception {
        assertEquals(""
                + "src/test/otherpkg/OtherPkg.java:11: Error: ProductionCode.testHelper6 can only be called from tests [RestrictedApi]\n"
                + "        new ProductionCode().testHelper6(); // ERROR\n"
                + "                             ~~~~~~~~~~~\n"
                + "src/test/pkg/ProductionCode.java:27: Error: ProductionCode.testHelper6 can only be called from tests [RestrictedApi]\n"
                + "            testHelper6(); // ERROR: should only be called from tests\n"
                + "            ~~~~~~~~~~~\n"
                + "src/test/otherpkg/OtherPkg.java:8: Warning: This method should only be accessed from tests or within protected scope [VisibleForTests]\n"
                + "        new ProductionCode().testHelper3(); // ERROR\n"
                + "                             ~~~~~~~~~~~\n"
                + "src/test/otherpkg/OtherPkg.java:9: Warning: This method should only be accessed from tests or within private scope [VisibleForTests]\n"
                + "        new ProductionCode().testHelper4(); // ERROR\n"
                + "                             ~~~~~~~~~~~\n"
                + "src/test/otherpkg/OtherPkg.java:10: Warning: This method should only be accessed from tests or within package private scope [VisibleForTests]\n"
                + "        new ProductionCode().testHelper5(); // ERROR\n"
                + "                             ~~~~~~~~~~~\n"
                + "2 errors, 3 warnings\n",
                lintProject(
                        java(""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.support.annotation.VisibleForTesting;\n"
                                + "\n"
                                + "public class ProductionCode {\n"
                                + "    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)\n"
                                + "    public void testHelper3() {\n"
                                + "    }\n"
                                + "\n"
                                + "    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)\n"
                                + "    public void testHelper4() {\n"
                                + "    }\n"
                                + "\n"
                                + "    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)\n"
                                + "    public void testHelper5() {\n"
                                + "    }\n"
                                + "\n"
                                + "    @VisibleForTesting(otherwise = VisibleForTesting.NONE)\n"
                                + "    public void testHelper6() {\n"
                                + "    }\n"
                                + "\n"
                                + "    private class Local {\n"
                                + "        private void localProductionCode() {\n"
                                + "            testHelper3();\n"
                                + "            testHelper4();\n"
                                + "            testHelper5();\n"
                                + "            testHelper6(); // ERROR: should only be called from tests\n"
                                + "            \n"
                                + "        }\n"
                                + "    }\n"
                                + "}\n"),
                        java(""
                                + "package test.otherpkg;\n"
                                + "\n"
                                + "import android.support.annotation.VisibleForTesting;\n"
                                + "import test.pkg.ProductionCode;\n"
                                + "\n"
                                + "public class OtherPkg {\n"
                                + "    public void test() {\n"
                                + "        new ProductionCode().testHelper3(); // ERROR\n"
                                + "        new ProductionCode().testHelper4(); // ERROR\n"
                                + "        new ProductionCode().testHelper5(); // ERROR\n"
                                + "        new ProductionCode().testHelper6(); // ERROR\n"
                                + "        \n"
                                + "    }\n"
                                + "}\n"),
                        // test/ prefix makes it a test folder entry:
                        java("test/test/pkg/UnitTest.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "public class UnitTest {\n"
                                + "    public void test() {\n"
                                + "        new ProductionCode().testHelper3(); // OK\n"
                                + "        new ProductionCode().testHelper4(); // OK\n"
                                + "        new ProductionCode().testHelper5(); // OK\n"
                                + "        new ProductionCode().testHelper6(); // OK\n"
                                + "        \n"
                                + "    }\n"
                                + "}\n"),
                        mSupportClasspath,
                        mSupportJar

                ));
    }

    public void testVisibleForTestingIncrementally() throws Exception {
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.support.annotation.VisibleForTesting;\n"
                        + "\n"
                        + "public class ProductionCode {\n"
                        + "    @VisibleForTesting\n"
                        + "    public void testHelper() {\n"
                        + "    }\n"
                        + "}\n"),
                // test/ prefix makes it a test folder entry:
                java("test/test/pkg/UnitTest.java", ""
                        + "package test.pkg;\n"
                        + "\n"
                        + "public class UnitTest {\n"
                        + "    public void test() {\n"
                        + "        new ProductionCode().testHelper(); // OK\n"
                        + "        \n"
                        + "    }\n"
                        + "}\n"),
                mSupportClasspath,
                mSupportJar)
                .incremental("test/test/pkg/UnitTest.java")
                .run()
                .expectClean();
    }

    public void testVisibleForTestingSameCompilationUnit() throws Exception {
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.support.annotation.VisibleForTesting;\n"
                        + "\n"
                        + "public class PrivTest {\n"
                        + "    private static CredentialsProvider sCredentialsProvider = new DefaultCredentialsProvider();\n"
                        + "\n"
                        + "    static interface CredentialsProvider {\n"
                        + "        void test();\n"
                        + "    }\n"
                        + "    @VisibleForTesting\n"
                        + "    static class DefaultCredentialsProvider implements CredentialsProvider {\n"
                        + "        @Override\n"
                        + "        public void test() {\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"),
                mSupportClasspath,
                mSupportJar)
                .run()
                .expectClean();
    }

    public void testGmsHide() throws Exception {
        lint().files(
                java("" +
                        "package test.pkg;\n" +
                        "\n" +
                        "import test.pkg.internal.HiddenInPackage;\n" +
                        "\n" +
                        "public class HideTest {\n" +
                        "    public void test() {\n" +
                        "        HiddenInPackage.test(); // Error\n" +
                        "        HiddenClass.test(); // Error\n" +
                        "        PublicClass.hiddenMethod(); // Error\n" +
                        "        PublicClass.normalMethod(); // OK!\n" +
                        "    }\n" +
                        "}\n"),
                java("" +
                        // Access from within the GMS codebase should not flag errors
                        "package com.google.android.gms.foo.bar;\n" +
                        "\n" +
                        "import test.pkg.internal.HiddenInPackage;\n" +
                        "\n" +
                        "public class HideTest {\n" +
                        "    public void test() {\n" +
                        "        HiddenInPackage.test(); // Error\n" +
                        "        HiddenClass.test(); // Error\n" +
                        "        PublicClass.hiddenMethod(); // Error\n" +
                        "        PublicClass.normalMethod(); // OK!\n" +
                        "    }\n" +
                        "}\n"),
                java("" +
                        "package test.pkg.internal;\n" +
                        "\n" +
                        "public class HiddenInPackage {\n" +
                        "    public static void test() {\n" +
                        "    }\n" +
                        "}\n"),
                java("" +
                        "package test.pkg;\n" +
                        "\n" +
                        "import com.google.android.gms.common.internal.Hide;\n" +
                        "\n" +
                        "@Hide\n" +
                        "public class HiddenClass {\n" +
                        "    public static void test() {\n" +
                        "    }\n" +
                        "}\n"),
                java("" +
                        "package test.pkg;\n" +
                        "\n" +
                        "import com.google.android.gms.common.internal.Hide;\n" +
                        "\n" +
                        "public class PublicClass {\n" +
                        "    public static void normalMethod() {\n" +
                        "    }\n" +
                        "\n" +
                        "    @Hide\n" +
                        "    public static void hiddenMethod() {\n" +
                        "    }\n" +
                        "}\n"),
                java("" +
                        "package com.google.android.gms.common.internal;\n" +
                        "\n" +
                        "import java.lang.annotation.Documented;\n" +
                        "import java.lang.annotation.ElementType;\n" +
                        "import java.lang.annotation.Retention;\n" +
                        "import java.lang.annotation.RetentionPolicy;\n" +
                        "import java.lang.annotation.Target;\n" +
                        "import java.lang.annotation.Target;\n" +
                        "import static java.lang.annotation.ElementType.*;\n" +
                        "@Target({TYPE,FIELD,METHOD,CONSTRUCTOR,PACKAGE})\n" +
                        "@Retention(RetentionPolicy.CLASS)\n" +
                        "public @interface Hide {}"),
                java("src/test/pkg/package-info.java", "" +
                        "@Hide\n" +
                        "package test.pkg.internal;\n" +
                        "\n" +
                        "import com.google.android.gms.common.internal.Hide;\n"),
                // Also register the compiled version of the above package-info jar file;
                // without this we don't resolve package annotations
                base64gzip("libs/packageinfoclass.jar", "" +
                        "H4sIAAAAAAAAAAvwZmYRYeDg4GC4tYDfmwEJcDKwMPi6hjjqevq56f87xcDA" +
                        "zBDgzc4BkmKCKgnAqVkEiOGafR39PN1cg0P0fN0++5457eOtq3eR11tX69yZ" +
                        "85uDDK4YP3hapOflq+Ppe7F0FQtnxAvJI9KzpF6KLX22RE1suVZGxdJpFqKq" +
                        "ac9EtUVei758mv2p6GMRI9gtbSuDVb2ANnmhuEVhPqpbVIC4JLW4RL8gO10/" +
                        "M68ktSgvMUe/IDE5OzE9VTczLy1fLzknsbjYt9cw75CDgOt/oQOKoRmXXB6x" +
                        "pc0qWZmhpKSoqKoe8SbRNM22+c1WfveDjBYih1RcP3X/X/q/q3znvHMM9wxO" +
                        "T0itKKn4tW2d5g9nJesz/fssfhzY+eLetKnv9x5+Hb7cM+vflbiom65xK6M+" +
                        "efpEt9cER/ge1HFRW5+aHBS0Ilrq3a0pLsLmr5TXLn1S3u76yOziR4F/J+qX" +
                        "H/581+ti9oK36x4p7WXgU/6T1tI+Xy7Z6E2JQvADNlAAHM4XN1kP9N5VcAAw" +
                        "MokwoEYHLKJAcYkKUGIWXStyuIqgaLPFEa/IJoDCH9lhKigmnCQyNgK8WdlA" +
                        "6pmB8DyQPsUI4gEAH9csuq8CAAA="))
                .run()
                .expect("" +
                        "src/test/pkg/HideTest.java:7: Error: HiddenInPackage.test is marked as internal and should not be accessed from apps [RestrictedApi]\n" +
                        "        HiddenInPackage.test(); // Error\n" +
                        "                        ~~~~\n" +
                        "src/test/pkg/HideTest.java:8: Error: HiddenClass is marked as internal and should not be accessed from apps [RestrictedApi]\n" +
                        "        HiddenClass.test(); // Error\n" +
                        "        ~~~~~~~~~~~\n" +
                        "src/test/pkg/HideTest.java:9: Error: PublicClass.hiddenMethod is marked as internal and should not be accessed from apps [RestrictedApi]\n" +
                        "        PublicClass.hiddenMethod(); // Error\n" +
                        "                    ~~~~~~~~~~~~\n" +
                        "3 errors, 0 warnings\n");
    }

    public void testRequiresPermissionWithinRequires() throws Exception {
        assertEquals("No warnings.",
                lintProject(
                        java(""
                                + "package com.example.mylibrary1;\n"
                                + "\n"
                                + "import android.Manifest;\n"
                                + "import android.content.Context;\n"
                                + "import android.net.wifi.WifiInfo;\n"
                                + "import android.net.wifi.WifiManager;\n"
                                + "import android.support.annotation.RequiresPermission;\n"
                                + "\n"
                                + "public class WifiInfoUtil {\n"
                                + "    @RequiresPermission(Manifest.permission.ACCESS_WIFI_STATE)\n"
                                + "    public static WifiInfo getWifiInfo(Context context) {\n"
                                + "        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);\n"
                                + "        return wm.getConnectionInfo();\n"
                                + "    }\n"
                                + "}"),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    // TODO: http://b.android.com/220686
    public void ignore_testSnackbarDuration() throws Exception {
        assertEquals(""
                + "src/test/pkg/SnackbarTest.java:13: Error: Must be one of: Snackbar.LENGTH_INDEFINITE, Snackbar.LENGTH_SHORT, Snackbar.LENGTH_LONG or value must be \u2265 1 (was -100) [WrongConstant]\n"
                + "        makeSnackbar(-100); // ERROR\n"
                + "                     ~~~~\n"
                + "1 errors, 0 warnings\n",
                lintProject(
                        java("src/test/pkg/SnackbarTest.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.support.design.widget.Snackbar;\n"
                                + "\n"
                                + "public class SnackbarTest {\n"
                                + "    public Snackbar makeSnackbar(@Snackbar.Duration int duration) {\n"
                                + "        return null;\n"
                                + "    }\n"
                                + "\n"
                                + "    public void test() {\n"
                                + "        makeSnackbar(Snackbar.LENGTH_LONG); // OK\n"
                                + "        makeSnackbar(100); // OK\n"
                                + "        makeSnackbar(-100); // ERROR\n"
                                + "    }\n"
                                + "}\n"),
                        java("src/android/support/design/widget/Snackbar.java", ""
                                + "package android.support.design.widget;\n"
                                + "\n"
                                + "import android.support.annotation.IntDef;\n"
                                + "import android.support.annotation.IntRange;\n"
                                + "\n"
                                + "import java.lang.annotation.Retention;\n"
                                + "import java.lang.annotation.RetentionPolicy;\n"
                                + "\n"
                                + "public class Snackbar {\n"
                                // In the real class definition, this annotation is there,
                                // but in the compiled design library, since it has source
                                // retention, the @IntDef is missing and only the @IntRange
                                // remains. Therefore, it's been extracted into the external
                                // database. We don't want to count it twice so don't repeat
                                // it here:
                                //+ "    @IntDef({LENGTH_INDEFINITE, LENGTH_SHORT, LENGTH_LONG})\n"
                                + "    @IntRange(from = 1)\n"
                                + "    @Retention(RetentionPolicy.SOURCE)\n"
                                + "    public @interface Duration {}\n"
                                + "\n"
                                + "    public static final int LENGTH_INDEFINITE = -2;\n"
                                + "    public static final int LENGTH_SHORT = -1;\n"
                                + "    public static final int LENGTH_LONG = 0;\n"
                                + "}\n"),
                        mSupportClasspath,
                        mSupportJar

                ));
    }

    public void testThreadingWithinLambdas() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=223101
        assertEquals("No warnings.",
                lintProject(
                        java(""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.app.Activity;\n"
                                + "import android.os.Bundle;\n"
                                + "import android.support.annotation.WorkerThread;\n"
                                + "\n"
                                + "public class LambdaThreadTest extends Activity {\n"
                                + "    @WorkerThread\n"
                                + "    static void doSomething() {}\n"
                                + "\n"
                                + "    static void doInBackground(Runnable r) {}\n"
                                + "\n"
                                + "    @Override protected void onCreate(Bundle savedInstanceState) {\n"
                                + "        super.onCreate(savedInstanceState);\n"
                                + "        doInBackground(new Runnable() {\n"
                                + "            @Override public void run() {\n"
                                + "                doSomething();\n"
                                + "            }\n"
                                + "        });\n"
                                + "        doInBackground(() -> doSomething());\n"
                                + "        doInBackground(LambdaThreadTest::doSomething);\n"
                                + "    }\n"
                                + "}\n"
                        ),
                        mSupportClasspath,
                        mSupportJar
                ));
    }

    public void testRestrictedInheritedAnnotation() {
        // Regression test for http://b.android.com/230387
        // Ensure that when we perform the @RestrictTo check, we don't incorrectly
        // inherit annotations from the base classes of AppCompatActivity and treat
        // those as @RestrictTo on the whole AppCompatActivity class itself.
        lint().files(
                /*
                Compiled version of these two classes:
                    package test.pkg;
                    import android.support.annotation.RestrictTo;
                    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                    public class RestrictedParent {
                    }
                and
                    package test.pkg;
                    public class Parent extends RestrictedParent {
                        public void myMethod() {
                        }
                    }
                 */
                base64gzip("libs/exploded-aar/my.group.id/mylib/25.0.0-SNAPSHOT/jars/classes.jar", ""
                        + "H4sIAAAAAAAAAAvwZmYRYeDg4GB4VzvRkwEJcDKwMPi6hjjqevq56f87xcDA"
                        + "zBDgzc4BkmKCKgnAqVkEiOGafR39PN1cg0P0fN0++5457eOtq3eR11tX69yZ"
                        + "85uDDK4YP3hapOflq+Ppe7F0FQtnxAvJI9JSUi/Flj5boia2XCujYuk0C1HV"
                        + "tGei2iKvRV8+zf5U9LGIEeyWNZtvhngBbfJCcYspmlvkgbgktbhEvyA7XT8I"
                        + "yCjKTC5JTQlILErNK9FLzkksLp4aGOvN5Chi+/j6tMxZqal2rK7xV+y+RLio"
                        + "iRyatGmWgO2RHdY3blgp7978b/28JrlfjH9XvMh66Cxwg6fY/tze73Mknz3+"
                        + "/Fb2gOaqSJXAbRvyEpsVi/WmmojznPzbrOe8al3twYCCJULbP25QP8T3nrVl"
                        + "iszbjwtOO1uerD8wpXKSoPNVQyWjby925u8WablkfCj/Y4BG8bEJua8tvhzZ"
                        + "OsdnSr35HJ4fM4RbpbWV2xctPGY0ySUu2Es6b0mYyobnBU/bo36VifS7WZmY"
                        + "zZ+aPknWN+mlIX9S4kKnxNuXlSedMZ0ilGj7IFCl43WF3bq5L00Mn809NjW6"
                        + "+L18/p1nsdrtIpd4ptrLnwmYs+cE345Xt8/ec6g4dkjs8EX7EMmy56+OmQl9"
                        + "mT75aMblsyfSNDYvt5xgV8NavVCBsTsnjSttg4PZ97sNrikn1TeavD2l6L/P"
                        + "Y2uqVSu7QWPomoUuGdMmKJltLIr8yQSKpPpfEa8iGBkYfJjwRZIociQhR01q"
                        + "n7//IQeBo/cv1AesjsiX2cmp9u1B4OOjLcGmbpzfl949oFRytszwY3Kl0cMD"
                        + "7B+cJZetzex5l3hvj/nn0+euf8/jf8BVyMGuzviL0Y/zX6/WlL2qFs8XSx7c"
                        + "e3mnypfg0BPtb9P0zoacuT5nzlIr4dczDVZ9sl+YPX2VypGVU5f6xsWLnVxs"
                        + "sGnD9ZZ3z/7G3Vp6jvPh5nuzfPxCWmVMpadrf1RT2vHhx2Z7k8QLav53JKZG"
                        + "zjQ35rn48PPq64yhNuHzYw95rbn3Q/hLYD/zujpZqxdFvbNYvwhs+qSpWxNY"
                        + "/Yd9b7zC1oSQfFl5cErewhTw/BEwCIIYQYHEyCTCgJqvYDkOlClRAUoWRdeK"
                        + "nEFEULTZ4sigyCaA4gg59uRRTDhJOFuhG4bsS1EUw/KYcER/gDcrG0gBCxDy"
                        + "ArVNZgbxABAMMsu2BAAA"),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "public class Cls extends Parent {\n"
                        + "    @Override\n"
                        + "    public void myMethod() {\n"
                        + "        super.myMethod();\n"
                        + "    }\n"
                        + "}\n"),
                gradle(""
                        + "apply plugin: 'com.android.application'\n"
                        + "\n"
                        + "dependencies {\n"
                        + "    compile 'my.group.id:mylib:25.0.0-SNAPSHOT'\n"
                        + "}"),
                classpath(SUPPORT_JAR_PATH,
                        "libs/exploded-aar/my.group.id/mylib/25.0.0-SNAPSHOT/jars/classes.jar"),
                mSupportJar)
                .run()
                .expectClean();
    }

    public void testSizeAnnotations() throws Exception {
        lint().files(
                java(""
                        + "package pkg.my.myapplication;\n"
                        + "\n"
                        + "import android.support.annotation.NonNull;\n"
                        + "import android.support.annotation.Size;\n"
                        + "\n"
                        + "public class SizeTest2 {\n"
                        + "    @Size(3)\n"
                        + "    public float[] toLinear(float r, float g, float b) {\n"
                        + "        return toLinear(new float[] { r, g, b });\n"
                        + "    }\n"
                        + "\n"
                        + "    @NonNull\n"
                        + "    public float[] toLinear(@NonNull @Size(min = 3) float[] v) {\n"
                        + "        return v;\n"
                        + "    }\n"
                        + "}\n"),
                mSupportClasspath,
                mSupportJar)
                .run()
                .expectClean();
    }

    public void testWrongConstant() throws Exception {
        // Regression test for scenario found to be inconsistent between PSI and UAST
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.support.annotation.NonNull;\n"
                        + "\n"
                        + "public class ViewableDayInterval {\n"
                        + "    @CalendarDay\n"
                        + "    private int mDayCreatedFor;\n"
                        + "\n"
                        + "    public ViewableDayInterval(long startOffset, long duration, @NonNull @CalendarDay int... startDays) {\n"
                        + "        this(startDays[0], startOffset, duration, startDays);\n"
                        + "    }\n"
                        + "\n"
                        + "    public ViewableDayInterval(long start, @NonNull @WeekDay int... weekdays) {\n"
                        + "        this(weekdays[0], start, start, weekdays);\n"
                        + "    }\n"
                        + "\n"
                        + "    public ViewableDayInterval(long start, @NonNull @WeekDay int weekday) {\n"
                        + "        this(weekday, start, start, weekday);\n"
                        + "    }\n"
                        + "\n"
                        + "    public ViewableDayInterval(@CalendarDay int dayCreatedFor, long startOffset, long duration, @NonNull @CalendarDay int... startDays) {\n"
                        + "        mDayCreatedFor = dayCreatedFor;\n"
                        + "    }\n"
                        + "}"),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.support.annotation.IntDef;\n"
                        + "\n"
                        + "import java.lang.annotation.Retention;\n"
                        + "import java.lang.annotation.RetentionPolicy;\n"
                        + "import java.util.Calendar;\n"
                        + "\n"
                        + "@Retention(RetentionPolicy.SOURCE)\n"
                        + "@IntDef({Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,\n"
                        + "        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY})\n"
                        + "public @interface CalendarDay {\n"
                        + "}"),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.support.annotation.IntDef;\n"
                        + "\n"
                        + "import java.lang.annotation.Retention;\n"
                        + "import java.lang.annotation.RetentionPolicy;\n"
                        + "import java.util.Calendar;\n"
                        + "\n"
                        + "@Retention(RetentionPolicy.SOURCE)\n"
                        + "@IntDef({Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,\n"
                        + "        Calendar.THURSDAY, Calendar.FRIDAY})\n"
                        + "public @interface WeekDay {\n"
                        + "}"),
                mSupportClasspath,
                mSupportJar)
                .run()
                .expectClean();
    }

    public void testPrivateVisibilityWithDefaultConstructor() {
        // Regression test for https://code.google.com/p/android/issues/detail?id=235661
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.support.annotation.VisibleForTesting;\n"
                        + "\n"
                        + "public class LintBugExample {\n"
                        + "    public static Object demonstrateBug() {\n"
                        + "        return new InnerClass();\n"
                        + "    }\n"
                        + "\n"
                        + "    @VisibleForTesting\n"
                        + "    static class InnerClass {\n"
                        + "    }\n"
                        + "}"),
                mSupportClasspath,
                mSupportJar)
                .run()
                .expectClean();
    }

    public void testIndirectTypedef() {
        // Regression test for b/36384014
        lint().files(
                java("package test.pkg;\n"
                        + "\n"
                        + "import android.support.annotation.IntDef;\n"
                        + "\n"
                        + "public class Lifecycle {\n"
                        + "    public static final int ON_CREATE = 1;\n"
                        + "    public static final int ON_START = 2;\n"
                        + "    public static final int ON_RESUME = 3;\n"
                        + "    public static final int ON_PAUSE = 4;\n"
                        + "    public static final int ON_STOP = 5;\n"
                        + "    public static final int ON_DESTROY = 6;\n"
                        + "    public static final int ANY = 7;\n"
                        + "\n"
                        + "    @IntDef(value = {ON_CREATE, ON_START, ON_RESUME, ON_PAUSE, ON_STOP, ON_DESTROY, ANY},\n"
                        + "            flag = true)\n"
                        + "    public @interface Event {\n"
                        + "    }\n"
                        + "}"),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import java.lang.annotation.ElementType;\n"
                        + "import java.lang.annotation.Retention;\n"
                        + "import java.lang.annotation.RetentionPolicy;\n"
                        + "import java.lang.annotation.Target;\n"
                        + "\n"
                        + "@Retention(RetentionPolicy.RUNTIME)\n"
                        + "@Target(ElementType.METHOD)\n"
                        + "public @interface OnLifecycleEvent {\n"
                        + "    @Lifecycle.Event\n"
                        + "    int value();\n"
                        + "}\n"),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "public interface Usage {\n"
                        + "    @OnLifecycleEvent(4494823) // this value is not valid\n"
                        + "    void addLocationListener();\n"
                        + "}\n"),
                mSupportClasspath,
                mSupportJar)
                .run()
                .expect(""
                        + "src/test/pkg/Usage.java:4: Error: Must be one or more of: Lifecycle.ON_CREATE, Lifecycle.ON_START, Lifecycle.ON_RESUME, Lifecycle.ON_PAUSE, Lifecycle.ON_STOP, Lifecycle.ON_DESTROY, Lifecycle.ANY [WrongConstant]\n"
                        + "    @OnLifecycleEvent(4494823) // this value is not valid\n"
                        + "                      ~~~~~~~\n"
                        + "1 errors, 0 warnings\n");
    }

    public void testCalendar() {
        // Regression test for
        // https://code.google.com/p/android/issues/detail?id=251256 and
        // http://youtrack.jetbrains.com/issue/IDEA-144891
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import java.util.Calendar;\n"
                        + "\n"
                        + "public class CalendarTest {\n"
                        + "    public void test() {\n"
                        + "        Calendar now = Calendar.getInstance();\n"
                        + "        now.get(Calendar.DAY_OF_MONTH);\n"
                        + "        now.get(Calendar.HOUR_OF_DAY);\n"
                        + "        now.get(Calendar.MINUTE);\n"
                        + "        if (now.get(Calendar.MONTH) == Calendar.JANUARY) {\n"
                        + "        }\n"
                        + "        now.set(Calendar.HOUR_OF_DAY, 50);\n"
                        + "        now.set(2017, 3, 29);\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expectClean();
    }

    public void testThreadsInLambdas() {
        // Regression test for b/38069472
        //noinspection all // Sample code
        lint().files(
                manifest().minSdk(1),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.support.annotation.MainThread;\n"
                        + "import android.support.annotation.WorkerThread;\n"
                        + "\n"
                        + "import java.util.concurrent.Executor;\n"
                        + "\n"
                        + "public abstract class ApiCallInLambda<T> {\n"
                        + "    Executor networkExecutor;\n"
                        + "    @MainThread\n"
                        + "    private void fetchFromNetwork(T data) {\n"
                        + "        networkExecutor.execute(() -> {\n"
                        + "            Call<T> call = createCall();\n"
                        + "        });\n"
                        + "    }\n"
                        + "\n"
                        + "    @WorkerThread\n"
                        + "    protected abstract Call<T> createCall();\n"
                        + "\n"
                        + "    private static class Call<T> {\n"
                        + "    }\n"
                        + "}\n"),
                mSupportClasspath,
                mSupportJar)
                .checkMessage(this::checkReportedError)
                .run()
                .expectClean();
    }

    public void testExtensionMethods() {
        if (skipKotlinTests()) {
            return;
        }

        // Regression test for https://issuetracker.google.com/65602862
        lint().files(
                kotlin("" +
                        "package test.pkg\n" +
                        "\n" +
                        "import android.app.Activity\n" +
                        "import android.content.Context\n" +
                        "import android.content.res.Resources\n" +
                        "import android.support.annotation.AttrRes\n" +
                        "import android.support.annotation.ColorInt\n" +
                        "\n" +
                        "class TestActivity: Activity() {\n" +
                        "\n" +
                        "    @ColorInt\n" +
                        "    fun Context.getColor(@AttrRes attrId: Int, @ColorInt defaultColor: Int) = theme.getColor(attrId, defaultColor)\n" +
                        "\n" +
                        "    @ColorInt\n" +
                        "    fun Resources.Theme.getColor(@AttrRes attrId: Int, @ColorInt defaultColor: Int): Int {\n" +
                        "        return 0;\n" +
                        "    }\n" +
                        "}"),
                mSupportClasspath,
                mSupportJar)
                .checkMessage(this::checkReportedError)
                .run()
                .expectClean();
    }

    public void testRangeInKotlin() {
        if (skipKotlinTests()) {
            return;
        }

        // Regression test for https://issuetracker.google.com/66892728
        lint().files(
                kotlin("" +
                        "package test.pkg\n" +
                        "\n" +
                        "import android.support.annotation.FloatRange\n" +
                        "import android.util.Log\n" +
                        "\n" +
                        "fun foo(@FloatRange(from = 0.0, to = 25.0) radius: Float) {\n" +
                        "    bar(radius)\n" +
                        "}\n" +
                        "\n" +
                        "fun bar(@FloatRange(from = 0.0, to = 25.0) radius: Float) {\n" +
                        "    Log.d(\"AppLog\", \"Radius:\" + radius)\n" +
                        "}"),
                mSupportClasspath,
                mSupportJar)
                .run()
                .expectClean();
    }

    public static final String SUPPORT_JAR_PATH = "libs/support-annotations.jar";
    private TestFile mSupportJar = base64gzip(SUPPORT_JAR_PATH,
            SUPPORT_ANNOTATIONS_JAR_BASE64_GZIP);
    private TestFile mSupportClasspath = classpath(SUPPORT_JAR_PATH);
}
