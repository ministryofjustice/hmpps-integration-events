package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway.ProbationIntegrationApiGateway
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PRISONER_OFFENDER_SEARCH_PRISONER_CREATED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PRISONER_OFFENDER_SEARCH_PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PROBATION_CASE_ENGAGEMENT_CREATED_MESSAGE
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PROBATION_CASE_PRISON_IDENTIFIER_ADDED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.generateHmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.PersonExists
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.PersonIdentifier
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime
import java.time.ZoneId

@Configuration
@ActiveProfiles("test")
class HmppsDomainEventServiceTest {

  private final val baseUrl = "https://dev.integration-api.hmpps.service.justice.gov.uk"

  private val eventNotificationRepository = mockk<EventNotificationRepository>()
  private val deadLetterQueueService = mockk<DeadLetterQueueService>()
  private val probationIntegrationApiGateway = mockk<ProbationIntegrationApiGateway>()
  private val getPrisonIdService = mockk<GetPrisonIdService>()
  private val hmppsDomainEventService: HmppsDomainEventService = HmppsDomainEventService(eventNotificationRepository, deadLetterQueueService, probationIntegrationApiGateway, getPrisonIdService, baseUrl)
  private val currentTime: LocalDateTime = LocalDateTime.now()
  private val zonedCurrentDateTime = currentTime.atZone(ZoneId.systemDefault())
  private val mockNomisId = "mockNomisId"
  private val mockCrn = "mockCrn"

  @BeforeEach
  fun setup() {
    mockkStatic(LocalDateTime::class)
    every { LocalDateTime.now() } returns currentTime
    every { eventNotificationRepository.existsByHmppsIdAndEventType(any(), any()) } returns false
    every { eventNotificationRepository.updateLastModifiedDateTimeByHmppsIdAndEventType(any(), any(), any()) } returns 1
    every { eventNotificationRepository.insertOrUpdate(any()) } returnsArgument 0
    every { deadLetterQueueService.sendEvent(any(), any()) } returnsArgument 0

    every { probationIntegrationApiGateway.getPersonIdentifier(mockNomisId) } returns PersonIdentifier(mockCrn, mockNomisId)
    every { probationIntegrationApiGateway.getPersonExists("X777776") } returns PersonExists("X777776", true)

    every { getPrisonIdService.execute(mockNomisId) } returns null
    every { getPrisonIdService.execute("A1234BC") } returns null
  }

