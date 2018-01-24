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
import java.io.File;
import org.intellij.lang.annotations.Language;

@SuppressWarnings("javadoc")
public class RequiredAttributeDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new RequiredAttributeDetector();
    }

    public void test() throws Exception {
        // Simple: Only consider missing attributes in the layout xml file
        // (though skip warnings on <merge> tags and under <GridLayout>
        lint().files(mSize).run().expect(""
                + "res/layout/size.xml:13: Error: The required layout_height attribute is missing [RequiredSize]\n"
                + "    <RadioButton\n"
                + "    ^\n"
                + "res/layout/size.xml:18: Error: The required layout_width attribute is missing [RequiredSize]\n"
                + "    <EditText\n"
                + "    ^\n"
                + "res/layout/size.xml:23: Error: The required layout_width and layout_height attributes are missing [RequiredSize]\n"
                + "    <EditText\n"
                + "    ^\n"
                + "3 errors, 0 warnings\n");
    }

    public void test2() throws Exception {
        // Consider styles (specifying sizes) and includes (providing sizes for the root tags)
        String expected = ""
                + "res/layout/size2.xml:9: Error: The required layout_width and layout_height attributes are missing [RequiredSize]\n"
                + "    <Button\n"
                + "    ^\n"
                + "res/layout/size2.xml:18: Error: The required layout_height attribute is missing [RequiredSize]\n"
                + "    <Button\n"
                + "    ^\n"
                + "2 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/size2.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:orientation=\"vertical\"\n"
                        + "    tools:ignore=\"HardcodedText\" >\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:id=\"@+id/button1\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:id=\"@+id/button2\"\n"
                        + "        style=\"@style/WidthAndHeight\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:id=\"@+id/button3\"\n"
                        + "        style=\"@style/Width\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:id=\"@+id/button4\"\n"
                        + "        style=\"@style/MyStyle\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:id=\"@+id/button5\"\n"
                        + "        style=\"@style/MyStyle.Big\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:id=\"@+id/button6\"\n"
                        + "        style=\"@style/MyOtherStyle\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <include\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"match_parent\"\n"
                        + "        layout=\"@layout/sizeincluded\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"),
                mSizeincluded,
                xml("res/values/sizestyles.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "\n"
                        + "    <style name=\"WidthAndHeight\" parent=\"@android:attr/textAppearanceMedium\">\n"
                        + "        <item name=\"android:layout_width\">match_parent</item>\n"
                        + "        <item name=\"android:layout_height\">match_parent</item>\n"
                        + "    </style>\n"
                        + "\n"
                        + "    <style name=\"Width\" parent=\"@android:attr/textAppearanceMedium\">\n"
                        + "        <item name=\"android:layout_width\">match_parent</item>\n"
                        + "    </style>\n"
                        + "\n"
                        + "    <style name=\"MyStyle\" parent=\"@style/WidthAndHeight\"></style>\n"
                        + "    <style name=\"MyStyle.Big\"></style>\n"
                        + "    <style name=\"MyOtherStyle\" parent=\"@style/MyStyle.Big\"></style>\n"
                        + "\n"
                        + "</resources>\n"))
                .run()
                .expect(expected);
    }

    public void testPercent() throws Exception {
        // Regression test for https://code.google.com/p/android/issues/detail?id=198432
        // Don't flag missing layout_width in PercentFrameLayout or PercentRelativeLayout
        String expected = ""
                + "res/layout/test.xml:28: Error: The required layout_width or layout_widthPercent and layout_height or layout_heightPercent attributes are missing [RequiredSize]\n"
                + "        <View />\n"
                + "        ~~~~~~~~\n"
                + "res/layout/test.xml:30: Error: The required layout_width or layout_widthPercent attribute is missing [RequiredSize]\n"
                + "        <View\n"
                + "        ^\n"
                + "res/layout/test.xml:34: Error: The required layout_height or layout_heightPercent attribute is missing [RequiredSize]\n"
                + "        <View\n"
                + "        ^\n"
                + "3 errors, 0 warnings\n";

        //noinspection all // Sample code
        lint().files(
                xml("res/layout/test.xml", ""
                        + "<merge xmlns:android=\"http://schemas.android.com/apk/res/android\""
                        + "     xmlns:app=\"http://schemas.android.com/apk/res-auto\">\n"
                        + "  <android.support.percent.PercentFrameLayout\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"match_parent\"\n"
                        + "        >\n"
                        + "        <View\n"
                        + "            app:layout_widthPercent=\"50%\"\n"
                        + "            app:layout_heightPercent=\"50%\"/>\n"
                        + "        <View\n"
                        + "            android:layout_width=\"wrap_content\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            app:layout_marginStartPercent=\"25%\"\n"
                        + "            app:layout_marginEndPercent=\"25%\"/>\n"
                        + "        <View\n"
                        + "            android:id=\"@+id/textview2\"\n"
                        + "            android:layout_height=\"wrap_content\"\n"
                        + "            app:layout_widthPercent=\"60%\"/>\n"
                        + "    </android.support.percent.PercentFrameLayout>"
                        + "\n"
                        + "    <android.support.percent.PercentRelativeLayout\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"match_parent\">\n"
                        + "        <View\n"
                        + "            android:layout_gravity=\"center\"\n"
                        + "            app:layout_widthPercent=\"50%\"\n"
                        + "            app:layout_heightPercent=\"50%\"/>\n"
                        + "        <!-- Errors -->\n"
                        + "        <!-- Missing both -->\n"
                        + "        <View />\n"
                        + "        <!-- Missing width -->\n"
                        + "        <View\n"
                        + "            android:layout_gravity=\"center\"\n"
                        + "            app:layout_heightPercent=\"50%\"/>\n"
                        + "        <!-- Missing height -->\n"
                        + "        <View\n"
                        + "            android:layout_gravity=\"center\"\n"
                        + "            app:layout_widthPercent=\"50%\"/>\n"
                        + "\n"
                        + "    </android.support.percent.PercentRelativeLayout>\n"
                        + "\n"
                        + "</merge>"))
                .run()
                .expect(expected);
    }

    public void testInflaters() throws Exception {
        // Consider java inflation
        String expected = ""
                + "res/layout/size5.xml:2: Error: The required layout_width and layout_height attributes are missing [RequiredSize]\n"
                + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "^\n"
                + "1 errors, 0 warnings\n";

        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import com.example.includetest.R;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.content.Context;\n"
                        + "import android.os.Bundle;\n"
                        + "import android.view.LayoutInflater;\n"
                        + "import android.view.View;\n"
                        + "import android.view.ViewGroup;\n"
                        + "import android.widget.Button;\n"
                        + "\n"
                        + "@SuppressWarnings(\"unused\")\n"
                        + "public class InflaterTest extends Activity {\n"
                        + "    private LayoutInflater mInflater;\n"
                        + "    private View mRootView;\n"
                        + "\n"
                        + "    private LayoutInflater getInflater() {\n"
                        + "        if (mInflater == null) {\n"
                        + "            mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);\n"
                        + "        }\n"
                        + "        return mInflater;\n"
                        + "    }\n"
                        + "\n"
                        + "    @Override\n"
                        + "    protected void onCreate(Bundle savedInstanceState) {\n"
                        + "        super.onCreate(savedInstanceState);\n"
                        + "    }\n"
                        + "\n"
                        + "    public void testInflate1() {\n"
                        + "        View.inflate(this, R.layout.size1, null);\n"
                        + "    }\n"
                        + "\n"
                        + "    public void testInflate2() {\n"
                        + "        mRootView = getInflater().inflate(R.layout.size2, null);\n"
                        + "    }\n"
                        + "\n"
                        + "    public void testInflate4() {\n"
                        + "        getInflater().inflate(R.layout.size3, null, false);\n"
                        + "    }\n"
                        + "\n"
                        + "    public void testInflate5() {\n"
                        + "        int mylayout = R.layout.size4;\n"
                        + "        getInflater().inflate(mylayout, null, false);\n"
                        + "    }\n"
                        + "\n"
                        + "    public void testNotNull(ViewGroup root) {\n"
                        + "        getInflater().inflate(R.layout.size5, root, false); // Should be flagged\n"
                        + "    }\n"
                        + "\n"
                        + "    public void testInflate6() {\n"
                        + "        int mylayout = R.layout.size7;\n"
                        + "        View.inflate(this, mylayout, null);\n"
                        + "    }\n"
                        + "\n"
                        + "    public class MyButton extends Button {\n"
                        + "        public MyButton(Context context) {\n"
                        + "            super(context);\n"
                        + "        }\n"
                        + "\n"
                        + "        public void test() {\n"
                        + "            inflate(getContext(), R.layout.size6, null);\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    public static final class R {\n"
                        + "        public static final class layout {\n"
                        + "            public static final int size1 = 0x7f0a0000;\n"
                        + "            public static final int size2 = 0x7f0a0001;\n"
                        + "            public static final int size3 = 0x7f0a0002;\n"
                        + "            public static final int size4 = 0x7f0a0003;\n"
                        + "            public static final int size5 = 0x7f0a0004;\n"
                        + "            public static final int size6 = 0x7f0a0005;\n"
                        + "            public static final int size7 = 0x7f0a0006;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"),
                mSizeincluded2,
                mSizeincluded3,
                mSizeincluded4,
                mSizeincluded5,
                mSizeincluded6,
                mSizeincluded7,
                mSizeincluded8)
                .run()
                .expect(expected);
    }

    public void testRequestFocus() throws Exception {
        // See http://code.google.com/p/android/issues/detail?id=38700
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/edit_type.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "    <!-- Wrong: doesn't specify textPassword -->\n"
                        + "\n"
                        + "    <EditText\n"
                        + "        android:id=\"@+id/mypassword\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:ems=\"10\"\n"
                        + "        android:inputType=\"text\" >\n"
                        + "\n"
                        + "        <requestFocus />\n"
                        + "    </EditText>\n"
                        + "\n"
                        + "    <!-- OK, specifies textPassword: -->\n"
                        + "\n"
                        + "    <EditText\n"
                        + "        android:id=\"@+id/password1\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:ems=\"10\"\n"
                        + "        android:inputType=\"text|numberPassword\" />\n"
                        + "\n"
                        + "    <!-- OK, specifies password: -->\n"
                        + "\n"
                        + "    <EditText\n"
                        + "        android:id=\"@+id/password2\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:ems=\"10\"\n"
                        + "        android:inputType=\"text\"\n"
                        + "        android:password=\"true\" />\n"
                        + "\n"
                        + "    <!-- Wrong, doesn't include number -->\n"
                        + "\n"
                        + "    <EditText\n"
                        + "        android:id=\"@+id/password_length\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:ems=\"10\"\n"
                        + "        android:inputType=\"text\" />\n"
                        + "\n"
                        + "    <!-- Wrong, doesn't include URL -->\n"
                        + "\n"
                        + "    <EditText\n"
                        + "        android:id=\"@+id/welcome_url\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:ems=\"10\"\n"
                        + "        android:inputType=\"text\" />\n"
                        + "\n"
                        + "    <!-- Wrong, doesn't include date -->\n"
                        + "\n"
                        + "    <EditText\n"
                        + "        android:id=\"@+id/start_date\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:ems=\"10\"\n"
                        + "        android:inputType=\"text\" />\n"
                        + "\n"
                        + "    <!-- Wrong, doesn't include e-mail -->\n"
                        + "\n"
                        + "    <EditText\n"
                        + "        android:id=\"@+id/email_address\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:ems=\"10\"\n"
                        + "        android:inputType=\"text\" />\n"
                        + "\n"
                        + "    <!-- Wrong, uses wrong password type for PIN -->\n"
                        + "\n"
                        + "    <EditText\n"
                        + "        android:id=\"@+id/login_pin\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:ems=\"10\"\n"
                        + "        android:inputType=\"textPassword\" />\n"
                        + "\n"
                        + "    <EditText\n"
                        + "        android:id=\"@+id/number_of_items\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:ems=\"10\" />\n"
                        + "\n"
                        + "    <EditText\n"
                        + "        style=\"@style/foo\"\n"
                        + "        android:id=\"@+id/number_of_items\"\n"
                        + "        android:layout_width=\"match_parent\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:ems=\"10\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"))
                .run()
                .expectClean();
    }

    public void testFrameworkStyles() throws Exception {
        // See http://code.google.com/p/android/issues/detail?id=38958
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/listseparator.xml", ""
                        + "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:id=\"@id/text1\"\n"
                        + "    style=\"?android:attr/listSeparatorTextViewStyle\" />\n"))
                .run()
                .expectClean();
    }

    public void testThemeStyles() throws Exception {
        // Check that we don't complain about cases where the size is defined in a theme
        //noinspection all // Sample code
        lint().files(
                mSize,
                xml("res/values/themes.xml", ""
                        + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "\n"
                        + "    <style name=\"MyTheme\" parent=\"@android:style/Theme\">\n"
                        + "        <item name=\"android:layout_width\">wrap_content</item>\n"
                        + "        <item name=\"android:layout_height\">wrap_content</item>\n"
                        + "    </style>\n"
                        + "\n"
                        + "</resources>\n"))
                .run()
                .expectClean();
    }

    public void testThemeStyles2() throws Exception {
        // Check that we don't complain about cases where the size is defined in a theme
        //noinspection all // Sample code
        lint().files(
                mSize,
                xml("res/values/themes2.xml", ""
                        + "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "\n"
                        + "    <style name=\"MyStyle\" parent=\"@style/Theme.Holo.Light\"></style>\n"
                        + "    <style name=\"MyStyle.Big\"></style>\n"
                        + "    <style name=\"MyOtherStyle\" parent=\"@style/MyStyle.Big\">\n"
                        + "        <item name=\"android:layout_width\">wrap_content</item>\n"
                        + "        <item name=\"android:layout_height\">wrap_content</item>\n"
                        + "    </style>\n"
                        + "\n"
                        + "</resources>\n"))
                .run()
                .expectClean();
    }

    public void testHasLayoutVariations() throws Exception {
        File projectDir = getProjectDir(null,
                xml("res/layout/size.xml", SIZE_XML),
                xml("res/layout-land/size.xml", SIZE_XML),
                xml("res/layout/size2.xml", SIZE_XML));
        assertTrue(RequiredAttributeDetector.hasLayoutVariations(
                new File(projectDir, "res/layout/size.xml".replace('/', File.separatorChar))));
        assertTrue(RequiredAttributeDetector.hasLayoutVariations(
                new File(projectDir, "res/layout-land/size.xml".replace('/', File.separatorChar))));
        assertFalse(RequiredAttributeDetector.hasLayoutVariations(
                new File(projectDir, "res/layout/size2.xml".replace('/', File.separatorChar))));
    }

    public void testDataBinding() throws Exception {
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/db.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<layout>\n"
                        + "    <data>\n"
                        + "        <variable />\n"
                        + "    </data>\n"
                        + "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "                android:orientation=\"vertical\"\n"
                        + "                android:layout_width=\"match_parent\"\n"
                        + "                android:layout_height=\"match_parent\">\n"
                        + "\n"
                        + "    <LinerLayout android:layout_width=\"match_parent\"\n"
                        + "                 android:layout_height=\"match_parent\"/>\n"
                        + "</RelativeLayout>\n"
                        + "</layout>\n"))
                .run()
                .expectClean();
    }

    @Language("XML")
    private static final String SIZE_XML = ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:layout_width=\"match_parent\"\n"
            + "    android:layout_height=\"match_parent\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "    <RadioButton\n"
            + "        android:id=\"@+id/button\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <RadioButton\n"
            + "        android:id=\"@+id/button2\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <EditText\n"
            + "        android:id=\"@+id/edittext\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:text=\"EditText\" />\n"
            + "\n"
            + "    <EditText\n"
            + "        android:id=\"@+id/edittext2\"\n"
            + "        android:text=\"EditText\" />\n"
            + "\n"
            + "</LinearLayout>\n";

    @SuppressWarnings("all") // Sample code
    private TestFile mSize = xml("res/layout/size.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:layout_width=\"match_parent\"\n"
            + "    android:layout_height=\"match_parent\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "    <RadioButton\n"
            + "        android:id=\"@+id/button\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <RadioButton\n"
            + "        android:id=\"@+id/button2\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <EditText\n"
            + "        android:id=\"@+id/edittext\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:text=\"EditText\" />\n"
            + "\n"
            + "    <EditText\n"
            + "        android:id=\"@+id/edittext2\"\n"
            + "        android:text=\"EditText\" />\n"
            + "\n"
            + "</LinearLayout>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mSizeincluded = xml("res/layout/sizeincluded.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "</LinearLayout>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mSizeincluded2 = xml("res/layout/size1.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "</LinearLayout>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mSizeincluded3 = xml("res/layout/size2.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "</LinearLayout>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mSizeincluded4 = xml("res/layout/size3.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "</LinearLayout>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mSizeincluded5 = xml("res/layout/size4.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "</LinearLayout>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mSizeincluded6 = xml("res/layout/size5.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "</LinearLayout>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mSizeincluded7 = xml("res/layout/size6.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "</LinearLayout>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mSizeincluded8 = xml("res/layout/size7.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:orientation=\"vertical\" >\n"
            + "\n"
            + "</LinearLayout>\n");
}
