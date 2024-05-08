package uk.gov.justice.digital.hmpps.hmppsintegrationevents.gateway

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.HmppsS3Properties
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.IntegrationApiProperties
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.S3Service
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory

@Service
@SuppressWarnings("unchecked")
@EnableConfigurationProperties(
  HmppsS3Properties::class,
  IntegrationApiProperties::class,
)
class IntegrationApiGateway(
  integrationApiConfig: IntegrationApiProperties,
  final val s3Service: S3Service,
) {

  private lateinit var webClient: WebClient

  init {
    val certificateInputStream = s3Service.getDocumentFile(integrationApiConfig.certificateBucketName, integrationApiConfig.certificatePath)

    val keyStore = KeyStore.getInstance("PKCS12")
    keyStore.load(certificateInputStream, integrationApiConfig.certificatePassword.toCharArray())

    val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
    keyManagerFactory.init(keyStore, integrationApiConfig.certificatePassword.toCharArray())

    val trustAllCerts = InsecureTrustManagerFactory.INSTANCE

    val sslContext = SslContextBuilder.forClient()
      .trustManager(trustAllCerts)
      .keyManager(keyManagerFactory)
      .build()

    val httpClient: HttpClient = HttpClient.create().secure { sslSpec -> sslSpec.sslContext(sslContext) }

    webClient = WebClient.builder()
      .clientConnector(ReactorClientHttpConnector(httpClient))
      .baseUrl(integrationApiConfig.url)
      .defaultHeaders { header -> header.set("x-api-key", integrationApiConfig.apiKey) }
      .build()
  }

  fun getApiAuthorizationConfig(): Map<String, List<String>> {
    return webClient.method(HttpMethod.GET)
      .uri("v1/config/authorisation")
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<Map<String, List<String>>>() {})
      .block()!!
      .mapKeys { (key, _) -> key.replace(".", "-") }
  }
}
