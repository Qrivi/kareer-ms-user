group = "be.kommaboard.kareer"
version = "0.0.2"

extra["springBootVersion"] = "2.7.6"

java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

plugins {
    signing
    `maven-publish`
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
    id("org.springframework.boot") version "2.7.6" apply false
    id("io.spring.dependency-management") version "1.1.0"
    kotlin("jvm") version "1.7.10"
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        register("libUser", MavenPublication::class) {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "snapshots"
            url = uri("https://maven.qrivi.dev/snapshots")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}")
    }
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(kotlin("test"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        apiVersion = "1.7" // Kotlin version
        languageVersion = "1.7" // Kotlin version
        jvmTarget = "17" // JVM version
        freeCompilerArgs = listOf("-Xjsr305=strict") // strict null-safety
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
