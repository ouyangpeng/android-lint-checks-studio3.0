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

import static com.android.tools.lint.checks.FloatRangeConstraint.atLeast;
import static com.android.tools.lint.checks.FloatRangeConstraint.atMost;
import static com.android.tools.lint.checks.FloatRangeConstraint.greaterThan;
import static com.android.tools.lint.checks.FloatRangeConstraint.lessThan;
import static com.android.tools.lint.checks.FloatRangeConstraint.range;
import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class FloatRangeConstraintTest {
    @Test
    public void testDescribe() {
        assertThat(range(1, 5).describe()).isEqualTo("Value must be ≥ 1.0 and ≤ 5.0");
        assertThat(atLeast(5).describe()).isEqualTo("Value must be ≥ 5.0");
        assertThat(atMost(5).describe()).isEqualTo("Value must be ≤ 5.0");
        assertThat(lessThan(5).describe()).isEqualTo("Value must be < 5.0");
        assertThat(greaterThan(5).describe()).isEqualTo("Value must be > 5.0");

        assertThat(range(1, 5).describe(3)).isEqualTo("Value must be ≥ 1.0 and ≤ 5.0 (is 3.0)");
        assertThat(range(1, 5).describe(0)).isEqualTo("Value must be ≥ 1.0 (was 0.0)");
        assertThat(range(1, 5).describe(6)).isEqualTo("Value must be ≤ 5.0 (was 6.0)");
    }

    @Test
    public void testRangeContainsRange() {
        assertThat(range(1, 5).contains(range(2, 4))).isTrue();
        assertThat(range(1, 5).contains(range(2, 5))).isTrue();
        assertThat(range(1, 5).contains(range(2, 6))).isFalse();
        assertThat(range(1, 5).contains(range(1, 5))).isTrue();
        assertThat(range(1, 5).contains(range(0, 5))).isFalse();
    }

    @Test
    public void testMinContainsMin() {
        assertThat(atLeast(2).contains(atLeast(3))).isTrue();
        assertThat(atLeast(2).contains(atLeast(2))).isTrue();
        assertThat(atLeast(2).contains(atLeast(1))).isFalse();
    }

    @Test
    public void testMaxContainsMax() {
        assertThat(atMost(4).contains(atMost(3))).isTrue();
        assertThat(atMost(4).contains(atMost(4))).isTrue();
        assertThat(atMost(4).contains(atMost(5))).isFalse();
    }

    @Test
    public void testInvalid() {
        // Ranges don't contain open intervals
        assertThat(atMost(4).contains(atLeast(1))).isFalse();
        assertThat(atLeast(4).contains(atMost(4))).isFalse();
        assertThat(range(1, 4).contains(atLeast(1))).isFalse();
        assertThat(range(1, 4).contains(atMost(4))).isFalse();
    }

    @Test
    public void testMixedEndpoints() {
        assertThat(atLeast(4).contains(atLeast(4))).isTrue();
        assertThat(atLeast(4).contains(atLeast(3.9999))).isFalse();
        assertThat(atLeast(4).contains(atLeast(4.0001))).isTrue();
        assertThat(greaterThan(4).contains(atLeast(4.0001))).isTrue();
        assertThat(greaterThan(4).contains(atLeast(4))).isFalse();

        assertThat(atMost(4).contains(atMost(4))).isTrue();
        assertThat(atMost(4).contains(atMost(4.0001))).isFalse();
        assertThat(atMost(4).contains(atMost(3.9999))).isTrue();
        assertThat(lessThan(4).contains(atMost(3.9999))).isTrue();
        assertThat(lessThan(4).contains(atMost(4))).isFalse();
        assertThat(lessThan(4).contains(lessThan(4))).isTrue();
    }

    @Test
    public void testCompareFloatWithIntRange() {
        assertThat(atLeast(4).contains(IntRangeConstraint.atLeast(4))).isTrue();
        assertThat(atLeast(4).contains(IntRangeConstraint.atLeast(3))).isFalse();
        assertThat(atLeast(4).contains(IntRangeConstraint.atLeast(5))).isTrue();
        assertThat(greaterThan(4).contains(IntRangeConstraint.atLeast(5))).isTrue();
        assertThat(greaterThan(4).contains(IntRangeConstraint.atLeast(4))).isFalse();

        assertThat(atMost(4).contains(IntRangeConstraint.atMost(4))).isTrue();
        assertThat(atMost(4).contains(IntRangeConstraint.atMost(5))).isFalse();
        assertThat(atMost(4).contains(IntRangeConstraint.atMost(3))).isTrue();
        assertThat(lessThan(4).contains(IntRangeConstraint.atMost(3))).isTrue();
        assertThat(lessThan(4).contains(IntRangeConstraint.atMost(4))).isFalse();
    }

    @Test
    public void testCompareIntWithFloatRange() {
        assertThat(IntRangeConstraint.atLeast(4).contains(atLeast(4))).isTrue();
        assertThat(IntRangeConstraint.atLeast(4).contains(atLeast(3.9999))).isFalse();
        assertThat(IntRangeConstraint.atLeast(4).contains(atLeast(4.0001))).isTrue();
        assertThat(IntRangeConstraint.atLeast(4).contains(greaterThan(5))).isTrue();
        assertThat(IntRangeConstraint.atLeast(4).contains(greaterThan(4))).isFalse();

        assertThat(IntRangeConstraint.atMost(4).contains(atMost(4))).isTrue();
        assertThat(IntRangeConstraint.atMost(4).contains(atMost(4.0001))).isFalse();
        assertThat(IntRangeConstraint.atMost(4).contains(atMost(3.9999))).isTrue();
        assertThat(IntRangeConstraint.atMost(4).contains(lessThan(4))).isFalse();
    }
}
