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

package com.android.tools.lint.checks.infrastructure;

import static com.android.SdkConstants.ANDROID_MANIFEST_XML;
import static com.android.SdkConstants.DOT_JAR;
import static com.android.SdkConstants.FN_ANNOTATIONS_ZIP;
import static com.android.SdkConstants.FN_CLASSES_JAR;
import static com.android.SdkConstants.VALUE_TRUE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.build.FilterData;
import com.android.builder.model.AndroidArtifact;
import com.android.builder.model.AndroidArtifactOutput;
import com.android.builder.model.AndroidLibrary;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.ApiVersion;
import com.android.builder.model.BuildType;
import com.android.builder.model.BuildTypeContainer;
import com.android.builder.model.ClassField;
import com.android.builder.model.Dependencies;
import com.android.builder.model.JavaCompileOptions;
import com.android.builder.model.JavaLibrary;
import com.android.builder.model.Library;
import com.android.builder.model.LintOptions;
import com.android.builder.model.MavenCoordinates;
import com.android.builder.model.ProductFlavor;
import com.android.builder.model.ProductFlavorContainer;
import com.android.builder.model.SourceProvider;
import com.android.builder.model.SourceProviderContainer;
import com.android.builder.model.Variant;
import com.android.builder.model.VectorDrawablesOptions;
import com.android.builder.model.level2.DependencyGraphs;
import com.android.builder.model.level2.GlobalLibraryMap;
import com.android.builder.model.level2.GraphItem;
import com.android.ide.common.repository.GradleCoordinate;
import com.android.ide.common.repository.GradleVersion;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.SdkVersionInfo;
import com.android.utils.FileUtils;
import com.android.utils.ILogger;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import junit.framework.TestCase;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.mockito.stubbing.OngoingStubbing;

/**
 * A utility class which builds mocks for the Gradle builder-model API, by loosely interpreting
 * .gradle files and building models based on recognizing common patterns there.
 *
 *
 * TODO: Clean way to configure whether build dep cache is enabled
 * TODO: Handle scopes (test dependencies etc)
 */
public class GradleModelMocker {
    private AndroidProject project;
    private Variant variant;
    private GlobalLibraryMap globalLibraryMap;
    private final List<BuildType> buildTypes = Lists.newArrayList();
    private final List<AndroidLibrary> androidLibraries = Lists.newArrayList();
    private final List<JavaLibrary> javaLibraries = Lists.newArrayList();
    private final List<JavaLibrary> allJavaLibraries = Lists.newArrayList();
    private ProductFlavor mergedFlavor;
    private ProductFlavor defaultFlavor;
    private LintOptions lintOptions;
    private File projectDir = new File("");
    private final List<ProductFlavor> productFlavors = Lists.newArrayList();
    private final Multimap<String, String> splits = ArrayListMultimap.create();
    private ILogger logger;
    private boolean initialized;
    @Language("Groovy")
    private final String gradle;
    private GradleVersion modelVersion = GradleVersion.parse("2.2.2");
    private final Map<String, Dep> graphs = Maps.newHashMap();
    private boolean useBuildCache;
    private VectorDrawablesOptions vectorDrawablesOptions;
    private boolean allowUnrecognizedConstructs;
    private boolean fullDependencies;
    private JavaCompileOptions compileOptions;
    private boolean javaPlugin;
    private boolean javaLibraryPlugin;

    public GradleModelMocker(@Language("Groovy") String gradle) {
        this.gradle = gradle;
    }

    @NonNull
    public GradleModelMocker withLogger(@Nullable ILogger logger) {
        this.logger = logger;
        return this;
    }

    @NonNull
    public GradleModelMocker withModelVersion(@NonNull String modelVersion) {
        this.modelVersion = GradleVersion.parse(modelVersion);
        return this;
    }

    @NonNull
    public GradleModelMocker withProjectDir(@NonNull File projectDir) {
        this.projectDir = projectDir;
        return this;
    }

    @NonNull
    public GradleModelMocker withDependencyGraph(@NonNull String graph) {
        parseDependencyGraph(graph, graphs);
        return this;
    }

    public GradleModelMocker allowUnrecognizedConstructs() {
        this.allowUnrecognizedConstructs = true;
        return this;
    }

    public GradleModelMocker withBuildCache(boolean useBuildCache) {
        this.useBuildCache = useBuildCache;
        return this;
    }

    /**
     * If true, model a full/deep dependency graph in
     * {@link com.android.builder.model.level2.DependencyGraphs}; the default
     * is flat. (This is normally controlled by sync/model builder flag
     * {@link AndroidProject#PROPERTY_BUILD_MODEL_FEATURE_FULL_DEPENDENCIES}.)
     */
    public GradleModelMocker withFullDependencies(boolean fullDependencies) {
        this.fullDependencies = fullDependencies;
        return this;
    }

    private void warn(String message) {
        if (!allowUnrecognizedConstructs) {
            error(message);
            return;
        }

        if (logger != null) {
            logger.warning(message);
        } else {
            System.err.println(message);
        }
    }

    private void error(String message) {
        if (logger != null) {
            logger.error(null, message);
        } else {
            System.err.println(message);
        }
    }

    private void ensureInitialized() {
        if (!initialized) {
            initialized = true;
            initialize();
        }
    }

    /** Whether the Gradle file applied the java plugin */
    public boolean hasJavaPlugin() {
        return javaPlugin;
    }

    /** Whether the Gradle file applied the java-library plugin */
    public boolean hasJavaLibraryPlugin() {
        return javaLibraryPlugin;
    }

    public boolean isLibrary() {
        return project.getProjectType() == AndroidProject.PROJECT_TYPE_LIBRARY
                || hasJavaLibraryPlugin();
    }

    /** Whether the Gradle file applied the java-library plugin */
    public boolean hasAndroidLibraryPlugin() {
        return javaLibraryPlugin;
    }

    public AndroidProject getProject() {
        ensureInitialized();
        return project;
    }

    public Variant getVariant() {
        ensureInitialized();
        return variant;
    }

    @Nullable
    public GlobalLibraryMap getGlobalLibraryMap() {
        ensureInitialized();
        return globalLibraryMap;
    }

