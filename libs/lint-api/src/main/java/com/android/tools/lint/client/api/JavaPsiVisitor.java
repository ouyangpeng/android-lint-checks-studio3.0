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
import static com.android.SdkConstants.R_CLASS;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceType;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.JavaPsiScanner;
import com.android.tools.lint.detector.api.Detector.XmlScanner;
import com.android.tools.lint.detector.api.JavaContext;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.ImplicitVariable;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMethod;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiArrayAccessExpression;
import com.intellij.psi.PsiArrayInitializerExpression;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiAssertStatement;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiBinaryExpression;
import com.intellij.psi.PsiBlockStatement;
import com.intellij.psi.PsiBreakStatement;
import com.intellij.psi.PsiCallExpression;
import com.intellij.psi.PsiCatchSection;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassInitializer;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiConditionalExpression;
import com.intellij.psi.PsiContinueStatement;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiDoWhileStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEmptyStatement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiEnumConstantInitializer;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiExpressionListStatement;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiForStatement;
import com.intellij.psi.PsiForeachStatement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiIfStatement;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiImportStaticReferenceElement;
import com.intellij.psi.PsiImportStaticStatement;
import com.intellij.psi.PsiInstanceOfExpression;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiKeyword;
import com.intellij.psi.PsiLabeledStatement;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiMethodReferenceExpression;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiPackageStatement;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiParenthesizedExpression;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.PsiPostfixExpression;
import com.intellij.psi.PsiPrefixExpression;
import com.intellij.psi.PsiReceiverParameter;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiReferenceParameterList;
import com.intellij.psi.PsiResourceList;
import com.intellij.psi.PsiResourceVariable;
import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiSuperExpression;
import com.intellij.psi.PsiSwitchLabelStatement;
import com.intellij.psi.PsiSwitchStatement;
import com.intellij.psi.PsiSynchronizedStatement;
import com.intellij.psi.PsiThisExpression;
import com.intellij.psi.PsiThrowStatement;
import com.intellij.psi.PsiTryStatement;
import com.intellij.psi.PsiTypeCastExpression;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.PsiTypeParameterList;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.PsiWhileStatement;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.javadoc.PsiDocTagValue;
import com.intellij.psi.javadoc.PsiDocToken;
import com.intellij.psi.javadoc.PsiInlineDocTag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Specialized visitor for running detectors on a Java AST.
 * It operates in three phases:
 * <ol>
 *   <li> First, it computes a set of maps where it generates a map from each
 *        significant AST attribute (such as method call names) to a list
 *        of detectors to consult whenever that attribute is encountered.
 *        Examples of "attributes" are method names, Android resource identifiers,
 *        and general AST node types such as "cast" nodes etc. These are
 *        defined on the {@link JavaPsiScanner} interface.
 *   <li> Second, it iterates over the document a single time, delegating to
 *        the detectors found at each relevant AST attribute.
 *   <li> Finally, it calls the remaining visitors (those that need to process a
 *        whole document on their own).
 * </ol>
 * It also notifies all the detectors before and after the document is processed
 * such that they can do pre- and post-processing.
 */
public class JavaPsiVisitor {
    /** Default size of lists holding detectors of the same type for a given node type */
    private static final int SAME_TYPE_COUNT = 8;

    private final Map<String, List<VisitingDetector>> methodDetectors =
            Maps.newHashMapWithExpectedSize(80);
    private final Map<String, List<VisitingDetector>> constructorDetectors =
            Maps.newHashMapWithExpectedSize(12);
    private final Map<String, List<VisitingDetector>> referenceDetectors =
            Maps.newHashMapWithExpectedSize(10);
    private final List<VisitingDetector> resourceFieldDetectors =
            new ArrayList<>();
    private final List<VisitingDetector> allDetectors;
    private final List<VisitingDetector> fullTreeDetectors;
    private final Map<Class<? extends PsiElement>, List<VisitingDetector>> nodePsiTypeDetectors =
            new HashMap<>(16);
    private final JavaParser parser;
    private final Map<String, List<VisitingDetector>> superClassDetectors =
            new HashMap<>();

    /**
     * Whether we should call {@link JavaParser#dispose(JavaContext, PsiJavaFile)} after the
     * file has been processed with this visitor
     */
    private boolean disposeUnitsAfterUse = true;

