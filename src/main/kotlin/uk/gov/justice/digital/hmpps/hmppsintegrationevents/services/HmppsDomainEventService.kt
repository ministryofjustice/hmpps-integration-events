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

@Service
@Configuration
class HmppsDomainEventService(
  @Autowired val eventNotificationRepository: EventNotificationRepository,
  @Autowired val deadLetterQueueService: DeadLetterQueueService,
  @Autowired val integrationEventCreationStrategyProvider: IntegrationEventCreationStrategyProvider,
  @Value("\${services.integration-api.url}") val baseUrl: String,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  private val objectMapper = ObjectMapper()

  fun execute(hmppsDomainEvent: HmppsDomainEvent, integrationEventTypes: List<IntegrationEventType>) {
    val hmppsEvent: HmppsDomainEventMessage = objectMapper.readValue(hmppsDomainEvent.message)
    for (integrationEventType in integrationEventTypes) {
      val notifications = try {
        integrationEventCreationStrategyProvider.forEventType(integrationEventType)
          .createNotifications(hmppsEvent, integrationEventType, baseUrl)
      } catch (ume: UnmappableUrlException) {
        log.warn(ume.message)
        emptyList()
      }
      for (notification in notifications) {
        eventNotificationRepository.insertOrUpdate(notification)
      }
    }
  }
}
