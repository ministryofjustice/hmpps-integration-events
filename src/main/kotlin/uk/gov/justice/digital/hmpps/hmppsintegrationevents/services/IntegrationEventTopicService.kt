package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.SetSubscriptionAttributesRequest
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Service
class IntegrationEventTopicService(
  private val hmppsQueueService: HmppsQueueService,
  private val deadLetterQueueService: DeadLetterQueueService,
  private val objectMapper: ObjectMapper,
) {
  private final val hmppsEventsTopicSnsClient: SnsAsyncClient
  private final val topicArn: String

  init {
    val hmppsEventTopic = hmppsQueueService.findByTopicId("integrationeventtopic")
    topicArn = hmppsEventTopic!!.arn
    hmppsEventsTopicSnsClient = hmppsEventTopic.snsClient
  }

  fun sendEvent(payload: EventNotification) {
    try {
      hmppsEventsTopicSnsClient.publish(
        PublishRequest.builder()
          .topicArn(topicArn)
          .message(objectMapper.writeValueAsString(payload))
          .messageAttributes(mapOf("eventType" to MessageAttributeValue.builder().dataType("String").stringValue(payload.eventType.name).build())).build(),
      )
    } catch (e: Exception) {
      deadLetterQueueService.sendEvent(payload, e.message)
    }
  }

  fun updateSubscriptionAttributes(subscriberArn: String, attributeName: String, attributeValueJson: String) {
    val request = SetSubscriptionAttributesRequest.builder()
      .subscriptionArn(subscriberArn)
      .attributeName(attributeName)
      .attributeValue(attributeValueJson)
      .build()
    hmppsEventsTopicSnsClient.setSubscriptionAttributes(request)
  }
}
