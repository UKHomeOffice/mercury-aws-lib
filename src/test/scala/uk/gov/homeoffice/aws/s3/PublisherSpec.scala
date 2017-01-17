package uk.gov.homeoffice.aws.s3

import java.io.File
import org.specs2.mutable.Specification

class PublisherSpec extends Specification {
  "Publisher" should {
    "publish a file" in new S3ServerEmbedded {
      val bucket = "test-bucket"

      val publisher = new Publisher(bucket)
      println(publisher.publish("test-file.txt", new File(s"$s3Directory/test-file.txt")))

      val publisher2 = new Publisher(bucket)
      println(publisher2.publish("test-file-2.txt", new File(s"$s3Directory/test-file-2.txt")))


      ok
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