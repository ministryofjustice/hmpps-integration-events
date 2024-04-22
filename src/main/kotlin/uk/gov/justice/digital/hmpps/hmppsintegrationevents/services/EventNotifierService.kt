package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.LocalDateTime
import software.amazon.awssdk.services.sns.model.MessageAttributeValue as snsMessageAttributeValue
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue as sqsMessageAttributeValue

@Service
@Configuration
class EventNotifierService(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
  val eventRepository: EventNotificationRepository,
) {
  private final val hmppsEventsTopicSnsClient: SnsAsyncClient
  private final val topicArn: String

  private val dlQueue by lazy { hmppsQueueService.findByQueueId("hmppsdomainqueue") as HmppsQueue }
  private val dlClient by lazy { dlQueue.sqsDlqClient!! }
  private val dlQueueUrl by lazy { dlQueue.dlqUrl }
  init {
    val hmppsEventTopic = hmppsQueueService.findByTopicId("integrationeventtopic")
    topicArn = hmppsEventTopic!!.arn
    hmppsEventsTopicSnsClient = hmppsEventTopic.snsClient
  }

  @Scheduled(fixedRateString = "\${notifier.schedule.rate}")
  fun sentNotifications() {
    val fiveMinutesAgo = LocalDateTime.now().minusMinutes(5)
    val events = eventRepository.findAllWithLastModifiedDateTimeBefore(fiveMinutesAgo)
    events.forEach { event -> sendEvent(event) }
  }

  fun sendEvent(payload: EventNotification) {
    try {
      hmppsEventsTopicSnsClient.publish(
        PublishRequest.builder()
          .topicArn(topicArn)
          .message(objectMapper.writeValueAsString(payload))
          .messageAttributes(mapOf("eventType" to snsMessageAttributeValue.builder().dataType("String").stringValue(payload.eventType.name).build())).build(),
      )
    } catch (e: Exception) {
      dlClient.sendMessage(
        SendMessageRequest.builder()
          .queueUrl(dlQueueUrl)
          .messageBody(
            objectMapper.writeValueAsString(payload),
          )
          .messageAttributes(mapOf("Error" to sqsMessageAttributeValue.builder().dataType("String").stringValue(e.message).build()))
          .build(),
      )
    }

    eventRepository.deleteById(payload.eventId)
  }
}
