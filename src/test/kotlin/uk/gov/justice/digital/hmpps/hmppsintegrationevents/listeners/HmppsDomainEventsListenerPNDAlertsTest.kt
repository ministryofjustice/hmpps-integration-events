package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.generateHmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DeadLetterQueueService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.HmppsDomainEventService

@ActiveProfiles("test")
@JsonTest
class HmppsDomainEventsListenerPNDAlertsTest {
  private val hmppsDomainEventService = mockk<HmppsDomainEventService>()
  private val deadLetterQueueService = mockk<DeadLetterQueueService>()

  private val hmppsDomainEventsListener: HmppsDomainEventsListener = HmppsDomainEventsListener(hmppsDomainEventService, deadLetterQueueService)

  private val nomsNumber = "A1234BC"

  @ParameterizedTest
  @ValueSource(
    strings = [
      "BECTER", "HA", "XA", "XCA", "XEL", "XELH", "XER", "XHT", "XILLENT",
      "XIS", "XR", "XRF", "XSA", "HA2", "RCS", "RDV", "RKC", "RPB", "RPC",
      "RSS", "RST", "RDP", "REG", "RLG", "ROP", "RRV", "RTP", "RYP", "HS", "SC",
    ],
  )
  fun `will process and save a pnd alert for person alert created event`(alertCode: String) {
    val eventType = "person.alert.created"
    val message = """
      {\"eventType\":\"$eventType\",\"additionalInformation\":{\"alertUuid\":\"8339dd96-4a02-4d5b-bc78-4eda22f678fa\",\"alertCode\":\"$alertCode\",\"source\":\"NOMIS\"},\"version\":1,\"description\":\"An alert has been created in the alerts service\",\"occurredAt\":\"2024-08-12T19:48:12.771347283+01:00\",\"detailUrl\":\"https://alerts-api.hmpps.service.justice.gov.uk/alerts/8339dd96-4a02-4d5b-bc78-4eda22f678fa\",\"personReference\":{\"identifiers\":[{\"type\":\"NOMS\",\"value\":\"$nomsNumber\"}]}}
    """.trimIndent()

    val hmppsMessage = message.replace("\\", "")
    val payload = DomainEvents.generateDomainEvent(eventType, message)
    val hmppsDomainEvent = generateHmppsDomainEvent(eventType, hmppsMessage)

    every { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventType.PERSON_PND_ALERTS_CHANGED) } just runs
    every { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventType.PERSON_ALERTS_CHANGED) } just runs

    hmppsDomainEventsListener.onDomainEvent(payload)

    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventType.PERSON_PND_ALERTS_CHANGED) }
    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventType.PERSON_ALERTS_CHANGED) }
    verify(exactly = 2) { hmppsDomainEventService.execute(any(), any()) }
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "BECTER", "HA", "XA", "XCA", "XEL", "XELH", "XER", "XHT", "XILLENT",
      "XIS", "XR", "XRF", "XSA", "HA2", "RCS", "RDV", "RKC", "RPB", "RPC",
      "RSS", "RST", "RDP", "REG", "RLG", "ROP", "RRV", "RTP", "RYP", "HS", "SC",
    ],
  )
  fun `will process and save a pnd alert for person alert changed event`(alertCode: String) {
    val eventType = "person.alert.changed"
    val message = """
      {\"eventType\":\"$eventType\",\"additionalInformation\":{\"alertUuid\":\"8339dd96-4a02-4d5b-bc78-4eda22f678fa\",\"alertCode\":\"$alertCode\",\"source\":\"NOMIS\"},\"version\":1,\"description\":\"An alert has been created in the alerts service\",\"occurredAt\":\"2024-08-12T19:48:12.771347283+01:00\",\"detailUrl\":\"https://alerts-api.hmpps.service.justice.gov.uk/alerts/8339dd96-4a02-4d5b-bc78-4eda22f678fa\",\"personReference\":{\"identifiers\":[{\"type\":\"NOMS\",\"value\":\"$nomsNumber\"}]}}
    """.trimIndent()

    val hmppsMessage = message.replace("\\", "")
    val payload = DomainEvents.generateDomainEvent(eventType, message)
    val hmppsDomainEvent = generateHmppsDomainEvent(eventType, hmppsMessage)

    every { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventType.PERSON_PND_ALERTS_CHANGED) } just runs
    every { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventType.PERSON_ALERTS_CHANGED) } just runs

    hmppsDomainEventsListener.onDomainEvent(payload)

    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventType.PERSON_PND_ALERTS_CHANGED) }
    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventType.PERSON_ALERTS_CHANGED) }
    verify(exactly = 2) { hmppsDomainEventService.execute(any(), any()) }
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "BECTER", "HA", "XA", "XCA", "XEL", "XELH", "XER", "XHT", "XILLENT",
      "XIS", "XR", "XRF", "XSA", "HA2", "RCS", "RDV", "RKC", "RPB", "RPC",
      "RSS", "RST", "RDP", "REG", "RLG", "ROP", "RRV", "RTP", "RYP", "HS", "SC",
    ],
  )
  fun `will process and save a pnd alert for person alert deleted event`(alertCode: String) {
    val eventType = "person.alert.deleted"
    val message = """
      {\"eventType\":\"$eventType\",\"additionalInformation\":{\"alertUuid\":\"8339dd96-4a02-4d5b-bc78-4eda22f678fa\",\"alertCode\":\"$alertCode\",\"source\":\"NOMIS\"},\"version\":1,\"description\":\"An alert has been created in the alerts service\",\"occurredAt\":\"2024-08-12T19:48:12.771347283+01:00\",\"detailUrl\":\"https://alerts-api.hmpps.service.justice.gov.uk/alerts/8339dd96-4a02-4d5b-bc78-4eda22f678fa\",\"personReference\":{\"identifiers\":[{\"type\":\"NOMS\",\"value\":\"$nomsNumber\"}]}}
    """.trimIndent()

    val hmppsMessage = message.replace("\\", "")
    val payload = DomainEvents.generateDomainEvent(eventType, message)
    val hmppsDomainEvent = generateHmppsDomainEvent(eventType, hmppsMessage)

    every { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventType.PERSON_PND_ALERTS_CHANGED) } just runs
    every { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventType.PERSON_ALERTS_CHANGED) } just runs

    hmppsDomainEventsListener.onDomainEvent(payload)

    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventType.PERSON_PND_ALERTS_CHANGED) }
    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventType.PERSON_ALERTS_CHANGED) }
    verify(exactly = 2) { hmppsDomainEventService.execute(any(), any()) }
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "BECTER", "HA", "XA", "XCA", "XEL", "XELH", "XER", "XHT", "XILLENT",
      "XIS", "XR", "XRF", "XSA", "HA2", "RCS", "RDV", "RKC", "RPB", "RPC",
      "RSS", "RST", "RDP", "REG", "RLG", "ROP", "RRV", "RTP", "RYP", "HS", "SC",
    ],
  )
  fun `will process and save a pnd alert for person alert updated event`(alertCode: String) {
    val eventType = "person.alert.updated"
    val message = """
      {\"eventType\":\"$eventType\",\"additionalInformation\":{\"alertUuid\":\"8339dd96-4a02-4d5b-bc78-4eda22f678fa\",\"alertCode\":\"$alertCode\",\"source\":\"NOMIS\"},\"version\":1,\"description\":\"An alert has been created in the alerts service\",\"occurredAt\":\"2024-08-12T19:48:12.771347283+01:00\",\"detailUrl\":\"https://alerts-api.hmpps.service.justice.gov.uk/alerts/8339dd96-4a02-4d5b-bc78-4eda22f678fa\",\"personReference\":{\"identifiers\":[{\"type\":\"NOMS\",\"value\":\"$nomsNumber\"}]}}
    """.trimIndent()

    val hmppsMessage = message.replace("\\", "")
    val payload = DomainEvents.generateDomainEvent(eventType, message)
    val hmppsDomainEvent = generateHmppsDomainEvent(eventType, hmppsMessage)

    every { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventType.PERSON_PND_ALERTS_CHANGED) } just runs
    every { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventType.PERSON_ALERTS_CHANGED) } just runs

    hmppsDomainEventsListener.onDomainEvent(payload)

    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventType.PERSON_PND_ALERTS_CHANGED) }
    verify(exactly = 1) { hmppsDomainEventService.execute(hmppsDomainEvent, IntegrationEventType.PERSON_ALERTS_CHANGED) }
    verify(exactly = 2) { hmppsDomainEventService.execute(any(), any()) }
  }
}
