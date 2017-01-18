package uk.gov.homeoffice.aws.sqs.subscribe

import uk.gov.homeoffice.aws.sqs.Message

package object protocol {

  sealed trait Protocol

  case class Processed(message: Message) extends Protocol

  case class ProcessingError(throwable: Throwable, message: Message) extends Protocol
}