    private void initialize() {
        project = mock(AndroidProject.class);

        when(project.getModelVersion()).thenReturn(modelVersion.toString());
        int apiVersion = modelVersion.getMajor() >= 2 ? 3 : 2;
        when(project.getApiVersion()).thenReturn(apiVersion);
        when(project.getFlavorDimensions()).thenReturn(Lists.newArrayList());

        variant = mock(Variant.class);

        lintOptions = createLintOptions();
        when(project.getLintOptions()).thenReturn(lintOptions);

        compileOptions = mock(JavaCompileOptions.class);
        when(compileOptions.getSourceCompatibility()).thenReturn("1.7");
        when(compileOptions.getTargetCompatibility()).thenReturn("1.7");
        when(compileOptions.getEncoding()).thenReturn("UTF-8");
        when(project.getJavaCompileOptions()).thenReturn(compileOptions);

        // built-in build-types
        getBuildType("debug", true);
        getBuildType("release", true);

        defaultFlavor = getProductFlavor("defaultConfig", true);
        when(defaultFlavor.getVersionCode()).thenReturn(null); // don't default to Integer.valueOf(0) !

        Dependencies dependencies = mock(Dependencies.class);
        when(dependencies.getLibraries()).thenReturn(androidLibraries);

        if (modelVersion.isAtLeast(2, 0, 0)) {
            when(dependencies.getJavaLibraries()).thenReturn(javaLibraries);
        } else {
            // Should really throw org.gradle.tooling.model.UnsupportedMethodException here!
            when(dependencies.getJavaLibraries()).thenThrow(new RuntimeException());
        }

        //mergedFlavor = mock(ProductFlavor.class);
        //when(variant.getMergedFlavor()).thenReturn(mergedFlavor);
        // TODO: Apply merge logic to apply a suitable default here
        when(variant.getMergedFlavor()).thenReturn(defaultFlavor);
        mergedFlavor = defaultFlavor; // shortcut for now
        getVectorDrawableOptions(); // ensure initialized

        scan(gradle, "");

        List<BuildTypeContainer> containers = Lists.newArrayList();
        for (BuildType buildType : buildTypes) {
            BuildTypeContainer container = mock(BuildTypeContainer.class);
            when(container.getBuildType()).thenReturn(buildType);
            containers.add(container);

            SourceProvider provider = createSourceProvider(projectDir, buildType.getName());
            when(container.getSourceProvider()).thenReturn(provider);
        }

        when(project.getBuildTypes()).thenReturn(containers);
        ProductFlavorContainer defaultContainer = mock(ProductFlavorContainer.class);
        when(defaultContainer.getProductFlavor()).thenReturn(defaultFlavor);
        when(project.getDefaultConfig()).thenReturn(defaultContainer);
        SourceProvider mainProvider = createSourceProvider(projectDir, "main");
        when(defaultContainer.getSourceProvider()).thenReturn(mainProvider);

        SourceProviderContainer androidTestProvider = mock(SourceProviderContainer.class);
        when(androidTestProvider.getArtifactName()).thenReturn(AndroidProject.ARTIFACT_ANDROID_TEST);
        SourceProvider androidSourceProvider = createSourceProvider(projectDir, "androidTest");
        when(androidTestProvider.getSourceProvider()).thenReturn(androidSourceProvider);
        SourceProviderContainer unitTestProvider = mock(SourceProviderContainer.class);
        when(unitTestProvider.getArtifactName()).thenReturn(AndroidProject.ARTIFACT_UNIT_TEST);
        SourceProvider unitSourceProvider = createSourceProvider(projectDir, "test");
        when(unitTestProvider.getSourceProvider()).thenReturn(unitSourceProvider);

        List<SourceProviderContainer> extraProviders = Lists.newArrayList(androidTestProvider,
                unitTestProvider);
        when(defaultContainer.getExtraSourceProviders()).thenReturn(extraProviders);

        List<ProductFlavorContainer> flavorContainers = Lists.newArrayList();
        flavorContainers.add(defaultContainer);
        for (ProductFlavor flavor : productFlavors) {
            if (flavor == defaultFlavor) {
                continue;
            }
            ProductFlavorContainer container = mock(ProductFlavorContainer.class);
            SourceProvider flavorSourceProvider = createSourceProvider(projectDir, flavor.getName());
            when(container.getSourceProvider()).thenReturn(flavorSourceProvider);
            when(container.getProductFlavor()).thenReturn(flavor);
            flavorContainers.add(container);
        }
        when(project.getProductFlavors()).thenReturn(flavorContainers);

        // Artifacts
        AndroidArtifact artifact = mock(AndroidArtifact.class);
        //noinspection deprecation
        when(artifact.getDependencies()).thenReturn(dependencies);
        when(variant.getMainArtifact()).thenReturn(artifact);

        if (modelVersion.isAtLeast(2, 5, 0, "alpha", 1, false)) {
            DependencyGraphs graphs = createDependencyGraphs();
            when(artifact.getDependencyGraphs()).thenReturn(graphs);
        } else {
            // Should really throw org.gradle.tooling.model.UnsupportedMethodException here!
            when(artifact.getDependencyGraphs()).thenThrow(new RuntimeException());
        }

        when(project.getBuildFolder()).thenReturn(new File(projectDir, "build"));

        Collection<AndroidArtifactOutput> outputs = Lists.newArrayList();
        outputs.add(createAndroidArtifactOutput("", ""));
        for (Map.Entry<String, String> entry : splits.entries()) {
            outputs.add(createAndroidArtifactOutput(entry.getKey(), entry.getValue()));
        }
        //outputs.add(createAndroidArtifactOutput("DENSITY", "mdpi"));
        //outputs.add(createAndroidArtifactOutput("DENSITY", "hdpi"));
        when(artifact.getOutputs()).thenReturn(outputs);


        Set<String> seenDimensions = Sets.newHashSet();
        String defaultVariant = "debug";
        for (ProductFlavor flavor : productFlavors) {
            if (flavor != defaultFlavor) {
                String dimension = flavor.getDimension();
                if (dimension == null) {
                    dimension = "";
                }
                if (!seenDimensions.contains(dimension)) {
                    seenDimensions.add(dimension);
                    String name = flavor.getName();
                    defaultVariant += (Character.toUpperCase(name.charAt(0)) + name.substring(1));
                }
            }
        }
        setVariantName(defaultVariant);
    }

    private static LintOptions createLintOptions() {
        LintOptions options = mock(LintOptions.class);
        // Configure default (and make it mutable, e.g. lists that we can append to)
        when(options.getEnable()).thenReturn(Sets.newHashSet());
        when(options.getDisable()).thenReturn(Sets.newHashSet());
        when(options.getCheck()).thenReturn(Sets.newHashSet());
        when(options.getEnable()).thenReturn(Sets.newHashSet());
        when(options.getSeverityOverrides()).thenReturn(Maps.newHashMap());

        // Set the same defaults as the Gradle side
        when(options.isAbortOnError()).thenReturn(true);
        when(options.isCheckReleaseBuilds()).thenReturn(true);
        when(options.getHtmlReport()).thenReturn(true);
        when(options.getXmlReport()).thenReturn(true);

        /* These are true in lint in general, but we want them to be false in the unit tests
        when(options.isAbsolutePaths()).thenReturn(true);
        when(options.isExplainIssues()).thenReturn(true);
        */

        return options;
    }

    @NonNull
    private DependencyGraphs createDependencyGraphs() {
        DependencyGraphs graphs = mock(DependencyGraphs.class);
        List<GraphItem> compileItems = Lists.newArrayList();
        Map<String, com.android.builder.model.level2.Library> globalMap = Maps.newHashMap();

        when(graphs.getCompileDependencies()).thenReturn(compileItems);
        when(graphs.getPackageDependencies()).thenReturn(compileItems);
        when(graphs.getProvidedLibraries()).thenReturn(Collections.emptyList());
        when(graphs.getSkippedLibraries()).thenReturn(Collections.emptyList());

        HashSet<String> seen = Sets.newHashSet();
        addGraphItems(compileItems, globalMap, seen, androidLibraries);
        addGraphItems(compileItems, globalMap, seen, javaLibraries);

        // Java libraries aren't available from the AndroidLibraries themselves;
        // stored in a separate global map during initialization
        for (JavaLibrary library : allJavaLibraries) {
            com.android.builder.model.level2.Library lib = createLevel2Library(library);
            globalMap.put(lib.getArtifactAddress(), lib);
        }

        globalLibraryMap = mock(GlobalLibraryMap.class);
        when(globalLibraryMap.getLibraries()).thenReturn(globalMap);

        return graphs;
    }

    private void addGraphItems(
            List<GraphItem> result,
            Map<String, com.android.builder.model.level2.Library> globalMap,
            Set<String> seen,
            Collection<? extends Library> libraries) {
        for (Library library : libraries) {
            MavenCoordinates coordinates = library.getResolvedCoordinates();
            String name = coordinates.getGroupId() + ':' + coordinates.getArtifactId() + ':'
                    + coordinates.getVersion() + '@' + coordinates.getPackaging();
            if (fullDependencies || !seen.contains(name)) {
                seen.add(name);

                GraphItem item = mock(GraphItem.class);
                result.add(item);
                when(item.getArtifactAddress()).thenReturn(name);
                when(item.getRequestedCoordinates()).thenReturn(name);
                when(item.getDependencies()).thenReturn(Lists.newArrayList());

                if (library instanceof AndroidLibrary) {
                    AndroidLibrary androidLibrary = (AndroidLibrary) library;
                    addGraphItems(fullDependencies ? item.getDependencies() : result, globalMap,
                            seen, androidLibrary.getLibraryDependencies());
                } else if (library instanceof JavaLibrary) {
                    JavaLibrary javaLibrary = (JavaLibrary) library;
                    addGraphItems(fullDependencies ? item.getDependencies() : result, globalMap,
                            seen, javaLibrary.getDependencies());
                }
            }

            globalMap.put(name, createLevel2Library(library));
        }
    }

    @NonNull
    private com.android.builder.model.level2.Library createLevel2Library(Library library) {
        com.android.builder.model.level2.Library lib = mock(
                com.android.builder.model.level2.Library.class);

        MavenCoordinates coordinates = library.getResolvedCoordinates();
        String name = coordinates.getGroupId() + ':' + coordinates.getArtifactId() + ':'
                + coordinates.getVersion() + '@' + coordinates.getPackaging();
        when(lib.getArtifactAddress()).thenReturn(name);
        if (library instanceof AndroidLibrary) {
            AndroidLibrary androidLibrary = (AndroidLibrary) library;
            File folder = androidLibrary.getFolder();
            when(lib.getType()).thenReturn(
                    com.android.builder.model.level2.Library.LIBRARY_ANDROID);
            when(lib.getFolder()).thenReturn(folder);
            when(lib.getLintJar()).thenReturn("lint.jar");
            when(lib.getLocalJars()).thenReturn(Collections.emptyList());
            when(lib.getExternalAnnotations()).thenReturn(FN_ANNOTATIONS_ZIP);
            when(lib.getJarFile()).thenReturn("jars/" + FN_CLASSES_JAR);
            File jar = new File(folder, "jars/" + FN_CLASSES_JAR);
            if (!jar.exists()) {
                createEmptyJar(jar);
            }
            //when(l2.isProvided).thenReturn(androidLibrary.isProvided());
        } else if (library instanceof JavaLibrary) {
            JavaLibrary javaLibrary = (JavaLibrary) library;
            when(lib.getType()).thenReturn(com.android.builder.model.level2.Library.LIBRARY_JAVA);
            List<String> jars = Lists.newArrayList();
            when(lib.getLocalJars()).thenReturn(jars);
            File jarFile = javaLibrary.getJarFile();
            when(lib.getArtifact()).thenReturn(jarFile);
            when(lib.getFolder()).thenThrow(new UnsupportedOperationException());
        }
        return lib;
    }

