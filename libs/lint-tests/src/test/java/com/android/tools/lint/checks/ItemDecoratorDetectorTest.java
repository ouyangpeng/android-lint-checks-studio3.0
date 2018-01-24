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

@SuppressWarnings({"javadoc", "ClassNameDiffersFromFileName", "ConstantConditions"})
public class ItemDecoratorDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new ItemDecoratorDetector();
    }

    public void test() throws Exception {
        assertEquals(""
                + "src/com/example/android/supportv7/widget/decorator/DividerItemDecoration.java:11: Warning: Replace with android.support.v7.widget.DividerItemDecoration? [DuplicateDivider]\n"
                + "public abstract class DividerItemDecoration extends RecyclerView.ItemDecoration {\n"
                + "                      ~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",
            lintProject(
                    java(""
                            + "package com.example.android.supportv7.widget.decorator;\n"
                            + "\n"
                            + "import android.content.Context;\n"
                            + "import android.content.res.TypedArray;\n"
                            + "import android.graphics.Canvas;\n"
                            + "import android.graphics.Rect;\n"
                            + "import android.graphics.drawable.Drawable;\n"
                            + "import android.support.v7.widget.RecyclerView;\n"
                            + "import android.view.View;\n"
                            + "\n"
                            + "public abstract class DividerItemDecoration extends RecyclerView.ItemDecoration {\n"
                            + "\n"
                            + "    private static final int[] ATTRS = new int[]{\n"
                            + "            android.R.attr.listDivider\n"
                            + "    };\n"
                            + "\n"
                            + "    public static int HORIZONTAL_LIST;\n"
                            + "\n"
                            + "    public static int VERTICAL_LIST;\n"
                            + "\n"
                            + "    private Drawable mDivider;\n"
                            + "\n"
                            + "    private int mOrientation;\n"
                            + "}"
                            + "\n"),
                    java(""
                            + "package android.support.v7.widget;\n"
                            + "public class RecyclerView {\n"
                            + "    public abstract static class ItemDecoration {\n"
                            + "    }\n"
                            + "\n"
                            + "}")
            ));
    }
}
