package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PRISONER_OFFENDER_SEARCH_PRISONER_CREATED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PRISONER_OFFENDER_SEARCH_PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PROBATION_CASE_ENGAGEMENT_CREATED_MESSAGE
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PROBATION_CASE_PRISON_IDENTIFIER_ADDED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.generateHmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime
import java.time.ZoneId

@Configuration
@ActiveProfiles("test")
class HmppsDomainEventServiceTest {

  private val hmppsId = "AA1234A"
  private final val baseUrl = "https://dev.integration-api.hmpps.service.justice.gov.uk"

  private val eventNotificationRepository = mockk<EventNotificationRepository>()
  private val deadLetterQueueService = mockk<DeadLetterQueueService>()
  private val domainEventIdentitiesResolver = mockk<DomainEventIdentitiesResolver>()
  private val hmppsDomainEventService: HmppsDomainEventService = HmppsDomainEventService(
    eventNotificationRepository,
    deadLetterQueueService,
    domainEventIdentitiesResolver,
    baseUrl,
  )
  private val currentTime: LocalDateTime = LocalDateTime.now()
  private val zonedCurrentDateTime = currentTime.atZone(ZoneId.systemDefault())

  @BeforeEach
  fun setup() {
    mockkStatic(LocalDateTime::class)
    every { domainEventIdentitiesResolver.getHmppsId(any()) } returns hmppsId
    every { domainEventIdentitiesResolver.getPrisonId(any()) } returns null
    every { LocalDateTime.now() } returns currentTime

    every { eventNotificationRepository.insertOrUpdate(any()) } returnsArgument 0
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
  ) {
    val event: HmppsDomainEvent =
      SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(eventType, registerTypeCode)

    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.valueOf(integrationEvent)))

    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.valueOf(integrationEvent),
          hmppsId = hmppsId,
          url = "$baseUrl/v1/persons/$hmppsId/$path",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

  @Test
  fun `process and save dynamic risks changed event`() {
    val event = SqsNotificationGeneratingHelper(zonedCurrentDateTime)
      .createHmppsDomainEvent("probation-case.registration.added", "RCCO")
    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.DYNAMIC_RISKS_CHANGED))
    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.DYNAMIC_RISKS_CHANGED,
          hmppsId = hmppsId,
          url = "$baseUrl/v1/persons/$hmppsId/risks/dynamic",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

  @Test
  fun `process and save mappa detail changed event`() {
    val event = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent()
    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.MAPPA_DETAIL_CHANGED))
    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.MAPPA_DETAIL_CHANGED,
          hmppsId = hmppsId,
          url = "$baseUrl/v1/persons/$hmppsId/risks/mappadetail",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

  @Test
  fun `process and save risk assessment scores rsr determined event`() {
    val event =
      SqsNotificationGeneratingHelper(zonedCurrentDateTime)
        .createHmppsDomainEvent(eventType = "risk-assessment.scores.rsr.determined")
    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.RISK_SCORE_CHANGED))
    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.RISK_SCORE_CHANGED,
          hmppsId = hmppsId,
          url = "$baseUrl/v1/persons/$hmppsId/risks/scores",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

  @Test
  fun `process and save probation case risk scores ogrs manual calculation event`() {
    val event = SqsNotificationGeneratingHelper(zonedCurrentDateTime)
      .createHmppsDomainEvent(eventType = "probation-case.risk-scores.ogrs.manual-calculation")
    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.RISK_SCORE_CHANGED))
    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.RISK_SCORE_CHANGED,
          hmppsId = hmppsId,
          url = "$baseUrl/v1/persons/$hmppsId/risks/scores",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

  @Test
  fun `process and save risk assessment scores ogrs determined event`() {
    val event = SqsNotificationGeneratingHelper(zonedCurrentDateTime)
      .createHmppsDomainEvent(eventType = "risk-assessment.scores.ogrs.determined")
    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.RISK_SCORE_CHANGED))
    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.RISK_SCORE_CHANGED,
          hmppsId = hmppsId,
          url = "$baseUrl/v1/persons/$hmppsId/risks/scores",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

  @Test
  fun `process and save prisoner released domain event message for event with message event type of CALCULATED_RELEASE_DATES_PRISONER_CHANGED`() {
    val hmppsMessage = """
      {"eventType":"calculate-release-dates.prisoner.changed","description":"Prisoners release dates have been re-calculated","additionalInformation":{"prisonerId":"mockNomisId","bookingId":1219387},"version":1,"occurredAt":"2024-08-13T14:15:16.460942253+01:00"}
    """.trimIndent()
    val event = generateHmppsDomainEvent("calculate-release-dates.prisoner.changed", hmppsMessage)

    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE))

    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE,
          hmppsId = hmppsId,
          url = "$baseUrl/v1/persons/$hmppsId/sentences/latest-key-dates-and-adjustments",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

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

    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.PERSON_STATUS_CHANGED))

    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.PERSON_STATUS_CHANGED,
          hmppsId = hmppsId,
          url = "$baseUrl/v1/persons/$hmppsId",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
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

    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.PERSON_PND_ALERTS_CHANGED))

    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.PERSON_PND_ALERTS_CHANGED,
          hmppsId = hmppsId,
          url = "$baseUrl/v1/pnd/persons/$hmppsId/alerts",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }
}
