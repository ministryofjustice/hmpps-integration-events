package uk.gov.justice.digital.hmpps.hmppsintegrationevents.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "hmpps.secret")
data class HmppsSecretManagerProperties(
  val provider: String = "aws",
  val region: String = "eu-west-2",
  val localstackUrl: String = "http://localhost:4566",
  val secrets: Map<String, SecretConfig> = mapOf(),
) {
  data class SecretConfig(
    val secretName: String,
    val subscriberArn: String,
  )
}
