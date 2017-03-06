package uk.gov.homeoffice.aws.s3

import java.io.File
import java.util.concurrent.atomic.AtomicLong
import java.util.function.LongUnaryOperator
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try
import com.amazonaws.event.ProgressEventType._
import com.amazonaws.event.{ProgressEvent, ProgressListener}
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import grizzled.slf4j.Logging

object S3 {
  type ResourceKey = String
  type ResourcesKey = String
}

/**
  * You may want to "set path style access on" for the S3Client you provide by doing the following:
  * <pre>
  * s3Client.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build())
  * </pre>
  * which configures the client to use path-style access for all requests.
  * Note that the S3Client you provide may need its Region set if you plan to use encryption - the open source "mercury" on Github uses encryption.
  * @param bucket String The name of the bucket
  * @param s3Client S3Client to interact with the bucket on AWS S3
  */
class S3(val bucket: String)(implicit val s3Client: S3Client) extends Logging {
  import S3._

  val groupByTopDirectory: Seq[Resource] => Map[ResourcesKey, Seq[Resource]] = _.groupBy { resource =>
    resource.key.indexOf("/") match {
      case -1 => resource.key
      case i => resource.key.take(i)
    }
  }

  /**
    * Pull resource by a given key
    * @param key ResourceKey (String) Uniquely identifying a resource within this bucket
    * @param ec ExecutionContext Used to run this function in
    * @return Future[Resource] if found, otherwise a failed Future
    */
  def pullResource(key: ResourceKey)(implicit ec: ExecutionContext): Future[Resource] = Future {
    val s3Object = s3Client.getObject(bucket, key)

    val inputStream = s3Object.getObjectContent
    val contentType = s3Object.getObjectMetadata.getContentType
    val numberOfBytes = s3Object.getObjectMetadata.getContentLength
    val lastModifiedDate = s3Object.getObjectMetadata.getLastModified
    info(s"""Pull for $key with $numberOfBytes Bytes of content type "$contentType" from bucket $bucket""")

    Resource(key, inputStream, contentType, numberOfBytes, lastModifiedDate)
  }

  /**
    * Pull resource by a given key that acts as a prefix for all possible resources
    * @param key ResourcesKey (String) Uniquely idenifying resources where their keys are prefixed by this given key within this bucket
    * @param ec ExecutionContext Used to run this function in
    * @return Future[Seq[Resource] The available resources for the given key
    * Example usage:
    * <pre>
    *   pullResources("myFolder/")
    * </pre>
    * NOTE the trailing backslash "/". S3 has no real concept of folders, but by using "/" a folder like grouping of resources can be achieved.
    */
  def pullResources(key: ResourcesKey)(implicit ec: ExecutionContext): Future[Seq[Resource]] = Future {
    s3Client.listObjects(bucket, key).getObjectSummaries.sortBy(_.getLastModified).map(_.getKey)
  } flatMap { keys =>
    Future.sequence(keys map pullResource)
  }

  /**
    * @param group Seq[Resource] => Map[ResourcesKey, Seq[Resource]
    * @param ec ExecutionContext Used to run this function in
    * @return Future[Map[ResourcesKey, Seq[Resource] The available resources
    */
  def pullResources(group: Seq[Resource] => Map[ResourcesKey, Seq[Resource]] = groupByTopDirectory)(implicit ec: ExecutionContext): Future[Map[ResourcesKey, Seq[Resource]]] = Future {
    s3Client.listObjects(bucket).getObjectSummaries.sortBy(_.getLastModified).map(_.getKey)
  } flatMap { keys =>
    Future.sequence(keys map pullResource) map group
  }

  def push(key: ResourceKey, file: File, encryption: Option[Encryption] = None)(implicit ec: ExecutionContext): Future[Push] = { // TODO add argument to provide Progress that can be called back
    val result = Promise[Push]()

    Try {
      val transferManager = TransferManagerBuilder.standard().withS3Client(s3Client).build()

      val putObjectRequest = {
        val p = new PutObjectRequest(bucket, key, file)

        encryption map { _.encrypt(p) } getOrElse p
      }

      val upload = transferManager.upload(putObjectRequest)

      val done: Push => Unit = { r =>
        result trySuccess r
        transferManager shutdownNow false
      }

      upload.addProgressListener(new ProgressListener {
        val start = new AtomicLong(0)
        val currentPercentTransferred = new AtomicLong(10)

        override def progressChanged(progressEvent: ProgressEvent): Unit = progressEvent.getEventType match {
          case TRANSFER_STARTED_EVENT =>
            info(s"Push started for ${file.getName} (key = $key) to bucket $bucket")
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
            val newCurrentPercentTransferred = upload.getProgress.getPercentTransferred

            currentPercentTransferred updateAndGet new LongUnaryOperator {
              override def applyAsLong(current: Long) = if (newCurrentPercentTransferred >= current) {
                info(s"Push progress for ${file.getName}: ${newCurrentPercentTransferred.toInt} %")
                current + 10
              } else {
                current
              }
            }

            if (newCurrentPercentTransferred >= 100) {
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
  // def publish(key: ResourceKey, stream: InputStream)
}