package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doNothing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.SubscriberService
import java.time.LocalDateTime
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DatabaseConstraintTest {

  @MockitoBean
  private lateinit var subscriberService: SubscriberService

  @Autowired
  private lateinit var eventRepository: EventNotificationRepository

  fun makeEvent(url: String): EventNotification = EventNotification(
    eventType = IntegrationEventType.MAPPA_DETAIL_CHANGED,
    hmppsId = "MockId",
    prisonId = "MKI",
    url = url,
    lastModifiedDateTime = LocalDateTime.now().minusHours(25),
  )

  @BeforeEach
  fun setUp() {
    doNothing().`when`(subscriberService).checkSubscriberFilterList()
    eventRepository.deleteAll()
  }

  @Test
  fun `does not create a new record when url, event type and status are all the same`() {
    assertThat(eventRepository.count()).isEqualTo(0)
    eventRepository.insertOrUpdate(makeEvent("MockUrl1"))
    assertThat(eventRepository.count()).isEqualTo(1)
    eventRepository.insertOrUpdate(makeEvent("MockUrl1"))
    assertThat(eventRepository.count()).isEqualTo(1)
  }

  @Test
  fun `creates a new record when url, event type are same, but status is different`() {
    assertThat(eventRepository.count()).isEqualTo(0)
    val claimId = UUID.randomUUID().toString()
    eventRepository.insertOrUpdate(makeEvent("MockUrl1"))
    assertThat(eventRepository.count()).isEqualTo(1)
    // Move status to processing
    eventRepository.setProcessing(LocalDateTime.now().minusMinutes(5), claimId)
    eventRepository.insertOrUpdate(makeEvent("MockUrl1"))
    assertThat(eventRepository.count()).isEqualTo(2)
  }
}
