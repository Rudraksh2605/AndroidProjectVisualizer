package com.projectvisualizer.parsers;

import com.projectvisualizer.models.CodeComponent;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class XmlParser {
    public List<CodeComponent> parse(File file) throws Exception {
        List<CodeComponent> components = new ArrayList<>();
        String content = new String(Files.readAllBytes(file.toPath()));
        String fileName = file.getName();

        // For Android XML files, check if it's a layout file
        boolean isLayout = fileName.startsWith("activity_") || fileName.startsWith("fragment_") ||
                fileName.startsWith("dialog_") || fileName.startsWith("item_") ||
                file.getParentFile().getName().equals("layout");

        if (isLayout) {
            CodeComponent component = new CodeComponent();
            component.setId(fileName);
            component.setName(fileName.replace(".xml", ""));
            component.setType("layout");
            component.setFilePath(file.getAbsolutePath());
            component.setLanguage("xml");

            // Find custom views used in this layout
            Pattern customViewPattern = Pattern.compile("<([a-zA-Z0-9]+\\.\\w+)\\w*");
            Matcher customViewMatcher = customViewPattern.matcher(content);

            while (customViewMatcher.find()) {
                String viewName = customViewMatcher.group(1);
                CodeComponent dependency = new CodeComponent();
                dependency.setId(viewName);
                dependency.setName(viewName);
                dependency.setType("view");
                component.getDependencies().add(dependency);
            }

            components.add(component);
        }

        return components;
    }
}