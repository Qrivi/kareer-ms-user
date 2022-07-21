group = "be.kommaboard.kareer"
version = "0.0.1"

repositories {
    mavenCentral()
}

plugins {
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
    id("org.springframework.boot") version "2.7.1"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
    kotlin("plugin.jpa") version "1.6.21"
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2021.0.3")
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
    implementation("org.springframework.boot:spring-boot-actuator")
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
    // Hibernate Validator
    implementation("org.hibernate.validator:hibernate-validator:6.2.0.Final")
    implementation("org.hibernate.validator:hibernate-validator-annotation-processor:6.2.0.Final")
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
