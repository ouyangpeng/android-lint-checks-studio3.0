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

package com.android.tools.lint.checks;

import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings("javadoc")
public class ProguardDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new ProguardDetector();
    }

    public void testProguard() throws Exception {
        assertEquals(""
                + "proguard.cfg:21: Error: Obsolete ProGuard file; use -keepclasseswithmembers instead of -keepclasseswithmembernames [Proguard]\n"
                + "-keepclasseswithmembernames class * {\n"
                + "^\n"
                + "1 errors, 0 warnings\n",
            lintFiles(mProguard));
    }

    public void testProguardNewPath() throws Exception {
        assertEquals(""
                + "proguard-project.txt:21: Error: Obsolete ProGuard file; use -keepclasseswithmembers instead of -keepclasseswithmembernames [Proguard]\n"
                + "-keepclasseswithmembernames class * {\n"
                + "^\n"
                + "1 errors, 0 warnings\n",
            lintFiles(mProguard2));
    }

    public void testProguardRandomName() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "myfile.txt:21: Error: Obsolete ProGuard file; use -keepclasseswithmembers instead of -keepclasseswithmembernames [Proguard]\n"
                + "-keepclasseswithmembernames class * {\n"
                + "^\n"
                + "myfile.txt:8: Warning: Local ProGuard configuration contains general Android configuration: Inherit these settings instead? Modify project.properties to define proguard.config=${sdk.dir}/tools/proguard/proguard-android.txt:myfile.txt and then keep only project-specific configuration here [ProguardSplit]\n"
                + "-keep public class * extends android.app.Activity\n"
                + "^\n"
                + "1 errors, 1 warnings\n",
            lintProject(
                    mProguard3,
                    source("project.properties", ""
                            + "target=android-14\n"
                            + "proguard.config=${sdk.dir}/foo.cfg:${user.home}/bar.pro;myfile.txt\n")));
    }

    public void testSilent() throws Exception {
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",

                lintFiles(
                        mProguard4,
                        projectProperties().compileSdk(3)));
    }

    public void testSilent2() throws Exception {
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",

                lintFiles(
                        mProguard4,
                        projectProperties().compileSdk(3)));
    }

    public void testSplit() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "proguard.cfg:14: Warning: Local ProGuard configuration contains general Android configuration: Inherit these settings instead? Modify project.properties to define proguard.config=${sdk.dir}/tools/proguard/proguard-android.txt:proguard.cfg and then keep only project-specific configuration here [ProguardSplit]\n"
                + "-keep public class * extends android.app.Activity\n"
                + "^\n"
                + "0 errors, 1 warnings\n",

            lintFiles(
                    mProguard4,
                    projectProperties().compileSdk(3).property("proguard.config", "proguard.cfg")));
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mProguard = source("proguard.cfg", ""
            + "-optimizationpasses 5\n"
            + "-dontusemixedcaseclassnames\n"
            + "-dontskipnonpubliclibraryclasses\n"
            + "-dontpreverify\n"
            + "-verbose\n"
            + "-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*\n"
            + "\n"
            + "-keep public class * extends android.app.Activity\n"
            + "-keep public class * extends android.app.Application\n"
            + "-keep public class * extends android.app.Service\n"
            + "-keep public class * extends android.content.BroadcastReceiver\n"
            + "-keep public class * extends android.content.ContentProvider\n"
            + "-keep public class * extends android.app.backup.BackupAgentHelper\n"
            + "-keep public class * extends android.preference.Preference\n"
            + "-keep public class com.android.vending.licensing.ILicensingService\n"
            + "\n"
            + "-keepclasseswithmembernames class * {\n"
            + "    native <methods>;\n"
            + "}\n"
            + "\n"
            + "-keepclasseswithmembernames class * {\n"
            + "    public <init>(android.content.Context, android.util.AttributeSet);\n"
            + "}\n"
            + "\n"
            + "-keepclasseswithmembernames class * {\n"
            + "    public <init>(android.content.Context, android.util.AttributeSet, int);\n"
            + "}\n"
            + "\n"
            + "-keepclassmembers enum * {\n"
            + "    public static **[] values();\n"
            + "    public static ** valueOf(java.lang.String);\n"
            + "}\n"
            + "\n"
            + "-keep class * implements android.os.Parcelable {\n"
            + "  public static final android.os.Parcelable$Creator *;\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mProguard2 = source("proguard-project.txt", ""
            + "-optimizationpasses 5\n"
            + "-dontusemixedcaseclassnames\n"
            + "-dontskipnonpubliclibraryclasses\n"
            + "-dontpreverify\n"
            + "-verbose\n"
            + "-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*\n"
            + "\n"
            + "-keep public class * extends android.app.Activity\n"
            + "-keep public class * extends android.app.Application\n"
            + "-keep public class * extends android.app.Service\n"
            + "-keep public class * extends android.content.BroadcastReceiver\n"
            + "-keep public class * extends android.content.ContentProvider\n"
            + "-keep public class * extends android.app.backup.BackupAgentHelper\n"
            + "-keep public class * extends android.preference.Preference\n"
            + "-keep public class com.android.vending.licensing.ILicensingService\n"
            + "\n"
            + "-keepclasseswithmembernames class * {\n"
            + "    native <methods>;\n"
            + "}\n"
            + "\n"
            + "-keepclasseswithmembernames class * {\n"
            + "    public <init>(android.content.Context, android.util.AttributeSet);\n"
            + "}\n"
            + "\n"
            + "-keepclasseswithmembernames class * {\n"
            + "    public <init>(android.content.Context, android.util.AttributeSet, int);\n"
            + "}\n"
            + "\n"
            + "-keepclassmembers enum * {\n"
            + "    public static **[] values();\n"
            + "    public static ** valueOf(java.lang.String);\n"
            + "}\n"
            + "\n"
            + "-keep class * implements android.os.Parcelable {\n"
            + "  public static final android.os.Parcelable$Creator *;\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mProguard3 = source("myfile.txt", ""
            + "-optimizationpasses 5\n"
            + "-dontusemixedcaseclassnames\n"
            + "-dontskipnonpubliclibraryclasses\n"
            + "-dontpreverify\n"
            + "-verbose\n"
            + "-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*\n"
            + "\n"
            + "-keep public class * extends android.app.Activity\n"
            + "-keep public class * extends android.app.Application\n"
            + "-keep public class * extends android.app.Service\n"
            + "-keep public class * extends android.content.BroadcastReceiver\n"
            + "-keep public class * extends android.content.ContentProvider\n"
            + "-keep public class * extends android.app.backup.BackupAgentHelper\n"
            + "-keep public class * extends android.preference.Preference\n"
            + "-keep public class com.android.vending.licensing.ILicensingService\n"
            + "\n"
            + "-keepclasseswithmembernames class * {\n"
            + "    native <methods>;\n"
            + "}\n"
            + "\n"
            + "-keepclasseswithmembernames class * {\n"
            + "    public <init>(android.content.Context, android.util.AttributeSet);\n"
            + "}\n"
            + "\n"
            + "-keepclasseswithmembernames class * {\n"
            + "    public <init>(android.content.Context, android.util.AttributeSet, int);\n"
            + "}\n"
            + "\n"
            + "-keepclassmembers enum * {\n"
            + "    public static **[] values();\n"
            + "    public static ** valueOf(java.lang.String);\n"
            + "}\n"
            + "\n"
            + "-keep class * implements android.os.Parcelable {\n"
            + "  public static final android.os.Parcelable$Creator *;\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mProguard4 = source("proguard.cfg", ""
            + "-optimizationpasses 5\n"
            + "-dontusemixedcaseclassnames\n"
            + "-dontskipnonpubliclibraryclasses\n"
            + "-verbose\n"
            + "-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*\n"
            + "-allowaccessmodification\n"
            + "-keepattributes *Annotation*\n"
            + "\n"
            + "\n"
            + "# dex does not like code run through proguard optimize and preverify steps.\n"
            + "-dontoptimize\n"
            + "-dontpreverify\n"
            + "\n"
            + "-keep public class * extends android.app.Activity\n"
            + "-keep public class * extends android.app.Application\n"
            + "-keep public class * extends android.app.Service\n"
            + "-keep public class * extends android.content.BroadcastReceiver\n"
            + "-keep public class * extends android.content.ContentProvider\n"
            + "-keep public class * extends android.app.backup.BackupAgent\n"
            + "-keep public class * extends android.preference.Preference\n"
            + "-keep public class com.android.vending.licensing.ILicensingService\n"
            + "\n"
            + "# For native methods, see http://proguard.sourceforge.net/manual/examples.html#native\n"
            + "-keepclasseswithmembernames class * {\n"
            + "    native <methods>;\n"
            + "}\n"
            + "\n"
            + "-keep public class * extends android.view.View {\n"
            + "    public <init>(android.content.Context);\n"
            + "    public <init>(android.content.Context, android.util.AttributeSet);\n"
            + "    public <init>(android.content.Context, android.util.AttributeSet, int);\n"
            + "    public void set*(...);\n"
            + "}\n"
            + "\n"
            + "-keepclasseswithmembers class * {\n"
            + "    public <init>(android.content.Context, android.util.AttributeSet);\n"
            + "}\n"
            + "\n"
            + "-keepclasseswithmembers class * {\n"
            + "    public <init>(android.content.Context, android.util.AttributeSet, int);\n"
            + "}\n"
            + "\n"
            + "-keepclassmembers class * extends android.app.Activity {\n"
            + "   public void *(android.view.View);\n"
            + "}\n"
            + "\n"
            + "# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations\n"
            + "-keepclassmembers enum * {\n"
            + "    public static **[] values();\n"
            + "    public static ** valueOf(java.lang.String);\n"
            + "}\n"
            + "\n"
            + "-keep class * implements android.os.Parcelable {\n"
            + "  public static final android.os.Parcelable$Creator *;\n"
            + "}\n"
            + "\n"
            + "-keepclassmembers class **.R$* {\n"
            + "    public static <fields>;\n"
            + "}\n"
            + "\n"
            + "# The support library contains references to newer platform versions.\n"
            + "# Don't warn about those in case this app is linking against an older\n"
            + "# platform version.  We know about them, and they are safe.\n"
            + "-dontwarn android.support.**\n");
}
