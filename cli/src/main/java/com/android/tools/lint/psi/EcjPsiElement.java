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
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.IncorrectOperationException;
import javax.swing.Icon;

public class EcjPsiElement implements PsiElement {

    @NonNull
    @Override
    public Project getProject() throws PsiInvalidElementAccessException {
        return null;
    }

    @NonNull
    @Override
    public Language getLanguage() {
        return null;
    }

    @Override
    public PsiManager getManager() {
        return null;
    }

    @NonNull
    @Override
    public PsiElement[] getChildren() {
        return new PsiElement[0];
    }

    @Override
    public PsiElement getParent() {
        return null;
    }

    @Override
    public PsiElement getFirstChild() {
        return null;
    }

    @Override
    public PsiElement getLastChild() {
        return null;
    }

    @Override
    public PsiElement getNextSibling() {
        return null;
    }

    @Override
    public PsiElement getPrevSibling() {
        return null;
    }

    @Override
    public PsiFile getContainingFile() throws PsiInvalidElementAccessException {
        return null;
    }

    @Override
    public TextRange getTextRange() {
        return null;
    }

    @Override
    public int getStartOffsetInParent() {
        return 0;
    }

    @Override
    public int getTextLength() {
        return 0;
    }

    @Nullable
    @Override
    public PsiElement findElementAt(int i) {
        return null;
    }

    @Nullable
    @Override
    public PsiReference findReferenceAt(int i) {
        return null;
    }

    @Override
    public int getTextOffset() {
        return 0;
    }

    @Override
    public String getText() {
        return null;
    }

    @NonNull
    @Override
    public char[] textToCharArray() {
        return new char[0];
    }

    @Override
    public PsiElement getNavigationElement() {
        return null;
    }

    @Override
    public PsiElement getOriginalElement() {
        return null;
    }

    @Override
    public boolean textMatches(@NonNull CharSequence charSequence) {
        return false;
    }

    @Override
    public boolean textMatches(@NonNull PsiElement psiElement) {
        return false;
    }

    @Override
    public boolean textContains(char c) {
        return false;
    }

    @Override
    public void accept(@NonNull PsiElementVisitor psiElementVisitor) {

    }

    @Override
    public void acceptChildren(@NonNull PsiElementVisitor psiElementVisitor) {

    }

    @Override
    public PsiElement copy() {
        return null;
    }

    @Override
    public PsiElement add(@NonNull PsiElement psiElement) throws IncorrectOperationException {
        return null;
    }

    @Override
    public PsiElement addBefore(@NonNull PsiElement psiElement, @Nullable PsiElement psiElement1)
            throws IncorrectOperationException {
        return null;
    }

    @Override
    public PsiElement addAfter(@NonNull PsiElement psiElement, @Nullable PsiElement psiElement1)
            throws IncorrectOperationException {
        return null;
    }

    @Override
    public void checkAdd(@NonNull PsiElement psiElement) throws IncorrectOperationException {

    }

    @Override
    public PsiElement addRange(PsiElement psiElement, PsiElement psiElement1)
            throws IncorrectOperationException {
        return null;
    }

    @Override
    public PsiElement addRangeBefore(@NonNull PsiElement psiElement,
            @NonNull PsiElement psiElement1, PsiElement psiElement2)
            throws IncorrectOperationException {
        return null;
    }

    @Override
    public PsiElement addRangeAfter(PsiElement psiElement, PsiElement psiElement1,
            PsiElement psiElement2) throws IncorrectOperationException {
        return null;
    }

    @Override
    public void delete() throws IncorrectOperationException {

    }

    @Override
    public void checkDelete() throws IncorrectOperationException {

    }

    @Override
    public void deleteChildRange(PsiElement psiElement, PsiElement psiElement1)
            throws IncorrectOperationException {

    }

    @Override
    public PsiElement replace(@NonNull PsiElement psiElement) throws IncorrectOperationException {
        return null;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Nullable
    @Override
    public PsiReference getReference() {
        return null;
    }

    @NonNull
    @Override
    public PsiReference[] getReferences() {
        return new PsiReference[0];
    }

    @Nullable
    @Override
    public <T> T getCopyableUserData(Key<T> key) {
        return null;
    }

    @Override
    public <T> void putCopyableUserData(Key<T> key, @Nullable T t) {

    }

    @Override
    public boolean processDeclarations(@NonNull PsiScopeProcessor psiScopeProcessor,
            @NonNull ResolveState resolveState, @Nullable PsiElement psiElement,
            @NonNull PsiElement psiElement1) {
        return false;
    }

    @Nullable
    @Override
    public PsiElement getContext() {
        return null;
    }

    @Override
    public boolean isPhysical() {
        return false;
    }

    @NonNull
    @Override
    public GlobalSearchScope getResolveScope() {
        return null;
    }

    @NonNull
    @Override
    public SearchScope getUseScope() {
        return null;
    }

    @Override
    public ASTNode getNode() {
        return null;
    }

    @Override
    public boolean isEquivalentTo(PsiElement psiElement) {
        return false;
    }

    @Override
    public Icon getIcon(@IconFlags int i) {
        return null;
    }

    @Nullable
    @Override
    public <T> T getUserData(@NonNull Key<T> key) {
        return null;
    }

    @Override
    public <T> void putUserData(@NonNull Key<T> key, @Nullable T t) {

    }
}
