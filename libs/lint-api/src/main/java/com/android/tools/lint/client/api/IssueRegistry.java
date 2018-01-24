/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.tools.lint.client.api;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.google.common.annotations.Beta;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry which provides a list of checks to be performed on an Android project
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public abstract class IssueRegistry {
    private static volatile List<Category> categories;
    private static volatile Map<String, Issue> idToIssue;
    private static Map<EnumSet<Scope>, List<Issue>> scopeIssues = Maps.newHashMap();

    /**
     * Creates a new {@linkplain IssueRegistry}
     */
    protected IssueRegistry() {
    }

    private static final Implementation DUMMY_IMPLEMENTATION = new Implementation(Detector.class,
            EnumSet.noneOf(Scope.class));
    /**
     * Issue reported by lint (not a specific detector) when it cannot even
     * parse an XML file prior to analysis
     */
    @NonNull
    public static final Issue PARSER_ERROR = Issue.create(
            "ParserError",
            "Parser Errors",
            "Lint will ignore any files that contain fatal parsing errors. These may contain " +
            "other errors, or contain code which affects issues in other files.",
            Category.LINT,
            10,
            Severity.ERROR,
            DUMMY_IMPLEMENTATION);

    /**
     * Issue reported by lint for various other issues which prevents lint from
     * running normally when it's not necessarily an error in the user's code base.
     */
    @NonNull
    public static final Issue LINT_ERROR = Issue.create(
            "LintError",
            "Lint Failure",
            "This issue type represents a problem running lint itself. Examples include " +
            "failure to find bytecode for source files (which means certain detectors " +
            "could not be run), parsing errors in lint configuration files, etc." +
            "\n" +
            "These errors are not errors in your own code, but they are shown to make " +
            "it clear that some checks were not completed.",

            Category.LINT,
            10,
            Severity.ERROR,
            DUMMY_IMPLEMENTATION);

    /**
     * Issue reported when lint is canceled
     */
    @NonNull
    public static final Issue CANCELLED = Issue.create(
            "LintCanceled",
            "Lint Canceled",
            "Lint canceled by user; the issue report may not be complete.",

            Category.LINT,
            0,
            Severity.INFORMATIONAL,
            DUMMY_IMPLEMENTATION);

    /**
     * Issue reported by lint for various other issues which prevents lint from
     * running normally when it's not necessarily an error in the user's code base.
     */
    @NonNull
    public static final Issue BASELINE = Issue.create(
            "LintBaseline",
            "Baseline Issues",
            "Lint can be configured with a \"baseline\"; a set of current issues found in " +
            "a codebase, which future runs of lint will silently ignore. Only new issues " +
            "not found in the baseline are reported.\n" +
            "\n" +
            "Note that while opening files in the IDE, baseline issues are not filtered out; " +
            "the purpose of baselines is to allow you to get started using lint and break " +
            "the build on all newly introduced errors, without having to go back and fix the " +
            "entire codebase up front. However, when you open up existing files you still " +
            "want to be aware of and fix issues as you come across them.\n" +
            "\n" +
            "This issue type is used to emit two types of informational messages in reports: " +
            "first, whether any issues were filtered out so you don't have a false sense of " +
            "security if you forgot that you've checked in a baseline file, and second, " +
            "whether any issues in the baseline file appear to have been fixed such that you " +
            "can stop filtering them out and get warned if the issues are re-introduced.",

            Category.LINT,
            10,
            Severity.INFORMATIONAL,
            DUMMY_IMPLEMENTATION);

    /**
     * Issue reported by lint when it encounters old lint checks that haven't been
     * updated to the latest APIs.
     */
    @NonNull
    public static final Issue OBSOLETE_LINT_CHECK = Issue.create(
            "ObsoleteLintCustomCheck",
            "Obsolete custom lint check",

            "Lint can be extended with \"custom checks\": additional checks implemented by " +
            "developers and libraries to for example enforce specific API usages required " +
            "by a library or a company coding style guideline.\n" +
            "\n" +
            "The Lint APIs are not yet stable, so these checks may either cause a performance, " +
            "degradation, or stop working, or provide wrong results.\n" +
            "\n" +
            "This warning flags custom lint checks that are found to be using obsolete APIs and " +
            "will need to be updated to run in the current lint environment.",

            Category.LINT,
            10,
            Severity.WARNING,
            DUMMY_IMPLEMENTATION);

    /**
     * Returns the list of issues that can be found by all known detectors.
     *
     * @return the list of issues to be checked (including those that may be
     *         disabled!)
     */
    @NonNull
    public abstract List<Issue> getIssues();

    /**
     * Get an approximate issue count for a given scope. This is just an optimization,
     * so the number does not have to be accurate.
     *
     * @param scope the scope set
     * @return an approximate ceiling of the number of issues expected for a given scope set
     */
    protected int getIssueCapacity(@NonNull EnumSet<Scope> scope) {
        return 20;
    }

    /**
     * Returns all available issues of a given scope (regardless of whether
     * they are actually enabled for a given configuration etc)
     *
     * @param scope the applicable scope set
     * @return a list of issues
     */
    @NonNull
    protected List<Issue> getIssuesForScope(@NonNull EnumSet<Scope> scope) {
        List<Issue> list = scopeIssues.get(scope);
        if (list == null) {
            List<Issue> issues = getIssues();
            if (scope.equals(Scope.ALL)) {
                list = issues;
            } else {
                list = new ArrayList<>(getIssueCapacity(scope));
                for (Issue issue : issues) {
                    // Determine if the scope matches
                    if (issue.getImplementation().isAdequate(scope)) {
                        list.add(issue);
                    }
                }
            }
            scopeIssues.put(scope, list);
        }

        return list;
    }

    /**
     * Creates a list of detectors applicable to the given scope, and with the
     * given configuration.
     *
     * @param client the client to report errors to
     * @param configuration the configuration to look up which issues are
     *            enabled etc from
     * @param scope the scope for the analysis, to filter out detectors that
     *            require wider analysis than is currently being performed
     * @param scopeToDetectors an optional map which (if not null) will be
     *            filled by this method to contain mappings from each scope to
     *            the applicable detectors for that scope
     * @return a list of new detector instances
     */
    @NonNull
    final List<? extends Detector> createDetectors(
            @NonNull LintClient client,
            @NonNull Configuration configuration,
            @NonNull EnumSet<Scope> scope,
            @Nullable Map<Scope, List<Detector>> scopeToDetectors) {

        List<Issue> issues = getIssuesForScope(scope);
        if (issues.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Class<? extends Detector>> detectorClasses = new HashSet<>();
        Map<Class<? extends Detector>, EnumSet<Scope>> detectorToScope =
                new HashMap<>();

        for (Issue issue : issues) {
            Implementation implementation = issue.getImplementation();
            Class<? extends Detector> detectorClass = implementation.getDetectorClass();
            EnumSet<Scope> issueScope = implementation.getScope();
            if (!detectorClasses.contains(detectorClass)) {
                // Determine if the issue is enabled
                if (!configuration.isEnabled(issue)) {
                    continue;
                }

                assert implementation.isAdequate(scope); // Ensured by getIssuesForScope above

                detectorClass = client.replaceDetector(detectorClass);

                assert detectorClass != null : issue.getId();
                detectorClasses.add(detectorClass);
            }

            if (scopeToDetectors != null) {
                EnumSet<Scope> s = detectorToScope.get(detectorClass);
                if (s == null) {
                    detectorToScope.put(detectorClass, issueScope);
                } else if (!s.containsAll(issueScope)) {
                    EnumSet<Scope> union = EnumSet.copyOf(s);
                    union.addAll(issueScope);
                    detectorToScope.put(detectorClass, union);
                }
            }
        }

        List<Detector> detectors = new ArrayList<>(detectorClasses.size());
        for (Class<? extends Detector> clz : detectorClasses) {
            try {
                Detector detector = clz.newInstance();
                detectors.add(detector);

                if (scopeToDetectors != null) {
                    EnumSet<Scope> union = detectorToScope.get(clz);
                    for (Scope s : union) {
                        List<Detector> list = scopeToDetectors.get(s);
                        if (list == null) {
                            list = new ArrayList<>();
                            scopeToDetectors.put(s, list);
                        }
                        list.add(detector);
                    }

                }
            } catch (Throwable t) {
                client.log(t, "Can't initialize detector %1$s", clz.getName());
            }
        }

        return detectors;
    }

    /**
     * Returns true if the given id represents a valid issue id
     *
     * @param id the id to be checked
     * @return true if the given id is valid
     */
    public final boolean isIssueId(@NonNull String id) {
        return getIssue(id) != null;
    }

    /**
     * Returns true if the given category is a valid category
     *
     * @param name the category name to be checked
     * @return true if the given string is a valid category
     */
    public final boolean isCategoryName(@NonNull String name) {
        for (Category category : getCategories()) {
            if (category.getName().equals(name) || category.getFullName().equals(name)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the available categories
     *
     * @return an iterator for all the categories, never null
     */
    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    @NonNull
    public List<Category> getCategories() {
        List<Category> categories = IssueRegistry.categories;
        if (categories == null) {
            synchronized (IssueRegistry.class) {
                categories = IssueRegistry.categories;
                if (categories == null) {
                    IssueRegistry.categories = categories = Collections.unmodifiableList(createCategoryList());
                }
            }
        }

        return categories;
    }

    @NonNull
    private List<Category> createCategoryList() {
        Set<Category> categorySet = Sets.newHashSetWithExpectedSize(20);
        for (Issue issue : getIssues()) {
            categorySet.add(issue.getCategory());
        }
        List<Category> sorted = new ArrayList<>(categorySet);
        Collections.sort(sorted);
        return sorted;
    }

    /**
     * Returns the issue for the given id, or null if it's not a valid id
     *
     * @param id the id to be checked
     * @return the corresponding issue, or null
     */
    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    @Nullable
    public final Issue getIssue(@NonNull String id) {
        Map<String, Issue> map = idToIssue;
        if (map == null) {
            synchronized (IssueRegistry.class) {
                map = idToIssue;
                if (map == null) {
                    map = createIdToIssueMap();
                    idToIssue = map;
                }
            }
        }

        return map.get(id);
    }

    @NonNull
    private Map<String, Issue> createIdToIssueMap() {
        List<Issue> issues = getIssues();
        Map<String, Issue> map = Maps.newHashMapWithExpectedSize(issues.size() + 2);
        for (Issue issue : issues) {
            map.put(issue.getId(), issue);
        }

        map.put(PARSER_ERROR.getId(), PARSER_ERROR);
        map.put(LINT_ERROR.getId(), LINT_ERROR);
        map.put(BASELINE.getId(), BASELINE);
        map.put(OBSOLETE_LINT_CHECK.getId(), OBSOLETE_LINT_CHECK);
        return map;
    }

    /**
     * Whether this issue registry is up to date. Normally true but for example
     * for custom rules loaded from disk, may return false if the underliny file is updated
     * or deleted.
     */
    public boolean isUpToDate() {
        return true;
    }

    /**
     * Reset the registry such that it recomputes its available issues.
     */
    protected static void reset() {
        synchronized (IssueRegistry.class) {
            idToIssue = null;
            categories = null;
            scopeIssues = Maps.newHashMap();
        }
    }
}
