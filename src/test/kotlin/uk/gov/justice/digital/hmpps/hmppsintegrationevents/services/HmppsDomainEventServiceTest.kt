package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PRISONER_OFFENDER_SEARCH_PRISONER_CREATED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PRISONER_OFFENDER_SEARCH_PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PROBATION_CASE_ENGAGEMENT_CREATED_MESSAGE
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PROBATION_CASE_PRISON_IDENTIFIER_ADDED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.generateHmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.SQSMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime
import java.time.ZoneId

class HmppsDomainEventServiceTest : HmppsDomainEventServiceTestCase() {
  private val hmppsId = "AA1234A"

  @BeforeEach
  internal fun setup() {
    assumeIdentities(hmppsId = hmppsId, prisonId = null)
  }

  @ParameterizedTest
  @CsvSource(
    "probation-case.registration.added, ASFO, PROBATION_STATUS_CHANGED, status-information",
    "probation-case.registration.added, RCCO, DYNAMIC_RISKS_CHANGED, risks/dynamic",
  )
  fun `will process and save a person status event`(
    eventType: String,
    registerTypeCode: String,
    integrationEvent: String,
    path: String,
  ) = executeShouldSaveEventNotification(
    hmppsDomainEvent = sqsNotificationHelper.createHmppsDomainEvent(eventType, registerTypeCode),
    integrationEventType = IntegrationEventType.valueOf(integrationEvent),
    url = "$baseUrl/v1/persons/$hmppsId/$path",
    hmppsId = hmppsId,
  )

  @Test
  fun `process and save dynamic risks changed event`() = executeShouldSaveEventNotification(
    hmppsDomainEvent = sqsNotificationHelper.createHmppsDomainEvent("probation-case.registration.added", "RCCO"),
    integrationEventType = IntegrationEventType.DYNAMIC_RISKS_CHANGED,
    url = "$baseUrl/v1/persons/$hmppsId/risks/dynamic",
    hmppsId = hmppsId,
  )

  @Test
  fun `process and save mappa detail changed event`() = executeShouldSaveEventNotification(
    hmppsDomainEvent = sqsNotificationHelper.createHmppsDomainEvent(),
    integrationEventType = IntegrationEventType.MAPPA_DETAIL_CHANGED,
    hmppsId = hmppsId,
    url = "$baseUrl/v1/persons/$hmppsId/risks/mappadetail",
  )

  @Test
  fun `process and save risk assessment scores rsr determined event`() = executeShouldSaveEventNotification(
    hmppsDomainEvent = sqsNotificationHelper.createHmppsDomainEvent(eventType = "risk-assessment.scores.rsr.determined"),
    integrationEventType = IntegrationEventType.RISK_SCORE_CHANGED,
    hmppsId = hmppsId,
    url = "$baseUrl/v1/persons/$hmppsId/risks/scores",
  )

  @Test
  fun `process and save probation case risk scores ogrs manual calculation event`() = executeShouldSaveEventNotification(
    hmppsDomainEvent = sqsNotificationHelper.createHmppsDomainEvent(eventType = "probation-case.risk-scores.ogrs.manual-calculation"),
    integrationEventType = IntegrationEventType.RISK_SCORE_CHANGED,
    hmppsId = hmppsId,
    url = "$baseUrl/v1/persons/$hmppsId/risks/scores",
  )

  @Test
  fun `process and save risk assessment scores ogrs determined event`() = executeShouldSaveEventNotification(
    hmppsDomainEvent = sqsNotificationHelper.createHmppsDomainEvent(eventType = "risk-assessment.scores.ogrs.determined"),
    integrationEventType = IntegrationEventType.RISK_SCORE_CHANGED,
    hmppsId = hmppsId,
    url = "$baseUrl/v1/persons/$hmppsId/risks/scores",
  )

  @Test
  fun `process and save prisoner released domain event message for event with message event type of CALCULATED_RELEASE_DATES_PRISONER_CHANGED`() = executeShouldSaveEventNotification(
    hmppsDomainEvent = """
      {"eventType":"calculate-release-dates.prisoner.changed","description":"Prisoners release dates have been re-calculated","additionalInformation":{"prisonerId":"mockNomisId","bookingId":1219387},"version":1,"occurredAt":"2024-08-13T14:15:16.460942253+01:00"}
    """.trimIndent().let { generateHmppsDomainEvent("calculate-release-dates.prisoner.changed", it) },
    integrationEventType = IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE,
    hmppsId = hmppsId,
    url = "$baseUrl/v1/persons/$hmppsId/sentences/latest-key-dates-and-adjustments",
  )

