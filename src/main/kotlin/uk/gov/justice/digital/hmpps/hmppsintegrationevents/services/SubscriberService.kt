package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.HmppsSecretManagerProperties
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway.IntegrationApiGateway
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.SubscriberFilterList
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IncomingEventType

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
    val apiResponse = integrationApiGateway.getApiAuthorizationConfig()

    apiResponse.filter { client -> subscriberProperties.secrets.containsKey(client.key) }
      .forEach { refreshClientFilter(it, subscriberProperties.secrets[it.key]!!) }
  }

  private fun refreshClientFilter(clientConfig: Map.Entry<String, List<String>>, subscriber: HmppsSecretManagerProperties.SecretConfig) {
    val events = clientConfig.value
      .flatMap { url ->
        listOfNotNull(
          url.takeIf { it.contains("/v1/persons/.*/risks/mappadetail") }?.let { IncomingEventType.REGISTRATION_ADDED.name },
        )
      }
      .ifEmpty { listOf("DEFAULT") }

    val secretValue = secretsManagerService.getSecretValue(subscriber.secretName)
    val filterList = objectMapper.readValue<SubscriberFilterList>(secretValue)

    if (filterList.eventType != events && filterList.eventType.isNotEmpty()) {
      val filterPolicy = objectMapper.writeValueAsString(SubscriberFilterList(events))
      secretsManagerService.setSecretValue(subscriber.secretName, filterPolicy)
      integrationEventTopicService.updateSubscriptionAttributes(subscriber.queueName, "FilterPolicy", filterPolicy)
    }
  }
}
