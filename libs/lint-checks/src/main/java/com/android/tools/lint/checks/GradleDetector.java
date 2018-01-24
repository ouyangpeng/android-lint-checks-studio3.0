/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.tools.lint.checks;

import static com.android.SdkConstants.FD_BUILD_TOOLS;
import static com.android.SdkConstants.GRADLE_PLUGIN_MINIMUM_VERSION;
import static com.android.SdkConstants.GRADLE_PLUGIN_RECOMMENDED_VERSION;
import static com.android.SdkConstants.SUPPORT_LIB_GROUP_ID;
import static com.android.ide.common.repository.GoogleMavenRepository.MAVEN_GOOGLE_CACHE_DIR_KEY;
import static com.android.ide.common.repository.GradleCoordinate.COMPARE_PLUS_HIGHER;
import static com.android.sdklib.SdkVersionInfo.LOWEST_ACTIVE_API;
import static com.android.tools.lint.checks.ManifestDetector.TARGET_NEWER;
import static com.android.tools.lint.detector.api.LintUtils.guessGradleLocation;
import static com.google.common.base.Charsets.UTF_8;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.builder.model.AndroidArtifact;
import com.android.builder.model.AndroidLibrary;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.Dependencies;
import com.android.builder.model.JavaLibrary;
import com.android.builder.model.Library;
import com.android.builder.model.MavenCoordinates;
import com.android.builder.model.Variant;
import com.android.ide.common.repository.GoogleMavenRepository;
import com.android.ide.common.repository.GradleCoordinate;
import com.android.ide.common.repository.GradleCoordinate.RevisionComponent;
import com.android.ide.common.repository.GradleVersion;
import com.android.ide.common.repository.MavenRepositories;
import com.android.ide.common.repository.SdkMavenRepository;
import com.android.repository.io.FileOpUtils;
import com.android.sdklib.AndroidTargetHash;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkVersionInfo;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Checks Gradle files for potential errors
 */
public class GradleDetector extends Detector implements Detector.GradleScanner {

    private static final Implementation IMPLEMENTATION = new Implementation(
            GradleDetector.class,
            Scope.GRADLE_SCOPE);

