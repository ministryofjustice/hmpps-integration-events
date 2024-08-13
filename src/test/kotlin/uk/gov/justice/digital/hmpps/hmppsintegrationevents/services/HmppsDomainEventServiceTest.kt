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
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PROBATION_CASE_ENGAGEMENT_CREATED_MESSAGE
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.generateHmppsDomainEvent
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
    "probation-case.registration.added, RCCO, DYNAMIC_RISKS_CHANGED, risks/dynamic",
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
  fun `will not process and save a domain registration event message with no CRN or no Nomis Number`() {
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(identifiers = "[{\"type\":\"PNC\",\"value\":\"2018/0123456X\"}]")

    val exception = assertThrows<NotFoundException> { hmppsDomainEventService.execute(event, IntegrationEventTypes.MAPPA_DETAIL_CHANGED) }

    verify { repo wasNot Called }
    assertThat(exception.message, equalTo("Identifier could not be found in domain event message ${event.messageId}"))
  }

  @Test
  fun `will update an events lastModifiedDate if a relevant event is already stored`() {
    every { repo.existsByHmppsIdAndEventType("X777776", IntegrationEventTypes.MAPPA_DETAIL_CHANGED) } returns true

    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent()

    hmppsDomainEventService.execute(event, IntegrationEventTypes.MAPPA_DETAIL_CHANGED)

    verify(exactly = 1) { repo.updateLastModifiedDateTimeByHmppsIdAndEventType(currentTime, "X777776", IntegrationEventTypes.MAPPA_DETAIL_CHANGED) }
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "risk-assessment.scores.rsr.determined",
      "probation-case.risk-scores.ogrs.manual-calculation",
      "risk-assessment.scores.ogrs.determined",
      "assessment.summary.produced",
    ],
  )
  fun `will process and save a risk changed domain event message`(eventType: String) {
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(eventType = eventType)

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
  fun `will update an events lastModifiedDate if a relevant risk score changed event is already stored`() {
    every { repo.existsByHmppsIdAndEventType("X777776", IntegrationEventTypes.RISK_SCORE_CHANGED) } returns true

    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(eventType = "assessment.summary.produced")

    hmppsDomainEventService.execute(event, IntegrationEventTypes.RISK_SCORE_CHANGED)

    verify(exactly = 1) { repo.updateLastModifiedDateTimeByHmppsIdAndEventType(currentTime, "X777776", IntegrationEventTypes.RISK_SCORE_CHANGED) }
  }

  @Test
  fun `will not process and save a domain registration event message with no CRN and cannot find CRN by nomis number`() {
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEventWithReason(identifiers = "[{\"type\":\"nomsNumber\",\"value\":\"0123456X\"}]")

    every { probationIntegrationApiGateway.getPersonIdentifier("0123456X") } returns null
    val exception = assertThrows<NotFoundException> { hmppsDomainEventService.execute(event, IntegrationEventTypes.MAPPA_DETAIL_CHANGED) }

    verify { repo wasNot Called }
    assertThat(exception.message, equalTo("Person not found nomsNumber 0123456X"))
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

  @Test
  fun `process event processing for api persons {hmppsId} `() {
    val message = PROBATION_CASE_ENGAGEMENT_CREATED_MESSAGE

    val hmppsMessage = message.replace("\\", "")
    val event = generateHmppsDomainEvent("probation-case.engagement.created", hmppsMessage)

    every { probationIntegrationApiGateway.getPersonIdentifier("A1234BC") } returns PersonIdentifier("X777776", "A1234BC")
    hmppsDomainEventService.execute(event, IntegrationEventTypes.PERSON_STATUS_CHANGED)

    verify(exactly = 1) {
      repo.save(
        EventNotification(
          eventType = IntegrationEventTypes.PERSON_STATUS_CHANGED,
          hmppsId = "X777776",
          url = "$baseUrl/v1/persons/X777776",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }
}
