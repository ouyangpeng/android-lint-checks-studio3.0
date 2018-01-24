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

package com.android.tools.lint.client.api;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Detector.UastScanner;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UArrayAccessExpression;
import org.jetbrains.uast.UBinaryExpression;
import org.jetbrains.uast.UBinaryExpressionWithType;
import org.jetbrains.uast.UBlockExpression;
import org.jetbrains.uast.UBreakExpression;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UCallableReferenceExpression;
import org.jetbrains.uast.UCatchClause;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UClassInitializer;
import org.jetbrains.uast.UClassLiteralExpression;
import org.jetbrains.uast.UContinueExpression;
import org.jetbrains.uast.UDeclarationsExpression;
import org.jetbrains.uast.UDoWhileExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UEnumConstant;
import org.jetbrains.uast.UExpressionList;
import org.jetbrains.uast.UField;
import org.jetbrains.uast.UFile;
import org.jetbrains.uast.UForEachExpression;
import org.jetbrains.uast.UForExpression;
import org.jetbrains.uast.UIfExpression;
import org.jetbrains.uast.UImportStatement;
import org.jetbrains.uast.ULabeledExpression;
import org.jetbrains.uast.ULambdaExpression;
import org.jetbrains.uast.ULiteralExpression;
import org.jetbrains.uast.ULocalVariable;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UObjectLiteralExpression;
import org.jetbrains.uast.UParameter;
import org.jetbrains.uast.UParenthesizedExpression;
import org.jetbrains.uast.UPolyadicExpression;
import org.jetbrains.uast.UPostfixExpression;
import org.jetbrains.uast.UPrefixExpression;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.UReturnExpression;
import org.jetbrains.uast.USimpleNameReferenceExpression;
import org.jetbrains.uast.USuperExpression;
import org.jetbrains.uast.USwitchClauseExpression;
import org.jetbrains.uast.USwitchExpression;
import org.jetbrains.uast.UThisExpression;
import org.jetbrains.uast.UThrowExpression;
import org.jetbrains.uast.UTryExpression;
import org.jetbrains.uast.UTypeReferenceExpression;
import org.jetbrains.uast.UUnaryExpression;
import org.jetbrains.uast.UVariable;
import org.jetbrains.uast.UWhileExpression;
import org.jetbrains.uast.visitor.UastVisitor;

/**
 * The {@linkplain UElementHandler} is similar to a {@link UastVisitor},
 * but it is used to only visit a single element. Detectors tell lint which types of elements
 * they want to be called for by invoking {@link UastScanner#getApplicableUastTypes()}.
 * <p>
 * If you want to actually perform a full file visitor iteration you should implement the
 * link {@link #visitFile(UFile)} and then create a {@link UastVisitor} and then invoke
 * that on {@code file.accept(visitor)}.
 */
@SuppressWarnings({"unused", "MethodMayBeStatic"})
public class UElementHandler {

    public static final UElementHandler NONE = new UElementHandler() {
        @Override
        void error(Class<? extends UElement> parameterType) {
        }
    };

    void error(Class<? extends UElement> parameterType) {
        String name = parameterType.getSimpleName();
        assert name.startsWith("U") : name;
        String methodName = "visit" + name.substring(1);
        throw new RuntimeException("You must override " + methodName + " (and don't call super."
                + methodName + "!)");
    }

    public void visitAnnotation(@NonNull UAnnotation uAnnotation) {
        error(UAnnotation.class);
    }

    public void visitArrayAccessExpression(@NonNull UArrayAccessExpression uArrayAccessExpression) {
        error(UArrayAccessExpression.class);
    }

    public void visitBinaryExpression(@NonNull UBinaryExpression uBinaryExpression) {
        error(UBinaryExpression.class);
    }

    public void visitBinaryExpressionWithType(
            UBinaryExpressionWithType uBinaryExpressionWithType) {
        error(UBinaryExpressionWithType.class);
    }

    public void visitBlockExpression(@NonNull UBlockExpression uBlockExpression) {
        error(UBlockExpression.class);
    }

    public void visitBreakExpression(@NonNull UBreakExpression uBreakExpression) {
        error(UBreakExpression.class);
    }

    public void visitCallExpression(@NonNull UCallExpression uCallExpression) {
        error(UCallExpression.class);
    }

    public void visitCallableReferenceExpression(
            UCallableReferenceExpression uCallableReferenceExpression) {
        error(UCallableReferenceExpression.class);
    }

    public void visitCatchClause(@NonNull UCatchClause uCatchClause) {
        error(UCatchClause.class);
    }

    public void visitClass(@NonNull UClass uClass) {
        error(UClass.class);
    }

    public void visitClassLiteralExpression(
            @NonNull UClassLiteralExpression uClassLiteralExpression) {
        error(UClassLiteralExpression.class);
    }

