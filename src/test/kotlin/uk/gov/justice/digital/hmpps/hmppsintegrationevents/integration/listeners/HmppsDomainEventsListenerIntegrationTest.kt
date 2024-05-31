package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.listeners

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.awaitility.Awaitility
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockReset
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.HmppsDomainEventsListener
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IncomingEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.OutgoingEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.SqsIntegrationTestBase
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class HmppsDomainEventsListenerIntegrationTest : SqsIntegrationTestBase() {

  @SpyBean(reset = MockReset.BEFORE)
  lateinit var repo: EventNotificationRepository

  @Autowired
  lateinit var hmppsDomainEventsListener: HmppsDomainEventsListener

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
  fun `when processing an event which already exists in DB, will update instead of saving another`() {
    val timestampOne: ZonedDateTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
    val timestampTwo = timestampOne.plusMinutes(3)

    // Create test events
    val rawMessageAdded = SqsNotificationGeneratingHelper(timestampOne).generateRawGenericEvent(IncomingEventType.REGISTRATION_ADDED.value)
    val rawMessageUpdated = SqsNotificationGeneratingHelper(timestampTwo).generateRawGenericEvent(IncomingEventType.REGISTRATION_UPDATED.value)
    val rawMessageUpdatedTwo = SqsNotificationGeneratingHelper(timestampTwo).generateRawGenericEvent(IncomingEventType.REGISTRATION_UPDATED.value)

    // Create event
    sendDomainSqsMessage(rawMessageAdded)
    // Update it twice
    sendDomainSqsMessage(rawMessageUpdated)
    sendDomainSqsMessage(rawMessageUpdatedTwo)
    // Ensure that it's been updated twice instead of being saved
    await.atMost(10, TimeUnit.SECONDS).untilAsserted { Mockito.verify(repo, Mockito.atLeast(2)).updateLastModifiedDateTimeByHmppsIdAndEventType(any(), eq("X777776"), eq(OutgoingEventType.MAPPA_DETAIL_CHANGED)) }
    // Verify we've checked whether an event of this type has been checked for the initial event and the two subsequent updates
    Mockito.verify(repo, times(3)).existsByHmppsIdAndEventType("X777776", OutgoingEventType.MAPPA_DETAIL_CHANGED)
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
    payload.shouldBe(hmppsDomainEvent.toString())
    message.messageAttributes()["Error"]!!.stringValue().shouldBe("Unexpected event type some.other-event")
    val savedEvent = repo.findAll().firstOrNull()
    savedEvent.shouldBeNull()
  }

  @Test
  fun `will not process and save a domain event message with an unknown register type code`() {
    val rawMessage = SqsNotificationGeneratingHelper().generateRawRegistrationEvent(registerTypeCode = "OtherType")

    sendDomainSqsMessage(rawMessage)
    val savedEvent = repo.findAll().firstOrNull()

    savedEvent.shouldBeNull()
  }

  @Test
  fun `will not process and save a domain event message with no crn type and log to dead letter queue`() {
    val rawMessage = SqsNotificationGeneratingHelper().generateRawRegistrationEvent(identifiers = "[]")

    sendDomainSqsMessage(rawMessage)

    Awaitility.await().until { getNumberOfMessagesCurrentlyOndomainEventsDeadLetterQueue() == 1 }
    val deadLetterQueueMessage = geMessagesCurrentlyOnDomainEventsDeadLetterQueue()
    val message = deadLetterQueueMessage.messages().first()
    val payload = message.body()
    val hmppsDomainEvent: HmppsDomainEvent = objectMapper.readValue(rawMessage)
    payload.shouldBe(hmppsDomainEvent.toString())
    message.messageAttributes()["Error"]!!.stringValue().shouldBe("CRN could not be found in registration event message")
    val savedEvent = repo.findAll().firstOrNull()
    savedEvent.shouldBeNull()
  }
}
