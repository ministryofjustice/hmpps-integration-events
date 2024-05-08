package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import uk.gov.justice.digital.hmpps.hmppsintegrationevents.config.HmppsS3Properties
import java.io.InputStream

@Service
@EnableConfigurationProperties(
  HmppsS3Properties::class,
)
class S3Service(private val s3Client: S3Client) {
  fun getDocumentFile(bucketName: String, fileName: String): InputStream {
    val request = GetObjectRequest.builder()
      .bucket(bucketName)
      .key(fileName)
      .build()
    return s3Client.getObject(request)
  }
}
