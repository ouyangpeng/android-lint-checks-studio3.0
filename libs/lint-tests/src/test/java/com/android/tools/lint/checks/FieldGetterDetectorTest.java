/*
 * Copyright (C) 2011 The Android Open Source Project
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
public class FieldGetterDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new FieldGetterDetector();
    }

    public void test() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/bytecode/GetterTest.java:47: Warning: Calling getter method getFoo1() on self is slower than field access (mFoo1) [FieldGetter]\n"
                + "  getFoo1();\n"
                + "  ~~~~~~~\n"
                + "src/test/bytecode/GetterTest.java:48: Warning: Calling getter method getFoo2() on self is slower than field access (mFoo2) [FieldGetter]\n"
                + "  getFoo2();\n"
                + "  ~~~~~~~\n"
                + "src/test/bytecode/GetterTest.java:52: Warning: Calling getter method isBar1() on self is slower than field access (mBar1) [FieldGetter]\n"
                + "  isBar1();\n"
                + "  ~~~~~~\n"
                + "src/test/bytecode/GetterTest.java:54: Warning: Calling getter method getFoo1() on self is slower than field access (mFoo1) [FieldGetter]\n"
                + "  this.getFoo1();\n"
                + "       ~~~~~~~\n"
                + "src/test/bytecode/GetterTest.java:55: Warning: Calling getter method getFoo2() on self is slower than field access (mFoo2) [FieldGetter]\n"
                + "  this.getFoo2();\n"
                + "       ~~~~~~~\n"
                + "0 errors, 5 warnings\n",

            lintProject(
                classpath(),
                manifest().minSdk(1),
                mGetterTest,
                mGetterTest2
                ));
    }

    public void testPostFroyo() throws Exception {
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintProject(
                classpath(),
                manifest().minSdk(10),
                mGetterTest,
                mGetterTest2
                ));
    }

    public void testLibraries() throws Exception {
        // This tests the infrastructure: it makes sure that we *don't* run this
        // check in jars that are on the jar library dependency path (testJar() checks
        // that it *does* work for local jar classes)
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintProject(
                classpath(),
                manifest().minSdk(1),
                mGetterTest,
                mGetterTest3
                ));
    }

    public void testJar() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/bytecode/GetterTest.java:47: Warning: Calling getter method getFoo1() on self is slower than field access (mFoo1) [FieldGetter]\n"
                + "  getFoo1();\n"
                + "  ~~~~~~~\n"
                + "src/test/bytecode/GetterTest.java:48: Warning: Calling getter method getFoo2() on self is slower than field access (mFoo2) [FieldGetter]\n"
                + "  getFoo2();\n"
                + "  ~~~~~~~\n"
                + "src/test/bytecode/GetterTest.java:52: Warning: Calling getter method isBar1() on self is slower than field access (mBar1) [FieldGetter]\n"
                + "  isBar1();\n"
                + "  ~~~~~~\n"
                + "src/test/bytecode/GetterTest.java:54: Warning: Calling getter method getFoo1() on self is slower than field access (mFoo1) [FieldGetter]\n"
                + "  this.getFoo1();\n"
                + "       ~~~~~~~\n"
                + "src/test/bytecode/GetterTest.java:55: Warning: Calling getter method getFoo2() on self is slower than field access (mFoo2) [FieldGetter]\n"
                + "  this.getFoo2();\n"
                + "       ~~~~~~~\n"
                + "0 errors, 5 warnings\n",

            lintProject(
                mClasspath_jar,
                manifest().minSdk(1),
                mGetterTest,
                mGetterTest4
                ));
    }

    public void testTruncatedData() throws Exception {
        assertEquals(
                "No warnings.",

                lintProject(
                    mClasspath_jar,
                    mGetterTest5
                    ));
    }

    public void testCornerCases() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/TestFieldGetter.java:21: Warning: Calling getter method getPath() on self is slower than field access (path) [FieldGetter]\n"
                + "        getPath(); // Should be flagged\n"
                + "        ~~~~~~~\n"
                + "0 errors, 1 warnings\n",

            lintProject(
                mClasspath_jar,
                manifest().minSdk(1),
                java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import java.io.File;\n"
                            + "import java.util.List;\n"
                            + "\n"
                            + "import android.content.Context;\n"
                            + "\n"
                            + "public class TestFieldGetter {\n"
                            + "    private int path;\n"
                            + "    private int foo;\n"
                            + "\n"
                            + "    public int getPath() {\n"
                            + "        return path;\n"
                            + "    }\n"
                            + "\n"
                            + "    public int getFoo() {\n"
                            + "        return foo;\n"
                            + "    }\n"
                            + "\n"
                            + "    public void test(TestFieldGetter other) {\n"
                            + "        getPath(); // Should be flagged\n"
                            + "        other.getPath(); // Ignore\n"
                            + "        File file = new File(\"/dummy\");\n"
                            + "        file.getPath(); // Ignore\n"
                            + "    }\n"
                            + "\n"
                            + "    public static void test2(TestFieldGetter other) {\n"
                            + "        other.getPath(); // Ignore\n"
                            + "    }\n"
                            + "\n"
                            + "    public class Inner extends TestFieldGetter {\n"
                            + "        public void test() {\n"
                            + "            getFoo(); // Ignore\n"
                            + "        }\n"
                            + "    }\n"
                            + "}\n"),
                base64gzip("bin/classes/test/pkg/TestFieldGetter.class", ""
                            + "H4sIAAAAAAAAAHVS227TQBA908R2YtxbSG8hKS0UcNJSo4o3EC+VgiKZi9Qq"
                            + "FY9Osk1cHLuyN0j8Ey9IqJVA4gP4KMTsNiJVmsjyzOzszJlzdvfP35+/ARzh"
                            + "hYUFwqYUmfQuP/e9Uw6aoYh6b4WUIrWQJ6xcBF8CLwrivvehcyG6kpC/DOSA"
                            + "QC1C7jxJCObrMA7lG1669TbvHyc9YSMHx0EBRcKyH8bi/WjYEelp0IkEoeQn"
                            + "3SBqB2mo1uNkXg7CjFDx5xF6RbD6Qn7U43lWqwhC2YEBk0nwTjNJVGrDgaVS"
                            + "eQVEqLnzEettmxseOFhFyUKN4Gi9YeI1w0gU8JCBvd5oOPxqo4pdJegRYc31"
                            + "J8dyItMw7mukKvYU0hNm5dbvlhCMRA5EyszOQ6V4yb89Te0rokcE+yQZpV3R"
                            + "1FXlKdKHqomZtuJYpMdRkGUis3BI2J4nc0+XMrz22GXJOX4BC/zx2enI0t7g"
                            + "Hb4ytjavPPakso1r3PuuyxbZmjpZwBJb56YAy1hhT+oY7zT/wP1vU82LM5vX"
                            + "ZjavTzevzmzexNa4ecDilLxK4wqVs31lfqH6qbR9jZ13B1d4fDbRss4I4MEG"
                            + "RzY2UNY4W3rCzg3KeIKKnnLE98E8nsFlhCLq/6e+HFM29dTJBJs9+GUYqN3i"
                            + "bWosVdPQdh8HupbwnH+W/w/CJRCdpQMAAA==")
                ));
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mClasspath_jar = source(".classpath", ""
            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<classpath>\n"
            + "\t<classpathentry kind=\"src\" path=\"src\"/>\n"
            + "\t<classpathentry kind=\"src\" path=\"gen\"/>\n"
            + "\t<classpathentry kind=\"con\" path=\"com.android.ide.eclipse.adt.ANDROID_FRAMEWORK\"/>\n"
            + "\t<classpathentry kind=\"con\" path=\"com.android.ide.eclipse.adt.LIBRARIES\"/>\n"
            + "\t<classpathentry kind=\"output\" path=\"bin/classes.jar\"/>\n"
            + "</classpath>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mGetterTest = java(""
            + "package test.bytecode;\n"
            + "\n"
            + "public class GetterTest {\n"
            + "\tprivate int mFoo1;\n"
            + "\tprivate String mFoo2;\n"
            + "\tprivate int mBar1;\n"
            + "\tprivate static int sFoo4;\n"
            + "\n"
            + "\tpublic int getFoo1() {\n"
            + "\t\treturn mFoo1;\n"
            + "\t}\n"
            + "\n"
            + "\tpublic String getFoo2() {\n"
            + "\t\treturn mFoo2;\n"
            + "\t}\n"
            + "\n"
            + "\tpublic int isBar1() {\n"
            + "\t\treturn mBar1;\n"
            + "\t}\n"
            + "\n"
            + "\t// Not \"plain\" getters:\n"
            + "\n"
            + "\tpublic String getFoo3() {\n"
            + "\t\t// NOT a plain getter\n"
            + "\t\tif (mFoo2 == null) {\n"
            + "\t\t\tmFoo2 = \"\";\n"
            + "\t\t}\n"
            + "\t\treturn mFoo2;\n"
            + "\t}\n"
            + "\n"
            + "\tpublic int getFoo4() {\n"
            + "\t\t// NOT a plain getter (using static)\n"
            + "\t\treturn sFoo4;\n"
            + "\t}\n"
            + "\n"
            + "\tpublic int getFoo5(int x) {\n"
            + "\t\t// NOT a plain getter (has extra argument)\n"
            + "\t\treturn sFoo4;\n"
            + "\t}\n"
            + "\n"
            + "\tpublic int isBar2(String s) {\n"
            + "\t\t// NOT a plain getter (has extra argument)\n"
            + "\t\treturn mFoo1;\n"
            + "\t}\n"
            + "\n"
            + "\tpublic void test() {\n"
            + "\t\tgetFoo1();\n"
            + "\t\tgetFoo2();\n"
            + "\t\tgetFoo3();\n"
            + "\t\tgetFoo4();\n"
            + "\t\tgetFoo5(42);\n"
            + "\t\tisBar1();\n"
            + "\t\tisBar2(\"foo\");\n"
            + "\t\tthis.getFoo1();\n"
            + "\t\tthis.getFoo2();\n"
            + "\t\tthis.getFoo3();\n"
            + "\t\tthis.getFoo4();\n"
            + "\t}\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mGetterTest2 = base64gzip("bin/classes/test/bytecode/GetterTest.class", ""
            + "H4sIAAAAAAAAAHVSXW8SQRQ9A2z5cAqItLa02mK1wqqlpdBCi22iSQ0J1QdM"
            + "eV5wxa0ta2Br9G/4O3xQozXxwVcTf5Tx3mG7JbB9mblz7z3nnjkzf//9+g2g"
            + "iP0wAgJzjjlwCu2PjtmxX5mFZ6bjmP2XlAsjJJA8Nt4bhROj1y28aB+bHUdA"
            + "Oz2w7Q0BUXfjokCqcdnXdPpWr7vLxSdGnxq1ATWVBKZqVs9y9gSCufyRQOgp"
            + "zYshiITENUiBRMPqmc/PTts032ifmExrd4yTI6Nv8dlNhpw31kAg07hKOI0O"
            + "d01nqJJm1aMQuCmhYcqrkOZ0Lj+pmlsXJMKIkF5rwBfg1G2J6Ah6M4KsAOWn"
            + "m47ReXtovHO1ufUSY+5JxEYwZVKeq+fJNPHhgptUzOQmRaimAd+UbhMjqjWJ"
            + "NGY4WpeYwzxHRYmlYVSSWBlWtyRWcZ+jisQiZiLYIQde2zanahJ56AKxpn3W"
            + "75gHFgtOXNq2xjqQpc4gfY8QAmwYRQF2Q+1RdY7xtWjnO9K7sQt0KtBOlkDT"
            + "fyL+VbUnaZ1SySCu0yqHDUjhBtg8utIE+DtmP4+BY75gcsEHnPkyBo77ghd9"
            + "J98an5zyBS95kw+pW/Xx5D+I6qnlH8hcyAgqprhyc548zBBmcYQx5TLeUb1i"
            + "mpIrnq41V1foG+6Oy8qOkIQ8WfTwLrZCe+AK7KrCzg7rLpajnKoL/iEuS9Vl"
            + "8X2TByM0mkejUT6iaB56/+KT61FNP8ejFi0FXjZ42WzpSdrKHG+39FT1HLst"
            + "v7bL71QkXn40Dev0LzaQoEwam1hACcsokwVbNHubKhWKqtjFzohZNc+sx4px"
            + "7z/CMfNvCwUAAA==");

    @SuppressWarnings("all") // Sample code
    private TestFile mGetterTest3 = base64gzip("libs/library.jar", ""
            + "H4sIAAAAAAAAAAvwZmYRYeAAQoFoOwcGJMDJwMLg6xriqOvp56b/7xQDAzND"
            + "gDc7B0iKCaokAKdmESCGa/Z19PN0cw0O0fN1++x75rSPt67eRV5vXa1zZ85v"
            + "DjK4YvzgaZGel6+Op+/F0lUsnDNeSh6RjtTKsBATebJEq+KZ6uvMT0UfixjB"
            + "tk83XmlkAzTbBmo7F8Q6NNtZgbgktbhEH7cSPpiSpMqS1OT8lFR9hGfQ1cph"
            + "qHVPLSlJLQoBiukl5yQWF5cGxeYLOYrYMuf8LODq2LJty62krYdWLV16wak7"
            + "d5EnL+dVdp/KuIKja3WzE7K/5P+wrglYbPrxYLhw/ZSP9xJ3q26onbmz+L3t"
            + "83mWxvX///7iXdDx14CJqbjPsoDrbX/fzY3xM1vTlz2e8Xf6FG5llQk2Zvek"
            + "W4UXX9fdkyE/W9bdwdp2w1texsDyx4scVhXevF7yK2z97tNH1d3mS21lNJ3K"
            + "siwr7HzRN5amnX8mOrzQPNut2NFyxNSj0eXwq5nnz/vdNrmfMX+GT3Z5z2Tl"
            + "xfkfb/q2zTG/5qBweYeXRS9fuW/6iklpVxcL7NBcmHhq9YRnJXr2K2dFi6sc"
            + "6pgQl31A/MGV3M4XHFXGTWsYni6f3XexsjpjT/HWnV+Fkt95HnEzSA2at/r5"
            + "SZOPD5tmh5x5oua6Yhnj/Sl5wsqrTDtN0iyips84bOPu2rk0MWRShGTYdpWw"
            + "wvmLu44opSndUGSPu222PEuo8gXTxmW1197PYBfj9ou5te2Y1YSl5xRq+wWY"
            + "ciRcGcuc3waW9n3cmvHc+tLujdwlWhf8pjlcrlf6F7pVPXNu0EmFdZe12nk9"
            + "HrLdsNl1ieWHdZp9f2PyvoSig+xzfhqx9f1uEq9Vvy81f84nVv3Kyfwro79+"
            + "fGLf8WrlU/kTMSc4tJbtKCqeZ3NGIK2wxfCp0b3AvUmzJmnPW2caHv5C+l3f"
            + "6VN9E1psIr980NvmVP2A682qQ+f4XutNWzxnFfc/RT3vq6kfayezK5vMcl8c"
            + "aLcoQ67q/6PJrwN97Y8vFtNljTOruJnz0vPWKZn87V9Cvsrs1t2/7fT7EJW4"
            + "OhPe11/0zSYs8JGaHeHAeVpjMmu0SfVsLdGuVTeOnuuIND2/5nhX4Xt7UEY4"
            + "ZPg5Pw+YD7lZQRmBkUmEATUjwrIoKBejApQ8ja4VOX+JoGizxZGjQSZwMeDO"
            + "hwiwG5ErcWvhQ9FyD0suRTgYpBc5HORQ9HIxEsq1Ad6sbBBnsjJYAFUfYQbx"
            + "AFJZ3LASBQAA");

    @SuppressWarnings("all") // Sample code
    private TestFile mGetterTest4 = base64gzip("bin/classes.jar", ""
            + "H4sIAAAAAAAAAAvwZmYRYeAAQoFoOwcGJMDJwMLg6xriqOvp56b/7xQDAzND"
            + "gDc7B0iKCaokAKdmESCGa/Z19PN0cw0O0fN1++x75rSPt67eRV5vXa1zZ85v"
            + "DjK4YvzgaZGel6+Op+/F0lUsnDNeSh6RjtTKsBATebJEq+KZ6uvMT0UfixjB"
            + "tk83XmlkAzTbBmo7F8Q6NNtZgbgktbhEH7cSPpiSpMqS1OT8lFR9hGfQ1cph"
            + "qHVPLSlJLQoBiukl5yQWF5cGxeYLOYrYMuf8LODq2LJty62krYdWLV16wak7"
            + "d5EnL+dVdp/KuIKja3WzE7K/5P+wrglYbPrxYLhw/ZSP9xJ3q26onbmz+L3t"
            + "83mWxvX///7iXdDx14CJqbjPsoDrbX/fzY3xM1vTlz2e8Xf6FG5llQk2Zvek"
            + "W4UXX9fdkyE/W9bdwdp2w1texsDyx4scVhXevF7yK2z97tNH1d3mS21lNJ3K"
            + "siwr7HzRN5amnX8mOrzQPNut2NFyxNSj0eXwq5nnz/vdNrmfMX+GT3Z5z2Tl"
            + "xfkfb/q2zTG/5qBweYeXRS9fuW/6iklpVxcL7NBcmHhq9YRnJXr2K2dFi6sc"
            + "6pgQl31A/MGV3M4XHFXGTWsYni6f3XexsjpjT/HWnV+Fkt95HnEzSA2at/r5"
            + "SZOPD5tmh5x5oua6Yhnj/Sl5wsqrTDtN0iyips84bOPu2rk0MWRShGTYdpWw"
            + "wvmLu44opSndUGSPu222PEuo8gXTxmW1197PYBfj9ou5te2Y1YSl5xRq+wWY"
            + "ciRcGcuc3waW9n3cmvHc+tLujdwlWhf8pjlcrlf6F7pVPXNu0EmFdZe12nk9"
            + "HrLdsNl1ieWHdZp9f2PyvoSig+xzfhqx9f1uEq9Vvy81f84nVv3Kyfwro79+"
            + "fGLf8WrlU/kTMSc4tJbtKCqeZ3NGIK2wxfCp0b3AvUmzJmnPW2caHv5C+l3f"
            + "6VN9E1psIr980NvmVP2A682qQ+f4XutNWzxnFfc/RT3vq6kfayezK5vMcl8c"
            + "aLcoQ67q/6PJrwN97Y8vFtNljTOruJnz0vPWKZn87V9Cvsrs1t2/7fT7EJW4"
            + "OhPe11/0zSYs8JGaHeHAeVpjMmu0SfVsLdGuVTeOnuuIND2/5nhX4Xt7UEY4"
            + "ZPg5Pw+YD7lZQRmBkUmEATUjwrIoKBejApQ8ja4VOX+JoGizxZGjQSZwMeDO"
            + "hwiwG5ErcWvhQ9FyD0suRTgYpBc5HORQ9HIxEsq1Ad6sbBBnsjJYAFUfYQbx"
            + "AFJZ3LASBQAA");

    @SuppressWarnings("all") // Sample code
    private TestFile mGetterTest5 = base64gzip("bin/test/pkg/bogus.class", ""
            + "H4sIAAAAAAAAAAvwZmYRYeAAQoFoOwcGJMDJwMLg6xriqOvp56b/7xQDAzND"
            + "gDc7B0iKCaokAKdmESCGa/Z19PN0cw0O0fN1++x75rSPt67eRV5vXa1zZ85v"
            + "DjK4YvzgaZGel6+Op+/F0lUsnDNeSh6RjtTKsBATebJEq+KZ6uvMT0UfixjB"
            + "tk83XmlkAzTbBmo7F8Q6NNtZgbgktbhEH7cSPpiSpMqS1OT8lFR9hGfQ1cph"
            + "qHVPLSlJLQoBiukl5yQWF5cGxeYLOYrYMuf8LODq2LJty62krYdWLV16wak7"
            + "d5EnL+dVdp/KuIKja3WzE7K/5P+wrglYbPrxYLhw/ZSP9xJ3q26onbmz+L3t"
            + "83mWxvX///7iXdDx14CJqbjPsoDrbX/fzY3xM1vTlz2e8Xf6FG5llQk2Zvek"
            + "W4UXX9fdkyE/W9bdwdp2w1texsDyx4scVhXevF7yK2z97tNH1d3mS21lNJ3K"
            + "siwr7HzRN5amnX8mOrzQPNut2NFyxNSj0eXwq5nnz/vdNrmfMX+GT3Z5z2Tl"
            + "xfkfb/q2zTG/5qBweYeXRS9fuW/6iklpVxcL7NBcmHhq9YRnJXr2K2dFi6sc"
            + "6pgQl31A/MGV3M4XHFXGTWsYni6f3XexsjpjT/HWnV+Fkt95HnEzSA2at/r5"
            + "SZOPD5tmh5x5oua6Yhnj/Sl5wsqrTDtN0iyips84bOPu2rk0MWRShGTYdpWw"
            + "wvmLu44opSndUGSPu222PEuo8gXTxmW1197PYBfj9ou5te2Y1YSl5xRq+wWY"
            + "ciRcGcuc3waW9n3cmvHc+tLujdwlWhf8pjlcrlf6F7pVPXNu0EmFdZe12nk9"
            + "HrLdsNl1ieWHdZp9f2PyvoSig+xzfhqx9f1uEq9Vvy81f84nVv3Kyfwro79+"
            + "fGLf8WrlU/kTMSc4tJbtKCqeZ3NGIK2wxfCp0b3AvUmzJmnPW2caHv5C+l3f"
            + "6VN9E1psIr980NvmVP2A682qQ+f4XutNWzxnFfc/RT3vq6kfayezK5vMcl8c"
            + "aLcoQ67q/6PJrwN97Y8vFtNljTOruJnz0vPWKZn87V9Cvsrs1t2/7fT7EJW4"
            + "OhPe11/0zSYs8JGaHeHAeVpjMmu0SfVsLdGuVTeOnuuIND2/5nhX4Xt7UEY4"
            + "ZPg5Pw+YD7lZQRmBkUmEATUjwrIoKBejApQ8ja4VOX+JoGizxZGjQSZwMeDO"
            + "hwiwG5ErcWvhQ9FyD0suRTgYpBc5HORQ9HIxEsq1Ad6sbBBnsjJYAFUfYQbx"
            + "AFJZ3LASBQAA");
}
