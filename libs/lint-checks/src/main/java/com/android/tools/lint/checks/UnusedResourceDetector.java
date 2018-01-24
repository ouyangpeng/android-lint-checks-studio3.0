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

import static com.android.SdkConstants.ATTR_CLASS;
import static com.android.SdkConstants.ATTR_DISCARD;
import static com.android.SdkConstants.ATTR_KEEP;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_SHRINK_MODE;
import static com.android.SdkConstants.DOT_JAVA;
import static com.android.SdkConstants.DOT_XML;
import static com.android.SdkConstants.TAG_DATA;
import static com.android.SdkConstants.TAG_LAYOUT;
import static com.android.SdkConstants.TOOLS_PREFIX;
import static com.android.SdkConstants.XMLNS_PREFIX;
import static com.android.utils.SdkUtils.endsWithIgnoreCase;
import static com.android.utils.XmlUtils.getFirstSubTagByName;
import static com.android.utils.XmlUtils.getNextTagByName;
import static com.google.common.base.Charsets.UTF_8;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.BuildTypeContainer;
import com.android.builder.model.ClassField;
import com.android.builder.model.ProductFlavor;
import com.android.builder.model.ProductFlavorContainer;
import com.android.builder.model.SourceProvider;
import com.android.builder.model.Variant;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.tools.lint.checks.ResourceUsageModel.Resource;
import com.android.tools.lint.client.api.UElementHandler;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector.BinaryResourceScanner;
import com.android.tools.lint.detector.api.Detector.UastScanner;
import com.android.tools.lint.detector.api.Detector.XmlScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.ResourceContext;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;
import com.android.utils.XmlUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.USimpleNameReferenceExpression;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Finds unused resources.
 */
