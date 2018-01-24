/*
 * Copyright (C) 2012 The Android Open Source Project
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

import static com.android.SdkConstants.CLASS_ACTIVITY;
import static com.android.SdkConstants.CLASS_APPLICATION;
import static com.android.SdkConstants.CLASS_BROADCASTRECEIVER;
import static com.android.SdkConstants.CLASS_CONTENTPROVIDER;
import static com.android.SdkConstants.CLASS_SERVICE;
import static com.android.SdkConstants.TAG_ACTIVITY;
import static com.android.SdkConstants.TAG_APPLICATION;
import static com.android.SdkConstants.TAG_PROVIDER;
import static com.android.SdkConstants.TAG_RECEIVER;
import static com.android.SdkConstants.TAG_SERVICE;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.ProductFlavorContainer;
import com.android.builder.model.SourceProviderContainer;
import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector.UastScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.utils.SdkUtils;
import com.android.utils.XmlUtils;
import com.google.common.collect.Maps;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jetbrains.uast.UClass;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Checks for missing manifest registrations for activities, services etc
 * and also makes sure that they are registered with the correct tag
 */
public class RegistrationDetector extends LayoutDetector implements UastScanner {
    /** Unregistered activities and services */
    public static final Issue ISSUE = Issue.create(
            "Registered",
            "Class is not registered in the manifest",

            "Activities, services and content providers should be registered in the " +
            "`AndroidManifest.xml` file using `<activity>`, `<service>` and `<provider>` tags.\n" +
            "\n" +
            "If your activity is simply a parent class intended to be subclassed by other " +
            "\"real\" activities, make it an abstract class.",

            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            new Implementation(
                    RegistrationDetector.class,
                    Scope.JAVA_FILE_SCOPE))
            .addMoreInfo(
            "http://developer.android.com/guide/topics/manifest/manifest-intro.html")
            // Temporary workaround for https://code.google.com/p/android/issues/detail?id=227579
            // The real solution is to have a merged manifest, which is coming
            .setEnabledByDefault(false);

    protected Map<String, String> mManifestRegistrations;

    /** Constructs a new {@link RegistrationDetector} */
    public RegistrationDetector() {
    }

    @Nullable
    private Map<String, String> getManifestRegistrations(@NonNull Project mainProject) {
        if (mManifestRegistrations == null) {
            Document mergedManifest = mainProject.getMergedManifest();
            if (mergedManifest == null ||
                    mergedManifest.getDocumentElement() == null) {
                return null;
            }
            mManifestRegistrations = Maps.newHashMap();
            Element application = XmlUtils.getFirstSubTagByName(
                    mergedManifest.getDocumentElement(), TAG_APPLICATION);
            if (application != null) {
                registerElement(application);
                for (Element c : XmlUtils.getSubTags(application)) {
                    registerElement(c);
                }
            }
        }

        return mManifestRegistrations;
    }

    private void registerElement(Element c) {
        String fqcn = LintUtils.resolveManifestName(c);
        String tag = c.getTagName();
        String frameworkClass = tagToClass(tag);
        if (frameworkClass != null) {
            mManifestRegistrations.put(fqcn, frameworkClass);
            if (fqcn.indexOf('$') != -1) {
                // The internal name contains a $ which means it's an inner class.
                // The conversion from fqcn to internal name is a bit ambiguous:
                // "a.b.C.D" usually means "inner class D in class C in package a.b".
                // However, it can (see issue 31592) also mean class D in package "a.b.C".
                // Place *both* of these possibilities in the registered map, since this
                // is only used to check that an activity is registered, not the other way
                // (so it's okay to have entries there that do not correspond to real classes).
                fqcn = fqcn.replace('$', '.');
                mManifestRegistrations.put(fqcn, frameworkClass);
            }
        }
    }

    // ---- Implements UastScanner ----

    @Nullable
    @Override
    public List<String> applicableSuperClasses() {
        return Arrays.asList(
                // Common super class for Activity, ContentProvider, Service, Application
                // (as well as some other classes not registered in the manifest, such as
                // Fragment and VoiceInteractionSession)
                "android.content.ComponentCallbacks2",
                CLASS_BROADCASTRECEIVER);
    }

