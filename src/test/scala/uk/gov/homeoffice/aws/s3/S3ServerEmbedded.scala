package uk.gov.homeoffice.aws.s3

import java.net.URL
import scala.language.postfixOps
import com.amazonaws.auth.AnonymousAWSCredentials
import org.specs2.execute.{AsResult, Result}
import org.specs2.matcher.Scope
import de.flapdoodle.embed.process.runtime.Network.getFreeServerPort
import grizzled.slf4j.Logging
import io.findify.s3mock.S3Mock
import uk.gov.homeoffice.specs2.ComposableAround

trait S3ServerEmbedded extends S3Server with Scope with ComposableAround with Logging {
  val s3Port = getFreeServerPort
  val s3Host = new URL(s"http://0.0.0.0:$s3Port")

  val s3Directory = "src/test/resources/s3"

  val server = S3Mock(s3Port, s3Directory)
  server start

  implicit val s3Client = new S3Client(s3Host, new AnonymousAWSCredentials())

  override def around[R: AsResult](r: => R): Result = try {
    info(s"Started S3 $s3Host")
    super.around(r)
  } finally {
    info(s"Stopping S3 $s3Host")
    server stop
  }
}

/*

import java.io.{File, InputStream}
import scala.io.Source
import org.specs2.mutable.Specification
import io.findify.s3mock.S3Mock
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client

class EmailAttachmentSpec extends Specification {
  "blah" should {
    "blah" in {
      // create and start S3 API mock
      val api = S3Mock(port = 8001, dir = "aws/tmp/s3")
      api.start

      // AWS S3 client setup
      val credentials = new AnonymousAWSCredentials()
      val client = new AmazonS3Client(credentials)
      // use IP for endpoint address as AWS S3 SDK uses DNS-based bucket access scheme
      // resulting in attempts to connect to addresses like "bucketname.localhost"
      // which requires specific DNS setup
      client.setEndpoint("http://127.0.0.1:8001")

      // use it as usual
      client.createBucket("email-bucket")
      //client.putObject("email-bucket", "key", "String content")
      client.putObject("email-bucket", "key", new File("aws/tmp/s3/test-file.txt"))

      val inputStream: InputStream = client.getObject("email-bucket", "key").getObjectContent
      println("Got back = " + Source.fromInputStream(inputStream).mkString)

      api.stop

      ok
    }
  }
}

 */