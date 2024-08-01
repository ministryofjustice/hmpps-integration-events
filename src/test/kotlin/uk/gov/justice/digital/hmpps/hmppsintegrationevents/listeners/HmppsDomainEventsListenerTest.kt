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
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventTypes
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DeadLetterQueueService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.HmppsDomainEventService
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@ActiveProfiles("test")
@JsonTest
class HmppsDomainEventsListenerTest {
  private val hmppsDomainEventService = mockk<HmppsDomainEventService>()
  private val deadLetterQueueService = mockk<DeadLetterQueueService>()
  private val hmppsDomainEventsListener: HmppsDomainEventsListener = HmppsDomainEventsListener(hmppsDomainEventService, deadLetterQueueService)
  private val currentTime: ZonedDateTime = LocalDateTime.now().atZone(ZoneId.systemDefault())

  @BeforeEach
  fun setup() {
    every { deadLetterQueueService.sendEvent(any(), any()) } returnsArgument 0
  }

  @Test
  fun `when a valid registration added sqs event is received it should call the hmppsDomainEventService`() {
    val rawMessage = SqsNotificationGeneratingHelper(timestamp = currentTime).generateRawHmppsDomainEvent()
    val hmppsDomainEvent = SqsNotificationGeneratingHelper(currentTime).createHmppsDomainEvent()

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventTypes.MAPPA_DETAIL_CHANGED) }
  }

  @Test
  fun `when a valid registration updated sqs event is received it should call the hmppsDomainEventService`() {
    val rawMessage = SqsNotificationGeneratingHelper(timestamp = currentTime).generateRawHmppsDomainEvent("probation-case.registration.updated")
    val hmppsDomainEvent = SqsNotificationGeneratingHelper(currentTime).createHmppsDomainEvent("probation-case.registration.updated")

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventTypes.MAPPA_DETAIL_CHANGED) }
  }

  @Test
  fun `when risk-assessment scores determined event is received it should call the hmppsDomainEventService`() {
    val rawMessage = SqsNotificationGeneratingHelper(timestamp = currentTime).generateRawHmppsDomainEventWithoutRegisterType("risk-assessment.scores.determined", messageEventType = "risk-assessment.scores.ogrs.determined")
    val hmppsDomainEvent = SqsNotificationGeneratingHelper(currentTime).createHmppsDomainEventWithoutRegisterType("risk-assessment.scores.ogrs.determined", attributeEventTypes = "risk-assessment.scores.determined")

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventTypes.RISK_SCORE_CHANGED) }
  }

  @Test
  fun `when an invalid message is received it should be sent to the dead letter queue`() {
    val rawMessage = "Invalid JSON message"

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify { hmppsDomainEventService wasNot Called }
    verify(exactly = 1) { deadLetterQueueService.sendEvent(rawMessage, "Malformed event received. Could not parse JSON") }
  }

  @Test
  fun `when an unexpected event type is received it should be sent to the dead letter queue`() {
    val unexpectedEventType = "unexpected.event.type"
    val rawMessage = SqsNotificationGeneratingHelper(timestamp = currentTime).generateRawHmppsDomainEvent(eventType = unexpectedEventType)
    val hmppsDomainEvent = SqsNotificationGeneratingHelper(currentTime).createHmppsDomainEvent(eventType = unexpectedEventType)

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify { hmppsDomainEventService wasNot Called }
    verify { deadLetterQueueService wasNot Called }
  }
}
