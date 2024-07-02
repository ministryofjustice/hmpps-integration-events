package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums

data class UpstreamApiError(val causedBy: UpstreamApi, val type: Type, val description: String? = null) {
  enum class Type {
    ENTITY_NOT_FOUND,
    FORBIDDEN,
  }
}
