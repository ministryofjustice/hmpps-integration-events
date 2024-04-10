package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class SqsNotificationGeneratingHelper {

  private val readableTimestampPatten: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy")
  fun generate(
    eventTypeValue: String = "probation-case.registration.added",
    timestamp: ZonedDateTime = LocalDateTime.now().atZone(ZoneId.systemDefault()),
  ): String {
    val isoInstantTimestamp = DateTimeFormatter.ISO_INSTANT.format(timestamp)
    val readableTimestamp = readableTimestampPatten.format(timestamp)
    val millis: Long = timestamp.toInstant().toEpochMilli()

    return """
    {
     "Type" : "Notification",
     "MessageId" : "1a2345bc-de67-890f-1g01-11h21314h151",
     "TopicArn" : "arn:aws:sns:eu-west-2:123456789123:cloud-platform-Digital-Prison-Services-12a3456bc12345a1a12345ab3c123b12",
     "Message" : "{\"eventType\":\"$eventTypeValue\",\"version\":1,\"occurredAt\":\"$isoInstantTimestamp\",\"description\":\"A new registration has been added to the probation case\",\"personReference\":{\"identifiers\":[{\"type\":\"CRN\",\"value\":\"X777776\"}]},\"additionalInformation\":{\"registrationLevelDescription\":\"MAPPA Level 3\",\"registerTypeDescription\":\"MAPPA\",\"registrationCategoryCode\":\"M1\",\"registrationId\":\"1234567890\",\"registrationDate\":\"$readableTimestamp\",\"registerTypeCode\":\"MAPP\",\"createdDateAndTime\":\"$readableTimestamp\",\"registrationCategoryDescription\":\"MAPPA Cat 1\",\"registrationLevelCode\":\"M3\"}}",
     "Timestamp" : "$isoInstantTimestamp",
     "SignatureVersion" : "1",
     "Signature" : "A5cK8hNQj+3PTeLwS9D4bFYUvz1aI6cGvT2XWmRkE+7MoJZniFQjDeo2tI3BwKhLI9RXznGvZD1KbOoEMp4kUqBnA2Wr3bH5D6NfYhJKYV8sGpVcWzUMlTgbxErC1L7RgM2o7bHWlCKqfzOiDRsLpPeuJElmuTcYJtn+Lxv3R4TyYndafQSoVpErHcRJwWjDSf5l6jrKVa4m0NQbgtyZlxVPcHYf+wpWcuJ1BvLSnDHM+egRw7GqpxDB+IU1kEolC3eL4RUjXGmkQ9YtczF2P1JrThsRi0oEgnp7f/VtB6oqOKkYvlMXaF/+uhdwZbWlDa/83HcYV9MkJpWpx8UeisflEMNt57hbJXIgGQvsTYo4cRn9VgWQJxb6yDw8zS+I6yuGNbcL5L2Aj+whsEM1ovcR7KBPvgsMlxtCyVYzj36sDa4nUfiR5iZkVDeT==",
     "SigningCertURL" : "https://sns.eu-west-2.amazonaws.com/SimpleNotificationService-12abcd123456d12b1e23a123456ef123.pem",
     "UnsubscribeURL" : "https://sns.eu-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-2:123456789123:cloud-platform-Digital-Prison-Services-12a3456bc12345a1a12345ab3c123b12:123b456a-123f-1234-a123-123456789d10",
     "MessageAttributes" : {
       "eventType" : {"Type":"String","Value":"$eventTypeValue"},
       "id" : {"Type":"String","Value":"12345678-a1af-a0ba-1b22-d12e12d1234f"},
       "timestamp" : {"Type":"Number.java.lang.Long","Value":"$millis"}
     }
    }
    `"""
  }
}
