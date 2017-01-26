package uk.gov.homeoffice.aws.sqs

import org.specs2.mutable.Specification

class SQSPublicationSpec extends Specification {
  "SQS" should {
    "publish some text" in new SQSServerEmbedded {
      val queue = create(new Queue("test-queue"))

      val publisher = new SQS(queue)
      publisher publish "Testing 1, 2, 3"

      val subscriber = new SQS(queue)

      subscriber.receive must beLike {
        case Seq(m: Message) => m.content mustEqual "Testing 1, 2, 3"
      }
    }
  }
}