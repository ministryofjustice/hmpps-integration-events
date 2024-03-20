package uk.gov.justice.digital.hmpps.hmppsintegrationevents

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HmppsIntegrationEvents

fun main(args: Array<String>) {
  runApplication<HmppsIntegrationEvents>(*args)
}
