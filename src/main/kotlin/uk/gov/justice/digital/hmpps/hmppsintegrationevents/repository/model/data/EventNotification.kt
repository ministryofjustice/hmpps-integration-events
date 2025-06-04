package uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import java.time.LocalDateTime

@Entity
@Table(
  name = "EVENT_NOTIFICATION",
  indexes = [
    Index(name = "idx_event_notification_url_event_type", columnList = "url, event_type", unique = true)
  ]
)
data class EventNotification(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "EVENT_ID", nullable = false, unique = true)
  val eventId: Long? = null,

  @Column(name = "HMPPS_ID", nullable = true)
  val hmppsId: String? = null,

  @Enumerated(EnumType.STRING)
  @Column(name = "EVENT_TYPE", nullable = false)
  val eventType: IntegrationEventType,

  @Column(name = "PRISON_ID", nullable = true)
  val prisonId: String? = null,

  @Column(name = "URL", nullable = false)
  val url: String,

  @Temporal(value = TemporalType.TIMESTAMP)
  @Column(name = "LAST_MODIFIED_DATETIME", nullable = false)
  val lastModifiedDateTime: LocalDateTime,
)
