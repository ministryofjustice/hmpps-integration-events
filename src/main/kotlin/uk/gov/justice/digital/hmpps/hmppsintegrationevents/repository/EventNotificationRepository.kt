package uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository

import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.IntegrationEventStatus
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
    set a.claimId = :claimId, a.status = :statusTo 
    where a.lastModifiedDateTime <= :dateTime and ( a.status is null or a.status = :statusFrom)
  """,
  )
  fun setProcessing(
    @Param("dateTime") dateTime: LocalDateTime,
    @Param("claimId") claimId: String,
    @Param("statusTo") statusTo: IntegrationEventStatus = IntegrationEventStatus.PROCESSING,
    @Param("statusFrom") statusFrom: IntegrationEventStatus = IntegrationEventStatus.PENDING,
  )

  @Transactional
  @Modifying
  @Query(
    """
    update EventNotification a
    set a.status = :status 
    where a.eventId = :eventId
  """,
  )
  fun setProcessed(
    @Param("eventId") eventId: Long,
    @Param("status") status: IntegrationEventStatus = IntegrationEventStatus.PROCESSED,
  )

  @Modifying
  @Query(
    """
    delete from EventNotification a
    where a.lastModifiedDateTime <= :dateTime and a.status = :status 
  """,
  )
  fun deleteEvents(
    @Param("dateTime") dateTime: LocalDateTime,
    @Param("status") status: IntegrationEventStatus = IntegrationEventStatus.PROCESSED,
  )

  @Query(
    """
    select a from EventNotification a where a.status = :status and a.claimId = :claimId
  """,
  )
  fun findAllProcessingEvents(
    @Param("claimId") claimId: String,
    @Param("status") status: IntegrationEventStatus = IntegrationEventStatus.PROCESSING,
  ): List<EventNotification>

  fun existsByHmppsIdAndEventType(hmppsId: String, eventType: IntegrationEventType): Boolean

  @Modifying
  @Transactional
  @Query(
    """
    INSERT INTO EventNotification (url, eventType, hmppsId, prisonId, lastModifiedDateTime)
    VALUES (:#{#eventNotification.url}, :#{#eventNotification.eventType}, :#{#eventNotification.hmppsId}, :#{#eventNotification.prisonId}, :#{#eventNotification.lastModifiedDateTime})
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
