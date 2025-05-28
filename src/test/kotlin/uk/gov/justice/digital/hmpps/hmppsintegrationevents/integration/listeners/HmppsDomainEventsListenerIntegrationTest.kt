package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.listeners

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
}
