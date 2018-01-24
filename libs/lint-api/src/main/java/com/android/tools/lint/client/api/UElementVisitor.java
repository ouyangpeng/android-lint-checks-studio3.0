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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.UastScanner;
import com.android.tools.lint.detector.api.Detector.XmlScanner;
import com.android.tools.lint.detector.api.JavaContext;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.jetbrains.uast.util.UastExpressionUtils;
import org.jetbrains.uast.visitor.AbstractUastVisitor;
import org.jetbrains.uast.visitor.UastVisitor;

/**
 * Specialized visitor for running detectors on a Java AST.
 * It operates in three phases:
 * <ol>
 *   <li> First, it computes a set of maps where it generates a map from each
 *        significant AST attribute (such as method call names) to a list
 *        of detectors to consult whenever that attribute is encountered.
 *        Examples of "attributes" are method names, Android resource identifiers,
 *        and general AST node types such as "cast" nodes etc. These are
 *        defined on the {@link UastScanner} interface.
 *   <li> Second, it iterates over the document a single time, delegating to
 *        the detectors found at each relevant AST attribute.
 *   <li> Finally, it calls the remaining visitors (those that need to process a
 *        whole document on their own).
 * </ol>
 * It also notifies all the detectors before and after the document is processed
 * such that they can do pre- and post-processing.
 */
public class UElementVisitor {
    /** Default size of lists holding detectors of the same type for a given node type */
    private static final int SAME_TYPE_COUNT = 8;

    private final Map<String, List<VisitingDetector>> methodDetectors =
            Maps.newHashMapWithExpectedSize(90);
    private final Map<String, List<VisitingDetector>> constructorDetectors =
            Maps.newHashMapWithExpectedSize(12);
    private final Map<String, List<VisitingDetector>> referenceDetectors =
            Maps.newHashMapWithExpectedSize(10);
    private final List<VisitingDetector> resourceFieldDetectors =
            new ArrayList<>();
    private final List<VisitingDetector> allDetectors;
    private final Map<Class<? extends UElement>, List<VisitingDetector>> nodePsiTypeDetectors =
            Maps.newHashMapWithExpectedSize(25);
    private final UastParser parser;
    private final Map<String, List<VisitingDetector>> superClassDetectors =
            new HashMap<>();

    UElementVisitor(@NonNull UastParser parser, @NonNull List<Detector> detectors) {
        this.parser = parser;
        allDetectors = new ArrayList<>(detectors.size());

        for (Detector detector : detectors) {
            UastScanner uastScanner = (UastScanner) detector;
            VisitingDetector v = new VisitingDetector(detector, uastScanner);
            allDetectors.add(v);

            List<String> names = detector.getApplicableMethodNames();
            if (names != null) {
                // not supported in Java visitors; adding a method invocation node is trivial
                // for that case.
                assert names != XmlScanner.ALL;

                for (String name : names) {
                    List<VisitingDetector> list = methodDetectors
                            .computeIfAbsent(name, k -> new ArrayList<>(SAME_TYPE_COUNT));
                    list.add(v);
                }
            }

            List<String> applicableSuperClasses = detector.applicableSuperClasses();
            if (applicableSuperClasses != null) {
                for (String fqn : applicableSuperClasses) {
                    List<VisitingDetector> list = superClassDetectors
                            .computeIfAbsent(fqn, k -> new ArrayList<>(SAME_TYPE_COUNT));
                    list.add(v);
                }
            }

            List<Class<? extends UElement>> nodePsiTypes = detector.getApplicableUastTypes();
            if (nodePsiTypes != null) {
                for (Class<? extends UElement> type : nodePsiTypes) {
                    List<VisitingDetector> list = nodePsiTypeDetectors
                            .computeIfAbsent(type, k -> new ArrayList<>(SAME_TYPE_COUNT));
                    list.add(v);
                }
            }

            List<String> types = detector.getApplicableConstructorTypes();
            if (types != null) {
                // not supported in Java visitors; adding a method invocation node is trivial
                // for that case.
                assert types != XmlScanner.ALL;
                for (String type : types) {
                    List<VisitingDetector> list = constructorDetectors.get(type);
                    if (list == null) {
                        list = new ArrayList<>(SAME_TYPE_COUNT);
                        constructorDetectors.put(type, list);
                    }
                    list.add(v);
                }
            }

            List<String> referenceNames = detector.getApplicableReferenceNames();
            if (referenceNames != null) {
                // not supported in Java visitors; adding a method invocation node is trivial
                // for that case.
                assert referenceNames != XmlScanner.ALL;

                for (String name : referenceNames) {
                    List<VisitingDetector> list = referenceDetectors
                            .computeIfAbsent(name, k -> new ArrayList<>(SAME_TYPE_COUNT));
                    list.add(v);
                }
            }

            if (detector.appliesToResourceRefs()) {
                resourceFieldDetectors.add(v);
            }
        }
    }

