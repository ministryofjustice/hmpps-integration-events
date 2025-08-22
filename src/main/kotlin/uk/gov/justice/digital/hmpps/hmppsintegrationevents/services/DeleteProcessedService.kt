package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.sentry.Sentry
import jakarta.transaction.Transactional
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import java.time.LocalDateTime

@Service
@ConditionalOnProperty("feature-flag.event-state-management", havingValue = "true")
@Configuration
class DeleteProcessedService(
  private val integrationEventTopicService: IntegrationEventTopicService,
  val eventRepository: EventNotificationRepository,
) {

  @Scheduled(fixedRateString = "\${notifier.schedule.rate}")
  @Transactional
  fun deleteProcessedEvents() {
    val cutOff = LocalDateTime.now().minusHours(24)
    try {
      eventRepository.deleteEvents(cutOff)
    } catch (e: Exception) {
      Sentry.captureException(e)
    }
  }
}
