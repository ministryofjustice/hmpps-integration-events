package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.ConsumerFilters
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.HmppsSecretManagerProperties
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.HmppsSecretManagerProperties.SecretConfig
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway.IntegrationApiGateway
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.ConfigAuthorisation

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
        "curious1" to SecretConfig("curious1-filter", "curious1-queue"),
        "police1" to SecretConfig("police1-filter", "police1-queue"),
        "mappa1" to SecretConfig("mappa1-filter", "mappa1-queue"),
        "privateprison1" to SecretConfig("privateprison1-filter", "privateprison1-queue"),
      ),
    )

    subscriberService = SubscriberService(
      integrationApiGateway,
      hmppsSecretManagerProperties,
      secretsManagerService,
      integrationEventTopicService,
      objectMapper,
    )
  }

  @Test
  fun `does not update secret if client not have secrets setup`() {
    // Arrange
    val apiResponse: Map<String, ConfigAuthorisation> = mapOf(
      "client3" to ConfigAuthorisation(
        endpoints = listOf("/v1/persons/.*/risks/mappadetail"),
        filters = null,
      ),
    )
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
    val apiResponse: Map<String, ConfigAuthorisation> = mapOf(
      "client1" to ConfigAuthorisation(
        endpoints = listOf("/v1/persons/.*/risks/mappadetail"),
        filters = null,
      ),
    )
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
    val apiResponse: Map<String, ConfigAuthorisation> = mapOf(
      "client1" to ConfigAuthorisation(
        endpoints = listOf("/v1/persons/.*/risks/mappadetail"),
        filters = ConsumerFilters(
          prisons = listOf("MKI"),
        ),
      ),
    )
    val expectedMessageAttributes = "{\"eventType\":[\"MAPPA_DETAIL_CHANGED\"],\"prisonId\":[\"MKI\"]}"
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
    whenever(secretsManagerService.getSecretValue("secret1")).thenReturn("{\"eventType\":[\"DEFAULT\"]}")

    // Act
    subscriberService.checkSubscriberFilterList()

    // Assert
    verify(secretsManagerService, times(1)).setSecretValue("secret1", expectedMessageAttributes)
    verify(integrationEventTopicService, times(1)).updateSubscriptionAttributes("queue1", "FilterPolicy", expectedMessageAttributes)
  }

  @Test
  fun `does not update secret if filters match filter`() {
    // Arrange
    val apiResponse: Map<String, ConfigAuthorisation> = mapOf(
      "client1" to ConfigAuthorisation(
        endpoints = listOf("/v1/persons/.*/risks/mappadetail"),
        filters = ConsumerFilters(prisons = listOf("MKI")),
      ),
    )
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
    whenever(secretsManagerService.getSecretValue("secret1")).thenReturn("{\"eventType\":[\"MAPPA_DETAIL_CHANGED\"],\"prisonId\":[\"MKI\"]}")
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
        endpoints = listOf("/v1/persons/.*/risks/mappadetail"),
        filters = ConsumerFilters(
          prisons = listOf("MKI"),
        ),
      ),
    )
    val expectedMessageAttributes = "{\"eventType\":[\"MAPPA_DETAIL_CHANGED\"],\"prisonId\":[\"MKI\"]}"
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
    whenever(secretsManagerService.getSecretValue("secret1")).thenReturn("{\"eventType\":[\"MAPPA_DETAIL_CHANGED\"],\"prisonId\":[\"ABC\"]}")

    // Act
    subscriberService.checkSubscriberFilterList()

    // Assert
    verify(secretsManagerService, times(1)).setSecretValue("secret1", expectedMessageAttributes)
    verify(integrationEventTopicService, times(1)).updateSubscriptionAttributes("queue1", "FilterPolicy", expectedMessageAttributes)
  }

  @Test
  fun `updates secret and subscription if prisonId mismatches filter and filter list is empty`() {
    // Arrange
    val apiResponse: Map<String, ConfigAuthorisation> = mapOf(
      "client1" to ConfigAuthorisation(
        endpoints = listOf("/v1/persons/.*/risks/mappadetail"),
        filters = null,
      ),
    )
    val expectedMessageAttributes = "{\"eventType\":[\"MAPPA_DETAIL_CHANGED\"]}"
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
    whenever(secretsManagerService.getSecretValue("secret1")).thenReturn("{\"eventType\":[\"MAPPA_DETAIL_CHANGED\"],\"prisonId\":[\"ABC\"]}")

    // Act
    subscriberService.checkSubscriberFilterList()

    // Assert
    verify(secretsManagerService, times(1)).setSecretValue("secret1", expectedMessageAttributes)
    verify(integrationEventTopicService, times(1)).updateSubscriptionAttributes("queue1", "FilterPolicy", expectedMessageAttributes)
  }

  @Test
  fun `should exclude prisonId from filter policy if not set`() {
    // Arrange
    val apiResponse: Map<String, ConfigAuthorisation> = mapOf(
      "client1" to ConfigAuthorisation(
        endpoints = listOf("/v1/persons/.*/risks/mappadetail"),
        filters = ConsumerFilters(
          prisons = null,
        ),
      ),
    )
    val expectedMessageAttributes = "{\"eventType\":[\"MAPPA_DETAIL_CHANGED\"]}"
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
    whenever(secretsManagerService.getSecretValue("secret1")).thenReturn("{\"eventType\":[\"DEFAULT\"]}")

    // Act
    subscriberService.checkSubscriberFilterList()

    // Assert
    verify(secretsManagerService, times(1)).setSecretValue("secret1", expectedMessageAttributes)
    verify(integrationEventTopicService, times(1)).updateSubscriptionAttributes("queue1", "FilterPolicy", expectedMessageAttributes)
  }

  @ParameterizedTest
  @CsvSource(
    "/v1/persons/.*/risks/scores, RISK_SCORE_CHANGED",
    "/v1/persons/[^/]*$, PERSON_STATUS_CHANGED",
  )
  fun `grant access to risk score events if client has access to risk score endpoint`(
    clientConsumerPath: String,
    eventType: String,
  ) {
    // Arrange
    val apiResponse: Map<String, ConfigAuthorisation> = mapOf(
      "client1" to ConfigAuthorisation(
        endpoints = listOf(clientConsumerPath),
        filters = null,
      ),
    )
    whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
    whenever(secretsManagerService.getSecretValue("secret1"))
      .thenReturn("{\"eventType\":[\"DEFAULT\"]}")

    // Act
    subscriberService.checkSubscriberFilterList()

    // Assert
    verify(secretsManagerService, times(1)).setSecretValue("secret1", "{\"eventType\":[\"$eventType\"]}")
    verify(integrationEventTopicService, times(1)).updateSubscriptionAttributes("queue1", "FilterPolicy", "{\"eventType\":[\"$eventType\"]}")
  }

  @Test
  fun `set filter list to be DEFAULT if client has no event access`() {
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
    verify(secretsManagerService, times(1)).setSecretValue("secret1", "{\"eventType\":[\"DEFAULT\"]}")
    verify(integrationEventTopicService, times(1)).updateSubscriptionAttributes("queue1", "FilterPolicy", "{\"eventType\":[\"DEFAULT\"]}")
  }

  @Nested
  @DisplayName("Given client with specific role")
  inner class GivenClientWithSpecificRole {
    private val defaultEventFilter = """{"eventType":["DEFAULT"]}"""

    @Test
    fun `grant access to events for curious`() {
      val consumer = "curious1" // with role "curious"
      val secretId = "curious1-filter"
      val queueName = "curious1-queue"
      val endpoints = listOf(
        "/v1/persons/.*/plp-induction-schedule",
        "/v1/persons/.*/plp-induction-schedule/history",
        "/v1/persons/.*/plp-review-schedule",
        "/v1/persons/[^/]+/expression-of-interest/jobs/[^/]+$",
        "/v1/hmpps/id/by-nomis-number/[^/]*$",
        "/v1/hmpps/id/nomis-number/by-hmpps-id/[^/]*$",
        "/v1/persons/.*/education/assessments/status",
        "/v1/persons/[^/]*$",
        "/v1/persons/[^/]+/prisoner-base-location",
        "/v1/persons/.*/education/assessments",
        "/v1/status",
        "/v1/persons/.*/education/san/plan-creation-schedule",
        "/v1/persons/.*/education/san/review-schedule",
        "/v1/persons/.*/education/status",
        "/v1/persons/.*/education/aln-assessment",
      )
      // current secret: PRISONER_MERGE is missing
      // {"eventType":["PERSON_EDUCATION_ASSESSMENTS_CHANGED","SAN_PLAN_CREATION_SCHEDULE_CHANGED","SAN_REVIEW_SCHEDULE_CHANGED","PLP_INDUCTION_SCHEDULE_CHANGED","PLP_REVIEW_SCHEDULE_CHANGED","PERSON_STATUS_CHANGED","PRISONER_BASE_LOCATION_CHANGED"]}
      val expectedEventTypes = setOf(
        "PERSON_EDUCATION_ASSESSMENTS_CHANGED",
        "SAN_PLAN_CREATION_SCHEDULE_CHANGED",
        "SAN_REVIEW_SCHEDULE_CHANGED",
        "PLP_INDUCTION_SCHEDULE_CHANGED",
        "PLP_REVIEW_SCHEDULE_CHANGED",
        "PERSON_STATUS_CHANGED",
        "PRISONER_BASE_LOCATION_CHANGED",
      )

      testSubscriptionFilter(endpoints, expectedEventTypes, consumer, secretId, queueName)
    }

    @Test
    fun `grant access to events for police`() {
      val consumer = "police1" // with role "police"
      val secretId = "police1-filter"
      val queueName = "police1-queue"
      val endpoints = listOf(
        "/v1/persons/[^/]*$",
        "/v1/persons/.*/addresses",
        "/v1/pnd/persons/.*/alerts",
        "/v1/persons/.*/sentences",
        "/v1/persons/.*/sentences/latest-key-dates-and-adjustments",
        "/v1/persons/.*/risks/scores",
        "/v1/persons/.*/risks/serious-harm",
        "/v1/persons/.*/risks/dynamic",
        "/v1/persons/.*/licences/conditions",
        "/v1/persons/.*/person-responsible-officer",
        "/v1/persons/.*/status-information",
        "/v1/hmpps/reference-data",
        "/v1/status",
      )
      val expectedFilter =
        """{"eventType":["PERSON_ADDRESS_CHANGED","LICENCE_CONDITION_CHANGED","PERSON_RESPONSIBLE_OFFICER_CHANGED","DYNAMIC_RISKS_CHANGED","RISK_SCORE_CHANGED","RISK_OF_SERIOUS_HARM_CHANGED","PERSON_SENTENCES_CHANGED","KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE","PROBATION_STATUS_CHANGED","PERSON_STATUS_CHANGED","PERSON_PND_ALERTS_CHANGED"]}"""

      testSubscriptionFilter(endpoints, expectedFilter, consumer, secretId, queueName)
    }

    @Test
    fun `grant access to events for mappa`() {
      val consumer = "mappa1" // with role "mappa"
      val secretId = "mappa1-filter"
      val queueName = "mappa1-queue"
      val endpoints = listOf(
        "/v1/persons",
        "/v1/persons/[^/]*$",
        "/v1/persons/.*/images",
        "/v1/images/.*",
        "/v1/persons/.*/addresses",
        "/v1/persons/.*/offences",
        "/v1/persons/.*/alerts",
        "/v1/persons/.*/sentences",
        "/v1/persons/.*/sentences/latest-key-dates-and-adjustments",
        "/v1/persons/.*/risks/scores",
        "/v1/persons/.*/needs",
        "/v1/persons/.*/risks/serious-harm",
        "/v1/persons/.*/reported-adjudications",
        "/v1/persons/.*/adjudications",
        "/v1/persons/.*/licences/conditions",
        "/v1/persons/.*/case-notes",
        "/v1/persons/.*/protected-characteristics",
        "/v1/persons/.*/risks/mappadetail",
        "/v1/persons/.*/risks/categories",
        "/v1/persons/.*/person-responsible-officer",
        "/v1/persons/.*/risk-management-plan",
        "/v1/persons/.*/contact-events",
        "/v1/persons/.*/contact-events/.*",
        "/v1/hmpps/reference-data",
        "/v1/status",
      )
      val expectedEventTypes = setOf(
        "PERSON_STATUS_CHANGED",
        "PERSON_IMAGES_CHANGED",
        "PERSON_ADDRESS_CHANGED",
        "PERSON_OFFENCES_CHANGED",
        "PERSON_ALERTS_CHANGED",
        "PERSON_SENTENCES_CHANGED",
        "KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE",
        "RISK_SCORE_CHANGED",
        "RISK_OF_SERIOUS_HARM_CHANGED",
        "PERSON_REPORTED_ADJUDICATIONS_CHANGED",
        "LICENCE_CONDITION_CHANGED",
        "PERSON_CASE_NOTES_CHANGED",
        "PERSON_PROTECTED_CHARACTERISTICS_CHANGED",
        "MAPPA_DETAIL_CHANGED",
        "PERSON_RISK_CATEGORIES_CHANGED",
        "PERSON_RESPONSIBLE_OFFICER_CHANGED",
      )

      testSubscriptionFilter(endpoints, expectedEventTypes, consumer, secretId, queueName)
    }

    @Test
    fun `grant access to events for private-prison`() {
      val consumer = "privateprison1" // with role "private-prison"
      val secretId = "privateprison1-filter"
      val queueName = "privateprison1-queue"
      val endpoints = listOf(
        "/v1/hmpps/id/by-nomis-number/[^/]*$",
        "/v1/hmpps/id/nomis-number/by-hmpps-id/[^/]*$",
        "/v1/persons/.*/addresses",
        "/v1/persons/.*/contacts[^/]*$",
        "/v1/persons/.*/iep-level",
        "/v1/persons/.*/visitor/.*/restrictions",
        "/v1/persons/.*/visit-restrictions",
        "/v1/persons/.*/visit-orders",
        "/v1/persons/.*/visit/future",
        "/v1/persons/.*/alerts",
        "/v1/persons/.*/case-notes",
        "/v1/persons/.*/name",
        "/v1/persons/.*/cell-location",
        "/v1/persons/.*/risks/categories",
        "/v1/persons/.*/sentences",
        "/v1/persons/.*/sentences/latest-key-dates-and-adjustments",
        "/v1/persons/.*/offences",
        "/v1/persons/.*/person-responsible-officer",
        "/v1/persons/.*/protected-characteristics",
        "/v1/persons/.*/reported-adjudications",
        "/v1/persons/.*/number-of-children",
        "/v1/persons/.*/physical-characteristics",
        "/v1/persons/.*/images",
        "/v1/persons/.*/images/.*",
        "/v1/prison/prisoners",
        "/v1/prison/prisoners/[^/]*$",
        "/v1/prison/.*/prisoners/[^/]*/balances$",
        "/v1/prison/.*/prisoners/.*/accounts/.*/balances",
        "/v1/prison/.*/prisoners/.*/accounts/.*/transactions",
        "/v1/prison/.*/prisoners/.*/transactions/[^/]*$",
        "/v1/prison/.*/prisoners/.*/transactions",
        "/v1/prison/.*/prisoners/.*/non-associations",
        "/v1/prison/.*/visit/search[^/]*$",
        "/v1/prison/.*/residential-hierarchy",
        "/v1/prison/.*/location/[^/]*$",
        "/v1/prison/.*/residential-details",
        "/v1/prison/.*/capacity",
        "/v1/prison/.*/prison-regime",
        "/v1/prison/.*/appointments/search",
        "/v1/activities/.*/schedules",
        "/v1/activities/schedule/[^/]*$",
        "/v1/activities/schedule/.*/suitability-criteria",
        "/v1/prison/.*/activities",
        "/v1/prison/.*/prison-pay-bands",
        "/v1/visit/[^/]*$",
        "/v1/visit",
        "/v1/visit/id/by-client-ref/[^/]*$",
        "/v1/visit/.*/cancel",
        "/v1/contacts/[^/]*$",
        "/v1/prison/.*/location/.*/deactivate",
        "/v1/persons/.*/health-and-diet",
        "/v1/persons/.*/care-needs",
        "/v1/persons/.*/languages",
        "/v1/persons/.*/education",
        "/v1/activities/schedule/attendance",
        "/v1/activities/attendance-reasons",
        "/v1/activities/schedule/.*/deallocate",
        "/v1/prison/.*/.*/scheduled-instances",
        "/v1/activities/deallocation-reasons",
        "/v1/activities/schedule/.*/allocate",
        "/v1/prison/prisoners/.*/activities/attendances",
        "/v1/activities/schedule/.*/waiting-list-applications",
        "/v1/status",
      )
      val expectedFilter =
        """{"eventType":["CONTACT_CHANGED","PERSON_ADDRESS_CHANGED","PERSON_ALERTS_CHANGED","PERSON_CARE_NEEDS_CHANGED","PERSON_CASE_NOTES_CHANGED","PERSON_CELL_LOCATION_CHANGED","PERSON_CONTACTS_CHANGED","PERSON_HEALTH_AND_DIET_CHANGED","PERSON_IEP_LEVEL_CHANGED","PERSON_IMAGES_CHANGED","PERSON_IMAGE_CHANGED","PERSON_LANGUAGES_CHANGED","PERSON_NAME_CHANGED","PERSON_NUMBER_OF_CHILDREN_CHANGED","PERSON_OFFENCES_CHANGED","PERSON_RESPONSIBLE_OFFICER_CHANGED","PERSON_PHYSICAL_CHARACTERISTICS_CHANGED","PERSON_PROTECTED_CHARACTERISTICS_CHANGED","PERSON_REPORTED_ADJUDICATIONS_CHANGED","PERSON_RISK_CATEGORIES_CHANGED","PERSON_SENTENCES_CHANGED","KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE","PERSON_VISIT_ORDERS_CHANGED","PERSON_VISIT_RESTRICTIONS_CHANGED","PERSON_FUTURE_VISITS_CHANGED","PERSON_VISITOR_RESTRICTIONS_CHANGED","PRISON_CAPACITY_CHANGED","PRISON_LOCATION_CHANGED","PRISONER_ACCOUNT_BALANCES_CHANGED","PRISONER_ACCOUNT_TRANSACTIONS_CHANGED","PRISONER_NON_ASSOCIATIONS_CHANGED","PRISONER_BALANCES_CHANGED","PRISON_RESIDENTIAL_DETAILS_CHANGED","PRISON_RESIDENTIAL_HIERARCHY_CHANGED","PRISON_VISITS_CHANGED","PRISONERS_CHANGED","PRISONER_CHANGED","VISIT_CHANGED","VISIT_FROM_EXTERNAL_SYSTEM_CREATED"],"prisonId":["MKI"]}"""

      testSubscriptionFilter(endpoints, expectedFilter, consumer, secretId, queueName)
    }

    private fun testSubscriptionFilter(
      endpoints: List<String> = emptyList(),
      expectedFilter: String = defaultEventFilter,
      consumer: String = "client1",
      secretId: String = "secret1",
      queueName: String = "queue1",
    ) = testSubscriptionFilter(
      endpoints,
      expectedEventTypes = extractEventTypesFrom(expectedFilter),
      consumer,
      secretId,
      queueName,
    )

    private fun testSubscriptionFilter(
      endpoints: List<String> = emptyList(),
      expectedEventTypes: Set<String> = emptySet(),
      consumer: String = "client1",
      secretId: String = "secret1",
      queueName: String = "queue1",
    ) {
      // Arrange
      val apiResponse: Map<String, ConfigAuthorisation> = mapOf(
        consumer to ConfigAuthorisation(endpoints, filters = null),
      )
      whenever(integrationApiGateway.getApiAuthorizationConfig()).thenReturn(apiResponse)
      whenever(secretsManagerService.getSecretValue(secretId)).thenReturn(defaultEventFilter)

      // Act
      subscriberService.checkSubscriberFilterList()

      // Assert
      val assertFilterString: (KArgumentCaptor<String>, Set<String>) -> Unit = { filterCaptor, expectedEventTypes ->
        extractEventTypesFrom(filterCaptor.singleValue).also { assertThat(it).hasSameElementsAs(expectedEventTypes) }
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
  }

  // filter is in this format: {"eventType":["eventType1", "eventType2, ...]}
  private fun extractEventTypesFrom(filter: String) = objectMapper.readTree(filter)["eventType"].map { it.textValue() }.toSet()
}
