package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway.ProbationIntegrationApiGateway
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.registration.HmppsDomainEventMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime

@Service
@Configuration
class HmppsDomainEventService(
  @Autowired val eventNotificationRepository: EventNotificationRepository,
  @Autowired val deadLetterQueueService: DeadLetterQueueService,
  @Autowired val probationIntegrationApiGateway: ProbationIntegrationApiGateway,
  @Autowired val getPrisonIdService: GetPrisonIdService,
  @Value("\${services.integration-api.url}") val baseUrl: String,
) {
  private val objectMapper = ObjectMapper()

  fun execute(hmppsDomainEvent: HmppsDomainEvent, integrationEventTypes: List<IntegrationEventType>) {
    val hmppsEvent: HmppsDomainEventMessage = objectMapper.readValue(hmppsDomainEvent.message)
    val hmppsId = getHmppsId(hmppsEvent)
    val prisonId = getPrisonId(hmppsEvent)

    for (integrationEventType in integrationEventTypes) {
      val eventNotification = EventNotification(
        eventType = integrationEventType,
        hmppsId = hmppsId,
        prisonId = prisonId,
        url = "$baseUrl/${integrationEventType.path(hmppsId, prisonId,hmppsEvent.additionalInformation)}",
        lastModifiedDateTime = LocalDateTime.now(),
      )
      eventNotificationRepository.insertOrUpdate(eventNotification)
    }
  }

  /**
   * The hmpps id is an id that the end client will use in ongoing processing.
   * In the future when we have a core person record it will be that id
   * for now the id will default to the crn but if there is no crn it will be the noms number.
   * The end client that receives the messages must treat it as a hmpps_id and NOT a crn/noms number.
   * A look-up service exist to decode the hmpps_id into a crn or noms number.
   */
  private fun getHmppsId(hmppsEvent: HmppsDomainEventMessage): String? {
    val crn: String? = hmppsEvent.personReference?.findCrnIdentifier()
    if (crn != null) {
      probationIntegrationApiGateway.getPersonExists(crn).let {
        if (it.existsInDelius) {
          return crn
        }
        throw NotFoundException("Person with crn $crn not found")
      }
    }

    val nomsNumber = getNomisNumber(hmppsEvent)

    return nomsNumber?.let { noms ->
      probationIntegrationApiGateway.getPersonIdentifier(noms)?.crn ?: noms
    }
  }

  private fun getNomisNumber(hmppsEvent: HmppsDomainEventMessage): String? {
    val nomsNumber = hmppsEvent.personReference?.findNomsIdentifier()
      ?: hmppsEvent.additionalInformation?.nomsNumber
      ?: hmppsEvent.additionalInformation?.prisonerId
      ?: hmppsEvent.additionalInformation?.prisonerNumber
      ?: hmppsEvent.prisonerId

    return nomsNumber
  }

  private fun getPrisonId(hmppsEvent: HmppsDomainEventMessage): String? {
    val prisonId = hmppsEvent.prisonId
      ?: hmppsEvent.additionalInformation?.prisonId
    if (prisonId != null) {
      return prisonId
    }

    val nomsNumber = getNomisNumber(hmppsEvent)
    if (nomsNumber != null) {
      return getPrisonIdService.execute(nomsNumber)
    }

    val locationKey = hmppsEvent.additionalInformation?.key
    if (locationKey != null) {
      val regex = Regex("^[A-Z]*((?=-)|$)")
      val match = regex.find(locationKey)
      if (match != null) {
        return match.groups.first()?.value
      }
    }

    return null
  }
}
