package uk.gov.homeoffice.aws.s3

import java.io.InputStream

case class Resource(key: String, inputStream: InputStream, contentType: String, numberOfBytes: Long)