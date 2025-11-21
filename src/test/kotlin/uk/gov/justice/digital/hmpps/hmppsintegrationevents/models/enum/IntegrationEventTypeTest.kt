package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enum

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType

class IntegrationEventTypeTest {
  companion object {
    /**
     * Testing URL patterns, matchable to External event type.
     * Multiple URL patterns may match to same event type, e.g. `PRISONER_BASE_LOCATION_CHANGED`;
     */
    private val urlToMostEventTypeMap by lazy {
      listOf(
        "/v1/contacts/[^/]*$" to IntegrationEventType.CONTACT_CHANGED,
        "/v1/persons/.*/risks/dynamic" to IntegrationEventType.DYNAMIC_RISKS_CHANGED,
        "/v1/persons/.*/sentences/latest-key-dates-and-adjustments" to IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE,
        "/v1/persons/.*/licences/conditions" to IntegrationEventType.LICENCE_CONDITION_CHANGED,
        "/v1/persons/.*/risks/mappadetail" to IntegrationEventType.MAPPA_DETAIL_CHANGED,
        "/v1/persons/.*/addresses" to IntegrationEventType.PERSON_ADDRESS_CHANGED,
        "/v1/persons/.*/alerts" to IntegrationEventType.PERSON_ALERTS_CHANGED,
        "/v1/persons/.*/care-needs" to IntegrationEventType.PERSON_CARE_NEEDS_CHANGED,
        "/v1/persons/.*/case-notes" to IntegrationEventType.PERSON_CASE_NOTES_CHANGED,
        "/v1/persons/.*/cell-location" to IntegrationEventType.PERSON_CELL_LOCATION_CHANGED,
        "/v1/persons/.*/contacts[^/]*$" to IntegrationEventType.PERSON_CONTACTS_CHANGED,
        "/v1/persons/.*/education/assessments" to IntegrationEventType.PERSON_EDUCATION_ASSESSMENTS_CHANGED,
        "/v1/persons/.*/visit/future" to IntegrationEventType.PERSON_FUTURE_VISITS_CHANGED,
        "/v1/persons/.*/health-and-diet" to IntegrationEventType.PERSON_HEALTH_AND_DIET_CHANGED,
        "/v1/persons/.*/iep-level" to IntegrationEventType.PERSON_IEP_LEVEL_CHANGED,
        "/v1/persons/.*/images" to IntegrationEventType.PERSON_IMAGES_CHANGED,
        "/v1/persons/.*/images/.*" to IntegrationEventType.PERSON_IMAGE_CHANGED,
        "/v1/persons/.*/languages" to IntegrationEventType.PERSON_LANGUAGES_CHANGED,
        "/v1/persons/.*/name" to IntegrationEventType.PERSON_NAME_CHANGED,
        "/v1/persons/.*/number-of-children" to IntegrationEventType.PERSON_NUMBER_OF_CHILDREN_CHANGED,
        "/v1/persons/.*/offences" to IntegrationEventType.PERSON_OFFENCES_CHANGED,
        "/v1/persons/.*/physical-characteristics" to IntegrationEventType.PERSON_PHYSICAL_CHARACTERISTICS_CHANGED,
        "/v1/pnd/persons/.*/alerts" to IntegrationEventType.PERSON_PND_ALERTS_CHANGED,
        "/v1/persons/.*/protected-characteristics" to IntegrationEventType.PERSON_PROTECTED_CHARACTERISTICS_CHANGED,
        "/v1/persons/.*/reported-adjudications" to IntegrationEventType.PERSON_REPORTED_ADJUDICATIONS_CHANGED,
        "/v1/persons/.*/person-responsible-officer" to IntegrationEventType.PERSON_RESPONSIBLE_OFFICER_CHANGED,
        "/v1/persons/.*/risks/categories" to IntegrationEventType.PERSON_RISK_CATEGORIES_CHANGED,
        "/v1/persons/.*/sentences" to IntegrationEventType.PERSON_SENTENCES_CHANGED,
        "/v1/persons/[^/]*$" to IntegrationEventType.PERSON_STATUS_CHANGED,
        "/v1/persons/[^/]*$" to IntegrationEventType.PRISONER_MERGE,
        "/v1/persons/.*/visitor/.*/restrictions" to IntegrationEventType.PERSON_VISITOR_RESTRICTIONS_CHANGED,
        "/v1/persons/.*/visit-orders" to IntegrationEventType.PERSON_VISIT_ORDERS_CHANGED,
        "/v1/persons/.*/visit-restrictions" to IntegrationEventType.PERSON_VISIT_RESTRICTIONS_CHANGED,
        "/v1/persons/.*/plp-induction-schedule/history" to IntegrationEventType.PLP_INDUCTION_SCHEDULE_CHANGED,
        "/v1/persons/.*/plp-review-schedule" to IntegrationEventType.PLP_REVIEW_SCHEDULE_CHANGED,
        "/v1/prison/prisoners" to IntegrationEventType.PRISONERS_CHANGED,
        "/v1/prison/prisoners/[^/]*$" to IntegrationEventType.PRISONER_CHANGED,
        "/v1/prison/.*/prisoners/.*/accounts/.*/balances" to IntegrationEventType.PRISONER_ACCOUNT_BALANCES_CHANGED,
        "/v1/prison/.*/prisoners/.*/accounts/.*/transactions" to IntegrationEventType.PRISONER_ACCOUNT_TRANSACTIONS_CHANGED,
        "/v1/prison/.*/prisoners/[^/]*/balances$" to IntegrationEventType.PRISONER_BALANCES_CHANGED,
        "/v1/persons/.*/prisoner-base-location" to IntegrationEventType.PRISONER_BASE_LOCATION_CHANGED,
        "/v1/persons/[^/]+/prisoner-base-location" to IntegrationEventType.PRISONER_BASE_LOCATION_CHANGED,
        "/v1/prison/.*/prisoners/.*/non-associations" to IntegrationEventType.PRISONER_NON_ASSOCIATIONS_CHANGED,
        "/v1/prison/.*/capacity" to IntegrationEventType.PRISON_CAPACITY_CHANGED,
        "/v1/prison/.*/location/[^/]*$" to IntegrationEventType.PRISON_LOCATION_CHANGED,
        "/v1/prison/.*/residential-details" to IntegrationEventType.PRISON_RESIDENTIAL_DETAILS_CHANGED,
        "/v1/prison/.*/residential-hierarchy" to IntegrationEventType.PRISON_RESIDENTIAL_HIERARCHY_CHANGED,
        "/v1/prison/.*/visit/search[^/]*$" to IntegrationEventType.PRISON_VISITS_CHANGED,
        "/v1/persons/.*/status-information" to IntegrationEventType.PROBATION_STATUS_CHANGED,
        "/v1/persons/.*/risks/serious-harm" to IntegrationEventType.RISK_OF_SERIOUS_HARM_CHANGED,
        "/v1/persons/.*/risks/scores" to IntegrationEventType.RISK_SCORE_CHANGED,
        "/v1/persons/.*/education/san/plan-creation-schedule" to IntegrationEventType.SAN_PLAN_CREATION_SCHEDULE_CHANGED,
        "/v1/persons/.*/education/san/review-schedule" to IntegrationEventType.SAN_REVIEW_SCHEDULE_CHANGED,
        "/v1/visit/[^/]*$" to IntegrationEventType.VISIT_CHANGED,
        "/v1/visit/id/by-client-ref/[^/]*$" to IntegrationEventType.VISIT_FROM_EXTERNAL_SYSTEM_CREATED,
      ).groupBy { it.first }.mapValues { it.value.map { it.second } }
    }

    private val urlToOneEventTypeMap by lazy {
      mapOf(
        "/v1/persons/.*/images" to IntegrationEventType.PERSON_IMAGES_CHANGED,
        "/v1/persons/.*/images/.*" to IntegrationEventType.PERSON_IMAGE_CHANGED,
        "/v1/persons/.*/plp-induction-schedule/history" to IntegrationEventType.PLP_INDUCTION_SCHEDULE_CHANGED,
      )
    }

    private val urlMappingToNoEvent by lazy {
      listOf(
        "/v1/persons/.*/plp-induction-schedule",
        "/v1/hmpps/reference-data",
        "/v1/status",
      )
    }

    @JvmStatic
    private fun matchUrlMostEventsTestSource() = urlToMostEventTypeMap.toPairs()

    @JvmStatic
    private fun matchUrlToOneEventTestSource() = urlToOneEventTypeMap.toPairs()

    @JvmStatic
    private fun matchUrlToNoEventTestSource() = urlMappingToNoEvent
  }

