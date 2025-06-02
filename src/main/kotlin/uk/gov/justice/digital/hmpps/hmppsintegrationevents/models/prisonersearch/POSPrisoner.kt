package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.prisonersearch

data class POSPrisoner(
  val prisonerNumber: String,
  val pncNumber: String? = null,
  val croNumber: String? = null,
  val bookingId: String? = null,
  val firstName: String,
  val middleNames: String? = null,
  val lastName: String,
  val prisonId: String? = null,
  val prisonName: String? = null,
)