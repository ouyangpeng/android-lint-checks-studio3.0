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

package com.android.tools.lint.client.api;

import static com.google.common.truth.Truth.assertThat;

import com.android.tools.lint.checks.AbstractCheckTest;
import com.android.tools.lint.checks.HardcodedValuesDetector;
import com.android.tools.lint.checks.ManifestDetector;
import com.android.tools.lint.checks.SupportAnnotationDetector;
import com.android.tools.lint.detector.api.DefaultPosition;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Severity;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import org.intellij.lang.annotations.Language;

public class LintBaselineTest extends AbstractCheckTest {
    public void testBaseline() throws IOException {
        File baselineFile = File.createTempFile("baseline", ".xml");
        baselineFile.deleteOnExit();

        @Language("XML")
        String baselineContents = ""
                + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<issues format=\"4\" by=\"lint unittest\">\n"
                + "\n"
                + "    <issue\n"
                + "        id=\"UsesMinSdkAttributes\"\n"
                + "        severity=\"Warning\"\n"
                + "        message=\"&lt;uses-sdk> tag should specify a target API level (the highest verified version; when running on later versions, compatibility behaviors may be enabled) with android:targetSdkVersion=&quot;?&quot;\"\n"
                + "        category=\"Correctness\"\n"
                + "        priority=\"9\"\n"
                + "        summary=\"Minimum SDK and target SDK attributes not defined\"\n"
                + "        explanation=\"The manifest should contain a `&lt;uses-sdk>` element which defines the minimum API Level required for the application to run, as well as the target version (the highest API level you have tested the version for.)\"\n"
                + "        url=\"http://developer.android.com/guide/topics/manifest/uses-sdk-element.html\"\n"
                + "        urls=\"http://developer.android.com/guide/topics/manifest/uses-sdk-element.html\"\n"
                + "        errorLine1=\"    &lt;uses-sdk android:minSdkVersion=&quot;8&quot; />\"\n"
                + "        errorLine2=\"    ^\">\n"
                + "        <location\n"
                + "            file=\"AndroidManifest.xml\"\n"
                + "            line=\"7\"/>\n"
                + "    </issue>\n"
                + "\n"
                + "    <issue\n"
                + "        id=\"HardcodedText\"\n"
                + "        severity=\"Warning\"\n"
                + "        message=\"[I18N] Hardcoded string &quot;Fooo&quot;, should use @string resource\"\n"
                + "        category=\"Internationalization\"\n"
                + "        priority=\"5\"\n"
                + "        summary=\"Hardcoded text\"\n"
                + "        explanation=\"Hardcoding text attributes directly in layout files is bad for several reasons:\n"
                + "\n"
                + "* When creating configuration variations (for example for landscape or portrait)you have to repeat the actual text (and keep it up to date when making changes)\n"
                + "\n"
                + "* The application cannot be translated to other languages by just adding new translations for existing string resources.\n"
                + "\n"
                + "There are quickfixes to automatically extract this hardcoded string into a resource lookup.\"\n"
                + "        errorLine1=\"        android:text=&quot;Fooo&quot; />\"\n"
                + "        errorLine2=\"        ~~~~~~~~~~~~~~~~~~~\">\n"
                + "        <location\n"
                + "            file=\"res/layout/main.xml\"\n"
                + "            line=\"12\"/>\n"
                + "        <location\n"
                + "            file=\"res/layout/main2.xml\"\n"
                + "            line=\"11\"/>\n"
                + "    </issue>\n"
                + "\n"
                + "    <issue\n"
                + "        id=\"Range\"\n"
                + "        message=\"Value must be \u2265 0 (was -1)\"\n"
                + "        errorLine1=\"                                childHeightSpec = MeasureSpec.makeMeasureSpec(maxLayoutHeight,\"\n"
                + "        errorLine2=\"                                                                              ~~~~~~~~~~~~~~~\">\n"
                + "        <location\n"
                + "            file=\"java/android/support/v4/widget/SlidingPaneLayout.java\"\n"
                + "            line=\"589\"\n"
                + "            column=\"79\"/>\n"
                +"    </issue>\n"
                + "\n"
                + "</issues>\n";
        Files.write(baselineContents, baselineFile, Charsets.UTF_8);

        LintBaseline baseline = new LintBaseline(createClient(), baselineFile);

        boolean found;
        found = baseline.findAndMark(ManifestDetector.USES_SDK,
                Location.create(new File("bogus")), "Unrelated)", Severity.WARNING, null);
        assertThat(found).isFalse();
        assertThat(baseline.getFoundWarningCount()).isEqualTo(0);
        assertThat(baseline.getFoundErrorCount()).isEqualTo(0);
        assertThat(baseline.getTotalCount()).isEqualTo(3);
        // because we haven't actually matched anything
        assertThat(baseline.getFixedCount()).isEqualTo(3);

        // Wrong issue
        found = baseline.findAndMark(ManifestDetector.USES_SDK,
                Location.create(new File("bogus")),
                "Hardcoded string \"Fooo\", should use @string resource", Severity.WARNING,
                null);
        assertThat(found).isFalse();
        assertThat(baseline.getFoundWarningCount()).isEqualTo(0);
        assertThat(baseline.getFoundErrorCount()).isEqualTo(0);
        assertThat(baseline.getFixedCount()).isEqualTo(3);

        // Wrong file
        found = baseline.findAndMark(HardcodedValuesDetector.ISSUE,
                Location.create(new File("res/layout-port/main.xml")),
                "Hardcoded string \"Fooo\", should use @string resource", Severity.WARNING,
                null);
        assertThat(found).isFalse();
        assertThat(baseline.getFoundWarningCount()).isEqualTo(0);
        assertThat(baseline.getFoundErrorCount()).isEqualTo(0);
        assertThat(baseline.getFixedCount()).isEqualTo(3);

        // Match
        found = baseline.findAndMark(HardcodedValuesDetector.ISSUE,
                Location.create(new File("res/layout/main.xml")),
                "Hardcoded string \"Fooo\", should use @string resource", Severity.WARNING,
                null);
        assertThat(found).isTrue();
        assertThat(baseline.getFixedCount()).isEqualTo(2);
        assertThat(baseline.getFoundWarningCount()).isEqualTo(1);
        assertThat(baseline.getFoundErrorCount()).isEqualTo(0);
        assertThat(baseline.getFixedCount()).isEqualTo(2);

        // Search for the same error once it's already been found: no longer there
        found = baseline.findAndMark(HardcodedValuesDetector.ISSUE,
                Location.create(new File("res/layout/main.xml")),
                "Hardcoded string \"Fooo\", should use @string resource", Severity.WARNING,
                null);
        assertThat(found).isFalse();
        assertThat(baseline.getFoundWarningCount()).isEqualTo(1);
        assertThat(baseline.getFoundErrorCount()).isEqualTo(0);
        assertThat(baseline.getFixedCount()).isEqualTo(2);

        // Match
        found = baseline.findAndMark(SupportAnnotationDetector.RANGE,
                Location.create(new File(
                        "java/android/support/v4/widget/SlidingPaneLayout.java")),
                "Value must be \u2265 0 (was -1)", Severity.WARNING,
                null);
        assertThat(found).isTrue();
        assertThat(baseline.getFixedCount()).isEqualTo(1);
        assertThat(baseline.getFoundWarningCount()).isEqualTo(2);
        assertThat(baseline.getFoundErrorCount()).isEqualTo(0);
        assertThat(baseline.getFixedCount()).isEqualTo(1);

        baseline.close();
    }

