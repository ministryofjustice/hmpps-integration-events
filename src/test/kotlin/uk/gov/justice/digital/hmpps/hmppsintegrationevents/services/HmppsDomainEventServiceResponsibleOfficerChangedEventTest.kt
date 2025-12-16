package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.crn
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType

// From `uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.ResponsibleOfficerChangedEventTest`
class HmppsDomainEventServiceResponsibleOfficerChangedEventTest : HmppsDomainEventServiceEventTestCase() {
  private val hmppsId = crn

  @ParameterizedTest
  @ValueSource(
    strings = [
      "person.community.manager.allocated",
      "person.community.manager.transferred",
      "probation.staff.updated",
    ],
  )
  fun `will process and save a responsible officer changed notification`(eventType: String) {
    val message = """
      {"eventType":"$eventType","version": 1,"description":"HMPPS Domain Event","detailUrl":"https://some-url.gov.uk","occurredAt": "2024-08-14T12:33:34+01:00","personReference":{"identifiers":[{"type": "CRN", "value": "$crn"}]}}
    """.trimIndent()

    executeShouldSaveEventNotificationOfPerson(
      hmppsEventType = eventType,
      hmppsMessage = message,
      hmppsId = hmppsId,
      expectedNotificationType = IntegrationEventType.PERSON_RESPONSIBLE_OFFICER_CHANGED,
      expectedUrl = "$baseUrl/v1/persons/$hmppsId/person-responsible-officer",
    )
  }
}
