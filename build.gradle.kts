import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

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
    implementation(libs.thymeleaf.extras.springsecurity6)
    implementation(libs.bouncy.castle.crypto)
    implementation(libs.aws.s3)
    implementation(libs.jjwt.api)
    runtimeOnly(libs.bundles.jjwt)
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

    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.SHORT
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }

    afterSuite(KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
        if (desc.parent == null) {
            println("Results: ${result.resultType}")
            println("Tests run: ${result.testCount}")
            println("Passed: ${result.successfulTestCount}, Failed: ${result.failedTestCount}, Skipped: ${result.skippedTestCount}")
            println("Duration: ${result.endTime - result.startTime} ms")
        }
    }))
}
