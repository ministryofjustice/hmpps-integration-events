package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.enums

import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.HmppsDomainEventName
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
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.registration.AdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.registration.HmppsDomainEventMessage
import kotlin.collections.contains

val DYNAMIC_RISK_EVENTS = listOf(
  HmppsDomainEventName.ProbabtionCase.Registration.ADDED,
  HmppsDomainEventName.ProbabtionCase.Registration.UPDATED,
  HmppsDomainEventName.ProbabtionCase.Registration.DELETED,
  HmppsDomainEventName.ProbabtionCase.Registration.DEREGISTERED,
)

val PROBATION_STATUS_CHANGED_EVENTS = listOf(
  HmppsDomainEventName.ProbabtionCase.Registration.ADDED,
  HmppsDomainEventName.ProbabtionCase.Registration.UPDATED,
  HmppsDomainEventName.ProbabtionCase.Registration.DELETED,
  HmppsDomainEventName.ProbabtionCase.Registration.DEREGISTERED,
)

val MAPPA_DETAIL_REGISTER_EVENTS = listOf(
  HmppsDomainEventName.ProbabtionCase.Registration.ADDED,
  HmppsDomainEventName.ProbabtionCase.Registration.UPDATED,
  HmppsDomainEventName.ProbabtionCase.Registration.DELETED,
  HmppsDomainEventName.ProbabtionCase.Registration.DEREGISTERED,
)

val KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE_EVENTS = listOf(
  HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.RELEASED,
  HmppsDomainEventName.PrisonOffenderEvents.Prisoner.RELEASED,
  HmppsDomainEventName.CalculateReleaseDates.Prisoner.CHANGED,
)

val PERSON_EVENTS = listOf(
  HmppsDomainEventName.ProbabtionCase.Engagement.CREATED,
  HmppsDomainEventName.ProbabtionCase.PrisonIdentifier.ADDED,
  HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.CREATED,
  HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED,
  HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.RECEIVED,
)

val PRISONER_EVENTS = listOf(
  HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.CREATED,
  HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED,
  HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.RECEIVED,
)

val NEW_PERSON_EVENTS = listOf(
  HmppsDomainEventName.ProbabtionCase.Engagement.CREATED,
  HmppsDomainEventName.ProbabtionCase.PrisonIdentifier.ADDED,
  HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.CREATED,
  HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.RECEIVED,
)

val NEW_PRISONER_EVENTS = listOf(
  HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.CREATED,
  HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.RECEIVED,
)

enum class PrisonerChangedCategory {
  IDENTIFIERS,
  PERSONAL_DETAILS,
  ALERTS,
  STATUS,
  LOCATION,
  SENTENCE,
  RESTRICTED_PATIENT,
  INCENTIVE_LEVEL,
  PHYSICAL_DETAILS,
}

val PERSON_ADDRESS_EVENTS = listOf(
  HmppsDomainEventName.ProbabtionCase.Address.CREATED,
  HmppsDomainEventName.ProbabtionCase.Address.UPDATED,
  HmppsDomainEventName.ProbabtionCase.Address.DELETED,
)

val RESPONSIBLE_OFFICER_EVENTS = listOf(
  HmppsDomainEventName.Person.Community.Manager.ALLOCATED,
  HmppsDomainEventName.Person.Community.Manager.TRANSFERRED,
  HmppsDomainEventName.Probation.Staff.UPDATED,
)

val ALERT_EVENTS = listOf(
  HmppsDomainEventName.Person.Alert.CREATED,
  HmppsDomainEventName.Person.Alert.CHANGED,
  HmppsDomainEventName.Person.Alert.UPDATED,
  HmppsDomainEventName.Person.Alert.DELETED,
)

