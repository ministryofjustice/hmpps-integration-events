package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.HmppsSecretManagerProperties
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway.IntegrationApiGateway
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.SubscriberFilterList
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventTypes

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
    val caseInsensitiveSecrets = subscriberProperties.secrets.mapKeys { it.key.uppercase() }

    apiResponse.filter { client -> caseInsensitiveSecrets.containsKey(client.key.uppercase()) }
      .forEach { refreshClientFilter(it, caseInsensitiveSecrets[it.key.uppercase()]!!) }
  }

  private fun refreshClientFilter(clientConfig: Map.Entry<String, List<String>>, subscriber: HmppsSecretManagerProperties.SecretConfig) {
    val events = clientConfig.value
      .flatMap { url ->
        listOfNotNull(
          url.takeIf { it.contains("/v1/persons/.*/risks/mappadetail") }?.let { IntegrationEventTypes.MAPPA_DETAIL_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/.*/risks/scores") }?.let { IntegrationEventTypes.RISK_SCORE_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/.*/sentences/latest-key-dates-and-adjustments") }?.let { IntegrationEventTypes.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE.name },
          url.takeIf { it.contains("/v1/persons/.*/status-information") }?.let { IntegrationEventTypes.PROBATION_STATUS_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/.*/risks/dynamic") }?.let { IntegrationEventTypes.DYNAMIC_RISKS_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/[^/]*$") }?.let { IntegrationEventTypes.PERSON_STATUS_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/.*/alerts/pnd") }?.let { IntegrationEventTypes.PND_ALERTS_CHANGED.name },
          url.takeIf { it.contains("/v1/pnd/persons/.*/alerts") }?.let { IntegrationEventTypes.PND_ALERTS_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/.*/licences/conditions") }?.let { IntegrationEventTypes.LICENCE_CONDITION_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/.*/risks/serious-harm") }?.let { IntegrationEventTypes.RISK_OF_SERIOUS_HARM_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/.*/plp-induction-schedule") }?.let { IntegrationEventTypes.PLP_INDUCTION_SCHEDULE_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/.*/plp-review-schedule") }?.let { IntegrationEventTypes.PLP_REVIEW_SCHEDULE_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/.*/addresses") }?.let { IntegrationEventTypes.PERSON_ADDRESS_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/.*/person-responsible-officer") }?.let { IntegrationEventTypes.RESPONSIBLE_OFFICER_CHANGED.name },
        )
      }
      .ifEmpty { listOf("DEFAULT") }

    val secretValue = secretsManagerService.getSecretValue(subscriber.secretId)
    val filterList = objectMapper.readValue<SubscriberFilterList>(secretValue)

    if (filterList.eventType != events && filterList.eventType.isNotEmpty()) {
      val filterPolicy = objectMapper.writeValueAsString(SubscriberFilterList(events))
      secretsManagerService.setSecretValue(subscriber.secretId, filterPolicy)
      integrationEventTopicService.updateSubscriptionAttributes(subscriber.queueId, "FilterPolicy", filterPolicy)
    }
  }
}