  @Test
  @Disabled("Enable to verify any untested new event type(s)")
  fun `should have all event types in test class`() {
    val expectedEvents = urlToMostEventTypeMap.values.flatten().toSet()
    val allEvents = IntegrationEventType.entries.sortedBy { it.name }.toSet()

    assertThat(allEvents).hasSameElementsAs(expectedEvents)
  }

  @Test
  fun `should match URL to one Event Type`() = assertMatchesUrlToEvent(
    urlPattern = "/v1/persons/.*/name",
    expectedEvent = IntegrationEventType.PERSON_NAME_CHANGED,
  )

  @Test
  fun `should match URL to two Event Types, when applicable`() = assertMatchesUrlToEvents(
    urlPattern = "/v1/persons/[^/]*$",
    expectedEvents = setOf(
      IntegrationEventType.PERSON_STATUS_CHANGED,
      IntegrationEventType.PRISONER_MERGE,
    ),
  )

  @Test
  fun `should match URL to nothing, when the URL does not associate to any event`() = assertMatchesUrlToNoEvent(
    urlPattern = "/v1/hmpps/id/by-nomis-number/[^/]*$",
  )

  @Test
  fun `should match wildcard URL to ALL event types`() = assertMatchesUrlToEvents(
    urlPattern = "/.*",
    expectedEvents = IntegrationEventType.entries.toSet(),
  )

