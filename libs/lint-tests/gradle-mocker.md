### Gradle Builder Model Mock Generator

#### Background

The Android Studio codebase has a lot of special handling for Gradle
features -- looking at the contents of source sets, product flavors,
build types, recursive dependency declarations, manifest placeholders,
and so on. However, unit testing this functionality is hard: you don't
want to create a real Gradle project and run through a full Gradle
sync to obtain the builder model. Until now, the way to do this has
been to build up your own mocks for the builder model.

However, mocking the builder model is hard -- it's a pretty deep
hierarchy of objects.

For example, take something as simple as the following Gradle file:

```groovy
android {
    defaultConfig {
        resConfigs "mdpi"
    }
    flavorDimensions  "pricing", "releaseType"
    productFlavors {
        beta {
            flavorDimension "releaseType"
            resConfig "en"
            resConfigs "nodpi", "hdpi"
        }
        normal { flavorDimension "releaseType" }
        free { flavorDimension "pricing" }
        paid { flavorDimension "pricing" }
    }
}
```

The above is from an existing unit test, and the corresponding mocking
code looked like this:

```java
ProductFlavor flavorFree = mock(ProductFlavor.class);
when(flavorFree.getName()).thenReturn("free");
when(flavorFree.getResourceConfigurations())
        .thenReturn(Collections.<String>emptyList());

ProductFlavor flavorNormal = mock(ProductFlavor.class);
when(flavorNormal.getName()).thenReturn("normal");
when(flavorNormal.getResourceConfigurations())
        .thenReturn(Collections.<String>emptyList());

ProductFlavor flavorPaid = mock(ProductFlavor.class);
when(flavorPaid.getName()).thenReturn("paid");
when(flavorPaid.getResourceConfigurations())
        .thenReturn(Collections.<String>emptyList());

ProductFlavor flavorBeta = mock(ProductFlavor.class);
when(flavorBeta.getName()).thenReturn("beta");
List<String> resConfigs = Arrays.asList("hdpi", "en", "nodpi");
when(flavorBeta.getResourceConfigurations()).thenReturn(resConfigs);

ProductFlavor defaultFlavor = mock(ProductFlavor.class);
when(defaultFlavor.getName()).thenReturn("main");
when(defaultFlavor.getResourceConfigurations()).thenReturn(
        Collections.singleton("mdpi"));

ProductFlavorContainer containerBeta =
        mock(ProductFlavorContainer.class);
when(containerBeta.getProductFlavor()).thenReturn(flavorBeta);

ProductFlavorContainer containerFree =
        mock(ProductFlavorContainer.class);
when(containerFree.getProductFlavor()).thenReturn(flavorFree);

ProductFlavorContainer containerPaid =
        mock(ProductFlavorContainer.class);
when(containerPaid.getProductFlavor()).thenReturn(flavorPaid);

ProductFlavorContainer containerNormal =
        mock(ProductFlavorContainer.class);
when(containerNormal.getProductFlavor()).thenReturn(flavorNormal);

ProductFlavorContainer defaultContainer =
        mock(ProductFlavorContainer.class);
when(defaultContainer.getProductFlavor()).thenReturn(defaultFlavor);

List<ProductFlavorContainer> containers = Arrays.asList(
        containerPaid, containerFree, containerNormal, containerBeta
);

AndroidProject project = mock(AndroidProject.class);
when(project.getProductFlavors()).thenReturn(containers);
when(project.getDefaultConfig()).thenReturn(defaultContainer);


List<String> productFlavorNames = Arrays.asList("free", "beta");
Variant mock = mock(Variant.class);
when(mock.getProductFlavors()).thenReturn(productFlavorNames);
```

And this isn't even a complete mock; the set of product flavors also
affects things like the variant name and so on.

In addition to the sheer volume of mocking code, this code wasn't
trivial to write either; you have to know the builder model APIs
pretty well in order to know how to put things together.


#### The Mock Generator

To solve this, there's a new mocker library which lets you just pass
the above Gradle code, and it spits out the corresponding builder
model objects (project and variant).

The above test code setup can be replaced by just

```java
GradleModelMocker mocker = createMocker(""
        + "apply plugin: 'com.android.application'\n"
        + "\n"
        + "android {\n"
        + "    defaultConfig {\n"
        + "        resConfigs \"mdpi\"\n"
        + "    }\n"
        + "    flavorDimensions  \"pricing\", \"releaseType\"\n"
        + "    productFlavors {\n"
        + "        beta {\n"
        + "            flavorDimension \"releaseType\"\n"
        + "            resConfig \"en\"\n"
        + "            resConfigs \"nodpi\", \"hdpi\"\n"
        + "        }\n"
        + "        normal { flavorDimension \"releaseType\" }\n"
        + "        free { flavorDimension \"pricing\" }\n"
        + "        paid { flavorDimension \"pricing\" }\n"
        + "    }\n"
        + "}");
AndroidProject project = mocker.getProject();
Variant variant = mocker.getVariant();
```

