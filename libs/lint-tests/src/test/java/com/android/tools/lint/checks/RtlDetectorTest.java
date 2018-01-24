/*
 * Copyright (C) 2012, 2017 The Android Open Source Project
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

import static com.android.SdkConstants.ATTR_DRAWABLE_RIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_END;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_LEFT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_START;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_RIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_END;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.tools.lint.checks.RtlDetector.ATTRIBUTES;
import static com.android.tools.lint.checks.RtlDetector.convertNewToOld;
import static com.android.tools.lint.checks.RtlDetector.convertOldToNew;
import static com.android.tools.lint.checks.RtlDetector.convertToOppositeDirection;
import static com.android.tools.lint.checks.RtlDetector.isRtlAttributeName;

import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("javadoc")
public class RtlDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new RtlDetector();
    }

    @Override
    protected boolean allowCompilationErrors() {
        // Some of these unit tests are still relying on source code that references
        // unresolved symbols etc.
        return true;
    }

    private Set<Issue> mEnabled = new HashSet<>();
    private static final Set<Issue> ALL = new HashSet<>();
    static {
        ALL.add(RtlDetector.USE_START);
        ALL.add(RtlDetector.ENABLED);
        ALL.add(RtlDetector.COMPAT);
        ALL.add(RtlDetector.SYMMETRY);
    }

    public void testIsRtlAttributeName() {
        assertTrue(isRtlAttributeName(ATTR_LAYOUT_ALIGN_PARENT_START));
        assertTrue(isRtlAttributeName(ATTR_LAYOUT_MARGIN_END));
        assertTrue(isRtlAttributeName(ATTR_LAYOUT_ALIGN_END));
        assertFalse(isRtlAttributeName(ATTR_LAYOUT_ALIGN_PARENT_LEFT));
        assertFalse(isRtlAttributeName(ATTR_DRAWABLE_RIGHT));
        assertFalse(isRtlAttributeName(ATTR_LAYOUT_ALIGN_RIGHT));
        assertFalse(isRtlAttributeName(ATTR_NAME));
    }

    @Override
    protected boolean isEnabled(Issue issue) {
        return super.isEnabled(issue) && mEnabled.contains(issue);
    }

    public void testTarget14WithRtl() throws Exception {
        mEnabled = ALL;
        //noinspection all // Sample code
        assertEquals(""
                + "No warnings.",

                lintProject(
                        projectProperties().compileSdk(17),
                        mMinsdk5targetsdk14,
                        mRtl
                ));
    }

    public void testTarget17WithRtl() throws Exception {
        mEnabled = ALL;
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/rtl.xml:14: Warning: Use \"start\" instead of \"left\" to ensure correct behavior in right-to-left locales [RtlHardcoded]\n"
                + "        android:layout_gravity=\"left\"\n"
                + "                                ~~~~\n"
                + "res/layout/rtl.xml:22: Warning: Use \"end\" instead of \"right\" to ensure correct behavior in right-to-left locales [RtlHardcoded]\n"
                + "        android:layout_gravity=\"right\"\n"
                + "                                ~~~~~\n"
                + "res/layout/rtl.xml:30: Warning: Use \"end\" instead of \"right\" to ensure correct behavior in right-to-left locales [RtlHardcoded]\n"
                + "        android:gravity=\"right\"\n"
                + "                         ~~~~~\n"
                + "AndroidManifest.xml: Warning: The project references RTL attributes, but does not explicitly enable or disable RTL support with android:supportsRtl in the manifest [RtlEnabled]\n"
                + "0 errors, 4 warnings\n",

                lintProject(
                        projectProperties().compileSdk(17),
                        manifest().minSdk(5).targetSdk(17),
                        mRtl
                ));
    }

    public void testTarget14() throws Exception {
        mEnabled = ALL;
        //noinspection all // Sample code
        assertEquals(""
                + "No warnings.",

                lintProject(
                        projectProperties().compileSdk(17),
                        mMinsdk5targetsdk14
                ));
    }

    public void testOlderCompilationTarget() throws Exception {
        mEnabled = ALL;
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",

                lintProject(
                        source("project.properties", ""
                                + "target=android-14\n"
                                + "proguard.config=${sdk.dir}/foo.cfg:${user.home}/bar.pro;myfile.txt\n"),
                        manifest().minSdk(5).targetSdk(17),
                        mRtl
                ));
    }

    public void testUseStart() throws Exception {
        mEnabled = Collections.singleton(RtlDetector.USE_START);
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/rtl.xml:14: Warning: Use \"start\" instead of \"left\" to ensure correct behavior in right-to-left locales [RtlHardcoded]\n"
                + "        android:layout_gravity=\"left\"\n"
                + "                                ~~~~\n"
                + "res/layout/rtl.xml:22: Warning: Use \"end\" instead of \"right\" to ensure correct behavior in right-to-left locales [RtlHardcoded]\n"
                + "        android:layout_gravity=\"right\"\n"
                + "                                ~~~~~\n"
                + "res/layout/rtl.xml:30: Warning: Use \"end\" instead of \"right\" to ensure correct behavior in right-to-left locales [RtlHardcoded]\n"
                + "        android:gravity=\"right\"\n"
                + "                         ~~~~~\n"
                + "0 errors, 3 warnings\n",

                lintProject(
                        projectProperties().compileSdk(17),
                        manifest().minSdk(5).targetSdk(17),
                        mRtl
                ));
    }

    public void testTarget17Rtl() throws Exception {
        mEnabled = Collections.singleton(RtlDetector.USE_START);
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/rtl.xml:14: Warning: Use \"start\" instead of \"left\" to ensure correct behavior in right-to-left locales [RtlHardcoded]\n"
                + "        android:layout_gravity=\"left\"\n"
                + "                                ~~~~\n"
                + "res/layout/rtl.xml:22: Warning: Use \"end\" instead of \"right\" to ensure correct behavior in right-to-left locales [RtlHardcoded]\n"
                + "        android:layout_gravity=\"right\"\n"
                + "                                ~~~~~\n"
                + "res/layout/rtl.xml:30: Warning: Use \"end\" instead of \"right\" to ensure correct behavior in right-to-left locales [RtlHardcoded]\n"
                + "        android:gravity=\"right\"\n"
                + "                         ~~~~~\n"
                + "0 errors, 3 warnings\n",

                lintProject(
                        projectProperties().compileSdk(17),
                        mMin17rtl,
                        mRtl
                ));
    }

    public void testRelativeLayoutInOld() throws Exception {
        mEnabled = Collections.singleton(RtlDetector.USE_START);
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/relative.xml:10: Warning: Consider adding android:layout_alignParentStart=\"true\" to better support right-to-left layouts [RtlHardcoded]\n"
                + "        android:layout_alignParentLeft=\"true\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:13: Warning: Consider adding android:layout_marginStart=\"40dip\" to better support right-to-left layouts [RtlHardcoded]\n"
                + "        android:layout_marginLeft=\"40dip\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:24: Warning: Consider adding android:layout_marginStart=\"40dip\" to better support right-to-left layouts [RtlHardcoded]\n"
                + "        android:layout_marginLeft=\"40dip\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:26: Warning: Consider adding android:layout_toEndOf=\"@id/loading_progress\" to better support right-to-left layouts [RtlHardcoded]\n"
                + "        android:layout_toRightOf=\"@id/loading_progress\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:29: Warning: Consider adding android:paddingEnd=\"120dip\" to better support right-to-left layouts [RtlHardcoded]\n"
                + "        android:paddingRight=\"120dip\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:37: Warning: Consider adding android:layout_alignParentStart=\"true\" to better support right-to-left layouts [RtlHardcoded]\n"
                + "        android:layout_alignParentLeft=\"true\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:38: Warning: Consider adding android:layout_alignEnd=\"@id/text\" to better support right-to-left layouts [RtlHardcoded]\n"
                + "        android:layout_alignRight=\"@id/text\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:47: Warning: Consider adding android:layout_alignStart=\"@id/cancel\" to better support right-to-left layouts [RtlHardcoded]\n"
                + "        android:layout_alignLeft=\"@id/cancel\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:48: Warning: Consider adding android:layout_alignEnd=\"@id/cancel\" to better support right-to-left layouts [RtlHardcoded]\n"
                + "        android:layout_alignRight=\"@id/cancel\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 9 warnings\n",

                lintProject(
                        projectProperties().compileSdk(17),
                        manifest().minSdk(5).targetSdk(17),
                        mRelative
                ));
    }

    public void testRelativeLayoutInNew() throws Exception {
        mEnabled = Collections.singleton(RtlDetector.USE_START);
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/relative.xml:10: Warning: Consider replacing android:layout_alignParentLeft with android:layout_alignParentStart=\"true\" to better support right-to-left layouts [RtlHardcoded]\n"
                + "        android:layout_alignParentLeft=\"true\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:13: Warning: Consider replacing android:layout_marginLeft with android:layout_marginStart=\"40dip\" to better support right-to-left layouts [RtlHardcoded]\n"
                + "        android:layout_marginLeft=\"40dip\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:24: Warning: Consider replacing android:layout_marginLeft with android:layout_marginStart=\"40dip\" to better support right-to-left layouts [RtlHardcoded]\n"
                + "        android:layout_marginLeft=\"40dip\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:26: Warning: Consider replacing android:layout_toRightOf with android:layout_toEndOf=\"@id/loading_progress\" to better support right-to-left layouts [RtlHardcoded]\n"
                + "        android:layout_toRightOf=\"@id/loading_progress\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:29: Warning: Consider replacing android:paddingRight with android:paddingEnd=\"120dip\" to better support right-to-left layouts [RtlHardcoded]\n"
                + "        android:paddingRight=\"120dip\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:37: Warning: Consider replacing android:layout_alignParentLeft with android:layout_alignParentStart=\"true\" to better support right-to-left layouts [RtlHardcoded]\n"
                + "        android:layout_alignParentLeft=\"true\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:38: Warning: Consider replacing android:layout_alignRight with android:layout_alignEnd=\"@id/text\" to better support right-to-left layouts [RtlHardcoded]\n"
                + "        android:layout_alignRight=\"@id/text\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:47: Warning: Consider replacing android:layout_alignLeft with android:layout_alignStart=\"@id/cancel\" to better support right-to-left layouts [RtlHardcoded]\n"
                + "        android:layout_alignLeft=\"@id/cancel\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:48: Warning: Consider replacing android:layout_alignRight with android:layout_alignEnd=\"@id/cancel\" to better support right-to-left layouts [RtlHardcoded]\n"
                + "        android:layout_alignRight=\"@id/cancel\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 9 warnings\n",

                lintProject(
                        projectProperties().compileSdk(17),
                        mMin17rtl,
                        mRelative
                ));
    }

    public void testRelativeLayoutCompat() throws Exception {
        //noinspection all // Sample code
        String expected = ""
                + "res/layout/relative.xml:10: Error: To support older versions than API 17 (project specifies 5) you should also add android:layout_alignParentLeft=\"true\" [RtlCompat]\n"
                + "        android:layout_alignParentStart=\"true\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:13: Error: To support older versions than API 17 (project specifies 5) you should also add android:layout_marginLeft=\"40dip\" [RtlCompat]\n"
                + "        android:layout_marginStart=\"40dip\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:24: Error: To support older versions than API 17 (project specifies 5) you should also add android:layout_marginLeft=\"40dip\" [RtlCompat]\n"
                + "        android:layout_marginStart=\"40dip\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:26: Error: To support older versions than API 17 (project specifies 5) you should also add android:layout_toRightOf=\"@id/loading_progress\" [RtlCompat]\n"
                + "        android:layout_toEndOf=\"@id/loading_progress\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:29: Error: To support older versions than API 17 (project specifies 5) you should also add android:paddingRight=\"120dip\" [RtlCompat]\n"
                + "        android:paddingEnd=\"120dip\"\n"
                + "        ~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:37: Error: To support older versions than API 17 (project specifies 5) you should also add android:layout_alignParentLeft=\"true\" [RtlCompat]\n"
                + "        android:layout_alignParentStart=\"true\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:38: Error: To support older versions than API 17 (project specifies 5) you should also add android:layout_alignRight=\"@id/text\" [RtlCompat]\n"
                + "        android:layout_alignEnd=\"@id/text\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:47: Error: To support older versions than API 17 (project specifies 5) you should also add android:layout_alignLeft=\"@id/cancel\" [RtlCompat]\n"
                + "        android:layout_alignStart=\"@id/cancel\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/layout/relative.xml:48: Error: To support older versions than API 17 (project specifies 5) you should also add android:layout_alignRight=\"@id/cancel\" [RtlCompat]\n"
                + "        android:layout_alignEnd=\"@id/cancel\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "9 errors, 0 warnings\n";
        lint().files(
                projectProperties().compileSdk(17),
                manifest().minSdk(5).targetSdk(17),
                xml("res/layout/relative.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"wrap_content\"\n"
                        + "    android:layout_height=\"wrap_content\" >\n"
                        + "\n"
                        + "    <ProgressBar\n"
                        + "        android:id=\"@+id/loading_progress\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:layout_alignParentStart=\"true\"\n"
                        + "        android:layout_alignParentTop=\"true\"\n"
                        + "        android:layout_marginBottom=\"60dip\"\n"
                        + "        android:layout_marginStart=\"40dip\"\n"
                        + "        android:layout_marginTop=\"40dip\"\n"
                        + "        android:max=\"10000\" />\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:id=\"@+id/text\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:layout_alignParentTop=\"true\"\n"
                        + "        android:layout_alignWithParentIfMissing=\"true\"\n"
                        + "        android:layout_marginBottom=\"60dip\"\n"
                        + "        android:layout_marginStart=\"40dip\"\n"
                        + "        android:layout_marginTop=\"40dip\"\n"
                        + "        android:layout_toEndOf=\"@id/loading_progress\"\n"
                        + "        android:ellipsize=\"end\"\n"
                        + "        android:maxLines=\"3\"\n"
                        + "        android:paddingEnd=\"120dip\"\n"
                        + "        android:text=\"@string/creating_instant_mix\"\n"
                        + "        android:textAppearance=\"?android:attr/textAppearanceMedium\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:id=\"@+id/cancel\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:layout_alignParentStart=\"true\"\n"
                        + "        android:layout_alignEnd=\"@id/text\"\n"
                        + "        android:layout_below=\"@id/text\"\n"
                        + "        android:background=\"@null\"\n"
                        + "        android:text=\"@string/cancel\" />\n"
                        + "\n"
                        + "    <ImageView\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:layout_above=\"@id/cancel\"\n"
                        + "        android:layout_alignStart=\"@id/cancel\"\n"
                        + "        android:layout_alignEnd=\"@id/cancel\"\n"
                        + "        android:scaleType=\"fitXY\"\n"
                        + "        android:src=\"@drawable/menu_list_divider\" />\n"
                        + "\n"
                        + "</RelativeLayout>\n"))
                .issues(RtlDetector.COMPAT)
                .run()
                .expect(expected)
                .verifyFixes().window(1).expectFixDiffs("Fix for res/layout/relative.xml line 9: Set layout_alignParentLeft=\"true\":\n"
                + "@@ -10 +10\n"
                + "          android:layout_height=\"wrap_content\"\n"
                + "+         android:layout_alignParentLeft=\"true\"\n"
                + "          android:layout_alignParentStart=\"true\"\n"
                + "Fix for res/layout/relative.xml line 12: Set layout_marginLeft=\"40dip\":\n"
                + "@@ -13 +13\n"
                + "          android:layout_marginBottom=\"60dip\"\n"
                + "+         android:layout_marginLeft=\"40dip\"\n"
                + "          android:layout_marginStart=\"40dip\"\n"
                + "Fix for res/layout/relative.xml line 23: Set layout_marginLeft=\"40dip\":\n"
                + "@@ -24 +24\n"
                + "          android:layout_marginBottom=\"60dip\"\n"
                + "+         android:layout_marginLeft=\"40dip\"\n"
                + "          android:layout_marginStart=\"40dip\"\n"
                + "Fix for res/layout/relative.xml line 25: Set layout_toRightOf=\"@id/loading_progress\":\n"
                + "@@ -27 +27\n"
                + "          android:layout_toEndOf=\"@id/loading_progress\"\n"
                + "+         android:layout_toRightOf=\"@id/loading_progress\"\n"
                + "          android:ellipsize=\"end\"\n"
                + "Fix for res/layout/relative.xml line 28: Set paddingRight=\"120dip\":\n"
                + "@@ -30 +30\n"
                + "          android:paddingEnd=\"120dip\"\n"
                + "+         android:paddingRight=\"120dip\"\n"
                + "          android:text=\"@string/creating_instant_mix\"\n"
                + "Fix for res/layout/relative.xml line 36: Set layout_alignParentLeft=\"true\":\n"
                + "@@ -38 +38\n"
                + "          android:layout_alignEnd=\"@id/text\"\n"
                + "+         android:layout_alignParentLeft=\"true\"\n"
                + "          android:layout_alignParentStart=\"true\"\n"
                + "Fix for res/layout/relative.xml line 37: Set layout_alignRight=\"@id/text\":\n"
                + "@@ -39 +39\n"
                + "          android:layout_alignParentStart=\"true\"\n"
                + "+         android:layout_alignRight=\"@id/text\"\n"
                + "          android:layout_below=\"@id/text\"\n"
                + "Fix for res/layout/relative.xml line 46: Set layout_alignLeft=\"@id/cancel\":\n"
                + "@@ -48 +48\n"
                + "          android:layout_alignEnd=\"@id/cancel\"\n"
                + "+         android:layout_alignLeft=\"@id/cancel\"\n"
                + "          android:layout_alignStart=\"@id/cancel\"\n"
                + "Fix for res/layout/relative.xml line 47: Set layout_alignRight=\"@id/cancel\":\n"
                + "@@ -48 +48\n"
                + "          android:layout_alignEnd=\"@id/cancel\"\n"
                + "+         android:layout_alignRight=\"@id/cancel\"\n"
                + "          android:layout_alignStart=\"@id/cancel\"\n");
    }

    public void testRelativeCompatOk() throws Exception {
        mEnabled = ALL;
        //noinspection all // Sample code
        assertEquals(""
                + "No warnings.",

                lintProject(
                        projectProperties().compileSdk(17),
                        manifest().minSdk(5).targetSdk(17),
                        xml("res/layout/relative.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    android:layout_width=\"wrap_content\"\n"
                            + "    android:layout_height=\"wrap_content\" >\n"
                            + "\n"
                            + "    <ProgressBar\n"
                            + "        android:id=\"@+id/loading_progress\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:layout_alignParentLeft=\"true\"\n"
                            + "        android:layout_alignParentStart=\"true\"\n"
                            + "        android:layout_alignParentTop=\"true\"\n"
                            + "        android:layout_marginBottom=\"60dip\"\n"
                            + "        android:layout_marginLeft=\"40dip\"\n"
                            + "        android:layout_marginStart=\"40dip\"\n"
                            + "        android:layout_marginTop=\"40dip\"\n"
                            + "        android:max=\"10000\" />\n"
                            + "\n"
                            + "    <TextView\n"
                            + "        android:id=\"@+id/text\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:layout_alignParentTop=\"true\"\n"
                            + "        android:layout_alignWithParentIfMissing=\"true\"\n"
                            + "        android:layout_marginBottom=\"60dip\"\n"
                            + "        android:layout_marginLeft=\"40dip\"\n"
                            + "        android:layout_marginStart=\"40dip\"\n"
                            + "        android:layout_marginTop=\"40dip\"\n"
                            + "        android:layout_toRightOf=\"@id/loading_progress\"\n"
                            + "        android:layout_toEndOf=\"@id/loading_progress\"\n"
                            + "        android:ellipsize=\"end\"\n"
                            + "        android:maxLines=\"3\"\n"
                            + "        android:paddingRight=\"120dip\"\n"
                            + "        android:paddingLeft=\"60dip\"\n"
                            + "        android:paddingStart=\"60dip\"\n"
                            + "        android:paddingEnd=\"120dip\"\n"
                            + "        android:text=\"@string/creating_instant_mix\"\n"
                            + "        android:textAppearance=\"?android:attr/textAppearanceMedium\" />\n"
                            + "\n"
                            + "    <Button\n"
                            + "        android:id=\"@+id/cancel\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:layout_alignParentLeft=\"true\"\n"
                            + "        android:layout_alignParentStart=\"true\"\n"
                            + "        android:layout_alignRight=\"@id/text\"\n"
                            + "        android:layout_alignEnd=\"@id/text\"\n"
                            + "        android:layout_below=\"@id/text\"\n"
                            + "        android:background=\"@null\"\n"
                            + "        android:text=\"@string/cancel\" />\n"
                            + "\n"
                            + "    <ImageView\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:layout_above=\"@id/cancel\"\n"
                            + "        android:layout_alignLeft=\"@id/cancel\"\n"
                            + "        android:layout_alignStart=\"@id/cancel\"\n"
                            + "        android:layout_alignRight=\"@id/cancel\"\n"
                            + "        android:layout_alignEnd=\"@id/cancel\"\n"
                            + "        android:scaleType=\"fitXY\"\n"
                            + "        android:src=\"@drawable/menu_list_divider\" />\n"
                            + "\n"
                            + "</RelativeLayout>\n")
                ));
    }

    public void testTarget17NoRtl() throws Exception {
        mEnabled = ALL;
        //noinspection all // Sample code
        assertEquals(""
                + "No warnings.",

                lintProject(
                        projectProperties().compileSdk(17),
                        xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"test.bytecode\"\n"
                            + "    android:versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\" >\n"
                            + "\n"
                            + "    <uses-sdk android:minSdkVersion=\"17\" android:targetSdkVersion=\"18\" />\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\"\n"
                            + "        android:supportsRtl=\"false\">\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                        mRtl
                ));
    }

    public void testRtlQuickFixBelow17() throws Exception {
        //noinspection all // Sample code
        lint().files(
                projectProperties().compileSdk(17),
                manifest().minSdk(16).targetSdk(17),
                mRtlQuickFixed)
                .issues(RtlDetector.USE_START)
                .run()
                .verifyFixes().window(1).expectFixDiffs("Fix for res/layout/rtl_quick_fixed.xml line 9: Add android:layout_marginStart=\"35dip\":\n"
                + "@@ -12 +12\n"
                + "          android:layout_marginRight=\"40dip\"\n"
                + "+         android:layout_marginStart=\"35dip\"\n"
                + "          android:gravity=\"center\"\n"
                + "Fix for res/layout/rtl_quick_fixed.xml line 10: Add android:layout_marginEnd=\"40dip\":\n"
                + "@@ -10 +10\n"
                + "          android:layout_height=\"match_parent\"\n"
                + "+         android:layout_marginEnd=\"40dip\"\n"
                + "          android:layout_marginLeft=\"35dip\"\n"
                + "Fix for res/layout/rtl_quick_fixed.xml line 11: Add android:paddingStart=\"25dip\":\n"
                + "@@ -15 +15\n"
                + "          android:paddingRight=\"20dip\"\n"
                + "+         android:paddingStart=\"25dip\"\n"
                + "          android:text=\"@string/creating_instant_mix\"\n"
                + "Fix for res/layout/rtl_quick_fixed.xml line 12: Add android:paddingEnd=\"20dip\":\n"
                + "@@ -13 +13\n"
                + "          android:gravity=\"center\"\n"
                + "+         android:paddingEnd=\"20dip\"\n"
                + "          android:paddingLeft=\"25dip\"\n");
    }

    public void testRtlQuickFix17() throws Exception {
        //noinspection all // Sample code
        lint().files(
                projectProperties().compileSdk(17),
                manifest().minSdk(17).targetSdk(17),
                mRtlQuickFixed)
                .issues(RtlDetector.USE_START)
                .run()
                .verifyFixes().window(1).expectFixDiffs("Fix for res/layout/rtl_quick_fixed.xml line 9: Replace with android:layout_marginStart=\"35dip\":\n"
                + "@@ -10 +10\n"
                + "          android:layout_height=\"match_parent\"\n"
                + "-         android:layout_marginLeft=\"35dip\"\n"
                + "+         android:layout_marginStart=\"35dip\"\n"
                + "          android:layout_marginRight=\"40dip\"\n"
                + "Fix for res/layout/rtl_quick_fixed.xml line 10: Replace with android:layout_marginEnd=\"40dip\":\n"
                + "@@ -11 +11\n"
                + "          android:layout_marginLeft=\"35dip\"\n"
                + "-         android:layout_marginRight=\"40dip\"\n"
                + "+         android:layout_marginEnd=\"40dip\"\n"
                + "          android:paddingLeft=\"25dip\"\n"
                + "Fix for res/layout/rtl_quick_fixed.xml line 11: Replace with android:paddingStart=\"25dip\":\n"
                + "@@ -12 +12\n"
                + "          android:layout_marginRight=\"40dip\"\n"
                + "-         android:paddingLeft=\"25dip\"\n"
                + "+         android:paddingStart=\"25dip\"\n"
                + "          android:paddingRight=\"20dip\"\n"
                + "Fix for res/layout/rtl_quick_fixed.xml line 12: Replace with android:paddingEnd=\"20dip\":\n"
                + "@@ -13 +13\n"
                + "          android:paddingLeft=\"25dip\"\n"
                + "-         android:paddingRight=\"20dip\"\n"
                + "+         android:paddingEnd=\"20dip\"\n"
                + "          android:text=\"@string/creating_instant_mix\"\n");
    }

    public void testJava() throws Exception {
        mEnabled = ALL;
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/GravityTest.java:24: Warning: Use \"Gravity.START\" instead of \"Gravity.LEFT\" to ensure correct behavior in right-to-left locales [RtlHardcoded]\n"
                + "        t1.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);\n"
                + "                              ~~~~\n"
                + "src/test/pkg/GravityTest.java:30: Warning: Use \"Gravity.START\" instead of \"Gravity.LEFT\" to ensure correct behavior in right-to-left locales [RtlHardcoded]\n"
                + "        t1.setGravity(LEFT | RIGHT); // static imports\n"
                + "                      ~~~~\n"
                + "0 errors, 2 warnings\n",

                lintProject(
                        projectProperties().compileSdk(17),
                        manifest().minSdk(5).targetSdk(17),
                        java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import com.example.includetest.R;\n"
                            + "\n"
                            + "import static android.view.Gravity.LEFT;\n"
                            + "import static foo.bar.RIGHT;\n"
                            + "import android.app.Activity;\n"
                            + "import android.content.Context;\n"
                            + "import android.os.Bundle;\n"
                            + "import android.view.LayoutInflater;\n"
                            + "import android.view.View;\n"
                            + "import android.view.ViewGroup;\n"
                            + "import android.widget.Button;\n"
                            + "import android.view.Gravity;\n"
                            + "\n"
                            + "@SuppressWarnings(\"unused\")\n"
                            + "public class GravityTest extends Activity {\n"
                            + "    @Override\n"
                            + "    protected void onCreate(Bundle savedInstanceState) {\n"
                            + "        super.onCreate(savedInstanceState);\n"
                            + "        TextView t1 = new TextView(context);\n"
                            + "        t1.setHeight(desiredHeight);\n"
                            + "        t1.setText(text);\n"
                            + "        t1.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);\n"
                            + "        t1.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);\n"
                            + "        final ViewGroup.LayoutParams lp1 = new LinearLayout.LayoutParams(\n"
                            + "                 0,\n"
                            + "                 ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);\n"
                            + "        int notAnError = Other.LEFT;\n"
                            + "        t1.setGravity(LEFT | RIGHT); // static imports\n"
                            + "    }\n"
                            + "}\n")
                ));
    }

    public void testOk1() throws Exception {
        mEnabled = ALL;
        // targetSdkVersion < 17
        assertEquals(
                "No warnings.",

                lintProject(mRtl));
    }

    public void testOk2() throws Exception {
        mEnabled = ALL;
        // build target < 14
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",

                lintProject(
                        projectProperties().compileSdk(10),
                        manifest().minSdk(5).targetSdk(17),
                        mRtl
                ));
    }

    public void testNullLocalName() throws Exception {
        // Regression test for attribute with null local name
        mEnabled = ALL;
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",

                lintProject(
                        projectProperties().compileSdk(10),
                        manifest().minSdk(5).targetSdk(17),
                        xml("res/layout/rtl.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "    tools:ignore=\"HardcodedText\" >\n"
                            + "\n"
                            + "    <Button\n"
                            + "        layout_gravity=\"left\"\n"
                            + "        layout_alignParentLeft=\"true\"\n"
                            + "        editable=\"false\"\n"
                            + "        android:text=\"Button\" />\n"
                            + "\n"
                            + "</LinearLayout>\n")
                ));
    }

    public void testSymmetry() throws Exception {
        mEnabled = Collections.singleton(RtlDetector.SYMMETRY);
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/relative.xml:29: Warning: When you define paddingRight you should probably also define paddingLeft for right-to-left symmetry [RtlSymmetry]\n"
                + "        android:paddingRight=\"120dip\"\n"
                + "        ~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",

                lintProject(
                        projectProperties().compileSdk(17),
                        manifest().minSdk(5).targetSdk(17),
                        mRelative
                ));
    }

    public void testCompatAttributeValueConversion() throws Exception {
        // Ensure that when the RTL value contains a direction, we produce the
        // compatibility version of it for the compatibility attribute, e.g. if the
        // attribute for paddingEnd is ?listPreferredItemPaddingEnd, when we suggest
        // also setting paddingRight we suggest ?listPreferredItemPaddingRight
        String expected = ""
                + "res/layout/symmetry.xml:8: Error: To support older versions than API 17 (project specifies 5) you should also add android:paddingRight=\"?android:listPreferredItemPaddingRight\" [RtlCompat]\n"
                + "        android:paddingEnd=\"?android:listPreferredItemPaddingEnd\"\n"
                + "        ~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                projectProperties().compileSdk(17),
                manifest().minSdk(5).targetSdk(17),
                xml("res/layout/symmetry.xml", ""
                        + "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\">\n"
                        + "\n"
                        + "    <TextView\n"
                        + "        android:text=\"@string/hello_world\"\n"
                        + "        android:paddingEnd=\"?android:listPreferredItemPaddingEnd\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\" />\n"
                        + "\n"
                        + "</RelativeLayout>\n"))
                .issues(RtlDetector.COMPAT)
                .run()
                .expect(expected)
                .verifyFixes().window(1).expectFixDiffs(""
                + "Fix for res/layout/symmetry.xml line 7: Set paddingRight=\"?android:listPreferredItemPaddingRight\":\n"
                + "@@ -11 +11\n"
                + "          android:paddingEnd=\"?android:listPreferredItemPaddingEnd\"\n"
                + "+         android:paddingRight=\"?android:listPreferredItemPaddingRight\"\n"
                + "          android:text=\"@string/hello_world\" />\n");
    }

    public void testTextAlignment() throws Exception {
        mEnabled = Collections.singleton(RtlDetector.COMPAT);
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/spinner.xml:49: Error: Inconsistent alignment specification between textAlignment and gravity attributes: was end, expected start [RtlCompat]\n"
                + "            android:textAlignment=\"textStart\"/> <!-- ERROR -->\n"
                + "                                   ~~~~~~~~~\n"
                + "    res/layout/spinner.xml:46: Incompatible direction here\n"
                + "1 errors, 0 warnings\n",

                lintProject(
                        projectProperties().compileSdk(17),
                        manifest().minSdk(5).targetSdk(17),
                        xml("res/layout/spinner.xml", ""
                            + "<merge xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                            + "    <TextView\n"
                            + "            android:id=\"@android:id/text1\"\n"
                            + "            style=\"?android:attr/spinnerItemStyle\"\n"
                            + "            android:layout_width=\"match_parent\"\n"
                            + "            android:layout_height=\"wrap_content\"\n"
                            + "            android:layout_gravity=\"start\"\n"
                            + "            android:ellipsize=\"marquee\"\n"
                            + "            android:singleLine=\"true\"\n"
                            + "            android:textAlignment=\"inherit\"/> <!-- OK -->\n"
                            + "\n"
                            + "    <TextView\n"
                            + "          android:id=\"@android:id/text2\"\n"
                            + "          style=\"?android:attr/spinnerItemStyle\"\n"
                            + "          android:layout_width=\"match_parent\"\n"
                            + "          android:layout_height=\"wrap_content\"\n"
                            + "          android:ellipsize=\"marquee\"\n"
                            + "          android:singleLine=\"true\"\n"
                            + "          android:textAlignment=\"inherit\"/> <!-- OK -->\n"
                            + "\n"
                            + "    <TextView\n"
                            + "            android:id=\"@android:id/text2\"\n"
                            + "            style=\"?android:attr/spinnerItemStyle\"\n"
                            + "            android:layout_width=\"match_parent\"\n"
                            + "            android:layout_height=\"wrap_content\"\n"
                            + "            android:layout_gravity=\"start|top\"\n"
                            + "            android:ellipsize=\"marquee\"\n"
                            + "            android:singleLine=\"true\"\n"
                            + "            android:textAlignment=\"textStart\"/> <!-- OK -->\n"
                            + "\n"
                            + "    <TextView\n"
                            + "            android:id=\"@android:id/text2\"\n"
                            + "            style=\"?android:attr/spinnerItemStyle\"\n"
                            + "            android:layout_width=\"match_parent\"\n"
                            + "            android:layout_height=\"wrap_content\"\n"
                            + "            android:layout_gravity=\"end|bottom\"\n"
                            + "            android:ellipsize=\"marquee\"\n"
                            + "            android:singleLine=\"true\"\n"
                            + "            android:textAlignment=\"textEnd\"/> <!-- OK -->\n"
                            + "\n"
                            + "    <TextView\n"
                            + "            android:id=\"@android:id/text2\"\n"
                            + "            style=\"?android:attr/spinnerItemStyle\"\n"
                            + "            android:layout_width=\"match_parent\"\n"
                            + "            android:layout_height=\"wrap_content\"\n"
                            + "            android:layout_gravity=\"end\"\n"
                            + "            android:ellipsize=\"marquee\"\n"
                            + "            android:singleLine=\"true\"\n"
                            + "            android:textAlignment=\"textStart\"/> <!-- ERROR -->\n"
                            + "\n"
                            + "</merge>\n")
                ));
    }

    public void testConvertBetweenAttributes() {
        assertEquals("alignParentStart", convertOldToNew("alignParentLeft"));
        assertEquals("alignParentEnd", convertOldToNew("alignParentRight"));
        assertEquals("paddingEnd", convertOldToNew("paddingRight"));
        assertEquals("paddingStart", convertOldToNew("paddingLeft"));

        assertEquals("alignParentLeft", convertNewToOld("alignParentStart"));
        assertEquals("alignParentRight", convertNewToOld("alignParentEnd"));
        assertEquals("paddingRight", convertNewToOld("paddingEnd"));
        assertEquals("paddingLeft", convertNewToOld("paddingStart"));

        for (int i = 0, n = ATTRIBUTES.length; i < n; i += 2) {
            String oldAttribute = ATTRIBUTES[i];
            String newAttribute = ATTRIBUTES[i + 1];
            assertEquals(newAttribute, convertOldToNew(oldAttribute));
            assertEquals(oldAttribute, convertNewToOld(newAttribute));
        }
    }

    public void testConvertToOppositeDirection() {
        assertEquals("alignParentRight", convertToOppositeDirection("alignParentLeft"));
        assertEquals("alignParentLeft", convertToOppositeDirection("alignParentRight"));
        assertEquals("alignParentStart", convertToOppositeDirection("alignParentEnd"));
        assertEquals("alignParentEnd", convertToOppositeDirection("alignParentStart"));
        assertEquals("paddingLeft", convertToOppositeDirection("paddingRight"));
        assertEquals("paddingRight", convertToOppositeDirection("paddingLeft"));
        assertEquals("paddingStart", convertToOppositeDirection("paddingEnd"));
        assertEquals("paddingEnd", convertToOppositeDirection("paddingStart"));
    }

    public void testEnumConstants() throws Exception {
        // Regression test for
        //   https://code.google.com/p/android/issues/detail?id=75480
        // Also checks that static imports work correctly
        mEnabled = ALL;
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/GravityTest2.java:19: Warning: Use \"Gravity.START\" instead of \"Gravity.LEFT\" to ensure correct behavior in right-to-left locales [RtlHardcoded]\n"
                + "        if (gravity == LEFT) { // ERROR\n"
                + "                       ~~~~\n"
                + "0 errors, 1 warnings\n",

            lintProject(
                    projectProperties().compileSdk(17),
                    manifest().minSdk(5).targetSdk(17),
                    java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import static android.view.Gravity.LEFT;\n"
                            + "\n"
                            + "@SuppressWarnings(\"StatementWithEmptyBody\")\n"
                            + "public class GravityTest2 {\n"
                            + "    public static final int RIGHT = 5;\n"
                            + "\n"
                            + "    enum Direction {\n"
                            + "        LEFT, UP, RIGHT, DOWN; // OK\n"
                            + "\n"
                            + "        void test() {\n"
                            + "            if (this == LEFT || this == Direction.RIGHT) { // OK\n"
                            + "            }\n"
                            + "        }\n"
                            + "    }\n"
                            + "\n"
                            + "    public void test1(int gravity) {\n"
                            + "        if (gravity == LEFT) { // ERROR\n"
                            + "        }\n"
                            + "        if (gravity == RIGHT) { // OK\n"
                            + "        }\n"
                            + "    }\n"
                            + "\n"
                            + "    public void test2(int gravity, int RIGHT) {\n"
                            + "        if (gravity == RIGHT) { // OK\n"
                            + "        }\n"
                            + "    }\n"
                            + "}\n")
            ));
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mMin17rtl = xml("AndroidManifest.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    package=\"test.bytecode\"\n"
            + "    android:versionCode=\"1\"\n"
            + "    android:versionName=\"1.0\" >\n"
            + "\n"
            + "    <uses-sdk android:minSdkVersion=\"17\" android:targetSdkVersion=\"18\" />\n"
            + "\n"
            + "    <application\n"
            + "        android:icon=\"@drawable/ic_launcher\"\n"
            + "        android:label=\"@string/app_name\"\n"
            + "        android:supportsRtl=\"true\">\n"
            + "    </application>\n"
            + "\n"
            + "</manifest>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mMinsdk5targetsdk14 = xml("AndroidManifest.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    package=\"test.bytecode\"\n"
            + "    android:versionCode=\"1\"\n"
            + "    android:versionName=\"1.0\" >\n"
            + "\n"
            + "    <uses-sdk android:minSdkVersion=\"5\" android:targetSdkVersion=\"14\" />\n"
            + "\n"
            + "    <application\n"
            + "        android:icon=\"@drawable/ic_launcher\"\n"
            + "        android:label=\"@string/app_name\" >\n"
            + "        <activity\n"
            + "            android:name=\".BytecodeTestsActivity\"\n"
            + "            android:label=\"@string/app_name\" >\n"
            + "            <intent-filter>\n"
            + "                <action android:name=\"android.intent.action.MAIN\" />\n"
            + "\n"
            + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
            + "            </intent-filter>\n"
            + "        </activity>\n"
            + "    </application>\n"
            + "\n"
            + "</manifest>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mRelative = xml("res/layout/relative.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:layout_width=\"wrap_content\"\n"
            + "    android:layout_height=\"wrap_content\" >\n"
            + "\n"
            + "    <ProgressBar\n"
            + "        android:id=\"@+id/loading_progress\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:layout_alignParentLeft=\"true\"\n"
            + "        android:layout_alignParentTop=\"true\"\n"
            + "        android:layout_marginBottom=\"60dip\"\n"
            + "        android:layout_marginLeft=\"40dip\"\n"
            + "        android:layout_marginTop=\"40dip\"\n"
            + "        android:max=\"10000\" />\n"
            + "\n"
            + "    <TextView\n"
            + "        android:id=\"@+id/text\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:layout_alignParentTop=\"true\"\n"
            + "        android:layout_alignWithParentIfMissing=\"true\"\n"
            + "        android:layout_marginBottom=\"60dip\"\n"
            + "        android:layout_marginLeft=\"40dip\"\n"
            + "        android:layout_marginTop=\"40dip\"\n"
            + "        android:layout_toRightOf=\"@id/loading_progress\"\n"
            + "        android:ellipsize=\"end\"\n"
            + "        android:maxLines=\"3\"\n"
            + "        android:paddingRight=\"120dip\"\n"
            + "        android:text=\"@string/creating_instant_mix\"\n"
            + "        android:textAppearance=\"?android:attr/textAppearanceMedium\" />\n"
            + "\n"
            + "    <Button\n"
            + "        android:id=\"@+id/cancel\"\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:layout_alignParentLeft=\"true\"\n"
            + "        android:layout_alignRight=\"@id/text\"\n"
            + "        android:layout_below=\"@id/text\"\n"
            + "        android:background=\"@null\"\n"
            + "        android:text=\"@string/cancel\" />\n"
            + "\n"
            + "    <ImageView\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:layout_above=\"@id/cancel\"\n"
            + "        android:layout_alignLeft=\"@id/cancel\"\n"
            + "        android:layout_alignRight=\"@id/cancel\"\n"
            + "        android:scaleType=\"fitXY\"\n"
            + "        android:src=\"@drawable/menu_list_divider\" />\n"
            + "\n"
            + "    <Button\n"
            + "            android:id=\"@+id/cancel2\"\n"
            + "            android:layout_width=\"wrap_content\"\n"
            + "            android:layout_height=\"wrap_content\"\n"
            + "            android:layout_alignParentStart=\"true\"\n"
            + "            android:layout_alignEnd=\"@id/text\"\n"
            + "            android:layout_below=\"@id/text\"\n"
            + "            android:background=\"@null\"\n"
            + "            android:layout_marginLeft=\"40dip\"\n"
            + "            android:layout_marginRight=\"40dip\"\n"
            + "            android:paddingLeft=\"120dip\"\n"
            + "            android:paddingRight=\"120dip\"\n"
            + "            android:text=\"@string/cancel\" />\n"
            + "\n"
            + "</RelativeLayout>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mRtlQuickFixed = xml("res/layout/rtl_quick_fixed.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    android:layout_width=\"match_parent\"\n"
            + "    android:layout_height=\"match_parent\" >\n"
            + "    <TextView\n"
            + "        android:id=\"@+id/text\"\n"
            + "        android:gravity=\"center\"\n"
            + "        android:layout_width=\"match_parent\"\n"
            + "        android:layout_height=\"match_parent\"\n"
            + "        android:layout_marginLeft=\"35dip\"\n"
            + "        android:layout_marginRight=\"40dip\"\n"
            + "        android:paddingLeft=\"25dip\"\n"
            + "        android:paddingRight=\"20dip\"\n"
            + "        android:text=\"@string/creating_instant_mix\"\n"
            + "        android:textAppearance=\"?android:attr/textAppearanceMedium\" />\n"
            + "</FrameLayout>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mRtl = xml("res/layout/rtl.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
            + "    android:layout_width=\"match_parent\"\n"
            + "    android:layout_height=\"match_parent\"\n"
            + "    android:orientation=\"vertical\"\n"
            + "    tools:ignore=\"HardcodedText\" >\n"
            + "\n"
            + "    <!-- Warn: Use start instead of left -->\n"
            + "\n"
            + "    <Button\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:layout_gravity=\"left\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <!-- Warn: Use end instead of right with layout_gravity -->\n"
            + "\n"
            + "    <Button\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:layout_gravity=\"right\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <!-- Warn: Use end instead of right with gravity -->\n"
            + "\n"
            + "    <TextView\n"
            + "        android:layout_width=\"match_parent\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:gravity=\"right\"\n"
            + "        android:text=\"TextView\" />\n"
            + "\n"
            + "    <!-- OK: No warning -->\n"
            + "\n"
            + "    <Button\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:layout_gravity=\"start\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <!-- OK: No warning -->\n"
            + "\n"
            + "    <Button\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:layout_gravity=\"end\"\n"
            + "        android:text=\"Button\" />\n"
            + "\n"
            + "    <!-- OK: No warning -->\n"
            + "\n"
            + "    <TextView\n"
            + "        android:layout_width=\"match_parent\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:gravity=\"end\"\n"
            + "        android:text=\"TextView\" />\n"
            + "\n"
            + "    <!-- OK: Suppressed -->\n"
            + "\n"
            + "    <Button\n"
            + "        android:layout_width=\"wrap_content\"\n"
            + "        android:layout_height=\"wrap_content\"\n"
            + "        android:layout_gravity=\"right\"\n"
            + "        android:text=\"Button\"\n"
            + "        tools:ignore=\"RtlHardcoded\" />\n"
            + "\n"
            + "</LinearLayout>\n");
}
