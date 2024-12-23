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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockReset.BEFORE
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesRequest
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.IntegrationEventTopicService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.SecretsManagerService
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.SubscriberService
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.util.concurrent.TimeUnit

@ExtendWith(WireMockExtension::class, SpringExtension::class)
@SpringBootTest
@ActiveProfiles("test")
class EventSubscriberTest() {

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  @MockitoSpyBean(reset = BEFORE)
  private lateinit var subscriberService: SubscriberService

  @Autowired
  private lateinit var secretService: SecretsManagerService

  @Autowired
  private lateinit var integrationEventTopicService: IntegrationEventTopicService

  private final val eventTopic by lazy { hmppsQueueService.findByTopicId("integrationeventtopic") as HmppsTopic }
  private final val hmppsEventsTopicSnsClient by lazy { eventTopic.snsClient }
  private final val subscriberArn by lazy { integrationEventTopicService.getSubscriptionArnByQueueName("subscribertestqueue") }

  fun getSubscriberFilterList(): String? {
    val getSubscriptionAttributesRequest = GetSubscriptionAttributesRequest.builder().subscriptionArn(subscriberArn).build()

    val getSubscriptionAttributesResponse = hmppsEventsTopicSnsClient.getSubscriptionAttributes(getSubscriptionAttributesRequest).get()

    return getSubscriptionAttributesResponse.attributes()["FilterPolicy"]
  }

  @Test
  fun `Subscriber Service should update client filter list in secret and subscription`() {
    stubApiResponse()
    val originalFilterPolicy = "{\"eventType\":[\"DEFAULT\"]}"
    secretService.setSecretValue("testSecret", originalFilterPolicy)
    integrationEventTopicService.updateSubscriptionAttributes("subscribertestqueue", "FilterPolicy", originalFilterPolicy)
    await.atMost(5, TimeUnit.SECONDS).untilAsserted {
      verify(subscriberService, atLeast(1)).checkSubscriberFilterList()
      wireMockServer.verify(moreThanOrExactly(1), getRequestedFor(urlEqualTo("/v1/config/authorisation")))
      // secret value updated
      val updatedSecretValue = secretService.getSecretValue("testSecret")
      updatedSecretValue.shouldBe("{\"eventType\":[\"MAPPA_DETAIL_CHANGED\",\"RISK_SCORE_CHANGED\"]}")
      // subscriber filter update
      val updatedFilterPolicy = getSubscriberFilterList()
      updatedFilterPolicy.shouldBe("{\"eventType\":[\"MAPPA_DETAIL_CHANGED\",\"RISK_SCORE_CHANGED\"]}")
    }
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
                        "/v1/persons/.*/risks/scores",
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
