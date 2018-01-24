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

package com.android.tools.lint;

import static com.android.SdkConstants.FN_ANNOTATIONS_ZIP;

import com.android.annotations.NonNull;
import com.android.builder.model.AndroidLibrary;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.Dependencies;
import com.android.builder.model.Variant;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.detector.api.Project;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.codeInsight.BaseExternalAnnotationsManager;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiModificationTrackerImpl;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LintExternalAnnotationsManager extends BaseExternalAnnotationsManager {

    private final List<VirtualFile> roots = Lists.newArrayList();

    public LintExternalAnnotationsManager(@NonNull final com.intellij.openapi.project.Project project, @NonNull PsiManager psiManager) {
        super(psiManager);
    }

    @Override
    protected boolean hasAnyAnnotationsRoots() {
        return !roots.isEmpty();
    }

    @NonNull
    @Override
    protected List<VirtualFile> getExternalAnnotationsRoots(@NonNull VirtualFile virtualFile) {
        return roots;
    }

    public void updateAnnotationRoots(@NonNull LintClient client) {
        Collection<Project> projects = client.getKnownProjects();
        if (Project.isAospBuildEnvironment()) {
            for (Project project : projects) {
                // If we are dealing with the AOSP frameworks project, we explicitly
                // set the external annotations manager to be a no-op.
                if (Project.isAospFrameworksProject(project.getDir())) {
                    return;
                }
            }
        }
        HashSet<AndroidLibrary> seen = Sets.newHashSet();
        List<File> files = Lists.newArrayListWithExpectedSize(2);
        for (Project project : projects) {
            if (project.isGradleProject()) {
                Variant variant = project.getCurrentVariant();
                AndroidProject model = project.getGradleProjectModel();
                if (model != null && variant != null) {
                    Dependencies dependencies = variant.getMainArtifact().getDependencies();
                    for (AndroidLibrary library : dependencies.getLibraries()) {
                        addLibraries(files, library, seen);
                    }
                }
            }
        }

        File sdkAnnotations = client.findResource(ExternalAnnotationRepository.SDK_ANNOTATIONS_PATH);
        if (sdkAnnotations == null) {
            // Until the SDK annotations are bundled in platform tools, provide
            // a fallback for Gradle builds to point to a locally installed version
            String path = System.getenv("SDK_ANNOTATIONS");
            if (path != null) {
                sdkAnnotations = new File(path);
                if (!sdkAnnotations.exists()) {
                    sdkAnnotations = null;
                }
            }
        }
        if (sdkAnnotations != null) {
            files.add(sdkAnnotations);
        }

        List<VirtualFile> newRoots = Lists.newArrayListWithCapacity(files.size());

        VirtualFileSystem local = StandardFileSystems.local();
        VirtualFileSystem jar = StandardFileSystems.jar();

        for (File file : files) {
            VirtualFile virtualFile;
            boolean isZip = file.getName().equals(FN_ANNOTATIONS_ZIP);
            if (isZip) {
                virtualFile = jar.findFileByPath(file.getPath() + "!/");
            } else {
                virtualFile = local.findFileByPath(file.getPath());
            }
            if (virtualFile == null) {
                if (isZip) {
                    virtualFile = jar.findFileByPath(file.getAbsolutePath() + "!/");
                } else {
                    virtualFile = local.findFileByPath(file.getAbsolutePath());
                }
                if (virtualFile == null) {
                    System.out.println("WTF?");
                }
            }
            if (virtualFile != null) {
                newRoots.add(virtualFile);
            }
        }

        // We don't need to do equals; we don't worry about having annotations
        // for removed projects, but make sure all the new projects are covered
        //if (this.roots.equals(roots)) {
        if (roots.containsAll(newRoots)) {
            return;
        }

        roots.addAll(newRoots);
        dropCache(); // TODO: Find out if I need to drop cache for pure additions
        ((PsiModificationTrackerImpl)myPsiManager.getModificationTracker()).incCounter();
    }

    private static void addLibraries(
            @NonNull List<File> result,
            @NonNull AndroidLibrary library,
            Set<AndroidLibrary> seen) {
        if (seen.contains(library)) {
            return;
        }
        seen.add(library);

        // As of 1.2 this is available in the model:
        //  https://android-review.googlesource.com/#/c/137750/
        // Switch over to this when it's in more common usage
        // (until it is, we'll pay for failed proxying errors)
        try {
            File zip = library.getExternalAnnotations();
            if (zip.exists()) {
                result.add(zip);
            }
        } catch (Throwable ignore) {
            // Using some older version than 1.2
            File zip = new File(library.getResFolder().getParent(), FN_ANNOTATIONS_ZIP);
            if (zip.exists()) {
                result.add(zip);
            }
        }

        for (AndroidLibrary dependency : library.getLibraryDependencies()) {
            addLibraries(result, dependency, seen);
        }
    }
}
