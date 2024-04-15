package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.listeners

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.HmppsDomainEventsListener
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.SqsIntegrationTestBase

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class HmppsDomainEventsListenerIntegrationTest : SqsIntegrationTestBase() {

  @Autowired
  lateinit var repo: EventNotificationRepository

  @Autowired
  lateinit var hmppsDomainEventsListener: HmppsDomainEventsListener

  @Test
  fun `will process and save a valid domain event SQS message`() {
    val rawMessage = SqsNotificationGeneratingHelper().generateGenericEvent()

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    val savedEvent = repo.findAll().firstOrNull()

    Assertions.assertNotNull(savedEvent)
  }

  @Test
  fun `will not process and save a malformed domain event SQS Message`() {
    hmppsDomainEventsListener.onDomainEvent("BAD JSON")

    val savedEvent = repo.findAll().firstOrNull()

    Assertions.assertNull(savedEvent)
  }

  @Test
  fun `will not process and save a domain event message with an unknown type`() {
    val rawMessage = SqsNotificationGeneratingHelper().generateGenericEvent(eventTypeValue = "some.other-event")

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    val savedEvent = repo.findAll().firstOrNull()

    Assertions.assertNull(savedEvent)
  }
}
