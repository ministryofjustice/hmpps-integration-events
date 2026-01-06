package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import io.sentry.Sentry
import org.jetbrains.annotations.NotNull
import org.springframework.stereotype.Service

/**
 * This is a service facade of Telemetry.
 *
 * It shall cover tools like Sentry ([io.sentry.Sentry]), Application Insights (e.g. [com.microsoft.applicationinsights.TelemetryClient]), etc.
 */
@Service
class TelemetryService {

  fun captureException(@NotNull throwable: Throwable) {
    Sentry.captureException(throwable)
  }

  fun captureMessage(message: String) {
    Sentry.captureMessage(message)
  }
}
