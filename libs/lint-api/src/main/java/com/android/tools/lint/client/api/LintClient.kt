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

import com.android.SdkConstants
import com.android.SdkConstants.CLASS_FOLDER
import com.android.SdkConstants.DOT_AAR
import com.android.SdkConstants.DOT_JAR
import com.android.SdkConstants.DOT_XML
import com.android.SdkConstants.FD_ASSETS
import com.android.SdkConstants.FN_BUILD_GRADLE
import com.android.SdkConstants.GEN_FOLDER
import com.android.SdkConstants.LIBS_FOLDER
import com.android.SdkConstants.RES_FOLDER
import com.android.SdkConstants.SRC_FOLDER
import com.android.builder.model.AndroidLibrary
import com.android.ide.common.repository.ResourceVisibilityLookup
import com.android.ide.common.res2.AbstractResourceRepository
import com.android.ide.common.res2.ResourceItem
import com.android.manifmerger.Actions
import com.android.prefs.AndroidLocation
import com.android.repository.api.ProgressIndicator
import com.android.repository.api.ProgressIndicatorAdapter
import com.android.sdklib.BuildToolInfo
import com.android.sdklib.IAndroidTarget
import com.android.sdklib.SdkVersionInfo
import com.android.sdklib.repository.AndroidSdkHandler
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.LintUtils
import com.android.tools.lint.detector.api.LintUtils.endsWith
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Project
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.TextFormat
import com.android.utils.CharSequences
import com.android.utils.Pair
import com.android.utils.XmlUtils
import com.google.common.annotations.Beta
import com.google.common.base.Charsets.UTF_8
import com.google.common.base.Splitter
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import com.google.common.io.Files
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLClassLoader
import java.net.URLConnection
import java.util.ArrayList
import java.util.HashMap

/**
 * Information about the tool embedding the lint analyzer. IDEs and other tools
 * implementing lint support will extend this to integrate logging, displaying errors,
 * etc.
 *
 * **NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.**
 */
@Beta
abstract class LintClient {

    protected constructor(clientName: String) {
        LintClient.clientName = clientName
    }

    protected constructor() {

        clientName = "unknown"
    }

    /**
     * Returns a configuration for use by the given project. The configuration
     * provides information about which issues are enabled, any customizations
     * to the severity of an issue, etc.
     *
     *
     * By default this method returns a [DefaultConfiguration].
     *
     * @param project the project to obtain a configuration for
     *
     * @param driver the current driver, if any
     *
     * @return a configuration, never null.
     */
    open fun getConfiguration(project: Project, driver: LintDriver?): Configuration =
            DefaultConfiguration.create(this, project, null)

    /**
     * Report the given issue. This method will only be called if the configuration
     * provided by [.getConfiguration] has reported the corresponding
     * issue as enabled and has not filtered out the issue with its
     * [Configuration.ignore] method.
     *
     *
     *
     * @param context  the context used by the detector when the issue was found
     *
     * @param issue    the issue that was found
     *
     * @param severity the severity of the issue
     *
     * @param location the location of the issue
     *
     * @param message  the associated user message
     *
     * @param format   the format of the description and location descriptions
     *
     * @param fix      an optional set of extra data provided by the detector for this issue; this
     *                 is intended to pass metadata to the IDE to help construct quickfixes without
     *                 having to parse error messages (which is brittle) or worse having to include
     *                 information in the error message (for later parsing) which is required by the
     *                 quickfix but not really helpful in the error message itself (such as the
     *                 maxVersion for a permission tag to be added to the
     */
    abstract fun report(
            context: Context,
            issue: Issue,
            severity: Severity,
            location: Location,
            message: String,
            format: TextFormat,
            fix: LintFix?)

    /**
     * Send an exception or error message (with warning severity) to the log
     *
     * @param exception the exception, possibly null
     *
     * @param format the error message using [java.lang.String.format] syntax, possibly null
     *    (though in that case the exception should not be null)
     *
     * @param args any arguments for the format string
     */
    open fun log(
            exception: Throwable?,
            format: String?,
            vararg args: Any) = log(Severity.WARNING, exception, format, *args)

    /**
     * Send an exception or error message to the log
     *
     * @param severity the severity of the warning
     *
     * @param exception the exception, possibly null
     *
     * @param format the error message using [java.lang.String.format] syntax, possibly null
     *    (though in that case the exception should not be null)
     *
     * @param args any arguments for the format string
     */
    abstract fun log(
            severity: Severity,
            exception: Throwable?,
            format: String?,
            vararg args: Any)

    /**
     * Returns a [XmlParser] to use to parse XML
     *
     * @return a new [XmlParser], or null if this client does not support
     *         XML analysis
     */
    abstract val xmlParser: XmlParser

    /**
     * Returns a [JavaParser] to use to parse Java
     *
     * @param project the project to parse, if known (this can be used to look up
     *                the class path for type attribution etc, and it can also be used
     *                to more efficiently process a set of files, for example to
     *                perform type attribution for multiple units in a single pass)
     *
     * @return a new [JavaParser], or null if this client does not
     *         support Java analysis
     */
    abstract fun getJavaParser(project: Project?): JavaParser?

    /**
     * Returns a [JavaParser] to use to parse Java
     *
     * @param project the project to parse, if known (this can be used to look up
     *                the class path for type attribution etc, and it can also be used
     *                to more efficiently process a set of files, for example to
     *                perform type attribution for multiple units in a single pass)
     *
     * @return a new [JavaParser], or null if this client does not
     *         support Java analysis
     */
    abstract fun getUastParser(project: Project?): UastParser?

    /**
     * Returns an optimal detector, if applicable. By default, just returns the
     * original detector, but tools can replace detectors using this hook with a version
     * that takes advantage of native capabilities of the tool.
     *
     * @param detectorClass the class of the detector to be replaced
     *
     * @return the new detector class, or just the original detector (not null)
     */
    open fun replaceDetector(
            detectorClass: Class<out Detector>): Class<out Detector> = detectorClass

