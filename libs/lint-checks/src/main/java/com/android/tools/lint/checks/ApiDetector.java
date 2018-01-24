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

import static com.android.SdkConstants.ANDROID_PREFIX;
import static com.android.SdkConstants.ANDROID_THEME_PREFIX;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_CLASS;
import static com.android.SdkConstants.ATTR_FULL_BACKUP_CONTENT;
import static com.android.SdkConstants.ATTR_HEIGHT;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LABEL_FOR;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_PADDING_START;
import static com.android.SdkConstants.ATTR_PARENT;
import static com.android.SdkConstants.ATTR_TARGET_API;
import static com.android.SdkConstants.ATTR_TEXT_IS_SELECTABLE;
import static com.android.SdkConstants.ATTR_THEME;
import static com.android.SdkConstants.ATTR_VALUE;
import static com.android.SdkConstants.ATTR_WIDTH;
import static com.android.SdkConstants.BUTTON;
import static com.android.SdkConstants.CHECK_BOX;
import static com.android.SdkConstants.CONSTRUCTOR_NAME;
import static com.android.SdkConstants.DOT_JAVA;
import static com.android.SdkConstants.FQCN_TARGET_API;
import static com.android.SdkConstants.PREFIX_ANDROID;
import static com.android.SdkConstants.SUPPORT_ANNOTATIONS_PREFIX;
import static com.android.SdkConstants.SWITCH;
import static com.android.SdkConstants.TAG;
import static com.android.SdkConstants.TAG_ITEM;
import static com.android.SdkConstants.TAG_STYLE;
import static com.android.SdkConstants.TARGET_API;
import static com.android.SdkConstants.TOOLS_URI;
import static com.android.SdkConstants.VIEW_INCLUDE;
import static com.android.SdkConstants.VIEW_TAG;
import static com.android.tools.lint.checks.RtlDetector.ATTR_SUPPORTS_RTL;
import static com.android.tools.lint.checks.VersionChecks.SDK_INT;
import static com.android.tools.lint.checks.VersionChecks.codeNameToApi;
import static com.android.tools.lint.checks.VersionChecks.isPrecededByVersionCheckExit;
import static com.android.tools.lint.checks.VersionChecks.isVersionCheckConditional;
import static com.android.tools.lint.checks.VersionChecks.isWithinVersionCheckConditional;
import static com.android.tools.lint.detector.api.ClassContext.getFqcn;
import static com.android.tools.lint.detector.api.LintUtils.skipParentheses;
import static com.android.utils.CharSequences.indexOf;
import static com.android.utils.SdkUtils.getResourceFieldName;
import static com.intellij.pom.java.LanguageLevel.JDK_1_7;
import static com.intellij.pom.java.LanguageLevel.JDK_1_8;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.repository.GradleVersion;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.repository.Revision;
import com.android.repository.api.LocalPackage;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.SdkVersionInfo;
import com.android.sdklib.repository.AndroidSdkHandler;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.client.api.LintDriver;
import com.android.tools.lint.client.api.UElementHandler;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ConstantEvaluator;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.DefaultPosition;
import com.android.tools.lint.detector.api.Detector.ResourceFolderScanner;
import com.android.tools.lint.detector.api.Detector.UastScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Position;
import com.android.tools.lint.detector.api.ResourceContext;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.UastLintUtils;
import com.android.tools.lint.detector.api.XmlContext;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiSuperExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UBinaryExpression;
import org.jetbrains.uast.UBinaryExpressionWithType;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UCallableReferenceExpression;
import org.jetbrains.uast.UCatchClause;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UClassLiteralExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UForEachExpression;
import org.jetbrains.uast.UIfExpression;
import org.jetbrains.uast.UImportStatement;
import org.jetbrains.uast.UInstanceExpression;
import org.jetbrains.uast.ULocalVariable;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.USimpleNameReferenceExpression;
import org.jetbrains.uast.USuperExpression;
import org.jetbrains.uast.USwitchClauseExpression;
import org.jetbrains.uast.USwitchExpression;
import org.jetbrains.uast.UThisExpression;
import org.jetbrains.uast.UTryExpression;
import org.jetbrains.uast.UTypeReferenceExpression;
import org.jetbrains.uast.UVariable;
import org.jetbrains.uast.UastBinaryOperator;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.java.JavaUAnnotation;
import org.jetbrains.uast.util.UastExpressionUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Looks for usages of APIs that are not supported in all the versions targeted
 * by this application (according to its minimum API requirement in the manifest).
 */
