package uk.gov.homeoffice.aws.s3.publish

import java.io.File
import scala.concurrent.{Future, Promise}
import scala.util.Try
import com.amazonaws.event.ProgressEventType._
import com.amazonaws.event.{ProgressEvent, ProgressListener}
import com.amazonaws.services.s3.transfer.TransferManager
import grizzled.slf4j.Logging
import uk.gov.homeoffice.aws.s3.S3Client

class Publisher(bucket: String)(implicit val s3Client: S3Client) extends Logging {
  val s3Bucket = s3Client.createBucket(bucket)

  def publish(key: String, file: File): Future[Result] = { // TODO add argument to provide Progress that can be called back
    val result = Promise[Result]()

    Try {
      val transferManager = new TransferManager(s3Client)
      val upload = transferManager.upload(bucket, key, file)

      val done: Result => Unit = { r =>
        result success r
        transferManager.shutdownNow(false)
      }

      upload.addProgressListener(new ProgressListener {
        var start = 0L
        var currentPercentTransferred = 0.0

        override def progressChanged(progressEvent: ProgressEvent): Unit = progressEvent.getEventType match {
          case TRANSFER_STARTED_EVENT =>
            info(s"Upload started for ${file.getName}")
            start = System.currentTimeMillis

          case c @ (TRANSFER_COMPLETED_EVENT | TRANSFER_PART_COMPLETED_EVENT) =>
            val completed = if (c == TRANSFER_COMPLETED_EVENT) {
              Completed(file.getName, upload.getProgress.getTotalBytesToTransfer, System.currentTimeMillis - start)
            } else {
              CompletedPartially(file.getName, upload.getProgress.getTotalBytesToTransfer, System.currentTimeMillis - start)
            }

            info(completed.message)
            done(completed)

          case TRANSFER_CANCELED_EVENT =>
            val cancelled = Cancelled(file.getName)
            warn(cancelled.message)
            done(cancelled)

          case e @ (TRANSFER_FAILED_EVENT | TRANSFER_PART_FAILED_EVENT) =>
            val failed = Failed(file.getName)
            error(s"${failed.message} because of $e")
            done(failed)

          case _ =>
            if (currentPercentTransferred != upload.getProgress.getPercentTransferred) {
              currentPercentTransferred = upload.getProgress.getPercentTransferred
              info(s"Upload progress for ${file.getName}: $currentPercentTransferred %")
            }
        }
      })
    } recover {
      case t: Throwable => result failure t
    }

    result.future
  }

  // TODO
  // def publish(key: String, stream: InputStream)
}