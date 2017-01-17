package uk.gov.homeoffice.aws.s3

import java.net.URL
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.AmazonS3Client

class S3Client(endpoint: URL, credentials: AWSCredentials) extends AmazonS3Client(credentials) {
  setEndpoint(endpoint.toString)
}