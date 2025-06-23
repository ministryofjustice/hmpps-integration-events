plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "8.2.0"
  kotlin("plugin.spring") version "2.1.21"
  kotlin("plugin.jpa") version "2.1.21"
  kotlin("plugin.lombok") version "2.1.21"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.7")
  runtimeOnly("org.flywaydb:flyway-core")

  annotationProcessor("org.projectlombok:lombok:1.18.38")
  testAnnotationProcessor("org.projectlombok:lombok:1.18.38")
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("com.google.code.gson:gson:2.13.1")
  implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
  implementation("software.amazon.awssdk:secretsmanager")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.4.5") {
    exclude("org.springframework.security", "spring-security-config")
    exclude("org.springframework.security", "spring-security-core")
    exclude("org.springframework.security", "spring-security-crypto")
    exclude("org.springframework.security", "spring-security-web")
    exclude("org.apache.common", "commons-compress")
  }
  implementation("io.sentry:sentry-spring-boot-starter-jakarta:7.18.0")
  testImplementation("org.testcontainers:localstack:1.21.2")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("org.apache.commons:commons-compress:1.27.1")
  testImplementation("io.kotest:kotest-assertions-json-jvm:5.9.1")
  testImplementation("io.kotest:kotest-runner-junit5-jvm:5.9.1")
  testImplementation("io.kotest:kotest-assertions-core-jvm:5.9.1")
  testImplementation("com.h2database:h2:2.3.232")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:4.1.1")
  testImplementation("org.wiremock:wiremock-standalone:3.13.1")
  testImplementation("io.mockk:mockk:1.14.4")
  testImplementation("io.mockk:mockk-agent-jvm:1.14.4")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "21"
    }
  }
}
