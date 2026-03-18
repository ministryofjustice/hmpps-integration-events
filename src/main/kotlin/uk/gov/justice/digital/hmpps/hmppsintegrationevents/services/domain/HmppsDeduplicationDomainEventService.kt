package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.domain

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.FeatureFlagConfig
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.exceptions.UnmappableUrlException
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import java.time.Clock
import java.time.LocalDateTime

@ConditionalOnProperty("feature-flag.${FeatureFlagConfig.DEDUPLICATE_EVENTS}", havingValue = "true")
@Service
class HmppsDeduplicationDomainEventService(
    @Autowired val eventNotificationRepository: EventNotificationRepository,
    @Autowired val domainEventIdentitiesResolver: DomainEventIdentitiesResolver,
    @Value("\${services.integration-api.url}") val baseUrl: String,
    private val clock: Clock,
    private val featureFlagConfig: FeatureFlagConfig,
) : HmppsDomainEventService {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override val log: Logger get() = Companion.log

  override fun execute(hmppsDomainEvent: HmppsDomainEvent) {
    // Matching domain event to integration event type(s)
    val integrationEventTypes = filterEventTypes(hmppsDomainEvent, featureFlagConfig)

    if (integrationEventTypes.isNotEmpty()) {
      val hmppsId = domainEventIdentitiesResolver.getHmppsId(hmppsDomainEvent)
      val prisonId = domainEventIdentitiesResolver.getPrisonId(hmppsDomainEvent)
      val nomisNumber = domainEventIdentitiesResolver.getNomisNumber(hmppsDomainEvent)

      val additionalInformation = hmppsDomainEvent.additionalInformation

      for (integrationEventType in integrationEventTypes) {
        try {
          val currentTime = LocalDateTime.now(clock)
          val eventNotification = integrationEventType.getNotification(baseUrl, hmppsId, prisonId, nomisNumber, additionalInformation, currentTime)

          eventNotificationRepository.insertOrUpdate(eventNotification)
        } catch (ume: UnmappableUrlException) {
          log.warn(ume.message)
        }
      }
    }
  }
}