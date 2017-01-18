package uk.gov.homeoffice.aws.sqs.publish

import org.specs2.mutable.Specification
import uk.gov.homeoffice.aws.sqs.subscribe.Subscriber
import uk.gov.homeoffice.aws.sqs.{Message, Queue, SQSServerEmbedded}

class PublisherSpec extends Specification {
  "Publisher" should {
    "publish some text" in new SQSServerEmbedded {
      val queue = create(new Queue("test-queue"))

      val publisher = new Publisher(queue)
      publisher publish "Testing 1, 2, 3"

      val subscriber = new Subscriber(queue)

      subscriber.receive must beLike {
        case Seq(m: Message) => m.content mustEqual "Testing 1, 2, 3"
      }
    }
  }
}