public class ApiDetector extends ResourceXmlDetector
        implements UastScanner, ResourceFolderScanner {

    public static final String REQUIRES_API_ANNOTATION = SUPPORT_ANNOTATIONS_PREFIX + "RequiresApi";
    public static final String SDK_SUPPRESS_ANNOTATION = "android.support.test.filters.SdkSuppress";

    /** Accessing an unsupported API */
    @SuppressWarnings("unchecked")
    public static final Issue UNSUPPORTED = Issue.create(
            "NewApi",
            "Calling new methods on older versions",

            "This check scans through all the Android API calls in the application and " +
            "warns about any calls that are not available on **all** versions targeted " +
            "by this application (according to its minimum SDK attribute in the manifest).\n" +
            "\n" +
            "If you really want to use this API and don't need to support older devices just " +
            "set the `minSdkVersion` in your `build.gradle` or `AndroidManifest.xml` files.\n" +
            "\n" +
            "If your code is **deliberately** accessing newer APIs, and you have ensured " +
            "(e.g. with conditional execution) that this code will only ever be called on a " +
            "supported platform, then you can annotate your class or method with the " +
            "`@TargetApi` annotation specifying the local minimum SDK to apply, such as " +
            "`@TargetApi(11)`, such that this check considers 11 rather than your manifest " +
            "file's minimum SDK as the required API level.\n" +
            "\n" +
            "If you are deliberately setting `android:` attributes in style definitions, " +
            "make sure you place this in a `values-v`*NN* folder in order to avoid running " +
            "into runtime conflicts on certain devices where manufacturers have added " +
            "custom attributes whose ids conflict with the new ones on later platforms.\n" +
            "\n" +
            "Similarly, you can use tools:targetApi=\"11\" in an XML file to indicate that " +
            "the element will only be inflated in an adequate context.",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            new Implementation(
                    ApiDetector.class,
                    EnumSet.of(Scope.JAVA_FILE, Scope.RESOURCE_FILE, Scope.MANIFEST),
                    Scope.JAVA_FILE_SCOPE,
                    Scope.RESOURCE_FILE_SCOPE,
                    Scope.MANIFEST_SCOPE));

    /** Accessing an inlined API on older platforms */
    public static final Issue INLINED = Issue.create(
            "InlinedApi",
            "Using inlined constants on older versions",

            "This check scans through all the Android API field references in the application " +
            "and flags certain constants, such as static final integers and Strings, " +
            "which were introduced in later versions. These will actually be copied " +
            "into the class files rather than being referenced, which means that " +
            "the value is available even when running on older devices. In some " +
            "cases that's fine, and in other cases it can result in a runtime " +
            "crash or incorrect behavior. It depends on the context, so consider " +
            "the code carefully and device whether it's safe and can be suppressed " +
            "or whether the code needs tbe guarded.\n" +
            "\n" +
            "If you really want to use this API and don't need to support older devices just " +
            "set the `minSdkVersion` in your `build.gradle` or `AndroidManifest.xml` files." +
            "\n" +
            "If your code is **deliberately** accessing newer APIs, and you have ensured " +
            "(e.g. with conditional execution) that this code will only ever be called on a " +
            "supported platform, then you can annotate your class or method with the " +
            "`@TargetApi` annotation specifying the local minimum SDK to apply, such as " +
            "`@TargetApi(11)`, such that this check considers 11 rather than your manifest " +
            "file's minimum SDK as the required API level.\n",
            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            new Implementation(
                    ApiDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    /** Method conflicts with new inherited method */
    public static final Issue OVERRIDE = Issue.create(
            "Override",
            "Method conflicts with new inherited method",

            "Suppose you are building against Android API 8, and you've subclassed Activity. " +
            "In your subclass you add a new method called `isDestroyed`(). At some later point, " +
            "a method of the same name and signature is added to Android. Your method will " +
            "now override the Android method, and possibly break its contract. Your method " +
            "is not calling `super.isDestroyed()`, since your compilation target doesn't " +
            "know about the method.\n" +
            "\n" +
            "The above scenario is what this lint detector looks for. The above example is " +
            "real, since `isDestroyed()` was added in API 17, but it will be true for **any** " +
            "method you have added to a subclass of an Android class where your build target " +
            "is lower than the version the method was introduced in.\n" +
            "\n" +
            "To fix this, either rename your method, or if you are really trying to augment " +
            "the builtin method if available, switch to a higher build target where you can " +
            "deliberately add `@Override` on your overriding method, and call `super` if " +
            "appropriate etc.\n",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            new Implementation(
                    ApiDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    /** Attribute unused on older versions */
    public static final Issue UNUSED = Issue.create(
            "UnusedAttribute",
            "Attribute unused on older versions",

            "This check finds attributes set in XML files that were introduced in a version " +
            "newer than the oldest version targeted by your application (with the " +
            "`minSdkVersion` attribute).\n" +
            "\n" +
            "This is not an error; the application will simply ignore the attribute. However, " +
            "if the attribute is important to the appearance or functionality of your " +
            "application, you should consider finding an alternative way to achieve the " +
            "same result with only available attributes, and then you can optionally create " +
            "a copy of the layout in a layout-vNN folder which will be used on API NN or " +
            "higher where you can take advantage of the newer attribute.\n" +
            "\n" +
            "Note: This check does not only apply to attributes. For example, some tags can be " +
            "unused too, such as the new `<tag>` element in layouts introduced in API 21.",
            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            new Implementation(
                    ApiDetector.class,
                    EnumSet.of(Scope.RESOURCE_FILE, Scope.RESOURCE_FOLDER),
                    Scope.RESOURCE_FILE_SCOPE,
                    Scope.RESOURCE_FOLDER_SCOPE));

    /** Obsolete SDK_INT version check */
    public static final Issue OBSOLETE_SDK = Issue.create(
            "ObsoleteSdkInt",
            "Obsolete SDK_INT Version Check",

            "This check flags version checks that are not necessary, because the " +
            "`minSdkVersion` (or surrounding known API level) is already at least " +
            "as high as the version checked for.\n" +
            "\n" +
            "Similarly, it also looks for resources in `-vNN` folders, such as " +
            "`values-v14` where the version qualifier is less than or equal to the " +
            "`minSdkVersion`, where the contents should be merged into the best folder.",

            Category.PERFORMANCE,
            6,
            Severity.WARNING,
            new Implementation(
                    ApiDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    private static final String TAG_RIPPLE = "ripple";
    private static final String TAG_VECTOR = "vector";
    private static final String TAG_ANIMATED_VECTOR = "animated-vector";
    private static final String TAG_ANIMATED_SELECTOR = "animated-selector";

    protected ApiLookup mApiDatabase;
    private boolean mWarnedMissingDb;
    private int mMinApi = -1;

    /** Constructs a new API check */
    public ApiDetector() {
    }

    @Override
    public void beforeCheckProject(@NonNull Context context) {
        if (mApiDatabase == null) {
            mApiDatabase = ApiLookup.get(context.getClient(),
                    context.getMainProject().getBuildTarget());
            // We can't look up the minimum API required by the project here:
            // The manifest file hasn't been processed yet in the -before- project hook.
            // For now it's initialized lazily in getMinSdk(Context), but the
            // lint infrastructure should be fixed to parse manifest file up front.

            if (mApiDatabase == null && !mWarnedMissingDb) {
                mWarnedMissingDb = true;
                context.report(IssueRegistry.LINT_ERROR, Location.create(context.file),
                        "Can't find API database; API check not performed");
            } else {
                if (mApiDatabase == null || mApiDatabase.getTarget() != null) {
                    // Don't warn about compileSdk/platform-tools mismatch if the API database
                    // corresponds to an SDK platform
                    return;
                }

                // See if you don't have at least version 23.0.1 of platform tools installed
                AndroidSdkHandler sdk = context.getClient().getSdk();
                if (sdk == null) {
                    return;
                }
                LocalPackage pkgInfo = sdk.getLocalPackage(SdkConstants.FD_PLATFORM_TOOLS,
                        context.getClient().getRepositoryLogger());
                if (pkgInfo == null) {
                    return;
                }
                Revision revision = pkgInfo.getVersion();

                // The platform tools must be at at least the same revision
                // as the compileSdkVersion!
                // And as a special case, for 23, they must be at 23.0.1
                // because 23.0.0 accidentally shipped without Android M APIs.
                int compileSdkVersion = context.getProject().getBuildSdk();
                if (compileSdkVersion == 23) {
                    if (revision.getMajor() > 23 || revision.getMajor() == 23
                      && (revision.getMinor() > 0 || revision.getMicro() > 0)) {
                        return;
                    }
                } else if (compileSdkVersion <= revision.getMajor()) {
                    return;
                }

                // Pick a location: when incrementally linting in the IDE, tie
                // it to the current file
                List<File> currentFiles = context.getProject().getSubset();
                Location location;
                if (currentFiles != null && currentFiles.size() == 1) {
                    File file = currentFiles.get(0);
                    CharSequence contents = context.getClient().readFile(file);
                    int firstLineEnd = indexOf(contents, '\n');
                    if (firstLineEnd == -1) {
                        firstLineEnd = contents.length();
                    }
                    location = Location.create(file,
                        new DefaultPosition(0, 0, 0), new
                        DefaultPosition(0, firstLineEnd, firstLineEnd));
                } else {
                    location = Location.create(context.file);
                }
                context.report(UNSUPPORTED,
                        location,
                        String.format("The SDK platform-tools version (%1$s) is too old "
                                        + "to check APIs compiled with API %2$d; please update",
                                revision.toShortString(),
                                compileSdkVersion));
            }
        }
    }

    // ---- Implements XmlScanner ----

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return true;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return ALL;
    }

    @Override
    public Collection<String> getApplicableAttributes() {
        return ALL;
    }

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        if (mApiDatabase == null) {
            return;
        }

        int attributeApiLevel = -1;
        if (ANDROID_URI.equals(attribute.getNamespaceURI())) {
            String name = attribute.getLocalName();
            if (!(name.equals(ATTR_LAYOUT_WIDTH) && !(name.equals(ATTR_LAYOUT_HEIGHT)) &&
                !(name.equals(ATTR_ID)))) {
                String owner = "android/R$attr";
                attributeApiLevel = mApiDatabase.getFieldVersion(owner, name);
                int minSdk = getMinSdk(context);
                if (attributeApiLevel > minSdk && attributeApiLevel > context.getFolderVersion()
                        && attributeApiLevel > getLocalMinSdk(attribute.getOwnerElement())
                        && !isBenignUnusedAttribute(name)
                        && !isAlreadyWarnedDrawableFile(context, attribute, attributeApiLevel)) {
                    if (RtlDetector.isRtlAttributeName(name) || ATTR_SUPPORTS_RTL.equals(name)) {
                        // No need to warn for example that
                        //  "layout_alignParentEnd will only be used in API level 17 and higher"
                        // since we have a dedicated RTL lint rule dealing with those attributes

                        // However, paddingStart in particular is known to cause crashes
                        // when used on TextViews (and subclasses of TextViews), on some
                        // devices, because vendor specific attributes conflict with the
                        // later-added framework resources, and these are apparently read
                        // by the text views.
                        //
                        // However, as of build tools 23.0.1 aapt works around this by packaging
                        // the resources differently.
                        if (name.equals(ATTR_PADDING_START)) {
                            BuildToolInfo buildToolInfo = context.getProject().getBuildTools();
                            Revision buildTools = buildToolInfo != null
                                    ? buildToolInfo.getRevision() : null;
                            boolean isOldBuildTools = buildTools != null &&
                                    (buildTools.getMajor() < 23 || buildTools.getMajor() == 23
                                     && buildTools.getMinor() == 0 && buildTools.getMicro() == 0);
                            if ((buildTools == null || isOldBuildTools) &&
                                    viewMayExtendTextView(attribute.getOwnerElement())) {
                                Location location = context.getLocation(attribute);
                                String message = String.format(
                                        "Attribute `%1$s` referenced here can result in a crash on "
                                                + "some specific devices older than API %2$d "
                                                + "(current min is %3$d)",
                                        attribute.getLocalName(), attributeApiLevel, minSdk);
                                //noinspection VariableNotUsedInsideIf
                                if (buildTools != null) {
                                    message = String.format("Upgrade `buildToolsVersion` from "
                                            + "`%1$s` to at least `23.0.1`; if not, ",
                                                buildTools.toShortString())
                                            + Character.toLowerCase(message.charAt(0))
                                            + message.substring(1);
                                }
                                context.report(UNSUPPORTED, attribute, location, message,
                                        apiLevelFix(attributeApiLevel));
                            }
                        }
                    } else {
                        Location location = context.getLocation(attribute);
                        String message = String.format(
                                "Attribute `%1$s` is only used in API level %2$d and higher "
                                        + "(current min is %3$d)",
                                attribute.getLocalName(), attributeApiLevel, minSdk);
                        context.report(UNUSED, attribute, location, message,
                                apiLevelFix(attributeApiLevel));
                    }
                }
            }

            // Special case:
            // the dividers attribute is present in API 1, but it won't be read on older
            // versions, so don't flag the common pattern
            //    android:divider="?android:attr/dividerHorizontal"
            // since this will work just fine. See issue 67440 for more.
            if (name.equals("divider")) {
                return;
            }

            if (name.equals(ATTR_THEME) && VIEW_INCLUDE.equals(attribute.getOwnerElement().getTagName())) {
                // Requires API 23
                int minSdk = getMinSdk(context);
                if (Math.max(minSdk, context.getFolderVersion()) < 23) {
                    Location location = context.getLocation(attribute);
                    String message = String.format(
                      "Attribute `android:theme` is only used by `<include>` tags in API level 23 and higher "
                      + "(current min is %1$d)", minSdk);
                    context.report(UNUSED, attribute, location, message, apiLevelFix(23));
                }
            }
        }

        String value = attribute.getValue();
        String owner = null;
        String name = null;
        String prefix;
        if (value.startsWith(ANDROID_PREFIX)) {
            prefix = ANDROID_PREFIX;
        } else if (value.startsWith(ANDROID_THEME_PREFIX)) {
            prefix = ANDROID_THEME_PREFIX;
            if (context.getResourceFolderType() == ResourceFolderType.DRAWABLE) {
                int api = 21;
                int minSdk = getMinSdk(context);
                if (api > minSdk && api > context.getFolderVersion()
                        && api > getLocalMinSdk(attribute.getOwnerElement())) {
                    Location location = context.getLocation(attribute);
                    String message;
                    message = String.format(
                            "Using theme references in XML drawables requires API level %1$d "
                                    + "(current min is %2$d)", api, minSdk);
                    context.report(UNSUPPORTED, attribute, location, message, apiLevelFix(api));
                    // Don't flag individual theme attribute requirements here, e.g. once
                    // we've told you that you need at least v21 to reference themes, we don't
                    // need to also tell you that ?android:selectableItemBackground requires
                    // API level 11
                    return;
                }
            }
        } else if (value.startsWith(PREFIX_ANDROID) && ATTR_NAME.equals(attribute.getName())
            && TAG_ITEM.equals(attribute.getOwnerElement().getTagName())
            && attribute.getOwnerElement().getParentNode() != null
            && TAG_STYLE.equals(attribute.getOwnerElement().getParentNode().getNodeName())) {
            owner = "android/R$attr";
            name = value.substring(PREFIX_ANDROID.length());
            prefix = null;
        } else if (value.startsWith(PREFIX_ANDROID) && ATTR_PARENT.equals(attribute.getName())
                && TAG_STYLE.equals(attribute.getOwnerElement().getTagName())) {
            owner = "android/R$style";
            name = getResourceFieldName(value.substring(PREFIX_ANDROID.length()));
            prefix = null;
        } else {
            return;
        }

        if (owner == null) {
            // Convert @android:type/foo into android/R$type and "foo"
            int index = value.indexOf('/', prefix.length());
            if (index != -1) {
                owner = "android/R$"
                        + value.substring(prefix.length(), index);
                name = getResourceFieldName(value.substring(index + 1));
            } else if (value.startsWith(ANDROID_THEME_PREFIX)) {
                owner = "android/R$attr";
                name = value.substring(ANDROID_THEME_PREFIX.length());
            } else {
                return;
            }
        }
        int api = mApiDatabase.getFieldVersion(owner, name);
        int minSdk = getMinSdk(context);
        if (api > minSdk && api > context.getFolderVersion()
                && api > getLocalMinSdk(attribute.getOwnerElement())) {
            // Don't complain about resource references in the tools namespace,
            // such as for example "tools:layout="@android:layout/list_content",
            // used only for designtime previews
            if (TOOLS_URI.equals(attribute.getNamespaceURI())) {
                return;
            }

            //noinspection StatementWithEmptyBody
            if (attributeApiLevel >= api) {
                // The attribute will only be *read* on platforms >= attributeApiLevel.
                // If this isn't lower than the attribute reference's API level, it
                // won't be a problem
            } else if (attributeApiLevel > minSdk) {
                String attributeName = attribute.getLocalName();
                Location location = context.getLocation(attribute);
                String message = String.format(
                        "`%1$s` requires API level %2$d (current min is %3$d), but note "
                                + "that attribute `%4$s` is only used in API level %5$d "
                                + "and higher",
                        name, api, minSdk, attributeName, attributeApiLevel);
                context.report(UNSUPPORTED, attribute, location, message, apiLevelFix(api));
            } else {
                Location location = context.getLocation(attribute);
                String message = String.format(
                        "`%1$s` requires API level %2$d (current min is %3$d)",
                        value, api, minSdk);
                context.report(UNSUPPORTED, attribute, location, message, apiLevelFix(api));
            }
        }
    }

    @NonNull
    private static LintFix apiLevelFix(int api) {
        return fix().data(api);
    }

    /**
     * Returns true if the view tag is possibly a text view. It may not be certain,
     * but will err on the side of caution (for example, any custom view is considered
     * to be a potential text view.)
     */
    private static boolean viewMayExtendTextView(@NonNull Element element) {
        String tag = element.getTagName();
        if (tag.equals(VIEW_TAG)) {
            tag = element.getAttribute(ATTR_CLASS);
            if (tag == null || tag.isEmpty()) {
                return false;
            }
        }

        //noinspection SimplifiableIfStatement
        if (tag.indexOf('.') != -1) {
            // Custom views: not sure. Err on the side of caution.
            return true;

        }

        return tag.contains("Text")  // TextView, EditText, etc
                || tag.contains(BUTTON)  // Button, ToggleButton, etc
                || tag.equals("DigitalClock")
                || tag.equals("Chronometer")
                || tag.equals(CHECK_BOX)
                || tag.equals(SWITCH);
    }

    /**
     * Returns true if this attribute is in a drawable document with one of the
     * root tags that require API 21
     */
    private static boolean isAlreadyWarnedDrawableFile(@NonNull XmlContext context,
            @NonNull Attr attribute, int attributeApiLevel) {
        // Don't complain if it's in a drawable file where we've already
        // flagged the root drawable type as being unsupported
        if (context.getResourceFolderType() == ResourceFolderType.DRAWABLE
                && attributeApiLevel == 21) {
            String root = attribute.getOwnerDocument().getDocumentElement().getTagName();
            if (TAG_RIPPLE.equals(root)
                    || TAG_VECTOR.equals(root)
                    || TAG_ANIMATED_VECTOR.equals(root)
                    || TAG_ANIMATED_SELECTOR.equals(root)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Is the given attribute a "benign" unused attribute, one we probably don't need to
     * flag to the user as not applicable on all versions? These are typically attributes
     * which add some nice platform behavior when available, but that are not critical
     * and developers would not typically need to be aware of to try to implement workarounds
     * on older platforms.
     */
    public static boolean isBenignUnusedAttribute(@NonNull String name) {
        return ATTR_LABEL_FOR.equals(name)
               || ATTR_TEXT_IS_SELECTABLE.equals(name)
               || "textAlignment".equals(name)
               || "roundIcon".equals(name)
               || ATTR_FULL_BACKUP_CONTENT.equals(name);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        if (mApiDatabase == null) {
            return;
        }

        String tag = element.getTagName();

        ResourceFolderType folderType = context.getResourceFolderType();
        if (folderType != ResourceFolderType.LAYOUT) {
            if (folderType == ResourceFolderType.DRAWABLE) {
                checkElement(context, element, TAG_VECTOR, 21, "1.4", UNSUPPORTED);
                checkElement(context, element, TAG_RIPPLE, 21, null, UNSUPPORTED);
                checkElement(context, element, TAG_ANIMATED_SELECTOR, 21, null, UNSUPPORTED);
                checkElement(context, element, TAG_ANIMATED_VECTOR, 21, null, UNSUPPORTED);
                checkElement(context, element, "drawable", 24, null, UNSUPPORTED);
                if ("layer-list".equals(tag)) {
                    checkLevelList(context, element);
                } else if (tag.contains(".")) {
                    checkElement(context, element, tag, 24, null, UNSUPPORTED);
                }
            }
            if (element.getParentNode().getNodeType() != Node.ELEMENT_NODE) {
                // Root node
                return;
            }
            NodeList childNodes = element.getChildNodes();
            for (int i = 0, n = childNodes.getLength(); i < n; i++) {
                Node textNode = childNodes.item(i);
                if (textNode.getNodeType() == Node.TEXT_NODE) {
                    String text = textNode.getNodeValue();
                    if (text.contains(ANDROID_PREFIX)) {
                        text = text.trim();
                        // Convert @android:type/foo into android/R$type and "foo"
                        int index = text.indexOf('/', ANDROID_PREFIX.length());
                        if (index != -1) {
                            String typeString = text.substring(ANDROID_PREFIX.length(), index);
                            if (ResourceType.getEnum(typeString) != null) {
                                String owner = "android/R$" + typeString;
                                String name = getResourceFieldName(text.substring(index + 1));
                                int api = mApiDatabase.getFieldVersion(owner, name);
                                int minSdk = getMinSdk(context);
                                if (api > minSdk && api > context.getFolderVersion()
                                        && api > getLocalMinSdk(element)) {
                                    Location location = context.getLocation(textNode);
                                    String message = String.format(
                                            "`%1$s` requires API level %2$d (current min is %3$d)",
                                            text, api, minSdk);
                                    context.report(UNSUPPORTED, element, location, message,
                                            apiLevelFix(api));
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (VIEW_TAG.equals(tag)) {
                tag = element.getAttribute(ATTR_CLASS);
                if (tag == null || tag.isEmpty()) {
                    return;
                }
            } else {
                // TODO: Complain if <tag> is used at the root level!
                checkElement(context, element, TAG, 21, null, UNUSED);
            }

            // Check widgets to make sure they're available in this version of the SDK.
            if (tag.indexOf('.') != -1) {
                // Custom views aren't in the index
                return;
            }
            String fqn = "android/widget/" + tag;
            if (tag.equals("TextureView")) {
                fqn = "android/view/TextureView";
            }
            // TODO: Consider other widgets outside of android.widget.*
            int api = mApiDatabase.getClassVersion(fqn);
            int minSdk = getMinSdk(context);
            if (api > minSdk && api > context.getFolderVersion()
                    && api > getLocalMinSdk(element)) {
                Location location = context.getLocation(element);
                String message = String.format(
                        "View requires API level %1$d (current min is %2$d): `<%3$s>`",
                        api, minSdk, tag);
                context.report(UNSUPPORTED, element, location, message, apiLevelFix(api));
            }
        }
    }

    /** Checks whether the given element is the given tag, and if so, whether it satisfied
     * the minimum version that the given tag is supported in */
    private void checkLevelList(@NonNull XmlContext context, @NonNull Element element) {
        Node curr = element.getFirstChild();
        while (curr != null) {
            if (curr.getNodeType() == Node.ELEMENT_NODE
                    && TAG_ITEM.equals(curr.getNodeName())) {
                Element e = (Element) curr;
                if (e.hasAttributeNS(ANDROID_URI, ATTR_WIDTH)
                        || e.hasAttributeNS(ANDROID_URI, ATTR_HEIGHT)) {
                    int attributeApiLevel = 23; // Using width and height on layer-list children requires M
                    int minSdk = getMinSdk(context);
                    if (attributeApiLevel > minSdk
                            && attributeApiLevel > context.getFolderVersion()
                            && attributeApiLevel > getLocalMinSdk(element)) {
                        for (String attributeName : new String[] { ATTR_WIDTH, ATTR_HEIGHT}) {
                            Attr attribute = e.getAttributeNodeNS(ANDROID_URI, attributeName);
                            if (attribute == null) {
                                continue;
                            }
                            Location location = context.getLocation(attribute);
                            String message = String.format(
                                    "Attribute `%1$s` is only used in API level %2$d and higher "
                                            + "(current min is %3$d)",
                                    attribute.getLocalName(), attributeApiLevel, minSdk);
                            context.report(UNUSED, attribute, location, message,
                                    apiLevelFix(attributeApiLevel));
                        }
                    }
                }
            }
            curr = curr.getNextSibling();
        }
    }

    /** Checks whether the given element is the given tag, and if so, whether it satisfied
     * the minimum version that the given tag is supported in */
    private void checkElement(@NonNull XmlContext context, @NonNull Element element,
            @NonNull String tag, int api, @Nullable String gradleVersion, @NonNull Issue issue) {
        if (tag.equals(element.getTagName())) {
            int minSdk = getMinSdk(context);
            if (api > minSdk
                    && api > context.getFolderVersion()
                    && api > getLocalMinSdk(element)
                    && !featureProvidedByGradle(context, gradleVersion)) {
                Location location = context.getLocation(element);

                // For the <drawable> tag we report it against the class= attribute
                if ("drawable".equals(tag)) {
                    Attr attribute = element.getAttributeNode(ATTR_CLASS);
                    if (attribute == null) {
                        return;
                    }
                    location = context.getLocation(attribute);
                    tag = ATTR_CLASS;
                }

                String message;
                if (issue == UNSUPPORTED) {
                    message = String.format(
                            "`<%1$s>` requires API level %2$d (current min is %3$d)", tag, api,
                            minSdk);
                    if (gradleVersion != null) {
                        message += String.format(
                                " or building with Android Gradle plugin %1$s or higher",
                                gradleVersion);
                    } else if (tag.contains(".")) {
                        message = String.format(
                                "Custom drawables requires API level %1$d (current min is %2$d)",
                                api, minSdk);
                    }
                } else {
                    assert issue == UNUSED : issue;
                    message = String.format(
                            "`<%1$s>` is only used in API level %2$d and higher "
                                    + "(current min is %3$d)", tag, api, minSdk);
                }
                context.report(issue, element, location, message, apiLevelFix(api));
            }
        }
    }

    protected int getMinSdk(Context context) {
        if (mMinApi == -1) {
            AndroidVersion minSdkVersion = context.getMainProject().getMinSdkVersion();
            mMinApi = minSdkVersion.getFeatureLevel();
            if (mMinApi == 1 && !context.getMainProject().isAndroidProject()) {
                // Don't flag API checks in non-Android projects
                mMinApi = Integer.MAX_VALUE;
            }
        }

        return mMinApi;
    }

    // ---- Implements ClassScanner ----

    private static void checkSimpleDateFormat(JavaContext context, UCallExpression call,
            int minSdk) {
        if (minSdk >= 9) {
            // Already OK
            return;
        }

        List<UExpression> expressions = call.getValueArguments();
        if (expressions.isEmpty()) {
            return;
        }
        UExpression argument = expressions.get(0);
        Object constant = ConstantEvaluator.evaluate(context, argument);
        if (constant instanceof String) {
            String pattern = (String) constant;
            boolean isEscaped = false;
            for (int i = 0; i < pattern.length(); i++) {
                char c = pattern.charAt(i);
                if (c == '\'') {
                    isEscaped = !isEscaped;
                } else if (!isEscaped && (c == 'L' || c == 'c')) {
                    String message = String.format(
                            "The pattern character '%1$c' requires API level 9 (current " +
                                    "min is %2$d) : \"`%3$s`\"", c, minSdk, pattern);
                    context.report(UNSUPPORTED, call, context.getRangeLocation(argument,
                            i + 1, 1), message, apiLevelFix(9));
                    return;
                }
            }
        }
    }

    /**
     * Returns the minimum SDK to use in the given element context, or -1 if no
     * {@code tools:targetApi} attribute was found.
     *
     * @param element the element to look at, including parents
     * @return the API level to use for this element, or -1
     */
    private static int getLocalMinSdk(@NonNull Element element) {
        //noinspection ConstantConditions
        while (element != null) {
            String targetApi = element.getAttributeNS(TOOLS_URI, ATTR_TARGET_API);
            if (targetApi != null && !targetApi.isEmpty()) {
                if (Character.isDigit(targetApi.charAt(0))) {
                    try {
                        return Integer.parseInt(targetApi);
                    } catch (NumberFormatException e) {
                        break;
                    }
                } else {
                    return SdkVersionInfo.getApiByBuildCode(targetApi, true);
                }
            }

            Node parent = element.getParentNode();
            if (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
                element = (Element) parent;
            } else {
                break;
            }
        }

        return -1;
    }

    /**
     * Checks if the current project supports features added in {@code minGradleVersion} version of
     * the Android gradle plugin.
     *
     * @param context                Current context.
     * @param minGradleVersionString Version in which support for a given feature was added, or null
     *                               if it's not supported at build time.
     */
    private static boolean featureProvidedByGradle(@NonNull XmlContext context,
            @Nullable String minGradleVersionString) {
        if (minGradleVersionString == null) {
            return false;
        }

        GradleVersion gradleModelVersion = context.getProject().getGradleModelVersion();
        if (gradleModelVersion != null) {
            GradleVersion minVersion = GradleVersion.tryParse(minGradleVersionString);
            if (minVersion != null
                    && gradleModelVersion.compareIgnoringQualifiers(minVersion) >= 0) {
                return true;
            }
        }
        return false;
    }

    // ---- Implements UastScanner ----

    @Nullable
    @Override
    public UElementHandler createUastHandler(@NonNull JavaContext context) {
        if (mApiDatabase == null || context.isTestSource()) {
            return null;
        }
        if (!context.getMainProject().isAndroidProject()) {
            return null;
        }
        return new ApiVisitor(context);
    }

    @Override
    public List<Class<? extends UElement>> getApplicableUastTypes() {
        List<Class<? extends UElement>> types = new ArrayList<>(14);
        types.add(UImportStatement.class);
        types.add(USimpleNameReferenceExpression.class);
        types.add(ULocalVariable.class);
        types.add(UTryExpression.class);
        types.add(UBinaryExpressionWithType.class);
        types.add(UBinaryExpression.class);
        types.add(UCallExpression.class);
        types.add(UClass.class);
        types.add(UMethod.class);
        types.add(UForEachExpression.class);
        types.add(UClassLiteralExpression.class);
        types.add(USwitchExpression.class);
        types.add(UCallableReferenceExpression.class);
        return types;
    }

    /**
     * Checks whether the given instruction is a benign usage of a constant defined in
     * a later version of Android than the application's {@code minSdkVersion}.
     *
     * @param node  the instruction to check
     * @param name  the name of the constant
     * @param owner the field owner
     * @return true if the given usage is safe on older versions than the introduction
     *              level of the constant
     */
    public static boolean isBenignConstantUsage(
            @Nullable UElement node,
            @NonNull String name,
            @NonNull String owner) {
        if (owner.equals("android/os/Build$VERSION_CODES")) {
            // These constants are required for compilation, not execution
            // and valid code checks it even on older platforms
            return true;
        }
        if (owner.equals("android/view/ViewGroup$LayoutParams")
                && name.equals("MATCH_PARENT")) {
            return true;
        }
        if (owner.equals("android/widget/AbsListView")
                && ((name.equals("CHOICE_MODE_NONE")
                || name.equals("CHOICE_MODE_MULTIPLE")
                || name.equals("CHOICE_MODE_SINGLE")))) {
            // android.widget.ListView#CHOICE_MODE_MULTIPLE and friends have API=1,
            // but in API 11 it was moved up to the parent class AbsListView.
            // Referencing AbsListView#CHOICE_MODE_MULTIPLE technically requires API 11,
            // but the constant is the same as the older version, so accept this without
            // warning.
            return true;
        }

        // Gravity#START and Gravity#END are okay; these were specifically written to
        // be backwards compatible (by using the same lower bits for START as LEFT and
        // for END as RIGHT)
        if ("android/view/Gravity".equals(owner)
                && ("START".equals(name) || "END".equals(name))) {
            return true;
        }

        if (node == null) {
            return false;
        }

        // It's okay to reference the constant as a case constant (since that
        // code path won't be taken) or in a condition of an if statement
        UElement curr = node.getUastParent();
        while (curr != null) {
            if (curr instanceof USwitchClauseExpression) {
                List<UExpression> caseValues = ((USwitchClauseExpression) curr).getCaseValues();
                for (UExpression condition : caseValues) {
                    if (condition != null && UastUtils.isChildOf(node, condition, false)) {
                        return true;
                    }
                }
                return false;
            } else if (curr instanceof UIfExpression) {
                UExpression condition = ((UIfExpression) curr).getCondition();
                return UastUtils.isChildOf(node, condition, false);
            } else if (curr instanceof UMethod || curr instanceof UClass) {
                break;
            }
            curr = curr.getUastParent();
        }

        return false;
    }

    private final class ApiVisitor extends UElementHandler {
        private final JavaContext mContext;

        private ApiVisitor(JavaContext context) {
            mContext = context;
        }

        @Override
        public void visitImportStatement(@NonNull UImportStatement statement) {
            if (!statement.isOnDemand()) {
                PsiElement resolved = statement.resolve();
                if (resolved instanceof PsiField) {
                    checkField(statement, (PsiField)resolved);
                }
            }
        }

        @Override
        public void visitSimpleNameReferenceExpression(@NonNull USimpleNameReferenceExpression node) {
            PsiElement resolved = node.resolve();
            if (resolved instanceof PsiField) {
                checkField(node, (PsiField)resolved);
            } else if (resolved instanceof PsiMethod &&
                    node instanceof UCallExpression) {
                checkMethodReference(node, (PsiMethod) resolved);
            }
        }

        @Override
        public void visitCallableReferenceExpression(@NonNull UCallableReferenceExpression node) {
            PsiElement resolved = node.resolve();
            if (resolved instanceof PsiMethod) {
                checkMethodReference(node, (PsiMethod) resolved);
            }
        }

        private void checkMethodReference(UReferenceExpression expression, PsiMethod method) {
            PsiClass containingClass = method.getContainingClass();
            if (containingClass == null) {
                return;
            }
            JavaEvaluator evaluator = mContext.getEvaluator();
            String owner = evaluator.getInternalName(containingClass);
            if (owner == null) {
                return; // Couldn't resolve type
            }
            if (!mApiDatabase.containsClass(owner)) {
                return;
            }

            String name = LintUtils.getInternalMethodName(method);
            String desc = evaluator.getInternalDescription(method, false, false);
            if (desc == null) {
                // Couldn't compute description of method for some reason; probably
                // failure to resolve parameter types
                return;
            }

            int api = mApiDatabase.getCallVersion(owner, name, desc);
            if (api == -1) {
                return;
            }
            int minSdk = getMinSdk(mContext);
            if (isSuppressed(mContext, api, expression, minSdk)) {
                return;
            }

            String signature = expression.asSourceString();
            Location location = mContext.getLocation(expression);
            String message = String.format(
                "Method reference requires API level %1$d (current min is %2$d): %3$s", api,
                Math.max(minSdk, getTargetApi(expression)), signature);
            mContext.report(UNSUPPORTED, expression, location, message, apiLevelFix(api));
        }

        @Override
        public void visitBinaryExpressionWithType(@NonNull UBinaryExpressionWithType node) {
            if (UastExpressionUtils.isTypeCast(node)) {
                visitTypeCastExpression(node);
            }
        }

        private void visitTypeCastExpression(UBinaryExpressionWithType expression) {
            UExpression operand = expression.getOperand();
            PsiType operandType = operand.getExpressionType();
            PsiType castType = expression.getType();
            if (castType.equals(operandType)) {
                return;
            }
            if (!(operandType instanceof PsiClassType)) {
                return;
            }
            if (!(castType instanceof PsiClassType)) {
                return;
            }
            PsiClassType classType = (PsiClassType)operandType;
            PsiClassType interfaceType = (PsiClassType)castType;
            checkCast(expression, classType, interfaceType);
        }

        private void checkCast(
                @NonNull UElement node,
                @NonNull PsiClassType classType,
                @NonNull PsiClassType interfaceType) {
            if (classType.equals(interfaceType)) {
                return;
            }
            JavaEvaluator evaluator = mContext.getEvaluator();
            String classTypeInternal = evaluator.getInternalName(classType);
            String interfaceTypeInternal = evaluator.getInternalName(interfaceType);
            if (interfaceTypeInternal == null || classTypeInternal == null) {
                return;
            }
            if ("java/lang/Object".equals(interfaceTypeInternal)) {
                return;
            }

            int api = mApiDatabase.getValidCastVersion(classTypeInternal, interfaceTypeInternal);
            if (api == -1) {
                return;
            }

            int minSdk = getMinSdk(mContext);
            if (api <= minSdk) {
                return;
            }

            if (isSuppressed(mContext, api, node, minSdk)) {
                return;
            }

            Location location = mContext.getLocation(node);
            String message = String.format("Cast from %1$s to %2$s requires API level %3$d "
                            + "(current min is %4$d)",
                    classType.getClassName(),
                    interfaceType.getClassName(), api, Math.max(minSdk, getTargetApi(node)));
            mContext.report(UNSUPPORTED, node, location, message, apiLevelFix(api));
        }

        @Override
        public void visitMethod(@NonNull UMethod method) {
            PsiClass containingClass = method.getContainingClass();

            // API check for default methods
            if (containingClass != null && containingClass.isInterface()
                    // (unless using desugar which supports this for all API levels)
                    && !isUsingDesugar(mContext, method)) {
                PsiModifierList methodModifierList = method.getModifierList();
                if (methodModifierList.hasExplicitModifier(PsiModifier.DEFAULT) ||
                        methodModifierList.hasExplicitModifier(PsiModifier.STATIC)) {
                    int api = 24; // minSdk for default methods
                    int minSdk = getMinSdk(mContext);

                    if (!isSuppressed(mContext, api, method, minSdk)) {
                        Location location = mContext.getLocation(method);
                        String message = String.format("%1$s method requires API level %2$d "
                                        + "(current min is %3$d)",
                                methodModifierList.hasExplicitModifier(PsiModifier.DEFAULT)
                                        ? "Default" : "Static interface ",
                                api,
                                Math.max(minSdk, getTargetApi(method)));
                        mContext.report(UNSUPPORTED, method, location, message, apiLevelFix(api));
                    }
                }
            }

            int buildSdk = mContext.getMainProject().getBuildSdk();
            String name = method.getName();
            JavaEvaluator evaluator = mContext.getEvaluator();
            PsiMethod superMethod = evaluator.getSuperMethod(method);
            while (superMethod != null) {
                PsiClass cls = superMethod.getContainingClass();
                if (cls == null) {
                    break;
                }
                String fqcn = cls.getQualifiedName();
                if (fqcn == null) {
                    break;
                }
                if (fqcn.startsWith("android.")
                        || fqcn.startsWith("java.")
                            && !fqcn.equals(CommonClassNames.JAVA_LANG_OBJECT)
                        || fqcn.startsWith("javax.")) {
                    String desc = evaluator.getInternalDescription(superMethod, false, false);
                    if (desc != null) {
                        String owner = evaluator.getInternalName(cls);
                        if (owner == null) {
                            return;
                        }
                        int api = mApiDatabase.getCallVersion(owner, name, desc);
                        if (api > buildSdk && buildSdk != -1) {
                            if (mContext.getDriver().isSuppressed(mContext, OVERRIDE,
                                    (UElement)method)) {
                                return;
                            }

                            // TODO: Don't complain if it's annotated with @Override; that means
                            // somehow the build target isn't correct.
                            if (containingClass != null) {
                                String className = containingClass.getName();
                                String fullClassName = containingClass.getQualifiedName();
                                if (fullClassName != null) {
                                    className = fullClassName;
                                }
                                fqcn = className + '#' + name;
                            } else {
                                fqcn = name;
                            }

                            String message = String.format(
                                    "This method is not overriding anything with the current "
                                            + "build target, but will in API level %1$d (current "
                                            + "target is %2$d): %3$s",
                                    api, buildSdk, fqcn);

                            PsiElement locationNode = method.getNameIdentifier();
                            if (locationNode == null) {
                                locationNode = method;
                            }
                            Location location = mContext.getLocation(locationNode);
                            mContext.report(OVERRIDE, method, location, message);
                        }

                    }
                } else {
                    break;
                }

                superMethod = evaluator.getSuperMethod(superMethod);
            }
        }

        @Override
        public void visitClass(@NonNull UClass aClass) {
            // Check for repeatable and type annotations
            if (aClass.isAnnotationType()
                    // Desugar adds support for type annotations
                    && !isUsingDesugar(mContext, aClass)) {
                PsiModifierList modifierList = aClass.getModifierList();
                if (modifierList != null) {
                    for (PsiAnnotation annotation : modifierList.getAnnotations()) {
                        String name = annotation.getQualifiedName();
                        if ("java.lang.annotation.Repeatable".equals(name)) {
                            int api = 24; // minSdk for repeatable annotations
                            int minSdk = getMinSdk(mContext);
                            if (!isSuppressed(mContext, api, aClass, minSdk)) {
                                Location location = mContext.getLocation(annotation);
                                String message = String.format("Repeatable annotation requires "
                                        + "API level %1$d (current min is %2$d)", api,
                                        Math.max(minSdk, getTargetApi(aClass)));
                                mContext.report(UNSUPPORTED, annotation, location, message,
                                        apiLevelFix(api));
                            }
                        } else if ("java.lang.annotation.Target".equals(name)) {
                            PsiNameValuePair[] attributes = annotation.getParameterList()
                                    .getAttributes();
                            for (PsiNameValuePair pair : attributes) {
                                PsiAnnotationMemberValue value = pair.getValue();
                                if (value instanceof PsiArrayInitializerMemberValue) {
                                    PsiArrayInitializerMemberValue array
                                            = (PsiArrayInitializerMemberValue) value;
                                    for (PsiAnnotationMemberValue t : array.getInitializers()) {
                                        checkAnnotationTarget(t, modifierList);
                                    }
                                } else if (value != null) {
                                    checkAnnotationTarget(value, modifierList);
                                }
                            }
                        }
                    }
                }
            }

            // Check super types
            for (UTypeReferenceExpression typeReferenceExpression : aClass.getUastSuperTypes()) {
                PsiType type = typeReferenceExpression.getType();
                if (type instanceof PsiClassType) {
                    PsiClassType classType = (PsiClassType)type;
                    PsiClass cls = classType.resolve();
                    if (cls != null) {
                        checkClass(typeReferenceExpression, cls);
                    }
                }
            }
        }

        @Override
        public void visitClassLiteralExpression(@NonNull UClassLiteralExpression expression) {
            UExpression element = expression.getExpression();
            if (element instanceof UTypeReferenceExpression) {
                PsiType type = ((UTypeReferenceExpression)element).getType();
                if (type instanceof PsiClassType) {
                    checkClassType(element, (PsiClassType) type, null);
                }
            }
        }

        private void checkClassType(
                @NonNull UElement element,
                @NonNull PsiClassType classType,
                @Nullable String descriptor) {
            String owner = mContext.getEvaluator().getInternalName(classType);
            String fqcn = classType.getCanonicalText();
            if (owner != null) {
                checkClass(element, descriptor, owner, fqcn);
            }
        }

        private void checkClass(
                @NonNull UElement element,
                @NonNull PsiClass cls) {
            String owner = mContext.getEvaluator().getInternalName(cls);
            if (owner == null) {
                return;
            }
            String fqcn = cls.getQualifiedName();
            if (fqcn != null) {
                checkClass(element, null, owner, fqcn);
            }
        }

        private void checkClass(@NonNull UElement element, @Nullable String descriptor,
                @NonNull String owner, @NonNull String fqcn) {
            int api = mApiDatabase.getClassVersion(owner);
            if (api == -1) {
                return;
            }
            int minSdk = getMinSdk(mContext);
            if (isSuppressed(mContext, api, element, minSdk)) {
                return;
            }

            // It's okay to reference classes from annotations
            if (UastUtils.getParentOfType(element, UAnnotation.class) != null) {
                return;
            }

            Location location = mContext.getNameLocation(element);
            minSdk = Math.max(minSdk, getTargetApi(element));
            String message = String.format(
                    "%1$s requires API level %2$d (current min is %3$d): %4$s",
                    descriptor == null ? "Class" : descriptor, api,
                    Math.max(minSdk, getTargetApi(element)), fqcn);
            mContext.report(UNSUPPORTED, element, location, message, apiLevelFix(api));
        }

        private void checkAnnotationTarget(@NonNull PsiAnnotationMemberValue element,
                PsiModifierList modifierList) {
            if (element instanceof UReferenceExpression) {
                UReferenceExpression ref = (UReferenceExpression) element;
                String referenceName = UastLintUtils.getReferenceName(ref);
                if ("TYPE_PARAMETER".equals(referenceName)
                        || "TYPE_USE".equals(referenceName)) {
                    PsiAnnotation retention = modifierList
                            .findAnnotation("java.lang.annotation.Retention");
                    if (retention == null ||
                            retention.getText().contains("RUNTIME")) {
                        Location location = mContext.getLocation(element);
                        String message = String.format("Type annotations are not "
                                + "supported in Android: %1$s", referenceName);
                        mContext.report(UNSUPPORTED, element, location, message);
                    }
                }
            }
        }

        @Override
        public void visitForEachExpression(@NonNull UForEachExpression statement) {
            // The for each method will implicitly call iterator() on the
            // Iterable that is used in the for each loop; make sure that
            // the API level for that

            UExpression value = statement.getIteratedValue();

            JavaEvaluator evaluator = mContext.getEvaluator();
            PsiType type = value.getExpressionType();
            if (type instanceof PsiClassType) {
                String expressionOwner = evaluator.getInternalName((PsiClassType)type);
                if (expressionOwner == null) {
                    return;
                }
                int api = mApiDatabase.getClassVersion(expressionOwner);
                if (api == -1) {
                    return;
                }
                int minSdk = getMinSdk(mContext);
                if (isSuppressed(mContext, api, statement, minSdk)) {
                    return;
                }

                Location location = mContext.getLocation(value);
                String message = String.format("The type of the for loop iterated value is "
                                + "%1$s, which requires API level %2$d"
                                + " (current min is %3$d)", type.getCanonicalText(), api,
                        Math.max(minSdk, getTargetApi(statement)));

                // Add specific check ConcurrentHashMap#keySet and add workaround text.
                // This was an unfortunate incompatible API change in Open JDK 8, which is
                // not an issue for the Android SDK but is relevant if you're using a
                // Java library.
                if (value instanceof UQualifiedReferenceExpression) {
                    UQualifiedReferenceExpression keySetRef = (UQualifiedReferenceExpression)value;
                    if ("keySet".equals(keySetRef.getResolvedName())) {
                        PsiElement keySet = keySetRef.resolve();
                        if (keySet instanceof PsiMethod) {
                            PsiClass containingClass = ((PsiMethod) keySet).getContainingClass();
                            if (containingClass != null &&
                                    "java.util.concurrent.ConcurrentHashMap".equals(
                                            containingClass.getQualifiedName())) {
                                message += "; to work around this, add an explicit cast to (Map) "
                                        + "before the `keySet` call.";
                            }
                        }
                    }
                }
                mContext.report(UNSUPPORTED, statement, location, message, apiLevelFix(api));
            }
        }

        @Override
        public void visitCallExpression(@NonNull UCallExpression expression) {
            PsiMethod method = expression.resolve();
            if (method == null) {
                return;
            }

            PsiClass containingClass = method.getContainingClass();
            if (containingClass == null) {
                return;
            }

            // Enforce @RequiresApi
            PsiModifierList modifierList = method.getModifierList();
            if (!checkRequiresApi(expression, method, modifierList)) {
                modifierList = containingClass.getModifierList();
                if (modifierList != null) {
                    checkRequiresApi(expression, method, modifierList);
                }
            }

            PsiParameterList parameterList = method.getParameterList();
            if (parameterList.getParametersCount() > 0) {
                PsiParameter[] parameters = parameterList.getParameters();
                List<UExpression> arguments = expression.getValueArguments();
                for (int i = 0; i < parameters.length; i++) {
                    PsiType parameterType = parameters[i].getType();
                    if (parameterType instanceof PsiClassType) {
                        if (i >= arguments.size()) {
                            // We can end up with more arguments than parameters when
                            // there is a varargs call.
                            break;
                        }
                        UExpression argument = arguments.get(i);
                        PsiType argumentType = argument.getExpressionType();
                        if (argumentType == null || parameterType.equals(argumentType)
                                || !(argumentType instanceof PsiClassType)) {
                            continue;
                        }
                        checkCast(argument, (PsiClassType)argumentType,
                                (PsiClassType)parameterType);
                    }
                }
            }

            JavaEvaluator evaluator = mContext.getEvaluator();
            String owner = evaluator.getInternalName(containingClass);
            if (owner == null) {
                return; // Couldn't resolve type
            }

            // Support library: we can do compile time resolution
            if (owner.startsWith("android/support/")) {
                return;
            }
            if (!mApiDatabase.containsClass(owner)) {
                return;
            }

            String name = LintUtils.getInternalMethodName(method);
            String desc = evaluator.getInternalDescription(method, false, false);
            if (desc == null) {
                // Couldn't compute description of method for some reason; probably
                // failure to resolve parameter types
                return;
            }

            if (owner.equals("java/text/SimpleDateFormat") &&
                    name.equals(CONSTRUCTOR_NAME) && !desc.equals("()V")) {
                checkSimpleDateFormat(mContext, expression, getMinSdk(mContext));
            }

            int api = mApiDatabase.getCallVersion(owner, name, desc);
            if (api == -1) {
                return;
            }
            int minSdk = getMinSdk(mContext);
            if (api <= minSdk) {
                return;
            }

            String fqcn = containingClass.getQualifiedName();

            // The lint API database contains two optimizations:
            // First, all members that were available in API 1 are omitted from the database,
            // since that saves about half of the size of the database, and for API check
            // purposes, we don't need to distinguish between "doesn't exist" and "available
            // in all versions".

            // Second, all inherited members were inlined into each class, so that it doesn't
            // have to do a repeated search up the inheritance chain.
            //
            // Unfortunately, in this custom PSI detector, we look up the real resolved method,
            // which can sometimes have a different minimum API.
            //
            // For example, SQLiteDatabase had a close() method from API 1. Therefore, calling
            // SQLiteDatabase is supported in all versions. However, it extends SQLiteClosable,
            // which in API 16 added "implements Closable". In this detector, if we have the
            // following code:
            //     void test(SQLiteDatabase db) { db.close }
            // here the call expression will be the close method on type SQLiteClosable. And
            // that will result in an API requirement of API 16, since the close method it now
            // resolves to is in API 16.
            //
            // To work around this, we can now look up the type of the call expression ("db"
            // in the above, but it could have been more complicated), and if that's a
            // different type than the type of the method, we look up *that* method from
            // lint's database instead. Furthermore, it's possible for that method to return
            // "-1" and we can't tell if that means "doesn't exist" or "present in API 1", we
            // then check the package prefix to see whether we know it's an API method whose
            // members should all have been inlined.
            if (UastExpressionUtils.isMethodCall(expression)) {
                UExpression qualifier = expression.getReceiver();
                if (qualifier != null && !(qualifier instanceof UThisExpression)
                        && !(qualifier instanceof PsiSuperExpression)) {
                    PsiType receiverType = qualifier.getExpressionType();
                    if (receiverType instanceof PsiClassType) {
                        PsiClassType containingType =
                                mContext.getEvaluator().getClassType(containingClass);
                        List<PsiClassType> inheritanceChain = getInheritanceChain(
                                (PsiClassType) receiverType, containingType);
                        if (inheritanceChain != null) {
                            for (PsiClassType type : inheritanceChain) {
                                String expressionOwner = evaluator.getInternalName(type);
                                if (expressionOwner != null && !expressionOwner.equals(owner)) {
                                    int specificApi =
                                            mApiDatabase.getCallVersion(expressionOwner, name, desc);
                                    if (specificApi == -1) {
                                        if (ApiLookup.isRelevantOwner(expressionOwner)) {
                                            return;
                                        }
                                    } else if (specificApi <= minSdk) {
                                        return;
                                    } else {
                                        // For example, for Bundle#getString(String,String) the API level
                                        // is 12, whereas for BaseBundle#getString(String,String) the API
                                        // level is 21. If the code specified a Bundle instead of
                                        // a BaseBundle, reported the Bundle level in the error message
                                        // instead.
                                        if (specificApi < api) {
                                            api = specificApi;
                                            fqcn = type.getCanonicalText();
                                        }
                                        api = Math.min(specificApi, api);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Unqualified call; need to search in our super hierarchy
                    // Unfortunately, expression.getReceiverType() does not work correctly
                    // in Java; it returns the type of the static binding of the call
                    // instead of giving the virtual dispatch type, as described in
                    // https://issuetracker.google.com/64528052 (and covered by
                    // for example ApiDetectorTest#testListView). Therefore, we continue
                    // to use the workaround method for Java (which isn't correct, and is
                    // particularly broken in Kotlin where the dispatch needs to take into
                    // account top level functions and extension methods), and then we use
                    // the correct receiver type in Kotlin.
                    PsiClass cls = null;
                    if (mContext.file.getPath().endsWith(DOT_JAVA)) {
                        cls = UastUtils.getContainingClass(expression);
                    } else {
                        PsiType receiverType = expression.getReceiverType();
                        if (receiverType instanceof PsiClassType) {
                            cls = ((PsiClassType) receiverType).resolve();
                        }
                    }

                    //noinspection ConstantConditions
                    if (qualifier instanceof UThisExpression
                            || qualifier instanceof USuperExpression) {
                        UInstanceExpression pte = (UInstanceExpression) qualifier;
                        PsiElement resolved = pte.resolve();
                        if (resolved instanceof PsiClass) {
                            cls = (PsiClass)resolved;
                        }
                    }

                    while (cls != null) {
                        if (cls instanceof PsiAnonymousClass) {
                            // If it's an unqualified call in an anonymous class, we need to
                            // rely on the resolve method to find out whether the method is
                            // picked up from the anonymous class chain or any outer classes
                            boolean found = false;
                            PsiClassType anonymousBaseType = ((PsiAnonymousClass) cls)
                                    .getBaseClassType();
                            PsiClass anonymousBase = anonymousBaseType.resolve();
                            if (anonymousBase != null && anonymousBase
                                    .isInheritor(containingClass, true)) {
                                cls = anonymousBase;
                                found = true;
                            } else {
                                PsiClass surroundingBaseType = PsiTreeUtil.getParentOfType(cls,
                                        PsiClass.class, true);
                                if (surroundingBaseType != null && surroundingBaseType
                                        .isInheritor(containingClass, true)) {
                                    cls = surroundingBaseType;
                                    found = true;
                                }
                            }
                            if (!found) {
                                break;
                            }
                        }
                        String expressionOwner = evaluator.getInternalName(cls);
                        if (expressionOwner == null || "java/lang/Object".equals(expressionOwner)) {
                            break;
                        }
                        int specificApi = mApiDatabase.getCallVersion(expressionOwner, name, desc);
                        if (specificApi == -1) {
                            if (ApiLookup.isRelevantOwner(expressionOwner)) {
                                return;
                            }
                        } else if (specificApi <= minSdk) {
                            return;
                        } else {
                            if (specificApi < api) {
                                api = specificApi;
                                fqcn = cls.getQualifiedName();
                            }
                            api = Math.min(specificApi, api);
                            break;
                        }
                        cls = cls.getSuperClass();
                    }
                }
            }

            if (isSuppressed(mContext, api, expression, minSdk)) {
                return;
            }

            if (UastExpressionUtils.isMethodCall(expression)) {
                UExpression receiver = expression.getReceiver();

                PsiClass target = null;
                if (!method.isConstructor()) {
                    if (receiver != null) {
                        PsiType type = receiver.getExpressionType();
                        if (type instanceof PsiClassType) {
                            target = ((PsiClassType)type).resolve();
                        }
                    }
                    else {
                        target = UastUtils.getContainingClass(expression);
                    }
                }

                // Look to see if there's a possible local receiver
                if (target != null) {
                    PsiMethod[] methods = target.findMethodsBySignature(method, true);
                    if (methods.length > 1) {
                        for (PsiMethod m : methods) {
                            if (!method.equals(m)) {
                                PsiClass provider = m.getContainingClass();
                                if (provider != null) {
                                    String methodOwner = evaluator.getInternalName(provider);
                                    if (methodOwner != null) {
                                        int methodApi = mApiDatabase.getCallVersion(methodOwner, name, desc);
                                        if (methodApi == -1 || methodApi <= minSdk) {
                                            // Yes, we found another call that doesn't have an API requirement
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // If you're simply calling super.X from method X, even if method X is in a higher
                // API level than the minSdk, we're generally safe; that method should only be
                // called by the framework on the right API levels. (There is a danger of somebody
                // calling that method locally in other contexts, but this is hopefully unlikely.)
                if (receiver instanceof USuperExpression) {
                    PsiMethod containingMethod = UastUtils.getContainingMethod(expression);
                    if (containingMethod != null && name.equals(containingMethod.getName())
                            && evaluator.areSignaturesEqual(method, containingMethod)
                            // We specifically exclude constructors from this check, because we
                            // do want to flag constructors requiring the new API level; it's
                            // highly likely that the constructor is called by local code so
                            // you should specifically investigate this as a developer
                            && !method.isConstructor()) {
                        return;
                    }
                }

                // If it's a method we have source for, obviously it shouldn't be a
                // violation. (This happens for example when compiling the support library.)
                if (!(method instanceof PsiCompiledElement)) {
                    return;
                }
            }

            // Desugar rewrites compare calls (see b/36390874)
            if (name.equals("compare") && api == 19
                    && owner.startsWith("java/lang/") && desc.length() == 4
                    && isUsingDesugar(mContext, expression)
                    && (desc.equals("(JJ)") || desc.equals("(ZZ)") || desc.equals("(BB)")
                    || desc.equals("(CC)") || desc.equals("(II)") || desc.equals("(JJ)")
                    || desc.equals("(SS)"))) {
                return;
            }

            String signature;
            if (CONSTRUCTOR_NAME.equals(name)) {
                signature = "new " + fqcn;
            } else {
                signature = fqcn + '#' + name;
            }

            UElement nameIdentifier = expression.getMethodIdentifier();

            Location location;
            if (UastExpressionUtils.isConstructorCall(expression)
                    && expression.getClassReference() != null) {
                location = mContext.getRangeLocation(expression, 0, expression.getClassReference(), 0);
            } else if (nameIdentifier != null) {
                location = mContext.getLocation(nameIdentifier);
            } else {
                location = mContext.getLocation(expression);
            }
            String message = String.format(
                    "Call requires API level %1$d (current min is %2$d): %3$s", api,
                    Math.max(minSdk, getTargetApi(expression)), signature);

            mContext.report(UNSUPPORTED, expression, location, message, apiLevelFix(api));
        }

        // Look for @RequiresApi in modifier lists
        private boolean checkRequiresApi(UElement expression, PsiMember member,
                    PsiModifierList modifierList) {
            for (PsiAnnotation annotation : modifierList.getAnnotations()) {
                if (REQUIRES_API_ANNOTATION.equals(annotation.getQualifiedName())) {
                    UAnnotation wrapped = JavaUAnnotation.wrap(annotation);
                    int api = (int) SupportAnnotationDetector.getLongAttribute(mContext,
                            wrapped, ATTR_VALUE, -1);
                    if (api <= 1) {
                        // @RequiresApi has two aliasing attributes: api and value
                        api = (int) SupportAnnotationDetector.getLongAttribute(mContext,
                                wrapped, "api", -1);
                    }
                    int minSdk = getMinSdk(mContext);
                    if (api > minSdk) {
                        int target = getTargetApi(expression);
                        if (target == -1 || api > target) {
                            if (isWithinVersionCheckConditional(
                                    expression, api)) {
                                return true;
                            }
                            if (isPrecededByVersionCheckExit(
                                    expression, api)) {
                                return true;
                            }

                            Location location;
                            if (UastExpressionUtils.isConstructorCall(expression)
                                    && ((UCallExpression)expression).getClassReference() != null) {
                                location = mContext.getRangeLocation(expression, 0,
                                        ((UCallExpression)expression).getClassReference(), 0);
                            } else {
                                location = mContext.getNameLocation(expression);
                            }

                            String fqcn = member.getName();
                            String message = String.format(
                                "Call requires API level %1$d (current min is %2$d): `%3$s`",
                                api, Math.max(minSdk, getTargetApi(expression)), fqcn);
                            mContext.report(UNSUPPORTED, expression, location, message,
                                    apiLevelFix(api));
                        }
                    }

                    return true;
                }
            }

            return false;
        }

        @Override
        public void visitLocalVariable(@NonNull ULocalVariable variable) {
            UExpression initializer = variable.getUastInitializer();
            if (initializer == null) {
                return;
            }

            PsiType initializerType = initializer.getExpressionType();
            if (!(initializerType instanceof PsiClassType)) {
                return;
            }

            PsiType interfaceType = variable.getType();
            if (initializerType.equals(interfaceType)) {
                return;
            }

            if (!(interfaceType instanceof PsiClassType)) {
                return;
            }

            checkCast(initializer, (PsiClassType)initializerType, (PsiClassType)interfaceType);
        }

        @Override
        public void visitBinaryExpression(@NonNull UBinaryExpression expression) {
            if (expression.getOperator() instanceof UastBinaryOperator.AssignOperator) {
                UExpression rExpression = expression.getRightOperand();
                PsiType rhsType = rExpression.getExpressionType();
                if (!(rhsType instanceof PsiClassType)) {
                    return;
                }

                PsiType interfaceType = expression.getLeftOperand().getExpressionType();
                if (rhsType.equals(interfaceType)) {
                    return;
                }

                if (!(interfaceType instanceof PsiClassType)) {
                    return;
                }

                checkCast(rExpression, (PsiClassType)rhsType, (PsiClassType)interfaceType);
            }
        }

        @Override
        public void visitTryExpression(@NonNull UTryExpression statement) {
            List<UVariable> resourceList = statement.getResourceVariables();
            //noinspection VariableNotUsedInsideIf
            if (!resourceList.isEmpty()
                    // (unless using desugar which supports this for all API levels)
                    && !isUsingDesugar(mContext, statement)) {

                int api = 19; // minSdk for try with resources
                int minSdk = getMinSdk(mContext);

                if (api > minSdk && api > getTargetApi(statement)) {
                    if (isSuppressed(mContext, api, statement, minSdk)) {
                        return;
                    }

                    // Create location range for the resource list
                    UVariable first = resourceList.get(0);
                    UVariable last = resourceList.get(resourceList.size() - 1);
                    Location location = mContext.getRangeLocation(first, 0, last, 0);

                    String message = String.format("Try-with-resources requires "
                            + "API level %1$d (current min is %2$d)", api,
                            Math.max(minSdk, getTargetApi(statement)));
                    mContext.report(UNSUPPORTED, statement, location, message, apiLevelFix(api));
                }
            }

            for (UCatchClause catchClause : statement.getCatchClauses()) {
                // Special case reflective operation exception which can be implicitly used
                // with multi-catches: see issue 153406
                int minSdk = getMinSdk(mContext);
                if (minSdk < 19 && isMultiCatchReflectiveOperationException(catchClause)) {
                    if (isSuppressed(mContext, 19, statement, minSdk)) {
                        return;
                    }

                    String message = String.format("Multi-catch with these reflection exceptions requires API level 19 (current min is %d) " +
                                    "because they get compiled to the common but new super type `ReflectiveOperationException`. " +
                                    "As a workaround either create individual catch statements, or catch `Exception`.",
                            minSdk);

                    mContext.report(UNSUPPORTED, statement,
                            getCatchParametersLocation(mContext, catchClause), message,
                            apiLevelFix(19));
                    continue;
                }

                for (UTypeReferenceExpression typeReference : catchClause.getTypeReferences()) {
                    checkCatchTypeElement(statement, typeReference, typeReference.getType());
                }
            }
        }

        private void checkCatchTypeElement(@NonNull UTryExpression statement,
                @NonNull UTypeReferenceExpression typeReference,
                @Nullable PsiType type) {
            PsiClass resolved = null;
            if (type instanceof PsiClassType) {
                resolved = ((PsiClassType) type).resolve();
            }
            if (resolved != null) {
                String signature = mContext.getEvaluator().getInternalName(resolved);
                if (signature == null) {
                    return;
                }
                int api = mApiDatabase.getClassVersion(signature);
                if (api == -1) {
                    return;
                }
                int minSdk = getMinSdk(mContext);
                if (api <= minSdk) {
                    return;
                }
                int target = getTargetApi(statement);
                if (target != -1 && api <= target) {
                    return;
                }

                if (isSuppressed(mContext, api, statement, minSdk)) {
                    return;
                }

                Location location = mContext.getLocation(typeReference);
                String fqcn = resolved.getQualifiedName();
                String message = String.format("Class requires API level %1$d (current min is %2$d): %3$s",
                        api, minSdk, fqcn);
                mContext.report(UNSUPPORTED, location, message, apiLevelFix(api));
            }
        }

        @Override
        public void visitSwitchExpression(@NonNull USwitchExpression statement) {
            UExpression expression = statement.getExpression();
            if (expression != null) {
                PsiType type = expression.getExpressionType();
                if (type instanceof PsiClassType) {
                    checkClassType(expression, (PsiClassType) type, "Enum for switch");
                }
            }
        }

        /**
         * Checks a Java source field reference. Returns true if the field is known
         * regardless of whether it's an invalid field or not
         */
        private void checkField(@NonNull UElement node, @NonNull PsiField field) {
            PsiType type = field.getType();
            String name = field.getName();

            if (SDK_INT.equals(name)) { // TODO && "android/os/Build$VERSION".equals(owner) ?
                checkObsoleteSdkVersion(mContext, node);
            }

            PsiClass containingClass = field.getContainingClass();
            if (containingClass == null || name == null) {
                return;
            }
            String owner = mContext.getEvaluator().getInternalName(containingClass);
            if (owner == null) {
                return;
            }

            // Enforce @RequiresApi
            PsiModifierList modifierList = field.getModifierList();
            if (!checkRequiresApi(node, field, modifierList)) {
                modifierList = containingClass.getModifierList();
                if (modifierList != null) {
                    checkRequiresApi(node, field, modifierList);
                }
            }


            int api = mApiDatabase.getFieldVersion(owner, name);
            if (api != -1) {
                int minSdk = getMinSdk(mContext);
                if (api > minSdk
                        && api > getTargetApi(node)) {
                    // Only look for compile time constants. See JLS 15.28 and JLS 13.4.9.
                    Issue issue = INLINED;
                    if (!(type instanceof PsiPrimitiveType) && !LintUtils.isString(type)) {
                        issue = UNSUPPORTED;

                        // Declaring enum constants are safe; they won't be called on older
                        // platforms.
                        UElement parent = skipParentheses(node.getUastParent());
                        if (parent instanceof USwitchClauseExpression) {
                            List<UExpression> conditions = ((USwitchClauseExpression) parent)
                                    .getCaseValues();
                            //noinspection SuspiciousMethodCalls
                            if (conditions.contains(node)) {
                                return;
                            }
                        }
                    } else if (isBenignConstantUsage(node, name, owner)) {
                        return;
                    }

                    String fqcn = getFqcn(owner) + '#' + name;

                    // For import statements, place the underlines only under the
                    // reference, not the import and static keywords
                    if (node instanceof UImportStatement) {
                        UElement reference = ((UImportStatement) node).getImportReference();
                        if (reference != null) {
                            node = reference;
                        }
                    }

                    if (isSuppressed(mContext, api, node, minSdk)) {
                        return;
                    }

                    String message = String.format(
                            "Field requires API level %1$d (current min is %2$d): `%3$s`",
                            api, Math.max(minSdk, getTargetApi(node)), fqcn);

                    // If the reference is a qualified expression, don't just highlight the
                    // field name itself; include the qualifiers too
                    UElement locationNode = node;
                    //noinspection ConstantConditions
                    while (locationNode.getUastParent() instanceof UQualifiedReferenceExpression
                            // But only include expressions to the left; for example, if we're
                            // trying to highlight the field "OVERLAY" in
                            //     PorterDuff.Mode.OVERLAY.hashCode()
                            // we should *not* include the .hashCode() suffix
                            && ((UQualifiedReferenceExpression)locationNode.getUastParent()).getSelector() == locationNode) {
                        locationNode = locationNode.getUastParent();
                    }

                    Location location = mContext.getLocation(locationNode);
                    mContext.report(issue, node, location, message, apiLevelFix(api));
                }
            }
        }
    }

    /**
     * Returns the first (in DFS order) inheritance chain connecting the two given classes.
     *
     * @param derivedClass the derived class
     * @param baseClass the base class
     * @return The first found inheritance chain connecting the two classes, or {@code null} if the
     *         classes are not related by inheritance. The {@code baseClass} is not included in the
     *         returned inheritance chain, which will be empty if the two classes are the same.
     */
    @Nullable
    private static List<PsiClassType> getInheritanceChain(PsiClassType derivedClass,
            PsiClassType baseClass) {
        if (derivedClass.equals(baseClass)) {
            return Collections.emptyList();
        }
        List<PsiClassType> chain = getInheritanceChain(derivedClass, baseClass, new HashSet<>(), 0);
        if (chain != null) {
            Collections.reverse(chain);
        }
        return chain;
    }

    @Nullable
    private static List<PsiClassType> getInheritanceChain(PsiClassType derivedClass,
            PsiClassType baseClass, HashSet<PsiType> visited, int depth) {
        if (derivedClass.equals(baseClass)) {
            return new ArrayList<>(depth);
        }

        ++depth;
        for (PsiType type : derivedClass.getSuperTypes()) {
            if (visited.add(type) && type instanceof PsiClassType) {
                PsiClassType classType = (PsiClassType)type;
                List<PsiClassType> chain =
                        getInheritanceChain(classType, baseClass, visited, depth);
                if (chain != null) {
                    chain.add(derivedClass);
                    return chain;
                }
            }
        }
        return null;
    }

    private static boolean isSuppressed(
            @NonNull JavaContext context,
            int api,
            @NonNull UElement element,
            int minSdk) {
        if (api <= minSdk) {
            return true;
        }
        int target = getTargetApi(element);
        if (target != -1) {
            if (api <= target) {
                return true;
            }
        }

        LintDriver driver = context.getDriver();
        return driver.isSuppressed(context, UNSUPPORTED, element)
                || driver.isSuppressed(context, INLINED, element)
                || isWithinVersionCheckConditional(element, api)
                || isPrecededByVersionCheckExit(element, api);

    }

    private static boolean isUsingDesugar(@NonNull Context context, @NonNull UElement element) {
        // Desugar runs if the Gradle plugin is 2.4.0 alpha 8 or higher...
        GradleVersion version = context.getProject().getGradleModelVersion();
        if (version == null) {
            return false;
        }
        if (!version.isAtLeast(2, 4, 0, "alpha", 8, true)) {
            return false;
        }

        // ... *and* the language level is at least 1.8
        return LintUtils.getLanguageLevel(element, JDK_1_7).isAtLeast(JDK_1_8);
    }

    public static int getTargetApi(@Nullable UElement scope) {
        while (scope != null) {
            if (scope instanceof PsiModifierListOwner) {
                PsiModifierList modifierList = ((PsiModifierListOwner) scope).getModifierList();
                int targetApi = getTargetApi(modifierList);
                if (targetApi != -1) {
                    return targetApi;
                }
            }
            scope = scope.getUastParent();
            if (scope instanceof PsiFile) {
                break;
            }
        }

        return -1;
    }

    /**
     * Returns the API level for the given AST node if specified with
     * an {@code @TargetApi} annotation.
     *
     * @param modifierList the modifier list to check
     * @return the target API level, or -1 if not specified
     */
    public static int getTargetApi(@Nullable PsiModifierList modifierList) {
        if (modifierList == null) {
            return -1;
        }

        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            String fqcn = annotation.getQualifiedName();
            if (fqcn != null &&
                    (fqcn.equals(FQCN_TARGET_API)
                    || fqcn.equals(REQUIRES_API_ANNOTATION)
                    || fqcn.equals(SDK_SUPPRESS_ANNOTATION)
                    || fqcn.equals(TARGET_API))) { // when missing imports
                PsiAnnotationParameterList parameterList = annotation.getParameterList();
                for (PsiNameValuePair pair : parameterList.getAttributes()) {
                    PsiAnnotationMemberValue v = pair.getValue();
                    if (v instanceof PsiLiteral) {
                        PsiLiteral literal = (PsiLiteral)v;
                        Object value = literal.getValue();
                        if (value instanceof Integer) {
                            return (Integer) value;
                        } else if (value instanceof String) {
                            return codeNameToApi((String) value);
                        }
                    } else if (v instanceof PsiArrayInitializerMemberValue) {
                        PsiArrayInitializerMemberValue mv = (PsiArrayInitializerMemberValue)v;
                        for (PsiAnnotationMemberValue mmv : mv.getInitializers()) {
                            if (mmv instanceof PsiLiteral) {
                                PsiLiteral literal = (PsiLiteral)mmv;
                                Object value = literal.getValue();
                                if (value instanceof Integer) {
                                    return (Integer) value;
                                } else if (value instanceof String) {
                                    return codeNameToApi((String) value);
                                }
                            }
                        }
                    } else if (v instanceof PsiExpression) {
                        // PsiExpression nodes are not present in light classes, so
                        // we can use Java PSI api to get the qualified name
                        if (v instanceof PsiReferenceExpression) {
                            String name = ((PsiReferenceExpression)v).getQualifiedName();
                            return codeNameToApi(name);
                        } else {
                            return codeNameToApi(v.getText());
                        }
                    }
                }
            }
        }

        return -1;
    }

    protected void checkObsoleteSdkVersion(@NonNull JavaContext context,
            @NonNull UElement node) {
        UBinaryExpression binary = UastUtils.getParentOfType(node,
                UBinaryExpression.class, true);
        if (binary != null) {
            int minSdk = getMinSdk(context);
            Boolean isConditional = isVersionCheckConditional(minSdk, binary);
            if (isConditional != null) {
                String message = (isConditional ? "Unnecessary; SDK_INT is always >= " : "Unnecessary; SDK_INT is never < ") + minSdk;
                context.report(OBSOLETE_SDK, binary, context.getLocation(binary),
                        message, fix().data(isConditional));
            }
        }
    }

    @Override
    public void checkFolder(@NonNull ResourceContext context, @NonNull String folderName) {
        int folderVersion = context.getFolderVersion();
        AndroidVersion minSdkVersion = context.getMainProject().getMinSdkVersion();
        if (folderVersion > 1 && folderVersion <= minSdkVersion.getFeatureLevel()) {
            FolderConfiguration folderConfig = FolderConfiguration.getConfigForFolder(folderName);
            assert folderConfig != null : context.file;
            folderConfig.setVersionQualifier(null);
            ResourceFolderType resourceFolderType = context.getResourceFolderType();
            assert resourceFolderType != null : context.file;
            String newFolderName = folderConfig.getFolderName(resourceFolderType);
            context.report(OBSOLETE_SDK, Location.create(context.file),
                    String.format("This folder configuration (`v%1$d`) is unnecessary; "
                            + "`minSdkVersion` is %2$s. Merge all the resources in this folder "
                            + "into `%3$s`.",
                            folderVersion, minSdkVersion.getApiString(), newFolderName),
                    fix().data(context.file, newFolderName, minSdkVersion));
        }
    }

    public static Location getCatchParametersLocation(JavaContext context,
            UCatchClause catchClause) {
        List<UTypeReferenceExpression> types = catchClause.getTypeReferences();
        if (types.isEmpty()) {
            return Location.NONE;
        }

        Location first = context.getLocation(types.get(0));
        if (types.size() < 2) {
            return first;
        }

        Location last = context.getLocation(types.get(types.size() - 1));
        File file = first.getFile();
        Position start = first.getStart();
        Position end = last.getEnd();

        if (start == null) {
            return Location.create(file);
        }

        return Location.create(file, start, end);
    }

    public static boolean isMultiCatchReflectiveOperationException(UCatchClause catchClause) {
        List<PsiType> types = catchClause.getTypes();
        if (types.size() < 2) {
            return false;
        }

        for (PsiType t : types) {
            if(!isSubclassOfReflectiveOperationException(t)) {
                return false;
            }
        }

        return true;
    }

    private static final String REFLECTIVE_OPERATION_EXCEPTION
            = "java.lang.ReflectiveOperationException";

    private static boolean isSubclassOfReflectiveOperationException(PsiType type) {
        for (PsiType t : type.getSuperTypes()) {
            if (REFLECTIVE_OPERATION_EXCEPTION.equals(t.getCanonicalText())) {
                return true;
            }
        }
        return false;
    }
}
