package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums

enum class PrisonerReleaseTypes (val code: String){

  PRISONER_OFFENDER_SEARCH_PRISONER_RELEASE(  "prisoner-offender-search.prisoner.released") ,
  PRISON_OFFENDER_EVEVNTS_PRISONER_RELEASE("prison-offender-events.prisoner.released"),
  CALCULATED_RELEASE_DATES_PRISONER_CHANGED( "calculate-release-dates.prisoner.changed");

  companion object {
    infix fun from(type: String): PrisonerReleaseTypes? = PrisonerReleaseTypes.entries.firstOrNull { it.code == type }
  }
}