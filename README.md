google 官方源代码地址为：https://android.googlesource.com/platform/tools/base/+/studio-3.0/lint/



参考的官方介绍视频为：https://www.youtube.com/watch?v=p8yX5-lPS6o&index=9&list=PLQ176FUIyIUY6UK1cgVsbdPYA3X5WLam5



Lint官方论坛：https://groups.google.com/forum/#!forum/lint-dev



这个版本的Lint API和原来的版本 有了很大的改版，很多代码都是kotlin来实现的，如果要自定义Lint的话 得好好看看源代码学习学习





https://groups.google.com/forum/#!topic/lint-dev/7nLiXa04baM  一文中有相关的介绍，如下所示：



<https://github.com/JetBrains/uast>

There are quite a few implications for lint. Here's the javadoc I wrote for the new UastScanner interface (when you port from PSI you'll change the implements JavaPsiScanner declaration to UastScanner, and then the various tips described below:

```
public static interface Detector.UastScanner
```

There are several different common patterns for detecting issues:

- Checking calls to a given method. For this see [`getApplicableMethodNames()`](http://../com/android/tools/lint/detector/api/Detector.UastScanner.html#getApplicableMethodNames--) and [`visitMethod(JavaContext, UCallExpression, PsiMethod)`](http://../com/android/tools/lint/detector/api/Detector.UastScanner.html#visitMethod-com.android.tools.lint.detector.api.JavaContext-org.jetbrains.uast.UCallExpression-com.intellij.psi.PsiMethod-)
- Instantiating a given class. For this, see [`getApplicableConstructorTypes()`](http://../com/android/tools/lint/detector/api/Detector.UastScanner.html#getApplicableConstructorTypes--) and [`visitConstructor(JavaContext, UCallExpression, PsiMethod)`](http://../com/android/tools/lint/detector/api/Detector.UastScanner.html#visitConstructor-com.android.tools.lint.detector.api.JavaContext-org.jetbrains.uast.UCallExpression-com.intellij.psi.PsiMethod-)
- Referencing a given constant. For this, see [`getApplicableReferenceNames()`](http://../com/android/tools/lint/detector/api/Detector.UastScanner.html#getApplicableReferenceNames--) and [`visitReference(JavaContext, UReferenceExpression, PsiElement)`](http://../com/android/tools/lint/detector/api/Detector.UastScanner.html#visitReference-com.android.tools.lint.detector.api.JavaContext-org.jetbrains.uast.UReferenceExpression-com.intellij.psi.PsiElement-)
- Extending a given class or implementing a given interface. For this, see [`applicableSuperClasses()`](http://../com/android/tools/lint/detector/api/Detector.UastScanner.html#applicableSuperClasses--) and `visitClass(JavaContext, UClass)`
- More complicated scenarios: perform a general AST traversal with a visitor. In this case, first tell lint which AST node types you're interested in with the [`getApplicableUastTypes()`](http://../com/android/tools/lint/detector/api/Detector.UastScanner.html#getApplicableUastTypes--) method, and then provide a [`UElementHandler`](http://../com/android/tools/lint/client/api/UElementHandler.html) from the [`createUastHandler(JavaContext)`](http://../com/android/tools/lint/detector/api/Detector.UastScanner.html#createUastHandler-com.android.tools.lint.detector.api.JavaContext-) where you override the various applicable handler methods. This is done rather than a general visitor from the root node to avoid having to have every single lint detector (there are hundreds) do a full tree traversal on its own.

[Detector.UastScanner](http://../com/android/tools/lint/detector/api/Detector.UastScanner.html) exposes the UAST API to lint checks. UAST is short for "Universal AST" and is an abstract syntax tree library which abstracts away details about Java versus Kotlin versus other similar languages and lets the client of the library access the AST in a unified way.

UAST isn't actually a full replacement for PSI; it **augments** PSI. Essentially, UAST is usd for the **inside** of methods (e.g. method bodies), and things like field initializers. PSI continues to be used at the outer level: for packages, classes, and methods (declarations and signatures). There are also wrappers around some of these for convenience.

The [Detector.UastScanner](http://../com/android/tools/lint/detector/api/Detector.UastScanner.html) interface reflects this fact. For example, when you indicate that you want to check calls to a method named `foo`, the call site node is a UAST node (in this case, `UCallExpression`, but the called method itself is a `PsiMethod`, since that method might be anywhere (including in a library that we don't have source for, so UAST doesn't make sense.)

## Migrating JavaPsiScanner to UastScanner

 

```
PsiMethod
```

 

 

```
PsiField
```

 

However, the visitor methods have all changed, generally to change to UAST types. For example, the signature [`Detector.JavaPsiScanner.visitMethod(JavaContext, JavaElementVisitor, PsiMethodCallExpression, PsiMethod)`](http://../com/android/tools/lint/detector/api/Detector.JavaPsiScanner.html#visitMethod-com.android.tools.lint.detector.api.JavaContext-com.intellij.psi.JavaElementVisitor-com.intellij.psi.PsiMethodCallExpression-com.intellij.psi.PsiMethod-) should be changed to [`visitMethod(JavaContext, UCallExpression, PsiMethod)`](http://../com/android/tools/lint/detector/api/Detector.UastScanner.html#visitMethod-com.android.tools.lint.detector.api.JavaContext-org.jetbrains.uast.UCallExpression-com.intellij.psi.PsiMethod-).

There are a bunch of new methods on classes like [`JavaContext`](http://../com/android/tools/lint/detector/api/JavaContext.html) which lets you pass in a `UElement` to match the existing `PsiElement` methods.

If you have code which does something specific with PSI classes, the following mapping table in alphabetical order might be helpful, since it lists the corresponding UAST classes.

| PSI                            | UAST                                   |
| ------------------------------ | -------------------------------------- |
| com.intellij.psi.              | org.jetbrains.uast.                    |
| IElementType                   | UastBinaryOperator                     |
| PsiAnnotation                  | UAnnotation                            |
| PsiAnonymousClass              | UAnonymousClass                        |
| PsiArrayAccessExpression       | UArrayAccessExpression                 |
| PsiBinaryExpression            | UBinaryExpression                      |
| PsiCallExpression              | UCallExpression                        |
| PsiCatchSection                | UCatchClause                           |
| PsiClass                       | UClass                                 |
| PsiClassObjectAccessExpression | UClassLiteralExpression                |
| PsiConditionalExpression       | UIfExpression                          |
| PsiDeclarationStatement        | UDeclarationsExpression                |
| PsiDoWhileStatement            | UDoWhileExpression                     |
| PsiElement                     | UElement                               |
| PsiExpression                  | UExpression                            |
| PsiForeachStatement            | UForEachExpression                     |
| PsiIdentifier                  | USimpleNameReferenceExpression         |
| PsiIfStatement                 | UIfExpression                          |
| PsiImportStatement             | UImportStatement                       |
| PsiImportStaticStatement       | UImportStatement                       |
| PsiJavaCodeReferenceElement    | UReferenceExpression                   |
| PsiLiteral                     | ULiteralExpression                     |
| PsiLocalVariable               | ULocalVariable                         |
| PsiMethod                      | UMethod                                |
| PsiMethodCallExpression        | UCallExpression                        |
| PsiNameValuePair               | UNamedExpression                       |
| PsiNewExpression               | UCallExpression                        |
| PsiParameter                   | UParameter                             |
| PsiParenthesizedExpression     | UParenthesizedExpression               |
| PsiPolyadicExpression          | UPolyadicExpression                    |
| PsiPostfixExpression           | UPostfixExpression or UUnaryExpression |
| PsiPrefixExpression            | UPrefixExpression or UUnaryExpression  |
| PsiReference                   | UReferenceExpression                   |
| PsiReference                   | UResolvable                            |
| PsiReferenceExpression         | UReferenceExpression                   |
| PsiReturnStatement             | UReturnExpression                      |
| PsiSuperExpression             | USuperExpression                       |
| PsiSwitchLabelStatement        | USwitchClauseExpression                |
| PsiSwitchStatement             | USwitchExpression                      |
| PsiThisExpression              | UThisExpression                        |
| PsiThrowStatement              | UThrowExpression                       |
| PsiTryStatement                | UTryExpression                         |
| PsiTypeCastExpression          | UBinaryExpressionWithType              |
| PsiWhileStatement              | UWhileExpression                       |

### Parents

 

```
getUastParent
```

 

 

```
getParent
```

 

```
UMethod
```

### Children

 

not

 

 

```
PsiMethod.getBody()
```

```
     UastContext context = UastUtils.getUastContext(element);
     UExpression body = context.getMethodBody(method);
 
```

 

```
PsiField
```

 

```
     UastContext context = UastUtils.getUastContext(element);
     UExpression initializer = context.getInitializerBody(field);
 
```

### Call names

```
 <    call.getMethodExpression().getReferenceName();
 ---
 >    call.getMethodName()
 
```

### Call qualifiers

```
 <    call.getMethodExpression().getQualifierExpression();
 ---
 >    call.getReceiver()
 
```

### Call arguments

```
 <    PsiExpression[] args = call.getArgumentList().getExpressions();
 ---
 >    List args = call.getValueArguments();
 
```

 

```
arg[i]
```

 

```
arg.get(i)
```

 

```
arg[i]
```

### Instanceof

 

```
UastExpressionUtils
```

 

 

```
UastExpressionUtils.isAssignment(UElement)
```

### Android Resources

 

`ResourceReference`

 

```
UExpression
```

 

```
        ResourceReference reference = ResourceReference.get(expression);
        if (reference == null || reference.getType() != ResourceType.STYLEABLE) {
            return;
        }
        ...
 
```

### Binary Expressions

 

```
PsiBinaryExpression
```

 

 

```
UBinaryExpression
```

 

```
UPolyadicExpression
```

 

 

```
UPolyadicExpression
```

 

### Method name changes

The following table maps some common method names and what their corresponding names are in UAST.

| createPsiVisitor      | createUastVisitor      |
| --------------------- | ---------------------- |
| getApplicablePsiTypes | getApplicableUastTypes |
| getApplicablePsiTypes | getApplicableUastTypes |
| getArgumentList       | getValueArguments      |
| getCatchSections      | getCatchClauses        |
| getDeclaredElements   | getDeclarations        |
| getElseBranch         | getElseExpression      |
| getInitializer        | getUastInitializer     |
| getLExpression        | getLeftOperand         |
| getOperationTokenType | getOperator            |
| getOwner              | getUastParent          |
| getParent             | getUastParent          |
| getRExpression        | getRightOperand        |
| getReturnValue        | getReturnExpression    |
| getText               | asSourceString         |
| getThenBranch         | getThenExpression      |
| getType               | getExpressionType      |
| getTypeParameters     | getTypeArguments       |
| resolveMethod         | resolve                |

### Handlers versus visitors

 

`getApplicableUastTypes()`

 

 

not

 

 

`UElementHandler`

 

getApplicableUastTypes()

 

### Migrating JavaScanner to UastScanner

First read the javadoc on how to convert from the older [Detector.JavaScanner](http://../com/android/tools/lint/detector/api/Detector.JavaScanner.html) interface over to [Detector.JavaPsiScanner](http://../com/android/tools/lint/detector/api/Detector.JavaPsiScanner.html). While [Detector.JavaPsiScanner](http://../com/android/tools/lint/detector/api/Detector.JavaPsiScanner.html) is itself deprecated, it's a lot closer to [`Detector.UastScanner`](http://../com/android/tools/lint/detector/api/Detector.UastScanner.html) so a lot of the same concepts apply; then follow the above section. 



