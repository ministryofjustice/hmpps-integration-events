package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.generateHmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.extractDomainEventFrom
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.SQSMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEventName
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime
import java.time.ZoneId

class HmppsDomainEventServiceEventTest {
  // From `uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.AdjudicationEventTest`
  @Nested
  inner class AdjudicationEventTest : HmppsDomainEventServiceEventTestCase() {
    private val nomsNumber = "A1234BC"
    private val hmppsId = nomsNumber

    @ParameterizedTest
    @ValueSource(
      strings = [
        HmppsDomainEventName.Adjudication.Hearing.CREATED,
        HmppsDomainEventName.Adjudication.Hearing.DELETED,
        HmppsDomainEventName.Adjudication.Hearing.COMPLETED,
        HmppsDomainEventName.Adjudication.Punishments.CREATED,
        HmppsDomainEventName.Adjudication.Report.CREATED,
      ],
    )
    fun `will process and save an adjudication notification`(eventType: String) {
      val message = """
        {
          "eventType": "$eventType",
          "version": "1.0",
          "description": "An adjudication has been created:  MDI-000169",
          "occurredAt": "2024-08-14T12:33:34+01:00",
          "additionalInformation": {
            "prisonerNumber": "$nomsNumber"
          }
        }
      """.cleansed()

      executeShouldSaveEventNotificationOfPerson(
        hmppsEventType = eventType,
        hmppsMessage = message,
        hmppsId = hmppsId,
        expectedNotificationType = IntegrationEventType.PERSON_REPORTED_ADJUDICATIONS_CHANGED,
        expectedUrl = "$baseUrl/v1/persons/$hmppsId/reported-adjudications",
      )
    }
  }

  // From `uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.IEPReviewEventTest`
  @Nested
  inner class IEPReviewEventTest : HmppsDomainEventServiceEventTestCase() {
    private val nomsNumber = "A1234BC"
    private val hmppsId = nomsNumber

    @ParameterizedTest
    @ValueSource(
      strings = [
        HmppsDomainEventName.Incentives.IEPReview.INSERTED,
        HmppsDomainEventName.Incentives.IEPReview.UPDATED,
        HmppsDomainEventName.Incentives.IEPReview.DELETED,
      ],
    )
    fun `will process and save an incentive review notification`(eventType: String) {
      val message = """
        {
          "eventType": "$eventType",
          "version": "1.0",
          "description": "An IEP review has been changed",
          "occurredAt": "2024-08-14T12:33:34+01:00",
          "additionalInformation": {
            "nomsNumber": "$nomsNumber"
          }
        }
      """.cleansed()

      executeShouldSaveEventNotificationOfPerson(
        hmppsEventType = eventType,
        hmppsMessage = message,
        hmppsId = hmppsId,
        expectedNotificationType = IntegrationEventType.PERSON_IEP_LEVEL_CHANGED,
        expectedUrl = "$baseUrl/v1/persons/$hmppsId/iep-level",
      )
    }
  }

  // From `uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.LocationEventTest`
  @Nested
  inner class LocationEventTest : HmppsDomainEventServiceEventTestCase() {
    private val prisonId = "MDI"
    private val locationKey = "MDI-001-01"

    @BeforeEach
    internal fun setUp() {
      stubDomainEventIdentitiesResolver(prisonId = prisonId)
    }

    @ParameterizedTest
    @ValueSource(
      strings = [
        HmppsDomainEventName.LocationsInsidePrison.Location.CREATED,
        HmppsDomainEventName.LocationsInsidePrison.Location.AMENDED,
        HmppsDomainEventName.LocationsInsidePrison.Location.DELETED,
        HmppsDomainEventName.LocationsInsidePrison.Location.DEACTIVATED,
        HmppsDomainEventName.LocationsInsidePrison.Location.REACTIVATED,
      ],
    )
    fun `will process and save location and residential events `(eventType: String) {
      val message = """
        {
          "eventType": "$eventType",
          "version": "1.0",
          "description": "Locations – a location inside prison has been amended",
          "occurredAt": "2024-08-14T12:33:34+01:00",
          "additionalInformation": {
            "key": "$locationKey"
          }
        }
      """.cleansed()
      val expectedEventNotifications = mapOf(
        IntegrationEventType.PRISON_LOCATION_CHANGED to "$baseUrl/v1/prison/$prisonId/location/$locationKey",
        IntegrationEventType.PRISON_RESIDENTIAL_HIERARCHY_CHANGED to "$baseUrl/v1/prison/$prisonId/residential-hierarchy",
        IntegrationEventType.PRISON_RESIDENTIAL_DETAILS_CHANGED to "$baseUrl/v1/prison/$prisonId/residential-details",
      ).map { generateEventNotificationOfPrison(eventType = it.key, url = it.value, prisonId = prisonId) }

      executeShouldSaveEventNotification(eventType, message, expectedEventNotifications)
    }

    @ParameterizedTest
    @ValueSource(
      strings = [
        HmppsDomainEventName.LocationsInsidePrison.Location.CREATED,
        HmppsDomainEventName.LocationsInsidePrison.Location.DELETED,
        HmppsDomainEventName.LocationsInsidePrison.Location.DEACTIVATED,
        HmppsDomainEventName.LocationsInsidePrison.Location.REACTIVATED,
        HmppsDomainEventName.LocationsInsidePrison.SignedOpCapacity.AMENDED,
      ],
    )
    fun `will process and save a prison capacity event`(eventType: String) {
      val message = """
        {
          "eventType": "$eventType",
          "version": "1.0",
          "description": "Locations – a location inside prison has been amended",
          "occurredAt": "2024-08-14T12:33:34+01:00",
          "additionalInformation": {
            "key": "$locationKey"
          }
        }
      """.cleansed()

      executeShouldSaveEventNotificationOfPrison(
        hmppsEventType = eventType,
        hmppsMessage = message,
        prisonId = prisonId,
        expectedNotificationType = IntegrationEventType.PRISON_CAPACITY_CHANGED,
        expectedUrl = "$baseUrl/v1/prison/$prisonId/capacity",
      )
    }
  }

