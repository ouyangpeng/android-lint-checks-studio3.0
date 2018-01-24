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
public class ViewConstructorDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new ViewConstructorDetector();
    }

    public void test() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/bytecode/CustomView1.java:5: Warning: Custom view CustomView1 is missing constructor used by tools: (Context) or (Context,AttributeSet) or (Context,AttributeSet,int) [ViewConstructor]\n"
                + "public class CustomView1 extends View {\n"
                + "             ~~~~~~~~~~~\n"
                + "src/test/bytecode/CustomView2.java:7: Warning: Custom view CustomView2 is missing constructor used by tools: (Context) or (Context,AttributeSet) or (Context,AttributeSet,int) [ViewConstructor]\n"
                + "public class CustomView2 extends Button {\n"
                + "             ~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

            lintProject(
                classpath(),
                manifest().minSdk(10),
                java(""
                            + "package test.bytecode;\n"
                            + "\n"
                            + "import android.view.View;\n"
                            + "\n"
                            + "public class CustomView1 extends View {\n"
                            + "\tpublic CustomView1() {\n"
                            + "\t\tsuper(null);\n"
                            + "\t}\n"
                            + "}\n"),
                java(""
                            + "package test.bytecode;\n"
                            + "\n"
                            + "import android.content.Context;\n"
                            + "import android.util.AttributeSet;\n"
                            + "import android.widget.Button;\n"
                            + "\n"
                            + "public class CustomView2 extends Button {\n"
                            + "\tpublic CustomView2(boolean foo,\n"
                            + "\t\t\tContext context, AttributeSet attrs, int defStyle) {\n"
                            + "\t\tsuper(context, attrs, defStyle);\n"
                            + "\t}\n"
                            + "}\n"),
                java(""
                            + "package test.bytecode;\n"
                            + "\n"
                            + "import android.content.Context;\n"
                            + "import android.util.AttributeSet;\n"
                            + "import android.view.View;\n"
                            + "\n"
                            + "public class CustomView3 extends View {\n"
                            + "\n"
                            + "\tpublic CustomView3(Context context, AttributeSet attrs, int defStyle) {\n"
                            + "\t\tsuper(context, attrs, defStyle);\n"
                            + "\t}\n"
                            + "\n"
                            + "}\n")
                ));
    }

    public void testInheritLocal() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/CustomViewTest.java:5: Warning: Custom view CustomViewTest is missing constructor used by tools: (Context) or (Context,AttributeSet) or (Context,AttributeSet,int) [ViewConstructor]\n"
                + "public class CustomViewTest extends IntermediateCustomV {\n"
                + "             ~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",

            lintProject(
                classpath(),
                manifest().minSdk(10),
                java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import android.app.Activity;\n"
                            + "import android.widget.Button;\n"
                            + "\n"
                            + "/** Local activity */\n"
                            + "public abstract class Intermediate extends Activity {\n"
                            + "\n"
                            + "\t/** Local Custom view */\n"
                            + "\tpublic abstract static class IntermediateCustomV extends Button {\n"
                            + "\t\tpublic IntermediateCustomV() {\n"
                            + "\t\t\tsuper(null);\n"
                            + "\t\t}\n"
                            + "\t}\n"
                            + "}\n"),
                java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import test.pkg.Intermediate.IntermediateCustomV;\n"
                            + "\n"
                            + "public class CustomViewTest extends IntermediateCustomV {\n"
                            + "}\n")
                ));
    }

    public void testAbstract() throws Exception {
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",

                lintProject(
                        classpath(),
                        manifest().minSdk(10),
                        java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import android.view.View;\n"
                            + "\n"
                            + "public abstract class AbstractCustomView extends View {\n"
                            + "    public AbstractCustomView() {\n"
                            + "        super(null);\n"
                            + "    }\n"
                            + "}\n")
                ));
    }

    @SuppressWarnings("ClassNameDiffersFromFileName")
    public void testPrivate() throws Exception {
        assertEquals("No warnings.",
            lintProject(java("src/test/pkg/Test.java", ""
                + "package test.pkg;\n"
                + "\n"
                + "import android.view.View;\n"
                + "\n"
                + "public class Test {\n"
                + "    private static class PrivateCustomView extends View {\n"
                + "        public PrivateCustomView() {\n"
                + "            super(null);\n"
                + "        }\n"
                + "    }\n"
                + "}\n")));
    }
}
