

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.google.gson.GsonBuilder
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class IntegrationApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val integrationApi = IntegrationApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    integrationApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    integrationApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    integrationApi.stop()
  }
}

class IntegrationApiMockServer : WireMockServer(WIREMOCK_PORT) {
  private val gson = GsonBuilder().create()

  fun stubAuthorisationConfig() {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("v1/config/authorisation"))
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
  }
  companion object {
    private const val WIREMOCK_PORT = 8998
  }
}
