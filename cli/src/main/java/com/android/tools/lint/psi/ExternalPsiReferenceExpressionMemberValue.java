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

package com.android.tools.lint.psi;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.HierarchicalMethodSignature;
import com.intellij.psi.JavaResolveResult;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassInitializer;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiReferenceParameterList;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.PsiTypeParameterList;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.util.IncorrectOperationException;
import java.util.Collection;
import java.util.List;

public class ExternalPsiReferenceExpressionMemberValue extends EcjPsiElement implements PsiField, PsiReferenceExpression {

    private final String mFullyQualifiedName;

    public ExternalPsiReferenceExpressionMemberValue(String fullyQualifiedName) {
        mFullyQualifiedName = fullyQualifiedName;
    }

    @Nullable
    @Override
    public PsiElement getReferenceNameElement() {
        return null;
    }

    @Nullable
    @Override
    public PsiReferenceParameterList getParameterList() {
        return null;
    }

    @NonNull
    @Override
    public PsiType[] getTypeParameters() {
        return new PsiType[0];
    }

    @Override
    public boolean isQualified() {
        return false;
    }

    @Override
    public String getQualifiedName() {
        return mFullyQualifiedName;
    }

    @Nullable
    @Override
    public PsiElement getQualifier() {
        return null;
    }

    @Nullable
    @Override
    public String getReferenceName() {
        return getName();
    }

    @Nullable
    @Override
    public String getName() {
        return mFullyQualifiedName.substring(mFullyQualifiedName.lastIndexOf('.') + 1);
    }

    @Nullable
    @Override
    public ItemPresentation getPresentation() {
        return null;
    }

    @Override
    public PsiElement getElement() {
        return null;
    }

    @Override
    public TextRange getRangeInElement() {
        return null;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        // Ideally we'd find the real class via the PSI manager, but if not, do a reasonable
        // job here
        return this;
    }

    @NonNull
    @Override
    public String getCanonicalText() {
        return null;
    }

    @Override
    public PsiElement handleElementRename(String s) throws IncorrectOperationException {
        return null;
    }

    @Override
    public PsiElement bindToElement(@NonNull PsiElement psiElement)
            throws IncorrectOperationException {
        return null;
    }

    @Override
    public boolean isReferenceTo(PsiElement psiElement) {
        return false;
    }

    @NonNull
    @Override
    public Object[] getVariants() {
        return new Object[0];
    }

