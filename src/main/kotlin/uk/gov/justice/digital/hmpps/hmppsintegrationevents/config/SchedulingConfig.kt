package uk.gov.justice.digital.hmpps.hmppsintegrationevents.config

import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@Profile("!test")
class SchedulingConfig {
}