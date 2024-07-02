package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.IntegrationTestBase
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException

class EventController : IntegrationTestBase() {

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  private val mockServiceClientQueueConfig by lazy { hmppsQueueService.findByQueueId("subscribertestqueue2") ?: throw MissingQueueException("Queue subscribertestqueue not found") }
  private val queueClient by lazy { mockServiceClientQueueConfig.sqsClient }
  private val queueUrl by lazy { mockServiceClientQueueConfig.queueUrl }

  @Test
  fun `Request not contain subject-distinguished-name header, return 403 `() {
    webTestClient.get()
      .uri("/events/mockservice")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Request client not configured to consumer events, return 403`() {
    webTestClient.get()
      .uri("/events/mockservice")
      .headers { it.add("subject-distinguished-name", "C=GB,ST=London,L=London,O=Home Office,CN=UnauthorizedClient") }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Request request path code mismatch, return 403`() {
    webTestClient.get()
      .uri("/events/otherClient")
      .headers { it.add("subject-distinguished-name", "C=GB,ST=London,L=London,O=Home Office,CN=MockService1") }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Valid consumer, return consumer event`() {
    queueClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(queueUrl).build()).get()
    val message = """
      {
      "Type": "Notification",
      "MessageId": "b025b5eb-c676-5e11-bd73-eb09dabd1dcb",
      "TopicArn": "arn:aws:sns:eu-west-2:754256621582:cloud-platform-hmpps-integration-api-03681c915391fb9206868bed93c97141",
      "Message": "{\"eventId\":2,\"hmppsId\":\"E534685\",\"eventType\":\"MAPPA_DETAIL_CHANGED\",\"url\":\"https://dev.integration-api.hmpps.service.justice.gov.uk/v1/persons/E534685/risks/mappadetail\",\"lastModifiedDateTime\":\"2024-06-11T10:30:37.317\"}",
      "Timestamp": "2024-06-11T09:36:11.191Z",
      "SignatureVersion": "1",
      "Signature": "ofAyJUHaOyCkk0Zf92WTsYCTXRBXt7aibmT84IxNH1HN7ynEMQ/kxwmKhO3FrBBVNzNg7P/+9UsXZjZaJ3tFX64cngMk4dY/w4xOXe5vy8WuTsnIX3TvqHho2mAxGYnmSNJS+1q3V0FuYaZ4YBWDMOmDcjIQDGtULm6M8+gZkmv7B4RJ2qtXwlrfNyl4UilKmO6Bq7dZVyOEc32SlDQz+wGYBC++QUqWufzWAMhF5mNCMb6qAKWmUhICqla2O1/haTMetmEF9cY3Qx7JTce/Z5z9uXHnrg1NmpS5FRu28vgBde9lNHNZ6EKXPBQ39kVXHglkq5ujldQxjDyTj1zwIQ==",
      "SigningCertURL": "https://sns.eu-west-2.amazonaws.com/SimpleNotificationService-60eadc530605d63b8e62a523676ef735.pem",
      "UnsubscribeURL": "https://sns.eu-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-2:754256621582:cloud-platform-hmpps-integration-api-03681c915391fb9206868bed93c97141:e857f144-b65d-495a-ad8c-ef52e7f5ceba",
      "MessageAttributes": {
        "eventType": {
          "Type": "String",
          "Value": "MAPPA_DETAIL_CHANGED"
        }
      }
    }""".replace("(\"[^\"]*\")|\\s".toRegex(), "\$1")
    val expectedResult = objectMapper.writeValueAsString(message)

    queueClient.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(expectedResult).build()).get()
    val result = webTestClient.get()
      .uri("/events/mockservice2")
      .headers { it.add("subject-distinguished-name", "C=GB,ST=London,L=London,O=Home Office,CN=MockService2") }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(String::class.java)
      .returnResult().getResponseBody()

    val resultJson = objectMapper.readTree(result)
    val messageJson = objectMapper.readTree(resultJson.get("ReceiveMessageResponse").get("ReceiveMessageResult").get("messages")[0].get("body").textValue())
    Assertions.assertEquals(message, messageJson.asText())
  }
}
