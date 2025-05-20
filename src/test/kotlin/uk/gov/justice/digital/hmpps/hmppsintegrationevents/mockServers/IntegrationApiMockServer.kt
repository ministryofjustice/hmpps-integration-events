package uk.gov.justice.digital.hmpps.hmppsintegrationevents.mockServers

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.notMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders

class IntegrationApiMockServer(httpsPort: Int) :
  WireMockServer(
    WireMockConfiguration.options().dynamicPort().httpsPort(httpsPort),
  ) {
  companion object {
    fun create(httpsPort: Int): IntegrationApiMockServer = IntegrationApiMockServer(httpsPort)
  }

  fun stubApiResponse(apiKey: String, body: String) {
    stubFor(
      get("/v2/config/authorisation")
        .withHeader("x-api-key", matching(apiKey))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(body.trimIndent()),
        ),
    )

    stubFor(
      get("/v2/config/authorisation")
        .withHeader("x-api-key", notMatching(apiKey))
        .willReturn(
          WireMock.aResponse()
            .withStatus(401)
            .withBody("Unauthorized"),
        ),
    )
  }
}
