package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.utils

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.EventTypeValue

@JsonIgnoreProperties(ignoreUnknown = true)
data class SqsMessage(
  @JsonProperty("Type") val Type: String,
  @JsonProperty("Message") val Message: String,
  @JsonProperty("MessageId") val MessageId: String,
//  @JsonProperty("MessageAttributes") val MessageAttributes: MessageAttributes,
)

//@JsonIgnoreProperties(ignoreUnknown = true)
//data class MessageAttributes(
//  @JsonProperty("eventType") val eventType: EventType
//)
//
//@JsonIgnoreProperties(ignoreUnknown = true)
//data class EventType(
//  @JsonProperty("Value") val eventType: EventTypeValue
//)