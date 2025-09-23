package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.exceptions.StuckEventsException
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import java.time.LocalDateTime
import java.util.*

@Service
@Configuration
class StateEventNotifierService(
  private val integrationEventTopicService: IntegrationEventTopicService,
  val eventRepository: EventNotificationRepository,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Scheduled(fixedRateString = "\${notifier.schedule.rate}")
  fun sentNotifications() {
    alertForAnyStuckMessages()

    val fiveMinutesAgo = LocalDateTime.now().minusMinutes(5)

    val claimId = UUID.randomUUID().toString()

    log.info("Setting to processing with claim id $claimId")
    // Claim records to process
    eventRepository.setProcessing(fiveMinutesAgo, claimId)

    log.info("Set processing with claim id $claimId")

    val events = eventRepository.findAllProcessingEvents(claimId)

    log.info("Sending ${events.size} events for claim id $claimId")
    events.forEach {
      try {
        integrationEventTopicService.sendEvent(it)
        eventRepository.setProcessed(it.eventId!!)
      } catch (e: Exception) {
        log.error("Error caught with msg ${e.message} for claim id $claimId", e)
        Sentry.captureException(e)
      }
    }
    log.info("Successfully sent ${events.size} events for claim id $claimId")
  }

  private fun alertForAnyStuckMessages() {
    val stuck = eventRepository.getStuckEvents(LocalDateTime.now().minusMinutes(10))
    if (stuck.isNotEmpty()) {
      val messages = stuck.map {
        "${it.eventCount} stuck events with status ${it.status}. Earliest event has date ${it.earliestDatetime}"
      }
      Sentry.captureException(StuckEventsException(messages.joinToString("\n")))
    }
  }
}
