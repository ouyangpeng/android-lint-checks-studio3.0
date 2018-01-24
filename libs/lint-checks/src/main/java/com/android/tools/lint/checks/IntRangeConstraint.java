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

import static com.android.tools.lint.checks.PermissionRequirement.getAnnotationLongValue;
import static com.android.tools.lint.checks.SupportAnnotationDetector.ATTR_FROM;
import static com.android.tools.lint.checks.SupportAnnotationDetector.ATTR_TO;
import static com.android.tools.lint.checks.SupportAnnotationDetector.INT_RANGE_ANNOTATION;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import org.jetbrains.uast.UAnnotation;

class IntRangeConstraint extends RangeConstraint {

    final long from;
    final long to;

    @NonNull
    public static IntRangeConstraint create(@NonNull UAnnotation annotation) {
        assert INT_RANGE_ANNOTATION.equals(annotation.getQualifiedName());
        long from = getAnnotationLongValue(annotation, ATTR_FROM, Long.MIN_VALUE);
        long to = getAnnotationLongValue(annotation, ATTR_TO, Long.MAX_VALUE);
        return new IntRangeConstraint(from, to);
    }

    @VisibleForTesting
    static IntRangeConstraint atLeast(long value) {
        return new IntRangeConstraint(value, Long.MAX_VALUE);
    }

    @VisibleForTesting
    static IntRangeConstraint atMost(long value) {
        return new IntRangeConstraint(Long.MIN_VALUE, value);
    }

    @VisibleForTesting
    static IntRangeConstraint range(long from, long to) {
        return new IntRangeConstraint(from, to);
    }

    private IntRangeConstraint(long from, long to) {
        this.from = from;
        this.to = to;
    }

    public boolean isValid(long value) {
        return value >= from && value <= to;
    }

    @NonNull
    public String describe() {
        return describe(null);
    }

    @NonNull
    public String describe(long argument) {
        return describe(Long.valueOf(argument));
    }

    @NonNull
    private String describe(@Nullable Long actualValue) {
        StringBuilder sb = new StringBuilder(20);

        // If we have an actual value, don't describe the full range, only describe
        // the parts that are outside the range
        if (actualValue != null && !isValid(actualValue)) {
            long value = actualValue;
            if (value < from) {
                sb.append("Value must be \u2265 ");
                sb.append(Long.toString(from));
            } else {
                assert value > to;
                sb.append("Value must be \u2264 ");
                sb.append(Long.toString(to));
            }
            sb.append(" (was ").append(value).append(')');
            return sb.toString();
        }

        if (to == Long.MAX_VALUE) {
            sb.append("Value must be \u2265 ");
            sb.append(Long.toString(from));
        } else if (from == Long.MIN_VALUE) {
            sb.append("Value must be \u2264 ");
            sb.append(Long.toString(to));
        } else {
            sb.append("Value must be \u2265 ");
            sb.append(Long.toString(from));
            sb.append(" and \u2264 ");
            sb.append(Long.toString(to));
        }

        if (actualValue != null) {
            sb.append(" (is ").append(actualValue).append(')');
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return describe(null);
    }

    @Nullable
    @Override
    public Boolean contains(@NonNull RangeConstraint other) {
        if (other instanceof IntRangeConstraint) {
            IntRangeConstraint otherRange = (IntRangeConstraint) other;
            return otherRange.from >= from && otherRange.to <= to;
        } else if (other instanceof FloatRangeConstraint) {
            FloatRangeConstraint otherRange = (FloatRangeConstraint) other;
            if (!otherRange.fromInclusive && otherRange.from == (double) from
                    || !otherRange.toInclusive && otherRange.to == (double) to) {
                return false;
            }

            // Both represent infinity
            if (otherRange.to > to
                    && !(Double.isInfinite(otherRange.to) && to == Long.MAX_VALUE)) {
                return false;
            }

            //noinspection RedundantIfStatement
            if (otherRange.from < from
                    && !(Double.isInfinite(otherRange.from) && from == Long.MIN_VALUE)) {
                return false;
            }

            return true;
        }

        return null;
    }
}
