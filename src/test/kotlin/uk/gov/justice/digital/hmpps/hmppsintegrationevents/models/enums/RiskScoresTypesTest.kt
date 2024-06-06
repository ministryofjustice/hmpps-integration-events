package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
class RiskScoresTypesTest {

  @Test
  fun `from return correct enum value for given know code`() {
    val map = mapOf(
      "risk-assessment.scores.ogrs.determined" to RiskScoreTypes.OFFENDER_GROUP_RECONVICTION_SCALE,
      "probation-case.risk-scores.ogrs.manual-calculation" to RiskScoreTypes.OFFENDER_GROUP_RECONVICTION_SCALE_MANUAL_CALCULATION,
      "risk-assessment.scores.rsr.determined"  to RiskScoreTypes.RISK_OF_SERIOUS_RECIDIVISM,
      "assessment.summary.produced" to RiskScoreTypes.ASSESSMENT_SUMMARY_PRODUCED)

    map.forEach { it->
      val result = RiskScoreTypes.from(it.key)
      result?.shouldBe(it.value)
    }
  }

  @Test
  fun `from return null value for unknow code`() {
      val result = RiskScoreTypes.from("Some type")
      result.shouldBe(null)
  }
}