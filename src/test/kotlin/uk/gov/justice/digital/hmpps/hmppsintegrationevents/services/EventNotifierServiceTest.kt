package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.EventTypeValue
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.LocalDateTime
import software.amazon.awssdk.services.sns.model.MessageAttributeValue as snsMessageAttributeValue
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue as sqsMessageAttributeValue

@ActiveProfiles("test")
@JsonTest
class EventNotifierServiceTest(@Autowired private val objectMapper: ObjectMapper) {

  lateinit var emitter: EventNotifierService
  val hmppsQueueService: HmppsQueueService = mock()
  val hmppsEventSnsClient: SnsAsyncClient = mock()

  val hmppsEventSqsClient: SqsAsyncClient = mock()
  val hmppsEventDLSqsClient: SqsAsyncClient = mock()
  val eventRepository: EventNotificationRepository = mock()

  @BeforeEach
  fun setUp() {
    Mockito.reset(eventRepository)
    whenever(hmppsQueueService.findByTopicId("integrationeventtopic"))
      .thenReturn(HmppsTopic("integrationeventtopic", "sometopicarn", hmppsEventSnsClient))
    whenever(hmppsQueueService.findByQueueId("prisoner"))
      .thenReturn(HmppsQueue("prisoner", hmppsEventSqsClient, "hmpps_integrations_events_queue", hmppsEventDLSqsClient, "hmpps_integrations_events_queue_dlq"))

    emitter = EventNotifierService(hmppsQueueService, objectMapper, eventRepository)
  }

  @Test
  fun `No event published when repository return no event notifications`() {
    whenever(eventRepository.findAllWithLastModifiedDateTimeBefore(any())).thenReturn(emptyList())

    emitter.sentNotifications()

    verifyNoInteractions(hmppsEventSnsClient)
  }

  @Test
  fun `Event published for event notification in database`() {
    val event = EventNotification(123, "hmppsId", EventTypeValue.ADDRESS_CHANGE, "mockUrl", LocalDateTime.now())
    whenever(eventRepository.findAllWithLastModifiedDateTimeBefore(any())).thenReturn(listOf(event))

    emitter.sentNotifications()

    argumentCaptor<PublishRequest>().apply {
      verify(hmppsEventSnsClient, times(1)).publish(capture())
      val payload = firstValue.message()
      val messageAttributes = firstValue.messageAttributes()
      assertThatJson(payload).node("eventType").isEqualTo(event.eventType.name)
      assertThatJson(payload).node("hmppsId").isEqualTo(event.hmppsId)
      assertThatJson(payload).node("url").isEqualTo(event.url)
      Assertions.assertThat(messageAttributes["eventType"])
        .isEqualTo(snsMessageAttributeValue.builder().stringValue(event.eventType.name).dataType("String").build())
    }
  }

  @Test
  fun `Remove event notification after event processed`() {
    val event = EventNotification(123, "hmppsId", EventTypeValue.ADDRESS_CHANGE, "mockUrl", LocalDateTime.now())
    whenever(eventRepository.findAllWithLastModifiedDateTimeBefore(any())).thenReturn(listOf(event))

    emitter.sentNotifications()
    verify(eventRepository, times(1)).deleteById(123)
  }

  @Test
  fun `Put event into dlq if failed to publish message and remove entity from database`() {
    val event = EventNotification(123, "hmppsId", EventTypeValue.ADDRESS_CHANGE, "mockUrl", LocalDateTime.now())
    whenever(eventRepository.findAllWithLastModifiedDateTimeBefore(any())).thenReturn(listOf(event))
    whenever(hmppsEventSnsClient.publish(any<PublishRequest>())).thenThrow(RuntimeException("MockError"))
    emitter.sentNotifications()
    argumentCaptor<SendMessageRequest>().apply {
      verify(hmppsEventDLSqsClient, times(1)).sendMessage(capture())
      val messageAttributes = firstValue.messageAttributes()
      val payload = firstValue.messageBody()
      assertThatJson(payload).node("eventType").isEqualTo(event.eventType.name)
      assertThatJson(payload).node("hmppsId").isEqualTo(event.hmppsId)
      assertThatJson(payload).node("url").isEqualTo(event.url)
      Assertions.assertThat(messageAttributes["Error"])
        .isEqualTo(sqsMessageAttributeValue.builder().stringValue("MockError").dataType("String").build())

      verify(eventRepository, times(1)).deleteById(123)
    }
  }
}
