package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.HmppsSecretManagerProperties
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.extensions.normalisePath
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

      apiResponse.filter { client -> caseInsensitiveSecrets.containsKey(client.key.uppercase()) }
        .forEach { refreshClientFilter(it, caseInsensitiveSecrets[it.key.uppercase()]!!) }

      log.info("Subscriber filter list checked")
    } catch (e: Exception) {
      logAndCapture("Error checking filter list", e)
    }
  }

  private fun refreshClientFilter(clientConfig: Map.Entry<String, ConfigAuthorisation>, subscriber: HmppsSecretManagerProperties.SecretConfig) {
    log.info("Checking filter list for ${clientConfig.key}...")
    try {
      val events = clientConfig.value.endpoints
        .map { normalisePath(it) }
        .mapNotNull { endpointMap[it]?.map { eventType -> eventType.name } }.flatten()
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
      return SubscriberFilterList(eventType = defaultEventTypeList, prisonId = null)
    }
    return objectMapper.readValue<SubscriberFilterList>(secretValue)
  }

  private val endpointMap: Map<String, List<IntegrationEventType>> by lazy {
    IntegrationEventType.entries.groupBy { normalisePath(it.pathTemplate) }
  }

  private fun logAndCapture(
    message: String,
    e: Exception,
  ) {
    log.error(message, e.message)
    telemetryService.captureException(RuntimeException(message, e))
  }
}
