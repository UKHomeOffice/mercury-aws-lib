package uk.gov.homeoffice.aws.s3

import java.io.File
import java.util.concurrent.atomic.AtomicLong
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try
import com.amazonaws.event.ProgressEventType._
import com.amazonaws.event.{ProgressEvent, ProgressListener}
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import com.google.common.util.concurrent.AtomicDouble
import grizzled.slf4j.Logging

/**
  * You may want to "set path style access on" for the S3Client you provide by doing the following:
  * <pre>
  * s3Client.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build())
  * </pre>
  * which configures the client to use path-style access for all requests.
  * @param bucket String The name of the bucket
  * @param s3Client S3Client to interact with the bucket on AWS S3
  */
class S3(bucket: String)(implicit val s3Client: S3Client) extends Logging {
  val s3Bucket = s3Client.createBucket(bucket)

  def pullResource(key: String)(implicit ec: ExecutionContext): Future[Pull] = Future {
    val s3Object = s3Client.getObject(bucket, key)
    val inputStream = s3Object.getObjectContent
    val contentType = s3Object.getObjectMetadata.getContentType
    val numberOfBytes = s3Object.getObjectMetadata.getContentLength
    info(s"""Pull for $key with $numberOfBytes Bytes of content type "$contentType" from bucket $bucket""")

    Pull(key, inputStream, contentType, numberOfBytes)
  }

  def pullResources(key: String)(implicit ec: ExecutionContext): Future[Seq[Pull]] = Future {
    s3Client.listObjects(bucket, s"$key/").getObjectSummaries.sortBy(_.getLastModified).map(_.getKey)
  } flatMap { keys =>
    Future.sequence(keys map pullResource)
  }

  def push(key: String, file: File, encryption: Option[Encryption] = None)(implicit ec: ExecutionContext): Future[Push] = { // TODO add argument to provide Progress that can be called back
    val result = Promise[Push]()

    Try {
      val transferManager = TransferManagerBuilder.standard().withS3Client(s3Client).build()

      val putObjectRequest = {
        val p = new PutObjectRequest(bucket, key, file)

        encryption map { _.encrypt(p) } getOrElse p
      }

      val upload = transferManager.upload(putObjectRequest)

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
        val start = new AtomicLong(0L)
        val currentPercentTransferred = new AtomicDouble(0.0)

        override def progressChanged(progressEvent: ProgressEvent): Unit = progressEvent.getEventType match {
          case TRANSFER_STARTED_EVENT =>
            info(s"Push started for ${file.getName} to bucket $bucket")
            start.set(System.currentTimeMillis)

          case c @ (TRANSFER_COMPLETED_EVENT | TRANSFER_PART_COMPLETED_EVENT) =>
            val completed = if (c == TRANSFER_COMPLETED_EVENT) {
              Push.Completed(file.getName, upload.getProgress.getTotalBytesToTransfer, System.currentTimeMillis - start.get())
            } else {
              Push.CompletedPartially(file.getName, upload.getProgress.getTotalBytesToTransfer, System.currentTimeMillis - start.get())
            }

            done(completed)

          case TRANSFER_CANCELED_EVENT =>
            val cancelled = Push.Cancelled(file.getName)
            done(cancelled)

          case TRANSFER_FAILED_EVENT | TRANSFER_PART_FAILED_EVENT =>
            val failed = Push.Failed(file.getName)
            done(failed)

          case _ =>
            if (currentPercentTransferred.get() != upload.getProgress.getPercentTransferred) {
              currentPercentTransferred.set(upload.getProgress.getPercentTransferred)
              info(s"Push progress for ${file.getName}: $currentPercentTransferred %")
            }

            if (upload.getProgress.getPercentTransferred == 100) {
              progressChanged(new ProgressEvent(TRANSFER_COMPLETED_EVENT))
            }
        }
      })
    } recover {
      case t: Throwable => result tryFailure t
    }

    result.future map { push =>
      push match {
        case p: Push.Failed => error(p.message)
        case p: Push.Cancelled => warn(p.message)
        case p => info(p.message)
      }

      push
    }
  }

  // TODO
  // def publish(key: String, stream: InputStream)
}