package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.crn
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.generateHmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DeadLetterQueueService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.HmppsDomainEventService

class AddressChangedEventTest {
  private val hmppsDomainEventService = mockk<HmppsDomainEventService>()
  private val deadLetterQueueService = mockk<DeadLetterQueueService>()

  private val hmppsDomainEventsListener: HmppsDomainEventsListener =
    HmppsDomainEventsListener(hmppsDomainEventService, deadLetterQueueService)

  @ParameterizedTest
  @ValueSource(
    strings = [
      "probation-case.address.created",
      "probation-case.address.updated",
      "probation-case.address.deleted",
    ],
  )
  fun `will process an address changed notification`(eventType: String) {
    val message =
      """{"eventType":"$eventType","version": 1,"description":"HMPPS Domain Event","detailUrl":"https://some-url.gov.uk","occurredAt": "2024-08-14T12:33:34+01:00","personReference":{"identifiers":[{"type": "CRN", "value": "$crn"}]}}
      """.trimIndent()

    val payload = DomainEvents.generateDomainEvent(eventType, message.replace("\"", "\\\""))
    val hmppsDomainEvent = generateHmppsDomainEvent(eventType, message)

    every {
      hmppsDomainEventService.execute(
        hmppsDomainEvent,
        IntegrationEventType.PERSON_ADDRESS_CHANGED,
      )
    } just runs

    hmppsDomainEventsListener.onDomainEvent(payload)

    verify(exactly = 1) {
      hmppsDomainEventService.execute(
        hmppsDomainEvent,
        IntegrationEventType.PERSON_ADDRESS_CHANGED,
      )
    }
  }
}
