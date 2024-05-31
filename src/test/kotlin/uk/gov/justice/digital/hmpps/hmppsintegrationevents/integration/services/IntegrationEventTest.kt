package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.services

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import junit.framework.AssertionFailedError
import net.javacrumbs.jsonunit.assertj.JsonAssertions
import org.assertj.core.api.Assertions
import org.assertj.core.api.ThrowingConsumer
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockReset
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.EventTypeValue
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.IntegrationEventTopicService
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.LocalDateTime
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class IntegrationEventTest {
  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var eventRepository: EventNotificationRepository

  @SpyBean(reset = MockReset.BEFORE)
  private lateinit var integrationEventTopicService: IntegrationEventTopicService

  internal val integrationEventTestQueue by lazy { hmppsQueueService.findByQueueId("integrationeventtestqueue") as HmppsQueue }
  internal val integrationEventTestQueueSqsClient by lazy { integrationEventTestQueue.sqsClient }
  protected val integrationEventTestQueueUrl: String by lazy { integrationEventTestQueue.queueUrl }

  @BeforeEach
  fun purgeQueues() {
    integrationEventTestQueueSqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(integrationEventTestQueueUrl).build()).get()
    await.until { getNumberOfMessagesCurrentlyOnIntegrationEventTestQueue() == 0 }
  }

  @JvmRecord
  internal data class SQSMessage(val Message: String)

  @Throws(ExecutionException::class, InterruptedException::class)
  private fun geMessagesCurrentlyOnTestQueue(): List<String> {
    val messageResult = integrationEventTestQueueSqsClient.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(integrationEventTestQueueUrl).build(),
    ).get()
    return messageResult
      .messages()
      .stream()
      .map { obj: Message -> obj.body() }
      .map { message: String -> toSQSMessage(message) }
      .map(SQSMessage::Message)
      .toList()
  }
  private fun toSQSMessage(message: String): SQSMessage {
    return try {
      objectMapper.readValue(message, SQSMessage::class.java)
    } catch (e: JsonProcessingException) {
      throw AssertionFailedError(String.format("Message %s is not parseable", message))
    }
  }
  fun getNumberOfMessagesCurrentlyOnIntegrationEventTestQueue(): Int = integrationEventTestQueueSqsClient.countAllMessagesOnQueue(integrationEventTestQueueUrl).get()

  @Test
  @DisplayName("will publish Integration Event")
  @Throws(
    ExecutionException::class,
    InterruptedException::class,
  )
  fun willPublishPrisonEvent() {
    await.atMost(10, TimeUnit.SECONDS).untilAsserted {
      eventRepository.save(
        EventNotification(
          eventType = EventTypeValue.REGISTRATION_ADDED,
          hmppsId = "MockId",
          url = "MockUrl",
          lastModifiedDateTime = LocalDateTime.now().minusMinutes(6),
        ),
      )
      Mockito.verify(integrationEventTopicService, Mockito.atLeast(1)).sendEvent(any())
      val prisonEventMessages = geMessagesCurrentlyOnTestQueue()
      Assertions.assertThat(prisonEventMessages)
        .singleElement()
        .satisfies(
          ThrowingConsumer { event: String? ->
            JsonAssertions.assertThatJson(event)
              .node("eventType")
              .isEqualTo(EventTypeValue.REGISTRATION_ADDED.name)
          },
        )
    }
  }
}
