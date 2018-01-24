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

package com.android.tools.lint.checks;

import static com.android.tools.lint.checks.MissingClassDetector.INNERCLASS;
import static com.android.tools.lint.checks.MissingClassDetector.INSTANTIATABLE;
import static com.android.tools.lint.checks.MissingClassDetector.MISSING;
import static com.android.tools.lint.checks.infrastructure.ProjectDescription.Type.LIBRARY;

import com.android.tools.lint.checks.infrastructure.ProjectDescription;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Scope;
import com.google.common.collect.Sets;
import java.io.File;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("javadoc")
public class MissingClassDetectorTest extends AbstractCheckTest {
    private EnumSet<Scope> mScopes;
    private Set<Issue> mEnabled = new HashSet<>();

    @Override
    protected Detector getDetector() {
        return new MissingClassDetector();
    }

    @Override
    protected EnumSet<Scope> getLintScope(List<File> file) {
        return mScopes;
    }

    @Override
    protected boolean isEnabled(Issue issue) {
        return super.isEnabled(issue) && mEnabled.contains(issue);
    }

    public void testIncrementalInManifest() throws Exception {
        mScopes = Scope.MANIFEST_SCOPE;
        mEnabled = Sets.newHashSet(MISSING, INSTANTIATABLE, INNERCLASS);
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",

                lintProject(
                    mAndroidManifestWrongRegs,
                    classpath()
                ));
    }

    public void test() throws Exception {
        mScopes = null;
        mEnabled = Sets.newHashSet(MISSING);
        //noinspection all // Sample code
        assertEquals(""
                        + "AndroidManifest.xml:13: Error: Class referenced in the manifest, test.pkg.TestProvider, was not found in the project or the libraries [MissingRegistered]\n"
                        + "        <activity android:name=\".TestProvider\" />\n"
                        + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "AndroidManifest.xml:14: Error: Class referenced in the manifest, test.pkg.TestProvider2, was not found in the project or the libraries [MissingRegistered]\n"
                        + "        <service android:name=\"test.pkg.TestProvider2\" />\n"
                        + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "AndroidManifest.xml:15: Error: Class referenced in the manifest, test.pkg.TestService, was not found in the project or the libraries [MissingRegistered]\n"
                        + "        <provider android:name=\".TestService\" />\n"
                        + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "AndroidManifest.xml:16: Error: Class referenced in the manifest, test.pkg.OnClickActivity, was not found in the project or the libraries [MissingRegistered]\n"
                        + "        <receiver android:name=\"OnClickActivity\" />\n"
                        + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "AndroidManifest.xml:17: Error: Class referenced in the manifest, test.pkg.TestReceiver, was not found in the project or the libraries [MissingRegistered]\n"
                        + "        <service android:name=\"TestReceiver\" />\n"
                        + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "5 errors, 0 warnings\n",

                lintProject(
                        mAndroidManifestWrongRegs,
                        mApiCallTest,
                        classpath()
                ));
    }

    public void testNoWarningBeforeBuild() throws Exception {
        mScopes = null;
        mEnabled = Sets.newHashSet(MISSING, INSTANTIATABLE, INNERCLASS);
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintProject(
                mAndroidManifestWrongRegs,
                classpath()
            ));
    }

    public void testOkClasses() throws Exception {
        mScopes = null;
        mEnabled = Sets.newHashSet(MISSING, INSTANTIATABLE, INNERCLASS);
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintProject(
                mAndroidManifestRegs,
                classpath(),
                mOnClickActivity,
                    mOnClickActivity_class,
                mTestService,
                    mTestService_class,
                mTestProvider,
                mTestProvider_class,
                    mTestProvider_class,
                mTestProvider2_class,
                mTestReceiver,
                    mTestReceiver_class
            ));
    }

    public void testOkLibraries() throws Exception {
        mScopes = null;
        mEnabled = Sets.newHashSet(MISSING, INSTANTIATABLE, INNERCLASS);
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintProject(
                mAndroidManifestRegs,
                classpath(),
                base64gzip("libs/classes.jar", ""
                            + "H4sIAAAAAAAAAJVWCTSUbRseBt+MyZdlsjRjzxChH5F9KbLNGFsllCVpwpRl"
                            + "aCxZJzPCZEu2JorEEGkxjRT5KJQt+5aMlAhFi6X5R8sXzuE///Oe95z3nPe+"
                            + "r/t+nvu67+dCWwA5oAAQ63mGQRgCVi0wgAOANLYzVDRDmSh/bwQAgAC0xV+g"
                            + "lV/sv0zQGzpDWe+/zkhDlJmJsa2dEtLkE7K5ydJCUamNx0JR/kVzS4XN7peq"
                            + "r8b8lMyRu8yQbbgiDvDldyJ1MJjCyb2CUMZ1+bNvZN5jPvrN+rH9iG4Q+OKL"
                            + "Dgtb51d07h/RMtZF52S9AR7+Acobm4B/m5zx8lT+s4/1ZmKrzayw+7wx7l6G"
                            + "7gGYQEwAXsnd29Xfn5A8iOU3hDYwPXNDFRRdbt6VvyuPTsZS7yHc1JACKYis"
                            + "YOvo9BMQmNbtSewlQiPs6+6hy5dPVs2whUKO1Z090evMl3uxOVtTnDmsqdrU"
                            + "MjwcDug5IA0mlI2IAK1wwc7zd3HecYHThVOkpJl6szqwzOBE5b23hQ9GC13P"
                            + "XzyrtGvkoUTdqXlByXPeQfFb3EzHy6I4JG+Bm/aKfm7h+ZJwDrs9JurEfVxF"
                            + "Pt1owXD3B2cim6uto9q+FEfnB/c6w6Binx3stZOQZjNa4FEyeCxTEKXNX5/f"
                            + "97rkXXp9vpXci2exC7Ix8/iF4JcjLtbGCtS9O+QCyRenyOFhS+0aVRD6kyGD"
                            + "OdGOs+MJPrLU0MxadSumJEG0dt7srQNB/KmaloiOwR5SbExnWW+rQEFR7yC8"
                            + "Cy1t59UXEna9uACL61Rt2Xbw/rfw/agrmhWx2P6zMgyv9BNiQVbmZTzf4y4+"
                            + "2kX5+n72ynErDwvvQ4GPpOYztnKrVMkuucRdiBy4HjG1515okunATNjRitrJ"
                            + "1ItSrmLFkTOPb1GkmxdZCajwNnZs5/5+1TiVvc7iaDmemEcutEy2JcLi80EF"
                            + "ZYru0hGit9MU03puiBzBn5KQydvJN4VQTx/EeNXkIIhqWGMiYdrh27huPrqv"
                            + "ZOxhUOkksvIh3No5qrIC3lbJzzaQwHvASbq4nWA+MqQxEL4olDYrELJ0w+7G"
                            + "mXeg9ERE0WLmoP4KW5NDozuFWc3Czrm6V9ZzDLaaY3asD7Tf6UDMcQ+/nwRL"
                            + "s3H2EjCE6moMmzn23ynoXGiz88HEVUpImV6XYzMEy6txWfMieiVRCXTfQPVW"
                            + "JiNaD6z3Fbhk/XqwFynXa/9erzknJ6Qlw78lh/Komgk4I7TowQDxSANHLfXi"
                            + "s8VOHz0VXuR0s0mONzBm/NQx2ovWL/Fz8R2kUSoRHVyo45EI+hZ9kIJ++7WU"
                            + "KkmeUImT4ztIb0xpeUIG3gxx230pZr5UHzpa3Sq/YPU35M1DbpDjoKWiT3VD"
                            + "mjum8mONYRCiMaUen29PD5EqqBWBnGbAYSVV1os+C4XzqXvOH+Gb2MlutMdl"
                            + "Ide6WghsGQlW+BTVEFxnrqFSKpF6oH0OJbvLX1mxIo0gt1P5Yh+vlkjYcs1y"
                            + "tXCsikt1gHj+zeezjO9sCLvQW9fyiTpjvQ8lofoEGwp/AUZG5PmVprn4/zTT"
                            + "nF3rDDja38VozVwT6Ub06Wcgcxs93xASxR32+pa0djN92mnDJlM9W+fRetZt"
                            + "/lRKWFhQXvBkVJjRXBq+4XRWtwpTE05yn52ByR/Dp+xL6E7WpbqQuscqI0a5"
                            + "D9RyUhGvPOEQPgQTz0Y7nhTrq25oQt/z0kcvFuc9s31KKa+mHZTVY3522v+O"
                            + "5sLtx/QTeA0BhpTyCL+SLJycfL4DztPUFl+R0XfPl3Co1t7Jogcrq56vPag3"
                            + "ao390Iab3PE0CG6RoZUpiId6quLUHLywHw6e+3BwckKJso32kQuLCJqfPFlh"
                            + "96Gf/uGf3kdz2TihkUvL/Ys8K8RrSbW5sptFPPymxINvRDyVn8zzQTX9XYPm"
                            + "1b3WadNiSiwRFczs4YeR7KX53cIdNZxuNdp/bzJ3GwGGpgqnJ9RZ+p14kY3X"
                            + "vXJfO0KpUckm5ogUmRTes2tZRFZAlNJpNFEKyUIvF/NlV/cVHzcoyyzRrBGr"
                            + "h7yrMVInq8GNdqJDNPWmjG56Sg2YG8kQpctzICVDS453UtK0+CdqmELBHKe3"
                            + "UwJ2HGic/Gqt7eUreDKC8rifmENWfdAc+iQyYDGumZAC1TFpgCoyhD9RuBI5"
                            + "VY+qOecLrRxFe9IEsIa1UXG2/6sHbTzcPTCBv3sQZ91k/oR1Eh2HvKldRSVd"
                            + "QoLQI0Jc4vS4KM5ctFMh1GkIIfSwD5F83ZhpfigqSvzVAVJKf0T+XYfmjO6M"
                            + "7IzF5U+znK94Hbn4Y9zkoluDg6sqsD205Ht2wHcwUz75WlyolealKVpuwL73"
                            + "A19i3C88Ujd6s58mryOzo/iuumTnDBwjRfL+G3Mc9O2S35jHo+6k7wlvA2Rc"
                            + "y8d68/oFq+Ne783gnrY3OIxZLi5APQ3GwvkSi0t8PRJAlCcL+3Nv5ophVCdd"
                            + "BXJ10logO3MkqagsY1Pt5aPThDLHEqQacqAQ60LQWAwqzwEKyKRecyzh6yqy"
                            + "7SK/zOo+Onx/jBD3wl5YcVqfGCnByCxVWDY5xBG5Bc2mD85Tfiamz+kYLX0h"
                            + "SbgjZPoy3l2WLcXHRJan+fZzHpjwuDD409MGhqpuov9onQNyQEzQ3Y2hePXq"
                            + "+6WJw24m7E1y9U8S76hILJLaFYsdwkRhmeLWKQmxiLLSKS51EWRp2tytGchK"
                            + "+YwY8NRAVum62Dcr3/b15bP18AvEuHv8qh65HMVuyHueGRndAeSM5HXULnMR"
                            + "gMDiCQjEDiEYaNnvfpv9gEzQXy2ENBJh66t9Ip7pHcLXHJv1K5ihGRlfyN0a"
                            + "AM+3hELeYEIjpDy4SkAVVSDf/3Y/AQRyxrQ3T9g97TRP5co9l5ugpATurDmD"
                            + "ApX6FEnZlQ/v58EZKXSZDJZSBOWznv2TTEJ5C9CymyzpD5a2xIXkCCzMHhtt"
                            + "0KnPVPCxqT3JGz6VdNhWzpZISp+d6S+jXa7EpmnahGSdWRI3JRYE5shMmNDt"
                            + "+/QRZDGGyWuOULGB4jP1nr5baPzUuiPaYTVDFfelE3FMkbyacpqXi66LDDzP"
                            + "vnacuq2h9bNQ78zx8PFL15v69n4EaldPnGNfOVh65eAEH+tgaT/6go0dClir"
                            + "5H5rvBUZuHatEYXrXVcLNOgaN90NJOEKAjdgYyH3Z9EB/8q6jV3Aa1yGAGtk"
                            + "3p9cV9xWX89ia9w42f6X7FuPtXriwtZgYYCbXu/rgVbPK/gaoNecm4/r9Uir"
                            + "W2dtSlNcm0679UCrqbJ9DRAVtFnfoS04uX6WBAw4xEqC80fF/gu8+KimcgwA"
                            + "AA==")
            ));
    }

    public void testLibraryProjects() throws Exception {
        mScopes = null;
        mEnabled = Sets.newHashSet(MISSING, INSTANTIATABLE, INNERCLASS);
        File master = getProjectDir("MasterProject",
                // Master project
                mAndroidManifestRegs,
                projectProperties().property("android.library.reference.1", "../LibraryProject"),
                mTestService,
                mTestService_class,
                classpath()
        );
        File library = getProjectDir("LibraryProject",
                // Library project
                manifest().pkg("foo.library").minSdk(14),
                projectProperties().library(true).compileSdk(14),
                mOnClickActivity,
                mOnClickActivity_class,
                mTestProvider,
                mTestProvider_class,
                mTestProvider2,
                mTestProvider2_class
                // Missing TestReceiver: Test should complain about just that class
        );
        assertEquals(""
                + "MasterProject/AndroidManifest.xml:32: Error: Class referenced in the manifest, test.pkg.TestReceiver, was not found in the project or the libraries [MissingRegistered]\n"
                + "        <receiver android:name=\"TestReceiver\" />\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

           checkLint(Arrays.asList(master, library)));
    }

    public void testIndirectLibraryProjects() throws Exception {
        mScopes = null;
        mEnabled = Sets.newHashSet(MISSING, INSTANTIATABLE, INNERCLASS);
        File master = getProjectDir("MasterProject",
                // Master project
                mAndroidManifestRegs,
                projectProperties().property("android.library.reference.1", "../LibraryProject"),
                mTestService,
                mTestService_class,
                classpath()
        );
        File library2 = getProjectDir("LibraryProject",
                // Library project
                xml("AndroidManifest.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"foo.library2\"\n"
                            + "    android:versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\" >\n"
                            + "\n"
                            + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                            + "\n"
                            + "</manifest>\n"),
                projectProperties().library(true).compileSdk(14).property("android.library.reference.1", "../RealLibrary")
        );
        File library = getProjectDir("RealLibrary",
                // Library project
                manifest().pkg("foo.library").minSdk(14),
                projectProperties().library(true).compileSdk(14),
                mOnClickActivity,
                mOnClickActivity_class,
                mTestProvider,
                mTestProvider_class,
                mTestProvider2,
                mTestProvider2_class
                // Missing TestReceiver: Test should complain about just that class
        );
        assertEquals(""
                + "MasterProject/AndroidManifest.xml:32: Error: Class referenced in the manifest, test.pkg.TestReceiver, was not found in the project or the libraries [MissingRegistered]\n"
                + "        <receiver android:name=\"TestReceiver\" />\n"
                + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n",

                checkLint(Arrays.asList(master, library2, library)));
    }

    public void testLibraryWithMissingClass() throws Exception {
        mScopes = null;
        mEnabled = Sets.newHashSet(MISSING);
        assertEquals("No warnings.",
                lintProject(
                        xml("AndroidManifest.xml", ""
                                + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                + "    package=\"test.pkg\"\n"
                                + "    android:versionCode=\"1\"\n"
                                + "    android:versionName=\"1.0\" >\n"
                                + "\n"
                                + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                                + "\n"
                                + "    <application\n"
                                + "        android:icon=\"@drawable/ic_launcher\"\n"
                                + "        android:label=\"@string/app_name\" >\n"
                                + "        <service android:name=\".TestService\" />\n"
                                + "\n"
                                + "    </application>\n"
                                + "\n"
                                + "</manifest>"),
                        // This is not the actual class that is present in AndroidManifest.xml
                        mTestProvider2_class,
                        // Note that the manifestmerger.enabled property is necessary for
                        // the manifest scoped lint detectors to run.
                        source("project.properties", ""
                                + "target=android-14\n"
                                + "android.library=true\n"
                                + "manifestmerger.enabled=true\n"))
        );
    }

    public void testLibraryWithMissingClassInApp() throws Exception {
        mScopes = null;

        //noinspection all // Sample code
        ProjectDescription library = project(
                // Library project
                xml("AndroidManifest.xml", ""
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg\"\n"
                        + "    android:versionCode=\"1\"\n"
                        + "    android:versionName=\"1.0\" >\n"
                        + "\n"
                        + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:icon=\"@drawable/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\" >\n"
                        + "        <service android:name=\".TestService\" />\n"
                        + "\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>"),
                projectProperties().library(true).compileSdk(14),
                mTestProvider2_class
        ).type(LIBRARY).name("LibraryProject");

        //noinspection all // Sample code
        ProjectDescription main = project(
                // Master project
                xml("AndroidManifest.xml", ""
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg.app\">\n"
                        + "</manifest>"),
                mTestProvider2_class,
                projectProperties().dependsOn("../LibraryProject").manifestMerger(true).compileSdk(14)
        ).name("App").dependsOn(library);

        lint().projects(main, library)
                .issues(MISSING)
                .run()
                .expect(""
                        + "LibraryProject/AndroidManifest.xml:11: Error: Class referenced in the manifest, test.pkg.TestService, was not found in the project or the libraries [MissingRegistered]\n"
                        + "        <service android:name=\".TestService\" />\n"
                        + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n");
    }

    public void testInnerClassStatic() throws Exception {
        mScopes = null;
        mEnabled = Sets.newHashSet(MISSING, INSTANTIATABLE, INNERCLASS);
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/Foo.java:8: Error: This inner class should be static (test.pkg.Foo.Baz) [Instantiatable]\n"
                + "    public class Baz extends Activity {\n"
                + "    ^\n"
                + "1 errors, 0 warnings\n",

            lintProject(
                mAndroidManifest,
                classpath(),
                mFoo,
                mFoo_class,
                base64gzip("bin/classes/test/pkg/Foo$Bar.class", ""
                            + "H4sIAAAAAAAAAF1Oy0oDQRCsTja7ybqah/kBwYN6cMCrImggEAhelNwnu4OO"
                            + "rjPLzCTgZ3kSPPgBfpTYO3oQu6G6q7qL7s+v9w8AZ5hm6BBGQfkgmqd7Mbf2"
                            + "8Fq6DAlhKk3lrK6EbBpxVQa91eGFkF5oo8MloXt0vCIkM1upHF0MCvSQEoZL"
                            + "bdTN5nmt3J1c14owWdpS1ivpdMt/xSQ8aN/O/t8+J+S3duNKNdftYp/100e5"
                            + "lYRiYYxys1p6r3yGCSt/3fwS+3EAbtAGcfJPjBkzETnQO3lD/5WbDnLGNIop"
                            + "dhiLnwWuu3G+F3GIEdc82sfYx+AbAxaHhz8BAAA="),
                base64gzip("bin/classes/test/pkg/Foo$Baz.class", ""
                            + "H4sIAAAAAAAAAF1QTUvDQBB9k8SkjbFNa/06Cj1UBSPiTRG0UBCCF6X3bbLo"
                            + "akxCsi3ov/Igggd/gD9KnA0eqgw8Zt57M/vYr++PTwDH2PFgEUItax2Vj3fR"
                            + "pCiGl+LFg0MYiDytCpVGoiyji0SrhdLPBFffq3p4ROjEy2unrJypXOlzQm/0"
                            + "V9qbEpxxkco2CKsBVuD6sLEWwEOHYI+MoRurXF7Pn2ayuhWzTBL6cZGIbCoq"
                            + "ZeZf0jHPG+1/Zg7g3xTzKpETZYwt5g8fxEIQgqs8l9U4E3Utaw9bzCxvcwLe"
                            + "xy6Hs/lXKAxNQtNxeWgxtnk6gcUF+PsHbwzvCF55stBldNkDdoaMQdP76KHf"
                            + "6OsNDrDRsObmJrZBP1YpAveEAQAA")
            ));
    }

    public void testInnerClassPublic() throws Exception {
        mScopes = null;
        mEnabled = Sets.newHashSet(MISSING, INSTANTIATABLE, INNERCLASS);
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/Foo/Bar.java:6: Error: The default constructor must be public [Instantiatable]\n"
                + "    private Bar() {\n"
                + "    ^\n"
                + "1 errors, 0 warnings\n",

            lintProject(
                mAndroidManifestInner,
                classpath(),
                mBar,
                mBar_class
            ));
    }

    public void testInnerClass() throws Exception {
        mScopes = null;
        mEnabled = Sets.newHashSet(MISSING, INSTANTIATABLE, INNERCLASS);
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:14: Error: Class referenced in the manifest, test.pkg.Foo.Bar, was not found in the project or the libraries [MissingRegistered]\n"
                + "        <activity\n"
                + "        ^\n"
                + "AndroidManifest.xml:23: Error: Class referenced in the manifest, test.pkg.Foo.Baz, was not found in the project or the libraries [MissingRegistered]\n"
                + "        <activity\n"
                + "        ^\n"
                + "2 errors, 0 warnings\n",

            lintProject(
                mAndroidManifest,
                classpath(),
                mApiCallTest,
                mFoo
            ));
    }

    public void testInnerClass2() throws Exception {
        mScopes = null;
        mEnabled = Sets.newHashSet(MISSING, INSTANTIATABLE, INNERCLASS);
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:14: Error: Class referenced in the manifest, test.pkg.Foo.Bar, was not found in the project or the libraries [MissingRegistered]\n"
                + "        <activity\n"
                + "        ^\n"
                + "1 errors, 0 warnings\n",

            lintProject(
                mAndroidManifestInner,
                classpath(),
                mApiCallTest,
                mBar
            ));
    }

    public void testWrongSeparator1() throws Exception {
        mScopes = null;
        mEnabled = Sets.newHashSet(MISSING, INSTANTIATABLE, INNERCLASS);
        //noinspection all // Sample code
        assertEquals(""
                + "AndroidManifest.xml:14: Error: Class referenced in the manifest, test.pkg.Foo.Bar, was not found in the project or the libraries [MissingRegistered]\n"
                + "        <activity\n"
                + "        ^\n"
                + "1 errors, 0 warnings\n",

            lintProject(
                xml("AndroidManifest.xml", ""
                            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    package=\"test.pkg.Foo\"\n"
                            + "    android:versionCode=\"1\"\n"
                            + "    android:versionName=\"1.0\" >\n"
                            + "\n"
                            + "    <uses-sdk\n"
                            + "        android:minSdkVersion=\"8\"\n"
                            + "        android:targetSdkVersion=\"16\" />\n"
                            + "\n"
                            + "    <application\n"
                            + "        android:icon=\"@drawable/ic_launcher\"\n"
                            + "        android:label=\"@string/app_name\"\n"
                            + "        android:theme=\"@style/AppTheme\" >\n"
                            + "        <activity\n"
                            + "            android:name=\"test.pkg.Foo.Bar\"\n"
                            + "            android:label=\"@string/app_name\" >\n"
                            + "            <intent-filter>\n"
                            + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                            + "\n"
                            + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                            + "            </intent-filter>\n"
                            + "        </activity>\n"
                            + "    </application>\n"
                            + "\n"
                            + "</manifest>\n"),
                classpath(),
                mApiCallTest,
                mBar
            ));
    }

    public void testWrongSeparator2() {
        mScopes = null;
        String expected = ""
                + "AndroidManifest.xml:14: Error: Class referenced in the manifest, test.pkg.Foo.Bar, was not found in the project or the libraries [MissingRegistered]\n"
                + "        <activity\n"
                + "        ^\n"
                + "AndroidManifest.xml:15: Warning: Use '$' instead of '.' for inner classes (or use only lowercase letters in package names); replace \".Foo.Bar\" with \".Foo$Bar\" [InnerclassSeparator]\n"
                + "            android:name=\".Foo.Bar\"\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg\"\n"
                        + "    android:versionCode=\"1\"\n"
                        + "    android:versionName=\"1.0\" >\n"
                        + "\n"
                        + "    <uses-sdk\n"
                        + "        android:minSdkVersion=\"8\"\n"
                        + "        android:targetSdkVersion=\"16\" />\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:icon=\"@drawable/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "        <activity\n"
                        + "            android:name=\".Foo.Bar\"\n"
                        + "            android:label=\"@string/app_name\" >\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                        + "\n"
                        + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"),
                classpath(),
                mApiCallTest,
                mBar)
                .issues(MISSING, INSTANTIATABLE, INNERCLASS)
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for AndroidManifest.xml line 14: Replace with .Foo$Bar:\n"
                        + "@@ -15 +15\n"
                        + "-             android:name=\".Foo.Bar\"\n"
                        + "+             android:name=\".Foo$Bar\"\n");
    }

    public void testNoClassesWithLibraries() throws Exception {
        mScopes = null;
        mEnabled = Sets.newHashSet(MISSING, INSTANTIATABLE, INNERCLASS);
        //noinspection all // Sample code
        assertEquals(
            "No warnings.",

            lintProject(
                mAndroidManifestWrongRegs,
                classpath(),
                base64gzip("libs/foo.jar", ""
                            + "H4sIAAAAAAAAAAvwZmYRYeAAQoFoOwcGJMDJwMLg6xriqOvp56b/7xQDAzND"
                            + "gDc7B0iKCaokAKdmESCGa/Z19PN0cw0O0fN1++x75rSPt67eRV5vXa1zZ85v"
                            + "DjK4YvzgaZGel6+Op+/F0lUsnDNeSh6RjtTKsBATebJEq+KZ6uvMT0UfixjB"
                            + "tk83XmlkAzTbBmo7F8Q6NNtZgbgktbhEH7cSPpiSpMqS1OT8lFR9hGfQ1cph"
                            + "qHVPLSlJLQoBiukl5yQWF5cGxeYLOYrYMuf8LODq2LJty62krYdWLV16wak7"
                            + "d5EnL+dVdp/KuIKja3WzE7K/5P+wrglYbPrxYLhw/ZSP9xJ3q26onbmz+L3t"
                            + "83mWxvX///7iXdDx14CJqbjPsoDrbX/fzY3xM1vTlz2e8Xf6FG5llQk2Zvek"
                            + "W4UXX9fdkyE/W9bdwdp2w1texsDyx4scVhXevF7yK2z97tNH1d3mS21lNJ3K"
                            + "siwr7HzRN5amnX8mOrzQPNut2NFyxNSj0eXwq5nnz/vdNrmfMX+GT3Z5z2Tl"
                            + "xfkfb/q2zTG/5qBweYeXRS9fuW/6iklpVxcL7NBcmHhq9YRnJXr2K2dFi6sc"
                            + "6pgQl31A/MGV3M4XHFXGTWsYni6f3XexsjpjT/HWnV+Fkt95HnEzSA2at/r5"
                            + "SZOPD5tmh5x5oua6Yhnj/Sl5wsqrTDtN0iyips84bOPu2rk0MWRShGTYdpWw"
                            + "wvmLu44opSndUGSPu222PEuo8gXTxmW1197PYBfj9ou5te2Y1YSl5xRq+wWY"
                            + "ciRcGcuc3waW9n3cmvHc+tLujdwlWhf8pjlcrlf6F7pVPXNu0EmFdZe12nk9"
                            + "HrLdsNl1ieWHdZp9f2PyvoSig+xzfhqx9f1uEq9Vvy81f84nVv3Kyfwro79+"
                            + "fGLf8WrlU/kTMSc4tJbtKCqeZ3NGIK2wxfCp0b3AvUmzJmnPW2caHv5C+l3f"
                            + "6VN9E1psIr980NvmVP2A682qQ+f4XutNWzxnFfc/RT3vq6kfayezK5vMcl8c"
                            + "aLcoQ67q/6PJrwN97Y8vFtNljTOruJnz0vPWKZn87V9Cvsrs1t2/7fT7EJW4"
                            + "OhPe11/0zSYs8JGaHeHAeVpjMmu0SfVsLdGuVTeOnuuIND2/5nhX4Xt7UEY4"
                            + "ZPg5Pw+YD7lZQRmBkUmEATUjwrIoKBejApQ8ja4VOX+JoGizxZGjQSZwMeDO"
                            + "hwiwG5ErcWvhQ9FyD0suRTgYpBc5HORQ9HIxEsq1Ad6sbBBnsjJYAFUfYQbx"
                            + "AFJZ3LASBQAA")
            ));
    }

    public void testFragment() throws Exception {
        mScopes = null;
        mEnabled = Sets.newHashSet(MISSING, INSTANTIATABLE, INNERCLASS);
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/fragment2.xml:7: Error: Class referenced in the layout file, my.app.Fragment, was not found in the project or the libraries [MissingRegistered]\n"
                + "    <fragment\n"
                + "    ^\n"
                + "res/layout/fragment2.xml:12: Error: Class referenced in the layout file, my.app.MyView, was not found in the project or the libraries [MissingRegistered]\n"
                + "    <view\n"
                + "    ^\n"
                + "res/layout/fragment2.xml:17: Error: Class referenced in the layout file, my.app.Fragment2, was not found in the project or the libraries [MissingRegistered]\n"
                + "    <fragment\n"
                + "    ^\n"
                + "src/test/pkg/Foo/Bar.java:6: Error: The default constructor must be public [Instantiatable]\n"
                + "    private Bar() {\n"
                + "    ^\n"
                + "4 errors, 0 warnings\n",

        lintProject(
            mAndroidManifestRegs,
            classpath(),
            mOnClickActivity,
            mOnClickActivity_class,
            mTestService,
            mTestService_class,
            mTestProvider,
            mTestProvider_class,
            mTestProvider2,
            mTestProvider2_class,
            mTestReceiver,
            mTestReceiver_class,
            mFoo,
            mFoo_class,
            mBar,
            mBar_class,
            xml("res/layout/fragment2.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\"\n"
                            + "    android:orientation=\"vertical\" >\n"
                            + "\n"
                            + "    <fragment\n"
                            + "        class=\"my.app.Fragment\"\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"wrap_content\" />\n"
                            + "\n"
                            + "    <view\n"
                            + "        class=\"my.app.MyView\"\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"wrap_content\" />\n"
                            + "\n"
                            + "    <fragment\n"
                            + "        android:name=\"my.app.Fragment2\"\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"wrap_content\" />\n"
                            + "\n"
                            + "    <view\n"
                            + "        android:name=\"test.pkg.TestService\"\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"wrap_content\" />\n"
                            + "\n"
                            + "    <view\n"
                            + "        class=\"test.pkg.TestService\"\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"wrap_content\" />\n"
                            + "\n"
                            + "    <fragment\n"
                            + "        android:name=\"test.pkg.TestService\"\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"wrap_content\" />\n"
                            + "\n"
                            + "    <fragment\n"
                            + "        class=\"test.pkg.TestService\"\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"wrap_content\" />\n"
                            + "\n"
                            + "    <fragment\n"
                            + "        class=\"test.pkg.Foo$Bar\"\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"wrap_content\" />\n"
                            + "\n"
                            + "    <fragment\n"
                            + "        class=\"test.pkg.Nonexistent\"\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        tools:ignore=\"MissingRegistered\" />\n"
                            + "\n"
                            + "</LinearLayout>\n")
        ));
    }

    public void testAnalytics() throws Exception {
        mScopes = null;
        mEnabled = Sets.newHashSet(MISSING, INSTANTIATABLE, INNERCLASS);
        //noinspection all // Sample code
        assertEquals(""
                + "res/values/analytics.xml:13: Error: Class referenced in the analytics file, com.example.app.BaseActivity, was not found in the project or the libraries [MissingRegistered]\n"
                + "  <string name=\"com.example.app.BaseActivity\">Home</string>\n"
                + "  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/values/analytics.xml:14: Error: Class referenced in the analytics file, com.example.app.PrefsActivity, was not found in the project or the libraries [MissingRegistered]\n"
                + "  <string name=\"com.example.app.PrefsActivity\">Preferences</string>\n"
                + "  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "2 errors, 0 warnings\n",

            lintProject(
                classpath(),
                xml("res/values/analytics.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n"
                            + "<resources>\n"
                            + "  <!--Replace placeholder ID with your tracking ID-->\n"
                            + "  <string name=\"ga_trackingId\">UA-12345678-1</string>\n"
                            + "\n"
                            + "  <!--Enable Activity tracking-->\n"
                            + "  <bool name=\"ga_autoActivityTracking\">true</bool>\n"
                            + "\n"
                            + "  <!--Enable automatic exception tracking-->\n"
                            + "  <bool name=\"ga_reportUncaughtExceptions\">true</bool>\n"
                            + "\n"
                            + "  <!-- The screen names that will appear in your reporting -->\n"
                            + "  <string name=\"com.example.app.BaseActivity\">Home</string>\n"
                            + "  <string name=\"com.example.app.PrefsActivity\">Preferences</string>\n"
                            + "  <string name=\"test.pkg.OnClickActivity\">Clicks</string>\n"
                            + "</resources>\n"),
                mOnClickActivity,
                mOnClickActivity_class
            ));
    }

    public void testCustomView() throws Exception {
        mScopes = null;
        mEnabled = Sets.newHashSet(MISSING, INSTANTIATABLE, INNERCLASS);
        //noinspection all // Sample code
        assertEquals(""
                + "res/layout/customview.xml:21: Error: Class referenced in the layout file, foo.bar.Baz, was not found in the project or the libraries [MissingRegistered]\n"
                + "    <foo.bar.Baz\n"
                + "    ^\n"
                + "1 errors, 0 warnings\n",

                lintProject(
                        classpath(),
                        xml("res/layout/customview.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "    xmlns:other=\"http://schemas.foo.bar.com/other\"\n"
                            + "    xmlns:foo=\"http://schemas.android.com/apk/res/foo\"\n"
                            + "    android:id=\"@+id/newlinear\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\"\n"
                            + "    android:orientation=\"vertical\" >\n"
                            + "\n"
                            + "    <foo.bar.Baz\n"
                            + "        android:id=\"@+id/button1\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:text=\"Button1\"\n"
                            + "        foo:misc=\"Custom attribute\"\n"
                            + "        tools:ignore=\"HardcodedText\" >\n"
                            + "    </foo.bar.Baz>\n"
                            + "\n"
                            + "    <!-- Wrong namespace uri prefix: Don't warn -->\n"
                            + "    <foo.bar.Baz\n"
                            + "        android:id=\"@+id/button1\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        android:text=\"Button1\"\n"
                            + "        other:misc=\"Custom attribute\"\n"
                            + "        tools:ignore=\"HardcodedText\" >\n"
                            + "    </foo.bar.Baz>\n"
                            + "\n"
                            + "</LinearLayout>\n"),
                        mOnClickActivity,
                        mOnClickActivity_class
                ));
    }

    public void testCustomViewInCapitalizedPackage() throws Exception {
        mScopes = null;
        mEnabled = Sets.newHashSet(MISSING, INSTANTIATABLE, INNERCLASS);
        //noinspection all // Sample code
        assertEquals(""
                + "No warnings.",

                lintProject(
                        classpath(),
                        xml("res/layout/customview3.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                            + "    android:id=\"@+id/newlinear\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\"\n"
                            + "    android:orientation=\"vertical\" >\n"
                            + "\n"
                            + "    <test.Pkg.CustomView3\n"
                            + "        android:id=\"@+id/button1\"\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        tools:ignore=\"HardcodedText\" />\n"
                            + "\n"
                            + "</LinearLayout>\n"),
                        mCustomView3,
                        mCustomView3_class
                ));
    }

    public void testCustomViewNotReferenced() throws Exception {
        mScopes = null;
        mEnabled = Sets.newHashSet(MISSING, INSTANTIATABLE, INNERCLASS);
        //noinspection all // Sample code
        assertEquals(""
                + "No warnings.",

                lintProject(
                        classpath(),
                        mCustomView3,
                        mCustomView3_class
                ));
    }

    public void testMissingClass() throws Exception {
        mScopes = null;
        mEnabled = Sets.newHashSet(MISSING, INSTANTIATABLE, INNERCLASS);
        //noinspection all // Sample code
        assertEquals(""
                + "No warnings.",

                lintProject(
                        classpath(),
                        xml("res/layout/user_prefs_fragment.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<fragment xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    class=\"course.examples.DataManagement.PreferenceActivity.ViewAndUpdatePreferencesActivity$UserPreferenceFragment\"\n"
                            + "    android:id=\"@+id/userPreferenceFragment\">\n"
                            + "</fragment>\n"),
                        base64gzip("bin/classes/course/examples/DataManagement/PreferenceActivity/ViewAndUpdatePreferencesActivity$UserPreferenceFragment.class", ""
                            + "H4sIAAAAAAAAAL1W6VITQRD+JglsjuUwKpcHh1GXiC7ghXIoBFE0HBKkyp9D"
                            + "dgyrySzublB/+QK+gu+gVYClVvkAPpRlzxJICpRQpUWq0jvT9fXdMz0/f337"
                            + "AWAYyzG0YDCOVgwpMhx+HwHixLuuyI0obsZxC7c1jMQRwx2Fvqs4owmMYTyB"
                            + "TkxouK9hkqHtmSfcRVe8EK6QeTHj8kJJSJ9Bn5VSuJki9zzhMYAhvDz5kCGZ"
                            + "fck3uFnksmDmfNeWhVGGpowjPZ9Lf4UXyyKKKYZYKWt7viAdGjIMPQsyt8Zd"
                            + "YVVtZdZIh9hFMUxnubRcx7bMvCOJ55v7JbxUPS3ky8mSCmmel0QVxNC9p3x9"
                            + "j2tWASTXOGZL25+gOI3+FYZIxrFIriVrSzFfLq0Kd5mvFoVKgJPnxRXu2mpf"
                            + "YUb8NZuy9Cqbd8quJ0zxlpfWi8Izp7nP57jkBaHSWmNxMu/bG7b/zlyxxZtJ"
                            + "aT1bt7hf47K3C0j9uULkcdSRGVeQFJXR2IvP8cypsrSKYlSFkfT4hrBmg+rk"
                            + "Rc4P0Cf/AGZoUJmhIC7UrwOh4zxPCy81ODjI8ME4xsD765Yy9JZ8iufIIxKz"
                            + "VX0u1jN2TXW1jrM4p+M8unVcxCUd03igYwbU9g1lSR2l4ZGOWTymSh9bvKkh"
                            + "5dETHR04oyOLOQ3zOhawqOMpljTkGOxjc4YyeWjyq7gTuwq8Km/q3w84Qxe3"
                            + "aiVmXKe0JLyg1nQOjVnV9acKwq9idjJBooZRp3cqSGqh1FFwDM0vbGnV3jN3"
                            + "jJr7kVx3c+J1OWjL+m0rj/UUVdJ0oARU4po0HXYF9NYFUcpdUVClc+sX9qHx"
                            + "PwaAiqvT+QvKYhg5gpXRgzNOqV36/9VBL83mVhrqYbp6TuE0jdk22oXQTn86"
                            + "8MGaLqUAwdTNRLSHdiZ9aSqjIf0Foc8BrJdoY8BsRx9RfQeAC0hBzW+60SrC"
                            + "HxEhdUAmfeULwulkZJv0pLfRmNS2Ed0CS39H7Dlpjm8hofjb0OcG0ptIfEVT"
                            + "CGoxkNS+ojmMquk+RIl2k8E+JMhYB/rJ8A0MYEQ9PTCF8cCtnh3TFbfU6jIM"
                            + "cmyA3O5HGqHWKK7Qbn+cm2CfDo3zaqAwhGsBNZGkbxcZa8E9nKB3UAw7v056"
                            + "BHU1xn4DOJTEmk4JAAA=")
                ));
    }

    @SuppressWarnings("ClassNameDiffersFromFileName")
    public void testMissingClassViaSource() throws Exception {
        mScopes = null;
        mEnabled = Sets.newHashSet(MISSING, INSTANTIATABLE, INNERCLASS);
        assertEquals("No warnings.",

                lintProject(
                        xml("res/layout/user_prefs_fragment.xml", ""
                                + "<fragment xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                                + "    class=\"course.examples.dataManagement.ViewAndUpdatePreferencesActivity\"\n"
                                + "    android:id=\"@+id/userPreferenceFragment\">\n"
                                + "</fragment>\n"),
                        mCustomView3_class,
                        java("src/course/examples/dataManagement/ViewAndUpdatePreferencesActivity.java", ""
                                + "package course.examples.DataManagement;\n"
                                + "public class ViewAndUpdatePreferencesActivity {\n"
                                + "}\n")
                ));
    }

    public void testFragments() throws Exception {
        mScopes = Scope.MANIFEST_SCOPE;
        mEnabled = Sets.newHashSet(MISSING, INSTANTIATABLE, INNERCLASS);

        // Ensure that we don't do instantiation checks here since they are handled by
        // the FragmentDetector
        assertEquals(
                "No warnings.",

                lintProject(
                        mFragmentTest$Fragment1,
                        mFragmentTest$Fragment2,
                        mFragmentTest$Fragment3,
                        mFragmentTest$Fragment4,
                        mFragmentTest$Fragment5,
                        mFragmentTest$Fragment6,
                        mFragmentTest$NotAFragment,
                        mFragmentTest));
    }

    public void testHeaders() throws Exception {
        // See https://code.google.com/p/android/issues/detail?id=51851
        mScopes = null;
        mEnabled = Sets.newHashSet(MISSING, INNERCLASS);
        //noinspection all // Sample code
        assertEquals(""
                + "res/xml/prefs_headers.xml:3: Error: Class referenced in the preference header file, foo.bar.MyFragment.Missing, was not found in the project or the libraries [MissingRegistered]\n"
                + "<header\n"
                + "^\n"
                + "1 errors, 0 warnings\n",

                lintProject(
                        mFragmentTest$Fragment1,
                        mFragmentTest$Fragment2,
                        mFragmentTest$Fragment3,
                        mFragmentTest$Fragment4,
                        mFragmentTest$Fragment5,
                        mFragmentTest$Fragment6,
                        mFragmentTest$NotAFragment,
                        mFragmentTest,
                        classpath(),
                        xml("res/xml/prefs_headers.xml", ""
                            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<preference-headers xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                            + "<header\n"
                            + "  android:fragment=\"foo.bar.MyFragment$Missing\"\n"
                            + "  android:summary=\"@string/summary\"\n"
                            + "  android:title=\"@string/title\" />\n"
                            + "<header android:fragment=\"test.pkg.FragmentTest$Fragment1\" />\n"
                            + "<header android:fragment=\"test.pkg.FragmentTest.Fragment1\" />\n"
                            + "</preference-headers>\n")));
    }

    @SuppressWarnings("all") // Sample code
    private TestFile mAndroidManifest = xml("AndroidManifest.xml", ""
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    package=\"test.pkg\"\n"
            + "    android:versionCode=\"1\"\n"
            + "    android:versionName=\"1.0\" >\n"
            + "\n"
            + "    <uses-sdk\n"
            + "        android:minSdkVersion=\"8\"\n"
            + "        android:targetSdkVersion=\"16\" />\n"
            + "\n"
            + "    <application\n"
            + "        android:icon=\"@drawable/ic_launcher\"\n"
            + "        android:label=\"@string/app_name\"\n"
            + "        android:theme=\"@style/AppTheme\" >\n"
            + "        <activity\n"
            + "            android:name=\".Foo$Bar\"\n"
            + "            android:label=\"@string/app_name\" >\n"
            + "            <intent-filter>\n"
            + "                <action android:name=\"android.intent.action.MAIN\" />\n"
            + "\n"
            + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
            + "            </intent-filter>\n"
            + "        </activity>\n"
            + "        <activity\n"
            + "            android:name=\".Foo$Baz\"\n"
            + "            android:label=\"@string/app_name\" >\n"
            + "        </activity>\n"
            + "    </application>\n"
            + "\n"
            + "</manifest>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mAndroidManifestInner = xml("AndroidManifest.xml", ""
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    package=\"test.pkg.Foo\"\n"
            + "    android:versionCode=\"1\"\n"
            + "    android:versionName=\"1.0\" >\n"
            + "\n"
            + "    <uses-sdk\n"
            + "        android:minSdkVersion=\"8\"\n"
            + "        android:targetSdkVersion=\"16\" />\n"
            + "\n"
            + "    <application\n"
            + "        android:icon=\"@drawable/ic_launcher\"\n"
            + "        android:label=\"@string/app_name\"\n"
            + "        android:theme=\"@style/AppTheme\" >\n"
            + "        <activity\n"
            + "            android:name=\".Bar\"\n"
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
    private TestFile mAndroidManifestRegs = xml("AndroidManifest.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<!--\n"
            + "  ~ Copyright (C) 2012 The Android Open Source Project\n"
            + "  ~\n"
            + "  ~  Licensed under the Apache License, Version 2.0 (the \"License\");\n"
            + "  ~  you may not use this file except in compliance with the License.\n"
            + "  ~  You may obtain a copy of the License at\n"
            + "  ~\n"
            + "  ~       http://www.apache.org/licenses/LICENSE-2.0\n"
            + "  ~\n"
            + "  ~  Unless required by applicable law or agreed to in writing, software\n"
            + "  ~  distributed under the License is distributed on an \"AS IS\" BASIS,\n"
            + "  ~  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
            + "  ~  See the License for the specific language governing permissions and\n"
            + "  ~  limitations under the License.\n"
            + "  -->\n"
            + "\n"
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    package=\"test.pkg\"\n"
            + "    android:versionCode=\"1\"\n"
            + "    android:versionName=\"1.0\" >\n"
            + "\n"
            + "    <uses-sdk android:minSdkVersion=\"10\" />\n"
            + "\n"
            + "    <application\n"
            + "        android:icon=\"@drawable/ic_launcher\"\n"
            + "        android:label=\"@string/app_name\" >\n"
            + "        <provider android:name=\".TestProvider\" />\n"
            + "        <provider android:name=\"test.pkg.TestProvider2\" />\n"
            + "        <service android:name=\".TestService\" />\n"
            + "        <activity android:name=\"OnClickActivity\" />\n"
            + "        <receiver android:name=\"TestReceiver\" />\n"
            + "\n"
            + "    </application>\n"
            + "\n"
            + "</manifest>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mAndroidManifestWrongRegs = xml("AndroidManifest.xml", ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    package=\"test.pkg\"\n"
            + "    android:versionCode=\"1\"\n"
            + "    android:versionName=\"1.0\" >\n"
            + "\n"
            + "    <uses-sdk android:minSdkVersion=\"10\" />\n"
            + "\n"
            + "    <application\n"
            + "        android:icon=\"@drawable/ic_launcher\"\n"
            + "        android:label=\"@string/app_name\" >\n"
            + "        <!-- These registrations are bogus (wrong type) -->\n"
            + "        <activity android:name=\".TestProvider\" />\n"
            + "        <service android:name=\"test.pkg.TestProvider2\" />\n"
            + "        <provider android:name=\".TestService\" />\n"
            + "        <receiver android:name=\"OnClickActivity\" />\n"
            + "        <service android:name=\"TestReceiver\" />\n"
            + "\n"
            + "    </application>\n"
            + "\n"
            + "</manifest>\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mApiCallTest = base64gzip("bin/classes/foo/bar/ApiCallTest.class", ""
            + "H4sIAAAAAAAAAJ1U7VLbRhQ9a4yFbVEIIQkfoaWUUNukVkISkmJKYwxJ3Mo4"
            + "kxCn7Z+OkNf2FlnSrASp+1RtpkNn+qMP0HfpK3RyVxaDx4HQqT2zn/eee+7d"
            + "e/T3v3/+BWANdQ0JhumQB6HhH7aNsi8qluPs015Dkm4styk90TQs3zfKdiiO"
            + "RdhjSG0KV4RbDCO5fIMhWfGaPIMRpHWMIsUwYQqX7x11D7jctw4czjBlerbl"
            + "NCwp1D4+TIYdETBcN8+LX6IwXR52vCbDw5x5SuSNaLZ5aFQ60nM9uueyZHqy"
            + "bby5ZxtNr2vs1GsqVOjJUr6RAcMVHVO4yqCTm8rAc7ctyTCby5vDyUVXJQ3X"
            + "GOaHMHel9OQzcnC4zOAGZjXMMMxdzErHHOYZbtJx3R043xf2oSmCkLucWDwa"
            + "YPE+xvKFniXFYUHHx/iE4WrAw33+c1gNXnKH22Fc3NwP+UYan+IzDUtEtWk5"
            + "x+LQOOiF3KbnMuq+mqpuy9OxjFsM47Xyd9Xaq9qPjbL5apeBVVX5PteRQ54h"
            + "TdxecN+TIcPKcO183xFUcypgVKe+XSmNVXyh4TbD0uXWOoowGLIHVki59hQv"
            + "hjv/Iczy9pkHhbyLexrWGBZPPdvS8jvCDoznZMzlzlGrtVyjzHXcxwMGrd7Y"
            + "fWGWvyeS5mUu1JJZ++xB6HU/1JaE7fQ7kWHmoh5lGOUqF6WC81qODEZs5xdq"
            + "Y/Mn69gyHMttGxXHCgLl2hLcIXlkWsJxnluSu/Q42a4V2p3TXbJLvCPwQfn1"
            + "/FMJTg/Dbha2CHmcknkqRdO0et4RwSy836dn12SfeekdSZs/EQpzckDFRQVP"
            + "4qu61LVRAB5o+IYE9oFiE21Vbg01hvv/Rx4k7wvvSBFUU6hfklb0vaJRo51B"
            + "M6N5tPAHxn6jRQIZGlPR4RVkadT7BjSP08zwESZi539ia6NwgsnXbG9qeiO5"
            + "eoLrr1eTJ7j5Fovro4n1VGJdI/CV31HYGHuLOxtpFWYkcixGAaYJ/AYBztBu"
            + "HpOk72vEdw5LWCCRLmIFefoXUYisW/2QMR21WsfDiKKBR/iSKGkk3g2UKEaW"
            + "/DfxFeU8R5hbuEWRFujL+DWtUoQ8gce00gg9pZSIMRoZyniANLbj1PsYFcIA"
            + "drAbp347rluC/TpUtOJA0RIxy4T6nFzquXau55NofIpnNE/R6i6q+PbxLEyq"
            + "2V4q/Q6UwMkB1gYAAA==");

    @SuppressWarnings("all") // Sample code
    private TestFile mBar = java(""
            + "package test.pkg.Foo;\n"
            + "\n"
            + "import android.app.Activity;\n"
            + "\n"
            + "public class Bar extends Activity {\n"
            + "    private Bar() {\n"
            + "    }\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mBar_class = base64gzip("bin/classes/test/pkg/Foo/Bar.class", ""
            + "H4sIAAAAAAAAAF2NzU7CQBRGv1sKxVopIb6AO3XBJLrUmIgJK+JGwn7aTvQq"
            + "dJrpQOJjuTJxwQPwUIY76Iq7mHN/Tubb/f5sAdxgmCAiDL1pvWo+XtXUWjXR"
            + "LkFMONd15SxXSjeNeiw9b9h/Enr3XLN/IHQurxaE+MlWJkUHJxm66BHyGdfm"
            + "eb0qjJvrYmkIo5kt9XKhHYf5fxn7N27D7Tj7jpC+2LUrzZSD2Jfd+F1vNC4g"
            + "oQhFiEKYMJHpVkjC7vU3+l/SREjlTYUQKRbpVLrsTxKeHb4YHMx8D9v6x2YM"
            + "AQAA");

    @SuppressWarnings("all") // Sample code
    private TestFile mCustomView3 = java(""
            + "package test.Pkg;\n"
            + "\n"
            + "import android.content.Context;\n"
            + "import android.util.AttributeSet;\n"
            + "import android.view.View;\n"
            + "\n"
            + "public class CustomView3 extends View {\n"
            + "\n"
            + "\tpublic CustomView3(Context context, AttributeSet attrs, int defStyle) {\n"
            + "\t\tsuper(context, attrs, defStyle);\n"
            + "\t}\n"
            + "\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mCustomView3_class = base64gzip("bin/classes/test/bytecode/CustomView3.class", ""
            + "H4sIAAAAAAAAAH1RTUvDQBB9U9Mmxmo/1IKIB29axYBehIggBaEQRGjpfZus"
            + "utomkE6q/ixPggd/gD9KnG1VPKg7MB9vhjdv2Lf3l1cAh2i5KBHWWE84uLy7"
            + "DjrFhLPxwOj7IxcOoaHSJM9MEkwFCixOqJyY1PAp4Xgn+mrHWco65aBj4wOH"
            + "342CzSg4Y87NsGDd0xx2dwcEp5Ml2scCFqsoo0KoRSbVF8V4qPO+Go40oRll"
            + "sRoNVG5s/Qk6fGMmhFb0m+KQ4MZzAYSNP7URykoECc3mPzIJXqKvevxo11KX"
            + "4PeyIo/1ubFA/cfag1s1VdgGyTn2ybi9SbwrVR+OGOC19/a3nuE9SV6CL96X"
            + "aL2LJTGgPZ9DFcszHg8rqAmLzepozKY9NLEqe+TPZjzrH9V09RbKAQAA");

    @SuppressWarnings("all") // Sample code
    private TestFile mFoo = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.app.Activity;\n"
            + "\n"
            + "public class Foo {\n"
            + "    public static class Bar extends Activity {\n"
            + "    }\n"
            + "    public class Baz extends Activity {\n"
            + "    }\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mFoo_class = base64gzip("bin/classes/test/pkg/Foo.class", ""
            + "H4sIAAAAAAAAAGVOu07DMBQ9t03jNoS2hNeMxAAMWOoK6kClSkgRDKDuTrCK"
            + "S0iQ7TL0r5iQGPiAfhTiJnSohC2dex7X8ln/fH0DGOFYoEWIvXZevr3M5bSq"
            + "BALCcKHelSxUOZf32ULnnhBem9L4MaF9dj4jBJPqSUdooxejg5AwSE2p75av"
            + "mbaPKis0IUmrXBUzZU2tN2bgn40j9NPtP68I0UO1tLmemnqpy95lXYG73Zal"
            + "tpNCOaedQMLdtl+e3ijLlRgFDv9nqyZb4QRMUB/iy30ZBSvZaKBz8YnuB5MW"
            + "Isbwz8QOY7zhMXabvN/gAEOeCbM9zvfRwwHPI9Av5fj6R1sBAAA=");

    @SuppressWarnings("all") // Sample code
    private TestFile mFragmentTest = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.annotation.SuppressLint;\n"
            + "import android.app.Fragment;\n"
            + "\n"
            + "@SuppressWarnings(\"unused\")\n"
            + "public class FragmentTest {\n"
            + "\n"
            + "\t// Should be public\n"
            + "\tprivate static class Fragment1 extends Fragment {\n"
            + "\n"
            + "\t}\n"
            + "\n"
            + "\t// Should be static\n"
            + "\tpublic class Fragment2 extends Fragment {\n"
            + "\n"
            + "\t}\n"
            + "\n"
            + "\t// Should have a public constructor\n"
            + "\tpublic static class Fragment3 extends Fragment {\n"
            + "\t\tprivate Fragment3() {\n"
            + "\t\t}\n"
            + "\t}\n"
            + "\n"
            + "\t// Should have a public constructor with no arguments\n"
            + "\tpublic static class Fragment4 extends Fragment {\n"
            + "\t\tprivate Fragment4(int dummy) {\n"
            + "\t\t}\n"
            + "\t}\n"
            + "\n"
            + "\t// Should *only* have the default constructor, not the\n"
            + "\t// multi-argument one\n"
            + "\tpublic static class Fragment5 extends Fragment {\n"
            + "\t\tpublic Fragment5() {\n"
            + "\t\t}\n"
            + "\t\tpublic Fragment5(int dummy) {\n"
            + "\t\t}\n"
            + "\t}\n"
            + "\n"
            + "\t// Suppressed\n"
            + "\t@SuppressLint(\"ValidFragment\")\n"
            + "\tpublic static class Fragment6 extends Fragment {\n"
            + "\t\tprivate Fragment6() {\n"
            + "\t\t}\n"
            + "\t}\n"
            + "\n"
            + "\tpublic static class ValidFragment1 extends Fragment {\n"
            + "\t\tpublic ValidFragment1() {\n"
            + "\t\t}\n"
            + "\t}\n"
            + "\n"
            + "\t// (Not a fragment)\n"
            + "\tprivate class NotAFragment {\n"
            + "\t}\n"
            + "\n"
            + "\t// Ok: Has implicit constructor\n"
            + "\tpublic static class Fragment7 extends Fragment {\n"
            + "\t}\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mFragmentTest$Fragment1 = base64gzip("bin/classes/test/pkg/FragmentTest$Fragment1.class", ""
            + "H4sIAAAAAAAAAIVPsU7DMBB9l6YJDYHSUmZAYgAGIlipWCpVQopYqLq7jVUM"
            + "qRPZLv/VCYmBD+CjEOcAFQMSHt679/zudPf+8foG4AqDGAHh0EnrsvppkY2N"
            + "WCyldhM2Tn7EZYyQMBC6MJUqMlHXmxwhGiqt3A2hdXo2JYSjqpAJWuikaCMi"
            + "dHOl5d1qOZNmImalJPTzai7KqTDK628zdA/KEo7zf1a5JiT31crM5Vj5vt7v"
            + "2MWjeBaE9FZraUalsFbaGH3CwZ9TCZ3NXByBT4B/hMCvzhyzypiJuX3+gq01"
            + "FwESxqgxE2wzpl8B5p3mf7fBLvaahG/vYR/JJ5sGcWt1AQAA");

    @SuppressWarnings("all") // Sample code
    private TestFile mFragmentTest$Fragment2 = base64gzip("bin/classes/test/pkg/FragmentTest$Fragment2.class", ""
            + "H4sIAAAAAAAAAHVQwUrDQBSc18TExmjT2lr1IoUeqoKR4k3xUigIwYul922z"
            + "1NU2KZvU//Igggc/wI8S3watHiIPhjdvZ2ff7Mfn2zuAPg5cVAhHuczycPk4"
            + "C4dazBYyyUc86P6Qvgub0BRJrFMVh2K5XOsITn6vsu45oR2Vulyy5EolKr8m"
            + "HPb+0RyPCfYgjWUVhC0fG3A8WNj24WKHYPWMoBapRN6uFhOpR2Iyl4RGlE7F"
            + "fCy0Mvx7aJuFCJ3yp35D8WLeXbrSUzlU5l79r+zsQTwJgn+TJFIP5iLLZOai"
            + "TWiVuhKqa190OIPFv0tBYIKYjsvFJmOV2QUqXIB3cvrC8Ar/mVkFNUaHNeAu"
            + "YPSL3kMdjeJ8t8AmWsXUeO5hH/QF9e37/swBAAA=");

    @SuppressWarnings("all") // Sample code
    private TestFile mFragmentTest$Fragment3 = base64gzip("bin/classes/test/pkg/FragmentTest$Fragment3.class", ""
            + "H4sIAAAAAAAAAIWPQUvDQBCF36RpY2K0trZepeBBPRiwR8VLoSAEL5bet81S"
            + "V9NN2E37vzwVevAH+KPE2ajFg+Ae5s0bvnnMvn9s3wBcoxfAI5xW0lZJ+bJI"
            + "xkYsllJXEx6c/ZhhAJ/QEzozhcoSUZY7jtC6VVpVd4TG+cWU4I+KTEZoIIzR"
            + "RIvQTpWWD6vlTJqJmOWS0E2Lucinwijnv4d+9aQsYZD+c8oNIXosVmYux8rt"
            + "dX5jV89iLQjxvdbSjHJhrbQBuoT+n6mEcJeLAfgLcI/gudNZA3ZDVmJtXm6w"
            + "98qNh4hrxAr04eME+9zFXxDrQc0c1rWNo5p1ER0cI/wEqt0hGnkBAAA=");

    @SuppressWarnings("all") // Sample code
    private TestFile mFragmentTest$Fragment4 = base64gzip("bin/classes/test/pkg/FragmentTest$Fragment4.class", ""
            + "H4sIAAAAAAAAAIVPTUvDQBB9kyaNidHa2upBQQoeag8GxJsfl0IhELxYet82"
            + "S11NNmGTCP4sT4IHf4A/StysWjwIzmFm3uO9+Xj/eH0DcIZ9FxbhqOJlFRYP"
            + "q3Cq2Crjsppp4vgHnLuwCX0mE5WLJGRFsdYR2pdCiuqaYI+ik7kukzzhPlrw"
            + "AjjwCa1RQ3diIflNnS24mrFFygm9OF+ydM6UaPA3aVd3oiQM438uuiA4SZ1l"
            + "TwSKCP5tXqsln4pmRve35fSePTJCEEnJ1SRlZclLFwPC4M8NBG+9A0Po69EE"
            + "wdLPtHV1NboyGHDGL9h41o2FTZ19Qx7AxiEC3e19ibCFbTPEQQc7xto1nh52"
            + "jUu/gL5We58sFTuBlgEAAA==");

    @SuppressWarnings("all") // Sample code
    private TestFile mFragmentTest$Fragment5 = base64gzip("bin/classes/test/pkg/FragmentTest$Fragment5.class", ""
            + "H4sIAAAAAAAAAIVPTU/CQBB9U1oqFUUQ9GhQTMCDTTSe/LiQkDRpvEi4L3SD"
            + "q/QjbTHxZ3ky8eAP8EcZZ0slHjTuYd7M7HtvZj4+394BnGHfhkE4yGWWu8nj"
            + "3B2lYh7KKB9zo/ddXNgwCW0RBWmsAlckyZpHqF6pSOU3hEp/MCGYwziQDiqo"
            + "1WGhSmj4KpK3y3Aq07GYLiSh5cczsZiIVOm6bJr5vcoIXf+fVS6Z2vf0JCtY"
            + "huEzgTyCcxcv05kcKW3V/Kk8fRBPglD3okimw4XIMpnZ6BA6vw4i1Naj0AVf"
            + "Bf0MzvgajjZX54zEaJ28YuOl+HY4OozAIUwcYZOz+orEuMWoDbZLg2tG4y+D"
            + "HhscFwZ7K1JpoLMGdgpps9C0sFuo9DZtZte+AKNdLuLXAQAA");

    @SuppressWarnings("all") // Sample code
    private TestFile mFragmentTest$Fragment6 = base64gzip("bin/classes/test/pkg/FragmentTest$Fragment6.class", ""
            + "H4sIAAAAAAAAAIVQTUvDQBB906aNja21tVXBgxQ8+IUBBS+KIIVCoXiwpRdP"
            + "22apq+kmZDf9X54ED/4Af5Q4qRo9CC7svpnZNzNv5u395RXAKbZdFAi7Vhrr"
            + "x48zv5eI2VxqO+LA3rdz7sIhtIQOkkgFvojjnEcoXyqt7BWhuH8wJjjdKJAe"
            + "iqhUUUKZUB8oLW/S+UQmIzEJJaE5iKYiHItEZf5X0LH3yhA6g3+kXBC8YZQm"
            + "U9lTWV7jN+3kQSwEYec21VbNZV8vlFFc/lrryAqrIr1skQ+Sh/1hGseJNIbF"
            + "Wm5RWogw5eq1sQhV8DNsta+1TLqhMEYaF5uE9p96CZVcMTrg5SA7hEK2FEaX"
            + "vTNGYiwdPmPliY0CPH49RuAIDo6xylb1k8RYYyxibcmsY51xjf8afJt3IIMN"
            + "tJb5Wdk2tlD5ALruRj7nAQAA");

    @SuppressWarnings("all") // Sample code
    private TestFile mFragmentTest$NotAFragment = base64gzip("bin/classes/test/pkg/FragmentTest$NotAFragment.class", ""
            + "H4sIAAAAAAAAAHVQwUrDQBScl8TGxmjTaq16EsmhVTAielIEKRSEUA+W3rdx"
            + "ialpIsnW//Igggc/wI8S3waFCpWF2Tezs2/f7OfX+weAU+zaMAgHSpYqeHqM"
            + "g0Eh4pnM1IgFf5ir61/BhkXwpuJZBKnI4uB2MpWRItTUQ1L6J4ROuLTJBVsu"
            + "kyxRV4S97j+e3phg9fN7WQdhzcUKag5MrLuwsUEwu9rQCJNMDueziSxGYpJK"
            + "QivMI5GORZFo/iNaeiCCv/ypP5l4NucunxeRHCT6anPReayzEtybLJNFPxVl"
            + "KUsbHUJ7aWN2LrbGPicx+YvJ83QcXcHgOKu815mdMTN4dw6PXhne4L4wM9Bg"
            + "rLEHOIfH6Fa1gyZa1flmhVtoVyrx2sYOjG/n52rE0QEAAA==");

    @SuppressWarnings("all") // Sample code
    private TestFile mOnClickActivity = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.app.Activity;\n"
            + "import android.util.Log;\n"
            + "import android.view.View;\n"
            + "\n"
            + "/** Test data for the OnClickDetector */\n"
            + "public class OnClickActivity extends Activity {\n"
            + "    // Wrong argument type 1\n"
            + "    public void wrong1() {\n"
            + "    }\n"
            + "\n"
            + "    // Wrong argument type 2\n"
            + "    public void wrong2(int i) {\n"
            + "    }\n"
            + "\n"
            + "    // Wrong argument type 3\n"
            + "    public void wrong3(View view, int i) {\n"
            + "    }\n"
            + "\n"
            + "    // Wrong return type\n"
            + "    public int wrong4(View view) {\n"
            + "        return 0;\n"
            + "    }\n"
            + "\n"
            + "    // Wrong modifier (not public)\n"
            + "    void wrong5(View view) {\n"
            + "    }\n"
            + "\n"
            + "    // Wrong modifier (is static)\n"
            + "    public static void wrong6(View view) {\n"
            + "    }\n"
            + "\n"
            + "    public void ok(View view) {\n"
            + "    }\n"
            + "\n"
            + "    // Ok: Unicode escapes\n"
            + "    public void my\\u1234method(View view) {\n"
            + "    }\n"
            + "\n"
            + "    // Typo\n"
            + "    public void simple_tyop(View view) {\n"
            + "    }\n"
            + "\n"
            + "    void wrong7(View view) {\n"
            + "        Log.i(\"x\", \"wrong7: called\");\n"
            + "    }\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mOnClickActivity_class = base64gzip("bin/classes/test/pkg/OnClickActivity.class", ""
            + "H4sIAAAAAAAAAIWS3W4SQRTH/2ehfCstYKm1KrUqUJJurLYmYjRNE5Mmm3pR"
            + "g5dmCxs6suxuloXKG/gw3piYaLzwAXwLX8R4ZtldEaGQzJw5H//fOTPLz9/f"
            + "fwDYRyMJhbDhGQNPdXpd9bV1bIp276jtiZHwxknECSXd6ri26Ki646hhhpB4"
            + "LizhvSDEavUWIX5sd4wMYkjnsIIEIa8Jyzgd9s8N941+bhqEgma3dbOlu0L6"
            + "QTDuXYgBYVNbNEOTW126ttV9FB72WVU7kU1J8DoJ448J5ZoWTjsSxqXa4q3p"
            + "l8alTyjOyYf6J4T1efp61OFgUUUrrDgkKHaPkO2Pf3380je8C7vD3kD0HdN4"
            + "541tJ6x8msI2T/8hhR3C9UnoWYUfyDQ6GTxANYmHhNWw2dATpqrZ3RzWUCNU"
            + "a9p7faSrpm511TPPFVa3+X9ETp45s4du23gl5GuXZh53T0qwDf6KkL8sn/jr"
            + "8Z5kT2VLbFd2vyL1mQ8KMrwn/GCKi4HcpIDtNbZ8kUjc8H1es8LslJAiYR6r"
            + "gfDAr58jzPvC9UkyEMrTGgr+oEWUAsRLXrF5iKKPqEySU4gbjKUIprAtYyOA"
            + "HbKVLZXYpxlaeWogJaIpEe0mNpfdaWvBnSaING5FiIXvWfnnPaVQ2q3lvXeu"
            + "7E24vRxRXYK4sxzRuBIB3I0QR1wjq9KFSuHeN9x/+xeV8RN7/KdUp3DpCJeO"
            + "Jqr7mt0/knyD1QIFAAA=");

    @SuppressWarnings("all") // Sample code
    private TestFile mTestProvider = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.content.ContentProvider;\n"
            + "import android.content.ContentValues;\n"
            + "import android.database.Cursor;\n"
            + "import android.net.Uri;\n"
            + "\n"
            + "public class TestProvider extends ContentProvider {\n"
            + "    @Override\n"
            + "    public int delete(Uri uri, String selection, String[] selectionArgs) {\n"
            + "        return 0;\n"
            + "    }\n"
            + "\n"
            + "    @Override\n"
            + "    public String getType(Uri uri) {\n"
            + "        return null;\n"
            + "    }\n"
            + "\n"
            + "    @Override\n"
            + "    public Uri insert(Uri uri, ContentValues values) {\n"
            + "        return null;\n"
            + "    }\n"
            + "\n"
            + "    @Override\n"
            + "    public boolean onCreate() {\n"
            + "        return false;\n"
            + "    }\n"
            + "\n"
            + "    @Override\n"
            + "    public Cursor query(Uri uri, String[] projection, String selection,\n"
            + "            String[] selectionArgs, String sortOrder) {\n"
            + "        return null;\n"
            + "    }\n"
            + "\n"
            + "    @Override\n"
            + "    public int update(Uri uri, ContentValues values, String selection,\n"
            + "            String[] selectionArgs) {\n"
            + "        return 0;\n"
            + "    }\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mTestProvider_class = base64gzip("bin/classes/test/pkg/TestProvider.class", ""
            + "H4sIAAAAAAAAAJVSXWsTQRQ9N99JW9u0ptX60VRtaYq4ICJIpCgBQQkqNAZR"
            + "ECbZIU6Nu3F2NtD/5IM+CT74A/xR4t3ZTSjZVes+zJ2de8+Zc8+dn7++/wBw"
            + "F/tl5AgNIwPjTD6MnB5vXmp/qlypyygQdoTnal+5ztD3jPSM04njrIhQeqg8"
            + "ZY4I+YNWn1Do+K6sIY/qMoooEVa7ypPPw48DqXtiMJaE9a4/FOO+0Cr6Tw4L"
            + "5r0KCFvdTC1tvseVY2m48sFBdybKk8Z5pVW7eyKmwhkLb+QcG628Uftt+qj1"
            + "lDWGWhHqKQJCNWD6oVG+FwlMgQkr84LHesRKNzKuIJRH0vROJyxzLy2zlYUo"
            + "KS+Q2hA6GX39wf2+GIcyYL50H6WpzfHk/gEmVHyvo6WIPOXZvSEUP4VSnxKm"
            + "aSUZzZ7L9IwxzLldYcRABNLphDrwoxnXJto/mU2hymfmhY4fWThxrdDX/23S"
            + "ud9G7dgP9VA+UdFzrJ99fXeieuyCfUL0lXnHb5vXMv85HIlj8fAbKl95k0ON"
            + "15I9rGCJ1+W4gOMKR8IFrCbgZxwLESb/eQG5ZJGHcTZBRrs11G0+h3Vs8Brt"
            + "LqLB0gib2Ep473O0OfqywLtmeTcT5CIv4RIuJxyPkobTHA3L0Yyzmdq2cYVX"
            + "wlVcS9huJzalO90+41Fu7tF17CTIdxxLmTqaFnkUZzN1NFlrbu5Wfu5W7Oou"
            + "nxW58gZuJnf17GizVO7bu+7F2b/0nHUX4Zb92/sNz5RSnnkFAAA=");

    @SuppressWarnings("all") // Sample code
    private TestFile mTestProvider2 = java(""
            + "package test.pkg;\n"
            + "\n"
            + "public class TestProvider2 extends TestProvider {\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mTestProvider2_class = base64gzip("bin/classes/test/pkg/TestProvider2.class", ""
            + "H4sIAAAAAAAAAG1Oyw7BUBA9o9VSz0iIrR0WmtgSG4lVIxJif1s3XK/KVf7L"
            + "SmLhA3yUGJeNxExyZs6ceT2etzuALsouUoRaIo+Jf9gs/RknEx2f1ULqrgub"
            + "UP2rEZy+2qtkQLCarTnBHsYL6cFCNo80HEIpUHs5Pu1CqWci3EpCJYgjsZ0L"
            + "rd78W7STlToS6sH/F3oEbxqfdCRHyuz4UTtrcRZogJ/A24idjzO6zHzDgXT7"
            + "isyFkxQ8RscULeQY858GjgUzXjRdpRfSkeoDHwEAAA==");

    @SuppressWarnings("all") // Sample code
    private TestFile mTestReceiver = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.content.BroadcastReceiver;\n"
            + "import android.content.Context;\n"
            + "import android.content.Intent;\n"
            + "\n"
            + "public class TestReceiver extends BroadcastReceiver {\n"
            + "\n"
            + "    @Override\n"
            + "    public void onReceive(Context context, Intent intent) {\n"
            + "    }\n"
            + "\n"
            + "    // Anonymous classes should NOT be counted as a must-register\n"
            + "    private BroadcastReceiver dummy() {\n"
            + "        return new BroadcastReceiver() {\n"
            + "            @Override\n"
            + "            public void onReceive(Context context, Intent intent) {\n"
            + "            }\n"
            + "        };\n"
            + "    }\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mTestReceiver_class = base64gzip("bin/classes/test/pkg/TestReceiver.class", ""
            + "H4sIAAAAAAAAAHVRy0rDUBA901dsrNaqrdYXFhRaFwYfu4qCBaFQXKgUXN4m"
            + "F73aJpKkRf9KV4KCH+BHiZPbgKW1WcyZ15mcmfv98/EF4BBbBhKEYiiD0Hp6"
            + "vLNu2LmStlQD6RtIESrCdXxPOZbtuaF0Q+vc94Rji782QuZEuSo8JSSrtTYh"
            + "1fAcaSKJbA5pZAj5lnLlZb/Xkf6N6HQlYbHl2aLbFr6K4jiZCu9VQFhp/aum"
            + "Tsh6bhwRjqutcWWNCJ/D+kShqaEeaTPsYROhPJXPCymdIaxOm0VIO/1e74Ww"
            + "W61NNE3cqG5ghTf7d7GdAxMllKNbrRHWqlPWj9Sb117ft+WFis5VGC3vP4iB"
            + "IOSariv9RlcEgQxQAT8Joi/JHj8FW4Mji5EY03vvmHljJwGTbUYnDcyyzQ0b"
            + "GOcYCfPIx+QzPY5z48RZTdweFmNi5C2goOv86lhiRgLLKMbDjrQyIPuJ0i2r"
            + "WX0dG5ofUZONhyawru0GNhlNrpX0r/ALQuQclNYCAAA=");

    @SuppressWarnings("all") // Sample code
    private TestFile mTestService = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.app.Service;\n"
            + "import android.content.Intent;\n"
            + "import android.os.IBinder;\n"
            + "\n"
            + "public class TestService extends Service {\n"
            + "\n"
            + "    @Override\n"
            + "    public IBinder onBind(Intent intent) {\n"
            + "        return null;\n"
            + "    }\n"
            + "\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    private TestFile mTestService_class = base64gzip("bin/classes/test/pkg/TestService.class", ""
            + "H4sIAAAAAAAAAHWPsU4CQRCG/4GD0wMFgRBbO7BgEwsbjIUmJiQXGwj9crfR"
            + "VdwldwfPhZWJhQ/gQxlnl9MYo1vMP7P/fJmZ94/XNwBn6IWoEHqFyguxerwT"
            + "M06mKtvoRIUICF1p0szqVMjVSpQGoX6hjS4uCdXBcE4Irm2qIlSx30QNdUIr"
            + "1kbdrp8WKpvJxZKJTmwTuZzLTLu6/AyKe50T+vFf48c8xporbVLCaBB/7ZFY"
            + "UyhTiImX8fDbsLmYuG6VOVJ7m3D8H0iIpnadJepGu1XaPyaPHuRG4gR8Htyr"
            + "cMZncQy5EqzEWjt9wd6ztyOOdf8ZosGxuWtgPWAlHKJVwuesFcfQ9hfZ8GR/"
            + "55aky9o48gM7vup+Aru43eq5AQAA");
}