  @ParameterizedTest
  @CsvSource(
    "probation-case.registration.added, ASFO, PROBATION_STATUS_CHANGED, status-information",
    "probation-case.registration.added, RCCO, DYNAMIC_RISKS_CHANGED, risks/dynamic",
  )
  fun `will process and save a person status event`(eventType: String, registerTypeCode: String, integrationEvent: String, path: String) {
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(eventType, registerTypeCode)

    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.valueOf(integrationEvent)))

    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.valueOf(integrationEvent),
          hmppsId = "X777776",
          url = "$baseUrl/v1/persons/X777776/$path",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

  @Test
  fun `will process and save a mapps domain registration event message`() {
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent()

    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.MAPPA_DETAIL_CHANGED))

    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.MAPPA_DETAIL_CHANGED,
          hmppsId = "X777776",
          url = "$baseUrl/v1/persons/X777776/risks/mappadetail",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

  @Test
  fun `will not process and save a domain registration event message with no CRN or no Nomis Number`() {
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(identifiers = "[{\"type\":\"PNC\",\"value\":\"2018/0123456X\"}]")

    val exception = assertThrows<NotFoundException> { hmppsDomainEventService.execute(event, listOf(IntegrationEventType.MAPPA_DETAIL_CHANGED)) }

    verify { eventNotificationRepository wasNot Called }
    assertThat(exception.message, equalTo("Identifier could not be found in domain event message ${event.messageId}"))
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "risk-assessment.scores.rsr.determined",
      "probation-case.risk-scores.ogrs.manual-calculation",
      "risk-assessment.scores.ogrs.determined",
    ],
  )
  fun `will process and save a risk changed domain event message`(eventType: String) {
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(eventType = eventType)

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
  fun `will use the prison ID if it is found on the domain event`() {
    val prisonId = "MDI"
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEventWithPrisonId(eventType = "assessment.summary.produced", prisonId = prisonId)

    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.RISK_SCORE_CHANGED))

    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.RISK_SCORE_CHANGED,
          hmppsId = "X777776",
          prisonId = prisonId,
          url = "$baseUrl/v1/persons/X777776/risks/scores",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
    verify(exactly = 0) { getPrisonIdService.execute(any()) }
    verify(exactly = 0) { eventNotificationRepository.updateLastModifiedDateTimeByHmppsIdAndEventType(currentTime, "X777776", IntegrationEventType.RISK_SCORE_CHANGED) }
  }

  @Test
  fun `will get the prison ID from the getPrisonIdService`() {
    val prisonId = "MDI"
    every { getPrisonIdService.execute("A1234BC") } returns prisonId
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(eventType = "assessment.summary.produced")

    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.RISK_SCORE_CHANGED))

    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.RISK_SCORE_CHANGED,
          hmppsId = "X777776",
          prisonId = prisonId,
          url = "$baseUrl/v1/persons/X777776/risks/scores",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
    verify(exactly = 0) { eventNotificationRepository.updateLastModifiedDateTimeByHmppsIdAndEventType(currentTime, "X777776", IntegrationEventType.RISK_SCORE_CHANGED) }
  }

  @Test
  fun `will process and save event message with no CRN and cannot find CRN by nomis number`() {
    every { probationIntegrationApiGateway.getPersonIdentifier(mockNomisId) } returns null

    val hmppsMessage = """
      {"eventType":"calculate-release-dates.prisoner.changed","description":"Prisoners release dates have been re-calculated","additionalInformation":{"prisonerId":"$mockNomisId","bookingId":1219387},"version":1,"occurredAt":"2024-08-13T14:15:16.460942253+01:00"}
    """.trimIndent()

    val event = generateHmppsDomainEvent("calculate-release-dates.prisoner.changed", hmppsMessage)

    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE))

    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE,
          hmppsId = mockNomisId,
          url = "$baseUrl/v1/persons/$mockNomisId/sentences/latest-key-dates-and-adjustments",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

  @Test
  fun `will not process and save a domain registration event message where CRN does not exist in delius`() {
    val crn = "X123456"
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEventWithReason(identifiers = "[{\"type\":\"CRN\",\"value\":\"$crn\"}]")

    every { probationIntegrationApiGateway.getPersonExists("X123456") } returns PersonExists(crn, false)
    val exception = assertThrows<NotFoundException> { hmppsDomainEventService.execute(event, listOf(IntegrationEventType.MAPPA_DETAIL_CHANGED)) }

    verify { eventNotificationRepository wasNot Called }
    assertThat(exception.message, equalTo("Person with crn $crn not found"))
  }

  @Test
  fun `will process and save a prisoner released domain event message for event with message event type of CALCULATED_RELEASE_DATES_PRISONER_CHANGED`() {
    val hmppsMessage = """
      {"eventType":"calculate-release-dates.prisoner.changed","description":"Prisoners release dates have been re-calculated","additionalInformation":{"prisonerId":"$mockNomisId","bookingId":1219387},"version":1,"occurredAt":"2024-08-13T14:15:16.460942253+01:00"}
    """.trimIndent()

    val event = generateHmppsDomainEvent("calculate-release-dates.prisoner.changed", hmppsMessage)

    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE))

    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE,
          hmppsId = mockCrn,
          url = "$baseUrl/v1/persons/$mockCrn/sentences/latest-key-dates-and-adjustments",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

  @Test
  fun `will process and save a prisoner released domain event message for event with message with reason is RELEASED`() {
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEventWithReason(eventType = "prison-offender-events.prisoner.released", reason = "RELEASED", identifiers = "[{\"type\":\"nomsNumber\",\"value\":\"$mockNomisId\"}]")

    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE))

    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE,
          hmppsId = mockCrn,
          url = "$baseUrl/v1/persons/$mockCrn/sentences/latest-key-dates-and-adjustments",
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

    every { probationIntegrationApiGateway.getPersonIdentifier("A1234BC") } returns PersonIdentifier("X777776", "A1234BC")
    every { getPrisonIdService.execute("A1234BC") } returns null
    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.PERSON_STATUS_CHANGED))

    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.PERSON_STATUS_CHANGED,
          hmppsId = "X777776",
          url = "$baseUrl/v1/persons/X777776",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "person.alert.changed",
      "person.alert.changed",
      "person.alert.deleted",
      "person.alert.updated",
    ],
  )
  fun `will process and save a pnd alert for person alert changed event `(eventType: String) {
    val message = """
      {\"eventType\":\"$eventType\",\"additionalInformation\":{\"alertUuid\":\"8339dd96-4a02-4d5b-bc78-4eda22f678fa\",\"alertCode\":\"BECTER\",\"source\":\"NOMIS\"},\"version\":1,\"description\":\"An alert has been created in the alerts service\",\"occurredAt\":\"2024-08-12T19:48:12.771347283+01:00\",\"detailUrl\":\"https://alerts-api.hmpps.service.justice.gov.uk/alerts/8339dd96-4a02-4d5b-bc78-4eda22f678fa\",\"personReference\":{\"identifiers\":[{\"type\":\"NOMS\",\"value\":\"A1234BC\"}]}}
    """.trimIndent()

    val hmppsMessage = message.replace("\\", "")
    val event = generateHmppsDomainEvent(eventType, hmppsMessage)

    every { probationIntegrationApiGateway.getPersonIdentifier("A1234BC") } returns PersonIdentifier("X777776", "A1234BC")
    every { getPrisonIdService.execute("A1234BC") } returns null
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