    @Override
    public void visitClass(@NonNull JavaContext context, @NonNull UClass cls) {
        // If a library project provides additional activities, it is not an error to
        // not register all of those here
        if (context.getProject().isLibrary()) {
            return;
        }

        if (cls.getName() == null) {
            // anonymous class; can't be registered
            return;
        }

        JavaEvaluator evaluator = context.getEvaluator();
        if (evaluator.isAbstract(cls) || evaluator.isPrivate(cls)) {
            // Abstract classes do not need to be registered, and
            // private classes are clearly not intended to be registered
            return;
        }

        String rightTag = getTag(evaluator, cls);
        if (rightTag == null) {
            // some non-registered Context, such as a BackupAgent
            return;
        }
        String className = cls.getQualifiedName();
        if (className == null) {
            return;
        }
        Map<String, String> manifestRegistrations = getManifestRegistrations(
                context.getMainProject());
        if (manifestRegistrations != null) {
            String framework = manifestRegistrations.get(className);
            if (framework == null) {
                reportMissing(context, cls, className, rightTag);
            } else if (!evaluator.extendsClass(cls, framework, false)) {
                reportWrongTag(context, cls, rightTag, className, framework);
            }
        }
    }

    private static void reportWrongTag(
            @NonNull JavaContext context,
            @NonNull UClass node,
            @NonNull String rightTag,
            @NonNull String className,
            @NonNull String framework) {
        String wrongTag = classToTag(framework);
        if (wrongTag == null) {
            return;
        }
        Location location = context.getNameLocation(node);
        String message = String.format("`%1$s` is %2$s but is registered "
                        + "in the manifest as %3$s", className, describeTag(rightTag),
                describeTag(wrongTag));
        context.report(ISSUE, node, location, message);
    }

    private static String describeTag(@NonNull String tag) {
        String article = tag.startsWith("a") ? "an" : "a"; // an for activity and application
        return String.format("%1$s `<%2$s>`", article, tag);
    }

    private static void reportMissing(
            @NonNull JavaContext context,
            @NonNull UClass node,
            @NonNull String className,
            @NonNull String tag) {
        if (tag.equals(TAG_RECEIVER)) {
            // Receivers can be registered in code; don't flag these.
            return;
        }

        // Don't flag activities registered in test source sets
        if (context.getProject().isGradleProject()) {
            AndroidProject model = context.getProject().getGradleProjectModel();
            if (model != null) {
                String javaSource = context.file.getPath();
                // Test source set?

                for (SourceProviderContainer extra : model.getDefaultConfig().getExtraSourceProviders()) {
                    String artifactName = extra.getArtifactName();
                    if (AndroidProject.ARTIFACT_ANDROID_TEST.equals(artifactName)) {
                        for (File file : extra.getSourceProvider().getJavaDirectories()) {
                            if (SdkUtils.startsWithIgnoreCase(javaSource, file.getPath())) {
                                return;
                            }
                        }
                    }
                }

                for (ProductFlavorContainer container : model.getProductFlavors()) {
                    for (SourceProviderContainer extra : container.getExtraSourceProviders()) {
                        String artifactName = extra.getArtifactName();
                        if (AndroidProject.ARTIFACT_ANDROID_TEST.equals(artifactName)) {
                            for (File file : extra.getSourceProvider().getJavaDirectories()) {
                                if (SdkUtils.startsWithIgnoreCase(javaSource, file.getPath())) {
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }

        Location location = context.getNameLocation(node);
        String message = String.format("The `<%1$s> %2$s` is not registered in the manifest",
                tag, className);
        context.report(ISSUE, node, location, message);
    }

    private static String getTag(@NonNull JavaEvaluator evaluator, @NonNull UClass cls) {
        String tag = null;
        for (String s : sClasses) {
            if (evaluator.extendsClass(cls, s, false)) {
                tag = classToTag(s);
                break;
            }
        }
        return tag;
    }

    /** The manifest tags we care about */
    private static final String[] sTags = new String[] {
        TAG_ACTIVITY,
        TAG_SERVICE,
        TAG_RECEIVER,
        TAG_PROVIDER,
        TAG_APPLICATION
        // Keep synchronized with {@link #sClasses}
    };

    /** The corresponding framework classes that the tags in {@link #sTags} should extend */
    private static final String[] sClasses = new String[] {
            CLASS_ACTIVITY,
            CLASS_SERVICE,
            CLASS_BROADCASTRECEIVER,
            CLASS_CONTENTPROVIDER,
            CLASS_APPLICATION
            // Keep synchronized with {@link #sTags}
    };

    /** Looks up the corresponding framework class a given manifest tag's class should extend */
    private static String tagToClass(String tag) {
        for (int i = 0, n = sTags.length; i < n; i++) {
            if (sTags[i].equals(tag)) {
                return sClasses[i];
            }
        }

        return null;
    }

    /** Looks up the tag a given framework class should be registered with */
    protected static String classToTag(String className) {
        for (int i = 0, n = sClasses.length; i < n; i++) {
            if (sClasses[i].equals(className)) {
                return sTags[i];
            }
        }

        return null;
    }
}
