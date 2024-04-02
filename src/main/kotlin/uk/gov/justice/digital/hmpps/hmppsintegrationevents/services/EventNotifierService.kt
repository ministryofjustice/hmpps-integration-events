package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import java.time.LocalDateTime

@Service
class EventNotifierService(
  val eventRepository: EventNotificationRepository,
) {

  @Scheduled(fixedRate = 10000)
  fun sentNotifications() {
    val fiveMinutesAgo = LocalDateTime.now().minusMinutes(5)
    val events = eventRepository.findAllWithLastModifiedDateTimeBefore(fiveMinutesAgo)

    var x =""
  }
}
