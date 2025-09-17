package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.exceptions.UnmappableUrlException
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.registration.HmppsDomainEventMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime

@Service
@Configuration
class HmppsDomainEventService(
  @Autowired val eventNotificationRepository: EventNotificationRepository,
  @Autowired val deadLetterQueueService: DeadLetterQueueService,
  @Autowired val domainEventIdentitiesResolver: DomainEventIdentitiesResolver,
  @Value("\${services.integration-api.url}") val baseUrl: String,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  private val objectMapper = ObjectMapper()

  fun execute(hmppsDomainEvent: HmppsDomainEvent, integrationEventTypes: List<IntegrationEventType>) {
    val hmppsEvent: HmppsDomainEventMessage = objectMapper.readValue(hmppsDomainEvent.message)

    val hmppsId = domainEventIdentitiesResolver.getHmppsId(hmppsEvent)
    val prisonId = domainEventIdentitiesResolver.getPrisonId(hmppsEvent)

    for (integrationEventType in integrationEventTypes) {
      try {
        val eventNotification = if (integrationEventType == IntegrationEventType.PRISONER_MERGE) {
          val nomisId = hmppsEvent.additionalInformation?.removedNomsNumber!!
          EventNotification(
            eventType = integrationEventType,
            hmppsId = nomisId,
            prisonId = prisonId,
            url = "$baseUrl/${integrationEventType.path(nomisId, prisonId, hmppsEvent.additionalInformation)}",
            lastModifiedDateTime = LocalDateTime.now(),
          )
        } else {
          EventNotification(
            eventType = integrationEventType,
            hmppsId = hmppsId,
            prisonId = prisonId,
            url = "$baseUrl/${integrationEventType.path(hmppsId, prisonId, hmppsEvent.additionalInformation)}",
            lastModifiedDateTime = LocalDateTime.now(),
          )
        }
        eventNotificationRepository.insertOrUpdate(eventNotification)
      } catch (ume: UnmappableUrlException) {
        log.warn(ume.message)
      }
    }
  }
}
