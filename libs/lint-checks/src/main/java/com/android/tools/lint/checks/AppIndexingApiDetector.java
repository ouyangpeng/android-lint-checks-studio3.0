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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.CLASS_ACTIVITY;
import static com.android.utils.XmlUtils.getFirstSubTagByName;
import static com.android.utils.XmlUtils.getSubTagsByName;
import static com.android.xml.AndroidManifest.ATTRIBUTE_NAME;
import static com.android.xml.AndroidManifest.NODE_ACTIVITY;
import static com.android.xml.AndroidManifest.NODE_APPLICATION;
import static com.android.xml.AndroidManifest.NODE_DATA;
import static com.android.xml.AndroidManifest.NODE_INTENT;
import static com.android.xml.AndroidManifest.NODE_MANIFEST;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.UastScanner;
import com.android.tools.lint.detector.api.Detector.XmlScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;
import com.android.utils.XmlUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.util.UastExpressionUtils;
import org.jetbrains.uast.visitor.AbstractUastVisitor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Check if the usage of App Indexing is correct.
 */
public class AppIndexingApiDetector extends Detector implements XmlScanner, UastScanner {

    private static final Implementation URL_IMPLEMENTATION = new Implementation(
            AppIndexingApiDetector.class, Scope.MANIFEST_SCOPE);

    @SuppressWarnings("unchecked")
    private static final Implementation APP_INDEXING_API_IMPLEMENTATION =
            new Implementation(
                    AppIndexingApiDetector.class,
                    EnumSet.of(Scope.JAVA_FILE, Scope.MANIFEST),
                    Scope.JAVA_FILE_SCOPE, Scope.MANIFEST_SCOPE);

    public static final Issue ISSUE_APP_INDEXING =
            Issue.create(
                    "GoogleAppIndexingWarning",
                    "Missing support for Firebase App Indexing",
                    "Adds URLs to get your app into the Google index, to get installs"
                            + " and traffic to your app from Google Search.",
                    Category.USABILITY, 5, Severity.WARNING, URL_IMPLEMENTATION)
                    .addMoreInfo("https://g.co/AppIndexing/AndroidStudio");

    public static final Issue ISSUE_APP_INDEXING_API =
            Issue.create(
                    "GoogleAppIndexingApiWarning",
                    "Missing support for Firebase App Indexing Api",
                    "Adds URLs to get your app into the Google index, to get installs"
                            + " and traffic to your app from Google Search.",
                    Category.USABILITY, 5, Severity.WARNING, APP_INDEXING_API_IMPLEMENTATION)
                    .addMoreInfo("https://g.co/AppIndexing/AndroidStudio")
                    .setEnabledByDefault(false);

    private static final String APP_INDEX_START = "start";
    private static final String APP_INDEX_END = "end";
    private static final String APP_INDEX_VIEW = "view";
    private static final String APP_INDEX_VIEW_END = "viewEnd";
    private static final String CLIENT_CONNECT = "connect";
    private static final String CLIENT_DISCONNECT = "disconnect";
    private static final String ADD_API = "addApi";

    private static final String APP_INDEXING_API_CLASS
            = "com.google.android.gms.appindexing.AppIndexApi";
    private static final String GOOGLE_API_CLIENT_CLASS
            = "com.google.android.gms.common.api.GoogleApiClient";
    private static final String GOOGLE_API_CLIENT_BUILDER_CLASS
            = "com.google.android.gms.common.api.GoogleApiClient.Builder";
    private static final String API_CLASS = "com.google.android.gms.appindexing.AppIndex";

    // ---- Implements XmlScanner ----
    @Override
    @Nullable
    public Collection<String> getApplicableElements() {
        return Collections.singletonList(NODE_APPLICATION);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element application) {
        boolean applicationHasActionView = false;
        for (Element activity : XmlUtils.getSubTagsByName(application, NODE_ACTIVITY)) {
            for (Element intent : XmlUtils.getSubTagsByName(activity, NODE_INTENT)) {
                boolean actionView = AppLinksValidDetector.hasActionView(intent);
                if (actionView) {
                    applicationHasActionView = true;
                }
            }
        }
        if (!applicationHasActionView && !context.getProject().isLibrary()) {
            // Report warning if there is no activity that supports action view.
            context.report(ISSUE_APP_INDEXING, application, context.getLocation(application),
                           // This error message is more verbose than the other app indexing lint warnings, because it
                           // shows up on a blank project, and we want to make it obvious by just looking at the error
                           // message what this is
                           "App is not indexable by Google Search; consider adding at least one Activity with an ACTION-VIEW " +
                           "intent filter. See issue explanation for more details.");
        }
    }

    @Nullable
    @Override
    public List<String> applicableSuperClasses() {
        return Collections.singletonList(CLASS_ACTIVITY);
    }

