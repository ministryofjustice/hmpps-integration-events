package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Service
class DlqService(
  private val hmppsQueueService: HmppsQueueService,
) {
  private val dlQueue by lazy { hmppsQueueService.findByQueueId("prisoner") as HmppsQueue }
  private val dlClient by lazy { dlQueue.sqsDlqClient!! }
  private val dlQueueUrl by lazy { dlQueue.dlqUrl }

  fun sendEvent(payload: Any, e: Exception) {
    dlClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(dlQueueUrl)
        .messageBody(
          payload.toString(),
        )
        .messageAttributes(mapOf("Error" to MessageAttributeValue.builder().dataType("String").stringValue(e.message).build()))
        .build(),
    )
  }
}
