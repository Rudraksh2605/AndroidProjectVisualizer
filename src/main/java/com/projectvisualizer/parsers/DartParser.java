package com.projectvisualizer.parsers;

import com.projectvisualizer.models.CodeComponent;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class DartParser {
    public List<CodeComponent> parse(File file) throws Exception {
        List<CodeComponent> components = new ArrayList<>();
        String content = new String(Files.readAllBytes(file.toPath()));

        // Pattern to match class definitions with extends and implements
        Pattern pattern = Pattern.compile("class\\s+(\\w+)(\\s+extends\\s+([^\\s{]+))?(\\s+implements\\s+([^\\s{]+))?");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String name = matcher.group(1);
            String extendsClass = matcher.group(3);
            String implementsList = matcher.group(5);

            CodeComponent component = new CodeComponent();
            component.setId(name);
            component.setName(name);
            component.setType("class");
            component.setFilePath(file.getAbsolutePath());
            component.setLanguage("dart");

            if (extendsClass != null) {
                component.setExtendsClass(extendsClass);
            }

            if (implementsList != null) {
                String[] interfaces = implementsList.split(",");
                for (String iface : interfaces) {
                    component.getImplementsList().add(iface.trim());
                }
            }

            // Look for dependency injection in Dart (get_it, injectable, etc.)
            findDartInjections(content, component);

            components.add(component);
        }

        // Pattern to match widget definitions
        Pattern widgetPattern = Pattern.compile("class\\s+(\\w+)\\s+extends\\s+(StatelessWidget|StatefulWidget|Widget)");
        Matcher widgetMatcher = widgetPattern.matcher(content);

        while (widgetMatcher.find()) {
            String name = widgetMatcher.group(1);
            String widgetType = widgetMatcher.group(2);

            CodeComponent component = new CodeComponent();
            component.setId(name);
            component.setName(name);
            component.setType("widget");
            component.setFilePath(file.getAbsolutePath());
            component.setLanguage("dart");
            component.setExtendsClass(widgetType);

            // Look for dependency injection in widgets
            findDartInjections(content, component);

            components.add(component);
        }

        return components;
    }

    private void findDartInjections(String content, CodeComponent component) {
        // Pattern for get_it dependency injection
        Pattern getItPattern = Pattern.compile("getIt\\.get<(\\w+)>\\(\\)");
        Matcher getItMatcher = getItPattern.matcher(content);

        while (getItMatcher.find()) {
            String dependencyType = getItMatcher.group(1);
            component.getInjectedDependencies().add(dependencyType);
        }

        // Pattern for injectable's @inject annotation
        Pattern injectablePattern = Pattern.compile("@inject\\s+[^\\n]*\\s+(\\w+)\\s+([^;]+);");
        Matcher injectableMatcher = injectablePattern.matcher(content);

        while (injectableMatcher.find()) {
            String dependencyType = injectableMatcher.group(1);
            component.getInjectedDependencies().add(dependencyType);
        }

        // Pattern for constructor injection with @inject annotation
        Pattern constructorPattern = Pattern.compile("@inject\\s+[^\\n]*\\s+(\\w+)\\s*\\([^)]*\\)");
        Matcher constructorMatcher = constructorPattern.matcher(content);

        if (constructorMatcher.find()) {
            String className = constructorMatcher.group(1);

            // Extract constructor parameters
            Pattern paramPattern = Pattern.compile("this\\.(\\w+)\\s*:\\s*super\\.key");
            Matcher paramMatcher = paramPattern.matcher(constructorMatcher.group(0));

            while (paramMatcher.find()) {
                String paramName = paramMatcher.group(1);
                // We'll need to infer the type from field declarations
                Pattern fieldPattern = Pattern.compile("final\\s+(\\w+)\\s+" + paramName + "\\s*;");
                Matcher fieldMatcher = fieldPattern.matcher(content);

                if (fieldMatcher.find()) {
                    String dependencyType = fieldMatcher.group(1);
                    component.getInjectedDependencies().add(dependencyType);
                }
            }
        }
    }
}