package com.projectvisualizer.parsers;

import com.projectvisualizer.models.CodeComponent;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class JavaScriptParser {
    public List<CodeComponent> parse(File file) throws Exception {
        List<CodeComponent> components = new ArrayList<>();
        String content = new String(Files.readAllBytes(file.toPath()));
        String fileName = file.getName();

        // Check if this is a React component file
        boolean isReact = content.contains("import React") || content.contains("from 'react'") ||
                content.contains("import {") && content.contains("} from 'react'");

        // Pattern to match class components
        Pattern classPattern = Pattern.compile("class\\s+(\\w+)\\s+extends\\s+([^{]+)");
        Matcher classMatcher = classPattern.matcher(content);

        while (classMatcher.find()) {
            String name = classMatcher.group(1);
            String extendsClass = classMatcher.group(2).trim();

            CodeComponent component = new CodeComponent();
            component.setId(name);
            component.setName(name);
            component.setType(isReact ? "component" : "class");
            component.setFilePath(file.getAbsolutePath());
            component.setLanguage(fileName.endsWith(".ts") || fileName.endsWith(".tsx") ? "typescript" : "javascript");

            if (extendsClass.contains("Component") || extendsClass.contains("React")) {
                component.setExtendsClass(extendsClass);
            }

            // Look for dependency injection in JavaScript/TypeScript
            findJsInjections(content, component);

            components.add(component);
        }

        // Pattern to match function components (React)
        if (isReact) {
            Pattern functionPattern = Pattern.compile("const\\s+(\\w+)\\s*=\\s*\\([^)]*\\)\\s*=>\\s*");
            Matcher functionMatcher = functionPattern.matcher(content);

            while (functionMatcher.find()) {
                String name = functionMatcher.group(1);

                CodeComponent component = new CodeComponent();
                component.setId(name);
                component.setName(name);
                component.setType("component");
                component.setFilePath(file.getAbsolutePath());
                component.setLanguage(fileName.endsWith(".ts") || fileName.endsWith(".tsx") ? "typescript" : "javascript");

                // Look for dependency injection in function components
                findJsInjections(content, component);

                components.add(component);
            }

            // Pattern for function declarations
            Pattern functionDeclPattern = Pattern.compile("function\\s+(\\w+)\\s*\\([^)]*\\)\\s*");
            Matcher functionDeclMatcher = functionDeclPattern.matcher(content);

            while (functionDeclMatcher.find()) {
                String name = functionDeclMatcher.group(1);

                CodeComponent component = new CodeComponent();
                component.setId(name);
                component.setName(name);
                component.setType("component");
                component.setFilePath(file.getAbsolutePath());
                component.setLanguage(fileName.endsWith(".ts") || fileName.endsWith(".tsx") ? "typescript" : "javascript");

                // Look for dependency injection in function components
                findJsInjections(content, component);

                components.add(component);
            }
        }

        return components;
    }

    private void findJsInjections(String content, CodeComponent component) {
        // Pattern for Angular-style constructor injection
        Pattern angularPattern = Pattern.compile("constructor\\s*\\([^)]*\\b(private|public|readonly)?\\s*([^:)]+)\\s*:[^)]*\\)");
        Matcher angularMatcher = angularPattern.matcher(content);

        while (angularMatcher.find()) {
            String dependencyType = angularMatcher.group(2).trim();
            component.getInjectedDependencies().add(dependencyType);
        }

        // Pattern for property injection with decorators
        Pattern decoratorPattern = Pattern.compile("@(Inject|Injectable|Autowired)\\s*(\\([^)]*\\))?\\s*[^=]*=[^;]*;");
        Matcher decoratorMatcher = decoratorPattern.matcher(content);

        while (decoratorMatcher.find()) {
            // Extract the type from the assignment
            Pattern typePattern = Pattern.compile(":\\s*(\\w+)");
            Matcher typeMatcher = typePattern.matcher(decoratorMatcher.group(0));

            if (typeMatcher.find()) {
                String dependencyType = typeMatcher.group(1);
                component.getInjectedDependencies().add(dependencyType);
            }
        }

        // Pattern for React context or hooks
        Pattern useContextPattern = Pattern.compile("useContext\\(([^)]+)\\)");
        Matcher useContextMatcher = useContextPattern.matcher(content);

        while (useContextMatcher.find()) {
            String contextType = useContextMatcher.group(1);
            component.getInjectedDependencies().add(contextType);
        }

        // Pattern for dependency injection libraries (Inversify, TSyringe, etc.)
        Pattern diLibPattern = Pattern.compile("(container\\.get|inject|resolve)\\(([^)]+)\\)");
        Matcher diLibMatcher = diLibPattern.matcher(content);

        while (diLibMatcher.find()) {
            String dependencyType = diLibMatcher.group(2);
            component.getInjectedDependencies().add(dependencyType);
        }
    }
}