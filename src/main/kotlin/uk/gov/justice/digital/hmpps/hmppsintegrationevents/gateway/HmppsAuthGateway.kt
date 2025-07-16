package uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway

import org.apache.tomcat.util.json.JSONParser
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.exceptions.AuthenticationFailedException
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*

@Component
@Scope("singleton")
class HmppsAuthGateway(
  @Value("\${services.hmpps-auth.base-url}") hmppsAuthUrl: String,
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

  private fun checkTokenValid(token: String): Boolean {
    val decodedToken = String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8)

    val exp = JSONParser(decodedToken).parseObject()["exp"].toString().toLong()
    val now = Instant.now().epochSecond
    return (now < exp)
  }
}