    private void createEmptyJar(@NonNull File jar) {
        if (!jar.exists()) {
            File parentFile = jar.getParentFile();
            if (parentFile != null && !parentFile.isDirectory()) {
                //noinspection ResultOfMethodCallIgnored
                parentFile.mkdirs();
            }

            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

            try (JarOutputStream jarOutputStream = new JarOutputStream(
                    new BufferedOutputStream(new FileOutputStream(jar)), manifest)) {
                jarOutputStream.putNextEntry(new ZipEntry("dummy.txt"));
                ByteStreams.copy(new ByteArrayInputStream("Dummy".getBytes(Charsets.UTF_8)),
                        jarOutputStream);
                jarOutputStream.closeEntry();
            } catch (IOException e) {
                error(e.getMessage());
            }
        }
    }

    @NonNull
    private static String normalize(@NonNull String line) {
        line = line.trim();
        int commentIndex = line.indexOf("//");
        if (commentIndex != -1) {
            line = line.substring(0, commentIndex).trim();
        }
        while (true) {
            // Strip out embedded comment markers, if any (there could be multiple)
            commentIndex = line.indexOf("/*");
            if (commentIndex == -1) {
                break;
            }
            int commentEnd = line.indexOf("*/", commentIndex + 2);
            if (commentEnd == -1) {
                break;
            }
            line = line.substring(0, commentIndex) + line.substring(commentEnd + 2);
        }

        return line.replaceAll("\\s+", " ").replace('"', '\'').replace(" = ", " ");
    }

    private static char findNonSpaceCharacterBackwards(@NonNull String s, int index) {
        int curr = index;
        while (curr > 0) {
            char c = s.charAt(curr);
            if (!Character.isWhitespace(c)) {
                return c;
            }
            curr--;
        }

        return 0;
    }

    private void scan(@Language("Groovy") String gradle, @NonNull String context) {
        int start = 0;
        int end = gradle.length();
        while (start < end) {
            // Iterate line by line, but as soon as a line has an imbalance of {}'s
            // then report the block instead
            int lineEnd = gradle.indexOf('\n', start);

            // Join comma statements
            while (true) {
                if (findNonSpaceCharacterBackwards(gradle, lineEnd) == ',') {
                    lineEnd = gradle.indexOf('\n', lineEnd + 1);
                } else {
                    if (lineEnd == -1) {
                        lineEnd = end;
                    }
                    break;
                }
            }

            int balance = 0;
            for (int i = start; i < lineEnd; i++) {
                char c = gradle.charAt(i);
                if (c == '{') {
                    balance++;
                } else if (c == '}') {
                    balance--;
                }
            }

            if (balance == 0) {
                String line = gradle.substring(start, lineEnd).trim();
                int index = line.indexOf('{');
                if (line.endsWith("}") && index != -1) {
                    // Single line block?
                    String name = line.substring(0, index).trim();
                    @Language("Groovy")
                    String blockBody = line.substring(index + 1, line.length() - 1);
                    block(name, blockBody, context);
                } else {
                    line(line, context);
                }
                start = lineEnd + 1;
            } else {
                // Find end of block
                int nameEnd = gradle.indexOf('{', start);
                String name = gradle.substring(start, nameEnd).trim();
                start = lineEnd + 1;
                for (int i = lineEnd; i < end; i++) {
                    char c = gradle.charAt(i);
                    if (c == '{') {
                        balance++;
                    } else if (c == '}') {
                        balance--;
                        if (balance == 0) {
                            // Found the end
                            @Language("Groovy")
                            String block = gradle.substring(nameEnd + 1, i);
                            block(name, block, context);
                            start = i + 1;
                            break;
                        }
                    }
                }
            }
        }
    }

    @NonNull
    private static String getUnquotedValue(String key) {
        int index = key.indexOf('\'');
        if (index != -1) {
            return key.substring(index + 1, key.length() - 1);
        }
        index = key.indexOf(' ');
        if (index != -1) {
            return key.substring(index + 1, key.length());
        }
        return key;
    }

