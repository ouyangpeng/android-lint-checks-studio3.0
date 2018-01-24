/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.tools.lint;

import com.android.tools.lint.checks.AbstractCheckTest;
import com.android.tools.lint.checks.UnusedResourceDetector;
import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.client.api.LintDriver;
import com.android.tools.lint.client.api.LintRequest;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class WarningTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new UnusedResourceDetector();
    }

    @Override
    protected boolean isEnabled(Issue issue) {
        return true;
    }

    public void testComparator() throws Exception {
        //noinspection all // Sample code
        File projectDir = getProjectDir(null, // Rename .txt files to .java
                java(""
                            + "package my.pgk;\n"
                            + "\n"
                            + "class Test {\n"
                            + "   private static String s = \" R.id.button1 \\\" \"; // R.id.button1 should not be considered referenced\n"
                            + "   static {\n"
                            + "       System.out.println(R.id.button2);\n"
                            + "       char c = '\"';\n"
                            + "       System.out.println(R.id.linearLayout1);\n"
                            + "   }\n"
                            + "}\n"),
                java(""
                            + "package my.pkg;\n"
                            + "public final class R {\n"
                            + "    public static final class attr {\n"
                            + "    }\n"
                            + "    public static final class drawable {\n"
                            + "        public static final int ic_launcher=0x7f020000;\n"
                            + "    }\n"
                            + "    public static final class id {\n"
                            + "        public static final int button1=0x7f050000;\n"
                            + "        public static final int button2=0x7f050004;\n"
                            + "        public static final int imageView1=0x7f050003;\n"
                            + "        public static final int include1=0x7f050005;\n"
                            + "        public static final int linearLayout1=0x7f050001;\n"
                            + "        public static final int linearLayout2=0x7f050002;\n"
                            + "    }\n"
                            + "    public static final class layout {\n"
                            + "        public static final int main=0x7f030000;\n"
                            + "        public static final int other=0x7f030001;\n"
                            + "    }\n"
                            + "    public static final class string {\n"
                            + "        public static final int app_name=0x7f040001;\n"
                            + "        public static final int hello=0x7f040000;\n"
                            + "    }\n"
                            + "}\n"),
                manifest().minSdk(14),
                xml("res/layout/accessibility.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" android:id=\"@+id/newlinear\" android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">\n"
                            + "    <Button android:text=\"Button\" android:id=\"@+id/button1\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"></Button>\n"
                            + "    <ImageView android:id=\"@+id/android_logo\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                            + "    <ImageButton android:importantForAccessibility=\"yes\" android:id=\"@+id/android_logo2\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                            + "    <Button android:text=\"Button\" android:id=\"@+id/button2\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"></Button>\n"
                            + "    <Button android:id=\"@+android:id/summary\" android:contentDescription=\"@string/label\" />\n"
                            + "    <ImageButton android:importantForAccessibility=\"no\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:src=\"@drawable/android_button\" android:focusable=\"false\" android:clickable=\"false\" android:layout_weight=\"1.0\" />\n"
                            + "</LinearLayout>\n"));

        final AtomicReference<List<Warning>> warningsHolder = new AtomicReference<>();
        TestLintClient lintClient = new TestLintClient() {
            @Override
            public String analyze(List<File> files) throws Exception {
                //String analyze = super.analyze(files);
                LintRequest lintRequest = new LintRequest(this, files);
                lintRequest.setScope(getLintScope(files));
                driver = new LintDriver(new LintDetectorTest.CustomIssueRegistry(), this,
                        lintRequest);
                configureDriver(driver);
                driver.analyze();
                warningsHolder.set(warnings);
                return null;
            }
        };
        List<File> files = Collections.singletonList(projectDir);
        lintClient.analyze(files);

        List<Warning> warnings = warningsHolder.get();
        Warning prev = null;
        for (Warning warning : warnings) {
            if (prev != null) {
                boolean equals = warning.equals(prev);
                assertEquals(equals, prev.equals(warning));
                int compare = warning.compareTo(prev);
                assertEquals(equals, compare == 0);
                assertEquals(-compare, prev.compareTo(warning));
            }
            prev = warning;
        }

        Collections.sort(warnings);

        Warning prev2 = prev;
        prev = null;
        for (Warning warning : warnings) {
            if (prev != null && prev2 != null) {
                assertTrue(warning.compareTo(prev) > 0);
                assertTrue(prev.compareTo(prev2) > 0);
                assertTrue(warning.compareTo(prev2) > 0);

                assertTrue(prev.compareTo(warning) < 0);
                assertTrue(prev2.compareTo(prev) < 0);
                assertTrue(prev2.compareTo(warning) < 0);
            }
            prev2 = prev;
            prev = warning;
        }
    }

    @Override
    protected boolean allowCompilationErrors() {
        // Some of these unit tests are still relying on source code that references
        // unresolved symbols etc.
        return true;
    }
}
