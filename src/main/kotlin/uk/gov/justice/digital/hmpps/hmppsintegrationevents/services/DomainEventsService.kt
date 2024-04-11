package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.EventTypeValue
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime

@Service
class DomainEventsService(
  @Autowired val repo: EventNotificationRepository,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun execute(hmppsDomainEvent: HmppsDomainEvent) {
    log.info("Received hmppsDomainEvent with type {}", hmppsDomainEvent.type)
    log.info("Received hmppsDomainEvent with a message body of {}", hmppsDomainEvent.message)
    log.info("Received hmppsDomainEvent with a messageId {}", hmppsDomainEvent.messageId)
    log.info("Received hmppsDomainEvent with a messageAttributes {}", hmppsDomainEvent.messageAttributes)

    val eventType = EventTypeValue.from(hmppsDomainEvent.messageAttributes.eventType.value)

    if (eventType != null) {
      val event = EventNotification(
        eventType = eventType,
        hmppsId = hmppsDomainEvent.message.personReference.identifiers[0].value,
        url = "test.registration.url",
        lastModifiedDateTime = LocalDateTime.now(),
      )

      repo.save(event)
    }
  }
}
