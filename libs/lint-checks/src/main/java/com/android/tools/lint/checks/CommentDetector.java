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

import static com.android.utils.CharSequences.indexOf;
import static com.android.utils.CharSequences.regionMatches;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.UElementHandler;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector.UastScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;
import java.util.Collections;
import java.util.List;
import org.jetbrains.uast.UComment;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UFile;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Looks for issues in Java comments
 */
public class CommentDetector extends ResourceXmlDetector implements UastScanner {
    private static final String STOPSHIP_COMMENT = "STOPSHIP";

    private static final Implementation IMPLEMENTATION = new Implementation(
            CommentDetector.class,
            Scope.JAVA_AND_RESOURCE_FILES,
            Scope.JAVA_FILE_SCOPE,
            Scope.RESOURCE_FILE_SCOPE);

    /** Looks for hidden code */
    public static final Issue EASTER_EGG = Issue.create(
            "EasterEgg",
            "Code contains easter egg",
            "An \"easter egg\" is code deliberately hidden in the code, both from potential " +
            "users and even from other developers. This lint check looks for code which " +
            "looks like it may be hidden from sight.",
            Category.SECURITY,
            6,
            Severity.WARNING,
            IMPLEMENTATION)
            .setEnabledByDefault(false);

    /** Looks for special comment markers intended to stop shipping the code */
    public static final Issue STOP_SHIP = Issue.create(
            "StopShip",
            "Code contains `STOPSHIP` marker",

            "Using the comment `// STOPSHIP` can be used to flag code that is incomplete but " +
            "checked in. This comment marker can be used to indicate that the code should not " +
            "be shipped until the issue is addressed, and lint will look for these.",
            Category.CORRECTNESS,
            10,
            Severity.WARNING,
            IMPLEMENTATION)
            .setEnabledByDefault(false);

    /** The current AST only passes comment nodes for Javadoc so I need to do manual token scanning
     instead */
    private static final boolean USE_AST = true;

    private static final String ESCAPE_STRING = "\\u002a\\u002f";


    /** Constructs a new {@link CommentDetector} check */
    public CommentDetector() {
    }

