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

public class CipherGetInstanceDetectorTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new CipherGetInstanceDetector();
    }

    public void testCipherGetInstanceAES() throws Exception {
        String expected = ""
                + "src/test/pkg/CipherGetInstanceAES.java:7: Warning: Cipher.getInstance should not be called without setting the encryption mode and padding [GetInstance]\n"
                + "    Cipher.getInstance(\"AES\");\n"
                + "                       ~~~~~\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import javax.crypto.Cipher;\n"
                        + "\n"
                        + "public class CipherGetInstanceAES {\n"
                        + "  private void foo() throws Exception {\n"
                        + "    Cipher.getInstance(\"AES\");\n"
                        + "  }\n"
                        + "}\n"))
                .run()
                .expect(expected);
    }

    public void testCipherGetInstanceDES() throws Exception {
        String expected = ""
                + "src/test/pkg/CipherGetInstanceDES.java:7: Warning: Cipher.getInstance should not be called without setting the encryption mode and padding [GetInstance]\n"
                + "    Cipher.getInstance(\"DES\");\n"
                + "                       ~~~~~\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import javax.crypto.Cipher;\n"
                        + "\n"
                        + "public class CipherGetInstanceDES {\n"
                        + "  private void foo() throws Exception {\n"
                        + "    Cipher.getInstance(\"DES\");\n"
                        + "  }\n"
                        + "}\n"))
                .run()
                .expect(expected);
    }

    public void testCipherGetInstanceAESECB() throws Exception {
        String expected = ""
                + "src/test/pkg/CipherGetInstanceAESECB.java:7: Warning: ECB encryption mode should not be used [GetInstance]\n"
                + "    Cipher.getInstance(\"AES/ECB/NoPadding\");\n"
                + "                       ~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import javax.crypto.Cipher;\n"
                        + "\n"
                        + "public class CipherGetInstanceAESECB {\n"
                        + "  private void foo() throws Exception {\n"
                        + "    Cipher.getInstance(\"AES/ECB/NoPadding\");\n"
                        + "  }\n"
                        + "}\n"))
                .run()
                .expect(expected);
    }

    public void testCipherGetInstanceAESCBC() throws Exception {
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import javax.crypto.Cipher;\n"
                        + "\n"
                        + "public class CipherGetInstanceAESCBC {\n"
                        + "  private void foo() throws Exception {\n"
                        + "    Cipher.getInstance(\"AES/CBC/NoPadding\");\n"
                        + "  }\n"
                        + "}\n"))
                .run()
                .expectClean();
    }

    // http://b.android.com/204099 Generate a warning only when ECB mode
    // is used with symmetric ciphers such as DES.
    public void testAsymmetricCipherRSA() throws Exception {
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import javax.crypto.Cipher;\n"
                        + "\n"
                        + "public class CipherGetInstanceRSA {\n"
                        + "  private void foo() throws Exception {\n"
                        + "    Cipher.getInstance(\"RSA/ECB/NoPadding\");\n"
                        + "  }\n"
                        + "}\n"))
                .run()
                .expectClean();
    }

    public void testResolveConstants() throws Exception {
        String expected = ""
                + "src/test/pkg/CipherGetInstanceTest.java:10: Warning: ECB encryption mode should not be used (was \"DES/ECB/NoPadding\") [GetInstance]\n"
                + "        Cipher des = Cipher.getInstance(Constants.DES);\n"
                + "                                        ~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import java.security.NoSuchAlgorithmException;\n"
                        + "\n"
                        + "import javax.crypto.Cipher;\n"
                        + "import javax.crypto.NoSuchPaddingException;\n"
                        + "\n"
                        + "public class CipherGetInstanceTest {\n"
                        + "    public void test() throws NoSuchPaddingException, NoSuchAlgorithmException {\n"
                        + "        Cipher des = Cipher.getInstance(Constants.DES);\n"
                        + "    }\n"
                        + "\n"
                        + "    public static class Constants {\n"
                        + "        public static final String DES = \"DES/ECB/NoPadding\";\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expect(expected);
    }
}