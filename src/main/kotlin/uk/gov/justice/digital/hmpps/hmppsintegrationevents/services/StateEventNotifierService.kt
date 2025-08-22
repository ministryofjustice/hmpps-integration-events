package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.sentry.Sentry
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import java.time.LocalDateTime
import java.util.*

@Service
@ConditionalOnProperty("feature-flag.event-state-management", havingValue = "true")
@Configuration
class StateEventNotifierService(
  private val integrationEventTopicService: IntegrationEventTopicService,
  val eventRepository: EventNotificationRepository,
) {
  @Scheduled(fixedRateString = "\${notifier.schedule.rate}")
  fun sentNotifications() {
    val fiveMinutesAgo = LocalDateTime.now().minusMinutes(5)

    val claimId = UUID.randomUUID().toString()
    // Claim records to process
    eventRepository.setProcessing(fiveMinutesAgo, claimId)

    val events = eventRepository.findAllProcessingEvents(claimId)
    events.forEach {
      try {
        integrationEventTopicService.sendEvent(it)
        eventRepository.setProcessed(it.eventId!!)
      } catch (e: Exception) {
        Sentry.captureException(e)
      }
    }
  }
}
