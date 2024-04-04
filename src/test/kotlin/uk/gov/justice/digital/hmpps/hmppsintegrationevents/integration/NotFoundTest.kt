package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.IntegrationTestBase

class NotFoundTest : IntegrationTestBase() {

  @Test
  fun `Resources that aren't found should return 404 - test of the exception handler`() {
    webTestClient.get().uri("/some-url-not-found")
      .exchange()
      .expectStatus().isNotFound
  }
}
