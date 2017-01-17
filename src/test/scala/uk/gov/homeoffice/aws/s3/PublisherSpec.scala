package uk.gov.homeoffice.aws.s3

import java.io.File
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}
import com.amazonaws.event.{ProgressEvent, ProgressEventType, ProgressListener}
import com.amazonaws.services.s3.transfer.Upload
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class PublisherSpec(implicit env: ExecutionEnv) extends Specification {
  val fileUploaded: Try[Upload] => Future[Boolean] = {
    case Success(upload) =>
      val fileUploaded = Promise[Boolean]()

      upload.addProgressListener(new ProgressListener {
        override def progressChanged(progressEvent: ProgressEvent): Unit = {
          if (progressEvent.getEventType == ProgressEventType.TRANSFER_COMPLETED_EVENT) {
            fileUploaded success true
          }
        }
      })

      fileUploaded.future

    case Failure(t) =>
      throw t
  }

  "Publisher" should {
    "publish a file" in new S3ServerEmbedded {
      val bucket = "test-bucket"

      val file = new File(s"$s3Directory/test-file.txt")
      val publisher = new Publisher(bucket)

      fileUploaded(publisher.publish(file.getName, file)) must beEqualTo(true).await

      // TODO subscribe

    }

    "publish a files to same bucket" in new S3ServerEmbedded {
      val bucket = "test-bucket"

      // First file to publish
      val file = new File(s"$s3Directory/test-file.txt")
      val publisher = new Publisher(bucket)

      fileUploaded(publisher.publish(file.getName, file)) must beEqualTo(true).await

      // Second file to publish
      val file2 = new File(s"$s3Directory/test-file-2.txt")
      val publisher2 = new Publisher(bucket)

      fileUploaded(publisher2.publish(file2.getName, file2)) must beEqualTo(true).await
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