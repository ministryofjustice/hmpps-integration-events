package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.utils.SqsMessage

@Service
class DomainEventsService {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
  fun execute(sqsMessage: SqsMessage) {
    log.info("Received sqsMessage with type {}", sqsMessage.type)
    log.info("Received sqsMessage with a message body of {}", sqsMessage.message)
    log.info("Received sqsMessage with a messageId {}", sqsMessage.messageId)
//    log.info("Received sqsMessage with atrributes {}", sqsMessage.messageAttributes)
  }
}