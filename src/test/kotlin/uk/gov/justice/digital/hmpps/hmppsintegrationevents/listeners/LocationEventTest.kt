package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEventName
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType

class LocationEventTest : HmppsDomainEventsListenerEventTestCase() {
  private val locationKey = "MDI-001-01"
  private val prisonId = "MDI"

  @BeforeEach
  internal fun setupLocationTest() {
    assumeIdentities(prisonId = prisonId)
  }

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
    // Arrange
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

    // Act, Assert
    onDomainEventShouldCreateEventNotifications(
      hmppsEventRawMessage = payload,
      IntegrationEventType.PRISON_LOCATION_CHANGED,
      IntegrationEventType.PRISON_RESIDENTIAL_HIERARCHY_CHANGED,
      IntegrationEventType.PRISON_RESIDENTIAL_DETAILS_CHANGED,
    )
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

    // Act, Assert
    onDomainEventShouldCreateEventNotification(
      hmppsEventRawMessage = payload,
      expectedNotificationType = IntegrationEventType.PRISON_CAPACITY_CHANGED,
    )
  }
}