    private void line(@NonNull String line, @NonNull String context) {
        line = normalize(line);
        if (line.isEmpty()) {
            return;
        }

        if (line.equals("apply plugin: 'com.android.library'")
                || line.equals("apply plugin: 'android-library'")) {
            //noinspection deprecation
            when(project.isLibrary()).thenReturn(true);
            when(project.getProjectType()).thenReturn(AndroidProject.PROJECT_TYPE_LIBRARY);
            return;
        } else if (line.equals("apply plugin: 'com.android.application'")
                || line.equals("apply plugin: 'android'")) {
            //noinspection deprecation
            when(project.isLibrary()).thenReturn(false);
            when(project.getProjectType()).thenReturn(AndroidProject.PROJECT_TYPE_APP);
            return;
        } else if (line.equals("apply plugin: 'com.android.feature'")) {
            when(project.getProjectType()).thenReturn(AndroidProject.PROJECT_TYPE_FEATURE);
            return;
        } else if (line.equals("apply plugin: 'com.android.instantapp'")) {
            when(project.getProjectType()).thenReturn(AndroidProject.PROJECT_TYPE_INSTANTAPP);
            return;
        } else if (line.equals("apply plugin: 'java'")) {
            javaPlugin = true;
            return;
        } else if (line.equals("apply plugin: 'java-library'")) {
            javaLibraryPlugin = true;
            return;
        } else if (context.equals("buildscript.repositories")
                || context.equals("allprojects.repositories")) {
            // Plugins not modeled in the builder model
            return;
        }

        String key = context.isEmpty() ? line : context + "." + line;
        if (key.startsWith("dependencies.compile ") ||
                key.startsWith("dependencies.implementation ")) {
            String declaration = getUnquotedValue(key);
            if (GradleCoordinate.parseCoordinateString(declaration) != null) {
                addDependency(declaration, null, false);
                return;
            } else {
                // Group/artifact/version syntax?
                if (line.contains("group:") && line.contains("name:") &&
                        line.contains("version:")) {
                    String group = null;
                    String artifact = null;
                    String version = null;
                    for (String part : Splitter.on(',').trimResults().omitEmptyStrings().split(
                            line.substring(line.indexOf(' ') + 1))) {
                        if (part.startsWith("group:")) {
                            group = getUnquotedValue(part);
                        } else if (part.startsWith("name:")) {
                            artifact = getUnquotedValue(part);
                        } else if (part.startsWith("version:")) {
                            version = getUnquotedValue(part);
                        }
                    }
                    if (group != null && artifact != null && version != null) {
                        declaration = group + ':' + artifact + ':' + version;
                        addDependency(declaration, null, false);
                        return;
                    }
                }
            }
            warn("Ignored unrecognized dependency " + line);
        } else if (key.startsWith("dependencies.provided '") && key.endsWith("'")) {
            String declaration = getUnquotedValue(key);
            addDependency(declaration, null, true);
        } else if (line.startsWith("applicationId ") || line.startsWith("packageName ")) {
            String id = getUnquotedValue(key);
            ProductFlavor flavor = getFlavorFromContext(context);
            if (flavor != null) {
                when(flavor.getApplicationId()).thenReturn(id);
            } else {
                error("Unexpected flavor context " + context);
            }
        } else if (line.startsWith("minSdkVersion ")) {
            ApiVersion apiVersion = createApiVersion(key);
            ProductFlavor flavor = getFlavorFromContext(context);
            if (flavor != null) {
                when(flavor.getMinSdkVersion()).thenReturn(apiVersion);
            } else {
                error("Unexpected flavor context " + context);
            }
        } else if (line.startsWith("targetSdkVersion ")) {
            ApiVersion version = createApiVersion(key);
            ProductFlavor flavor = getFlavorFromContext(context);
            if (flavor != null) {
                when(flavor.getTargetSdkVersion()).thenReturn(version);
            } else {
                error("Unexpected flavor context " + context);
            }
        } else if (line.startsWith("versionCode ")) {
            String value = key.substring(key.indexOf(' ') + 1).trim();
            if (Character.isDigit(value.charAt(0))) {
                int number = Integer.decode(value);
                ProductFlavor flavor = getFlavorFromContext(context);
                if (flavor != null) {
                    when(flavor.getVersionCode()).thenReturn(number);
                } else {
                    error("Unexpected flavor context " + context);
                }
            } else {
                warn("Ignoring unrecognized versionCode token: " + value);
            }
        } else if (line.startsWith("versionName ")) {
            String name = getUnquotedValue(key);
            ProductFlavor flavor = getFlavorFromContext(context);
            if (flavor != null) {
                when(flavor.getVersionName()).thenReturn(name);
            } else {
                error("Unexpected flavor context " + context);
            }
        } else if (line.startsWith("versionNameSuffix ")) {
            String name = getUnquotedValue(key);
            ProductFlavor flavor = getFlavorFromContext(context);
            if (flavor != null) {
                when(flavor.getVersionNameSuffix()).thenReturn(name);
            } else {
                error("Unexpected flavor context " + context);
            }
        } else if (line.startsWith("applicationIdSuffix ")) {
            String name = getUnquotedValue(key);
            ProductFlavor flavor = getFlavorFromContext(context);
            if (flavor != null) {
                when(flavor.getApplicationIdSuffix()).thenReturn(name);
            } else {
                error("Unexpected flavor context " + context);
            }
        } else if (key.startsWith("android.resourcePrefix ")) {
            String value = getUnquotedValue(key);
            when(project.getResourcePrefix()).thenReturn(value);
        } else if (key.startsWith("android.buildToolsVersion ")) {
            String value = getUnquotedValue(key);
            when(project.getBuildToolsVersion()).thenReturn(value);
        } else if (line.startsWith("minifyEnabled ")
                && key.startsWith("android.buildTypes.")) {
            String name = key.substring("android.buildTypes.".length(),
                    key.indexOf(".minifyEnabled"));
            BuildType buildType = getBuildType(name, true);
            String value = getUnquotedValue(line);
            when(buildType.isMinifyEnabled()).thenReturn(VALUE_TRUE.equals(value));
        } else if (key.startsWith("android.compileSdkVersion ")) {
            String value = getUnquotedValue(key);
            when(project.getCompileTarget()).thenReturn(
                    Character.isDigit(value.charAt(0)) ? "android-" + value : value);
        } else if (line.startsWith("resConfig")) { // and resConfigs
            ProductFlavor flavor;
            if (context.startsWith("android.productFlavors.")) {
                String flavorName = context.substring("android.productFlavors.".length());
                flavor = getProductFlavor(flavorName, true);
            } else if (context.equals("android.defaultConfig")) {
                flavor = defaultFlavor;
            } else {
                error("Unexpected flavor " + context);
                return;
            }
            Collection<String> configs = flavor.getResourceConfigurations();
            for (String s : Splitter.on(",").trimResults()
                    .split(line.substring(line.indexOf(' ') + 1))) {
                if (!configs.contains(s)) {
                    configs.add(getUnquotedValue(s));
                }
            }
        } else if (key.startsWith("android.defaultConfig.vectorDrawables.useSupportLibrary ")) {
            String value = getUnquotedValue(key);
            if (VALUE_TRUE.equals(value)) {
                VectorDrawablesOptions options = getVectorDrawableOptions();
                when(options.getUseSupportLibrary()).thenReturn(true);
            }
        } else if (key.startsWith("android.compileOptions.sourceCompatibility JavaVersion.VERSION_")) {
            String s = key.substring(key.indexOf("VERSION_") + "VERSION_".length()).replace('_','.');
            when(compileOptions.getSourceCompatibility()).thenReturn(s);
        } else if (key.startsWith("android.compileOptions.targetCompatibility JavaVersion.VERSION_")) {
            String s = key.substring(key.indexOf("VERSION_") + "VERSION_".length()).replace('_','.');
            when(compileOptions.getTargetCompatibility()).thenReturn(s);
        } else if (key.startsWith("buildscript.dependencies.classpath ")) {
            if (key.contains("'com.android.tools.build:gradle:")) {
                String value = getUnquotedValue(key);
                GradleCoordinate gc = GradleCoordinate.parseCoordinateString(value);
                if (gc != null) {
                    modelVersion = GradleVersion.parse(gc.getRevision());
                    when(project.getModelVersion()).thenReturn(gc.getRevision());
                }
            } // else ignore other class paths
        } else if (key.startsWith("android.defaultConfig.testInstrumentationRunner ")
                || key.contains(".proguardFiles ")
                || key.startsWith("android.compileSdkVersion ")
                || key.equals("dependencies.compile fileTree(dir: 'libs', include: ['*.jar'])")
                || key.startsWith("dependencies.androidTestCompile('")) {
            // Ignored for now
        } else if (line.startsWith("manifestPlaceholders [") && key.startsWith("android.") && line
                .endsWith("]")) {
            // Example:
            // android.defaultConfig.manifestPlaceholders [ localApplicationId:'com.example.manifest_merger_example']
            Map<String, Object> manifestPlaceholders;
            if (context.startsWith("android.buildTypes.")) {
                String name = context.substring("android.buildTypes.".length());
                BuildType buildType = getBuildType(name, false);
                if (buildType != null) {
                    manifestPlaceholders = buildType.getManifestPlaceholders();
                } else {
                    error("Couldn't find flavor " + name + "; ignoring " + key);
                    return;
                }
            } else if (context.startsWith("android.productFlavors.")) {
                String name = context.substring("android.productFlavors.".length());
                ProductFlavor flavor = getProductFlavor(name, false);
                if (flavor != null) {
                    manifestPlaceholders = flavor.getManifestPlaceholders();
                } else {
                    error("Couldn't find flavor " + name + "; ignoring " + key);
                    return;
                }
            } else {
                manifestPlaceholders = defaultFlavor.getManifestPlaceholders();
            }

            String mapString = key.substring(key.indexOf('[') + 1, key.indexOf(']')).trim();

            // TODO: Support one than one more entry in the map? Comma separated list
            int index = mapString.indexOf(':');
            assert index != -1 : mapString;
            String mapKey = mapString.substring(0, index).trim();
            mapKey = getUnquotedValue(mapKey);
            String mapValue = mapString.substring(index + 1).trim();
            mapValue = getUnquotedValue(mapValue);
            manifestPlaceholders.put(mapKey, mapValue);
        } else if (key.startsWith("android.flavorDimensions ")) {
            String value = key.substring("android.flavorDimensions ".length());
            Collection<String> flavorDimensions = project.getFlavorDimensions();
            for (String s : Splitter.on(',').omitEmptyStrings().trimResults().split(value)) {
                String dimension = getUnquotedValue(s);
                if (!flavorDimensions.contains(dimension)) {
                    flavorDimensions.add(dimension);
                }
            }
        } else if (line.startsWith("flavorDimension ") &&
                key.startsWith("android.productFlavors.")) {
            String name = key.substring("android.productFlavors.".length(),
                    key.indexOf(".flavorDimension"));
            ProductFlavor productFlavor = getProductFlavor(name, true);
            String dimension = getUnquotedValue(line);
            when(productFlavor.getDimension()).thenReturn(dimension);
        } else if (key.startsWith("android.") && line.startsWith("resValue ")) {
            // Example:
            // android.defaultConfig.resValue 'string', 'defaultConfigName', 'Some DefaultConfig Data'
            int index = key.indexOf(".resValue ");
            String name = key.substring("android.".length(), index);

            Map<String, ClassField> resValues;
            if (name.startsWith("buildTypes.")) {
                name = name.substring("buildTypes.".length());
                BuildType buildType = getBuildType(name, false);
                if (buildType != null) {
                    resValues = buildType.getResValues();
                } else {
                    error("Couldn't find flavor " + name + "; ignoring " + key);
                    return;
                }
            } else if (name.startsWith("productFlavors.")) {
                name = name.substring("productFlavors.".length());
                ProductFlavor flavor = getProductFlavor(name, false);
                if (flavor != null) {
                    resValues = flavor.getResValues();
                } else {
                    error("Couldn't find flavor " + name + "; ignoring " + key);
                    return;
                }
            } else {
                assert name.indexOf('.') == -1 : name;
                resValues = defaultFlavor.getResValues();
            }

            String fieldName = null;
            String value = null;
            String type = null;
            String declaration = key.substring(index + ".resValue ".length());
            Splitter splitter = Splitter.on(',').trimResults().omitEmptyStrings();
            int resIndex = 0;
            for (String component : splitter.split(declaration)) {
                component = getUnquotedValue(component);
                switch (resIndex) {
                    case 0:
                        type = component;
                        break;
                    case 1:
                        fieldName = component;
                        break;
                    case 2:
                        value = component;
                        break;
                }

                resIndex++;
            }

            ClassField field = mock(ClassField.class);
            when(field.getName()).thenReturn(fieldName);
            when(field.getType()).thenReturn(type);
            when(field.getValue()).thenReturn(value);
            when(field.getDocumentation()).thenReturn("");
            when(field.getAnnotations()).thenReturn(Collections.emptySet());
            resValues.put(fieldName, field);
        } else if (context.startsWith("android.splits.")
                && context.indexOf('.', "android.splits.".length()) == -1) {
            String type = context.substring("android.splits.".length()).toUpperCase(Locale.ROOT);

            if (line.equals("reset")) {
                splits.removeAll(type);
            } else if (line.startsWith("include ")) {
                String value = line.substring("include ".length());
                for (String s : Splitter.on(',').trimResults().omitEmptyStrings().split(value)) {
                    s = getUnquotedValue(s);
                    splits.put(type, s);
                }
            } else if (line.startsWith("exclude ")) {
                warn("Warning: Split exclude not supported for mocked builder model yet");
            }
        } else if (key.startsWith("android.lintOptions.")) {
            key = key.substring("android.lintOptions.".length());
            int argIndex = key.indexOf(' ');
            if (argIndex == -1) {
                error("No value supplied for lint option " + key);
                return;
            }
            String arg = key.substring(argIndex).trim();
            key = key.substring(0, argIndex);

            switch (key) {
                case "quiet":
                    when(lintOptions.isQuiet()).thenReturn(Boolean.valueOf(arg));
                    break;
                case "abortOnError":
                    when(lintOptions.isAbortOnError()).thenReturn(Boolean.valueOf(arg));
                    break;
                case "checkReleaseBuilds":
                    when(lintOptions.isCheckReleaseBuilds()).thenReturn(Boolean.valueOf(arg));
                    break;
                case "ignoreWarnings":
                    when(lintOptions.isIgnoreWarnings()).thenReturn(Boolean.valueOf(arg));
                    break;
                case "absolutePaths":
                    when(lintOptions.isAbsolutePaths()).thenReturn(Boolean.valueOf(arg));
                    break;
                case "checkAllWarnings":
                    when(lintOptions.isCheckAllWarnings()).thenReturn(Boolean.valueOf(arg));
                    break;
                case "warningsAsErrors":
                    when(lintOptions.isWarningsAsErrors()).thenReturn(Boolean.valueOf(arg));
                    break;
                case "noLines":
                    when(lintOptions.isNoLines()).thenReturn(Boolean.valueOf(arg));
                    break;
                case "showAll":
                    when(lintOptions.isShowAll()).thenReturn(Boolean.valueOf(arg));
                    break;
                case "explainIssues":
                    when(lintOptions.isExplainIssues()).thenReturn(Boolean.valueOf(arg));
                    break;
                case "textReport":
                    when(lintOptions.getTextReport()).thenReturn(Boolean.valueOf(arg));
                    break;
                case "xmlReport":
                    when(lintOptions.getXmlReport()).thenReturn(Boolean.valueOf(arg));
                    break;
                case "htmlReport":
                    when(lintOptions.getHtmlReport()).thenReturn(Boolean.valueOf(arg));
                    break;
                case "checkTestSources":
                    when(lintOptions.isCheckTestSources()).thenReturn(Boolean.valueOf(arg));
                    break;
                case "checkGeneratedSources":
                    when(lintOptions.isCheckGeneratedSources()).thenReturn(Boolean.valueOf(arg));
                    break;
                case "enable": {
                    for (String s : Splitter.on(',').trimResults().omitEmptyStrings().split(arg)) {
                        lintOptions.getEnable().add(stripQuotes(s, true));
                    }
                    break;
                }
                case "disable": {
                    for (String s : Splitter.on(',').trimResults().omitEmptyStrings().split(arg)) {
                        lintOptions.getDisable().add(stripQuotes(s, true));
                    }
                    break;
                }
                case "check": {
                    for (String s : Splitter.on(',').trimResults().omitEmptyStrings().split(arg)) {
                        lintOptions.getCheck().add(stripQuotes(s, true));
                    }
                    break;
                }
                case "fatal": {
                    for (String s : Splitter.on(',').trimResults().omitEmptyStrings().split(arg)) {
                        lintOptions.getSeverityOverrides().put(stripQuotes(s, true),
                                LintOptions.SEVERITY_FATAL);
                    }
                    break;
                }
                case "error": {
                    for (String s : Splitter.on(',').trimResults().omitEmptyStrings().split(arg)) {
                        lintOptions.getSeverityOverrides().put(stripQuotes(s, true),
                                LintOptions.SEVERITY_ERROR);
                    }
                    break;
                }
                case "warning": {
                    for (String s : Splitter.on(',').trimResults().omitEmptyStrings().split(arg)) {
                        lintOptions.getSeverityOverrides().put(stripQuotes(s, true),
                                LintOptions.SEVERITY_WARNING);
                    }
                    break;
                }
                case "informational": {
                    for (String s : Splitter.on(',').trimResults().omitEmptyStrings().split(arg)) {
                        lintOptions.getSeverityOverrides().put(stripQuotes(s, true),
                                LintOptions.SEVERITY_INFORMATIONAL);
                    }
                    break;
                }
                case "ignore": {
                    for (String s : Splitter.on(',').trimResults().omitEmptyStrings().split(arg)) {
                        lintOptions.getSeverityOverrides().put(stripQuotes(s, true),
                                LintOptions.SEVERITY_IGNORE);
                    }
                    break;
                }

                case "lintConfig": {
                    when(lintOptions.getLintConfig()).thenReturn(file(arg, true));
                    break;
                }
                case "textOutput": {
                    when(lintOptions.getTextOutput()).thenReturn(file(arg, true));
                    break;
                }
                case "xmlOutput": {
                    when(lintOptions.getXmlOutput()).thenReturn(file(arg, true));
                    break;
                }
                case "htmlOutput": {
                    when(lintOptions.getHtmlOutput()).thenReturn(file(arg, true));
                    break;
                }
                case "baseline": {
                    when(lintOptions.getBaselineFile()).thenReturn(file(arg, true));
                    break;
                }
            }
        } else {
            warn("ignored line: " + line + ", context=" + context);
        }
    }

