package uk.gov.homeoffice.aws.s3

import java.io.File
import scala.util.Try
import grizzled.slf4j.Logging

class Publisher(bucket: String)(implicit val s3Client: S3Client) extends Logging{
  val s3Bucket = s3Client.createBucket(bucket)

  def publish(key: String, file: File) = Try {
    val putObjectResult = s3Client.putObject(bucket, key, file)
    info(s"S3 publication result: $putObjectResult")
    key
  }

  // TODO
  // def publish(key: String, stream: InputStream)
}