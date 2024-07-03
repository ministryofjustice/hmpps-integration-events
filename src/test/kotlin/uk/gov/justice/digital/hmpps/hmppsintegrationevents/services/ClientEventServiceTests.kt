package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.awscore.DefaultAwsResponseMetadata
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.ClientConfig
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.EventClientProperties
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.util.concurrent.CompletableFuture

@ActiveProfiles("test")
@JsonTest
class ClientEventServiceTests(@Autowired private val objectMapper: ObjectMapper) {

  val hmppsQueueService: HmppsQueueService = Mockito.mock()
  val hmppsEventSqsClient: SqsAsyncClient = Mockito.mock()
  val clientProperties: EventClientProperties = EventClientProperties(clients = mapOf("MockService1" to ClientConfig(queueId = "mockClientQueue", pathCode = "mockClient")))
  val mockQueue: HmppsQueue = Mockito.mock()
  val auditService: AuditService = Mockito.mock()
  lateinit var service: ClientEventService

  val mockMessage = """
     {
      "Type": "Notification",
      "MessageId": "a89b02b9-dd7f-5948-ba30-5c4fcdd1d0b1",
      "TopicArn": "arn:aws:sns:eu-west-2:754256621582:cloud-platform-hmpps-integration-api-03681c915391fb9206868bed93c97141",
      "Message": "{\"eventId\":1,\"hmppsId\":\"X627337\",\"eventType\":\"RISK_SCORE_CHANGED\",\"url\":\"https://dev.integration-api.hmpps.service.justice.gov.uk/v1/persons/X627337/risks/scores\",\"lastModifiedDateTime\":\"2024-06-26T13:44:42.445179\"}",
      "Timestamp": "2024-06-26T12:49:50.316Z",
      "SignatureVersion": "1",
      "Signature": "CyC65oUhhgjb2s0s+zJ67y4yGqt0D3ljmPG8P/FJ5RSecdrdAGddp0/SnweaOegHCHIgQRCYrHIKfHemodNegI5/EfDl2nabeZPbKDeUOpSeCztnod3Ml7P0MxBGLaETwmF8m9qmOkpAM7AVDBToDZYWtZweHmTnbxqEwwRUgeGc6Cohkw6wCFTqpUaV2YGwbV8A4p1i3S0oFb0VivEgcvjMJtdtp6KU9nmb0Zduto9f11902M/RRB/LLHVPx+az08jPbUGraV08G+dwTZhas/NpH2s6rYzqobS2sV84c3qBxoa3Qds+M0Fva3DVSNFWjD19wlN7WIJgF8AwLhhGAQ==",
      "SigningCertURL": "https://sns.eu-west-2.amazonaws.com/SimpleNotificationService-60eadc530605d63b8e62a523676ef735.pem",
      "UnsubscribeURL": "https://sns.eu-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-2:754256621582:cloud-platform-hmpps-integration-api-03681c915391fb9206868bed93c97141:e857f144-b65d-495a-ad8c-ef52e7f5ceba",
      "MessageAttributes": {
        "eventType": {
          "Type": "String",
          "Value": "RISK_SCORE_CHANGED"
        }
      }
    }
  """.trimIndent()

  @BeforeEach
  fun setUp() {
    whenever(mockQueue.sqsClient).thenReturn(hmppsEventSqsClient)
    whenever(mockQueue.queueUrl).thenReturn("mockUrl")
    whenever(hmppsQueueService.findByQueueId("mockClientQueue"))
      .thenReturn(mockQueue)

    service = ClientEventService(clientProperties, hmppsQueueService, objectMapper, auditService)
  }

  @Test
  fun `Retrieve max one message with wait time of one seconds `() {
    whenever(hmppsEventSqsClient.receiveMessage(any<ReceiveMessageRequest>())).thenReturn(
      CompletableFuture.completedFuture(
        ReceiveMessageResponse.builder()
          .messages(listOf(Message.builder().body(mockMessage).build()))
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
          .messages(listOf(Message.builder().body(mockMessage).build()))
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
    Assertions.assertThat(result.messageResponse.receiveMessageResult.messages.first().build().body()).isEqualTo(mockMessage)
  }

  @Test
  fun `Log audit`() {
    whenever(hmppsEventSqsClient.receiveMessage(any<ReceiveMessageRequest>())).thenReturn(
      CompletableFuture.completedFuture(
        ReceiveMessageResponse.builder()
          .messages(listOf(Message.builder().body(mockMessage).build()))
          .responseMetadata(
            DefaultAwsResponseMetadata.create(
              mapOf("AWS_REQUEST_ID" to "mockRequestId"),
            ),
          )
          .build() as ReceiveMessageResponse,
      ),
    )

    service.getClientMessage("mockClient")

    verify(auditService, times(1)).createEvent("mockClient", "mockClient received event", mapOf("eventType" to "RISK_SCORE_CHANGED", "hmppsId" to "X627337"))
  }

  @Test
  fun `Delete message from queue after retrieve`() {
    whenever(hmppsEventSqsClient.receiveMessage(any<ReceiveMessageRequest>())).thenReturn(
      CompletableFuture.completedFuture(
        ReceiveMessageResponse.builder()
          .messages(listOf(Message.builder().body(mockMessage).receiptHandle("MockHandle").build()))
          .responseMetadata(
            DefaultAwsResponseMetadata.create(
              mapOf("AWS_REQUEST_ID" to "mockRequestId"),
            ),
          )
          .build() as ReceiveMessageResponse,
      ),
    )

    service.getClientMessage("mockClient")

    argumentCaptor<DeleteMessageRequest>().apply {
      verify(hmppsEventSqsClient, times(1)).deleteMessage(capture())
      Assertions.assertThat(firstValue.queueUrl()).isEqualTo("mockUrl")
      Assertions.assertThat(firstValue.receiptHandle()).isEqualTo("MockHandle")
    }
  }
}
