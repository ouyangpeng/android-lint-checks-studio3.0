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

import com.android.tools.lint.checks.infrastructure.TestFile.JarTestFile;
import com.android.tools.lint.detector.api.Detector;

public class AppCompatCustomViewDetectorTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new AppCompatCustomViewDetector();
    }

    public void test() throws Exception {
        String expected = ""
                + "src/test/pkg/TestAppCompatSuperClasses.java:23: Error: This custom view should extend android.support.v7.widget.AppCompatButton instead [AppCompatCustomView]\n"
                + "    public class MyButton1 extends Button { // ERROR\n"
                + "                                   ~~~~~~\n"
                + "src/test/pkg/TestAppCompatSuperClasses.java:28: Error: This custom view should extend android.support.v7.widget.AppCompatButton instead [AppCompatCustomView]\n"
                + "    public class MyButton2 extends Button implements Runnable { // ERROR\n"
                + "                                   ~~~~~~\n"
                + "src/test/pkg/TestAppCompatSuperClasses.java:47: Error: This custom view should extend android.support.v7.widget.AppCompatEditText instead [AppCompatCustomView]\n"
                + "    public class MyEditText extends EditText { // ERROR\n"
                + "                                    ~~~~~~~~\n"
                + "src/test/pkg/TestAppCompatSuperClasses.java:51: Error: This custom view should extend android.support.v7.widget.AppCompatTextView instead [AppCompatCustomView]\n"
                + "    public class MyTextView extends TextView { // ERROR\n"
                + "                                    ~~~~~~~~\n"
                + "src/test/pkg/TestAppCompatSuperClasses.java:55: Error: This custom view should extend android.support.v7.widget.AppCompatCheckBox instead [AppCompatCustomView]\n"
                + "    public class MyCheckBox extends CheckBox { // ERROR\n"
                + "                                    ~~~~~~~~\n"
                + "src/test/pkg/TestAppCompatSuperClasses.java:59: Error: This custom view should extend android.support.v7.widget.AppCompatCheckedTextView instead [AppCompatCustomView]\n"
                + "    public class MyCheckedTextView extends CheckedTextView { // ERROR\n"
                + "                                           ~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/TestAppCompatSuperClasses.java:63: Error: This custom view should extend android.support.v7.widget.AppCompatImageButton instead [AppCompatCustomView]\n"
                + "    public class MyImageButton extends ImageButton { // ERROR\n"
                + "                                       ~~~~~~~~~~~\n"
                + "src/test/pkg/TestAppCompatSuperClasses.java:67: Error: This custom view should extend android.support.v7.widget.AppCompatImageView instead [AppCompatCustomView]\n"
                + "    public class MyImageView extends ImageView { // ERROR\n"
                + "                                     ~~~~~~~~~\n"
                + "src/test/pkg/TestAppCompatSuperClasses.java:71: Error: This custom view should extend android.support.v7.widget.AppCompatMultiAutoCompleteTextView instead [AppCompatCustomView]\n"
                + "    public class MyMultiAutoCompleteTextView extends MultiAutoCompleteTextView { // ERROR\n"
                + "                                                     ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/TestAppCompatSuperClasses.java:75: Error: This custom view should extend android.support.v7.widget.AppCompatAutoCompleteTextView instead [AppCompatCustomView]\n"
                + "    public class MyAutoCompleteTextView extends AutoCompleteTextView { // ERROR\n"
                + "                                                ~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/TestAppCompatSuperClasses.java:79: Error: This custom view should extend android.support.v7.widget.AppCompatRadioButton instead [AppCompatCustomView]\n"
                + "    public class MyRadioButton extends RadioButton { // ERROR\n"
                + "                                       ~~~~~~~~~~~\n"
                + "src/test/pkg/TestAppCompatSuperClasses.java:83: Error: This custom view should extend android.support.v7.widget.AppCompatRatingBar instead [AppCompatCustomView]\n"
                + "    public class MyRatingBar extends RatingBar { // ERROR\n"
                + "                                     ~~~~~~~~~\n"
                + "src/test/pkg/TestAppCompatSuperClasses.java:87: Error: This custom view should extend android.support.v7.widget.AppCompatSeekBar instead [AppCompatCustomView]\n"
                + "    public class MySeekBar extends SeekBar { // ERROR\n"
                + "                                   ~~~~~~~\n"
                + "src/test/pkg/TestAppCompatSuperClasses.java:91: Error: This custom view should extend android.support.v7.widget.AppCompatSpinner instead [AppCompatCustomView]\n"
                + "    public class MySpinner extends Spinner { // ERROR\n"
                + "                                   ~~~~~~~\n"
                + "14 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.annotation.SuppressLint;\n"
                        + "import android.content.Context;\n"
                        + "import android.widget.AutoCompleteTextView;\n"
                        + "import android.widget.Button;\n"
                        + "import android.widget.CalendarView;\n"
                        + "import android.widget.CheckBox;\n"
                        + "import android.widget.CheckedTextView;\n"
                        + "import android.widget.Chronometer;\n"
                        + "import android.widget.EditText;\n"
                        + "import android.widget.ImageButton;\n"
                        + "import android.widget.ImageView;\n"
                        + "import android.widget.MultiAutoCompleteTextView;\n"
                        + "import android.widget.RadioButton;\n"
                        + "import android.widget.RatingBar;\n"
                        + "import android.widget.SeekBar;\n"
                        + "import android.widget.Spinner;\n"
                        + "import android.widget.TextView;\n"
                        + "\n"
                        + "@SuppressWarnings(\"unused\")\n"
                        + "public class TestAppCompatSuperClasses {\n"
                        + "    public class MyButton1 extends Button { // ERROR\n"
                        + "        public MyButton1(Context context) { super(context); }\n"
                        + "    }\n"
                        + "\n"
                        + "    // Check extends+implements list\n"
                        + "    public class MyButton2 extends Button implements Runnable { // ERROR\n"
                        + "        public MyButton2(Context context) { super(context); }\n"
                        + "\n"
                        + "        @Override\n"
                        + "        public void run() {\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    public class MyButton3 extends MyButton1 { // Indirect: OK\n"
                        + "        public MyButton3(Context context) { super(context); }\n"
                        + "    }\n"
                        + "    \n"
                        + "    @SuppressLint(\"AppCompatCustomView\")\n"
                        + "    public class MyButton4 extends Button { // Suppressed: OK\n"
                        + "        public MyButton4(Context context) { super(context); }\n"
                        + "    }\n"
                        + "\n"
                        + "    // Other widgets\n"
                        + "    \n"
                        + "    public class MyEditText extends EditText { // ERROR\n"
                        + "        public MyEditText(Context context) { super(context); }\n"
                        + "    }\n"
                        + "\n"
                        + "    public class MyTextView extends TextView { // ERROR\n"
                        + "        public MyTextView(Context context) { super(context); }\n"
                        + "    }\n"
                        + "\n"
                        + "    public class MyCheckBox extends CheckBox { // ERROR\n"
                        + "        public MyCheckBox(Context context) { super(context); }\n"
                        + "    }\n"
                        + "\n"
                        + "    public class MyCheckedTextView extends CheckedTextView { // ERROR\n"
                        + "        public MyCheckedTextView(Context context) { super(context); }\n"
                        + "    }\n"
                        + "\n"
                        + "    public class MyImageButton extends ImageButton { // ERROR\n"
                        + "        public MyImageButton(Context context) { super(context); }\n"
                        + "    }\n"
                        + "\n"
                        + "    public class MyImageView extends ImageView { // ERROR\n"
                        + "        public MyImageView(Context context) { super(context); }\n"
                        + "    }\n"
                        + "\n"
                        + "    public class MyMultiAutoCompleteTextView extends MultiAutoCompleteTextView { // ERROR\n"
                        + "        public MyMultiAutoCompleteTextView(Context context) { super(context); }\n"
                        + "    }\n"
                        + "\n"
                        + "    public class MyAutoCompleteTextView extends AutoCompleteTextView { // ERROR\n"
                        + "        public MyAutoCompleteTextView(Context context) { super(context); }\n"
                        + "    }\n"
                        + "\n"
                        + "    public class MyRadioButton extends RadioButton { // ERROR\n"
                        + "        public MyRadioButton(Context context) { super(context); }\n"
                        + "    }\n"
                        + "\n"
                        + "    public class MyRatingBar extends RatingBar { // ERROR\n"
                        + "        public MyRatingBar(Context context) { super(context); }\n"
                        + "    }\n"
                        + "\n"
                        + "    public class MySeekBar extends SeekBar { // ERROR\n"
                        + "        public MySeekBar(Context context) { super(context); }\n"
                        + "    }\n"
                        + "\n"
                        + "    public class MySpinner extends Spinner { // ERROR\n"
                        + "        public MySpinner(Context context) { super(context); }\n"
                        + "    }\n"
                        + "\n"
                        + "    // No current appcompat delegates\n"
                        + "\n"
                        + "    public class MyCalendarView extends CalendarView { // OK\n"
                        + "        public MyCalendarView(Context context) { super(context); }\n"
                        + "    }\n"
                        + "\n"
                        + "    public class MyChronometer extends Chronometer { // OK\n"
                        + "        public MyChronometer(Context context) { super(context); }\n"
                        + "    }\n"
                        + "}\n"),
                appCompatJar)
                .run()
                .expect(expected);
    }

    public void testNoWarningsWithoutAppCompatDependency() throws Exception {
        lint().files(mTestClass).run().expectClean();
    }

    public void testWarningsForMinSdk20() throws Exception {
        String expected = ""
                + "src/test/pkg/MyButton.java:7: Error: This custom view should extend android.support.v7.widget.AppCompatButton instead [AppCompatCustomView]\n"
                + "public class MyButton extends Button implements Runnable {\n"
                + "                              ~~~~~~\n"
                + "1 errors, 0 warnings\n";
        lint().files(mTestClass, appCompatJar, manifest().minSdk(20)).run().expect(expected);
    }

    public void testWarningsForMinSdkVersion22() throws Exception {
        // We're not applying a minSdkVersion filter yet/ever
        String expected = ""
                + "src/test/pkg/MyButton.java:7: Error: This custom view should extend android.support.v7.widget.AppCompatButton instead [AppCompatCustomView]\n"
                + "public class MyButton extends Button implements Runnable {\n"
                + "                              ~~~~~~\n"
                + "1 errors, 0 warnings\n";
        lint().files(mTestClass, appCompatJar, manifest().minSdk(20)).run().expect(expected);
    }

    public void testQuickfix() throws Exception {
        lint().files(mTestClass, appCompatJar, manifest().minSdk(20)).run().expectFixDiffs(""
                + "Fix for src/test/pkg/MyButton.java line 6: Extend AppCompat widget instead:\n"
                + "@@ -7 +7\n"
                + "- public class MyButton extends Button implements Runnable {\n"
                + "+ public class MyButton extends android.support.v7.widget.AppCompatButton implements Runnable {\n");
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mTestClass = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.content.Context;\n"
            + "import android.util.AttributeSet;\n"
            + "import android.widget.Button;\n"
            + "\n"
            + "public class MyButton extends Button implements Runnable {\n"
            + "    public MyButton(Context context, AttributeSet attrs, int defStyleAttr) {\n"
            + "        super(context, attrs, defStyleAttr);\n"
            + "    }\n"
            + "\n"
            + "    @Override\n"
            + "    public void run() {\n"
            + "    }\n"
            + "}\n");

    // Dummy file
    private final JarTestFile appCompatJar = jar("libs/appcompat-v7-18.0.0.jar");
}