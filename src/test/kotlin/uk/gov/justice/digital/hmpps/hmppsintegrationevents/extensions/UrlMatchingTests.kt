package uk.gov.justice.digital.hmpps.hmppsintegrationevents.extensions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.*

class UrlMatchingTests {
  @Nested
  @DisplayName("Given an URL pattern for matching")
  inner class GivenUrlPatternForMatching {
    @Nested
    @DisplayName("And it is Regex pattern")
    inner class AndRegexPattern {
      @Test
      fun `should match url pattern`() = assertMatchesUrlPatternDoesMatch(
        input = "/v1/persons/A1234BC",
        urlPattern = "/v1/persons/.*",
      )

      @Test
      fun `should match url pattern without wildcard`() = assertMatchesUrlPatternDoesMatch(
        input = "/v1/prison/prisoners",
        urlPattern = "/v1/prison/prisoners",
      )

      @Test
      fun `should not match unexpected url pattern`() = assertMatchesUrlPatternNotMatch(
        input = "/v1/something/id123",
        urlPattern = "/v1/another/.*",
      )

      @Test
      fun `should not match url pattern partially`() = assertMatchesUrlPatternNotMatch(
        input = "/v1/persons/A1234BC/def",
        urlPattern = "/v1/persons/[^/]*",
      )
    }

    @Nested
    @DisplayName("And it is canonical URL")
    inner class AndCanonicalUrl {
      private val crn = "X777776"
      private val nomsNumber = "A1234BC"

      private val canonicalUrlGetPerson = "/v1/persons/{hmppsId}"

      @Nested
      @DisplayName("And a HMPPS ID")
      inner class AndHmppsId {
        @Test
        fun `should match url to canonical pattern, with CRN as HMPPS ID`() = assertMatchesUrlPatternDoesMatch(
          input = "/v1/persons/$crn",
          urlPattern = canonicalUrlGetPerson,
        )

        @Test
        fun `should match url to canonical pattern, with PRN as HMPPS ID`() = assertMatchesUrlPatternDoesMatch(
          input = "/v1/persons/$crn",
          urlPattern = canonicalUrlGetPerson,
        )

        @Test
        fun `should match url to canonical pattern, with CPR ID (UUID) as HMPPS ID`() = assertMatchesUrlPatternDoesMatch(
          input = "/v1/persons/${UUID.randomUUID()}",
          urlPattern = canonicalUrlGetPerson,
        )
      }

      @Nested
      @DisplayName("And a named parameter or more")
      inner class AndNamedParameters {
        @Test
        fun `should match url to canonical pattern, with alphanumeric value`() = assertMatchesUrlPatternDoesMatch(
          input = "/v1/persons/someId1",
          urlPattern = canonicalUrlGetPerson,
        )

        @Test
        fun `should match url to canonical pattern, with hyphen in value`() = assertMatchesUrlPatternDoesMatch(
          input = "/v1/persons/some-id-with-hyphen",
          urlPattern = canonicalUrlGetPerson,
        )

        @Test
        fun `should match url to canonical pattern, with multiple named parameters`() {
          val prisonId = "MDI"
          val hmppsId = nomsNumber
          val accountCode = "spends"
          assertMatchesUrlPatternDoesMatch(
            input = "/v1/prison/$prisonId/prisoners/$hmppsId/accounts/$accountCode/balances",
            urlPattern = "/v1/prison/{prisonId}/prisoners/{hmppsId}/accounts/{accountCode}/balances",
          )
        }
      }

      @Test
      fun `should match url to canonical pattern, without leading slash at path`() = assertMatchesUrlPatternDoesMatch(
        input = "/v1/persons/$crn",
        urlPattern = "v1/persons/{hmppsId}",
      )

      @Test
      fun `should match url to canonical pattern, with actual URL as input`() = assertMatchesUrlPatternDoesMatch(
        input = "/v1/persons/$nomsNumber/name",
        urlPattern = "/v1/persons/{hmppsId}/name",
      )

      @Test
      fun `should not match unexpected url to canonical pattern`() = assertMatchesUrlPatternNotMatch(
        input = "/some/url/id1",
        urlPattern = "/my/url/{id}",
      )

      @Test
      fun `should not match invalid url to canonical pattern`() = assertMatchesUrlPatternNotMatch(
        // { and } are not in allowlist of named parameter value
        input = "/some/id1/more/{id2}",
        urlPattern = "/some/{id1}/more/{id2}",
      )

      /**
       * Tests for these known named parameters
       * - common: hmppsId, prisonId
       * - specific: contactId, id(imageId), accountCode, key(locationKey), clientReference(clientVisitReference), visitReference
       *
       * The textBlock last line must end with the """
       */
      @ParameterizedTest
      @CsvSource(
        textBlock = """
        /v1/persons/X777776                                           , /v1/persons/{hmppsId}
        /v1/persons/X777776/addresses                                 , /v1/persons/{hmppsId}/addresses
        /v1/persons/X777776/cell-location                             , /v1/persons/{hmppsId}/cell-location
        /v1/persons/X777776/education/assessments                     , /v1/persons/{hmppsId}/education/assessments
        /v1/persons/X777776/education/san/plan-creation-schedule      , /v1/persons/{hmppsId}/education/san/plan-creation-schedule
        /v1/prison/MDI/prisoners/A1234BC/non-associations             , /v1/prison/{prisonId}/prisoners/{hmppsId}/non-associations
        /v1/prison/MDI/prisoners/A1234BC/account/spends/transactions  , /v1/prison/{prisonId}/prisoners/{hmppsId}/account/{accountCode}/transactions
        /v1/contacts/12345678                                         , /v1/contacts/{contactId}
        /v1/persons/A1234BC/images                                    , /v1/persons/{hmppsId}/images
        /v1/persons/A1234BC/images/1234567                            , /v1/persons/{hmppsId}/images/{id}
        /v1/prison/MDI/location/MDI1                                  , /v1/prison/{prisonId}/location/{key}
        /v1/visit/id/by-client-ref/someReferenceValue                 , /v1/visit/id/by-client-ref/{clientReference}
        /v1/visit/ab-cd-ef-gh                                         , /v1/visit/{visitReference}""",
      )
      fun `should match urls to canonical pattern`(input: String, pathTemplate: String) = assertMatchesUrlPatternDoesMatch(input, pathTemplate)

      @ParameterizedTest
      @CsvSource(
        textBlock = """
        /v1/persons/X777776/name                                      , /v1/persons/{hmppsId}
        /v1/persons/X777776/cell-location                             , /v1/persons/{hmppsId}/addresses
        /v1/persons/X777776/education                                 , /v1/persons/{hmppsId}/education/assessments
        /v1/persons/X777776/education/san/plan-creation-schedule      , /v1/persons/{hmppsId}/education/san
        /v1/prison/MDI/prisoners/A1234BC/account/spends/              , /v1/prison/{prisonId}/prisoners/{hmppsId}/account/{accountCode}/transactions
        /v1/visits/ab-cd-ef-gh                                        , /v1/visit/{visitReference}""",
      )
      fun `should not match mismatching urls to canonical pattern`(input: String, pathTemplate: String) = assertMatchesUrlPatternNotMatch(input, pathTemplate)
    }

    private fun assertMatchesUrlPatternDoesMatch(input: String, urlPattern: String) {
      assertMatchesUrlPattern(input, urlPattern).isTrue
    }

    private fun assertMatchesUrlPatternNotMatch(input: String, urlPattern: String) {
      assertMatchesUrlPattern(input, urlPattern).isFalse
    }

    private fun assertMatchesUrlPattern(input: String, urlPattern: String) = assertThat(matchesUrl(input, urlPattern))
  }

