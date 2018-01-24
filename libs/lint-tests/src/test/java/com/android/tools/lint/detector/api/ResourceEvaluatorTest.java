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

import com.android.resources.ResourceType;
import com.android.resources.ResourceUrl;
import com.android.utils.Pair;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLocalVariable;
import java.io.File;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicReference;
import junit.framework.TestCase;
import org.intellij.lang.annotations.Language;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UFile;
import org.jetbrains.uast.UVariable;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

@SuppressWarnings("ClassNameDiffersFromFileName")
public class ResourceEvaluatorTest extends TestCase {

    @Language("JAVA")
    private static String getString(String statementsSource) {
        @Language("JAVA")
        String s = ""
                + "package test.pkg;\n"
                + "public class Test {\n"
                + "    public void test() {\n"
                + "        " + statementsSource + "\n"
                + "    }\n"
                + "    public static final int RED = R.color.red;\n"
                + "    public static final int MY_COLOR = RED;\n"
                + "    public void someMethod(@android.support.annotation.DrawableRes @android.support.annotation.StringRes int param) { }\n"
                + "\n"
                + "    private static class R {\n"
                + "        private static class color {\n"
                + "            public static final int foo=0x7f050000;\n"
                + "        }\n"
                + "        private static class color {\n"
                + "            public static final int red=0x7f060000;\n"
                + "            public static final int green=0x7f060001;\n"
                + "            public static final int blue=0x7f060002;\n"
                + "        }\n"
                + "    }"
                + "}\n";
        return s;
    }

    private static void checkUast(String expected, String statementsSource,
            final String targetVariable, boolean getSpecificType, boolean allowDereference) {
        @Language("JAVA")
        String source = getString(statementsSource);
        Pair<JavaContext, Disposable> pair =
                LintUtilsTest.parseUast(source, new File("src/test/pkg/Test.java"));
        JavaContext context = pair.getFirst();
        Disposable disposable = pair.getSecond();
        assertNotNull(context);
        UFile uFile = context.getUastFile();
        assertNotNull(uFile);

        // Find the expression
        final AtomicReference<UExpression> reference = new AtomicReference<>();
        uFile.accept(new AbstractUastVisitor() {
            @Override
            public boolean visitVariable(UVariable variable) {
                String name = variable.getName();
                if (name != null && name.equals(targetVariable)) {
                    reference.set(variable.getUastInitializer());
                }

                return super.visitVariable(variable);
            }
        });

        UExpression expression = reference.get();
        ResourceEvaluator evaluator = new ResourceEvaluator(context.getEvaluator())
                .allowDereference(allowDereference);

        if (getSpecificType) {
            ResourceUrl actual = evaluator.getResource(expression);
            if (expected == null) {
                assertNull(actual);
            } else {
                assertNotNull("Couldn't compute resource for " + source + ", expected " + expected,
                        actual);

                assertEquals(expected, actual.toString());
            }
        } else {
            EnumSet<ResourceType> types = evaluator.getResourceTypes(expression);
            if (expected == null) {
                assertNull(types);
            } else {
                assertNotNull("Couldn't compute resource types for " + source
                        + ", expected " + expected, types);

                assertEquals(expected, types.toString());
            }
        }
        Disposer.dispose(disposable);
    }

