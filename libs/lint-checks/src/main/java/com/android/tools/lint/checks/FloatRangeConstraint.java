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

import static com.android.tools.lint.checks.PermissionRequirement.getAnnotationBooleanValue;
import static com.android.tools.lint.checks.PermissionRequirement.getAnnotationDoubleValue;
import static com.android.tools.lint.checks.SupportAnnotationDetector.ATTR_FROM;
import static com.android.tools.lint.checks.SupportAnnotationDetector.ATTR_FROM_INCLUSIVE;
import static com.android.tools.lint.checks.SupportAnnotationDetector.ATTR_TO;
import static com.android.tools.lint.checks.SupportAnnotationDetector.ATTR_TO_INCLUSIVE;
import static com.android.tools.lint.checks.SupportAnnotationDetector.FLOAT_RANGE_ANNOTATION;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.ULiteralExpression;

class FloatRangeConstraint extends RangeConstraint {

    final double from;
    final double to;
    final boolean fromInclusive;
    final boolean toInclusive;

    @NonNull
    public static FloatRangeConstraint create(@NonNull UAnnotation annotation) {
        assert FLOAT_RANGE_ANNOTATION.equals(annotation.getQualifiedName());
        double from = getAnnotationDoubleValue(annotation, ATTR_FROM, Double.NEGATIVE_INFINITY);
        double to = getAnnotationDoubleValue(annotation, ATTR_TO, Double.POSITIVE_INFINITY);
        boolean fromInclusive = getAnnotationBooleanValue(annotation, ATTR_FROM_INCLUSIVE, true);
        boolean toInclusive = getAnnotationBooleanValue(annotation, ATTR_TO_INCLUSIVE, true);
        return new FloatRangeConstraint(from, to, fromInclusive, toInclusive);
    }

    @VisibleForTesting
    static FloatRangeConstraint range(double from, double to) {
        return new FloatRangeConstraint(from, to, true, true);
    }

    @VisibleForTesting
    static FloatRangeConstraint atLeast(double from) {
        return new FloatRangeConstraint(from, Double.POSITIVE_INFINITY, true, true);
    }

    @VisibleForTesting
    static FloatRangeConstraint atMost(double to) {
        return new FloatRangeConstraint(Double.NEGATIVE_INFINITY, to, true, true);
    }

    @VisibleForTesting
    static FloatRangeConstraint greaterThan(double from) {
        return new FloatRangeConstraint(from, Double.POSITIVE_INFINITY, false, true);
    }

    @VisibleForTesting
    static FloatRangeConstraint lessThan(double to) {
        return new FloatRangeConstraint(Double.NEGATIVE_INFINITY, to, true, false);
    }

    @VisibleForTesting
    FloatRangeConstraint(double from, double to, boolean fromInclusive, boolean toInclusive) {
        this.from = from;
        this.to = to;
        this.fromInclusive = fromInclusive;
        this.toInclusive = toInclusive;
    }

    public boolean isValid(double value) {
        return (fromInclusive && value >= from || !fromInclusive && value > from) &&
                (toInclusive && value <= to || !toInclusive && value < to);
    }

    @NonNull
    public String describe() {
        return describe(null, null);
    }

    @NonNull
    public String describe(double argument) {
        return describe(null, argument);
    }

    @NonNull
    public String describe(@Nullable UExpression argument, @Nullable Double actualValue) {
        StringBuilder sb = new StringBuilder(20);

        String valueString = null;
        if (argument instanceof ULiteralExpression) {
            // Use source text instead to avoid rounding errors involved in conversion, e.g
            //    Error: Value must be > 2.5 (was 2.490000009536743) [Range]
            //    printAtLeastExclusive(2.49f); // ERROR
            //                          ~~~~~
            String str = argument.asSourceString();
            if (str.endsWith("f") || str.endsWith("F")) {
                str = str.substring(0, str.length() - 1);
            }
            valueString = str;
        } else if (actualValue != null) {
            valueString = actualValue.toString();
        }

        // If we have an actual value, don't describe the full range, only describe
        // the parts that are outside the range
        if (actualValue != null && !isValid(actualValue)) {
            double value = actualValue;
            if (from != Double.NEGATIVE_INFINITY) {
                if (to != Double.POSITIVE_INFINITY) {
                    if (fromInclusive && value < from || !fromInclusive && value <= from) {
                        sb.append("Value must be ");
                        if (fromInclusive) {
                            sb.append('\u2265'); // >= sign
                        } else {
                            sb.append('>');
                        }
                        sb.append(' ');
                        sb.append(Double.toString(from));
                    } else {
                        assert toInclusive && value > to || !toInclusive && value >= to;
                        sb.append("Value must be ");
                        if (toInclusive) {
                            sb.append('\u2264'); // <= sign
                        } else {
                            sb.append('<');
                        }
                        sb.append(' ');
                        sb.append(Double.toString(to));
                    }
                } else {
                    sb.append("Value must be ");
                    if (fromInclusive) {
                        sb.append('\u2265'); // >= sign
                    } else {
                        sb.append('>');
                    }
                    sb.append(' ');
                    sb.append(Double.toString(from));
                }
            } else if (to != Double.POSITIVE_INFINITY) {
                sb.append("Value must be ");
                if (toInclusive) {
                    sb.append('\u2264'); // <= sign
                } else {
                    sb.append('<');
                }
                sb.append(' ');
                sb.append(Double.toString(to));
            }
            sb.append(" (was ").append(valueString).append(")");
            return sb.toString();
        }

        if (from != Double.NEGATIVE_INFINITY) {
            if (to != Double.POSITIVE_INFINITY) {
                sb.append("Value must be ");
                if (fromInclusive) {
                    sb.append('\u2265'); // >= sign
                } else {
                    sb.append('>');
                }
                sb.append(' ');
                sb.append(Double.toString(from));
                sb.append(" and ");
                if (toInclusive) {
                    sb.append('\u2264'); // <= sign
                } else {
                    sb.append('<');
                }
                sb.append(' ');
                sb.append(Double.toString(to));
            } else {
                sb.append("Value must be ");
                if (fromInclusive) {
                    sb.append('\u2265'); // >= sign
                } else {
                    sb.append('>');
                }
                sb.append(' ');
                sb.append(Double.toString(from));
            }
        } else if (to != Double.POSITIVE_INFINITY) {
            sb.append("Value must be ");
            if (toInclusive) {
                sb.append('\u2264'); // <= sign
            } else {
                sb.append('<');
            }
            sb.append(' ');
            sb.append(Double.toString(to));
        }

        if (valueString != null) {
            sb.append(" (is ").append(valueString).append(')');
        }
        return sb.toString();
    }

    @Nullable
    @Override
    public Boolean contains(@NonNull RangeConstraint other) {
        if (other instanceof FloatRangeConstraint) {
            FloatRangeConstraint otherRange = (FloatRangeConstraint) other;
            return !(otherRange.from < from || otherRange.to > to) && !(!fromInclusive
                    && otherRange.fromInclusive && otherRange.from == from) && !(!toInclusive
                    && otherRange.toInclusive && otherRange.to == to);
        } else if (other instanceof IntRangeConstraint) {
            IntRangeConstraint otherRange = (IntRangeConstraint) other;
            return !(otherRange.from < from || otherRange.to > to) && !(!fromInclusive
                    && otherRange.from == from) && !(!toInclusive && otherRange.to == to);
        }
        return null;
    }

    @Override
    public String toString() {
        return describe(null, null);
    }
}
