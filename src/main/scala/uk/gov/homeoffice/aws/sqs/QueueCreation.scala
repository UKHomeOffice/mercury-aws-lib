package uk.gov.homeoffice.aws.sqs

import grizzled.slf4j.Logging

trait QueueCreation extends Logging {
  def create(queue: Queue)(implicit sqsClient: SQSClient): Queue = {
    def createQueue(queueName: String) = try {
      info(s"Creating queue $queueName")
      sqsClient createQueue queueName
    } catch {
      case t: Throwable => warn(s"Application has not created queue $queueName - Exception: ${t.getMessage}")
    }

    createQueue(queue.queueName)
    createQueue(queue.errorQueueName)
    queue
  }
}