    /**
     * Reads the given text file and returns the content as a string
     *
     * @param file the file to read
     *
     * @return the string to return, never null (will be empty if there is an
     *         I/O error)
     */
    abstract fun readFile(file: File): CharSequence

    /**
     * Reads the given binary file and returns the content as a byte array.
     * By default this method will read the bytes from the file directly,
     * but this can be customized by a client if for example I/O could be
     * held in memory and not flushed to disk yet.
     *
     * @param file the file to read
     *
     * @return the bytes in the file, never null
     *
     * @throws IOException if the file does not exist, or if the file cannot be
     *             read for some reason
     */
    @Throws(IOException::class)
    open fun readBytes(file: File): ByteArray = Files.toByteArray(file)

    /**
     * Returns the list of source folders for Java source files
     *
     * @param project the project to look up Java source file locations for
     *
     * @return a list of source folders to search for .java files
     */
    open fun getJavaSourceFolders(project: Project): List<File> =
            getClassPath(project).sourceFolders

    /**
     * Returns the list of generated source folders
     *
     * @param project the project to look up generated source file locations for
     *
     * @return a list of generated source folders to search for source files
     */
    open fun getGeneratedSourceFolders(project: Project): List<File> =
            getClassPath(project).generatedFolders

    /**
     * Returns the list of output folders for class files
     * @param project the project to look up class file locations for
     *
     * @return a list of output folders to search for .class files
     */
    open fun getJavaClassFolders(project: Project): List<File> = getClassPath(project).classFolders

    /**
     * Returns the list of Java libraries
     *
     * @param project         the project to look up jar dependencies for
     *
     * @param includeProvided If true, included provided libraries too (libraries that are not
     *                        packaged with the app, but are provided for compilation purposes and
     *                        are assumed to be present in the running environment)
     *
     * @return a list of jar dependencies containing .class files
     */
    open fun getJavaLibraries(project: Project, includeProvided: Boolean): List<File> =
            getClassPath(project).getLibraries(includeProvided)

    /**
     * Returns the list of source folders for test source files
     *
     * @param project the project to look up test source file locations for
     *
     * @return a list of source folders to search for .java files
     */
    open fun getTestSourceFolders(project: Project): List<File> =
            getClassPath(project).testSourceFolders

    /**
     * Returns the list of libraries needed to compile the test source files
     *
     * @param project the project to look up test source file locations for
     *
     * @return a list of jar files to add to the regular project dependencies when compiling the
     * test sources
     */
    open fun getTestLibraries(project: Project): List<File> =
            getClassPath(project).testLibraries

    /**
     * Returns the resource folders.
     *
     * @param project the project to look up the resource folder for
     *
     * @return a list of files pointing to the resource folders, possibly empty
     */
    open fun getResourceFolders(project: Project): List<File> {
        val res = File(project.dir, RES_FOLDER)
        if (res.exists()) {
            return listOf(res)
        }

        return emptyList()
    }

    /**
     * Returns the asset folders.
     *
     * @param project the project to look up the asset folder for
     *
     * @return a list of files pointing to the asset folders, possibly empty
     */
    open fun getAssetFolders(project: Project): List<File> {
        val assets = File(project.dir, FD_ASSETS)
        if (assets.exists()) {
            return listOf(assets)
        }

        return emptyList()
    }

    /**
     * Returns the [SdkInfo] to use for the given project.
     *
     * @param project the project to look up an [SdkInfo] for
     *
     * @return an [SdkInfo] for the project
     */
    open fun getSdkInfo(project: Project): SdkInfo = // By default no per-platform SDK info
            DefaultSdkInfo()

    /**
     * Returns a suitable location for storing cache files. Note that the
     * directory may not exist. You can override the default location
     * using `$ANDROID_SDK_CACHE_DIR` (though note that specific
     * lint integrations may not honor that environment variable; for example,
     * in Gradle the cache directory will **always** be build/intermediates/lint-cache/.)
     *
     * @param create if true, attempt to create the cache dir if it does not
     *            exist
     *
     * @return a suitable location for storing cache files, which may be null if
     *         the create flag was false, or if for some reason the directory
     *         could not be created
     *
     */
    @Deprecated("Use {@link #getCacheDir(String, boolean)} instead",
            ReplaceWith("getCacheDir(null, create)"))
    open fun getCacheDir(create: Boolean): File? = getCacheDir(null, create)

    /**
     * Returns a suitable location for storing cache files of a given named
     * type. The named type is typically created as a directory within the shared
     * cache directory. For example, from the command line, lint will typically store
     * its cache files in ~/.android/cache/. In order to avoid files colliding,
     * caches that create a lot of files should provide a specific name such that
     * the cache is isolated to a sub directory.
     *
     *
     * Note that in some cases lint may interpret that name to provide an alternate
     * cache. For example, when lint runs in the IDE it normally uses the same
     * cache as lint on the command line (~/.android/cache), but specifically for
     * the cache for maven.google.com repository versions, it will instead point to
     * the same cache directory as the IDE is already using for non-lint purposes,
     * in order to share data that may already exist there.
     *
     *
     * Note that the cache directory may not exist. You can override the default location
     * using `$ANDROID_SDK_CACHE_DIR` (though note that specific
     * lint integrations may not honor that environment variable; for example,
     * in Gradle the cache directory will **always** be build/intermediates/lint-cache/.)
     *
     * @param create if true, attempt to create the cache dir if it does not
     *            exist
     *
     * @return a suitable location for storing cache files, which may be null if
     *         the create flag was false, or if for some reason the directory
     *         could not be created
     */
    open fun getCacheDir(name: String?, create: Boolean): File? {
        var path: String? = System.getenv("ANDROID_SDK_CACHE_DIR")
        if (path != null) {
            if (name != null) {
                path += File.separator + name
            }
            val dir = File(path)
            if (create && !dir.exists()) {
                if (!dir.mkdirs()) {
                    return null
                }
            }
            return dir
        }

        val home = System.getProperty("user.home")
        var relative = ".android" + File.separator + "cache"
        if (name != null) {
            relative += File.separator + name
        }
        val dir = File(home, relative)
        if (create && !dir.exists()) {
            if (!dir.mkdirs()) {
                return null
            }
        }
        return dir
    }

