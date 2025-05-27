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
      "/v1/persons/.*/licences/conditions" to IntegrationEventType.LICENCE_CONDITION_CHANGED,
      "/v1/persons/.*/risks/serious-harm" to IntegrationEventType.RISK_OF_SERIOUS_HARM_CHANGED,
      "/v1/persons/.*/plp-induction-schedule" to IntegrationEventType.PLP_INDUCTION_SCHEDULE_CHANGED,
      "/v1/persons/.*/plp-review-schedule" to IntegrationEventType.PLP_REVIEW_SCHEDULE_CHANGED,
      "/v1/persons/.*/addresses" to IntegrationEventType.PERSON_ADDRESS_CHANGED,
      "/v1/persons/.*/contacts[^/]*$" to IntegrationEventType.PERSON_CONTACTS_CHANGED,
      "/v1/persons/.*/iep-level" to IntegrationEventType.PERSON_IEP_LEVEL_CHANGED,
      "/v1/persons/.*/visitor/.*/restrictions" to IntegrationEventType.PERSON_VISITOR_RESTRICTIONS_CHANGED,
      "/v1/persons/.*/visit-restrictions" to IntegrationEventType.PERSON_VISIT_RESTRICTIONS_CHANGED,
      "/v1/persons/.*/visit-orders" to IntegrationEventType.PERSON_VISIT_ORDERS_CHANGED,
      "/v1/persons/.*/visit/future" to IntegrationEventType.PERSON_FUTURE_VISITS_CHANGED,
      "/v1/persons/.*/alerts" to IntegrationEventType.PERSON_ALERTS_CHANGED,
      "/v1/persons/.*/alerts/pnd" to IntegrationEventType.PERSON_PND_ALERTS_CHANGED,
      "/v1/pnd/persons/.*/alerts" to IntegrationEventType.PERSON_PND_ALERTS_CHANGED,
      "/v1/persons/.*/case-notes" to IntegrationEventType.PERSON_CASE_NOTES_CHANGED,
      "/v1/persons/.*/name" to IntegrationEventType.PERSON_NAME_CHANGED,
      "/v1/persons/.*/cell-location" to IntegrationEventType.PERSON_CELL_LOCATION_CHANGED,
      "/v1/persons/.*/risks/categories" to IntegrationEventType.PERSON_RISK_CATEGORIES_CHANGED,
      "/v1/persons/.*/sentences" to IntegrationEventType.PERSON_SENTENCES_CHANGED,
      "/v1/persons/.*/offences" to IntegrationEventType.PERSON_OFFENCES_CHANGED,
      "/v1/persons/.*/person-responsible-officer" to IntegrationEventType.PERSON_RESPONSIBLE_OFFICER_CHANGED,
      "/v1/persons/.*/protected-characteristics" to IntegrationEventType.PERSON_PROTECTED_CHARACTERISTICS_CHANGED,
      "/v1/persons/.*/reported-adjudications" to IntegrationEventType.PERSON_REPORTED_ADJUDICATIONS_CHANGED,
      "/v1/persons/.*/number-of-children" to IntegrationEventType.PERSON_NUMBER_OF_CHILDREN_CHANGED,
      "/v1/persons/.*/physical-characteristics" to IntegrationEventType.PERSON_PHYSICAL_CHARACTERISTICS_CHANGED,
      "/v1/persons/.*/images" to IntegrationEventType.PERSON_IMAGES_CHANGED,
      "/v1/persons/.*/images/.*" to IntegrationEventType.PERSON_IMAGE_CHANGED,
      "/v1/prison/prisoners" to IntegrationEventType.PRISONERS_CHANGED,
      "/v1/prison/prisoners/[^/]*$" to IntegrationEventType.PRISONER_CHANGED,
      "/v1/prison/.*/prisoners/[^/]*/balances$" to IntegrationEventType.PRISONER_BALANCES_CHANGED,
      "/v1/prison/.*/prisoners/.*/accounts/.*/balances" to IntegrationEventType.PRISONER_ACCOUNT_BALANCES_CHANGED,
      "/v1/prison/.*/prisoners/.*/accounts/.*/transactions" to IntegrationEventType.PRISONER_ACCOUNT_TRANSACTIONS_CHANGED,
      "/v1/prison/.*/prisoners/.*/non-associations" to IntegrationEventType.PRISONER_NON_ASSOCIATIONS_CHANGED,
      "/v1/prison/.*/visit/search[^/]*$" to IntegrationEventType.PRISON_VISITS_CHANGED,
      "/v1/prison/.*/residential-hierarchy" to IntegrationEventType.PRISON_RESIDENTIAL_HIERARCHY_CHANGED,
      "/v1/prison/.*/location/[^/]*$" to IntegrationEventType.PRISON_LOCATION_CHANGED,
      "/v1/prison/.*/residential-details" to IntegrationEventType.PRISON_RESIDENTIAL_DETAILS_CHANGED,
      "/v1/prison/.*/capacity" to IntegrationEventType.PRISON_CAPACITY_CHANGED,
      "/v1/visit/[^/]*$" to IntegrationEventType.VISIT_CHANGED,
      "/v1/visit/id/by-client-ref/[^/]*$" to IntegrationEventType.VISIT_FROM_EXTERNAL_SYSTEM_CREATED,
      "/v1/contacts/[^/]*$" to IntegrationEventType.CONTACT_CHANGED,
      "/v1/persons/.*/health-and-diet" to IntegrationEventType.PERSON_HEALTH_AND_DIET_CHANGED,
      "/v1/persons/.*/care-needs" to IntegrationEventType.PERSON_CARE_NEEDS_CHANGED,
      "/v1/persons/.*/languages" to IntegrationEventType.PERSON_LANGUAGES_CHANGED,
    )
  }
}
