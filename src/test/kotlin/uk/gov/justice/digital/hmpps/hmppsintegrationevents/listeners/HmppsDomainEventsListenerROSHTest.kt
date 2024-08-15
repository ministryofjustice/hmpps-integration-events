package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.ASSESSMENT_SUMMARY_PRODUCED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.generateHmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventTypes
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DeadLetterQueueService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.HmppsDomainEventService

@ActiveProfiles("test")
@JsonTest
class HmppsDomainEventsListenerROSHTest {
  private val hmppsDomainEventService = mockk<HmppsDomainEventService>()
  private val deadLetterQueueService = mockk<DeadLetterQueueService>()

  private val hmppsDomainEventsListener: HmppsDomainEventsListener = HmppsDomainEventsListener(hmppsDomainEventService, deadLetterQueueService)

  private val crn = "X777776"

  @Test
  fun `will process and save a rosh notification`() {
    val eventType = "assessment.summary.produced"

    val message = ASSESSMENT_SUMMARY_PRODUCED

    val hmppsMessage = message.replace("\\", "")
    val payload = DomainEvents.generateDomainEvent(eventType, message)
    val hmppsDomainEvent = generateHmppsDomainEvent(eventType, hmppsMessage)

    every { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventTypes.RISK_OF_SERIOUS_HARM_CHANGED) } just runs

    hmppsDomainEventsListener.onDomainEvent(payload)

    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventTypes.RISK_OF_SERIOUS_HARM_CHANGED) }
  }
}
