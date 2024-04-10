package uk.gov.justice.digital.hmpps.hmppsintegrationevents.util

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.Message
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.SqsMessage

class SqsMessageReader() {
  fun mapRawMessage(rawMessage: String): SqsMessage {
    val objectMapper = ObjectMapper()
    val incomingSqsMessage: IncomingSqsMessage = objectMapper.readValue(rawMessage)
    val message: Message = objectMapper.readValue(incomingSqsMessage.message)
    return SqsMessage(type = incomingSqsMessage.type, message = message, messageId = incomingSqsMessage.messageId)
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  data class IncomingSqsMessage(
    @JsonProperty("Type") val type: String,
    @JsonProperty("Message") val message: String,
    @JsonProperty("MessageId") val messageId: String,
  )
}
