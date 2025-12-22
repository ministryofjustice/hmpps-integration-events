package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType

@Configuration
@ActiveProfiles("test")
class HmppsDomainEventServicePrisonerMergedTest : HmppsDomainEventServiceEventTestCase() {
  private val hmppsId = "hmpps-1234"
  private val prisonId = "MDI"
  private val sqsHelper = SqsNotificationGeneratingHelper(zonedCurrentDateTime)

  @BeforeEach
  internal fun setUp() {
    stubDomainEventIdentitiesResolver(hmppsId, prisonId)
  }

  @Test
  fun `will process and save a prisoner merged notification`() {
    // Merging "AA0001A" into "AA0002A" (associated with hmppsId "hmpps-1234")
    // Arrange
    val removedNomisNumber = "AA0001A"
    val updatedNomisNumber = "AA0002A"
    val expectedNotifications = mapOf(
      IntegrationEventType.PRISONER_MERGED to removedNomisNumber,
      IntegrationEventType.PERSON_STATUS_CHANGED to hmppsId,
    ).map { generateEventNotificationOfPrison(it.key, "$baseUrl/v1/persons/${it.value}", prisonId, it.value) }

    val event = sqsHelper.createHmppsMergedDomainEvent(nomisNumber = updatedNomisNumber, removedNomisNumber = removedNomisNumber).domainEvent()

    // Act, Assert
    executeShouldSaveEventNotification(event, expectedNotifications)
  }

  @Test
  fun `will throw an exception if the removed nomis number is missing`() {
    // Arrange
    val removedNomisNumber = null
    val updatedNomisNumber = "AA0002A"

    val event = sqsHelper.createHmppsMergedDomainEvent(nomisNumber = updatedNomisNumber, removedNomisNumber = removedNomisNumber).domainEvent()

    // Assert
    assertThrows<IllegalStateException> {
      // Act
      hmppsDomainEventService.execute(event)
    }

    // Assert
    val verifyEventPersistence: (IntegrationEventType, Int) -> Unit = { eventType, occurrence ->
      verify(exactly = occurrence) { eventNotificationRepository.insertOrUpdate(match { eventNotification -> eventNotification.eventType == eventType }) }
    }
    verifyEventPersistence(IntegrationEventType.PRISONER_MERGED, 0)
    verifyEventPersistence(IntegrationEventType.PERSON_STATUS_CHANGED, 1)
  }
}
