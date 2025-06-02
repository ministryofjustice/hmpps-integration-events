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
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.wiremock.PrisonerSearchMockServer

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(HmppsAuthExtension::class)
class PrisonerSearchGatewayTest {
  @Autowired
  lateinit var prisonerSearchGateway: PrisonerSearchGateway

  val mockServer = PrisonerSearchMockServer()

  @BeforeEach
  fun setup() {
    mockServer.start()
    mockServer.stubGetPrisoner("mockNomis")
  }

  @AfterEach
  fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun `Return null if prisoner is not found`() {
    val result = prisonerSearchGateway.getPrisoner("otherNomis")

    result.shouldBeNull()
  }

  @Test
  fun `Return prisoner`() {
    val result = prisonerSearchGateway.getPrisoner("mockNomis")

    result.shouldNotBeNull()
    result.prisonId.shouldBe("MDI")
  }
}
