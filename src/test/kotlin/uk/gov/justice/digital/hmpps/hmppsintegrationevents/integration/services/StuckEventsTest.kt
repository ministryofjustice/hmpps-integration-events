package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.AdditionalAnswers
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventStatus
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.IntegrationEventTopicService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.StateEventNotifierService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.SubscriberService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.TelemetryService
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = ["notifier.schedule.rate=10000"])
class StuckEventsTest {

  @Autowired
  private lateinit var eventNotifierService: StateEventNotifierService

  @MockitoBean
  private lateinit var integrationEventTopicService: IntegrationEventTopicService

  @MockitoBean
  private lateinit var subscriberService: SubscriberService

  @MockitoSpyBean
  private lateinit var eventNotificationRepository: EventNotificationRepository

  @MockitoBean
  private lateinit var telemetryService: TelemetryService

  @BeforeEach
  fun setup() {
    Mockito.doNothing().`when`(eventNotificationRepository).setProcessing(any(), any(), any())
    whenever(integrationEventTopicService.sendEvent(any())).thenAnswer(
      AdditionalAnswers.answersWithDelay(
        300,
        { "SUCCESS" },
      ),
    )
    doNothing().`when`(subscriberService).checkSubscriberFilterList()
    eventNotificationRepository.deleteAll()

    val baseDate = LocalDateTime.of(2025, 8, 12, 0, 0)
    eventNotificationRepository.save(makeEvent("MockUrl11", "claimId1", IntegrationEventStatus.PROCESSING, baseDate.plusDays(2).plusHours(1)))
    eventNotificationRepository.save(makeEvent("MockUrl12", "claimId1", IntegrationEventStatus.PENDING, baseDate.plusDays(1).plusHours(2)))
    eventNotificationRepository.save(makeEvent("MockUrl13", "claimId1", IntegrationEventStatus.PROCESSING, baseDate.plusDays(3).plusHours(3)))
    eventNotificationRepository.save(makeEvent("MockUrl14", "claimId2", IntegrationEventStatus.PROCESSING, baseDate))
    eventNotificationRepository.save(makeEvent("MockUrl15", "claimId2", IntegrationEventStatus.PROCESSING, baseDate.plusDays(2).plusHours(4)))
    eventNotificationRepository.save(makeEvent("MockUrl16", "claimId3", IntegrationEventStatus.PENDING, baseDate.plusDays(-1).plusHours(5)))
  }

  fun makeEvent(
    url: String,
    claimId: String? = null,
    status: IntegrationEventStatus = IntegrationEventStatus.PENDING,
    lastModifiedDateTime: LocalDateTime,
  ): EventNotification = EventNotification(
    eventType = IntegrationEventType.MAPPA_DETAIL_CHANGED,
    hmppsId = "MockId",
    prisonId = "MKI",
    url = url,
    claimId = claimId,
    status = status,
    lastModifiedDateTime = lastModifiedDateTime,
  )

  @Test
  fun `Stuck messages are found in the database`() {
    val expectedExceptionMessage = """
      stuck events with status PROCESSING
    """.trimIndent()
    val message = argumentCaptor<String>()
    val thread1 = Thread { eventNotifierService.sentNotifications() }
    thread1.start()
    verify(telemetryService, timeout(10_000).atLeast(1)).captureMessage(message.capture())
    assertThat(message.firstValue).contains(expectedExceptionMessage)
  }
}
