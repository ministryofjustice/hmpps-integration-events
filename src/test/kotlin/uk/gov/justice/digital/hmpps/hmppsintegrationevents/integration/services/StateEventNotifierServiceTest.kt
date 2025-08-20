package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.services

import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.sentry.Sentry
import org.awaitility.Awaitility
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DeleteProcessedService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.IntegrationEventTopicService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.StateEventNotifierService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.SubscriberService
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@SpringBootTest(properties = ["feature-flag.event-state-management=true"])
@ActiveProfiles("test")
class StateEventNotifierServiceTest {

  @MockitoBean
  private lateinit var integrationEventTopicService: IntegrationEventTopicService

  @MockitoBean
  private lateinit var subscriberService: SubscriberService

  @Autowired
  private lateinit var eventNotifierService: StateEventNotifierService

  @Autowired
  private lateinit var deleteProcessedService: DeleteProcessedService

  @Autowired
  private lateinit var eventNotificationRepository: EventNotificationRepository

  @Autowired
  private lateinit var threadPoolTaskExecutor: ThreadPoolTaskExecutor

  @BeforeEach
  fun setup() {
    mockkStatic(Sentry::class)
    // Stop the scheduled task executor, we are going to schedule the task manually in this test
    threadPoolTaskExecutor.stop()

    doNothing().`when`(integrationEventTopicService).sendEvent(any())
    doNothing().`when`(subscriberService).checkSubscriberFilterList()
    eventNotificationRepository.deleteAll()
    eventNotificationRepository.save(getEvent("MockUrl1"))
    eventNotificationRepository.save(getEvent("MockUrl2"))
    eventNotificationRepository.save(getEvent("MockUrl3"))
    eventNotificationRepository.save(getEvent("MockUrl4"))
    eventNotificationRepository.save(getEvent("MockUrl5"))
  }

  @BeforeEach
  fun teardown() {
    unmockkStatic(Sentry::class)
  }
  fun getEvent(url: String): EventNotification = EventNotification(
    eventType = IntegrationEventType.MAPPA_DETAIL_CHANGED,
    hmppsId = "MockId",
    prisonId = "MKI",
    url = url,
    lastModifiedDateTime = LocalDateTime.now().minusHours(25),
  )

  @Test
  fun `Concurrent Event Notifier services reads the DB records and deletes them without any exceptions`() {
    val thread1 = Thread { eventNotifierService.sentNotifications() }
    val thread2 = Thread { eventNotifierService.sentNotifications() }
    val deleteThread1 = Thread { deleteProcessedService.deleteProcessedEvents() }
    val deleteThread2 = Thread { deleteProcessedService.deleteProcessedEvents() }
    thread1.start()
    thread2.start()
    deleteThread1.start()
    deleteThread2.start()
    Awaitility.await().until { eventNotificationRepository.findAll().isEmpty() }
    io.mockk.verify(exactly = 0) { Sentry.captureException(any()) }
  }
}
