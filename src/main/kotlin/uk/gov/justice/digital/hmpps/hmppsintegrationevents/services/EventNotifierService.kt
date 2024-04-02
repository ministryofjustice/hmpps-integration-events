package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.LocalDateTime

@Service
class EventNotifierService(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
  val eventRepository: EventNotificationRepository,
) {
  private final val hmppsEventsTopicSnsClient: SnsAsyncClient
  private final val topicArn: String
  init {
    val hmppsEventTopic = hmppsQueueService.findByTopicId("integrationeventtopic")
    topicArn = hmppsEventTopic!!.arn
    hmppsEventsTopicSnsClient = hmppsEventTopic.snsClient
  }

  @Scheduled(fixedRate = 10000)
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
          .messageAttributes(mapOf("eventType" to MessageAttributeValue.builder().dataType("String").stringValue(payload.eventType.name).build())).build(),
      )
    } catch (e: JsonProcessingException) {
      // TODO put into dl queue
    }

    eventRepository.deleteById(payload.eventId)
  }
}
