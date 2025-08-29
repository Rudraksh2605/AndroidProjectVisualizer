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

                components.add(component);
            }
        }

        return components;
    }
}