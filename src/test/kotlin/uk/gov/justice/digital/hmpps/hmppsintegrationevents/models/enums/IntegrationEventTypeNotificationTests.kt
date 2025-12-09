package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.registration.AdditionalInformation

private const val CLASS_NAME = "uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventTypeNotificationTests"

class IntegrationEventTypeNotificationTests {
  companion object {
    private val prisonId = "MDI"
    private val baseUrl = ""
    private val crn = "X777776"
    private val nomsNumber = "A1234AA"
    private val removedNomsNumber = "B5678BB"
    private val contactPersonId = "1234567"
    private val locationKey = "$prisonId-001-01"
    private val visitReference = "ab-cd-ef-gh"

    /**
     * Event Type code, all test fixtures
     */
    private val eventTypeTestFixtures by lazy { activeEventTypeTestFixtures + notYetSupportedEventTypeTestFixtures + deprecatedEventTypeTestFixtures }
    private val activeEventTypeTestFixtures by lazy {
      listOf(
        testFixture("CONTACT_CHANGED", "$baseUrl/v1/contacts/$contactPersonId", AdditionalInformation(contactPersonId = contactPersonId)),
        testFixture("DYNAMIC_RISKS_CHANGED", crn, "$baseUrl/v1/persons/$crn/risks/dynamic"),
        testFixture("KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE", nomsNumber, "$baseUrl/v1/persons/$nomsNumber/sentences/latest-key-dates-and-adjustments"),
        testFixture("LICENCE_CONDITION_CHANGED", crn, "$baseUrl/v1/persons/$crn/licences/conditions"),
        testFixture("MAPPA_DETAIL_CHANGED", crn, "$baseUrl/v1/persons/$crn/risks/mappadetail"),
        testFixture("PERSON_ADDRESS_CHANGED", crn, "$baseUrl/v1/persons/$crn/addresses"),
        testFixture("PERSON_ALERTS_CHANGED", crn, "$baseUrl/v1/persons/$crn/alerts"),
        testFixture("PERSON_CARE_NEEDS_CHANGED", crn, "$baseUrl/v1/persons/$crn/care-needs"),
        testFixture("PERSON_CASE_NOTES_CHANGED", crn, "$baseUrl/v1/persons/$crn/case-notes"),
        testFixture("PERSON_CELL_LOCATION_CHANGED", nomsNumber, "$baseUrl/v1/persons/$nomsNumber/cell-location"),
        testFixture("PERSON_CONTACTS_CHANGED", crn, "$baseUrl/v1/persons/$crn/contacts"),
        testFixture("PERSON_EDUCATION_ASSESSMENTS_CHANGED", nomsNumber, "$baseUrl/v1/persons/$nomsNumber/education/assessments"),
        testFixture("PERSON_FUTURE_VISITS_CHANGED", crn, "$baseUrl/v1/persons/$crn/visit/future"),
        testFixture("PERSON_HEALTH_AND_DIET_CHANGED", crn, "$baseUrl/v1/persons/$crn/health-and-diet"),
        testFixture("PERSON_IEP_LEVEL_CHANGED", crn, "$baseUrl/v1/persons/$crn/iep-level"),
        testFixture("PERSON_IMAGES_CHANGED", crn, "$baseUrl/v1/persons/$crn/images"),
        testFixture("PERSON_LANGUAGES_CHANGED", crn, "$baseUrl/v1/persons/$crn/languages"),
        testFixture("PERSON_NAME_CHANGED", crn, "$baseUrl/v1/persons/$crn/name"),
        testFixture("PERSON_NUMBER_OF_CHILDREN_CHANGED", crn, "$baseUrl/v1/persons/$crn/number-of-children"),
        testFixture("PERSON_OFFENCES_CHANGED", crn, "$baseUrl/v1/persons/$crn/offences"),
        testFixture("PERSON_PHYSICAL_CHARACTERISTICS_CHANGED", crn, "$baseUrl/v1/persons/$crn/physical-characteristics"),
        testFixture("PERSON_PND_ALERTS_CHANGED", crn, "$baseUrl/v1/pnd/persons/$crn/alerts"),
        testFixture("PERSON_PROTECTED_CHARACTERISTICS_CHANGED", crn, "$baseUrl/v1/persons/$crn/protected-characteristics"),
        testFixture("PERSON_REPORTED_ADJUDICATIONS_CHANGED", crn, "$baseUrl/v1/persons/$crn/reported-adjudications"),
        testFixture("PERSON_RESPONSIBLE_OFFICER_CHANGED", crn, "$baseUrl/v1/persons/$crn/person-responsible-officer"),
        testFixture("PERSON_RISK_CATEGORIES_CHANGED", crn, "$baseUrl/v1/persons/$crn/risks/categories"),
        testFixture("PERSON_SENTENCES_CHANGED", crn, "$baseUrl/v1/persons/$crn/sentences"),
        testFixture("PERSON_STATUS_CHANGED", crn, "$baseUrl/v1/persons/$crn"),
        testFixture("PERSON_STATUS_CHANGED", nomsNumber, "$baseUrl/v1/persons/$nomsNumber"),
        testFixture("PERSON_VISITOR_RESTRICTIONS_CHANGED", crn, "$baseUrl/v1/persons/$crn/visitor/$contactPersonId/restrictions", AdditionalInformation(contactPersonId = contactPersonId)),
        testFixture("PERSON_VISIT_ORDERS_CHANGED", crn, "$baseUrl/v1/persons/$crn/visit-orders"),
        testFixture("PERSON_VISIT_RESTRICTIONS_CHANGED", crn, "$baseUrl/v1/persons/$crn/visit-restrictions"),
        testFixture("PLP_INDUCTION_SCHEDULE_CHANGED", nomsNumber, "$baseUrl/v1/persons/$nomsNumber/plp-induction-schedule/history"),
        testFixture("PLP_REVIEW_SCHEDULE_CHANGED", nomsNumber, "$baseUrl/v1/persons/$nomsNumber/plp-review-schedule"),
        testFixture("PRISONERS_CHANGED", "$baseUrl/v1/prison/prisoners"),
        testFixture("PRISONER_BALANCES_CHANGED", nomsNumber, "$baseUrl/v1/prison/$prisonId/prisoners/$nomsNumber/balances"),
        testFixture("PRISONER_BASE_LOCATION_CHANGED", nomsNumber, "$baseUrl/v1/persons/$nomsNumber/prisoner-base-location"),
        testFixture("PRISONER_CHANGED", nomsNumber, "$baseUrl/v1/prison/prisoners/$nomsNumber"),
        TestFixture("PRISONER_MERGED", nomsNumber, removedNomsNumber, "$baseUrl/v1/persons/$removedNomsNumber", AdditionalInformation(removedNomsNumber = removedNomsNumber)),
        testFixture("PRISONER_NON_ASSOCIATIONS_CHANGED", nomsNumber, "$baseUrl/v1/prison/$prisonId/prisoners/$nomsNumber/non-associations"),
        testFixture("PRISON_CAPACITY_CHANGED", "$baseUrl/v1/prison/$prisonId/capacity"),
        testFixture("PRISON_LOCATION_CHANGED", nomsNumber, "$baseUrl/v1/prison/$prisonId/location/$locationKey", AdditionalInformation(key = locationKey)),
        testFixture("PRISON_RESIDENTIAL_DETAILS_CHANGED", "$baseUrl/v1/prison/$prisonId/residential-details"),
        testFixture("PRISON_RESIDENTIAL_HIERARCHY_CHANGED", "$baseUrl/v1/prison/$prisonId/residential-hierarchy"),
        testFixture("PRISON_VISITS_CHANGED", "$baseUrl/v1/prison/$prisonId/visit/search"),
        testFixture("PROBATION_STATUS_CHANGED", crn, "$baseUrl/v1/persons/$crn/status-information"),
        testFixture("RISK_OF_SERIOUS_HARM_CHANGED", crn, "$baseUrl/v1/persons/$crn/risks/serious-harm"),
        testFixture("RISK_SCORE_CHANGED", crn, "$baseUrl/v1/persons/$crn/risks/scores"),
        testFixture("SAN_PLAN_CREATION_SCHEDULE_CHANGED", nomsNumber, "$baseUrl/v1/persons/$nomsNumber/education/san/plan-creation-schedule"),
        testFixture("SAN_REVIEW_SCHEDULE_CHANGED", nomsNumber, "$baseUrl/v1/persons/$nomsNumber/education/san/review-schedule"),
        testFixture("VISIT_CHANGED", "$baseUrl/v1/visit/$visitReference", AdditionalInformation(reference = visitReference)),
      )
    }

    /**
     * Event type codes NOT yet supported:
     *  - {imageId}
     *  - {accountCode}
     *  - {clientVisitReference}
     *  @see IntegrationEventType.path
     */
    private val notYetSupportedEventTypeTestFixtures by lazy {
      listOf(
        // {imageId} is NOT supported yet! Event type not in use
        testFixture("PERSON_IMAGE_CHANGED", crn, "$baseUrl/v1/persons/$crn/images/{imageId}"),
        // {accountCode} is NOT supported yet! Event type not in use
        testFixture("PRISONER_ACCOUNT_BALANCES_CHANGED", nomsNumber, "$baseUrl/v1/prison/$prisonId/prisoners/$nomsNumber/accounts/{accountCode}/balances"),
        // {accountCode} is NOT supported yet! Event type not in use
        testFixture("PRISONER_ACCOUNT_TRANSACTIONS_CHANGED", nomsNumber, "$baseUrl/v1/prison/$prisonId/prisoners/$nomsNumber/accounts/{accountCode}/transactions"),
        // {clientVisitReference} is NOT supported! Event type not in use
        testFixture("VISIT_FROM_EXTERNAL_SYSTEM_CREATED", "$baseUrl/v1/visit/id/by-client-ref/{clientVisitReference}"),
      )
    }

    /**
     * Event type codes deprecated/to be removed
     */
    private val deprecatedEventTypeTestFixtures by lazy {
      listOf(
        TestFixture("PRISONER_MERGE", nomsNumber, removedNomsNumber, "$baseUrl/v1/persons/$removedNomsNumber", AdditionalInformation(removedNomsNumber = removedNomsNumber)),
      )
    }

    private fun testFixture(eventTypeCode: String, expectedUrl: String, additionalInformation: AdditionalInformation? = null) = TestFixture(eventTypeCode, expectedUrl = expectedUrl, additionalInformation = additionalInformation)
    private fun testFixture(eventTypeCode: String, hmppsId: String, expectedUrl: String) = TestFixture(eventTypeCode, hmppsId, hmppsId, expectedUrl)
    private fun testFixture(eventTypeCode: String, hmppsId: String, expectedUrl: String, additionalInformation: AdditionalInformation) = TestFixture(eventTypeCode, hmppsId, hmppsId, expectedUrl, additionalInformation)

    @JvmStatic
    private fun eventTypeCodesTestSource() = eventTypeTestFixtures.map {
      Arguments.of(
        it.eventTypeCode,
        it.hmppsId,
        it.expectedHmppsId,
        it.expectedUrl,
        it.additionalInformation,
      )
    }
  }