    @NonNull
    private File file(String gradle, boolean reportError) {
        if (gradle.startsWith("file(\"") && gradle.endsWith("\")") ||
                gradle.startsWith("file('") && gradle.endsWith("')")) {
            String path = gradle.substring(6, gradle.length() - 2);
            return new File(projectDir, path);
        }
        gradle = stripQuotes(gradle, true);
        if (gradle.equals("stdout") || gradle.equals("stderr")) {
            return new File(gradle);
        }
        if (reportError) {
            error("Only support file(\"\") paths in gradle mocker");
        }
        return new File(gradle);
    }

    private String stripQuotes(String string, boolean reportError) {
        if (string.startsWith("'") && string.endsWith("'") && string.length() >= 2) {
            return string.substring(1, string.length() - 1);
        }
        if (string.startsWith("\"") && string.endsWith("\"") && string.length() >= 2) {
            return string.substring(1, string.length() - 1);
        }
        if (reportError) {
            error("Expected quotes around " + string);
        }
        return string;
    }

    @Nullable
    private ProductFlavor getFlavorFromContext(@NonNull String context) {
        if (context.equals("android.defaultConfig")) {
            return defaultFlavor;
        } else if (context.startsWith("android.productFlavors.")) {
            String name = context.substring("android.productFlavors.".length());
            return getProductFlavor(name, true);
        } else {
            return null;
        }
    }

    private VectorDrawablesOptions getVectorDrawableOptions() {
        if (vectorDrawablesOptions == null) {
            vectorDrawablesOptions = mock(VectorDrawablesOptions.class);
            when(mergedFlavor.getVectorDrawables()).thenReturn(vectorDrawablesOptions);
        }
        return vectorDrawablesOptions;
    }

    @Contract("_,true -> !null")
    @Nullable
    private BuildType getBuildType(@NonNull String name, boolean create) {
        for (BuildType type : buildTypes) {
            if (type.getName().equals(name)) {
                return type;
            }
        }

        if (create) {
            return createBuildType(name);
        }

        return null;
    }

    @NonNull
    private BuildType createBuildType(@NonNull String name) {
        BuildType buildType = mock(BuildType.class);
        when(buildType.getName()).thenReturn(name);
        when(buildType.isDebuggable()).thenReturn(name.equals("debug"));
        buildTypes.add(buildType);
        // Creating mutable map here which we can add to later
        when(buildType.getResValues()).thenReturn(Maps.newHashMap());
        when(buildType.getManifestPlaceholders()).thenReturn(Maps.newHashMap());

        return buildType;
    }

    private void block(@NonNull String name, @Language("Groovy") @NonNull String blockBody,
            @NonNull String context) {
        if ("android.productFlavors".equals(context)) {
            // Defining new product flavors
            createProductFlavor(name);
        }
        if ("android.buildTypes".equals(context)) {
            // Defining new build types
            createBuildType(name);
        }

        scan(blockBody, context.isEmpty() ? name : context + "." + name);
    }

    @Contract("_,true -> !null")
    private ProductFlavor getProductFlavor(@NonNull String name, boolean create) {
        for (ProductFlavor flavor : productFlavors) {
            if (flavor.getName().equals(name)) {
                return flavor;
            }
        }

        if (create) {
            return createProductFlavor(name);
        }

        return null;
    }

    @NonNull
    private ProductFlavor createProductFlavor(@NonNull String name) {
        ProductFlavor flavor = mock(ProductFlavor.class);
        when(flavor.getName()).thenReturn(name);
        // Creating mutable map here which we can add to later
        when(flavor.getResValues()).thenReturn(Maps.newHashMap());
        when(flavor.getManifestPlaceholders()).thenReturn(Maps.newHashMap());
        // Creating mutable list here which we can add to later
        when(flavor.getResourceConfigurations()).thenReturn(Lists.newArrayList());

        productFlavors.add(flavor);
        return flavor;
    }

