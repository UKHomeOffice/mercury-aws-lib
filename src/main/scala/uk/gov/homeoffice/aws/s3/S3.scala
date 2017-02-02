package uk.gov.homeoffice.aws.s3

import java.io.File
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try
import com.amazonaws.event.ProgressEventType._
import com.amazonaws.event.{ProgressEvent, ProgressListener}
import com.amazonaws.services.s3.transfer.TransferManager
import grizzled.slf4j.Logging

class S3(bucket: String)(implicit val s3Client: S3Client) extends Logging {
  val s3Bucket = s3Client.createBucket(bucket)

  def pull(key: String)(implicit ec: ExecutionContext): Future[Pull] = Future {
    val s3Object = s3Client.getObject(bucket, key)
    val inputStream = s3Object.getObjectContent
    val contentType = s3Object.getObjectMetadata.getContentType
    val numberOfBytes = s3Object.getObjectMetadata.getContentLength
    info(s"""Pull for $key with $numberOfBytes Bytes of content type "$contentType" from bucket $bucket""")

    Pull(inputStream, contentType, numberOfBytes)
  }

  def push(key: String, file: File)(implicit ec: ExecutionContext): Future[Push] = { // TODO add argument to provide Progress that can be called back
    val result = Promise[Push]()

    Try {
      val transferManager = new TransferManager(s3Client)
      val upload = transferManager.upload(bucket, key, file)

      Future {
        val exception = upload.waitForException()

        if (exception != null) {
          result tryFailure exception
        }

        transferManager.shutdownNow(false)
      }

      val done: Push => Unit = { r =>
        result trySuccess r
        transferManager.shutdownNow(false)
      }

      upload.addProgressListener(new ProgressListener {
        var start = 0L
        var currentPercentTransferred = 0.0

        override def progressChanged(progressEvent: ProgressEvent): Unit = progressEvent.getEventType match {
          case TRANSFER_STARTED_EVENT =>
            info(s"Push started for ${file.getName} to bucket $bucket")
            start = System.currentTimeMillis

          case c @ (TRANSFER_COMPLETED_EVENT | TRANSFER_PART_COMPLETED_EVENT) =>
            val completed = if (c == TRANSFER_COMPLETED_EVENT) {
              Push.Completed(file.getName, upload.getProgress.getTotalBytesToTransfer, System.currentTimeMillis - start)
            } else {
              Push.CompletedPartially(file.getName, upload.getProgress.getTotalBytesToTransfer, System.currentTimeMillis - start)
            }

            info(completed.message)
            done(completed)

          case TRANSFER_CANCELED_EVENT =>
            val cancelled = Push.Cancelled(file.getName)
            warn(cancelled.message)
            done(cancelled)

          case e @ (TRANSFER_FAILED_EVENT | TRANSFER_PART_FAILED_EVENT) =>
            val failed = Push.Failed(file.getName)
            error(s"${failed.message} because of $e")
            done(failed)

          case _ =>
            if (currentPercentTransferred != upload.getProgress.getPercentTransferred) {
              currentPercentTransferred = upload.getProgress.getPercentTransferred
              info(s"Push progress for ${file.getName}: $currentPercentTransferred %")
            }
        }
      })
    } recover {
      case t: Throwable => result tryFailure t
    }

    result.future
  }

  // TODO
  // def publish(key: String, stream: InputStream)
}