  // From `uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.NonAssociationsEventTest`
  @Nested
  inner class NonAssociationsEventTest : HmppsDomainEventServiceEventTestCase() {
    private val nomsNumber = "A1234BC"
    private val hmppsId = nomsNumber
    private val prisonId = "KMI"

    @ParameterizedTest
    @ValueSource(
      strings = [
        HmppsDomainEventName.PrisonOffenderEvents.Prisoner.NonAssociationDetail.CHANGED,
//      HmppsDomainEventName.NonAssociations.CREATED,
//      HmppsDomainEventName.NonAssociations.AMENDED,
//      HmppsDomainEventName.NonAssociations.CLOSED,
//      HmppsDomainEventName.NonAssociations.DELETED,
      ],
    )
    fun `will process and save a non-association notification`(eventType: String) {
      val message = """
        {
          "eventType": "$eventType",
          "version": "1.0",
          "description": "A prisoner non-association detail record has changed",
          "occurredAt": "2024-08-14T12:33:34+01:00",
          "personReference": {
            "identifiers": [
              {
                "type": "NOMS", 
                "value": "$nomsNumber"
               }
            ]
          }
        }
      """.cleansed()

      executeShouldSaveEventNotificationOfPersonInPrison(
        hmppsEventType = eventType,
        hmppsMessage = message,
        prisonId = prisonId,
        hmppsId = hmppsId,
        expectedNotificationType = IntegrationEventType.PRISONER_NON_ASSOCIATIONS_CHANGED,
        expectedUrl = "$baseUrl/v1/prison/$prisonId/prisoners/$hmppsId/non-associations",
      )
    }
  }

  // From `uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.PersonCaseNotesEventTest`
  @Nested
  inner class PersonCaseNotesEventTest : HmppsDomainEventServiceEventTestCase() {
    private val nomsNumber = "A1234BC"

    @ParameterizedTest
    @ValueSource(
      strings = [
        HmppsDomainEventName.Person.CaseNote.CREATED,
        HmppsDomainEventName.Person.CaseNote.UPDATED,
        HmppsDomainEventName.Person.CaseNote.DELETED,
      ],
    )
    fun `will process and save a case note notification`(eventType: String) {
      val hmppsId = nomsNumber
      val message = """
        {
          "eventType": "$eventType",
          "version": "1.0",
          "description": "A case note has been created for a person",
          "occurredAt": "2024-08-14T12:33:34+01:00",
          "personReference": {
            "identifiers": [
              {
                "type": "NOMS", 
                "value": "$nomsNumber"
               }
            ]
          }
        }
      """.cleansed()

      executeShouldSaveEventNotificationOfPerson(
        hmppsEventType = eventType,
        hmppsMessage = message,
        hmppsId = hmppsId,
        expectedNotificationType = IntegrationEventType.PERSON_CASE_NOTES_CHANGED,
        expectedUrl = "$baseUrl/v1/persons/$hmppsId/case-notes",
      )
    }
  }

