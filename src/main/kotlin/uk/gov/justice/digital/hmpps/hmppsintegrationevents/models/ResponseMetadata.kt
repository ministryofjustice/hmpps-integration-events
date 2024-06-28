package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models

import com.fasterxml.jackson.annotation.JsonProperty

data class ResponseMetadata(
  @JsonProperty("RequestId")
  val requestId: String,
)
