package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
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
class PrisonerEventTest {
  private val hmppsDomainEventService = mockk<HmppsDomainEventService>()
  private val deadLetterQueueService = mockk<DeadLetterQueueService>()

  private val hmppsDomainEventsListener: HmppsDomainEventsListener = HmppsDomainEventsListener(hmppsDomainEventService, deadLetterQueueService)

  private val nomsNumber = "A1234BC"

  @ParameterizedTest
  @ValueSource(
    strings = [
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.CREATED,
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED,
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.RECEIVED,
    ],
  )
  fun `will process prisoner events`(eventType: String) {
    val message =
      """
      {
        "eventType": "$eventType",
        "version": "1.0",
        "description": "This is when a prisoner index record has been updated.",
        "occurredAt": "2024-08-14T12:33:34+01:00",
        "additionalInformation": {
          "categoriesChanged": []
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

    every { hmppsDomainEventService.execute(hmppsDomainEvent, any()) } just runs

    hmppsDomainEventsListener.onDomainEvent(payload)

    verify(exactly = 1) {
      hmppsDomainEventService.execute(
        hmppsDomainEvent,
        match {
          it.containsAll(
            listOf(
              IntegrationEventType.PERSON_STATUS_CHANGED,
              IntegrationEventType.PRISONER_CHANGED,
              IntegrationEventType.PRISONERS_CHANGED,
            ),
          )
        },
      )
    }
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.CREATED,
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.RECEIVED,
    ],
  )
  fun `will process new prisoner events`(eventType: String) {
    val message =
      """
      {
        "eventType": "$eventType",
        "version": "1.0",
        "description": "This is when a prisoner index record has been updated.",
        "occurredAt": "2024-08-14T12:33:34+01:00",
        "additionalInformation": {
          "categoriesChanged": []
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

    every { hmppsDomainEventService.execute(hmppsDomainEvent, any()) } just runs

    hmppsDomainEventsListener.onDomainEvent(payload)

    verify(exactly = 1) {
      hmppsDomainEventService.execute(
        hmppsDomainEvent,
        match {
          it.containsAll(
            listOf(
              IntegrationEventType.PERSON_STATUS_CHANGED,
              IntegrationEventType.PRISONER_CHANGED,
              IntegrationEventType.PRISONERS_CHANGED,
              IntegrationEventType.PRISONER_NON_ASSOCIATIONS_CHANGED,
            ),
          )
        },
      )
    }
  }

  @Test
  fun `will process an prisoner personal details changed event`() {
    val eventType = HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED
    val message =
      """
      {
        "eventType": "$eventType",
        "version": "1.0",
        "description": "This is when a prisoner index record has been updated.",
        "occurredAt": "2024-08-14T12:33:34+01:00",
        "additionalInformation": {
          "categoriesChanged": ["PERSONAL_DETAILS"]
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

    every { hmppsDomainEventService.execute(hmppsDomainEvent, any()) } just runs

    hmppsDomainEventsListener.onDomainEvent(payload)

    verify(exactly = 1) {
      hmppsDomainEventService.execute(
        hmppsDomainEvent,
        match {
          it.containsAll(
            listOf(
              IntegrationEventType.PERSON_STATUS_CHANGED,
              IntegrationEventType.PERSON_NAME_CHANGED,
            ),
          )
        },
      )
    }
  }

  @Test
  fun `will process an prisoner sentence changed event`() {
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

    every { hmppsDomainEventService.execute(hmppsDomainEvent, any()) } just runs

    hmppsDomainEventsListener.onDomainEvent(payload)

    verify(exactly = 1) {
      hmppsDomainEventService.execute(
        hmppsDomainEvent,
        match {
          it.containsAll(
            listOf(
              IntegrationEventType.PERSON_STATUS_CHANGED,
              IntegrationEventType.PERSON_SENTENCES_CHANGED,
            ),
          )
        },
      )
    }
  }

  @Test
  fun `will process an prisoner location changed notification`() {
    val eventType = HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED
    val message =
      """
      {
        "eventType": "$eventType",
        "version": "1.0",
        "description": "This is when a prisoner index record has been updated.",
        "occurredAt": "2024-08-14T12:33:34+01:00",
        "additionalInformation": {
          "categoriesChanged": ["LOCATION"]
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

    every { hmppsDomainEventService.execute(hmppsDomainEvent, any()) } just runs

    hmppsDomainEventsListener.onDomainEvent(payload)

    verify(exactly = 1) {
      hmppsDomainEventService.execute(
        hmppsDomainEvent,
        match {
          it.containsAll(
            listOf(
              IntegrationEventType.PERSON_STATUS_CHANGED,
              IntegrationEventType.PERSON_CELL_LOCATION_CHANGED,
            ),
          )
        },
      )
    }
  }

  @Test
  fun `will process an prisoner physical details changed notification`() {
    val eventType = HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED
    val message =
      """
      {
        "eventType": "$eventType",
        "version": "1.0",
        "description": "This is when a prisoner index record has been updated.",
        "occurredAt": "2024-08-14T12:33:34+01:00",
        "additionalInformation": {
          "categoriesChanged": ["PHYSICAL_DETAILS"]
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

    every { hmppsDomainEventService.execute(hmppsDomainEvent, any()) } just runs

    hmppsDomainEventsListener.onDomainEvent(payload)

    verify(exactly = 1) {
      hmppsDomainEventService.execute(
        hmppsDomainEvent,
        match {
          it.containsAll(
            listOf(
              IntegrationEventType.PERSON_STATUS_CHANGED,
              IntegrationEventType.PERSON_PHYSICAL_CHARACTERISTICS_CHANGED,
            ),
          )
        },
      )
    }
  }
}
