package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import com.fasterxml.jackson.core.JsonParseException
import io.awspring.cloud.sqs.listener.AsyncAdapterBlockingExecutionFailedException
import io.awspring.cloud.sqs.listener.ListenerExecutionFailedException
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.messaging.support.GenericMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DeadLetterQueueService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DomainEventIdentitiesResolver
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.HmppsDomainEventService
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.CompletionException

/**
 * Sociable unit tests for [HmppsDomainEventsListener]
 */
class HmppsDomainEventsListenerTest : HmppsDomainEventsListenerTestCase() {
  private val crn = "X777776"

  @Test
  fun `when risk-assessment scores determined event is received, it should create event notification RISK_SCORE_CHANGED`() {
    val hmppsEventRawMessage = sqsNotificationHelper.generateRawHmppsDomainEventWithoutRegisterType(
      eventType = "risk-assessment.scores.determined",
      messageEventType = "risk-assessment.scores.ogrs.determined",
    )
    val expectedNotificationType = IntegrationEventType.RISK_SCORE_CHANGED
    assumeIdentities(hmppsId = crn)

    onDomainEventShouldCreateEventNotification(hmppsEventRawMessage, expectedNotificationType)
  }

  @ParameterizedTest
  @CsvSource(
    "probation-case.registration.added, ASFO, PROBATION_STATUS_CHANGED",
    "probation-case.registration.deleted, ASFO, PROBATION_STATUS_CHANGED",
    "probation-case.registration.deregistered, ASFO, PROBATION_STATUS_CHANGED",
    "probation-case.registration.updated, ASFO, PROBATION_STATUS_CHANGED",

    "probation-case.registration.added, WRSM, PROBATION_STATUS_CHANGED",
    "probation-case.registration.deleted, WRSM, PROBATION_STATUS_CHANGED",
    "probation-case.registration.deregistered, WRSM, PROBATION_STATUS_CHANGED",
    "probation-case.registration.updated, WRSM, PROBATION_STATUS_CHANGED",

    "probation-case.registration.added, RCCO, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, RCCO, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, RCCO, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, RCCO, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, RCPR, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, RCPR, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, RCPR, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, RCPR, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, RVAD, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, RVAD, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, RVAD, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, RVAD, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, STRG, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, STRG, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, STRG, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, STRG, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, AVIS, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, AVIS, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, AVIS, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, AVIS, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, WEAP, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, WEAP, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, WEAP, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, WEAP, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, RLRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, RLRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, RLRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, RLRH, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, RMRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, RMRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, RMRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, RMRH, DYNAMIC_RISKS_CHANGED",

    "probation-case.registration.added, RHRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deleted, RHRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.deregistered, RHRH, DYNAMIC_RISKS_CHANGED",
    "probation-case.registration.updated, RHRH, DYNAMIC_RISKS_CHANGED",
  )
  fun `will process and save a person status event`(
    eventType: String,
    registerTypeCode: String,
    integrationEvent: String,
  ) {
    val hmppsEventRawMessage = sqsNotificationHelper.generateRawHmppsDomainEvent(eventType, registerTypeCode = registerTypeCode)
    val expectedNotificationType = IntegrationEventType.valueOf(integrationEvent)
    assumeIdentities(hmppsId = crn)

    onDomainEventShouldCreateEventNotification(hmppsEventRawMessage, expectedNotificationType)
  }

  @Test
  fun `when a valid registration added sqs event is received, it should create event notification MAPPA_DETAIL_CHANGED`() {
    val hmppsEventRawMessage = sqsNotificationHelper.generateRawHmppsDomainEvent()
    val expectedNotificationType = IntegrationEventType.MAPPA_DETAIL_CHANGED
    assumeIdentities(hmppsId = crn)

    onDomainEventShouldCreateEventNotification(hmppsEventRawMessage, expectedNotificationType)
  }

  @Test
  fun `when a valid registration updated sqs event is received, it should create event notification MAPPA_DETAIL_CHANGED`() {
    val hmppsEventRawMessage = sqsNotificationHelper.generateRawHmppsDomainEvent("probation-case.registration.updated")
    val expectedNotificationType = IntegrationEventType.MAPPA_DETAIL_CHANGED
    assumeIdentities(hmppsId = crn)

    onDomainEventShouldCreateEventNotification(hmppsEventRawMessage, expectedNotificationType)
  }

  @Test
  fun `when an invalid SQS message (domain event) is received it should not create notification`() {
    val rawMessage = "Invalid JSON message"

    assertThrows<JsonParseException> { hmppsDomainEventsListener.onDomainEvent(rawMessage) }

    verify { eventNotificationRepository wasNot Called }
  }

  @Test
  fun `when an unexpected event type is received, it should not create event notification`() {
    val unexpectedEventType = "unexpected.event.type"
    val hmppsEventRawMessage = sqsNotificationHelper.generateRawHmppsDomainEvent(eventType = unexpectedEventType)
    assumeIdentities(hmppsId = crn)

    onDomainEventShouldNotCreateEventNotification(hmppsEventRawMessage)
  }

  @Test
  fun `will not process and save a domain registration event message of none MAPP type`() = onDomainEventShouldNotCreateEventNotification(
    hmppsEventRawMessage = sqsNotificationHelper.generateRawHmppsDomainEvent(registerTypeCode = "NOTMAPP"),
  )

