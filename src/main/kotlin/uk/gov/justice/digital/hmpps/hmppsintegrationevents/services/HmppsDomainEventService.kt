package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventTypes
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.RiskScoreTypes
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.registration.HmppsDomainEventMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime

@Service
@Configuration
class HmppsDomainEventService(
  @Autowired val repo: EventNotificationRepository,
  @Autowired val deadLetterQueueService: DeadLetterQueueService,
  @Value("\${services.integration-api.url}") val baseUrl: String,
) {
  private val objectMapper = ObjectMapper()

  fun execute(hmppsDomainEvent: HmppsDomainEvent, eventType: IntegrationEventTypes) {
    val registrationEventMessage: HmppsDomainEventMessage = objectMapper.readValue(hmppsDomainEvent.message)
    val hmppsId: String? = registrationEventMessage.personReference.findCrnIdentifier()

    if (hmppsId != null) {
      val notification = when (eventType) {
        IntegrationEventTypes.MAPPA_DETAIL_CHANGED -> getMappsDetailUpdateEvent(registrationEventMessage, hmppsId)
        IntegrationEventTypes.RISK_SCORE_CHANGED -> getRiskScoreChangedEvent(registrationEventMessage, hmppsId)
      }

      if (notification != null) {
        handleMessage(notification)
      }
    } else {
      deadLetterQueueService.sendEvent(hmppsDomainEvent, "CRN could not be found in registration event message")
    }
  }

  private fun getMappsDetailUpdateEvent(message: HmppsDomainEventMessage, hmppsId: String): EventNotification? {
    if (message.additionalInformation.isMappRegistrationType()) {
      return EventNotification(
        eventType = IntegrationEventTypes.MAPPA_DETAIL_CHANGED,
        hmppsId = hmppsId,
        url = "$baseUrl/v1/persons/$hmppsId/risks/mappadetail",
        lastModifiedDateTime = LocalDateTime.now(),
      )
    }
    return null
  }

  private fun getRiskScoreChangedEvent(message: HmppsDomainEventMessage, hmppsId: String): EventNotification? {
    val riskScoreType = RiskScoreTypes.from(message.eventType)
    if (riskScoreType != null) {
      return EventNotification(
        eventType = IntegrationEventTypes.RISK_SCORE_CHANGED,
        hmppsId = hmppsId,
        url = "$baseUrl/v1/persons/$hmppsId/risks/scores",
        lastModifiedDateTime = LocalDateTime.now(),
      )
    }
    return null
  }

  private fun handleMessage(notification: EventNotification) {
    if (!repo.existsByHmppsIdAndEventType(notification.hmppsId, notification.eventType)) {
      repo.save(notification)
    } else {
      repo.updateLastModifiedDateTimeByHmppsIdAndEventType(LocalDateTime.now(), notification.hmppsId, notification.eventType)
    }
  }
}
