package com.projectvisualizer.visualization;

import com.projectvisualizer.model.BusinessProcessComponent;
import com.projectvisualizer.model.CodeComponent;
import com.projectvisualizer.model.CodeMethod;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced Use Case Diagram Generator that deeply analyzes Java/Kotlin code
 * to understand what the app does and generate meaningful UML use case diagrams.
 */
public class UseCaseDiagramGenerator {

    // Method name patterns for detecting functionality
    private static final Map<String, String> METHOD_PATTERNS = new LinkedHashMap<>();
    private static final Map<String, String> CATEGORY_MAP = new LinkedHashMap<>();
    
    static {
        // Authentication patterns
        METHOD_PATTERNS.put("login|signIn|authenticate|logIn", "Login");
        METHOD_PATTERNS.put("logout|signOut|logOut", "Logout");
        METHOD_PATTERNS.put("register|signUp|createAccount", "Register Account");
        METHOD_PATTERNS.put("resetPassword|forgotPassword|recoverPassword", "Reset Password");
        METHOD_PATTERNS.put("verifyEmail|confirmEmail|validateEmail", "Verify Email");
        METHOD_PATTERNS.put("verifyPhone|confirmPhone|validateOtp", "Verify Phone");
        
        // User management patterns
        METHOD_PATTERNS.put("getProfile|loadProfile|fetchProfile|showProfile", "View Profile");
        METHOD_PATTERNS.put("updateProfile|editProfile|saveProfile", "Update Profile");
        METHOD_PATTERNS.put("changePassword|updatePassword", "Change Password");
        METHOD_PATTERNS.put("uploadAvatar|changePhoto|updatePhoto|uploadImage", "Upload Photo");
        
        // Content/Data patterns
        METHOD_PATTERNS.put("getList|loadList|fetchList|loadAll|getAll|fetchAll", "Browse Items");
        METHOD_PATTERNS.put("getDetail|loadDetail|fetchDetail|getById|loadById", "View Details");
        METHOD_PATTERNS.put("create|add|insert|save|post|submit", "Create Item");
        METHOD_PATTERNS.put("update|edit|modify|patch|put", "Update Item");
        METHOD_PATTERNS.put("delete|remove|destroy", "Delete Item");
        METHOD_PATTERNS.put("search|find|query|filter", "Search Items");
        METHOD_PATTERNS.put("sort|orderBy|arrange", "Sort Items");
        METHOD_PATTERNS.put("refresh|reload|sync", "Refresh Data");
        
        // Commerce patterns
        METHOD_PATTERNS.put("addToCart|addCart|cartAdd", "Add to Cart");
        METHOD_PATTERNS.put("removeFromCart|removeCart|cartRemove", "Remove from Cart");
        METHOD_PATTERNS.put("checkout|placeOrder|submitOrder|processOrder", "Checkout");
        METHOD_PATTERNS.put("pay|payment|processPayment|makePayment", "Make Payment");
        METHOD_PATTERNS.put("getOrders|loadOrders|fetchOrders|orderHistory", "View Orders");
        METHOD_PATTERNS.put("trackOrder|orderStatus|getOrderStatus", "Track Order");
        
        // Communication patterns
        METHOD_PATTERNS.put("sendMessage|postMessage|submitMessage", "Send Message");
        METHOD_PATTERNS.put("getMessages|loadMessages|fetchMessages|loadChat", "View Messages");
        METHOD_PATTERNS.put("sendNotification|pushNotification|notify", "Send Notification");
        METHOD_PATTERNS.put("share|shareContent|sharePost", "Share Content");
        
        // Media patterns
        METHOD_PATTERNS.put("takePhoto|capturePhoto|openCamera|captureImage", "Take Photo");
        METHOD_PATTERNS.put("recordVideo|captureVideo", "Record Video");
        METHOD_PATTERNS.put("playVideo|playMedia|startPlayback", "Play Media");
        METHOD_PATTERNS.put("downloadFile|download|saveFile", "Download File");
        METHOD_PATTERNS.put("uploadFile|upload", "Upload File");
        
        // Location patterns
        METHOD_PATTERNS.put("getLocation|getCurrentLocation|fetchLocation", "Get Location");
        METHOD_PATTERNS.put("showMap|loadMap|displayMap", "View Map");
        METHOD_PATTERNS.put("findNearby|searchNearby|nearbyPlaces", "Find Nearby");
        METHOD_PATTERNS.put("getDirections|navigate|showRoute", "Get Directions");
        
        // Settings patterns
        METHOD_PATTERNS.put("getSettings|loadSettings|fetchSettings", "View Settings");
        METHOD_PATTERNS.put("saveSettings|updateSettings|applySettings", "Update Settings");
        METHOD_PATTERNS.put("toggleDarkMode|setTheme|changeTheme", "Change Theme");
        METHOD_PATTERNS.put("setLanguage|changeLanguage", "Change Language");
        METHOD_PATTERNS.put("enableNotifications|disableNotifications|toggleNotifications", "Configure Notifications");
        
        // Social patterns
        METHOD_PATTERNS.put("follow|addFriend|connect", "Follow User");
        METHOD_PATTERNS.put("unfollow|removeFriend|disconnect", "Unfollow User");
        METHOD_PATTERNS.put("like|favorite|heart", "Like Item");
        METHOD_PATTERNS.put("comment|addComment|postComment", "Add Comment");
        METHOD_PATTERNS.put("rate|review|addRating", "Rate Item");
        
        // Category mappings
        CATEGORY_MAP.put("Login", "Authentication");
        CATEGORY_MAP.put("Logout", "Authentication");
        CATEGORY_MAP.put("Register Account", "Authentication");
        CATEGORY_MAP.put("Reset Password", "Authentication");
        CATEGORY_MAP.put("Verify Email", "Authentication");
        CATEGORY_MAP.put("Verify Phone", "Authentication");
        
        CATEGORY_MAP.put("View Profile", "User Management");
        CATEGORY_MAP.put("Update Profile", "User Management");
        CATEGORY_MAP.put("Change Password", "User Management");
        CATEGORY_MAP.put("Upload Photo", "User Management");
        
        CATEGORY_MAP.put("Browse Items", "Content Management");
        CATEGORY_MAP.put("View Details", "Content Management");
        CATEGORY_MAP.put("Create Item", "Content Management");
        CATEGORY_MAP.put("Update Item", "Content Management");
        CATEGORY_MAP.put("Delete Item", "Content Management");
        CATEGORY_MAP.put("Search Items", "Content Management");
        CATEGORY_MAP.put("Sort Items", "Content Management");
        CATEGORY_MAP.put("Refresh Data", "Content Management");
        
        CATEGORY_MAP.put("Add to Cart", "Shopping");
        CATEGORY_MAP.put("Remove from Cart", "Shopping");
        CATEGORY_MAP.put("Checkout", "Shopping");
        CATEGORY_MAP.put("Make Payment", "Shopping");
        CATEGORY_MAP.put("View Orders", "Shopping");
        CATEGORY_MAP.put("Track Order", "Shopping");
        
        CATEGORY_MAP.put("Send Message", "Communication");
        CATEGORY_MAP.put("View Messages", "Communication");
        CATEGORY_MAP.put("Send Notification", "Communication");
        CATEGORY_MAP.put("Share Content", "Communication");
        
        CATEGORY_MAP.put("Take Photo", "Media");
        CATEGORY_MAP.put("Record Video", "Media");
        CATEGORY_MAP.put("Play Media", "Media");
        CATEGORY_MAP.put("Download File", "Media");
        CATEGORY_MAP.put("Upload File", "Media");
        
        CATEGORY_MAP.put("Get Location", "Location");
        CATEGORY_MAP.put("View Map", "Location");
        CATEGORY_MAP.put("Find Nearby", "Location");
        CATEGORY_MAP.put("Get Directions", "Location");
        
        CATEGORY_MAP.put("View Settings", "Settings");
        CATEGORY_MAP.put("Update Settings", "Settings");
        CATEGORY_MAP.put("Change Theme", "Settings");
        CATEGORY_MAP.put("Change Language", "Settings");
        CATEGORY_MAP.put("Configure Notifications", "Settings");
        
        CATEGORY_MAP.put("Follow User", "Social");
        CATEGORY_MAP.put("Unfollow User", "Social");
        CATEGORY_MAP.put("Like Item", "Social");
        CATEGORY_MAP.put("Add Comment", "Social");
        CATEGORY_MAP.put("Rate Item", "Social");
    }

