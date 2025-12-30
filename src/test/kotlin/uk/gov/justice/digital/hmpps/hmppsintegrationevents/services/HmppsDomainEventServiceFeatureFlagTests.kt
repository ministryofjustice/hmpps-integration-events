package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.FeatureFlagConfig
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEventName
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Tests of [IntegrationEventTypeFilter]
 *
 * - IntegrationEventTypes with no feature flag associated are enabled. e.g. `PERSON_STATUS_CHANGED`
 * - IntegrationEventTypes associated with a feature flag set to “true” are enabled, e.g. `PRISONER_MERGED`
 * - IntegrationEventTypes associated with a feature flag set to “false” are not enabled, e.g. `PERSON_LANGUAGES_CHANGED`
 * - IntegrationEventTypes that reference a feature flag that does not exist are disabled, e.g. `PRISONER_BASE_LOCATION_CHANGED`
 *      * and an error is logged with the name of the event and the name of the flag
 */
class IntegrationEventTypeFilterTest {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  private val filterLog = spyk<Logger>()
  private val featureFlagTestConfig = FeatureFlagTestConfig()
  private val integrationEventTypeFilter = IntegrationEventTypeFilter(featureFlagTestConfig.featureFlagConfig, filterLog)

  private val currentTime: LocalDateTime = LocalDateTime.now()
  private val zonedCurrentDateTime = currentTime.atZone(ZoneId.systemDefault())
  private val sqsNotificationHelper by lazy { SqsNotificationGeneratingHelper(zonedCurrentDateTime) }

  @BeforeEach
  internal fun setup() {
   /*
   `person-languages-changed-notifications-enabled`: false
   `prisoner-merge-notifications-enabled`: true
   `prisoner-base-location-changed-notifications-enabled` is undefined (not set)
    */
    mapOf(
      FeatureFlagConfig.PERSON_LANGUAGES_CHANGED_NOTIFICATIONS_ENABLED to false,
      FeatureFlagConfig.PRISONER_MERGED_NOTIFICATIONS_ENABLED to true,
    ).forEach { featureFlagTestConfig.assumeFeatureFlag(it.key, it.value) }
  }

  @AfterEach
  internal fun tearDown() {
    featureFlagTestConfig.resetAllFlags()
  }

  @Test
  fun `should keep integration event types, without feature flag associated`() = filterEventTypeShouldContainExactEventTypes(
    hmppsEvent = sqsNotificationHelper.createHmppsDomainEvent(eventType = HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.CREATED),
    IntegrationEventType.PERSON_STATUS_CHANGED,
    IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE,
    IntegrationEventType.PERSON_STATUS_CHANGED,
    IntegrationEventType.PERSON_ADDRESS_CHANGED,
    IntegrationEventType.PERSON_CONTACTS_CHANGED,
    IntegrationEventType.PERSON_IEP_LEVEL_CHANGED,
    IntegrationEventType.PERSON_VISIT_RESTRICTIONS_CHANGED,
    IntegrationEventType.PERSON_ALERTS_CHANGED,
    IntegrationEventType.PERSON_PND_ALERTS_CHANGED,
    IntegrationEventType.PERSON_CASE_NOTES_CHANGED,
    IntegrationEventType.PERSON_NAME_CHANGED,
    IntegrationEventType.PERSON_CELL_LOCATION_CHANGED,
    IntegrationEventType.PERSON_SENTENCES_CHANGED,
    IntegrationEventType.PERSON_RESPONSIBLE_OFFICER_CHANGED,
    IntegrationEventType.PERSON_PROTECTED_CHARACTERISTICS_CHANGED,
    IntegrationEventType.PERSON_REPORTED_ADJUDICATIONS_CHANGED,
    IntegrationEventType.PERSON_NUMBER_OF_CHILDREN_CHANGED,
    IntegrationEventType.PERSON_PHYSICAL_CHARACTERISTICS_CHANGED,
    IntegrationEventType.PERSON_IMAGES_CHANGED,
    IntegrationEventType.PRISONERS_CHANGED,
    IntegrationEventType.PRISONER_CHANGED,
    IntegrationEventType.PRISONER_NON_ASSOCIATIONS_CHANGED,
    IntegrationEventType.PERSON_HEALTH_AND_DIET_CHANGED,
    IntegrationEventType.PERSON_CARE_NEEDS_CHANGED,
  )

  @Test
  fun `should keep integration event type, with feature flag associated and set to true`() = filterEventTypeShouldContainEventTypes(
    hmppsEvent = createHmppsDomainEvent(HmppsDomainEventName.PrisonOffenderEvents.Prisoner.MERGED),
    IntegrationEventType.PRISONER_MERGED,
  )

