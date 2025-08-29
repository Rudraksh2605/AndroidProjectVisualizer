package com.projectvisualizer.parsers;

import com.projectvisualizer.models.CodeComponent;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class KotlinParser {
    public List<CodeComponent> parse(File file) throws Exception {
        List<CodeComponent> components = new ArrayList<>();
        String content = new String(Files.readAllBytes(file.toPath()));

        // Get package name
        String packageName = "";
        Pattern packagePattern = Pattern.compile("package\\s+([^\\s;]+)");
        Matcher packageMatcher = packagePattern.matcher(content);
        if (packageMatcher.find()) {
            packageName = packageMatcher.group(1);
        }

        // Pattern to match class and interface definitions
        Pattern pattern = Pattern.compile("(class|interface|object)\\s+(\\w+)(\\s*:\\s*([^{]+))?");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String type = matcher.group(1);
            String name = matcher.group(2);
            String extendsOrImplements = matcher.group(4);

            CodeComponent component = new CodeComponent();
            String fullName = packageName.isEmpty() ? name : packageName + "." + name;
            component.setId(fullName);
            component.setName(name);
            component.setType(type.equals("object") ? "class" : type);
            component.setFilePath(file.getAbsolutePath());
            component.setLanguage("kotlin");

            // Handle extends/implements
            if (extendsOrImplements != null && !extendsOrImplements.trim().isEmpty()) {
                String[] parts = extendsOrImplements.split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (part.contains("(")) {
                        part = part.substring(0, part.indexOf("(")).trim();
                    }

                    if (part.contains("<")) {
                        part = part.substring(0, part.indexOf("<")).trim();
                    }

                    if (!part.isEmpty()) {
                        if (type.equals("class") && component.getExtendsClass() == null) {
                            component.setExtendsClass(part);
                        } else {
                            component.getImplementsList().add(part);
                        }
                    }
                }
            }

            // Look for dependency injection in Kotlin (Koin, Dagger, etc.)
            findKotlinInjections(content, component);

            components.add(component);
        }

        return components;
    }

    private void findKotlinInjections(String content, CodeComponent component) {
        // Pattern for field injection with annotations like @Inject, @Autowired
        Pattern injectionPattern = Pattern.compile("@(Inject|Autowired|Resource)\\s+[^\\n]*\\s+(var|val)\\s+(\\w+)\\s*:\\s*(\\w+)");
        Matcher injectionMatcher = injectionPattern.matcher(content);

        while (injectionMatcher.find()) {
            String dependencyType = injectionMatcher.group(4);
            component.getInjectedDependencies().add(dependencyType);
        }

        // Pattern for constructor injection
        Pattern constructorPattern = Pattern.compile("constructor\\s*\\([^)]*\\)");
        Matcher constructorMatcher = constructorPattern.matcher(content);

        if (constructorMatcher.find()) {
            String constructorParams = constructorMatcher.group(0);

            // Look for injected parameters in constructor
            Pattern paramPattern = Pattern.compile("@(Inject|Autowired|Resource)\\s+[^,)]*\\s+(\\w+)\\s*:\\s*(\\w+)");
            Matcher paramMatcher = paramPattern.matcher(constructorParams);

            while (paramMatcher.find()) {
                String dependencyType = paramMatcher.group(3);
                component.getInjectedDependencies().add(dependencyType);
            }
        }

        // Pattern for Koin's by inject() and get()
        Pattern koinPattern = Pattern.compile("(by\\s+inject\\(\\)|get\\(\\))\\s*:\\s*(\\w+)");
        Matcher koinMatcher = koinPattern.matcher(content);

        while (koinMatcher.find()) {
            String dependencyType = koinMatcher.group(2);
            component.getInjectedDependencies().add(dependencyType);
        }
    }
}