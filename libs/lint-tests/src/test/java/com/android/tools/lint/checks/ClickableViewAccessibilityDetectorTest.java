/*
 * Copyright (C) 2014 The Android Open Source Project
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

public class ClickableViewAccessibilityDetectorTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new ClickableViewAccessibilityDetector();
    }

    public void testWarningWhenViewOverridesOnTouchEventButNotPerformClick() throws Exception {
        String expected = ""
                + "src/test/pkg/ClickableViewAccessibilityTest.java:15: Warning: Custom view ViewOverridesOnTouchEventButNotPerformClick overrides onTouchEvent but not performClick [ClickableViewAccessibility]\n"
                + "        public boolean onTouchEvent(MotionEvent event) {\n"
                + "                       ~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.Context;\n"
                        + "import android.view.MotionEvent;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class ClickableViewAccessibilityTest {\n"
                        + "    // Fails because should also implement performClick.\n"
                        + "    private static class ViewOverridesOnTouchEventButNotPerformClick extends View {\n"
                        + "\n"
                        + "        public ViewOverridesOnTouchEventButNotPerformClick(Context context) {\n"
                        + "            super(context);\n"
                        + "        }\n"
                        + "\n"
                        + "        public boolean onTouchEvent(MotionEvent event) {\n"
                        + "            return false;\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "}\n"))
                .run()
                .expect(expected);
    }

    public void testWarningWhenOnTouchEventDoesNotCallPerformClick() throws Exception {
        String expected = ""
                + "src/test/pkg/ClickableViewAccessibilityTest.java:15: Warning: ViewDoesNotCallPerformClick#onTouchEvent should call ViewDoesNotCallPerformClick#performClick when a click is detected [ClickableViewAccessibility]\n"
                + "        public boolean onTouchEvent(MotionEvent event) {\n"
                + "                       ~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        lint().files(
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.Context;\n"
                        + "import android.view.MotionEvent;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class ClickableViewAccessibilityTest {\n"
                        + "    // Fails because should call performClick.\n"
                        + "    private static class ViewDoesNotCallPerformClick extends View {\n"
                        + "\n"
                        + "        public ViewDoesNotCallPerformClick(Context context) {\n"
                        + "            super(context);\n"
                        + "        }\n"
                        + "\n"
                        + "        public boolean onTouchEvent(MotionEvent event) {\n"
                        + "            return false;\n"
                        + "        }\n"
                        + "\n"
                        + "        public boolean performClick() {\n"
                        + "            return super.performClick();\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "}\n"))
                .run()
                .expect(expected);
    }

    public void testWarningWhenPerformClickDoesNotCallSuper() throws Exception {
        String expected = ""
                + "src/test/pkg/ClickableViewAccessibilityTest.java:15: Warning: PerformClickDoesNotCallSuper#performClick should call super#performClick [ClickableViewAccessibility]\n"
                + "        public boolean performClick() {\n"
                + "                       ~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        lint().files(
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.Context;\n"
                        + "import android.view.MotionEvent;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class ClickableViewAccessibilityTest {\n"
                        + "    // Fails because performClick should call super.performClick.\n"
                        + "    private static class PerformClickDoesNotCallSuper extends View {\n"
                        + "\n"
                        + "        public PerformClickDoesNotCallSuper(Context context) {\n"
                        + "            super(context);\n"
                        + "        }\n"
                        + "\n"
                        + "        public boolean performClick() {\n"
                        + "            return false;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expect(expected);
    }

    public void testNoWarningOnValidView() throws Exception {
        //noinspection all // Sample code
        lint().files(
                classpath(),
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.Context;\n"
                        + "import android.view.MotionEvent;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class ClickableViewAccessibilityTest {\n"
                        + "\n"
                        + "    // Valid view.\n"
                        + "    private static class ValidView extends View {\n"
                        + "\n"
                        + "        public ValidView(Context context) {\n"
                        + "            super(context);\n"
                        + "        }\n"
                        + "\n"
                        + "        public boolean onTouchEvent(MotionEvent event) {\n"
                        + "            performClick();\n"
                        + "            return false;\n"
                        + "        }\n"
                        + "\n"
                        + "        public boolean performClick() {\n"
                        + "            return super.performClick();\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expectClean();
    }

    public void testNoWarningOnNonViewSubclass() throws Exception {
        //noinspection all // Sample code
        lint().files(
                classpath(),
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.Context;\n"
                        + "import android.view.MotionEvent;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class ClickableViewAccessibilityTest {\n"
                        + "    // Okay because it's not actually a view subclass.\n"
                        + "    private static class NotAView {\n"
                        + "\n"
                        + "        public boolean onTouchEvent(MotionEvent event) {\n"
                        + "            return false;\n"
                        + "        }\n"
                        + "\n"
                        + "        public void setOnTouchListener(View.OnTouchListener onTouchListener) { }\n"
                        + "    }\n"
                        + "}"))
                .run()
                .expectClean();
    }

    public void testWarningOnViewSubclass() throws Exception {
        // ViewSubclass is actually a subclass of ValidView. This tests that we can detect
        // tests further down in the inheritance hierarchy than direct children of View.
        //noinspection all // Sample code
        String expected = ""
                + "src/test/pkg/ClickableViewAccessibilityTest.java:34: Warning: ViewSubclass#performClick should call super#performClick [ClickableViewAccessibility]\n"
                + "        public boolean performClick() {\n"
                + "                       ~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        lint().files(
                classpath(),
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.Context;\n"
                        + "import android.view.MotionEvent;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class ClickableViewAccessibilityTest {\n"
                        + "\n"
                        + "    // Valid view.\n"
                        + "    private static class ValidView extends View {\n"
                        + "\n"
                        + "        public ValidView(Context context) {\n"
                        + "            super(context);\n"
                        + "        }\n"
                        + "\n"
                        + "        public boolean onTouchEvent(MotionEvent event) {\n"
                        + "            performClick();\n"
                        + "            return false;\n"
                        + "        }\n"
                        + "\n"
                        + "        public boolean performClick() {\n"
                        + "            return super.performClick();\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    // Should fail because it's a view subclass. This tests that we can detect Views that are\n"
                        + "    // not just direct sub-children.\n"
                        + "    private static class ViewSubclass extends ValidView {\n"
                        + "\n"
                        + "        public ViewSubclass(Context context) {\n"
                        + "            super(context);\n"
                        + "        }\n"
                        + "\n"
                        + "        public boolean performClick() {\n"
                        + "            return false;\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "}\n"))
                .run()
                .expect(expected);
    }

    public void testNoWarningOnOnTouchEventWithDifferentSignature() throws Exception {
        //noinspection all // Sample code
        lint().files(
                classpath(),
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.Context;\n"
                        + "import android.view.MotionEvent;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class ClickableViewAccessibilityTest {\n"
                        + "    // Okay because it's declaring onTouchEvent with a different signature.\n"
                        + "    private static class ViewWithDifferentOnTouchEvent extends View {\n"
                        + "\n"
                        + "        public ViewWithDifferentOnTouchEvent(Context context) {\n"
                        + "            super(context);\n"
                        + "        }\n"
                        + "\n"
                        + "        public boolean onTouchEvent() {\n"
                        + "            return false;\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "}\n"))
                .run()
                .expectClean();
    }

    public void testNoWarningOnPerformClickWithDifferentSignature() throws Exception {
        //noinspection all // Sample code
        lint().files(
                classpath(),
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.Context;\n"
                        + "import android.view.MotionEvent;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class ClickableViewAccessibilityTest {\n"
                        + "    // Okay because it's declaring performClick with a different signature.\n"
                        + "    private static class ViewWithDifferentPerformClick extends View {\n"
                        + "\n"
                        + "        public ViewWithDifferentPerformClick(Context context) {\n"
                        + "            super(context);\n"
                        + "        }\n"
                        + "\n"
                        + "        public boolean performClick(Context context) {\n"
                        + "            return false;\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "}\n"))
                .run()
                .expectClean();
    }

    public void testWarningWhenSetOnTouchListenerCalledOnViewWithNoPerformClick() throws Exception {
        String expected = ""
                + "src/test/pkg/ClickableViewAccessibilityTest.java:19: Warning: Custom view `NoPerformClick` has setOnTouchListener called on it but does not override performClick [ClickableViewAccessibility]\n"
                + "            view.setOnTouchListener(new ValidOnTouchListener());\n"
                + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        lint().files(
                classpath(),
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.Context;\n"
                        + "import android.view.MotionEvent;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class ClickableViewAccessibilityTest {\n"
                        + "    // Okay when NoPerformClickOnTouchListenerSetter in project.\n"
                        + "    // When NoPerformClickOnTouchListenerSetter is in the project, fails because no perform click\n"
                        + "    // and setOnTouchListener is called below.\n"
                        + "    private static class NoPerformClick extends View {\n"
                        + "        public NoPerformClick(Context context) {\n"
                        + "            super(context);\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class NoPerformClickOnTouchListenerSetter {\n"
                        + "        private void callSetOnTouchListenerOnNoPerformClick(NoPerformClick view) {\n"
                        + "            view.setOnTouchListener(new ValidOnTouchListener());\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    // Okay because onTouch calls view.performClick().\n"
                        + "    private static class ValidOnTouchListener implements View.OnTouchListener {\n"
                        + "        public boolean onTouch(View v, MotionEvent event) {\n"
                        + "            return v.performClick();\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"

                        + "}\n"))
                .run()
                .expect(expected);
    }

    public void testNoWarningWhenSetOnTouchListenerNotCalledOnViewWithNoPerformClick() throws Exception {
        //noinspection all // Sample code
        lint().files(
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.Context;\n"
                        + "import android.view.MotionEvent;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class ClickableViewAccessibilityTest {\n"
                        + "    // Okay when NoPerformClickOnTouchListenerSetter in project.\n"
                        + "    // When NoPerformClickOnTouchListenerSetter is in the project, fails because no perform click\n"
                        + "    // and setOnTouchListener is called below.\n"
                        + "    private static class NoPerformClick extends View {\n"
                        + "        public NoPerformClick(Context context) {\n"
                        + "            super(context);\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expectClean();

    }

    public void testNoWarningWhenSetOnTouchListenerCalledOnViewWithPerformClick() throws Exception {
        //noinspection all // Sample code
        lint().files(
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.Context;\n"
                        + "import android.view.MotionEvent;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class ClickableViewAccessibilityTest {\n"
                        + "    // Succeeds because has performClick call.\n"
                        + "    private static class HasPerformClick extends View {\n"
                        + "       public HasPerformClick(Context context) {\n"
                        + "            super(context);\n"
                        + "        }\n"
                        + "\n"
                        + "        public boolean performClick() {\n"
                        + "            return super.performClick();\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class HasPerformClickOnTouchListenerSetter {\n"
                        + "        private void callSetOnTouchListenerOnHasPerformClick(HasPerformClick view) {\n"
                        + "            view.setOnTouchListener(new ValidOnTouchListener());\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "}\n"))
                .run()
                .expectClean();
    }

    public void testNoWarningWhenOnTouchListenerCalledOnNonViewSubclass() throws Exception {
        //noinspection all // Sample code
        lint().files(
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.Context;\n"
                        + "import android.view.MotionEvent;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class ClickableViewAccessibilityTest {\n"
                        + "    private static class NotAView {\n"
                        + "\n"
                        + "        public boolean onTouchEvent(MotionEvent event) {\n"
                        + "            return false;\n"
                        + "        }\n"
                        + "\n"
                        + "        public void setOnTouchListener(View.OnTouchListener onTouchListener) { }\n"
                        + "    }\n"
                        + "\n"
                        + "    // Okay because even though NotAView doesn't have a performClick call, it isn't\n"
                        + "    // a View subclass.\n"
                        + "    private static class NotAViewOnTouchListenerSetter {\n"
                        + "        private void callSetOnTouchListenerOnNotAView(NotAView notAView) {\n"
                        + "            notAView.setOnTouchListener(new ValidOnTouchListener());\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "}\n"))
                .run()
                .expectClean();

    }

    public void testNoWarningWhenOnTouchCallsPerformClick() throws Exception {
        //noinspection all // Sample code
        lint().files(
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.Context;\n"
                        + "import android.view.MotionEvent;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class ClickableViewAccessibilityTest {\n"
                        + "    // Okay because onTouch calls view.performClick().\n"
                        + "    private static class ValidOnTouchListener implements View.OnTouchListener {\n"
                        + "        public boolean onTouch(View v, MotionEvent event) {\n"
                        + "            return v.performClick();\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "}\n"))
                .run()
                .expectClean();

    }

    public void testWarningWhenOnTouchDoesNotCallPerformClick() throws Exception {
        String expected = ""
                + "src/test/pkg/ClickableViewAccessibilityTest.java:10: Warning: InvalidOnTouchListener#onTouch should call View#performClick when a click is detected [ClickableViewAccessibility]\n"
                + "        public boolean onTouch(View v, MotionEvent event) {\n"
                + "                       ~~~~~~~\n"
                + "0 errors, 1 warnings\n";
        //noinspection all // Sample code
        lint().files(
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.Context;\n"
                        + "import android.view.MotionEvent;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class ClickableViewAccessibilityTest {\n"
                        + "    // Fails because onTouch does not call view.performClick().\n"
                        + "    private static class InvalidOnTouchListener implements View.OnTouchListener {\n"
                        + "        public boolean onTouch(View v, MotionEvent event) {\n"
                        + "            return false;\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "}\n"))
                .run()
                .expect(expected);

    }

    public void testNoWarningWhenAnonymousOnTouchListenerCallsPerformClick() throws Exception {
        //noinspection all // Sample code
        lint().files(
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.Context;\n"
                        + "import android.view.MotionEvent;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class ClickableViewAccessibilityTest {\n"
                        + "    // Okay because anonymous OnTouchListener calls view.performClick().\n"
                        + "    private static class AnonymousValidOnTouchListener {\n"
                        + "        private void callSetOnTouchListener(HasPerformClick view) {\n"
                        + "            view.setOnTouchListener(new View.OnTouchListener() {\n"
                        + "                public boolean onTouch(View v, MotionEvent event) {\n"
                        + "                    return v.performClick();\n"
                        + "                }\n"
                        + "            });\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "}\n"))
                .run()
                .expectClean();
    }

    public void testWarningWhenAnonymousOnTouchListenerDoesNotCallPerformClick() throws Exception {
        String expected = ""
                + "src/test/pkg/ClickableViewAccessibilityTest.java:12: Warning: onTouch should call View#performClick when a click is detected [ClickableViewAccessibility]\n"
                + "                public boolean onTouch(View v, MotionEvent event) {\n"
                + "                               ~~~~~~~\n"
                + "0 errors, 1 warnings\n";

        //noinspection all // Sample code
        lint().files(
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.content.Context;\n"
                        + "import android.view.MotionEvent;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class ClickableViewAccessibilityTest {\n"
                        + "    // Fails because anonymous OnTouchListener does not call view.performClick().\n"
                        + "    private static class AnonymousInvalidOnTouchListener {\n"
                        + "        private void callSetOnTouchListener(HasPerformClick view) {\n"
                        + "            view.setOnTouchListener(new View.OnTouchListener() {\n"
                        + "                public boolean onTouch(View v, MotionEvent event) {\n"
                        + "                    return false;\n"
                        + "                }\n"
                        + "            });\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expect(expected);
    }

    public void testLambdas() {
        //noinspection all // Sample code
        lint().files(
                java("" 
                        + "package com.example.tnorbye.myapplication;\n"
                        + "\n"
                        + "import android.view.MotionEvent;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class ClickTest {\n"
                        + "    private View.OnTouchListener okListener = (v, event) -> v.performClick();\n"
                        + "    private View.OnTouchListener okListener2 = (v, event) -> v.performContextClick();\n"
                        + "    private View.OnTouchListener wrongListener = (v, event) -> false;\n"
                        + "\n"
                        + "    public void testWithInnerClass1(View view) {\n"
                        + "        view.setOnTouchListener(new View.OnTouchListener() {\n"
                        + "            @Override\n"
                        + "            public boolean onTouch(View v, MotionEvent event) {\n"
                        + "                return false;\n"
                        + "            }\n"
                        + "        });\n"
                        + "    }\n"
                        + "\n"
                        + "    public void testWithInnerClass2(View view) {\n"
                        + "        view.setOnTouchListener(new View.OnTouchListener() {\n"
                        + "            @Override\n"
                        + "            public boolean onTouch(View v, MotionEvent event) {\n"
                        + "                return v.performClick();\n"
                        + "            }\n"
                        + "        });\n"
                        + "    }\n"
                        + "\n"
                        + "    public void testWithLambda(View view) {\n"
                        + "        view.setOnTouchListener((v, event) -> false);\n"
                        + "    }\n"
                        + "\n"
                        + "    public void testWithLambda2(View view) {\n"
                        + "        view.setOnTouchListener((v, event) -> v.performClick());\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expect(""
                        + "src/com/example/tnorbye/myapplication/ClickTest.java:8: Warning: onTouch lambda should call View#performClick when a click is detected [ClickableViewAccessibility]\n"
                        + "    private View.OnTouchListener okListener2 = (v, event) -> v.performContextClick();\n"
                        + "                                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "src/com/example/tnorbye/myapplication/ClickTest.java:9: Warning: onTouch lambda should call View#performClick when a click is detected [ClickableViewAccessibility]\n"
                        + "    private View.OnTouchListener wrongListener = (v, event) -> false;\n"
                        + "                                                 ~~~~~~~~~~~~~~~~~~~\n"
                        + "src/com/example/tnorbye/myapplication/ClickTest.java:14: Warning: onTouch should call View#performClick when a click is detected [ClickableViewAccessibility]\n"
                        + "            public boolean onTouch(View v, MotionEvent event) {\n"
                        + "                           ~~~~~~~\n"
                        + "src/com/example/tnorbye/myapplication/ClickTest.java:30: Warning: onTouch lambda should call View#performClick when a click is detected [ClickableViewAccessibility]\n"
                        + "        view.setOnTouchListener((v, event) -> false);\n"
                        + "                                ~~~~~~~~~~~~~~~~~~~\n"
                        + "0 errors, 4 warnings\n");
    }

    public void testGenericFindViewById() throws Exception {
        //noinspection all // Sample code
        lint().files(
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class OView {\n"
                        + "    public final <T extends View> T findViewById(int id) { return null; }\n"
                        + "}\n"),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class MyView extends OView {\n"
                        + "    public void test(int id) {\n"
                        + "        View view = findViewById(id);\n"
                        + "        assert view != null;\n"
                        + "        view.setOnTouchListener((view1, motionEvent) -> view1.performClick());\n"
                        + "    }\n"
                        + "}\n"))
                .run()
                .expectClean();
    }
}
