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

import static com.android.SdkConstants.ATTR_VALUE;
import static com.android.tools.lint.checks.PermissionRequirement.getAnnotationLongValue;
import static com.android.tools.lint.checks.SupportAnnotationDetector.ATTR_MAX;
import static com.android.tools.lint.checks.SupportAnnotationDetector.ATTR_MIN;
import static com.android.tools.lint.checks.SupportAnnotationDetector.ATTR_MULTIPLE;
import static com.android.tools.lint.checks.SupportAnnotationDetector.SIZE_ANNOTATION;
import static com.intellij.psi.CommonClassNames.JAVA_LANG_STRING;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UExpression;

class SizeConstraint extends RangeConstraint {

    final long exact;
    final long min;
    final long max;
    final long multiple;

    @NonNull
    public static SizeConstraint create(@NonNull UAnnotation annotation) {
        assert SIZE_ANNOTATION.equals(annotation.getQualifiedName());
        long exact = getAnnotationLongValue(annotation, ATTR_VALUE, -1);
        long min = getAnnotationLongValue(annotation, ATTR_MIN, Long.MIN_VALUE);
        long max = getAnnotationLongValue(annotation, ATTR_MAX, Long.MAX_VALUE);
        long multiple = getAnnotationLongValue(annotation, ATTR_MULTIPLE, 1);
        return new SizeConstraint(exact, min, max, multiple);
    }

    @VisibleForTesting
    @NonNull
    static SizeConstraint exactly(long value) {
        return new SizeConstraint(value, Long.MIN_VALUE, Long.MAX_VALUE, 1);
    }

    @VisibleForTesting
    @NonNull
    static SizeConstraint atLeast(long value) {
        return new SizeConstraint(-1, value, Long.MAX_VALUE, 1);
    }

    @VisibleForTesting
    @NonNull
    static SizeConstraint atMost(long value) {
        return new SizeConstraint(-1, Long.MIN_VALUE, value, 1);
    }

    @VisibleForTesting
    @NonNull
    static SizeConstraint range(long from, long to) {
        return new SizeConstraint(-1, from, to, 1);
    }

    @VisibleForTesting
    @NonNull
    static SizeConstraint multiple(int multiple) {
        return new SizeConstraint(-1, Long.MIN_VALUE, Long.MAX_VALUE, multiple);
    }

    @VisibleForTesting
    @NonNull
    static SizeConstraint rangeWithMultiple(long from, long to, int multiple) {
        return new SizeConstraint(-1, from, to, multiple);
    }

    @VisibleForTesting
    @NonNull
    static SizeConstraint minWithMultiple(long from, int multiple) {
        return new SizeConstraint(-1, from, Long.MAX_VALUE, multiple);
    }

    private SizeConstraint(long exact, long min, long max, long multiple) {
        this.exact = exact;
        this.min = min;
        this.max = max;
        this.multiple = multiple;
    }

    @Override
    public String toString() {
        return describe(null, null, null);
    }

    public boolean isValid(long actual) {
        if (exact != -1) {
            if (exact != actual) {
                return false;
            }
        } else if (actual < min || actual > max || actual % multiple != 0) {
            return false;
        }

        return true;
    }

    @NonNull
    public String describe() {
        return describe(null, null, null);
    }

    @NonNull
    public String describe(long argument) {
        return describe(null, null, argument);
    }

    @NonNull
    public String describe(@Nullable UExpression argument, @Nullable String unit,
            @Nullable Long actualValue) {
        if (unit == null) {
            if (argument != null && argument.getExpressionType() != null
                    && argument.getExpressionType().getCanonicalText().equals(JAVA_LANG_STRING)) {
                unit = "Length";
            } else {
                unit = "Size";
            }
        }

        if (actualValue != null && !isValid(actualValue)) {
            long actual = actualValue;
            if (exact != -1) {
                if (exact != actual) {
                    return String.format("Expected %1$s %2$d (was %3$d)",
                            unit, exact, actual);
                }
            } else if (actual < min || actual > max) {
                StringBuilder sb = new StringBuilder(20);
                if (actual < min) {
                    sb.append("Expected ").append(unit).append(" \u2265 ");
                    sb.append(Long.toString(min));
                } else {
                    assert actual > max;
                    sb.append("Expected ").append(unit).append(" \u2264 ");
                    sb.append(Long.toString(max));
                }
                sb.append(" (was ").append(actual).append(')');
                return sb.toString();
            } else if (actual % multiple != 0) {
                return String.format("Expected %1$s to be a multiple of %2$d (was %3$d "
                                + "and should be either %4$d or %5$d)",
                        unit, multiple, actual, (actual / multiple) * multiple,
                        (actual / multiple + 1) * multiple);
            }
        }

        StringBuilder sb = new StringBuilder(20);
        sb.append(unit);
        sb.append(" must be");
        if (exact != -1) {
            sb.append(" exactly ");
            sb.append(Long.toString(exact));
            return sb.toString();
        }
        boolean continued = true;
        if (min != Long.MIN_VALUE && max != Long.MAX_VALUE) {
            sb.append(" at least ");
            sb.append(Long.toString(min));
            sb.append(" and at most ");
            sb.append(Long.toString(max));
        } else if (min != Long.MIN_VALUE) {
            sb.append(" at least ");
            sb.append(Long.toString(min));
        } else if (max != Long.MAX_VALUE) {
            sb.append(" at most ");
            sb.append(Long.toString(max));
        } else {
            continued = false;
        }
        if (multiple != 1) {
            if (continued) {
                sb.append(" and");
            }
            sb.append(" a multiple of ");
            sb.append(Long.toString(multiple));
        }
        if (actualValue != null) {
            sb.append(" (was ").append(actualValue).append(')');
        }
        return sb.toString();
    }

    @Nullable
    @Override
    public Boolean contains(@NonNull RangeConstraint other) {
        if (other instanceof SizeConstraint) {
            SizeConstraint otherRange = (SizeConstraint) other;
            if (exact != -1 && otherRange.exact != -1) {
                return exact == otherRange.exact;
            }
            if (multiple != 1) {
                if (otherRange.exact != -1) {
                    if (otherRange.exact % multiple != 0) {
                        return false;
                    }
                } else if (otherRange.multiple % multiple != 0) {
                    return false;
                }
            }
            if (otherRange.exact != -1) {
                return otherRange.exact >= min && otherRange.exact <= max;
            }
            return otherRange.min >= min && otherRange.max <= max;
        }
        return null;
    }
}
