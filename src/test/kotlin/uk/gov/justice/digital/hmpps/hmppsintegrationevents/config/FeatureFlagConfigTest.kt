package uk.gov.justice.digital.hmpps.hmppsintegrationevents.config

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Tests of Feature flag configuration
 * from `hmpps-integration-api`
 */
class FeatureFlagConfigTest : ConfigTest() {
  companion object {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val flags: FeatureFlagConfig =
    FeatureFlagConfig(
      mapOf(
        "flag-on" to true,
        "flag-off" to false,
      ),
    )

  @Test
  fun `feature flag correctly enabled`() {
    assertTrue(flags.isEnabled("flag-on"))
  }

  @Test
  fun `feature flag correctly disabled`() {
    assertFalse(flags.isEnabled("flag-off"))
  }

  @Test
  fun `feature flag disabled if missing`() {
    assertFalse(flags.isEnabled("flag-missing"))
  }

  @Test
  fun `no flags are enabled in all configs`() {
    val flagCounts = mutableMapOf<String, Int>()

    val allConfigs = listConfigs()
    for (config in allConfigs) {
      for (flag in getFeatureConfig(config)) {
        val countKey = "${flag.key} is ${flag.value}"
        flagCounts[countKey] = flagCounts.getOrDefault(countKey, 0) + 1
      }
    }

    val allSameFlags = flagCounts.filter { it.value == allConfigs.size }
    allSameFlags.forEach {
      logger.warn("${it.key} is enabled in all ${allConfigs.size} configs")
    }

    // Reduce the threshold in the following assertion over time
    assertTrue(allSameFlags.size <= 3, "Too many flags enabled in all configs: $flagCounts")
  }
}
