package uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.exceptions.AuthenticationFailedException
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.wiremock.HmppsAuthExtension

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(HmppsAuthExtension::class)
class HmppsAuthGatewayTest {

  @Autowired
  lateinit var hmppsAuthGateway: HmppsAuthGateway

  @BeforeEach
  fun setup() {
    HmppsAuthExtension.server.start()
  }

  @AfterEach
  fun tearDown() {
    HmppsAuthExtension.server.stop()
    hmppsAuthGateway.reset()
  }

  @Test
  fun `throws an exception if connection is refused`() {
    HmppsAuthExtension.server.stop()

    val exception =
      shouldThrow<AuthenticationFailedException> {
        hmppsAuthGateway.getClientToken("NOMIS")
      }

    exception.message.shouldBe("Connection to localhost:8444 failed for NOMIS.")
  }

  @Test
  fun `throws an exception if auth service is unavailable`() {
    HmppsAuthExtension.server.stubServiceUnavailableForGetOAuthToken()

    val exception =
      shouldThrow<AuthenticationFailedException> {
        hmppsAuthGateway.getClientToken("NOMIS")
      }

    exception.message.shouldBe("localhost:8444 is unavailable for NOMIS.")
  }

  @Test
  fun `throws an exception if credentials are invalid`() {
    HmppsAuthExtension.server.stubUnauthorizedForGetOAAuthToken()

    val exception =
      shouldThrow<AuthenticationFailedException> {
        hmppsAuthGateway.getClientToken("NOMIS")
      }

    exception.message.shouldBe("Invalid credentials used for NOMIS.")
  }

  @Test
  fun `re-uses the existing access token if it is still valid`() {
    val firstMockedToken = HmppsAuthExtension.server.getToken()
    HmppsAuthExtension.server.stubGetOAuthToken("TestClient", "TestSecret", firstMockedToken)
    val firstToken = hmppsAuthGateway.getClientToken("NOMIS")
    firstToken shouldBe firstMockedToken

    val secondMockedToken = HmppsAuthExtension.server.getToken()
    HmppsAuthExtension.server.stubGetOAuthToken("TestClient", "TestSecret", HmppsAuthExtension.server.getToken())
    val secondToken = hmppsAuthGateway.getClientToken("NOMIS")
    secondToken shouldBe firstToken
    secondToken shouldNotBe secondMockedToken
  }

  @Test
  fun `asks for new token if the existing access token is not valid`() {
    val firstMockedToken = HmppsAuthExtension.server.getToken(expiresInMinutes = 0)
    HmppsAuthExtension.server.stubGetOAuthToken("TestClient", "TestSecret", firstMockedToken)
    val firstToken = hmppsAuthGateway.getClientToken("NOMIS")
    firstToken shouldBe firstMockedToken

    val secondMockedToken = HmppsAuthExtension.server.getToken()
    HmppsAuthExtension.server.stubGetOAuthToken("TestClient", "TestSecret", secondMockedToken)
    val secondToken = hmppsAuthGateway.getClientToken("NOMIS")
    secondToken shouldBe secondMockedToken
    secondToken shouldNotBe firstToken
  }
}
