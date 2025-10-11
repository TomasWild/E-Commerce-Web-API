plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "com.wild"
version = "0.0.1-SNAPSHOT"
description = "ecommerce"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

val mockitoAgent = configurations.create("mockitoAgent")

dependencies {
    implementation(libs.bundles.spring.boot.starter)
    implementation(libs.jspecify)
    implementation(libs.dotenv.java)
    implementation(libs.springdoc.openapi)
    runtimeOnly(libs.mysql)
    runtimeOnly(libs.h2)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testImplementation(libs.bundles.spring.boot.test)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.mockito.core)
    testRuntimeOnly(libs.junit.platform.launcher)

    mockitoAgent(libs.mockito.core) { isTransitive = false }
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-javaagent:${mockitoAgent.asPath}")
}
