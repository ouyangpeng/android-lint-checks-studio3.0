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
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Location;
import com.google.common.annotations.Beta;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import java.io.File;
import java.util.List;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UFile;
import org.jetbrains.uast.UastContext;

/**
 * A wrapper for a UAST parser. This allows tools integrating lint to map directly
 * to builtin services, such as already-parsed data structures in Java editors.
 * <p>
 * <b>NOTE: This is not public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public abstract class UastParser {
    /**
     * Prepare to parse the given contexts. This method will be called before
     * a series of {@link #parse(JavaContext)} calls, which allows some
     * parsers to do up front global computation in case they want to more
     * efficiently process multiple files at the same time. This allows a single
     * type-attribution pass for example, which is a lot more efficient than
     * performing global type analysis over and over again for each individual
     * file
     *
     * @param contexts a list of contexts to be parsed
     * @return true if the preparation succeeded; false if there were errors
     */
    public boolean prepare(@NonNull List<? extends JavaContext> contexts) {
        return true;
    }

    /**
     * Returns an evaluator which can perform various resolution tasks,
     * evaluate inheritance lookup etc.
     *
     * @return an evaluator
     */
    @NonNull
    public abstract JavaEvaluator getEvaluator();

    /**
     * Parse the file pointed to by the given context.
     *
     * @param context the context pointing to the file to be parsed, typically via {@link
     *                Context#getContents()} but the file handle ( {@link Context#file} can also be
     *                used to map to an existing editor buffer in the surrounding tool, etc)
     * @return the compilation unit node for the file
     */
    @Nullable
    public abstract UFile parse(@NonNull JavaContext context);

    /**
     * Returns a UastContext which can provide UAST representations for source files
     */
    @Nullable
    public abstract UastContext getUastContext();

    /**
     * Returns a {@link Location} for the given element
     *
     * @param context information about the file being parsed
     * @param element the element to create a location for
     * @return a location for the given node
     */
    @NonNull
    public abstract Location getLocation(@NonNull JavaContext context, @NonNull PsiElement element);

    @NonNull
    public abstract Location getLocation(@NonNull JavaContext context, @NonNull UElement element);

    @NonNull
    public abstract Location getCallLocation(@NonNull JavaContext context, @NonNull UCallExpression call,
            boolean includeReceiver, boolean includeArguments);

    @Nullable
    public abstract File getFile(@NonNull PsiFile file);

    @NonNull
    public abstract CharSequence getFileContents(@NonNull PsiFile file);

    @NonNull
    public abstract Location createLocation(@NonNull PsiElement element);

    @NonNull
    public abstract Location createLocation(@NonNull UElement element);

    /**
     * Returns a {@link Location} for the given node range (from the starting offset of the first
     * node to the ending offset of the second node).
     *
     * @param context   information about the file being parsed
     * @param from      the AST node to get a starting location from
     * @param fromDelta Offset delta to apply to the starting offset
     * @param to        the AST node to get a ending location from
     * @param toDelta   Offset delta to apply to the ending offset
     * @return a location for the given node
     */
    @NonNull
    public abstract Location getRangeLocation(@NonNull JavaContext context, @NonNull PsiElement from,
            int fromDelta, @NonNull PsiElement to, int toDelta);

    @NonNull
    public abstract Location getRangeLocation(@NonNull JavaContext context, @NonNull UElement from,
            int fromDelta, @NonNull UElement to, int toDelta);

    /**
     * Like {@link #getRangeLocation(JavaContext, PsiElement, int, PsiElement, int)}
     * but both offsets are relative to the starting offset of the given node. This is
     * sometimes more convenient than operating relative to the ending offset when you
     * have a fixed range in mind.
     *
     * @param context   information about the file being parsed
     * @param from      the AST node to get a starting location from
     * @param fromDelta Offset delta to apply to the starting offset
     * @param toDelta   Offset delta to apply to the starting offset
     * @return a location for the given node
     */
    @SuppressWarnings("MethodMayBeStatic") // subclasses may want to override/optimize
    @NonNull
    public abstract Location getRangeLocation(@NonNull JavaContext context, @NonNull PsiElement from,
            int fromDelta, int toDelta);

    @NonNull
    public abstract Location getRangeLocation(@NonNull JavaContext context, @NonNull UElement from,
            int fromDelta, int toDelta);

    /**
     * Returns a {@link Location} for the given node. This attempts to pick a shorter
     * location range than the entire node; for a class or method for example, it picks
     * the name node (if found). For statement constructs such as a {@code switch} statement
     * it will highlight the keyword, etc.
     *
     * @param context information about the file being parsed
     * @param element the node to create a location for
     * @return a location for the given node
     */
    @NonNull
    public abstract Location getNameLocation(@NonNull JavaContext context, @NonNull PsiElement element) ;

    @NonNull
    public abstract Location getNameLocation(@NonNull JavaContext context, @NonNull UElement element);

    /**
     * Dispose any data structures held for the given context.
     *
     * @param context         information about the file previously parsed
     * @param compilationUnit the compilation unit being disposed
     */
    public void dispose(@NonNull JavaContext context, @NonNull UFile compilationUnit) {
    }

    /**
     * Dispose any remaining data structures held for all contexts.
     * Typically frees up any resources allocated by
     * {@link #prepare(List)}
     */
    public void dispose() {
    }
}