  @Nested
  @DisplayName("Given notification associated with event type")
  inner class GivenNotificationAssociatedOfEventType {
    @Test
    fun `should get notification of PERSON_STATUS_CHANGED`() {
      val eventType = IntegrationEventType.PERSON_STATUS_CHANGED
      val baseUrl = ""
      val hmppsId = "A1234AA"
      val expectedUrl = "$baseUrl/v1/persons/$hmppsId"

      val actualNotification = eventType.getNotification(
        baseUrl = baseUrl,
        hmppsId = hmppsId,
        prisonId = null,
        additionalInformation = null,
      )

      assertThat(actualNotification.hmppsId).isEqualTo(hmppsId)
      assertThat(actualNotification.prisonId).isNull()
      assertThat(actualNotification.eventType).isEqualTo(eventType)
      assertThat(actualNotification.url).isEqualTo(expectedUrl)
    }

    @Test
    fun `should get notification of PRISONER_MERGED`() {
      val eventType = IntegrationEventType.PRISONER_MERGED
      val baseUrl = ""
      val nomsNumber = "A1234AA"
      val removedNomsNumber = "B5678BB"
      val expectedUrl = "$baseUrl/v1/persons/$removedNomsNumber"
      val hmppsId = removedNomsNumber

      val actualNotification = eventType.getNotification(
        baseUrl = baseUrl,
        hmppsId = hmppsId,
        prisonId = null,
        additionalInformation = AdditionalInformation(nomsNumber = nomsNumber, removedNomsNumber = removedNomsNumber),
      )

      assertThat(actualNotification.hmppsId).isEqualTo(hmppsId)
      assertThat(actualNotification.prisonId).isNull()
      assertThat(actualNotification.eventType).isEqualTo(eventType)
      assertThat(actualNotification.url).isEqualTo(expectedUrl)
    }

    @ParameterizedTest
    @MethodSource("$CLASS_NAME#eventTypeCodesTestSource")
    fun `should get notification of the event type`(
      eventTypeCode: String,
      hmppsId: String?,
      expectedHmppsId: String?,
      expectedUrl: String,
      additionalInformation: AdditionalInformation?,
    ) {
      val eventType = IntegrationEventType.valueOf(eventTypeCode)
      val notification = eventType.getNotification(
        baseUrl = baseUrl,
        hmppsId = hmppsId,
        prisonId = prisonId,
        additionalInformation = additionalInformation,
      )

      assertThat(notification.eventType).isEqualTo(eventType)
      assertThat(notification.prisonId).isEqualTo(prisonId)
      assertThat(notification.hmppsId).isEqualTo(expectedHmppsId)
      assertThat(notification.url).isEqualTo(expectedUrl)
    }
  }

