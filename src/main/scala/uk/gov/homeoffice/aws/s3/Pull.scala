package uk.gov.homeoffice.aws.s3

import java.io.InputStream

case class Pull(inputStream: InputStream, contentType: String, numberOfBytes: Long)