package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEventName
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType

class PrisonerVisitorRestrictionEventTest : HmppsDomainEventsListenerEventTestCase() {
  private val nomsNumber = "A1234BC"
  private val hmppsId = nomsNumber

  @ParameterizedTest
  @ValueSource(
    strings = [
      HmppsDomainEventName.PrisonOffenderEvents.Prisoner.PersonRestriction.UPSERTED,
      HmppsDomainEventName.PrisonOffenderEvents.Prisoner.PersonRestriction.DELETED,
    ],
  )
  fun `will process an prison visitor restriction notification`(eventType: String) {
    // Arrange
    val message =
      """
      {
        "eventType": "$eventType",
        "version": "1.0",
        "description": "This event is raised when a global visitor restriction is created or updated.",
        "occurredAt": "2024-08-14T12:33:34+01:00",
        "additionalInformation": {
          "contactPersonId": "7551236"
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

    // Act, Assert
    onDomainEventShouldCreateEventNotification(
      hmppsEventRawMessage = payload,
      hmppsId = hmppsId,
      expectedNotificationType = IntegrationEventType.PERSON_VISITOR_RESTRICTIONS_CHANGED,
    )
  }
}
