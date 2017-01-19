package uk.gov.homeoffice.aws.s3

import java.io.File
import scala.io.Source
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

      s3.pull(file.getName) must beLike[Pull] {
        case Pull(inputStream, contentType, numberOfBytes) =>
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
      s3First.pull(file1.getName) must beLike[Pull] {
        case Pull(inputStream, contentType, numberOfBytes) =>
          Source.fromInputStream(inputStream).mkString mustEqual "blah blah"
          contentType must startWith("text/plain")
          numberOfBytes mustEqual 9
      }.await

      s3Second.pull(file2.getName) must beLike[Pull] {
        case Pull(inputStream, contentType, numberOfBytes) =>
          Source.fromInputStream(inputStream).mkString mustEqual "blah blah 2"
          contentType must startWith("text/plain")
          numberOfBytes mustEqual 11
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

      s3.pull("whoops.text") must throwAn[Exception]("Not Found").await
    }
  }
}