package uk.gov.homeoffice.aws.s3

import java.io.File
import java.net.URL
import java.util.Date
import scala.concurrent.duration.{Duration, _}
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
import uk.gov.homeoffice.aws.s3.S3.ResourcesKey
import uk.gov.homeoffice.specs2.ComposableAround

/**
  * Test and example of using S3 to interact with AWS S3.
  * Note the function "oldEnough" which shows an example of filtering the S3 resources by their "time".
  * @param env ExecutionEnv For asynchronous testing
  */
class S3Spec(implicit env: ExecutionEnv) extends Specification {
  sequential

  trait Context extends ComposableAround {
    var s3: S3 = _

    /**
      * Duration Acquired resources must have been uploaded at least a "duration" amount of time in the past
      */
    val oldEnough: (Date, Duration) => Boolean = { (date, olderThan) =>
      date before new Date(System.currentTimeMillis() - olderThan.toMillis)
    }

    override def around[R: AsResult](r: => R): Result = try {
      val config = ConfigFactory.load("application.it.conf")

      val s3Host = new URL(config.getString("aws.s3.uri"))

      val accessKey = config.getString("aws.s3.credentials.access-key")
      val secretKey = config.getString("aws.s3.credentials.secret-key")

      implicit val clientConfiguration = new ClientConfiguration().withRetryPolicy(PredefinedRetryPolicies.NO_RETRY_POLICY)
      implicit val s3Client = new S3Client(s3Host, new BasicAWSCredentials(accessKey, secretKey))
      s3Client.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build())

      s3 = new uk.gov.homeoffice.aws.s3.S3(config.getString("aws.s3.buckets.example"))

      super.around(r)
    } finally {
      // Need to close everything down (gracefully) if running in sbt interactive mode, we don't want anything hanging around.
      Try {
        s3.s3Client.deleteBucket(s3.s3Bucket.getName)
        s3.s3Client.shutdown()
      }
    }
  }

  "S3" should {
    "allow file to be pushed" in new Context {
      val file = new File("src/it/resources/s3/test-file-1.txt")

      s3.push(file.getName, file) must beLike[Push] {
        case Push.Completed(fileName, _, _) => fileName mustEqual file.getName
      }.await
    }

    "allow files to be pushed and all pulled back" in new Context {
      val resources = for {
      _ <- s3.push("blah/sub1", new File("src/it/resources/s3/test-file-1.txt"))
      _ <- s3.push("blah/sub2", new File("src/it/resources/s3/test-file-2.txt"))
      _ <- s3.push("blah1", new File("src/it/resources/s3/test-file-1.txt"))
      _ <- s3.push("blah2", new File("src/it/resources/s3/test-file-2.txt"))
        resources <- s3.pullResources()
      } yield resources

      resources must beLike[Map[ResourcesKey, Seq[Resource]]] {
        case m =>
          println(s"Pulled resources: ${m.mkString(", ")}")
          m("blah").size mustEqual 2
          m("blah1").size mustEqual 1
          m("blah2").size mustEqual 1
      }.await
    }

    "allow files to be pushed and all pulled back, but filter out by files not being old enough" in new Context {
      val resources = for {
        _ <- s3.push("blah/sub1", new File("src/it/resources/s3/test-file-1.txt"))
        _ <- s3.push("blah/sub2", new File("src/it/resources/s3/test-file-2.txt"))
        _ <- s3.push("blah1", new File("src/it/resources/s3/test-file-1.txt"))
        _ <- s3.push("blah2", new File("src/it/resources/s3/test-file-2.txt"))
        resources <- s3.pullResources()
      } yield {
        resources mapValues { resources =>
          resources.filter { r => oldEnough(r.lastModifiedDate, 30 seconds) }
        } filterNot {
          case (_, rs) => rs.isEmpty
        }
      }

      resources must beEqualTo(Map.empty).await
    }

    "push an AES256 encrypted file and pull it back" in new Context {
      pending

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