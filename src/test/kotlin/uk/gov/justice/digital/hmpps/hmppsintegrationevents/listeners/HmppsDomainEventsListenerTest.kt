package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.DomainEventMessageAttributes
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.EventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DlqService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.RegistrationEventsService
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HmppsDomainEventsListenerTest {

  val mockRegistrationEventsService: RegistrationEventsService = Mockito.mock(RegistrationEventsService::class.java)
  val mockDlqService: DlqService = Mockito.mock(DlqService::class.java)
  val hmppsDomainEventsListener: HmppsDomainEventsListener = HmppsDomainEventsListener(mockRegistrationEventsService, mockDlqService)

  @Test
  fun `when a registration added sqs event is received it should call the registrationEventService`() {
    val currentTime = LocalDateTime.now().atZone(ZoneId.systemDefault())

    val rawMessage = SqsNotificationGeneratingHelper().generateRegistrationEvent(timestamp = currentTime)

    val isoInstantTimestamp = DateTimeFormatter.ISO_INSTANT.format(currentTime)
    val readableTimestamp = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy").format(currentTime)

    val hmppsDomainEvent = HmppsDomainEvent(
      type = "Notification",
      message = "{\"eventType\":\"probation-case.registration.added\",\"version\":1,\"occurredAt\":\"$isoInstantTimestamp\",\"description\":\"A new registration has been added to the probation case\",\"personReference\":{\"identifiers\":[{\"type\":\"CRN\",\"value\":\"X777776\"}]},\"additionalInformation\":{\"registrationLevelDescription\":\"MAPPA Level 3\",\"registerTypeDescription\":\"MAPPA\",\"registrationCategoryCode\":\"M1\",\"registrationId\":\"1234567890\",\"registrationDate\":\"$readableTimestamp\",\"registerTypeCode\":\"MAPP\",\"createdDateAndTime\":\"$readableTimestamp\",\"registrationCategoryDescription\":\"MAPPA Cat 1\",\"registrationLevelCode\":\"M3\"}}",
      messageId = "1a2345bc-de67-890f-1g01-11h21314h151",
      messageAttributes = DomainEventMessageAttributes(eventType = EventType(value = "probation-case.registration.added")),
    )

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(mockRegistrationEventsService, times(1)).execute(hmppsDomainEvent)
  }
}
