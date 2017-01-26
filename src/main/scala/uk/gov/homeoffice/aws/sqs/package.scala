package uk.gov.homeoffice.aws

package object sqs {
  type MessageID = String

  val `not-aws-sqs-message` = "Not AWS SQS Message"

  def queueUrl(queueName: String)(implicit sqsClient: SQSClient): String = s"${sqsClient.sqsHost}/$queueName"

  case object Subscribe
}