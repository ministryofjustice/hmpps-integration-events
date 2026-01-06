package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.exceptions.UnmappableUrlException
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.SQSMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import java.time.Clock
import java.time.LocalDateTime

@Service
@Configuration
class HmppsDomainEventService(
  @Autowired val eventNotificationRepository: EventNotificationRepository,
  @Autowired val deadLetterQueueService: DeadLetterQueueService,
  @Autowired val domainEventIdentitiesResolver: DomainEventIdentitiesResolver,
  @Value("\${services.integration-api.url}") val baseUrl: String,
  private val clock: Clock,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  private val objectMapper = ObjectMapper()

  fun execute(hmppsDomainEvent: SQSMessage, integrationEventTypes: List<IntegrationEventType>) {
    val hmppsEvent: HmppsDomainEvent = objectMapper.readValue(hmppsDomainEvent.message)

    val hmppsId = domainEventIdentitiesResolver.getHmppsId(hmppsEvent)
    val prisonId = domainEventIdentitiesResolver.getPrisonId(hmppsEvent)

    for (integrationEventType in integrationEventTypes) {
      try {
        val eventNotification = integrationEventType.getNotification(baseUrl, hmppsId, prisonId, hmppsEvent.additionalInformation, LocalDateTime.now(clock))

        eventNotificationRepository.insertOrUpdate(eventNotification)
      } catch (ume: UnmappableUrlException) {
        log.warn(ume.message)
      }
    }
  }
}
