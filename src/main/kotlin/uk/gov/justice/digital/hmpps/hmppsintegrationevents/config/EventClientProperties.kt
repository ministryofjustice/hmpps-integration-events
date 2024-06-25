package uk.gov.justice.digital.hmpps.hmppsintegrationevents.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "event")
data class EventClientProperties(
  val clients: Map<String, ClientConfig>,
)

data class ClientConfig(
  val queueId: String,
  val pathCode: String,
)