    /**
     * Returns the File pointing to the user's SDK install area. This is generally
     * the root directory containing the lint tool (but also platforms/ etc).
     *
     * @return a file pointing to the user's install area
     */
    open fun getSdkHome(): File? {
        val binDir = lintBinDir
        if (binDir != null) {
            assert(binDir.name == "tools")

            val root = binDir.parentFile
            if (root != null && root.isDirectory) {
                return root
            }
        }

        val home = System.getenv("ANDROID_HOME") ?: return null
        return File(home)
    }

    /**
     * Locates an SDK resource (relative to the SDK root directory).
     *
     *
     * TODO: Consider switching to a [URL] return type instead.
     *
     * @param relativePath A relative path (using [File.separator] to
     *            separate path components) to the given resource
     *
     * @return a [File] pointing to the resource, or null if it does not
     *         exist
     */
    open fun findResource(relativePath: String): File? {
        val top = getSdkHome() ?: throw IllegalArgumentException("Lint must be invoked with "
                + "\$ANDROID_HOME set to point to the SDK, or the System property "
                + PROP_BIN_DIR + " pointing to the ANDROID_SDK tools directory")

        // Files looked up by ExternalAnnotationRepository and ApiLookup, respectively
        val isAnnotationZip = "annotations.zip" == relativePath
        val isApiDatabase = "api-versions.xml" == relativePath
        if (isAnnotationZip || isApiDatabase) {
            if (isAnnotationZip) {
                // Allow Gradle builds etc to point to a specific location
                val path = System.getenv("SDK_ANNOTATIONS")
                if (path != null) {
                    val file = File(path)
                    if (file.exists()) {
                        return file
                    }
                }
            }

            // Look for annotations.zip or api-versions.xml: these used to ship with the
            // platform-tools, but were (in API 26) moved over to the API platform.
            // Look for the most recent version, falling back to platform-tools if necessary.
            val targets = getTargets()
            for (i in targets.indices.reversed()) {
                val target = targets[i]
                if (target.isPlatform && target.version.featureLevel >= SDK_DATABASE_MIN_VERSION) {
                    val file = File(target.getFile(IAndroidTarget.DATA), relativePath)
                    if (file.isFile) {
                        return file
                    }
                }
            }

            // Fallback to looking in the old location: platform-tools/api/<name> under the SDK
            var file = File(top, "platform-tools" + File.separator + "api" +
                    File.separator + relativePath)
            if (file.exists()) {
                return file
            }

            if (isApiDatabase) {
                // AOSP build environment?
                val build = System.getenv("ANDROID_BUILD_TOP")
                if (build != null) {
                    file = File(build, "development/sdk/api-versions.xml"
                            .replace('/', File.separatorChar))
                    if (file.exists()) {
                        return file
                    }
                }
            }

            return null
        }

        val file = File(top, relativePath)
        return when {
            file.exists() -> file
            else -> null
        }
    }

    private val projectInfo: MutableMap<Project, ClassPathInfo> = mutableMapOf()

    /**
     * Returns true if this project is a Gradle-based Android project
     *
     * @param project the project to check
     *
     * @return true if this is a Gradle-based project
     */
    open fun isGradleProject(project: Project): Boolean {
        // This is not an accurate test; specific LintClient implementations (e.g.
        // IDEs or a gradle-integration of lint) have more context and can perform a more accurate
        // check
        if (File(project.dir, SdkConstants.FN_BUILD_GRADLE).exists()) {
            return true
        }

        val parent = project.dir.parentFile
        if (parent != null && parent.name == SdkConstants.FD_SOURCES) {
            val root = parent.parentFile
            if (root != null && File(root, SdkConstants.FN_BUILD_GRADLE).exists()) {
                return true
            }
        }

        return false
    }

    /**
     * Information about class paths (sources, class files and libraries)
     * usually associated with a project.
     */
    class ClassPathInfo(
        val sourceFolders: List<File>,
        val classFolders: List<File>,
        private val libraries: List<File>,
        private val nonProvidedLibraries: List<File>,
        val testSourceFolders: List<File>,
        val testLibraries: List<File>,
        val generatedFolders: List<File>) {

        fun getLibraries(includeProvided: Boolean): List<File> =
                if (includeProvided) libraries else nonProvidedLibraries
    }

