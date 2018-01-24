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
public class OverrideDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new OverrideDetector();
    }

    public void test() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "src/pkg2/Class2.java:7: Error: This package private method may be unintentionally overriding method in pkg1.Class1 [DalvikOverride]\n"
                + "    void method() { // Flag this as an accidental override\n"
                + "         ~~~~~~\n"
                + "    src/pkg1/Class1.java:4: This method is treated as overridden\n"
                + "1 errors, 0 warnings\n",

            lintProject(
                classpath(),
                manifest().minSdk(10),
                java(""
                            + "package pkg1;\n"
                            + "\n"
                            + "public class Class1 {\n"
                            + "    void method() {\n"
                            + "    }\n"
                            + "\n"
                            + "    void method2(int foo) {\n"
                            + "    }\n"
                            + "\n"
                            + "    void method3() {\n"
                            + "    }\n"
                            + "\n"
                            + "    void method4() {\n"
                            + "    }\n"
                            + "\n"
                            + "    void method5() {\n"
                            + "    }\n"
                            + "\n"
                            + "    void method6() {\n"
                            + "    }\n"
                            + "\n"
                            + "    void method7() {\n"
                            + "    }\n"
                            + "\n"
                            + "    public static class Class4 extends Class1 {\n"
                            + "        void method() { // Not an error: same package\n"
                            + "        }\n"
                            + "    }\n"
                            + "}\n"),
                java(""
                            + "package pkg2;\n"
                            + "\n"
                            + "import android.annotation.SuppressLint;\n"
                            + "import pkg1.Class1;\n"
                            + "\n"
                            + "public class Class2 extends Class1 {\n"
                            + "    void method() { // Flag this as an accidental override\n"
                            + "    }\n"
                            + "\n"
                            + "    void method2(String foo) { // not an override: different signature\n"
                            + "    }\n"
                            + "\n"
                            + "    static void method4() { // not an override: static\n"
                            + "    }\n"
                            + "\n"
                            + "    private void method3() { // not an override: private\n"
                            + "    }\n"
                            + "\n"
                            + "    protected void method5() { // not an override: protected\n"
                            + "    }\n"
                            + "\n"
                            + "    public void method6() { // not an override: public\n"
                            + "    }\n"
                            + "\n"
                            + "    @SuppressLint(\"DalvikOverride\")\n"
                            + "    public void method7() { // suppressed: no warning\n"
                            + "    }\n"
                            + "\n"
                            + "    public class Class3 extends Object {\n"
                            + "        void method() { // Not an override: not a subclass\n"
                            + "        }\n"
                            + "    }\n"
                            + "}\n"),
                base64gzip("bin/classes/pkg1/Class1.class", ""
                            + "H4sIAAAAAAAAAIWPy07CUBCG/6HQQgtyEcF7YuICNbEBRRcaNyQmJEQXGvYF"
                            + "jlAsrWmL7+XKxIUP4EMZpy1piGnSk5yZ/8zMNzPn5/frG0AHhwoyBO3tddrW"
                            + "e5bheW0FWUJlbrwbumXYU/1xNBdjnyDfmrbp3xGk1smQkO05E6FCQqGIHGRC"
                            + "eWDa4mG5GAn32RhZglAbOGPDGhquGbxXwaw/Mz1CabA28oa7L4Q/cyYEJRId"
                            + "rmz1g0HSi+MQqB+nLmJ1GaturK5idU1Qn5ylOxb3ZjBZi4adB18jFPu2Ldww"
                            + "JDwF+7zu2kbHoeP+ciRwBN4Ewcmz4g+zVfilsyf2udNP5D9YZKCylcOgBI1t"
                            + "MSpgXwobbMTwGd8gR//B3BpIMVhGZQV2w/oEMB+CjSgZg4QqamF+M322lji7"
                            + "ng5uJIJb6WA1EWykg/VEsJkONhPADLZDu4Nd9irH9/geoPAHTomv/SwDAAA="),
                base64gzip("bin/classes/pkg1/Class1$Class4.class", ""
                            + "H4sIAAAAAAAAAGVOy2rCUBA9k8REk/io7aZ0VXDRB1Qs7izdCIIQumlxf9WL"
                            + "3jYmkof/1VXBhR/Qj5JObkSkXrjnzJyZMzO/++0OwDMuHRiE9vpr0esOQ5Gm"
                            + "vY6mvgOL4J3oBPtFRSp7JZh39xOCNYzn0oWJmo8KbEIzUJF8y1dTmXyIaSh5"
                            + "bhDPRDgRiSryg2hlS5USroLzpQNespLZMp4T3Pc4T2ZypAqPV7Y9fYqNIPjj"
                            + "KJKJliRPskszbsGnoXgGR3wSo8NZl5mYKw8/qH7rsstoa/EaHqNfNjDX9YDG"
                            + "0fzIv6jRf+PNiZEORgNNjS1cMLt6r4k2an8qMhFhcAEAAA=="),
                base64gzip("bin/classes/pkg2/Class2.class", ""
                            + "H4sIAAAAAAAAAIVSXWsTQRQ9d5PsxiRNbW2tGr8iPlQFl0arDxVBKkJhUTDS"
                            + "lz5Nu2M6djMT9ut/+ST44A/wR4l3Z8sQ7GIG7r1nzp1zP5b9/efnLwATPArg"
                            + "EfqLi9kkPExElk0CtGtiryb2CP4bpVX+ltDafXJMaB+aWPbQwrUBOvAJ65HS"
                            + "8mMxP5XpF3GaSMJmZM5EcixSVd0vyXZ+rjLCWrTU7YCrz2V+bmJCUIMJYXs3"
                            + "+iZKESZCz8Jpnio9O6g6t74aUxW/knTilw69cGjfoVcOvSaMPhc6V3N5pEuV"
                            + "KZ7wndYmF7kymqccR0LHqVFxKBwdTovFIpVZxvvm3LNTiqTgxYbvRVKqi0+l"
                            + "TFMVM9GbmiI9kx9UtXa/3vR5NTRhcKS1TC0lswAPeZ2lz/HYBh7drwHG4K1R"
                            + "nS4j/trsA76FHIlj5+kPdL8z8NBj71vSR5/9oH7Acc0WGDrxM7YqR/8Ku0tC"
                            + "csJ1XL8U7tv3DcK+Fd6sk05I2MAm+y5uuN5j1OdKiaEt4VnbWj3qRsOobWyv"
                            + "Fm41CIlHXyncaRTucNbDLbsA4TbbnRNQhtH/i40ainm4a/093OfYY/4BG/8B"
                            + "fwGE8OKVrgMAAA=="),
                base64gzip("bin/classes/pkg2/Class2$Class3.class", ""
                            + "H4sIAAAAAAAAAGVQy0rDQBQ9N42NTdM21voEEaGLasFIdae4KQhC0IXS/aQd"
                            + "2qlpIknqf7kQwYUf4EeJd9Isos4wZ+Y+zpkz8/X98QlggH0LBqH9/DQdeMNQ"
                            + "pOmgm2/nFkyCOxcvwgtFNPXug7kcZ4RqNlNp94zQ8EukSy5cqUhl10zq/aoc"
                            + "jwjmMJ7IGgh1B2uo2qig4cBCk1Dp6YaWryJ5t1wEMnkUQSjZkh+PRTgSidJx"
                            + "kTT15YSO/9+vdrCQ2SyeEOyHeJmM5Y3SnPqq7VQ/heDcRpFM8pRMLexyvaTF"
                            + "Gis1HLHZCv8Qua52zCeDMxbWGWscXXBs8G6f9N8Y3uG85j0txir3AAdwGZ38"
                            + "bGMDbeixiWah0Nfqev0lHpaIVBANdHLcwnYuRzx3sAf6Aftm6nvJAQAA")
                ));
    }
}
