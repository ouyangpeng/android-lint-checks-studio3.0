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

import com.intellij.codeInsight.ContainerProvider;
import com.intellij.codeInsight.CustomExceptionHandler;
import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.codeInsight.InferredAnnotationsManager;
import com.intellij.codeInsight.runner.JavaMainMethodProvider;
import com.intellij.core.CoreApplicationEnvironment;
import com.intellij.core.CoreJavaFileManager;
import com.intellij.core.JavaCoreApplicationEnvironment;
import com.intellij.core.JavaCoreProjectEnvironment;
import com.intellij.lang.MetaLanguage;
import com.intellij.lang.java.JavaParserDefinition;
import com.intellij.mock.MockProject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.application.TransactionGuardImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.fileTypes.FileTypeExtensionPoint;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.impl.ZipHandler;
import com.intellij.psi.FileContextProvider;
import com.intellij.psi.PsiElementFinder;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.augment.TypeAnnotationModifier;
import com.intellij.psi.compiled.ClassFileDecompilers;
import com.intellij.psi.impl.JavaClassSupersImpl;
import com.intellij.psi.impl.PsiElementFinderImpl;
import com.intellij.psi.impl.PsiTreeChangePreprocessor;
import com.intellij.psi.impl.compiled.ClsCustomNavigationPolicy;
import com.intellij.psi.impl.file.impl.JavaFileManager;
import com.intellij.psi.meta.MetaDataContributor;
import com.intellij.psi.stubs.BinaryFileStubBuilders;
import com.intellij.psi.util.JavaClassSupers;
import org.jetbrains.uast.UastContext;
import org.jetbrains.uast.UastLanguagePlugin;

public class LintCoreApplicationEnvironment extends JavaCoreApplicationEnvironment {
    private static final Object APPLICATION_LOCK = new Object();

    private static LintCoreApplicationEnvironment ourApplicationEnvironment;
    private static int ourProjectCount;

    public static LintCoreApplicationEnvironment get() {
        synchronized (APPLICATION_LOCK) {
            if (ourApplicationEnvironment != null) {
                return ourApplicationEnvironment;
            }

            Disposable parentDisposable = Disposer.newDisposable();
            ourApplicationEnvironment = createApplicationEnvironment(parentDisposable);
            ourProjectCount = 0;
            Disposer.register(parentDisposable, () -> {
                synchronized (APPLICATION_LOCK) {
                    ourApplicationEnvironment = null;
                }
            });

            return ourApplicationEnvironment;
        }
    }

    public LintCoreApplicationEnvironment(Disposable parentDisposable) {
        super(parentDisposable);
    }

    private static LintCoreApplicationEnvironment createApplicationEnvironment(
            Disposable parentDisposable) {
        // We don't bundle .dll files in the Gradle plugin for native file system access;
        // prevent warning logs on Windows when it's not found (see b.android.com/260180)
        System.setProperty("idea.use.native.fs.for.win", "false");

        Extensions.cleanRootArea(parentDisposable);
        registerAppExtensionPoints();
        LintCoreApplicationEnvironment applicationEnvironment =
                new LintCoreApplicationEnvironment(parentDisposable);

        registerApplicationServicesForCLI(applicationEnvironment);
        registerApplicationServices(applicationEnvironment);

        synchronized (APPLICATION_LOCK) {
            ourProjectCount++;
        }

        return applicationEnvironment;
    }

    public static void clearAccessorCache() {
        synchronized (APPLICATION_LOCK) {
            ZipHandler.clearFileAccessorCache();
        }
    }

    public static void disposeApplicationEnvironment() {
        synchronized (APPLICATION_LOCK) {
            LintCoreApplicationEnvironment environment = ourApplicationEnvironment;
            if (environment == null) {
                return;
            }
            ourApplicationEnvironment = null;
            Disposer.dispose(environment.getParentDisposable());
            ZipHandler.clearFileAccessorCache();
        }
    }

