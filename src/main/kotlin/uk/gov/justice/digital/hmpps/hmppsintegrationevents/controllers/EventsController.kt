package uk.gov.justice.digital.hmpps.hmppsintegrationevents.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.EventResponse
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.services.ClientEventService

@RestController
@RequestMapping("/events")
class EventsController(
  @Autowired val clientEventService: ClientEventService,
) {

  @GetMapping("/{pathCode}")
  fun getEvents(@PathVariable pathCode: String): EventResponse {
    return clientEventService.getClientMessage(pathCode)
  }
}
