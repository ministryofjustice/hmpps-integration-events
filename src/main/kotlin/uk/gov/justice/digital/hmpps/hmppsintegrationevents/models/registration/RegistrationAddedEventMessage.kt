package uk.gov.justice.digital.hmpps.hmppsintegrationevents.models.registration

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class RegistrationAddedEventMessage(
  @JsonProperty("occurredAt") val occurredAt: String,
  @JsonProperty("personReference") val personReference: PersonReference,
  @JsonProperty("additionalInformation") val additionalInformation: AdditionalInformation,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PersonReference(
  @JsonProperty("identifiers") val identifiers: List<Identifier>,
) {
  fun findCrnIdentifier(): String? {
    return this.identifiers.firstOrNull { it.type == "CRN" }?.value
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Identifier(
  @JsonProperty("type") val type: String,
  @JsonProperty("value") val value: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AdditionalInformation(
  @JsonProperty("registerTypeDescription") val registerTypeDescription: String,
  @JsonProperty("registerTypeCode") val registerTypeCode: String,
) {
  fun isMappRegistrationType(): Boolean = (
    this.registerTypeCode == "MAPP"
    )
}
