package uk.gov.justice.digital.hmpps.hmppsintegrationevents.extensions

import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent

val mappaCategories = intArrayOf(1, 2, 3, 4)

fun HmppsDomainEvent.isValidContactEvent(): Boolean = additionalInformation?.mappa?.category != null &&
  mappaCategories.contains(additionalInformation.mappa.category)
