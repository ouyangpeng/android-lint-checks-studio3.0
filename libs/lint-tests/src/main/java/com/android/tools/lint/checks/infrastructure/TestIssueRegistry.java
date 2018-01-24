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

package com.android.tools.lint.checks.infrastructure;

import com.android.annotations.NonNull;
import com.android.tools.lint.checks.BuiltinIssueRegistry;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Scope;
import java.util.EnumSet;
import java.util.List;

public class TestIssueRegistry extends BuiltinIssueRegistry {
    private final List<Issue> issues;

    public TestIssueRegistry() {
        issues = null;
        BuiltinIssueRegistry.reset();
    }

    public TestIssueRegistry(@NonNull List<Issue> issues) {
        this.issues = issues;
        BuiltinIssueRegistry.reset();
    }

    @NonNull
    @Override
    public List<Issue> getIssues() {
        return issues != null ? issues : super.getIssues();
    }

    // Override to make method accessible outside package
    @NonNull
    @Override
    public List<Issue> getIssuesForScope(@NonNull EnumSet<Scope> scope) {
        return super.getIssuesForScope(scope);
    }
}
