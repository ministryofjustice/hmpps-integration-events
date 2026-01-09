package uk.gov.justice.digital.hmpps.hmppsintegrationevents.extensions

const val DEFAULT_PATH_PLACEHOLDER = "[a-zA-Z0-9_-]+"

/*
 * URL/Path Matching
 * - The underlying problem is that we use different formats for path expressions in different areas of the code and config, across the 2 repos, which makes it very hard to compare them.
 * - Specifically it makes it hard to match URL patterns in integration event types with role permissions, but there are other scenarios where it makes things tricky.
 *
 * The solution has two components to it...
 * - in the long term, use a "canonical" URL pattern everywhere (code and config, both repos) so you can easily see that two patterns are the same with the mark 1 eyeball
 * - in the short term, normalise all URL path regular expressions into a common format, so that we can match them even when they use different formats for placeholders
 *
 * "Canonical" refers to the format that should be used for URL patterns in our code & config (changing to canonical format could be a task with later PR)
 * "Normalised" refers to the regular expressions that the code ends up comparing and matching against
 */

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
 * @param pathPattern A URL pattern can be normalised by [normalisePath]
 */
fun matchesUrl(input: String, pathPattern: String): Boolean = normalisePath(pathPattern).toRegex().matches(input.substringBefore('?'))

/**
 * Normalise path pattern
 *
 * - Examples:
 *    - Regex URL: `/v1/persons/.*`
 *    - Canonical URL: with named parameter: `/v1/persons/{hmppsId}`
 * - Normalised URL: Regex with parameter allowlist: `/v1/persons/[a-zA-Z0-9_\-]+`
 *
 * @param pathPattern The path pattern to be normalised
 * @return A normalised pattern
 */
fun normalisePath(pathPattern: String): String {
  val placeholders = arrayOf(
    "{hmppsId}",
    "{prisonId}",
    "{contactId}",
    "{visitReference}",
    "{scheduleId}",
    "{key}",
    "{locationKey}",
    "{id}",
    "{imageId}",
    "{accountCode}",
    "{clientReference}",
    "{clientVisitReference}",
    "[^/]*",
    "[^/]+",
    ".*",
  )
  return pathPattern
    .normalisePathPlaceholders(*placeholders)
    .removePrefix("^")
    .removeSuffix("$")
    .ensurePrefix("/")
}

private fun String.ensurePrefix(prefix: String) = if (startsWith(prefix)) this else "$prefix$this"

private fun String.normalisePathPlaceholders(vararg placeholders: String) = placeholders.fold(this) { path, placeholder ->
  path.normalisePathPlaceholder(placeholder)
}

private fun String.normalisePathPlaceholder(placeholder: String) = replace(placeholder, DEFAULT_PATH_PLACEHOLDER)
