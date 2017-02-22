package uk.gov.homeoffice.aws.s3

import java.io.File
import java.util.concurrent.TimeUnit
import scala.io.Source
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.retry.PredefinedRetryPolicies
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class S3Spec(implicit env: ExecutionEnv) extends Specification {
  "S3" should {
    "push a file and pull it back" in new S3ServerEmbedded {
      val bucket = "test-bucket"
      val s3 = new S3(bucket)

      val file = new File(s"$s3Directory/test-file.txt")

      s3.push(file.getName, file) must beLike[Push] {
        case c: Push.Completed => c.key mustEqual file.getName
      }.await

      s3.pullResource(file.getName) must beLike[Pull] {
        case Pull(key, inputStream, contentType, numberOfBytes) =>
          key mustEqual file.getName
          Source.fromInputStream(inputStream).mkString mustEqual "blah blah"
          contentType must startWith("text/plain")
          numberOfBytes mustEqual 9
      }.await
    }

    "push files to same bucket and pull them back" in new S3ServerEmbedded {
      val bucket = "test-bucket"

      // First file to push
      val s3First = new S3(bucket)
      val file1 = new File(s"$s3Directory/test-file.txt")

      s3First.push(file1.getName, file1) must beLike[Push] {
        case Push.Completed(key, _, _) => key mustEqual file1.getName
      }.await

      // Second file to push
      val s3Second = new S3(bucket)
      val file2 = new File(s"$s3Directory/test-file-2.txt")

      s3Second.push(file2.getName, file2) must beLike[Push] {
        case Push.Completed(key, _, _) => key mustEqual file2.getName
      }.await

      // And pull them back
      s3First.pullResource(file1.getName) must beLike[Pull] {
        case Pull(key, inputStream, contentType, numberOfBytes) =>
          key mustEqual file1.getName
          Source.fromInputStream(inputStream).mkString mustEqual "blah blah"
          contentType must startWith("text/plain")
          numberOfBytes mustEqual 9
      }.await

      s3Second.pullResource(file2.getName) must beLike[Pull] {
        case Pull(key, inputStream, contentType, numberOfBytes) =>
          key mustEqual file2.getName
          Source.fromInputStream(inputStream).mkString mustEqual "blah blah 2"
          contentType must startWith("text/plain")
          numberOfBytes mustEqual 11
      }.await
    }

    "push files to folder in bucket and pull them back" in new S3ServerEmbedded {
      val bucket = "test-bucket"

      // First file to push
      val s3First = new S3(bucket)
      val file1 = new File(s"$s3Directory/test-file.txt")

      s3First.push(s"folder/${file1.getName}", file1) must beLike[Push] {
        case Push.Completed(key, _, _) => key mustEqual file1.getName
      }.await

      TimeUnit.SECONDS.sleep(1) // Pause so that files are stored at different times for timestamp ordering when pulling back in

      // Second file to push
      val s3Second = new S3(bucket)
      val file2 = new File(s"$s3Directory/test-file-2.txt")

      s3Second.push(s"folder/${file2.getName}", file2) must beLike[Push] {
        case Push.Completed(key, _, _) => key mustEqual file2.getName
      }.await

      // And pull them back
      s3First.pullResources("folder") must beLike[Seq[Pull]] {
        case Seq(Pull(key1, inputStream1, contentType1, numberOfBytes1), Pull(key2, inputStream2, contentType2, numberOfBytes2)) =>
          key1 mustEqual s"folder/${file1.getName}"
          Source.fromInputStream(inputStream1).mkString mustEqual "blah blah"
          contentType1 must startWith("text/plain")
          numberOfBytes1 mustEqual 9

          key2 mustEqual s"folder/${file2.getName}"
          Source.fromInputStream(inputStream2).mkString mustEqual "blah blah 2"
          contentType2 must startWith("text/plain")
          numberOfBytes2 mustEqual 11
      }.await
    }

    "push an input stream" in new S3ServerEmbedded {
      todo
    }

    "push a non existing file" in new S3ServerEmbedded {
      val bucket = "test-bucket"
      val s3 = new S3(bucket)

      val file = new File(s"whoops.txt")

      s3.push(file.getName, file) must throwAn[Exception](file.getName).await
    }

    "pull a non existing object" in new S3ServerEmbedded {
      val bucket = "test-bucket"
      val s3 = new S3(bucket)

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