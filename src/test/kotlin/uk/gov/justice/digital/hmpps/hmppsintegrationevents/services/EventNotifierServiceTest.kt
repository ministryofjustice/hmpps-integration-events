package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.mockito.Mockito.mock
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.test.context.ContextConfiguration
import software.amazon.awssdk.services.sns.SnsAsyncClient
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@ContextConfiguration(
  initializers = [ConfigDataApplicationContextInitializer::class],
  classes = [EventNotifierService::class],
)
class EventNotifierServiceTest {

  private val hmppsQueueService: HmppsQueueService = mock()
  private val hmppsEventSnsClient: SnsAsyncClient = mock()
  private val eventRepository: EventNotificationRepository = mock()
}
