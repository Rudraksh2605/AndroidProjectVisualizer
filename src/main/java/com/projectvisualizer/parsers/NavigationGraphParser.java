package com.projectvisualizer.parsers;

import com.projectvisualizer.model.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NavigationGraphParser {

    public List<CodeComponent> parse(File navGraphFile) throws Exception {
        List<CodeComponent> navigationComponents = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(navGraphFile);

            // Get the root element
            Element root = document.getDocumentElement();

            // Find all destinations
            NodeList destinationNodes = root.getElementsByTagName("fragment");
            for (int i = 0; i < destinationNodes.getLength(); i++) {
                Element destinationElement = (Element) destinationNodes.item(i);
                String destinationId = destinationElement.getAttribute("android:id");
                String destinationName = destinationElement.getAttribute("android:name");

                CodeComponent navComponent = new CodeComponent();
                navComponent.setId(destinationId);
                navComponent.setName(destinationName);
                navComponent.setType("Navigation Destination");
                navComponent.setLayer("UI");
                navComponent.setFilePath(navGraphFile.getAbsolutePath());
                navComponent.setLanguage("xml");

                // Find actions from this destination
                NodeList actionNodes = destinationElement.getElementsByTagName("action");
                for (int j = 0; j < actionNodes.getLength(); j++) {
                    Element actionElement = (Element) actionNodes.item(j);
                    String actionId = actionElement.getAttribute("android:id");
                    String targetDestination = actionElement.getAttribute("app:destination");

                    NavigationDestination destination = new NavigationDestination(
                            targetDestination,
                            getDestinationName(targetDestination, document),
                            "fragment",
                            actionId
                    );

                    navComponent.addNavigationDestination(destination);
                }

                navigationComponents.add(navComponent);
            }

        } catch (Exception e) {
            System.err.println("Error parsing navigation graph: " + e.getMessage());
        }

        return navigationComponents;
    }

    private String getDestinationName(String destinationId, Document document) {
        // Find the destination with the given ID
        NodeList allDestinations = document.getElementsByTagName("fragment");
        for (int i = 0; i < allDestinations.getLength(); i++) {
            Element destination = (Element) allDestinations.item(i);
            if (destinationId.equals(destination.getAttribute("android:id"))) {
                return destination.getAttribute("android:name");
            }
        }
        return "Unknown";
    }
}