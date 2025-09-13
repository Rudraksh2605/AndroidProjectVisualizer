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
        // 1) Field/property injection with annotations like @Inject, @Autowired
        Pattern fieldAnnPattern = Pattern.compile("@(Inject|Autowired|Resource)\\s+.*?\\b(val|var)\\s+(\\w+)\\s*:\\s*([\\w\\.<>?]+)");
        Matcher fieldAnnMatcher = fieldAnnPattern.matcher(content);
        while (fieldAnnMatcher.find()) {
            String dependencyType = fieldAnnMatcher.group(4);
            component.getInjectedDependencies().add(dependencyType);
        }

        // 2) Primary/secondary constructor parameter injection with @Inject
        // Pattern A: class Foo @Inject constructor(dep: Type, ...)
        Pattern injectCtorAfterClass = Pattern.compile("class\\s+\\w+\\s+@Inject\\s+constructor\\s*\\(([^)]*)\\)");
        Matcher injectCtorAfterClassMatcher = injectCtorAfterClass.matcher(content);
        while (injectCtorAfterClassMatcher.find()) {
            String params = injectCtorAfterClassMatcher.group(1);
            Matcher typeMatcher = Pattern.compile("(val|var)?\\s*\\w+\\s*:\\s*([\\w\\.<>?]+)").matcher(params);
            while (typeMatcher.find()) {
                component.getInjectedDependencies().add(typeMatcher.group(2));
            }
        }
        // Pattern B: @Inject constructor(dep: Type)
        Pattern injectCtorPattern = Pattern.compile("@Inject\\s+constructor\\s*\\(([^)]*)\\)");
        Matcher injectCtorMatcher = injectCtorPattern.matcher(content);
        while (injectCtorMatcher.find()) {
            String params = injectCtorMatcher.group(1);
            Matcher typeMatcher = Pattern.compile("(val|var)?\\s*\\w+\\s*:\\s*([\\w\\.<>?]+)").matcher(params);
            while (typeMatcher.find()) {
                component.getInjectedDependencies().add(typeMatcher.group(2));
            }
        }

        // 3) Koin: `val repo: Repo by inject()`
        Pattern koinByInject = Pattern.compile("\\b(val|var)\\s+\\w+\\s*:\\s*([\\w\\.<>?]+)\\s+by\\s+inject\\s*\\(");
        Matcher koinByInjectMatcher = koinByInject.matcher(content);
        while (koinByInjectMatcher.find()) {
            component.getInjectedDependencies().add(koinByInjectMatcher.group(2));
        }
        // 3b) Koin get<T>()
        Pattern koinGetGeneric = Pattern.compile("\\bget\\s*<\\s*([\\w\\.<>?]+)\\s*>\\s*\\(");
        Matcher koinGetGenericMatcher = koinGetGeneric.matcher(content);
        while (koinGetGenericMatcher.find()) {
            component.getInjectedDependencies().add(koinGetGenericMatcher.group(1));
        }
        // 3c) Koin get() with explicit type on LHS: `val repo: Repo = get()`
        Pattern koinGetAssigned = Pattern.compile("\\b(val|var)\\s+\\w+\\s*:\\s*([\\w\\.<>?]+)\\s*=\\s*get\\s*\\(");
        Matcher koinGetAssignedMatcher = koinGetAssigned.matcher(content);
        while (koinGetAssignedMatcher.find()) {
            component.getInjectedDependencies().add(koinGetAssignedMatcher.group(2));
        }
    }
}