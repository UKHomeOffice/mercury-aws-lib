package uk.gov.homeoffice.aws.s3

import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest, SSEAlgorithm, SSEAwsKeyManagementParams}

trait Encryption {
  def encrypt(request: PutObjectRequest): PutObjectRequest
}

trait EncryptByAlgorithm {
  this: Encryption =>

  def encrypt(request: PutObjectRequest, algorithm: String, secret: String): PutObjectRequest = {
    val objectMetadata = new ObjectMetadata()
    objectMetadata.setSSEAlgorithm(algorithm)

    request.withMetadata(objectMetadata).withSSEAwsKeyManagementParams(new SSEAwsKeyManagementParams(secret))
  }
}

case class AES256(secret: String) extends Encryption with EncryptByAlgorithm {
  override def encrypt(request: PutObjectRequest): PutObjectRequest =
    encrypt(request, SSEAlgorithm.AES256.getAlgorithm, secret)
}

case class KMS(secret: String) extends Encryption with EncryptByAlgorithm  {
  override def encrypt(request: PutObjectRequest): PutObjectRequest =
    encrypt(request, SSEAlgorithm.KMS.getAlgorithm, secret)
}