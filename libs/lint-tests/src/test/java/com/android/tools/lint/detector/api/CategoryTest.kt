/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.tools.lint.detector.api

import com.google.common.base.Joiner
import com.google.common.collect.Lists
import junit.framework.TestCase
import java.lang.reflect.Modifier
import java.util.Collections

class CategoryTest : TestCase() {
    fun testCompare() {
        val categories = Lists.newArrayList<Category>()
        for (field in Category::class.java.declaredFields) {
            if (field.type == Category::class.java && field.modifiers and Modifier.STATIC != 0) {
                field.isAccessible = true
                val o = field.get(null)
                if (o is Category) {
                    categories.add(o)
                }
            }
        }

        Collections.sort(categories)

        assertEquals(""
                + "Lint\n"
                + "Correctness\n"
                + "Correctness:Messages\n"
                + "Correctness:Chrome OS\n"
                + "Security\n"
                + "Performance\n"
                + "Usability:Typography\n"
                + "Usability:Icons\n"
                + "Usability\n"
                + "Accessibility\n"
                + "Internationalization\n"
                + "Internationalization:Bidirectional Text",
                Joiner.on("\n").join(categories))
    }

    fun testGetName() = assertEquals("Messages", Category.MESSAGES.name)

    fun testGetFullName() = assertEquals("Correctness:Messages", Category.MESSAGES.fullName)

    fun testEquals() {
        assertEquals(Category.MESSAGES, Category.MESSAGES)
        assertEquals(Category.create("Correctness", 100), Category.create("Correctness", 100))
        assertFalse(Category.MESSAGES == Category.CORRECTNESS)
        assertFalse(Category.create("Correctness", 100) == Category.create("Correct", 100))
    }
}
