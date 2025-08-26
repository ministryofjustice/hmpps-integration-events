package uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository

import jakarta.transaction.Transactional
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

  fun findByHmppsIdIsIn(hmppsIds: Collection<String>): List<EventNotification>

  @Query("select a from EventNotification a where a.lastModifiedDateTime <= :dateTime")
  fun findAllWithLastModifiedDateTimeBefore(
    @Param("dateTime") dateTime: LocalDateTime?,
  ): List<EventNotification>

  @Transactional
  @Modifying
  @Query(
    """
    update EventNotification a 
    set a.claimId = :claimId, a.status = "PROCESSING" 
    where a.lastModifiedDateTime <= :dateTime and ( a.status is null or a.status = "PENDING")
  """,
  )
  fun setProcessing(
    @Param("dateTime") dateTime: LocalDateTime,
    @Param("claimId") claimId: String,
  )

  @Transactional
  @Modifying
  @Query(
    """
    update EventNotification a
    set a.status = "PROCESSED" 
    where a.eventId = :eventId
  """,
  )
  fun setProcessed(
    @Param("eventId") eventId: Long,
  )

  @Modifying
  @Query(
    """
    delete from EventNotification a
    where a.lastModifiedDateTime <= :dateTime and a.status = "PROCESSED" 
  """,
  )
  fun deleteEvents(
    @Param("dateTime") dateTime: LocalDateTime,
  )

  @Query(
    """
    select a from EventNotification a where a.status = "PROCESSING" and a.claimId = :claimId
  """,
  )
  fun findAllProcessingEvents(
    @Param("claimId") claimId: String,
  ): List<EventNotification>

  fun existsByHmppsIdAndEventType(hmppsId: String, eventType: IntegrationEventType): Boolean

  @Modifying
  @Transactional
  @Query(
    """
    INSERT INTO EventNotification (url, eventType, hmppsId, prisonId, status, lastModifiedDateTime)
    VALUES (:#{#eventNotification.url}, :#{#eventNotification.eventType}, :#{#eventNotification.hmppsId}, :#{#eventNotification.prisonId}, :#{#eventNotification.status}, :#{#eventNotification.lastModifiedDateTime})
    ON CONFLICT(url, eventType)
    DO UPDATE SET
      lastModifiedDateTime = :#{#eventNotification.lastModifiedDateTime}
  """,
  )
  fun insertOrUpdate(
    @Param("eventNotification") eventNotification: EventNotification,
  )

  @Modifying
  @Query("update EventNotification e set e.lastModifiedDateTime = :dateTime where e.hmppsId = :hmppsId and e.eventType = :eventType")
  fun updateLastModifiedDateTimeByHmppsIdAndEventType(
    @Param("dateTime") dateTime: LocalDateTime,
    @Param("hmppsId") hmppsId: String,
    @Param("eventType") eventType: IntegrationEventType,
  ): Int
}