    @Override
    public boolean isSoft() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PsiReferenceExpression) {
            return mFullyQualifiedName.equals(((PsiReferenceExpression)o).getQualifiedName());
        } else if (o instanceof PsiField) {
            PsiField psiField = (PsiField) o;
            PsiClass containingClass = psiField.getContainingClass();
            String name = psiField.getName();
            if (containingClass != null && name != null) {
                String qualifiedName = containingClass.getQualifiedName();
                if (qualifiedName != null) {
                    return mFullyQualifiedName.length() == qualifiedName.length() + name.length() + 1
                            && mFullyQualifiedName.startsWith(qualifiedName)
                            && mFullyQualifiedName.endsWith(name);
                }
            }
        }
        return super.equals(o);
    }

    @Nullable
    @Override
    public PsiClass getContainingClass() {
        return new ExternalPsiClass(mFullyQualifiedName.substring(0, mFullyQualifiedName.lastIndexOf('.')));
    }

    @Nullable
    @Override
    public PsiModifierList getModifierList() {
        return null;
    }

    @Override
    public boolean hasModifierProperty(@NonNull @PsiModifier.ModifierConstant String s) {
        return false;
    }

    @NonNull
    @Override
    public PsiType getType() {
        return null;
    }

    @Nullable
    @Override
    public PsiTypeElement getTypeElement() {
        return null;
    }

    @Nullable
    @Override
    public PsiExpression getInitializer() {
        return null;
    }

    @Override
    public boolean hasInitializer() {
        return false;
    }

    @Override
    public void normalizeDeclaration() throws IncorrectOperationException {

    }

    @Nullable
    @Override
    public Object computeConstantValue() {
        return null;
    }

    @Override
    public void setInitializer(@Nullable PsiExpression psiExpression)
            throws IncorrectOperationException {

    }

    @NonNull
    @Override
    public PsiIdentifier getNameIdentifier() {
        // See nullness comment in the ExternalPsiClass constructor.
        //noinspection ConstantConditions
        return null;
    }

    @Override
    public PsiElement setName(@NonNull String s) {
        return null;
    }

    @Nullable
    @Override
    public PsiDocComment getDocComment() {
        return null;
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    @Override
    public void navigate(boolean b) {

    }

    @Override
    public boolean canNavigate() {
        return false;
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }

    @Nullable
    @Override
    public PsiExpression getQualifierExpression() {
        return null;
    }

    @Override
    public PsiElement bindToElementViaStaticImport(@NonNull PsiClass psiClass)
            throws IncorrectOperationException {
        return null;
    }

    @Override
    public void setQualifierExpression(
            @Nullable PsiExpression psiExpression)
            throws IncorrectOperationException {

    }

    @Override
    public void processVariants(@NonNull PsiScopeProcessor psiScopeProcessor) {

    }

    @NonNull
    @Override
    public JavaResolveResult advancedResolve(boolean b) {
        return null;
    }

    @NonNull
    @Override
    public JavaResolveResult[] multiResolve(boolean b) {
        return new JavaResolveResult[0];
    }

    private static class ExternalPsiClass extends EcjPsiElement implements PsiClass {

        private final String mFullyQualifiedName;

        public ExternalPsiClass(@Nullable String fullyQualifiedName) {
            mFullyQualifiedName = fullyQualifiedName;
        }

        @Nullable
        @Override
        public String getQualifiedName() {
            return mFullyQualifiedName;
        }

        @Nullable
        @Override
        public String getName() {
            return mFullyQualifiedName.substring(mFullyQualifiedName.lastIndexOf('.') + 1);
        }

        @Nullable
        @Override
        public ItemPresentation getPresentation() {
            return null;
        }

        @Override
        public int hashCode() {
            return 0; // not intended to be used in collections
        }

        @Override
        public boolean isInterface() {
            return false;
        }

        @Override
        public boolean isAnnotationType() {
            return false;
        }

        @Override
        public boolean isEnum() {
            return false;
        }

        @Nullable
        @Override
        public PsiReferenceList getExtendsList() {
            return null;
        }

        @Nullable
        @Override
        public PsiReferenceList getImplementsList() {
            return null;
        }

        @NonNull
        @Override
        public PsiClassType[] getExtendsListTypes() {
            return PsiClassType.EMPTY_ARRAY;
        }

        @NonNull
        @Override
        public PsiClassType[] getImplementsListTypes() {
            return PsiClassType.EMPTY_ARRAY;
        }

        @Nullable
        @Override
        public PsiClass getSuperClass() {
            return null;
        }

        @Override
        public PsiClass[] getInterfaces() {
            return PsiClass.EMPTY_ARRAY;
        }

        @NonNull
        @Override
        public PsiClass[] getSupers() {
            return PsiClass.EMPTY_ARRAY;
        }

        @NonNull
        @Override
        public PsiClassType[] getSuperTypes() {
            return PsiClassType.EMPTY_ARRAY;
        }

        @NonNull
        @Override
        public PsiField[] getFields() {
            return PsiField.EMPTY_ARRAY;
        }

        @NonNull
        @Override
        public PsiMethod[] getMethods() {
            return PsiMethod.EMPTY_ARRAY;
        }

        @NonNull
        @Override
        public PsiMethod[] getConstructors() {
            return PsiMethod.EMPTY_ARRAY;
        }

        @NonNull
        @Override
        public PsiClass[] getInnerClasses() {
            return PsiClass.EMPTY_ARRAY;
        }

        @NonNull
        @Override
        public PsiClassInitializer[] getInitializers() {
            return PsiClassInitializer.EMPTY_ARRAY;
        }

        @NonNull
        @Override
        public PsiField[] getAllFields() {
            return PsiField.EMPTY_ARRAY;
        }

        @NonNull
        @Override
        public PsiMethod[] getAllMethods() {
            return PsiMethod.EMPTY_ARRAY;
        }

        @NonNull
        @Override
        public PsiClass[] getAllInnerClasses() {
            return PsiClass.EMPTY_ARRAY;
        }

        @Nullable
        @Override
        public PsiField findFieldByName(String s, boolean b) {
            return null;
        }

        @Nullable
        @Override
        public PsiMethod findMethodBySignature(PsiMethod psiMethod, boolean b) {
            return null;
        }

        @NonNull
        @Override
        public PsiMethod[] findMethodsBySignature(PsiMethod psiMethod, boolean b) {
            return PsiMethod.EMPTY_ARRAY;
        }

        @NonNull
        @Override
        public PsiMethod[] findMethodsByName(String s, boolean b) {
            return PsiMethod.EMPTY_ARRAY;
        }

        @NonNull
        @Override
        public List<Pair<PsiMethod, PsiSubstitutor>> findMethodsAndTheirSubstitutorsByName(
                String s, boolean b) {
            return null;
        }

        @NonNull
        @Override
        public List<Pair<PsiMethod, PsiSubstitutor>> getAllMethodsAndTheirSubstitutors() {
            return null;
        }

        @Nullable
        @Override
        public PsiClass findInnerClassByName(String s, boolean b) {
            return null;
        }

        @Nullable
        @Override
        public PsiElement getLBrace() {
            return null;
        }

        @Nullable
        @Override
        public PsiElement getRBrace() {
            return null;
        }

        @Nullable
        @Override
        public PsiIdentifier getNameIdentifier() {
            return null;
        }

        @Override
        public PsiElement getScope() {
            return null;
        }

        @Override
        public boolean isInheritor(@NonNull PsiClass psiClass, boolean b) {
            return false;
        }

        @Override
        public boolean isInheritorDeep(PsiClass psiClass, PsiClass psiClass1) {
            return false;
        }

        @Nullable
        @Override
        public PsiClass getContainingClass() {
            return null;
        }

        @NonNull
        @Override
        public Collection<HierarchicalMethodSignature> getVisibleSignatures() {
            return null;
        }

        @Override
        public PsiElement setName(@NonNull String s) {
            return null;
        }

        @Nullable
        @Override
        public PsiDocComment getDocComment() {
            return null;
        }

        @Override
        public boolean isDeprecated() {
            return false;
        }

        @Override
        public boolean hasTypeParameters() {
            return false;
        }

        @Nullable
        @Override
        public PsiTypeParameterList getTypeParameterList() {
            return null;
        }

        @NonNull
        @Override
        public PsiTypeParameter[] getTypeParameters() {
            return PsiTypeParameter.EMPTY_ARRAY;
        }

        @Nullable
        @Override
        public PsiModifierList getModifierList() {
            return null;
        }

        @Override
        public boolean hasModifierProperty(@NonNull @PsiModifier.ModifierConstant String s) {
            return false;
        }

        @Override
        public void navigate(boolean b) {

        }

        @Override
        public boolean canNavigate() {
            return false;
        }

        @Override
        public boolean canNavigateToSource() {
            return false;
        }
    }
}
