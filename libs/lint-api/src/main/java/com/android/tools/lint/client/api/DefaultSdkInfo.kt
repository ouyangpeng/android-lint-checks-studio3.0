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

import com.android.SdkConstants.ABSOLUTE_LAYOUT
import com.android.SdkConstants.ABS_LIST_VIEW
import com.android.SdkConstants.ABS_SEEK_BAR
import com.android.SdkConstants.ABS_SPINNER
import com.android.SdkConstants.ADAPTER_VIEW
import com.android.SdkConstants.AUTO_COMPLETE_TEXT_VIEW
import com.android.SdkConstants.BUTTON
import com.android.SdkConstants.CHECKABLE
import com.android.SdkConstants.CHECKED_TEXT_VIEW
import com.android.SdkConstants.CHECK_BOX
import com.android.SdkConstants.COMPOUND_BUTTON
import com.android.SdkConstants.EDIT_TEXT
import com.android.SdkConstants.EXPANDABLE_LIST_VIEW
import com.android.SdkConstants.FRAME_LAYOUT
import com.android.SdkConstants.GALLERY
import com.android.SdkConstants.GRID_VIEW
import com.android.SdkConstants.HORIZONTAL_SCROLL_VIEW
import com.android.SdkConstants.IMAGE_BUTTON
import com.android.SdkConstants.IMAGE_VIEW
import com.android.SdkConstants.LINEAR_LAYOUT
import com.android.SdkConstants.LIST_VIEW
import com.android.SdkConstants.MULTI_AUTO_COMPLETE_TEXT_VIEW
import com.android.SdkConstants.PROGRESS_BAR
import com.android.SdkConstants.RADIO_BUTTON
import com.android.SdkConstants.RADIO_GROUP
import com.android.SdkConstants.RELATIVE_LAYOUT
import com.android.SdkConstants.SCROLL_VIEW
import com.android.SdkConstants.SEEK_BAR
import com.android.SdkConstants.SPINNER
import com.android.SdkConstants.SURFACE_VIEW
import com.android.SdkConstants.SWITCH
import com.android.SdkConstants.TABLE_LAYOUT
import com.android.SdkConstants.TABLE_ROW
import com.android.SdkConstants.TAB_HOST
import com.android.SdkConstants.TAB_WIDGET
import com.android.SdkConstants.TEXT_VIEW
import com.android.SdkConstants.TOGGLE_BUTTON
import com.android.SdkConstants.VIEW
import com.android.SdkConstants.VIEW_ANIMATOR
import com.android.SdkConstants.VIEW_GROUP
import com.android.SdkConstants.VIEW_PKG_PREFIX
import com.android.SdkConstants.VIEW_STUB
import com.android.SdkConstants.VIEW_SWITCHER
import com.android.SdkConstants.WEB_VIEW
import com.android.SdkConstants.WIDGET_PKG_PREFIX
import com.google.common.annotations.Beta

/**
 * Default simple implementation of an [SdkInfo]
 *
 *
 * **NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.**
 */
@Beta
internal class DefaultSdkInfo : SdkInfo() {
    override fun getParentViewName(name: String): String? {
        val rawType = getRawType(name) ?: return null
        return getParent(rawType)
    }

    override fun getParentViewClass(fqcn: String): String? {
        var simpleName = fqcn
        val index = simpleName.lastIndexOf('.')
        if (index != -1) {
            simpleName = simpleName.substring(index + 1)
        }

        val parent = getParent(simpleName) ?: return null
        // The map only stores class names internally; correct for full package paths
        return if (parent == VIEW || parent == VIEW_GROUP || parent == SURFACE_VIEW) {
            VIEW_PKG_PREFIX + parent
        } else {
            WIDGET_PKG_PREFIX + parent
        }
    }

    override fun isSubViewOf(parentViewFqcn: String, childViewFqcn: String): Boolean {
        var parent = getRawType(parentViewFqcn) ?: return false
        var child: String? = getRawType(childViewFqcn)

        // Do analysis just on non-fqcn paths
        if (parent.indexOf('.') != -1) {
            parent = parent.substring(parent.lastIndexOf('.') + 1)
        }
        if (child!!.indexOf('.') != -1) {
            child = child.substring(child.lastIndexOf('.') + 1)
        }

        if (parent == VIEW) {
            return true
        }

        while (child != VIEW) {
            if (parent == child) {
                return true
            }
            if (implementsInterface(child!!, parent)) {
                return true
            }
            child = getParent(child)
            if (child == null) {
                // Unknown view - err on the side of caution
                return true
            }
        }

        return false
    }

    private fun implementsInterface(className: String, interfaceName: String): Boolean =
            interfaceName == getInterface(className)

    // Strip off type parameters, e.g. AdapterView<?> ⇒ AdapterView
    private fun getRawType(type: String?): String? {
        if (type != null) {
            val index = type.indexOf('<')
            if (index != -1) {
                return type.substring(0, index)
            }
        }

        return type
    }

