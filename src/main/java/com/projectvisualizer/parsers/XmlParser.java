package com.projectvisualizer.parsers;

import com.projectvisualizer.models.CodeComponent;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

public class XmlParser {
    public List<CodeComponent> parse(File file) throws Exception {
        List<CodeComponent> components = new ArrayList<>();
        // For XML files, we might want to represent the file as a component
        CodeComponent component = new CodeComponent();
        component.setId(file.getName());
        component.setName(file.getName());
        component.setType("xml");
        component.setFilePath(file.getAbsolutePath());
        component.setLanguage("xml");
        components.add(component);
        return components;
    }
}