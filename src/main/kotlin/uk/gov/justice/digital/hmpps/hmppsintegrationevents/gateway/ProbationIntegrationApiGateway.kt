package uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.extensions.WebClientWrapper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.extensions.WebClientWrapper.WebClientWrapperResponse
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.PersonExists
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.PersonIdentifier
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.UpstreamApi

@Component
class ProbationIntegrationApiGateway(
  @Value("\${services.probation-integration.base-url}") baseUrl: String,
) {
  private val webClient = WebClientWrapper(baseUrl)

  @Autowired
  lateinit var hmppsAuthGateway: HmppsAuthGateway

  fun getPersonIdentifier(nomisId: String): PersonIdentifier? {
    val result =
      webClient.request<PersonIdentifier>(
        HttpMethod.GET,
        "/identifier-converter/noms-to-crn/$nomisId",
        authenticationHeader(),
        UpstreamApi.PROBATION_INTEGRATION,
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

  fun getPersonExists(crn: String): PersonExists {
    val result =
      webClient.request<PersonExists>(
        HttpMethod.GET,
        "/exists-in-delius/crn/$crn",
        authenticationHeader(),
        UpstreamApi.PROBATION_INTEGRATION,
      )

    return when (result) {
      is WebClientWrapperResponse.Success -> {
        result.data
      }

      is WebClientWrapperResponse.Error -> {
        PersonExists(crn, false)
      }
    }
  }

  private fun authenticationHeader(): Map<String, String> {
    val token = hmppsAuthGateway.getClientToken("ProbationIntegrationApi")

    return mapOf(
      "Authorization" to "Bearer $token",
    )
  }
}
