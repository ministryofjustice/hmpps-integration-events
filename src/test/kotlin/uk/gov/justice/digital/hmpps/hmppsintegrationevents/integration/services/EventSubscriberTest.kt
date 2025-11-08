package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.services

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockReset.BEFORE
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesRequest
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.HmppsSecretManagerProperties
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.IntegrationApiProperties
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.mockServers.IntegrationApiMockServer
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.IntegrationEventTopicService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.SecretsManagerService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.SubscriberService
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.util.concurrent.TimeUnit

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("test")
class EventSubscriberTest {
  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  @MockitoSpyBean(reset = BEFORE)
  private lateinit var subscriberService: SubscriberService

  @Autowired
  private lateinit var secretService: SecretsManagerService

  @Autowired
  private lateinit var integrationEventTopicService: IntegrationEventTopicService

  @Autowired
  private lateinit var integrationApiProperties: IntegrationApiProperties

  @Autowired
  private lateinit var hmppsSecretManagerProperties: HmppsSecretManagerProperties

  private final val eventTopic by lazy { hmppsQueueService.findByTopicId("integrationeventtopic") as HmppsTopic }
  private final val hmppsEventsTopicSnsClient by lazy { eventTopic.snsClient }
  private final val subscriberArn by lazy { integrationEventTopicService.getSubscriptionArnByQueueName("subscribertestqueue") }

  fun getSubscriberFilterList(): String? {
    val getSubscriptionAttributesRequest = GetSubscriptionAttributesRequest.builder().subscriptionArn(subscriberArn).build()

    val getSubscriptionAttributesResponse = hmppsEventsTopicSnsClient.getSubscriptionAttributes(getSubscriptionAttributesRequest).get()

    return getSubscriptionAttributesResponse.attributes()["FilterPolicy"]
  }

  val server = IntegrationApiMockServer.create(httpsPort = 8443)

  @BeforeEach
  fun setUp() {
    server.start()
  }

  @AfterEach
  fun cleanup() {
    server.stop()
  }

  @Test
  fun `Subscriber Service should update client filter list in secret and subscription`() {
    val clientId = "MOCKSERVICE1"
    val prisonId = "MKI"
    val secret = hmppsSecretManagerProperties.secrets[clientId]
    val secretId = secret?.secretId
    secretId.shouldNotBeNull()

    val body = """
      {
        "$clientId": {
          "endpoints": [
            "/v1/persons/.*/risks/mappadetail",
            "/v1/persons/.*/risks/scores",
            "/v1/persons/.*/risks"
          ],
          "filters": {
            "prisons": [
              "$prisonId"
            ]
          }
        },
        "mockservice2": {
          "endpoints": [
            "/v1/persons/.*/risks"
          ],
          "filters": null
        }
      }
    """

    server.stubApiResponse(integrationApiProperties.apiKey, body)

    val originalFilterPolicy = "{\"eventType\":[\"DEFAULT\"]}"
    secretService.setSecretValue(secretId, originalFilterPolicy)
    integrationEventTopicService.updateSubscriptionAttributes("subscribertestqueue", "FilterPolicy", originalFilterPolicy)
    await.atMost(5, TimeUnit.SECONDS).untilAsserted {
      subscriberService.checkSubscriberFilterList()
      server.verify(moreThanOrExactly(1), getRequestedFor(urlEqualTo("/v2/config/authorisation")))
      // secret value updated
      val updatedSecretValue = secretService.getSecretValue(secretId)
      updatedSecretValue.shouldBe("{\"eventType\":[\"MAPPA_DETAIL_CHANGED\",\"RISK_SCORE_CHANGED\"],\"prisonId\":[\"$prisonId\"]}")

      // subscriber filter update
      val updatedFilterPolicy = getSubscriberFilterList()
      updatedFilterPolicy.shouldBe("{\"eventType\":[\"MAPPA_DETAIL_CHANGED\",\"RISK_SCORE_CHANGED\"],\"prisonId\":[\"$prisonId\"]}")
    }
  }
}
