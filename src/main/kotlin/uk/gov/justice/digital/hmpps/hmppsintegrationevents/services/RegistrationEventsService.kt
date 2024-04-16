package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.EventTypeValue
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.registration.RegistrationAddedEventMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime

@Service
class RegistrationEventsService(
  @Autowired val repo: EventNotificationRepository,
  @Value("\${services.integrations-api.base-url}") val baseUrl: String
) {
  private val objectMapper = ObjectMapper()


  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun execute(hmppsDomainEvent: HmppsDomainEvent) {
    val registrationEventMessage: RegistrationAddedEventMessage = objectMapper.readValue(hmppsDomainEvent.message)

    val eventType = EventTypeValue.from(hmppsDomainEvent.messageAttributes.eventType.value)
    val hmppsId = registrationEventMessage.personReference.findCrnIdentifier()

    if (eventType != null && hmppsId != null) {
      if (!repo.existsByHmppsIdAndEventType(hmppsId, eventType)) {
        val event = EventNotification(
          eventType = eventType,
          hmppsId = hmppsId,
          url = "$baseUrl/v1/persons/$hmppsId/risks/mappadetail",
          lastModifiedDateTime = LocalDateTime.now(),
        )
        repo.save(event)
      } else {
        // TODO update date time of existing record
        log.info("A similar SQS Event for nominal $hmppsId of type $eventType has already been processed")
      }
    }
  }
}