    @NonNull
    private static AndroidArtifactOutput createAndroidArtifactOutput(
            @NonNull String filterType,
            @NonNull String identifier) {
        AndroidArtifactOutput artifactOutput = mock(
                AndroidArtifactOutput.class);

        if (filterType.isEmpty()) {
            when(artifactOutput.getFilterTypes()).thenReturn(Collections.emptyList());
            when(artifactOutput.getFilters()).thenReturn(Collections.emptyList());
        } else {
            when(artifactOutput.getFilterTypes()).thenReturn(Collections.singletonList(filterType));
            List<FilterData> filters = Lists.newArrayList();
            FilterData filter = mock(FilterData.class);
            when(filter.getFilterType()).thenReturn(filterType);
            when(filter.getIdentifier()).thenReturn(identifier);
            filters.add(filter);
            when(artifactOutput.getFilters()).thenReturn(filters);
        }

        return artifactOutput;
    }

    @NonNull
    private static SourceProvider createSourceProvider(@NonNull File root, @NonNull String name) {
        SourceProvider provider = mock(SourceProvider.class);
        when(provider.getName()).thenReturn(name);
        when(provider.getManifestFile()).thenReturn(
                new File(root, "src/" + name + "/" + ANDROID_MANIFEST_XML));
        List<File> resDirectories = Lists.newArrayListWithCapacity(2);
        List<File> javaDirectories = Lists.newArrayListWithCapacity(2);
        resDirectories.add(new File(root, "src/" + name + "/res"));
        javaDirectories.add(new File(root, "src/" + name + "/java"));
        javaDirectories.add(new File(root, "src/" + name + "/kotlin"));

        // Add generated source provider to let us test generated source handling
        if ("main".equals(name)) {
            File generated = new File(root, "generated");
            if (generated.exists()) {
                javaDirectories.add(generated);
            }
        }

        when(provider.getResDirectories()).thenReturn(resDirectories);
        when(provider.getJavaDirectories()).thenReturn(javaDirectories);

        // TODO: other file types
        return provider;
    }

    @NonNull
    private static ApiVersion createApiVersion(@NonNull String value) {
        ApiVersion version = mock(ApiVersion.class);
        String s = value.substring(value.indexOf(' ') + 1);
        if (s.startsWith("'")) {
            String codeName = getUnquotedValue(s);
            AndroidVersion sdkVersion = SdkVersionInfo.getVersion(codeName, null);
            if (sdkVersion != null) {
                when(version.getCodename()).thenReturn(sdkVersion.getCodename());
                when(version.getApiString()).thenReturn(sdkVersion.getApiString());
                when(version.getApiLevel()).thenReturn(sdkVersion.getApiLevel());
            }
        } else {
            when(version.getApiString()).thenReturn(s);
            when(version.getCodename()).thenReturn(null);
            when(version.getApiLevel()).thenReturn(Integer.parseInt(s));
        }
        return version;
    }

    private void addDependency(String declaration, String scope, boolean isProvided) {
        // If it's one of the common libraries, built up the full dependency graph
        // that we know will actually be used
        if (declaration.startsWith("com.android.support:appcompat-v7:")) {
            String version = declaration.substring("com.android.support:appcompat-v7:".length());
            addTransitiveLibrary((""
                    + "+--- com.android.support:appcompat-v7:VERSION\n"
                    + "|    +--- com.android.support:support-v4:VERSION\n"
                    + "|    |    +--- com.android.support:support-compat:VERSION\n"
                    + "|    |    |    \\--- com.android.support:support-annotations:VERSION\n"
                    + "|    |    +--- com.android.support:support-media-compat:VERSION\n"
                    + "|    |    |    \\--- com.android.support:support-compat:VERSION (*)\n"
                    + "|    |    +--- com.android.support:support-core-utils:VERSION\n"
                    + "|    |    |    \\--- com.android.support:support-compat:VERSION (*)\n"
                    + "|    |    +--- com.android.support:support-core-ui:VERSION\n"
                    + "|    |    |    \\--- com.android.support:support-compat:VERSION (*)\n"
                    + "|    |    \\--- com.android.support:support-fragment:VERSION\n"
                    + "|    |         +--- com.android.support:support-compat:VERSION (*)\n"
                    + "|    |         +--- com.android.support:support-media-compat:VERSION (*)\n"
                    + "|    |         +--- com.android.support:support-core-ui:VERSION (*)\n"
                    + "|    |         \\--- com.android.support:support-core-utils:VERSION (*)\n"
                    + "|    +--- com.android.support:support-vector-drawable:VERSION\n"
                    + "|    |    \\--- com.android.support:support-compat:VERSION (*)\n"
                    + "|    \\--- com.android.support:animated-vector-drawable:VERSION\n"
                    + "|         \\--- com.android.support:support-vector-drawable:VERSION (*)\n")
                    .replace("VERSION", version));

        } else if (declaration.startsWith("com.android.support:support-v4:")) {
            String version = declaration.substring("com.android.support:support-v4:".length());
            addTransitiveLibrary((""
                    + "+--- com.android.support:support-v4:VERSION\n"
                    + "|    +--- com.android.support:support-compat:VERSION\n"
                    + "|    |    \\--- com.android.support:support-annotations:VERSION\n"
                    + "|    +--- com.android.support:support-media-compat:VERSION\n"
                    + "|    |    \\--- com.android.support:support-compat:VERSION (*)\n"
                    + "|    +--- com.android.support:support-core-utils:VERSION\n"
                    + "|    |    \\--- com.android.support:support-compat:VERSION (*)\n"
                    + "|    +--- com.android.support:support-core-ui:VERSION\n"
                    + "|    |    \\--- com.android.support:support-compat:VERSION (*)\n"
                    + "|    \\--- com.android.support:support-fragment:VERSION\n"
                    + "|         +--- com.android.support:support-compat:VERSION (*)\n"
                    + "|         +--- com.android.support:support-media-compat:VERSION (*)\n"
                    + "|         +--- com.android.support:support-core-ui:VERSION (*)\n"
                    + "|         \\--- com.android.support:support-core-utils:VERSION (*)\n")
                    .replace("VERSION", version));
        } else if (declaration.startsWith("com.android.support.constraint:constraint-layout:")) {
            String version = declaration.substring(
                    "com.android.support.constraint:constraint-layout:".length());
            addTransitiveLibrary((""
                    + "+--- com.android.support.constraint:constraint-layout:VERSION\n"
                    + "     \\--- com.android.support.constraint:constraint-layout-solver:VERSION\n")
                    .replace("VERSION", version));
        } else if (declaration.startsWith("com.firebase:firebase-client-android:")) {
            String version = declaration.substring("com.firebase:firebase-client-android:".length());
            addTransitiveLibrary((""
                    + "\\--- com.firebase:firebase-client-android:VERSION\n"
                    + "     \\--- com.firebase:firebase-client-jvm:VERSION\n"
                    + "          +--- com.fasterxml.jackson.core:jackson-databind:2.2.2\n"
                    + "          |    +--- com.fasterxml.jackson.core:jackson-annotations:2.2.2\n"
                    + "          |    \\--- com.fasterxml.jackson.core:jackson-core:2.2.2\n"
                    + "          \\--- com.firebase:tubesock:0.0.12")
                    .replace("VERSION", version));
        } else if (declaration.startsWith("com.android.support:design:")) {
            // Design library
            String version = declaration.substring("com.android.support:design:".length());
            addTransitiveLibrary((""
                    + "+--- com.android.support:design:VERSION\n"
                    + "|    +--- com.android.support:recyclerview-v7:VERSION\n"
                    + "|    |    +--- com.android.support:support-annotations:VERSION\n"
                    + "|    |    \\--- com.android.support:support-v4:VERSION (*)\n"
                    + "|    +--- com.android.support:appcompat-v7:VERSION (*)\n"
                    + "|    \\--- com.android.support:support-v4:VERSION (*)")
                    .replace("VERSION", version));
        } else if (declaration.startsWith("com.google.android.gms:play-services-analytics:")) {
            // Analytics
            String version = declaration.substring("com.google.android.gms:play-services-analytics:".length());
            addTransitiveLibrary((""
                    + "+--- com.google.android.gms:play-services-analytics:VERSION\n"
                    + "|    \\--- com.google.android.gms:play-services-basement:VERSION\n"
                    + "|         \\--- com.android.support:support-v4:23.0.0 -> 23.4.0\n"
                    + "|              \\--- com.android.support:support-annotations:23.4.0")
                    .replace("VERSION", version));
        } else if (declaration.startsWith("com.google.android.gms:play-services-gcm:")) {
            // GMS
            String version = declaration.substring("com.google.android.gms:play-services-gcm:".length());
            addTransitiveLibrary((""
                    + "+--- com.google.android.gms:play-services-gcm:VERSION\n"
                    + "|    +--- com.google.android.gms:play-services-base:VERSION (*)\n"
                    + "|    \\--- com.google.android.gms:play-services-measurement:VERSION\n"
                    + "|         \\--- com.google.android.gms:play-services-basement:VERSION (*)")
                    .replace("VERSION", version));
        } else if (declaration.startsWith("com.google.android.gms:play-services-appindexing:")) {
            // App Indexing
            String version = declaration.substring("com.google.android.gms:play-services-appindexing:".length());
            addTransitiveLibrary((""
                    + "+--- com.google.android.gms:play-services-appindexing:VERSION\n"
                    + "|    \\--- com.google.android.gms:play-services-base:VERSION\n"
                    + "|         \\--- com.google.android.gms:play-services-basement:VERSION (*)")
                    .replace("VERSION", version));
        } else {
            // Look for the library in the dependency graph provided
            Dep dep = graphs.get(declaration);
            if (dep != null) {
                addLibrary(dep);
            } else if (isJavaLibrary(declaration)) {
                // Not found in dependency graphs: create a single Java library
                JavaLibrary library = createJavaLibrary(declaration, isProvided);
                javaLibraries.add(library);
            } else {
                // Not found in dependency graphs: create a single Android library
                AndroidLibrary library = createAndroidLibrary(declaration, isProvided);
                androidLibraries.add(library);
            }
        }
    }

