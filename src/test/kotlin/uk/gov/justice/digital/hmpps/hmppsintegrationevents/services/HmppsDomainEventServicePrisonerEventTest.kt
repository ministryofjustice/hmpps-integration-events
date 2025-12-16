package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEventName
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType

// From `uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.PrisonerEventTest`
class HmppsDomainEventServicePrisonerEventTest : HmppsDomainEventServiceEventTestCase() {
  private val nomsNumber = "A1234BC"
  private val prisonId = "MDI"
  private val hmppsId = nomsNumber

  @ParameterizedTest
  @ValueSource(
    strings = [
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.CREATED,
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED,
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.RECEIVED,
    ],
  )
  fun `will process and save prisoner events`(eventType: String) = executeShouldSavePrisonerChangedEvents(
    expectedNotificationTypeAndUrls = mapOf(
      IntegrationEventType.PERSON_STATUS_CHANGED to "$baseUrl/v1/persons/$hmppsId",
      IntegrationEventType.PRISONER_CHANGED to "$baseUrl/v1/prison/prisoners/$hmppsId",
      IntegrationEventType.PRISONERS_CHANGED to "$baseUrl/v1/prison/prisoners",
    ),
    eventType = eventType,
  )

  @ParameterizedTest
  @ValueSource(
    strings = [
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.CREATED,
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.RECEIVED,
    ],
  )
  fun `will process and save new prisoner events`(eventType: String) = executeShouldSavePrisonerChangedEvents(
    expectedNotificationTypeAndUrls = mapOf(
      IntegrationEventType.PERSON_STATUS_CHANGED to "$baseUrl/v1/persons/$hmppsId",
      IntegrationEventType.PRISONER_CHANGED to "$baseUrl/v1/prison/prisoners/$hmppsId",
      IntegrationEventType.PRISONERS_CHANGED to "$baseUrl/v1/prison/prisoners",
      IntegrationEventType.PRISONER_NON_ASSOCIATIONS_CHANGED to "$baseUrl/v1/prison/$prisonId/prisoners/$hmppsId/non-associations",
    ),
    eventType = eventType,
  )

  @Test
  fun `will process and save a prisoner personal details changed event`() = executeShouldSavePrisonerChangedEvents(
    expectedNotificationTypeAndUrls = mapOf(
      IntegrationEventType.PERSON_STATUS_CHANGED to "$baseUrl/v1/persons/$hmppsId",
      IntegrationEventType.PERSON_NAME_CHANGED to "$baseUrl/v1/persons/$hmppsId/name",
    ),
    eventType = HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED,
    "PERSONAL_DETAILS",
  )

  @Test
  fun `will process and save a prisoner sentence changed event`() = executeShouldSavePrisonerChangedEvents(
    expectedNotificationTypeAndUrls = mapOf(
      IntegrationEventType.PERSON_STATUS_CHANGED to "$baseUrl/v1/persons/$hmppsId",
      IntegrationEventType.PERSON_SENTENCES_CHANGED to "$baseUrl/v1/persons/$hmppsId/sentences",
    ),
    eventType = HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED,
    "SENTENCE",
  )

  @Test
  fun `will process an prisoner location changed notification`() = executeShouldSavePrisonerChangedEvents(
    expectedNotificationTypeAndUrls = mapOf(
      IntegrationEventType.PERSON_STATUS_CHANGED to "$baseUrl/v1/persons/$hmppsId",
      IntegrationEventType.PERSON_CELL_LOCATION_CHANGED to "$baseUrl/v1/persons/$hmppsId/cell-location",
    ),
    eventType = HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED,
    "LOCATION",
  )

  @Test
  fun `will process an prisoner physical details changed notification`() = executeShouldSavePrisonerChangedEvents(
    expectedNotificationTypeAndUrls = mapOf(
      IntegrationEventType.PERSON_STATUS_CHANGED to "$baseUrl/v1/persons/$hmppsId",
      IntegrationEventType.PERSON_PHYSICAL_CHARACTERISTICS_CHANGED to "$baseUrl/v1/persons/$hmppsId/physical-characteristics",
    ),
    eventType = HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED,
    "PHYSICAL_DETAILS",
  )

  private fun executeShouldSavePrisonerChangedEvents(
    expectedNotificationTypeAndUrls: Map<IntegrationEventType, String>,
    eventType: String,
    vararg categoriesChanged: String,
  ) = executeShouldSaveMultipleEventNotificationsOfPersonInPrison(
    hmppsEventType = eventType,
    hmppsMessage = generatePrisonerEventHmppsMessage(eventType, *categoriesChanged),
    hmppsId = hmppsId,
    prisonId = prisonId,
    expectedNotificationTypeAndUrls = expectedNotificationTypeAndUrls,
  )

  private fun generatePrisonerEventHmppsMessage(eventType: String, vararg categoriesChanged: String) = """
    {
      "eventType": "$eventType",
      "version": "1.0",
      "description": "This is when a prisoner index record has been updated.",
      "occurredAt": "2024-08-14T12:33:34+01:00",
      "additionalInformation": {
        "categoriesChanged": [${categoriesChanged.joinToString(",") { "\"$it\"" }}]
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
}
