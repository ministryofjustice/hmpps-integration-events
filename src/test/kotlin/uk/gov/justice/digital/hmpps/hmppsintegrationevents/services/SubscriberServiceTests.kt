package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.HmppsSecretManagerProperties
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.HmppsSecretManagerProperties.SecretConfig
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway.IntegrationApiGateway

@ActiveProfiles("test")
@JsonTest
class SubscriberServiceTests {

  val integrationApiGateway: IntegrationApiGateway = mock()
  val secretsManagerService: SecretsManagerService = mock()
  val integrationEventTopicService: IntegrationEventTopicService = mock()
  private lateinit var hmppsSecretManagerProperties: HmppsSecretManagerProperties
  private val objectMapper = ObjectMapper()
  private lateinit var subscriberService: SubscriberService

  @BeforeEach
  fun setUp() {
    `when`(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(
      mapOf(
        "client1" to listOf("url1", "url2"),
        "client2" to listOf("url3"),
      ),
    )
    `when`(secretsManagerService.getSecretValue("secret1")).thenReturn("{\"eventType\":[\"MAPPA_DETAIL_CHANGED\"]}")

    hmppsSecretManagerProperties = HmppsSecretManagerProperties(provider = "localstack", secrets = mapOf("client1" to SecretConfig("secret1", "queue1"), "client2" to SecretConfig("secret2", "queue2")))
    subscriberService = SubscriberService(integrationApiGateway, hmppsSecretManagerProperties, secretsManagerService, integrationEventTopicService, objectMapper)
  }

  @Test
  fun `does not update secret if client not have secrets setup`() {
    // Arrange
    val apiResponse: Map<String, List<String>> = mapOf("client3" to listOf("/v1/persons/.*/risks/mappadetail"))
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
    // Act
    subscriberService.checkSubscriberFilterList()

    // Assert
    verify(secretsManagerService, times(0)).getSecretValue(any())
    verify(secretsManagerService, times(0)).setSecretValue(any(), any())
    verify(integrationEventTopicService, times(0)).updateSubscriptionAttributes(any(), any(), any())
  }

  @Test
  fun `does not update secret if events match filter`() {
    // Arrange
    val apiResponse: Map<String, List<String>> = mapOf("client1" to listOf("/v1/persons/.*/risks/mappadetail"))
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
    whenever(secretsManagerService.getSecretValue("secret1")).thenReturn("{\"eventType\":[\"MAPPA_DETAIL_CHANGED\"]}")
    // Act
    subscriberService.checkSubscriberFilterList()

    // Assert
    verify(secretsManagerService, times(0)).setSecretValue(any(), any())
    verify(integrationEventTopicService, times(0)).updateSubscriptionAttributes(any(), any(), any())
  }

  @Test
  fun `updates secret and subscription if events mismatch filter`() {
    // Arrange
    val apiResponse: Map<String, List<String>> = mapOf("client1" to listOf("/v1/persons/.*/risks/mappadetail"))
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
    whenever(secretsManagerService.getSecretValue("secret1")).thenReturn("{\"eventType\":[\"DEFAULT\"]}")
    // Act
    subscriberService.checkSubscriberFilterList()

    // Assert
    verify(secretsManagerService, times(1)).setSecretValue("secret1", "{\"eventType\":[\"MAPPA_DETAIL_CHANGED\"]}")
    verify(integrationEventTopicService, times(1)).updateSubscriptionAttributes("queue1", "FilterPolicy", "{\"eventType\":[\"MAPPA_DETAIL_CHANGED\"]}")
  }

  @ParameterizedTest
  @CsvSource("/v1/persons/.*/risks/scores, RISK_SCORE_CHANGED", "/v1/persons/\\.*+[^/]*\$, PERSON_STATUS_CHANGED")
  fun `grant access to risk score events if client has access to risk score endpoint`(clientConsumerPath: String, eventType: String) {
    // Arrange
    val apiResponse: Map<String, List<String>> = mapOf("client1" to listOf(clientConsumerPath))
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
    whenever(secretsManagerService.getSecretValue("secret1")).thenReturn("{\"eventType\":[\"DEFAULT\"]}")
    // Act
    subscriberService.checkSubscriberFilterList()

    // Assert
    verify(secretsManagerService, times(1)).setSecretValue("secret1", "{\"eventType\":[\"$eventType\"]}")
    verify(integrationEventTopicService, times(1)).updateSubscriptionAttributes("queue1", "FilterPolicy", "{\"eventType\":[\"$eventType\"]}")
  }

  @Test
  fun `set filter list to be DEFAULT if client has no event access`() {
    // Arrange
    val apiResponse: Map<String, List<String>> = mapOf("client1" to listOf("/v1/otherendpoints"))
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
    whenever(secretsManagerService.getSecretValue("secret1")).thenReturn("{\"eventType\":[\"MAPPA_DETAIL_CHANGED\"]}")
    // Act
    subscriberService.checkSubscriberFilterList()

    // Assert
    verify(secretsManagerService, times(1)).setSecretValue("secret1", "{\"eventType\":[\"DEFAULT\"]}")
    verify(integrationEventTopicService, times(1)).updateSubscriptionAttributes("queue1", "FilterPolicy", "{\"eventType\":[\"DEFAULT\"]}")
  }
}
