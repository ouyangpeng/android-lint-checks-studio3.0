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

package com.android.tools.lint.client.api

import com.google.common.annotations.Beta

/**
 * Information about SDKs
 *
 *
 * **NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.**
 */
@Beta
abstract class SdkInfo {
    /**
     * Returns true if the given child view is the same class or a sub class of
     * the given parent view class
     *
     * @param parentViewFqcn the fully qualified class name of the parent view
     *
     * @param childViewFqcn the fully qualified class name of the child view
     *
     * @return true if the child view is a sub view of (or the same class as)
     *         the parent view
     */
    open fun isSubViewOf(parentViewFqcn: String, childViewFqcn: String): Boolean {
        var current = childViewFqcn
        while (current != "android.view.View") {
            if (parentViewFqcn == current) {
                return true
            }
            val parent = getParentViewClass(current) ?: // Unknown view - err on the side of caution
                    return true
            current = parent
        }

        return false
    }

    /**
     * Returns the fully qualified name of the parent view, or null if the view
     * is the root android.view.View class.
     *
     * @param fqcn the fully qualified class name of the view
     *
     * @return the fully qualified class name of the parent view, or null
     */
    abstract fun getParentViewClass(fqcn: String): String?

    /**
     * Returns the class name of the parent view, or null if the view is the
     * root android.view.View class. This is the same as the
     * [.getParentViewClass] but without the package.
     *
     * @param name the view class name to look up the parent for (not including
     *             package)
     * @return the view name of the parent
     */
    abstract fun getParentViewName(name: String): String?

    /**
     * Returns true if the given widget name is a layout
     *
     * @param tag the XML tag for the view
     *
     * @return true if the given tag corresponds to a layout
     */
    open fun isLayout(tag: String): Boolean = tag.endsWith("Layout")

    // TODO: Add access to resource resolution here.
}
