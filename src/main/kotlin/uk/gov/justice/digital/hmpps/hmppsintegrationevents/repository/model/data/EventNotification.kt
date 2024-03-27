package uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.enum.EventType
import java.time.LocalDateTime

@Entity
class EventNotification (

  @Id
  @Column(name = "EVENT_ID", nullable = false, unique = true)
  val eventId: Long,

  @Column(name = "HMPPS_ID", nullable = false)
  val hmppsId: String,

  @Column(name = "EVENT_TYPE", nullable = false)
  val eventType: EventType,

  @Column(name = "URL", nullable = false)
  val url: String,

  @Column(name = "LAST_MODIFIED_DATETIME", nullable = false)
  val lastModifiedDateTime: LocalDateTime

)