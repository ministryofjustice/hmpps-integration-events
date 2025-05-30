package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.generateHmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEventName
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DeadLetterQueueService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.HmppsDomainEventService

@ActiveProfiles("test")
@JsonTest
class PrisonVisitEventTest {
  private val hmppsDomainEventService = mockk<HmppsDomainEventService>()
  private val deadLetterQueueService = mockk<DeadLetterQueueService>()

  private val hmppsDomainEventsListener: HmppsDomainEventsListener = HmppsDomainEventsListener(hmppsDomainEventService, deadLetterQueueService)

  private val nomsNumber = "A1234BC"

  @ParameterizedTest
  @ValueSource(
    strings = [
      HmppsDomainEventName.PrisonVisit.BOOKED,
      HmppsDomainEventName.PrisonVisit.CHANGED,
      HmppsDomainEventName.PrisonVisit.CANCELLED,
    ],
  )
  fun `will process an visit changed notification`(eventType: String) {
    val message =
      """
      {
        "eventType": "$eventType",
        "version": "1.0",
        "description": "Prison visit changed",
        "occurredAt": "2024-08-14T12:33:34+01:00",
        "prisonerId": "$nomsNumber",
        "additionalInformation": {
          "reference": "nx-ce-vq-ry"
        }
      }
      """.trimIndent().replace("\n", "")

    val payload = DomainEvents.generateDomainEvent(eventType, message.replace("\"", "\\\""))
    val hmppsDomainEvent = generateHmppsDomainEvent(eventType, message)

      every { hmppsDomainEventService.execute(hmppsDomainEvent, any()) } just runs

      hmppsDomainEventsListener.onDomainEvent(payload)

      verify(exactly = 1) {
          hmppsDomainEventService.execute(
              hmppsDomainEvent,
              match {
                  it.containsAll(
                      listOf(
                          IntegrationEventType.PERSON_FUTURE_VISITS_CHANGED,
                          IntegrationEventType.PRISON_VISITS_CHANGED,
                          IntegrationEventType.VISIT_CHANGED
                      )
                  )
              }
          )
      }
  }
}
