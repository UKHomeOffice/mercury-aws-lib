package uk.gov.homeoffice.aws.s3.publish

import java.io.File
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import uk.gov.homeoffice.aws.s3.S3ServerEmbedded

class PublisherSpec(implicit env: ExecutionEnv) extends Specification {
  "Publisher" should {
    "publish a file" in new S3ServerEmbedded {
      val bucket = "test-bucket"

      val file = new File(s"$s3Directory/test-file.txt")
      val publisher = new Publisher(bucket)

      publisher.publish(file.getName, file) must beLike[Result] {
        case c: Completed => c.key mustEqual file.getName
      }.await

      // TODO subscribe

    }

    "publish files to same bucket" in new S3ServerEmbedded {
      val bucket = "test-bucket"

      // First file to publish
      val file = new File(s"$s3Directory/test-file.txt")
      val publisher = new Publisher(bucket)

      publisher.publish(file.getName, file) must beLike[Result] {
        case c: Completed => c.key mustEqual file.getName
      }.await

      // Second file to publish
      val file2 = new File(s"$s3Directory/test-file-2.txt")
      val publisher2 = new Publisher(bucket)

      publisher2.publish(file2.getName, file2) must beLike[Result] {
        case c: Completed => c.key mustEqual file2.getName
      }.await
    }
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