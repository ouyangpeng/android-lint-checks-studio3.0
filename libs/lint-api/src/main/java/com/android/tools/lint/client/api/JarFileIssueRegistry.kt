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
// This class deliberately refers to deprecated APIs like Lombok and PSI
@file:Suppress("DEPRECATION")

package com.android.tools.lint.client.api

import com.android.SdkConstants.DOT_CLASS
import com.android.tools.lint.detector.api.Detector.JavaPsiScanner
import com.android.tools.lint.detector.api.Detector.JavaScanner
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.utils.SdkUtils
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.lang.ref.SoftReference
import java.net.URLClassLoader
import java.util.HashMap
import java.util.jar.Attributes
import java.util.jar.JarFile

/**
 *  An [IssueRegistry] for a custom lint rule jar file. The rule jar should provide a
 * manifest entry with the key `Lint-Registry` and the value of the fully qualified name of an
 * implementation of [IssueRegistry] (with a default constructor).
 *
 *  NOTE: The custom issue registry should not extend this file; it should be a plain
 * IssueRegistry! This file is used internally to wrap the given issue registry.
 */
class JarFileIssueRegistry
private constructor(
        client: LintClient,
        /** The jar file the rules were loaded from */
        val jarFile: File,
        /** The custom lint check's issue registry that this [JarFileIssueRegistry] wraps */
        registry: IssueRegistry) : IssueRegistry() {

    private val issues: List<Issue> = registry.issues.toList()
    private var timestamp: Long = jarFile.lastModified()

    private var hasLombokLegacyDetectors: Boolean = false
    private var hasPsiLegacyDetectors: Boolean = false

    /** True if one or more java detectors were found that use the old Lombok-based API  */
    fun hasLombokLegacyDetectors(): Boolean {
        return hasLombokLegacyDetectors
    }

    /** True if one or more java detectors were found that use the old PSI-based API  */
    fun hasPsiLegacyDetectors(): Boolean {
        return hasPsiLegacyDetectors
    }

    override fun isUpToDate(): Boolean {
        return timestamp == jarFile.lastModified()
    }

    init {
        // If it's an old registry, look through the issues to see if it
        // provides Java scanning and if so create the old style visitors
        for (issue in issues) {
            val scope = issue.implementation.scope
            if (scope.contains(Scope.JAVA_FILE) || scope.contains(Scope.JAVA_LIBRARIES)
                    || scope.contains(Scope.ALL_JAVA_FILES)) {
                val detectorClass = issue.implementation.detectorClass
                if (JavaScanner::class.java.isAssignableFrom(detectorClass)) {
                    hasLombokLegacyDetectors = true
                } else if (JavaPsiScanner::class.java.isAssignableFrom(detectorClass)) {
                    hasPsiLegacyDetectors = true
                }
                break
            }
        }

        val loader = registry.javaClass.classLoader
        if (loader is URLClassLoader) {
            loadAndCloseURLClassLoader(client, jarFile, loader)
        }
    }

    override fun getIssues(): List<Issue> {
        return issues
    }

    companion object Factory {
        /** Service key for automatic discovery of lint rules */
        private const val SERVICE_KEY =
                "META-INF/services/com.android.tools.lint.client.api.IssueRegistry"

        /**
         * Manifest constant for declaring an issue provider.
         *
         * Example: Lint-Registry-v2: foo.bar.CustomIssueRegistry
         */
        private const val MF_LINT_REGISTRY = "Lint-Registry-v2"

        /** Older key: these are for older custom rules */
        private const val MF_LINT_REGISTRY_OLD = "Lint-Registry"

        /** Cache of custom lint check issue registries */
        private var cache: MutableMap<File, SoftReference<JarFileIssueRegistry>>? = null

        /**
         * Loads custom rules from the given list of jar files and returns a list
         * of [JarFileIssueRegistry} instances.
         *
         * It will also deduplicate issue registries, since in Gradle projects with
         * local lint.jar's it's possible for the same lint.jar to be handed back
         * multiple times with different paths through various separate dependencies.
         */
        fun get(client: LintClient,  jarFiles: Collection<File>): List<JarFileIssueRegistry> {
            val registryMap = try {
                findRegistries(client, jarFiles)
            } catch (e: IOException) {
                client.log(e, "Could not load custom lint check jar files: ${e.message}")
                return emptyList()
            }

            if (registryMap.isEmpty()) {
                return emptyList()
            }

            val capacity = jarFiles.size + 1
            val registries = ArrayList<JarFileIssueRegistry>(capacity)

            for ((registryClass, jarFile) in registryMap) {
                try {
                    val registry = get(client, registryClass, jarFile) ?: continue
                    registries.add(registry)
                } catch (e: Throwable) {
                    client.log(e, "Could not load custom lint check jar file %1\$s", jarFile)
                }
            }

            return registries
        }

        /**
         * Returns a [JarFileIssueRegistry] for the given issue registry class name
         * and jar file, with caching
         */
        private fun get(client: LintClient, registryClassName: String, jarFile: File):
                JarFileIssueRegistry? {
            if (cache == null) {
                cache = HashMap()
            } else {
                val reference = cache!![jarFile]
                if (reference != null) {
                    val registry = reference.get()
                    if (registry != null && registry.isUpToDate) {
                        return registry
                    }
                }
            }

            // Ensure that the scope-to-detector map doesn't return stale results
            IssueRegistry.reset()

            val userRegistry = loadIssueRegistry(client, jarFile, registryClassName)
            return if (userRegistry != null) {
                val jarIssueRegistry = JarFileIssueRegistry(client, jarFile, userRegistry)
                cache!!.put(jarFile, SoftReference(jarIssueRegistry))
                jarIssueRegistry
            } else {
                null
            }
        }

        /** Combine one or more issue registries into a single one */
        fun join(vararg registries: IssueRegistry): IssueRegistry {
            return if (registries.size == 1) {
                registries[0]
            } else {
                CompositeIssueRegistry(registries.toList())
            }
        }

        /**
         * Given a jar file, create a class loader for it and instantiate
         * the named issue registry.
         *
         * TODO: Add a custom class loader architecture here such that
         * custom rules can have dependent jars without needing to jar-jar them!
         */
        private fun loadIssueRegistry(
                client: LintClient,
                jarFile: File,
                className: String): IssueRegistry? {
            // Make a class loader for this jar
            val url = SdkUtils.fileToUrl(jarFile)
            return try {
                val loader = client.createUrlClassLoader(arrayOf(url),
                        JarFileIssueRegistry::class.java.classLoader)
                val registryClass = Class.forName(className, true, loader)
                registryClass.newInstance() as IssueRegistry
            } catch (e: Throwable) {
                client.log(e, "Could not load custom lint check jar file %1\$s", jarFile)
                null
            }
        }

        /**
         * Returns a map from issue registry qualified name to the corresponding jar file
         * that contains it
         */
        private fun findRegistries(
                client: LintClient,
                jarFiles: Collection<File>): Map<String,File> {
            val registryClassToJarFile = HashMap<String,File>()
            for (jarFile in jarFiles) {
                JarFile(jarFile).use { file ->
                    val manifest = file.manifest
                    val attrs = manifest.mainAttributes
                    var attribute: Any? = attrs[Attributes.Name(MF_LINT_REGISTRY)]
                    var isLegacy = false
                    if (attribute == null) {
                        attribute = attrs[Attributes.Name(MF_LINT_REGISTRY_OLD)]
                        // It's an old rule. We don't yet conclude that
                        //   hasLombokLegacyDetectors=true
                        // because the lint checks may not be Java related.
                        if (attribute != null) {
                            isLegacy = true
                        }
                    }
                    if (attribute is String) {
                        val className = attribute

                        // Store class name -- but it may not be unique (there could be
                        // multiple separate jar files pointing to the same issue registry
                        // (due to the way local lint.jar files propagate via project
                        // dependencies) so only store this file if it hasn't already
                        // been found, or if it's a v2 version (e.g. not legacy)
                        if (!isLegacy || registryClassToJarFile[className] == null) {
                            registryClassToJarFile[className] = jarFile
                        }
                    } else {
                        // Load service keys. We're reading it manually instead of using
                        // ServiceLoader because we don't want to put these jars into
                        // the class loaders yet (since there can be many duplicates
                        // when a library is available through multiple dependencies)
                        val services = file.getJarEntry(SERVICE_KEY)
                        if (services != null) {
                            file.getInputStream(services).use {
                                val reader = InputStreamReader(it, Charsets.UTF_8)
                                reader.useLines {
                                    for (line in it) {
                                        val comment = line.indexOf("#")
                                        val className = if (comment >= 0) {
                                            line.substring(0, comment).trim()
                                        } else {
                                            line.trim()
                                        }
                                        if (!className.isEmpty() &&
                                                registryClassToJarFile[className] == null) {
                                            registryClassToJarFile[className] = jarFile
                                        }
                                    }
                                }
                            }
                            return registryClassToJarFile
                        }

                        client.log(Severity.ERROR, null,
                                "Custom lint rule jar %1\$s does not contain a valid " +
                                "registry manifest key (%2\$s).\n" +
                                "Either the custom jar is invalid, or it uses an outdated " +
                                "API not supported this lint client",
                                jarFile.path, MF_LINT_REGISTRY)
                    }
                }
            }

            return registryClassToJarFile
        }

        /**
         * Work around http://bugs.java.com/bugdatabase/view_bug.do?bug_id=5041014 :
         * URLClassLoader, on Windows, locks the .jar file forever.
         * As of Java 7, there's a workaround: you can call close() when you're "done"
         * with the file. We'll do that here. However, the whole point of the
         * [JarFileIssueRegistry] is that when lint is run over and over again
         * as the user is editing in the IDE and we're background checking the code, we
         * don't to keep loading the custom view classes over and over again: we want to
         * cache them. Therefore, just closing the URLClassLoader right away isn't great
         * either. However, it turns out it's safe to close the URLClassLoader once you've
         * loaded the classes you need, since the URLClassLoader will continue to serve
         * those classes even after its close() methods has been called.
         *
         * Therefore, if we can call close() on this URLClassLoader, we'll proactively load
         * all class files we find in the .jar file, then close it.
         *
         * @param client the client to report errors to
         * @param file the .jar file
         * @param loader the URLClassLoader we should close
         */
        private fun loadAndCloseURLClassLoader(
                client: LintClient,
                file: File,
                loader: URLClassLoader) {
            // Before closing the jar file, proactively load all classes:
            try {
                JarFile(file).use { jar ->
                    val enumeration = jar.entries()
                    while (enumeration.hasMoreElements()) {
                        val entry = enumeration.nextElement()
                        var name = entry.name
                        // Load non-inner-classes
                        if (name.endsWith(DOT_CLASS)) {
                            // Strip .class suffix and change .jar file path (/)
                            // to class name (.'s).
                            name = name.substring(0, name.length - DOT_CLASS.length)
                            name = name.replace('/', '.')
                            try {
                                val aClass = Class.forName(name, true, loader)
                                // Actually, initialize them too to make sure basic classes
                                // needed by the detector are available
                                aClass.newInstance()
                            } catch (e: Throwable) {
                                client.log(Severity.ERROR, e,
                                        "Failed to prefetch $name from $file")
                            }

                        }
                    }
                }
            } catch (ignore: Throwable) {
            } finally {
                // Finally close the URL class loader
                try {
                    loader.close()
                } catch (ignore: Throwable) {
                    // Couldn't close. This is unlikely.
                }
            }
        }
    }
}
