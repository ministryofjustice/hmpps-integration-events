package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import com.fasterxml.jackson.core.JsonParseException
import io.mockk.Called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
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
  fun `when risk-assessment scores determined event is received it should call the hmppsDomainEventService`() {
    val rawMessage = SqsNotificationGeneratingHelper(timestamp = currentTime).generateRawHmppsDomainEventWithoutRegisterType("risk-assessment.scores.determined", messageEventType = "risk-assessment.scores.ogrs.determined")
    val hmppsDomainEvent = SqsNotificationGeneratingHelper(currentTime).createHmppsDomainEventWithoutRegisterType("risk-assessment.scores.ogrs.determined", attributeEventTypes = "risk-assessment.scores.determined")

    every { hmppsDomainEventService.execute(hmppsDomainEvent, any()) } just runs

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent, listOf(IntegrationEventType.RISK_SCORE_CHANGED)) }
  }

  @ParameterizedTest
  @CsvSource(
    "probation-case.registration.added, ASFO, PROBATION_STATUS_CHANGED",
    "probation-case.registration.deleted, ASFO, PROBATION_STATUS_CHANGED",
    "probation-case.registration.deregistered, ASFO, PROBATION_STATUS_CHANGED",
    "probation-case.registration.updated, ASFO, PROBATION_STATUS_CHANGED",

    "probation-case.registration.added, WRSM, PROBATION_STATUS_CHANGED",
    "probation-case.registration.deleted, WRSM, PROBATION_STATUS_CHANGED",
    "probation-case.registration.deregistered, WRSM, PROBATION_STATUS_CHANGED",
    "probation-case.registration.updated, WRSM, PROBATION_STATUS_CHANGED",

    "probation-case.registration.added, RCCO, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, RCCO, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, RCCO, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, RCCO, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, RCPR, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, RCPR, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, RCPR, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, RCPR, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, RVAD, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, RVAD, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, RVAD, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, RVAD, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, STRG, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, STRG, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, STRG, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, STRG, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, AVIS, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, AVIS, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, AVIS, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, AVIS, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, WEAP, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, WEAP, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, WEAP, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, WEAP, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, RLRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, RLRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, RLRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, RLRH, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, RMRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, RMRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, RMRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, RMRH, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, RHRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, RHRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, RHRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, RHRH, DYNAMIC_RISKS_CHANGED",
  )
  fun `will process and save a person status event`(eventType: String, registerTypeCode: String, integrationEvent: String) {
    val rawMessage = SqsNotificationGeneratingHelper(timestamp = currentTime).generateRawHmppsDomainEvent(eventType, registerTypeCode = registerTypeCode)
    val hmppsDomainEvent = SqsNotificationGeneratingHelper(currentTime).createHmppsDomainEvent(eventType, registerTypeCode = registerTypeCode)

    every { hmppsDomainEventService.execute(hmppsDomainEvent, any()) } just runs

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent, listOf(IntegrationEventType.valueOf(integrationEvent))) }
  }

  @Test
  fun `when a valid registration added sqs event is received it should call the hmppsDomainEventService`() {
    val rawMessage = SqsNotificationGeneratingHelper(timestamp = currentTime).generateRawHmppsDomainEvent()
    val hmppsDomainEvent = SqsNotificationGeneratingHelper(currentTime).createHmppsDomainEvent()

    every { hmppsDomainEventService.execute(hmppsDomainEvent, any()) } just runs

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent, listOf(IntegrationEventType.MAPPA_DETAIL_CHANGED)) }
  }

  @Test
  fun `when a valid registration updated sqs event is received it should call the hmppsDomainEventService`() {
    val rawMessage = SqsNotificationGeneratingHelper(timestamp = currentTime).generateRawHmppsDomainEvent("probation-case.registration.updated")
    val hmppsDomainEvent = SqsNotificationGeneratingHelper(currentTime).createHmppsDomainEvent("probation-case.registration.updated")

    every { hmppsDomainEventService.execute(hmppsDomainEvent, any()) } just runs

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent, listOf(IntegrationEventType.MAPPA_DETAIL_CHANGED)) }
  }

  @Test
  fun `when an invalid message is received it should be sent to the dead letter queue`() {
    val rawMessage = "Invalid JSON message"

    assertThrows<JsonParseException> { hmppsDomainEventsListener.onDomainEvent(rawMessage) }

    verify { hmppsDomainEventService wasNot Called }
  }

  @Test
  fun `when an unexpected event type is received it should be sent to the dead letter queue`() {
    val unexpectedEventType = "unexpected.event.type"
    val rawMessage = SqsNotificationGeneratingHelper(timestamp = currentTime).generateRawHmppsDomainEvent(eventType = unexpectedEventType)

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify { hmppsDomainEventService wasNot Called }
    verify { deadLetterQueueService wasNot Called }
  }

  @Test
  fun `will not process and save a domain registration event message of none MAPP type`() {
    val rawMessage = SqsNotificationGeneratingHelper(timestamp = currentTime).generateRawHmppsDomainEvent(registerTypeCode = "NOTMAPP")

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify { hmppsDomainEventService wasNot Called }
    verify { deadLetterQueueService wasNot Called }
  }

  @Test
  fun `when alert event matches multiple filters using generator, both services should be called`() {
    val rawMessage = SqsNotificationGeneratingHelper(timestamp = currentTime)
      .generateRawHmppsDomainEventWithAlertCode(
        eventType = "person.alert.created",
        alertCode = "HA",
      )

    val hmppsDomainEvent = SqsNotificationGeneratingHelper(currentTime)
      .createHmppsDomainEventWithAlertCode(
        eventType = "person.alert.created",
        alertCode = "HA",
      )

    every { hmppsDomainEventService.execute(hmppsDomainEvent, any()) } just runs

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(exactly = 1) {
      hmppsDomainEventService.execute(
        hmppsDomainEvent,
        match {
          it.containsAll(
            listOf(
              IntegrationEventType.PERSON_PND_ALERTS_CHANGED,
              IntegrationEventType.PERSON_ALERTS_CHANGED,
            ),
          )
        },
      )
    }
  }
}
