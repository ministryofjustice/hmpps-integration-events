package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.maps.shouldNotHaveKey
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
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventStatus
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
  private lateinit var integrationEventTopicService: IntegrationEventTopicService
  val currentTime: LocalDateTime = LocalDateTime.now()

  @BeforeEach
  fun setUp() {
    whenever(hmppsQueueService.findByTopicId("integrationeventtopic"))
      .thenReturn(HmppsTopic("integrationeventtopic", "sometopicarn", hmppsEventSnsClient))
    whenever(hmppsQueueService.findByQueueId("mockQueue")).thenReturn(mockQueue)
    whenever(mockQueue.queueArn).thenReturn("mockARN")
    integrationEventTopicService = IntegrationEventTopicService(hmppsQueueService, objectMapper)
  }

  @Test
  fun `Publish Event`() {
    val event = EventNotification(eventId = 123, hmppsId = "hmppsId", eventType = IntegrationEventType.MAPPA_DETAIL_CHANGED, prisonId = "MKI", url = "mockUrl", lastModifiedDateTime = currentTime)

    val response = PublishResponse
      .builder()
      .messageId("123")
      .build()

    whenever(hmppsEventSnsClient.publish(any<PublishRequest>())).thenReturn(CompletableFuture.completedFuture(response))
    integrationEventTopicService.sendEvent(event)

    argumentCaptor<PublishRequest>().apply {
      verify(hmppsEventSnsClient, times(1)).publish(capture())
      val payload = firstValue.message()
      val messageAttributes = firstValue.messageAttributes()
      JsonAssertions.assertThatJson(payload).node("eventType").isEqualTo(event.eventType.name)
      JsonAssertions.assertThatJson(payload).node("hmppsId").isEqualTo(event.hmppsId)
      JsonAssertions.assertThatJson(payload).node("prisonId").isEqualTo(event.prisonId)
      JsonAssertions.assertThatJson(payload).node("url").isEqualTo(event.url)
      Assertions.assertThat(messageAttributes["eventType"])
        .isEqualTo(MessageAttributeValue.builder().stringValue(event.eventType.name).dataType("String").build())
      Assertions.assertThat(messageAttributes["prisonId"])
        .isEqualTo(MessageAttributeValue.builder().stringValue(event.prisonId).dataType("String").build())
    }
  }

  @Test
  fun `Publish Event with no prison Id`() {
    val event = EventNotification(eventId = 123, claimId = "claimId", status = IntegrationEventStatus.PROCESSING, hmppsId = "hmppsId", eventType = IntegrationEventType.MAPPA_DETAIL_CHANGED, prisonId = null, url = "mockUrl", lastModifiedDateTime = currentTime)

    val response = PublishResponse
      .builder()
      .messageId("123")
      .build()

    whenever(hmppsEventSnsClient.publish(any<PublishRequest>())).thenReturn(CompletableFuture.completedFuture(response))
    integrationEventTopicService.sendEvent(event)

    argumentCaptor<PublishRequest>().apply {
      verify(hmppsEventSnsClient, times(1)).publish(capture())
      val payload = firstValue.message()
      val messageAttributes = firstValue.messageAttributes()
      JsonAssertions.assertThatJson(payload).node("eventType").isEqualTo(event.eventType.name)
      JsonAssertions.assertThatJson(payload).node("hmppsId").isEqualTo(event.hmppsId)
      JsonAssertions.assertThatJson(payload).node("prisonId").isEqualTo(event.prisonId)
      JsonAssertions.assertThatJson(payload).node("url").isEqualTo(event.url)
      JsonAssertions.assertThatJson(payload).node("claimId").isAbsent()
      JsonAssertions.assertThatJson(payload).node("status").isAbsent()
      Assertions.assertThat(messageAttributes["eventType"])
        .isEqualTo(MessageAttributeValue.builder().stringValue(event.eventType.name).dataType("String").build())
      messageAttributes.shouldNotHaveKey("prisonId")
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

    integrationEventTopicService.updateSubscriptionAttributes("mockQueue", "AttriName", "mockValue")

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

    val result = integrationEventTopicService.getSubscriptionArnByQueueName("mockQueue")

    argumentCaptor<ListSubscriptionsByTopicRequest>().apply {
      verify(hmppsEventSnsClient, times(1)).listSubscriptionsByTopic(capture())
      Assertions.assertThat("sometopicarn").isEqualTo(firstValue.topicArn())
    }
    result.shouldBe("mockSubscriptionArn")
  }
}
