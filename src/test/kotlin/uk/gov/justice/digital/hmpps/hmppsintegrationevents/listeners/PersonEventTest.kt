package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PRISONER_OFFENDER_SEARCH_PRISONER_CREATED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PRISONER_OFFENDER_SEARCH_PRISONER_RECEIVED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PRISONER_OFFENDER_SEARCH_PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PROBATION_CASE_ENGAGEMENT_CREATED_MESSAGE
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PROBATION_CASE_PRISON_IDENTIFIER_ADDED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.generateDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.generateHmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEventName
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DeadLetterQueueService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.HmppsDomainEventService

class PersonEventTest {
  private val hmppsDomainEventService = mockk<HmppsDomainEventService>()
  private val deadLetterQueueService = mockk<DeadLetterQueueService>()

  private val hmppsDomainEventsListener: HmppsDomainEventsListener =
    HmppsDomainEventsListener(hmppsDomainEventService, deadLetterQueueService)

  @ParameterizedTest
  @ValueSource(
    strings = [
      HmppsDomainEventName.ProbabtionCase.Engagement.CREATED,
      HmppsDomainEventName.ProbabtionCase.PrisonIdentifier.ADDED,
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.CREATED,
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED,
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.RECEIVED,
    ],
  )
  fun `process person events`(eventType: String) {
    val message = when (eventType) {
      HmppsDomainEventName.ProbabtionCase.Engagement.CREATED -> PROBATION_CASE_ENGAGEMENT_CREATED_MESSAGE
      HmppsDomainEventName.ProbabtionCase.PrisonIdentifier.ADDED -> PROBATION_CASE_PRISON_IDENTIFIER_ADDED
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.CREATED -> PRISONER_OFFENDER_SEARCH_PRISONER_CREATED
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED -> PRISONER_OFFENDER_SEARCH_PRISONER_UPDATED
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.RECEIVED -> PRISONER_OFFENDER_SEARCH_PRISONER_RECEIVED
      else -> throw RuntimeException("Unexpected event type: $eventType")
    }

    val hmppsMessage = message.replace("\\", "")
    val payload = generateDomainEvent(eventType, message)
    val hmppsDomainEvent = generateHmppsDomainEvent(eventType, hmppsMessage)

    every { hmppsDomainEventService.execute(hmppsDomainEvent, any()) } just runs

    hmppsDomainEventsListener.onDomainEvent(payload)

    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent, match { it.contains(IntegrationEventType.PERSON_STATUS_CHANGED) }) }
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      HmppsDomainEventName.ProbabtionCase.Engagement.CREATED,
      HmppsDomainEventName.ProbabtionCase.PrisonIdentifier.ADDED,
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.CREATED,
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.RECEIVED,
    ],
  )
  fun `process new person events`(eventType: String) {
    val message = when (eventType) {
      HmppsDomainEventName.ProbabtionCase.Engagement.CREATED -> PROBATION_CASE_ENGAGEMENT_CREATED_MESSAGE
      HmppsDomainEventName.ProbabtionCase.PrisonIdentifier.ADDED -> PROBATION_CASE_PRISON_IDENTIFIER_ADDED
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.CREATED -> PRISONER_OFFENDER_SEARCH_PRISONER_CREATED
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.RECEIVED -> PRISONER_OFFENDER_SEARCH_PRISONER_RECEIVED
      else -> throw RuntimeException("Unexpected event type: $eventType")
    }

    val hmppsMessage = message.replace("\\", "")
    val payload = generateDomainEvent(eventType, message)
    val hmppsDomainEvent = generateHmppsDomainEvent(eventType, hmppsMessage)

    every { hmppsDomainEventService.execute(hmppsDomainEvent, any()) } just runs

    hmppsDomainEventsListener.onDomainEvent(payload)

    verify(exactly = 1) {
      hmppsDomainEventService.execute(
        hmppsDomainEvent,
        match {
          it.containsAll(
            listOf(
              IntegrationEventType.PERSON_STATUS_CHANGED,
              IntegrationEventType.PERSON_CASE_NOTES_CHANGED,
              IntegrationEventType.PERSON_NAME_CHANGED,
              IntegrationEventType.PERSON_SENTENCES_CHANGED,
              IntegrationEventType.PERSON_PROTECTED_CHARACTERISTICS_CHANGED,
              IntegrationEventType.PERSON_REPORTED_ADJUDICATIONS_CHANGED,
              IntegrationEventType.PERSON_NUMBER_OF_CHILDREN_CHANGED,
              IntegrationEventType.PERSON_PHYSICAL_CHARACTERISTICS_CHANGED,
              IntegrationEventType.PERSON_IMAGES_CHANGED,
              IntegrationEventType.PERSON_HEALTH_AND_DIET_CHANGED,
              IntegrationEventType.PERSON_CARE_NEEDS_CHANGED,
              IntegrationEventType.PERSON_LANGUAGES_CHANGED,
              IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE,
              IntegrationEventType.PERSON_ADDRESS_CHANGED,
              IntegrationEventType.PERSON_CONTACTS_CHANGED,
              IntegrationEventType.PERSON_IEP_LEVEL_CHANGED,
              IntegrationEventType.PERSON_VISIT_RESTRICTIONS_CHANGED,
              IntegrationEventType.PERSON_ALERTS_CHANGED,
              IntegrationEventType.PERSON_PND_ALERTS_CHANGED,
              IntegrationEventType.PERSON_RESPONSIBLE_OFFICER_CHANGED
            ),
          )
        },
      )
    }
  }
}