    JavaPsiVisitor(@NonNull JavaParser parser, @NonNull List<Detector> detectors) {
        this.parser = parser;
        allDetectors = new ArrayList<>(detectors.size());
        fullTreeDetectors = new ArrayList<>(detectors.size());

        for (Detector detector : detectors) {
            JavaPsiScanner javaPsiScanner = (JavaPsiScanner) detector;
            VisitingDetector v = new VisitingDetector(detector, javaPsiScanner);
            allDetectors.add(v);

            List<String> applicableSuperClasses = detector.applicableSuperClasses();
            if (applicableSuperClasses != null) {
                for (String fqn : applicableSuperClasses) {
                    List<VisitingDetector> list = superClassDetectors.get(fqn);
                    if (list == null) {
                        list = new ArrayList<>(SAME_TYPE_COUNT);
                        superClassDetectors.put(fqn, list);
                    }
                    list.add(v);
                }
            }

            List<Class<? extends PsiElement>> nodePsiTypes = detector.getApplicablePsiTypes();
            if (nodePsiTypes != null) {
                for (Class<? extends PsiElement> type : nodePsiTypes) {
                    List<VisitingDetector> list = nodePsiTypeDetectors.get(type);
                    if (list == null) {
                        list = new ArrayList<>(SAME_TYPE_COUNT);
                        nodePsiTypeDetectors.put(type, list);
                    }
                    list.add(v);
                }
            }

            List<String> names = detector.getApplicableMethodNames();
            if (names != null) {
                // not supported in Java visitors; adding a method invocation node is trivial
                // for that case.
                assert names != XmlScanner.ALL;

                for (String name : names) {
                    List<VisitingDetector> list = methodDetectors.get(name);
                    if (list == null) {
                        list = new ArrayList<>(SAME_TYPE_COUNT);
                        methodDetectors.put(name, list);
                    }
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
                    List<VisitingDetector> list = referenceDetectors.get(name);
                    if (list == null) {
                        list = new ArrayList<>(SAME_TYPE_COUNT);
                        referenceDetectors.put(name, list);
                    }
                    list.add(v);
                }
            }

            if (detector.appliesToResourceRefs()) {
                resourceFieldDetectors.add(v);
            } else if ((applicableSuperClasses == null || applicableSuperClasses.isEmpty())
                    && (referenceNames == null || referenceNames.isEmpty())
                    && (nodePsiTypes == null || nodePsiTypes.isEmpty())
                    && (types == null || types.isEmpty())) {
                fullTreeDetectors.add(v);
            }
        }
    }

    public void setDisposeUnitsAfterUse(boolean disposeUnitsAfterUse) {
        this.disposeUnitsAfterUse = disposeUnitsAfterUse;
    }

    void visitFile(@NonNull final JavaContext context) {
        try {
            final PsiJavaFile javaFile = parser.parseJavaToPsi(context);
            if (javaFile == null) {
                // No need to log this; the parser should be reporting
                // a full warning (such as IssueRegistry#PARSER_ERROR)
                // with details, location, etc.
                return;
            }
            try {
                context.setJavaFile(javaFile);
                LintClient client = context.getClient();

                client.runReadAction(() -> {
                    for (VisitingDetector v : allDetectors) {
                        v.setContext(context);
                        v.getDetector().beforeCheckFile(context);
                    }
                });

                if (!superClassDetectors.isEmpty()) {
                    client.runReadAction(() -> {
                        SuperclassPsiVisitor visitor = new SuperclassPsiVisitor(context);
                        javaFile.accept(visitor);
                    });
                }

                for (final VisitingDetector v : fullTreeDetectors) {
                    client.runReadAction(() -> {
                        JavaElementVisitor visitor = v.getVisitor();
                        javaFile.accept(visitor);
                    });
                }

                if (!methodDetectors.isEmpty()
                        || !resourceFieldDetectors.isEmpty()
                        || !constructorDetectors.isEmpty()
                        || !referenceDetectors.isEmpty()) {
                    client.runReadAction(() -> {
                        // TODO: Do we need to break this one up into finer grain
                        // locking units
                        JavaElementVisitor visitor = new DelegatingPsiVisitor(context);
                        javaFile.accept(visitor);
                    });
                } else {
                    if (!nodePsiTypeDetectors.isEmpty()) {
                        client.runReadAction(() -> {
                            // TODO: Do we need to break this one up into finer grain
                            // locking units
                            JavaElementVisitor visitor = new DispatchPsiVisitor();
                            javaFile.accept(visitor);
                        });
                    }
                }

                client.runReadAction(() -> {
                    for (VisitingDetector v : allDetectors) {
                        v.getDetector().afterCheckFile(context);
                    }
                });
            } finally {
                if (disposeUnitsAfterUse) {
                    parser.dispose(context, javaFile);
                }
                context.setJavaFile(null);
            }
        } catch (RuntimeException e) {
            // Don't allow lint bugs to take down the whole build. TRY to log this as a
            // lint error instead!
            LintDriver.handleDetectorError(context, context.getDriver(), e);
        }
    }

