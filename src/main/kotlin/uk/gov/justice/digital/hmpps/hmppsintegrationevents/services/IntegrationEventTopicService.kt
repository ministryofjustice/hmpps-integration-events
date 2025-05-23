package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.SetSubscriptionAttributesRequest
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Service
class IntegrationEventTopicService(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {
  private final val hmppsEventsTopicSnsClient: SnsAsyncClient
  private final val topicArn: String

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  init {
    val hmppsEventTopic = hmppsQueueService.findByTopicId("integrationeventtopic")
    topicArn = hmppsEventTopic!!.arn
    hmppsEventsTopicSnsClient = hmppsEventTopic.snsClient
  }

  fun sendEvent(payload: EventNotification) {
    val messageAttributes = mutableMapOf(
      "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(payload.eventType.name).build(),
    )
    if (payload.prisonId != null) {
      messageAttributes["prisonId"] = MessageAttributeValue.builder().dataType("String").stringValue(payload.prisonId).build()
    }
    hmppsEventsTopicSnsClient.publish(
      PublishRequest.builder()
        .topicArn(topicArn)
        .message(objectMapper.writeValueAsString(payload))
        .messageAttributes(messageAttributes)
        .build(),
    ).get()
    log.info("successfully published event ${payload.eventType.name} to integrationeventtopic. $payload")
  }

  fun updateSubscriptionAttributes(queueName: String, attributeName: String, attributeValueJson: String) {
    val subscriberArn = getSubscriptionArnByQueueName(queueName)
    val request = SetSubscriptionAttributesRequest.builder()
      .subscriptionArn(subscriberArn)
      .attributeName(attributeName)
      .attributeValue(attributeValueJson)
      .build()
    hmppsEventsTopicSnsClient.setSubscriptionAttributes(request)
  }

  fun getSubscriptionArnByQueueName(queueName: String): String? {
    val queue = hmppsQueueService.findByQueueId(queueName) as HmppsQueue
    val listSubscriptionsByTopicRequest = ListSubscriptionsByTopicRequest.builder().topicArn(topicArn).build()
    val listSubscriptionsResponse = hmppsEventsTopicSnsClient.listSubscriptionsByTopic(listSubscriptionsByTopicRequest).get()

    return listSubscriptionsResponse.subscriptions().first {
      it.protocol() == "sqs" && it.endpoint() == queue.queueArn
    }.subscriptionArn()
  }
}
