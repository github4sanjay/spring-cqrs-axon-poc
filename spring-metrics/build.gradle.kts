plugins {
    java
    jacoco
    `maven-publish`
    id("com.diffplug.spotless") version "6.5.1"
    id("io.freefair.lombok") version "6.4.3"
    id("ai.clarity.codeartifact") version "0.0.12"
}

group = "com.example.spring"
version = "0.1"
java.sourceCompatibility = JavaVersion.VERSION_17
val artifactoryURL: String by project

repositories {
    maven {
        url = uri(artifactoryURL)
    }
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
    implementation(platform("org.springframework.boot:spring-boot-dependencies:2.7.4"))
    implementation ("org.springframework.boot:spring-boot-autoconfigure")
    implementation ("org.springframework:spring-context")
    implementation ("io.micrometer:micrometer-registry-cloudwatch2:1.8.5")
    implementation ("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.axonframework:axon-metrics:4.6.1")
    implementation("org.axonframework:axon-micrometer:4.6.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
}