    public void testSuffix() {
        assertTrue(LintBaseline.isSamePathSuffix("foo", "foo"));
        assertTrue(LintBaseline.isSamePathSuffix("", ""));
        assertTrue(LintBaseline.isSamePathSuffix("abc/def/foo", "def/foo"));
        assertTrue(LintBaseline.isSamePathSuffix("abc/def/foo", "../../def/foo"));
        assertTrue(LintBaseline.isSamePathSuffix("abc\\def\\foo", "abc\\def\\foo"));
        assertTrue(LintBaseline.isSamePathSuffix("abc\\def\\foo", "..\\..\\abc\\def\\foo"));
        assertTrue(LintBaseline.isSamePathSuffix("abc\\def\\foo", "def\\foo"));
        assertFalse(LintBaseline.isSamePathSuffix("foo", "bar"));
    }

    public void testFormat() throws IOException {
        File baselineFile = File.createTempFile("lint-baseline", ".xml");
        LintBaseline baseline = new LintBaseline(createClient(), baselineFile);
        assertThat(baseline.isWriteOnClose()).isFalse();
        baseline.setWriteOnClose(true);
        assertThat(baseline.isWriteOnClose()).isTrue();

        baseline.findAndMark(HardcodedValuesDetector.ISSUE,
                Location.create(new File("my/source/file.txt"), "", 0),
                "Hardcoded string \"Fooo\", should use `@string` resource",
                Severity.WARNING, null);
        baseline.findAndMark(
                ManifestDetector.USES_SDK,
                Location.create(new File("/foo/bar/Foo/AndroidManifest.xml"),
                        new DefaultPosition(6, 4, 198), new DefaultPosition(6, 42, 236)),
                "<uses-sdk> tag should specify a target API level (the highest verified \n"
                        + "version; when running on later versions, compatibility behaviors may \n"
                        + "be enabled) with android:targetSdkVersion=\"?\"",
                Severity.WARNING, null);
        baseline.close();

        String actual = Files.toString(baselineFile, Charsets.UTF_8).
                replace(File.separatorChar, '/');

        @Language("XML")
        String expected = ""
                + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<issues format=\"4\" by=\"lint unittest\">\n"
                + "\n"
                + "    <issue\n"
                + "        id=\"UsesMinSdkAttributes\"\n"
                + "        message=\"&lt;uses-sdk> tag should specify a target API level (the highest verified &#xA;version; when running on later versions, compatibility behaviors may &#xA;be enabled) with android:targetSdkVersion=&quot;?&quot;\">\n"
                + "        <location\n"
                + "            file=\"/foo/bar/Foo/AndroidManifest.xml\"\n"
                + "            line=\"7\"/>\n"
                + "    </issue>\n"
                + "\n"
                + "    <issue\n"
                + "        id=\"HardcodedText\"\n"
                + "        message=\"Hardcoded string &quot;Fooo&quot;, should use `@string` resource\">\n"
                + "        <location\n"
                + "            file=\"my/source/file.txt\"\n"
                + "            line=\"1\"/>\n"
                + "    </issue>\n"
                + "\n"
                + "</issues>\n";
        assertThat(actual).isEqualTo(expected);

        // Now load the baseline back in and make sure we can match entries correctly
        baseline = new LintBaseline(createClient(), baselineFile);
        baseline.setWriteOnClose(true);
        assertThat(baseline.isRemoveFixed()).isFalse();

        boolean found;
        found = baseline.findAndMark(HardcodedValuesDetector.ISSUE,
                Location.create(new File("my/source/file.txt"), "", 0),
                "Hardcoded string \"Fooo\", should use `@string` resource",
                Severity.WARNING, null);
        assertThat(found).isTrue();
        found = baseline.findAndMark(
                ManifestDetector.USES_SDK,
                Location.create(new File("/foo/bar/Foo/AndroidManifest.xml"),
                        new DefaultPosition(6, 4, 198), new DefaultPosition(6, 42, 236)),
                "<uses-sdk> tag should specify a target API level (the highest verified \n"
                        + "version; when running on later versions, compatibility behaviors may \n"
                        + "be enabled) with android:targetSdkVersion=\"?\"",
                Severity.WARNING, null);
        assertThat(found).isTrue();
        baseline.close();

        actual = Files.toString(baselineFile, Charsets.UTF_8).
                replace(File.separatorChar, '/');
        assertThat(actual).isEqualTo(expected);

        // Test the skip fix flag
        baseline = new LintBaseline(createClient(), baselineFile);
        baseline.setWriteOnClose(true);
        baseline.setRemoveFixed(true);
        assertThat(baseline.isRemoveFixed()).isTrue();

        found = baseline.findAndMark(HardcodedValuesDetector.ISSUE,
                Location.create(new File("my/source/file.txt"), "", 0),
                "Hardcoded string \"Fooo\", should use `@string` resource",
                Severity.WARNING, null);
        assertThat(found).isTrue();

        // Note that this is a different, unrelated issue
        found = baseline.findAndMark(
                ManifestDetector.APPLICATION_ICON,
                Location.create(new File("/foo/bar/Foo/AndroidManifest.xml"),
                        new DefaultPosition(4, 4, 198), new DefaultPosition(4, 42, 236)),
                "Should explicitly set `android:icon`, there is no default",
                Severity.WARNING, null);
        assertThat(found).isFalse();
        baseline.close();

        actual = Files.toString(baselineFile, Charsets.UTF_8).
                replace(File.separatorChar, '/');

        // This time we should ONLY get the initial baseline issue back; we should
        // NOT see the new issue, and the fixed issue (the uses sdk error reported in the baseline
        // before but not repeated now) should be missing.
        assertThat(actual).isEqualTo(""
                + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<issues format=\"4\" by=\"lint unittest\">\n"
                + "\n"
                + "    <issue\n"
                + "        id=\"HardcodedText\"\n"
                + "        message=\"Hardcoded string &quot;Fooo&quot;, should use `@string` resource\">\n"
                + "        <location\n"
                + "            file=\"my/source/file.txt\"\n"
                + "            line=\"1\"/>\n"
                + "    </issue>\n"
                + "\n"
                + "</issues>\n");
    }

    @Override
    protected Detector getDetector() {
        fail("Not used by this test");
        return null;
    }
}