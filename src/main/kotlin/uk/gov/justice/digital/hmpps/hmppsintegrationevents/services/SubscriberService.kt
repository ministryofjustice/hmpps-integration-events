package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.HmppsSecretManagerProperties
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway.IntegrationApiGateway
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.SubscriberFilterList
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.OutgoingEventType

@Service
class SubscriberService(
  private val integrationApiGateway: IntegrationApiGateway,
  private val subscriberProperties: HmppsSecretManagerProperties,
  private val secretsManagerService: SecretsManagerService,
  private val integrationEventTopicService: IntegrationEventTopicService,
  private val objectMapper: ObjectMapper,
) {
  @Scheduled(fixedRateString = "\${subscriber-checker.schedule.rate}")
  fun checkSubscriberFilterList() {
    try {
      val apiResponse = integrationApiGateway.getApiAuthorizationConfig()
      val caseInsensitiveSecrets = subscriberProperties.secrets.mapKeys { it.key.uppercase() }

      apiResponse.filter { client -> caseInsensitiveSecrets.containsKey(client.key.uppercase()) }
        .forEach { refreshClientFilter(it, caseInsensitiveSecrets[it.key.uppercase()]!!) }
    } catch (ex: Exception) {}
  }

  private fun refreshClientFilter(clientConfig: Map.Entry<String, List<String>>, subscriber: HmppsSecretManagerProperties.SecretConfig) {
    val events = clientConfig.value
      .flatMap { url ->
        listOfNotNull(
          url.takeIf { it.contains("/v1/persons/.*/risks/mappadetail") }?.let { OutgoingEventType.MAPPA_DETAIL_CHANGED.name },
        )
      }
      .ifEmpty { listOf("DEFAULT") }

    val secretValue = secretsManagerService.getSecretValue(subscriber.secretId)
    val filterList = objectMapper.readValue<SubscriberFilterList>(secretValue)

    if (filterList.eventType != events && filterList.eventType.isNotEmpty()) {
      val filterPolicy = objectMapper.writeValueAsString(SubscriberFilterList(events))
      secretsManagerService.setSecretValue(subscriber.secretId, filterPolicy)
      integrationEventTopicService.updateSubscriptionAttributes(subscriber.queueName, "FilterPolicy", filterPolicy)
    }
  }
}
