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

package com.android.tools.lint.detector.api

import com.google.common.annotations.Beta

/**
 * A category is a container for related issues.
 *
 * **NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.**
 */
@Beta
data class Category
/**
 * Creates a new [Category].
 *
 * @param parent the name of a parent category, or null
 *
 * @param name the name of the category
 *
 * @param priority a sorting priority, with higher being more important
 */
constructor(
        /**
         * The parent category, or null if this is a top level category
         */
        val parent: Category?,

        /**
         * The name of this category
         */
        val name: String,
        private val priority: Int) : Comparable<Category> {

    /**
     * Returns a full name for this category. For a top level category, this is just
     * the [.getName] value, but for nested categories it will include the parent
     * names as well.
     *
     * @return a full name for this category
     */
    val fullName: String
        get() =
            if (parent != null) {
                parent.fullName + ':' + name
            } else {
                name
            }

    override fun toString(): String = fullName

    override fun compareTo(other: Category): Int {
        if (other.priority == priority) {
            if (parent === other) {
                return 1
            } else if (other.parent === this) {
                return -1
            }
        }

        val delta = other.priority - priority
        if (delta != 0) {
            return delta
        }

        return name.compareTo(other.name)
    }

    companion object {
        /**
         * Creates a new top level [Category] with the given sorting priority.
         *
         * @param name the name of the category
         *
         * @param priority a sorting priority, with higher being more important
         *
         * @return a new category
         */
        @JvmStatic fun create(name: String, priority: Int): Category =
                Category(null, name, priority)

        /**
         * Creates a new top level [Category] with the given sorting priority.
         *
         * @param parent the name of a parent category, or null
         *
         * @param name the name of the category
         *
         * @param priority a sorting priority, with higher being more important
         *
         * @return a new category
         */
        @JvmStatic fun create(parent: Category?, name: String, priority: Int): Category =
                Category(parent, name, priority)

        /** Issues related to running lint itself  */
        @JvmField val LINT = create("Lint", 110)

        /** Issues related to correctness  */
        @JvmField val CORRECTNESS = create("Correctness", 100)

        /** Issues related to security  */
        @JvmField val SECURITY = create("Security", 90)

        /** Issues related to performance  */
        @JvmField val PERFORMANCE = create("Performance", 80)

        /** Issues related to usability  */
        @JvmField val USABILITY = create("Usability", 70)

        /** Issues related to accessibility  */
        @JvmField val A11Y = create("Accessibility", 60)

        /** Issues related to internationalization  */
        @JvmField val I18N = create("Internationalization", 50)

        // Sub categories

        /** Issues related to icons  */
        @JvmField val ICONS = create(USABILITY, "Icons", 73)

        /** Issues related to typography  */
        @JvmField val TYPOGRAPHY = create(USABILITY, "Typography", 76)

        /** Issues related to messages/strings  */
        @JvmField val MESSAGES = create(CORRECTNESS, "Messages", 95)

        /** Issues related to Chrome OS devices  */
        @JvmField val CHROME_OS = create(CORRECTNESS, "Chrome OS", 93)

        /** Issues related to right to left and bidirectional text support  */
        @JvmField val RTL = create(I18N, "Bidirectional Text", 40)
    }
}
