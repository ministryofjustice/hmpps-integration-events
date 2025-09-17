package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.listeners

import io.kotest.matchers.shouldBe
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doNothing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.EventNotificationRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.wiremock.HmppsAuthExtension
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.wiremock.ProbationIntegrationApiExtension
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DeleteProcessedService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.StateEventNotifierService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.SubscriberService
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(ProbationIntegrationApiExtension::class, HmppsAuthExtension::class)
class NonAssociationEventIntegrationTest : SqsIntegrationTestBase() {

  @MockitoBean
  private lateinit var subscriberService: SubscriberService

  @MockitoBean
  private lateinit var deleteProcessedService: DeleteProcessedService

  @MockitoBean
  private lateinit var stateEventNotifierService: StateEventNotifierService

  @Autowired
  lateinit var repo: EventNotificationRepository

  val nomsNumber = "A1234BD"
  val crn = "mockCrn"
  val prisonId = "MDI"
  val prisonerSearchMockServer = PrisonerSearchMockServer()
  val awaitTimeOut = Duration.ofSeconds(30)
  val awaitPollDelay = Duration.ofMillis(200)

  @BeforeEach
  fun setup() {
    repo.deleteAll()
    ProbationIntegrationApiExtension.server.stubGetPersonIdentifier(nomsNumber, crn)
    prisonerSearchMockServer.start()
    Awaitility.setDefaultTimeout(awaitTimeOut)
    Awaitility.setDefaultPollDelay(awaitPollDelay)
    doNothing().`when`(subscriberService).checkSubscriberFilterList()
    doNothing().`when`(deleteProcessedService).deleteProcessedEvents()
    doNothing().`when`(stateEventNotifierService).sentNotifications()
  }

  @AfterEach
  fun cleanup() {
    prisonerSearchMockServer.stop()
    Awaitility.reset()
  }

  @Test
  fun `will not process or save a any event triggered by a prisoner created event where there is no prison id in the response from prisoner search`() {
    prisonerSearchMockServer.stubGetPrisonerNullPrisonId(nomsNumber)
    generateRawPersonCreatedEvent().also { sendDomainSqsMessage(it) }
    Awaitility.await().until { repo.findAll().isNotEmpty() }
    repo.findAll().size.shouldBe(23)
    assertThat(getNumberOfMessagesCurrentlyOndomainEventsDeadLetterQueue()).isEqualTo(0)
  }

  @Test
  fun `will process or save a all events triggered by a prisoner created event where there is no prison id in the response from prisoner search`() {
    prisonerSearchMockServer.stubGetPrisoner(nomsNumber)
    generateRawPersonCreatedEvent().also { sendDomainSqsMessage(it) }
    Awaitility.await().until { repo.findAll().isNotEmpty() }
    repo.findAll().size.shouldBe(24)
    assertThat(getNumberOfMessagesCurrentlyOndomainEventsDeadLetterQueue()).isEqualTo(0)
  }

  fun generateRawPersonCreatedEvent() = """
  {
    "Type" : "Notification",
    "MessageId" : "eb4a33f3-1b4e-5646-80a9-52b00650b7ff",
    "TopicArn" : "N/A",
    "Message" : "{\"additionalInformation\":{\"nomsNumber\":\"A1234BD\"},\"occurredAt\":\"2025-09-16T09:07:56.135238735+01:00\",\"eventType\":\"prisoner-offender-search.prisoner.created\",\"version\":1,\"description\":\"A prisoner record has been created\",\"detailUrl\":\"http://localhost:8080/prisoner/A1234BD\",\"personReference\":{\"identifiers\":[{\"type\":\"NOMS\",\"value\":\"A1234BD\"}]}}",
    "Timestamp" : "2025-09-16T08:07:58.218Z",
    "SignatureVersion" : "1",
    "Signature" : "N/A",
    "SigningCertURL" : "N/A",
    "UnsubscribeURL" : "N/A",
    "MessageAttributes" : {
      "eventType" : {"Type":"String","Value":"prisoner-offender-search.prisoner.created"}
    }
  }  
  """.trimIndent()
}
