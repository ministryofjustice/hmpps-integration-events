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
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.generateHmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventTypes
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DeadLetterQueueService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.HmppsDomainEventService

@ActiveProfiles("test")
@JsonTest
class HmppsDomainEventsListenerPLPReviewUpdatedTest {
  private val hmppsDomainEventService = mockk<HmppsDomainEventService>()
  private val deadLetterQueueService = mockk<DeadLetterQueueService>()

  private val hmppsDomainEventsListener: HmppsDomainEventsListener = HmppsDomainEventsListener(hmppsDomainEventService, deadLetterQueueService)

  private val nomsNumber = "A1234BC"

  private val eventType = "plp.review-schedule.updated"

  @Test
  fun `will process a plp review schedule updated notification`() {
    val message = """
      { \"eventType\": \"$eventType\",  \"description\": \"A prisoner learning plan review schedule created or amended\",  \"detailUrl\": \"http://localhost:8080/inductions/$nomsNumber/review-schedule\",  \"occurredAt\": \"2024-08-08T09:07:55\",  \"personReference\": {    \"identifiers\": [      {        \"type\": \"NOMS\",        \"value\": \"$nomsNumber\"      }    ]  }}
    """.trimIndent()

    val hmppsMessage = message.replace("\\", "")
    val payload = DomainEvents.generateDomainEvent(eventType, message)
    val hmppsDomainEvent = generateHmppsDomainEvent(eventType, hmppsMessage)

    every { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventTypes.PLP_REVIEW_SCHEDULE_CHANGED) } just runs

    hmppsDomainEventsListener.onDomainEvent(payload)

    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventTypes.PLP_REVIEW_SCHEDULE_CHANGED) }
  }
}
