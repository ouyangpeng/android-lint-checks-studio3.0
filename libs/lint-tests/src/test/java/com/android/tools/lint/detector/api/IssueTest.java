/*
 * Copyright (C) 2012 The Android Open Source Project
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

import static org.junit.Assert.assertNotEquals;

import com.android.tools.lint.checks.AccessibilityDetector;
import java.util.Arrays;
import java.util.Collections;
import junit.framework.TestCase;

@SuppressWarnings("javadoc")
public class IssueTest extends TestCase {
    public void testIssueUrls() {
        // Test that the addMoreInfo/getMoreInfo handling (which switches between
        // a direct string and a list internally when there is more than one item)
        // works correctly
        Implementation implementation = new Implementation(AccessibilityDetector.class,
                Scope.RESOURCE_FILE_SCOPE);
        Issue issue = Issue.create("MyId", "ShortDesc", "Explanation",
                Category.CORRECTNESS, 10, Severity.ERROR, implementation);
        assertTrue(issue.getMoreInfo().isEmpty());
        String url1 = "http://tools.android.com";
        String url2 = "http://tools.android.com/recent";
        issue.addMoreInfo(url1);
        assertEquals(Collections.singletonList(url1), issue.getMoreInfo());
        issue.addMoreInfo(url2);
        assertEquals(Arrays.asList(url1, url2), issue.getMoreInfo());
    }

    public void testEquals() {
        Implementation implementation = new Implementation(AccessibilityDetector.class,
                Scope.RESOURCE_FILE_SCOPE);
        Issue issue1 = Issue.create("MyId", "ShortDesc", "Explanation",
                Category.CORRECTNESS, 10, Severity.ERROR, implementation);

        Issue issue2 = Issue.create("MyId", "ShortDesc2", "Explanation",
                Category.CORRECTNESS, 10, Severity.ERROR, implementation);

        Issue issue3 = Issue.create("SomethingElse", "ShortDesc", "Explanation",
                Category.CORRECTNESS, 10, Severity.ERROR, implementation);

        assertEquals(issue1, issue1);
        assertEquals(issue1, issue2); // only id counts, not the other attributes
        assertNotEquals(issue1, issue3);
    }
}
