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

package com.android.tools.lint.checks

import com.android.tools.lint.detector.api.Detector

class DuplicateIdDetectorTest : AbstractCheckTest() {
    override fun getDetector(): Detector {
        return DuplicateIdDetector()
    }

    fun testDuplicate() {
        val expected =
                """
res/layout/duplicate.xml:5: Error: Duplicate id @+id/android_logo, already defined earlier in this layout [DuplicateIds]
    <ImageButton android:id="@+id/android_logo" android:layout_width="wrap_content" android:layout_height="wrap_content" android:src="@drawable/android_button" android:focusable="false" android:clickable="false" android:layout_weight="1.0" />
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    res/layout/duplicate.xml:4: Duplicate id @+id/android_logo originally defined here
1 errors, 0 warnings
"""

        lint().files(
                xml("res/layout/duplicate.xml", """
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:id="@+id/newlinear" android:orientation="vertical" android:layout_width="match_parent" android:layout_height="match_parent">
    <Button android:text="Button" android:id="@+id/button1" android:layout_width="wrap_content" android:layout_height="wrap_content"></Button>
    <ImageView android:id="@+id/android_logo" android:layout_width="wrap_content" android:layout_height="wrap_content" android:src="@drawable/android_button" android:focusable="false" android:clickable="false" android:layout_weight="1.0" />
    <ImageButton android:id="@+id/android_logo" android:layout_width="wrap_content" android:layout_height="wrap_content" android:src="@drawable/android_button" android:focusable="false" android:clickable="false" android:layout_weight="1.0" />
    <Button android:text="Button" android:id="@+id/button2" android:layout_width="wrap_content" android:layout_height="wrap_content"></Button>
</LinearLayout>

"""))
                .run()
                .expect(expected)
    }

    fun testDuplicateChains() {
        val expected = """
res/layout/layout1.xml:7: Warning: Duplicate id @+id/button1, defined or included multiple times in layout/layout1.xml: [layout/layout1.xml defines @+id/button1, layout/layout1.xml => layout/layout2.xml => layout/layout3.xml defines @+id/button1, layout/layout1.xml => layout/layout2.xml => layout/layout4.xml defines @+id/button1] [DuplicateIncludedIds]
    <include
    ^
    res/layout/layout1.xml:13: Defined here
    res/layout/layout3.xml:8: Defined here, included via layout/layout1.xml => layout/layout2.xml => layout/layout3.xml defines @+id/button1
    res/layout/layout4.xml:8: Defined here, included via layout/layout1.xml => layout/layout2.xml => layout/layout4.xml defines @+id/button1
res/layout/layout1.xml:7: Warning: Duplicate id @+id/button2, defined or included multiple times in layout/layout1.xml: [layout/layout1.xml defines @+id/button2, layout/layout1.xml => layout/layout2.xml => layout/layout4.xml defines @+id/button2] [DuplicateIncludedIds]
    <include
    ^
    res/layout/layout1.xml:19: Defined here
    res/layout/layout4.xml:14: Defined here, included via layout/layout1.xml => layout/layout2.xml => layout/layout4.xml defines @+id/button2
res/layout/layout2.xml:18: Warning: Duplicate id @+id/button1, defined or included multiple times in layout/layout2.xml: [layout/layout2.xml => layout/layout3.xml defines @+id/button1, layout/layout2.xml => layout/layout4.xml defines @+id/button1] [DuplicateIncludedIds]
    <include
    ^
    res/layout/layout3.xml:8: Defined here, included via layout/layout2.xml => layout/layout3.xml defines @+id/button1
    res/layout/layout4.xml:8: Defined here, included via layout/layout2.xml => layout/layout4.xml defines @+id/button1
0 errors, 3 warnings
"""

        // layout1: defines @+id/button1, button2
        // layout3: defines @+id/button1
        // layout4: defines @+id/button1, button2
        // layout1 include layout2
        // layout2 includes layout3 and layout4

        // Therefore, layout3 and layout4 have no problems
        // In layout2, there's a duplicate definition of button1 (coming from 3 and 4)
        // In layout1, there's a duplicate definition of button1 (coming from layout1, 3 and 4)
        // In layout1, there'sa duplicate definition of button2 (coming from 1 and 4)

        lint().files(
                xml("res/layout/layout1.xml", """
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical" >

    <include
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        layout="@layout/layout2" />

    <Button
        android:id="@+id/button1"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Button" />

    <Button
        android:id="@+id/button2"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Button" />

</LinearLayout>
"""), mLayout2, mLayout3, mLayout4)
                .run()
                .expect(expected)
    }

    fun testSuppress() {
        val expected = """
res/layout/layout2.xml:18: Warning: Duplicate id @+id/button1, defined or included multiple times in layout/layout2.xml: [layout/layout2.xml => layout/layout3.xml defines @+id/button1, layout/layout2.xml => layout/layout4.xml defines @+id/button1] [DuplicateIncludedIds]
    <include
    ^
    res/layout/layout3.xml:8: Defined here, included via layout/layout2.xml => layout/layout3.xml defines @+id/button1
    res/layout/layout4.xml:8: Defined here, included via layout/layout2.xml => layout/layout4.xml defines @+id/button1
0 errors, 1 warnings
"""
        lint().files(
                xml("res/layout/layout1.xml", """
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical" >

    <include
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        layout="@layout/layout2"
        tools:ignore="DuplicateIncludedIds" />

    <Button
        android:id="@+id/button1"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Button" />

    <Button
        android:id="@+id/button2"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Button" />

</LinearLayout>
"""), mLayout2, mLayout3, mLayout4)
                .run()
                .expect(expected)
    }

