package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.sentry.Sentry
import org.jetbrains.annotations.NotNull
import org.springframework.stereotype.Service

@Service
class TelemetryService {

  fun captureException(@NotNull throwable: Throwable) {
    Sentry.captureException(throwable)
  }

  fun captureMessage(message: String) {
    Sentry.captureMessage(message)
  }
}
