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

val DYNAMIC_RISK_EVENTS = listOf(
  PROBATION_CASE_REGISTRATION_ADDED,
  PROBATION_CASE_REGISTRATION_DELETED,
  PROBATION_CASE_REGISTRATION_DEREGISTERED,
  PROBATION_CASE_REGISTRATION_UPDATED,
)

val PROBATION_STATUS_CHANGED_EVENTS = listOf(
  PROBATION_CASE_REGISTRATION_ADDED,
  PROBATION_CASE_REGISTRATION_DELETED,
  PROBATION_CASE_REGISTRATION_DEREGISTERED,
  PROBATION_CASE_REGISTRATION_UPDATED,
)

val MAPPA_DETAIL_REGISTER_EVENTS = listOf(
  PROBATION_CASE_REGISTRATION_ADDED,
  PROBATION_CASE_REGISTRATION_DELETED,
  PROBATION_CASE_REGISTRATION_DEREGISTERED,
  PROBATION_CASE_REGISTRATION_UPDATED,
)

val RISK_SCORE_CHANGED_EVENTS =
  listOf("risk-assessment.scores.determined", "probation-case.risk-scores.ogrs.manual-calculation")

val KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE_EVENTS = listOf(
  "prisoner-offender-search.prisoner.released",
  "prison-offender-events.prisoner.released",
  "calculate-release-dates.prisoner.changed",
)

val PERSON_EVENTS = listOf(
  "probation-case.engagement.created",
  "probation-case.prison-identifier.added",
  "prisoner-offender-search.prisoner.created",
  "prisoner-offender-search.prisoner.updated",
)

val PERSON_ADDRESS_EVENTS = listOf(
  "probation-case.address.created",
  "probation-case.address.updated",
  "probation-case.address.deleted",
)

val RESPONSIBLE_OFFICER_EVENTS = listOf(
  "person.community.manager.allocated",
  "person.community.manager.transferred",
  "probation.staff.updated",
)

val PND_ALERT_EVENTS = listOf(
  "person.alert.created",
  "person.alert.changed",
  "person.alert.deleted",
  "person.alert.updated",
)

val LICENCE_CONDITION_EVENTS =
  listOf("create-and-vary-a-licence.licence.activated", "create-and-vary-a-licence.licence.inactivated")

val MAPPA_DETAIL_REGISTER_TYPES = listOf(MAPPA_CODE)

val RISK_SCORE_TYPES = listOf(
  "risk-assessment.scores.ogrs.determined",
  "probation-case.risk-scores.ogrs.manual-calculation",
  "risk-assessment.scores.rsr.determined",
)

val ROSH_TYPES = listOf("assessment.summary.produced")

val PROBATION_STATUS_REGISTER_TYPES = listOf(SERIOUS_FURTHER_OFFENCE_CODE, WARRANT_SUMMONS_CODE)

val PND_ALERT_TYPES = listOf(
  "BECTER", "HA", "XA", "XCA", "XEL", "XELH", "XER", "XHT", "XILLENT",
  "XIS", "XR", "XRF", "XSA", "HA2", "RCS", "RDV", "RKC", "RPB", "RPC",
  "RSS", "RST", "RDP", "REG", "RLG", "ROP", "RRV", "RTP", "RYP", "HS", "SC",
)

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

val PLP_INDUCTION_SCHEDULE_EVENT = listOf("plp.induction-schedule.updated")
val PLP_REVIEW_SCHEDULE_EVENT = listOf("plp.review-schedule.updated")

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

