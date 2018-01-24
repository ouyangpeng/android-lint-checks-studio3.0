/*
 * Copyright (C) 2017 The Android Open Source Project
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

public class PrivateApiDetectorTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new PrivateApiDetector();
    }

    public void testForNameOnInternalClass() {
        String expected = ""+
                "src/test/pkg/myapplication/ReflectionTest1.java:8: Warning: Accessing internal APIs via reflection is not supported and may not work on all devices or in the future [PrivateApi]\n"+
                "        Class<?> c = Class.forName(\"com.android.internal.widget.LockPatternUtils\"); // ERROR\n"+
                "                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"+
                "src/test/pkg/myapplication/ReflectionTest1.java:9: Warning: Accessing internal APIs via reflection is not supported and may not work on all devices or in the future [PrivateApi]\n"+
                "        int titleContainerId = (Integer) Class.forName(\"com.android.internal.R$id\").getField(\"title_container\").get(null);\n"+
                "                                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"+
                "src/test/pkg/myapplication/ReflectionTest1.java:11: Warning: Accessing internal APIs via reflection is not supported and may not work on all devices or in the future [PrivateApi]\n"+
                "        Class SystemProperties = cl.loadClass(\"android.os.SystemProperties\");\n"+
                "                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"+
                "0 errors, 3 warnings\n";

        //noinspection all // Sample code
        lint().files(
                java("package test.pkg.myapplication;\n"+
                        "\n"+
                        "import android.app.Activity;\n"+
                        "\n"+
                        "public class ReflectionTest1 extends Activity {\n"+
                        "    public void testForName(ClassLoader cl) throws Exception {\n"+
                        "        Class.forName(\"java.lang.String\"); // OK\n"+
                        "        Class<?> c = Class.forName(\"com.android.internal.widget.LockPatternUtils\"); // ERROR\n"+
                        "        int titleContainerId = (Integer) Class.forName(\"com.android.internal.R$id\").getField(\"title_container\").get(null);\n"+
                        "        @SuppressWarnings(\"rawtypes\")\n"+
                        "        Class SystemProperties = cl.loadClass(\"android.os.SystemProperties\");\n"+
                        "    }\n"+
                        "}\n"))
                .run()
                .expect(expected);
    }

    public void testForNameOnSdkClass() {
        //noinspection all // Sample code
        lint().files(
                java(""
                        +"package test.pkg.myapplication;\n"
                        +"\n"
                        +"import android.app.Activity;\n"
                        +"\n"
                        +"public class ReflectionTest1 extends Activity {\n"
                        +"    public void testForName() throws ClassNotFoundException {\n"
                        +"        Class.forName(\"android.view.View\"); // OK\n"
                        +"    }\n"
                        +"}\n"))
                .run()
                .expectClean();
    }

    public void testLoadClass() {
        String expected = ""+
                "src/test/pkg/myapplication/ReflectionTest2.java:9: Warning: Accessing internal APIs via reflection is not supported and may not work on all devices or in the future [PrivateApi]\n"+
                "        classLoader.loadClass(\"com.android.internal.widget.LockPatternUtils\"); // ERROR\n"+
                "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"+
                "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        lint().files(
                java(""
                        +"package test.pkg.myapplication;\n"
                        +"\n"
                        +"import android.app.Activity;\n"
                        +"\n"
                        +"public class ReflectionTest2 extends Activity {\n"
                        +"    public void testLoadClass() throws ClassNotFoundException {\n"
                        +"        ClassLoader classLoader = ClassLoader.getSystemClassLoader();\n"
                        +"        classLoader.loadClass(\"java.lang.String\"); // OK\n"
                        +
                        "        classLoader.loadClass(\"com.android.internal.widget.LockPatternUtils\"); // ERROR\n"
                        +"    }\n"
                        +"}\n"))
                .run()
                .expect(expected);
    }

    public void testGetDeclaredMethod1() {
        String expected = ""+
                "src/test/pkg/myapplication/ReflectionTest3.java:7: Warning: Accessing internal APIs via reflection is not supported and may not work on all devices or in the future [PrivateApi]\n"+
                "        Class<?> c = Class.forName(\"com.android.internal.widget.LockPatternUtils\"); // ERROR\n"+
                "                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"+
                "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        lint().files(
                java(""
                        +"package test.pkg.myapplication;\n"
                        +"\n"
                        +"import android.app.Activity;\n"
                        +"\n"
                        +"public class ReflectionTest3 extends Activity {\n"
                        +
                        "    public void testGetDeclaredMethod1() throws ClassNotFoundException, NoSuchMethodException {\n"
                        +
                        "        Class<?> c = Class.forName(\"com.android.internal.widget.LockPatternUtils\"); // ERROR\n"
                        +"        c.getDeclaredMethod(\"getKeyguardStoredPasswordQuality\");\n"
                        +"    }\n"
                        +"}\n"))
                .run()
                .expect(expected);
    }

    public void testReflectionWithoutClassLoad() {
        String expected = ""+
                "src/test/pkg/myapplication/ReflectionTest4.java:12: Warning: Accessing internal APIs via reflection is not supported and may not work on all devices or in the future [PrivateApi]\n"+
                "        Method m1 = tm.getClass().getDeclaredMethod(\"getITelephony\"); // ERROR\n"+
                "                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"+
                "src/test/pkg/myapplication/ReflectionTest4.java:13: Warning: Accessing internal APIs via reflection is not supported and may not work on all devices or in the future [PrivateApi]\n"+
                "        Method m2 = TelephonyManager.class.getDeclaredMethod(\"getITelephony\"); // ERROR\n"+
                "                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"+
                "0 errors, 2 warnings\n";

        //noinspection all // Sample code
        lint().files(
                java(""
                        +"package test.pkg.myapplication;\n"
                        +"\n"
                        +"import android.app.Activity;\n"
                        +"import android.telephony.TelephonyManager;\n"
                        +"\n"
                        +"import java.lang.reflect.Method;\n"
                        +"\n"
                        +"public class ReflectionTest4 extends Activity {\n"
                        +
                        "    public void testReflectionWithoutClassLoad() throws NoSuchMethodException {\n"
                        +
                        "        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);\n"
                        +"        // Reflection of unsupported method:\n"
                        +
                        "        Method m1 = tm.getClass().getDeclaredMethod(\"getITelephony\"); // ERROR\n"
                        +
                        "        Method m2 = TelephonyManager.class.getDeclaredMethod(\"getITelephony\"); // ERROR\n"
                        +
                        "        // Reflection of supported method: OK (probably conditional for version checks\n"
                        +"        // compiling with old SDK; this one requires API 23)\n"
                        +
                        "        Method m3 = tm.getClass().getDeclaredMethod(\"canChangeDtmfToneLength\"); // OK\n"
                        +"    }\n"
                        +"}\n"))
                .run()
                .expect(expected);
    }

    public void testLoadingClassesViaDexFile() {
        String expected = ""+
                "src/test/pkg/myapplication/ReflectionTest.java:15: Warning: Accessing internal APIs via reflection is not supported and may not work on all devices or in the future [PrivateApi]\n"+
                "        Class LocalePicker = df.loadClass(name, cl);\n"+
                "                             ~~~~~~~~~~~~~~~~~~~~~~\n"+
                "src/test/pkg/myapplication/ReflectionTest.java:16: Warning: Accessing internal APIs via reflection is not supported and may not work on all devices or in the future [PrivateApi]\n"+
                "        Class ActivityManagerNative = Class.forName(\"android.app.ActivityManagerNative\");\n"+
                "                                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"+
                "src/test/pkg/myapplication/ReflectionTest.java:17: Warning: Accessing internal APIs via reflection is not supported and may not work on all devices or in the future [PrivateApi]\n"+
                "        Class IActivityManager = Class.forName(\"android.app.IActivityManager\");\n"+
                "                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"+
                "0 errors, 3 warnings\n";

        //noinspection all // Sample code
        lint().files(
                java(""
                        +"package test.pkg.myapplication;\n"
                        +"\n"
                        +"import android.app.Activity;\n"
                        +"import android.content.res.Configuration;\n"
                        +"\n"
                        +"import java.io.File;\n"
                        +"import java.lang.reflect.Method;\n"
                        +"\n"
                        +"import dalvik.system.DexFile;\n"
                        +"\n"
                        +"public class ReflectionTest extends Activity {\n"
                        +"    public void testReflection(DexFile df) throws Exception {\n"
                        +"        String name = \"com.android.settings.LocalePicker\";\n"
                        +"        ClassLoader cl = getClassLoader();\n"
                        +"        Class LocalePicker = df.loadClass(name, cl);\n"
                        +
                        "        Class ActivityManagerNative = Class.forName(\"android.app.ActivityManagerNative\");\n"
                        +
                        "        Class IActivityManager = Class.forName(\"android.app.IActivityManager\");\n"
                        +
                        "        Method getDefault = ActivityManagerNative.getMethod(\"getDefault\", null);\n"
                        +
                        "        Object am = IActivityManager.cast(getDefault.invoke(ActivityManagerNative, null));\n"
                        +"    }\n"
                        +"}\n"))
                .run()
                .expect(expected);
    }

    public void testCaseFromIssue78420() {
        // Testcase from https://code.google.com/p/android/issues/detail?id=78420
        String expected = ""+
                "src/test/pkg/myapplication/ReflectionTest.java:9: Warning: Accessing internal APIs via reflection is not supported and may not work on all devices or in the future [PrivateApi]\n"+
                "        Class<?> loadedStringsClass = Class.forName(\"com.android.internal.R$styleable\");\n"+
                "                                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"+
                "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        lint().files(
                java(""
                        +"package test.pkg.myapplication;\n"
                        +"\n"
                        +"import android.app.Activity;\n"
                        +"import android.util.Log;\n"
                        +"\n"
                        +"public class ReflectionTest extends Activity {\n"
                        +
                        "    public void test(String TAG) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {\n"
                        +"        Log.e (TAG, \"TestClass 1, String load start\");\n"
                        +
                        "        Class<?> loadedStringsClass = Class.forName(\"com.android.internal.R$styleable\");\n"
                        +
                        "        Log.e (TAG, \"    TestClass 1-1, Class.forName end, com.android.internal.R$styleable end, \" + loadedStringsClass);\n"
                        +
                        "        java.lang.reflect.Field color_field1 = loadedStringsClass.getField(\"TextAppearance_textColorHint\");\n"
                        +
                        "        Log.e (TAG, \"    TestClass 1-2, getField end, TextAppearance_textColorHint = \" + color_field1.getInt(null));\n"
                        +"        Log.e (TAG, \"TestClass 1, String load end\");\n"
                        +"    }\n"
                        +"}\n"))
                .run()
                .expect(expected);
    }
}