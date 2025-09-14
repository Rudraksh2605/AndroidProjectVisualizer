// DaggerHiltAnalyzer.java
package com.projectvisualizer.parsers;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.projectvisualizer.models.CodeComponent;

import java.util.ArrayList;
import java.util.List;

public class DaggerHiltAnalyzer {

    public void analyzeDaggerComponents(CompilationUnit cu, CodeComponent component) {
        cu.accept(new DaggerComponentVisitor(), component);
    }

    public void analyzeHiltComponents(CompilationUnit cu, CodeComponent component) {
        cu.accept(new HiltComponentVisitor(), component);
    }

    private static class DaggerComponentVisitor extends VoidVisitorAdapter<CodeComponent> {
        @Override
        public void visit(ClassOrInterfaceDeclaration n, CodeComponent component) {
            // Check for Dagger components
            for (AnnotationExpr ann : n.getAnnotations()) {
                String annName = ann.getNameAsString();
                if (annName.equals("Component") || annName.equals("Subcomponent")) {
                    component.setDaggerComponentType(annName);

                    // Parse component dependencies
                    if (n.getImplementedTypes() != null) {
                        List<String> dependencies = new ArrayList<>();
                        for (var impl : n.getImplementedTypes()) {
                            dependencies.add(impl.getNameAsString());
                        }
                        component.setDaggerDependencies(dependencies);
                    }
                }
            }
            super.visit(n, component);
        }

        @Override
        public void visit(FieldDeclaration n, CodeComponent component) {
            // Check for Dagger injections
            for (AnnotationExpr ann : n.getAnnotations()) {
                String annName = ann.getNameAsString();
                if (annName.equals("Inject")) {
                    component.setHasDaggerInjection(true);

                    // Add the field type as a dependency
                    String fieldType = n.getElementType().asString();
                    if (!isPrimitiveType(fieldType) && !fieldType.startsWith("java.")) {
                        component.getDaggerInjectedDependencies().add(fieldType);
                    }
                }
            }
            super.visit(n, component);
        }
    }

    private static class HiltComponentVisitor extends VoidVisitorAdapter<CodeComponent> {
        @Override
        public void visit(ClassOrInterfaceDeclaration n, CodeComponent component) {
            // Check for Hilt annotations
            for (AnnotationExpr ann : n.getAnnotations()) {
                String annName = ann.getNameAsString();
                if (annName.equals("HiltAndroidApp") || annName.equals("AndroidEntryPoint")) {
                    component.setHiltComponent(true);
                    component.setHiltComponentType(annName);
                }
            }
            super.visit(n, component);
        }
    }

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