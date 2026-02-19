package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType

class HmppsDomainEventsListenerSANPlanCreationScheduleUpdatedTest : HmppsDomainEventsListenerTestCase() {
  private val nomsNumber = "A1234BC"

  private val eventType = "san.plan-creation-schedule.updated"

  @Test
  fun `will process a san induction schedule updated notification`() {
    // Arrange
    val message = """
      { \"eventType\": \"$eventType\",  \"description\": \"A Support for additional needs plan creation schedule created or amended\",  \"detailUrl\": \"http://localhost:8080/profile/$nomsNumber/plan-creation-schedule\",  \"occurredAt\": \"2024-08-08T09:07:55\",  \"personReference\": {    \"identifiers\": [      {        \"type\": \"NOMS\",        \"value\": \"$nomsNumber\"      }    ]  }}
    """.trimIndent()

    val payload = DomainEvents.generateDomainEvent(eventType, message)

    // Act, Assert
    onDomainEventShouldCreateEventNotification(
      hmppsEventRawMessage = payload,
      hmppsId = nomsNumber,
      expectedNotificationType = IntegrationEventType.SAN_PLAN_CREATION_SCHEDULE_CHANGED,
      nomisNumber = nomsNumber,
    )
  }
}
