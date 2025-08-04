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
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.PersonExists
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.registration.HmppsDomainEventMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime

@Configuration
@ActiveProfiles("test")
class HmppsDomainEventServiceLicenceConditionTest {

  private final val baseUrl = "https://dev.integration-api.hmpps.service.justice.gov.uk"

  private val eventNotificationRepository = mockk<EventNotificationRepository>()
  private val deadLetterQueueService = mockk<DeadLetterQueueService>()
  private val probationIntegrationApiGateway = mockk<ProbationIntegrationApiGateway>()
  private val getPrisonIdService = mockk<GetPrisonIdService>()
  private val integrationEventCreationStrategyProvider = mockk<IntegrationEventCreationStrategyProvider>()
  private val defaultEventCreationStrategy = mockk<DefaultEventCreationStrategy>()

  private val hmppsDomainEventService: HmppsDomainEventService = HmppsDomainEventService(
    eventNotificationRepository,
    deadLetterQueueService,
    integrationEventCreationStrategyProvider,
    baseUrl,
  )
  private val currentTime: LocalDateTime = LocalDateTime.now()
  private val crn = "X777776"
  private val nomsNumber = "A1234BC"

  @BeforeEach
  fun setup() {
    mockkStatic(LocalDateTime::class)
    every { LocalDateTime.now() } returns currentTime
    every { probationIntegrationApiGateway.getPersonExists(crn) } returns PersonExists(crn, true)
    every { eventNotificationRepository.existsByHmppsIdAndEventType(any(), any()) } returns false
    every { eventNotificationRepository.insertOrUpdate(any()) } returnsArgument 0

    every { getPrisonIdService.execute(nomsNumber) } returns null
    every { integrationEventCreationStrategyProvider.forEventType(any()) } returns defaultEventCreationStrategy

    every { defaultEventCreationStrategy.createNotifications(any(), any(), any()) } answers {
      val hmppsDomainEventMessage = arg<HmppsDomainEventMessage>(0)
      val integrationEventType = arg<IntegrationEventType>(1)
      val baseUrl = arg<String>(2)

      val additionalInfo = hmppsDomainEventMessage.additionalInformation

      listOf(
        EventNotification(
          eventType = integrationEventType,
          hmppsId = crn,
          url = "$baseUrl/${integrationEventType.path(crn, null, additionalInfo)}",
          lastModifiedDateTime = LocalDateTime.now(),
        ),
      )
    }
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "create-and-vary-a-licence.licence.activated, 99059",
      "create-and-vary-a-licence.licence.inactivated, 90386",
    ],
  )
  fun `will process and save a licence notification`(eventType: String, licenceId: String) {
    val message = """
      {\"eventType\":\"$eventType\",\"additionalInformation\":{\"licenceId\":\"99059\"},\"detailUrl\":\"https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk/public/licences/id/$licenceId\",\"version\":1,\"occurredAt\":\"2024-08-14T16:42:13.725721689+01:00\",\"description\":\"Licence activated for Licence ID $licenceId\",\"personReference\":{\"identifiers\":[{\"type\":\"CRN\",\"value\":\"$crn\"},{\"type\":\"NOMS\",\"value\":\"$nomsNumber\"}]}}
    """.trimIndent()

    val hmppsMessage = message.replace("\\", "")
    val event = generateHmppsDomainEvent(eventType, hmppsMessage)

    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.LICENCE_CONDITION_CHANGED))

    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.LICENCE_CONDITION_CHANGED,
          hmppsId = "X777776",
          url = "$baseUrl/v1/persons/X777776/licences/conditions",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }
}
