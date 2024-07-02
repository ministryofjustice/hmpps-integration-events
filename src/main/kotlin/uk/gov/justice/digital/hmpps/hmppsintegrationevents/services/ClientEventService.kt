package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.EventClientProperties
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.EventResponse
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.MessageResponse
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.ReceiveMessageResult
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.ResponseMetadata
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Service
@Component
@EnableConfigurationProperties(EventClientProperties::class)
class ClientEventService(
  clientProperties: EventClientProperties,
  private val hmppsQueueService: HmppsQueueService,
) {

  val clientQueueList = clientProperties.clients.map { it.value.pathCode to hmppsQueueService.findByQueueId(it.value.queueId) }.toMap()

  fun getClientMessage(pathCode: String): EventResponse {
    val clientQueueConfig = clientQueueList[pathCode]!!
    val rawResponse = clientQueueConfig.sqsClient.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(clientQueueConfig.queueUrl).messageAttributeNames("All").maxNumberOfMessages(1).waitTimeSeconds(1).build(),
    ).get()
    rawResponse.messages().firstOrNull()?.let { message ->
      //TODO Audit
      val deleteRequest = DeleteMessageRequest.builder().queueUrl(clientQueueConfig.queueUrl).receiptHandle(message.receiptHandle()).build()
      clientQueueConfig.sqsClient.deleteMessage(deleteRequest)
    }

    return EventResponse(
      MessageResponse(
        receiveMessageResult = ReceiveMessageResult(rawResponse.messages().map { it.toBuilder() }),
        responseMetadata = ResponseMetadata(rawResponse.responseMetadata().requestId()),
      ),
    )
  }
}
