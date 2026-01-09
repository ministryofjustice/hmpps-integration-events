package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.ConsumerFilters
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.HmppsSecretManagerProperties
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.HmppsSecretManagerProperties.SecretConfig
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway.IntegrationApiGateway
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.ConfigAuthorisation

class SubscriberServiceTests {

  val integrationApiGateway: IntegrationApiGateway = mock()
  val secretsManagerService: SecretsManagerService = mock()
  val integrationEventTopicService: IntegrationEventTopicService = mock()
  private val telemetryService: TelemetryService = mock()
  private lateinit var hmppsSecretManagerProperties: HmppsSecretManagerProperties
  private val objectMapper = ObjectMapper()
  private lateinit var subscriberService: SubscriberService

  private val defaultEventFilter = """{"eventType":["default"]}"""

  @BeforeEach
  fun setUp() {
    `when`(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(
      mapOf(
        "client1" to ConfigAuthorisation(listOf("url1", "url2"), null),
        "client2" to ConfigAuthorisation(listOf("url3"), null),
      ),
    )

    `when`(secretsManagerService.getSecretValue("secret1"))
      .thenReturn("{\"eventType\":[\"MAPPA_DETAIL_CHANGED\"]}")

    hmppsSecretManagerProperties = HmppsSecretManagerProperties(
      provider = "localstack",
      secrets = mapOf(
        "client1" to SecretConfig("secret1", "queue1"),
        "client2" to SecretConfig("secret2", "queue2"),
      ),
    )

    subscriberService = SubscriberService(
      integrationApiGateway,
      hmppsSecretManagerProperties,
      secretsManagerService,
      integrationEventTopicService,
      objectMapper,
      telemetryService,
    )
  }

  @Test
  fun `does not update secret if client not have secrets setup`() {
    // Arrange
    val apiResponse: Map<String, ConfigAuthorisation> = mapOf(
      "client3" to ConfigAuthorisation(
        endpoints = listOf("/v1/persons/.*/risks/mappadetail", "/v1/persons/.*/name"),
        filters = null,
      ),
    )
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)

    // Act
    subscriberService.checkSubscriberFilterList()

    // Assert
    verify(secretsManagerService, never()).getSecretValue(any())
    verify(secretsManagerService, never()).setSecretValue(any(), any())
    verify(integrationEventTopicService, never()).updateSubscriptionAttributes(any(), any(), any())
  }

  @Test
  fun `does not update secret if events match filter`() {
    // Arrange
    val apiResponse: Map<String, ConfigAuthorisation> = mapOf(
      "client1" to ConfigAuthorisation(
        endpoints = listOf("/v1/persons/.*/risks/mappadetail", "/v1/persons/.*/name"),
        filters = null,
      ),
    )
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
    whenever(secretsManagerService.getSecretValue("secret1")).thenReturn(convertEventTypesToFilter("MAPPA_DETAIL_CHANGED", "PERSON_NAME_CHANGED"))
    // Act
    subscriberService.checkSubscriberFilterList()

    // Assert
    verify(secretsManagerService, times(0)).setSecretValue(any(), any())
    verify(integrationEventTopicService, times(0)).updateSubscriptionAttributes(any(), any(), any())
  }

  @Test
  fun `updates secret and subscription if events mismatch filter`() {
    // Arrange
    val apiResponse: Map<String, ConfigAuthorisation> = mapOf(
      "client1" to ConfigAuthorisation(
        endpoints = listOf("/v1/persons/.*/risks/mappadetail", "/v1/persons/.*/name"),
        filters = ConsumerFilters(
          prisons = listOf("MKI"),
        ),
      ),
    )
    val expectedFilter = convertEventTypesToFilter(listOf("MAPPA_DETAIL_CHANGED", "PERSON_NAME_CHANGED"), "MKI")
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
    whenever(secretsManagerService.getSecretValue("secret1")).thenReturn(defaultEventFilter)

    // Act
    subscriberService.checkSubscriberFilterList()

    // Assert
    verify(secretsManagerService, times(1)).setSecretValue("secret1", expectedFilter)
    verify(integrationEventTopicService, times(1)).updateSubscriptionAttributes("queue1", "FilterPolicy", expectedFilter)
  }

  @Test
  fun `does not update secret if filters match filter`() {
    // Arrange
    val apiResponse: Map<String, ConfigAuthorisation> = mapOf(
      "client1" to ConfigAuthorisation(
        endpoints = listOf("/v1/persons/.*/risks/mappadetail", "/v1/persons/.*/name"),
        filters = ConsumerFilters(prisons = listOf("MKI")),
      ),
    )
    val currentFilter = convertEventTypesToFilter(listOf("MAPPA_DETAIL_CHANGED", "PERSON_NAME_CHANGED"), "MKI")
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
    whenever(secretsManagerService.getSecretValue("secret1")).thenReturn(currentFilter)
    // Act
    subscriberService.checkSubscriberFilterList()

    // Assert
    verify(secretsManagerService, times(0)).setSecretValue(any(), any())
    verify(integrationEventTopicService, times(0)).updateSubscriptionAttributes(any(), any(), any())
  }

