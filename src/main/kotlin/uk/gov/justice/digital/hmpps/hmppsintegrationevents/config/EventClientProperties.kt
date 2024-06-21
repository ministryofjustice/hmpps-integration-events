package uk.gov.justice.digital.hmpps.hmppsintegrationevents.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "event.clients")
data class EventClientProperties(
  val clietns: Map<String, ClientConfig>
)

data class ClientConfig(
  val queueName:String,
  val pathCode:String
)