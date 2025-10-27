package com.projectvisualizer.model;

import java.util.ArrayList;
import java.util.List;

public class CodeComponent {
    private String id;
    private String name;
    private String type;
    private String filePath;
    private String language;
    private String layer;
    private String extendsClass;
    private List<String> implementsList;
    private List<String> annotations;
    private List<CodeField> fields;
    private List<CodeComponent> intents;
    private List<CodeMethod> methods;
    private List<CodeComponent> dependencies;
    private List<String> injectedDependencies;
    private List<String> daggerInjectedDependencies;
    private List<String> layoutFiles;
    private List<NavigationDestination> navigationDestinations;
    private String fileExtension;

    // New unified schema fields
    private String packageName;
    private List<String> imports;
    private List<String> modifiers;
    private boolean dataClass;
    private boolean sealedClass;
    private boolean objectDeclaration;
    private boolean hasCompanionObject;
    private String componentType; // Activity, Fragment, Service, Receiver, ViewModel, Repository, UseCase, Composable, etc.
    private boolean manifestRegistered;
    private String moduleName;
    private List<String> composablesUsed;
    private List<String> resourcesUsed; // e.g., layout/activity_main, string/app_name
    private boolean viewBindingUsed;
    private boolean dataBindingUsed;
    private boolean coroutineUsage;
    private List<String> apiClients; // Retrofit/Ktor indicators
    private List<String> dbEntities; // Room entities detected on this type
    private List<String> dbDaos; // Room DAOs implemented/used

    // Dagger/Hilt specific fields
    private boolean hasDaggerInjection;
    private String daggerComponentType;
    private boolean hiltComponent;
    private String hiltComponentType;
    private List<String> daggerDependencies;

    public CodeComponent() {
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.dependencies = new ArrayList<>();
        this.injectedDependencies = new ArrayList<>();
        this.daggerInjectedDependencies = new ArrayList<>();
        this.layoutFiles = new ArrayList<>();
        this.navigationDestinations = new ArrayList<>();
        this.implementsList = new ArrayList<>();
        this.annotations = new ArrayList<>();
        this.daggerDependencies = new ArrayList<>();
        this.imports = new ArrayList<>();
        this.modifiers = new ArrayList<>();
        this.composablesUsed = new ArrayList<>();
        this.resourcesUsed = new ArrayList<>();
        this.apiClients = new ArrayList<>();
        this.dbEntities = new ArrayList<>();
        this.dbDaos = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getLayer() { return layer; }
    public void setLayer(String layer) { this.layer = layer; }

    public String getExtendsClass() { return extendsClass; }
    public void setExtendsClass(String extendsClass) { this.extendsClass = extendsClass; }

    public List<String> getImplementsList() { return implementsList; }
    public void setImplementsList(List<String> implementsList) { this.implementsList = implementsList; }

    public List<String> getAnnotations() { return annotations; }
    public void setAnnotations(List<String> annotations) { this.annotations = annotations; }

    public List<CodeField> getFields() { return fields; }
    public void setFields(List<CodeField> fields) { this.fields = fields; }

    public List<CodeMethod> getMethods() { return methods; }
    public void setMethods(List<CodeMethod> methods) { this.methods = methods; }

    public List<CodeComponent> getDependencies() { return dependencies; }
    public void setDependencies(List<CodeComponent> dependencies) { this.dependencies = dependencies; }

    public List<String> getInjectedDependencies() { return injectedDependencies; }
    public void setInjectedDependencies(List<String> injectedDependencies) { this.injectedDependencies = injectedDependencies; }

    public List<String> getDaggerInjectedDependencies() { return daggerInjectedDependencies; }
    public void setDaggerInjectedDependencies(List<String> daggerInjectedDependencies) { this.daggerInjectedDependencies = daggerInjectedDependencies; }

    public List<String> getLayoutFiles() { return layoutFiles; }
    public void setLayoutFiles(List<String> layoutFiles) { this.layoutFiles = layoutFiles; }

    public List<NavigationDestination> getNavigationDestinations() { return navigationDestinations; }
    public void setNavigationDestinations(List<NavigationDestination> navigationDestinations) { this.navigationDestinations = navigationDestinations; }

    public boolean isHasDaggerInjection() { return hasDaggerInjection; }
    public void setHasDaggerInjection(boolean hasDaggerInjection) { this.hasDaggerInjection = hasDaggerInjection; }

    public String getDaggerComponentType() { return daggerComponentType; }
    public void setDaggerComponentType(String daggerComponentType) { this.daggerComponentType = daggerComponentType; }

    public boolean isHiltComponent() { return hiltComponent; }
    public void setHiltComponent(boolean hiltComponent) { this.hiltComponent = hiltComponent; }

    public String getHiltComponentType() { return hiltComponentType; }
    public void setHiltComponentType(String hiltComponentType) { this.hiltComponentType = hiltComponentType; }

    public List<String> getDaggerDependencies() { return daggerDependencies; }
    public void setDaggerDependencies(List<String> daggerDependencies) { this.daggerDependencies = daggerDependencies; }

    // New fields accessors
    public String getFileExtension() { return fileExtension; }
    public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public List<String> getImports() { return imports; }
    public void setImports(List<String> imports) { this.imports = imports; }

    public List<String> getModifiers() { return modifiers; }
    public void setModifiers(List<String> modifiers) { this.modifiers = modifiers; }

    public boolean isDataClass() { return dataClass; }
    public void setDataClass(boolean dataClass) { this.dataClass = dataClass; }

    public boolean isSealedClass() { return sealedClass; }
    public void setSealedClass(boolean sealedClass) { this.sealedClass = sealedClass; }

    public boolean isObjectDeclaration() { return objectDeclaration; }
    public void setObjectDeclaration(boolean objectDeclaration) { this.objectDeclaration = objectDeclaration; }

    public boolean isHasCompanionObject() { return hasCompanionObject; }
    public void setHasCompanionObject(boolean hasCompanionObject) { this.hasCompanionObject = hasCompanionObject; }

    public String getComponentType() { return componentType; }
    public void setComponentType(String componentType) { this.componentType = componentType; }

    public boolean isManifestRegistered() { return manifestRegistered; }
    public void setManifestRegistered(boolean manifestRegistered) { this.manifestRegistered = manifestRegistered; }

    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }

