package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.services

import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.sentry.Sentry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.AdditionalAnswers
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
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

  @MockitoSpyBean
  private lateinit var threadPoolTaskExecutor: ThreadPoolTaskExecutor

  @MockitoSpyBean
  private lateinit var threadPoolTaskScheduler: ThreadPoolTaskScheduler

  @BeforeEach
  fun setup() {
    mockkStatic(Sentry::class)
    Mockito.doNothing().`when`(eventNotificationRepository).setProcessing(any(), any(), any())
    // Stop the scheduled task executor, we are going to schedule the task manually in this test
    threadPoolTaskExecutor.stop()
    threadPoolTaskScheduler.stop()
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

  @AfterEach
  fun teardown() {
    unmockkStatic(Sentry::class)
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
    val message = slot<String>()
    val thread1 = Thread { eventNotifierService.sentNotifications() }
    thread1.start()
    io.mockk.verify(atLeast = 1, timeout = 10000) { Sentry.captureMessage(capture(message)) }
    assertThat(message.captured).contains(expectedExceptionMessage)
  }
}
