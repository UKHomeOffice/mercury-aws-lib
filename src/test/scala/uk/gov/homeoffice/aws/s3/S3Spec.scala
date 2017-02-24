package uk.gov.homeoffice.aws.s3

import java.io.File
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.io.Source
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.retry.PredefinedRetryPolicies
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import uk.gov.homeoffice.aws.s3.S3._

class S3Spec(implicit env: ExecutionEnv) extends Specification {
  "S3" should {
    "push a file and pull it back" in new S3ServerEmbedded {
      val file = new File(s"$s3Directory/test-file.txt")

      s3.push(file.getName, file) must beLike[Push] {
        case c: Push.Completed => c.key mustEqual file.getName
      }.await

      s3.pullResource(file.getName) must beLike[Resource] {
        case Resource(key, inputStream, contentType, numberOfBytes) =>
          key mustEqual file.getName
          Source.fromInputStream(inputStream).mkString mustEqual "blah blah"
          contentType must startWith("text/plain")
          numberOfBytes mustEqual 9
      }.await
    }

    "push files to same bucket and pull them back" in new S3ServerEmbedded {
      val file1 = new File(s"$s3Directory/test-file.txt")
      val fileName1 = file1.getName

      val file2 = new File(s"$s3Directory/test-file-2.txt")
      val fileName2 = file2.getName

      val resources = for {
        Push.Completed(`fileName1`, _, _) <- s3.push(file1.getName, file1)
        _ = TimeUnit.SECONDS.sleep(1) // To make sure timestamps are different for pushed resources, making assertions in this test deterministic.
        Push.Completed(`fileName2`, _, _) <- s3.push(file2.getName, file2)
        resource1 <- s3.pullResource(file1.getName)
        resource2 <- s3.pullResource(file2.getName)
      } yield (resource1, resource2)

      resources must beLike[(Resource, Resource)] {
        case (r1, r2) =>
          r1.key mustEqual file1.getName
          Source.fromInputStream(r1.inputStream).mkString mustEqual "blah blah"
          r1.contentType must startWith("text/plain")
          r1.numberOfBytes mustEqual 9

          r2.key mustEqual file2.getName
          Source.fromInputStream(r2.inputStream).mkString mustEqual "blah blah 2"
          r2.contentType must startWith("text/plain")
          r2.numberOfBytes mustEqual 11
      }.awaitFor(10 seconds)
    }

    "push files to folder in bucket and pull them back" in new S3ServerEmbedded {
      val file1 = new File(s"$s3Directory/test-file.txt")

      val file2 = new File(s"$s3Directory/test-file-2.txt")

      val resources = for {
        Push.Completed(_, _, _) <- s3.push(s"folder/${file1.getName}", file1)
        _ = TimeUnit.SECONDS.sleep(1) // To make sure timestamps are different for pushed resources, making assertions in this test deterministic.
        Push.Completed(_, _, _) <- s3.push(s"folder/${file2.getName}", file2)
        resources <- s3 pullResources "folder/"
      } yield resources

      resources must beLike[Seq[Resource]] {
        case Seq(Resource(key1, inputStream1, contentType1, numberOfBytes1), Resource(key2, inputStream2, contentType2, numberOfBytes2)) =>
          key1 mustEqual s"folder/${file1.getName}"
          Source.fromInputStream(inputStream1).mkString mustEqual "blah blah"
          contentType1 must startWith("text/plain")
          numberOfBytes1 mustEqual 9

          key2 mustEqual s"folder/${file2.getName}"
          Source.fromInputStream(inputStream2).mkString mustEqual "blah blah 2"
          contentType2 must startWith("text/plain")
          numberOfBytes2 mustEqual 11
      }.awaitFor(10 seconds)
    }

    "push files to two separate folders in bucket and pull them back" in new S3ServerEmbedded {
      val file1 = new File(s"$s3Directory/test-file.txt")

      val file2 = new File(s"$s3Directory/test-file-2.txt")

      val resources = for {
        // Folder 1
        Push.Completed(_, _, _) <- s3.push(s"folder1/${file1.getName}", file1)
        _ = TimeUnit.SECONDS.sleep(1) // To make sure timestamps are different for pushed resources, making assertions in this test deterministic.
        Push.Completed(_, _, _) <- s3.push(s"folder1/${file2.getName}", file2)
        // Folder 2
        Push.Completed(_, _, _) <- s3.push(s"folder2/${file1.getName}", file1)
        _ = TimeUnit.SECONDS.sleep(1) // To make sure timestamps are different for pushed resources, making assertions in this test deterministic.
        Push.Completed(_, _, _) <- s3.push(s"folder2/${file2.getName}", file2)

        resources <- s3 pullResources

      } yield resources

      resources.map(_.toSeq.filterNot(_._1 == "").sortBy(_._1)) must beLike[Seq[(ResourcesKey, Seq[Resource])]] {
        case Seq(("folder1", Seq(firstResource1, firstResource2)), ("folder2", Seq(secondResource1, secondResource2))) =>
          // Folder 1
          firstResource1.key mustEqual s"folder1/${file1.getName}"
          Source.fromInputStream(firstResource1.inputStream).mkString mustEqual "blah blah"
          firstResource1.contentType must startWith("text/plain")
          firstResource1.numberOfBytes mustEqual 9

          firstResource2.key mustEqual s"folder1/${file2.getName}"
          Source.fromInputStream(firstResource2.inputStream).mkString mustEqual "blah blah 2"
          firstResource2.contentType must startWith("text/plain")
          firstResource2.numberOfBytes mustEqual 11

          // Folder 2
          secondResource1.key mustEqual s"folder2/${file1.getName}"
          Source.fromInputStream(secondResource1.inputStream).mkString mustEqual "blah blah"
          secondResource1.contentType must startWith("text/plain")
          secondResource1.numberOfBytes mustEqual 9

          secondResource2.key mustEqual s"folder2/${file2.getName}"
          Source.fromInputStream(secondResource2.inputStream).mkString mustEqual "blah blah 2"
          secondResource2.contentType must startWith("text/plain")
          secondResource2.numberOfBytes mustEqual 11
      }.awaitFor(10 seconds)
    }

    "push an input stream" in new S3ServerEmbedded {
      todo
    }

    "push a non existing file" in new S3ServerEmbedded {
      val file = new File(s"whoops.txt")

      s3.push(file.getName, file) must throwAn[Exception](file.getName).await
    }

    "pull a non existing object" in new S3ServerEmbedded {
      s3.pullResource("whoops.text") must throwAn[Exception]("NoSuchKey").await
    }

    "configured" in new S3ServerEmbedded {
      override implicit val s3Client: S3Client = new S3Client(s3Host, new AnonymousAWSCredentials())(new ClientConfiguration().withRetryPolicy(PredefinedRetryPolicies.NO_RETRY_POLICY))

      s3Client.clientConfig.getRetryPolicy mustEqual PredefinedRetryPolicies.NO_RETRY_POLICY
    }

    "configured implicitly" in new S3ServerEmbedded {
      implicit val clientConfiguration = new ClientConfiguration().withRetryPolicy(PredefinedRetryPolicies.NO_RETRY_POLICY)

      override implicit val s3Client: S3Client = new S3Client(s3Host, new AnonymousAWSCredentials())

      s3Client.clientConfig.getRetryPolicy mustEqual PredefinedRetryPolicies.NO_RETRY_POLICY
    }
  }
}