    public List<String> getComposablesUsed() { return composablesUsed; }
    public void setComposablesUsed(List<String> composablesUsed) { this.composablesUsed = composablesUsed; }

    public List<String> getResourcesUsed() { return resourcesUsed; }
    public void setResourcesUsed(List<String> resourcesUsed) { this.resourcesUsed = resourcesUsed; }

    public boolean isViewBindingUsed() { return viewBindingUsed; }
    public void setViewBindingUsed(boolean viewBindingUsed) { this.viewBindingUsed = viewBindingUsed; }

    public boolean isDataBindingUsed() { return dataBindingUsed; }
    public void setDataBindingUsed(boolean dataBindingUsed) { this.dataBindingUsed = dataBindingUsed; }

    public boolean isCoroutineUsage() { return coroutineUsage; }
    public void setCoroutineUsage(boolean coroutineUsage) { this.coroutineUsage = coroutineUsage; }

    public List<String> getApiClients() { return apiClients; }
    public void setApiClients(List<String> apiClients) { this.apiClients = apiClients; }

    public List<String> getDbEntities() { return dbEntities; }
    public void setDbEntities(List<String> dbEntities) { this.dbEntities = dbEntities; }

    public List<String> getDbDaos() { return dbDaos; }
    public void setDbDaos(List<String> dbDaos) { this.dbDaos = dbDaos; }

    // Helper methods
    public void addLayoutFile(String layoutFile) {
        this.layoutFiles.add(layoutFile);
    }

    public void addNavigationDestination(NavigationDestination destination) {
        this.navigationDestinations.add(destination);
    }

    public void addDependency(CodeComponent dependency) {
        this.dependencies.add(dependency);
    }

    public void addInjectedDependency(String dependency) {
        this.injectedDependencies.add(dependency);
    }

    public void addDaggerInjectedDependency(String dependency) {
        this.daggerInjectedDependencies.add(dependency);
    }

    public void addAnnotation(String annotation) {
        this.annotations.add(annotation);
    }

    public void addImport(String imp) {
        this.imports.add(imp);
    }

    public void addComposable(String composable) {
        this.composablesUsed.add(composable);
    }

    public void addResourceUsage(String res) {
        this.resourcesUsed.add(res);
    }

    public List<CodeComponent> getIntents() {
        if (intents == null) {
            intents = new ArrayList<>();
        }
        return intents;
    }

    public void setIntents(List<CodeComponent> intents) {
        this.intents = intents;
    }

    public void addIntent(CodeComponent intent) {
        getIntents().add(intent);
    }
}