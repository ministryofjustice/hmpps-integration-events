package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.ASSESSMENT_SUMMARY_PRODUCED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType

class HmppsDomainEventsListenerROSHTest : HmppsDomainEventsListenerTestCase() {
  private val crn = "X777776"

  @Test
  fun `will process and save a rosh notification`() {
    // Arrange
    val eventType = "assessment.summary.produced"
    val message = ASSESSMENT_SUMMARY_PRODUCED

    val payload = DomainEvents.generateDomainEvent(eventType, message)

    // Act, Assert
    onDomainEventShouldCreateEventNotification(
      hmppsEventRawMessage = payload,
      hmppsId = crn,
      expectedNotificationType = IntegrationEventType.RISK_OF_SERIOUS_HARM_CHANGED,
    )
  }
}