    private void addTransitiveLibrary(@NonNull String graph) {
        for (Dep dep : parseDependencyGraph(graph)) {
            addLibrary(dep);
        }
    }

    private void addLibrary(@NonNull Dep dep) {
        Library library = dep.createLibrary();
        if (library instanceof AndroidLibrary) {
            androidLibraries.add((AndroidLibrary) library);
        } else if (library instanceof JavaLibrary) {
            javaLibraries.add((JavaLibrary) library);
        }
    }

    /**
     * Returns whether a library declaration is a plain Java library instead of an
     * Android library. There is no way to tell from the Gradle description; it involves
     * looking at the actual Maven artifacts. For mocking purposes we have a hardcoded
     * list.
     */
    private static boolean isJavaLibrary(@NonNull String declaration) {
        if (declaration.startsWith("com.android.support:support-annotations:")) {
            return true;
        } else if (declaration.startsWith("com.android.support:support-v4:")
            || declaration.startsWith("com.android.support:support-v13:")) {
            // Jar prior to to v20
            return declaration.contains(":13")
                    || declaration.contains(":18")
                    || declaration.contains(":19");
        } else if (declaration.startsWith("com.google.guava:guava:")) {
            return true;
        } else if (declaration.startsWith("com.google.android.wearable:wearable:")) {
            return true;
        } else if (declaration.startsWith("com.android.support.constraint:constraint-layout-solver:")) {
            return true;
        } else if (declaration.startsWith("junit:junit:")) {
            return true;
        }
        return false;
    }

    @NonNull
    private AndroidLibrary createAndroidLibrary(String coordinateString, boolean isProvided) {
        return createAndroidLibrary(coordinateString, null, isProvided);
    }

    private AndroidLibrary createAndroidLibrary(String coordinateString,
            @Nullable String promotedTo, boolean isProvided) {
        GradleCoordinate coordinate = GradleCoordinate.parseCoordinateString(coordinateString);
        TestCase.assertNotNull(coordinateString, coordinate);
        MavenCoordinates mavenCoordinates = mock(MavenCoordinates.class);
        when(mavenCoordinates.getGroupId()).thenReturn(coordinate.getGroupId());
        when(mavenCoordinates.getArtifactId()).thenReturn(coordinate.getArtifactId());
        when(mavenCoordinates.getVersion()).thenReturn(coordinate.getRevision());
        when(mavenCoordinates.getVersionlessId()).thenReturn(
                coordinate.getGroupId() + ':' + coordinate.getArtifactId());
        when(mavenCoordinates.getPackaging()).thenReturn("aar");
        when(mavenCoordinates.toString()).thenReturn(coordinateString);

        AndroidLibrary library = mock(AndroidLibrary.class);
        when(library.getRequestedCoordinates()).thenReturn(mavenCoordinates);
        if (promotedTo != null) {
            mavenCoordinates = mock(MavenCoordinates.class);
            when(mavenCoordinates.getGroupId()).thenReturn(coordinate.getGroupId());
            when(mavenCoordinates.getArtifactId()).thenReturn(coordinate.getArtifactId());
            when(mavenCoordinates.getVersion()).thenReturn(promotedTo);
            when(mavenCoordinates.getVersionlessId()).thenReturn(
                    coordinate.getGroupId() + ':' + coordinate.getArtifactId());
            when(mavenCoordinates.getPackaging()).thenReturn("aar");
            when(mavenCoordinates.toString()).thenReturn(
                    coordinateString.replace(coordinate.getRevision(), promotedTo));
            when(library.getResolvedCoordinates()).thenReturn(mavenCoordinates);
        } else {
            when(library.getResolvedCoordinates()).thenReturn(mavenCoordinates);
        }
        File dir;
        if (useBuildCache) {
            // Not what build cache uses, but we just want something stable and unique
            // for tests
            String hash = Hashing.sha1().hashString(coordinateString, Charsets.UTF_8).toString();
            dir = new File(FileUtils.join(System.getProperty("user.home"),
                    ".android", "build-cache", hash, "output"));
        } else {
            dir = new File(projectDir, "build/intermediates/exploded-aar/"
                    + coordinate.getGroupId() + "/"
                    + coordinate.getArtifactId() + "/" + coordinate.getRevision());
        }
        when(library.getFolder()).thenReturn(dir);
        when(library.getLintJar()).thenReturn(new File(dir, "lint.jar"));
        when(library.isProvided()).thenReturn(isProvided);
        when(library.getLocalJars()).thenReturn(Collections.emptyList());
        when(library.getExternalAnnotations()).thenReturn(new File(dir, FN_ANNOTATIONS_ZIP));
        File jar = new File(dir, "jars/" + FN_CLASSES_JAR);
        if (!jar.exists()) {
            createEmptyJar(jar);
        }
        when(library.getJarFile()).thenReturn(jar);

        return library;
    }

    @NonNull
    private JavaLibrary createJavaLibrary(@NonNull String coordinateString,
            boolean isProvided) {
        return createJavaLibrary(coordinateString, null, isProvided);
    }

    @NonNull
    private JavaLibrary createJavaLibrary(@NonNull String coordinateString,
            @Nullable String promotedTo, boolean isProvided) {
        GradleCoordinate coordinate = GradleCoordinate.parseCoordinateString(coordinateString);
        TestCase.assertNotNull(coordinate);
        MavenCoordinates mavenCoordinates = mock(MavenCoordinates.class);
        when(mavenCoordinates.getGroupId()).thenReturn(coordinate.getGroupId());
        when(mavenCoordinates.getArtifactId()).thenReturn(coordinate.getArtifactId());
        when(mavenCoordinates.getVersion()).thenReturn(coordinate.getRevision());
        when(mavenCoordinates.getVersionlessId()).thenReturn(
                coordinate.getGroupId() + ':' + coordinate.getArtifactId());
        when(mavenCoordinates.getPackaging()).thenReturn("jar");
        when(mavenCoordinates.toString()).thenReturn(coordinateString);

        JavaLibrary library = mock(JavaLibrary.class);
        when(library.getRequestedCoordinates()).thenReturn(mavenCoordinates);
        if (promotedTo != null) {
            mavenCoordinates = mock(MavenCoordinates.class);
            when(mavenCoordinates.getGroupId()).thenReturn(coordinate.getGroupId());
            when(mavenCoordinates.getArtifactId()).thenReturn(coordinate.getArtifactId());
            when(mavenCoordinates.getVersion()).thenReturn(promotedTo);
            when(mavenCoordinates.getVersionlessId()).thenReturn(
                    coordinate.getGroupId() + ':' + coordinate.getArtifactId());
            when(mavenCoordinates.getPackaging()).thenReturn("jar");
            when(mavenCoordinates.toString()).thenReturn(
                    coordinateString.replace(coordinate.getRevision(), promotedTo));
            when(library.getResolvedCoordinates()).thenReturn(mavenCoordinates);
        } else {
            when(library.getResolvedCoordinates()).thenReturn(mavenCoordinates);
        }
        when(library.isProvided()).thenReturn(isProvided);
        when(library.getName()).thenReturn(coordinate.toString());
        when(library.toString()).thenReturn(coordinate.toString());

        File jar = new File(projectDir,"caches/modules-2/files-2.1/"
                + coordinate.getGroupId() + "/"
                + coordinate.getArtifactId() + "/" + coordinate.getRevision() +
                // Usually some hex string here, but keep same to keep test behavior stable
                "9c6ef172e8de35fd8d4d8783e4821e57cdef7445/"
                + coordinate.getArtifactId() + "-" + coordinate.getRevision() +
                DOT_JAR);
        if (!jar.exists()) {
            createEmptyJar(jar);
        }
        when(library.getJarFile()).thenReturn(jar);
        when(library.isProvided()).thenReturn(isProvided);

        return library;
    }

    @NonNull
    public File getProjectDir() {
        return projectDir;
    }

