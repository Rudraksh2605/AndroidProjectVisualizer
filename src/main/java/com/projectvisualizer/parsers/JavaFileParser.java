package com.projectvisualizer.parsers;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.projectvisualizer.model.*;
import com.projectvisualizer.parsers.DaggerHiltAnalyzer;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JavaFileParser {

    private Map<String, String> activityLayoutMap;
    private IntentAnalyzer intentAnalyzer = new IntentAnalyzer();
    private ScreenFlowDetector screenFlowDetector = new ScreenFlowDetector();
    private static final DaggerHiltAnalyzer DAGGER_HILT_ANALYZER = new DaggerHiltAnalyzer();


    public JavaFileParser() {
        // Default constructor
    }

    public JavaFileParser(Map<String, String> activityLayoutMap) {
        this.activityLayoutMap = activityLayoutMap;
    }

    public List<CodeComponent> parse(File javaFile) throws Exception {
        List<CodeComponent> components = new ArrayList<>();

        // Reuse analyzer instances instead of creating new ones
        List<NavigationFlow> intentFlows = intentAnalyzer.analyzeIntentFlows(javaFile);

        try (FileInputStream in = new FileInputStream(javaFile)) {
            CompilationUnit cu = StaticJavaParser.parse(in);

            String packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString())
                    .orElse("");

            // Collect imports
            List<String> imports = new ArrayList<>();
            cu.getImports().forEach(imp -> imports.add(imp.getNameAsString()));

            String moduleName = deriveModuleName(javaFile);

            cu.accept(new ClassVisitor(javaFile, packageName, imports, moduleName), components);

            components.parallelStream().forEach(component -> {
                cu.accept(new FieldVisitor(), component);
                cu.accept(new MethodVisitor(), component);
                cu.accept(new EnhancedMethodVisitor(), component);
                cu.accept(new ConstructorVisitor(), component);
                cu.accept(new AnnotationVisitor(), component);
                cu.accept(new InheritanceVisitor(), component);

                // Dagger/Hilt analysis
                DAGGER_HILT_ANALYZER.analyzeDaggerComponents(cu, component);
                DAGGER_HILT_ANALYZER.analyzeHiltComponents(cu, component);

                // Android Layer detector
                detectAndroidLayer(component);

                // Associate UI components via Activity layout mapping
                if (activityLayoutMap != null && component.getType().equals("class")) {
                    String fullClassName = component.getId();
                    if (activityLayoutMap.containsKey(fullClassName)) {
                        component.setLayer("UI");
                        component.setManifestRegistered(true);
                        component.setComponentType("Activity");
                        String layoutFile = findLayoutFileForActivity(javaFile, component.getName());
                        if (layoutFile != null) {
                            component.addLayoutFile(layoutFile);
                        }
                    }
                }
            });

            // --- NEW: Attach analyzed Intent Flows as formal CodeComponents ---
            for (CodeComponent component : components) {
                if (intentFlows != null && !intentFlows.isEmpty()) {
                    for (NavigationFlow flow : intentFlows) {
                        // Match flow source to current component name
                        // We use a lenient match since sourceScreenId might be simple name or qualified
                        if (component.getName().equals(flow.getSourceScreenId()) ||
                                (component.getId() != null && component.getId().endsWith("." + flow.getSourceScreenId()))) {

                            CodeComponent intentComp = new CodeComponent();
                            intentComp.setId(UUID.randomUUID().toString());
                            intentComp.setName("Intent to " + flow.getTargetScreenId()); // Descriptive name
                            intentComp.setType("Intent");
                            intentComp.setLayer("UI");

                            // Add target as explicit dependency for robustness
                            CodeComponent targetDep = new CodeComponent();
                            targetDep.setId("dep_" + flow.getTargetScreenId());
                            targetDep.setName(flow.getTargetScreenId());
                            targetDep.setType("Activity");
                            intentComp.addDependency(targetDep);

                            component.addIntent(intentComp);
                        }
                    }
                }
            }

        }

        return components;
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

    private void detectAndroidLayer(CodeComponent component) {
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
                className.contains("view") && !className.contains("modelview") && !className.contains("viewmodel")) {
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

    private String findLayoutFileForActivity(File javaFile, String activityName) {
        // Convert Activity name to layout name convention (MyActivity -> activity_my)
        String baseName = activityName.toLowerCase()
                .replace("activity", "")
                .replace("fragment", "")
                .replace("adapter", "");

        // Create a final variable for use in lambda
        final String layoutName = baseName.isEmpty() ? "activity_main" : "activity_" + baseName;

        // Look for layout file in res/layout directory
        File projectDir = javaFile.getParentFile().getParentFile(); // Move up from src/main/java
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

    private static class ClassVisitor extends VoidVisitorAdapter<List<CodeComponent>> {
        private final File javaFile;
        private final String packageName;
        private final List<String> imports;
        private final String moduleName;

        public ClassVisitor(File javaFile, String packageName, List<String> imports, String moduleName) {
            this.javaFile = javaFile;
            this.packageName = packageName;
            this.imports = imports;
            this.moduleName = moduleName;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, List<CodeComponent> components) {
            CodeComponent component = new CodeComponent();
            String className = n.getNameAsString();
            String fullClassName = packageName.isEmpty() ? className : packageName + "." + className;

            component.setId(fullClassName);
            component.setName(className);
            component.setType(n.isInterface() ? "interface" : "class");
            component.setFilePath(javaFile.getAbsolutePath());
            component.setLanguage("java");
            component.setPackageName(packageName);
            component.setModuleName(moduleName);
            component.setImports(new ArrayList<>(imports));
            component.setFileExtension("java");

            // Modifiers
            List<String> modifiers = new ArrayList<>();
            n.getModifiers().forEach(m -> modifiers.add(m.getKeyword().asString()));
            component.setModifiers(modifiers);

            if (!n.getExtendedTypes().isEmpty()) {
                component.setExtendsClass(n.getExtendedTypes().get(0).getNameAsString());
            }

            List<String> implementsList = new ArrayList<>();
            for (var t : n.getImplementedTypes()) {
                implementsList.add(t.getNameAsString());
            }
            component.setImplementsList(implementsList);

            components.add(component);
            super.visit(n, components);
        }
    }

    private static class FieldVisitor extends VoidVisitorAdapter<CodeComponent> {
        @Override
        public void visit(FieldDeclaration n, CodeComponent component) {
            for (var v : n.getVariables()) {
                CodeField codeField = new CodeField();
                codeField.setName(v.getNameAsString());
                codeField.setType(n.getElementType().asString());
                codeField.setVisibility(n.getAccessSpecifier().asString());

                // Check for dependency injection annotations
                boolean isInjected = false;
                for (var ann : n.getAnnotations()) {
                    String annName = ann.getNameAsString().toLowerCase();
                    if (annName.contains("autowired") || annName.contains("inject") || annName.contains("resource")) {
                        isInjected = true;
                        break;
                    }
                }

                if (isInjected) {
                    component.getInjectedDependencies().add(n.getElementType().asString());
                }

                component.getFields().add(codeField);

                // Add dependency if it's a custom type
                String fieldType = n.getElementType().asString();
                if (!isPrimitiveType(fieldType) && !fieldType.startsWith("java.")) {
                    CodeComponent dependency = new CodeComponent();
                    dependency.setId(fieldType);
                    dependency.setName(fieldType);
                    dependency.setType("class");
                    component.getDependencies().add(dependency);
                }
            }
            super.visit(n, component);
        }
    }

    private static class MethodVisitor extends VoidVisitorAdapter<CodeComponent> {
        @Override
        public void visit(MethodDeclaration n, CodeComponent component) {
            CodeMethod method = new CodeMethod();
            method.setName(n.getNameAsString());
            method.setReturnType(n.getType().asString());
            method.setVisibility(n.getAccessSpecifier().asString());

            List<String> parameters = new ArrayList<>();
            for (var p : n.getParameters()) {
                parameters.add(p.getNameAsString() + ": " + p.getType().asString());
            }
            method.setParameters(parameters);

            component.getMethods().add(method);
            super.visit(n, component);
        }
    }

    private static class ConstructorVisitor extends VoidVisitorAdapter<CodeComponent> {
        @Override
        public void visit(ConstructorDeclaration n, CodeComponent component) {
            boolean isInjected = false;
            for (var ann : n.getAnnotations()) {
                String annName = ann.getNameAsString().toLowerCase();
                if (annName.contains("autowired") || annName.contains("inject")) {
                    isInjected = true;
                    break;
                }
            }

            if (isInjected) {
                for (var p : n.getParameters()) {
                    String paramType = p.getType().asString();
                    if (!isPrimitiveType(paramType) && !paramType.startsWith("java.")) {
                        component.getInjectedDependencies().add(paramType);
                    }
                }
            }
            super.visit(n, component);
        }
    }

    private static class AnnotationVisitor extends VoidVisitorAdapter<CodeComponent> {
        @Override
        public void visit(ClassOrInterfaceDeclaration n, CodeComponent component) {
            List<String> annotations = new ArrayList<>();
            for (AnnotationExpr annotation : n.getAnnotations()) {
                annotations.add(annotation.getNameAsString());
            }
            component.setAnnotations(annotations);
            super.visit(n, component);
        }
    }

    // Utility method for primitive type checking
    private static boolean isPrimitiveType(String type) {
        switch (type) {
            case "byte":
            case "short":
            case "int":
            case "long":
            case "float":
            case "double":
            case "boolean":
            case "char":
                return true;
            default:
                return false;
        }
    }

    private static class EnhancedMethodVisitor extends VoidVisitorAdapter<CodeComponent> {
        @Override
        public void visit(MethodDeclaration n, CodeComponent component) {
            // Check return type
            String returnType = n.getType().asString();
            if (!isPrimitiveType(returnType) && !returnType.startsWith("java.")) {
                addDependency(component, returnType);
            }

            // Check parameter types
            for (var parameter : n.getParameters()) {
                String paramType = parameter.getType().asString();
                if (!isPrimitiveType(paramType) && !paramType.startsWith("java.")) {
                    addDependency(component, paramType);
                }
            }
            super.visit(n, component);
        }

        private void addDependency(CodeComponent component, String typeName) {
            // Avoid duplicates
            boolean exists = component.getDependencies().stream()
                    .anyMatch(dep -> dep.getId().equals(typeName));

            if (!exists) {
                CodeComponent dependency = new CodeComponent();
                dependency.setId(typeName);
                dependency.setName(typeName);
                dependency.setType("class");
                component.getDependencies().add(dependency);
            }
        }
    }

    private static class InheritanceVisitor extends VoidVisitorAdapter<CodeComponent> {
        @Override
        public void visit(ClassOrInterfaceDeclaration n, CodeComponent component) {
            // Handle extends
            if (!n.getExtendedTypes().isEmpty()) {
                String extendsType = n.getExtendedTypes().get(0).getNameAsString();
                if (!isPrimitiveType(extendsType) && !extendsType.startsWith("java.")) {
                    addDependency(component, extendsType);
                }
            }

            // Handle implements
            for (var implementedType : n.getImplementedTypes()) {
                String implType = implementedType.getNameAsString();
                if (!isPrimitiveType(implType) && !implType.startsWith("java.")) {
                    addDependency(component, implType);
                }
            }
            super.visit(n, component);
        }

        private void addDependency(CodeComponent component, String typeName) {
            boolean exists = component.getDependencies().stream()
                    .anyMatch(dep -> dep.getId().equals(typeName));

            if (!exists) {
                CodeComponent dependency = new CodeComponent();
                dependency.setId(typeName);
                dependency.setName(typeName);
                dependency.setType("class");
                component.getDependencies().add(dependency);
            }
        }
    }
}