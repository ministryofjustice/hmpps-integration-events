package uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources.wiremock

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class HmppsAuthExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  override fun afterAll(context: ExtensionContext): Unit = server.stop()

  override fun beforeAll(context: ExtensionContext) {
    server.start()
    server.stubGrantToken()
  }

  override fun beforeEach(context: ExtensionContext): Unit = server.resetRequests()

  companion object {
    val server = HmppsAuthMockServer()
  }
}
