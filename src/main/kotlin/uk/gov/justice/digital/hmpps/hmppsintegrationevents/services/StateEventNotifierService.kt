package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.sentry.Sentry
import jakarta.transaction.Transactional
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import java.time.LocalDateTime
import java.util.*

/**
 This performs the read of the events within a transaction with a PESSIMISTIC lock so the transaction has exclusive access to the records.
 eventRepository.findAllEventsWithLastModifiedDateTimeBefore has a PESSIMISTIC_WRITE lock applied.
 This means that the thread obtains a lock on the record at the start of the transaction, for the purpose of writing (or deleting in this case).
 The thread will always be deleting this record, so no one can read or write to until the transaction is complete.
 Subsequent threads will not be able to read those records and will therefore not be candidates for deletion avoiding any optimistic locking issues.
 **/

@Service
@ConditionalOnProperty("feature-flag.event-state-management", havingValue = "true")
@Configuration
class StateEventNotifierService(
  private val integrationEventTopicService: IntegrationEventTopicService,
  val eventRepository: EventNotificationRepository,
) {
  @Scheduled(fixedRateString = "\${notifier.schedule.rate}")
  @Transactional
  fun sentNotifications() {
    val fiveMinutesAgo = LocalDateTime.now().minusMinutes(5)

    val claimId = UUID.randomUUID().toString()
    // Claim records to process
    eventRepository.setProcessing(fiveMinutesAgo, claimId)

    val events = eventRepository.findAllProcessingEvents(claimId)
    events.forEach {
      try {
        integrationEventTopicService.sendEvent(it)
        eventRepository.setProcessed(it.eventId!!)
      } catch (e: Exception) {
        Sentry.captureException(e)
      }
    }
  }
}
