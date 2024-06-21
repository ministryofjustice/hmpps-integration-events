package uk.gov.justice.digital.hmpps.hmppsintegrationevents.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/events")
class EventsController {

  @GetMapping("/{client}")
  fun getEvents(@PathVariable client:String):String{

    return client
  }
}