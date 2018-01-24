/*
 * Copyright (C) 2011 The Android Open Source Project
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
public class StateListDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new StateListDetector();
    }

    public void testStates() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/drawable/states.xml:3: Warning: This item is unreachable because a previous item (item #1) is a more general match than this one [StateListReachable]\n"
                + "    <item android:state_pressed=\"true\"\n"
                + "    ^\n"
                + "    res/drawable/states.xml:2: Earlier item which masks item\n"
                + "0 errors, 1 warnings\n",
            lintProject(xml("res/drawable/states.xml", ""
                            + "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                            + "    <item  android:color=\"#ff000000\"/> <!-- WRONG, SHOULD BE LAST -->\n"
                            + "    <item android:state_pressed=\"true\"\n"
                            + "          android:color=\"#ffff0000\"/> <!-- pressed -->\n"
                            + "    <item android:state_focused=\"true\"\n"
                            + "          android:color=\"#ff0000ff\"/> <!-- focused -->\n"
                            + "</selector>\n")));
    }

    public void testCustomStates() throws Exception {
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",
            lintProject(xml("res/drawable/states2.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:app=\"http://schemas.android.com/apk/res/com.domain.pkg\">\n"
                            + "<item\n"
                            + "    app:mystate_custom=\"false\"\n"
                            + "    android:drawable=\"@drawable/item\" />\n"
                            + "</selector>\n")));
    }

    public void testStates3() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/drawable/states3.xml:24: Warning: This item is unreachable because a previous item (item #1) is a more general match than this one [StateListReachable]\n"
                + "    <item android:state_checked=\"false\" android:state_window_focused=\"false\"\n"
                + "    ^\n"
                + "    res/drawable/states3.xml:18: Earlier item which masks item\n"
                + "0 errors, 1 warnings\n",
            lintProject(xml("res/drawable/states3.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<!-- Copyright (C) 2008 The Android Open Source Project\n"
                            + "\n"
                            + "     Licensed under the Apache License, Version 2.0 (the \"License\");\n"
                            + "     you may not use this file except in compliance with the License.\n"
                            + "     You may obtain a copy of the License at\n"
                            + "\n"
                            + "          http://www.apache.org/licenses/LICENSE-2.0\n"
                            + "\n"
                            + "     Unless required by applicable law or agreed to in writing, software\n"
                            + "     distributed under the License is distributed on an \"AS IS\" BASIS,\n"
                            + "     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
                            + "     See the License for the specific language governing permissions and\n"
                            + "     limitations under the License.\n"
                            + "-->\n"
                            + "\n"
                            + "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                            + "    <item android:state_checked=\"false\" android:state_window_focused=\"false\"\n"
                            + "          android:drawable=\"@drawable/btn_star_big_off\" />\n"
                            + "    <item android:state_checked=\"true\" android:state_window_focused=\"false\"\n"
                            + "          android:drawable=\"@drawable/btn_star_big_on\" />\n"
                            + "    <item android:state_checked=\"true\" android:state_window_focused=\"false\"\n"
                            + "          android:state_enabled=\"false\" android:drawable=\"@drawable/btn_star_big_on_disable\" />\n"
                            + "    <item android:state_checked=\"false\" android:state_window_focused=\"false\"\n"
                            + "          android:state_enabled=\"false\" android:drawable=\"@drawable/btn_star_big_off_disable\" />\n"
                            + "\n"
                            + "    <item android:state_checked=\"true\" android:state_pressed=\"true\"\n"
                            + "          android:drawable=\"@drawable/btn_star_big_on_pressed\" />\n"
                            + "    <item android:state_checked=\"false\" android:state_pressed=\"true\"\n"
                            + "          android:drawable=\"@drawable/btn_star_big_off_pressed\" />\n"
                            + "\n"
                            + "    <item android:state_checked=\"true\" android:state_focused=\"true\"\n"
                            + "          android:drawable=\"@drawable/btn_star_big_on_selected\" />\n"
                            + "    <item android:state_checked=\"false\" android:state_focused=\"true\"\n"
                            + "          android:drawable=\"@drawable/btn_star_big_off_selected\" />\n"
                            + "\n"
                            + "    <item android:state_checked=\"true\" android:state_focused=\"true\" android:state_enabled=\"false\"\n"
                            + "          android:drawable=\"@drawable/btn_star_big_on_disable_focused\" />\n"
                            + "    <item android:state_checked=\"true\" android:state_focused=\"false\" android:state_enabled=\"false\"\n"
                            + "          android:drawable=\"@drawable/btn_star_big_on_disable\" />\n"
                            + "\n"
                            + "    <item android:state_checked=\"false\" android:state_focused=\"true\" android:state_enabled=\"false\"\n"
                            + "          android:drawable=\"@drawable/btn_star_big_off_disable_focused\" />\n"
                            + "    <item android:state_checked=\"false\" android:state_focused=\"false\" android:state_enabled=\"false\"\n"
                            + "          android:drawable=\"@drawable/btn_star_big_off_disable\" />\n"
                            + "\n"
                            + "    <item android:state_checked=\"false\" android:drawable=\"@drawable/btn_star_big_off\" />\n"
                            + "    <item android:state_checked=\"true\" android:drawable=\"@drawable/btn_star_big_on\" />\n"
                            + "</selector>\n")));
    }
}
