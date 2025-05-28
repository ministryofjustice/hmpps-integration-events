package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models

object HmppsDomainEventName {
  object ProbabtionCase {
    object Registration {
      const val ADDED = "probation-case.registration.added"
      const val UPDATED = "probation-case.registration.updated"
      const val DELETED = "probation-case.registration.deleted"
      const val DEREGISTERED = "probation-case.registration.deregistered"
    }
    object Engagement {
      const val CREATED = "probation-case.engagement.created"
    }
    object PrisonIdentifier {
      const val ADDED = "probation-case.prison.prison-identifier.added"
    }
    object Address {
      const val CREATED = "probation-case.address.created"
      const val UPDATED = "probation-case.address.updated"
      const val DELETED = "probation-case.address.deleted"
    }
    object RiskScores {
      object OGRS {
        const val MANUAL_CALCULATION = "probation-case.risk-scores.ogrs.manual-calculation"
      }
    }
  }

  object Probation {
    object Staff {
      const val UPDATED = "probation.staff.updated"
    }
  }

  object PLP {
    object InductionSchedule {
      const val UPDATED = "plp.induction-schedule.updated"
    }
    object ReviewSchedule {
      const val UPDATED = "plp.review-schedule.updated"
    }
  }

  object CreateAndVaryALicence {
    object Licence {
      const val ACTIVATED = "create-and-vary-a-licence.licence.activated"
      const val INACTIVATED = "create-and-vary-a-licence.licence.inactived"
    }
  }

  object Person {
    object Alert {
      const val CREATED = "person.alert.created"
      const val CHANGED = "person.alert.changed"
      const val UPDATED = "person.alert.updated"
      const val DELETED = "person.alert.deleted"
    }
    object Community {
      object Manager {
        const val ALLOCATED = "person.community.manager.allocated"
        const val TRANSFERRED = "person.community.manager.transferred"
      }
    }
  }

  object PrisonerOffenderSearch {
    object Prisoner {
      const val CREATED = "prisoner-offender-search.prisoner.created"
      const val UPDATED = "prisoner-offender-search.prisoner.updated"
      const val RELEASED = "prisoner-offender-search.prisoner.released"
    }
  }

  object PrisonOffenderEvents {
    object Prisoner {
      const val RELEASED = "prison-offender-events.prisoner.released"
    }
  }

  object CalculateReleaseDates {
    object Prisoner {
      const val CHANGED = "calculate-release-dates.prisoner.changed"
    }
  }

  object RiskAssessment {
    object Scores {
      object OGRS {
        const val DETERMINED = "risk-assessment.scores.ogrs.determined"
      }
      object RSR {
        const val DETERMINED = "risk-assessment.scores.rsr.determined"
      }
    }
  }

  object Assessment {
    object Summary {
      const val PRODUCED = "assessment.summary.produced"
    }
  }
}