  @Test
  fun `updates secret and subscription if prisonId mismatches filter`() {
    // Arrange
    val apiResponse: Map<String, ConfigAuthorisation> = mapOf(
      "client1" to ConfigAuthorisation(
        endpoints = listOf("/v1/persons/.*/risks/mappadetail", "/v1/persons/.*/name"),
        filters = ConsumerFilters(
          prisons = listOf("MKI"),
        ),
      ),
    )
    val eventTypes = listOf("MAPPA_DETAIL_CHANGED", "PERSON_NAME_CHANGED")
    val currentFilter = convertEventTypesToFilter(eventTypes, prisonId = "ABC")
    val expectedFilter = convertEventTypesToFilter(eventTypes, prisonId = "MKI")
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
    whenever(secretsManagerService.getSecretValue("secret1")).thenReturn(currentFilter)

    // Act
    subscriberService.checkSubscriberFilterList()

    // Assert
    verify(secretsManagerService, times(1)).setSecretValue("secret1", expectedFilter)
    verify(integrationEventTopicService, times(1)).updateSubscriptionAttributes("queue1", "FilterPolicy", expectedFilter)
  }

  @Test
  fun `updates secret and subscription if prisonId mismatches filter and filter list is empty`() {
    // Arrange
    val apiResponse: Map<String, ConfigAuthorisation> = mapOf(
      "client1" to ConfigAuthorisation(
        endpoints = listOf("/v1/persons/.*/risks/mappadetail", "/v1/persons/.*/addresses"),
        filters = null,
      ),
    )
    val expectedFilter = convertEventTypesToFilter("MAPPA_DETAIL_CHANGED", "PERSON_ADDRESS_CHANGED")
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
    whenever(secretsManagerService.getSecretValue("secret1")).thenReturn("{\"eventType\":[\"MAPPA_DETAIL_CHANGED\"],\"prisonId\":[\"ABC\"]}")

    // Act
    subscriberService.checkSubscriberFilterList()

    // Assert
    verify(secretsManagerService, times(1)).setSecretValue("secret1", expectedFilter)
    verify(integrationEventTopicService, times(1)).updateSubscriptionAttributes("queue1", "FilterPolicy", expectedFilter)
  }

  @Test
  fun `should exclude prisonId from filter policy if not set`() {
    // Arrange
    val apiResponse: Map<String, ConfigAuthorisation> = mapOf(
      "client1" to ConfigAuthorisation(
        endpoints = listOf("/v1/persons/.*/risks/mappadetail", "/v1/persons/.*/name"),
        filters = ConsumerFilters(
          prisons = null,
        ),
      ),
    )
    val expectedFilter = convertEventTypesToFilter(listOf("MAPPA_DETAIL_CHANGED", "PERSON_NAME_CHANGED"))
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
    whenever(secretsManagerService.getSecretValue("secret1")).thenReturn(defaultEventFilter)

    // Act
    subscriberService.checkSubscriberFilterList()

    // Assert
    verify(secretsManagerService, times(1)).setSecretValue("secret1", expectedFilter)
    verify(integrationEventTopicService, times(1)).updateSubscriptionAttributes("queue1", "FilterPolicy", expectedFilter)
  }

  @ParameterizedTest
  @CsvSource(
    "/v1/persons/.*/risks/scores, RISK_SCORE_CHANGED,",
    "/v1/persons/[^/]*$, PERSON_STATUS_CHANGED, PRISONER_MERGED",
  )
  fun `grant access to risk score events if client has access to risk score endpoint`(
    clientConsumerPath: String,
    eventType: String,
    eventType2: String?,
  ) {
    // Arrange
    val apiResponse: Map<String, ConfigAuthorisation> = mapOf(
      "client1" to ConfigAuthorisation(
        endpoints = listOf(clientConsumerPath),
        filters = null,
      ),
    )
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
    whenever(secretsManagerService.getSecretValue("secret1")).thenReturn(defaultEventFilter)
    val expectedFilter = convertEventTypesToFilter(listOfNotNull(eventType, eventType2))

    // Act
    subscriberService.checkSubscriberFilterList()

    // Assert
    verify(secretsManagerService, times(1)).setSecretValue("secret1", expectedFilter)
    verify(integrationEventTopicService, times(1)).updateSubscriptionAttributes("queue1", "FilterPolicy", expectedFilter)
  }

