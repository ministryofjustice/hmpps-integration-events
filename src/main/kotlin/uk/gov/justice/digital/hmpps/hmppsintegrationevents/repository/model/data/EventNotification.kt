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
  val event_id: Long,

  @Column(name = "HMPPS_ID", nullable = false)
  val hmpps_id: String,

  @Column(name = "EVENT_TYPE", nullable = false)
  val event_type: EventType,

  @Column(name = "URL", nullable = false)
  val url: String,

  @Column(name = "LAST_MODIFIED_DATETIME", nullable = false)
  val last_modified_date_time: LocalDateTime

)