public class UnusedResourceDetector extends ResourceXmlDetector implements UastScanner,
        BinaryResourceScanner, XmlScanner {

    private static final Implementation IMPLEMENTATION = new Implementation(
            UnusedResourceDetector.class,
            EnumSet.of(Scope.MANIFEST, Scope.ALL_RESOURCE_FILES, Scope.ALL_JAVA_FILES,
                    Scope.BINARY_RESOURCE_FILE, Scope.TEST_SOURCES));

    /** Unused resources (other than ids). */
    public static final Issue ISSUE = Issue.create(
            "UnusedResources",
            "Unused resources",
            "Unused resources make applications larger and slow down builds.",
            Category.PERFORMANCE,
            3,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Unused id's */
    public static final Issue ISSUE_IDS = Issue.create(
            "UnusedIds",
            "Unused id",
            "This resource id definition appears not to be needed since it is not referenced " +
            "from anywhere. Having id definitions, even if unused, is not necessarily a bad " +
            "idea since they make working on layouts and menus easier, so there is not a " +
            "strong reason to delete these.",
            Category.PERFORMANCE,
            1,
            Severity.WARNING,
            IMPLEMENTATION)
            .setEnabledByDefault(false);

    private final UnusedResourceDetectorUsageModel model = new UnusedResourceDetectorUsageModel();

    // Map from data-binding ViewBinding classes (base names, not fully qualified names)
    // to corresponding layout resource names, if any
    private Map<String,String> bindingClasses;

    /**
     * Whether the resource detector will look for inactive resources (e.g. resource and code
     * references in source sets that are not the primary/active variant)
     */
    public static boolean sIncludeInactiveReferences = true;

    /**
     * Constructs a new {@link UnusedResourceDetector}
     */
    public UnusedResourceDetector() {
    }

    private void addDynamicResources(
            @NonNull Context context) {
        Project project = context.getProject();
        AndroidProject model = project.getGradleProjectModel();
        if (model != null) {
            Variant selectedVariant = project.getCurrentVariant();
            if (selectedVariant != null) {
                for (BuildTypeContainer container : model.getBuildTypes()) {
                    if (selectedVariant.getBuildType().equals(container.getBuildType().getName())) {
                        addDynamicResources(project, container.getBuildType().getResValues());
                    }
                }
            }
            ProductFlavor flavor = model.getDefaultConfig().getProductFlavor();
            addDynamicResources(project, flavor.getResValues());
        }
    }

    private void addDynamicResources(@NonNull Project project,
            @NonNull Map<String, ClassField> resValues) {
        Set<String> keys = resValues.keySet();
        if (!keys.isEmpty()) {
            Location location = LintUtils.guessGradleLocation(project);
            for (String name : keys) {
                ClassField field = resValues.get(name);
                ResourceType type = ResourceType.getEnum(field.getType());
                if (type == null) {
                    // Highly unlikely. This would happen if in the future we add
                    // some new ResourceType, that the Gradle plugin (and the user's
                    // Gradle file is creating) and it's an older version of Studio which
                    // doesn't yet have this ResourceType in its enum.
                    continue;
                }
                Resource resource = model.declareResource(type, name, null);
                resource.recordLocation(location);
            }
        }
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (context.getPhase() == 1) {
            Project project = context.getProject();

            // Look for source sets that aren't part of the active variant;
            // we need to make sure we find references in those source sets as well
            // such that we don't incorrectly remove resources that are
            // used by some other source set.
            if (sIncludeInactiveReferences && project.isGradleProject() && !project.isLibrary()) {
                AndroidProject model = project.getGradleProjectModel();
                Variant variant = project.getCurrentVariant();
                if (model != null && variant != null) {
                    addInactiveReferences(model, variant);
                }
            }

            addDynamicResources(context);
            model.processToolsAttributes();

            List<Resource> unusedResources = model.findUnused();
            Set<Resource> unused = Sets.newHashSetWithExpectedSize(unusedResources.size());
            for (Resource resource : unusedResources) {
                if (resource.isDeclared()
                        && !resource.isPublic()
                        && resource.type != ResourceType.PUBLIC) {
                    unused.add(resource);
                }
            }

            // Remove id's if the user has disabled reporting issue ids
            if (!unused.isEmpty() && !context.isEnabled(ISSUE_IDS)) {
                // Remove all R.id references
                List<Resource> ids = Lists.newArrayList();
                for (Resource resource : unused) {
                    if (resource.type == ResourceType.ID) {
                        ids.add(resource);
                    }
                }
                unused.removeAll(ids);
            }

            if (!unused.isEmpty()) {
                model.unused = unused;

                // Request another pass, and in the second pass we'll gather location
                // information for all declaration locations we've found
                context.requestRepeat(this, Scope.ALL_RESOURCES_SCOPE);
            }
        } else {
            assert context.getPhase() == 2;

            // Report any resources that we (for some reason) could not find a declaration
            // location for
            Collection<Resource> unused = model.unused;
            if (!unused.isEmpty()) {
                // Final pass: we may have marked a few resource declarations with
                // tools:ignore; we don't check that on every single element, only those
                // first thought to be unused. We don't just remove the elements explicitly
                // marked as unused, we revisit everything transitively such that resources
                // referenced from the ignored/kept resource are also kept.
                unused = model.findUnused(Lists.newArrayList(unused));
                if (unused.isEmpty()) {
                    return;
                }

                // Fill in locations for files that we didn't encounter in other ways
                for (Resource resource : unused) {
                    Location location = resource.locations;
                    //noinspection VariableNotUsedInsideIf
                    if (location != null) {
                        continue;
                    }

                    // Try to figure out the file if it's a file based resource (such as R.layout) --
                    // in that case we can figure out the filename since it has a simple mapping
                    // from the resource name (though the presence of qualifiers like -land etc
                    // makes it a little tricky if there's no base file provided)
                    ResourceType type = resource.type;
                    if (type != null && LintUtils.isFileBasedResourceType(type)) {
                        String name = resource.name;

                        List<File> folders = Lists.newArrayList();
                        List<File> resourceFolders = context.getProject().getResourceFolders();
                        for (File res : resourceFolders) {
                            File[] f = res.listFiles();
                            if (f != null) {
                                folders.addAll(Arrays.asList(f));
                            }
                        }
                        if (!folders.isEmpty()) {
                            // Process folders in alphabetical order such that we process
                            // based folders first: we want the locations in base folder
                            // order
                            folders.sort(Comparator.comparing(File::getName));
                            for (File folder : folders) {
                                if (folder.getName().startsWith(type.getName())) {
                                    File[] files = folder.listFiles();
                                    if (files != null) {
                                        Arrays.sort(files);
                                        for (File file : files) {
                                            String fileName = file.getName();
                                            if (fileName.startsWith(name)
                                                    && fileName.startsWith(".",
                                                            name.length())) {
                                                resource.recordLocation(Location.create(file));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                List<Resource> sorted = Lists.newArrayList(unused);
                Collections.sort(sorted);

                Boolean skippedLibraries = null;

                for (Resource resource : sorted) {
                    Location location = resource.locations;
                    if (location != null) {
                        // We were prepending locations, but we want to prefer the base folders
                        location = Location.reverse(location);
                    }

                    if (location == null) {
                        if (skippedLibraries == null) {
                            skippedLibraries = false;
                            for (Project project : context.getDriver().getProjects()) {
                                if (!project.getReportIssues()) {
                                    skippedLibraries = true;
                                    break;
                                }
                            }
                        }
                        if (skippedLibraries) {
                            // Skip this resource if we don't have a location, and one or
                            // more library projects were skipped; the resource was very
                            // probably defined in that library project and only encountered
                            // in the main project's java R file
                            continue;
                        }
                    }

                    String field = resource.getField();
                    String message = String.format("The resource `%1$s` appears to be unused",
                            field);
                    if (location == null) {
                        location = Location.create(context.getProject().getDir());
                    }
                    LintFix fix = fix().data(field);
                    context.report(getIssue(resource), location, message, fix);
                }
            }
        }
    }

    /** Returns source providers that are <b>not</b> part of the given variant */
    @NonNull
    private static List<SourceProvider> getInactiveSourceProviders(
            @NonNull AndroidProject project,
            @NonNull Variant variant) {
        Collection<Variant> variants = project.getVariants();
        List<SourceProvider> providers = Lists.newArrayList();

        // Add other flavors
        Collection<ProductFlavorContainer> flavors = project.getProductFlavors();
        for (ProductFlavorContainer pfc : flavors) {
            if (variant.getProductFlavors().contains(pfc.getProductFlavor().getName())) {
                continue;
            }
            providers.add(pfc.getSourceProvider());
        }

        // Add other multi-flavor source providers
        for (Variant v : variants) {
            if (variant.getName().equals(v.getName())) {
                continue;
            }
            SourceProvider provider = v.getMainArtifact().getMultiFlavorSourceProvider();
            if (provider != null) {
                providers.add(provider);
            }
        }

        // Add other the build types
        Collection<BuildTypeContainer> buildTypes = project.getBuildTypes();
        for (BuildTypeContainer btc : buildTypes) {
            if (variant.getBuildType().equals(btc.getBuildType().getName())) {
                continue;
            }
            providers.add(btc.getSourceProvider());
        }

        // Add other the other variant source providers
        for (Variant v : variants) {
            if (variant.getName().equals(v.getName())) {
                continue;
            }
            SourceProvider provider = v.getMainArtifact().getVariantSourceProvider();
            if (provider != null) {
                providers.add(provider);
            }
        }

        return providers;
    }

    private void recordInactiveJavaReferences(@NonNull File resDir) {
        File[] files = resDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    recordInactiveJavaReferences(file);
                } else if (file.getName().endsWith(DOT_JAVA)) {
                    try {
                        String java = Files.toString(file, UTF_8);
                        model.tokenizeJavaCode(java);
                    } catch (Throwable ignore) {
                        // Tolerate parsing errors etc in these files; they're user
                        // sources, and this is even for inactive source sets.
                    }
                }
            }
        }
    }

    private void recordInactiveXmlResources(@NonNull File resDir) {
        File[] resourceFolders = resDir.listFiles();
        if (resourceFolders != null) {
            for (File folder : resourceFolders) {
                ResourceFolderType folderType = ResourceFolderType.getFolderType(folder.getName());
                if (folderType != null) {
                    recordInactiveXmlResources(folderType, folder);
                }
            }
        }
    }

    // Used for traversing resource folders *outside* of the normal Gradle variant
    // folders: these are not necessarily on the project path, so we don't have PSI files
    // for them
    private void recordInactiveXmlResources(@NonNull ResourceFolderType folderType,
      @NonNull File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                String path = file.getPath();
                boolean isXml = endsWithIgnoreCase(path, DOT_XML);
                try {
                    if (isXml) {
                        String xml = Files.toString(file, UTF_8);
                        Document document = XmlUtils.parseDocument(xml, true);
                        model.visitXmlDocument(file, folderType, document);
                    } else {
                        model.visitBinaryResource(folderType, file);
                    }
                } catch (Throwable ignore) {
                    // Tolerate parsing errors etc in these files; they're user
                    // sources, and this is even for inactive source sets.
                }
            }
        }
    }

    private void addInactiveReferences(@NonNull AndroidProject model,
                              @NonNull Variant variant) {
        for (SourceProvider provider : getInactiveSourceProviders(model, variant)) {
            for (File res : provider.getResDirectories()) {
                // Scan resource directory
                if (res.isDirectory()) {
                    recordInactiveXmlResources(res);
                }
            }
            for (File file : provider.getJavaDirectories()) {
                // Scan Java directory
                if (file.isDirectory()) {
                    recordInactiveJavaReferences(file);
                }
            }
        }
    }

    private static Issue getIssue(@NonNull Resource resource) {
        return resource.type != ResourceType.ID ? ISSUE : ISSUE_IDS;
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return true;
    }

    // ---- Implements BinaryResourceScanner ----

    @Override
    public void checkBinaryResource(@NonNull ResourceContext context) {
        model.context = context;
        try {
            model.visitBinaryResource(context.getResourceFolderType(), context.file);
        } finally {
            model.context = null;
        }
    }

    // ---- Implements XmlScanner ----

    @Override
    public void visitDocument(@NonNull XmlContext context, @NonNull Document document) {
        model.context = model.xmlContext = context;
        try {
            ResourceFolderType folderType = context.getResourceFolderType();
            model.visitXmlDocument(context.file, folderType, document);

            // Data binding layout? If so look for usages of the binding class too
            Element root = document.getDocumentElement();
            if (folderType == ResourceFolderType.LAYOUT && root != null
                    && TAG_LAYOUT.equals(root.getTagName())) {
                if (bindingClasses == null) {
                    bindingClasses = Maps.newHashMap();
                }
                String fileName = context.file.getName();
                String resourceName = LintUtils.getBaseName(fileName);
                Element data = getFirstSubTagByName(root, TAG_DATA);
                String bindingClass = null;
                while (data != null) {
                    bindingClass = data.getAttribute(ATTR_CLASS);
                    if (bindingClass != null && !bindingClass.isEmpty()) {
                        int dot = bindingClass.lastIndexOf('.');
                        bindingClass = bindingClass.substring(dot + 1);
                        break;
                    }
                    data = getNextTagByName(data, TAG_DATA);
                }
                if (bindingClass == null || bindingClass.isEmpty()) {
                    // See ResourceBundle#getFullBindingClass
                    bindingClass = toClassName(resourceName) + "Binding";
                }
                bindingClasses.put(bindingClass, resourceName);
            }

        } finally {
            model.context = model.xmlContext = null;
        }
    }

    // Copy from android.databinding.tool.util.ParserHelper:
    public static String toClassName(String name) {
        StringBuilder builder = new StringBuilder();
        for (String item : name.split("[_-]")) {
            builder.append(capitalize(item));
        }
        return builder.toString();
    }

    // Copy from android.databinding.tool.util.StringUtils: using
    // this instead of IntelliJ's more flexible method to ensure
    // we compute the same names as data-binding generated code
    private static String capitalize(String string) {
        if (Strings.isNullOrEmpty(string)) {
            return string;
        }
        char ch = string.charAt(0);
        if (Character.isTitleCase(ch)) {
            return string;
        }
        return Character.toTitleCase(ch) + string.substring(1);
    }

    // ---- Implements UastScanner ----

    @Override
    public boolean appliesToResourceRefs() {
        return true;
    }

    @Override
    public void visitResourceReference(@NonNull JavaContext context, @NonNull UElement node,
            @NonNull ResourceType type, @NonNull String name, boolean isFramework) {
        if (!isFramework) {
            ResourceUsageModel.markReachable(model.addResource(type, name, null));
        }
    }

    @Nullable
    @Override
    public List<Class<? extends UElement>> getApplicableUastTypes() {
        return Collections.singletonList(USimpleNameReferenceExpression.class);
    }

    @Nullable
    @Override
    public UElementHandler createUastHandler(@NonNull final JavaContext context) {
        // If using data binding we also have to look for references to the
        // ViewBinding classes which could be implicit usages of layout resources
        if (bindingClasses == null) {
            return null;
        }

        return new UElementHandler() {
            @Override
            public void visitSimpleNameReferenceExpression(
                    USimpleNameReferenceExpression expression) {

                String name = expression.getIdentifier();
                String resourceName = bindingClasses.get(name);
                if (resourceName != null) {
                    // Make sure it's really a binding class
                    PsiElement resolved = expression.resolve();
                    if (resolved instanceof PsiClass
                            && context.getEvaluator().extendsClass((PsiClass) resolved,
                            "android.databinding.ViewDataBinding", true)) {
                        ResourceUsageModel.markReachable(model.getResource(ResourceType.LAYOUT,
                                resourceName));
                    }
                }
            }
        };
    }

    private static class UnusedResourceDetectorUsageModel extends ResourceUsageModel {
        public XmlContext xmlContext;
        public Context context;
        public Set<Resource> unused = Sets.newHashSet();

        @NonNull
        @Override
        protected String readText(@NonNull File file) {
            if (context != null) {
                return context.getClient().readFile(file).toString();
            }
            try {
                return Files.toString(file, UTF_8);
            } catch (IOException e) {
                return ""; // Lint API
            }
        }

        @Override
        protected Resource declareResource(ResourceType type, String name, Node node) {
            Resource resource = super.declareResource(type, name, node);
            if (context != null) {
                resource.setDeclared(context.getProject().getReportIssues());
                if (context.getPhase() == 2 && unused.contains(resource)) {
                    if (xmlContext != null && xmlContext.getDriver().isSuppressed(xmlContext,
                            getIssue(resource), node)) {
                        resource.setKeep(true);
                    } else {
                        // For positions we try to use the name node rather than the
                        // whole declaration element
                        if (node == null || xmlContext == null) {
                            resource.recordLocation(Location.create(context.file));
                        } else {
                            if (node instanceof Element) {
                                Node attribute = ((Element) node).getAttributeNode(ATTR_NAME);
                                if (attribute != null) {
                                    node = attribute;
                                }
                            }
                            resource.recordLocation(xmlContext.getLocation(node));
                        }
                    }
                }

                if (type == ResourceType.RAW &&isKeepFile(name, xmlContext)) {
                    // Don't flag raw.keep: these are used for resource shrinking
                    // keep lists
                    //    https://developer.android.com/studio/build/shrink-code.html
                    resource.setReachable(true);
                }
            }

            return resource;
        }

        private static boolean isKeepFile(
          @NonNull String name,
          @Nullable XmlContext xmlContext) {
            if ("keep".equals(name)) {
                return true;
            }

            if (xmlContext != null && xmlContext.document != null) {
                Element element = xmlContext.document.getDocumentElement();
                if (element != null && element.getFirstChild() == null) {
                    NamedNodeMap attributes = element.getAttributes();
                    boolean found = false;
                    for (int i = 0, n = attributes.getLength(); i < n; i++) {
                        Node attr = attributes.item(i);
                        String nodeName = attr.getNodeName();
                        if (!nodeName.startsWith(XMLNS_PREFIX)
                                && !nodeName.startsWith(TOOLS_PREFIX)) {
                            return false;
                        } else if (nodeName.endsWith(ATTR_SHRINK_MODE) ||
                                nodeName.endsWith(ATTR_DISCARD) ||
                                nodeName.endsWith(ATTR_KEEP)) {
                            found = true;
                        }
                    }

                    return found;
                }
            }


            return false;
        }
    }
}
