package uk.gov.homeoffice.aws.s3

import java.io.{File, InputStream}
import java.net.URL
import java.security.cert.X509Certificate
import java.security.{KeyStore, SecureRandom, Security}
import javax.net.ssl._
import scala.io.Source
import com.amazonaws.{ClientConfiguration, Protocol}
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.retry.PredefinedRetryPolicies
import org.specs2.concurrent.ExecutionEnv
import org.specs2.execute.{AsResult, Result}
import org.specs2.matcher.Scope
import org.specs2.mutable.Specification
import de.flapdoodle.embed.process.runtime.Network.getFreeServerPort
import grizzled.slf4j.Logging
import uk.gov.homeoffice.specs2.ComposableAround
// import io.findify.s3mock.S3Mock

class S3EncryptionSpec(implicit env: ExecutionEnv) extends Specification {
  // -Dtrust_all_cert=true
  System.setProperty("trust_all_cert", "true")

  "S3" should {
    pending

    /*"push an AES256 encrypted file and pull it back" in new S3ServerSSLEmbedded {
      val bucket = "test-bucket"
      val s3 = new S3(bucket)(s3Client)

      val file = new File(s"$s3Directory/test-file.txt")

      s3.push(file.getName, file, Some(AES256("secret key"))) must beLike[Push] {
        case c: Push.Completed => c.key mustEqual file.getName
      }.await

      s3.pullResource(file.getName) must beLike[Resource] {
        case Resource(key, inputStream, contentType, numberOfBytes) =>
          Source.fromInputStream(inputStream).mkString mustEqual "blah blah"
          contentType must startWith("text/plain")
          numberOfBytes mustEqual 9
      }.await
    }*/

    /*"push a KMS encrypted file and pull it back" in new S3ServerEmbedded {
      val bucket = "test-bucket"
      val s3 = new S3(bucket)

      val file = new File(s"$s3Directory/test-file.txt")

      s3.push(file.getName, file, Some(KMS("secret key"))) must beLike[Push] {
        case c: Push.Completed => c.key mustEqual file.getName
      }.await

      s3.pullResource(file.getName) must beLike[Resource] {
        case Resource(key, inputStream, contentType, numberOfBytes) =>
          Source.fromInputStream(inputStream).mkString mustEqual "blah blah"
          contentType must startWith("text/plain")
          numberOfBytes mustEqual 9
      }.await
    }*/
  }
}

//////////////

/*trait S3ServerSSLEmbedded extends S3Server with Scope with ComposableAround with Logging {
  val s3Port = getFreeServerPort
  val s3Host = new URL(s"https://127.0.0.1:$s3Port")

  val s3Directory = "src/test/resources/s3"

  val s3Server = S3Mock(s3Port, s3Directory)
  s3Server start

  implicit val clientConfiguration = new ClientConfiguration
  clientConfiguration.setProtocol(Protocol.HTTPS)

  implicit val s3Client = new S3Client(s3Host, new AnonymousAWSCredentials())

  override def around[R: AsResult](r: => R): Result = try {
    info(s"Started S3 $s3Host")
    super.around(r)
  } finally {
    info(s"Stopping S3 $s3Host")
    s3Server stop
  }
}


import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl._
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{HttpHeader, HttpResponse, Multipart, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Framing, Sink}
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.error.NoSuchKeyException
import io.findify.s3mock.provider.{FileProvider, Provider}
import io.findify.s3mock.request.{CompleteMultipartUpload, CreateBucketConfiguration}
import io.findify.s3mock.route.{PutObject, _}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.io.Source
import scala.util.{Failure, Success, Try}


class S3Mock(port:Int, provider:Provider)(implicit system:ActorSystem = ActorSystem.create("sqsmock")) extends LazyLogging {
  implicit val p = provider
  private var bind: Http.ServerBinding = _

  val chunkSignaturePattern = """([0-9a-fA-F]+);chunk\-signature=([a-z0-9]){64}""".r

  def start = {
    implicit val mat = ActorMaterializer()

    val password: Array[Char] = "change me".toCharArray // do not store passwords in code, read them from somewhere safe!

    val ks: KeyStore = KeyStore.getInstance("PKCS12")
    val keystore: InputStream = getClass.getClassLoader.getResourceAsStream("server.p12")

    require(keystore != null, "Keystore required!")
    ks.load(keystore, password)

    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)

    val sslContext: SSLContext = SSLContext.getInstance("SSL")
    sslContext.init(null, Array(new DummyTrustManager), new java.security.SecureRandom())

    //val sslContext: SSLContext = SSLContext.getInstance("TLS")
    //sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
    val https: HttpsConnectionContext = ConnectionContext.https(sslContext)

    val http = Http(system)

    val route =
      pathPrefix(Segment) { bucket =>
        pathSingleSlash {
          concat(
            ListBucket().route(bucket),
            CreateBucket().route(bucket),
            DeleteBucket().route(bucket)
          )
        } ~ pathEnd {
          concat(
            ListBucket().route(bucket),
            CreateBucket().route(bucket),
            DeleteBucket().route(bucket)
          )
        } ~ path(RemainingPath) { key =>
          concat(
            GetObject().route(bucket, key.toString()),
            CopyObject().route(bucket, key.toString()),
            PutObjectMultipart().route(bucket, key.toString()),
            PutObjectMultipartStart().route(bucket, key.toString()),
            PutObjectMultipartComplete().route(bucket, key.toString()),
            PutObject().route(bucket, key.toString()),
            DeleteObject().route(bucket, key.toString())
          )
        }
      } ~ ListBuckets().route() ~ extractRequest { request =>
        complete {
          logger.error(s"method not implemented: ${request.method.value} ${request.uri.toString}")
          HttpResponse(status = StatusCodes.NotImplemented)
        }
      }

    bind = Await.result(http.bindAndHandle(route, "localhost", port, connectionContext = https), Duration.Inf)

    bind
  }

  def stop = Await.result(bind.unbind(), Duration.Inf)

}

object S3Mock {
  def apply(port:Int, dir:String) = new S3Mock(port, new FileProvider(dir))
  def create(port:Int, dir:String) = apply(port, dir) // Java API
}

class DummyTrustManager extends X509TrustManager {
  override def getAcceptedIssuers() = Array[X509Certificate]()
  override def checkClientTrusted(arg0: Array[X509Certificate], arg1: String) = {
    println("checkClientTrusted "+arg0+" ... "+arg1)
  }
  override def checkServerTrusted(arg0: Array[X509Certificate], arg1: String) = {
    println("checkServedTrusted "+arg0+" ... "+arg1)
  }
}*/
