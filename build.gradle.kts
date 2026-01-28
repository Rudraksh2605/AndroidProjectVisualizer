plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.0.13"
}

group = "com.projectvisualizer"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    // Java Parser
    implementation("com.github.javaparser:javaparser-core:3.25.7")
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.25.7")

    // Graph Visualization
    implementation("org.graphstream:gs-core:2.0")
    implementation("org.graphstream:gs-ui-javafx:2.0")

    implementation ("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.24")
    implementation ("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")



    // Enhanced XML parsing
    implementation("xerces:xercesImpl:2.12.2")
    implementation("org.dom4j:dom4j:2.1.4")

    // AST Analysis Utilities
    implementation("org.eclipse.jdt:org.eclipse.jdt.core:3.37.0")
    implementation("org.eclipse.platform:org.eclipse.core.runtime:3.21.100")

    // UI and Controls
    implementation("org.controlsfx:controlsfx:11.1.2")

    // JavaFX
    implementation("org.openjfx:javafx-controls:17.0.2")
    implementation("org.openjfx:javafx-fxml:17.0.2")
    implementation("org.openjfx:javafx-swing:17.0.2")
    implementation("org.openjfx:javafx-web:17.0.2")

    // Diagram Generation
    implementation("net.sourceforge.plantuml:plantuml:1.2023.12")

    // Graphviz diagram generation
    implementation("guru.nidi:graphviz-java:0.18.1")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Utility libraries
    implementation("commons-io:commons-io:2.15.1")
    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("com.google.guava:guava:32.1.3-jre")

    // JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-core:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.16.1")

    // PDF Generation
    implementation("com.itextpdf:itext7-core:7.2.5")

    // LLM Inference - llama.java for Phi-2 GGUF model with GPU support
    implementation("de.kherud:llama:3.2.1")

    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

javafx {
    version = "17.0.2"
    modules = listOf(
        "javafx.controls",
        "javafx.fxml",
        "javafx.graphics",
        "javafx.swing",
        "javafx.base",
        "javafx.media",
        "javafx.web"
    )
}

application {
    mainClass.set("com.projectvisualizer.Main")
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    modularity.inferModulePath.set(true)
}

// LLM Model Configuration
val modelDir = file("$projectDir/models")
val nativesDir = file("$buildDir/natives")

// Task to create models directory
tasks.register("createModelDir") {
    doLast {
        modelDir.mkdirs()
        println("Models directory created at: ${modelDir.absolutePath}")
        println("Please download phi-2 GGUF model from HuggingFace and place it in this directory.")
        println("Recommended: https://huggingface.co/TheBloke/phi-2-GGUF/resolve/main/phi-2.Q4_K_M.gguf")
    }
}

// Task to copy native libraries for distribution
tasks.register<Copy>("copyNativeLibs") {
    dependsOn("jar")
    from(configurations.runtimeClasspath.get().filter { 
        it.name.contains("llama") || it.name.contains("jna")
    })
    into(nativesDir)
}

// Ensure models directory exists during build
tasks.named("processResources") {
    dependsOn("createModelDir")
}
