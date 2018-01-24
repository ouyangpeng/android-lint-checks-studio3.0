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

package com.android.tools.lint.checks

import com.android.tools.lint.detector.api.Detector

class ExtraTextDetectorTest : AbstractCheckTest() {
    override fun getDetector(): Detector = ExtraTextDetector()

    fun testBroken() {
        val expected =
"""
res/layout/broken.xml:6: Warning: Unexpected text found in layout file: "ImageButton android:id="@+id/android_logo2" android:layout_width="wrap_content" android:layout_heigh..." [ExtraText]
    <Button android:text="Button" android:id="@+id/button2" android:layout_width="wrap_content" android:layout_height="wrap_content"></Button>
    ^
0 errors, 1 warnings
"""

        lint().files(
                xml("res/layout/broken.xml",
"""<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:id="@+id/newlinear" android:orientation="vertical" android:layout_width="match_parent" android:layout_height="match_parent">
    <Button android:text="Button" android:id="@+id/button1" android:layout_width="wrap_content" android:layout_height="wrap_content"></Button>
    <ImageView android:id="@+id/android_logo" android:layout_width="wrap_content" android:layout_height="wrap_content" android:src="@drawable/android_button" android:focusable="false" android:clickable="false" android:layout_weight="1.0" />
    ImageButton android:id="@+id/android_logo2" android:layout_width="wrap_content" android:layout_height="wrap_content" android:src="@drawable/android_button" android:focusable="false" android:clickable="false" android:layout_weight="1.0" />
    <Button android:text="Button" android:id="@+id/button2" android:layout_width="wrap_content" android:layout_height="wrap_content"></Button>
    <Button android:id="@+android:id/summary" android:contentDescription="@string/label" />
</LinearLayout>"""))
                .run()
                .expect(expected)
    }

    fun testManifest() {
        val expected =
                """
AndroidManifest.xml:8: Warning: Unexpected text found in layout file: "permission android:name="com.android.vending.BILLING"
        android:label="@string/perm_billing_la..." [ExtraText]
        android:description="@string/perm_billing_desc"
    ^
0 errors, 1 warnings
"""

        lint().files(
                manifest(
"""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          xmlns:tools="http://schemas.android.com/tools">
    <uses-feature android:name="android.software.leanback"/>

    permission android:name="com.android.vending.BILLING"
        android:label="@string/perm_billing_label"
        android:description="@string/perm_billing_desc"
        android:permissionGroup="android.permission-group.NETWORK"
        android:protectionLevel="normal" />

    <application android:banner="@drawable/banner">
        <activity>
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LEANBACK_LAUNCHER"/>
            </intent-filter>
        </activity>
    </application>
</manifest>
"""))
                .run()
                .expect(expected)
    }

    fun testValuesOk() {
        lint().files(
                xml("res/values/strings.xml",
                        """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="foo">Foo</string>
</resources>"""))
                .run()
                .expectClean()
    }
}
