package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.scheduling.annotation.Scheduled
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
) {
  @Scheduled(fixedRateString = "\${subscriber-checker.schedule.rate}")
  fun checkSubscriberFilterList() {
    val apiResponse = integrationApiGateway.getApiAuthorizationConfig()
    val caseInsensitiveSecrets = subscriberProperties.secrets.mapKeys { it.key.uppercase() }

    apiResponse.filter { client -> caseInsensitiveSecrets.containsKey(client.key.uppercase()) }
      .forEach { refreshClientFilter(it, caseInsensitiveSecrets[it.key.uppercase()]!!) }
  }

  private fun refreshClientFilter(
    clientConfig: Map.Entry<String, ConfigAuthorisation>,
    subscriber: HmppsSecretManagerProperties.SecretConfig,
  ) {
    val events = clientConfig.value.endpoints
      .flatMap { url ->
        listOfNotNull(
          url.takeIf { it.contains("/v1/persons/.*/risks/mappadetail") }?.let { IntegrationEventType.MAPPA_DETAIL_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/.*/risks/scores") }?.let { IntegrationEventType.RISK_SCORE_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/.*/sentences/latest-key-dates-and-adjustments") }?.let { IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE.name },
          url.takeIf { it.contains("/v1/persons/.*/status-information") }?.let { IntegrationEventType.PROBATION_STATUS_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/.*/risks/dynamic") }?.let { IntegrationEventType.DYNAMIC_RISKS_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/[^/]*$") }?.let { IntegrationEventType.PERSON_STATUS_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/.*/alerts/pnd") }?.let { IntegrationEventType.PND_ALERTS_CHANGED.name },
          url.takeIf { it.contains("/v1/pnd/persons/.*/alerts") }?.let { IntegrationEventType.PND_ALERTS_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/.*/licences/conditions") }?.let { IntegrationEventType.LICENCE_CONDITION_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/.*/risks/serious-harm") }?.let { IntegrationEventType.RISK_OF_SERIOUS_HARM_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/.*/plp-induction-schedule") }?.let { IntegrationEventType.PLP_INDUCTION_SCHEDULE_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/.*/plp-review-schedule") }?.let { IntegrationEventType.PLP_REVIEW_SCHEDULE_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/.*/addresses") }?.let { IntegrationEventType.PERSON_ADDRESS_CHANGED.name },
          url.takeIf { it.contains("/v1/persons/.*/person-responsible-officer") }?.let { IntegrationEventType.RESPONSIBLE_OFFICER_CHANGED.name },
        )
      }
      .ifEmpty { listOf("DEFAULT") }
    val prisonIds = clientConfig.value.filters?.prisons

    val secretValue = secretsManagerService.getSecretValue(subscriber.secretId)
    val filterList = objectMapper.readValue<SubscriberFilterList>(secretValue)

    if (filterList.eventType != events && filterList.eventType.isNotEmpty()) {
      val filterPolicy = objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL).writeValueAsString(SubscriberFilterList(eventType = events, prisonId = prisonIds))
      secretsManagerService.setSecretValue(subscriber.secretId, filterPolicy)
      integrationEventTopicService.updateSubscriptionAttributes(subscriber.queueId, "FilterPolicy", filterPolicy)
    }
  }
}
