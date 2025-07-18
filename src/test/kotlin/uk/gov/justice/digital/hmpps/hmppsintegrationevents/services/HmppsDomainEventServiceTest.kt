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
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
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

  @Test
  fun `process and save probation status changed event`() {
    val event = SqsNotificationGeneratingHelper(zonedCurrentDateTime)
      .createHmppsDomainEvent("probation-case.registration.added", "ASFO")
    hmppsDomainEventService.execute(event, listOf(IntegrationEventType.PROBATION_STATUS_CHANGED))
    verify(exactly = 1) {
      eventNotificationRepository.insertOrUpdate(
        EventNotification(
          eventType = IntegrationEventType.PROBATION_STATUS_CHANGED,
          hmppsId = "X777776",
          url = "$baseUrl/v1/persons/X777776/status-information",
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
          hmppsId = "X777776",
          url = "$baseUrl/v1/persons/X777776/risks/mappadetail",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

  @Test
  fun `will not process and save a domain registration event message with no CRN or no Nomis Number which requires a hmppsId`() {
    val event = SqsNotificationGeneratingHelper(zonedCurrentDateTime)
      .createHmppsDomainEvent(identifiers = "[{\"type\":\"PNC\",\"value\":\"2018/0123456X\"}]")
    val exception = assertThrows<NotFoundException> { hmppsDomainEventService.execute(event, listOf(IntegrationEventType.MAPPA_DETAIL_CHANGED)) }
    verify { eventNotificationRepository wasNot Called }
    assertThat(exception.message, equalTo("Identifier could not be found in domain event message"))
  }

  @Test
  fun `process and save domain registration event message with no CRN or no Nomis Number which doesn't require a hmppsId`() {
    val event = SqsNotificationGeneratingHelper(zonedCurrentDateTime)
      .createHmppsDomainEvent(identifiers = "[{\"type\":\"PNC\",\"value\":\"2018/0123456X\"}]")
    assertDoesNotThrow { hmppsDomainEventService.execute(event, listOf(IntegrationEventType.PRISONERS_CHANGED)) }
    verify(exactly = 1) { eventNotificationRepository.insertOrUpdate(any()) }
  }

  @Test
  fun `process and save risk assessment scores rsr determined event`() {
    val event = SqsNotificationGeneratingHelper(zonedCurrentDateTime)
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
          hmppsId = "X777776",
          url = "$baseUrl/v1/persons/X777776/risks/scores",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

  @Test
  fun `will not process and save a domain event message with no prisonId which requires a prisonId`() {
    val event = SqsNotificationGeneratingHelper(zonedCurrentDateTime)
      .createHmppsDomainEvent(identifiers = "[{\"type\":\"PNC\",\"value\":\"2018/0123456X\"}]")
    val exception = assertThrows<NotFoundException> { hmppsDomainEventService.execute(event, listOf(IntegrationEventType.PRISON_LOCATION_CHANGED)) }
    verify { eventNotificationRepository wasNot Called }
    assertThat(exception.message, equalTo("Prison ID could not be found in domain event message"))
  }

  @Test
  fun `will use prisonId if found on the domain event`() {
    val prisonId = "MDI"
    val event = SqsNotificationGeneratingHelper(zonedCurrentDateTime)
      .createHmppsDomainEventWithPrisonId(eventType = "assessment.summary.produced", prisonId = prisonId)
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
    val event = SqsNotificationGeneratingHelper(zonedCurrentDateTime)
      .createHmppsDomainEvent(eventType = "assessment.summary.produced")
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
  fun `process and save event message with no CRN and cannot find CRN by nomis number`() {
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
    val event = SqsNotificationGeneratingHelper(zonedCurrentDateTime)
      .createHmppsDomainEventWithReason(identifiers = "[{\"type\":\"CRN\",\"value\":\"$crn\"}]")
    every { probationIntegrationApiGateway.getPersonExists("X123456") } returns PersonExists(crn, false)
    val exception = assertThrows<NotFoundException> { hmppsDomainEventService.execute(event, listOf(IntegrationEventType.MAPPA_DETAIL_CHANGED)) }
    verify { eventNotificationRepository wasNot Called }
    assertThat(exception.message, equalTo("Person with crn $crn not found"))
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
          hmppsId = mockCrn,
          url = "$baseUrl/v1/persons/$mockCrn/sentences/latest-key-dates-and-adjustments",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

  @Test
  fun `process and save prisoner released domain event message for event with message with reason is RELEASED`() {
    val event = SqsNotificationGeneratingHelper(zonedCurrentDateTime)
      .createHmppsDomainEventWithReason(eventType = "prison-offender-events.prisoner.released", reason = "RELEASED", identifiers = "[{\"type\":\"nomsNumber\",\"value\":\"$mockNomisId\"}]")
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
  fun `process probation case engagement created event`() {
    val eventType = "probation-case.engagement.created"
    val message = PROBATION_CASE_ENGAGEMENT_CREATED_MESSAGE.replace("\\", "")
    val event = generateHmppsDomainEvent(eventType, message)
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

  @Test
  fun `process probation case prison identifier added event`() {
    val eventType = "probation-case.prison-identifier.added"
    val message = PROBATION_CASE_PRISON_IDENTIFIER_ADDED.replace("\\", "")
    val event = generateHmppsDomainEvent(eventType, message)
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

  @Test
  fun `process prisoner offender search prisoner created event`() {
    val eventType = "prisoner-offender-search.prisoner.created"
    val message = PRISONER_OFFENDER_SEARCH_PRISONER_CREATED.replace("\\", "")
    val event = generateHmppsDomainEvent(eventType, message)
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

  @Test
  fun `process prisoner offender search prisoner updated event`() {
    val eventType = "prisoner-offender-search.prisoner.updated"
    val message = PRISONER_OFFENDER_SEARCH_PRISONER_UPDATED.replace("\\", "")
    val event = generateHmppsDomainEvent(eventType, message)
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

  @Test
  fun `process person alert changed event`() {
    val eventType = "person.alert.changed"
    val message = """
      {"eventType":"$eventType","additionalInformation":{"alertUuid":"8339dd96-4a02-4d5b-bc78-4eda22f678fa","alertCode":"BECTER","source":"NOMIS"},"version":1,"description":"An alert has been created in the alerts service","occurredAt":"2024-08-12T19:48:12.771347283+01:00","detailUrl":"https://alerts-api.hmpps.service.justice.gov.uk/alerts/8339dd96-4a02-4d5b-bc78-4eda22f678fa","personReference":{"identifiers":[{"type":"NOMS","value":"A1234BC"}]}}
    """.trimIndent()
    val event = generateHmppsDomainEvent(eventType, message)
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

  @Test
  fun `process person alert deleted event`() {
    val eventType = "person.alert.deleted"
    val message = """
      {"eventType":"$eventType","additionalInformation":{"alertUuid":"8339dd96-4a02-4d5b-bc78-4eda22f678fa","alertCode":"BECTER","source":"NOMIS"},"version":1,"description":"An alert has been created in the alerts service","occurredAt":"2024-08-12T19:48:12.771347283+01:00","detailUrl":"https://alerts-api.hmpps.service.justice.gov.uk/alerts/8339dd96-4a02-4d5b-bc78-4eda22f678fa","personReference":{"identifiers":[{"type":"NOMS","value":"A1234BC"}]}}
    """.trimIndent()
    val event = generateHmppsDomainEvent(eventType, message)
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

  @Test
  fun `process person alert updated event`() {
    val eventType = "person.alert.updated"
    val message = """
      {"eventType":"$eventType","additionalInformation":{"alertUuid":"8339dd96-4a02-4d5b-bc78-4eda22f678fa","alertCode":"BECTER","source":"NOMIS"},"version":1,"description":"An alert has been created in the alerts service","occurredAt":"2024-08-12T19:48:12.771347283+01:00","detailUrl":"https://alerts-api.hmpps.service.justice.gov.uk/alerts/8339dd96-4a02-4d5b-bc78-4eda22f678fa","personReference":{"identifiers":[{"type":"NOMS","value":"A1234BC"}]}}
    """.trimIndent()
    val event = generateHmppsDomainEvent(eventType, message)
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
