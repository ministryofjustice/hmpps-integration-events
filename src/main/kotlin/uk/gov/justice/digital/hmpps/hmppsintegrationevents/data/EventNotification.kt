package uk.gov.justice.digital.hmpps.hmppsintegrationevents.data

import uk.gov.justice.digital.hmpps.hmppsintegrationevents.enum.EventType
import java.time.LocalDateTime

class EventNotification (

  event_id: String,
  hmpps_id: String,
  event_type: EventType,
  url: String,
  last_modified_date_time: LocalDateTime

)