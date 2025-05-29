package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
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
class PrisonerSentencesChangedEventTest {
  private val hmppsDomainEventService = mockk<HmppsDomainEventService>()
  private val deadLetterQueueService = mockk<DeadLetterQueueService>()

  private val hmppsDomainEventsListener: HmppsDomainEventsListener = HmppsDomainEventsListener(hmppsDomainEventService, deadLetterQueueService)

  private val nomsNumber = "A1234BC"

  @Test
  fun `will process an prisoner sentence changed notification`() {
    val eventType = HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED
    val message =
      """
      {
        "eventType": "$eventType",
        "version": "1.0",
        "description": "This is when a prisoner index record has been updated.",
        "occurredAt": "2024-08-14T12:33:34+01:00",
        "additionalInformation": {
          "categoriesChanged": ["SENTENCE"]
        },
        "personReference": {
          "identifiers": [
            {
              "type": "NOMS", 
              "value": "$nomsNumber"
             }
          ]
        }
      }
      """.trimIndent().replace("\n", "")

    val payload = DomainEvents.generateDomainEvent(eventType, message.replace("\"", "\\\""))
    val hmppsDomainEvent = generateHmppsDomainEvent(eventType, message)

    every {
      hmppsDomainEventService.execute(
        hmppsDomainEvent,
        any(),
      )
    } just runs

    hmppsDomainEventsListener.onDomainEvent(payload)

    verify(exactly = 1) {
      hmppsDomainEventService.execute(
        hmppsDomainEvent,
        IntegrationEventType.PERSON_STATUS_CHANGED,
      )
    }
    verify(exactly = 1) {
      hmppsDomainEventService.execute(
        hmppsDomainEvent,
        IntegrationEventType.PERSON_SENTENCES_CHANGED,
      )
    }
  }
}
