package uk.gov.homeoffice.aws.sqs

import java.net.URL

trait SQSServer {
  def sqsHost: URL
}