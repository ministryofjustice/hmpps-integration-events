package uk.gov.justice.digital.hmpps.hmppsintegrationevents.listeners

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.mockito.Mockito
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.utils.SqsMessage
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.DomainEventsService

class HmppsDomainEventsListenerTest {
  val mockDomainEventsService: DomainEventsService = Mockito.mock(DomainEventsService::class.java)
  val hmppsDomainEventsListener: HmppsDomainEventsListener = HmppsDomainEventsListener(domainEventsService = mockDomainEventsService, objectMapper = ObjectMapper())
  @Test
  fun `when a sqs event is received it should call the domainEventService`() {
    val rawMessage = """
      {
       "Type" : "Notification",
       "MessageId" : "1a2345bc-de67-890f-1g01-11h21314h151",
       "TopicArn" : "arn:aws:sns:eu-west-2:123456789123:cloud-platform-Digital-Prison-Services-12a3456bc12345a1a12345ab3c123b12",
       "Message" : "{\"eventType\":\"probation-case.registration.added\",\"version\":1,\"occurredAt\":\"2023-03-25T10:35:38.285Z\",\"description\":\"A new registration has been added to the probation case\",\"personReference\":{\"identifiers\":[{\"type\":\"CRN\",\"value\":\"X777776\"}]},\"additionalInformation\":{\"registrationLevelDescription\":\"MAPPA Level 3\",\"registerTypeDescription\":\"MAPPA\",\"registrationCategoryCode\":\"M1\",\"registrationId\":\"1234567890\",\"registrationDate\":\"Fri Mar 22 00:00:00 GMT 2024\",\"registerTypeCode\":\"MAPP\",\"createdDateAndTime\":\"Mon Mar 25 10:45:38 GMT 2024\",\"registrationCategoryDescription\":\"MAPPA Cat 1\",\"registrationLevelCode\":\"M3\"}}",
       "Timestamp" : "2023-03-25T10:35:38.403Z",
       "SignatureVersion" : "1",
       "Signature" : "A5cK8hNQj+3PTeLwS9D4bFYUvz1aI6cGvT2XWmRkE+7MoJZniFQjDeo2tI3BwKhLI9RXznGvZD1KbOoEMp4kUqBnA2Wr3bH5D6NfYhJKYV8sGpVcWzUMlTgbxErC1L7RgM2o7bHWlCKqfzOiDRsLpPeuJElmuTcYJtn+Lxv3R4TyYndafQSoVpErHcRJwWjDSf5l6jrKVa4m0NQbgtyZlxVPcHYf+wpWcuJ1BvLSnDHM+egRw7GqpxDB+IU1kEolC3eL4RUjXGmkQ9YtczF2P1JrThsRi0oEgnp7f/VtB6oqOKkYvlMXaF/+uhdwZbWlDa/83HcYV9MkJpWpx8UeisflEMNt57hbJXIgGQvsTYo4cRn9VgWQJxb6yDw8zS+I6yuGNbcL5L2Aj+whsEM1ovcR7KBPvgsMlxtCyVYzj36sDa4nUfiR5iZkVDeT==",
       "SigningCertURL" : "https://sns.eu-west-2.amazonaws.com/SimpleNotificationService-12abcd123456d12b1e23a123456ef123.pem",
       "UnsubscribeURL" : "https://sns.eu-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-2:123456789123:cloud-platform-Digital-Prison-Services-12a3456bc12345a1a12345ab3c123b12:123b456a-123f-1234-a123-123456789d10",
       "MessageAttributes" : {
         "eventType" : {"Type":"String","Value":"probation-case.registration.added"},
         "id" : {"Type":"String","Value":"12345678-a1af-a0ba-1b22-d12e12d1234f"},
         "timestamp" : {"Type":"Number.java.lang.Long","Value":"1711363538399"}
       }
     }
    `"""
    val sqsMessage = SqsMessage(
      Type ="Notification",
      Message = "{\"eventType\":\"probation-case.registration.added\",\"version\":1,\"occurredAt\":\"2023-03-25T10:35:38.285Z\",\"description\":\"A new registration has been added to the probation case\",\"personReference\":{\"identifiers\":[{\"type\":\"CRN\",\"value\":\"X777776\"}]},\"additionalInformation\":{\"registrationLevelDescription\":\"MAPPA Level 3\",\"registerTypeDescription\":\"MAPPA\",\"registrationCategoryCode\":\"M1\",\"registrationId\":\"1234567890\",\"registrationDate\":\"Fri Mar 22 00:00:00 GMT 2024\",\"registerTypeCode\":\"MAPP\",\"createdDateAndTime\":\"Mon Mar 25 10:45:38 GMT 2024\",\"registrationCategoryDescription\":\"MAPPA Cat 1\",\"registrationLevelCode\":\"M3\"}}",
      MessageId = "1a2345bc-de67-890f-1g01-11h21314h151"
    )

    hmppsDomainEventsListener.onDomainEvent(rawMessage)

    verify(mockDomainEventsService, times(1)).execute(sqsMessage)
  }

  @Test
  fun `Throws exception when the json object is not deserialisable`() {
    val rawMessage = """
      {
       "Type" : ,
       "MessageId" : "1a2345bc-de67-890f-1g01-11h21314h151",
       "TopicArn" : "arn:aws:sns:eu-west-2:123456789123:cloud-platform-Digital-Prison-Services-12a3456bc12345a1a12345ab3c123b12",
       "Message" : "This won't be able to deserialise into an object.",
       "Timestamp" : "2023-03-25T10:35:38.403Z",
       "SignatureVersion" : "1",
       "Signature" : "A5cK8hNQj+3PTeLwS9D4bFYUvz1aI6cGvT2XWmRkE+7MoJZniFQjDeo2tI3BwKhLI9RXznGvZD1KbOoEMp4kUqBnA2Wr3bH5D6NfYhJKYV8sGpVcWzUMlTgbxErC1L7RgM2o7bHWlCKqfzOiDRsLpPeuJElmuTcYJtn+Lxv3R4TyYndafQSoVpErHcRJwWjDSf5l6jrKVa4m0NQbgtyZlxVPcHYf+wpWcuJ1BvLSnDHM+egRw7GqpxDB+IU1kEolC3eL4RUjXGmkQ9YtczF2P1JrThsRi0oEgnp7f/VtB6oqOKkYvlMXaF/+uhdwZbWlDa/83HcYV9MkJpWpx8UeisflEMNt57hbJXIgGQvsTYo4cRn9VgWQJxb6yDw8zS+I6yuGNbcL5L2Aj+whsEM1ovcR7KBPvgsMlxtCyVYzj36sDa4nUfiR5iZkVDeT==",
       "SigningCertURL" : "https://sns.eu-west-2.amazonaws.com/SimpleNotificationService-12abcd123456d12b1e23a123456ef123.pem",
       "UnsubscribeURL" : "https://sns.eu-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-2:123456789123:cloud-platform-Digital-Prison-Services-12a3456bc12345a1a12345ab3c123b12:123b456a-123f-1234-a123-123456789d10",
       "MessageAttributes" : {
         "eventType" : {"Type":"String","Value":"probation-case.registration.added"},
         "id" : {"Type":"String","Value":"12345678-a1af-a0ba-1b22-d12e12d1234f"},
         "timestamp" : {"Type":"Number.java.lang.Long","Value":"1711363538399"}
       }
     }
    `"""


    val executable = Executable { hmppsDomainEventsListener.onDomainEvent(rawMessage)}

    val e = assertThrows(
      JsonParseException::class.java, executable
    )
  }


}