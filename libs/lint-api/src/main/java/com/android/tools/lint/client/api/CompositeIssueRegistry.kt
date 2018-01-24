/*
 * Copyright (C) 2013 The Android Open Source Project
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

import com.android.tools.lint.detector.api.Issue

/**
 * Registry which merges many issue registries into one, and presents a unified list
 * of issues.
 *
 * **NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.**
 */
class CompositeIssueRegistry(
        private val registries: List<IssueRegistry>) : IssueRegistry() {

    private var issues: List<Issue>? = null

    override fun getIssues(): List<Issue> {
        val issues = this.issues
        if (issues != null) {
            return issues
        }

        var capacity = 0
        for (registry in registries) {
            capacity += registry.issues.size
        }
        val list = ArrayList<Issue>(capacity)
        for (registry in registries) {
            list.addAll(registry.issues)
        }
        this.issues = list
        return list
    }

    override fun isUpToDate(): Boolean {
        for (registry in registries) {
            if (!registry.isUpToDate) {
                return false
            }
        }

        return true
    }

    /** True if one or more java detectors were found that use the old Lombok-based API  */
    fun hasLombokLegacyDetectors(): Boolean {
        for (registry in registries) {
            if (registry is JarFileIssueRegistry && registry.hasLombokLegacyDetectors()) {
                return true
            } else if (registry is CompositeIssueRegistry && registry.hasLombokLegacyDetectors()) {
                return true
            }
        }

        return false
    }

    /** True if one or more java detectors were found that use the old PSI-based API */
    fun hasPsiLegacyDetectors(): Boolean {
        for (registry in registries) {
            if (registry is JarFileIssueRegistry && registry.hasPsiLegacyDetectors()) {
                return true
            } else if (registry is CompositeIssueRegistry && registry.hasPsiLegacyDetectors()) {
                return true
            }
        }
        return false
    }
}