    /**
     * Considers the given project as an Eclipse project and returns class path
     * information for the project - the source folder(s), the output folder and
     * any libraries.
     *
     *
     * Callers will not cache calls to this method, so if it's expensive to compute
     * the classpath info, this method should perform its own caching.
     *
     * @param project the project to look up class path info for
     *
     * @return a class path info object, never null
     */
    open protected fun getClassPath(project: Project): ClassPathInfo {
        var info = projectInfo[project]
        if (info == null) {
            val sources = ArrayList<File>(2)
            val classes = ArrayList<File>(1)
            val generated = ArrayList<File>(1)
            val libraries = ArrayList<File>()
            // No test folders in Eclipse:
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=224708
            val tests = emptyList<File>()

            val projectDir = project.dir
            val classpathFile = File(projectDir, ".classpath")
            if (classpathFile.exists()) {
                val classpathXml = readFile(classpathFile)
                val document = CharSequences.parseDocumentSilently(classpathXml, false)
                if (document != null) {
                    val tags = document.getElementsByTagName("classpathentry")
                    var i = 0
                    val n = tags.length
                    while (i < n) {
                        val element = tags.item(i) as Element
                        val kind = element.getAttribute("kind")
                        var addTo: MutableList<File>? = null
                        when (kind) {
                            "src" -> addTo = sources
                            "output" -> addTo = classes
                            "lib" -> addTo = libraries
                        }
                        if (addTo != null) {
                            val path = element.getAttribute("path")
                            val folder = File(projectDir, path)
                            if (folder.exists()) {
                                addTo.add(folder)
                            }
                        }
                        i++
                    }
                }
            }

            // Add in libraries that aren't specified in the .classpath file
            val libs = File(project.dir, LIBS_FOLDER)
            if (libs.isDirectory) {
                val jars = libs.listFiles()
                if (jars != null) {
                    for (jar in jars) {
                        if (endsWith(jar.path, DOT_JAR) && !libraries.contains(jar)) {
                            libraries.add(jar)
                        }
                    }
                }
            }

            if (classes.isEmpty()) {
                var folder = File(projectDir, CLASS_FOLDER)
                if (folder.exists()) {
                    classes.add(folder)
                } else {
                    // Maven checks
                    folder = File(projectDir,
                            "target" + File.separator + "classes")
                    if (folder.exists()) {
                        classes.add(folder)

                        // If it's maven, also correct the source path, "src" works but
                        // it's in a more specific subfolder
                        if (sources.isEmpty()) {
                            var src = File(projectDir,
                                    "src" + File.separator
                                            + "main" + File.separator
                                            + "java")
                            if (src.exists()) {
                                sources.add(src)
                            } else {
                                src = File(projectDir, SRC_FOLDER)
                                if (src.exists()) {
                                    sources.add(src)
                                }
                            }

                            val gen = File(projectDir,
                                    "target" + File.separator
                                            + "generated-sources" + File.separator
                                            + "r")
                            if (gen.exists()) {
                                generated.add(gen)
                            }
                        }
                    }
                }
            }

            // Fallback, in case there is no Eclipse project metadata here
            if (sources.isEmpty()) {
                val src = File(projectDir, SRC_FOLDER)
                if (src.exists()) {
                    sources.add(src)
                }
                val gen = File(projectDir, GEN_FOLDER)
                if (gen.exists()) {
                    generated.add(gen)
                }
            }

            info = ClassPathInfo(sources, classes, libraries, libraries, tests,
                    emptyList(), generated)
            projectInfo.put(project, info)
        }

        return info
    }

    /**
     * A map from directory to existing projects, or null. Used to ensure that
     * projects are unique for a directory (in case we process a library project
     * before its including project for example)
     */
    protected val dirToProject: MutableMap<File, Project> = HashMap()

    /**
     * Returns a project for the given directory. This should return the same
     * project for the same directory if called repeatedly.
     *
     * @param dir the directory containing the project
     *
     * @param referenceDir See [Project.getReferenceDir].
     *
     * @return a project, never null
     */
    open fun getProject(dir: File, referenceDir: File): Project {
        val canonicalDir =
                try {
                    // Attempt to use the canonical handle for the file, in case there
                    // are symlinks etc present (since when handling library projects,
                    // we also call getCanonicalFile to compute the result of appending
                    // relative paths, which can then resolve symlinks and end up with
                    // a different prefix)
                    dir.canonicalFile
                } catch (ioe: IOException) {
                    dir
                }

        val existingProject: Project? = dirToProject[canonicalDir]
        if (existingProject != null) {
            return existingProject
        }

        val project = createProject(dir, referenceDir)
        dirToProject.put(canonicalDir, project)
        return project
    }

    /**
     * Returns the list of known projects (projects registered via
     * [.getProject]
     *
     * @return a collection of projects in any order
     */
    val knownProjects: Collection<Project>
        get() = dirToProject.values

    /**
     * Registers the given project for the given directory. This can
     * be used when projects are initialized outside of the client itself.
     *
     * @param dir the directory of the project, which must be unique
     *
     * @param project the project
     */
    open fun registerProject(dir: File, project: Project) {
        val canonicalDir =
                try {
                    // Attempt to use the canonical handle for the file, in case there
                    // are symlinks etc present (since when handling library projects,
                    // we also call getCanonicalFile to compute the result of appending
                    // relative paths, which can then resolve symlinks and end up with
                    // a different prefix)
                    dir.canonicalFile
                } catch (ioe: IOException) {
                    dir
                }
        assert(!dirToProject.containsKey(dir)) { dir }
        dirToProject.put(canonicalDir, project)
    }

    protected val projectDirs: MutableSet<File> = Sets.newHashSet<File>()

    /**
     * Create a project for the given directory
     * @param dir the root directory of the project
     *
     * @param referenceDir See [Project.getReferenceDir].
     *
     * @return a new project
     */
    open protected fun createProject(dir: File, referenceDir: File): Project {
        if (projectDirs.contains(dir)) {
            throw CircularDependencyException(
                    "Circular library dependencies; check your project.properties files carefully")
        }
        projectDirs.add(dir)
        return Project.create(this, dir, referenceDir)
    }

    /**
     * Perform any startup initialization of the full set of projects that lint will be
     * run on, if necessary.
     *
     * @param knownProjects the list of projects
     */
    open protected fun initializeProjects(knownProjects: Collection<Project>) = Unit

    /**
     * Perform any post-analysis cleaninup of the full set of projects that lint was
     * run on, if necessary.
     *
     * @param knownProjects the list of projects
     */
    open protected fun disposeProjects(knownProjects: Collection<Project>) = Unit

    /** Trampoline method to let [LintDriver] access protected method */
    internal fun performGetClassPath(project: Project): ClassPathInfo = getClassPath(project)

    /** Trampoline method to let [LintDriver] access protected method */
    internal fun performInitializeProjects(knownProjects: Collection<Project>) =
        initializeProjects(knownProjects)

    /** Trampoline method to let [LintDriver] access protected method */
    internal fun performDisposeProjects(knownProjects: Collection<Project>) =
        disposeProjects(knownProjects)

    /**
     * Returns the name of the given project
     *
     * @param project the project to look up
     *
     * @return the name of the project
     */
    open fun getProjectName(project: Project): String = project.dir.name

    private var targets: Array<IAndroidTarget>? = null

