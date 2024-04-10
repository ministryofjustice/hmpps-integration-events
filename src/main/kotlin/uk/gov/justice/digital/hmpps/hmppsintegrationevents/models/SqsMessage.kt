package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class SqsMessage(
  @JsonProperty("Type") val type: String,
  @JsonProperty("Message") val message: Message,
  @JsonProperty("MessageId") val messageId: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Message(
  @JsonProperty("eventType") val eventType: String,
  @JsonProperty("occurredAt") val occurredAt: String,
)
