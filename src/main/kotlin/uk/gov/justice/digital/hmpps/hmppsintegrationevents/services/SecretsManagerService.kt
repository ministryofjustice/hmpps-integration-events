package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

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

  fun getSecretValue(secretId: String): String {
    val getSecretValueRequest = GetSecretValueRequest.builder()
      .secretId(secretId)
      .build()

    return secretsManagerClient.getSecretValue(getSecretValueRequest).secretString()
  }

  fun setSecretValue(secretId: String, secretValue: String) {
    val putSecretValueRequest = PutSecretValueRequest.builder()
      .secretId(secretId)
      .secretString(secretValue)
      .build()

    secretsManagerClient.putSecretValue(putSecretValueRequest)
  }
}
