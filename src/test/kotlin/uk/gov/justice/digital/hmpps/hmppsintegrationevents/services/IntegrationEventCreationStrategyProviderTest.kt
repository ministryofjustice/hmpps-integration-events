package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType

@ActiveProfiles("test")
class IntegrationEventCreationStrategyProviderTest {
  private val singleStrategy = mockk<DefaultEventCreationStrategy>(relaxed = true)
  private val multipleStrategy = mockk<MultipleEventCreationStrategy>(relaxed = true)

  private val strategyProvider = IntegrationEventCreationStrategyProvider(
    singleEmissionStrategy = singleStrategy,
    multipleEmissionStrategy = multipleStrategy,
  )

  @Test
  fun `should return multipleEmissionStrategy for PRISONER_MERGE`() {
    val result = strategyProvider.forEventType(IntegrationEventType.PRISONER_MERGE)

    assertThat(multipleStrategy).isEqualTo(result)
  }

  @Test
  fun `should return singleEmissionStrategy for other event types`() {
    val result = strategyProvider.forEventType(IntegrationEventType.CONTACT_CHANGED) // or any non-merge type

    assertThat(singleStrategy).isEqualTo(result)
  }
}
