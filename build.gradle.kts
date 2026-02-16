import kotlinx.kover.gradle.plugin.dsl.tasks.KoverReport
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "9.3.0"
  kotlin("plugin.spring") version "2.3.10"
  kotlin("plugin.jpa") version "2.3.10"
  kotlin("plugin.lombok") version "2.3.10"
  id("io.gitlab.arturbosch.detekt") version "1.23.8"
  id("org.jetbrains.kotlinx.kover") version "0.9.7"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

configurations.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.apache.logging.log4j") {
      useVersion("2.25.3")
      because("Fix CVE-2025-68161")
    }
    if (requested.group == "ch.qos.logback") {
      useVersion("1.5.25")
      because("Fix CVE-2026-1225")
    }
  }
}

dependencies {
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.10")
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
  implementation("io.awspring.cloud:spring-cloud-aws-starter-s3:3.4.2")
  implementation("software.amazon.awssdk:secretsmanager")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.0.0") {
    exclude("org.springframework.security", "spring-security-config")
    exclude("org.springframework.security", "spring-security-core")
    exclude("org.springframework.security", "spring-security-crypto")
    exclude("org.springframework.security", "spring-security-web")
    exclude("org.apache.common", "commons-compress")
  }
  implementation("io.sentry:sentry-spring-boot-starter-jakarta:8.32.0")
  implementation("io.jsonwebtoken:jjwt-api:0.13.0")
  testImplementation("org.testcontainers:localstack:1.21.4")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("org.apache.commons:commons-compress:1.28.0")
  testImplementation("io.kotest:kotest-assertions-json-jvm:6.1.3")
  testImplementation("io.kotest:kotest-runner-junit5-jvm:6.1.3")
  testImplementation("io.kotest:kotest-assertions-core-jvm:6.1.3")
  testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
  testImplementation("com.h2database:h2:2.4.240")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:4.1.1")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("io.mockk:mockk:1.14.9")
  testImplementation("io.mockk:mockk-agent-jvm:1.14.9")
}

kotlin {
  jvmToolchain(21)
}

tasks {

  val classesToBeExcluded =
    arrayOf(
      "uk.gov.justice.digital.hmpps.hmppsintegrationevents.HmppsIntegrationEventsKt",
    )

  withType<KoverReport>().configureEach {
    kover {
      reports {
        filters {
          excludes {
            classes(*classesToBeExcluded)
          }
        }
      }
    }
  }

  withType<KotlinCompile> {
    compilerOptions {
      jvmTarget = JvmTarget.JVM_21
      freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
  }

  withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    source = source.asFileTree
  }
}

detekt {
  config.setFrom("./detekt.yml")
  buildUponDefaultConfig = true
  ignoreFailures = true
  baseline = file("./detekt-baseline.xml")
}

configurations.matching { it.name == "detekt" }.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin") {
      useVersion(
        io.gitlab.arturbosch.detekt
          .getSupportedKotlinVersion(),
      )
    }
  }
}
