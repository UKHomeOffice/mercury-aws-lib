package uk.gov.homeoffice.aws.sqs.subscribe

import scala.collection.JavaConversions._
import uk.gov.homeoffice.aws.sqs._
import uk.gov.homeoffice.aws.sqs.{QueueCreation, SQSClient}

class Subscriber(val queue: Queue)(implicit val sqsClient: SQSClient) extends QueueCreation {
  create(queue)

  def receive: Seq[Message] = receive(queue.queueName)

  def receiveErrors: Seq[Message] = receive(queue.errorQueueName)

  private def receive(queueName: String): Seq[Message] = try {
    sqsClient.receiveMessage(queueUrl(queueName)).getMessages.map(Message)
  } catch {
    case t: Throwable =>
      t.printStackTrace()
      Nil
  }
}