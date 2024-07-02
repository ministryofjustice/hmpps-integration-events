package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.registration

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class HmppsDomainEventMessage(
  @JsonProperty("eventType") val eventType: String,
  @JsonProperty("occurredAt") val occurredAt: String,
  @JsonProperty("personReference") val personReference: PersonReference,
  @JsonProperty("additionalInformation") val additionalInformation: AdditionalInformation,
  @JsonProperty("reason") val reason: String?= null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PersonReference(
  @JsonProperty("identifiers") val identifiers: List<Identifier>,
) {
  fun findCrnIdentifier(): String? {
    return this.identifiers.firstOrNull { it.type == "CRN" }?.value
  }
  fun findNomsIdentifier(): String? {
    return this.identifiers.firstOrNull { it.type == "nomsNumber" }?.value
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Identifier(
  @JsonProperty("type") val type: String,
  @JsonProperty("value") val value: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AdditionalInformation(
  @JsonProperty("registerTypeDescription") val registerTypeDescription: String? = null,
  @JsonProperty("registerTypeCode") val registerTypeCode: String? = null,
) {
  fun isMappRegistrationType(): Boolean = (
    this.registerTypeCode == "MAPP"
    )
}
