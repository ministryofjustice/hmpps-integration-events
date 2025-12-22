package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import com.fasterxml.jackson.core.JsonParseException
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
import org.springframework.messaging.support.GenericMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DeadLetterQueueService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.HmppsDomainEventService
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.CompletionException

/**
 * Solitary unit tests for [HmppsDomainEventsListener]
 */
class HmppsDomainEventsListenerTest : HmppsDomainEventsListenerTestCase(hmppsDomainEventService = mockk<HmppsDomainEventService>()) {

  @Test
  fun `when a valid SQS message (domain event) is received it should call the hmppsDomainEventService`() {
    val rawMessage = sqsNotificationHelper.generateRawHmppsDomainEvent()
    val hmppsDomainEvent = sqsNotificationHelper.createHmppsDomainEvent()

    every { hmppsDomainEventService.execute(hmppsDomainEvent, any()) } just runs

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent, any()) }
  }

  @Test
  fun `when an invalid SQS message (domain event) is received it should be sent to the dead letter queue`() {
    val rawMessage = "Invalid JSON message"

    assertThrows<JsonParseException> { hmppsDomainEventsListener.onDomainEvent(rawMessage) }

    verify { hmppsDomainEventService wasNot Called }
  }

  @Nested
  inner class GivenErrorOfEventExecution {
    private val error = IllegalStateException("Something went wrong")
    private val rawMessage = sqsNotificationHelper.generateRawHmppsDomainEvent()
    private val hmppsDomainEvent = sqsNotificationHelper.createHmppsDomainEvent()
    private val wrappedErrorMessage = "Error executing HmppsDomainEvent"

    @AfterEach
    internal fun tearDown() {
      clearAllMocks()
    }

    @Test
    fun `when there is CompletionException, the error cause shall be extracted and logged`() = onDomainEventShouldThrowError(
      wrappedError = CompletionException(wrappedErrorMessage, error),
    )

    @Test
    fun `when there is AsyncAdapterBlockingExecutionFailedException, the error cause shall be extracted and logged`() = onDomainEventShouldThrowError(
      wrappedError = AsyncAdapterBlockingExecutionFailedException(wrappedErrorMessage, error),
    )

    @Test
    fun `when there is ListenerExecutionFailedException, the error cause shall be extracted and logged`() = onDomainEventShouldThrowError(
      wrappedError = ListenerExecutionFailedException(wrappedErrorMessage, error, GenericMessage(rawMessage)),
    )

    private inline fun <reified T : Throwable> onDomainEventShouldThrowError(
      wrappedError: T,
      unwrappedError: Throwable = error,
    ) {
      // Arrange
      every { hmppsDomainEventService.execute(hmppsDomainEvent, any()) } throws wrappedError

      // Act, Assert (error)
      assertThrows<T> { hmppsDomainEventsListener.onDomainEvent(rawMessage) }

      // Assert (verify)
      verify(exactly = 1) { Sentry.captureException(match { it.message == unwrappedError.message }) }
    }
  }
}

/**
 * Base class for Solitary unit tests of [HmppsDomainEventsListener]
 */
abstract class HmppsDomainEventsListenerTestCase(
  protected val hmppsDomainEventService: HmppsDomainEventService,
  protected val deadLetterQueueService: DeadLetterQueueService = mockk<DeadLetterQueueService>(),
) {
  protected val hmppsDomainEventsListener = HmppsDomainEventsListener(hmppsDomainEventService, deadLetterQueueService)
  protected val currentTime: LocalDateTime = LocalDateTime.now()
  protected val zonedCurrentTime = currentTime.atZone(ZoneId.systemDefault())

  protected val sqsNotificationHelper by lazy { SqsNotificationGeneratingHelper(timestamp = zonedCurrentTime) }

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
  internal fun setup() {
    every { deadLetterQueueService.sendEvent(any(), any()) } returnsArgument 0
  }

  @AfterEach
  internal fun tearDown() {
    clearAllMocks()
  }
}
