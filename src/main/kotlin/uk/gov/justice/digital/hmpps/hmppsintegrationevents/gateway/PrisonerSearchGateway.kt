package uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.extensions.WebClientWrapper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.extensions.WebClientWrapper.WebClientWrapperResponse
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.UpstreamApi
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.prisonersearch.POSPrisoner

@Component
class PrisonerSearchGateway(
  @Value("\${services.prisoner-search.base-url}") baseUrl: String,
) {
  private val webClient = WebClientWrapper(baseUrl)

  @Autowired
  lateinit var hmppsAuthGateway: HmppsAuthGateway

  fun getPrisoner(nomsNumber: String): POSPrisoner? {
    val result =
      webClient.request<POSPrisoner>(
        HttpMethod.GET,
        "/prisoner/$nomsNumber",
        authenticationHeader(),
        UpstreamApi.PRISONER_SEARCH,
      )

    return when (result) {
      is WebClientWrapperResponse.Success -> {
        result.data
      }

      is WebClientWrapperResponse.Error -> {
        null
      }
    }
  }

  private fun authenticationHeader(): Map<String, String> {
    val token = hmppsAuthGateway.getClientToken("PRISONER_SEARCH")

    return mapOf(
      "Authorization" to "Bearer $token",
    )
  }
}
