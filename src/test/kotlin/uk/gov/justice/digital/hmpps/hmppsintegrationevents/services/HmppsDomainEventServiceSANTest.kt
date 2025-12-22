package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType

/**
 * Tests of SAN events
 *
 * - `SAN`: Support for additional needs
 */
class HmppsDomainEventServiceSANTest {
  private val nomsNumber = "A1234BC"
  private val hmppsId = nomsNumber

  // From `uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.HmppsDomainEventsListenerSANPlanCreationScheduleUpdatedTest
  @Nested
  inner class HmppsDomainEventsListenerSANPlanCreationScheduleUpdatedTest : HmppsDomainEventServiceEventTestCase() {
    private val eventType = "san.plan-creation-schedule.updated"

    @Test
    fun `will process and save a san induction schedule updated notification`() {
      val message = """
        { \"eventType\": \"$eventType\",  \"description\": \"A Support for additional needs plan creation schedule created or amended\",  \"detailUrl\": \"http://localhost:8080/profile/$nomsNumber/plan-creation-schedule\",  \"occurredAt\": \"2024-08-08T09:07:55\",  \"personReference\": {    \"identifiers\": [      {        \"type\": \"NOMS\",        \"value\": \"$nomsNumber\"      }    ]  }}
      """.trimIndent().replace("\\", "")

      executeShouldSaveEventNotificationOfPerson(
        hmppsEventType = eventType,
        hmppsMessage = message,
        hmppsId = hmppsId,
        expectedNotificationType = IntegrationEventType.SAN_PLAN_CREATION_SCHEDULE_CHANGED,
        expectedUrl = "$baseUrl/v1/persons/$hmppsId/education/san/plan-creation-schedule",
      )
    }
  }

  // From `uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.HmppsDomainEventsListenerHmppsDomainEventsListenerSANReviewUpdatedTest`
  @Nested
  inner class SANReviewUpdatedTest : HmppsDomainEventServiceEventTestCase() {
    private val eventType = "san.review-schedule.updated"

    @Test
    fun `will process and save a san review schedule updated notification`() {
      val message = """
        { \"eventType\": \"$eventType\",  \"description\": \"A Support for additional needs review schedule was created or amended\",  \"detailUrl\": \"http://localhost:8080/profile/$nomsNumber/reviews/review-schedules\",  \"occurredAt\": \"2024-08-08T09:07:55\",  \"personReference\": {    \"identifiers\": [      {        \"type\": \"NOMS\",        \"value\": \"$nomsNumber\"      }    ]  }}
      """.trimIndent().replace("\\", "")

      executeShouldSaveEventNotificationOfPerson(
        hmppsEventType = eventType,
        hmppsMessage = message,
        hmppsId = hmppsId,
        expectedNotificationType = IntegrationEventType.SAN_REVIEW_SCHEDULE_CHANGED,
        expectedUrl = "$baseUrl/v1/persons/$hmppsId/education/san/review-schedule",
      )
    }
  }
}
