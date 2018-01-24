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

package com.android.tools.lint.checks;

import com.android.tools.lint.detector.api.Detector;

public class JobSchedulerDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new JobSchedulerDetector();
    }

    public void testOk() throws Exception {
        lint().files(
                VALID_SERVICE_REFERENCE,
                MY_JOB_SERVICE,
                NOT_A_JOB_SERVICE,
                VALID_MANIFEST)
                .run()
                .expectClean();
    }

    public void testFlagWrongClass() throws Exception {
        String expected = ""
                + "src/test/pkg/JobSchedulerTest.java:21: Warning: Scheduled job class NotAJobService must extend android.app.job.JobService [JobSchedulerService]\n"
                + "                new ComponentName(this, NotAJobService.class));\n"
                + "                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/JobSchedulerTest.java:29: Warning: Scheduled job class NotAJobService must extend android.app.job.JobService [JobSchedulerService]\n"
                + "                new ComponentName(this, NotAJobService.class));\n"
                + "                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n";

        //noinspection all // Sample code
        lint().files(
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.app.job.JobInfo;\n"
                        + "import android.app.job.JobScheduler;\n"
                        + "import android.content.ComponentName;\n"
                        + "\n"
                        + "public class JobSchedulerTest extends Activity {\n"
                        + "    private static final int MY_ID = 0x52323;\n"
                        + "\n"
                        + "    public void testOk(JobScheduler jobScheduler) {\n"
                        + "        ComponentName componentName = new ComponentName(this, MyJobService.class);\n"
                        + "        JobInfo.Builder builder = new JobInfo.Builder(MY_ID, componentName);\n"
                        + "        jobScheduler.schedule(builder\n"
                        + "                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)\n"
                        + "                .build());\n"
                        + "    }\n"
                        + "\n"
                        + "    public void testWrong(JobScheduler jobScheduler) {\n"
                        + "        JobInfo.Builder builder = new JobInfo.Builder(MY_ID,\n"
                        + "                new ComponentName(this, NotAJobService.class));\n"
                        + "        jobScheduler.schedule(builder\n"
                        + "                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)\n"
                        + "                .build());\n"
                        + "    }\n"
                        + "\n"
                        + "    public void testWrongInlined(JobScheduler jobScheduler) {\n"
                        + "        JobInfo.Builder builder = new JobInfo.Builder(MY_ID, \n"
                        + "                new ComponentName(this, NotAJobService.class));\n"
                        + "        jobScheduler.schedule(builder\n"
                        + "                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)\n"
                        + "                .build());\n"
                        + "    }\n"
                        + "}\n"),
                MY_JOB_SERVICE,
                NOT_A_JOB_SERVICE,
                VALID_MANIFEST)
                .run()
                .expect(expected);
    }

    public void testMissingManifestRegistration() throws Exception {
        String expected = ""
                + "src/test/pkg/JobSchedulerTest.java:10: Warning: Did not find a manifest registration for this service [JobSchedulerService]\n"
                + "        ComponentName componentName = new ComponentName(this, MyJobService.class);\n"
                + "                                                              ~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                VALID_SERVICE_REFERENCE,
                MY_JOB_SERVICE,
                manifest(""
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg\">\n"
                        + "\n"
                        + "    <uses-sdk android:minSdkVersion=\"14\" />\n"
                        + "\n"
                        + "    <application />\n"
                        + "\n"
                        + "</manifest>"))
                .run()
                .expect(expected);
    }

    public void testMissingPermission() throws Exception {
        String expected = ""
                + "src/test/pkg/JobSchedulerTest.java:10: Warning: The manifest registration for this service does not declare android:permission=\"android.permission.BIND_JOB_SERVICE\" [JobSchedulerService]\n"
                + "        ComponentName componentName = new ComponentName(this, MyJobService.class);\n"
                + "                                                              ~~~~~~~~~~~~~~~~~~\n"
                + "    AndroidManifest.xml:5: Service declaration here\n"
                + "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        lint().files(
                VALID_SERVICE_REFERENCE,
                MY_JOB_SERVICE,
                manifest(""
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg\">\n"
                        + "\n"
                        + "    <application>\n"
                        + "        <service android:name=\".MyJobService\"\n"
                        + "                 android:exported=\"true\" />\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>"))
                .run()
                .expect(expected);
    }

    @SuppressWarnings("all") // Sample code
    public static final TestFile MY_JOB_SERVICE = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.annotation.TargetApi;\n"
            + "import android.app.job.JobParameters;\n"
            + "import android.app.job.JobService;\n"
            + "import android.os.Build;\n"
            + "\n"
            + "@TargetApi(Build.VERSION_CODES.LOLLIPOP)\n"
            + "public class MyJobService extends JobService {\n"
            + "    @Override\n"
            + "    public boolean onStartJob(JobParameters jobParameters) {\n"
            + "        return false;\n"
            + "    }\n"
            + "\n"
            + "    @Override\n"
            + "    public boolean onStopJob(JobParameters jobParameters) {\n"
            + "        return false;\n"
            + "    }\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    public static final TestFile NOT_A_JOB_SERVICE = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.app.Service;\n"
            + "\n"
            + "public abstract class NotAJobService extends Service {\n"
            + "}\n");

    @SuppressWarnings("all") // Sample code
    public static final TestFile VALID_SERVICE_REFERENCE = java(""
            + "package test.pkg;\n"
            + "\n"
            + "import android.app.Activity;\n"
            + "import android.app.job.JobInfo;\n"
            + "import android.app.job.JobScheduler;\n"
            + "import android.content.ComponentName;\n"
            + "\n"
            + "public class JobSchedulerTest extends Activity {\n"
            + "    public void test(JobScheduler jobScheduler) {\n"
            + "        ComponentName componentName = new ComponentName(this, MyJobService.class);\n"
            + "        JobInfo.Builder builder = new JobInfo.Builder(1234, componentName);\n"
            + "    }\n"
            + "}\n");

    public static final TestFile VALID_MANIFEST = manifest(""
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    package=\"test.pkg\">\n"
            + "\n"
            + "    <application>\n"
            + "        <service android:name=\".MyJobService\"\n"
            + "                 android:permission=\"android.permission.BIND_JOB_SERVICE\"\n"
            + "                 android:exported=\"true\" />\n"
            + "        <service android:name=\".NotAJobService\"\n"
            + "                 android:exported=\"true\" />\n"
            + "    </application>\n"
            + "\n"
            + "</manifest>");
}
