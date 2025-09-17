package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.ASSESSMENT_SUMMARY_PRODUCED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.generateHmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime

@Configuration
@ActiveProfiles("test")
class HmppsDomainEventServiceROSHTest {

  private final val baseUrl = "https://dev.integration-api.hmpps.service.justice.gov.uk"

  private val eventNotificationRepository = mockk<EventNotificationRepository>()
  private val deadLetterQueueService = mockk<DeadLetterQueueService>()
  private val domainEventIdentitiesResolver = mockk<DomainEventIdentitiesResolver>()
  private val hmppsDomainEventService: HmppsDomainEventService = HmppsDomainEventService(
    eventNotificationRepository,
    deadLetterQueueService,
    domainEventIdentitiesResolver,
    baseUrl,
  )
  private val currentTime: LocalDateTime = LocalDateTime.now()
  private val hmppsId = "AA1234A"

  @BeforeEach
  fun setup() {
    mockkStatic(LocalDateTime::class)
    every { domainEventIdentitiesResolver.getHmppsId(any()) } returns hmppsId
    every { domainEventIdentitiesResolver.getPrisonId(any()) } returns null
    every { LocalDateTime.now() } returns currentTime

    every { eventNotificationRepository.insertOrUpdate(any()) } returnsArgument 0
  }

  @Test
  fun `will process and save a rosh notification`() {
    val message = ASSESSMENT_SUMMARY_PRODUCED

    val hmppsMessage = message.replace("\\", "")
    val event = generateHmppsDomainEvent("assessment.summary.produced", hmppsMessage)

    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.RISK_OF_SERIOUS_HARM_CHANGED))

    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.RISK_OF_SERIOUS_HARM_CHANGED,
          hmppsId = hmppsId,
          url = "$baseUrl/v1/persons/$hmppsId/risks/serious-harm",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }
}
