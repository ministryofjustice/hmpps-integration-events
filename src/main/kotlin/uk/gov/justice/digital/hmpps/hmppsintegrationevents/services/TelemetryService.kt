package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.sentry.Sentry
import org.jetbrains.annotations.NotNull
import org.springframework.stereotype.Service

@Service
open class TelemetryService {

  open fun captureException(@NotNull throwable: Throwable) {
    Sentry.captureException(throwable)
  }
  open fun captureMessage(message: String) {
    Sentry.captureMessage(message)
  }
}
