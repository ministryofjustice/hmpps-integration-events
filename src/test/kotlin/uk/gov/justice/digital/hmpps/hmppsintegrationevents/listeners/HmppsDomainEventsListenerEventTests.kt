package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DeadLetterQueueService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DomainEventIdentitiesResolver
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.HmppsDomainEventService
import java.time.LocalDateTime

/**
 * Sociable unit tests for [HmppsDomainEventsListener]
 */
class HmppsDomainEventsListenerEventTest : HmppsDomainEventsListenerEventTestCase() {
  private val crn = "X777776"

  @Nested
  @DisplayName("Given an expected event and a person")
  inner class GivenExpectedEventAndPerson {
    private val hmppsId = crn

    @BeforeEach
    internal fun setUp() {
      assumeIdentities(hmppsId)
    }

    // From: uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.HmppsDomainEventsListenerTest.when a valid registration added sqs event is received it should call the hmppsDomainEventService
    @Test
    fun `when a valid registration added sqs event is received, it should create event notification MAPPA_DETAIL_CHANGED`() = onDomainEventShouldCreateEventNotification(
      hmppsEventRawMessage = sqsNotificationHelper.generateRawHmppsDomainEvent(),
      expectedNotificationType = IntegrationEventType.MAPPA_DETAIL_CHANGED,
    )

    // From: uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.HmppsDomainEventsListenerTest.when a valid registration updated sqs event is received it should call the hmppsDomainEventService
    @Test
    fun `when a valid registration updated sqs event is received, it should create event notification MAPPA_DETAIL_CHANGED`() = onDomainEventShouldCreateEventNotification(
      hmppsEventRawMessage = sqsNotificationHelper.generateRawHmppsDomainEvent("probation-case.registration.updated"),
      IntegrationEventType.MAPPA_DETAIL_CHANGED,
    )

    // From: uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.HmppsDomainEventsListenerTest.when risk-assessment scores determined event is received it should call the hmppsDomainEventService
    @Test
    fun `when risk-assessment scores determined event is received, it should create event notification RISK_SCORE_CHANGED`() = onDomainEventShouldCreateEventNotification(
      hmppsEventRawMessage = sqsNotificationHelper.generateRawHmppsDomainEventWithoutRegisterType(
        eventType = "risk-assessment.scores.determined",
        messageEventType = "risk-assessment.scores.ogrs.determined",
      ),
      expectedNotificationType = IntegrationEventType.RISK_SCORE_CHANGED,
    )

    // From: uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.HmppsDomainEventsListenerTest#will process and save a person status event
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
    ) = onDomainEventShouldCreateEventNotification(
      hmppsEventRawMessage = sqsNotificationHelper.generateRawHmppsDomainEvent(eventType, registerTypeCode = registerTypeCode),
      expectedNotificationType = IntegrationEventType.valueOf(integrationEvent),
    )

    // From: uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.HmppsDomainEventsListenerTest.when alert event matches multiple filters using generator, both services should be called
    @Test
    fun `when alert event matches multiple filters using generator, both services should be called`() = onDomainEventShouldCreateEventNotifications(
      hmppsEventRawMessage = sqsNotificationHelper.generateRawHmppsDomainEventWithAlertCode(eventType = "person.alert.created", alertCode = "HA"),
      IntegrationEventType.PERSON_PND_ALERTS_CHANGED,
      IntegrationEventType.PERSON_ALERTS_CHANGED,
    )
  }

  @Nested
  @DisplayName("Given an unexpected event")
  inner class GivenAnUnexpectedDomainEvent {
    // From: uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.HmppsDomainEventsListenerTest.when an unexpected event type is received it should be sent to the dead letter queue
    @Test
    fun `when an unexpected event type is received, it should not create event notification`() = onDomainEventShouldNotCreateEventNotification(
      hmppsEventRawMessage = sqsNotificationHelper.generateRawHmppsDomainEvent(eventType = "unexpected.event.type"),
    )

    // From: uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.HmppsDomainEventsListenerTest.will not process and save a domain registration event message of none MAPP type
    @Test
    fun `will not process and save a domain registration event message of none MAPP type`() = onDomainEventShouldNotCreateEventNotification(
      hmppsEventRawMessage = sqsNotificationHelper.generateRawHmppsDomainEvent(registerTypeCode = "NOTMAPP"),
    )
  }
}

/**
 * Base class for Sociable unit tests of [HmppsDomainEventsListener]
 */
abstract class HmppsDomainEventsListenerEventTestCase(
  protected val eventNotificationRepository: EventNotificationRepository = mockk<EventNotificationRepository>(),
  protected val domainEventIdentitiesResolver: DomainEventIdentitiesResolver = mockk<DomainEventIdentitiesResolver>(),
  deadLetterQueueService: DeadLetterQueueService = mockk<DeadLetterQueueService>(),
) : HmppsDomainEventsListenerTestCase(
  deadLetterQueueService = deadLetterQueueService,
  hmppsDomainEventService = HmppsDomainEventService(eventNotificationRepository, deadLetterQueueService, domainEventIdentitiesResolver, baseUrl),
) {
  companion object {
    protected val baseUrl = "https://dev.integration-api.hmpps.service.justice.gov.uk"

    @BeforeAll
    @JvmStatic
    internal fun setupAll() {
      mockkStatic(LocalDateTime::class)
    }

    @AfterAll
    @JvmStatic
    internal fun tearDownAll() {
      unmockkStatic(LocalDateTime::class)
    }
  }

  @BeforeEach
  open fun setupEventTest() {
    every { LocalDateTime.now() } returns currentTime
    every { eventNotificationRepository.insertOrUpdate(any()) } returnsArgument 0
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
}
