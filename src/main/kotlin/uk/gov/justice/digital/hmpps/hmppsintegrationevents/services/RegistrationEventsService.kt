package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IncomingEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.registration.RegistrationAddedEventMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime

@Service
@Configuration
class RegistrationEventsService(
  @Autowired val repo: EventNotificationRepository,
  @Autowired val deadLetterQueueService: DeadLetterQueueService,
  @Value("\${services.integrations-api.url}") val baseUrl: String,
) {
  private val objectMapper = ObjectMapper()

  fun execute(hmppsDomainEvent: HmppsDomainEvent, eventType: IncomingEventType) {
    val registrationEventMessage: RegistrationAddedEventMessage = objectMapper.readValue(hmppsDomainEvent.message)

    if (registrationEventMessage.additionalInformation.isMappRegistrationType()) {
      val hmppsId: String? = registrationEventMessage.personReference.findCrnIdentifier()

      if (hmppsId != null) {
        handleMessage(hmppsId, eventType)
      } else {
        deadLetterQueueService.sendEvent(hmppsDomainEvent, "CRN could not be found in registration event message")
      }
    }
  }

  private fun handleMessage(hmppsId: String, eventType: IncomingEventType) {
    if (!repo.existsByHmppsIdAndEventType(hmppsId, eventType)) {
      saveEventNotification(eventType, hmppsId)
    } else {
      updateEventNotification(eventType, hmppsId)
    }
  }

  private fun saveEventNotification(eventType: IncomingEventType, hmppsId: String): EventNotification = (
    repo.save(
      EventNotification(
        eventType = eventType.outgoingEvent,
        hmppsId = hmppsId,
        url = "$baseUrl/v1/persons/$hmppsId/risks/mappadetail",
        lastModifiedDateTime = LocalDateTime.now(),
      ),
    )
    )

  private fun updateEventNotification(eventType: IncomingEventType, hmppsId: String): Int = (
    repo.updateLastModifiedDateTimeByHmppsIdAndEventType(LocalDateTime.now(), hmppsId, eventType)
    )
}
