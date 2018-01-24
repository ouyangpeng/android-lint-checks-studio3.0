/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.tools.lint.helpers;

import static com.android.SdkConstants.CONSTRUCTOR_NAME;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.model.Dependencies;
import com.android.builder.model.Variant;
import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Project;
import com.google.common.collect.Sets;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCompiledFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.file.PsiPackageImpl;
import com.intellij.psi.impl.source.tree.java.PsiCompositeModifierList;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.MethodSignatureUtil;
import com.intellij.psi.util.TypeConversionUtil;
import java.util.Collections;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UFile;
import org.jetbrains.uast.UastUtils;

public class DefaultJavaEvaluator extends JavaEvaluator {
    private final com.intellij.openapi.project.Project myProject;
    private final Project myLintProject;

    public DefaultJavaEvaluator(com.intellij.openapi.project.Project project,
            Project lintProject) {
        myProject = project;
        myLintProject = lintProject;
    }

    @Nullable
    @Override
    public Dependencies getDependencies() {
        if (myLintProject.isAndroidProject()) {
            Variant variant = myLintProject.getCurrentVariant();
            if (variant != null) {
                return variant.getMainArtifact().getDependencies();
            }
        }
        return null;
    }

    @Override
    public boolean extendsClass(@Nullable PsiClass cls, @NonNull String className, boolean strict) {
        // TODO: This checks interfaces too. Let's find a cheaper method which only checks direct super classes!
        return InheritanceUtil.isInheritor(cls, strict, className);
    }

    @Override
    public boolean implementsInterface(@NonNull PsiClass cls, @NonNull String interfaceName, boolean strict) {
        // TODO: This checks superclasses too. Let's find a cheaper method which only checks interfaces.
        return InheritanceUtil.isInheritor(cls, strict, interfaceName);
    }

    @Override
    public boolean inheritsFrom(@NonNull PsiClass cls, @NonNull String className, boolean strict) {
        return InheritanceUtil.isInheritor(cls, strict, className);
    }

    @Nullable
    @Override
    public PsiClass findClass(@NonNull String qualifiedName) {
        return JavaPsiFacade.getInstance(myProject).findClass(qualifiedName, GlobalSearchScope.allScope(myProject));
    }

    @Nullable
    @Override
    public PsiClassType getClassType(@Nullable PsiClass cls) {
        return cls != null ? JavaPsiFacade.getElementFactory(myProject).createType(cls) : null;
    }

    @NonNull
    @Override
    public PsiAnnotation[] getAllAnnotations(@NonNull PsiModifierListOwner owner, boolean inHierarchy) {
        // withInferred=false when running outside the IDE: we don't have
        // an InferredAnnotationsManager
        return AnnotationUtil.getAllAnnotations(owner, inHierarchy, null, false);
    }

    @Nullable
    @Override
    public PsiAnnotation findAnnotationInHierarchy(@NonNull PsiModifierListOwner listOwner, @NonNull String... annotationNames) {
        return AnnotationUtil.findAnnotationInHierarchy(listOwner, Sets.newHashSet(annotationNames));
    }

    @Nullable
    @Override
    public PsiAnnotation findAnnotation(@Nullable PsiModifierListOwner listOwner, @NonNull String... annotationNames) {
        return AnnotationUtil.findAnnotation(listOwner, false, annotationNames);
    }

    @Override
    public boolean areSignaturesEqual(@NonNull PsiMethod method1, @NonNull PsiMethod method2) {
        return MethodSignatureUtil.areSignaturesEqual(method1, method2);
    }

    @Nullable
    @Override
    public String findJarPath(@NonNull PsiElement element) {
        PsiFile containingFile = element.getContainingFile();
        return findJarPath(containingFile);
    }

    @Nullable
    @Override
    public String findJarPath(@NonNull UElement element) {
        UFile uFile = UastUtils.getContainingFile(element);
        return uFile != null ? findJarPath(uFile.getPsi()) : null;
    }

    private static String findJarPath(@Nullable PsiFile containingFile) {
        if (containingFile instanceof PsiCompiledFile) {
            ///This code is roughly similar to the following:
            //      VirtualFile jarVirtualFile = PsiUtil.getJarFile(containingFile);
            //      if (jarVirtualFile != null) {
            //        return jarVirtualFile.getPath();
            //      }
            // However, the above methods will do some extra string manipulation and
            // VirtualFile lookup which we don't actually need (we're just after the
            // raw URL suffix)
            VirtualFile file = containingFile.getVirtualFile();
            if (file != null && file.getFileSystem().getProtocol().equals("jar")) {
                String path = file.getPath();
                final int separatorIndex = path.indexOf("!/");
                if (separatorIndex >= 0) {
                    return path.substring(0, separatorIndex);
                }
            }
        }

        return null;
    }

