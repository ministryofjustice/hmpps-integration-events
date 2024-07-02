package uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock

class ProbationIntegrationApiMockServer internal constructor() : WireMockServer(8445) {
  fun stubGetPersonIdentifier(nomisId: String, crn: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/identifier-converter/noms-to-crn/$nomisId"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                                      {
                                      "crn": "$crn",
                                      "nomisId": "$nomisId"
                                  }
                                
                                """
                .trimIndent(),
            ),
        ),
    )
  }
}
