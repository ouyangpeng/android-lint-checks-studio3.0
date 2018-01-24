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


import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_TYPE;
import static com.android.SdkConstants.TAG_ITEM;
import static com.android.utils.SdkUtils.getResourceFieldName;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.resources.ResourceUrl;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Location.Handle;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;
import com.android.utils.Pair;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This detector identifies cases where a resource is defined multiple times in the
 * same resource folder
 */
public class DuplicateResourceDetector extends ResourceXmlDetector {

    /** The main issue discovered by this detector */
    @SuppressWarnings("unchecked")
    public static final Issue ISSUE = Issue.create(
            "DuplicateDefinition",
            "Duplicate definitions of resources",

            "You can define a resource multiple times in different resource folders; that's how " +
            "string translations are done, for example. However, defining the same resource " +
            "more than once in the same resource folder is likely an error, for example " +
            "attempting to add a new resource without realizing that the name is already used, " +
            "and so on.",

            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            new Implementation(
                    DuplicateResourceDetector.class,
                    // We should be able to do this incrementally!
                    Scope.ALL_RESOURCES_SCOPE,
                    Scope.RESOURCE_FILE_SCOPE));

    public static final Implementation IMPLEMENTATION_XML = new Implementation(
            DuplicateResourceDetector.class,
            Scope.RESOURCE_FILE_SCOPE);

    /** Wrong resource value type */
    public static final Issue TYPE_MISMATCH = Issue.create(
            "ReferenceType",
            "Incorrect reference types",
            "When you generate a resource alias, the resource you are pointing to must be " +
                    "of the same type as the alias",
            Category.CORRECTNESS,
            8,
            Severity.FATAL,
            IMPLEMENTATION_XML);

    /** Invalid XML escaping */
    public static final Issue STRING_ESCAPING = Issue.create(
            "StringEscaping",
            "Invalid string escapes",

            "Apostrophes (') must always be escaped (with a \\\\), unless they appear " +
            "in a string which is itself escaped in double quotes (\").",

            Category.MESSAGES,
            9,
            Severity.ERROR,
            IMPLEMENTATION_XML);

    private static final String PRODUCT = "product";
    private Map<ResourceType, Set<String>> mTypeMap;
    private Map<ResourceType, List<Pair<String, Location.Handle>>> mLocations;
    private File mParent;

    /** Constructs a new {@link DuplicateResourceDetector} */
    public DuplicateResourceDetector() {
    }

    @Override
    @Nullable
    public Collection<String> getApplicableAttributes() {
        return Collections.singletonList(ATTR_NAME);
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.VALUES;
    }

    @Override
    public void beforeCheckFile(@NonNull Context context) {
        File parent = context.file.getParentFile();
        if (!parent.equals(mParent)) {
            mParent = parent;
            mTypeMap = Maps.newEnumMap(ResourceType.class);
            mLocations = Maps.newEnumMap(ResourceType.class);
        }
    }

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        Element element = attribute.getOwnerElement();

        if (element.hasAttribute(PRODUCT)) {
            return;
        }

        String tag = element.getTagName();
        String typeString = tag;
        if (tag.equals(TAG_ITEM)) {
            typeString = element.getAttribute(ATTR_TYPE);
            if (typeString == null || typeString.isEmpty()) {
                if (element.getParentNode().getNodeName().equals(
                        ResourceType.STYLE.getName()) && isFirstElementChild(element)) {
                    checkUniqueNames(context, (Element) element.getParentNode());
                }
                return;
            }
        }
        ResourceType type = ResourceType.getEnum(typeString);
        if (type == null) {
            return;
        } else if (type == ResourceType.PUBLIC) {
            // We can't easily check public declarations since it's not as simple as
            // just looking up the type attribute and switching the ResourceType to it;
            // that would treat <dimen name="foo"> and <public name="foo" type="dimen">
            // as an actual duplicate name. A simple way to do it would be to change the
            // name, e.g. by prefixing "public-" to it, but that would require restructuring
            // the code a bit, and we'd need to remove it when displaying the conflicts --
            // and most importantly, there really isn't a good reason to do it; a public
            // declaration has no value, so there's no chance of creating conflicting
            // definitions.
            return;
        }

