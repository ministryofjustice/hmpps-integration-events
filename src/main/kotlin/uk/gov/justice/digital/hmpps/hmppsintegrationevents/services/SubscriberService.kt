package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.HmppsSecretManagerProperties
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway.IntegrationApiGateway
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.ConfigAuthorisation
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.SubscriberFilterList
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import kotlin.math.min

@Service
class SubscriberService(private val integrationApiGateway: IntegrationApiGateway, private val subscriberProperties: HmppsSecretManagerProperties, private val secretsManagerService: SecretsManagerService, private val integrationEventTopicService: IntegrationEventTopicService, private val objectMapper: ObjectMapper) {
  private val defaultEventTypeList by lazy { listOf("DEFAULT") }
  private val eventTypeMappingCacheCapacity by lazy { min(IntegrationEventType.entries.count(), MAPPING_CACHE_CAPACITY) }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val MAPPING_CACHE_CAPACITY = 100
    private const val MAPPING_CACHE_LOAD_FACTOR = 0.75f
  }

  @Scheduled(fixedRateString = "\${subscriber-checker.schedule.rate}")
  fun checkSubscriberFilterList() {
    try {
      log.info("Checking subscriber filter list...")

      val apiResponse = integrationApiGateway.getApiAuthorizationConfig()
      val caseInsensitiveSecrets = subscriberProperties.secrets.mapKeys { it.key.uppercase() }

      // Mappings' Cache (endpoints to events)
      val endpointToEventCache: MutableMap<String, List<String>> = lruCache(capacity = eventTypeMappingCacheCapacity)

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
    endpointToEventCache: MutableMap<String, List<String>>,
  ) {
    log.info("Checking filter list for ${clientConfig.key}...")
    try {
      val events = clientConfig.value.endpoints.map { urlPattern ->
        endpointToEventCache[urlPattern] ?: IntegrationEventType.matchesUrlToEvents(urlPattern).map { it.name }
          .also { endpointToEventCache[urlPattern] = it }
      }.asSequence().distinct().flatten().toList()
        .ifEmpty { defaultEventTypeList }
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

  private fun unmarshalFilterList(secretValue: String): SubscriberFilterList {
    if (secretValue == "") {
      return SubscriberFilterList(eventType = listOf("default"), prisonId = null)
    }
    return objectMapper.readValue<SubscriberFilterList>(secretValue)
  }

  private fun logAndCapture(
    message: String,
    e: Exception,
  ) {
    log.error(message, e.message)
    Sentry.captureException(RuntimeException(message, e))
  }

  // Least-Recently-Used cache using LinkedHashMap with accessOrder
  private fun <K, V> lruCache(
    capacity: Int = MAPPING_CACHE_CAPACITY,
    loadFactor: Float = MAPPING_CACHE_LOAD_FACTOR,
  ): MutableMap<K, V> = object : LinkedHashMap<K, V>(capacity, loadFactor, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>) = size > capacity
  }
}
