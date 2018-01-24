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

import static com.android.SdkConstants.ANDROID_NS_NAME;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.APPCOMPAT_LIB_ARTIFACT_ID;
import static com.android.SdkConstants.APP_PREFIX;
import static com.android.SdkConstants.ATTR_FONT_PROVIDER_AUTHORITY;
import static com.android.SdkConstants.ATTR_FONT_PROVIDER_CERTS;
import static com.android.SdkConstants.ATTR_FONT_PROVIDER_PACKAGE;
import static com.android.SdkConstants.ATTR_FONT_PROVIDER_QUERY;
import static com.android.SdkConstants.AUTO_URI;
import static com.android.SdkConstants.SUPPORT_LIB_GROUP_ID;
import static com.android.SdkConstants.TAG_FONT;
import static com.android.SdkConstants.TAG_FONT_FAMILY;
import static com.android.ide.common.repository.GradleCoordinate.COMPARE_PLUS_LOWER;
import static com.android.tools.lint.detector.api.LintUtils.coalesce;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.model.AndroidLibrary;
import com.android.builder.model.Dependencies;
import com.android.builder.model.MavenCoordinates;
import com.android.builder.model.Variant;
import com.android.ide.common.fonts.FontDetail;
import com.android.ide.common.fonts.FontFamily;
import com.android.ide.common.fonts.FontLoader;
import com.android.ide.common.fonts.FontProvider;
import com.android.ide.common.fonts.MutableFontDetail;
import com.android.ide.common.fonts.QueryParser;
import com.android.ide.common.repository.GradleCoordinate;
import com.android.resources.ResourceFolderType;
import com.android.sdklib.AndroidVersion;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;
import com.android.utils.XmlUtils;
import com.google.common.base.Joiner;
import com.intellij.openapi.util.text.StringUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class FontDetector extends ResourceXmlDetector {
    private static final Implementation IMPLEMENTATION =
            new Implementation(FontDetector.class, Scope.RESOURCE_FILE_SCOPE);

    public static final Issue FONT_VALIDATION_WARNING =
            Issue.create(
                    "FontValidationWarning",
                    "Validation of font files",
                    "Look for problems in various font files.",
                    Category.CORRECTNESS,
                    9,
                    Severity.WARNING,
                    IMPLEMENTATION).addMoreInfo(
                    "https://developer.android.com/guide/topics/text/downloadable-fonts.html");

    public static final Issue FONT_VALIDATION_ERROR =
            Issue.create(
                    "FontValidationError",
                    "Validation of font files",
                    "Look for problems in various font files.",
                    Category.CORRECTNESS,
                    8,
                    Severity.ERROR,
                    IMPLEMENTATION).addMoreInfo(
                    "https://developer.android.com/guide/topics/text/downloadable-fonts.html");

    public static final GradleCoordinate MIN_APPSUPPORT_VERSION = new GradleCoordinate(
      SUPPORT_LIB_GROUP_ID, APPCOMPAT_LIB_ARTIFACT_ID, "26.0.0-beta1");

    protected FontLoader mFontLoader;

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.FONT;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(TAG_FONT_FAMILY, TAG_FONT);
    }

    @Override
    public void beforeCheckProject(@NonNull Context context) {
        if (mFontLoader == null) {
            mFontLoader = FontLoader.getInstance(context.getClient().getSdkHome());
        }
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        Attr authority = element.getAttributeNodeNS(ANDROID_URI, ATTR_FONT_PROVIDER_AUTHORITY);
        Attr query = element.getAttributeNodeNS(ANDROID_URI, ATTR_FONT_PROVIDER_QUERY);
        Attr androidPackage = element.getAttributeNodeNS(ANDROID_URI, ATTR_FONT_PROVIDER_PACKAGE);
        Attr certs = element.getAttributeNodeNS(ANDROID_URI, ATTR_FONT_PROVIDER_CERTS);

        Attr appAuthority = element.getAttributeNodeNS(AUTO_URI, ATTR_FONT_PROVIDER_AUTHORITY);
        Attr appQuery = element.getAttributeNodeNS(AUTO_URI, ATTR_FONT_PROVIDER_QUERY);
        Attr appPackage = element.getAttributeNodeNS(AUTO_URI, ATTR_FONT_PROVIDER_PACKAGE);
        Attr appCerts = element.getAttributeNodeNS(AUTO_URI, ATTR_FONT_PROVIDER_CERTS);

        Attr firstAndroidAttribute = coalesce(authority, query, androidPackage, certs);
        Attr firstAppAttribute = coalesce(appAuthority, appQuery, appPackage, appCerts);
        List<String> missingAndroidAttributes =
                findMissingAttributes(authority, query, androidPackage, certs);
        List<String> missingAppAttributes =
                findMissingAttributes(appAuthority, appQuery, appPackage, appCerts);

        Element fontTag = XmlUtils.getFirstSubTagByName(element, TAG_FONT);

        AndroidVersion minSdk = context.getMainProject().getMinSdkVersion();
        boolean downloadableFontFile = coalesce(firstAndroidAttribute, firstAppAttribute) != null;

        if (downloadableFontFile) {
            checkSupportLibraryVersion(context, element);
            reportMisplacedFontTag(context, fontTag);
            if (minSdk.getApiLevel() >= 26) {
                reportUnexpectedAttributeNamespace(context, firstAppAttribute, ANDROID_NS_NAME);
            }
            if (1 < minSdk.getFeatureLevel() && minSdk.getFeatureLevel() <= 25) {
                reportUnexpectedAttributeNamespace(context, firstAndroidAttribute, APP_PREFIX);
            }
            FontProvider provider = reportUnknownProvider(context, authority, appAuthority);
            if (provider != null) {
                reportUnknownPackage(context, androidPackage, appPackage, provider);
                reportQueryProblem(context, query, appQuery, provider);
            }
            if (1 < minSdk.getApiLevel() && minSdk.getApiLevel() <= 25) {
                reportMissingAppAttribute(
                        context,
                        firstAppAttribute,
                        missingAppAttributes,
                        AUTO_URI,
                        APP_PREFIX,
                        provider);
            }
            if (minSdk.getFeatureLevel() >= 26) {
                reportMissingAppAttribute(
                        context,
                        firstAndroidAttribute,
                        missingAndroidAttributes,
                        ANDROID_URI,
                        ANDROID_NS_NAME,
                        provider);
            }
        }
    }

    @NonNull
    private static List<String> findMissingAttributes(
            @Nullable Attr authority,
            @Nullable Attr query,
            @Nullable Attr packageName,
            @Nullable Attr certs) {
        if (authority != null && query != null && packageName != null && certs != null) {
            return Collections.emptyList();
        }
        List<String> missing = new ArrayList<>();
        if (authority == null) {
            missing.add(ATTR_FONT_PROVIDER_AUTHORITY);
        }
        if (query == null) {
            missing.add(ATTR_FONT_PROVIDER_QUERY);
        }
        if (packageName == null) {
            missing.add(ATTR_FONT_PROVIDER_PACKAGE);
        }
        if (certs == null) {
            missing.add(ATTR_FONT_PROVIDER_CERTS);
        }
        return missing;
    }

    private static void checkSupportLibraryVersion(@NonNull XmlContext context,
      @NonNull Element element) {
        Variant variant = context.getMainProject().getCurrentVariant();
        if (variant == null) {
            return;
        }
        Dependencies dependencies = variant.getMainArtifact().getDependencies();
        for (AndroidLibrary library : dependencies.getLibraries()) {
            MavenCoordinates rc = library.getResolvedCoordinates();
            if (SUPPORT_LIB_GROUP_ID.equals(rc.getGroupId()) &&
              APPCOMPAT_LIB_ARTIFACT_ID.equals(rc.getArtifactId())) {
                GradleCoordinate version = new GradleCoordinate(
                  SUPPORT_LIB_GROUP_ID, APPCOMPAT_LIB_ARTIFACT_ID, rc.getVersion());
                if (COMPARE_PLUS_LOWER.compare(version, MIN_APPSUPPORT_VERSION) < 0) {
                    String message = "Using version " + version.getRevision()
                            + " of the " + APPCOMPAT_LIB_ARTIFACT_ID
                            + " library. Required version for using downloadable fonts: "
                            + MIN_APPSUPPORT_VERSION.getRevision() + " or higher.";
                    LintFix fix = fix().data(APPCOMPAT_LIB_ARTIFACT_ID);
                    reportError(context, element, message, fix);
                }
            }
        }
    }

    private static void reportMisplacedFontTag(
            @NonNull XmlContext context,
            @Nullable Element fontTag) {
        if (fontTag != null) {
            LintFix fix = fix().replace().with("").build();
            reportError(context, fontTag,
                    "A downloadable font cannot have a `<font>` sub tag", fix);
        }
    }

    private static void reportUnexpectedAttributeNamespace(
            @NonNull XmlContext context,
            @Nullable Attr first,
            @NonNull String namespace) {
        if (first != null) {
            AndroidVersion minSdk = context.getMainProject().getMinSdkVersion();
            String message = String.format(
                    "For `minSdkVersion`=%1$d only `%2$s:` attributes should be used",
                    minSdk.getApiLevel(), namespace);
            LintFix fix = fix().unset(first.getNamespaceURI(), first.getLocalName()).build();
            reportWarning(context, first, message, fix);
        }
    }

    private void reportMissingAppAttribute(
            @NonNull XmlContext context,
            @Nullable Attr firstFontAttribute,
            @NonNull List<String> missingAttributes,
            @NonNull String namespaceUri,
            @NonNull String namespacePrefix,
            @Nullable FontProvider provider) {
        if (firstFontAttribute != null && !missingAttributes.isEmpty()) {
            String message = String.format(
                    "Missing required %1$s: %2$s:%3$s",
                    StringUtil.pluralize("attribute", missingAttributes.size()),
                    namespacePrefix,
                    Joiner.on(", " + namespacePrefix + ":").join(missingAttributes));
            LintFix fix = makeMissingAttributeFix(missingAttributes, namespaceUri, provider);
            reportError(context, firstFontAttribute.getOwnerElement(), message, fix);
        }
    }

    private LintFix makeMissingAttributeFix(
            @NonNull List<String> missingAttributes,
            @NonNull String namespaceUri,
            @Nullable FontProvider provider) {
        if (provider == null) {
            provider = mFontLoader.findOnlyKnownProvider();
        }

        LintFix.GroupBuilder fix = fix().composite();
        for (String missingAttribute : missingAttributes) {
            String value = generateNewValue(missingAttribute, provider);
            if (value == null) {
                fix.add(fix().set().todo(namespaceUri, missingAttribute).build());
            } else {
                fix.add(fix().set(namespaceUri, missingAttribute, value).build());
            }
        }
        return fix.build();
    }

    @Nullable
    private static String generateNewValue(
            @NonNull String missingAttribute, @Nullable FontProvider provider) {
        if (provider == null) {
            return null;
        }
        switch (missingAttribute) {
            case ATTR_FONT_PROVIDER_AUTHORITY:
                return provider.getAuthority();
            case ATTR_FONT_PROVIDER_PACKAGE:
                return provider.getPackageName();
            case ATTR_FONT_PROVIDER_CERTS:
                return "@array/" + provider.getCertificateResourceName();
            default:
                return null;
        }
    }

    @Nullable
    private FontProvider reportUnknownProvider(
            @NonNull XmlContext context,
            @Nullable Attr attrAuthority,
            @Nullable Attr attrAppAuthority) {
        String authority = attrAuthority != null ? attrAuthority.getValue() : null;
        String appAuthority = attrAppAuthority != null ? attrAppAuthority.getValue() : null;
        FontProvider provider = null;
        if (authority != null) {
            provider = reportUnknownProvider(context, attrAuthority, authority);
        } else if (appAuthority != null) {
            provider = reportUnknownProvider(context, attrAppAuthority, appAuthority);
        }
        if (authority != null
                && appAuthority != null
                && !authority.equals(appAuthority)
                && provider != null) {
            LintFix fix = fix().name("Replace with " + authority)
                    .set(AUTO_URI, ATTR_FONT_PROVIDER_AUTHORITY, authority)
                    .build();
            reportError(context, attrAppAuthority, "Unexpected font provider authority", fix);
            provider = null;
        }
        return provider;
    }

    private FontProvider reportUnknownProvider(
            @NonNull XmlContext context, @NonNull Attr attrAuthority, @NonNull String authority) {
        FontProvider provider = mFontLoader.findProvider(authority);
        if (provider != null) {
            return provider;
        }
        LintFix fix = null;
        FontProvider onlyKnownProvider = mFontLoader.findOnlyKnownProvider();
        if (onlyKnownProvider != null) {
            fix = fix().name("Replace with " + onlyKnownProvider.getAuthority())
                    .replace()
                    .text(authority)
                    .with(onlyKnownProvider.getAuthority())
                    .build();
        }
        reportError(context, attrAuthority, "Unknown font provider authority", fix);
        return null;
    }

    private static void reportUnknownPackage(
            @NonNull XmlContext context,
            @Nullable Attr attrAndroidPackage,
            @Nullable Attr attrAppPackage,
            @NonNull FontProvider provider) {
        String androidPackage = attrAndroidPackage != null ? attrAndroidPackage.getValue() : null;
        String appPackage = attrAppPackage != null ? attrAppPackage.getValue() : null;
        if (androidPackage != null && !androidPackage.equals(provider.getPackageName())) {
            reportUnknownPackage(context, attrAndroidPackage, androidPackage, provider);
        } else if (appPackage != null && !appPackage.equals(provider.getPackageName())) {
            reportUnknownPackage(context, attrAppPackage, appPackage, provider);
        }
    }

    private static void reportUnknownPackage(
            @NonNull XmlContext context,
            @NonNull Attr attrPackage,
            @NonNull String packageName,
            @NonNull FontProvider provider) {
        if (provider.getPackageName().equals(packageName)) {
            return;
        }
        LintFix fix = fix().name("Replace with " + provider.getPackageName())
                .replace()
                .text(packageName)
                .with(provider.getPackageName())
                .build();
        reportError(context, attrPackage, "Unexpected font provider package", fix);
    }

    private void reportQueryProblem(
            @NonNull XmlContext context,
            @Nullable Attr androidQueryAttr,
            @Nullable Attr appQueryAttr,
            @NonNull FontProvider provider) {
        String androidQuery = androidQueryAttr != null ? androidQueryAttr.getValue() : null;
        String appQuery = appQueryAttr != null ? appQueryAttr.getValue() : null;
        boolean error = false;
        if (androidQuery != null) {
            error = reportQueryProblem(context, androidQueryAttr, androidQuery, provider);
        } else if (appQuery != null) {
            error = reportQueryProblem(context, appQueryAttr, appQuery, provider);
        }
        if (androidQuery != null && appQuery != null && !androidQuery.equals(appQuery) && !error) {
            LintFix fix = fix().name("Replace with " + androidQuery)
                    .set(AUTO_URI, ATTR_FONT_PROVIDER_QUERY, androidQuery)
                    .build();
            reportError(context, appQueryAttr, "Unexpected query", fix);
        }
    }

    private boolean reportQueryProblem(
            @NonNull XmlContext context,
            @NonNull Attr queryAttr,
            @NonNull String query,
            @NonNull FontProvider provider) {
        if (query.isEmpty()) {
            LintFix fix = fix().set().todo(queryAttr.getNamespaceURI(), queryAttr.getLocalName())
                    .build();
            reportError(context, queryAttr, "Missing provider query", fix);
            return true;
        }
        try {
            QueryParser.DownloadableParseResult result =
                    QueryParser.parseDownloadableFont(
                            provider.getAuthority(), XmlUtils.fromXmlAttributeValue(query));
            if (!mFontLoader.fontsLoaded()) {
                return false;
            }
            boolean errorReported = false;
            for (String fontName : result.getFonts().keySet()) {
                FontFamily family = mFontLoader.findFont(provider, fontName);
                if (family == null) {
                    reportError(context, queryAttr, "Unknown font: " + fontName, null);
                    errorReported = true;
                } else {
                    for (MutableFontDetail detail : result.getFonts().get(fontName)) {
                        FontDetail best = detail.findBestMatch(family.getFonts());
                        if (best != null && detail.match(best) != 0) {
                            LintFix fix = null;
                            if (result.getFonts().size() == 1) {
                                String better = best.generateQuery(detail.getExact());

                                fix = fix().name("Replace with closest font: " + better)
                                        .set(queryAttr.getNamespaceURI(), queryAttr.getLocalName(),
                                                better)
                                        .build();
                            }
                            if (detail.getExact()) {
                                reportError(
                                        context,
                                        queryAttr,
                                        "No exact match found for: " + fontName,
                                        fix);
                            } else {
                                reportWarning(
                                        context,
                                        queryAttr,
                                        "No exact match found for: " + fontName,
                                        fix);
                            }
                            errorReported = true;
                        }
                    }
                }
            }
            return errorReported;
        } catch (QueryParser.FontQueryParserError ex) {
            reportError(context, queryAttr, ex.getMessage(), null);
            return true;
        }
    }

    private static void reportError(
            @NonNull XmlContext context,
            @NonNull Node node,
            @NonNull String message,
            @Nullable LintFix fix) {
        if (!context.isEnabled(FONT_VALIDATION_ERROR)) {
            return;
        }
        context.report(FONT_VALIDATION_ERROR, node, context.getLocation(node), message, fix);
    }

    private static void reportWarning(
            @NonNull XmlContext context,
            @NonNull Node node,
            @NonNull String message,
            @Nullable LintFix fix) {
        if (!context.isEnabled(FONT_VALIDATION_WARNING)) {
            return;
        }
        context.report(FONT_VALIDATION_WARNING, node, context.getLocation(node), message, fix);
    }
}
