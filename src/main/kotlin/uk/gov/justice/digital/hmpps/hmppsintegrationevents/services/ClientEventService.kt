package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.EventClientProperties
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Service
@Component
@EnableConfigurationProperties(EventClientProperties::class)
class ClientEventService(
  clientProperties: EventClientProperties,
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {

  val clientQueueList = clientProperties.clients.map { it -> it.value.pathCode to hmppsQueueService.findByQueueId(it.value.queueName) }.toMap()

  fun getClientMessage(pathCode: String): EventResponse {
    val clientQueueConfig = clientQueueList[pathCode]!!
    val rawResponse = clientQueueConfig.sqsClient.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(clientQueueConfig.queueUrl).messageAttributeNames("All").maxNumberOfMessages(1).waitTimeSeconds(1).build(),
    ).get()

    return EventResponse(
      MessageResponse(
        receiveMessageResult = ReceiveMessageResult(rawResponse.messages().map { it.toBuilder() }),
        responseMetadata = ResponseMetadata(rawResponse.responseMetadata().requestId()),
      ),
    )
  }
}

data class EventResponse(
  @JsonProperty("ReceiveMessageResponse")
  val messageResponse: MessageResponse,
)

data class MessageResponse(
  @JsonProperty("ReceiveMessageResult")
  val receiveMessageResult: ReceiveMessageResult,
  @JsonProperty("ResponseMetadata")
  val responseMetadata: ResponseMetadata,
)

data class ReceiveMessageResult(
  val messages: List<Message.Builder>,
)

data class ResponseMetadata(
  @JsonProperty("RequestId")
  val requestId: String,
)
