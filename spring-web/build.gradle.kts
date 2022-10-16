plugins {
    java
    jacoco
    `maven-publish`
    id("io.freefair.lombok") version "6.4.3"
    id("com.diffplug.spotless") version "6.5.1"
    id("ai.clarity.codeartifact") version "0.0.12"
}

group = "com.example.spring.web"
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
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("ch.qos.logback.contrib:logback-json-classic:0.1.5")
    implementation("ch.qos.logback.contrib:logback-jackson:0.1.5")
    implementation("net.logstash.logback:logstash-logback-encoder:7.1.1")
    implementation("com.j256.cloudwatchlogbackappender:cloudwatchlogbackappender:2.1")
    implementation("org.codehaus.janino:janino:3.1.7")
    implementation(project(":spring-core"))
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
