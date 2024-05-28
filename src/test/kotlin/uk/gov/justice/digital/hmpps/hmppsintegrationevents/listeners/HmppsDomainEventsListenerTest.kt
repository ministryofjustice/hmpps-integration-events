package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IncomingEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DeadLetterQueueService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.RegistrationEventsService
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@ActiveProfiles("test")
@JsonTest
class HmppsDomainEventsListenerTest {
  private val registrationEventsService = mockk<RegistrationEventsService>()
  private val deadLetterQueueService = mockk<DeadLetterQueueService>()
  private val hmppsDomainEventsListener: HmppsDomainEventsListener = HmppsDomainEventsListener(registrationEventsService, deadLetterQueueService)
  private val currentTime: ZonedDateTime = LocalDateTime.now().atZone(ZoneId.systemDefault())

  @BeforeEach
  fun setup() {
    every { deadLetterQueueService.sendEvent(any(), any()) } returnsArgument 0
  }

  @Test
  fun `when a valid registration added sqs event is received it should call the registrationEventService`() {
    val rawMessage = SqsNotificationGeneratingHelper(timestamp = currentTime).generateRawRegistrationEvent()
    val hmppsDomainEvent = SqsNotificationGeneratingHelper(currentTime).createRegistrationAddedDomainEvent()

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(exactly = 1) { registrationEventsService.execute(hmppsDomainEvent, IncomingEventType.REGISTRATION_ADDED) }
  }

  @Test
  fun `when a valid registration updated sqs event is received it should call the registrationEventService`() {
    val rawMessage = SqsNotificationGeneratingHelper(timestamp = currentTime).generateRawRegistrationEvent(IncomingEventType.REGISTRATION_UPDATED.value)
    val hmppsDomainEvent = SqsNotificationGeneratingHelper(currentTime).createRegistrationAddedDomainEvent(eventType = IncomingEventType.REGISTRATION_UPDATED.value)

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(exactly = 1) { registrationEventsService.execute(hmppsDomainEvent, IncomingEventType.REGISTRATION_UPDATED) }
  }

  @Test
  fun `when an invalid message is received it should be sent to the dead letter queue`() {
    val rawMessage = "Invalid JSON message"

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify { registrationEventsService wasNot Called }
    verify(exactly = 1) { deadLetterQueueService.sendEvent(rawMessage, "Malformed event received. Could not parse JSON") }
  }

  @Test
  fun `when an unexpected event type is received it should be sent to the dead letter queue`() {
    val unexpectedEventType = "unexpected.event.type"
    val rawMessage = SqsNotificationGeneratingHelper(timestamp = currentTime).generateRawRegistrationEvent(eventType = unexpectedEventType)
    val hmppsDomainEvent = SqsNotificationGeneratingHelper(currentTime).createRegistrationAddedDomainEvent(eventType = unexpectedEventType)

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify { registrationEventsService wasNot Called }
    verify(exactly = 1) { deadLetterQueueService.sendEvent(hmppsDomainEvent, "Unexpected event type ${hmppsDomainEvent.messageAttributes.eventType.value}") }
  }
}
