package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.EventTypeValue
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DeadLetterQueueService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.RegistrationEventsService

@Service
class HmppsDomainEventsListener(@Autowired val registrationEventsService: RegistrationEventsService, @Autowired val deadLetterQueueService: DeadLetterQueueService) {

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val objectMapper = ObjectMapper()

  @SqsListener("prisoner", factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) {
    log.info("Received message: $rawMessage")
    try {
      val hmppsDomainEvent: HmppsDomainEvent = objectMapper.readValue(rawMessage)
      determineEventProcess(hmppsDomainEvent)
    } catch (e: Exception) {
      log.error("Received bad domain event message :$rawMessage", e)
      deadLetterQueueService.sendEvent(rawMessage, e)
    }
  }

  fun determineEventProcess(hmppsDomainEvent: HmppsDomainEvent) {
    val hmppsDomainEventType = EventTypeValue.from(hmppsDomainEvent.messageAttributes.eventType.value)

    when (hmppsDomainEventType) {
      EventTypeValue.REGISTRATION_ADDED -> registrationEventsService.execute(hmppsDomainEvent)
      else -> {
        log.warn("Unexpected event type ${hmppsDomainEvent.messageAttributes.eventType.value}")
        deadLetterQueueService.sendEvent(hmppsDomainEvent, Exception("Unexpected event type ${hmppsDomainEvent.messageAttributes.eventType.value}"))
      }
    }
  }
}
