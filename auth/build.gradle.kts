plugins {
    id("org.springframework.boot") version "2.7.4"
    id("io.spring.dependency-management") version "1.0.14.RELEASE"
    id("com.diffplug.spotless") version "6.11.0"
    id("io.freefair.lombok") version "6.4.3"
    id("java")
    id("jacoco")
}

group = "com.example.auth"
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

dependencies {
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2021.0.4"))
    implementation("org.springframework.cloud:spring-cloud-starter-kubernetes-fabric8-all")
    implementation("org.springframework.cloud:spring-cloud-starter-consul-discovery")
    implementation("org.springframework.cloud:spring-cloud-starter-sleuth")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.axonframework:axon-spring-boot-starter:4.6.1")
    implementation("org.axonframework.extensions.reactor:axon-reactor-spring-boot-starter:4.6.0")
    implementation("org.springdoc:springdoc-openapi-webflux-ui:1.6.12")
    implementation("dev.samstevens.totp:totp-spring-boot-starter:1.7.1")

    implementation(project(":spring-core"))
    implementation(project(":spring-metrics"))
    implementation(project(":spring-web"))
    implementation(project(":spring-axon-reactor"))

    implementation(project(":security-core"))

    implementation(project(":api-messaging"))
    implementation(project(":api-otp"))

    implementation("com.auth0:java-jwt:3.10.3")
    implementation("com.nimbusds:nimbus-jose-jwt:8.19")
    implementation("com.github.ben-manes.caffeine:caffeine:2.8.5")
    implementation("de.mkammerer:argon2-jvm:2.11")
    runtimeOnly("com.h2database:h2")
    implementation("me.yaman.can:spring-boot-webflux-h2-console:0.0.1")
    testImplementation("org.axonframework:axon-test:4.6.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
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
