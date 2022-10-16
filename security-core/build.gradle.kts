plugins {
    java
    jacoco
    `maven-publish`
    id("io.freefair.lombok") version "6.4.3"
    id("com.diffplug.spotless") version "6.5.1"
    id("ai.clarity.codeartifact") version "0.0.12"
}

group = "com.example.security-core"
version = "0.1"
java.sourceCompatibility = JavaVersion.VERSION_17
val artifactoryURL: String by project

repositories {
    maven {
        url = uri(artifactoryURL)
    }
    mavenLocal()
    mavenCentral()
}

publishing {
    repositories {
        maven {
            url = uri(artifactoryURL)
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.1")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

spotless {
    java {
        googleJavaFormat()
    }
    kotlinGradle {
        ktlint()
    }
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(false)
    }
}
