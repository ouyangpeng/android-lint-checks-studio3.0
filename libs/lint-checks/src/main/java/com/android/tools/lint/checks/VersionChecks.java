package com.android.tools.lint.checks;

import static com.android.tools.lint.detector.api.LintUtils.getNextInstruction;
import static com.android.tools.lint.detector.api.LintUtils.skipParentheses;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.SdkVersionInfo;
import com.android.tools.lint.detector.api.ClassContext;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.Collections;
import java.util.List;
import org.jetbrains.uast.UBinaryExpression;
import org.jetbrains.uast.UBlockExpression;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UIfExpression;
import org.jetbrains.uast.ULiteralExpression;
import org.jetbrains.uast.ULocalVariable;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UPolyadicExpression;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.UReturnExpression;
import org.jetbrains.uast.UUnaryExpression;
import org.jetbrains.uast.UastBinaryOperator;
import org.jetbrains.uast.UastContext;
import org.jetbrains.uast.UastPrefixOperator;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.visitor.AbstractUastVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

/**
 * Utility methods for checking whether a given element is surrounded (or preceded!) by
 * an API check using SDK_INT (or other version checking utilities such as BuildCompat#isAtLeastN)
 */
public class VersionChecks {

    private interface ApiLevelLookup {
        int getApiLevel(@NonNull UElement element);
    }

    public static final String SDK_INT = "SDK_INT";
    private static final String ANDROID_OS_BUILD_VERSION = "android/os/Build$VERSION";
    /** SDK int method used by the data binding compiler */
    private static final String GET_BUILD_SDK_INT = "getBuildSdkInt";

    public static int codeNameToApi(@NonNull String text) {
        int dotIndex = text.lastIndexOf('.');
        if (dotIndex != -1) {
            text = text.substring(dotIndex + 1);
        }

        return SdkVersionInfo.getApiByBuildCode(text, true);
    }

    public static boolean isWithinSdkConditional(
            @NonNull ClassContext context,
            @NonNull ClassNode classNode,
            @NonNull MethodNode method,
            @NonNull AbstractInsnNode call,
            int requiredApi) {
        assert requiredApi != -1;

        if (!containsSimpleSdkCheck(method)) {
            return false;
        }

        try {
            // Search in the control graph, from beginning, up to the target call
            // node, to see if it's reachable. The call graph is constructed in a
            // special way: we include all control flow edges, *except* those that
            // are satisfied by a SDK_INT version check (where the operand is a version
            // that is at least as high as the one needed for the given call).
            //
            // If we can reach the call, that means that there is a way this call
            // can be reached on some versions, and lint should flag the call/field lookup.
            //
            //
            // Let's say you have code like this:
            //   if (SDK_INT >= LOLLIPOP) {
            //       // Call
            //       return property.hasAdjacentMapping();
            //   }
            //   ...
            //
            // The compiler will turn this into the following byte code:
            //
            //    0:    getstatic #3; //Field android/os/Build$VERSION.SDK_INT:I
            //    3:    bipush 21
            //    5:    if_icmple 17
            //    8:    aload_1
            //    9:    invokeinterface	#4, 1; //InterfaceMethod
            //                       android/view/ViewDebug$ExportedProperty.hasAdjacentMapping:()Z
            //    14:   ifeq 17
            //    17:   ... code after if loop
            //
            // When the call graph is constructed, for an if branch we're called twice; once
            // where the target is the next instruction (the one taken if byte code check is false)
            // and one to the jump label (the one taken if the byte code condition is true).
            //
            // Notice how at the byte code level, the logic is reversed: the >= instruction
            // is turned into "<" and we jump to the code *after* the if clause; otherwise
            // it will just fall through. Therefore, if we take a byte code branch, that means
            // that the SDK check was *not* satisfied, and conversely, the target call is reachable
            // if we don't take the branch.
            //
            // Therefore, when we build the call graph, we will add call graph nodes for an
            // if check if :
            //   (1) it is some other comparison than <, <= or !=.
            //   (2) if the byte code comparison check is *not* satisfied, this means that the the
            //       SDK check was successful and that the call graph should only include
            //       the jump edge
            //   (3) all other edges are added
            //
            // With a flow control graph like that, we can determine whether a target call
            // is guarded by a given SDK check: that will be the case if we cannot reach
            // the target call in the call graph

            ApiCheckGraph graph = new ApiCheckGraph(requiredApi);
            ControlFlowGraph.create(graph, classNode, method);

            // Note: To debug unit tests, you may want to for example do
            //   ControlFlowGraph.Node callNode = graph.getNode(call);
            //   Set<ControlFlowGraph.Node> highlight = Sets.newHashSet(callNode);
            //   Files.write(graph.toDot(highlight), new File("/tmp/graph.gv"), Charsets.UTF_8);
            // This will generate a graphviz file you can visualize with the "dot" utility
            AbstractInsnNode first = method.instructions.get(0);
            return !graph.isConnected(first, call);
        } catch (AnalyzerException e) {
            context.log(e, null);
        }

        return false;
    }

