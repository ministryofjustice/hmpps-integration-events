package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import java.time.LocalDateTime

@Service
@Configuration
class EventNotifierService(
  private val integrationEventTopicService: IntegrationEventTopicService,
  val eventRepository: EventNotificationRepository,
) {
  @Scheduled(fixedRateString = "\${notifier.schedule.rate}")
  fun sentNotifications() {
    val fiveMinutesAgo = LocalDateTime.now().minusMinutes(5)
    val events = eventRepository.findAllWithLastModifiedDateTimeBefore(fiveMinutesAgo)
    events.forEach {
      integrationEventTopicService.sendEvent(it)
      eventRepository.deleteById(it.eventId!!)
    }
  }
}
