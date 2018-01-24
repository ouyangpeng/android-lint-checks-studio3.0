/*
 * Copyright (C) 2014 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import junit.framework.TestCase;

public class SeverityTest extends TestCase {
    public void testGetName() {
        assertThat(Severity.ERROR.name()).isEqualTo("ERROR");
        assertThat(Severity.WARNING.name()).isEqualTo("WARNING");
    }

    public void testGetDescription() {
        assertThat(Severity.ERROR.getDescription()).isEqualTo("Error");
        assertThat(Severity.WARNING.getDescription()).isEqualTo("Warning");
    }

    public void testFromString() {
        assertThat(Severity.fromName("ERROR")).isSameAs(Severity.ERROR);
        assertThat(Severity.fromName("error")).isSameAs(Severity.ERROR);
        assertThat(Severity.fromName("Error")).isSameAs(Severity.ERROR);
        assertThat(Severity.fromName("WARNING")).isSameAs(Severity.WARNING);
        assertThat(Severity.fromName("warning")).isSameAs(Severity.WARNING);
        assertThat(Severity.fromName("FATAL")).isSameAs(Severity.FATAL);
        assertThat(Severity.fromName("Informational")).isSameAs(Severity.INFORMATIONAL);
        assertThat(Severity.fromName("ignore")).isSameAs(Severity.IGNORE);
        assertThat(Severity.fromName("IGNORE")).isSameAs(Severity.IGNORE);
    }

    public void testCompare() {
        assertThat(Severity.IGNORE).isGreaterThan(Severity.ERROR);
        assertThat(Severity.WARNING).isGreaterThan(Severity.ERROR);
        assertThat(Severity.ERROR).isEquivalentAccordingToCompareTo(Severity.ERROR);
        assertThat(Severity.FATAL).isLessThan(Severity.ERROR);
        assertThat(Severity.WARNING).isGreaterThan(Severity.ERROR);
    }

    public void testIsError() {
        assertThat(Severity.IGNORE.isError()).isFalse();
        assertThat(Severity.INFORMATIONAL.isError()).isFalse();
        assertThat(Severity.WARNING.isError()).isFalse();
        assertThat(Severity.ERROR.isError()).isTrue();
        assertThat(Severity.FATAL.isError()).isTrue();
    }

    public void testMin() {
        assertSame(Severity.INFORMATIONAL, Severity.min(Severity.ERROR, Severity.INFORMATIONAL));
        assertSame(Severity.INFORMATIONAL, Severity.min(Severity.INFORMATIONAL, Severity.ERROR));
        assertSame(Severity.ERROR, Severity.min(Severity.ERROR, Severity.ERROR));
        assertSame(Severity.ERROR, Severity.min(Severity.FATAL, Severity.ERROR));
    }

    public void testMax() {
        assertSame(Severity.ERROR, Severity.max(Severity.ERROR, Severity.INFORMATIONAL));
        assertSame(Severity.ERROR, Severity.max(Severity.INFORMATIONAL, Severity.ERROR));
        assertSame(Severity.ERROR, Severity.max(Severity.ERROR, Severity.ERROR));
        assertSame(Severity.FATAL, Severity.max(Severity.FATAL, Severity.ERROR));
    }
}
