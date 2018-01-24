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
package com.android.tools.lint.checks;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.java.JavaUAnnotation;

public abstract class RangeConstraint {

    @Nullable
    public static RangeConstraint create(@NonNull UAnnotation annotation) {
        String qualifiedName = annotation.getQualifiedName();
        if (qualifiedName == null) {
            return null;
        }
        switch (qualifiedName) {
            case SupportAnnotationDetector.INT_RANGE_ANNOTATION:
                return IntRangeConstraint.create(annotation);
            case SupportAnnotationDetector.FLOAT_RANGE_ANNOTATION:
                return FloatRangeConstraint.create(annotation);
            case SupportAnnotationDetector.SIZE_ANNOTATION:
                return SizeConstraint.create(annotation);
            default:
                return null;
        }
    }

    @Nullable
    public static RangeConstraint create(@NonNull PsiModifierListOwner owner) {
        PsiModifierList modifierList = owner.getModifierList();
        if (modifierList != null) {
            for (PsiAnnotation annotation : modifierList.getAnnotations()) {
                RangeConstraint constraint = create(JavaUAnnotation.wrap(annotation));
                // Pick first; they're mutually exclusive
                if (constraint != null) {
                    return constraint;
                }
            }
        }

        return null;
    }

    /**
     * Checks whether the given range is compatible with this one.
     * We err on the side of caution. E.g. if we have
     * <pre>
     *    method(x)
     * </pre>
     * and the parameter declaration says that x is between 0 and 10,
     * and then we have a parameter which is known to be in the range 5 to 15,
     * here we consider this a compatible range; we don't flag this as
     * an error. If however, the ranges don't overlap, *then* we complain.
     */
    @Nullable
    public Boolean contains(@NonNull RangeConstraint other) {
        return null;
    }
}
