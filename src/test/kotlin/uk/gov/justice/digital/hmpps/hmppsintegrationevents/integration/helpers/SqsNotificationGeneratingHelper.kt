package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers

import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.DomainEventMessageAttributes
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.EventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class SqsNotificationGeneratingHelper(timestamp: ZonedDateTime = LocalDateTime.now().atZone(ZoneId.systemDefault())) {

  private val readableTimestampPatten: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy")
  private val isoInstantTimestamp = DateTimeFormatter.ISO_INSTANT.format(timestamp)
  private val readableTimestamp = readableTimestampPatten.format(timestamp)
  private val millis: Long = timestamp.toInstant().toEpochMilli()

  fun generateRawGenericEvent(
    eventTypeValue: String = "probation-case.registration.added",
  ): String = (
    """
    {
     "Type" : "Notification",
     "MessageId" : "1a2345bc-de67-890f-1g01-11h21314h151",
     "Message" : "{\"eventType\":\"$eventTypeValue\",\"version\":1,\"occurredAt\":\"$isoInstantTimestamp\",\"description\":\"A new registration has been added to the probation case\",\"personReference\":{\"identifiers\":[{\"type\":\"CRN\",\"value\":\"X777776\"}]},\"additionalInformation\":{\"registrationLevelDescription\":\"MAPPA Level 3\",\"registerTypeDescription\":\"MAPPA\",\"registrationCategoryCode\":\"M1\",\"registrationId\":\"1234567890\",\"registrationDate\":\"$readableTimestamp\",\"registerTypeCode\":\"MAPP\",\"createdDateAndTime\":\"$readableTimestamp\",\"registrationCategoryDescription\":\"MAPPA Cat 1\",\"registrationLevelCode\":\"M3\"}}",
     "Timestamp" : "$isoInstantTimestamp",
     "MessageAttributes" : {
       "eventType" : {"Type":"String","Value":"$eventTypeValue"},
       "id" : {"Type":"String","Value":"12345678-a1af-a0ba-1b22-d12e12d1234f"},
       "timestamp" : {"Type":"Number.java.lang.Long","Value":"$millis"}
     }
    }
    """
    )

  fun generatePrisonerReleasedEvent(
    eventTypeValue: String = "prison-offender-events.prisoner.released",
    reason: String = "RELEASED",
  ): String = (
    """
    {
     "Type" : "Notification",
     "MessageId" : "1a2345bc-de67-890f-1g01-11h21314h151",
     "Message" : "{\"eventType\":\"$eventTypeValue\",\"version\":1,\"reason\":\"$reason\",\"occurredAt\":\"$isoInstantTimestamp\",\"description\":\"A new registration has been added to the probation case\",\"personReference\":{\"identifiers\":[{\"type\":\"nomsNumber\",\"value\":\"mockNomsNumber\"}]},\"additionalInformation\":{\"registrationLevelDescription\":\"MAPPA Level 3\",\"registerTypeDescription\":\"MAPPA\",\"registrationCategoryCode\":\"M1\",\"registrationId\":\"1234567890\",\"registrationDate\":\"$readableTimestamp\",\"createdDateAndTime\":\"$readableTimestamp\",\"registrationCategoryDescription\":\"MAPPA Cat 1\",\"registrationLevelCode\":\"M3\"}}",
     "Timestamp" : "$isoInstantTimestamp",
     "MessageAttributes" : {
       "eventType" : {"Type":"String","Value":"$eventTypeValue"},
       "id" : {"Type":"String","Value":"12345678-a1af-a0ba-1b22-d12e12d1234f"},
       "timestamp" : {"Type":"Number.java.lang.Long","Value":"$millis"}
     }
    }
    """
    )

  fun generateRawHmppsDomainEvent(
    eventType: String = "probation-case.registration.added",
    registerTypeCode: String = "MAPP",
    identifiers: String = "[{\\\"type\\\":\\\"CRN\\\",\\\"value\\\":\\\"X777776\\\"},{\\\"type\\\":\\\"NOMS\\\",\\\"value\\\":\\\"A1234BC\\\"}]",
    messageEventType: String = eventType,
  ): String = (
    """
    {
     "Type" : "Notification",
     "MessageId" : "1a2345bc-de67-890f-1g01-11h21314h151",
     "TopicArn" : "arn:aws:sns:eu-west-2:123456789123:cloud-platform-Digital-Prison-Services-12a3456bc12345a1a12345ab3c123b12",
     "Message" : "{\"eventType\":\"$messageEventType\",\"version\":1,\"occurredAt\":\"$isoInstantTimestamp\",\"description\":\"A new registration has been added to the probation case\",\"personReference\":{\"identifiers\":$identifiers},\"additionalInformation\":{\"registrationLevelDescription\":\"MAPPA Level 3\",\"registerTypeDescription\":\"MAPPA\",\"registrationCategoryCode\":\"M1\",\"registrationId\":\"1234567890\",\"registrationDate\":\"$readableTimestamp\",\"registerTypeCode\":\"$registerTypeCode\",\"createdDateAndTime\":\"$readableTimestamp\",\"registrationCategoryDescription\":\"MAPPA Cat 1\",\"registrationLevelCode\":\"M3\"}}",
     "Timestamp" : "$isoInstantTimestamp",
     "SignatureVersion" : "1",
     "Signature" : "A5cK8hNQj+3PTeLwS9D4bFYUvz1aI6cGvT2XWmRkE+7MoJZniFQjDeo2tI3BwKhLI9RXznGvZD1KbOoEMp4kUqBnA2Wr3bH5D6NfYhJKYV8sGpVcWzUMlTgbxErC1L7RgM2o7bHWlCKqfzOiDRsLpPeuJElmuTcYJtn+Lxv3R4TyYndafQSoVpErHcRJwWjDSf5l6jrKVa4m0NQbgtyZlxVPcHYf+wpWcuJ1BvLSnDHM+egRw7GqpxDB+IU1kEolC3eL4RUjXGmkQ9YtczF2P1JrThsRi0oEgnp7f/VtB6oqOKkYvlMXaF/+uhdwZbWlDa/83HcYV9MkJpWpx8UeisflEMNt57hbJXIgGQvsTYo4cRn9VgWQJxb6yDw8zS+I6yuGNbcL5L2Aj+whsEM1ovcR7KBPvgsMlxtCyVYzj36sDa4nUfiR5iZkVDeT==",
     "SigningCertURL" : "https://sns.eu-west-2.amazonaws.com/SimpleNotificationService-12abcd123456d12b1e23a123456ef123.pem",
     "UnsubscribeURL" : "https://sns.eu-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-2:123456789123:cloud-platform-Digital-Prison-Services-12a3456bc12345a1a12345ab3c123b12:123b456a-123f-1234-a123-123456789d10",
     "MessageAttributes" : {
       "eventType" : {"Type":"String","Value":"$eventType"},
       "id" : {"Type":"String","Value":"12345678-a1af-a0ba-1b22-d12e12d1234f"},
       "timestamp" : {"Type":"Number.java.lang.Long","Value":"$millis"}
     }
    }
    """
    )

  fun generateRawHmppsDomainEventWithoutRegisterType(
    eventType: String,
    identifiers: String = "[{\\\"type\\\":\\\"CRN\\\",\\\"value\\\":\\\"X777776\\\"}]",
    messageEventType: String = eventType,
  ): String = (
    """
    {
     "Type" : "Notification",
     "MessageId" : "1a2345bc-de67-890f-1g01-11h21314h151",
     "TopicArn" : "arn:aws:sns:eu-west-2:123456789123:cloud-platform-Digital-Prison-Services-12a3456bc12345a1a12345ab3c123b12",
     "Message" : "{\"eventType\":\"$messageEventType\",\"version\":1,\"occurredAt\":\"$isoInstantTimestamp\",\"description\":\"A new registration has been added to the probation case\",\"personReference\":{\"identifiers\":$identifiers},\"additionalInformation\":{\"registrationLevelDescription\":\"MAPPA Level 3\",\"registerTypeDescription\":\"MAPPA\",\"registrationCategoryCode\":\"M1\",\"registrationId\":\"1234567890\",\"registrationDate\":\"$readableTimestamp\",\"createdDateAndTime\":\"$readableTimestamp\",\"registrationCategoryDescription\":\"MAPPA Cat 1\",\"registrationLevelCode\":\"M3\"}}",
     "Timestamp" : "$isoInstantTimestamp",
     "SignatureVersion" : "1",
     "Signature" : "A5cK8hNQj+3PTeLwS9D4bFYUvz1aI6cGvT2XWmRkE+7MoJZniFQjDeo2tI3BwKhLI9RXznGvZD1KbOoEMp4kUqBnA2Wr3bH5D6NfYhJKYV8sGpVcWzUMlTgbxErC1L7RgM2o7bHWlCKqfzOiDRsLpPeuJElmuTcYJtn+Lxv3R4TyYndafQSoVpErHcRJwWjDSf5l6jrKVa4m0NQbgtyZlxVPcHYf+wpWcuJ1BvLSnDHM+egRw7GqpxDB+IU1kEolC3eL4RUjXGmkQ9YtczF2P1JrThsRi0oEgnp7f/VtB6oqOKkYvlMXaF/+uhdwZbWlDa/83HcYV9MkJpWpx8UeisflEMNt57hbJXIgGQvsTYo4cRn9VgWQJxb6yDw8zS+I6yuGNbcL5L2Aj+whsEM1ovcR7KBPvgsMlxtCyVYzj36sDa4nUfiR5iZkVDeT==",
     "SigningCertURL" : "https://sns.eu-west-2.amazonaws.com/SimpleNotificationService-12abcd123456d12b1e23a123456ef123.pem",
     "UnsubscribeURL" : "https://sns.eu-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-2:123456789123:cloud-platform-Digital-Prison-Services-12a3456bc12345a1a12345ab3c123b12:123b456a-123f-1234-a123-123456789d10",
     "MessageAttributes" : {
       "eventType" : {"Type":"String","Value":"$eventType"},
       "id" : {"Type":"String","Value":"12345678-a1af-a0ba-1b22-d12e12d1234f"},
       "timestamp" : {"Type":"Number.java.lang.Long","Value":"$millis"}
     }
    }
    """
    )

  fun generateRawHmppsDomainEventWithAlertCode(
    eventType: String,
    identifiers: String = "[{\\\"type\\\":\\\"CRN\\\",\\\"value\\\":\\\"X777776\\\"}]",
    messageEventType: String = eventType,
    alertCode: String,
  ): String = (
    """
    {
     "Type" : "Notification",
     "MessageId" : "1a2345bc-de67-890f-1g01-11h21314h151",
     "TopicArn" : "arn:aws:sns:eu-west-2:123456789123:cloud-platform-Digital-Prison-Services-12a3456bc12345a1a12345ab3c123b12",
     "Message" : "{\"eventType\":\"$messageEventType\",\"version\":1,\"occurredAt\":\"$isoInstantTimestamp\",\"description\":\"A new registration has been added to the probation case\",\"personReference\":{\"identifiers\":$identifiers},\"additionalInformation\":{\"registrationLevelDescription\":\"MAPPA Level 3\",\"registerTypeDescription\":\"MAPPA\",\"registrationCategoryCode\":\"M1\",\"alertCode\":\"$alertCode\",\"registrationId\":\"1234567890\",\"registrationDate\":\"$readableTimestamp\",\"createdDateAndTime\":\"$readableTimestamp\",\"registrationCategoryDescription\":\"MAPPA Cat 1\",\"registrationLevelCode\":\"M3\"}}",
     "Timestamp" : "$isoInstantTimestamp",
     "SignatureVersion" : "1",
     "Signature" : "A5cK8hNQj+3PTeLwS9D4bFYUvz1aI6cGvT2XWmRkE+7MoJZniFQjDeo2tI3BwKhLI9RXznGvZD1KbOoEMp4kUqBnA2Wr3bH5D6NfYhJKYV8sGpVcWzUMlTgbxErC1L7RgM2o7bHWlCKqfzOiDRsLpPeuJElmuTcYJtn+Lxv3R4TyYndafQSoVpErHcRJwWjDSf5l6jrKVa4m0NQbgtyZlxVPcHYf+wpWcuJ1BvLSnDHM+egRw7GqpxDB+IU1kEolC3eL4RUjXGmkQ9YtczF2P1JrThsRi0oEgnp7f/VtB6oqOKkYvlMXaF/+uhdwZbWlDa/83HcYV9MkJpWpx8UeisflEMNt57hbJXIgGQvsTYo4cRn9VgWQJxb6yDw8zS+I6yuGNbcL5L2Aj+whsEM1ovcR7KBPvgsMlxtCyVYzj36sDa4nUfiR5iZkVDeT==",
     "SigningCertURL" : "https://sns.eu-west-2.amazonaws.com/SimpleNotificationService-12abcd123456d12b1e23a123456ef123.pem",
     "UnsubscribeURL" : "https://sns.eu-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-2:123456789123:cloud-platform-Digital-Prison-Services-12a3456bc12345a1a12345ab3c123b12:123b456a-123f-1234-a123-123456789d10",
     "MessageAttributes" : {
       "eventType" : {"Type":"String","Value":"$eventType"},
       "id" : {"Type":"String","Value":"12345678-a1af-a0ba-1b22-d12e12d1234f"},
       "timestamp" : {"Type":"Number.java.lang.Long","Value":"$millis"}
     }
    }
    """
    )

  fun generateRawDomainEvent(eventType: String, message: String) = """
    {
      "Type" : "Notification",
      "MessageId" : "d4419bdd-2079-598c-b608-c4f2ddb1bcd1",
      "TopicArn" : "arn:aws:sns:eu-west-2:754256621582:cloud-platform-Digital-Prison-Services-97e6567cf80881a8a52290ff2c269b08",
      "Message" : "${message.trimIndent().replace("\n", "").replace("\"", "\\\"")}",
      "Timestamp" : "2024-08-09T11:20:40.320Z",
      "SignatureVersion" : "1",
      "Signature" : "IMtmzxSgFYKD4fljhMOGSLVPyt0eCduKLN9Y8j9Zr3dbWHgjL9lM4qaMbLo/XPOdz8Cya2N50KGkFf4pAmp8yGAGM56gkJHQFCcIbdHGkW9w86woxjvHb0kh13BAiv7JWwrAvTIgJgPqtph6RCQY385eqGk4jU7JmPvtU+YeZoSv657Qa4LP6DPNjvdmnOYfrXnt+BVyzpVHBlWLnBi9dv+WMnRBxZ36IhppjTQw+hAnlU1yg98r93GRH43d2PLiINlIkyMP7TXH7rYX1RwPCceC9VAeXNJdzCLTteUDCI4trwKloZLYfqpZXgWRzhyB/ZaBJz/wmjA7iKBvtbIdUA==",
      "SigningCertURL" : "https://sns.eu-west-2.amazonaws.com/SimpleNotificationService-60eadc530605d63b8e62a523676ef735.pem",
      "UnsubscribeURL" : "https://sns.eu-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-2:754256621582:cloud-platform-Digital-Prison-Services-97e6567cf80881a8a52290ff2c269b08:340b799a-084f-4027-a214-510087556d97",
      "MessageAttributes" : {
        "traceparent" : {"Type":"String","Value":"00-e46c152a1097400c9c5e8f9b53b26ca5-e1a16aff9e932bba-01"},
        "eventType" : {"Type":"String","Value":"$eventType"},
        "id" : {"Type":"String","Value":"51c928a9-4d16-5e97-1674-02ff2a616177"},
        "timestamp" : {"Type":"Number.java.lang.Long","Value":"1723202440316"}
      }
    }    
  """.trimIndent()

  fun createHmppsDomainEvent(
    eventType: String = "probation-case.registration.added",
    registerTypeCode: String = "MAPP",
    identifiers: String = "[{\"type\":\"CRN\",\"value\":\"X777776\"},{\"type\":\"NOMS\",\"value\":\"A1234BC\"}]",
    attributeEventTypes: String = eventType,
  ): HmppsDomainEvent = (
    HmppsDomainEvent(
      type = "Notification",
      message = "{\"eventType\":\"$eventType\",\"version\":1,\"occurredAt\":\"$isoInstantTimestamp\",\"description\":\"A new registration has been added to the probation case\",\"personReference\":{\"identifiers\":$identifiers},\"additionalInformation\":{\"registrationLevelDescription\":\"MAPPA Level 3\",\"registerTypeDescription\":\"MAPPA\",\"registrationCategoryCode\":\"M1\",\"registrationId\":\"1234567890\",\"registrationDate\":\"$readableTimestamp\",\"registerTypeCode\":\"$registerTypeCode\",\"createdDateAndTime\":\"$readableTimestamp\",\"registrationCategoryDescription\":\"MAPPA Cat 1\",\"registrationLevelCode\":\"M3\"}}",
      messageId = "1a2345bc-de67-890f-1g01-11h21314h151",
      messageAttributes = DomainEventMessageAttributes(eventType = EventType(value = attributeEventTypes)),
    )
    )

  fun createHmppsDomainEventWithPrisonId(
    eventType: String = "probation-case.registration.added",
    registerTypeCode: String = "MAPP",
    identifiers: String = "[{\"type\":\"CRN\",\"value\":\"X777776\"}]",
    attributeEventTypes: String = eventType,
    prisonId: String = "MDI",
  ): HmppsDomainEvent = HmppsDomainEvent(
    type = "Notification",
    message = "{\"eventType\":\"$eventType\",\"version\":1,\"occurredAt\":\"$isoInstantTimestamp\",\"description\":\"A new registration has been added to the probation case\",\"prisonId\":\"$prisonId\",\"personReference\":{\"identifiers\":$identifiers},\"additionalInformation\":{\"registrationLevelDescription\":\"MAPPA Level 3\",\"registerTypeDescription\":\"MAPPA\",\"registrationCategoryCode\":\"M1\",\"registrationId\":\"1234567890\",\"registrationDate\":\"$readableTimestamp\",\"registerTypeCode\":\"$registerTypeCode\",\"createdDateAndTime\":\"$readableTimestamp\",\"registrationCategoryDescription\":\"MAPPA Cat 1\",\"registrationLevelCode\":\"M3\"}}",
    messageId = "1a2345bc-de67-890f-1g01-11h21314h151",
    messageAttributes = DomainEventMessageAttributes(eventType = EventType(value = attributeEventTypes)),
  )

  fun createHmppsDomainEventWithoutRegisterType(
    eventType: String,
    identifiers: String = "[{\"type\":\"CRN\",\"value\":\"X777776\"}]",
    attributeEventTypes: String = eventType,
  ): HmppsDomainEvent = (
    HmppsDomainEvent(
      type = "Notification",
      message = "{\"eventType\":\"$eventType\",\"version\":1,\"occurredAt\":\"$isoInstantTimestamp\",\"description\":\"A new registration has been added to the probation case\",\"personReference\":{\"identifiers\":$identifiers},\"additionalInformation\":{\"registrationLevelDescription\":\"MAPPA Level 3\",\"registerTypeDescription\":\"MAPPA\",\"registrationCategoryCode\":\"M1\",\"registrationId\":\"1234567890\",\"registrationDate\":\"$readableTimestamp\",\"createdDateAndTime\":\"$readableTimestamp\",\"registrationCategoryDescription\":\"MAPPA Cat 1\",\"registrationLevelCode\":\"M3\"}}",
      messageId = "1a2345bc-de67-890f-1g01-11h21314h151",
      messageAttributes = DomainEventMessageAttributes(eventType = EventType(value = attributeEventTypes)),
    )
    )

  fun createHmppsDomainEventWithAlertCode(
    eventType: String,
    identifiers: String = "[{\"type\":\"CRN\",\"value\":\"X777776\"}]",
    attributeEventTypes: String = eventType,
    alertCode: String,
  ): HmppsDomainEvent = (
    HmppsDomainEvent(
      type = "Notification",
      message = "{\"eventType\":\"$eventType\",\"version\":1,\"occurredAt\":\"$isoInstantTimestamp\",\"description\":\"A new registration has been added to the probation case\",\"personReference\":{\"identifiers\":$identifiers},\"additionalInformation\":{\"registrationLevelDescription\":\"MAPPA Level 3\",\"registerTypeDescription\":\"MAPPA\",\"registrationCategoryCode\":\"M1\",\"alertCode\":\"$alertCode\",\"registrationId\":\"1234567890\",\"registrationDate\":\"$readableTimestamp\",\"createdDateAndTime\":\"$readableTimestamp\",\"registrationCategoryDescription\":\"MAPPA Cat 1\",\"registrationLevelCode\":\"M3\"}}",
      messageId = "1a2345bc-de67-890f-1g01-11h21314h151",
      messageAttributes = DomainEventMessageAttributes(eventType = EventType(value = attributeEventTypes)),
    )
    )

  fun createHmppsDomainEventWithReason(
    eventType: String = "probation-case.registration.added",
    registerTypeCode: String = "MAPP",
    identifiers: String = "[{\"type\":\"CRN\",\"value\":\"X777776\"}]",
    attributeEventTypes: String = eventType,
    reason: String = "RELEASED",
  ): HmppsDomainEvent = (
    HmppsDomainEvent(
      type = "Notification",
      message = "{\"eventType\":\"$eventType\",\"version\":1,\"occurredAt\":\"$isoInstantTimestamp\",\"reason\":\"$reason\",\"description\":\"A new registration has been added to the probation case\",\"personReference\":{\"identifiers\":$identifiers},\"additionalInformation\":{\"registrationLevelDescription\":\"MAPPA Level 3\",\"registerTypeDescription\":\"MAPPA\",\"registrationCategoryCode\":\"M1\",\"registrationId\":\"1234567890\",\"registrationDate\":\"$readableTimestamp\",\"registerTypeCode\":\"$registerTypeCode\",\"createdDateAndTime\":\"$readableTimestamp\",\"registrationCategoryDescription\":\"MAPPA Cat 1\",\"registrationLevelCode\":\"M3\"}}",
      messageId = "1a2345bc-de67-890f-1g01-11h21314h151",
      messageAttributes = DomainEventMessageAttributes(eventType = EventType(value = attributeEventTypes)),
    )
    )
}
