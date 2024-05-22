package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums

enum class EventTypeValue(val value: String) {
  ADDRESS_CHANGE("Address Change"),
  MAPPA_DETAIL_CHANGED("MappaDetail.Changed"),
  ;

  companion object {
    infix fun from(value: String): EventTypeValue? = EventTypeValue.entries.firstOrNull { it.value == value }
  }
}
