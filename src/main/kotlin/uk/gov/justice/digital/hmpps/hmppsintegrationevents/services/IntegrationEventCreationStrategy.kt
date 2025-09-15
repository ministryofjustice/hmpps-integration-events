package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEventName.PrisonOffenderEvents
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.registration.HmppsDomainEventMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime

interface IntegrationEventCreationStrategy {
  fun createNotifications(
    hmppsDomainEventMessage: HmppsDomainEventMessage,
    integrationEventType: IntegrationEventType,
    baseUrl: String,
  ): List<EventNotification>
}

open class BaseStrategy(private val domainEventIdentitiesResolver: DomainEventIdentitiesResolver) {
  protected fun getHmppsId(message: HmppsDomainEventMessage): String? = domainEventIdentitiesResolver.getHmppsId(message)
  protected fun getPrisonId(message: HmppsDomainEventMessage): String? = domainEventIdentitiesResolver.getPrisonId(message)
}

@Component
class DefaultEventCreationStrategy(domainEventIdentitiesResolver: DomainEventIdentitiesResolver) :
  BaseStrategy(domainEventIdentitiesResolver),
  IntegrationEventCreationStrategy {
  override fun createNotifications(
    hmppsDomainEventMessage: HmppsDomainEventMessage,
    integrationEventType: IntegrationEventType,
    baseUrl: String,
  ): List<EventNotification> = listOf(
    notification(
      integrationEventType,
      hmppsDomainEventMessage,
      getHmppsId(hmppsDomainEventMessage),
      getPrisonId(hmppsDomainEventMessage),
      baseUrl,
    ),
  )
}

@Component
class MultipleEventCreationStrategy(domainEventIdentitiesResolver: DomainEventIdentitiesResolver) :
  BaseStrategy(domainEventIdentitiesResolver),
  IntegrationEventCreationStrategy {
  override fun createNotifications(
    hmppsDomainEventMessage: HmppsDomainEventMessage,
    integrationEventType: IntegrationEventType,
    baseUrl: String,
  ): List<EventNotification> {
    if (hmppsDomainEventMessage.eventType == PrisonOffenderEvents.Prisoner.MERGED) {
      return listOf(
        notification(
          integrationEventType,
          hmppsDomainEventMessage,
          hmppsDomainEventMessage.additionalInformation?.removedNomsNumber,
          getPrisonId(hmppsDomainEventMessage),
          baseUrl,
        ),
        notification(
          integrationEventType,
          hmppsDomainEventMessage,
          hmppsDomainEventMessage.additionalInformation?.nomsNumber,
          getPrisonId(hmppsDomainEventMessage),
          baseUrl,
        ),
      )
    }
    else {
      return listOf(
        notification(
          integrationEventType,
          hmppsDomainEventMessage,
          getHmppsId(hmppsDomainEventMessage),
          getPrisonId(hmppsDomainEventMessage),
          baseUrl,
        ),
      )
    }
  }
}

private fun notification(
  integrationEventType: IntegrationEventType,
  hmppsDomainEventMessage: HmppsDomainEventMessage,
  hmppsId: String?,
  prisonId: String?,
  baseUrl: String,
): EventNotification = EventNotification(
  eventType = integrationEventType,
  hmppsId = hmppsId,
  prisonId = prisonId,
  url = "$baseUrl/${integrationEventType.path(hmppsId, prisonId, hmppsDomainEventMessage.additionalInformation)}",
  lastModifiedDateTime = LocalDateTime.now(),
)
