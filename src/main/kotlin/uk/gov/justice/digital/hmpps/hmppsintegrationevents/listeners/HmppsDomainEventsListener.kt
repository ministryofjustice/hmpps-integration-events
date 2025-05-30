package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import io.awspring.cloud.sqs.listener.AsyncAdapterBlockingExecutionFailedException
import io.awspring.cloud.sqs.listener.ListenerExecutionFailedException
import io.sentry.Sentry
import io.sentry.spring.jakarta.tracing.SentryTransaction
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.registration.HmppsDomainEventMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DeadLetterQueueService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.HmppsDomainEventService
import java.util.concurrent.CompletionException

@Service
@Transactional
class HmppsDomainEventsListener(
  @Autowired val hmppsDomainEventService: HmppsDomainEventService,
  @Autowired val deadLetterQueueService: DeadLetterQueueService,
) {

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val objectMapper = ObjectMapper()

  @SentryTransaction(operation = "messaging")
  @SqsListener("hmppsdomainqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) {
    log.info("Received message: $rawMessage")
    try {
      val hmppsDomainEvent: HmppsDomainEvent = objectMapper.readValue(rawMessage)
      val hmppsEvent: HmppsDomainEventMessage = objectMapper.readValue(hmppsDomainEvent.message)
      val matchingIntegrationEventTypes = IntegrationEventType.entries.filter { it.predicate.invoke(hmppsEvent) }
      if (matchingIntegrationEventTypes.isNotEmpty()) {
        hmppsDomainEventService.execute(hmppsDomainEvent, matchingIntegrationEventTypes)
      }
    } catch (e: Exception) {
      Sentry.captureException(unwrapSqsExceptions(e))
      throw e
    }
  }

  fun unwrapSqsExceptions(e: Throwable): Throwable {
    fun unwrap(e: Throwable) = e.cause ?: e
    var cause = e
    if (cause is CompletionException) {
      cause = unwrap(cause)
    }
    if (cause is AsyncAdapterBlockingExecutionFailedException) {
      cause = unwrap(cause)
    }
    if (cause is ListenerExecutionFailedException) {
      cause = unwrap(cause)
    }
    return cause
  }
}
