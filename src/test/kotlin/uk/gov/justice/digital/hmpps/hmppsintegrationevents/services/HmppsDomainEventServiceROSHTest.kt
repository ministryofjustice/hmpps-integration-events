package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.ASSESSMENT_SUMMARY_PRODUCED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.generateHmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType

class HmppsDomainEventServiceROSHTest : HmppsDomainEventServiceTestCase() {
  private val hmppsId = "AA1234A"

  @BeforeEach
  fun setup() {
    assumeIdentities(hmppsId = hmppsId, prisonId = null)
  }

  @Test
  fun `will process and save a rosh notification`() {
    // Arrange
    val message = ASSESSMENT_SUMMARY_PRODUCED

    val hmppsMessage = message.replace("\\", "")
    val event = generateHmppsDomainEvent("assessment.summary.produced", hmppsMessage)

    // Act, Assert
    executeShouldSaveEventNotification(
      hmppsDomainEvent = event,
      integrationEventType = IntegrationEventType.RISK_OF_SERIOUS_HARM_CHANGED,
      url = "$baseUrl/v1/persons/$hmppsId/risks/serious-harm",
      hmppsId = hmppsId,
    )
  }
}
