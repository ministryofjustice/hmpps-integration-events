package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueResponse

class SecretsmanagerServiceTests {

  val secretsManagerClient: SecretsManagerClient = mock()
  private lateinit var service: SecretsManagerService

  @BeforeEach
  fun setUp() {
    service = SecretsManagerService(secretsManagerClient)
  }

  @Test
  fun `getSecretValue call secretsMangerClient and return secretString`() {
    whenever(secretsManagerClient.getSecretValue(any<GetSecretValueRequest>())).thenReturn(GetSecretValueResponse.builder().secretString("MockValue").build())

    val response = service.getSecretValue("MockSecret")

    argumentCaptor<GetSecretValueRequest>().apply {
      verify(secretsManagerClient, times(1)).getSecretValue(capture())
      assertThat(firstValue.secretId()).isEqualTo("MockSecret")
    }
    assertThat(response).isEqualTo("MockValue")
  }

  @Test
  fun `setSecretValue call secretsMangerClient `() {
    whenever(secretsManagerClient.putSecretValue(any<PutSecretValueRequest>())).thenReturn(PutSecretValueResponse.builder().versionId("MockVersionId").build())

    service.setSecretValue("MockSecret", "MockValue")

    argumentCaptor<PutSecretValueRequest>().apply {
      verify(secretsManagerClient, times(1)).putSecretValue(capture())
      assertThat(firstValue.secretId()).isEqualTo("MockSecret")
      assertThat(firstValue.secretString()).isEqualTo("MockValue")
    }
  }

  @Test
  fun `getSecretValue call secretsMangerClient and return empty string, for secret value not yet set`() {
    whenever(secretsManagerClient.getSecretValue(any<GetSecretValueRequest>())).thenReturn(null)

    val response = service.getSecretValue("MockSecret")

    argumentCaptor<GetSecretValueRequest>().apply {
      verify(secretsManagerClient, times(1)).getSecretValue(capture())
      assertThat(firstValue.secretId()).isEqualTo("MockSecret")
    }
    assertThat(response).isEqualTo("")
  }
}
