package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.kotest.matchers.shouldBe
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic

@ActiveProfiles("test")
@JsonTest
class DeadletterQueueServiceTest {

  val hmppsQueueService: HmppsQueueService = Mockito.mock()
  val hmppsEventSnsClient: SnsAsyncClient = Mockito.mock()

  val hmppsEventSqsClient: SqsAsyncClient = Mockito.mock()
  val hmppsEventDLSqsClient: SqsAsyncClient = Mockito.mock()

  lateinit var service: DeadLetterQueueService

  @BeforeEach
  fun setUp() {
    whenever(hmppsQueueService.findByTopicId("hmppsdomainqueue"))
      .thenReturn(HmppsTopic("integrationeventtopic", "sometopicarn", hmppsEventSnsClient))
    whenever(hmppsQueueService.findByQueueId("hmppsdomainqueue"))
      .thenReturn(HmppsQueue("hmppsdomainqueue", hmppsEventSqsClient, "hmpps_integrations_events_queue", hmppsEventDLSqsClient, "hmpps_integrations_events_queue_dlq"))

    service = DeadLetterQueueService(hmppsQueueService)
  }

  @Test
  fun `Put event into dlq if failed to publish message and remove entity from database`() {
    val mockPayload = "mockPayload"
    val error = "mockError"

    service.sendEvent(mockPayload, error)

    argumentCaptor<SendMessageRequest>().apply {
      verify(hmppsEventDLSqsClient, times(1)).sendMessage(capture())
      val messageAttributes = firstValue.messageAttributes()
      val payload = firstValue.messageBody()
      payload.shouldBe(mockPayload)
      Assertions.assertThat(messageAttributes["Error"])
        .isEqualTo(MessageAttributeValue.builder().stringValue(error).dataType("String").build())
    }
  }
}
