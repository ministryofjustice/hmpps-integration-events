package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway.ProbationIntegrationApiGateway
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.ASSESSMENT_SUMMARY_PRODUCED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.generateHmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.PersonExists
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventTypes
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime

@Configuration
@ActiveProfiles("test")
class HmppsDomainEventServiceROSHTest {

  private final val baseUrl = "https://dev.integration-api.hmpps.service.justice.gov.uk"

  private val repo = mockk<EventNotificationRepository>()
  private val deadLetterQueueService = mockk<DeadLetterQueueService>()
  private val probationIntegrationApiGateway = mockk<ProbationIntegrationApiGateway>()
  private val hmppsDomainEventService: HmppsDomainEventService = HmppsDomainEventService(repo = repo, deadLetterQueueService, probationIntegrationApiGateway, baseUrl)
  private val currentTime: LocalDateTime = LocalDateTime.now()
  private val crn = "X777776"

  @BeforeEach
  fun setup() {
    mockkStatic(LocalDateTime::class)
    every { LocalDateTime.now() } returns currentTime
    every { probationIntegrationApiGateway.getPersonExists(crn) } returns PersonExists(crn, true)
    every { repo.existsByHmppsIdAndEventType(any(), any()) } returns false
    every { repo.save(any()) } returnsArgument 0
  }

  @Test
  fun `will process and save a rosh notification`() {
    val message = ASSESSMENT_SUMMARY_PRODUCED

    val hmppsMessage = message.replace("\\", "")
    val event = generateHmppsDomainEvent("assessment.summary.produced", hmppsMessage)

    hmppsDomainEventService.execute(event, IntegrationEventTypes.RISK_OF_SERIOUS_HARM_CHANGED)

    verify(exactly = 1) {
      repo.save(
        EventNotification(
          eventType = IntegrationEventTypes.RISK_OF_SERIOUS_HARM_CHANGED,
          hmppsId = crn,
          url = "$baseUrl/v1/persons/X777776/risks/serious-harm",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }
}
