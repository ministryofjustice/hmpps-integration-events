package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime

@Service
class DateTimeService(
  private val clock: Clock,
) {
  fun now() = LocalDateTime.now(clock)!!

  fun instantNow() = Instant.now(clock)
}
