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
public class SetJavaScriptEnabledDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new SetJavaScriptEnabledDetector();
    }

    public void test() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/SetJavaScriptEnabled.java:14: Warning: Using setJavaScriptEnabled can introduce XSS vulnerabilities into your application, review carefully. [SetJavaScriptEnabled]\n"
                + "        webView.getSettings().setJavaScriptEnabled(true); // bad\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",
            lintProject(
                    java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import android.app.Activity;\n"
                            + "import android.os.Bundle;\n"
                            + "import android.webkit.WebView;\n"
                            + "\n"
                            + "public class SetJavaScriptEnabled extends Activity {\n"
                            + "\n"
                            + "    @Override\n"
                            + "    public void onCreate(Bundle savedInstanceState) {\n"
                            + "        super.onCreate(savedInstanceState);\n"
                            + "        setContentView(R.layout.main);\n"
                            + "        WebView webView = (WebView)findViewById(R.id.webView);\n"
                            + "        webView.getSettings().setJavaScriptEnabled(true); // bad\n"
                            + "        webView.getSettings().setJavaScriptEnabled(false); // good\n"
                            + "        webView.loadUrl(\"file:///android_asset/www/index.html\");\n"
                            + "    }\n"
                            + "\n"
                            + "    // Test Suppress\n"
                            + "    // Constructor: See issue 35588\n"
                            + "    @android.annotation.SuppressLint(\"SetJavaScriptEnabled\")\n"
                            + "    public void HelloWebApp() {\n"
                            + "        WebView webView = (WebView)findViewById(R.id.webView);\n"
                            + "        webView.getSettings().setJavaScriptEnabled(true); // bad\n"
                            + "        webView.getSettings().setJavaScriptEnabled(false); // good\n"
                            + "        webView.loadUrl(\"file:///android_asset/www/index.html\");\n"
                            + "    }\n"
                            + "\n"
                            + "    public static final class R {\n"
                            + "        public static final class layout {\n"
                            + "            public static final int main = 0x7f0a0000;\n"
                            + "        }\n"
                            + "        public static final class id {\n"
                            + "            public static final int webView = 0x7f0a0001;\n"
                            + "        }\n"
                            + "    }\n"
                            + "}\n")));
    }
}
