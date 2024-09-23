package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models

data class PersonExists(
  val crn: String,
  val existsInDelius: Boolean,
)
