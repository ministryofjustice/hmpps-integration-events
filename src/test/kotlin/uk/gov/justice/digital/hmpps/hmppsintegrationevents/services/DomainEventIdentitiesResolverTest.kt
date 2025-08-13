package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway.ProbationIntegrationApiGateway
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.DomainEvents.generateHmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.PersonExists
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.PersonIdentifier
import java.time.LocalDateTime
import java.time.ZoneId

class DomainEventIdentitiesResolverTest {
  private val objectMapper = ObjectMapper()
  private val currentTime: LocalDateTime = LocalDateTime.now()
  private val zonedCurrentDateTime = currentTime.atZone(ZoneId.systemDefault())

  private val getPrisonIdService = mockk<GetPrisonIdService>()
  private val probationIntegrationApiGateway = mockk<ProbationIntegrationApiGateway>()

  private lateinit var resolver: DomainEventIdentitiesResolver

  @BeforeEach
  fun setUp() {
    resolver = DomainEventIdentitiesResolver(probationIntegrationApiGateway, getPrisonIdService)
  }

  @Test
  fun `should throw exception for a domain registration event message where CRN does not exist in delius`() {
    val crn = "X123456"
    val domainEvent: HmppsDomainEvent =
      SqsNotificationGeneratingHelper(zonedCurrentDateTime).createHmppsDomainEventWithReason(identifiers = "[{\"type\":\"CRN\",\"value\":\"$crn\"}]")
    every { probationIntegrationApiGateway.getPersonExists(crn) } returns PersonExists(crn, false)

    val exception = assertThrows<NotFoundException> {
      resolver.getHmppsId(objectMapper.readValue(domainEvent.message))
    }

    assertThat(exception.message).isEqualTo("Person with crn $crn not found")
  }

  @Test
  fun `should return crn for domain event when both noms id and crn found for prisoner`() {
    val mockNomisId = "MOCK-NOMIS-ID"
    val crn = "X123456"
    val hmppsMessage = """
      {"eventType":"calculate-release-dates.prisoner.changed","description":"Prisoners release dates have been re-calculated","additionalInformation":{"prisonerId":"$mockNomisId","bookingId":1219387},"version":1,"occurredAt":"2024-08-13T14:15:16.460942253+01:00"}
    """.trimIndent()
    val domainEvent = generateHmppsDomainEvent("calculate-release-dates.prisoner.changed", hmppsMessage)
    every { probationIntegrationApiGateway.getPersonIdentifier(mockNomisId) } returns PersonIdentifier(crn, mockNomisId)

    val hmppsId = resolver.getHmppsId(objectMapper.readValue(domainEvent.message))

    assertThat(hmppsId).isEqualTo(crn)
  }

  @Test
  fun `should return nomsId for domain event with no crn and no crn found for prisoner`() {
    val mockNomisId = "MOCK-NOMIS-ID"
    val hmppsMessage = """
      {"eventType":"calculate-release-dates.prisoner.changed","description":"Prisoners release dates have been re-calculated","additionalInformation":{"prisonerId":"$mockNomisId","bookingId":1219387},"version":1,"occurredAt":"2024-08-13T14:15:16.460942253+01:00"}
    """.trimIndent()
    val domainEvent = generateHmppsDomainEvent("calculate-release-dates.prisoner.changed", hmppsMessage)

    every { probationIntegrationApiGateway.getPersonIdentifier(mockNomisId) } returns null

    val hmppsId = resolver.getHmppsId(objectMapper.readValue(domainEvent.message))

    assertThat(hmppsId).isEqualTo(mockNomisId)
  }
}
