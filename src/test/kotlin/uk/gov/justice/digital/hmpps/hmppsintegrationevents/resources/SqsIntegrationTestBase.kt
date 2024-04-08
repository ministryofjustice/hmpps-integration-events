package uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class SqsIntegrationTestBase {

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  lateinit var webTestClient: WebTestClient

  protected val domainEventsQueueConfig by lazy { hmppsQueueService.findByQueueId("prisoner") ?: throw MissingQueueException("HmppsQueue prisoner not found") }
  protected val domainEventsQueueSqsUrl by lazy { domainEventsQueueConfig.queueUrl }
  protected val domainEventsQueueSqsClient by lazy { domainEventsQueueConfig.sqsClient }

  @BeforeEach
  fun cleanQueue() {
    domainEventsQueueSqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(domainEventsQueueSqsUrl).build()).get()
  }
}
