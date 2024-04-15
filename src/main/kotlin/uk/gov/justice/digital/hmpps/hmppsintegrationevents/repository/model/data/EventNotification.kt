package uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.EventTypeValue

@Entity
@Table(name = "EVENT_NOTIFICATION")
class EventNotification(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "EVENT_ID", nullable = false, unique = true)
  val eventId: Long? = null,

  @Column(name = "HMPPS_ID", nullable = false)
  val hmppsId: String,

  @Enumerated(EnumType.STRING)
  @Column(name = "EVENT_TYPE", nullable = false)
  val eventType: EventTypeValue,

  @Column(name = "URL", nullable = false)
  val url: String,

  @Column(name = "LAST_MODIFIED_DATETIME", nullable = false)
  val lastModifiedDateTime: String,
) {
  override fun equals(other: Any?): Boolean {
    if (other == null) return false
    if (this === other) return true
    if (other !is EventNotification) return false
    if (eventId != other.eventId ||
      hmppsId != other.hmppsId ||
      eventType != other.eventType ||
      url != other.url ||
      lastModifiedDateTime != other.lastModifiedDateTime
    ) {
      return false
    }
    return true
  }

}
