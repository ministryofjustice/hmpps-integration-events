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
    val events = clientConfig.value.endpoints.mapNotNull { endpointMap[it]?.name }.ifEmpty { listOf("DEFAULT") }
    val prisonIds = clientConfig.value.filters?.prisons

    val secretValue = secretsManagerService.getSecretValue(subscriber.secretId)
    val existingFilterList = objectMapper.readValue<SubscriberFilterList>(secretValue)
    val updatedFilterList = SubscriberFilterList(eventType = events, prisonId = prisonIds)

    if (updatedFilterList != existingFilterList) {
      val filterPolicy = objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL).writeValueAsString(updatedFilterList)
      secretsManagerService.setSecretValue(subscriber.secretId, filterPolicy)
      integrationEventTopicService.updateSubscriptionAttributes(subscriber.queueId, "FilterPolicy", filterPolicy)
    }
  }

  private val endpointMap: Map<String, IntegrationEventType> by lazy {
    mapOf(
      "/v1/persons/.*/risks/mappadetail" to IntegrationEventType.MAPPA_DETAIL_CHANGED,
      "/v1/persons/.*/risks/scores" to IntegrationEventType.RISK_SCORE_CHANGED,
      "/v1/persons/.*/sentences/latest-key-dates-and-adjustments" to IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE,
      "/v1/persons/.*/status-information" to IntegrationEventType.PROBATION_STATUS_CHANGED,
      "/v1/persons/.*/risks/dynamic" to IntegrationEventType.DYNAMIC_RISKS_CHANGED,
      "/v1/persons/[^/]*$" to IntegrationEventType.PERSON_STATUS_CHANGED,
      "/v1/persons/.*/alerts/pnd" to IntegrationEventType.PND_ALERTS_CHANGED,
      "/v1/pnd/persons/.*/alerts" to IntegrationEventType.PND_ALERTS_CHANGED,
      "/v1/persons/.*/licences/conditions" to IntegrationEventType.LICENCE_CONDITION_CHANGED,
      "/v1/persons/.*/risks/serious-harm" to IntegrationEventType.RISK_OF_SERIOUS_HARM_CHANGED,
      "/v1/persons/.*/plp-induction-schedule" to IntegrationEventType.PLP_INDUCTION_SCHEDULE_CHANGED,
      "/v1/persons/.*/plp-review-schedule" to IntegrationEventType.PLP_REVIEW_SCHEDULE_CHANGED,
      "/v1/persons/.*/addresses" to IntegrationEventType.PERSON_ADDRESS_CHANGED,
      "/v1/persons/.*/person-responsible-officer" to IntegrationEventType.RESPONSIBLE_OFFICER_CHANGED,
    )
  }
}
