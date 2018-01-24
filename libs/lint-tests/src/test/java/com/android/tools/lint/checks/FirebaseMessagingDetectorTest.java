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

public class FirebaseMessagingDetectorTest extends AbstractCheckTest {

    @SuppressWarnings("all") // Sample code
    final TestFile mFirebaseInstanceIdService = java(
            "src/com/google/firebase/iid/FirebaseInstanceIdService.java", ""
                    + "package com.google.firebase.iid;\n"
                    + "public class FirebaseInstanceIdService {\n"
                    + "    public void onTokenRefresh() {}\n"
                    + "}");

    @SuppressWarnings("all") // Sample code
    final TestFile mFirebaseInstanceId = java(
            "src/com/google/firebase/iid/FirebaseInstanceId.java", ""
                    + "package com.google.firebase.iid;\n"
                    + "public class FirebaseInstanceId {\n"
                    + "    private String token;"
                    + "    private FirebaseInstanceId () {\n"
                    + "        token = \"foo\";\n"
                    + "    }\n"
                    + "    public static FirebaseInstanceId getInstance() {\n"
                    + "        return new FirebaseInstanceId();\n"
                    + "    }\n"
                    + "    public String getToken() {\n"
                    + "        return token;\n"
                    + "    }\n"
                    + "}");

    public void testMissingRefreshCallback() throws Exception {
        //noinspection all // Sample code
        TestFile myInstanceIdService = java("src/test/pkg/MyFirebaseInstanceIdService.java", ""
                + "package test.pkg;\n"
                + "import com.google.firebase.iid.FirebaseInstanceId;\n"
                + "import com.google.firebase.iid.FirebaseInstanceIdService;\n"
                + "public class MyFirebaseInstanceIdService extends FirebaseInstanceIdService {\n"
                + "    public MyFirebaseInstanceIdService() {\n"
                + "        String token = FirebaseInstanceId.getInstance().getToken();\n"
                + "        sendTokenToServer(token);\n"
                + "    }\n"
                + "    private void sendTokenToServer(String token) {\n"
                + "        // update app server with token\n"
                + "    }\n"
                + "}\n");
        String expected = ""
                + "src/test/pkg/MyFirebaseInstanceIdService.java:6: Warning: getToken() called without defining onTokenRefresh callback. [MissingFirebaseInstanceTokenRefresh]\n"
                + "        String token = FirebaseInstanceId.getInstance().getToken();\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";
        String result = lintProject(mFirebaseInstanceIdService, mFirebaseInstanceId,
                myInstanceIdService);
        assertEquals(expected, result);
    }

    public void testWithRefreshCallback() throws Exception {
        //noinspection all // Sample code
        TestFile myInstanceIdService = java("src/test/pkg/MyFirebaseInstanceIdService.java", ""
                + "package test.pkg;\n"
                + "import com.google.firebase.iid.FirebaseInstanceId;\n"
                + "import com.google.firebase.iid.FirebaseInstanceIdService;\n"
                + "public class MyFirebaseInstanceIdService extends FirebaseInstanceIdService {\n"
                + "    @Override\n"
                + "    public void onTokenRefresh() {\n"
                + "        sendTokenToServer(FirebaseInstanceId.getInstance().getToken());\n"
                + "    }\n"
                + "    private void sendTokenToServer(String token) {\n"
                + "        // update app server with token\n"
                + "    }\n"
                + "}\n");
        String expected = "No warnings.";
        String result = lintProject(mFirebaseInstanceIdService, mFirebaseInstanceId,
                myInstanceIdService);
        assertEquals(expected, result);
    }

    public void testGetTokenInDifferentFileThanCallback() throws Exception {
        //noinspection all // Sample code
        TestFile myInstanceIdService = java("src/test/pkg/MyFirebaseInstanceIdService.java", ""
                + "package test.pkg;\n"
                + "import com.google.firebase.iid.FirebaseInstanceId;\n"
                + "import com.google.firebase.iid.FirebaseInstanceIdService;\n"
                + "public class MyFirebaseInstanceIdService extends FirebaseInstanceIdService {\n"
                + "    @Override\n"
                + "    public void onTokenRefresh() {\n"
                + "        sendTokenToServer(\"\");\n"
                + "    }\n"
                + "    private void sendTokenToServer(String token) {\n"
                + "        // update app server with token\n"
                + "    }\n"
                + "}\n");
        //noinspection all // Sample code
        TestFile mainActivity = java("src/test/pkg/MainActivity.java", ""
                + "package test.pkg;\n"
                + "import com.google.firebase.iid.FirebaseInstanceId;\n"
                + "public class MainActivity {\n"
                + "    public MainActivity() {\n"
                + "        FirebaseInstanceId.getInstance().getToken();\n"
                + "    }\n"
                + "}");
        String expected = "No warnings.";
        String result = lintProject(mFirebaseInstanceIdService, mFirebaseInstanceId,
                myInstanceIdService, mainActivity);
        assertEquals(expected, result);
    }

    @Override
    protected Detector getDetector() {
        return new FirebaseMessagingDetector();
    }
}
