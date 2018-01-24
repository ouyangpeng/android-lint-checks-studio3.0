/*
 * Copyright (C) 2016 The Android Open Source Project
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

import com.android.SdkConstants.ANDROID_URI
import com.android.SdkConstants.ATTR_NAME
import com.android.SdkConstants.TAG_ACTION
import com.android.SdkConstants.TAG_RECEIVER
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector.UastScanner
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.ResourceXmlDetector
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import org.jetbrains.uast.UReferenceExpression
import org.w3c.dom.Element
import java.util.EnumSet

/**
 * Checks for issues that negatively affect battery life
 */
class BatteryDetector : ResourceXmlDetector(), UastScanner {
    companion object Issues {
        private val IMPLEMENTATION = Implementation(
                BatteryDetector::class.java,
                EnumSet.of(Scope.MANIFEST, Scope.JAVA_FILE),
                Scope.MANIFEST_SCOPE,
                Scope.JAVA_FILE_SCOPE)

        /** Issues that negatively affect battery life  */
        @JvmField val ISSUE = Issue.create(
                "BatteryLife",
                "Battery Life Issues",

"""This issue flags code that either
* negatively affects battery life, or
* uses APIs that have recently changed behavior to prevent background tasks from consuming memory and battery excessively.

Generally, you should be using `JobScheduler` or `GcmNetworkManager` instead.

For more details on how to update your code, please seehttp://developer.android.com/preview/features/background-optimization.html""",

                Category.CORRECTNESS,
                5,
                Severity.WARNING,
                IMPLEMENTATION)
                .addMoreInfo("http://developer.android.com/preview/features/background-optimization.html")
    }

    override fun getApplicableElements(): Collection<String>? = listOf(TAG_ACTION)

    override fun visitElement(context: XmlContext, element: Element) {
        assert(element.tagName == TAG_ACTION)
        val attr = element.getAttributeNodeNS(ANDROID_URI, ATTR_NAME) ?: return
        val name = attr.value
        if ("android.net.conn.CONNECTIVITY_CHANGE" == name
                && element.parentNode != null
                && element.parentNode.parentNode != null
                && TAG_RECEIVER == element.parentNode.parentNode.nodeName
                && context.mainProject.targetSdkVersion.featureLevel >= 24) {
            val message = "Declaring a broadcastreceiver for " +
                    "`android.net.conn.CONNECTIVITY_CHANGE` is deprecated for apps targeting " +
                    "N and higher. In general, apps should not rely on this broadcast and " +
                    "instead use `JobScheduler` or `GCMNetworkManager`."
            context.report(ISSUE, element, context.getValueLocation(attr), message)
        }

        if ("android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" == name &&
                context.mainProject.targetSdkVersion.featureLevel >= 23) {
            context.report(ISSUE, element, context.getValueLocation(attr),
                    "Use of `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` violates the " +
                    "Play Store Content Policy regarding acceptable use cases, as described in " +
                    "http://developer.android.com/training/monitoring-device-state/doze-standby.html")
        }

        if ("android.hardware.action.NEW_PICTURE" == name
                || "android.hardware.action.NEW_VIDEO" == name
                || "com.android.camera.NEW_PICTURE" == name) {
            val message = "Use of `$name` is deprecated for all apps starting " +
                    "with the N release independent of the target SDK. Apps should not " +
                    "rely on these broadcasts and instead use `JobScheduler`"
            context.report(ISSUE, element, context.getValueLocation(attr), message)
        }
    }

    override fun getApplicableReferenceNames(): List<String>? =
            listOf("ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS")

    override fun visitReference(context: JavaContext,
                                reference: UReferenceExpression, resolved: PsiElement) {
        if (resolved is PsiField &&
                context.evaluator.isMemberInSubClassOf(resolved,
                        "android.provider.Settings", false)
                && context.getMainProject().targetSdkVersion.featureLevel >= 23) {
            context.report(ISSUE, reference, context.getNameLocation(reference),
                    "Use of `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` violates the " +
                    "Play Store Content Policy regarding acceptable use cases, as described in " +
                    "http://developer.android.com/training/monitoring-device-state/doze-standby.html")
        }
    }
}
