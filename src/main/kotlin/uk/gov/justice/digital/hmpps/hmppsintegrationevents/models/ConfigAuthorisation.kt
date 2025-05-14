package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models

import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.ConsumerFilters

data class ConfigAuthorisation(
  val endpoints: List<String>,
  val filters: ConsumerFilters?,
)