  @Test
  fun `when alert event matches multiple filters using generator, both services should be called`() {
    val hmppsEventRawMessage = sqsNotificationHelper.generateRawHmppsDomainEventWithAlertCode(eventType = "person.alert.created", alertCode = "HA")
    val expectedNotificationTypes = arrayOf(
      IntegrationEventType.PERSON_PND_ALERTS_CHANGED,
      IntegrationEventType.PERSON_ALERTS_CHANGED,
    )
    assumeIdentities(hmppsId = crn)

    onDomainEventShouldCreateEventNotifications(hmppsEventRawMessage, *expectedNotificationTypes)
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
      every { domainEventIdentitiesResolver.getHmppsId(hmppsDomainEvent) } throws wrappedError

      // Act, Assert (error)
      assertThrows<T> { hmppsDomainEventsListener.onDomainEvent(rawMessage) }

      // Assert (verify)
      verify(exactly = 1) { Sentry.captureException(match { it.message == unwrappedError.message }) }
    }
  }

  @Test
  fun `when a valid SQS message (domain event) is received it should create notification`() {
    val rawMessage = sqsNotificationHelper.generateRawHmppsDomainEvent()
    val expectedEvent = IntegrationEventType.MAPPA_DETAIL_CHANGED
    assumeIdentities(hmppsId = crn)

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(exactly = 1) { eventNotificationRepository.insertOrUpdate(match { it.eventType == expectedEvent }) }
  }
}

/**
 * Base class for Sociable unit tests of [HmppsDomainEventsListener]
 */
abstract class HmppsDomainEventsListenerTestCase {
  companion object {
    protected val baseUrl = "https://dev.integration-api.hmpps.service.justice.gov.uk"

    @BeforeAll
    @JvmStatic
    internal fun setupAll() {
      mockkStatic(LocalDateTime::class)
      mockkStatic(Sentry::class)
    }

    @AfterAll
    @JvmStatic
    internal fun tearDownAll() {
      unmockkStatic(LocalDateTime::class)
      unmockkStatic(Sentry::class)
    }
  }

  protected val currentTime: LocalDateTime = LocalDateTime.now()
  protected val zonedCurrentTime: ZonedDateTime = currentTime.atZone(ZoneId.systemDefault())
  protected val sqsNotificationHelper by lazy { SqsNotificationGeneratingHelper(timestamp = zonedCurrentTime) }

  protected val deadLetterQueueService = mockk<DeadLetterQueueService>()
  protected val eventNotificationRepository = mockk<EventNotificationRepository>()
  protected val domainEventIdentitiesResolver = mockk<DomainEventIdentitiesResolver>()

  protected val hmppsDomainEventService = HmppsDomainEventService(eventNotificationRepository, deadLetterQueueService, domainEventIdentitiesResolver, baseUrl)
  protected val hmppsDomainEventsListener = HmppsDomainEventsListener(hmppsDomainEventService, deadLetterQueueService)

  @BeforeEach
  open fun setupEventTest() {
    every { LocalDateTime.now() } returns currentTime
    every { eventNotificationRepository.insertOrUpdate(any()) } returnsArgument 0

    every { deadLetterQueueService.sendEvent(any(), any()) } returnsArgument 0
  }

  @AfterEach
  fun cleanupEventTest() {
    clearAllMocks()
  }

  protected fun onDomainEventShouldCreateEventNotification(
    hmppsEventRawMessage: String,
    hmppsId: String,
    expectedNotificationType: IntegrationEventType,
  ) {
    assumeIdentities(hmppsId = hmppsId)
    onDomainEventShouldCreateEventNotifications(hmppsEventRawMessage, expectedNotificationType)
  }

  protected fun onDomainEventShouldCreateEventNotification(
    hmppsEventRawMessage: String,
    expectedNotificationType: IntegrationEventType,
  ) = onDomainEventShouldCreateEventNotifications(hmppsEventRawMessage, expectedNotificationType)

  protected fun onDomainEventShouldCreateEventNotifications(
    hmppsEventRawMessage: String,
    vararg expectedNotificationType: IntegrationEventType,
  ) {
    // Act
    hmppsDomainEventsListener.onDomainEvent(hmppsEventRawMessage)

    // Assert
    expectedNotificationType.forEach { expectedEventType ->
      verify(exactly = 1) { eventNotificationRepository.insertOrUpdate(match { it.eventType == expectedEventType }) }
    }
  }

  protected fun onDomainEventShouldNotCreateEventNotification(hmppsEventRawMessage: String) {
    // Act
    hmppsDomainEventsListener.onDomainEvent(hmppsEventRawMessage)

    // Assert
    verify { eventNotificationRepository wasNot Called }
    verify { deadLetterQueueService wasNot Called }
  }

  protected fun assumeIdentities(hmppsId: String? = null, prisonId: String? = null) {
    every { domainEventIdentitiesResolver.getHmppsId(any()) } returns hmppsId
    every { domainEventIdentitiesResolver.getPrisonId(any()) } returns prisonId
  }

  protected fun SQSMessage.domainEvent(): HmppsDomainEvent = sqsNotificationHelper.extractDomainEventFrom(this)
}
