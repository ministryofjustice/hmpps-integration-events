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
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IncomingEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.OutgoingEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime
import java.time.ZoneId

@Configuration
@ActiveProfiles("test")
class RegistrationEventsServiceTest {

  private final val baseUrl = "https://dev.integration-api.hmpps.service.justice.gov.uk"

  private val repo = mockk<EventNotificationRepository>()
  private val deadLetterQueueService = mockk<DeadLetterQueueService>()
  private val registrationEventsService: RegistrationEventsService = RegistrationEventsService(repo = repo, deadLetterQueueService, baseUrl)
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
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createRegistrationAddedDomainEvent()

    registrationEventsService.execute(event, IncomingEventType.REGISTRATION_ADDED)

    verify(exactly = 1) {
      repo.save(
        EventNotification(
          eventType = OutgoingEventType.MAPPA_DETAIL_CHANGED,
          hmppsId = "X777776",
          url = "$baseUrl/v1/persons/X777776/risks/mappadetail",
          lastModifiedDateTime = currentTime,
        ),
      )
    }
  }

  @Test
  fun `will not process and save a domain registration event message of none MAPP type`() {
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createRegistrationAddedDomainEvent(registerTypeCode = "NOTMAPP")

    registrationEventsService.execute(event, IncomingEventType.REGISTRATION_ADDED)

    verify { repo wasNot Called }
  }

  @Test
  fun `will not process and save a domain registration event message with no CRN`() {
    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createRegistrationAddedDomainEvent(identifiers = "[{\"type\":\"PNC\",\"value\":\"2018/0123456X\"}]")

    registrationEventsService.execute(event, IncomingEventType.REGISTRATION_ADDED)

    verify { repo wasNot Called }
    verify(exactly = 1) { deadLetterQueueService.sendEvent(event, "CRN could not be found in registration event message") }
  }

  @Test
  fun `will update an events lastModifiedDate if a relevant event is already stored`() {
    every { repo.existsByHmppsIdAndEventType("X777776", OutgoingEventType.MAPPA_DETAIL_CHANGED) } returns true

    val event: HmppsDomainEvent = SqsNotificationGeneratingHelper(zonedCurrentDateTime).createRegistrationAddedDomainEvent()

    registrationEventsService.execute(event, IncomingEventType.REGISTRATION_ADDED)

    verify(exactly = 1) { repo.updateLastModifiedDateTimeByHmppsIdAndEventType(currentTime, "X777776", OutgoingEventType.MAPPA_DETAIL_CHANGED) }
  }
}
