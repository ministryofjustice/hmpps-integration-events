package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.utils.SqsMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DomainEventsService

@Service
class HmppsDomainEventsListener(private val objectMapper: ObjectMapper, @Autowired val domainEventsService: DomainEventsService) {
  @SqsListener("prisoner", factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) {
    val sqsMessage: SqsMessage = objectMapper.readValue(rawMessage)
    domainEventsService.execute(sqsMessage)
  }
}