    fun testSuppressForConstraintsSet() {
        lint().files(
                xml("res/layout/layout1.xml", """
<android.support.constraint.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:constraintSet="@+id/constraints"
    tools:layout_editor_absoluteX="0dp"
    tools:layout_editor_absoluteY="81dp">

    <Button
        android:id="@+id/button16"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="8dp"
        android:layout_marginTop="8dp"
        android:text="Button A"
        app:layout_constraintWidth_default="wrap"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <Button
        android:id="@+id/button18"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginBottom="8dp"
        android:layout_marginEnd="8dp"
        android:text="Button B"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintHorizontal_bias="0.5"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent" />

    <android.support.constraint.Constraints
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:id="@+id/constraints">

        <android.support.constraint.Reference
            android:id="@+id/button16"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:layout_marginTop="50dp"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintWidth_default="wrap"
            app:layout_constraintEnd_toEndOf="parent"
            android:layout_marginEnd="8dp"
            app:layout_constraintHorizontal_bias="0.498" />

        <android.support.constraint.Reference
            android:id="@+id/button18"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginBottom="8dp"
            android:layout_marginEnd="8dp"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintHorizontal_bias="0.5"
            app:layout_constraintLeft_toLeftOf="parent"
            app:layout_constraintRight_toRightOf="parent"
            android:layout_marginTop="8dp"
            app:layout_constraintTop_toBottomOf="@+id/button16" />
    </android.support.constraint.Constraints>

    <android.support.constraint.Constraints
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:id="@+id/constraints2">

        <android.support.constraint.Reference
            android:id="@+id/button16"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:layout_marginTop="8dp"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintWidth_default="wrap" />

        <android.support.constraint.Reference
            android:id="@+id/button18"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginBottom="8dp"
            android:layout_marginEnd="8dp"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintHorizontal_bias="0.5"
            app:layout_constraintLeft_toLeftOf="parent"
            app:layout_constraintRight_toRightOf="parent" />
    </android.support.constraint.Constraints>

</android.support.constraint.ConstraintLayout>
"""))
                .run()
                .expectClean()
    }

    fun testSuppressForEmbeddedTags() {
        lint().files(
                xml("res/layout/layout1.xml", """
<android.support.constraint.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <Button
        android:id="@+id/button1"
        android:text="A"
        android:layout_width="0dp"
        android:layout_height="wrap_content"/>

    <Button
        android:id="@+id/button2"
        android:text="B"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        tools:layout_editor_absoluteY="70dp"
        tools:layout_editor_absoluteX="171dp" />

    <Button
        android:id="@+id/button3"
        android:text="C"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        tools:layout_editor_absoluteY="145dp"
        tools:layout_editor_absoluteX="259dp" />

    <android.support.constraint.Chain
        app:layout_constraintLeft_toLeftOf="@+id/guideline"
        app:layout_constraintRight_toRightOf="@+id/guideline2"
        android:layout_width="0dp"
        android:layout_height="0dp">
        <tag android:id="@+id/button1" android:value="true" />
        <tag android:id="@+id/button2" android:value="true" />
        <tag android:id="@+id/button3" android:value="true" />
    </android.support.constraint.Chain>

    <android.support.constraint.Guideline
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:id="@+id/guideline"
        app:layout_constraintGuide_begin="79dp"
        android:orientation="vertical" />

    <android.support.constraint.Guideline
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:id="@+id/guideline2"
        android:orientation="vertical"
        app:layout_constraintGuide_end="43dp" />

</android.support.constraint.ConstraintLayout>
"""))
                .run()
                .expectClean()
    }

    fun testNavigationOk() {
        lint().files(
                xml("res/navigation/test.xml",
                        """
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
   xmlns:app="http://schemas.android.com/apk/res-auto">
  <fragment android:id="@+id/first">
    <action android:id="@+id/next" app:destination="@+id/second" />
  </fragment>
  <fragment android:id="@+id/second">
    <action android:id="@+id/next" app:destination="@+id/second" />
  </fragment>
  <fragment android:id="@+id/third" />
</navigation>"""))
                .run()
                .expectClean()
    }

    fun testNavigationDuplicate() {
        lint().files(
                xml("res/navigation/test.xml",
                        """
<navigation xmlns:android="http://schemas.android.com/apk/res/android">
  <foo>
  <fragment android:id="@+id/first" />
  </foo>
  <fragment android:id="@+id/first" />
  <fragment android:id="@+id/first" />
</navigation>"""))
                .run()
                .expect("""
res/navigation/test.xml:7: Error: Duplicate id @+id/first, already defined earlier in this layout [DuplicateIds]
  <fragment android:id="@+id/first" />
            ~~~~~~~~~~~~~~~~~~~~~~~
    res/navigation/test.xml:6: Duplicate id @+id/first originally defined here
1 errors, 0 warnings

""")
    }

    private val mLayout2 = xml("res/layout/layout2.xml", """
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical" >

    <RadioButton
        android:id="@+id/radioButton1"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="RadioButton" />

    <include
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        layout="@layout/layout3" />

    <include
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        layout="@layout/layout4" />

</LinearLayout>
""")

    private // Sample code
    val mLayout3 = xml("res/layout/layout3.xml", """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical" >

    <Button
        android:id="@+id/button1"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Button" />

    <CheckBox
        android:id="@+id/checkBox1"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="CheckBox" />

</LinearLayout>
""")

    private val mLayout4 = xml("res/layout/layout4.xml", """
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical" >

    <Button
        android:id="@+id/button1"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Button" />

    <Button
        android:id="@+id/button2"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Button" />

</LinearLayout>
""")
}