  // From `uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.PrisonerContactEventTest`
  @Nested
  inner class PrisonerContactEventTest : HmppsDomainEventServiceEventTestCase() {
    private val nomsNumber = "A1234BC"
    private val hmppsId = nomsNumber

    @ParameterizedTest
    @ValueSource(
      strings = [
        HmppsDomainEventName.PrisonOffenderEvents.Prisoner.CONTACT_ADDED,
        HmppsDomainEventName.PrisonOffenderEvents.Prisoner.CONTACT_APPROVED,
        HmppsDomainEventName.PrisonOffenderEvents.Prisoner.CONTACT_UNAPPROVED,
        HmppsDomainEventName.PrisonOffenderEvents.Prisoner.CONTACT_REMOVED,
      ],
    )
    fun `will process and save a prisoner contact notification`(eventType: String) {
      val message = """
        {
          "eventType": "$eventType",
          "version": 1,
          "description": "A contact has been added to a prisoner",
          "occurredAt": "2024-08-14T12:33:34+01:00",
          "personReference": {
            "identifiers": [
              {
                "type": "NOMS", 
                "value": "$nomsNumber"
               }
            ]
          }
        }
      """.cleansed()

      executeShouldSaveEventNotificationOfPerson(
        hmppsEventType = eventType,
        hmppsMessage = message,
        hmppsId = hmppsId,
        expectedNotificationType = IntegrationEventType.PERSON_CONTACTS_CHANGED,
        expectedUrl = "$baseUrl/v1/persons/$hmppsId/contacts",
      )
    }
  }

  // From `uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.PrisonerVisitorRestrictionEventTest`
  @Nested
  inner class PrisonerVisitorRestrictionEventTest : HmppsDomainEventServiceEventTestCase() {
    private val nomsNumber = "A1234BC"
    private val hmppsId = nomsNumber

    @ParameterizedTest
    @ValueSource(
      strings = [
        HmppsDomainEventName.PrisonOffenderEvents.Prisoner.PersonRestriction.UPSERTED,
        HmppsDomainEventName.PrisonOffenderEvents.Prisoner.PersonRestriction.DELETED,
      ],
    )
    fun `will process and save a prison visitor restriction notification`(eventType: String) {
      val contactId = "7551236"
      val message = """
        {
          "eventType": "$eventType",
          "version": "1.0",
          "description": "This event is raised when a global visitor restriction is created or updated.",
          "occurredAt": "2024-08-14T12:33:34+01:00",
          "additionalInformation": {
            "contactPersonId": "$contactId"
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
      """.cleansed()

      executeShouldSaveEventNotificationOfPerson(
        hmppsEventType = eventType,
        hmppsMessage = message,
        hmppsId = hmppsId,
        expectedNotificationType = IntegrationEventType.PERSON_VISITOR_RESTRICTIONS_CHANGED,
        expectedUrl = "$baseUrl/v1/persons/$hmppsId/visitor/$contactId/restrictions",
      )
    }
  }

  // From `uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.PrisonerVisitRestrictionEventTest`
  @Nested
  inner class PrisonerVisitRestrictionEventTest : HmppsDomainEventServiceEventTestCase() {
    private val nomsNumber = "A1234BC"
    private val hmppsId = nomsNumber

    @ParameterizedTest
    @ValueSource(
      strings = [
        HmppsDomainEventName.PrisonOffenderEvents.Prisoner.Restriction.CHANGED,
      ],
    )
    fun `will process an prisoner visit restriction notification`(eventType: String) {
      val message = """
        {
          "eventType": "$eventType",
          "version": "1.0",
          "description": "This event is raised when a prisoner visits restriction is created/updated/deleted",
          "occurredAt": "2024-08-14T12:33:34+01:00",
          "personReference": {
            "identifiers": [
              {
                "type": "NOMS", 
                "value": "$nomsNumber"
               }
            ]
          }
        }
      """.cleansed()
      //
      executeShouldSaveEventNotificationOfPerson(
        hmppsEventType = eventType,
        hmppsMessage = message,
        hmppsId = hmppsId,
        expectedNotificationType = IntegrationEventType.PERSON_VISIT_RESTRICTIONS_CHANGED,
        expectedUrl = "$baseUrl/v1/persons/$hmppsId/visit-restrictions",
      )
    }
  }