  @Test
  fun `set filter list to be default if client has no event access`() {
    // Arrange
    val apiResponse: Map<String, ConfigAuthorisation> = mapOf(
      "client1" to ConfigAuthorisation(
        endpoints = listOf("/v1/otherendpoints"),
        filters = null,
      ),
    )
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
    whenever(secretsManagerService.getSecretValue("secret1"))
      .thenReturn("{\"eventType\":[\"MAPPA_DETAIL_CHANGED\"]}")

    // Act
    subscriberService.checkSubscriberFilterList()

    // Assert
    verify(secretsManagerService, times(1)).setSecretValue("secret1", defaultEventFilter)
    verify(integrationEventTopicService, times(1)).updateSubscriptionAttributes("queue1", "FilterPolicy", defaultEventFilter)
  }

  @Test
  fun `set filter list if current filter is empty`() = testSubscriptionFilter(
    endpoints = listOf("/v1/persons/[^/]*$"),
    expectedEventTypes = listOf("PERSON_STATUS_CHANGED", "PRISONER_MERGED"),
    currentFilter = "",
  )

  @Test
  fun `does not update filter, if current filter is empty and no matching event`() = testSubscriptionFilterHasNoUpdate(
    endpoints = listOf("/v1/status"),
    currentFilter = "",
  )

  @Test
  fun `should grant access to person events, if client has access to person endpoint`() {
    // Arrange
    val consumer = "client1"
    val endpoints = listOf("/v1/persons/[^/]*$")
    val expectedEventTypes = listOf(
      "PERSON_STATUS_CHANGED",
      "PRISONER_MERGED",
    )

    // Act, Assert
    testSubscriptionFilter(endpoints, expectedEventTypes, consumer)
  }

  @ParameterizedTest
  @CsvSource(
    textBlock = """
      /v1/persons/.*/plp-induction-schedule/history       , PLP_INDUCTION_SCHEDULE_CHANGED
      /v1/persons/.*/prisoner-base-location               , PRISONER_BASE_LOCATION_CHANGED,
      /v1/persons/[^/]+/prisoner-base-location            , PRISONER_BASE_LOCATION_CHANGED
      /v1/persons/.*/education/assessments                , PERSON_EDUCATION_ASSESSMENTS_CHANGED""",
  )
  fun `should grant access to event, if client has access to relevant endpoint`(endpoint: String, eventType: String) = testSubscriptionFilter(
    endpoints = listOf(endpoint),
    expectedEventTypes = listOf(eventType),
  )

  @Test
  fun `should NOT grant access to event, if client has access to endpoints without relevant event`() = testSubscriptionFilterHasNoUpdate(
    // should not update default filter
    endpoints = listOf(
      "/v1/status",
      "/v1/persons/.*/plp-induction-schedule",
      "/v1/persons/[^/]+/expression-of-interest/jobs/[^/]+$",
    ),
  )

  @Nested
  @DisplayName("Given error, while checking subscriber filter list")
  inner class GivenErrorCheckingSubscriberFilter {

    @Test
    fun `log exception to telemetry, when fail to obtain authorization configuration`() {
      // Arrange
      val errorMessage = "Error checking filter list"
      whenever(integrationApiGateway.getApiAuthorizationConfig()).thenThrow(RuntimeException::class.java)

      // Act
      subscriberService.checkSubscriberFilterList()

      // Assert
      verify(telemetryService, times(1)).captureException(argThat { message == errorMessage })
    }

    @Test
    fun `log exception to telemetry, when fail to refreshing a client filter`() {
      // Arrange
      val client = "client1"
      val apiResponse: Map<String, ConfigAuthorisation> = mapOf(client to ConfigAuthorisation(emptyList(), null))
      val secretId = "secret1"
      val errorMessage = "Error checking filter list for $client"
      whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
      // error when getting secret
      whenever(secretsManagerService.getSecretValue(secretId)).thenThrow(RuntimeException::class.java)

      // Act
      subscriberService.checkSubscriberFilterList()

      // Assert
      verify(telemetryService, times(1)).captureException(argThat { message == errorMessage })
    }
  }

