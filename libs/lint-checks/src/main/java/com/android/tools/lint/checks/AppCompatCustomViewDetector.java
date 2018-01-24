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

import static com.android.SdkConstants.APPCOMPAT_LIB_ARTIFACT;
import static com.android.SdkConstants.CLASS_VIEW;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.UastScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiReferenceList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.uast.UClass;

/**
 * Looks for subclasses of custom widgets in projects using app compat
 */
public class AppCompatCustomViewDetector extends Detector implements UastScanner {

    /** Copy/pasted item decorator code */
    public static final Issue ISSUE = Issue.create(
            "AppCompatCustomView",
            "Appcompat Custom Widgets",

            "In order to support features such as tinting, the appcompat library will "
                    + "automatically load special appcompat replacements for the builtin "
                    + "widgets. However, this does not work for your own custom views.\n"
                    + "\n"
                    + "Instead of extending the `android.widget` classes directly, you should "
                    + "instead extend one of the delegate classes in "
                    + "`android.support.v7.widget.AppCompat`.",

            Category.CORRECTNESS,
            4,
            Severity.ERROR,
            new Implementation(
                    AppCompatCustomViewDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    /** Constructs a new {@link AppCompatCustomViewDetector} */
    public AppCompatCustomViewDetector() {
    }

    // ---- Implements UastScanner ----

    @Nullable
    @Override
    public List<String> applicableSuperClasses() {
        return Collections.singletonList(CLASS_VIEW);
    }

    @Override
    public void visitClass(@NonNull JavaContext context, @NonNull UClass declaration) {
        Project project = context.getMainProject();
        if (project.dependsOn(APPCOMPAT_LIB_ARTIFACT) != Boolean.TRUE) {
            return;
        }

        PsiClass superClass = declaration.getSuperClass();
        if (!hasAppCompatDelegate(context, superClass)) {
            return;
        }

        PsiElement locationNode = declaration;
        PsiReferenceList extendsList = declaration.getExtendsList();
        if (extendsList != null) {
            PsiJavaCodeReferenceElement[] elements = extendsList.getReferenceElements();
            if (elements.length > 0) {
                locationNode = elements[0];
            }
        }
        Location location = context.getNameLocation(locationNode);
        String suggested = getAppCompatDelegate(superClass);
        String message = String.format("This custom view should extend `%1$s` instead", suggested);
        LintFix fix = fix().name("Extend AppCompat widget instead").replace().all()
                .with(suggested).build();
        context.report(ISSUE, declaration, location, message, fix);
    }

    @NonNull
    private static String getAppCompatDelegate(PsiClass superClass) {
        return "android.support.v7.widget.AppCompat" + superClass.getName();
    }

    private static boolean hasAppCompatDelegate(@NonNull JavaContext context,
            @Nullable PsiClass superClass) {
        if (superClass == null) {
            return false;
        }

        String qualifiedName = superClass.getQualifiedName();
        if (qualifiedName == null || !qualifiedName.startsWith("android.widget.")) {
            return false;
        }

        // Set of android.widget widgets that have appcompat replacements
        switch (qualifiedName) {
            case SdkConstants.FQCN_AUTO_COMPLETE_TEXT_VIEW:
            case SdkConstants.FQCN_BUTTON:
            case SdkConstants.FQCN_CHECK_BOX:
            case SdkConstants.FQCN_CHECKED_TEXT_VIEW:
            case SdkConstants.FQCN_EDIT_TEXT:
            case SdkConstants.FQCN_IMAGE_BUTTON:
            case SdkConstants.FQCN_IMAGE_VIEW:
            case SdkConstants.FQCN_MULTI_AUTO_COMPLETE_TEXT_VIEW:
            case SdkConstants.FQCN_RADIO_BUTTON:
            case SdkConstants.FQCN_RATING_BAR:
            case SdkConstants.FQCN_SEEK_BAR:
            case SdkConstants.FQCN_SPINNER:
            case SdkConstants.FQCN_TEXT_VIEW:
                return true;
        }

        // Extending some other android.widget. Instead of hardcoding "no", look for
        // the expected app compat class in the current compilation context.
        return context.getEvaluator().findClass(getAppCompatDelegate(superClass)) != null;
    }
}