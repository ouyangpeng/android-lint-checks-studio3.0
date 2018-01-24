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

package com.android.tools.lint

import com.android.SdkConstants
import com.android.SdkConstants.VALUE_FALSE
import com.android.SdkConstants.VALUE_TRUE
import com.android.sdklib.AndroidTargetHash
import com.android.sdklib.AndroidTargetHash.PLATFORM_HASH_PREFIX
import com.android.sdklib.SdkVersionInfo
import com.android.tools.lint.checks.BuiltinIssueRegistry
import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.LintClient
import com.android.tools.lint.client.api.LintDriver
import com.android.tools.lint.client.api.LintRequest
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Project
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.TextFormat
import com.android.utils.XmlUtils.getFirstSubTag
import com.android.utils.XmlUtils.getNextTag
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import org.w3c.dom.Element
import java.io.File
import java.util.HashSet
import java.util.regex.Pattern

/** Regular expression used to match package statements with [ProjectInitializer.findPackage] */
private val PACKAGE_PATTERN = Pattern.compile("package\\s+([\\S&&[^;]]*)")
private const val TAG_PROJECT = "project"
private const val TAG_MODULE = "module"
private const val TAG_CLASSES = "classes"
private const val TAG_CLASSPATH = "classpath"
private const val TAG_SRC = "src"
private const val TAG_DEP = "dep"
private const val TAG_ROOT = "root"
private const val TAG_MANIFEST = "manifest"
private const val TAG_RESOURCE = "resource"
private const val TAG_AAR = "aar"
private const val TAG_SDK = "sdk"
private const val TAG_LINT_CHECKS = "lint-checks"
private const val TAG_BASELINE = "baseline"
private const val TAG_MERGED_MANIFEST = "merged-manifest"
private const val TAG_CACHE = "cache"
private const val ATTR_COMPILE_SDK_VERSION = "compile-sdk-version"
private const val ATTR_TEST = "test"
private const val ATTR_NAME = "name"
private const val ATTR_FILE = "file"
private const val ATTR_DIR = "dir"
private const val ATTR_JAR = "jar"
private const val ATTR_ANDROID = "android"
private const val ATTR_LIBRARY = "library"
private const val ATTR_MODULE = "module"

/**
 * Compute a list of lint [Project] instances from the given XML descriptor files.
 * Each descriptor is considered completely separate from the other (e.g. you can't
 * have library definitions in one referenced from another descriptor.
 */
fun computeMetadata(client: LintClient, descriptor: File): ProjectMetadata {
    val initializer = ProjectInitializer(client, descriptor,
            descriptor.parentFile ?: descriptor)
    return initializer.computeMetadata()
}

/**
 * Result data passed from parsing a project metadata XML file - returns the
 * set of projects, any SDK or cache directories configured within the file, etc
 */
data class ProjectMetadata(
        /** List of projects. Will be empty if there was an error in the configuration. */
        val projects: List<Project> = emptyList(),
        /** A baseline file to apply, if any */
        val baseline: File? = null,
        /** The SDK to use, if overriding the default */
        val sdk: File? = null,
        /** The cache directory to use, if overriding the default */
        val cache: File? = null,
        /** A map from module to a merged manifest for that module, if any */
        val mergedManifests: Map<Project, File?> = emptyMap(),
        /** A map from module to a baseline to apply to that module, if any */
        val moduleBaselines: Map<Project, File?> = emptyMap(),
        /** A map from module to a list of custom rule JAR files to apply, if any */
        val lintChecks: Map<Project, List<File>> = emptyMap())

/**
 * Class which handles initialization of a project hierarchy from a config XML file.
 *
 * Note: This code uses both the term "projects" and "modules". That's because
 * lint internally uses the term "project" for what Studio (and these XML config
 * files) refers to as a "module".
 *
 * @param client the lint handler
 * @param file the XML description file
 * @param root the root project directory (relative paths in the config file are considered
 *             relative to this directory)
 */
