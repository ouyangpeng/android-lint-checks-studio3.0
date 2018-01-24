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

import com.android.builder.model.AndroidLibrary
import com.android.builder.model.JavaLibrary
import com.android.builder.model.Library
import com.android.tools.lint.checks.GradleDetector.getCompileDependencies
import com.android.tools.lint.detector.api.Project
import java.util.ArrayDeque

data class Coordinate(val group: String, val artifact: String) : Comparable<Coordinate> {
    override fun compareTo(other: Coordinate): Int {
        val delta = group.compareTo(other.group)
        if (delta != 0) {
            return delta
        }
        return artifact.compareTo(other.artifact)
    }
}

/**
 * This class finds blacklisted dependencies in a project by looking
 * transitively
 */
class BlacklistedDeps(val project: Project) {

    private var map: MutableMap<Coordinate,List<Library>>? = null

    init {
        val dependencies = getCompileDependencies(project)
        if (dependencies != null) {
            val stack = ArrayDeque<Library>()
            visitAndroidLibraries(stack, dependencies.libraries)
            visitJavaLibraries(stack, dependencies.javaLibraries)
        }
    }

    /**
     * Returns the path from this dependency to one of the blacklisted dependencies,
     * or null if this dependency is not blacklisted. If [remove] is true, the
     * dependency is removed from the map after this.
     */
    fun checkDependency(groupId: String, artifactId: String, remove: Boolean): List<Library>? {
        val map = this.map ?: return null
        val coordinate = Coordinate(groupId, artifactId)
        val path = map[coordinate] ?: return null
        if (remove) {
            map.remove(coordinate)
        }
        return path
    }

    /**
     * Returns all the dependencies found in this project that lead to a
     * blacklisted dependency. Each list is a list from the root dependency
     * to the blacklisted dependency.
     */
    fun getBlacklistedDependencies(): List<List<Library>> {
        val map = this.map ?: return emptyList()
        return map.values.toMutableList().sortedBy { it[0].resolvedCoordinates.groupId }
    }

    private fun visitAndroidLibraries(
            stack: ArrayDeque<Library>,
            libraries: Collection<AndroidLibrary>) {
        for (library in libraries) {
            visitAndroidLibrary(stack, library)
        }
    }

    private fun visitJavaLibraries(
            stack: ArrayDeque<Library>,
            libraries: Collection<JavaLibrary>) {
        for (library in libraries) {
            visitJavaLibrary(stack, library)
        }
    }

    private fun visitAndroidLibrary(stack: ArrayDeque<Library>, library: AndroidLibrary) {
        stack.addLast(library)
        checkLibrary(stack, library)
        visitAndroidLibraries(stack, library.libraryDependencies)
        visitJavaLibraries(stack, library.javaDependencies)
        stack.removeLast()
    }

    private fun visitJavaLibrary(stack: ArrayDeque<Library>, library: JavaLibrary) {
        stack.addLast(library)
        checkLibrary(stack, library)
        visitJavaLibraries(stack, library.dependencies)
        stack.removeLast()
    }

    private fun checkLibrary(stack: ArrayDeque<Library>, library: Library) {
        // Provided dependencies are fine
        if (library.isProvided/* && stack.size == 1*/) {
            return
        }

        val coordinates = library.resolvedCoordinates

        // This shouldn't be necessary according to the nullability annotations on this API,
        // but it's been observed in practice during IDE sessions, probably after failed syncs?
        // Playing it safe.
        @Suppress("NullChecksToSafeCall", "SENSELESS_COMPARISON")
        if (coordinates == null || coordinates.groupId == null || coordinates.artifactId == null) {
            return
        }

        if (isBlacklistedDependency(coordinates.groupId, coordinates.artifactId)) {
            if (map == null) {
                map = HashMap()
            }
            val root = stack.first.resolvedCoordinates
            map?.put(Coordinate(root.groupId, root.artifactId), ArrayList(stack))
        }
    }

    private fun isBlacklistedDependency(
            groupId: String,
            artifactId: String): Boolean {
        when (groupId) {
            // org.apache.http.*
            "org.apache.httpcomponents" -> return "httpclient" == artifactId

            // org.xmlpull.v1.*
            "xpp3" -> return "xpp3" == artifactId

            // org.apache.commons.logging
            "commons-logging" -> return "commons-logging" == artifactId

            // org.xml.sax.*, org.w3c.dom.*
            "xerces" -> return "xmlParserAPIs" == artifactId

            // org.json.*
            "org.json" -> return "json" == artifactId

            // javax.microedition.khronos.*
            "org.khronos" -> return "opengl-api" == artifactId

            // all of the above
            "com.google.android" -> return "android" == artifactId
        }

        return false
    }
}