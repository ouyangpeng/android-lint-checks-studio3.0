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

package com.android.tools.lint.checks;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.TAG_APPLICATION;
import static com.android.SdkConstants.VALUE_FALSE;
import static com.android.xml.AndroidManifest.ATTRIBUTE_EXTRACT_NATIVE_LIBS;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.repository.GradleVersion;
import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.UastScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;
import com.android.xml.AndroidManifest;
import com.intellij.psi.PsiMethod;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.jetbrains.uast.UCallExpression;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Checks for extractNativeLibs flag in <code>AndroidManifest.xml</code> when Native code is present.
 * <p/>
 * <b>NOTE: This is not a final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
public class UnpackedNativeCodeDetector extends ResourceXmlDetector implements Detector.XmlScanner,
        UastScanner, Detector.ClassScanner {

    public static final Issue ISSUE = Issue.create(
            "UnpackedNativeCode",

            "Missing `android:extractNativeLibs=false`",

            "This app loads native libraries using `System.loadLibrary()`.\n\n" +
                    "Consider adding `android:extractNativeLibs=\"false\"` " +
                    "to the `<application>` tag in AndroidManifest.xml. " +
                    "Starting with Android 6.0, this will make installation faster, " +
                    "the app will take up less space on the device " +
                    "and updates will have smaller download sizes.",
            Category.PERFORMANCE,
            6,
            Severity.WARNING,
            new Implementation(
                    UnpackedNativeCodeDetector.class,
                    EnumSet.of(Scope.MANIFEST, Scope.JAVA_FILE, Scope.JAVA_LIBRARIES),
                    EnumSet.of(Scope.MANIFEST, Scope.JAVA_FILE)
            )
    ).setEnabledByDefault(false); // b.android.com/232158

    private static final String SYSTEM_CLASS = "java.lang.System";
    private static final String RUNTIME_CLASS = "java.lang.Runtime";

    private static final String SYSTEM_CLASS_ALT = "java/lang/System";
    private static final String RUNTIME_CLASS_ALT = "java/lang/Runtime";

    private static final String LOAD_LIBRARY = "loadLibrary";

    /**
     * Android Gradle plugin 2.2.0+ supports uncompressed native libs in the APK
     */
    private static final GradleVersion MIN_GRADLE_VERSION = GradleVersion.parse("2.2.0");


    /**
     * This will be <code>true</code> if the project or dependencies use native libraries
     */
    private boolean mHasNativeLibs;

    /**
     * The <application> manifest tag location for the main project,
     */
    private Location.Handle mApplicationTagHandle;

    /**
     * If the issue should be suppressed.
     */
    private boolean mSuppress;


    /**
     * No-args constructor used by the lint framework to instantiate the detector.
     */
    public UnpackedNativeCodeDetector() {
    }

    // ---- Implements Detector.ClassDetector----

    @Nullable
    @Override
    public List<String> getApplicableCallNames() {
        if (mSuppress) {
            return null;
        }
        return Collections.singletonList(LOAD_LIBRARY);
    }

    @Override
    public void checkCall(@NonNull ClassContext context, @NonNull ClassNode classNode,
            @NonNull MethodNode method, @NonNull MethodInsnNode call) {
        String owner = call.owner;
        String name = call.name;

        if (name.equals(LOAD_LIBRARY)
                && (owner.equals(SYSTEM_CLASS_ALT)
                || owner.equals(RUNTIME_CLASS_ALT))) {
            mHasNativeLibs = true;
        }
    }

    // ---- Implements Detector.JavaPsiScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        // Identify calls to Runtime.loadLibrary() and System.loadLibrary()
        if (mSuppress) {
            return null;
        }
        return Collections.singletonList(LOAD_LIBRARY);
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @NonNull UCallExpression call,
            @NonNull PsiMethod method) {
        // Identify calls to Runtime.loadLibrary() and System.loadLibrary()
        if (LOAD_LIBRARY.equals(method.getName())) {
            JavaEvaluator evaluator = context.getEvaluator();
            if (evaluator.isMemberInSubClassOf(method, RUNTIME_CLASS, false) ||
                    evaluator.isMemberInSubClassOf(method, SYSTEM_CLASS, false)) {
                mHasNativeLibs = true;
            }
        }
    }

    // ---- Implements XmlScanner ----

    @Override
    public Collection<String> getApplicableElements() {
        return Collections.singleton(TAG_APPLICATION);
    }

    @Override
    public void beforeCheckProject(@NonNull Context context) {
        mHasNativeLibs = false;
        mApplicationTagHandle = null;

        if (!context.getMainProject().isGradleProject()
                || context.getMainProject().getGradleModelVersion() == null) {
            mSuppress = true;
            return;
        }

        //compileSdkVersion must be >= 23
        boolean projectSupportsAttribute = context.getMainProject().getBuildSdk() >= 23;

        //android gradle plugin must be 2.2.0+
        GradleVersion gradleVersion = context.getMainProject().getGradleModelVersion();
        boolean gradleSupportsAttribute =
                MIN_GRADLE_VERSION.compareIgnoringQualifiers(gradleVersion) <= 0;

        //suppress lint check if the compile SDK or the Gradle plugin are too old
        mSuppress = !projectSupportsAttribute || !gradleSupportsAttribute;
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        // Don't report issues on libraries
        if (context.getProject() == context.getMainProject()
                && !context.getMainProject().isLibrary()
                && mApplicationTagHandle != null) {
            if (!mSuppress && mHasNativeLibs) {
                LintFix fix = fix().set(ANDROID_URI, ATTRIBUTE_EXTRACT_NATIVE_LIBS, VALUE_FALSE)
                        .build();
                context.report(ISSUE, mApplicationTagHandle.resolve(),
                        "Missing attribute android:extractNativeLibs=\"false\"" +
                                " on the `<application>` tag.", fix);
            }
        }
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        // Don't check library manifests
        if (context.getProject() != context.getMainProject()
                || context.getMainProject().isLibrary()) {
            return;
        }

        if (context.getDriver().isSuppressed(context, ISSUE, element)) {
            mSuppress = true;
            return;
        }

        if (TAG_APPLICATION.equals(element.getNodeName())) {
            Node extractAttr = element.getAttributeNodeNS(ANDROID_URI,
                    AndroidManifest.ATTRIBUTE_EXTRACT_NATIVE_LIBS);
            //noinspection VariableNotUsedInsideIf
            if (extractAttr != null) {
                mSuppress = true;
            } else {
                mApplicationTagHandle = context.createLocationHandle(element);
            }
        }
    }
}
