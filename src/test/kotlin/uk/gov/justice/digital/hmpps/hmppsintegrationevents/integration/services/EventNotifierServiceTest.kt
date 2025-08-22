package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.services

import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.sentry.Sentry
import org.awaitility.Awaitility
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.EventNotifierService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.IntegrationEventTopicService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.SubscriberService
import java.time.LocalDateTime


@ExtendWith(SpringExtension::class)
@SpringBootTest(properties = ["feature-flag.event-state-management=false"])
@ActiveProfiles("test")
class EventNotifierServiceTest {

  @MockitoBean
  private lateinit var integrationEventTopicService: IntegrationEventTopicService

  @MockitoBean
  private lateinit var subscriberService: SubscriberService

  @Autowired
  private lateinit var eventNotifierService: EventNotifierService

  @Autowired
  private lateinit var eventNotificationRepository: EventNotificationRepository

  @Autowired
  private lateinit var threadPoolTaskExecutor: ThreadPoolTaskExecutor

  @Autowired
  private lateinit var threadPoolTaskScheduler: ThreadPoolTaskScheduler

  @BeforeEach
  fun setup() {
    mockkStatic(Sentry::class)
    // Stop the scheduled task executor, we are going to schedule the task manually in this test
    threadPoolTaskExecutor.stop()
    threadPoolTaskScheduler.stop()
    whenever(integrationEventTopicService.sendEvent(any())).thenAnswer(
      AdditionalAnswers.answersWithDelay(
        300,
      ) { "SUCCESS" },
    )
    doNothing().`when`(subscriberService).checkSubscriberFilterList()
    eventNotificationRepository.deleteAll()
    eventNotificationRepository.save(makeEvent("MockUrl1"))
    eventNotificationRepository.save(makeEvent("MockUrl2"))
    eventNotificationRepository.save(makeEvent("MockUrl3"))
    eventNotificationRepository.save(makeEvent("MockUrl4"))
    eventNotificationRepository.save(makeEvent("MockUrl5"))
  }

  @AfterEach
  fun teardown() {
    unmockkStatic(Sentry::class)
  }
  fun makeEvent(url: String): EventNotification = EventNotification(
    eventType = IntegrationEventType.MAPPA_DETAIL_CHANGED,
    hmppsId = "MockId",
    prisonId = "MKI",
    url = url,
    lastModifiedDateTime = LocalDateTime.now().minusMinutes(60),
  )

  @Test
  fun `Concurrent Event Notifier services reads the DB records and deletes them with exceptions`() {
    val thread1 = Thread { eventNotifierService.sentNotifications() }
    val thread2 = Thread { eventNotifierService.sentNotifications() }
    thread1.start()
    Thread.sleep(10)
    eventNotificationRepository.save(makeEvent("MockUrl6"))
    eventNotificationRepository.save(makeEvent("MockUrl7"))
    eventNotificationRepository.save(makeEvent("MockUrl8"))
    eventNotificationRepository.save(makeEvent("MockUrl9"))
    eventNotificationRepository.save(makeEvent("MockUrl10"))
    thread2.start()
    Awaitility.await().until { eventNotificationRepository.findAll().isEmpty() }
    io.mockk.verify { Sentry.captureException(any()) }
  }
}
