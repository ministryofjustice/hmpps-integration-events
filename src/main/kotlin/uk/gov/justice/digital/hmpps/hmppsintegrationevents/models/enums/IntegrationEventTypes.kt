package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums

import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.RegisterTypes.CHILD_CONCERNS_CODE
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.RegisterTypes.CHILD_PROTECTION_CODE
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.RegisterTypes.HIGH_ROSH_CODE
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.RegisterTypes.LOW_ROSH_CODE
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.RegisterTypes.MAPPA_CODE
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.RegisterTypes.MED_ROSH_CODE
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.RegisterTypes.RISK_TO_VULNERABLE_ADULT_CODE
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.RegisterTypes.SERIOUS_FURTHER_OFFENCE_CODE
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.RegisterTypes.STREET_GANGS_CODE
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.RegisterTypes.VISOR_CODE
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.RegisterTypes.WARRANT_SUMMONS_CODE
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums.RegisterTypes.WEAPONS_CODE
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.registration.HmppsDomainEventMessage

const val PROBATION_CASE_REGISTRATION_ADDED = "probation-case.registration.added"
const val PROBATION_CASE_REGISTRATION_DELETED = "probation-case.registration.deleted"
const val PROBATION_CASE_REGISTRATION_DEREGISTERED = "probation-case.registration.deregistered"
const val PROBATION_CASE_REGISTRATION_UPDATED = "probation-case.registration.updated"

val DYNAMIC_RISK_EVENTS = listOf(PROBATION_CASE_REGISTRATION_ADDED, PROBATION_CASE_REGISTRATION_DELETED, PROBATION_CASE_REGISTRATION_DEREGISTERED, PROBATION_CASE_REGISTRATION_UPDATED)

val PROBATION_STATUS_CHANGED_EVENTS = listOf(PROBATION_CASE_REGISTRATION_ADDED, PROBATION_CASE_REGISTRATION_DELETED, PROBATION_CASE_REGISTRATION_DEREGISTERED, PROBATION_CASE_REGISTRATION_UPDATED)

val MAPPA_DETAIL_REGISTER_EVENTS = listOf(PROBATION_CASE_REGISTRATION_ADDED, PROBATION_CASE_REGISTRATION_DELETED, PROBATION_CASE_REGISTRATION_DEREGISTERED, PROBATION_CASE_REGISTRATION_UPDATED)

val RISK_SCORE_CHANGED_EVENTS = listOf("risk-assessment.scores.determined", "probation-case.risk-scores.ogrs.manual-calculation")

val KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE_EVENTS = listOf("prisoner-offender-search.prisoner.released", "prison-offender-events.prisoner.released", "calculate-release-dates.prisoner.changed")

val PERSON_EVENTS = listOf("probation-case.engagement.created", "probation-case.prison-identifier.added", "prisoner-offender-search.prisoner.created", "prisoner-offender-search.prisoner.updated")

val MAPPA_DETAIL_REGISTER_TYPES = listOf(MAPPA_CODE)

val RISK_SCORE_TYPES = listOf("risk-assessment.scores.ogrs.determined", "probation-case.risk-scores.ogrs.manual-calculation", "risk-assessment.scores.rsr.determined", "assessment.summary.produced")

val PROBATION_STATUS_REGISTER_TYPES = listOf(SERIOUS_FURTHER_OFFENCE_CODE, WARRANT_SUMMONS_CODE)

val DYNAMIC_RISKS_REGISTER_TYPES = listOf(
  CHILD_CONCERNS_CODE,
  CHILD_PROTECTION_CODE,
  RISK_TO_VULNERABLE_ADULT_CODE,
  STREET_GANGS_CODE,
  VISOR_CODE,
  WEAPONS_CODE,
  LOW_ROSH_CODE,
  MED_ROSH_CODE,
  HIGH_ROSH_CODE,
)

object RegisterTypes {
  const val MAPPA_CODE = "MAPP" // Multi-Agency Public Protection Arrangements
  const val CHILD_CONCERNS_CODE = "RCCO" // Safeguarding concerns where a child is at risk from the offender
  const val CHILD_PROTECTION_CODE = "RCPR" // Child is subject to a protection plan/conference
  const val RISK_TO_VULNERABLE_ADULT_CODE = "RVAD" // Risk to a vulnerable adult
  const val STREET_GANGS_CODE = "STRG" // Involved in serious group offending
  const val VISOR_CODE = "AVIS" // Subject has a ViSOR record
  const val WEAPONS_CODE = "WEAP" // Known to use/carry weapon
  const val LOW_ROSH_CODE = "RLRH" // Low risk of serious harm
  const val MED_ROSH_CODE = "RMRH" // Medium risk of serious harm
  const val HIGH_ROSH_CODE = "RHRH" // High risk of serious harm
  const val SERIOUS_FURTHER_OFFENCE_CODE = "ASFO" // Subject to SFO review/investigation
  const val WARRANT_SUMMONS_CODE = "WRSM" // Outstanding warrant or summons
}

enum class IntegrationEventTypes(val value: String, val path: String) {
  DYNAMIC_RISKS_CHANGED("DynamicRisks.Changed", "/risks/dynamic"),
  PROBATION_STATUS_CHANGED("ProbationStatus.Changed", "/status-information"),
  MAPPA_DETAIL_CHANGED("MappaDetail.Changed", "/risks/mappadetail"),
  RISK_SCORE_CHANGED("RiskScore.Changed", "/risks/scores"),
  KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE("KeyDatesAndAdjustments.PrisonerReleased", "/sentences/latest-key-dates-and-adjustments"),
  PERSON_STATUS_CHANGED("PersonStatus.Changed", ""),
  ;

  companion object {
    fun from(eventType: IntegrationEventTypes): IntegrationEventTypes? =
      IntegrationEventTypes.entries.firstOrNull {
        it.value == eventType.value
      }
  }
}

object IntegrationEventTypesFilters {
  val filters: List<IntegrationEventTypesFilter> = listOf(
    IntegrationEventTypesFilter(IntegrationEventTypes.DYNAMIC_RISKS_CHANGED) {
      DYNAMIC_RISK_EVENTS.contains(it.eventType) && DYNAMIC_RISKS_REGISTER_TYPES.contains(it.additionalInformation.registerTypeCode)
    },
    IntegrationEventTypesFilter(IntegrationEventTypes.PROBATION_STATUS_CHANGED) {
      PROBATION_STATUS_CHANGED_EVENTS.contains(it.eventType) && PROBATION_STATUS_REGISTER_TYPES.contains(it.additionalInformation.registerTypeCode)
    },
    IntegrationEventTypesFilter(IntegrationEventTypes.MAPPA_DETAIL_CHANGED) {
      MAPPA_DETAIL_REGISTER_EVENTS.contains(it.eventType) && MAPPA_DETAIL_REGISTER_TYPES.contains(it.additionalInformation.registerTypeCode)
    },
    IntegrationEventTypesFilter(IntegrationEventTypes.RISK_SCORE_CHANGED) {
      RISK_SCORE_TYPES.contains(it.eventType)
    },
    IntegrationEventTypesFilter(IntegrationEventTypes.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE) {
      KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE_EVENTS.contains(it.eventType)
    },
    IntegrationEventTypesFilter(IntegrationEventTypes.PERSON_STATUS_CHANGED) {
      PERSON_EVENTS.contains(it.eventType)
    },
  )
}

data class IntegrationEventTypesFilter(
  val integrationEventTypes: IntegrationEventTypes,
  val predicate: (HmppsDomainEventMessage) -> Boolean,
)
