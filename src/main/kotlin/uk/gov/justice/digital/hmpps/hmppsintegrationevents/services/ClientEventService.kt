package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.EventClientProperties
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Service
@EnableConfigurationProperties(EventClientProperties::class)
class ClientEventService( private val clientProperties: EventClientProperties,
                         private val hmppsQueueService: HmppsQueueService,) {
  val clientQueueList= clientProperties.clietns.map { it-> it.value.pathCode to hmppsQueueService.findByQueueName(it.value.queueName) }.toMap()
  fun getClientMessage( pathCode:String,  clientName:String):String {

   //if(clientProperties.clietns.containsKey(clientName))
     //return clientQueueList[pathCode].sqsClient.receiveMessage()

  return ""
  }
}