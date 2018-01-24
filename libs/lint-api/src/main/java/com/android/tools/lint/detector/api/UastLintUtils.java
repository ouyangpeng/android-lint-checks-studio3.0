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

package com.android.tools.lint.detector.api;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.ResourceReference;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiVariable;
import org.jetbrains.uast.UDeclaration;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.ULiteralExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.USimpleNameReferenceExpression;
import org.jetbrains.uast.UVariable;
import org.jetbrains.uast.UastContext;
import org.jetbrains.uast.UastUtils;

public class UastLintUtils {
    @Nullable
    public static String getQualifiedName(PsiElement element) {
        if (element instanceof PsiClass) {
            return ((PsiClass) element).getQualifiedName();
        } else if (element instanceof PsiMethod) {
            PsiClass containingClass = ((PsiMethod) element).getContainingClass();
            if (containingClass == null) {
                return null;
            }
            String containingClassFqName = getQualifiedName(containingClass);
            if (containingClassFqName == null) {
                return null;
            }
            return containingClassFqName + "." + ((PsiMethod) element).getName();
        } else if (element instanceof PsiField) {
            PsiClass containingClass = ((PsiField) element).getContainingClass();
            if (containingClass == null) {
                return null;
            }
            String containingClassFqName = getQualifiedName(containingClass);
            if (containingClassFqName == null) {
                return null;
            }
            return containingClassFqName + "." + ((PsiField) element).getName();
        } else {
            return null;
        }
    }

    @Nullable
    public static PsiElement resolve(ExternalReferenceExpression expression, UElement context) {
        UDeclaration declaration = UastUtils.getParentOfType(context, UDeclaration.class);
        if (declaration == null) {
            return null;
        }

        return expression.resolve(declaration.getPsi());
    }

    @NonNull
    public static String getClassName(PsiClassType type) {
        PsiClass psiClass = type.resolve();
        if (psiClass == null) {
            return type.getClassName();
        } else {
            return getClassName(psiClass);
        }
    }

    @NonNull
    public static String getClassName(PsiClass psiClass) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(psiClass.getName());
        psiClass = psiClass.getContainingClass();
        while (psiClass != null) {
            stringBuilder.insert(0, psiClass.getName() + ".");
            psiClass = psiClass.getContainingClass();
        }
        return stringBuilder.toString();
    }

    @Nullable
    public static UExpression findLastAssignment(
            @NonNull PsiVariable variable,
            @NonNull UElement call) {
        UElement lastAssignment = null;

        if (variable instanceof UVariable) {
            variable = ((UVariable) variable).getPsi();
        }

        if (!variable.hasModifierProperty(PsiModifier.FINAL) &&
                (variable instanceof PsiLocalVariable || variable instanceof PsiParameter)) {
            UMethod containingFunction = UastUtils.getContainingUMethod(call);
            if (containingFunction != null) {
                UastContext context = UastUtils.getUastContext(call);
                ConstantEvaluator.LastAssignmentFinder finder =
                        new ConstantEvaluator.LastAssignmentFinder(variable, call, context, null, -1);
                containingFunction.accept(finder);
                lastAssignment = finder.getLastAssignment();
            }
        } else {
            UastContext context = UastUtils.getUastContext(call);
            lastAssignment = context.getInitializerBody(variable);
        }

        if (lastAssignment instanceof UExpression) {
            return (UExpression) lastAssignment;
        }

        return null;
    }

    @Nullable
    public static String getReferenceName(UReferenceExpression expression) {
        if (expression instanceof USimpleNameReferenceExpression) {
            return ((USimpleNameReferenceExpression) expression).getIdentifier();
        } else if (expression instanceof UQualifiedReferenceExpression) {
            UExpression selector = ((UQualifiedReferenceExpression) expression).getSelector();
            if (selector instanceof USimpleNameReferenceExpression) {
                return ((USimpleNameReferenceExpression) selector).getIdentifier();
            }
        }

        return null;
    }

    @Nullable
    public static Object findLastValue(
            @NonNull PsiVariable variable,
            @NonNull UElement call,
            @NonNull UastContext context,
            @NonNull ConstantEvaluator evaluator) {
        Object value = null;

        if (!variable.hasModifierProperty(PsiModifier.FINAL) &&
                (variable instanceof PsiLocalVariable || variable instanceof PsiParameter)) {
            UMethod containingFunction = UastUtils.getContainingUMethod(call);
            if (containingFunction != null) {
                ConstantEvaluator.LastAssignmentFinder
                        finder = new ConstantEvaluator.LastAssignmentFinder(
                        variable, call, context, evaluator, 1);
                containingFunction.getUastBody().accept(finder);
                value = finder.getCurrentValue();
            }
        } else {
            UExpression initializer = context.getInitializerBody(variable);
            if (initializer != null) {
                value = initializer.evaluate();
            }
        }

        return value;
    }

    @Nullable
    public static ResourceReference toAndroidReferenceViaResolve(UElement element) {
        return ResourceReference.get(element);
    }

    public static boolean areIdentifiersEqual(UExpression first, UExpression second) {
        String firstIdentifier = getIdentifier(first);
        String secondIdentifier = getIdentifier(second);
        return firstIdentifier != null && secondIdentifier != null
                && firstIdentifier.equals(secondIdentifier);
    }

    @Nullable
    public static String getIdentifier(UExpression expression) {
        if (expression instanceof ULiteralExpression) {
            expression.asRenderString();
        } else if (expression instanceof USimpleNameReferenceExpression) {
            return ((USimpleNameReferenceExpression) expression).getIdentifier();
        } else if (expression instanceof UQualifiedReferenceExpression) {
            UQualifiedReferenceExpression qualified = (UQualifiedReferenceExpression) expression;
            String receiverIdentifier = getIdentifier(qualified.getReceiver());
            String selectorIdentifier = getIdentifier(qualified.getSelector());
            if (receiverIdentifier == null || selectorIdentifier == null) {
                return null;
            }
            return receiverIdentifier + "." + selectorIdentifier;
        }

        return null;
    }
}