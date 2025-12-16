package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.crn
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType

// From `uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.AddressChangedEventTest`
class HmppsDomainEventServiceAddressChangedEventTest : HmppsDomainEventServiceEventTestCase() {
  private val hmppsId = crn

  @ParameterizedTest
  @ValueSource(
    strings = [
      "probation-case.address.created",
      "probation-case.address.updated",
      "probation-case.address.deleted",
    ],
  )
  fun `will process and save an address changed notification`(eventType: String) {
    val message = """
      {"eventType":"$eventType","version": 1,"description":"HMPPS Domain Event","detailUrl":"https://some-url.gov.uk","occurredAt": "2024-08-14T12:33:34+01:00","personReference":{"identifiers":[{"type": "CRN", "value": "$crn"}]}}
    """.trimIndent().replace("\\", "")

    executeShouldSaveEventNotificationOfPerson(
      hmppsEventType = eventType,
      hmppsMessage = message,
      hmppsId = hmppsId,
      expectedNotificationType = IntegrationEventType.PERSON_ADDRESS_CHANGED,
      expectedUrl = "$baseUrl/v1/persons/$hmppsId/addresses",
    )
  }
}
