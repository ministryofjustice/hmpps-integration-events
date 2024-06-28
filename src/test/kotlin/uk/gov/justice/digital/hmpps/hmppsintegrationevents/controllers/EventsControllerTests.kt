package uk.gov.justice.digital.hmpps.hmppsintegrationevents.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import software.amazon.awssdk.services.sqs.model.Message
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.EventResponse
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.MessageResponse
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.ReceiveMessageResult
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.ResponseMetadata
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.EventAPIMockMvc
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.ClientEventService

@WebMvcTest(controllers = [EventsController::class])
@ActiveProfiles("test")
class EventsControllerTests() {
  @Autowired private lateinit var springMockMvc: MockMvc

  @MockBean private lateinit var clientEventService: ClientEventService

  @Autowired private lateinit var objectMapper: ObjectMapper
  private val basePath = "/events/mockservice2"
  private val mockMvc: EventAPIMockMvc by lazy { EventAPIMockMvc(springMockMvc) }

  @Test
  fun `Request not contain subject-distinguished-name header, return 403`() {
    val result = mockMvc.performUnAuthorised(basePath)

    assertThat(result.response.status).isEqualTo(403)
  }

  @Test
  fun `Request client not configured to consumer events, return 403`() {
    val result = mockMvc.performAuthorisedWithCN(basePath, "clientNoEvent")

    assertThat(result.response.status).isEqualTo(403)
  }

  @Test
  fun `Request request path code mismatch, return 403`() {
    val result = mockMvc.performAuthorised("/events/otherClient")

    assertThat(result.response.status).isEqualTo(403)
  }

  @Test
  fun `Valid consumer, return consumer event`() {
    whenever(clientEventService.getClientMessage("mockservice2")).thenReturn(
      EventResponse(
        MessageResponse(
          receiveMessageResult = ReceiveMessageResult(messages = listOf(Message.builder().body("MockMessageBody"))),
          responseMetadata = ResponseMetadata(requestId = "MockRequestId"),
        ),
      ),
    )

    val result = mockMvc.performAuthorisedWithCN(basePath, "MockService2")
    var expectedResult = """
         {
          "ReceiveMessageResponse": {
            "ReceiveMessageResult": {
              "messages": [
                {
                  "messageId": null,
                  "receiptHandle": null,
                  "md5OfBody": null,
                  "body": "MockMessageBody",
                  "attributes": null,
                  "md5OfMessageAttributes": null,
                  "messageAttributes": null
                }
              ]
            },
            "ResponseMetadata": {
              "RequestId": "MockRequestId"
            }
          }
        }
        """.replace("(\"[^\"]*\")|\\s".toRegex(), "\$1")
    assertThat(result.response.status).isEqualTo(200)
    assertThat(result.response.contentAsString).isEqualTo(expectedResult)
  }
}
