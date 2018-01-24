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

package com.android.tools.lint.client.api;

import static com.android.SdkConstants.CONSTRUCTOR_NAME;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.model.AndroidLibrary;
import com.android.builder.model.Dependencies;
import com.android.builder.model.JavaLibrary;
import com.android.builder.model.Library;
import com.android.builder.model.MavenCoordinates;
import com.android.tools.lint.detector.api.ClassContext;
import com.google.common.collect.Maps;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationOwner;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEllipsisType;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeVisitor;
import com.intellij.psi.PsiWildcardType;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import org.jetbrains.uast.UAnnotated;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UMethod;

@SuppressWarnings("MethodMayBeStatic") // Some of these methods may be overridden by LintClients
public abstract class JavaEvaluator {
    public abstract boolean extendsClass(
            @Nullable PsiClass cls,
            @NonNull String className,
            boolean strict);

    public abstract boolean implementsInterface(
            @NonNull PsiClass cls,
            @NonNull String interfaceName,
            boolean strict);

    public boolean isMemberInSubClassOf(
            @NonNull PsiMember method,
            @NonNull String className,
            boolean strict) {
        PsiClass containingClass = method.getContainingClass();
        return containingClass != null && extendsClass(containingClass, className, strict);
    }

    public boolean isMemberInClass(
            @Nullable PsiMember method,
            @NonNull String className) {
        if (method == null) {
            return false;
        }
        PsiClass containingClass = method.getContainingClass();
        return containingClass != null && className.equals(containingClass.getQualifiedName());
    }

    public int getParameterCount(@NonNull PsiMethod method) {
        return method.getParameterList().getParametersCount();
    }

    /**
     * Checks whether the class extends a super class or implements a given interface. Like calling
     * both {@link #extendsClass(PsiClass, String, boolean)} and {@link
     * #implementsInterface(PsiClass, String, boolean)}.
     */
    public boolean inheritsFrom(
            @NonNull PsiClass cls,
            @NonNull String className,
            boolean strict) {
        return extendsClass(cls, className, strict) || implementsInterface(cls, className, strict);
    }

    /**
     * Returns true if the given method (which is typically looked up by resolving a method call) is
     * either a method in the exact given class, or if {@code allowInherit} is true, a method in a
     * class possibly extending the given class, and if the parameter types are the exact types
     * specified.
     *
     * @param method        the method in question
     * @param className     the class name the method should be defined in or inherit from (or
     *                      if null, allow any class)
     * @param allowInherit  whether we allow checking for inheritance
     * @param argumentTypes the names of the types of the parameters
     * @return true if this method is defined in the given class and with the given parameters
     */
    public boolean methodMatches(
            @NonNull PsiMethod method,
            @Nullable String className,
            boolean allowInherit,
            @NonNull String... argumentTypes) {
        if (className != null && allowInherit) {
            if (!isMemberInSubClassOf(method, className, false)) {
                return false;
            }
        }

        return parametersMatch(method, argumentTypes);
    }

    /**
     * Returns true if the given method's parameters are the exact types specified.
     *
     * @param method        the method in question
     * @param argumentTypes the names of the types of the parameters
     * @return true if this method is defined in the given class and with the given parameters
     */
    public boolean parametersMatch(
            @NonNull PsiMethod method,
            @NonNull String... argumentTypes) {
        PsiParameterList parameterList = method.getParameterList();
        if (parameterList.getParametersCount() != argumentTypes.length) {
            return false;
        }
        PsiParameter[] parameters = parameterList.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            PsiType type = parameters[i].getType();
            if (!type.getCanonicalText().equals(argumentTypes[i])) {
                return false;
            }
        }

