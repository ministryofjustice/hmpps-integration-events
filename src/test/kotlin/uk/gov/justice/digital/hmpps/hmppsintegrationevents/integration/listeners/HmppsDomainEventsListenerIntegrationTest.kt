package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.listeners

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.awaitility.Awaitility
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventTypes
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.wiremock.HMPPSAuthExtension
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.wiremock.ProbationIntegrationApiExtension
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@ExtendWith(ProbationIntegrationApiExtension::class, HMPPSAuthExtension::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class HmppsDomainEventsListenerIntegrationTest : SqsIntegrationTestBase() {

  @Autowired
  lateinit var repo: EventNotificationRepository

  @BeforeEach
  fun setup() {
    repo.deleteAll()
  }

  @Test
  fun `will process and save a valid domain event SQS message`() {
    val rawMessage = SqsNotificationGeneratingHelper().generateRawGenericEvent()
    sendDomainSqsMessage(rawMessage)

    Awaitility.await().until { repo.findAll().isNotEmpty() }
    val savedEvent = repo.findAll().firstOrNull()
    savedEvent.shouldNotBeNull()
  }

  @Test
  fun `will process and save a prisoner released event SQS message`() {
    ProbationIntegrationApiExtension.server.stubGetPersonIdentifier("mockNomsNumber","mockCrn")
    val rawMessage = SqsNotificationGeneratingHelper().generatePrisonerReleasedEvent()
    sendDomainSqsMessage(rawMessage)


    Awaitility.await().until { repo.findAll().isNotEmpty() }
    val savedEvent = repo.findAll().firstOrNull()
    savedEvent.shouldNotBeNull()
    savedEvent.eventType.shouldBe(IntegrationEventTypes.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE)
    savedEvent.hmppsId.shouldBe("mockCrn")
    savedEvent.url.shouldBe("https://localhost:8443/v1/persons/mockCrn/sentences/latest-key-dates-and-adjustments")
  }

  @Test
  fun `will not process a malformed domain event SQS Message and log to dead letter queue`() {
    sendDomainSqsMessage("BAD JSON")

    Awaitility.await().until { getNumberOfMessagesCurrentlyOndomainEventsDeadLetterQueue() == 1 }
    val deadLetterQueueMessage = geMessagesCurrentlyOnDomainEventsDeadLetterQueue()
    val message = deadLetterQueueMessage.messages().first()
    message.body().shouldBe("BAD JSON")
    message.messageAttributes()["Error"]!!.stringValue().shouldBe("Malformed event received. Could not parse JSON")
    val savedEvent = repo.findAll().firstOrNull()
    savedEvent.shouldBeNull()
  }

  @Test
  fun `will not process and save a domain event message with an unknown type and log to dead letter queue`() {
    val rawMessage = SqsNotificationGeneratingHelper().generateRawGenericEvent(eventTypeValue = "some.other-event")

    sendDomainSqsMessage(rawMessage)

    Awaitility.await().until { getNumberOfMessagesCurrentlyOndomainEventsDeadLetterQueue() == 1 }
    val deadLetterQueueMessage = geMessagesCurrentlyOnDomainEventsDeadLetterQueue()
    val message = deadLetterQueueMessage.messages().first()
    val payload = message.body()
    val hmppsDomainEvent: HmppsDomainEvent = objectMapper.readValue(rawMessage)
    payload.shouldBe(objectMapper.writeValueAsString(hmppsDomainEvent))
    message.messageAttributes()["Error"]!!.stringValue().shouldBe("Unexpected event type some.other-event")
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
    val hmppsDomainEvent: HmppsDomainEvent = objectMapper.readValue(rawMessage)
    payload.shouldBe(objectMapper.writeValueAsString(hmppsDomainEvent))
    message.messageAttributes()["Error"]!!.stringValue().shouldBe("CRN could not be found in registration event message")
    val savedEvent = repo.findAll().firstOrNull()
    savedEvent.shouldBeNull()
  }
}
