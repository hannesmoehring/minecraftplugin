plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "me.hmPlugin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveBaseName.set("AssistPl")
        archiveClassifier.set("")
    }
    
    build {
        dependsOn(shadowJar)
    }
    
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(17) // This ensures compatibility with Java 17+ while allowing you to use Java 21
    }
}