  @Test
  fun `should discard integration event type, with feature flag associated and set to false`() = filterEventTypeShouldNotContainEventTypes(
    hmppsEvent = createHmppsDomainEvent(HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.CREATED),
    IntegrationEventType.PERSON_LANGUAGES_CHANGED,
  )

  @Test
  fun `should discard integration event type and log an error, with feature flag associated and not set (undefined)`() {
    // Arrange
    val unexpectedEventType = IntegrationEventType.PRISONER_BASE_LOCATION_CHANGED
    val hmppsEvent = sqsNotificationHelper.createHmppsPrisonerReceivedDomainEvent()

    // Act, Assert
    filterEventTypeShouldNotContainEventTypes(hmppsEvent, unexpectedEventType)

    // Assert (verify error logging of missing feature flag)
    verify(exactly = 1) {
      filterLog.error(
        match<String> { it.startsWith("Missing feature flag") },
        eq(unexpectedEventType.featureFlag!!),
        eq(unexpectedEventType.name),
      )
    }
  }

  private fun filterEventType(hmppsEvent: HmppsDomainEvent) = integrationEventTypeFilter.filterEventTypes(hmppsEvent)
    .also { log.debug("hmppsEvent: {}, actualEventTypes: {}", hmppsEvent, it) }.toSet()

  private fun filterEventTypeShouldContainEventTypes(hmppsEvent: HmppsDomainEvent, vararg expectedEventTypes: IntegrationEventType) {
    val expectedEventTypeNames = expectedEventTypes.map { it.name }
    val actualEventTypes = filterEventType(hmppsEvent).toSet()

    actualEventTypes shouldContainAll expectedEventTypes.toSet()
    // No expected event type discarded, no logging
    verifyLoggingOfDiscardedEvents(exactly = 0, eventTypeNames = expectedEventTypeNames)
  }

  private fun filterEventTypeShouldContainExactEventTypes(hmppsEvent: HmppsDomainEvent, vararg expectedEventTypes: IntegrationEventType) {
    val actualEventTypes = filterEventType(hmppsEvent).toSet()
    actualEventTypes shouldContainExactly expectedEventTypes.toSet()
  }

  private fun filterEventTypeShouldNotContainEventTypes(hmppsEvent: HmppsDomainEvent, vararg unexpectedEventTypes: IntegrationEventType) {
    val unexpectedEventTypeNames = unexpectedEventTypes.map { it.name }.toSet()
    val actualEventTypes = filterEventType(hmppsEvent).toSet()

    actualEventTypes shouldNotContain unexpectedEventTypes
    // Event type(s) has/have been discarded with logging
    verifyLoggingOfDiscardedEvents(exactly = 1, eventTypeNames = unexpectedEventTypeNames)
  }

  private fun createHmppsDomainEvent(eventType: String) = sqsNotificationHelper.createHmppsDomainEvent(eventType)

  /**
   * Verify logging (info) of discarded events
   *
   * @param exactly verifies logging happened exactly `exactly` times.
   * @param eventTypeNames names of event type to verify with
   */
  private fun verifyLoggingOfDiscardedEvents(exactly: Int, eventTypeNames: Collection<String>) {
    verify(exactly = exactly) {
      filterLog.info(
        match<String> { it.startsWith("These event type(s) have been discarded") },
        match<List<String>> { it.toSet().containsAll(eventTypeNames) },
      )
    }
  }
}

/**
 * Tests of [HmppsDomainEventService] with feature flags [FeatureFlagTestConfig]
 *
 * to test around:
 * - IntegrationEventTypes can have a feature flag name associated with them
 */
class HmppsDomainEventServiceFeatureFlagTest : HmppsDomainEventServiceTestCase() {
  /**
   *  Feature flags:
   * - `person-languages-changed-notifications-enabled` : false
   * - `prisoner-merge-notifications-enabled` : true
   * - `prisoner-base-location-changed-notifications-enabled` is undefined (not set)
   */
  private val featureFlags = mapOf(
    FeatureFlagConfig.PERSON_LANGUAGES_CHANGED_NOTIFICATIONS_ENABLED to false,
    FeatureFlagConfig.PRISONER_MERGED_NOTIFICATIONS_ENABLED to true,
  )
  private val featureFlagTestConfig: FeatureFlagTestConfig = FeatureFlagTestConfig()
  private val hmppsId = "AA1234A"

  override val featureFlagConfig get() = featureFlagTestConfig.featureFlagConfig

  @BeforeEach
  internal fun setup() {
    assumeIdentities(hmppsId = hmppsId, prisonId = null)
    // Assuming defined feature flags
    featureFlags.forEach { featureFlagTestConfig.assumeFeatureFlag(it.key, it.value) }
  }

  @AfterEach
  internal fun tearDown() {
    featureFlagTestConfig.resetAllFlags()
  }

