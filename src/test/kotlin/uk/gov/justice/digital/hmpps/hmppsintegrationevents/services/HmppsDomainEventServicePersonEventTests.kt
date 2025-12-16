package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.mockk.verify
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PRISONER_OFFENDER_SEARCH_PRISONER_CREATED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PRISONER_OFFENDER_SEARCH_PRISONER_RECEIVED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PRISONER_OFFENDER_SEARCH_PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PROBATION_CASE_ENGAGEMENT_CREATED_MESSAGE
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PROBATION_CASE_PRISON_IDENTIFIER_ADDED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.crn
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.nomsNumber
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEventName
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import java.util.stream.Stream

// From `uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.PersonEventTest`
class HmppsDomainEventServicePersonEventTest : HmppsDomainEventServiceEventTestCase() {
  private companion object {
    private val newPersonEventToMessageMap = mapOf(
      HmppsDomainEventName.ProbabtionCase.Engagement.CREATED to PROBATION_CASE_ENGAGEMENT_CREATED_MESSAGE,
      HmppsDomainEventName.ProbabtionCase.PrisonIdentifier.ADDED to PROBATION_CASE_PRISON_IDENTIFIER_ADDED,
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.CREATED to PRISONER_OFFENDER_SEARCH_PRISONER_CREATED,
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.RECEIVED to PRISONER_OFFENDER_SEARCH_PRISONER_RECEIVED,
    )
    private val allPersonEventToMessageMap = newPersonEventToMessageMap + mapOf(
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED to PRISONER_OFFENDER_SEARCH_PRISONER_UPDATED,
    )
  }

  @ParameterizedTest
  @ArgumentsSource(AllPersonEventsArgumentSource::class)
  fun `process and save person events`(eventType: String) {
    val hmppsMessage = requireNotNull(allPersonEventToMessageMap[eventType])
      .replace("\\", "")
    val hmppsId = getHmppsIdForTesting(eventType)

    executeShouldSaveEventNotificationOfPerson(
      hmppsEventType = eventType,
      hmppsMessage = hmppsMessage,
      hmppsId = hmppsId,
      expectedNotificationType = IntegrationEventType.PERSON_STATUS_CHANGED,
      expectedUrl = "$baseUrl/v1/persons/$hmppsId",
    )
  }

  @ParameterizedTest
  @ArgumentsSource(NewPersonEventsArgumentSource::class)
  fun `process and save new person events`(eventType: String) {
    val hmppsMessage = requireNotNull(newPersonEventToMessageMap[eventType])
      .replace("\\", "")
    val hmppsId = getHmppsIdForTesting(eventType)

    executeShouldSaveMultipleEventNotificationsOfPerson(
      hmppsEventType = eventType,
      hmppsMessage = hmppsMessage,
      hmppsId = hmppsId,
      expectedNotificationTypeAndUrls = mapOf(
        IntegrationEventType.PERSON_STATUS_CHANGED to "$baseUrl/v1/persons/$hmppsId",
        IntegrationEventType.PERSON_CASE_NOTES_CHANGED to "$baseUrl/v1/persons/$hmppsId/case-notes",
        IntegrationEventType.PERSON_NAME_CHANGED to "$baseUrl/v1/persons/$hmppsId/name",
        IntegrationEventType.PERSON_SENTENCES_CHANGED to "$baseUrl/v1/persons/$hmppsId/sentences",
        IntegrationEventType.PERSON_PROTECTED_CHARACTERISTICS_CHANGED to "$baseUrl/v1/persons/$hmppsId/protected-characteristics",
        IntegrationEventType.PERSON_REPORTED_ADJUDICATIONS_CHANGED to "$baseUrl/v1/persons/$hmppsId/reported-adjudications",
        IntegrationEventType.PERSON_NUMBER_OF_CHILDREN_CHANGED to "$baseUrl/v1/persons/$hmppsId/number-of-children",
        IntegrationEventType.PERSON_PHYSICAL_CHARACTERISTICS_CHANGED to "$baseUrl/v1/persons/$hmppsId/physical-characteristics",
        IntegrationEventType.PERSON_IMAGES_CHANGED to "$baseUrl/v1/persons/$hmppsId/images",
        IntegrationEventType.PERSON_HEALTH_AND_DIET_CHANGED to "$baseUrl/v1/persons/$hmppsId/health-and-diet",
        IntegrationEventType.PERSON_CARE_NEEDS_CHANGED to "$baseUrl/v1/persons/$hmppsId/care-needs",
        IntegrationEventType.PERSON_LANGUAGES_CHANGED to "$baseUrl/v1/persons/$hmppsId/languages",
        IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE to "$baseUrl/v1/persons/$hmppsId/sentences/latest-key-dates-and-adjustments",
        IntegrationEventType.PERSON_ADDRESS_CHANGED to "$baseUrl/v1/persons/$hmppsId/addresses",
        IntegrationEventType.PERSON_CONTACTS_CHANGED to "$baseUrl/v1/persons/$hmppsId/contacts",
        IntegrationEventType.PERSON_IEP_LEVEL_CHANGED to "$baseUrl/v1/persons/$hmppsId/iep-level",
        IntegrationEventType.PERSON_VISIT_RESTRICTIONS_CHANGED to "$baseUrl/v1/persons/$hmppsId/visit-restrictions",
        IntegrationEventType.PERSON_ALERTS_CHANGED to "$baseUrl/v1/persons/$hmppsId/alerts",
        IntegrationEventType.PERSON_PND_ALERTS_CHANGED to "$baseUrl/v1/pnd/persons/$hmppsId/alerts",
        IntegrationEventType.PERSON_RESPONSIBLE_OFFICER_CHANGED to "$baseUrl/v1/persons/$hmppsId/person-responsible-officer",
      ),
    )
  }

