package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.SqsMessage
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
  fun execute(sqsMessage: SqsMessage) {
    log.info("Received sqsMessage with type {}", sqsMessage.type)
    log.info("Received sqsMessage with a message body of {}", sqsMessage.message)
    log.info("Received sqsMessage with a messageId {}", sqsMessage.messageId)

    // AKH TODO Check for an existing one first

    val eventType = EventTypeValue.from(sqsMessage.message.eventType)

    if (eventType != null) {
      val event = EventNotification(
        eventType = eventType,
        // AKH TODO find the ID from the message
        hmppsId = "",
        // AKH TODO generate this so they can quickly access API
        url = "",
        lastModifiedDateTime = LocalDateTime.now(),
      )

      repo.save(event)
    } else {
      // AKH TODO what do we do this this is not an event type we support?
    }
  }
}
