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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockReset
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventStatus
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.IntegrationEventTopicService
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.LocalDateTime
import java.util.UUID
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

  @MockitoSpyBean(reset = MockReset.BEFORE)
  private lateinit var integrationEventTopicService: IntegrationEventTopicService

  internal val integrationEventTestQueue by lazy { hmppsQueueService.findByQueueId("integrationeventtestqueue") as HmppsQueue }
  internal val integrationEventTestQueueSqsClient by lazy { integrationEventTestQueue.sqsClient }
  private val integrationEventTestQueueUrl: String by lazy { integrationEventTestQueue.queueUrl }

  @BeforeEach
  fun purgeQueues() {
    Mockito.reset(integrationEventTopicService)
    integrationEventTestQueueSqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(integrationEventTestQueueUrl).build()).get()
  }

  @JvmRecord
  internal data class SQSMessage(val Message: String)

  @Throws(ExecutionException::class, InterruptedException::class)
  private fun getMessagesCurrentlyOnTestQueue(): List<String> {
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

  private fun toSQSMessage(message: String): SQSMessage = try {
    objectMapper.readValue(message, SQSMessage::class.java)
  } catch (_: JsonProcessingException) {
    throw AssertionFailedError(String.format("Message %s is not parseable", message))
  }

  fun getEvent(prisonId: String? = null, url: String) = EventNotification(
    status = IntegrationEventStatus.PENDING,
    eventType = IntegrationEventType.MAPPA_DETAIL_CHANGED,
    hmppsId = "MockId",
    prisonId = prisonId,
    url = url,
    lastModifiedDateTime = LocalDateTime.now().minusMinutes(6),
  )

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["MKI"])
  @DisplayName("will publish Integration Event with no prison Id")
  @Throws(
    ExecutionException::class,
    InterruptedException::class,
  )
  fun willPublishPrisonEvent(prisonId: String?) {
    await.atMost(5, TimeUnit.SECONDS).untilAsserted {
      eventRepository.save(getEvent(prisonId, UUID.randomUUID().toString()))
      Mockito.verify(integrationEventTopicService, Mockito.atLeast(1)).sendEvent(any())
      val prisonEventMessages = getMessagesCurrentlyOnTestQueue()
      Assertions.assertThat(prisonEventMessages)
        .singleElement()
        .satisfies(
          ThrowingConsumer { event: String? ->
            JsonAssertions.assertThatJson(event)
              .node("eventType")
              .isEqualTo(IntegrationEventType.MAPPA_DETAIL_CHANGED.name)
            JsonAssertions.assertThatJson(event)
              .node("prisonId")
              .isEqualTo(prisonId)
          },
        )
    }
  }
}