  @Nested
  @DisplayName("Given all event type codes")
  inner class GivenAllEventTypeCodes {
    private val expectedEventTypes = eventTypeTestFixtures.map { it.eventTypeCode }.sorted().toSet()
    private val allEventTypeCodes = IntegrationEventType.entries.map { it.name }.sorted().toSet()

    /**
     * All active/current event type codes in enum `IntegrationEventType` shall be tested
     *
     * It may be unsafe to
     *   i) Rename current type code(s), e.g. PRISONER_MERGE to `PRISONER_MERGED`
     *   ii) Delete current type code(s), e.g. retiring unused type code `PERSON_IMAGE_CHANGED`
     * Please consult the dev team for any need/demand
     *
     * It is safe to:
     * - Add a new event type code
     */
    @Test
    fun `should have not deleted or renamed current event type codes in use`() {
      // No unexpected deletion or renaming
      assertThat(allEventTypeCodes).containsAll(expectedEventTypes)
    }

    /**
     * All active/current event type codes in enum `IntegrationEventType` shall be tested
     */
    @Test
    fun `should have tested all event type codes`() {
      // All active event type codes in enum `IntegrationEventType` shall be tested
      assertThat(allEventTypeCodes).isEqualTo(expectedEventTypes)
    }
  }
}

private data class TestFixture(
  val eventTypeCode: String,
  val hmppsId: String? = null,
  val expectedHmppsId: String? = null,
  val expectedUrl: String,
  val additionalInformation: AdditionalInformation? = null,
)
