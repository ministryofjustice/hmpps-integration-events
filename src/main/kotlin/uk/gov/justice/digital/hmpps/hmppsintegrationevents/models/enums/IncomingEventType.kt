package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums

enum class IncomingEventType(val value: String, val outgoingEvent: OutgoingEventType) {
  ADDRESS_CHANGE("Address Change", OutgoingEventType.ADDRESS_CHANGE),
  REGISTRATION_ADDED("probation-case.registration.added", OutgoingEventType.MAPPA_DETAIL_CHANGED),
  REGISTRATION_UPDATED("probation-case.registration.updated", OutgoingEventType.MAPPA_DETAIL_CHANGED),
  ;

  companion object {
    infix fun from(value: String): IncomingEventType? = IncomingEventType.entries.firstOrNull { it.value == value }
  }
}