val PND_ALERT_TYPES = listOf(
  "BECTER", "HA", "XA", "XCA", "XEL", "XELH", "XER", "XHT", "XILLENT",
  "XIS", "XR", "XRF", "XSA", "HA2", "RCS", "RDV", "RKC", "RPB", "RPC",
  "RSS", "RST", "RDP", "REG", "RLG", "ROP", "RRV", "RTP", "RYP", "HS", "SC",
)

val LICENCE_CONDITION_EVENTS = listOf(
  HmppsDomainEventName.CreateAndVaryALicence.Licence.ACTIVATED,
  HmppsDomainEventName.CreateAndVaryALicence.Licence.INACTIVATED,
)

val MAPPA_DETAIL_REGISTER_TYPES = listOf(MAPPA_CODE)

val RISK_SCORE_TYPES = listOf(
  HmppsDomainEventName.ProbabtionCase.RiskScores.OGRS.MANUAL_CALCULATION,
  HmppsDomainEventName.RiskAssessment.Scores.OGRS.DETERMINED,
  HmppsDomainEventName.RiskAssessment.Scores.RSR.DETERMINED,
)

val ROSH_TYPES = listOf(HmppsDomainEventName.Assessment.Summary.PRODUCED)

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

val PLP_INDUCTION_SCHEDULE_EVENTS = listOf(HmppsDomainEventName.PLP.InductionSchedule.UPDATED)
val PLP_REVIEW_SCHEDULE_EVENTS = listOf(HmppsDomainEventName.PLP.ReviewSchedule.UPDATED)

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

val PERSON_CONTACT_EVENTS = listOf(
  HmppsDomainEventName.PrisonOffenderEvents.Prisoner.CONTACT_ADDED,
  HmppsDomainEventName.PrisonOffenderEvents.Prisoner.CONTACT_APPROVED,
  HmppsDomainEventName.PrisonOffenderEvents.Prisoner.CONTACT_UNAPPROVED,
  HmppsDomainEventName.PrisonOffenderEvents.Prisoner.CONTACT_REMOVED,
)

val PERSON_IEP_EVENTS = listOf(
  HmppsDomainEventName.Incentives.IEPReview.INSERTED,
  HmppsDomainEventName.Incentives.IEPReview.UPDATED,
  HmppsDomainEventName.Incentives.IEPReview.DELETED,
)

val PERSON_VISITOR_RESTRICTION_EVENTS = listOf(
  HmppsDomainEventName.PrisonOffenderEvents.Prisoner.PersonRestriction.UPSERTED,
  HmppsDomainEventName.PrisonOffenderEvents.Prisoner.PersonRestriction.DELETED,
)

val PERSON_CASE_NOTE_EVENTS = listOf(
  HmppsDomainEventName.Person.CaseNote.CREATED,
  HmppsDomainEventName.Person.CaseNote.UPDATED,
  HmppsDomainEventName.Person.CaseNote.DELETED,
)

val PERSON_ADJUDICATION_EVENTS = listOf(
  HmppsDomainEventName.Adjudication.Hearing.CREATED,
  HmppsDomainEventName.Adjudication.Hearing.DELETED,
  HmppsDomainEventName.Adjudication.Hearing.COMPLETED,
  HmppsDomainEventName.Adjudication.Punishments.CREATED,
  HmppsDomainEventName.Adjudication.Report.CREATED,
)

val PERSON_NON_ASSOCIATION_EVENTS = listOf(
  HmppsDomainEventName.PrisonOffenderEvents.Prisoner.NonAssociationDetail.CHANGED,
  HmppsDomainEventName.NonAssociations.CREATED,
  HmppsDomainEventName.NonAssociations.AMENDED,
  HmppsDomainEventName.NonAssociations.CLOSED,
  HmppsDomainEventName.NonAssociations.DELETED,
)

val VISIT_CHANGED_EVENTS = listOf(
  HmppsDomainEventName.PrisonVisit.BOOKED,
  HmppsDomainEventName.PrisonVisit.CHANGED,
  HmppsDomainEventName.PrisonVisit.CANCELLED,
)

