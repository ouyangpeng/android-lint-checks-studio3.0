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

import static com.android.tools.lint.checks.CleanupDetector.SHARED_PREF;

import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings({"javadoc", "ClassNameDiffersFromFileName"})
public class CleanupDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new CleanupDetector();
    }

    public void testRecycle() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/RecycleTest.java:56: Warning: This TypedArray should be recycled after use with #recycle() [Recycle]\n"
                + "  final TypedArray a = getContext().obtainStyledAttributes(attrs,\n"
                + "                                    ~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RecycleTest.java:63: Warning: This TypedArray should be recycled after use with #recycle() [Recycle]\n"
                + "  final TypedArray a = getContext().obtainStyledAttributes(new int[0]);\n"
                + "                                    ~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RecycleTest.java:79: Warning: This VelocityTracker should be recycled after use with #recycle() [Recycle]\n"
                + "  VelocityTracker tracker = VelocityTracker.obtain();\n"
                + "                                            ~~~~~~\n"
                + "src/test/pkg/RecycleTest.java:92: Warning: This MotionEvent should be recycled after use with #recycle() [Recycle]\n"
                + "  MotionEvent event1 = MotionEvent.obtain(null);\n"
                + "                                   ~~~~~~\n"
                + "src/test/pkg/RecycleTest.java:93: Warning: This MotionEvent should be recycled after use with #recycle() [Recycle]\n"
                + "  MotionEvent event2 = MotionEvent.obtainNoHistory(null);\n"
                + "                                   ~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RecycleTest.java:98: Warning: This MotionEvent should be recycled after use with #recycle() [Recycle]\n"
                + "  MotionEvent event2 = MotionEvent.obtainNoHistory(null); // Not recycled\n"
                + "                                   ~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/RecycleTest.java:103: Warning: This MotionEvent should be recycled after use with #recycle() [Recycle]\n"
                + "  MotionEvent event1 = MotionEvent.obtain(null);  // Not recycled\n"
                + "                                   ~~~~~~\n"
                + "src/test/pkg/RecycleTest.java:129: Warning: This Parcel should be recycled after use with #recycle() [Recycle]\n"
                + "  Parcel myparcel = Parcel.obtain();\n"
                + "                           ~~~~~~\n"
                + "src/test/pkg/RecycleTest.java:190: Warning: This TypedArray should be recycled after use with #recycle() [Recycle]\n"
                + "        final TypedArray a = getContext().obtainStyledAttributes(attrs,  // Not recycled\n"
                + "                                          ~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 9 warnings\n",

            lintProject(
                classpath(),
                manifest().minSdk(4),
                projectProperties().compileSdk(19),
                java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "\n"
                            + "\n"
                            + "import android.annotation.SuppressLint;\n"
                            + "import android.content.Context;\n"
                            + "import android.content.res.TypedArray;\n"
                            + "import android.os.Message;\n"
                            + "import android.os.Parcel;\n"
                            + "import android.util.AttributeSet;\n"
                            + "import android.view.MotionEvent;\n"
                            + "import android.view.VelocityTracker;\n"
                            + "import android.view.View;\n"
                            + "\n"
                            + "@SuppressWarnings(\"unused\")\n"
                            + "public class RecycleTest extends View {\n"
                            + "\t// ---- Check recycling TypedArrays ----\n"
                            + "\n"
                            + "\tpublic RecycleTest(Context context, AttributeSet attrs, int defStyle) {\n"
                            + "\t\tsuper(context, attrs, defStyle);\n"
                            + "\t}\n"
                            + "\n"
                            + "\tpublic void ok1(AttributeSet attrs, int defStyle) {\n"
                            + "\t\tfinal TypedArray a = getContext().obtainStyledAttributes(attrs,\n"
                            + "\t\t\t\tR.styleable.MyView, defStyle, 0);\n"
                            + "\t\tString example = a.getString(R.styleable.MyView_exampleString);\n"
                            + "\t\ta.recycle();\n"
                            + "\t}\n"
                            + "\n"
                            + "\tpublic void ok2(AttributeSet attrs, int defStyle) {\n"
                            + "\t\tfinal TypedArray a = getContext().obtainStyledAttributes(attrs,\n"
                            + "\t\t\t\tR.styleable.MyView, defStyle, 0);\n"
                            + "\t\tString example = a.getString(R.styleable.MyView_exampleString);\n"
                            + "\t\t// If there's complicated logic, don't flag\n"
                            + "\t\tif (something()) {\n"
                            + "\t\t\ta.recycle();\n"
                            + "\t\t}\n"
                            + "\t}\n"
                            + "\n"
                            + "\tpublic TypedArray ok3(AttributeSet attrs, int defStyle) {\n"
                            + "\t\t// Value passes out of method: don't flag, caller might be recycling\n"
                            + "\t\treturn getContext().obtainStyledAttributes(attrs, R.styleable.MyView,\n"
                            + "\t\t\t\tdefStyle, 0);\n"
                            + "\t}\n"
                            + "\n"
                            + "\tprivate TypedArray myref;\n"
                            + "\n"
                            + "\tpublic void ok4(AttributeSet attrs, int defStyle) {\n"
                            + "\t\t// Value stored in a field: might be recycled later\n"
                            + "\t\tTypedArray ref = getContext().obtainStyledAttributes(attrs,\n"
                            + "\t\t\t\tR.styleable.MyView, defStyle, 0);\n"
                            + "\t\tmyref = ref;\n"
                            + "\t}\n"
                            + "\n"
                            + "\tpublic void wrong1(AttributeSet attrs, int defStyle) {\n"
                            + "\t\tfinal TypedArray a = getContext().obtainStyledAttributes(attrs,\n"
                            + "\t\t\t\tR.styleable.MyView, defStyle, 0);\n"
                            + "\t\tString example = a.getString(R.styleable.MyView_exampleString);\n"
                            + "\t\t// a.recycle();\n"
                            + "\t}\n"
                            + "\n"
                            + "\tpublic void wrong2(AttributeSet attrs, int defStyle) {\n"
                            + "\t\tfinal TypedArray a = getContext().obtainStyledAttributes(new int[0]);\n"
                            + "\t\t// a.recycle();\n"
                            + "\t}\n"
                            + "\n"
                            + "\tpublic void unknown(AttributeSet attrs, int defStyle) {\n"
                            + "\t\tfinal TypedArray a = getContext().obtainStyledAttributes(attrs,\n"
                            + "\t\t\t\tR.styleable.MyView, defStyle, 0);\n"
                            + "\t\t// We don't know what this method is (usually it will be in a different\n"
                            + "\t\t// class)\n"
                            + "\t\t// so don't flag it; it might recycle\n"
                            + "\t\thandle(a);\n"
                            + "\t}\n"
                            + "\n"
                            + "\t// ---- Check recycling VelocityTracker ----\n"
                            + "\n"
                            + "\tpublic void tracker() {\n"
                            + "\t\tVelocityTracker tracker = VelocityTracker.obtain();\n"
                            + "\t}\n"
                            + "\n"
                            + "\t// ---- Check recycling Message ----\n"
                            + "\n"
                            + "\tpublic void message() {\n"
                            + "\t\tMessage message1 = getHandler().obtainMessage();\n"
                            + "\t\tMessage message2 = Message.obtain();\n"
                            + "\t}\n"
                            + "\n"
                            + "\t// ---- Check recycling MotionEvent ----\n"
                            + "\n"
                            + "\tpublic void motionEvent() {\n"
                            + "\t\tMotionEvent event1 = MotionEvent.obtain(null);\n"
                            + "\t\tMotionEvent event2 = MotionEvent.obtainNoHistory(null);\n"
                            + "\t}\n"
                            + "\n"
                            + "\tpublic void motionEvent2() {\n"
                            + "\t\tMotionEvent event1 = MotionEvent.obtain(null); // OK\n"
                            + "\t\tMotionEvent event2 = MotionEvent.obtainNoHistory(null); // Not recycled\n"
                            + "\t\tevent1.recycle();\n"
                            + "\t}\n"
                            + "\n"
                            + "\tpublic void motionEvent3() {\n"
                            + "\t\tMotionEvent event1 = MotionEvent.obtain(null);  // Not recycled\n"
                            + "\t\tMotionEvent event2 = MotionEvent.obtain(event1);\n"
                            + "\t\tevent2.recycle();\n"
                            + "\t}\n"
                            + "\n"
                            + "\t// ---- Using recycled objects ----\n"
                            + "\n"
                            + "\tpublic void recycled() {\n"
                            + "\t\tMotionEvent event1 = MotionEvent.obtain(null);  // Not recycled\n"
                            + "\t\tevent1.recycle();\n"
                            + "\t\tint contents2 = event1.describeContents(); // BAD, after recycle\n"
                            + "\t\tfinal TypedArray a = getContext().obtainStyledAttributes(new int[0]);\n"
                            + "\t\tString example = a.getString(R.styleable.MyView_exampleString); // OK\n"
                            + "\t\ta.recycle();\n"
                            + "\t\texample = a.getString(R.styleable.MyView_exampleString); // BAD, after recycle\n"
                            + "\t}\n"
                            + "\n"
                            + "\t// ---- Check recycling Parcel ----\n"
                            + "\n"
                            + "\tpublic void parcelOk() {\n"
                            + "\t\tParcel myparcel = Parcel.obtain();\n"
                            + "\t\tmyparcel.createBinderArray();\n"
                            + "\t\tmyparcel.recycle();\n"
                            + "\t}\n"
                            + "\n"
                            + "\tpublic void parcelMissing() {\n"
                            + "\t\tParcel myparcel = Parcel.obtain();\n"
                            + "\t\tmyparcel.createBinderArray();\n"
                            + "\t}\n"
                            + "\n"
                            + "\n"
                            + "\t// ---- Check suppress ----\n"
                            + "\n"
                            + "\t@SuppressLint(\"Recycle\")\n"
                            + "\tpublic void recycledSuppress() {\n"
                            + "\t\tMotionEvent event1 = MotionEvent.obtain(null);  // Not recycled\n"
                            + "\t\tevent1.recycle();\n"
                            + "\t\tint contents2 = event1.describeContents(); // BAD, after recycle\n"
                            + "\t\tfinal TypedArray a = getContext().obtainStyledAttributes(new int[0]);\n"
                            + "\t\tString example = a.getString(R.styleable.MyView_exampleString); // OK\n"
                            + "\t}\n"
                            + "\n"
                            + "\t// ---- Stubs ----\n"
                            + "\n"
                            + "\tstatic void handle(TypedArray a) {\n"
                            + "\t\t// Unknown method\n"
                            + "\t}\n"
                            + "\n"
                            + "\tprotected boolean something() {\n"
                            + "\t\treturn true;\n"
                            + "\t}\n"
                            + "\n"
                            + "\tpublic android.content.res.TypedArray obtainStyledAttributes(\n"
                            + "\t\t\tAttributeSet set, int[] attrs, int defStyleAttr, int defStyleRes) {\n"
                            + "\t\treturn null;\n"
                            + "\t}\n"
                            + "\n"
                            + "    private static class R {\n"
                            + "        public static class styleable {\n"
                            + "            public static final int[] MyView = new int[] {};\n"
                            + "            public static final int MyView_exampleString = 2;\n"
                            + "        }\n"
                            + "    }\n"
                            + "\n"
                            + "    // Local variable tracking\n"
                            + "\n"
                            + "    @SuppressWarnings(\"UnnecessaryLocalVariable\")\n"
                            + "    public void ok5(AttributeSet attrs, int defStyle) {\n"
                            + "        final TypedArray a = getContext().obtainStyledAttributes(attrs,\n"
                            + "                R.styleable.MyView, defStyle, 0);\n"
                            + "        String example = a.getString(R.styleable.MyView_exampleString);\n"
                            + "        TypedArray b = a;\n"
                            + "        b.recycle();\n"
                            + "    }\n"
                            + "\n"
                            + "    @SuppressWarnings(\"UnnecessaryLocalVariable\")\n"
                            + "    public void ok6(AttributeSet attrs, int defStyle) {\n"
                            + "        final TypedArray a = getContext().obtainStyledAttributes(attrs,\n"
                            + "                R.styleable.MyView, defStyle, 0);\n"
                            + "        String example = a.getString(R.styleable.MyView_exampleString);\n"
                            + "        TypedArray b;\n"
                            + "        b = a;\n"
                            + "        b.recycle();\n"
                            + "    }\n"
                            + "\n"
                            + "    @SuppressWarnings({\"UnnecessaryLocalVariable\", \"UnusedAssignment\"})\n"
                            + "    public void wrong3(AttributeSet attrs, int defStyle) {\n"
                            + "        final TypedArray a = getContext().obtainStyledAttributes(attrs,  // Not recycled\n"
                            + "                R.styleable.MyView, defStyle, 0);\n"
                            + "        String example = a.getString(R.styleable.MyView_exampleString);\n"
                            + "        TypedArray b;\n"
                            + "        b = a;\n"
                            + "    }\n"
                            + "}\n")
            ));
    }

    public void testCommit() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/CommitTest.java:25: Warning: This transaction should be completed with a commit() call [CommitTransaction]\n"
                + "        getFragmentManager().beginTransaction(); // Missing commit\n"
                + "                             ~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/CommitTest.java:30: Warning: This transaction should be completed with a commit() call [CommitTransaction]\n"
                + "        FragmentTransaction transaction2 = getFragmentManager().beginTransaction(); // Missing commit\n"
                + "                                                                ~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/CommitTest.java:39: Warning: This transaction should be completed with a commit() call [CommitTransaction]\n"
                + "        getFragmentManager().beginTransaction(); // Missing commit\n"
                + "                             ~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/CommitTest.java:65: Warning: This transaction should be completed with a commit() call [CommitTransaction]\n"
                + "        getSupportFragmentManager().beginTransaction();\n"
                + "                                    ~~~~~~~~~~~~~~~~\n"
                + "0 errors, 4 warnings\n",

            lintProject(
                    classpath(),
                    manifest().minSdk(4),
                    projectProperties().compileSdk(19),
                    java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import android.app.Activity;\n"
                            + "import android.app.Fragment;\n"
                            + "import android.app.FragmentManager;\n"
                            + "import android.app.FragmentTransaction;\n"
                            + "\n"
                            + "@SuppressWarnings(\"unused\")\n"
                            + "public class CommitTest extends Activity {\n"
                            + "    public void ok1() {\n"
                            + "        getFragmentManager().beginTransaction().commit();\n"
                            + "    }\n"
                            + "\n"
                            + "    public void ok2() {\n"
                            + "        FragmentTransaction transaction = getFragmentManager().beginTransaction();\n"
                            + "        transaction.commit();\n"
                            + "    }\n"
                            + "\n"
                            + "    public void ok3() {\n"
                            + "        FragmentTransaction transaction = getFragmentManager().beginTransaction();\n"
                            + "        transaction.commitAllowingStateLoss();\n"
                            + "    }\n"
                            + "\n"
                            + "    public void error1() {\n"
                            + "        getFragmentManager().beginTransaction(); // Missing commit\n"
                            + "    }\n"
                            + "\n"
                            + "    public void error() {\n"
                            + "        FragmentTransaction transaction1 = getFragmentManager().beginTransaction();\n"
                            + "        FragmentTransaction transaction2 = getFragmentManager().beginTransaction(); // Missing commit\n"
                            + "        transaction1.commit();\n"
                            + "    }\n"
                            + "\n"
                            + "    public void error3_public() {\n"
                            + "        error3();\n"
                            + "    }\n"
                            + "\n"
                            + "    private void error3() {\n"
                            + "        getFragmentManager().beginTransaction(); // Missing commit\n"
                            + "    }\n"
                            + "\n"
                            + "    public void ok4(FragmentManager manager, String tag) {\n"
                            + "        FragmentTransaction ft = manager.beginTransaction();\n"
                            + "        ft.add(null, tag);\n"
                            + "        ft.commit();\n"
                            + "    }\n"
                            + "\n"
                            + "    // Support library\n"
                            + "\n"
                            + "    private android.support.v4.app.FragmentManager getSupportFragmentManager() {\n"
                            + "        return null;\n"
                            + "    }\n"
                            + "\n"
                            + "    public void ok5() {\n"
                            + "        getSupportFragmentManager().beginTransaction().commit();\n"
                            + "    }\n"
                            + "\n"
                            + "    public void ok6(android.support.v4.app.FragmentManager manager, String tag) {\n"
                            + "        android.support.v4.app.FragmentTransaction ft = manager.beginTransaction();\n"
                            + "        ft.add(null, tag);\n"
                            + "        ft.commit();\n"
                            + "    }\n"
                            + "\n"
                            + "    public void error4() {\n"
                            + "        getSupportFragmentManager().beginTransaction();\n"
                            + "    }\n"
                            + "\n"
                            + "    android.support.v4.app.Fragment mFragment1 = null;\n"
                            + "    Fragment mFragment2 = null;\n"
                            + "\n"
                            + "    public void ok7() {\n"
                            + "        getSupportFragmentManager().beginTransaction().add(android.R.id.content, mFragment1).commit();\n"
                            + "    }\n"
                            + "\n"
                            + "    public void ok8() {\n"
                            + "        getFragmentManager().beginTransaction().add(android.R.id.content, mFragment2).commit();\n"
                            + "    }\n"
                            + "\n"
                            + "    public void ok10() {\n"
                            + "        // Test chaining\n"
                            + "        FragmentManager fragmentManager = getFragmentManager();\n"
                            + "        fragmentManager.beginTransaction().addToBackStack(\"test\").attach(mFragment2).detach(mFragment2)\n"
                            + "        .disallowAddToBackStack().hide(mFragment2).setBreadCrumbShortTitle(\"test\")\n"
                            + "        .show(mFragment2).setCustomAnimations(0, 0).commit();\n"
                            + "    }\n"
                            + "\n"
                            + "    public void ok9() {\n"
                            + "        FragmentManager fragmentManager = getFragmentManager();\n"
                            + "        fragmentManager.beginTransaction().commit();\n"
                            + "    }\n"
                            + "\n"
                            + "    public void ok11() {\n"
                            + "        FragmentTransaction transaction;\n"
                            + "        // Comment in between variable declaration and assignment\n"
                            + "        transaction = getFragmentManager().beginTransaction();\n"
                            + "        transaction.commit();\n"
                            + "    }\n"
                            + "\n"
                            + "    public void ok12() {\n"
                            + "        FragmentTransaction transaction;\n"
                            + "        transaction = (getFragmentManager().beginTransaction());\n"
                            + "        transaction.commit();\n"
                            + "    }\n"
                            + "\n"
                            + "    @SuppressWarnings(\"UnnecessaryLocalVariable\")\n"
                            + "    public void ok13() {\n"
                            + "        FragmentTransaction transaction = getFragmentManager().beginTransaction();\n"
                            + "        FragmentTransaction temp;\n"
                            + "        temp = transaction;\n"
                            + "        temp.commitAllowingStateLoss();\n"
                            + "    }\n"
                            + "\n"
                            + "    @SuppressWarnings(\"UnnecessaryLocalVariable\")\n"
                            + "    public void ok14() {\n"
                            + "        FragmentTransaction transaction = getFragmentManager().beginTransaction();\n"
                            + "        FragmentTransaction temp = transaction;\n"
                            + "        temp.commitAllowingStateLoss();\n"
                            + "    }\n"
                            + "\n"
                            + "    public void error5(FragmentTransaction unrelated) {\n"
                            + "        FragmentTransaction transaction;\n"
                            + "        // Comment in between variable declaration and assignment\n"
                            + "        transaction = getFragmentManager().beginTransaction();\n"
                            + "        transaction = unrelated;\n"
                            + "        transaction.commit();\n"
                            + "    }\n"
                            + "\n"
                            + "    public void error6(FragmentTransaction unrelated) {\n"
                            + "        FragmentTransaction transaction;\n"
                            + "        FragmentTransaction transaction2;\n"
                            + "        // Comment in between variable declaration and assignment\n"
                            + "        transaction = getFragmentManager().beginTransaction();\n"
                            + "        transaction2 = transaction;\n"
                            + "        transaction2 = unrelated;\n"
                            + "        transaction2.commit();\n"
                            + "    }\n"
                            + "}\n"),
                    // Stubs just to be able to do type resolution without needing the full appcompat jar
                    mFragment,
                    mDialogFragment,
                    mFragmentTransaction,
                    mFragmentManager
            ));
    }

    public void testCommit2() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "No warnings.",

                lintProject(
                        classpath(),
                        manifest().minSdk(4),
                        projectProperties().compileSdk(19),
                        // Stubs just to be able to do type resolution without needing the full appcompat jar
                        mFragment,
                        mDialogFragment,
                        mFragmentTransaction,
                        mFragmentManager
                ));
    }

    public void testCommit3() throws Exception {
        //noinspection all // Sample code
        assertEquals("" +
                "No warnings.",

                lintProject(
                        classpath(),
                        manifest().minSdk(4),
                        projectProperties().compileSdk(19),
                        java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import android.support.v4.app.DialogFragment;\n"
                            + "import android.support.v4.app.Fragment;\n"
                            + "import android.support.v4.app.FragmentManager;\n"
                            + "import android.support.v4.app.FragmentTransaction;\n"
                            + "\n"
                            + "@SuppressWarnings(\"unused\")\n"
                            + "public class CommitTest2 {\n"
                            + "\tprivate void test() {\n"
                            + "\t\tFragmentTransaction transaction = getFragmentManager().beginTransaction();\n"
                            + "\t\tMyDialogFragment fragment = new MyDialogFragment();\n"
                            + "\t\tfragment.show(transaction, \"MyTag\");\n"
                            + "\t}\n"
                            + "\n"
                            + "\tprivate FragmentManager getFragmentManager() {\n"
                            + "\t\treturn null;\n"
                            + "\t}\n"
                            + "\n"
                            + "\tpublic static class MyDialogFragment extends DialogFragment {\n"
                            + "\t\tpublic MyDialogFragment() {\n"
                            + "\t\t}\n"
                            + "\n"
                            + "\t\t@Override\n"
                            + "\t\tpublic int show(FragmentTransaction transaction, String tag) {\n"
                            + "\t\t\treturn super.show(transaction, tag);\n"
                            + "\t\t}\n"
                            + "\t}\n"
                            + "}\n"),
                        // Stubs just to be able to do type resolution without needing the full appcompat jar
                        mFragment,
                        mDialogFragment,
                        mFragmentTransaction,
                        mFragmentManager
                ));
    }

    public void testCommit4() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/CommitTest3.java:35: Warning: This transaction should be completed with a commit() call [CommitTransaction]\n"
                + "    getCompatFragmentManager().beginTransaction();\n"
                + "                               ~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",

                lintProject(
                        classpath(),
                        manifest().minSdk(4),
                        projectProperties().compileSdk(19),
                        java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import android.support.v4.app.DialogFragment;\n"
                            + "import android.support.v4.app.Fragment;\n"
                            + "import android.support.v4.app.FragmentManager;\n"
                            + "import android.support.v4.app.FragmentTransaction;\n"
                            + "\n"
                            + "@SuppressWarnings(\"unused\")\n"
                            + "public class CommitTest3 {\n"
                            + "\tprivate void testOk() {\n"
                            + "\t\tandroid.app.FragmentTransaction transaction =\n"
                            + "\t\t\t\tgetFragmentManager().beginTransaction();\n"
                            + "\t\ttransaction.commit();\n"
                            + "\t\tandroid.app.FragmentTransaction transaction2 =\n"
                            + "\t\t\t\tgetFragmentManager().beginTransaction();\n"
                            + "\t\tMyDialogFragment fragment = new MyDialogFragment();\n"
                            + "\t\tfragment.show(transaction2, \"MyTag\");\n"
                            + "\t}\n"
                            + "\n"
                            + "\tprivate void testCompatOk() {\n"
                            + "\t\tandroid.support.v4.app.FragmentTransaction transaction =\n"
                            + "\t\t\t\tgetCompatFragmentManager().beginTransaction();\n"
                            + "\t\ttransaction.commit();\n"
                            + "\t\tandroid.support.v4.app.FragmentTransaction transaction2 =\n"
                            + "\t\t\t\tgetCompatFragmentManager().beginTransaction();\n"
                            + "\t\tMyCompatDialogFragment fragment = new MyCompatDialogFragment();\n"
                            + "\t\tfragment.show(transaction2, \"MyTag\");\n"
                            + "\t}\n"
                            + "\n"
                            + "\tprivate void testCompatWrong() {\n"
                            + "\t\tandroid.support.v4.app.FragmentTransaction transaction =\n"
                            + "\t\t\t\tgetCompatFragmentManager().beginTransaction();\n"
                            + "\t\ttransaction.commit();\n"
                            + "\t\tandroid.support.v4.app.FragmentTransaction transaction2 =\n"
                            + "\t\t\t\tgetCompatFragmentManager().beginTransaction();\n"
                            + "\t\tMyCompatDialogFragment fragment = new MyCompatDialogFragment();\n"
                            + "\t\tfragment.show(transaction, \"MyTag\"); // Note: Should have been transaction2!\n"
                            + "\t}\n"
                            + "\n"
                            + "\tprivate android.support.v4.app.FragmentManager getCompatFragmentManager() {\n"
                            + "\t\treturn null;\n"
                            + "\t}\n"
                            + "\n"
                            + "\tprivate android.app.FragmentManager getFragmentManager() {\n"
                            + "\t\treturn null;\n"
                            + "\t}\n"
                            + "\n"
                            + "\tpublic static class MyDialogFragment extends android.app.DialogFragment {\n"
                            + "\t\tpublic MyDialogFragment() {\n"
                            + "\t\t}\n"
                            + "\n"
                            + "\t\t@Override\n"
                            + "\t\tpublic int show(android.app.FragmentTransaction transaction, String tag) {\n"
                            + "\t\t\treturn super.show(transaction, tag);\n"
                            + "\t\t}\n"
                            + "\t}\n"
                            + "\n"
                            + "\tpublic static class MyCompatDialogFragment extends android.support.v4.app.DialogFragment {\n"
                            + "\t\tpublic MyCompatDialogFragment() {\n"
                            + "\t\t}\n"
                            + "\n"
                            + "\t\t@Override\n"
                            + "\t\tpublic int show(android.support.v4.app.FragmentTransaction transaction, String tag) {\n"
                            + "\t\t\treturn super.show(transaction, tag);\n"
                            + "\t\t}\n"
                            + "\t}\n"
                            + "}\n"),
                        // Stubs just to be able to do type resolution without needing the full appcompat jar
                        mFragment,
                        mDialogFragment,
                        mFragmentTransaction,
                        mFragmentManager
                ));
    }

    public void testCommitChainedCalls() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=135204
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/TransactionTest.java:8: Warning: This transaction should be completed with a commit() call [CommitTransaction]\n"
                + "        android.app.FragmentTransaction transaction2 = getFragmentManager().beginTransaction();\n"
                + "                                                                            ~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",

                lintProject(
                        classpath(),
                        manifest().minSdk(4),
                        projectProperties().compileSdk(19),
                        java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import android.app.Activity;\n"
                            + "\n"
                            + "public class TransactionTest extends Activity {\n"
                            + "    void test() {\n"
                            + "        android.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();\n"
                            + "        android.app.FragmentTransaction transaction2 = getFragmentManager().beginTransaction();\n"
                            + "        transaction.disallowAddToBackStack().commit();\n"
                            + "    }\n"
                            + "}\n"),
                        // Stubs just to be able to do type resolution without needing the full appcompat jar
                        mFragment,
                        mDialogFragment,
                        mFragmentTransaction,
                        mFragmentManager
                ));
    }

    public void testSurfaceTexture() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/SurfaceTextureTest.java:18: Warning: This SurfaceTexture should be freed up after use with #release() [Recycle]\n"
                + "        SurfaceTexture texture = new SurfaceTexture(1); // Warn: texture not released\n"
                + "                                 ~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/SurfaceTextureTest.java:25: Warning: This SurfaceTexture should be freed up after use with #release() [Recycle]\n"
                + "        SurfaceTexture texture = new SurfaceTexture(1); // Warn: texture not released\n"
                + "                                 ~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/SurfaceTextureTest.java:32: Warning: This Surface should be freed up after use with #release() [Recycle]\n"
                + "        Surface surface = new Surface(texture); // Warn: surface not released\n"
                + "                          ~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 3 warnings\n",

            lintProject(
                    classpath(),
                    manifest().minSdk(4),
                    projectProperties().compileSdk(19),
                    java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import android.graphics.SurfaceTexture;\n"
                            + "import android.view.Surface;\n"
                            + "\n"
                            + "public class SurfaceTextureTest {\n"
                            + "    public void test1() {\n"
                            + "        SurfaceTexture texture = new SurfaceTexture(1); // OK: released\n"
                            + "        texture.release();\n"
                            + "    }\n"
                            + "\n"
                            + "    public void test2() {\n"
                            + "        SurfaceTexture texture = new SurfaceTexture(1); // OK: not sure what the method does\n"
                            + "        unknown(texture);\n"
                            + "    }\n"
                            + "\n"
                            + "    public void test3() {\n"
                            + "        SurfaceTexture texture = new SurfaceTexture(1); // Warn: texture not released\n"
                            + "    }\n"
                            + "\n"
                            + "    private void unknown(SurfaceTexture texture) {\n"
                            + "    }\n"
                            + "\n"
                            + "    public void test4() {\n"
                            + "        SurfaceTexture texture = new SurfaceTexture(1); // Warn: texture not released\n"
                            + "        Surface surface = new Surface(texture);\n"
                            + "        surface.release();\n"
                            + "    }\n"
                            + "\n"
                            + "    public void test5() {\n"
                            + "        SurfaceTexture texture = new SurfaceTexture(1);\n"
                            + "        Surface surface = new Surface(texture); // Warn: surface not released\n"
                            + "        texture.release();\n"
                            + "    }\n"
                            + "}\n")
            ));
    }

    public void testContentProviderClient() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/ContentProviderClientTest.java:8: Warning: This ContentProviderClient should be freed up after use with #release() [Recycle]\n"
                + "        ContentProviderClient client = resolver.acquireContentProviderClient(\"test\"); // Warn\n"
                + "                                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",

                lintProject(
                        classpath(),
                        manifest().minSdk(4),
                        projectProperties().compileSdk(19),
                        java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import android.content.ContentProviderClient;\n"
                            + "import android.content.ContentResolver;\n"
                            + "\n"
                            + "public class ContentProviderClientTest {\n"
                            + "    public void error1(ContentResolver resolver) {\n"
                            + "        ContentProviderClient client = resolver.acquireContentProviderClient(\"test\"); // Warn\n"
                            + "    }\n"
                            + "\n"
                            + "    public void ok1(ContentResolver resolver) {\n"
                            + "        ContentProviderClient client = resolver.acquireContentProviderClient(\"test\"); // OK\n"
                            + "        client.release();\n"
                            + "    }\n"
                            + "\n"
                            + "    public void ok2(ContentResolver resolver) {\n"
                            + "        ContentProviderClient client = resolver.acquireContentProviderClient(\"test\"); // OK\n"
                            + "        unknown(client);\n"
                            + "    }\n"
                            + "\n"
                            + "    public ContentProviderClient ok3(ContentResolver resolver) {\n"
                            + "        ContentProviderClient client = resolver.acquireContentProviderClient(\"test\"); // OK\n"
                            + "        return client;\n"
                            + "    }\n"
                            + "\n"
                            + "    private void unknown(ContentProviderClient client) {\n"
                            + "    }\n"
                            + "}\n")
                ));
    }

    public void testDatabaseCursor() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/CursorTest.java:14: Warning: This Cursor should be freed up after use with #close() [Recycle]\n"
                + "        Cursor cursor = db.query(\"TABLE_TRIPS\",\n"
                + "                           ~~~~~\n"
                + "src/test/pkg/CursorTest.java:23: Warning: This Cursor should be freed up after use with #close() [Recycle]\n"
                + "        Cursor cursor = db.query(\"TABLE_TRIPS\",\n"
                + "                           ~~~~~\n"
                + "src/test/pkg/CursorTest.java:74: Warning: This Cursor should be freed up after use with #close() [Recycle]\n"
                + "        Cursor query = provider.query(uri, null, null, null, null);\n"
                + "                                ~~~~~\n"
                + "src/test/pkg/CursorTest.java:75: Warning: This Cursor should be freed up after use with #close() [Recycle]\n"
                + "        Cursor query2 = resolver.query(uri, null, null, null, null);\n"
                + "                                 ~~~~~\n"
                + "src/test/pkg/CursorTest.java:76: Warning: This Cursor should be freed up after use with #close() [Recycle]\n"
                + "        Cursor query3 = client.query(uri, null, null, null, null);\n"
                + "                               ~~~~~\n"
                + "0 errors, 5 warnings\n",

                lintProject(
                        classpath(),
                        manifest().minSdk(4),
                        projectProperties().compileSdk(19),
                        java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import android.content.ContentProvider;\n"
                            + "import android.content.ContentProviderClient;\n"
                            + "import android.content.ContentResolver;\n"
                            + "import android.database.Cursor;\n"
                            + "import android.database.sqlite.SQLiteDatabase;\n"
                            + "import android.net.Uri;\n"
                            + "import android.os.RemoteException;\n"
                            + "\n"
                            + "@SuppressWarnings(\"UnusedDeclaration\")\n"
                            + "public class CursorTest {\n"
                            + "    public void error1(SQLiteDatabase db, long route_id) {\n"
                            + "        Cursor cursor = db.query(\"TABLE_TRIPS\",\n"
                            + "                new String[]{\"KEY_TRIP_ID\"},\n"
                            + "                \"ROUTE_ID=?\",\n"
                            + "                new String[]{Long.toString(route_id)},\n"
                            + "                null, null, null);\n"
                            + "    }\n"
                            + "\n"
                            + "    public int error2(SQLiteDatabase db, long route_id, String table, String whereClause, String id) {\n"
                            + "        int total_deletions = 0;\n"
                            + "        Cursor cursor = db.query(\"TABLE_TRIPS\",\n"
                            + "                new String[]{\"KEY_TRIP_ID\"},\n"
                            + "                \"ROUTE_ID=?\",\n"
                            + "                new String[]{Long.toString(route_id)},\n"
                            + "                null, null, null);\n"
                            + "\n"
                            + "        while (cursor.moveToNext()) {\n"
                            + "            total_deletions += db.delete(table, whereClause + \"=?\",\n"
                            + "                    new String[]{Long.toString(cursor.getLong(0))});\n"
                            + "        }\n"
                            + "\n"
                            + "        // Not closed!\n"
                            + "        //cursor.close();\n"
                            + "\n"
                            + "        total_deletions += db.delete(table, id + \"=?\", new String[]{Long.toString(route_id)});\n"
                            + "\n"
                            + "        return total_deletions;\n"
                            + "    }\n"
                            + "\n"
                            + "    public int ok(SQLiteDatabase db, long route_id, String table, String whereClause, String id) {\n"
                            + "        int total_deletions = 0;\n"
                            + "        Cursor cursor = db.query(\"TABLE_TRIPS\",\n"
                            + "                new String[]{\n"
                            + "                        \"KEY_TRIP_ID\"},\n"
                            + "                \"ROUTE_ID\" + \"=?\",\n"
                            + "                new String[]{Long.toString(route_id)},\n"
                            + "                null, null, null);\n"
                            + "\n"
                            + "        while (cursor.moveToNext()) {\n"
                            + "            total_deletions += db.delete(table, whereClause + \"=?\",\n"
                            + "                    new String[]{Long.toString(cursor.getLong(0))});\n"
                            + "        }\n"
                            + "        cursor.close();\n"
                            + "\n"
                            + "        return total_deletions;\n"
                            + "    }\n"
                            + "\n"
                            + "    public Cursor getCursor(SQLiteDatabase db) {\n"
                            + "        @SuppressWarnings(\"UnnecessaryLocalVariable\")\n"
                            + "        Cursor cursor = db.query(\"TABLE_TRIPS\",\n"
                            + "                new String[]{\n"
                            + "                        \"KEY_TRIP_ID\"},\n"
                            + "                \"ROUTE_ID\" + \"=?\",\n"
                            + "                new String[]{Long.toString(5)},\n"
                            + "                null, null, null);\n"
                            + "\n"
                            + "        return cursor;\n"
                            + "    }\n"
                            + "\n"
                            + "    void testProviderQueries(Uri uri, ContentProvider provider, ContentResolver resolver,\n"
                            + "                             ContentProviderClient client) throws RemoteException {\n"
                            + "        Cursor query = provider.query(uri, null, null, null, null);\n"
                            + "        Cursor query2 = resolver.query(uri, null, null, null, null);\n"
                            + "        Cursor query3 = client.query(uri, null, null, null, null);\n"
                            + "    }\n"
                            + "\n"
                            + "    void testProviderQueriesOk(Uri uri, ContentProvider provider, ContentResolver resolver,\n"
                            + "                               ContentProviderClient client) throws RemoteException {\n"
                            + "        Cursor query = provider.query(uri, null, null, null, null);\n"
                            + "        Cursor query2 = resolver.query(uri, null, null, null, null);\n"
                            + "        Cursor query3 = client.query(uri, null, null, null, null);\n"
                            + "        query.close();\n"
                            + "        query2.close();\n"
                            + "        query3.close();\n"
                            + "    }\n"
                            + "}\n")
                ));
    }

    public void testDatabaseCursorReassignment() throws Exception {
        //noinspection ClassNameDiffersFromFileName,SpellCheckingInspection
        assertEquals("No warnings.",
                lintProject(java("src/test/pkg/CursorTest.java", ""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.database.Cursor;\n"
                        + "import android.database.sqlite.SQLiteException;\n"
                        + "import android.net.Uri;\n"
                        + "\n"
                        + "public class CursorTest extends Activity {\n"
                        + "    public void testSimple() {\n"
                        + "        Cursor cursor;\n"
                        + "        try {\n"
                        + "            cursor = getContentResolver().query(Uri.parse(\"blahblah\"),\n"
                        + "                    new String[]{\"_id\", \"display_name\"}, null, null, null);\n"
                        + "        } catch (SQLiteException e) {\n"
                        + "            // Fallback\n"
                        + "            cursor = getContentResolver().query(Uri.parse(\"blahblah\"),\n"
                        + "                    new String[]{\"_id2\", \"display_name\"}, null, null, null);\n"
                        + "        }\n"
                        + "        assert cursor != null;\n"
                        + "        cursor.close();\n"
                        + "    }\n"
                        + "}\n")));
    }

    // Shared preference tests

    public void test() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/SharedPrefsTest.java:54: Warning: SharedPreferences.edit() without a corresponding commit() or apply() call [CommitPrefEdits]\n"
                + "        SharedPreferences.Editor editor = preferences.edit();\n"
                + "                                          ~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/SharedPrefsTest.java:62: Warning: SharedPreferences.edit() without a corresponding commit() or apply() call [CommitPrefEdits]\n"
                + "        SharedPreferences.Editor editor = preferences.edit();\n"
                + "                                          ~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

                lintProject(java(""
                            + "package test.pkg;\n"
                            + "import android.app.Activity;\n"
                            + "import android.content.Context;\n"
                            + "import android.os.Bundle;\n"
                            + "import android.widget.Toast;\n"
                            + "import android.content.SharedPreferences; import android.content.SharedPreferences.Editor;\n"
                            + "import android.preference.PreferenceManager;\n"
                            + "public class SharedPrefsTest extends Activity {\n"
                            + "    // OK 1\n"
                            + "    public void onCreate1(Bundle savedInstanceState) {\n"
                            + "        super.onCreate(savedInstanceState);\n"
                            + "        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);\n"
                            + "        SharedPreferences.Editor editor = preferences.edit();\n"
                            + "        editor.putString(\"foo\", \"bar\");\n"
                            + "        editor.putInt(\"bar\", 42);\n"
                            + "        editor.commit();\n"
                            + "    }\n"
                            + "\n"
                            + "    // OK 2\n"
                            + "    public void onCreate2(Bundle savedInstanceState, boolean apply) {\n"
                            + "        super.onCreate(savedInstanceState);\n"
                            + "        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);\n"
                            + "        SharedPreferences.Editor editor = preferences.edit();\n"
                            + "        editor.putString(\"foo\", \"bar\");\n"
                            + "        editor.putInt(\"bar\", 42);\n"
                            + "        if (apply) {\n"
                            + "            editor.apply();\n"
                            + "        }\n"
                            + "    }\n"
                            + "\n"
                            + "    // OK 3\n"
                            + "    public boolean test1(Bundle savedInstanceState) {\n"
                            + "        super.onCreate(savedInstanceState);\n"
                            + "        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);\n"
                            + "        SharedPreferences.Editor editor = preferences.edit();\n"
                            + "        editor.putString(\"foo\", \"bar\");\n"
                            + "        editor.putInt(\"bar\", 42);\n"
                            + "        editor.apply(); return true;\n"
                            + "    }\n"
                            + "\n"
                            + "    // Not a bug\n"
                            + "    public void test(Foo foo) {\n"
                            + "        Bar bar1 = foo.edit();\n"
                            + "        Bar bar2 = Foo.edit();\n"
                            + "        Bar bar3 = edit();\n"
                            + "\n"
                            + "\n"
                            + "    }\n"
                            + "\n"
                            + "    // Bug\n"
                            + "    public void bug1(Bundle savedInstanceState) {\n"
                            + "        super.onCreate(savedInstanceState);\n"
                            + "        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);\n"
                            + "        SharedPreferences.Editor editor = preferences.edit();\n"
                            + "        editor.putString(\"foo\", \"bar\");\n"
                            + "        editor.putInt(\"bar\", 42);\n"
                            + "    }\n"
                            + "\n"
                            + "    // Constructor test\n"
                            + "    public SharedPrefsTest(Context context) {\n"
                            + "        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);\n"
                            + "        SharedPreferences.Editor editor = preferences.edit();\n"
                            + "        editor.putString(\"foo\", \"bar\");\n"
                            + "    }\n"
                            + "\n"
                            + "    private Bar edit() {\n"
                            + "        return null;\n"
                            + "    }\n"
                            + "\n"
                            + "    private static class Foo {\n"
                            + "        static Bar edit() { return null; }\n"
                            + "    }\n"
                            + "\n"
                            + "    private static class Bar {\n"
                            + "\n"
                            + "    }\n"
                            + " }\n"
                            + "\n")));
    }

    public void test2() throws Exception {
        // Regression test 1 for http://code.google.com/p/android/issues/detail?id=34322
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/SharedPrefsTest2.java:13: Warning: SharedPreferences.edit() without a corresponding commit() or apply() call [CommitPrefEdits]\n"
                + "        SharedPreferences.Editor editor = preferences.edit();\n"
                + "                                          ~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/SharedPrefsTest2.java:17: Warning: SharedPreferences.edit() without a corresponding commit() or apply() call [CommitPrefEdits]\n"
                + "        Editor editor = preferences.edit();\n"
                + "                        ~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

                lintProject(java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import android.annotation.SuppressLint;\n"
                            + "import android.app.Activity;\n"
                            + "import android.content.SharedPreferences;\n"
                            + "import android.content.SharedPreferences.Editor;\n"
                            + "import android.os.Bundle;\n"
                            + "import android.preference.PreferenceManager;\n"
                            + "\n"
                            + "@SuppressWarnings(\"unused\")\n"
                            + "public class SharedPrefsTest2 extends Activity {\n"
                            + "    public void test1(SharedPreferences preferences) {\n"
                            + "        SharedPreferences.Editor editor = preferences.edit();\n"
                            + "    }\n"
                            + "\n"
                            + "    public void test2(SharedPreferences preferences) {\n"
                            + "        Editor editor = preferences.edit();\n"
                            + "    }\n"
                            + "}\n")));
    }

    public void test3() throws Exception {
        // Regression test 2 for http://code.google.com/p/android/issues/detail?id=34322
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/SharedPrefsTest3.java:13: Warning: SharedPreferences.edit() without a corresponding commit() or apply() call [CommitPrefEdits]\n"
                + "        Editor editor = preferences.edit();\n"
                + "                        ~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",

                lintProject(java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import android.annotation.SuppressLint;\n"
                            + "import android.app.Activity;\n"
                            + "import android.content.SharedPreferences;\n"
                            + "import android.content.SharedPreferences.*;\n"
                            + "import android.os.Bundle;\n"
                            + "import android.preference.PreferenceManager;\n"
                            + "\n"
                            + "@SuppressWarnings(\"unused\")\n"
                            + "public class SharedPrefsTest3 extends Activity {\n"
                            + "    public void test(SharedPreferences preferences) {\n"
                            + "        Editor editor = preferences.edit();\n"
                            + "    }\n"
                            + "}\n")));
    }

    public void test4() throws Exception {
        // Regression test 3 for http://code.google.com/p/android/issues/detail?id=34322
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/SharedPrefsTest4.java:13: Warning: SharedPreferences.edit() without a corresponding commit() or apply() call [CommitPrefEdits]\n"
                + "        Editor editor = preferences.edit();\n"
                + "                        ~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",

                lintProject(java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import android.annotation.SuppressLint;\n"
                            + "import android.app.Activity;\n"
                            + "import android.content.SharedPreferences;\n"
                            + "import android.content.SharedPreferences.Editor;\n"
                            + "import android.os.Bundle;\n"
                            + "import android.preference.PreferenceManager;\n"
                            + "\n"
                            + "@SuppressWarnings(\"unused\")\n"
                            + "public class SharedPrefsTest4 extends Activity {\n"
                            + "    public void test(SharedPreferences preferences) {\n"
                            + "        Editor editor = preferences.edit();\n"
                            + "    }\n"
                            + "}\n")));
    }

    public void test5() throws Exception {
        // Check fields too: http://code.google.com/p/android/issues/detail?id=39134
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/SharedPrefsTest5.java:16: Warning: SharedPreferences.edit() without a corresponding commit() or apply() call [CommitPrefEdits]\n"
                + "        mPreferences.edit().putString(PREF_FOO, \"bar\");\n"
                + "        ~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/SharedPrefsTest5.java:17: Warning: SharedPreferences.edit() without a corresponding commit() or apply() call [CommitPrefEdits]\n"
                + "        mPreferences.edit().remove(PREF_BAZ).remove(PREF_FOO);\n"
                + "        ~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/SharedPrefsTest5.java:26: Warning: SharedPreferences.edit() without a corresponding commit() or apply() call [CommitPrefEdits]\n"
                + "        preferences.edit().putString(PREF_FOO, \"bar\");\n"
                + "        ~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/SharedPrefsTest5.java:27: Warning: SharedPreferences.edit() without a corresponding commit() or apply() call [CommitPrefEdits]\n"
                + "        preferences.edit().remove(PREF_BAZ).remove(PREF_FOO);\n"
                + "        ~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/SharedPrefsTest5.java:32: Warning: SharedPreferences.edit() without a corresponding commit() or apply() call [CommitPrefEdits]\n"
                + "        preferences.edit().putString(PREF_FOO, \"bar\");\n"
                + "        ~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/SharedPrefsTest5.java:33: Warning: SharedPreferences.edit() without a corresponding commit() or apply() call [CommitPrefEdits]\n"
                + "        preferences.edit().remove(PREF_BAZ).remove(PREF_FOO);\n"
                + "        ~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/SharedPrefsTest5.java:38: Warning: SharedPreferences.edit() without a corresponding commit() or apply() call [CommitPrefEdits]\n"
                + "        Editor editor = preferences.edit().putString(PREF_FOO, \"bar\");\n"
                + "                        ~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 7 warnings\n",

                lintProject(java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import android.content.Context;\n"
                            + "import android.content.SharedPreferences;\n"
                            + "import android.content.SharedPreferences.Editor;\n"
                            + "import android.preference.PreferenceManager;\n"
                            + "\n"
                            + "@SuppressWarnings(\"unused\")\n"
                            + "class SharedPrefsTest5 {\n"
                            + "    SharedPreferences mPreferences;\n"
                            + "    private static final String PREF_FOO = \"foo\";\n"
                            + "    private static final String PREF_BAZ = \"bar\";\n"
                            + "\n"
                            + "    private void wrong() {\n"
                            + "        // Field reference to preferences\n"
                            + "        mPreferences.edit().putString(PREF_FOO, \"bar\");\n"
                            + "        mPreferences.edit().remove(PREF_BAZ).remove(PREF_FOO);\n"
                            + "    }\n"
                            + "\n"
                            + "    private void ok() {\n"
                            + "        mPreferences.edit().putString(PREF_FOO, \"bar\").commit();\n"
                            + "        mPreferences.edit().remove(PREF_BAZ).remove(PREF_FOO).commit();\n"
                            + "    }\n"
                            + "\n"
                            + "    private void wrong2(SharedPreferences preferences) {\n"
                            + "        preferences.edit().putString(PREF_FOO, \"bar\");\n"
                            + "        preferences.edit().remove(PREF_BAZ).remove(PREF_FOO);\n"
                            + "    }\n"
                            + "\n"
                            + "    private void wrong3(Context context) {\n"
                            + "        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);\n"
                            + "        preferences.edit().putString(PREF_FOO, \"bar\");\n"
                            + "        preferences.edit().remove(PREF_BAZ).remove(PREF_FOO);\n"
                            + "    }\n"
                            + "\n"
                            + "    private void wrong4(Context context) {\n"
                            + "        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);\n"
                            + "        Editor editor = preferences.edit().putString(PREF_FOO, \"bar\");\n"
                            + "    }\n"
                            + "\n"
                            + "    private void ok2(Context context) {\n"
                            + "        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);\n"
                            + "        preferences.edit().putString(PREF_FOO, \"bar\").commit();\n"
                            + "    }\n"
                            + "\n"
                            + "    private final SharedPreferences mPrefs = null;\n"
                            + "\n"
                            + "    public void ok3() {\n"
                            + "        final SharedPreferences.Editor editor = mPrefs.edit().putBoolean(\n"
                            + "                PREF_FOO, true);\n"
                            + "        editor.putString(PREF_BAZ, \"\");\n"
                            + "        editor.apply();\n"
                            + "    }\n"
                            + "}\n")));
    }

    public void test6() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=68692
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/SharedPrefsTest7.java:13: Warning: SharedPreferences.edit() without a corresponding commit() or apply() call [CommitPrefEdits]\n"
                + "        settings.edit().putString(MY_PREF_KEY, myPrefValue);\n"
                + "        ~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",

                lintProject(java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import android.content.SharedPreferences;\n"
                            + "\n"
                            + "public class SharedPrefsTest7 {\n"
                            + "    private static final String PREF_NAME = \"MyPrefName\";\n"
                            + "    private static final String MY_PREF_KEY = \"MyKey\";\n"
                            + "    SharedPreferences getSharedPreferences(String key, int deflt) {\n"
                            + "        return null;\n"
                            + "    }\n"
                            + "    public void test(String myPrefValue) {\n"
                            + "        SharedPreferences settings = getSharedPreferences(PREF_NAME, 0);\n"
                            + "        settings.edit().putString(MY_PREF_KEY, myPrefValue);\n"
                            + "    }\n"
                            + "}\n")));
    }

    public void test7() throws Exception {
        assertEquals("No warnings.", // minSdk < 9: no warnings

                lintProject(mSharedPrefsTest8));
    }

    public void test8() throws Exception {
        String expected = ""
                + "src/test/pkg/SharedPrefsTest8.java:11: Warning: Consider using apply() instead; commit writes its data to persistent storage immediately, whereas apply will handle it in the background [ApplySharedPref]\n"
                + "        editor.commit();\n"
                + "        ~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";
        lint().files(manifest().minSdk(11), mSharedPrefsTest8).run().expect(expected)
                .expectFixDiffs(""
                        + "Fix for src/test/pkg/SharedPrefsTest8.java line 10: Replace commit() with apply():\n"
                        + "@@ -11 +11\n"
                        + "-         editor.commit();\n"
                        + "+         editor.apply();\n");
    }

    public void testChainedCalls() throws Exception {
        assertEquals(""
                + "src/test/pkg/Chained.java:24: Warning: SharedPreferences.edit() without a corresponding commit() or apply() call [CommitPrefEdits]\n"
                + "        PreferenceManager\n"
                + "        ^\n"
                + "0 errors, 1 warnings\n",
                lintProject(java("src/test/pkg/Chained.java", ""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.Context;\n"
                        + "import android.preference.PreferenceManager;\n"
                        + "\n"
                        + "public class Chained {\n"
                        + "    private static void falsePositive(Context context) {\n"
                        + "        PreferenceManager\n"
                        + "                .getDefaultSharedPreferences(context)\n"
                        + "                .edit()\n"
                        + "                .putString(\"wat\", \"wat\")\n"
                        + "                .commit();\n"
                        + "    }\n"
                        + "\n"
                        + "    private static void falsePositive2(Context context) {\n"
                        + "        boolean var = PreferenceManager\n"
                        + "                .getDefaultSharedPreferences(context)\n"
                        + "                .edit()\n"
                        + "                .putString(\"wat\", \"wat\")\n"
                        + "                .commit();\n"
                        + "    }\n"
                        + "\n"
                        + "    private static void truePositive(Context context) {\n"
                        + "        PreferenceManager\n"
                        + "                .getDefaultSharedPreferences(context)\n"
                        + "                .edit()\n"
                        + "                .putString(\"wat\", \"wat\");\n"
                        + "    }\n"
                        + "}\n")));
    }

    @SuppressWarnings("ALL") // sample code with warnings
    public void testCommitDetector() throws Exception {
        assertEquals("No warnings.",
                lintProject(
                        java("src/test/pkg/CommitTest.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.app.Activity;\n"
                                + "import android.app.FragmentManager;\n"
                                + "import android.app.FragmentTransaction;\n"
                                + "import android.content.Context;\n"
                                + "\n"
                                + "public class CommitTest {\n"
                                + "    private Context mActivity;\n"
                                + "    public void selectTab1() {\n"
                                + "        FragmentTransaction trans = null;\n"
                                + "        if (mActivity instanceof Activity) {\n"
                                + "            trans = ((Activity)mActivity).getFragmentManager().beginTransaction()\n"
                                + "                    .disallowAddToBackStack();\n"
                                + "        }\n"
                                + "\n"
                                + "        if (trans != null && !trans.isEmpty()) {\n"
                                + "            trans.commit();\n"
                                + "        }\n"
                                + "    }\n"
                                + "\n"
                                + "    public void select(FragmentManager fragmentManager) {\n"
                                + "        FragmentTransaction trans = fragmentManager.beginTransaction().disallowAddToBackStack();\n"
                                + "        trans.commit();\n"
                                + "    }"
                                + "}")));
    }

    @SuppressWarnings("ALL") // sample code with warnings
    public void testCommitDetectorOnParameters() throws Exception {
        // Handle transactions assigned to parameters (this used to not work)
        assertEquals("No warnings.",
                lintProject(
                        java("src/test/pkg/CommitTest2.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.app.FragmentManager;\n"
                                + "import android.app.FragmentTransaction;\n"
                                + "\n"
                                + "@SuppressWarnings(\"unused\")\n"
                                + "public class CommitTest2 {\n"
                                + "    private void navigateToFragment(FragmentTransaction transaction,\n"
                                + "                                    FragmentManager supportFragmentManager) {\n"
                                + "        if (transaction == null) {\n"
                                + "            transaction = supportFragmentManager.beginTransaction();\n"
                                + "        }\n"
                                + "\n"
                                + "        transaction.commit();\n"
                                + "    }\n"
                                + "}")));
    }

    @SuppressWarnings("ALL") // sample code with warnings
    public void testReturn() throws Exception {
        // If you return the object to be cleaned up, it doesn'st have to be cleaned up (caller
        // may do that)
        assertEquals("No warnings.",
                lintProject(
                        java("src/test/pkg/SharedPrefsTest.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.content.Context;\n"
                                + "import android.content.SharedPreferences;\n"
                                + "import android.preference.PreferenceManager;\n"
                                + "\n"
                                + "@SuppressWarnings(\"unused\")\n"
                                + "public abstract class SharedPrefsTest extends Context {\n"
                                + "    private SharedPreferences.Editor getEditor() {\n"
                                + "        return getPreferences().edit();\n"
                                + "    }\n"
                                + "\n"
                                + "    private boolean editAndCommit() {\n"
                                + "        return getPreferences().edit().commit();\n"
                                + "    }\n"
                                + "\n"
                                + "    private SharedPreferences getPreferences() {\n"
                                + "        return PreferenceManager.getDefaultSharedPreferences(this);\n"
                                + "    }\n"
                                + "}")));
    }

    @SuppressWarnings("ALL") // sample code with warnings
    public void testCommitNow() throws Exception {
        assertEquals("No warnings.",
                lintProject(
                        java("src/test/pkg/CommitTest.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.app.FragmentManager;\n"
                                + "import android.app.FragmentTransaction;\n"
                                + "\n"
                                + "public class CommitTest {\n"
                                + "    public void select(FragmentManager fragmentManager) {\n"
                                + "        FragmentTransaction trans = fragmentManager.beginTransaction().disallowAddToBackStack();\n"
                                + "        trans.commitNow();\n"
                                + "    }"
                                + "}")));
    }

    @SuppressWarnings("MethodMayBeStatic")
    public void testAutoCloseable() throws Exception {
        // Regression test for
        //   https://code.google.com/p/android/issues/detail?id=214086
        //
        // Queries assigned to try/catch resource variables are automatically
        // closed.
        assertEquals("No warnings.",
                lintProject(
                        java("src/test/pkg/TryWithResources.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.content.ContentResolver;\n"
                                + "import android.database.Cursor;\n"
                                + "import android.net.Uri;\n"
                                + "import android.os.Build;\n"
                                + "\n"
                                + "public class TryWithResources {\n"
                                + "    public void test(ContentResolver resolver, Uri uri, String[] projection) {\n"
                                + "        try (Cursor cursor = resolver.query(uri, projection, null, null, null)) {\n"
                                + "            if (cursor != null) {\n"
                                + "                //noinspection StatementWithEmptyBody\n"
                                + "                while (cursor.moveToNext()) {\n"
                                + "                    // ..\n"
                                + "                }\n"
                                + "            }\n"
                                + "        }\n"
                                + "    }\n"
                                + "}\n"
                        )
                ));
    }

    public void testApplyOnPutMethod() throws Exception {
        // Regression test for
        //    https://code.google.com/p/android/issues/detail?id=214196
        //
        // Ensure that if you call commit/apply on a put* call
        // (not the edit field itself, but put passes it through)
        // we correctly consider the editor operation finished.
        assertEquals("No warnings.",
                lintProject(
                        java("src/test/pkg/CommitPrefTest.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.content.Context;\n"
                                + "import android.content.SharedPreferences;\n"
                                + "import android.preference.PreferenceManager;\n"
                                + "\n"
                                + "@SuppressWarnings(\"unused\")\n"
                                + "public abstract class CommitPrefTest extends Context {\n"
                                + "    public void test() {\n"
                                + "        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();\n"
                                + "        edit.putInt(\"foo\", 1).apply();\n"
                                + "    }\n"
                                + "}\n")
                ));
    }

    @SuppressWarnings("ALL") // sample code with warnings
    public void testCommitNowAllowingStateLoss() throws Exception {
        // Handle transactions assigned to parameters (this used to not work)
        assertEquals("No warnings.",
                lintProject(
                        java("src/test/pkg/CommitTest2.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.app.FragmentManager;\n"
                                + "import android.app.FragmentTransaction;\n"
                                + "\n"
                                + "@SuppressWarnings(\"unused\")\n"
                                + "public class CommitTest2 {\n"
                                + "    private void navigateToFragment(FragmentManager supportFragmentManager) {\n"
                                + "        FragmentTransaction transaction = supportFragmentManager.beginTransaction();\n"
                                + "        transaction.commitNowAllowingStateLoss();\n"
                                + "    }\n"
                                + "}")));
    }

    public void testFields() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=224435
        //noinspection all // Sample code
        assertEquals("No warnings.",
                lintProject(
                        java(""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.app.Service;\n"
                                + "import android.content.SharedPreferences;\n"
                                + "import android.preference.PreferenceManager;\n"
                                + "\n"
                                + "public abstract class CommitFromField extends Service {\n"
                                + "    private SharedPreferences prefs;\n"
                                + "    @SuppressWarnings(\"FieldCanBeLocal\")\n"
                                + "    private SharedPreferences.Editor editor;\n"
                                + "\n"
                                + "    @Override\n"
                                + "    public void onCreate() {\n"
                                + "        prefs = PreferenceManager.getDefaultSharedPreferences(this);\n"
                                + "    }\n"
                                + "\n"
                                + "    private void engine() {\n"
                                + "        editor = prefs.edit();\n"
                                + "        editor.apply();\n"
                                + "    }\n"
                                + "}\n")
                ));
    }

    public void testUnrelatedSharedPrefEdit() {
        // Regression test for https://code.google.com/p/android/issues/detail?id=234868
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.SharedPreferences;\n"
                        + "\n"
                        + "public abstract class PrefTest {\n"
                        + "    public static void something(SomePref pref) {\n"
                        + "        pref.edit(1, 2, 3);\n"
                        + "    }\n"
                        + "\n"
                        + "    public interface SomePref extends SharedPreferences {\n"
                        + "        void edit(Object...args);\n"
                        + "    }\n"
                        + "}"))
                .issues(SHARED_PREF)
                .run()
                .expectClean();
    }

    public void testCommitVariable() {
        // Regression test for https://code.google.com/p/android/issues/detail?id=237776
        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.app.Fragment;\n"
                        + "\n"
                        + "public class CommitTest extends Activity {\n"
                        + "    public void test() {\n"
                        + "        final int id = getFragmentManager().beginTransaction()\n"
                        + "                .add(new Fragment(), null)\n"
                        + "                .addToBackStack(null)\n"
                        + "                .commit();\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expectClean();
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mDialogFragment = java(""
            + "package android.support.v4.app;\n"
            + "\n"
            + "/** Stub to make unit tests able to resolve types without having a real dependency\n"
            + " * on the appcompat library */\n"
            + "public abstract class DialogFragment extends Fragment {\n"
            + "    public void show(FragmentManager manager, String tag) { }\n"
            + "    public int show(FragmentTransaction transaction, String tag) { return 0; }\n"
            + "    public void dismiss() { }\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mFragment = java(""
            + "package android.support.v4.app;\n"
            + "\n"
            + "/** Stub to make unit tests able to resolve types without having a real dependency\n"
            + " * on the appcompat library */\n"
            + "public class Fragment {\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mFragmentManager = java(""
            + "package android.support.v4.app;\n"
            + "\n"
            + "/** Stub to make unit tests able to resolve types without having a real dependency\n"
            + " * on the appcompat library */\n"
            + "public abstract class FragmentManager {\n"
            + "    public abstract FragmentTransaction beginTransaction();\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mFragmentTransaction = java(""
            + "package android.support.v4.app;\n"
            + "\n"
            + "/** Stub to make unit tests able to resolve types without having a real dependency\n"
            + " * on the appcompat library */\n"
            + "public abstract class FragmentTransaction {\n"
            + "    public abstract int commit();\n"
            + "    public abstract int commitAllowingStateLoss();\n"
            + "    public abstract FragmentTransaction show(Fragment fragment);\n"
            + "    public abstract FragmentTransaction hide(Fragment fragment);\n"
            + "    public abstract FragmentTransaction attach(Fragment fragment);\n"
            + "    public abstract FragmentTransaction detach(Fragment fragment);\n"
            + "    public abstract FragmentTransaction add(int containerViewId, Fragment fragment);\n"
            + "    public abstract FragmentTransaction add(Fragment fragment, String tag);\n"
            + "    public abstract FragmentTransaction addToBackStack(String name);\n"
            + "    public abstract FragmentTransaction disallowAddToBackStack();\n"
            + "    public abstract FragmentTransaction setBreadCrumbShortTitle(int res);\n"
            + "    public abstract FragmentTransaction setCustomAnimations(int enter, int exit);\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mSharedPrefsTest8 = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.app.Activity;\n"
            + "import android.content.SharedPreferences;\n"
            + "import android.preference.PreferenceManager;\n"
            + "\n"
            + "public class SharedPrefsTest8 extends Activity {\n"
            + "    public void commitWarning1() {\n"
            + "        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);\n"
            + "        SharedPreferences.Editor editor = preferences.edit();\n"
            + "        editor.commit();\n"
            + "    }\n"
            + "\n"
            + "    public void commitWarning2() {\n"
            + "        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);\n"
            + "        SharedPreferences.Editor editor = preferences.edit();\n"
            + "        boolean b = editor.commit(); // OK: reading return value\n"
            + "    }\n"
            + "    public void commitWarning3() {\n"
            + "        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);\n"
            + "        SharedPreferences.Editor editor = preferences.edit();\n"
            + "        boolean c;\n"
            + "        c = editor.commit(); // OK: reading return value\n"
            + "    }\n"
            + "\n"
            + "    public void commitWarning4() {\n"
            + "        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);\n"
            + "        SharedPreferences.Editor editor = preferences.edit();\n"
            + "        if (editor.commit()) { // OK: reading return value\n"
            + "            //noinspection UnnecessaryReturnStatement\n"
            + "            return;\n"
            + "        }\n"
            + "    }\n"
            + "\n"
            + "    public void commitWarning5() {\n"
            + "        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);\n"
            + "        SharedPreferences.Editor editor = preferences.edit();\n"
            + "        boolean c = false;\n"
            + "        c |= editor.commit(); // OK: reading return value\n"
            + "    }\n"
            + "\n"
            + "    public void commitWarning6() {\n"
            + "        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);\n"
            + "        SharedPreferences.Editor editor = preferences.edit();\n"
            + "        foo(editor.commit()); // OK: reading return value\n"
            + "    }\n"
            + "\n"
            + "    public void foo(boolean x) {\n"
            + "    }\n"
            + "\n"
            + "    public void noWarning() {\n"
            + "        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);\n"
            + "        SharedPreferences.Editor editor = preferences.edit();\n"
            + "        editor.apply();\n"
            + "    }\n"
            + "}\n");
}