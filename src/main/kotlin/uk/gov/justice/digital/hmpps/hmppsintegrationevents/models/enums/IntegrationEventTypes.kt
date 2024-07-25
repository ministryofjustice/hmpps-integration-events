package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums

enum class IntegrationEventTypes(val value: String, val registerTypes: List<String>?, val upstreamEventTypes: List<String>) {
  PROBATION_STATUS_CHANGED("ProbationStatus.Changed", listOf("ASFO", "WRSM"), listOf("probation-case.registration.added","probation-case.registration.deleted","probation-case.registration.deregistered", "probation-case.registration.updated")),
  MAPPA_DETAIL_CHANGED("MappaDetail.Changed", listOf("MAPP"), listOf("probation-case.registration.added","probation-case.registration.deleted","probation-case.registration.deregistered", "probation-case.registration.updated")),
  RISK_SCORE_CHANGED("RiskScore.Changed", null, listOf("risk-assessment.scores.determined", "probation-case.risk-scores.ogrs.manual-calculation")),
  KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE("KeyDatesAndAdjustments.PrisonerReleased", null, listOf("prisoner-offender-search.prisoner.released", "prison-offender-events.prisoner.released", "calculate-release-dates.prisoner.changed")),
  ;


  companion object {
    fun from(value: String, registerType: String?): IntegrationEventTypes? =
      IntegrationEventTypes.entries.firstOrNull { when (registerType) {
        null -> it.upstreamEventTypes.contains(value)
        else -> it.upstreamEventTypes.contains(value) && it.registerTypes!!.contains(registerType)
      } }
  }
}
