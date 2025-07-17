package uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import java.time.Instant
import java.util.Base64
import java.util.UUID

class HmppsAuthMockServer internal constructor() : WireMockServer(8444) {
  val authUrl = "/auth/oauth/token?grant_type=client_credentials"

  fun getToken(expiresInMinutes: Long = 20): String {
    val decodedPayload = """
    {
      "client_id": "client_id",
      "exp": ${Instant.now().plusSeconds(60 * expiresInMinutes).epochSecond}
    }
    """.trimIndent()
    val encodedPayload = Base64.getEncoder().encodeToString(decodedPayload.toByteArray())
    val token = "${UUID.randomUUID()}.$encodedPayload.${UUID.randomUUID()}" // UUIDs to simulate the other parts of a JWT
    return token
  }

  fun stubGetOAuthToken(
    client: String,
    clientSecret: String,
    token: String = getToken(),
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
