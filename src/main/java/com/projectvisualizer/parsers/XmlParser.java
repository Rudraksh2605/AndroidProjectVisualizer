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

        // For Spring XML configuration files
        boolean isSpringConfig = fileName.endsWith("-context.xml") ||
                fileName.endsWith("-config.xml") ||
                content.contains("<beans") ||
                content.contains("<context:component-scan");

        if (isSpringConfig) {
            // Parse bean definitions
            Pattern beanPattern = Pattern.compile("<bean\\s+[^>]*class\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>");
            Matcher beanMatcher = beanPattern.matcher(content);

            while (beanMatcher.find()) {
                String className = beanMatcher.group(1);

                CodeComponent component = new CodeComponent();
                component.setId(className);
                component.setName(className.substring(className.lastIndexOf('.') + 1));
                component.setType("bean");
                component.setFilePath(file.getAbsolutePath());
                component.setLanguage("xml");

                // Find dependencies for this bean
                findSpringDependencies(content, component, className);

                components.add(component);
            }

            // Parse component scan to find all beans in the scanned packages
            Pattern componentScanPattern = Pattern.compile("<context:component-scan\\s+[^>]*base-package\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>");
            Matcher componentScanMatcher = componentScanPattern.matcher(content);

            // Note: This would need to be combined with Java file parsing to find actual components
            while (componentScanMatcher.find()) {
                String basePackage = componentScanMatcher.group(1);
                // We can't parse the actual components from XML alone
                // This would need integration with the Java parser
            }
        }

        return components;
    }

    private void findSpringDependencies(String content, CodeComponent component, String className) {
        // Find constructor arguments
        Pattern constructorArgPattern = Pattern.compile("<constructor-arg[^>]*ref\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>");
        Matcher constructorArgMatcher = constructorArgPattern.matcher(content);

        while (constructorArgMatcher.find()) {
            String refBean = constructorArgMatcher.group(1);
            component.getInjectedDependencies().add(refBean);
        }

        // Find property injections
        Pattern propertyPattern = Pattern.compile("<property[^>]*ref\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>");
        Matcher propertyMatcher = propertyPattern.matcher(content);

        while (propertyMatcher.find()) {
            String refBean = propertyMatcher.group(1);
            component.getInjectedDependencies().add(refBean);
        }

        // Find autowiring
        Pattern autowirePattern = Pattern.compile("<bean[^>]*autowire\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>");
        Matcher autowireMatcher = autowirePattern.matcher(content);

        if (autowireMatcher.find()) {
            String autowireMode = autowireMatcher.group(1);
            if ("byName".equals(autowireMode) || "byType".equals(autowireMode)) {
                // We can't determine the exact dependencies from XML alone
                // This would need integration with the Java parser
                component.getInjectedDependencies().add("AUTOWIRED");
            }
        }
    }
}