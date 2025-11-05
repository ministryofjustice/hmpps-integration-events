package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.HmppsSecretManagerProperties

@Service
@EnableConfigurationProperties(
  HmppsSecretManagerProperties::class,
)
class SecretsManagerService(private val secretsManagerClient: SecretsManagerClient) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Returns the value of the specified secret, or an empty string if no value is found.
   */
  fun getSecretValue(secretId: String): String {
    val getSecretValueRequest = GetSecretValueRequest.builder()
      .secretId(secretId)
      .build()

    val secret = secretsManagerClient.getSecretValue(getSecretValueRequest)
    if (secret == null) {
      return ""
    }
    return secret.secretString().orEmpty()
  }

  fun setSecretValue(secretId: String, secretValue: String) {
    val putSecretValueRequest = PutSecretValueRequest.builder()
      .secretId(secretId)
      .secretString(secretValue)
      .build()

    val response = secretsManagerClient.putSecretValue(putSecretValueRequest)

    log.info("Secret version created ${response.versionId()} for secret $secretId")
  }
}
