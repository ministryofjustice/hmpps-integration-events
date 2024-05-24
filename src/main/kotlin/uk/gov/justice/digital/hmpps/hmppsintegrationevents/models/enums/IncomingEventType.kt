package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums

enum class IncomingEventType(val value: String) {
  ADDRESS_CHANGE("Address Change"),
  REGISTRATION_ADDED("probation-case.registration.added"),
  REGISTRATION_UPDATED("probation-case.registration.updated"),
  ;

  companion object {
    infix fun from(value: String): IncomingEventType? = IncomingEventType.entries.firstOrNull { it.value == value }
  }
}
