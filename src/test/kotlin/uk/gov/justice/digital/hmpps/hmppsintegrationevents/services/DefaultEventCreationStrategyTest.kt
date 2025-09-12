package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PRISONER_OFFENDER_SEARCH_PRISONER_CREATED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PRISONER_OFFENDER_SEARCH_PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PROBATION_CASE_ENGAGEMENT_CREATED_MESSAGE
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.PROBATION_CASE_PRISON_IDENTIFIER_ADDED
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.generateHmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.registration.HmppsDomainEventMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.stream.Stream

class DefaultEventCreationStrategyTest {

  private val domainEventIdentitiesResolver = mockk<DomainEventIdentitiesResolver>()
  private val objectMapper = ObjectMapper()
  private val hmppsId = "X777776"
  private val prisonId = "LEI"
  private val baseUrl = "https://event-service.test"
  private var eventType = IntegrationEventType.PRISONER_CHANGED
  private val currentTime: LocalDateTime = LocalDateTime.now()
  private val zonedCurrentDateTime = currentTime.atZone(ZoneId.systemDefault())
  private var domainMessage = HmppsDomainEventMessage(
    eventType = eventType.name,
    occurredAt = "2024-08-13T14:15:16.460942253+01:00",
    prisonId = prisonId,
    personReference = null,
    additionalInformation = null,
  )

  private lateinit var strategy: DefaultEventCreationStrategy

  @BeforeEach
  fun setup() {
    strategy = DefaultEventCreationStrategy(domainEventIdentitiesResolver)

    every { domainEventIdentitiesResolver.getHmppsId(any()) } returns hmppsId
    every { domainEventIdentitiesResolver.getPrisonId(any()) } returns prisonId
  }

  @Test
  fun `should create a notification with resolved HMPPS ID and prison ID`() {
    val domainEvent: HmppsDomainEvent =
      SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEventWithPrisonId(
        eventType = "assessment.summary.produced",
        prisonId = prisonId,
      )
    domainMessage = objectMapper.readValue(domainEvent.message)

    val notifications = strategy.createNotifications(domainMessage, eventType, baseUrl)

    verify { domainEventIdentitiesResolver.getHmppsId(domainMessage) }
    verify { domainEventIdentitiesResolver.getPrisonId(domainMessage) }

    assertThat(notifications.size).isEqualTo(1)
    val notification = notifications[0]
    assertThat(notification)
      .extracting(
        EventNotification::eventType,
        EventNotification::hmppsId,
        EventNotification::url,
      ).containsExactly(
        IntegrationEventType.PRISONER_CHANGED,
        hmppsId,
        "$baseUrl/v1/prison/prisoners/$hmppsId",
      )
    assertThat(notification.eventType).isEqualTo(eventType)
    assertThat(notification.hmppsId).isEqualTo(hmppsId)
    assertThat(notification.prisonId).isEqualTo(prisonId)
    assertThat(notification.lastModifiedDateTime).isNotNull
    assertThat(notification.url).contains(hmppsId)
  }

  @ParameterizedTest
  @MethodSource("eventTypeAndIntegrationEventTypeProvider")
  fun `process event processing for api persons {hmppsId} `(domainEventType: String, hmppsMessageJson: String) {
    val hmppsMessage = hmppsMessageJson.replace("\\", "")
    val domainEvent = generateHmppsDomainEvent(domainEventType, hmppsMessage)
    domainMessage = objectMapper.readValue(domainEvent.message)

    every { domainEventIdentitiesResolver.getHmppsId(domainMessage) } returns "X777776" andThen "A1234BC"
    every { domainEventIdentitiesResolver.getPrisonId(domainMessage) } returns null

    val notifications = strategy.createNotifications(
      domainMessage,
      IntegrationEventType.PERSON_STATUS_CHANGED,
      baseUrl,
    )

    assertThat(notifications.size).isEqualTo(1)
    val notification = notifications[0]
    assertThat(notification)
      .extracting(EventNotification::eventType, EventNotification::hmppsId, EventNotification::url)
      .containsExactly(
        IntegrationEventType.PERSON_STATUS_CHANGED,
        "X777776",
        "$baseUrl/v1/persons/X777776",
      )
    assertThat(notification.lastModifiedDateTime).isAfterOrEqualTo(currentTime)
  }

