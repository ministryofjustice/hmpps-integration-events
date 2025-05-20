package uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import junit.framework.AssertionFailedError
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.services.IntegrationEventTest
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class SqsIntegrationTestBase {

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  protected lateinit var objectMapper: ObjectMapper

  lateinit var webTestClient: WebTestClient

  protected val domainEventsQueueConfig by lazy { hmppsQueueService.findByQueueId("hmppsdomainqueue") ?: throw MissingQueueException("HmppsQueue hmppsDomainQueue not found") }
  protected val domainEventsQueueSqsUrl by lazy { domainEventsQueueConfig.queueUrl }
  protected val domainEventsQueueSqsClient by lazy { domainEventsQueueConfig.sqsClient }
  private val domainEventsDeadLetterSqsClient by lazy { domainEventsQueueConfig.sqsDlqClient as SqsAsyncClient }
  private val domainEventsDeadLetterSqsUrl by lazy { domainEventsQueueConfig.dlqUrl as String }
  protected val integrationEventTestQueue by lazy { hmppsQueueService.findByQueueId("integrationeventtestqueue") as HmppsQueue }
  protected val integrationEventTestQueueSqsClient by lazy { integrationEventTestQueue.sqsClient }
  protected val integrationEventTestQueueUrl: String by lazy { integrationEventTestQueue.queueUrl }

  @BeforeEach
  fun cleanQueue() {
    domainEventsQueueSqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(domainEventsQueueSqsUrl).build()).get()
    integrationEventTestQueueSqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(integrationEventTestQueueUrl).build()).get()
    domainEventsDeadLetterSqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(domainEventsDeadLetterSqsUrl).build()).get()
  }

  protected fun getNumberOfMessagesCurrentlyOndomainEventsDeadLetterQueue(): Int = domainEventsDeadLetterSqsClient.countAllMessagesOnQueue(domainEventsDeadLetterSqsUrl).get()
  protected fun geMessagesCurrentlyOnTestQueue(): List<String> {
    val messageResult = integrationEventTestQueueSqsClient.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(integrationEventTestQueueUrl).build(),
    ).get()
    return messageResult
      .messages()
      .stream()
      .map { obj: Message -> obj.body() }
      .map { message: String -> toSQSMessage(message) }
      .map(IntegrationEventTest.SQSMessage::Message)
      .toList()
  }

  protected fun geMessagesCurrentlyOnDomainEventsDeadLetterQueue(): ReceiveMessageResponse = domainEventsDeadLetterSqsClient.receiveMessage(
    ReceiveMessageRequest.builder().queueUrl(domainEventsDeadLetterSqsUrl).messageAttributeNames("All").build(),
  ).get()

  internal fun toSQSMessage(message: String): IntegrationEventTest.SQSMessage = try {
    objectMapper.readValue(message, IntegrationEventTest.SQSMessage::class.java)
  } catch (e: JsonProcessingException) {
    throw AssertionFailedError(String.format("Message %s is not parseable", message))
  }

  protected fun sendDomainSqsMessage(rawMessage: String) {
    domainEventsQueueSqsClient.sendMessage(SendMessageRequest.builder().queueUrl(domainEventsQueueSqsUrl).messageBody(rawMessage).build())
  }
}