    @Override
    public void visitClass(@NonNull JavaContext context, @NonNull UClass declaration) {
        if (declaration.getName() == null) {
            return;
        }

        // In case linting the base class itself.
        if (!context.getEvaluator().extendsClass(declaration, CLASS_ACTIVITY, true)) {
            return;
        }

        declaration.accept(new MethodVisitor(context, declaration));
    }

    static class MethodVisitor extends AbstractUastVisitor {
        private final JavaContext mContext;
        private final UClass mCls;

        private final List<UCallExpression> mStartMethods;
        private final List<UCallExpression> mEndMethods;
        private final List<UCallExpression> mConnectMethods;
        private final List<UCallExpression> mDisconnectMethods;
        private boolean mHasAddAppIndexApi;

        MethodVisitor(JavaContext context, UClass cls) {
            mCls = cls;
            mContext = context;
            mStartMethods = Lists.newArrayListWithExpectedSize(2);
            mEndMethods = Lists.newArrayListWithExpectedSize(2);
            mConnectMethods = Lists.newArrayListWithExpectedSize(2);
            mDisconnectMethods = Lists.newArrayListWithExpectedSize(2);
        }

        @Override
        public boolean visitClass(UClass aClass) {
            //noinspection SimplifiableIfStatement
            if (aClass.getPsi().equals(mCls.getPsi())) {
                return super.visitClass(aClass);
            } else {
                // Don't go into inner classes
                return true;
            }
        }

        @Override
        public void afterVisitClass(UClass node) {
            report();
        }

        @Override
        public boolean visitCallExpression(UCallExpression node) {
            if (UastExpressionUtils.isMethodCall(node)) {
                visitMethodCallExpression(node);
            }
            return super.visitCallExpression(node);
        }

        private void visitMethodCallExpression(UCallExpression node) {
            String methodName = node.getMethodName();
            if (methodName == null) {
                return;
            }

            JavaEvaluator evaluator = mContext.getEvaluator();
            switch (methodName) {
                case APP_INDEX_START:
                    if (evaluator.isMemberInClass(node.resolve(), APP_INDEXING_API_CLASS)) {
                        mStartMethods.add(node);
                    }
                    break;
                case APP_INDEX_END:
                    if (evaluator.isMemberInClass(node.resolve(), APP_INDEXING_API_CLASS)) {
                        mEndMethods.add(node);
                    }
                    break;
                case APP_INDEX_VIEW:
                    if (evaluator.isMemberInClass(node.resolve(), APP_INDEXING_API_CLASS)) {
                        mStartMethods.add(node);
                    }
                    break;
                case APP_INDEX_VIEW_END:
                    if (evaluator.isMemberInClass(node.resolve(), APP_INDEXING_API_CLASS)) {
                        mEndMethods.add(node);
                    }
                    break;
                case CLIENT_CONNECT:
                    if (evaluator.isMemberInClass(node.resolve(), GOOGLE_API_CLIENT_CLASS)) {
                        mConnectMethods.add(node);
                    }
                    break;
                case CLIENT_DISCONNECT:
                    if (evaluator.isMemberInClass(node.resolve(), GOOGLE_API_CLIENT_CLASS)) {
                        mDisconnectMethods.add(node);
                    }
                    break;
                case ADD_API:
                    if (evaluator.isMemberInClass(node.resolve(),
                            GOOGLE_API_CLIENT_BUILDER_CLASS)) {
                        if (evaluator
                                .isMemberInClass(node.resolve(), GOOGLE_API_CLIENT_BUILDER_CLASS)) {
                            List<UExpression> args = node.getValueArguments();
                            if (!args.isEmpty()) {
                                PsiElement resolved = UastUtils.tryResolve(args.get(0));
                                if (resolved instanceof PsiField &&
                                        evaluator.isMemberInClass((PsiField) resolved, API_CLASS)) {
                                    mHasAddAppIndexApi = true;
                                }
                            }
                            break;
                        }
                    }
            }
        }

