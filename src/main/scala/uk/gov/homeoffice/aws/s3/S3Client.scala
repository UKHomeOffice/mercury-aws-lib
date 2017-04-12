package uk.gov.homeoffice.aws.s3

import java.net.URL

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.model.{CryptoConfiguration, EncryptionMaterialsProvider}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client, AmazonS3EncryptionClient}

trait S3Client extends AmazonS3

class S3PlainClient(endpoint: URL, credentials: AWSCredentials)(implicit clientConfiguration: ClientConfiguration = new ClientConfiguration) extends AmazonS3Client(credentials, clientConfiguration) with S3Client {
  setEndpoint(endpoint.toString)

  def clientConfig = clientConfiguration
}

class S3EncryptionClient(endpoint: URL, credentials: AWSCredentials,
                         encryptionMaterialsProvider: EncryptionMaterialsProvider, cryptoConfiguration: CryptoConfiguration)
                        (implicit clientConfiguration: ClientConfiguration = new ClientConfiguration)
  extends AmazonS3EncryptionClient(credentials, encryptionMaterialsProvider, clientConfiguration, cryptoConfiguration) with S3Client {
  setEndpoint(endpoint.toString)

  def clientConfig = clientConfiguration
}