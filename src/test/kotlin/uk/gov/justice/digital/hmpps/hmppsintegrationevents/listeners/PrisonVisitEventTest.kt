package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEventName
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType

class PrisonVisitEventTest : HmppsDomainEventsListenerEventTestCase() {
  private val nomsNumber = "A1234BC"

  @BeforeEach
  internal fun setupVisitTest() {
    assumeIdentities(hmppsId = nomsNumber, prisonId = "MDI")
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      HmppsDomainEventName.PrisonVisit.BOOKED,
      HmppsDomainEventName.PrisonVisit.CHANGED,
      HmppsDomainEventName.PrisonVisit.CANCELLED,
    ],
  )
  fun `will process an visit changed notification`(eventType: String) {
    // Arrange
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

    // Act, Assert
    onDomainEventShouldCreateEventNotifications(
      hmppsEventRawMessage = payload,
      IntegrationEventType.PERSON_FUTURE_VISITS_CHANGED,
      IntegrationEventType.PRISON_VISITS_CHANGED,
      IntegrationEventType.VISIT_CHANGED,
    )
  }
}
