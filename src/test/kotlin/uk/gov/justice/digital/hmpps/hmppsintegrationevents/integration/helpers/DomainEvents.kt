package uk.gov.justice.digital.hmpps.hmppsintegrationevents.integration.helpers

import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.DomainEventMessageAttributes
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.EventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEvent

object DomainEvents {

  val PROBATION_CASE_ENGAGEMENT_CREATED_MESSAGE = """
    {\"eventType\":\"probation-case.engagement.created\",\"version\":1,\"detailUrl\":\"https://domain-events-and-delius.hmpps.service.justice.gov.uk/probation-case.engagement.created/X777776\",\"occurredAt\":\"2024-08-09T12:20:40.282+01:00\",\"description\":\"A probation case record for a person has been created in Delius\",\"additionalInformation\":{},\"personReference\":{\"identifiers\":[{\"type\":\"CRN\",\"value\":\"X777776\"}]}}
  """.trimIndent()

  val PERSON_HMPPS_ID_MESSAGE = "{\"eventType\":\"probation-case.engagement.created\",\"version\":1,\"detailUrl\":\"https://domain-events-and-delius.hmpps.service.justice.gov.uk/probation-case.engagement.created/X777776\",\"occurredAt\":\"2024-08-09T12:20:40.282+01:00\",\"description\":\"A probation case record for a person has been created in Delius\",\"additionalInformation\":{},\"personReference\":{\"identifiers\":[{\"type\":\"CRN\",\"value\":\"X777776\"}]}}"

  fun generateDomainEvent(eventType: String, message: String) = """
    {
      "Type" : "Notification",
      "MessageId" : "d4419bdd-2079-598c-b608-c4f2ddb1bcd1",
      "TopicArn" : "arn:aws:sns:eu-west-2:754256621582:cloud-platform-Digital-Prison-Services-97e6567cf80881a8a52290ff2c269b08",
      "Message" : "$message",
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

  fun generateHmppsDomainEvent(eventType: String, message: String) =
    HmppsDomainEvent(
      type = "Notification",
      message = message,
      messageId = "d4419bdd-2079-598c-b608-c4f2ddb1bcd1",
      messageAttributes = DomainEventMessageAttributes(EventType(eventType)),
    )
}
