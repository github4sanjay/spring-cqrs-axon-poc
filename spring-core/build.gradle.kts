plugins {
    java
    jacoco
    `maven-publish`
    id("com.diffplug.spotless") version "6.5.1"
    id("io.freefair.lombok") version "6.4.3"
    id("ai.clarity.codeartifact") version "0.0.12"
}

group = "com.example.spring.core"
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
    implementation("org.springframework.boot:spring-boot-starter-json")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
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
