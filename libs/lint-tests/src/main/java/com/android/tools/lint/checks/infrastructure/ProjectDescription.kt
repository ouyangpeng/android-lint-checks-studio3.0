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

package com.android.tools.lint.checks.infrastructure

/**
 * A description of a lint test project
 */
class ProjectDescription {
    var files: Array<out TestFile> = emptyArray()
    val dependsOn: MutableList<ProjectDescription> = mutableListOf()
    var dependencyGraph: String? = null
    var name: String = ""
    var type = Type.APP
    var report = true

    /**
     * Creates a new project description
     */
    constructor() {
    }

    /**
     * Creates a new project with the given set of files
     */
    constructor(vararg files: TestFile) {
        files(*files)
    }

    /**
     * Names the project; most useful in multi-project tests where the project name
     * will be part of the error output
     *
     * @param name the name for the project
     *
     * @return this for constructor chaining
     */
    fun name(name: String): ProjectDescription {
        this.name = name
        return this
    }

    /**
     * Sets the given set of test files as the project contents
     *
     * @param files the test files
     *
     * @return this for constructor chaining
     */
    fun files(vararg files: TestFile): ProjectDescription {
        this.files = files
        return this
    }

    /**
     * Adds the given project description as a direct dependency for this project
     *
     * @param library the project to depend on
     *
     * @return this for constructor chaining
     */
    fun dependsOn(library: ProjectDescription): ProjectDescription {
        dependsOn.add(library)
        return this
    }

    /**
     * Adds the given dependency graph (the output of the Gradle dependency task)
     * to be constructed when mocking a Gradle model for this project.
     * <p>
     * To generate this, run for example
     * <pre>
     *     ./gradlew :app:dependencies
     * </pre>
     * and then look at the debugCompileClasspath (or other graph that you want
     * to model).
     *
     * @param dependencyGraph the graph description
     * @return this for constructor chaining
     */
    fun withDependencyGraph(dependencyGraph: String) : ProjectDescription {
        this.dependencyGraph = dependencyGraph
        return this
    }

    /**
     * Marks the project as an app, library or Java module
     *
     * @param type the type of project to create
     *
     * @return this for constructor chaining
     */
    fun type(type: Type): ProjectDescription {
        this.type = type
        return this
    }

    /**
     * Marks this project as reportable (the default) or non-reportable.
     * Lint projects are usually reportable, but if they depend on libraries
     * (such as appcompat) those dependencies are marked as non-reportable.
     * Lint will still analyze those projects (for example, an unused resource
     * analysis should list resources pulled in from these libraries) but issues
     * found within those libraries will not be reported.
     *
     * @param report whether we should report issues for this project
     *
     * @return this for constructor chaining
     */
    fun report(report: Boolean): ProjectDescription {
        this.report = report
        return this
    }

    /** Describes different types of lint test projects  */
    enum class Type {
        APP,
        LIBRARY,
        JAVA
    }
}