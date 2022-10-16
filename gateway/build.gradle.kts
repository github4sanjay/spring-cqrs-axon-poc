plugins {
    java
    jacoco
    id("org.springframework.boot") version "2.7.4"
    id("io.freefair.lombok") version "6.4.3"
    id("com.diffplug.spotless") version "6.5.1"
    id("ai.clarity.codeartifact") version "0.0.12"
}

springBoot {
    buildInfo()
}

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
    implementation(platform("org.springframework.boot:spring-boot-dependencies:2.7.4"))
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2021.0.4"))
    implementation(platform("software.amazon.awssdk:bom:2.17.179"))
    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:2.4.1"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.cloud:spring-cloud-starter-consul-discovery")
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation("org.springframework.cloud:spring-cloud-starter-kubernetes-fabric8-all")
    implementation("io.awspring.cloud:spring-cloud-starter-aws-secrets-manager-config")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.cloud:spring-cloud-starter-sleuth")
    implementation("org.springframework.security:spring-security-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-oauth2-jose")

    implementation("org.springdoc:springdoc-openapi-security:1.6.8")
    implementation("org.springdoc:springdoc-openapi-webflux-ui:1.6.8")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.0")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("io.github.openfeign:feign-jackson")
    implementation("com.amazonaws:aws-java-sdk-sts")
    implementation("software.amazon.awssdk:sts")

    implementation(project(":security-core"))

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
