package uk.gov.justice.digital.hmpps.hmppsintegrationevents.extensions

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.AdditionalInformation

class DomainEventPredicatesTests {

  @Test
  fun `is not a valid contact event - no additional information`() {
    val event = SqsNotificationGeneratingHelper().createHmppsDomainEvent().copy(additionalInformation = null)
    assertFalse(event.isValidContactEvent())
  }

  @Test
  fun `is not a valid contact event - not a visor contact`() {
    val event = SqsNotificationGeneratingHelper().createHmppsDomainEvent().copy(additionalInformation = AdditionalInformation(visorContact = false))
    assertFalse(event.isValidContactEvent())
  }

  @Test
  fun `is not a valid contact event - no mappa category`() {
    val event = SqsNotificationGeneratingHelper().createHmppsDomainEvent().copy(additionalInformation = AdditionalInformation(visorContact = true))
    assertFalse(event.isValidContactEvent())
  }

  @Test
  fun `is not a valid contact event - not valid mappa category`() {
    val event = SqsNotificationGeneratingHelper().createHmppsDomainEvent().copy(additionalInformation = AdditionalInformation(visorContact = true, mappaCategoryNumber = 999))
    assertFalse(event.isValidContactEvent())
  }

  @Test
  fun `is a valid contact event`() {
    val event = SqsNotificationGeneratingHelper().createHmppsDomainEvent().copy(additionalInformation = AdditionalInformation(visorContact = true, mappaCategoryNumber = 1))
    assertTrue(event.isValidContactEvent())
  }
}
