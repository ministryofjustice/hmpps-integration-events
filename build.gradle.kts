import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "9.1.1"
  kotlin("plugin.spring") version "2.2.20"
  kotlin("plugin.jpa") version "2.2.20"
  kotlin("plugin.lombok") version "2.2.20"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.8")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

  annotationProcessor("org.projectlombok:lombok:1.18.42")
  testAnnotationProcessor("org.projectlombok:lombok:1.18.42")
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("com.google.code.gson:gson:2.13.2")
  // This needs to be fixed in hmpps-sqs-spring-boot-starter so the version is made available there
  // Pinning to version 3.4.0 in the meantime
  implementation("io.awspring.cloud:spring-cloud-aws-starter-s3:3.4.0")
  implementation("software.amazon.awssdk:secretsmanager")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.4.11") {
    exclude("org.springframework.security", "spring-security-config")
    exclude("org.springframework.security", "spring-security-core")
    exclude("org.springframework.security", "spring-security-crypto")
    exclude("org.springframework.security", "spring-security-web")
    exclude("org.apache.common", "commons-compress")
  }
  implementation("io.sentry:sentry-spring-boot-starter-jakarta:7.18.0")
  implementation("io.jsonwebtoken:jjwt-api:0.13.0")

  testImplementation("org.testcontainers:localstack:1.21.3")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("org.apache.commons:commons-compress:1.28.0")
  testImplementation("io.kotest:kotest-assertions-json-jvm:6.0.3")
  testImplementation("io.kotest:kotest-runner-junit5-jvm:6.0.3")
  testImplementation("io.kotest:kotest-assertions-core-jvm:6.0.3")
  testImplementation("com.h2database:h2:2.3.232")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:4.1.1")
  testImplementation("org.wiremock:wiremock-standalone:3.13.1")
  testImplementation("io.mockk:mockk:1.14.5")
  testImplementation("io.mockk:mockk-agent-jvm:1.14.5")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<KotlinCompile> {
    compilerOptions {
      jvmTarget = JvmTarget.JVM_21
      freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
  }
}
