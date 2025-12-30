package uk.gov.justice.digital.hmpps.hmppsintegrationevents.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.bind.Name
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(FeatureFlagConfig::class)
class FeatureFlagConfigurations

/**
 * Feature flag configuration for the application.
 *
 * The values of the feature flags can be different in each environment, allowing
 * code to be enabled in a dev/test environment but not in production.
 *
 * Feature flag settings are in the `application-*.yml` files, with each having
 * a true/false value.
 *
 * from `hmpps-integration-api`
 */
@ConfigurationProperties
data class FeatureFlagConfig(
  @Name("feature-flag")
  val flags: Map<String, Boolean> = mutableMapOf(),
) {
  companion object {
    const val PERSON_LANGUAGES_CHANGED_NOTIFICATIONS_ENABLED = "person-languages-changed-notifications-enabled"
    const val PRISONER_MERGED_NOTIFICATIONS_ENABLED = "prisoner-merge-notifications-enabled"
    const val PRISONER_BASE_LOCATION_CHANGED_NOTIFICATIONS_ENABLED = "prisoner-base-location-changed-notifications-enabled"
  }

  /**
   * Returns the value of a feature flag, or null if it is not set.
   */
  fun getConfigFlagValue(feature: String): Boolean? = flags[feature]

  /**
   * Returns true if the feature flag is defined and set to true.
   */
  fun isEnabled(feature: String): Boolean = flags.getOrDefault(feature, false)
}
