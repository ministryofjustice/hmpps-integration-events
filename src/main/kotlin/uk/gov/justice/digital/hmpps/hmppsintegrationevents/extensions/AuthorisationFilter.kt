package uk.gov.justice.digital.hmpps.hmppsintegrationevents.extensions

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.EventClientProperties
import java.io.IOException

@Component
@Order(1)
@EnableConfigurationProperties(EventClientProperties::class)
class AuthorisationFilter(
  private val clientProperties: EventClientProperties,
) : Filter {
  val pathPattern: String = "/events/\\.*+[^/]*\$"
  val allowedList= listOf("/info","/health", "/health/ping","/health/readiness","/health/liveness")

  @Throws(IOException::class, ServletException::class)
  override fun doFilter(
    request: ServletRequest,
    response: ServletResponse?,
    chain: FilterChain,
  ) {
    val req = request as HttpServletRequest
    val res = response as HttpServletResponse

    val subjectDistinguishedNameHeader = req.getHeader("subject-distinguished-name")
    val subjectDistinguishedName = extractConsumerName(subjectDistinguishedNameHeader)

    val requestedPath = req.requestURI

    if(!allowedList.contains(requestedPath)){
        if (subjectDistinguishedName == null) {
          res.sendError(
            HttpServletResponse.SC_FORBIDDEN,
            "No subject-distinguished-name header provided for authorisation"
          )
          return
        }

        if (!Regex(pathPattern).matches(req.requestURI) || !clientProperties.clients.containsKey(
            subjectDistinguishedName
          )
        ) {
          res.sendError(
            HttpServletResponse.SC_FORBIDDEN,
            "Unable to authorise $requestedPath for $subjectDistinguishedName"
          )
          return
        }
      }

    chain.doFilter(request, response)
  }

  fun extractConsumerName(subjectDistinguishedName: String?): String? {
    if (subjectDistinguishedName.isNullOrEmpty()) {
      return null
    }

    val match = Regex("^.*,CN=(.*)$").find(subjectDistinguishedName)

    if (match?.groupValues == null) {
      return null
    }

    return match.groupValues[1]
  }
}
