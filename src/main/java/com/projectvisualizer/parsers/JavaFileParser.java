package com.projectvisualizer.parsers;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.projectvisualizer.models.CodeComponent;
import com.projectvisualizer.models.CodeField;
import com.projectvisualizer.models.CodeMethod;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JavaFileParser {

    private Map<String, String> activityLayoutMap; // For resource mapping

    public JavaFileParser() {
        // Default constructor
    }

    public JavaFileParser(Map<String, String> activityLayoutMap) {
        this.activityLayoutMap = activityLayoutMap;
    }

    public List<CodeComponent> parse(File javaFile) throws Exception {
        List<CodeComponent> components = new ArrayList<>();

        try (FileInputStream in = new FileInputStream(javaFile)) {
            CompilationUnit cu = StaticJavaParser.parse(in);

            String packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString())
                    .orElse("");

            cu.accept(new ClassVisitor(javaFile, packageName), components);

            for (CodeComponent component : components) {
                cu.accept(new FieldVisitor(), component);
                cu.accept(new MethodVisitor(), component);
                cu.accept(new ConstructorVisitor(), component);
                cu.accept(new AnnotationVisitor(), component);

                detectAndroidLayer(component);

                if (activityLayoutMap != null && component.getType().equals("class")) {
                    String fullClassName = packageName + "." + component.getName();
                    if (activityLayoutMap.containsKey(fullClassName)) {
                        component.setLayer("UI");
                        // Try to find associated layout file
                        String layoutFile = findLayoutFileForActivity(javaFile, component.getName());
                        if (layoutFile != null) {
                            component.addLayoutFile(layoutFile);
                        }
                    }
                }
            }
        }

        return components;
    }

    private void detectAndroidLayer(CodeComponent component) {
        if (component.getExtendsClass() != null) {
            String extendsClass = component.getExtendsClass();

            // UI Layer detection
            if (extendsClass.contains("Activity") || extendsClass.contains("Fragment") ||
                    extendsClass.contains("AppCompatActivity") || extendsClass.contains("DialogFragment")) {
                component.setLayer("UI");
            }
            // Business Logic Layer detection
            else if (extendsClass.contains("ViewModel") || extendsClass.contains("Presenter") ||
                    extendsClass.contains("Controller")) {
                component.setLayer("Business Logic");
            }
            // Data Layer detection
            else if (extendsClass.contains("Repository") || extendsClass.contains("DataSource") ||
                    extendsClass.contains("Dao")) {
                component.setLayer("Data");
            }
        }

        // Additional detection based on class name patterns
        String className = component.getName().toLowerCase();
        if (className.endsWith("activity") || className.endsWith("fragment") ||
                className.endsWith("adapter") || className.endsWith("viewholder")) {
            component.setLayer("UI");
        } else if (className.endsWith("viewmodel") || className.endsWith("presenter") ||
                className.endsWith("controller")) {
            component.setLayer("Business Logic");
        } else if (className.endsWith("repository") || className.endsWith("datasource") ||
                className.endsWith("dao") || className.endsWith("service")) {
            component.setLayer("Data");
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

        public ClassVisitor(File javaFile, String packageName) {
            this.javaFile = javaFile;
            this.packageName = packageName;
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
}