package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.FeatureFlagConfig
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.exceptions.UnmappableUrlException
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository

@Service
@Configuration
class HmppsDomainEventService(
  @Autowired val eventNotificationRepository: EventNotificationRepository,
  @Autowired val deadLetterQueueService: DeadLetterQueueService,
  @Autowired val domainEventIdentitiesResolver: DomainEventIdentitiesResolver,
  @Value("\${services.integration-api.url}") val baseUrl: String,
  featureFlagConfig: FeatureFlagConfig,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  private val integrationEventTypeFilter = IntegrationEventTypeFilter(featureFlagConfig)

  fun execute(hmppsDomainEvent: HmppsDomainEvent) {
    // Matching domain event to integration event type(s)
    val integrationEventTypes = integrationEventTypeFilter.filterEventTypes(hmppsDomainEvent)

    if (integrationEventTypes.isNotEmpty()) {
      val hmppsId = domainEventIdentitiesResolver.getHmppsId(hmppsDomainEvent)
      val prisonId = domainEventIdentitiesResolver.getPrisonId(hmppsDomainEvent)

      for (integrationEventType in integrationEventTypes) {
        try {
          val eventNotification = integrationEventType.getNotification(baseUrl, hmppsId, prisonId, hmppsDomainEvent.additionalInformation)

          eventNotificationRepository.insertOrUpdate(eventNotification)
        } catch (ume: UnmappableUrlException) {
          log.warn(ume.message)
        }
      }
    }
  }
}

/**
 * Event filter of [IntegrationEventType] from domain event [HmppsDomainEvent], configurable with feature-flag.
 *
 * - IntegrationEventTypes with no feature flag associated are enabled
 * - IntegrationEventTypes associated with a feature flag set to “true” are enabled
 * - IntegrationEventTypes associated with a feature flag set to “false” are not enabled
 * - IntegrationEventTypes that reference a feature flag that does not exist are disabled,
 *      * and an error is logged with the name of the event and the name of the flag
 */
class IntegrationEventTypeFilter(
  private val featureFlagConfig: FeatureFlagConfig,
  private val log: Logger = defaultLogger,
) {
  companion object {
    private val defaultLogger = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Matching domain event to integration event type(s), respecting feature flags
   */
  fun filterEventTypes(hmppsEvent: HmppsDomainEvent) = IntegrationEventType.entries.filter { it.predicate.invoke(hmppsEvent) }.let { matchingTypes ->
    // Filter event types per feature flag, if associated with
    val eventTypes = matchingTypes.filter { eventType ->
      eventType.featureFlag?.let { feature ->
        // i) enabled or disabled according to the defined feature flag;
        // ii) otherwise disabled, when feature flag is associated but undefined
        featureFlagConfig.getConfigFlagValue(feature) ?: run {
          log.error("Missing feature flag \"{}\" of event type \"{}\"", feature, eventType.name)
          false
        }
      } ?: true // default true (enabled), if no feature-flag has been associated
    }
    if (eventTypes.size < matchingTypes.size) {
      val droppedTypes = (matchingTypes.toSet() - eventTypes.toSet()).sortedBy { it.name }
      log.info("These event type(s) have been discarded: {}", droppedTypes.map { it.name })
    }
    eventTypes
  }
}
