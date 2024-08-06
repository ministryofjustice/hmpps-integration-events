package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway.ProbationIntegrationApiGateway
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.PersonIdentifier
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventTypes
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime
import java.time.ZoneId

@Configuration
@ActiveProfiles("test")
class HmppsDomainEventServiceTest {

  private final val baseUrl = "https://dev.integration-api.hmpps.service.justice.gov.uk"

  private val repo = mockk<EventNotificationRepository>()
  private val deadLetterQueueService = mockk<DeadLetterQueueService>()
  private val probationIntegrationApiGateway = mockk<ProbationIntegrationApiGateway>()
  private val hmppsDomainEventService: HmppsDomainEventService = HmppsDomainEventService(repo = repo, deadLetterQueueService, probationIntegrationApiGateway, baseUrl)
  private val currentTime: LocalDateTime = LocalDateTime.now()
  private val zonedCurrentDateTime = currentTime.atZone(ZoneId.systemDefault())
  private val mockNomisId = "mockNomisId"
  private val mockCrn = "mockCrn"

  @BeforeEach
  fun setup() {
    mockkStatic(LocalDateTime::class)
    every { LocalDateTime.now() } returns currentTime
    every { repo.existsByHmppsIdAndEventType(any(), any()) } returns false
    every { repo.updateLastModifiedDateTimeByHmppsIdAndEventType(any(), any(), any()) } returns 1
    every { repo.save(any()) } returnsArgument 0
    every { deadLetterQueueService.sendEvent(any(), any()) } returnsArgument 0

    every { probationIntegrationApiGateway.getPersonIdentifier(mockNomisId) } returns PersonIdentifier(mockCrn, mockNomisId)
  }

