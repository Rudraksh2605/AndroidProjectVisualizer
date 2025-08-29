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

        // Pattern to match class and interface definitions in Dart
        Pattern pattern = Pattern.compile("(class|interface)\\s+(\\w+)");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String type = matcher.group(1);
            String name = matcher.group(2);
            CodeComponent component = new CodeComponent();
            component.setId(name);
            component.setName(name);
            component.setType(type);
            component.setFilePath(file.getAbsolutePath());
            component.setLanguage("dart");
            components.add(component);
        }

        return components;
    }
}