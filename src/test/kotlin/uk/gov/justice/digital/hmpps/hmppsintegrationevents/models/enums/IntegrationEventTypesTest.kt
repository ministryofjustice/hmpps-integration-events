package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class IntegrationEventTypesTest {

  @Test
  fun `from return correct enum value for given know code`() {
    val map = mapOf(
      "probation-case.registration.added" to IntegrationEventTypes.MAPPA_DETAIL_CHANGED,
      "probation-case.registration.updated" to IntegrationEventTypes.MAPPA_DETAIL_CHANGED,
      "risk-assessment.scores.determined" to IntegrationEventTypes.RISK_SCORE_CHANGED,
      "probation-case.risk-scores.ogrs.manual-calculation" to IntegrationEventTypes.RISK_SCORE_CHANGED,
    )

    map.forEach { it ->
      val result = IntegrationEventTypes.from(it.key, null)
      result?.shouldBe(it.value)
    }
  }

  @Test
  fun `from return null value for unknow code`() {
    val result = IntegrationEventTypes.from("Some type", "")
    result.shouldBe(null)
  }
}
