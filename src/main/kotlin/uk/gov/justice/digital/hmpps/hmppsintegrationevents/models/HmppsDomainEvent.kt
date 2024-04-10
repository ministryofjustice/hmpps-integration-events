package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class HmppsDomainEvent(
  @JsonProperty("Type") val type: String,
  @JsonProperty("Message") val message: DomainEventMessage,
  @JsonProperty("MessageId") val messageId: String,
  @JsonProperty("MessageAttributes") val messageAttributes: DomainEventMessageAttributes,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DomainEventMessage(
  @JsonProperty("occurredAt") val occurredAt: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DomainEventMessageAttributes(
  @JsonProperty("eventType") val eventType: EventType,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EventType(
  @JsonProperty("Value") val value: String,
)