  // From `uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.PrisonVisitEventTest`
  @Nested
  inner class PrisonVisitEventTest : HmppsDomainEventServiceEventTestCase() {
    private val nomsNumber = "A1234BC"
    private val hmppsId = nomsNumber
    private val prisonId = "MDI"

    @ParameterizedTest
    @ValueSource(
      strings = [
        HmppsDomainEventName.PrisonVisit.BOOKED,
        HmppsDomainEventName.PrisonVisit.CHANGED,
        HmppsDomainEventName.PrisonVisit.CANCELLED,
      ],
    )
    fun `will process and save a visit changed notification`(eventType: String) {
      val visitReference = "nx-ce-vq-ry"
      val message = """
        {
          "eventType": "$eventType",
          "version": "1.0",
          "description": "Prison visit changed",
          "occurredAt": "2024-08-14T12:33:34+01:00",
          "prisonerId": "$nomsNumber",
          "additionalInformation": {
            "reference": "$visitReference"
          }
        }
      """.cleansed()
      val expectedEventNotifications = mapOf(
        IntegrationEventType.PERSON_FUTURE_VISITS_CHANGED to "$baseUrl/v1/persons/$hmppsId/visit/future",
        IntegrationEventType.PRISON_VISITS_CHANGED to "$baseUrl/v1/prison/$prisonId/visit/search",
        IntegrationEventType.VISIT_CHANGED to "$baseUrl/v1/visit/$visitReference",
      ).map { generateEventNotificationOfPrison(eventType = it.key, url = it.value, prisonId = prisonId, hmppsId = hmppsId) }
      stubDomainEventIdentitiesResolver(hmppsId, prisonId)

      executeShouldSaveEventNotification(eventType, message, expectedEventNotifications)
    }
  }
}

abstract class HmppsDomainEventServiceEventTestCase {
  protected val baseUrl = "https://dev.integration-api.hmpps.service.justice.gov.uk"

  protected val eventNotificationRepository = mockk<EventNotificationRepository>()
  protected val deadLetterQueueService = mockk<DeadLetterQueueService>()
  protected val domainEventIdentitiesResolver = mockk<DomainEventIdentitiesResolver>()
  protected val hmppsDomainEventService: HmppsDomainEventService = HmppsDomainEventService(
    eventNotificationRepository,
    deadLetterQueueService,
    domainEventIdentitiesResolver,
    baseUrl,
  )
  protected val currentTime: LocalDateTime = LocalDateTime.now()
  protected val zonedCurrentDateTime = currentTime.atZone(ZoneId.systemDefault())
  protected val objectMapper by lazy { jacksonObjectMapper() }

  companion object {
    @BeforeAll
    @JvmStatic
    internal fun setupAll() {
      mockkStatic(LocalDateTime::class)
    }

    @AfterAll
    @JvmStatic
    internal fun tearDownAll() {
      unmockkStatic(LocalDateTime::class)
    }
  }

  @BeforeEach
  open fun setup() {
    every { LocalDateTime.now() } returns currentTime

    every { eventNotificationRepository.insertOrUpdate(any()) } returnsArgument 0
  }

  @AfterEach
  fun cleanup() {
    clearAllMocks()
  }

  protected fun executeShouldSaveEventNotificationOfPerson(
    hmppsEventType: String,
    hmppsMessage: String,
    hmppsId: String,
    expectedNotificationType: IntegrationEventType,
    expectedUrl: String,
  ) {
    // Arrange
    val hmppsDomainEvent = generateHmppsDomainEvent(hmppsEventType, hmppsMessage).domainEvent()
    val expectedEventNotification = generateEventNotification(expectedNotificationType, expectedUrl, hmppsId)
    stubDomainEventIdentitiesResolver(hmppsId = hmppsId)

    // Act, Assert
    executeShouldSaveEventNotification(hmppsDomainEvent, expectedEventNotification)
  }

  /**
   * Run a test of execute() should save multiply `EventNotification` of a person
   *
   * @param hmppsEventType type of the domain event
   * @param hmppsMessage message (event message body) of the domain event
   * @param hmppsId HMPPS ID
   * @param expectedNotificationTypeAndUrls Expected notification type (mapping key) and URL (mapping value)
   */
  protected fun executeShouldSaveMultipleEventNotificationsOfPerson(
    hmppsEventType: String,
    hmppsMessage: String,
    hmppsId: String,
    expectedNotificationTypeAndUrls: Map<IntegrationEventType, String>,
  ) {
    // Arrange
    val expectedEventNotifications = expectedNotificationTypeAndUrls.map { generateEventNotification(it.key, it.value, hmppsId) }
    stubDomainEventIdentitiesResolver(hmppsId = hmppsId)

    // Act, Assert
    executeShouldSaveEventNotification(hmppsEventType, hmppsMessage, expectedEventNotifications)
  }

