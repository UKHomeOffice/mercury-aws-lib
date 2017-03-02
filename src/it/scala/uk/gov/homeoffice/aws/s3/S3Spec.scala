package uk.gov.homeoffice.aws.s3

import java.io.File
import java.net.URL
import scala.io.Source
import scala.util.Try
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.retry.PredefinedRetryPolicies
import com.amazonaws.services.s3.S3ClientOptions
import com.typesafe.config.ConfigFactory
import org.specs2.concurrent.ExecutionEnv
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.Specification
import uk.gov.homeoffice.specs2.ComposableAround

class S3Spec(implicit env: ExecutionEnv) extends Specification {
  trait Context extends ComposableAround {
    var s3: S3 = _

    override def around[R: AsResult](r: => R): Result = try {
      val config = ConfigFactory.load("application.it.conf")

      val s3Host = new URL(config.getString("aws.s3.uri"))

      val accessKey = config.getString("aws.s3.credentials.access-key")
      val secretKey = config.getString("aws.s3.credentials.secret-key")

      implicit val clientConfiguration = new ClientConfiguration().withRetryPolicy(PredefinedRetryPolicies.NO_RETRY_POLICY)
      implicit val s3Client = new S3Client(s3Host, new BasicAWSCredentials(accessKey, secretKey))
      s3Client.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build())

      s3 = new uk.gov.homeoffice.aws.s3.S3(config.getString("aws.s3.buckets.my-bucket"))

      super.around(r)
    } finally {
      // Need to close everything down (gracefully) if running in sbt interactive mode, we don't want anything hanging around.
      Try { s3.s3Client.shutdown() }
    }
  }

  "S3" should {
    "push an AES256 encrypted file and pull it back" in new Context {
      val file = new File(s"./src/it/resources/test-file.txt")

      s3.push(file.getName, file, Some(AES256("secret key"))) must beLike[Push] {
        case c: Push.Completed => c.key mustEqual file.getName
      }.await

      s3.pullResource(file.getName) must beLike[Resource] {
        case Resource(key, inputStream, contentType, numberOfBytes, _) =>
          Source.fromInputStream(inputStream).mkString mustEqual "blah blah"
          contentType must startWith("text/plain")
          numberOfBytes mustEqual 9
      }.await
    }
  }
}