  @Nested
  @DisplayName("Given a few clients")
  inner class GivenMultipleClients {
    @Test
    fun `should update secret and subscription, with repeating URLs`() {
      // Arrange
      val repeatingUrls = listOf(
        "/v1/persons/[^/]*$",
        "/v1/status",
      )
      val client1Endpoints = listOf(
        repeatingUrls[0],
        "/v1/persons/[^/]+/prisoner-base-location",
        "/v1/persons/.*/education/assessments",
        repeatingUrls[1],
      )
      val client2Endpoints = listOf(
        repeatingUrls[0],
        "/v1/persons/.*/addresses",
        "/v1/persons/.*/status-information",
        "/v1/hmpps/reference-data",
        repeatingUrls[1],
      )
      val apiResponse: Map<String, ConfigAuthorisation> = mapOf(
        "client1" to ConfigAuthorisation(client1Endpoints, null),
        "client2" to ConfigAuthorisation(client2Endpoints, null),
      )
      val secretNames = listOf("secret1", "secret2")
      val queueNames = listOf("queue1", "queue2")
      whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
      secretNames.forEach { whenever(secretsManagerService.getSecretValue(it)).thenReturn(defaultEventFilter) }

      // Act
      subscriberService.checkSubscriberFilterList()

      // Assert
      // 1) Secret has been set to the consumer filter
      verify(secretsManagerService, times(2)).setSecretValue(argThat { this in secretNames }, any())
      // 2) Filter has been updated to subscription
      verify(integrationEventTopicService, times(2)).updateSubscriptionAttributes(argThat { this in queueNames }, eq("FilterPolicy"), any())
    }
  }

  private fun testSubscriptionFilter(
    endpoints: List<String> = emptyList(),
    expectedEventTypes: List<String> = emptyList(),
    consumer: String = "client1",
    secretId: String = "secret1",
    queueName: String = "queue1",
    consumerFilters: ConsumerFilters? = null,
    currentFilter: String = defaultEventFilter,
  ) {
    // Arrange
    val apiResponse: Map<String, ConfigAuthorisation> = mapOf(
      consumer to ConfigAuthorisation(
        endpoints = endpoints.sorted(), // API response endpoints are sorted
        filters = consumerFilters,
      ),
    )
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
    whenever(secretsManagerService.getSecretValue(secretId)).thenReturn(currentFilter)

    // Act
    subscriberService.checkSubscriberFilterList()

    // Assert
    val assertFilterString: (KArgumentCaptor<String>, List<String>) -> Unit = { filterCaptor, expectedEventTypes ->
      extractEventTypesFrom(filterCaptor.singleValue).also {
        // All event types are expected
        assertThat(it.toSet()).hasSameElementsAs(expectedEventTypes.toSet())
        // Event types are in stable order
        assertThat(it).isEqualTo(expectedEventTypes)
      }
    }
    // 1) correct secret has been set to the consumer filter
    argumentCaptor<String>().let { filterCaptor ->
      verify(secretsManagerService, times(1))
        .setSecretValue(eq(secretId), filterCaptor.capture())
      assertFilterString(filterCaptor, expectedEventTypes)
    }
    // 2) correct filter has been updated to subscription
    argumentCaptor<String>().let { filterCaptor ->
      verify(integrationEventTopicService, times(1))
        .updateSubscriptionAttributes(eq(queueName), eq("FilterPolicy"), filterCaptor.capture())
      assertFilterString(filterCaptor, expectedEventTypes)
    }
  }

  private fun testSubscriptionFilterHasNoUpdate(
    endpoints: List<String> = emptyList(),
    consumer: String = "client1",
    secretId: String = "secret1",
    queueName: String = "queue1",
    consumerFilters: ConsumerFilters? = null,
    currentFilter: String = defaultEventFilter,
  ) {
    // Arrange
    val apiResponse: Map<String, ConfigAuthorisation> = mapOf(
      consumer to ConfigAuthorisation(
        endpoints = endpoints.sorted(), // API response endpoints are sorted
        filters = consumerFilters,
      ),
    )
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
    whenever(secretsManagerService.getSecretValue(secretId)).thenReturn(currentFilter)

    // Act
    subscriberService.checkSubscriberFilterList()

    // Assert
    // 1) Consumer filter has not been updated
    verify(secretsManagerService, never()).setSecretValue(eq(secretId), anyString())
    // 2) Subscription's filter has not been updated
    verify(integrationEventTopicService, never()).updateSubscriptionAttributes(eq(queueName), eq("FilterPolicy"), anyString())
  }

  // filter is in this format: {"eventType":["eventType1", "eventType2", ...]}
  private fun extractEventTypesFrom(filter: String) = objectMapper.readTree(filter)["eventType"].map { it.textValue() }.toList()
  private fun convertEventTypesToFilter(vararg eventType: String) = convertEventTypesToFilter(eventType.toList())
  private fun convertEventTypesToFilter(eventTypes: List<String>) = """{"eventType":[${eventTypes.joinToString(separator = ",") { "\"$it\"" }}]}"""
  private fun convertEventTypesToFilter(eventTypes: List<String>, prisonId: String) = """
  {
    "eventType": [${eventTypes.joinToString(separator = ",") { "\"$it\"" }}],
    "prisonId": ["$prisonId"]
  }
  """.trimIndent().replace("\n", "").replace(" ", "")
}