  protected fun executeShouldSaveMultipleEventNotificationsOfPersonInPrison(
    hmppsEventType: String,
    hmppsMessage: String,
    hmppsId: String,
    prisonId: String,
    expectedNotificationTypeAndUrls: Map<IntegrationEventType, String>,
  ) {
    // Arrange
    val expectedEventNotifications = expectedNotificationTypeAndUrls.map { generateEventNotificationOfPrison(it.key, it.value, prisonId, hmppsId) }
    stubDomainEventIdentitiesResolver(hmppsId, prisonId)

    // Act, Assert
    executeShouldSaveEventNotification(hmppsEventType, hmppsMessage, expectedEventNotifications)
  }

  protected fun executeShouldSaveEventNotificationOfPersonInPrison(
    hmppsEventType: String,
    hmppsMessage: String,
    prisonId: String,
    hmppsId: String,
    expectedNotificationType: IntegrationEventType,
    expectedUrl: String,
  ) {
    // Arrange
    val hmppsDomainEvent = generateHmppsDomainEvent(hmppsEventType, hmppsMessage).domainEvent()
    val expectedEventNotification = generateEventNotificationOfPrison(expectedNotificationType, expectedUrl, prisonId, hmppsId)
    stubDomainEventIdentitiesResolver(hmppsId = hmppsId, prisonId = prisonId)

    // Act, Assert
    executeShouldSaveEventNotification(hmppsDomainEvent, expectedEventNotification)
  }

  protected fun executeShouldSaveEventNotificationOfPrison(
    hmppsEventType: String,
    hmppsMessage: String,
    prisonId: String,
    expectedNotificationType: IntegrationEventType,
    expectedUrl: String,
  ) {
    // Arrange
    val hmppsDomainEvent = generateHmppsDomainEvent(hmppsEventType, hmppsMessage).domainEvent()
    val expectedEventNotification = generateEventNotificationOfPrison(expectedNotificationType, expectedUrl, prisonId)
    stubDomainEventIdentitiesResolver(prisonId = prisonId)

    // Act, Assert
    executeShouldSaveEventNotification(hmppsDomainEvent, expectedEventNotification)
  }

  protected fun executeShouldSaveEventNotification(
    hmppsEventType: String,
    hmppsMessage: String,
    expectedEventNotifications: List<EventNotification>,
  ) = executeShouldSaveEventNotification(
    hmppsDomainEvent = generateHmppsDomainEvent(hmppsEventType, hmppsMessage).domainEvent(),
    expectedEventNotifications = expectedEventNotifications,
  )

  protected fun executeShouldSaveEventNotification(
    hmppsDomainEvent: HmppsDomainEvent,
    expectedEventNotifications: List<EventNotification>,
  ) {
    // Act
    hmppsDomainEventService.execute(hmppsDomainEvent)

    // Assert
    expectedEventNotifications.forEach { expectedNotification ->
      // Verify all expected event notifications persisted via repository
      verify(exactly = 1) { eventNotificationRepository.insertOrUpdate(expectedNotification) }
    }
  }

  protected fun executeShouldSaveEventNotification(
    hmppsDomainEvent: HmppsDomainEvent,
    vararg expectedEventNotification: EventNotification,
  ) {
    // Act
    hmppsDomainEventService.execute(hmppsDomainEvent)

    // Assert
    expectedEventNotification.forEach { expectedNotification ->
      // Verify all expected event notifications persisted via repository
      verify(exactly = 1) { eventNotificationRepository.insertOrUpdate(expectedNotification) }
    }
  }

  protected fun generateEventNotification(
    eventType: IntegrationEventType,
    url: String,
    hmppsId: String,
  ) = EventNotification(eventType = eventType, hmppsId = hmppsId, url = url, lastModifiedDateTime = currentTime)

  protected fun generateEventNotificationOfPrison(
    eventType: IntegrationEventType,
    url: String,
    prisonId: String,
    hmppsId: String? = null,
  ) = EventNotification(eventType = eventType, prisonId = prisonId, hmppsId = hmppsId, url = url, lastModifiedDateTime = currentTime)

  protected fun stubDomainEventIdentitiesResolver(hmppsId: String? = null, prisonId: String? = null) {
    every { domainEventIdentitiesResolver.getHmppsId(any()) } returns hmppsId
    every { domainEventIdentitiesResolver.getPrisonId(any()) } returns prisonId
  }

  protected fun SQSMessage.domainEvent(): HmppsDomainEvent = extractDomainEventFrom(this, objectMapper)
}

/**
 * Cleanse an event message
 *
 * - tidy up format
 * @receiver A message of domain event
 */
private fun String.cleansed() = this.trimIndent().replace("\n", "")
