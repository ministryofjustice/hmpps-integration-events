package uk.gov.justice.digital.hmpps.hmppsintegrationevents.config

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.core.io.ClassPathResource
import java.io.File

/**
 * Abstract base class for unit tests of configuration files.
 *
 * from `hmpps-integration-api`
 */
abstract class ConfigTest {
  val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

  fun getConfigPath(
    environment: String,
    path: String,
  ): Any? = mapper.readTree(ClassPathResource("application-$environment.yml").file).path(path)

  fun getFeatureConfig(environment: String): Map<String, Boolean> = getConfigPath(environment, "feature-flag")
    ?.let { mapper.convertValue(it, object : TypeReference<Map<String, Boolean>>() {}) } ?: emptyMap()

  /**
   * Parses the specified config text as a particular config class.
   */
  inline fun <reified T> parseConfig(config: String): T = mapper.readValue(config, T::class.java)

  /**
   * Returns a list of all the configured environments.
   */
  fun listConfigs(): Set<String> = File("src/main/resources")
    .walk()
    .filter({ it.name.startsWith("application-") })
    .map({ it.name.replaceFirst("application-", "").replaceFirst(".yml", "") })
    .toSet()
}
