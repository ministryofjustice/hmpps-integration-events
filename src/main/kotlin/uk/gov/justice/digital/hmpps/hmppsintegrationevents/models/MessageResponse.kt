package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models

import com.fasterxml.jackson.annotation.JsonProperty

data class MessageResponse(
  @JsonProperty("ReceiveMessageResult")
  val receiveMessageResult: ReceiveMessageResult,
  @JsonProperty("ResponseMetadata")
  val responseMetadata: ResponseMetadata,
)