    @Nullable
    @Override
    public List<Class<? extends UElement>> getApplicableUastTypes() {
        if (USE_AST) {
            return Collections.singletonList(UFile.class);
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public UElementHandler createUastHandler(@NonNull JavaContext context) {
        //noinspection ConstantConditions
        return USE_AST ? new CommentChecker(context) : null;
    }

    @Override
    public void afterCheckFile(@NonNull Context context) {
        if (USE_AST) {
            return;
        }
        if (context instanceof JavaContext) {
            checkJava((JavaContext) context);
        }
    }

    private static void checkJava(@NonNull JavaContext context) {
        // Lombok does not generate comment nodes for block and line comments, only for
        // javadoc comments!
        CharSequence source = context.getContents();
        if (source == null) {
            return;
        }
        // Process the Java source such that we pass tokens to it

        for (int i = 0, n = source.length() - 1; i < n; i++) {
            char c = source.charAt(i);
            if (c == '\\') {
                i += 1;
            } else if (c == '/') {
                char next = source.charAt(i + 1);
                if (next == '/') {
                    // Line comment
                    int start = i + 2;
                    int end = indexOf(source, '\n', start);
                    if (end == -1) {
                        end = n;
                    }
                    checkComment(context, null, null, null, source, 0, start, end);
                } else if (next == '*') {
                    // Block comment
                    int start = i + 2;
                    int end = indexOf(source, "*/", start);
                    if (end == -1) {
                        end = n;
                    }
                    checkComment(context, null, null, null, source, 0, start, end);
                }
            }
        }
    }

    @Override
    public void visitDocument(@NonNull XmlContext context, @NonNull Document document) {
        checkXml(context, document);
    }

    private static void checkXml(@NonNull XmlContext context, Node node) {
        if (node.getNodeType() == Node.COMMENT_NODE) {
            String source = node.getNodeValue();
            checkComment(null, context, node, null, source, 0, 0, source.length());
        }

        NodeList children = node.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            checkXml(context, children.item(i));
        }
    }

    private static void checkComment(
            @Nullable JavaContext javaContext,
            @Nullable XmlContext xmlContext,
            @Nullable Node xmlNode,
            @Nullable UComment javaNode,
            @NonNull CharSequence source,
            int offset,
            int start,
            int end) {
        assert javaContext != null || xmlContext != null;
        char prev = 0;
        char c;
        Context context = javaContext != null ? javaContext : xmlContext;
        for (int i = start; i < end - 2; i++, prev = c) {
            c = source.charAt(i);
            if (prev == '\\') {
                if (c == 'u' || c == 'U') {
                    if (regionMatches(source,true, i - 1, ESCAPE_STRING,
                            0, ESCAPE_STRING.length())) {
                        String message =
                                "Code might be hidden here; found unicode escape sequence " +
                                "which is interpreted as comment end, compiled code follows";
                        if (javaContext != null) {
                            Location location;
                            if (javaNode != null) {
                                // Use node when possible, since that allows @SuppressLint,
                                // noinspection etc to work to suppress it
                                location = javaContext.getRangeLocation(javaNode, offset + i - 1,
                                        ESCAPE_STRING.length());
                            } else {
                                location = Location.create(context.file, source,
                                        offset + i - 1, offset + i - 1 + ESCAPE_STRING.length());
                            }
                            javaContext.report(EASTER_EGG, javaNode, location, message);
                        } else {
                            assert xmlNode != null;
                            Location location = xmlContext.getLocation(xmlNode, i,
                                    i + ESCAPE_STRING.length());
                            xmlContext.report(EASTER_EGG, xmlNode, location, message);
                        }
                    }
                } else {
                    i++;
                }
            } else if (prev == 'S' && c == 'T' &&
                    regionMatches(source, i - 1, STOPSHIP_COMMENT, 0, STOPSHIP_COMMENT.length())) {
                // TODO: Only flag this issue in release mode??
                String message =
                        "`STOPSHIP` comment found; points to code which must be fixed prior " +
                        "to release";
                if (javaContext != null) {
                    Location location;
                    if (javaNode != null) {
                        // Use node when possible, since that allows @SuppressLint, //noinspection etc
                        // to work to suppress it
                        location = javaContext.getRangeLocation(javaNode, offset + i - 1,
                                STOPSHIP_COMMENT.length());
                    } else {
                        location = Location.create(context.file, source,
                                offset + i - 1, offset + i - 1 + STOPSHIP_COMMENT.length());
                    }
                    LintFix fix = createRemoveStopShipFix();
                    javaContext.report(STOP_SHIP, javaNode, location, message, fix);
                } else {
                    assert xmlNode != null;
                    Location location = xmlContext.getLocation(xmlNode, i,
                            i + STOPSHIP_COMMENT.length());
                    LintFix fix = createRemoveStopShipFix();
                    xmlContext.report(STOP_SHIP, xmlNode, location, message, fix);
                }
            }
        }
    }

    @NonNull
    private static LintFix createRemoveStopShipFix() {
        // TODO: Remove comment if that's all that remains
        return fix()
                .name("Remove STOPSHIP").replace().pattern("(\\s*STOPSHIP)")
                .with("")
                .build();
    }

    private static class CommentChecker extends UElementHandler {
        private final JavaContext mContext;

        public CommentChecker(JavaContext context) {
            mContext = context;
        }

        @Override
        public void visitFile(@NonNull UFile node) {
            for (UComment comment : node.getAllCommentsInFile()) {
                String contents = comment.getText();
                checkComment(mContext, null, null, comment, contents,
                        0, 0, contents.length());
            }
        }
    }
}
