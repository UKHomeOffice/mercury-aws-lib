package uk.gov.homeoffice.aws.s3

import java.io.File
import scala.util.Try
import com.amazonaws.event.ProgressEventType._
import com.amazonaws.event.{ProgressEvent, ProgressListener}
import com.amazonaws.services.s3.transfer.TransferManager
import grizzled.slf4j.Logging

class Publisher(bucket: String)(implicit val s3Client: S3Client) extends Logging {
  val s3Bucket = s3Client.createBucket(bucket)

  def publish(key: String, file: File) = Try {
    val transferManager = new TransferManager(s3Client)
    val upload = transferManager.upload(bucket, key, file)
    var start = 0L

    upload.addProgressListener(new ProgressListener {
      var currentPercentTransferred = 0.0

      override def progressChanged(progressEvent: ProgressEvent): Unit = progressEvent.getEventType match {
        case TRANSFER_STARTED_EVENT =>
          info(s"Upload started for file ${file.getName}")
          start = System.currentTimeMillis

        case TRANSFER_COMPLETED_EVENT =>
          val time = (System.currentTimeMillis - start) / 1000.0

          val mb = upload.getProgress.getTotalBytesToTransfer / 1048576.0
          val kb = if (mb < 1) Some(upload.getProgress.getTotalBytesToTransfer / 1024.0) else None

          kb.fold(info(s"Upload completed for file ${file.getName} with size $mb M in $time seconds")) { kb =>
            info(s"Upload completed for file ${file.getName} with size $kb K in $time seconds")
          }

          transferManager.shutdownNow(false)

        case e @ (TRANSFER_CANCELED_EVENT | TRANSFER_PART_COMPLETED_EVENT) =>
          warn(s"Upload incomplete for file ${file.getName} because of $e")

        case e @ (TRANSFER_FAILED_EVENT | TRANSFER_PART_FAILED_EVENT) =>
          error(s"Upload failed for file ${file.getName} because of $e")

        case _ =>

          if (currentPercentTransferred != upload.getProgress.getPercentTransferred) {
            currentPercentTransferred = upload.getProgress.getPercentTransferred
            info(s"Upload progress for file ${file.getName}: $currentPercentTransferred %")
          }
      }
    })

    upload
  }

  // TODO
  // def publish(key: String, stream: InputStream)
}