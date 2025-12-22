package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import java.util.stream.Stream

// From `uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners.HmppsDomainEventsListenerPNDAlertsTest`
class HmppsDomainEventServicePNDAlertsTest : HmppsDomainEventServiceEventTestCase() {
  private val nomsNumber = "A1234BC"
  private val hmppsId = nomsNumber

  @ParameterizedTest
  @ArgumentsSource(AlertCodeArgumentSource::class)
  fun `will process and save a pnd alert for person alert created event`(alertCode: String) = executeShouldSaveAlertsChangedEvents(
    eventType = "person.alert.created",
    alertCode = alertCode,
  )

  @ParameterizedTest
  @ArgumentsSource(AlertCodeArgumentSource::class)
  fun `will process and save a pnd alert for person alert changed event`(alertCode: String) = executeShouldSaveAlertsChangedEvents(
    eventType = "person.alert.changed",
    alertCode = alertCode,
  )

  @ParameterizedTest
  @ArgumentsSource(AlertCodeArgumentSource::class)
  fun `will process and save a pnd alert for person alert deleted event`(alertCode: String) = executeShouldSaveAlertsChangedEvents(
    eventType = "person.alert.deleted",
    alertCode = alertCode,
  )

  @ParameterizedTest
  @ArgumentsSource(AlertCodeArgumentSource::class)
  fun `will process and save a pnd alert for person alert updated event`(alertCode: String) = executeShouldSaveAlertsChangedEvents(
    eventType = "person.alert.updated",
    alertCode = alertCode,
  )

  private fun executeShouldSaveAlertsChangedEvents(eventType: String, alertCode: String) {
    val message = """
      {\"eventType\":\"$eventType\",\"additionalInformation\":{\"alertUuid\":\"8339dd96-4a02-4d5b-bc78-4eda22f678fa\",\"alertCode\":\"$alertCode\",\"source\":\"NOMIS\"},\"version\":1,\"description\":\"An alert has been created in the alerts service\",\"occurredAt\":\"2024-08-12T19:48:12.771347283+01:00\",\"detailUrl\":\"https://alerts-api.hmpps.service.justice.gov.uk/alerts/8339dd96-4a02-4d5b-bc78-4eda22f678fa\",\"personReference\":{\"identifiers\":[{\"type\":\"NOMS\",\"value\":\"$nomsNumber\"}]}}
    """.trimIndent()
    val hmppsMessage = message.replace("\\", "")

    executeShouldSaveMultipleEventNotificationsOfPerson(
      hmppsEventType = eventType,
      hmppsMessage = hmppsMessage,
      hmppsId = hmppsId,
      expectedNotificationTypeAndUrls = mapOf(
        IntegrationEventType.PERSON_PND_ALERTS_CHANGED to "$baseUrl/v1/pnd/persons/$hmppsId/alerts",
        IntegrationEventType.PERSON_ALERTS_CHANGED to "$baseUrl/v1/persons/$hmppsId/alerts",
      ),
    )
  }

  private class AlertCodeArgumentSource : ArgumentsProvider {
    private val alertCodes = arrayOf(
      "BECTER", "HA", "XA", "XCA", "XEL", "XELH", "XER", "XHT", "XILLENT",
      "XIS", "XR", "XRF", "XSA", "HA2", "RCS", "RDV", "RKC", "RPB", "RPC",
      "RSS", "RST", "RDP", "REG", "RLG", "ROP", "RRV", "RTP", "RYP", "HS", "SC",
    )

    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments?> = alertCodes.map { Arguments.of(it) }.stream()
  }
}
