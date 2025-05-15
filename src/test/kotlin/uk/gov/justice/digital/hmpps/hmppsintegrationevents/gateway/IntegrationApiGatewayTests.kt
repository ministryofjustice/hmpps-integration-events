package uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.IntegrationApiProperties
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.mockServers.IntegrationApiMockServer
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.S3Service

@Service
class IntegrationApiGatewayTests {

  private lateinit var integrationApiProperties: IntegrationApiProperties
	
  private val s3Service: S3Service = mock()
	
  lateinit var integrationApiGateway: IntegrationApiGateway

  val server = IntegrationApiMockServer.create(8550)

  val body = """
      {
        "mockservice1": {
          "endpoints": ["/v1/persons/.*/risks/mappadetail", "/v1/persons/.*/risks"],
          "filters": null
        },
        "mockservice2": {
          "endpoints": ["/v1/persons/.*/risks"],
          "filters": null
        }
      }
    """

  @BeforeEach
  fun setUp() {
    server.start()
    integrationApiProperties = IntegrationApiProperties(
      url = "https://localhost:8550/",
      apiKey = "test-api-key",
      certificateBucketName = "test-bucket",
      certificatePath = "client.p12",
      certificatePassword = "client",
    )

    val certificateStream = this::class.java.getResourceAsStream("/certificates/client.p12")
    requireNotNull(certificateStream) { "certificate/client.p12 not found on classpath" }
    whenever(s3Service.getDocumentFile(integrationApiProperties.certificateBucketName, integrationApiProperties.certificatePath))
      .thenReturn(certificateStream)
		
    integrationApiGateway = IntegrationApiGateway(integrationApiProperties, s3Service)
  }

  @AfterEach
  fun tearDown() {
    server.stop()
  }

  @Test
  fun `getApiAuthorizationConfig should include x-api-key header`() {
    server.stubApiResponse("test-api-key", body)
    // Act
    integrationApiGateway.getApiAuthorizationConfig()
		
    // Assert
    server.allServeEvents.forEach {
      it.request.headers.getHeader("x-api-key").firstValue().shouldBe("test-api-key")
    }
  }

  @Test
  fun `return client configs`() {
    server.stubApiResponse("test-api-key", body)
    // Act
    val result = integrationApiGateway.getApiAuthorizationConfig()

    // Assert
    result.keys.contains("mockservice1").shouldBeTrue()
    result["mockservice1"]!!.endpoints.contains("/v1/persons/.*/risks/mappadetail").shouldBeTrue()
    result["mockservice1"]!!.endpoints.contains("/v1/persons/.*/risks").shouldBeTrue()
    result.keys.contains("mockservice2").shouldBeTrue()
    result["mockservice2"]!!.endpoints.contains("/v1/persons/.*/risks").shouldBeTrue()
  }
}
