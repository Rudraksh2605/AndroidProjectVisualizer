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

            components.add(component);
        }

        return components;
    }
}