        return true;
    }

    /** Returns true if the given type matches the given fully qualified type name */
    public boolean parameterHasType(
            @Nullable PsiMethod method,
            int parameterIndex,
            @NonNull String typeName) {
        if (method == null) {
            return false;
        }
        PsiParameterList parameterList = method.getParameterList();
        return parameterList.getParametersCount() > parameterIndex
                && typeMatches(parameterList.getParameters()[parameterIndex].getType(), typeName);
    }

    /** Returns true if the given type matches the given fully qualified type name */
    public boolean typeMatches(
            @Nullable PsiType type,
            @NonNull String typeName) {
        return type != null && type.getCanonicalText().equals(typeName);

    }

    @Nullable
    public PsiElement resolve(@NonNull PsiElement element) {
        if (element instanceof PsiReference) {
            return ((PsiReference)element).resolve();
        } else if (element instanceof PsiMethodCallExpression) {
            PsiElement resolved = ((PsiMethodCallExpression) element).resolveMethod();
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    public boolean isPublic(@Nullable PsiModifierListOwner owner) {
        if (owner != null) {
            PsiModifierList modifierList = owner.getModifierList();
            return modifierList != null && modifierList.hasModifierProperty(PsiModifier.PUBLIC);
        }
        return false;
    }

    public boolean isStatic(@Nullable PsiModifierListOwner owner) {
        if (owner != null) {
            PsiModifierList modifierList = owner.getModifierList();
            return modifierList != null && modifierList.hasModifierProperty(PsiModifier.STATIC);
        }
        return false;
    }

    public boolean isPrivate(@Nullable PsiModifierListOwner owner) {
        if (owner != null) {
            PsiModifierList modifierList = owner.getModifierList();
            return modifierList != null && modifierList.hasModifierProperty(PsiModifier.PRIVATE);
        }
        return false;
    }

    public boolean isAbstract(@Nullable PsiModifierListOwner owner) {
        if (owner != null) {
            PsiModifierList modifierList = owner.getModifierList();
            return modifierList != null && modifierList.hasModifierProperty(PsiModifier.ABSTRACT);
        }
        return false;
    }

    public boolean isFinal(@Nullable PsiModifierListOwner owner) {
        if (owner != null) {
            PsiModifierList modifierList = owner.getModifierList();
            return modifierList != null && modifierList.hasModifierProperty(PsiModifier.FINAL);
        }
        return false;
    }

    @Nullable
    public PsiMethod getSuperMethod(@Nullable PsiMethod method) {
        if (method == null) {
            return null;
        }
        final PsiMethod[] superMethods = method.findSuperMethods();
        if (superMethods.length > 0) {
            return superMethods[0];
        }
        return null;
    }

    @Nullable
    public String getInternalName(@NonNull PsiClass psiClass) {
        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null) {
            qualifiedName = psiClass.getName();
            if (qualifiedName == null) {
                assert psiClass instanceof PsiAnonymousClass;
                //noinspection ConstantConditions
                return getInternalName(psiClass.getContainingClass());
            }
        }
        return ClassContext.getInternalName(qualifiedName);
    }

    @Nullable
    public String getInternalName(@NonNull PsiClassType psiClassType) {
        return ClassContext.getInternalName(psiClassType.getCanonicalText());
    }

    /**
     * Computes the internal JVM description of the given method. This is in the same
     * format as the ASM desc fields for methods; meaning that a method named foo which for example takes an
     * int and a String and returns a void will have description {@code foo(ILjava/lang/String;):V}.
     *
     * @param method the method to look up the description for
     * @param includeName whether the name should be included
     * @param includeReturn whether the return type should be included
     * @return the internal JVM description for this method
     */
    @Nullable
    public String getInternalDescription(@NonNull PsiMethod method, boolean includeName,
            boolean includeReturn) {
        assert !includeName; // not yet tested
        assert !includeReturn; // not yet tested

        StringBuilder signature = new StringBuilder();

        if (includeName) {
            if (method.isConstructor()) {
                final PsiClass declaringClass = method.getContainingClass();
                if (declaringClass != null) {
                    final PsiClass outerClass = declaringClass.getContainingClass();
                    if (outerClass != null) {
                        // declaring class is an inner class
                        if (!declaringClass.hasModifierProperty(PsiModifier.STATIC)) {
                            if (!appendJvmTypeName(signature, outerClass)) {
                                return null;
                            }
                        }
                    }
                }
                signature.append(CONSTRUCTOR_NAME);
            } else {
                signature.append(method.getName());
            }
        }

        signature.append('(');

        for (PsiParameter psiParameter : method.getParameterList().getParameters()) {
            if (!appendJvmSignature(signature, psiParameter.getType())) {
                return null;
            }
        }
        signature.append(')');
        if (includeReturn) {
            if (!method.isConstructor()) {
                if (!appendJvmSignature(signature, method.getReturnType())) {
                    return null;
                }
            }
            else {
                signature.append('V');
            }
        }
        return signature.toString();
    }

    private boolean appendJvmTypeName(@NonNull StringBuilder signature, @NonNull PsiClass outerClass) {
        String className = getInternalName(outerClass);
        if (className == null) {
            return false;
        }
        signature.append('L').append(className.replace('.', '/')).append(';');
        return true;
    }

    private boolean appendJvmSignature(@NonNull StringBuilder buffer, @Nullable PsiType type) {
        if (type == null) {
            return false;
        }
        final PsiType psiType = erasure(type);
        if (psiType instanceof PsiArrayType) {
            buffer.append('[');
            appendJvmSignature(buffer, ((PsiArrayType)psiType).getComponentType());
        }
        else if (psiType instanceof PsiClassType) {
            PsiClass resolved = ((PsiClassType)psiType).resolve();
            if (resolved == null) {
                return false;
            }
            if (!appendJvmTypeName(buffer, resolved)) {
                return false;
            }
        }
        else if (psiType instanceof PsiPrimitiveType) {
            buffer.append(getPrimitiveSignature(psiType.getCanonicalText()));
        }
        else {
            return false;
        }
        return true;
    }

    public boolean areSignaturesEqual(@NonNull PsiMethod method1, @NonNull PsiMethod method2) {
        PsiParameterList parameterList1 = method1.getParameterList();
        PsiParameterList parameterList2 = method2.getParameterList();
        if (parameterList1.getParametersCount() != parameterList2.getParametersCount()) {
            return false;
        }

        PsiParameter[] parameters1 = parameterList1.getParameters();
        PsiParameter[] parameters2 = parameterList2.getParameters();

        for (int i = 0, n = parameters1.length; i < n; i++) {
            PsiParameter parameter1 = parameters1[i];
            PsiParameter parameter2 = parameters2[i];
            PsiType type1 = parameter1.getType();
            PsiType type2 = parameter2.getType();
            if (!type1.equals(type2)) {
                type1 = erasure(parameter1.getType());
                type2 = erasure(parameter2.getType());
                if (!type1.equals(type2)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Nullable
    public static PsiType erasure(@Nullable final PsiType type) {
        if (type == null) {
            return null;
        }
        return type.accept(new PsiTypeVisitor<PsiType>() {
            @Nullable
            @Override
            public PsiType visitType(PsiType type) {
                return type;
            }

            @Override
            public PsiType visitClassType(PsiClassType classType) {
                return classType.rawType();
            }

            @Override
            public PsiType visitWildcardType(PsiWildcardType wildcardType) {
                return wildcardType;
            }

            @Override
            public PsiType visitPrimitiveType(PsiPrimitiveType primitiveType) {
                return primitiveType;
            }

            @Override
            public PsiType visitEllipsisType(PsiEllipsisType ellipsisType) {
                final PsiType componentType = ellipsisType.getComponentType();
                final PsiType newComponentType = componentType.accept(this);
                if (newComponentType == componentType) return ellipsisType;
                return newComponentType != null ? newComponentType.createArrayType() : null;
            }

            @Override
            public PsiType visitArrayType(PsiArrayType arrayType) {
                final PsiType componentType = arrayType.getComponentType();
                final PsiType newComponentType = componentType.accept(this);
                if (newComponentType == componentType) return arrayType;
                return newComponentType != null ? newComponentType.createArrayType() : null;
            }
        });
    }

    @Nullable
    @SuppressWarnings({"HardCodedStringLiteral"})
    public static String getPrimitiveSignature(String typeName) {
        switch (typeName) {
            case "boolean":
                return "Z";
            case "byte":
                return "B";
            case "char":
                return "C";
            case "short":
                return "S";
            case "int":
                return "I";
            case "long":
                return "J";
            case "float":
                return "F";
            case "double":
                return "D";
            case "void":
                return "V";
            default:
                return null;
        }
    }

    @Nullable
    public abstract PsiClass findClass(@NonNull String qualifiedName);

    @Nullable
    public abstract PsiClassType getClassType(@Nullable PsiClass psiClass);

    @NonNull
    public abstract PsiAnnotation[] getAllAnnotations(@NonNull PsiModifierListOwner owner,
            boolean inHierarchy);

    @Nullable
    public abstract PsiAnnotation findAnnotationInHierarchy(
            @NonNull PsiModifierListOwner listOwner,
            @NonNull String... annotationNames);

    @Nullable
    public abstract PsiAnnotation findAnnotation(
            @Nullable PsiModifierListOwner listOwner,
            @NonNull String... annotationNames);

    /**
     * Try to determine the path to the .jar file containing the element, <b>if</b> applicable
     */
    @Nullable
    public abstract String findJarPath(@NonNull PsiElement element);

    /**
     * Try to determine the path to the .jar file containing the element, <b>if</b> applicable
     */
    @Nullable
    public abstract String findJarPath(@NonNull UElement element);

    /**
     * Returns true if the given annotation is inherited (instead of being defined directly
     * on the given modifier list holder
     *
     * @param annotation the annotation to check
     * @param owner      the owner potentially declaring the annotation
     * @return true if the annotation is inherited rather than being declared directly on this owner
     */
    public boolean isInherited(@NonNull PsiAnnotation annotation,
            @NonNull PsiModifierListOwner owner) {
        PsiAnnotationOwner annotationOwner = annotation.getOwner();
        return annotationOwner == null || !annotationOwner.equals(owner.getModifierList());
    }

    public boolean isInherited(@NonNull UAnnotation annotation,
            @NonNull PsiModifierListOwner owner) {
        PsiElement psi = annotation.getPsi();
        if (psi instanceof PsiAnnotation) {
            PsiAnnotationOwner annotationOwner = ((PsiAnnotation)psi).getOwner();
            return annotationOwner == null || !annotationOwner.equals(owner.getModifierList());
        }

        return true;
    }

    /**
     * Returns true if the given annotation is inherited (instead of being defined directly
     * on the given modifier list holder
     *
     * @param annotation the annotation to check
     * @param owner      the owner potentially declaring the annotation
     * @return true if the annotation is inherited rather than being declared directly on this owner
     */
    public boolean isInherited(@NonNull UAnnotation annotation, @NonNull UAnnotated owner) {
        return owner.getAnnotations().contains(annotation);
    }

    @Nullable
    public abstract PsiPackage getPackage(@NonNull PsiElement node);

    @Nullable
    public abstract PsiPackage getPackage(@NonNull UElement node);

    // Just here to disambiguate getPackage(PsiElement) and getPackage(UElement) since
    // a UMethod is both a PsiElement and a UElement
    @Nullable
    public PsiPackage getPackage(@NonNull UMethod node) {
        return getPackage((PsiElement) node);
    }

    /**
     * Return the Gradle group id for the given element, <b>if</b> applicable. For example, for
     * a method in the appcompat library, this would return "com.android.support".
     */
    @Nullable
    public MavenCoordinates getLibrary(@NonNull PsiElement element) {
        return getLibrary(findJarPath(element));
    }

    /**
     * Return the Gradle group id for the given element, <b>if</b> applicable. For example, for
     * a method in the appcompat library, this would return "com.android.support".
     */
    @Nullable
    public MavenCoordinates getLibrary(@NonNull UElement element) {
        return getLibrary(findJarPath(element));
    }

    /** Disambiguate between UElement and PsiElement since a UMethod is both */
    @Nullable
    public MavenCoordinates getLibrary(@NonNull UMethod element) {
        return getLibrary((PsiElement)element);
    }

    public abstract Dependencies getDependencies();

    @Nullable
    private MavenCoordinates getLibrary(@Nullable String jarFile) {
        if (jarFile != null) {
            if (jarToGroup == null) {
                jarToGroup = Maps.newHashMap();
            }
            MavenCoordinates coordinates = jarToGroup.get(jarFile);
            if (coordinates == null) {
                Library library = findOwnerLibrary(jarFile.replace('/', File.separatorChar));
                if (library != null) {
                    coordinates = library.getResolvedCoordinates();
                }
                if (coordinates == null) {
                    // Use string location to figure it out. Note however that
                    // this doesn't work when the build cache is in effect.
                    // Example:
                    // $PROJECT_DIRECTORY/app/build/intermediates/exploded-aar/com.android.support/
                    //          /appcompat-v7/25.0.0-SNAPSHOT/jars/classes.jar
                    // and we want to pick out "com.android.support" and "appcompat-v7"
                    int index = jarFile.indexOf("exploded-aar");
                    if (index != -1) {
                        index += 13; // "exploded-aar/".length()
                        for (int i = index; i < jarFile.length(); i++) {
                            char c = jarFile.charAt(i);
                            if (c == '/' || c == File.separatorChar) {
                                String groupId = jarFile.substring(index, i);
                                i++;
                                for (int j = i; j < jarFile.length(); j++) {
                                    c = jarFile.charAt(j);
                                    if (c == '/' || c == File.separatorChar) {
                                        String artifactId = jarFile.substring(i, j);
                                        coordinates = new MyMavenCoordinates(groupId, artifactId);
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
                if (coordinates == null) {
                    coordinates = MyMavenCoordinates.NONE;
                }
                jarToGroup.put(jarFile, coordinates);
            }
            return coordinates == MyMavenCoordinates.NONE ? null : coordinates;
        }

        return null;
    }

    @Nullable
    public Library findOwnerLibrary(@NonNull String jarFile) {
        Dependencies dependencies = getDependencies();
        if (dependencies != null) {
            Library match = findOwnerLibrary(dependencies.getLibraries(), jarFile);
            if (match != null) {
                return match;
            }
            match = findOwnerJavaLibrary(dependencies.getJavaLibraries(), jarFile);
            if (match != null) {
                return match;
            }
        }

        return null;
    }

    @Nullable
    private static Library findOwnerJavaLibrary(
            @NonNull Collection<? extends JavaLibrary> dependencies,
            @NonNull String jarFile) {
        for (JavaLibrary library : dependencies) {
            if (jarFile.equals(library.getJarFile().getPath())) {
                return library;
            }
            Library match = findOwnerJavaLibrary(library.getDependencies(), jarFile);
            if (match != null) {
                return match;
            }
        }

        return null;
    }

    @Nullable
    private static Library findOwnerLibrary(
            @NonNull Collection<? extends AndroidLibrary> dependencies,
            @NonNull String jarFile) {
        for (AndroidLibrary library : dependencies) {
            if (jarFile.equals(library.getJarFile().getPath())) {
                return library;
            }
            for (File jar : library.getLocalJars()) {
                if (jarFile.equals(jar.getPath())) {
                    return library;
                }
            }
            Library match = findOwnerLibrary(library.getLibraryDependencies(), jarFile);
            if (match != null) {
                return match;
            }

            match = findOwnerJavaLibrary(library.getJavaDependencies(), jarFile);
            if (match != null) {
                return match;
            }
        }

        return null;
    }

    /**
     * Cache for {@link #getLibrary(PsiElement)}
     */
    private Map<String, MavenCoordinates> jarToGroup;

    /**
     * Dummy implementation of {@link com.android.builder.model.MavenCoordinates} which
     * only stores group and artifact id's for now
     */
    private static class MyMavenCoordinates implements MavenCoordinates {

        private static final MyMavenCoordinates NONE = new MyMavenCoordinates("", "");

        private final String groupId;
        private final String artifactId;

        public MyMavenCoordinates(@NonNull String groupId, @NonNull String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }

        @NonNull
        @Override
        public String getGroupId() {
            return groupId;
        }

        @NonNull
        @Override
        public String getArtifactId() {
            return artifactId;
        }

        @NonNull
        @Override
        public String getVersion() {
            return "";
        }

        @NonNull
        @Override
        public String getPackaging() {
            return "";
        }

        @Nullable
        @Override
        public String getClassifier() {
            return "";
        }

        @Override
        public String getVersionlessId() {
            return groupId + ':' + artifactId;
        }
    }
}
