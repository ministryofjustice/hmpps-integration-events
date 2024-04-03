plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.15.5"
  kotlin("plugin.spring") version "1.9.23"
  kotlin("plugin.jpa") version "1.9.23"
  kotlin("plugin.lombok") version "1.9.23"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  runtimeOnly("org.postgresql:postgresql")
  runtimeOnly("org.flywaydb:flyway-core")

  annotationProcessor("org.projectlombok:lombok:1.18.32")
  testAnnotationProcessor("org.projectlombok:lombok:1.18.32")

  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:3.1.2") {
    exclude("org.springframework.security", "spring-security-config")
    exclude("org.springframework.security", "spring-security-core")
    exclude("org.springframework.security", "spring-security-crypto")
    exclude("org.springframework.security", "spring-security-web")
    exclude("org.apache.common", "commons-compress")
  }
  testImplementation("org.testcontainers:localstack:1.19.7")
  testImplementation("org.apache.commons:commons-compress:1.26.1")
  testImplementation("io.kotest:kotest-assertions-json-jvm:5.8.0")
  testImplementation("io.kotest:kotest-runner-junit5-jvm:5.8.0")
  testImplementation("io.kotest:kotest-assertions-core-jvm:5.8.0")
  testImplementation("com.h2database:h2:2.2.224")
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