        if (type == ResourceType.ATTR
                && element.getParentNode().getNodeName().equals(
                        ResourceType.DECLARE_STYLEABLE.getName())) {
            if (isFirstElementChild(element)) {
                checkUniqueNames(context, (Element) element.getParentNode());
            }
            return;
        }

        NodeList children = element.getChildNodes();
        int childCount = children.getLength();
        for (int i = 0; i < childCount; i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                String text = child.getNodeValue();

                checkXmlEscapes(context, child, element, text);

                for (int j = 0, length = text.length(); j < length; j++) {
                    char c = text.charAt(j);
                    if (c == '@') {
                        if (!text.regionMatches(false, j + 1, typeString, 0,
                                typeString.length()) && context.isEnabled(TYPE_MISMATCH)) {
                            ResourceUrl url = ResourceUrl.parse(text.trim());
                            if (url != null && url.type != type &&
                                // colors and mipmaps can apparently be used as drawables
                                !(type == ResourceType.DRAWABLE
                                        && (url.type == ResourceType.COLOR
                                            || url.type == ResourceType.MIPMAP))) {
                                LintFix fix = fix().replace().pattern("(@.*/)")
                                        .with("@" + type + "/").build();
                                String message = "Unexpected resource reference type; "
                                        + "expected value of type `@" + type + "/`";
                                context.report(TYPE_MISMATCH, element, context.getLocation(child),
                                        message, fix);
                            }
                        }
                        break;
                    } else if (!Character.isWhitespace(c)) {
                        break;
                    }
                }
                break;
            }
        }

        Set<String> names = mTypeMap.computeIfAbsent(type,
                k -> Sets.newHashSetWithExpectedSize(40));

        String name = attribute.getValue();
        String originalName = name;
        // AAPT will flatten the namespace, turning dots, dashes and colons into _
        name = getResourceFieldName(name);

        if (names.contains(name)) {
            String message = String.format("`%1$s` has already been defined in this folder", name);
            if (!name.equals(originalName)) {
                message += " (`" + name + "` is equivalent to `" + originalName + "`)";
            }
            Location location = context.getLocation(attribute);
            List<Pair<String, Handle>> list = mLocations.get(type);
            for (Pair<String, Handle> pair : list) {
                if (name.equals(pair.getFirst())) {
                    Location secondary = pair.getSecond().resolve();
                    secondary.setMessage("Previously defined here");
                    location.setSecondary(secondary);
                }
            }
            context.report(ISSUE, attribute, location, message);
        } else {
            names.add(name);
            List<Pair<String, Handle>> list = mLocations
                    .computeIfAbsent(type, k -> Lists.newArrayList());
            Location.Handle handle = context.createLocationHandle(attribute);
            list.add(Pair.of(name, handle));
        }
    }

    private static void checkUniqueNames(XmlContext context, Element parent) {
        List<Element> items = LintUtils.getChildren(parent);
        if (items.size() > 1) {
            Set<String> names = Sets.newHashSet();
            for (Element item : items) {
                Attr nameNode = item.getAttributeNode(ATTR_NAME);
                if (nameNode != null) {
                    String name = nameNode.getValue();
                    if (names.contains(name) && context.isEnabled(ISSUE)) {
                        Location location = context.getLocation(nameNode);
                        for (Element prevItem : items) {
                          Attr attribute = item.getAttributeNode(ATTR_NAME);
                          if (attribute != null && name.equals(attribute.getValue())) {
                                assert prevItem != item;
                                Location prev = context.getLocation(prevItem);
                                prev.setMessage("Previously defined here");
                                location.setSecondary(prev);
                                break;
                            }
                        }
                        String message = String.format(
                                "`%1$s` has already been defined in this `<%2$s>`",
                                name, parent.getTagName());
                        context.report(ISSUE, nameNode, location, message);
                    }
                    names.add(name);
                }
            }
        }
    }

    private static boolean isFirstElementChild(Node node) {
        node = node.getPreviousSibling();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                return false;
            }
            node = node.getPreviousSibling();
        }

        return true;
    }

    /**
     * Check the XML for the string format. This is a port of portions of the code
     * in frameworks/base/libs/androidfw/ResourceTypes.cpp (and in particular,
     * the stringToValue and collectString methods)
     */
    private static void checkXmlEscapes(@NonNull XmlContext context, @NonNull Node textNode,
            @NonNull Element element,
            @NonNull String string) {
        int s = 0;
        int len = string.length();

        // First strip leading/trailing whitespace.  Do this before handling
        // escapes, so they can be used to force whitespace into the string.
        while (len > 0 && Character.isWhitespace(string.charAt(s))) {
            s++;
            len--;
        }
        while (len > 0 && Character.isWhitespace(string.charAt(s + len-1))) {
            len--;
        }
        // If the string ends with '\', then we keep the space after it.
        if (len > 0 && string.charAt(s+len-1) == '\\' && len < string.length()) {
            len++;
        }

        char quoted = 0;
        int p = s;
        while (p < (s + len)) {
            while (p < (s + len)) {
                char c = string.charAt(p);
                if (c == '\\') {
                    break;
                }
                if (quoted == 0 && Character.isWhitespace(c)
                        && (c != ' ' || Character.isWhitespace(string.charAt(p + 1)))) {
                    break;
                }
                if (c == '"' && (quoted == 0 || quoted == '"')) {
                    break;
                }
                if (c == '\'' && (quoted == 0 || quoted == '\'')) {
                    // In practice, when people write ' instead of \'
                    // in a string, they are doing it by accident
                    // instead of really meaning to use ' as a quoting
                    // character.  Warn them so they don't lose it.

                    // Use a location range not just from p to p+1 but from p to len
                    // such that the error is more visually prominent/evident in
                    // the source editor.
                    Location location = context.getLocation(textNode, p, len);
                    LintFix fix = fix()
                            .name("Escape Apostrophe").replace().pattern("[^\\\\]?(')")
                            .with("\\'")
                            .build();
                    context.report(STRING_ESCAPING, element, location,
                            "Apostrophe not preceded by \\\\", fix);
                    return;
                }
                p++;
            }
            if (p < (s + len)) {
                char cp = string.charAt(p);
                if ((cp == '"' || cp == '\'')) {
                    if (quoted == 0) {
                        quoted = cp;
                    } else {
                        quoted = 0;
                    }
                    p++;
                } else if (Character.isWhitespace(cp)) {
                    // Space outside of a quote -- consume all spaces and
                    // leave a single plain space char.
                    p++;
                    while (p < (s + len) && Character.isWhitespace(string.charAt(p))) {
                        p++;
                    }
                } else if (cp == '\\') {
                    p++;
                    if (p < (s + len)) {
                        switch (string.charAt(p)) {
                            case 't':
                            case 'n':
                            case '#':
                            case '@':
                            case '?':
                            case '"':
                            case '\'':
                            case '\\':
                                break;
                            case 'u': {
                                int i = 0;
                                while (i < 4 && p + 1 < len) {
                                    p++;
                                    i++;
                                    char h = string.charAt(p);
                                    if ((h < '0' || h > '9')
                                            && (h < 'a' || h > 'f')
                                            && (h < 'A' || h > 'F')) {
                                        Location location = context.getLocation(textNode, p, p + 1);
                                        context.report(STRING_ESCAPING, element, location,
                                                       "Bad character in \\\\u unicode escape sequence");
                                        return;
                                    }
                                }
                            }
                            break;
                            default:
                                // ignore unknown escape chars.
                                break;
                        }
                        p++;
                    }
                }
                len -= (p - s);
                s = p;
            }
        }
    }
}
