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

  /**
   * The hmpps id is an id that the end client will use in on going processing.
   * In the future when we have a core person record it will be that id
   * for now the id will default to the crn but if there is no crn it will be the noms number.
   * The end client that receives the messages must treat it as a hmpps_id and NOT a crn/noms number.
   * A look up service exist to decode the hmpps_id into a crn or noms number.
   */
  private fun getHmppsId(hmppsEvent: HmppsDomainEventMessage): String? {
    val crn: String? = hmppsEvent.personReference?.findCrnIdentifier()
    if (crn != null) {
      probationIntegrationApiGateway.getPersonExists(crn).let {
        if (it.existsInDelius) {
          return crn
        }
        throw NotFoundException("Person with crn $crn not found")
      }
    }

    val nomsNumber = hmppsEvent.personReference?.findNomsIdentifier()
      ?: hmppsEvent.additionalInformation?.nomsNumber
      ?: hmppsEvent.additionalInformation?.prisonerId

    return nomsNumber?.let { noms ->
      probationIntegrationApiGateway.getPersonIdentifier(noms)?.crn ?: noms
    }
  }

  private fun getEventNotification(integrationEventType: IntegrationEventTypes, hmppsId: String): EventNotification? {
    val eventType = IntegrationEventTypes.from(integrationEventType)
    if (eventType != null) {
      return EventNotification(
        eventType = eventType,
        hmppsId = hmppsId,
        url = "$baseUrl/${eventType.path(hmppsId)}",
        lastModifiedDateTime = LocalDateTime.now(),
      )
    }
    return null
  }

  private fun handleMessage(notification: EventNotification) {
    if (!repo.existsByHmppsIdAndEventType(notification.hmppsId, notification.eventType)) {
      repo.save(notification)
    }
  }
}
