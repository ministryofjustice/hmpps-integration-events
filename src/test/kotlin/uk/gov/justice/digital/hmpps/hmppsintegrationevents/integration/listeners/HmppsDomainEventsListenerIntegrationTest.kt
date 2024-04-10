package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.listeners

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.HmppsDomainEventsListener
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DomainEventsService

@ExtendWith(MockKExtension::class)
class HmppsDomainEventsListenerIntegrationTest : SqsIntegrationTestBase() {

  private val repo = mockk<EventNotificationRepository>()

  private final val service = DomainEventsService(repo)

  val hmppsDomainEventsListener = HmppsDomainEventsListener(service)

  @Test
  fun `will process and save a mapps domain registration event message`() {
    val rawMessage = SqsNotificationGeneratingHelper().generate()

    every { repo.save(any()) } returnsArgument 0

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(exactly = 1) { repo.save(any()) }
  }

  @Test
  fun `will not process and save an unknown event message`() {
    val rawMessage = SqsNotificationGeneratingHelper().generate(eventTypeValue = "some.other-event")

    every { repo.save(any()) } returnsArgument 0

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(exactly = 0) { repo.save(any()) }
  }
}
