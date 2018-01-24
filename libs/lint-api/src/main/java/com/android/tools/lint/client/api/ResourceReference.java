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

package com.android.tools.lint.client.api;


import static com.android.SdkConstants.ANDROID_PKG;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceType;
import com.google.common.base.Joiner;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import java.util.Collections;
import java.util.List;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.UResolvable;
import org.jetbrains.uast.USimpleNameReferenceExpression;
import org.jetbrains.uast.UVariable;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.java.JavaAbstractUExpression;
import org.jetbrains.uast.java.JavaUDeclarationsExpression;

/**
 * A reference to an Android resource in the AST; the reference may not be qualified.
 * For example, in the below, the {@code foo} reference on the right hand side of
 * the assignment can be resolved as an {@link ResourceReference}.
 * <pre>
 *     import my.pkg.R.string.foo;
 *     ...
 *         int id = foo;
 * </pre>
 */
public class ResourceReference {

    public final UExpression node;

    private final String rPackage;

    private final ResourceType type;

    private final String name;

    // getPackage() can be empty if not a package-qualified import (e.g. android.R.id.name).
    @NonNull
    public String getPackage() {
        return rPackage;
    }

    @NonNull
    public ResourceType getType() {
        return type;
    }

    @NonNull
    public String getName() {
        return name;
    }

    boolean isFramework() {
        return rPackage.equals(ANDROID_PKG);
    }

    public ResourceReference(
            UExpression node,
            String rPackage,
            ResourceType type,
            String name) {
        this.node = node;
        this.rPackage = rPackage;
        this.type = type;
        this.name = name;
    }

    @Nullable
    private static ResourceReference toAndroidReference(UQualifiedReferenceExpression expression) {
        List<String> path = UastUtils.asQualifiedPath(expression);

        String packageNameFromResolved = null;

        PsiClass containingClass = UastUtils.getContainingClass(expression.resolve());
        if (containingClass != null) {
            String containingClassFqName = containingClass.getQualifiedName();

            if (containingClassFqName != null) {
                int i = containingClassFqName.lastIndexOf(".R.");
                if (i >= 0) {
                    packageNameFromResolved = containingClassFqName.substring(0, i);
                }
            }
        }

        if (path == null) {
            return null;
        }

        int size = path.size();
        if (size < 3) {
            return null;
        }

        String r = path.get(size - 3);
        if (!r.equals(SdkConstants.R_CLASS)) {
            return null;
        }

        String packageName = packageNameFromResolved != null
                ? packageNameFromResolved
                : Joiner.on('.').join(path.subList(0, size - 3));

        String type = path.get(size - 2);
        String name = path.get(size - 1);

        ResourceType resourceType = null;
        for (ResourceType value : ResourceType.values()) {
            if (value.getName().equals(type)) {
                resourceType = value;
                break;
            }
        }

        if (resourceType == null) {
            return null;
        }

        return new ResourceReference(expression, packageName, resourceType, name);
    }


    @Nullable
    public static ResourceReference get(UElement element) {
        if (element instanceof UQualifiedReferenceExpression
                && element instanceof JavaAbstractUExpression) {
            ResourceReference ref = toAndroidReference((UQualifiedReferenceExpression) element);
            if (ref != null) {
                return ref;
            }
        }

        PsiElement declaration;
        if (element instanceof UVariable) {
            declaration = ((UVariable) element).getPsi();
        } else if (element instanceof UResolvable) {
            declaration = ((UResolvable) element).resolve();
        } else {
            return null;
        }

        if (declaration == null && element instanceof USimpleNameReferenceExpression
                && element instanceof JavaAbstractUExpression) {
            // R class can't be resolved in tests so we need to use heuristics to calc the reference
            UExpression maybeQualified = UastUtils.getQualifiedParentOrThis((UExpression) element);
            if (maybeQualified instanceof UQualifiedReferenceExpression) {
                ResourceReference ref = toAndroidReference(
                        (UQualifiedReferenceExpression) maybeQualified);
                if (ref != null) {
                    return ref;
                }
            }
        }

        if (!(declaration instanceof PsiVariable)) {
            return null;
        }

        PsiVariable variable = (PsiVariable) declaration;
        if (!(variable instanceof PsiField)
                || variable.getType() != PsiType.INT
                // Note that we don't check for PsiModifier.FINAL; in library projects
                // the R class fields are deliberately not made final such that their
                // values can be substituted when all the resources are merged together
                // in the app module and unique id's can be assigned for all resources
                || !variable.hasModifierProperty(PsiModifier.STATIC)) {
            return null;
        }

        PsiClass resTypeClass = ((PsiField) variable).getContainingClass();
        if (resTypeClass == null || !resTypeClass.hasModifierProperty(PsiModifier.STATIC)) {
            return null;
        }

        PsiClass rClass = resTypeClass.getContainingClass();
        if (rClass == null || rClass.getContainingClass() != null) {
            return null;
        } else {
            String className = rClass.getName();
            if (!("R".equals(className) || "R2".equals(className))) { // R2: butterknife library
                return null;
            }
        }

        String packageName = ((PsiJavaFile) rClass.getContainingFile()).getPackageName();
        if (packageName.isEmpty()) {
            return null;
        }

        ResourceType resourceType = ResourceType.getEnum(resTypeClass.getName());
        if (resourceType == null) {
            return null;
        }

        String resourceName = variable.getName();

        UExpression node;
        if (element instanceof UExpression) {
            node = (UExpression) element;
        } else if (element instanceof UVariable) {
            node = new JavaUDeclarationsExpression(
                    null, Collections.singletonList(((UVariable) element)));
        } else {
            throw new IllegalArgumentException("element must be an expression or a UVariable");
        }

        return new ResourceReference(node, packageName, resourceType, resourceName);
    }
}