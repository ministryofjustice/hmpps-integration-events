package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway.ProbationIntegrationApiGateway
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventTypes
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.registration.HmppsDomainEventMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime

@Service
@Configuration
class HmppsDomainEventService(
  @Autowired val repo: EventNotificationRepository,
  @Autowired val deadLetterQueueService: DeadLetterQueueService,
  @Autowired val probationIntegrationApiGateway: ProbationIntegrationApiGateway,
  @Value("\${services.integration-api.url}") val baseUrl: String,
) {
  private val objectMapper = ObjectMapper()

  fun execute(hmppsDomainEvent: HmppsDomainEvent, eventType: IntegrationEventTypes) {
    val hmppsEvent: HmppsDomainEventMessage = objectMapper.readValue(hmppsDomainEvent.message)
    val hmppsId = getHmppsId(hmppsEvent)

    if (hmppsId != null) {
      val notification = getEventNotification(eventType, hmppsId)

      if (notification != null) {
        handleMessage(notification)
      }
    } else {
      throw NotFoundException("Identifier could not be found in domain event message ${hmppsDomainEvent.messageId}")
    }
  }

  private fun getHmppsId(hmppsEvent: HmppsDomainEventMessage): String? {
    val crn: String? = hmppsEvent.personReference?.findCrnIdentifier()
    if (crn != null) {
      return crn
    }
    val nomsNumber = hmppsEvent.personReference?.findNomsIdentifier() ?: hmppsEvent.additionalInformation.nomsNumber

    nomsNumber?.let {
      return probationIntegrationApiGateway.getPersonIdentifier(nomsNumber)?.crn ?: throw NotFoundException("Person not found nomsNumber $nomsNumber")
    }

    return null
  }

  private fun getEventNotification(integrationEventType: IntegrationEventTypes, hmppsId: String): EventNotification? {
    val eventType = IntegrationEventTypes.from(integrationEventType)
    if (eventType != null) {
      return EventNotification(
        eventType = eventType,
        hmppsId = hmppsId,
        url = "$baseUrl/v1/persons/$hmppsId${eventType.path}",
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
