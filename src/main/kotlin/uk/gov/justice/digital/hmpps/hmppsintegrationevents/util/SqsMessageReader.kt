package uk.gov.justice.digital.hmpps.hmppsintegrationevents.util

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.DomainEventMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.DomainEventMessageAttributes
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent

class SqsMessageReader() {
  private val objectMapper = ObjectMapper()

  fun mapRawMessage(rawMessage: String): HmppsDomainEvent {
    val incomingSqsMessage: IncomingSqsMessage = objectMapper.readValue(rawMessage)
    val domainEventMessage: DomainEventMessage = objectMapper.readValue(incomingSqsMessage.message)
    return HmppsDomainEvent(type = incomingSqsMessage.type, message = domainEventMessage, messageId = incomingSqsMessage.messageId, messageAttributes = incomingSqsMessage.messageAttributes)
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  data class IncomingSqsMessage(
    @JsonProperty("Type") val type: String,
    @JsonProperty("Message") val message: String,
    @JsonProperty("MessageId") val messageId: String,
    @JsonProperty("MessageAttributes") val messageAttributes: DomainEventMessageAttributes,
  )
}
