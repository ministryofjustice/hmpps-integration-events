package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.util

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers.SqsNotificationGeneratingHelper
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.SqsMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.util.SqsMessageReader
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SqsMessageReaderTest {

  @Test
  fun testSqsMessageReaderCreatesObjectFromRawSqsMessageCorrectly() {
    val expectedEventType = "some-test-event"
    val currentTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
    val rawMessage = SqsNotificationGeneratingHelper().generate(eventTypeValue = expectedEventType, timestamp = currentTime)

    val sqsMessage: SqsMessage = SqsMessageReader().mapRawMessage(rawMessage)

    assert(sqsMessage.message.eventType == expectedEventType)
    assert(sqsMessage.type == "Notification")
    assert(sqsMessage.messageId == "1a2345bc-de67-890f-1g01-11h21314h151")
    assert(sqsMessage.message.occurredAt == DateTimeFormatter.ISO_INSTANT.format(currentTime))
  }
}
