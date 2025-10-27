// ExternalIntegration.java
package com.projectvisualizer.model;

import java.util.ArrayList;
import java.util.List;

public class ExternalIntegration {
    private String id;
    private String name;
    private String endpoint;
    private String integrationType;
    private String authType;
    private List<String> requiredPermissions;

    public ExternalIntegration() {
        this.requiredPermissions = new ArrayList<>();
    }

    public ExternalIntegration(String id, String name, String endpoint) {
        this();
        this.id = id;
        this.name = name;
        this.endpoint = endpoint;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getIntegrationType() { return integrationType; }
    public void setIntegrationType(String integrationType) { this.integrationType = integrationType; }

    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }

    public List<String> getRequiredPermissions() { return requiredPermissions; }
    public void setRequiredPermissions(List<String> requiredPermissions) { this.requiredPermissions = requiredPermissions; }

    // Helper methods
    public void addRequiredPermission(String permission) {
        this.requiredPermissions.add(permission);
    }
}