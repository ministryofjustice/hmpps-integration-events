package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums

enum class IntegrationEventTypes(val value: String, val upstreamEventTypes: List<String>) {
  MAPPA_DETAIL_CHANGED("MappaDetail.Changed", listOf("probation-case.registration.added", "probation-case.registration.updated")),
  RISK_SCORE_CHANGED("RiskScore.Changed", listOf("risk-assessment.scores.determined", "probation-case.risk-scores.ogrs.manual-calculation")),
  ;

  companion object {
    infix fun from(value: String): IntegrationEventTypes? = IntegrationEventTypes.entries.firstOrNull { it.upstreamEventTypes.contains(value) }
  }
}
