package uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock

class HmppsAuthMockServer internal constructor() : WireMockServer(8444) {
  val token = "mock-bearer-token"
  val authUrl = "/auth/oauth/token?grant_type=client_credentials"

  fun stubGetOAuthToken(
    client: String,
    clientSecret: String,
  ) {
    stubFor(
      WireMock.post(authUrl)
        .withBasicAuth(client, clientSecret)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
              { 
                "access_token": "$token"
              }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubServiceUnavailableForGetOAuthToken() {
    stubFor(
      WireMock.post(authUrl)
        .willReturn(
          WireMock.serviceUnavailable(),
        ),
    )
  }

  fun stubUnauthorizedForGetOAAuthToken() {
    stubFor(
      WireMock.post(authUrl)
        .willReturn(
          WireMock.unauthorized(),
        ),
    )
  }
}
