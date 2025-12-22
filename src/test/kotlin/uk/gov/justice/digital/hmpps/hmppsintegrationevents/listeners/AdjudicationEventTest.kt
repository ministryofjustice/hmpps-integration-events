package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEventName
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType

class AdjudicationEventTest : HmppsDomainEventsListenerEventTestCase() {
  private val nomsNumber = "A1234BC"

  @ParameterizedTest
  @ValueSource(
    strings = [
      HmppsDomainEventName.Adjudication.Hearing.CREATED,
      HmppsDomainEventName.Adjudication.Hearing.DELETED,
      HmppsDomainEventName.Adjudication.Hearing.COMPLETED,
      HmppsDomainEventName.Adjudication.Punishments.CREATED,
      HmppsDomainEventName.Adjudication.Report.CREATED,
    ],
  )
  fun `will process an adjudication notification`(eventType: String) {
    // Arrange
    val message =
      """
      {
        "eventType": "$eventType",
        "version": "1.0",
        "description": "An adjudication has been created:  MDI-000169",
        "occurredAt": "2024-08-14T12:33:34+01:00",
        "additionalInformation": {
          "prisonerNumber": "$nomsNumber"
        }
      }
      """.trimIndent().replace("\n", "")

    val payload = DomainEvents.generateDomainEvent(eventType, message.replace("\"", "\\\""))

    onDomainEventShouldCreateEventNotification(
      hmppsEventRawMessage = payload,
      hmppsId = nomsNumber,
      expectedNotificationType = IntegrationEventType.PERSON_REPORTED_ADJUDICATIONS_CHANGED,
    )
  }
}