  @ParameterizedTest
  @ValueSource(
    strings = [
      "probation-case.engagement.created",
      "probation-case.prison-identifier.added",
      "prisoner-offender-search.prisoner.created",
      "prisoner-offender-search.prisoner.updated",
    ],
  )
  fun `process event processing for api persons {hmppsId} `(eventType: String) {
    val message = when (eventType) {
      "probation-case.engagement.created" -> PROBATION_CASE_ENGAGEMENT_CREATED_MESSAGE
      "probation-case.prison-identifier.added" -> PROBATION_CASE_PRISON_IDENTIFIER_ADDED
      "prisoner-offender-search.prisoner.created" -> PRISONER_OFFENDER_SEARCH_PRISONER_CREATED
      "prisoner-offender-search.prisoner.updated" -> PRISONER_OFFENDER_SEARCH_PRISONER_UPDATED
      else -> throw RuntimeException("Unexpected event type: $eventType")
    }

    val hmppsMessage = message.replace("\\", "")
    val event = generateHmppsDomainEvent(eventType, hmppsMessage)

    executeShouldSaveEventNotification(
      hmppsDomainEvent = event,
      integrationEventType = IntegrationEventType.PERSON_STATUS_CHANGED,
      url = "$baseUrl/v1/persons/$hmppsId",
      hmppsId = hmppsId,
    )
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "person.alert.changed",
      "person.alert.deleted",
      "person.alert.updated",
      "person.alert.updated",
    ],
  )
  fun `process person alert changed event`(eventType: String) {
    val message = """
      {"eventType":"$eventType","additionalInformation":{"alertUuid":"8339dd96-4a02-4d5b-bc78-4eda22f678fa","alertCode":"BECTER","source":"NOMIS"},"version":1,"description":"An alert has been created in the alerts service","occurredAt":"2024-08-12T19:48:12.771347283+01:00","detailUrl":"https://alerts-api.hmpps.service.justice.gov.uk/alerts/8339dd96-4a02-4d5b-bc78-4eda22f678fa","personReference":{"identifiers":[{"type":"NOMS","value":"A1234BC"}]}}
    """.trimIndent()
    val event = generateHmppsDomainEvent(eventType, message)

    executeShouldSaveEventNotification(
      hmppsDomainEvent = event,
      integrationEventType = IntegrationEventType.PERSON_PND_ALERTS_CHANGED,
      url = "$baseUrl/v1/pnd/persons/$hmppsId/alerts",
      hmppsId = hmppsId,
    )
  }
}

abstract class HmppsDomainEventServiceTestCase {
  companion object {
    @JvmStatic
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

  protected val eventNotificationRepository = mockk<EventNotificationRepository>()
  protected val deadLetterQueueService = mockk<DeadLetterQueueService>()
  protected val domainEventIdentitiesResolver = mockk<DomainEventIdentitiesResolver>()
  protected val hmppsDomainEventService: HmppsDomainEventService = HmppsDomainEventService(
    eventNotificationRepository,
    deadLetterQueueService,
    domainEventIdentitiesResolver,
    baseUrl,
  )
  protected val currentTime: LocalDateTime = LocalDateTime.now()
  protected val zonedCurrentDateTime = currentTime.atZone(ZoneId.systemDefault())

  protected val sqsNotificationHelper by lazy { SqsNotificationGeneratingHelper(zonedCurrentDateTime) }

  @BeforeEach
  internal fun setupBase() {
    every { LocalDateTime.now() } returns currentTime
    every { eventNotificationRepository.insertOrUpdate(any()) } returnsArgument 0
  }

  @AfterEach
  internal fun teardownBase() {
    clearAllMocks()
  }

  protected fun generateEventNotification(
    eventType: IntegrationEventType,
    url: String,
    hmppsId: String,
  ) = EventNotification(eventType = eventType, hmppsId = hmppsId, url = url, lastModifiedDateTime = currentTime)

  protected fun assumeIdentities(hmppsId: String? = null, prisonId: String? = null) {
    every { domainEventIdentitiesResolver.getHmppsId(any()) } returns hmppsId
    every { domainEventIdentitiesResolver.getPrisonId(any()) } returns prisonId
  }

  protected fun executeShouldSaveEventNotification(
    hmppsDomainEvent: SQSMessage,
    integrationEventType: IntegrationEventType,
    url: String,
    hmppsId: String,
  ) = executeShouldSaveEventNotification(
    hmppsDomainEvent = hmppsDomainEvent,
    integrationEventType = integrationEventType,
    expectedEventNotification = generateEventNotification(eventType = integrationEventType, url = url, hmppsId = hmppsId),
  )

  protected fun executeShouldSaveEventNotification(
    hmppsDomainEvent: SQSMessage,
    integrationEventType: IntegrationEventType,
    expectedEventNotification: EventNotification,
  ) = executeShouldSaveEventNotifications(
    hmppsDomainEvent = hmppsDomainEvent,
    integrationEventTypes = listOf(integrationEventType),
    expectedEventNotifications = listOf(expectedEventNotification),
  )

  protected fun executeShouldSaveEventNotifications(
    hmppsDomainEvent: SQSMessage,
    integrationEventTypes: List<IntegrationEventType>,
    expectedEventNotifications: List<EventNotification>,
  ) {
    // Act
    hmppsDomainEventService.execute(hmppsDomainEvent, integrationEventTypes)

    // Assert
    expectedEventNotifications.forEach { expectedNotification ->
      // Verify all expected event notifications persisted via repository
      verify(exactly = 1) { eventNotificationRepository.insertOrUpdate(expectedNotification) }
    }
  }

  protected inline fun <reified T : Throwable> executeEventShouldThrowError(
    hmppsDomainEvent: SQSMessage,
    integrationEventTypes: List<IntegrationEventType>,
    error: T? = null,
  ) {
    // Act, Assert (error)
    val errorThrown = assertThrows<T> { hmppsDomainEventService.execute(hmppsDomainEvent, integrationEventTypes) }

    // Assert (verify)
    error?.let { assertEquals(it.message, errorThrown.message) }
  }
}
