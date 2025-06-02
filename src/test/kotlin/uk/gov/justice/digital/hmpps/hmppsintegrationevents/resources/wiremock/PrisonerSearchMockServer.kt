package uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock

class PrisonerSearchMockServer internal constructor() : WireMockServer(8446) {
  fun stubGetPrisoner(nomsNumber: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/prisoner/$nomsNumber"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                "prisonerNumber": "$nomsNumber",
                "firstName": "Jane",
                "lastName": "Smith",
                "prisonId": "MDI"
              }
              """
                .trimIndent(),
            ),
        ),
    )
  }
}