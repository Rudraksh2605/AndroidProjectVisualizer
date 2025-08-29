package com.projectvisualizer.models;

import java.util.ArrayList;
import java.util.List;

public class CodeMethod {
    private String name;
    private String returnType;
    private String visibility;
    private List<String> parameters = new ArrayList<>();

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public List<String> getParameters() { return parameters; }
    public void setParameters(List<String> parameters) { this.parameters = parameters; }
}