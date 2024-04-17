package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.EventTypeValue
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DeadLetterQueueService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.RegistrationEventsService
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@ActiveProfiles("test")
@JsonTest
class HmppsDomainEventsListenerTest {
  private val mockRegistrationEventsService: RegistrationEventsService = mock()
  private val mockDlqService: DeadLetterQueueService = mock()
  private val hmppsDomainEventsListener: HmppsDomainEventsListener = HmppsDomainEventsListener(mockRegistrationEventsService, mockDlqService)
  private val currentTime: ZonedDateTime = LocalDateTime.now().atZone(ZoneId.systemDefault())

  @Test
  fun `when a valid registration added sqs event is received it should call the registrationEventService`() {
    val rawMessage = SqsNotificationGeneratingHelper(timestamp = currentTime).generateRawRegistrationEvent()
    val hmppsDomainEvent = SqsNotificationGeneratingHelper(currentTime).createRegistrationAddedEvent()

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(mockRegistrationEventsService, times(1)).execute(hmppsDomainEvent, EventTypeValue.REGISTRATION_ADDED)
  }

  @Test
  fun `when an invalid message is received it should be sent to the dead letter queue`() {
    val rawMessage = "Invalid JSON message"

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(mockDlqService, times(1)).sendEvent(eq(rawMessage), any())
  }

  @Test
  fun `when an unexpected event type is received it should be sent to the dead letter queue`() {
    val unexpectedEventType = "unexpected.event.type"
    val rawMessage = SqsNotificationGeneratingHelper(timestamp = currentTime).generateRawRegistrationEvent(eventType = unexpectedEventType)
    val hmppsDomainEvent = SqsNotificationGeneratingHelper(currentTime).createRegistrationAddedEvent(eventType = unexpectedEventType)

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(mockDlqService, times(1)).sendEvent(eq(hmppsDomainEvent), any())
  }

  @Test
  fun `when deserialization fails it should be sent to the dead letter queue`() {
    val rawMessage = "{}"

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(mockDlqService, times(1)).sendEvent(eq(rawMessage), any())
  }
}
