package uk.gov.homeoffice.aws.s3

import java.net.URL
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.AmazonS3Client

class S3Client(endpoint: URL, credentials: AWSCredentials)(implicit clientConfiguration: ClientConfiguration = new ClientConfiguration) extends AmazonS3Client(credentials, clientConfiguration) {
  setEndpoint(endpoint.toString)

  def clientConfig = clientConfiguration
}