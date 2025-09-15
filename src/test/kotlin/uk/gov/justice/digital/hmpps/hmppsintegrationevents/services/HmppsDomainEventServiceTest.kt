package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PRISONER_OFFENDER_SEARCH_PRISONER_CREATED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PRISONER_OFFENDER_SEARCH_PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PROBATION_CASE_ENGAGEMENT_CREATED_MESSAGE
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PROBATION_CASE_PRISON_IDENTIFIER_ADDED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.generateHmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.registration.HmppsDomainEventMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime
import java.time.ZoneId

@Configuration
@ActiveProfiles("test")
class HmppsDomainEventServiceTest {

  private val hmppsId = "X777776"
  private val prisonId = "MDI"
  private final val baseUrl = "https://dev.integration-api.hmpps.service.justice.gov.uk"
  private final val objectMapper = ObjectMapper()

  private val eventNotificationRepository = mockk<EventNotificationRepository>()
  private val deadLetterQueueService = mockk<DeadLetterQueueService>()
  private val integrationEventCreationStrategyProvider = mockk<IntegrationEventCreationStrategyProvider>()
  private val defaultEventCreationStrategy = mockk<DefaultEventCreationStrategy>()
  private val hmppsDomainEventService: HmppsDomainEventService = HmppsDomainEventService(
    eventNotificationRepository,
    deadLetterQueueService,
    integrationEventCreationStrategyProvider,
    baseUrl,
  )
  private val currentTime: LocalDateTime = LocalDateTime.now()
  private val zonedCurrentDateTime = currentTime.atZone(ZoneId.systemDefault())
  private val mockNomisId = "mockNomisId"

  @BeforeEach
  fun setup() {
    mockkStatic(LocalDateTime::class)
    every { LocalDateTime.now() } returns currentTime
    every { eventNotificationRepository.existsByHmppsIdAndEventType(any(), any()) } returns false
    every { eventNotificationRepository.updateLastModifiedDateTimeByHmppsIdAndEventType(any(), any(), any()) } returns 1
    every { eventNotificationRepository.insertOrUpdate(any()) } returnsArgument 0
    every { deadLetterQueueService.sendEvent(any(), any()) } returnsArgument 0
    every { integrationEventCreationStrategyProvider.forEventType(any()) } returns defaultEventCreationStrategy

    every { defaultEventCreationStrategy.createNotifications(any(), any(), any()) } answers {
      val hmppsDomainEventMessage = arg<HmppsDomainEventMessage>(0)
      val integrationEventType = arg<IntegrationEventType>(1)
      val baseUrl = arg<String>(2)

      val additionalInfo = hmppsDomainEventMessage.additionalInformation

      listOf(
        EventNotification(
          eventType = integrationEventType,
          hmppsId = hmppsId,
          url = "$baseUrl/${integrationEventType.path(hmppsId, prisonId, additionalInfo)}",
          lastModifiedDateTime = LocalDateTime.now(),
        ),
      )
    }
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
      integrationEventCreationStrategyProvider.forEventType(eventType)
    }
    verify(exactly = 1) {
      defaultEventCreationStrategy.createNotifications(
        objectMapper.readValue(event.message),
        IntegrationEventType.valueOf(integrationEvent),
        baseUrl,
      )
    }
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
          hmppsId = "X777776",
          url = "$baseUrl/v1/persons/X777776/risks/dynamic",
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
          hmppsId = "X777776",
          url = "$baseUrl/v1/persons/X777776/risks/scores",
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
          hmppsId = "X777776",
          url = "$baseUrl/v1/persons/X777776/risks/scores",
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
  fun `will use prisonId if found on the domain event`() {
    val prisonId = "MDI"
    val event =
      SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEventWithPrisonId(
        eventType = "assessment.summary.produced",
        prisonId = prisonId,
      )
    mockStrategy(prisonId, hmppsId)
    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.RISK_SCORE_CHANGED))
    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.RISK_SCORE_CHANGED,
          hmppsId = hmppsId,
          prisonId = prisonId,
          url = "$baseUrl/v1/persons/$hmppsId/risks/scores",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
    verify(exactly = 0) {
      eventNotificationRepository.updateLastModifiedDateTimeByHmppsIdAndEventType(
        currentTime,
        hmppsId,
        IntegrationEventType.RISK_SCORE_CHANGED,
      )
    }
  }

  @Test
  fun `will get the prison ID from the getPrisonIdService`() {
    val prisonId = "MDI"
    val hmppsId = hmppsId
    mockStrategy(prisonId, hmppsId)
    val event =
      SqsNotificationGeneratingHelper(zonedCurrentDateTime)
        .createHmppsDomainEvent(eventType = "assessment.summary.produced")
    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.RISK_SCORE_CHANGED))
    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.RISK_SCORE_CHANGED,
          hmppsId = hmppsId,
          prisonId = prisonId,
          url = "$baseUrl/v1/persons/$hmppsId/risks/scores",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
    verify(exactly = 0) {
      eventNotificationRepository.updateLastModifiedDateTimeByHmppsIdAndEventType(
        currentTime,
        hmppsId,
        IntegrationEventType.RISK_SCORE_CHANGED,
      )
    }
  }

  @Test
  fun `should throw exception when hmpps id not found`() {
    val crn = "NOT_FOUND_CRN"
    every {
      defaultEventCreationStrategy.createNotifications(
        any(),
        any(),
        any(),
      )
    } throws NotFoundException("Person with crn $crn not found")

    val event: HmppsDomainEvent =
      SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(eventType = "assessment.summary.produced")

    assertThatThrownBy {
      hmppsDomainEventService.execute(event, listOf(IntegrationEventType.RISK_SCORE_CHANGED))
    }.isInstanceOf(NotFoundException::class.java)
      .hasMessage("Person with crn $crn not found")
  }

  private fun mockStrategy(prisonId: String? = null, hmppsId: String) {
    every { defaultEventCreationStrategy.createNotifications(any(), any(), any()) } answers {
      val hmppsDomainEventMessage = arg<HmppsDomainEventMessage>(0)
      val integrationEventType = arg<IntegrationEventType>(1)
      val baseUrl = arg<String>(2)
      val additionalInfo = hmppsDomainEventMessage.additionalInformation
      listOf(
        EventNotification(
          eventType = integrationEventType,
          hmppsId = hmppsId,
          prisonId = prisonId,
          url = "$baseUrl/${integrationEventType.path(hmppsId, prisonId, additionalInfo)}",
          lastModifiedDateTime = LocalDateTime.now(),
        ),
      )
    }
  }

  @Test
  fun `process and save prisoner released domain event message for event with message event type of CALCULATED_RELEASE_DATES_PRISONER_CHANGED`() {
    val hmppsMessage = """
      {"eventType":"calculate-release-dates.prisoner.changed","description":"Prisoners release dates have been re-calculated","additionalInformation":{"prisonerId":"$mockNomisId","bookingId":1219387},"version":1,"occurredAt":"2024-08-13T14:15:16.460942253+01:00"}
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

    mockStrategy(hmppsId = "X777776")

    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.PERSON_PND_ALERTS_CHANGED))

    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.PERSON_PND_ALERTS_CHANGED,
          hmppsId = "X777776",
          url = "$baseUrl/v1/pnd/persons/X777776/alerts",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }
}
