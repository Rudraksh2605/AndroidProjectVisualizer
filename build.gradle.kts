plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.0.13"
}

group = "com.projectvisualizer"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {

    // Java Parser
    implementation("com.github.javaparser:javaparser-core:3.24.2")

    // Graph Visualization
    implementation("org.graphstream:gs-core:2.0")
    implementation("org.graphstream:gs-ui-javafx:2.0")

    // Kotlin Parser
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.6.21")

    implementation("org.controlsfx:controlsfx:11.1.2")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0")

    implementation("org.yaml:snakeyaml:1.30")


    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

javafx {
    version = "17.0.2"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics")
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
}