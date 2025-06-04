package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("test")
class EventNotificationRepositoryTest {

  @Autowired
  private lateinit var eventNotificationRepository: EventNotificationRepository

  @BeforeEach
  fun setup() {
    eventNotificationRepository.deleteAll()
  }

  @Test
  fun `insert an event`() {
    val eventNotification = EventNotification(
      eventType = IntegrationEventType.MAPPA_DETAIL_CHANGED,
      hmppsId = "MockId",
      prisonId = "MKI",
      url = "MockUrl",
      lastModifiedDateTime = LocalDateTime.now().minusMinutes(6),
    )
    eventNotificationRepository.insertOrUpdate(eventNotification)

    val eventNotifications = eventNotificationRepository.findAll()
    assertThat(eventNotifications).hasSize(1)
    assertThat(eventNotifications[0].eventType).isEqualTo(eventNotification.eventType)
    assertThat(eventNotifications[0].hmppsId).isEqualTo(eventNotification.hmppsId)
    assertThat(eventNotifications[0].prisonId).isEqualTo(eventNotification.prisonId)
    assertThat(eventNotifications[0].url).isEqualTo(eventNotification.url)
    assertThat(eventNotifications[0].lastModifiedDateTime).isEqualTo(eventNotification.lastModifiedDateTime)
  }

  @Test
  fun `updates timestamp on an existing record`() {
    val eventNotification = EventNotification(
      eventType = IntegrationEventType.MAPPA_DETAIL_CHANGED,
      hmppsId = "MockId",
      prisonId = "MKI",
      url = "MockUrl",
      lastModifiedDateTime = LocalDateTime.now().minusMinutes(6),
    )
    eventNotificationRepository.insertOrUpdate(eventNotification)

    val eventNotifications = eventNotificationRepository.findAll()
    assertThat(eventNotifications).hasSize(1)

    val updatedEventNotification = eventNotification.copy(lastModifiedDateTime = LocalDateTime.now())
    eventNotificationRepository.insertOrUpdate(updatedEventNotification)

    val updatedEventNotifications = eventNotificationRepository.findAll()
    assertThat(updatedEventNotifications).hasSize(1)
    assertThat(updatedEventNotifications[0].lastModifiedDateTime).isEqualTo(updatedEventNotification.lastModifiedDateTime)
  }
}
