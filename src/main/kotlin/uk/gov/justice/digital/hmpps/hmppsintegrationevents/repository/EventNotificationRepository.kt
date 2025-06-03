package uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventType
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification
import java.time.LocalDateTime

@Repository
interface EventNotificationRepository : JpaRepository<EventNotification, Long> {

  @Query("select a from EventNotification a where a.lastModifiedDateTime <= :dateTime")
  fun findAllWithLastModifiedDateTimeBefore(
    @Param("dateTime") dateTime: LocalDateTime?,
  ): List<EventNotification>

  fun existsByHmppsIdAndEventType(hmppsId: String, eventType: IntegrationEventType): Boolean

  @Modifying
  @Query("""
    INSERT INTO EventNotification (url, eventType, hmppsId, prisonId, lastModifiedDateTime)
    VALUES (:#{#eventNotification.url}, :#{#eventNotification.eventType}, :#{#eventNotification.hmppsId}, :#{#eventNotification.prisonId}, :#{#eventNotification.lastModifiedDateTime})
    ON CONFLICT(url, eventType)
    DO UPDATE SET
      lastModifiedDateTime = :#{#eventNotification.lastModifiedDateTime}
  """)
  fun insertOrUpdate(eventNotification: EventNotification): Int

  @Modifying
  @Query("update EventNotification e set e.lastModifiedDateTime = :dateTime where e.hmppsId = :hmppsId and e.eventType = :eventType")
  fun updateLastModifiedDateTimeByHmppsIdAndEventType(
    @Param("dateTime") dateTime: LocalDateTime,
    @Param("hmppsId") hmppsId: String,
    @Param("eventType") eventType: IntegrationEventType,
  ): Int
}
