package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models

import software.amazon.awssdk.services.sqs.model.Message

data class ReceiveMessageResult(
  val messages: List<Message.Builder>,
)
