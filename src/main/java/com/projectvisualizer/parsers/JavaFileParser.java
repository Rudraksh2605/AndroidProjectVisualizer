package com.projectvisualizer.parsers;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.projectvisualizer.models.CodeComponent;
import com.projectvisualizer.models.CodeField;
import com.projectvisualizer.models.CodeMethod;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class JavaFileParser {

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
            }
        }

        return components;
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
            n.getImplementedTypes().forEach(t -> implementsList.add(t.getNameAsString()));
            component.setImplementsList(implementsList);

            components.add(component);
            super.visit(n, components);
        }
    }

    private static class FieldVisitor extends VoidVisitorAdapter<CodeComponent> {
        @Override
        public void visit(FieldDeclaration n, CodeComponent component) {
            n.getVariables().forEach(v -> {
                CodeField field = new CodeField();
                field.setName(v.getNameAsString());
                field.setType(n.getElementType().asString());
                field.setVisibility(n.getAccessSpecifier().asString());

                // Check for dependency injection annotations
                boolean isInjected = n.getAnnotations().stream()
                        .anyMatch(ann -> {
                            String annName = ann.getNameAsString().toLowerCase();
                            return annName.contains("autowired") ||
                                    annName.contains("inject") ||
                                    annName.contains("resource");
                        });

                if (isInjected) {
                    component.getInjectedDependencies().add(n.getElementType().asString());
                }

                component.getFields().add(field);

                // Add dependency if it's a custom type
                String fieldType = n.getElementType().asString();
                if (!isPrimitiveType(fieldType) && !fieldType.startsWith("java.")) {
                    CodeComponent dependency = new CodeComponent();
                    dependency.setId(fieldType);
                    dependency.setName(fieldType);
                    dependency.setType("class");
                    component.getDependencies().add(dependency);
                }
            });
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
            n.getParameters().forEach(p -> parameters.add(p.getNameAsString() + ": " + p.getType().asString()));
            method.setParameters(parameters);

            component.getMethods().add(method);
            super.visit(n, component);
        }
    }

    private static class ConstructorVisitor extends VoidVisitorAdapter<CodeComponent> {
        @Override
        public void visit(ConstructorDeclaration n, CodeComponent component) {
            boolean isInjected = n.getAnnotations().stream()
                    .anyMatch(ann -> {
                        String annName = ann.getNameAsString().toLowerCase();
                        return annName.contains("autowired") ||
                                annName.contains("inject");
                    });

            if (isInjected) {
                n.getParameters().forEach(p -> {
                    String paramType = p.getType().asString();
                    if (!isPrimitiveType(paramType) && !paramType.startsWith("java.")) {
                        component.getInjectedDependencies().add(paramType);
                    }
                });
            }
            super.visit(n, component);
        }
    }

    // Utility method for primitive type checking
    private static boolean isPrimitiveType(String type) {
        switch(type) {
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
