package uk.gov.homeoffice.aws.sqs

import java.net.URL
import akka.actor.{ActorSystem, Props}
import com.amazonaws.auth.BasicAWSCredentials
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import uk.gov.homeoffice.system.Exit

/**
  * Example of booting an application to publish/subscribe to an AWS SQS instance.
  * <pre>
  * 1) Start up an instance of ElasticMQ (to run an instance of AWS SQS locally) - From the root of this project:
  *    java -jar ./libexec/elasticmq-server-<version>.jar
  *    OR with a configuration to e.g. set up queues
  *    java -Dconfig.file=src/test/resources/application.test.conf -jar ./libexec/elasticmq-server-<version>.jar
  *    which starts up a working server that binds to localhost:9324
  * 2) Boot this application:
  *    sbt test:run
  * </pre>
  */
object ExampleBoot extends App {
  val system = ActorSystem("aws-sqs-actor-system")

  implicit val sqsClient = new SQSClient(new URL("http://localhost:9324/queue"), new BasicAWSCredentials("x", "x"))

  val queue = new Queue("test-queue")

  system actorOf Props {
    new SQSActor(new SQS(queue)) with ExampleSubscription
  }

  new SQS(queue) publish compact(render("input" -> "blah"))
}

/**
  * Example of processing JSON subscription.
  */
trait ExampleSubscription extends JsonSubscription with Exit {
  this: SQSActor =>

  def receive: Receive = {
    case m: Message => exitAfter {
      val result = s"Well Done! Processed given message $m"
      println(result)
      result
    }
  }
}