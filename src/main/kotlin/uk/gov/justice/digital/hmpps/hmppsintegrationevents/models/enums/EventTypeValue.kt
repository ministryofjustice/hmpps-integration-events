package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums

enum class EventTypeValue(val value: String) {
  ADDRESS_CHANGE("Address Change"),
  REGISTRATION_ADDED("probation-case.registration.added"),
  ;

  companion object {
    infix fun from(value: String): EventTypeValue? = EventTypeValue.entries.firstOrNull { it.value == value }
  }
}