    private static void check(String expected, String statementsSource,
            final String targetVariable, boolean getSpecificType, boolean allowDereference) {
        checkUast(expected, statementsSource, targetVariable, getSpecificType, allowDereference);

        @Language("JAVA")
        String source = getString(statementsSource);
        Pair<JavaContext, Disposable> pair =
                LintUtilsTest.parsePsi(source, new File("src/test/pkg/Test.java"));
        JavaContext context = pair.getFirst();
        Disposable disposable = pair.getSecond();
        assertNotNull(context);
        PsiFile javaFile = context.getPsiFile();
        assertNotNull(javaFile);

        // Find the expression
        final AtomicReference<PsiExpression> reference = new AtomicReference<>();
        javaFile.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitLocalVariable(PsiLocalVariable variable) {
                super.visitLocalVariable(variable);
                String name = variable.getName();
                if (name != null && name.equals(targetVariable)) {
                    reference.set(variable.getInitializer());
                }
            }
        });
        PsiExpression expression = reference.get();
        ResourceEvaluator evaluator = new ResourceEvaluator(context.getEvaluator())
                .allowDereference(allowDereference);

        if (getSpecificType) {
            ResourceUrl actual = evaluator.getResource(expression);
            if (expected == null) {
                assertNull(actual);
            } else {
                assertNotNull("Couldn't compute resource for " + source + ", expected " + expected,
                        actual);

                assertEquals(expected, actual.toString());
            }
        } else {
            EnumSet<ResourceType> types = evaluator.getResourceTypes(expression);
            if (expected == null) {
                assertNull(types);
            } else {
                assertNotNull("Couldn't compute resource types for " + source
                        + ", expected " + expected, types);

                assertEquals(expected, types.toString());
            }
        }
        Disposer.dispose(disposable);
    }

    private static void checkType(String expected, String statementsSource,
            final String targetVariable) {
        check(expected, statementsSource, targetVariable, true, true);
    }

    private static void checkTypes(String expected, String statementsSource,
            final String targetVariable) {
        check(expected, statementsSource, targetVariable, false, true);
    }

    private static void checkTypes(String expected, String statementsSource,
            final String targetVariable, boolean allowDereference) {
        check(expected, statementsSource, targetVariable, false, allowDereference);
    }

    public void test() throws Exception {
        checkType("@string/foo", "int x = R.string.foo", "x");
    }

    public void testIndirectFieldReference() throws Exception {
        checkType("@color/red", ""
                + "int z = RED;\n"
                + "int w = true ? z : 0",
                "w");
    }

    public void testMethodCall() throws Exception {
        checkType("@color/green", ""
                        + "android.app.Activity context = null;"
                        + "int w = context.getResources().getColor(R.color.green);",
                "w");
    }

    public void testMethodCallNoDereference() throws Exception {
        check(null, ""
                        + "android.app.Activity context = null;"
                        + "int w = context.getResources().getColor(R.color.green);",
                "w", true, false);
    }

    public void testReassignment() throws Exception {
        checkType("@string/foo", ""
                        + "int x = R.string.foo;\n"
                        + "int y = x;\n"
                        + "int w;\n"
                        + "w = -1;\n"
                        + "int z = y;\n",
                "z");
    }

    // Resource Types

    public void testReassignmentType() throws Exception {
        checkTypes("[string]", ""
                        + "int x = R.string.foo;\n"
                        + "int y = x;\n"
                        + "int w;\n"
                        + "w = -1;\n"
                        + "int z = y;\n",
                "z");
    }

    public void testMethodCallTypes() throws Exception {
        // public=color int marker
        checkTypes("[public]", ""
                        + "android.app.Activity context = null;"
                        + "int w = context.getResources().getColor(R.color.green);",
                "w");
    }

    public void testConditionalTypes() throws Exception {
        // Constant expression: we know exactly which branch to take
        checkTypes("[color]", ""
                        + "int z = RED;\n"
                        + "int w = true ? z : R.string.foo",
                "w");
    }

    public void testConditionalTypesUnknownCondition() throws Exception {
        // Constant expression: we know exactly which branch to take
        checkTypes("[color, string]", ""
                        + "int z = RED;\n"
                        + "int w = toString().indexOf('x') > 2 ? z : R.string.foo",
                "w");
    }

    public void testResourceTypes() {
        assertEquals(ResourceType.ANIM, ResourceEvaluator.getTypeFromAnnotationSignature(
                ResourceEvaluator.ANIM_RES_ANNOTATION));
        assertEquals(ResourceType.STRING, ResourceEvaluator.getTypeFromAnnotationSignature(
                ResourceEvaluator.STRING_RES_ANNOTATION));
        assertEquals(ResourceType.LAYOUT, ResourceEvaluator.getTypeFromAnnotationSignature(
                ResourceEvaluator.LAYOUT_RES_ANNOTATION));
    }
}