    /**
     * Returns all the [IAndroidTarget] versions installed in the user's SDK install
     * area.
     *
     * @return all the installed targets
     */
    open fun getTargets(): Array<IAndroidTarget> {
        if (targets == null) {
            val sdkHandler = getSdk()
            if (sdkHandler != null) {
                val logger = getRepositoryLogger()
                val targets = sdkHandler.getAndroidTargetManager(logger)
                        .getTargets(logger)
                this.targets = targets.toTypedArray()
            } else {
                targets = emptyArray()
            }
        }

        return targets as Array<IAndroidTarget>
    }

    private var sdk: AndroidSdkHandler? = null

    open fun getSdk(): AndroidSdkHandler? {
        if (sdk == null) {
            sdk = AndroidSdkHandler.getInstance(getSdkHome())
        }

        return sdk
    }

    /**
     * Returns the compile target to use for the given project
     *
     * @param project the project in question
     * @return the compile target to use to build the given project
     */
    open fun getCompileTarget(project: Project): IAndroidTarget? {
        val compileSdkVersion = project.buildTargetHash
        if (compileSdkVersion != null) {
            val logger = getRepositoryLogger()
            val handler = getSdk()
            if (handler != null) {
                val manager = handler.getAndroidTargetManager(logger)
                val target = manager.getTargetFromHashString(compileSdkVersion,
                        logger)
                if (target != null) {
                    return target
                }
            }
        }

        val buildSdk = project.buildSdk
        val targets = getTargets()
        for (i in targets.indices.reversed()) {
            val target = targets[i]
            if (target.isPlatform && target.version.apiLevel == buildSdk) {
                return target
            }
        }

        // Pick the highest compilation target we can find; the build API level
        // is not known or not found, but having *any* SDK is better than not (without
        // it, most symbol resolution will fail.)
        return targets.findLast { it.isPlatform }
    }

    /**
     * The highest known API level.
     */
    val highestKnownApiLevel: Int
        get() {
            var max = SdkVersionInfo.HIGHEST_KNOWN_STABLE_API

            for (target in getTargets()) {
                if (target.isPlatform) {
                    val api = target.version.apiLevel
                    if (api > max && !target.version.isPreview) {
                        max = api
                    }
                }
            }

            return max
        }

    /**
     * Returns the specific version of the build tools being used for the given project, if known
     *
     * @param project the project in question
     *
     *
     * @return the build tools version in use by the project, or null if not known
     */
    open fun getBuildTools(project: Project): BuildToolInfo? {
        val sdk = getSdk()
        // Build systems like Eclipse and ant just use the latest available
        // build tools, regardless of project metadata. In Gradle, this
        // method is overridden to use the actual build tools specified in the
        // project.
        if (sdk != null) {
            val compileTarget = getCompileTarget(project)
            if (compileTarget != null) {
                return compileTarget.buildToolInfo
            }
            return sdk.getLatestBuildTool(getRepositoryLogger(), false)
        }

        return null
    }

    /**
     * Returns the super class for the given class name, which should be in VM
     * format (e.g. java/lang/Integer, not java.lang.Integer, and using $ rather
     * than . for inner classes). If the super class is not known, returns null.
     *
     *
     * This is typically not necessary, since lint analyzes all the available
     * classes. However, if this lint client is invoking lint in an incremental
     * context (for example, an IDE offering incremental analysis of a single
     * source file), then lint may not see all the classes, and the client can
     * provide its own super class lookup.
     *
     * @param project the project containing the class
     *
     * @param name the fully qualified class name
     *
     * @return the corresponding super class name (in VM format), or null if not
     *         known
     */
    open fun getSuperClass(project: Project, name: String): String? {
        assert(name.indexOf('.') == -1) { "Use VM signatures, e.g. java/lang/Integer" }

        if ("java/lang/Object" == name) {
            return null
        }

        val superClass: String? = project.superClassMap[name]
        if (superClass != null) {
            return superClass
        }

        for (library in project.allLibraries) {
            val librarySuperClass = library.superClassMap[name]
            if (librarySuperClass != null) {
                return librarySuperClass
            }
        }

        return null
    }

    /**
     * Creates a super class map for the given project. The map maps from
     * internal class name (e.g. java/lang/Integer, not java.lang.Integer) to its
     * corresponding super class name. The root class, java/lang/Object, is not in the map.
     *
     * @param project the project to initialize the super class with; this will include
     *                local classes as well as any local .jar libraries; not transitive
     *                dependencies
     *
     * @return a map from class to its corresponding super class; never null
     */
    open fun createSuperClassMap(project: Project): Map<String, String> {
        val libraries = project.getJavaLibraries(true)
        val classFolders = project.javaClassFolders
        val classEntries = ClassEntry.fromClassPath(this, classFolders, true)
        if (libraries.isEmpty()) {
            return ClassEntry.createSuperClassMap(this, classEntries)
        }
        val libraryEntries = ClassEntry.fromClassPath(this, libraries, true)
        return ClassEntry.createSuperClassMap(this, libraryEntries, classEntries)
    }

    /**
     * Checks whether the given name is a subclass of the given super class. If
     * the method does not know, it should return null, and otherwise return
     * [java.lang.Boolean.TRUE] or [java.lang.Boolean.FALSE].
     *
     *
     * Note that the class names are in internal VM format (java/lang/Integer,
     * not java.lang.Integer, and using $ rather than . for inner classes).
     *
     * @param project the project context to look up the class in
     *
     * @param name the name of the class to be checked
     *
     * @param superClassName the name of the super class to compare to
     *
     * @return true if the class of the given name extends the given super class
     */
    open fun isSubclassOf(
            project: Project,
            name: String,
            superClassName: String): Boolean? = null

