package com.projectvisualizer.parsers;

import com.projectvisualizer.model.CodeComponent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

public class XmlParser {
    public List<CodeComponent> parse(File file) throws Exception {
        List<CodeComponent> components = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(file);

            String fileName = file.getName();
            String filePath = file.getAbsolutePath();

            if (fileName.contains("navigation") && document.getDocumentElement().getTagName().equals("navigation")) {
                NavigationGraphParser navParser = new NavigationGraphParser();
                components.addAll(navParser.parse(file));
                return components;
            }

            if (isLayoutFile(document)) {
                components.add(parseLayoutFile(document, file));
            }

            if (isSpringConfig(document)) {
                components.addAll(parseSpringConfig(document, file));
            }

        } catch (Exception e) {
            System.err.println("Error parsing XML file: " + e.getMessage());
        }

        return components;
    }

    private boolean isLayoutFile(Document document) {
        Element root = document.getDocumentElement();
        String rootTag = root.getTagName();
        return rootTag.contains("Layout") ||
                rootTag.equals("LinearLayout") ||
                rootTag.equals("RelativeLayout") ||
                rootTag.equals("ConstraintLayout") ||
                rootTag.equals("FrameLayout") ||
                rootTag.equals("androidx.constraintlayout.widget.ConstraintLayout");
    }

    private boolean isSpringConfig(Document document) {
        Element root = document.getDocumentElement();
        return root.getTagName().equals("beans") ||
                root.getTagName().contains("spring") ||
                hasSpringNamespace(root);
    }

    private boolean hasSpringNamespace(Element element) {
        return element.getAttribute("xmlns:spring") != null ||
                element.getAttribute("xmlns:context") != null;
    }

    private CodeComponent parseLayoutFile(Document document, File file) {
        CodeComponent component = new CodeComponent();
        component.setId(file.getName());
        component.setName(file.getName().replace(".xml", ""));
        component.setType("layout");
        component.setFilePath(file.getAbsolutePath());
        component.setLanguage("xml");
        component.setFileExtension("xml");
        component.setLayer("UI");

        // Find custom views and fragments
        findCustomViews(document, component);

        // Find associated activity/fragment
        String associatedComponent = findAssociatedComponent(file);
        if (associatedComponent != null) {
            CodeComponent dependency = new CodeComponent();
            dependency.setId(associatedComponent);
            dependency.setName(associatedComponent);
            dependency.setType("class");
            component.getDependencies().add(dependency);
        }

        return component;
    }

    private void findCustomViews(Document document, CodeComponent component) {
        NodeList allElements = document.getElementsByTagName("*");
        for (int i = 0; i < allElements.getLength(); i++) {
            Element element = (Element) allElements.item(i);
            String tagName = element.getTagName();

            // Check if it's a custom view (contains package name)
            if (tagName.contains(".") && !tagName.startsWith("android.")) {
                CodeComponent dependency = new CodeComponent();
                dependency.setId(tagName);
                dependency.setName(tagName.substring(tagName.lastIndexOf('.') + 1));
                dependency.setType("view");
                component.getDependencies().add(dependency);
            }

            // Check for fragments
            if (tagName.equals("fragment")) {
                String fragmentClass = element.getAttribute("android:name");
                if (!fragmentClass.isEmpty()) {
                    CodeComponent dependency = new CodeComponent();
                    dependency.setId(fragmentClass);
                    dependency.setName(fragmentClass.substring(fragmentClass.lastIndexOf('.') + 1));
                    dependency.setType("fragment");
                    component.getDependencies().add(dependency);
                }
            }
        }
    }

    private List<CodeComponent> parseSpringConfig(Document document, File file) {
        List<CodeComponent> components = new ArrayList<>();

        NodeList beanNodes = document.getElementsByTagName("bean");
        for (int i = 0; i < beanNodes.getLength(); i++) {
            Element beanElement = (Element) beanNodes.item(i);
            String className = beanElement.getAttribute("class");

            if (!className.isEmpty()) {
                CodeComponent component = new CodeComponent();
                component.setId(className);
                component.setName(className.substring(className.lastIndexOf('.') + 1));
                component.setType("bean");
                component.setFilePath(file.getAbsolutePath());
                component.setLanguage("xml");

                findSpringDependencies(beanElement, component);
                components.add(component);
            }
        }

        // Handle component scanning
        NodeList componentScanNodes = document.getElementsByTagName("context:component-scan");
        for (int i = 0; i < componentScanNodes.getLength(); i++) {
            Element scanElement = (Element) componentScanNodes.item(i);
            String basePackage = scanElement.getAttribute("base-package");
            if (!basePackage.isEmpty()) {
                // Note: This would need integration with Java file parsing
                CodeComponent component = new CodeComponent();
                component.setId("component-scan-" + basePackage);
                component.setName("ComponentScan: " + basePackage);
                component.setType("configuration");
                component.setFilePath(file.getAbsolutePath());
                component.setLanguage("xml");
                components.add(component);
            }
        }

        return components;
    }

    private void findSpringDependencies(Element beanElement, CodeComponent component) {
        // Find constructor arguments
        NodeList constructorArgs = beanElement.getElementsByTagName("constructor-arg");
        for (int i = 0; i < constructorArgs.getLength(); i++) {
            Element argElement = (Element) constructorArgs.item(i);
            String ref = argElement.getAttribute("ref");
            if (!ref.isEmpty()) {
                component.getInjectedDependencies().add(ref);
            }
        }

        // Find property injections
        NodeList properties = beanElement.getElementsByTagName("property");
        for (int i = 0; i < properties.getLength(); i++) {
            Element propertyElement = (Element) properties.item(i);
            String ref = propertyElement.getAttribute("ref");
            if (!ref.isEmpty()) {
                component.getInjectedDependencies().add(ref);
            }
        }

        // Check for autowiring
        String autowire = beanElement.getAttribute("autowire");
        if (!autowire.isEmpty() && !"no".equals(autowire)) {
            component.getInjectedDependencies().add("AUTOWIRED");
        }
    }

    private String findAssociatedComponent(File layoutFile) {
        // Implementation remains the same as before
        String layoutName = layoutFile.getName().replace(".xml", "");
        String componentName = convertLayoutNameToComponentName(layoutName);

        // Look for Java/Kotlin files with this name
        File projectDir = layoutFile.getParentFile().getParentFile().getParentFile();
        File javaDir = new File(projectDir, "src/main/java");
        File kotlinDir = new File(projectDir, "src/main/kotlin");

        String javaFile = findFileWithName(javaDir, componentName + ".java");
        if (javaFile != null) return javaFile;

        String kotlinFile = findFileWithName(kotlinDir, componentName + ".kt");
        if (kotlinFile != null) return kotlinFile;

        return null;
    }

    private String convertLayoutNameToComponentName(String layoutName) {
        // Implementation remains the same
        String[] parts = layoutName.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1));
            }
        }

        if (layoutName.startsWith("activity_")) {
            result.append("Activity");
        } else if (layoutName.startsWith("fragment_")) {
            result.append("Fragment");
        } else if (layoutName.startsWith("dialog_")) {
            result.append("Dialog");
        } else if (layoutName.startsWith("item_")) {
            result.append("Adapter");
        }

        return result.toString();
    }

    private String findFileWithName(File directory, String fileName) {
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        String result = findFileWithName(file, fileName);
                        if (result != null) return result;
                    } else if (file.getName().equals(fileName)) {
                        return file.getAbsolutePath();
                    }
                }
            }
        }
        return null;
    }
}