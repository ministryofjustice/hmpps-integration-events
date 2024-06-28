package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models

import com.fasterxml.jackson.annotation.JsonProperty

data class EventResponse(
  @JsonProperty("ReceiveMessageResponse")
  val messageResponse: MessageResponse,
)
