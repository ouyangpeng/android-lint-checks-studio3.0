package com.android.tools.lint.checks;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.UElementHandler;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.ULiteralExpression;

/**
 * Detector that looks for leaked credentials in strings.
 */
public class StringAuthLeakDetector extends Detector implements Detector.UastScanner {

    /** Looks for hidden code */
    public static final Issue AUTH_LEAK = Issue.create(
            "AuthLeak", "Code might contain an auth leak",
            "Strings in java apps can be discovered by decompiling apps, this lint check looks " +
            "for code which looks like it may contain an url with a username and password",
            Category.SECURITY, 6, Severity.WARNING,
            new Implementation(StringAuthLeakDetector.class, Scope.JAVA_FILE_SCOPE));

    @Nullable
    @Override
    public List<Class<? extends UElement>> getApplicableUastTypes() {
        return Collections.singletonList(ULiteralExpression.class);
    }

    @Nullable
    @Override
    public UElementHandler createUastHandler(@NonNull JavaContext context) {
        return new AuthLeakChecker(context);
    }

    private static class AuthLeakChecker extends UElementHandler {
        private static final String LEGAL_CHARS = "([\\w_.!~*\'()%;&=+$,-]+)";      // From RFC 2396
        private static final Pattern AUTH_REGEXP =
                Pattern.compile("([\\w+.-]+)://" + LEGAL_CHARS + ':' + LEGAL_CHARS + '@' +
                        LEGAL_CHARS);

        private final JavaContext context;

        private AuthLeakChecker(JavaContext context) {
            this.context = context;
        }

        @Override
        public void visitLiteralExpression(@NonNull ULiteralExpression node) {
            if (node.getValue() instanceof String) {
                Matcher matcher = AUTH_REGEXP.matcher((String)node.getValue());
                if (matcher.find()) {
                    String password = matcher.group(3);
                    if (password == null || (password.startsWith("%") && password.endsWith("s"))) {
                        return;
                    }
                    Location location = context.getRangeLocation(node, matcher.start() + 1,
                            matcher.end() - matcher.start());
                    context.report(AUTH_LEAK, node, location, "Possible credential leak");
                }
            }
        }
    }
}
