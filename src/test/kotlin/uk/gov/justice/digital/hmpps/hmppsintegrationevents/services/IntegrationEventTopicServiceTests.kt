package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import net.javacrumbs.jsonunit.assertj.JsonAssertions
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicResponse
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import software.amazon.awssdk.services.sns.model.SetSubscriptionAttributesRequest
import software.amazon.awssdk.services.sns.model.Subscription
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

@ActiveProfiles("test")
@JsonTest
class IntegrationEventTopicServiceTests(@Autowired private val objectMapper: ObjectMapper) {

  val hmppsQueueService: HmppsQueueService = mock()
  val hmppsEventSnsClient: SnsAsyncClient = mock()
  val mockQueue: HmppsQueue = mock()
  private lateinit var service: IntegrationEventTopicService
  val currentTime = LocalDateTime.now()

  @BeforeEach
  fun setUp() {
    whenever(hmppsQueueService.findByTopicId("integrationeventtopic"))
      .thenReturn(HmppsTopic("integrationeventtopic", "sometopicarn", hmppsEventSnsClient))
    whenever(hmppsQueueService.findByQueueId("mockQueue")).thenReturn(mockQueue)
    whenever(mockQueue.queueArn).thenReturn("mockARN")
    service = IntegrationEventTopicService(hmppsQueueService, objectMapper)
  }

  @Test
  fun `Publish Event `() {
    val event = EventNotification(123, "hmppsId", IntegrationEventType.MAPPA_DETAIL_CHANGED, "mockUrl", currentTime)

    val response = PublishResponse
      .builder()
      .messageId("123")
      .build()

    whenever(hmppsEventSnsClient.publish(any<PublishRequest>())).thenReturn(CompletableFuture.completedFuture(response))
    service.sendEvent(event)

    argumentCaptor<PublishRequest>().apply {
      verify(hmppsEventSnsClient, times(1)).publish(capture())
      val payload = firstValue.message()
      val messageAttributes = firstValue.messageAttributes()
      JsonAssertions.assertThatJson(payload).node("eventType").isEqualTo(event.eventType.name)
      JsonAssertions.assertThatJson(payload).node("hmppsId").isEqualTo(event.hmppsId)
      JsonAssertions.assertThatJson(payload).node("url").isEqualTo(event.url)
      Assertions.assertThat(messageAttributes["eventType"])
        .isEqualTo(MessageAttributeValue.builder().stringValue(event.eventType.name).dataType("String").build())
    }
  }

  @Test
  fun `Update Subscription Attributes`() {
    val mockSubs = listOf(Subscription.builder().protocol("sqs").endpoint("mockARN").subscriptionArn("mockSubscriptionArn").build())
    whenever(hmppsEventSnsClient.listSubscriptionsByTopic(any<ListSubscriptionsByTopicRequest>()))
      .thenReturn(
        CompletableFuture.completedFuture(
          ListSubscriptionsByTopicResponse
            .builder()
            .subscriptions(mockSubs)
            .build(),
        ),
      )

    service.updateSubscriptionAttributes("mockQueue", "AttriName", "mockValue")

    argumentCaptor<SetSubscriptionAttributesRequest>().apply {
      verify(hmppsEventSnsClient, times(1)).setSubscriptionAttributes(capture())
      Assertions.assertThat("mockSubscriptionArn").isEqualTo(firstValue.subscriptionArn())
      Assertions.assertThat("AttriName").isEqualTo(firstValue.attributeName())
      Assertions.assertThat("mockValue").isEqualTo(firstValue.attributeValue())
    }
  }

  @Test
  fun `Get subscription arn for given queue name`() {
    val mockSubs = listOf(Subscription.builder().protocol("sqs").endpoint("mockARN").subscriptionArn("mockSubscriptionArn").build())

    whenever(hmppsEventSnsClient.listSubscriptionsByTopic(any<ListSubscriptionsByTopicRequest>()))
      .thenReturn(
        CompletableFuture.completedFuture(
          ListSubscriptionsByTopicResponse
            .builder()
            .subscriptions(mockSubs)
            .build(),
        ),
      )

    val result = service.getSubscriptionArnByQueueName("mockQueue")

    argumentCaptor<ListSubscriptionsByTopicRequest>().apply {
      verify(hmppsEventSnsClient, times(1)).listSubscriptionsByTopic(capture())
      Assertions.assertThat("sometopicarn").isEqualTo(firstValue.topicArn())
    }
    result.shouldBe("mockSubscriptionArn")
  }
}