    void visitFile(@NonNull final JavaContext context) {
        try {
            UastParser uastParser = context.getUastParser();
            if (uastParser == null) {
                return;
            }

            UFile uFile = uastParser.parse(context);
            if (uFile == null) {
                // No need to log this; the parser should be reporting
                // a full warning (such as IssueRegistry#PARSER_ERROR)
                // with details, location, etc.
                return;
            }

            LintClient client = context.getClient();
            try {
                context.setJavaFile(uFile.getPsi()); // needed for getLocation
                context.setUastFile(uFile);

                client.runReadAction(() -> {
                    for (VisitingDetector v : allDetectors) {
                        v.setContext(context);
                        v.getDetector().beforeCheckFile(context);
                    }
                });

                if (!superClassDetectors.isEmpty()) {
                    client.runReadAction(() -> {
                        SuperclassPsiVisitor visitor = new SuperclassPsiVisitor(context);
                        uFile.accept(visitor);
                    });
                }

                if (!methodDetectors.isEmpty()
                        || !resourceFieldDetectors.isEmpty()
                        || !constructorDetectors.isEmpty()
                        || !referenceDetectors.isEmpty()) {
                    client.runReadAction(() -> {
                        // TODO: Do we need to break this one up into finer grain
                        // locking units
                        UastVisitor visitor = new DelegatingPsiVisitor(context);
                        uFile.accept(visitor);
                    });
                } else {
                    if (!nodePsiTypeDetectors.isEmpty()) {
                        client.runReadAction(() -> {
                            // TODO: Do we need to break this one up into finer grain
                            // locking units
                            UastVisitor visitor = new DispatchPsiVisitor();
                            uFile.accept(visitor);
                        });
                    }
                }

                client.runReadAction(() -> {
                    for (VisitingDetector v : allDetectors) {
                        ProgressManager.checkCanceled();
                        v.getDetector().afterCheckFile(context);
                    }
                });
            } finally {
                parser.dispose(context, uFile);
                context.setJavaFile(null);
                context.setUastFile(null);
            }
        } catch (ProcessCanceledException ignore) {
            // Cancelling inspections in the IDE
        } catch (RuntimeException e) {
            // Don't allow lint bugs to take down the whole build. TRY to log this as a
            // lint error instead!
            LintDriver.handleDetectorError(context, context.getDriver(), e);
        }
    }

    public boolean prepare(@NonNull List<JavaContext> contexts) {
        return parser.prepare(contexts);
    }

    public void dispose() {
        parser.dispose();
    }

    @Nullable
    private static Set<String> getInterfaceNames(
            @Nullable Set<String> addTo,
            @NonNull PsiClass cls) {
        for (PsiClass resolvedInterface : cls.getInterfaces()) {
            String name = resolvedInterface.getQualifiedName();
            if (addTo == null) {
                addTo = Sets.newHashSet();
            } else if (addTo.contains(name)) {
                // Superclasses can explicitly implement the same interface,
                // so keep track of visited interfaces as we traverse up the
                // super class chain to avoid checking the same interface
                // more than once.
                continue;
            }
            addTo.add(name);
            getInterfaceNames(addTo, resolvedInterface);
        }

        return addTo;
    }

