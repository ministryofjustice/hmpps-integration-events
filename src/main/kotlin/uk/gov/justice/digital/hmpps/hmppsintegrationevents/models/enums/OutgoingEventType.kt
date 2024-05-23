package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums

enum class OutgoingEventType (val value: String) {

  MAPPA_DETAIL_CHANGED("MappaDetail.Changed"),
  ;
  companion object {
    infix fun from(value: String): OutgoingEventType? = OutgoingEventType.entries.firstOrNull { it.value == value }
  }
}