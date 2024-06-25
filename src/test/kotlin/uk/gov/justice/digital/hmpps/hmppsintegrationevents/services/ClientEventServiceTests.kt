package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.awscore.DefaultAwsResponseMetadata
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.ClientConfig
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.EventClientProperties
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.util.concurrent.CompletableFuture

@ActiveProfiles("test")
class ClientEventServiceTests {
  val hmppsQueueService: HmppsQueueService = Mockito.mock()
  val hmppsEventSqsClient: SqsAsyncClient = Mockito.mock()
  val clientProperties: EventClientProperties = EventClientProperties(clients = mapOf("MockService1" to ClientConfig(queueId = "mockClientQueue", pathCode = "mockClient")))
  val mockQueue: HmppsQueue = Mockito.mock()
  lateinit var service: ClientEventService

  @BeforeEach
  fun setUp() {
    whenever(mockQueue.sqsClient).thenReturn(hmppsEventSqsClient)
    whenever(mockQueue.queueUrl).thenReturn("mockUrl")
    whenever(hmppsQueueService.findByQueueId("mockClientQueue"))
      .thenReturn(mockQueue)

    service = ClientEventService(clientProperties, hmppsQueueService)
  }

  @Test
  fun `Retrieve max one message with wait time of one seconds `() {
    whenever(hmppsEventSqsClient.receiveMessage(any<ReceiveMessageRequest>())).thenReturn(
      CompletableFuture.completedFuture(
        ReceiveMessageResponse.builder()
          .messages(listOf(Message.builder().body("MockMessage").build()))
          .responseMetadata(
            DefaultAwsResponseMetadata.create(
              mapOf("AWS_REQUEST_ID" to "mockRequestId"),
            ),
          )
          .build() as ReceiveMessageResponse,
      ),
    )

    service.getClientMessage("mockClient")

    argumentCaptor<ReceiveMessageRequest>().apply {
      verify(hmppsEventSqsClient, times(1)).receiveMessage(capture())
      Assertions.assertThat(firstValue.maxNumberOfMessages()).isEqualTo(1)
      Assertions.assertThat(firstValue.queueUrl()).isEqualTo("mockUrl")
      Assertions.assertThat(firstValue.messageAttributeNames()).isEqualTo(listOf("All"))
      Assertions.assertThat(firstValue.waitTimeSeconds()).isEqualTo(1)
    }
  }

  @Test
  fun `Retrieve message from client sqs queue`() {
    whenever(hmppsEventSqsClient.receiveMessage(any<ReceiveMessageRequest>())).thenReturn(
      CompletableFuture.completedFuture(
        ReceiveMessageResponse.builder()
          .messages(listOf(Message.builder().body("MockMessage").build()))
          .responseMetadata(
            DefaultAwsResponseMetadata.create(
              mapOf("AWS_REQUEST_ID" to "mockRequestId"),
            ),
          )
          .build() as ReceiveMessageResponse,
      ),
    )

    val result = service.getClientMessage("mockClient")

    Assertions.assertThat(result).isNotNull
    Assertions.assertThat(result.messageResponse.responseMetadata.requestId).isEqualTo("mockRequestId")
    Assertions.assertThat(result.messageResponse.receiveMessageResult.messages.first().build().body()).isEqualTo("MockMessage")
  }
}
