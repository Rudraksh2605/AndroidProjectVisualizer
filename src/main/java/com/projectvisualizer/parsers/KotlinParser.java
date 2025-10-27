package com.projectvisualizer.parsers;

import com.projectvisualizer.model.*;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.com.intellij.openapi.Disposable;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.lexer.KtTokens;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KotlinParser {

    private Map<String, String> activityLayoutMap;
    private IntentAnalyzer intentAnalyzer = new IntentAnalyzer();
    private ScreenFlowDetector screenFlowDetector = new ScreenFlowDetector();
    private static final DaggerHiltAnalyzer DAGGER_HILT_ANALYZER = new DaggerHiltAnalyzer();

    public KotlinParser() {
        // Default constructor
    }

    public KotlinParser(Map<String, String> activityLayoutMap) {
        this.activityLayoutMap = activityLayoutMap;
    }

    public List<CodeComponent> parse(File file) throws Exception {
        List<CodeComponent> components = new ArrayList<>();

        // Validate file input
        if (file == null || !file.exists() || !file.isFile()) {
            System.err.println("Invalid file: " + (file != null ? file.getName() : "null"));
            return components;
        }

        String content;
        try {
            content = new String(Files.readAllBytes(file.toPath()));
        } catch (Exception e) {
            System.err.println("Error reading file: " + file.getName() + " - " + e.getMessage());
            return components;
        }

        Disposable disposable = Disposer.newDisposable();
        try {
            // Kotlin PSI Environment Setup
            CompilerConfiguration configuration = new CompilerConfiguration();
            KotlinCoreEnvironment environment = KotlinCoreEnvironment.createForProduction(
                    disposable,
                    configuration,
                    EnvironmentConfigFiles.JVM_CONFIG_FILES
            );

            PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(environment.getProject());

            // Create PSI tree from file content
            KtFile ktFile = (KtFile) psiFileFactory.createFileFromText(
                    file.getName(),
                    KotlinLanguage.INSTANCE,
                    content
            );

            // Get package name
            String packageName = ktFile.getPackageFqName().asString();

            // Collect imports
            List<String> imports = new ArrayList<>();
            if (ktFile.getImportList() != null) {
                for (KtImportDirective dir : ktFile.getImportList().getImports()) {
                    if (dir.getImportedFqName() != null) {
                        imports.add(dir.getImportedFqName().asString());
                    } else if (dir.getText() != null) {
                        imports.add(dir.getText().replace("import ", "").trim());
                    }
                }
            }

            String moduleName = deriveModuleName(file);

            // Traverse Kotlin class/object/interface declarations
            for (KtClassOrObject classNode : PsiTreeUtil.findChildrenOfType(ktFile, KtClassOrObject.class)) {
                CodeComponent component = new CodeComponent();
                component.setFilePath(file.getAbsolutePath());
                component.setLanguage("kotlin");
                component.setPackageName(packageName);
                component.setModuleName(moduleName);
                component.setImports(new ArrayList<>(imports));
                component.setFileExtension("kt");

                // Class name and ID - Handle null name properly
                String className = classNode.getName();
                if (className == null || className.trim().isEmpty()) {
                    // Use file name as fallback, remove .kt extension
                    className = file.getName().replace(".kt", "");
                    // If still empty, use a default name
                    if (className.isEmpty()) {
                        className = "UnknownClass";
                    }
                }

                String fullClassName = packageName.isEmpty() ? className : packageName + "." + className;

                component.setId(fullClassName);
                component.setName(className);

                // Type detection
                if (classNode instanceof KtObjectDeclaration) {
                    component.setType("object");
                    component.setObjectDeclaration(true);
                } else if (classNode instanceof KtClass && ((KtClass) classNode).isInterface()) {
                    component.setType("interface");
                } else {
                    component.setType("class");
                }

                // Kotlin-specific flags and modifiers
                List<String> modifiers = new ArrayList<>();
                String vis = getVisibility(classNode);
                if (vis != null && !vis.isEmpty()) modifiers.add(vis);
                if (classNode.hasModifier(KtTokens.ABSTRACT_KEYWORD)) modifiers.add("abstract");
                if (classNode.hasModifier(KtTokens.OPEN_KEYWORD)) modifiers.add("open");
                if (classNode.hasModifier(KtTokens.FINAL_KEYWORD)) modifiers.add("final");
                if (classNode.hasModifier(KtTokens.SEALED_KEYWORD)) { modifiers.add("sealed"); component.setSealedClass(true); }
                if (classNode instanceof KtClass && ((KtClass) classNode).isData()) { modifiers.add("data"); component.setDataClass(true); }
                component.setHasCompanionObject(!classNode.getCompanionObjects().isEmpty());
                component.setModifiers(modifiers);

                // Super types (extends/implements)
                List<KtSuperTypeListEntry> superTypes = classNode.getSuperTypeListEntries();
                if (superTypes != null && !superTypes.isEmpty()) {
                    List<String> allSupers = superTypes.stream()
                            .map(e -> e.getTypeReference() != null ? e.getTypeReference().getText() : "")
                            .filter(text -> !text.isEmpty())
                            .collect(Collectors.toList());
                    if (!allSupers.isEmpty()) {
                        component.setExtendsClass(allSupers.get(0));
                        if (allSupers.size() > 1) {
                            component.setImplementsList(allSupers.subList(1, allSupers.size()));
                        }
                    }
                }

                // Extract annotations
                extractAnnotations(classNode, component);

                // Extract properties (fields)
                extractProperties(classNode, component);

                // Extract functions (methods)
                extractFunctions(classNode, component);

                // Extract constructors for dependency injection
                extractConstructors(classNode, component);

                // Detect Android layer
                detectAndroidLayer(component);

                // Heuristic scan for resources, ViewBinding/DataBinding, API/DB hints
                try {
                    scanAdditionalHints(content, imports, component);
                } catch (Exception ignored) { }

                // Infer component type based on inheritance or naming
                try {
                    String extendsClass = component.getExtendsClass();
                    String lower = component.getName() != null ? component.getName().toLowerCase() : "";
                    if (extendsClass != null) {
                        if (extendsClass.endsWith("Activity") || extendsClass.contains("android.app.Activity") || extendsClass.contains("androidx.activity")) {
                            component.setComponentType("Activity");
                        } else if (extendsClass.endsWith("Fragment") || extendsClass.contains("androidx.fragment")) {
                            component.setComponentType("Fragment");
                        } else if (extendsClass.endsWith("Service") || extendsClass.contains("android.app.Service")) {
                            component.setComponentType("Service");
                        } else if (extendsClass.endsWith("BroadcastReceiver") || extendsClass.contains("android.content.BroadcastReceiver")) {
                            component.setComponentType("BroadcastReceiver");
                        } else if (extendsClass.endsWith("ViewModel")) {
                            component.setComponentType("ViewModel");
                        }
                    }
                    if (component.getComponentType() == null) {
                        if (lower.endsWith("viewmodel")) component.setComponentType("ViewModel");
                        else if (lower.endsWith("repository")) component.setComponentType("Repository");
                        else if (lower.endsWith("usecase")) component.setComponentType("UseCase");
                    }
                } catch (Exception ignored) { }

                // Associate layout files for Android components
                if (activityLayoutMap != null && component.getType().equals("class")) {
                    if (activityLayoutMap.containsKey(fullClassName)) {
                        component.setLayer("UI");
                        component.setManifestRegistered(true);
                        component.setComponentType("Activity");
                        String layoutFile = findLayoutFileForComponent(file, component.getName());
                        if (layoutFile != null) {
                            component.addLayoutFile(layoutFile);
                        }
                    }
                }

                // Analyze Dagger/Hilt components
                analyzeDaggerHiltComponents(ktFile, component);

                // Validate component before adding
                if (component.getName() != null) {
                    components.add(component);
                } else {
                    System.err.println("Skipping component with null name in file: " + file.getName());
                }
            }

            // Analyze navigation flows (skip for Kotlin files to avoid JavaParser errors)
            // List<NavigationFlow> intentFlows = intentAnalyzer.analyzeIntentFlows(file);

        } catch (Exception e) {
            System.err.println("Error parsing Kotlin file " + file.getName() + ": " + e.getMessage());
        } finally {
            Disposer.dispose(disposable);
        }

        return components;
    }

    // Heuristic scan for resources, bindings, and API/DB hints
    private void scanAdditionalHints(String content, List<String> imports, CodeComponent component) {
        if (content == null) return;
        try {
            // Resources used: layouts, strings, drawables, colors, ids
            Pattern resPattern = Pattern.compile("R\\.(layout|string|drawable|color|id)\\.([A-Za-z0-9_]+)");
            Matcher rm = resPattern.matcher(content);
            while (rm.find()) {
                String type = rm.group(1);
                String name = rm.group(2);
                component.addResourceUsage(type + "/" + name);
            }

            // ViewBinding/DataBinding heuristics
            if (content.contains("inflate(LayoutInflater.from(") || content.contains(".inflate(layoutInflater")) {
                component.setViewBindingUsed(true);
            }
            Pattern bindingClass = Pattern.compile("([A-Za-z0-9_]+)Binding\\.inflate\\(");
            if (bindingClass.matcher(content).find()) {
                component.setViewBindingUsed(true);
            }
            if (content.contains("DataBindingUtil.setContentView") || content.contains("DataBindingUtil.inflate")) {
                component.setDataBindingUsed(true);
            }

            // Coroutine usage cues beyond suspend keyword
            if (content.contains("CoroutineScope(") || content.contains("launch(") || content.contains("async(") || content.contains("withContext(")) {
                component.setCoroutineUsage(true);
            }

            // API/DB indicators from imports
            for (String imp : imports) {
                String low = imp.toLowerCase();
                if (low.startsWith("retrofit2") || low.contains("retrofit2")) {
                    if (!component.getApiClients().contains("Retrofit")) component.getApiClients().add("Retrofit");
                }
                if (low.contains("ktor")) {
                    if (!component.getApiClients().contains("Ktor")) component.getApiClients().add("Ktor");
                }
                if (low.contains("androidx.room")) {
                    if (!component.getDbDaos().contains("Room")) component.getDbDaos().add("Room");
                }
            }
        } catch (Exception ignored) { }
    }

    private String deriveModuleName(File file) {
        try {
            String path = file.getAbsolutePath().replace('\\', '/');
            if (path.contains("/app/")) return "app";
            int idx = path.indexOf("/src/");
            if (idx > 0) {
                String base = path.substring(0, idx);
                int lastSlash = base.lastIndexOf('/');
                if (lastSlash >= 0 && lastSlash < base.length() - 1) {
                    return base.substring(lastSlash + 1);
                }
            }
            // Fallback to parent directory name
            File parent = file.getParentFile();
            while (parent != null) {
                if (parent.getName().equals("src")) {
                    File moduleDir = parent.getParentFile();
                    return moduleDir != null ? moduleDir.getName() : "root";
                }
                parent = parent.getParentFile();
            }
        } catch (Exception ignored) { }
        return "root";
    }

    private void extractAnnotations(KtClassOrObject classNode, CodeComponent component) {
        List<String> annotations = new ArrayList<>();
        for (KtAnnotationEntry annotation : classNode.getAnnotationEntries()) {
            String annotationText = annotation.getText();
            if (annotationText != null && !annotationText.isEmpty()) {
                annotations.add(annotationText);
            }
        }
        component.setAnnotations(annotations);
    }

    private void extractProperties(KtClassOrObject classNode, CodeComponent component) {
        for (KtProperty property : PsiTreeUtil.findChildrenOfType(classNode, KtProperty.class)) {
            // Skip if property name is null
            if (property.getName() == null) {
                continue;
            }

            CodeField codeField = new CodeField();
            codeField.setName(property.getName());

            if (property.getTypeReference() != null) {
                codeField.setType(property.getTypeReference().getText());
            }

            // Determine visibility
            String visibility = getVisibility(property);
            codeField.setVisibility(visibility);

            component.getFields().add(codeField);

            // Check for dependency injection
            if (hasInjectAnnotation(property.getAnnotationEntries()) || hasKoinInjection(property)) {
                if (property.getTypeReference() != null) {
                    String type = property.getTypeReference().getText();
                    if (type != null) {
                        component.getInjectedDependencies().add(type);
                    }
                }
            }

            // Add dependency if it's a custom type
            if (property.getTypeReference() != null) {
                String fieldType = property.getTypeReference().getText();
                if (fieldType != null && !isPrimitiveType(fieldType) && !fieldType.startsWith("java.")) {
                    CodeComponent dependency = new CodeComponent();
                    dependency.setId(fieldType);
                    dependency.setName(fieldType);
                    dependency.setType("class");
                    component.getDependencies().add(dependency);
                }
            }
        }
    }

    private void extractFunctions(KtClassOrObject classNode, CodeComponent component) {
        for (KtNamedFunction function : PsiTreeUtil.findChildrenOfType(classNode, KtNamedFunction.class)) {
            // Skip if function name is null
            if (function.getName() == null) {
                continue;
            }

            CodeMethod method = new CodeMethod();
            method.setName(function.getName());

            String returnType = "Unit";
            if (function.getTypeReference() != null) {
                returnType = function.getTypeReference().getText();
                method.setReturnType(returnType);
            } else {
                method.setReturnType("Unit");
            }

            // Determine visibility
            String visibility = getVisibility(function);
            method.setVisibility(visibility);

            // Detect suspend/coroutine usage
            if (function.hasModifier(KtTokens.SUSPEND_KEYWORD)) {
                component.setCoroutineUsage(true);
            }

            // Detect @Composable
            boolean isComposable = false;
            for (KtAnnotationEntry ann : function.getAnnotationEntries()) {
                String text = ann.getText();
                if (text != null && text.contains("@Composable")) {
                    isComposable = true;
                    break;
                }
                if (ann.getShortName() != null && "Composable".equals(ann.getShortName().asString())) {
                    isComposable = true;
                    break;
                }
            }
            if (isComposable) {
                component.addComposable(function.getName());
                if (component.getComponentType() == null) {
                    component.setComponentType("Composable");
                }
            }

            // Extract parameters and add type dependencies
            List<String> parameters = new ArrayList<>();
            for (KtParameter param : function.getValueParameters()) {
                String paramName = param.getName() != null ? param.getName() : "unknown";
                String paramType = param.getTypeReference() != null ? param.getTypeReference().getText() : "Any";
                parameters.add(paramName + ": " + paramType);

                if (param.getTypeReference() != null) {
                    String typeText = param.getTypeReference().getText();
                    if (typeText != null && !isPrimitiveType(typeText) && !typeText.startsWith("java.")) {
                        CodeComponent dependency = new CodeComponent();
                        dependency.setId(typeText);
                        dependency.setName(typeText);
                        dependency.setType("class");
                        component.getDependencies().add(dependency);
                    }
                }
            }
            method.setParameters(parameters);

            // Add return type dependency if custom
            if (returnType != null && !"Unit".equals(returnType) && !isPrimitiveType(returnType) && !returnType.startsWith("java.")) {
                CodeComponent dependency = new CodeComponent();
                dependency.setId(returnType);
                dependency.setName(returnType);
                dependency.setType("class");
                component.getDependencies().add(dependency);
            }

            component.getMethods().add(method);
        }
    }

    private void extractConstructors(KtClassOrObject classNode, CodeComponent component) {
        // Primary constructor
        KtPrimaryConstructor primary = classNode.getPrimaryConstructor();
        if (primary != null) {
            boolean isInjected = hasInjectAnnotation(primary.getAnnotationEntries());
            if (isInjected) {
                for (KtParameter param : primary.getValueParameters()) {
                    if (param.getTypeReference() != null) {
                        String paramType = param.getTypeReference().getText();
                        if (paramType != null && !isPrimitiveType(paramType) && !paramType.startsWith("java.")) {
                            component.getInjectedDependencies().add(paramType);
                        }
                    }
                }
            }
        }

        // Secondary constructors
        for (KtSecondaryConstructor constructor : classNode.getSecondaryConstructors()) {
            boolean isInjected = hasInjectAnnotation(constructor.getAnnotationEntries());
            if (isInjected) {
                for (KtParameter param : constructor.getValueParameters()) {
                    if (param.getTypeReference() != null) {
                        String paramType = param.getTypeReference().getText();
                        if (paramType != null && !isPrimitiveType(paramType) && !paramType.startsWith("java.")) {
                            component.getInjectedDependencies().add(paramType);
                        }
                    }
                }
            }
        }
    }

    // In KotlinParser.java - Update the detectAndroidLayer method
    private void detectAndroidLayer(CodeComponent component) {
        // Add null check for component name
        if (component.getName() == null) {
            component.setLayer("Unknown");
            return;
        }

        if (component.getExtendsClass() != null) {
            String extendsClass = component.getExtendsClass();

            // More specific UI Layer detection
            if (extendsClass.endsWith("Activity") ||
                    extendsClass.endsWith("Fragment") ||
                    extendsClass.endsWith("AppCompatActivity") ||
                    extendsClass.endsWith("DialogFragment") ||
                    extendsClass.endsWith("ComposeActivity") ||
                    extendsClass.contains("android.app.Activity") ||
                    extendsClass.contains("androidx.fragment.app.Fragment")) {
                component.setLayer("UI");
                return;
            }
            // Business Logic Layer detection
            else if (extendsClass.endsWith("ViewModel") ||
                    extendsClass.endsWith("Presenter") ||
                    extendsClass.endsWith("Controller")) {
                component.setLayer("Business Logic");
                return;
            }
            // Data Layer detection
            else if (extendsClass.endsWith("Repository") ||
                    extendsClass.endsWith("DataSource") ||
                    extendsClass.endsWith("Dao")) {
                component.setLayer("Data");
                return;
            }
        }

        // More specific detection based on class name patterns
        String className = component.getName().toLowerCase();
        if (className.endsWith("activity") ||
                className.endsWith("fragment") ||
                className.endsWith("adapter") ||
                className.endsWith("viewholder") ||
                className.endsWith("screen") ||
                className.endsWith("composable") ||
                (className.contains("view") && !className.contains("modelview") && !className.contains("viewmodel"))) {
            component.setLayer("UI");
        } else if (className.endsWith("viewmodel") ||
                className.endsWith("presenter") ||
                className.endsWith("controller") ||
                className.endsWith("usecase")) {
            component.setLayer("Business Logic");
        } else if (className.endsWith("repository") ||
                className.endsWith("datasource") ||
                className.endsWith("dao") ||
                className.endsWith("service")) {
            component.setLayer("Data");
        } else {
            component.setLayer("Unknown");
        }
    }

    private String findLayoutFileForComponent(File kotlinFile, String componentName) {
        if (componentName == null) {
            return null;
        }

        // Convert component name to layout name convention
        String baseName = componentName.toLowerCase()
                .replace("activity", "")
                .replace("fragment", "")
                .replace("composable", "")
                .replace("screen", "");

        String layoutName = baseName.isEmpty() ? "activity_main" : "activity_" + baseName;

        // Look for layout file in res/layout directory
        File projectDir = kotlinFile.getParentFile().getParentFile(); // Move up from src/main/java
        File layoutDir = new File(projectDir, "src/main/res/layout");

        if (layoutDir.exists() && layoutDir.isDirectory()) {
            File[] layoutFiles = layoutDir.listFiles((dir, name) ->
                    name.startsWith(layoutName) && name.endsWith(".xml"));

            if (layoutFiles != null && layoutFiles.length > 0) {
                return layoutFiles[0].getName();
            }
        }

        return null;
    }

    private void analyzeDaggerHiltComponents(KtFile ktFile, CodeComponent component) {
        // Analyze for Dagger annotations
        for (String annotation : component.getAnnotations()) {
            if (annotation != null) {
                if (annotation.contains("@Component") || annotation.contains("@Subcomponent")) {
                    component.setHasDaggerInjection(true);
                    component.setDaggerComponentType(extractComponentType(annotation));
                }
                if (annotation.contains("@AndroidEntryPoint") || annotation.contains("@HiltAndroidApp")) {
                    component.setHiltComponent(true);
                    component.setHiltComponentType(extractHiltComponentType(annotation));
                }
            }
        }

        // Analyze injected dependencies for Dagger
        for (String injectedDep : component.getInjectedDependencies()) {
            if (injectedDep != null) {
                component.getDaggerInjectedDependencies().add(injectedDep);
            }
        }
    }

    private String extractComponentType(String annotation) {
        if (annotation.contains("SingletonComponent")) return "Singleton";
        if (annotation.contains("ActivityComponent")) return "Activity";
        if (annotation.contains("FragmentComponent")) return "Fragment";
        if (annotation.contains("ViewComponent")) return "View";
        return "Component";
    }

    private String extractHiltComponentType(String annotation) {
        if (annotation.contains("@HiltAndroidApp")) return "Application";
        if (annotation.contains("@AndroidEntryPoint")) {
            if (componentHasType(annotation, "Activity")) return "Activity";
            if (componentHasType(annotation, "Fragment")) return "Fragment";
            if (componentHasType(annotation, "View")) return "View";
            if (componentHasType(annotation, "Service")) return "Service";
        }
        return "AndroidEntryPoint";
    }

    private boolean componentHasType(String annotation, String type) {
        return annotation != null && type != null && annotation.toLowerCase().contains(type.toLowerCase());
    }

    private String getVisibility(KtModifierListOwner owner) {
        if (owner.hasModifier(KtTokens.PUBLIC_KEYWORD)) return "public";
        if (owner.hasModifier(KtTokens.PRIVATE_KEYWORD)) return "private";
        if (owner.hasModifier(KtTokens.PROTECTED_KEYWORD)) return "protected";
        if (owner.hasModifier(KtTokens.INTERNAL_KEYWORD)) return "internal";
        return "public"; // default in Kotlin is public
    }

    private boolean hasInjectAnnotation(List<KtAnnotationEntry> annotations) {
        for (KtAnnotationEntry ann : annotations) {
            if (ann.getShortName() != null) {
                String annName = ann.getShortName().asString().toLowerCase();
                if (annName.contains("inject") || annName.contains("autowired")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasKoinInjection(KtProperty property) {
        if (property == null) return false;

        String text = property.getText();
        return text != null && (text.contains("by inject()") || text.contains("by viewModel()") ||
                text.contains("by koin.inject()") || text.contains("get()"));
    }

    // Utility method for primitive type checking
    private static boolean isPrimitiveType(String type) {
        if (type == null) return false;

        switch (type.toLowerCase()) {
            case "byte":
            case "short":
            case "int":
            case "long":
            case "float":
            case "double":
            case "boolean":
            case "char":
            case "string":
            case "unit":
            case "any":
                return true;
            default:
                return false;
        }
    }


}