package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.awspring.cloud.sqs.listener.AsyncAdapterBlockingExecutionFailedException
import io.awspring.cloud.sqs.listener.ListenerExecutionFailedException
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import io.sentry.Sentry
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.messaging.support.GenericMessage
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.extractDomainEventFrom
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DeadLetterQueueService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.HmppsDomainEventService
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.CompletionException

@ActiveProfiles("test")
@JsonTest
class HmppsDomainEventsListenerTest {
  private val hmppsDomainEventService = mockk<HmppsDomainEventService>()
  private val deadLetterQueueService = mockk<DeadLetterQueueService>()
  private val hmppsDomainEventsListener: HmppsDomainEventsListener = HmppsDomainEventsListener(hmppsDomainEventService, deadLetterQueueService)
  private val currentTime: ZonedDateTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
  private val objectMapper = jacksonObjectMapper()
  private val sqsHelper = SqsNotificationGeneratingHelper(timestamp = currentTime)

  companion object {
    @BeforeAll
    @JvmStatic
    internal fun setUpAll() {
      mockkStatic(Sentry::class)
    }

    @AfterAll
    @JvmStatic
    internal fun tearDownAll() {
      unmockkStatic(Sentry::class)
    }
  }

  @BeforeEach
  fun setup() {
    every { deadLetterQueueService.sendEvent(any(), any()) } returnsArgument 0
  }

  @AfterEach
  internal fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun `when risk-assessment scores determined event is received it should call the hmppsDomainEventService`() {
    val rawMessage = sqsHelper.generateRawHmppsDomainEventWithoutRegisterType("risk-assessment.scores.determined", messageEventType = "risk-assessment.scores.ogrs.determined")
    val hmppsDomainEvent = sqsHelper.createHmppsDomainEventWithoutRegisterType("risk-assessment.scores.ogrs.determined", attributeEventTypes = "risk-assessment.scores.determined").domainEvent()

    every { hmppsDomainEventService.execute(hmppsDomainEvent) } just runs

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent) }
  }

  @Test
  fun `when a valid registration added sqs event is received it should call the hmppsDomainEventService`() {
    val rawMessage = sqsHelper.generateRawHmppsDomainEvent()
    val hmppsDomainEvent = sqsHelper.createHmppsDomainEvent().domainEvent()

    every { hmppsDomainEventService.execute(hmppsDomainEvent) } just runs

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent) }
  }

  @Test
  fun `when a valid registration updated sqs event is received it should call the hmppsDomainEventService`() {
    val rawMessage = sqsHelper.generateRawHmppsDomainEvent("probation-case.registration.updated")
    val hmppsDomainEvent = sqsHelper.createHmppsDomainEvent("probation-case.registration.updated").domainEvent()

    every { hmppsDomainEventService.execute(hmppsDomainEvent) } just runs

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent) }
  }

  @Test
  fun `when an invalid message is received it should be sent to the dead letter queue`() {
    val rawMessage = "Invalid JSON message"

    assertThrows<JsonParseException> { hmppsDomainEventsListener.onDomainEvent(rawMessage) }

    verify { hmppsDomainEventService wasNot Called }
  }

  /**
   * Should still call `hmppsDomainEventService`, given an unknown event type
   *
   * Unknown event type will be handled (discarded) later at service; Relevant test: `process and discard an unexpected event type` of [uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.HmppsDomainEventServiceTest]
   */
  @Test
  fun `when an unexpected event type is received it should still call the hmppsDomainEventService`() {
    val unexpectedEventType = "unexpected.event.type"
    val rawMessage = sqsHelper.generateRawHmppsDomainEvent(eventType = unexpectedEventType)
    val hmppsDomainEvent = sqsHelper.createHmppsDomainEvent(eventType = unexpectedEventType).domainEvent()

    every { hmppsDomainEventService.execute(hmppsDomainEvent) } just runs

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent) }
    verify { deadLetterQueueService wasNot Called }
  }

  @Nested
  inner class GivenErrorOfEventExecution {
    private val error = IllegalStateException("Something went wrong")
    private val rawMessage = sqsHelper.generateRawHmppsDomainEvent()
    private val hmppsDomainEvent = sqsHelper.createHmppsDomainEvent().domainEvent()
    private val wrappedErrorMessage = "Error executing HmppsDomainEvent"

    @AfterEach
    internal fun tearDown() {
      clearAllMocks()
    }

    @Test
    fun `when there is CompletionException, the error cause shall be extracted and logged`() = executeEventWithError(
      wrappedError = CompletionException(wrappedErrorMessage, error),
    )

    @Test
    fun `when there is AsyncAdapterBlockingExecutionFailedException, the error cause shall be extracted and logged`() = executeEventWithError(
      wrappedError = AsyncAdapterBlockingExecutionFailedException(wrappedErrorMessage, error),
    )

    @Test
    fun `when there is ListenerExecutionFailedException, the error cause shall be extracted and logged`() = executeEventWithError(
      wrappedError = ListenerExecutionFailedException(wrappedErrorMessage, error, GenericMessage(rawMessage)),
    )

    private inline fun <reified T : Throwable> executeEventWithError(
      wrappedError: T,
      unwrappedError: Throwable = error,
    ) {
      // Arrange
      every { hmppsDomainEventService.execute(hmppsDomainEvent) } throws wrappedError

      // Act, Assert (error)
      assertThrows<T> { hmppsDomainEventsListener.onDomainEvent(rawMessage) }

      // Assert (verify)
      verify(exactly = 1) { Sentry.captureException(match { it.message == unwrappedError.message }) }
    }
  }

  private fun SQSMessage.domainEvent(): HmppsDomainEvent = extractDomainEventFrom(this, objectMapper)
}