enum class IntegrationEventType(
  private val pathTemplate: String,
  val predicate: (HmppsDomainEventMessage) -> Boolean,
) {
  DYNAMIC_RISKS_CHANGED(
    "v1/persons/{hmppsId}/risks/dynamic",
    { DYNAMIC_RISK_EVENTS.contains(it.eventType) && DYNAMIC_RISKS_REGISTER_TYPES.contains(it.additionalInformation!!.registerTypeCode) },
  ),
  PROBATION_STATUS_CHANGED(
    "v1/persons/{hmppsId}/status-information",
    { PROBATION_STATUS_CHANGED_EVENTS.contains(it.eventType) && PROBATION_STATUS_REGISTER_TYPES.contains(it.additionalInformation!!.registerTypeCode) },
  ),
  MAPPA_DETAIL_CHANGED(
    "v1/persons/{hmppsId}/risks/mappadetail",
    { MAPPA_DETAIL_REGISTER_EVENTS.contains(it.eventType) && MAPPA_DETAIL_REGISTER_TYPES.contains(it.additionalInformation!!.registerTypeCode) },
  ),
  RISK_SCORE_CHANGED(
    "v1/persons/{hmppsId}/risks/scores",
    { RISK_SCORE_TYPES.contains(it.eventType) },
  ),
  KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE(
    "v1/persons/{hmppsId}/sentences/latest-key-dates-and-adjustments",
    { KEY_DATES_AND_ADJUSTMENTS_PRISONER_RELEASE_EVENTS.contains(it.eventType) },
  ),
  LICENCE_CONDITION_CHANGED(
    "v1/persons/{hmppsId}/licences/conditions",
    { LICENCE_CONDITION_EVENTS.contains(it.eventType) },
  ),
  RISK_OF_SERIOUS_HARM_CHANGED(
    "v1/persons/{hmppsId}/risks/serious-harm",
    { ROSH_TYPES.contains(it.eventType) },
  ),
  PLP_INDUCTION_SCHEDULE_CHANGED(
    "v1/persons/{hmppsId}/plp-induction-schedule/history",
    { PLP_INDUCTION_SCHEDULE_EVENTS.contains(it.eventType) },
  ),
  PLP_REVIEW_SCHEDULE_CHANGED(
    "v1/persons/{hmppsId}/plp-review-schedule",
    { PLP_REVIEW_SCHEDULE_EVENTS.contains(it.eventType) },
  ),
  PERSON_STATUS_CHANGED(
    "v1/persons/{hmppsId}",
    { PERSON_EVENTS.contains(it.eventType) },
  ),
  PERSON_ADDRESS_CHANGED(
    "v1/persons/{hmppsId}/addresses",
    { PERSON_ADDRESS_EVENTS.contains(it.eventType) },
  ),
  PERSON_CONTACTS_CHANGED(
    "v1/persons/{hmppsId}/contacts",
    { PERSON_CONTACT_EVENTS.contains(it.eventType) },
  ),
  PERSON_IEP_LEVEL_CHANGED(
    "v1/persons/{hmppsId}/iep-level",
    { PERSON_IEP_EVENTS.contains(it.eventType) },
  ),
  PERSON_VISITOR_RESTRICTIONS_CHANGED(
    "v1/persons/{hmppsId}/visitor/{contactId}/restrictions",
    { PERSON_VISITOR_RESTRICTION_EVENTS.contains(it.eventType) },
  ),
  PERSON_VISIT_RESTRICTIONS_CHANGED(
    "v1/persons/{hmppsId}/visit-restrictions",
    { it.eventType == HmppsDomainEventName.PrisonOffenderEvents.Prisoner.Restriction.CHANGED },
  ),
  PERSON_VISIT_ORDERS_CHANGED(
    "v1/persons/{hmppsId}/visit-orders",
    { false }, // Probably not needed
  ),
  PERSON_FUTURE_VISITS_CHANGED(
    "v1/persons/{hmppsId}/visits/future",
    { VISIT_CHANGED_EVENTS.contains(it.eventType) },
  ),
  PERSON_ALERTS_CHANGED(
    "v1/persons/{hmppsId}/alerts",
    { ALERT_EVENTS.contains(it.eventType) },
  ),
  PERSON_PND_ALERTS_CHANGED(
    "v1/pnd/persons/{hmppsId}/alerts",
    { ALERT_EVENTS.contains(it.eventType) && PND_ALERT_TYPES.contains(it.additionalInformation!!.alertCode) },
  ),
  PERSON_CASE_NOTES_CHANGED(
    "v1/persons/{hmppsId}/case-notes",
    { NEW_PERSON_EVENTS.contains(it.eventType) || PERSON_CASE_NOTE_EVENTS.contains(it.eventType) },
  ),
  PERSON_NAME_CHANGED(
    "v1/persons/{hmppsId}/name",
    {
      NEW_PERSON_EVENTS.contains(it.eventType) ||
        (
          it.eventType == HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED &&
            (it.additionalInformation?.categoriesChanged?.contains(PrisonerChangedCategory.PERSONAL_DETAILS.name) ?: false)
          )
    },
  ),
  PERSON_CELL_LOCATION_CHANGED(
    "v1/persons/{hmppsId}/cell-location",
    {
      NEW_PERSON_EVENTS.contains(it.eventType) ||
        (
          it.eventType == HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED &&
            (it.additionalInformation?.categoriesChanged?.contains(PrisonerChangedCategory.LOCATION.name) ?: false)
          )
    },
  ),
  PERSON_RISK_CATEGORIES_CHANGED(
    "v1/persons/{hmppsId}/risks/categories",
    { false }, // Probably not needed
  ),
  PERSON_SENTENCES_CHANGED(
    "v1/persons/{hmppsId}/sentences",
    {
      NEW_PERSON_EVENTS.contains(it.eventType) ||
        (
          it.eventType == HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED &&
            (it.additionalInformation?.categoriesChanged?.contains(PrisonerChangedCategory.SENTENCE.name) ?: false)
          )
    },
  ),
  PERSON_OFFENCES_CHANGED(
    "v1/persons/{hmppsId}/offences",
    { false }, // Probably not needed
  ),
  PERSON_RESPONSIBLE_OFFICER_CHANGED(
    "v1/persons/{hmppsId}/person-responsible-officer",
    { RESPONSIBLE_OFFICER_EVENTS.contains(it.eventType) },
  ),
  PERSON_PROTECTED_CHARACTERISTICS_CHANGED(
    "v1/persons/{hmppsId}/protected-characteristics",
    { NEW_PERSON_EVENTS.contains(it.eventType) }, // No specific event found
  ),
  PERSON_REPORTED_ADJUDICATIONS_CHANGED(
    "v1/persons/{hmppsId}/reported-adjudications",
    { NEW_PERSON_EVENTS.contains(it.eventType) || PERSON_ADJUDICATION_EVENTS.contains(it.eventType) },
  ),
  PERSON_NUMBER_OF_CHILDREN_CHANGED(
    "v1/persons/{hmppsId}/number-of-children",
    { NEW_PERSON_EVENTS.contains(it.eventType) }, // No specific event found
  ),
  PERSON_PHYSICAL_CHARACTERISTICS_CHANGED(
    "v1/persons/{hmppsId}/physical-characteristics",
    {
      NEW_PERSON_EVENTS.contains(it.eventType) ||
        (
          it.eventType == HmppsDomainEventName.PrisonerOffenderSearch.Prisoner.UPDATED &&
            (it.additionalInformation?.categoriesChanged?.contains(PrisonerChangedCategory.PHYSICAL_DETAILS.name) ?: false)
          )
    },
  ),
  PERSON_IMAGES_CHANGED(
    "v1/persons/{hmppsId}/images",
    { NEW_PERSON_EVENTS.contains(it.eventType) }, // No specific event found
  ),
  PERSON_IMAGE_CHANGED(
    "v1/persons/{hmppsId}/images/{imageId}",
    { false }, // Probably not needed
  ),
  PRISONERS_CHANGED(
    "v1/prison/prisoners",
    { PRISONER_EVENTS.contains(it.eventType) },
  ),
  PRISONER_CHANGED(
    "v1/prison/prisoners/{hmppsId}",
    { PRISONER_EVENTS.contains(it.eventType) },
  ),
  PRISONER_BALANCES_CHANGED(
    "v1/prison/{prisonId}/prisoners/{hmppsId}/balances",
    { false },
  ),
  PRISONER_ACCOUNT_BALANCES_CHANGED(
    "v1/prison/{prisonId}/prisoners/{hmppsId}/account/{accountCode}/balances",
    { false },
  ),
  PRISONER_ACCOUNT_TRANSACTIONS_CHANGED(
    "v1/prison/{prisonId}/prisoners/{hmppsId}/account/{accountCode}/transactions",
    { false },
  ),
  PRISONER_NON_ASSOCIATIONS_CHANGED(
    "v1/prison/{prisonId}/prisoners/{hmppsId}/non-associations",
    { NEW_PRISONER_EVENTS.contains(it.eventType) || PERSON_NON_ASSOCIATION_EVENTS.contains(it.eventType) },
  ),
  PRISON_VISITS_CHANGED(
    "v1/prison/{prisonId}/visit/search",
    { VISIT_CHANGED_EVENTS.contains(it.eventType) },
  ),
  PRISON_RESIDENTIAL_HIERARCHY_CHANGED(
    "v1/prison/{prisonId}/residential-hierarchy",
    { false },
  ),
  PRISON_LOCATION_CHANGED(
    "v1/prison/{prisonId}/location/{locationKey}",
    { false },
  ),
  PRISON_RESIDENTIAL_DETAILS_CHANGED(
    "v1/prison/{prisonId}/residential-details",
    { false },
  ),
  PRISON_CAPACITY_CHANGED(
    "v1/prison/{prisonId}/capacity",
    { false },
  ),
  VISIT_CHANGED(
    "v1/visit/{visitReference}",
    { VISIT_CHANGED_EVENTS.contains(it.eventType) },
  ),
  VISIT_FROM_EXTERNAL_SYSTEM_CREATED(
    "v1/visit/id/by-client-ref/{clientVisitReference}",
    { false }, // Probably want to add clientVisitReference to visit created domain event
  ),
  CONTACT_CHANGED(
    "v1/contacts/{contactId}",
    { false },
  ),
  PERSON_HEALTH_AND_DIET_CHANGED(
    "v1/persons/{hmppsId}/health-and-diet",
    { NEW_PERSON_EVENTS.contains(it.eventType) }, // No specific event found
  ),
  PERSON_CARE_NEEDS_CHANGED(
    "v1/persons/{hmppsId}/care-needs",
    { NEW_PERSON_EVENTS.contains(it.eventType) }, // No specific event found
  ),
  PERSON_LANGUAGES_CHANGED(
    "v1/persons/{hmppsId}/languages",
    { NEW_PERSON_EVENTS.contains(it.eventType) }, // No specific event found
  ),
  ;

  fun path(hmppsId: String, additionalInformation: AdditionalInformation?): String {
    var replacedPath = pathTemplate.replace("{hmppsId}", hmppsId)
    additionalInformation?.let {
      if (it.contactPersonId != null) replacedPath = replacedPath.replace("{contactId}", it.contactPersonId)
      if (it.reference != null) replacedPath = replacedPath.replace("{visitReference}", it.reference)
    }
    return replacedPath
  }

  companion object {
    fun from(eventType: IntegrationEventType): IntegrationEventType? = IntegrationEventType.entries.firstOrNull {
      it.ordinal == eventType.ordinal
    }
  }
}