    public void setVariantName(@NonNull String variantName) {
        ensureInitialized();

        // For something like debugFreeSubscription, set the variant's build type
        // to "debug", and the flavor set to ["free", "subscription"]
        when(variant.getName()).thenReturn(variantName);
        Splitter splitter = Splitter.on('_');
        List<String> flavors = Lists.newArrayList();
        for (String s : splitter.split(SdkVersionInfo.camelCaseToUnderlines(variantName))) {
            BuildType buildType = getBuildType(s, false);
            //noinspection VariableNotUsedInsideIf
            if (buildType != null) {
                when(variant.getBuildType()).thenReturn(s);
            } else {
                ProductFlavor flavor = getProductFlavor(s, false);
                //noinspection VariableNotUsedInsideIf
                if (flavor != null) {
                    flavors.add(s);
                }
            }
        }

        when(variant.getProductFlavors()).thenReturn(flavors);
    }

    /**
     * Given a dependency graph, returns a populated {@link Dependencies} object.
     * You can generate Gradle dependency graphs by running for example:
     * <pre>
     *     $ ./gradlew :app:dependencies
     * </pre>
     * <p>
     * Sample graph:
     * <pre>
     * \--- com.android.support.test.espresso:espresso-core:2.2.2
     *      +--- com.squareup:javawriter:2.1.1
     *      +--- com.android.support.test:rules:0.5
     *      |    \--- com.android.support.test:runner:0.5
     *      |         +--- junit:junit:4.12
     *      |         |    \--- org.hamcrest:hamcrest-core:1.3
     *      |         \--- com.android.support.test:exposed-instrumentation-api-publish:0.5
     *      +--- com.android.support.test:runner:0.5 (*)
     *      +--- javax.inject:javax.inject:1
     *      +--- org.hamcrest:hamcrest-library:1.3
     *      |    \--- org.hamcrest:hamcrest-core:1.3
     *      +--- com.android.support.test.espresso:espresso-idling-resource:2.2.2
     *      +--- org.hamcrest:hamcrest-integration:1.3
     *      |    \--- org.hamcrest:hamcrest-library:1.3 (*)
     *      +--- com.google.code.findbugs:jsr305:2.0.1
     *      \--- javax.annotation:javax.annotation-api:1.2
     * </pre>
     *
     * @param graph the graph
     * @return the corresponding dependencies
     */
    @VisibleForTesting
    @NonNull
    Dependencies createDependencies(@NonNull String graph) {
        List<Dep> deps = parseDependencyGraph(graph);
        return createDependencies(deps);
    }

    @NonNull
    List<Dep> parseDependencyGraph(@NonNull String graph) {
        return parseDependencyGraph(graph, Maps.newHashMap());
    }

    @NonNull
    List<Dep> parseDependencyGraph(@NonNull String graph, @NonNull Map<String, Dep> map) {
        String[] lines = graph.split("\n");
        // TODO: Check that it's using the expected graph format - e.g. indented to levels
        // that are multiples of 5
        if (lines.length == 0) {
            return Collections.emptyList();
        }

        Dep root = new Dep("", 0);
        Deque<Dep> stack = new ArrayDeque<>();
        stack.push(root);
        Dep parent = root;
        for (String line : lines) {
            int depth = getDepth(line);
            Dep dep = new Dep(line.substring(getIndent(line)), depth);
            map.put(dep.coordinateString, dep);
            if (depth == parent.depth + 1) {
                // Just to append to parent
                parent.add(dep);
            } else if (depth == parent.depth + 2) {
                Dep lastChild = parent.getLastChild();
                if (lastChild != null) {
                    lastChild.add(dep);
                    stack.push(lastChild);
                    parent = lastChild;
                } else {
                    parent.add(dep);
                }
            } else {
                while (true) {
                    stack.pop();
                    parent = stack.peek();
                    if (parent.depth == depth - 1) {
                        parent.add(dep);
                        break;
                    }
                }
            }
        }

        return root.children;
    }

    private static int getDepth(@NonNull String line) {
        return getIndent(line) / 5;
    }

    private static int getIndent(@NonNull String line) {
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (Character.isLetter(c)) {
                return i;
            }
        }
        return line.length();
    }

    @NonNull
    private Dependencies createDependencies(@NonNull List<Dep> deps) {
        Dependencies dependencies = mock(Dependencies.class);
        List<AndroidLibrary> androidLibraries = createAndroidLibraries(deps);
        List<JavaLibrary> javaLibraries = createJavaLibraries(deps);
        List<String> projects = Lists.newArrayList();
        for (AndroidLibrary library : androidLibraries) {
            String name = library.getName();
            if (name != null && !projects.contains(name)) {
                projects.add(name);
            }
        }
        when(dependencies.getLibraries()).thenReturn(androidLibraries);
        when(dependencies.getJavaLibraries()).thenReturn(javaLibraries);
        //noinspection deprecation
        when(dependencies.getProjects()).thenReturn(projects);

        return dependencies;
    }

    @NonNull
    private List<AndroidLibrary> createAndroidLibraries(@NonNull List<Dep> deps) {
        List<AndroidLibrary> androidLibraries = Lists.newArrayList();
        for (Dep dep : deps) {
            if (!dep.isJavaLibrary()) {
                AndroidLibrary androidLibrary = dep.createAndroidLibrary();
                androidLibraries.add(androidLibrary);
            }
        }

        return androidLibraries;
    }

    @NonNull
    private List<JavaLibrary> createJavaLibraries(@NonNull List<Dep> deps) {
        List<JavaLibrary> javaLibraries = Lists.newArrayList();
        for (Dep dep : deps) {
            if (dep.isJavaLibrary()) {
                JavaLibrary javaLibrary = dep.createJavaLibrary();
                javaLibraries.add(javaLibrary);
            }
        }

        return javaLibraries;
    }

    /** Dependency graph node */
    private class Dep {
        public final GradleCoordinate coordinate;
        public final String coordinateString;
        public final String promotedTo;
        public final List<Dep> children = Lists.newArrayList();
        public final int depth;

        public Dep(String coordinateString, int depth) {
            int promoted = coordinateString.indexOf(" -> ");
            if (promoted != -1) {
                promotedTo = coordinateString.substring(promoted + 4);
                coordinateString = coordinateString.substring(0, promoted);
            } else {
                promotedTo = null;
            }
            if (coordinateString.endsWith(" (*)")) {
                coordinateString = coordinateString.substring(0,
                        coordinateString.length() - " (*)".length());
            }
            this.coordinateString = coordinateString;
            this.coordinate = !coordinateString.isEmpty()
                    ? GradleCoordinate.parseCoordinateString(coordinateString) : null;
            this.depth = depth;
        }

        private void add(Dep child) {
            children.add(child);
        }

        public boolean isJavaLibrary() {
            return GradleModelMocker.isJavaLibrary(coordinateString);
        }

        public boolean isProject() {
            return coordinate == null && coordinateString.startsWith("project ");
        }

        @NonNull
        Library createLibrary() {
            if (isJavaLibrary()) {
                return createJavaLibrary();
            } else {
                return createAndroidLibrary();
            }
        }

        private AndroidLibrary createAndroidLibrary() {
            AndroidLibrary androidLibrary;
            if (isProject()) {
                androidLibrary = mock(AndroidLibrary.class);
                String name = coordinateString.substring("project ".length());
                when(androidLibrary.getProject()).thenReturn(name);
                when(androidLibrary.getName()).thenReturn(name);
            } else {
                androidLibrary = GradleModelMocker.this.createAndroidLibrary(coordinateString,
                        promotedTo,false);
            }
            if (!children.isEmpty()) {
                // We can't store these in the dependencies object but store it for the
                // global dependency list
                List<JavaLibrary> jc = createJavaLibraries(children);
                allJavaLibraries.addAll(jc);

                List<AndroidLibrary> ac = createAndroidLibraries(children);
                // Work around wildcard capture
                OngoingStubbing<? extends List<? extends AndroidLibrary>> stub2 = when(
                        androidLibrary.getLibraryDependencies());
                //noinspection unchecked,RedundantCast
                ((OngoingStubbing<List<? extends AndroidLibrary>>) stub2).thenReturn(ac);
            }
            return androidLibrary;
        }

        private JavaLibrary createJavaLibrary() {
            JavaLibrary javaLibrary = GradleModelMocker.this.createJavaLibrary(coordinateString,
                    promotedTo,false);

            if (!children.isEmpty()) {
                List<JavaLibrary> children = createJavaLibraries(this.children);
                // Work around wildcard capture
                //when(javaLibrary.getDependencies()).thenReturn(childrenLibraries);
                OngoingStubbing<? extends List<? extends JavaLibrary>> stub = when(
                        javaLibrary.getDependencies());
                //noinspection unchecked,RedundantCast
                ((OngoingStubbing<List<? extends JavaLibrary>>) stub).thenReturn(children);
            }
            return javaLibrary;
        }

        @Nullable
        private Dep getLastChild() {
            return children.isEmpty() ? null : children.get(children.size() - 1);
        }

        @Override
        public String toString() {
            return coordinate + ":" + depth;
        }

        @SuppressWarnings("unused") // For debugging
        public void printTree(int indent, PrintStream writer) {
            for (int i = 0; i < indent; i++) {
                writer.print("    ");
            }
            writer.println(coordinate);
            for (Dep child : children) {
                child.printTree(indent + 1, writer);
            }
        }
    }
}
