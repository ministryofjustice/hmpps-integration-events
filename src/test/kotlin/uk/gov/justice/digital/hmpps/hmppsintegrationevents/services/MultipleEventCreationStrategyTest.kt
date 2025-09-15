package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEventName.PrisonOffenderEvents
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.registration.HmppsDomainEventMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime
import java.time.ZoneId

class MultipleEventCreationStrategyTest {

  private val domainEventIdentitiesResolver = mockk<DomainEventIdentitiesResolver>()
  private val objectMapper = ObjectMapper()
  private val hmppsId = "X777776"
  private val prisonId = "LEI"
  private val baseUrl = "https://event-service.test"
  private var eventType = PrisonOffenderEvents.Prisoner.MERGED
  private val currentTime: LocalDateTime = LocalDateTime.now()
  private val zonedCurrentDateTime = currentTime.atZone(ZoneId.systemDefault())
  private var domainMessage = HmppsDomainEventMessage(
    eventType = eventType,
    occurredAt = "2024-08-13T14:15:16.460942253+01:00",
    prisonId = prisonId,
    personReference = null,
    additionalInformation = null,
  )

  private lateinit var strategy: MultipleEventCreationStrategy

  @BeforeEach
  fun setup() {
    strategy = MultipleEventCreationStrategy(domainEventIdentitiesResolver)

    every { domainEventIdentitiesResolver.getHmppsId(any()) } returns hmppsId
    every { domainEventIdentitiesResolver.getPrisonId(any()) } returns prisonId
  }

  @Test
  fun `should create two notifications with resolved nomsId`() {
    val nomisNumber = "A1234BC"
    val removedNomisNumber = "A1234B"
    val domainEvent: HmppsDomainEvent =
      SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsMergedDomainEvent(
        nomisNumber = nomisNumber,
        removedNomisNumber = removedNomisNumber,
      )
    domainMessage = objectMapper.readValue(domainEvent.message)

    val notifications = strategy.createNotifications(domainMessage, IntegrationEventType.PERSON_STATUS_CHANGED, baseUrl)

    assertThat(notifications)
      .extracting(
        EventNotification::eventType,
        EventNotification::hmppsId,
        EventNotification::url,
      )
      .containsExactlyInAnyOrder(
        tuple(IntegrationEventType.PERSON_STATUS_CHANGED, nomisNumber, "$baseUrl/v1/persons/$nomisNumber"),
        tuple(IntegrationEventType.PERSON_STATUS_CHANGED, removedNomisNumber, "$baseUrl/v1/persons/$removedNomisNumber"),
      )
  }
}
