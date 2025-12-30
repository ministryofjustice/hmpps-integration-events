package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.FeatureFlagConfig
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType

class HmppsDomainEventServicePrisonerMergedTest : HmppsDomainEventServiceTestCase() {
  private val hmppsId = "hmpps-1234"
  private val featureFlagTestConfig: FeatureFlagTestConfig = FeatureFlagTestConfig()

  override val featureFlagConfig get() = featureFlagTestConfig.featureFlagConfig

  @BeforeEach
  internal fun setup() {
    assumeIdentities(hmppsId = hmppsId)

    // Enable the feature for event testing
    featureFlagTestConfig.assumeFeatureFlag(FeatureFlagConfig.PRISONER_MERGED_NOTIFICATIONS_ENABLED, true)
  }

  @Test
  fun `will process and save a prisoner merged notification`() {
    // Arrange
    val removedNomisNumber = "AA0001A"
    val updatedNomisNumber = "AA0002A"

    val hmppsDomainEvent = sqsNotificationHelper.createHmppsMergedDomainEvent(nomisNumber = updatedNomisNumber, removedNomisNumber = removedNomisNumber)
    val expectedEventNotifications = listOf(
      generateEventNotification(IntegrationEventType.PERSON_STATUS_CHANGED, "$baseUrl/v1/persons/$hmppsId", hmppsId),
      generateEventNotification(IntegrationEventType.PRISONER_MERGED, "$baseUrl/v1/persons/$removedNomisNumber", removedNomisNumber),
    )

    // Act, Assert
    executeShouldSaveEventNotifications(hmppsDomainEvent, expectedEventNotifications)
  }

  @Test
  fun `will throw an exception if the removed nomis number is missing`() {
    // Arrange
    val removedNomisNumber = null
    val updatedNomisNumber = "AA0002A"

    val hmppsDomainEvent = sqsNotificationHelper.createHmppsMergedDomainEvent(nomisNumber = updatedNomisNumber, removedNomisNumber = removedNomisNumber)
    val error = IllegalStateException("removedNomsNumber is required for PRISONER_MERGED event")

    // Act, Assert
    executeEventShouldThrowError<IllegalStateException>(hmppsDomainEvent, error)
  }
}
