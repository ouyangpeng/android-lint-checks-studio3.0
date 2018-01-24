/*
 * Copyright (C) 2015 The Android Open Source Project
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
import com.android.testutils.TestUtils;
import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.checks.infrastructure.TestIssueRegistry;
import com.android.tools.lint.checks.infrastructure.TestLintTask;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.client.api.LintDriver;
import com.android.tools.lint.client.api.LintRequest;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class AbstractCheckTest extends LintDetectorTest {
    @Override
    protected boolean allowAndroidBuildEnvironment() {
        return false;
    }

    @Override
    protected List<Issue> getIssues() {
        return getRegisteredIssuesFromDetector();
    }

    @NonNull
    private List<Issue> getRegisteredIssuesFromDetector() {
        List<Issue> issues = new ArrayList<>();
        Class<? extends Detector> detectorClass = getDetectorInstance().getClass();
        // Get the list of issues from the registry and filter out others, to make sure
        // issues are properly registered
        List<Issue> candidates = new TestIssueRegistry().getIssues();
        for (Issue issue : candidates) {
            if (issue.getImplementation().getDetectorClass() == detectorClass) {
                issues.add(issue);
            }
        }

        return issues;
    }

    @Override
    public InputStream getTestResource(String relativePath, boolean expectExists) {
        fail("We should not be using file-based resources in the lint builtin unit tests.");
        return null;
    }

    @Override
    protected TestLintClient createClient() {
        return new ToolsBaseTestLintClient();
    }

    static File sdk;
    static {
        sdk = TestUtils.getSdk();
    }

    @NonNull
    public static File getSdk() {
        return sdk;
    }

    @Override
    @NonNull
    protected TestLintTask lint() {
        // instead of super.lint: don't set issues such that we can compute and compare
        // detector results below
        TestLintTask task = TestLintTask.lint();
        task.runCompatChecks(false);

        task.checkMessage((context, issue, severity, location, message, fix)
                -> AbstractCheckTest.super.checkReportedError(context, issue, severity, location,
                message, fix));

        // We call getIssues() instead of setting task.detector() because the above
        // getIssues call will ensure that we only check issues registered in the class
        task.detector(getDetectorInstance());

        // Now check check the discrepancy to look for unregistered issues and
        // highlight these
// TODO: Handle problems from getRegisteredIssuesFromDetector and if no fields are found
// don't assert the below. Basically, let the ISSUE field live outside the detector class
// (such as in a companion.)
        List<Issue> computedIssues = getRegisteredIssuesFromDetector();
        if (getIssues().equals(computedIssues)) {
            Set<Issue> checkedIssues = Sets.newHashSet(task.getCheckedIssues());
            Set<Issue> detectorIssues = Sets.newHashSet(computedIssues);
            if (!checkedIssues.equals(detectorIssues)) {
                Set<Issue> difference = Sets.symmetricDifference(checkedIssues, detectorIssues);
                fail("Discrepancy in issues listed in detector class "
                        +getDetectorInstance().getClass().getSimpleName()+" and issues "
                        +"found in the issue registry: "+difference);
            }
        }

        task.issues(getIssues().toArray(new Issue[0]));
        task.sdkHome(sdk);
        return task;
    }

    /**
     * Overrides TestLintClient to use the checked-in SDK that is available in the tools/base
     * repo. The "real" TestLintClient is a public utility for writing lint tests, so it cannot
     * make assumptions specific to tools/base.
     */
    protected class ToolsBaseTestLintClient extends TestLintClient {
        @Nullable
        @Override
        public File getSdkHome() {
            return TestUtils.getSdk();
        }

        @NonNull
        @Override
        protected LintDriver createDriver(@NonNull IssueRegistry registry,
                @NonNull LintRequest request) {
            LintDriver driver = super.createDriver(registry, request);
            // All the builtin tests have been migrated; make tests faster
            // by not computing PSI/Lombok trees when not necessary, and better yet,
            // make sure that no new tests are accidentally integrated that are using
            // the old mechanism (with this the tests won't work)
            driver.setRunCompatChecks(false, false);
            return driver;
        }
    }
}
