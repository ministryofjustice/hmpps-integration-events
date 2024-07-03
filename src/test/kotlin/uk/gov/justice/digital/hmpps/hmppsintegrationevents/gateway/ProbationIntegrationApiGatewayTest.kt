package uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.wiremock.HmppsAuthExtension
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.wiremock.ProbationIntegrationApiExtension

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(ProbationIntegrationApiExtension::class, HmppsAuthExtension::class)
class ProbationIntegrationApiGatewayTest {
  @Autowired
  lateinit var probationIntegrationApiGateway: ProbationIntegrationApiGateway

  @BeforeEach
  fun setup() {
    ProbationIntegrationApiExtension.server.start()
    ProbationIntegrationApiExtension.server.stubGetPersonIdentifier("mockNomis", "mockCrn")
  }

  @AfterEach
  fun tearDown() {
    ProbationIntegrationApiExtension.server.stop()
  }

  @Test
  fun `Return null if person identifier is not found`() {
    val result = probationIntegrationApiGateway.getPersonIdentifier("otherNomis")

    result.shouldBeNull()
  }

  @Test
  fun `Return person identifier`() {
    val result = probationIntegrationApiGateway.getPersonIdentifier("mockNomis")

    result.shouldNotBeNull()
    result.crn.shouldBe("mockCrn")
  }
}
