package uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.OutgoingEventType
import java.time.LocalDateTime

@Entity
@Table(name = "EVENT_NOTIFICATION")
data class EventNotification(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "EVENT_ID", nullable = false, unique = true)
  val eventId: Long? = null,

  @Column(name = "HMPPS_ID", nullable = false)
  val hmppsId: String,

  @Enumerated(EnumType.STRING)
  @Column(name = "EVENT_TYPE", nullable = false)
  val eventType: OutgoingEventType,

  @Column(name = "URL", nullable = false)
  val url: String,

  @Temporal(value = TemporalType.TIMESTAMP)
  @Column(name = "LAST_MODIFIED_DATETIME", nullable = false)
  val lastModifiedDateTime: LocalDateTime,
)
