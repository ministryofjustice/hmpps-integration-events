package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums

enum class RiskScoreTypes(val code: String) {
  OFFENDER_GROUP_RECONVICTION_SCALE("risk-assessment.scores.ogrs.determined"),
  OFFENDER_GROUP_RECONVICTION_SCALE_MANUAL_CALCULATION("probation-case.risk-scores.ogrs.manual-calculation"),
  RISK_OF_SERIOUS_RECIDIVISM("risk-assessment.scores.rsr.determined"),
  ASSESSMENT_SUMMARY_PRODUCED("assessment.summary.produced"),
  ;

  companion object {
    infix fun from(type: String): RiskScoreTypes? = RiskScoreTypes.entries.firstOrNull { it.code == type }
  }
}
