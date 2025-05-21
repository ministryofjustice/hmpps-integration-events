package uk.gov.justice.digital.hmpps.hmppsintegrationevents

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
class HmppsIntegrationEvents

fun main(args: Array<String>) {
  runApplication<HmppsIntegrationEvents>(*args)
}
