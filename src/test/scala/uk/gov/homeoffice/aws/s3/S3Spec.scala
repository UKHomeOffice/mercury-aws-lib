package uk.gov.homeoffice.aws.s3

import java.io.File
import scala.io.Source
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class S3Spec(implicit env: ExecutionEnv) extends Specification {
  "S3" should {
    "push a file" in new S3ServerEmbedded {
      val bucket = "test-bucket"

      val file = new File(s"$s3Directory/test-file.txt")
      val s3 = new S3(bucket)

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

    "push files to same bucket" in new S3ServerEmbedded {
      val bucket = "test-bucket"

      // First file to push
      val file = new File(s"$s3Directory/test-file.txt")
      val s3 = new S3(bucket)

      s3.push(file.getName, file) must beLike[Push] {
        case c: Push.Completed => c.key mustEqual file.getName
      }.await

      // Second file to push
      val file2 = new File(s"$s3Directory/test-file-2.txt")
      val s32 = new S3(bucket)

      s32.push(file2.getName, file2) must beLike[Push] {
        case c: Push.Completed => c.key mustEqual file2.getName
      }.await
    }

    "push a non existing file" in new S3ServerEmbedded {
      todo
    }

    "pull a non existing object" in new S3ServerEmbedded {
      todo
    }
  }
}