  @ParameterizedTest
  @CsvSource(
    "probation-case.registration.added, ASFO, PROBATION_STATUS_CHANGED, status-information",
    "probation-case.registration.deleted, ASFO, PROBATION_STATUS_CHANGED, status-information",
    "probation-case.registration.deregistered, ASFO, PROBATION_STATUS_CHANGED, status-information",
    "probation-case.registration.updated, ASFO, PROBATION_STATUS_CHANGED, status-information",

    "probation-case.registration.added, WRSM, PROBATION_STATUS_CHANGED, status-information",
    "probation-case.registration.deleted, WRSM, PROBATION_STATUS_CHANGED, status-information",
    "probation-case.registration.deregistered, WRSM, PROBATION_STATUS_CHANGED, status-information",
    "probation-case.registration.updated, WRSM, PROBATION_STATUS_CHANGED, status-information",

    "probation-case.registration.added, RCCO, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.deleted, RCCO, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.deregistered, RCCO, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.updated, RCCO, DYNAMIC_RISKS_CHANGED, risks/dynamic",

    "probation-case.registration.added, RCPR, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.deleted, RCPR, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.deregistered, RCPR, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.updated, RCPR, DYNAMIC_RISKS_CHANGED, risks/dynamic",

    "probation-case.registration.added, RVAD, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.deleted, RVAD, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.deregistered, RVAD, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.updated, RVAD, DYNAMIC_RISKS_CHANGED, risks/dynamic",

    "probation-case.registration.added, STRG, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.deleted, STRG, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.deregistered, STRG, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.updated, STRG, DYNAMIC_RISKS_CHANGED, risks/dynamic",

    "probation-case.registration.added, AVIS, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.deleted, AVIS, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.deregistered, AVIS, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.updated, AVIS, DYNAMIC_RISKS_CHANGED, risks/dynamic",

    "probation-case.registration.added, WEAP, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.deleted, WEAP, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.deregistered, WEAP, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.updated, WEAP, DYNAMIC_RISKS_CHANGED, risks/dynamic",

    "probation-case.registration.added, RLRH, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.deleted, RLRH, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.deregistered, RLRH, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.updated, RLRH, DYNAMIC_RISKS_CHANGED, risks/dynamic",

    "probation-case.registration.added, RMRH, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.deleted, RMRH, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.deregistered, RMRH, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.updated, RMRH, DYNAMIC_RISKS_CHANGED, risks/dynamic",

    "probation-case.registration.added, RHRH, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.deleted, RHRH, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.deregistered, RHRH, DYNAMIC_RISKS_CHANGED, risks/dynamic",
    "probation-case.registration.updated, RHRH, DYNAMIC_RISKS_CHANGED, risks/dynamic",
  )
  fun `will process and save a person status event`(eventType: String, registerTypeCode: String, integrationEvent: String, path: String) {
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(eventType, registerTypeCode)

    hmppsDomainEventService.execute(event, IntegrationEventTypes.valueOf(integrationEvent))

    verify(exactly = 1) {
      repo.save(
        EventNotification(
          eventType = IntegrationEventTypes.valueOf(integrationEvent),
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

    hmppsDomainEventService.execute(event, IntegrationEventTypes.MAPPA_DETAIL_CHANGED)

    verify(exactly = 1) {
      repo.save(
        EventNotification(
          eventType = IntegrationEventTypes.MAPPA_DETAIL_CHANGED,
          hmppsId = "X777776",
          url = "$baseUrl/v1/persons/X777776/risks/mappadetail",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

  @Test
  fun `will not process and save a domain registration event message of none MAPP type`() {
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(registerTypeCode = "NOTMAPP")

    hmppsDomainEventService.execute(event, IntegrationEventTypes.MAPPA_DETAIL_CHANGED)

    verify { repo wasNot Called }
  }

  @Test
  fun `will not process and save a domain registration event message with no CRN or no Nomis Number`() {
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(identifiers = "[{\"type\":\"PNC\",\"value\":\"2018/0123456X\"}]")

    hmppsDomainEventService.execute(event, IntegrationEventTypes.MAPPA_DETAIL_CHANGED)

    verify { repo wasNot Called }
    verify(exactly = 1) { deadLetterQueueService.sendEvent(event, "CRN could not be found in registration event message") }
  }

  @Test
  fun `will update an events lastModifiedDate if a relevant event is already stored`() {
    every { repo.existsByHmppsIdAndEventType("X777776", IntegrationEventTypes.MAPPA_DETAIL_CHANGED) } returns true

    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent()

    hmppsDomainEventService.execute(event, IntegrationEventTypes.MAPPA_DETAIL_CHANGED)

    verify(exactly = 1) { repo.updateLastModifiedDateTimeByHmppsIdAndEventType(currentTime, "X777776", IntegrationEventTypes.MAPPA_DETAIL_CHANGED) }
  }

  @Test
  fun `will process and save a risk changed domain event message for event with message event type of RISK_OF_SERIOUS_RECIDIVISM`() {
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(eventType = "risk-assessment.scores.rsr.determined")

    hmppsDomainEventService.execute(event, IntegrationEventTypes.RISK_SCORE_CHANGED)

    verify(exactly = 1) {
      repo.save(
        EventNotification(
          eventType = IntegrationEventTypes.RISK_SCORE_CHANGED,
          hmppsId = "X777776",
          url = "$baseUrl/v1/persons/X777776/risks/scores",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

  @Test
  fun `will process and save a risk changed domain event message for event with message event type of OFFENDER_GROUP_RECONVICTION_SCALE`() {
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(eventType = "risk-assessment.scores.ogrs.determined")

    hmppsDomainEventService.execute(event, IntegrationEventTypes.RISK_SCORE_CHANGED)

    verify(exactly = 1) {
      repo.save(
        EventNotification(
          eventType = IntegrationEventTypes.RISK_SCORE_CHANGED,
          hmppsId = "X777776",
          url = "$baseUrl/v1/persons/X777776/risks/scores",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

  @Test
  fun `will process and save a risk changed domain event message for event with message event type of OFFENDER_GROUP_RECONVICTION_SCALE_MANUAL_CALCULATION`() {
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(eventType = "probation-case.risk-scores.ogrs.manual-calculation")

    hmppsDomainEventService.execute(event, IntegrationEventTypes.RISK_SCORE_CHANGED)

    verify(exactly = 1) {
      repo.save(
        EventNotification(
          eventType = IntegrationEventTypes.RISK_SCORE_CHANGED,
          hmppsId = "X777776",
          url = "$baseUrl/v1/persons/X777776/risks/scores",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

  @Test
  fun `will process and save a risk changed domain event message for event with message event type of ASSESSMENT_SUMMARY_PRODUCED`() {
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(eventType = "assessment.summary.produced")

    hmppsDomainEventService.execute(event, IntegrationEventTypes.RISK_SCORE_CHANGED)

    verify(exactly = 1) {
      repo.save(
        EventNotification(
          eventType = IntegrationEventTypes.RISK_SCORE_CHANGED,
          hmppsId = "X777776",
          url = "$baseUrl/v1/persons/X777776/risks/scores",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

  @Test
  fun `will process and save a risk changed domain event message for event with unkonwn message event type `() {
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(eventType = "someType")
    hmppsDomainEventService.execute(event, IntegrationEventTypes.RISK_SCORE_CHANGED)
    verify { repo wasNot Called }
  }

  @Test
  fun `will update an events lastModifiedDate if a relevant risk score changed event is already stored`() {
    every { repo.existsByHmppsIdAndEventType("X777776", IntegrationEventTypes.RISK_SCORE_CHANGED) } returns true

    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(eventType = "assessment.summary.produced")

    hmppsDomainEventService.execute(event, IntegrationEventTypes.RISK_SCORE_CHANGED)

    verify(exactly = 1) { repo.updateLastModifiedDateTimeByHmppsIdAndEventType(currentTime, "X777776", IntegrationEventTypes.RISK_SCORE_CHANGED) }
  }

  @Test
  fun `will not process and save a domain registration event message with no CRN and cannot find CRN by nomis number`() {
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEventWithReason(identifiers = "[{\"type\":\"nomisNumber\",\"value\":\"2018/0123456X\"}]")

    hmppsDomainEventService.execute(event, IntegrationEventTypes.MAPPA_DETAIL_CHANGED)

    verify { repo wasNot Called }
    verify(exactly = 1) { deadLetterQueueService.sendEvent(event, "CRN could not be found in registration event message") }
  }

  @Test
  fun `will process and save a prisoner released domain event message for event with message event type of CALCULATED_RELEASE_DATES_PRISONER_CHANGED`() {
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEventWithReason(eventType = "calculate-release-dates.prisoner.changed", reason = "RELEASED", identifiers = "[{\"type\":\"nomsNumber\",\"value\":\"$mockNomisId\"}]")

    hmppsDomainEventService.execute(event, IntegrationEventTypes.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE)

    verify(exactly = 1) {
      repo.save(
        EventNotification(
          eventType = IntegrationEventTypes.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE,
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

    hmppsDomainEventService.execute(event, IntegrationEventTypes.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE)

    verify(exactly = 1) {
      repo.save(
        EventNotification(
          eventType = IntegrationEventTypes.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE,
          hmppsId = mockCrn,
          url = "$baseUrl/v1/persons/$mockCrn/sentences/latest-key-dates-and-adjustments",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }
}