    /**
     * Finds any custom lint rule jars that should be included for analysis,
     * regardless of project.
     *
     *
     * The default implementation locates custom lint jars in ~/.android/lint/ and
     * in $ANDROID_LINT_JARS
     *
     * @return a list of rule jars (possibly empty).
     */
    open fun findGlobalRuleJars(): List<File> {
        // Look for additional detectors registered by the user, via
        // (1) an environment variable (useful for build servers etc), and
        // (2) via jar files in the .android/lint directory
        var files: MutableList<File>? = null
        try {
            val androidHome = AndroidLocation.getFolder()
            val lint = File(androidHome + File.separator + "lint")
            if (lint.exists()) {
                val list = lint.listFiles()
                if (list != null) {
                    for (jarFile in list) {
                        if (endsWith(jarFile.name, DOT_JAR)) {
                            if (files == null) {
                                files = ArrayList()
                            }
                            files.add(jarFile)
                        }
                    }
                }
            }
        } catch (e: AndroidLocation.AndroidLocationException) {
            // Ignore -- no android dir, so no rules to load.
        }

        val lintClassPath = System.getenv("ANDROID_LINT_JARS")
        if (lintClassPath != null && !lintClassPath.isEmpty()) {
            val paths = lintClassPath.split(File.pathSeparator)
            for (path in paths) {
                val jarFile = File(path)
                if (jarFile.exists()) {
                    if (files == null) {
                        files = mutableListOf()
                    } else if (files.contains(jarFile)) {
                        continue
                    }
                    files.add(jarFile)
                }
            }
        }

        return if (files != null) files else emptyList()
    }

    /**
     * Finds any custom lint rule jars that should be included for analysis
     * in the given project
     *
     * @param project the project to look up rule jars from
     *
     * @return a list of rule jars (possibly empty).
     */
    open fun findRuleJars(project: Project): List<File> {
        if (project.isGradleProject) {
            if (project.isLibrary && project.gradleLibraryModel != null) {
                val model = project.gradleLibraryModel
                if (model != null) {
                    val lintJar = model.lintJar
                    if (lintJar.exists()) {
                        return listOf(lintJar)
                    }
                }
            } else if (project.subset != null) {
                // Probably just analyzing a single file: we still want to look for custom
                // rules applicable to the file
                val variant = project.currentVariant
                if (variant != null) {
                    val rules = ArrayList<File>(4)
                    addLintJarsFromDependencies(rules, variant.mainArtifact.dependencies.libraries,
                            mutableSetOf())

                    // Locally packaged jars
                    project.gradleProjectModel?.buildFolder?.let {
                        // Soon we'll get these paths via the builder-model so we
                        // don't need to have a hardcoded path (b/66166521)
                        val lintFolder = File(it, "intermediates${File.separator}lint")
                        if (lintFolder.exists()) {
                            lintFolder.listFiles()?.forEach { lintJar ->
                                // Note that currently there will just be a single one
                                // for now (b/66164808), and it will always be named lint.jar.
                                if (lintJar.path.endsWith(DOT_JAR)) {
                                    rules.add(lintJar)
                                }
                            }
                        }
                    }

                    if (!rules.isEmpty()) {
                        return rules
                    }
                }
            } else if (project.dir.path.endsWith(DOT_AAR)) {
                val lintJar = File(project.dir, "lint.jar")
                if (lintJar.exists()) {
                    return listOf(lintJar)
                }
            }
        }

        return emptyList()
    }

    /**
     * Recursively add all lint jars found recursively from the given collection of
     * [AndroidLibrary] instances into the given [lintJars] list
     */
    private fun addLintJarsFromDependencies(
            lintJars: MutableList<File>,
            libraries: Collection<AndroidLibrary>,
            seen: MutableSet<AndroidLibrary>) {
        for (library in libraries) {
            if (!seen.add(library)) { // Already processed
                return
            }
            addLintJarsFromDependency(lintJars, library, seen)
        }
    }

    /**
     * Recursively add all lint jars found from the given [AndroidLibrary] **or its dependencies**
     * into the given [lintJars] list
     */
    private fun addLintJarsFromDependency(
            lintJars: MutableList<File>,
            library: AndroidLibrary,
            seen: MutableSet<AndroidLibrary>) {
        val lintJar = library.lintJar
        if (lintJar.exists()) {
            lintJars.add(lintJar)
        }
        addLintJarsFromDependencies(lintJars, library.libraryDependencies, seen)
    }


    /**
     * Opens a URL connection.
     *
     * Clients such as IDEs can override this to for example consider the user's IDE proxy
     * settings.
     *
     * @param url the URL to read
     *
     * @return a [URLConnection] or null
     *
     * @throws IOException if any kind of IO exception occurs
     */
    @Throws(IOException::class)
    open fun openConnection(url: URL): URLConnection? = openConnection(url, 0)

    /**
     * Opens a URL connection.
     *
     * Clients such as IDEs can override this to for example consider the user's IDE proxy
     * settings.
     *
     * @param url     the URL to read
     *
     * @param timeout the timeout to apply for HTTP connections (or 0 to wait indefinitely)
     *
     * @return a [URLConnection] or null
     *
     * @throws IOException if any kind of IO exception occurs including timeouts
     */
    @Throws(IOException::class)
    open fun openConnection(url: URL, timeout: Int): URLConnection? {
        val connection = url.openConnection()
        if (timeout > 0) {
            connection.connectTimeout = timeout
            connection.readTimeout = timeout
        }
        return connection
    }

    /** Closes a connection previously returned by [.openConnection]  */
    open fun closeConnection(connection: URLConnection) {
        (connection as? HttpURLConnection)?.disconnect()
    }

    /**
     * Returns true if the given directory is a lint project directory.
     * By default, a project directory is the directory containing a manifest file,
     * but in Gradle projects for example it's the root gradle directory.
     *
     * @param dir the directory to check
     *
     * @return true if the directory represents a lint project
     */
    open fun isProjectDirectory(dir: File): Boolean =
            LintUtils.isManifestFolder(dir) ||
                    Project.isAospFrameworksRelatedProject(dir) ||
                    File(dir, FN_BUILD_GRADLE).exists()

    /**
     * Returns whether lint should look for suppress comments. Tools that already do
     * this on their own can return false here to avoid doing unnecessary work.
     */
    open fun checkForSuppressComments(): Boolean = true

