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

public class ObjectAnimatorDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new ObjectAnimatorDetector();
    }

    public void test() throws Exception {
        String expected = ""
                + "src/main/java/test/pkg/AnimatorTest.java:21: Error: The setter for this property does not match the expected signature (public void setProp2(int arg) [ObjectAnimatorBinding]\n"
                + "        ObjectAnimator.ofInt(myObject, \"prop2\", 0, 1, 2, 5).start();\n"
                + "                                       ~~~~~~~\n"
                + "    src/main/java/test/pkg/AnimatorTest.java:58: Property setter here\n"
                + "src/main/java/test/pkg/AnimatorTest.java:24: Error: Could not find property setter method setUnknown on test.pkg.AnimatorTest.MyObject [ObjectAnimatorBinding]\n"
                + "        ObjectAnimator.ofInt(myObject, \"unknown\", 0, 1, 2, 5).start();\n"
                + "                                       ~~~~~~~~~\n"
                + "src/main/java/test/pkg/AnimatorTest.java:27: Error: The setter for this property (test.pkg.AnimatorTest.MyObject.setProp3) should not be static [ObjectAnimatorBinding]\n"
                + "        ObjectAnimator.ofInt(myObject, \"prop3\", 0, 1, 2, 5).start();\n"
                + "                                       ~~~~~~~\n"
                + "    src/main/java/test/pkg/AnimatorTest.java:61: Property setter here\n"
                + "src/main/java/test/pkg/AnimatorTest.java:40: Error: Could not find property setter method setAlpha2 on android.widget.Button [ObjectAnimatorBinding]\n"
                + "        ObjectAnimator.ofArgb(button, \"alpha2\", 1, 5); // Missing\n"
                + "                                      ~~~~~~~~\n"
                + "src/main/java/test/pkg/AnimatorTest.java:55: Warning: This method is accessed from an ObjectAnimator so it should be annotated with @Keep to ensure that it is not discarded or renamed in release builds [AnimatorKeep]\n"
                + "        public void setProp1(int x) {\n"
                + "                    ~~~~~~~~~~~~~~~\n"
                + "    src/main/java/test/pkg/AnimatorTest.java:15: ObjectAnimator usage here\n"
                + "src/main/java/test/pkg/AnimatorTest.java:58: Warning: This method is accessed from an ObjectAnimator so it should be annotated with @Keep to ensure that it is not discarded or renamed in release builds [AnimatorKeep]\n"
                + "        private void setProp2(float x) {\n"
                + "                     ~~~~~~~~~~~~~~~~~\n"
                + "    src/main/java/test/pkg/AnimatorTest.java:47: ObjectAnimator usage here\n"
                + "4 errors, 2 warnings\n";

        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "\n"
                        + "import android.animation.ObjectAnimator;\n"
                        + "import android.animation.PropertyValuesHolder;\n"
                        + "import android.support.annotation.Keep;\n"
                        + "import android.view.View;\n"
                        + "import android.widget.Button;\n"
                        + "import android.animation.FloatEvaluator;\n"
                        + "@SuppressWarnings(\"unused\")\n"
                        + "public class AnimatorTest {\n"
                        + "\n"
                        + "    public void testObjectAnimator(Button button) {\n"
                        + "        Object myObject = new MyObject();\n"
                        + "        ObjectAnimator animator1 = ObjectAnimator.ofInt(myObject, \"prop1\", 0, 1, 2, 5);\n"
                        + "        animator1.setDuration(10);\n"
                        + "        animator1.start();\n"
                        + "\n"
                        + "\n"
                        + "        // Incorrect type (float parameter) warning\n"
                        + "        ObjectAnimator.ofInt(myObject, \"prop2\", 0, 1, 2, 5).start();\n"
                        + "\n"
                        + "        // Missing method warning\n"
                        + "        ObjectAnimator.ofInt(myObject, \"unknown\", 0, 1, 2, 5).start();\n"
                        + "\n"
                        + "        // Static method warning\n"
                        + "        ObjectAnimator.ofInt(myObject, \"prop3\", 0, 1, 2, 5).start();\n"
                        + "\n"
                        + "        // OK: Already marked @Keep\n"
                        + "        ObjectAnimator.ofInt(myObject, \"prop4\", 0, 1, 2, 5).start();\n"
                        + "\n"
                        + "        // OK: multi int\n"
                        + "        ObjectAnimator.ofMultiInt(myObject, \"prop4\", new int[0][]).start();\n"
                        + "\n"
                        + "        // OK: multi int\n"
                        + "        ObjectAnimator.ofMultiFloat(myObject, \"prop5\", new float[0][]).start();\n"
                        + "\n"
                        + "        // View stuff\n"
                        + "        ObjectAnimator.ofFloat(button, \"alpha\", 1, 5); // TODO: Warn about better method?, e.g. button.animate().alpha(...)\n"
                        + "        ObjectAnimator.ofArgb(button, \"alpha2\", 1, 5); // Missing\n"
                        + "    }\n"
                        + "\n"
                        + "    public void testPropertyHolder() {\n"
                        + "        Object myObject = new MyObject();\n"
                        + "\n"
                        + "        PropertyValuesHolder p1 = PropertyValuesHolder.ofInt(\"prop1\", 50);\n"
                        + "        PropertyValuesHolder p2 = PropertyValuesHolder.ofFloat(\"prop2\", 100f);\n"
                        + "        ObjectAnimator.ofPropertyValuesHolder(myObject, p1, p2).start();\n"
                        + "        ObjectAnimator.ofPropertyValuesHolder(myObject,\n"
                        + "                PropertyValuesHolder.ofInt(\"prop1\", 50),\n"
                        + "                PropertyValuesHolder.ofFloat(\"prop2\", 100f)).start();\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class MyObject {\n"
                        + "        public void setProp1(int x) {\n"
                        + "        }\n"
                        + "\n"
                        + "        private void setProp2(float x) {\n"
                        + "        }\n"
                        + "\n"
                        + "        public static void setProp3(int x) {\n"
                        + "        }\n"
                        + "\n"
                        + "        @Keep\n"
                        + "        public void setProp4(int[] x) {\n"
                        + "        }\n"
                        + "\n"
                        + "        @Keep\n"
                        + "        public void setProp5(float[] x) {\n"
                        + "        }\n"
                        + "\n"
                        + "        @Keep\n"
                        + "        public void setProp4(int x) {\n"
                        + "        }\n"
                        + "\n"
                        + "        @Keep\n"
                        + "        public void setProp5(float x) {\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    public void testEvaluators() {\n"
                        + "        Object myObject = new MyObject();\n"
                        + "        PropertyValuesHolder p1 = PropertyValuesHolder.ofObject(\"prop5\", new FloatEvaluator());\n"
                        + "        ObjectAnimator.ofPropertyValuesHolder(myObject, p1);\n"
                        + "        ObjectAnimator.ofObject(myObject, \"prop5\", new FloatEvaluator(), 1f, 2f);\n"
                        + "    }\n"
                        + "\n"
                        + "}"),
                java(""
                        + "package android.support.annotation;\n"
                        + "import java.lang.annotation.Retention;\n"
                        + "import java.lang.annotation.Target;\n"
                        + "import static java.lang.annotation.ElementType.*;\n"
                        + "import static java.lang.annotation.RetentionPolicy.CLASS;\n"
                        + "@Retention(CLASS)\n"
                        + "@Target({PACKAGE,TYPE,ANNOTATION_TYPE,CONSTRUCTOR,METHOD,FIELD})\n"
                        + "public @interface Keep {\n"
                        + "}"),
                gradle(""
                        + "android {\n"
                        + "    buildTypes {\n"
                        + "        release {\n"
                        + "            minifyEnabled true\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expect(expected);
    }

    public void testNotMinifying() throws Exception {
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "\n"
                        + "import android.animation.ObjectAnimator;\n"
                        + "import android.animation.PropertyValuesHolder;\n"
                        + "import android.support.annotation.Keep;\n"
                        + "import android.view.View;\n"
                        + "import android.widget.Button;\n"
                        + "import android.animation.FloatEvaluator;\n"
                        + "@SuppressWarnings(\"unused\")\n"
                        + "public class AnimatorTest {\n"
                        + "\n"
                        + "    public void testObjectAnimator(Button button) {\n"
                        + "        Object myObject = new MyObject();\n"
                        + "        ObjectAnimator animator1 = ObjectAnimator.ofInt(myObject, \"prop1\", 0, 1, 2, 5);\n"
                        + "        animator1.setDuration(10);\n"
                        + "        animator1.start();\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class MyObject {\n"
                        + "        public void setProp1(int x) {\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "}"),
                java(""
                        + "package android.support.annotation;\n"
                        + "import java.lang.annotation.Retention;\n"
                        + "import java.lang.annotation.Target;\n"
                        + "import static java.lang.annotation.ElementType.*;\n"
                        + "import static java.lang.annotation.RetentionPolicy.CLASS;\n"
                        + "@Retention(CLASS)\n"
                        + "@Target({PACKAGE,TYPE,ANNOTATION_TYPE,CONSTRUCTOR,METHOD,FIELD})\n"
                        + "public @interface Keep {\n"
                        + "}"),
                gradle(""
                        + "android {\n"
                        + "    buildTypes {\n"
                        + "        release {\n"
                        + "            minifyEnabled false\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .issues(ObjectAnimatorDetector.MISSING_KEEP)
                .run()
                .expectClean();
    }

    public void testFlow() throws Exception {
        String expected = ""
                + "src/test/pkg/AnimatorFlowTest.java:10: Error: The setter for this property does not match the expected signature (public void setProp1(int arg) [ObjectAnimatorBinding]\n"
                + "        PropertyValuesHolder p1 = PropertyValuesHolder.ofInt(\"prop1\", 50); // ERROR\n"
                + "                                                             ~~~~~~~\n"
                + "    src/test/pkg/AnimatorFlowTest.java:37: Property setter here\n"
                + "src/test/pkg/AnimatorFlowTest.java:14: Error: The setter for this property does not match the expected signature (public void setProp1(int arg) [ObjectAnimatorBinding]\n"
                + "    private PropertyValuesHolder field = PropertyValuesHolder.ofInt(\"prop1\", 50); // ERROR\n"
                + "                                                                    ~~~~~~~\n"
                + "    src/test/pkg/AnimatorFlowTest.java:37: Property setter here\n"
                + "src/test/pkg/AnimatorFlowTest.java:21: Error: The setter for this property does not match the expected signature (public void setProp1(int arg) [ObjectAnimatorBinding]\n"
                + "        p1 = PropertyValuesHolder.ofInt(\"prop1\", 50); // ERROR\n"
                + "                                        ~~~~~~~\n"
                + "    src/test/pkg/AnimatorFlowTest.java:37: Property setter here\n"
                + "src/test/pkg/AnimatorFlowTest.java:26: Error: The setter for this property does not match the expected signature (public void setProp1(int arg) [ObjectAnimatorBinding]\n"
                + "        PropertyValuesHolder p1 = PropertyValuesHolder.ofInt(\"prop1\", 50); // ERROR\n"
                + "                                                             ~~~~~~~\n"
                + "    src/test/pkg/AnimatorFlowTest.java:37: Property setter here\n"
                + "src/test/pkg/AnimatorFlowTest.java:33: Error: The setter for this property does not match the expected signature (public void setProp1(int arg) [ObjectAnimatorBinding]\n"
                + "        ObjectAnimator.ofPropertyValuesHolder(new MyObject(), PropertyValuesHolder.ofInt(\"prop1\", 50)).start(); // ERROR\n"
                + "                                                                                         ~~~~~~~\n"
                + "    src/test/pkg/AnimatorFlowTest.java:37: Property setter here\n"
                + "5 errors, 0 warnings\n";

        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.animation.ObjectAnimator;\n"
                        + "import android.animation.PropertyValuesHolder;\n"
                        + "\n"
                        + "@SuppressWarnings(\"unused\")\n"
                        + "public class AnimatorFlowTest {\n"
                        + "\n"
                        + "    public void testVariableInitializer() {\n"
                        + "        PropertyValuesHolder p1 = PropertyValuesHolder.ofInt(\"prop1\", 50); // ERROR\n"
                        + "        ObjectAnimator.ofPropertyValuesHolder(new MyObject(), p1).start();\n"
                        + "    }\n"
                        + "\n"
                        + "    private PropertyValuesHolder field = PropertyValuesHolder.ofInt(\"prop1\", 50); // ERROR\n"
                        + "    public void testFieldInitializer() {\n"
                        + "        ObjectAnimator.ofPropertyValuesHolder(new MyObject(), field).start();\n"
                        + "    }\n"
                        + "\n"
                        + "    public void testAssignment() {\n"
                        + "        PropertyValuesHolder p1;\n"
                        + "        p1 = PropertyValuesHolder.ofInt(\"prop1\", 50); // ERROR\n"
                        + "        ObjectAnimator.ofPropertyValuesHolder(new MyObject(), p1).start();\n"
                        + "    }\n"
                        + "\n"
                        + "    public void testReassignment() {\n"
                        + "        PropertyValuesHolder p1 = PropertyValuesHolder.ofInt(\"prop1\", 50); // ERROR\n"
                        + "        PropertyValuesHolder p2 = p1;\n"
                        + "        p1 = null;\n"
                        + "        ObjectAnimator.ofPropertyValuesHolder(new MyObject(), p2).start();\n"
                        + "    }\n"
                        + "\n"
                        + "    public void testInline() {\n"
                        + "        ObjectAnimator.ofPropertyValuesHolder(new MyObject(), PropertyValuesHolder.ofInt(\"prop1\", 50)).start(); // ERROR\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class MyObject {\n"
                        + "        public void setProp1(double z) { // ERROR\n"
                        + "        }\n"
                        + "    }\n"
                        + "}"))
                .run()
                .expect(expected);
    }

    public void test229545() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=229545

        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package com.example.objectanimatorbinding;\n"
                        + "\n"
                        + "import android.animation.ArgbEvaluator;\n"
                        + "import android.animation.ObjectAnimator;\n"
                        + "import android.databinding.DataBindingUtil;\n"
                        + "import android.graphics.Color;\n"
                        + "import android.support.v7.app.AppCompatActivity;\n"
                        + "import android.os.Bundle;\n"
                        + "import android.support.v7.widget.CardView;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "import com.example.objectanimatorbinding.databinding.ActivityMainBinding;\n"
                        + "\n"
                        + "public class MainActivity extends AppCompatActivity {\n"
                        + "\n"
                        + "    private ActivityMainBinding binding;\n"
                        + "\n"
                        + "    boolean isChecked = false;\n"
                        + "\n"
                        + "    @Override\n"
                        + "    protected void onCreate(Bundle savedInstanceState) {\n"
                        + "        super.onCreate(savedInstanceState);\n"
                        + "        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);\n"
                        + "        binding.activityMain.setOnClickListener(new View.OnClickListener() {\n"
                        + "            @Override\n"
                        + "            public void onClick(View view) {\n"
                        + "                animateColorChange(binding.activityMain, isChecked);\n"
                        + "                isChecked = !isChecked;\n"
                        + "            }\n"
                        + "        });\n"
                        + "    }\n"
                        + "\n"
                        + "    private void animateColorChange (CardView view, boolean isChecked){\n"
                        + "        ObjectAnimator backgroundColorAnimator = ObjectAnimator.ofObject(view,\n"
                        + "                \"cardBackgroundColor\",\n"
                        + "                new ArgbEvaluator(),\n"
                        + "                isChecked ? Color.BLUE : Color.GRAY,\n"
                        + "                isChecked ? Color.GRAY : Color.BLUE);\n"
                        + "        backgroundColorAnimator.setDuration(200);\n"
                        + "        backgroundColorAnimator.start();\n"
                        + "    }\n"
                        + "}"),
                gradle(""
                        + "android {\n"
                        + "    buildTypes {\n"
                        + "        release {\n"
                        + "            minifyEnabled true\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .issues(ObjectAnimatorDetector.MISSING_KEEP)
                .allowCompilationErrors()
                .run()
                .expectClean();
    }

    public void test230387() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=230387

        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.animation.ObjectAnimator;\n"
                        + "import android.animation.PropertyValuesHolder;\n"
                        + "import android.animation.ValueAnimator;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class KeepTest {\n"
                        + "    public void test(View view) {\n"
                        + "        ValueAnimator animator = ObjectAnimator.ofPropertyValuesHolder(\n"
                        + "                view,\n"
                        + "                PropertyValuesHolder.ofFloat(\"translationX\", 0)\n"
                        + "        );\n"
                        + "    }\n"
                        + "}\n"),
                gradle(""
                        + "android {\n"
                        + "    buildTypes {\n"
                        + "        release {\n"
                        + "            minifyEnabled true\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .issues(ObjectAnimatorDetector.MISSING_KEEP)
                .run()
                .expectClean();
    }

    public void testCreateValueAnimator() throws Exception {
        // Regression test which makes sure that when we use ValueAnimator.ofPropertyValuesHolder
        // to create a property holder and we don't know the associated object, we don't falsely
        // report broken properties

        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.animation.PropertyValuesHolder;\n"
                        + "import android.animation.ValueAnimator;\n"
                        + "\n"
                        + "public class MyAndroidLibraryClass {\n"
                        + "\n"
                        + "    ValueAnimator create(float fromX, float toX, float fromY, float toY) {\n"
                        + "        final PropertyValuesHolder xHolder = PropertyValuesHolder.ofFloat(\"x\", fromX, toX);\n"
                        + "        final PropertyValuesHolder yHolder = PropertyValuesHolder.ofFloat(\"y\", fromY, toY);\n"
                        + "        return ValueAnimator.ofPropertyValuesHolder(xHolder, yHolder);\n"
                        + "    }\n"
                        + "}\n"))
                .issues(ObjectAnimatorDetector.BROKEN_PROPERTY)
                .run()
                .expectClean();
    }

    public void testSuppress() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=232405
        // Ensure that we can suppress both types of issues by annotating either the
        // property binding site *or* the property declaration site

        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.animation.ObjectAnimator;\n"
                        + "import android.annotation.SuppressLint;\n"
                        + "import android.widget.Button;\n"
                        + "\n"
                        + "@SuppressWarnings(\"unused\")\n"
                        + "public class AnimatorTest {\n"
                        + "\n"
                        + "    // Suppress at the binding site\n"
                        + "    @SuppressLint({\"ObjectAnimatorBinding\", \"AnimatorKeep\"})\n"
                        + "    public void testObjectAnimator02(Button button) {\n"
                        + "        Object myObject = new MyObject();\n"
                        + "\n"
                        + "        ObjectAnimator.ofInt(myObject, \"prop0\", 0, 1, 2, 5);\n"
                        + "        ObjectAnimator.ofInt(myObject, \"prop2\", 0, 1, 2, 5).start();\n"
                        + "    }\n"
                        + "\n"
                        + "    // Suppressed at the property site\n"
                        + "    public void testObjectAnimator13(Button button) {\n"
                        + "        Object myObject = new MyObject();\n"
                        + "\n"
                        + "        ObjectAnimator.ofInt(myObject, \"prop1\", 0, 1, 2, 5);\n"
                        + "        ObjectAnimator.ofInt(myObject, \"prop3\", 0, 1, 2, 5).start();\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class MyObject {\n"
                        + "        public void setProp0(int x) {\n"
                        + "        }\n"
                        + "\n"
                        + "        @SuppressLint(\"AnimatorKeep\")\n"
                        + "        public void setProp1(int x) {\n"
                        + "        }\n"
                        + "\n"
                        + "        private void setProp2(float x) {\n"
                        + "        }\n"
                        + "\n"
                        + "        @SuppressLint(\"ObjectAnimatorBinding\")\n"
                        + "        private void setProp3(float x) {\n"
                        + "        }\n"
                        + "    }\n"
                        + "}"))
                .issues(ObjectAnimatorDetector.BROKEN_PROPERTY,
                        ObjectAnimatorDetector.MISSING_KEEP)
                .run()
                .expectClean();
    }

    public void test37136742() {
        // Regression test for 37136742
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "import android.animation.Keyframe;\n"
                        + "import android.animation.ObjectAnimator;\n"
                        + "import android.animation.PropertyValuesHolder;\n"
                        + "import android.app.Activity;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class TestObjAnimator extends Activity {\n"
                        + "    public void animate(View target) {\n"
                        + "        Keyframe kf0 = Keyframe.ofFloat(0f, 0f);\n"
                        + "        Keyframe kf1 = Keyframe.ofFloat(.5f, 360f);\n"
                        + "        Keyframe kf2 = Keyframe.ofFloat(1f, 0f);\n"
                        + "        PropertyValuesHolder pvhRotation = PropertyValuesHolder.ofKeyframe(\"rotation\", kf0, kf1, kf2);\n"
                        + "        ObjectAnimator rotationAnim = ObjectAnimator.ofPropertyValuesHolder(target, pvhRotation);\n"
                        + "        rotationAnim.setDuration(5000);\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expectClean();
    }
}