    @Nullable
    @Override
    public PsiPackage getPackage(@NonNull PsiElement node) {
        PsiFile containingFile = node instanceof PsiFile ? (PsiFile) node : node.getContainingFile();
        if (containingFile != null) {
            // Optimization: JavaDirectoryService can be slow so try to compute it directly
            if (containingFile instanceof PsiJavaFile) {
                String packageName = ((PsiJavaFile) containingFile).getPackageName();
                return new PsiPackageImpl(node.getManager(), packageName) {
                    @Nullable
                    @Override
                    public PsiModifierList getAnnotationList() {
                        PsiClass cls = findClass(packageName + '.' + PACKAGE_INFO_CLASS);
                        if (cls != null) {
                            PsiModifierList modifierList = cls.getModifierList();
                            if (modifierList != null) {
                                // Use composite even if we just have one such that we don't
                                // pass a modifier list tied to source elements in the class
                                // (modifier lists can be part of the AST)
                                return new PsiCompositeModifierList(getManager(),
                                        Collections.singletonList(modifierList));
                            }
                            return modifierList;
                        }
                        return null;
                    }
                };
            }

            PsiDirectory dir = containingFile.getParent();
            if (dir != null) {
                return JavaDirectoryService.getInstance().getPackage(dir);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public PsiPackage getPackage(@NonNull UElement node) {
        UFile uFile = UastUtils.getContainingFile(node);
        if (uFile != null) {
            return getPackage(uFile.getPsi());
        }
        return null;
    }

    @Nullable
    @Override
    public String getInternalName(@NonNull PsiClassType psiClassType) {
        PsiType erased = TypeConversionUtil.erasure(psiClassType);
        if (erased instanceof PsiClassType) {
            return super.getInternalName((PsiClassType) erased);
        }

        return super.getInternalName(psiClassType);
    }

    /**
     * Computes the internal class name of the given class.
     * For example, for PsiClass foo.bar.Foo.Bar it returns foo/bar/Foo$Bar.
     *
     * @param psiClass the class to look up the internal name for
     * @return the internal class name
     * @see ClassContext#getInternalName(String)
     */
    @Override
    @Nullable
    public String getInternalName(@NonNull PsiClass psiClass) {
        return LintUtils.getInternalName(psiClass);
    }

    /**
     * Computes the internal JVM description of the given method. This is in the same
     * format as the ASM desc fields for methods; meaning that a method named foo which for example takes an
     * int and a String and returns a void will have description {@code foo(ILjava/lang/String;):V}.
     *
     * @param method the method to look up the description for
     * @param includeName whether the name should be included
     * @param includeReturn whether the return type should be included
     * @return the internal JVM description for this method
     */
    @Override
    @Nullable
    public String getInternalDescription(@NonNull PsiMethod method, boolean includeName,
            boolean includeReturn) {
        assert !includeName; // not yet tested
        assert !includeReturn; // not yet tested

        StringBuilder signature = new StringBuilder();

        if (includeName) {
            if (method.isConstructor()) {
                final PsiClass declaringClass = method.getContainingClass();
                if (declaringClass != null) {
                    final PsiClass outerClass = declaringClass.getContainingClass();
                    if (outerClass != null) {
                        // declaring class is an inner class
                        if (!declaringClass.hasModifierProperty(PsiModifier.STATIC)) {
                            if (!appendJvmTypeName(signature, outerClass)) {
                                return null;
                            }
                        }
                    }
                }
                signature.append(CONSTRUCTOR_NAME);
            } else {
                signature.append(method.getName());
            }
        }

        signature.append('(');

        for (PsiParameter psiParameter : method.getParameterList().getParameters()) {
            if (!appendJvmSignature(signature, psiParameter.getType())) {
                return null;
            }
        }
        signature.append(')');
        if (includeReturn) {
            if (!method.isConstructor()) {
                if (!appendJvmSignature(signature, method.getReturnType())) {
                    return null;
                }
            }
            else {
                signature.append('V');
            }
        }
        return signature.toString();
    }

    private boolean appendJvmTypeName(@NonNull StringBuilder signature, @NonNull PsiClass outerClass) {
        String className = getInternalName(outerClass);
        if (className == null) {
            return false;
        }
        signature.append('L').append(className.replace('.', '/')).append(';');
        return true;
    }

    private boolean appendJvmSignature(@NonNull StringBuilder buffer, @Nullable PsiType type) {
        if (type == null) {
            return false;
        }
        final PsiType psiType = TypeConversionUtil.erasure(type);
        if (psiType instanceof PsiArrayType) {
            buffer.append('[');
            appendJvmSignature(buffer, ((PsiArrayType)psiType).getComponentType());
        }
        else if (psiType instanceof PsiClassType) {
            PsiClass resolved = ((PsiClassType)psiType).resolve();
            if (resolved == null) {
                return false;
            }
            if (!appendJvmTypeName(buffer, resolved)) {
                return false;
            }
        }
        else if (psiType instanceof PsiPrimitiveType) {
            buffer.append(getPrimitiveSignature(psiType.getCanonicalText()));
        }
        else {
            return false;
        }
        return true;
    }
}
