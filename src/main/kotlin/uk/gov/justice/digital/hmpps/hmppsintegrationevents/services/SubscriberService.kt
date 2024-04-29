package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.SetSubscriptionAttributesRequest
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.HmppsSecretManagerProperties
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway.IntegrationApiGateway
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.SubscriberFilterList
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.EventTypeValue
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Service
class SubscriberService(
  private val integrationApiGateway: IntegrationApiGateway,
  private val subscriberProperties: HmppsSecretManagerProperties,
  private val secretsManagerService: SecretsManagerService,

  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {

  private final val hmppsEventsTopicSnsClient: SnsAsyncClient

  init {
    val hmppsEventTopic = hmppsQueueService.findByTopicId("integrationeventtopic")
    hmppsEventsTopicSnsClient = hmppsEventTopic!!.snsClient
  }

  @Scheduled(fixedRateString = "\${notifier.schedule.rate}")
  fun checkSubscriberFilterList() {
    val apiResponse = integrationApiGateway.getApiAuthorizationConfig()

    apiResponse.filter { client -> subscriberProperties.secrets.containsKey(client.key) }
      .forEach { refreshClientFilter(it, subscriberProperties.secrets[it.key]!!) }
  }

  fun refreshClientFilter(clientConfig: Map.Entry<String, List<String>>, subscriber: HmppsSecretManagerProperties.SecretConfig) {
    val events = clientConfig.value
      .flatMap { url ->
        listOfNotNull(
          url.takeIf { it.contains("/v1/persons/.*/risks/mappadetail") }?.let { EventTypeValue.REGISTRATION_ADDED.name },
        )
      }

    val secretValue = secretsManagerService.getSecretValue(subscriber.secretName)
    val filterList = objectMapper.readValue<SubscriberFilterList>(secretValue)

    if (filterList.eventType != events) {
      val filterPolicy = objectMapper.writeValueAsString(SubscriberFilterList(events))

      secretsManagerService.setSecretValue(subscriber.secretName, filterPolicy)

      val request = SetSubscriptionAttributesRequest.builder()
        .subscriptionArn(subscriber.subscriberArn)
        .attributeName("FilterPolicy")
        .attributeValue(filterPolicy)
        .build()
      hmppsEventsTopicSnsClient.setSubscriptionAttributes(request)
    }
  }
}
