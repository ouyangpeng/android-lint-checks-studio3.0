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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ANDROID_VIEW_PKG;
import static com.android.SdkConstants.ANDROID_WEBKIT_PKG;
import static com.android.SdkConstants.ANDROID_WIDGET_PREFIX;
import static com.android.SdkConstants.ATTR_CLASS;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.CLASS_VIEW;
import static com.android.SdkConstants.DOT_XML;
import static com.android.SdkConstants.ID_PREFIX;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static com.android.SdkConstants.VIEW_TAG;
import static com.intellij.pom.java.LanguageLevel.JDK_1_7;
import static com.intellij.pom.java.LanguageLevel.JDK_1_8;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.ide.common.res2.AbstractResourceRepository;
import com.android.ide.common.res2.ResourceFile;
import com.android.ide.common.res2.ResourceItem;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.resources.ResourceUrl;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector.UastScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.ResourceEvaluator;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;
import com.android.utils.CharSequences;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.util.TypeConversionUtil;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.uast.UBinaryExpressionWithType;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.UVariable;
import org.kxml2.io.KXmlParser;
import org.w3c.dom.Attr;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/** Detector for finding inconsistent usage of views and casts
 * <p>
 * TODO: Check findFragmentById
 * <pre>
 * ((ItemListFragment) getSupportFragmentManager()
 *   .findFragmentById(R.id.item_list))
 *   .setActivateOnItemClick(true);
 * </pre>
 * Here we should check the {@code <fragment>} tag pointed to by the id, and
 * check its name or class attributes to make sure the cast is compatible with
 * the named fragment class!
 */
public class ViewTypeDetector extends ResourceXmlDetector implements UastScanner {
    /** Mismatched view types */
    @SuppressWarnings("unchecked")
    public static final Issue WRONG_VIEW_CAST = Issue.create(
            "WrongViewCast",
            "Mismatched view type",
            "Keeps track of the view types associated with ids and if it finds a usage of " +
            "the id in the Java code it ensures that it is treated as the same type.",
            Category.CORRECTNESS,
            9,
            Severity.FATAL,
            new Implementation(
                    ViewTypeDetector.class,
                    EnumSet.of(Scope.ALL_RESOURCE_FILES, Scope.ALL_JAVA_FILES),
                    Scope.JAVA_FILE_SCOPE));