private class ProjectInitializer(val client: LintClient, val file: File,
        var root: File) {

    /** map from module name to module instance */
    private val modules = mutableMapOf<String, ManualProject>()

    /** list of global classpath jars to add to all modules */
    private val globalClasspath = mutableListOf<File>()

    /** map from module instance to names of modules it depends on */
    private val dependencies: Multimap<ManualProject, String> = ArrayListMultimap.create()

    /** map from module to the merged manifest to use, if any */
    private val mergedManifests = mutableMapOf<Project, File?>()

    /** map from module to a list of custom lint rules to apply for that module, if any */
    private val lintChecks = mutableMapOf<Project, List<File>>()

    /** map from module to a baseline to use for a given module, if any */
    private val baselines = mutableMapOf<Project, File?>()

    /** Compute a list of lint [Project] instances from the given XML descriptor */
    fun computeMetadata(): ProjectMetadata {
        assert(file.isFile) // should already have been enforced by the driver
        val document = client.xmlParser.parseXml(file)

        if (document == null || document.documentElement == null) {
            // Lint isn't quite up and running yet, so create a dummy context
            // in order to be able to report an error
            reportError("Failed to parse project descriptor $file")
            return ProjectMetadata()
        }

        return parseModules(document.documentElement)
    }

    /** Reports the given error message as an error to lint, with an optional element location */
    private fun reportError(message: String, element: Element? = null) {
        // To report an error using the lint infrastructure, we have to have
        // an associated "project", but there isn't an actual project yet
        // (that's what this whole class is attempting to compute and initialize).
        // Therefore, we'll make a dummy project. A project has to have an
        // associated directory, so we'll just randomly pick the folder containing
        // the project.xml folder itself. (In case it's not a full path, e.g.
        // just "project.xml", get the current directory instead.)
        val dir = file.parentFile ?: File("").absoluteFile
        val project = Project.create(client, dir, dir)
        val location = if (element != null)
            client.xmlParser.getLocation(file, element)
        else
            Location.create(file)
        val request = LintRequest(client, emptyList())
        val driver = LintDriver(BuiltinIssueRegistry(), client, request)
        val context = Context(driver, project, project, file, null)
        client.report(context, IssueRegistry.LINT_ERROR, IssueRegistry.LINT_ERROR.defaultSeverity,
                location, message, TextFormat.RAW, null)
    }

    private fun parseModules(projectElement: Element): ProjectMetadata {
        if (projectElement.tagName != TAG_PROJECT) {
            reportError("Expected <project> as the root tag", projectElement)
            return ProjectMetadata()
        }

        // First gather modules and sources, and collect dependency information.
        // The dependency information is captured into a separate map since dependencies
        // may refer to modules we have not encountered yet.
        var child = getFirstSubTag(projectElement)
        var sdk: File? = null
        var cache: File? = null
        var baseline: File? = null
        while (child != null) {
            val tag = child.tagName
            when (tag) {
                TAG_MODULE -> {
                    parseModule(child)
                }
                TAG_CLASSPATH -> {
                    globalClasspath.add(getFile(child, this.root))
                }
                TAG_SDK -> {
                    sdk = getFile(child, this.root)
                }
                TAG_BASELINE -> {
                    baseline = getFile(child, this.root)
                }
                TAG_CACHE -> {
                    cache = getFile(child, this.root)
                }
                TAG_ROOT -> {
                    val dir = File(child.getAttribute(ATTR_DIR))
                    if (dir.isDirectory) {
                        root = dir
                    } else {
                        reportError("$dir does not exist", child)
                    }
                }
                else -> {
                    reportError("Unexpected top level tag $tag in $file", child)
                }
            }

            child = getNextTag(child)
        }

        // Now that we have all modules recorded, process our dependency map
        // and add dependencies between the modules
        for ((module, dependencyName) in dependencies.entries()) {
            val to = modules[dependencyName]
            if (to != null) {
                module.addDirectDependency(to)
            } else {
                reportError("No module $dependencyName found (depended on by ${module.name}")
            }
        }

        val allModules = modules.values

        // Partition the projects up such that we only return projects that aren't
        // included by other projects (e.g. because they are library projects)
        val roots = HashSet(allModules)
        for (project in allModules) {
            roots.removeAll(project.allLibraries)
        }

        val sortedModules = roots.toMutableList()
        sortedModules.sortBy { it.name }

        // Initialize the classpath. We have a single massive jar for all
        // modules instead of individual folders...
        if (!globalClasspath.isEmpty()) {
            var useForAnalysis = true
            for (module in sortedModules) {
                if (module.getJavaLibraries(true).isEmpty()) {
                    module.setClasspath(globalClasspath, false)
                }
                if (module.javaClassFolders.isEmpty()) {
                    module.setClasspath(globalClasspath, useForAnalysis)
                    // Only allow the .class files in the classpath to be bytecode analyzed once;
                    // for the remainder we only use it for type resolution
                    useForAnalysis = false
                }
            }
        }

        return ProjectMetadata(
                projects = sortedModules,
                sdk = sdk,
                baseline = baseline,
                lintChecks = lintChecks,
                cache = cache,
                moduleBaselines = baselines,
                mergedManifests = mergedManifests)
    }

    private fun parseModule(moduleElement: Element) {
        val name: String = moduleElement.getAttribute(ATTR_NAME)
        val library = moduleElement.getAttribute(ATTR_LIBRARY) == VALUE_TRUE
        val android = moduleElement.getAttribute(ATTR_ANDROID) != VALUE_FALSE
        val buildApi: String = moduleElement.getAttribute(ATTR_COMPILE_SDK_VERSION)

        // Special case: if the module is a path (with an optional :suffix),
        // use this as the module directory, otherwise fall back to the default root
        val dir =
                if (name.startsWith(File.separator) && name.contains(":")) {
                    File(name.substring(0, name.indexOf(":")))
                } else {
                    root
                }
        val module = ManualProject(client, dir, name, library, android)
        modules.put(name, module)

        val sources = mutableListOf<File>()
        val testSources = mutableListOf<File>()
        val resources = mutableListOf<File>()
        val manifests = mutableListOf<File>()
        val classes = mutableListOf<File>()
        val classpath = mutableListOf<File>()
        val aars = mutableListOf<File>()
        val lintChecks = mutableListOf<File>()
        var baseline: File? = null
        var mergedManifest: File? = null

        var child = getFirstSubTag(moduleElement)
        while (child != null) {
            when (child.tagName) {
                TAG_MANIFEST -> {
                    manifests.add(getFile(child, dir))
                }
                TAG_MERGED_MANIFEST -> {
                    mergedManifest = getFile(child, dir)
                }
                TAG_SRC -> {
                    val file = getFile(child, dir)
                    when (child.getAttribute(ATTR_TEST) == VALUE_TRUE) {
                        false -> sources.add(file)
                        true -> testSources.add(file)
                    }
                }
                TAG_RESOURCE -> {
                    resources.add(getFile(child, dir))
                }
                TAG_CLASSES -> {
                    classes.add(getFile(child, dir))
                }
                TAG_CLASSPATH -> {
                    classpath.add(getFile(child, dir))
                }
                TAG_AAR -> {
                    aars.add(getFile(child, dir))
                }
                TAG_BASELINE -> {
                    baseline = getFile(child, dir)
                }
                TAG_LINT_CHECKS -> {
                    lintChecks.add(getFile(child, dir))
                }
                TAG_DEP -> {
                    val target = child.getAttribute(ATTR_MODULE)
                    if (target.isEmpty()) {
                        reportError("Invalid module dependency in ${module.name}", child)
                    }
                    dependencies.put(module, target)
                }
                else -> {
                    reportError("Unexpected tag ${child.tagName}", child)
                    return
                }
            }

            child = getNextTag(child)
        }

        // Compute source roots
        val sourceRoots = computeSourceRoots(sources)
        val testSourceRoots = computeTestSourceRoots(testSources, sourceRoots)

        val resourceRoots = mutableListOf<File>()
        if (!resources.isEmpty()) {
            // Compute resource roots. Note that these directories are not allowed
            // to overlap.
            for (file in resources) {
                val typeFolder = file.parentFile ?: continue
                val res = typeFolder.parentFile ?: continue
                if (!resourceRoots.contains(res)) {
                    resourceRoots.add(res)
                }
            }
        }

        module.setManifests(manifests)
        module.setResources(resourceRoots, resources)
        module.setTestSources(testSourceRoots, testSources)
        module.setSources(sourceRoots, sources)
        module.setClasspath(classes, true)
        module.setClasspath(classpath, false)

        module.setCompileSdkVersion(buildApi)

        this.lintChecks[module] = lintChecks
        this.mergedManifests[module] = mergedManifest
        this.baselines[module] = baseline
    }

    private fun computeTestSourceRoots(testSources: MutableList<File>,
            sourceRoots: MutableList<File>): List<File> {
        when {
            !testSources.isNotEmpty() -> return emptyList()
            else -> {
                val testSourceRoots = computeSourceRoots(testSources)

                // We don't allow the test sources and product sources to overlap
                for (root in testSourceRoots) {
                    if (sourceRoots.contains(root)) {
                        reportError("Tests cannot be in the same source root as production files; " +
                                "source root $root is also a test root")
                        break
                    }
                }

                testSourceRoots.removeAll(sourceRoots)
                return testSourceRoots
            }
        }
    }

    private fun computeSourceRoots(sources: List<File>): MutableList<File> {
        val sourceRoots = mutableListOf<File>()
        if (!sources.isEmpty()) {
            // Cache for each directory since computing root for a source file is
            // expensive
            val dirToRootCache = mutableMapOf<String, File>()
            for (file in sources) {
                val parent = file.parentFile ?: continue
                val found = dirToRootCache[parent.path]
                if (found != null) {
                    continue
                }

                // Find the source root for a file. There are several scenarios.
                // Let's say the original file path is "/a/b/c/d", and findRoot
                // returns "/a/b" - in that case, great, we'll use it.
                //
                // But let's say the file is in the default package; in that case
                // findRoot returns null. Let's say the file we passed in was
                // "src/Foo.java". In that case, we can just take its parent
                // file, "src", as the source root.
                // But what if the source file itself was just passed as "Foo.java" ?
                // In that case it is relative to the pwd, so we get the *absolute*
                // path of the file instead, and take its parent path.
                val root = findRoot(file) ?: file.parentFile ?: file.absoluteFile.parentFile ?:
                        continue

                dirToRootCache.put(parent.path, root)

                if (!sourceRoots.contains(root)) {
                    sourceRoots.add(root)
                }
            }
        }
        return sourceRoots
    }

    /**
     * Given an element that is expected to have a "file" attribute (or "dir" or "jar"),
     * produces a full path to the file
     */
    private fun getFile(child: Element, dir: File): File {
        var path = child.getAttribute(ATTR_FILE)
        if (path.isEmpty()) {
            path = child.getAttribute(ATTR_DIR)
            if (path.isEmpty()) {
                path = child.getAttribute(ATTR_JAR)
            }
        }
        var source = File(path)
        if (!source.isAbsolute && !source.exists()) {
            source = File(dir, path)
            if (!source.exists()) {
                source = File(root, path)
            }
        }

        if (!source.exists()) {
            reportError("$path ${if (!File(path).isAbsolute) "(relative to " +
                    "${dir.canonicalPath}) " else ""}does not exist", child)
        }
        return source
    }

    /**
     * If given a full path to a Java or Kotlin source file, produces the path to
     * the source root if possible.
     */
    private fun findRoot(file: File): File? {
        val path = file.path
        if (path.endsWith(SdkConstants.DOT_JAVA) || path.endsWith(SdkConstants.DOT_KT)) {
            val pkg = findPackage(file) ?: return null
            val parent = file.parentFile ?: return null
            return File(path.substring(0, parent.path.length - pkg.length))
        }

        return null
    }

    /** Finds the package of the given Java/Kotlin source file, if possible */
    private fun findPackage(file: File): String? {
        val source = client.readFile(file)
        val matcher = PACKAGE_PATTERN.matcher(source)
        val foundPackage = matcher.find()
        return if (foundPackage) {
            matcher.group(1).trim { it <= ' ' }
        } else {
            null
        }
    }
}

/**
 * A special subclass of lint's [Project] class which can be manually configured
 * with custom source locations, custom library types, etc.
 */
private class ManualProject
constructor(client: LintClient, dir: File, name: String, library: Boolean,
        private var android: Boolean) : Project(client, dir, dir) {

    init {
        setName(name)
        directLibraries = mutableListOf()
        this.library = library
        // We don't have this info yet; add support to the XML to specify it
        this.buildSdk = SdkVersionInfo.HIGHEST_KNOWN_STABLE_API
        this.mergeManifests = true
    }

    /** Adds the given project as a dependency from this project */
    fun addDirectDependency(project: ManualProject) {
        directLibraries.add(project)
    }

    override fun isAndroidProject(): Boolean = android

    override fun isGradleProject(): Boolean = false

    override fun toString(): String = "Project [name=$name]"

    override fun equals(other: Any?): Boolean {
        // Normally Project.equals checks directory equality, but we can't
        // do that here since we don't have guarantees that the directories
        // won't overlap (and furthermore we don't actually have the directory
        // locations of each module)
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val project = other as Project?
        return name == project!!.name
    }

    override fun hashCode(): Int = name.hashCode()

    /** Sets the given files as the manifests applicable for this module */
    fun setManifests(manifests: List<File>) {
        this.manifestFiles = manifests
        addFilteredFiles(manifests)
    }

    /** Sets the given resource files and their roots for this module */
    fun setResources(resourceRoots: List<File>, resources: List<File>) {
        this.resourceFolders = resourceRoots
        addFilteredFiles(resources)
    }

    /** Sets the given source files and their roots for this module */
    fun setSources(sourceRoots: List<File>, sources: List<File>) {
        this.javaSourceFolders = sourceRoots
        addFilteredFiles(sources)
    }

    /** Sets the given source files and their roots for this module */
    fun setTestSources(sourceRoots: List<File>, sources: List<File>) {
        this.testSourceFolders = sourceRoots
        addFilteredFiles(sources)
    }

    /**
     * Adds the given files to the set of filtered files for this project. With
     * a filter applied, lint won't look at all sources in for example the source
     * or resource roots, it will limit itself to these specific files.
     */
    private fun addFilteredFiles(sources: List<File>) {
        if (files == null) {
            files = mutableListOf()
        }
        files.addAll(sources)
    }

    /** Sets the global class path for this module */
    fun setClasspath(allClasses: List<File>, useForAnalysis: Boolean) =
            if (useForAnalysis) {
                this.javaClassFolders = allClasses
            } else {
                this.javaLibraries = allClasses
            }

    fun setCompileSdkVersion(buildApi: String) {
        if (!buildApi.isEmpty()) {
            buildTargetHash = if (Character.isDigit(buildApi[0]))
                PLATFORM_HASH_PREFIX + buildApi
            else
                buildApi
            val version = AndroidTargetHash.getPlatformVersion(buildApi)
            if (version != null) {
                buildSdk = version.featureLevel
            } else {
                client.log(Severity.WARNING, null,
                        "Unexpected build target format: %1\$s", buildApi)
            }
        }
    }
}
