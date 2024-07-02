package uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration

class HMPPSAuthMockServer internal constructor() : WireMockServer(8444) {
  fun stubGrantToken() {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/auth/oauth/token?grant_type=client_credentials"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                                      {
                                      "token_type": "bearer",
                                      "access_token": "ABCDE"
                                  }
                                
                                """
                .trimIndent(),
            ),
        ),
    )
  }
}

