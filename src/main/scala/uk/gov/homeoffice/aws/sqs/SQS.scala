package uk.gov.homeoffice.aws.sqs

import scala.collection.JavaConversions._
import com.amazonaws.services.sqs.model.SendMessageResult

class SQS(val queue: Queue)(implicit val sqsClient: SQSClient) extends QueueCreation {
  create(queue)

  def publish(message: String): MessageID = publish(message, queue.queueName).getMessageId

  def publishError(message: String): MessageID = publish(message, queue.errorQueueName).getMessageId

  def receive: Seq[Message] = receive(queue.queueName)

  def receiveErrors: Seq[Message] = receive(queue.errorQueueName)

  private def publish(message: String, queueName: String): SendMessageResult = sqsClient.sendMessage(queueUrl(queueName), message)

  private def receive(queueName: String): Seq[Message] = try {
    sqsClient.receiveMessage(queueUrl(queueName)).getMessages.map(Message)
  } catch {
    case t: Throwable =>
      t.printStackTrace()
      Nil
  }
}