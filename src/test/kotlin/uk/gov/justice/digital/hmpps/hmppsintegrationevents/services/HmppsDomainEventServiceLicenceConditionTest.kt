package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway.ProbationIntegrationApiGateway
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.generateHmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventTypes
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime

@Configuration
@ActiveProfiles("test")
class HmppsDomainEventServiceLicenceConditionTest {

  private final val baseUrl = "https://dev.integration-api.hmpps.service.justice.gov.uk"

  private val repo = mockk<EventNotificationRepository>()
  private val deadLetterQueueService = mockk<DeadLetterQueueService>()
  private val probationIntegrationApiGateway = mockk<ProbationIntegrationApiGateway>()
  private val hmppsDomainEventService: HmppsDomainEventService = HmppsDomainEventService(repo = repo, deadLetterQueueService, probationIntegrationApiGateway, baseUrl)
  private val currentTime: LocalDateTime = LocalDateTime.now()
  private val crn = "X777776"
  private val nomsNumber = "A1234BC"

  @BeforeEach
  fun setup() {
    mockkStatic(LocalDateTime::class)
    every { LocalDateTime.now() } returns currentTime
    every { repo.existsByHmppsIdAndEventType(any(), any()) } returns false
    every { repo.save(any()) } returnsArgument 0
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "create-and-vary-a-licence.licence.activated, 99059",
      "create-and-vary-a-licence.licence.inactivated, 90386",
    ],
  )
  fun `will process and save a pnd alert for person alert changed event `(eventType: String, licenceId: String) {
    val message = """
      {\"eventType\":\"$eventType\",\"additionalInformation\":{\"licenceId\":\"99059\"},\"detailUrl\":\"https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk/public/licences/id/$licenceId\",\"version\":1,\"occurredAt\":\"2024-08-14T16:42:13.725721689+01:00\",\"description\":\"Licence activated for Licence ID $licenceId\",\"personReference\":{\"identifiers\":[{\"type\":\"CRN\",\"value\":\"$crn\"},{\"type\":\"NOMS\",\"value\":\"$nomsNumber\"}]}}
    """.trimIndent()

    val hmppsMessage = message.replace("\\", "")
    val event = generateHmppsDomainEvent(eventType, hmppsMessage)

    hmppsDomainEventService.execute(event, IntegrationEventTypes.LICENCE_CONDITION_CHANGES)

    verify(exactly = 1) {
      repo.save(
        EventNotification(
          eventType = IntegrationEventTypes.LICENCE_CONDITION_CHANGES,
          hmppsId = "X777776",
          url = "$baseUrl/v1/persons/X777776/licences/conditions",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }
}