    /** Obsolete dependencies */
    public static final Issue DEPENDENCY = Issue.create(
            "GradleDependency",
            "Obsolete Gradle Dependency",
            "This detector looks for usages of libraries where the version you are using " +
            "is not the current stable release. Using older versions is fine, and there are " +
            "cases where you deliberately want to stick with an older version. However, " +
            "you may simply not be aware that a more recent version is available, and that is " +
            "what this lint check helps find.",
            Category.CORRECTNESS,
            4,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Deprecated Gradle constructs */
    public static final Issue DEPRECATED = Issue.create(
            "GradleDeprecated",
            "Deprecated Gradle Construct",
            "This detector looks for deprecated Gradle constructs which currently work but " +
            "will likely stop working in a future update.",
            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Incompatible Android Gradle plugin */
    public static final Issue GRADLE_PLUGIN_COMPATIBILITY = Issue.create(
            "GradlePluginVersion",
            "Incompatible Android Gradle Plugin",
            "Not all versions of the Android Gradle plugin are compatible with all versions " +
            "of the SDK. If you update your tools, or if you are trying to open a project that " +
            "was built with an old version of the tools, you may need to update your plugin " +
            "version number.",
            Category.CORRECTNESS,
            8,
            Severity.ERROR,
            IMPLEMENTATION);

    /** Invalid or dangerous paths */
    public static final Issue PATH = Issue.create(
            "GradlePath",
            "Gradle Path Issues",
            "Gradle build scripts are meant to be cross platform, so file paths use " +
            "Unix-style path separators (a forward slash) rather than Windows path separators " +
            "(a backslash). Similarly, to keep projects portable and repeatable, avoid " +
            "using absolute paths on the system; keep files within the project instead. To " +
            "share code between projects, consider creating an android-library and an AAR " +
            "dependency",
            Category.CORRECTNESS,
            4,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Constructs the IDE support struggles with */
    public static final Issue IDE_SUPPORT = Issue.create(
            "GradleIdeError",
            "Gradle IDE Support Issues",
            "Gradle is highly flexible, and there are things you can do in Gradle files which " +
            "can make it hard or impossible for IDEs to properly handle the project. This lint " +
            "check looks for constructs that potentially break IDE support.",
            Category.CORRECTNESS,
            4,
            Severity.ERROR,
            IMPLEMENTATION);

    /** Using + in versions */
    public static final Issue PLUS = Issue.create(
            "GradleDynamicVersion",
            "Gradle Dynamic Version",
            "Using `+` in dependencies lets you automatically pick up the latest available " +
            "version rather than a specific, named version. However, this is not recommended; " +
            "your builds are not repeatable; you may have tested with a slightly different " +
            "version than what the build server used. (Using a dynamic version as the major " +
            "version number is more problematic than using it in the minor version position.)",
            Category.CORRECTNESS,
            4,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Accidentally calling a getter instead of your own methods */
    public static final Issue GRADLE_GETTER = Issue.create(
            "GradleGetter",
            "Gradle Implicit Getter Call",
            "Gradle will let you replace specific constants in your build scripts with method " +
            "calls, so you can for example dynamically compute a version string based on your " +
            "current version control revision number, rather than hardcoding a number.\n" +
            "\n" +
            "When computing a version name, it's tempting to for example call the method to do " +
            "that `getVersionName`. However, when you put that method call inside the " +
            "`defaultConfig` block, you will actually be calling the Groovy getter for the "  +
            "`versionName` property instead. Therefore, you need to name your method something " +
            "which does not conflict with the existing implicit getters. Consider using " +
            "`compute` as a prefix instead of `get`.",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            IMPLEMENTATION);

    /** Using incompatible versions */
    public static final Issue COMPATIBILITY = Issue.create(
            "GradleCompatible",
            "Incompatible Gradle Versions",

            "There are some combinations of libraries, or tools and libraries, that are " +
            "incompatible, or can lead to bugs. One such incompatibility is compiling with " +
            "a version of the Android support libraries that is not the latest version (or in " +
            "particular, a version lower than your `targetSdkVersion`.)",

            Category.CORRECTNESS,
            8,
            Severity.FATAL,
            IMPLEMENTATION);

    /** Using a string where an integer is expected */
    public static final Issue STRING_INTEGER = Issue.create(
            "StringShouldBeInt",
            "String should be int",

            "The properties `compileSdkVersion`, `minSdkVersion` and `targetSdkVersion` are " +
            "usually numbers, but can be strings when you are using an add-on (in the case " +
            "of `compileSdkVersion`) or a preview platform (for the other two properties).\n" +
            "\n" +
            "However, you can not use a number as a string (e.g. \"19\" instead of 19); that " +
            "will result in a platform not found error message at build/sync time.",

            Category.CORRECTNESS,
            8,
            Severity.ERROR,
            IMPLEMENTATION);

    /** Attempting to use substitution with single quotes */
    public static final Issue NOT_INTERPOLATED = Issue.create(
          "NotInterpolated",
          "Incorrect Interpolation",

          "To insert the value of a variable, you can use `${variable}` inside " +
          "a string literal, but **only** if you are using double quotes!",

          Category.CORRECTNESS,
          8,
          Severity.ERROR,
          IMPLEMENTATION)
          .addMoreInfo("http://www.groovy-lang.org/syntax.html#_string_interpolation");

    /** A newer version is available on a remote server */
    public static final Issue REMOTE_VERSION = Issue.create(
            "NewerVersionAvailable",
            "Newer Library Versions Available",
            "This detector checks with a central repository to see if there are newer versions " +
            "available for the dependencies used by this project. " +
            "This is similar to the `GradleDependency` check, which checks for newer versions " +
            "available in the Android SDK tools and libraries, but this works with any " +
            "MavenCentral dependency, and connects to the library every time, which makes " +
            "it more flexible but also **much** slower.",
            Category.CORRECTNESS,
            4,
            Severity.WARNING,
            IMPLEMENTATION).setEnabledByDefault(false);

    /** The API version is set too low. */
    public static final Issue MIN_SDK_TOO_LOW = Issue.create(
            "MinSdkTooLow",
            "API Version Too Low",
            "The value of the `minSdkVersion` property is too low and can be incremented" +
                    "without noticeably reducing the number of supported devices.",
            Category.CORRECTNESS,
            4,
            Severity.WARNING,
            IMPLEMENTATION).setEnabledByDefault(false);

    /** Accidentally using octal numbers */
    public static final Issue ACCIDENTAL_OCTAL = Issue.create(
            "AccidentalOctal",
            "Accidental Octal",

            "In Groovy, an integer literal that starts with a leading 0 will be interpreted " +
            "as an octal number. That is usually (always?) an accident and can lead to " +
            "subtle bugs, for example when used in the `versionCode` of an app.",

            Category.CORRECTNESS,
            2,
            Severity.ERROR,
            IMPLEMENTATION);

    @SuppressWarnings("SpellCheckingInspection")
    public static final Issue BUNDLED_GMS = Issue.create(
            "UseOfBundledGooglePlayServices",
            "Use of bundled version of Google Play services",

            "Google Play services SDK's can be selectively included, which enables a smaller APK " +
            "size. Consider declaring dependencies on individual Google Play services SDK's. " +
            "If you are using Firebase API's (http://firebase.google.com/docs/android/setup), " +
            "Android Studio's Tools \u2192 Firebase assistant window can automatically add " +
            "just the dependencies needed for each feature.",

            Category.PERFORMANCE,
            4,
            Severity.WARNING,
            IMPLEMENTATION)
            .addMoreInfo("http://developers.google.com/android/guides/setup#split");

    /**
     * Using a versionCode that is very high
     */
    public static final Issue HIGH_APP_VERSION_CODE = Issue.create(
            "HighAppVersionCode",
            "VersionCode too high",

            "The declared `versionCode` is an Integer. Ensure that the version number is " +
            "not close to the limit. It is recommended to monotonically increase this number " +
            "each minor or major release of the app. Note that updating an app with a " +
            "versionCode over `Integer.MAX_VALUE` is not possible.",

            Category.CORRECTNESS,
            8,
            Severity.ERROR,
            IMPLEMENTATION)
            .addMoreInfo("https://developer.android.com/studio/publish/versioning.html");

    /** Dev mode is no longer relevant */
    public static final Issue DEV_MODE_OBSOLETE = Issue.create(
            "DevModeObsolete",
            "Dev Mode Obsolete",
            "In the past, our documentation recommended creating a `dev` product flavor with " +
            "has a minSdkVersion of 21, in order to enable multidexing to speed up builds " +
            "significantly during development.\n" +
            "\n" +
            "That workaround is no longer necessary, and it has some serious downsides, such " +
            "as breaking API access checking (since the true `minSdkVersion` is no longer " +
            "known.)\n" +
            "\n" +
            "In recent versions of the IDE and the Gradle plugin, the IDE automatically passes " +
            "the API level of the connected device used for deployment, and if that device " +
            "is at least API 21, then multidexing is automatically turned on, meaning that " +
            "you get the same speed benefits as the `dev` product flavor but without the " +
            "downsides.",
            Category.PERFORMANCE,
            2,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Duplicate HTTP classes */
    public static final Issue DUPLICATE_CLASSES = Issue.create(
            "DuplicatePlatformClasses",
            "Duplicate Platform Classes",
            "There are a number of libraries that duplicate not just functionality of the " +
            "Android platform but using the exact same class names as the ones provided " +
            "in Android -- for example the apache http classes. This can lead to unexpected " +
            "crashes.\n" +
            "\n" +
            "To solve this, you need to either find a newer version of the library which " +
            "no longer has this problem, or to repackage the library (and all of its " +
            "dependencies) using something like the `jarjar` tool, or finally, rewriting " +
            "the code to use different APIs (for example, for http code, consider using " +
            "`HttpUrlConnection` or a library like `okhttp`.)",
            Category.CORRECTNESS,
            8,
            Severity.FATAL,
            IMPLEMENTATION);

    /** The Gradle plugin ID for Android applications */
    public static final String APP_PLUGIN_ID = "com.android.application";
    /** The Gradle plugin ID for Android libraries */
    public static final String LIB_PLUGIN_ID = "com.android.library";

    /** Previous plugin id for applications */
    public static final String OLD_APP_PLUGIN_ID = "android";
    /** Previous plugin id for libraries */
    public static final String OLD_LIB_PLUGIN_ID = "android-library";

    /** Group ID for GMS */
    public static final String GMS_GROUP_ID = "com.google.android.gms";
    public static final String FIREBASE_GROUP_ID = "com.google.firebase";
    public static final String GOOGLE_SUPPORT_GROUP_ID = "com.google.android.support";
    public static final String ANDROID_WEAR_GROUP_ID = "com.google.android.wearable";
    private static final String WEARABLE_ARTIFACT_ID = "wearable";

    @SuppressWarnings("ConstantConditions")
    @NonNull
    private static final GradleCoordinate PLAY_SERVICES_V650 =
            GradleCoordinate.parseCoordinateString(GMS_GROUP_ID + ":play-services:6.5.0");

    /**
     * Threshold to consider a versionCode very high and issue a warning.
     * https://developer.android.com/studio/publish/versioning.html indicates
     * that the highest value accepted by Google Play is 2100000000
     */
    private static final int VERSION_CODE_HIGH_THRESHOLD = 2000000000;
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    private int minSdkVersion;
    private int compileSdkVersion;
    private Object compileSdkVersionCookie;
    private int targetSdkVersion;

    // ---- Implements Detector.GradleScanner ----

    @Override
    public void visitBuildScript(@NonNull Context context, Map<String, Object> sharedData) {
    }

    @SuppressWarnings("UnusedDeclaration")
    protected static boolean isInterestingBlock(
            @NonNull String parent,
            @Nullable String parentParent) {
        switch (parent) {
            case "defaultConfig":
            case "android":
            case "dependencies":
            case "repositories":
                return true;
            case "dev":
                return "productFlavors".equals(parentParent);
            default:
                return "buildTypes".equals(parentParent);
        }
    }

    protected static boolean isInterestingStatement(
            @NonNull String statement,
            @Nullable String parent) {
        return parent == null && statement.equals("apply");
    }

    @SuppressWarnings("UnusedDeclaration")
    protected static boolean isInterestingProperty(
            @NonNull String property,
            @SuppressWarnings("UnusedParameters")
            @NonNull String parent,
            @Nullable String parentParent) {
        switch (property) {
            case "targetSdkVersion":
            case "buildToolsVersion":
            case "versionName":
            case "versionCode":
            case "compileSdkVersion":
            case "minSdkVersion":
            case "applicationIdSuffix":
            case "packageName":
            case "packageNameSuffix":
                //|| ) {
                return true;
            default:
                return parent.equals("dependencies");
        }
    }

    protected void checkOctal(
            @NonNull Context context,
            @NonNull String value,
            @NonNull Object cookie) {
        if (value.length() >= 2
                && value.charAt(0) == '0'
                && (value.length() > 2 || value.charAt(1) >= '8'
                && isNonnegativeInteger(value))
                && context.isEnabled(ACCIDENTAL_OCTAL)) {
            String message = "The leading 0 turns this number into octal which is probably "
                    + "not what was intended";
            try {
                long numericValue = Long.decode(value);
                message += " (interpreted as " + numericValue + ")";
            } catch (NumberFormatException nufe) {
                message += " (and it is not a valid octal number)";
            }
            report(context, cookie, ACCIDENTAL_OCTAL, message);
        }
    }

    /**
     * Called with for example "android", "defaultConfig", "minSdkVersion", "7"
     */
    @SuppressWarnings("UnusedDeclaration")
    protected void checkDslPropertyAssignment(
            @NonNull Context context,
            @NonNull String property,
            @NonNull String value,
            @NonNull String parent,
            @Nullable String parentParent,
            @NonNull Object valueCookie,
            @NonNull Object statementCookie) {
        if (parent.equals("defaultConfig")) {
            if (property.equals("targetSdkVersion")) {
                int version = getSdkVersion(value);
                if (version > 0 && version < context.getClient().getHighestKnownApiLevel()) {
                    String message = ""
                            + "Not targeting the latest versions of Android; compatibility \n"
                            + "modes apply. Consider testing and updating this version. \n"
                            + "Consult the android.os.Build.VERSION_CODES javadoc for details.";

                    int highest = context.getClient().getHighestKnownApiLevel();
                    String label = "Update targetSdkVersion to " + highest;
                    LintFix fix = fix().name(label)
                            .replace().all().with(Integer.toString(highest)).build();
                    report(context, valueCookie, TARGET_NEWER, message, fix);
                }
                if (version > 0) {
                    targetSdkVersion = version;
                    checkTargetCompatibility(context);
                } else {
                    checkIntegerAsString(context, value, valueCookie);
                }
            } else if (property.equals("minSdkVersion")) {
                int version = getSdkVersion(value);
                if (version > 0) {
                    minSdkVersion = version;
                    checkMinSdkVersion(context, version, valueCookie);
                } else {
                    checkIntegerAsString(context, value, valueCookie);
                }
            }

            if (value.startsWith("0")) {
                checkOctal(context, value, valueCookie);
            }

            if (property.equals("versionName") || property.equals("versionCode") &&
                    !isNonnegativeInteger(value) || !isStringLiteral(value)) {
                // Method call -- make sure it does not match one of the getters in the
                // configuration!
                if ((value.equals("getVersionCode") ||
                        value.equals("getVersionName"))) {
                    String message = "Bad method name: pick a unique method name which does not "
                            + "conflict with the implicit getters for the defaultConfig "
                            + "properties. For example, try using the prefix compute- "
                            + "instead of get-.";
                    report(context, valueCookie, GRADLE_GETTER, message);
                }
            } else if (property.equals("packageName")) {
                String message = "Deprecated: Replace 'packageName' with 'applicationId'";
                LintFix fix = fix().replace().text("packageName")
                        .with("applicationId").build();
                report(context, getPropertyKeyCookie(valueCookie), DEPRECATED, message, fix);
            }
            if (property.equals("versionCode") && context.isEnabled(HIGH_APP_VERSION_CODE)
                    && isNonnegativeInteger(value)) {
                int version = getIntLiteralValue(value, -1);
                if (version >= VERSION_CODE_HIGH_THRESHOLD) {
                    String message =
                            "The 'versionCode' is very high and close to the max allowed value";
                    report(context, valueCookie, HIGH_APP_VERSION_CODE, message);
                }
            }
        } else if (property.equals("compileSdkVersion") && parent.equals("android")) {
            int version = -1;
            if (isStringLiteral(value)) {
                // Try to resolve values like "android-O"
                String hash = getStringLiteralValue(value);
                if (hash != null && !isNumberString(hash)) {
                    AndroidVersion platformVersion = AndroidTargetHash.getPlatformVersion(hash);
                    if (platformVersion != null) {
                        version = platformVersion.getFeatureLevel();
                    }
                }
            } else {
                version = getIntLiteralValue(value, -1);
            }
            if (version > 0) {
                compileSdkVersion = version;
                compileSdkVersionCookie = valueCookie;
                checkTargetCompatibility(context);
            } else {
                checkIntegerAsString(context, value, valueCookie);
            }
        } else if (property.equals("buildToolsVersion") && parent.equals("android")) {
            String versionString = getStringLiteralValue(value);
            if (versionString != null) {
                GradleVersion version = GradleVersion.tryParse(versionString);
                if (version != null) {
                    GradleVersion recommended = getLatestBuildTools(context.getClient(),
                            version.getMajor());
                    if (recommended != null && version.compareTo(recommended) < 0) {
                        String message = "Old buildToolsVersion " + version +
                                "; recommended version is " + recommended + " or later";
                        LintFix fix = getUpdateDependencyFix(version.toString(),
                                recommended.toString());
                        report(context, valueCookie, DEPENDENCY, message, fix);
                    }

                    // 23.0.0 shipped with a serious bugs which affects program correctness
                    // (such as https://code.google.com/p/android/issues/detail?id=183180)
                    // Make developers aware of this and suggest upgrading
                    if (version.getMajor() == 23 && version.getMinor() == 0 &&
                            version.getMicro() == 0 && context.isEnabled(COMPATIBILITY)) {
                        // This specific version is actually a preview version which should
                        // not be used (https://code.google.com/p/android/issues/detail?id=75292)
                        if (recommended == null || recommended.getMajor() < 23) {
                            // First planned release to fix this
                            recommended = new GradleVersion(23, 0, 3);
                        }
                        String message = String.format("Build Tools `23.0.0` should not be used; "
                                + "it has some known serious bugs. Use version `%1$s` "
                                + "instead.", recommended);
                        reportFatalCompatibilityIssue(context, valueCookie, message);
                    }
                }
            }
        } else if (parent.equals("dependencies")) {
            if (value.startsWith("files('") && value.endsWith("')")) {
                String path = value.substring("files('".length(), value.length() - 2);
                if (path.contains("\\\\")) {
                    String message = "Do not use Windows file separators in .gradle files; "
                            + "use / instead";
                    report(context, valueCookie, PATH, message);

                } else if (path.startsWith("/")
                        || new File(path.replace('/', File.separatorChar)).isAbsolute()) {
                    String message = "Avoid using absolute paths in .gradle files";
                    report(context, valueCookie, PATH, message);
                }
            } else {
                String dependency = getStringLiteralValue(value);
                if (dependency == null) {
                    dependency = getNamedDependency(value);
                }
                // If the dependency is a GString (i.e. it uses Groovy variable substitution,
                // with a $variable_name syntax) then don't try to parse it.
                if (dependency != null) {
                    GradleCoordinate gc = GradleCoordinate.parseCoordinateString(dependency);
                    boolean isResolved = false;
                    if (gc != null && dependency.contains("$")) {
                        if (value.startsWith("'") && value.endsWith("'") &&
                                context.isEnabled(NOT_INTERPOLATED)) {
                            String message = "It looks like you are trying to substitute a "
                                    + "version variable, but using single quotes ('). For Groovy "
                                    + "string interpolation you must use double quotes (\").";
                            LintFix fix = fix()
                                    .name("Replace single quotes with double quotes").replace()
                                    .text(value)
                                    .with("\"" + value.substring(1, value.length() - 1) + "\"")
                                    .build();
                            report(context, statementCookie, NOT_INTERPOLATED, message, fix);
                        }

                        gc = resolveCoordinate(context, gc);
                        isResolved = true;
                    }
                    if (gc != null) {
                        if (gc.acceptsGreaterRevisions()) {
                            String message = "Avoid using + in version numbers; can lead "
                                    + "to unpredictable and unrepeatable builds (" + dependency
                                    + ")";
                            LintFix fix = fix().data(gc);
                            report(context, valueCookie, PLUS, message, fix);
                        }

                        checkDependency(context, gc, isResolved, valueCookie, statementCookie);
                    }
                }
            }
        } else if (property.equals("packageNameSuffix")) {
            String message = "Deprecated: Replace 'packageNameSuffix' with 'applicationIdSuffix'";
            LintFix fix = fix().replace().text("packageNameSuffix")
                    .with("applicationIdSuffix").build();
            report(context, getPropertyKeyCookie(valueCookie), DEPRECATED, message, fix);
        } else if (property.equals("applicationIdSuffix")) {
            String suffix = getStringLiteralValue(value);
            if (suffix != null && !suffix.startsWith(".")) {
                String message = "Application ID suffix should probably start with a \".\"";
                report(context, valueCookie, PATH, message);
            }
        } else if (property.equals("minSdkVersion")
                && parent.equals("dev")
                && "21".equals(value)
                // Don't flag this error from Gradle; users invoking lint from Gradle may
                // still want dev mode for command line usage
                && !LintClient.CLIENT_GRADLE.equals(LintClient.getClientName())) {
            report(context, statementCookie, DEV_MODE_OBSOLETE,
                    "You no longer need a `dev` mode to enable multi-dexing during "
                            + "development, and this can break API version checks");
        }
    }

    private void checkMinSdkVersion(Context context, int version, Object valueCookie) {
        if (version > 0 && version < LOWEST_ACTIVE_API) {
            String message = ""
                             + "The value of minSdkVersion is too low. It can be incremented\n"
                             + "without noticeably reducing the number of supported devices.";

            String label = "Update minSdkVersion to " + LOWEST_ACTIVE_API;
            LintFix fix = fix().name(label).replace()
                    .text(Integer.toString(version))
                    .with(Integer.toString(LOWEST_ACTIVE_API)).build();
            report(context, valueCookie, MIN_SDK_TOO_LOW, message, fix);
        }
    }

    private static int getSdkVersion(@NonNull String value) {
        int version = 0;
        if (isStringLiteral(value)) {
            String codeName = getStringLiteralValue(value);
            if (codeName != null) {
                if (isNumberString(codeName)) {
                    // Don't access numbered strings; should be literal numbers (lint will warn)
                    return -1;
                }
                AndroidVersion androidVersion = SdkVersionInfo.getVersion(codeName, null);
                if (androidVersion != null) {
                    version = androidVersion.getFeatureLevel();
                }
            }
        } else {
            version = getIntLiteralValue(value, -1);
        }
        return version;
    }

    @Nullable
    private static GradleCoordinate resolveCoordinate(@NonNull Context context,
            @NonNull GradleCoordinate gc) {
        assert gc.getRevision().contains("$") : gc.getRevision();
        Project project = context.getProject();
        Variant variant = project.getCurrentVariant();
        if (variant != null) {
            Dependencies dependencies = variant.getMainArtifact().getDependencies();
            for (AndroidLibrary library : dependencies.getLibraries()) {
                MavenCoordinates mc = library.getResolvedCoordinates();
                // Even though the method is annotated as non-null, this code can run
                // after a failed sync and there are observed scenarios where it returns
                // null in that ase
                //noinspection ConstantConditions
                if (mc != null
                        && mc.getGroupId().equals(gc.getGroupId())
                        && mc.getArtifactId().equals(gc.getArtifactId())) {
                    List<RevisionComponent> revisions =
                            GradleCoordinate.parseRevisionNumber(mc.getVersion());
                    if (!revisions.isEmpty()) {
                        return new GradleCoordinate(mc.getGroupId(), mc.getArtifactId(),
                                revisions, null);
                    }
                    break;
                }
            }
        }

        return null;
    }

    // Convert a long-hand dependency, like
    //    group: 'com.android.support', name: 'support-v4', version: '21.0.+'
    // into an equivalent short-hand dependency, like
    //   com.android.support:support-v4:21.0.+
    @VisibleForTesting
    @Nullable
    static String getNamedDependency(@NonNull String expression) {
        //if (value.startsWith("group: 'com.android.support', name: 'support-v4', version: '21.0.+'"))
        if (expression.indexOf(',') != -1 && expression.contains("version:")) {
            String artifact = null;
            String group = null;
            String version = null;
            Splitter splitter = Splitter.on(',').omitEmptyStrings().trimResults();
            for (String property : splitter.split(expression)) {
                int colon = property.indexOf(':');
                if (colon == -1) {
                    return null;
                }
                char quote = '\'';
                int valueStart = property.indexOf(quote, colon + 1);
                if (valueStart == -1) {
                    quote = '"';
                    valueStart = property.indexOf(quote, colon + 1);
                }
                if (valueStart == -1) {
                    // For example, "transitive: false"
                    continue;
                }
                valueStart++;
                int valueEnd = property.indexOf(quote, valueStart);
                if (valueEnd == -1) {
                    return null;
                }
                String value = property.substring(valueStart, valueEnd);
                if (property.startsWith("group:")) {
                    group = value;
                } else if (property.startsWith("name:")) {
                    artifact = value;
                } else if (property.startsWith("version:")) {
                    version = value;
                }
            }

            if (artifact != null && group != null && version != null) {
                return group + ':' + artifact + ':' + version;
            }
        }

        return null;
    }

    private void checkIntegerAsString(Context context, String value, Object valueCookie) {
        // When done developing with a preview platform you might be tempted to switch from
        //     compileSdkVersion 'android-G'
        // to
        //     compileSdkVersion '19'
        // but that won't work; it needs to be
        //     compileSdkVersion 19
        String string = getStringLiteralValue(value);
        if (isNumberString(string)) {
            String message = String.format("Use an integer rather than a string here "
                    + "(replace %1$s with just %2$s)", value, string);
            LintFix fix = fix()
                    .name("Replace with integer").replace().text(value)
                    .with(string)
                    .build();
            report(context, valueCookie, STRING_INTEGER, message, fix);
        }
    }

    private static boolean isNumberString(@Nullable String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (int i = 0, n = s.length(); i < n; i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    protected void checkMethodCall(
            @NonNull Context context,
            @NonNull String statement,
            @Nullable String parent,
            @NonNull Map<String, String> namedArguments,
            @SuppressWarnings("UnusedParameters")
            @NonNull List<String> unnamedArguments,
            @NonNull Object cookie) {
        String plugin = namedArguments.get("plugin");
        if (statement.equals("apply") && parent == null) {
            boolean isOldAppPlugin = OLD_APP_PLUGIN_ID.equals(plugin);
            if (isOldAppPlugin || OLD_LIB_PLUGIN_ID.equals(plugin)) {
                String replaceWith = isOldAppPlugin ? APP_PLUGIN_ID : LIB_PLUGIN_ID;
                String message = String.format("'%1$s' is deprecated; use '%2$s' instead", plugin,
                        replaceWith);
                LintFix fix = fix().replace().text(plugin).with(replaceWith).build();
                report(context, cookie, DEPRECATED, message, fix);
            }
        }
    }

    private static int sMajorBuildTools;
    private static GradleVersion sLatestBuildTools;

    /**
     * Returns the latest build tools installed for the given major version.
     * We just cache this once; we don't need to be accurate in the sense that if the
     * user opens the SDK manager and installs a more recent version, we capture this in
     * the same IDE session.
     *
     * @param client the associated client
     * @param major  the major version of build tools to look up (e.g. typically 18, 19, ...)
     * @return the corresponding highest known revision
     */
    @Nullable
    private static GradleVersion getLatestBuildTools(@NonNull LintClient client, int major) {
        if (major != sMajorBuildTools) {
            sMajorBuildTools = major;

            List<GradleVersion> revisions = new ArrayList<>();
            switch (major) {
                case 25:
                    revisions.add(new GradleVersion(25, 0, 2));
                    break;
                case 24:
                    revisions.add(new GradleVersion(24, 0, 2));
                    break;
                case 23:
                    revisions.add(new GradleVersion(23, 0, 3));
                    break;
                case 22:
                    revisions.add(new GradleVersion(22, 0, 1));
                    break;
                case 21:
                    revisions.add(new GradleVersion(21, 1, 2));
                    break;
                case 20:
                    revisions.add(new GradleVersion(20, 0));
                    break;
                case 19:
                    revisions.add(new GradleVersion(19, 1));
                    break;
                case 18:
                    revisions.add(new GradleVersion(18, 1, 1));
                    break;
            }

            // The above versions can go stale.
            // Check if a more recent one is installed. (The above are still useful for
            // people who haven't updated with the SDK manager recently.)
            File sdkHome = client.getSdkHome();
            if (sdkHome != null) {
                File[] dirs = new File(sdkHome, FD_BUILD_TOOLS).listFiles();
                if (dirs != null) {
                    for (File dir : dirs) {
                        String name = dir.getName();
                        if (!dir.isDirectory() || !Character.isDigit(name.charAt(0))) {
                            continue;
                        }
                        GradleVersion v = GradleVersion.tryParse(name);
                        if (v != null && v.getMajor() == major) {
                            revisions.add(v);
                        }
                    }
                }
            }

            if (!revisions.isEmpty()) {
                sLatestBuildTools = Collections.max(revisions);
            }
        }

        return sLatestBuildTools;
    }

    private void checkTargetCompatibility(Context context) {
        if (compileSdkVersion > 0 && targetSdkVersion > 0
                && targetSdkVersion > compileSdkVersion) {
            String message = "The compileSdkVersion (" + compileSdkVersion
                    + ") should not be lower than the targetSdkVersion ("
                    + targetSdkVersion + ")";
            LintFix fix = fix()
                    .name("Set compileSdkVersion to " + targetSdkVersion).replace()
                    .text(Integer.toString(compileSdkVersion))
                    .with(Integer.toString(targetSdkVersion)).build();
            reportNonFatalCompatibilityIssue(context, compileSdkVersionCookie, message, fix);
        }
    }

    @Nullable
    private static String getStringLiteralValue(@NonNull String value) {
        if (value.length() > 2 && (value.startsWith("'") && value.endsWith("'") ||
                value.startsWith("\"") && value.endsWith("\""))) {
            return value.substring(1, value.length() - 1);
        }

        return null;
    }

    private static int getIntLiteralValue(@NonNull String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean isNonnegativeInteger(String token) {
        return DIGITS.matcher(token).matches();
    }

    private static boolean isStringLiteral(String token) {
        return token.startsWith("\"") && token.endsWith("\"") ||
                token.startsWith("'") && token.endsWith("'");
    }

    private void checkDependency(
        @NonNull Context context,
        @NonNull GradleCoordinate dependency,
        boolean isResolved,
        @NonNull Object cookie,
        @NonNull Object statementCookie) {
        GradleVersion version = dependency.getVersion();
        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();
        String revision = dependency.getRevision();
        if (version == null || groupId == null || artifactId == null) {
            return;
        }
        GradleVersion newerVersion = null;

        Predicate<GradleVersion> filter = getUpgradeVersionFilter(groupId, artifactId);

        switch (groupId) {
            case SUPPORT_LIB_GROUP_ID:
            case "com.android.support.test": {
                // Check to make sure you have the Android support repository installed.
                File sdkHome = context.getClient().getSdkHome();
                File repository = SdkMavenRepository.ANDROID.getRepositoryLocation(sdkHome, true,
                        FileOpUtils.create());
                if (repository != null) {
                    GradleVersion max = MavenRepositories.getHighestInstalledVersionNumber(
                            groupId, artifactId, repository, filter, false, FileOpUtils.create());
                    if (max != null && version.compareTo(max) < 0 && context.isEnabled(DEPENDENCY)) {
                        newerVersion = max;
                    }
                }

                break;
            }

            case GMS_GROUP_ID:
            case FIREBASE_GROUP_ID:
            case GOOGLE_SUPPORT_GROUP_ID:
            case ANDROID_WEAR_GROUP_ID: {
                // Play services

                checkPlayServices(context, dependency, version, revision, cookie);

                File sdkHome = context.getClient().getSdkHome();
                File repository = SdkMavenRepository.GOOGLE.getRepositoryLocation(sdkHome, true,
                        FileOpUtils.create());
                if (repository != null) {
                    GradleVersion max = MavenRepositories.getHighestInstalledVersionNumber(
                            groupId, artifactId, repository, filter, false, FileOpUtils.create());
                    if (max != null && version.compareTo(max) < 0 && context.isEnabled(DEPENDENCY)) {
                        newerVersion = max;
                    }
                }

                break;
            }

            case "com.android.tools.build": {
                if ("gradle".equals(artifactId)) {
                    if (checkGradlePluginDependency(context, dependency, cookie)) {
                        return;
                    }

                    // If it's available in maven.google.com, fetch latest available version
                    newerVersion = GradleVersion.max(version,
                            getGoogleMavenRepoVersion(context, dependency, filter));
                }
                break;
            }

            case "com.google.guava": {
                if ("guava".equals(artifactId)) {
                    newerVersion = getNewerVersion(version, 21, 0);
                }
                break;
            }

            case "com.google.code.gson": {
                if ("gson".equals(artifactId)) {
                    newerVersion = getNewerVersion(version, 2, 8, 0);
                }
                break;
            }
            case "org.apache.httpcomponents": {
                if ("httpclient".equals(artifactId)) {
                    newerVersion = getNewerVersion(version, 4, 3, 5);
                }
                break;
            }
            case "com.squareup.okhttp3": {
                if ("okhttp".equals(artifactId)) {
                    newerVersion = getNewerVersion(version, 3, 7, 0);
                }
                break;
            }
            case "com.github.bumptech.glide": {
                if ("glide".equals(artifactId)) {
                    newerVersion = getNewerVersion(version, 3, 7, 0);
                }
                break;
            }
            case "io.fabric.tools": {
                if ("gradle".equals(artifactId)) {
                    GradleVersion parsed = GradleVersion.tryParse(revision);
                    if (parsed != null && parsed.compareTo("1.21.6") < 0) {
                        LintFix fix = getUpdateDependencyFix(revision, "1.22.1");
                        report(context, cookie, DEPENDENCY,
                          "Use Fabric Gradle plugin version 1.21.6 or "
                          + "later to improve Instant Run performance (was " +
                          revision + ")", fix);
                    } else {
                        // From https://s3.amazonaws.com/fabric-artifacts/public/io/fabric/tools/gradle/maven-metadata.xml
                        newerVersion = getNewerVersion(version, new GradleVersion(1, 22, 1));
                    }
                }
                break;
            }
            case "com.bugsnag": {
                if ("bugsnag-android-gradle-plugin".equals(artifactId)) {
                    if (!version.isAtLeast(2, 1, 2)) {
                        LintFix fix = getUpdateDependencyFix(revision, "2.4.1");
                        report(context, cookie, DEPENDENCY,
                          "Use BugSnag Gradle plugin version 2.1.2 or "
                            + "later to improve Instant Run performance (was "
                            + revision + ")", fix);
                    } else {
                        // From http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.bugsnag%22%20AND
                        // %20a%3A%22bugsnag-android-gradle-plugin%22
                        newerVersion = getNewerVersion(version, 2, 4, 1);
                    }
                }
                break;
            }
        }

        BlacklistedDeps blacklistedDeps = blacklisted.get(context.getProject());
        List<Library> path = blacklistedDeps.checkDependency(groupId, artifactId, true);
        if (path != null) {
            String message = getBlacklistedDependencyMessage(context, path);
            if (message != null) {
                LintFix fix = fix().name("Delete dependency").replace().all().build();
                report(context, statementCookie, DUPLICATE_CLASSES, message, fix);
            }
        }

        // Network check for really up to date libraries? Only done in batch mode.
        Issue issue = DEPENDENCY;
        if (context.getScope().size() > 1 && context.isEnabled(REMOTE_VERSION)) {
            GradleVersion latest = getLatestVersionFromRemoteRepo(context.getClient(), dependency,
                    filter, dependency.isPreview());
            if (latest != null && version.compareTo(latest) < 0) {
                newerVersion = latest;
                issue = REMOTE_VERSION;
            }
        }

        // Compare with what's in the Gradle cache.
        newerVersion = GradleVersion.max(newerVersion, findCachedNewerVersion(dependency, filter));

        // Compare with IDE's repository cache, if available.
        newerVersion = GradleVersion.max(newerVersion, getHighestKnownVersion(context.getClient(),
                dependency, filter));

        // If it's available in maven.google.com, fetch latest available version.
        newerVersion = GradleVersion.max(newerVersion,
                getGoogleMavenRepoVersion(context, dependency, filter));

        if (groupId.equals(SUPPORT_LIB_GROUP_ID) || groupId.equals("com.android.support.test")) {
            checkSupportLibraries(context, dependency, version, newerVersion, cookie);
        }

        if (newerVersion != null && newerVersion.compareTo(version) > 0) {
            String versionString = newerVersion.toString();
            String message = getNewerVersionAvailableMessage(dependency, versionString);
            LintFix fix = !isResolved ? getUpdateDependencyFix(revision, versionString) : null;
            report(context, cookie, issue, message, fix);
        }
    }

    /** True if the given project uses the legacy http library */
    private static boolean usesLegacyHttpLibrary(@NonNull Project project) {
        AndroidProject model = project.getGradleProjectModel();
        if (model == null) {
            return false;
        }
        for (String path : model.getBootClasspath()) {
            if (path.endsWith("org.apache.http.legacy.jar")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns a predicate that encapsulates version constraints for the given library, or null if
     * there are no constraints.
     */
    @Nullable
    private Predicate<GradleVersion> getUpgradeVersionFilter(@NonNull String groupId,
            @NonNull String artifactId) {
        // Logic here has to match checkSupportLibraries method to avoid creating contradictory
        // warnings.
        if (isSupportLibraryDependentOnCompileSdk(groupId, artifactId)) {
            if (compileSdkVersion >= 18) {
                return version -> version.getMajor() == compileSdkVersion;
            } else if (targetSdkVersion > 0) {
                return version -> version.getMajor() >= targetSdkVersion;
            }
        }
        return null;
    }

    @VisibleForTesting
    static GoogleMavenRepository googleMavenRepository;

    @Nullable
    private static GradleVersion getGoogleMavenRepoVersion(@NonNull Context context,
            @NonNull GradleCoordinate dependency,
            @Nullable Predicate<GradleVersion> filter) {
        synchronized (GradleDetector.class) {
            if (googleMavenRepository == null) {
                LintClient client = context.getClient();
                File cacheDir = client.getCacheDir(MAVEN_GOOGLE_CACHE_DIR_KEY, true);
                googleMavenRepository = new GoogleMavenRepository(cacheDir) {
                    @Override
                    @Nullable
                    public byte[] readUrlData(@NonNull String url, int timeout) {
                        try {
                            return LintUtils.readUrlData(client, url, timeout);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void error(@NonNull Throwable throwable, @Nullable String message) {
                        client.log(throwable, message);
                    }
                };
            }
        }

        return googleMavenRepository.findVersion(dependency, filter, dependency.isPreview());
    }

    protected File getGradleUserHome() {
        // See org.gradle.initialization.BuildLayoutParameters
        String gradleUserHome = System.getProperty("gradle.user.home");
        if (gradleUserHome == null) {
            gradleUserHome = System.getenv("GRADLE_USER_HOME");
            if (gradleUserHome == null) {
                gradleUserHome = System.getProperty("user.home") + File.separator + ".gradle";
            }
        }

        return new File(gradleUserHome);
    }

    private File artifactCacheHome;

    /**
     * Home in the Gradle cache for artifact caches
     */
    protected File getArtifactCacheHome() {
        if (artifactCacheHome == null) {
            artifactCacheHome = new File(getGradleUserHome(), "caches"
                    + File.separator + "modules-2" + File.separator + "files-2.1");
        }

        return artifactCacheHome;
    }

    @Nullable
    private GradleVersion findCachedNewerVersion(GradleCoordinate dependency,
            @Nullable Predicate<GradleVersion> filter) {
        File versionDir = new File(getArtifactCacheHome(),
                dependency.getGroupId() + File.separator + dependency.getArtifactId());
        if (versionDir.exists()) {
            return MavenRepositories.getHighestVersion(versionDir, filter,
                    MavenRepositories.isPreview(dependency), FileOpUtils.create());
        }

        return null;
    }

    private void ensureTargetCompatibleWithO(@NonNull Context context,
            @Nullable GradleVersion version, @NonNull Object cookie,
            int major, int minor, int micro) {
        if (version != null && !version.isAtLeast(major, minor, micro)) {
            GradleVersion revision = new GradleVersion(major, minor, micro);
            GradleVersion newest = getNewerVersion(version, revision);
            if (newest != null) {
                revision = newest;
            }

            String message = String.format("Version must be at least %1$s when "
                    + "targeting O", revision);

            reportFatalCompatibilityIssue(context, cookie, message);
        }
    }

    @NonNull
    private static LintFix getUpdateDependencyFix(
            @NonNull String currentVersion,
            @NonNull String suggestedVersion) {
        return fix()
                .name("Change to " + suggestedVersion).replace().text(currentVersion)
                .with(suggestedVersion)
                .build();
    }

    private static String getNewerVersionAvailableMessage(GradleCoordinate dependency,
            String version) {
        return "A newer version of " + dependency.getGroupId() + ":" +
                dependency.getArtifactId() + " than " + dependency.getRevision() +
                " is available: " + version;
    }

    // Overridden in Studio to consult SDK manager's cache
    @SuppressWarnings({"MethodMayBeStatic", "unused"})
    @Nullable
    protected GradleVersion getHighestKnownVersion(
            @NonNull LintClient client,
            @NonNull GradleCoordinate coordinate,
            @Nullable Predicate<GradleVersion> filter) {
        return null;
    }

    /**
     * TODO: Cache these results somewhere!
     */
    @Nullable
    public static GradleVersion getLatestVersionFromRemoteRepo(@NonNull LintClient client,
            @NonNull GradleCoordinate dependency, @Nullable Predicate<GradleVersion> filter,
            boolean allowPreview) {
        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();
        if (groupId == null || artifactId == null) {
            return null;
        }
        StringBuilder query = new StringBuilder();
        String encoding = UTF_8.name();
        try {
            query.append("http://search.maven.org/solrsearch/select?q=g:%22");
            query.append(URLEncoder.encode(groupId, encoding));
            query.append("%22+AND+a:%22");
            query.append(URLEncoder.encode(artifactId, encoding));
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        query.append("%22&core=gav");
        if (filter == null && allowPreview) {
            query.append("&rows=1");
        }
        query.append("&wt=json");

        String response;
        try {
            response = LintUtils.readUrlDataAsString(client, query.toString(), 20000);
            if (response == null) {
                return null;
            }
        } catch (IOException e) {
            client.log(e, "Could not connect to maven central to look up the latest " +
                    "available version for %1$s", dependency);
            return null;
        }

        // Sample response:
        //    {
        //        "responseHeader": {
        //            "status": 0,
        //            "QTime": 0,
        //            "params": {
        //                "fl": "id,g,a,v,p,ec,timestamp,tags",
        //                "sort": "score desc,timestamp desc,g asc,a asc,v desc",
        //                "indent": "off",
        //                "q": "g:\"com.google.guava\" AND a:\"guava\"",
        //                "core": "gav",
        //                "wt": "json",
        //                "rows": "1",
        //                "version": "2.2"
        //            }
        //        },
        //        "response": {
        //            "numFound": 37,
        //            "start": 0,
        //            "docs": [{
        //                "id": "com.google.guava:guava:17.0",
        //                "g": "com.google.guava",
        //                "a": "guava",
        //                "v": "17.0",
        //                "p": "bundle",
        //                "timestamp": 1398199666000,
        //                "tags": ["spec", "libraries", "classes", "google", "code"],
        //                "ec": ["-javadoc.jar", "-sources.jar", ".jar", "-site.jar", ".pom"]
        //            }]
        //        }
        //    }

        // Look for version info:  This is just a cheap skim of the above JSON results.
        int index = response.indexOf("\"response\"");
        while (index != -1) {
            index = response.indexOf("\"v\":", index);
            if (index != -1) {
                index += 4;
                int start = response.indexOf('"', index) + 1;
                int end = response.indexOf('"', start + 1);
                if (end > start && start >= 0) {
                    GradleVersion revision = GradleVersion.tryParse(response.substring(start, end));
                    if (revision != null) {
                        if ((allowPreview || !revision.isPreview())
                                && (filter == null || filter.test(revision))) {
                            return revision;
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean checkGradlePluginDependency(Context context, GradleCoordinate dependency,
            Object cookie) {
        GradleCoordinate minimum = GradleCoordinate.parseCoordinateString(
                SdkConstants.GRADLE_PLUGIN_NAME + GRADLE_PLUGIN_MINIMUM_VERSION);
        if (minimum != null && COMPARE_PLUS_HIGHER.compare(dependency, minimum) < 0) {
            GradleVersion recommended = GradleVersion.max(
                    getGoogleMavenRepoVersion(context, minimum, null),
                    GradleVersion.tryParse(GRADLE_PLUGIN_RECOMMENDED_VERSION));
            String message = "You must use a newer version of the Android Gradle plugin. The "
                    + "minimum supported version is " + GRADLE_PLUGIN_MINIMUM_VERSION +
                    " and the recommended version is " +  recommended;
            report(context, cookie, GRADLE_PLUGIN_COMPATIBILITY, message);
            return true;
        }
        return false;
    }

    private void checkSupportLibraries(
            @NonNull Context context,
            @NonNull GradleCoordinate dependency,
            @NonNull GradleVersion version,
            @Nullable GradleVersion newerVersion,
            @NonNull Object cookie) {
        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();
        assert groupId != null && artifactId != null;

        // For artifacts that follow the platform numbering scheme, check that it matches the SDK
        // versions used.
        if (isSupportLibraryDependentOnCompileSdk(groupId, artifactId)) {
            if (compileSdkVersion >= 18
                    && dependency.getMajorVersion() != compileSdkVersion
                    && dependency.getMajorVersion() != GradleCoordinate.PLUS_REV_VALUE
                    && context.isEnabled(COMPATIBILITY)) {
                LintFix fix = null;
                if (newerVersion != null) {
                    fix = fix().name("Replace with " + newerVersion)
                            .replace().text(version.toString())
                            .with(newerVersion.toString()).build();
                }
                String message = "This support library should not use a different version ("
                        + dependency.getMajorVersion() + ") than the `compileSdkVersion` ("
                        + compileSdkVersion + ")";
                reportNonFatalCompatibilityIssue(context, cookie, message, fix);
            } else if (targetSdkVersion > 0
                    && dependency.getMajorVersion() < targetSdkVersion
                    && dependency.getMajorVersion() != GradleCoordinate.PLUS_REV_VALUE
                    && context.isEnabled(COMPATIBILITY)) {
                LintFix fix = null;
                if (newerVersion != null) {
                    fix = fix().name("Replace with " + newerVersion)
                            .replace().text(version.toString())
                            .with(newerVersion.toString()).build();
                }
                String message = "This support library should not use a lower version ("
                        + dependency.getMajorVersion() + ") than the `targetSdkVersion` ("
                        + targetSdkVersion + ")";
                reportNonFatalCompatibilityIssue(context, cookie, message, fix);
            }
        }

        if (!mCheckedSupportLibs
                && !artifactId.startsWith("multidex")
                && !artifactId.startsWith("renderscript")
                && !artifactId.equals("support-annotations")) {
            mCheckedSupportLibs = true;
            if (!context.getScope().contains(Scope.ALL_RESOURCE_FILES)) {
                // Incremental editing: try flagging them in this file!
                checkConsistentSupportLibraries(context, cookie);
            }
        }

        if ("appcompat-v7".equals(artifactId)) {
            boolean supportLib26Beta = version.isAtLeast(26, 0, 0, "beta", 1, true);
            boolean compile26Beta = compileSdkVersion >= 26;
            // It's not actually compileSdkVersion 26, it's using O revision 2 or higher
            if (compileSdkVersion == 26) {
                IAndroidTarget buildTarget = context.getProject().getBuildTarget();
                if (buildTarget != null && buildTarget.getVersion().isPreview()) {
                    compile26Beta = buildTarget.getRevision() != 1;
                }
            }

            if (supportLib26Beta && !compile26Beta
                    // We already flag problems when these aren't matching.
                    && compileSdkVersion == version.getMajor()) {
                reportNonFatalCompatibilityIssue(context, cookie, String.format(
                        "When using a `compileSdkVersion` older than android-O revision 2, the "
                              + "support library version must be 26.0.0-alpha1 or lower (was %1$s)",
                        version));
            } else if (!supportLib26Beta && compile26Beta) {
                reportNonFatalCompatibilityIssue(context, cookie,
                        String.format("When using a `compileSdkVersion` android-O revision 2 "
                              + "or higher, the support library version should be 26.0.0-beta1 "
                              + "or higher (was %1$s)", version));
            }

            if (minSdkVersion >= 14 && compileSdkVersion >= 1 && compileSdkVersion < 21) {
                report(context, cookie, DEPENDENCY,
                        "Using the appcompat library when minSdkVersion >= 14 and "
                              + "compileSdkVersion < 21 is not necessary");
            }
        }
    }

    /**
     * Checks if the library with the given {@code groupId} and {@code artifactId} has to match
     * compileSdkVersion.
     */
    private static boolean isSupportLibraryDependentOnCompileSdk(
            @NonNull String groupId,
            @NonNull String artifactId) {
        return SUPPORT_LIB_GROUP_ID.equals(groupId)
                && !artifactId.startsWith("multidex")
                && !artifactId.startsWith("renderscript")
                // Support annotation libraries work with any compileSdkVersion
                && !artifactId.equals("support-annotations");
    }


    /**
     * If incrementally editing a single build.gradle file, tracks whether we've already
     * transitively checked GMS versions such that we don't flag the same error on every
     * single dependency declaration
     */
    private boolean mCheckedGms;

    /**
     * If incrementally editing a single build.gradle file, tracks whether we've already
     * transitively checked support library versions such that we don't flag the same
     * error on every single dependency declaration
     */
    private boolean mCheckedSupportLibs;

    /**
     * If incrementally editing a single build.gradle file, tracks whether we've already
     * transitively checked wearable library versions such that we don't flag the same
     * error on every single dependency declaration
     */
    private boolean mCheckedWearableLibs;

    private void checkPlayServices(
            @NonNull Context context,
            @NonNull GradleCoordinate dependency,
            @NonNull GradleVersion version, @NonNull String revision, @NonNull Object cookie) {
        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();
        assert groupId != null && artifactId != null;

        // 5.2.08 is not supported; special case and warn about this
        if ("5.2.08".equals(revision) && context.isEnabled(COMPATIBILITY)) {
            // This specific version is actually a preview version which should
            // not be used (https://code.google.com/p/android/issues/detail?id=75292)
            String maxVersion = "10.2.1";
            // Try to find a more recent available version, if one is available
            File sdkHome = context.getClient().getSdkHome();
            File repository = SdkMavenRepository.GOOGLE.getRepositoryLocation(sdkHome, true,
                    FileOpUtils.create());
            if (repository != null) {
                GradleCoordinate max = MavenRepositories.getHighestInstalledVersion(
                        groupId, artifactId, repository,
                        null, false, FileOpUtils.create());
                if (max != null) {
                    if (COMPARE_PLUS_HIGHER.compare(dependency, max) < 0) {
                        maxVersion = max.getRevision();
                    }
                }
            }
            LintFix fix = getUpdateDependencyFix(revision, maxVersion);
            String message = String.format("Version `5.2.08` should not be used; the app "
                    + "can not be published with this version. Use version `%1$s` "
                    + "instead.", maxVersion);
            reportFatalCompatibilityIssue(context, cookie, message, fix);
        }

        if (context.isEnabled(BUNDLED_GMS)
                && PLAY_SERVICES_V650.isSameArtifact(dependency)
                && COMPARE_PLUS_HIGHER.compare(dependency, PLAY_SERVICES_V650) >= 0) {
            // Play services 6.5.0 is the first version to allow un-bundling, so if the user is
            // at or above 6.5.0, recommend un-bundling
            String message = "Avoid using bundled version of Google Play services SDK.";
            report(context, cookie, BUNDLED_GMS, message);

        }

        if (GMS_GROUP_ID.equals(groupId)
                && "play-services-appindexing".equals(artifactId)) {
            String message = "Deprecated: Replace '" + GMS_GROUP_ID
                    + ":play-services-appindexing:" + revision
                    + "' with 'com.google.firebase:firebase-appindexing:10.0.0' or above. "
                    + "More info: http://firebase.google.com/docs/app-indexing/android/migrate";
            LintFix fix = fix()
                    .name("Replace with Firebase").replace()
                    .text(GMS_GROUP_ID + ":play-services-appindexing:" + revision)
                    .with("com.google.firebase:firebase-appindexing:10.2.1").build();
            report(context, cookie, DEPRECATED, message, fix);
        }

        if (targetSdkVersion >= 26) {
            // When targeting O the following libraries must be using at least version 10.2.1
            // (or 0.6.0 of the jobdispatcher API)
            //com.google.android.gms:play-services-gcm:V
            //com.google.firebase:firebase-messaging:V
            if (GMS_GROUP_ID.equals(groupId)
                    && "play-services-gcm".equals(artifactId)) {
                ensureTargetCompatibleWithO(context, version, cookie, 10, 2, 1);
            } else if (FIREBASE_GROUP_ID.equals(groupId)
                    && "firebase-messaging".equals(artifactId)) {
                ensureTargetCompatibleWithO(context, version, cookie, 10, 2, 1);
            } else if ("firebase-jobdispatcher".equals(artifactId)
                    || "firebase-jobdispatcher-with-gcm-dep".equals(artifactId)) {
                ensureTargetCompatibleWithO(context, version, cookie, 0, 6, 0);
            }
        }

        if (GMS_GROUP_ID.equals(groupId) || FIREBASE_GROUP_ID.equals(groupId)) {
            if (!mCheckedGms) {
                mCheckedGms = true;
                // Incremental analysis only? If so, tie the check to
                // a specific GMS play dependency if only, such that it's highlighted
                // in the editor
                if (!context.getScope().contains(Scope.ALL_RESOURCE_FILES)) {
                    // Incremental editing: try flagging them in this file!
                    checkConsistentPlayServices(context, cookie);
                }
            }
        } else {
            if (!mCheckedWearableLibs) {
                mCheckedWearableLibs = true;
                // Incremental analysis only? If so, tie the check to
                // a specific GMS play dependency if only, such that it's highlighted
                // in the editor
                if (!context.getScope().contains(Scope.ALL_RESOURCE_FILES)) {
                    // Incremental editing: try flagging them in this file!
                    checkConsistentWearableLibraries(context, cookie);
                }
            }
        }
    }

    private void checkConsistentSupportLibraries(@NonNull Context context,
            @Nullable Object cookie) {
        checkConsistentLibraries(context, cookie, SUPPORT_LIB_GROUP_ID, null);
    }

    private void checkConsistentPlayServices(@NonNull Context context,
            @Nullable Object cookie) {
        checkConsistentLibraries(context, cookie, GMS_GROUP_ID, FIREBASE_GROUP_ID);
    }

    private void checkConsistentWearableLibraries(@NonNull Context context,
            @Nullable Object cookie) {
        // Make sure we have both
        //   compile 'com.google.android.support:wearable:2.0.0-alpha3'
        //   provided 'com.google.android.wearable:wearable:2.0.0-alpha3'
        Project project = context.getMainProject();
        if (!project.isGradleProject()) {
            return;
        }
        Set<String> supportVersions = new HashSet<>();
        Set<String> wearableVersions = new HashSet<>();
        for (AndroidLibrary library : getAndroidLibraries(project)) {
            MavenCoordinates coordinates = library.getResolvedCoordinates();
            // Claims to be non-null but may not be after a failed gradle sync
            //noinspection ConstantConditions
            if (coordinates != null &&
                    WEARABLE_ARTIFACT_ID.equals(coordinates.getArtifactId()) &&
                    GOOGLE_SUPPORT_GROUP_ID.equals(coordinates.getGroupId())) {
                supportVersions.add(coordinates.getVersion());
            }
        }
        for (JavaLibrary library : getJavaLibraries(project)) {
            MavenCoordinates coordinates = library.getResolvedCoordinates();
            // Claims to be non-null but may not be after a failed gradle sync
            //noinspection ConstantConditions
            if (coordinates != null &&
                    WEARABLE_ARTIFACT_ID.equals(coordinates.getArtifactId()) &&
                    ANDROID_WEAR_GROUP_ID.equals(coordinates.getGroupId())) {
                if (!library.isProvided()) {
                    if (cookie != null) {
                        String message = "This dependency should be marked as "
                                + "`provided`, not `compile`";

                        reportFatalCompatibilityIssue(context, cookie, message);
                    } else {
                        String message = String.format("The %1$s:%2$s dependency should be "
                                        + "marked as `provided`, not `compile`",
                                ANDROID_WEAR_GROUP_ID,
                                WEARABLE_ARTIFACT_ID);
                        reportFatalCompatibilityIssue(context,
                                guessGradleLocation(context.getProject()),
                                message);
                    }
                }
                wearableVersions.add(coordinates.getVersion());
            }
        }

        if (!supportVersions.isEmpty()) {
            if (wearableVersions.isEmpty()) {
                List<String> list = new ArrayList<>(supportVersions);
                String first = Collections.min(list);
                String message = String.format("Project depends on %1$s:%2$s:%3$s, so it must "
                                + "also depend (as a provided dependency) on %4$s:%5$s:%6$s",
                        GOOGLE_SUPPORT_GROUP_ID,
                        WEARABLE_ARTIFACT_ID,
                        first,
                        ANDROID_WEAR_GROUP_ID,
                        WEARABLE_ARTIFACT_ID,
                        first);
                if (cookie != null) {
                    reportFatalCompatibilityIssue(context, cookie, message);
                } else {
                    reportFatalCompatibilityIssue(context,
                            guessGradleLocation(context.getProject()),
                            message);
                }
            } else {
                // Check that they have the same versions
                if (!supportVersions.equals(wearableVersions)) {
                    List<String> sortedSupportVersions = new ArrayList<>(supportVersions);
                    Collections.sort(sortedSupportVersions);
                    List<String> supportedWearableVersions = new ArrayList<>(wearableVersions);
                    Collections.sort(supportedWearableVersions);
                    String message = String.format("The wearable libraries for %1$s and %2$s " +
                                    "must use **exactly** the same versions; found %3$s " +
                                    "and %4$s",
                            GOOGLE_SUPPORT_GROUP_ID,
                            ANDROID_WEAR_GROUP_ID,
                            sortedSupportVersions.size() == 1 ? sortedSupportVersions.get(0)
                                    : sortedSupportVersions.toString(),
                            supportedWearableVersions.size() == 1 ? supportedWearableVersions.get(0)
                                    : supportedWearableVersions.toString());
                    if (cookie != null) {
                        reportFatalCompatibilityIssue(context, cookie, message);
                    } else {
                        reportFatalCompatibilityIssue(context,
                                guessGradleLocation(context.getProject()),
                                message);
                    }
                }
            }
        }
    }

    private void checkConsistentLibraries(@NonNull Context context,
            @Nullable Object cookie, @NonNull String groupId, @Nullable String groupId2) {
        // Make sure we're using a consistent version across all play services libraries
        // (b/22709708)

        Project project = context.getMainProject();
        Multimap<String, MavenCoordinates> versionToCoordinate = ArrayListMultimap.create();
        Collection<AndroidLibrary> androidLibraries = getAndroidLibraries(project);
        for (AndroidLibrary library : androidLibraries) {
            MavenCoordinates coordinates = library.getResolvedCoordinates();
            // Claims to be non-null but may not be after a failed gradle sync
            //noinspection ConstantConditions
            if (coordinates != null && (coordinates.getGroupId().equals(groupId)
                    || (coordinates.getGroupId().equals(groupId2)))
                    // Historically the multidex library ended up in the support package but
                    // decided to do its own numbering (and isn't tied to the rest in terms
                    // of implementation dependencies)
                    && !coordinates.getArtifactId().startsWith("multidex")
                    // Renderscript has stated in b/37630182 that they are built and
                    // distributed separate from the rest and do not have any version
                    // dependencies
                    && !coordinates.getArtifactId().startsWith("renderscript")
                    // Similarly firebase job dispatcher doesn't follow normal firebase version
                    // numbering
                    && !coordinates.getArtifactId().startsWith("firebase-jobdispatcher")) {
                versionToCoordinate.put(coordinates.getVersion(), coordinates);
            }
        }

        for (JavaLibrary library : getJavaLibraries(project)) {
            MavenCoordinates coordinates = library.getResolvedCoordinates();
            // Claims to be non-null but may not be after a failed gradle sync
            //noinspection ConstantConditions
            if (coordinates != null && (coordinates.getGroupId().equals(groupId)
                    || (coordinates.getGroupId().equals(groupId2)))
                    // The Android annotations library is decoupled from the rest and doesn't
                    // need to be matched to the other exact support library versions
                    && !coordinates.getArtifactId().equals("support-annotations")) {
                versionToCoordinate.put(coordinates.getVersion(), coordinates);
            }
        }

        Set<String> versions = versionToCoordinate.keySet();
        if (versions.size() > 1) {
            List<String> sortedVersions = new ArrayList<>(versions);
            sortedVersions.sort(Collections.reverseOrder());
            MavenCoordinates c1 = findFirst(versionToCoordinate.get(sortedVersions.get(0)));
            MavenCoordinates c2 = findFirst(versionToCoordinate.get(sortedVersions.get(1)));
            // Not using toString because in the IDE, these are model proxies which display garbage output
            String example1 = c1.getGroupId() + ":" + c1.getArtifactId() + ":" + c1.getVersion();
            String example2 = c2.getGroupId() + ":" + c2.getArtifactId() + ":" + c2.getVersion();
            String groupDesc = GMS_GROUP_ID.equals(groupId) ? "gms/firebase" : groupId;
            String message = "All " + groupDesc + " libraries must use the exact same "
                    + "version specification (mixing versions can lead to runtime crashes). "
                    + "Found versions " + Joiner.on(", ").join(sortedVersions) + ". "
                    + "Examples include `" + example1 + "` and `" + example2 + "`";

            // Create an improved error message for a confusing scenario where you use
            // data binding and end up with conflicting versions:
            // https://code.google.com/p/android/issues/detail?id=229664
            for (AndroidLibrary library : androidLibraries) {
                MavenCoordinates coordinates = library.getResolvedCoordinates();
                // Claims to be non-null but may not be after a failed gradle sync
                //noinspection ConstantConditions
                if (coordinates != null
                        && coordinates.getGroupId().equals("com.android.databinding")
                        && coordinates.getArtifactId().equals("library")) {
                    for (AndroidLibrary dep : library.getLibraryDependencies()) {
                        MavenCoordinates c = dep.getResolvedCoordinates();
                        // Claims to be non-null but may not be after a failed gradle sync
                        //noinspection ConstantConditions
                        if (c != null
                                && c.getGroupId().equals("com.android.support")
                                && c.getArtifactId().equals("support-v4") &&
                                !sortedVersions.get(0).equals(c.getVersion())) {
                            message += ". Note that this project is using data binding "
                                    + "(com.android.databinding:library:"
                                    + coordinates.getVersion()
                                    + ") which pulls in com.android.support:support-v4:"
                                    + c.getVersion() + ". You can try to work around this "
                                    + "by adding an explicit dependency on "
                                    + "com.android.support:support-v4:" + sortedVersions.get(0);
                            break;
                        }

                    }
                    break;
                }
            }

            if (cookie != null) {
                reportNonFatalCompatibilityIssue(context, cookie, message);
            } else {
                File projectDir = context.getProject().getDir();
                Location location1 = guessGradleLocation(context.getClient(), projectDir, example1);
                Location location2 = guessGradleLocation(context.getClient(), projectDir, example2);
                if (location1.getStart() != null) {
                    if (location2.getStart() != null) {
                        location1.setSecondary(location2);
                    }
                } else {
                    if (location2.getStart() == null) {
                        location1 = guessGradleLocation(context.getClient(), projectDir,
                                // Probably using version variable
                                c1.getGroupId() + ":" + c1.getArtifactId() + ":");
                        if (location1.getStart() == null) {
                            location1 = guessGradleLocation(context.getClient(), projectDir,
                                    // Probably using version variable
                                    c2.getGroupId() + ":" + c2.getArtifactId() + ":");
                        }
                    } else {
                        location1 = location2;
                    }
                }
                reportNonFatalCompatibilityIssue(context, location1, message);
            }
        }
    }

    private static MavenCoordinates findFirst(@NonNull Collection<MavenCoordinates> coordinates) {
        return Collections.min(coordinates, Comparator.comparing(Object::toString));
    }

    @NonNull
    public static Collection<AndroidLibrary> getAndroidLibraries(@NonNull Project project) {
        Dependencies compileDependencies = getCompileDependencies(project);
        if (compileDependencies == null) {
            return Collections.emptyList();
        }

        Set<AndroidLibrary> allLibraries = new HashSet<>();
        addIndirectAndroidLibraries(compileDependencies.getLibraries(), allLibraries);
        return allLibraries;
    }

    @NonNull
    public static Collection<JavaLibrary> getJavaLibraries(@NonNull Project project) {
        Dependencies compileDependencies = getCompileDependencies(project);
        if (compileDependencies == null) {
            return Collections.emptyList();
        }

        Set<JavaLibrary> allLibraries = new HashSet<>();
        addIndirectJavaLibraries(compileDependencies.getJavaLibraries(), allLibraries);
        return allLibraries;
    }

    private static void addIndirectAndroidLibraries(
            @NonNull Collection<? extends AndroidLibrary> libraries,
            @NonNull Set<AndroidLibrary> result) {
        for (AndroidLibrary library : libraries) {
            if (!result.contains(library)) {
                result.add(library);
                addIndirectAndroidLibraries(library.getLibraryDependencies(), result);
            }
        }
    }

    private static void addIndirectJavaLibraries(
            @NonNull Collection<? extends JavaLibrary> libraries,
            @NonNull Set<JavaLibrary> result) {
        for (JavaLibrary library : libraries) {
            if (!result.contains(library)) {
                result.add(library);
                addIndirectJavaLibraries(library.getDependencies(), result);
            }
        }
    }

    private Map<Project, BlacklistedDeps> blacklisted = new HashMap<>();
    @Override
    public void beforeCheckProject(@NonNull Context context) {
        Project project = context.getProject();
        blacklisted.put(project, new BlacklistedDeps(project));
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        Project project = context.getProject();
        if (project == context.getMainProject() &&
                // Full analysis? Don't tie check to any specific Gradle DSL element
                context.getScope().contains(Scope.ALL_RESOURCE_FILES)) {
            checkConsistentPlayServices(context, null);
            checkConsistentSupportLibraries(context, null);
            checkConsistentWearableLibraries(context, null);
        }

        // Check for blacklisted dependencies
        checkBlacklistedDependencies(context, project);
    }

    /**
     * Report any blacklisted dependencies that weren't found in the build.gradle
     * source file during processing (we don't have accurate position info at this point)
     */
    private void checkBlacklistedDependencies(@NonNull Context context, Project project) {
        BlacklistedDeps blacklistedDeps = blacklisted.get(project);
        List<List<Library>> dependencies = blacklistedDeps.getBlacklistedDependencies();
        if (!dependencies.isEmpty()) {
            for (List<Library> path : dependencies) {
                String message = getBlacklistedDependencyMessage(context, path);
                if (message == null) {
                    continue;
                }
                File projectDir = context.getProject().getDir();
                MavenCoordinates coordinates = path.get(0).getRequestedCoordinates();
                if (coordinates == null) {
                    coordinates = path.get(0).getResolvedCoordinates();
                }
                Location location = guessGradleLocation(context.getClient(), projectDir,
                        coordinates.getGroupId() + ":" + coordinates.getArtifactId());
                if (location.getStart() == null) {
                    location = guessGradleLocation(context.getClient(), projectDir,
                            coordinates.getArtifactId());
                }
                context.report(DUPLICATE_CLASSES, location, message);
            }
        }
        blacklisted.remove(project);
    }

    @Nullable
    private static String getBlacklistedDependencyMessage(
            @NonNull Context context, @NonNull List<Library> path) {
        if (context.getMainProject().getMinSdkVersion().getApiLevel() >= 23
                && !usesLegacyHttpLibrary(context.getMainProject())) {
            return null;
        }

        boolean direct = path.size() == 1;
        String message;
        String resolution = "Solutions include " +
                "finding newer versions or alternative libraries that don't have the " +
                "same problem (for example, for `httpclient` use `HttpUrlConnection` or " +
                "`okhttp` instead), or repackaging the library using something like " +
                "`jarjar`.";
        if (direct) {
            message = String.format("" +
                            "`%1$s` defines classes that conflict with classes now " +
                            "provided by Android. %2$s",
                    path.get(0).getResolvedCoordinates().getArtifactId(), resolution);
        } else {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Library library : path) {
                if (first) {
                    first = false;
                } else {
                    sb.append(" \u2192 "); // right arrow
                }
                MavenCoordinates coordinates = library.getResolvedCoordinates();
                sb.append(coordinates.getGroupId());
                sb.append(':');
                sb.append(coordinates.getArtifactId());
            }
            sb.append(") ");
            String chain = sb.toString();
            message = String.format("" +
                            "`%1$s` depends on a library (%2$s) which defines classes that " +
                            "conflict with classes now provided by Android. %3$s " +
                            "Dependency chain: %4$s",
                    path.get(0).getResolvedCoordinates().getArtifactId(),
                    path.get(path.size() - 1).getResolvedCoordinates().getArtifactId(),
                    resolution,
                    chain);
        }
        return message;
    }

    @Nullable
    private static GradleVersion getNewerVersion(@NonNull GradleVersion version1,
            int major, int minor, int micro) {
        if (!version1.isAtLeast(major, minor, micro)) {
            return new GradleVersion(major, minor, micro);
        }
        return null;
    }

    @Nullable
    private static GradleVersion getNewerVersion(@NonNull GradleVersion version1,
            @SuppressWarnings("SameParameterValue") int major,
            @SuppressWarnings("SameParameterValue") int minor) {
        if (!version1.isAtLeast(major, minor, 0)) {
            return new GradleVersion(major, minor);
        }
        return null;
    }

    @Nullable
    private static GradleVersion getNewerVersion(@NonNull GradleVersion version1,
            @NonNull GradleVersion version2) {
        if (version1.compareTo(version2) < 0) {
            return version2;
        }
        return null;
    }

    private void report(@NonNull Context context, @NonNull Object cookie, @NonNull Issue issue,
            @NonNull String message) {
        report(context, cookie, issue, message, null);
    }

    private void report(@NonNull Context context, @NonNull Object cookie, @NonNull Issue issue,
            @NonNull String message, @Nullable LintFix fix) {
        if (context.isEnabled(issue)) {
            // Suppressed?
            // Temporarily unconditionally checking for suppress comments in Gradle files
            // since Studio insists on an AndroidLint id prefix
            boolean checkComments = /*context.getClient().checkForSuppressComments()
                    &&*/ context.containsCommentSuppress();
            if (checkComments) {
                int startOffset = getStartOffset(context, cookie);
                if (startOffset >= 0 && context.isSuppressedWithComment(startOffset, issue)) {
                    return;
                }
            }

            context.report(issue, createLocation(context, cookie), message, fix);
        }
    }

    /**
     * Normally, all warnings reported for a given issue will have the same severity, so
     * it isn't possible to have some of them reported as errors and others as warnings.
     * And this is intentional, since users should get to designate whether an issue is
     * an error or a warning (or ignored for that matter).
     * <p>
     * However, for {@link #COMPATIBILITY} we want to treat some issues as fatal (breaking
     * the build) but not others. To achieve this we tweak things a little bit.
     * All compatibility issues are now marked as fatal, and if we're *not* in the
     * "fatal only" mode, all issues are reported as before (with severity fatal, which has
     * the same visual appearance in the IDE as the previous severity, "error".)
     * However, if we're in a "fatal-only" build, then we'll stop reporting the issues
     * that aren't meant to be treated as fatal. That's what this method does; issues
     * reported to it should always be reported as fatal. There is a corresponding method,
     * {@link #reportNonFatalCompatibilityIssue(Context, Object, String)} which can be used
     * to report errors that shouldn't break the build; those are ignored in fatal-only
     * mode.
     */
    private void reportFatalCompatibilityIssue(
            @NonNull Context context,
            @NonNull Object cookie,
            @NonNull String message) {
        report(context, cookie, COMPATIBILITY, message);
    }

    private void reportFatalCompatibilityIssue(
            @NonNull Context context,
            @NonNull Object cookie,
            @NonNull String message,
            @Nullable LintFix fix) {
        report(context, cookie, COMPATIBILITY, message, fix);
    }

    private static void reportFatalCompatibilityIssue(
            @NonNull Context context,
            @NonNull Location location,
            @NonNull String message) {
        context.report(COMPATIBILITY, location, message);
    }

    /** See {@link #reportFatalCompatibilityIssue(Context, Object, String)} for an explanation. */
    private void reportNonFatalCompatibilityIssue(
            @NonNull Context context,
            @NonNull Object cookie,
            @NonNull String message) {
        reportNonFatalCompatibilityIssue(context, cookie, message, null);
    }

    /** See {@link #reportFatalCompatibilityIssue(Context, Object, String)} for an explanation. */
    private void reportNonFatalCompatibilityIssue(
            @NonNull Context context,
            @NonNull Object cookie,
            @NonNull String message,
            @Nullable LintFix lintFix) {
        if (context.getDriver().isFatalOnlyMode()) {
            return;
        }

        report(context, cookie, COMPATIBILITY, message, lintFix);
    }

    /** See {@link #reportFatalCompatibilityIssue(Context, Object, String)} for an explanation. */
    private static void reportNonFatalCompatibilityIssue(
            @NonNull Context context,
            @NonNull Location location,
            @NonNull String message) {
        if (context.getDriver().isFatalOnlyMode()) {
            return;
        }

        context.report(COMPATIBILITY, location, message);
    }

    @SuppressWarnings("MethodMayBeStatic")
    @NonNull
    protected Object getPropertyKeyCookie(@NonNull Object cookie) {
        return cookie;
    }

    @SuppressWarnings({"MethodMayBeStatic", "UnusedDeclaration"})
    @NonNull
    protected Object getPropertyPairCookie(@NonNull Object cookie) {
      return cookie;
    }

    @SuppressWarnings({"MethodMayBeStatic", "UnusedParameters"})
    protected int getStartOffset(@NonNull Context context, @NonNull Object cookie) {
        return -1;
    }

    @SuppressWarnings({"MethodMayBeStatic", "UnusedParameters"})
    protected Location createLocation(@NonNull Context context, @NonNull Object cookie) {
        return null;
    }

    @Nullable
    public static Dependencies getCompileDependencies(@NonNull Project project) {
        if (!project.isGradleProject()) {
            return null;
        }
        Variant variant = project.getCurrentVariant();
        if (variant == null) {
            return null;
        }

        AndroidArtifact artifact = variant.getMainArtifact();
        return artifact.getDependencies();
    }
}