  @ParameterizedTest
  @ValueSource(
    strings = [
      "/v1/persons/[^/]+/prisoner-base-location",
      "/v1/persons/.*/prisoner-base-location",
    ],
  )
  fun `should match multiple URLs to same Event Type`(urlPattern: String) = assertMatchesUrlToEvent(
    urlPattern = urlPattern,
    expectedEvent = IntegrationEventType.PRISONER_BASE_LOCATION_CHANGED,
  )

  @ParameterizedTest
  @MethodSource("matchUrlToOneEventTestSource")
  fun `should match URL to one Event Type, when URL is associated with one event type`(urlToEventType: Pair<String, IntegrationEventType>) = assertMatchesUrlToEvent(urlToEventType)

  @ParameterizedTest
  @MethodSource("matchUrlToNoEventTestSource")
  fun `should match URL to nothing, when URL is not associated with any event type`(urlPattern: String) = assertMatchesUrlToNoEvent(urlPattern)

  @ParameterizedTest
  @MethodSource("matchUrlMostEventsTestSource")
  fun `should match URL to these events`(urlToEventType: Pair<String, List<IntegrationEventType>>) = assertMatchesUrlToEvents(urlToEventType)

  // matches URL pattern to list of event types
  private fun assertMatchesUrlToEvents(urlToEventType: Pair<String, List<IntegrationEventType>>) = assertMatchesUrlToEvents(
    urlPattern = urlToEventType.first,
    expectedEvents = urlToEventType.second.toSet(),
  )

  // matches URL pattern to one event type
  private fun assertMatchesUrlToEvent(urlToEventType: Pair<String, IntegrationEventType>) = assertMatchesUrlToEvents(
    urlPattern = urlToEventType.first,
    expectedEvents = setOf(urlToEventType.second),
  )

  // matches URL pattern to one event type
  private fun assertMatchesUrlToEvent(
    urlPattern: String,
    expectedEvent: IntegrationEventType,
  ) = assertMatchesUrlToEvents(urlPattern, setOf(expectedEvent))

  // matches URL pattern to set of event types
  private fun assertMatchesUrlToEvents(
    urlPattern: String,
    expectedEvents: Set<IntegrationEventType>,
  ) {
    val actualEvents = IntegrationEventType.matchesUrlToEvents(urlPattern).toSet()
    assertThat(actualEvents).hasSameElementsAs(expectedEvents)
  }

  // matches URL pattern to no event type
  private fun assertMatchesUrlToNoEvent(urlPattern: String) {
    val actualEvents = IntegrationEventType.matchesUrlToEvents(urlPattern)
    assertThat(actualEvents).isEmpty()
  }
}

private fun <K, V> Map<K, V>.toPairs() = this.map { it.toPair() }
private fun <K, V> Map.Entry<K, V>.toPair() = Pair(key, value)