    public boolean prepare(@NonNull List<JavaContext> contexts) {
        return parser.prepareJavaParse(contexts);
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
        private JavaElementVisitor mVisitor;
        private JavaContext mContext;
        public final Detector mDetector;
        public final JavaPsiScanner mJavaScanner;

        public VisitingDetector(@NonNull Detector detector, @NonNull JavaPsiScanner javaScanner) {
            mDetector = detector;
            mJavaScanner = javaScanner;
        }

        @NonNull
        public Detector getDetector() {
            return mDetector;
        }

        @Nullable
        public JavaPsiScanner getJavaScanner() {
            return mJavaScanner;
        }

        public void setContext(@NonNull JavaContext context) {
            mContext = context;

            // The visitors are one-per-context, so clear them out here and construct
            // lazily only if needed
            mVisitor = null;
        }

        @NonNull
        JavaElementVisitor getVisitor() {
            if (mVisitor == null) {
                mVisitor = mDetector.createPsiVisitor(mContext);
                assert !(mVisitor instanceof JavaRecursiveElementVisitor) :
                        "Your visitor (returned by " + mDetector.getClass().getSimpleName()
                        + "#createPsiVisitor(...) should *not* extend "
                        + " JavaRecursiveElementVisitor; use a plain "
                        + "JavaElementVisitor instead. The lint infrastructure does its own "
                        + "recursion calling *just* your visit methods specified in "
                        + "getApplicablePsiTypes";
                if (mVisitor == null) {
                    mVisitor = new JavaElementVisitor() {
                        @Override
                        public void visitElement(PsiElement element) {
                            // No-op. Workaround for super currently calling
                            //   ProgressIndicatorProvider.checkCanceled();
                        }
                    };
                }
            }
            return mVisitor;
        }
    }

    private class SuperclassPsiVisitor extends JavaRecursiveElementVisitor {
        private final JavaContext mContext;

        public SuperclassPsiVisitor(@NonNull JavaContext context) {
            mContext = context;
        }

        @Override
        public void visitClass(@NonNull PsiClass node) {
            super.visitClass(node);
            checkClass(node);
        }

        private void checkClass(@NonNull PsiClass node) {
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
                        JavaPsiScanner javaPsiScanner = v.getJavaScanner();
                        if (javaPsiScanner != null) {
                            javaPsiScanner.checkClass(mContext, node);
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
                                JavaPsiScanner javaPsiScanner = v.getJavaScanner();
                                if (javaPsiScanner != null) {
                                    javaPsiScanner.checkClass(mContext, node);
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

    private class DispatchPsiVisitor extends JavaRecursiveElementVisitor {

        @Override
        public void visitAnonymousClass(PsiAnonymousClass node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiAnonymousClass.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitAnonymousClass(node);
                }
            }
            super.visitAnonymousClass(node);
        }

        @Override
        public void visitArrayAccessExpression(PsiArrayAccessExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiArrayAccessExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitArrayAccessExpression(node);
                }
            }
            super.visitArrayAccessExpression(node);
        }

        @Override
        public void visitArrayInitializerExpression(PsiArrayInitializerExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors
                    .get(PsiArrayInitializerExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitArrayInitializerExpression(node);
                }
            }
            super.visitArrayInitializerExpression(node);
        }

