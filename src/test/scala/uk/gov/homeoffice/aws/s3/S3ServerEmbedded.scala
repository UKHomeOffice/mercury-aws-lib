package uk.gov.homeoffice.aws.s3

import java.net.URL
import java.util.UUID
import scala.language.postfixOps
import scala.util.Try
import com.amazonaws.auth.AnonymousAWSCredentials
import org.specs2.execute.{AsResult, Result}
import org.specs2.matcher.Scope
import de.flapdoodle.embed.process.runtime.Network.getFreeServerPort
import grizzled.slf4j.Logging
import io.findify.s3mock.S3Mock
import uk.gov.homeoffice.specs2.ComposableAround

trait S3ServerEmbedded extends S3Server with Scope with ComposableAround with Logging {
  val s3Port = getFreeServerPort
  val s3Host = new URL(s"http://127.0.0.1:$s3Port")

  val s3Directory = "src/test/resources/s3"

  val s3Server = S3Mock(s3Port, s3Directory)
  s3Server start

  implicit lazy val s3Client = new S3Client(s3Host, new AnonymousAWSCredentials())

  lazy val bucket = UUID.randomUUID().toString
  lazy val s3 = new S3(bucket)

  override def around[R: AsResult](r: => R): Result = try {
    info(s"Started S3 $s3Host")
    super.around(r)
  } finally {
    info(s"Stopping S3 $s3Host")

    Try { // Underlying API is a tad odd - checking if bucket exists can throw a wobbly
      if (s3Client.doesBucketExist(bucket)) {
        s3Client.deleteBucket(bucket)
      }
    }

    s3Server stop
  }
}