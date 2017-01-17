package uk.gov.homeoffice.aws.s3

import java.io.File
import scala.util.Try
import com.amazonaws.event.{ProgressEvent, ProgressEventType, ProgressListener}
import com.amazonaws.services.s3.transfer.TransferManager
import grizzled.slf4j.Logging

/*
Original without using TransferManager
class Publisher(bucket: String)(implicit val s3Client: S3Client) extends Logging {
  val s3Bucket = s3Client.createBucket(bucket)

  def publish(key: String, file: File) = Try {
    val putObjectResult = s3Client.putObject(bucket, key, file)
    info(s"S3 publication result: $putObjectResult")
    key
  }

  // TODO
  // def publish(key: String, stream: InputStream)
}
*/

class Publisher(bucket: String)(implicit val s3Client: S3Client) extends Logging {
  val s3Bucket = s3Client.createBucket(bucket)

  def publish(key: String, file: File) = Try {
    val transferManager = new TransferManager(s3Client)
    val upload = transferManager.upload(bucket, key, file)

    upload.addProgressListener(new ProgressListener {
      var currentPercentTransferred = 0.0

      override def progressChanged(progressEvent: ProgressEvent): Unit = progressEvent.getEventType match {
        case ProgressEventType.TRANSFER_COMPLETED_EVENT =>
          info(s"Upload completed for file ${file.getName}")
          transferManager.shutdownNow(false)

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

/*
public void progressChanged(ProgressEvent progressEvent) {
        System.out.println(upload.getProgress().getPercentTransferred() + "%");

        if (progressEvent.getEventCode() == ProgressEvent.COMPLETED_EVENT_CODE) {
            System.out.println("Upload complete!!!");
        }
    }
 */