  @Test
  fun `should throw exception for a domain registration event message where CRN does not exist in delius`() {
    val crn = "X123456"
    val event: HmppsDomainEvent =
      SqsNotificationGeneratingHelper(zonedCurrentDateTime)
        .createHmppsDomainEventWithReason(identifiers = "[{\"type\":\"CRN\",\"value\":\"$crn\"}]")
    domainMessage = objectMapper.readValue(event.message)

    every { domainEventIdentitiesResolver.getHmppsId(any()) } throws NotFoundException("Person with crn $crn not found")

    assertThatThrownBy {
      strategy.createNotifications(domainMessage, eventType, baseUrl)
    }.isInstanceOf(NotFoundException::class.java)
      .hasMessage("Person with crn $crn not found")
  }

  @Test
  fun `should create event notification with hmpps id as nomis number when no CRN and cannot find CRN by nomis number`() {
    val mockNomisId = "MOCK-NOMIS-ID"
    every { domainEventIdentitiesResolver.getHmppsId(any()) } returns mockNomisId
    every { domainEventIdentitiesResolver.getPrisonId(any()) } returns null

    val hmppsMessage = """
      {"eventType":"calculate-release-dates.prisoner.changed","description":"Prisoners release dates have been re-calculated","additionalInformation":{"prisonerId":"$mockNomisId","bookingId":1219387},"version":1,"occurredAt":"2024-08-13T14:15:16.460942253+01:00"}
    """.trimIndent()
    val domainEvent = generateHmppsDomainEvent("calculate-release-dates.prisoner.changed", hmppsMessage)
    domainMessage = objectMapper.readValue(domainEvent.message)
    eventType = IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE

    val notifications = strategy.createNotifications(domainMessage, eventType, baseUrl)

    assertThat(1).isEqualTo(notifications.size)

    val notification = notifications[0]
    assertThat(notification)
      .extracting(
        EventNotification::eventType,
        EventNotification::hmppsId,
        EventNotification::url,
      ).containsExactly(
        IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE,
        mockNomisId,
        "$baseUrl/v1/persons/$mockNomisId/sentences/latest-key-dates-and-adjustments",
      )
    assertThat(notification.lastModifiedDateTime).isAfterOrEqualTo(currentTime)
  }

  @Test
  fun `should throw exception for a domain event message with no prisonId which requires a prisonId`() {
    every { domainEventIdentitiesResolver.getPrisonId(any()) } returns null
    val event: HmppsDomainEvent =
      SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(identifiers = "[{\"type\":\"PNC\",\"value\":\"2018/0123456X\"}]")
    domainMessage = objectMapper.readValue(event.message)
    eventType = IntegrationEventType.PRISON_LOCATION_CHANGED

    assertThatThrownBy {
      strategy.createNotifications(domainMessage, eventType, baseUrl)
    }.isInstanceOf(NotFoundException::class.java)
      .hasMessage("Prison ID could not be found in domain event message for path v1/prison/{prisonId}/location/{locationKey}")
  }

  @Test
  fun `should create event notification for a domain registration event message with no CRN or no Nomis Number which doesn't require a hmppsId`() {
    val event: HmppsDomainEvent =
      SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEvent(identifiers = "[{\"type\":\"PNC\",\"value\":\"2018/0123456X\"}]")
    domainMessage = objectMapper.readValue(event.message)
    eventType = IntegrationEventType.PRISONERS_CHANGED

    assertDoesNotThrow {
      val createNotifications = strategy.createNotifications(domainMessage, eventType, baseUrl)
      createNotifications
      assertThat(createNotifications).hasSize(1)
    }
  }

