package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.ASSESSMENT_SUMMARY_PRODUCED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType

@Configuration
@ActiveProfiles("test")
class HmppsDomainEventServiceROSHTest : HmppsDomainEventServiceEventTestCase() {
  private val hmppsId = "AA1234A"

  @BeforeEach
  fun setupTest() {
    stubDomainEventIdentitiesResolver(hmppsId, null)
  }

  @Test
  fun `will process and save a rosh notification`() {
    val eventType = "assessment.summary.produced"
    val message = ASSESSMENT_SUMMARY_PRODUCED.replace("\\", "")

    executeShouldSaveEventNotificationOfPerson(
      hmppsEventType = eventType,
      hmppsMessage = message,
      hmppsId = hmppsId,
      expectedNotificationType = IntegrationEventType.RISK_OF_SERIOUS_HARM_CHANGED,
      expectedUrl = "$baseUrl/v1/persons/$hmppsId/risks/serious-harm",
    )
  }
}
