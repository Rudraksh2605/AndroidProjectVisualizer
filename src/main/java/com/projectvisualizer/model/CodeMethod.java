// CodeMethod.java
package com.projectvisualizer.model;

import java.util.ArrayList;
import java.util.List;

public class CodeMethod {
    private String name;
    private String returnType;
    private String visibility;
    private List<String> parameters;
    private boolean isStatic;
    private boolean isAbstract;

    public CodeMethod() {
        this.parameters = new ArrayList<>();
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public List<String> getParameters() { return parameters; }
    public void setParameters(List<String> parameters) { this.parameters = parameters; }

    public boolean isStatic() { return isStatic; }
    public void setStatic(boolean isStatic) { this.isStatic = isStatic; }

    public boolean isAbstract() { return isAbstract; }
    public void setAbstract(boolean isAbstract) { this.isAbstract = isAbstract; }

    // Helper method
    public void addParameter(String parameter) {
        this.parameters.add(parameter);
    }
}