    public void visitContinueExpression(@NonNull UContinueExpression uContinueExpression) {
        error(UContinueExpression.class);
    }

    public void visitDeclarationsExpression(
            @NonNull UDeclarationsExpression uDeclarationsExpression) {
        error(UDeclarationsExpression.class);
    }

    public void visitDoWhileExpression(@NonNull UDoWhileExpression uDoWhileExpression) {
        error(UDoWhileExpression.class);
    }

    public void visitElement(@NonNull UElement uElement) {
        error(UElement.class);
    }

    public void visitEnumConstant(@NonNull UEnumConstant node) {
        error(UEnumConstant.class);
    }

    public void visitExpressionList(@NonNull UExpressionList uExpressionList) {
        error(UExpressionList.class);
    }

    public void visitField(@NonNull UField node) {
        error(UField.class);
    }

    public void visitFile(@NonNull UFile uFile) {
        error(UFile.class);
    }

    public void visitForEachExpression(@NonNull UForEachExpression uForEachExpression) {
        error(UForEachExpression.class);
    }

    public void visitForExpression(@NonNull UForExpression uForExpression) {
        error(UForExpression.class);
    }

    public void visitIfExpression(@NonNull UIfExpression uIfExpression) {
        error(UIfExpression.class);
    }

    public void visitImportStatement(@NonNull UImportStatement uImportStatement) {
        error(UImportStatement.class);
    }

    public void visitInitializer(@NonNull UClassInitializer uClassInitializer) {
        error(UClassInitializer.class);
    }

    public void visitLabeledExpression(@NonNull ULabeledExpression uLabeledExpression) {
        error(ULabeledExpression.class);
    }

    public void visitLambdaExpression(@NonNull ULambdaExpression uLambdaExpression) {
        error(ULambdaExpression.class);
    }

    public void visitLiteralExpression(@NonNull ULiteralExpression uLiteralExpression) {
        error(ULiteralExpression.class);
    }

    public void visitLocalVariable(@NonNull ULocalVariable node) {
        error(ULocalVariable.class);
    }

    public void visitMethod(@NonNull UMethod uMethod) {
        error(UMethod.class);
    }

    public void visitObjectLiteralExpression(
            @NonNull UObjectLiteralExpression uObjectLiteralExpression) {
        error(UObjectLiteralExpression.class);
    }

    public void visitParameter(@NonNull UParameter node) {
        error(UParameter.class);
    }

    public void visitParenthesizedExpression(
            @NonNull UParenthesizedExpression uParenthesizedExpression) {
        error(UParenthesizedExpression.class);
    }

    public void visitPolyadicExpression(@NonNull UPolyadicExpression node) {
        error(UPolyadicExpression.class);
    }

    public void visitPostfixExpression(@NonNull UPostfixExpression uPostfixExpression) {
        error(UPostfixExpression.class);
    }

    public void visitPrefixExpression(@NonNull UPrefixExpression uPrefixExpression) {
        error(UPrefixExpression.class);
    }

    public void visitQualifiedReferenceExpression(
            UQualifiedReferenceExpression uQualifiedReferenceExpression) {
        error(UQualifiedReferenceExpression.class);
    }

    public void visitReturnExpression(@NonNull UReturnExpression uReturnExpression) {
        error(UReturnExpression.class);
    }

    public void visitSimpleNameReferenceExpression(
            USimpleNameReferenceExpression uSimpleNameReferenceExpression) {
        error(USimpleNameReferenceExpression.class);
    }

    public void visitSuperExpression(@NonNull USuperExpression uSuperExpression) {
        error(USuperExpression.class);
    }

    public void visitSwitchClauseExpression(
            @NonNull USwitchClauseExpression uSwitchClauseExpression) {
        error(USwitchClauseExpression.class);
    }

    public void visitSwitchExpression(@NonNull USwitchExpression uSwitchExpression) {
        error(USwitchExpression.class);
    }

    public void visitThisExpression(@NonNull UThisExpression uThisExpression) {
        error(UThisExpression.class);
    }

    public void visitThrowExpression(@NonNull UThrowExpression uThrowExpression) {
        error(UThrowExpression.class);
    }

    public void visitTryExpression(@NonNull UTryExpression uTryExpression) {
        error(UTryExpression.class);
    }

    public void visitTypeReferenceExpression(
            @NonNull UTypeReferenceExpression uTypeReferenceExpression) {
        error(UTypeReferenceExpression.class);
    }

    public void visitUnaryExpression(@NonNull UUnaryExpression uUnaryExpression) {
        error(UUnaryExpression.class);
    }

    public void visitVariable(@NonNull UVariable uVariable) {
        error(UVariable.class);
    }

    public void visitWhileExpression(@NonNull UWhileExpression uWhileExpression) {
        error(UWhileExpression.class);
    }
}
