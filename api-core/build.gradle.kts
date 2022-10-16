plugins {
    java
    jacoco
    `maven-publish`
    id("com.diffplug.spotless") version "6.4.2"
    id("io.freefair.lombok") version "6.4.3"
    id("ai.clarity.codeartifact") version "0.0.12"
}

group = "com.example.api.core"
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
    implementation(platform("org.springframework.boot:spring-boot-dependencies:2.7.4"))
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2021.0.1"))
    compileOnly("org.springframework:spring-context")
    implementation("com.playtika.reactivefeign:feign-reactor-spring-cloud-starter:3.2.6")
    // implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation(project(":spring-core"))
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
