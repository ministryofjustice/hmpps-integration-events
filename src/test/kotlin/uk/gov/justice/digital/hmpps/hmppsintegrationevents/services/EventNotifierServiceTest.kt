package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.EventTypeValue
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime

@ActiveProfiles("test")
@JsonTest
class EventNotifierServiceTest() {

  lateinit var emitter: EventNotifierService
  val integrationEventTopicService: IntegrationEventTopicService = mock()

  val eventRepository: EventNotificationRepository = mock()

  val currentTime = LocalDateTime.now()

  @BeforeEach
  fun setUp() {
    Mockito.reset(eventRepository)

    emitter = EventNotifierService(integrationEventTopicService, eventRepository)
  }

  @Test
  fun `No event published when repository return no event notifications`() {
    whenever(eventRepository.findAllWithLastModifiedDateTimeBefore(any())).thenReturn(emptyList())
    emitter.sentNotifications()
    verifyNoInteractions(integrationEventTopicService)
  }

  @Test
  fun `Event published for event notification in database`() {
    val event = EventNotification(123, "hmppsId", EventTypeValue.ADDRESS_CHANGE, "mockUrl", currentTime)
    whenever(eventRepository.findAllWithLastModifiedDateTimeBefore(any())).thenReturn(listOf(event))

    emitter.sentNotifications()

    argumentCaptor<EventNotification>().apply {
      verify(integrationEventTopicService, times(1)).sendEvent(capture())
      Assertions.assertThat(firstValue.eventType).isEqualTo(event.eventType)
      Assertions.assertThat(firstValue.hmppsId).isEqualTo(event.hmppsId)
      Assertions.assertThat(firstValue.url).isEqualTo(event.url)
    }
  }

  @Test
  fun `Remove event notification after event processed`() {
    val event = EventNotification(123, "hmppsId", EventTypeValue.ADDRESS_CHANGE, "mockUrl", currentTime)
    whenever(eventRepository.findAllWithLastModifiedDateTimeBefore(any())).thenReturn(listOf(event))

    emitter.sentNotifications()
    verify(eventRepository, times(1)).deleteById(123)
  }
}
