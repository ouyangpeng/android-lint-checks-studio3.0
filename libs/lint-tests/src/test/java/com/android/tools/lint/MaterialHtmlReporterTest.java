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

package com.android.tools.lint;

import com.android.annotations.NonNull;
import com.android.testutils.TestUtils;
import com.android.tools.lint.checks.AbstractCheckTest;
import com.android.tools.lint.checks.HardcodedValuesDetector;
import com.android.tools.lint.checks.IconDetector;
import com.android.tools.lint.checks.ManifestDetector;
import com.android.tools.lint.checks.infrastructure.TestFile.ImageTestFile;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.DefaultPosition;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Severity;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MaterialHtmlReporterTest  extends AbstractCheckTest {
    public void test() throws Exception {
        //noinspection ResultOfMethodCallIgnored
        File projectDir = TestUtils.createTempDirDeletedOnExit();
        File buildDir = new File(projectDir, "build");
        File reportFile = new File(projectDir, "report.html");
        //noinspection ResultOfMethodCallIgnored
        buildDir.mkdirs();

        try {
            LintCliClient client = new LintCliClient() {
                @Override
                IssueRegistry getRegistry() {
                    if (registry == null) {
                        registry = new IssueRegistry()  {
                            @NonNull
                            @Override
                            public List<Issue> getIssues() {
                                return Arrays.asList(
                                        ManifestDetector.USES_SDK,
                                        HardcodedValuesDetector.ISSUE,
                                        // Not reported, but for the disabled-list
                                        ManifestDetector.MOCK_LOCATION);
                            }
                        };
                    }
                    return registry;
                }
            };

            MaterialHtmlReporter reporter = new MaterialHtmlReporter(client, reportFile,
                    new LintCliFlags());
            File res = new File(projectDir, "res");
            File layout = new File(res, "layout");
            File main = new File(layout, "main.xml");
            File manifest = new File(projectDir, "AndroidManifest.xml");
            Project project = Project.create(client, projectDir, projectDir);
            Warning warning1 = new Warning(ManifestDetector.USES_SDK,
                    "<uses-sdk> tag should specify a target API level (the highest verified " +
                            "version; when running on later versions, compatibility behaviors may " +
                            "be enabled) with android:targetSdkVersion=\"?\"",
                    Severity.WARNING, project);
            warning1.line = 6;
            warning1.file = manifest;
            warning1.errorLine = "    <uses-sdk android:minSdkVersion=\"8\" />\n    ^\n";
            warning1.path = "AndroidManifest.xml";
            warning1.location = Location.create(warning1.file,
                    new DefaultPosition(6, 4, 198), new DefaultPosition(6, 42, 236));

            Warning warning2 = new Warning(HardcodedValuesDetector.ISSUE,
                    "Hardcoded string \"Fooo\", should use @string resource",
                    Severity.WARNING, project);
            warning2.line = 11;
            warning2.file = main;
            warning2.errorLine = " (java.lang.String)         android:text=\"Fooo\" />\n" +
                    "        ~~~~~~~~~~~~~~~~~~~\n";
            warning2.path = "res/layout/main.xml";
            warning2.location = Location.create(warning2.file,
                    new DefaultPosition(11, 8, 377), new DefaultPosition(11, 27, 396));

            ImageTestFile icon1 = new ImageTestFile("res/drawable-mdpi/icon.png", 100, 100).fill(0xFFFF00FF);
            ImageTestFile icon2 = new ImageTestFile("res/drawable-340dpi/icon2.png", 100, 100).fill(0xFFFF00FF);
            ImageTestFile icon3 = new ImageTestFile("res/drawable-260dpi/icon3.png", 100, 100).fill(0xFFFF00FF);
            ImageTestFile icon4 = new ImageTestFile("res/drawable-xxhdpi/icon4.png", 100, 100).fill(0xFFFF00FF);
            File iconFile1 = icon1.createFile(projectDir);
            File iconFile2 = icon2.createFile(projectDir);
            File iconFile3 = icon3.createFile(projectDir);
            File iconFile4 = icon4.createFile(projectDir);
            Location location1 = Location.create(iconFile1);
            Location location2 = Location.create(iconFile2);
            Location location3 = Location.create(iconFile3);
            Location location4 = Location.create(iconFile4);
            location1.setSecondary(location2);
            location2.setSecondary(location3);
            location3.setSecondary(location4);

            Warning warning3 = new Warning(IconDetector.DUPLICATES_NAMES,
                    "The following unrelated icon files have identical contents: icon.png, icon2.png, icon3.png, icon4.png",
                    Severity.WARNING, project);
            warning3.file = location1.getFile();
            warning3.path = icon1.targetRelativePath;
            warning3.location = location1;

            List<Warning> warnings = new ArrayList<>();
            warnings.add(warning1);
            warnings.add(warning2);
            warnings.add(warning3);

            reporter.write(new Reporter.Stats(0, 2), warnings);

            String report = Files.toString(reportFile, Charsets.UTF_8);

            // Replace the timestamp to make golden file comparison work
            String timestampPrefix = "Check performed at ";
            int begin = report.indexOf(timestampPrefix);
            assertTrue(begin != -1);
            begin += timestampPrefix.length();
            int end = report.indexOf("</nav>", begin);
            assertTrue(end != -1);
            report = report.substring(0, begin) + "$DATE" + report.substring(end);

            // Not intended to be user configurable; we'll remove the old support soon
            assertTrue("This test is hardcoded for inline resource mode",
                    HtmlReporter.INLINE_RESOURCES);

            // NOTE: If you change the output, please validate it manually in
            //  http://validator.w3.org/#validate_by_input
            // before updating the following
            assertEquals(""
                            + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
                            + "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n"
                            + "\n"
                            + "<head>\n"
                            + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n"
                            + "<title>Lint Report</title>\n"
                            + "<link rel=\"stylesheet\" href=\"https://fonts.googleapis.com/icon?family=Material+Icons\">\n"
                            + " <link rel=\"stylesheet\" href=\"https://code.getmdl.io/1.2.1/material.blue-indigo.min.css\" />\n"
                            + "<link rel=\"stylesheet\" href=\"http://fonts.googleapis.com/css?family=Roboto:300,400,500,700\" type=\"text/css\">\n"
                            + "<script defer src=\"https://code.getmdl.io/1.2.0/material.min.js\"></script>\n"
                            + "<style>\n"
                            + MaterialHtmlReporter.CSS_STYLES
                            + "</style>\n"
                            + "<script language=\"javascript\" type=\"text/javascript\"> \n"
                            + "<!--\n"
                            + "function reveal(id) {\n"
                            + "if (document.getElementById) {\n"
                            + "document.getElementById(id).style.display = 'block';\n"
                            + "document.getElementById(id+'Link').style.display = 'none';\n"
                            + "}\n"
                            + "}\n"
                            + "function hideid(id) {\n"
                            + "if (document.getElementById) {\n"
                            + "document.getElementById(id).style.display = 'none';\n"
                            + "}\n"
                            + "}\n"
                            + "//--> \n"
                            + "</script>\n"
                            + "</head>\n"
                            + "<body class=\"mdl-color--grey-100 mdl-color-text--grey-700 mdl-base\">\n"
                            + "<div class=\"mdl-layout mdl-js-layout mdl-layout--fixed-header\">\n"
                            + "  <header class=\"mdl-layout__header\">\n"
                            + "    <div class=\"mdl-layout__header-row\">\n"
                            + "      <span class=\"mdl-layout-title\">Lint Report: 2 warnings</span>\n"
                            + "      <div class=\"mdl-layout-spacer\"></div>\n"
                            + "      <nav class=\"mdl-navigation mdl-layout--large-screen-only\">\n"
                            + "Check performed at $DATE</nav>\n"
                            + "    </div>\n"
                            + "  </header>\n"
                            + "  <div class=\"mdl-layout__drawer\">\n"
                            + "    <span class=\"mdl-layout-title\">Issue Types</span>\n"
                            + "    <nav class=\"mdl-navigation\">\n"
                            + "      <a class=\"mdl-navigation__link\" href=\"#overview\"><i class=\"material-icons\">dashboard</i>Overview</a>\n"
                            + "      <a class=\"mdl-navigation__link\" href=\"#UsesMinSdkAttributes\"><i class=\"material-icons warning-icon\">warning</i>Minimum SDK and target SDK attributes not defined (1)</a>\n"
                            + "      <a class=\"mdl-navigation__link\" href=\"#HardcodedText\"><i class=\"material-icons warning-icon\">warning</i>Hardcoded text (1)</a>\n"
                            + "      <a class=\"mdl-navigation__link\" href=\"#IconDuplicates\"><i class=\"material-icons warning-icon\">warning</i>Duplicated icons under different names (1)</a>\n"
                            + "    </nav>\n"
                            + "  </div>\n"
                            + "  <main class=\"mdl-layout__content\">\n"
                            + "    <div class=\"mdl-layout__tab-panel is-active\">\n"
                            + "<a name=\"overview\"></a>\n"
                            + "<section class=\"section--center mdl-grid mdl-grid--no-spacing mdl-shadow--2dp\" id=\"OverviewCard\" style=\"display: block;\">\n"
                            + "            <div class=\"mdl-card mdl-cell mdl-cell--12-col\">\n"
                            + "  <div class=\"mdl-card__title\">\n"
                            + "    <h2 class=\"mdl-card__title-text\">Overview</h2>\n"
                            + "  </div>\n"
                            + "              <div class=\"mdl-card__supporting-text\">\n"
                            + "<table class=\"overview\">\n"
                            + "<tr><td class=\"countColumn\"></td><td class=\"categoryColumn\"><a href=\"#Correctness\">Correctness</a>\n"
                            + "</td></tr>\n"
                            + "<tr>\n"
                            + "<td class=\"countColumn\">1</td><td class=\"issueColumn\"><i class=\"material-icons warning-icon\">warning</i>\n"
                            + "<a href=\"#UsesMinSdkAttributes\">UsesMinSdkAttributes</a>: Minimum SDK and target SDK attributes not defined</td></tr>\n"
                            + "<tr><td class=\"countColumn\"></td><td class=\"categoryColumn\"><a href=\"#Internationalization\">Internationalization</a>\n"
                            + "</td></tr>\n"
                            + "<tr>\n"
                            + "<td class=\"countColumn\">1</td><td class=\"issueColumn\"><i class=\"material-icons warning-icon\">warning</i>\n"
                            + "<a href=\"#HardcodedText\">HardcodedText</a>: Hardcoded text</td></tr>\n"
                            + "<tr><td class=\"countColumn\"></td><td class=\"categoryColumn\"><a href=\"#Usability:Icons\">Usability:Icons</a>\n"
                            + "</td></tr>\n"
                            + "<tr>\n"
                            + "<td class=\"countColumn\">1</td><td class=\"issueColumn\"><i class=\"material-icons warning-icon\">warning</i>\n"
                            + "<a href=\"#IconDuplicates\">IconDuplicates</a>: Duplicated icons under different names</td></tr>\n"
                            + "</table>\n"
                            + "<br/>              </div>\n"
                            + "              <div class=\"mdl-card__actions mdl-card--border\">\n"
                            + "<button class=\"mdl-button mdl-js-button mdl-js-ripple-effect\" id=\"OverviewCardLink\" onclick=\"hideid('OverviewCard');\">\n"
                            + "Dismiss</button>            </div>\n"
                            + "            </div>\n"
                            + "          </section>\n"
                            + "<a name=\"Correctness\"></a>\n"
                            + "<a name=\"UsesMinSdkAttributes\"></a>\n"
                            + "<section class=\"section--center mdl-grid mdl-grid--no-spacing mdl-shadow--2dp\" id=\"UsesMinSdkAttributesCard\" style=\"display: block;\">\n"
                            + "            <div class=\"mdl-card mdl-cell mdl-cell--12-col\">\n"
                            + "  <div class=\"mdl-card__title\">\n"
                            + "    <h2 class=\"mdl-card__title-text\">Minimum SDK and target SDK attributes not defined</h2>\n"
                            + "  </div>\n"
                            + "              <div class=\"mdl-card__supporting-text\">\n"
                            + "<div class=\"issue\">\n"
                            + "<div class=\"warningslist\">\n"
                            + "<span class=\"location\"><a href=\"AndroidManifest.xml\">AndroidManifest.xml</a>:7</span>: <span class=\"message\">&lt;uses-sdk> tag should specify a target API level (the highest verified version; when running on later versions, compatibility behaviors may be enabled) with android:targetSdkVersion=\"?\"</span><br />\n"
                            + "</div>\n"
                            + "<div class=\"metadata\"><div class=\"explanation\" id=\"explanationUsesMinSdkAttributes\" style=\"display: none;\">\n"
                            + "The manifest should contain a <code>&lt;uses-sdk></code> element which defines the minimum API Level required for the application to run, as well as the target version (the highest API level you have tested the version for.)<br/><div class=\"moreinfo\">More info: <a href=\"http://developer.android.com/guide/topics/manifest/uses-sdk-element.html\">http://developer.android.com/guide/topics/manifest/uses-sdk-element.html</a>\n"
                            + "</div>To suppress this error, use the issue id \"UsesMinSdkAttributes\" as explained in the <a href=\"#SuppressInfo\">Suppressing Warnings and Errors</a> section.<br/>\n"
                            + "<br/></div>\n"
                            + "</div>\n"
                            + "</div>\n"
                            + "<div class=\"chips\">\n"
                            + "<span class=\"mdl-chip\">\n"
                            + "    <span class=\"mdl-chip__text\">UsesMinSdkAttributes</span>\n"
                            + "</span>\n"
                            + "<span class=\"mdl-chip\">\n"
                            + "    <span class=\"mdl-chip__text\">Correctness</span>\n"
                            + "</span>\n"
                            + "<span class=\"mdl-chip\">\n"
                            + "    <span class=\"mdl-chip__text\">Warning</span>\n"
                            + "</span>\n"
                            + "<span class=\"mdl-chip\">\n"
                            + "    <span class=\"mdl-chip__text\">Priority 9/10</span>\n"
                            + "</span>\n"
                            + "</div>\n"
                            + "              </div>\n"
                            + "              <div class=\"mdl-card__actions mdl-card--border\">\n"
                            + "<button class=\"mdl-button mdl-js-button mdl-js-ripple-effect\" id=\"explanationUsesMinSdkAttributesLink\" onclick=\"reveal('explanationUsesMinSdkAttributes');\">\n"
                            + "Explain</button><button class=\"mdl-button mdl-js-button mdl-js-ripple-effect\" id=\"UsesMinSdkAttributesCardLink\" onclick=\"hideid('UsesMinSdkAttributesCard');\">\n"
                            + "Dismiss</button>            </div>\n"
                            + "            </div>\n"
                            + "          </section>\n"
                            + "<a name=\"Internationalization\"></a>\n"
                            + "<a name=\"HardcodedText\"></a>\n"
                            + "<section class=\"section--center mdl-grid mdl-grid--no-spacing mdl-shadow--2dp\" id=\"HardcodedTextCard\" style=\"display: block;\">\n"
                            + "            <div class=\"mdl-card mdl-cell mdl-cell--12-col\">\n"
                            + "  <div class=\"mdl-card__title\">\n"
                            + "    <h2 class=\"mdl-card__title-text\">Hardcoded text</h2>\n"
                            + "  </div>\n"
                            + "              <div class=\"mdl-card__supporting-text\">\n"
                            + "<div class=\"issue\">\n"
                            + "<div class=\"warningslist\">\n"
                            + "<span class=\"location\"><a href=\"res/layout/main.xml\">res/layout/main.xml</a>:12</span>: <span class=\"message\">Hardcoded string \"Fooo\", should use @string resource</span><br />\n"
                            + "</div>\n"
                            + "<div class=\"metadata\"><div class=\"explanation\" id=\"explanationHardcodedText\" style=\"display: none;\">\n"
                            + "Hardcoding text attributes directly in layout files is bad for several reasons:<br/>\n"
                            + "<br/>\n"
                            + "* When creating configuration variations (for example for landscape or portrait)you have to repeat the actual text (and keep it up to date when making changes)<br/>\n"
                            + "<br/>\n"
                            + "* The application cannot be translated to other languages by just adding new translations for existing string resources.<br/>\n"
                            + "<br/>\n"
                            + "There are quickfixes to automatically extract this hardcoded string into a resource lookup.<br/>To suppress this error, use the issue id \"HardcodedText\" as explained in the <a href=\"#SuppressInfo\">Suppressing Warnings and Errors</a> section.<br/>\n"
                            + "<br/></div>\n"
                            + "</div>\n"
                            + "</div>\n"
                            + "<div class=\"chips\">\n"
                            + "<span class=\"mdl-chip\">\n"
                            + "    <span class=\"mdl-chip__text\">HardcodedText</span>\n"
                            + "</span>\n"
                            + "<span class=\"mdl-chip\">\n"
                            + "    <span class=\"mdl-chip__text\">Internationalization</span>\n"
                            + "</span>\n"
                            + "<span class=\"mdl-chip\">\n"
                            + "    <span class=\"mdl-chip__text\">Warning</span>\n"
                            + "</span>\n"
                            + "<span class=\"mdl-chip\">\n"
                            + "    <span class=\"mdl-chip__text\">Priority 5/10</span>\n"
                            + "</span>\n"
                            + "</div>\n"
                            + "              </div>\n"
                            + "              <div class=\"mdl-card__actions mdl-card--border\">\n"
                            + "<button class=\"mdl-button mdl-js-button mdl-js-ripple-effect\" id=\"explanationHardcodedTextLink\" onclick=\"reveal('explanationHardcodedText');\">\n"
                            + "Explain</button><button class=\"mdl-button mdl-js-button mdl-js-ripple-effect\" id=\"HardcodedTextCardLink\" onclick=\"hideid('HardcodedTextCard');\">\n"
                            + "Dismiss</button>            </div>\n"
                            + "            </div>\n"
                            + "          </section>\n"
                            + "<a name=\"Usability:Icons\"></a>\n"
                            + "<a name=\"IconDuplicates\"></a>\n"
                            + "<section class=\"section--center mdl-grid mdl-grid--no-spacing mdl-shadow--2dp\" id=\"IconDuplicatesCard\" style=\"display: block;\">\n"
                            + "            <div class=\"mdl-card mdl-cell mdl-cell--12-col\">\n"
                            + "  <div class=\"mdl-card__title\">\n"
                            + "    <h2 class=\"mdl-card__title-text\">Duplicated icons under different names</h2>\n"
                            + "  </div>\n"
                            + "              <div class=\"mdl-card__supporting-text\">\n"
                            + "<div class=\"issue\">\n"
                            + "<div class=\"warningslist\">\n"
                            + "<span class=\"location\"><a href=\"res/drawable-mdpi/icon.png\">res/drawable-mdpi/icon.png</a></span>: <span class=\"message\">The following unrelated icon files have identical contents: icon.png, icon2.png, icon3.png, icon4.png</span><br />\n"
                            + "<ul></ul><button id=\"Location1DivLink\" onclick=\"reveal('Location1Div');\" />+ 3 Additional Locations...</button>\n"
                            + "<div id=\"Location1Div\" style=\"display: none\">\n"
                            + "Additional locations: <ul>\n"
                            + "<li> <span class=\"location\"><a href=\"res/drawable-340dpi/icon2.png\">res/drawable-340dpi/icon2.png</a></span>\n"
                            + "<li> <span class=\"location\"><a href=\"res/drawable-260dpi/icon3.png\">res/drawable-260dpi/icon3.png</a></span>\n"
                            + "<li> <span class=\"location\"><a href=\"res/drawable-xxhdpi/icon4.png\">res/drawable-xxhdpi/icon4.png</a></span>\n"
                            + "</ul>\n"
                            + "</div><br/><br/>\n"
                            + "<table>\n"
                            + "<tr><td><a href=\"res/drawable-mdpi/icon.png\"><img border=\"0\" align=\"top\" src=\"res/drawable-mdpi/icon.png\" /></a>\n"
                            + "</td><td><a href=\"res/drawable-260dpi/icon3.png\"><img border=\"0\" align=\"top\" src=\"res/drawable-260dpi/icon3.png\" /></a>\n"
                            + "</td><td><a href=\"res/drawable-340dpi/icon2.png\"><img border=\"0\" align=\"top\" src=\"res/drawable-340dpi/icon2.png\" /></a>\n"
                            + "</td><td><a href=\"res/drawable-xxhdpi/icon4.png\"><img border=\"0\" align=\"top\" src=\"res/drawable-xxhdpi/icon4.png\" /></a>\n"
                            + "</td></tr><tr><th>mdpi</th><th>260dpi</th><th>340dpi</th><th>xxhdpi</th></tr>\n"
                            + "</table>\n"
                            + "</div>\n"
                            + "<div class=\"metadata\"><div class=\"explanation\" id=\"explanationIconDuplicates\" style=\"display: none;\">\n"
                            + "If an icon is repeated under different names, you can consolidate and just use one of the icons and delete the others to make your application smaller. However, duplicated icons usually are not intentional and can sometimes point to icons that were accidentally overwritten or accidentally not updated.<br/>To suppress this error, use the issue id \"IconDuplicates\" as explained in the <a href=\"#SuppressInfo\">Suppressing Warnings and Errors</a> section.<br/>\n"
                            + "<br/></div>\n"
                            + "</div>\n"
                            + "</div>\n"
                            + "<div class=\"chips\">\n"
                            + "<span class=\"mdl-chip\">\n"
                            + "    <span class=\"mdl-chip__text\">IconDuplicates</span>\n"
                            + "</span>\n"
                            + "<span class=\"mdl-chip\">\n"
                            + "    <span class=\"mdl-chip__text\">Icons</span>\n"
                            + "</span>\n"
                            + "<span class=\"mdl-chip\">\n"
                            + "    <span class=\"mdl-chip__text\">Usability</span>\n"
                            + "</span>\n"
                            + "<span class=\"mdl-chip\">\n"
                            + "    <span class=\"mdl-chip__text\">Warning</span>\n"
                            + "</span>\n"
                            + "<span class=\"mdl-chip\">\n"
                            + "    <span class=\"mdl-chip__text\">Priority 3/10</span>\n"
                            + "</span>\n"
                            + "</div>\n"
                            + "              </div>\n"
                            + "              <div class=\"mdl-card__actions mdl-card--border\">\n"
                            + "<button class=\"mdl-button mdl-js-button mdl-js-ripple-effect\" id=\"explanationIconDuplicatesLink\" onclick=\"reveal('explanationIconDuplicates');\">\n"
                            + "Explain</button><button class=\"mdl-button mdl-js-button mdl-js-ripple-effect\" id=\"IconDuplicatesCardLink\" onclick=\"hideid('IconDuplicatesCard');\">\n"
                            + "Dismiss</button>            </div>\n"
                            + "            </div>\n"
                            + "          </section>\n"
                            + "<a name=\"MissingIssues\"></a>\n"
                            + "<section class=\"section--center mdl-grid mdl-grid--no-spacing mdl-shadow--2dp\" id=\"MissingIssuesCard\" style=\"display: block;\">\n"
                            + "            <div class=\"mdl-card mdl-cell mdl-cell--12-col\">\n"
                            + "  <div class=\"mdl-card__title\">\n"
                            + "    <h2 class=\"mdl-card__title-text\">Disabled Checks</h2>\n"
                            + "  </div>\n"
                            + "              <div class=\"mdl-card__supporting-text\">\n"
                            + "One or more issues were not run by lint, either \n"
                            + "because the check is not enabled by default, or because \n"
                            + "it was disabled with a command line flag or via one or \n"
                            + "more <code>lint.xml</code> configuration files in the project directories.\n"
                            + "<div id=\"SuppressedIssues\" style=\"display: none;\"><br/><br/></div>              </div>\n"
                            + "              <div class=\"mdl-card__actions mdl-card--border\">\n"
                            + "<button class=\"mdl-button mdl-js-button mdl-js-ripple-effect\" id=\"SuppressedIssuesLink\" onclick=\"reveal('SuppressedIssues');\">\n"
                            + "List Missing Issues</button><button class=\"mdl-button mdl-js-button mdl-js-ripple-effect\" id=\"MissingIssuesCardLink\" onclick=\"hideid('MissingIssuesCard');\">\n"
                            + "Dismiss</button>            </div>\n"
                            + "            </div>\n"
                            + "          </section>\n"
                            + "<a name=\"SuppressInfo\"></a>\n"
                            + "<section class=\"section--center mdl-grid mdl-grid--no-spacing mdl-shadow--2dp\" id=\"SuppressCard\" style=\"display: block;\">\n"
                            + "            <div class=\"mdl-card mdl-cell mdl-cell--12-col\">\n"
                            + "  <div class=\"mdl-card__title\">\n"
                            + "    <h2 class=\"mdl-card__title-text\">Suppressing Warnings and Errors</h2>\n"
                            + "  </div>\n"
                            + "              <div class=\"mdl-card__supporting-text\">\n"
                            + "Lint errors can be suppressed in a variety of ways:<br/>\n"
                            + "<br/>\n"
                            + "1. With a <code>@SuppressLint</code> annotation in the Java code<br/>\n"
                            + "2. With a <code>tools:ignore</code> attribute in the XML file<br/>\n"
                            + "3. With a //noinspection comment in the source code<br/>\n"
                            + "4. With ignore flags specified in the <code>build.gradle</code> file, as explained below<br/>\n"
                            + "5. With a <code>lint.xml</code> configuration file in the project<br/>\n"
                            + "6. With a <code>lint.xml</code> configuration file passed to lint via the --config flag<br/>\n"
                            + "7. With the --ignore flag passed to lint.<br/>\n"
                            + "<br/>\n"
                            + "To suppress a lint warning with an annotation, add a <code>@SuppressLint(\"id\")</code> annotation on the class, method or variable declaration closest to the warning instance you want to disable. The id can be one or more issue id's, such as <code>\"UnusedResources\"</code> or <code>{\"UnusedResources\",\"UnusedIds\"}</code>, or it can be <code>\"all\"</code> to suppress all lint warnings in the given scope.<br/>\n"
                            + "<br/>\n"
                            + "To suppress a lint warning with a comment, add a <code>//noinspection id</code> comment on the line before the statement with the error.<br/>\n"
                            + "<br/>\n"
                            + "To suppress a lint warning in an XML file, add a <code>tools:ignore=\"id\"</code> attribute on the element containing the error, or one of its surrounding elements. You also need to define the namespace for the tools prefix on the root element in your document, next to the <code>xmlns:android</code> declaration:<br/>\n"
                            + "<code>xmlns:tools=\"http://schemas.android.com/tools\"</code><br/>\n"
                            + "<br/>\n"
                            + "To suppress a lint warning in a <code>build.gradle</code> file, add a section like this:<br/>\n"
                            + "<br/>\n"
                            + "android {<br/>\n"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;lintOptions {<br/>\n"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;disable 'TypographyFractions','TypographyQuotes'<br/>\n"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;}<br/>\n"
                            + "}<br/>\n"
                            + "<br/>\n"
                            + "Here we specify a comma separated list of issue id's after the disable command. You can also use <code>warning</code> or <code>error</code> instead of <code>disable</code> to change the severity of issues.<br/>\n"
                            + "<br/>\n"
                            + "To suppress lint warnings with a configuration XML file, create a file named <code>lint.xml</code> and place it at the root directory of the module in which it applies.<br/>\n"
                            + "<br/>\n"
                            + "The format of the <code>lint.xml</code> file is something like the following:<br/>\n"
                            + "<br/>\n"
                            + "&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?><br/>\n"
                            + "&lt;lint><br/>\n"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&lt;!-- Ignore everything in the test source set --><br/>\n"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&lt;issue id=\"all\"><br/>\n"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;ignore path=\"*/test/*\" /><br/>\n"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&lt;/issue><br/>\n"
                            + "<br/>\n"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&lt;!-- Disable this given check in this project --><br/>\n"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&lt;issue id=\"IconMissingDensityFolder\" severity=\"ignore\" /><br/>\n"
                            + "<br/>\n"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&lt;!-- Ignore the ObsoleteLayoutParam issue in the given files --><br/>\n"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&lt;issue id=\"ObsoleteLayoutParam\"><br/>\n"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;ignore path=\"res/layout/activation.xml\" /><br/>\n"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;ignore path=\"res/layout-xlarge/activation.xml\" /><br/>\n"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;ignore regexp=\"(foo|bar).java\" /><br/>\n"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&lt;/issue><br/>\n"
                            + "<br/>\n"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&lt;!-- Ignore the UselessLeaf issue in the given file --><br/>\n"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&lt;issue id=\"UselessLeaf\"><br/>\n"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;ignore path=\"res/layout/main.xml\" /><br/>\n"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&lt;/issue><br/>\n"
                            + "<br/>\n"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&lt;!-- Change the severity of hardcoded strings to \"error\" --><br/>\n"
                            + "&nbsp;&nbsp;&nbsp;&nbsp;&lt;issue id=\"HardcodedText\" severity=\"error\" /><br/>\n"
                            + "&lt;/lint><br/>\n"
                            + "<br/>\n"
                            + "To suppress lint checks from the command line, pass the --ignore flag with a comma separated list of ids to be suppressed, such as:<br/>\n"
                            + "<code>$ lint --ignore UnusedResources,UselessLeaf /my/project/path</code><br/>\n"
                            + "<br/>\n"
                            + "For more information, see <a href=\"http://g.co/androidstudio/suppressing-lint-warnings\">http://g.co/androidstudio/suppressing-lint-warnings</a><br/>\n"
                            + "\n"
                            + "            </div>\n"
                            + "            </div>\n"
                            + "          </section>    </div>\n"
                            + "  </main>\n"
                            + "</div>\n"
                            + "</body>\n"
                            + "</html>",
                    report.replace(File.separatorChar, '/'));
        } finally {
            deleteFile(projectDir);
        }
    }

    @Override
    protected Detector getDetector() {
        fail("Not used in this test");
        return null;
    }
}