    /**
     * Generates PlantUML Use Case diagram from analyzed components.
     */
    public String generatePlantUML(List<CodeComponent> components, List<BusinessProcessComponent> processes) {
        // Analyze components to extract use cases
        Map<String, FeatureInfo> features = analyzeComponents(components);
        
        // Add business processes if available
        if (processes != null) {
            addBusinessProcesses(features, processes);
        }
        
        if (features.isEmpty()) {
            return generateEmptyDiagram();
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        
        // Configuration - cleaner layout
        sb.append("' Use Case Diagram - Generated from Code Analysis\n");
        sb.append("top to bottom direction\n");  // Changed to top-to-bottom for cleaner layout
        sb.append("skinparam packageStyle rectangle\n");
        sb.append("skinparam actorStyle awesome\n");
        sb.append("skinparam shadowing false\n");
        sb.append("skinparam backgroundColor #FEFEFE\n");
        sb.append("skinparam defaultFontName Arial\n");
        sb.append("skinparam roundcorner 15\n");
        sb.append("skinparam padding 10\n");
        sb.append("skinparam nodesep 50\n");  // Spacing between nodes
        sb.append("skinparam ranksep 40\n");  // Spacing between ranks
        sb.append("\n");
        
        // Modern styling by category
        sb.append("skinparam usecase {\n");
        sb.append("  BackgroundColor<<Auth>> #FFCDD2\n");
        sb.append("  BorderColor<<Auth>> #C62828\n");
        sb.append("  BackgroundColor<<User>> #E1BEE7\n");
        sb.append("  BorderColor<<User>> #7B1FA2\n");
        sb.append("  BackgroundColor<<Content>> #BBDEFB\n");
        sb.append("  BorderColor<<Content>> #1976D2\n");
        sb.append("  BackgroundColor<<Shop>> #C8E6C9\n");
        sb.append("  BorderColor<<Shop>> #388E3C\n");
        sb.append("  BackgroundColor<<Comm>> #FFE0B2\n");
        sb.append("  BorderColor<<Comm>> #F57C00\n");
        sb.append("  BackgroundColor<<Media>> #B2EBF2\n");
        sb.append("  BorderColor<<Media>> #0097A7\n");
        sb.append("  BackgroundColor<<Location>> #F0F4C3\n");
        sb.append("  BorderColor<<Location>> #AFB42B\n");
        sb.append("  BackgroundColor<<Settings>> #CFD8DC\n");
        sb.append("  BorderColor<<Settings>> #607D8B\n");
        sb.append("  BackgroundColor<<Social>> #FFCCBC\n");
        sb.append("  BorderColor<<Social>> #E64A19\n");
        sb.append("  BackgroundColor<<Data>> #D7CCC8\n");
        sb.append("  BorderColor<<Data>> #5D4037\n");
        sb.append("}\n\n");
        
        // Package styling
        sb.append("skinparam package {\n");
        sb.append("  BackgroundColor #F5F5F5\n");
        sb.append("  BorderColor #BDBDBD\n");
        sb.append("  FontSize 14\n");
        sb.append("  FontStyle bold\n");
        sb.append("}\n\n");

        // Group features by category
        Map<String, List<FeatureInfo>> byCategory = features.values().stream()
            .collect(Collectors.groupingBy(f -> f.category));
        
        // Determine actors - simplified to just User and System
        Set<String> actors = new LinkedHashSet<>();
        actors.add("User");
        boolean hasSystemFeatures = features.values().stream()
            .anyMatch(f -> f.actor.equals("System") || f.actor.equals("Payment Gateway"));
        if (hasSystemFeatures) {
            actors.add("System");
        }
        
        for (String actor : actors) {
            sb.append("actor \"").append(actor).append("\" as ").append(sanitizeId(actor)).append("\n");
        }
        sb.append("\n");
        
        // App name from package
        String appName = inferAppName(components);
        sb.append("rectangle \"").append(appName).append("\" {\n");
        
        // Generate use cases by category (limit use cases per category to reduce clutter)
        int ucCounter = 1;
        Map<String, String> featureToId = new HashMap<>();
        Map<String, String> categoryFirstUc = new HashMap<>();  // Track first UC per category for cleaner connections
        
        for (Map.Entry<String, List<FeatureInfo>> entry : byCategory.entrySet()) {
            String category = entry.getKey();
            List<FeatureInfo> categoryFeatures = entry.getValue();
            
            // Limit to top 5 features per category to reduce clutter
            List<FeatureInfo> limitedFeatures = categoryFeatures.stream()
                .limit(5)
                .collect(Collectors.toList());
            
            sb.append("\n  package \"").append(category);
            if (categoryFeatures.size() > 5) {
                sb.append(" (").append(categoryFeatures.size()).append(" features)");
            }
            sb.append("\" {\n");
            
            boolean first = true;
            for (FeatureInfo feature : limitedFeatures) {
                String ucId = "UC" + ucCounter++;
                featureToId.put(feature.name, ucId);
                if (first) {
                    categoryFirstUc.put(category, ucId);
                    first = false;
                }
                String stereotype = getStereotype(category);
                sb.append("    usecase \"").append(feature.name).append("\" as ")
                  .append(ucId).append(" <<").append(stereotype).append(">>\n");
            }
            sb.append("  }\n");
        }
        sb.append("}\n\n");
        
        // Simplified Actor relationships - connect to category packages instead of individual use cases
        sb.append("' Actor Relationships (simplified)\n");
        for (Map.Entry<String, String> entry : categoryFirstUc.entrySet()) {
            String category = entry.getKey();
            String firstUcId = entry.getValue();
            
            // Determine actor for this category
            String actor = "User";
            if (category.equals("Data Management") || category.equals("Background Tasks") || 
                category.equals("State Management") || category.equals("API Endpoints")) {
                actor = "System";
            }
            
            sb.append(sanitizeId(actor)).append(" --> ").append(firstUcId).append("\n");
        }
        sb.append("\n");
        
        // Minimal relationships - only show the most important ones
        sb.append("' Key Relationships\n");
        generateMinimalRelationships(sb, features, featureToId);
        
        // Compact legend
        sb.append("\nnote as Legend\n");
        sb.append("  **Feature Categories**\n");
        sb.append("  Colors indicate feature type\n");
        sb.append("end note\n");
        
        sb.append("@enduml\n");
        return sb.toString();
    }

    /**
     * Generates minimal relationships to avoid cluttered diagrams.
     */
    private void generateMinimalRelationships(StringBuilder sb, Map<String, FeatureInfo> features, Map<String, String> featureToId) {
        // Only show Login include relationship if both Login and protected features exist
        String loginId = featureToId.get("Login");
        String checkoutId = featureToId.get("Checkout");
        String profileId = featureToId.get("Manage Profile");
        
        if (loginId != null && checkoutId != null) {
            sb.append(checkoutId).append(" ..> ").append(loginId).append(" : <<include>>\n");
        }
        if (loginId != null && profileId != null) {
            sb.append(profileId).append(" ..> ").append(loginId).append(" : <<include>>\n");
        }
        
        // Register extends Login
        String registerId = featureToId.get("Register Account");
        if (registerId != null && loginId != null) {
            sb.append(registerId).append(" .> ").append(loginId).append(" : <<extend>>\n");
        }
    }

    /**
     * Deeply analyzes components to extract features from methods, annotations, and types.
     */
    private Map<String, FeatureInfo> analyzeComponents(List<CodeComponent> components) {
        Map<String, FeatureInfo> features = new LinkedHashMap<>();
        
        if (components == null) return features;
        
        for (CodeComponent comp : components) {
            if (comp == null) continue;
            
            // Analyze methods
            if (comp.getMethods() != null) {
                for (CodeMethod method : comp.getMethods()) {
                    if (method.getName() == null) continue;
                    
                    FeatureInfo feature = analyzeMethod(method.getName(), comp);
                    if (feature != null && !features.containsKey(feature.name)) {
                        features.put(feature.name, feature);
                    }
                }
            }
            
            // Analyze component name
            FeatureInfo componentFeature = analyzeComponentName(comp);
            if (componentFeature != null && !features.containsKey(componentFeature.name)) {
                features.put(componentFeature.name, componentFeature);
            }
            
            // Analyze API clients (Retrofit interfaces)
            if (comp.getApiClients() != null && !comp.getApiClients().isEmpty()) {
                for (String api : comp.getApiClients()) {
                    FeatureInfo apiFeature = createApiFeature(api, comp);
                    if (apiFeature != null && !features.containsKey(apiFeature.name)) {
                        features.put(apiFeature.name, apiFeature);
                    }
                }
            }
            
            // Analyze database operations
            if (comp.getDbDaos() != null && !comp.getDbDaos().isEmpty()) {
                FeatureInfo dataFeature = new FeatureInfo("Manage Local Data", "Data Management", "System");
                if (!features.containsKey(dataFeature.name)) {
                    features.put(dataFeature.name, dataFeature);
                }
            }
            
            // Analyze annotations for special features
            analyzeAnnotations(comp, features);
        }
        
        // If no features detected, create from component types
        if (features.isEmpty()) {
            extractFeaturesFromComponentTypes(components, features);
        }
        
        return features;
    }

    /**
     * Analyzes a method name to detect user-facing functionality.
     */
    private FeatureInfo analyzeMethod(String methodName, CodeComponent comp) {
        for (Map.Entry<String, String> pattern : METHOD_PATTERNS.entrySet()) {
            if (methodName.matches("(?i).*(" + pattern.getKey() + ").*")) {
                String featureName = pattern.getValue();
                String category = CATEGORY_MAP.getOrDefault(featureName, "Features");
                String actor = determineActor(featureName, comp);
                return new FeatureInfo(featureName, category, actor);
            }
        }
        return null;
    }

    /**
     * Analyzes component name to extract features.
     */
    private FeatureInfo analyzeComponentName(CodeComponent comp) {
        String name = comp.getName();
        if (name == null) return null;
        
        String lowerName = name.toLowerCase();
        String type = comp.getType() != null ? comp.getType().toLowerCase() : "";
        String componentType = comp.getComponentType() != null ? comp.getComponentType().toLowerCase() : "";
        
        // Only analyze main UI components
        boolean isUIComponent = type.contains("activity") || type.contains("fragment") ||
                                componentType.contains("activity") || componentType.contains("fragment") ||
                                name.endsWith("Activity") || name.endsWith("Fragment");
        
        if (!isUIComponent) return null;
        
        // Feature detection from component name
        if (containsAny(lowerName, "login", "signin", "auth")) {
            return new FeatureInfo("Login", "Authentication", "User");
        } else if (containsAny(lowerName, "register", "signup")) {
            return new FeatureInfo("Register Account", "Authentication", "User");
        } else if (containsAny(lowerName, "home", "main", "dashboard")) {
            return new FeatureInfo("View Dashboard", "Navigation", "User");
        } else if (containsAny(lowerName, "profile")) {
            return new FeatureInfo("Manage Profile", "User Management", "User");
        } else if (containsAny(lowerName, "settings", "preference")) {
            return new FeatureInfo("Configure Settings", "Settings", "User");
        } else if (containsAny(lowerName, "search")) {
            return new FeatureInfo("Search Content", "Content Management", "User");
        } else if (containsAny(lowerName, "detail", "view")) {
            return new FeatureInfo("View Details", "Content Management", "User");
        } else if (containsAny(lowerName, "list", "browse")) {
            return new FeatureInfo("Browse Items", "Content Management", "User");
        } else if (containsAny(lowerName, "cart", "basket")) {
            return new FeatureInfo("Manage Cart", "Shopping", "User");
        } else if (containsAny(lowerName, "checkout", "payment")) {
            return new FeatureInfo("Checkout", "Shopping", "User");
        } else if (containsAny(lowerName, "order")) {
            return new FeatureInfo("View Orders", "Shopping", "User");
        } else if (containsAny(lowerName, "chat", "message", "conversation")) {
            return new FeatureInfo("Send Messages", "Communication", "User");
        } else if (containsAny(lowerName, "notification")) {
            return new FeatureInfo("View Notifications", "Communication", "User");
        } else if (containsAny(lowerName, "camera", "photo")) {
            return new FeatureInfo("Capture Photo", "Media", "User");
        } else if (containsAny(lowerName, "map", "location")) {
            return new FeatureInfo("View Location", "Location", "User");
        } else if (containsAny(lowerName, "splash", "onboard", "welcome")) {
            return new FeatureInfo("Onboarding", "Navigation", "User");
        } else if (containsAny(lowerName, "about", "info")) {
            return new FeatureInfo("View App Info", "Settings", "User");
        }
        
        // For other activities/fragments, create a generic feature
        String featureName = extractReadableName(name);
        if (featureName != null && !featureName.isEmpty()) {
            return new FeatureInfo("View " + featureName, "Features", "User");
        }
        
        return null;
    }

    /**
     * Creates feature from API client detection.
     */
    private FeatureInfo createApiFeature(String apiName, CodeComponent comp) {
        String lowerApi = apiName.toLowerCase();
        
        if (containsAny(lowerApi, "auth", "login", "user")) {
            return new FeatureInfo("User Authentication API", "Authentication", "System");
        } else if (containsAny(lowerApi, "product", "item", "catalog")) {
            return new FeatureInfo("Fetch Products", "Content Management", "System");
        } else if (containsAny(lowerApi, "order", "purchase")) {
            return new FeatureInfo("Process Orders", "Shopping", "System");
        } else if (containsAny(lowerApi, "payment")) {
            return new FeatureInfo("Process Payment", "Shopping", "Payment Gateway");
        } else if (containsAny(lowerApi, "notification", "push")) {
            return new FeatureInfo("Push Notifications", "Communication", "System");
        } else if (containsAny(lowerApi, "message", "chat")) {
            return new FeatureInfo("Messaging Service", "Communication", "System");
        }
        
        return new FeatureInfo("API: " + extractReadableName(apiName), "Data Management", "System");
    }

    /**
     * Analyzes annotations to detect additional features.
     */
    private void analyzeAnnotations(CodeComponent comp, Map<String, FeatureInfo> features) {
        if (comp.getAnnotations() == null) return;
        
        for (String annotation : comp.getAnnotations()) {
            String lowerAnnotation = annotation.toLowerCase();
            
            if (containsAny(lowerAnnotation, "get", "post", "put", "delete") &&
                containsAny(lowerAnnotation, "mapping", "retrofit")) {
                // REST endpoint detected
                String endpoint = extractEndpointFeature(annotation);
                if (endpoint != null && !features.containsKey(endpoint)) {
                    features.put(endpoint, new FeatureInfo(endpoint, "API Endpoints", "User"));
                }
            }
            
            if (lowerAnnotation.contains("hiltviewmodel") || lowerAnnotation.contains("androidviewmodel")) {
                // ViewModel with state management
                String vmName = comp.getName();
                if (vmName != null) {
                    String feature = "Manage " + extractReadableName(vmName.replace("ViewModel", ""));
                    if (!features.containsKey(feature)) {
                        features.put(feature, new FeatureInfo(feature, "State Management", "System"));
                    }
                }
            }
        }
    }

    /**
     * Extracts features from component types when method analysis yields nothing.
     */
    private void extractFeaturesFromComponentTypes(List<CodeComponent> components, Map<String, FeatureInfo> features) {
        for (CodeComponent comp : components) {
            if (comp == null || comp.getName() == null) continue;
            
            String name = comp.getName();
            String type = comp.getType() != null ? comp.getType() : "";
            
            if (type.contains("Activity") || name.endsWith("Activity")) {
                String feature = "Use " + extractReadableName(name.replace("Activity", ""));
                if (!feature.equals("Use ") && !features.containsKey(feature)) {
                    features.put(feature, new FeatureInfo(feature, "User Interface", "User"));
                }
            } else if (type.contains("Fragment") || name.endsWith("Fragment")) {
                String feature = "View " + extractReadableName(name.replace("Fragment", ""));
                if (!feature.equals("View ") && !features.containsKey(feature)) {
                    features.put(feature, new FeatureInfo(feature, "User Interface", "User"));
                }
            } else if (type.contains("Service") || name.endsWith("Service")) {
                String feature = "Run " + extractReadableName(name.replace("Service", ""));
                if (!feature.equals("Run ") && !features.containsKey(feature)) {
                    features.put(feature, new FeatureInfo(feature, "Background Tasks", "System"));
                }
            }
        }
    }

    /**
     * Add business processes as features.
     */
    private void addBusinessProcesses(Map<String, FeatureInfo> features, List<BusinessProcessComponent> processes) {
        for (BusinessProcessComponent process : processes) {
            if (process == null || process.getProcessName() == null) continue;
            
            String name = formatName(process.getProcessName());
            if (!features.containsKey(name)) {
                String category = "Business Process";
                if (process.getProcessType() != null) {
                    switch (process.getProcessType()) {
                        case AUTHENTICATION: category = "Authentication"; break;
                        case PAYMENT: category = "Shopping"; break;
                        case USER_REGISTRATION: category = "User Management"; break;
                        case SEARCH: category = "Content Management"; break;
                        case DATA_SYNC: category = "Data Management"; break;
                        case NOTIFICATION: category = "Communication"; break;
                    }
                }
                features.put(name, new FeatureInfo(name, category, "User"));
            }
        }
    }

    private String determineActor(String featureName, CodeComponent comp) {
        String lowerFeature = featureName.toLowerCase();
        
        if (containsAny(lowerFeature, "sync", "background", "schedule", "fetch", "api")) {
            return "System";
        } else if (containsAny(lowerFeature, "admin", "manage", "moderate")) {
            return "Admin";
        } else if (containsAny(lowerFeature, "payment", "gateway")) {
            return "Payment Gateway";
        }
        return "User";
    }

    private Set<String> determineActors(Collection<FeatureInfo> features) {
        Set<String> actors = new LinkedHashSet<>();
        actors.add("User"); // Always include User
        
        for (FeatureInfo feature : features) {
            if (!feature.actor.equals("User")) {
                actors.add(feature.actor);
            }
        }
        return actors;
    }

    private String getStereotype(String category) {
        switch (category) {
            case "Authentication": return "Auth";
            case "User Management": return "User";
            case "Content Management": case "Features": case "User Interface": return "Content";
            case "Shopping": return "Shop";
            case "Communication": return "Comm";
            case "Media": return "Media";
            case "Location": return "Location";
            case "Settings": case "Navigation": return "Settings";
            case "Social": return "Social";
            case "Data Management": case "Background Tasks": case "State Management": case "API Endpoints": return "Data";
            default: return "Content";
        }
    }

    private void generateRelationships(StringBuilder sb, Map<String, FeatureInfo> features, Map<String, String> featureToId) {
        // Login is included by protected features
        String loginId = featureToId.get("Login");
        if (loginId != null) {
            for (Map.Entry<String, FeatureInfo> entry : features.entrySet()) {
                String ucId = featureToId.get(entry.getKey());
                if (ucId != null && !ucId.equals(loginId)) {
                    String category = entry.getValue().category;
                    if (category.equals("Shopping") || category.equals("User Management") ||
                        entry.getKey().contains("Profile") || entry.getKey().contains("Order")) {
                        sb.append(ucId).append(" ..> ").append(loginId).append(" : <<include>>\n");
                    }
                }
            }
        }
        
        // Register extends Login
        String registerId = featureToId.get("Register Account");
        if (registerId != null && loginId != null) {
            sb.append(registerId).append(" .> ").append(loginId).append(" : <<extend>>\n");
        }
    }

    private void addTechnologyNotes(StringBuilder sb, List<CodeComponent> components) {
        Set<String> technologies = new HashSet<>();
        
        if (components != null) {
            for (CodeComponent comp : components) {
                if (comp.getApiClients() != null && !comp.getApiClients().isEmpty()) {
                    technologies.add("REST API");
                }
                if (comp.getDbDaos() != null && !comp.getDbDaos().isEmpty()) {
                    technologies.add("Local Database (Room)");
                }
                if (comp.isCoroutineUsage()) {
                    technologies.add("Kotlin Coroutines");
                }
                if (comp.getComposablesUsed() != null && !comp.getComposablesUsed().isEmpty()) {
                    technologies.add("Jetpack Compose");
                }
                if (comp.isHiltComponent() || comp.isHasDaggerInjection()) {
                    technologies.add("Dependency Injection");
                }
            }
        }
        
        if (!technologies.isEmpty()) {
            sb.append("\nnote right\n");
            sb.append("  **Technologies Detected:**\n");
            for (String tech : technologies) {
                sb.append("  • ").append(tech).append("\n");
            }
            sb.append("end note\n");
        }
    }

    private String extractEndpointFeature(String annotation) {
        if (annotation.contains("login")) return "API: Login";
        if (annotation.contains("user")) return "API: User Data";
        if (annotation.contains("product")) return "API: Products";
        if (annotation.contains("order")) return "API: Orders";
        return null;
    }

    private String extractReadableName(String name) {
        if (name == null) return "";
        // Remove common suffixes
        String result = name.replaceAll("(Activity|Fragment|Service|ViewModel|Repository|Adapter|Manager|Helper|Impl)$", "");
        // Add spaces between camelCase
        result = result.replaceAll("([a-z])([A-Z])", "$1 $2");
        return result.trim();
    }

    private String formatName(String name) {
        if (name == null) return "";
        String result = name.replaceAll("([a-z])([A-Z])", "$1 $2").replaceAll("_", " ");
        return Character.toUpperCase(result.charAt(0)) + result.substring(1);
    }

    private String inferAppName(List<CodeComponent> components) {
        if (components == null || components.isEmpty()) return "Android App";
        for (CodeComponent comp : components) {
            if (comp.getPackageName() != null) {
                String[] parts = comp.getPackageName().split("\\.");
                if (parts.length >= 2) {
                    String name = parts[parts.length - 1];
                    return Character.toUpperCase(name.charAt(0)) + name.substring(1) + " App";
                }
            }
        }
        return "Android App";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private String sanitizeId(String id) {
        return id.replaceAll("[^A-Za-z0-9_]", "_");
    }

    public String generatePlantUML(List<BusinessProcessComponent> processes) {
        return generatePlantUML(null, processes);
    }

    private String generateEmptyDiagram() {
        return "@startuml\nleft to right direction\nactor User\nrectangle \"Android App\" {\n  usecase \"Load a project to analyze\" as UC0\n}\nUser --> UC0\n@enduml\n";
    }

    /**
     * Internal class for feature information.
     */
    private static class FeatureInfo {
        final String name;
        final String category;
        final String actor;

        FeatureInfo(String name, String category, String actor) {
            this.name = name;
            this.category = category;
            this.actor = actor;
        }
    }
}
