package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.EventTypeValue
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime
import java.time.ZoneId

@ActiveProfiles("test")
class RegistrationEventsServiceTest {

  private val repo = mockk<EventNotificationRepository>()
  private val service: RegistrationEventsService = RegistrationEventsService(repo)
  private val currentTime: LocalDateTime = LocalDateTime.now()
  private val zonedCurrentDateTime = currentTime.atZone(ZoneId.systemDefault())

  @Test
  fun `will process and save a mapps domain registration event message`() {
    val objectMapper = ObjectMapper()
    val event: HmppsDomainEvent = objectMapper.readValue(SqsNotificationGeneratingHelper(zonedCurrentDateTime).generateRegistrationEvent())

    mockkStatic(LocalDateTime::class)
    every { LocalDateTime.now() } returns currentTime
    every { repo.existsByHmppsIdAndEventType(any(), any()) } returns false
    every { repo.save(any()) } returnsArgument 0

    service.execute(event)

    val expectedEventSave = EventNotification(
      eventType = EventTypeValue.REGISTRATION_ADDED,
      hmppsId = "X777776",
      url = "https://dev.integration-api.hmpps.service.justice.gov.uk/v1/persons/X777776/risks/mappadetail",
      lastModifiedDateTime = currentTime,
    )

    verify(exactly = 1) { repo.save(expectedEventSave) }
  }
}