    private static class VisitingDetector {
        private UElementHandler mVisitor;
        private JavaContext mContext;
        public final Detector mDetector;
        public final UastScanner mUastScanner;

        public VisitingDetector(@NonNull Detector detector, @NonNull UastScanner uastScanner) {
            mDetector = detector;
            mUastScanner = uastScanner;
        }

        @NonNull
        public Detector getDetector() {
            return mDetector;
        }

        @Nullable
        public UastScanner getUastScanner() {
            return mUastScanner;
        }

        public void setContext(@NonNull JavaContext context) {
            mContext = context;

            // The visitors are one-per-context, so clear them out here and construct
            // lazily only if needed
            mVisitor = null;
        }

        @NonNull
        UElementHandler getVisitor() {
            if (mVisitor == null) {
                mVisitor = mDetector.createUastHandler(mContext);
                if (mVisitor == null) {
                    mVisitor = UElementHandler.NONE;
                }
            }
            return mVisitor;
        }
    }

    private class SuperclassPsiVisitor extends AbstractUastVisitor {
        private final JavaContext context;

        public SuperclassPsiVisitor(@NonNull JavaContext context) {
            this.context = context;
        }

        @Override
        public boolean visitLambdaExpression(ULambdaExpression node) {
            // Have to go to PSI here; not available on ULambdaExpression yet
            // https://github.com/JetBrains/uast/issues/16
            //    ULambdaExpression#getFunctionalInterfaceType
            PsiElement psi = node.getPsi();
            if (psi instanceof PsiLambdaExpression) {
                PsiType type = ((PsiLambdaExpression) psi).getFunctionalInterfaceType();
                if (type instanceof PsiClassType) {
                    PsiClass resolved = ((PsiClassType) type).resolve();
                    if (resolved != null) {
                        checkClass(node, null, resolved);
                    }
                }
            }

            return super.visitLambdaExpression(node);
        }

        @Override
        public boolean visitClass(UClass node) {
            boolean result = super.visitClass(node);
            checkClass(null, node, node);
            return result;
        }

        private void checkClass(@Nullable ULambdaExpression lambda,
                @Nullable UClass uClass,
                @NonNull PsiClass node) {
            ProgressManager.checkCanceled();

            if (node instanceof PsiTypeParameter) {
                // Not included: explained in javadoc for JavaPsiScanner#checkClass
                return;
            }

            PsiClass cls = node;
            int depth = 0;
            while (cls != null) {
                List<VisitingDetector> list = superClassDetectors.get(cls.getQualifiedName());
                if (list != null) {
                    for (VisitingDetector v : list) {
                        UastScanner uastScanner = v.getUastScanner();
                        if (uastScanner != null) {
                            if (uClass != null) {
                                uastScanner.visitClass(context, uClass);
                            } else {
                                assert lambda != null;
                                uastScanner.visitClass(context, lambda);
                            }
                        }
                    }
                }

                // Check interfaces too
                Set<String> interfaceNames = getInterfaceNames(null, cls);
                if (interfaceNames != null) {
                    for (String name : interfaceNames) {
                        list = superClassDetectors.get(name);
                        if (list != null) {
                            for (VisitingDetector v : list) {
                                UastScanner uastScanner = v.getUastScanner();
                                if (uastScanner != null) {
                                    if (uClass != null) {
                                        uastScanner.visitClass(context, uClass);
                                    } else {
                                        assert lambda != null;
                                        uastScanner.visitClass(context, lambda);
                                    }
                                }
                            }
                        }
                    }
                }

                cls = cls.getSuperClass();
                depth++;
                if (depth == 500) {
                    // Shouldn't happen in practice; this prevents the IDE from
                    // hanging if the user has accidentally typed in an incorrect
                    // super class which creates a cycle.
                    break;
                }
            }
        }
    }

    private class DispatchPsiVisitor extends AbstractUastVisitor {