enum class IntegrationEventTypes(val value: String, private val pathTemplate: String) {
  DYNAMIC_RISKS_CHANGED("DynamicRisks.Changed", "v1/persons/{hmppsId}/risks/dynamic"),
  PROBATION_STATUS_CHANGED("ProbationStatus.Changed", "v1/persons/{hmppsId}/status-information"),
  MAPPA_DETAIL_CHANGED("MappaDetail.Changed", "v1/persons/{hmppsId}/risks/mappadetail"),
  RISK_SCORE_CHANGED("RiskScore.Changed", "v1/persons/{hmppsId}/risks/scores"),
  KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE(
    "KeyDatesAndAdjustments.PrisonerReleased",
    "v1/persons/{hmppsId}/sentences/latest-key-dates-and-adjustments",
  ),
  PERSON_STATUS_CHANGED("PersonStatus.Changed", "v1/persons/{hmppsId}"),
  PND_ALERTS_CHANGED("PNDAlerts.Changed", "v1/pnd/persons/{hmppsId}/alerts"),
  LICENCE_CONDITION_CHANGED("LicenceCondition.Changed", "v1/persons/{hmppsId}/licences/conditions"),
  RISK_OF_SERIOUS_HARM_CHANGED("RiskOfSeriousHarm.Changed", "v1/persons/{hmppsId}/risks/serious-harm"),
  PLP_INDUCTION_SCHEDULE_CHANGED("PLPInductionSchedule.Changed", "v1/persons/{hmppsId}/plp-induction-schedule/history"),
  PLP_REVIEW_SCHEDULE_CHANGED("PLPReviewSchedule.Changed", "v1/persons/{hmppsId}/plp-review-schedule"),
  PERSON_ADDRESS_CHANGED("PersonAddress.Changed", "v1/persons/{hmppsId}/addresses"),
  RESPONSIBLE_OFFICER_CHANGED("ResponsibleOfficer.Changed", "/v1/persons/{hmppsId}/person-responsible-officer"),
  ;

  fun path(hmppsId: String) = pathTemplate.replace("{hmppsId}", hmppsId)

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
      DYNAMIC_RISK_EVENTS.contains(it.eventType) && DYNAMIC_RISKS_REGISTER_TYPES.contains(it.additionalInformation!!.registerTypeCode)
    },
    IntegrationEventTypesFilter(IntegrationEventTypes.PROBATION_STATUS_CHANGED) {
      PROBATION_STATUS_CHANGED_EVENTS.contains(it.eventType) && PROBATION_STATUS_REGISTER_TYPES.contains(it.additionalInformation!!.registerTypeCode)
    },
    IntegrationEventTypesFilter(IntegrationEventTypes.MAPPA_DETAIL_CHANGED) {
      MAPPA_DETAIL_REGISTER_EVENTS.contains(it.eventType) && MAPPA_DETAIL_REGISTER_TYPES.contains(it.additionalInformation!!.registerTypeCode)
    },
    IntegrationEventTypesFilter(IntegrationEventTypes.RISK_SCORE_CHANGED, {
      RISK_SCORE_TYPES.contains(it.eventType)
    }),
    IntegrationEventTypesFilter(IntegrationEventTypes.KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE) {
      KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE_EVENTS.contains(it.eventType)
    },
    IntegrationEventTypesFilter(IntegrationEventTypes.PERSON_STATUS_CHANGED) {
      PERSON_EVENTS.contains(it.eventType)
    },
    IntegrationEventTypesFilter(IntegrationEventTypes.PND_ALERTS_CHANGED) {
      PND_ALERT_EVENTS.contains(it.eventType) && PND_ALERT_TYPES.contains(it.additionalInformation!!.alertCode)
    },
    IntegrationEventTypesFilter(IntegrationEventTypes.LICENCE_CONDITION_CHANGED) {
      LICENCE_CONDITION_EVENTS.contains(it.eventType)
    },
    IntegrationEventTypesFilter(IntegrationEventTypes.RISK_OF_SERIOUS_HARM_CHANGED) {
      ROSH_TYPES.contains(it.eventType)
    },
    IntegrationEventTypesFilter(IntegrationEventTypes.PLP_INDUCTION_SCHEDULE_CHANGED) {
      PLP_INDUCTION_SCHEDULE_EVENT.contains(it.eventType)
    },
    IntegrationEventTypesFilter(IntegrationEventTypes.PLP_REVIEW_SCHEDULE_CHANGED) {
      PLP_REVIEW_SCHEDULE_EVENT.contains(it.eventType)
    },
    IntegrationEventTypesFilter(IntegrationEventTypes.PERSON_ADDRESS_CHANGED) {
      PERSON_ADDRESS_EVENTS.contains(it.eventType)
    },
    IntegrationEventTypesFilter(IntegrationEventTypes.RESPONSIBLE_OFFICER_CHANGED) {
      RESPONSIBLE_OFFICER_EVENTS.contains(it.eventType)
    },
  )
}

data class IntegrationEventTypesFilter(
  val integrationEventTypes: IntegrationEventTypes,
  val predicate: (HmppsDomainEventMessage) -> Boolean,
)