  private fun getHmppsIdForTesting(eventType: String): String = when (eventType) {
    HmppsDomainEventName.ProbabtionCase.Engagement.CREATED,
    HmppsDomainEventName.ProbabtionCase.PrisonIdentifier.ADDED,
    -> crn

    HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.CREATED,
    HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.RECEIVED,
    HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED,
    -> nomsNumber

    else -> throw RuntimeException("Unexpected event type: $eventType")
  }

  private class AllPersonEventsArgumentSource : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments?> = allPersonEventToMessageMap.map { Arguments.of(it.key) }.stream()
  }

  private class NewPersonEventsArgumentSource : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments?> = newPersonEventToMessageMap.map { Arguments.of(it.key) }.stream()
  }
}

// From: `uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.HmppsDomainEventsListenerTest` (`will process and save a person status event`)
class HmppsDomainEventServicePersonStatusEventTest : HmppsDomainEventServiceEventTestCase() {
  @ParameterizedTest
  @CsvSource(
    "probation-case.registration.added, ASFO, PROBATION_STATUS_CHANGED",
    "probation-case.registration.deleted, ASFO, PROBATION_STATUS_CHANGED",
    "probation-case.registration.deregistered, ASFO, PROBATION_STATUS_CHANGED",
    "probation-case.registration.updated, ASFO, PROBATION_STATUS_CHANGED",

    "probation-case.registration.added, WRSM, PROBATION_STATUS_CHANGED",
    "probation-case.registration.deleted, WRSM, PROBATION_STATUS_CHANGED",
    "probation-case.registration.deregistered, WRSM, PROBATION_STATUS_CHANGED",
    "probation-case.registration.updated, WRSM, PROBATION_STATUS_CHANGED",

    "probation-case.registration.added, RCCO, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, RCCO, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, RCCO, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, RCCO, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, RCPR, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, RCPR, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, RCPR, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, RCPR, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, RVAD, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, RVAD, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, RVAD, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, RVAD, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, STRG, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, STRG, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, STRG, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, STRG, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, AVIS, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, AVIS, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, AVIS, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, AVIS, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, WEAP, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, WEAP, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, WEAP, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, WEAP, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, RLRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, RLRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, RLRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, RLRH, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, RMRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, RMRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, RMRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, RMRH, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, RHRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, RHRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, RHRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, RHRH, DYNAMIC_RISKS_CHANGED",
  )
  fun `will process and save a person status event`(eventType: String, registerTypeCode: String, integrationEvent: String) {
    // Arrange
    val hmppsId = crn
    val hmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(
      eventType = eventType,
      registerTypeCode = registerTypeCode,
      identifiers = """[{"type":"CRN","value":"$crn"},{"type":"NOMS","value":"$nomsNumber"}]""",
    )
    stubDomainEventIdentitiesResolver(hmppsId = hmppsId)

    // Act
    hmppsDomainEventService.execute(hmppsDomainEvent)

    // Assert
    // verify event notification has been saved with expected event type
    verify(exactly = 1) { eventNotificationRepository.insertOrUpdate(match { it.eventType.name == integrationEvent }) }
  }
}