  @Test
  fun `will process and save a prisoner released domain event message for event with message with reason is RELEASED`() {
    val mockNomisId = "MOCK-NOMIS-ID"
    val mockCrn = "mockCrn"
    val event: HmppsDomainEvent =
      SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEventWithReason(
        eventType = "prison-offender-events.prisoner.released",
        reason = "RELEASED",
        identifiers = "[{\"type\":\"nomsNumber\",\"value\":\"$mockNomisId\"}]",
      )
    domainMessage = objectMapper.readValue(event.message)
    eventType = IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE

    every { domainEventIdentitiesResolver.getHmppsId(any()) } returns mockCrn

    val notifications = strategy.createNotifications(domainMessage, eventType, baseUrl)

    assertThat(1).isEqualTo(notifications.size)

    assertThat(notifications[0])
      .extracting(
        EventNotification::eventType,
        EventNotification::hmppsId,
        EventNotification::url,
      ).containsExactly(
        IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE,
        mockCrn,
        "$baseUrl/v1/persons/$mockCrn/sentences/latest-key-dates-and-adjustments",
      )
    assertThat(notifications[0].lastModifiedDateTime).isAfterOrEqualTo(currentTime)
  }

  @Test
  fun `will process and save a prisoner released domain event message for event with message event type of CALCULATED_RELEASE_DATES_PRISONER_CHANGED`() {
    val mockNomisId = "MOCK-NOMIS-ID"
    val mockCrn = "mockCrn"
    val hmppsMessage = """
      {"eventType":"calculate-release-dates.prisoner.changed","description":"Prisoners release dates have been re-calculated","additionalInformation":{"prisonerId":"$mockNomisId","bookingId":1219387},"version":1,"occurredAt":"2024-08-13T14:15:16.460942253+01:00"}
    """.trimIndent()

    val event = generateHmppsDomainEvent("calculate-release-dates.prisoner.changed", hmppsMessage)
    domainMessage = objectMapper.readValue(event.message)
    eventType = IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE

    every { domainEventIdentitiesResolver.getHmppsId(any()) } returns mockCrn

    val notifications = strategy.createNotifications(domainMessage, eventType, baseUrl)

    assertThat(1).isEqualTo(notifications.size)
    assertThat(notifications[0])
      .extracting(
        EventNotification::eventType,
        EventNotification::hmppsId,
        EventNotification::url,
      ).containsExactly(
        IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE,
        mockCrn,
        "$baseUrl/v1/persons/$mockCrn/sentences/latest-key-dates-and-adjustments",
      )
    assertThat(notifications[0].lastModifiedDateTime).isAfterOrEqualTo(currentTime)
  }

  @Test
  fun `will process and save event message with no CRN and cannot find CRN by nomis number`() {
    val mockNomisId = "MOCK-NOMIS-ID"
    every { domainEventIdentitiesResolver.getHmppsId(any()) } returns mockNomisId

    val hmppsMessage = """
      {"eventType":"calculate-release-dates.prisoner.changed","description":"Prisoners release dates have been re-calculated","additionalInformation":{"prisonerId":"$mockNomisId","bookingId":1219387},"version":1,"occurredAt":"2024-08-13T14:15:16.460942253+01:00"}
    """.trimIndent()

    val event = generateHmppsDomainEvent("calculate-release-dates.prisoner.changed", hmppsMessage)
    domainMessage = objectMapper.readValue(event.message)
    eventType = IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE

    val notifications = strategy.createNotifications(domainMessage, eventType, baseUrl)

    assertThat(1).isEqualTo(notifications.size)
    assertThat(notifications[0])
      .extracting(
        EventNotification::eventType,
        EventNotification::hmppsId,
        EventNotification::url,
      ).containsExactly(
        IntegrationEventType.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE,
        mockNomisId,
        "$baseUrl/v1/persons/$mockNomisId/sentences/latest-key-dates-and-adjustments",
      )
    assertThat(notifications[0].lastModifiedDateTime).isAfterOrEqualTo(currentTime)
  }

  companion object {
    @JvmStatic
    fun eventTypeAndIntegrationEventTypeProvider() = Stream.of(
      Arguments.of("probation-case.engagement.created", PROBATION_CASE_ENGAGEMENT_CREATED_MESSAGE),
      Arguments.of("probation-case.prison-identifier.added", PROBATION_CASE_PRISON_IDENTIFIER_ADDED),
      Arguments.of("prisoner-offender-search.prisoner.created", PRISONER_OFFENDER_SEARCH_PRISONER_CREATED),
      Arguments.of("prisoner-offender-search.prisoner.updated", PRISONER_OFFENDER_SEARCH_PRISONER_UPDATED),
    )
  }
}
