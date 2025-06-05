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
class LocationEventTest {
  private val hmppsDomainEventService = mockk<HmppsDomainEventService>()
  private val deadLetterQueueService = mockk<DeadLetterQueueService>()

  private val hmppsDomainEventsListener: HmppsDomainEventsListener = HmppsDomainEventsListener(hmppsDomainEventService, deadLetterQueueService)

  private val locationKey = "MDI-001-01"

  @ParameterizedTest
  @ValueSource(
    strings = [
      HmppsDomainEventName.LocationsInsidePrison.Location.CREATED,
      HmppsDomainEventName.LocationsInsidePrison.Location.AMENDED,
      HmppsDomainEventName.LocationsInsidePrison.Location.DELETED,
      HmppsDomainEventName.LocationsInsidePrison.Location.DEACTIVATED,
      HmppsDomainEventName.LocationsInsidePrison.Location.REACTIVATED,
    ],
  )
  fun `will process an location event`(eventType: String) {
    val message =
      """
      {
        "eventType": "$eventType",
        "version": "1.0",
        "description": "Locations – a location inside prison has been amended",
        "occurredAt": "2024-08-14T12:33:34+01:00",
        "additionalInformation": {
          "key": "$locationKey"
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
              IntegrationEventType.PRISON_LOCATION_CHANGED,
              IntegrationEventType.PRISON_RESIDENTIAL_HIERARCHY_CHANGED,
              IntegrationEventType.PRISON_RESIDENTIAL_DETAILS_CHANGED,
            ),
          )
        },
      )
    }
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      HmppsDomainEventName.LocationsInsidePrison.Location.CREATED,
      HmppsDomainEventName.LocationsInsidePrison.Location.DELETED,
      HmppsDomainEventName.LocationsInsidePrison.Location.DEACTIVATED,
      HmppsDomainEventName.LocationsInsidePrison.Location.REACTIVATED,
      HmppsDomainEventName.LocationsInsidePrison.SignedOpCapacity.AMENDED,
    ],
  )
  fun `will process an prison capacity event`(eventType: String) {
    val message =
      """
      {
        "eventType": "$eventType",
        "version": "1.0",
        "description": "Locations – a location inside prison has been amended",
        "occurredAt": "2024-08-14T12:33:34+01:00",
        "additionalInformation": {
          "key": "$locationKey"
        }
      }
      """.trimIndent().replace("\n", "")

    val payload = DomainEvents.generateDomainEvent(eventType, message.replace("\"", "\\\""))
    val hmppsDomainEvent = generateHmppsDomainEvent(eventType, message)

    every { hmppsDomainEventService.execute(hmppsDomainEvent, any()) } just runs

    hmppsDomainEventsListener.onDomainEvent(payload)

    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent, listOf(IntegrationEventType.PRISON_CAPACITY_CHANGED)) }
  }
}
