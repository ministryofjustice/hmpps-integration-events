package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway.PrisonerSearchGateway
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.prisonersearch.POSPrisoner

@ActiveProfiles("test")
class GetPrisonIdServiceTest {
  val prisonerSearchGateway: PrisonerSearchGateway = mock()
  val getPrisonIdService = GetPrisonIdService(prisonerSearchGateway)
  val nomsNumber = "A1234AB"
  val prisonId = "MDI"
  val prisoner = POSPrisoner(prisonerNumber = nomsNumber, prisonId = prisonId, firstName = "John", lastName = "Smith")

  @BeforeEach
  fun setup(){
    Mockito.reset(prisonerSearchGateway)
  }

  @Test
  fun `should get prison Id`() {
    whenever(prisonerSearchGateway.getPrisoner(nomsNumber)).thenReturn(prisoner)
    val prisonIdResult = getPrisonIdService.execute(nomsNumber)
    prisonIdResult.shouldBe(prisonId)
  }

  @Test
  fun `should return null when gateway returns null`() {
    whenever(prisonerSearchGateway.getPrisoner(nomsNumber)).thenReturn(null)
    val prisonIdResult = getPrisonIdService.execute(nomsNumber)
    prisonIdResult.shouldBeNull()
  }
}