package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

<<<<<<< HEAD
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import java.time.LocalDateTime
=======
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
>>>>>>> main

@Service
@Configuration
class EventNotifierService(
  private val integrationEventTopicService: IntegrationEventTopicService,
  val eventRepository: EventNotificationRepository,
) {
  @Scheduled(fixedRateString = "\${notifier.schedule.rate}")
  fun sentNotifications() {
    val fiveMinutesAgo = LocalDateTime.now().minusMinutes(5)
    val events = eventRepository.findAllWithLastModifiedDateTimeBefore(fiveMinutesAgo)
    events.forEach {
      integrationEventTopicService.sendEvent(it)
      eventRepository.deleteById(it.eventId!!)
    }
  }
}
