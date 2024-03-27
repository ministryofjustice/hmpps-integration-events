package uk.gov.justice.digital.hmpps.hmppsintegrationevents.data

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.LastModifiedDate
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.enum.EventType
import java.time.LocalDateTime

@Entity
@Table(name = "EVENT_NOTIFICATION")
data class EventNotification(
  @Id @Column(name="EVENT_ID") val id: Long,
  @Column(name="HMPPS_ID")val hmppsId: String,
  @Column(name="EVENT_TYPE")val eventType: EventType,
  @Column(name="URL")val url: String,
  @Column(name="LAST_MODIFIED_DATETIME")  val lastModifiedDateTime:  LocalDateTime,
  )