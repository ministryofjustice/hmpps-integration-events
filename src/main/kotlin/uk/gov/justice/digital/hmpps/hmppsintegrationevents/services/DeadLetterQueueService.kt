package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Service
class DeadLetterQueueService(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {

  private val dlQueue by lazy { hmppsQueueService.findByQueueId("hmppsdomainqueue") as HmppsQueue }
  private val dlClient by lazy { dlQueue.sqsDlqClient!! }
  private val dlQueueUrl by lazy { dlQueue.dlqUrl }

  fun sendEvent(payload: Any, errorMessage: String?) {
    val messageBody = if (payload is String) payload else objectMapper.writeValueAsString(payload)

    dlClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(dlQueueUrl)
        .messageBody(messageBody)
        .messageAttributes(mapOf("Error" to MessageAttributeValue.builder().dataType("String").stringValue(errorMessage).build()))
        .build(),
    )
  }
}
