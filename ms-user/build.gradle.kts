// ms-user
group = "be.kommaboard.kareer"
version = "0.0.4"

extra["springBootVersion"] = "3.0.3"
extra["springCloudVersion"] = "2022.0.1"
extra["springCloudKubernetesVersion"] = "3.0.1"

java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.qrivi.dev/snapshots")
    }
}

plugins {
    id("org.jlleitschuh.gradle.ktlint") version "11.2.0"
    id("org.springframework.boot") version "3.0.3"
    id("io.spring.dependency-management") version "1.1.0"
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.spring") version "1.8.10"
    kotlin("plugin.jpa") version "1.8.10"
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
        mavenBom("org.springframework.cloud:spring-cloud-kubernetes-dependencies:${property("springCloudKubernetesVersion")}")
    }
}

dependencies {
    // Kareer Libraries
    implementation("be.kommaboard.kareer:lib-authorization:0.0.3")
    implementation("be.kommaboard.kareer:lib-common:0.0.3")
    implementation("be.kommaboard.kareer:lib-user:$version")
    implementation("be.kommaboard.kareer:lib-mailing:0.0.2")
    implementation("be.kommaboard.kareer:lib-organization:0.0.1")
    implementation("be.kommaboard.kareer:lib-storage:0.0.1")
    // Kotlin (required by Spring Web)
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    // Spring Cloud
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.cloud:spring-cloud-starter-kubernetes-client")
    implementation("org.springframework.cloud:spring-cloud-starter-kubernetes-client-loadbalancer")
    // Spring Data
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    // Liquibase
    implementation("org.liquibase:liquibase-core:4.19.0")
    // OpenAPI + Swagger UI
    implementation("org.springdoc:springdoc-openapi-kotlin:1.6.14")
    implementation("org.springdoc:springdoc-openapi-ui:1.6.14")
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.h2database:h2")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        apiVersion = "1.8" // Kotlin version
        languageVersion = "1.8" // Kotlin version
        jvmTarget = "17" // JVM version
        freeCompilerArgs = listOf("-Xjsr305=strict") // strict null-safety
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
