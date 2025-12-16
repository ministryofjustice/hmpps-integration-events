package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType

/**
 * Tests of PLP (LWP) events.
 *
 * - `PLP`:  Personal Learning Plan
 * - known as `LWP`: Learning and Work Progress
 */
class HmppsDomainEventServicePLPTest {
  private val nomsNumber = "A1234BC"
  private val hmppsId = nomsNumber

  // From `uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.HmppsDomainEventsListenerPLPInductionUpdatedTest`
  @Nested
  inner class PLPInductionUpdatedTest : HmppsDomainEventServiceEventTestCase() {
    private val eventType = "plp.induction-schedule.updated"

    @Test
    fun `will process and save a plp induction schedule updated notification`() {
      val message = """
        { \"eventType\": \"$eventType\",  \"description\": \"A prisoner learning plan induction schedule created or amended\",  \"detailUrl\": \"http://localhost:8080/inductions/$nomsNumber/induction-schedule\",  \"occurredAt\": \"2024-08-08T09:07:55\",  \"personReference\": {    \"identifiers\": [      {        \"type\": \"NOMS\",        \"value\": \"$nomsNumber\"      }    ]  }}
      """.trimIndent().replace("\\", "")

      executeShouldSaveEventNotificationOfPerson(
        hmppsEventType = eventType,
        hmppsMessage = message,
        hmppsId = hmppsId,
        expectedNotificationType = IntegrationEventType.PLP_INDUCTION_SCHEDULE_CHANGED,
        expectedUrl = "$baseUrl/v1/persons/$hmppsId/plp-induction-schedule/history",
      )
    }
  }

  // From `uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.HmppsDomainEventsListenerPLPReviewUpdatedTest`
  @Nested
  inner class PLPReviewUpdatedTest : HmppsDomainEventServiceEventTestCase() {
    private val eventType = "plp.review-schedule.updated"

    @Test
    fun `will process and save a plp review schedule updated notification`() {
      val message = """
        { \"eventType\": \"$eventType\",  \"description\": \"A prisoner learning plan review schedule created or amended\",  \"detailUrl\": \"http://localhost:8080/inductions/$nomsNumber/review-schedule\",  \"occurredAt\": \"2024-08-08T09:07:55\",  \"personReference\": {    \"identifiers\": [      {        \"type\": \"NOMS\",        \"value\": \"$nomsNumber\"      }    ]  }}
      """.trimIndent().replace("\\", "")

      executeShouldSaveEventNotificationOfPerson(
        hmppsEventType = eventType,
        hmppsMessage = message,
        hmppsId = hmppsId,
        expectedNotificationType = IntegrationEventType.PLP_REVIEW_SCHEDULE_CHANGED,
        expectedUrl = "$baseUrl/v1/persons/$hmppsId/plp-review-schedule",
      )
    }
  }
}
