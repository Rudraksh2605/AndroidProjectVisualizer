package com.projectvisualizer.parsers;

import com.projectvisualizer.model.CodeComponent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AndroidManifestParser {

    public Map<String, String> parseManifest(File manifestFile) throws Exception {
        Map<String, String> activityLayoutMap = new HashMap<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(manifestFile);

            // Get all activity elements
            NodeList activityNodes = document.getElementsByTagName("activity");
            for (int i = 0; i < activityNodes.getLength(); i++) {
                Element activityElement = (Element) activityNodes.item(i);
                String activityName = activityElement.getAttribute("android:name");

                // Find the intent-filter with category LAUNCHER to identify main activities
                NodeList intentFilters = activityElement.getElementsByTagName("intent-filter");
                boolean isLauncher = false;
                for (int j = 0; j < intentFilters.getLength(); j++) {
                    Element intentFilter = (Element) intentFilters.item(j);
                    NodeList categories = intentFilter.getElementsByTagName("category");
                    for (int k = 0; k < categories.getLength(); k++) {
                        Element category = (Element) categories.item(k);
                        if ("android.intent.category.LAUNCHER".equals(category.getAttribute("android:name"))) {
                            isLauncher = true;
                            break;
                        }
                    }
                    if (isLauncher) break;
                }

                activityLayoutMap.put(activityName, isLauncher ? "LAUNCHER" : "REGULAR");
            }

        } catch (Exception e) {
            System.err.println("Error parsing AndroidManifest.xml: " + e.getMessage());
        }

        return activityLayoutMap;
    }
}