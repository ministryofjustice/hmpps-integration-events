package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.generateHmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType

class HmppsDomainEventServiceLicenceConditionTest : HmppsDomainEventServiceTestCase() {
  private val hmppsId = "AA1234A"

  @BeforeEach
  fun setup() {
    assumeIdentities(hmppsId = hmppsId, prisonId = null)
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "create-and-vary-a-licence.licence.activated, 99059, Licence activated for Licence ID 99059",
      "create-and-vary-a-licence.licence.inactivated, 90386, Licence inactivated for Licence ID 90386",
    ],
  )
  fun `will process and save a licence notification`(eventType: String, licenceId: String, description: String) {
    // Arrange
    val message = """
      {\"eventType\":\"$eventType\",\"additionalInformation\":{\"licenceId\":\"$licenceId\"},\"detailUrl\":\"https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk/public/licences/id/$licenceId\",\"version\":1,\"occurredAt\":\"2024-08-14T16:42:13.725721689+01:00\",\"description\":\"$description\",\"personReference\":{\"identifiers\":[{\"type\":\"CRN\",\"value\":\"crn\"},{\"type\":\"NOMS\",\"value\":\"nomsNumber\"}]}}
    """.trimIndent()

    val hmppsMessage = message.replace("\\", "")
    val event = generateHmppsDomainEvent(eventType, hmppsMessage).domainEvent()

    // Act, Assert
    executeShouldSaveEventNotification(
      hmppsDomainEvent = event,
      integrationEventType = IntegrationEventType.LICENCE_CONDITION_CHANGED,
      url = "$baseUrl/v1/persons/$hmppsId/licences/conditions",
      hmppsId = hmppsId,
    )
  }
}