    /**
     * Adds in any custom lint rules and returns the result as a new issue registry,
     * or the same one if no custom rules were found
     *
     * @param registry the main registry to add rules to
     *
     * @return a new registry containing the passed in rules plus any custom rules,
     *   or the original registry if no custom rules were found
     */
    open fun addCustomLintRules(registry: IssueRegistry): IssueRegistry {
        val jarFiles = findGlobalRuleJars()


        if (!jarFiles.isEmpty()) {
            val extraRegistries = JarFileIssueRegistry.get(this, jarFiles)
            if (extraRegistries.isNotEmpty()) {
                return JarFileIssueRegistry.join(registry, *extraRegistries.toTypedArray())
            }
        }

        return registry
    }

    /**
     * Creates a [ClassLoader] which can load in a set of Jar files.
     *
     * @param urls the URLs
     *
     * @param parent the parent class loader
     *
     * @return a new class loader
     */
    open fun createUrlClassLoader(urls: Array<URL>, parent: ClassLoader): ClassLoader =
            URLClassLoader(urls, parent)

    /**
     * Returns the merged manifest of the given project. This may return null
     * if not called on the main project. Note that the file reference
     * in the merged manifest isn't accurate; the merged manifest accumulates
     * information from a wide variety of locations.
     *
     * @return The merged manifest, if available.
     */
    open fun getMergedManifest(project: Project): Document? {
        val manifestFiles = project.manifestFiles
        if (manifestFiles.size == 1) {
            val primary = manifestFiles[0]
            try {
                val xml = Files.toString(primary, UTF_8)
                return XmlUtils.parseDocumentSilently(xml, true)
            } catch (e: IOException) {
                log(Severity.ERROR, e, "Could not read manifest " + primary)
            }

        }

        return null
    }

    /**
     * Record that the given document corresponds to a merged manifest file;
     * locations from this document should attempt to resolve back to the original
     * source location
     *
     * @param mergedManifest the document for the merged manifest
     * @param reportFile the manifest merger report file, or the report itself
     */
    open fun resolveMergeManifestSources(mergedManifest: Document, reportFile: Any) {
        mergedManifest.setUserData(MERGED_MANIFEST, reportFile, null)
    }

    /**
     * Returns true if the given node is part of a merged manifest document
     * (already configured via [.resolveMergeManifestSources])
     *
     * @param node the node to look up
     *
     * @return true if this node is part of a merged manifest document
     */
    fun isMergeManifestNode(node: Node): Boolean =
        node.ownerDocument?.getUserData(MERGED_MANIFEST) != null

    /** Cache used by [.findManifestSourceNode]  */
    @Suppress("MemberVisibilityCanPrivate")
    protected val reportFileCache: MutableMap<Any, BlameFile> =
        Maps.newHashMap<Any, BlameFile>()

    /** Cache used by [.findManifestSourceNode]  */
    @Suppress("MemberVisibilityCanPrivate")
    protected val sourceNodeCache: MutableMap<Node, Pair<File, Node>> =
        Maps.newIdentityHashMap<Node, Pair<File, Node>>()

    /**
     * For the given node from a merged manifest, find the corresponding
     * source manifest node, if possible
     *
     * @param mergedNode the node from the merged manifest
     *
     * @return the corresponding manifest node in one of the source files, if possible
     */
    open fun findManifestSourceNode(mergedNode: Node): Pair<File, Node>? {
        val doc = mergedNode.ownerDocument ?: return null
        val report = doc.getUserData(MERGED_MANIFEST) ?: return null

        val cached = sourceNodeCache[mergedNode]
        if (cached != null) {
            if (cached === NOT_FOUND) {
                return null
            }
            return cached
        }

        var blameFile = reportFileCache[report]
        if (blameFile == null) {
            try {
                when (report) {
                    is File -> {
                        if (report.path.endsWith(DOT_XML)) {
                            // Single manifest file: no manifest merging, passed source document
                            // straight through
                            return Pair.of(report, mergedNode)
                        }
                        blameFile = BlameFile.parse(report)
                    }
                    is String -> {
                        val lines = Splitter.on('\n').splitToList(report)
                        blameFile = BlameFile.parse(lines)
                    }
                    is Actions -> blameFile = BlameFile.parse(report)
                    else -> {
                        assert(false) { report }
                        blameFile = BlameFile.NONE
                    }
                }
            } catch (ignore: IOException) {
                blameFile = BlameFile.NONE
            }

            @Suppress("ALWAYS_NULL")
            blameFile!!

            reportFileCache.put(report, blameFile)
        }

        var source: Pair<File, Node>? = null
        if (blameFile !== BlameFile.NONE) {
            source = blameFile.findSourceNode(this, mergedNode)
        }

        // Cache for next time
        val cacheValue = source ?: NOT_FOUND
        sourceNodeCache[mergedNode] = cacheValue

        return source
    }

    /**
     * Returns the location for a given node from a merged manifest file. Convenience
     * wrapper around [.findManifestSourceNode] and
     * [XmlParser.getLocation]
     */
    open fun findManifestSourceLocation(mergedNode: Node): Location? {
        val source = findManifestSourceNode(mergedNode)
        if (source != null) {
            return xmlParser.getLocation(source.first, source.second)
        }

        return null
    }

    /**
     * Formats the given path
     * @param file the path to compute a display name for
     *
     * @return a path formatted for user display, in [TextFormat.RAW] text format (e.g.
     *      with backslashes, asterisks etc escaped)
     */
    open fun getDisplayPath(file: File): String = TextFormat.TEXT.convertTo(file.path, TextFormat.RAW)

    /**
     * Returns true if this client supports project resource repository lookup via
     * [.getResourceRepository]
     *
     * @return true if the client can provide project resources
     */
    open fun supportsProjectResources(): Boolean = false

