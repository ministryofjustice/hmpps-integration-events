package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.SqsMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DomainEventsService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.util.SqsMessageReader

@Service
class HmppsDomainEventsListener(@Autowired val domainEventsService: DomainEventsService) {

  val log: Logger = LoggerFactory.getLogger(this::class.java)

  @SqsListener("prisoner", factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) {
    log.info("Received message: $rawMessage")
    val sqsMessage: SqsMessage = SqsMessageReader().mapRawMessage(rawMessage)
    domainEventsService.execute(sqsMessage)
  }
}
