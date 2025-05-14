package uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.IntegrationApiProperties
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.S3Service

@Service
class IntegrationApiGatewayTests {

  private lateinit var integrationApiProperties: IntegrationApiProperties
	
  private val s3Service: S3Service = mock()
	
  lateinit var integrationApiGateway: IntegrationApiGateway
  private lateinit var wireMockServer: WireMockServer

  @BeforeEach
  fun setUp() {
    wireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort().httpsPort(8550))
    wireMockServer.start()
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
    wireMockServer.stop()
  }

  @Test
  fun `getApiAuthorizationConfig should include x-api-key header`() {
    stubApiResponse()
    // Act
    integrationApiGateway.getApiAuthorizationConfig()
		
    // Assert
    wireMockServer.allServeEvents.forEach {
      it.request.headers.getHeader("x-api-key").firstValue().shouldBe("test-api-key")
    }
  }
	
  @Test
  fun `return client configs`() {
    stubApiResponse()
    // Act
    var result = integrationApiGateway.getApiAuthorizationConfig()
		
    // Assert
    result.keys.contains("mockservice1").shouldBeTrue()
    result["mockservice1"]!!.contains("/v1/persons/.*/risks/mappadetail").shouldBeTrue()
    result["mockservice1"]!!.contains("/v1/persons/.*/risks").shouldBeTrue()
    result.keys.contains("mockservice2").shouldBeTrue()
    result["mockservice2"]!!.contains("/v1/persons/.*/risks").shouldBeTrue()
  }
  fun stubApiResponse() {
    wireMockServer.stubFor(
      WireMock.get(WireMock.urlMatching("/v2/config/authorisation"))
        .withHeader("x-api-key", WireMock.matching("test-api-key"))
        .willReturn(
          WireMock.aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """{
                    "mockservice1": [
                        "/v1/persons/.*/risks/mappadetail",
                         "/v1/persons/.*/risks"
                    ],
                    "mockservice2": [
                         "/v1/persons/.*/risks"
                    ]
                }
              """.trimIndent(),
            ),
        ),
    )
    wireMockServer.stubFor(
      WireMock.get(WireMock.urlMatching("/v2/config/authorisation"))
        .withHeader("x-api-key", WireMock.notMatching("test-api-key"))
        .willReturn(
          WireMock.aResponse()
            .withStatus(401)
            .withBody("Unauthorized"),
        ),
    )
  }
}
