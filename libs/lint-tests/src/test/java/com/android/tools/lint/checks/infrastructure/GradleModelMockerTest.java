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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.FilterData;
import com.android.builder.model.AndroidArtifact;
import com.android.builder.model.AndroidArtifactOutput;
import com.android.builder.model.AndroidLibrary;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.BuildType;
import com.android.builder.model.BuildTypeContainer;
import com.android.builder.model.ClassField;
import com.android.builder.model.Dependencies;
import com.android.builder.model.JavaCompileOptions;
import com.android.builder.model.JavaLibrary;
import com.android.builder.model.LintOptions;
import com.android.builder.model.MavenCoordinates;
import com.android.builder.model.ProductFlavor;
import com.android.builder.model.ProductFlavorContainer;
import com.android.builder.model.Variant;
import com.android.builder.model.level2.DependencyGraphs;
import com.android.builder.model.level2.GlobalLibraryMap;
import com.android.builder.model.level2.GraphItem;
import com.android.builder.model.level2.Library;
import com.android.utils.ILogger;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.intellij.lang.annotations.Language;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GradleModelMockerTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private GradleModelMocker createMocker(@Language("Groovy") String gradle) {
        try {
            return new GradleModelMocker(gradle).withLogger(new ILogger() {
                @Override
                public void error(@Nullable Throwable t, @Nullable String msgFormat, Object... args) {
                    fail(msgFormat);
                }

                @Override
                public void warning(@NonNull String msgFormat, Object... args) {
                    System.out.println(msgFormat);
                }

                @Override
                public void info(@NonNull String msgFormat, Object... args) {
                }

                @Override
                public void verbose(@NonNull String msgFormat, Object... args) {
                }
            }).withProjectDir(tempFolder.newFolder("build"));
        } catch (IOException e) {
            fail(e.getMessage());
            return null;
        }
    }

    @Nullable
    private static BuildType findBuildType(@NonNull GradleModelMocker mocker,
            @NonNull String name) {
        AndroidProject project = mocker.getProject();
        for (BuildTypeContainer container : project.getBuildTypes()) {
            BuildType buildType = container.getBuildType();
            if (name.equals(buildType.getName())) {
                return buildType;
            }
        }
        return null;
    }

    @Nullable
    private static ProductFlavor findProductFlavor(@NonNull GradleModelMocker mocker,
            @NonNull String name) {
        AndroidProject project = mocker.getProject();
        for (ProductFlavorContainer container : project.getProductFlavors()) {
            ProductFlavor productFlavor = container.getProductFlavor();
            if (name.equals(productFlavor.getName())) {
                return productFlavor;
            }
        }
        return null;
    }

    @Test
    public void testLibraries() {
        GradleModelMocker mocker = createMocker(""
                + "apply plugin: 'com.android.application'\n"
                + "\n"
                + "dependencies {\n"
                + "    compile 'my.group.id:mylib:25.0.0-SNAPSHOT'\n"
                + "}");
        Variant variant = mocker.getVariant();
        AndroidProject project = mocker.getProject();
        assertThat(project.getProjectType()).isEqualTo(AndroidProject.PROJECT_TYPE_APP);

        assertThat(variant.getMergedFlavor().getVersionCode()).isNull(); // not Integer.valueOf(0)!
        Collection<AndroidLibrary> libraries = variant.getMainArtifact()
                .getDependencies().getLibraries();
        assertThat(libraries).hasSize(1);
        AndroidLibrary library = libraries.iterator().next();
        MavenCoordinates resolvedCoordinates = library.getResolvedCoordinates();
        assertThat(resolvedCoordinates.getGroupId()).isEqualTo("my.group.id");
        assertThat(resolvedCoordinates.getArtifactId()).isEqualTo("mylib");
        assertThat(resolvedCoordinates.getVersion()).isEqualTo("25.0.0-SNAPSHOT");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMinSdkVersion() {
        GradleModelMocker mocker = createMocker(""
                + "android {\n"
                + "    compileSdkVersion 25\n"
                + "    buildToolsVersion \"25.0.0\"\n"
                + "    defaultConfig {\n"
                + "        applicationId \"com.android.tools.test\"\n"
                + "        minSdkVersion 5\n"
                + "        targetSdkVersion 16\n"
                + "        versionCode 2\n"
                + "        versionName \"MyName\"\n"
                + "    }\n"
                + "}");

        AndroidProject project = mocker.getProject();
        Variant variant = mocker.getVariant();

        assertThat(project.getCompileTarget()).isEqualTo("android-25");
        assertThat(project.getBuildToolsVersion()).isEqualTo("25.0.0");

        ProductFlavor mergedFlavor = variant.getMergedFlavor();
        assertThat(mergedFlavor.getMinSdkVersion()).isNotNull();
        assertThat(mergedFlavor.getMinSdkVersion().getApiLevel()).isEqualTo(5);
        assertThat(mergedFlavor.getTargetSdkVersion().getApiLevel()).isEqualTo(16);
        assertThat(mergedFlavor.getVersionCode()).isEqualTo(2);
        assertThat(mergedFlavor.getVersionName()).isEqualTo("MyName");
        assertThat(mergedFlavor.getApplicationId()).isEqualTo("com.android.tools.test");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testFlavors() {
        GradleModelMocker mocker = createMocker(""
                + "apply plugin: 'com.android.application'\n"
                + "\n"
                + "android {\n"
                + "    defaultConfig {\n"
                + "        resConfigs \"mdpi\"\n"
                + "    }\n"
                + "    flavorDimensions  \"pricing\", \"releaseType\"\n"
                + "    productFlavors {\n"
                + "        beta {\n"
                + "            flavorDimension \"releaseType\"\n"
                + "            resConfig \"en\"\n"
                + "            resConfigs \"nodpi\", \"hdpi\"\n"
                + "            versionNameSuffix \"-beta\"\n"
                + "            applicationIdSuffix '.beta'\n"
                + "        }\n"
                + "        normal { flavorDimension \"releaseType\" }\n"
                + "        free { flavorDimension \"pricing\" }\n"
                + "        paid { flavorDimension \"pricing\" }\n"
                + "    }\n"
                + "}");
        AndroidProject project = mocker.getProject();
        Variant variant = mocker.getVariant();
        BuildType buildType = findBuildType(mocker, "debug");
        assertThat(buildType).isNotNull();
        assertThat(buildType.getName()).isEqualTo("debug");
        assertThat(buildType.isDebuggable()).isTrue();

        assertThat(project.getFlavorDimensions()).containsExactly("pricing", "releaseType");

        // Flavor dimensions
        assertThat(findProductFlavor(mocker, "beta").getDimension()).isEqualTo("releaseType");
        assertThat(findProductFlavor(mocker, "normal").getDimension()).isEqualTo("releaseType");
        assertThat(findProductFlavor(mocker, "free").getDimension()).isEqualTo("pricing");
        assertThat(findProductFlavor(mocker, "paid").getDimension()).isEqualTo("pricing");

        assertThat(variant.getName()).isEqualTo("debugBetaFree");

        // ResConfigs
        ProductFlavor beta = findProductFlavor(mocker, "beta");
        assertThat(beta.getResourceConfigurations()).containsExactly("en", "nodpi", "hdpi");
        ProductFlavor defaultConfig = findProductFlavor(mocker, "defaultConfig");
        assertThat(defaultConfig.getResourceConfigurations()).containsExactly("mdpi");

        // Suffix handling
        assertThat(beta.getApplicationIdSuffix()).isEqualTo(".beta");
        assertThat(beta.getVersionNameSuffix()).isEqualTo("-beta");
    }

    @Test
    public void testSourceSets() {
        GradleModelMocker mocker = createMocker(""
                + "android {\n"
                + "    compileSdkVersion 25\n"
                + "    buildToolsVersion \"25.0.0\"\n"
                + "    defaultConfig {\n"
                + "        applicationId \"com.android.tools.test\"\n"
                + "        minSdkVersion 5\n"
                + "        targetSdkVersion 16\n"
                + "        versionCode 2\n"
                + "        versionName \"MyName\"\n"
                + "    }\n"
                + "}");

        AndroidProject project = mocker.getProject();

        File manifestFile = project.getDefaultConfig().getSourceProvider().getManifestFile();
        assertThat(manifestFile.getPath()).endsWith("AndroidManifest.xml");
        assertThat(manifestFile.getPath()
                .replace(File.separatorChar, '/')).endsWith("src/main/AndroidManifest.xml");
        assertThat(project.getDefaultConfig().getSourceProvider().getJavaDirectories()).isNotEmpty();
    }

    @Test
    public void testProvidedScopes() {
        GradleModelMocker mocker = createMocker(""
                + "apply plugin: 'android-library'\n"
                + "\n"
                + "dependencies {\n"
                + "    provided \"com.google.android.wearable:wearable:2.0.0-alpha4\"\n"
                + "}\n");

        Variant variant = mocker.getVariant();
        AndroidProject project = mocker.getProject();
        assertThat(project.getProjectType()).isEqualTo(AndroidProject.PROJECT_TYPE_LIBRARY);
        Dependencies dependencies = variant.getMainArtifact().getDependencies();
        Collection<JavaLibrary> libraries = dependencies.getJavaLibraries();
        assertThat(libraries.size()).isEqualTo(1);
        JavaLibrary library = libraries.iterator().next();
        assertThat(library.getResolvedCoordinates().getArtifactId()).isEqualTo("wearable");
        assertThat(library.isProvided()).isTrue();
    }

    @Test
    public void testDependencyPropertyForm() {
        GradleModelMocker mocker = createMocker(""
                + "apply plugin: 'android'\n"
                + "\n"
                + "dependencies {\n"
                + "    implementation group: 'com.android.support', \n"
                + "            name: \"support-v4\", version: '19.0'\n"
                + "}\n");

        Variant variant = mocker.getVariant();
        Dependencies dependencies = variant.getMainArtifact().getDependencies();
        Collection<JavaLibrary> libraries = dependencies.getJavaLibraries();
        assertThat(libraries.size()).isEqualTo(1);
        JavaLibrary library = libraries.iterator().next();
        MavenCoordinates coordinates = library.getResolvedCoordinates();
        assertThat(coordinates.getGroupId()).isEqualTo("com.android.support");
        assertThat(coordinates.getArtifactId()).isEqualTo("support-v4");
        assertThat(coordinates.getVersion()).isEqualTo("19.0");
    }

    @Test
    public void testModelVersion() {
        GradleModelMocker mocker = createMocker(""
                + "buildscript {\n"
                + "    repositories {\n"
                + "        jcenter()\n"
                + "    }\n"
                + "    dependencies {\n"
                + "        classpath 'com.android.tools.build:gradle:1.5.1'\n"
                + "\n"
                + "        // NOTE: Do not place your application dependencies here; they belong\n"
                + "        // in the individual module build.gradle files\n"
                + "    }\n"
                + "}");

        AndroidProject project = mocker.getProject();
        assertThat(project.getModelVersion()).isEqualTo("1.5.1");
        assertThat(project.getApiVersion()).isEqualTo(3);
    }

    @Test
    public void testVectors() {
        GradleModelMocker mocker = createMocker(""
                + "android.defaultConfig.vectorDrawables {\n"
                + "    useSupportLibrary = true\n"
                + "}");

        Variant variant = mocker.getVariant();
        assertThat(variant.getMergedFlavor().getVectorDrawables().getUseSupportLibrary()).isTrue();
    }

    @Test
    public void testResValues() {
        GradleModelMocker mocker = createMocker(""
                + "android {\n"
                + "    defaultConfig {\n"
                + "        resValue \"string\", \"defaultConfigName\", \"Some DefaultConfig Data\"\n"
                + "    }\n"
                + "    buildTypes {\n"
                + "        debug {\n"
                + "            resValue \"string\", \"debugName\", \"Some Debug Data\"\n"
                + "        }\n"
                + "        release {\n"
                + "            resValue \"string\", \"releaseName1\", \"Some Release Data 1\"\n"
                + "            resValue \"string\", \"releaseName2\", \"Some Release Data 2\"\n"
                + "        }\n"
                + "    }\n"
                + "    productFlavors {\n"
                + "         flavor1 {\n"
                + "             resValue \"string\", \"VALUE_DEBUG\",   \"10\"\n"
                + "             resValue \"string\", \"VALUE_FLAVOR\",  \"10\"\n"
                + "             resValue \"string\", \"VALUE_VARIANT\", \"10\"\n"
                + "         }\n"
                + "         flavor2 {\n"
                + "             resValue \"string\", \"VALUE_DEBUG\",   \"20\"\n"
                + "             resValue \"string\", \"VALUE_FLAVOR\",  \"20\"\n"
                + "             resValue \"string\", \"VALUE_VARIANT\", \"20\"\n"
                + "         }\n"
                + "     }\n"
                + "}");

        AndroidProject project = mocker.getProject();

        // Check default config
        Map<String, ClassField> resValues = project.getDefaultConfig().getProductFlavor()
                .getResValues();
        assertThat(resValues).isNotEmpty();
        String name = resValues.keySet().iterator().next();
        assertThat(name).isEqualTo("defaultConfigName");
        ClassField field = resValues.get(name);
        assertThat(field.getType()).isEqualTo("string");
        assertThat(field.getName()).isEqualTo("defaultConfigName");
        assertThat(field.getValue()).isEqualTo("Some DefaultConfig Data");

        // Check debug build type
        BuildType buildType = findBuildType(mocker, "debug");
        assertThat(buildType).isNotNull();
        resValues = buildType.getResValues();
        assertThat(resValues).isNotEmpty();
        name = resValues.keySet().iterator().next();
        assertThat(name).isEqualTo("debugName");
        field = resValues.get(name);
        assertThat(field.getType()).isEqualTo("string");
        assertThat(field.getName()).isEqualTo("debugName");
        assertThat(field.getValue()).isEqualTo("Some Debug Data");

        // Check product flavor
        ProductFlavor flavor = findProductFlavor(mocker, "flavor1");
        assertThat(flavor).isNotNull();
        resValues = flavor.getResValues();
        assertThat(resValues).isNotEmpty();
        name = resValues.keySet().iterator().next();
        assertThat(name).isEqualTo("VALUE_DEBUG");
        field = resValues.get(name);
        assertThat(field.getType()).isEqualTo("string");
        assertThat(field.getName()).isEqualTo("VALUE_DEBUG");
        assertThat(field.getValue()).isEqualTo("10");

        flavor = findProductFlavor(mocker, "flavor2");
        assertThat(flavor).isNotNull();
        resValues = flavor.getResValues();
        assertThat(resValues).isNotEmpty();
        name = resValues.keySet().iterator().next();
        assertThat(name).isEqualTo("VALUE_DEBUG");
        field = resValues.get(name);
        assertThat(field.getType()).isEqualTo("string");
        assertThat(field.getName()).isEqualTo("VALUE_DEBUG");
        assertThat(field.getValue()).isEqualTo("20");

        // Check release build type
        buildType = findBuildType(mocker, "release");
        assertThat(buildType).isNotNull();
        resValues = buildType.getResValues();
        assertThat(resValues).hasSize(2);
    }

    @Test
    public void testSetVariantName() {
        GradleModelMocker mocker = createMocker(""
                + "android {\n"
                + "    defaultConfig {\n"
                + "        resValue \"string\", \"defaultConfigName\", \"Some DefaultConfig Data\"\n"
                + "    }\n"
                + "    buildTypes {\n"
                + "        debug {\n"
                + "            resValue \"string\", \"debugName\", \"Some Debug Data\"\n"
                + "        }\n"
                + "        release {\n"
                + "            resValue \"string\", \"releaseName1\", \"Some Release Data 1\"\n"
                + "            resValue \"string\", \"releaseName2\", \"Some Release Data 2\"\n"
                + "        }\n"
                + "    }\n"
                + "    productFlavors {\n"
                + "         flavor1 {\n"
                + "             resValue \"string\", \"VALUE_DEBUG\",   \"10\"\n"
                + "             resValue \"string\", \"VALUE_FLAVOR\",  \"10\"\n"
                + "             resValue \"string\", \"VALUE_VARIANT\", \"10\"\n"
                + "         }\n"
                + "         flavor2 {\n"
                + "             resValue \"string\", \"VALUE_DEBUG\",   \"20\"\n"
                + "             resValue \"string\", \"VALUE_FLAVOR\",  \"20\"\n"
                + "             resValue \"string\", \"VALUE_VARIANT\", \"20\"\n"
                + "         }\n"
                + "     }\n"
                + "}");

        Variant variant = mocker.getVariant();
        assertThat(variant.getName()).isEqualTo("debugFlavor1");
        assertThat(variant.getBuildType()).isEqualTo("debug");
        assertThat(variant.getProductFlavors()).containsExactly("flavor1");

        mocker.setVariantName("releaseFlavor2");
        assertThat(variant.getName()).isEqualTo("releaseFlavor2");
        assertThat(variant.getBuildType()).isEqualTo("release");
        assertThat(variant.getProductFlavors()).containsExactly("flavor2");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testPlaceHolders() {
        GradleModelMocker mocker = createMocker(""
                + "android {\n"
                + "    defaultConfig {\n"
                + "        manifestPlaceholders = [ localApplicationId:\"com.example.manifest_merger_example\"]\n"
                + "    }\n"
                + "    productFlavors {\n"
                + "        flavor {\n"
                + "            manifestPlaceholders = [ localApplicationId:\"com.example.manifest_merger_example.flavor\"]\n"
                + "        }\n"
                + "    }\n"
                + "    productFlavors {\n"
                + "        free {\n"
                + "            manifestPlaceholders = [\"holder\":\"free\"]\n"
                + "        }\n"
                + "        beta {\n"
                + "            manifestPlaceholders = [\"holder\":\"beta\"]\n"
                + "        }\n"
                + "    }\n"
                + "}");

        ProductFlavor defaultConfig = findProductFlavor(mocker, "defaultConfig");
        Map<String, Object> manifestPlaceholders = defaultConfig.getManifestPlaceholders();
        assertThat(manifestPlaceholders).containsEntry("localApplicationId",
                "com.example.manifest_merger_example");
        ProductFlavor flavor = findProductFlavor(mocker, "flavor");
        assertThat(flavor.getManifestPlaceholders()).containsEntry("localApplicationId",
                "com.example.manifest_merger_example.flavor");
        flavor = findProductFlavor(mocker, "free");
        assertThat(flavor.getManifestPlaceholders()).containsEntry("holder", "free");
        flavor = findProductFlavor(mocker, "beta");
        assertThat(flavor.getManifestPlaceholders()).containsEntry("holder", "beta");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMinifyEnabled() {
        GradleModelMocker mocker = createMocker(""
                + "android {\n"
                + "    buildTypes {\n"
                + "        release {\n"
                + "            minifyEnabled true\n"
                + "        }\n"
                + "    }\n"
                + "}");

        assertThat(findBuildType(mocker, "release").isMinifyEnabled()).isTrue();
    }

    @Test(expected = AssertionError.class)
    public void testFailOnUnexpected() {
        GradleModelMocker mocker = createMocker(""
                + "android {\n"
                + "    minSdkVersion 15\n"
                + "}");
        mocker.getProject();
    }

    @Test
    public void testWarnOnUnrecognized() {
        final AtomicBoolean hasWarning = new AtomicBoolean();
        final AtomicBoolean hasError = new AtomicBoolean();
        GradleModelMocker mocker = createMocker(""
                + "apply plugin: 'java'\n"
                + "foo.bar\n").withLogger(new ILogger() {
            @Override
            public void error(@Nullable Throwable t, @Nullable String msgFormat,
                    Object... args) {
                hasError.set(true);
            }

            @Override
            public void warning(@NonNull String msgFormat, Object... args) {
                hasWarning.set(true);
            }

            @Override
            public void info(@NonNull String msgFormat, Object... args) {
            }

            @Override
            public void verbose(@NonNull String msgFormat, Object... args) {
            }
        }).withModelVersion("1.5.0").allowUnrecognizedConstructs();
        assertThat(mocker.getProject().getModelVersion()).isEqualTo("1.5.0");
        assertThat(hasWarning.get()).isTrue();
        assertThat(hasError.get()).isFalse();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testVersionProperties() {
        GradleModelMocker mocker = createMocker(""
                + "android {\n"
                + "    defaultConfig {\n"
                + "        applicationId \"com.example.manifest_merger_example\"\n"
                + "        minSdkVersion 15\n"
                + "        targetSdkVersion 21\n"
                + "        versionCode 1\n"
                + "        versionName \"1.0\"\n"
                + "    }\n"
                + "    productFlavors {\n"
                + "        flavor {\n"
                + "            applicationId \"com.example.manifest_merger_example.flavor\"\n"
                + "            minSdkVersion 16\n"
                + "            targetSdkVersion 22\n"
                + "            versionCode 2\n"
                + "            versionName \"2.0\"\n"
                + "        }\n"
                + "    }\n"
                + "}");

        ProductFlavor flavor = findProductFlavor(mocker, "defaultConfig");
        assertThat(flavor.getApplicationId())
                .isEqualTo("com.example.manifest_merger_example");
        assertThat(flavor.getMinSdkVersion().getApiLevel()).isEqualTo(15);
        assertThat(flavor.getTargetSdkVersion().getApiLevel()).isEqualTo(21);
        assertThat(flavor.getVersionCode()).isEqualTo(1);
        assertThat(flavor.getVersionName()).isEqualTo("1.0");

        flavor = findProductFlavor(mocker, "flavor");
        assertThat(flavor.getApplicationId())
                .isEqualTo("com.example.manifest_merger_example.flavor");
        assertThat(flavor.getMinSdkVersion().getApiLevel()).isEqualTo(16);
        assertThat(flavor.getTargetSdkVersion().getApiLevel()).isEqualTo(22);
        assertThat(flavor.getVersionCode()).isEqualTo(2);
        assertThat(flavor.getVersionName()).isEqualTo("2.0");
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testApkSplits() {
        GradleModelMocker mocker = createMocker(""
                + "android {\n"
                + "    splits {\n"
                + "        density {\n"
                + "            enable true\n"
                + "            reset()\n"
                + "            include \"mdpi\", \"hdpi\"\n"
                + "        }\n"
                + "        language {\n"
                + "            enable true\n"
                + "            include \"fr\", \"fr-rCA\", \"en\"\n"
                + "        }\n"
                + "        abi {\n"
                + "            enable = true\n"
                + "            include \"x86_64\", \"mips64\", \"arm64-v8a\", \"armeabi\"\n"
                + "        }\n"
                + "    }\n"
                + "}\n");

        AndroidArtifact mainArtifact = mocker.getVariant().getMainArtifact();
        AndroidArtifactOutput[] outputs = Iterators
                .toArray(mainArtifact.getOutputs().iterator(), AndroidArtifactOutput.class);
        assertThat(outputs).hasLength(10);
        assertThat(outputs[0].getFilterTypes()).isEmpty();
        FilterData filterData = outputs[1].getFilters().iterator().next();
        assertThat(filterData.getFilterType()).isEqualTo("DENSITY");
        assertThat(filterData.getIdentifier()).isEqualTo("mdpi");

        filterData = outputs[3].getFilters().iterator().next();
        assertThat(filterData.getFilterType()).isEqualTo("LANGUAGE");
        assertThat(filterData.getIdentifier()).isEqualTo("fr");

        filterData = outputs[9].getFilters().iterator().next();
        assertThat(filterData.getFilterType()).isEqualTo("ABI");
        assertThat(filterData.getIdentifier()).isEqualTo("armeabi");
    }

    @Test
    public void testNestedDependencies() {
        GradleModelMocker mocker = createMocker(""
                + "apply plugin: 'com.android.application'\n"
                + "\n"
                + "dependencies {\n"
                + "    compile \"com.android.support:appcompat-v7:25.0.1\"\n"
                + "    compile \"com.android.support.constraint:constraint-layout:1.0.0-beta3\"\n"
                + "}")
                .withDependencyGraph(""
                + "+--- com.android.support:appcompat-v7:25.0.1\n"
                + "|    +--- com.android.support:support-v4:25.0.1\n"
                + "|    |    +--- com.android.support:support-compat:25.0.1\n"
                + "|    |    |    \\--- com.android.support:support-annotations:25.0.1\n"
                + "|    |    +--- com.android.support:support-media-compat:25.0.1\n"
                + "|    |    |    \\--- com.android.support:support-compat:25.0.1 (*)\n"
                + "|    |    +--- com.android.support:support-core-utils:25.0.1\n"
                + "|    |    |    \\--- com.android.support:support-compat:25.0.1 (*)\n"
                + "|    |    +--- com.android.support:support-core-ui:25.0.1\n"
                + "|    |    |    \\--- com.android.support:support-compat:25.0.1 (*)\n"
                + "|    |    \\--- com.android.support:support-fragment:25.0.1\n"
                + "|    |         +--- com.android.support:support-compat:25.0.1 (*)\n"
                + "|    |         +--- com.android.support:support-media-compat:25.0.1 (*)\n"
                + "|    |         +--- com.android.support:support-core-ui:25.0.1 (*)\n"
                + "|    |         \\--- com.android.support:support-core-utils:25.0.1 (*)\n"
                + "|    +--- com.android.support:support-vector-drawable:25.0.1\n"
                + "|    |    \\--- com.android.support:support-compat:25.0.1 (*)\n"
                + "|    \\--- com.android.support:animated-vector-drawable:25.0.1\n"
                + "|         \\--- com.android.support:support-vector-drawable:25.0.1 (*)\n"
                + "+--- com.android.support.constraint:constraint-layout:1.0.0-beta3\n"
                + "|    \\--- com.android.support.constraint:constraint-layout-solver:1.0.0-beta3\n");
        Dependencies dependencies = mocker.getVariant().getMainArtifact().getDependencies();

        List<JavaLibrary> javaLibraries;
        List<AndroidLibrary> androidLibraries;
        javaLibraries = Lists.newArrayList(dependencies.getJavaLibraries());
        androidLibraries = Lists.newArrayList(dependencies.getLibraries());

        assertThat(javaLibraries).hasSize(0);
        assertThat(androidLibraries).hasSize(2);

        AndroidLibrary library;
        MavenCoordinates coordinates;

        library = androidLibraries.get(0);
        coordinates = library.getResolvedCoordinates();
        assertThat(coordinates.getGroupId()).isEqualTo("com.android.support");
        assertThat(coordinates.getArtifactId()).isEqualTo("appcompat-v7");
        assertThat(coordinates.getVersion()).isEqualTo("25.0.1");
        assertThat(coordinates.getPackaging()).isEqualTo("aar");

        library = androidLibraries.get(1);
        coordinates = library.getResolvedCoordinates();
        assertThat(coordinates.getGroupId()).isEqualTo("com.android.support.constraint");
        assertThat(coordinates.getArtifactId()).isEqualTo("constraint-layout");
        assertThat(coordinates.getVersion()).isEqualTo("1.0.0-beta3");
        assertThat(coordinates.getPackaging()).isEqualTo("aar");

        // Check recursive dependencies
        library = androidLibraries.get(0);
        androidLibraries = Lists.newArrayList(library.getLibraryDependencies());
        androidLibraries.sort(LIBRARY_COMPARATOR);
        library = androidLibraries.get(1);
        coordinates = library.getResolvedCoordinates();
        assertThat(coordinates.getGroupId()).isEqualTo("com.android.support");
        assertThat(coordinates.getArtifactId()).isEqualTo("support-v4");
        assertThat(coordinates.getVersion()).isEqualTo("25.0.1");
        assertThat(coordinates.getPackaging()).isEqualTo("aar");

        androidLibraries = Lists.newArrayList(library.getLibraryDependencies());
        androidLibraries.sort(LIBRARY_COMPARATOR);
        library = androidLibraries.get(0);
        coordinates = library.getResolvedCoordinates();
        assertThat(coordinates.getGroupId()).isEqualTo("com.android.support");
        assertThat(coordinates.getArtifactId()).isEqualTo("support-compat");
        assertThat(coordinates.getVersion()).isEqualTo("25.0.1");
        assertThat(coordinates.getPackaging()).isEqualTo("aar");

        //JavaLibrary javaLibrary = library.getJavaDependencies().iterator().next();
        //coordinates = javaLibrary.getResolvedCoordinates();
        //assertThat(coordinates.getGroupId()).isEqualTo("com.android.support");
        //assertThat(coordinates.getArtifactId()).isEqualTo("support-annotations");
        //assertThat(coordinates.getVersion()).isEqualTo("25.0.1");
        //assertThat(coordinates.getPackaging()).isEqualTo("jar");
    }

    @Test
    public void testDependencyGraph() {
        GradleModelMocker mocker = createMocker(""
                + "apply plugin: 'com.android.application'\n"
                + "\n"
                + "dependencies {\n"
                + "    compile \"com.android.support:appcompat-v7:25.0.1\"\n"
                + "    compile \"com.android.support.constraint:constraint-layout:1.0.0-beta3\"\n"
                + "}");

        Dependencies dependencies = mocker.createDependencies(""
                + "+--- com.android.support:appcompat-v7:25.0.1\n"
                + "|    +--- com.android.support:support-v4:25.0.1\n"
                + "|    |    +--- com.android.support:support-compat:25.0.1\n"
                + "|    |    |    \\--- com.android.support:support-annotations:25.0.1\n"
                + "|    |    +--- com.android.support:support-media-compat:25.0.1\n"
                + "|    |    |    \\--- com.android.support:support-compat:25.0.1 (*)\n"
                + "|    |    +--- com.android.support:support-core-utils:25.0.1\n"
                + "|    |    |    \\--- com.android.support:support-compat:25.0.1 (*)\n"
                + "|    |    +--- com.android.support:support-core-ui:25.0.1\n"
                + "|    |    |    \\--- com.android.support:support-compat:25.0.1 (*)\n"
                + "|    |    \\--- com.android.support:support-fragment:25.0.1\n"
                + "|    |         +--- com.android.support:support-compat:25.0.1 (*)\n"
                + "|    |         +--- com.android.support:support-media-compat:25.0.1 (*)\n"
                + "|    |         +--- com.android.support:support-core-ui:25.0.1 (*)\n"
                + "|    |         \\--- com.android.support:support-core-utils:25.0.1 (*)\n"
                + "|    +--- com.android.support:support-vector-drawable:25.0.1\n"
                + "|    |    \\--- com.android.support:support-compat:25.0.1 (*)\n"
                + "|    \\--- com.android.support:animated-vector-drawable:25.0.1\n"
                + "|         \\--- com.android.support:support-vector-drawable:25.0.1 (*)\n"
                + "+--- com.android.support.constraint:constraint-layout:1.0.0-beta3\n"
                + "|    \\--- com.android.support.constraint:constraint-layout-solver:1.0.0-beta3\n"
                + "\\--- project :mylibrary\n"
                + "     \\--- com.android.support:appcompat-v7:25.0.1 (*)");
        List<JavaLibrary> javaLibraries;
        List<AndroidLibrary> androidLibraries;
        List<String> projects;
        javaLibraries = Lists.newArrayList(dependencies.getJavaLibraries());
        androidLibraries = Lists.newArrayList(dependencies.getLibraries());
        //noinspection deprecation
        projects = Lists.newArrayList(dependencies.getProjects());

        assertThat(projects).containsExactly(":mylibrary");
        assertThat(javaLibraries).hasSize(0);
        assertThat(androidLibraries).hasSize(3);

        AndroidLibrary library;
        MavenCoordinates coordinates;

        library = androidLibraries.get(0);
        coordinates = library.getResolvedCoordinates();
        assertThat(coordinates.getGroupId()).isEqualTo("com.android.support");
        assertThat(coordinates.getArtifactId()).isEqualTo("appcompat-v7");
        assertThat(coordinates.getVersion()).isEqualTo("25.0.1");
        assertThat(coordinates.getPackaging()).isEqualTo("aar");

        library = androidLibraries.get(1);
        coordinates = library.getResolvedCoordinates();
        assertThat(coordinates.getGroupId()).isEqualTo("com.android.support.constraint");
        assertThat(coordinates.getArtifactId()).isEqualTo("constraint-layout");
        assertThat(coordinates.getVersion()).isEqualTo("1.0.0-beta3");
        assertThat(coordinates.getPackaging()).isEqualTo("aar");

        library = androidLibraries.get(2);
        assertThat(library.getProject()).isEqualTo(":mylibrary");
        assertThat(library.getName()).isEqualTo(":mylibrary");

        // Check recursive dependencies
        library = androidLibraries.get(0);
        androidLibraries = Lists.newArrayList(library.getLibraryDependencies());
        androidLibraries.sort(LIBRARY_COMPARATOR);
        library = androidLibraries.get(1);
        coordinates = library.getResolvedCoordinates();
        assertThat(coordinates.getGroupId()).isEqualTo("com.android.support");
        assertThat(coordinates.getArtifactId()).isEqualTo("support-v4");
        assertThat(coordinates.getVersion()).isEqualTo("25.0.1");
        assertThat(coordinates.getPackaging()).isEqualTo("aar");

        androidLibraries = Lists.newArrayList(library.getLibraryDependencies());
        androidLibraries.sort(LIBRARY_COMPARATOR);
        library = androidLibraries.get(0);
        coordinates = library.getResolvedCoordinates();
        assertThat(coordinates.getGroupId()).isEqualTo("com.android.support");
        assertThat(coordinates.getArtifactId()).isEqualTo("support-compat");
        assertThat(coordinates.getVersion()).isEqualTo("25.0.1");
        assertThat(coordinates.getPackaging()).isEqualTo("aar");

        //JavaLibrary javaLibrary = library.getJavaDependencies().iterator().next();
        //coordinates = javaLibrary.getResolvedCoordinates();
        //assertThat(coordinates.getGroupId()).isEqualTo("com.android.support");
        //assertThat(coordinates.getArtifactId()).isEqualTo("support-annotations");
        //assertThat(coordinates.getVersion()).isEqualTo("25.0.1");
        //assertThat(coordinates.getPackaging()).isEqualTo("jar");
    }

    @Test
    public void testPromotedDependencies() {
        GradleModelMocker mocker = createMocker(""
                + "dependencies {\n"
                + "    compile 'junit:junit:4.12'\n"
                + "    compile 'org.hamcrest:hamcrest-core:1.3'\n"
                + "    compile 'org.mockito:mockito-core:1.10.8'\n"
                + "    compile 'org.powermock:powermock-api-mockito:1.6.4'\n"
                + "    compile 'org.powermock:powermock-module-junit4-rule-agent:1.6.4'\n"
                + "    compile 'org.powermock:powermock-module-junit4-rule:1.6.4'\n"
                + "    compile 'org.powermock:powermock-module-junit4:1.6.4'\n"
                + "    compile 'org.json:json:20090211'"
                + "}\n"
                + "").withDependencyGraph(""
                + "+--- junit:junit:4.12\n"
                + "|    \\--- org.hamcrest:hamcrest-core:1.3\n"
                + "+--- org.hamcrest:hamcrest-core:1.3\n"
                + "+--- org.mockito:mockito-core:1.10.8 -> 1.10.19\n"
                + "|    +--- org.hamcrest:hamcrest-core:1.1 -> 1.3\n"
                + "|    \\--- org.objenesis:objenesis:2.1\n"
                + "+--- org.powermock:powermock-api-mockito:1.6.4\n"
                + "|    +--- org.mockito:mockito-core:1.10.19 (*)\n"
                + "|    +--- org.hamcrest:hamcrest-core:1.3\n"
                + "|    \\--- org.powermock:powermock-api-support:1.6.4\n"
                + "|         +--- org.powermock:powermock-core:1.6.4\n"
                + "|         |    +--- org.powermock:powermock-reflect:1.6.4\n"
                + "|         |    |    \\--- org.objenesis:objenesis:2.1\n"
                + "|         |    \\--- org.javassist:javassist:3.20.0-GA\n"
                + "|         \\--- org.powermock:powermock-reflect:1.6.4 (*)\n"
                + "+--- org.powermock:powermock-module-junit4-rule-agent:1.6.4\n"
                + "|    +--- org.powermock:powermock-module-javaagent:1.6.4\n"
                + "|    |    \\--- org.powermock:powermock-core:1.6.4 (*)\n"
                + "|    \\--- org.powermock:powermock-core:1.6.4 (*)\n"
                + "+--- org.powermock:powermock-module-junit4-rule:1.6.4\n"
                + "|    +--- org.powermock:powermock-classloading-base:1.6.4\n"
                + "|    |    +--- org.powermock:powermock-api-support:1.6.4 (*)\n"
                + "|    |    \\--- org.powermock:powermock-reflect:1.6.4 (*)\n"
                + "|    \\--- org.powermock:powermock-core:1.6.4 (*)\n"
                + "+--- org.powermock:powermock-module-junit4:1.6.4\n"
                + "|    +--- junit:junit:4.12 (*)\n"
                + "|    \\--- org.powermock:powermock-module-junit4-common:1.6.4\n"
                + "|         +--- junit:junit:4.4 -> 4.12 (*)\n"
                + "|         +--- org.powermock:powermock-core:1.6.4 (*)\n"
                + "|         \\--- org.powermock:powermock-reflect:1.6.4 (*)\n"
                + "\\--- org.json:json:20090211");

        List<JavaLibrary> javaLibraries;
        List<AndroidLibrary> androidLibraries;
        Dependencies dependencies = mocker.getVariant().getMainArtifact().getDependencies();
        javaLibraries = Lists.newArrayList(dependencies.getJavaLibraries());
        androidLibraries = Lists.newArrayList(dependencies.getLibraries());

        assertThat(javaLibraries).hasSize(1);
        assertThat(androidLibraries).hasSize(7);

        // org.mockito:mockito-core:1.10.8 -> 1.10.19
        AndroidLibrary library = androidLibraries.get(1);
        MavenCoordinates coordinates = library.getResolvedCoordinates();
        assertThat(coordinates.getGroupId()).isEqualTo("org.mockito");
        assertThat(coordinates.getArtifactId()).isEqualTo("mockito-core");
        assertThat(coordinates.getVersion()).isEqualTo("1.10.19");
        assertThat(coordinates.getPackaging()).isEqualTo("aar");

        coordinates = library.getRequestedCoordinates();
        assertThat(coordinates.getGroupId()).isEqualTo("org.mockito");
        assertThat(coordinates.getArtifactId()).isEqualTo("mockito-core");
        assertThat(coordinates.getVersion()).isEqualTo("1.10.8");
        assertThat(coordinates.getPackaging()).isEqualTo("aar");
    }

    @Test
    public void testDependencyGraphs() {
        GradleModelMocker mocker = createMocker(""
                + "buildscript {\n"
                + "    dependencies {\n"
                // Need at least 2.5.0-alpha1 for this dependency graph behavior
                + "        classpath 'com.android.tools.build:gradle:2.5.0-alpha1'\n"
                + "    }\n"
                + "}\n"
                + "dependencies {\n"
                + "    compile 'junit:junit:4.12'\n"
                + "    compile 'org.hamcrest:hamcrest-core:1.3'\n"
                + "    compile 'org.mockito:mockito-core:1.10.8'\n"
                + "    compile 'org.powermock:powermock-api-mockito:1.6.4'\n"
                + "    compile 'org.powermock:powermock-module-junit4-rule-agent:1.6.4'\n"
                + "    compile 'org.powermock:powermock-module-junit4-rule:1.6.4'\n"
                + "    compile 'org.powermock:powermock-module-junit4:1.6.4'\n"
                + "    compile 'org.json:json:20090211'"
                + "}\n"
                + "").withDependencyGraph(""
                + "+--- junit:junit:4.12\n"
                + "|    \\--- org.hamcrest:hamcrest-core:1.3\n"
                + "+--- org.hamcrest:hamcrest-core:1.3\n"
                + "+--- org.mockito:mockito-core:1.10.8 -> 1.10.19\n"
                + "|    +--- org.hamcrest:hamcrest-core:1.1 -> 1.3\n"
                + "|    \\--- org.objenesis:objenesis:2.1\n"
                + "+--- org.powermock:powermock-api-mockito:1.6.4\n"
                + "|    +--- org.mockito:mockito-core:1.10.19 (*)\n"
                + "|    +--- org.hamcrest:hamcrest-core:1.3\n"
                + "|    \\--- org.powermock:powermock-api-support:1.6.4\n"
                + "|         +--- org.powermock:powermock-core:1.6.4\n"
                + "|         |    +--- org.powermock:powermock-reflect:1.6.4\n"
                + "|         |    |    \\--- org.objenesis:objenesis:2.1\n"
                + "|         |    \\--- org.javassist:javassist:3.20.0-GA\n"
                + "|         \\--- org.powermock:powermock-reflect:1.6.4 (*)\n"
                + "+--- org.powermock:powermock-module-junit4-rule-agent:1.6.4\n"
                + "|    +--- org.powermock:powermock-module-javaagent:1.6.4\n"
                + "|    |    \\--- org.powermock:powermock-core:1.6.4 (*)\n"
                + "|    \\--- org.powermock:powermock-core:1.6.4 (*)\n"
                + "+--- org.powermock:powermock-module-junit4-rule:1.6.4\n"
                + "|    +--- org.powermock:powermock-classloading-base:1.6.4\n"
                + "|    |    +--- org.powermock:powermock-api-support:1.6.4 (*)\n"
                + "|    |    \\--- org.powermock:powermock-reflect:1.6.4 (*)\n"
                + "|    \\--- org.powermock:powermock-core:1.6.4 (*)\n"
                + "+--- org.powermock:powermock-module-junit4:1.6.4\n"
                + "|    +--- junit:junit:4.12 (*)\n"
                + "|    \\--- org.powermock:powermock-module-junit4-common:1.6.4\n"
                + "|         +--- junit:junit:4.4 -> 4.12 (*)\n"
                + "|         +--- org.powermock:powermock-core:1.6.4 (*)\n"
                + "|         \\--- org.powermock:powermock-reflect:1.6.4 (*)\n"
                + "\\--- org.json:json:20090211")
                .withFullDependencies(false);

        DependencyGraphs graph = mocker.getVariant().getMainArtifact()
                .getDependencyGraphs();
        List<GraphItem> compileDependencies = graph.getCompileDependencies();
        List<String> names = Lists.newArrayList();
        for (GraphItem item : compileDependencies) {
            names.add(item.getArtifactAddress());
            assertThat(item.getDependencies()).isEmpty(); // Because we asked for flat deps above
        }
        Collections.sort(names);
        System.out.println(names);
        assertThat(names).containsExactly(
                "junit:junit:4.12@jar",
                "org.hamcrest:hamcrest-core:1.3@aar",
                "org.javassist:javassist:3.20.0-GA@aar",
                "org.json:json:20090211@aar",
                "org.mockito:mockito-core:1.10.19@aar",
                "org.objenesis:objenesis:2.1@aar",
                "org.powermock:powermock-api-mockito:1.6.4@aar",
                "org.powermock:powermock-api-support:1.6.4@aar",
                "org.powermock:powermock-classloading-base:1.6.4@aar",
                "org.powermock:powermock-core:1.6.4@aar",
                "org.powermock:powermock-module-javaagent:1.6.4@aar",
                "org.powermock:powermock-module-junit4-common:1.6.4@aar",
                "org.powermock:powermock-module-junit4-rule-agent:1.6.4@aar",
                "org.powermock:powermock-module-junit4-rule:1.6.4@aar",
                "org.powermock:powermock-module-junit4:1.6.4@aar",
                "org.powermock:powermock-reflect:1.6.4@aar"
        );

        GlobalLibraryMap libraryMap = mocker.getGlobalLibraryMap();
        assertThat(libraryMap).isNotNull();
        Library library = libraryMap.getLibraries().get("org.hamcrest:hamcrest-core:1.3@aar");
        assertThat(library.getArtifactAddress()).isEqualTo("org.hamcrest:hamcrest-core:1.3@aar");
        assertThat(library.getJarFile()).isNotNull();
    }

    @Test
    public void testLintOptions() {
        GradleModelMocker mocker = createMocker(""
                + "android {\n"
                + "    lintOptions {\n"
                + "        quiet true\n"
                + "        abortOnError false\n"
                + "        ignoreWarnings true\n"
                + "        absolutePaths false\n"
                + "        checkAllWarnings true\n"
                + "        warningsAsErrors true\n"
                + "        disable 'TypographyFractions','TypographyQuotes'\n"
                + "        enable 'RtlHardcoded','RtlCompat', 'RtlEnabled'\n"
                + "        check 'NewApi', 'InlinedApi'\n"
                + "        noLines true\n"
                + "        showAll true\n"
                + "        lintConfig file(\"default-lint.xml\")\n"
                + "        textReport true\n"
                + "        textOutput 'stdout'\n"
                + "        xmlReport false\n"
                + "        xmlOutput file(\"lint-report.xml\")\n"
                + "        htmlReport true\n"
                + "        htmlOutput file(\"lint-report.html\")\n"
                + "        informational 'LogConditional'\n"
                + "        checkTestSources true\n"
                + "    }\n"
                + "}\n");
        assertThat(mocker).isNotNull();
        AndroidProject project = mocker.getProject();
        assertThat(project).isNotNull();
        LintOptions options = project.getLintOptions();
        assertThat(options).isNotNull();
        assertThat(options.isQuiet()).isTrue();
        assertThat(options.isAbortOnError()).isFalse();
        assertThat(options.isIgnoreWarnings()).isTrue();
        assertThat(options.isCheckAllWarnings()).isTrue();
        assertThat(options.isWarningsAsErrors()).isTrue();
        assertThat(options.isAbsolutePaths()).isFalse();
        assertThat(options.getDisable()).containsExactly("TypographyFractions", "TypographyQuotes");
        assertThat(options.getEnable()).containsExactly("RtlHardcoded", "RtlCompat", "RtlEnabled");
        assertThat(options.getCheck()).containsExactly("NewApi", "InlinedApi");
    }

    @Test
    public void testLanguageOptions1() {
        GradleModelMocker mocker = createMocker(""
                + "android {\n"
                + "    compileOptions {\n"
                + "        sourceCompatibility JavaVersion.VERSION_1_8\n"
                + "        targetCompatibility JavaVersion.VERSION_1_8\n"
                + "    }\n"
                + "}");
        assertThat(mocker).isNotNull();
        AndroidProject project = mocker.getProject();
        assertThat(project).isNotNull();
        JavaCompileOptions compileOptions = project.getJavaCompileOptions();
        assertThat(compileOptions.getSourceCompatibility()).isEqualTo("1.8");
        assertThat(compileOptions.getTargetCompatibility()).isEqualTo("1.8");
    }

    @Test
    public void testLanguageOptions2() {
        GradleModelMocker mocker = createMocker(""
                + "android {\n"
                + "    compileOptions {\n"
                + "        sourceCompatibility JavaVersion.VERSION_1_7\n"
                + "        targetCompatibility JavaVersion.VERSION_1_7\n"
                + "    }\n"
                + "}");
        assertThat(mocker).isNotNull();
        AndroidProject project = mocker.getProject();
        assertThat(project).isNotNull();
        JavaCompileOptions compileOptions = project.getJavaCompileOptions();
        assertThat(compileOptions.getSourceCompatibility()).isEqualTo("1.7");
        assertThat(compileOptions.getTargetCompatibility()).isEqualTo("1.7");
    }

    private static final Comparator<AndroidLibrary> LIBRARY_COMPARATOR = (o1, o2) -> {
        MavenCoordinates r1 = o1.getResolvedCoordinates();
        MavenCoordinates r2 = o2.getResolvedCoordinates();
        int delta;
        delta = r1.getGroupId().compareTo(r2.getGroupId());
        if (delta != 0) {
            return delta;
        }
        delta = r1.getArtifactId().compareTo(r2.getArtifactId());
        if (delta != 0) {
            return delta;
        }
        delta = r1.getVersion().compareTo(r2.getVersion());
        if (delta != 0) {
            return delta;
        }
        return r1.toString().compareTo(r2.toString());
    };
}