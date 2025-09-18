package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.lang.IllegalStateException
import java.time.LocalDateTime
import java.time.ZoneId

@Configuration
@ActiveProfiles("test")
class HmppsDomainEventServicePrisonerMergedTest {

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
  private val zonedCurrentDateTime = currentTime.atZone(ZoneId.systemDefault())
  private val hmppsId = "hmpps-1234"

  @BeforeEach
  fun setup() {
    clearAllMocks()

    mockkStatic(LocalDateTime::class)
    every { domainEventIdentitiesResolver.getHmppsId(any()) } returns hmppsId
    every { domainEventIdentitiesResolver.getPrisonId(any()) } returns null
    every { LocalDateTime.now() } returns currentTime

    every { eventNotificationRepository.insertOrUpdate(any()) } returnsArgument 0
  }

  @Test
  fun `will process and save a prisoner merged notification`() {
    val removedNomisNumber = "AA0001A"
    val updatedNomisNumber = "AA0002A"

    val event = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsMergedDomainEvent(nomisNumber = updatedNomisNumber, removedNomisNumber = removedNomisNumber)

    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.PRISONER_MERGE, IntegrationEventType.PERSON_STATUS_CHANGED))

    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.PRISONER_MERGE,
          hmppsId = removedNomisNumber,
          url = "$baseUrl/v1/persons/$removedNomisNumber",
          lastModifiedDateTime = currentTime,
        ),
      )
    }

    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.PERSON_STATUS_CHANGED,
          hmppsId = hmppsId,
          url = "$baseUrl/v1/persons/$hmppsId",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

  @Test
  fun `will throw an exception if the removed nomis number is missing`() {
    val removedNomisNumber = null
    val updatedNomisNumber = "AA0002A"

    val event = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsMergedDomainEvent(nomisNumber = updatedNomisNumber, removedNomisNumber = removedNomisNumber)

    assertThrows<IllegalStateException> {
      hmppsDomainEventService.execute(event, listOf(IntegrationEventType.PRISONER_MERGE, IntegrationEventType.PERSON_STATUS_CHANGED))
    }

    verify(exactly = 0) {
      eventNotificationRepository.insertOrUpdate(any())
    }
  }
}
