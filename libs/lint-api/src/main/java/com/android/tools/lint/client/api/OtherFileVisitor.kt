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

import com.android.SdkConstants.ANDROID_MANIFEST_XML
import com.android.SdkConstants.DOT_CLASS
import com.android.SdkConstants.DOT_JAVA
import com.android.SdkConstants.DOT_XML
import com.android.SdkConstants.FD_ASSETS
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Detector.OtherFileScanner
import com.android.tools.lint.detector.api.Project
import com.android.tools.lint.detector.api.Scope
import com.android.utils.SdkUtils
import com.google.common.collect.Lists
import java.io.File
import java.util.ArrayList
import java.util.EnumMap
import java.util.EnumSet

/**
 * Visitor for "other" files: files that aren't java sources,
 * XML sources, etc -- or which should have custom handling in some
 * other way.
 */
internal class OtherFileVisitor(private val detectors: List<Detector>) {

    private val files = EnumMap<Scope, List<File>>(Scope::class.java)

    /** Analyze other files in the given project  */
    fun scan(
            driver: LintDriver,
            project: Project,
            main: Project?) {
        // Collect all project files
        val projectFolder = project.dir

        var scopes = EnumSet.noneOf(Scope::class.java)
        for (detector in detectors) {
            val fileScanner = detector as OtherFileScanner
            val applicable = fileScanner.applicableFiles
            if (applicable.contains(Scope.OTHER)) {
                scopes = Scope.ALL
                break
            }
            scopes.addAll(applicable)
        }

        val subset = project.subset

        if (scopes.contains(Scope.RESOURCE_FILE)) {
            if (subset != null && !subset.isEmpty()) {
                val files = ArrayList<File>(subset.size)
                for (file in subset) {
                    if (SdkUtils.endsWith(file.path, DOT_XML) && file.name != ANDROID_MANIFEST_XML) {
                        files.add(file)
                    }
                }
                if (!files.isEmpty()) {
                    this.files.put(Scope.RESOURCE_FILE, files)
                }
            } else {
                val files = Lists.newArrayListWithExpectedSize<File>(100)
                for (res in project.resourceFolders) {
                    collectFiles(files, res)
                }
                val assets = File(projectFolder, FD_ASSETS)
                if (assets.exists()) {
                    collectFiles(files, assets)
                }
                if (!files.isEmpty()) {
                    this.files.put(Scope.RESOURCE_FILE, files)
                }
            }
        }

        if (scopes.contains(Scope.JAVA_FILE)) {
            if (subset != null && !subset.isEmpty()) {
                val files = ArrayList<File>(subset.size)
                for (file in subset) {
                    if (file.path.endsWith(DOT_JAVA)) {
                        files.add(file)
                    }
                }
                if (!files.isEmpty()) {
                    this.files.put(Scope.JAVA_FILE, files)
                }
            } else {
                val files = Lists.newArrayListWithExpectedSize<File>(100)
                for (srcFolder in project.javaSourceFolders) {
                    collectFiles(files, srcFolder)
                }
                if (!files.isEmpty()) {
                    this.files.put(Scope.JAVA_FILE, files)
                }
            }
        }

        if (scopes.contains(Scope.CLASS_FILE)) {
            if (subset != null && !subset.isEmpty()) {
                val files = ArrayList<File>(subset.size)
                for (file in subset) {
                    if (file.path.endsWith(DOT_CLASS)) {
                        files.add(file)
                    }
                }
                if (!files.isEmpty()) {
                    this.files.put(Scope.CLASS_FILE, files)
                }
            } else {
                val files = Lists.newArrayListWithExpectedSize<File>(100)
                for (classFolder in project.javaClassFolders) {
                    collectFiles(files, classFolder)
                }
                if (!files.isEmpty()) {
                    this.files.put(Scope.CLASS_FILE, files)
                }
            }
        }

        if (scopes.contains(Scope.MANIFEST)) {
            if (subset != null && !subset.isEmpty()) {
                val files = ArrayList<File>(subset.size)
                for (file in subset) {
                    if (file.name == ANDROID_MANIFEST_XML) {
                        files.add(file)
                    }
                }
                if (!files.isEmpty()) {
                    this.files.put(Scope.MANIFEST, files)
                }
            } else {
                val manifestFiles = project.manifestFiles
                if (manifestFiles != null) {
                    files.put(Scope.MANIFEST, manifestFiles)
                }
            }
        }

        for ((scope, files) in files) {
            val applicable = ArrayList<Detector>(detectors.size)
            for (detector in detectors) {
                val fileScanner = detector as OtherFileScanner
                val appliesTo = fileScanner.applicableFiles
                if (appliesTo.contains(Scope.OTHER) || appliesTo.contains(scope)) {
                    applicable.add(detector)
                }
            }
            if (!applicable.isEmpty()) {
                for (file in files) {
                    val context = Context(driver, project, main, file)
                    for (detector in applicable) {
                        detector.beforeCheckFile(context)
                        detector.run(context)
                        detector.afterCheckFile(context)
                    }
                    if (driver.isCanceled) {
                        return
                    }
                }
            }
        }
    }

    private fun collectFiles(files: MutableList<File>, file: File) {
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children != null) {
                for (child in children) {
                    collectFiles(files, child)
                }
            }
        } else {
            files.add(file)
        }
    }
}
