package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventTypes
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.IntegrationTestBase
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.time.LocalDateTime

class EventController : IntegrationTestBase() {

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  private val mockServiceClientQueueConfig by lazy { hmppsQueueService.findByQueueId("subscribertestqueue2") ?: throw MissingQueueException("Queue subscribertestqueue not found") }
  private val queueClient by lazy { mockServiceClientQueueConfig.sqsClient }
  private val queueUrl by lazy { mockServiceClientQueueConfig.queueUrl }
  private final val eventTopic by lazy { hmppsQueueService.findByTopicId("integrationeventtopic") as HmppsTopic }
  private final val hmppsEventsTopicSnsClient by lazy { eventTopic.snsClient }

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

    val notification = EventNotification(eventId = 2, hmppsId = "E534685", eventType = IntegrationEventTypes.MAPPA_DETAIL_CHANGED, url = "https://dev.integration-api.hmpps.service.justice.gov.uk/v1/persons/E534685/risks/mappadetail", lastModifiedDateTime = LocalDateTime.now())
    val expectedResult = objectMapper.writeValueAsString(notification)

    hmppsEventsTopicSnsClient.publish(
      PublishRequest.builder()
        .topicArn(eventTopic.arn)
        .message(expectedResult)
        .messageAttributes(mapOf("eventType" to MessageAttributeValue.builder().dataType("String").stringValue(notification.eventType.name).build()))
        .build(),
    ).get()
    val result = webTestClient.get()
      .uri("/events/mockservice2")
      .headers { it.add("subject-distinguished-name", "C=GB,ST=London,L=London,O=Home Office,CN=MockService2") }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(String::class.java)
      .returnResult().responseBody

    val resultJson = objectMapper.readTree(result)
    val messageJson = objectMapper.readTree(resultJson.get("ReceiveMessageResponse").get("ReceiveMessageResult").get("messages")[0].get("body").textValue())
    Assertions.assertEquals(expectedResult, messageJson.get("Message").asText())
  }
}
