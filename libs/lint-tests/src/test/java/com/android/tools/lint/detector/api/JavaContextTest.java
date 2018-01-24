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

package com.android.tools.lint.detector.api;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.utils.Pair;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiCompiledFile;
import com.intellij.psi.PsiElement;
import java.io.File;
import junit.framework.TestCase;
import org.mockito.Mockito;

public class JavaContextTest extends TestCase {
    public void testSuppressedBytecode() throws Exception {
        // Regression test for https://issuetracker.google.com/issues/37335487
        String source = "package test.pkg;\npublic class Test { }\n";
        Pair<JavaContext, Disposable> pair =
                LintUtilsTest.parseUast(source, new File("src/test/pkg/Test.java"));
        JavaContext context = pair.getFirst();
        Disposable disposable = pair.getSecond();
        PsiElement compiled = Mockito.mock(PsiCompiledFile.class);
        Mockito.when(compiled.getTextRange()).thenReturn(null);
        //noinspection ResultOfMethodCallIgnored
        Mockito.doThrow(NullPointerException.class).when(compiled).getTextRange();
        assertTrue(!context.isSuppressedWithComment(compiled, IssueRegistry.LINT_ERROR));
        Disposer.dispose(disposable);
    }
}