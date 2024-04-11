package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class DomainEventMessageAttributes(
  @JsonProperty("eventType") val eventType: EventType,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EventType(
  @JsonProperty("Value") val value: String,
)
