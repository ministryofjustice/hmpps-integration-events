package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.services

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import io.kotest.matchers.shouldBe
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockReset
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesRequest
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.IntegrationEventTopicService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.SecretsManagerService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.SubscriberService
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.util.concurrent.TimeUnit

@ExtendWith(WireMockExtension::class, SpringExtension::class)
@SpringBootTest
@ActiveProfiles("test")
class EventSubscriberTests {

  @SpyBean(reset = MockReset.BEFORE)
  private lateinit var subscriberService: SubscriberService

  @Autowired
  private lateinit var s3Client: S3Client

  @Autowired
  private lateinit var secretService: SecretsManagerService

  @Autowired
  private lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  private lateinit var integrationEventTopicService: IntegrationEventTopicService

  private final val hmppsEventsTopicSnsClient: SnsAsyncClient
  private final val topicArn: String
  private final val subscriberArn: String

  private val testQueue by lazy { hmppsQueueService.findByQueueId("integrationeventtestqueue") as HmppsQueue }
  private val testQueueArn by lazy { testQueue.queueArn }
  init {
    val hmppsEventTopic = hmppsQueueService.findByTopicId("integrationeventtopic")
    topicArn = hmppsEventTopic!!.arn
    hmppsEventsTopicSnsClient = hmppsEventTopic.snsClient
    val listSubscriptionsByTopicRequest = ListSubscriptionsByTopicRequest.builder().topicArn(topicArn).build()

    val listSubscriptionsResponse = hmppsEventsTopicSnsClient.listSubscriptionsByTopic(listSubscriptionsByTopicRequest).get()

    subscriberArn = listSubscriptionsResponse.subscriptions().first {
      it.protocol() == "sqs" && it.endpoint() == testQueueArn
    }.subscriptionArn()
  }

  fun getSubscriberFilterList(): String? {
    val getSubscriptionAttributesRequest = GetSubscriptionAttributesRequest.builder().subscriptionArn(subscriberArn).build()

    val getSubscriptionAttributesResponse = hmppsEventsTopicSnsClient.getSubscriptionAttributes(getSubscriptionAttributesRequest).get()

    return getSubscriptionAttributesResponse.attributes().get("FilterPolicy")
  }

  @Test
  fun `Test Run once`() {
    stubApiResponse()
    var originalFilterPolicy = "{\"eventType\":[\"DEFAULT\"]}"
    secretService.setSecretValue("testSecret", originalFilterPolicy)
    integrationEventTopicService.updateSubscriptionAttributes(subscriberArn, "FilterPolicy", originalFilterPolicy)
    await.atMost(10000, TimeUnit.SECONDS).untilAsserted {
      verify(subscriberService, atLeast(1)).checkSubscriberFilterList()
      wireMockServer.verify(moreThanOrExactly(1), getRequestedFor(urlEqualTo("/v1/config/authorisation")))
    }

    // secret value updated
    val updatedSecretValue = secretService.getSecretValue("testSecret")
    updatedSecretValue.shouldBe("{\"eventType\":[\"REGISTRATION_ADDED\"]}")
    // subscriber filter update
    var updatedFilterPolicy = getSubscriberFilterList()
    updatedFilterPolicy.shouldBe("{\"eventType\":[\"REGISTRATION_ADDED\"]}")
  }
  companion object {
    @JvmStatic
    @RegisterExtension
    private val wireMockServer = WireMockExtension.newInstance()
      .options(
        WireMockConfiguration.wireMockConfig()
          .dynamicPort()
          .httpsPort(8443),

      )
      .build()
  }

  fun stubApiResponse() {
    wireMockServer.stubFor(
      WireMock.get(WireMock.urlMatching("/v1/config/authorisation"))
        .withHeader("x-api-key", WireMock.matching("mockApiKey"))
        .willReturn(
          WireMock.aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """{
                    "mockservice1": [
                        "/v1/persons/.*/risks/mappadetail",
                         "/v1/persons/.*/risks"
                    ],
                    "mockservice2": [
                         "/v1/persons/.*/risks"
                    ]
                }
              """.trimIndent(),
            ),
        ),
    )
    wireMockServer.stubFor(
      WireMock.get(WireMock.urlMatching("/v1/config/authorisation"))
        .withHeader("x-api-key", WireMock.notMatching("mockApiKey"))
        .willReturn(
          WireMock.aResponse()
            .withStatus(401)
            .withBody("Unauthorized"),
        ),
    )
  }
}
