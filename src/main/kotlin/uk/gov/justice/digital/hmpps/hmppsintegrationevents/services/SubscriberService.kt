package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.HmppsSecretManagerProperties
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway.IntegrationApiGateway
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.ConfigAuthorisation
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.SubscriberFilterList
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType

@Service
class SubscriberService(
  private val integrationApiGateway: IntegrationApiGateway,
  private val subscriberProperties: HmppsSecretManagerProperties,
  private val secretsManagerService: SecretsManagerService,
  private val integrationEventTopicService: IntegrationEventTopicService,
  private val objectMapper: ObjectMapper,
  private val telemetryService: TelemetryService,
  private val integrationEventTypeUrlMatcher: IntegrationEventTypeUrlMatcher,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val defaultEventTypeList = listOf("default")
  }

  @Scheduled(fixedRateString = "\${subscriber-checker.schedule.rate}")
  fun checkSubscriberFilterList() {
    try {
      log.info("Checking subscriber filter list...")

      val apiResponse = integrationApiGateway.getApiAuthorizationConfig()
      val caseInsensitiveSecrets = subscriberProperties.secrets.mapKeys { it.key.uppercase() }

      // Mappings' Cache (endpoints to events)
      val endpointToEventCache: EndpointToEventCache = MappingCache.create()

      apiResponse.filter { client -> caseInsensitiveSecrets.containsKey(client.key.uppercase()) }
        .forEach { refreshClientFilter(it, caseInsensitiveSecrets[it.key.uppercase()]!!, endpointToEventCache) }

      log.info("Subscriber filter list checked")
    } catch (e: Exception) {
      logAndCapture("Error checking filter list", e)
    }
  }

  private fun refreshClientFilter(
    clientConfig: Map.Entry<String, ConfigAuthorisation>,
    subscriber: HmppsSecretManagerProperties.SecretConfig,
    endpointToEventCache: EndpointToEventCache,
  ) {
    log.info("Checking filter list for ${clientConfig.key}...")
    try {
      val events = matchesUrlToEvents(clientConfig.value.endpoints, endpointToEventCache)
      val prisonIds = clientConfig.value.filters?.prisons

      val secretValue = secretsManagerService.getSecretValue(subscriber.secretId)
      val existingFilterList = unmarshalFilterList(secretValue)
      val updatedFilterList = SubscriberFilterList(eventType = events, prisonId = prisonIds)

      if (updatedFilterList != existingFilterList) {
        log.info("Updating filter list for ${clientConfig.key} (${subscriber.secretId})")

        val filterPolicy = objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL).writeValueAsString(updatedFilterList)

        // Update value in the SNS subscription itself
        integrationEventTopicService.updateSubscriptionAttributes(subscriber.queueId, "FilterPolicy", filterPolicy)

        // Update value in Secrets Manager so it is available for future Terraform updates, and to detect changes
        secretsManagerService.setSecretValue(subscriber.secretId, filterPolicy)

        log.info("Filter list for ${clientConfig.key} updated")
      }
      log.info("Finished checking filter list for ${clientConfig.key}")
    } catch (e: Exception) {
      logAndCapture("Error checking filter list for ${clientConfig.key}", e)
    }
  }

  /**
   * Match endpoints' URL to event types
   * - The sequence of resolved events is consistent according to inputs (endpoint URLs).
   * - The resolved events are distinct (deduplicated when needed).
   *
   * Caching at `endpointToEventCache` for repeating endpoints; (transactional per refresh)
   * - return cached result when found, or call [IntegrationEventType.matchesUrl] to resolve (and cache)
   */
  private fun matchesUrlToEvents(endpoints: List<String>, endpointToEventCache: EndpointToEventCache): List<String> = endpoints.map { urlPattern ->
    endpointToEventCache[urlPattern] ?: integrationEventTypeUrlMatcher.matchesUrl(urlPattern).map { it.name }
      .also { endpointToEventCache[urlPattern] = it }
  }.asSequence().distinct().flatten().toList()
    .ifEmpty { defaultEventTypeList }

  private fun unmarshalFilterList(secretValue: String): SubscriberFilterList {
    if (secretValue == "") {
      return SubscriberFilterList(eventType = defaultEventTypeList, prisonId = null)
    }
    return objectMapper.readValue<SubscriberFilterList>(secretValue)
  }

  private fun logAndCapture(
    message: String,
    e: Exception,
  ) {
    log.error(message, e.message)
    telemetryService.captureException(RuntimeException(message, e))
  }
}

/**
 * Matcher of IntegrationEventType from endpoint URL
 *
 * - Matches endpoint URL to [IntegrationEventType].
 * - This is extracted for test verification with spying.
 */
@Component
class IntegrationEventTypeUrlMatcher {
  fun matchesUrl(urlPattern: String): List<IntegrationEventType> = IntegrationEventType.entries.filter { it.matchesUrl(urlPattern) }
}

/**
 * EndpointToEventCache: Mapping Endpoint URL to a list of Event types
 */
typealias EndpointToEventCache = MutableMap<String, List<String>>

private class MappingCache private constructor() {
  companion object {
    private object MappingCacheConfiguration {
      const val LOAD_FACTOR = 0.75f
      val defaultCapacity = IntegrationEventType.entries.count()
    }

    fun create(): EndpointToEventCache = lruCache()

    // Least-Recently-Used cache using LinkedHashMap with accessOrder
    private fun <K, V> lruCache(
      capacity: Int = MappingCacheConfiguration.defaultCapacity,
      loadFactor: Float = MappingCacheConfiguration.LOAD_FACTOR,
    ): MutableMap<K, V> = object : LinkedHashMap<K, V>(capacity, loadFactor, true) {
      override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>) = size > capacity
    }
  }
}