        @Override
        public boolean visitAnnotation(UAnnotation node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UAnnotation.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitAnnotation(node);
                }
            }
            return super.visitAnnotation(node);
        }

        @Override
        public boolean visitArrayAccessExpression(UArrayAccessExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UArrayAccessExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitArrayAccessExpression(node);
                }
            }
            return super.visitArrayAccessExpression(node);
        }

        @Override
        public boolean visitBinaryExpression(UBinaryExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UBinaryExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitBinaryExpression(node);
                }
            }
            return super.visitBinaryExpression(node);
        }

        @Override
        public boolean visitBinaryExpressionWithType(UBinaryExpressionWithType node) {
            List<VisitingDetector> list = nodePsiTypeDetectors
                    .get(UBinaryExpressionWithType.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitBinaryExpressionWithType(node);
                }
            }
            return super.visitBinaryExpressionWithType(node);
        }

        @Override
        public boolean visitBlockExpression(UBlockExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UBlockExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitBlockExpression(node);
                }
            }
            return super.visitBlockExpression(node);
        }

        @Override
        public boolean visitBreakExpression(UBreakExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UBreakExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitBreakExpression(node);
                }
            }
            return super.visitBreakExpression(node);
        }

        @Override
        public boolean visitCallExpression(UCallExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UCallExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitCallExpression(node);
                }
            }
            return super.visitCallExpression(node);
        }

        @Override
        public boolean visitCallableReferenceExpression(UCallableReferenceExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors
                    .get(UCallableReferenceExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitCallableReferenceExpression(node);
                }
            }
            return super.visitCallableReferenceExpression(node);
        }

        @Override
        public boolean visitCatchClause(UCatchClause node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UCatchClause.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitCatchClause(node);
                }
            }
            return super.visitCatchClause(node);
        }

        @Override
        public boolean visitClass(UClass node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UClass.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitClass(node);
                }
            }
            return super.visitClass(node);
        }

        @Override
        public boolean visitClassLiteralExpression(UClassLiteralExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UClassLiteralExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitClassLiteralExpression(node);
                }
            }
            return super.visitClassLiteralExpression(node);
        }

        @Override
        public boolean visitContinueExpression(UContinueExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UContinueExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitContinueExpression(node);
                }
            }
            return super.visitContinueExpression(node);
        }

        @Override
        public boolean visitDeclarationsExpression(UDeclarationsExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UDeclarationsExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitDeclarationsExpression(node);
                }
            }
            return super.visitDeclarationsExpression(node);
        }

        @Override
        public boolean visitDoWhileExpression(UDoWhileExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UDoWhileExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitDoWhileExpression(node);
                }
            }
            return super.visitDoWhileExpression(node);
        }

        @Override
        public boolean visitElement(UElement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UElement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitElement(node);
                }
            }
            return super.visitElement(node);
        }

        @Override
        public boolean visitEnumConstant(UEnumConstant node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UEnumConstant.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitEnumConstant(node);
                }
            }
            return super.visitEnumConstant(node);
        }

        @Override
        public boolean visitExpressionList(UExpressionList node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UExpressionList.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitExpressionList(node);
                }
            }
            return super.visitExpressionList(node);
        }

        @Override
        public boolean visitField(UField node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UField.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitField(node);
                }
            }
            return super.visitField(node);
        }

        @Override
        public boolean visitFile(UFile node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UFile.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitFile(node);
                }
            }
            return super.visitFile(node);
        }

        @Override
        public boolean visitForEachExpression(UForEachExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UForEachExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitForEachExpression(node);
                }
            }
            return super.visitForEachExpression(node);
        }

        @Override
        public boolean visitForExpression(UForExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UForExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitForExpression(node);
                }
            }
            return super.visitForExpression(node);
        }

        @Override
        public boolean visitIfExpression(UIfExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UIfExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitIfExpression(node);
                }
            }
            return super.visitIfExpression(node);
        }

        @Override
        public boolean visitImportStatement(UImportStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UImportStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitImportStatement(node);
                }
            }
            return super.visitImportStatement(node);
        }

        @Override
        public boolean visitInitializer(UClassInitializer node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UClassInitializer.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitInitializer(node);
                }
            }
            return super.visitInitializer(node);
        }

        @Override
        public boolean visitLabeledExpression(ULabeledExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(ULabeledExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitLabeledExpression(node);
                }
            }
            return super.visitLabeledExpression(node);
        }

        @Override
        public boolean visitLambdaExpression(ULambdaExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(ULambdaExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitLambdaExpression(node);
                }
            }
            return super.visitLambdaExpression(node);
        }

        @Override
        public boolean visitLiteralExpression(ULiteralExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(ULiteralExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitLiteralExpression(node);
                }
            }
            return super.visitLiteralExpression(node);
        }

        @Override
        public boolean visitLocalVariable(ULocalVariable node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(ULocalVariable.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitLocalVariable(node);
                }
            }
            return super.visitLocalVariable(node);
        }

        @Override
        public boolean visitMethod(UMethod node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UMethod.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitMethod(node);
                }
            }
            return super.visitMethod(node);
        }

        @Override
        public boolean visitObjectLiteralExpression(UObjectLiteralExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UObjectLiteralExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitObjectLiteralExpression(node);
                }
            }
            return super.visitObjectLiteralExpression(node);
        }

        @Override
        public boolean visitParameter(UParameter node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UParameter.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitParameter(node);
                }
            }
            return super.visitParameter(node);
        }

        @Override
        public boolean visitParenthesizedExpression(UParenthesizedExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UParenthesizedExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitParenthesizedExpression(node);
                }
            }
            return super.visitParenthesizedExpression(node);
        }

        @Override
        public boolean visitPolyadicExpression(UPolyadicExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UPolyadicExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitPolyadicExpression(node);
                }
            }
            return super.visitPolyadicExpression(node);
        }

        @Override
        public boolean visitPostfixExpression(UPostfixExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UPostfixExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitPostfixExpression(node);
                }
            }
            return super.visitPostfixExpression(node);
        }

        @Override
        public boolean visitPrefixExpression(UPrefixExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UPrefixExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitPrefixExpression(node);
                }
            }
            return super.visitPrefixExpression(node);
        }

        @Override
        public boolean visitQualifiedReferenceExpression(UQualifiedReferenceExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors
                    .get(UQualifiedReferenceExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitQualifiedReferenceExpression(node);
                }
            }
            return super.visitQualifiedReferenceExpression(node);
        }

        @Override
        public boolean visitReturnExpression(UReturnExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UReturnExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitReturnExpression(node);
                }
            }
            return super.visitReturnExpression(node);
        }

        @Override
        public boolean visitSimpleNameReferenceExpression(USimpleNameReferenceExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors
                    .get(USimpleNameReferenceExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitSimpleNameReferenceExpression(node);
                }
            }
            return super.visitSimpleNameReferenceExpression(node);
        }

        @Override
        public boolean visitSuperExpression(USuperExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(USuperExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitSuperExpression(node);
                }
            }
            return super.visitSuperExpression(node);
        }

        @Override
        public boolean visitSwitchClauseExpression(USwitchClauseExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(USwitchClauseExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitSwitchClauseExpression(node);
                }
            }
            return super.visitSwitchClauseExpression(node);
        }

        @Override
        public boolean visitSwitchExpression(USwitchExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(USwitchExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitSwitchExpression(node);
                }
            }
            return super.visitSwitchExpression(node);
        }

        @Override
        public boolean visitThisExpression(UThisExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UThisExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitThisExpression(node);
                }
            }
            return super.visitThisExpression(node);
        }

        @Override
        public boolean visitThrowExpression(UThrowExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UThrowExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitThrowExpression(node);
                }
            }
            return super.visitThrowExpression(node);
        }

        @Override
        public boolean visitTryExpression(UTryExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UTryExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitTryExpression(node);
                }
            }
            return super.visitTryExpression(node);
        }

        @Override
        public boolean visitTypeReferenceExpression(UTypeReferenceExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UTypeReferenceExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitTypeReferenceExpression(node);
                }
            }
            return super.visitTypeReferenceExpression(node);
        }

        @Override
        public boolean visitUnaryExpression(UUnaryExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UUnaryExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitUnaryExpression(node);
                }
            }
            return super.visitUnaryExpression(node);
        }

        @Override
        public boolean visitVariable(UVariable node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UVariable.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitVariable(node);
                }
            }
            return super.visitVariable(node);
        }

        @Override
        public boolean visitWhileExpression(UWhileExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(UWhileExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitWhileExpression(node);
                }
            }
            return super.visitWhileExpression(node);
        }
    }

    /** Performs common AST searches for method calls and R-type-field references.
     * Note that this is a specialized form of the {@link DispatchPsiVisitor}. */
    private class DelegatingPsiVisitor extends DispatchPsiVisitor {
        private final JavaContext mContext;
        private final boolean mVisitResources;
        private final boolean mVisitMethods;
        private final boolean mVisitConstructors;
        private final boolean mVisitReferences;

        DelegatingPsiVisitor(JavaContext context) {
            mContext = context;

            mVisitMethods = !methodDetectors.isEmpty();
            mVisitConstructors = !constructorDetectors.isEmpty();
            mVisitResources = !resourceFieldDetectors.isEmpty();
            mVisitReferences = !referenceDetectors.isEmpty();
        }

        @Override
        public boolean visitSimpleNameReferenceExpression(USimpleNameReferenceExpression node) {
            if (mVisitReferences || mVisitResources) {
                ProgressManager.checkCanceled();
            }

            if (mVisitReferences) {
                List<VisitingDetector> list = referenceDetectors.get(node.getIdentifier());
                if (list != null) {
                    PsiElement referenced = node.resolve();
                    if (referenced != null) {
                        for (VisitingDetector v : list) {
                            UastScanner uastScanner = v.getUastScanner();
                            if (uastScanner != null) {
                                uastScanner.visitReference(mContext, node, referenced);
                            }
                        }
                    }
                }
            }

            if (mVisitResources) {
                ResourceReference reference = ResourceReference.get(node);
                if (reference != null) {
                    for (VisitingDetector v : resourceFieldDetectors) {
                        UastScanner uastScanner = v.getUastScanner();
                        if (uastScanner != null) {
                            uastScanner.visitResourceReference(mContext,
                                    reference.node,
                                    reference.getType(),
                                    reference.getName(),
                                    reference.getPackage().equals(ANDROID_PKG));
                        }
                    }
                }
            }

            return super.visitSimpleNameReferenceExpression(node);
        }

        @Override
        public boolean visitCallExpression(UCallExpression node) {
            boolean result = super.visitCallExpression(node);

            ProgressManager.checkCanceled();

            if (UastExpressionUtils.isMethodCall(node)) {
                visitMethodCallExpression(node);
            } else if (UastExpressionUtils.isConstructorCall(node)) {
                visitNewExpression(node);
            }

            return result;
        }

        private void visitMethodCallExpression(UCallExpression node) {
            if (mVisitMethods) {
                String methodName = node.getMethodName();
                if (methodName != null) {
                    List<VisitingDetector> list = methodDetectors.get(methodName);
                    if (list != null) {
                        PsiMethod function = node.resolve();
                        if (function != null) {
                            for (VisitingDetector v : list) {
                                UastScanner scanner = v.getUastScanner();
                                if (scanner != null) {
                                    scanner.visitMethod(mContext, node, function);
                                }
                            }
                        }
                    }
                }
            }
        }

        private void visitNewExpression(UCallExpression node) {
            if (mVisitConstructors) {
                PsiMethod method = node.resolve();
                if (method == null) {
                    return;
                }

                PsiClass resolvedClass = method.getContainingClass();
                if (resolvedClass != null) {
                    List<VisitingDetector> list = constructorDetectors.get(
                            resolvedClass.getQualifiedName());
                    if (list != null) {
                        for (VisitingDetector v : list) {
                            UastScanner javaPsiScanner = v.getUastScanner();
                            if (javaPsiScanner != null) {
                                javaPsiScanner.visitConstructor(mContext, node, method);
                            }
                        }
                    }
                }
            }
        }
    }
}