    private static boolean containsSimpleSdkCheck(@NonNull MethodNode method) {
        // Look for a compiled version of "if (Build.VERSION.SDK_INT op N) {"
        InsnList nodes = method.instructions;
        for (int i = 0, n = nodes.size(); i < n; i++) {
            AbstractInsnNode instruction = nodes.get(i);
            if (isSdkVersionLookup(instruction)) {
                AbstractInsnNode bipush = getNextInstruction(instruction);
                if (bipush != null && bipush.getOpcode() == Opcodes.BIPUSH) {
                    AbstractInsnNode ifNode = getNextInstruction(bipush);
                    if (ifNode != null && ifNode.getType() == AbstractInsnNode.JUMP_INSN) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isSdkVersionLookup(@NonNull AbstractInsnNode instruction) {
        if (instruction.getOpcode() == Opcodes.GETSTATIC) {
            FieldInsnNode fieldNode = (FieldInsnNode) instruction;
            return (SDK_INT.equals(fieldNode.name)
                    && ANDROID_OS_BUILD_VERSION.equals(fieldNode.owner));
        }
        return false;
    }

    public static boolean isPrecededByVersionCheckExit(@NonNull UElement element, int api) {
        //noinspection unchecked
        UExpression currentExpression = UastUtils.getParentOfType(element, UExpression.class,
                true, UMethod.class, UClass.class);

        while (currentExpression != null) {
            VersionCheckWithExitFinder visitor = new VersionCheckWithExitFinder(
                    currentExpression, element, api);
            currentExpression.accept(visitor);

            if (visitor.found()) {
                return true;
            }

            element = currentExpression;
            //noinspection unchecked
            currentExpression = UastUtils.getParentOfType(currentExpression, UExpression.class,
                    true, UMethod.class, UClass.class); // TODO: what about lambdas?
        }

        return false;
    }

    private static class VersionCheckWithExitFinder extends AbstractUastVisitor {

        private final UExpression expression;
        private final UElement endElement;
        private final int api;

        private boolean found = false;
        private boolean done = false;

        public VersionCheckWithExitFinder(UExpression expression, UElement endElement,
                int api) {
            this.expression = expression;

            this.endElement = endElement;
            this.api = api;
        }

        @Override
        public boolean visitElement(UElement node) {
            if (done) {
                return true;
            }

            if (node.equals(endElement)) {
                done = true;
            }

            return done || !expression.equals(node);
        }

        @Override
        public boolean visitIfExpression(UIfExpression ifStatement) {

            if (done) {
                return true;
            }

            UExpression thenBranch = ifStatement.getThenExpression();
            UExpression elseBranch = ifStatement.getElseExpression();

            if (thenBranch != null) {
                Boolean level = isVersionCheckConditional(api, ifStatement.getCondition(), false, null, null);

                //noinspection VariableNotUsedInsideIf
                if (level != null && level) {
                    // See if the body does an immediate return
                    if (isUnconditionalReturn(thenBranch)) {
                        found = true;
                        done = true;
                    }
                }
            }

            if (elseBranch != null) {
                Boolean level = isVersionCheckConditional(api, ifStatement.getCondition(), true, null, null);

                //noinspection VariableNotUsedInsideIf
                if (level != null && level) {
                    if (isUnconditionalReturn(elseBranch)) {
                        found = true;
                        done = true;
                    }
                }
            }

            return true;
        }

        public boolean found() {
            return found;
        }
    }

    private static boolean isUnconditionalReturn(UExpression statement) {
        if (statement instanceof UBlockExpression) {
            List<UExpression> expressions = ((UBlockExpression) statement).getExpressions();
            if (expressions.size() == 1 && expressions.get(0) instanceof UReturnExpression) {
                return true;
            }
        }
        return statement instanceof UReturnExpression;
    }

    public static boolean isWithinVersionCheckConditional(@NonNull UElement element, int api) {
        UElement current = skipParentheses(element.getUastParent());
        UElement prev = element;
        while (current != null) {
            if (current instanceof UIfExpression) {
                UIfExpression ifStatement = (UIfExpression) current;
                UExpression condition = ifStatement.getCondition();
                if (prev != condition) {
                    boolean fromThen = prev == ifStatement.getThenExpression();
                    Boolean ok = isVersionCheckConditional(api, condition, fromThen, prev, null);
                    if (ok != null) {
                        return ok;
                    }
                }
            } else if (current instanceof UPolyadicExpression &&
                    (isAndedWithConditional(current, api, prev) ||
                            isOredWithConditional(current, api, prev))) {
                return true;
            } else if (current instanceof UMethod || current instanceof PsiFile) {
                return false;
            }
            prev = current;
            current = skipParentheses(current.getUastParent());
        }

        return false;
    }

    @Nullable
    private static Boolean isVersionCheckConditional(int api,
            @NonNull UElement element, boolean and, @Nullable UElement prev,
            @Nullable ApiLevelLookup apiLookup) {
        if (element instanceof UPolyadicExpression) {
            if (element instanceof UBinaryExpression) {
                UBinaryExpression binary = (UBinaryExpression) element;
                Boolean ok = isVersionCheckConditional(api, and, binary, apiLookup);
                if (ok != null) {
                    return ok;
                }
            }
            UPolyadicExpression expression = (UPolyadicExpression) element;
            UastBinaryOperator tokenType = expression.getOperator();
            if (and && tokenType == UastBinaryOperator.LOGICAL_AND) {
                if (isAndedWithConditional(element, api, prev)) {
                    return true;
                }

            }  else if (!and && tokenType == UastBinaryOperator.LOGICAL_OR) {
                if (isOredWithConditional(element, api, prev)) {
                    return true;
                }
            }
        } else if (element instanceof UCallExpression) {
            UCallExpression call = (UCallExpression) element;
            return isValidVersionCall(api, and, call);
        } else if (element instanceof UReferenceExpression) {
            // Constant expression for an SDK version check?
            UReferenceExpression refExpression = (UReferenceExpression) element;
            PsiElement resolved = refExpression.resolve();
            if (resolved instanceof PsiField) {
                PsiField field = (PsiField) resolved;
                PsiModifierList modifierList = field.getModifierList();
                if (modifierList != null && modifierList.hasExplicitModifier(PsiModifier.STATIC)) {
                    UastContext context = UastUtils.getUastContext(element);
                    UExpression initializer = context.getInitializerBody(field);
                    if (initializer != null) {
                        Boolean ok = isVersionCheckConditional(api, initializer, and, null, null);
                        if (ok != null) {
                            return ok;
                        }
                    }
                }
            } else if (resolved instanceof PsiMethod &&
                    element instanceof UQualifiedReferenceExpression &&
                    ((UQualifiedReferenceExpression)element).getSelector() instanceof UCallExpression) {
                UCallExpression call = (UCallExpression) ((UQualifiedReferenceExpression)element).getSelector();
                return isValidVersionCall(api, and, call);
            } else if (resolved instanceof PsiMethod &&
                    element instanceof UQualifiedReferenceExpression &&
                    ((UQualifiedReferenceExpression)element).getReceiver() instanceof UReferenceExpression) {
                // Method call via Kotlin property syntax
                return isValidVersionCall(api, and, element, (PsiMethod) resolved);
            }
        } else if (element instanceof UUnaryExpression) {
            UUnaryExpression prefixExpression = (UUnaryExpression) element;
            if (prefixExpression.getOperator() == UastPrefixOperator.LOGICAL_NOT) {
                UExpression operand = prefixExpression.getOperand();
                Boolean ok = isVersionCheckConditional(api, operand, !and, null, null);
                if (ok != null) {
                    return ok;
                }
            }
        }
        return null;
    }

    @Nullable
    private static Boolean isValidVersionCall(int api, boolean and,
            UCallExpression call) {
        PsiMethod method = call.resolve();
        if (method == null) {
            return null;
        }
        return isValidVersionCall(api, and, call, method);
    }

    @Nullable
    private static Boolean isValidVersionCall(int api, boolean and,
            @NonNull UElement call, @NonNull PsiMethod method) {
        String name = method.getName();
        if (name.startsWith("isAtLeast")) {
            PsiClass containingClass = method.getContainingClass();
            if (containingClass != null && "android.support.v4.os.BuildCompat".equals(
                    containingClass.getQualifiedName())) {
                if (name.equals("isAtLeastN")) {
                    return api <= 24;
                } else if (name.equals("isAtLeastNMR1")) {
                    return api <= 25;
                } else if (name.equals("isAtLeastO")) {
                    return api <= 26;
                } else if (name.startsWith("isAtLeastP")) {
                    return api <= 27; // could be higher if there's an OMR1 etc
                }
            }
        }

        // Unconditional version utility method? If so just attempt to call it
        if (!method.hasModifierProperty(PsiModifier.ABSTRACT)) {
            UastContext context = UastUtils.getUastContext(call);
            UExpression body = context.getMethodBody(method);
            List<UExpression> expressions;
            if (body instanceof UBlockExpression) {
                expressions = ((UBlockExpression) body).getExpressions();
            } else {
                expressions = Collections.singletonList(body);
            }

            if (expressions.size() == 1) {
                UExpression statement = expressions.get(0);
                UExpression returnValue = null;
                if (statement instanceof UReturnExpression) {
                    UReturnExpression returnStatement = (UReturnExpression) statement;
                    returnValue = returnStatement.getReturnExpression();
                } else if (statement != null) {
                    // Kotlin: may not have an explicit return statement
                    returnValue = statement;
                }
                if (returnValue != null) {
                    List<UExpression> arguments =
                            call instanceof UCallExpression ?
                                    ((UCallExpression)call).getValueArguments() :
                                    // Property syntax
                                    Collections.emptyList();
                    if (arguments.isEmpty()) {
                        if (returnValue instanceof UPolyadicExpression
                                || returnValue instanceof UCallExpression
                                || returnValue instanceof UQualifiedReferenceExpression) {
                            Boolean isConditional = isVersionCheckConditional(api,
                                    returnValue,
                                    and, null, null);
                            if (isConditional != null) {
                                return isConditional;
                            }
                        }
                    } else if (arguments.size() == 1) {
                        // See if we're passing in a value to the version utility method
                        ApiLevelLookup lookup = arg -> {
                            if (arg instanceof UReferenceExpression) {
                                PsiElement resolved = ((UReferenceExpression) arg)
                                        .resolve();
                                if (resolved instanceof PsiParameter) {
                                    PsiParameter parameter = (PsiParameter) resolved;
                                    PsiParameterList parameterList = PsiTreeUtil.getParentOfType(resolved,
                                                    PsiParameterList.class);
                                    if (parameterList != null) {
                                        int index = parameterList.getParameterIndex(parameter);
                                        if (index != -1 && index < arguments.size()) {
                                            return getApiLevel(arguments.get(index), null);
                                        }
                                    }
                                }
                            }
                            return -1;
                        };
                        Boolean ok = isVersionCheckConditional(api, returnValue,
                                and, null, lookup);
                        if (ok != null) {
                            return ok;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSdkInt(@NonNull PsiElement element) {
        if (element instanceof PsiReferenceExpression) {
            PsiReferenceExpression ref = (PsiReferenceExpression) element;
            if (SDK_INT.equals(ref.getReferenceName())) {
                return true;
            }
            PsiElement resolved = ref.resolve();
            if (resolved instanceof PsiVariable) {
                PsiExpression initializer = ((PsiVariable) resolved).getInitializer();
                if (initializer != null) {
                    return isSdkInt(initializer);
                }
            }
        } else if (element instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression callExpression = (PsiMethodCallExpression) element;
            if (GET_BUILD_SDK_INT.equals(callExpression.getMethodExpression().getReferenceName())) {
                return true;
            } // else look inside the body?
        }

        return false;
    }

    private static boolean isSdkInt(@NonNull UElement element) {
        if (element instanceof UReferenceExpression) {
            UReferenceExpression ref = (UReferenceExpression) element;
            if (SDK_INT.equals(ref.getResolvedName())) {
                return true;
            }
            PsiElement resolved = ref.resolve();
            if (resolved instanceof ULocalVariable) {
                UExpression initializer = ((ULocalVariable) resolved).getUastInitializer();
                if (initializer != null) {
                    return isSdkInt(initializer);
                }
            } else if (resolved instanceof PsiVariable) {
                PsiExpression initializer = ((PsiVariable) resolved).getInitializer();
                if (initializer != null) {
                    return isSdkInt(initializer);
                }
            }
        } else if (element instanceof UCallExpression) {
            UCallExpression callExpression = (UCallExpression) element;
            if (GET_BUILD_SDK_INT.equals(callExpression.getMethodName())) {
                return true;
            } // else look inside the body?
        }

        return false;
    }

    @Nullable
    private static Boolean isVersionCheckConditional(int api,
            boolean fromThen,
            @NonNull UBinaryExpression binary,
            @Nullable ApiLevelLookup apiLevelLookup) {
        UastBinaryOperator tokenType = binary.getOperator();
        if (tokenType == UastBinaryOperator.GREATER || tokenType == UastBinaryOperator.GREATER_OR_EQUALS ||
                tokenType == UastBinaryOperator.LESS_OR_EQUALS || tokenType == UastBinaryOperator.LESS ||
                tokenType == UastBinaryOperator.EQUALS || tokenType == UastBinaryOperator.IDENTITY_EQUALS) {
            UExpression left = binary.getLeftOperand();
            int level;
            UExpression right;
            if (!isSdkInt(left)) {
                right = binary.getRightOperand();
                if (isSdkInt(right)) {
                    fromThen = !fromThen;
                    level = getApiLevel(left, apiLevelLookup);
                } else {
                    return null;
                }
            } else {
                right = binary.getRightOperand();
                level = getApiLevel(right, apiLevelLookup);
            }
            if (level != -1) {
                if (tokenType == UastBinaryOperator.GREATER_OR_EQUALS) {
                    // if (SDK_INT >= ICE_CREAM_SANDWICH) { <call> } else { ... }
                    return level >= api && fromThen;
                }
                else if (tokenType == UastBinaryOperator.GREATER) {
                    // if (SDK_INT > ICE_CREAM_SANDWICH) { <call> } else { ... }
                    return level >= api - 1 && fromThen;
                }
                else if (tokenType == UastBinaryOperator.LESS_OR_EQUALS) {
                    // if (SDK_INT <= ICE_CREAM_SANDWICH) { ... } else { <call> }
                    return level >= api - 1 && !fromThen;
                }
                else if (tokenType == UastBinaryOperator.LESS) {
                    // if (SDK_INT < ICE_CREAM_SANDWICH) { ... } else { <call> }
                    return level >= api && !fromThen;
                }
                else if (tokenType == UastBinaryOperator.EQUALS
                        || tokenType == UastBinaryOperator.IDENTITY_EQUALS) {
                    // if (SDK_INT == ICE_CREAM_SANDWICH) { <call> } else {  }
                    return level >= api && fromThen;
                } else {
                    assert false : tokenType;
                }
            }
        }
        return null;
    }

    private static int getApiLevel(
            @Nullable UExpression element,
            @Nullable ApiLevelLookup apiLevelLookup) {
        int level = -1;
        if (element instanceof UReferenceExpression) {
            UReferenceExpression ref2 = (UReferenceExpression)element;
            String codeName = ref2.getResolvedName();
            if (codeName != null) {
                level = SdkVersionInfo.getApiByBuildCode(codeName, false);
            }
        } else if (element instanceof ULiteralExpression) {
            ULiteralExpression lit = (ULiteralExpression)element;
            Object value = lit.getValue();
            if (value instanceof Integer) {
                level = (Integer) value;
            }
        }
        if (level == -1 && apiLevelLookup != null && element != null) {
            level = apiLevelLookup.getApiLevel(element);
        }
        return level;
    }

    private static boolean isOredWithConditional(@NonNull UElement element, int api,
            @Nullable UElement before) {
        if (element instanceof UBinaryExpression) {
            UBinaryExpression inner = (UBinaryExpression) element;
            if (inner.getOperator() == UastBinaryOperator.LOGICAL_OR) {
                UExpression left = inner.getLeftOperand();

                if (before != left) {
                    Boolean ok = isVersionCheckConditional(api, left, false, null, null);
                    if (ok != null) {
                        return ok;
                    }
                    UExpression right = inner.getRightOperand();
                    ok = isVersionCheckConditional(api, right, false, null, null);
                    if (ok != null) {
                        return ok;
                    }
                }
            }
            Boolean value = isVersionCheckConditional(api, false, inner, null);
            return value != null && value;
        } else if (element instanceof UPolyadicExpression) {
            UPolyadicExpression ppe = (UPolyadicExpression) element;
            if (ppe.getOperator() == UastBinaryOperator.LOGICAL_OR) {
                for (UExpression operand : ppe.getOperands()) {
                    if (operand == before) {
                        break;
                    } else if (isOredWithConditional(operand, api, before)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isAndedWithConditional(@NonNull UElement element, int api,
            @Nullable UElement before) {
        if (element instanceof UBinaryExpression) {
            UBinaryExpression inner = (UBinaryExpression) element;
            if (inner.getOperator() == UastBinaryOperator.LOGICAL_AND) {
                UExpression left = inner.getLeftOperand();
                if (before != left) {
                    Boolean ok = isVersionCheckConditional(api, left, true, null, null);
                    if (ok != null) {
                        return ok;
                    }
                    UExpression right = inner.getRightOperand();
                    ok = isVersionCheckConditional(api, right, true, null, null);
                    if (ok != null) {
                        return ok;
                    }
                }
            }

            Boolean value = isVersionCheckConditional(api, true, inner, null);
            return value != null && value;
        } else if (element instanceof UPolyadicExpression) {
            UPolyadicExpression ppe = (UPolyadicExpression) element;
            if (ppe.getOperator() == UastBinaryOperator.LOGICAL_AND) {
                for (UExpression operand : ppe.getOperands()) {
                    if (operand == before) {
                        break;
                    } else if (isAndedWithConditional(operand, api, before)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // TODO: Merge with the other isVersionCheckConditional
    @Nullable
    public static Boolean isVersionCheckConditional(int api,
            @NonNull UBinaryExpression binary) {
        UastBinaryOperator tokenType = binary.getOperator();
        if (tokenType == UastBinaryOperator.GREATER || tokenType == UastBinaryOperator.GREATER_OR_EQUALS ||
                tokenType == UastBinaryOperator.LESS_OR_EQUALS || tokenType == UastBinaryOperator.LESS ||
                tokenType == UastBinaryOperator.EQUALS || tokenType == UastBinaryOperator.IDENTITY_EQUALS) {
            UExpression left = binary.getLeftOperand();
            if (left instanceof UReferenceExpression) {
                UReferenceExpression ref = (UReferenceExpression) left;
                if (SDK_INT.equals(ref.getResolvedName())) {
                    UExpression right = binary.getRightOperand();
                    int level = -1;
                    if (right instanceof UReferenceExpression) {
                        UReferenceExpression ref2 = (UReferenceExpression) right;
                        String codeName = ref2.getResolvedName();
                        if (codeName == null) {
                            return false;
                        }
                        level = SdkVersionInfo.getApiByBuildCode(codeName, true);
                    } else if (right instanceof ULiteralExpression) {
                        ULiteralExpression lit = (ULiteralExpression)right;
                        Object value = lit.getValue();
                        if (value instanceof Integer) {
                            level = (Integer) value;
                        }
                    }
                    if (level != -1) {
                        if (tokenType == UastBinaryOperator.GREATER_OR_EQUALS && level < api) {
                            // SDK_INT >= ICE_CREAM_SANDWICH
                            return true;
                        } else if (tokenType == UastBinaryOperator.GREATER && level <= api - 1) {
                            // SDK_INT > ICE_CREAM_SANDWICH
                            return true;
                        } else if (tokenType == UastBinaryOperator.LESS_OR_EQUALS && level < api) {
                            return false;
                        } else if (tokenType == UastBinaryOperator.LESS && level <= api) {
                            // SDK_INT < ICE_CREAM_SANDWICH
                            return false;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Control flow graph which skips control flow edges that check
     * a given SDK_VERSION requirement that is not met by a given call
     */
    private static class ApiCheckGraph extends ControlFlowGraph {
        private final int mRequiredApi;

        public ApiCheckGraph(int requiredApi) {
            mRequiredApi = requiredApi;
        }

        @Override
        protected void add(@NonNull AbstractInsnNode from, @NonNull AbstractInsnNode to) {
            if (from.getType() == AbstractInsnNode.JUMP_INSN &&
                    from.getPrevious() != null &&
                    from.getPrevious().getType() == AbstractInsnNode.INT_INSN) {
                IntInsnNode intNode = (IntInsnNode) from.getPrevious();
                if (intNode.getPrevious() != null && isSdkVersionLookup(intNode.getPrevious())) {
                    JumpInsnNode jumpNode = (JumpInsnNode) from;
                    int api = intNode.operand;
                    boolean isJumpEdge = to == jumpNode.label;
                    boolean includeEdge;
                    switch (from.getOpcode()) {
                        case Opcodes.IF_ICMPNE:
                            includeEdge = api < mRequiredApi || isJumpEdge;
                            break;
                        case Opcodes.IF_ICMPLE:
                            includeEdge = api < mRequiredApi - 1 || isJumpEdge;
                            break;
                        case Opcodes.IF_ICMPLT:
                            includeEdge = api < mRequiredApi || isJumpEdge;
                            break;

                        case Opcodes.IF_ICMPGE:
                            includeEdge = api < mRequiredApi || !isJumpEdge;
                            break;
                        case Opcodes.IF_ICMPGT:
                            includeEdge = api < mRequiredApi - 1 || !isJumpEdge;
                            break;
                        default:
                            // unexpected comparison for int API level
                            includeEdge = true;
                    }
                    if (!includeEdge) {
                        return;
                    }
                }
            }

            super.add(from, to);
        }
    }
}
