package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models

data class SubscriberFilterList(
  val eventType: List<String> = emptyList(),
  val prisonId: List<String>? = null,
)
