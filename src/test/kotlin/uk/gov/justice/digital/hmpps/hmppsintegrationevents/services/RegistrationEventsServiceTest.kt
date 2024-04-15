package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@ActiveProfiles("test")
class RegistrationEventsServiceTest {

  private val repo = mockk<EventNotificationRepository>()
  private val service: RegistrationEventsService = RegistrationEventsService(repo)
  private val currentTime: ZonedDateTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
  private val currentTimeString = currentTime.format(DateTimeFormatter.ISO_INSTANT)

  @Test
  fun `will process and save a mapps domain registration event message`() {
    val objectMapper = ObjectMapper()
    val event: HmppsDomainEvent = objectMapper.readValue(SqsNotificationGeneratingHelper(currentTime).generateRegistrationEvent())

    every { repo.save(any()) } returnsArgument 0
    every { repo.existsByHmppsIdAndEventType(any(), any()) } returns false

    service.execute(event)

    val expectedEventSave = EventNotification(
      eventType = EventTypeValue.REGISTRATION_ADDED,
      hmppsId = "X777776",
      url = "https://dev.integration-api.hmpps.service.justice.gov.uk/v1/persons/X777776/risks/mappadetail",
      lastModifiedDateTime = currentTimeString,
    )

    verify(exactly = 1) { repo.save(expectedEventSave) }
  }
}
