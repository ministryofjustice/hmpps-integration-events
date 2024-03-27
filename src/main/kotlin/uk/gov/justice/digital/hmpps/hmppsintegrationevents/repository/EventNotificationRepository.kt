package uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.repository.model.data.EventNotification

interface EventNotificationRepository: CrudRepository<EventNotification, Long> {

}