This dramatically reduces the amount of boiler plate code you have to
write in unit tests.

#### Dependency Graphs

You can easily configure dependencies like this:

```
dependencies {
    compile "com.android.support:appcompat-v7:25.0.1"
    compile "com.android.support.constraint:constraint-layout:1.0.0-beta3"
}
```

This will create the AndroidLibrary mock objects for the above two
dependencies. However, even though the Gradle files doesn't express
it, the above dependencies actually have transitive dependencies --
for example, appcompat depends on the support-v4 library. The Gradle
build mocker has built-in knowledge of many of our standard libraries,
and actually produces a full recursive tree for the above two
dependencies (and others like firebase and play services).

However, you can also feed the Gradle mocker one or more dependency
graphs. These dependency graphs can be created by Gradle by running
the `dependencies` task, e.g.

```shell
$ ./gradlew :app:dependencies
```

You then configure the mocker like this:
```java
GradleModelMocker mocker = createMocker(""
        + "apply plugin: 'com.android.application'\n"
        + "\n"
        + "dependencies {\n"
        + "    compile \"com.android.support:appcompat-v7:25.0.1\"\n"
        + "    compile \"com.android.support.constraint:constraint-layout:1.0.0-beta3\"\n"
        + "}")
        .withDependencyGraph(""
        + "+--- com.android.support:appcompat-v7:25.0.1\n"
        + "|    +--- com.android.support:support-v4:25.0.1\n"
        + "|    |    +--- com.android.support:support-compat:25.0.1\n"
        + "|    |    |    \\--- com.android.support:support-annotations:25.0.1\n"
        + "|    |    +--- com.android.support:support-media-compat:25.0.1\n"
        + "|    |    |    \\--- com.android.support:support-compat:25.0.1 (*)\n"
        + "|    |    +--- com.android.support:support-core-utils:25.0.1\n"
        + "|    |    |    \\--- com.android.support:support-compat:25.0.1 (*)\n"
        + "|    |    +--- com.android.support:support-core-ui:25.0.1\n"
        + "|    |    |    \\--- com.android.support:support-compat:25.0.1 (*)\n"
        + "|    |    \\--- com.android.support:support-fragment:25.0.1\n"
        + "|    |         +--- com.android.support:support-compat:25.0.1 (*)\n"
        + "|    |         +--- com.android.support:support-media-compat:25.0.1 (*)\n"
        + "|    |         +--- com.android.support:support-core-ui:25.0.1 (*)\n"
        + "|    |         \\--- com.android.support:support-core-utils:25.0.1 (*)\n"
        + "|    +--- com.android.support:support-vector-drawable:25.0.1\n"
        + "|    |    \\--- com.android.support:support-compat:25.0.1 (*)\n"
        + "|    \\--- com.android.support:animated-vector-drawable:25.0.1\n"
        + "|         \\--- com.android.support:support-vector-drawable:25.0.1 (*)\n"
        + "+--- com.android.support.constraint:constraint-layout:1.0.0-beta3\n"
        + "|    \\--- com.android.support.constraint:constraint-layout-solver:1.0.0-beta3\n");

Dependencies dependencies = mocker.getVariant().getMainArtifact().getDependencies();
```

In the above, the string passed to `withDependencyGraph` is the graph
produced by Gradle. When the Gradle mocker is building up its
AndroidLibrary graph, for any artifact it comes across, it will
consult the dependency graph and if it finds a subgraph that matches
the artifact, it will construct a hierarchy instead of a shallow
library as necessary.


#### Supported Features

The mock generator supports all the constructs that used to be
manually mocked in the lint codebase.  This includes

* Dependencies
* DefaultConfig (and minSdkVersion, targetSdkVersion, applicationId, versionCode, etc.)
* Product Flavors, flavor dimensions and Build types
* Splits (density, language, ABI)
* ResValues and ResConfigs
* Manifest placeholders
* Vector drawable options

The main things that are *not* yet supported:

* Scopes (e.g. testCompile)
* Source sets other than main, or files other than manifest, java and resources

#### Limitations

As noted under supported features, there are some Gradle DSL features
that are not yet supported.

More importantly, note that the mock generator does a very lightweight
parse of the Gradle code and looks for patterns that it recognizes. It
then constructs the mocks based on these patterns.

This means that if your Gradle file is doing anything non-static, such
as calling Groovy code, or if it's formatted in an unusual way, things
won't work. (Note however that when the mocker doesn't understand
something, it will throw an error. You can optionally ask it to just
ignore these constructs.)

#### Intended Use

The mock generator is intended to replace manual mocking. You should
not use it to test actual Gradle behavior, such the exact semantics of
whether the minSdkVersion from a product flavor is overridden by the
minSdkVersion from a build type.

If you run into problems, let tnorbye@ know.
