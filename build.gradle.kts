group = "be.kommaboard.kareer"
version = "0.0.1"

extra["springBootVersion"] = "2.7.2"
extra["springCloudVersion"] = "2021.0.3"
extra["springCloudKubernetesVersion"] = "2.1.3"

repositories {
    mavenCentral()
}

plugins {
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
    id("org.springframework.boot") version "2.7.2"
    id("io.spring.dependency-management") version "1.0.12.RELEASE"
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.spring") version "1.7.10"
    kotlin("plugin.jpa") version "1.7.10"
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
        mavenBom("org.springframework.cloud:spring-cloud-kubernetes-dependencies:${property("springCloudKubernetesVersion")}")
    }
}

dependencies {
    // Kareer Library
    implementation(project(":common"))
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    // Spring Boot
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    // Spring Cloud
    implementation("org.springframework.cloud:spring-cloud-starter-kubernetes-client")
    implementation("org.springframework.cloud:spring-cloud-starter-kubernetes-client-config")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    // Spring Data
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    // Liquibase
    implementation("org.liquibase:liquibase-core:4.11.0")
    // OpenAPI
    implementation("org.springdoc:springdoc-openapi-ui:1.6.9")
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        apiVersion = "1.6" // Kotlin version
        languageVersion = "1.6" // Kotlin version
        jvmTarget = "17" // JVM version
        freeCompilerArgs = listOf("-Xjsr305=strict") // strict null-safety
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
