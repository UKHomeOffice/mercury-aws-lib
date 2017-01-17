package uk.gov.homeoffice.aws.s3

import java.io.File

class Publisher(bucket: String)(implicit val s3Client: S3Client) {
  val s3Bucket = s3.createBucket(bucket) // s3Client.s3.createBucket(bucket) //s3Client.createBucket(bucket)

  def publish(key: String, file: File) = s3Client.publish(key, file) //s3Bucket. .put(key, file)

  // TODO
  // def publish(key: String, stream: InputStream)
}