        @Override
        public void visitAssertStatement(PsiAssertStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiAssertStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitAssertStatement(node);
                }
            }
            super.visitAssertStatement(node);
        }

        @Override
        public void visitAssignmentExpression(PsiAssignmentExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiAssignmentExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitAssignmentExpression(node);
                }
            }
            super.visitAssignmentExpression(node);
        }

        @Override
        public void visitBinaryExpression(PsiBinaryExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiBinaryExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitBinaryExpression(node);
                }
            }
            super.visitBinaryExpression(node);
        }

        @Override
        public void visitBlockStatement(PsiBlockStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiBlockStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitBlockStatement(node);
                }
            }
            super.visitBlockStatement(node);
        }

        @Override
        public void visitBreakStatement(PsiBreakStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiBreakStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitBreakStatement(node);
                }
            }
            super.visitBreakStatement(node);
        }

        @Override
        public void visitClass(PsiClass node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiClass.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitClass(node);
                }
            }
            super.visitClass(node);
        }

        @Override
        public void visitClassInitializer(PsiClassInitializer node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiClassInitializer.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitClassInitializer(node);
                }
            }
            super.visitClassInitializer(node);
        }

        @Override
        public void visitClassObjectAccessExpression(PsiClassObjectAccessExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors
                    .get(PsiClassObjectAccessExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitClassObjectAccessExpression(node);
                }
            }
            super.visitClassObjectAccessExpression(node);
        }

        @Override
        public void visitCodeBlock(PsiCodeBlock node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiCodeBlock.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitCodeBlock(node);
                }
            }
            super.visitCodeBlock(node);
        }

        @Override
        public void visitConditionalExpression(PsiConditionalExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiConditionalExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitConditionalExpression(node);
                }
            }
            super.visitConditionalExpression(node);
        }

        @Override
        public void visitContinueStatement(PsiContinueStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiContinueStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitContinueStatement(node);
                }
            }
            super.visitContinueStatement(node);
        }

        @Override
        public void visitDeclarationStatement(PsiDeclarationStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiDeclarationStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitDeclarationStatement(node);
                }
            }
            super.visitDeclarationStatement(node);
        }

        @Override
        public void visitDocComment(PsiDocComment node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiDocComment.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitDocComment(node);
                }
            }
            super.visitDocComment(node);
        }

        @Override
        public void visitDocTag(PsiDocTag node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiDocTag.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitDocTag(node);
                }
            }
            super.visitDocTag(node);
        }

        @Override
        public void visitDocTagValue(PsiDocTagValue node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiDocTagValue.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitDocTagValue(node);
                }
            }
            super.visitDocTagValue(node);
        }

        @Override
        public void visitDoWhileStatement(PsiDoWhileStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiDoWhileStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitDoWhileStatement(node);
                }
            }
            super.visitDoWhileStatement(node);
        }

        @Override
        public void visitEmptyStatement(PsiEmptyStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiEmptyStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitEmptyStatement(node);
                }
            }
            super.visitEmptyStatement(node);
        }

        @Override
        public void visitExpression(PsiExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitExpression(node);
                }
            }
            super.visitExpression(node);
        }

        @Override
        public void visitExpressionList(PsiExpressionList node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiExpressionList.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitExpressionList(node);
                }
            }
            super.visitExpressionList(node);
        }

        @Override
        public void visitExpressionListStatement(PsiExpressionListStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors
                    .get(PsiExpressionListStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitExpressionListStatement(node);
                }
            }
            super.visitExpressionListStatement(node);
        }

        @Override
        public void visitExpressionStatement(PsiExpressionStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiExpressionStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitExpressionStatement(node);
                }
            }
            super.visitExpressionStatement(node);
        }

        @Override
        public void visitField(PsiField node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiField.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitField(node);
                }
            }
            super.visitField(node);
        }

        @Override
        public void visitForStatement(PsiForStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiForStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitForStatement(node);
                }
            }
            super.visitForStatement(node);
        }

        @Override
        public void visitForeachStatement(PsiForeachStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiForeachStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitForeachStatement(node);
                }
            }
            super.visitForeachStatement(node);
        }

        @Override
        public void visitIdentifier(PsiIdentifier node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiIdentifier.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitIdentifier(node);
                }
            }
            super.visitIdentifier(node);
        }

        @Override
        public void visitIfStatement(PsiIfStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiIfStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitIfStatement(node);
                }
            }
            super.visitIfStatement(node);
        }

        @Override
        public void visitImportList(PsiImportList node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiImportList.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitImportList(node);
                }
            }
            super.visitImportList(node);
        }

        @Override
        public void visitImportStatement(PsiImportStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiImportStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitImportStatement(node);
                }
            }
            super.visitImportStatement(node);
        }

        @Override
        public void visitImportStaticStatement(PsiImportStaticStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiImportStaticStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitImportStaticStatement(node);
                }
            }
            super.visitImportStaticStatement(node);
        }

        @Override
        public void visitInlineDocTag(PsiInlineDocTag node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiInlineDocTag.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitInlineDocTag(node);
                }
            }
            super.visitInlineDocTag(node);
        }

        @Override
        public void visitInstanceOfExpression(PsiInstanceOfExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiInstanceOfExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitInstanceOfExpression(node);
                }
            }
            super.visitInstanceOfExpression(node);
        }

        @Override
        public void visitJavaToken(PsiJavaToken node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiJavaToken.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitJavaToken(node);
                }
            }
            super.visitJavaToken(node);
        }

        @Override
        public void visitKeyword(PsiKeyword node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiKeyword.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitKeyword(node);
                }
            }
            super.visitKeyword(node);
        }

        @Override
        public void visitLabeledStatement(PsiLabeledStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiLabeledStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitLabeledStatement(node);
                }
            }
            super.visitLabeledStatement(node);
        }

        @Override
        public void visitLiteralExpression(PsiLiteralExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiLiteralExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitLiteralExpression(node);
                }
            }
            super.visitLiteralExpression(node);
        }

        @Override
        public void visitLocalVariable(PsiLocalVariable node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiLocalVariable.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitLocalVariable(node);
                }
            }
            super.visitLocalVariable(node);
        }

        @Override
        public void visitMethod(PsiMethod node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiMethod.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitMethod(node);
                }
            }
            super.visitMethod(node);
        }

        @Override
        public void visitMethodCallExpression(PsiMethodCallExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiMethodCallExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitMethodCallExpression(node);
                }
            }
            super.visitMethodCallExpression(node);
        }

        @Override
        public void visitCallExpression(PsiCallExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiCallExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitCallExpression(node);
                }
            }
            super.visitCallExpression(node);
        }

        @Override
        public void visitModifierList(PsiModifierList node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiModifierList.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitModifierList(node);
                }
            }
            super.visitModifierList(node);
        }

        @Override
        public void visitNewExpression(PsiNewExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiNewExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitNewExpression(node);
                }
            }
            super.visitNewExpression(node);
        }

        @Override
        public void visitPackage(PsiPackage node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiPackage.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitPackage(node);
                }
            }
            super.visitPackage(node);
        }

        @Override
        public void visitPackageStatement(PsiPackageStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiPackageStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitPackageStatement(node);
                }
            }
            super.visitPackageStatement(node);
        }

        @Override
        public void visitParameter(PsiParameter node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiParameter.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitParameter(node);
                }
            }
            super.visitParameter(node);
        }

        @Override
        public void visitReceiverParameter(PsiReceiverParameter node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiReceiverParameter.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitReceiverParameter(node);
                }
            }
            super.visitReceiverParameter(node);
        }

        @Override
        public void visitParameterList(PsiParameterList node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiParameterList.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitParameterList(node);
                }
            }
            super.visitParameterList(node);
        }

        @Override
        public void visitParenthesizedExpression(PsiParenthesizedExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors
                    .get(PsiParenthesizedExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitParenthesizedExpression(node);
                }
            }
            super.visitParenthesizedExpression(node);
        }

        @Override
        public void visitPostfixExpression(PsiPostfixExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiPostfixExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitPostfixExpression(node);
                }
            }
            super.visitPostfixExpression(node);
        }

        @Override
        public void visitPrefixExpression(PsiPrefixExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiPrefixExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitPrefixExpression(node);
                }
            }
            super.visitPrefixExpression(node);
        }

        @Override
        public void visitReferenceElement(PsiJavaCodeReferenceElement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors
                    .get(PsiJavaCodeReferenceElement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitReferenceElement(node);
                }
            }
            super.visitReferenceElement(node);
        }

        @Override
        public void visitImportStaticReferenceElement(PsiImportStaticReferenceElement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors
                    .get(PsiImportStaticReferenceElement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitImportStaticReferenceElement(node);
                }
            }
            super.visitImportStaticReferenceElement(node);
        }

        @Override
        public void visitReferenceExpression(PsiReferenceExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiReferenceExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitReferenceExpression(node);
                }
            }
            super.visitReferenceExpression(node);
        }

        @Override
        public void visitMethodReferenceExpression(PsiMethodReferenceExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors
                    .get(PsiMethodReferenceExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitMethodReferenceExpression(node);
                }
            }
            super.visitMethodReferenceExpression(node);
        }

        @Override
        public void visitReferenceList(PsiReferenceList node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiReferenceList.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitReferenceList(node);
                }
            }
            super.visitReferenceList(node);
        }

        @Override
        public void visitReferenceParameterList(PsiReferenceParameterList node) {
            List<VisitingDetector> list = nodePsiTypeDetectors
                    .get(PsiReferenceParameterList.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitReferenceParameterList(node);
                }
            }
            super.visitReferenceParameterList(node);
        }

        @Override
        public void visitTypeParameterList(PsiTypeParameterList node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiTypeParameterList.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitTypeParameterList(node);
                }
            }
            super.visitTypeParameterList(node);
        }

        @Override
        public void visitReturnStatement(PsiReturnStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiReturnStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitReturnStatement(node);
                }
            }
            super.visitReturnStatement(node);
        }

        @Override
        public void visitStatement(PsiStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitStatement(node);
                }
            }
            super.visitStatement(node);
        }

        @Override
        public void visitSuperExpression(PsiSuperExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiSuperExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitSuperExpression(node);
                }
            }
            super.visitSuperExpression(node);
        }

        @Override
        public void visitSwitchLabelStatement(PsiSwitchLabelStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiSwitchLabelStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitSwitchLabelStatement(node);
                }
            }
            super.visitSwitchLabelStatement(node);
        }

        @Override
        public void visitSwitchStatement(PsiSwitchStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiSwitchStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitSwitchStatement(node);
                }
            }
            super.visitSwitchStatement(node);
        }

        @Override
        public void visitSynchronizedStatement(PsiSynchronizedStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiSynchronizedStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitSynchronizedStatement(node);
                }
            }
            super.visitSynchronizedStatement(node);
        }

        @Override
        public void visitThisExpression(PsiThisExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiThisExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitThisExpression(node);
                }
            }
            super.visitThisExpression(node);
        }

        @Override
        public void visitThrowStatement(PsiThrowStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiThrowStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitThrowStatement(node);
                }
            }
            super.visitThrowStatement(node);
        }

        @Override
        public void visitTryStatement(PsiTryStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiTryStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitTryStatement(node);
                }
            }
            super.visitTryStatement(node);
        }

        @Override
        public void visitCatchSection(PsiCatchSection node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiCatchSection.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitCatchSection(node);
                }
            }
            super.visitCatchSection(node);
        }

        @Override
        public void visitResourceList(PsiResourceList node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiResourceList.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitResourceList(node);
                }
            }
            super.visitResourceList(node);
        }

        @Override
        public void visitResourceVariable(PsiResourceVariable node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiResourceVariable.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitResourceVariable(node);
                }
            }
            super.visitResourceVariable(node);
        }

        @Override
        public void visitTypeElement(PsiTypeElement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiTypeElement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitTypeElement(node);
                }
            }
            super.visitTypeElement(node);
        }

        @Override
        public void visitTypeCastExpression(PsiTypeCastExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiTypeCastExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitTypeCastExpression(node);
                }
            }
            super.visitTypeCastExpression(node);
        }

        @Override
        public void visitVariable(PsiVariable node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiVariable.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitVariable(node);
                }
            }
            super.visitVariable(node);
        }

        @Override
        public void visitWhileStatement(PsiWhileStatement node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiWhileStatement.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitWhileStatement(node);
                }
            }
            super.visitWhileStatement(node);
        }

        @Override
        public void visitJavaFile(PsiJavaFile node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiJavaFile.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitJavaFile(node);
                }
            }
            super.visitJavaFile(node);
        }

        @Override
        public void visitImplicitVariable(ImplicitVariable node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(ImplicitVariable.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitImplicitVariable(node);
                }
            }
            super.visitImplicitVariable(node);
        }

        @Override
        public void visitDocToken(PsiDocToken node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiDocToken.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitDocToken(node);
                }
            }
            super.visitDocToken(node);
        }

        @Override
        public void visitTypeParameter(PsiTypeParameter node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiTypeParameter.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitTypeParameter(node);
                }
            }
            super.visitTypeParameter(node);
        }

        @Override
        public void visitAnnotation(PsiAnnotation node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiAnnotation.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitAnnotation(node);
                }
            }
            super.visitAnnotation(node);
        }

        @Override
        public void visitAnnotationParameterList(PsiAnnotationParameterList node) {
            List<VisitingDetector> list = nodePsiTypeDetectors
                    .get(PsiAnnotationParameterList.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitAnnotationParameterList(node);
                }
            }
            super.visitAnnotationParameterList(node);
        }

        @Override
        public void visitAnnotationArrayInitializer(PsiArrayInitializerMemberValue node) {
            List<VisitingDetector> list = nodePsiTypeDetectors
                    .get(PsiArrayInitializerMemberValue.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitAnnotationArrayInitializer(node);
                }
            }
            super.visitAnnotationArrayInitializer(node);
        }

        @Override
        public void visitNameValuePair(PsiNameValuePair node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiNameValuePair.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitNameValuePair(node);
                }
            }
            super.visitNameValuePair(node);
        }

        @Override
        public void visitAnnotationMethod(PsiAnnotationMethod node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiAnnotationMethod.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitAnnotationMethod(node);
                }
            }
            super.visitAnnotationMethod(node);
        }

        @Override
        public void visitEnumConstant(PsiEnumConstant node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiEnumConstant.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitEnumConstant(node);
                }
            }
            super.visitEnumConstant(node);
        }

        @Override
        public void visitEnumConstantInitializer(PsiEnumConstantInitializer node) {
            List<VisitingDetector> list = nodePsiTypeDetectors
                    .get(PsiEnumConstantInitializer.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitEnumConstantInitializer(node);
                }
            }
            super.visitEnumConstantInitializer(node);
        }

        @Override
        public void visitPolyadicExpression(PsiPolyadicExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiPolyadicExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitPolyadicExpression(node);
                }
            }
            super.visitPolyadicExpression(node);
        }

        @Override
        public void visitLambdaExpression(PsiLambdaExpression node) {
            List<VisitingDetector> list = nodePsiTypeDetectors.get(PsiLambdaExpression.class);
            if (list != null) {
                for (VisitingDetector v : list) {
                    v.getVisitor().visitLambdaExpression(node);
                }
            }
            super.visitLambdaExpression(node);
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

        public DelegatingPsiVisitor(JavaContext context) {
            mContext = context;

            mVisitMethods = !methodDetectors.isEmpty();
            mVisitConstructors = !constructorDetectors.isEmpty();
            mVisitResources = !resourceFieldDetectors.isEmpty();
            mVisitReferences = !referenceDetectors.isEmpty();
        }

        @Override
        public void visitReferenceElement(PsiJavaCodeReferenceElement element) {
            if (mVisitReferences) {
                String name = element.getReferenceName();
                if (name != null) {
                    List<VisitingDetector> list = referenceDetectors.get(name);
                    if (list != null) {
                        PsiElement referenced = element.resolve();
                        if (referenced != null) {
                            for (VisitingDetector v : list) {
                                JavaPsiScanner javaPsiScanner = v.getJavaScanner();
                                if (javaPsiScanner != null) {
                                    javaPsiScanner.visitReference(mContext, v.getVisitor(),
                                            element, referenced);
                                }
                            }
                        }
                    }
                }
            }

            super.visitReferenceElement(element);
        }

        @Override
        public void visitReferenceExpression(PsiReferenceExpression node) {
            if (mVisitResources) {
                // R.type.name
                PsiElement qualifier = node.getQualifier();
                if (qualifier instanceof PsiReferenceExpression) {
                    PsiReferenceExpression select = (PsiReferenceExpression)qualifier;
                    if (select.getQualifier() instanceof PsiReferenceExpression) {
                        PsiReferenceExpression reference = (PsiReferenceExpression) select.getQualifier();
                        if (R_CLASS.equals(reference.getReferenceName())) {
                            String typeName = select.getReferenceName();
                            String name = node.getReferenceName();

                            ResourceType type = ResourceType.getEnum(typeName);
                            if (type != null) {
                                boolean isFramework =
                                        reference.getQualifier() instanceof PsiReferenceExpression
                                        && ANDROID_PKG.equals(((PsiReferenceExpression)reference.
                                                getQualifier()).getReferenceName());

                                for (VisitingDetector v : resourceFieldDetectors) {
                                    JavaPsiScanner detector = v.getJavaScanner();
                                    if (detector != null) {
                                        //noinspection ConstantConditions
                                        detector.visitResourceReference(mContext, v.getVisitor(),
                                                node, type, name, isFramework);
                                    }
                                }
                            }
                        }
                    }
                }

                // Arbitrary packages -- android.R.type.name, foo.bar.R.type.name
                if (qualifier != null && R_CLASS.equals(node.getReferenceName())) {
                    PsiElement parent = node.getParent();
                    if (parent instanceof PsiReferenceExpression) {
                        PsiElement grandParent = parent.getParent();
                        if (grandParent instanceof PsiReferenceExpression) {
                            PsiReferenceExpression select = (PsiReferenceExpression) grandParent;
                            String name = select.getReferenceName();
                            PsiElement typeOperand = select.getQualifier();
                            if (name != null && typeOperand instanceof PsiReferenceExpression) {
                                PsiReferenceExpression typeSelect =
                                        (PsiReferenceExpression) typeOperand;
                                String typeName = typeSelect.getReferenceName();
                                ResourceType type = typeName != null
                                        ? ResourceType.getEnum(typeName)
                                        : null;
                                if (type != null) {
                                    boolean isFramework = qualifier.textMatches(ANDROID_PKG);
                                    for (VisitingDetector v : resourceFieldDetectors) {
                                        JavaPsiScanner detector = v.getJavaScanner();
                                        if (detector != null) {
                                            detector.visitResourceReference(mContext,
                                                    v.getVisitor(),
                                                    node, type, name, isFramework);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            super.visitReferenceExpression(node);
        }

        @Override
        public void visitMethodCallExpression(PsiMethodCallExpression node) {
            super.visitMethodCallExpression(node);

            if (mVisitMethods) {
                String methodName = node.getMethodExpression().getReferenceName();
                if (methodName != null) {
                    List<VisitingDetector> list = methodDetectors.get(methodName);
                    if (list != null) {
                        PsiMethod method = node.resolveMethod();
                        if (method != null) {
                            for (VisitingDetector v : list) {
                                JavaPsiScanner javaPsiScanner = v.getJavaScanner();
                                if (javaPsiScanner != null) {
                                    javaPsiScanner.visitMethod(mContext, v.getVisitor(), node,
                                            method);
                                }
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void visitNewExpression(PsiNewExpression node) {
            super.visitNewExpression(node);

            if (mVisitConstructors) {
                PsiJavaCodeReferenceElement typeReference = node.getClassReference();
                if (typeReference != null) {
                    String type = typeReference.getQualifiedName();
                    if (type != null) {
                        List<VisitingDetector> list = constructorDetectors.get(type);
                        if (list != null) {
                            PsiMethod method = node.resolveMethod();
                            if (method != null) {
                                for (VisitingDetector v : list) {
                                    JavaPsiScanner javaPsiScanner = v.getJavaScanner();
                                    if (javaPsiScanner != null) {
                                        javaPsiScanner.visitConstructor(mContext,
                                                v.getVisitor(), node, method);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