  @Nested
  @DisplayName("Given URL pattern to be normalised")
  inner class GivenUrlPatternToBeNormalised {
    private val param = URL_PARAM

    @Nested
    @DisplayName("And URL pattern is Regex")
    inner class AndRegexPattern {
      @Test
      fun `should normalise wildcard #1 except-forward-slash`() = assertNormaliseUrlEquals(
        expectedUrlPattern = "/v1/persons/$param",
        urlPattern = "/v1/persons/[^/]*",
      )

      @Test
      fun `should normalise wildcard #2 dot-asterisk`() = assertNormaliseUrlEquals(
        expectedUrlPattern = "/v1/persons/$param/name",
        urlPattern = "/v1/persons/.*/name",
      )

      @Test
      fun `should normalise wildcard #3 except-forward-slash plus`() = assertNormaliseUrlEquals(
        expectedUrlPattern = "/v1/persons/$param",
        urlPattern = "/v1/persons/[^/]+",
      )

      @Test
      fun `should normalise wildcard #4 dot-plus`() = assertNormaliseUrlEquals(
        expectedUrlPattern = "/v1/persons/$param/addresses",
        urlPattern = "/v1/persons/.*/addresses",
      )

      @Test
      fun `should normalise, ignoring prefix ^`() = assertNormaliseUrlEquals(
        expectedUrlPattern = "/v1/persons/$param",
        urlPattern = "^/v1/persons/.*",
      )

      @Test
      fun `should normalise, ignoring suffix $`() = assertNormaliseUrlEquals(
        expectedUrlPattern = "/v1/persons/$param",
        urlPattern = "/v1/persons/[^/]*$",
      )

      @ParameterizedTest
      @CsvSource(
        textBlock = """
          /v1/persons/[^/]*$                                    , /v1/persons/$PARAM
          /v1/persons/[^/]+/prisoner-base-location              , /v1/persons/$PARAM/prisoner-base-location
          /v1/persons/.*/addresses                              , /v1/persons/$PARAM/addresses
          /v1/persons/.*/cell-location                          , /v1/persons/$PARAM/cell-location
          /v1/persons/.*/education/assessments                  , /v1/persons/$PARAM/education/assessments
          /v1/persons/.*/education/san/plan-creation-schedule   , /v1/persons/$PARAM/education/san/plan-creation-schedule
          /v1/persons/.*/images                                 , /v1/persons/$PARAM/images
          /v1/persons/.*/images/.*                              , /v1/persons/$PARAM/images/$PARAM
          /v1/persons/.*/contacts                               , /v1/persons/$PARAM/contacts
          /v1/persons/.*/contacts[^/]*$                         , /v1/persons/$PARAM/contacts$PARAM                                     , is this desired suffix?
          /v1/prison/prisoners/[^/]*$                           , /v1/prison/prisoners/$PARAM
          /v1/prison/.*/visit/search                            , /v1/prison/$PARAM/visit/search
          /v1/prison/.*/visit/search[^/]*$                      , /v1/prison/$PARAM/visit/search$PARAM                                  , is this desired suffix?
          /v1/prison/.*/prisoners/[^/]*/balances$               , /v1/prison/$PARAM/prisoners/$PARAM/balances
          /v1/prison/.*/prisoners/.*/non-associations           , /v1/prison/$PARAM/prisoners/$PARAM/non-associations
          /v1/prison/.*/prisoners/.*/accounts/.*/balances       , /v1/prison/$PARAM/prisoners/$PARAM/accounts/$PARAM/balances
          /v1/prison/.*/prisoners/.*/accounts/.*/transactions   , /v1/prison/$PARAM/prisoners/$PARAM/accounts/$PARAM/transactions
          /v1/prison/.*/location/[^/]*$                         , /v1/prison/$PARAM/location/$PARAM
          /v1/contacts/[^/]*$                                   , /v1/contacts/$PARAM
          /v1/visit/id/by-client-ref/[^/]*$                     , /v1/visit/id/by-client-ref/$PARAM
          /v1/visit/[^/]*$                                      , /v1/visit/$PARAM""",
      )
      fun `should normalise URL patterns of Regex`(urlPattern: String, expectedUrlPattern: String) = assertNormaliseUrlEquals(expectedUrlPattern, urlPattern)
    }

    @Nested
    @DisplayName("And URL pattern is canonical (path template)")
    inner class AndCanonicalPattern {
      @Test
      fun `should normalise canonical pattern`() = assertNormaliseUrlEquals(
        expectedUrlPattern = "/v1/persons/$param",
        urlPattern = "/v1/persons/{hmppsId}",
      )

      @Test
      fun `should normalise canonical pattern, with leading slash missing`() = assertNormaliseUrlEquals(
        expectedUrlPattern = "/v1/persons/$param",
        urlPattern = "v1/persons/{hmppsId}",
      )

      @ParameterizedTest
      @CsvSource(
        textBlock = """
          /v1/persons/{hmppsId}                                                         , /v1/persons/$PARAM
          /v1/persons/{hmppsId}/prisoner-base-location                                  , /v1/persons/$PARAM/prisoner-base-location
          /v1/persons/{hmppsId}/addresses                                               , /v1/persons/$PARAM/addresses
          /v1/persons/{hmppsId}/cell-location                                           , /v1/persons/$PARAM/cell-location
          /v1/persons/{hmppsId}/education/assessments                                   , /v1/persons/$PARAM/education/assessments
          /v1/persons/{hmppsId}/education/san/plan-creation-schedule                    , /v1/persons/$PARAM/education/san/plan-creation-schedule
          /v1/persons/{hmppsId}/images                                                  , /v1/persons/$PARAM/images
          /v1/persons/{hmppsId}/images/{imageId}                                        , /v1/persons/$PARAM/images/$PARAM
          /v1/persons/{hmppsId}/contacts                                                , /v1/persons/$PARAM/contacts
          /v1/prison/prisoners/{hmppsId}                                                , /v1/prison/prisoners/$PARAM
          /v1/prison/{prisonId}/visit/search                                            , /v1/prison/$PARAM/visit/search
          /v1/prison/{prisonId}/prisoners/{hmppsId}/balances                            , /v1/prison/$PARAM/prisoners/$PARAM/balances
          /v1/prison/{prisonId}/prisoners/{hmppsId}/non-associations                    , /v1/prison/$PARAM/prisoners/$PARAM/non-associations
          /v1/prison/{prisonId}/prisoners/{hmppsId}/accounts/{accountCode}/balances     , /v1/prison/$PARAM/prisoners/$PARAM/accounts/$PARAM/balances
          /v1/prison/{prisonId}/prisoners/{hmppsId}/accounts/{accountCode}/transactions , /v1/prison/$PARAM/prisoners/$PARAM/accounts/$PARAM/transactions
          /v1/prison/{prisonId}/location/{key}                                          , /v1/prison/$PARAM/location/$PARAM
          /v1/contacts/{contactId}                                                      , /v1/contacts/$PARAM
          /v1/visit/id/by-client-ref/{clientReference}                                  , /v1/visit/id/by-client-ref/$PARAM
          /v1/visit/{visitReference}                                                    , /v1/visit/$PARAM""",
      )
      fun `should normalise URL patterns of Regex`(urlPattern: String, expectedUrlPattern: String) = assertNormaliseUrlEquals(expectedUrlPattern, urlPattern)
    }

    @Test
    fun `should return error, with invalid URL pattern`() {
      // URL pattern is mixture of canonical and regex
      val urlPattern = "/v1/persons/{hmppsId}/images/.*"
      val ex = assertThrows<IllegalArgumentException> {
        normaliseUrl(urlPattern)
      }
      assertThat(ex).hasMessageStartingWith("Invalid URL pattern:")
    }

    private fun assertNormaliseUrlEquals(
      expectedUrlPattern: String,
      urlPattern: String,
    ) {
      val actualUrlPattern = normaliseUrl(urlPattern)
      assertThat(actualUrlPattern).isEqualTo(expectedUrlPattern)
    }
  }
}

private const val PARAM = "$URL_PARAM_ALLOWLIST$URL_PARAM_QUANTIFIER"
