plugins {
    id("org.springframework.boot") version "2.7.4"
    id("io.spring.dependency-management") version "1.0.14.RELEASE"
    id("com.diffplug.spotless") version "6.11.0"
    id("io.freefair.lombok") version "6.4.3"
    id("java")
}

group = "com.example.otp"
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
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.axonframework:axon-spring-boot-starter:4.6.1")
    implementation("org.axonframework.extensions.reactor:axon-reactor-spring-boot-starter:4.6.0")
    implementation("org.redisson:redisson-spring-boot-starter:3.17.7")

    implementation(project(":spring-core"))
    implementation(project(":spring-metrics"))
    implementation(project(":spring-web"))
    implementation(project(":spring-axon-reactor"))

    implementation(project(":api-messaging"))
    implementation(project(":api-otp"))

    implementation("com.github.ben-manes.caffeine:caffeine:2.8.5")
    implementation("org.apache.commons:commons-lang3:3.12.0")

    testImplementation("org.signal:embedded-redis:0.8.2")
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