        private void report() {
            // finds the activity classes that need app activity annotation
            Set<String> activitiesToCheck = getActivitiesToCheck(mContext);

            // app indexing API used but no support in manifest
            boolean hasIntent = activitiesToCheck.contains(mCls.getQualifiedName());
            if (!hasIntent) {
                for (UCallExpression call : mStartMethods) {
                    mContext.report(ISSUE_APP_INDEXING_API, call,
                            mContext.getNameLocation(call),
                            "Missing support for Firebase App Indexing in the manifest");
                }
                for (UCallExpression call : mEndMethods) {
                    mContext.report(ISSUE_APP_INDEXING_API, call,
                            mContext.getNameLocation(call),
                            "Missing support for Firebase App Indexing in the manifest");
                }
                return;
            }

            // `AppIndex.AppIndexApi.start / end / view / viewEnd` should exist
            if (mStartMethods.isEmpty() && mEndMethods.isEmpty()) {
                mContext.report(ISSUE_APP_INDEXING_API, mCls,
                        mContext.getNameLocation(mCls),
                        "Missing support for Firebase App Indexing API");
                return;
            }

            for (UCallExpression startNode : mStartMethods) {
                List<UExpression> expressions = startNode.getValueArguments();
                if (expressions.isEmpty()) {
                    continue;
                }
                UExpression startClient = expressions.get(0);

                // GoogleApiClient should `addApi(AppIndex.APP_INDEX_API)`
                if (!mHasAddAppIndexApi) {
                    String message = String.format(
                            "GoogleApiClient `%1$s` has not added support for App Indexing API",
                            startClient.asSourceString());
                    mContext.report(ISSUE_APP_INDEXING_API, startClient,
                            mContext.getLocation(startClient), message);
                }

                // GoogleApiClient `connect` should exist
                if (!hasOperand(startClient, mConnectMethods)) {
                    String message = String.format("GoogleApiClient `%1$s` is not connected",
                                    startClient.asSourceString());
                    mContext.report(ISSUE_APP_INDEXING_API, startClient,
                            mContext.getLocation(startClient), message);
                }

                // `AppIndex.AppIndexApi.end` should pair with `AppIndex.AppIndexApi.start`
                if (!hasFirstArgument(startClient, mEndMethods)) {
                    mContext.report(ISSUE_APP_INDEXING_API, startNode,
                            mContext.getNameLocation(startNode),
                            "Missing corresponding `AppIndex.AppIndexApi.end` method");
                }
            }

            for (UCallExpression endNode : mEndMethods) {
                List<UExpression> expressions = endNode.getValueArguments();
                if (expressions.isEmpty()) {
                    continue;
                }
                UExpression endClient = expressions.get(0);

                // GoogleApiClient should `addApi(AppIndex.APP_INDEX_API)`
                if (!mHasAddAppIndexApi) {
                    String message = String.format(
                            "GoogleApiClient `%1$s` has not added support for App Indexing API",
                            endClient.asSourceString());
                    mContext.report(ISSUE_APP_INDEXING_API, endClient,
                            mContext.getLocation(endClient), message);
                }

                // GoogleApiClient `disconnect` should exist
                if (!hasOperand(endClient, mDisconnectMethods)) {
                    String message = String.format("GoogleApiClient `%1$s`"
                            + " is not disconnected", endClient.asSourceString());
                    mContext.report(ISSUE_APP_INDEXING_API, endClient,
                            mContext.getLocation(endClient), message);
                }

                // `AppIndex.AppIndexApi.start` should pair with `AppIndex.AppIndexApi.end`
                if (!hasFirstArgument(endClient, mStartMethods)) {
                    mContext.report(ISSUE_APP_INDEXING_API, endNode,
                            mContext.getNameLocation(endNode),
                            "Missing corresponding `AppIndex.AppIndexApi.start` method");
                }
            }
        }
    }

    /**
     * Gets names of activities which needs app indexing. i.e. the activities have data tag in
     * their intent filters.
     *
     * @param context The context to check in.
     */
    @NonNull
    private static Set<String> getActivitiesToCheck(Context context) {
        Set<String> activitiesToCheck = Sets.newHashSet();
        Document doc = context.getMainProject().getMergedManifest();
        if (doc == null) {
            return Collections.emptySet();
        }
        for (Element child : XmlUtils.getSubTags(doc)) {
            if (child.getNodeName().equals(NODE_MANIFEST)) {
                for (Element app : getSubTagsByName(child, NODE_APPLICATION)) {
                    for (Element act : getSubTagsByName(app, NODE_ACTIVITY)) {
                        for (Element intent : getSubTagsByName(act, NODE_INTENT)) {
                            boolean hasData = getFirstSubTagByName(intent, NODE_DATA) != null;
                            if (hasData && act.hasAttributeNS(ANDROID_URI, ATTRIBUTE_NAME)) {
                                String activityName = LintUtils.resolveManifestName(act);
                                activitiesToCheck.add(activityName);
                            }
                        }
                    }
                }
            }
        }
        return activitiesToCheck;
    }

    /**
     * If a method with a certain argument exists in the list of methods.
     *
     * @param argument The first argument of the method.
     * @param list     The methods list.
     * @return If such a method exists in the list.
     */
    private static boolean hasFirstArgument(UExpression argument, List<UCallExpression> list) {
        for (UCallExpression call : list) {
            List<UExpression> expressions = call.getValueArguments();
            if (!expressions.isEmpty()) {
                UExpression argument2 = expressions.get(0);
                if (argument.asSourceString().equals(argument2.asSourceString())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * If a method with a certain operand exists in the list of methods.
     *
     * @param operand The operand of the method.
     * @param list    The methods list.
     * @return If such a method exists in the list.
     */
    private static boolean hasOperand(UExpression operand, List<UCallExpression> list) {
        for (UCallExpression method : list) {
            UElement operand2 = method.getReceiver();
            if (operand2 != null && operand.asSourceString().equals(operand2.asSourceString())) {
                return true;
            }
        }
        return false;
    }
}
