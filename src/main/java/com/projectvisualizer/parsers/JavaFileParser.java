package com.projectvisualizer.parsers;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.projectvisualizer.models.CodeComponent;
import com.projectvisualizer.models.CodeField;
import com.projectvisualizer.models.CodeMethod;

import com.github.javaparser.StaticJavaParser;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class JavaFileParser {

    public List<CodeComponent> parse(File javaFile) throws Exception {
        List<CodeComponent> components = new ArrayList<>();

        try (FileInputStream in = new FileInputStream(javaFile)) {
            CompilationUnit cu = StaticJavaParser.parse(in);

            // Extract package name
            String packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString())
                    .orElse("");

            // Extract class information
            cu.accept(new ClassVisitor(javaFile, packageName), components);

            // For each component, extract fields and methods
            for (CodeComponent component : components) {
                cu.accept(new FieldVisitor(), component);
                cu.accept(new MethodVisitor(), component);
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

            // Get extends and implements
            if (n.getExtendedTypes().isNonEmpty()) {
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
                component.getFields().add(field);

                // Add dependency if it's a custom type
                String fieldType = n.getElementType().asString();
                if (!isPrimitiveType(fieldType) && !fieldType.startsWith("java.")) {
                    // This would need to be enhanced to handle imports properly
                    // For now, we'll just add the type name as a dependency
                    CodeComponent dependency = new CodeComponent();
                    dependency.setId(fieldType);
                    dependency.setName(fieldType);
                    dependency.setType("class"); // Assume it's a class
                    component.getDependencies().add(dependency);
                }
            });
            super.visit(n, component);
        }

        private boolean isPrimitiveType(String type) {
            return type.equals("int") || type.equals("long") || type.equals("double") ||
                    type.equals("float") || type.equals("boolean") || type.equals("char") ||
                    type.equals("byte") || type.equals("short") || type.equals("void");
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
}