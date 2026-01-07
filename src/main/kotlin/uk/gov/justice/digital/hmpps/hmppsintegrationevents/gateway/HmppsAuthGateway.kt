package uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway

import org.apache.tomcat.util.json.JSONParser
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.exceptions.AuthenticationFailedException
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.TelemetryService
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.util.*

@Component
@Scope("singleton")
class HmppsAuthGateway(
  @Value("\${services.hmpps-auth.base-url}") hmppsAuthUrl: String,
  private val telemetryService: TelemetryService,
  private val clock: Clock,
) {
  private val webClient: WebClient = WebClient.builder().baseUrl(hmppsAuthUrl).build()

  @Value("\${services.hmpps-auth.username}")
  private lateinit var username: String

  @Value("\${services.hmpps-auth.password}")
  private lateinit var password: String

  private var existingAccessToken: String? = null

  companion object Credentials {
    fun toBasicAuth(username: String, password: String): String {
      val encodedCredentials = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
      return "Basic $encodedCredentials"
    }
  }

  fun reset() {
    existingAccessToken = null
  }

  fun getClientToken(service: String): String {
    existingAccessToken?.let {
      if (checkTokenValid(it)) {
        return it
      }
    }

    return try {
      val response =
        webClient
          .post()
          .uri("/auth/oauth/token?grant_type=client_credentials")
          .header("Authorization", Credentials.toBasicAuth(username, password))
          .retrieve()
          .bodyToMono(String::class.java)
          .block()

      val accessToken = JSONParser(response).parseObject()["access_token"].toString()
      this.existingAccessToken = accessToken
      accessToken
    } catch (exception: WebClientRequestException) {
      throw AuthenticationFailedException("Connection to ${exception.uri.authority} failed for $service.")
    } catch (exception: WebClientResponseException.ServiceUnavailable) {
      throw AuthenticationFailedException("${exception.request?.uri?.authority} is unavailable for $service.")
    } catch (exception: WebClientResponseException.Unauthorized) {
      throw AuthenticationFailedException("Invalid credentials used for $service.")
    }
  }

  private fun checkTokenValid(token: String): Boolean = try {
    val encodedPayload = token.split(".")[1]
    val decodedToken = String(Base64.getDecoder().decode(encodedPayload), StandardCharsets.UTF_8)
    val now = clock.instant().epochSecond
    val expiration = JSONParser(decodedToken).parseObject()["exp"].toString().toLong()
    (now < (expiration - 5))
  } catch (e: Exception) {
    telemetryService.captureException(e)
    false
  }
}
