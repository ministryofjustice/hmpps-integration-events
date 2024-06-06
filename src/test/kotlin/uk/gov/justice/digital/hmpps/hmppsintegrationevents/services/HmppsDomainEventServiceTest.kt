package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventTypes
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.RiskScoreTypes
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
  private val hmppsDomainEventService: HmppsDomainEventService = HmppsDomainEventService(repo = repo, deadLetterQueueService, baseUrl)
  private val currentTime: LocalDateTime = LocalDateTime.now()
  private val zonedCurrentDateTime = currentTime.atZone(ZoneId.systemDefault())

  @BeforeEach
  fun setup() {
    mockkStatic(LocalDateTime::class)
    every { LocalDateTime.now() } returns currentTime
    every { repo.existsByHmppsIdAndEventType(any(), any()) } returns false
    every { repo.updateLastModifiedDateTimeByHmppsIdAndEventType(any(), any(), any()) } returns 1
    every { repo.save(any()) } returnsArgument 0
    every { deadLetterQueueService.sendEvent(any(), any()) } returnsArgument 0
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
  fun `will not process and save a domain registration event message with no CRN`() {
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
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(eventType = RiskScoreTypes.RISK_OF_SERIOUS_RECIDIVISM.code)

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
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(eventType = RiskScoreTypes.OFFENDER_GROUP_RECONVICTION_SCALE.code)

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
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(eventType = RiskScoreTypes.OFFENDER_GROUP_RECONVICTION_SCALE_MANUAL_CALCULATION.code)

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
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(eventType = RiskScoreTypes.ASSESSMENT_SUMMARY_PRODUCED.code)

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

    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(eventType = RiskScoreTypes.ASSESSMENT_SUMMARY_PRODUCED.code)

    hmppsDomainEventService.execute(event, IntegrationEventTypes.RISK_SCORE_CHANGED)

    verify(exactly = 1) { repo.updateLastModifiedDateTimeByHmppsIdAndEventType(currentTime, "X777776", IntegrationEventTypes.RISK_SCORE_CHANGED) }
  }
}
