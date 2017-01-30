package uk.gov.homeoffice.aws.s3

import akka.http.scaladsl.model.MediaType

case class Attachment(key: String, fileName: String, contentType: MediaType)