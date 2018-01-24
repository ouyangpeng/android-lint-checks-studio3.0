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
package com.android.tools.lint.client.api;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.blame.SourceFilePosition;
import com.android.manifmerger.Actions;
import com.android.manifmerger.XmlNode;
import com.android.utils.Pair;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class BlameFile {
    public static final BlameFile NONE = new BlameFile(Collections.emptyMap(), null);

    private final Map<String, BlameNode> nodes;
    private final Actions actions;

    BlameFile(@NonNull Map<String, BlameNode> nodes, @Nullable Actions actions) {
        this.nodes = nodes;
        this.actions = actions;
    }

    @Nullable
    private BlameNode findBlameNode(@NonNull Element element) {
        String key = getNodeKey(element);
        BlameNode blameNode = nodes.get(key);

        if (blameNode == null && actions != null) {
            XmlNode.NodeKey nodeKey = XmlNode.NodeKey.fromXml(element);
            ImmutableList<Actions.NodeRecord> records = actions.getNodeRecords(nodeKey);
            for (Actions.NodeRecord record : records) {
                Actions.ActionType actionType = record.getActionType();
                if (actionType == Actions.ActionType.ADDED ||
                        actionType == Actions.ActionType.MERGED) {
                    if (blameNode == null) {
                        blameNode = new BlameNode(key);
                        nodes.put(key, blameNode);
                    }
                    SourceFilePosition actionLocation = record.getActionLocation();
                    File sourceFile = actionLocation.getFile().getSourceFile();
                    if (sourceFile != null) {
                        blameNode.setElementLocation(" from " + sourceFile.getPath());
                    }
                }
            }
            for (XmlNode.NodeName nodeName : actions.getRecordedAttributeNames(nodeKey)) {
                for (Actions.AttributeRecord record : actions
                        .getAttributeRecords(nodeKey, nodeName)) {
                    Actions.ActionType actionType = record.getActionType();
                    if (actionType == Actions.ActionType.ADDED ||
                            actionType == Actions.ActionType.MERGED) {
                        if (blameNode == null) {
                            blameNode = new BlameNode(key);
                            nodes.put(key, blameNode);
                        }
                        SourceFilePosition actionLocation = record.getActionLocation();
                        File sourceFile = actionLocation.getFile().getSourceFile();
                        if (sourceFile != null) {
                            blameNode.setAttributeLocations(nodeName.getLocalName(),
                                    " from " + sourceFile.getPath());
                        }
                    }
                }
            }
        }

        return blameNode;
    }

    @Nullable
    public Pair<File,Node> findSourceNode(@NonNull LintClient client, @NonNull Node node) {
        if (node instanceof Attr) {
            return findSourceAttribute(client, (Attr)node);
        } else if (node instanceof Element) {
            return findSourceElement(client, (Element) node);
        } else {
            return null;
        }
    }

    @Nullable
    public Pair<File,Node> findSourceElement(
            @NonNull LintClient client,
            @NonNull Element element) {
        Pair<File,Node> source = findElementOrAttribute(client, element, null);
        if (source != null && source.getSecond() instanceof Element) {
            return source;
        }

        return null;
    }

    @Nullable
    public Pair<File,Node> findSourceAttribute(@NonNull LintClient client, @NonNull Attr attr) {
        Element element = attr.getOwnerElement();
        Pair<File,Node> source = findElementOrAttribute(client, element, attr);
        if (source != null && source.getSecond() instanceof Attr) {
            return source;
        } else if (source != null && source.getSecond() instanceof Element) {
            Element sourceElement = (Element)source.getSecond();
            if (attr.getPrefix() != null) {
                String namespace = attr.getNamespaceURI();
                String localName = attr.getLocalName();
                Attr sourceAttribute = sourceElement.getAttributeNodeNS(namespace, localName);
                if (sourceAttribute != null) {
                    return Pair.of(source.getFirst(), sourceAttribute);
                }
                return null;
            } else {
                Attr sourceAttribute = sourceElement.getAttributeNode(attr.getName());
                if (sourceAttribute != null) {
                    return Pair.of(source.getFirst(), sourceAttribute);
                }
                return null;
            }
        }

        return null;
    }

    @Nullable
    private Pair<File,Node> findElementOrAttribute(@NonNull LintClient client,
                                        @NonNull Element element,
                                        @Nullable Attr attribute) {
        BlameNode blameNode = findBlameNode(element);
        if (blameNode == null) {
            return null;
        }

        String location = null;
        if (attribute != null) {
            location = blameNode.getAttributeLocation(attribute.getName());
            if (location == null) {
                location = blameNode.getAttributeLocation(attribute.getLocalName());
            }
            // If null use element location instead
        }

        if (location == null) {
            location = blameNode.getElementLocation();
        }
        if (location == null) {
            return null;
        }

        int index = location.indexOf(" from ");
        if (index == -1) {
            return null;
        }
        index += " from ".length();

        if (location.startsWith("[", index)) {
            // Library name included
            index = location.indexOf("] ");
            if (index == -1) {
                return null;
            }
            index += 2;
        }

        int range = location.length();
        while (range > 0) {
            char c = location.charAt(range - 1);
            if (c != ':' && c != '-' && !Character.isDigit(c)) {
                break;
            }
            range--;
        }

        String path = location.substring(index, range);
        File manifest = new File(path);
        if (!manifest.isFile()) {
            return null;
        }

        // We're using lint's XML parser here, not something simple
        // like XmlUtils#parseDocument since we'll typically be queried
        // for locations; it's the main use case for resolving
        // merged nodes back to their sources
        XmlParser parser = client.getXmlParser();
        Document document;
        try {
             document = parser.parseXml(manifest);
             if (document == null) {
                 return null;
             }
        } catch (Throwable ignore) {
            return null;
        }

        String targetKey = blameNode.getKey();

        // We have several options here; one is to use the location ranges
        // listed by the manifest merger. The big downside with that is that
        // it's not very accurate, and only gives line numbers and offsets.
        // We typically want to find the *actual* DOM node, not just its general
        // offset range (such that we can perform additional range math on
        // the source node, such as producing sub ranges (just the name portion
        // etc.)
        //
        // The alternative is to visit the source document and match up the node
        // keys. That's what we're doing below.

        AtomicReference<Element> reference = new AtomicReference<>();
        XmlVisitor.accept(document, new XmlVisitor() {
            @Override
            public boolean visitTag(Element element, String tag) {
                String key = getNodeKey(element);
                if (targetKey.equals(key)) {
                    reference.set(element);
                    return true;
                }

                return false;
            }
        });
        return Pair.of(manifest, reference.get());
    }

    @NonNull
    private static String getNodeKey(Element element) {
        // This unfortunately doesn't work well because in the merged manifest we'll
        // have fully qualified names, e.g.
        //    activity#com.google.myapplication.MainActivity
        // and in the source manifest files we may not, e.g.
        //    activity#.MainActivity
        //
        // (I've actually just patched the key lookup to produce
        // qualified names. If that's not acceptable in the manifest merger,
        // the alternative is to duplicate the naming logic here.)
        return XmlNode.NodeKey.fromXml(element).toString();
    }

    @NonNull
    public static BlameFile parse(File file) throws IOException {
        List<String> lines = Files.readLines(file, Charsets.UTF_8);
        return parse(lines);
    }

    @NonNull
    public static BlameFile parse(Actions mergerActions) {
        Map<String, BlameNode> nodes = Maps.newHashMapWithExpectedSize(80);
        return new BlameFile(nodes, mergerActions);
    }

    @NonNull
    public static BlameFile parse(List<String> lines) {
        Map<String, BlameNode> nodes = Maps.newHashMapWithExpectedSize(80);

        BlameNode last = null;
        String attributeName = null;
        for (String line : lines) {
            if (line.isEmpty()) {
                continue;
            }
            int indent = getIndent(line);
            if (line.startsWith("INJECTED ", indent)) {
                // Ignore injected attributes: coming from Gradle or merger: no corresponding
                // source location (at least not in the manifest, and the merger doesn't model
                // Gradle source code)
                continue;
            }
            if (line.startsWith("ADDED ", indent)
                    || line.startsWith("MERGED ", indent)) {
                if (last != null) {
                    if (indent > 0) {
                        // Indented: it's an attribute
                        assert attributeName != null;
                        last.setAttributeLocations(attributeName, line.trim());
                    } else if (last.getElementLocation() == null) {
                        last.setElementLocation(line.trim());
                    }
                }
                continue;
            } else if (line.startsWith("--")) {
                continue;
            }

            if (indent > 0) {
                attributeName = line.trim();
                continue;
            }

            String key = line.trim();
            BlameNode node = new BlameNode(key);
            nodes.put(key, node);
            attributeName = null;
            last = node;
        }

        return new BlameFile(nodes, null);
    }

    private static int getIndent(@NonNull String line) {
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c != '\t') {
                return i;
            }
        }
        return line.length();
    }

    // TODO: Make tag visitor, node visitor, attribute visitor, etc
    // such that I don't need to visit all attributes when not considered etc
    public abstract static class XmlVisitor {

        public boolean visitTag(Element element, String tag) {
            return false;
        }

        public boolean visitAttribute(Attr attribute) {
            return false;
        }

        public static void accept(Node node, XmlVisitor visitor) {
            visitor.visit(node);
        }

        private boolean visit(Node node) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element tag = (Element) node;
                if (visitTag(tag, tag.getLocalName())) {
                    return true;
                }

                NamedNodeMap attributes = tag.getAttributes();
                for (int i = 0, n = attributes.getLength(); i < n; i++) {
                    Node attr = attributes.item(i);
                    if (visitAttribute((Attr) attr)) {
                        return true;
                    }
                }
            }

            Node child = node.getFirstChild();
            while (child != null) {
                if (visit(child)) {
                    return true;
                }
                child = child.getNextSibling();
            }

            return false;
        }
    }

    /** Represents a node in a manifest merger blame file (for example, in a typical
     * Gradle project, {@code app/build/outputs/logs/manifest-merger-debug-report.txt}. */
    static class BlameNode {
        @Nullable private String from;
        @Nullable private List<Pair<String,String>> attributeLocations;
        private final String key;

        public BlameNode(String key) {
            this.key = key;
        }

        @NonNull
        public String getKey() {
            return key;
        }

        @Nullable
        public String getElementLocation() {
            return from;
        }

        @Nullable
        public String getAttributeLocation(@NonNull String name) {
            if (attributeLocations != null) {
                for (Pair<String,String> pair : attributeLocations) {
                    if (name.equals(pair.getFirst())) {
                        return pair.getSecond();
                    }
                }
            }
            return null;
        }

        public void setElementLocation(@NonNull String location) {
            from = location;
        }

        public void setAttributeLocations(@NonNull String name, @NonNull String location) {
            if (attributeLocations != null) {
                // Locations are always adjacent, so it will always be the last one
                if (name.equals(attributeLocations.get(attributeLocations.size()-1).getFirst())) {
                    attributeLocations.remove(attributeLocations.size()-1);
                }
            } else {
                attributeLocations = Lists.newArrayList();
            }
            //noinspection ConstantConditions
            attributeLocations.add(Pair.of(name, location));
        }
    }

}
