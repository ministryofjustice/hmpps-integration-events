package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class MessageAttributes(
  @JsonProperty("eventType") val eventType: EventType,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EventType(
  @JsonProperty("Value") val value: String,
)
