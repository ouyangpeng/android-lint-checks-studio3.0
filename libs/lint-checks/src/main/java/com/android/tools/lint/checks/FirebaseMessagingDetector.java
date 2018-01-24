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

package com.android.tools.lint.checks;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import java.util.Collections;
import java.util.List;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClass;

public class FirebaseMessagingDetector extends Detector implements Detector.UastScanner {

    private static final String FIREBASE_IID_PACKAGE = "com.google.firebase.iid";
    private static final String FIREBASE_IID_CLASS_NAME = FIREBASE_IID_PACKAGE
            + ".FirebaseInstanceId";
    private static final String FIREBASE_IID_SERVICE_CLASS_NAME = FIREBASE_IID_PACKAGE
            + ".FirebaseInstanceIdService";
    private static final String ON_TOKEN_REFRESH_METHOD_NAME = "onTokenRefresh";
    private static final String GET_TOKEN_METHOD_NAME = "getToken";
    private static final Implementation IMPLEMENTATION = new Implementation(
            FirebaseMessagingDetector.class,
            Scope.JAVA_FILE_SCOPE,
            Scope.ALL);

    public static final Issue MISSING_TOKEN_REFRESH = Issue.create(
            "MissingFirebaseInstanceTokenRefresh",
            "Missing Firebase Instance ID Token Refresh",
            "Apps that check the Firebase Instance ID should usually implement the " +
                    "FirebaseInstanceIdService#onTokenRefresh() callback in order to observe " +
                    "changes.",
            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            IMPLEMENTATION)
            .addMoreInfo(
                    "https://firebase.google.com/docs/cloud-messaging/android/client#monitor-token-generation");

    private boolean mIsOnTokenRefreshDefined;
    private UCallExpression mGetTokenCallSite;
    private JavaContext mGetTokenContext;

    /**
     * Constructs a new {@link FirebaseMessagingDetector}
     */
    public FirebaseMessagingDetector() {
    }

    @Override
    public void beforeCheckProject(@NonNull Context context) {
        mIsOnTokenRefreshDefined = false;
        mGetTokenCallSite = null;
        mGetTokenContext = null;
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @NonNull UCallExpression call,
            @NonNull PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass != null &&
                FIREBASE_IID_CLASS_NAME.equals(containingClass.getQualifiedName())) {
            // Save references to the call site and context since createLocationHandle uses the
            // older Lombok API and is deprecated.
            mGetTokenCallSite = call;
            mGetTokenContext = context;
        }

    }

    @Override
    public void visitClass(@NonNull JavaContext context, @NonNull UClass declaration) {
        if (!FIREBASE_IID_SERVICE_CLASS_NAME.equals(declaration.getQualifiedName())) {
            for (PsiMethod method : declaration.getMethods()) {
                if (method.getName().equals(ON_TOKEN_REFRESH_METHOD_NAME)) {
                    mIsOnTokenRefreshDefined = true;
                }
            }
        }
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (mGetTokenCallSite != null && !mIsOnTokenRefreshDefined) {
            context.report(
                    MISSING_TOKEN_REFRESH,
                    mGetTokenContext.getLocation(mGetTokenCallSite),
                    "getToken() called without defining onTokenRefresh callback.");
        }
    }

    @Nullable
    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList(GET_TOKEN_METHOD_NAME);
    }

    @Nullable
    @Override
    public List<String> applicableSuperClasses() {
        return Collections.singletonList(FIREBASE_IID_SERVICE_CLASS_NAME);
    }
}