    /**
     * Returns the project resources, if available
     *
     * @param includeDependencies if true, include merged view of all dependencies
     *
     * @return the project resources, or null if not available
     *
     */
    @Deprecated("Use {@link #getResourceRepository} instead",
            ReplaceWith("getResourceRepository(project, includeDependencies, false)"))
    open fun getProjectResources(project: Project, includeDependencies: Boolean):
            AbstractResourceRepository? =
            getResourceRepository(project, includeDependencies, false)

    /**
     * Returns the project resources, if available
     *
     * @param includeModuleDependencies if true, include merged view of all module dependencies
     *
     * @param includeLibraries          if true, include merged view of all library dependencies
     *                                  (this also requires all module dependencies)
     *
     * @return the project resources, or null if not available
     */
    open fun getResourceRepository(project: Project, includeModuleDependencies: Boolean,
                                   includeLibraries: Boolean): AbstractResourceRepository? = null

    /**
     * For a lint client which supports resource items (via [.supportsProjectResources])
     * return a handle for a resource item
     *
     * @param item the resource item to look up a location handle for
     *
     * @return a corresponding handle
     */
    open fun createResourceItemHandle(item: ResourceItem): Location.Handle =
            Location.ResourceItemHandle(item)

    private var resourceVisibilityProvider: ResourceVisibilityLookup.Provider? = null

    /**
     * Returns a shared [ResourceVisibilityLookup.Provider]
     *
     * @return a shared provider for looking up resource visibility
     */
    open fun getResourceVisibilityProvider(): ResourceVisibilityLookup.Provider {
        if (resourceVisibilityProvider == null) {
            resourceVisibilityProvider = ResourceVisibilityLookup.Provider()
        }
        return resourceVisibilityProvider!!
    }

    /** Returns the version number of this lint client, if known  */
    open fun getClientRevision(): String? = null

    /**
     * Runs the given runnable under a read lock such that it can access the PSI
     *
     * @param runnable the runnable to be run
     */
    open fun runReadAction(runnable: Runnable) = runnable.run()

    /** Returns a repository logger used by this client.  */
    open fun getRepositoryLogger(): ProgressIndicator = RepoLogger()

    private class RepoLogger : ProgressIndicatorAdapter() {
        // Intentionally not logging these: the SDK manager is
        // logging events such as package.xml parsing
        //   Parsing /path/to/sdk//build-tools/19.1.0/package.xml
        //   Parsing /path/to/sdk//build-tools/20.0.0/package.xml
        //   Parsing /path/to/sdk//build-tools/21.0.0/package.xml
        // which we don't want to spam on the console.
        // It's also warning about packages that it's encountering
        // multiple times etc; that's not something we should include
        // in lint command line output.

        override fun logError(s: String, e: Throwable?) = Unit

        override fun logInfo(s: String) = Unit

        override fun logWarning(s: String, e: Throwable?) = Unit
    }

    companion object {
        @JvmStatic private val PROP_BIN_DIR = "com.android.tools.lint.bindir"

        /**
         * Returns the File corresponding to the system property or the environment variable
         * for [.PROP_BIN_DIR].
         * This property is typically set by the SDK/tools/lint[.bat] wrapper.
         * It denotes the path of the wrapper on disk.
         *
         * @return A new File corresponding to [LintClient.PROP_BIN_DIR] or null.
         */
        private val lintBinDir: File?
            get() {
                // First check the Java properties (e.g. set using "java -jar ... -Dname=value")
                // If not found, check environment variables.
                var path: String? = System.getProperty(PROP_BIN_DIR)
                if (path == null || path.isEmpty()) {
                    path = System.getenv(PROP_BIN_DIR)
                }
                if (path != null && !path.isEmpty()) {
                    val file = File(path)
                    if (file.exists()) {
                        return file
                    }
                }
                return null
            }

        /**
         * Database moved from platform-tools to SDK in API level 26.
         *
         * This duplicates the constant in [LintClient] but that
         * constant is not public (because it's in the API package and I don't
         * want this part of the API surface; it's an implementation optimization.)
         */
        private const val SDK_DATABASE_MIN_VERSION = 26

        /**
         * Key stashed as user data on merged manifest documents such that we
         * can quickly determine if a node is originally from a merged manifest (this
         * is used to automatically resolve reported errors on the merged manifest
         * back to the corresponding source locations, when possible.)
         */
        private const val MERGED_MANIFEST = "lint-merged-manifest"

        @JvmField protected val NOT_FOUND: Pair<File, Node> = Pair.of<File, Node>(null, null)

        /**
         * The client name returned by [.getClientName] when running in
         * Android Studio/IntelliJ IDEA
         */
        @JvmField val CLIENT_STUDIO = "studio"

        /**
         * The client name returned by [.getClientName] when running in
         * Gradle
         */
        @JvmField val CLIENT_GRADLE = "gradle"

        /**
         * The client name returned by [.getClientName] when running in
         * the CLI (command line interface) version of lint, `lint`
         */
        @JvmField val CLIENT_CLI = "cli"

        /**
         * The client name returned by [.getClientName] when running in
         * some unknown client
         */
        @JvmField val CLIENT_UNKNOWN = "unknown"

        /** The client name.  */
        /**
         * Returns the name of the embedding client. It could be not just
         * [.CLIENT_STUDIO], [.CLIENT_GRADLE], [.CLIENT_CLI]
         * etc but other values too as lint is integrated in other embedding contexts.
         *
         * @return the name of the embedding client
         */
        @JvmStatic var clientName = CLIENT_UNKNOWN
            private set

        /**
         * Returns true if the embedding client currently running lint is Android Studio
         * (or IntelliJ IDEA)
         *
         * @return true if running in Android Studio / IntelliJ IDEA
         */
        @JvmStatic val isStudio: Boolean
            get() = CLIENT_STUDIO == clientName

        /**
         * Returns true if the embedding client currently running lint is Gradle
         *
         * @return true if running in Gradle
         */
        @JvmStatic val isGradle: Boolean
            get() = CLIENT_GRADLE == clientName
    }
}
