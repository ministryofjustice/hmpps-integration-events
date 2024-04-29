package uk.gov.justice.digital.hmpps.hmppsintegrationevents.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "services.integrations-api")
data class IntegrationApiProperties(
        val url: String ,
        val apiKey: String,
        val certificateBucketName: String,
        val certificatePassword: String,
        var certificatePath: String
        ) {

}
