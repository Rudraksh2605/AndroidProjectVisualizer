package com.projectvisualizer.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

/**
 * Extracts structured data from AndroidManifest.xml for documentation.
 */
public class ManifestDataExtractor {
    
    private static final Logger logger = LoggerFactory.getLogger(ManifestDataExtractor.class);
    
    private String projectPath;
    private Document manifestDoc;
    private boolean parsed = false;
    
    // Extracted data
    private String packageName = "";
    private List<String> permissions = new ArrayList<>();
    private List<ActivityInfo> activities = new ArrayList<>();
    private List<String> services = new ArrayList<>();
    private List<String> receivers = new ArrayList<>();
    private Map<String, String> applicationMetadata = new HashMap<>();
    
    public ManifestDataExtractor(String projectPath) {
        this.projectPath = projectPath;
        parseManifest();
    }
    
    /**
     * Parses the AndroidManifest.xml file.
     */
    private void parseManifest() {
        String[] possiblePaths = {
            projectPath + "/app/src/main/AndroidManifest.xml",
            projectPath + "/src/main/AndroidManifest.xml",
            projectPath + "/AndroidManifest.xml"
        };
        
        File manifestFile = null;
        for (String path : possiblePaths) {
            File f = new File(path);
            if (f.exists()) {
                manifestFile = f;
                break;
            }
        }
        
        if (manifestFile == null) {
            logger.warn("AndroidManifest.xml not found in project: {}", projectPath);
            return;
        }
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            manifestDoc = builder.parse(manifestFile);
            manifestDoc.getDocumentElement().normalize();
            
            extractPackageName();
            extractPermissions();
            extractActivities();
            extractServices();
            extractReceivers();
            
            parsed = true;
            logger.info("Parsed AndroidManifest.xml: package={}, activities={}, permissions={}", 
                packageName, activities.size(), permissions.size());
                
        } catch (Exception e) {
            logger.error("Failed to parse AndroidManifest.xml", e);
        }
    }
    
    private void extractPackageName() {
        Element root = manifestDoc.getDocumentElement();
        packageName = root.getAttribute("package");
        if (packageName.isEmpty()) {
            // Try namespace attribute
            packageName = root.getAttribute("android:package");
        }
    }
    
    private void extractPermissions() {
        NodeList permNodes = manifestDoc.getElementsByTagName("uses-permission");
        for (int i = 0; i < permNodes.getLength(); i++) {
            Element elem = (Element) permNodes.item(i);
            String perm = elem.getAttribute("android:name");
            if (!perm.isEmpty()) {
                // Extract just the permission name
                String shortName = perm.replace("android.permission.", "");
                permissions.add(shortName);
            }
        }
    }
    
    private void extractActivities() {
        NodeList activityNodes = manifestDoc.getElementsByTagName("activity");
        for (int i = 0; i < activityNodes.getLength(); i++) {
            Element elem = (Element) activityNodes.item(i);
            String name = elem.getAttribute("android:name");
            String label = elem.getAttribute("android:label");
            String exported = elem.getAttribute("android:exported");
            
            // Check for launcher intent
            boolean isLauncher = false;
            NodeList intentFilters = elem.getElementsByTagName("intent-filter");
            for (int j = 0; j < intentFilters.getLength(); j++) {
                Element filter = (Element) intentFilters.item(j);
                NodeList categories = filter.getElementsByTagName("category");
                for (int k = 0; k < categories.getLength(); k++) {
                    Element cat = (Element) categories.item(k);
                    if (cat.getAttribute("android:name").contains("LAUNCHER")) {
                        isLauncher = true;
                        break;
                    }
                }
            }
            
            ActivityInfo info = new ActivityInfo();
            info.fullName = name;
            info.shortName = extractShortName(name);
            info.label = label;
            info.isLauncher = isLauncher;
            info.exported = "true".equals(exported);
            
            activities.add(info);
        }
    }
    
    private void extractServices() {
        NodeList nodes = manifestDoc.getElementsByTagName("service");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element elem = (Element) nodes.item(i);
            String name = elem.getAttribute("android:name");
            services.add(extractShortName(name));
        }
    }
    
    private void extractReceivers() {
        NodeList nodes = manifestDoc.getElementsByTagName("receiver");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element elem = (Element) nodes.item(i);
            String name = elem.getAttribute("android:name");
            receivers.add(extractShortName(name));
        }
    }
    
    private String extractShortName(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "";
        if (fullName.startsWith(".")) {
            return fullName.substring(1);
        }
        int lastDot = fullName.lastIndexOf('.');
        if (lastDot >= 0) {
            return fullName.substring(lastDot + 1);
        }
        return fullName;
    }
    
    // Getters
    public boolean isParsed() { return parsed; }
    public String getPackageName() { return packageName; }
    public List<String> getPermissions() { return permissions; }
    public List<ActivityInfo> getActivities() { return activities; }
    public List<String> getServices() { return services; }
    public List<String> getReceivers() { return receivers; }
    
    /**
     * Returns the launcher activity name.
     */
    public String getLauncherActivity() {
        for (ActivityInfo info : activities) {
            if (info.isLauncher) {
                return info.shortName;
            }
        }
        return activities.isEmpty() ? "Unknown" : activities.get(0).shortName;
    }
    
    /**
     * Activity information holder.
     */
    public static class ActivityInfo {
        public String fullName = "";
        public String shortName = "";
        public String label = "";
        public boolean isLauncher = false;
        public boolean exported = false;
        
        @Override
        public String toString() {
            return shortName + (isLauncher ? " (Launcher)" : "");
        }
    }
}
