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
package com.android.tools.lint.checks;

import static com.android.tools.lint.checks.AnnotationDetectorTest.SUPPORT_ANNOTATIONS_JAR_BASE64_GZIP;
import static com.android.tools.lint.checks.AnnotationDetectorTest.SUPPORT_JAR_PATH;

import com.android.tools.lint.detector.api.Detector;

public class CallSuperDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new CallSuperDetector();
    }

    public void testCallSuper() throws Exception {
        String expected = ""
                + "src/test/pkg/CallSuperTest.java:11: Error: Overriding method should call super.test1 [MissingSuperCall]\n"
                + "        protected void test1() { // ERROR\n"
                + "                       ~~~~~\n"
                + "src/test/pkg/CallSuperTest.java:14: Error: Overriding method should call super.test2 [MissingSuperCall]\n"
                + "        protected void test2() { // ERROR\n"
                + "                       ~~~~~\n"
                + "src/test/pkg/CallSuperTest.java:17: Error: Overriding method should call super.test3 [MissingSuperCall]\n"
                + "        protected void test3() { // ERROR\n"
                + "                       ~~~~~\n"
                + "src/test/pkg/CallSuperTest.java:20: Error: Overriding method should call super.test4 [MissingSuperCall]\n"
                + "        protected void test4(int arg) { // ERROR\n"
                + "                       ~~~~~\n"
                + "src/test/pkg/CallSuperTest.java:26: Error: Overriding method should call super.test5 [MissingSuperCall]\n"
                + "        protected void test5(int arg1, boolean arg2, Map<List<String>,?> arg3,  // ERROR\n"
                + "                       ~~~~~\n"
                + "src/test/pkg/CallSuperTest.java:30: Error: Overriding method should call super.test5 [MissingSuperCall]\n"
                + "        protected void test5() { // ERROR\n"
                + "                       ~~~~~\n"
                + "6 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.support.annotation.CallSuper;\n"
                        + "\n"
                        + "import java.util.List;\n"
                        + "import java.util.Map;\n"
                        + "\n"
                        + "@SuppressWarnings(\"UnusedDeclaration\")\n"
                        + "public class CallSuperTest {\n"
                        + "    private static class Child extends Parent {\n"
                        + "        protected void test1() { // ERROR\n"
                        + "        }\n"
                        + "\n"
                        + "        protected void test2() { // ERROR\n"
                        + "        }\n"
                        + "\n"
                        + "        protected void test3() { // ERROR\n"
                        + "        }\n"
                        + "\n"
                        + "        protected void test4(int arg) { // ERROR\n"
                        + "        }\n"
                        + "\n"
                        + "        protected void test4(String arg) { // OK\n"
                        + "        }\n"
                        + "\n"
                        + "        protected void test5(int arg1, boolean arg2, Map<List<String>,?> arg3,  // ERROR\n"
                        + "                             int[][] arg4, int... arg5) {\n"
                        + "        }\n"
                        + "\n"
                        + "        protected void test5() { // ERROR\n"
                        + "            super.test6(); // (wrong super)\n"
                        + "        }\n"
                        + "\n"
                        + "        protected void test6() { // OK\n"
                        + "            int x = 5;\n"
                        + "            super.test6();\n"
                        + "            System.out.println(x);\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class Parent extends ParentParent {\n"
                        + "        @CallSuper\n"
                        + "        protected void test1() {\n"
                        + "        }\n"
                        + "\n"
                        + "        protected void test3() {\n"
                        + "            super.test3();\n"
                        + "        }\n"
                        + "\n"
                        + "        @CallSuper\n"
                        + "        protected void test4(int arg) {\n"
                        + "        }\n"
                        + "\n"
                        + "        protected void test4(String arg) {\n"
                        + "        }\n"
                        + "\n"
                        + "        @CallSuper\n"
                        + "        protected void test5() {\n"
                        + "        }\n"
                        + "\n"
                        + "        @CallSuper\n"
                        + "        protected void test5(int arg1, boolean arg2, Map<List<String>,?> arg3,\n"
                        + "                             int[][] arg4, int... arg5) {\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class ParentParent extends ParentParentParent {\n"
                        + "        @CallSuper\n"
                        + "        protected void test2() {\n"
                        + "        }\n"
                        + "\n"
                        + "        @CallSuper\n"
                        + "        protected void test3() {\n"
                        + "        }\n"
                        + "\n"
                        + "        @CallSuper\n"
                        + "        protected void test6() {\n"
                        + "        }\n"
                        + "\n"
                        + "        @CallSuper\n"
                        + "        protected void test7() {\n"
                        + "        }\n"
                        + "\n"
                        + "\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class ParentParentParent {\n"
                        + "\n"
                        + "    }\n"
                        + "}\n"),
                java(""
                        + "package android.support.annotation;\n"
                        + "\n"
                        + "import java.lang.annotation.Retention;\n"
                        + "import java.lang.annotation.Target;\n"
                        + "\n"
                        + "import static java.lang.annotation.ElementType.CONSTRUCTOR;\n"
                        + "import static java.lang.annotation.ElementType.METHOD;\n"
                        + "import static java.lang.annotation.RetentionPolicy.CLASS;\n"
                        + "\n"
                        + "@Retention(CLASS)\n"
                        + "@Target({METHOD,CONSTRUCTOR})\n"
                        + "public @interface CallSuper {\n"
                        + "}\n"))
                .run()
                .expect(expected);
    }

    public void testForeignSuperAnnotations() throws Exception {
        String expected = ""
                + "src/test/pkg/OverrideTest.java:9: Error: Overriding method should call super.test [MissingSuperCall]\n"
                + "        protected void test() { // ERROR\n"
                + "                       ~~~~\n"
                + "src/test/pkg/OverrideTest.java:21: Error: Overriding method should call super.test [MissingSuperCall]\n"
                + "        protected void test() { // ERROR\n"
                + "                       ~~~~\n"
                + "2 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import javax.annotation.OverridingMethodsMustInvokeSuper;\n"
                        + "import edu.umd.cs.findbugs.annotations.OverrideMustInvoke;\n"
                        + "\n"
                        + "@SuppressWarnings(\"UnusedDeclaration\")\n"
                        + "public class OverrideTest {\n"
                        + "    private static class Child1 extends Parent1 {\n"
                        + "        protected void test() { // ERROR\n"
                        + "        }\n"
                        + "\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class Parent1 {\n"
                        + "        @OverrideMustInvoke\n"
                        + "        protected void test() {\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class Child2 extends Parent2 {\n"
                        + "        protected void test() { // ERROR\n"
                        + "        }\n"
                        + "\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class Parent2 {\n"
                        + "        @OverridingMethodsMustInvokeSuper\n"
                        + "        protected void test() {\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"),
                java(""
                        // This is not the source code for
                        //    edu.umd.cs.findbugs.annotations.OverrideMustInvoke
                        // It's the @CallSuper annotation with the package and
                        // class name replaced; lint doesn't care about the annotation
                        // content
                        + "package edu.umd.cs.findbugs.annotations;\n"
                        + "\n"
                        + "import java.lang.annotation.Retention;\n"
                        + "import java.lang.annotation.Target;\n"
                        + "\n"
                        + "import static java.lang.annotation.ElementType.CONSTRUCTOR;\n"
                        + "import static java.lang.annotation.ElementType.METHOD;\n"
                        + "import static java.lang.annotation.RetentionPolicy.CLASS;\n"
                        + "\n"
                        + "@Retention(CLASS)\n"
                        + "@Target({METHOD,CONSTRUCTOR})\n"
                        + "public @interface OverrideMustInvoke {\n"
                        + "}\n"),
                java(""
                        // This is not the source code for
                        //    javax.annotation.OverridingMethodsMustInvokeSuper
                        // It's the @CallSuper annotation with the package and
                        // class name replaced; lint doesn't care about the annotation
                        // content
                        + "package javax.annotation;\n"
                        + "\n"
                        + "import java.lang.annotation.Retention;\n"
                        + "import java.lang.annotation.Target;\n"
                        + "\n"
                        + "import static java.lang.annotation.ElementType.CONSTRUCTOR;\n"
                        + "import static java.lang.annotation.ElementType.METHOD;\n"
                        + "import static java.lang.annotation.RetentionPolicy.CLASS;\n"
                        + "\n"
                        + "@Retention(CLASS)\n"
                        + "@Target({METHOD,CONSTRUCTOR})\n"
                        + "public @interface OverridingMethodsMustInvokeSuper {\n"
                        + "}\n"))
                .run()
                .expect(expected);
    }

    public void testCallSuperIndirect() throws Exception {
        // Ensure that when the @CallSuper is on an indirect super method,
        // we correctly check that you call the direct super method, not the ancestor.
        //
        // Regression test for
        //    https://code.google.com/p/android/issues/detail?id=174964
        //noinspection all // Sample code
        lint().files(
                java("src/test/pkg/CallSuperTest.java", ""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.support.annotation.CallSuper;\n"
                        + "\n"
                        + "import java.util.List;\n"
                        + "import java.util.Map;\n"
                        + "\n"
                        + "@SuppressWarnings(\"UnusedDeclaration\")\n"
                        + "public class CallSuperTest {\n"
                        + "    private static class Child extends Parent {\n"
                        + "        @Override\n"
                        + "        protected void test1() {\n"
                        + "            super.test1();\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class Parent extends ParentParent {\n"
                        + "        @Override\n"
                        + "        protected void test1() {\n"
                        + "            super.test1();\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class ParentParent extends ParentParentParent {\n"
                        + "        @CallSuper\n"
                        + "        protected void test1() {\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class ParentParentParent {\n"
                        + "\n"
                        + "    }\n"
                        + "}\n"),
                classpath(SUPPORT_JAR_PATH),
                base64gzip(SUPPORT_JAR_PATH, SUPPORT_ANNOTATIONS_JAR_BASE64_GZIP))
                .run()
                .expectClean();
    }

    public void testDetachFromWindow() throws Exception {
        String expected = ""
                + "src/test/pkg/DetachedFromWindow.java:7: Error: Overriding method should call super.onDetachedFromWindow [MissingSuperCall]\n"
                + "        protected void onDetachedFromWindow() {\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/DetachedFromWindow.java:26: Error: Overriding method should call super.onDetachedFromWindow [MissingSuperCall]\n"
                + "        protected void onDetachedFromWindow() {\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~\n"
                + "2 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class DetachedFromWindow {\n"
                        + "    private static class Test1 extends ViewWithDefaultConstructor {\n"
                        + "        protected void onDetachedFromWindow() {\n"
                        + "            // Error\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class Test2 extends ViewWithDefaultConstructor {\n"
                        + "        protected void onDetachedFromWindow(int foo) {\n"
                        + "            // OK: not overriding the right method\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class Test3 extends ViewWithDefaultConstructor {\n"
                        + "        protected void onDetachedFromWindow() {\n"
                        + "            // OK: Calling super\n"
                        + "            super.onDetachedFromWindow();\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class Test4 extends ViewWithDefaultConstructor {\n"
                        + "        protected void onDetachedFromWindow() {\n"
                        + "            // Error: missing detach call\n"
                        + "            int x = 1;\n"
                        + "            x++;\n"
                        + "            System.out.println(x);\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class Test5 extends Object {\n"
                        + "        protected void onDetachedFromWindow() {\n"
                        + "            // OK - not in a view\n"
                        + "            // Regression test for http://b.android.com/73571\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    public class ViewWithDefaultConstructor extends View {\n"
                        + "        public ViewWithDefaultConstructor() {\n"
                        + "            super(null);\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expect(expected);
    }

    public void testWatchFaceVisibility() throws Exception {
        String expected = ""
                + "src/test/pkg/WatchFaceTest.java:9: Error: Overriding method should call super.onVisibilityChanged [MissingSuperCall]\n"
                + "        public void onVisibilityChanged(boolean visible) { // ERROR: Missing super call\n"
                + "                    ~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.support.wearable.watchface.CanvasWatchFaceService;\n"
                        + "\n"
                        + "@SuppressWarnings(\"UnusedDeclaration\")\n"
                        + "public class WatchFaceTest extends CanvasWatchFaceService {\n"
                        + "    private static class MyEngine1 extends CanvasWatchFaceService.Engine {\n"
                        + "        @Override\n"
                        + "        public void onVisibilityChanged(boolean visible) { // ERROR: Missing super call\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class MyEngine2 extends CanvasWatchFaceService.Engine {\n"
                        + "        @Override\n"
                        + "        public void onVisibilityChanged(boolean visible) { // OK: Super called\n"
                        + "            super.onVisibilityChanged(visible);\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class MyEngine3 extends CanvasWatchFaceService.Engine {\n"
                        + "        @Override\n"
                        + "        public void onVisibilityChanged(boolean visible) { // OK: Super called sometimes\n"
                        + "            boolean something = System.currentTimeMillis() % 1 != 0;\n"
                        + "            if (visible && something) {\n"
                        + "                super.onVisibilityChanged(true);\n"
                        + "            }\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class MyEngine4 extends CanvasWatchFaceService.Engine {\n"
                        + "        public void onVisibilityChanged() { // OK: Different signature\n"
                        + "        }\n"
                        + "        public void onVisibilityChanged(int flags) { // OK: Different signature\n"
                        + "        }\n"
                        + "        public void onVisibilityChanged(boolean visible, int flags) { // OK: Different signature\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"),
                java(""
                        + "package android.support.wearable.watchface;\n"
                        + "\n"
                        + "// Unit testing stub\n"
                        + "public class WatchFaceService {\n"
                        + "    public static class Engine {\n"
                        + "        public void onVisibilityChanged(boolean visible) {\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"),
                java(""
                        + "package android.support.wearable.watchface;\n"
                        + "\n"
                        + "public class CanvasWatchFaceService extends WatchFaceService {\n"
                        + "    public static class Engine extends WatchFaceService.Engine {\n"
                        + "        public void onVisibilityChanged(boolean visible) {\n"
                        + "            super.onVisibilityChanged(visible);\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expect(expected);
    }

    public void testKotlinMissing() {
        if (skipKotlinTests()) {
            return;
        }

        lint().files(
                kotlin("package test.pkg\n" +
                        "\n" +
                        "import android.content.Context\n" +
                        "import android.view.View\n" +
                        "class MissingSuperCallLibrary(context: Context) : View(context) {\n" +
                        "    override fun onDetachedFromWindow() {\n" +
                        "    }\n" +
                        "}"))
                .incremental()
                .run()
                .expect("src/test/pkg/test.kt:6: Error: Overriding method should call super.onDetachedFromWindow [MissingSuperCall]\n" +
                        "    override fun onDetachedFromWindow() {\n" +
                        "                 ~~~~~~~~~~~~~~~~~~~~\n" +
                        "1 errors, 0 warnings\n");
    }

    public void testKotlinOk() {
        if (skipKotlinTests()) {
            return;
        }

        lint().files(
                kotlin("package test.pkg\n" +
                        "\n" +
                        "import android.content.Context\n" +
                        "import android.view.View\n" +
                        "class MissingSuperCallLibrary(context: Context) : View(context) {\n" +
                        "    override fun onDetachedFromWindow() {\n" +
                        "        super.onDetachedFromWindow();\n" +
                        "    }\n" +
                        "}"))
                .incremental()
                .run()
                .expectClean();
    }
}
