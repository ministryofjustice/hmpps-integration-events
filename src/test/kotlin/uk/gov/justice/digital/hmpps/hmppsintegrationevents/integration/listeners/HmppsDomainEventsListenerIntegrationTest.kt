package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.listeners

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.awaitility.Awaitility
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEventName
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.wiremock.HmppsAuthExtension
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.wiremock.ProbationIntegrationApiExtension
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@ExtendWith(ProbationIntegrationApiExtension::class, HmppsAuthExtension::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class HmppsDomainEventsListenerIntegrationTest : SqsIntegrationTestBase() {

  @Autowired
  lateinit var repo: EventNotificationRepository

  val nomsNumber = "mockNomsNumber"
  val crn = "mockCrn"

  @BeforeEach
  fun setup() {
    repo.deleteAll()
    ProbationIntegrationApiExtension.server.stubGetPersonIdentifier(nomsNumber, crn)
  }

  @Test
  fun `will process and save a valid domain event SQS message`() {
    ProbationIntegrationApiExtension.server.stubGetIfPersonExists("X777776")
    val rawMessage = SqsNotificationGeneratingHelper().generateRawGenericEvent()
    sendDomainSqsMessage(rawMessage)

    Awaitility.await().until { repo.findAll().isNotEmpty() }
    val savedEvent = repo.findAll().firstOrNull()
    savedEvent.shouldNotBeNull()
  }

  @Test
  fun `will not process a malformed domain event SQS Message and log to dead letter queue`() {
    sendDomainSqsMessage("BAD JSON")

    Awaitility.await().until { getNumberOfMessagesCurrentlyOndomainEventsDeadLetterQueue() == 1 }
    val deadLetterQueueMessage = geMessagesCurrentlyOnDomainEventsDeadLetterQueue()
    val message = deadLetterQueueMessage.messages().first()
    message.body().shouldBe("BAD JSON")
    val savedEvent = repo.findAll().firstOrNull()
    savedEvent.shouldBeNull()
  }

  @Test
  fun `will not process and save a domain event message with an unknown type`() {
    val rawMessage = SqsNotificationGeneratingHelper().generateRawGenericEvent(eventTypeValue = "some.other-event")

    sendDomainSqsMessage(rawMessage)

    Awaitility.await().until { getNumberOfMessagesCurrentlyOndomainEventsDeadLetterQueue() == 0 }
    val savedEvent = repo.findAll().firstOrNull()
    savedEvent.shouldBeNull()
  }

  @Test
  fun `will not process and save a domain event message with an unknown register type code`() {
    val rawMessage = SqsNotificationGeneratingHelper().generateRawHmppsDomainEvent(registerTypeCode = "OtherType")

    sendDomainSqsMessage(rawMessage)

    val savedEvent = repo.findAll().firstOrNull()

    savedEvent.shouldBeNull()
  }

  @Test
  fun `will not process and save a domain event message with no crn type and log to dead letter queue`() {
    val rawMessage = SqsNotificationGeneratingHelper().generateRawHmppsDomainEvent(identifiers = "[]")

    sendDomainSqsMessage(rawMessage)

    Awaitility.await().until { getNumberOfMessagesCurrentlyOndomainEventsDeadLetterQueue() == 1 }
    val deadLetterQueueMessage = geMessagesCurrentlyOnDomainEventsDeadLetterQueue()
    val message = deadLetterQueueMessage.messages().first()
    val payload = message.body()
    payload.shouldBe(rawMessage)
    val savedEvent = repo.findAll().firstOrNull()
    savedEvent.shouldBeNull()
  }

  // Specific event tests

  @Test
  fun `will process and save a prisoner released event SQS message`() {
    val rawMessage = SqsNotificationGeneratingHelper().generatePrisonerReleasedEvent()
    sendDomainSqsMessage(rawMessage)

    Awaitility.await().until { repo.findAll().isNotEmpty() }
    val savedEvent = repo.findAll().firstOrNull()
    savedEvent.shouldNotBeNull()
    savedEvent.eventType.shouldBe(IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE)
    savedEvent.hmppsId.shouldBe("mockCrn")
    savedEvent.url.shouldBe("https://localhost:8443/v1/persons/mockCrn/sentences/latest-key-dates-and-adjustments")
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      HmppsDomainEventName.PrisonOffenderEvents.Prisoner.CONTACT_ADDED,
      HmppsDomainEventName.PrisonOffenderEvents.Prisoner.CONTACT_APPROVED,
      HmppsDomainEventName.PrisonOffenderEvents.Prisoner.CONTACT_UNAPPROVED,
      HmppsDomainEventName.PrisonOffenderEvents.Prisoner.CONTACT_REMOVED,
    ],
  )
  fun `will process and save a person contacts event SQS message`(eventType: String) {
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
    """
    val rawMessage = SqsNotificationGeneratingHelper().generateRawDomainEvent(eventType, message)
    sendDomainSqsMessage(rawMessage)

    Awaitility.await().until { repo.findAll().isNotEmpty() }
    val savedEvent = repo.findAll().firstOrNull()
    savedEvent.shouldNotBeNull()
    savedEvent.eventType.shouldBe(IntegrationEventType.PERSON_CONTACTS_CHANGED)
    savedEvent.hmppsId.shouldBe(crn)
    savedEvent.url.shouldBe("https://localhost:8443/v1/persons/$crn/contacts")
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      HmppsDomainEventName.Incentives.IEPReview.INSERTED,
      HmppsDomainEventName.Incentives.IEPReview.UPDATED,
      HmppsDomainEventName.Incentives.IEPReview.DELETED,
    ],
  )
  fun `will process and save a person iep event SQS message`(eventType: String) {
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
    """
    val rawMessage = SqsNotificationGeneratingHelper().generateRawDomainEvent(eventType, message)
    sendDomainSqsMessage(rawMessage)

    Awaitility.await().until { repo.findAll().isNotEmpty() }
    val savedEvent = repo.findAll().firstOrNull()
    savedEvent.shouldNotBeNull()
    savedEvent.eventType.shouldBe(IntegrationEventType.PERSON_IEP_LEVEL_CHANGED)
    savedEvent.hmppsId.shouldBe(crn)
    savedEvent.url.shouldBe("https://localhost:8443/v1/persons/$crn/iep-level")
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      HmppsDomainEventName.PrisonOffenderEvents.Prisoner.PersonRestriction.UPSERTED,
      HmppsDomainEventName.PrisonOffenderEvents.Prisoner.PersonRestriction.DELETED,
    ],
  )
  fun `will process and save a visitor restriction event SQS message`(eventType: String) {
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
    """
    val rawMessage = SqsNotificationGeneratingHelper().generateRawDomainEvent(eventType, message)
    sendDomainSqsMessage(rawMessage)

    Awaitility.await().until { repo.findAll().isNotEmpty() }
    val savedEvent = repo.findAll().firstOrNull()
    savedEvent.shouldNotBeNull()
    savedEvent.eventType.shouldBe(IntegrationEventType.PERSON_VISITOR_RESTRICTIONS_CHANGED)
    savedEvent.hmppsId.shouldBe(crn)
    savedEvent.url.shouldBe("https://localhost:8443/v1/persons/$crn/visitor/$contactId/restrictions")
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      HmppsDomainEventName.PrisonOffenderEvents.Prisoner.Restriction.CHANGED,
    ],
  )
  fun `will process and save a visit restriction event SQS message`(eventType: String) {
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
    """
    val rawMessage = SqsNotificationGeneratingHelper().generateRawDomainEvent(eventType, message)
    sendDomainSqsMessage(rawMessage)

    Awaitility.await().until { repo.findAll().isNotEmpty() }
    val savedEvent = repo.findAll().firstOrNull()
    savedEvent.shouldNotBeNull()
    savedEvent.eventType.shouldBe(IntegrationEventType.PERSON_VISIT_RESTRICTIONS_CHANGED)
    savedEvent.hmppsId.shouldBe(crn)
    savedEvent.url.shouldBe("https://localhost:8443/v1/persons/$crn/visit-restrictions")
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      HmppsDomainEventName.PrisonVisit.BOOKED,
      HmppsDomainEventName.PrisonVisit.CHANGED,
      HmppsDomainEventName.PrisonVisit.CANCELLED,
    ],
  )
  fun `will process and save a visit changed event SQS message`(eventType: String) {
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
    """
    val rawMessage = SqsNotificationGeneratingHelper().generateRawDomainEvent(eventType, message)
    sendDomainSqsMessage(rawMessage)

    Awaitility.await().until { repo.findAll().isNotEmpty() }
    val savedEvents = repo.findAll()
    savedEvents.size.shouldBe(3)
    savedEvents[0].eventType.shouldBe(IntegrationEventType.PERSON_FUTURE_VISITS_CHANGED)
    savedEvents[0].hmppsId.shouldBe(crn)
    savedEvents[0].url.shouldBe("https://localhost:8443/v1/persons/$crn/visits/future")
    savedEvents[1].eventType.shouldBe(IntegrationEventType.PRISON_VISITS_CHANGED)
    savedEvents[1].hmppsId.shouldBe(crn)
    savedEvents[1].url.shouldBe("https://localhost:8443/v1/prison/{prisonId}/visit/search")
    savedEvents[2].eventType.shouldBe(IntegrationEventType.VISIT_CHANGED)
    savedEvents[2].hmppsId.shouldBe(crn)
    savedEvents[2].url.shouldBe("https://localhost:8443/v1/visit/$visitReference")
  }

  @Test
  fun `will process and save a prisoner personal details changed event SQS message`() {
    val eventType = HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED
    val message = """
    {
      "eventType": "$eventType",
      "version": "1.0",
      "description": "This is when a prisoner index record has been updated.",
      "occurredAt": "2024-08-14T12:33:34+01:00",
      "additionalInformation": {
        "categoriesChanged": ["PERSONAL_DETAILS"]
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
    """
    val rawMessage = SqsNotificationGeneratingHelper().generateRawDomainEvent(eventType, message)
    sendDomainSqsMessage(rawMessage)

    Awaitility.await().until { repo.findAll().isNotEmpty() }
    val savedEvents = repo.findAll()
    savedEvents.size.shouldBe(4)
    savedEvents[0].eventType.shouldBe(IntegrationEventType.PERSON_STATUS_CHANGED)
    savedEvents[0].hmppsId.shouldBe(crn)
    savedEvents[0].url.shouldBe("https://localhost:8443/v1/persons/$crn")
    savedEvents[1].eventType.shouldBe(IntegrationEventType.PERSON_NAME_CHANGED)
    savedEvents[1].hmppsId.shouldBe(crn)
    savedEvents[1].url.shouldBe("https://localhost:8443/v1/persons/$crn/name")
    savedEvents[2].eventType.shouldBe(IntegrationEventType.PRISONERS_CHANGED)
    savedEvents[2].hmppsId.shouldBe(crn)
    savedEvents[2].url.shouldBe("https://localhost:8443/v1/prison/prisoners")
    savedEvents[3].eventType.shouldBe(IntegrationEventType.PRISONER_CHANGED)
    savedEvents[3].hmppsId.shouldBe(crn)
    savedEvents[3].url.shouldBe("https://localhost:8443/v1/prison/prisoners/$crn")
  }

  @Test
  fun `will process and save a prisoner sentences changed event SQS message`() {
    val eventType = HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED
    val message = """
    {
      "eventType": "$eventType",
      "version": "1.0",
      "description": "This is when a prisoner index record has been updated.",
      "occurredAt": "2024-08-14T12:33:34+01:00",
      "additionalInformation": {
        "categoriesChanged": ["SENTENCE"]
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
    """
    val rawMessage = SqsNotificationGeneratingHelper().generateRawDomainEvent(eventType, message)
    sendDomainSqsMessage(rawMessage)

    Awaitility.await().until { repo.findAll().isNotEmpty() }
    val savedEvents = repo.findAll()
    savedEvents.size.shouldBe(4)
    savedEvents[0].eventType.shouldBe(IntegrationEventType.PERSON_STATUS_CHANGED)
    savedEvents[0].hmppsId.shouldBe(crn)
    savedEvents[0].url.shouldBe("https://localhost:8443/v1/persons/$crn")
    savedEvents[1].eventType.shouldBe(IntegrationEventType.PERSON_SENTENCES_CHANGED)
    savedEvents[1].hmppsId.shouldBe(crn)
    savedEvents[1].url.shouldBe("https://localhost:8443/v1/persons/$crn/sentences")
    savedEvents[2].eventType.shouldBe(IntegrationEventType.PRISONERS_CHANGED)
    savedEvents[2].hmppsId.shouldBe(crn)
    savedEvents[2].url.shouldBe("https://localhost:8443/v1/prison/prisoners")
    savedEvents[3].eventType.shouldBe(IntegrationEventType.PRISONER_CHANGED)
    savedEvents[3].hmppsId.shouldBe(crn)
    savedEvents[3].url.shouldBe("https://localhost:8443/v1/prison/prisoners/$crn")
  }

  @Test
  fun `will process and save a prisoner physical details changed event SQS message`() {
    val eventType = HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED
    val message = """
    {
      "eventType": "$eventType",
      "version": "1.0",
      "description": "This is when a prisoner index record has been updated.",
      "occurredAt": "2024-08-14T12:33:34+01:00",
      "additionalInformation": {
        "categoriesChanged": ["PHYSICAL_DETAILS"]
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
    """
    val rawMessage = SqsNotificationGeneratingHelper().generateRawDomainEvent(eventType, message)
    sendDomainSqsMessage(rawMessage)

    Awaitility.await().until { repo.findAll().isNotEmpty() }
    val savedEvents = repo.findAll()
    savedEvents.size.shouldBe(4)
    savedEvents[0].eventType.shouldBe(IntegrationEventType.PERSON_STATUS_CHANGED)
    savedEvents[0].hmppsId.shouldBe(crn)
    savedEvents[0].url.shouldBe("https://localhost:8443/v1/persons/$crn")
    savedEvents[1].eventType.shouldBe(IntegrationEventType.PERSON_PHYSICAL_CHARACTERISTICS_CHANGED)
    savedEvents[1].hmppsId.shouldBe(crn)
    savedEvents[1].url.shouldBe("https://localhost:8443/v1/persons/$crn/physical-characteristics")
    savedEvents[2].eventType.shouldBe(IntegrationEventType.PRISONERS_CHANGED)
    savedEvents[2].hmppsId.shouldBe(crn)
    savedEvents[2].url.shouldBe("https://localhost:8443/v1/prison/prisoners")
    savedEvents[3].eventType.shouldBe(IntegrationEventType.PRISONER_CHANGED)
    savedEvents[3].hmppsId.shouldBe(crn)
    savedEvents[3].url.shouldBe("https://localhost:8443/v1/prison/prisoners/$crn")
  }

  @Test
  fun `will process and save a prisoner location changed event SQS message`() {
    val eventType = HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED
    val message = """
    {
      "eventType": "$eventType",
      "version": "1.0",
      "description": "This is when a prisoner index record has been updated.",
      "occurredAt": "2024-08-14T12:33:34+01:00",
      "additionalInformation": {
        "categoriesChanged": ["LOCATION"]
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
    """
    val rawMessage = SqsNotificationGeneratingHelper().generateRawDomainEvent(eventType, message)
    sendDomainSqsMessage(rawMessage)

    Awaitility.await().until { repo.findAll().isNotEmpty() }
    val savedEvents = repo.findAll()
    savedEvents.size.shouldBe(4)
    savedEvents[0].eventType.shouldBe(IntegrationEventType.PERSON_STATUS_CHANGED)
    savedEvents[0].hmppsId.shouldBe(crn)
    savedEvents[0].url.shouldBe("https://localhost:8443/v1/persons/$crn")
    savedEvents[1].eventType.shouldBe(IntegrationEventType.PERSON_CELL_LOCATION_CHANGED)
    savedEvents[1].hmppsId.shouldBe(crn)
    savedEvents[1].url.shouldBe("https://localhost:8443/v1/persons/$crn/cell-location")
    savedEvents[2].eventType.shouldBe(IntegrationEventType.PRISONERS_CHANGED)
    savedEvents[2].hmppsId.shouldBe(crn)
    savedEvents[2].url.shouldBe("https://localhost:8443/v1/prison/prisoners")
    savedEvents[3].eventType.shouldBe(IntegrationEventType.PRISONER_CHANGED)
    savedEvents[3].hmppsId.shouldBe(crn)
    savedEvents[3].url.shouldBe("https://localhost:8443/v1/prison/prisoners/$crn")
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.CREATED,
      HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.RECEIVED,
    ],
  )
  fun `will process and save a prisoner created + received event SQS messages`(eventType: String) {
    val message = """
    {
      "eventType": "$eventType",
      "version": "1.0",
      "description": "This is when a prisoner index record has been updated.",
      "occurredAt": "2024-08-14T12:33:34+01:00",
      "additionalInformation": {
        "categoriesChanged": []
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
    """
    val rawMessage = SqsNotificationGeneratingHelper().generateRawDomainEvent(eventType, message)
    sendDomainSqsMessage(rawMessage)

    Awaitility.await().until { repo.findAll().isNotEmpty() }
    val savedEvents = repo.findAll()
    savedEvents.size.shouldBe(16)
    val eventTypes = savedEvents.map { it.eventType }
    val hmppsIds = savedEvents.map { it.hmppsId }
    val urls = savedEvents.map { it.url }

    eventTypes.shouldContainExactlyInAnyOrder(
      IntegrationEventType.PERSON_STATUS_CHANGED,
        IntegrationEventType.PERSON_CASE_NOTES_CHANGED,
        IntegrationEventType.PERSON_NAME_CHANGED,
        IntegrationEventType.PERSON_CELL_LOCATION_CHANGED,
        IntegrationEventType.PERSON_SENTENCES_CHANGED,
        IntegrationEventType.PERSON_PROTECTED_CHARACTERISTICS_CHANGED,
        IntegrationEventType.PERSON_REPORTED_ADJUDICATIONS_CHANGED,
        IntegrationEventType.PERSON_NUMBER_OF_CHILDREN_CHANGED,
        IntegrationEventType.PERSON_PHYSICAL_CHARACTERISTICS_CHANGED,
        IntegrationEventType.PERSON_IMAGES_CHANGED,
        IntegrationEventType.PERSON_HEALTH_AND_DIET_CHANGED,
        IntegrationEventType.PERSON_CARE_NEEDS_CHANGED,
        IntegrationEventType.PERSON_LANGUAGES_CHANGED,
        IntegrationEventType.PRISONERS_CHANGED,
        IntegrationEventType.PRISONER_CHANGED,
        IntegrationEventType.PRISONER_NON_ASSOCIATIONS_CHANGED,
      )
    hmppsIds.shouldContainOnly(crn)
    urls.shouldContainExactlyInAnyOrder(
      "https://localhost:8443/v1/persons/$crn",
      "https://localhost:8443/v1/persons/$crn/case-notes",
      "https://localhost:8443/v1/persons/$crn/name",
      "https://localhost:8443/v1/persons/$crn/cell-location",
      "https://localhost:8443/v1/persons/$crn/sentences",
      "https://localhost:8443/v1/persons/$crn/protected-characteristics",
      "https://localhost:8443/v1/persons/$crn/reported-adjudications",
      "https://localhost:8443/v1/persons/$crn/number-of-children",
      "https://localhost:8443/v1/persons/$crn/physical-characteristics",
      "https://localhost:8443/v1/persons/$crn/images",
      "https://localhost:8443/v1/persons/$crn/health-and-diet",
      "https://localhost:8443/v1/persons/$crn/care-needs",
      "https://localhost:8443/v1/persons/$crn/languages",
      "https://localhost:8443/v1/prison/prisoners",
      "https://localhost:8443/v1/prison/prisoners/$crn",
      "https://localhost:8443/v1/prison/{prisonId}/prisoners/$crn/non-associations",
    )
  }

  @Test
  fun `will process and save a prisoner updated event SQS message`() {
    val eventType = HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED
    val message = """
    {
      "eventType": "$eventType",
      "version": "1.0",
      "description": "This is when a prisoner index record has been updated.",
      "occurredAt": "2024-08-14T12:33:34+01:00",
      "additionalInformation": {
        "categoriesChanged": []
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
    """
    val rawMessage = SqsNotificationGeneratingHelper().generateRawDomainEvent(eventType, message)
    sendDomainSqsMessage(rawMessage)

    Awaitility.await().until { repo.findAll().isNotEmpty() }
    val savedEvents = repo.findAll()
    savedEvents.size.shouldBe(3)
    savedEvents[0].eventType.shouldBe(IntegrationEventType.PERSON_STATUS_CHANGED)
    savedEvents[0].hmppsId.shouldBe(crn)
    savedEvents[0].url.shouldBe("https://localhost:8443/v1/persons/$crn")
    savedEvents[1].eventType.shouldBe(IntegrationEventType.PRISONERS_CHANGED)
    savedEvents[1].hmppsId.shouldBe(crn)
    savedEvents[1].url.shouldBe("https://localhost:8443/v1/prison/prisoners")
    savedEvents[2].eventType.shouldBe(IntegrationEventType.PRISONER_CHANGED)
    savedEvents[2].hmppsId.shouldBe(crn)
    savedEvents[2].url.shouldBe("https://localhost:8443/v1/prison/prisoners/$crn")
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      HmppsDomainEventName.Person.CaseNote.CREATED,
      HmppsDomainEventName.Person.CaseNote.UPDATED,
      HmppsDomainEventName.Person.CaseNote.DELETED,
    ],
  )
  fun `will process and save a case note changed event SQS message`(eventType: String) {
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
    """
    val rawMessage = SqsNotificationGeneratingHelper().generateRawDomainEvent(eventType, message)
    sendDomainSqsMessage(rawMessage)

    Awaitility.await().until { repo.findAll().isNotEmpty() }
    val savedEvents = repo.findAll()
    savedEvents.size.shouldBe(1)
    savedEvents[0].eventType.shouldBe(IntegrationEventType.PERSON_CASE_NOTES_CHANGED)
    savedEvents[0].hmppsId.shouldBe(crn)
    savedEvents[0].url.shouldBe("https://localhost:8443/v1/persons/$crn/case-notes")
  }

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
  fun `will process and save a adjudication changed event SQS message`(eventType: String) {
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
    """
    val rawMessage = SqsNotificationGeneratingHelper().generateRawDomainEvent(eventType, message)
    sendDomainSqsMessage(rawMessage)

    Awaitility.await().until { repo.findAll().isNotEmpty() }
    val savedEvents = repo.findAll()
    savedEvents.size.shouldBe(1)
    savedEvents[0].eventType.shouldBe(IntegrationEventType.PERSON_REPORTED_ADJUDICATIONS_CHANGED)
    savedEvents[0].hmppsId.shouldBe(crn)
    savedEvents[0].url.shouldBe("https://localhost:8443/v1/persons/$crn/reported-adjudications")
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      HmppsDomainEventName.PrisonOffenderEvents.Prisoner.NonAssociationDetail.CHANGED,
      HmppsDomainEventName.NonAssociations.CREATED,
      HmppsDomainEventName.NonAssociations.AMENDED,
      HmppsDomainEventName.NonAssociations.CLOSED,
      HmppsDomainEventName.NonAssociations.DELETED,
    ],
  )
  fun `will process and save a non-association event SQS message`(eventType: String) {
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
    """
    val rawMessage = SqsNotificationGeneratingHelper().generateRawDomainEvent(eventType, message)
    sendDomainSqsMessage(rawMessage)

    Awaitility.await().until { repo.findAll().isNotEmpty() }
    val savedEvents = repo.findAll()
    savedEvents.size.shouldBe(1)
    savedEvents[0].eventType.shouldBe(IntegrationEventType.PRISONER_NON_ASSOCIATIONS_CHANGED)
    savedEvents[0].hmppsId.shouldBe(crn)
    savedEvents[0].url.shouldBe("https://localhost:8443/v1/prison/{prisonId}/prisoners/$crn/non-associations")
  }
}
