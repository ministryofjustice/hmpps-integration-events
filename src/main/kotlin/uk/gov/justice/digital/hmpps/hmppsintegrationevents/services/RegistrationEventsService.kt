package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.EventTypeValue
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.registration.RegistrationAddedEventMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime

@Service
@Transactional
class RegistrationEventsService(
  @Autowired val repo: EventNotificationRepository,
  @Autowired val deadLetterQueueService: DeadLetterQueueService,
) {
  private val objectMapper = ObjectMapper()

  fun execute(hmppsDomainEvent: HmppsDomainEvent, eventType: EventTypeValue) {
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

  private fun handleMessage(hmppsId: String, eventType: EventTypeValue) {
    if (!repo.existsByHmppsIdAndEventType(hmppsId, eventType)) {
      saveEventNotification(eventType, hmppsId)
    } else {
      updateEventNotification(eventType, hmppsId)
    }
  }

  private fun saveEventNotification(eventType: EventTypeValue, hmppsId: String): EventNotification = (
    repo.save(
      EventNotification(
        eventType = eventType,
        hmppsId = hmppsId,
        url = "/v1/persons/$hmppsId/risks/mappadetail",
        lastModifiedDateTime = LocalDateTime.now(),
      ),
    )
    )

  private fun updateEventNotification(eventType: EventTypeValue, hmppsId: String): Int = (
    repo.updateLastModifiedDateTimeByHmppsIdAndEventType(LocalDateTime.now(), hmppsId, eventType)
    )
}
