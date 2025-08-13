package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.registration.HmppsDomainEventMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository

@Service
@Configuration
class HmppsDomainEventService(
  @Autowired val eventNotificationRepository: EventNotificationRepository,
  @Autowired val deadLetterQueueService: DeadLetterQueueService,
  @Autowired val integrationEventCreationStrategyProvider: IntegrationEventCreationStrategyProvider,
  @Value("\${services.integration-api.url}") val baseUrl: String,
) {
  private val objectMapper = ObjectMapper()

  fun execute(hmppsDomainEvent: HmppsDomainEvent, integrationEventTypes: List<IntegrationEventType>) {
    val hmppsEvent: HmppsDomainEventMessage = objectMapper.readValue(hmppsDomainEvent.message)
    for (integrationEventType in integrationEventTypes) {
      val notifications = integrationEventCreationStrategyProvider.forEventType(integrationEventType)
        .createNotifications(hmppsEvent, integrationEventType, baseUrl)
      for (notification in notifications) {
        eventNotificationRepository.insertOrUpdate(notification)
      }
    }
  }
}
