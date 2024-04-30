package uk.gov.justice.digital.hmpps.hmppsintegrationevents.services

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.http.AbortableInputStream
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import java.io.ByteArrayInputStream
import java.io.InputStream

class S3ServiceTest {

  val s3Client: S3Client = mock()
  private lateinit var service: S3Service

  @BeforeEach
  fun setUp() {
    service = S3Service(s3Client)
  }

  @Test
  fun `getDocumentFile calls S3 client and returns file content as InputStream`() {
    val bucketName = "test-bucket"
    val fileName = "test.file"
    val fileContent = "This is a test file content."
    val mockStream: InputStream = ByteArrayInputStream(fileContent.toByteArray())
    val getObjectResponse = ResponseInputStream(
      GetObjectResponse.builder().build(),
      AbortableInputStream.create(mockStream),
    )

    whenever(s3Client.getObject(any<GetObjectRequest>())).thenReturn(getObjectResponse)

    val documentStream = service.getDocumentFile(bucketName, fileName)

    argumentCaptor<GetObjectRequest>().apply {
      verify(s3Client, times(1)).getObject(capture())
      Assertions.assertThat(firstValue.bucket()).isEqualTo(bucketName)
      Assertions.assertThat(firstValue.key()).isEqualTo(fileName)
    }
    Assertions.assertThat(documentStream.readAllBytes().decodeToString()).isEqualTo(fileContent)
  }
}