  // IntegrationEventTypes with no feature flag associated are enabled; e.g. person-status-changed
  @Test
  fun `should process and save an event without feature flag associated`() = executeShouldSaveEventNotification(
    hmppsDomainEvent = hmppsDomainEvent("prisoner-offender-search.prisoner.created"),
    generateEventNotification(IntegrationEventType.PERSON_STATUS_CHANGED, "v1/persons/$hmppsId"),
  )

  @Nested
  inner class GivenFeatureFlags {
    // IntegrationEventTypes associated with a feature flag set to “true” are enabled; e.g. prisoner-merged
    @Test
    fun `should process and save an event with feature flag enabled`() {
      // Arrange
      val removedNomisNumber = "AA0001A"
      val updatedNomisNumber = "AA0002A"
      val prisonId = "MDI"
      val hmppsId = updatedNomisNumber

      val hmppsDomainEvent = sqsNotificationHelper.createHmppsMergedDomainEvent(nomisNumber = updatedNomisNumber, removedNomisNumber = removedNomisNumber)

      val expectedEventNotifications = listOf(
        generateEventNotification(
          eventType = IntegrationEventType.PRISONER_MERGED,
          urlSuffix = "v1/persons/$removedNomisNumber",
          hmppsId = removedNomisNumber,
          prisonId = prisonId,
        ),
        generateEventNotification(
          eventType = IntegrationEventType.PERSON_STATUS_CHANGED,
          urlSuffix = "v1/persons/$hmppsId",
          hmppsId = hmppsId,
          prisonId = prisonId,
        ),
      )
      assumeIdentities(hmppsId, prisonId)

      // Act, Assert
      executeShouldSaveEventNotifications(hmppsDomainEvent, expectedEventNotifications)
    }

    // IntegrationEventTypes associated with a feature flag set to “false” are not enabled; e.g. person-languages-changed
    // IntegrationEventTypes that are not enabled are not written to the database
    @Test
    fun `should process and save event with feature flag enabled, and skip event with feature flag disabled`() {
      // Arrange
      val hmppsDomainEvent = hmppsDomainEvent("prisoner-offender-search.prisoner.created", "MDI")
      val unexpectedNotificationTypes = arrayOf(
        IntegrationEventType.PERSON_LANGUAGES_CHANGED,
      )
      val expectedNotificationTypes = listOf(
        IntegrationEventType.PERSON_STATUS_CHANGED,
        IntegrationEventType.PERSON_ADDRESS_CHANGED,
        IntegrationEventType.PRISONERS_CHANGED,
        IntegrationEventType.PRISONER_CHANGED,
      )

      // Act, Assert (unexpected events)
      executeShouldNotSaveEventNotification(hmppsDomainEvent, *unexpectedNotificationTypes)

      // Assert (expected events)
      expectedNotificationTypes.forEach { expectedNotificationType ->
        verify(exactly = 1) { eventNotificationRepository.insertOrUpdate(match { it.eventType == expectedNotificationType }) }
      }
    }

    // IntegrationEventTypes that reference a feature flag that does not exist are disabled; e.g. prisoner-base-location-changed
    // IntegrationEventTypes that are not enabled are not written to the database
    @Test
    fun `should process and NOT save an event with feature flag disabled`() = executeShouldNotSaveEventNotification(
      hmppsDomainEvent = sqsNotificationHelper.createHmppsPrisonerReceivedDomainEvent(),
      IntegrationEventType.PRISONER_BASE_LOCATION_CHANGED,
    )

    // Execute event and verify that no unexpected notification has been saved/persisted
    private fun executeShouldNotSaveEventNotification(
      hmppsDomainEvent: HmppsDomainEvent,
      vararg unexpectedNotificationTypes: IntegrationEventType,
    ) {
      // Act
      hmppsDomainEventService.execute(hmppsDomainEvent)

      // Assert
      // Verify no unexpected event notifications persisted via repository
      unexpectedNotificationTypes.forEach { unexpectedNotificationType ->
        verify(exactly = 0) { eventNotificationRepository.insertOrUpdate(match { it.eventType == unexpectedNotificationType }) }
      }
    }
  }

  private fun generateEventNotification(
    eventType: IntegrationEventType,
    urlSuffix: String,
    hmppsId: String = this.hmppsId,
    prisonId: String? = null,
  ) = EventNotification(
    eventType = eventType,
    url = "$baseUrl/$urlSuffix",
    hmppsId = hmppsId,
    prisonId = prisonId,
    lastModifiedDateTime = currentTime,
  )

  private fun hmppsDomainEvent(domainEventType: String) = sqsNotificationHelper.createHmppsDomainEvent(domainEventType)
  private fun hmppsDomainEvent(domainEventType: String, prisonId: String) = sqsNotificationHelper.createHmppsDomainEventWithPrisonId(domainEventType, prisonId = prisonId)
}
