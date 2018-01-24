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

import static com.android.tools.lint.checks.SizeConstraint.atLeast;
import static com.android.tools.lint.checks.SizeConstraint.atMost;
import static com.android.tools.lint.checks.SizeConstraint.exactly;
import static com.android.tools.lint.checks.SizeConstraint.multiple;
import static com.android.tools.lint.checks.SizeConstraint.range;
import static com.android.tools.lint.checks.SizeConstraint.rangeWithMultiple;
import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class SizeConstraintTest {

    @Test
    public void testDescribe() {
        assertThat(range(1, 5).describe()).isEqualTo("Size must be at least 1 and at most 5");
        assertThat(atLeast(5).describe()).isEqualTo("Size must be at least 5");
        assertThat(atMost(5).describe()).isEqualTo("Size must be at most 5");
        assertThat(range(1, 5).describe(3)).isEqualTo("Size must be at least 1 and at most 5 (was 3)");
        assertThat(range(1, 5).describe(0)).isEqualTo("Expected Size ≥ 1 (was 0)");
        assertThat(range(1, 5).describe(15)).isEqualTo("Expected Size ≤ 5 (was 15)");
        assertThat(exactly(42).describe()).isEqualTo("Size must be exactly 42");
        assertThat(multiple(4).describe()).isEqualTo("Size must be a multiple of 4");
        assertThat(rangeWithMultiple(20, 100, 5).describe()).isEqualTo("Size must be at least 20 and at most 100 and a multiple of 5");
        assertThat(rangeWithMultiple(20, 100, 5).describe(10)).isEqualTo("Expected Size ≥ 20 (was 10)");
        assertThat(rangeWithMultiple(20, 100, 5).describe(200)).isEqualTo("Expected Size ≤ 100 (was 200)");
        assertThat(rangeWithMultiple(20, 100, 5).describe(51)).isEqualTo("Expected Size to be a multiple of 5 (was 51 and should be either 50 or 55)");
    }

    @Test
    public void testExactly() {
        assertThat(exactly(3).contains(exactly(3))).isTrue();
        assertThat(exactly(3).contains(exactly(4))).isFalse();
    }

    @Test
    public void testRangeContainsExactly() {
        assertThat(range(2, 4).contains(exactly(3))).isTrue();
        assertThat(range(2, 4).contains(exactly(2))).isTrue();
        assertThat(range(2, 4).contains(exactly(4))).isTrue();
        assertThat(range(2, 4).contains(exactly(5))).isFalse();
        assertThat(range(2, 4).contains(exactly(1))).isFalse();
        assertThat(range(2, 2).contains(exactly(2))).isTrue();
    }

    @Test
    public void testMinContainsExactly() {
        assertThat(atLeast(2).contains(exactly(3))).isTrue();
        assertThat(atLeast(3).contains(exactly(3))).isTrue();
        assertThat(atLeast(4).contains(exactly(3))).isFalse();
    }

    @Test
    public void testMaxContainsExactly() {
        assertThat(atMost(4).contains(exactly(3))).isTrue();
        assertThat(atMost(3).contains(exactly(3))).isTrue();
        assertThat(atMost(2).contains(exactly(3))).isFalse();
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
    public void testMultiples() {
        assertThat(multiple(8).contains(multiple(8))).isTrue();
        assertThat(multiple(8).contains(multiple(4))).isFalse();
        assertThat(multiple(8).contains(multiple(16))).isTrue();
        assertThat(multiple(8).contains(multiple(1))).isFalse();
        assertThat(multiple(8).contains(multiple(32))).isTrue();
        assertThat(multiple(8).contains(multiple(33))).isFalse();

        assertThat(rangeWithMultiple(20, 100, 5).contains(exactly(20))).isTrue();
        assertThat(rangeWithMultiple(20, 100, 5).contains(exactly(21))).isFalse();

        assertThat(rangeWithMultiple(20, 100, 5).contains(rangeWithMultiple(20, 40, 5))).isTrue();
        assertThat(rangeWithMultiple(20, 100, 5).contains(rangeWithMultiple(20, 40, 10))).isTrue();
        assertThat(rangeWithMultiple(20, 100, 5).contains(rangeWithMultiple(20, 40, 3))).isFalse();
        assertThat(rangeWithMultiple(20, 100, 5).contains(range(20, 40))).isFalse();
        assertThat(rangeWithMultiple(40, 100, 5).contains(range(40, 60))).isFalse();
        assertThat(range(20, 100).contains(rangeWithMultiple(20, 40, 2))).isTrue();
    }
}
