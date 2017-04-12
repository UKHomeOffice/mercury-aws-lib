package uk.gov.homeoffice.aws.s3

import java.io.File
import java.net.URL
import java.util.UUID
import scala.concurrent.duration._
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.retry.PredefinedRetryPolicies
import com.amazonaws.services.s3.S3ClientOptions
import org.specs2.concurrent.ExecutionEnv
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.Specification
import uk.gov.homeoffice.aws.s3.S3.ResourcesKey
import uk.gov.homeoffice.configuration.HasConfig
import uk.gov.homeoffice.specs2.ComposableAround

/**
  * Testing and example of using S3 to interact with AWS S3.
  * @param env ExecutionEnv For asynchronous testing
  */
class S3Spec(implicit env: ExecutionEnv) extends Specification with HasConfig {
  sequential

  trait Context extends ComposableAround {
    val s3: S3 = {
      val s3Host = new URL(config.getString("aws.s3.uri"))

      val accessKey = config.getString("aws.s3.credentials.access-key")
      val secretKey = config.getString("aws.s3.credentials.secret-key")

      implicit val clientConfiguration = new ClientConfiguration().withRetryPolicy(PredefinedRetryPolicies.NO_RETRY_POLICY)
      implicit val s3Client = new S3PlainClient(s3Host, new BasicAWSCredentials(accessKey, secretKey))
      s3Client.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build())

      new uk.gov.homeoffice.aws.s3.S3(UUID.randomUUID().toString)
    }

    override def around[R: AsResult](r: => R): Result = try {
      super.around(r)
    } finally {
      // Need to close everything down (gracefully) if running in sbt interactive mode, we don't want anything hanging around.
      s3.s3Client.asInstanceOf[S3PlainClient].shutdown()
    }
  }

  "S3" should {
    "allow file to be pushed" in new Context {
      val file = new File("src/it/resources/s3/test-file-1.txt")

      s3.push(file.getName, file) must beLike[Push] {
        case Push.Completed(fileName, _, _) => fileName mustEqual file.getName
      }.awaitFor(30 seconds)
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
      }.awaitFor(30 seconds)
    }

    /*"push an AES256 encrypted file and pull it back" in new Context {
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
    }*/
  }
}