    override fun isLayout(tag: String): Boolean {
        // TODO: Read in widgets.txt from the platform install area to look up this information
        // dynamically instead!

        if (super.isLayout(tag)) {
            return true
        }

        when (tag) {
            TAB_HOST, HORIZONTAL_SCROLL_VIEW, VIEW_SWITCHER, TAB_WIDGET, VIEW_ANIMATOR,
            SCROLL_VIEW, GRID_VIEW, TABLE_ROW, RADIO_GROUP, LIST_VIEW, EXPANDABLE_LIST_VIEW,
            "MediaController", "DialerFilter", "ViewFlipper", "SlidingDrawer", "StackView",
            "SearchView", "TextSwitcher", "AdapterViewFlipper", "ImageSwitcher" -> return true
        }

        return false
    }

    private fun getParent(layout: String): String? {
        when (layout) {
            COMPOUND_BUTTON -> return BUTTON
            ABS_SPINNER -> return ADAPTER_VIEW
            ABS_LIST_VIEW -> return ADAPTER_VIEW
            ABS_SEEK_BAR -> return PROGRESS_BAR
            ADAPTER_VIEW -> return VIEW_GROUP
            VIEW_GROUP -> return VIEW

            TEXT_VIEW -> return VIEW
            CHECKED_TEXT_VIEW -> return TEXT_VIEW
            RADIO_BUTTON -> return COMPOUND_BUTTON
            SPINNER -> return ABS_SPINNER
            IMAGE_BUTTON -> return IMAGE_VIEW
            IMAGE_VIEW -> return VIEW
            EDIT_TEXT -> return TEXT_VIEW
            PROGRESS_BAR -> return VIEW
            TOGGLE_BUTTON -> return COMPOUND_BUTTON
            VIEW_STUB -> return VIEW
            BUTTON -> return TEXT_VIEW
            SEEK_BAR -> return ABS_SEEK_BAR
            CHECK_BOX -> return COMPOUND_BUTTON
            SWITCH -> return COMPOUND_BUTTON
            GALLERY -> return ABS_SPINNER
            SURFACE_VIEW -> return VIEW
            ABSOLUTE_LAYOUT -> return VIEW_GROUP
            LINEAR_LAYOUT -> return VIEW_GROUP
            RELATIVE_LAYOUT -> return VIEW_GROUP
            LIST_VIEW -> return ABS_LIST_VIEW
            VIEW_SWITCHER -> return VIEW_ANIMATOR
            FRAME_LAYOUT -> return VIEW_GROUP
            HORIZONTAL_SCROLL_VIEW -> return FRAME_LAYOUT
            VIEW_ANIMATOR -> return FRAME_LAYOUT
            TAB_HOST -> return FRAME_LAYOUT
            TABLE_ROW -> return LINEAR_LAYOUT
            RADIO_GROUP -> return LINEAR_LAYOUT
            TAB_WIDGET -> return LINEAR_LAYOUT
            EXPANDABLE_LIST_VIEW -> return LIST_VIEW
            TABLE_LAYOUT -> return LINEAR_LAYOUT
            SCROLL_VIEW -> return FRAME_LAYOUT
            GRID_VIEW -> return ABS_LIST_VIEW
            WEB_VIEW -> return ABSOLUTE_LAYOUT
            AUTO_COMPLETE_TEXT_VIEW -> return EDIT_TEXT
            MULTI_AUTO_COMPLETE_TEXT_VIEW -> return AUTO_COMPLETE_TEXT_VIEW

            "MediaController" -> return FRAME_LAYOUT
            "SlidingDrawer" -> return VIEW_GROUP
            "DialerFilter" -> return RELATIVE_LAYOUT
            "DigitalClock" -> return TEXT_VIEW
            "Chronometer" -> return TEXT_VIEW
            "ImageSwitcher" -> return VIEW_SWITCHER
            "TextSwitcher" -> return VIEW_SWITCHER
            "AnalogClock" -> return VIEW
            "TwoLineListItem" -> return RELATIVE_LAYOUT
            "ZoomControls" -> return LINEAR_LAYOUT
            "DatePicker" -> return FRAME_LAYOUT
            "TimePicker" -> return FRAME_LAYOUT
            "VideoView" -> return SURFACE_VIEW
            "ZoomButton" -> return IMAGE_BUTTON
            "RatingBar" -> return ABS_SEEK_BAR
            "ViewFlipper" -> return VIEW_ANIMATOR
            "NumberPicker" -> return LINEAR_LAYOUT
        }

        return null
    }

    private fun getInterface(cls: String): String? =
            when (cls) {
                CHECKED_TEXT_VIEW, COMPOUND_BUTTON -> CHECKABLE
                else -> null
            }
}
