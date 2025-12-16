package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType

class HmppsDomainEventServiceLicenceConditionTest : HmppsDomainEventServiceEventTestCase() {
  private val hmppsId = "AA1234A"

  @ParameterizedTest
  @CsvSource(
    value = [
      "create-and-vary-a-licence.licence.activated, 99059",
      "create-and-vary-a-licence.licence.inactivated, 90386",
    ],
  )
  fun `will process and save a licence notification`(eventType: String, licenceId: String) {
    val message = """
      {\"eventType\":\"$eventType\",\"additionalInformation\":{\"licenceId\":\"$licenceId\"},\"detailUrl\":\"https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk/public/licences/id/$licenceId\",\"version\":1,\"occurredAt\":\"2024-08-14T16:42:13.725721689+01:00\",\"description\":\"Licence activated for Licence ID $licenceId\",\"personReference\":{\"identifiers\":[{\"type\":\"CRN\",\"value\":\"crn\"},{\"type\":\"NOMS\",\"value\":\"nomsNumber\"}]}}
    """.trimIndent().replace("\\", "")

    executeShouldSaveEventNotificationOfPerson(
      hmppsEventType = eventType,
      hmppsMessage = message,
      hmppsId = hmppsId,
      expectedNotificationType = IntegrationEventType.LICENCE_CONDITION_CHANGED,
      expectedUrl = "$baseUrl/v1/persons/$hmppsId/licences/conditions",
    )
  }
}
