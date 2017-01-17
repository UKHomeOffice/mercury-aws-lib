package uk.gov.homeoffice.aws.s3

import java.net.URL

trait S3Server {
  def s3Host: URL
}