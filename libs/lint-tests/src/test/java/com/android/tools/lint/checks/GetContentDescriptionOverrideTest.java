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

public class GetContentDescriptionOverrideTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new GetContentDescriptionOverrideDetector();
    }

    public void testGetContentDescriptionOverrideExtendingView() throws Exception {
        assertEquals(
                "src/test/pkg/MyView.java:13: Error: Overriding getContentDescription() on a View is not recommended [GetContentDescriptionOverride]\n"
                        + "    public CharSequence getContentDescription() {\n"
                        + "                        ~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n",
                lintProject(
                        java(
                                "src/test/pkg/MyView.java",
                                ""
                                        + "package test.pkg;\n"
                                        + "\n"
                                        + "import android.content.Context;\n"
                                        + "import android.view.View;\n"
                                        + "\n"
                                        + "public class MyView extends View {\n"
                                        + "\n"
                                        + "    public MyView(Context context) {\n"
                                        + "        super(context);\n"
                                        + "    }\n"
                                        + "\n"
                                        + "    @Override\n"
                                        + "    public CharSequence getContentDescription() {\n"
                                        + "        return \"\";\n"
                                        + "    }\n"
                                        + "}")));
    }

    public void testGetContentDescriptionOverrideViewHierarchy() throws Exception {
        assertEquals(
                "src/test/pkg/ParentView.java:12: Error: Overriding getContentDescription() on a View is not recommended [GetContentDescriptionOverride]\n"
                        + "    public CharSequence getContentDescription() {\n"
                        + "                        ~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n",
                lintProject(
                        java(
                                "src/test/pkg/ParentView.java",
                                ""
                                        + "package test.pkg;\n"
                                        + "\n"
                                        + "import android.content.Context;\n"
                                        + "import android.view.View;\n"
                                        + "\n"
                                        + "public class ParentView extends View {\n"
                                        + "\n"
                                        + "    public ParentView(Context context) {\n"
                                        + "        super(context);\n"
                                        + "    }\n"
                                        + "\n"
                                        + "    public CharSequence getContentDescription() {\n"
                                        + "        return \"\";\n"
                                        + "    }\n"
                                        + "}"),
                        java(
                                "src/test/pkg/ChildView.java",
                                ""
                                        + "package test.pkg;\n"
                                        + "\n"
                                        + "import android.content.Context;\n"
                                        + "import android.view.View;\n"
                                        + "\n"
                                        + "public class ChildView extends ParentView {\n"
                                        + "\n"
                                        + "    public ChildView(Context context) {\n"
                                        + "        super(context);\n"
                                        + "    }\n"
                                        + "}")));
    }

    public void testGetContentDescriptionOverrideExtendingViewWithArg() throws Exception {
        assertEquals(
                "No warnings.",
                lintProject(
                        java(
                                "src/test/pkg/MyView.java",
                                ""
                                        + "package test.pkg;\n"
                                        + "\n"
                                        + "import android.content.Context;\n"
                                        + "import android.view.View;\n"
                                        + "\n"
                                        + "public class MyView extends View {\n"
                                        + "\n"
                                        + "    public MyView(Context context) {\n"
                                        + "        super(context);\n"
                                        + "    }\n"
                                        + "\n"
                                        + "    public CharSequence getContentDescription(String arg) {\n"
                                        + "        return \"\";\n"
                                        + "    }\n"
                                        + "}")));
    }

    public void testGetContentDescriptionOverrideNotExtendingView() throws Exception {
        assertEquals(
                "No warnings.",
                lintProject(
                        java(
                                "src/test/pkg/MyPojo.java",
                                ""
                                        + "package test.pkg;\n"
                                        + "\n"
                                        + "public class MyPojo {\n"
                                        + "    public CharSequence getContentDescription() {\n"
                                        + "        return \"\";\n"
                                        + "    }\n"
                                        + "}\n")));
    }
}
