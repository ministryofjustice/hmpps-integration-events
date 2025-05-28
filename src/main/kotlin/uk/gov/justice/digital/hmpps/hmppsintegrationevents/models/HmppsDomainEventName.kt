package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models

enum class HmppsDomainEventName(name: String) {
  PROBATION_CASE_REGISTRATION_ADDED("probation-case.registration.added"),
  PROBATION_CASE_REGISTRATION_DELETED("probation-case.registration.deleted"),
  PROBATION_CASE_REGISTRATION_DEREGISTERED("probation-case.registration.deregistered"),
  PROBATION_CASE_REGISTRATION_UPDATED("probation-case.registration.updated")
}