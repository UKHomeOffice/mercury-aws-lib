package uk.gov.homeoffice.aws.s3

import java.io.InputStream

sealed trait Pull

case class Resource(key: String, inputStream: InputStream, contentType: String, numberOfBytes: Long) extends Pull

case class ResourceMissing(message: String, cause: Option[Throwable]) extends Pull

case class ResourceFailure(throwable: Throwable) extends Pull