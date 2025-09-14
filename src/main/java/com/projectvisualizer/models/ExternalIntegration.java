package com.projectvisualizer.models;

import java.util.ArrayList;
import java.util.List;

public class ExternalIntegration {
    private String integrationId;
    private String integrationName;
    private String endpoint;
    private String protocol;
    private String authType;
    private List<String> requiredPermissions;
    private String integrationType;
    private boolean isActive;
    private String description;
    private String version;
    private long lastUpdated;

    // Default constructor
    public ExternalIntegration() {
        this.requiredPermissions = new ArrayList<>();
        this.isActive = true;
        this.protocol = "HTTPS";
        this.authType = "API_KEY";
        this.integrationType = "REST_API";
        this.lastUpdated = System.currentTimeMillis();
    }

    // Constructor with basic fields
    public ExternalIntegration(String integrationId, String integrationName, String endpoint) {
        this();
        this.integrationId = integrationId;
        this.integrationName = integrationName;
        this.endpoint = endpoint;
    }

    // Constructor with main fields
    public ExternalIntegration(String integrationId, String integrationName, String endpoint,
                               String protocol, String authType) {
        this(integrationId, integrationName, endpoint);
        this.protocol = protocol;
        this.authType = authType;
    }

    // Getters and Setters
    public String getIntegrationId() {
        return integrationId;
    }

    public void setIntegrationId(String integrationId) {
        this.integrationId = integrationId;
    }

    public String getIntegrationName() {
        return integrationName;
    }

    public void setIntegrationName(String integrationName) {
        this.integrationName = integrationName;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public List<String> getRequiredPermissions() {
        return requiredPermissions;
    }

    public void setRequiredPermissions(List<String> requiredPermissions) {
        this.requiredPermissions = requiredPermissions != null ? requiredPermissions : new ArrayList<>();
    }

    public String getIntegrationType() {
        return integrationType;
    }

    public void setIntegrationType(String integrationType) {
        this.integrationType = integrationType;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    // Utility methods for managing permissions
    public void addRequiredPermission(String permission) {
        if (permission != null && !permission.trim().isEmpty()) {
            if (!this.requiredPermissions.contains(permission)) {
                this.requiredPermissions.add(permission);
            }
        }
    }

    public void removeRequiredPermission(String permission) {
        this.requiredPermissions.remove(permission);
    }

    public boolean hasPermission(String permission) {
        return this.requiredPermissions.contains(permission);
    }

    public void clearPermissions() {
        this.requiredPermissions.clear();
    }

    // Utility methods
    public boolean isSecure() {
        return "HTTPS".equalsIgnoreCase(protocol) || "WSS".equalsIgnoreCase(protocol);
    }

    public boolean requiresAuthentication() {
        return authType != null && !"NONE".equalsIgnoreCase(authType);
    }

    public void activate() {
        setActive(true);
    }

    public void deactivate() {
        setActive(false);
    }

    @Override
    public String toString() {
        return "ExternalIntegration{" +
                "integrationId='" + integrationId + '\'' +
                ", integrationName='" + integrationName + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", protocol='" + protocol + '\'' +
                ", authType='" + authType + '\'' +
                ", integrationType='" + integrationType + '\'' +
                ", isActive=" + isActive +
                ", permissionsCount=" + (requiredPermissions != null ? requiredPermissions.size() : 0) +
                '}';
    }
}