    /** Mismatched view types */
    @SuppressWarnings("unchecked")
    public static final Issue ADD_CAST = Issue.create(
            "FindViewByIdCast",
            "Add Explicit Cast",
            "In Android O, the `findViewById` signature switched to using generics, which means " +
            "that most of the time you can leave out explicit casts and just assign the result " +
            "of the `findViewById` call to variables of specific view classes.\n" +
            "\n" +
            "However, due to language changes between Java 7 and 8, this change may cause code " +
            "to not compile without explicit casts. This lint check looks for these scenarios " +
            "and suggests casts to be added now such that the code will continue to compile " +
            "if the language level is updated to 1.8.",
            Category.CORRECTNESS,
            9,
            Severity.WARNING,
            new Implementation(
                    ViewTypeDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    public static final String FIND_VIEW_BY_ID = "findViewById";

    /** Flag used to do no work if we're running in incremental mode in a .java file without
     * a client supporting project resources */
    private Boolean ignore = null;

    private final Map<String, Object> idToViewTag = new HashMap<>(50);

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.LAYOUT;
    }

    @Override
    public Collection<String> getApplicableAttributes() {
        return Collections.singletonList(ATTR_ID);
    }

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        String view = attribute.getOwnerElement().getTagName();
        String value = attribute.getValue();
        String id = null;
        if (value.startsWith(ID_PREFIX)) {
            id = value.substring(ID_PREFIX.length());
        } else if (value.startsWith(NEW_ID_PREFIX)) {
            id = value.substring(NEW_ID_PREFIX.length());
        } // else: could be @android id

        if (id != null) {
            if (view.equals(VIEW_TAG)) {
                view = attribute.getOwnerElement().getAttribute(ATTR_CLASS);
            }

            Object existing = idToViewTag.get(id);
            if (existing == null) {
                idToViewTag.put(id, view);
            } else if (existing instanceof String) {
                String existingString = (String) existing;
                if (!existingString.equals(view)) {
                    // Convert to list
                    List<String> list = new ArrayList<>(2);
                    list.add((String) existing);
                    list.add(view);
                    idToViewTag.put(id, list);
                }
            } else if (existing instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) existing;
                if (!list.contains(view)) {
                    list.add(view);
                }
            }
        }
    }

    // ---- Implements Detector.JavaScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList(FIND_VIEW_BY_ID);
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @NonNull UCallExpression call,
            @NonNull PsiMethod method) {
        LintClient client = context.getClient();
        if (ignore == Boolean.TRUE) {
            return;
        } else if (ignore == null) {
            ignore = !context.getScope().contains(Scope.ALL_RESOURCE_FILES) &&
                    !client.supportsProjectResources();
            if (ignore) {
                return;
            }
        }
        UElement node = LintUtils.skipParentheses(call);
        if (node == null) {
            return;
        }
        UElement parent = node.getUastParent();

        UElement errorNode;
        PsiClassType castType;

        if (parent instanceof UBinaryExpressionWithType) {
            UBinaryExpressionWithType cast = (UBinaryExpressionWithType) parent;
            PsiType type = cast.getType();
            if (!(type instanceof PsiClassType)) {
                return;
            }
            castType = (PsiClassType) type;
            errorNode = cast;
        } else if (parent instanceof UExpression) {
            if (parent instanceof UCallExpression) {
                UCallExpression c = (UCallExpression) parent;
                checkMissingCast(context, call, c);
                return;
            }

            // Implicit cast?
            UExpression variable = (UExpression) parent;
            PsiType type = variable.getExpressionType();
            if (!(type instanceof PsiClassType)) {
                return;
            }
            castType = (PsiClassType) type;
            errorNode = parent;
        } else if (parent instanceof UVariable) {
            // Implicit cast?
            UVariable variable = (UVariable) parent;
            PsiType type = variable.getType();
            if (!(type instanceof PsiClassType)) {
                return;
            }
            castType = (PsiClassType) type;
            errorNode = parent;
        } else {
            return;
        }

        String castTypeClass = castType.getCanonicalText();
        if (castTypeClass.equals(CLASS_VIEW) || castTypeClass.equals("kotlin.Unit")) {
            return;
        }

        List<UExpression> args = call.getValueArguments();
        if (args.size() == 1) {
            UExpression first = args.get(0);
            ResourceUrl resourceUrl = ResourceEvaluator.getResource(context.getEvaluator(),
                    first);
            if (resourceUrl != null && resourceUrl.type == ResourceType.ID &&
                    !resourceUrl.framework) {
                String id = resourceUrl.name;

                if (client.supportsProjectResources()) {
                    AbstractResourceRepository resources = client
                            .getResourceRepository(context.getMainProject(), true, false);
                    if (resources == null) {
                        return;
                    }

                    List<ResourceItem> items = resources.getResourceItem(ResourceType.ID,
                            id);
                    if (items != null && !items.isEmpty()) {
                        Set<String> compatible = Sets.newHashSet();
                        for (ResourceItem item : items) {
                            Collection<String> tags = getViewTags(context, item);
                            if (tags != null) {
                               compatible.addAll(tags);
                            }
                        }
                        if (!compatible.isEmpty()) {
                            ArrayList<String> layoutTypes = Lists.newArrayList(compatible);
                            checkCompatible(context, castType, castTypeClass, null,
                                    layoutTypes, errorNode, first, items);
                        }
                    }
                } else {
                    Object types = idToViewTag.get(id);
                    if (types instanceof String) {
                        String layoutType = (String) types;
                        checkCompatible(context, castType, castTypeClass, layoutType, null,
                                errorNode, first, null);
                    } else if (types instanceof List<?>) {
                        @SuppressWarnings("unchecked")
                        List<String> layoutTypes = (List<String>) types;
                        checkCompatible(context, castType, castTypeClass, null, layoutTypes,
                                errorNode, first, null);
                    }
                }
            }
        }
    }

    private static void checkMissingCast(
          @NonNull JavaContext context,
          @NonNull UCallExpression findViewByIdCall,
          @NonNull UCallExpression surroundingCall) {
        // This issue only applies in Java, not Kotlin etc - and for language level 1.8
        LanguageLevel languageLevel = LintUtils.getLanguageLevel(surroundingCall, JDK_1_7);
        if (languageLevel.isLessThan(JDK_1_8)) {
            return;
        }

        UElement uastParent = surroundingCall.getUastParent();
        if (!(uastParent instanceof UQualifiedReferenceExpression)) {
            return;
        }

        List<UExpression> valueArguments = surroundingCall.getValueArguments();
        int parameterIndex = -1;
        for (int i = 0, n = valueArguments.size(); i < n; i++) {
            if (findViewByIdCall.equals(valueArguments.get(i))) {
                parameterIndex = i;
            }
        }
        if (parameterIndex == -1) {
            return;
        }

        PsiMethod resolvedMethod = surroundingCall.resolve();
        if (resolvedMethod == null) {
            return;
        }
        PsiParameter[] parameters = resolvedMethod.getParameterList().getParameters();
        if (parameterIndex >= parameters.length) {
            return;
        }

        PsiType parameterType = parameters[parameterIndex].getType();

        if (!(parameterType instanceof PsiClassType)) {
            return;
        }

        PsiClass parameterTypeClass = ((PsiClassType)parameterType).resolve();
        if (!(parameterTypeClass instanceof PsiTypeParameter)) {
            return;
        }

        PsiType erasure = TypeConversionUtil.erasure(parameterType);
        if (erasure == null || erasure.getCanonicalText().equals(CLASS_VIEW)) {
            return;
        }

        LintFix fix = fix().replace()
          .name("Add cast")
          .text(FIND_VIEW_BY_ID)
          .shortenNames()
          .reformat(true)
          .with("(android.view.View)" + FIND_VIEW_BY_ID).build();

        context.report(ADD_CAST, context.getLocation(findViewByIdCall),
                "Add explicit cast here; won't compile with Java language level 1.8 "
                        + "without it", fix);
    }

    @Nullable
    protected Collection<String> getViewTags(
            @NonNull Context context,
            @NonNull ResourceItem item) {
        // Check view tag in this file. Can I do it cheaply? Try with
        // an XML pull parser. Or DOM if we have multiple resources looked
        // up?
        ResourceFile source = item.getSource();
        if (source != null) {
            File file = source.getFile();
            Multimap<String,String> map = getIdToTagsIn(context, file);
            if (map != null) {
                return map.get(item.getName());
            }
        }

        return null;
    }

    private Map<File, Multimap<String, String>> fileIdMap;

    @Nullable
    private Multimap<String, String> getIdToTagsIn(@NonNull Context context, @NonNull File file) {
        if (!file.getPath().endsWith(DOT_XML)) {
            return null;
        }
        if (fileIdMap == null) {
            fileIdMap = Maps.newHashMap();
        }
        Multimap<String, String> map = fileIdMap.get(file);
        if (map == null) {
            map = ArrayListMultimap.create();
            fileIdMap.put(file, map);

            CharSequence contents = context.getClient().readFile(file);
            try {
                addTags(CharSequences.getReader(contents, true), map);
            } catch (XmlPullParserException | IOException ignore) {
                // Users might be editing these files in the IDE; don't flag
            }
        }
        return map;
    }

    @VisibleForTesting
    private static void addTags(@NonNull Reader reader, Multimap<String, String> map)
            throws XmlPullParserException, IOException {
        KXmlParser parser = new KXmlParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(reader);

        while (true) {
            int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                String id = parser.getAttributeValue(ANDROID_URI, ATTR_ID);
                if (id != null && !id.isEmpty()) {
                    id = LintUtils.stripIdPrefix(id);
                    String tag = parser.getName();
                    if (!map.containsEntry(id, tag)) {
                        map.put(id, tag);
                    }
                }
            } else if (event == XmlPullParser.END_DOCUMENT) {
                return;
            }
        }
    }

    /** Check if the view and cast type are compatible */
    private void checkCompatible(
            @NonNull JavaContext context,
            @NonNull PsiClassType castType,
            @NonNull String castTypeClass,
            @Nullable String tag,
            @Nullable List<String> tags,
            @NonNull UElement node,
            @NonNull UExpression resourceReference,
            @Nullable List<ResourceItem> items) {
        assert tag == null || tags == null : tag + tags; // Should only specify one or the other

        // Common case: they match: quickly check for this and fail if not
        if (castTypeClass.equals(tag) ||
                tags != null && tags.contains(castTypeClass)) {
            return;
        }


        PsiClass castClass = castType.resolve();

        boolean compatible = true;
        if (tag != null) {
            if (!tag.equals(castTypeClass)
                    && !context.getSdkInfo().isSubViewOf(castTypeClass, tag)) {
                compatible = false;
            }
        } else {
            compatible = false;
            assert tags != null;
            for (String type : tags) {
                if (type.equals(castTypeClass)
                        || context.getSdkInfo().isSubViewOf(castTypeClass, type)) {
                    compatible = true;
                    break;
                }
            }
        }

        // Use real classes to handle checks
        if (castClass != null && !compatible) {
            if (tag != null) {
                if (isCompatible(context, castClass, tag)) {
                    return;
                }
            } else {
                for (String t : tags) {
                    if (isCompatible(context, castClass, t)) {
                        return;
                    }
                }
            }
        }

        if (!compatible) {
            String sampleLayout = null;
            if (tag == null) {
                tag = Joiner.on("|").join(tags);
            }

            if (items != null && (tags == null || tags.size() == 1)) {
                for (ResourceItem item : items) {
                    Collection<String> t = getViewTags(context, item);
                    if (t != null && t.contains(tag)) {
                        ResourceFile source = item.getSource();
                        if (source != null) {
                            File file = source.getFile();
                            if (source.getFolderConfiguration().isDefault()) {
                                sampleLayout = file.getName();
                            } else {
                                sampleLayout = file.getParentFile().getName()
                                        + "/" + file.getName();
                            }
                            break;
                        }
                    }
                }
            }

            String incompatibleTag = castTypeClass.substring(
                    castTypeClass.lastIndexOf('.') + 1);

            String message;
            Location location;
            if (!(node instanceof UBinaryExpressionWithType)) {
                if (node instanceof UVariable && ((UVariable)node).getTypeReference() != null) {
                    //noinspection ConstantConditions
                    location = context.getLocation(((UVariable)node).getTypeReference());
                    location.setSecondary(createSecondary(context, tag, resourceReference,
                            sampleLayout));
                } else {
                    location = context.getLocation(node);
                }
                message = String.format(
                        "Unexpected implicit cast to `%1$s`: layout tag was `%2$s`",
                        incompatibleTag, tag);

            } else {
                location = context.getLocation(node);
                if (sampleLayout != null) {
                    location.setSecondary(createSecondary(context, tag, resourceReference,
                            sampleLayout));
                }
                message = String.format(
                        "Unexpected cast to `%1$s`: layout tag was `%2$s`",
                        incompatibleTag, tag);

            }
            context.report(WRONG_VIEW_CAST, node, location, message);
        }
    }

    @NonNull
    private static Location createSecondary(@NonNull JavaContext context, @NonNull String tag,
            @NonNull UExpression resourceReference, @Nullable String sampleLayout) {
        Location secondary = context.getLocation(resourceReference);
        if (sampleLayout != null) {
            String article = tag.indexOf('.') == -1
                    && tag.indexOf('|') == -1
                    // We don't have widgets right now which start with a silent consonant
                    && StringUtil.isVowel(Character.toLowerCase(tag.charAt(0))) ? "an" : "a";
            secondary.setMessage(String.format("Id bound to %1$s `%2$s` in `%3$s`",
                    article, tag, sampleLayout));
        }
        return secondary;
    }

    private static boolean isCompatible(
            @NonNull JavaContext context,
            @NonNull PsiClass castClass,
            @NonNull String tag) {
        PsiClass cls = null;
        if (tag.indexOf('.') == -1) {
            for (String prefix : new String[]{
                    // See framework's PhoneLayoutInflater: these are the prefixes
                    // that don't need fully qualified names in layouts
                    ANDROID_WIDGET_PREFIX,
                    ANDROID_VIEW_PKG,
                    ANDROID_WEBKIT_PKG}) {
                cls = context.getEvaluator().findClass(prefix + tag);
                //noinspection VariableNotUsedInsideIf
                if (cls != null) {
                    break;
                }
            }
        } else {
            cls = context.getEvaluator().findClass(tag);
        }

        if (cls != null) {
            return cls.isInheritor(castClass, true);
        }

        // Didn't find class - just assume it's compatible since we don't want false positives
        return true;
    }
}
