package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType

@Service
class IntegrationEventCreationStrategyProvider(
  @Autowired private val singleEmissionStrategy: DefaultEventCreationStrategy,
  @Autowired private val multipleEmissionStrategy: MultipleEventCreationStrategy,
) {
  fun forEventType(eventType: IntegrationEventType): IntegrationEventCreationStrategy = when (eventType) {
    IntegrationEventType.PRISONER_MERGE -> multipleEmissionStrategy
    else -> singleEmissionStrategy
  }
}
