package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums

enum class EventTypeValue(val type: String) {
  ADDRESS_CHANGE("Address Change"),
  REGISTRATION_ADDED("probation-case.registration.added");

  companion object {
    infix fun from(value: String): EventTypeValue? = EventTypeValue.values().firstOrNull { it.type == value }
  }
}
