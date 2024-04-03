package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class HmppsRegistrationListener(private val objectMapper: ObjectMapper) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener("prisoner", factory = "hmppsQueueContainerFactoryProxy")
  fun onAuditEvent(rawMessage: String) {
    log.info("Received message {}", rawMessage)
  }
}
