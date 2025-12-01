package uk.gov.justice.digital.hmpps.hmppsintegrationevents.extensions

const val URL_PARAM_ALLOWLIST = "[a-zA-Z0-9_-]"
const val URL_PARAM_QUANTIFIER = "+"
const val URL_PARAM = URL_PARAM_ALLOWLIST + URL_PARAM_QUANTIFIER

/**
 * Match URL with given URL pattern
 *
 * - URL canonical pattern, with named parameters, e.g.
 *    - /v1/persons/{hmppsId}
 *    - /v1/persons/{hmppsId}/education/assessments
 *    - /v1/prison/{prisonId}/prisoners/{hmppsId}/accounts/{accountCode}/balances
 * - wildcard paths in regex like these are not supported: /.*
 *
 * @param input An actual url, not pattern
 * @param urlPattern A URL pattern can be normalised by [normaliseUrl]
 */
fun matchesUrl(input: String, urlPattern: String): Boolean = normaliseUrl(urlPattern).toRegex().matches(input)

/**
 * Normalise URL pattern to RegEx pattern
 * e.g.
 * Regex URL:
 *    /v1/persons/.*
 * Canonical URL: with named parameter(s
 *    /v1/persons/{hmppsId}
 * Normalised URL: Regex with parameter allowlist
 *    /v1/persons/[a-zA-Z0-9_\-]+
 *
 * @param urlPattern The URL pattern to be normalised
 * @return A normalised Regex pattern
 * @throws IllegalArgumentException when urlPattern is invalid (e.g. mixture of regex and named parameter)
 */
fun normaliseUrl(urlPattern: String) = when (determinePatternType(urlPattern)) {
  "Regex" -> urlPattern.normalisedFromRegex()
  "PathTemplate" -> urlPattern.normalisedFromCanonicalUrl()
  "Mixed" -> throw IllegalArgumentException("Invalid URL pattern: $urlPattern")
  else -> urlPattern
}.addPrefixIfMissing("/").cleansedFromNormalisedUrl()

private val regexWildcards by lazy {
  listOf(
    "\\.", // Wildcard .
    "\\[\\^\\/\\]", // Wildcard [^/]
  ).joinToString(separator = "|").let { Regex(it) }
}
private val regexQuantifiers by lazy { Regex("\\*") }
private val pathTemplatePattern by lazy { Regex("\\{[^}]+}") }

/**
 * Determine type of URL pattern.
 *
 * @return string of either one: `Regex`, `PathTemplate`, `Mixed` or `Otherwise`
 */
private fun determinePatternType(input: String): String {
  // Heuristic for Regex: contains regex-specific symbols
  val regexIndicators = listOf("^", "$", ".*", ".+", "\\d", "\\w", "[", "]", "(", ")")
  val isRegex = regexIndicators.any { input.contains(it) }

  // Heuristic for Spring Flux path template: contains {variable} placeholders
  val isPathTemplate = pathTemplatePattern.containsMatchIn(input)

  return when {
    isRegex && !isPathTemplate -> "Regex"
    isPathTemplate && !isRegex -> "PathTemplate"
    isRegex && isPathTemplate -> "Mixed"
    else -> "Otherwise"
  }
}

/**
 * Normalise a regex pattern:
 * - remove prefix ^ and suffix $
 * - replace Regex wildcards with allowlist
 * - enforce quantifier + (replacing all *)
 *
 * - supporting wildcard:
 *    - `.`
 *    - `[^/]`
 * - enforcing quantifier `+`, by replacing:
 *    - `*`
 * @receiver A string of Regex URL pattern
 */
private fun String.normalisedFromRegex() = this.removePrefix("^").removeSuffix("$")
  .removeWildcardSuffix("\\[\\^/]\\*", "[^/]*")
  .replace(regexWildcards, URL_PARAM_ALLOWLIST)
  .replace(regexQuantifiers, URL_PARAM_QUANTIFIER)

/**
 * Normalise canonical path to regex,
 * e.g. /v1/persons/{hmppsId} to /v1/persons/[a-zA-Z0-9-]*
 *
 * @receiver A string of canonical URL pattern
 */
private fun String.normalisedFromCanonicalUrl() = replace(Regex("""\{[^}]+}"""), URL_PARAM)

/**
 * Cleanse an already-normalised URL
 * query parameters (anything from the first ? onwards) will be ignored.
 * @receiver A string of normalised URL pattern
 */
private fun String.cleansedFromNormalisedUrl(): String = this.substringBefore('?')
  .substringBefore('#')
  .trimEnd('/')

/**
 * This is for compatibility: A leading slash `/` is expected, but it may be absent.
 *
 * @receiver A string of URL pattern
 */
private fun String.addPrefixIfMissing(prefix: String) = if (startsWith(prefix)) this else "/$this"

/**
 * This is for compatibility: remove undesired wildcard suffix that is not mappable to named parameter
 *  e.g. /v1/prison/.+/visit/search[^/]*$  ===> /v1/prison/.+/visit/search
 */
private fun String.removeWildcardSuffix(pattern: String, suffix: String) = if (endsWith(suffix)) {
  this.replace(Regex("[^/]$pattern")) { it.value.removeSuffix(suffix) }
} else {
  this
}
