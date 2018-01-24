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

@SuppressWarnings("javadoc")
public class LeakDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new LeakDetector();
    }

    public void testStaticFields() throws Exception {
        String expected = ""
                + "src/test/pkg/LeakTest.java:18: Warning: Do not place Android context classes in static fields; this is a memory leak (and also breaks Instant Run) [StaticFieldLeak]\n"
                + "    private static Activity sField7; // LEAK!\n"
                + "            ~~~~~~\n"
                + "src/test/pkg/LeakTest.java:19: Warning: Do not place Android context classes in static fields; this is a memory leak (and also breaks Instant Run) [StaticFieldLeak]\n"
                + "    private static Fragment sField8; // LEAK!\n"
                + "            ~~~~~~\n"
                + "src/test/pkg/LeakTest.java:20: Warning: Do not place Android context classes in static fields; this is a memory leak (and also breaks Instant Run) [StaticFieldLeak]\n"
                + "    private static Button sField9; // LEAK!\n"
                + "            ~~~~~~\n"
                + "src/test/pkg/LeakTest.java:21: Warning: Do not place Android context classes in static fields (static reference to MyObject which has field mActivity pointing to Activity); this is a memory leak (and also breaks Instant Run) [StaticFieldLeak]\n"
                + "    private static MyObject sField10;\n"
                + "            ~~~~~~\n"
                + "src/test/pkg/LeakTest.java:30: Warning: Do not place Android context classes in static fields; this is a memory leak (and also breaks Instant Run) [StaticFieldLeak]\n"
                + "    private static Activity sAppContext1; // LEAK\n"
                + "            ~~~~~~\n"
                + "0 errors, 5 warnings\n";
        //noinspection all // Sample code
        lint().files(
                java("src/test/pkg/LeakTest.java", ""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.annotation.SuppressLint;\n"
                        + "import android.app.Activity;\n"
                        + "import android.content.Context;\n"
                        + "import android.app.Fragment;\n"
                        + "import android.widget.Button;\n"
                        + "import java.util.List;\n"
                        + "\n"
                        + "@SuppressWarnings(\"unused\")\n"
                        + "public class LeakTest {\n"
                        + "    private static int sField1;\n"
                        + "    private static Object sField2;\n"
                        + "    private static String sField3;\n"
                        + "    private static List sField4;\n"
                        + "    private int mField5;\n"
                        + "    private Activity mField6;\n"
                        + "    private static Activity sField7; // LEAK!\n"
                        + "    private static Fragment sField8; // LEAK!\n"
                        + "    private static Button sField9; // LEAK!\n"
                        + "    private static MyObject sField10;\n"
                        + "    private MyObject mField11;\n"
                        + "    @SuppressLint(\"StaticFieldLeak\")\n"
                        + "    private static Activity sField12;\n"
                        + "\n"
                        + "    private static class MyObject {\n"
                        + "        private int mKey;\n"
                        + "        private Activity mActivity;\n"
                        + "    }\n"
                        + "    private static Activity sAppContext1; // LEAK\n"
                        + "    private static Context sAppContext2; // Probably app context leak\n"
                        + "    private static Context applicationCtx; // Probably app context leak\n"
                        + "}\n"))
                .run()
                .expect(expected);
    }

    public void testLoader() {
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.content.Context;\n"
                        + "import android.content.Loader;\n"
                        + "\n"
                        + "public class LoaderTest {\n"
                        + "    public static class MyLoader1 extends Loader { // OK\n"
                        + "        public MyLoader1(Context context) { super(context); }\n"
                        + "    }\n"
                        + "\n"
                        + "    public class MyLoader2 extends Loader { // Leak\n"
                        + "        public MyLoader2(Context context) { super(context); }\n"
                        + "    }\n"
                        + "\n"
                        + "    public static class MyLoader3 extends Loader {\n"
                        + "        private Activity activity; // Leak\n"
                        + "        public MyLoader3(Context context) { super(context); }\n"
                        + "    }\n"
                        + "\n"
                        + "    public Loader createLoader(Context context) {\n"
                        + "        return new Loader(context) { // Leak\n"
                        + "        };\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expect(""
                        + "src/test/pkg/LoaderTest.java:12: Warning: This Loader class should be static or leaks might occur (test.pkg.LoaderTest.MyLoader2) [StaticFieldLeak]\n"
                        + "    public class MyLoader2 extends Loader { // Leak\n"
                        + "                 ~~~~~~~~~\n"
                        + "src/test/pkg/LoaderTest.java:17: Warning: This field leaks a context object [StaticFieldLeak]\n"
                        + "        private Activity activity; // Leak\n"
                        + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/LoaderTest.java:22: Warning: This Loader class should be static or leaks might occur (anonymous android.content.Loader) [StaticFieldLeak]\n"
                        + "        return new Loader(context) { // Leak\n"
                        + "               ^\n"
                        + "0 errors, 3 warnings\n");
    }

    public void testSupportLoader() {
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.content.Context;\n"
                        + "import android.support.v4.content.Loader;\n"
                        + "\n"
                        + "public class SupportLoaderTest {\n"
                        + "    public static class MyLoader1 extends Loader { // OK\n"
                        + "        public MyLoader1(Context context) { super(context); }\n"
                        + "    }\n"
                        + "\n"
                        + "    public class MyLoader2 extends Loader { // Leak\n"
                        + "        public MyLoader2(Context context) { super(context); }\n"
                        + "    }\n"
                        + "\n"
                        + "    public static class MyLoader3 extends Loader {\n"
                        + "        private Activity activity; // Leak\n"
                        + "        public MyLoader3(Context context) { super(context); }\n"
                        + "    }\n"
                        + "\n"
                        + "    public Loader createLoader(Context context) {\n"
                        + "        return new Loader(context) { // Leak\n"
                        + "        };\n"
                        + "    }\n"
                        + "}\n"),
                // Stub since support library isn't in SDK
                java(""
                        + "package android.support.v4.content;\n"
                        + "public class Loader {\n"
                        + "}\n"))
                .run()
                .expect(""
                        + "src/test/pkg/SupportLoaderTest.java:12: Warning: This Loader class should be static or leaks might occur (test.pkg.SupportLoaderTest.MyLoader2) [StaticFieldLeak]\n"
                        + "    public class MyLoader2 extends Loader { // Leak\n"
                        + "                 ~~~~~~~~~\n"
                        + "src/test/pkg/SupportLoaderTest.java:17: Warning: This field leaks a context object [StaticFieldLeak]\n"
                        + "        private Activity activity; // Leak\n"
                        + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/SupportLoaderTest.java:22: Warning: This Loader class should be static or leaks might occur (anonymous android.support.v4.content.Loader) [StaticFieldLeak]\n"
                        + "        return new Loader(context) { // Leak\n"
                        + "               ^\n"
                        + "0 errors, 3 warnings\n");
    }

    public void testTopLevelLoader() {
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.content.Context;\n"
                        + "import android.content.Loader;\n"
                        + "\n"
                        + "public abstract class SupportLoaderTest extends Loader {\n"
                        + "    private Activity activity; // Leak\n"
                        + "    public SupportLoaderTest(Context context) { super(context); }\n"
                        + "}\n"))
                .run()
                .expect(""
                        + "src/test/pkg/SupportLoaderTest.java:8: Warning: This field leaks a context object [StaticFieldLeak]\n"
                        + "    private Activity activity; // Leak\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "0 errors, 1 warnings\n");
    }

    public void testAsyncTask() {
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.os.AsyncTask;\n"
                        + "\n"
                        + "public class AsyncTaskTest {\n"
                        + "    public static class MyAsyncTask1 extends AsyncTask { // OK\n"
                        + "        @Override protected Object doInBackground(Object[] objects) { return null; }\n"
                        + "    }\n"
                        + "\n"
                        + "    public class MyAsyncTask2 extends AsyncTask { // Leak\n"
                        + "        @Override protected Object doInBackground(Object[] objects) { return null; }\n"
                        + "    }\n"
                        + "\n"
                        + "    public AsyncTask createTask() {\n"
                        + "        return new AsyncTask() { // Leak\n"
                        + "            @Override protected Object doInBackground(Object[] objects) { return null; }\n"
                        + "            android.view.View view; // Leak\n"
                        + "        };\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expect(""
                        + "src/test/pkg/AsyncTaskTest.java:10: Warning: This AsyncTask class should be static or leaks might occur (test.pkg.AsyncTaskTest.MyAsyncTask2) [StaticFieldLeak]\n"
                        + "    public class MyAsyncTask2 extends AsyncTask { // Leak\n"
                        + "                 ~~~~~~~~~~~~\n"
                        + "src/test/pkg/AsyncTaskTest.java:15: Warning: This AsyncTask class should be static or leaks might occur (anonymous android.os.AsyncTask) [StaticFieldLeak]\n"
                        + "        return new AsyncTask() { // Leak\n"
                        + "               ^\n"
                        + "src/test/pkg/AsyncTaskTest.java:17: Warning: This field leaks a context object [StaticFieldLeak]\n"
                        + "            android.view.View view; // Leak\n"
                        + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "0 errors, 3 warnings\n");
    }

    public void testAssignAppContext() {
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.Context;\n"
                        + "\n"
                        + "public class StaticFieldTest {\n"
                        + "    public static Context context;\n"
                        + "\n"
                        + "    public StaticFieldTest(Context c) {\n"
                        + "        context = c.getApplicationContext();\n"
                        + "    }\n"
                        + "\n"
                        + "    public StaticFieldTest() {\n"
                        + "        context = null;\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expectClean();
    }

    public void testNoAssignAppContext() {
        // Regression test for 62318813; prior to this fix this code would trigger
        // an NPE in lint

        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.Context;\n"
                        + "\n"
                        + "public class StaticFieldTest {\n"
                        + "    public static Context context;\n"
                        + "\n"
                        + "    public StaticFieldTest(Context c) {\n"
                        + "    }\n"
                        + "\n"
                        + "    public StaticFieldTest() {\n"
                        + "        context = null;\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expect(""
                        + "src/test/pkg/StaticFieldTest.java:6: Warning: Do not place Android context classes in static fields; this is a memory leak (and also breaks Instant Run) [StaticFieldLeak]\n"
                        + "    public static Context context;\n"
                        + "           ~~~~~~\n"
                        + "0 errors, 1 warnings\n");
    }

    public void testLifeCycle() {
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.arch.lifecycle.ViewModel;\n"
                        + "import android.content.Context;\n"
                        + "import android.view.View;\n"
                        + "import android.widget.LinearLayout;\n"
                        + "\n"
                        + "public class MyModel extends ViewModel {\n"
                        + "    private String myString; // OK\n"
                        + "    private LinearLayout myLayout; // ERROR\n"
                        + "    private InnerClass2 myObject; // ERROR\n"
                        + "    private Context myContext; // ERROR\n"
                        + "\n"
                        + "    public static class InnerClass1 {\n"
                        + "        public View view; // OK\n"
                        + "    }\n"
                        + "\n"
                        + "    public static class InnerClass2 {\n"
                        + "        public View view; // OK\n"
                        + "    }\n"
                        + "}\n"),
                java(""
                        + "package android.arch.lifecycle;\n"
                        + "public class ViewModel { }\n"))
                .run()
                .expect(""
                        + "src/test/pkg/MyModel.java:10: Warning: This field leaks a context object [StaticFieldLeak]\n"
                        + "    private LinearLayout myLayout; // ERROR\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/MyModel.java:12: Warning: This field leaks a context object [StaticFieldLeak]\n"
                        + "    private Context myContext; // ERROR\n"
                        + "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "0 errors, 2 warnings\n");
    }
}
