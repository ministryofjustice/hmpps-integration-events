package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import java.time.Clock
import java.time.LocalDateTime

@Service
@Configuration
class DeleteProcessedService(
  val eventRepository: EventNotificationRepository,
  private val telemetryService: TelemetryService,
  private val clock: Clock,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Scheduled(fixedRateString = "\${notifier.schedule.rate}")
  @Transactional
  fun deleteProcessedEvents() {
    val cutOff = LocalDateTime.now(clock).minusHours(24)
    try {
      log.info("Deleting processed events older than $cutOff")
      eventRepository.deleteEvents(cutOff)
    } catch (e: Exception) {
      log.error("Error deleting processed events", e)
      telemetryService.captureException(e)
    }
    log.info("Successfully deleted processed events")
  }
}