    private static void registerAppExtensionPoints() {
        ExtensionsArea rootArea = Extensions.getRootArea();
        CoreApplicationEnvironment.registerExtensionPoint(rootArea, BinaryFileStubBuilders.EP_NAME, FileTypeExtensionPoint.class);
        CoreApplicationEnvironment.registerExtensionPoint(rootArea, FileContextProvider.EP_NAME, FileContextProvider.class);
        CoreApplicationEnvironment.registerExtensionPoint(rootArea, MetaDataContributor.EP_NAME, MetaDataContributor.class);
        CoreApplicationEnvironment.registerExtensionPoint(rootArea, PsiAugmentProvider.EP_NAME, PsiAugmentProvider.class);
        CoreApplicationEnvironment.registerExtensionPoint(rootArea, JavaMainMethodProvider.EP_NAME, JavaMainMethodProvider.class);
        CoreApplicationEnvironment.registerExtensionPoint(rootArea, ContainerProvider.EP_NAME, ContainerProvider.class);
        CoreApplicationEnvironment.registerExtensionPoint(rootArea, ClsCustomNavigationPolicy.EP_NAME, ClsCustomNavigationPolicy.class);
        CoreApplicationEnvironment.registerExtensionPoint(rootArea, ClassFileDecompilers.EP_NAME, ClassFileDecompilers.Decompiler.class);
        CoreApplicationEnvironment.registerExtensionPoint(rootArea, TypeAnnotationModifier.EP_NAME, TypeAnnotationModifier.class);
        CoreApplicationEnvironment.registerExtensionPoint(rootArea, UastLanguagePlugin.Companion.getExtensionPointName(), UastLanguagePlugin.class);
        CoreApplicationEnvironment.registerExtensionPoint(rootArea, MetaLanguage.EP_NAME, MetaLanguage.class);
        CoreApplicationEnvironment.registerExtensionPoint(rootArea, CustomExceptionHandler.KEY, CustomExceptionHandler.class);

        rootArea.getExtensionPoint(UastLanguagePlugin.Companion.getExtensionPointName()).registerExtension(
                new org.jetbrains.uast.java.JavaUastLanguagePlugin());
    }

    private static boolean kotlinAttempted;

    /** Attempt to register the Kotlin plugin, assuming the Kotlin plugin has been enabled */
    public static void registerKotlinUastPlugin() {
        // Not yet working (needs additional fixes to the app and project environment instances;
        // disable for now)
        if (false && !kotlinAttempted) {
            kotlinAttempted = true;

            try {
                Class<?> clz = Class.forName("org.jetbrains.uast.kotlin.KotlinUastLanguagePlugin");
                UastLanguagePlugin plugin = (UastLanguagePlugin) clz.newInstance();
                ExtensionsArea rootArea = Extensions.getRootArea();

                CoreApplicationEnvironment.registerExtensionPoint(rootArea, UastLanguagePlugin.Companion.getExtensionPointName(), UastLanguagePlugin.class);

                rootArea.getExtensionPoint(UastLanguagePlugin.Companion.getExtensionPointName()).
                        registerExtension(plugin);
            } catch (Throwable ex) {
                System.err.println("Couldn't find or register Kotlin UAST plugin:" +
                        ex.getLocalizedMessage());
            }
        }
    }

    private static void registerApplicationServicesForCLI(JavaCoreApplicationEnvironment applicationEnvironment) {
        // ability to get text from annotations xml files
        applicationEnvironment.registerFileType(PlainTextFileType.INSTANCE, "xml");
        applicationEnvironment.registerParserDefinition(new JavaParserDefinition());
    }

    private static void registerApplicationServices(JavaCoreApplicationEnvironment applicationEnvironment) {
        applicationEnvironment.getApplication().registerService(JavaClassSupers.class, JavaClassSupersImpl.class);
        applicationEnvironment.getApplication().registerService(TransactionGuard.class, TransactionGuardImpl.class);
    }

    static void registerProjectExtensionPoints(ExtensionsArea area) {
        CoreApplicationEnvironment.registerExtensionPoint(area, PsiTreeChangePreprocessor.EP_NAME, PsiTreeChangePreprocessor.class);
        CoreApplicationEnvironment.registerExtensionPoint(area, PsiElementFinder.EP_NAME, PsiElementFinder.class);
        CoreApplicationEnvironment.registerExtensionPoint(area, "org.jetbrains.uast.uastLanguagePlugin", UastLanguagePlugin.class);
    }

    public static void registerProjectServices(JavaCoreProjectEnvironment projectEnvironment) {
        MockProject project = projectEnvironment.getProject();
        project.registerService(UastContext.class, new UastContext(project));
        project.registerService(ExternalAnnotationsManager.class, LintExternalAnnotationsManager.class);
        project.registerService(InferredAnnotationsManager.class, LintInferredAnnotationsManager.class);
    }

    static void registerProjectServicesForCLI(
            JavaCoreProjectEnvironment projectEnvironment) {
        MockProject project = projectEnvironment.getProject();
        project.registerService(CoreJavaFileManager.class,
                (CoreJavaFileManager) ServiceManager.getService(project, JavaFileManager.class));

        ExtensionsArea area = Extensions.getArea(project);
        area.getExtensionPoint(PsiElementFinder.EP_NAME).registerExtension(
                new PsiElementFinderImpl(
                        project, ServiceManager.getService(
                        project, JavaFileManager.class)));
    }
}
