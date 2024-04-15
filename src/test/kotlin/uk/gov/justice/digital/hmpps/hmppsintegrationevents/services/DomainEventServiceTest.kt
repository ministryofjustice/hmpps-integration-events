package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.AdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.DomainEventMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.DomainEventMessageAttributes
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.EventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.Identifier
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.PersonReference
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.EventTypeValue
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@ActiveProfiles("test")
class DomainEventServiceTest {

  private val repo = mockk<EventNotificationRepository>()
  val service: DomainEventsService = DomainEventsService(repo)
  val currentTime = LocalDateTime.now()
  val currentTimeString =  currentTime.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  @Test
  fun `will process and save a mapps domain registration event message`() {
    val event = HmppsDomainEvent(
      type = "Notification",
      message = DomainEventMessage(
        occurredAt = currentTimeString,
        personReference = PersonReference(
          identifiers = listOf((Identifier(type = "CRN", value = "X777776"))),
        ),
        additionalInformation = AdditionalInformation(
          registerTypeCode = "rTpeCode",
          registerTypeDescription = "rTypeDesc",
        ),
      ),
      messageId = "abc123",
      messageAttributes = DomainEventMessageAttributes(
        eventType = EventType(
          value = EventTypeValue.REGISTRATION_ADDED.value,
        ),
      ),
    )


    every { LocalDateTime.now() } returns currentTime

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